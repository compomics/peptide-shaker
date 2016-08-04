package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import eu.isas.peptideshaker.PeptideShaker;
import static eu.isas.peptideshaker.cmd.PeptideShakerCLI.redirectErrorStream;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
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
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;

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
            ArrayList<PathKey> errorKeys = PeptideShakerPathPreferences.getErrorKeys();
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

        // Initiate factories
        ptmFactory = PTMFactory.getInstance();
        enzymeFactory = EnzymeFactory.getInstance();

        // Load resources files
        loadEnzymes();
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
        } catch (SQLException e) {
            waitingHandler.appendReport("An error occurred while reading: " + inputFilePath + ". "
                    + "It looks like another instance of PeptideShaker is still connected to the file. "
                    + "Please close all instances of PeptideShaker and try again.", true, true);
            e.printStackTrace();
        } catch (Exception e) {
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

        // load fasta file
        try {
            if (!loadFastaFile(waitingHandler)) {
                waitingHandler.appendReport("The FASTA file was not found. Please provide its location in the command line parameters.", true, true);
                try {
                    PeptideShakerCLI.closePeptideShaker(identification);
                } catch (Exception e2) {
                    waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                    e2.printStackTrace();
                }
                return 1;
            }
            waitingHandler.appendReport("Protein database " + identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase().getName() + ".", true, true);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while loading the fasta file.", true, true);
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
                if (identification.getSpectrumFiles().size() > 1) {
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

        // Load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // recalibrate spectra
        if (followUpCLIInputBean.recalibrationNeeded()) {
            try {
                CLIMethods.recalibrateSpectra(followUpCLIInputBean, identification, identificationParameters, waitingHandler);
                waitingHandler.appendReport("Recalibration process completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while recalibrating the spectra.", true, true);
                e.printStackTrace();
            }
        }

        // export spectra
        if (followUpCLIInputBean.spectrumExportNeeded()) {
            try {
                CLIMethods.exportSpectra(followUpCLIInputBean, identification, waitingHandler, identificationParameters.getSequenceMatchingPreferences());
                waitingHandler.appendReport("Spectrum export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the spectra.", true, true);
                e.printStackTrace();
            }
        }

        // export protein accessions
        if (followUpCLIInputBean.accessionExportNeeded()) {
            try {
                CLIMethods.exportAccessions(followUpCLIInputBean, identification, identificationFeaturesGenerator, waitingHandler, filterPreferences);
                waitingHandler.appendReport("Protein accessions export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the protein accessions.", true, true);
                e.printStackTrace();
            }
        }

        // export protein details
        if (followUpCLIInputBean.fastaExportNeeded()) {
            try {
                CLIMethods.exportFasta(followUpCLIInputBean, identification, identificationFeaturesGenerator, waitingHandler, filterPreferences);
                waitingHandler.appendReport("Protein details export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the protein details.", true, true);
                e.printStackTrace();
            }
        }

        // progenesis export
        if (followUpCLIInputBean.progenesisExportNeeded()) {
            try {
                CLIMethods.exportProgenesis(followUpCLIInputBean, identification, waitingHandler, identificationParameters.getSequenceMatchingPreferences());
                waitingHandler.appendReport("Progenesis export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the Progenesis file.", true, true);
                e.printStackTrace();
            }
        }

        // PepNovo training export
        if (followUpCLIInputBean.pepnovoTrainingExportNeeded()) {
            try {
                CLIMethods.exportPepnovoTrainingFiles(followUpCLIInputBean, identification, identificationParameters, waitingHandler);
                waitingHandler.appendReport("Pepnovo training export completed.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while exporting the Pepnovo training file.", true, true);
                e.printStackTrace();
            }
        }

        // inclusion list export
        if (followUpCLIInputBean.inclusionListNeeded()) {
            try {
                CLIMethods.exportInclusionList(followUpCLIInputBean, identification, identificationFeaturesGenerator, identificationParameters.getSearchParameters(), waitingHandler, filterPreferences);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while generating the inclusion list.", true, true);
                e.printStackTrace();
            }
        }

        try {
            closePeptideShaker();
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e.printStackTrace();
        }

        waitingHandler.appendReport("Follow-up export completed.", true, true);

        try {
            PeptideShakerCLI.closePeptideShaker(identification);
        } catch (Exception e2) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e2.printStackTrace();
        }
        System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
        // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!

        return null;
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
    }

    /**
     * Close the PeptideShaker instance by clearing up factories and cache.
     *
     * @throws IOException thrown of IOException occurs
     * @throws SQLException thrown if SQLException occurs
     */
    public void closePeptideShaker() throws IOException, SQLException {

        SpectrumFactory.getInstance().closeFiles();
        SequenceFactory.getInstance().closeFile();
        identification.close();

        DerbyUtil.closeConnection();

        File matchFolder = PeptideShaker.getMatchesFolder();
        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                Util.deleteDir(currentFile);
            }
        }
    }

    /**
     * PeptideShaker follow-up CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker follow-up command line takes a cpsx file and generates various types of output files." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://compomics.github.io/projects/peptide-shaker.html and http://compomics.github.io/peptide-shaker/wiki/peptideshakercli.html." + System.getProperty("line.separator")
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
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            File lEnzymeFile = new File(PeptideShaker.getJarFilePath() + File.separator + PeptideShaker.ENZYME_FILE);
            enzymeFactory.importEnzymes(lEnzymeFile);
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
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
