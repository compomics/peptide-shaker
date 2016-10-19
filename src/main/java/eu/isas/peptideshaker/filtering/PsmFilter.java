package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.filtering.items.AssumptionFilterItem;
import eu.isas.peptideshaker.filtering.items.PsmFilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.sql.SQLException;
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
        return new PsmFilter();
    }

    @Override
    public boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String matchKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        PsmFilterItem filterItem = PsmFilterItem.getItem(itemName);
        if (filterItem == null) {
            return assumptionFilter.isValidated(itemName, filterItemComparator, value, matchKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, peptideSpectrumAnnotator);
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

    @Override
    public FilterItem[] getPossibleFilterItems() {
        return PsmFilterItem.values();
    }

    @Override
    public FilterItem getFilterItem(String itemName) {
        FilterItem psmFilterItem = PsmFilterItem.getItem(itemName);
        if (psmFilterItem != null) {
            return psmFilterItem;
        }
        return AssumptionFilterItem.getItem(itemName);
    }
}
