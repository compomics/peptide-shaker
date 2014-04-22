package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import static eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures.values;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the annotation export features.
 *
 * @author Marc Vaudel
 */
public enum AnnotationFeatures implements ExportFeature {

    intensity_limit("Intensity Limit", "The intensity threshold for considering a peak (in percentile of the intensities in the spectrum)."),
    automatic_annotation("Automatic Annotation", "Indicates whether the PeptideShaker automated peak annotation was used (1: yes, 0: no)."),
    selected_ions("Selected Ions", "Indicates the ion types selected for peak annotation."),
    neutral_losses("Neutral Losses", "Indicates the neutral losses selected for peak annotation."),
    neutral_losses_sequence_dependence("Neutral Losses Sequence Dependence", "Indicates whether neutral losses consideration is sequence dependent (1: yes, 0: no)."),
    selected_charges("Selected Charges", "The charges selected for fragment ion peak annotation."),
    fragment_ion_accuracy("Fragment Ion m/z Tolerance", "The m/z tolerance used for fragment ion annotation.");
    /**
     * The title of the feature which will be used for column heading.
     */
    private String title;
    /**
     * The description of the feature.
     */
    private String description;
    /**
     * The type of export feature.
     */
    public final static String type = "Annotation Settings";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private AnnotationFeatures(String title, String description) {
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
