package eu.isas.peptideshaker.filtering;

import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SpecificAnnotationPreferences;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.RowFilter;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Assumption filter.
 *
 * @author Marc Vaudel
 */
public class AssumptionFilter extends MatchFilter {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = 5082744251034128558L;
    /**
     * The charges allowed.
     */
    private ArrayList<Integer> charges = null;
    /**
     * The precursor m/z error.
     */
    private Double precursorMzError = null;
    /**
     * The type of comparison to be used for the precursor m/z error.
     */
    private RowFilter.ComparisonType precursorMzErrorComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * The type of precursor m/z error to use.
     */
    private IonMatch.MzErrorType precursorMzErrorType = IonMatch.MzErrorType.RelativePpm;
    /**
     * The minimal precursor m/z error.
     */
    private Double minPrecursorMzError = null;
    /**
     * The type of comparison to be used for the min precursor m/z error.
     */
    private RowFilter.ComparisonType precursorMinMzErrorComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * The maximal precursor m/z error.
     */
    private Double maxPrecursorMzError = null;
    /**
     * The type of comparison to be used for the max precursor m/z error.
     */
    private RowFilter.ComparisonType precursorMaxMzErrorComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * The amino acid coverage by fragment ions.
     */
    private Double sequenceCoverage = null;
    /**
     * The type of comparison to be used for the PSM confidence.
     */
    private RowFilter.ComparisonType sequenceCoverageComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * Score limit.
     */
    private Double searchEngineScore = null;
    /**
     * The type of comparison to be used for the score.
     */
    private RowFilter.ComparisonType searchEngineScoreComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * Confidence limit.
     */
    private Double searchEngineConfidence = null;
    /**
     * The type of comparison to be used for the confidence.
     */
    private RowFilter.ComparisonType searchEngineConfidenceComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * List of spectrum files names retained.
     */
    private ArrayList<String> fileNames = null;
    /**
     * The precursor m/z.
     */
    private Double precursorMz = null;
    /**
     * The type of comparison to be used for the precursor m/z.
     */
    private RowFilter.ComparisonType precursorMzComparison = RowFilter.ComparisonType.EQUAL;
    /**
     * The precursor retention time.
     */
    private Double precursorRT = null;
    /**
     * The type of comparison to be used for the precursor retention time.
     */
    private RowFilter.ComparisonType precursorRTComparison = RowFilter.ComparisonType.EQUAL;

    /**
     * Constructor.
     *
     * @param name the name of the filter
     */
    public AssumptionFilter(String name) {
        this.name = name;
        this.filterType = MatchFilter.FilterType.ASSUMPTION;
    }

    /**
     * Constructor.
     *
     * @param name the name of the filter
     * @param description the description of the filter
     */
    public AssumptionFilter(String name, String description) {
        this.name = name;
        this.description = description;
        this.filterType = FilterType.ASSUMPTION;
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
    public AssumptionFilter(String name, String description, String condition, String reportPassed, String reportFailed) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.reportPassed = reportPassed;
        this.reportFailed = reportFailed;
        this.filterType = MatchFilter.FilterType.ASSUMPTION;
    }

    /**
     * Returns the allowed charges.
     *
     * @return the allowed charges
     */
    public ArrayList<Integer> getCharges() {
        return charges;
    }

