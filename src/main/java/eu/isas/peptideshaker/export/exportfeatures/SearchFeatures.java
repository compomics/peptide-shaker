package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;

/**
 * This class lists the export features linked to the spectrum identification.
 *
 * @author Marc Vaudel
 */
public enum SearchFeatures implements ExportFeature {

    precursor_accuracy_unit("Precursor Accuracy Unit", "Unit of the precursor accuracy: ppm or Da."),
    precursor_tolerance("Precursor Ion m/z Tolerance", "Precursor ion m/z tolerance used for the search."),
    fragment_tolerance("Fragment Ion m/z Tolerance", "Fragment ion m/z tolerance used for the search."),
    enzyme("Enzyme", "Enzyme used for the search."),
    mc("Number of Missed Cleavages", "The number of missed cleavages."),
    database("Database", "The protein sequence database."),
    forward_ion("Forward Ion", "The forward ion type searched for."),
    rewind_ion("Rewind Ion", "The rewind ion type searched for."),
    fixed_modifications("Fixed Modifications", "The fixed posttranslational modifications used for the search."),
    variable_modifications("Variable Modifications", "The variable posttranslational modifications used for the search."),
    refinement_variable_modifications("Refinement Variable Modifications", "The refinement variable posttranslational modifications used for the search, typically a second pass search."),
    refinement_fixed_modifications("Refinement Fixed Modifications", "The refinement fixed posttranslational modifications used for the search, typically a second pass search.");
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
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private SearchFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.add(precursor_accuracy_unit);
        result.add(precursor_tolerance);
        result.add(fragment_tolerance);
        result.add(enzyme);
        result.add(mc);
        result.add(database);
        result.add(forward_ion);
        result.add(rewind_ion);
        result.add(fixed_modifications);
        result.add(variable_modifications);
        result.add(refinement_variable_modifications);
        result.add(refinement_fixed_modifications);
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
}
