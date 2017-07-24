package eu.isas.peptideshaker.scoring.targetdecoy;

import com.compomics.util.IdObject;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.Serializable;

/**
 * This class will compile Target/decoy results of a certain target/decoy map
 * according to user's validation criteria
 *
 * @author Marc Vaudel
 */
public class TargetDecoyResults extends IdObject implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -8387463582045627644L;
    /**
     * Boolean indicating whether a classical validation was used or an a
     * posteriori probabilistic validation.
     */
    private boolean classicalValidation;
    /**
     * Boolean indicating whether classical or probabilistic estimators are used.
     */
    private boolean classicalEstimators;
    /**
     * The confidence limit.
     */
    private double confidenceLimit = -1;
    /**
     * The FDR limit.
     */
    private double fdrLimit;
    /**
     * The FNR limit.
     */
    private double fnrLimit;
    /**
     * The estimated number of false positives.
     */
    private double nFP;
    /**
     * The estimated number of true positives.
     */
    private double n;
    /**
     * The estimated number of true positives reachable.
     */
    private double nTPTotal;
    /**
     * The corresponding score limit.
     */
    private double scoreLimit;
    /**
     * A boolean indicating that everything was validated.
     */
    private Boolean noValidated = false;
    /**
     * The user input which gave the displayed results.
     */
    private Double userInput;
    /**
     * The type of input 0 &gt; confidence 1 &gt; FDR 2 &gt; FNR.
     */
    private Integer inputType;

    /**
     * Constructor.
     */
    public TargetDecoyResults() {
    }

    /**
     * Returns a boolean indicating whether classical or probabilistic
     * estimators were used.
     *
     * @return a boolean indicating whether classical or probabilistic
     * estimators were used
     */
    public boolean isClassicalEstimators() {
        zooActivateRead();
        return classicalEstimators;
    }

    /**
     * sets whether classical or probabilistic estimators should be used.
     *
     * @param classicalEstimators a boolean indicating whether classical or
     * probabilistic estimators should be used
     */
    public void setClassicalEstimators(boolean classicalEstimators) {
        zooActivateWrite();
        this.classicalEstimators = classicalEstimators;
    }

    /**
     * Returns a boolean indicating whether a classical validation was used.
     *
     * @return a boolean indicating whether a classical validation was used
     */
    public boolean isClassicalValidation() {
        zooActivateRead();
        return classicalValidation;
    }

    /**
     * Sets a boolean indicating whether a classical validation was used.
     *
     * @param classicalValidation a boolean indicating whether a classical
     * validation was used
     */
    public void setClassicalValidation(boolean classicalValidation) {
        zooActivateWrite();
        this.classicalValidation = classicalValidation;
    }

    /**
     * Returns the confidence limit.
     *
     * @return the confidence limit
     */
    public Double getConfidenceLimit() {
        zooActivateRead();
        return confidenceLimit;
    }

    /**
     * Sets the confidence limit.
     *
     * @param confidenceLimit the confidence limit
     */
    public void setConfidenceLimit(double confidenceLimit) {
        zooActivateWrite();
        this.confidenceLimit = confidenceLimit;
    }

    /**
     * Returns the FDR limit.
     *
     * @return the FDR limit
     */
    public Double getFdrLimit() {
        zooActivateRead();
        return fdrLimit;
    }

    /**
     * Sets the FDR limit.
     *
     * @param fdrLimit the FDR limit
     */
    public void setFdrLimit(double fdrLimit) {
        zooActivateWrite();
        this.fdrLimit = fdrLimit;
    }

    /**
     * Returns the FNR limit.
     *
     * @return the FNR limit
     */
    public double getFnrLimit() {
        zooActivateRead();
        return fnrLimit;
    }

    /**
     * Sets the FNR limit.
     *
     * @param fnrLimit the FNR limit
     */
    public void setFnrLimit(double fnrLimit) {
        zooActivateWrite();
        this.fnrLimit = fnrLimit;
    }

    /**
     * Returns the estimated number of false positives.
     *
     * @return the estimated number of false positives
     */
    public double getnFP() {
        zooActivateRead();
        return nFP;
    }

    /**
     * Sets the estimated number of false positives.
     *
     * @param nFP the estimated number of false positives
     */
    public void setnFP(double nFP) {
        zooActivateWrite();
        this.nFP = nFP;
    }

    /**
     * Returns the estimated number of retained True positives.
     *
     * @return the estimated number of retained True positives
     */
    public double getnTP() {
        zooActivateRead();
        return n - nFP;
    }

    /**
     * Sets the number of retained hits.
     *
     * @param n the estimated number of retained True positives
     */
    public void setn(double n) {
        zooActivateWrite();
        this.n = n;
    }

    /**
     * Returns the estimated total number of True positives.
     *
     * @return the estimated total number of True positives
     */
    public double getnTPTotal() {
        zooActivateRead();
        return nTPTotal;
    }

    /**
     * Sets the estimated total number of True positives.
     *
     * @param nTPTotal the estimated total number of True positives
     */
    public void setnTPTotal(double nTPTotal) {
        zooActivateWrite();
        this.nTPTotal = nTPTotal;
    }

    /**
     * Returns the number of retained hits.
     *
     * @return the number of retained hits
     */
    public double getN() {
        zooActivateRead();
        return n;
    }

    /**
     * Returns the score limit obtained with the current validation settings.
     *
     * @return the score limit obtained with the current validation settings
     */
    public double getScoreLimit() {
        zooActivateRead();
        return scoreLimit;
    }

    /**
     * Returns the score limit obtained with the current validation settings.
     *
     * @return the score limit obtained with the current validation settings
     */
    public double getLogScoreLimit() {
        zooActivateRead();
        return PSParameter.getScore(scoreLimit);
    }

    /**
     * Sets the score limit obtained with the current validation settings.
     *
     * @param scoreLimit the score limit obtained with the current validation
     * settings
     */
    public void setScoreLimit(double scoreLimit) {
        zooActivateWrite();
        this.scoreLimit = scoreLimit;
    }

    /**
     * Returns a boolean indicating that everything was validated.
     * 
     * @return a boolean indicating that everything was validated
     */
    public boolean noValidated() {
        zooActivateRead();
        return (noValidated != null) && noValidated;
    }

    /**
     * Sets whether everything was validated.
     *
     * @param validateAll a boolean indicating whether everything should be
     * validated
     */
    public void setNoValidated(boolean validateAll) {
        zooActivateWrite();
        this.noValidated = validateAll;
    }

    /**
     * Returns the type of input.  0 &gt; confidence 1 &gt; FDR 2 &gt; FNR.
     *
     * @return the type of input
     */
    public Integer getInputType() {
        zooActivateRead();
        if (inputType == null) {
            inputType = 1;
        }
        return inputType;
    }

    /**
     * Sets the type of input.
     *
     * @param inputType the input type
     */
    public void setInputType(Integer inputType) {
        zooActivateWrite();
        this.inputType = inputType;
    }

    /**
     * Returns the user input.
     *
     * @return the user input
     */
    public Double getUserInput() {
        zooActivateRead();
        if (userInput == null) {
            userInput = 1.0;
        }
        return userInput;
    }

    /**
     * Sets the user input.
     *
     * @param userInput the user input
     */
    public void setUserInput(Double userInput) {
        zooActivateWrite();
        this.userInput = userInput;
    }
}