    /**
     * Sets the allowed charges.
     *
     * @param charges the allowed charges
     */
    public void setCharges(ArrayList<Integer> charges) {
        this.charges = charges;
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
     * Returns the comparison type used for the precursor m/z comparison.
     *
     * @return the comparison type used for the precursor m/z comparison
     */
    public RowFilter.ComparisonType getPrecursorMzComparison() {
        return precursorMzComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z comparison.
     *
     * @param precursorMzComparison the comparison type used for the precursor
     * m/z comparison
     */
    public void setPrecursorMzComparison(RowFilter.ComparisonType precursorMzComparison) {
        this.precursorMzComparison = precursorMzComparison;
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
     * Returns the comparison type used for the precursor RT comparison.
     *
     * @return the comparison type used for the precursor RT comparison
     */
    public RowFilter.ComparisonType getPrecursorRTComparison() {
        return precursorRTComparison;
    }

    /**
     * Sets the comparison type used for the precursor RT comparison.
     *
     * @param precursorRTComparison the comparison type used for the precursor
     * RT comparison
     */
    public void setPrecursorRTComparison(RowFilter.ComparisonType precursorRTComparison) {
        this.precursorRTComparison = precursorRTComparison;
    }

    /**
     * Returns the precursor m/z error.
     *
     * @return the precursor m/z error
     */
    public Double getPrecursorMzError() {
        return precursorMzError;
    }

    /**
     * Sets the precursor m/z error.
     *
     * @param precursorMzError the precursor m/z error
     */
    public void setPrecursorMzError(Double precursorMzError) {
        this.precursorMzError = precursorMzError;
    }

    /**
     * Returns the comparison type used for the confidence.
     *
     * @return the comparison type used for the confidence
     */
    public RowFilter.ComparisonType getSearchEngineConfidenceComparison() {
        return searchEngineConfidenceComparison;
    }

    /**
     * Sets the comparison type used for the confidence.
     *
     * @param searchEngineConfidenceComparison the comparison type used for the
     * confidence
     */
    public void setSearchEngineConfidenceComparison(RowFilter.ComparisonType searchEngineConfidenceComparison) {
        this.searchEngineConfidenceComparison = searchEngineConfidenceComparison;
    }

    /**
     * Returns the comparison type used for the score.
     *
     * @return the comparison type used for the score
     */
    public RowFilter.ComparisonType getSearchEngineScoreComparison() {
        return searchEngineScoreComparison;
    }

    /**
     * Sets the comparison type used for the search engine score.
     *
     * @param searchEngineScoreComparison the comparison type used for the
     * search engine score
     */
    public void setSearchEngineScoreComparison(RowFilter.ComparisonType searchEngineScoreComparison) {
        this.searchEngineScoreComparison = searchEngineScoreComparison;
    }

    /**
     * Returns the threshold for the score.
     *
     * @return the threshold for the score
     */
    public Double getSearchEngineScore() {
        return searchEngineScore;
    }

    /**
     * Sets the threshold for the search engine score.
     *
     * @param searchEngineScore the threshold for the search engine score
     */
    public void setSearchEngineScore(Double searchEngineScore) {
        this.searchEngineScore = searchEngineScore;
    }

    /**
     * Returns the threshold for the search engine confidence.
     *
     * @return the threshold for the search engine confidence
     */
    public Double getSearchEngineConfidence() {
        return searchEngineConfidence;
    }

    /**
     * Sets the threshold for the search engine confidence.
     *
     * @param searchEngineConfidence the threshold for the search engine
     * confidence
     */
    public void setSearchEngineConfidence(Double searchEngineConfidence) {
        this.searchEngineConfidence = searchEngineConfidence;
    }

    /**
     * Returns the comparison type used for the precursor m/z error comparison.
     *
     * @return the comparison type used for the precursor m/z error comparison
     */
    public RowFilter.ComparisonType getPrecursorMzErrorComparison() {
        return precursorMzErrorComparison;
    }

    /**
     * Sets the comparison type used for the precursor m/z error comparison.
     *
     * @param precursorMzErrorComparison the comparison type used for the
     * precursor m/z error comparison
     */
    public void setPrecursorMzErrorComparison(RowFilter.ComparisonType precursorMzErrorComparison) {
        this.precursorMzErrorComparison = precursorMzErrorComparison;
    }

    /**
     * Returns the minimal precursor m/z error.
     *
     * @return the minimal precursor m/z error
     */
    public Double getMinPrecursorMzError() {
        return minPrecursorMzError;
    }

    /**
     * Sets the minimal precursor m/z error.
     *
     * @param minPrecursorMzError the minimal precursor m/z error
     */
    public void setMinPrecursorMzError(Double minPrecursorMzError) {
        this.minPrecursorMzError = minPrecursorMzError;
    }

    /**
     * Returns the comparison type used for the precursor min m/z error
     * comparison.
     *
     * @return the comparison type used for the precursor min m/z error
     * comparison
     */
    public RowFilter.ComparisonType getPrecursorMinMzErrorComparison() {
        return precursorMinMzErrorComparison;
    }

    /**
     * Sets the comparison type used for the precursor min m/z error comparison.
     *
     * @param precursorMinMzErrorComparison the comparison type used for the
     * precursor min m/z error comparison
     */
    public void setPrecursorMinMzErrorComparison(RowFilter.ComparisonType precursorMinMzErrorComparison) {
        this.precursorMinMzErrorComparison = precursorMinMzErrorComparison;
    }

    /**
     * Returns the maximal precursor m/z error.
     *
     * @return the maximal precursor m/z error
     */
    public Double getMaxPrecursorMzError() {
        return maxPrecursorMzError;
    }

    /**
     * Sets the maximal precursor m/z error.
     *
     * @param maxPrecursorMzError the maximal precursor m/z error
     */
    public void setMaxPrecursorMzError(Double maxPrecursorMzError) {
        this.maxPrecursorMzError = maxPrecursorMzError;
    }

    /**
     * Returns the comparison type used for the precursor max m/z error
     * comparison.
     *
     * @return the comparison type used for the precursor max m/z error
     * comparison
     */
    public RowFilter.ComparisonType getPrecursorMaxMzErrorComparison() {
        return precursorMaxMzErrorComparison;
    }

    /**
     * Sets the comparison type used for the precursor max m/z error comparison.
     *
     * @param precursorMaxMzErrorComparison the comparison type used for the
     * precursor max m/z error comparison
     */
    public void setPrecursorMaxMzErrorComparison(RowFilter.ComparisonType precursorMaxMzErrorComparison) {
        this.precursorMaxMzErrorComparison = precursorMaxMzErrorComparison;
    }

    /**
     * Returns the sequence coverage by fragment ions threshold in percent.
     *
     * @return the sequence coverage by fragment ions threshold in percent
     */
    public Double getSequenceCoverage() {
        return sequenceCoverage;
    }

    /**
     * Sets the sequence coverage by fragment ions threshold in percent.
     *
     * @param sequenceCoverage the sequence coverage by fragment ions threshold
     * in percent
     */
    public void setSequenceCoverage(Double sequenceCoverage) {
        this.sequenceCoverage = sequenceCoverage;
    }

    /**
     * Returns the comparator for the sequence coverage by fragment ions.
     *
     * @return the comparator for the sequence coverage by fragment ions
     */
    public RowFilter.ComparisonType getSequenceCoverageComparison() {
        return sequenceCoverageComparison;
    }

    /**
     * Sets the comparator for the sequence coverage by fragment ions.
     *
     * @param sequenceCoverageComparison the comparator for the sequence
     * coverage by fragment ions
     */
    public void setSequenceCoverageComparison(RowFilter.ComparisonType sequenceCoverageComparison) {
        this.sequenceCoverageComparison = sequenceCoverageComparison;
    }

    /**
     * Returns the type of error to use.
     *
     * @return the type of error to use
     */
    public IonMatch.MzErrorType getPrecursorMzErrorType() {
        return precursorMzErrorType;
    }

    /**
     * Sets the type of error to use.
     *
     * @param errorType the type of error to use
     */
    public void setPrecursorMzErrorType(IonMatch.MzErrorType errorType) {
        this.precursorMzErrorType = errorType;
    }

    /**
     * Returns the list of spectrum files containing the desired spectra.
     *
     * @return the list of spectrum files containing the desired spectra
     */
    public ArrayList<String> getFileNames() {
        return fileNames;
    }

    /**
     * Sets the list of spectrum files containing the desired spectra.
     *
     * @param filesNames the list of spectrum files containing the desired
     * spectra
     */
    public void setFileNames(ArrayList<String> filesNames) {
        this.fileNames = filesNames;
    }

    /**
     * Tests whether a spectrum match is validated by this filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption to filter
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this one for the probabilistic m/z error filtering (the list should be sorted)
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the identification database
     * @throws java.io.IOException exception thrown whenever an error occurred while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever a backward compatibility error occurred while deserializing a match from the database
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred while interacting with the identification database
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     */
    public boolean isValidated(String spectrumKey, PeptideAssumption peptideAssumption, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator, ArrayList<Double> precursorMzDeviations) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, peptideAssumption, this, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator, precursorMzDeviations);
    }

    /**
     * Tests whether a peptide assumption is validated by a given filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption to filter
     * @param assumptionFilter an assumption filter
     * @param shotgunProtocol information about the protocol
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator a spectrum annotator to annotate the
     * spectrum, can be null
     * @param precursorMzDeviations list of the precursor m/z deviations to
     * compare this one for the probabilistic m/z error filtering (the list should be sorted)
     *
     * @return a boolean indicating whether a spectrum match is validated by a
     * given filter
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the identification database
     * @throws java.io.IOException exception thrown whenever an error occurred while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever a backward compatibility error occurred while deserializing a match from the database
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred while interacting with the identification database
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     */
    public static boolean isValidated(String spectrumKey, PeptideAssumption peptideAssumption, AssumptionFilter assumptionFilter, ShotgunProtocol shotgunProtocol,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator, ArrayList<Double> precursorMzDeviations)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (assumptionFilter.getExceptions().contains(spectrumKey)) {
            return false;
        }
        if (assumptionFilter.getManualValidation().size() > 0) {
            return assumptionFilter.getManualValidation().contains(spectrumKey);
        }

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);

