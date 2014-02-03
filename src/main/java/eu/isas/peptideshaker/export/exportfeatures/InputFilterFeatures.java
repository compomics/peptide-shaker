package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;

/**
 * This enum lists the export features related to the import features.
 *
 * @author Marc
 */
public enum InputFilterFeatures implements ExportFeature {

    min_peptide_length("Minimal Peptide Length", "The minimal peptide length."),
    max_peptide_length("Maximal Peptide Length", "The maximal peptide length."),
    mascot_max_evalue("Mascot Maximal E-value", "The maximal e-value allowed for Mascot Peptide Spectrum Matches (PSMs)."),
    omssa_max_evalue("OMSSA Maximal E-value", "The maximal e-value allowed for OMSSA Peptide Spectrum Matches (PSMs)."),
    xtandem_max_evalue("X!Tandem Maximal E-value", "The maximal e-value allowed for X!Tandem Peptide Spectrum Matches (PSMs)."),
    max_mz_deviation("Precursor m/z Tolerance", "The maximal precursor m/z error tolerance allowed."),
    max_mz_deviation_unit("Precursor m/z Tolerance Unit", "The unit of the maximal precursor m/z error tolerance allowed."),
    unknown_PTM("Unrecognized Modifications Discarded", "Indicates whether the Peptide Spectrum Matches (PSMs) presenting PTMs which do not match the search parameters were discarded.");
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
    public final static String type = "Input Filters";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private InputFilterFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.add(min_peptide_length);
        result.add(max_peptide_length);
        result.add(mascot_max_evalue);
        result.add(omssa_max_evalue);
        result.add(xtandem_max_evalue);
        result.add(max_mz_deviation);
        result.add(max_mz_deviation_unit);
        result.add(unknown_PTM);
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
