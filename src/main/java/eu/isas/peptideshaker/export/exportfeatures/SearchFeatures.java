package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import static eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures.values;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the export features linked to the spectrum identification.
 *
 * @author Marc Vaudel
 */
public enum SearchFeatures implements ExportFeature {

    precursor_accuracy_unit("Precursor Accuracy Unit", "Unit of the precursor accuracy: ppm or Da.", false),
    precursor_tolerance("Precursor Ion m/z Tolerance", "Precursor ion m/z tolerance used for the search.", false),
    fragment_tolerance("Fragment Ion m/z Tolerance", "Fragment ion m/z tolerance used for the search.", false),
    enzyme("Enzyme", "Enzyme used for the search.", false),
    mc("Number of Missed Cleavages", "The number of missed cleavages.", false),
    database("Database", "The protein sequence database.", false),
    forward_ion("Forward Ion", "The forward ion type searched for.", false),
    rewind_ion("Rewind Ion", "The rewind ion type searched for.", false),
    fixed_modifications("Fixed Modifications", "The fixed posttranslational modifications used for the search.", false),
    variable_modifications("Variable Modifications", "The variable posttranslational modifications used for the search.", false),
    refinement_variable_modifications("Refinement Variable Modifications", "The refinement variable posttranslational modifications used for the search, typically a second pass search.", false),
    refinement_fixed_modifications("Refinement Fixed Modifications", "The refinement fixed posttranslational modifications used for the search, typically a second pass search.", false);
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
    public final static String type = "Database Search Parameters";
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
    private SearchFeatures(String title, String description, boolean advanced) {
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
