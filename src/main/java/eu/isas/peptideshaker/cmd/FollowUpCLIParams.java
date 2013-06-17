package eu.isas.peptideshaker.cmd;

import eu.isas.peptideshaker.followup.FastaExport;
import eu.isas.peptideshaker.followup.ProgenesisExport;
import eu.isas.peptideshaker.followup.SpectrumExporter;
import org.apache.commons.cli.Options;

/**
 * Enum class specifying the Command Line Parameters for follow up analysis.
 *
 * @author Marc Vaudel
 */
public enum FollowUpCLIParams {

    CPS_FILE("in", "PeptideShaker project (.cps file)", true),
    RECALIBRATION_FOLDER("recalibration_folder", "Folder where recalibrated files shall be exported. Note: existing files will be silently overwritten.", false),
    RECALIBRATION_MODE("recalibration_mode", "Type of recalibration. 0: precursor and fragment ions (default), 1: precursor only, 2: fragment ions only.", false),
    SPECTRUM_FOLDER("spectrum_folder", "Folder where to export spectra. Note: existing files will be silently overwritten.", false),
    PSM_TYPE("psm_type", "When exporting spectra, select a category of PSMs. " + SpectrumExporter.ExportType.getCommandLineOptions(), false),
    ACCESSIONS_FILE("accessions_file", "File where to export the protein accessions in text format. Note: existing files will be silently overwritten.", false),
    ACCESSIONS_TYPE("accessions_type", "When exporting accessions, select a category of proteins. " + FastaExport.ExportType.getCommandLineOptions(), false),
    FASTA_FILE("fasta_file", "File where to export the protein details in fasta format. Note: existing files will be silently overwritten.", false),
    FASTA_TYPE("fasta_type", "When exporting protein details, select a category of proteins. " + FastaExport.ExportType.getCommandLineOptions(), false),
    PROGENESIS_FILE("progenesis_file", "File where to export the identification results in a Non-Linear Progenesis compatible format. Note: existing files will be silently overwritten.", false),
    PROGENESIS_TYPE("progenesis_type", "Type of hits to export to Progenesis. " + ProgenesisExport.ExportType.getCommandLineOptions(), false),
    PEPNOVO_TRAINING_FOLDER("pepnovo_training_folder", "Folder where to output the pepnovo training files. Note: existing files will be silently overwritten.", false),
    PEPNOVO_TRAINING_RECALIBRATION("pepnovo_training_recalibration", "Indicate whether the exported mgf files shall be recalibrated. 0: No, 1: Yes (default).", false),
    PEPNOVO_TRAINING_FDR("pepnovo_training_fdr", "The FDR used for the 'good spectra' export. If not set, the validation FDR will be used.", false),
    PEPNOVO_TRAINING_FNR("pepnovo_training_fnr", "The FNR used for the 'bad spectra' export. If not set, the same value as for the 'good spectra' FDR will be used.", false);
    
    
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
        aOptions.addOption(PEPNOVO_TRAINING_FOLDER.id, true, PEPNOVO_TRAINING_FOLDER.description);
        aOptions.addOption(PEPNOVO_TRAINING_RECALIBRATION.id, true, PEPNOVO_TRAINING_RECALIBRATION.description);
        aOptions.addOption(PEPNOVO_TRAINING_FDR.id, true, PEPNOVO_TRAINING_FDR.description);
        aOptions.addOption(PEPNOVO_TRAINING_FNR.id, true, PEPNOVO_TRAINING_FNR.description);

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

        output += "Mandatory parameter:\n\n";
        output += "-" + String.format(formatter, CPS_FILE.id) + CPS_FILE.description + "\n";

        output += "\n\nOptional output parameters:\n\n";
        output += getOutputOptionsAsString();

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

        output += "\nRecalibration parameters:\n\n";
        output += "-" + String.format(formatter, RECALIBRATION_FOLDER.id) + RECALIBRATION_FOLDER.description + "\n";
        output += "-" + String.format(formatter, RECALIBRATION_MODE.id) + RECALIBRATION_MODE.description + "\n";
        
        output += "\nSpectrum export:\n\n";
        output += "-" + String.format(formatter, SPECTRUM_FOLDER.id) + SPECTRUM_FOLDER.description + "\n";
        output += "-" + String.format(formatter, PSM_TYPE.id) + PSM_TYPE.description + "\n";
        
        output += "\nProgenesis export:\n\n";
        output += "-" + String.format(formatter, PROGENESIS_FILE.id) + PROGENESIS_FILE.description + "\n";
        output += "-" + String.format(formatter, PROGENESIS_TYPE.id) + PROGENESIS_TYPE.description + "\n";
        
        output += "\nAccessions export:\n\n";
        output += "-" + String.format(formatter, ACCESSIONS_FILE.id) + ACCESSIONS_FILE.description + "\n";
        output += "-" + String.format(formatter, ACCESSIONS_TYPE.id) + ACCESSIONS_TYPE.description + "\n";
        
        output += "\nAccessions export:\n\n";
        output += "-" + String.format(formatter, FASTA_FILE.id) + FASTA_FILE.description + "\n";
        output += "-" + String.format(formatter, FASTA_TYPE.id) + FASTA_TYPE.description + "\n";
        
        output += "\nPepnovo training files export:\n\n";
        output += "-" + String.format(formatter, PEPNOVO_TRAINING_FOLDER.id) + PEPNOVO_TRAINING_FOLDER.description + "\n";
        output += "-" + String.format(formatter, PEPNOVO_TRAINING_RECALIBRATION.id) + PEPNOVO_TRAINING_RECALIBRATION.description + "\n";
        output += "-" + String.format(formatter, PEPNOVO_TRAINING_FDR.id) + PEPNOVO_TRAINING_FDR.description + "\n";
        output += "-" + String.format(formatter, PEPNOVO_TRAINING_FNR.id) + PEPNOVO_TRAINING_FNR.description + "\n";
        

        return output;
    }
}
