package eu.isas.peptideshaker.cmd;

import eu.isas.peptideshaker.export.PSExportFactory;
import org.apache.commons.cli.Options;

/**
 * This class provides the available reports as command line parameters.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum ReportCLIParams {

    CPS_FILE("in", "PeptideShaker project (.cpsx or .zip file)", true, true),
    EXPORT_FOLDER("out_reports", "Output folder for report files. (Existing files will be overwritten.)", true, true),
    EXPORT_PREFIX("report_prefix", "Prefix added to the report file name.", false, true),
    REPORT_TYPE("reports", "Comma separated list of types of report to export. " + PSExportFactory.getInstance().getCommandLineOptions(), false, true),
    DOCUMENTATION_TYPE("documentation", "Comma separated list of types of report documentation to export. " + PSExportFactory.getInstance().getCommandLineOptions(), false, true),
    GZIP("gzip", "Indicates whether the report should be compressed (0: no, 1: yes, default is 0).", false, true);

    /**
     * Short Id for the CLI parameter.
     */
    public final String id;
    /**
     * Explanation for the CLI parameter.
     */
    public final String description;
    /**
     * Boolean indicating whether the parameter is mandatory.
     */
    public final boolean mandatory;
    /**
     * Boolean indicating whether the parameter has arguments.
     */
    public final boolean hasArg;

    /**
     * Private constructor managing the various variables for the enum
     * instances.
     *
     * @param id the parameter id
     * @param description the parameter description
     * @param mandatory boolean indicating whether the parameter mandatory
     * @param hasArg boolean indicating whether the parameter needs an argument
     */
    private ReportCLIParams(String id, String description, boolean mandatory, boolean hasArg) {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
        this.hasArg = hasArg;
    }

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {

        for (ReportCLIParams reportCLIParams : values()) {
            aOptions.addOption(reportCLIParams.id, reportCLIParams.hasArg, reportCLIParams.description);
        }

        // path setup
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

        output += "Mandatory parameters:\n\n";
        output += "-" + String.format(formatter, CPS_FILE.id) + CPS_FILE.description + "\n";
        output += "-" + String.format(formatter, EXPORT_FOLDER.id) + EXPORT_FOLDER.description + "\n";

        output += "\n\nOptional output parameters:\n";
        output += getOutputOptionsAsString();

        output += "\n\nOptional temporary folder and name prefix:\n\n";
        output += "-" + String.format(formatter, EXPORT_PREFIX.id) + EXPORT_PREFIX.description + "\n";
        output += "-" + String.format(formatter, PathSettingsCLIParams.ALL.id) + PathSettingsCLIParams.ALL.description + "\n";

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

        output += "\nReport documentation export:\n\n";
        output += "-" + String.format(formatter, DOCUMENTATION_TYPE.id) + DOCUMENTATION_TYPE.description + "\n";

        output += "\nCompression:\n\n";
        output += "-" + String.format(formatter, GZIP.id) + GZIP.description + "\n";

        return output;
    }
}
