package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.psm_scoring.PsmScore;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This report section contains the results of the identification algorithms.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsIdentificationAlgorithmMatchesSection {

    /**
     * The features to export.
     */
    private ArrayList<PsIdentificationAlgorithmMatchesFeature> matchExportFeatures = new ArrayList<PsIdentificationAlgorithmMatchesFeature>();
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
     * A peptide spectrum annotator.
     */
    private static final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsIdentificationAlgorithmMatchesSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsIdentificationAlgorithmMatchesFeature) {
                PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature = (PsIdentificationAlgorithmMatchesFeature) exportFeature;
                matchExportFeatures.add(identificationAlgorithmMatchesFeature);
            } else if (exportFeature instanceof PsFragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        Collections.sort(matchExportFeatures);
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new PsFragmentSection(fragmentFeatures, indexes, header, writer);
        }
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section. Exports all algorithm assumptions including
     * the decoy and non-validated matches.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param shotgunProtocol information on the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> keys,
            String linePrefix, int nSurroundingAA, WaitingHandler waitingHandler) throws IOException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        HashMap<String, HashSet<String>> psmMap = new HashMap<String, HashSet<String>>();

        if (keys == null) {
            psmMap = identification.getSpectrumIdentificationMap();
        } else {
            for (String key : keys) {
                String fileName = Spectrum.getSpectrumFile(key);
                if (!psmMap.containsKey(fileName)) {
                    psmMap.put(fileName, new HashSet<String>());
                }
                psmMap.get(fileName).add(key);
            }
        }

        PSParameter psParameter = new PSParameter();
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

        for (String spectrumFile : psmMap.keySet()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFile, new ArrayList<String>(psmMap.get(spectrumFile)), null, true, waitingHandler); //@TODO: make an assumptions iterator?

            while (psmIterator.hasNext()) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }

                SpectrumMatch spectrumMatch = psmIterator.next();

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }

                String spectrumKey = spectrumMatch.getKey();

                HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);

                for (int advocateId : assumptions.keySet()) {

                    HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateId);
                    ArrayList<Double> scores = new ArrayList<Double>(advocateAssumptions.keySet());
                    Collections.sort(scores);

                    for (double score : scores) {
                        for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(score)) {

                            boolean firstFeature = true;

                            if (indexes) {
                                if (linePrefix != null) {
                                    writer.write(linePrefix);
                                }
                                writer.write(line + "");
                                firstFeature = false;
                            }

                            for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : matchExportFeatures) {
                                if (!firstFeature) {
                                    writer.addSeparator();
                                } else {
                                    firstFeature = false;
                                }
                                psParameter = (PSParameter) assumption.getUrParam(psParameter);
                                String feature;
                                if (assumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    feature = getPeptideAssumptionFeature(identification, identificationFeaturesGenerator,
                                            shotgunProtocol, identificationParameters, keys, linePrefix, nSurroundingAA,
                                            peptideAssumption, spectrumKey, psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                                } else if (assumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    feature = getTagAssumptionFeature(identification, identificationFeaturesGenerator, shotgunProtocol,
                                            identificationParameters, keys, linePrefix, tagAssumption, spectrumKey, psParameter,
                                            identificationAlgorithmMatchesFeature, waitingHandler);
                                } else {
                                    throw new IllegalArgumentException("Spectrum identification assumption of type " + assumption.getClass() + " not supported.");
                                }
                                writer.write(feature);
                            }
                            writer.addSeparator();
                            if (fragmentSection != null) {
                                String fractionPrefix = "";
                                if (linePrefix != null) {
                                    fractionPrefix += linePrefix;
                                }
                                fractionPrefix += line + ".";
                                fragmentSection.writeSection(spectrumMatch.getKey(), assumption, shotgunProtocol, identificationParameters, fractionPrefix, null);
                            }
                            line++;
                            writer.newLine();
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a map of the modifications in a peptide. Modification name &gt;
     * sites.
     *
     * @param peptide the peptide
     * @param variablePtms if true, only variable PTMs are shown, false return
     * only the fixed PTMs
     *
     * @return the map of the modifications on a peptide sequence
     */
    private static HashMap<String, ArrayList<Integer>> getModMap(Peptide peptide, boolean variablePtms) {

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>(peptide.getNModifications());
        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                if ((variablePtms && modificationMatch.isVariable()) || (!variablePtms && !modificationMatch.isVariable())) {
                    if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                        modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                    }
                    modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                }
            }
        }

        return modMap;
    }

    /**
     * Writes the header of this section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        boolean firstColumn = true;
        for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : matchExportFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(identificationAlgorithmMatchesFeature.getTitle());
        }
        writer.newLine();
    }

    /**
     * Writes the feature associated to the match of the given peptide
     * assumption.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param shotgunProtocol information on the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param nSurroundingAA the number of surrounding amino acids to export
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
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     */
    public static String getPeptideAssumptionFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> keys, String linePrefix, int nSurroundingAA,
            PeptideAssumption peptideAssumption, String spectrumKey, PSParameter psParameter, PsIdentificationAlgorithmMatchesFeature exportFeature,
            WaitingHandler waitingHandler) throws IOException, SQLException,
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
                ArrayList<String> accessions = peptideAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
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
                accessions = peptideAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                for (String accession : accessions) {
                    if (descriptions.length() > 0) {
                        descriptions.append("; ");
                    }
                    descriptions.append(sequenceFactory.getHeader(accession).getDescription());
                }
                return descriptions.toString();
            case algorithm_confidence:
                return psParameter.getSearchEngineConfidence() + "";
            case algorithm_delta_confidence:
                Double delta = psParameter.getAlgorithmDeltaPEP();
                if (delta == null) {
                    return "Not available";
                }
                delta *= 100;
                return delta + "";
            case delta_confidence:
                delta = psParameter.getDeltaPEP();
                if (delta == null) {
                    return "Not available";
                }
                delta *= 100;
                return delta + "";
            case decoy:
                if (peptideAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {
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
                return peptideAssumption.getIsotopeNumber(precursor.getMz(), identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";
            case mz:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getMz() + "";
            case total_spectrum_intensity:
                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getTotalIntensity() + "";
            case max_intensity:
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getMaxIntensity() + "";
            case intensity_coverage:
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                double coveredIntensity = 0;
                Peptide peptide = peptideAssumption.getPeptide();
                AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
                SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                ArrayList<IonMatch> matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences,
                        (MSnSpectrum) spectrum, peptide);
                for (IonMatch ionMatch : matches) {
                    coveredIntensity += ionMatch.peak.intensity;
                }
                double coverage = 100 * coveredIntensity / spectrum.getTotalIntensity();
                return coverage + "";
            case mz_error_ppm:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return peptideAssumption.getDeltaMass(precursor.getMz(), true, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";
            case mz_error_da:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return peptideAssumption.getDeltaMass(precursor.getMz(), false, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";
            case rt:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getRt() + "";
            case algorithm_score:
                int id = peptideAssumption.getAdvocate();
                Double score = peptideAssumption.getRawScore();
                if (score == null) {
                    score = peptideAssumption.getScore();
                }
                return Advocate.getAdvocate(id).getName() + " (" + score + ")";
            case sequence:
                return peptideAssumption.getPeptide().getSequence();
            case aaBefore:
                peptide = peptideAssumption.getPeptide();
                accessions = peptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                String subSequence = "";
                for (String proteinAccession : accessions) {
                    if (!subSequence.equals("")) {
                        subSequence += "; ";
                    }
                    HashMap<Integer, String[]> surroundingAAs = SequenceFactory.getInstance().getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                            nSurroundingAA, identificationParameters.getSequenceMatchingPreferences());
                    ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            subSequence += ", ";
                        }
                        subSequence += surroundingAAs.get(startAa)[0];
                    }
                }
                return subSequence;
            case aaAfter:
                peptide = peptideAssumption.getPeptide();
                accessions = peptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                subSequence = "";
                for (String proteinAccession : accessions) {
                    if (!subSequence.equals("")) {
                        subSequence += "; ";
                    }
                    HashMap<Integer, String[]> surroundingAAs
                            = SequenceFactory.getInstance().getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                    nSurroundingAA, identificationParameters.getSequenceMatchingPreferences());
                    ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.keySet());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            subSequence += ", ";
                        }
                        subSequence += surroundingAAs.get(startAa)[1];
                    }
                }
                return subSequence;
            case position:
                accessions = peptideAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
                Collections.sort(accessions);
                String peptideSequence = peptideAssumption.getPeptide().getSequence();
                String start = "";
                for (String proteinAccession : accessions) {
                    if (!start.equals("")) {
                        start += "; ";
                    }
                    Protein protein = SequenceFactory.getInstance().getProtein(proteinAccession);
                    ArrayList<Integer> starts = protein.getPeptideStart(peptideSequence,
                            identificationParameters.getSequenceMatchingPreferences());
                    Collections.sort(starts);
                    boolean first = true;
                    for (int startAa : starts) {
                        if (first) {
                            first = false;
                        } else {
                            start += ", ";
                        }
                        start += startAa;
                    }
                }
                return start;
            case missed_cleavages:
                String sequence = peptideAssumption.getPeptide().getSequence();
                return Peptide.getNMissedCleavages(sequence, shotgunProtocol.getEnzyme()) + "";
            case modified_sequence:
                return peptideAssumption.getPeptide().getTaggedModifiedSequence(identificationParameters.getSearchParameters().getPtmSettings(), false, false, true) + "";
            case spectrum_charge:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString() + "";
            case spectrum_file:
                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                return spectrumFile;
            case spectrum_scan_number:
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getScanNumber();
            case spectrum_array_list:
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getPeakListAsString();
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
            case fragment_mz_accuracy_score:
                annotationPreferences = identificationParameters.getAnnotationPreferences();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                score = PsmScore.getDecreasingScore(peptideAssumption.getPeptide(), peptideAssumption.getIdentificationCharge().value,
                        (MSnSpectrum) SpectrumFactory.getInstance().getSpectrum(spectrumKey), shotgunProtocol,
                        identificationParameters, specificAnnotationPreferences, peptideSpectrumAnnotator, PsmScore.aa_ms2_mz_fidelity.index);
                return score + "";
            case intensity_score:
                annotationPreferences = identificationParameters.getAnnotationPreferences();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                score = PsmScore.getDecreasingScore(peptideAssumption.getPeptide(), peptideAssumption.getIdentificationCharge().value,
                        (MSnSpectrum) SpectrumFactory.getInstance().getSpectrum(spectrumKey), shotgunProtocol,
                        identificationParameters, specificAnnotationPreferences, peptideSpectrumAnnotator, PsmScore.aa_intensity.index);
                return score + "";
            case sequence_coverage:
                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationPreferences();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);
                int sequenceLength = peptide.getSequence().length();
                boolean[] aaCoverage = new boolean[sequenceLength];
                for (IonMatch ionMatch : matches) {
                    Ion ion = ionMatch.ion;
                    if (ion instanceof PeptideFragmentIon) {
                        PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ion;
                        int number = peptideFragmentIon.getNumber();
                        aaCoverage[number - 1] = true;
                    }
                }
                double nIons = 0.0;
                for (boolean aa : aaCoverage) {
                    if (aa) {
                        nIons += 1;
                    }
                }
                coverage = 100 * nIons / sequenceLength;
                return coverage + "";
            case longest_amino_acid_sequence_annotated:
                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationPreferences();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);
                sequence = peptide.getSequence();
                sequenceLength = sequence.length();
                boolean[] coverageForward = new boolean[sequenceLength];
                boolean[] coverageRewind = new boolean[sequenceLength];
                for (IonMatch ionMatch : matches) {
                    Ion ion = ionMatch.ion;
                    if (ion instanceof PeptideFragmentIon) {
                        PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ion;
                        int number = peptideFragmentIon.getNumber();
                        if (peptideFragmentIon.getSubType() == PeptideFragmentIon.A_ION
                                || peptideFragmentIon.getSubType() == PeptideFragmentIon.B_ION
                                || peptideFragmentIon.getSubType() == PeptideFragmentIon.C_ION) {
                            coverageForward[number - 1] = true;
                        } else {
                            coverageRewind[number - 1] = true;
                        }
                    }
                }
                aaCoverage = new boolean[sequenceLength];
                boolean previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = coverageForward[aaIndex];
                    if (current && previous) {
                        aaCoverage[aaIndex] = true;
                    }
                    previous = current;
                }
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = coverageRewind[aaIndex];
                    if (current && previous) {
                        aaCoverage[sequenceLength - aaIndex - 1] = true;
                    }
                    previous = current;
                }
                StringBuilder currentTag = new StringBuilder();
                String longestTag = new String();
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    if (aaCoverage[aaIndex]) {
                        currentTag.append(sequence.charAt(aaIndex));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.toString();
                        }
                        currentTag = new StringBuilder();
                    }
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.toString();
                }
                return longestTag;
            case longest_amino_acid_sequence_annotated_single_serie:
                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationPreferences();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);
                sequence = peptide.getSequence();
                sequenceLength = sequence.length();
                HashMap<Integer, boolean[]> ionCoverage = new HashMap<Integer, boolean[]>(6);
                ionCoverage.put(PeptideFragmentIon.A_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.B_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.C_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.X_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.Y_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.Z_ION, new boolean[sequenceLength]);
                for (IonMatch ionMatch : matches) {
                    if (ionMatch.charge.value == 1) {
                        Ion ion = ionMatch.ion;
                        if (ion instanceof PeptideFragmentIon) {
                            PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ion;
                            int number = peptideFragmentIon.getNumber();
                            if (peptideFragmentIon.getSubType() == PeptideFragmentIon.A_ION && !peptideFragmentIon.hasNeutralLosses()) {
                                ionCoverage.get(PeptideFragmentIon.A_ION)[number - 1] = true;
                            } else if (peptideFragmentIon.getSubType() == PeptideFragmentIon.B_ION && !peptideFragmentIon.hasNeutralLosses()) {
                                ionCoverage.get(PeptideFragmentIon.B_ION)[number - 1] = true;
                            } else if (peptideFragmentIon.getSubType() == PeptideFragmentIon.C_ION && !peptideFragmentIon.hasNeutralLosses()) {
                                ionCoverage.get(PeptideFragmentIon.C_ION)[number - 1] = true;
                            } else if (peptideFragmentIon.getSubType() == PeptideFragmentIon.X_ION && !peptideFragmentIon.hasNeutralLosses()) {
                                ionCoverage.get(PeptideFragmentIon.X_ION)[number - 1] = true;
                            } else if (peptideFragmentIon.getSubType() == PeptideFragmentIon.Y_ION && !peptideFragmentIon.hasNeutralLosses()) {
                                ionCoverage.get(PeptideFragmentIon.Y_ION)[number - 1] = true;
                            } else if (peptideFragmentIon.getSubType() == PeptideFragmentIon.Z_ION && !peptideFragmentIon.hasNeutralLosses()) {
                                ionCoverage.get(PeptideFragmentIon.Z_ION)[number - 1] = true;
                            }
                        }
                    }
                }
                longestTag = new String();
                currentTag = new StringBuilder();
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = ionCoverage.get(PeptideFragmentIon.A_ION)[aaIndex];
                    if (current && previous) {
                        currentTag.append(sequence.charAt(aaIndex));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.toString();
                        }
                        currentTag = new StringBuilder();
                    }
                    previous = current;
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.reverse().toString();
                }
                currentTag = new StringBuilder();
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = ionCoverage.get(PeptideFragmentIon.B_ION)[aaIndex];
                    if (current && previous) {
                        currentTag.append(sequence.charAt(aaIndex));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.toString();
                        }
                        currentTag = new StringBuilder();
                    }
                    previous = current;
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.reverse().toString();
                }
                currentTag = new StringBuilder();
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = ionCoverage.get(PeptideFragmentIon.C_ION)[aaIndex];
                    if (current && previous) {
                        currentTag.append(sequence.charAt(aaIndex));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.toString();
                        }
                        currentTag = new StringBuilder();
                    }
                    previous = current;
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.reverse().toString();
                }
                currentTag = new StringBuilder();
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = ionCoverage.get(PeptideFragmentIon.X_ION)[aaIndex];
                    if (current && previous) {
                        currentTag.append(sequence.charAt(sequenceLength - aaIndex - 1));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.reverse().toString();
                        }
                        currentTag = new StringBuilder();
                    }
                    previous = current;
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.reverse().toString();
                }
                currentTag = new StringBuilder();
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = ionCoverage.get(PeptideFragmentIon.Y_ION)[aaIndex];
                    if (current && previous) {
                        currentTag.append(sequence.charAt(sequenceLength - aaIndex - 1));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.reverse().toString();
                        }
                        currentTag = new StringBuilder();
                    }
                    previous = current;
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.reverse().toString();
                }
                currentTag = new StringBuilder();
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = ionCoverage.get(PeptideFragmentIon.Z_ION)[aaIndex];
                    if (current && previous) {
                        currentTag.append(sequence.charAt(sequenceLength - aaIndex - 1));
                    } else {
                        if (currentTag.length() > longestTag.length()) {
                            longestTag = currentTag.reverse().toString();
                        }
                        currentTag = new StringBuilder();
                    }
                    previous = current;
                }
                if (currentTag.length() > longestTag.length()) {
                    longestTag = currentTag.reverse().toString();
                }

                return longestTag;
            case amino_acids_annotated:
                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationPreferences();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);
                sequence = peptide.getSequence();
                sequenceLength = sequence.length();
                coverageForward = new boolean[sequenceLength];
                coverageRewind = new boolean[sequenceLength];
                for (IonMatch ionMatch : matches) {
                    Ion ion = ionMatch.ion;
                    if (ion instanceof PeptideFragmentIon) {
                        PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ion;
                        int number = peptideFragmentIon.getNumber();
                        if (peptideFragmentIon.getSubType() == PeptideFragmentIon.A_ION
                                || peptideFragmentIon.getSubType() == PeptideFragmentIon.B_ION
                                || peptideFragmentIon.getSubType() == PeptideFragmentIon.C_ION) {
                            coverageForward[number - 1] = true;
                        } else {
                            coverageRewind[number - 1] = true;
                        }
                    }
                }
                aaCoverage = new boolean[sequenceLength];
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = coverageForward[aaIndex];
                    if (current && previous) {
                        aaCoverage[aaIndex] = true;
                    }
                    previous = current;
                }
                previous = true;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    boolean current = coverageRewind[aaIndex];
                    if (current && previous) {
                        aaCoverage[sequenceLength - aaIndex - 1] = true;
                    }
                    previous = current;
                }
                StringBuilder tag = new StringBuilder();
                double gap = 0;
                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {
                    if (aaCoverage[aaIndex]) {
                        if (gap > 0) {
                            tag.append("<").append(gap).append(">");
                        }
                        tag.append(sequence.charAt(aaIndex));
                        gap = 0;
                    } else {
                        gap += AminoAcid.getAminoAcid(sequence.charAt(aaIndex)).getMonoisotopicMass();
                    }
                }
                if (gap > 0) {
                    tag.append("<").append(gap).append(">");
                }
                return tag.toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the feature associated to the match of the given tag assumption.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
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
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     */
    public static String getTagAssumptionFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> keys, String linePrefix,
            TagAssumption tagAssumption, String spectrumKey, PSParameter psParameter, PsIdentificationAlgorithmMatchesFeature exportFeature,
            WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

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
                return psParameter.getSearchEngineConfidence() + "";
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
                return tagAssumption.getIsotopeNumber(precursor.getMz(), identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";
            case mz:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getMz() + "";
            case total_spectrum_intensity:
                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getTotalIntensity() + "";
            case max_intensity:
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return spectrum.getMaxIntensity() + "";
            case mz_error_ppm:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return tagAssumption.getDeltaMass(precursor.getMz(), true, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";
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
                return tagAssumption.getTag().getTaggedModifiedSequence(identificationParameters.getSearchParameters().getPtmSettings(), false, false, true, false);
            case spectrum_charge:
                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString() + "";
            case spectrum_file:
                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                return spectrumFile;
            case spectrum_scan_number:
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getScanNumber();
            case spectrum_array_list:
                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getPeakListAsString();
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
            case fragment_mz_accuracy_score:
            case intensity_score:
            case sequence_coverage:
            case longest_amino_acid_sequence_annotated:
            case amino_acids_annotated:
            case position:
                return "";
            default:
                return "Not implemented";
        }
    }
}
