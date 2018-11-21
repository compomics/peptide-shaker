package eu.isas.peptideshaker.export.exportfeatures;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class lists the Algorithm identification features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum PsIdentificationAlgorithmMatchesFeature implements ExportFeature {

    rank("Rank", "The rank assigned by the identification algorithm.", false),
    accessions("Protein(s)", "Protein(s) to which the peptide can be attached.", false),
    protein_description("Description(s)", "Description of the Protein(s) to which this peptide can be attached.", false),
    sequence("Sequence", "The identified sequence of amino acids.", false),
    aaBefore("AAs Before", "The amino acids before the sequence.", false),
    aaAfter("AAs After", "The amino acids after the sequence.", false),
    position("Position", "Position of the peptide in the protein sequence(s).", false),
    missed_cleavages("Missed Cleavages", "The number of missed cleavages.", false),
    modified_sequence("Modified Sequence", "The amino acids sequence annotated with variable modifications.", false),
    variable_ptms("Variable Modifications", "The variable modifications.", false),
    fixed_ptms("Fixed Modifications", "The fixed modifications.", false),
    spectrum_file("Spectrum File", "The spectrum file.", false),
    spectrum_title("Spectrum Title", "The title of the spectrum.", false),
    spectrum_scan_number("Spectrum Scan Number", "The spectrum scan number.", false),
    spectrum_array_list("Spectrum Array List", "The peaks in the spectrum as an array list.", false),
    rt("RT", "Retention time as provided in the spectrum file.", false),
    mz("m/z", "Measured m/z.", false),
    spectrum_charge("Measured Charge", "The charge as given in the spectrum file.", false),
    total_spectrum_intensity("Total Spectrum Intensity", "The summed intensity of all peaks in the spectrum.", true),
    intensity_coverage("Intensity Coverage [%]", "Annotated share of the total spectrum intensity.", true),
    max_intensity("Maximal Spectrum Intensity", "The maximal intensity found in the spectrum.", true),
    identification_charge("Identification Charge", "The charge as inferred by the search engine.", false),
    theoretical_mass("Theoretical Mass", "The theoretical mass of the peptide.", false),
    isotope("Isotope Number", "The isotope number targetted by the instrument.", false),
    mz_error_ppm("Precursor m/z Error [ppm]", "The precursor m/z matching error in ppm.", false),
    mz_error_da("Precursor m/z Error [Da]", "The precursor m/z matching error in Da.", false),
    algorithm_score("Algorithm Score", "Score given by the identification algorithm to the hit.", false),
    algorithm_confidence("Algorithm Confidence [%]", "Confidence in percent associated to the algorithm score.", false),
    algorithm_delta_confidence("Algorithm Delta Confidence [%]", "Difference in percent between the match and the next best for a given identification algorithm without accounting for PTM localization.", true),
    delta_confidence("Delta Confidence [%]", "Difference in percent between the match and the next best across all search engines without accounting for PTM localization.", true),
    fragment_mz_accuracy_score("Fragment m/z accuracy score", "Score reflecting the accuracy of the fragment ions m/z.", true),
    intensity_score("Intensity score", "Score reflecting the coverage of the spectrum in intensity.", true),
    sequence_coverage("Sequence Coverage [%]", "Coverage of the amino acid sequence by the annotated fragment ions in percent.", true),
    longest_amino_acid_sequence_annotated("Longest amino acid sequence annotated", "Longest consecutive series of amino acid annotated on the spectrum.", true),
    longest_amino_acid_sequence_annotated_single_serie("Single ion longest amino acid sequence annotated", "Longest consecutive series of amino acid annotated on the spectrum by a single type of ions of charge 1 without neutral losses.", true),
    amino_acids_annotated("Amino Acids Annotated", "Amino acid sequence annotated on the spectrum.", true),
    decoy("Decoy", "Indicates whether the peptide is a decoy (1: yes, 0: no).", false),
    validated("Validation", "Indicates the validation level of the protein group.", false),
    starred("Starred", "Indicates whether the match was starred in the interface (1: yes, 0: no).", false),
    hidden("Hidden", "Indicates whether the match was hidden in the interface (1: yes, 0: no).", false);

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
    public final static String type = "Identification Algorithm Results";
    /**
     * Indicates whether a feature is for advanced user only.
     */
    private final boolean advanced;

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param advanced indicates whether a feature is for advanced user only
     */
    private PsIdentificationAlgorithmMatchesFeature(String title, String description, boolean advanced) {
        this.title = title;
        this.description = description;
        this.advanced = advanced;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = new ArrayList<>();
        result.addAll(Arrays.asList(values()));
        if (includeSubFeatures) {
            result.addAll(PsFragmentFeature.values()[0].getExportFeatures(includeSubFeatures));
        }
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
