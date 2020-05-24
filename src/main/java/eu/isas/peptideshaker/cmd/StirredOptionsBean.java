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
    public final File spectrumFile;
    /**
     * The fasta file.
     */
    public final File fastaFile;
    /**
     * The identification parameters file.
     */
    public final File identificationParametersFile;
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
     * The output file.
     */
    public final File outputFile;
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

            if (option.mandatory && !aLine.hasOption(option.opt)) {

                throw new IllegalArgumentException("No value found for mandatory option " + option.opt + ".");

            }
        }

        // Input file
        String arg = aLine.getOptionValue(StirredOptions.input.opt);
        inputFile = new File(arg);

        // Spectrum file
        arg = aLine.getOptionValue(StirredOptions.spectrum.opt);
        spectrumFile = new File(arg);

        // Spectrum file
        arg = aLine.getOptionValue(StirredOptions.fasta.opt);
        fastaFile = new File(arg);

        // Spectrum file
        arg = aLine.getOptionValue(StirredOptions.identificationParametersFile.opt);
        identificationParametersFile = new File(arg);

        // Output file
        arg = aLine.getOptionValue(StirredOptions.output.opt);

        if (!arg.endsWith(".mzid.gz")) {
            
            if (arg.endsWith(".gz")) {
                
                arg = arg.substring(0, arg.length() - 3);
                
            }

            if (!arg.endsWith(".mzid")) {

                arg = arg + ".mzid.gz";

            } else {

                arg = arg + ".gz";

            }
        }

        outputFile = new File(arg);

        // Log file
        if (aLine.hasOption(StirredOptions.log.opt)) {

            arg = aLine.getOptionValue(StirredOptions.log.opt);
            logFile = new File(arg);

        } else {

            logFile = new File(outputFile.getAbsolutePath() + ".log");

        }

        // Temp folder
        if (aLine.hasOption(StirredOptions.tempFolder.opt)) {

            arg = aLine.getOptionValue(StirredOptions.tempFolder.opt);
            tempFolder = new File(arg);

        } else {

            tempFolder = outputFile.getParentFile();

        }

        // Number of threads to use
        if (aLine.hasOption(StirredOptions.nThreads.opt)) {

            String argString = aLine.getOptionValue(StirredOptions.nThreads.opt);

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
        if (aLine.hasOption(StirredOptions.timeOutDays.opt)) {

            String argString = aLine.getOptionValue(StirredOptions.timeOutDays.opt);

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
        if (aLine.hasOption(StirredOptions.contactFirstName.opt)) {

            contactFirstName = aLine.getOptionValue(StirredOptions.contactFirstName.opt);

        }

        // The contact last name
        if (aLine.hasOption(StirredOptions.contactLastName.opt)) {

            contactLastName = aLine.getOptionValue(StirredOptions.contactLastName.opt);

        }

        // The contact address
        if (aLine.hasOption(StirredOptions.contactAddress.opt)) {

            contactAddress = aLine.getOptionValue(StirredOptions.contactAddress.opt);

        }

        // The contact email
        if (aLine.hasOption(StirredOptions.contactEmail.opt)) {

            contactEmail = aLine.getOptionValue(StirredOptions.contactEmail.opt);

        }

        // The contact organization name
        if (aLine.hasOption(StirredOptions.contactOrganizationName.opt)) {

            contactOrganizationName = aLine.getOptionValue(StirredOptions.contactOrganizationName.opt);

        }

        // The contact organization address
        if (aLine.hasOption(StirredOptions.contactOrganizationAddress.opt)) {

            contactOrganizationAddress = aLine.getOptionValue(StirredOptions.contactOrganizationAddress.opt);

        }

        // The contact organization email
        if (aLine.hasOption(StirredOptions.contactOrganizationEmail.opt)) {

            contactOrganizationEmail = aLine.getOptionValue(StirredOptions.contactOrganizationEmail.opt);

        }
    }
}
