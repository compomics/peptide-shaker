package eu.isas.peptideshaker.scoring.targetdecoy;

import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class will contain the PEP, FDR and FNR values of a target decoy map
 * directly available for plotting.
 *
 * @author Marc Vaudel
 */
public class TargetDecoySeries {

    /**
     * The probabilistic score series.
     */
    private final double[] scores;
    /**
     * The log transformed probabilistic score series.
     */
    private final double[] scoresLog;
    /**
     * The confidence series.
     */
    private final double[] confidence;
    /**
     * The confidence series corresponding to the log score series.
     */
    private final double[] confidenceLog;
    /**
     * The PEP series.
     */
    private final double[] pep;
    /**
     * The bin size to use in the histogram.
     */
    private final double binSize = 5;
    /**
     * The series of bins to use for the target / decoy histogram.
     */
    private double[] tdBins;
    /**
     * The target histogram series.
     */
    private double[] nTarget;
    /**
     * The decoy histogram series.
     */
    private double[] nDecoy;
    /**
     * The classical FDR.
     */
    private final double[] fdr;
    /**
     * The probabilistic FNR.
     */
    private final double[] fnr;
    /**
     * The number of validated target hits.
     */
    private final double[] n;
    /**
     * The classically estimated number of false positives.
     */
    private final double[] fp;
    /**
     * Indicates whether the current point is only made of decoy hits.
     */
    private final boolean[] decoy;
    /**
     * The probabilistically estimated total number of false positives.
     */
    private double probaNTotal;

    /**
     * Constructor.
     *
     * @param hitMap a map as present in target decoy maps
     */
    public TargetDecoySeries(HashMap<Double, TargetDecoyPoint> hitMap) {

        scores = new double[hitMap.size()];
        scoresLog = new double[scores.length];
        probaNTotal = 0;
        int counter = 0;
        TargetDecoyPoint currentPoint;
        double minScore = 0, maxScore = 100;

        for (double score : hitMap.keySet()) {
            
            currentPoint = hitMap.get(score);
            double scoreLog = PSParameter.transformScore(score);
            scores[counter] = score;
            scoresLog[counter] = scoreLog;
            probaNTotal += (1 - currentPoint.p) * currentPoint.nTarget;
            counter++;
            
            if (scoreLog < minScore) {
            
                minScore = scoreLog;
            
            }
            
            if (scoreLog > maxScore) {
            
                maxScore = scoreLog;
            
            }
        }

        int histogramScoreMin = (int) minScore;
        int histogramScoreMax = (int) maxScore;
        initiateTDHistogram(histogramScoreMin, histogramScoreMax);

        Arrays.sort(scores);
        Arrays.sort(scoresLog);

        confidence = new double[scores.length];
        confidenceLog = new double[scores.length];
        fdr = new double[scores.length];
        fnr = new double[scores.length];
        n = new double[scores.length];
        fp = new double[scores.length];
        pep = new double[scores.length];
        decoy = new boolean[scores.length];

        double nTemp = 0;
        double fpTemp = 0;
        double probaTP = 0;
        double fnrTemp;

        for (int i = 0; i < scores.length; i++) {
            
            double score = scores[i];
            
            currentPoint = hitMap.get(score);
            nTemp += currentPoint.nTarget;
            fpTemp += currentPoint.nDecoy;
            probaTP += currentPoint.nTarget * (1 - currentPoint.p);
            fnrTemp = 100.0 * (probaNTotal - probaTP) / probaNTotal;
            pep[i] = 100.0 * currentPoint.p;
            double confidenceAtI = 100 * (1 - currentPoint.p);
            confidence[i] = confidenceAtI;
            int iInvert = scores.length - i - 1;
            confidenceLog[iInvert] = confidenceAtI;
            n[i] = nTemp;
            fp[i] = fpTemp;
            fdr[i] = 100.0 * fpTemp / nTemp;
            fnr[i] = fnrTemp;
            decoy[i] = currentPoint.nTarget == 0;

            double scoreLog = scoresLog[iInvert];
            int bin = ((int) (Math.round((scoreLog - histogramScoreMin) / binSize)));
            nDecoy[bin] += currentPoint.nDecoy;
            nTarget[bin] += currentPoint.nTarget;
            
        }
    }

    /**
     * Creates the bins of the target decoy histogram and set empty values for
     * the target and decoy series.
     *
     * @param histogramScoreMin the minimal value of the histogram
     * @param histogramScoreMax the maximal value of the histogram
     */
    private void initiateTDHistogram(int histogramScoreMin, int histogramScoreMax) {

        int nBins = (int) (Math.floor(histogramScoreMax / binSize)) - (int) (Math.floor(histogramScoreMin / binSize)) + 1;

        tdBins = new double[nBins];
        nTarget = new double[nBins];
        nDecoy = new double[nBins];

        for (int i = 0; i < nBins; i++) {
            tdBins[i] = histogramScoreMin + (i * binSize);
            nTarget[i] = 0.0;
            nDecoy[i] = 0.0;
        }
    }

