package eu.isas.peptideshaker.scoring;

import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
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
public class InputMap {

    /**
     * Map of the hits as imported. One target/decoy map per search engine
     * (referenced by their compomics utilities index)
     */
    private HashMap<Integer, TargetDecoyMap> inputMap = new HashMap<Integer, TargetDecoyMap>();
    /**
     * The filters to use to flag doubtful matches.
     */
    private ArrayList<AssumptionFilter> doubtfulMatchesFilters = getDefaultAssumptionFilters();

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
     * Returns a set containing the indexes of the algorithms scored in this input map.
     * 
     * @return a set containing the indexes of the algorithms scored in this input map
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
     * returns the first target/decoy map of the input map in case a single algorithm was used.
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
     * constructor
     */
    public InputMap() {
    }

    /**
     * Adds an entry to the input map.
     *
     * @param searchEngine The search engine used referenced by its compomics
     * index
     * @param eValue The search engine e-value
     * @param isDecoy boolean indicating whether the hit was decoy or target
     * (resp. true/false)
     */
    public void addEntry(int searchEngine, double eValue, boolean isDecoy) {
        if (inputMap.get(searchEngine) == null) {
            inputMap.put(searchEngine, new TargetDecoyMap());
        }
        inputMap.get(searchEngine).put(eValue, isDecoy);
    }

    /**
     * Returns the number of entries
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
        
        AssumptionFilter psmFilter = new AssumptionFilter(">40% Fragment Ion Sequence Coverage");
        psmFilter.setDescription(">40% sequence coverage by fragment ions");
        psmFilter.setSequenceCoverage(40.0);
        psmFilter.setSequenceCoverageComparison(RowFilter.ComparisonType.AFTER);
//        filters.add(psmFilter);
        
        return filters;
    }
}
