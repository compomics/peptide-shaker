package eu.isas.peptideshaker.fdrestimation;

import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This map contains the information of a target/decoy strategy
 *
 * @author Marc Vaudel
 */
public class TargetDecoyMap implements Serializable {

    /**
     * The level at which the target/decoy investigation is conducted (for instance psm, peptide, protein, phosphorylated peptides...)
     */
    private String level;
    /**
     * The hit map containing the indexed target/decoy points
     */
    private HashMap<Double, TargetDecoyPoint> hitMap = new HashMap<Double, TargetDecoyPoint>();
    /**
     * The scores imported in the map
     */
    private ArrayList<Double> scores;
    /**
     * The number of hits retained at the implemented threshold
     */
    private int nHits;
    /**
     * The estimated FDR at the implemented threshold
     */
    private double fdr;
    /**
     * The estimated FNR at the implemented threshold
     */
    private Double fnr;
    /**
     * The implemented FDR threshold
     */
    private double fdrThreshold;
    /**
     * The maximal amount of target hits comprised between two subsequent decoy hits
     */
    private Integer nmax;
    /**
     * The number of target hits found before the first decoy hit
     */
    private Integer nTargetOnly;
    /**
     * Boolean indicating whether the FDR will be estimated probabilistically or not
     */
    private boolean probabilistic = true;
    /**
     * The estimated score limit at the implemented FDR threshold
     */
    private double scoreLimit = -1;

    /**
     * estimates the results at a new FDR threshold
     * @param fdrThreshold the new FDR threshold
     */
    public void getResults(double fdrThreshold) {
        this.fdrThreshold = fdrThreshold;
        getResults();
    }

    /**
     * Estimates the dataset metrics Nmax, NtargetOnly. Estimates whether a probabilistic processing is possible. Retrieves the FDR, FNR (when possible) and the number of hits passing the threshold.
     */
    private void getResults() {
        int nP = 0;
        int nTotal = 0;
        double nFP = 0;
        double nFN = 0;
        TargetDecoyPoint point;
        if (probabilistic) {
            for (double score : scores) {
                point = hitMap.get(score);
                if (point.nTarget > 0) {
                    if ((nFP + point.nTarget * point.p) / (nP + point.nTarget) <= fdrThreshold) {
                        nFP += point.nTarget * point.p;
                        nP += point.nTarget;
                        scoreLimit = score;
                        nFN = 0;
                    } else {
                        nFN += point.nTarget * (1 - point.p);
                    }
                    nTotal += point.nTarget * (1 - point.p);
                }
            }
            nHits = nP;
            fdr = nFP / nP;
            fnr = nFN / nTotal;
        } else {
            for (double score : scores) {
                point = hitMap.get(score);
                nFP += point.nDecoy;
                nP += point.nTarget;
                if (nFP / nP <= fdrThreshold) {
                    nHits = nP;
                    fdr = nFP / nP;
                    fnr = Double.NaN;
                    scoreLimit = score;
                }
            }
        }
    }

    /**
     * Returns the score limit at the implemented FDR threshold
     * @return the score limit at the implemented FDR threshold
     */
    public double getScoreLimit() {
        return scoreLimit;
    }

    /**
     * Returns the posterior error probability estimated at the given score
     * @param score the given score
     * @return the estimated posterior error probability
     */
    public Double getProbability(double score) {
        TargetDecoyPoint point = hitMap.get(score);
        return point.p;
    }

    /**
     * Returns the number of target hits found at the given score
     * @param score the given score
     * @return the number of target hits found at the given score
     */
    public int getNTarget(double score) {
        return hitMap.get(score).nTarget;
    }

    /**
     * the number of decoy hits found at the given score
     * @param score the given score
     * @return the number of decoy hits found at the given score
     */
    public int getNDecoy(double score) {
        return hitMap.get(score).nDecoy;
    }

    /**
     * Puts a new point in the target/decoy map at the given score
     * @param score   The given score
     * @param isDecoy boolean indicating whether the hit is decoy
     */
    public void put(double score, boolean isDecoy) {
        if (!hitMap.containsKey(score)) {
            hitMap.put(score, new TargetDecoyPoint());
        }
        if (isDecoy) {
            hitMap.get(score).nDecoy++;
        } else {
            hitMap.get(score).nTarget++;
        }
    }

    /**
     * Constructs a target/decoy map at the desired level
     * @param level the level of the target/decoy map
     */
    public TargetDecoyMap(String level) {
        this.level = level;
    }

    /**
     * estimate the probability for this map (without graphical feedback)
     */
    public void estimateProbabilities() {
        estimateProbabilities(null);
    }

