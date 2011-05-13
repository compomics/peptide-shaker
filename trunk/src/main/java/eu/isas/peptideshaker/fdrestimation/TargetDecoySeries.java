package eu.isas.peptideshaker.fdrestimation;

import java.util.Arrays;
import java.util.HashMap;

/**
 * This class will contain the PEP, FDR and FNR values of a target decoy map directly available for plotting
 *
 * @author Marc
 */
public class TargetDecoySeries {

    /**
     * The implemented probabilistic score
     */
    private double[] scores;
    /**
     * The confidence
     */
    private double[] confidence;
    /**
     * The pep
     */
    private double[] pep;
    /**
     * The classical FDR
     */
    private double[] classicalFDR;
    /**
     * The probabilistic FDR
     */
    private double[] probaFDR;
    /**
     * The probabilistic FNR
     */
    private double[] probaFNR;
    /**
     * The benefit (1-FNR)
     */
    private double[] probaBenefit;
    /**
     * The amount of validated target hits
     */
    private double[] n;
    /**
     * The classically estimated amount of false positives
     */
    private double[] classicalFP;
    /**
     * The probabilistically estimated amount of false positives
     */
    private double[] probaFP;
    /**
     * indicates whether the current point is only made of decoy hits
     */
    private boolean[] decoy;
    /**
     * The probabilistically estimated total amount of false positives
     */
    private double probaNTotal;

    /**
     * Constructor
     * @param hitMap A map as present in target decoy maps
     */
    public TargetDecoySeries(HashMap<Double, TargetDecoyPoint> hitMap) {
        scores = new double[hitMap.size()];
        probaNTotal = 0;
        int cpt = 0;
        TargetDecoyPoint currentPoint;
        for (double score : hitMap.keySet()) {
            currentPoint = hitMap.get(score);
            scores[cpt] = score;
            probaNTotal += (1 - currentPoint.p) * currentPoint.nTarget;
            cpt++;
        }
        Arrays.sort(scores);

        confidence = new double[scores.length];
        classicalFDR = new double[scores.length];
        probaFDR = new double[scores.length];
        probaFNR = new double[scores.length];
        n = new double[scores.length];
        probaFP = new double[scores.length];
        classicalFP = new double[scores.length];
        probaBenefit = new double[scores.length];
        pep = new double[scores.length];
        decoy = new boolean[scores.length];
        double nTemp = 0;
        double classicalFPTemp = 0;
        double probaFPTemp = 0;
        double probaTP = 0;
        double probaFnrTemp;
        for (int i = 0; i < scores.length; i++) {
            currentPoint = hitMap.get(scores[i]);
            nTemp += currentPoint.nTarget;
            classicalFPTemp += currentPoint.nDecoy;
            probaFPTemp += currentPoint.nTarget * (currentPoint.p);
            probaTP += currentPoint.nTarget * (1 - currentPoint.p);
            probaFnrTemp = 100 * (probaNTotal - probaTP) / probaNTotal;
            pep[i] = 100 * currentPoint.p;
            confidence[i] = 100 * (1 - currentPoint.p);
            n[i] = nTemp;
            classicalFP[i] = classicalFPTemp;
            probaFP[i] = probaFPTemp;
            classicalFDR[i] = 100 * classicalFPTemp / nTemp;
            probaFDR[i] = 100 * probaFPTemp / nTemp;
            probaFNR[i] = probaFnrTemp;
            probaBenefit[i] = 100 - probaFnrTemp;
            decoy[i] = currentPoint.nTarget == 0;
            if (currentPoint.p == 0) {
                break;
            }
        }
    }

