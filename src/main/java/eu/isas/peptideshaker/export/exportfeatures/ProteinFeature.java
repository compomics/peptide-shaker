package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the protein identification features.
 *
 * @author Marc Vaudel
 */
public enum ProteinFeature implements ExportFeature {

    accession(new String[]{"Main Accession"}, "Accession of the leading protein of the group.", false),
    protein_description(new String[]{"Description"}, "Description of the leading protein of the group.", false),
    descriptions(new String[]{"Descriptinos"}, "Description of the proteins of the group", false),
    ensembl_gene_id(new String[]{"Ensembl Gene ID"}, "The Ensembl gene ID associated to the accession of the leading protein of the group.", false),
    gene_name(new String[]{"Gene Name"}, "The gene names of the Ensembl gene ID associated to the accession of the leading protein of the group.", false),
    chromosome(new String[]{"Chromosome"}, "The chromosome of the Ensembl gene ID associated to the accession of the leading protein of the group.", false),
    go_accession(new String[]{"GO Accession"}, "The accessions of the Gene Ontology terms associated to the accessions of the group.", false),
    go_description(new String[]{"GO Description"}, "The descriptions of the Gene Ontology terms associated to the accessions of the group.", false),
    mw(new String[]{"MW [kDa]"}, "Molecular Weight [kDa].", false),
    coverage(new String[]{"Coverage [%]"}, "Sequence coverage in percent of the protein designed by the main accession.", false),
    possible_coverage(new String[]{"Possible Coverage [%]"}, "Possible sequence coverage in percent of the protein designed by the main accession according to the search settings.", false),
    non_enzymatic(new String[]{"Non-Enzymatic"}, "Indicates how many non-enzymatic peptides were found for this protein match.", false),
    spectrum_counting_nsaf(new String[]{"Spectrum Counting NSAF "}, "Normalized Spectrum Abundance Factor (NSAF)", false),
    spectrum_counting_empai(new String[]{"Spectrum Counting emPAI"}, "exponentially modified Protein Abundance Index (emPAI)", false),
    confident_PTMs(new String[]{"Confident Modification Sites", "# Confident Modification Sites"}, "List of the sites where a variable modification was confidently localized.", false),
    other_PTMs(new String[]{"Other Modification Sites", "# Other Modification Sites"}, "List of the non-confident sites where a variable modification was localized.", false),
    confident_phosphosites(new String[]{"Confident Phosphosites"}, "List of the sites where a phosphorylation was confidently localized.", false),
    other_phosphosites(new String[]{"Other Phosphosites"}, "List of the non-confident sites where a phosphorylation was localized.", false),
    pi(new String[]{"PI"}, "Protein Inference status of the protein group.", false),
    other_proteins(new String[]{"Secondary Accessions"}, "Other accessions in the protein group (alphabetical order).", false),
    protein_group(new String[]{"Protein Group"}, "The complete protein group (alphabetical order).", false),
    validated_peptides(new String[]{"#Validated Peptides"}, "Number of validated peptides.", false),
    peptides(new String[]{"#Peptides"}, "Total number of peptides.", false),
    unique_peptides(new String[]{"#Unique"}, "Total number of peptides unique to this protein group.", false),
    validated_psms(new String[]{"#Validated PSMs"}, "Number of validated PSMs", false),
    psms(new String[]{"#PSMs"}, "Number of PSMs", false),
    score(new String[]{"Score"}, "Score of the protein group.", true),
    raw_score(new String[]{"Raw Score"}, "Protein group score before log transformation.", true),
    confidence(new String[]{"Confidence"}, "Confidence in percent associated to the protein group.", false),
    decoy(new String[]{"Decoy"}, "Indicates whether the protein group is a decoy (1: yes, 0: no).", false),
    validated(new String[]{"Validation"}, "Indicates the validation level of the protein group.", false),
    starred(new String[]{"Starred"}, "Indicates whether the match was starred in the interface (1: yes, 0: no).", false),
    hidden(new String[]{"Hidden"}, "Indicates whether the match was hidden in the interface (1: yes, 0: no).", false);
    /**
     * The title of the feature which will be used for column heading.
     */
    public String[] title;
    /**
     * The description of the feature.
     */
    public String description;
    /**
     * The type of export feature.
     */
    public final static String type = "Protein Identification Summary";
    /**
     * Indicates whether a feature is for advanced user only.
     */
    private boolean advanced;

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param advanced indicates whether a feature is for advanced user only
     */
    private ProteinFeature(String[] title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.addAll(Arrays.asList(values()));
        if (includeSubFeatures) {
        result.addAll(PeptideFeature.values()[0].getExportFeatures(includeSubFeatures));
        }
        return result;
    }

    @Override
    public String[] getTitles() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getFeatureFamily() {
        return type;
    }

    @Override
    public boolean isAdvanced() {
        return advanced;
    }
}
