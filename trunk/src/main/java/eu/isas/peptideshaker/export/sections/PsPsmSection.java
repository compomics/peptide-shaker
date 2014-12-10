package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.math.util.FastMath;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM level export features.
 *
 * @author Marc Vaudel
 */
public class PsPsmSection {

    /**
     * The features to export.
     */
    private ArrayList<PsPsmFeature> psmFeatures = new ArrayList<PsPsmFeature>();
    /**
     * The features to export.
     */
    private ArrayList<PsIdentificationAlgorithmMatchesFeature> identificationAlgorithmMatchesFeatures = new ArrayList<PsIdentificationAlgorithmMatchesFeature>();
    /**
     * The fragment subsection if needed.
     */
    private PsFragmentSection fragmentSection = null;
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
    private ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsPsmSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPsmFeature) {
                psmFeatures.add((PsPsmFeature) exportFeature);
            } else if (exportFeature instanceof PsIdentificationAlgorithmMatchesFeature) {
                identificationAlgorithmMatchesFeatures.add((PsIdentificationAlgorithmMatchesFeature) exportFeature);
            } else if (exportFeature instanceof PsFragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(psmFeatures);
        Collections.sort(identificationAlgorithmMatchesFeatures);
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new PsFragmentSection(fragmentFeatures, indexes, header, writer);
        }
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
     * @param shotgunProtocol information on the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> keys, String linePrefix, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
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
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(totalSize);
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        for (String spectrumFile : psmMap.keySet()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFile, spectrumKeys, parameters, !identificationAlgorithmMatchesFeatures.isEmpty());

            while (psmIterator.hasNext()) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();

                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (decoys || peptideAssumption == null || !peptideAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {

                        boolean first = true;

                        if (indexes) {
                            if (linePrefix != null) {
                                writer.write(linePrefix);
                            }
                            writer.write(line + "");
                            first = false;
                        }
                        for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : identificationAlgorithmMatchesFeatures) {
                            if (!first) {
                                writer.addSeparator();
                            } else {
                                first = false;
                            }
                            String feature;
                            if (peptideAssumption != null) {
                                peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                                feature = PsIdentificationAlgorithmMatchesSection.getPeptideAssumptionFeature(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, keys, linePrefix, peptideAssumption, spectrumMatch.getKey(), psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else if (spectrumMatch.getBestTagAssumption() != null) {
                                TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                                feature = PsIdentificationAlgorithmMatchesSection.getTagAssumptionFeature(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, keys, linePrefix, tagAssumption, spectrumMatch.getKey(), psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else {
                                throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                            }
                            writer.write(feature);
                        }
                        for (PsPsmFeature psmFeature : psmFeatures) {
                            if (!first) {
                                writer.addSeparator();
                            } else {
                                first = false;
                            }
                            writer.write(getFeature(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, keys, linePrefix, spectrumMatch, psParameter, psmFeature, validatedOnly, decoys, waitingHandler));
                        }
                        writer.newLine();
                        if (fragmentSection != null) {
                            String fractionPrefix = "";
                            if (linePrefix != null) {
                                fractionPrefix += linePrefix;
                            }
                            fractionPrefix += line + ".";
                            writer.increaseDepth();
                            fragmentSection.writeSection(spectrumMatch, shotgunProtocol, identificationParameters, fractionPrefix, null);
                            writer.decreseDepth();
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
     * @param shotgunProtocol information on the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
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
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> keys, String linePrefix, SpectrumMatch spectrumMatch, PSParameter psParameter, PsPsmFeature psmFeature, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        switch (psmFeature) {
            case probabilistic_score:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        StringBuilder result = new StringBuilder();
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
                        return result.toString();
                    }
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
                    PSPtmScores ptmScores = new PSPtmScores();
                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                    if (ptmScores != null) {
                        StringBuilder result = new StringBuilder();
                        ArrayList<String> modList = ptmScores.getScoredPTMs();
                        Collections.sort(modList);
                        for (String mod : modList) {

                            PTM ptm = PTMFactory.getInstance().getPTM(mod);

                            if (ptm.getType() == PTM.MODAA) {

                                if (result.length() > 0) {
                                    result.append(", ");
                                }
                                result.append(mod);

                                result.append(" (");
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

                                result.append(")");
                            }
                        }
                        return result.toString();
                    }
                }
                return "";
            case algorithm_score:
                HashMap<Integer, Double> scoreMap = new HashMap<Integer, Double>();
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = identification.getAssumptions(spectrumMatch.getKey());
                    for (Integer id : assumptionsMap.keySet()) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmAssumptions = assumptionsMap.get(id);
                        for (ArrayList<SpectrumIdentificationAssumption> assumptionsAtScore : algorithmAssumptions.values()) {
                            for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : assumptionsAtScore) {
                                if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                                    if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(spectrumMatch.getBestPeptideAssumption().getPeptide(), identificationParameters.getSequenceMatchingPreferences())) {
                                        double score = peptideAssumption.getScore();
                                        Double currentScore = scoreMap.get(id);
                                        if (currentScore == null || score < currentScore) {
                                            scoreMap.put(id, score);
                                        }
                                    }
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
                return -10 * FastMath.log10(psParameter.getPsmScore()) + "";
            case raw_score:
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
     * Writes the header of this section.
     *
     * @throws IOException
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        for (ExportFeature exportFeature : psmFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
        }
        writer.newLine();
    }
}
