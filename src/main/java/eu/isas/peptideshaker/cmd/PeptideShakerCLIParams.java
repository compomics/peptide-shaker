package eu.isas.peptideshaker.cmd;

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
    EXPERIMENT("experiment", "Mandatory: Specifies the experiment name.", true),
    SAMPLE("sample", "Mandatory: Specifies the sample name.", true),
    REPLICATE("replicate", "The replicate number.", true),
    SPECTRUM_FILES("spectrum_files", "Mandatory: Spectrum files (mgf format), comma separated list or an entire folder.", true),
    IDENTIFICATION_FILES("identification_files", "Mandatory: Identification files (.dat, .omx or .t.xml), comma separated list or an entire folder.", true),
    PEPTIDESHAKER_OUTPUT("out", "Mandatory: PeptideShaker output file. Note: if file exists it will be overwritten.", true),
    PEPTIDESHAKER_TXT_1("out_txt_1", "Output folder for text summary - format 1 (three files: proteins, peptides and psms).", false),
    PEPTIDESHAKER_TXT_2("out_txt_2", "Output folder for text summary - format 2 (one file: proteins and peptides).", false),
    PEPTIDESHAKER_PRIDE("out_pride", "PeptideShaker PRIDE XML output file (not yet implemented).", false),
    PSM_FDR("psm_FDR", "FDR at the PSM level (default 1% FDR: '1').", false),
    PSM_FLR("psm_FLR", "FLR at the PSM level (default 1% FLR: '1').", false),
    PEPTIDE_FDR("peptide_FDR", "FDR at the peptide level (default 1% FDR: '1').", false),
    PROTEIN_FDR("protein_FDR", "FDR at the protein level (default 1% FDR: '1').", false),
    SEARCH_PARAMETERS("search_params", "Serialized com.compomics.util.experiment.identification.SearchParameters file created by SearchGUI.", false),
    GUI("gui", "Use a dialog to display the progress (1: true, 0: false, default is 0)", false);
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
        aOptions.addOption(EXPERIMENT.id, true, EXPERIMENT.description);
        aOptions.addOption(SAMPLE.id, true, SAMPLE.description);
        aOptions.addOption(REPLICATE.id, true, REPLICATE.description);
        aOptions.addOption(SPECTRUM_FILES.id, true, SPECTRUM_FILES.description);
        aOptions.addOption(IDENTIFICATION_FILES.id, true, IDENTIFICATION_FILES.description);
        aOptions.addOption(PEPTIDESHAKER_OUTPUT.id, true, PEPTIDESHAKER_OUTPUT.description);
        aOptions.addOption(PEPTIDESHAKER_TXT_1.id, true, PEPTIDESHAKER_TXT_1.description);
        aOptions.addOption(PEPTIDESHAKER_TXT_2.id, true, PEPTIDESHAKER_TXT_2.description);
        aOptions.addOption(PEPTIDESHAKER_PRIDE.id, true, PEPTIDESHAKER_PRIDE.description);
        aOptions.addOption(PSM_FDR.id, true, PSM_FDR.description);
        aOptions.addOption(PSM_FLR.id, true, PSM_FLR.description);
        aOptions.addOption(PEPTIDE_FDR.id, true, PEPTIDE_FDR.description);
        aOptions.addOption(PROTEIN_FDR.id, true, PROTEIN_FDR.description);
        aOptions.addOption(SEARCH_PARAMETERS.id, true, SEARCH_PARAMETERS.description);
        aOptions.addOption(GUI.id, true, GUI.description);
    }
}
