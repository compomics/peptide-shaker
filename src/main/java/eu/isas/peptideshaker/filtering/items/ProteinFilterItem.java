package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;
import java.util.Arrays;

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
    modification("modification", "Modification carried by the protein."),
    nPeptides("#Peptides", "Number of peptides."),
    nValidatedPeptides("#Validated Peptides", "Number of validated peptides."),
    nConfidentPeptides("#Confident Peptides", "Number of confident peptides."),
    nPSMs("#PSMs", "Number of PSMs."),
    nValidatedPSMs("#Validated PSMs", "Number of Validated PSMs."),
    nConfidentPSMs("#Confident PSMs", "Number of Confident PSMs."),
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

    @Override
    public FilterItem[] getPossibleValues() {
        ProteinFilterItem[] values = values();
        FilterItem[] result = new FilterItem[values.length];
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }

    @Override
    public boolean isNumber() {
        switch (this) {
            case expectedCoverage:
            case validatedCoverage:
            case confidentCoverage:
            case spectrumCounting:
            case nPeptides:
            case nValidatedPeptides:
            case nConfidentPeptides:
            case nPSMs:
            case nValidatedPSMs:
            case nConfidentPSMs:
            case confidence:
                return true;
            default:
                return false;
        }
    }

    @Override
    public ArrayList<String> getPossibilities() {
        switch (this) {
            case proteinInference:
                ArrayList<String> pi = new ArrayList<>(4);
                pi.add(PSParameter.getProteinInferenceClassAsString(PSParameter.NOT_GROUP));
                pi.add(PSParameter.getProteinInferenceClassAsString(PSParameter.RELATED));
                pi.add(PSParameter.getProteinInferenceClassAsString(PSParameter.RELATED_AND_UNRELATED));
                pi.add(PSParameter.getProteinInferenceClassAsString(PSParameter.UNRELATED));
                return pi;
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
    public boolean needsModifications() {
        switch (this) {
            case modification:
                return true;
            default:
                return false;
        }
    }
}
