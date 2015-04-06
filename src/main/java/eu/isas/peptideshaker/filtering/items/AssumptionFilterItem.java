package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;

/**
 * Enum of the different items an assumption filter can filter on.
 *
 * @author Marc Vaudel
 */
public enum AssumptionFilterItem  implements FilterItem {

    precrusorMz("Precursor m/z", "Spectrum precursor m/z."),
    precrusorRT("Precursor RT", "Spectrum precursor retention time."),
    precrusorCharge("Precursor charge", "Spectrum precursor charge according to the identification."),
    precrusorMzErrorDa("Precursor m/z error (Da)", "Spectrum precursor m/z error in Dalton."),
    precrusorMzErrorPpm("Precursor m/z error (ppm)", "Spectrum precursor m/z error in ppm."),
    precrusorMzErrorStat("Precursor m/z error (%p)", "Probability in percent of getting the spectrum precursor m/z error in the spectrum file."),
    sequenceCoverage("Sequence coverage", "Coverage of the sequence by fragment ions in percent."),
    algorithmScore("Algorithm score", "Score given by the identification algorithm."),
    fileNames("Spectrum file", "Name of the spectrum file."),
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
    private AssumptionFilterItem(String name, String description) {
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
    public static AssumptionFilterItem getItem(String itemName) {
        for (AssumptionFilterItem filterItem : AssumptionFilterItem.values()) {
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
