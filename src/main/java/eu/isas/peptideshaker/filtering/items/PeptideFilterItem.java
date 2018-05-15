package eu.isas.peptideshaker.filtering.items;

import com.compomics.util.experiment.filtering.FilterItem;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Enum of the different items a peptide filter can filter on.
 *
 * @author Marc Vaudel
 */
public enum PeptideFilterItem implements FilterItem {

    proteinAccession("Protein Accession", "Accession of the protein this peptide maps to according to the sequence database."),
    proteinDescription("Protein Description", "Description of the protein according to the sequence database."),
    sequence("Peptide Sequence", "Amino acid pattern which should be contained in the peptide sequence."),
    modification("modification", "Modification carried by the peptide."),
    nPSMs("#PSMs", "Number of PSMs."),
    nValidatedPSMs("#Validated PSMs", "Number of Validated PSMs."),
    nConfidentPSMs("#Confident PSMs", "Number of Confident PSMs."),
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

    @Override
    public FilterItem[] getPossibleValues() {
        PeptideFilterItem[] values = values();
        FilterItem[] result = new FilterItem[values.length];
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }

    @Override
    public boolean isNumber() {
        switch (this) {
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
                ArrayList<String> pi = new ArrayList<>(4); // @TODO: check that this is correct
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