    /**
     * Completes the results at the desired FDR threshold.
     *
     * @param targetDecoyResults the results containing the threshold
     */
    public void getFDRResults(TargetDecoyResults targetDecoyResults) {

        double threshold = targetDecoyResults.getFdrLimit();

        targetDecoyResults.setNoValidated(false);
        
        for (int i = scores.length - 1; i >= 0; i--) {
        
            if (fdr[i] <= threshold && !decoy[i]) {
            
                targetDecoyResults.setConfidenceLimit(confidence[i]);
                targetDecoyResults.setFdrLimit(fdr[i]);
                targetDecoyResults.setn(n[i]);
                targetDecoyResults.setnFP(fp[i]);
                targetDecoyResults.setFnrLimit(fnr[i]);
                targetDecoyResults.setnTPTotal(probaNTotal);
                targetDecoyResults.setScoreLimit(scores[i]);
                return;
                
            } else if (i == 0) {
                
                targetDecoyResults.setNoValidated(true);
                targetDecoyResults.setFdrLimit(0);
                targetDecoyResults.setnFP(0);
                targetDecoyResults.setConfidenceLimit(0);
                targetDecoyResults.setn(0);
                targetDecoyResults.setFnrLimit(fnr[0]);
                targetDecoyResults.setnTPTotal(probaNTotal);
                targetDecoyResults.setScoreLimit(scores[0]);
                
            }
        }
    }

    /**
     * Completes the results at the desired confidence threshold.
     *
     * @param targetDecoyResults the results containing the threshold
     */
    public void getConfidenceResults(TargetDecoyResults targetDecoyResults) {

        double threshold = targetDecoyResults.getConfidenceLimit();

        for (int i = 0; i < scores.length - 1; i++) {
            if (confidence[i] < threshold) {
                for (int k = i; k >= 0; k--) {
                    if (!decoy[k]) {
                        targetDecoyResults.setNoValidated(false);
                        targetDecoyResults.setFdrLimit(fdr[k]);
                        targetDecoyResults.setnFP(fp[k]);
                        targetDecoyResults.setConfidenceLimit(confidence[k]);
                        targetDecoyResults.setFnrLimit(fnr[k]);
                        targetDecoyResults.setn(n[k]);
                        targetDecoyResults.setnTPTotal(probaNTotal);
                        targetDecoyResults.setScoreLimit(scores[k]);
                        return;
                    }
                }
                targetDecoyResults.setNoValidated(true);
                targetDecoyResults.setFdrLimit(0);
                targetDecoyResults.setnFP(0);
                targetDecoyResults.setConfidenceLimit(confidence[0]);
                targetDecoyResults.setn(0);
                targetDecoyResults.setnTPTotal(probaNTotal);
                targetDecoyResults.setFnrLimit(probaNTotal);
                targetDecoyResults.setScoreLimit(scores[0]);
                return;
            }
        }
    }

    /**
     * Completes the results at the desired FNR threshold.
     *
     * @param targetDecoyResults the results containing the threshold
     */
    public void getFNRResults(TargetDecoyResults targetDecoyResults) {

        double threshold = targetDecoyResults.getFnrLimit();
        targetDecoyResults.setNoValidated(false);

        for (int i = scores.length - 1; i >= 0; i--) {
            if (fnr[i] > threshold || i == 0) {
                for (int k = i; k < scores.length; k++) {
                    if (!decoy[k]) {
                        targetDecoyResults.setConfidenceLimit(confidence[k]);
                            targetDecoyResults.setFdrLimit(fdr[k]);
                            targetDecoyResults.setnFP(fp[k]);
                        targetDecoyResults.setn(n[k]);
                        targetDecoyResults.setFnrLimit(fnr[k]);
                        targetDecoyResults.setnTPTotal(probaNTotal);
                        targetDecoyResults.setScoreLimit(scores[k]);
                        return;
                    }
                }
                targetDecoyResults.setNoValidated(true);
                targetDecoyResults.setFdrLimit(0);
                targetDecoyResults.setnFP(0);
                targetDecoyResults.setConfidenceLimit(confidence[0]);
                targetDecoyResults.setn(0);
                targetDecoyResults.setnTPTotal(probaNTotal);
                targetDecoyResults.setFnrLimit(probaNTotal);
                targetDecoyResults.setScoreLimit(scores[0]);
            }
        }
    }

    /**
     * Returns the target decoy bins to use for the histogram.
     *
     * @return the target decoy bins to use for the histogram
     */
    public double[] getTdBins() {
        return tdBins;
    }

    /**
     * Returns the target series of the target decoy histogram.
     *
     * @return the target series of the target decoy histogram
     */
    public double[] getnTarget() {
        return nTarget;
    }

    /**
     * Returns the decoy series of the target decoy histogram.
     *
     * @return the decoy series of the target decoy histogram
     */
    public double[] getnDecoy() {
        return nDecoy;
    }

    /**
     * Returns the FDR series.
     *
     * @return the FDR series
     */
    public double[] getFDR() {
        return fdr;
    }

    /**
     * Returns the confidence series.
     *
     * @return the confidence series
     */
    public double[] getConfidence() {
        return confidence;
    }

    /**
     * Returns the confidence log series.
     *
     * @return the confidence log series
     */
    public double[] getConfidenceLog() {
        return confidenceLog;
    }

    /**
     * Returns the benefit series.
     *
     * @return the benefit series
     */
    public double[] getBenefit() {
        
        return Arrays.stream(fnr)
                .map(fnrValue -> 100.0 - fnrValue)
                .toArray();
        
    }

    /**
     * Returns the probabilistic FNR series.
     *
     * @return the probabilistic FNR series
     */
    public double[] getFNR() {
        return fnr;
    }

    /**
     * Returns the score series.
     *
     * @return the score series
     */
    public double[] getScores() {
        return scores;
    }

    /**
     * Returns the log score series.
     *
     * @return the log score series
     */
    public double[] getScoresLog() {
        return scoresLog;
    }

    /**
     * Returns the score series.
     *
     * @return the score series
     */
    public double[] getPEP() {
        return pep;
    }
}
