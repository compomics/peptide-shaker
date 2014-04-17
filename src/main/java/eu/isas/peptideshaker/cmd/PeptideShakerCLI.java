package eu.isas.peptideshaker.cmd;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.TxtExporter;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.gui.DummyFrame;
import com.compomics.util.messages.FeedBack;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.Properties;
import java.awt.Point;
import java.awt.Toolkit;
import org.apache.commons.cli.*;

import java.io.*;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * A Command line interface to run PeptideShaker
 *
 * @author Kenny Helsens
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerCLI extends CpsParent implements Callable {

    /**
     * The Progress messaging handler reports the status throughout all
     * PeptideShaker processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The CLI input parameters to start PeptideShaker from command line.
     */
    private PeptideShakerCLIInputBean cliInputBean = null;
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();

    /**
     * Construct a new PeptideShakerCLI runnable from a PeptideShakerCLI Bean.
     * When initialization is successful, calling "run" will start PeptideShaker
     * and write the output files when finished.
     *
     * @param cliInputBean the PeptideShakerCLIInputBean
     */
    public PeptideShakerCLI(PeptideShakerCLIInputBean cliInputBean) {
        this.cliInputBean = cliInputBean;
        loadEnzymes();
    }

    /**
     * Calling this method will run the configured PeptideShaker process.
     */
    public Object call() {

        PathSettingsCLIInputBean pathSettingsCLIInputBean = cliInputBean.getPathSettingsCLIInputBean();
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

        // Set up the waiting handler
        if (cliInputBean.isGUI()) {

            // set the look and feel
            try {
                UtilitiesGUIDefaults.setLookAndFeel();
            } catch (Exception e) {
                // ignore, use default look and feel
            }

            PeptideShakerGUI peptideShakerGUI = new PeptideShakerGUI(); // dummy object to get at the version and tips
            peptideShakerGUI.setUpLogFile(false); // redirect the error stream to the PeptideShaker log file
            waitingHandler = new WaitingDialog(new DummyFrame("PeptideShaker " + peptideShakerGUI.getVersion(), "/icons/peptide-shaker.gif"),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    false, peptideShakerGUI.getTips(), "Importing Data", "PeptideShaker", peptideShakerGUI.getVersion(), true);
            ((WaitingDialog) waitingHandler).setCloseDialogWhenImportCompletes(false, false);
            ((WaitingDialog) waitingHandler).setLocationRelativeTo(null);
            Point tempLocation = ((WaitingDialog) waitingHandler).getLocation();
            ((WaitingDialog) waitingHandler).setLocation((int) tempLocation.getX() + 30, (int) tempLocation.getY() + 30);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        ((WaitingDialog) waitingHandler).setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();
        } else {
            waitingHandler = new WaitingHandlerCLIImpl();
        }

        // create project
        createProject();

        // see if the project was created or canceled
        if (waitingHandler.isRunCanceled()) {
            try {
                closePeptideShaker(identification);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                e.printStackTrace();
            }
            System.exit(0);
            return null;
        } else {
            waitingHandler.appendReport("Project successfully created.", true, true);
        }

        // save project
        try {
            cpsFile = cliInputBean.getOutput();
            waitingHandler.appendReport("Saving Results. Please Wait...", true, true);
            saveProject(waitingHandler, true);
            waitingHandler.appendReport("Results saved to " + cpsFile.getAbsolutePath() + ".", true, true);
            waitingHandler.appendReportEndLine();

            // save the peptide shaker report next to the cps file
            String report = getExtendedProjectReport();

            if (report != null) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
                String fileName = "PeptideShaker Report " + getCpsFile().getName() + " " + df.format(new Date()) + ".html";
                File psReportFile = new File(getCpsFile().getParentFile(), fileName);
                FileWriter fw = new FileWriter(psReportFile);
                fw.write(report);
                fw.close();
            }
        } catch (Exception e) {
            waitingHandler.appendReport("An exception occurred while saving the project.", true, true);
            e.printStackTrace();
        }

        // Finished
        waitingHandler.setPrimaryProgressCounterIndeterminate(false);
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        // Follow up tasks if needed
        FollowUpCLIInputBean followUpCLIInputBean = cliInputBean.getFollowUpCLIInputBean();
        if (followUpCLIInputBean.followUpNeeded()) {
            waitingHandler.appendReport("Starting follow up tasks.", true, true);

            // recalibrate spectra
            if (followUpCLIInputBean.recalibrationNeeded()) {
                try {
                    CLIMethods.recalibrateSpectra(followUpCLIInputBean, identification, annotationPreferences, waitingHandler);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while recalibrating the spectra.", true, true);
                    e.printStackTrace();
                }
            }

            // export spectra
            if (followUpCLIInputBean.spectrumExportNeeded()) {
                try {
                    CLIMethods.exportSpectra(followUpCLIInputBean, identification, waitingHandler, searchParameters);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the spectra.", true, true);
                    e.printStackTrace();
                }
            }

            // export protein accessions
            if (followUpCLIInputBean.accessionExportNeeded()) {
                try {
                    CLIMethods.exportAccessions(followUpCLIInputBean, identification, identificationFeaturesGenerator, waitingHandler, filterPreferences);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the protein accessions.", true, true);
                    e.printStackTrace();
                }
            }

            // export protein details
            if (followUpCLIInputBean.accessionExportNeeded()) {
                try {
                    CLIMethods.exportFasta(followUpCLIInputBean, identification, identificationFeaturesGenerator, waitingHandler, filterPreferences);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the protein details.", true, true);
                    e.printStackTrace();
                }
            }

            // progenesis export
            if (followUpCLIInputBean.progenesisExportNeeded()) {
                try {
                    CLIMethods.exportProgenesis(followUpCLIInputBean, identification, waitingHandler, searchParameters);
                    waitingHandler.appendReport("Progenesis export completed.", true, true);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the Progenesis file.", true, true);
                    e.printStackTrace();
                }
            }

            // Pepnovo training export
            if (followUpCLIInputBean.pepnovoTrainingExportNeeded()) {
                try {
                    CLIMethods.exportPepnovoTrainingFiles(followUpCLIInputBean, identification, annotationPreferences, waitingHandler);
                    waitingHandler.appendReport("Pepnovo training export completed.", true, true);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while exporting the Pepnovo training file.", true, true);
                    e.printStackTrace();
                }
            }

        }

        // Report export if needed
        ReportCLIInputBean reportCLIInputBean = cliInputBean.getReportCLIInputBean();

        // see if output folder is set, and if not set to the same folder as the cps file
        if (reportCLIInputBean.getReportOutputFolder() == null) {
            reportCLIInputBean.setReportOutputFolder(cliInputBean.getOutput().getParentFile());
        }

        if (reportCLIInputBean.exportNeeded()) {
            waitingHandler.appendReport("Starting report export.", true, true);

            // Export report(s)
            if (reportCLIInputBean.exportNeeded()) {
                int nSurroundingAAs = 2; //@TODO: this shall not be hard coded //peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides()
                for (String reportType : reportCLIInputBean.getReportTypes()) {
                    try {
                        CLIMethods.exportReport(reportCLIInputBean, reportType, experiment.getReference(), sample.getReference(), replicateNumber, projectDetails, identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, nSurroundingAAs, idFilter, ptmScoringPreferences, spectrumCountingPreferences, waitingHandler);
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
        }

        //@TODO: move that to the report cli as soon as tested
        // text summary output - format 1
        if (cliInputBean.getTextFormat1Directory() != null) {
            waitingHandler.appendReport("Exporting results as text files. Please wait...", true, true);
            TxtExporter exporter = new TxtExporter(experiment, sample, replicateNumber, identificationFeaturesGenerator, searchParameters);
            exporter.exportResults(waitingHandler, cliInputBean.getTextFormat1Directory());
            waitingHandler.appendReport("Results saved as text in " + cliInputBean.getTextFormat1Directory().getAbsolutePath() + ".", true, true);
            waitingHandler.appendReportEndLine();
        }

        waitingHandler.setWaitingText("PeptideShaker Import Completed.");
        waitingHandler.appendReportEndLine();

        try {
            closePeptideShaker(identification);
        } catch (Exception e) {
            waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
            e.printStackTrace();
        }

        waitingHandler.appendReport("End of PeptideShaker processing.", true, true);
        waitingHandler.setSecondaryProgressText("Processing Completed!");

        System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
        // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!

        return null;
    }

    /**
     * Creates the PeptideShaker project based on the identification files
     * provided in the command line input
     */
    public void createProject() {

        // Define new project references.
        experiment = new MsExperiment(cliInputBean.getiExperimentID());
        sample = new Sample(cliInputBean.getiSampleID());
        replicateNumber = cliInputBean.getReplicate();

        // Create the analysis set of this PeptideShaker process
        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber));
        experiment.addAnalysisSet(sample, analysisSet);

        // Set the project details
        projectDetails = new ProjectDetails();
        projectDetails.setCreationDate(new Date());
        projectDetails.setPeptideShakerVersion(new Properties().getVersion());

        // Get the search parameters
        searchParameters = cliInputBean.getIdentificationParameters();
        String error = PeptideShaker.loadModifications(searchParameters);
        if (error != null) {
            System.out.println(error);
        }

        // Get the input files
        ArrayList<File> spectrumFiles = cliInputBean.getSpectrumFiles();
        ArrayList<File> identificationFiles = cliInputBean.getIdFiles();

        // set the filtering import settings
        idFilter = new IdFilter();
        idFilter.setOmssaMaxEvalue(cliInputBean.getOmssaMaxEvalue());
        idFilter.setXtandemMaxEvalue(cliInputBean.getXtandemMaxEvalue());
        idFilter.setMascotMaxEvalue(cliInputBean.getMascotMaxEvalue());
        idFilter.setMinPepLength(cliInputBean.getMinPepLength());
        idFilter.setMaxPepLength(cliInputBean.getMaxPepLength());
        idFilter.setMaxMzDeviation(cliInputBean.getMaxMzDeviation());
        idFilter.setIsPpm(cliInputBean.isMaxMassDeviationPpm());
        idFilter.setRemoveUnknownPTMs(cliInputBean.excludeUnknownPTMs());

        // set the processing settings
        processingPreferences = new ProcessingPreferences();
        processingPreferences.setPsmFDR(cliInputBean.getPsmFDR());
        processingPreferences.setPeptideFDR(cliInputBean.getPeptideFDR());
        processingPreferences.setProteinFDR(cliInputBean.getProteinFDR());
        processingPreferences.setProteinConfidenceMwPlots(cliInputBean.getProteinConfidenceMwPlots());

        // set the PTM scoring preferences
        ptmScoringPreferences = new PTMScoringPreferences();
        if (cliInputBean.getPtmScore() != null) {
            ptmScoringPreferences.setProbabilitsticScoreCalculation(true);
            ptmScoringPreferences.setSelectedProbabilisticScore(cliInputBean.getPtmScore());
            ptmScoringPreferences.setProbabilisticScoreNeutralLosses(cliInputBean.isaScoreNeutralLosses());
            if (cliInputBean.getPtmScoreThreshold() != null) {
                ptmScoringPreferences.setEstimateFlr(false);
                ptmScoringPreferences.setProbabilisticScoreThreshold(cliInputBean.getPtmScoreThreshold());
            } else {
                ptmScoringPreferences.setEstimateFlr(true);
            }
        } else {
            ptmScoringPreferences.setProbabilitsticScoreCalculation(false);
        }

        // set the gene preferences
        genePreferences = new GenePreferences();
        genePreferences.setCurrentSpecies(cliInputBean.getSpecies());
        genePreferences.setCurrentSpeciesType(cliInputBean.getSpeciesType());

        // set the spectrum counting prefrences
        spectrumCountingPreferences = new SpectrumCountingPreferences();

        // set the annotation preferences
        annotationPreferences = new AnnotationPreferences();
        annotationPreferences.setPreferencesFromSearchParamaers(searchParameters);

        // create a shaker which will perform the analysis
        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

        // import the files
        peptideShaker.importFiles(waitingHandler, idFilter, identificationFiles, spectrumFiles, searchParameters,
                annotationPreferences, projectDetails, processingPreferences, ptmScoringPreferences,
                spectrumCountingPreferences, false);

        // show the warnings
        Iterator<String> iterator = peptideShaker.getWarnings().keySet().iterator();
        while (iterator.hasNext()) {
            FeedBack warning = peptideShaker.getWarnings().get(iterator.next());
            if (warning.getType() == FeedBack.FeedBackType.WARNING) {
                System.out.println(warning.getMessage()); // @TODO: better interaction between notes and feedback objetcs...
            }
        }

        if (!waitingHandler.isRunCanceled()) {

            // identification as created by PeptideShaker
            ProteomicAnalysis tempProteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
            identification = tempProteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

            // metrics saved while processing the data
            metrics = peptideShaker.getMetrics();

            // the identification feature generator
            identificationFeaturesGenerator = peptideShaker.getIdentificationFeaturesGenerator();

            // the cache used for identification
            objectsCache = peptideShaker.getCache();

            if (waitingHandler instanceof WaitingDialog) {
                projectDetails.setReport(((WaitingDialog) waitingHandler).getReport(null));
                ((WaitingDialog) waitingHandler).setRunNotFinished();
                ((WaitingDialog) waitingHandler).setCloseDialogWhenImportCompletes(true, false);
            }

        } else {
            waitingHandler.setWaitingText("PeptideShaker Processing Canceled.");
            System.out.println("<CompomicsError>PeptideShaker processing canceled.</CompomicsError>");
        }
    }

    /**
     * Close the PeptideShaker instance by clearing up factories and cache.
     *
     * @param identification the identification to close
     * @throws IOException
     * @throws SQLException
     */
    public void closePeptideShaker(Identification identification) throws IOException, SQLException {

        SpectrumFactory.getInstance().closeFiles();
        SequenceFactory.getInstance().clearFactory();
        GOFactory.getInstance().closeFiles();

        if (identification != null) {
            identification.close();
        }

        File matchFolder = PeptideShaker.getSerializationDirectory(getJarFilePath());
        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                boolean deleted = Util.deleteDir(currentFile);
                if (!deleted) {
                    System.out.println(currentFile.getAbsolutePath() + " could not be deleted!"); // @TODO: better handling of this error?
                }
            }
        }
    }

    /**
     * PeptideShaker CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker command line takes identification files from X!Tandem, OMSSA and Mascot and generates various types of output files." + System.getProperty("line.separator")
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
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            File lEnzymeFile = new File(getJarFilePath() + File.separator + PeptideShaker.ENZYME_FILE);
            enzymeFactory.importEnzymes(lEnzymeFile);
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
    }

    @Override
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("PeptideShakerCLI.class").getPath(), "PeptideShaker");
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(getJarFilePath(), PeptideShakerPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
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

        if (!aLine.hasOption(PeptideShakerCLIParams.EXPERIMENT.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.EXPERIMENT.id)).equals("")) {
            System.out.println("\nExperiment name not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.SAMPLE.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.SAMPLE.id)).equals("")) {
            System.out.println("\nSample name not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.REPLICATE.id) || aLine.getOptionValue(PeptideShakerCLIParams.REPLICATE.id) == null) {
            System.out.println("\nReplicate number not specified.\n");
            return false;
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id)).equals("")) {
            System.out.println("\nSpectrum files not specified.\n");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            ArrayList<File> idFiles = PeptideShakerCLIInputBean.getSpectrumFiles(filesTxt);
            if (idFiles.isEmpty()) {
                System.out.println("\nNo spectrum file found.\n");
                return false;
            }
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.IDENTIFICATION_FILES.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id)).equals("")) {
            System.out.println("\nIdentification files not specified.\n");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
            ArrayList<File> idFiles = PeptideShakerCLIInputBean.getIdentificationFiles(filesTxt);
            if (idFiles.isEmpty()) {
                System.out.println("\nNo identification file found.\n");
                return false;
            }
        }

        if (!aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id) || ((String) aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id)).equals("")) {
            System.out.println("\nOutput file not specified.\n");
            return false;
        } else {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id);
            File testFile = new File(filesTxt.trim());
            File parentFolder = testFile.getParentFile();
            if (!parentFolder.exists() && !parentFolder.mkdirs()) {
                System.out.println("\nDestination folder \'" + parentFolder.getPath() + "\' not found and cannot be created. Make sure that PeptideShaker has the right to write in the destination folder.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_TXT_1.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_TXT_1.id).trim();
            File testFile = new File(filesTxt);
            if (!testFile.exists()) {
                System.out.println("\nDestination folder for text summary \'" + filesTxt + "\' not found.\n");
                return false;
            }
        }

//        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_TXT_2.id)) {
//            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_TXT_2.id).trim();
//            File testFile = new File(filesTxt);
//            if (!testFile.exists()) {
//                System.out.println("\nDestination folder for text summary \'" + filesTxt + "\' not found.\n");
//                return false;
//            }
//        }
//
//        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id)) {
//            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id);
//            File testFile = new File(filesTxt.trim());
//            File parentFolder = testFile.getParentFile();
//            if (!parentFolder.exists()) {
//                System.out.println("\nDestination folder for PRIDE file \'" + parentFolder.getPath() + "\' not found.\n");
//                return false;
//            }
//        }
        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PSM_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse \'" + input + "\' as PSM FDR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PTM_SCORE.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PTM_SCORE.id).trim();
            try {
                Integer.parseInt(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse \'" + input + "\' as integer.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.SCORE_NEUTRAL_LOSSES.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.SCORE_NEUTRAL_LOSSES.id).trim();
            try {
                Integer.parseInt(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse \'" + input + "\' as integer.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PTM_THRESHOLD.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PTM_THRESHOLD.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse \'" + input + "\' as peptide PTM score threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDE_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDE_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse \'" + input + "\' as peptide FDR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FDR.id)) {
            String input = aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FDR.id).trim();
            try {
                Double.parseDouble(input);
            } catch (Exception e) {
                System.out.println("\nCould not parse \'" + input + "\' as protein FDR threshold.\n");
                return false;
            }
        }

        if (aLine.hasOption(PeptideShakerCLIParams.IDENTIFICATION_PARAMETERS.id)) {

            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_PARAMETERS.id).trim();
            File testFile = new File(filesTxt);
            if (testFile.exists()) {
                try {
                    SearchParameters.getIdentificationParameters(testFile);
                } catch (Exception e) {
                    System.out.println("\nAn error occurred while parsing \'" + filesTxt + "\'.\n");
                    e.printStackTrace();
                }
            } else {
                System.out.println("\nSearch parameters file \'" + filesTxt + "\' not found.\n");
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
            PeptideShakerCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!isValidStartup(line)) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "==============================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("==============================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(PeptideShakerCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                PeptideShakerCLIInputBean lCLIBean = new PeptideShakerCLIInputBean(line);
                PeptideShakerCLI lPeptideShakerCLI = new PeptideShakerCLI(lCLIBean);
                lPeptideShakerCLI.call();
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
        return "PeptideShakerCLI{"
                + ", waitingHandler=" + waitingHandler
                + ", cliInputBean=" + cliInputBean
                + ", ptmFactory=" + ptmFactory
                + ", enzymeFactory=" + enzymeFactory
                + '}';
    }
}
