package eu.isas.peptideshaker.cmd;

import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.IonFactory;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.gui.DummyFrame;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.messages.FeedBack;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.peptideshaker.export.ProjectExport;
import eu.isas.peptideshaker.fileimport.FileImporter;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.Properties;
import eu.isas.peptideshaker.utils.Tips;
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
     *
     * @throws Exception thrown if an exception occurs
     */
    public Object call() throws Exception {

        try {
            PathSettingsCLIInputBean pathSettingsCLIInputBean = cliInputBean.getPathSettingsCLIInputBean();
            if (pathSettingsCLIInputBean.hasInput()) {
                PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
                pathSettingsCLI.setPathSettings();
            } else {
                try {
                    setPathConfiguration();
                } catch (Exception e) {
                    System.out.println("An error occurred when setting path configuration. Default will be used.");
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

                ArrayList<String> tips;
                try {
                    tips = Tips.getTips();
                } catch (Exception e) {
                    tips = new ArrayList<String>();
                    // Do something here?
                }

                PeptideShakerGUI peptideShakerGUI = new PeptideShakerGUI(); // dummy object to get at the version and tips
                peptideShakerGUI.setUpLogFile(false); // redirect the error stream to the PeptideShaker log file
                waitingHandler = new WaitingDialog(new DummyFrame("PeptideShaker " + PeptideShaker.getVersion(), "/icons/peptide-shaker.gif"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        false, tips, "Importing Data", "PeptideShaker", PeptideShaker.getVersion(), true);
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
            try {
                createProject();
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while creating the PeptideShaker project.", true, true);
                e.printStackTrace();
            }

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
                waitingHandler.appendReport("Saving results.", true, true);
                saveProject(waitingHandler, true);
                waitingHandler.appendReport("Results saved to " + cpsFile.getAbsolutePath() + ".", true, true);
                waitingHandler.appendReportEndLine();
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
                        CLIMethods.recalibrateSpectra(followUpCLIInputBean, identification, identificationParameters.getAnnotationPreferences(), waitingHandler);
                    } catch (Exception e) {
                        waitingHandler.appendReport("An error occurred while recalibrating the spectra.", true, true);
                        e.printStackTrace();
                    }
                }

                // export spectra
                if (followUpCLIInputBean.spectrumExportNeeded()) {
                    try {
                        CLIMethods.exportSpectra(followUpCLIInputBean, identification, waitingHandler, identificationParameters.getSequenceMatchingPreferences());
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
                        CLIMethods.exportProgenesis(followUpCLIInputBean, identification, waitingHandler, identificationParameters.getSequenceMatchingPreferences());
                        waitingHandler.appendReport("Progenesis export completed.", true, true);
                    } catch (Exception e) {
                        waitingHandler.appendReport("An error occurred while exporting the Progenesis file.", true, true);
                        e.printStackTrace();
                    }
                }

                // de novo training export
                if (followUpCLIInputBean.pepnovoTrainingExportNeeded()) {
                    try {
                        CLIMethods.exportPepnovoTrainingFiles(followUpCLIInputBean, identification, identificationParameters.getAnnotationPreferences(), waitingHandler);
                        waitingHandler.appendReport("PepNovo training export completed.", true, true);
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
                            CLIMethods.exportReport(reportCLIInputBean, reportType, experiment.getReference(), sample.getReference(), replicateNumber, projectDetails, identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, nSurroundingAAs, spectrumCountingPreferences, waitingHandler);
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

            // Export as zip
            File zipFile = cliInputBean.getZipExport();
            if (zipFile != null) {

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Zipping project.", true, true);

                File parent = zipFile.getParentFile();
                try {
                    parent.mkdirs();
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while creating folder " + parent.getAbsolutePath() + ".", true, true);
                }

                File fastaFile = identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase();
                ArrayList<File> spectrumFiles = new ArrayList<File>();
                for (String spectrumFileName : getIdentification().getSpectrumFiles()) {
                    File spectrumFile = getProjectDetails().getSpectrumFile(spectrumFileName);
                    spectrumFiles.add(spectrumFile);
                }

                try {
                    ProjectExport.exportProjectAsZip(zipFile, fastaFile, spectrumFiles, cpsFile, waitingHandler);
                    final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                    double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);
                    waitingHandler.appendReport("Project zipped to \'" + zipFile.getAbsolutePath() + "\' (" + sizeOfZippedFile + " MB)", true, true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    waitingHandler.appendReport("An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ".", true, true);
                } catch (IOException e) {
                    e.printStackTrace();
                    waitingHandler.appendReport("An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ".", true, true);
                }
            }

            waitingHandler.appendReportEndLine();

            try {
                closePeptideShaker(identification);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                e.printStackTrace();
            }

            waitingHandler.appendReport("PeptideShaker process completed.", true, true);
            waitingHandler.setSecondaryProgressText("Processing Completed.");

            saveReport();
        } catch (Exception e) {
            waitingHandler.appendReport("PeptideShaker processing failed. See the PeptideShaker log for details.", true, true);
            saveReport();
            throw e;
        }

        System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
        // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!
        return null;
    }

    /**
     * Save the peptide shaker report next to the cps file.
     */
    private void saveReport() {

        String report;

        if (waitingHandler instanceof WaitingDialog) {
            report = getExtendedProjectReport(((WaitingDialog) waitingHandler).getReport(null));
        } else {
            report = getExtendedProjectReport(null);
        }

        if (report != null) {
            if (waitingHandler instanceof WaitingDialog) {
                report = "<html><br>";
                report += "<b>Report:</b><br>";
                report += "<pre>" + ((WaitingDialog) waitingHandler).getReport(null) + "</pre>";
                report += "</html>";
            }
        }

        if (report != null) {

            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
                File psReportFile;

                if (getCpsFile() != null) {
                    String fileName = "PeptideShaker Report " + getCpsFile().getName() + " " + df.format(new Date()) + ".html";
                    psReportFile = new File(getCpsFile().getParentFile(), fileName);
                } else {
                    String fileName = "PeptideShaker Report " + df.format(new Date()) + ".html";
                    psReportFile = new File(cliInputBean.getOutput().getParentFile(), fileName);
                }

                FileWriter fw = new FileWriter(psReportFile);
                fw.write(report);
                fw.close();
            } catch (IOException ex) {
                waitingHandler.appendReport("An error occurred while saving the PeptideShaker report.", true, true);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Creates the PeptideShaker project based on the identification files
     * provided in the command line input
     *
     * @throws IOException thrown if an exception occurs
     */
    public void createProject() throws IOException {

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

        // Get the input files
        ArrayList<File> identificationFilesInput = cliInputBean.getIdFiles();
        ArrayList<File> dataFolders = new ArrayList<File>();
        ArrayList<File> spectrumFiles = cliInputBean.getSpectrumFiles();

        // export data from zip files, try to find the search parameter and mgf files
        ArrayList<File> identificationFiles = new ArrayList<File>();
        SearchParameters tempSearchParameters = null;
        for (File inputFile : identificationFilesInput) {

            File parentFile = inputFile.getParentFile();
            if (!dataFolders.contains(parentFile)) {
                dataFolders.add(parentFile);
            }
            File dataFolder = new File(parentFile, "mgf");
            if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                dataFolders.add(dataFolder);
            }
            dataFolder = new File(parentFile, "fasta");
            if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                dataFolders.add(dataFolder);
            }
            dataFolder = new File(parentFile, PeptideShaker.DATA_DIRECTORY);
            if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                dataFolders.add(dataFolder);
            }

            String fileName = inputFile.getName();
            if (fileName.toLowerCase().endsWith("zip")) {
                waitingHandler.appendReport("Unzipping " + fileName + ".", true, true);
                String newName = FileImporter.getTempFolderName(fileName);
                File destinationFolder = new File(parentFile, newName);
                destinationFolder.mkdir();
                TempFilesManager.registerTempFolder(destinationFolder);
                ZipUtils.unzip(inputFile, destinationFolder, waitingHandler);

                dataFolder = new File(destinationFolder, PeptideShaker.DATA_DIRECTORY);
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(destinationFolder, "mgf");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(destinationFolder, "fasta");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                for (File zippedFile : destinationFolder.listFiles()) {
                    String nameLowerCase = zippedFile.getName().toLowerCase();
                    if (nameLowerCase.endsWith("dat")
                            || nameLowerCase.endsWith("omx")
                            || nameLowerCase.endsWith("xml")
                            || nameLowerCase.endsWith("mzid")
                            || nameLowerCase.endsWith("csv")
                            || nameLowerCase.endsWith("tags")) {
                        if (!nameLowerCase.endsWith("mods.xml")
                                && !nameLowerCase.endsWith("usermods.xml")
                                && !nameLowerCase.endsWith("settings.xml")) {
                            identificationFiles.add(zippedFile);
                        }
                    } else if (nameLowerCase.endsWith(".parameters")
                            || nameLowerCase.endsWith(".properties")) {
                        try {
                            tempSearchParameters = SearchParameters.getIdentificationParameters(zippedFile);
                        } catch (Exception e) {
                            waitingHandler.appendReport("Error processing search parameters.", true, true);
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                identificationFiles.add(inputFile);
            }
        }

        // List the spectrum files found
        ArrayList<String> names = new ArrayList<String>();
        for (File spectrumFile : spectrumFiles) {
            names.add(spectrumFile.getName());
        }
        for (File dataFolder : dataFolders) {
            for (File file : dataFolder.listFiles()) {
                String name = file.getName();
                if (name.endsWith(".mgf") && !names.contains(name)) {
                    spectrumFiles.add(file);
                    names.add(name);
                }
            }
        }

        // Get the search parameters
        SearchParameters searchParameters = null;
        if (cliInputBean.getSearchParameters() != null) {
            searchParameters = cliInputBean.getSearchParameters();
        } else if (tempSearchParameters != null) {
            searchParameters = tempSearchParameters;
        }

        if (searchParameters == null) {
            waitingHandler.appendReport("Search parameter settings not found!", true, true);
            waitingHandler.setRunCanceled();
        }

        String error = PeptideShaker.loadModifications(searchParameters);
        if (error != null) {
            System.out.println(error);
        }

        // try to locate the fasta file
        File fastaFile = searchParameters.getFastaFile();
        if (!fastaFile.exists()) {
            boolean found = false;
            // look in the database folder {
            try {
                UtilitiesUserPreferences utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
                File dbFolder = utilitiesUserPreferences.getDbFolder();
                File newFile = new File(dbFolder, fastaFile.getName());
                if (newFile.exists()) {
                    fastaFile = newFile;
                    searchParameters.setFastaFile(fastaFile);
                    found = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!found) {
                // look in the data folders
                for (File dataFolder : dataFolders) {
                    File newFile = new File(dataFolder, fastaFile.getName());
                    if (newFile.exists()) {
                        fastaFile = newFile;
                        searchParameters.setFastaFile(fastaFile);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    waitingHandler.appendReport("FASTA file \'" + fastaFile.getName() + "\' not found.", true, true);
                }
            }
        }

        // Set the default identification parameters
        identificationParameters = IdentificationParameters.getDefaultIdentificationParameters(searchParameters);

        // set the filtering import settings
        IdFilter idFilter = new IdFilter();
        idFilter.setMinPepLength(cliInputBean.getMinPepLength());
        idFilter.setMaxPepLength(cliInputBean.getMaxPepLength());
        idFilter.setMaxMzDeviation(cliInputBean.getMaxMzDeviation());
        idFilter.setIsPpm(cliInputBean.isMaxMassDeviationPpm());
        idFilter.setRemoveUnknownPTMs(cliInputBean.excludeUnknownPTMs());
        identificationParameters.setIdFilter(idFilter);

        // set the validation preferences
        IdMatchValidationPreferences idMatchValidationPreferences = new IdMatchValidationPreferences();
        idMatchValidationPreferences.setDefaultPsmFDR(cliInputBean.getPsmFDR());
        idMatchValidationPreferences.setDefaultPeptideFDR(cliInputBean.getPeptideFDR());
        idMatchValidationPreferences.setDefaultProteinFDR(cliInputBean.getProteinFDR());

        // set the processing settings
        processingPreferences = new ProcessingPreferences();
        Integer nThreads = cliInputBean.getnThreads();
        if (nThreads != null) {
            processingPreferences.setnThreads(nThreads);
        }
        processingPreferences.setProteinConfidenceMwPlots(cliInputBean.getProteinConfidenceMwPlots());

        // set up the shotgun protocol
        shotgunProtocol = ShotgunProtocol.inferProtocolFromSearchSettings(searchParameters);

        // set the PTM scoring preferences
        PTMScoringPreferences ptmScoringPreferences = new PTMScoringPreferences();
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
        identificationParameters.setPtmScoringPreferences(ptmScoringPreferences);

        // set the gene preferences
        if (cliInputBean.getSpecies() != null) {
            try {
                GenePreferences genePreferences = new GenePreferences();
                identificationParameters.setGenePreferences(genePreferences);

                genePreferences.loadSpeciesAndGoDomains();
                genePreferences.setCurrentSpecies(cliInputBean.getSpecies());
                genePreferences.setCurrentSpeciesType(cliInputBean.getSpeciesType());

                // try to download gene and go information
                GeneFactory geneFactory = GeneFactory.getInstance();

                String currentEnsemblSpeciesType = cliInputBean.getSpeciesType().toLowerCase();
                if (currentEnsemblSpeciesType.equalsIgnoreCase("Vertebrates")) {
                    currentEnsemblSpeciesType = "ensembl";
                }

                Integer latestEnsemblVersion = geneFactory.getCurrentEnsemblVersion(currentEnsemblSpeciesType);

                String selectedSpecies = cliInputBean.getSpecies();
                String selectedDb = genePreferences.getEnsemblDatabaseName(cliInputBean.getSpeciesType(), selectedSpecies);
                String currentEnsemblVersionAsString = genePreferences.getEnsemblVersion(selectedDb);

                boolean downloadNewMappings;

                if (currentEnsemblVersionAsString == null) {
                    if (cliInputBean.updateSpecies()) {
                        downloadNewMappings = true;
                    } else {
                        waitingHandler.appendReport("Species and GO mappings where not found for " + selectedSpecies + "! Download manually or use the species_update option.", true, true);
                        waitingHandler.setRunCanceled();
                        downloadNewMappings = false;
                    }
                } else {
                    if (cliInputBean.updateSpecies()) {
                        downloadNewMappings = checkForSpeciesUpdate(currentEnsemblVersionAsString, latestEnsemblVersion);
                    } else {
                        downloadNewMappings = false;
                    }
                }

                // download mappings if needed
                if (downloadNewMappings) {
                    genePreferences.clearOldMappings(cliInputBean.getSpeciesType(), selectedSpecies, true);
                    genePreferences.downloadMappings(waitingHandler, cliInputBean.getSpeciesType(), selectedSpecies, true);
                }

            } catch (IOException e) {
                System.out.println("Failed to load the species and GO domains!");
                e.printStackTrace();
            }
        }

        // set the spectrum counting prefrences
        spectrumCountingPreferences = new SpectrumCountingPreferences();

        // set the annotation preferences
        IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.NH3);
        IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.H2O);

        // create a shaker which will perform the analysis
        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

        // import the files
        peptideShaker.importFiles(waitingHandler, identificationFiles, spectrumFiles,
                shotgunProtocol, identificationParameters, projectDetails, processingPreferences,
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
            if (waitingHandler instanceof WaitingDialog) {
                saveReport();
            }
            TempFilesManager.deleteTempFolders();
            waitingHandler.setWaitingText("PeptideShaker Processing Canceled.");
            System.out.println("<CompomicsError>PeptideShaker processing canceled. See the PeptideShaker log for details.</CompomicsError>");
        }
    }

    /**
     * Close the PeptideShaker instance by clearing up factories and cache.
     *
     * @param identification the identification to close
     * @throws IOException thrown of IOException occurs
     * @throws SQLException thrown if SQLException occurs
     */
    public void closePeptideShaker(Identification identification) throws IOException, SQLException {

        SpectrumFactory.getInstance().closeFiles();
        SequenceFactory.getInstance().clearFactory();
        GOFactory.getInstance().closeFiles();

        if (identification != null) {
            identification.close();
        }

        DerbyUtil.closeConnection();

        File matchFolder = PeptideShaker.getSerializationDirectory(PeptideShaker.getJarFilePath());
        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                boolean deleted = Util.deleteDir(currentFile);
                if (!deleted) {
                    System.out.println(currentFile.getAbsolutePath() + " could not be deleted!"); // @TODO: better handling of this error?
                }
            }
        }

        TempFilesManager.deleteTempFolders();
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
            File lEnzymeFile = new File(PeptideShaker.getJarFilePath() + File.separator + PeptideShaker.ENZYME_FILE);
            enzymeFactory.importEnzymes(lEnzymeFile);
        } catch (Exception e) {
            System.err.println("Not able to load the enzyme file.");
            e.printStackTrace();
        }
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

        if (aLine.hasOption(PeptideShakerCLIParams.SPECTRUM_FILES.id)) {
            String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
            ArrayList<File> idFiles = PeptideShakerCLIInputBean.getSpectrumFiles(filesTxt);
            if (idFiles.isEmpty()) {
                System.out.println("\nNo spectrum file found for command line input " + filesTxt + ".\n");
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
            System.out.println("<CompomicsError>PeptideShaker processing failed. See the PeptideShaker log for details.</CompomicsError>");
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

    /**
     * Returns true if a new species mapping is available.
     *
     * @param currentEnsemblVersionAsString the current version
     * @param latestEnsemblVersion the latest version available
     * @return true if a new species mapping is available
     */
    private boolean checkForSpeciesUpdate(String currentEnsemblVersionAsString, Integer latestEnsemblVersion) {

        currentEnsemblVersionAsString = currentEnsemblVersionAsString.substring(currentEnsemblVersionAsString.indexOf(" ") + 1);
        Integer currentEnsemblVersion;

        try {
            currentEnsemblVersion = new Integer(currentEnsemblVersionAsString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            currentEnsemblVersion = latestEnsemblVersion;
        }

        return currentEnsemblVersion < latestEnsemblVersion;
    }
}
