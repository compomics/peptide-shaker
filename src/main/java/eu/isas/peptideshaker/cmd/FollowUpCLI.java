package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathParameters;
import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.UtilitiesUserParameters;
import eu.isas.peptideshaker.PeptideShaker;
import static eu.isas.peptideshaker.cmd.PeptideShakerCLI.redirectErrorStream;
import eu.isas.peptideshaker.preferences.PeptideShakerPathParameters;
import eu.isas.peptideshaker.utils.CpsParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Command line interface to run follow-up analysis on cps files.
 *
 * @author Marc Vaudel
 */
public class FollowUpCLI extends CpsParent {

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
     * The PTM factory.
     */
    private ModificationFactory ptmFactory;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
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
    public FollowUpCLI(FollowUpCLIInputBean followUpCLIInputBean) {
        this.followUpCLIInputBean = followUpCLIInputBean;
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     *
     * @return returns 1 if the process was canceled
     */
    public Object call() {

        PathSettingsCLIInputBean pathSettingsCLIInputBean = followUpCLIInputBean.getPathSettingsCLIInputBean();

        if (pathSettingsCLIInputBean.getLogFolder() != null) {
            redirectErrorStream(pathSettingsCLIInputBean.getLogFolder());
        }

        if (pathSettingsCLIInputBean.hasInput()) {
            PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
            pathSettingsCLI.setPathSettings();
        } else {
            try {
                setPathConfiguration();
            } catch (Exception e) {
                System.out.println("An error occurred when the setting path configurations. Default paths will be used.");
                e.printStackTrace();
            }
        }

        setDbFolder(PeptideShaker.getMatchesFolder());

        try {
            ArrayList<PathKey> errorKeys = PeptideShakerPathParameters.getErrorKeys();
            if (!errorKeys.isEmpty()) {
                System.out.println("Unable to write in the following configuration folders. Please use a temporary folder, "
                        + "the path configuration command line, or edit the configuration paths from the graphical interface.");
                for (PathKey pathKey : errorKeys) {
                    System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load the path configurations. Default paths will be used.");
            e.printStackTrace();
        }

        // Load user preferences
        utilitiesUserPreferences = UtilitiesUserParameters.loadUserParameters();

        // Instantiate factories
        PeptideShaker.instantiateFacories(utilitiesUserPreferences);
        ptmFactory = ModificationFactory.getInstance();
        enzymeFactory = EnzymeFactory.getInstance();

        // Load resources files
        loadSpecies();

        waitingHandler = new WaitingHandlerCLIImpl();

        String inputFilePath = null;

        try {
            if (followUpCLIInputBean.getZipFile() != null) {
                inputFilePath = followUpCLIInputBean.getZipFile().getAbsolutePath();
                loadCpsFromZipFile(followUpCLIInputBean.getZipFile(), PeptideShaker.getMatchesFolder(), waitingHandler);
            } else if (followUpCLIInputBean.getCpsFile() != null) {
                inputFilePath = followUpCLIInputBean.getCpsFile().getAbsolutePath();
                cpsFile = followUpCLIInputBean.getCpsFile();
                loadCpsFile(PeptideShaker.getMatchesFolder(), waitingHandler);
            } else {
                waitingHandler.appendReport("PeptideShaker project input missing.", true, true);
                return 1;
            }
        } catch (IOException e) {
            waitingHandler.appendReport("An error occurred while reading: " + inputFilePath + ".", true, true);
            e.printStackTrace();
            try {
                PeptideShakerCLI.closePeptideShaker(identification);
            } catch (Exception e2) {
                waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                e2.printStackTrace();
            }
            return 1;
        }

        // load the spectrum files
        try {
            if (!loadSpectrumFiles(waitingHandler)) {
                if (identification.getFractions().size() > 1) {
                    waitingHandler.appendReport("The spectrum files were not found. Please provide their location in the command line parameters.", true, true);
                } else {
                    waitingHandler.appendReport("The spectrum file was not found. Please provide its location in the command line parameters", true, true);
                }
                try {
                    PeptideShakerCLI.closePeptideShaker(identification);
                } catch (Exception e2) {
                    waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                    e2.printStackTrace();
                }
                return 1;
            }
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while loading the spectrum file(s).", true, true);
            e.printStackTrace();
            try {
                PeptideShakerCLI.closePeptideShaker(identification);
            } catch (Exception e2) {
                waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                e2.printStackTrace();
            }
            return 1;
        }
        
        // If not available on the computer, parse summary information about the fasta file
        try {
            
            loadFastaFile(waitingHandler);
            
        } catch (IOException e) {
            
            e.printStackTrace();
            waitingHandler.appendReport("An error occurred while parsing the fasta file.", true, true);
            waitingHandler.setRunCanceled();
            return 1;
            
        }

        // Load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // recalibrate spectra
        if (followUpCLIInputBean.recalibrationNeeded()) {
            try {
                CLIExportMethods.recalibrateSpectra(followUpCLIInputBean, identification, sequenceProvider, identificationParameters, waitingHandler);
                waitingHandler.appendReport("Recalibration process completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while recalibrating the spectra.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        // export spectra
        if (followUpCLIInputBean.spectrumExportNeeded()) {
            try {
                CLIExportMethods.exportSpectra(followUpCLIInputBean, identification, waitingHandler, identificationParameters.getSequenceMatchingParameters());
                waitingHandler.appendReport("Spectrum export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the spectra.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        // export protein accessions
        if (followUpCLIInputBean.accessionExportNeeded()) {
            try {
                CLIExportMethods.exportAccessions(followUpCLIInputBean, identification, sequenceProvider, waitingHandler, filterParameters);
                waitingHandler.appendReport("Protein accessions export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the protein accessions.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        // export protein details
        if (followUpCLIInputBean.fastaExportNeeded()) {
            try {
                CLIExportMethods.exportFasta(followUpCLIInputBean, identification, sequenceProvider, waitingHandler, filterParameters);
                waitingHandler.appendReport("Protein details export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the protein details.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        // progenesis export
        if (followUpCLIInputBean.progenesisExportNeeded()) {
            try {
                CLIExportMethods.exportProgenesis(followUpCLIInputBean, identification, waitingHandler, sequenceProvider, proteinDetailsProvider, identificationParameters.getSequenceMatchingParameters());
                waitingHandler.appendReport("Progenesis export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the Progenesis file.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        // inclusion list export
        if (followUpCLIInputBean.inclusionListNeeded()) {
            try {
                CLIExportMethods.exportInclusionList(followUpCLIInputBean, identification, identificationFeaturesGenerator, identificationParameters.getSearchParameters(), waitingHandler, filterParameters);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while generating the inclusion list.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        try {
            PeptideShakerCLI.closePeptideShaker(identification);
        } catch (Exception e2) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e2.printStackTrace();
            waitingHandler.setRunCanceled();
        }

        if (!waitingHandler.isRunCanceled()) {
            waitingHandler.appendReport("Follow-up export completed.", true, true);
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
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathParameters.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathParameters.loadPathParametersFromFile(pathConfigurationFile);
        }
    }

    /**
     * PeptideShaker follow-up CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker follow-up command line takes a cpsx file and generates various types of output files." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://compomics.github.io/projects/peptide-shaker.html and http://compomics.github.io/projects/peptide-shaker/wiki/peptideshakercli.html." + System.getProperty("line.separator")
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
        if (!aLine.hasOption(FollowUpCLIParams.CPS_FILE.id) || ((String) aLine.getOptionValue(FollowUpCLIParams.CPS_FILE.id)).equals("")) {
            System.out.println("\n" + FollowUpCLIParams.CPS_FILE.description + " not specified.\n");
            return false;
        } else {
            String fileTxt = aLine.getOptionValue(FollowUpCLIParams.CPS_FILE.id);
            File testFile = new File(fileTxt.trim());
            if (!testFile.exists()) {
                System.out.println("\n" + FollowUpCLIParams.CPS_FILE.description + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
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
            Options lOptions = new Options();
            FollowUpCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

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
