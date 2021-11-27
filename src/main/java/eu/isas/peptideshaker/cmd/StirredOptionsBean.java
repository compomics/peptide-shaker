package eu.isas.peptideshaker.cmd;

import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;

/**
 * This class parses the parameters from an ModificationsCLI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class StirredOptionsBean {

    /**
     * The input file.
     */
    public final File inputFile;
    /**
     * The spectrum file.
     */
    public File spectrumFile = null;
    /**
     * The fasta file.
     */
    public File fastaFile = null;
    /**
     * The identification parameters file.
     */
    public File identificationParametersFile = null;
    /**
     * The temp folder to use for temp files.
     */
    public final File tempFolder;
    /**
     * The log file.
     */
    public final File logFile;
    /**
     * The number of threads to use.
     */
    public int nThreads = Runtime.getRuntime().availableProcessors();
    /**
     * The timeout time in days.
     */
    public int timeOutDays = 365;
    /**
     * The output folder.
     */
    public final File outputFolder;
    /**
     * The first name of the contact to annotate in the mzIdentML file.
     */
    public String contactFirstName = "Unknown";
    /**
     * The last name of the contact to annotate in the mzIdentML file.
     */
    public String contactLastName = "Unknown";
    /**
     * The address of the contact to annotate in the mzIdentML file.
     */
    public String contactAddress = "Unknown";
    /**
     * The email of the contact to annotate in the mzIdentML file.
     */
    public String contactEmail = "Unknown";
    /**
     * The name of the organization of the contact to annotate in the mzIdentML
     * file.
     */
    public String contactOrganizationName = "Unknown";
    /**
     * The address of the organization of the contact to annotate in the
     * mzIdentML file.
     */
    public String contactOrganizationAddress = "Unknown";
    /**
     * The email of the organization of the contact to annotate in the mzIdentML
     * file.
     */
    public String contactOrganizationEmail = "Unknown";

    /**
     * Parses all the arguments from a command line.
     *
     * @param aLine the command line
     *
     * @throws IOException if an error occurs while reading or writing a file.
     */
    public StirredOptionsBean(CommandLine aLine) throws IOException {

        // Check that mandatory options are provided
        for (StirredOptions option : StirredOptions.values()) {

            if (option.mandatory && !hasOption(aLine, option)) {

                throw new IllegalArgumentException("No value found for mandatory option " + option.opt + " (" + option.longOpt + ").");

            }
        }

        // Input file
        String arg = getOptionValue(aLine, StirredOptions.input);
        inputFile = new File(arg);

        // Spectrum file
        if (hasOption(aLine, StirredOptions.spectrum)) {

            arg = getOptionValue(aLine, StirredOptions.spectrum);
            spectrumFile = new File(arg);

        }

        // Fasta file
        if (hasOption(aLine, StirredOptions.fasta)) {

            arg = getOptionValue(aLine, StirredOptions.fasta);
            fastaFile = new File(arg);

        }

        // Spectrum file
        if (hasOption(aLine, StirredOptions.identificationParametersFile)) {

            arg = getOptionValue(aLine, StirredOptions.identificationParametersFile);
            identificationParametersFile = new File(arg);

        }

        // Output folder
        arg = getOptionValue(aLine, StirredOptions.output);
        outputFolder = new File(arg);

        if (!outputFolder.exists()) {

            throw new IllegalArgumentException("Output folder '" + outputFolder + "' not found.");

        }
        if (!outputFolder.isDirectory()) {

            throw new IllegalArgumentException("Output folder '" + outputFolder + "' must be a directory.");

        }

        // Log file
        if (hasOption(aLine, StirredOptions.log)) {

            arg = getOptionValue(aLine, StirredOptions.log);
            logFile = new File(arg);

        } else {

            logFile = new File(outputFolder, inputFile.getName() + ".stirred.log");

        }

        // Temp folder
        if (hasOption(aLine, StirredOptions.tempFolder)) {

            arg = getOptionValue(aLine, StirredOptions.tempFolder);
            tempFolder = new File(arg);

            if (!tempFolder.exists()) {

                tempFolder.mkdir();

            }

        } else {

            tempFolder = outputFolder;

        }

        // Number of threads to use
        if (hasOption(aLine, StirredOptions.nThreads)) {

            String argString = getOptionValue(aLine, StirredOptions.nThreads);

            try {

                nThreads = Integer.parseInt(argString);

                if (nThreads <= 0) {

                    throw new IllegalArgumentException(
                            "Input for number of threads must be a strictly positive number."
                    );

                }

            } catch (Exception e) {

                e.printStackTrace();

                throw new IllegalArgumentException(
                        "Input for number of threads could not be parsed as a number: " + argString + "."
                );

            }
        }

        // Time out
        if (hasOption(aLine, StirredOptions.timeOutDays)) {

            String argString = getOptionValue(aLine, StirredOptions.timeOutDays);

            try {

                timeOutDays = Integer.parseInt(argString);

                if (timeOutDays <= 0) {

                    throw new IllegalArgumentException(
                            "Input for timeout must be a strictly positive number."
                    );

                }

            } catch (Exception e) {

                e.printStackTrace();

                throw new IllegalArgumentException(
                        "Input for timeout could not be parsed as a number: " + argString + "."
                );

            }
        }

        // The contact first name
        if (hasOption(aLine, StirredOptions.contactFirstName)) {

            contactFirstName = getOptionValue(aLine, StirredOptions.contactFirstName);

        }

        // The contact last name
        if (hasOption(aLine, StirredOptions.contactLastName)) {

            contactLastName = getOptionValue(aLine, StirredOptions.contactLastName);

        }

        // The contact address
        if (hasOption(aLine, StirredOptions.contactAddress)) {

            contactAddress = getOptionValue(aLine, StirredOptions.contactAddress);

        }

        // The contact email
        if (hasOption(aLine, StirredOptions.contactEmail)) {

            contactEmail = getOptionValue(aLine, StirredOptions.contactEmail);

        }

        // The contact organization name
        if (hasOption(aLine, StirredOptions.contactOrganizationName)) {

            contactOrganizationName = getOptionValue(aLine, StirredOptions.contactOrganizationName);

        }

        // The contact organization address
        if (hasOption(aLine, StirredOptions.contactOrganizationAddress)) {

            contactOrganizationAddress = getOptionValue(aLine, StirredOptions.contactOrganizationAddress);

        }

        // The contact organization email
        if (hasOption(aLine, StirredOptions.contactOrganizationEmail)) {

            contactOrganizationEmail = getOptionValue(aLine, StirredOptions.contactOrganizationEmail);

        }
    }

    /**
     * Returns a boolean indicating whether the given command line has the given option.
     * 
     * @param commandLine The command line.
     * @param option The option.
     * 
     * @return A boolean indicating whether the given command line has the given option.
     */
    private static boolean hasOption(
            CommandLine commandLine,
            StirredOptions option
    ) {

        return commandLine.hasOption(option.opt) || commandLine.hasOption(option.longOpt);

    }

    /**
     * Returns the value for the given option in the given command line.
     * 
     * @param commandLine The command line.
     * @param option The option.
     * 
     * @return The value for the given option in the given command line.
     */
    private static String getOptionValue(
            CommandLine commandLine,
            StirredOptions option
    ) {

        if (commandLine.hasOption(option.opt)) {

            return commandLine.getOptionValue(option.opt);

        }
        if (commandLine.hasOption(option.longOpt)) {

            return commandLine.getOptionValue(option.longOpt);

        }

        return null;

    }
}
