package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.filtering.items.PsmFilterItem;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.RowFilter.ComparisonType;
import org.apache.commons.math.MathException;
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
     * The filter used to filter the best assumption.
     */
    private AssumptionFilter assumptionFilter;

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
     */
    public PsmFilter() {
        assumptionFilter = new AssumptionFilter();
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
     * Returns the filter used to filter at the assumption level.
     * 
     * @return the assumption filter
     */
    public AssumptionFilter getAssumptionFilter() {
        return assumptionFilter;
    }

    @Override
    protected MatchFilter getNew() {
        return new ProteinFilter();
    }

    @Override
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {
        
        PsmFilterItem filterItem = PsmFilterItem.getItem(itemName);
        if (filterItem == null) {
            return assumptionFilter.isValidated(itemName, filterItemComparator, value, matchKey, identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator);
        }
        String input = value.toString();
        switch (filterItem) {
            case confidence:
                PSParameter psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                Double confidence = psParameter.getProteinConfidence();
                return filterItemComparator.passes(input, confidence.toString());
            case validationStatus:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                Integer validation = psParameter.getMatchValidationLevel().getIndex();
                return filterItemComparator.passes(input, validation.toString());
            case stared:
                psParameter = new PSParameter();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
                String starred;
                if (psParameter.isStarred()) {
                    starred = FilterItemComparator.trueFalse[0];
                } else {
                    starred = FilterItemComparator.trueFalse[1];
                }
                return filterItemComparator.passes(input, starred);
            default:
                throw new IllegalArgumentException("Protein filter not implemented for item " + filterItem.name + ".");
        }
    }

    /**
     * Checks whether it is an old filter using the deprecated code below and
     * converts it to the new structure
     */
    public void backwardCompatibilityCheck() {
        if (psmConfidence != null) {
            if (psmConfidenceComparison == ComparisonType.BEFORE) {
                setFilterItem(PsmFilterItem.confidence.name, FilterItemComparator.lower, psmConfidence);
            } else if (psmConfidenceComparison == ComparisonType.AFTER) {
                setFilterItem(PsmFilterItem.confidence.name, FilterItemComparator.higher, psmConfidence);
            } else if (psmConfidenceComparison == ComparisonType.EQUAL) {
                setFilterItem(PsmFilterItem.confidence.name, FilterItemComparator.equal, psmConfidence);
            }
            psmConfidence = null;
        }
    }
    
    /**
     * Score limit.
     *
     * @deprecated use the filter items instead
     */
    private Double psmScore = null;
    /**
     * The type of comparison to be used for the PSM score.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType psmScoreComparison = ComparisonType.EQUAL;
    /**
     * Confidence limit.
     *
     * @deprecated use the filter items instead
     */
    private Double psmConfidence = null;
    /**
     * The type of comparison to be used for the PSM confidence.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType psmConfidenceComparison = ComparisonType.EQUAL;
    /**
     * The precursor m/z.
     *
     * @deprecated use the filter items instead
     */
    private Double precursorMz = null;
    /**
     * The type of comparison to be used for the precursor m/z.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType precursorMzComparison = ComparisonType.EQUAL;
    /**
     * The precursor retention time.
     *
     * @deprecated use the filter items instead
     */
    private Double precursorRT = null;
    /**
     * The type of comparison to be used for the precursor retention time.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType precursorRTComparison = ComparisonType.EQUAL;
    /**
     * List of spectrum files names retained.
     *
     * @deprecated use the filter items instead
     */
    private ArrayList<String> fileName = null;
    /**
     * The charges allowed.
     *
     * @deprecated use the filter items instead
     */
    private ArrayList<Integer> charges = null;
    /**
     * The precursor m/z error.
     *
     * @deprecated use the filter items instead
     */
    private Double precursorMzError = null;
    /**
     * The type of comparison to be used for the precursor m/z error.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType precursorMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The minimal precursor m/z error.
     *
     * @deprecated use the filter items instead
     */
    private Double minPrecursorMzError = null;
    /**
     * The type of comparison to be used for the min precursor m/z error
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType precursorMinMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The maximal precursor m/z error.
     *
     * @deprecated use the filter items instead
     */
    private Double maxPrecursorMzError = null;
    /**
     * The type of comparison to be used for the max precursor m/z error.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType precursorMaxMzErrorComparison = ComparisonType.EQUAL;
    /**
     * The amino acid coverage by fragment ions.
     *
     * @deprecated use the filter items instead
     */
    private Double sequenceCoverage = null;
    /**
     * The type of comparison to be used for the PSM confidence.
     *
     * @deprecated use the filter items instead
     */
    private ComparisonType sequenceCoverageComparison = ComparisonType.EQUAL;
}
