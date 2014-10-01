package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.preferences.AnnotationPreferences;
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
     * Constructor.
     *
     * @param name the name of the filter
     */
    public AssumptionFilter(String name) {
        this.name = name;
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
     * Tests whether a spectrum match is validated by this filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption to filter
     * @param searchParameters the identification parameters
     * @param annotationPreferences the spectrum annotation preferences
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
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
    public boolean isValidated(String spectrumKey, PeptideAssumption peptideAssumption, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, PeptideSpectrumAnnotator peptideSpectrumAnnotator)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        return isValidated(spectrumKey, peptideAssumption, this, searchParameters, annotationPreferences, peptideSpectrumAnnotator);
    }

    /**
     * Tests whether a peptide assumption is validated by a given filter.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption to filter
     * @param assumptionFilter the assumption filter to use
     * @param searchParameters the identification parameters
     * @param annotationPreferences the spectrum annotation preferences
     * @param peptideSpectrumAnnotator a spectrum annotator to annotate the spectrum, can be null
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
    public static boolean isValidated(String spectrumKey, PeptideAssumption peptideAssumption, AssumptionFilter assumptionFilter, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, PeptideSpectrumAnnotator peptideSpectrumAnnotator)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (assumptionFilter.getExceptions().contains(spectrumKey)) {
            return false;
        }
        if (assumptionFilter.getManualValidation().size() > 0) {
            return assumptionFilter.getManualValidation().contains(spectrumKey);
        }

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);

        if (assumptionFilter.getSearchEngineScore() != null) {
            if (assumptionFilter.getSearchEngineScoreComparison() == RowFilter.ComparisonType.AFTER) {
                if (peptideAssumption.getScore() <= assumptionFilter.getSearchEngineScore()) {
                    return false;
                }
            } else if (assumptionFilter.getSearchEngineScoreComparison() == RowFilter.ComparisonType.BEFORE) {
                if (peptideAssumption.getScore() >= assumptionFilter.getSearchEngineScore()) {
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
                if (psParameter.getSearchEngineConfidence() >= assumptionFilter.getSearchEngineConfidence()) {
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
            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

            if (assumptionFilter.getPrecursorMzError() != null) {
                double error = Math.abs(peptideAssumption.getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm()));
                if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                    if (error <= assumptionFilter.getPrecursorMzError()) {
                        return false;
                    }
                } else if (assumptionFilter.getPrecursorMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (error >= assumptionFilter.getPrecursorMzError()) {
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

            if (assumptionFilter.getMinPrecursorMzError() != null) {
                double error = peptideAssumption.getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm());
                if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                    if (error <= assumptionFilter.getMinPrecursorMzError()) {
                        return false;
                    }
                } else if (assumptionFilter.getPrecursorMinMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (error >= assumptionFilter.getMinPrecursorMzError()) {
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

            if (assumptionFilter.getMaxPrecursorMzError() != null) {
                double error = peptideAssumption.getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm());
                if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.AFTER) {
                    if (error <= assumptionFilter.getMaxPrecursorMzError()) {
                        return false;
                    }
                } else if (assumptionFilter.getPrecursorMaxMzErrorComparison() == RowFilter.ComparisonType.BEFORE) {
                    if (error >= assumptionFilter.getMaxPrecursorMzError()) {
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
        if (assumptionFilter.getCharges() != null) {
            int charge = peptideAssumption.getIdentificationCharge().value;
            if (!assumptionFilter.getCharges().contains(charge)) {
                return false;
            }
        }

        if (assumptionFilter.getSequenceCoverage() != null) {

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
            Peptide peptide = peptideAssumption.getPeptide();
            if (peptideSpectrumAnnotator == null) {
                peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
            }
            HashMap<Integer, ArrayList<IonMatch>> ionMatches = peptideSpectrumAnnotator.getCoveredAminoAcids(annotationPreferences.getIonTypes(),
                    annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                    peptideAssumption.getIdentificationCharge().value, spectrum, peptide, spectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                    searchParameters.getFragmentIonAccuracy(), false, annotationPreferences.isHighResolutionAnnotation());

            double nCovered = 0;
            int nAA = peptide.getSequence().length();
            for (int i = 0; i <= nAA; i++) {
                ArrayList<IonMatch> matchesAtAa = ionMatches.get(i);
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
                if (coverage >= assumptionFilter.getSequenceCoverage()) {
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
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException {
        throw new UnsupportedOperationException("Not implemented for Peptide Assumption filters.");
    }
}
