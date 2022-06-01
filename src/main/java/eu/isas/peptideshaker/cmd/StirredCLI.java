package eu.isas.peptideshaker.cmd;

import com.compomics.software.log.CliLogger;
import com.compomics.util.Util;
import static com.compomics.util.Util.LINE_SEPARATOR;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.stirred.Stirred;
import java.io.PrintWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Command line to manage the modifications.
 *
 * @author Marc Vaudel
 */
public class StirredCLI {

    /**
     * Main method.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        if (args.length == 0
                || args.length == 1 && args[0].equals("-h")
                || args.length == 1 && args[0].equals("--help")) {

            printHelp();
            return;

        }

        if (args.length == 1 && args[0].equals("-v")
                || args.length == 1 && args[0].equals("--version")) {

            System.out.println(Util.getVersion());

            return;

        }

        try {

            Options lOptions = new Options();
            StirredOptions.createOptionsCLI(lOptions);
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(lOptions, args);

            StirredOptionsBean optionBean = new StirredOptionsBean(commandLine);

            try ( CliLogger cliLogger = new CliLogger(
                    optionBean.logFile,
                    "PeptideShaker",
                    Util.getVersion()
            )) {

                cliLogger.writeComment("Software", "PeptideShaker");
                cliLogger.writeComment("Version", PeptideShaker.getVersion());
                cliLogger.writeComment("CLI", "StirredCLI");
                cliLogger.writeComment("Command", String.join(" ", args));

                Stirred stirred = new Stirred(
                        optionBean.inputFile,
                        optionBean.spectrumFile,
                        optionBean.fastaFile,
                        optionBean.outputFolder,
                        optionBean.identificationParametersFile,
                        optionBean.tempFolder,
                        cliLogger,
                        optionBean.nThreads,
                        optionBean.timeOutDays,
                        optionBean.contactFirstName,
                        optionBean.contactLastName,
                        optionBean.contactAddress,
                        optionBean.contactEmail,
                        optionBean.contactOrganizationName,
                        optionBean.contactOrganizationAddress,
                        optionBean.contactOrganizationEmail
                );

                stirred.run();

            }

        } catch (Throwable e) {

            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints basic help
     */
    private static void printHelp() {

        try ( PrintWriter lPrintWriter = new PrintWriter(System.out)) {
            lPrintWriter.print(LINE_SEPARATOR);
            lPrintWriter.print("==================================" + LINE_SEPARATOR);
            lPrintWriter.print("          PeptideShaker           " + LINE_SEPARATOR);
            lPrintWriter.print("               ****               " + LINE_SEPARATOR);
            lPrintWriter.print("              Stirred             " + LINE_SEPARATOR);
            lPrintWriter.print("==================================" + LINE_SEPARATOR);
            lPrintWriter.print(LINE_SEPARATOR
                    + "The Stirred CLI maps peptides to proteins, scores modification localization, and exports mzIdentML from search engine results." + LINE_SEPARATOR
                    + LINE_SEPARATOR
                    + "For documentation and bug report please refer to our code repository github.com/compomics/compomics-utilities/." + LINE_SEPARATOR
                    + LINE_SEPARATOR
                    + "----------------------"
                    + LINE_SEPARATOR
                    + "OPTIONS"
                    + LINE_SEPARATOR
                    + "----------------------" + LINE_SEPARATOR
                    + LINE_SEPARATOR);
            lPrintWriter.print(StirredOptions.getOptionsAsString());
            lPrintWriter.flush();
        }
    }
}