    /**
     * Estimates the metrics of the map: Nmax and NtargetOnly
     */
    private void estimateNs() {
        estimateScores();
        // get N
        boolean onlyTarget = true;
        int nMax = 0;
        int targetCpt = 0;
        nTargetOnly = 0;
        TargetDecoyPoint point;
        for (double peptideP : scores) {
            point = hitMap.get(peptideP);
            if (onlyTarget) {
                nTargetOnly += point.nTarget;
                if (point.nDecoy > 0) {
                    onlyTarget = false;
                }
            } else {
                targetCpt += point.nTarget;
                if (point.nDecoy > 0) {
                    if (targetCpt > nMax && peptideP < 1) {
                        nMax = targetCpt;
                    }
                    targetCpt = point.nTarget;
                }
            }
        }
        nmax = nMax;
    }

    /**
     * Estimates the posterior error probabilities in this map.
     * @param waitingDialog dialog giving feedback to the user.
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        boolean report = waitingDialog != null;
        estimateScores();
        estimateNs();
        if (nmax < 100) {
            if (report) {
                waitingDialog.appendReport(level + " Nmax = " + nmax + " probability estimation might not be reliable.");
            }
            nmax = 100;
            probabilistic = false;
        }
        if (nTargetOnly < 100) {
            if (report) {
                waitingDialog.appendReport("Less than 100 " + level + " were identified confidently. Statistics might not be reliable.");
            }
            probabilistic = false;
        }

        // estimate p
        TargetDecoyPoint tempPoint, previousPoint = hitMap.get(scores.get(0));
        double nLimit = 0.5 * nmax;
        double nTargetSup = 1.5 * previousPoint.nTarget;
        double nTargetInf = -0.5 * previousPoint.nTarget;
        double nTargetInfTemp;
        double nDecoy = previousPoint.nDecoy;
        int cptInf = 0;
        int cptSup = 1;
        double change;
        TargetDecoyPoint point;
        for (int cpt = 0; cpt < scores.size(); cpt++) {
            point = hitMap.get(scores.get(cpt));
            change = 0.5 * (previousPoint.nTarget + point.nTarget);
            nTargetInf += change;
            nTargetSup -= change;
            while (nTargetInf > nLimit) {
                if (cptInf < cpt) {
                    tempPoint = hitMap.get(scores.get(cptInf));
                    nTargetInfTemp = nTargetInf - tempPoint.nTarget;
                    if (nTargetInfTemp >= nLimit) {
                        nDecoy -= tempPoint.nDecoy;
                        nTargetInf = nTargetInfTemp;
                        cptInf++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            while (nTargetSup < nLimit) {
                if (cptSup < scores.size()) {
                    tempPoint = hitMap.get(scores.get(cptSup));
                    nTargetSup += tempPoint.nTarget;
                    nDecoy += tempPoint.nDecoy;
                    cptSup++;
                } else {
                    break;
                }
            }
            point.p = Math.min(nDecoy / (nTargetInf + nTargetSup), 1);
            previousPoint = point;
        }
    }

    /**
     * Returns the estimated FDR at the given threshold
     * @return the estimated FDR at the given threshold
     */
    public double getFdr() {
        return fdr;
    }

    /**
     * Returns the estimated FNR at the given threshold. NaN if no FNR could be computed.
     * @return the estimated FNR at the given threshold
     */
    public Double getFnr() {
        if (probabilistic) {
            return fnr;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Returns the number of hits retained at the given threshold.
     * @return the number of hits retained at the given threshold.
     */
    public int getNHits() {
        return nHits;
    }

    /**
     * Returns the Nmax metric
     * @return the Nmax metric
     */
    public int getnMax() {
        if (nmax == null) {
            estimateNs();
        }
        return nmax;
    }

    /**
     * Sorts the scores implemented in this map
     */
    private void estimateScores() {
        scores = new ArrayList<Double>(hitMap.keySet());
        Collections.sort(scores);
    }

    /**
     * Sets whether probabilistic thresholds should be applied when recommended
     * @param probabilistic boolean indicating whether probabilistic thresholds should be applied when recommended
     */
    public void setProbabilistic(boolean probabilistic) {
        this.probabilistic = probabilistic;
    }

    /**
     * Returns the sorted scores implemented in this map.
     * @return the sorted scores implemented in this map.
     */
    public ArrayList<Double> getScores() {
        if (scores == null) {
            estimateScores();
        }
        return scores;
    }

    /**
     * Adds all the points from another target/decoy map
     * @param anOtherMap another target/decoy map
     */
    public void addAll(TargetDecoyMap anOtherMap) {
        for (double score : anOtherMap.getScores()) {
            for (int i = 0; i < anOtherMap.getNDecoy(score); i++) {
                put(score, true);
            }
            for (int i = 0; i < anOtherMap.getNTarget(score); i++) {
                put(score, false);
            }
        }
    }

    /**
     * Private class representing a point in the target/decoy map
     */
    private class TargetDecoyPoint implements Serializable {

        /**
         * The number of target hits at this point
         */
        public int nTarget = 0;
        /**
         * The number of decoy hits at this point
         */
        public int nDecoy = 0;
        /**
         * The posterior error probability associated to this point
         */
        public double p;

        /**
         * constructor
         */
        public TargetDecoyPoint() {
        }
    }
}
