package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.SearchParameters;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class contains metrics and methods for the posterior validation of PSMs and Peptides
 *
 * @author Marc
 */
public class PosteriorValidationMetrics {

    /**
     * The compomics utilities ptm factory
     */
    private PTMFactory pTMFactory = PTMFactory.getInstance();
    /**
     * The peptide search space size
     */
    private BigInteger searchSpaceSize;
    /**
     * The posterior validation metrics map. score threshold -> result
     */
    private HashMap<Double, PosteriorValidationPoint> resultMap;
    /**
     * Number of protein modifications to account for
     */
    private int nProteinModif = 0;
    /**
     * Number of peptide C-term modifications to account for
     */
    private int nPeptideCtermModif = 0;
    /**
     * number of peptide N-term modifications to account for
     */
    private int nPeptideNtermModif = 0;
    /**
     * list of potentially modified amino acids
     */
    private HashMap<String, Integer> aaModifs = new HashMap<String, Integer>();
    /**
     * list of potentially modified N-term amino acids
     */
    private ArrayList<String> aaNtermModifs = new ArrayList<String>();
    /**
     * list of potentially modified C-term amino acids
     */
    private ArrayList<String> aaCtermModifs = new ArrayList<String>();

    /**
     * Constructor
     */
    public PosteriorValidationMetrics() {
    }

    /**
     * Returns the validation metrics at a desired protein score
     * @param proteinScore  the protein score
     * @return              The corresponding results as PosteriorValidationPoint
     */
    public PosteriorValidationPoint getResults(double proteinScore) {
        return resultMap.get(proteinScore);
    }

    /**
     * Returns the size of the search space
     * @return the size of the search space
     */
    public BigInteger getSearchSpace() {
        return searchSpaceSize;
    }

    /**
     * Method called whenever something changed in the probabilities estimation
     */
    public void probabilitiesPropertiesChanged(SearchParameters searchParameters) {
        int ptmType;
        PTM currentPTM;
        HashMap<String, Integer> expectedModifications = searchParameters.getExpectedModifications();
        for (String modName : expectedModifications.keySet()) {
            currentPTM = pTMFactory.getPTM(modName);
            ptmType = currentPTM.getType();
            if (ptmType == PTM.MODCP
                    || ptmType == PTM.MODNP) {
                nProteinModif++;
            } else if (ptmType == PTM.MODC) {
                nPeptideCtermModif++;
            } else if (ptmType == PTM.MODN) {
                nPeptideNtermModif++;
            } else if (ptmType == PTM.MODCAA) {
                for (String aa : currentPTM.getResiduesArray()) {
                    if (!aaCtermModifs.contains(aa)
                            && !aaModifs.keySet().contains(aa)) {
                        aaCtermModifs.add(aa);
                    }
                }
            } else if (ptmType == PTM.MODNAA) {
                for (String aa : currentPTM.getResiduesArray()) {
                    if (!aaNtermModifs.contains(aa)
                            && !aaModifs.keySet().contains(aa)) {
                        aaNtermModifs.add(aa);
                    }
                }
            } else if (ptmType == PTM.MODAA) {
                for (String aa : currentPTM.getResiduesArray()) {
                    if (!aaModifs.keySet().contains(aa)) {
                        aaModifs.put(aa, expectedModifications.get(modName));
                        if (aaNtermModifs.contains(aa)) {
                            aaNtermModifs.remove(aa);
                        }
                        if (aaCtermModifs.contains(aa)) {
                            aaCtermModifs.remove(aa);
                        }
                    } else {
                        aaModifs.put(aa, aaModifs.get(aa) + expectedModifications.get(modName));
                    }
                }
            }
        }
    }

