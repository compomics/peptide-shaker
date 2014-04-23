package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import static eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures.values;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the PSM identification features.
 *
 * @author Marc Vaudel
 */
public enum PsmFeatures implements ExportFeature {

    accessions("Protein(s)", "Protein(s) to which the peptide can be attached."),
    protein_description("Description(s)", "Description of the Protein(s) to which this peptide can be attached."),
    sequence("Sequence", "Sequence of the peptide."),
    missed_cleavages("Missed Cleavages", "The number of missed cleavages."),
    modified_sequence("Modified Sequence", "The peptide sequence annotated with variable modifications."),
    variable_ptms("Variable Modifications", "The variable modifications."),
    fixed_ptms("Fixed Modifications", "The fixed modifications."),
    localization_confidence("Localization Confidence", "The confidence in variable PTM localization."),
    probabilistic_score("probabilistic PTM score", "The probabilistic score (e.g. A-score or PhosphoRS) used for variable PTM localization."),
    d_score("D-score", "D-score for variable PTM localization."),
    spectrum_file("Spectrum File", "The spectrum file."),
    spectrum_title("Spectrum Title", "The title of the spectrum."),
    spectrum_scan_number("Spectrum Scan Number", "The spectrum scan number."),
    rt("RT", "Retention time"),
    mz("m/z", "Measured m/z"),
    spectrum_charge("Measured Charge", "The charge as given in the spectrum file."),
    total_spectrum_intensity("Total Spectrum Intensity", "The summed intensity of all peaks in the spectrum."),
    max_intensity("Maximal Spectrum Intensity", "The maximal intensity found in the spectrum."),
    identification_charge("Identification Charge", "The charge as inferred by the search engine."),
    theoretical_mass("Theoretical Mass", "The theoretical mass of the peptide."),
    isotope("Isotope Number", "The isotope number targetted by the instrument."),
    mz_error("Precursor m/z Error", "The precursor m/z matching error."),
    algorithm_score("Algorithm Score", "Best score given by the identification algorithm to the hit retained by PeptideShaker independent of modification localization."),
    score("Score", "Score of the retained peptide as a combination of the algorithm scores (used to rank PSMs)."),
    confidence("Confidence", "Confidence in percent associated to the retained PSM."),
    decoy("Decoy", "Indicates whether the peptide is a decoy (1: yes, 0: no)."),
    validated("Validation", "Indicates the validation level of the protein group."),
    starred("Starred", "Indicates whether the match was starred in the interface (1: yes, 0: no)."),
    hidden("Hidden", "Indicates whether the match was hidden in the interface (1: yes, 0: no).");
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
    public final static String type = "Peptide Spectrum Matching Summary";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private PsmFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = new ArrayList<ExportFeature>();
        result.addAll(Arrays.asList(values()));
        result.addAll(FragmentFeatures.values()[0].getExportFeatures());
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
