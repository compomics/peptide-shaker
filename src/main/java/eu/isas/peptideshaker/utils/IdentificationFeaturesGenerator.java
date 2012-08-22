package eu.isas.peptideshaker.utils;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.export.OutputGenerator;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.awt.Color;
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

    /**
     * Instance of the main GUI class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The identification features cache where the recently accessed
     * identification features are stored
     */
    private IdentificationFeaturesCache identificationFeaturesCache = new IdentificationFeaturesCache();

    /**
     * Constructor.
     *
     * @param peptideShakerGUI instance of the main GUI class
     */
    public IdentificationFeaturesGenerator(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
    }

    /**
     * Returns an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @return an array of boolean indicating whether the amino acids of given
     * peptides can generate peptides
     */
    public boolean[] getCoverableAA(String proteinMatchKey) {
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
     */
    public void updateCoverableAA(String proteinMatchKey) {
        boolean[] result = estimateCoverableAA(proteinMatchKey);
        identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.coverable_AA, proteinMatchKey, result);
    }

    /**
     * Returns the modifications found in the currently loaded dataset
     *
     * @return the modifications found in the currently loaded dataset
     */
    public ArrayList<String> getFoundModifications() {
        if (peptideShakerGUI.getMetrics() == null) {
            return new ArrayList<String>();
        }
        ArrayList<String> modifications = peptideShakerGUI.getMetrics().getFoundModifications();
        if (modifications == null) {
            modifications = new ArrayList<String>();
            for (String peptideKey : peptideShakerGUI.getIdentification().getPeptideIdentification()) {
                for (String modification : Peptide.getModificationFamily(peptideKey)) {
                    if (!modifications.contains(modification)) {
                        modifications.add(modification);
                    }
                }
            }
            peptideShakerGUI.getMetrics().setFoundModifications(modifications);
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
    private boolean[] estimateCoverableAA(String proteinMatchKey) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            boolean[] result = new boolean[sequence.length()];
            if (peptideShakerGUI.getSearchParameters().getEnzyme().enzymeCleaves()) {
                int pepMax = peptideShakerGUI.getIdFilter().getMaxPepLength();
                Enzyme enzyme = peptideShakerGUI.getSearchParameters().getEnzyme();
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
            }
            result[sequence.length() - 1] = result[sequence.length() - 2];
            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return new boolean[0];
        }
    }

    /**
     * Returns the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     * @return the sequence coverage
     */
    public Double getSequenceCoverage(String proteinMatchKey) {
        try {
            Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey);

            if (result == null) {
                result = estimateSequenceCoverage(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey, result);
            }
            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return Double.NaN;
        }
    }
    
    /**
     * Updates the sequence coverage of the protein of interest.
     *
     * @param proteinMatchKey the key of the protein of interest
     */
    public void updateSequenceCoverage(String proteinMatchKey) {
        try {
            Double result = estimateSequenceCoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.sequence_coverage, proteinMatchKey, result);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Estimates the sequence coverage for the given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the sequence coverage
     */
    private double estimateSequenceCoverage(String proteinMatchKey) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            // an array containing the coverage index for each residue
            int[] coverage = new int[sequence.length() + 1];
            int peptideTempStart, peptideTempEnd;
            String tempSequence, peptideSequence;
            PSParameter pSParameter = new PSParameter();

            // iterate the peptide table and store the coverage for each peptide
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
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest
     * using the preference settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @return the corresponding spectrum counting metric
     */
    public Double getSpectrumCounting(String proteinMatchKey) {
        return getSpectrumCounting(proteinMatchKey, peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod());
    }

    /**
     * Returns the spectrum counting metric of the protein match of interest for
     * the given method.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @param method the method to use
     * @return the corresponding spectrum counting metric
     */
    public Double getSpectrumCounting(String proteinMatchKey, SpectrumCountingPreferences.SpectralCountingMethod method) {
        try {
            if (method == peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod()) {
                Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey);

                if (result == null) {
                    result = estimateSpectrumCounting(proteinMatchKey);
                    identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.spectrum_counting, proteinMatchKey, result);
                }
                return result;
            } else {
                SpectrumCountingPreferences tempPreferences = new SpectrumCountingPreferences();
                tempPreferences.setSelectedMethod(method);
                return estimateSpectrumCounting(peptideShakerGUI.getIdentification(), sequenceFactory, proteinMatchKey, tempPreferences,
                        peptideShakerGUI.getSearchParameters().getEnzyme(), peptideShakerGUI.getIdFilter().getMaxPepLength());
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return Double.NaN;
        }
    }

    /**
     * Returns the spectrum counting score based on the user's settings.
     *
     * @param proteinMatch the inspected protein match
     * @return the spectrum counting score
     */
    private double estimateSpectrumCounting(String proteinMatchKey) {
        try {
            return estimateSpectrumCounting(peptideShakerGUI.getIdentification(), sequenceFactory, proteinMatchKey,
                    peptideShakerGUI.getSpectrumCountingPreferences(), peptideShakerGUI.getSearchParameters().getEnzyme(),
                    peptideShakerGUI.getIdFilter().getMaxPepLength());
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0.0;
        }
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
                int cpt = 0; // @TODO: this value is not really used?? 

                for (String spectrumMatchKey : peptideMatch.getSpectrumMatches()) {
                    pSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumMatchKey, pSParameter);
                    if (!spectrumCountingPreferences.isValidatedHits() || pSParameter.isValidated()) {
                        result += ratio;
                        cpt++;
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
     */
    public Double getObservableCoverage(String proteinMatchKey) {
        try {
            Double result = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey);

            if (result == null) {
                result = estimateObservableCoverage(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey, result);
            }
            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return Double.NaN;
        }
    }
    
    /**
     * Updates the best protein coverage possible according to the given
     * cleavage settings. Used when the main key for a protein has 
     * been altered.
     *
     * @param proteinMatchKey the key of the protein match of interest
     */
    public void updateObservableCoverage(String proteinMatchKey) {
        try {
            Double result = estimateObservableCoverage(proteinMatchKey);
            identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.expected_coverage, proteinMatchKey, result);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Returns the best protein coverage possible according to the given
     * cleavage settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @return the best protein coverage possible according to the given
     * cleavage settings
     */
    private Double estimateObservableCoverage(String proteinMatchKey) {
        try {
            Enzyme enyzme = peptideShakerGUI.getSearchParameters().getEnzyme();
            Identification identification = peptideShakerGUI.getIdentification();
            String mainMatch;
            if (ProteinMatch.getNProteins(proteinMatchKey) == 1) {
                mainMatch = proteinMatchKey;
            } else {
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
                mainMatch = proteinMatch.getMainMatch();
            }
            Protein currentProtein = sequenceFactory.getProtein(mainMatch);
            return ((double) currentProtein.getObservableLength(enyzme, peptideShakerGUI.getIdFilter().getMaxPepLength())) / currentProtein.getLength();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
            return Double.NaN;
        }
    }

    /**
     * Returns the amount of validated proteins. Note that this value is only
     * available after getSortedProteinKeys has been called.
     *
     * @return the amount of validated proteins
     */
    public int getNValidatedProteins() {
        if (peptideShakerGUI.getMetrics().getnValidatedProteins() == -1) {
            estimateNValidatedProteins();
        }
        return peptideShakerGUI.getMetrics().getnValidatedProteins();
    }

    /**
     * Estimates the amount of validated proteins and saves it in the metrics.
     */
    private void estimateNValidatedProteins() {
        PSParameter probabilities = new PSParameter();
        int cpt = 0;
        try {
            for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
                if (!ProteinMatch.isDecoy(proteinKey)) {
                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(proteinKey, probabilities);
                    if (probabilities.isValidated()) {
                        cpt++;
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        peptideShakerGUI.getMetrics().setnValidatedProteins(cpt);
    }

    /**
     * Estimates the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated peptides
     */
    public int estimateNValidatedPeptides(String proteinMatchKey) {

        int cpt = 0;

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            PSParameter pSParameter = new PSParameter();

            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
                if (pSParameter.isValidated()) {
                    cpt++;
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return cpt;
    }

    /**
     * Returns the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated peptides
     */
    public int getNValidatedPeptides(String proteinMatchKey) {
        try {
            Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey);

            if (result == null) {
                result = estimateNValidatedPeptides(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_peptides, proteinMatchKey, result);
            }

            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Returns the max mz value for all the psms for a given peptide match.
     *
     * @param peptideKey the peptide key
     * @return the max mz value for all the psms
     */
    public double getMaxPsmMzValue(String peptideKey) {
        try {
            Double maxPsmMzValue = (Double) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.max_psm_mz_for_peptides, peptideKey);

            if (maxPsmMzValue == null) {
                maxPsmMzValue = estimateMaxPsmMzValue(peptideKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.max_psm_mz_for_peptides, peptideKey, maxPsmMzValue);
            }

            return maxPsmMzValue;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Returns the max mz value for all the psms for a given peptide match.
     *
     * @param peptideKey the peptide key
     * @return the max mz value for all the psms
     */
    private double estimateMaxPsmMzValue(String peptideMatchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        double maxPsmMzValue = 0;

        try {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);

            if (peptideMatch != null) {
                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                    MSnSpectrum tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                    if (tempSpectrum.getPeakList() != null && maxPsmMzValue < tempSpectrum.getMaxMz()) {
                        maxPsmMzValue = tempSpectrum.getMaxMz();
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return maxPsmMzValue;
    }

    /**
     * Estimates the number of spectra for the given protein match.
     *
     * @param proteinMatchKey the key of the given protein match
     * @return the number of spectra for the given protein match
     */
    public Integer getNSpectra(String proteinMatchKey) {
        try {
            Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_spectra, proteinMatchKey);

            if (result == null) {
                result = estimateNSpectra(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_spectra, proteinMatchKey, result);
            }

            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Returns the number of spectra where this protein was found independantly
     * from the validation process.
     *
     * @param proteinMatch the protein match of interest
     * @return the number of spectra where this protein was found
     */
    private int estimateNSpectra(String proteinMatchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        int result = 0;

        try {
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            PeptideMatch peptideMatch;
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                peptideMatch = identification.getPeptideMatch(peptideKey);
                result += peptideMatch.getSpectrumCount();
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return result;
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
     * @return the number of validated spectra
     */
    public int getNValidatedSpectra(String proteinMatchKey) {
        try {
            Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey);

            if (result == null) {
                result = estimateNValidatedSpectra(proteinMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, proteinMatchKey, result);
            }

            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Estimates the number of validated spectra for a given protein match.
     *
     * @param proteinMatch the protein match of interest
     * @return the number of spectra where this protein was found
     */
    private int estimateNValidatedSpectra(String proteinMatchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        int result = 0;

        try {
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
            PSParameter psParameter = new PSParameter();

            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                    if (psParameter.isValidated()) {
                        result++;
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return result;
    }

    /**
     * Returns the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the key of the peptide match
     * @return the number of validated spectra
     */
    public int getNValidatedSpectraForPeptide(String peptideMatchKey) {
        try {
            Integer result = (Integer) identificationFeaturesCache.getObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey);

            if (result == null) {
                result = estimateNValidatedSpectraForPeptide(peptideMatchKey);
                identificationFeaturesCache.addObject(IdentificationFeaturesCache.ObjectType.number_of_validated_spectra, peptideMatchKey, result);
            }

            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0;
        }
    }

    /**
     * Estimates the number of validated spectra for a given peptide match.
     *
     * @param peptideMatchKey the peptide match of interest
     * @return the number of spectra where this peptide was found
     */
    private int estimateNValidatedSpectraForPeptide(String peptideMatchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        int nValidated = 0;

        try {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);
            PSParameter psParameter = new PSParameter();

            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
                if (psParameter.isValidated()) {
                    nValidated++;
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
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
     * Returns a summary of the PTMs present on the sequence confidently
     * assigned to an amino acid. Example: SEQVEM<mox>CE gives Oxidation of M
     * (M6)
     *
     * @param proteinKey the key of the protein match of interest
     * @return a PTM summary for the given protein
     */
    public String getPrimaryPTMSummary(String proteinKey) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            PSPtmScores psPtmScores = new PSPtmScores();
            psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
            HashMap<String, ArrayList<String>> locations = new HashMap<String, ArrayList<String>>();
            String report;
            int index;

            for (int aa = 0; aa < sequence.length(); aa++) {
                if (!psPtmScores.getMainModificationsAt(aa).isEmpty()) {
                    index = aa + 1;
                    for (String ptm : psPtmScores.getMainModificationsAt(aa)) {
                        if (!locations.containsKey(ptm)) {
                            locations.put(ptm, new ArrayList<String>());
                        }
                        report = sequence.charAt(aa) + "" + index;
                        if (!locations.get(ptm).contains(report)) {
                            locations.get(ptm).add(report);
                        }
                    }
                }
            }

            String result = "";
            boolean firstSite, firstPtm = true;
            ArrayList<String> ptms = new ArrayList<String>(locations.keySet());
            Collections.sort(ptms);

            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result += "; ";
                }
                result += ptm + "(";
                firstSite = true;
                for (String site : locations.get(ptm)) {
                    if (!firstSite) {
                        result += ", ";
                    } else {
                        firstSite = false;
                    }
                    result += site;
                }
                result += ")";
            }
            result += OutputGenerator.SEPARATOR;
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result += "; ";
                }
                result += ptm + "(" + locations.get(ptm).size() + ")";
            }
            return result;
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            return "IO exception";
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

    /**
     * Returns a summary of the PTMs present on the sequence not confidently
     * assigned to an amino acid. Example: SEQVEM<mox>CE gives Oxidation of M
     * (M6)
     *
     * @param proteinKey the key of the protein match of interest
     * @return a PTM summary for the given protein
     */
    public String getSecondaryPTMSummary(String proteinKey) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            PSPtmScores psPtmScores = new PSPtmScores();
            psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
            HashMap<String, ArrayList<String>> locations = new HashMap<String, ArrayList<String>>();
            String report;
            int index;

            for (int aa = 0; aa < sequence.length(); aa++) {
                if (!psPtmScores.getSecondaryModificationsAt(aa).isEmpty()) {
                    index = aa + 1;
                    for (String ptm : psPtmScores.getSecondaryModificationsAt(aa)) {
                        if (!locations.containsKey(ptm)) {
                            locations.put(ptm, new ArrayList<String>());
                        }
                        report = sequence.charAt(aa) + "" + index;
                        if (!locations.get(ptm).contains(report)) {
                            locations.get(ptm).add(report);
                        }
                    }
                }
            }

            String result = "";
            boolean firstSite, firstPtm = true;
            ArrayList<String> ptms = new ArrayList<String>(locations.keySet());
            Collections.sort(ptms);
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result += "; ";
                }
                result += ptm + "(";
                firstSite = true;
                for (String site : locations.get(ptm)) {
                    if (!firstSite) {
                        result += ", ";
                    } else {
                        firstSite = false;
                    }
                    result += site;
                }
                result += ")";
            }
            result += OutputGenerator.SEPARATOR;
            for (String ptm : ptms) {
                if (firstPtm) {
                    firstPtm = false;
                } else {
                    result += "; ";
                }
                result += ptm + "(" + locations.get(ptm).size() + ")";
            }
            return result;
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            return "IO exception";
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

    /**
     * Returns the protein sequence annotated with modifications.
     *
     * @param proteinKey the key of the protein match
     * @return the protein sequence annotated with modifications
     */
    public String getModifiedSequence(String proteinKey) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            String result = "";
            PSPtmScores psPtmScores = new PSPtmScores();
            psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);

            for (int aa = 0; aa < sequence.length(); aa++) {
                result += sequence.charAt(aa);
                if (!psPtmScores.getMainModificationsAt(aa).isEmpty()) {
                    boolean first = true;
                    result += "<";
                    for (String ptm : psPtmScores.getMainModificationsAt(aa)) {
                        if (first) {
                            first = false;
                        } else {
                            result += ", ";
                        }
                        result += ptmFactory.getPTM(ptm).getShortName();
                    }
                    result += ">";
                }
            }

            return result;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return "Error";
        }
    }

    /**
     * Transforms the protein accession number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteinAccession the protein to get the database link for
     * @return the transformed accession number
     */
    public String addDatabaseLink(String proteinAccession) {

        String accessionNumberWithLink = proteinAccession;

        try {
            if (sequenceFactory.getHeader(proteinAccession) != null) {

                // try to find the database from the SequenceDatabase
                DatabaseType databaseType = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                // create the database link
                if (databaseType != null) {

                    // @TODO: support more databases

                    if (databaseType == DatabaseType.IPI || databaseType == DatabaseType.UniProt) {
                        accessionNumberWithLink = "<html><a href=\"" + getUniProtAccessionLink(proteinAccession)
                                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else if (databaseType == DatabaseType.NCBI) {
                        accessionNumberWithLink = "<html><a href=\"" + getNcbiAccessionLink(proteinAccession)
                                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else {
                        // unknown database!
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return accessionNumberWithLink;
    }

    /**
     * Transforms the protein accesion number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteins the list of proteins to get the database links for
     * @return the transformed accession number
     */
    public String addDatabaseLinks(ArrayList<String> proteins) {

        if (proteins.isEmpty()) {
            return "";
        }

        String accessionNumberWithLink = "<html>";

        for (int i = 0; i < proteins.size(); i++) {

            String proteinAccession = proteins.get(i);
            try {
                if (!SequenceFactory.isDecoy(proteins.get(i)) && sequenceFactory.getHeader(proteinAccession) != null) {

                    // try to find the database from the SequenceDatabase
                    DatabaseType database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                    // create the database link
                    if (database != null) {

                        // @TODO: support more databases

                        if (database == DatabaseType.IPI || database == DatabaseType.UniProt) {
                            accessionNumberWithLink += "<a href=\"" + getUniProtAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else if (database == DatabaseType.NCBI) {
                            accessionNumberWithLink += "<a href=\"" + getNcbiAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else {
                            // unknown database!
                            accessionNumberWithLink += proteinAccession + ", ";
                        }
                    }
                } else {
                    accessionNumberWithLink += proteinAccession + ", ";
                }
            } catch (Exception e) {
                accessionNumberWithLink += proteinAccession + ", ";
            }
        }

        // remove the last ', '
        accessionNumberWithLink = accessionNumberWithLink.substring(0, accessionNumberWithLink.length() - 2);
        accessionNumberWithLink += "</html>";

        return accessionNumberWithLink;
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://srs.ebi.ac.uk.
     *
     * @param proteinAccession the protein accession number
     * @param database the protein database
     * @return the protein accession web link
     */
    public String getSrsAccessionLink(String proteinAccession, String database) {
        return "http://srs.ebi.ac.uk/srsbin/cgi-bin/wgetz?-e+%5b" + database + "-AccNumber:" + proteinAccession + "%5d";
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://www.uniprot.org/uniprot.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getUniProtAccessionLink(String proteinAccession) {
        return "http://www.uniprot.org/uniprot/" + proteinAccession;
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://www.ncbi.nlm.nih.gov/protein.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getNcbiAccessionLink(String proteinAccession) {
        return "http://www.ncbi.nlm.nih.gov/protein/" + proteinAccession;
    }

    /**
     * Returns a String with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide
     * @return a String with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        String tooltip = "<html>";
        ArrayList<ModificationMatch> modifications = peptide.getModificationMatches();
        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        for (int i = 0; i < modifications.size(); i++) {

            PTM ptm = ptmFactory.getPTM(modifications.get(i).getTheoreticPtm());

            if (ptm.getType() == PTM.MODAA && modifications.get(i).isVariable()) {

                int modSite = modifications.get(i).getModificationSite();
                String modName = modifications.get(i).getTheoreticPtm();
                char affectedResidue = peptide.getSequence().charAt(modSite - 1);
                Color ptmColor = peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(modifications.get(i).getTheoreticPtm());

                if (!alreadyAnnotated.contains(modName + "_" + affectedResidue)) {
                    tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                            + affectedResidue
                            + "</span>"
                            + ": " + modName + "<br>";

                    alreadyAnnotated.add(modName + "_" + affectedResidue);
                }
            }
        }

        if (!tooltip.equalsIgnoreCase("<html>")) {
            tooltip += "</html>";
        } else {
            tooltip = null;
        }

        return tooltip;
    }

    /**
     * Returns the peptide with modification sites colored on the sequence.
     * Shall be used for peptides, not PSMs.
     *
     * @param peptideKey the peptide key
     * @param includeHtmlStartEndTag if true, html start and end tags are added
     * @return the colored peptide sequence
     */
    public String getColoredPeptideSequence(String peptideKey, boolean includeHtmlStartEndTag) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            PSPtmScores ptmScores = new PSPtmScores();
            ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
            if (ptmScores != null) {
                HashMap<Integer, ArrayList<String>> mainLocations = ptmScores.getMainModificationSites();
                HashMap<Integer, ArrayList<String>> secondaryLocations = ptmScores.getSecondaryModificationSites();
                return Peptide.getModifiedSequenceAsHtml(peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(),
                        includeHtmlStartEndTag, peptideMatch.getTheoreticPeptide(),
                        mainLocations, secondaryLocations);
            } else {
                return peptideMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                        peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), includeHtmlStartEndTag);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return "Error";
        }
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
     * @param progressDialog the progress dialog, can be null
     * @return the sorted list of protein keys
     */
    public ArrayList<String> getProcessedProteinKeys(ProgressDialogX progressDialog) {
        try {
            if (identificationFeaturesCache.getProteinList() == null) {
                if (progressDialog != null) {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setTitle("Loading Protein Information. Please Wait...");
                    progressDialog.setMaxProgressValue(peptideShakerGUI.getIdentification().getProteinIdentification().size());
                    progressDialog.setValue(0);
                }
                boolean needMaxValues = (peptideShakerGUI.getMetrics().getMaxNPeptides() == null)
                        || (peptideShakerGUI.getMetrics().getMaxNPeptides() <= 0)
                        || (peptideShakerGUI.getMetrics().getMaxNSpectra() == null)
                        || (peptideShakerGUI.getMetrics().getMaxNSpectra() <= 0)
                        || (peptideShakerGUI.getMetrics().getMaxSpectrumCounting() == null)
                        || (peptideShakerGUI.getMetrics().getMaxSpectrumCounting() <= 0)
                        || (peptideShakerGUI.getMetrics().getMaxMW() == null)
                        || (peptideShakerGUI.getMetrics().getMaxMW() <= 0);

                // sort the proteins according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
                HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap =
                        new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
                ArrayList<Double> scores = new ArrayList<Double>();
                PSParameter probabilities = new PSParameter();
                int maxPeptides = 0, maxSpectra = 0;
                double maxSpectrumCounting = 0, maxMW = 0;
                Protein currentProtein = null;
                int nValidatedProteins = 0;

                for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {

                    if (!SequenceFactory.isDecoy(proteinKey)) {
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(proteinKey, probabilities);
                        if (!probabilities.isHidden()) {
                            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                            double score = probabilities.getProteinProbabilityScore();
                            int nPeptides = -proteinMatch.getPeptideMatches().size();
                            int nSpectra = -peptideShakerGUI.getIdentificationFeaturesGenerator().getNSpectra(proteinKey);

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

                                try {
                                    currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                                } catch (Exception e) {
                                    peptideShakerGUI.catchException(e);
                                }

                                if (currentProtein != null) {
                                    double mw = currentProtein.computeMolecularWeight() / 1000;
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

                    if (progressDialog != null) {
                        progressDialog.increaseProgressValue();
                    }
                }

                if (needMaxValues) {
                    peptideShakerGUI.getMetrics().setMaxNPeptides(maxPeptides);
                    peptideShakerGUI.getMetrics().setMaxNSpectra(maxSpectra);
                    peptideShakerGUI.getMetrics().setMaxSpectrumCounting(maxSpectrumCounting);
                    peptideShakerGUI.getMetrics().setMaxMW(maxMW);
                    peptideShakerGUI.getMetrics().setnValidatedProteins(nValidatedProteins);
                }

                ArrayList<String> proteinList = new ArrayList<String>();

                ArrayList<Double> scoreList = new ArrayList<Double>(orderMap.keySet());
                Collections.sort(scoreList);

                if (progressDialog != null) {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setTitle("Updating Protein Table. Please Wait...");
                    progressDialog.setMaxProgressValue(peptideShakerGUI.getIdentification().getProteinIdentification().size());
                    progressDialog.setValue(0);
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
                            if (progressDialog != null) {
                                progressDialog.increaseProgressValue(tempList.size());
                            }
                        }
                    }
                }

                identificationFeaturesCache.setProteinList(proteinList);

                if (progressDialog != null) {
                    progressDialog.setIndeterminate(true);
                }
            }

            if (hidingNeeded() || identificationFeaturesCache.getProteinListAfterHiding() == null) {
                ArrayList<String> proteinListAfterHiding = new ArrayList<String>();
                ArrayList<String> validatedProteinList = new ArrayList<String>();
                PSParameter psParameter = new PSParameter();
                int nValidatedProteins = 0;

                for (String proteinKey : identificationFeaturesCache.getProteinList()) {
                    psParameter = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(proteinKey, psParameter);
                    if (!psParameter.isHidden()) {
                        proteinListAfterHiding.add(proteinKey);
                        if (psParameter.isValidated()) {
                            nValidatedProteins++;
                            validatedProteinList.add(proteinKey);
                        }
                    }
                }
                identificationFeaturesCache.setProteinListAfterHiding(proteinListAfterHiding);
                identificationFeaturesCache.setValidatedProteinList(validatedProteinList);
                peptideShakerGUI.getMetrics().setnValidatedProteins(nValidatedProteins);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            identificationFeaturesCache.setProteinListAfterHiding(new ArrayList<String>());
        }

        return identificationFeaturesCache.getProteinListAfterHiding();
    }

    /**
     * Returns the ordered protein keys to display when no filtering is applied.
     *
     * @param progressDialogX can be null
     * @return the ordered protein keys to display when no filtering is applied.
     */
    public ArrayList<String> getProteinKeys(ProgressDialogX progressDialogX) {
        if (identificationFeaturesCache.getProteinList() == null) {
            getProcessedProteinKeys(progressDialogX);
        }
        return identificationFeaturesCache.getProteinList();
    }

    /**
     * Repopulates the cache with the details of nProteins proteins first
     * proteins
     *
     * @param nProteins the number of proteins to load in the cache
     * @param waitingHandler a waiting handler displaying progress to the user.
     * can be null. The progress will be displayed as secondary progress.
     */
    public void repopulateCache(int nProteins, WaitingHandler waitingHandler) {

        //long start = System.currentTimeMillis();

        try {
            if (waitingHandler != null) {
                waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                waitingHandler.setMaxSecondaryProgressValue(2 * nProteins);
                waitingHandler.setSecondaryProgressValue(0);
            }

            for (int i = 0; i < nProteins; i++) {

                String proteinKey = identificationFeaturesCache.getProteinList().get(i);
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);

                if (proteinMatch == null) {
                    throw new IllegalArgumentException("Protein match " + proteinKey + " not found.");
                }

                getSequenceCoverage(proteinKey);
                getObservableCoverage(proteinKey);

                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressValue();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }

                getNValidatedPeptides(proteinKey);
                getNValidatedSpectra(proteinKey);
                getSpectrumCounting(proteinKey);

                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressValue();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }

                if (!peptideShakerGUI.getCache().memoryCheck()) {
                    break;
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        //long end = System.currentTimeMillis();  
        //System.out.println("loading proteins total: " + (end-start));
    }

    /**
     * Returns a sorted list of peptide keys from the protein of interest.
     *
     * @param proteinKey the key of the protein of interest
     * @return a sorted list of the corresponding peptide keys
     */
    public ArrayList<String> getSortedPeptideKeys(String proteinKey) {
        try {
            if (!proteinKey.equals(identificationFeaturesCache.getCurrentProteinKey()) || identificationFeaturesCache.getPeptideList() == null) {

                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                HashMap<Double, HashMap<Integer, ArrayList<String>>> peptideMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
                PSParameter probabilities = new PSParameter();
                int maxSpectrumCount = 0;

                for (String peptideKey : proteinMatch.getPeptideMatches()) {

                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideKey, probabilities); // @TODO: replace by batch selection?

                    if (!probabilities.isHidden()) {
                        double peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                        if (!peptideMap.containsKey(peptideProbabilityScore)) {
                            peptideMap.put(peptideProbabilityScore, new HashMap<Integer, ArrayList<String>>());
                        }
                        PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
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
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            identificationFeaturesCache.setPeptideList(new ArrayList<String>());
        }
        return identificationFeaturesCache.getPeptideList();
    }

    /**
     * Returns the ordered list of spectrum keys for a given peptide.
     *
     * @param peptideKey the key of the peptide of interest
     * @return the ordered list of spectrum keys
     */
    public ArrayList<String> getSortedPsmKeys(String peptideKey) {
        try {
            if (!peptideKey.equals(identificationFeaturesCache.getCurrentPeptideKey()) || identificationFeaturesCache.getPsmList() == null) {

                PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                HashMap<Integer, HashMap<Double, ArrayList<String>>> orderingMap = new HashMap<Integer, HashMap<Double, ArrayList<String>>>();
                boolean hasRT = true;
                double rt = -1;
                PSParameter psParameter = new PSParameter();
                int nValidatedPsms = 0;

                for (String spectrumKey : currentPeptideMatch.getSpectrumMatches()) {

                    psParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, psParameter); // @TODO: could be replaced by batch selection?

                    if (!psParameter.isHidden()) {
                        if (psParameter.isValidated()) {
                            nValidatedPsms++;
                        }

                        SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey); // @TODO: could be replaced by batch selection?
                        int charge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                        if (!orderingMap.containsKey(charge)) {
                            orderingMap.put(charge, new HashMap<Double, ArrayList<String>>());
                        }
                        if (hasRT) {
                            try {
                                rt = peptideShakerGUI.getPrecursor(spectrumKey).getRt();

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
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            identificationFeaturesCache.setPsmList(new ArrayList<String>());
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
     * @return a boolean indicating whether hiding proteins is necessary
     */
    private boolean hidingNeeded() {

        if (identificationFeaturesCache.isFiltered()) {
            return true;
        }

        for (ProteinFilter proteinFilter : peptideShakerGUI.getFilterPreferences().getProteinHideFilters().values()) {
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
