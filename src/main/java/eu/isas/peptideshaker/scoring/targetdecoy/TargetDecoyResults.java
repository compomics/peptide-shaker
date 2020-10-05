package eu.isas.peptideshaker.scoring.targetdecoy;

import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import java.io.Serializable;

/**
 * This class will compile Target/decoy results of a certain target/decoy map
 * according to user's validation criteria
 *
 * @author Marc Vaudel
 */
public class TargetDecoyResults extends ExperimentObject implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -8387463582045627644L;
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
    private double userInput = 1.0;
    /**
     * The type of input 0 &gt; confidence 1 &gt; FDR 2 &gt; FNR.
     */
    private int inputType = 1;

    /**
     * Constructor.
     */
    public TargetDecoyResults() {
    }

    /**
     * Returns the confidence limit.
     *
     * @return the confidence limit
     */
    public double getConfidenceLimit() {
        
        return confidenceLimit;
    }

    /**
     * Sets the confidence limit.
     *
     * @param confidenceLimit the confidence limit
     */
    public void setConfidenceLimit(double confidenceLimit) {
        
        this.confidenceLimit = confidenceLimit;
    }

    /**
     * Returns the FDR limit.
     *
     * @return the FDR limit
     */
    public double getFdrLimit() {
        
        return fdrLimit;
    }

    /**
     * Sets the FDR limit.
     *
     * @param fdrLimit the FDR limit
     */
    public void setFdrLimit(double fdrLimit) {
        
        this.fdrLimit = fdrLimit;
    }

    /**
     * Returns the FNR limit.
     *
     * @return the FNR limit
     */
    public double getFnrLimit() {
        
        return fnrLimit;
    }

    /**
     * Sets the FNR limit.
     *
     * @param fnrLimit the FNR limit
     */
    public void setFnrLimit(double fnrLimit) {
        
        this.fnrLimit = fnrLimit;
    }

    /**
     * Returns the estimated number of false positives.
     *
     * @return the estimated number of false positives
     */
    public double getnFP() {
        
        return nFP;
    }

    /**
     * Sets the estimated number of false positives.
     *
     * @param nFP the estimated number of false positives
     */
    public void setnFP(double nFP) {
        
        this.nFP = nFP;
    }

    /**
     * Returns the estimated number of retained True positives.
     *
     * @return the estimated number of retained True positives
     */
    public double getnTP() {
        
        return n - nFP;
    }

    /**
     * Sets the number of retained hits.
     *
     * @param n the estimated number of retained True positives
     */
    public void setn(double n) {
        
        this.n = n;
    }

    /**
     * Returns the estimated total number of True positives.
     *
     * @return the estimated total number of True positives
     */
    public double getnTPTotal() {
        
        return nTPTotal;
    }

    /**
     * Sets the estimated total number of True positives.
     *
     * @param nTPTotal the estimated total number of True positives
     */
    public void setnTPTotal(double nTPTotal) {
        
        this.nTPTotal = nTPTotal;
    }

    /**
     * Returns the number of retained hits.
     *
     * @return the number of retained hits
     */
    public double getN() {
        
        return n;
    }

    /**
     * Returns the score limit obtained with the current validation settings.
     *
     * @return the score limit obtained with the current validation settings
     */
    public double getScoreLimit() {
        
        return scoreLimit;
    }

    /**
     * Returns the score limit obtained with the current validation settings.
     *
     * @return the score limit obtained with the current validation settings
     */
    public double getLogScoreLimit() {
        
        return PSParameter.transformScore(scoreLimit);
    }

    /**
     * Sets the score limit obtained with the current validation settings.
     *
     * @param scoreLimit the score limit obtained with the current validation
     * settings
     */
    public void setScoreLimit(double scoreLimit) {
        
        this.scoreLimit = scoreLimit;
    }

    /**
     * Returns a boolean indicating that everything was validated.
     * 
     * @return a boolean indicating that everything was validated
     */
    public boolean noValidated() {
        
        return (noValidated != null) && noValidated;
    }

    /**
     * Sets whether everything was validated.
     *
     * @param validateAll a boolean indicating whether everything should be
     * validated
     */
    public void setNoValidated(boolean validateAll) {
        
        this.noValidated = validateAll;
    }

    /**
     * Returns the type of input.  0 &gt; confidence 1 &gt; FDR 2 &gt; FNR.
     *
     * @return the type of input
     */
    public int getInputType() {
        
        return inputType;
    }

    /**
     * Sets the type of input.
     *
     * @param inputType the input type
     */
    public void setInputType(int inputType) {
        
        this.inputType = inputType;
    }

    /**
     * Returns the user input.
     *
     * @return the user input
     */
    public double getUserInput() {
        
        return userInput;
    }

    /**
     * Sets the user input.
     *
     * @param userInput the user input
     */
    public void setUserInput(double userInput) {
        
        this.userInput = userInput;
    }
}
