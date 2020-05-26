package eu.isas.peptideshaker.cmd;

import com.compomics.software.cli.CommandLineUtils;
import org.apache.commons.cli.Options;

/**
 * Enum class specifying the ModificationsCLI parameters.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum StirredOptions {

    input("i", "input", "The file containing the peptides.", true, true),
    spectrum("s", "spectrum", "The file containing the spectra. Mandatory when not provided in a SearchGUI packaged output file.", false, true),
    fasta("f", "fasta", "The file containing the protein or peptide sequences. Mandatory when not provided in a SearchGUI packaged output file.", false, true),
    identificationParametersFile("p", "identificationParametersFile", "The file containing the identification parameters. Mandatory when not provided in a SearchGUI packaged output file.", false, true),
    tempFolder("tmp", "tempFolder", "The temp folder to use for temp files. Default: next to output file.", false, true),
    log("l", "log", "The file to write the log to. Default: next to output file.", false, true),
    nThreads("t", "nThreads", "The number of threads to use. Default: the number of available processors.", false, true),
    timeOutDays("to", "timeOutDays", "Timeout time in days. Default: 365.", false, true),
    contactFirstName("cfn", "contactFirstName", "The first name of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    contactLastName("cln", "contactLastName", "The last name of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    contactAddress("ca", "contactAddress", "The address of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    contactEmail("ce", "contactEmail", "The email of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    contactOrganizationName("con", "contactOrganizationName", "The name of the organization of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    contactOrganizationAddress("coa", "contactOrganizationAddress", "The address of the organization of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    contactOrganizationEmail("coe", "contactOrganizationEmail", "The email of the organization of the contact to annotate in the mzIdentML file. Default: 'Unknown'.", false, true),
    output("o", "output", "The folder where to write the results.", true, true);

    /**
     * Short key for the command line argument.
     */
    public final String opt;
    /**
     * Key for the command line argument.
     */
    public final String longOpt;
    /**
     * Description for the command line argument.
     */
    public final String description;
    /**
     * If true the parameter is mandatory.
     */
    public final boolean mandatory;
    /**
     * If true this command line argument needs a value.
     */
    public final boolean hasValue;

    /**
     * Private constructor managing the various variables for the enum
     * instances.
     *
     * @param opt Short key for the CLI parameter.
     * @param longOpt Key for the CLI parameter.
     * @param description Description for the command line argument.
     * @param mandatory If true the parameter is mandatory.
     * @param hasValue If true this command line argument needs a value.
     */
    private StirredOptions(
            String opt,
            String longOpt,
            String description,
            boolean mandatory,
            boolean hasValue
    ) {
        this.opt = opt;
        this.longOpt = longOpt;
        this.description = description;
        this.mandatory = mandatory;
        this.hasValue = hasValue;
    }

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(
            Options aOptions
    ) {

        for (StirredOptions option : values()) {

            aOptions.addOption(option.opt,
                    option.hasValue,
                    option.description
            );

        }
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";

        output += "Mandatory arguments:\n";

        for (StirredOptions option : values()) {

            if (option.mandatory) {

                output += "-" + String.format(CommandLineUtils.FORMATTER, option.opt) + " " + option.description + "\n";

            }
        }

        output += "\n\nOptional arguments:\n";

        for (StirredOptions option : values()) {

            if (option.mandatory) {

                output += "-" + String.format(CommandLineUtils.FORMATTER, option.opt) + " " + option.description + "\n";

            }
        }

        return output;
    }
}
