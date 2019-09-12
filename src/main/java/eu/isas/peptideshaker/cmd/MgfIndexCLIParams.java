package eu.isas.peptideshaker.cmd;

import org.apache.commons.cli.Options;

/**
 * Enum class specifying the Command Line Parameters for mgf index creation.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 * @author Carlos Horro
 */
public enum MgfIndexCLIParams {

    PSDB_FILE("in", "PeptideShaker project (.psdb file).", true, false),
    SPECTRUM_FILES("spectrum_files", "Spectrum files (mgf format), comma separated list or an entire folder. This or '"+PSDB_FILE.id+"' option must be used. ", true, false),
    EXPORT_ZIP("zip", "Exports the mgf index files into the specified zip file.", true, false),
    EXPORT_FOLDER("out_folder", "Output folder for mgf index files (existing files will be overwritten). This or '"+EXPORT_ZIP.id+"' option must be used.", true, false);

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
     * Indicates whether user input is expected.
     */
    public final boolean hasArgument;

    /**
     * Private constructor managing the various variables for the enum
     * instances.
     *
     * @param id the id
     * @param description the description
     * @param hasArgument is input expected
     * @param mandatory is the parameter mandatory
     */
    private MgfIndexCLIParams(String id, String description, boolean hasArgument, boolean mandatory) {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
        this.hasArgument = hasArgument;
    }

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {

        for (MgfIndexCLIParams mgfIndexCLIParams : values()) {
            aOptions.addOption(mgfIndexCLIParams.id, mgfIndexCLIParams.hasArgument, mgfIndexCLIParams.description);
        }
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";
        String formatter = "%-35s";

        output += "\n\nOptional Parameters:\n";
        for (MgfIndexCLIParams mgfIndexCLIParams : values()) {
            if (!mgfIndexCLIParams.mandatory) {
                output += "-" + String.format(formatter, mgfIndexCLIParams.id) + " " + mgfIndexCLIParams.description + "\n";
            }
        }

        output += "\n\nOptional Temporary Folder:\n";
        output += "-" + String.format(formatter, PathSettingsCLIParams.ALL.id) + " " + PathSettingsCLIParams.ALL.description + "\n";

        return output;
    }
}
