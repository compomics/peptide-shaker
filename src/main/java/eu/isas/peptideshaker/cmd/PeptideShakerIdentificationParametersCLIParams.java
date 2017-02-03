package eu.isas.peptideshaker.cmd;

import com.compomics.cli.identification_parameters.IdentificationParametersCLIParams;
import static com.compomics.software.cli.CommandLineUtils.formatter;
import org.apache.commons.cli.Options;

/**
 * This class provides the parameters which can be used for the identification
 * parameters cli in PeptideShaker.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerIdentificationParametersCLIParams {

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {
        for (IdentificationParametersCLIParams identificationParametersCLIParams : IdentificationParametersCLIParams.values()) {
            aOptions.addOption(identificationParametersCLIParams.id, identificationParametersCLIParams.hasArgument, identificationParametersCLIParams.description);
        }

        // Path setup
        PathSettingsCLIParams.createOptionsCLI(aOptions);

        //@TODO: Add QC filters?
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";

        output += "Parameters Files:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OUT.id) + " " + IdentificationParametersCLIParams.OUT.description + ". (Mandatory)\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.id) + " " + IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.description + " (Optional)\n";
        output += getParametersOptionsAsString();
        return output;
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getParametersOptionsAsString() {

        String output = "";
        String formatter = "%-35s";

        output += "\n\nSpectrum Matching:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DB.id) + " " + IdentificationParametersCLIParams.DB.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PREC_TOL.id) + " " + IdentificationParametersCLIParams.PREC_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PREC_PPM.id) + " " + IdentificationParametersCLIParams.PREC_PPM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FRAG_TOL.id) + " " + IdentificationParametersCLIParams.FRAG_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ENZYME.id) + " " + IdentificationParametersCLIParams.ENZYME.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FIXED_MODS.id) + " " + IdentificationParametersCLIParams.FIXED_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.VARIABLE_MODS.id) + " " + IdentificationParametersCLIParams.VARIABLE_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MIN_CHARGE.id) + " " + IdentificationParametersCLIParams.MIN_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MAX_CHARGE.id) + " " + IdentificationParametersCLIParams.MAX_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MC.id) + " " + IdentificationParametersCLIParams.MC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FI.id) + " " + IdentificationParametersCLIParams.FI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.RI.id) + " " + IdentificationParametersCLIParams.RI.description + "\n";

        output += "\n\nSpectrum Annotation:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_LEVEL.id) + " " + IdentificationParametersCLIParams.ANNOTATION_LEVEL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_MZ_TOLERANCE.id) + " " + IdentificationParametersCLIParams.ANNOTATION_MZ_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_HIGH_RESOLUTION.id) + " " + IdentificationParametersCLIParams.ANNOTATION_HIGH_RESOLUTION.description + "\n";

        output += "\n\nSequence Matching:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SEQUENCE_MATCHING_TYPE.id) + " " + IdentificationParametersCLIParams.SEQUENCE_MATCHING_TYPE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SEQUENCE_MATCHING_X.id) + " " + IdentificationParametersCLIParams.SEQUENCE_MATCHING_X.description + "\n";

        output += "\n\nImport Filters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MIN.id) + " " + IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MIN.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MAX.id) + " " + IdentificationParametersCLIParams.IMPORT_PEPTIDE_LENGTH_MAX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_MC_MIN.id) + " " + IdentificationParametersCLIParams.IMPORT_MC_MIN.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_MC_MAX.id) + " " + IdentificationParametersCLIParams.IMPORT_MC_MAX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ.id) + " " + IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ_PPM.id) + " " + IdentificationParametersCLIParams.IMPORT_PRECURSOR_MZ_PPM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.EXCLUDE_UNKNOWN_PTMs.id) + " " + IdentificationParametersCLIParams.EXCLUDE_UNKNOWN_PTMs.description + "\n";

        output += "\n\nPTM Localization:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_SCORE.id) + " " + IdentificationParametersCLIParams.PTM_SCORE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_THRESHOLD.id) + " " + IdentificationParametersCLIParams.PTM_THRESHOLD.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SCORE_NEUTRAL_LOSSES.id) + " " + IdentificationParametersCLIParams.SCORE_NEUTRAL_LOSSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PTM_SEQUENCE_MATCHING_TYPE.id) + " " + IdentificationParametersCLIParams.PTM_SEQUENCE_MATCHING_TYPE.description + "\n";

        output += "\n\nGene Annotation:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.USE_GENE_MAPPING.id) + " " + IdentificationParametersCLIParams.USE_GENE_MAPPING.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.UPDATE_GENE_MAPPING.id) + " " + IdentificationParametersCLIParams.UPDATE_GENE_MAPPING.description + "\n";

        output += "\n\nProtein Inference:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DB_PI.id) + " " + IdentificationParametersCLIParams.DB_PI.description + "\n";

        output += "\n\nValidation Levels:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PSM_FDR.id) + " " + IdentificationParametersCLIParams.PSM_FDR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPTIDE_FDR.id) + " " + IdentificationParametersCLIParams.PEPTIDE_FDR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PROTEIN_FDR.id) + " " + IdentificationParametersCLIParams.PROTEIN_FDR.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SEPARATE_PSMs.id) + " " + IdentificationParametersCLIParams.SEPARATE_PSMs.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.SEPARATE_PEPTIDES.id) + " " + IdentificationParametersCLIParams.SEPARATE_PEPTIDES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MERGE_SUBGROUPS.id) + " " + IdentificationParametersCLIParams.MERGE_SUBGROUPS.description + "\n";

        output += "\n\nFraction Analysis:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PROTEIN_FRACTION_MW_CONFIDENCE.id) + " " + IdentificationParametersCLIParams.PROTEIN_FRACTION_MW_CONFIDENCE.description + "\n";

//        output += "\n\nQuality Control:\n\n";
//        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ANNOTATION_LEVEL.id) + IdentificationParametersCLIParams.ANNOTATION_LEVEL.description + "\n";
        output += "\n\nHelp:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MODS.id) + IdentificationParametersCLIParams.MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.USAGE.id) + IdentificationParametersCLIParams.USAGE.description + "\n";

        return output;
    }
}
