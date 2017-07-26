package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the annotation export features.
 *
 * @author Marc Vaudel
 */
public enum PsAnnotationFeature implements ExportFeature {

    intensity_limit("Intensity Limit", "The intensity threshold for considering a peak (in percentile of the intensities in the spectrum).", false),
    automatic_annotation("Automatic Annotation", "Indicates whether the PeptideShaker automated peak annotation was used (1: yes, 0: no).", false),
    selected_ions("Selected Ions", "Indicates the ion types selected for peak annotation.", false),
    neutral_losses("Neutral Losses", "Indicates the neutral losses selected for peak annotation.", false),
    neutral_losses_sequence_dependence("Neutral Losses Sequence Dependence", "Indicates whether neutral losses consideration is sequence dependent (1: yes, 0: no).", false),
    fragment_ion_accuracy("Fragment Ion m/z Tolerance", "The m/z tolerance used for fragment ion annotation.", false);

    /**
     * The title of the feature which will be used for column heading.
     */
    private String title;
    /**
     * The description of the feature.
     */
    private String description;
    /**
     * Indicates whether a feature is for advanced user only.
     */
    private final boolean advanced;
    /**
     * The type of export feature.
     */
    public final static String type = "Annotation Settings";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param advanced indicates whether a feature is for advanced user only
     */
    private PsAnnotationFeature(String title, String description, boolean advanced) {
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
