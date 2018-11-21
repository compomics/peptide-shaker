package eu.isas.peptideshaker.cmd;

import eu.isas.peptideshaker.followup.FastaExport;
import eu.isas.peptideshaker.followup.InclusionListExport;
import eu.isas.peptideshaker.followup.ProgenesisExport;
import eu.isas.peptideshaker.followup.SpectrumExporter;
import org.apache.commons.cli.Options;

/**
 * Enum class specifying the Command Line Parameters for follow up analysis.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum FollowUpCLIParams {

    CPS_FILE("in", "PeptideShaker project (.cpsx or .zip file)", true),
    RECALIBRATION_FOLDER("recalibration_folder", "Output folder for the recalibrated files. (Existing files will be overwritten.)", false),
    RECALIBRATION_MODE("recalibration_mode", "Recalibration type. 0: precursor and fragment ions (default), 1: precursor only, 2: fragment ions only.", false),
    SPECTRUM_FOLDER("spectrum_folder", "Output folder for the spectra. (Existing files will be overwritten.)", false),
    PSM_TYPE("psm_type", "Type of PSMs. " + SpectrumExporter.ExportType.getCommandLineOptions(), false),
    ACCESSIONS_FILE("accessions_file", "Output file to export the protein accessions in text format. (Existing files will be overwritten.)", false),
    ACCESSIONS_TYPE("accessions_type", "When exporting accessions, select a category of proteins. " + FastaExport.ExportType.getCommandLineOptions(), false),
    FASTA_FILE("fasta_file", "File where to export the protein details in fasta format. (Existing files will be overwritten.)", false),
    FASTA_TYPE("fasta_type", "When exporting protein details, select a category of proteins. " + FastaExport.ExportType.getCommandLineOptions(), false),
    PROGENESIS_FILE("progenesis_file", "Output file for identification results in Progenesis LC-MS compatible format. (Existing files will be overwritten.)", false),
    PROGENESIS_TYPE("progenesis_type", "Type of hits to export to Progenesis. " + ProgenesisExport.ExportType.getCommandLineOptions(), false),
    PROGENESIS_TARGETED_PTMS("progenesis_ptms", "For the progenesis PTM export, the comma separated list of targeted PTMs in a list of PTM names", false),
    INCLUSION_LIST_FILE("inclusion_list_file", "Output file for an inclusion list of validated hits. (Existing files will be overwritten.)", false),
    INCLUSION_LIST_FORMAT("inclusion_list_format", "Format for the inclusion list. " + InclusionListExport.ExportFormat.getCommandLineOptions(), false),
    INCLUSION_LIST_PROTEIN_FILTERS("inclusion_list_protein_filters", "Protein inference filters to be used for the inclusion list export (comma separated). " + InclusionListExport.getProteinFiltersCommandLineOptions(), false),
    INCLUSION_LIST_PEPTIDE_FILTERS("inclusion_list_peptide_filters", "Peptide filters to be used for the inclusion list export (comma separated). " + InclusionListExport.PeptideFilterType.getCommandLineOptions(), false),
    INCLUSION_LIST_RT_WINDOW("inclusion_list_rt_window", "Retention time window for the inclusion list export (in seconds).", false);

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
    private FollowUpCLIParams(String id, String description, boolean mandatory) {
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

        aOptions.addOption(CPS_FILE.id, true, CPS_FILE.description);
        aOptions.addOption(RECALIBRATION_FOLDER.id, true, RECALIBRATION_FOLDER.description);
        aOptions.addOption(RECALIBRATION_MODE.id, true, RECALIBRATION_MODE.description);
        aOptions.addOption(SPECTRUM_FOLDER.id, true, SPECTRUM_FOLDER.description);
        aOptions.addOption(PSM_TYPE.id, true, PSM_TYPE.description);
        aOptions.addOption(ACCESSIONS_FILE.id, true, ACCESSIONS_FILE.description);
        aOptions.addOption(ACCESSIONS_TYPE.id, true, ACCESSIONS_TYPE.description);
        aOptions.addOption(FASTA_FILE.id, true, FASTA_FILE.description);
        aOptions.addOption(FASTA_TYPE.id, true, FASTA_TYPE.description);
        aOptions.addOption(PROGENESIS_FILE.id, true, PROGENESIS_FILE.description);
        aOptions.addOption(PROGENESIS_TYPE.id, true, PROGENESIS_TYPE.description);
        aOptions.addOption(PROGENESIS_TARGETED_PTMS.id, true, PROGENESIS_TARGETED_PTMS.description);
        aOptions.addOption(INCLUSION_LIST_FILE.id, true, INCLUSION_LIST_FILE.description);
        aOptions.addOption(INCLUSION_LIST_FORMAT.id, true, INCLUSION_LIST_FORMAT.description);
        aOptions.addOption(INCLUSION_LIST_PEPTIDE_FILTERS.id, true, INCLUSION_LIST_PEPTIDE_FILTERS.description);
        aOptions.addOption(INCLUSION_LIST_PROTEIN_FILTERS.id, true, INCLUSION_LIST_PROTEIN_FILTERS.description);
        aOptions.addOption(INCLUSION_LIST_RT_WINDOW.id, true, INCLUSION_LIST_RT_WINDOW.description);
        
        // Path setup
        PathSettingsCLIParams.createOptionsCLI(aOptions);

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

        output += "Mandatory Parameter:\n\n";
        output += "-" + String.format(formatter, CPS_FILE.id) + " " + CPS_FILE.description + "\n";

        output += "\n\nOptional Output Parameters:\n";
        output += getOutputOptionsAsString();

        output += "\n\nOptional Temporary Folder:\n\n";
        output += "-" + String.format(formatter, PathSettingsCLIParams.ALL.id) + " " + PathSettingsCLIParams.ALL.description + "\n";

        return output;
    }

    /**
     * Returns the output options as a string.
     *
     * @return the output options as a string
     */
    public static String getOutputOptionsAsString() {

        String output = "";
        String formatter = "%-35s";

        output += "\nRecalibration Parameters:\n\n";
        output += "-" + String.format(formatter, RECALIBRATION_FOLDER.id) + " " + RECALIBRATION_FOLDER.description + "\n";
        output += "-" + String.format(formatter, RECALIBRATION_MODE.id) + " " + RECALIBRATION_MODE.description + "\n";
        
        output += "\nSpectrum Export:\n\n";
        output += "-" + String.format(formatter, SPECTRUM_FOLDER.id) + " " + SPECTRUM_FOLDER.description + "\n";
        output += "-" + String.format(formatter, PSM_TYPE.id) + " " + PSM_TYPE.description + "\n";
        
        output += "\nProgenesis Export:\n\n";
        output += "-" + String.format(formatter, PROGENESIS_FILE.id) + " " + PROGENESIS_FILE.description + "\n";
        output += "-" + String.format(formatter, PROGENESIS_TYPE.id) + " " + PROGENESIS_TYPE.description + "\n";
        output += "-" + String.format(formatter, PROGENESIS_TARGETED_PTMS.id) + " " + PROGENESIS_TARGETED_PTMS.description + "\n";
        
        output += "\nAccessions Export:\n\n";
        output += "-" + String.format(formatter, ACCESSIONS_FILE.id) + " " + ACCESSIONS_FILE.description + "\n";
        output += "-" + String.format(formatter, ACCESSIONS_TYPE.id) + " " + ACCESSIONS_TYPE.description + "\n";
        
        output += "\nFASTA Export:\n\n";
        output += "-" + String.format(formatter, FASTA_FILE.id) + " " + FASTA_FILE.description + "\n";
        output += "-" + String.format(formatter, FASTA_TYPE.id) + " " + FASTA_TYPE.description + "\n";
        
        output += "\nInclusion List Generation\n\n";
        output += "-" + String.format(formatter, INCLUSION_LIST_FILE.id) + " " + INCLUSION_LIST_FILE.description + "\n";
        output += "-" + String.format(formatter, INCLUSION_LIST_FORMAT.id) + " " + INCLUSION_LIST_FORMAT.description + "\n";
        output += "-" + String.format(formatter, INCLUSION_LIST_PEPTIDE_FILTERS.id) + " " + INCLUSION_LIST_PEPTIDE_FILTERS.description + "\n";
        output += "-" + String.format(formatter, INCLUSION_LIST_PROTEIN_FILTERS.id) + " " + INCLUSION_LIST_PROTEIN_FILTERS.description + "\n";
        output += "-" + String.format(formatter, INCLUSION_LIST_RT_WINDOW.id) + " " + INCLUSION_LIST_RT_WINDOW.description + "\n";
        
        return output;
    }
}
