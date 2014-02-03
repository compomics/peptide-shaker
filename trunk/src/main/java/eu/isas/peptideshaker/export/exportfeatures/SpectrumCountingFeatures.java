package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;

/**
 * This class lists all the export features related to the spectrum counting.
 *
 * @author Marc Vaudel
 */
public enum SpectrumCountingFeatures implements ExportFeature {

    method("Method", "The method used to establish the spectrum counting index."),
    validated("Validated Matches Only", "Indicates whether only validated matches were used to establis the spectrum counting metric.");
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
    public final static String type = "Spectrum Counting Parameters";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private SpectrumCountingFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.add(method);
        result.add(validated);
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
