package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;

/**
 * Enum of the different items a PSM filter can filter on.
 *
 * @author Marc Vaudel
 */
public enum PsmFilterItem implements FilterItem {

    confidence("Confidence", "Confidence in protein identification."), 
    validationStatus("Validation", "Validation status."),
    stared("Stared", "Marked with a yellow star.");
    
    /**
     * The name of the filtering item.
     */
    public final String name;
    /**
     * The description of the filtering item.
     */
    public final String description;
    
    /**
     * Constructor.
     * 
     * @param name name of the filtering item
     * @param description description of the filtering item
     */
    private PsmFilterItem(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Returns the item designated by the given name.
     * 
     * @param itemName the name of the item of interest
     * 
     * @return the item of interest
     */
    public static PsmFilterItem getItem(String itemName) {
        for (PsmFilterItem filterItem : PsmFilterItem.values()) {
            if (filterItem.name.equals(itemName)) {
                return filterItem;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
