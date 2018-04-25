package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

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
    protected HashSet<Long> manualValidation = new HashSet<>(0);
    /**
     * The exceptions to the rule.
     */
    protected HashSet<Long> exceptions = new HashSet<>(0);
    /**
     * Name of the manual selection filter.
     */
    public static final String MANUAL_SELECTION = "manual selection";
    /**
     * Map of the comparators to use.
     */
    protected HashMap<String, FilterItemComparator> comparatorsMap = new HashMap<>();
    /**
     * Map of the values to filter on.
     */
    protected HashMap<String, Object> valuesMap = new HashMap<>();

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
    public HashSet<Long> getExceptions() {
        return exceptions;
    }

    /**
     * Returns the manually validated items.
     *
     * @return the manually validated items
     */
    public HashSet<Long> getManualValidation() {
        return manualValidation;
    }

    /**
     * Adds a manually validated Match.
     *
     * @param matchKey the key of the match to add
     */
    public void addManualValidation(Long matchKey) {
        manualValidation.add(matchKey);
    }

    /**
     * Sets the list of manually validated keys.
     *
     * @param manualValidation list of manually validated keys
     */
    public void setManualValidation(HashSet<Long> manualValidation) {
        this.manualValidation = manualValidation;
    }

    /**
     * Adds an exception.
     *
     * @param matchKey the key of the exception to add
     */
    public void addException(Long matchKey) {
        exceptions.add(matchKey);
    }

    /**
     * Sets the excepted matches.
     *
     * @param exceptions the excepted matches
     */
    public void setExceptions(HashSet<Long> exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * Removes a manually validated Match.
     *
     * @param matchKey the key of the match to remove
     */
    public void removeManualValidation(Long matchKey) {
        manualValidation.remove(matchKey);
    }

    /**
     * Removes an exception.
     *
     * @param matchKey the key of the exception to remove
     */
    public void removeException(Long matchKey) {
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
        newFilter.setManualValidation(new HashSet<>(manualValidation));
        newFilter.setExceptions(new HashSet<>(exceptions));
        
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
        return new HashSet<>(valuesMap.keySet());
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
     * Clears the filter items.
     */
    public void clear() {
        valuesMap.clear();
        comparatorsMap.clear();
    }

    /**
     * Tests whether a match is validated by this filter.
     *
     * @param matchKey the key of the match
     * @param identification the identification where to get the information
     * from
     * @param geneMaps the gene maps
     * @param identificationFeaturesGenerator the identification features
     * generator providing identification features
     * @param identificationParameters the identification parameters
     * @param sequenceProvider the protein sequence provider
     * @param proteinDetailsProvider a provider for protein details
     *
     * @return a boolean indicating whether a match is validated by a given
     * filter
     */
    public boolean isValidated(long matchKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider) {

        if (exceptions.contains(matchKey)) {
            
            return false;
        
        }

        if (manualValidation.contains(matchKey)) {
        
            return true;
        
        }
        
        for (Entry<String, Object> entry : valuesMap.entrySet()) {
        
            String itemName = entry.getKey();
            Object value = entry.getValue();
            FilterItemComparator filterItemComparator = comparatorsMap.get(itemName);
            
            if (!isValidated(itemName, filterItemComparator, value, matchKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, sequenceProvider, proteinDetailsProvider)) {
            
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
     * @param geneMaps the gene maps
     * @param identificationFeaturesGenerator the identification feature
     * generator where to get identification features
     * @param identificationParameters the identification parameters used
     * @param sequenceProvider the protein sequence provider
     * @param proteinDetailsProvider the protein details provider
     *
     * @return a boolean indicating whether the match designated by the protein
     * key validates the given item using the given comparator and value
     * threshold.
     */
    public abstract boolean isValidated(String itemName, FilterItemComparator filterItemComparator, Object value, long matchKey, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider);

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
            if (!Util.sameSets(exceptions, otherFilter.getExceptions())) {
                return false;
            }
            if (!Util.sameSets(manualValidation, otherFilter.getManualValidation())) {
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
        return true;
    }
    
    /**
     * Returns the filter items accepted by this filter.
     * 
     * @return the filter items accepted by this filter
     */
    public abstract FilterItem[] getPossibleFilterItems();
    
    /**
     * Returns the filter items accepted by this filter.
     * 
     * @return the filter items accepted by this filter
     */
    public String[] getPossibleFilterItemsNames() {
        FilterItem[] values = getPossibleFilterItems();
        String[] names = new String[values.length];
        for (int i = 0 ; i < values.length ; i++) {
            names[i] = values[i].getName();
        }
        return names;
    }
    
    /**
     * Returns the filter item corresponding to the given name.
     * 
     * @param itemName the name of the filter item
     * 
     * @return the filter item corresponding to the given name
     */
    public abstract FilterItem getFilterItem(String itemName);
    
}
