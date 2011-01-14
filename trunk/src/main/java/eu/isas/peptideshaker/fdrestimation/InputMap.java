package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.identification.AdvocateFactory;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.util.HashMap;

/**
 * This class contains basic information about the hits as imported from the various search engine result files.
 *
 * @author Marc Vaudel
 */
public class InputMap {

    /**
     * Map of the hits as imported. One target/decoy map per search engine (referenced by their compomics index)
     */
    private HashMap<Integer, TargetDecoyMap> inputMap = new HashMap<Integer, TargetDecoyMap>();

    /**
     * Returns true for multiple search engines investigations.
     *
     * @return true for multiple search engines investigations
     */
    public boolean isMultipleSearchEngines() {
        return inputMap.size() > 1;
    }

    /**
     * returns the first target/decoy map of the input map (used in case of single search engines studies).
     *
     * @return the first target/decoy map of the input map
     */
    public TargetDecoyMap getFirstMap() {
        for (TargetDecoyMap firstMap : inputMap.values()) {
            return firstMap;
        }
        return null;
    }

    /**
     * Estimates the posterior error probability for each search engine
     *
     * @param waitingDialog the dialog displaying the output
     */
    public void computeProbabilities(WaitingDialog waitingDialog) {
        for (TargetDecoyMap hitmap : inputMap.values()) {
            hitmap.estimateProbabilities(waitingDialog);
        }
    }

    /**
     * returns the posterior error probability associated to the given e-value for the given search-engine (indexed by its utilities index)
     *
     * @param searchEngine  The search engine
     * @param eValue        The e-value
     * @return the posterior error probability corresponding
     */
    public double getProbability(int searchEngine, double eValue) {
        return inputMap.get(searchEngine).getProbability(eValue);
    }

    /**
     * constructor
     */
    public InputMap() {
    }

    /**
     * Adds an entry to the input map.
     *
     * @param searchEngine  The search engine used referenced by its compomics index
     * @param eValue    The search engine e-value
     * @param isDecoy   boolean indicating whether the hit was decoy or target (resp. true/false)
     */
    public void addEntry(int searchEngine, double eValue, boolean isDecoy) {
        if (inputMap.get(searchEngine) == null) {
            String searchEngineName = AdvocateFactory.getInstance().getAdvocate(searchEngine).getName();
            inputMap.put(searchEngine, new TargetDecoyMap(searchEngineName + " spectrum match"));
        }
        inputMap.get(searchEngine).put(eValue, isDecoy);
    }
}
