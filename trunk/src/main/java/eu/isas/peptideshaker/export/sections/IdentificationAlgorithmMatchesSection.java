package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.tags.Tag;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures;
import eu.isas.peptideshaker.export.exportfeatures.IdentificationAlgorithmMatchesFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeatures;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This report section contains the results of the identification algorithms
 *
 * @author Marc
 */
public class IdentificationAlgorithmMatchesSection {

    /**
     * The features to export.
     */
    private ArrayList<ExportFeature> matchExportFeatures = new ArrayList<ExportFeature>();
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
    public IdentificationAlgorithmMatchesSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof IdentificationAlgorithmMatchesFeatures) {
                matchExportFeatures.add(exportFeature);
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
     * Writes the desired section. Exports all algorithm assuptions including the decoy and non-validated matches.
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

                    boolean first = true;
                    
                    if (indexes) {
                        if (linePrefix != null) {
                            writer.write(linePrefix);
                        }
                        writer.write(line + "");
                        first = false;
                    }

                    spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    
                    for (int advocateId : spectrumMatch.getAdvocates()) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptions = spectrumMatch.getAllAssumptions(advocateId);
                        ArrayList<Double> scores = new ArrayList<Double>(assumptions.keySet());
                        Collections.sort(scores);
                        for (double score : scores) {
                            for (SpectrumIdentificationAssumption assumption : assumptions.get(score)) {
                        for (ExportFeature exportFeature : matchExportFeatures) {
                            if (!first) {
                                writer.write(separator);
                            } else {
                                first = false;
                            }
                            psParameter = (PSParameter) assumption.getUrParam(psParameter);
                            IdentificationAlgorithmMatchesFeatures identificationAlgorithmMatchesFeature = (IdentificationAlgorithmMatchesFeatures) exportFeature;
                            String feature;
                            if (assumption instanceof PeptideAssumption) {
                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                feature = getPeptideAssumptionFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, peptideAssumption, spectrumKey, psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else if (assumption instanceof TagAssumption) {
                                TagAssumption tagAssumption = (TagAssumption) assumption;
                                feature = getTagAssumptionFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, tagAssumption, spectrumKey, psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else {
                                throw new IllegalArgumentException("Spectrum identification assumption of type " + assumption.getClass() + " not supported.");
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
        for (ExportFeature exportFeature : matchExportFeatures) {
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

    /**
     * Writes the feature associated to the match of the given peptide assumption.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param separator the column separator
     * @param peptideAssumption the assumption for the match to inspect
     * @param spectrumKey the key of the spectrum
     * @param psParameter the PeptideShaker parameter of the match
     * @param exportFeature the feature to export
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
    public static String getPeptideAssumptionFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, String separator, 
            PeptideAssumption peptideAssumption, String spectrumKey, PSParameter psParameter, IdentificationAlgorithmMatchesFeatures exportFeature, 
            WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (exportFeature) {
            case rank:
                return peptideAssumption.getRank() + "";
            case variable_ptms:
                    HashMap<String, ArrayList<Integer>> modMap = getModMap(peptideAssumption.getPeptide(), true);
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
                    modMap = getModMap(peptideAssumption.getPeptide(), false);
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
            case accessions:
                result = new StringBuilder();
                    ArrayList<String> accessions = peptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                    for (String accession : accessions) {
                        if (result.length() > 0) {
                            result.append(", ");
                        }
                        result.append(accession);
                    }
                return result.toString();
            case protein_description:
                SequenceFactory sequenceFactory = SequenceFactory.getInstance();
                StringBuilder descriptions = new StringBuilder();
                    accessions = peptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                    Collections.sort(accessions);
                    for (String accession : accessions) {
                        if (descriptions.length() > 0) {
                            descriptions.append("; ");
                        }
                        descriptions.append(sequenceFactory.getHeader(accession).getDescription());
                    }
                return descriptions.toString();
            case algorithm_confidence:
                return psParameter.getSearchEngineConfidence()+ "";
            case decoy:
                    if (peptideAssumption.getPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
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
                    return peptideAssumption.getIdentificationCharge().toString();
            case isotope:
                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                    return peptideAssumption.getIsotopeNumber(precursor.getMz()) + "";
            case mz:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getMz() + "";
            case total_spectrum_intensity:
                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getTotalIntensity() + "";
            case max_intensity:
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getMaxIntensity() + "";
            case mz_error:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                    return peptideAssumption.getDeltaMass(precursor.getMz(), true) + "";
            case rt:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getRt() + "";
            case algorithm_score:
                                int id = peptideAssumption.getAdvocate();
                                double score = peptideAssumption.getScore();
                    return Advocate.getAdvocate(id).getName() + " (" + score + ")";
            case sequence:
                    return peptideAssumption.getPeptide().getSequence();
            case missed_cleavages:
                    String sequence = peptideAssumption.getPeptide().getSequence();
                    return Peptide.getNMissedCleavages(sequence, searchParameters.getEnzyme()) + "";
            case modified_sequence:
                    return peptideAssumption.getPeptide().getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true) + "";
            case spectrum_charge:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString() + "";
            case spectrum_file:
                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                return spectrumFile;
            case spectrum_scan_number:
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getScanNumber();
            case spectrum_title:
                return Spectrum.getSpectrumTitle(spectrumKey);
            case starred:
                if (psParameter.isStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case theoretical_mass:
                    return peptideAssumption.getPeptide().getMass() + "";
            case validated:
                return psParameter.getMatchValidationLevel().toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the feature associated to the match of the given tag
     * assumption.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param separator the column separator
     * @param spectrumKey the key of the spectrum
     * @param tagAssumption the assumption for the match to inspect
     * @param psParameter the PeptideShaker parameter of the match
     * @param exportFeature the feature to export
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
    public static String getTagAssumptionFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, String separator, 
            TagAssumption tagAssumption, String spectrumKey, PSParameter psParameter, IdentificationAlgorithmMatchesFeatures exportFeature, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (exportFeature) {
            case rank:
                return tagAssumption.getRank() + "";
            case variable_ptms:
                    return Tag.getTagModificationsAsString(tagAssumption.getTag());
            case fixed_ptms:
                    return ""; //@TODO: impplement
            case accessions:
                return "";
            case protein_description:
                return "";
            case algorithm_confidence:
                return psParameter.getSearchEngineConfidence()+ "";
            case decoy:
                return "";
            case hidden:
                if (psParameter.isHidden()) {
                    return "1";
                } else {
                    return "0";
                }
            case identification_charge:
                    return tagAssumption.getIdentificationCharge().toString();
            case isotope:
                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                    return tagAssumption.getIsotopeNumber(precursor.getMz()) + "";
            case mz:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getMz() + "";
            case total_spectrum_intensity:
                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getTotalIntensity() + "";
            case max_intensity:
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getMaxIntensity() + "";
            case mz_error:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                    return tagAssumption.getDeltaMass(precursor.getMz(), true) + "";
            case rt:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getRt() + "";
            case algorithm_score:
                                int id = tagAssumption.getAdvocate();
                                double score = tagAssumption.getScore();
                    return Advocate.getAdvocate(id).getName() + " (" + score + ")";
            case sequence:
                    return tagAssumption.getTag().asSequence();
            case missed_cleavages:
                return "";
            case modified_sequence:
                    return tagAssumption.getTag().getTaggedModifiedSequence(searchParameters.getModificationProfile(), false, false, true, false);
            case spectrum_charge:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString() + "";
            case spectrum_file:
                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                return spectrumFile;
            case spectrum_scan_number:
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getScanNumber();
            case spectrum_title:
                return Spectrum.getSpectrumTitle(spectrumKey);
            case starred:
                if (psParameter.isStarred()) {
                    return "1";
                } else {
                    return "0";
                }
            case theoretical_mass:
                    return tagAssumption.getTag().getMass() + "";
            case validated:
                return psParameter.getMatchValidationLevel().toString();
            default:
                return "Not implemented";
        }
    }
}
