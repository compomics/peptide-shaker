package eu.isas.peptideshaker.cmd;

import org.apache.commons.cli.Options;

/**
 * Enum class specifying the Command Line Parameters for PeptideShaker.
 *
 * @author Kenny Helsens
 * @author Marc Vaudel
 */
public enum PeptideShakerCLIParams {

    // Any change here must be reported in the wiki: https://code.google.com/p/peptide-shaker/wiki/PeptideShakerCLI
    EXPERIMENT("experiment", "Mandatory: Specifies the experiment name", true),
    SAMPLE("sample", "Mandatory: Specifies the sample name", true),
    REPLICATE("replicate", "The replicate number", true),
    SPECTRUM_FILES("spectrum_files", "Mandatory: The spectrum files (mgf format) in a comma separated list or an entire folder. Example: 'file1.mgf, file2.mgf'.", true),
    IDENTIFICATION_FILES("identification_files", "Mandatory: The identification files (Mascot .dat, OMSSA .omx and X!Tandem .t.xml formats) in a comma separated list or an entire folder. Example: 'file1.omx, file1.dat, file1.t.xml'.", true),
    PEPTIDESHAKER_OUTPUT("out", "Mandatory: PeptideShaker output file. If the file already exists it will be silently overwritten.", true),
    PEPTIDESHAKER_CSV("out_csv", "Output folder for csv summary", false),
    PEPTIDESHAKER_PRIDE("out_pride", "PeptideShaker Pride xml output file (not implemented yet)", false),
    PSM_FDR("psm_FDR", "FDR at the PSM level in percent (default 1% FDR: '1')", false),
    PSM_FLR("psm_FLR", "FLR at the PSM level in percent for peptides with different potential modification sites and one variable modification (default 1% FLR: '1')", false),
    PEPTIDE_FDR("peptide_FDR", "FDR at the peptide level in percent (default 1% FDR: '1')", false),
    PROTEIN_FDR("protein_FDR", "FDR at the protein level in percent (default 1% FDR: '1')", false),
    SEARCH_PARAMETERS("identification_parameters", "The identification parameters as serialized file of com.compomics.util.experiment.identification.SearchParameters. This file is automatically saved by SearchGUI along with the identification files. If not provided default settings will be tried. If the default settings don't work the import will crash - hopefully decently.", false);
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
        aOptions.addOption(PEPTIDESHAKER_CSV.id, true, PEPTIDESHAKER_CSV.description);
        aOptions.addOption(PEPTIDESHAKER_PRIDE.id, true, PEPTIDESHAKER_PRIDE.description);
        aOptions.addOption(PSM_FDR.id, true, PSM_FDR.description);
        aOptions.addOption(PSM_FLR.id, true, PSM_FLR.description);
        aOptions.addOption(PEPTIDE_FDR.id, true, PEPTIDE_FDR.description);
        aOptions.addOption(PROTEIN_FDR.id, true, PROTEIN_FDR.description);
        aOptions.addOption(SEARCH_PARAMETERS.id, true, SEARCH_PARAMETERS.description);
    }
}
