/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class groups the user preferences for the initial processing
 *
 * @author marc
 */
public class ProcessingPreferences implements Serializable {

    /**
     * boolean indicating whether the A-score should be estimated
     */
    private boolean aScore = false;
    /**
     * The default protein FDR
     */
    private double proteinFDR = 1.0;
    /**
     * The default peptide FDR
     */
    private double peptideFDR = 1.0;
    /**
     * The default PSM FDR
     */
    private double psmFDR = 1.0;
    
    /**
     * Constructor with default settings
     */
    public ProcessingPreferences() {
        
    }

    /**
     * Indicates whether the A-score should be calculated
     * @return a boolean indicating whether the A-score should be calculated
     */
    public boolean isAScoreCalculated() {
        return aScore;
    }

    /**
     * sets whether the A-score should be calculated
     * @param aScore whether the A-score should be calculated
     */
    public void estimateAScore(boolean shouldEstimateAScore) {
        this.aScore = shouldEstimateAScore;
    }

    /**
     * returns the initial peptide FDR
     * @return the initial peptide FDR
     */
    public double getPeptideFDR() {
        return peptideFDR;
    }

    /**
     * Sets the initial peptide FDR
     * @param peptideFDR the initial peptide FDR
     */
    public void setPeptideFDR(double peptideFDR) {
        this.peptideFDR = peptideFDR;
    }

    /**
     * returns the initial protein FDR
     * @return the initial protein FDR
     */
    public double getProteinFDR() {
        return proteinFDR;
    }

    /**
     * Sets the initial protein FDR
     * @param proteinFDR the initial protein FDR
     */
    public void setProteinFDR(double proteinFDR) {
        this.proteinFDR = proteinFDR;
    }

    /**
     * Returns the initial PSM FDR
     * @return the initial PSM FDR
     */
    public double getPsmFDR() {
        return psmFDR;
    }

    /**
     * Sets the initial PSM FDR
     * @param psmFDR the initial PSM FDR
     */
    public void setPsmFDR(double psmFDR) {
        this.psmFDR = psmFDR;
    }
}
