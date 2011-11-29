/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.IdentificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import java.util.ArrayList;

/**
 * Peptide Filter
 *
 * @author marc
 */
public class PeptideFilter extends MatchFilter {
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
    private Double peptideScore = null;
    /**
     * The type of comparison to be used for the peptide score
     */
    private ComparisonType peptideScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit
     */
    private Double peptideConfidence = null;
    /**
     * The type of comparison to be used for the peptide confidence
     */
    private ComparisonType peptideConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The current protein inference filter selection.
     */
    private int pi = 5;
    /**
     * The list of modifications allowed for the peptide
     */
    private ArrayList<String> modificationStatus;
    
    /**
     * Constructor
     * @param name the name of the filter
     * @param allModifications list of all modifications found in peptides
     */
    public PeptideFilter(String name, ArrayList<String> allModifications) {
        this.name = name;
        this.modificationStatus = allModifications;
        this.filterType = FilterType.PEPTIDE;
    }

    /**
     * Returns the threshold for the peptide confidence
     * @return the threshold for the peptide confidence
     */
    public Double getPeptideConfidence() {
        return peptideConfidence;
    }

    /**
     * Sets the threshold for the peptide confidence
     * @param proteinConfidence the threshold for the peptide confidence
     */
    public void setPeptideConfidence(Double peptideConfidence) {
        this.peptideConfidence = peptideConfidence;
    }

    /**
     * Returns the threshold for the number of spectra
     * @return the threshold for the number of spectra
     */
    public Integer getNSpectra() {
        return nSpectra;
    }

    /**
     * Sets the threshold for the number of spectra
     * @param nSpectra the threshold for the number of spectra
     */
    public void setNSpectra(Integer nSpectra) {
        this.nSpectra = nSpectra;
    }

    /**
     * Returns the threshold for the peptide score
     * @return the threshold for the peptide score
     */
    public Double getPeptideScore() {
        return peptideScore;
    }

    /**
     * Sets the threshold for the peptide score
     * @param proteinScore the threshold for the peptide score
     */
    public void setPeptideScore(Double peptideScore) {
        this.peptideScore = peptideScore;
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
    public ComparisonType getPeptideConfidenceComparison() {
        return peptideConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence
     * @param proteinConfidenceComparison the comparison type used for the confidence
     */
    public void setPeptideConfidenceComparison(ComparisonType peptideConfidenceComparison) {
        this.peptideConfidenceComparison = peptideConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the peptide score
     * @return the comparison type used for the peptide score
     */
    public ComparisonType getPeptideScoreComparison() {
        return peptideScoreComparison;
    }

    /**
     * Sets the comparison type used for the peptide score
     * @param proteinScoreComparison the comparison type used for the peptide score
     */
    public void setPeptideScoreComparison(ComparisonType peptideScoreComparison) {
        this.peptideScoreComparison = peptideScoreComparison;
    }

    /**
     * Returns the modifications to retain
     * @return the modifications to retain
     */
    public ArrayList<String> getModificationStatus() {
        return modificationStatus;
    }

    /**
     * Sets the modifications to retain
     * @param modificationStatus the modifications to retain
     */
    public void setModificationStatus(ArrayList<String> modificationStatus) {
        this.modificationStatus = modificationStatus;
    }

    
}
