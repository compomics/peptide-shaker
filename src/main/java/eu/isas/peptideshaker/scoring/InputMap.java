package eu.isas.peptideshaker.scoring;

import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.Serializable;
import java.util.ArrayList;
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
     * Map of the hits as imported. One target/decoy map per search engine
     * (referenced by their compomics utilities index).
     */
    private HashMap<Integer, TargetDecoyMap> inputMap = new HashMap<Integer, TargetDecoyMap>();
    /**
     * Map of the hits per file as imported. One target/decoy map per search
     * engine (referenced by their compomics utilities index).
     */
    private HashMap<Integer, HashMap<String, TargetDecoyMap>> inputSpecificMap = new HashMap<Integer, HashMap<String, TargetDecoyMap>>();
    /**
     * The filters to use to flag doubtful matches.
     */
    private ArrayList<AssumptionFilter> doubtfulMatchesFilters = getDefaultAssumptionFilters();
    /**
     * Map of the search engine contribution. Advocate Id -> Spectrum file name
     * -> number of validated hits
     */
    private HashMap<Integer, HashMap<String, Integer>> advocateContribution;
    /**
     * Map of the search engine contribution. Advocate Id -> Spectrum file name
     * -> number of validated hits found by this advocate only
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
     * suspicious input
     *
     * @return a list of search engines presenting a suspicious input
     */
    public ArrayList<Integer> suspiciousInput() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if (inputMap.size() == 1) {
            return result;
        }
        for (int key : inputMap.keySet()) {
            if (inputMap.get(key).suspiciousInput()) {
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
    public void addEntry(int searchEngine, String spectrumFileName, double eValue, boolean isDecoy) {
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
     * Returns the filters used to flag doubtful matches.
     *
     * @return the filters used to flag doubtful matches
     */
    public ArrayList<AssumptionFilter> getDoubtfulMatchesFilters() {
        if (doubtfulMatchesFilters == null) { // Backward compatibility check for projects without filters
            doubtfulMatchesFilters = new ArrayList<AssumptionFilter>();
        }
        return doubtfulMatchesFilters;
    }

    /**
     * Sets the filters used to flag doubtful matches.
     *
     * @param doubtfulMatchesFilters the filters used to flag doubtful matches
     */
    public void setDoubtfulMatchesFilters(ArrayList<AssumptionFilter> doubtfulMatchesFilters) {
        this.doubtfulMatchesFilters = doubtfulMatchesFilters;
    }

    /**
     * Adds a PSM filter to the list of doubtful matches filters.
     *
     * @param assumptionFilter the new filter to add
     */
    public void addDoubtfulMatchesFilter(AssumptionFilter assumptionFilter) {
        this.doubtfulMatchesFilters.add(assumptionFilter);
    }

    /**
     * Returns the default filters for setting a match as doubtful.
     *
     * @return the default filters for setting a match as doubtful
     */
    public static ArrayList<AssumptionFilter> getDefaultAssumptionFilters() {
        ArrayList<AssumptionFilter> filters = new ArrayList<AssumptionFilter>();

        AssumptionFilter psmFilter = new AssumptionFilter(">30% Fragment Ion Sequence Coverage");
        psmFilter.setDescription(">30% sequence coverage by fragment ions");
        psmFilter.setSequenceCoverage(30.0);
        psmFilter.setSequenceCoverageComparison(RowFilter.ComparisonType.AFTER);
        filters.add(psmFilter);

        return filters;
    }

    /**
     * resets the advocate contribution mappings for the given file
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
     * resets the advocate contribution mappings
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
     * Adds an advocate contribution
     *
     * @param advocateId the index of the advocate
     * @param fileName the name of the spectrum file of interest
     * @param unique boolean indicating whether the advocate was the only
     * advocate for the considered assumption
     */
    public void addAdvocateContribution(Integer advocateId, String fileName, boolean unique) {
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
     * @return a boolean indicating whether the advocate contributions are present in this map
     */
    public boolean hasAdvocateContribution() {
        return advocateContribution != null;
    }
}
