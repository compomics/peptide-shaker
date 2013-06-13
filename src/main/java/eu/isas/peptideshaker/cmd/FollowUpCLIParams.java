package eu.isas.peptideshaker.cmd;

import org.apache.commons.cli.Options;

/**
 * Enum class specifying the Command Line Parameters for follow up analysis.
 *
 * @author Marc Vaudel
 */
public enum FollowUpCLIParams {

    CPS_FILE("in", "PeptideShaker project (.cps file)", true),
    RECALIBRATION_FOLDER("recalibration_folder", "Folder where recalibrated files shall be exported. Note: existing files will be silently overwritten.", false),
    RECALIBRATION_MODE("recalibration_mode", "Type of recalibration. 0: precursor and fragment ions (default), 1: precursor only, 2: fragment ions only.", false);
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

        return output;
    }
}
