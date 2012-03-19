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
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class provides identification features at the protein level
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
     * The number of values kept in memory for small objects.
     */
    private final int smallObjectsCacheSize = 1000;
    /**
     * The number of values kept in memory for big objects.
     */
    private final int bigObjectsCacheSize = 3;
    /**
     * The cached protein matches for small objects.
     */
    private ArrayList<String> smallObjectsCache = new ArrayList<String>();
    /**
     * The cached protein matches for big objects.
     */
    private ArrayList<String> bigObjectsCache = new ArrayList<String>();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The sequence coverage of the main match of the loaded protein match.
     */
    private HashMap<String, Double> sequenceCoverage = new HashMap<String, Double>();
    /**
     * The possible sequence coverage of the main match of the loaded protein.
     * match
     */
    private HashMap<String, Double> possibleCoverage = new HashMap<String, Double>();
    /**
     * The spectrum counting metric of the loaded protein match.
     */
    private HashMap<String, Double> spectrumCounting = new HashMap<String, Double>();
    /**
     * The number of spectra.
     */
    private HashMap<String, Integer> numberOfSpectra = new HashMap<String, Integer>();
    /**
     * The number of validated spectra.
     */
    private HashMap<String, Integer> numberOfValidatedSpectra = new HashMap<String, Integer>();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * A map containing the list of coverable amino acids for each protein in
     * the big object cache.
     */
    private HashMap<String, boolean[]> coverableAA = new HashMap<String, boolean[]>();
    /**
     * The current protein key.
     */
    private String currentProteinKey = "";
    /**
     * The current peptide key.
     */
    private String currentPeptideKey = "";
    /**
     * The protein list.
     */
    private ArrayList<String> proteinListAfterHiding = null;
    /**
     * back-up list for when proteins are hidden
     */
    private ArrayList<String> proteinList = null;
    /**
     * The peptide list.
     */
    private ArrayList<String> peptideList;
    /**
     * The psm list.
     */
    private ArrayList<String> psmList;
    /**
     * Boolean indicating whether a filtering was already used. If yes, proteins
     * might need to be unhiden.
     */
    private boolean filtered = false;

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
        boolean[] result = coverableAA.get(proteinMatchKey);
        if (result == null) {
            if (bigObjectsCache.size() >= bigObjectsCacheSize) {
                int nRemove = bigObjectsCache.size() - bigObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();
                for (String tempKey : bigObjectsCache) {
                    if (coverableAA.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }
                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }
            result = estimateCoverableAA(proteinMatchKey);
            coverableAA.put(proteinMatchKey, result);
            bigObjectsCache.remove(proteinMatchKey);
            bigObjectsCache.add(proteinMatchKey);
        }
        return result;
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

        Double result = sequenceCoverage.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (sequenceCoverage.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }

            result = estimateSequenceCoverage(proteinMatchKey);
            sequenceCoverage.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }
        return result;
    }

    /**
     * Removes a key from the cache.
     *
     * @param key the key to remove
     */
    public void removeFromSmallCache(String key) {
        smallObjectsCache.remove(key);
        sequenceCoverage.remove(key);
        possibleCoverage.remove(key);
        spectrumCounting.remove(key);
        numberOfValidatedSpectra.remove(key);
        numberOfSpectra.remove(key);
    }

    /**
     * Removes a key from the cache.
     *
     * @param key the key to remove
     */
    public void removeFromBigCache(String key) {
        bigObjectsCache.remove(key);
        coverableAA.remove(key);
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
                pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
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

        if (method == peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod()) {
            Double result = spectrumCounting.get(proteinMatchKey);

            if (result == null) {
                if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                    int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                    ArrayList<String> toRemove = new ArrayList<String>();

                    for (String tempKey : smallObjectsCache) {
                        if (spectrumCounting.containsKey(tempKey)) {
                            toRemove.add(tempKey);
                            if (toRemove.size() == nRemove) {
                                break;
                            }
                        }
                    }

                    for (String tempKey : toRemove) {
                        removeFromSmallCache(tempKey);
                    }
                }

                result = estimateSpectrumCounting(proteinMatchKey);
                spectrumCounting.put(proteinMatchKey, result);
                smallObjectsCache.remove(proteinMatchKey);
                smallObjectsCache.add(proteinMatchKey);
            }
            return result;
        } else {
            SpectrumCountingPreferences tempPreferences = new SpectrumCountingPreferences();
            tempPreferences.setSelectedMethod(method);
            try {
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinMatchKey);
                Protein mainMatch = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                return estimateSpectrumCounting(peptideShakerGUI.getIdentification(), proteinMatchKey, mainMatch, tempPreferences, peptideShakerGUI.getSearchParameters().getEnzyme(), peptideShakerGUI.getIdFilter().getMaxPepLength());
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                return 0.0;
            }
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
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinMatchKey);
            Protein mainMatch = sequenceFactory.getProtein(proteinMatch.getMainMatch());
            return estimateSpectrumCounting(peptideShakerGUI.getIdentification(), proteinMatchKey, mainMatch, peptideShakerGUI.getSpectrumCountingPreferences(), peptideShakerGUI.getSearchParameters().getEnzyme(), peptideShakerGUI.getIdFilter().getMaxPepLength());
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return 0.0;
        }
    }

    /**
     * Returns the spectrum counting index based on the project settings
     *
     * @param identification the identification
     * @param proteinMatchKey the protein match key
     * @param mainMatch the main protein of the match
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param enzyme the enzyme used
     * @param maxPepLength the maximal length accepted for a peptide
     * @return the spectrum counting index
     */
    public static double estimateSpectrumCounting(Identification identification, String proteinMatchKey, Protein mainMatch, SpectrumCountingPreferences spectrumCountingPreferences, Enzyme enzyme, int maxPepLength) {

        double ratio, result;
        PSParameter pSParameter = new PSParameter();
        ProteinMatch testMatch, proteinMatch = identification.getProteinMatch(proteinMatchKey);
        if (spectrumCountingPreferences.getSelectedMethod() == SpectralCountingMethod.NSAF) {

            // NSAF

            if (mainMatch == null) {
                return 0.0;
            }
            result = 0;
            PeptideMatch peptideMatch;
            ArrayList<String> possibleProteinMatches;
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                peptideMatch = identification.getPeptideMatch(peptideKey);
                possibleProteinMatches = new ArrayList<String>();
                for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                    if (identification.getProteinMap().get(protein) != null) {
                        for (String proteinKey : identification.getProteinMap().get(protein)) {
                            if (!possibleProteinMatches.contains(proteinKey)) {
                                try {
                                    testMatch = identification.getProteinMatch(proteinKey);
                                    if (testMatch.getPeptideMatches().contains(peptideKey)) {
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
                    System.err.println("No protein found for the given peptide (" + peptideKey + ") when estimating NSAF of " + mainMatch + ".");
                }
                ratio = 1.0 / possibleProteinMatches.size();
                int cpt = 0;
                for (String spectrumMatchKey : peptideMatch.getSpectrumMatches()) {
                    pSParameter = (PSParameter) identification.getMatchParameter(spectrumMatchKey, pSParameter);
                    if (!spectrumCountingPreferences.isValidatedHits() || pSParameter.isValidated()) {
                        result += ratio;
                        cpt++;
                    }
                }
            }

            if (enzyme.enzymeCleaves()) {
                return result / mainMatch.getObservableLength(enzyme, maxPepLength);
            } else {
                return result / mainMatch.getLength();
            }
        } else {

            // emPAI

            if (spectrumCountingPreferences.isValidatedHits()) {
                result = 0;

                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
                    if (pSParameter.isValidated()) {
                        result++;
                    }
                }
            } else {
                result = proteinMatch.getPeptideCount();
            }

            return Math.pow(10, result / mainMatch.getNPossiblePeptides(enzyme)) - 1;
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

        Double result = possibleCoverage.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (possibleCoverage.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }

            result = estimateObservableCoverage(proteinMatchKey);
            possibleCoverage.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }
        return result;
    }

    /**
     * Returns the best protein coverage possible according to the given
     * cleavage settings.
     *
     * @param proteinMatchKey the key of the protein match of interest
     * @return the best protein coverage possible according to the given
     * cleavage settings
     */
    private double estimateObservableCoverage(String proteinMatchKey) {
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
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Returns the list of indexes where a peptide can be found in a protein
     * sequence
     *
     * @param accession the protein accession
     * @param peptide the sequence of the peptide of interest
     * @return the list of indexes where a peptide can be found in a protein
     * sequence
     * @throws IOException Exception thrown whenever an error occurred while
     * parsing the protein sequence
     */
    public ArrayList<Integer> getPeptideStart(String accession, String peptide) throws IOException {
        ArrayList<Integer> result = new ArrayList<Integer>();
        String tempSequence = sequenceFactory.getProtein(accession).getSequence();
        while (tempSequence.lastIndexOf(peptide) >= 0) {
            int startIndex = tempSequence.lastIndexOf(peptide);
            result.add(startIndex + 1);
            tempSequence = tempSequence.substring(0, startIndex);
        }
        return result;
    }

    /**
     * Returns the amino acids surrounding a peptide in the sequence of the
     * given protein in a map: peptide start index -> (amino acids before, amino
     * acids after) The number of amino acids is taken from the display
     * preferences
     *
     * @param accession the protein accession
     * @param peptide the sequence of the peptide of interest
     * @return the amino acids surrounding a peptide in the protein sequence
     * @throws IOException Exception thrown whenever an error occurred while
     * parsing the protein sequence
     */
    public HashMap<Integer, String[]> getSurroundingAA(String accession, String peptide) throws IOException {
        ArrayList<Integer> startIndexes = getPeptideStart(accession, peptide);
        HashMap<Integer, String[]> result = new HashMap<Integer, String[]>();
        String proteinSequence = sequenceFactory.getProtein(accession).getSequence();
        String subsequence;
        int nAA = peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides();
        for (int startIndex : startIndexes) {
            result.put(startIndex, new String[2]);
            subsequence = "";
            for (int aa = startIndex - nAA; aa < startIndex; aa++) {
                if (aa >= 0 && aa < proteinSequence.length()) {
                    subsequence += proteinSequence.charAt(aa);
                }
            }
            result.get(startIndex)[0] = subsequence;
            subsequence = "";
            for (int aa = startIndex + peptide.length(); aa < startIndex + peptide.length() + nAA; aa++) {
                if (aa >= 0 && aa < proteinSequence.length()) {
                    subsequence += proteinSequence.charAt(aa);
                }
            }
            result.get(startIndex)[1] = subsequence;
        }
        return result;
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
        for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
            if (!ProteinMatch.isDecoy(proteinKey)) {
                probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, probabilities);
                if (probabilities.isValidated()) {
                    cpt++;
                }
            }
        }
        peptideShakerGUI.getMetrics().setnValidatedProteins(cpt);
    }

    /**
     * Returns the number of validated peptides for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated peptides
     */
    public int getNValidatedPeptides(String proteinMatchKey) {
        Identification identification = peptideShakerGUI.getIdentification();
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        PSParameter pSParameter = new PSParameter();
        int cpt = 0;
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
            if (pSParameter.isValidated()) {
                cpt++;
            }
        }
        return cpt;
    }

    /**
     * Estimates the number of spectra for the given protein match.
     *
     * @param proteinMatchKey the key of the given protein match
     * @return the number of spectra for the given protein match
     */
    public Integer getNSpectra(String proteinMatchKey) {
        Integer result = numberOfSpectra.get(proteinMatchKey);

        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (numberOfSpectra.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }
            result = estimateNSpectra(proteinMatchKey);
            numberOfSpectra.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }

        return result;
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
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int result = 0;

        try {
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
     * all found in a protein match
     *
     * @param proteinMatchKey the key of the protein match
     * @return the maximum number of spectra accounted by a single peptide Match
     * all found in a protein match
     */
    public int getMaxNSpectra(String proteinMatchKey) {
        PeptideMatch peptideMatch;
        ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinMatchKey);
        int nSpectraMax = 0;
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
            if (peptideMatch.getSpectrumCount() > nSpectraMax) {
                nSpectraMax = peptideMatch.getSpectrumCount();
            }
        }
        return nSpectraMax;
    }

    /**
     * Returns the number of validated spectra for a given protein match.
     *
     * @param proteinMatchKey the key of the protein match
     * @return the number of validated peptides
     */
    public int getNValidatedSpectra(String proteinMatchKey) {
        Integer result = numberOfValidatedSpectra.get(proteinMatchKey);
        if (result == null) {
            if (smallObjectsCache.size() >= smallObjectsCacheSize) {
                int nRemove = smallObjectsCache.size() - smallObjectsCacheSize + 1;
                ArrayList<String> toRemove = new ArrayList<String>();

                for (String tempKey : smallObjectsCache) {
                    if (numberOfValidatedSpectra.containsKey(tempKey)) {
                        toRemove.add(tempKey);
                        if (toRemove.size() == nRemove) {
                            break;
                        }
                    }
                }

                for (String tempKey : toRemove) {
                    removeFromSmallCache(tempKey);
                }
            }
            result = estimateNValidatedSpectra(proteinMatchKey);
            numberOfValidatedSpectra.put(proteinMatchKey, result);
            smallObjectsCache.remove(proteinMatchKey);
            smallObjectsCache.add(proteinMatchKey);
        }

        return result;
    }

    /**
     * Returns the number of validated spectra for a given protein match.
     *
     * @param proteinMatch the protein match of interest
     * @return the number of spectra where this protein was found
     */
    private int estimateNValidatedSpectra(String proteinMatchKey) {
        Identification identification = peptideShakerGUI.getIdentification();
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
        int result = 0;
        try {
            PeptideMatch peptideMatch;
            PSParameter psParameter = new PSParameter();
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                peptideMatch = identification.getPeptideMatch(peptideKey);
                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                    psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
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
                if (ptm.equals("phosphorylation")) {
                    if (firstPtm) {
                        firstPtm = false;
                    } else {
                        result += "; ";
                    }
                    firstSite = true;
                    for (String site : locations.get(ptm)) {
                        if (!firstSite) {
                            result += ", ";
                        } else {
                            firstSite = false;
                        }
                        result += site;
                    }
                }
            }
            result += "\t";
            if (locations.containsKey("phosphorylation")) {
                result += locations.get("phosphorylation").size();
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
                        if (ptm.equals("phosphorylation")) {
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
                firstSite = true;
                for (String site : locations.get(ptm)) {
                    if (!firstSite) {
                        result += ", ";
                    } else {
                        firstSite = false;
                    }
                    result += site;
                }
            }
            result += "\t";
            if (locations.containsKey("phosphorylation")) {
                result += locations.get("phosphorylation").size();
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
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            return "IO exception";
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
    }

    /**
     * Returns the sorted list of protein keys.
     *
     * @param progressDialog the progress dialog, can be null
     * @return the sorted list of protein keys
     */
    public ArrayList<String> getProcessedProteinKeys(ProgressDialogX progressDialog) {
        if (proteinListAfterHiding == null) {
            if (progressDialog != null) {
                progressDialog.setIndeterminate(false);
                progressDialog.setTitle("Loading Protein Information. Please Wait...");
                progressDialog.setMax(peptideShakerGUI.getIdentification().getProteinIdentification().size());
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
            ProteinMatch proteinMatch;
            double score;
            int nPeptides, nSpectra, maxPeptides = 0, maxSpectra = 0;
            double tempSpectrumCounting, maxSpectrumCounting = 0, mw, maxMW = 0;
            Protein currentProtein = null;
            int nValidatedProteins = 0;

            for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {

                if (!SequenceFactory.isDecoy(proteinKey)) {
                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, probabilities);
                    if (!probabilities.isHidden()) {
                        proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                        score = probabilities.getProteinProbabilityScore();
                        nPeptides = -proteinMatch.getPeptideMatches().size();
                        nSpectra = -peptideShakerGUI.getIdentificationFeaturesGenerator().getNSpectra(proteinKey);

                        if (needMaxValues) {

                            if (-nPeptides > maxPeptides) {
                                maxPeptides = -nPeptides;
                            }

                            if (-nSpectra > maxSpectra) {
                                maxSpectra = -nSpectra;
                            }

                            tempSpectrumCounting = estimateSpectrumCounting(proteinKey);

                            if (tempSpectrumCounting > maxSpectrumCounting) {
                                maxSpectrumCounting = tempSpectrumCounting;
                            }

                            try {
                                currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                            }

                            if (currentProtein != null) {
                                mw = currentProtein.computeMolecularWeight() / 1000;
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
                    progressDialog.incrementValue();
                }
            }

            if (needMaxValues) {
                peptideShakerGUI.getMetrics().setMaxNPeptides(maxPeptides);
                peptideShakerGUI.getMetrics().setMaxNSpectra(maxSpectra);
                peptideShakerGUI.getMetrics().setMaxSpectrumCounting(maxSpectrumCounting);
                peptideShakerGUI.getMetrics().setMaxMW(maxMW);
                peptideShakerGUI.getMetrics().setnValidatedProteins(nValidatedProteins);
            }

            proteinList = new ArrayList<String>();

            ArrayList<Double> scoreList = new ArrayList<Double>(orderMap.keySet());
            Collections.sort(scoreList);
            ArrayList<Integer> nPeptideList, nPsmList;
            ArrayList<String> tempList;

            if (progressDialog != null) {
                progressDialog.setIndeterminate(false);
                progressDialog.setTitle("Updating Protein Table. Please Wait...");
                progressDialog.setMax(peptideShakerGUI.getIdentification().getProteinIdentification().size());
                progressDialog.setValue(0);
            }

            for (double currentScore : scoreList) {

                nPeptideList = new ArrayList<Integer>(orderMap.get(currentScore).keySet());
                Collections.sort(nPeptideList);

                for (int currentNPeptides : nPeptideList) {

                    nPsmList = new ArrayList<Integer>(orderMap.get(currentScore).get(currentNPeptides).keySet());
                    Collections.sort(nPsmList);

                    for (int currentNPsms : nPsmList) {
                        tempList = orderMap.get(currentScore).get(currentNPeptides).get(currentNPsms);
                        Collections.sort(tempList);
                        proteinList.addAll(tempList);
                        if (progressDialog != null) {
                            progressDialog.incrementValue(tempList.size());
                        }
                    }
                }
            }

            if (progressDialog != null) {
                progressDialog.setIndeterminate(true);
            }
        }
        if (hidingNeeded()) {
            proteinListAfterHiding = new ArrayList<String>();
            PSParameter psParameter = new PSParameter();
            int nValidatedProteins = 0;
            for (String proteinKey : proteinList) {
                psParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, psParameter);
                if (!psParameter.isHidden()) {
                    proteinListAfterHiding.add(proteinKey);
                    if (psParameter.isValidated()) {
                        nValidatedProteins++;
                    }
                }
            }
            peptideShakerGUI.getMetrics().setnValidatedProteins(nValidatedProteins);
        }
        return proteinListAfterHiding;
    }

    /**
     * Returns the ordered protein keys to display when no filtering is applied.
     *
     * @param progressDialogX can be null
     * @return the ordered protein keys to display when no filtering is applied.
     */
    public ArrayList<String> getProteinKeys(ProgressDialogX progressDialogX) {
        if (proteinList == null) {
            getProcessedProteinKeys(progressDialogX);
        }
        return proteinList;
    }

    /**
     * Sets the sorted protein key list.
     *
     * @param sortedProteinKeys the new sorted protein key list
     */
    public void setProteinKeys(ArrayList<String> proteinList) {
        this.proteinList = proteinList;
    }

    /**
     * Returns a sorted list of peptide keys from the protein of interest.
     *
     * @param proteinKey the key of the protein of interest
     * @return a sorted list of the corresponding peptide keys
     */
    public ArrayList<String> getSortedPeptideKeys(String proteinKey) {
        if (!proteinKey.equals(currentProteinKey)) {
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            HashMap<Double, HashMap<Integer, ArrayList<String>>> peptideMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
            PSParameter probabilities = new PSParameter();
            double peptideProbabilityScore;
            PeptideMatch peptideMatch;
            int spectrumCount;

            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, probabilities);
                if (!probabilities.isHidden()) {
                    peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {
                        peptideMap.put(peptideProbabilityScore, new HashMap<Integer, ArrayList<String>>());
                    }
                    peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                    spectrumCount = -peptideMatch.getSpectrumCount();
                    if (!peptideMap.get(peptideProbabilityScore).containsKey(spectrumCount)) {
                        peptideMap.get(peptideProbabilityScore).put(spectrumCount, new ArrayList<String>());
                    }
                    peptideMap.get(peptideProbabilityScore).get(spectrumCount).add(peptideKey);
                }
            }

            ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
            Collections.sort(scores);
            ArrayList<Integer> nSpectra;
            ArrayList<String> keys;
            peptideList = new ArrayList<String>();
            for (double currentScore : scores) {
                nSpectra = new ArrayList<Integer>(peptideMap.get(currentScore).keySet());
                Collections.sort(nSpectra);
                for (int currentNPsm : nSpectra) {
                    keys = peptideMap.get(currentScore).get(currentNPsm);
                    Collections.sort(keys);
                    peptideList.addAll(keys);
                }
            }
        }
        return peptideList;
    }

    /**
     * Returns the ordered list of spectrum keys for a given peptide.
     *
     * @param peptideKey the key of the peptide of interest
     * @return the ordered list of spectrum keys
     */
    public ArrayList<String> getSortedPsmKeys(String peptideKey) {
        if (!peptideKey.equals(currentPeptideKey)) {
            PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
            SpectrumMatch spectrumMatch;
            HashMap<Integer, HashMap<Double, ArrayList<String>>> orderingMap = new HashMap<Integer, HashMap<Double, ArrayList<String>>>();
            int charge;
            boolean hasRT = true;
            double rt = -1;
            for (String spectrumKey : currentPeptideMatch.getSpectrumMatches()) {
                spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                charge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                if (!orderingMap.containsKey(charge)) {
                    orderingMap.put(charge, new HashMap<Double, ArrayList<String>>());
                }
                if (hasRT) {
                    rt = peptideShakerGUI.getPrecursor(spectrumKey).getRt();
                    if (rt == -1) {
                        hasRT = false;
                    }
                }
                if (!hasRT) {
                    PSParameter pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(spectrumKey, new PSParameter());
                    rt = pSParameter.getPsmProbabilityScore();
                }
                if (!orderingMap.get(charge).containsKey(rt)) {
                    orderingMap.get(charge).put(rt, new ArrayList<String>());
                }
                orderingMap.get(charge).get(rt).add(spectrumKey);
            }
            ArrayList<Integer> charges = new ArrayList<Integer>(orderingMap.keySet());
            Collections.sort(charges);
            ArrayList<Double> rts;
            ArrayList<String> tempResult;
            psmList = new ArrayList<String>();
            for (int currentCharge : charges) {
                rts = new ArrayList<Double>(orderingMap.get(currentCharge).keySet());
                Collections.sort(rts);
                for (double currentRT : rts) {
                    tempResult = orderingMap.get(currentCharge).get(currentRT);
                    Collections.sort(tempResult);
                    psmList.addAll(tempResult);
                }
            }
        }
        return psmList;
    }

    /**
     * Returns a boolean indicating whether hiding proteins is necessary
     *
     * @return a boolean indicating whether hiding proteins is necessary
     */
    private boolean hidingNeeded() {
        if (filtered) {
            return true;
        }
        for (ProteinFilter proteinFilter : peptideShakerGUI.getFilterPreferences().getProteinHideFilters().values()) {
            if (proteinFilter.isActive()) {
                filtered = true;
                return true;
            }
        }
        return false;
    }
}
