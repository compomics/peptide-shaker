package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;

/**
 * Enum of the different items a protein filter can filter on.
 *
 * @author Marc Vaudel
 */
public enum ProteinFilterItem implements FilterItem {

    proteinAccession("Protein Accession", "Accession of the protein according to the sequence database."), 
    proteinDescription("Protein Description", "Description of the protein according to the sequence database."),
    sequence("Potein Sequence", "Amino acid pattern which should be contained in the protein sequence."),
    chromosome("Chromosome", "Chromosome number."),
    gene("Gene", "Gene name."),
    GO("GO Term", "Gene Ontology term"),
    expectedCoverage("Expected Coverage", "Protein sequence coverage expected."), 
    validatedCoverage("Validated Coverage", "Protein sequence coverage achieved using validated peptides."), 
    confidentCoverage("Confident Coverage", "Protein sequence coverage achieved using confident peptides."), 
    spectrumCounting("Spectrum Counting", "Spectrum counting quantification value."),
    ptm("PTM", "Posttranslational modification carried by the protein."),
    nPeptides("# Peptides", "Number of peptides."), 
    nValidatedPeptides("# Validated Peptides", "Number of validated peptides."), 
    nConfidentPeptides("# Confident Peptides", "Number of confident peptides."), 
    nPSMs("# PSMs", "Number of PSMs."), 
    nValidatedPSMs("# Validated PSMs", "Number of Validated PSMs."), 
    nConfidentPSMs("# Confident PSMs", "Number of Confident PSMs."), 
    confidence("Confidence", "Confidence in protein identification."), 
    proteinInference("PI", "Protein inference status."), 
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
    private ProteinFilterItem(String name, String description) {
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
    public static ProteinFilterItem getItem(String itemName) {
        for (ProteinFilterItem filterItem : ProteinFilterItem.values()) {
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
