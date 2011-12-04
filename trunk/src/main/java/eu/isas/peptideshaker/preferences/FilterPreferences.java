package eu.isas.peptideshaker.preferences;

import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import java.io.Serializable;
import java.util.HashMap;

/**
 * This class groups the display filter preferences
 *
 * @author Marc Vaudel
 */
public class FilterPreferences implements Serializable {

    /**
     * Serial number for serialization compatibility
     */
    static final long serialVersionUID = -348520469065277089L;
    /**
     * The protein star filters
     */
    private HashMap<String, ProteinFilter> proteinStarFilters = new HashMap<String, ProteinFilter>();
    /**
     * The protein hide filters
     */
    private HashMap<String, ProteinFilter> proteinHideFilters = new HashMap<String, ProteinFilter>();
    /**
     * The peptide star filters
     */
    private HashMap<String, PeptideFilter> peptideStarFilters = new HashMap<String, PeptideFilter>();
    /**
     * The peptide hide filters
     */
    private HashMap<String, PeptideFilter> peptideHideFilters = new HashMap<String, PeptideFilter>();
    /**
     * The psm star filters
     */
    private HashMap<String, PsmFilter> psmStarFilters = new HashMap<String, PsmFilter>();
    /**
     * The psm hide filters
     */
    private HashMap<String, PsmFilter> psmHideFilters = new HashMap<String, PsmFilter>();
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

    /**
     * Returns the protein hide filters
     * @return the protein hide filters 
     */
    public HashMap<String, ProteinFilter> getProteinHideFilters() {
        return proteinHideFilters;
    }

    /**
     * Sets  the protein hide filters
     * @param proteinHideFilters  the protein hide filters
     */
    public void setProteinHideFilters(HashMap<String, ProteinFilter> proteinHideFilters) {
        this.proteinHideFilters = proteinHideFilters;
    }

    /**
     * Returns the protein star filters
     * @return the protein star filters
     */
    public HashMap<String, ProteinFilter> getProteinStarFilters() {
        return proteinStarFilters;
    }

    /**
     * Sets the protein star filters
     * @param proteinStarFilters the protein star filters
     */
    public void setProteinStarFilters(HashMap<String, ProteinFilter> proteinStarFilters) {
        this.proteinStarFilters = proteinStarFilters;
    }

    /**
     * Returns the peptide hide filters
     * @return the peptide hide filters 
     */
    public HashMap<String, PeptideFilter> getPeptideHideFilters() {
        return peptideHideFilters;
    }

    /**
     * Sets  the peptide hide filters
     * @param peptideHideFilters  the peptide hide filters
     */
    public void setPeptideHideFilters(HashMap<String, PeptideFilter> peptideHideFilters) {
        this.peptideHideFilters = peptideHideFilters;
    }

    /**
     * Returns the peptide star filters
     * @return the peptide star filters
     */
    public HashMap<String, PeptideFilter> getPeptideStarFilters() {
        return peptideStarFilters;
    }

    /**
     * Sets the peptide star filters
     * @param peptideStarFilters the peptide star filters
     */
    public void setPeptideStarFilters(HashMap<String, PeptideFilter> peptideStarFilters) {
        this.peptideStarFilters = peptideStarFilters;
    }

    /**
     * Returns the psm hide filters
     * @return the psm hide filters 
     */
    public HashMap<String, PsmFilter> getPsmHideFilters() {
        return psmHideFilters;
    }

    /**
     * Sets  the psm hide filters
     * @param psmHideFilters  the psm hide filters
     */
    public void setPsmHideFilters(HashMap<String, PsmFilter> psmHideFilters) {
        this.psmHideFilters = psmHideFilters;
    }

    /**
     * Returns the psm star filters
     * @return the psm star filters
     */
    public HashMap<String, PsmFilter> getPsmStarFilters() {
        return psmStarFilters;
    }

    /**
     * Sets the psm star filters
     * @param psmStarFilters the psm star filters
     */
    public void setPsmStarFilters(HashMap<String, PsmFilter> psmStarFilters) {
        this.psmStarFilters = psmStarFilters;
    }
}
