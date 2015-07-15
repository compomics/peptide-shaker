package eu.isas.peptideshaker.scoring;

import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import javax.swing.RowFilter;

/**
 * This class contains basic information about the hits as imported from the
 * various search engine result files.
 *
 * @author Marc Vaudel
 */
public class InputMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 1117083720476649996L;
    /**
     * Map of the hits as imported. One target/decoy map per identification
     * advocate (referenced by their compomics utilities index).
     */
    private HashMap<Integer, TargetDecoyMap> inputMap = new HashMap<Integer, TargetDecoyMap>();
    /**
     * Map of the hits per file as imported. advocate index &gt; file name &gt;
     * target decoy map
     */
    private HashMap<Integer, HashMap<String, TargetDecoyMap>> inputSpecificMap = new HashMap<Integer, HashMap<String, TargetDecoyMap>>();
    /**
     * Map of the intermediate scores. Name of the file &gt; advocate index &gt;
     * score index
     */
    private HashMap<String, HashMap<Integer, HashMap<Integer, TargetDecoyMap>>> intermediateScores = new HashMap<String, HashMap<Integer, HashMap<Integer, TargetDecoyMap>>>();
    /**
     * Map of the search engine contribution. Advocate Id &gt; Spectrum file name
     * &gt; number of validated hits.
     */
    private HashMap<Integer, HashMap<String, Integer>> advocateContribution;
    /**
     * Map of the search engine contribution. Advocate Id &gt; Spectrum file name
     * &gt; number of validated hits found by this advocate only.
     */
    private HashMap<Integer, HashMap<String, Integer>> advocateUniqueContribution;
    
    /**
     * Returns true for multiple search engines investigations.
     *
     * @return true for multiple search engines investigations
     */
    public boolean isMultipleAlgorithms() {
        return inputMap.size() > 1;
    }

    /**
     * Returns the number of algorithms in the input map.
     *
     * @return the number of algorithms in the input map
     */
    public int getNalgorithms() {
        return inputMap.size();
    }

    /**
     * Returns a set containing the indexes of the algorithms scored in this
     * input map.
     *
     * @return a set containing the indexes of the algorithms scored in this
     * input map
     */
    public Set<Integer> getInputAlgorithms() {
        return inputMap.keySet();
    }

    /**
     * Returns a sorted arraylist containing the indexes of the algorithms
     * scored in this input map.
     *
     * @return a set containing the indexes of the algorithms scored in this
     * input map
     */
    public ArrayList<Integer> getInputAlgorithmsSorted() {
        Object[] keys = inputMap.keySet().toArray();
        Arrays.sort(keys);

        ArrayList<Integer> sortedKeys = new ArrayList<Integer>();

        for (Object key : keys) {
            sortedKeys.add((Integer) key); // @TODO: is there a quicker way of doing this..?
        }

        return sortedKeys;
    }

    /**
     * Returns the algorithms having an intermediate score for the given
     * spectrum file.
     *
     * @param fileName the name of the spectrum file of interest
     *
     * @return the algorithm having an intermediate score for this file
     */
    public Set<Integer> getIntermediateScoreInputAlgorithms(String fileName) {
        return intermediateScores.get(fileName).keySet();
    }

    /**
     * Returns the target decoy map attached to the given algorithm.
     *
     * @param algorithm the utilities index of the algorithm of interest
     *
     * @return the target decoy map of interest
     */
    public TargetDecoyMap getTargetDecoyMap(int algorithm) {
        return inputMap.get(algorithm);
    }

    /**
     * Returns the target decoy map attached to the given algorithm for a
     * specific spectrum file. Null if not found.
     *
     * @param algorithm the utilities index of the algorithm of interest
     * @param fileName the name of the spectrum file of interest
     *
     * @return the target decoy map of interest
     */
    public TargetDecoyMap getTargetDecoyMap(int algorithm, String fileName) {
        HashMap<String, TargetDecoyMap> algorithmInput = inputSpecificMap.get(algorithm);
        if (algorithmInput == null) {
            return null;
        }
        return algorithmInput.get(fileName);
    }

    /**
     * Returns the first target/decoy map of the input map in case a single
     * algorithm was used.
     *
     * @return the first target/decoy map of the input map
     */
    public TargetDecoyMap getMap() {
        if (inputMap == null || inputMap.isEmpty()) {
            throw new IllegalArgumentException("No algorithm input found.");
        }
        if (isMultipleAlgorithms()) {
            throw new IllegalArgumentException("Multiple search engine results found.");
        }
        for (TargetDecoyMap firstMap : inputMap.values()) {
            return firstMap;
        }
        return null;
    }

    /**
     * Estimates the posterior error probability for each search engine.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        int max = getNEntries();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        for (TargetDecoyMap hitmap : inputMap.values()) {
            waitingHandler.increaseSecondaryProgressCounter();
            hitmap.estimateProbabilities(waitingHandler);
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }
        for (HashMap<String, TargetDecoyMap> algorithmMaps : inputSpecificMap.values()) {
            for (TargetDecoyMap targetDecoyMap : algorithmMaps.values()) {
                waitingHandler.increaseSecondaryProgressCounter();
                targetDecoyMap.estimateProbabilities(waitingHandler);
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * returns the posterior error probability associated to the given e-value
     * for the given search-engine (indexed by its utilities index)
     *
     * @param searchEngine The search engine
     * @param eValue The e-value
     * @return the posterior error probability corresponding
     */
    public double getProbability(int searchEngine, double eValue) {
        return inputMap.get(searchEngine).getProbability(eValue);
    }

    /**
     * Returns a list of search engines indexed by utilities index presenting a
     * suspicious input.
     *
     * @param minimalFDR the minimal FDR requested for a group
     *
     * @return a list of search engines presenting a suspicious input
     */
    public ArrayList<Integer> suspiciousInput(Double minimalFDR) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if (inputMap.size() == 1) {
            return result;
        }
        for (int key : inputMap.keySet()) {
            if (inputMap.get(key).suspiciousInput(minimalFDR)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Constructor.
     */
    public InputMap() {
    }

    /**
     * Adds an entry to the input map.
     *
     * @param searchEngine The search engine used referenced by its compomics
     * index
     * @param spectrumFileName the name of the inspected spectrum file
     * @param eValue The search engine e-value
     * @param isDecoy boolean indicating whether the hit was decoy or target
     * (resp. true/false)
     */
    public synchronized void addEntry(int searchEngine, String spectrumFileName, double eValue, boolean isDecoy) {
        TargetDecoyMap targetDecoyMap = inputMap.get(searchEngine);
        if (targetDecoyMap == null) {
            targetDecoyMap = new TargetDecoyMap();
            inputMap.put(searchEngine, targetDecoyMap);
        }
        targetDecoyMap.put(eValue, isDecoy);
        HashMap<String, TargetDecoyMap> algorithmMap = inputSpecificMap.get(searchEngine);
        if (algorithmMap == null) {
            algorithmMap = new HashMap<String, TargetDecoyMap>();
            inputSpecificMap.put(searchEngine, algorithmMap);
        }
        targetDecoyMap = algorithmMap.get(spectrumFileName);
        if (targetDecoyMap == null) {
            targetDecoyMap = new TargetDecoyMap();
            algorithmMap.put(spectrumFileName, targetDecoyMap);
        }
        targetDecoyMap.put(eValue, isDecoy);
    }

    /**
     * Returns the number of entries.
     *
     * @return the number of entries
     */
    public int getNEntries() {
        int result = 0;
        for (TargetDecoyMap targetDecoyMap : inputMap.values()) {
            result += targetDecoyMap.getMapSize();
        }
        return result;
    }

    /**
     * Resets the advocate contribution mappings for the given file.
     *
     * @param fileName the file of interest
     */
    public void resetAdvocateContributions(String fileName) {
        if (advocateContribution == null) {
            advocateContribution = new HashMap<Integer, HashMap<String, Integer>>();
        } else {
            for (HashMap<String, Integer> advocateMapping : advocateContribution.values()) {
                advocateMapping.put(fileName, 0);
            }
        }
        if (advocateUniqueContribution == null) {
            advocateUniqueContribution = new HashMap<Integer, HashMap<String, Integer>>();
        } else {
            for (HashMap<String, Integer> advocateMapping : advocateUniqueContribution.values()) {
                advocateMapping.put(fileName, 0);
            }
        }
    }

    /**
     * Resets the advocate contribution mappings.
     */
    public void resetAdvocateContributions() {
        if (advocateContribution == null) {
            advocateContribution = new HashMap<Integer, HashMap<String, Integer>>();
        } else {
            advocateContribution.clear();
        }
        if (advocateUniqueContribution == null) {
            advocateUniqueContribution = new HashMap<Integer, HashMap<String, Integer>>();
        } else {
            advocateUniqueContribution.clear();
        }
    }

    /**
     * Adds an advocate contribution.
     *
     * @param advocateId the index of the advocate
     * @param fileName the name of the spectrum file of interest
     * @param unique boolean indicating whether the advocate was the only
     * advocate for the considered assumption
     */
    public synchronized void addAdvocateContribution(Integer advocateId, String fileName, boolean unique) {
        HashMap<String, Integer> advocateContributions = advocateContribution.get(advocateId);
        if (advocateContributions == null) {
            advocateContributions = new HashMap<String, Integer>();
            advocateContribution.put(advocateId, advocateContributions);
        }
        Integer contribution = advocateContributions.get(fileName);
        if (contribution == null) {
            advocateContributions.put(fileName, 1);
        } else {
            advocateContributions.put(fileName, contribution + 1);
        }
        if (unique) {
            HashMap<String, Integer> advocateUniqueContributions = advocateUniqueContribution.get(advocateId);
            if (advocateUniqueContributions == null) {
                advocateUniqueContributions = new HashMap<String, Integer>();
                advocateUniqueContribution.put(advocateId, advocateUniqueContributions);
            }
            Integer uniqueContribution = advocateUniqueContributions.get(fileName);
            if (uniqueContribution == null) {
                advocateUniqueContributions.put(fileName, 1);
            } else {
                advocateUniqueContributions.put(fileName, uniqueContribution + 1);
            }
        }
    }

    /**
     * Returns the contribution of validated hits of the given advocate for the
     * given file.
     *
     * @param advocateId the advocate index
     * @param fileName the name of the spectrum file
     *
     * @return the number of validated hits supported by this advocate
     */
    public int getAdvocateContribution(Integer advocateId, String fileName) {
        HashMap<String, Integer> advocateContributions = advocateContribution.get(advocateId);
        if (advocateContributions != null) {
            Integer contribution = advocateContributions.get(fileName);
            if (contribution != null) {
                return contribution;
            }
        }
        return 0;
    }

    /**
     * Returns the contribution of validated hits of the given advocate for the
     * entire dataset.
     *
     * @param advocateId the advocate index
     *
     * @return the number of validated hits supported by this advocate
     */
    public int getAdvocateContribution(Integer advocateId) {
        HashMap<String, Integer> advocateContributions = advocateContribution.get(advocateId);
        if (advocateContributions != null) {
            int contribution = 0;
            for (int tempContribution : advocateContributions.values()) {
                contribution += tempContribution;
            }
            return contribution;
        }
        return 0;
    }

    /**
     * Returns the contribution of unique validated hits of the given advocate
     * for the given file.
     *
     * @param advocateId the advocate index
     * @param fileName the name of the spectrum file
     *
     * @return the number of validated hits uniquely supported by this advocate
     */
    public int getAdvocateUniqueContribution(Integer advocateId, String fileName) {
        HashMap<String, Integer> advocateContributions = advocateUniqueContribution.get(advocateId);
        if (advocateContributions != null) {
            Integer contribution = advocateContributions.get(fileName);
            if (contribution != null) {
                return contribution;
            }
        }
        return 0;
    }

    /**
     * Returns the contribution of unique validated hits of the given advocate
     * for the entire dataset.
     *
     * @param advocateId the advocate index
     *
     * @return the number of validated hits uniquely supported by this advocate
     */
    public int getAdvocateUniqueContribution(Integer advocateId) {
        HashMap<String, Integer> advocateContributions = advocateUniqueContribution.get(advocateId);
        if (advocateContributions != null) {
            int contribution = 0;
            for (int tempContribution : advocateContributions.values()) {
                contribution += tempContribution;
            }
            return contribution;
        }
        return 0;
    }

    /**
     * Returns a list of all target decoy maps contained in this mapping.
     *
     * @return all target decoy maps contained in this mapping
     */
    public ArrayList<TargetDecoyMap> getTargetDecoyMaps() {
        ArrayList<TargetDecoyMap> result = new ArrayList<TargetDecoyMap>(inputMap.values());
        for (HashMap<String, TargetDecoyMap> advocateMapping : inputSpecificMap.values()) {
            result.addAll(advocateMapping.values());
        }
        return result;
    }

    /**
     * Indicates whether the advocate contributions are present in this map.
     *
     * @return a boolean indicating whether the advocate contributions are
     * present in this map
     */
    public boolean hasAdvocateContribution() {
        return advocateContribution != null;
    }

    /**
     * Adds an intermediate score for a given match.
     *
     * @param fileName the name of the spectrum file of interest
     * @param advocateIndex the index of the advocate
     * @param scoreIndex the index of the score
     * @param score the score of the match
     * @param decoy indicates whether the match maps to a target or a decoy
     * protein
     */
    public synchronized void setIntermediateScore(String fileName, int advocateIndex, int scoreIndex, double score, boolean decoy) {
        HashMap<Integer, HashMap<Integer, TargetDecoyMap>> advocateMap = intermediateScores.get(fileName);
        if (advocateMap == null) {
            advocateMap = new HashMap<Integer, HashMap<Integer, TargetDecoyMap>>();
            intermediateScores.put(fileName, advocateMap);
        }
        HashMap<Integer, TargetDecoyMap> scoreMap = advocateMap.get(advocateIndex);
        if (scoreMap == null) {
            scoreMap = new HashMap<Integer, TargetDecoyMap>();
            advocateMap.put(advocateIndex, scoreMap);
        }
        TargetDecoyMap targetDecoyMap = scoreMap.get(scoreIndex);
        if (targetDecoyMap == null) {
            targetDecoyMap = new TargetDecoyMap();
            scoreMap.put(scoreIndex, targetDecoyMap);
        }
        targetDecoyMap.put(score, decoy);
    }

    /**
     * Returns the target decoy map associated to a given spectrum file,
     * advocate and score type. Null if not found.
     *
     * @param fileName the name of the spectrum file
     * @param advocateIndex the index of the advocate
     * @param scoreIndex the index of the score
     *
     * @return the target decoy map associated to the given spectrum file,
     * advocate and score type
     */
    public TargetDecoyMap getIntermediateScoreMap(String fileName, int advocateIndex, int scoreIndex) {
        HashMap<Integer, HashMap<Integer, TargetDecoyMap>> advocateMap = intermediateScores.get(fileName);
        if (advocateMap != null) {
            HashMap<Integer, TargetDecoyMap> scoreMap = advocateMap.get(advocateIndex);
            if (scoreMap != null) {
                TargetDecoyMap targetDecoyMap = scoreMap.get(scoreIndex);
                return targetDecoyMap;
            }
        }
        return null;
    }

}
