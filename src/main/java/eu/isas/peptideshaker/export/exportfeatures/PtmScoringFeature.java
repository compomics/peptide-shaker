package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This enum groups the export features related to PTM scoring.
 *
 * @author Marc Vaudel
 */
public enum PtmScoringFeature implements ExportFeature {

    aScore("A-score", "Indicates whether the A-score was computed for PTM localization.", false),
    neutral_losses("Accounting for Neutral Losses", "Indicates whether the neutral losses are accounted for in the A-score calculation.", false),
    flr("False Location Rate", "For peptides presenting a single modification of a kind and more than one modification site, the site is marked as confident if the A-score passes this estimated FLR.", false);
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
    public final static String type = "Postranslational Modification Scoring Settings";
    /**
     * Indicates whether a feature is for advanced user only.
     */
    private boolean advanced;

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param advanced indicates whether a feature is for advanced user only
     */
    private PtmScoringFeature(String title, String description, boolean advanced) {
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