        if (assumptionFilter.getValidationLevel() != null) {
            if (assumptionFilter.getValidationComparison() == RowFilter.ComparisonType.AFTER) {
                if (psParameter.getMatchValidationLevel().getIndex() <= assumptionFilter.getValidationLevel()) {
                    return false;
                }
            } else if (assumptionFilter.getValidationComparison() == RowFilter.ComparisonType.BEFORE) {
                if (psParameter.getMatchValidationLevel().getIndex() > assumptionFilter.getValidationLevel()) {
                    return false;
                }
            } else if (assumptionFilter.getValidationComparison() == RowFilter.ComparisonType.EQUAL) {
                if (psParameter.getMatchValidationLevel().getIndex() != assumptionFilter.getValidationLevel()) {
                    return false;
                }
            } else if (assumptionFilter.getValidationComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                if (psParameter.getMatchValidationLevel().getIndex() == assumptionFilter.getValidationLevel()) {
                    return false;
                }
            }
        }

        if (assumptionFilter.getSearchEngineScore() != null) {
            if (assumptionFilter.getSearchEngineScoreComparison() == RowFilter.ComparisonType.AFTER) {
                if (peptideAssumption.getScore() <= assumptionFilter.getSearchEngineScore()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineScoreComparison() == RowFilter.ComparisonType.BEFORE) {
                if (peptideAssumption.getScore() > assumptionFilter.getSearchEngineScore()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineScoreComparison() == RowFilter.ComparisonType.EQUAL) {
                if (peptideAssumption.getScore() != assumptionFilter.getSearchEngineScore()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineScoreComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                if (peptideAssumption.getScore() == assumptionFilter.getSearchEngineScore()) {
                    return false;
                }
            }
        }

        if (assumptionFilter.getSearchEngineConfidence() != null) {
            if (assumptionFilter.getSearchEngineConfidenceComparison() == RowFilter.ComparisonType.AFTER) {
                if (psParameter.getSearchEngineConfidence() <= assumptionFilter.getSearchEngineConfidence()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineConfidenceComparison() == RowFilter.ComparisonType.BEFORE) {
                if (psParameter.getSearchEngineConfidence() > assumptionFilter.getSearchEngineConfidence()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineConfidenceComparison() == RowFilter.ComparisonType.EQUAL) {
                if (psParameter.getSearchEngineConfidence() != assumptionFilter.getSearchEngineConfidence()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineConfidenceComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                if (psParameter.getSearchEngineConfidence() == assumptionFilter.getSearchEngineConfidence()) {
                    return false;
                }
            }
        }

        if (assumptionFilter.getPrecursorMzError() != null
                || assumptionFilter.getMinPrecursorMzError() != null
                || assumptionFilter.getMaxPrecursorMzError() != null) {

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);

            if (assumptionFilter.getPrecursorMzError() != null) {
                if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.Statistical) {
                    NonSymmetricalNormalDistribution precDeviationDistribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistributionFromSortedList(precursorMzDeviations);
                    Double minDeviation = precDeviationDistribution.getMinValueForProbability(assumptionFilter.getPrecursorMzError());
                    Double maxDeviation = precDeviationDistribution.getMaxValueForProbability(assumptionFilter.getPrecursorMzError());
                    double error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, shotgunProtocol.isMs1ResolutionPpm()));
                    if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                        if (error >= minDeviation && error <= maxDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                        if (error < minDeviation || error > maxDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                        if (error != minDeviation || error != maxDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                        if (error == minDeviation && error == maxDeviation) {
                            return false;
                        }
                    }
                } else {
                    double error;
                    if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.Absolute) {
                        error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, false));
                    } else if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.RelativePpm) {
                        error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, true));
                    } else {
                        throw new IllegalArgumentException("Filter not implemented for error type " + assumptionFilter.getPrecursorMzErrorType().name + ".");
                    }
                    if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                        if (error <= assumptionFilter.getPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                        if (error > assumptionFilter.getPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                        if (error != assumptionFilter.getPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                        if (error == assumptionFilter.getPrecursorMzError()) {
                            return false;
                        }
                    }
                }
            }

            if (assumptionFilter.getMinPrecursorMzError() != null) {
                if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.Statistical) {
                    NonSymmetricalNormalDistribution precDeviationDistribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistribution(precursorMzDeviations); // @TODO: precursorMzDeviations can be empty!!!
                    Double minDeviation = precDeviationDistribution.getMinValueForProbability(assumptionFilter.getMinPrecursorMzError());
                    double error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, shotgunProtocol.isMs1ResolutionPpm()));
                    if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                        if (error <= minDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                        if (error > minDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                        if (error != minDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                        if (error == minDeviation) {
                            return false;
                        }
                    }
                } else {
                    double error;
                    if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.Absolute) {
                        error = peptideAssumption.getDeltaMass(precursorMz, false);
                    } else if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.RelativePpm) {
                        error = peptideAssumption.getDeltaMass(precursorMz, true);
                    } else {
                        throw new IllegalArgumentException("Filter not implemented for error type " + assumptionFilter.getPrecursorMzErrorType().name + ".");
                    }
                    if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                        if (error <= assumptionFilter.getMinPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                        if (error > assumptionFilter.getMinPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                        if (error != assumptionFilter.getMinPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                        if (error == assumptionFilter.getMinPrecursorMzError()) {
                            return false;
                        }
                    }
                }
            }

