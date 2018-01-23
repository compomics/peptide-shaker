package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathParameters;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.genes.ProteinGeneDetailsProvider;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.cli.identification_parameters.IdentificationParametersInputBean;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.gui.DummyFrame;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.tools.UtilitiesUserParameters;
import com.compomics.util.parameters.identification.advanced.ValidationQcParameters;
import eu.isas.peptideshaker.export.ProjectExport;
import eu.isas.peptideshaker.preferences.PeptideShakerPathParameters;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingParameters;
import eu.isas.peptideshaker.utils.Properties;
import eu.isas.peptideshaker.utils.PsZipUtils;
import eu.isas.peptideshaker.utils.Tips;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.awt.Point;
import java.awt.Toolkit;
import org.apache.commons.cli.*;
import java.io.*;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * A command line interface to run PeptideShaker.
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
     * The exception handler.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The CLI input parameters to start PeptideShaker from command line.
     */
    private PeptideShakerCLIInputBean cliInputBean = null;
    /**
     * The modification factory.
     */
    private ModificationFactory modificationFactory;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserParameters utilitiesUserParameters;

    /**
     * Construct a new PeptideShakerCLI runnable. When initialization is
     * successful and the PeptideShakerCLIInputBean is set, calling "run" will
     * start PeptideShaker and write the output files when finished.
     */
    public PeptideShakerCLI() {
    }

    /**
     * Set the PeptideShakerCLIInputBean.
     *
     * @param cliInputBean the PeptideShakerCLIInputBean
     */
    public void setPeptideShakerCLIInputBean(PeptideShakerCLIInputBean cliInputBean) {
        this.cliInputBean = cliInputBean;
    }

    @Override
    public Object call() {

        try {
            // get the path settings input
            PathSettingsCLIInputBean pathSettingsCLIInputBean = cliInputBean.getPathSettingsCLIInputBean();

            // redirect the error stream
            if (pathSettingsCLIInputBean.getLogFolder() != null) {
                redirectErrorStream(pathSettingsCLIInputBean.getLogFolder());
            }

            // set up the waiting handler
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
                    tips = new ArrayList<>();
                    // do something here?
                }

                PeptideShakerGUI peptideShakerGUI = new PeptideShakerGUI(); // dummy object to get the version and tips
                if (pathSettingsCLIInputBean.getLogFolder() == null) {
                    peptideShakerGUI.setUpLogFile(false); // redirect the error stream to the PeptideShaker log file
                }
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

            // Set up exception handler
            exceptionHandler = new CommandLineExceptionHandler();

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

            setDbFolder(PeptideShaker.getMatchesFolder());

            try {
                ArrayList<PathKey> errorKeys = PeptideShakerPathParameters.getErrorKeys();
                if (!errorKeys.isEmpty()) {
                    System.out.println("Failed to write in the following configuration folders. Please use a temporary folder, "
                            + "the path configuration command line, or edit the configuration paths from the graphical interface.");
                    for (PathKey pathKey : errorKeys) {
                        System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                    }
                }
            } catch (Exception e) {
                System.out.println("Unable to load the path configurations. Default paths will be used.");
                e.printStackTrace();
            }

            // Load user parameters
            utilitiesUserParameters = UtilitiesUserParameters.loadUserParameters();

            // Instantiate factories
            PeptideShaker.instantiateFacories(utilitiesUserParameters);
            modificationFactory = ModificationFactory.getInstance();
            enzymeFactory = EnzymeFactory.getInstance();

            // Load resources files
            loadSpecies();

            // Set the gene mappings
            ProteinGeneDetailsProvider geneFactory = new ProteinGeneDetailsProvider();
            geneFactory.initialize(PeptideShaker.getJarFilePath());

            // Load the species mapping
            try {
                SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
                speciesFactory.initiate(PeptideShaker.getJarFilePath());
            } catch (Exception e) {
                System.out.println("An error occurred while loading the species mapping. Gene annotation might be impaired.");
                e.printStackTrace();
            }

            // create project
            try {
                createProject();
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while creating the PeptideShaker project.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }

            // see if the project was created or canceled
            if (waitingHandler.isRunCanceled()) {
                try {
                    closePeptideShaker(identification);
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                    e.printStackTrace();
                }
                System.exit(1);
                return 1;
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
                waitingHandler.setRunCanceled();
            }

            // finished
            waitingHandler.setPrimaryProgressCounterIndeterminate(false);
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);

            // follow up tasks if needed
            FollowUpCLIInputBean followUpCLIInputBean = cliInputBean.getFollowUpCLIInputBean();
            if (followUpCLIInputBean.followUpNeeded()) {
                waitingHandler.appendReport("Starting follow up tasks.", true, true);

                // recalibrate spectra
                if (followUpCLIInputBean.recalibrationNeeded()) {
                    try {
                        CLIExportMethods.recalibrateSpectra(followUpCLIInputBean, identification, identificationParameters, waitingHandler);
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
                    } catch (Exception e) {
                        waitingHandler.appendReport("An error occurred while exporting the protein accessions.", true, true);
                        e.printStackTrace();
                        waitingHandler.setRunCanceled();
                    }
                }

                // export protein details
                if (followUpCLIInputBean.accessionExportNeeded()) {
                    try {
                        CLIExportMethods.exportFasta(followUpCLIInputBean, identification, sequenceProvider, waitingHandler, filterParameters);
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
            }

            // report export if needed
            ReportCLIInputBean reportCLIInputBean = cliInputBean.getReportCLIInputBean();

            // see if output folder is set, and if not set to the same folder as the cps file
            if (reportCLIInputBean.getReportOutputFolder() == null) {
                reportCLIInputBean.setReportOutputFolder(cliInputBean.getOutput().getParentFile());
            }

            // array to be filled with all exported reports
            ArrayList<File> reportFiles = new ArrayList<File>();

            if (reportCLIInputBean.exportNeeded()) {
                waitingHandler.appendReport("Starting report export.", true, true);

                // Export report(s)
                if (reportCLIInputBean.exportNeeded()) {
                    int nSurroundingAAs = 2; //@TODO: this shall not be hard coded //peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides()
                    for (String reportType : reportCLIInputBean.getReportTypes()) {
                        try {
                            CLIExportMethods.exportReport(reportCLIInputBean, reportType, projectParameters.getProjectUniqueName(), projectDetails, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, sequenceProvider, proteinDetailsProvider, nSurroundingAAs, spectrumCountingParameters, waitingHandler);
                        } catch (Exception e) {
                            waitingHandler.appendReport("An error occurred while exporting the " + reportType + ".", true, true);
                            e.printStackTrace();
                            waitingHandler.setRunCanceled();
                        }
                    }
                }

                // export documentation
                if (reportCLIInputBean.documentationExportNeeded()) {
                    for (String reportType : reportCLIInputBean.getReportTypes()) {
                        try {
                            CLIExportMethods.exportDocumentation(reportCLIInputBean, reportType, waitingHandler);
                        } catch (Exception e) {
                            waitingHandler.appendReport("An error occurred while exporting the documentation for " + reportType + ".", true, true);
                            e.printStackTrace();
                            waitingHandler.setRunCanceled();
                        }
                    }
                }
            }

            // export as zip
            File zipFile = cliInputBean.getZipExport();
            if (zipFile != null) {

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Zipping project.", true, true);

                File parent = zipFile.getParentFile();
                try {
                    parent.mkdirs();
                } catch (Exception e) {
                    waitingHandler.appendReport("An error occurred while creating folder " + parent.getAbsolutePath() + ".", true, true);
                    waitingHandler.setRunCanceled();
                }

                File fastaFile = identificationParameters.getProteinInferenceParameters().getProteinSequenceDatabase();
                ArrayList<File> spectrumFiles = new ArrayList<>();
                for (String spectrumFileName : getIdentification().getSpectrumFiles()) {
                    File spectrumFile = getProjectDetails().getSpectrumFile(spectrumFileName);
                    spectrumFiles.add(spectrumFile);
                }

                try {
                    ProjectExport.exportProjectAsZip(zipFile, fastaFile, spectrumFiles, reportFiles, cpsFile, waitingHandler);
                    final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                    double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);
                    waitingHandler.appendReport("Project zipped to \'" + zipFile.getAbsolutePath() + "\' (" + sizeOfZippedFile + " MB)", true, true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    waitingHandler.appendReport("An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ".", true, true);
                    waitingHandler.setRunCanceled();
                } catch (IOException e) {
                    e.printStackTrace();
                    waitingHandler.appendReport("An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ".", true, true);
                    waitingHandler.setRunCanceled();
                }
            }

            waitingHandler.appendReportEndLine();

            try {
                closePeptideShaker(identification);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while closing PeptideShaker.", true, true);
                e.printStackTrace();
            }

            saveReport();
        } catch (Exception e) {
            e.printStackTrace();
            if (waitingHandler != null) {
                waitingHandler.appendReport("PeptideShaker processing failed. See the PeptideShaker log for details.", true, true);
                saveReport();
                waitingHandler.setRunCanceled();
            }
        }

        if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
            waitingHandler.appendReport("PeptideShaker process completed.", true, true);
            waitingHandler.setSecondaryProgressText("Processing Completed.");
            System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
            // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!
            return 0;
        } else {
            System.out.println("PeptideShaker process failed!");
            System.exit(1); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
            // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!
            return 1;
        }
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
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
                File psReportFile;
                File logReportFile = null;
                PathSettingsCLIInputBean pathSettingsCLIInputBean = cliInputBean.getPathSettingsCLIInputBean();

                if (getCpsFile() != null) {
                    String fileName = "PeptideShaker Report " + getCpsFile().getName() + " " + df.format(new Date()) + ".html";
                    psReportFile = new File(getCpsFile().getParentFile(), fileName);
                    if (pathSettingsCLIInputBean.getLogFolder() != null) {
                        logReportFile = new File(pathSettingsCLIInputBean.getLogFolder(), fileName);
                    }
                } else {
                    String fileName = "PeptideShaker Report " + df.format(new Date()) + ".html";
                    psReportFile = new File(cliInputBean.getOutput().getParentFile(), fileName);
                    if (pathSettingsCLIInputBean.getLogFolder() != null) {
                        logReportFile = new File(pathSettingsCLIInputBean.getLogFolder(), fileName);
                    }
                }

                FileWriter fw = new FileWriter(psReportFile);
                try {
                    fw.write(report);
                } finally {
                    fw.close();
                }

                if (logReportFile != null) {
                    fw = new FileWriter(logReportFile);
                    try {
                        fw.write(report);
                    } finally {
                        fw.close();
                    }
                }

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
     * @throws java.io.IOException exception thrown if an error occurs while reading or writing a file
     * @throws java.lang.InterruptedException exception thrown if a thread is interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if a process times out
     */
    public void createProject() throws IOException, InterruptedException, TimeoutException {

        // define new project references
        projectParameters = new ProjectParameters(cliInputBean.getiExperimentID());

        // set the project details
        projectDetails = new ProjectDetails();
        projectDetails.setCreationDate(new Date());
        projectDetails.setPeptideShakerVersion(new Properties().getVersion());

        // get the input files
        ArrayList<File> identificationFilesInput = cliInputBean.getIdFiles();
        ArrayList<File> dataFolders = new ArrayList<>();
        ArrayList<File> spectrumFiles = cliInputBean.getSpectrumFiles();

        // export data from zip files, try to find the search parameter and mgf files
        ArrayList<File> identificationFiles = new ArrayList<>();
        IdentificationParameters tempIdentificationParameters = null;
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
            if (fileName.toLowerCase().endsWith(".zip")) {
                waitingHandler.appendReport("Unzipping " + fileName + ".", true, true);
                String newName = PsZipUtils.getTempFolderName(fileName);
                String parentFolder = PsZipUtils.getUnzipParentFolder();
                if (parentFolder == null) {
                    parentFolder = parentFile.getAbsolutePath();
                }
                File parentFolderFile = new File(parentFolder, PsZipUtils.getUnzipSubFolder());
                File destinationFolder = new File(parentFolderFile, newName);
                destinationFolder.mkdir();
                TempFilesManager.registerTempFolder(parentFolderFile);
                ZipUtils.unzip(inputFile, destinationFolder, waitingHandler);
                if (waitingHandler instanceof WaitingHandlerCLIImpl) {
                    waitingHandler.appendReportEndLine();
                }

                dataFolder = new File(destinationFolder, PeptideShaker.DATA_DIRECTORY);
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(destinationFolder, ".mgf");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(destinationFolder, ".fasta");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                for (File unzippedFile : destinationFolder.listFiles()) {
                    String nameLowerCase = unzippedFile.getName().toLowerCase();
                    if (nameLowerCase.endsWith(".dat")
                            || nameLowerCase.endsWith(".omx")
                            || nameLowerCase.endsWith(".xml")
                            || nameLowerCase.endsWith(".mzid")
                            || nameLowerCase.endsWith(".csv")
                            || nameLowerCase.endsWith(".tags")
                            || nameLowerCase.endsWith(".tide-search.target.txt")
                            || nameLowerCase.endsWith(".res")) {
                        if (!nameLowerCase.endsWith("mods.xml")
                                && !nameLowerCase.endsWith("usermods.xml")
                                && !nameLowerCase.endsWith("settings.xml")) {
                            identificationFiles.add(unzippedFile);
                        }
                    } else if (nameLowerCase.endsWith(".par")) {
                        try {
                            tempIdentificationParameters = IdentificationParameters.getIdentificationParameters(unzippedFile);
                            ValidationQcParameters validationQCParameters = tempIdentificationParameters.getIdValidationParameters().getValidationQCParameters();
                            if (validationQCParameters == null
                                    || validationQCParameters.getPsmFilters() == null
                                    || validationQCParameters.getPeptideFilters() == null
                                    || validationQCParameters.getProteinFilters() == null
                                    || validationQCParameters.getPsmFilters().isEmpty()
                                    && validationQCParameters.getPeptideFilters().isEmpty()
                                    && validationQCParameters.getProteinFilters().isEmpty()) {
                                MatchesValidator.setDefaultMatchesQCFilters(validationQCParameters);
                            }
                        } catch (Exception e) {
                            waitingHandler.appendReport("An error occurred while parsing the parameters file " + unzippedFile.getName() + ".", true, true);
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                identificationFiles.add(inputFile);
            }
        }

        // list the spectrum files found
        ArrayList<String> names = new ArrayList<>();
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

        // get the identification parameters
        IdentificationParametersInputBean identificationParametersInputBean = cliInputBean.getIdentificationParametersInputBean();
        if (tempIdentificationParameters != null && identificationParametersInputBean.getInputFile() == null) {
            identificationParametersInputBean.setIdentificationParameters(tempIdentificationParameters);
            identificationParametersInputBean.updateIdentificationParameters();
        }
        identificationParameters = identificationParametersInputBean.getIdentificationParameters();
        ValidationQcParameters validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();
        if (validationQCParameters == null
                || validationQCParameters.getPsmFilters() == null
                || validationQCParameters.getPeptideFilters() == null
                || validationQCParameters.getProteinFilters() == null
                || validationQCParameters.getPsmFilters().isEmpty()
                && validationQCParameters.getPeptideFilters().isEmpty()
                && validationQCParameters.getProteinFilters().isEmpty()) {
            MatchesValidator.setDefaultMatchesQCFilters(validationQCParameters);
        }
        if (identificationParameters == null) {
            waitingHandler.appendReport("Identification parameters not found!", true, true);
            waitingHandler.setRunCanceled();
        }
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        String error = PeptideShaker.loadModifications(searchParameters);
        if (error != null) {
            System.out.println(error);
        }

        // try to locate the fasta file
        File fastaFile = searchParameters.getFastaFile();
        if (!fastaFile.exists()) {
            boolean found = false;
            // look in the database folder
            try {
                File tempDbFolder = utilitiesUserParameters.getDbFolder();
                File newFile = new File(tempDbFolder, fastaFile.getName());
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

            if (found) {
                // see if the protein inference fasta file is also missing
                File proteinInferenceSequenceDatabase = identificationParameters.getProteinInferenceParameters().getProteinSequenceDatabase();
                if (!proteinInferenceSequenceDatabase.exists() && proteinInferenceSequenceDatabase.getName().equalsIgnoreCase(fastaFile.getName())) {
                    identificationParameters.getProteinInferenceParameters().setProteinSequenceDatabase(fastaFile);
                } else {
                    waitingHandler.appendReport("FASTA file \'" + proteinInferenceSequenceDatabase.getName() + "\' not found.", true, true);
                }
            }
        }

        // set the processing settings
        ProcessingParameters processingParameters = new ProcessingParameters();
        Integer nThreads = cliInputBean.getnThreads();
        if (nThreads != null) {
            processingParameters.setnThreads(nThreads);
        }

        // set the spectrum counting prefrences
        spectrumCountingParameters = new SpectrumCountingParameters();

        // incrementing the counter for a new PeptideShaker start run via GUI
        if (utilitiesUserParameters.isAutoUpdate()) {
            Util.sendGAUpdate("UA-36198780-1", "startrun-cl", "peptide-shaker-" + PeptideShaker.getVersion());
        }

        // create a shaker which will perform the analysis
        PeptideShaker peptideShaker = new PeptideShaker(projectParameters);

        // import the files
        peptideShaker.importFiles(waitingHandler, identificationFiles, spectrumFiles,
                identificationParameters, projectDetails, processingParameters, exceptionHandler);
        peptideShaker.createProject(identificationParameters, processingParameters, spectrumCountingParameters, projectDetails, waitingHandler, exceptionHandler);

        if (!waitingHandler.isRunCanceled()) {

            // identification as created by PeptideShaker
            identification = peptideShaker.getIdentification();

            // metrics saved while processing the data
            metrics = peptideShaker.getMetrics();

            // Gene maps
            geneMaps = peptideShaker.getGeneMaps();

            // the identification feature generator
            identificationFeaturesGenerator = peptideShaker.getIdentificationFeaturesGenerator();

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
     * Close the PeptideShaker instance. Closes file connections and deletes
     * temporary files.
     *
     * @param identification the identification to close
     *
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while writing the object
     * @throws SQLException exception thrown whenever an error occurred while
     * closing the database connection
     */
    public static void closePeptideShaker(Identification identification) throws IOException, SQLException {

        try {
            if (identification != null) {
                identification.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File matchFolder = PeptideShaker.getMatchesFolder();
            File[] tempFiles = matchFolder.listFiles();

            if (tempFiles != null) {
                for (File currentFile : tempFiles) {
                    boolean deleted = Util.deleteDir(currentFile);
                    if (!deleted) {
                        System.out.println(currentFile.getAbsolutePath() + " could not be deleted!"); // @TODO: better handling of this error?
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SpectrumFactory.getInstance().closeFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            TempFilesManager.deleteTempFolders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * PeptideShaker CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker command line takes identification files from search engines and creates a PeptideShaker project saved as cpsx file. Various exports can be generated from the project." + System.getProperty("line.separator")
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
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathParameters.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathParameters.loadPathParametersFromFile(pathConfigurationFile);
        }
    }

    /**
     * Redirects the error stream to the PeptideShaker.log of a given folder.
     *
     * @param logFolder the folder where to save the log
     */
    public static void redirectErrorStream(File logFolder) {

        try {
            logFolder.mkdirs();
            File file = new File(logFolder, "PeptideShaker.log");
            System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

            System.err.println(System.getProperty("line.separator") + System.getProperty("line.separator") + new Date()
                    + ": PeptideShaker version " + PeptideShaker.getVersion() + ".");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Total amount of memory in the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory: " + Runtime.getRuntime().freeMemory() + ".");
            System.err.println("Java version: " + System.getProperty("java.version") + ".");
        } catch (Exception e) {
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
            PeptideShakerCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (!PeptideShakerCLIInputBean.isValidStartup(line)) {
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
                PeptideShakerCLI lPeptideShakerCLI = new PeptideShakerCLI();
                PeptideShakerCLIInputBean lCLIBean = new PeptideShakerCLIInputBean(line);
                lPeptideShakerCLI.setPeptideShakerCLIInputBean(lCLIBean);
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
                + ", ptmFactory=" + modificationFactory
                + ", enzymeFactory=" + enzymeFactory
                + '}';
    }
}
