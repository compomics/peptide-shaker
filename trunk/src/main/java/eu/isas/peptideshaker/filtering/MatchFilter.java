package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.RowFilter;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Abstract representing a filter.
 *
 * @author Marc Vaudel
 */
public abstract class MatchFilter implements Serializable, Filter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 7413446840381260115L;
    /**
     * Name of the filter.
     */
    protected String name = "";
    /**
     * Description of the filter.
     */
    protected String description = "";
    /**
     * Description of the condition to meet to pass the filter.
     */
    protected String condition = "";
    /**
     * Report when the filter is passed.
     */
    protected String reportPassed = "";
    /**
     * Report when the filter is not passed.
     */
    protected String reportFailed = "";
    /**
     * Boolean indicating whether the filter is active.
     */
    private boolean active = true;
    /**
     * The key of the manually validated matches.
     */
    protected ArrayList<String> manualValidation = new ArrayList<String>();
    /**
     * The exceptions to the rule.
     */
    protected ArrayList<String> exceptions = new ArrayList<String>();
    /**
     * Name of the manual selection filter.
     */
    public static final String MANUAL_SELECTION = "manual selection";
    /**
     * Map of the comparators to use.
     */
    protected HashMap<String, FilterItemComparator> comparatorsMap = new HashMap<String, FilterItemComparator>();
    /**
     * Map of the values to filter on.
     */
    protected HashMap<String, Object> valuesMap = new HashMap<String, Object>();

    /**
     * Enum for the type of possible filter.
     */
    public enum FilterType {

        /**
         * Protein filter.
         */
        PROTEIN,
        /**
         * Peptide filter.
         */
        PEPTIDE,
        /**
         * PSM filter.
         */
        PSM,
        /**
         * Peptide Assumption filter.
         */
        ASSUMPTION
    }
    /**
     * The type of filter.
     */
    protected FilterType filterType;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    public String getReport(boolean filterPassed) {
        if (filterPassed) {
            return reportPassed;
        } else {
            return reportFailed;
        }
    }

    /**
     * Sets the name of the filter.
     *
     * @param newName the name to be given to the filter
     */
    public void setName(String newName) {
        name = newName;
    }

    /**
     * Sets the description of the filter.
     *
     * @param description the description of the filter
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the description of the condition to meet.
     *
     * @param condition the description of the condition to meet
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Sets the report when the filter is passed.
     *
     * @param reportPassed the report when the filter is passed
     */
    public void setReportPassed(String reportPassed) {
        this.reportPassed = reportPassed;
    }

    /**
     * Sets the report when the filter is not passed.
     *
     * @param reportFailed the report when the filter is not passed
     */
    public void setReportFailed(String reportFailed) {
        this.reportFailed = reportFailed;
    }

    /**
     * Return the type of the filter.
     *
     * @return the type of the filter
     */
    public FilterType getType() {
        return filterType;
    }

    /**
     * Sets the type of the filter.
     *
     * @param filterType the type of the filter
     */
    public void setType(FilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * Indicates whether the filter is active.
     *
     * @return a boolean indicating whether the filter is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the filter is active.
     *
     * @param active a boolean indicating whether the filter is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns the exceptions to the rule.
     *
     * @return the exceptions to the rule
     */
    public ArrayList<String> getExceptions() {
        return exceptions;
    }

    /**
     * Returns the manually validated items.
     *
     * @return the manually validated items
     */
    public ArrayList<String> getManualValidation() {
        return manualValidation;
    }

    /**
     * Adds a manually validated Match.
     *
     * @param matchKey the key of the match to add
     */
    public void addManualValidation(String matchKey) {
        manualValidation.add(matchKey);
    }

    /**
     * Sets the list of manually validated keys.
     *
     * @param manualValidation list of manually validated keys
     */
    public void setManualValidation(ArrayList<String> manualValidation) {
        this.manualValidation = manualValidation;
    }

    /**
     * Adds an exception.
     *
     * @param matchKey the key of the exception to add
     */
    public void addException(String matchKey) {
        exceptions.add(matchKey);
    }

    /**
     * Sets the excepted matches.
     *
     * @param exceptions the excepted matches
     */
    public void setExceptions(ArrayList<String> exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * Removes a manually validated Match.
     *
     * @param matchKey the key of the match to remove
     */
    public void removeManualValidation(String matchKey) {
        manualValidation.remove(matchKey);
    }

    /**
     * Removes an exception.
     *
     * @param matchKey the key of the exception to remove
     */
    public void removeException(String matchKey) {
        exceptions.remove(matchKey);
    }

    /**
     * Returns a new empty filter.
     *
     * @return a new empty filter
     */
    protected abstract MatchFilter getNew();

    @Override
    public MatchFilter clone() {
        MatchFilter newFilter = getNew();
        newFilter.setName(name);
        newFilter.setDescription(description);
        newFilter.setCondition(condition);
        newFilter.setReportPassed(reportPassed);
        newFilter.setReportFailed(reportFailed);
        newFilter.setType(filterType);
        newFilter.setManualValidation((ArrayList<String>) manualValidation.clone());
        newFilter.setExceptions((ArrayList<String>) exceptions.clone());
        for (String itemName : getItemsNames()) {
            FilterItemComparator filterItemComparator = getComparatorForItem(itemName);
            Object value = getValue(itemName);
            newFilter.setFilterItem(itemName, filterItemComparator, value);
        }
        return newFilter;
    }

    /**
     * Removes an item from the filter.
     *
     * @param itemName the name of the item to remove
     */
    public void removeFilterItem(String itemName) {
        comparatorsMap.remove(itemName);
        valuesMap.remove(itemName);
    }

    /**
     * Sets an item to the filter.
     *
     * @param itemName the name of the item to filter on
     * @param filterItemComparator the comparator
     * @param value the value to filter
     */
    public void setFilterItem(String itemName, FilterItemComparator filterItemComparator, Object value) {
        setComparatorForItem(itemName, filterItemComparator);
        setValueForItem(itemName, value);
    }
    
    /**
     * Sets the comparator for a given item.
     * 
     * @param itemName the name of the item to filter on
     * @param filterItemComparator the comparator
     */
    public void setComparatorForItem(String itemName, FilterItemComparator filterItemComparator) {
        comparatorsMap.put(itemName, filterItemComparator);
    }
    
    /**
     * Sets the value for a given item.
     * 
     * @param itemName the name of the item to filter on
     * @param value the comparator
     */
    public void setValueForItem(String itemName, Object value) {
        valuesMap.put(itemName, value);
    }

    /**
     * Returns the name of the items used to filter.
     *
     * @return the name of the items used to filter
     */
    public HashSet<String> getItemsNames() {
        return new HashSet<String>(valuesMap.keySet());
    }

    /**
     * Returns the comparator set for a given filtering item.
     *
     * @param itemName the name of the item
     *
     * @return the comparator set for a given filtering item
     */
    public FilterItemComparator getComparatorForItem(String itemName) {
        return comparatorsMap.get(itemName);
    }

    /**
     * Returns the value used for comparison for a given filtering item.
     *
     * @param itemName the name of the item
     *
     * @return the value used for comparison for a given filtering item
     */
    public Object getValue(String itemName) {
        return valuesMap.get(itemName);
    }

    /**
     * Tests whether a match is validated by this filter.
     *
     * @param matchKey the key of the match
     * @param identification the identification where to get the information
     * from
     * @param identificationFeaturesGenerator the identification features
     * generator providing identification features
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator the annotator to use to annotate spectra when filtering on psm or assumptions
     *
     * @return a boolean indicating whether a match is validated by a given
     * filter
     *
     * @throws java.io.IOException exception thrown whenever an exception
     * occurred while reading or writing a file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading issue occurred while validating that the match passes the
     * filter
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserilalizing a match
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public boolean isValidated(String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        if (exceptions.contains(matchKey)) {
            return false;
        }

        if (manualValidation.contains(matchKey)) {
            return true;
        }
        for (String itemName : valuesMap.keySet()) {
            FilterItemComparator filterItemComparator = comparatorsMap.get(itemName);
            Object value = valuesMap.get(itemName);
            if (!isValidated(itemName, filterItemComparator, value, matchKey, identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether the match designated by the match key validates the
     * given item using the given comparator and value threshold.
     *
     * @param itemName the name of the item to filter on
     * @param filterItemComparator the comparator to use
     * @param value the value to use as a threshold
     * @param matchKey the key of the match of interest
     * @param identification the identification objects where to get
     * identification matches from
     * @param identificationFeaturesGenerator the identification feature
     * generator where to get identification features
     * @param shotgunProtocol information on the protocol used
     * @param identificationParameters the identification parameters used
     * @param peptideSpectrumAnnotator the annotator to use to annotate spectra when filtering on psm or assumptions
     *
     * @return a boolean indicating whether the match designated by the protein
     * key validates the given item using the given comparator and value
     * threshold.
     *
     * @throws java.io.IOException exception thrown whenever an exception
     * occurred while reading or writing a file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading issue occurred while validating that the match passes the
     * filter
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserilalizing a match
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with a database
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public abstract boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException;

    @Override
    public boolean isSameAs(Filter anotherFilter) {
        if (anotherFilter instanceof MatchFilter) {
            MatchFilter otherFilter = (MatchFilter) anotherFilter;

            if (!name.equals(otherFilter.getName())) {
                return false;
            }
            if (!description.equals(otherFilter.getDescription())) {
                return false;
            }
            if (!condition.equals(otherFilter.getCondition())) {
                return false;
            }
            if (!reportPassed.equals(otherFilter.getReport(true))) {
                return false;
            }
            if (!reportFailed.equals(otherFilter.getReport(false))) {
                return false;
            }
            if (isActive() != otherFilter.isActive()) {
                return false;
            }
            if (!Util.sameLists(exceptions, otherFilter.getExceptions())) {
                return false;
            }
            if (!Util.sameLists(manualValidation, otherFilter.getManualValidation())) {
                return false;
            }
            if (!Util.sameSets(getItemsNames(), otherFilter.getItemsNames())) {
                return false;
            }
            for (String itemName : getItemsNames()) {
                FilterItemComparator thisComparator = getComparatorForItem(itemName),
                        otherComparator = otherFilter.getComparatorForItem(itemName);
                if (thisComparator != otherComparator) {
                    return false;
                }
                Object thisValue = getValue(itemName),
                        otherValue = otherFilter.getValue(itemName);
                if (!thisValue.equals(otherValue)) {
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns the filter items accepted by this filter.
     * 
     * @return the filter items accepted by this filter
     */
    public abstract FilterItem[] getPossibleFilterItems();
    
    /**
     * Returns the filter item corresponding to the given name.
     * 
     * @param itemName the name of the filter item
     * 
     * @return the filter item corresponding to the given name
     */
    public abstract FilterItem getFilterItem(String itemName);

    /**
     * Validation level.
     *
     * @deprecated
     */
    private Integer validationLevel = null;
    /**
     * The type of comparison to be used for the confidence.
     *
     * @deprecated
     */
    private RowFilter.ComparisonType validationComparison = RowFilter.ComparisonType.EQUAL;

    /**
     * Returns the validation level used for filtering.
     *
     * @return the validation level used for filtering
     *
     * @deprecated
     */
    public Integer getValidationLevel() {
        return validationLevel;
    }

    /**
     * Sets the validation level used for filtering.
     *
     * @param validationLevel the validation level used for filtering
     *
     * @deprecated
     */
    public void setValidationLevel(Integer validationLevel) {
        this.validationLevel = validationLevel;
    }

    /**
     * Returns the comparison type used for validation level comparison.
     *
     * @return the comparison type used for validation level comparison
     *
     * @deprecated
     */
    public RowFilter.ComparisonType getValidationComparison() {
        return validationComparison;
    }

    /**
     * Sets the comparison type used for validation level comparison.
     *
     * @param validationComparison the comparison type used for validation level
     * comparison
     *
     * @deprecated
     */
    public void setValidationComparison(RowFilter.ComparisonType validationComparison) {
        this.validationComparison = validationComparison;
    }
}
