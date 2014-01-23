package eu.isas.peptideshaker.export.exportfeatures;

import eu.isas.peptideshaker.export.ExportFeature;
import static eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures.missed_cleavages;
import java.util.ArrayList;

/**
 * This class lists the PSM identification features.
 *
 * @author Marc Vaudel
 */
public enum PsmFeatures implements ExportFeature {

    accessions("Protein(s)", "Protein(s) to which the peptide can be attached."),
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
    score("Score", "Score of the retained peptide as a combination of the algorithms scores (used to rank PSMs)."),
    confidence("Confidence", "Confidence in percent associated to the peptide."),
    decoy("Decoy", "Indicates whether the peptide is a decoy (1: yes, 0: no)."),
    validated("Validated", "Indicates whether the peptide passed the validation process (1: yes, 0: no)."),
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
        result.add(accessions);
        result.add(sequence);
        result.add(missed_cleavages);
        result.add(modified_sequence);
        result.add(variable_ptms);
        result.add(fixed_ptms);
        result.add(localization_confidence);
        result.add(probabilistic_score);
        result.add(d_score);
        result.add(spectrum_file);
        result.add(spectrum_title);
        result.add(spectrum_scan_number);
        result.add(rt);
        result.add(mz);
        result.add(total_spectrum_intensity);
        result.add(max_intensity);
        result.add(spectrum_charge);
        result.add(identification_charge);
        result.add(theoretical_mass);
        result.add(isotope);
        result.add(mz_error);
        result.add(score);
        result.add(confidence);
        result.add(decoy);
        result.add(validated);
        result.add(starred);
        result.add(hidden);
        result.addAll(FragmentFeatures.values()[0].getExportFeatures());
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
