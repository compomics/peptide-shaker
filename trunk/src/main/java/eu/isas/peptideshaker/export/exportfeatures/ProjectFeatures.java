package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class lists the PSM identification features.
 *
 * @author Marc Vaudel
 */
public enum ProjectFeatures implements ExportFeature, Serializable {

    peptide_shaker("PeptideShaker Version", "Software version used to create the project."),
    date("Date", "Date of project creation."),
    experiment("Experiment", "Experiment name."),
    sample("Sample", "Sample name."),
    replicate("Replicate Number", "Replicate number."),
    search_engines("Search Engines", "The search engines used.");
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
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private ProjectFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.add(peptide_shaker);
        result.add(date);
        result.add(experiment);
        result.add(sample);
        result.add(replicate);
        result.add(search_engines);
        return result;
    }

    @Override
    public String getTitle(String separator) {
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
