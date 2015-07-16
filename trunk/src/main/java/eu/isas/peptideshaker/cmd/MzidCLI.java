package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
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
 * Command line interface to export mzid files from cps files.
 *
 * @author Marc Vaudel
 */
public class MzidCLI extends CpsParent {

    /**
     * The mzid creation options.
     */
    private MzidCLIInputBean mzidCLIInputBean = null;
    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();

    /**
     * Construct a new MzidCLI runnable from a MzidCLI input bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired mzid file.
     *
     * @param mzidCLIInputBean the mzId creation options
     */
    public MzidCLI(MzidCLIInputBean mzidCLIInputBean) {
        this.mzidCLIInputBean = mzidCLIInputBean;
        loadEnzymes();
        loadPtms();
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     *
     * @return returns 1 if the process was canceled
     */
    public Object call() {

        PathSettingsCLIInputBean pathSettingsCLIInputBean = mzidCLIInputBean.getPathSettingsCLIInputBean();
        if (pathSettingsCLIInputBean.hasInput()) {
            PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
            pathSettingsCLI.setPathSettings();
        } else {
            try {
                setPathConfiguration();
            } catch (Exception e) {
                System.out.println("An error occurred when setting the path configurations. Default paths will be used.");
                e.printStackTrace();
            }
        }
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
        }

        waitingHandler = new WaitingHandlerCLIImpl();

        String inputFilePath = null;

        try {
            if (mzidCLIInputBean.getZipFile() != null) {
                inputFilePath = mzidCLIInputBean.getZipFile().getAbsolutePath();
                loadCpsFromZipFile(mzidCLIInputBean.getZipFile(), PeptideShaker.getJarFilePath(), waitingHandler);
            } else if (mzidCLIInputBean.getCpsFile() != null) {
                inputFilePath = mzidCLIInputBean.getCpsFile().getAbsolutePath();
                cpsFile = mzidCLIInputBean.getCpsFile();
                loadCpsFile(PeptideShaker.getJarFilePath(), waitingHandler);
            } else {
                waitingHandler.appendReport("PeptideShaker project input missing.", true, true);
                return 1;
            }
        } catch (SQLException e) {
            waitingHandler.appendReport("An error occurred while reading: " + inputFilePath + ". "
                    + "It looks like another instance of PeptideShaker is still connected to the file. "
                    + "Please close all instances of PeptideShaker and try again.", true, true);
            e.printStackTrace();
            waitingHandler.appendReport(inputFilePath + " successfuly loaded.", true, true);
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
                waitingHandler.appendReport("The FASTA file was not found, please locate it using the GUI.", true, true);
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
            waitingHandler.appendReport("An error occurred while loading the FASTA file.", true, true);
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
                    waitingHandler.appendReport("The spectrum files were not found, please locate them using the GUI.", true, true);
                } else {
                    waitingHandler.appendReport("The spectrum file was not found, please locate it using the GUI.", true, true);
                }
                try {
                    PeptideShakerCLI.closePeptideShaker(identification);
                } catch (Exception e2) {
                    waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                    e2.printStackTrace();
                }
                return 1;
            }
            waitingHandler.appendReport("Spectrum file(s) successfully loaded.", true, true);
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

        loadGeneMappings(PeptideShaker.getJarFilePath(), waitingHandler);

        // export mzid file
        // make sure that all annotations are included
        double currentIntensityLimit = this.getIdentificationParameters().getAnnotationPreferences().getAnnotationIntensityLimit();
        this.getIdentificationParameters().getAnnotationPreferences().setAnnotationLevel(0.0);
        
        try {
            CLIMethods.exportMzId(mzidCLIInputBean, this, waitingHandler);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while generating the mzid file.", true, true);
            e.printStackTrace();
        } finally {
            // reset the annotation level
            this.getIdentificationParameters().getAnnotationPreferences().setAnnotationLevel(currentIntensityLimit);
        }

        try {
            closePeptideShaker();
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e.printStackTrace();
        }

        try {
            PeptideShakerCLI.closePeptideShaker(identification);
        } catch (Exception e2) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e2.printStackTrace();
        }
        waitingHandler.appendReport("MzIdentML export completed.", true, true);

        System.exit(0);

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

        File matchFolder = PeptideShaker.getSerializationDirectory(PeptideShaker.getJarFilePath());
        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                Util.deleteDir(currentFile);
            }
        }
    }

    /**
     * PeptideShaker mzid CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker mzid command line takes a cps file and export the identification results in the mzIdentML format." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://peptide-shaker.googlecode.com and http://code.google.com/p/peptide-shaker/wiki/PeptideShakerCLI." + System.getProperty("line.separator")
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
        for (MzidCLIParams mzidCLIParam : MzidCLIParams.values()) {
            if (mzidCLIParam.mandatory && mzidCLIParam.hasArgument
                    && (!aLine.hasOption(mzidCLIParam.id) || ((String) aLine.getOptionValue(mzidCLIParam.id)).equals(""))) {
                System.out.println("\n" + mzidCLIParam.description + " not specified.\n");
                return false;
            }
        }
        String fileTxt = aLine.getOptionValue(MzidCLIParams.CPS_FILE.id);
        File testFile = new File(fileTxt.trim());
        if (!testFile.exists()) {
            System.out.println("\n" + MzidCLIParams.CPS_FILE.description + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
            return false;
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
     * Loads the modifications.
     */
    public void loadPtms() {

        // reset ptm factory
        ptmFactory = PTMFactory.getInstance();
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
            MzidCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "========================================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker mzid - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("========================================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(MzidCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                MzidCLIInputBean lCLIBean = new MzidCLIInputBean(line);
                MzidCLI mzidCLI = new MzidCLI(lCLIBean);
                mzidCLI.call();
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
        return "MzidCLI{"
                + ", cliInputBean=" + mzidCLIInputBean
                + '}';
    }
}
