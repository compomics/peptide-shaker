package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.aminoacids.AminoAcid;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final ArrayList<PsIdentificationAlgorithmMatchesFeature> matchExportFeatures = new ArrayList<>();
    /**
     * The fragment subsection if needed.
     */
    private PsFragmentSection fragmentSection = null;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private final boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private final boolean header;
    /**
     * The writer used to send the output to file.
     */
    private final ExportWriter writer;
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
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<>();
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
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param identificationParameters the identification parameters
     * @param keys the keys of the spectrum matches to output
     * @param linePrefix the line prefix
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, IdentificationParameters identificationParameters, long[] keys,
            String linePrefix, int nSurroundingAA, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        PSParameter psParameter = new PSParameter();
        int line = 1;

        int totalSize = identification.getNumber(SpectrumMatch.class);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(totalSize);
        }

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptions = spectrumMatch.getPeptideAssumptionsMap();

            for (int advocateId : peptideAssumptions.keySet()) {

                TreeMap<Double, ArrayList<PeptideAssumption>> advocateAssumptions = peptideAssumptions.get(advocateId);
                ArrayList<Double> scores = new ArrayList<>(advocateAssumptions.keySet());
                Collections.sort(scores);

                for (double score : scores) {

                    for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(score)) {

                        boolean firstFeature = true;

                        if (indexes) {

                            if (linePrefix != null) {

                                writer.write(linePrefix);

                            }

                            writer.write(Integer.toString(line));
                            firstFeature = false;

                        }

                        for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : matchExportFeatures) {

                            if (!firstFeature) {

                                writer.addSeparator();

                            } else {

                                firstFeature = false;

                            }

                            psParameter = (PSParameter) assumption.getUrParam(psParameter);
                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                            String feature = getPeptideAssumptionFeature(identification, identificationFeaturesGenerator,
                                    sequenceProvider, proteinDetailsProvider,
                                    identificationParameters, linePrefix, nSurroundingAA,
                                    peptideAssumption, spectrumMatch.getSpectrumKey(), psParameter,
                                    identificationAlgorithmMatchesFeature, waitingHandler);
                            writer.write(feature);

                        }

                        writer.addSeparator();

                        if (fragmentSection != null) {

                            String fractionPrefix = "";

                            if (linePrefix != null) {

                                fractionPrefix += linePrefix;

                            }

                            fractionPrefix += line + ".";
                            fragmentSection.writeSection(spectrumMatch.getSpectrumKey(), assumption, sequenceProvider, identificationParameters, fractionPrefix, null);

                        }

                        line++;
                        writer.newLine();

                    }
                }
            }

            HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> tagAssumptions = spectrumMatch.getTagAssumptionsMap();

            for (int advocateId : tagAssumptions.keySet()) {

                TreeMap<Double, ArrayList<TagAssumption>> advocateAssumptions = tagAssumptions.get(advocateId);
                ArrayList<Double> scores = new ArrayList<>(advocateAssumptions.keySet());
                Collections.sort(scores);

                for (double score : scores) {

                    for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(score)) {

                        boolean firstFeature = true;

                        if (indexes) {

                            if (linePrefix != null) {

                                writer.write(linePrefix);

                            }

                            writer.write(Integer.toString(line));
                            firstFeature = false;

                        }

                        for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : matchExportFeatures) {

                            if (!firstFeature) {

                                writer.addSeparator();

                            } else {

                                firstFeature = false;

                            }

                            psParameter = (PSParameter) assumption.getUrParam(psParameter);
                            TagAssumption tagAssumption = (TagAssumption) assumption;
                            String feature = getTagAssumptionFeature(identification, identificationFeaturesGenerator,
                                    identificationParameters, linePrefix, tagAssumption, spectrumMatch.getSpectrumKey(), psParameter,
                                    identificationAlgorithmMatchesFeature, waitingHandler);
                            writer.write(feature);

                        }

                        writer.addSeparator();

                        if (fragmentSection != null) {

                            String fractionPrefix = "";

                            if (linePrefix != null) {

                                fractionPrefix += linePrefix;

                            }

                            fractionPrefix += line + ".";
                            fragmentSection.writeSection(spectrumMatch.getSpectrumKey(), assumption, sequenceProvider, identificationParameters, fractionPrefix, null);

                        }

                        line++;
                        writer.newLine();

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
     * @param modificationParameters the modification parameters
     * @param sequenceProvider the sequence provider
     * @param modificationSequenceMatchingParameters the modification sequence
     * matching parameters
     * @param variablePtms if true, only variable PTMs are shown, false return
     * only the fixed PTMs
     *
     * @return the map of the modifications on a peptide sequence
     */
    private static TreeMap<String, TreeSet<Integer>> getModMap(Peptide peptide, ModificationParameters modificationParameters, SequenceProvider sequenceProvider, SequenceMatchingParameters modificationSequenceMatchingParameters, boolean variablePtms) {

        if (variablePtms) {

            ModificationMatch[] modificationMatches = peptide.getVariableModifications();

            return modificationMatches == null ? new TreeMap<>()
                    : Arrays.stream(modificationMatches)
                            .collect(Collectors.groupingBy(ModificationMatch::getModification,
                                    TreeMap::new,
                                    Collectors.mapping(ModificationMatch::getSite,
                                            Collectors.toCollection(TreeSet::new))));

        } else {

            String[] fixedModifications = peptide.getFixedModifications(modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);

            return IntStream.range(0, fixedModifications.length)
                    .mapToObj(i -> fixedModifications[i])
                    .filter(modName -> modName != null)
                    .collect(Collectors.groupingBy(Function.identity(),
                            TreeMap::new,
                            Collectors.mapping(i -> new Integer(i),
                                    Collectors.toCollection(TreeSet::new))));

        }

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
     * @param sequenceProvider a provider for the protein sequences
     * @param proteinDetailsProvider a provider for protein details
     * @param identificationParameters the identification parameters
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
     */
    public static String getPeptideAssumptionFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, IdentificationParameters identificationParameters, String linePrefix, int nSurroundingAA,
            PeptideAssumption peptideAssumption, String spectrumKey, PSParameter psParameter, PsIdentificationAlgorithmMatchesFeature exportFeature,
            WaitingHandler waitingHandler) {

        switch (exportFeature) {
            case rank:

                return Integer.toString(peptideAssumption.getRank());

            case variable_ptms:

                ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                TreeMap<String, TreeSet<Integer>> modMap = getModMap(peptideAssumption.getPeptide(), modificationParameters, sequenceProvider, modificationSequenceMatchingParameters, true);

                return modMap.entrySet().stream()
                        .map(entry -> getModificationAsString(entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(";"));

            case fixed_ptms:

                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                modMap = getModMap(peptideAssumption.getPeptide(), modificationParameters, sequenceProvider, modificationSequenceMatchingParameters, false);

                return modMap.entrySet().stream()
                        .map(entry -> getModificationAsString(entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(";"));

            case accessions:

                TreeMap<String, int[]> proteinMapping = peptideAssumption.getPeptide().getProteinMapping();

                return proteinMapping.navigableKeySet().stream()
                        .collect(Collectors.joining(","));

            case protein_description:

                proteinMapping = peptideAssumption.getPeptide().getProteinMapping();

                return proteinMapping.navigableKeySet().stream()
                        .map(accession -> proteinDetailsProvider.getDescription(accession))
                        .collect(Collectors.joining(","));

            case algorithm_confidence:

                return Double.toString(psParameter.getConfidence());

            case algorithm_delta_confidence:

                Double delta = psParameter.getAlgorithmDeltaPEP();

                return delta == null ? "Not available" : Double.toString(100 * delta);

            case delta_confidence:

                delta = psParameter.getDeltaPEP();

                return delta == null ? "Not available" : Double.toString(100 * delta);

            case decoy:

                return PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider) ? "1" : "0";

            case hidden:

                return psParameter.getHidden() ? "1" : "0";

            case identification_charge:

                return Integer.toString(peptideAssumption.getIdentificationCharge());

            case isotope:

                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return peptideAssumption.getIsotopeNumber(precursor.getMz(), identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";

            case mz:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return Double.toString(precursor.getMz());

            case total_spectrum_intensity:

                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return Double.toString(spectrum.getTotalIntensity());

            case max_intensity:

                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return Double.toString(spectrum.getMaxIntensity());

            case intensity_coverage:

                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                Peptide peptide = peptideAssumption.getPeptide();
                AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();
                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                IonMatch[] matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences,
                        spectrum, peptide, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                double coveredIntensity = Arrays.stream(matches)
                        .mapToDouble(ionMatch -> ionMatch.peak.intensity)
                        .sum();
                double coverage = 100 * coveredIntensity / spectrum.getTotalIntensity();
                return Double.toString(coverage);

            case mz_error_ppm:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return Double.toString(peptideAssumption.getDeltaMass(precursor.getMz(), true,
                        identificationParameters.getSearchParameters().getMinIsotopicCorrection(),
                        identificationParameters.getSearchParameters().getMaxIsotopicCorrection()));

            case mz_error_da:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return Double.toString(peptideAssumption.getDeltaMass(precursor.getMz(), false,
                        identificationParameters.getSearchParameters().getMinIsotopicCorrection(),
                        identificationParameters.getSearchParameters().getMaxIsotopicCorrection()));

            case rt:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return Double.toString(precursor.getRt());

            case algorithm_score:

                int id = peptideAssumption.getAdvocate();
                double score = peptideAssumption.getRawScore();
                return Advocate.getAdvocate(id).getName() + " (" + score + ")";

            case sequence:

                return peptideAssumption.getPeptide().getSequence();

            case aaBefore:

                TreeMap<String, String[]> aaMap = PeptideUtils.getAaBefore(peptideAssumption.getPeptide(), nSurroundingAA, sequenceProvider);

                return aaMap.values().stream()
                        .map(aas -> (Arrays.stream(aas))
                        .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";"));

            case aaAfter:

                aaMap = PeptideUtils.getAaAfter(peptideAssumption.getPeptide(), nSurroundingAA, sequenceProvider);

                return aaMap.values().stream()
                        .map(aas -> (Arrays.stream(aas))
                        .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";"));

            case position:

                proteinMapping = peptideAssumption.getPeptide().getProteinMapping();

                return proteinMapping.values().stream()
                        .map(positions -> (Arrays.stream(positions))
                        .mapToObj(pos -> Integer.toString(pos))
                        .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";"));

            case missed_cleavages:

                peptide = peptideAssumption.getPeptide();
                int nMissedCleavages = peptide.getNMissedCleavages(identificationParameters.getSearchParameters().getDigestionParameters());
                return Integer.toString(nMissedCleavages);

            case modified_sequence:

                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                HashSet<String> modToExport = new HashSet(modificationParameters.getVariableModifications());
                return peptideAssumption.getPeptide().getTaggedModifiedSequence(modificationParameters, sequenceProvider, modificationSequenceMatchingParameters, false, false, true, modToExport);

            case spectrum_charge:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString();

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

                return psParameter.getStarred() ? "1" : "0";

            case theoretical_mass:

                return Double.toString(peptideAssumption.getPeptide().getMass());

            case validated:

                return psParameter.getMatchValidationLevel().toString();

            case sequence_coverage:

                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationParameters();
                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, spectrum, peptide, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                int sequenceLength = peptide.getSequence().length();
                int[] aaCoverage = new int[sequenceLength];

                Arrays.stream(matches)
                        .filter(ionMatch -> ionMatch.ion instanceof PeptideFragmentIon)
                        .forEach(ionMatch -> aaCoverage[((PeptideFragmentIon) ionMatch.ion).getNumber() - 1] = 1);

                double nIons = Arrays.stream(aaCoverage).sum();
                coverage = 100 * nIons / sequenceLength;

                return Double.toString(coverage);

            case longest_amino_acid_sequence_annotated:

                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationParameters();
                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, spectrum, peptide, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                String sequence = peptide.getSequence();
                sequenceLength = sequence.length();
                boolean[] coverageForward = new boolean[sequenceLength];
                boolean[] coverageRewind = new boolean[sequenceLength];

                Arrays.stream(matches)
                        .filter(ionMatch -> ionMatch.ion instanceof PeptideFragmentIon)
                        .map(ionMatch -> ((PeptideFragmentIon) ionMatch.ion))
                        .forEach(peptideFragmentIon -> {
                            if (PeptideFragmentIon.isForward(peptideFragmentIon.getSubType())) {
                                coverageForward[peptideFragmentIon.getNumber() - 1] = true;
                            } else {
                                coverageRewind[peptideFragmentIon.getNumber() - 1] = true;
                            }
                        });

                boolean[] aaCoverageB = new boolean[sequenceLength];
                boolean previous = true;

                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {

                    boolean current = coverageForward[aaIndex];

                    if (current && previous) {

                        aaCoverageB[aaIndex] = true;

                    }

                    previous = current;

                }

                previous = true;

                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {

                    boolean current = coverageRewind[aaIndex];

                    if (current && previous) {

                        aaCoverageB[sequenceLength - aaIndex - 1] = true;

                    }

                    previous = current;

                }

                StringBuilder currentTag = new StringBuilder();
                String longestTag = new String();

                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {

                    if (aaCoverageB[aaIndex]) {

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
                annotationPreferences = identificationParameters.getAnnotationParameters();
                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, spectrum, peptide, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                sequence = peptide.getSequence();
                sequenceLength = sequence.length();
                HashMap<Integer, boolean[]> ionCoverage = new HashMap<>(6);
                ionCoverage.put(PeptideFragmentIon.A_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.B_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.C_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.X_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.Y_ION, new boolean[sequenceLength]);
                ionCoverage.put(PeptideFragmentIon.Z_ION, new boolean[sequenceLength]);

                Arrays.stream(matches)
                        .filter(ionMatch -> ionMatch.charge == 1 && ionMatch.ion instanceof PeptideFragmentIon && !ionMatch.ion.hasNeutralLosses())
                        .map(ionMatch -> ((PeptideFragmentIon) ionMatch.ion))
                        .forEach(peptideFragmentIon -> ionCoverage.get(peptideFragmentIon.getSubType())[peptideFragmentIon.getNumber() - 1] = true);

                longestTag = new String();
                currentTag = new StringBuilder();
                previous = true;

                for (int ionType : PeptideFragmentIon.getPossibleSubtypes()) {

                    for (int i = 0; i < sequenceLength; i++) {

                        int aaIndex = PeptideFragmentIon.isForward(ionType) ? i : sequence.length() - i - 1;

                        boolean current = ionCoverage.get(ionType)[i];

                        if (current && previous) {

                            currentTag.append(sequence.charAt(aaIndex));

                        } else {

                            if (currentTag.length() > longestTag.length()) {

                                if (PeptideFragmentIon.isForward(ionType)) {

                                    currentTag.reverse();

                                }

                                longestTag = currentTag.toString();

                            }

                            currentTag = new StringBuilder();

                        }

                        previous = current;

                    }

                    if (currentTag.length() > longestTag.length()) {

                        if (PeptideFragmentIon.isForward(ionType)) {

                            currentTag.reverse();

                        }

                        longestTag = currentTag.toString();
                    }
                }

                return longestTag;

            case amino_acids_annotated:

                peptide = peptideAssumption.getPeptide();
                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                annotationPreferences = identificationParameters.getAnnotationParameters();
                modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, spectrum, peptide, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);
                sequence = peptide.getSequence();
                sequenceLength = sequence.length();
                coverageForward = new boolean[sequenceLength];
                coverageRewind = new boolean[sequenceLength];

                Arrays.stream(matches)
                        .filter(ionMatch -> ionMatch.ion instanceof PeptideFragmentIon)
                        .map(ionMatch -> ((PeptideFragmentIon) ionMatch.ion))
                        .forEach(peptideFragmentIon -> {
                            if (PeptideFragmentIon.isForward(peptideFragmentIon.getSubType())) {
                                coverageForward[peptideFragmentIon.getNumber() - 1] = true;
                            } else {
                                coverageRewind[peptideFragmentIon.getNumber() - 1] = true;
                            }
                        });

                aaCoverageB = new boolean[sequenceLength];
                previous = true;

                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {

                    boolean current = coverageForward[aaIndex];

                    if (current && previous) {

                        aaCoverageB[aaIndex] = true;

                    }

                    previous = current;

                }

                previous = true;

                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {

                    boolean current = coverageRewind[aaIndex];

                    if (current && previous) {

                        aaCoverageB[sequenceLength - aaIndex - 1] = true;

                    }

                    previous = current;

                }

                StringBuilder tag = new StringBuilder();
                double gap = 0;

                for (int aaIndex = 0; aaIndex < sequenceLength; aaIndex++) {

                    if (aaCoverageB[aaIndex]) {

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
     * Returns a string containing a modification name and modification sites.
     *
     * @param modification the modification name
     * @param location the locations as a list
     *
     * @return a string containing a modification name and modification sites
     */
    private static String getModificationAsString(String modification, TreeSet<Integer> location) {

        StringBuilder sb = new StringBuilder(modification.length() + 2 * location.size() + 2);

        sb.append(modification).append(" (").append(location.stream().map(site -> site.toString()).collect(Collectors.joining(","))).append(")");

        return sb.toString();

    }

    /**
     * Writes the feature associated to the match of the given tag assumption.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param identificationParameters the identification parameters
     * @param linePrefix the line prefix
     * @param spectrumKey the key of the spectrum
     * @param tagAssumption the assumption for the match to inspect
     * @param psParameter the PeptideShaker parameter of the match
     * @param exportFeature the feature to export
     * @param waitingHandler the waiting handler
     *
     * @return the content corresponding to the given feature of the current
     * section
     */
    public static String getTagAssumptionFeature(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, String linePrefix,
            TagAssumption tagAssumption, String spectrumKey, PSParameter psParameter, PsIdentificationAlgorithmMatchesFeature exportFeature,
            WaitingHandler waitingHandler) {

        switch (exportFeature) {

            case rank:

                return Integer.toString(tagAssumption.getRank());

            case variable_ptms:

                return Tag.getTagModificationsAsString(tagAssumption.getTag());

            case fixed_ptms:

                return ""; //@TODO: impplement

            case accessions:

                return "";

            case protein_description:

                return "";

            case algorithm_confidence:

                return Double.toString(psParameter.getConfidence());

            case decoy:

                return "";

            case hidden:

                return psParameter.getHidden() ? "1" : "0";

            case identification_charge:

                return Integer.toString(tagAssumption.getIdentificationCharge());

            case isotope:

                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return tagAssumption.getIsotopeNumber(precursor.getMz(), identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";

            case mz:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return Double.toString(precursor.getMz());

            case total_spectrum_intensity:

                Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return Double.toString(spectrum.getTotalIntensity());

            case max_intensity:

                spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
                return Double.toString(spectrum.getMaxIntensity());

            case mz_error_ppm:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return tagAssumption.getDeltaMass(precursor.getMz(), true, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection()) + "";

            case rt:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return Double.toString(precursor.getRt());

            case algorithm_score:

                int id = tagAssumption.getAdvocate();
                double score = tagAssumption.getScore();
                return Advocate.getAdvocate(id).getName() + " (" + score + ")";

            case sequence:

                return tagAssumption.getTag().asSequence();

            case missed_cleavages:

                return "";

            case modified_sequence:

                ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                HashSet<String> modToExport = new HashSet(modificationParameters.getVariableModifications());
                return tagAssumption.getTag().getTaggedModifiedSequence(modificationParameters, false, false, true, false, modificationSequenceMatchingParameters, modToExport);

            case spectrum_charge:

                precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                return precursor.getPossibleChargesAsString();

            case spectrum_file:

                return Spectrum.getSpectrumFile(spectrumKey);

            case spectrum_scan_number:

                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getScanNumber();

            case spectrum_array_list:

                return SpectrumFactory.getInstance().getSpectrum(spectrumKey).getPeakListAsString();

            case spectrum_title:

                return Spectrum.getSpectrumTitle(spectrumKey);

            case starred:

                return psParameter.getStarred() ? "1" : "0";

            case theoretical_mass:

                return Double.toString(tagAssumption.getTag().getMass());

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
