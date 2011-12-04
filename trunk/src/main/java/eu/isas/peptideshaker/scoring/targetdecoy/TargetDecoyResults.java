package eu.isas.peptideshaker.scoring.targetdecoy;

import java.io.Serializable;

/**
 * This class will compile Target/decoy results of a certain target/decoy map according to user's validation criteria
 *
 * @author Marc Vaudel
 */
public class TargetDecoyResults implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = -8387463582045627644L;
    /**
     * Boolean indicating whether a classical validation was used or an a posteriori probabilistic validation
     */
    private boolean classicalValidation;
    /**
     * Boolean indicating whether classical or probabilistic estimators are used
     */
    private boolean classicalEstimators;
    /**
     * The confidence limit
     */
    private double confidenceLimit = -1;
    /**
     * The FDR limit
     */
    private double fdrLimit;
    /**
     * The FNR limit
     */
    private double fnrLimit;
    /**
     * The estimated amount of false positives
     */
    private double nFP;
    /**
     * The estimated amount of true positives
     */
    private double n;
    /**
     * The estimated amount of true positives reachable
     */
    private double nTPTotal;
    /**
     * The corresponding score limit
     */
    private double scoreLimit;

    /**
     * Constructor
     */
    public TargetDecoyResults() {
    }

    /**
     * Returns a boolean indicating whether classical or probabilistic estimators were used
     * @return  a boolean indicating whether classical or probabilistic estimators were used
     */
    public boolean isClassicalEstimators() {
        return classicalEstimators;
    }

    /**
     * sets whether classical or probabilistic estimators should be used
     * @param classicalEstimators a boolean indicating whether classical or probabilistic estimators should be used
     */
    public void setClassicalEstimators(boolean classicalEstimators) {
        this.classicalEstimators = classicalEstimators;
    }

    /**
     * Returns a boolean indicating whether a classical validation was used
     * @return a boolean indicating whether a classical validation was used
     */
    public boolean isClassicalValidation() {
        return classicalValidation;
    }

    /**
     * Sets a boolean indicating whether a classical validation was used
     * @param classicalValidation  a boolean indicating whether a classical validation was used
     */
    public void setClassicalValidation(boolean classicalValidation) {
        this.classicalValidation = classicalValidation;
    }

    /**
     * Returns the confidence limit
     * @return the confidence limit
     */
    public Double getConfidenceLimit() {
        return confidenceLimit;
    }

    /**
     * Sets the confidence limit
     * @param confidenceLimit the confidence limit
     */
    public void setConfidenceLimit(double confidenceLimit) {
        this.confidenceLimit = confidenceLimit;
    }

    /**
     * Returns the FDR limit
     * @return the FDR limit
     */
    public Double getFdrLimit() {
        return fdrLimit;
    }

    /**
     * Sets the FDR limit
     * @param fdrLimit the FDR limit
     */
    public void setFdrLimit(double fdrLimit) {
        this.fdrLimit = fdrLimit;
    }

    /**
     * Returns the FNR limit
     * @return the FNR limit
     */
    public double getFnrLimit() {
        return fnrLimit;
    }

    /**
     * Sets the FNR limit
     * @param fnrLimit the FNR limit
     */
    public void setFnrLimit(double fnrLimit) {
        this.fnrLimit = fnrLimit;
    }

    /**
     * Returns the estimated number of false positives
     * @return the estimated number of false positives
     */
    public double getnFP() {
        return nFP;
    }

    /**
     * Sets the estimated number of false positives
     * @param nFP the estimated number of false positives
     */
    public void setnFP(double nFP) {
        this.nFP = nFP;
    }

    /**
     * Returns the estimated number of retained True positives
     * @return the estimated number of retained True positives
     */
    public double getnTP() {
        return n - nFP;
    }

    /**
     * Sets the  number of retained hits
     * @param n the estimated number of retained True positives
     */
    public void setn(double n) {
        this.n = n;
    }

    /**
     * Returns the estimated total number of True positives
     * @return the estimated total number of True positives
     */
    public double getnTPTotal() {
        return nTPTotal;
    }

    /**
     * Sets the estimated total number of True positives
     * @param nTPTotal the estimated total number of True positives
     */
    public void setnTPTotal(double nTPTotal) {
        this.nTPTotal = nTPTotal;
    }

    /**
     * Returns the amount of retained hits
     * @return the amount of retained hits
     */
    public double getN() {
        return n;
    }

    /**
     * Returns the score limit obtained with the current validation settings
     * @return the score limit obtained with the current validation settings
     */
    public double getScoreLimit() {
        return scoreLimit;
    }

    /**
     * Sets the score limit obtained with the current validation settings
     * @param scoreLimit the score limit obtained with the current validation settings
     */
    public void setScoreLimit(double scoreLimit) {
        this.scoreLimit = scoreLimit;
    }
}
