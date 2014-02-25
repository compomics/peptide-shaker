package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * PSM filter.
 *
 * @author Marc Vaudel
 */
public class PsmFilter extends MatchFilter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 2930349531911042645L;
    /**
     * the precursor m/z.
     */
    private Double precursorMz = null;
    /**
     * The type of comparison to be used for the precursor m/z.
     */
    private ComparisonType precursorMzComparison = ComparisonType.EQUAL;
    /**
     * The precursor retention time.
     */
    private Double precursorRT = null;
    /**
     * The type of comparison to be used for the precursor retention time.
     */
    private ComparisonType precursorRTComparison = ComparisonType.EQUAL;
    /**
     * Score limit.
     */
    private Double psmScore = null;
    /**
     * The type of comparison to be used for the psm score.
     */
    private ComparisonType psmScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     */
    private Double psmConfidence = null;
    /**
     * The type of comparison to be used for the psm confidence.
     */
    private ComparisonType psmConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The filter used to filter the best assumption
     */
    private AssumptionFilter assumptionFilter;
    /**
     * List of spectrum files names retained.
     */
    private ArrayList<String> fileName = null;
    /**
     * The charges allowed.
     * @deprecated use the assumption filter instead
     */
    private ArrayList<Integer> charges = null;
    /**
     * The precursor m/z error.
     * @deprecated use the assumption filter instead
     */
    private Double precursorMzError = null;
    /**
     * The type of comparison to be used for the precursor m/z error.
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The minimal precursor m/z error.
     * @deprecated use the assumption filter instead
     */
    private Double minPrecursorMzError = null;
    /**
     * The type of comparison to be used for the min precursor m/z error.
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMinMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The maximal precursor m/z error.
     * @deprecated use the assumption filter instead
     */
    private Double maxPrecursorMzError = null;
    /**
     * The type of comparison to be used for the max precursor m/z error.
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMaxMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The amino acid coverage by fragment ions.
     * @deprecated use the assumption filter instead
     */
    private Double sequenceCoverage = null;
    /**
     * The type of comparison to be used for the psm confidence.
     * @deprecated use the assumption filter instead
     */
    private ComparisonType sequenceCoverageComparison = ComparisonType.EQUAL;

    /**
     * Constructor.
     *
     * @param name the name of the filter
     */
    public PsmFilter(String name) {
        this.name = name;
        assumptionFilter = new AssumptionFilter(name);
        this.filterType = FilterType.PSM;
    }
    
    /**
     * Verifies that the filter is compatible with the current version and makes necessary updates if not
     */
    private void compatibilityCheck() {
        if (assumptionFilter == null) {
            assumptionFilter = new AssumptionFilter(name);
            if (charges != null) {
                assumptionFilter.setCharges(charges);
            }
            if (precursorMzError != null) {
                assumptionFilter.setPrecursorMzError(precursorMzError);
                assumptionFilter.setPrecursorMzErrorComparison(precursorMzErrorComparison);
            }
            if (minPrecursorMzError != null) {
                assumptionFilter.setMinPrecursorMzError(minPrecursorMzError);
                assumptionFilter.setPrecursorMinMzErrorComparison(precursorMinMzErrorComparison);
            }
            if (maxPrecursorMzError != null) {
                assumptionFilter.setMaxPrecursorMzError(maxPrecursorMzError);
                assumptionFilter.setPrecursorMaxMzErrorComparison(precursorMaxMzErrorComparison);
            }
            if (sequenceCoverage != null) {
                assumptionFilter.setSequenceCoverage(sequenceCoverage);
                assumptionFilter.setSequenceCoverageComparison(sequenceCoverageComparison);
            }
        }
    }

    /**
     * Returns the precursor m/z.
     *
     * @return the precursor m/z
     */
    public Double getPrecursorMz() {
        return precursorMz;
    }

    /**
     * Sets the precursor m/z.
     *
     * @param precursorMz the precursor m/z
     */
    public void setPrecursorMz(Double precursorMz) {
        this.precursorMz = precursorMz;
    }

    /**
     * Returns the comparison type used for the precursor m/z error comparison.
     *
     * @return the comparison type used for the precursor m/z error comparison
     */
    public ComparisonType getPrecursorMzErrorComparison() {
        compatibilityCheck();
        return assumptionFilter.getPrecursorMzErrorComparison();
    }

    /**
     * Sets the comparison type used for the precursor m/z error comparison.
     *
     * @param precursorMzErrorComparison the comparison type used for the
     * precursor m/z error comparison
     */
    public void setPrecursorMzErrorComparison(ComparisonType precursorMzErrorComparison) {
        compatibilityCheck();
        assumptionFilter.setPrecursorMzErrorComparison(precursorMzErrorComparison);
    }

    /**
     * Returns the precursor retention time.
     *
     * @return the precursor retention time
     */
    public Double getPrecursorRT() {
        return precursorRT;
    }

    /**
     * Sets the precursor retention time.
     *
     * @param precursorRT the precursor retention time
     */
    public void setPrecursorRT(Double precursorRT) {
        this.precursorRT = precursorRT;
    }

    /**
     * Returns the comparison type used for the confidence.
     *
     * @return the comparison type used for the confidence
     */
    public ComparisonType getPsmConfidenceComparison() {
        return psmConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence.
     *
     * @param psmConfidenceComparison the comparison type used for the
     * confidence
     */
    public void setPsmConfidenceComparison(ComparisonType psmConfidenceComparison) {
        this.psmConfidenceComparison = psmConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the psm score.
     *
     * @return the comparison type used for the psm score
     */
    public ComparisonType getPsmScoreComparison() {
        return psmScoreComparison;
    }

    /**
     * Sets the comparison type used for the psm score.
     *
     * @param psmScoreComparison the comparison type used for the psm score
     */
    public void setPsmScoreComparison(ComparisonType psmScoreComparison) {
        this.psmScoreComparison = psmScoreComparison;
    }

    /**
     * Returns the threshold for the psm score.
     *
     * @return the threshold for the psm score
     */
    public Double getPsmScore() {
        return psmScore;
    }

    /**
     * Sets the threshold for the psm score.
     *
     * @param psmScore the threshold for the psm score
     */
    public void setPsmScore(Double psmScore) {
        this.psmScore = psmScore;
    }

    /**
     * Returns the threshold for the psm confidence.
     *
     * @return the threshold for the psm confidence
     */
    public Double getPsmConfidence() {
        return psmConfidence;
    }

    /**
     * Sets the threshold for the psm confidence.
     *
     * @param psmConfidence the threshold for the psm confidence
     */
    public void setPsmConfidence(Double psmConfidence) {
        this.psmConfidence = psmConfidence;
    }

    /**
     * Returns the comparison type used for the precursor m/z comparison.
     *
     * @return the comparison type used for the precursor m/z comparison
     */
    public ComparisonType getPrecursorMzComparison() {
        return precursorMzComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z comparison.
     *
     * @param precursorMzComparison the comparison type used for the precursor
     * m/z comparison
     */
    public void setPrecursorMzComparison(ComparisonType precursorMzComparison) {
        this.precursorMzComparison = precursorMzComparison;
    }

    /**
     * Returns the filter used to filter the best assumption of this match.
     * 
     * @return the filter used to filter the best assumption of this match
     */
    public AssumptionFilter getAssumptionFilter() {
        compatibilityCheck();
        return assumptionFilter;
    }

    /**
     * Returns the comparison type used for the precursor RT comparison.
     *
     * @return the comparison type used for the precursor RT comparison
     */
    public ComparisonType getPrecursorRTComparison() {
        return precursorRTComparison;
    }

    /**
     * Sets the comparison type used for the precursor RT comparison.
     *
     * @param precursorRTComparison the comparison type used for the precursor
     * RT comparison
     */
    public void setPrecursorRTComparison(ComparisonType precursorRTComparison) {
        this.precursorRTComparison = precursorRTComparison;
    }

    /**
     * Returns the list of spectrum files containing the desired spectra.
     *
     * @return the list of spectrum files containing the desired spectra
     */
    public ArrayList<String> getFileNames() {
        return fileName;
    }

    /**
     * Sets the list of spectrum files containing the desired spectra.
     *
     * @param filesNames the list of spectrum files containing the desired
     * spectra
     */
    public void setFileNames(ArrayList<String> filesNames) {
        this.fileName = filesNames;
    }

    /**
     * Sets the allowed charges for the best assumption.
     *
     * @param charges the allowed charges
     */
    public void setCharges(ArrayList<Integer> charges) {
        compatibilityCheck();
        assumptionFilter.setCharges(charges);
    }

    /**
     * Sets the precursor m/z error for the best assumption.
     *
     * @param precursorMzError the precursor m/z error
     */
    public void setPrecursorMzError(Double precursorMzError) {
        compatibilityCheck();
        assumptionFilter.setPrecursorMzError(precursorMzError);
    }

    /**
     * Sets the comparison type used for the search engine confidence of the best assumption.
     *
     * @param searchEngineConfidenceComparison the comparison type used for the
     * confidence
     */
    public void setSearchEngineConfidenceComparison(RowFilter.ComparisonType searchEngineConfidenceComparison) {
        compatibilityCheck();
        assumptionFilter.setSearchEngineConfidenceComparison(searchEngineConfidenceComparison);
    }

    /**
     * Sets the comparison type used for the search engine score if the best assumption.
     *
     * @param searchEngineScoreComparison the comparison type used for the search engine score
     */
    public void setSearchEngineScoreComparison(RowFilter.ComparisonType searchEngineScoreComparison) {
        compatibilityCheck();
        assumptionFilter.setSearchEngineScoreComparison(searchEngineScoreComparison);
    }

    /**
     * Sets the threshold for the search engine score of the best assumption.
     *
     * @param searchEngineScore the threshold for the psm score
     */
    public void setSearchEngineScore(Double searchEngineScore) {
        compatibilityCheck();
        assumptionFilter.setSearchEngineScore(searchEngineScore);
    }

    /**
     * Sets the threshold for the search engine confidence of the best assumption.
     *
     * @param searchEngineConfidence the threshold for the search engine confidence
     */
    public void setSearchEngineConfidence(Double searchEngineConfidence) {
        compatibilityCheck();
        assumptionFilter.setSearchEngineConfidence(searchEngineConfidence);
    }

    /**
     * Sets the minimal precursor m/z error of the best assumption.
     *
     * @param minPrecursorMzError the minimal precursor m/z error
     */
    public void setMinPrecursorMzError(Double minPrecursorMzError) {
        compatibilityCheck();
        assumptionFilter.setMinPrecursorMzError(minPrecursorMzError);
    }

    /**
     * Sets the comparison type used for the precursor min m/z error comparison of the best assumption.
     *
     * @param precursorMinMzErrorComparison the comparison type used for the
     * precursor min m/z error comparison
     */
    public void setPrecursorMinMzErrorComparison(RowFilter.ComparisonType precursorMinMzErrorComparison) {
        compatibilityCheck();
        assumptionFilter.setMinPrecursorMzError(minPrecursorMzError);
    }

    /**
     * Sets the maximal precursor m/z error of the best assumption.
     *
     * @param maxPrecursorMzError the maximal precursor m/z error
     */
    public void setMaxPrecursorMzError(Double maxPrecursorMzError) {
        compatibilityCheck();
        assumptionFilter.setMaxPrecursorMzError(maxPrecursorMzError);
    }

    /**
     * Sets the comparison type used for the precursor max m/z error comparison of the best assumption.
     *
     * @param precursorMaxMzErrorComparison the comparison type used for the
     * precursor max m/z error comparison
     */
    public void setPrecursorMaxMzErrorComparison(RowFilter.ComparisonType precursorMaxMzErrorComparison) {
        compatibilityCheck();
        assumptionFilter.setPrecursorMaxMzErrorComparison(precursorMaxMzErrorComparison);
    }

    /**
     * Sets the sequence coverage by fragment ions threshold in percent of the best assumption.
     *
     * @param sequenceCoverage the sequence coverage by fragment ions threshold
     * in percent
     */
    public void setSequenceCoverage(Double sequenceCoverage) {
        compatibilityCheck();
        assumptionFilter.setSequenceCoverage(sequenceCoverage);
    }

    /**
     * Sets the comparator for the sequence coverage by fragment ions of the best assumption.
     *
     * @param sequenceCoverageComparison the comparator for the sequence
     * coverage by fragment ions
     */
    public void setSequenceCoverageComparison(RowFilter.ComparisonType sequenceCoverageComparison) {
        compatibilityCheck();
        assumptionFilter.setSequenceCoverageComparison(sequenceCoverageComparison);
    }

    /**
     * Sets the spectrum annotator of this filter
     *
     * @param spectrumAnnotator the spectrum annotator of this filter
     */
    public void setSpectrumAnnotator(PeptideSpectrumAnnotator spectrumAnnotator) {
        compatibilityCheck();
        assumptionFilter.setSpectrumAnnotator(spectrumAnnotator);
    }

    /**
     * Tests whether a spectrum match is validated by this filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param identification the identification object to get the information
     * from
     * @param searchParameters the identification parameters
     * @param annotationPreferences the spectrum annotation preferences
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public boolean isValidated(String spectrumKey, Identification identification, SearchParameters searchParameters, AnnotationPreferences annotationPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, this, identification, searchParameters, annotationPreferences);
    }

    /**
     * Tests whether a spectrum match is validated by a given filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param psmFilter the filter
     * @param identification the identification object to get the information
     * from
     * @param searchParameters the identification parameters
     * @param annotationPreferences the spectrum annotation preferences
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public static boolean isValidated(String spectrumKey, PsmFilter psmFilter, Identification identification, SearchParameters searchParameters, AnnotationPreferences annotationPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (psmFilter.getExceptions().contains(spectrumKey)) {
            return false;
        }
        if (psmFilter.getManualValidation().size() > 0) {
            return psmFilter.getManualValidation().contains(spectrumKey);
        }

        PSParameter psParameter = new PSParameter();

        if (psmFilter.getPsmScore() != null
                || psmFilter.getPsmConfidence() != null) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

            if (psmFilter.getPsmScore() != null) {
                if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.AFTER) {
                    if (psParameter.getPsmScore() <= psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (psParameter.getPsmScore() >= psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (psParameter.getPsmScore() != psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPsmScore() == psmFilter.getPsmScore()) {
                        return false;
                    }
                }
            }

            if (psmFilter.getPsmConfidence() != null) {
                if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.AFTER) {
                    if (psParameter.getPsmConfidence() <= psmFilter.getPsmConfidence()) {
                        return false;
                    }
                } else if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (psParameter.getPsmConfidence() >= psmFilter.getPsmConfidence()) {
                        return false;
                    }
                } else if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (psParameter.getPsmConfidence() != psmFilter.getPsmConfidence()) {
                        return false;
                    }
                } else if (psmFilter.getPsmConfidenceComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPsmConfidence() == psmFilter.getPsmConfidence()) {
                        return false;
                    }
                }
            }
        }

        if (psmFilter.getPrecursorMz() != null
                || psmFilter.getPrecursorRT() != null) {

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

            if (psmFilter.getPrecursorMz() != null) {
                if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.AFTER) {
                    if (precursor.getMz() <= psmFilter.getPrecursorMz()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (precursor.getMz() >= psmFilter.getPrecursorMz()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (precursor.getMz() != psmFilter.getPrecursorMz()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorMzComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (precursor.getMz() == psmFilter.getPrecursorMz()) {
                        return false;
                    }
                }
            }

            if (psmFilter.getPrecursorRT() != null) {
                if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.AFTER) {
                    if (precursor.getRt() <= psmFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (precursor.getRt() >= psmFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (precursor.getRt() != psmFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (psmFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (precursor.getRt() == psmFilter.getPrecursorRT()) {
                        return false;
                    }
                }
            }
        }

        if (psmFilter.getFileNames() != null && !psmFilter.getFileNames().contains(Spectrum.getSpectrumFile(spectrumKey))) {
            return false;
        }
        
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        return psmFilter.getAssumptionFilter().isValidated(spectrumKey, spectrumMatch.getBestPeptideAssumption(), searchParameters, annotationPreferences);
    }

    @Override
    public boolean isValidated(String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        return isValidated(matchKey, identification, searchParameters, annotationPreferences);
    }
}
