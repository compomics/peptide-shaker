package eu.isas.peptideshaker.cmd;

import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * This class performs the command line export of reports in command line.
 *
 * @author Marc Vaudel
 */
public class ReportCLI extends CpsParent {

    /**
     * The report command line options.
     */
    private ReportCLIInputBean reportCLIInputBean;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
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
     * Construct a new ReportCLI runnable from a ReportCLI Bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired output files.
     *
     * @param reportCLIInputBean the input bean
     */
    public ReportCLI(ReportCLIInputBean reportCLIInputBean) {
        this.reportCLIInputBean = reportCLIInputBean;
        loadEnzymes();
        loadPtms();
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     * 
     * @return returns 1 if the process was canceled
     */
    public Object call() {

        PathSettingsCLIInputBean pathSettingsCLIInputBean = reportCLIInputBean.getPathSettingsCLIInputBean();
        if (pathSettingsCLIInputBean.hasInput()) {
            PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
            pathSettingsCLI.setPathSettings();
        } else {
            try {
                setPathConfiguration();
            } catch (Exception e) {
                System.out.println("An error occured when setting path configuration. Default will be used.");
                e.printStackTrace();
            }
        }

        waitingHandler = new WaitingHandlerCLIImpl();

        cpsFile = reportCLIInputBean.getCpsFile();

        try {
            loadCpsFile(PeptideShaker.getJarFilePath(), waitingHandler);
        } catch (SQLException e) {
            waitingHandler.appendReport("An error occured while reading: " + cpsFile.getAbsolutePath() + ". "
                    + "It looks like another instance of PeptideShaker is still connected to the file. "
                    + "Please close all instances of PeptideShaker and try again.", true, true);
            e.printStackTrace();
            waitingHandler.appendReport(cpsFile.getAbsolutePath() + " successfuly loaded.", true, true);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occured while reading: " + cpsFile.getAbsolutePath() + ".", true, true);
            e.printStackTrace();
            return 1;
        }

        // Load fasta file
        try {
            if (!loadFastaFile(waitingHandler)) {
                waitingHandler.appendReport("The fasta file was not found, please locate it using the GUI.", true, true);
                return 1;
            }
            waitingHandler.appendReport("Protein database " + searchParameters.getFastaFile().getName() + ".", true, true);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while loading the fasta file.", true, true);
            e.printStackTrace();
            return 1;
        }

        // Load the spectrum files
        try {
            if (!loadSpectrumFiles(waitingHandler)) {
                if (identification.getSpectrumFiles().size() > 1) {
                    waitingHandler.appendReport("The spectrum files were not found, please locate them using the GUI.", true, true);
                } else {
                    waitingHandler.appendReport("The spectrum file was not found, please locate it using the GUI.", true, true);
                }
                return 1;
            }
            waitingHandler.appendReport("Spectrum file(s) successfully loaded.", true, true);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while loading the spectrum file(s).", true, true);
            e.printStackTrace();
            return 1;
        }

        loadGeneMappings(PeptideShaker.getJarFilePath(), waitingHandler);

        // Export report(s)
        if (reportCLIInputBean.exportNeeded()) {
            int nSurroundingAAs = 2; //@TODO: this shall not be hard coded
            for (String reportType : reportCLIInputBean.getReportTypes()) {
                try {
                    CLIMethods.exportReport(reportCLIInputBean, reportType, experiment.getReference(), sample.getReference(), replicateNumber, projectDetails, identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, nSurroundingAAs, idFilter, ptmScoringPreferences, spectrumCountingPreferences, waitingHandler);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the " + reportType + ".", true, true);
                    e.printStackTrace();
                }
            }
        }

        // Export documentation(s)
        if (reportCLIInputBean.documentationExportNeeded()) {
            for (String reportType : reportCLIInputBean.getReportTypes()) {
                try {
                    CLIMethods.exportDocumentation(reportCLIInputBean, reportType, waitingHandler);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the documentation for " + reportType + ".", true, true);
                    e.printStackTrace();
                }
            }
        }
        
        waitingHandler.appendReport("Report export completed.", true, true);

        System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
        // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!

        return null;
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), PeptideShakerPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
    }

    /**
     * PeptideShaker report CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker report command line takes a cps file and generates various types of reports." + System.getProperty("line.separator")
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
        if (!aLine.hasOption(ReportCLIParams.CPS_FILE.id) || ((String) aLine.getOptionValue(ReportCLIParams.CPS_FILE.id)).equals("")) {
            System.out.println("\n" + ReportCLIParams.CPS_FILE.description + " not specified.\n");
            return false;
        } else {
            String fileTxt = aLine.getOptionValue(ReportCLIParams.CPS_FILE.id);
            File testFile = new File(fileTxt.trim());
            if (!testFile.exists()) {
                System.out.println("\n" + ReportCLIParams.CPS_FILE.description + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
                return false;
            }
        }
        if (!aLine.hasOption(ReportCLIParams.EXPORT_FOLDER.id) || ((String) aLine.getOptionValue(ReportCLIParams.EXPORT_FOLDER.id)).equals("")) {
            System.out.println("\n" + ReportCLIParams.EXPORT_FOLDER.description + " not specified.\n");
            return false;
        } else {
            String fileTxt = aLine.getOptionValue(ReportCLIParams.EXPORT_FOLDER.id);
            File testFile = new File(fileTxt.trim());
            if (!testFile.exists()) {
                System.out.println("\n" + ReportCLIParams.EXPORT_FOLDER.description + " \'" + testFile.getAbsolutePath() + "\' not found.\n");
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
            Options lOptions = new Options();
            ReportCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "===============================================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker Report Exporter - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("===============================================" + System.getProperty("line.separator"));
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
        return "FollowUpCLI{" + ", cliInputBean=" + reportCLIInputBean + '}';
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
        GOFactory.getInstance().closeFiles();
        identification.close();

        File matchFolder = PeptideShaker.getSerializationDirectory(PeptideShaker.getJarFilePath());

        DerbyUtil.closeConnection();

        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                Util.deleteDir(currentFile);
            }
        }
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
     * Loads the modifications from the modification file.
     */
    public void loadPtms() {

        // reset ptm factory
        ptmFactory = PTMFactory.getInstance();

        try {
            ptmFactory.importModifications(new File(PeptideShaker.getJarFilePath(), PeptideShaker.MODIFICATIONS_FILE), false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ptmFactory.importModifications(new File(PeptideShaker.getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
