package eu.isas.peptideshaker.cmd;

import com.compomics.software.log.CliLogger;
import com.compomics.util.Util;
import static com.compomics.util.Util.LINE_SEPARATOR;
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

            StirredOptionsBean modificationScoreOptionsBean = new StirredOptionsBean(commandLine);

            try ( CliLogger cliLogger = new CliLogger(
                    modificationScoreOptionsBean.logFile,
                    "compomics-utilities",
                    Util.getVersion()
            )) {

                cliLogger.writeComment("Software", "compomics-utilities");
                cliLogger.writeComment("Version", Util.getVersion());
                cliLogger.writeComment("CLI", "ModificationScoreCLI");
                cliLogger.writeComment("Command", String.join(" ", args));

                run(
                        modificationScoreOptionsBean,
                        cliLogger
                );

            }

        } catch (Throwable e) {

            e.printStackTrace();
        }
    }

    /**
     * Runs the command.
     *
     * @param bean the bean of command line parameters
     */
    private static void run(
            StirredOptionsBean bean,
            CliLogger cliLogger
    ) {

        

    }

    /**
     * Prints basic help
     */
    private static void printHelp() {

        try ( PrintWriter lPrintWriter = new PrintWriter(System.out)) {
            lPrintWriter.print(LINE_SEPARATOR);
            lPrintWriter.print("==================================" + LINE_SEPARATOR);
            lPrintWriter.print("       CompOmics Utilities        " + LINE_SEPARATOR);
            lPrintWriter.print("               ****               " + LINE_SEPARATOR);
            lPrintWriter.print("        Modification Score        " + LINE_SEPARATOR);
            lPrintWriter.print("==================================" + LINE_SEPARATOR);
            lPrintWriter.print(LINE_SEPARATOR
                    + "The Modification Score CLI scores the modification localization of peptides." + LINE_SEPARATOR
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
