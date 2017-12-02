package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.units.MetricsPrefix;
import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMatch;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.units.StandardUnit;
import com.compomics.util.experiment.units.UnitOfMeasurement;
import com.compomics.util.math.statistics.Distribution;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSModificationScores;
import eu.isas.peptideshaker.preferences.FilterParameters;
import eu.isas.peptideshaker.preferences.SpectrumCountingParameters;
import eu.isas.peptideshaker.preferences.SpectrumCountingParameters.SpectralCountingMethod;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This class provides identification features and stores them in cache.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class IdentificationFeaturesGenerator {

    // @TODO: move to utilities once the back-end allows it
    /**
     * The modification factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The identification features cache where the recently accessed
     * identification features are stored
     */
    private IdentificationFeaturesCache identificationFeaturesCache = new IdentificationFeaturesCache();
    /**
     * The metrics picked-up wile loading the data.
     */
    private final Metrics metrics;
    /**
     * The identification of interest.
     */
    private final Identification identification;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The spectrum counting preferences.
     */
    private SpectrumCountingParameters spectrumCountingPreferences;
    /**
     * Map of the distributions of precursor mass errors.
     */
    private HashMap<String, NonSymmetricalNormalDistribution> massErrorDistribution = null;

    /**
     * Constructor.
     *
     * @param identification an identification object allowing retrieving
     * matches and parameters
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     * @param metrics the metrics picked-up wile loading the data
     * @param spectrumCountingPreferences the spectrum counting preferences
     */
    public IdentificationFeaturesGenerator(Identification identification, IdentificationParameters identificationParameters, SequenceProvider sequenceProvider,
            Metrics metrics, SpectrumCountingParameters spectrumCountingPreferences) {

        this.metrics = metrics;
        this.sequenceProvider = sequenceProvider;
        this.identificationParameters = identificationParameters;
        this.identification = identification;
        this.spectrumCountingPreferences = spectrumCountingPreferences;

    }

    /**
     * Sets a mass error distribution in the massErrorDistribution map.
     *
     * @param spectrumFile the spectrum file of interest
     * @param precursorMzDeviations list of precursor mass errors
     */
    public void setMassErrorDistribution(String spectrumFile, ArrayList<Double> precursorMzDeviations) {

        if (massErrorDistribution == null) {

            massErrorDistribution = new HashMap<>(1);

        }

        NonSymmetricalNormalDistribution distribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistributionFromSortedList(precursorMzDeviations);
        massErrorDistribution.put(spectrumFile, distribution);

    }

    /**
     * Returns the precursor mass error distribution of validated peptides in a
     * spectrum file.
     *
     * @param spectrumFile the name of the file of interest
     *
     * @return the precursor mass error distribution of validated peptides in a
     * spectrum file
     */
    public NonSymmetricalNormalDistribution getMassErrorDistribution(String spectrumFile) {

        if (massErrorDistribution == null || massErrorDistribution.get(spectrumFile) == null) {

            estimateMassErrorDistribution(spectrumFile);

        }

        return massErrorDistribution.get(spectrumFile);

    }

    /**
     * Estimates the precursor mass errors of validated peptides in a file and
     * sets in in the massErrorDistribution map.
     *
     * @param spectrumFile the spectrum file of interest
     */
    private void estimateMassErrorDistribution(String spectrumFile) {

        HashSet<Long> spectrumMatchesKeys = identification.getSpectrumIdentification().get(spectrumFile);
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        ArrayList<Double> precursorMzDeviations = spectrumMatchesKeys.stream()
                .map(key -> identification.getSpectrumMatch(key))
                .filter(spectrumMatch -> spectrumMatch.getBestPeptideAssumption() != null
                && ((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy)).getMatchValidationLevel().isValidated())
                .map(spectrumMatch -> spectrumMatch.getBestPeptideAssumption().getDeltaMass(
                spectrumFactory.getPrecursorMz(spectrumMatch.getSpectrumKey()),
                searchParameters.isPrecursorAccuracyTypePpm(),
                searchParameters.getMinIsotopicCorrection(),
                searchParameters.getMaxIsotopicCorrection()))
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        setMassErrorDistribution(spectrumFile, precursorMzDeviations);

    }

    /**
     * Returns an array of the likelihood to find identify a given amino acid in
     * the protein sequence. 0 is the first amino acid.
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @return an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides
     */
    public double[] getCoverableAA(long proteinMatchKey) {

        double[] result = (double[]) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.coverable_AA_p, proteinMatchKey);

        if (result == null) {

            result = estimateCoverableAA(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.coverable_AA_p, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Indicates the validation level of every amino acid in the given protein.
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @return an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides
     */
    public int[] getAACoverage(long proteinMatchKey) {

        int[] result = (int[]) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.AA_coverage, proteinMatchKey);

        if (result == null) {

            result = estimateAACoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.AA_coverage, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Updates the array of booleans indicating whether the amino acids of given
     * peptides can generate peptides. Used when the main key for a protein has
     * been altered.
     *
     * @param proteinMatchKey the key of the protein of interest
     */
    public void updateCoverableAA(long proteinMatchKey) {

        double[] result = estimateCoverableAA(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.coverable_AA_p, proteinMatchKey, result);

    }

    /**
     * Returns the variable modifications found in the currently loaded dataset.
     *
     * @return the variable modifications found in the currently loaded dataset
     */
    public TreeSet<String> getFoundModifications() {

        if (metrics == null) {

            return new TreeSet<>();

        }

        return metrics.getFoundModifications();

    }

    /**
     * Estimates the sequence coverage for the given protein match according to
     * the validation level: validation level &gt; share of the sequence
     * uniquely covered by this validation level.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the sequence coverage
     */
    private HashMap<Integer, Double> estimateSequenceCoverage(long proteinMatchKey) {

        int[] aaCoverage = getAACoverage(proteinMatchKey);
        HashMap<Integer, Double> result = new HashMap<>();

        for (int validationLevel : MatchValidationLevel.getValidationLevelIndexes()) {

            result.put(validationLevel, 0.0);

        }

        for (int validationLevel : aaCoverage) {

            result.put(validationLevel, result.get(validationLevel) + 1);

        }

        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinMatchKey);
        String sequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());

        for (int validationLevel : MatchValidationLevel.getValidationLevelIndexes()) {

            result.put(validationLevel, result.get(validationLevel) / sequence.length());

        }

        return result;
    }

    /**
     * Estimates the sequence coverage for the given protein match using the
     * validated peptides only.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the sequence coverage
     */
    private Double estimateValidatedSequenceCoverage(long proteinMatchKey) {

        int[] aaCoverage = getAACoverage(proteinMatchKey);

        double nAAValidated = (double) Arrays.stream(aaCoverage)
                .filter(validationLevel -> validationLevel == MatchValidationLevel.doubtful.getIndex()
                || validationLevel == MatchValidationLevel.confident.getIndex())
                .count();

        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinMatchKey);
        String sequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());

        return nAAValidated / sequence.length();

    }

    /**
     * Returns amino acid coverage of this protein by enzymatic or non-enzymatic
     * peptides only in an array where the index of the best validation level of
     * every peptide covering a given amino acid is given. 0 is the first amino
     * acid.
     *
     * @param proteinMatchKey the key of the protein match
     * @param enzymatic if not all peptides are considered, if true only
     * enzymatic peptides will be considered, if false only non enzymatic
     *
     * @return the identification coverage of the protein sequence
     */
    public int[] estimateAACoverage(long proteinMatchKey, boolean enzymatic) {

        return estimateAACoverage(proteinMatchKey, false, enzymatic);

    }

    /**
     * Returns amino acid coverage of this protein by all peptides or by
     * enzymatic or non-enzymatic peptides only in an array where the index of
     * the best validation level of every peptide covering a given amino acid is
     * given. 0 is the first amino acid.
     *
     * @param proteinMatchKey the key of the protein match
     * @param allPeptides indicates whether all peptides should be taken into
     * account
     * @param enzymatic if not all peptides are considered, if true only
     * enzymatic peptides will be considered, if false only non enzymatic
     *
     * @return the identification coverage of the protein sequence
     */
    private int[] estimateAACoverage(long proteinMatchKey, boolean allPeptides, boolean enzymatic) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String accession = proteinMatch.getLeadingAccession();
        String sequence = sequenceProvider.getSequence(accession);

        HashMap<Integer, HashSet<Integer>> coverage = new HashMap<>();

        // iterate the peptides and store the coverage for each peptide validation level
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            Peptide peptide = peptideMatch.getPeptide();
            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
            boolean enzymaticPeptide = false;

            if (!allPeptides) {

                DigestionParameters digestionParameters = identificationParameters.getSearchParameters().getDigestionParameters();

                if (digestionParameters.getCleavagePreference() == DigestionParameters.CleavagePreference.enzyme) {

                    enzymaticPeptide = PeptideUtils.isEnzymatic(peptide, accession, sequence, digestionParameters.getEnzymes());

                }
            }

            if (allPeptides || enzymatic && enzymaticPeptide || !enzymatic && !enzymaticPeptide) {

                int validationLevel = psParameter.getMatchValidationLevel().getIndex();
                HashSet<Integer> validationLevelCoverage = coverage.get(validationLevel);

                if (validationLevelCoverage == null) {

                    validationLevelCoverage = new HashSet<>(peptide.getSequence().length());
                    coverage.put(validationLevel, validationLevelCoverage);

                }

                for (int peptideStart : peptide.getProteinMapping().get(accession)) {

                    int peptideEnd = peptide.getPeptideEnd(accession, peptideStart);

                    for (int j = peptideStart; j < peptideEnd; j++) {

                        validationLevelCoverage.add(j);

                    }
                }
            }
        }

        TreeMap<Integer, HashSet<Integer>> validationLevels = new TreeMap<>(coverage);
        int[] result = new int[sequence.length()];

        for (int i = 0; i < sequence.length(); i++) {

            result[i] = MatchValidationLevel.none.getIndex();

        }

        for (Entry<Integer, HashSet<Integer>> entry : validationLevels.entrySet()) {

            for (int index : entry.getValue()) {

                result[index] = entry.getKey();

            }

        }

        return result;
    }

    /**
     * Returns amino acid coverage of this protein in an array where the index
     * of the best validation level of every peptide covering a given amino acid
     * is given. 0 is the first amino acid.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the identification coverage of the protein sequence
     */
    private int[] estimateAACoverage(long proteinMatchKey) {
        return estimateAACoverage(proteinMatchKey, true, true);
    }

    /**
     * Returns an array of the probability to cover the sequence of the given
     * protein. aa index &gt; probability, 0 is the first amino acid
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @return an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides
     */
    private double[] estimateCoverableAA(long proteinMatchKey) {

        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinMatchKey);
        String accession = proteinMatch.getLeadingAccession();
        String sequence = sequenceProvider.getSequence(accession);

        double[] result = new double[sequence.length()];
        Distribution peptideLengthDistribution = metrics.getPeptideLengthDistribution();
        DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();

        // special case for no cleavage searches
        if (digestionPreferences.getCleavagePreference() != DigestionParameters.CleavagePreference.enzyme) {

            for (int i = 0; i < result.length; i++) {

                result[i] = 1.0;

            }

            return result;

        }

        int lastCleavage = -1;
        char previousChar = sequence.charAt(0), nextChar;

        for (int i = 0; i < sequence.length() - 1; i++) {

            double p = 1.0;
            nextChar = sequence.charAt(i + 1);
            boolean cleavage = false;

            for (Enzyme enzyme : digestionPreferences.getEnzymes()) {

                if (enzyme.isCleavageSite(previousChar, nextChar)) {

                    cleavage = true;
                    break;

                }
            }

            if (cleavage) {

                int length = i - lastCleavage;

                if (peptideLengthDistribution == null) { // < 100 validated peptide

                    int pepMax = identificationParameters.getPeptideAssumptionFilter().getMaxPepLength();

                    if (length > pepMax) {

                        p = 0.0;

                    }

                } else {

                    p = peptideLengthDistribution.getProbabilityAt(length);

                }

                for (int j = lastCleavage + 1; j <= i; j++) {

                    result[j] = p;

                }

                lastCleavage = i;

            }

            previousChar = nextChar;

        }

        double p = 1.0;

        int length = sequence.length() - 1 - lastCleavage;

        if (peptideLengthDistribution == null) { // < 100 validated peptide

            int pepMax = identificationParameters.getPeptideAssumptionFilter().getMaxPepLength();

            if (length > pepMax) {

                p = 0.0;

            }

        } else {

            p = peptideLengthDistribution.getProbabilityAt(length);

        }

        for (int j = lastCleavage + 1; j < sequence.length(); j++) {

            result[j] = p;

        }

        return result;
    }

    /**
     * Returns the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @return the sequence coverage
     */
    public double getValidatedSequenceCoverage(long proteinMatchKey) {

        Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey);

        if (result == null) {

            result = estimateValidatedSequenceCoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Indicates whether the sequence coverage is in cache.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return true if the sequence coverage is in cache
     */
    public boolean validatedSequenceCoverageInCache(long proteinMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey) != null;

    }

    /**
     * Returns the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @return the sequence coverage
     */
    public HashMap<Integer, Double> getSequenceCoverage(long proteinMatchKey) {

        HashMap<Integer, Double> result = (HashMap<Integer, Double>) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey);

        if (result == null) {

            result = estimateSequenceCoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey, result);

        }

        return result;

    }

    /**
     * Indicates whether the sequence coverage is in cache.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return true if the sequence coverage is in cache
     */
    public boolean sequenceCoverageInCache(long proteinMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey) != null;

    }

    /**
     * Returns a list of non-enzymatic peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @param digestionPreferences the digestion preferences
     *
     * @return a list of non-enzymatic peptides for a given protein match
     */
    public long[] getNonEnzymatic(long proteinMatchKey, DigestionParameters digestionPreferences) {

        long[] result = (long[]) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.tryptic_protein, proteinMatchKey);

        if (result == null) {

            result = estimateNonEnzymatic(proteinMatchKey, digestionPreferences);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.tryptic_protein, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Returns a list of non-enzymatic peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @param digestionPreferences the digestion preferences
     *
     * @return a list of non-enzymatic peptides for a given protein match
     */
    private long[] estimateNonEnzymatic(long proteinMatchKey, DigestionParameters digestionPreferences) {

        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinMatchKey);
        long[] peptideKeys = proteinMatch.getPeptideMatchesKeys();

        return digestionPreferences.getCleavagePreference() == DigestionParameters.CleavagePreference.enzyme
                ? Arrays.stream(peptideKeys)
                        .filter(key -> ((PSParameter) (identification.getSpectrumMatch(key)).getUrParam(PSParameter.dummy)).getMatchValidationLevel().isValidated()
                        && !PeptideUtils.isEnzymatic(
                                identification.getSpectrumMatch(key).getBestPeptideAssumption().getPeptide(),
                                sequenceProvider,
                                digestionPreferences.getEnzymes()))
                        .toArray()
                : new long[0];
    }

    /**
     * Updates the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     */
    public void updateSequenceCoverage(long proteinMatchKey) {
        HashMap<Integer, Double> result = estimateSequenceCoverage(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey, result);
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings normalized to the injected protein amount
     * using the spectrum counting preferences of the identification features
     * generator.
     *
     * @param proteinMatchKey the key of the protein match of interest
     *
     * @return the corresponding spectrum counting metric normalized in the
     * metrics prefix of mol
     */
    public double getNormalizedSpectrumCounting(long proteinMatchKey) {
        return getNormalizedSpectrumCounting(proteinMatchKey, metrics, spectrumCountingPreferences.getUnit(), spectrumCountingPreferences.getReferenceMass(), spectrumCountingPreferences.getSelectedMethod());
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings normalized to the injected protein amount
     * using the given spectrum counting preferences.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param metrics the metrics on the dataset
     *
     * @return the corresponding spectrum counting metric normalized in the
     * metricsprefix of mol
     */
    public double getNormalizedSpectrumCounting(long proteinMatchKey, SpectrumCountingParameters spectrumCountingPreferences, Metrics metrics) {
        return getNormalizedSpectrumCounting(proteinMatchKey, metrics, spectrumCountingPreferences.getUnit(), spectrumCountingPreferences.getReferenceMass(), spectrumCountingPreferences.getSelectedMethod());
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings normalized to the injected protein amount.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @param unit the unit to use for the normalization
     * @param method the method to use
     *
     * @return the corresponding spectrum counting metric normalized in the
     * metricsprefix of mol
     */
    public double getNormalizedSpectrumCounting(long proteinMatchKey, UnitOfMeasurement unit, SpectrumCountingParameters.SpectralCountingMethod method) {
        return getNormalizedSpectrumCounting(proteinMatchKey, metrics, unit, spectrumCountingPreferences.getReferenceMass(), method);
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings normalized to the injected protein amount.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @param metrics the metrics on the dataset
     * @param unit the unit to use for the normalization
     * @param method the method to use
     * @param referenceMass the reference mass if abundance normalization is
     * chosen
     *
     * @return the corresponding spectrum counting metric normalized in the
     * metrics prefix of mol
     */
    public double getNormalizedSpectrumCounting(long proteinMatchKey, Metrics metrics, UnitOfMeasurement unit, Double referenceMass, SpectrumCountingParameters.SpectralCountingMethod method) {

        double spectrumCounting = getSpectrumCounting(proteinMatchKey, method);

        if (spectrumCountingPreferences.getNormalize()) {

            String unitFullName = unit.getFullName();
            StandardUnit standardUnit = StandardUnit.getStandardUnit(unitFullName);

            if (standardUnit == null) {

                throw new UnsupportedOperationException("Unit " + unitFullName + " not supported.");

            }

            switch (standardUnit) {

                case mol:

                    if (referenceMass == null) {

                        throw new IllegalArgumentException("Reference mass missing for abundance normalization.");

                    }

                    MetricsPrefix metricsPrefix = unit.getMetricsPrefix();
                    int unitCorrection = 9 + metricsPrefix.POWER; // kg to u for ref mass + metrics.POWER
                    Double result = spectrumCounting * Math.pow(10, -unitCorrection);
                    result *= referenceMass;
                    Double totalCounting = metrics.getTotalSpectrumCountingMass();
                    result /= totalCounting;
                    return result;

                case percentage:
                    totalCounting = metrics.getTotalSpectrumCounting();
                    result = 100 * spectrumCounting / totalCounting;
                    return result;

                case ppm:
                    totalCounting = metrics.getTotalSpectrumCounting();
                    result = 1000000 * spectrumCounting / totalCounting;
                    return result;

                default:
                    throw new UnsupportedOperationException("Unit " + unitFullName + " not supported.");
            }

        } else {

            return spectrumCounting;

        }
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     *
     * @return the corresponding spectrum counting metric
     */
    public double getSpectrumCounting(long proteinMatchKey) {
        return getSpectrumCounting(proteinMatchKey, spectrumCountingPreferences.getSelectedMethod());
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest for
     * the given method.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @param method the method to use
     *
     * @return the corresponding spectrum counting metric
     */
    public Double getSpectrumCounting(long proteinMatchKey, SpectrumCountingParameters.SpectralCountingMethod method) {

        if (method == spectrumCountingPreferences.getSelectedMethod()) {

            Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey);

            if (result == null) {

                result = estimateSpectrumCounting(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey, result);

            }

            return result;
        } else {

            SpectrumCountingParameters tempPreferences = new SpectrumCountingParameters();
            tempPreferences.setSelectedMethod(method);

            return estimateSpectrumCounting(identification, sequenceProvider, proteinMatchKey, tempPreferences,
                    identificationParameters.getPeptideAssumptionFilter().getMaxPepLength(), identificationParameters);

        }
    }

    /**
     * Indicates whether the default spectrum counting value is in cache for a
     * protein match.
     *
     * @param proteinMatchKey the key of the protein match of interest
     *
     * @return true if the data is cached
     */
    public boolean spectrumCountingInCache(long proteinMatchKey) {

        Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey);
        return result != null;

    }

    /**
     * Returns the spectrum counting score based on the user's settings.
     *
     * @param proteinMatch the inspected protein match
     *
     * @return the spectrum counting score
     */
    private double estimateSpectrumCounting(long proteinMatchKey) {

        return estimateSpectrumCounting(identification, sequenceProvider, proteinMatchKey,
                spectrumCountingPreferences,
                identificationParameters.getPeptideAssumptionFilter().getMaxPepLength(), identificationParameters);

    }

    /**
     * Returns the spectrum counting index based on the project settings.
     *
     * @param identification the identification
     * @param sequenceProvider a provider for the protein sequences
     * @param proteinMatchKey the protein match key
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param maxPepLength the maximal length accepted for a peptide
     * @param identificationParameters the identification parameters
     *
     * @return the spectrum counting index
     */
    public static double estimateSpectrumCounting(Identification identification, SequenceProvider sequenceProvider, long proteinMatchKey,
            SpectrumCountingParameters spectrumCountingPreferences, int maxPepLength, IdentificationParameters identificationParameters) {

        ProteinMatch proteinMatch = (ProteinMatch) identification.retrieveObject(proteinMatchKey);
        DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();

        if (spectrumCountingPreferences.getSelectedMethod() == SpectralCountingMethod.NSAF) {

            // NSAF
            double result = 0;

            // iterate the peptides and store the coverage for each peptide validation level
            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null);
            PeptideMatch peptideMatch;

            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                if (psParameter.getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel()) {

                    Peptide peptide = peptideMatch.getPeptide();

                    int peptideOccurrence = identification.getProteinMatches(peptide).stream()
                            .map(groupKey -> identification.getProteinMatch(groupKey))
                            .filter(sharedGroup -> ((PSParameter) sharedGroup.getUrParam(PSParameter.dummy))
                            .getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel())
                            .mapToInt(sharedGroup -> peptide.getProteinMapping()
                            .get(sharedGroup.getLeadingAccession()).length)
                            .sum();

                    double spectrumCount = Arrays.stream(peptideMatch.getSpectrumMatchesKeys())
                            .mapToObj(key -> identification.getSpectrumMatch(key))
                            .filter(spectrumMatch -> ((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                            .getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel())
                            .count();

                    double ratio = spectrumCount / peptideOccurrence;

                    result += ratio;

                }
            }

            String proteinSequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());

            if (digestionPreferences.getCleavagePreference() == DigestionParameters.CleavagePreference.enzyme) {

                result /= ProteinUtils.getObservableLength(proteinSequence, digestionPreferences.getEnzymes(), maxPepLength);

            } else {

                result /= proteinSequence.length();

            }

            if (new Double(result).isInfinite() || new Double(result).isNaN()) {

                result = 0.0;

            }

            return result;

        } else {

            // emPAI
            double result = Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                    .mapToObj(key -> identification.getPeptideMatch(key))
                    .filter(peptideMatch -> ((PSParameter) peptideMatch.getUrParam(PSParameter.dummy))
                    .getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel())
                    .count();

            if (digestionPreferences.getCleavagePreference() == DigestionParameters.CleavagePreference.enzyme) {

                String proteinSequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());
                result = Math.pow(10, result / (ProteinUtils.getNCleavageSites(proteinSequence, digestionPreferences.getEnzymes()) + 1)) - 1;

            } else {

                result = Math.pow(10, result) - 1;

            }

            if (new Double(result).isInfinite() || new Double(result).isNaN()) {

                result = 0.0;

            }

            return result;

        }
    }

    /**
     * Returns the best protein coverage possible according to the given
     * cleavage settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     *
     * @return the best protein coverage possible according to the given
     * cleavage settingswhile estimating the probability to observe an amino
     * acid
     */
    public double getObservableCoverage(long proteinMatchKey) {

        Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey);

        if (result == null) {

            result = estimateObservableCoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Indicates whether the observable coverage of a protein match is in cache.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return true if the data is in cache
     */
    public boolean observableCoverageInCache(long proteinMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey) != null;

    }

    /**
     * Updates the best protein coverage possible according to the given
     * cleavage settings. Used when the main key for a protein has been altered.
     *
     * @param proteinMatchKey the key of the protein match of interest
     */
    public void updateObservableCoverage(long proteinMatchKey) {

        double result = estimateObservableCoverage(proteinMatchKey);

        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey, result);

    }

    /**
     * Returns the best protein coverage possible according to the given
     * cleavage settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     *
     * @return the best protein coverage possible according to the given
     * cleavage settings
     */
    private double estimateObservableCoverage(long proteinMatchKey) {

        DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();

        if (digestionPreferences.getCleavagePreference() != DigestionParameters.CleavagePreference.enzyme) {

            return 1.0;

        }

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String leadingAccession = proteinMatch.getLeadingAccession();
        String sequence = sequenceProvider.getSequence(leadingAccession);
        double lengthMax = identificationParameters.getPeptideAssumptionFilter().getMaxPepLength();

        if (metrics.getPeptideLengthDistribution() != null) {

            lengthMax = Math.min(lengthMax, metrics.getPeptideLengthDistribution().getValueAtCumulativeProbability(0.99));

        }

        return ((double) ProteinUtils.getObservableLength(sequence, digestionPreferences.getEnzymes(), lengthMax)) / sequence.length();

    }

    /**
     * Returns the number of validated proteins. Note that this value is only
     * available after getSortedProteinKeys has been called.
     *
     * @return the number of validated proteins
     */
    public int getNValidatedProteins() {

        if (metrics.getnValidatedProteins() == -1) {

            estimateNValidatedProteins();

        }

        return metrics.getnValidatedProteins();

    }

    /**
     * Estimates the number of validated proteins and saves it in the metrics.
     */
    private void estimateNValidatedProteins() {

        metrics.setnValidatedProteins(
                (int) identification.getProteinIdentification().stream()
                        .map(key -> identification.getProteinMatch(key))
                        .filter(proteinMatch -> !proteinMatch.isDecoy()
                        && ((PSParameter) proteinMatch.getUrParam(PSParameter.dummy))
                                .getMatchValidationLevel().isValidated())
                        .count());
    }

    /**
     * Returns the number of confident proteins. Note that this value is only
     * available after getSortedProteinKeys has been called.
     *
     * @return the number of validated proteins
     */
    public int getNConfidentProteins() {

        if (metrics.getnConfidentProteins() == -1) {

            estimateNConfidentProteins();

        }

        return metrics.getnConfidentProteins();

    }

    /**
     * Estimates the number of confident proteins and saves it in the metrics.
     */
    private void estimateNConfidentProteins() {

        metrics.setnConfidentProteins(
                (int) identification.getProteinIdentification().stream()
                        .map(key -> identification.getProteinMatch(key))
                        .filter(proteinMatch -> !proteinMatch.isDecoy()
                        && ((PSParameter) proteinMatch.getUrParam(PSParameter.dummy))
                                .getMatchValidationLevel() == MatchValidationLevel.confident)
                        .count());

    }

    /**
     * Estimates the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated peptides
     */
    private int estimateNValidatedPeptides(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return (int) Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .mapToObj(key -> identification.getPeptideMatch(key))
                .filter(peptideMatch -> ((PSParameter) peptideMatch.getUrParam(PSParameter.dummy))
                .getMatchValidationLevel().isValidated())
                .count();

    }

    /**
     * Estimates the number of confident peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of confident peptides
     */
    private int estimateNConfidentPeptides(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return (int) Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .mapToObj(key -> identification.getPeptideMatch(key))
                .filter(peptideMatch -> ((PSParameter) peptideMatch.getUrParam(PSParameter.dummy))
                .getMatchValidationLevel() == MatchValidationLevel.confident)
                .count();
    }

    /**
     * Returns the number of unique peptides for this protein match. Note, this
     * is independent of the validation status.
     *
     * @param proteinMatchKey the key of the match
     *
     * @return the number of unique peptides
     */
    public int getNUniquePeptides(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.unique_peptides, proteinMatchKey);

        if (result == null) {

            result = estimateNUniquePeptides(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.unique_peptides, proteinMatchKey, result);

        }

        return result;

    }

    /**
     * Estimates the number of peptides unique to a protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of peptides unique to a protein match
     */
    private int estimateNUniquePeptides(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return (int) Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .mapToObj(peptideKey -> identification.getPeptideMatch(peptideKey))
                .filter(peptideMatch -> identification.getProteinMatches(peptideMatch.getPeptide()).size() == 1)
                .count();

    }

    /**
     * Returns the number of unique validated peptides for this protein match.
     *
     * @param proteinMatchKey the key of the match
     *
     * @return the number of unique peptides
     */
    public int getNUniqueValidatedPeptides(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.unique_validated_peptides, proteinMatchKey);

        if (result == null) {

            result = estimateNUniqueValidatedPeptides(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.unique_validated_peptides, proteinMatchKey, result);

        }

        return result;

    }

    /**
     * Estimates the number of validated peptides unique to a protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of peptides unique to a protein match
     */
    private int estimateNUniqueValidatedPeptides(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return (int) Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .mapToObj(peptideKey -> identification.getPeptideMatch(peptideKey))
                .filter(peptideMatch
                        -> ((PSParameter) peptideMatch.getUrParam(PSParameter.dummy)).getMatchValidationLevel().isValidated()
                && identification.getProteinMatches(peptideMatch.getPeptide()).size() == 1)
                .count();

    }

    /**
     * Returns true if the leading protein of the given group has any enzymatic
     * peptides.
     *
     * @param proteinMatchKey the protein match
     *
     * @return true if the protein has any enzymatic peptides
     */
    public boolean hasEnzymaticPeptides(long proteinMatchKey) {

        Boolean result = (Boolean) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.containsEnzymaticPeptides, proteinMatchKey);

        if (result == null) {

            result = checkEnzymaticPeptides(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.containsEnzymaticPeptides, proteinMatchKey, result);

        }

        return result;

    }

    /**
     * Returns true if the leading protein of the given group has any enzymatic
     * peptides.
     *
     * @param proteinMatchKey the protein match
     *
     * @return true if the protein has any enzymatic peptides
     */
    private boolean checkEnzymaticPeptides(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();

        if (digestionPreferences.getCleavagePreference() == DigestionParameters.CleavagePreference.enzyme) {

            return Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                    .mapToObj(key -> identification.getPeptideMatch(key))
                    .anyMatch(peptideMatch -> PeptideUtils.isEnzymatic(
                    peptideMatch.getPeptide(),
                    sequenceProvider,
                    digestionPreferences.getEnzymes()));

        }

        return true;

    }

    /**
     * Returns the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated peptides
     */
    public int getNValidatedPeptides(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey);

        if (result == null) {

            result = estimateNValidatedPeptides(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Returns the number of confident peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of confident peptides
     */
    public int getNConfidentPeptides(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_confident_peptides, proteinMatchKey);

        if (result == null) {

            result = estimateNConfidentPeptides(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_peptides, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Updates the number of confident peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     */
    public void updateNConfidentPeptides(long proteinMatchKey) {

        Integer result = estimateNConfidentPeptides(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_peptides, proteinMatchKey, result);

    }

    /**
     * Updates the number of confident spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     */
    public void updateNConfidentSpectra(long proteinMatchKey) {

        Integer result = estimateNConfidentSpectra(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_spectra, proteinMatchKey, result);

    }

    /**
     * Indicates whether the number of validated peptides is in cache for a
     * given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return true if the information is in cache
     */
    public boolean nValidatedPeptidesInCache(long proteinMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey) != null;

    }

    /**
     * Estimates the number of spectra for the given protein match.
     *
     * @param proteinMatchKey the key of the given protein match
     *
     * @return the number of spectra for the given protein match
     */
    public Integer getNSpectra(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_spectra, proteinMatchKey);

        if (result == null) {

            result = estimateNSpectra(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_spectra, proteinMatchKey, result);

        }

        return result;

    }

    /**
     * Indicates whether the number of spectra for a given protein match is in
     * cache.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return true if the data is in cache
     */
    public boolean nSpectraInCache(long proteinMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_spectra, proteinMatchKey) != null;

    }

    /**
     * Returns the number of spectra where this protein was found independently
     * from the validation process.
     *
     * @param proteinMatch the protein match of interest
     *
     * @return the number of spectra where this protein was found
     */
    private int estimateNSpectra(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .mapToInt(key -> identification.getPeptideMatch(key).getSpectrumCount())
                .sum();

    }

    /**
     * Returns the maximum number of spectra accounted by a single peptide Match
     * all found in a protein match.
     *
     * @return the maximum number of spectra accounted by a single peptide Match
     * all found in a protein match
     */
    public int getMaxNSpectra() {

        return identificationFeaturesCache.getMaxSpectrumCount();

    }

    /**
     * Returns the number of validated spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated spectra
     */
    public int getNValidatedSpectra(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey);

        if (result == null) {

            result = estimateNValidatedSpectra(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey, result);

        }

        return result;
    }

    /**
     * Returns the number of confident spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated spectra
     */
    public int getNConfidentSpectra(long proteinMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_confident_spectra, proteinMatchKey);

        if (result == null) {
            result = estimateNConfidentSpectra(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_spectra, proteinMatchKey, result);
        }

        return result;
    }

    /**
     * Indicates whether the number of validated spectra is in cache for the
     * given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return true if the data is in cache
     */
    public boolean nValidatedSpectraInCache(long proteinMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey) != null;

    }

    /**
     * Estimates the number of validated spectra for a given protein match.
     *
     * @param proteinMatch the protein match of interest
     *
     * @return the number of spectra where this protein was found
     */
    private int estimateNValidatedSpectra(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return (int) Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .flatMap(key -> Arrays.stream(identification.getPeptideMatch(key).getSpectrumMatchesKeys()))
                .filter(key -> ((PSParameter) identification.getSpectrumMatch(key).getUrParam(PSParameter.dummy))
                .getMatchValidationLevel().isValidated())
                .count();
    }

    /**
     * Estimates the number of confident spectra for a given protein match.
     *
     * @param proteinMatch the protein match of interest
     *
     * @return the number of spectra where this protein was found
     */
    private int estimateNConfidentSpectra(long proteinMatchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);

        return (int) Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                .flatMap(key -> Arrays.stream(identification.getPeptideMatch(key).getSpectrumMatchesKeys()))
                .filter(key -> ((PSParameter) identification.getSpectrumMatch(key).getUrParam(PSParameter.dummy))
                .getMatchValidationLevel() == MatchValidationLevel.confident)
                .count();
    }

    /**
     * Returns the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the key of the peptide match
     *
     * @return the number of validated spectra
     */
    public int getNValidatedSpectraForPeptide(long peptideMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey);

        if (result == null) {

            result = estimateNValidatedSpectraForPeptide(peptideMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey, result);

        }

        return result;
    }

    /**
     * Returns the number of confident spectra for a given peptide match.
     *
     * @param peptideMatchKey the key of the peptide match
     *
     * @return the number of confident spectra
     */
    public int getNConfidentSpectraForPeptide(long peptideMatchKey) {

        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_confident_spectra, peptideMatchKey);

        if (result == null) {

            result = estimateNConfidentSpectraForPeptide(peptideMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_spectra, peptideMatchKey, result);

        }

        return result;
    }

    /**
     * Updates the number of confident spectra for a given peptide match.
     *
     * @param peptideMatchKey the key of the peptide match
     */
    public void updateNConfidentSpectraForPeptide(long peptideMatchKey) {

        Integer result = estimateNConfidentSpectraForPeptide(peptideMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_spectra, peptideMatchKey, result);

    }

    /**
     * Indicates whether the number of validated spectra for a peptide match is
     * in cache.
     *
     * @param peptideMatchKey the key of the peptide match
     *
     * @return true if the data is in cache
     */
    public boolean nValidatedSpectraForPeptideInCache(long peptideMatchKey) {

        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey) != null;

    }

    /**
     * Estimates the number of confident spectra for a given peptide match.
     *
     * @param peptideMatchKey the peptide match of interest
     *
     * @return the number of confident spectra where this peptide was found
     */
    private int estimateNConfidentSpectraForPeptide(long peptideMatchKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);

        return (int) Arrays.stream(peptideMatch.getSpectrumMatchesKeys())
                .filter(key -> ((PSParameter) identification.getSpectrumMatch(key).getUrParam(PSParameter.dummy))
                .getMatchValidationLevel() == MatchValidationLevel.confident)
                .count();

    }

    /**
     * Estimates the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the peptide match of interest
     *
     * @return the number of validated spectra where this peptide was found
     */
    private int estimateNValidatedSpectraForPeptide(long peptideMatchKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);

        return (int) Arrays.stream(peptideMatch.getSpectrumMatchesKeys())
                .filter(key -> ((PSParameter) identification.getSpectrumMatch(key).getUrParam(PSParameter.dummy))
                .getMatchValidationLevel().isValidated())
                .count();

    }

    /**
     * Clears the spectrum counting data in cache.
     */
    public void clearSpectrumCounting() {

        identificationFeaturesCache.removeObjects(IdentificationFeaturesCache.ObjectType.spectrum_counting);

    }

    /**
     * Returns a summary of all modifications present on the sequence
     * confidently assigned to an amino acid. Example: SEQVEM&lt;mox&gt;CE gives
     * Oxidation of M (M6).
     *
     * @param identificationMatch the identification match
     * @param sequence the sequence
     *
     * @return a modificatoin summary for the given match
     */
    public String getConfidentModificationSites(IdentificationMatch identificationMatch, String sequence) {

        PSModificationScores modificationsScores = (PSModificationScores) identificationMatch.getUrParam(new PSModificationScores());

        return modificationsScores == null ? ""
                : modificationsScores.getConfidentlyLocalizedModifications().stream()
                        .map(modName -> String.join("", modName, modificationsScores.getConfidentSitesForModification(modName).stream()
                        .sorted()
                        .map(site -> String.join("", sequence.substring(site - 1, site), site.toString()))
                        .collect(Collectors.joining(", ", "(", ")"))))
                        .collect(Collectors.joining(";"));

    }

    /**
     * Returns the number of confidently localized variable modifications.
     *
     * @param identificationMatch the identification match
     *
     * @return a modification summary for the given protein
     */
    public String getConfidentModificationSitesNumber(IdentificationMatch identificationMatch) {

        final PSModificationScores modificationScores = (PSModificationScores) identificationMatch.getUrParam(new PSModificationScores());

        return modificationScores == null ? ""
                : modificationScores.getConfidentlyLocalizedModifications().stream()
                        .map(modName -> String.join("", modName, "(", Integer.toString(modificationScores.getConfidentSitesForModification(modName).size()), ")"))
                        .collect(Collectors.joining(";"));

    }

    /**
     * Returns a list of the modifications present on the sequence ambiguously
     * assigned to an amino acid grouped by representative site followed by
     * secondary ambiguous sites. Example: SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K
     * returns M6 {M9}.
     *
     * @param identificationMatch the identification match
     * @param sequence the sequence
     *
     * @return a modification summary for the given protein
     */
    public String getAmbiguousModificationSites(IdentificationMatch identificationMatch, String sequence) {

        final PSModificationScores modificationScores = (PSModificationScores) identificationMatch.getUrParam(new PSModificationScores());

        return modificationScores == null ? ""
                : modificationScores.getAmbiguouslyLocalizedModifications().stream()
                        .map(modName -> String.join("", modName, "(", getSitesSummary(modificationScores.getAmbiguousModificationsSites(modName), sequence), ")"))
                        .collect(Collectors.joining(";"));

    }

    /**
     * Returns a string representing the given map of modification sites.
     *
     * @param sites the modification sites
     * @param sequence the sequence
     *
     * @return a string representing the given map of modification sites
     */
    private String getSitesSummary(HashMap<Integer, HashSet<Integer>> sites, String sequence) {

        return (new TreeMap<>(sites)).entrySet().stream()
                .map(entry -> String.join("",
                sequence.substring(entry.getKey() - 1, entry.getKey()),
                entry.getKey().toString(),
                entry.getValue().stream()
                        .map(site -> site.toString())
                        .collect(Collectors.joining(" ", "-{", "}"))))
                .collect(Collectors.joining(","));

    }

    /**
     * Returns a summary of the number of modifications present on the sequence
     * ambiguously assigned to an amino acid grouped by representative site
     * followed by secondary ambiguous sites. Example:
     * SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns M6 {M9}.
     *
     * @param identificationMatch the identification match
     *
     * @return a modification summary for the given match
     */
    public String getAmbiguousModificationSiteNumber(IdentificationMatch identificationMatch) {

        final PSModificationScores modificationScores = (PSModificationScores) identificationMatch.getUrParam(new PSModificationScores());

        return modificationScores == null ? ""
                : modificationScores.getAmbiguouslyLocalizedModifications().stream()
                        .map(modName -> String.join("",
                        modName, " (", Integer.toString(modificationScores.getAmbiguousModificationsSites(modName).size()), ")"))
                        .collect(Collectors.joining(";"));

    }

    /**
     * Returns a summary of the modifications present on the peptide sequence
     * confidently assigned to an amino acid with focus on given list of
     * modifications.
     *
     * @param match the identification match
     * @param sequence the sequence
     * @param targetedModifications the mosifications to include in the summary
     *
     * @return a modification summary for the given match
     */
    public String getConfidentModificationSites(IdentificationMatch match, String sequence, ArrayList<String> targetedModifications) {

        final PSModificationScores modificationScores = (PSModificationScores) match.getUrParam(new PSModificationScores());

        return modificationScores == null ? ""
                : targetedModifications.stream()
                        .flatMap(modName -> modificationScores.getConfidentSitesForModification(modName).stream())
                        .distinct()
                        .sorted()
                        .map(site -> String.join("", sequence.substring(site - 1, site), site.toString()))
                        .collect(Collectors.joining(","));

    }

    /**
     * Returns the number of confidently localized variable modifications.
     *
     * @param match the identification match
     * @param targetedModifications the modifications to include in the summary
     *
     * @return a modification summary for the given match
     */
    public String getConfidentModificationSitesNumber(IdentificationMatch match, ArrayList<String> targetedModifications) {

        final PSModificationScores modificationScores = (PSModificationScores) match.getUrParam(new PSModificationScores());

        return modificationScores == null ? ""
                : Long.toString(
                        targetedModifications.stream()
                                .flatMap(modName -> modificationScores.getConfidentSitesForModification(modName).stream())
                                .distinct()
                                .count());

    }

    /**
     * Returns a list of the modifications present on the sequence ambiguously
     * assigned to an amino acid grouped by representative site followed by
     * secondary ambiguous sites. Example: SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K
     * returns M6 {M9}.
     *
     * @param match the identification match
     * @param sequence the sequence
     * @param targetedModifications the modifications to include in the summary
     *
     * @return a modification summary for the given match
     */
    public String getAmbiguousModificationSites(IdentificationMatch match, String sequence, ArrayList<String> targetedModifications) {

        PSModificationScores modificationScores = new PSModificationScores();
        modificationScores = (PSModificationScores) match.getUrParam(modificationScores);

        if (modificationScores == null) {

            return "";

        } else {

            TreeMap<Integer, ArrayList<String>> reportPerSite = new TreeMap<>();

            for (String modName : targetedModifications) {

                TreeMap<Integer, HashSet<Integer>> sites = new TreeMap<>(modificationScores.getAmbiguousModificationsSites(modName));

                for (Entry<Integer, HashSet<Integer>> entry : sites.entrySet()) {

                    int representativeSite = entry.getKey();
                    HashSet<Integer> ambiguousSites = entry.getValue();

                    StringBuilder prefix = new StringBuilder();
                    prefix.append(sequence.charAt(representativeSite - 1)).append(representativeSite).append("-{");

                    String reportAtSite = ambiguousSites.stream()
                            .sorted()
                            .map(site -> String.join("", sequence.substring(site - 1, site), site.toString()))
                            .collect(Collectors.joining(" ", prefix, "}"));

                    ArrayList<String> reportsAtSite = reportPerSite.get(representativeSite);

                    if (reportsAtSite == null) {

                        reportsAtSite = new ArrayList<>(1);
                        reportPerSite.put(representativeSite, reportsAtSite);

                    }

                    reportsAtSite.add(reportAtSite);

                }
            }

            return reportPerSite.values().stream()
                    .flatMap(ArrayList::stream)
                    .collect(Collectors.joining(";"));

        }
    }

    /**
     * Returns a summary of the number of modifications present on the sequence
     * ambiguously assigned to an amino acid grouped by representative site
     * followed by secondary ambiguous sites. Example:
     * SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns M6 {M9}.
     *
     * @param match the identification match
     * @param targetedModifications the modifications to include in the summary
     *
     * @return a modification summary for the given match
     */
    public String getAmbiguousModificationSiteNumber(IdentificationMatch match, ArrayList<String> targetedModifications) {

        final PSModificationScores modificationScores = (PSModificationScores) match.getUrParam(new PSModificationScores());

        return modificationScores == null ? ""
                : Long.toString(
                        targetedModifications.stream()
                                .flatMap(modName -> modificationScores.getAmbiguousModificationsSites(modName).keySet().stream())
                                .distinct()
                                .count());
        
    }

    /**
     * Returns the match sequence annotated with modifications.
     *
     * @param identificationMatch the identification match
     * @param sequence the sequence of the match
     *
     * @return the protein sequence annotated with modifications
     */
    public String getModifiedSequence(IdentificationMatch identificationMatch, String sequence) {

        PSModificationScores modificationScores = (PSModificationScores) identificationMatch.getUrParam(new PSModificationScores());

        if (modificationScores == null) {
            
            return "";
            
        } else {
            
            StringBuilder result = new StringBuilder(sequence.length());

            for (int aa = 0; aa < sequence.length(); aa++) {

                result.append(sequence.charAt(aa));
                
                int aaNumber = aa + 1;
                HashSet<String> modificationsAtSite = modificationScores.getConfidentModificationsAt(aaNumber);

                if (!modificationsAtSite.isEmpty()) {
                    
                    result.append(
                        modificationsAtSite.stream()
                            .sorted()
                                .map(modName -> modificationFactory.getModification(modName).getShortName())
                            .collect(Collectors.joining(",", "<", ">")));
                    
                }
            }

            return result.toString();
            
        }
    }

    /**
     * Returns the list of validated protein keys. Returns null if the proteins
     * have yet to be validated.
     *
     * @param filterPreferences the filtering preferences used. can be null
     *
     * @return the list of validated protein keys
     */
    public long[] getValidatedProteins(FilterParameters filterPreferences) {

        return getValidatedProteins(null, filterPreferences);

    }

    /**
     * Returns the list of validated protein keys. Returns null if the proteins
     * have yet to be validated.
     *
     * @param filterPreferences the filtering preferences used. can be null
     * @param waitingHandler the waiting handler, can be null
     *
     * @return the list of validated protein keys
     */
    public long[] getValidatedProteins(WaitingHandler waitingHandler, FilterParameters filterPreferences) {

        long[] result = identificationFeaturesCache.getValidatedProteinList();

        if (result == null) {

            getProcessedProteinKeys(waitingHandler, filterPreferences);

        }

        return identificationFeaturesCache.getValidatedProteinList();

    }

    /**
     * Returns the sorted list of protein keys.
     *
     * @param filterPreferences the filtering preferences used. can be null
     * @param waitingHandler the waiting handler, can be null
     *
     * @return the sorted list of protein keys
     */
    public long[] getProcessedProteinKeys(WaitingHandler waitingHandler, FilterParameters filterPreferences) {

        if (identificationFeaturesCache.getProteinList() == null) {
            if (waitingHandler != null) {
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setWaitingText("Loading Protein Information. Please Wait...");
                waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());
            }
            boolean needMaxValues = (metrics.getMaxNPeptides() == null)
                    || metrics.getMaxNPeptides() <= 0
                    || metrics.getMaxNSpectra() == null
                    || metrics.getMaxNSpectra() <= 0
                    || metrics.getMaxSpectrumCounting() == null
                    || metrics.getMaxSpectrumCounting() <= 0
                    || metrics.getMaxMW() == null
                    || metrics.getMaxMW() <= 0;

            // sort the proteins according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
            HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<Long>>>> orderMap
                    = new HashMap<>();
            ArrayList<Double> scores = new ArrayList<>();
            int maxPeptides = 0, maxSpectra = 0;
            double maxSpectrumCounting = 0, maxMW = 0;
            int nValidatedProteins = 0;
            int nConfidentProteins = 0;

            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
            ProteinMatch proteinMatch;

            while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                long proteinKey = proteinMatch.getKey();

                if (!proteinMatch.isDecoy()) {

                    PSParameter proteinPsParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                    if (!proteinPsParameter.getHidden()) {

                        double score = proteinPsParameter.getProteinProbabilityScore();
                        int nPeptides = -proteinMatch.getPeptideMatchesKeys().length;
                        int nSpectra = -getNSpectra(proteinKey);

                        if (needMaxValues) {

                            if (-nPeptides > maxPeptides) {
                                maxPeptides = -nPeptides;
                            }

                            if (-nSpectra > maxSpectra) {
                                maxSpectra = -nSpectra;
                            }

                            double tempSpectrumCounting = getNormalizedSpectrumCounting(proteinKey);

                            if (tempSpectrumCounting > maxSpectrumCounting) {
                                maxSpectrumCounting = tempSpectrumCounting;
                            }

                            String proteinSequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());

                            double mw = ProteinUtils.computeMolecularWeight(proteinSequence);

                            if (mw > maxMW) {
                                maxMW = mw;
                            }

                            if (proteinPsParameter.getMatchValidationLevel().isValidated()) {

                                nValidatedProteins++;

                                if (proteinPsParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {

                                    nConfidentProteins++;

                                }
                            }
                        }

                        if (!orderMap.containsKey(score)) {

                            orderMap.put(score, new HashMap<>(1));
                            scores.add(score);

                        }

                        if (!orderMap.get(score).containsKey(nPeptides)) {

                            orderMap.get(score).put(nPeptides, new HashMap<>(1));

                        }

                        if (!orderMap.get(score).get(nPeptides).containsKey(nSpectra)) {

                            orderMap.get(score).get(nPeptides).put(nSpectra, new ArrayList<>(1));

                        }

                        orderMap.get(score).get(nPeptides).get(nSpectra).add(proteinKey);

                    }
                }

                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {
                        return null;
                    }
                }
            }

            if (needMaxValues) {

                metrics.setMaxNPeptides(maxPeptides);
                metrics.setMaxNSpectra(maxSpectra);
                metrics.setMaxSpectrumCounting(maxSpectrumCounting);
                metrics.setMaxMW(maxMW);
                metrics.setnValidatedProteins(nValidatedProteins);
                metrics.setnConfidentProteins(nConfidentProteins);

            }

            ArrayList<Long> proteinList = new ArrayList<>();

            ArrayList<Double> scoreList = new ArrayList<>(orderMap.keySet());
            Collections.sort(scoreList);

            if (waitingHandler != null) {

                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setWaitingText("Updating Protein Table. Please Wait...");
                waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());

            }

            for (double currentScore : scoreList) {

                ArrayList<Integer> nPeptideList = new ArrayList<>(orderMap.get(currentScore).keySet());
                Collections.sort(nPeptideList);

                for (int currentNPeptides : nPeptideList) {

                    ArrayList<Integer> nPsmList = new ArrayList<>(orderMap.get(currentScore).get(currentNPeptides).keySet());
                    Collections.sort(nPsmList);

                    for (int currentNPsms : nPsmList) {

                        ArrayList<Long> tempList = orderMap.get(currentScore).get(currentNPeptides).get(currentNPsms);
                        Collections.sort(tempList);
                        proteinList.addAll(tempList);

                        if (waitingHandler != null) {

                            waitingHandler.setMaxSecondaryProgressCounter(tempList.size());

                            if (waitingHandler.isRunCanceled()) {

                                return null;

                            }
                        }
                    }
                }
            }

            long[] proteinArray = proteinList.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            identificationFeaturesCache.setProteinList(proteinArray);

            if (waitingHandler != null) {

                waitingHandler.setPrimaryProgressCounterIndeterminate(true);

                if (waitingHandler.isRunCanceled()) {
                    return null;
                }
            }
        }

        if (hidingNeeded(filterPreferences) || identificationFeaturesCache.getProteinListAfterHiding() == null) {

            ArrayList<Long> proteinListAfterHiding = new ArrayList<>();
            ArrayList<Long> validatedProteinList = new ArrayList<>();
            int nValidatedProteins = 0;
            int nConfidentProteins = 0;

            for (long proteinKey : identificationFeaturesCache.getProteinList()) {

                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

                if (!proteinMatch.isDecoy()) {

                    PSParameter psParameter = (PSParameter) (proteinMatch).getUrParam(PSParameter.dummy);

                    if (!psParameter.getHidden()) {

                        proteinListAfterHiding.add(proteinKey);

                        if (psParameter.getMatchValidationLevel().isValidated()) {

                            nValidatedProteins++;
                            validatedProteinList.add(proteinKey);

                            if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {

                                nConfidentProteins++;

                            }
                        }
                    }
                }

                if (waitingHandler != null) {

                    waitingHandler.setPrimaryProgressCounterIndeterminate(true);

                    if (waitingHandler.isRunCanceled()) {
                        return null;
                    }
                }
            }

            long[] proteinArrayAfterHiding = proteinListAfterHiding.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            identificationFeaturesCache.setProteinListAfterHiding(proteinArrayAfterHiding);

            long[] validatedProteinArray = validatedProteinList.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            identificationFeaturesCache.setValidatedProteinList(validatedProteinArray);

            metrics.setnValidatedProteins(nValidatedProteins);
            metrics.setnConfidentProteins(nConfidentProteins);

        }

        return identificationFeaturesCache.getProteinListAfterHiding();
    }

    /**
     * Returns the ordered protein keys to display when no filtering is applied.
     *
     * @param waitingHandler can be null
     * @param filterPreferences the filtering preferences used. can be null
     *
     * @return the ordered protein keys to display when no filtering is applied.
     */
    public long[] getProteinKeys(WaitingHandler waitingHandler, FilterParameters filterPreferences) {

        if (identificationFeaturesCache.getProteinList() == null) {

            getProcessedProteinKeys(waitingHandler, filterPreferences);

        }

        return identificationFeaturesCache.getProteinList();
    }

    /**
     * Returns a sorted list of peptide keys from the protein of interest.
     *
     * @param proteinKey the key of the protein of interest
     *
     * @return a sorted list of the corresponding peptide keys
     */
    public long[] getSortedPeptideKeys(long proteinKey) {

        if (proteinKey != identificationFeaturesCache.getCurrentProteinKey() || identificationFeaturesCache.getPeptideList() == null) {

            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

            HashMap<Double, HashMap<Integer, ArrayList<Long>>> peptideMap = new HashMap<>(1);

            int maxSpectrumCount = 0;

            // iterate the peptides and store the coverage for each peptide validation level
            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null);
            PeptideMatch peptideMatch;

            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                long peptideKey = peptideMatch.getKey();
                PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                if (!psParameter.getHidden()) {

                    double peptideProbabilityScore = psParameter.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {

                        peptideMap.put(peptideProbabilityScore, new HashMap<>(1));

                    }

                    int spectrumCount = -peptideMatch.getSpectrumCount();

                    if (peptideMatch.getSpectrumCount() > maxSpectrumCount) {

                        maxSpectrumCount = peptideMatch.getSpectrumCount();

                    }

                    if (!peptideMap.get(peptideProbabilityScore).containsKey(spectrumCount)) {

                        peptideMap.get(peptideProbabilityScore).put(spectrumCount, new ArrayList<>(1));

                    }

                    peptideMap.get(peptideProbabilityScore).get(spectrumCount).add(peptideKey);

                }
            }

            identificationFeaturesCache.setMaxSpectrumCount(maxSpectrumCount);

            ArrayList<Double> scores = new ArrayList<>(peptideMap.keySet());
            Collections.sort(scores);
            ArrayList<Long> peptideList = new ArrayList<>();

            for (double currentScore : scores) {

                ArrayList<Integer> nSpectra = new ArrayList<>(peptideMap.get(currentScore).keySet());
                Collections.sort(nSpectra);

                for (int currentNPsm : nSpectra) {

                    ArrayList<Long> keys = peptideMap.get(currentScore).get(currentNPsm);
                    Collections.sort(keys);
                    peptideList.addAll(keys);

                }
            }

            long[] peptideArray = peptideList.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            identificationFeaturesCache.setPeptideList(peptideArray);
            identificationFeaturesCache.setCurrentProteinKey(proteinKey);
        }
        return identificationFeaturesCache.getPeptideList();
    }

    /**
     * Returns the ordered list of spectrum keys for a given peptide.
     *
     * @param peptideKey the key of the peptide of interest
     * @param sortOnRt if true, the PSMs are sorted in retention time, false
     * sorts on PSM score
     * @param forceUpdate if true, the sorted listed is recreated even if not
     * needed
     *
     * @return the ordered list of spectrum keys
     */
    public long[] getSortedPsmKeys(long peptideKey, boolean sortOnRt, boolean forceUpdate) {

        if (peptideKey != identificationFeaturesCache.getCurrentPeptideKey() || identificationFeaturesCache.getPsmList() == null || forceUpdate) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

            HashMap<Integer, HashMap<Double, ArrayList<Long>>> orderingMap = new HashMap<>(1);
            boolean hasRT = sortOnRt;
            double rt = -1;
            int nValidatedPsms = 0;

            long[] spectrumKeys = peptideMatch.getSpectrumMatchesKeys();

            SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(spectrumKeys, null);
            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = psmIterator.next()) != null) {

                long matchKey = spectrumMatch.getKey();
                PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                if (!psParameter.getHidden()) {

                    if (psParameter.getMatchValidationLevel().isValidated()) {

                        nValidatedPsms++;

                    }

                    int charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();

                    if (!orderingMap.containsKey(charge)) {

                        orderingMap.put(charge, new HashMap<>(spectrumKeys.length));

                    }

                    if (hasRT) {

                        try {

                            Precursor precursor = spectrumFactory.getPrecursor(spectrumMatch.getSpectrumKey());
                            rt = precursor.getRt();

                            if (rt == -1) {

                                hasRT = false;

                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                            hasRT = false;

                        }
                    }

                    if (!hasRT) {

                        rt = psParameter.getPsmProbabilityScore();

                    }

                    if (!orderingMap.get(charge).containsKey(rt)) {

                        orderingMap.get(charge).put(rt, new ArrayList<>(1));

                    }

                    orderingMap.get(charge).get(rt).add(matchKey);

                }
            }

            identificationFeaturesCache.setnValidatedPsms(nValidatedPsms);

            ArrayList<Integer> charges = new ArrayList<>(orderingMap.keySet());
            Collections.sort(charges);
            ArrayList<Long> psmList = new ArrayList<>();

            for (int currentCharge : charges) {

                ArrayList<Double> rts = new ArrayList<>(orderingMap.get(currentCharge).keySet());
                Collections.sort(rts);

                for (double currentRT : rts) {

                    ArrayList<Long> tempResult = orderingMap.get(currentCharge).get(currentRT);
                    Collections.sort(tempResult);
                    psmList.addAll(tempResult);

                }
            }

            long[] psmArray = psmList.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            identificationFeaturesCache.setPsmList(psmArray);
            identificationFeaturesCache.setCurrentPeptideKey(peptideKey);
        }

        return identificationFeaturesCache.getPsmList();
    }

    /**
     * Returns the number of validated PSMs for the last selected peptide /!\
     * This value is only available after getSortedPsmKeys has been called.
     *
     * @return the number of validated PSMs for the last selected peptide
     */
    public int getNValidatedPsms() {

        return identificationFeaturesCache.getnValidatedPsms();

    }

    /**
     * Returns a boolean indicating whether hiding proteins is necessary.
     *
     * @param filterPreferences the filtering preferences used. can be null
     *
     * @return a boolean indicating whether hiding proteins is necessary
     */
    private boolean hidingNeeded(FilterParameters filterPreferences) {

        if (filterPreferences == null) {

            return false;

        }

        if (identificationFeaturesCache.isFiltered()) {

            return true;

        }

        for (ProteinFilter proteinFilter : filterPreferences.getProteinHideFilters().values()) {

            if (proteinFilter.isActive()) {

                identificationFeaturesCache.setFiltered(true);
                return true;

            }
        }

        return false;
    }

    /**
     * Sets the ordered protein list.
     *
     * @param proteinList the ordered protein list
     */
    public void setProteinKeys(long[] proteinList) {

        identificationFeaturesCache.setProteinList(proteinList);

    }

    /**
     * Returns the identification features cache.
     *
     * @return the identification features cache
     */
    public IdentificationFeaturesCache getIdentificationFeaturesCache() {

        return identificationFeaturesCache;

    }

    /**
     * Sets the the identification features cache.
     *
     * @param identificationFeaturesCache the new identification features cache
     */
    public void setIdentificationFeaturesCache(IdentificationFeaturesCache identificationFeaturesCache) {

        this.identificationFeaturesCache = identificationFeaturesCache;

    }

    /**
     * Returns the metrics.
     *
     * @return the metrics
     */
    public Metrics getMetrics() {

        return metrics;

    }

    /**
     * Sets the spectrum couting preferences.
     *
     * @param spectrumCountingPreferences the spectrum counting preferences
     */
    public void setSpectrumCountingPreferences(SpectrumCountingParameters spectrumCountingPreferences) {

        this.spectrumCountingPreferences = spectrumCountingPreferences;

    }

    /**
     * Indicates whether a peptide is found in a single protein match.
     *
     * @param peptide the peptide of interest
     *
     * @return true if peptide is found in a single protein match
     */
    public int getNValidatedProteinGroups(Peptide peptide) {

        return getNValidatedProteinGroups(peptide, null);

    }

    /**
     * Indicates whether a peptide is found in a single protein match.
     *
     * @param peptide the peptide of interest
     * @param waitingHandler waiting handler allowing the canceling of the
     * progress
     *
     * @return true if peptide is found in a single protein match
     */
    public int getNValidatedProteinGroups(Peptide peptide, WaitingHandler waitingHandler) {

        HashSet<Long> keys = identification.getProteinMatches(peptide);

        return (int) keys.stream()
                .filter(key -> ((PSParameter) identification.getProteinMatch(key).getUrParam(PSParameter.dummy))
                .getMatchValidationLevel().isValidated())
                .count();
    }
}