    /**
     * Completes the results at the desired FDR threshold
     * @param targetDecoyResults The results containing the threshold
     */
    public void getFDRResults(TargetDecoyResults targetDecoyResults) {
        Double threshold = targetDecoyResults.getFdrLimit();
        if (targetDecoyResults.isClassicalEstimators()) {
            for (int i = scores.length - 1; i >= 0; i--) {
                if (classicalFDR[i] <= threshold && !decoy[i]) {
                    targetDecoyResults.setConfidenceLimit(confidence[i]);
                    targetDecoyResults.setFdrLimit(classicalFDR[i]);
                    targetDecoyResults.setn(n[i]);
                    targetDecoyResults.setnFP(classicalFP[i]);
                    targetDecoyResults.setFnrLimit(probaFNR[i]);
                    targetDecoyResults.setnTPTotal(probaNTotal);
                    targetDecoyResults.setScoreLimit(scores[i]);
                    return;
                }
            }
        } else {
            for (int i = 1; i < scores.length; i++) {
                if (probaFDR[i] > threshold) {
                    for (int k = 1; k < i; k++) {
                        if (!decoy[i - k]) {
                            targetDecoyResults.setConfidenceLimit(confidence[i - k]);
                            targetDecoyResults.setFdrLimit(probaFDR[i - k]);
                            targetDecoyResults.setFnrLimit(probaFNR[i - k]);
                            targetDecoyResults.setn(n[i - k]);
                            targetDecoyResults.setnFP(probaFP[i - k]);
                            targetDecoyResults.setnTPTotal(probaNTotal);
                            targetDecoyResults.setScoreLimit(scores[i - k]);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Completes the results at the desired confidence threshold
     * @param targetDecoyResults The results containing the threshold
     */
    public void getConfidenceResults(TargetDecoyResults targetDecoyResults) {
        double threshold = targetDecoyResults.getConfidenceLimit();
        if (targetDecoyResults.isClassicalEstimators()) {
            for (int i = 1; i < scores.length; i++) {
                if (confidence[i] < threshold) {
                    for (int k = 1; k < i; k++) {
                        if (!decoy[i - k]) {
                            targetDecoyResults.setConfidenceLimit(confidence[i - k]);
                            targetDecoyResults.setFdrLimit(classicalFDR[i - k]);
                            targetDecoyResults.setn(n[i - k]);
                            targetDecoyResults.setnFP(classicalFP[i - k]);
                            targetDecoyResults.setnTPTotal(probaNTotal);
                            targetDecoyResults.setFnrLimit(probaFNR[i - k]);
                            targetDecoyResults.setScoreLimit(scores[i - k]);
                            return;
                        }
                    }
                }
            }
        } else {
            for (int i = 1; i < scores.length; i++) {
                if (confidence[i] < threshold) {
                    for (int k = 1; k < i; k++) {
                        if (!decoy[i - k]) {
                            targetDecoyResults.setConfidenceLimit(confidence[i - k]);
                            targetDecoyResults.setFdrLimit(probaFDR[i - k]);
                            targetDecoyResults.setFnrLimit(probaFNR[i - k]);
                            targetDecoyResults.setn(n[i - k]);
                            targetDecoyResults.setnFP(probaFP[i - k]);
                            targetDecoyResults.setnTPTotal(probaNTotal);
                            targetDecoyResults.setScoreLimit(scores[i - k]);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Completes the results at the desired FNR threshold
     * @param targetDecoyResults The results containing the threshold
     */
    public void getFNRResults(TargetDecoyResults targetDecoyResults) {
        double threshold = targetDecoyResults.getFnrLimit();
        if (targetDecoyResults.isClassicalEstimators()) {
            for (int i = scores.length - 2; i >= 0; i--) {
                if (probaFNR[i] > threshold) {
                    for (int k = 1; k < i; k++) {
                        if (!decoy[i + k]) {
                            targetDecoyResults.setConfidenceLimit(confidence[i + k]);
                            targetDecoyResults.setFdrLimit(classicalFDR[i + k]);
                            targetDecoyResults.setn(n[i + k]);
                            targetDecoyResults.setnFP(classicalFP[i + k]);
                            targetDecoyResults.setFnrLimit(probaFNR[i + k]);
                            targetDecoyResults.setnTPTotal(probaNTotal);
                            targetDecoyResults.setScoreLimit(scores[i - k]);
                            return;
                        }
                    }
                }
            }
        } else {
            for (int i = scores.length - 2; i >= 0; i--) {
                if (probaFNR[i] > threshold) {
                    for (int k = 1; k < i; k++) {
                        if (!decoy[i + k]) {
                            targetDecoyResults.setConfidenceLimit(confidence[i + k]);
                            targetDecoyResults.setFdrLimit(probaFDR[i + k]);
                            targetDecoyResults.setFnrLimit(probaFNR[i + k]);
                            targetDecoyResults.setn(n[i + k]);
                            targetDecoyResults.setnFP(probaFP[i + k]);
                            targetDecoyResults.setnTPTotal(probaNTotal);
                            targetDecoyResults.setScoreLimit(scores[i - k]);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the classical FDR series
     * @return the classical FDR series
     */
    public double[] getClassicalFDR() {
        return classicalFDR;
    }

    /**
     * Returns the confidence series
     * @return the confidence series
     */
    public double[] getConfidence() {
        return confidence;
    }

    /**
     * Returns the probabilistic benefit series
     * @return the probabilistic benefit series
     */
    public double[] getProbaBenefit() {
        return probaBenefit;
    }

    /**
     * Returns the probabilistic FDR series
     * @return the probabilistic FDR series
     */
    public double[] getProbaFDR() {
        return probaFDR;
    }

    /**
     * Returns the probabilistic FNR series
     * @return the probabilistic FNR series
     */
    public double[] getProbaFNR() {
        return probaFNR;
    }

    /**
     * Returns the score series
     * @return the score series
     */
    public double[] getScores() {
        return scores;
    }

    /**
     * Returns the score series
     * @return the score series
     */
    public double[] getPEP() {
        return pep;
    }
}
