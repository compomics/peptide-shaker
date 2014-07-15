package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.IdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeature;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM level export features.
 *
 * @author Marc Vaudel
 */
public class PsmSection {

    /**
     * The features to export.
     */
    private ArrayList<PsmFeature> psmFeatures = new ArrayList<PsmFeature>();
    /**
     * The features to export.
     */
    private ArrayList<IdentificationAlgorithmMatchesFeature> identificationAlgorithmMatchesFeatures = new ArrayList<IdentificationAlgorithmMatchesFeature>();
    /**
     * The fragment subsection if needed.
     */
    private FragmentSection fragmentSection = null;
    /**
     * The separator used to separate columns.
     */
    private String separator;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param separator the separator to write between columns
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsmSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsmFeature) {
                psmFeatures.add((PsmFeature) exportFeature);
            } else if (exportFeature instanceof IdentificationAlgorithmMatchesFeature) {
                identificationAlgorithmMatchesFeatures.add((IdentificationAlgorithmMatchesFeature) exportFeature);
            } else if (exportFeature instanceof FragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(psmFeatures);
        Collections.sort(identificationAlgorithmMatchesFeatures);
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new FragmentSection(fragmentFeatures, separator, indexes, header, writer);
        }
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        HashMap<String, ArrayList<String>> psmMap = new HashMap<String, ArrayList<String>>();

        if (keys == null) {
            psmMap = identification.getSpectrumIdentificationMap();
        } else {
            for (String key : keys) {
                String fileName = Spectrum.getSpectrumFile(key);
                if (!psmMap.containsKey(fileName)) {
                    psmMap.put(fileName, new ArrayList<String>());
                }
                psmMap.get(fileName).add(key);
            }
        }

        PSParameter psParameter = new PSParameter();
        SpectrumMatch spectrumMatch = null;
        int line = 1;

        int totalSize = 0;

        for (String spectrumFile : psmMap.keySet()) {
            totalSize += psmMap.get(spectrumFile).size();
        }

        // get the spectrum keys
        ArrayList<String> spectrumKeys = new ArrayList<String>();

        for (String spectrumFile : psmMap.keySet()) {
            for (String spectrumKey : psmMap.get(spectrumFile)) {
                if (!spectrumKeys.contains(spectrumKey)) {
                    spectrumKeys.add(spectrumKey);
                }
            }
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Spectra. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadSpectrumMatches(spectrumKeys, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Spectrum Details. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadSpectrumMatchParameters(spectrumKeys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(totalSize);
        }

        for (String spectrumFile : psmMap.keySet()) {

            for (String spectrumKey : psmMap.get(spectrumFile)) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }

                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                    spectrumMatch = identification.getSpectrumMatch(spectrumKey);

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (decoys || peptideAssumption == null || !peptideAssumption.getPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {

                        boolean first = true;

                        if (indexes) {
                            if (linePrefix != null) {
                                writer.write(linePrefix);
                            }
                            writer.write(line + "");
                            first = false;
                        }

                        for (PsmFeature psmFeature : psmFeatures) {
                            if (!first) {
                                writer.write(separator);
                            } else {
                                first = false;
                            }
                            writer.write(getFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, spectrumMatch, psParameter, psmFeature, validatedOnly, decoys, waitingHandler));
                        }
                        for (IdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : identificationAlgorithmMatchesFeatures) {
                            if (!first) {
                                writer.write(separator);
                            } else {
                                first = false;
                            }
                            String feature;
                            if (peptideAssumption != null) {
                                peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                                feature = IdentificationAlgorithmMatchesSection.getPeptideAssumptionFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, peptideAssumption, spectrumMatch.getKey(), psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else if (spectrumMatch.getBestTagAssumption() != null) {
                                TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                                feature = IdentificationAlgorithmMatchesSection.getTagAssumptionFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, tagAssumption, spectrumMatch.getKey(), psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else {
                                throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                            }
                            writer.write(feature);
                        }
                        writer.newLine();
                        if (fragmentSection != null) {
                            String fractionPrefix = "";
                            if (linePrefix != null) {
                                fractionPrefix += linePrefix;
                            }
                            fractionPrefix += line + ".";
                            fragmentSection.writeSection(spectrumMatch, searchParameters, annotationPreferences, fractionPrefix, null);
                        }
                        line++;
                    }
                }
            }
        }
    }

    /**
     * Writes the given feature of the current section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param separator the column separator
     * @param spectrumMatch the spectrum match inspected
     * @param psParameter the PeptideShaker parameter of the match
     * @param psmFeature the feature to export
     * @param validatedOnly indicates whether only validated hits should be
     * exported
     * @param decoys indicates whether decoys should be included in the export
     * @param waitingHandler the waiting handler
     *
     * @return the content corresponding to the given feature of the current
     * section
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static String getFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, String separator, SpectrumMatch spectrumMatch, PSParameter psParameter, PsmFeature psmFeature, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        switch (psmFeature) {
            case probabilistic_score:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    StringBuilder result = new StringBuilder();
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        ArrayList<String> modList = new ArrayList<String>(ptmScores.getScoredPTMs());
                        Collections.sort(modList);
                        for (String mod : modList) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                            ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getProbabilisticSites());
                            if (!sites.isEmpty()) {
                                Collections.sort(sites);
                                if (result.length() > 0) {
                                    result.append(", ");
                                }
                                result.append(mod).append(" (");
                                boolean firstSite = true;
                                for (int site : sites) {
                                    if (firstSite) {
                                        firstSite = false;
                                    } else {
                                        result.append(", ");
                                    }
                                    result.append(site).append(": ").append(ptmScoring.getProbabilisticScore(site));
                                }
                                result.append(")");
                            }
                        }
                    }
                    return result.toString();
                }
                return "";
            case d_score:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    StringBuilder result = new StringBuilder();
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        ArrayList<String> modList = new ArrayList<String>(ptmScores.getScoredPTMs());
                        Collections.sort(modList);
                        for (String mod : modList) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                            ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getDSites());
                            if (!sites.isEmpty()) {
                                Collections.sort(sites);
                                if (result.length() > 0) {
                                    result.append(", ");
                                }
                                result.append(mod).append(" (");
                                boolean firstSite = true;
                                for (int site : sites) {
                                    if (firstSite) {
                                        firstSite = false;
                                    } else {
                                        result.append(", ");
                                    }
                                    result.append(site).append(": ").append(ptmScoring.getDeltaScore(site));
                                }
                                result.append(")");
                            }
                        }
                    }
                    return result.toString();
                }
                return "";
            case localization_confidence:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    ArrayList<String> modList = new ArrayList<String>(getModMap(spectrumMatch.getBestPeptideAssumption().getPeptide(), true).keySet());
                    Collections.sort(modList);
                    PSPtmScores ptmScores = new PSPtmScores();
                    StringBuilder result = new StringBuilder();
                    for (String mod : modList) {

                        PTM ptm = PTMFactory.getInstance().getPTM(mod);

                        if (ptm.getType() == PTM.MODAA) {

                            if (result.length() > 0) {
                                result.append(", ");
                            }
                            result.append(mod);

                            result.append(" (");
                            if (spectrumMatch.getUrParam(ptmScores) != null) {
                                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                    PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                    boolean firstSite = true;

                                    ArrayList<Integer> sites = ptmScoring.getOrderedPtmLocations();
                                    if (sites.isEmpty()) {
                                        result.append("Not Scored");
                                    } else {
                                        for (int site : ptmScoring.getOrderedPtmLocations()) {

                                            if (firstSite) {
                                                firstSite = false;
                                            } else {
                                                result.append(", ");
                                            }
                                            int ptmConfidence = ptmScoring.getLocalizationConfidence(site);

                                            if (ptmConfidence == PtmScoring.NOT_FOUND) {
                                                result.append(site).append(": Not Scored");
                                            } else if (ptmConfidence == PtmScoring.RANDOM) {
                                                result.append(site).append(": Random");
                                            } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                                                result.append(site).append(": Doubtfull");
                                            } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                                                result.append(site).append(": Confident");
                                            } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                                                result.append(site).append(": Very Confident");
                                            }
                                        }
                                    }
                                } else {
                                    result.append("Not Scored");
                                }

                            } else {
                                result.append("Not Scored");
                            }
                            result.append(")");
                        }
                    }
                    return result.toString();
                }
                return "";
            case algorithm_score:
                HashMap<Integer, Double> scoreMap = new HashMap<Integer, Double>();
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : spectrumMatch.getAllAssumptions()) {
                        if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                            if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(spectrumMatch.getBestPeptideAssumption().getPeptide(), PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                                int id = peptideAssumption.getAdvocate();
                                double score = peptideAssumption.getScore();
                                Double currentScore = scoreMap.get(id);
                                if (currentScore == null || score < currentScore) {
                                    scoreMap.put(id, score);
                                }
                            }
                        }
                    }
                }
                ArrayList<Integer> ids = new ArrayList<Integer>(scoreMap.keySet());
                Collections.sort(ids);
                StringBuilder result = new StringBuilder();
                for (int id : ids) {
                    if (result.length() != 0) {
                        result.append(", ");
                    }
                    result.append(Advocate.getAdvocate(id).getName()).append(" (").append(scoreMap.get(id)).append(")");
                }
                return result.toString();
            case confidence:
                return psParameter.getPsmConfidence() + "";
            case score:
                return psParameter.getPsmScore() + "";
            case validated:
                return psParameter.getMatchValidationLevel().toString();
            case starred:
                if (psParameter.isStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case hidden:
                if (psParameter.isHidden()) {
                    return "1";
                } else {
                    return "0";
                }
            default:
                return "Not implemented";
        }
    }

    /**
     * Returns a map of the modifications in a peptide. Modification name ->
     * sites.
     *
     * @param peptide
     * @param variablePtms if true, only variable PTMs are shown, false return
     * only the fixed PTMs
     * @return the map of the modifications on a peptide sequence
     */
    private static HashMap<String, ArrayList<Integer>> getModMap(Peptide peptide, boolean variablePtms) {

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if ((variablePtms && modificationMatch.isVariable()) || (!variablePtms && !modificationMatch.isVariable())) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }

        return modMap;
    }

    /**
     * Writes the header of this section.
     *
     * @throws IOException
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.write(separator);
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : psmFeatures) {
            for (String title : exportFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title);
            }
        }
        for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
            for (String title : exportFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title);
            }
        }
        writer.newLine();
    }
}
