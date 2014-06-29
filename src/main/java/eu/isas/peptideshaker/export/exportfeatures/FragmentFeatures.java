package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import static eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures.values;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the fragment identification features.
 *
 * @author Marc Vaudel
 */
public enum FragmentFeatures implements ExportFeature {

    annotation("Peak Annotation", "The fragment annotation as it would appear in the GUI.", false),
    fragment_type("Type", "The type of fragment ion, for example 'Peptide Fragment Ion'.", false),
    fragment_subType("Subtype", "The subtype of fragment if existing, for example 'b ion'.", false),
    fragment_number("Number", "The fragment ion number, for example '5' for b5.", false),
    fragment_losses("Neutral losses", "The fragment ion neutral losses, for example '-H2O' for b5-H2O.", false),
    fragment_name("Name", "The name of the fragment ion, for example b5.", false),
    fragment_charge("Fragment Charge", "The charge of the fragment ion according to the identification process.", false),
    theoretic_mz("Theoretic m/z", "The theoretic m/z of the fragment ion.", false),
    mz("m/z", "The m/z of the peak.", false),
    intensity("Intensity", "The intensity of the peak.", false),
    error_Da("m/z Error (Da)", "The absolute m/z error.", false),
    error_ppm("m/z Error (ppm)", "The relative m/z error (in ppm).", false);
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
    public final static String type = "Fragment Ions Summary";
    /**
     * indicates whether a feature is for advanced user only
     */
    private boolean advanced;

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param advanced indicates whether a feature is for advanced user only
     */
    private FragmentFeatures(String title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
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

    @Override
    public boolean isAdvanced() {
        return advanced;
    }
}
