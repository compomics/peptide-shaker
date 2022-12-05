package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.UtilitiesUserParameters;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.utils.PsdbParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Command line interface to run follow-up analysis on psdb files.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FollowUpCLI extends PsdbParent {

    /**
     * The follow up options.
     */
    private FollowUpCLIInputBean followUpCLIInputBean = null;
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserParameters utilitiesUserPreferences;

    /**
     * Construct a new FollowUpCLI runnable from a FollowUpCLI Bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired output files.
     *
     * @param followUpCLIInputBean the follow-up options
     */
    public FollowUpCLI(
            FollowUpCLIInputBean followUpCLIInputBean
    ) {

        this.followUpCLIInputBean = followUpCLIInputBean;

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
        ModificationFactory.getInstance();
        EnzymeFactory.getInstance();

        // Load resources files
        loadSpecies();

        waitingHandler = new WaitingHandlerCLIImpl();

        String inputFilePath = null;

        try {

            if (followUpCLIInputBean.getZipFile() != null) {

                inputFilePath = followUpCLIInputBean.getZipFile().getAbsolutePath();
                loadPsdbFromZipFile(
                        followUpCLIInputBean.getZipFile(),
                        PeptideShaker.getMatchesFolder(),
                        waitingHandler
                );

            } else if (followUpCLIInputBean.getPsdbFile() != null) {

                inputFilePath = followUpCLIInputBean.getPsdbFile().getAbsolutePath();
                psdbFile = followUpCLIInputBean.getPsdbFile();
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

                waitingHandler.appendReport(
                        "An error occurred while closing PeptideShaker.",
                        true,
                        true
                );
                e2.printStackTrace();

            }
            return 1;
        }

        // load the spectrum files
        try {

            if (!loadSpectrumFiles(waitingHandler)) {

                if (identification.getFractions().size() > 1) {

                    waitingHandler.appendReport(
                            "The spectrum files were not found. Please provide their location in the command line parameters.",
                            true,
                            true
                    );

                } else {

                    waitingHandler.appendReport(
                            "The spectrum file was not found. Please provide its location in the command line parameters",
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

        // if not available on the computer, parse summary information about the FASTA file
        try {

            loadFastaFile(waitingHandler);

        } catch (IOException e) {

            e.printStackTrace();

            waitingHandler.appendReport(
                    "An error occurred while parsing the FASTA file.",
                    true,
                    true
            );

            waitingHandler.setRunCanceled();

            return 1;

        }

        // load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // recalibrate spectra
        if (followUpCLIInputBean.recalibrationNeeded()) {

            waitingHandler.appendReport("Recalibration of spectra.", true, true);

            try {

                CLIExportMethods.recalibrateSpectra(
                        followUpCLIInputBean,
                        identification,
                        sequenceProvider,
                        msFileHandler,
                        identificationParameters,
                        waitingHandler
                );
                waitingHandler.appendReport(
                        "Recalibration process completed.",
                        true,
                        true
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while recalibrating the spectra.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // export spectra
        if (followUpCLIInputBean.spectrumExportNeeded()) {

            waitingHandler.appendReport("Spectrum export.", true, true);

            try {

                CLIExportMethods.exportSpectra(
                        followUpCLIInputBean,
                        identification,
                        msFileHandler,
                        waitingHandler,
                        identificationParameters.getSequenceMatchingParameters()
                );

                waitingHandler.appendReport(
                        "Spectrum export completed.",
                        true,
                        true
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while exporting the spectra.",
                        true,
                        true
                );
                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // export protein accessions
        if (followUpCLIInputBean.accessionExportNeeded()) {

            waitingHandler.appendReport("Protein accession export.", true, true);

            try {

                CLIExportMethods.exportAccessions(
                        followUpCLIInputBean,
                        identification,
                        sequenceProvider,
                        waitingHandler,
                        filterParameters
                );
                waitingHandler.appendReport(
                        "Protein accessions export completed.",
                        true,
                        true
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while exporting the protein accessions.",
                        true,
                        true
                );
                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // export protein details
        if (followUpCLIInputBean.proteinSequencesExportNeeded()) {

            waitingHandler.appendReport("Protein sequences export.", true, true);

            try {

                CLIExportMethods.exportProteinSequences(
                        followUpCLIInputBean,
                        identification,
                        sequenceProvider,
                        waitingHandler,
                        filterParameters
                );
                waitingHandler.appendReport(
                        "Protein details export completed.",
                        true,
                        true
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while exporting the protein details.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // progenesis export
        if (followUpCLIInputBean.progenesisExportNeeded()) {

            waitingHandler.appendReport("Progenesis export.", true, true);

            try {

                CLIExportMethods.exportProgenesis(
                        followUpCLIInputBean,
                        identification,
                        waitingHandler,
                        sequenceProvider,
                        proteinDetailsProvider,
                        identificationParameters.getSequenceMatchingParameters()
                );
                waitingHandler.appendReport(
                        "Progenesis export completed.",
                        true,
                        true
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while exporting the Progenesis file.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // inclusion list export
        if (followUpCLIInputBean.inclusionListNeeded()) {

            waitingHandler.appendReport("Inclusion list export.", true, true);

            try {

                CLIExportMethods.exportInclusionList(
                        followUpCLIInputBean,
                        identification,
                        identificationFeaturesGenerator,
                        msFileHandler,
                        identificationParameters.getSearchParameters(),
                        waitingHandler,
                        filterParameters
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the inclusion list.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // proteoforms export
        if (followUpCLIInputBean.proteoformsNeeded()) {

            waitingHandler.appendReport("Proteoform export.", true, true);

            try {

                CLIExportMethods.exportProteoforms(
                        followUpCLIInputBean,
                        identification,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the proteoforms list.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // DeepLC export
        if (followUpCLIInputBean.deepLcExportNeeded()) {

            waitingHandler.appendReport("DeepLC export.", true, true);

            try {

                CLIExportMethods.exportDeepLC(
                        followUpCLIInputBean,
                        identification,
                        identificationParameters.getSearchParameters().getModificationParameters(),
                        identificationParameters.getSequenceMatchingParameters(),
                        sequenceProvider,
                        msFileHandler,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the DeepLC export.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // ms2pip export
        if (followUpCLIInputBean.ms2pipExportNeeded()) {

            waitingHandler.appendReport("ms2pip export.", true, true);

            try {

                CLIExportMethods.exportMs2pip(
                        followUpCLIInputBean,
                        identification,
                        identificationParameters.getSearchParameters(),
                        identificationParameters.getSequenceMatchingParameters(),
                        sequenceProvider,
                        msFileHandler,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the MS2PIP export.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }

        // Percolator export
        if (followUpCLIInputBean.percolatorExportNeeded()) {

            waitingHandler.appendReport("Percolator export.", true, true);

            try {

                CLIExportMethods.exportPercolator(
                        followUpCLIInputBean,
                        identification,
                        identificationParameters.getSearchParameters(),
                        identificationParameters.getSequenceMatchingParameters(),
                        identificationParameters.getAnnotationParameters(),
                        identificationParameters.getModificationLocalizationParameters(),
                        identificationParameters.getSearchParameters().getModificationParameters(),
                        sequenceProvider,
                        msFileHandler,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the Percolator export.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }
        
        // RT values export
        if (followUpCLIInputBean.RTValuesExportNeeded()) {

            waitingHandler.appendReport("RT values export.", true, true);

            try {

                CLIExportMethods.exportRTValues(
                        followUpCLIInputBean,
                        identification,
                        identificationParameters.getSearchParameters(),
                        identificationParameters.getSequenceMatchingParameters(),
                        identificationParameters.getAnnotationParameters(),
                        identificationParameters.getModificationLocalizationParameters(),
                        identificationParameters.getSearchParameters().getModificationParameters(),
                        sequenceProvider,
                        msFileHandler,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the RT values export.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }
        
        // PSM identifiers export
        if (followUpCLIInputBean.PSMIdentifiersExportNeeded()) {

            waitingHandler.appendReport("PSM identifiers export.", true, true);

            try {

                CLIExportMethods.exportPSMIdentifiers(
                        followUpCLIInputBean,
                        identification,
                        identificationParameters.getSearchParameters().getModificationParameters(),
                        sequenceProvider,
                        identificationParameters.getSequenceMatchingParameters(),
                        msFileHandler,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the PSM identifiers export.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

            }
        }
        
        // peaks intensities export
        if (followUpCLIInputBean.peaksIntensitiesObsExportNeeded()) {

            waitingHandler.appendReport("Peaks intensities export.", true, true);

            try {

                CLIExportMethods.exportPeaksIntensities(
                        followUpCLIInputBean,
                        identification,
                        identificationParameters.getSearchParameters(),
                        identificationParameters.getSequenceMatchingParameters(),
                        identificationParameters.getSearchParameters().getModificationParameters(),
                        sequenceProvider,
                        msFileHandler,
                        waitingHandler
                );

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while generating the peaks intensities export.",
                        true,
                        true
                );

                e.printStackTrace();
                waitingHandler.setRunCanceled();

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
            waitingHandler.setRunCanceled();

        }

        if (!waitingHandler.isRunCanceled()) {

            waitingHandler.appendReport(
                    "Follow-up export completed.",
                    true,
                    true
            );

            System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.

            // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!
            return 0;

        } else {

            System.exit(1); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.

            // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!
            return 1;

        }
    }

    /**
     * PeptideShaker follow-up CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker follow-up command line takes a psdb file and generates various types of output files." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see https://compomics.github.io/projects/peptide-shaker.html and https://compomics.github.io/projects/peptide-shaker/wiki/PeptideshakerCLI.html." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/peptide-shaker." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator");
    }

    /**
     * Verifies the command line start parameters.
     *
     * @return true if the startup was valid
     */
    private static boolean isValidStartup(CommandLine aLine) throws IOException {

        if (aLine.getOptions().length == 0) {
            return false;
        }
        if (!aLine.hasOption(FollowUpCLIParams.PSDB_FILE.id) || ((String) aLine.getOptionValue(FollowUpCLIParams.PSDB_FILE.id)).equals("")) {
            System.out.println("\n" + FollowUpCLIParams.PSDB_FILE.description + " not specified.\n");
            return false;
        } else {
            String fileTxt = aLine.getOptionValue(FollowUpCLIParams.PSDB_FILE.id);
            File testFile = new File(fileTxt.trim());
            if (!testFile.exists()) {
                System.out.println("\n" + FollowUpCLIParams.PSDB_FILE.description + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
                return false;
            }
        }

        return true;
    }

    /**
     * Loads the species from the species file into the species factory.
     */
    private void loadSpecies() {
        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(PeptideShaker.getJarFilePath());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the species.");
            e.printStackTrace();
        }
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
            FollowUpCLIParams.createOptionsCLI(nonPathOptions);
            DefaultParser parser = new DefaultParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "========================================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker Follow Up - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("========================================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(FollowUpCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                FollowUpCLIInputBean lCLIBean = new FollowUpCLIInputBean(line);
                FollowUpCLI followUpCLI = new FollowUpCLI(lCLIBean);
                followUpCLI.call();
            }
        } catch (OutOfMemoryError e) {
            System.out.println("<CompomicsError>PeptideShaker used up all the memory and had to be stopped. See the PeptideShaker log for details.</CompomicsError>");
            System.err.println("Ran out of memory!");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.print("<CompomicsError>PeptideShaker processing failed. See the PeptideShaker log for details.</CompomicsError>");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "FollowUpCLI{"
                + ", cliInputBean=" + followUpCLIInputBean
                + '}';
    }
}
