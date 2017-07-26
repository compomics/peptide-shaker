package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the PSM identification features.
 *
 * @author Marc Vaudel
 */
public enum PsPsmFeature implements ExportFeature {

    protein_groups("Protein Group(s)", "List of identified protein groups the peptide of this PSM can map to with associated validation level.", true),
    best_protein_group_validation("Protein Validation", "Best validation status among the protein groups derived from the peptide of this PSM.", true),
    localization_confidence("Localization Confidence", "The confidence in variable PTM localization.", false),
    probabilistic_score("Probabilistic PTM score", "The probabilistic score (e.g. A-score or PhosphoRS) used for variable PTM localization.", false),
    d_score("D-score", "D-score for variable PTM localization.", false),
    confident_modification_sites("Confidently Localized Modification Sites", "List of the sites where a variable modification was confidently localized.", false),
    confident_modification_sites_number("# Confidently Localized Modification Sites", "Number of sites where a variable modification was confidently localized.", false),
    ambiguous_modification_sites("Ambiguously Localized Modification Sites", "List of the sites where ambiguously localized variable modification could possibly be located.", false),
    ambiguous_modification_sites_number("#Ambiguously Localized Modification Sites", "Number of ambiguously localized modifications.", false),
    confident_phosphosites("Confident Phosphosites", "List of the sites where a phosphorylation was confidently localized.", false),
    confident_phosphosites_number("#Confident Phosphosites", "Number of confidently localized phosphorylations.", false),
    ambiguous_phosphosites("Ambiguous Phosphosites", "List of the sites where a phosphorylation was ambiguously localized.", false),
    ambiguous_phosphosites_number("#Ambiguous Phosphosites", "Number of ambiguously localized phosphorylations.", false),
    algorithm_score("Algorithm Score", "Best score given by the identification algorithm to the hit retained by PeptideShaker independent of modification localization.", false),
    score("Score", "Score of the retained PSM as a combination of the algorithm scores (used to rank PSMs).", true),
    raw_score("Raw score", "Score before log transformation.", true),
    confidence("Confidence [%]", "Confidence in percent associated to the retained PSM.", false),
    validated("Validation", "Indicates the validation level of the retained PSM.", false),
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
    public final static String type = "Peptide Spectrum Matching Summary";
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
    private PsPsmFeature(String title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = new ArrayList<>();
        result.addAll(PsIdentificationAlgorithmMatchesFeature.values()[0].getExportFeatures(includeSubFeatures));
        result.addAll(Arrays.asList(values()));
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
