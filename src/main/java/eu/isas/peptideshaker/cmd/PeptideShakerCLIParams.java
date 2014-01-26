package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.identification.ptm.PtmScore;
import org.apache.commons.cli.Options;

/**
 * Enum class specifying the Command Line Parameters for PeptideShaker.
 *
 * @author Kenny Helsens
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum PeptideShakerCLIParams {

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // IMPORTANT: Any change here must be reported in the wiki: http://code.google.com/p/peptide-shaker/wiki/PeptideShakerCLI
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    EXPERIMENT("experiment", "Specifies the experiment name.", true),
    SAMPLE("sample", "Specifies the sample name.", true),
    REPLICATE("replicate", "The replicate number.", true),
    SPECTRUM_FILES("spectrum_files", "Spectrum files (mgf format), comma separated list or an entire folder.", true),
    IDENTIFICATION_FILES("identification_files", "Identification files (.t.xml, .omx or .dat), comma separated list or an entire folder.", true),
    PEPTIDESHAKER_OUTPUT("out", "PeptideShaker output file. Note: if file exists it will be overwritten.", true),
    PEPTIDESHAKER_TXT_1("out_txt_1", "Output folder for text summary - format 1 (three files: proteins, peptides and psms, soon deprecated).", false),
    //PEPTIDESHAKER_TXT_2("out_txt_2", "Output folder for text summary - format 2 (one file: proteins and peptides). (Not yet implemented and will most likely not be implemented)", false),
    //PEPTIDESHAKER_PRIDE("out_pride", "PeptideShaker PRIDE XML output file. (Not yet implemented)", false),
    PSM_FDR("psm_FDR", "FDR at the PSM level (default 1% FDR: '1').", false),
    PEPTIDE_FDR("peptide_FDR", "FDR at the peptide level (default 1% FDR: '1').", false),
    PROTEIN_FDR("protein_FDR", "FDR at the protein level (default 1% FDR: '1').", false),
    IDENTIFICATION_PARAMETERS("id_params", "The identification parameters file. Generated using SeachGUI or via IdentificationParametersCLI.", false),
    GUI("gui", "Use a dialog to display the progress (1: true, 0: false, default is '0').", false),
    PTM_SCORE("ptm_score", "The PTM probabilistic score to use for PTM localization. " + PtmScore.getCommandLineOptions() + ".", false),
    PTM_THRESHOLD("ptm_threshold", "The threshold to use for the PTM scores. Automatic mode will be used if not set.", false),
    SCORE_NEUTRAL_LOSSES("score_neutral_losses", "Include neutral losses in spectrum annotation of the PTM score (1: true, 0: false, default is '0').", false),
    PROTEIN_FRACTION_MW_CONFIDENCE("protein_fraction_mw_confidence", "Minimum confidence required for a protein in the fraction MW plot (default 95%: '95.0').", false),
    MIN_PEPTIDE_LENGTH("min_peptide_length", "Minimim peptide length filter (default is '6').", false),
    MAX_PEPTIDE_LENGTH("max_peptide_length", "Maximum peptide length filter (default is '30').", false),
    MASCOT_E_VALUE_MAX("max_mascot_e", "Maximum Mascot E-value filter (default '100').", false),
    OMSSA_E_VALUE_MAX("max_omssa_e", "Maximum OMSSA E-value filter (default '100').", false),
    XTANDEM_E_VALUE_MAX("max_xtandem_e", "Maximum X!Tandem E-value filter (default '100').", false),
    MAX_PRECURSOR_ERROR("max_precursor_error", "Maximum precursor error filter (default '10'). See also max_precursor_error_type.", false),
    MAX_PRECURSOR_ERROR_TYPE("max_precursor_error_type", "Maximum precursor error type (0: ppm, 1: Da, default is '0'). See also max_precursor_error.", false),
    EXCLUDE_UNKNOWN_PTMS("exclude_unknown_ptms", "Exclude unknown PTMs (1: true, 0: false, default is '1').", false),
    SPECIES("species", "The species to use for the gene annotation. Supported species are listed in the GUI.", false),
    SPECIES_TYPE("species_type", "The species type to use for the gene annotation. Supported species types are listed in the GUI.", false);
    /**
     * Short Id for the CLI parameter.
     */
    public String id;
    /**
     * Explanation for the CLI parameter.
     */
    public String description;
    /**
     * Boolean indicating whether the parameter is mandatory.
     */
    public boolean mandatory;

    /**
     * Private constructor managing the various variables for the enum
     * instances.
     *
     * @param id the id
     * @param description the description
     * @param mandatory is the parameter mandatory
     */
    private PeptideShakerCLIParams(String id, String description, boolean mandatory) {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
    }

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {

        // Standard options
        aOptions.addOption(EXPERIMENT.id, true, EXPERIMENT.description);
        aOptions.addOption(SAMPLE.id, true, SAMPLE.description);
        aOptions.addOption(REPLICATE.id, true, REPLICATE.description);
        aOptions.addOption(SPECTRUM_FILES.id, true, SPECTRUM_FILES.description);
        aOptions.addOption(IDENTIFICATION_FILES.id, true, IDENTIFICATION_FILES.description);
        aOptions.addOption(PEPTIDESHAKER_OUTPUT.id, true, PEPTIDESHAKER_OUTPUT.description);
        aOptions.addOption(PEPTIDESHAKER_TXT_1.id, true, PEPTIDESHAKER_TXT_1.description);
        //aOptions.addOption(PEPTIDESHAKER_TXT_2.id, true, PEPTIDESHAKER_TXT_2.description);
        //aOptions.addOption(PEPTIDESHAKER_PRIDE.id, true, PEPTIDESHAKER_PRIDE.description);
        aOptions.addOption(PSM_FDR.id, true, PSM_FDR.description);
        aOptions.addOption(PEPTIDE_FDR.id, true, PEPTIDE_FDR.description);
        aOptions.addOption(PROTEIN_FDR.id, true, PROTEIN_FDR.description);
        aOptions.addOption(IDENTIFICATION_PARAMETERS.id, true, IDENTIFICATION_PARAMETERS.description);
        aOptions.addOption(GUI.id, true, GUI.description);
        aOptions.addOption(PTM_SCORE.id, true, PTM_SCORE.description);
        aOptions.addOption(PTM_THRESHOLD.id, true, PTM_THRESHOLD.description);
        aOptions.addOption(PROTEIN_FRACTION_MW_CONFIDENCE.id, true, PROTEIN_FRACTION_MW_CONFIDENCE.description);
        aOptions.addOption(MIN_PEPTIDE_LENGTH.id, true, MIN_PEPTIDE_LENGTH.description);
        aOptions.addOption(MAX_PEPTIDE_LENGTH.id, true, MAX_PEPTIDE_LENGTH.description);
        aOptions.addOption(MASCOT_E_VALUE_MAX.id, true, MASCOT_E_VALUE_MAX.description);
        aOptions.addOption(OMSSA_E_VALUE_MAX.id, true, OMSSA_E_VALUE_MAX.description);
        aOptions.addOption(XTANDEM_E_VALUE_MAX.id, true, XTANDEM_E_VALUE_MAX.description);
        aOptions.addOption(MAX_PRECURSOR_ERROR.id, true, MAX_PRECURSOR_ERROR.description);
        aOptions.addOption(MAX_PRECURSOR_ERROR_TYPE.id, true, MAX_PRECURSOR_ERROR_TYPE.description);
        aOptions.addOption(EXCLUDE_UNKNOWN_PTMS.id, true, EXCLUDE_UNKNOWN_PTMS.description);
        aOptions.addOption(SPECIES.id, true, SPECIES.description);
        aOptions.addOption(SPECIES_TYPE.id, true, SPECIES_TYPE.description);

        // Follow-up options
        FollowUpCLIParams.createOptionsCLI(aOptions);

        // Report options
        ReportCLIParams.createOptionsCLI(aOptions);

        // note: remember to add new parameters to the getOptionsAsString below as well
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";
        String formatter = "%-35s";

        output += "Mandatory parameters:\n\n";
        output += "-" + String.format(formatter, EXPERIMENT.id) + EXPERIMENT.description + "\n";
        output += "-" + String.format(formatter, SAMPLE.id) + SAMPLE.description + "\n";
        output += "-" + String.format(formatter, REPLICATE.id) + REPLICATE.description + "\n";
        output += "-" + String.format(formatter, SPECTRUM_FILES.id) + SPECTRUM_FILES.description + "\n";
        output += "-" + String.format(formatter, IDENTIFICATION_FILES.id) + IDENTIFICATION_FILES.description + "\n";
        output += "-" + String.format(formatter, PEPTIDESHAKER_OUTPUT.id) + PEPTIDESHAKER_OUTPUT.description + "\n";
        output += "-" + String.format(formatter, IDENTIFICATION_PARAMETERS.id) + IDENTIFICATION_PARAMETERS.description + "\n";

        output += "\n\nOptional gene annotation parameter:\n\n";
        output += "-" + String.format(formatter, SPECIES.id) + SPECIES.description + "\n";
        output += "-" + String.format(formatter, SPECIES_TYPE.id) + SPECIES_TYPE.description + "\n";

        output += "\n\nOptional processing parameters:\n\n";
        output += "-" + String.format(formatter, PROTEIN_FDR.id) + PROTEIN_FDR.description + "\n";
        output += "-" + String.format(formatter, PEPTIDE_FDR.id) + PEPTIDE_FDR.description + "\n";
        output += "-" + String.format(formatter, PSM_FDR.id) + PSM_FDR.description + "\n";
        output += "-" + String.format(formatter, PTM_SCORE.id) + PTM_SCORE.description + "\n";
        output += "-" + String.format(formatter, PTM_THRESHOLD.id) + PTM_THRESHOLD.description + "\n";
        output += "-" + String.format(formatter, SCORE_NEUTRAL_LOSSES.id) + SCORE_NEUTRAL_LOSSES.description + "\n";
        output += "-" + String.format(formatter, PROTEIN_FRACTION_MW_CONFIDENCE.id) + PROTEIN_FRACTION_MW_CONFIDENCE.description + "\n";

        output += "\n\nOptional filtering parameters:\n\n";
        output += "-" + String.format(formatter, MIN_PEPTIDE_LENGTH.id) + MIN_PEPTIDE_LENGTH.description + "\n";
        output += "-" + String.format(formatter, MAX_PEPTIDE_LENGTH.id) + MAX_PEPTIDE_LENGTH.description + "\n";
        output += "-" + String.format(formatter, MASCOT_E_VALUE_MAX.id) + MASCOT_E_VALUE_MAX.description + "\n";
        output += "-" + String.format(formatter, OMSSA_E_VALUE_MAX.id) + OMSSA_E_VALUE_MAX.description + "\n";
        output += "-" + String.format(formatter, XTANDEM_E_VALUE_MAX.id) + XTANDEM_E_VALUE_MAX.description + "\n";
        output += "-" + String.format(formatter, MAX_PRECURSOR_ERROR.id) + MAX_PRECURSOR_ERROR.description + "\n";
        output += "-" + String.format(formatter, MAX_PRECURSOR_ERROR_TYPE.id) + MAX_PRECURSOR_ERROR_TYPE.description + "\n";
        output += "-" + String.format(formatter, EXCLUDE_UNKNOWN_PTMS.id) + EXCLUDE_UNKNOWN_PTMS.description + "\n";

        output += "\n\nOptional output parameters:\n\n";
        output += "-" + String.format(formatter, PEPTIDESHAKER_TXT_1.id) + PEPTIDESHAKER_TXT_1.description + "\n";
        //output += "-" + String.format(formatter, PEPTIDESHAKER_TXT_2.id) + PEPTIDESHAKER_TXT_2.description + "\n";
        //output += "-" + String.format(formatter, PEPTIDESHAKER_PRIDE.id) + PEPTIDESHAKER_PRIDE.description + "\n";

        output += "\n\n\nFor follow up export options:\nReplace eu.isas.peptideshaker.cmd.PeptideShakerCLI with eu.isas.peptideshaker.cmd.FollowUpCLI\n\n";
        output += "\nFor report export options:\nReplace eu.isas.peptideshaker.cmd.PeptideShakerCLI with eu.isas.peptideshaker.cmd.ReportCLI\n";

        return output;
    }
}
