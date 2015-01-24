package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
     * Score limit.
     */
    private Double psmScore = null;
    /**
     * The type of comparison to be used for the PSM score.
     */
    private ComparisonType psmScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     */
    private Double psmConfidence = null;
    /**
     * The type of comparison to be used for the PSM confidence.
     */
    private ComparisonType psmConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The filter used to filter the best assumption.
     */
    private AssumptionFilter assumptionFilter;
    /**
     * The precursor m/z.
     *
     * @deprecated use the assumption filter instead
     */
    private Double precursorMz = null;
    /**
     * The type of comparison to be used for the precursor m/z.
     *
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMzComparison = ComparisonType.EQUAL;
    /**
     * The precursor retention time.
     *
     * @deprecated use the assumption filter instead
     */
    private Double precursorRT = null;
    /**
     * The type of comparison to be used for the precursor retention time.
     *
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorRTComparison = ComparisonType.EQUAL;
    /**
     * List of spectrum files names retained.
     *
     * @deprecated use the assumption filter instead
     */
    private ArrayList<String> fileName = null;
    /**
     * The charges allowed.
     *
     * @deprecated use the assumption filter instead
     */
    private ArrayList<Integer> charges = null;
    /**
     * The precursor m/z error.
     *
     * @deprecated use the assumption filter instead
     */
    private Double precursorMzError = null;
    /**
     * The type of comparison to be used for the precursor m/z error.
     *
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The minimal precursor m/z error.
     *
     * @deprecated use the assumption filter instead
     */
    private Double minPrecursorMzError = null;
    /**
     * The type of comparison to be used for the min precursor m/z error.
     *
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMinMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The maximal precursor m/z error.
     *
     * @deprecated use the assumption filter instead
     */
    private Double maxPrecursorMzError = null;
    /**
     * The type of comparison to be used for the max precursor m/z error.
     *
     * @deprecated use the assumption filter instead
     */
    private ComparisonType precursorMaxMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The amino acid coverage by fragment ions.
     *
     * @deprecated use the assumption filter instead
     */
    private Double sequenceCoverage = null;
    /**
     * The type of comparison to be used for the PSM confidence.
     *
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
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     * @param condition a description of the condition to be met to pass the
     * filter
     * @param reportPassed a report for when the filter is passed
     * @param reportFailed a report for when the filter is not passed
     */
    public PsmFilter(String name, String description, String condition, String reportPassed, String reportFailed) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.reportPassed = reportPassed;
        this.reportFailed = reportFailed;
        assumptionFilter = new AssumptionFilter(name, description, condition, reportPassed, reportFailed);
        this.filterType = FilterType.PSM;
    }

    /**
     * Verifies that the filter is compatible with the current version and makes
     * necessary updates if not.
     */
    private void compatibilityCheck() {
        if (assumptionFilter == null) {
            assumptionFilter = new AssumptionFilter(name, "", "", "", "");
        }
        if (charges != null && assumptionFilter.getCharges() == null) {
            assumptionFilter.setCharges(charges);
        }
        if (precursorMzError != null && assumptionFilter.getPrecursorMzError() == null) {
            assumptionFilter.setPrecursorMzError(precursorMzError);
            assumptionFilter.setPrecursorMzErrorComparison(precursorMzErrorComparison);
        }
        if (minPrecursorMzError != null && assumptionFilter.getMinPrecursorMzError() == null) {
            assumptionFilter.setMinPrecursorMzError(minPrecursorMzError);
            assumptionFilter.setPrecursorMinMzErrorComparison(precursorMinMzErrorComparison);
        }
        if (maxPrecursorMzError != null && assumptionFilter.getMaxPrecursorMzError() == null) {
            assumptionFilter.setMaxPrecursorMzError(maxPrecursorMzError);
            assumptionFilter.setPrecursorMaxMzErrorComparison(precursorMaxMzErrorComparison);
        }
        if (sequenceCoverage != null && assumptionFilter.getSequenceCoverage() == null) {
            assumptionFilter.setSequenceCoverage(sequenceCoverage);
            assumptionFilter.setSequenceCoverageComparison(sequenceCoverageComparison);
        }
        if (precursorMz != null && assumptionFilter.getPrecursorMz() == null) {
            assumptionFilter.setPrecursorMz(precursorMz);
            assumptionFilter.setPrecursorMzComparison(precursorMzComparison);
        }
        if (precursorRT != null && assumptionFilter.getPrecursorRT() == null) {
            assumptionFilter.setPrecursorRT(precursorRT);
            assumptionFilter.setPrecursorRTComparison(precursorRTComparison);
        }
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
     * Returns the filter used to filter the best assumption of this match.
     *
     * @return the filter used to filter the best assumption of this match
     */
    public AssumptionFilter getAssumptionFilter() {
        compatibilityCheck();
        return assumptionFilter;
    }

    /**
     * Sets the assumption filter.
     *
     * @param assumptionFilter the assumption filter
     */
    public void setAssumptionFilter(AssumptionFilter assumptionFilter) {
        this.assumptionFilter = assumptionFilter;
    }

    /**
     * Tests whether a spectrum match is validated by this filter. No
     * probabilistic m/z error filtering possible.
     *
     * @param spectrumKey the key of the spectrum match
     * @param identification the identification object to get the information
     * from
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public boolean isValidated(String spectrumKey, Identification identification, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, identification, shotgunProtocol, identificationParameters, null, null);
    }

    /**
     * Tests whether a spectrum match is validated by this filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param identification the identification object to get the information
     * from
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this one for the probabilistic m/z error filtering
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public boolean isValidated(String spectrumKey, Identification identification, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<Double> precursorMzDeviations)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, identification, shotgunProtocol, identificationParameters, null, precursorMzDeviations);
    }

    /**
     * Tests whether a spectrum match is validated by this filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param identification the identification object to get the information
     * from
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this one for the probabilistic m/z error filtering
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public boolean isValidated(String spectrumKey, Identification identification, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator, ArrayList<Double> precursorMzDeviations)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, this, identification, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, precursorMzDeviations);
    }

    /**
     * Tests whether a spectrum match is validated by a given filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param psmFilter the filter
     * @param identification the identification object to get the information
     * from
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this one for the probabilistic m/z error filtering
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public static boolean isValidated(String spectrumKey, PsmFilter psmFilter, Identification identification, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator, ArrayList<Double> precursorMzDeviations)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (psmFilter.getExceptions().contains(spectrumKey)) {
            return false;
        }
        if (psmFilter.getManualValidation().size() > 0) {
            return psmFilter.getManualValidation().contains(spectrumKey);
        }

        PSParameter psParameter = new PSParameter();

        if (psmFilter.getPsmScore() != null
                || psmFilter.getPsmConfidence() != null
                || psmFilter.getValidationLevel() != null) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

            if (psmFilter.getValidationComparison() == ComparisonType.AFTER) {
                if (psParameter.getMatchValidationLevel().getIndex() <= psmFilter.getValidationLevel()) {
                    return false;
                }
            } else if (psmFilter.getValidationComparison() == ComparisonType.BEFORE) {
                if (psParameter.getMatchValidationLevel().getIndex() > psmFilter.getValidationLevel()) {
                    return false;
                }
            } else if (psmFilter.getValidationComparison() == ComparisonType.EQUAL) {
                if (psParameter.getMatchValidationLevel().getIndex() != psmFilter.getValidationLevel()) {
                    return false;
                }
            } else if (psmFilter.getValidationComparison() == ComparisonType.NOT_EQUAL) {
                if (psParameter.getMatchValidationLevel().getIndex() == psmFilter.getValidationLevel()) {
                    return false;
                }
            }

            if (psmFilter.getPsmScore() != null) {
                if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.AFTER) {
                    if (psParameter.getPsmScore() <= psmFilter.getPsmScore()) {
                        return false;
                    }
                } else if (psmFilter.getPsmScoreComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (psParameter.getPsmScore() > psmFilter.getPsmScore()) {
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
                    if (psParameter.getPsmConfidence() > psmFilter.getPsmConfidence()) {
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

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        if (spectrumMatch.getBestPeptideAssumption() != null) {
            return psmFilter.getAssumptionFilter().isValidated(spectrumKey, spectrumMatch.getBestPeptideAssumption(), shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, precursorMzDeviations);
        } else if (spectrumMatch.getBestTagAssumption() != null) {
            //TODO: implement a tag assumption filter
            return true;
        } else {
            return true;
        }
    }

    @Override
    public boolean isValidated(String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        return isValidated(matchKey, identification, shotgunProtocol, identificationParameters, null);
    }

    @Override
    public MatchFilter clone() {
        PsmFilter psmFilter = new PsmFilter(name, description, condition, reportPassed, reportFailed);
        psmFilter.setActive(isActive());
        psmFilter.setPsmScore(getPsmScore());
        psmFilter.setPsmScoreComparison(getPsmScoreComparison());
        psmFilter.setPsmConfidence(getPsmConfidence());
        psmFilter.setPsmConfidenceComparison(getPsmConfidenceComparison());
        psmFilter.setAssumptionFilter((AssumptionFilter) assumptionFilter.clone());
        return psmFilter;
    }

    /**
     * Indicates whether another filter is the same as the current filter.
     *
     * @param anotherFilter another filter
     *
     * @return a boolean indicating whether another filter is the same as the
     * current filter
     */
    public boolean isSameAs(PsmFilter anotherFilter) {
        if (!name.equals(anotherFilter.getName())) {
            return false;
        }
        if (!description.equals(anotherFilter.getDescription())) {
            return false;
        }
        if (!condition.equals(anotherFilter.getCondition())) {
            return false;
        }
        if (!reportPassed.equals(anotherFilter.getReport(true))) {
            return false;
        }
        if (!reportFailed.equals(anotherFilter.getReport(false))) {
            return false;
        }
        if (isActive() != anotherFilter.isActive()) {
            return false;
        }
        if (getPsmScore() == null && anotherFilter.getPsmScore() != null
                || getPsmScore() != null && !getPsmScore().equals(anotherFilter.getPsmScore())) {
            return false;
        }
        if (getPsmScoreComparison() != anotherFilter.getPsmScoreComparison()) {
            return false;
        }
        if (getPsmConfidence() == null && anotherFilter.getPsmConfidence() != null
                || getPsmConfidence() != null && !getPsmConfidence().equals(anotherFilter.getPsmConfidence())) {
            return false;
        }
        if (getPsmConfidenceComparison() != anotherFilter.getPsmConfidenceComparison()) {
            return false;
        }
        return assumptionFilter.isSameAs(anotherFilter.getAssumptionFilter());
    }

    @Override
    public boolean isSameAs(Filter anotherFilter) {
        if (anotherFilter instanceof PsmFilter) {
            return isSameAs((PsmFilter) anotherFilter);
        }
        return false;
    }
}
