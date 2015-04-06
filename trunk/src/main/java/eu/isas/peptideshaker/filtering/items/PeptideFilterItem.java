package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;

/**
 * Enum of the different items a peptide filter can filter on.
 *
 * @author Marc Vaudel
 */
public enum PeptideFilterItem implements FilterItem {

    proteinAccession("Protein Accession", "Accession of the protein this peptide maps to according to the sequence database."), 
    proteinDescription("Protein Description", "Description of the protein according to the sequence database."),
    sequence("Peptide Sequence", "Amino acid pattern which should be contained in the peptide sequence."),
    ptm("PTM", "Posttranslational modification carried by the peptide."),
    nPSMs("# PSMs", "Number of PSMs."), 
    nValidatedPSMs("# Validated PSMs", "Number of Validated PSMs."), 
    nConfidentPSMs("# Confident PSMs", "Number of Confident PSMs."), 
    confidence("Confidence", "Confidence in protein identification."), 
    proteinInference("PI", "Protein inference status of the peptide."), 
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
    private PeptideFilterItem(String name, String description) {
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
    public static PeptideFilterItem getItem(String itemName) {
        for (PeptideFilterItem filterItem : PeptideFilterItem.values()) {
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
