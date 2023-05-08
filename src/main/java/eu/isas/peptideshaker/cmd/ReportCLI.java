package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.UtilitiesUserParameters;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.utils.PsdbParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.SQLException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * This class performs the command line export of reports in command line.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReportCLI extends PsdbParent {

    /**
     * The report command line options.
     */
    private final ReportCLIInputBean reportCLIInputBean;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The modification factory.
     */
    private ModificationFactory modificationFactory;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserParameters utilitiesUserPreferences;

    /**
     * Construct a new ReportCLI runnable from a ReportCLI Bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired output files.
     *
     * @param reportCLIInputBean the input bean
     */
    public ReportCLI(ReportCLIInputBean reportCLIInputBean) {
        this.reportCLIInputBean = reportCLIInputBean;
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     *
     * @return returns 1 if the process was canceled
     */
    public Object call() {

        // turn off illegal access log messages
        try {
            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Long offset = (Long) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, loggerField);
            unsafeClass.getMethod("putObjectVolatile", Object.class, long.class, Object.class) //
                    .invoke(unsafe, loggerClass, offset, null);
        } catch (Throwable ex) {
            // ignore, i.e. simply show the warnings...
            //ex.printStackTrace();
        }

        setDbFolder(PeptideShaker.getMatchesFolder());

        // Load user preferences
        utilitiesUserPreferences = UtilitiesUserParameters.loadUserParameters();

        // Instantiate factories
        PeptideShaker.instantiateFacories(utilitiesUserPreferences);
        modificationFactory = ModificationFactory.getInstance();
        enzymeFactory = EnzymeFactory.getInstance();

        // Load resources files
        loadSpecies();

        waitingHandler = new WaitingHandlerCLIImpl();

        String inputFilePath = null;

        try {

            if (reportCLIInputBean.getZipFile() != null) {

                inputFilePath = reportCLIInputBean.getZipFile().getAbsolutePath();
                loadPsdbFromZipFile(reportCLIInputBean.getZipFile(), PeptideShaker.getMatchesFolder(), waitingHandler);

            } else if (reportCLIInputBean.getPsdbFile() != null) {

                inputFilePath = reportCLIInputBean.getPsdbFile().getAbsolutePath();
                psdbFile = reportCLIInputBean.getPsdbFile();
                loadPsdbFile(PeptideShaker.getMatchesFolder(), waitingHandler, false);

            } else {

                waitingHandler.appendReport(
                        "PeptideShaker project input missing.",
                        true,
                        true
                );

                return 1;

            }

        } catch (IOException e) {

            waitingHandler.appendReport(
                    "An error occurred while reading: " + inputFilePath + ".",
                    true,
                    true
            );

            e.printStackTrace();

            try {

                PeptideShakerCLI.closePeptideShaker(identification);

            } catch (Exception e2) {
                // Ignore
            }

            return 1;

        }

        // load the spectrum files
        try {

            if (!loadSpectrumFiles(waitingHandler)) {

                if (identification.getFractions().size() > 1) {

                    waitingHandler.appendReport(
                            "The spectrum files were not found. Please provide "
                            + "their location in the command line parameters.",
                            true,
                            true
                    );

                } else {

                    waitingHandler.appendReport(
                            "The spectrum file was not found. Please provide its "
                            + "location in the command line parameters.",
                            true,
                            true
                    );

                }
                try {

                    PeptideShakerCLI.closePeptideShaker(identification);

                } catch (Exception e2) {

                    waitingHandler.appendReport(
                            "An error occurred while closing PeptideShaker.",
                            true,
                            true
                    );

                    e2.printStackTrace();

                }

                return 1;

            }
        } catch (Exception e) {

            waitingHandler.appendReport(
                    "An error occurred while loading the spectrum file(s).",
                    true,
                    true
            );

            e.printStackTrace();

            try {

                PeptideShakerCLI.closePeptideShaker(identification);

            } catch (Exception e2) {

                waitingHandler.appendReport(
                        "An error occurred while closing PeptideShaker.",
                        true,
                        true
                );

                e2.printStackTrace();

            }

            return 1;

        }

        // If not available on the computer, parse summary information about the fasta file
        try {

            loadFastaFile(waitingHandler);

        } catch (IOException e) {

            e.printStackTrace();

            waitingHandler.appendReport(
                    "An error occurred while parsing the fasta file.",
                    true,
                    true
            );

            waitingHandler.setRunCanceled();
            return 1;

        }

        // Load project-specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // export report(s)
        if (reportCLIInputBean.exportNeeded()) {

            for (String reportType : reportCLIInputBean.getReportTypes()) {

                try {

                    CLIExportMethods.exportReport(
                            reportCLIInputBean,
                            reportType,
                            projectParameters.getProjectUniqueName(),
                            projectDetails,
                            identification,
                            geneMaps,
                            identificationFeaturesGenerator,
                            identificationParameters,
                            sequenceProvider,
                            proteinDetailsProvider,
                            msFileHandler,
                            displayParameters.getnAASurroundingPeptides(),
                            spectrumCountingParameters,
                            waitingHandler
                    );

                } catch (Exception e) {

                    waitingHandler.appendReport(
                            "An error occurred while exporting the " + reportType + ".",
                            true,
                            true
                    );

                    e.printStackTrace();
                    waitingHandler.setRunCanceled();

                }
            }
        }

        // export documentation(s)
        if (reportCLIInputBean.documentationExportNeeded()) {

            for (String reportType : reportCLIInputBean.getReportTypes()) {

                try {

                    CLIExportMethods.exportDocumentation(
                            reportCLIInputBean,
                            reportType,
                            waitingHandler
                    );

                } catch (Exception e) {

                    waitingHandler.appendReport(
                            "An error occurred while exporting the documentation for " + reportType + ".",
                            true,
                            true
                    );

                    e.printStackTrace();
                    waitingHandler.setRunCanceled();

                }
            }
        }

        try {

            PeptideShakerCLI.closePeptideShaker(identification);
        } catch (Exception e2) {

            waitingHandler.appendReport(
                    "An error occurred while closing PeptideShaker.",
                    true,
                    true
            );

            e2.printStackTrace();

        }

        if (!waitingHandler.isRunCanceled()) {

            waitingHandler.appendReport(
                    "Report export completed.",
                    true,
                    true
            );

            System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.

            // Note that if a different solution is found, the DummyFrame has to
            // be closed similar to the setVisible method in the WelcomeDialog!!
            return 0;

        } else {

            System.exit(1); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.

            // Note that if a different solution is found, the DummyFrame has to 
            // be closed similar to the setVisible method in the WelcomeDialog!!
            return 1;
        }
    }

    /**
     * PeptideShaker report CLI header message when printing the usage.
     */
    private static String getHeader() {

        return System.getProperty("line.separator")
                + "The PeptideShaker report command line takes a psdb file and "
                + "generates various types of reports."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see https://compomics.github.io/projects/peptide-shaker.html "
                + "and https://compomics.github.io/projects/peptide-shaker/wiki/PeptideShakerCLI.html."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/peptide-shaker."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + System.getProperty("line.separator");

    }

    /**
     * Verifies the command line start parameters.
     *
     * @return true if the startup was valid
     */
    private static boolean isValidStartup(
            CommandLine aLine
    ) throws IOException {

        if (aLine.getOptions().length == 0) {
            return false;
        }

        if (!aLine.hasOption(ReportCLIParams.PSDB_FILE.id)
                || ((String) aLine.getOptionValue(ReportCLIParams.PSDB_FILE.id)).equals("")) {

            System.out.println(
                    "\n"
                    + ReportCLIParams.PSDB_FILE.description
                    + " not specified.\n"
            );

            return false;

        } else {

            String fileTxt = aLine.getOptionValue(ReportCLIParams.PSDB_FILE.id);
            File testFile = new File(fileTxt.trim());

            if (!testFile.exists()) {

                System.out.println(
                        "\n"
                        + ReportCLIParams.PSDB_FILE.description
                        + " \'"
                        + testFile.getAbsolutePath()
                        + "\' not found.\n"
                );

                return false;
            }

        }

        if (!aLine.hasOption(ReportCLIParams.EXPORT_FOLDER.id)
                || ((String) aLine.getOptionValue(ReportCLIParams.EXPORT_FOLDER.id)).equals("")) {

            System.out.println(
                    "\n"
                    + ReportCLIParams.EXPORT_FOLDER.description
                    + " not specified.\n"
            );

            return false;

        } else {

            String fileTxt = aLine.getOptionValue(ReportCLIParams.EXPORT_FOLDER.id);
            File testFile = new File(fileTxt.trim());

            if (!testFile.exists()) {

                System.out.println(
                        "\n"
                        + ReportCLIParams.EXPORT_FOLDER.description
                        + " \'"
                        + testFile.getAbsolutePath()
                        + "\' not found.\n"
                );

                return false;
            }

        }

        return true;
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            // check if there are updates to the paths
            String[] nonPathSettingArgsAsList = PathSettingsCLI.extractAndUpdatePathOptions(args);

            // parse the rest of the options   
            Options nonPathOptions = new Options();
            ReportCLIParams.createOptionsCLI(nonPathOptions);
            DefaultParser parser = new DefaultParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

            if (!isValidStartup(line)) {

                PrintWriter lPrintWriter = new PrintWriter(System.out);

                lPrintWriter.print(
                        System.getProperty("line.separator")
                        + "==============================================="
                        + System.getProperty("line.separator")
                );

                lPrintWriter.print(
                        "PeptideShaker Report Exporter - Command Line"
                        + System.getProperty("line.separator")
                );

                lPrintWriter.print(
                        "==============================================="
                        + System.getProperty("line.separator")
                );

                lPrintWriter.print(getHeader());
                lPrintWriter.print(ReportCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);

            } else {

                ReportCLIInputBean lCLIBean = new ReportCLIInputBean(line);
                ReportCLI cli = new ReportCLI(lCLIBean);
                cli.call();

            }
        } catch (OutOfMemoryError e) {

            System.out.println(
                    "<CompomicsError>PeptideShaker used up all the memory and "
                    + "had to be stopped. See the PeptideShaker log for details."
                    + "</CompomicsError>"
            );

            System.err.println("Ran out of memory!");

            System.err.println(
                    "Memory given to the Java virtual machine: "
                    + Runtime.getRuntime().maxMemory()
                    + "."
            );

            System.err.println(
                    "Memory used by the Java virtual machine: "
                    + Runtime.getRuntime().totalMemory()
                    + "."
            );

            System.err.println(
                    "Free memory in the Java virtual machine: "
                    + Runtime.getRuntime().freeMemory()
                    + "."
            );

            e.printStackTrace();
            System.exit(1);

        } catch (Exception e) {

            System.out.print(
                    "<CompomicsError>PeptideShaker processing failed. See the "
                    + "PeptideShaker log for details.</CompomicsError>"
            );

            e.printStackTrace();
            System.exit(1);

        }
    }

    @Override
    public String toString() {
        return "FollowUpCLI{" + ", cliInputBean=" + reportCLIInputBean + '}';
    }

    /**
     * Close the PeptideShaker instance by clearing up factories and cache.
     *
     * @throws IOException thrown if an exception occurred while closing the
     * connection to a file
     * @throws SQLException thrown if an exception occurred while closing the
     * connection to a database
     * @throws java.lang.InterruptedException if a thread was interrupted when
     * closing the database
     * @throws java.lang.ClassNotFoundException if a class was not found when
     * emptying the cache
     */
    public void closePeptideShaker() throws IOException, SQLException, InterruptedException, ClassNotFoundException {

        identification.close(false);

        File matchFolder = PeptideShaker.getMatchesFolder();

        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                IoUtil.deleteDir(currentFile);
            }
        }
    }

    /**
     * Loads the species from the species file into the species factory.
     */
    private void loadSpecies() {
        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(PeptideShaker.getConfigFolder());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the species.");
            e.printStackTrace();
        }
    }
}
