package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeatures;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.total_spectrum_intensity;
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
    private ArrayList<ExportFeature> psmFeatures = new ArrayList<ExportFeature>();
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
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public PsmSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsmFeatures) {
                psmFeatures.add(exportFeature);
            } else if (exportFeature instanceof FragmentFeatures) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
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
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
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

                if (indexes) {
                    if (linePrefix != null) {
                        writer.write(linePrefix);
                    }
                    writer.write(line + separator);
                }

                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                for (ExportFeature exportFeature : psmFeatures) {
                    PsmFeatures psmFeature = (PsmFeatures) exportFeature;
                    writer.write(getFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, spectrumMatch, psParameter, psmFeature, waitingHandler) + separator);
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
     * @param waitingHandler the waiting handler
     * 
     * @return the content corresponding to the given feature of the current section
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
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, String separator, SpectrumMatch spectrumMatch, PSParameter psParameter, PsmFeatures psmFeature, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (psmFeature) {
            case variable_ptms:
                HashMap<String, ArrayList<Integer>> modMap = getModMap(spectrumMatch.getBestPeptideAssumption().getPeptide(), true);
                ArrayList<String> modList = new ArrayList<String>(modMap.keySet());
                Collections.sort(modList);

                StringBuilder result = new StringBuilder();
                for (String mod : modList) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    boolean firstAa = true;
                    result.append(mod).append("(");
                    for (int aa : modMap.get(mod)) {
                        if (firstAa) {
                            firstAa = false;
                        } else {
                            result.append(", ");
                        }
                        result.append(aa).append("");
                    }
                    result.append(")");
                }
                return result.toString();
            case fixed_ptms:
                modMap = getModMap(spectrumMatch.getBestPeptideAssumption().getPeptide(), false);
                modList = new ArrayList<String>(modMap.keySet());
                Collections.sort(modList);

                result = new StringBuilder();
                for (String mod : modList) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    boolean first2 = true;
                    result.append(mod).append("(");
                    for (int aa : modMap.get(mod)) {
                        if (first2) {
                            first2 = false;
                        } else {
                            result.append(", ");
                        }
                        result.append(aa).append("");
                    }
                    result.append(")");
                }
                return result.toString();
            case probabilistic_score:
                result = new StringBuilder();
                PSPtmScores ptmScores = new PSPtmScores();
                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                if (ptmScores != null) {
                    modList = new ArrayList<String>(ptmScores.getScoredPTMs());
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
            case d_score:
                result = new StringBuilder();
                ptmScores = new PSPtmScores();
                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
                if (ptmScores != null) {
                    modList = new ArrayList<String>(ptmScores.getScoredPTMs());
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
            case accessions:
                result = new StringBuilder();
                ArrayList<String> accessions = spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                for (String accession : accessions) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    result.append(accession);
                }
                return result.toString();
            case confidence:
                return psParameter.getPsmConfidence() + "";
            case decoy:
                if (spectrumMatch.getBestPeptideAssumption().getPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
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
            case identification_charge:
                return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString();
            case isotope:
                String spectrumKey = spectrumMatch.getKey();
                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return spectrumMatch.getBestPeptideAssumption().getIsotopeNumber(precursor.getMz()) + "";
            case localization_confidence:
                modList = new ArrayList<String>(getModMap(spectrumMatch.getBestPeptideAssumption().getPeptide(), true).keySet());
                Collections.sort(modList);
                ptmScores = new PSPtmScores();
                result = new StringBuilder();
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
            case mz:
                spectrumKey = spectrumMatch.getKey();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getMz() + "";
            case total_spectrum_intensity:
                spectrumKey = spectrumMatch.getKey();
                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getTotalIntensity() + "";
            case max_intensity:
                spectrumKey = spectrumMatch.getKey();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getMaxIntensity() + "";
            case mz_error:
                spectrumKey = spectrumMatch.getKey();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), true) + "";
            case rt:
                spectrumKey = spectrumMatch.getKey();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getRt() + "";
            case score:
                return psParameter.getPsmScore()+ "";
            case sequence:
                return spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
            case missed_cleavages:
                String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                return Peptide.getNMissedCleavages(sequence, searchParameters.getEnzyme()) + "";
            case modified_sequence:
                return spectrumMatch.getBestPeptideAssumption().getPeptide().getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true) + "";
            case spectrum_charge:
                spectrumKey = spectrumMatch.getKey();
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString() + "";
            case spectrum_file:
                spectrumKey = spectrumMatch.getKey();
                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                return spectrumFile;
            case spectrum_scan_number:
                spectrumKey = spectrumMatch.getKey();
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getScanNumber();
            case spectrum_title:
                spectrumKey = spectrumMatch.getKey();
                return Spectrum.getSpectrumTitle(spectrumKey);
            case starred:
                if (psParameter.isStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case theoretical_mass:
                return spectrumMatch.getBestPeptideAssumption().getPeptide().getMass() + "";
            case validated:
                return psParameter.getMatchValidationLevel().toString();
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
        writer.newLine();
    }
}