            if (assumptionFilter.getMaxPrecursorMzError() != null) {
                if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.Statistical) {
                    NonSymmetricalNormalDistribution precDeviationDistribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistribution(precursorMzDeviations); // @TODO: precursorMzDeviations can be empty!!!
                    Double maxDeviation = precDeviationDistribution.getMaxValueForProbability(assumptionFilter.getMaxPrecursorMzError());
                    double error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, shotgunProtocol.isMs1ResolutionPpm()));
                    if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                        if (error <= maxDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                        if (error > maxDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                        if (error != maxDeviation) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                        if (error == maxDeviation) {
                            return false;
                        }
                    }
                } else {
                    double error;
                    if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.Absolute) {
                        error = peptideAssumption.getDeltaMass(precursorMz, false);
                    } else if (assumptionFilter.getPrecursorMzErrorType() == IonMatch.MzErrorType.RelativePpm) {
                        error = peptideAssumption.getDeltaMass(precursorMz, true);
                    } else {
                        throw new IllegalArgumentException("Filter not implemented for error type " + assumptionFilter.getPrecursorMzErrorType().name + ".");
                    }
                    if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                        if (error <= assumptionFilter.getMaxPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                        if (error > assumptionFilter.getMaxPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.EQUAL) {
                        if (error != assumptionFilter.getMaxPrecursorMzError()) {
                            return false;
                        }
                    } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                        if (error == assumptionFilter.getMaxPrecursorMzError()) {
                            return false;
                        }
                    }
                }
            }
        }
        if (assumptionFilter.getCharges() != null) {
            int charge = peptideAssumption.getIdentificationCharge().value;
            if (!assumptionFilter.getCharges().contains(charge)) {
                return false;
            }
        }

        if (assumptionFilter.getPrecursorRT() != null) {

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

            if (assumptionFilter.getPrecursorRT() != null) {
                if (assumptionFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.AFTER) {
                    if (precursor.getRt() <= assumptionFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (assumptionFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (precursor.getRt() > assumptionFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (assumptionFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.EQUAL) {
                    if (precursor.getRt() != assumptionFilter.getPrecursorRT()) {
                        return false;
                    }
                } else if (assumptionFilter.getPrecursorRTComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                    if (precursor.getRt() == assumptionFilter.getPrecursorRT()) {
                        return false;
                    }
                }
            }
        }

        if (assumptionFilter.getFileNames() != null && !assumptionFilter.getFileNames().contains(Spectrum.getSpectrumFile(spectrumKey))) {
            return false;
        }

        if (assumptionFilter.getSequenceCoverage() != null) {

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
            Peptide peptide = peptideAssumption.getPeptide();
            if (peptideSpectrumAnnotator == null) {
                peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
            }
            AnnotationPreferences annotationPreferences = identificationParameters.getAnnotationPreferences();
            SpecificAnnotationPreferences specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), peptideAssumption, identificationParameters.getSequenceMatchingPreferences());
            HashMap<Integer, ArrayList<IonMatch>> matches = peptideSpectrumAnnotator.getCoveredAminoAcids(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);

            double nCovered = 0;
            int nAA = peptide.getSequence().length();
            for (int i = 0; i <= nAA; i++) {
                ArrayList<IonMatch> matchesAtAa = matches.get(i);
                if (matchesAtAa != null && !matchesAtAa.isEmpty()) {
                    nCovered++;
                }
            }
            double coverage = 100 * nCovered / nAA;

            if (assumptionFilter.getSequenceCoverageComparison() == RowFilter.ComparisonType.AFTER) {
                if (coverage <= assumptionFilter.getSequenceCoverage()) {
                    return false;
                }
            } else if (assumptionFilter.getSequenceCoverageComparison() == RowFilter.ComparisonType.BEFORE) {
                if (coverage > assumptionFilter.getSequenceCoverage()) {
                    return false;
                }
            } else if (assumptionFilter.getSequenceCoverageComparison() == RowFilter.ComparisonType.EQUAL) {
                if (coverage != assumptionFilter.getSequenceCoverage()) {
                    return false;
                }
            } else if (assumptionFilter.getSequenceCoverageComparison() == RowFilter.ComparisonType.NOT_EQUAL) {
                if (coverage == assumptionFilter.getSequenceCoverage()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean isValidated(String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        throw new UnsupportedOperationException("Not implemented for Peptide Assumption filters.");
    }

    @Override
    public MatchFilter clone() {
        AssumptionFilter assumptionFilter = new AssumptionFilter(name, description, condition, reportPassed, reportFailed);
        assumptionFilter.setActive(isActive());
        assumptionFilter.setCharges(getCharges());
        assumptionFilter.setPrecursorMzError(getPrecursorMzError());
        assumptionFilter.setPrecursorMzErrorComparison(getPrecursorMzErrorComparison());
        assumptionFilter.setPrecursorMzErrorType(getPrecursorMzErrorType());
        assumptionFilter.setMinPrecursorMzError(getMinPrecursorMzError());
        assumptionFilter.setPrecursorMinMzErrorComparison(getPrecursorMinMzErrorComparison());
        assumptionFilter.setMaxPrecursorMzError(getMaxPrecursorMzError());
        assumptionFilter.setPrecursorMaxMzErrorComparison(getPrecursorMaxMzErrorComparison());
        assumptionFilter.setSequenceCoverage(getSequenceCoverage());
        assumptionFilter.setSequenceCoverageComparison(getSequenceCoverageComparison());
        assumptionFilter.setSearchEngineScore(getSearchEngineScore());
        assumptionFilter.setSearchEngineScoreComparison(getSearchEngineScoreComparison());
        assumptionFilter.setSearchEngineConfidence(getSearchEngineConfidence());
        assumptionFilter.setSearchEngineConfidenceComparison(getSearchEngineConfidenceComparison());
        assumptionFilter.setFileNames(getFileNames());
        assumptionFilter.setPrecursorMz(getPrecursorMz());
        assumptionFilter.setPrecursorMzComparison(getPrecursorMzComparison());
        assumptionFilter.setPrecursorRT(getPrecursorRT());
        assumptionFilter.setPrecursorRTComparison(getPrecursorRTComparison());
        return assumptionFilter;
    }

    /**
     * Indicates whether another filter is the same as the current filter.
     *
     * @param anotherFilter another filter
     *
     * @return a boolean indicating whether another filter is the same as the
     * current filter
     */
    public boolean isSameAs(AssumptionFilter anotherFilter) {
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
        if (getCharges() == null && anotherFilter.getCharges() != null
                || getCharges() != null && anotherFilter.getCharges() == null
                || getCharges() != null && anotherFilter.getCharges() != null
                && !Util.sameLists(getCharges(), anotherFilter.getCharges())) {
            return false;
        }
        if (getPrecursorMzError() == null && anotherFilter.getPrecursorMzError() != null
                || getPrecursorMzError() != null && !getPrecursorMzError().equals(anotherFilter.getPrecursorMzError())) {
            return false;
        }
        if (!getPrecursorMzErrorComparison().equals(anotherFilter.getPrecursorMzErrorComparison())) {
            return false;
        }
        if (!getPrecursorMzErrorType().equals(anotherFilter.getPrecursorMzErrorType())) {
            return false;
        }
        if (getMinPrecursorMzError() == null && anotherFilter.getMinPrecursorMzError() != null
                || getMinPrecursorMzError() != null && !getMinPrecursorMzError().equals(anotherFilter.getMinPrecursorMzError())) {
            return false;
        }
        if (!getPrecursorMinMzErrorComparison().equals(anotherFilter.getPrecursorMinMzErrorComparison())) {
            return false;
        }
        if (getMaxPrecursorMzError() == null && anotherFilter.getMaxPrecursorMzError() != null
                || getMaxPrecursorMzError() != null && !getMaxPrecursorMzError().equals(anotherFilter.getMaxPrecursorMzError())) {
            return false;
        }
        if (!getPrecursorMaxMzErrorComparison().equals(anotherFilter.getPrecursorMaxMzErrorComparison())) {
            return false;
        }
        if (getSequenceCoverage() == null && anotherFilter.getSequenceCoverage() != null
                || getSequenceCoverage() != null && !getSequenceCoverage().equals(anotherFilter.getSequenceCoverage())) {
            return false;
        }
        if (!getSequenceCoverageComparison().equals(anotherFilter.getSequenceCoverageComparison())) {
            return false;
        }
        if (getSearchEngineScore() == null && anotherFilter.getSearchEngineScore() != null
                || getSearchEngineScore() != null && !getSearchEngineScore().equals(anotherFilter.getSearchEngineScore())) {
            return false;
        }
        if (!getSearchEngineScoreComparison().equals(anotherFilter.getSearchEngineScoreComparison())) {
            return false;
        }
        if (getSearchEngineConfidence() == null && anotherFilter.getSearchEngineConfidence() != null
                || getSearchEngineConfidence() != null && !getSearchEngineConfidence().equals(anotherFilter.getSearchEngineConfidence())) {
            return false;
        }
        if (!getSearchEngineConfidenceComparison().equals(anotherFilter.getSearchEngineConfidenceComparison())) {
            return false;
        }
        if (getFileNames() == null && anotherFilter.getFileNames() != null
                || getFileNames() != null && anotherFilter.getFileNames() == null
                || getFileNames() != null && anotherFilter.getFileNames() != null
                && !Util.sameLists(getFileNames(), anotherFilter.getFileNames())) {
            return false;
        }
        if (getPrecursorMz() == null && anotherFilter.getPrecursorMz() != null
                || getPrecursorMz() != null && !getPrecursorMz().equals(anotherFilter.getPrecursorMz())) {
            return false;
        }
        if (!getPrecursorMzComparison().equals(anotherFilter.getPrecursorMzComparison())) {
            return false;
        }
        if (getPrecursorRT() == null && anotherFilter.getPrecursorRT() != null
                || getPrecursorRT() != null && !getPrecursorRT().equals(anotherFilter.getPrecursorRT())) {
            return false;
        }
        if (!getPrecursorRTComparison().equals(anotherFilter.getPrecursorRTComparison())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isSameAs(Filter anotherFilter) {
        if (anotherFilter instanceof AssumptionFilter) {
            return isSameAs((AssumptionFilter) anotherFilter);
        }
        return false;
    }
}