    /**
     * Creates a map of the result space size depending on the protein score threshold.
     * For protein groups only one protein will be retained, assuming that the sequences are similar (typically isoforms)
     * 
     * @param searchParameters
     * @param sequenceDataBase 
     * @param identification 
     * @param peptideSpecificMap
     * @param psmSpecificMap  
     */
    public void estimateDatasetPossibilities(SearchParameters searchParameters, SequenceDataBase sequenceDataBase, 
            Identification identification, PeptideSpecificMap peptideSpecificMap, PsmSpecificMap psmSpecificMap) {
        
        resultMap = new HashMap<Double, PosteriorValidationPoint>();
        PSParameter probabilities = new PSParameter();
        int nProtein;
        double proteinScore;
        PosteriorValidationPoint currentPoint, tempPoint;
        ArrayList<String> takenPeptides = new ArrayList<String>();
        ArrayList<String> takenPsms = new ArrayList<String>();
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            if (!proteinMatch.isDecoy()) {
                probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);
                proteinScore = probabilities.getProteinProbabilityScore();
                String proteinSequence = "";
                for (String proteinKey : proteinMatch.getTheoreticProteinsAccessions()) {
                    proteinSequence = sequenceDataBase.getProtein(proteinKey).getSequence();
                    break;
                }
                nProtein = estimateProteinPossibilities(searchParameters, proteinSequence, true);
                if (!resultMap.containsKey(proteinScore)) {
                    resultMap.put(proteinScore, new PosteriorValidationPoint());
                }
                currentPoint = resultMap.get(proteinScore);
                currentPoint.resultSpaceSize += nProtein;
                for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                    if (!takenPeptides.contains(peptideMatch.getKey())) {
                        currentPoint.addPeptide(peptideSpecificMap.getCorrectedKey(peptideMatch));
                        takenPeptides.add(peptideMatch.getKey());
                        probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
                        if (probabilities.getPeptideProbability() == 1) {
                            currentPoint.peptideP1++;
                        }
                        for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
                            if (!takenPsms.contains(spectrumMatch.getKey())) {
                                currentPoint.addPsm(psmSpecificMap.getCorrectedKey(spectrumMatch));
                                takenPsms.add(spectrumMatch.getKey());
                                probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);
                                if (probabilities.getPsmProbability() == 1) {
                                    currentPoint.psmP1++;
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<Double> scores = new ArrayList<Double>(resultMap.keySet());
        Collections.sort(scores);
        for (int i = 1; i < scores.size(); i++) {
            currentPoint = resultMap.get(scores.get(i));
            tempPoint = resultMap.get(scores.get(i - 1));
            currentPoint.resultSpaceSize += tempPoint.resultSpaceSize;
            currentPoint.peptideSpaceSize += tempPoint.peptideSpaceSize;
            currentPoint.psmSpaceSize += tempPoint.psmSpaceSize;
            for (String key : tempPoint.specificPeptideSpaceSize.keySet()) {
                if (!currentPoint.specificPeptideSpaceSize.containsKey(key)) {
                    currentPoint.specificPeptideSpaceSize.put(key, 0);
                }
                currentPoint.specificPeptideSpaceSize.put(key, currentPoint.specificPeptideSpaceSize.get(key) + tempPoint.specificPeptideSpaceSize.get(key));
            }
            for (int key : tempPoint.specificPsmSpaceSize.keySet()) {
                if (!currentPoint.specificPsmSpaceSize.containsKey(key)) {
                    currentPoint.specificPsmSpaceSize.put(key, 0);
                }
                currentPoint.specificPsmSpaceSize.put(key, currentPoint.specificPsmSpaceSize.get(key) + tempPoint.specificPsmSpaceSize.get(key));
            }
        }
    }

    /**
     * Estimates the number of potential peptides for a desired protein sequence
     * Protein modifications occurring at specific amino acids are not accounted for.
     * Also, only one modification per aa is allowed.
     * Peptides with less than 8 aa and more than 20 will be ignored as well as decoy proteins
     * when restricted the amount of combinations is set to a realistic value as set by the user in the search parameters
     *
     * @param proteinSequence the protein sequence
     * @return the potential number of peptides
     */
    private int estimateProteinPossibilities(SearchParameters searchParameters, String proteinSequence, boolean restricted) {
        ArrayList<String> inSillicoDigest = inSillicoDigest(proteinSequence, searchParameters.getEnzyme(), searchParameters.getnMissedCleavages());
        int nPeptide, nPepPoss, nProtein = 0;
        for (String sequence : inSillicoDigest) {
            if (sequence.length() >= 8 && sequence.length() <= 20) {
                nPeptide = 0;
                for (int c = 0; c <= nPeptideCtermModif; c++) {
                    for (int n = 0; n <= nPeptideNtermModif; n++) {
                        nPepPoss = 0;
                        if (n == 0) {
                            for (String aa : aaNtermModifs) {
                                if (sequence.startsWith(aa)) {
                                    nPepPoss++;
                                }
                            }
                        }
                        if (c == 0) {
                            for (String aa : aaCtermModifs) {
                                if (sequence.endsWith(aa)) {
                                    nPepPoss++;
                                }
                            }
                        }
                        sequence = "#" + sequence + "#";
                        for (String aa : aaModifs.keySet()) {
                            if (restricted) {
                                nPepPoss += Math.min(sequence.split(aa).length - 1, aaModifs.get(aa));
                            } else {
                                nPepPoss += sequence.split(aa).length - 1;
                            }
                        }
                        nPeptide += (new Double(Math.pow(2, nPepPoss))).intValue();
                    }
                }
                nProtein += nPeptide;
            }
        }
        return nProtein * (nProteinModif + 1);
    }

    /**
     * Estimates the size of the search space based on the database sequences and search settings.
     */
    public void estimateDataBasePossibilities(SearchParameters searchParameters, SequenceDataBase sequenceDataBase) {
        searchSpaceSize = new BigInteger("0");
        Protein currentProtein;
        int nProtein;
        for (String key : sequenceDataBase.getProteinList()) {
            currentProtein = sequenceDataBase.getProtein(key);
            if (!currentProtein.isDecoy()) {
                nProtein = estimateProteinPossibilities(searchParameters, currentProtein.getSequence(), false);
                searchSpaceSize = searchSpaceSize.add(new BigInteger(nProtein + ""));
            }
        }
    }

    /**
     * Performs the in silico digestion of a protein sequence using the selected enzyme and accounting for missed cleavages
     * @param sequence          the protein sequence
     * @param enzyme            the desired enzyme
     * @param nMissedCeavages   the number of allowed missed cleavages
     * @return                  an arraylist of all potential peptide sequences
     */
    private ArrayList<String> inSillicoDigest(String sequence, Enzyme enzyme, int nMissedCeavages) {
        ArrayList<String> result = new ArrayList<String>();
        String tempSequence;
        int lastIndex, currentIndex;
        boolean restricted;
        while (sequence.length() > 0) {
            lastIndex = -1;
            tempSequence = sequence.substring(0, sequence.length() - 1);
            for (char aa : enzyme.getAminoAcidBefore()) {
                restricted = false;
                currentIndex = tempSequence.lastIndexOf(aa);
                if (currentIndex > -1) {
                    for (char restriction : enzyme.getRestrictionAfter()) {
                        if (sequence.charAt(currentIndex + 1) == restriction) {
                            restricted = true;
                            break;
                        }
                    }
                    if (!restricted) {
                        lastIndex = Math.max(lastIndex, currentIndex);
                    }
                }
            }
            for (char aa : enzyme.getAminoAcidAfter()) {
                restricted = false;
                currentIndex = sequence.lastIndexOf(aa) - 1;
                if (currentIndex > -1) {
                    for (char restriction : enzyme.getRestrictionBefore()) {
                        if (sequence.charAt(currentIndex) == restriction) {
                            restricted = true;
                            break;
                        }
                    }
                    if (!restricted) {
                        lastIndex = Math.max(lastIndex, currentIndex);
                    }
                }
            }
            if (lastIndex > -1) {
                result.add(sequence.substring(lastIndex + 1));
                sequence = sequence.substring(0, lastIndex + 1);
            } else {
                result.add(sequence);
                sequence = "";
            }
        }

        ArrayList<String> tempSequences = new ArrayList<String>(result);
        int missedCleavagesCount = 0;
        while (missedCleavagesCount < nMissedCeavages) {
            for (int i = 0; i < tempSequences.size() - 1; i++) {
                tempSequence = tempSequences.get(i + 1) + tempSequences.get(i);
                tempSequences.set(i, tempSequence);
                result.add(tempSequence);
            }
            tempSequences.remove(tempSequences.size() - 1);
            missedCleavagesCount++;
            if (tempSequences.isEmpty()) {
                break;
            }
        }

        return result;
    }
}
