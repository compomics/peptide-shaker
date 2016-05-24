package eu.isas.peptideshaker.scoring.maps;

import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.Serializable;

/**
 * This map will be used to score protein matches and solve protein inference
 * problems
 *
 * @author Marc Vaudel
 */
public class ProteinMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -2438674334416191482L;
    /**
     * The protein target/decoy map.
     */
    private TargetDecoyMap proteinMatchMap = new TargetDecoyMap();

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
        proteinMatchMap.remove(probabilityScore, isDecoy);
    }

    /**
     * Removes empty points and clears dependent metrics if needed.
     */
    public void cleanUp() {
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
        return proteinMatchMap.suspiciousInput(minimalFDR);
    }

    /**
     * Returns the target decoy map.
     *
     * @return the target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap() {
        return proteinMatchMap;
    }
}
