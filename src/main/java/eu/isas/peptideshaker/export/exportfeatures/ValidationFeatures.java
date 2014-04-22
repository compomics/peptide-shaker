package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This enum lists the export features linked to the validation process.
 *
 * @author Marc Vaudel
 */
public enum ValidationFeatures implements ExportFeature {

    validated_protein("#Validated Proteins", "The number of validated proteins."),
    total_protein("Protein Total", "The estimated total number of proteins."),
    protein_fdr("Protein FDR Limit", "The estimated protein False Discovery Rate (FDR)."),
    protein_fnr("Protein FNR Limit", "The estimated protein False Negative Rate (FNR)."),
    protein_confidence("Protein Confidence Limit", "The lowest confidence among validated proteins."),
    protein_pep("Protein PEP Limit", "The highest Posterior Error Probability (PEP) among validated proteins."),
    protein_accuracy("Protein Confidence Accuracy", "The estimated protein Posterior Error Probability (PEP) and confidence estimation accuracy."),
    validated_peptide("#Validated Peptides", "The number of validated peptides. Note that peptides are grouped by modification status when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    total_peptide("Peptide Total", "The estimated total number of peptides. Note that peptides are grouped by modification status when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    peptide_fdr("Peptide FDR Limit", "The estimated peptide False Discovery Rate (FDR). Note that peptides are grouped by modification status when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    peptide_fnr("Peptide FNR Limit", "The estimated peptide False Negative Rate (FNR). Note that peptides are grouped by modification status when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    peptide_confidence("Peptide Confidence Limit", "The lowest confidence among validated peptides. Note that peptides are grouped by modification status when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    peptide_pep("Peptide PEP Limit", "The highest Posterior Error Probability (PEP) among validated peptides. Note that peptides are grouped by modification status when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    peptide_accuracy("Peptide Confidence Accuracy", "The estimated peptide Posterior Error Probability (PEP) and confidence estimation accuracy. Note that peptides are grouped by modification status when statistical significance is ensured based on this parameter: \"Confidence accuracy\" < 1%."),
    validated_psm("#Validated PSM", "The number of validated Peptide Spectrum Matches (PSMs). Note that PSMs are grouped by identified charge when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    total_psm("PSM Total", "The estimated total number of Peptide Spectrum Matches (PSMs). Note that PSMs are grouped by identified charge when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    psm_fdr("PSM FDR Limit", "The estimated Peptide Spectrum Match (PSM) False Discovery Rate (FDR). Note that PSMs are grouped by identified charge when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    psm_fnr("PSM FNR Limit", "The estimated Peptide Spectrum Match (PSM) False Negative Rate (FNR). Note that PSMs are grouped by identified charge when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    psm_confidence("PSM Confidence Limit", "The lowest confidence among validated Peptide Spectrum Matches (PSMs). Note that PSMs are grouped by identified charge when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    psm_pep("PSM PEP Limit", "The highest Posterior Error Probability (PEP) among validated Peptide Spectrum Matches (PSMs). Note that PSMs are grouped by identified charge when statistical significance is ensured, i.e. \"Confidence accuracy\" < 1%."),
    psm_accuracy("PSM Confidence Accuracy", "The estimated Peptide Spectrum Match (PSM) Posterior Error Probability (PEP) and confidence estimation accuracy. Note that PSMs are grouped by identified charge when statistical significance is ensured based on this parameter: \"Confidence accuracy\" < 1%.");
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
    public final static String type = "Target/Decoy Validation Summary";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private ValidationFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.addAll(Arrays.asList(values()));
        return result;
    }

    @Override
    public String[] getTitles() {
        return new String[]{title};
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
