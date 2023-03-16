package eu.isas.peptideshaker.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.genes.ProteinGeneDetailsProvider;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.cli.identification_parameters.IdentificationParametersInputBean;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileExporter;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.gui.DummyFrame;
import com.compomics.util.experiment.io.temp.TempFilesManager;
import com.compomics.util.io.IoUtil;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.parameters.identification.advanced.ValidationQcParameters;
import eu.isas.peptideshaker.export.ProjectExport;
import eu.isas.peptideshaker.utils.PsdbParent;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import com.compomics.util.experiment.io.mass_spectrometry.cms.CmsFolder;
import com.compomics.util.experiment.io.mass_spectrometry.mgf.IndexedMgfReader;
import com.compomics.util.experiment.io.mass_spectrometry.mgf.MgfIndex;
import com.compomics.util.io.file.SerializationUtils;
import eu.isas.peptideshaker.utils.Properties;
import eu.isas.peptideshaker.utils.PsZipUtils;
import eu.isas.peptideshaker.utils.Tips;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.awt.Point;
import java.awt.Toolkit;
import org.apache.commons.cli.*;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import org.slf4j.LoggerFactory;

/**
 * A command line interface to run PeptideShaker.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerCLI extends PsdbParent implements Callable {

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
     * The log folder given on the command line. Null if not set.
     */
    private static File logFolder = null;

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

                waitingHandler = new WaitingDialog(
                        new DummyFrame("PeptideShaker " + PeptideShaker.getVersion(), "/icons/peptide-shaker.gif"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        false,
                        tips,
                        "Importing Data",
                        "PeptideShaker",
                        PeptideShaker.getVersion(),
                        true
                );

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

            // @TODO: improve the primary progress display?
            int progressCounter = cliInputBean.getIdFiles().size() + cliInputBean.getSpectrumFiles().size();
            progressCounter++; // establishing the database connection
            progressCounter++; // the FASTA file
            progressCounter++; // the peptide to protein map
            progressCounter += 6; // computing probabilities etc
            progressCounter++; // simplify protein groups
            progressCounter++; // resolving protein inference
            progressCounter += 4; // correcting protein probabilities, Validating identifications at 1% FDR, Scoring PTMs in peptides, Scoring PTMs in proteins.
            progressCounter += 2; // scoring PTMs in PSMs. Estimating PTM FLR.
            progressCounter++; // peptide inference

            // project zipping
            if (cliInputBean.getZipExport() != null) {
                progressCounter++;
            }

            // add one more just to not start at 0%
            progressCounter++;

            waitingHandler.setMaxPrimaryProgressCounter(progressCounter);
            waitingHandler.increasePrimaryProgressCounter(); // just to not start at 0%

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

            // turn off the zoodb logging
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger logger = loggerContext.getLogger("org.zoodb");
            logger.setLevel(Level.toLevel("ERROR"));

            setDbFolder(PeptideShaker.getMatchesFolder());

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

                waitingHandler.appendReport("An error occurred while loading the "
                        + "species mapping. Gene annotation might be impaired. "
                        + getLogFileMessage(), true, true);

                e.printStackTrace();

            }

            // create project
            try {

                createProject();

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while creating the "
                        + "PeptideShaker project. "
                        + getLogFileMessage(),
                        true,
                        true
                );

                e.printStackTrace();

                waitingHandler.setRunCanceled();

            }

            // see if the project was created or canceled
            if (waitingHandler.isRunCanceled()) {

                try {

                    closePeptideShaker(identification);

                } catch (Exception e) {

                    waitingHandler.appendReport(
                            "An error occurred while closing PeptideShaker. "
                            + getLogFileMessage(),
                            true,
                            true
                    );

                    e.printStackTrace();

                }

                System.exit(1);

                return 1;

            } else {

                waitingHandler.appendReport("Project successfully created.", true, true);

                waitingHandler.increasePrimaryProgressCounter();
            }

            // save project
            if (cliInputBean.getOutput() != null) {

                try {

                    psdbFile = cliInputBean.getOutput();
                    waitingHandler.appendReport("Saving results.", true, true);
                    saveProject(waitingHandler, true);
                    waitingHandler.appendReport("Results saved to " + psdbFile.getAbsolutePath() + ".", true, true);
                    waitingHandler.appendReportEndLine();

                } catch (Exception e) {

                    waitingHandler.appendReport(
                            "An exception occurred while saving the project. "
                            + getLogFileMessage(),
                            true,
                            true
                    );

                    e.printStackTrace();
                    waitingHandler.setRunCanceled();

                }

                waitingHandler.increasePrimaryProgressCounter();
            }

            // finished
            waitingHandler.setPrimaryProgressCounterIndeterminate(false);
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);

            // follow up tasks if needed
            FollowUpCLIInputBean followUpCLIInputBean = cliInputBean.getFollowUpCLIInputBean();

            // array to be filled with all exported follow-up reports
            ArrayList<File> followupAnalysisFiles = new ArrayList<File>();

            if (followUpCLIInputBean.followUpNeeded()) {

                waitingHandler.appendReport("Starting follow up tasks.", true, true);

                // recalibrate spectra
                if (followUpCLIInputBean.recalibrationNeeded()) {

                    waitingHandler.appendReport("Spectrum recalibration.", true, true);

                    try {

                        followupAnalysisFiles.addAll(
                                CLIExportMethods.recalibrateSpectra(
                                        followUpCLIInputBean,
                                        identification,
                                        sequenceProvider,
                                        msFileHandler,
                                        identificationParameters,
                                        waitingHandler
                                )
                        );

                    } catch (Exception e) {

                        waitingHandler.appendReport(
                                "An error occurred while recalibrating the spectra. " + getLogFileMessage(),
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

                        followupAnalysisFiles.addAll(
                                CLIExportMethods.exportSpectra(
                                        followUpCLIInputBean,
                                        identification,
                                        msFileHandler,
                                        waitingHandler,
                                        identificationParameters.getSequenceMatchingParameters()
                                )
                        );

                    } catch (Exception e) {

                        waitingHandler.appendReport(
                                "An error occurred while exporting the spectra. " + getLogFileMessage(),
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

                        followupAnalysisFiles.add(
                                CLIExportMethods.exportAccessions(
                                        followUpCLIInputBean,
                                        identification,
                                        sequenceProvider,
                                        waitingHandler,
                                        filterParameters
                                )
                        );

                    } catch (Exception e) {

                        waitingHandler.appendReport(
                                "An error occurred while exporting the protein accessions. " + getLogFileMessage(),
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

                        followupAnalysisFiles.add(
                                CLIExportMethods.exportProteinSequences(
                                        followUpCLIInputBean,
                                        identification,
                                        sequenceProvider,
                                        waitingHandler,
                                        filterParameters
                                )
                        );

                    } catch (Exception e) {

                        waitingHandler.appendReport(
                                "An error occurred while exporting the protein details. " + getLogFileMessage(),
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

                        followupAnalysisFiles.add(
                                CLIExportMethods.exportProgenesis(
                                        followUpCLIInputBean,
                                        identification,
                                        waitingHandler,
                                        sequenceProvider,
                                        proteinDetailsProvider,
                                        identificationParameters.getSequenceMatchingParameters()
                                )
                        );

                        waitingHandler.appendReport(
                                "Progenesis export completed.",
                                true,
                                true
                        );

                    } catch (Exception e) {

                        waitingHandler.appendReport(
                                "An error occurred while exporting the Progenesis file. " + getLogFileMessage(),
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

                        followupAnalysisFiles.add(
                                CLIExportMethods.exportInclusionList(
                                        followUpCLIInputBean,
                                        identification,
                                        identificationFeaturesGenerator,
                                        msFileHandler,
                                        identificationParameters.getSearchParameters(),
                                        waitingHandler,
                                        filterParameters
                                )
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

                        followupAnalysisFiles.add(
                                CLIExportMethods.exportProteoforms(
                                        followUpCLIInputBean,
                                        identification,
                                        waitingHandler
                                )
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

                        followupAnalysisFiles.addAll(
                                CLIExportMethods.exportDeepLC(
                                        followUpCLIInputBean,
                                        identification,
                                        identificationParameters.getSearchParameters().getModificationParameters(),
                                        identificationParameters.getSequenceMatchingParameters(),
                                        sequenceProvider,
                                        msFileHandler,
                                        waitingHandler
                                )
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

                // ms2pip export
                if (followUpCLIInputBean.ms2pipExportNeeded()) {

                    waitingHandler.appendReport("ms2pip export.", true, true);

                    try {

                        followupAnalysisFiles.addAll(
                                CLIExportMethods.exportMs2pip(
                                        followUpCLIInputBean,
                                        identification,
                                        identificationParameters.getSearchParameters(),
                                        identificationParameters.getSequenceMatchingParameters(),
                                        sequenceProvider,
                                        msFileHandler,
                                        waitingHandler
                                )
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
            }

            // report export if needed
            ReportCLIInputBean reportCLIInputBean = cliInputBean.getReportCLIInputBean();

            // array to be filled with all exported reports
            ArrayList<File> reportFiles = new ArrayList<File>();

            if (reportCLIInputBean.exportNeeded()) {

                // see if output folder is set, and if not set to the same folder as the psdb file
                boolean reportOutputFolderSet = reportCLIInputBean.getReportOutputFolder() != null;

                if (!reportOutputFolderSet) {

                    if (cliInputBean.getOutput() == null) {

                        waitingHandler.appendReport( // @TODO: ideally this test should be done before starting the processing of the data!
                                "Report output folder not set. Please use -out_reports (or the more general -out option). Processing canceled.",
                                true,
                                true
                        );

                        System.err.println("Report output folder not set. Please use -out_reports (or the more general -out option). Processing canceled.");
                        waitingHandler.setRunCanceled();

                    } else {

                        reportCLIInputBean.setReportOutputFolder(cliInputBean.getOutput().getParentFile());
                        reportOutputFolderSet = true;

                    }
                }

                if (reportOutputFolderSet) {

                    waitingHandler.appendReport("Exporting reports.", true, true);

                    // Export report(s)
                    if (reportCLIInputBean.exportNeeded()) {

                        int nSurroundingAAs = 2; //@TODO: this shall not be hard coded //peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides()

                        for (String reportType : reportCLIInputBean.getReportTypes()) {

                            waitingHandler.appendReport("Exporting " + reportType + ".", true, true);

                            try {

                                reportFiles.add(
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
                                                nSurroundingAAs,
                                                spectrumCountingParameters,
                                                waitingHandler
                                        )
                                );

                            } catch (Exception e) {

                                waitingHandler.appendReport(
                                        "An error occurred while exporting the " + reportType + ". " + getLogFileMessage(),
                                        true,
                                        true
                                );

                                e.printStackTrace();
                                waitingHandler.setRunCanceled();

                            }
                        }
                    }

                    // export documentation
                    if (reportCLIInputBean.documentationExportNeeded()) {

                        waitingHandler.appendReport("Exporting report documentation.", true, true);

                        for (String reportType : reportCLIInputBean.getReportTypes()) {

                            try {

                                CLIExportMethods.exportDocumentation(
                                        reportCLIInputBean,
                                        reportType,
                                        waitingHandler
                                );

                            } catch (Exception e) {

                                waitingHandler.appendReport(
                                        "An error occurred while exporting the documentation for "
                                        + reportType
                                        + ". "
                                        + getLogFileMessage(),
                                        true,
                                        true
                                );

                                e.printStackTrace();
                                waitingHandler.setRunCanceled();

                            }
                        }
                    }
                }
            }

            // export as mzid
            MzidCLIInputBean mzidCLIInputBean = cliInputBean.getMzidCLIInputBean();
            File mzidFile = mzidCLIInputBean.getOutputFile();

            if (mzidFile != null) {

                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport(
                        "Exporting project as mzIdentML.",
                        true,
                        true
                );

                // export mzid file
                // make sure that all annotations are included
                double currentIntensityLimit = this.getIdentificationParameters().getAnnotationParameters().getAnnotationIntensityLimit();
                this.getIdentificationParameters().getAnnotationParameters().setIntensityLimit(0.0);

                try {

                    CLIExportMethods.exportMzId(mzidCLIInputBean, this, waitingHandler);

                } catch (Exception e) {

                    waitingHandler.appendReport(
                            "An error occurred while generating the mzid file. " + getLogFileMessage(),
                            true,
                            true
                    );

                    e.printStackTrace();
                    waitingHandler.setRunCanceled();

                } finally {

                    // reset the annotation level
                    this.getIdentificationParameters().getAnnotationParameters().setIntensityLimit(currentIntensityLimit);

                }
            }

            // export project as zip
            File zipFile = cliInputBean.getZipExport();

            if (zipFile != null) {

                waitingHandler.appendReportEndLine();

                waitingHandler.appendReport(
                        "Zipping project.",
                        true,
                        true
                );

                File parent = zipFile.getParentFile();

                try {

                    parent.mkdirs();

                } catch (Exception e) {

                    waitingHandler.appendReport(
                            "An error occurred while creating folder " + parent.getAbsolutePath() + ". " + getLogFileMessage(),
                            true,
                            true
                    );

                    waitingHandler.setRunCanceled();

                }

                ArrayList<File> spectrumFiles = new ArrayList<>();

                for (String spectrumFileName : getIdentification().getFractions()) {

                    File spectrumFile = new File(msFileHandler.getFilePaths().get(spectrumFileName));
                    spectrumFiles.add(spectrumFile);

                }

                File fastaFile = new File(projectDetails.getFastaFile());

                try {

                    ProjectExport.exportProjectAsZip(
                            zipFile,
                            fastaFile,
                            msFileHandler,
                            followupAnalysisFiles,
                            reportFiles,
                            mzidFile,
                            psdbFile,
                            true,
                            waitingHandler
                    );

                    final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                    double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);

                    waitingHandler.appendReport(
                            "Project zipped to \'" + zipFile.getAbsolutePath() + "\' (" + sizeOfZippedFile + " MB)",
                            true,
                            true
                    );

                    // export mgf file(s) out of the zip file
                    boolean mgfExport = cliInputBean.getMgfExport(); // @TODO: what about non-mgf spectrum files..?

                    if (mgfExport) {

                        waitingHandler.appendReportEndLine();
                        waitingHandler.appendReport(
                                "Writing mgf file(s) to output folder.",
                                true,
                                true
                        );

                        int i = 0;

                        for (String spectrumFileName : msFileHandler.getCmsFilePaths().keySet()) {

                            if (waitingHandler.isRunCanceled()) {
                                break;
                            }

                            waitingHandler.appendReport(
                                    "Writing: " + IoUtil.removeExtension(spectrumFileName) + ".mgf"
                                    + " (" + (i + 1) + "/" + getIdentification().getFractions().size() + ")",
                                    true,
                                    true
                            );

                            File mgfFile = new File(parent,
                                    IoUtil.removeExtension(spectrumFileName) + ".mgf");

                            MsFileExporter.writeMgfFile(
                                    msFileHandler,
                                    false, // only include ms2 spectra
                                    spectrumFileName,
                                    mgfFile,
                                    waitingHandler);

                            // writing the index too
                            MgfIndex mgfIndex = IndexedMgfReader.getMgfIndex(mgfFile, waitingHandler);
                            File indexFile = new File(parent, IoUtil.removeExtension(mgfIndex.getFileName()) + ".cui");
                            SerializationUtils.writeObject(mgfIndex, indexFile);

                            i++;
                        }

                        waitingHandler.appendReport(
                                "MGF file(s) written to output folder.",
                                true,
                                true
                        );

                    }

                    waitingHandler.increasePrimaryProgressCounter();

                } catch (IOException e) {

                    e.printStackTrace();

                    waitingHandler.appendReport(
                            "An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ". " + getLogFileMessage(),
                            true,
                            true
                    );

                    waitingHandler.setRunCanceled();

                }
            }

            waitingHandler.appendReportEndLine();

            try {

                closePeptideShaker(identification);

            } catch (Exception e) {

                waitingHandler.appendReport(
                        "An error occurred while closing PeptideShaker. " + getLogFileMessage(),
                        true,
                        true
                );

                e.printStackTrace();
            }

            saveReport();

        } catch (Exception e) {

            e.printStackTrace();

            if (waitingHandler != null) {

                waitingHandler.appendReport(
                        "PeptideShaker processing failed. " + getLogFileMessage(),
                        true,
                        true
                );

                saveReport();
                waitingHandler.setRunCanceled();

            }
        }

        if (waitingHandler != null && !waitingHandler.isRunCanceled()) {

            waitingHandler.appendReport(
                    "PeptideShaker process completed.",
                    true,
                    true
            );

            waitingHandler.setSecondaryProgressText("Processing Completed.");
            System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
            // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!

            return 0;

        } else {

            System.out.println("PeptideShaker process failed! " + getLogFileMessage());
            System.exit(1); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.
            // Note that if a different solution is found, the DummyFrame has to be closed similar to the setVisible method in the WelcomeDialog!!

            return 1;

        }
    }

    /**
     * Save the peptide shaker report next to the psdb file.
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

            try {

                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
                File psReportFile = null;
                PathSettingsCLIInputBean pathSettingsCLIInputBean = cliInputBean.getPathSettingsCLIInputBean();

                if (getPsdbFile() != null) {
                    String fileName = "PeptideShaker Report " + getPsdbFile().getName() + " " + df.format(new Date()) + ".html";
                    psReportFile = new File(getPsdbFile().getParentFile(), fileName);
                } else if (cliInputBean.getOutput() != null) {
                    String fileName = "PeptideShaker Report " + df.format(new Date()) + ".html";
                    psReportFile = new File(cliInputBean.getOutput().getParentFile(), fileName);
                } else if (pathSettingsCLIInputBean.getLogFolder() != null) {
                    String fileName = "PeptideShaker Report " + df.format(new Date()) + ".html";
                    psReportFile = new File(pathSettingsCLIInputBean.getLogFolder(), fileName);
                }

                if (psReportFile != null) {

                    FileWriter fw = new FileWriter(psReportFile);

                    try {
                        fw.write(report);
                    } finally {
                        fw.close();
                    }

                }

            } catch (Exception ex) {

                waitingHandler.appendReport(
                        "An error occurred while saving the PeptideShaker report. "
                        + getLogFileMessage(),
                        true,
                        true
                );

                ex.printStackTrace();

            }
        }
    }

    /**
     * Creates the PeptideShaker project based on the identification files
     * provided in the command line input
     *
     * @throws java.io.IOException exception thrown if an error occurs while
     * reading or writing a file
     * @throws java.lang.InterruptedException exception thrown if a thread is
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if a
     * process times out
     */
    public void createProject() throws IOException, InterruptedException, TimeoutException {

        // define new project reference
        projectParameters = new ProjectParameters(cliInputBean.getExperimentID());

        // set the project details
        projectDetails = new ProjectDetails();
        projectDetails.setCreationDate(new Date());
        projectDetails.setPeptideShakerVersion(new Properties().getVersion());

        // set up spectrum provider
        msFileHandler = new MsFileHandler();

        // get the input files
        ArrayList<File> identificationFilesInput = cliInputBean.getIdFiles();
        ArrayList<File> dataFolders = new ArrayList<>();
        ArrayList<File> spectrumFiles = cliInputBean.getSpectrumFiles();
        File fastaFile = null;

        // Extract data from zip files, try to find the search parameter and spectrum files
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

            dataFolder = new File(parentFile, "mzml");

            if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                dataFolders.add(dataFolder);
            }

            dataFolder = new File(parentFile, "cms");

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

                dataFolder = new File(parentFile, "mzml");

                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }

                dataFolder = new File(destinationFolder, ".cms");

                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }

                dataFolder = new File(destinationFolder, ".fasta");

                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }

                for (File unzippedFile : destinationFolder.listFiles()) {

                    String nameLowerCase = unzippedFile.getName().toLowerCase();

                    if (nameLowerCase.endsWith(".omx")
                            || nameLowerCase.endsWith(".t.xml")
                            || nameLowerCase.endsWith(".pep.xml")
                            || nameLowerCase.endsWith(".dat")
                            || nameLowerCase.endsWith(".mzid")
                            || nameLowerCase.endsWith(".ms-amanda.csv")
                            || nameLowerCase.endsWith(".res")
                            || nameLowerCase.endsWith(".tide-search.target.txt")
                            || nameLowerCase.endsWith(".tags")
                            || nameLowerCase.endsWith(".pnovo.txt")
                            || nameLowerCase.endsWith(".novor.csv")
                            || nameLowerCase.endsWith(".coss.tsv")
                            || nameLowerCase.endsWith(".sage.tsv")
                            || nameLowerCase.endsWith(".psm")
                            || nameLowerCase.endsWith(".omx.gz")
                            || nameLowerCase.endsWith(".t.xml.gz")
                            || nameLowerCase.endsWith(".pep.xml.gz")
                            || nameLowerCase.endsWith(".mzid.gz")
                            || nameLowerCase.endsWith(".ms-amanda.csv.gz")
                            || nameLowerCase.endsWith(".res.gz")
                            || nameLowerCase.endsWith(".tide-search.target.txt.gz")
                            || nameLowerCase.endsWith(".tags.gz")
                            || nameLowerCase.endsWith(".pnovo.txt.gz")
                            || nameLowerCase.endsWith(".novor.csv.gz")
                            || nameLowerCase.endsWith(".coss.tsv.gz")
                            || nameLowerCase.endsWith(".sage.tsv.gz")
                            || nameLowerCase.endsWith(".psm.gz")) {

                        identificationFiles.add(unzippedFile);

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

                            e.printStackTrace();

                            waitingHandler.appendReport(
                                    "An error occurred while parsing the parameters file "
                                    + unzippedFile.getName()
                                    + ". " + getLogFileMessage(),
                                    true,
                                    true
                            );

                            waitingHandler.setRunCanceled();

                        }
                    }
                }
            } else {
                identificationFiles.add(inputFile);
            }
        }

        // list the spectrum files found
        HashSet<String> dataFileNamesRequired = new HashSet<>();

        for (File spectrumFile : spectrumFiles) {
            dataFileNamesRequired.add(IoUtil.getFileName(spectrumFile));
        }

        for (File dataFolder : dataFolders) {

            for (File file : dataFolder.listFiles()) {

                String name = file.getName();

                if (name.endsWith(".mgf") || name.endsWith(".mgf.gz")
                        || name.endsWith(".mzml") || name.endsWith(".mzml.gz")
                        || name.endsWith(".cms")) {

                    if (!dataFileNamesRequired.contains(name)) {

                        spectrumFiles.add(file);
                        dataFileNamesRequired.add(name);

                    }

                } else if (name.endsWith(".fasta")) {

                    if (!dataFileNamesRequired.contains(name)) {

                        fastaFile = file;
                        dataFileNamesRequired.add(name);

                    }
                }
            }
        }

        // Load the spectrum files
        for (File spectrumFile : spectrumFiles) {

            File folder = CmsFolder.getParentFolder() == null ? spectrumFile.getParentFile() : new File(CmsFolder.getParentFolder());
            msFileHandler.register(spectrumFile, folder, waitingHandler);

        }

        // If there is a specific fasta file chosen, it is used insted of the one included in the searchgui zip
        if (cliInputBean.getFastaFile() != null) {
            fastaFile = cliInputBean.getFastaFile();
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

            waitingHandler.appendReport(
                    "Identification parameters not found!",
                    true,
                    true
            );

            waitingHandler.setRunCanceled();

        }

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        String error = PeptideShaker.loadModifications(searchParameters);

        if (error != null) {
            System.out.println(error);
        }

        // try to locate the fasta file
        if (fastaFile == null) {

            waitingHandler.appendReport(
                    "FASTA file not set (or not in zip file)!",
                    true,
                    true
            );

            waitingHandler.setRunCanceled();

        } else if (!fastaFile.exists()) {

            boolean found = false;

            // look in the database folder
            try {

                File tempDbFolder = utilitiesUserParameters.getDbFolder();
                File newFile = new File(tempDbFolder, fastaFile.getName());

                if (newFile.exists()) {
                    fastaFile = newFile;
                    projectDetails.setFastaFile(fastaFile);
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
                        projectDetails.setFastaFile(fastaFile);
                        found = true;
                        break;

                    }

                }

                if (!found) {
                    waitingHandler.appendReport("FASTA file \'" + fastaFile.getName() + "\' not found.", true, true);
                    waitingHandler.setRunCanceled();
                }

            }

        } else {
            projectDetails.setFastaFile(fastaFile);
        }

        // get the summary information for the FASTA file
        try {

            // get the FASTA summary
            FastaSummary fastaSummary = loadFastaFile(waitingHandler);

            // set the background species
            identificationParameters.getGeneParameters().setBackgroundSpeciesFromFastaSummary(fastaSummary);

        } catch (IOException e) {

            e.printStackTrace();
            waitingHandler.appendReport("An error occurred while parsing the FASTA file.", true, true);
            waitingHandler.setRunCanceled();

        }

        // set the processing settings
        ProcessingParameters processingParameters = new ProcessingParameters();
        Integer nThreads = cliInputBean.getnThreads();

        if (nThreads != null) {
            processingParameters.setnThreads(nThreads);
        }

        Boolean cachePercolatorFeatures = cliInputBean.getCachePercolatorFeatures();

        if (cachePercolatorFeatures != null) {
            processingParameters.setCachePercolatorFeatures(cachePercolatorFeatures);
        }

        // set the spectrum counting prefrences
        spectrumCountingParameters = new SpectrumCountingParameters();

        // set the project type
        projectType = cliInputBean.getProjectType();

        // check the project reference
        for (String forbiddenChar : Util.FORBIDDEN_CHARACTERS) {

            if (cliInputBean.getExperimentID().contains(forbiddenChar)) {
                waitingHandler.appendReport("The project name cannot not contain " + forbiddenChar + ".", true, true);
                waitingHandler.setRunCanceled();
            }

        }

        // incrementing the counter for a new PeptideShaker start run via CLI
        if (utilitiesUserParameters.isAutoUpdate()) {
            Util.sendGAUpdate("UA-36198780-1", "startrun-cl", "peptide-shaker-" + PeptideShaker.getVersion());
        }

        // create a shaker which will perform the analysis
        PeptideShaker peptideShaker = new PeptideShaker(projectParameters);

        // import the files
        int outcome = peptideShaker.importFiles(
                waitingHandler,
                identificationFiles,
                msFileHandler,
                identificationParameters,
                projectDetails,
                processingParameters,
                exceptionHandler
        );

        if (outcome == 0) {

            peptideShaker.createProject(
                    identificationParameters,
                    processingParameters,
                    spectrumCountingParameters,
                    msFileHandler,
                    projectDetails,
                    projectType,
                    waitingHandler,
                    false,
                    exceptionHandler
            );

        }

        if (!waitingHandler.isRunCanceled()) {

            // identification as created by PeptideShaker
            identification = peptideShaker.getIdentification();

            // metrics saved while processing the data
            metrics = peptideShaker.getMetrics();

            // fene maps
            geneMaps = peptideShaker.getGeneMaps();

            // the identification feature generator
            identificationFeaturesGenerator = peptideShaker.getIdentificationFeaturesGenerator();

            // the sequence provider
            sequenceProvider = peptideShaker.getSequenceProvider();

            // the protein details provider
            proteinDetailsProvider = peptideShaker.getProteinDetailsProvider();

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
            System.out.println("<CompomicsError>PeptideShaker processing canceled. " + getLogFileMessage() + "</CompomicsError>");

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
                identification.close(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            File matchFolder = PeptideShaker.getMatchesFolder();
            File[] tempFiles = matchFolder.listFiles();

            if (tempFiles != null) {

                for (File currentFile : tempFiles) {

                    boolean deleted = IoUtil.deleteDir(currentFile);

                    if (!deleted) {

                        System.out.println(currentFile.getAbsolutePath() + " could not be deleted!"); // @TODO: better handling of this error?

                    }

                }

            }

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
                + "The PeptideShaker command line takes identification files from search engines and creates a PeptideShaker project saved as psdb file. Various exports can be generated from the project." + System.getProperty("line.separator")
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
     * Loads the species from the species file into the species factory.
     */
    private void loadSpecies() {

        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(PeptideShaker.getJarFilePath());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the species. " + getLogFileMessage());
            e.printStackTrace();
        }

    }

    /**
     * Redirects the error stream to the PeptideShaker.log of a given folder.
     *
     * @param aLogFolder the folder where to save the log
     */
    public static void redirectErrorStream(File aLogFolder) {

        logFolder = aLogFolder;

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
     *
     * Returns the "see the log file" message. With the path if available.
     *
     * @return the "see the log file" message
     */
    public static String getLogFileMessage() {

        if (logFolder == null) {
            return "Please see the PeptideShaker log file.";
        } else {
            return "Please see the PeptideShaker log file: " + logFolder.getAbsolutePath() + File.separator + "PeptideShaker.log";
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

            // parse the rest of the cptions   
            Options nonPathOptions = new Options();
            PeptideShakerCLIParams.createOptionsCLI(nonPathOptions);
            DefaultParser parser = new DefaultParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

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

            System.out.println("<CompomicsError>PeptideShaker used up all the memory and had to be stopped. " + getLogFileMessage() + "</CompomicsError>");
            System.err.println("Ran out of memory!");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
            e.printStackTrace();

        } catch (Exception e) {

            System.out.println("<CompomicsError>PeptideShaker processing failed. " + getLogFileMessage() + "</CompomicsError>");
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
