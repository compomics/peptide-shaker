package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the PSM identification features.
 *
 * @author Marc Vaudel
 */
public enum PsProjectFeature implements ExportFeature, Serializable {

    peptide_shaker("PeptideShaker Version", "Software version used to create the project.", false),
    date("Date", "Date of project creation.", false),
    experiment("Experiment", "Experiment name.", false),
    sample("Sample", "Sample name.", false),
    replicate("Replicate Number", "Replicate number.", false),
    identification_algorithms("Identification Algorithms", "The identification algorithms used.", false),
    algorithms_versions("Identification Algorithms Version", "The identification algorithms used with version number.", false);

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
    public final static String type = "Project Details";
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
    private PsProjectFeature(String title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = new ArrayList<>();
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
