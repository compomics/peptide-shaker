package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class contains the PTM scoring preferences.
 *
 * @deprecated use the com.compomics.util.preferences version instead
 * @author Marc Vaudel
 */
public class PTMScoringPreferences implements Serializable {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = -6656074270981104708L;
    /**
     * Boolean indicating whether the A-score should be calculated.
     */
    private boolean aScoreCalculation = true;
    /**
     * Boolean indicating whether neutral losses shall be accounted in the
     * calculation of the A-score.
     */
    private boolean aScoreNeutralLosses = false;
    /**
     * the FLR threshold in percent
     */
    private double flr = 1.0;

    /**
     * Constructor.
     */
    public PTMScoringPreferences() {
    }

    /**
     * Returns a boolean indicating whether the A-score should be calculated.
     *
     * @return a boolean indicating whether the A-score should be calculated
     */
    public boolean aScoreCalculation() {
        return aScoreCalculation;
    }

    /**
     * Sets whether the A-score should be calculated.
     *
     * @param aScoreCalculation a boolean indicating whether the A-score should
     * be calculated
     */
    public void setaScoreCalculation(boolean aScoreCalculation) {
        this.aScoreCalculation = aScoreCalculation;
    }

    /**
     * Indicates whether the A-score calculation should take neutral losses into
     * account.
     *
     * @return a boolean indicating whether the A-score calculation should take
     * neutral losses into account
     */
    public boolean isaScoreNeutralLosses() {
        return aScoreNeutralLosses;
    }

    /**
     * Sets whether the A-score calculation should take neutral losses into
     * account.
     *
     * @param aScoreNeutralLosses a boolean indicating whether the A-score
     * calculation should take neutral losses into account
     */
    public void setaScoreNeutralLosses(boolean aScoreNeutralLosses) {
        this.aScoreNeutralLosses = aScoreNeutralLosses;
    }

    /**
     * Returns the FLR threshold.
     *
     * @return the FLR threshold
     */
    public double getFlrThreshold() {
        return flr;
    }

    /**
     * Sets the FLR threshold.
     *
     * @param flr the FLR threshold
     */
    public void setFlrThreshold(double flr) {
        this.flr = flr;
    }
}
