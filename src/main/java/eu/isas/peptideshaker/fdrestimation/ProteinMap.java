package eu.isas.peptideshaker.fdrestimation;

import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.Serializable;

/**
 * This map will be used to score protein matches and solve protein inference problems
 *
 * @author Marc
 */
public class ProteinMap implements Serializable {

    /**
     * The user specified fdr threshold
     */
    private double fdrThreshold;
    /**
     * The number of hits at this threshold
     */
    private int nHits;
    /**
     * The estimated FDR
     */
    private double fdr;
    /**
     * The estimated FNR
     */
    private double fnr;

    /**
     * The protein target/decoy map
     */
    private TargetDecoyMap proteinMatchMap = new TargetDecoyMap("protein inferences");

    /**
     * Constructor
     */
    public ProteinMap() {
        
    }


    /**
     * estimate the posterior error probabilities
     *
     * @param waitingDialog The dialog which display the information while processing
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        proteinMatchMap.estimateProbabilities(waitingDialog);
    }


    /**
     * Sets whether probabilistic thresholds should be applied when recommended
     * @param probabilistic boolean indicating whether probabilistic thresholds should be applied when recommended
     */
    public void setProbabilistic(boolean probabilistic) {
        proteinMatchMap.setProbabilistic(probabilistic);
    }

    /**
     * Estimates FDR, FNR and number of hits for the protein map at the given threshold
     *
     * @param newThreshold the new threshold
     */
    public void getResults(double newThreshold) {
        this.fdrThreshold = newThreshold;
        proteinMatchMap.getResults(fdrThreshold);
    }

    /**
     * Returns the score limit for the given peptide match at the selected FDR threshold
     *
     * @param peptideMatch the given peptide match
     * @return the score threshold
     */
    public double getScoreLimit() {
        return proteinMatchMap.getScoreLimit();
    }

    /**
     * Adds a point in the target/decoy map.
     *
     * @param probabilityScore The estimated protein probabilistic score
     */
    public void addPoint(double probabilityScore, boolean isDecoy) {
        proteinMatchMap.put(probabilityScore, isDecoy);
    }


    /**
     * Returns the posterior error probability of a peptide match at the given score
     *
     * @param peptideMatch the peptide match
     * @param score        the score of the match
     * @return the posterior error probability
     */
    public double getProbability(double score) {
        return proteinMatchMap.getProbability(score);
    }
}
