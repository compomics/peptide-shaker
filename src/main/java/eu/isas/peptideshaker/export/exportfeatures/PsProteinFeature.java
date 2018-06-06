package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the protein identification features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum PsProteinFeature implements ExportFeature {

    accession("Main Accession", "Accession of the leading protein of the group.", false),
    protein_description("Description", "Description of the leading protein of the group.", false),
    descriptions("Descriptions", "Description of the proteins of the group", false),
    ensembl_gene_id("Ensembl Gene ID", "The Ensembl gene ID associated to the accession of the leading protein of the group.", false),
    gene_name("Gene Name", "The gene names of the Ensembl gene ID associated to the accession of the leading protein of the group.", false),
    chromosome("Chromosome", "The chromosome of the Ensembl gene ID associated to the accession of the leading protein of the group.", false),
    go_accession("GO Accession", "The accessions of the Gene Ontology terms associated to the accessions of the group.", false),
    go_description("GO Description", "The descriptions of the Gene Ontology terms associated to the accessions of the group.", false),
    mw("MW [kDa]", "Molecular Weight [kDa].", false),
    proteinLength("Protein length", "The length of the protein.", false),
    possible_coverage("Possible Coverage [%]", "Possible sequence coverage in percent of the protein designed by the main accession according to the search settings.", false),
    coverage("Coverage [%]", "Sequence coverage by validated peptides in percent of the protein designed by the main accession.", false),
    confident_coverage("Confident Coverage [%]", "Sequence coverage by confident peptides in percent of the protein designed by the main accession.", false),
    non_enzymatic("Non-Enzymatic", "Indicates how many non-enzymatic peptides were found for this protein match.", false),
    spectrum_counting("Spectrum Counting", "The selected spectrum counting metric.", false),
    spectrum_counting_nsaf("Spectrum Counting NSAF", "Raw Normalized Spectrum Abundance Factor (NSAF).", false),
    spectrum_counting_empai("Spectrum Counting emPAI", "Raw exponentially modified Protein Abundance Index (emPAI).", false),
    spectrum_counting_nsaf_percent("Spectrum Counting NSAF [%]", "Normalized Spectrum Abundance Factor (NSAF) in percent.", false),
    spectrum_counting_empai_percent("Spectrum Counting emPAI [%]", "exponentially modified Protein Abundance Index (emPAI) in percent.", false),
    spectrum_counting_nsaf_ppm("Spectrum Counting NSAF [ppm]", "Normalized Spectrum Abundance Factor (NSAF) in ppm.", false),
    spectrum_counting_empai_ppm("Spectrum Counting emPAI [ppm]", "exponentially modified Protein Abundance Index (emPAI) in ppm.", false),
    spectrum_counting_nsaf_fmol("Spectrum Counting NSAF [fmol]", "Normalized Spectrum Abundance Factor (NSAF) in femtomol.", false),
    spectrum_counting_empai_fmol("Spectrum Counting emPAI [fmol]", "exponentially modified Protein Abundance Index (emPAI) in femtomol.", false),
    confident_modification_sites("Confidently Localized Modification Sites", "List of the sites where a variable modification was confidently localized.", false),
    confident_modification_sites_number("# Confidently Localized Modification Sites", "Number of sites where a variable modification was confidently localized.", false),
    ambiguous_modification_sites("Ambiguously Localized Modification Sites", "List of the sites where ambiguously localized variable modification could possibly be located.", false),
    ambiguous_modification_sites_number("#Ambiguously Localized Modification Sites", "Number of ambiguously localized modifications.", false),
    confident_phosphosites("Confident Phosphosites", "List of the sites where a phosphorylation was confidently localized.", false),
    confident_phosphosites_number("#Confident Phosphosites", "Number of sites where a phosphorylation was confidently localized.", false),
    ambiguous_phosphosites("Ambiguous Phosphosites", "List of the sites where a phosphorylation was ambiguously localized.", false),
    ambiguous_phosphosites_number("#Ambiguous Phosphosites", "Number of sites where a phosphorylation was ambiguously localized.", false),
    pi("Protein Inference", "Protein Inference status of the protein group.", false),
    other_proteins("Secondary Accessions", "Other accessions in the protein group (alphabetical order).", false),
    protein_group("Protein Group", "The complete protein group (alphabetical order).", false),
    validated_peptides("#Validated Peptides", "Number of validated peptides.", false),
    peptides("#Peptides", "Total number of peptides.", false),
    unique_peptides("#Unique", "Total number of peptides unique to this protein group.", false),
    unique_validated_peptides("#Validated Unique", "Total number of peptides unique to this protein group.", false),
    validated_psms("#Validated PSMs", "Number of validated PSMs.", false),
    psms("#PSMs", "Number of PSMs.", false),
    score("Score", "Score of the protein group.", true),
    raw_score("Raw Score", "Protein group score before log transformation.", true),
    confidence("Confidence [%]", "Confidence in percent associated to the protein group.", false),
    decoy("Decoy", "Indicates whether the protein group is a decoy (1: yes, 0: no).", false),
    validated("Validation", "Indicates the validation level of the protein group.", false),
    starred("Starred", "Indicates whether the match was starred in the interface (1: yes, 0: no).", false),
    hidden("Hidden", "Indicates whether the match was hidden in the interface (1: yes, 0: no).", false);

    /**
     * The title of the feature which will be used for column heading.
     */
    public String title;
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
    private final boolean advanced;

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param advanced indicates whether a feature is for advanced user only
     */
    private PsProteinFeature(String title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = new ArrayList<>();
        result.addAll(Arrays.asList(values()));
        if (includeSubFeatures) {
            result.addAll(PsPeptideFeature.values()[0].getExportFeatures(includeSubFeatures));
        }
        return result;
    }

    @Override
    public String getTitle() {
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
