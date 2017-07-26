package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;
import java.util.Arrays;

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

    @Override
    public FilterItem[] getPossibleValues() {
        PsmFilterItem[] values = values();
        AssumptionFilterItem[] assumptionValues = AssumptionFilterItem.values();
        int totalLength = values.length + assumptionValues.length;
        FilterItem[] result = new FilterItem[totalLength];
        int i = 0;
        for (; i < values.length; i++) {
            result[i] = values[i];
        }
        for (AssumptionFilterItem assumptionValue : assumptionValues) {
            result[i] = assumptionValue;
            i++;
        }
        return result;
    }

    @Override
    public boolean isNumber() {
        switch (this) {
            case confidence:
                return true;
            default:
                return false;
        }
    }

    @Override
    public ArrayList<String> getPossibilities() {
        switch (this) {
            case validationStatus:
                return new ArrayList<>(Arrays.asList(MatchValidationLevel.getValidationLevelsNames()));
            case stared:
                ArrayList<String> starred = new ArrayList<>(2);
                starred.add("Starred");
                starred.add("Not Starred");
                return starred;
            default:
                return null;
        }
    }

    @Override
    public boolean isPtm() {
        switch (this) {
            default:
                return false;
        }
    }
}
