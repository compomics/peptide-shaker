package eu.isas.peptideshaker.filtering;

import javax.swing.RowFilter.ComparisonType;

/**
 * Protein Filter
 *
 * @author Marc Vaudel
 */
public class ProteinFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility
     */
    static final long serialVersionUID = 5753850468907866679L;
    /**
     * regex in the protein accession
     */
    private String accessionRegex = null;
    /**
     * protein coverage limit
     */
    private Double proteinCoverage = null;
    /**
     * The type of comparison to be used for the protein coverage
     */
    private ComparisonType proteinCoverageComparison = ComparisonType.EQUAL;
    /**
     * spectrum counting limit
     */
    private Double spectrumCounting = null;
    /**
     * The type of comparison to be used for the spectrum counting
     */
    private ComparisonType spectrumCountingComparison = ComparisonType.EQUAL;
    /**
     * Number of peptides limit
     */
    private Integer nPeptides = null;
    /**
     * The type of comparison to be used for the number of peptides
     */
    private ComparisonType nPeptidesComparison = ComparisonType.EQUAL;
    /**
     * Number of spectra limit
     */
    private Integer nSpectra = null;
    /**
     * The type of comparison to be used for the number of spectra
     */
    private ComparisonType nSpectraComparison = ComparisonType.EQUAL;
    /**
     * Score limit
     */
    private Double proteinScore = null;
    /**
     * The type of comparison to be used for the protein score
     */
    private ComparisonType proteinScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit
     */
    private Double proteinConfidence = null;
    /**
     * The type of comparison to be used for the protein confidence
     */
    private ComparisonType proteinConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The current protein inference filter selection.
     */
    private int pi = 5;
    /**
     * The type of comparison to be used for the PI
     */
    private ComparisonType piComparison = ComparisonType.EQUAL;

    /**
     * Constructor.
     * 
     * @param name the name of the filter
     */
    public ProteinFilter(String name) {
        this.name = name;
        this.filterType = FilterType.PROTEIN;
    }

    /**
     * Returns the threshold for the number of peptides
     * @return the threshold for the number of peptides 
     */
    public Integer getnPeptides() {
        return nPeptides;
    }

    /**
     * Returns  the threshold for the number of peptides
     * @param nPeptides  the threshold for the number of peptides
     */
    public void setnPeptides(int nPeptides) {
        this.nPeptides = nPeptides;
    }

    /**
     * Returns the threshold for the protein confidence
     * @return the threshold for the protein confidence
     */
    public Double getProteinConfidence() {
        return proteinConfidence;
    }

    /**
     * Sets the threshold for the protein confidence
     * @param proteinConfidence the threshold for the protein confidence
     */
    public void setProteinConfidence(Double proteinConfidence) {
        this.proteinConfidence = proteinConfidence;
    }

    /**
     * Returns the threshold for the protein coverage
     * @return the threshold for the protein coverage
     */
    public Double getProteinCoverage() {
        return proteinCoverage;
    }

    /**
     * sets the threshold for the protein coverage
     * @param proteinCoverage the threshold for the protein coverage
     */
    public void setProteinCoverage(Double proteinCoverage) {
        this.proteinCoverage = proteinCoverage;
    }

    /**
     * Returns the threshold for the number of spectra
     * @return the threshold for the number of spectra
     */
    public Integer getProteinNSpectra() {
        return nSpectra;
    }

    /**
     * Sets the threshold for the number of spectra
     * @param nSpectra the threshold for the number of spectra
     */
    public void setProteinNSpectra(Integer nSpectra) {
        this.nSpectra = nSpectra;
    }

    /**
     * Returns the threshold for the protein score
     * @return the threshold for the protein score
     */
    public Double getProteinScore() {
        return proteinScore;
    }

    /**
     * Sets the threshold for the protein score
     * @param proteinScore the threshold for the protein score
     */
    public void setProteinScore(Double proteinScore) {
        this.proteinScore = proteinScore;
    }

    /**
     * Returns the threshold for the spectrum counting
     * @return the threshold for the spectrum counting
     */
    public Double getSpectrumCounting() {
        return spectrumCounting;
    }

    /**
     * Sets the threshold for the spectrum counting
     * @param spectrumCounting the threshold for the spectrum counting
     */
    public void setSpectrumCounting(Double spectrumCounting) {
        this.spectrumCounting = spectrumCounting;
    }

    /**
     * Returns the comparison type used for the number of peptides
     * @return the comparison type used for the number of peptides
     */
    public ComparisonType getnPeptidesComparison() {
        return nPeptidesComparison;
    }

    /**
     * Sets the comparison type used for the number of peptides
     * @param nPeptidesComparison the comparison type used for the number of peptides
     */
    public void setnPeptidesComparison(ComparisonType nPeptidesComparison) {
        this.nPeptidesComparison = nPeptidesComparison;
    }

    /**
     * Returns the comparison type used for the number of spectra
     * @return the comparison type used for the number of spectra
     */
    public ComparisonType getnSpectraComparison() {
        return nSpectraComparison;
    }

    /**
     * Sets the comparison type used for the number of spectra
     * @param nSpectraComparison the comparison type used for the number of spectra
     */
    public void setnSpectraComparison(ComparisonType nSpectraComparison) {
        this.nSpectraComparison = nSpectraComparison;
    }

    /**
     * Returns the protein inference desired
     * @return the protein inference desired
     */
    public int getPi() {
        return pi;
    }

    /**
     * Sets the protein inference desired
     * @param pi the protein inference desired
     */
    public void setPi(int pi) {
        this.pi = pi;
    }

    /**
     * Returns the comparison type used for the confidence
     * @return the comparison type used for the confidence
     */
    public ComparisonType getProteinConfidenceComparison() {
        return proteinConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence
     * @param proteinConfidenceComparison the comparison type used for the confidence
     */
    public void setProteinConfidenceComparison(ComparisonType proteinConfidenceComparison) {
        this.proteinConfidenceComparison = proteinConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the protein coverage
     * @return the comparison type used for the protein coverage
     */
    public ComparisonType getProteinCoverageComparison() {
        return proteinCoverageComparison;
    }

    /**
     * Sets the comparison type used for the protein coverage
     * @param proteinCoverageComparison the comparison type used for the protein coverage
     */
    public void setProteinCoverageComparison(ComparisonType proteinCoverageComparison) {
        this.proteinCoverageComparison = proteinCoverageComparison;
    }

    /**
     * Returns the comparison type used for the protein score
     * @return the comparison type used for the protein score
     */
    public ComparisonType getProteinScoreComparison() {
        return proteinScoreComparison;
    }

    /**
     * Sets the comparison type used for the protein score
     * @param proteinScoreComparison the comparison type used for the protein score
     */
    public void setProteinScoreComparison(ComparisonType proteinScoreComparison) {
        this.proteinScoreComparison = proteinScoreComparison;
    }

    /**
     * Returns the comparison type used for the spectrum counting
     * @return the comparison type used for the spectrum counting
     */
    public ComparisonType getSpectrumCountingComparison() {
        return spectrumCountingComparison;
    }

    /**
     * Sets the comparison type used for the spectrum counting
     * @param spectrumCountingComparison the comparison type used for the spectrum counting
     */
    public void setSpectrumCountingComparison(ComparisonType spectrumCountingComparison) {
        this.spectrumCountingComparison = spectrumCountingComparison;
    }

    /**
     * Returns the regex contained in the accession
     * @return  the regex contained in the accession
     */
    public String getIdentifierRegex() {
        return accessionRegex;
    }

    /**
     * Sets the regex contained in the accession
     * @param accessionRegex the regex contained in the accession
     */
    public void setIdentifierRegex(String accessionRegex) {
        this.accessionRegex = accessionRegex;
    }

    /**
     * Returns the comparison type to use for the PI
     * @return the comparison type to use for the PI
     */
    public ComparisonType getPiComparison() {
        return piComparison;
    }
/**
     * Sets the comparison type to use for the PI
     * @param piComparison the comparison type to use for the PI
     */
    public void setPiComparison(ComparisonType piComparison) {
        this.piComparison = piComparison;
    }
}
