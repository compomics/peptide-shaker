package eu.isas.peptideshaker.fdrestimation;

import eu.isas.peptideshaker.gui.WaitingDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Marc Vaudel
 */
public class TargetDecoyMap {

    private String level;
    private HashMap<Double, TargetDecoyPoint> hitMap = new HashMap<Double, TargetDecoyPoint>();
    private ArrayList<Double> scores;
    private int nHits;
    private double fdr;
    private Double fnr;
    private double fdrThreshold;
    private Integer nmax;
    private Integer nTargetOnly;
    private boolean probabilistic = true;

    /**
     * @TODO: JavaDoc missing
     *
     * @param fdrThreshold
     */
    public void getResults(double fdrThreshold) {
        if (this.fdrThreshold != fdrThreshold) {
            this.fdrThreshold = fdrThreshold;
            getResults();
        }
    }

    /**
     * @TODO: JavaDoc missing
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
                    if (nFP / nP <= fdrThreshold) {
                        nFP += point.nTarget * point.p;
                        nP += point.nTarget;
                    } else {
                        nFN += point.nTarget * (1 - point.p);
                    }
                    nTotal += point.nTarget;
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
                }
            }
        }
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param score
     * @return
     */
    public Double getProbability(double score) {
        TargetDecoyPoint point = hitMap.get(score);
        return point.p;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param score
     * @return
     */
    public int getNTarget(double score) {
        return hitMap.get(score).nTarget;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param score
     * @return
     */
    public int getNDecoy(double score) {
        return hitMap.get(score).nDecoy;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param score
     * @param isDecoy
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
     * @TODO: JavaDoc missing
     *
     * @param level
     */
    public TargetDecoyMap(String level) {
        this.level = level;
    }

    /**
     * @TODO: JavaDoc missing
     */
    public void estimateProbabilities() {
        estimateProbabilities(null);
    }

    /**
     * @TODO: JavaDoc missing
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
     * @TODO: JavaDoc missing
     *
     * @param waitingDialog
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
     * Returns the FRD value.
     *
     * @return the FRD value
     */
    public double getFdr() {
        return fdr;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public Double getFnr() {
        if (probabilistic) {
            return fnr;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Return the number of hits.
     *
     * @return the number of hits
     */
    public int getNHits() {
        return nHits;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public int getnMax() {
        if (nmax == null) {
            estimateNs();
        }
        return nmax;
    }

    /**
     * @TODO: JavaDoc missing
     */
    private void estimateScores() {
        scores = new ArrayList<Double>(hitMap.keySet());
        Collections.sort(scores);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public ArrayList<Double> getScores() {
        if (scores == null) {
            estimateScores();
        }
        return scores;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param anOtherMap
     */
    public void addAll(TargetDecoyMap anOtherMap) {
        for(double score : anOtherMap.getScores()) {
            for (int i = 0 ; i < anOtherMap.getNDecoy(score) ; i++) {
                put(score, true);
            }
            for (int i = 0 ; i < anOtherMap.getNTarget(score) ; i++) {
                put(score, false);
            }
        }
    }

    /**
     * @TODO: JavaDoc missing
     */
    private class TargetDecoyPoint {

        public int nTarget = 0;
        public int nDecoy = 0;
        public double p;

        public TargetDecoyPoint() {
        }
    }
}
