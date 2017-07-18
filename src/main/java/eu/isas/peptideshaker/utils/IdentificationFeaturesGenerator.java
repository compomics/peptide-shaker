package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.units.MetricsPrefix;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMatch;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.units.StandardUnit;
import com.compomics.util.experiment.units.UnitOfMeasurement;
import com.compomics.util.math.statistics.Distribution;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class provides identification features and stores them in cache.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class IdentificationFeaturesGenerator {

    // @TODO: move to utilities once the back-end allows it
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The identification features cache where the recently accessed
     * identification features are stored
     */
    private IdentificationFeaturesCache identificationFeaturesCache = new IdentificationFeaturesCache();
    /**
     * The metrics picked-up wile loading the data.
     */
    private Metrics metrics;
    /**
     * The identification of interest.
     */
    private Identification identification;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * The spectrum counting preferences.
     */
    private SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * Map of the distributions of precursor mass errors.
     */
    private HashMap<String, NonSymmetricalNormalDistribution> massErrorDistribution = null;

    /**
     * Constructor.
     *
     * @param identification the identification of interest
     * @param identificationParameters the identification parameters
     * @param metrics the metrics picked-up wile loading the data
     * @param spectrumCountingPreferences the spectrum counting preferences
     */
    public IdentificationFeaturesGenerator(Identification identification, IdentificationParameters identificationParameters,
            Metrics metrics, SpectrumCountingPreferences spectrumCountingPreferences) {
        this.metrics = metrics;
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
            massErrorDistribution = new HashMap<String, NonSymmetricalNormalDistribution>(1);
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
     *
     * @throws SQLException Exception thrown whenever an error occurred while
     * interacting with a database
     * @throws IOException Exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing a file
     * @throws InterruptedException Exception thrown whenever a threading error
     * occurred while estimating the mass distribution
     * @throws MzMLUnmarshallerException Exception thrown whenever an error
     * occurred while reading an mzML file
     */
    public NonSymmetricalNormalDistribution getMassErrorDistribution(String spectrumFile) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
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
     *
     * @throws SQLException Exception thrown whenever an error occurred while
     * interacting with a database
     * @throws IOException Exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing a file
     * @throws InterruptedException Exception thrown whenever a threading error
     * occurred while estimating the mass distribution
     * @throws MzMLUnmarshallerException Exception thrown whenever an error
     * occurred while reading an mzML file
     */
    private void estimateMassErrorDistribution(String spectrumFile) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        ArrayList<Double> precursorMzDeviations = new ArrayList<Double>(512);
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PsmIterator psmIterator = identification.getPsmIterator(spectrumFile, parameters, false, null);
        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {
            PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
            if (peptideAssumption != null) {
                String spectrumKey = spectrumMatch.getKey();
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
                    SearchParameters searchParameters = identificationParameters.getSearchParameters();
                    double precursorMzError = peptideAssumption.getDeltaMass(precursorMz, searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                    precursorMzDeviations.add(precursorMzError);
                }
            }
        }
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public double[] getCoverableAA(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int[] getAACoverage(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public void updateCoverableAA(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        double[] result = estimateCoverableAA(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.coverable_AA_p, proteinMatchKey, result);
    }

    /**
     * Returns the variable modifications found in the currently loaded dataset.
     *
     * @return the variable modifications found in the currently loaded dataset
     */
    public ArrayList<String> getFoundModifications() {
        if (metrics == null) {
            return new ArrayList<String>();
        }
        ArrayList<String> modifications = metrics.getFoundModifications();
        if (modifications == null) {
            modifications = new ArrayList<String>();
            for (String peptideKey : identification.getPeptideIdentification()) {
                for (String modification : Peptide.getModificationFamily(peptideKey)) {
                    if (!modifications.contains(modification)) {
                        modifications.add(modification);
                    }
                }
            }
            metrics.setFoundModifications(modifications);
        }
        return modifications;
    }

    /**
     * Estimates the sequence coverage for the given protein match according to
     * the validation level: validation level &gt; share of the sequence
     * uniquely covered by this validation level.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the sequence coverage
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private HashMap<Integer, Double> estimateSequenceCoverage(String proteinMatchKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int[] aaCoverage = getAACoverage(proteinMatchKey);
        HashMap<Integer, Double> result = new HashMap<Integer, Double>();
        for (int validationLevel : MatchValidationLevel.getValidationLevelIndexes()) {
            result.put(validationLevel, 0.0);
        }
        for (int validationLevel : aaCoverage) {
            result.put(validationLevel, result.get(validationLevel) + 1);
        }
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private Double estimateValidatedSequenceCoverage(String proteinMatchKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int[] aaCoverage = getAACoverage(proteinMatchKey);
        double nAAValidated = 0;
        for (int validationLevel : aaCoverage) {
            if (validationLevel == MatchValidationLevel.doubtful.getIndex()
                    || validationLevel == MatchValidationLevel.confident.getIndex()) {
                nAAValidated++;
            }
        }
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        double result = nAAValidated / sequence.length();
        return result;
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int[] estimateAACoverage(String proteinMatchKey, boolean enzymatic)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with a database (from the protein tree or identification)
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from a database (from the protein
     * tree or identification)
     * @throws InterruptedException exception thrown whenever a threading error
     * occurred while interacting with a database (from the protein tree or
     * identification)
     */
    private int[] estimateAACoverage(String proteinMatchKey, boolean allPeptides, boolean enzymatic) // @TODO: add waiting handler?
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
        String sequence = currentProtein.getSequence();

        HashMap<Integer, ArrayList<Integer>> aminoAcids = new HashMap<Integer, ArrayList<Integer>>();
        HashMap<Integer, boolean[]> coverage = new HashMap<Integer, boolean[]>();
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        // iterate the peptides and store the coverage for each peptide validation level
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, null);

        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            String peptideKey = peptideMatch.getKey();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
            String peptideSequence = Peptide.getSequence(peptideKey);
            boolean enzymaticPeptide = true;
            if (!allPeptides) {
                DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();
                if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                    enzymaticPeptide = currentProtein.isEnzymaticPeptide(peptideSequence, digestionPreferences.getEnzymes(),
                            identificationParameters.getSequenceMatchingPreferences());
                }
            }
            if (allPeptides || enzymatic && enzymaticPeptide || !enzymatic && !enzymaticPeptide) {
                int validationLevel = psParameter.getMatchValidationLevel().getIndex();
                boolean[] validationLevelCoverage = coverage.get(validationLevel);
                ArrayList<Integer> levelAminoAcids = aminoAcids.get(validationLevel);
                if (validationLevelCoverage == null) {
                    validationLevelCoverage = new boolean[sequence.length() + 1];
                    coverage.put(validationLevel, validationLevelCoverage);
                    levelAminoAcids = new ArrayList<Integer>();
                    aminoAcids.put(validationLevel, levelAminoAcids);
                }
                AminoAcidPattern aminoAcidPattern = AminoAcidPattern.getAminoAcidPatternFromString(peptideSequence);
                for (int index : aminoAcidPattern.getIndexes(sequence, identificationParameters.getSequenceMatchingPreferences())) {
                    int peptideTempStart = index - 1;
                    int peptideTempEnd = peptideTempStart + peptideSequence.length();
                    for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                        boolean found = validationLevelCoverage[j];
                        if (!found) {
                            validationLevelCoverage[j] = true;
                            levelAminoAcids.add(j);
                        }
                    }
                }
            }
        }

        int[] result = new int[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            result[i] = MatchValidationLevel.none.getIndex();
        }
        ArrayList<Integer> validationLevels = new ArrayList<Integer>(coverage.keySet());
        Collections.sort(validationLevels, Collections.reverseOrder());
        for (int validationLevel : validationLevels) {
            boolean[] validationLevelCoverage = coverage.get(validationLevel);
            if (validationLevelCoverage != null) {
                ArrayList<Integer> levelAminoAcids = aminoAcids.get(validationLevel);
                for (int aa : levelAminoAcids) {
                    int previousLevel = result[aa];
                    if (previousLevel == MatchValidationLevel.none.getIndex()) {
                        result[aa] = validationLevel;
                    }
                }
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int[] estimateAACoverage(String proteinMatchKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private double[] estimateCoverableAA(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        double[] result = new double[sequence.length()];
        Distribution peptideLengthDistribution = metrics.getPeptideLengthDistribution();
        DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();

        // special case for no cleavage searches
        if (digestionPreferences.getCleavagePreference() != DigestionPreferences.CleavagePreference.enzyme) {
            for (int i = 0; i < result.length; i++) {
                result[i] = 1.0;
            }
            return result;
        }

        int lastCleavage = -1;
        char previousChar = sequence.charAt(0), nextChar;

        for (int i = 0; i < sequence.length() - 1; i++) {
            double p = 1;
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
                        p = 0;
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

        double p = 1;

        int length = sequence.length() - 1 - lastCleavage;
        if (peptideLengthDistribution == null) { // < 100 validated peptide
            int pepMax = identificationParameters.getPeptideAssumptionFilter().getMaxPepLength();
            if (length > pepMax) {
                p = 0;
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getValidatedSequenceCoverage(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
    public boolean validatedSequenceCoverageInCache(String proteinMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey) != null;
    }

    /**
     * Returns the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @return the sequence coverage
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public HashMap<Integer, Double> getSequenceCoverage(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
    public boolean sequenceCoverageInCache(String proteinMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_validation_coverage, proteinMatchKey) != null;
    }

    /**
     * Returns a list of non-enzymatic peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @param digestionPreferences the digestion preferences
     *
     * @return a list of non-enzymatic peptides for a given protein match
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getNonEnzymatic(String proteinMatchKey, DigestionPreferences digestionPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        ArrayList<String> result = (ArrayList<String>) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.tryptic_protein, proteinMatchKey);

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
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading a file
     * @throws InterruptedException exception thrown whenever a synchronization
     * error occurred
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     */
    private ArrayList<String> estimateNonEnzymatic(String proteinMatchKey, DigestionPreferences digestionPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        ArrayList<String> peptideKeys = proteinMatch.getPeptideMatchesKeys();
        PSParameter peptidePSParameter = new PSParameter();

        identification.loadPeptideMatchParameters(peptideKeys, peptidePSParameter, null, false);

        ArrayList<String> result = new ArrayList<String>();

        if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
            // see if we have non-tryptic peptides
            for (String peptideKey : peptideKeys) {

                peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptidePSParameter);

                if (peptidePSParameter.getMatchValidationLevel().isValidated()) {

                    String peptideSequence = Peptide.getSequence(peptideKey);
                    boolean enzymatic = false;
                    for (String accession : ProteinMatch.getAccessions(proteinMatchKey)) {
                        Protein currentProtein = sequenceFactory.getProtein(accession);
                        if (currentProtein.isEnzymaticPeptide(peptideSequence, digestionPreferences.getEnzymes(),
                                identificationParameters.getSequenceMatchingPreferences())) {
                            enzymatic = true;
                            break;
                        }
                    }

                    if (!enzymatic) {
                        result.add(peptideKey);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Updates the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public void updateSequenceCoverage(String proteinMatchKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     * metricsprefix of mol
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getNormalizedSpectrumCounting(String proteinMatchKey)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getNormalizedSpectrumCounting(String proteinMatchKey, SpectrumCountingPreferences spectrumCountingPreferences, Metrics metrics)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getNormalizedSpectrumCounting(String proteinMatchKey, UnitOfMeasurement unit, SpectrumCountingPreferences.SpectralCountingMethod method)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getNormalizedSpectrumCounting(String proteinMatchKey, Metrics metrics, UnitOfMeasurement unit, Double referenceMass, SpectrumCountingPreferences.SpectralCountingMethod method)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        Double spectrumCounting = getSpectrumCounting(proteinMatchKey, method);
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getSpectrumCounting(String proteinMatchKey) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Double getSpectrumCounting(String proteinMatchKey, SpectrumCountingPreferences.SpectralCountingMethod method)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        if (method == spectrumCountingPreferences.getSelectedMethod()) {
            Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey);

            if (result == null) {
                result = estimateSpectrumCounting(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey, result);
            }
            return result;
        } else {
            SpectrumCountingPreferences tempPreferences = new SpectrumCountingPreferences();
            tempPreferences.setSelectedMethod(method);
            return estimateSpectrumCounting(identification, sequenceFactory, proteinMatchKey, tempPreferences,
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
    public boolean spectrumCountingInCache(String proteinMatchKey) {
        Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey);
        return result != null;
    }

    /**
     * Returns the spectrum counting score based on the user's settings.
     *
     * @param proteinMatch the inspected protein match
     *
     * @return the spectrum counting score
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private double estimateSpectrumCounting(String proteinMatchKey) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        return estimateSpectrumCounting(identification, sequenceFactory, proteinMatchKey,
                spectrumCountingPreferences,
                identificationParameters.getPeptideAssumptionFilter().getMaxPepLength(), identificationParameters);
    }

    /**
     * Returns the spectrum counting index based on the project settings.
     *
     * @param identification the identification
     * @param sequenceFactory the sequence factory
     * @param proteinMatchKey the protein match key
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param maxPepLength the maximal length accepted for a peptide
     * @param identificationParameters the identification parameters
     *
     * @return the spectrum counting index
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public static Double estimateSpectrumCounting(Identification identification, SequenceFactory sequenceFactory, String proteinMatchKey,
            SpectrumCountingPreferences spectrumCountingPreferences, int maxPepLength, IdentificationParameters identificationParameters)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        ProteinMatch testMatch, proteinMatch = identification.getProteinMatch(proteinMatchKey);
        DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();

        if (spectrumCountingPreferences.getSelectedMethod() == SpectralCountingMethod.NSAF) {

            SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();

            // NSAF
            double result = 0;
            int peptideOccurrence = 0;

            PSParameter psParameter = new PSParameter();
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(psParameter);

            // iterate the peptides and store the coverage for each peptide validation level
            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, parameters, null);
            PeptideMatch peptideMatch;
            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                String peptideKey = peptideMatch.getKey();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                if (psParameter.getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel()) {
                    String peptideSequence = Peptide.getSequence(peptideKey);
                    ArrayList<String> possibleProteinMatches = new ArrayList<String>();

                    for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins(sequenceMatchingPreferences)) {
                        if (identification.getProteinMap().get(protein) != null) {
                            for (String proteinKey : identification.getProteinMap().get(protein)) {
                                if (!possibleProteinMatches.contains(proteinKey)) {
                                    try {
                                        testMatch = identification.getProteinMatch(proteinKey);
                                        if (testMatch.getPeptideMatchesKeys().contains(peptideKey)) {
                                            Protein currentProtein = sequenceFactory.getProtein(testMatch.getMainMatch());
                                            peptideOccurrence += currentProtein.getPeptideStart(peptideSequence,
                                                    sequenceMatchingPreferences).size();
                                            possibleProteinMatches.add(proteinKey);
                                        }
                                    } catch (Exception e) {
                                        // protein deleted due to protein inference issue and not deleted from the map in versions earlier than 0.14.6
                                        System.out.println("Non-existing protein key in protein map: " + proteinKey);
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    if (possibleProteinMatches.isEmpty()) {
                        System.err.println("No protein found for the given peptide (" + peptideKey + ") when estimating NSAF of '" + proteinMatchKey + "'.");
                    }

                    double ratio = 1.0 / peptideOccurrence;

                    identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatchesKeys(), psParameter, null, false);
                    for (String spectrumMatchKey : peptideMatch.getSpectrumMatchesKeys()) {
                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumMatchKey, psParameter);
                        if (psParameter.getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel()) {
                            result += ratio;
                        }
                    }
                }
            }

            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

            if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                result /= currentProtein.getObservableLength(digestionPreferences.getEnzymes(), maxPepLength);
            } else {
                result /= currentProtein.getLength();
            }

            if (new Double(result).isInfinite() || new Double(result).isNaN()) {
                result = 0.0;
            }

            return result;
        } else {

            // emPAI
            double result = 0;

            PSParameter psParameter = new PSParameter();

            identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null, false);
            for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                if (psParameter.getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel()) {
                    result++;
                }
            }

            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
            if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                result = Math.pow(10, result / (currentProtein.getNCleavageSites(digestionPreferences.getEnzymes()) + 1)) - 1;
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
     * cleavage settings
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while estimating the probability to observe an amino
     * acid
     */
    public Double getObservableCoverage(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MathException {

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
    public boolean observableCoverageInCache(String proteinMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey) != null;
    }

    /**
     * Updates the best protein coverage possible according to the given
     * cleavage settings. Used when the main key for a protein has been altered.
     *
     * @param proteinMatchKey the key of the protein match of interest
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while estimating the probability to observe an amino
     * acid
     */
    public void updateObservableCoverage(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MathException {
        Double result = estimateObservableCoverage(proteinMatchKey);
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while estimating the probability to observe an amino
     * acid
     */
    private Double estimateObservableCoverage(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MathException {

        DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();
        if (digestionPreferences.getCleavagePreference() != DigestionPreferences.CleavagePreference.enzyme) {
            return 1.0;
        }
        String mainMatch;
        if (ProteinMatch.getNProteins(proteinMatchKey) == 1) {
            mainMatch = proteinMatchKey;
        } else {
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            mainMatch = proteinMatch.getMainMatch();
        }
        Protein currentProtein = sequenceFactory.getProtein(mainMatch);
        double lengthMax = identificationParameters.getPeptideAssumptionFilter().getMaxPepLength();
        if (metrics.getPeptideLengthDistribution() != null) {
            lengthMax = Math.min(lengthMax, metrics.getPeptideLengthDistribution().getValueAtCumulativeProbability(0.99));
        }
        return ((double) currentProtein.getObservableLength(digestionPreferences.getEnzymes(), lengthMax)) / currentProtein.getLength();
    }

    /**
     * Returns the number of validated proteins. Note that this value is only
     * available after getSortedProteinKeys has been called.
     *
     * @return the number of validated proteins
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNValidatedProteins() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (metrics.getnValidatedProteins() == -1) {
            estimateNValidatedProteins();
        }
        return metrics.getnValidatedProteins();
    }

    /**
     * Estimates the number of validated proteins and saves it in the metrics.
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private void estimateNValidatedProteins() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        PSParameter probabilities = new PSParameter();
        int cpt = 0;

        // batch load the protein parameters
        identification.loadProteinMatchParameters(new ArrayList<String>(identification.getProteinIdentification()), probabilities, null, false);

        for (String proteinKey : identification.getProteinIdentification()) {
            if (!ProteinMatch.isDecoy(proteinKey)) {
                probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                if (probabilities.getMatchValidationLevel().isValidated()) {
                    cpt++;
                }
            }
        }

        metrics.setnValidatedProteins(cpt);
    }

    /**
     * Returns the number of confident proteins. Note that this value is only
     * available after getSortedProteinKeys has been called.
     *
     * @return the number of validated proteins
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNConfidentProteins() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (metrics.getnConfidentProteins() == -1) {
            estimateNConfidentProteins();
        }
        return metrics.getnConfidentProteins();
    }

    /**
     * Estimates the number of confident proteins and saves it in the metrics.
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private void estimateNConfidentProteins() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        PSParameter probabilities = new PSParameter();
        int cpt = 0;

        // batch load the protein parameters
        identification.loadProteinMatchParameters(new ArrayList<String>(identification.getProteinIdentification()), probabilities, null, false);

        for (String proteinKey : identification.getProteinIdentification()) {
            if (!ProteinMatch.isDecoy(proteinKey)) {
                probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                if (probabilities.getMatchValidationLevel() == MatchValidationLevel.confident) {
                    cpt++;
                }
            }
        }
        metrics.setnConfidentProteins(cpt);
    }

    /**
     * Estimates the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNValidatedPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int cpt = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter pSParameter = new PSParameter();

        // batch load the peptide match parameters
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), pSParameter, null, false);

        for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
            pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
            if (pSParameter.getMatchValidationLevel().isValidated()) {
                cpt++;
            }
        }

        return cpt;
    }

    /**
     * Estimates the number of confident peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of confident peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNConfidentPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int cpt = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter pSParameter = new PSParameter();

        // batch load the peptide match parameters
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), pSParameter, null, false);

        for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
            pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
            if (pSParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                cpt++;
            }
        }

        return cpt;
    }

    /**
     * Returns the number of unique peptides for this protein match. Note, this
     * is independent of the validation status.
     *
     * @param proteinMatchKey the key of the match
     *
     * @return the number of unique peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNUniquePeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNUniquePeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int cpt = 0;

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null, false, null, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            if (identification.isUniqueInDatabase(peptideMatch.getTheoreticPeptide())) {
                cpt++;
            }
        }
        return cpt;
    }

    /**
     * Returns the number of unique validated peptides for this protein match.
     *
     * @param proteinMatchKey the key of the match
     *
     * @return the number of unique peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNUniqueValidatedPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNUniqueValidatedPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int cpt = 0;

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            if (identification.isUniqueInDatabase(peptideMatch.getTheoreticPeptide())) {
                String peptideKey = peptideMatch.getKey();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    cpt++;
                }
            }
        }
        return cpt;
    }

    /**
     * Returns the number of peptides unique for this protein group. Note, this
     * is independent of the validation status.
     *
     * @param proteinMatchKey the key of the match
     *
     * @return the number of unique peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNUniquePeptidesGroup(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.unique_peptides_group, proteinMatchKey);

        if (result == null) {
            result = estimateNUniquePeptidesGroup(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.unique_peptides_group, proteinMatchKey, result);
        }
        return result;
    }

    /**
     * Estimates the number of peptides unique to a protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of peptides unique to a protein match
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNUniquePeptidesGroup(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int cpt = 0;

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null, false, null, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            if (getNValidatedProteinGroups(peptideMatch.getTheoreticPeptide()) == 1) {
                cpt++;
            }
        }
        return cpt;
    }

    /**
     * Returns the number of unique validated peptides for this protein match.
     *
     * @param proteinMatchKey the key of the match
     *
     * @return the number of unique peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNUniqueValidatedPeptidesGroup(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.unique_validated_peptides_group, proteinMatchKey);

        if (result == null) {
            result = estimateNUniqueValidatedPeptidesGroup(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.unique_validated_peptides_group, proteinMatchKey, result);
        }
        return result;
    }

    /**
     * Estimates the number of validated peptides unique to a protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of peptides unique to a protein match
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNUniqueValidatedPeptidesGroup(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int cpt = 0;

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            if (getNValidatedProteinGroups(peptideMatch.getTheoreticPeptide()) == 1) {
                String peptideKey = peptideMatch.getKey();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    cpt++;
                }
            }
        }
        return cpt;
    }

    /**
     * Returns true if the protein has any enzymatic peptides.
     *
     * @param proteinMatch the protein match
     * @param proteinAccession the protein accession to check
     *
     * @return true if the protein has any enzymatic peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public boolean hasEnzymaticPeptides(ProteinMatch proteinMatch, String proteinAccession) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        Boolean result = (Boolean) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.containsEnzymaticPeptides, proteinAccession);

        if (result == null) {
            result = checkEnzymaticPeptides(proteinMatch, proteinAccession);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.containsEnzymaticPeptides, proteinAccession, result);
        }
        return result;
    }

    /**
     * Returns true if the protein has any enzymatic peptides.
     *
     * @param proteinMatch the protein match
     * @param proteinAccession the protein accession to check
     *
     * @return true if the protein has any enzymatic peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private boolean checkEnzymaticPeptides(ProteinMatch proteinMatch, String proteinAccession) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();
        switch (digestionPreferences.getCleavagePreference()) {
            case enzyme:
                return proteinMatch.hasEnzymaticPeptide(proteinAccession, digestionPreferences.getEnzymes(), identificationParameters.getSequenceMatchingPreferences());
            default:
                return true;
        }
    }

    /**
     * Returns the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated peptides
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNValidatedPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNConfidentPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public void updateNConfidentPeptides(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = estimateNConfidentPeptides(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_confident_peptides, proteinMatchKey, result);
    }

    /**
     * Updates the number of confident spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public void updateNConfidentSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
    public boolean nValidatedPeptidesInCache(String proteinMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey) != null;
    }

    /**
     * Estimates the number of spectra for the given protein match.
     *
     * @param proteinMatchKey the key of the given protein match
     *
     * @return the number of spectra for the given protein match
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public Integer getNSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
    public boolean nSpectraInCache(String proteinMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_spectra, proteinMatchKey) != null;
    }

    /**
     * Returns the number of spectra where this protein was found independently
     * from the validation process.
     *
     * @param proteinMatch the protein match of interest
     *
     * @return the number of spectra where this protein was found
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int result = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null, false, null, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            result += peptideMatch.getSpectrumCount();
        }

        return result;
    }

    /**
     * Returns the maximum number of spectra accounted by a single peptide Match
     * all found in a protein match.
     *
     * @return the maximum number of spectra accounted by a single peptide Match
     * all found in a protein match
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getMaxNSpectra() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        return identificationFeaturesCache.getMaxSpectrumCount();
    }

    /**
     * Returns the number of validated spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     *
     * @return the number of validated spectra
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNValidatedSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNConfidentSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
    public boolean nValidatedSpectraInCache(String proteinMatchKey) {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey);
        return result != null;
    }

    /**
     * Estimates the number of validated spectra for a given protein match.
     *
     * @param proteinMatch the protein match of interest
     *
     * @return the number of spectra where this protein was found
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNValidatedSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int result = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null, false, parameters, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    result++;
                }
            }
        }

        return result;
    }

    /**
     * Estimates the number of confident spectra for a given protein match.
     *
     * @param proteinMatch the protein match of interest
     *
     * @return the number of spectra where this protein was found
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNConfidentSpectra(String proteinMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int result = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), null, false, parameters, null);
        PeptideMatch peptideMatch;
        while ((peptideMatch = peptideMatchesIterator.next()) != null) {
            for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                    result++;
                }
            }
        }

        return result;
    }

    /**
     * Returns the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the key of the peptide match
     *
     * @return the number of validated spectra
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNValidatedSpectraForPeptide(String peptideMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey);

        if (result == null) {
            result = estimateNValidatedSpectraForPeptide(peptideMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey, result);
        }

        return result;
    }

    /**
     * Sets the number of confident spectra for a given peptide match.
     *
     * @param peptideMatchKey the key of the peptide match
     *
     * @return the number of confident spectra
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public int getNConfidentSpectraForPeptide(String peptideMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public void updateNConfidentSpectraForPeptide(String peptideMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
    public boolean nValidatedSpectraForPeptideInCache(String peptideMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey) != null;
    }

    /**
     * Estimates the number of confident spectra for a given peptide match.
     *
     * @param peptideMatchKey the peptide match of interest
     *
     * @return the number of confident spectra where this peptide was found
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNConfidentSpectraForPeptide(String peptideMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int nValidated = 0;

        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);
        PSParameter psParameter = new PSParameter();

        identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatchesKeys(), psParameter, null, false);
        for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
            if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                nValidated++;
            }
        }

        return nValidated;
    }

    /**
     * Estimates the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the peptide match of interest
     *
     * @return the number of validated spectra where this peptide was found
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    private int estimateNValidatedSpectraForPeptide(String peptideMatchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        int nValidated = 0;

        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);
        PSParameter psParameter = new PSParameter();

        identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatchesKeys(), psParameter, null, false);
        for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
            if (psParameter.getMatchValidationLevel().isValidated()) {
                nValidated++;
            }
        }

        return nValidated;
    }

    /**
     * Clears the spectrum counting data in cache.
     */
    public void clearSpectrumCounting() {
        identificationFeaturesCache.removeObjects(IdentificationFeaturesCache.ObjectType.spectrum_counting);
    }

    /**
     * Returns a summary of all PTMs present on the sequence confidently
     * assigned to an amino acid. Example: SEQVEM&lt;mox&gt;CE gives Oxidation
     * of M (M6).
     *
     * @param identificationMatch the identification match
     * @param sequence the sequence
     *
     * @return a PTM summary for the given match
     */
    public String getConfidentPtmSites(IdentificationMatch identificationMatch, String sequence) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) identificationMatch.getUrParam(psPtmScores);

        StringBuilder result = new StringBuilder();

        if (psPtmScores != null) {

            boolean firstPtm = true;
            ArrayList<String> ptms = psPtmScores.getConfidentlyLocalizedPtms();
            Collections.sort(ptms);

            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result.append("; ");
                }
                result.append(ptm);
                result.append("(");
                boolean firstSite = true;
                ArrayList<Integer> sites = psPtmScores.getConfidentSitesForPtm(ptm);
                Collections.sort(sites);
                for (Integer site : sites) {
                    if (!firstSite) {
                        result.append(", ");
                    } else {
                        firstSite = false;
                    }
                    char aa = sequence.charAt(site - 1);
                    result.append(aa).append(site);
                }
                result.append(")");
            }
        }

        return result.toString();
    }

    /**
     * Returns the number of confidently localized variable modifications.
     *
     * @param identificationMatch the identification match
     *
     * @return a PTM summary for the given protein
     */
    public String getConfidentPtmSitesNumber(IdentificationMatch identificationMatch) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) identificationMatch.getUrParam(psPtmScores);

        StringBuilder result = new StringBuilder();

        if (psPtmScores != null) {
            boolean firstPtm = true;
            ArrayList<String> ptms = psPtmScores.getConfidentlyLocalizedPtms();
            Collections.sort(ptms);

            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result.append("; ");
                }
                result.append(ptm);
                result.append("(");
                result.append(psPtmScores.getConfidentSitesForPtm(ptm).size());
                result.append(")");
            }
        }

        return result.toString();
    }

    /**
     * Returns a list of the PTMs present on the sequence ambiguously assigned
     * to an amino acid grouped by representative site followed by secondary
     * ambiguous sites. Example: SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns M6
     * {M9}.
     *
     * @param identificationMatch the identification match
     * @param sequence the sequence
     *
     * @return a PTM summary for the given protein
     */
    public String getAmbiguousPtmSites(IdentificationMatch identificationMatch, String sequence) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) identificationMatch.getUrParam(psPtmScores);
        StringBuilder result = new StringBuilder();

        if (psPtmScores != null) {
            ArrayList<String> ptms = psPtmScores.getAmbiguouslyLocalizedPtms();
            Collections.sort(ptms);
            for (String ptmName : ptms) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(ptmName).append(" (");
                HashMap<Integer, ArrayList<Integer>> sites = psPtmScores.getAmbiguousModificationsSites(ptmName);
                ArrayList<Integer> representativeSites = new ArrayList<Integer>(sites.keySet());
                Collections.sort(representativeSites);
                boolean firstRepresentativeSite = true;
                for (int representativeSite : representativeSites) {
                    if (firstRepresentativeSite) {
                        firstRepresentativeSite = false;
                    } else {
                        result.append(", ");
                    }
                    char aa = sequence.charAt(representativeSite - 1);
                    result.append(aa).append(representativeSite).append("-{");
                    ArrayList<Integer> secondarySites = sites.get(representativeSite);
                    Collections.sort(secondarySites);
                    boolean firstSecondarySite = true;
                    for (Integer secondarySite : secondarySites) {
                        if (firstSecondarySite) {
                            firstSecondarySite = false;
                        } else {
                            result.append(" ");
                        }
                        aa = sequence.charAt(secondarySite - 1);
                        result.append(aa).append(secondarySite);
                    }
                    result.append("}");
                }
                result.append(")");
            }
        }
        return result.toString();
    }

    /**
     * Returns a summary of the number of PTMs present on the sequence
     * ambiguously assigned to an amino acid grouped by representative site
     * followed by secondary ambiguous sites. Example:
     * SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns M6 {M9}.
     *
     * @param identificationMatch the identification match
     *
     * @return a PTM summary for the given protein
     */
    public String getAmbiguousPtmSiteNumber(IdentificationMatch identificationMatch) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) identificationMatch.getUrParam(psPtmScores);
        StringBuilder result = new StringBuilder();

        if (psPtmScores != null) {
            ArrayList<String> ptms = psPtmScores.getAmbiguouslyLocalizedPtms();
            Collections.sort(ptms);

            for (String ptmName : ptms) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(ptmName).append(" (").append(psPtmScores.getAmbiguousModificationsSites(ptmName).size()).append(")");
            }
        }

        return result.toString();
    }

    /**
     * Returns a summary of the PTMs present on the peptide sequence confidently
     * assigned to an amino acid with focus on given PTMs.
     *
     * @param match the identification match
     * @param sequence the sequence
     * @param targetedPtms the PTMs to include in the summary
     *
     * @return a PTM summary for the given protein
     */
    public String getConfidentPtmSites(IdentificationMatch match, String sequence, ArrayList<String> targetedPtms) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) match.getUrParam(psPtmScores);

        StringBuilder result = new StringBuilder();

        if (psPtmScores != null) {

            Collections.sort(targetedPtms);
            ArrayList<Integer> sites = new ArrayList<Integer>();
            for (String ptm : targetedPtms) {
                for (Integer site : psPtmScores.getConfidentSitesForPtm(ptm)) {
                    if (!sites.contains(site)) {
                        sites.add(site);
                    }
                }
            }
            boolean firstSite = true;
            Collections.sort(sites);
            for (Integer site : sites) {
                if (!firstSite) {
                    result.append(", ");
                } else {
                    firstSite = false;
                }
                char aa = sequence.charAt(site - 1);
                result.append(aa).append(site);
            }
        }
        return result.toString();
    }

    /**
     * Returns the number of confidently localized variable modifications.
     *
     * @param match the identification match
     * @param targetedPtms the PTMs to include in the summary
     *
     * @return a PTM summary for the given protein
     */
    public String getConfidentPtmSitesNumber(IdentificationMatch match, ArrayList<String> targetedPtms) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) match.getUrParam(psPtmScores);

        if (psPtmScores != null) {

            ArrayList<Integer> sites = new ArrayList<Integer>();
            for (String ptm : targetedPtms) {
                for (Integer site : psPtmScores.getConfidentSitesForPtm(ptm)) {
                    if (!sites.contains(site)) {
                        sites.add(site);
                    }
                }
            }
            return sites.size() + "";
        }
        return "";
    }

    /**
     * Returns a list of the PTMs present on the sequence ambiguously assigned
     * to an amino acid grouped by representative site followed by secondary
     * ambiguous sites. Example: SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns M6
     * {M9}.
     *
     * @param match the identification match
     * @param sequence the sequence
     * @param targetedPtms the targeted PTMs, can be null
     *
     * @return a PTM summary for the given protein
     */
    public String getAmbiguousPtmSites(IdentificationMatch match, String sequence, ArrayList<String> targetedPtms) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) match.getUrParam(psPtmScores);

        if (psPtmScores != null) {
            HashMap<Integer, ArrayList<String>> reportPerSite = new HashMap<Integer, ArrayList<String>>();

            for (String ptmName : targetedPtms) {

                HashMap<Integer, ArrayList<Integer>> sites = psPtmScores.getAmbiguousModificationsSites(ptmName);
                ArrayList<Integer> representativeSites = new ArrayList<Integer>(sites.keySet());
                Collections.sort(representativeSites);

                for (int representativeSite : representativeSites) {

                    StringBuilder reportAtSite = new StringBuilder();
                    if (reportAtSite.length() > 0) {
                        reportAtSite.append(", ");
                    }

                    char aa = sequence.charAt(representativeSite - 1);
                    reportAtSite.append(aa).append(representativeSite).append("-{");
                    ArrayList<Integer> secondarySites = sites.get(representativeSite);
                    Collections.sort(secondarySites);
                    boolean firstSecondarySite = true;

                    for (Integer secondarySite : secondarySites) {
                        if (firstSecondarySite) {
                            firstSecondarySite = false;
                        } else {
                            reportAtSite.append(" ");
                        }
                        aa = sequence.charAt(secondarySite - 1);
                        reportAtSite.append(aa).append(secondarySite);
                    }

                    reportAtSite.append("}");

                    ArrayList<String> reportsAtSite = reportPerSite.get(representativeSite);
                    if (reportsAtSite == null) {
                        reportsAtSite = new ArrayList<String>();
                        reportPerSite.put(representativeSite, reportsAtSite);
                    }
                    reportsAtSite.add(reportAtSite.toString());
                }
            }

            ArrayList<Integer> sites = new ArrayList<Integer>(reportPerSite.keySet());
            Collections.sort(sites);
            StringBuilder result = new StringBuilder();

            for (int site : sites) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                ArrayList<String> siteReports = reportPerSite.get(site);
                Collections.sort(siteReports);
                for (String siteReport : siteReports) {
                    result.append(siteReport);
                }
            }

            return result.toString();
        }
        return "";
    }

    /**
     * Returns a summary of the number of PTMs present on the sequence
     * ambiguously assigned to an amino acid grouped by representative site
     * followed by secondary ambiguous sites. Example:
     * SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns M6 {M9}.
     *
     * @param match the identification match
     * @param targetedPtms the targeted PTMs, can be null
     *
     * @return a PTM summary for the given protein
     */
    public String getAmbiguousPtmSiteNumber(IdentificationMatch match, ArrayList<String> targetedPtms) {

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) match.getUrParam(psPtmScores);

        if (psPtmScores != null) {
            ArrayList<Integer> sites = new ArrayList<Integer>();

            for (String ptmName : targetedPtms) {
                HashMap<Integer, ArrayList<Integer>> ptmAmbiguousSites = psPtmScores.getAmbiguousModificationsSites(ptmName);
                for (int site : ptmAmbiguousSites.keySet()) {
                    if (!sites.contains(site)) {
                        sites.add(site);
                    }
                }
            }

            return sites.size() + "";
        }
        return "";
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

        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) identificationMatch.getUrParam(psPtmScores);

        if (psPtmScores != null) {
            StringBuilder result = new StringBuilder();

            for (int aa = 0; aa < sequence.length(); aa++) {
                result.append(sequence.charAt(aa));
                int aaNumber = aa + 1;
                if (!psPtmScores.getConfidentModificationsAt(aaNumber).isEmpty()) {
                    boolean first = true;
                    result.append("<");
                    for (String ptmName : psPtmScores.getConfidentModificationsAt(aaNumber)) {
                        if (first) {
                            first = false;
                        } else {
                            result.append(", ");
                        }
                        PTM ptm = ptmFactory.getPTM(ptmName);
                        result.append(ptm.getShortName());
                    }
                    result.append(">");
                }
            }

            return result.toString();
        }
        return "";
    }

    /**
     * Returns the list of validated protein keys. Returns null if the proteins
     * have yet to be validated.
     *
     * @param filterPreferences the filtering preferences used. can be null
     *
     * @return the list of validated protein keys
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getValidatedProteins(FilterPreferences filterPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getValidatedProteins(WaitingHandler waitingHandler, FilterPreferences filterPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        ArrayList<String> result = identificationFeaturesCache.getValidatedProteinList();
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getProcessedProteinKeys(WaitingHandler waitingHandler, FilterPreferences filterPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

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
            HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap
                    = new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
            ArrayList<Double> scores = new ArrayList<Double>();
            PSParameter probabilities = new PSParameter();
            int maxPeptides = 0, maxSpectra = 0;
            double maxSpectrumCounting = 0, maxMW = 0;
            int nValidatedProteins = 0;
            int nConfidentProteins = 0;

            PSParameter psParameter = new PSParameter();
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(psParameter);
            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

            ProteinMatch proteinMatch;
            while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                String proteinKey = proteinMatch.getKey();

                if (!ProteinMatch.isDecoy(proteinKey)) {
                    probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                    if (!probabilities.isHidden()) {
                        double score = probabilities.getProteinProbabilityScore();
                        int nPeptides = -proteinMatch.getPeptideMatchesKeys().size();
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

                            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

                            if (currentProtein != null) {
                                double mw = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                                if (mw > maxMW) {
                                    maxMW = mw;
                                }
                            }

                            if (probabilities.getMatchValidationLevel().isValidated()) {
                                nValidatedProteins++;
                                if (probabilities.getMatchValidationLevel() == MatchValidationLevel.confident) {
                                    nConfidentProteins++;
                                }
                            }
                        }

                        if (!orderMap.containsKey(score)) {
                            orderMap.put(score, new HashMap<Integer, HashMap<Integer, ArrayList<String>>>());
                            scores.add(score);
                        }

                        if (!orderMap.get(score).containsKey(nPeptides)) {
                            orderMap.get(score).put(nPeptides, new HashMap<Integer, ArrayList<String>>());
                        }

                        if (!orderMap.get(score).get(nPeptides).containsKey(nSpectra)) {
                            orderMap.get(score).get(nPeptides).put(nSpectra, new ArrayList<String>());
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

            ArrayList<String> proteinList = new ArrayList<String>();

            ArrayList<Double> scoreList = new ArrayList<Double>(orderMap.keySet());
            Collections.sort(scoreList);

            if (waitingHandler != null) {
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setWaitingText("Updating Protein Table. Please Wait...");
                waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());
            }

            for (double currentScore : scoreList) {

                ArrayList<Integer> nPeptideList = new ArrayList<Integer>(orderMap.get(currentScore).keySet());
                Collections.sort(nPeptideList);

                for (int currentNPeptides : nPeptideList) {

                    ArrayList<Integer> nPsmList = new ArrayList<Integer>(orderMap.get(currentScore).get(currentNPeptides).keySet());
                    Collections.sort(nPsmList);

                    for (int currentNPsms : nPsmList) {
                        ArrayList<String> tempList = orderMap.get(currentScore).get(currentNPeptides).get(currentNPsms);
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

            identificationFeaturesCache.setProteinList(proteinList);

            if (waitingHandler != null) {
                waitingHandler.setPrimaryProgressCounterIndeterminate(true);

                if (waitingHandler.isRunCanceled()) {
                    return null;
                }
            }
        }

        if (hidingNeeded(filterPreferences) || identificationFeaturesCache.getProteinListAfterHiding() == null) {
            ArrayList<String> proteinListAfterHiding = new ArrayList<String>();
            ArrayList<String> validatedProteinList = new ArrayList<String>();
            PSParameter psParameter = new PSParameter();
            int nValidatedProteins = 0;
            int nConfidentProteins = 0;

            for (String proteinKey : identificationFeaturesCache.getProteinList()) {
                if (!ProteinMatch.isDecoy(proteinKey)) {
                    psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                    if (!psParameter.isHidden()) {
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

            identificationFeaturesCache.setProteinListAfterHiding(proteinListAfterHiding);
            identificationFeaturesCache.setValidatedProteinList(validatedProteinList);
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getProteinKeys(WaitingHandler waitingHandler, FilterPreferences filterPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getSortedPeptideKeys(String proteinKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (!proteinKey.equals(identificationFeaturesCache.getCurrentProteinKey()) || identificationFeaturesCache.getPeptideList() == null) {

            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            HashMap<Double, HashMap<Integer, ArrayList<String>>> peptideMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
            int maxSpectrumCount = 0;

            PSParameter psParameter = new PSParameter();
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(psParameter);

            // iterate the peptides and store the coverage for each peptide validation level
            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, parameters, null);
            PeptideMatch peptideMatch;
            while ((peptideMatch = peptideMatchesIterator.next()) != null) {
                String peptideKey = peptideMatch.getKey();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

                if (!psParameter.isHidden()) {
                    double peptideProbabilityScore = psParameter.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {
                        peptideMap.put(peptideProbabilityScore, new HashMap<Integer, ArrayList<String>>());
                    }
                    int spectrumCount = -peptideMatch.getSpectrumCount();
                    if (peptideMatch.getSpectrumCount() > maxSpectrumCount) {
                        maxSpectrumCount = peptideMatch.getSpectrumCount();
                    }
                    if (!peptideMap.get(peptideProbabilityScore).containsKey(spectrumCount)) {
                        peptideMap.get(peptideProbabilityScore).put(spectrumCount, new ArrayList<String>());
                    }
                    peptideMap.get(peptideProbabilityScore).get(spectrumCount).add(peptideKey);
                }
            }

            identificationFeaturesCache.setMaxSpectrumCount(maxSpectrumCount);

            ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
            Collections.sort(scores);
            ArrayList<String> peptideList = new ArrayList<String>();

            for (double currentScore : scores) {
                ArrayList<Integer> nSpectra = new ArrayList<Integer>(peptideMap.get(currentScore).keySet());
                Collections.sort(nSpectra);
                for (int currentNPsm : nSpectra) {
                    ArrayList<String> keys = peptideMap.get(currentScore).get(currentNPsm);
                    Collections.sort(keys);
                    peptideList.addAll(keys);
                }
            }

            identificationFeaturesCache.setPeptideList(peptideList);
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database (from the protein tree or
     * identification)
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object from a database (from the
     * protein tree or identification)
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while interacting with a database (from the
     * protein tree or identification)
     */
    public ArrayList<String> getSortedPsmKeys(String peptideKey, boolean sortOnRt, boolean forceUpdate) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        if (!peptideKey.equals(identificationFeaturesCache.getCurrentPeptideKey()) || identificationFeaturesCache.getPsmList() == null || forceUpdate) {

            PeptideMatch currentPeptideMatch = identification.getPeptideMatch(peptideKey);
            HashMap<Integer, HashMap<Double, ArrayList<String>>> orderingMap = new HashMap<Integer, HashMap<Double, ArrayList<String>>>();
            boolean hasRT = sortOnRt;
            double rt = -1;
            int nValidatedPsms = 0;

            ArrayList<String> spectrumKeys = currentPeptideMatch.getSpectrumMatchesKeys();
            PSParameter psParameter = new PSParameter();
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(psParameter);

            PsmIterator psmIterator = identification.getPsmIterator(spectrumKeys, parameters, false, null);

            SpectrumMatch spectrumMatch;
            while ((spectrumMatch = psmIterator.next()) != null) {

                String spectrumKey = spectrumMatch.getKey();
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (!psParameter.isHidden()) {
                    if (psParameter.getMatchValidationLevel().isValidated()) {
                        nValidatedPsms++;
                    }

                    int charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                    if (!orderingMap.containsKey(charge)) {
                        orderingMap.put(charge, new HashMap<Double, ArrayList<String>>());
                    }
                    if (hasRT) {
                        try {
                            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);
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
                        orderingMap.get(charge).put(rt, new ArrayList<String>());
                    }
                    orderingMap.get(charge).get(rt).add(spectrumKey);
                }
            }

            identificationFeaturesCache.setnValidatedPsms(nValidatedPsms);

            ArrayList<Integer> charges = new ArrayList<Integer>(orderingMap.keySet());
            Collections.sort(charges);
            ArrayList<String> psmList = new ArrayList<String>();

            for (int currentCharge : charges) {
                ArrayList<Double> rts = new ArrayList<Double>(orderingMap.get(currentCharge).keySet());
                Collections.sort(rts);
                for (double currentRT : rts) {
                    ArrayList<String> tempResult = orderingMap.get(currentCharge).get(currentRT);
                    Collections.sort(tempResult);
                    psmList.addAll(tempResult);
                }
            }

            identificationFeaturesCache.setPsmList(psmList);
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
    private boolean hidingNeeded(FilterPreferences filterPreferences) {

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
    public void setProteinKeys(ArrayList<String> proteinList) {
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
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }

    /**
     * Indicates whether a peptide is found in a single protein match.
     *
     * @param peptide the peptide of interest
     *
     * @return true if peptide is found in a single protein match
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * loading the object from the database
     * @throws IOException exception thrown whenever an error occurred while
     * reading the object in the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while casting the database input in the desired match class
     * @throws InterruptedException thrown whenever a threading issue occurred
     * while interacting with the database
     */
    public int getNValidatedProteinGroups(Peptide peptide) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
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
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * loading the object from the database
     * @throws IOException exception thrown whenever an error occurred while
     * reading the object in the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while casting the database input in the desired match class
     * @throws InterruptedException thrown whenever a threading issue occurred
     * while interacting with the database
     */
    public int getNValidatedProteinGroups(Peptide peptide, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        HashSet<String> keys = identification.getProteinMatches(peptide);
        int nValidated = 0;
        PSParameter psParameter = new PSParameter();
        identification.loadProteinMatchParameters(new ArrayList<String>(keys), psParameter, null, false);
        for (String key : keys) {
            psParameter = (PSParameter) identification.getProteinMatchParameter(key, psParameter);
            if (psParameter.getMatchValidationLevel().isValidated()) {
                nValidated++;
            }
        }
        return nValidated;
    }
}
