package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class groups the user preferences for the initial processing.
 *
 * @author Marc Vaudel
 */
public class ProcessingPreferences implements Serializable {

    /**
     * Serial number for backward compatibility
     */
    static final long serialVersionUID = -5883143685674607162L;
    /**
     * Boolean indicating whether the A-score should be estimated.
     *
     * @deprecated use the PTM scoring preferences instead
     */
    private boolean aScore = false;
    /**
     * The default protein FDR.
     */
    private double proteinFDR = 1.0;
    /**
     * The default peptide FDR.
     */
    private double peptideFDR = 1.0;
    /**
     * The default PSM FDR.
     */
    private double psmFDR = 1.0;

    /**
     * Constructor with default settings.
     */
    public ProcessingPreferences() {
    }

    /**
     * Indicates whether the A-score should be calculated.
     *
     * @deprecated use the PTM scoring preferences instead
     * @return a boolean indicating whether the A-score should be calculated
     */
    public boolean isAScoreCalculated() {
        return aScore;
    }

    /**
     * Sets whether the A-score should be calculated.
     *
     * @deprecated use the PTM scoring preferences instead
     * @param shouldEstimateAScore whether the A-score should be calculated
     */
    public void estimateAScore(boolean shouldEstimateAScore) {
        this.aScore = shouldEstimateAScore;
    }

    /**
     * Returns the initial peptide FDR.
     *
     * @return the initial peptide FDR
     */
    public double getPeptideFDR() {
        return peptideFDR;
    }

    /**
     * Sets the initial peptide FDR.
     *
     * @param peptideFDR the initial peptide FDR
     */
    public void setPeptideFDR(double peptideFDR) {
        this.peptideFDR = peptideFDR;
    }

    /**
     * Returns the initial protein FDR.
     *
     * @return the initial protein FDR
     */
    public double getProteinFDR() {
        return proteinFDR;
    }

    /**
     * Sets the initial protein FDR.
     *
     * @param proteinFDR the initial protein FDR
     */
    public void setProteinFDR(double proteinFDR) {
        this.proteinFDR = proteinFDR;
    }

    /**
     * Returns the initial PSM FDR.
     *
     * @return the initial PSM FDR
     */
    public double getPsmFDR() {
        return psmFDR;
    }

    /**
     * Sets the initial PSM FDR.
     *
     * @param psmFDR the initial PSM FDR
     */
    public void setPsmFDR(double psmFDR) {
        this.psmFDR = psmFDR;
    }
}
