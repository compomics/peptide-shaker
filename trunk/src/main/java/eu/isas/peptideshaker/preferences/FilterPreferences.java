/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class groups the display filter preferences
 *
 * @author marc
 */
public class FilterPreferences implements Serializable {
    
    private String proteinAccession = "";
    private String proteinDescription = "";
    private String proteinCoverage = "";
    private String spectrumCounting = "";
    private String nPeptides = "";
    private String proteinNSpectra = "";
    private String proteinScore = "";
    private String proteinConfidence = "";
    private int coverageButtonSelection = 0;
    private int spectrumCountingButtonSelection = 0;
    private int nPeptidesButtonSelection = 0;
    private int proteinNSpectraButtonSelection = 0;
    private int proteinScoreButtonSelection = 0;
    private int proteinConfidenceButtonSelection = 0;
    /**
     * The current protein inference filter selection.
     */
    private int currentProteinInferenceFilterSelection = 5;
    
    /**
     * Constructors
     */
    public FilterPreferences() {
        
    }

    public String getnPeptides() {
        return nPeptides;
    }

    public void setnPeptides(String nPeptides) {
        this.nPeptides = nPeptides;
    }

    public String getProteinAccession() {
        return proteinAccession;
    }

    public void setProteinAccession(String proteinAccession) {
        this.proteinAccession = proteinAccession;
    }

    public String getProteinConfidence() {
        return proteinConfidence;
    }

    public void setProteinConfidence(String proteinConfidence) {
        this.proteinConfidence = proteinConfidence;
    }

    public String getProteinCoverage() {
        return proteinCoverage;
    }

    public void setProteinCoverage(String proteinCoverage) {
        this.proteinCoverage = proteinCoverage;
    }

    public String getProteinDescription() {
        return proteinDescription;
    }

    public void setProteinDescription(String proteinDescription) {
        this.proteinDescription = proteinDescription;
    }

    public String getProteinNSpectra() {
        return proteinNSpectra;
    }

    public void setProteinNSpectra(String proteinNSpectra) {
        this.proteinNSpectra = proteinNSpectra;
    }

    public String getProteinScore() {
        return proteinScore;
    }

    public void setProteinScore(String proteinScore) {
        this.proteinScore = proteinScore;
    }

    public String getSpectrumCounting() {
        return spectrumCounting;
    }

    public void setSpectrumCounting(String spectrumCounting) {
        this.spectrumCounting = spectrumCounting;
    }

    public int getCoverageButtonSelection() {
        return coverageButtonSelection;
    }

    public void setCoverageButtonSelection(int coverageButtonSelection) {
        this.coverageButtonSelection = coverageButtonSelection;
    }

    public int getCurrentProteinInferenceFilterSelection() {
        return currentProteinInferenceFilterSelection;
    }

    public void setCurrentProteinInferenceFilterSelection(int currentProteinInferenceFilterSelection) {
        this.currentProteinInferenceFilterSelection = currentProteinInferenceFilterSelection;
    }

    public int getnPeptidesButtonSelection() {
        return nPeptidesButtonSelection;
    }

    public void setnPeptidesButtonSelection(int nPeptidesButtonSelection) {
        this.nPeptidesButtonSelection = nPeptidesButtonSelection;
    }

    public int getProteinConfidenceButtonSelection() {
        return proteinConfidenceButtonSelection;
    }

    public void setProteinConfidenceButtonSelection(int proteinConfidenceButtonSelection) {
        this.proteinConfidenceButtonSelection = proteinConfidenceButtonSelection;
    }

    public int getProteinNSpectraButtonSelection() {
        return proteinNSpectraButtonSelection;
    }

    public void setProteinNSpectraButtonSelection(int proteinNSpectraButtonSelection) {
        this.proteinNSpectraButtonSelection = proteinNSpectraButtonSelection;
    }

    public int getProteinScoreButtonSelection() {
        return proteinScoreButtonSelection;
    }

    public void setProteinScoreButtonSelection(int proteinScoreButtonSelection) {
        this.proteinScoreButtonSelection = proteinScoreButtonSelection;
    }

    public int getSpectrumCountingButtonSelection() {
        return spectrumCountingButtonSelection;
    }

    public void setSpectrumCountingButtonSelection(int spectrumCountingButtonSelection) {
        this.spectrumCountingButtonSelection = spectrumCountingButtonSelection;
    }
    
    
    
}
