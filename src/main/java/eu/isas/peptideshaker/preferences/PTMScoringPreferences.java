/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class contains the PTM scoring preferences
 *
 * @author Marc
 */
public class PTMScoringPreferences implements Serializable {

    /**
     * Serial number for backward compatibility
     */
    static final long serialVersionUID = -6656074270981104708L;
    /**
     * The A-score threshold
     */
    private double aScoreThreshold = 10;
    /**
     * Boolean indicating whether the A-score should be calculated
     */
    private boolean aScoreCalculation = false;
    /**
     * Boolean indicating whether neutral losses shall be accounted in the
     * calculation of the A-score
     */
    private boolean aScoreNeutralLosses = false;

    /**
     * Constructor
     */
    public PTMScoringPreferences() {
    }

    /**
     * Returns a boolean indicating whether the A-score should be calculated
     *
     * @return a boolean indicating whether the A-score should be calculated
     */
    public boolean aScoreCalculation() {
        return aScoreCalculation;
    }

    /**
     * Sets whether the A-score should be calculated
     *
     * @param aScoreCalculation a boolean indicating whether the A-score should
     * be calculated
     */
    public void setaScoreCalculation(boolean aScoreCalculation) {
        this.aScoreCalculation = aScoreCalculation;
    }

    /**
     * Indicates whether the A-score calculation should take neutral losses into
     * account
     *
     * @return a boolean indicating whether the A-score calculation should take
     * neutral losses into account
     */
    public boolean isaScoreNeutralLosses() {
        return aScoreNeutralLosses;
    }

    /**
     * Sets whether the A-score calculation should take neutral losses into
     * account
     *
     * @param aScoreNeutralLosses a boolean indicating whether the A-score
     * calculation should take neutral losses into account
     */
    public void setaScoreNeutralLosses(boolean aScoreNeutralLosses) {
        this.aScoreNeutralLosses = aScoreNeutralLosses;
    }

    /**
     * Returns the A-score threshold
     *
     * @return the A-score threshold
     */
    public double getaScoreThreshold() {
        return aScoreThreshold;
    }

    /**
     * Sets the A-score threshold
     *
     * @param aScoreThreshold the A-score threshold
     */
    public void setaScoreThreshold(double aScoreThreshold) {
        this.aScoreThreshold = aScoreThreshold;
    }
}
