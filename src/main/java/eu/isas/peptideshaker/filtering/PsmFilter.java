package eu.isas.peptideshaker.filtering;

import java.util.ArrayList;
import javax.swing.RowFilter.ComparisonType;

/**
 * PSM filter
 *
 * @author Marc Vaudel
 */
public class PsmFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility
     */
    static final long serialVersionUID = 2930349531911042645L;
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
     * List of spectrum files names retained
     */
    private ArrayList<String> fileName = null;

    /**
     * Constructor
     * @param name the name of the filter
     * @param charges list of allowed charges
     * @param files list of allowed files
     */
    public PsmFilter(String name, ArrayList<Integer> charges, ArrayList<String> files) {
        this.name = name;
        this.filterType = FilterType.PSM;
        this.charges = charges;
        this.fileName = files;
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
     * @param psmConfidenceComparison the comparison type used for the confidence
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
     * @param psmScoreComparison the comparison type used for the psm score
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
     * @param psmScore the threshold for the psm score
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
     * @param psmConfidence the threshold for the psm confidence
     */
    public void setPsmConfidence(Double psmConfidence) {
        this.psmConfidence = psmConfidence;
    }

    /**
     * Returns the comparison type used for the precursor m/z comparison
     * @return the comparison type used for the precursor m/z comparison
     */
    public ComparisonType getPrecursorMzComparison() {
        return precursorMzComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z comparison
     * @param precursorMzComparison the comparison type used for the precursor m/z comparison
     */
    public void setPrecursorMzComparison(ComparisonType precursorMzComparison) {
        this.precursorMzComparison = precursorMzComparison;
    }

    /**
     * Returns the comparison type used for the precursor m/z error comparison
     * @return  the comparison type used for the precursor m/z error comparison
     */
    public ComparisonType getPrecursorMzErrorComparison() {
        return precursorMzErrorComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z error comparison
     * @param precursorMzErrorComparison the comparison type used for the precursor m/z error comparison
     */
    public void setPrecursorMzErrorComparison(ComparisonType precursorMzErrorComparison) {
        this.precursorMzErrorComparison = precursorMzErrorComparison;
    }

    /**
     * Returns the comparison type used for the precursor RT comparison
     * @return  the comparison type used for the precursor RT comparison
     */
    public ComparisonType getPrecursorRTComparison() {
        return precursorRTComparison;
    }

    /**
     * Sets the comparison type used for the precursor RT comparison
     * @param precursorRTComparison the comparison type used for the precursor RT comparison
     */
    public void setPrecursorRTComparison(ComparisonType precursorRTComparison) {
        this.precursorRTComparison = precursorRTComparison;
    }
    
    /**
     * Returns the list of spectrum files containing the desired spectra
     * @return the list of spectrum files containing the desired spectra
     */
    public ArrayList<String> getFileNames() {
        return fileName;
    }
    /**
     * Sets the list of spectrum files containing the desired spectra
     * @param filesNames the list of spectrum files containing the desired spectra
     */
    public void setFileNames(ArrayList<String> filesNames) {
        this.fileName = filesNames;
    }
    
}
