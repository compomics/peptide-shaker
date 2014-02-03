package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;

/**
 * This class lists the fragment identification features.
 *
 * @author Marc Vaudel
 */
public enum FragmentFeatures implements ExportFeature {

    annotation("Peak Annotation", "The fragment annotation as it would appear in the GUI."),
    fragment_type("Type", "The type of fragment ion, for example 'Peptide Fragment Ion'."),
    fragment_subType("Subtype", "The subtype of fragment if existing, for example 'b ion'."),
    fragment_number("Number", "The fragment ion number, for example '5' for b5."),
    fragment_losses("Neutral losses", "The fragment ion neutral losses, for example '-H2O' for b5-H2O."),
    fragment_name("Name", "The name of the fragment ion, for example b5."),
    fragment_charge("Fragment Charge", "The charge of the fragment ion according to the identification process."),
    theoretic_mz("Theoretic m/z", "The theoretic m/z of the fragment ion."),
    mz("m/z", "The m/z of the peak."),
    intensity("Intensity", "The intensity of the peak."),
    error_Da("m/z Error (Da)", "The absolute m/z error."),
    error_ppm("m/z Error (ppm)", "The relative m/z error (in ppm).");
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
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private FragmentFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.add(annotation);
        result.add(fragment_type);
        result.add(fragment_subType);
        result.add(fragment_number);
        result.add(fragment_losses);
        result.add(fragment_name);
        result.add(fragment_charge);
        result.add(theoretic_mz);
        result.add(mz);
        result.add(intensity);
        result.add(error_Da);
        result.add(error_ppm);
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
