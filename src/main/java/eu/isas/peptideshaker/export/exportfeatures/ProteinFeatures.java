package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import static eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures.values;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the protein identification features.
 *
 * @author Marc Vaudel
 */
public enum ProteinFeatures implements ExportFeature {

    accession(new String[]{"Main Accession"}, "Main accession of the protein group."),
    protein_description(new String[]{"Description"}, "Description of the protein designed by the main accession."),
    ensembl_gene_id(new String[]{"Ensembl Gene ID"}, "The Ensembl gene ID associated to the main accession."),
    gene_name(new String[]{"Gene Name"}, "The gene names of the Ensembl gene ID associated to the main accession."),
    chromosome(new String[]{"Chromosome"}, "The chromosome of the Ensembl gene ID associated to the main accession."),
    go_accession(new String[]{"GO Accession"}, "The accessions of the Gene Ontology terms associated to the accessions of the group."),
    go_description(new String[]{"GO Description"}, "The descriptions of the Gene Ontology terms associated to the accessions of the group."),
    mw(new String[]{"MW (kDa)"}, "Molecular Weight."),
    coverage(new String[]{"Coverage (%)"}, "Sequence coverage in percent of the protein designed by the main accession."),
    possible_coverage(new String[]{"Possible Coverage (%)"}, "Possible sequence coverage in percent of the protein designed by the main accession according to the search settings."),
    non_enzymatic(new String[]{"Non-Enzymatic"}, "Indicates how many non-enzymatic peptides were found for this protein match."),
    spectrum_counting_nsaf(new String[]{"Spectrum Counting NSAF "}, "Normalized Spectrum Abundance Factor (NSAF)"),
    spectrum_counting_empai(new String[]{"Spectrum Counting emPAI"}, "exponentially modified Protein Abundance Index (emPAI)"),
    confident_PTMs(new String[]{"Confident Modification Sites", "# Confident Modification Sites"}, "List of the sites where a variable modification was confidently localized."),
    other_PTMs(new String[]{"Other Modification Sites", "# Other Modification Sites"}, "List of the non-confident sites where a variable modification was localized."),
    confident_phosphosites(new String[]{"Confident Phosphosites"}, "List of the sites where a phosphorylation was confidently localized."),
    other_phosphosites(new String[]{"Other Phosphosites"}, "List of the non-confident sites where a phosphorylation was localized."),
    pi(new String[]{"PI"}, "Protein Inference status of the protein group."),
    other_proteins(new String[]{"Secondary Accessions"}, "Other accessions in the protein group (alphabetical order)."),
    protein_group(new String[]{"Protein Group"}, "The complete protein group (alphabetical order)."),
    validated_peptides(new String[]{"#Validated Peptides"}, "Number of validated peptides."),
    peptides(new String[]{"#Peptides"}, "Total number of peptides."),
    unique_peptides(new String[]{"#Unique"}, "Total number of peptides unique to this protein group."),
    validated_psms(new String[]{"#Validated PSMs"}, "Number of validated PSMs"),
    psms(new String[]{"#PSMs"}, "Number of PSMs"),
    score(new String[]{"Score"}, "Score of the protein group."),
    confidence(new String[]{"Confidence"}, "Confidence in percent associated to the protein group."),
    decoy(new String[]{"Decoy"}, "Indicates whether the protein group is a decoy (1: yes, 0: no)."),
    validated(new String[]{"Validation"}, "Indicates the validation level of the protein group."),
    starred(new String[]{"Starred"}, "Indicates whether the match was starred in the interface (1: yes, 0: no)."),
    hidden(new String[]{"Hidden"}, "Indicates whether the match was hidden in the interface (1: yes, 0: no).");
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
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private ProteinFeatures(String[] title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.addAll(Arrays.asList(values()));
        result.addAll(PeptideFeatures.values()[0].getExportFeatures());
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
}
