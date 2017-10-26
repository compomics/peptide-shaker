package eu.isas.peptideshaker.scoring.maps;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;

/**
 * This map will be used to score protein matches and solve protein inference
 * problems
 *
 * @author Marc Vaudel
 */
public class ProteinMap extends DbObject {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -2438674334416191482L;
    /**
     * The protein target/decoy map.
     */
    private final TargetDecoyMap proteinMatchMap = new TargetDecoyMap();

    /**
     * Constructor.
     */
    public ProteinMap() {
    }

    /**
     * Estimate the posterior error probabilities.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        waitingHandler.setWaitingText("Estimating Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(proteinMatchMap.getMapSize());

        proteinMatchMap.estimateProbabilities(waitingHandler);

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Adds a point in the target/decoy map.
     *
     * @param probabilityScore The estimated protein probabilistic score
     * @param isDecoy a boolean indicating whether the protein is decoy
     */
    public void addPoint(double probabilityScore, boolean isDecoy) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        proteinMatchMap.put(probabilityScore, isDecoy);
    }

    /**
     * Removes a point in the target/decoy map. Note: it is necessary to run
     * cleanUp() afterwards to clean up the map.
     *
     * @param probabilityScore The estimated protein probabilistic score
     * @param isDecoy a boolean indicating whether the protein is decoy
     */
    public void removePoint(double probabilityScore, boolean isDecoy) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        proteinMatchMap.remove(probabilityScore, isDecoy);
    }

    /**
     * Removes empty points and clears dependent metrics if needed.
     */
    public void cleanUp() {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        proteinMatchMap.cleanUp();
    }

    /**
     * Returns the posterior error probability of a peptide match at the given
     * score.
     *
     * @param score the score of the match
     * @return the posterior error probability
     */
    public double getProbability(double score) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinMatchMap.getProbability(score);
    }

    /**
     * Returns a boolean indicating if a suspicious input was detected.
     *
     * @param minimalFDR the minimal FDR requested for a group
     *
     * @return a boolean indicating if a suspicious input was detected
     */
    public boolean suspicousInput(Double minimalFDR) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinMatchMap.suspiciousInput(minimalFDR);
    }

    /**
     * Returns the target decoy map.
     *
     * @return the target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinMatchMap;
    }
}
