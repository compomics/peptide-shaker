package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the peptide identification features.
 *
 * @author Marc Vaudel
 */
public enum PsPeptideFeature implements ExportFeature {

    starred("Starred", "Indicates whether the match was starred in the interface (1: yes, 0: no).", false),
    pi("Protein Inference", "The protein inference status of this peptide.", false),
    accessions("Protein(s)", "All Protein(s) to which this peptide can map.", false),
    protein_description("Description(s)", "Description of the protein(s) to which this peptide can map.", false),
    protein_groups("Protein Group(s)", "List of identified protein groups this peptide can map to with associated validation level.", true),
    best_protein_group_validation("Protein Validation", "Best validation status among the protein groups this peptide maps to.", true),
    nValidatedProteinGroups("#Validated Protein Group(s)", "Indicates the number of protein groups this peptide maps to.", false),
    unique_group("Unique Protein Group", "Indicates whether the peptide maps to a unique protein group.", false),
    sequence("Sequence", "Sequence of the peptide.", false),
    modified_sequence("Modified Sequence", "The peptide sequence annotated with variable modifications.", false),
    position("Position", "Position of the peptide in the protein sequence(s).", false),
    aaBefore("AAs Before", "The amino acids before the sequence.", false),
    aaAfter("AAs After", "The amino acids after the sequence.", false),
    missed_cleavages("Missed Cleavages", "The number of missed cleavages.", false),
    variable_ptms("Variable Modifications", "The variable modifications.", false),
    fixed_ptms("Fixed Modifications", "The fixed modifications.", false),
    localization_confidence("Localization Confidence", "The confidence in PTMs localization.", false),
    probabilistic_score("Probabilistic PTM score", "The best probabilistic score (e.g. A-score or PhosphoRS) among all validated PSMs for this peptide.", false),
    d_score("D-score", "The best D-score for variable PTM localization among all validated PSMs for this peptide.", false),
    confident_modification_sites("Confidently Localized Modification Sites", "List of the sites where a variable modification was confidently localized.", false),
    confident_modification_sites_number("#Confidently Localized Modification Sites", "Number of sites where a variable modification was confidently localized.", false),
    ambiguous_modification_sites("Ambiguously Localized Modification Sites", "List of the sites where ambiguously localized variable modification could possibly be located.", false),
    ambiguous_modification_sites_number("#Ambiguously Localized Modification Sites", "Number of ambiguously localized modifications.", false),
    confident_phosphosites("Confident Phosphosites", "List of the sites where a phosphorylation was confidently localized.", false),
    confident_phosphosites_number("#Confident Phosphosites", "Number of confidently localized phosphorylations.", false),
    ambiguous_phosphosites("Ambiguous Phosphosites", "List of the sites where a phosphorylation was ambiguously localized.", false),
    ambiguous_phosphosites_number("#Ambiguous Phosphosites", "Number of ambiguously localized phosphorylations.", false),
    psms("#PSMs", "Number of PSMs.", false),
    validated_psms("#Validated PSMs", "Number of validated PSMs.", false),
    raw_score("Raw Score", "Peptide score before log transform.", true),
    score("Score", "Score of the peptide.", true),
    confidence("Confidence [%]", "Confidence in percent associated to the peptide.", false),
    decoy("Decoy", "Indicates whether the peptide is a decoy (1: yes, 0: no).", false),
    validated("Validation", "Indicates the validation level of the peptide.", false),
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
    public final static String type = "Peptide Identification Summary";
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
    private PsPeptideFeature(String title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = new ArrayList<>();
        result.addAll(Arrays.asList(values()));
        if (includeSubFeatures) {
            result.addAll(PsPsmFeature.values()[0].getExportFeatures(includeSubFeatures));
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
