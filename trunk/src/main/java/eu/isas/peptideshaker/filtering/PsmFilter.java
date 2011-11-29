/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.filtering;

import java.util.ArrayList;

/**
 * PSM filter
 *
 * @author marc
 */
public class PsmFilter extends MatchFilter {
    /**
     * The charges allowed
     */
    private ArrayList<Integer> charges;
    /**
     * the precursor m/z
     */
    private Double precursorMz = null;
    /**
     * The type of comparison to be used for the precursor m/z
     */
    private ComparisonType precursorMzComparison = ComparisonType.EQUAL;
    /**
     * The precursor retention time
     */
    private Double precursorRT = null;
    /**
     * The type of comparison to be used for the precursor retention time
     */
    private ComparisonType precursorRTComparison = ComparisonType.EQUAL;
    /**
     * The precursor m/z error
     */
    private Double precursorMzError = null;
    /**
     * The type of comparison to be used for the precursor m/z error
     */
    private ComparisonType precursorMzErrorComparison = ComparisonType.EQUAL;
    /**
     * Score limit
     */
    private Double psmScore = null;
    /**
     * The type of comparison to be used for the psm score
     */
    private ComparisonType psmScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit
     */
    private Double psmConfidence = null;
    /**
     * The type of comparison to be used for the psm confidence
     */
    private ComparisonType psmConfidenceComparison = ComparisonType.EQUAL;
    
    /**
     * Constructor
     * @param name the name of the filter
     * @param charges list of allowed charges
     */
    public PsmFilter(String name, ArrayList<Integer> charges) {
        this.name = name;
        this.filterType = FilterType.PEPTIDE;
        this.charges = charges;
    }

    /**
     * Returns the allowed charges
     * @return the allowed charges
     */
    public ArrayList<Integer> getCharges() {
        return charges;
    }

    /**
     * Sets the allowed charges
     * @param charges the allowed charges
     */
    public void setCharges(ArrayList<Integer> charges) {
        this.charges = charges;
    }

    /**
     * Returns the precursor m/z
     * @return the precursor m/z
     */
    public Double getPrecursorMz() {
        return precursorMz;
    }

    /**
     * Sets the precursor m/z
     * @param precursorMz the precursor m/z
     */
    public void setPrecursorMz(Double precursorMz) {
        this.precursorMz = precursorMz;
    }

    /**
     * Returns the precursor m/z error
     * @return the precursor m/z error
     */
    public Double getPrecursorMzError() {
        return precursorMzError;
    }

    /**
     * Sets the precursor m/z error
     * @param precursorMzError the precursor m/z error
     */
    public void setPrecursorMzError(Double precursorMzError) {
        this.precursorMzError = precursorMzError;
    }

    /**
     * Returns the precursor retention time
     * @return the precursor retention time
     */
    public Double getPrecursorRT() {
        return precursorRT;
    }

    /**
     * Sets the precursor retention time
     * @param precursorRT the precursor retention time
     */
    public void setPrecursorRT(Double precursorRT) {
        this.precursorRT = precursorRT;
    }

    /**
     * Returns the comparison type used for the confidence
     * @return the comparison type used for the confidence
     */
    public ComparisonType getPsmConfidenceComparison() {
        return psmConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence
     * @param proteinConfidenceComparison the comparison type used for the confidence
     */
    public void setPsmConfidenceComparison(ComparisonType psmConfidenceComparison) {
        this.psmConfidenceComparison = psmConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the psm score
     * @return the comparison type used for the psm score
     */
    public ComparisonType getPsmScoreComparison() {
        return psmScoreComparison;
    }

    /**
     * Sets the comparison type used for the psm score
     * @param proteinScoreComparison the comparison type used for the psm score
     */
    public void setPsmScoreComparison(ComparisonType psmScoreComparison) {
        this.psmScoreComparison = psmScoreComparison;
    }

    /**
     * Returns the threshold for the psm score
     * @return the threshold for the psm score
     */
    public Double getPsmScore() {
        return psmScore;
    }

    /**
     * Sets the threshold for the psm score
     * @param proteinScore the threshold for the psm score
     */
    public void setPsmScore(Double psmScore) {
        this.psmScore = psmScore;
    }

    /**
     * Returns the threshold for the psm confidence
     * @return the threshold for the psm confidence
     */
    public Double getPsmConfidence() {
        return psmConfidence;
    }

    /**
     * Sets the threshold for the psm confidence
     * @param proteinConfidence the threshold for the psm confidence
     */
    public void setPsmConfidence(Double psmConfidence) {
        this.psmConfidence = psmConfidence;
    }

    /**
     * Returns the comparison type used for the precursor m/z comparison
     * @param proteinScoreComparison the comparison type used for the precursor m/z comparison
     */
    public ComparisonType getPrecursorMzComparison() {
        return precursorMzComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z comparison
     * @param proteinScoreComparison the comparison type used for the precursor m/z comparison
     */
    public void setPrecursorMzComparison(ComparisonType precursorMzComparison) {
        this.precursorMzComparison = precursorMzComparison;
    }

    /**
     * Returns the comparison type used for the precursor m/z error comparison
     * @param proteinScoreComparison the comparison type used for the precursor m/z error comparison
     */
    public ComparisonType getPrecursorMzErrorComparison() {
        return precursorMzErrorComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z error comparison
     * @param proteinScoreComparison the comparison type used for the precursor m/z error comparison
     */
    public void setPrecursorMzErrorComparison(ComparisonType precursorMzErrorComparison) {
        this.precursorMzErrorComparison = precursorMzErrorComparison;
    }

    /**
     * Returns the comparison type used for the precursor RT comparison
     * @param proteinScoreComparison the comparison type used for the precursor RT comparison
     */
    public ComparisonType getPrecursorRTComparison() {
        return precursorRTComparison;
    }


    /**
     * Sets the comparison type used for the precursor RT comparison
     * @param proteinScoreComparison the comparison type used for the precursor RT comparison
     */
    public void setPrecursorRTComparison(ComparisonType precursorRTComparison) {
        this.precursorRTComparison = precursorRTComparison;
    }
    
    
}
