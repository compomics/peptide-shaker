package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.IdFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class provides identification features and stores them in cache.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class IdentificationFeaturesGenerator {

    // @TODO: moved to utilities once the back-end allows it!!!
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
     * The search parameters.
     */
    private SearchParameters searchParameters;
    /**
     * The identification filter.
     */
    private IdFilter idFilter;
    /**
     * The spectrum counting preferences.
     */
    private SpectrumCountingPreferences spectrumCountingPreferences;

    /**
     * Constructor.
     *
     * @param identification the identification of interest
     * @param searchParameters the search parameters
     * @param idFilter the identification filter
     * @param metrics the metrics picked-up wile loading the data
     * @param spectrumCountingPreferences the spectrum counting preferences
     */
    public IdentificationFeaturesGenerator(Identification identification, SearchParameters searchParameters, IdFilter idFilter, Metrics metrics, SpectrumCountingPreferences spectrumCountingPreferences) {
        this.metrics = metrics;
        this.idFilter = idFilter;
        this.searchParameters = searchParameters;
        this.identification = identification;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }

    /**
     * Returns an array of booleans indicating whether the amino acids of given
     * peptides can generate peptides.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @return an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public boolean[] getCoverableAA(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        boolean[] result = (boolean[]) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.coverable_AA, proteinMatchKey);
        if (result == null) {
            result = estimateCoverableAA(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.coverable_AA, proteinMatchKey, result);
        }
        return result;
    }

    /**
     * Updates the array of booleans indicating whether the amino acids of given
     * peptides can generate peptides. Used when the main key for a protein has
     * been altered.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void updateCoverableAA(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        boolean[] result = estimateCoverableAA(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.coverable_AA, proteinMatchKey, result);
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
     * Returns an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @return an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides
     */
    private boolean[] estimateCoverableAA(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        boolean[] result = new boolean[sequence.length() + 1];

        if (searchParameters.getEnzyme().enzymeCleaves()) {
            int pepMax = idFilter.getMaxPepLength();
            Enzyme enzyme = searchParameters.getEnzyme();
            int cleavageAA = 0;
            int lastCleavage = 0;
            while (++cleavageAA < sequence.length() - 2) {
                if (enzyme.getAminoAcidAfter().contains(sequence.charAt(cleavageAA + 1)) && !enzyme.getRestrictionBefore().contains(sequence.charAt(cleavageAA))
                        || enzyme.getAminoAcidBefore().contains(sequence.charAt(cleavageAA)) && !enzyme.getRestrictionAfter().contains(sequence.charAt(cleavageAA + 1))) {
                    if (cleavageAA - lastCleavage <= pepMax) {
                        for (int i = lastCleavage + 1; i <= cleavageAA; i++) {
                            result[i] = true;
                        }
                    }
                    lastCleavage = cleavageAA;
                }
            }

            // add the last peptide if short enough
            if (sequence.length() - lastCleavage < pepMax) {
                for (int i = lastCleavage; i < sequence.length(); i++) {
                    result[i] = true;
                }
            }
        }

        result[sequence.length() - 1] = result[sequence.length() - 2];
        return result;
    }

    /**
     * Returns the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @return the sequence coverage
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public Double getSequenceCoverage(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey);

        if (result == null) {
            result = estimateSequenceCoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey, result);
        }
        return result;
    }

    /**
     * Indicates whether the sequence coverage is in cache.
     *
     * @param proteinMatchKey the key of the protein match
     * @return true if the sequence coverage is in cache
     */
    public boolean sequenceCoverageInCache(String proteinMatchKey) {
        Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey);
        return result != null;
    }

    /**
     * Returns a list of non-enzymatic peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @param enzyme the enzyme used
     * @return a list of non-enzymatic peptides for a given protein match
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public ArrayList<String> getNonEnzymatic(String proteinMatchKey, Enzyme enzyme) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        ArrayList<String> result = (ArrayList<String>) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.tryptic_protein, proteinMatchKey);

        if (result == null) {
            result = estimateNonEnzymatic(proteinMatchKey, enzyme);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.tryptic_protein, proteinMatchKey, result);
        }
        return result;
    }

    /**
     * Returns a list of non-enzymatic peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @param enzyme the enzyme used
     * @return a list of non-enzymatic peptides for a given protein match
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    private ArrayList<String> estimateNonEnzymatic(String proteinMatchKey, Enzyme enzyme) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        ArrayList<String> peptideKeys = proteinMatch.getPeptideMatches();
        PSParameter peptidePSParameter = new PSParameter();

        identification.loadPeptideMatchParameters(peptideKeys, peptidePSParameter, null);

        ArrayList<String> result = new ArrayList<String>();

        // see if we have non-tryptic peptides
        for (String peptideKey : peptideKeys) {

            peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptidePSParameter);

            if (peptidePSParameter.isValidated()) {

                String peptideSequence = Peptide.getSequence(peptideKey);
                boolean enzymatic = false;
                for (String accession : ProteinMatch.getAccessions(proteinMatchKey)) {
                    Protein currentProtein = sequenceFactory.getProtein(accession);
                    if (currentProtein.isEnzymaticPeptide(peptideSequence, enzyme)) {
                        enzymatic = true;
                        break;
                    }
                }

                if (!enzymatic) {
                    result.add(peptideKey);
                }
            }
        }
        return result;
    }

    /**
     * Updates the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     */
    public void updateSequenceCoverage(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Double result = estimateSequenceCoverage(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey, result);
    }

    /**
     * Estimates the sequence coverage for the given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the sequence coverage
     */
    private double estimateSequenceCoverage(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        // an array containing the coverage index for each residue
        int[] coverage = new int[sequence.length() + 1];
        int peptideTempStart, peptideTempEnd;
        String tempSequence, peptideSequence;
        PSParameter pSParameter = new PSParameter();

        // iterate the peptide table and store the coverage for each peptide
        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), pSParameter, null);
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
            if (pSParameter.isValidated()) {
                tempSequence = sequence;
                peptideSequence = Peptide.getSequence(peptideKey);
                while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                    peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                    peptideTempEnd = peptideTempStart + peptideSequence.length();
                    for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                        coverage[j] = 1;
                    }
                    tempSequence = sequence.substring(0, peptideTempStart);
                }
            }
        }

        double covered = 0.0;

        for (int aa : coverage) {
            covered += aa;
        }

        return covered / ((double) sequence.length());
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @return the corresponding spectrum counting metric
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public Double getSpectrumCounting(String proteinMatchKey) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {
        return getSpectrumCounting(proteinMatchKey, spectrumCountingPreferences.getSelectedMethod());
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest for
     * the given method.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @param method the method to use
     * @return the corresponding spectrum counting metric
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public Double getSpectrumCounting(String proteinMatchKey, SpectrumCountingPreferences.SpectralCountingMethod method) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {
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
                    searchParameters.getEnzyme(), idFilter.getMaxPepLength());
        }
    }

    /**
     * Indicates whether the default spectrum counting value is in cache for a
     * protein match.
     *
     * @param proteinMatchKey the key of the protein match of interest
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
     * @return the spectrum counting score
     */
    private double estimateSpectrumCounting(String proteinMatchKey) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {
        return estimateSpectrumCounting(identification, sequenceFactory, proteinMatchKey,
                spectrumCountingPreferences, searchParameters.getEnzyme(),
                idFilter.getMaxPepLength());
    }

    /**
     * Returns the spectrum counting index based on the project settings.
     *
     * @param identification the identification
     * @param sequenceFactory the sequence factory
     * @param proteinMatchKey the protein match key
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param enzyme the enzyme used
     * @param maxPepLength the maximal length accepted for a peptide
     * @return the spectrum counting index
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static Double estimateSpectrumCounting(Identification identification, SequenceFactory sequenceFactory, String proteinMatchKey,
            SpectrumCountingPreferences spectrumCountingPreferences, Enzyme enzyme, int maxPepLength) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {

        PSParameter pSParameter = new PSParameter();
        ProteinMatch testMatch, proteinMatch = identification.getProteinMatch(proteinMatchKey);

        if (spectrumCountingPreferences.getSelectedMethod() == SpectralCountingMethod.NSAF) {

            // NSAF

            double result = 0;
            int peptideOccurrence = 0;

            identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
            for (String peptideKey : proteinMatch.getPeptideMatches()) {

                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                ArrayList<String> possibleProteinMatches = new ArrayList<String>();

                for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                    if (identification.getProteinMap().get(protein) != null) {
                        for (String proteinKey : identification.getProteinMap().get(protein)) {
                            if (!possibleProteinMatches.contains(proteinKey)) {
                                try {
                                    testMatch = identification.getProteinMatch(proteinKey);
                                    if (testMatch.getPeptideMatches().contains(peptideKey)) {
                                        Protein currentProtein = sequenceFactory.getProtein(testMatch.getMainMatch());
                                        peptideOccurrence += currentProtein.getPeptideStart(Peptide.getSequence(peptideKey)).size();
                                        possibleProteinMatches.add(proteinKey);
                                    }
                                } catch (Exception e) {
                                    // protein deleted due to protein inference issue and not deleted from the map in versions earlier than 0.14.6
                                    System.out.println("Non-existing protein key in protein map:" + proteinKey);
                                }
                            }
                        }
                    }
                }

                if (possibleProteinMatches.isEmpty()) {
                    System.err.println("No protein found for the given peptide (" + peptideKey + ") when estimating NSAF of '" + proteinMatchKey + "'.");
                }

                double ratio = 1.0 / peptideOccurrence;

                identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), pSParameter, null);
                for (String spectrumMatchKey : peptideMatch.getSpectrumMatches()) {
                    pSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumMatchKey, pSParameter);
                    if (!spectrumCountingPreferences.isValidatedHits() || pSParameter.isValidated()) {
                        result += ratio;
                    }
                }
            }

            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

            if (enzyme.enzymeCleaves()) {
                result /= currentProtein.getObservableLength(enzyme, maxPepLength);
            } else {
                result /= currentProtein.getLength();
            }

            if (new Double(result).isInfinite() || new Double(result).isNaN()) {
                result = 0.0;
            }

            return result;
        } else {

            // emPAI
            double result;

            if (spectrumCountingPreferences.isValidatedHits()) {

                result = 0;

                identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), pSParameter, null);
                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
                    if (pSParameter.isValidated()) {
                        result++;
                    }
                }
            } else {
                result = proteinMatch.getPeptideCount();
            }

            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
            result = Math.pow(10, result / currentProtein.getNPossiblePeptides(enzyme)) - 1;

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
     * @return the best protein coverage possible according to the given
     * cleavage settings
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public Double getObservableCoverage(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void updateObservableCoverage(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Double result = estimateObservableCoverage(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey, result);
    }

    /**
     * Returns the best protein coverage possible according to the given
     * cleavage settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @return the best protein coverage possible according to the given
     * cleavage settings
     */
    private Double estimateObservableCoverage(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Enzyme enyzme = searchParameters.getEnzyme();
        String mainMatch;
        if (ProteinMatch.getNProteins(proteinMatchKey) == 1) {
            mainMatch = proteinMatchKey;
        } else {
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            mainMatch = proteinMatch.getMainMatch();
        }
        Protein currentProtein = sequenceFactory.getProtein(mainMatch);
        return ((double) currentProtein.getObservableLength(enyzme, idFilter.getMaxPepLength())) / currentProtein.getLength();
    }

    /**
     * Returns the amount of validated proteins. Note that this value is only
     * available after getSortedProteinKeys has been called.
     *
     * @return the amount of validated proteins
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public int getNValidatedProteins() throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (metrics.getnValidatedProteins() == -1) {
            estimateNValidatedProteins();
        }
        return metrics.getnValidatedProteins();
    }

    /**
     * Estimates the amount of validated proteins and saves it in the metrics.
     */
    private void estimateNValidatedProteins() throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        PSParameter probabilities = new PSParameter();
        int cpt = 0;
        for (String proteinKey : identification.getProteinIdentification()) {
            if (!ProteinMatch.isDecoy(proteinKey)) {
                probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                if (probabilities.isValidated()) {
                    cpt++;
                }
            }
        }
        metrics.setnValidatedProteins(cpt);
    }

    /**
     * Estimates the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated peptides
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private int estimateNValidatedPeptides(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        int cpt = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter pSParameter = new PSParameter();

        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), pSParameter, null);
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
            if (pSParameter.isValidated()) {
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
     * @return the number of unique peptides
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException  
     */
    public int getNUniquePeptides(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     * @return the number of peptides unique to a protein match
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private int estimateNUniquePeptides(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int cpt = 0;

        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            if (identification.isUnique(peptideMatch.getTheoreticPeptide())) {
                cpt++;
            }
        }
        return cpt;
    }

    /**
     * Returns the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated peptides
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public int getNValidatedPeptides(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey);

        if (result == null) {
            result = estimateNValidatedPeptides(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey, result);
        }

        return result;
    }

    /**
     * Indicates whether the number of validated peptides is in cache for a
     * given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return true if the information is in cache
     */
    public boolean nValidatedPeptidesInCache(String proteinMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey) != null;
    }

    /**
     * Estimates the number of spectra for the given protein match.
     *
     * @param proteinMatchKey the key of the given protein match
     * @return the number of spectra for the given protein match
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public Integer getNSpectra(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
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
     * @return the number of spectra where this protein was found
     */
    private int estimateNSpectra(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        int result = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PeptideMatch peptideMatch;
        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            peptideMatch = identification.getPeptideMatch(peptideKey);
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
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public int getMaxNSpectra() throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        return identificationFeaturesCache.getMaxSpectrumCount();
    }

    /**
     * Returns the number of validated spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated spectra
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public int getNValidatedSpectra(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey);

        if (result == null) {
            result = estimateNValidatedSpectra(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey, result);
        }

        return result;
    }

    /**
     * Indicates whether the number of validated spectra is in cache for the
     * given protein match.
     *
     * @param proteinMatchKey the key of the protein match
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
     * @return the number of spectra where this protein was found
     */
    private int estimateNValidatedSpectra(String proteinMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        int result = 0;

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter psParameter = new PSParameter();

        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                if (psParameter.isValidated()) {
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
     * @return the number of validated spectra
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public int getNValidatedSpectraForPeptide(String peptideMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey);

        if (result == null) {
            result = estimateNValidatedSpectraForPeptide(peptideMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey, result);
        }

        return result;
    }

    /**
     * Indicates whether the number of validated spectra for a peptide match is
     * in cache.
     *
     * @param peptideMatchKey the key of the peptide match
     * @return true if the data is in cache
     */
    public boolean nValidatedSpectraForPeptideInCache(String peptideMatchKey) {
        return identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey) != null;
    }

    /**
     * Estimates the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the peptide match of interest
     * @return the number of spectra where this peptide was found
     */
    private int estimateNValidatedSpectraForPeptide(String peptideMatchKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        int nValidated = 0;

        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);
        PSParameter psParameter = new PSParameter();

        identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);
        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
            if (psParameter.isValidated()) {
                nValidated++;
            }
        }

        return nValidated;
    }

    /**
     * Clears the spectrum counting data in cache
     */
    public void clearSpectrumCounting() {
        identificationFeaturesCache.removeObjects(IdentificationFeaturesCache.ObjectType.spectrum_counting);
    }

    /**
     * Returns a summary of all PTMs present on the sequence confidently
     * assigned to an amino acid. Example: SEQVEM&lt;mox&gt;CE gives Oxidation
     * of M (M6)
     *
     * @param proteinKey the key of the protein match of interest
     * @param separator the separator used to separate the sites and the number
     * of sites
     * @return a PTM summary for the given protein
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public String getPrimaryPTMSummary(String proteinKey, String separator) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        return getPrimaryPTMSummary(proteinKey, null, separator);
    }

    /**
     * Returns a summary of the PTMs present on the sequence confidently
     * assigned to an amino acid with focus on given PTMs. Example:
     * SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K returns 6, 9.
     *
     * @param proteinKey the key of the protein match of interest
     * @param targetedPtms the PTMs to include in the summary
     * @param separator the separator used to separate the sites and the number
     * of sites
     * @return a PTM summary for the given protein
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public String getPrimaryPTMSummary(String proteinKey, ArrayList<String> targetedPtms, String separator) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
        HashMap<String, ArrayList<String>> locations = new HashMap<String, ArrayList<String>>();

        for (int aa = 0; aa < sequence.length(); aa++) {
            if (!psPtmScores.getMainModificationsAt(aa).isEmpty()) {
                int index = aa + 1;
                for (String ptm : psPtmScores.getMainModificationsAt(aa)) {
                    if (!locations.containsKey(ptm)) {
                        locations.put(ptm, new ArrayList<String>());
                    }
                    String report = sequence.charAt(aa) + "" + index;
                    if (!locations.get(ptm).contains(report)) {
                        locations.get(ptm).add(report);
                    }
                }
            }
        }

        StringBuilder result = new StringBuilder();
        boolean firstPtm = true;
        ArrayList<String> ptms = new ArrayList<String>(locations.keySet());
        Collections.sort(ptms);

        if (targetedPtms == null) {
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result.append("; ");
                }
                result.append(ptm);
                result.append("(");
                boolean firstSite = true;
                for (String site : locations.get(ptm)) {
                    if (!firstSite) {
                        result.append(", ");
                    } else {
                        firstSite = false;
                    }
                    result.append(site);
                }
                result.append(")");
            }
        } else {
            Collections.sort(targetedPtms);
            for (String ptm : targetedPtms) {
                if (locations.containsKey(ptm)) {
                    for (String site : locations.get(ptm)) {
                        if (result.length() > 0) {
                            result.append(", ");
                        }
                        result.append(site);
                    }
                }
            }
        }

        result.append(separator);

        firstPtm = true;

        if (targetedPtms == null) {
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result.append("; ");
                }
                result.append(ptm);
                result.append("(");
                result.append(locations.get(ptm).size());
                result.append(")");
            }
        } else {
            int n = 0;
            for (String ptm : targetedPtms) {
                if (locations.containsKey(ptm)) {
                    n += locations.get(ptm).size();
                }
            }
            result.append(n);
        }

        String resultsAsString = result.toString();

        if (resultsAsString.equalsIgnoreCase("|")) {
            resultsAsString = "";
        }

        return resultsAsString;
    }

    /**
     * Returns a summary of the PTMs present on the sequence not confidently
     * assigned to an amino acid. Example: SEQVEM&lt;mox&gt;CE gives Oxidation
     * of M (M6)
     *
     * @param proteinKey the key of the protein match of interest
     * @param separator the separator used to separate the sites and the number
     * of sites
     * @return a PTM summary for the given protein
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public String getSecondaryPTMSummary(String proteinKey, String separator) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        return getSecondaryPTMSummary(proteinKey, null, separator);
    }

    /**
     * Returns a summary of the PTMs present on the sequence not confidently
     * assigned to an amino acid. Example: SEQVEM&lt;mox&gt;CEM&lt;mox&gt;K
     * returns 6, 9.
     *
     * @param proteinKey the key of the protein match of interest
     * @param targetedPtms the targeted PTMs, can be null
     * @param separator the separator used to separate the sites and the number
     * of sites
     * @return a PTM summary for the given protein
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public String getSecondaryPTMSummary(String proteinKey, ArrayList<String> targetedPtms, String separator) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
        HashMap<String, ArrayList<String>> locations = new HashMap<String, ArrayList<String>>();

        for (int aa = 0; aa < sequence.length(); aa++) {
            if (!psPtmScores.getSecondaryModificationsAt(aa).isEmpty()) {
                int index = aa + 1;
                for (String ptm : psPtmScores.getSecondaryModificationsAt(aa)) {
                    if (!locations.containsKey(ptm)) {
                        locations.put(ptm, new ArrayList<String>());
                    }
                    String report = sequence.charAt(aa) + "" + index;
                    if (!locations.get(ptm).contains(report)) {
                        locations.get(ptm).add(report);
                    }
                }
            }
        }

        StringBuilder result = new StringBuilder();
        boolean firstPtm = true;
        ArrayList<String> ptms = new ArrayList<String>(locations.keySet());
        Collections.sort(ptms);

        if (targetedPtms == null) {
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result.append("; ");
                }
                result.append(ptm);
                result.append("(");
                boolean firstSite = true;
                for (String site : locations.get(ptm)) {
                    if (!firstSite) {
                        result.append(", ");
                    } else {
                        firstSite = false;
                    }
                    result.append(site);
                }
                result.append(")");
            }
        } else {
            Collections.sort(targetedPtms);
            for (String ptm : targetedPtms) {
                if (locations.containsKey(ptm)) {
                    for (String site : locations.get(ptm)) {
                        if (result.length() > 0) {
                            result.append(", ");
                        }
                        result.append(site);
                    }
                }
            }
        }

        result.append(separator);

        firstPtm = true;

        if (targetedPtms == null) {
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result.append("; ");
                }
                result.append(ptm);
                result.append("(");
                result.append(locations.get(ptm).size());
                result.append(")");
            }
        } else {
            int n = 0;
            for (String ptm : targetedPtms) {
                if (locations.containsKey(ptm)) {
                    n += locations.get(ptm).size();
                }
            }
            result.append(n);
        }

        String resultsAsString = result.toString();

        if (resultsAsString.equalsIgnoreCase("|")) {
            resultsAsString = "";
        }

        return resultsAsString;
    }

    /**
     * Returns the protein sequence annotated with modifications.
     *
     * @param proteinKey the key of the protein match
     * @return the protein sequence annotated with modifications
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public String getModifiedSequence(String proteinKey) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
        PSPtmScores psPtmScores = new PSPtmScores();
        psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
        StringBuilder result = new StringBuilder();

        for (int aa = 0; aa < sequence.length(); aa++) {
            result.append(sequence.charAt(aa));
            if (!psPtmScores.getMainModificationsAt(aa).isEmpty()) {
                boolean first = true;
                result.append("<");
                for (String ptm : psPtmScores.getMainModificationsAt(aa)) {
                    if (first) {
                        first = false;
                    } else {
                        result.append(", ");
                    }
                    result.append(ptmFactory.getShortName(ptm));
                }
                result.append(">");
            }
        }

        return result.toString();
    }

    /**
     * Returns the list of validated protein keys. Returns null if the proteins
     * have yet to be validated.
     *
     * @return the list of validated protein keys
     */
    public ArrayList<String> getValidatedProteins() {
        return identificationFeaturesCache.getValidatedProteinList();
    }

    /**
     * Returns the sorted list of protein keys.
     *
     * @param filterPreferences the filtering preferences used. can be null
     * @param waitingHandler the waiting handler, can be null
     * @return the sorted list of protein keys
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public ArrayList<String> getProcessedProteinKeys(WaitingHandler waitingHandler, FilterPreferences filterPreferences) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
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
            HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap =
                    new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
            ArrayList<Double> scores = new ArrayList<Double>();
            PSParameter probabilities = new PSParameter();
            int maxPeptides = 0, maxSpectra = 0;
            double maxSpectrumCounting = 0, maxMW = 0;
            int nValidatedProteins = 0;

            for (String proteinKey : identification.getProteinIdentification()) {

                if (!sequenceFactory.isDecoyAccession(proteinKey)) {
                    probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                    if (!probabilities.isHidden()) {
                        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                        double score = probabilities.getProteinProbabilityScore();
                        int nPeptides = -proteinMatch.getPeptideMatches().size();
                        int nSpectra = -getNSpectra(proteinKey);

                        if (needMaxValues) {

                            if (-nPeptides > maxPeptides) {
                                maxPeptides = -nPeptides;
                            }

                            if (-nSpectra > maxSpectra) {
                                maxSpectra = -nSpectra;
                            }

                            double tempSpectrumCounting = estimateSpectrumCounting(proteinKey);

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

                            if (probabilities.isValidated()) {
                                nValidatedProteins++;
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

            for (String proteinKey : identificationFeaturesCache.getProteinList()) {
                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                if (!psParameter.isHidden()) {
                    proteinListAfterHiding.add(proteinKey);
                    if (psParameter.isValidated()) {
                        nValidatedProteins++;
                        validatedProteinList.add(proteinKey);
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
        }

        return identificationFeaturesCache.getProteinListAfterHiding();
    }

    /**
     * Returns the ordered protein keys to display when no filtering is applied.
     *
     * @param waitingHandler can be null
     * @param filterPreferences the filtering preferences used. can be null
     * @return the ordered protein keys to display when no filtering is applied.
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public ArrayList<String> getProteinKeys(WaitingHandler waitingHandler, FilterPreferences filterPreferences) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (identificationFeaturesCache.getProteinList() == null) {
            getProcessedProteinKeys(waitingHandler, filterPreferences);
        }
        return identificationFeaturesCache.getProteinList();
    }

    /**
     * Returns a sorted list of peptide keys from the protein of interest.
     *
     * @param proteinKey the key of the protein of interest
     * @return a sorted list of the corresponding peptide keys
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public ArrayList<String> getSortedPeptideKeys(String proteinKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (!proteinKey.equals(identificationFeaturesCache.getCurrentProteinKey()) || identificationFeaturesCache.getPeptideList() == null) {

            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            HashMap<Double, HashMap<Integer, ArrayList<String>>> peptideMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
            PSParameter probabilities = new PSParameter();
            int maxSpectrumCount = 0;

            identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
            identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), probabilities, null);
            for (String peptideKey : proteinMatch.getPeptideMatches()) {

                probabilities = (PSParameter) identification.getPeptideMatchParameter(peptideKey, probabilities); // @TODO: replace by batch selection?

                if (!probabilities.isHidden()) {
                    double peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {
                        peptideMap.put(peptideProbabilityScore, new HashMap<Integer, ArrayList<String>>());
                    }
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
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
     * @return the ordered list of spectrum keys
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public ArrayList<String> getSortedPsmKeys(String peptideKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (!peptideKey.equals(identificationFeaturesCache.getCurrentPeptideKey()) || identificationFeaturesCache.getPsmList() == null) {

            PeptideMatch currentPeptideMatch = identification.getPeptideMatch(peptideKey);
            HashMap<Integer, HashMap<Double, ArrayList<String>>> orderingMap = new HashMap<Integer, HashMap<Double, ArrayList<String>>>();
            boolean hasRT = true;
            double rt = -1;
            PSParameter psParameter = new PSParameter();
            int nValidatedPsms = 0;

            identification.loadSpectrumMatchParameters(currentPeptideMatch.getSpectrumMatches(), psParameter, null);
            identification.loadSpectrumMatches(currentPeptideMatch.getSpectrumMatches(), null);
            for (String spectrumKey : currentPeptideMatch.getSpectrumMatches()) {

                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter); // @TODO: could be replaced by batch selection?

                if (!psParameter.isHidden()) {
                    if (psParameter.isValidated()) {
                        nValidatedPsms++;
                    }

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey); // @TODO: could be replaced by batch selection?
                    int charge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                    if (!orderingMap.containsKey(charge)) {
                        orderingMap.put(charge, new HashMap<Double, ArrayList<String>>());
                    }
                    if (hasRT) {
                        try {

                            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey, true);
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
     * Returns the number of validated psms for the last selected peptide /!\
     * This value is only available after getSortedPsmKeys has been called.
     *
     * @return the number of validated psms for the last selected peptide
     */
    public int getNValidatedPsms() {
        return identificationFeaturesCache.getnValidatedPsms();
    }

    /**
     * Returns a boolean indicating whether hiding proteins is necessary.
     *
     * @param filterPreferences the filtering preferences used. can be null
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
}
