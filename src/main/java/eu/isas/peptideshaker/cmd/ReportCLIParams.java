package eu.isas.peptideshaker.cmd;

import eu.isas.peptideshaker.export.ExportFactory;
import org.apache.commons.cli.Options;

/**
 * This class provides the available reports as command line parameters.
 *
 * @author Marc Vaudel
 */
public enum ReportCLIParams {

    CPS_FILE("in", "PeptideShaker project (.cps file)", true),
    EXPORT_FOLDER("out", "Folder where report files shall be exported. Note: existing files will be silently overwritten.", true),
    REPORT_TYPE("reports", "Comma separated list of types of report to export. " + ExportFactory.getInstance().getCommandLineOptions(), false),
    DOCUMENTATION_TYPE("documentation", "Comma separated list of types of report documentation to export. " + ExportFactory.getInstance().getCommandLineOptions(), false);
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
    private ReportCLIParams(String id, String description, boolean mandatory) {
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
        aOptions.addOption(EXPORT_FOLDER.id, true, EXPORT_FOLDER.description);
        aOptions.addOption(REPORT_TYPE.id, true, REPORT_TYPE.description);
        aOptions.addOption(DOCUMENTATION_TYPE.id, true, DOCUMENTATION_TYPE.description);

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
        output += "-" + String.format(formatter, CPS_FILE.id) + CPS_FILE.description + "\n";
        output += "-" + String.format(formatter, EXPORT_FOLDER.id) + EXPORT_FOLDER.description + "\n";

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

        output += "\nReport export:\n\n";
        output += "-" + String.format(formatter, REPORT_TYPE.id) + REPORT_TYPE.description + "\n";

        output += "\nReport Documentation export:\n\n";
        output += "-" + String.format(formatter, DOCUMENTATION_TYPE.id) + DOCUMENTATION_TYPE.description + "\n";

        return output;
    }
}
