package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.identification.AdvocateFactory;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.util.HashMap;

/**
 *
 * @author Marc Vaudel
 */
public class InputMap {

    /**
     * Map of the hits as imported.
     */
    private HashMap<Integer, TargetDecoyMap> inputMap = new HashMap<Integer, TargetDecoyMap>();

    /**
     * Returns true of multiple search engines are used.
     *
     * @return true of multiple search engines are used
     */
    public boolean isMultipleSearchEngines() {
        return inputMap.size() > 1;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public TargetDecoyMap getFirstMap() {
        for (TargetDecoyMap firstMap : inputMap.values()) {
            return firstMap;
        }
        return null;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param waitingDialog
     */
    public void computeProbabilities(WaitingDialog waitingDialog) {
        for (TargetDecoyMap hitmap : inputMap.values()) {
            hitmap.estimateProbabilities(waitingDialog);
        }
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param searchEngine
     * @param eValue
     * @return
     */
    public double getProbability(int searchEngine, double eValue) {
        return inputMap.get(searchEngine).getProbability(eValue);
    }

    /**
     * @TODO: JavaDoc missing
     */
    public InputMap() {
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param searchEngine
     * @param eValue
     * @param isDecoy
     */
    public void addEntry(int searchEngine, double eValue, boolean isDecoy) {
        if (inputMap.get(searchEngine) == null) {
            String searchEngineName = AdvocateFactory.getInstance().getAdvocate(searchEngine).getName();
            inputMap.put(searchEngine, new TargetDecoyMap(searchEngineName + " spectrum match"));
        }
        inputMap.get(searchEngine).put(eValue, isDecoy);
    }
}
