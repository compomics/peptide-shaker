package eu.isas.peptideshaker.fileimport;

import com.compomics.mascotdatfile.util.io.MascotIdfileReader;
import com.compomics.util.preferences.IdFilter;
import eu.isas.peptideshaker.gui.MgfFilesNotFoundDialog;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.io.identifications.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MsAmandaIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MzIdentMLIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.PepxmlIdfileReader;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.WaitingDialogExceptionHandler;
import com.compomics.util.gui.JOptionEditorPane;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import de.proteinms.omxparser.util.OMSSAIdfileReader;
import de.proteinms.xtandemparser.parser.XTandemIdfileReader;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.protein_inference.PeptideMapper;
import eu.isas.peptideshaker.protein_inference.TagMapper;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.utils.Metrics;
import org.xml.sax.SAXException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * This class is responsible for the import of identifications.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FileImporter {

    /**
     * The class which will load the information into the various maps and do
     * the associated calculations.
     */
    private PeptideShaker peptideShaker;
    /**
     * The current proteomicAnalysis.
     */
    private ProteomicAnalysis proteomicAnalysis;
    /**
     * The identification filter to use.
     */
    private IdFilter idFilter;
    /**
     * A dialog to display feedback to the user.
     */
    private WaitingHandler waitingHandler;
    /**
     * An exception handler to handle exceptions
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The modification factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance(30000);
    /**
     * If a Mascot dat file is bigger than this size, an indexed parsing will be
     * used.
     */
    public static final double mascotMaxSize = 400;
    /**
     * Metrics of the dataset picked-up while loading the data.
     */
    private Metrics metrics;
    /**
     * The mass tolerance to be used to match PTMs from search engines and
     * expected PTMs. 0.01 by default, as far as I can remember it is the mass
     * resolution in X!Tandem result files.
     */
    public static final double ptmMassTolerance = 0.01;
    /**
     * The protein tree used to map peptides on protein sequences.
     */
    private ProteinTree proteinTree;
    /**
     * Suffix for folders where the content of zip files should be extracted.
     */
    public final static String tempFolderName = "PeptideShaker_temp";

    /**
     * Constructor for the importer.
     *
     * @param identificationShaker the identification shaker which will load the
     * data into the maps and do the preliminary calculations
     * @param waitingHandler The handler displaying feedback to the user
     * @param proteomicAnalysis The current proteomic analysis
     * @param idFilter The identification filter to use
     * @param metrics metrics of the dataset to be saved for the GUI
     */
    public FileImporter(PeptideShaker identificationShaker, WaitingHandler waitingHandler, ProteomicAnalysis proteomicAnalysis, IdFilter idFilter, Metrics metrics) {
        this.peptideShaker = identificationShaker;
        this.waitingHandler = waitingHandler;
        this.proteomicAnalysis = proteomicAnalysis;
        this.idFilter = idFilter;
        this.metrics = metrics;
        if (waitingHandler instanceof WaitingDialog) {
            exceptionHandler = new WaitingDialogExceptionHandler((WaitingDialog) waitingHandler, "http://code.google.com/p/peptide-shaker/issues/list");
        } else {
            exceptionHandler = new CommandLineExceptionHandler();
        }
    }

    /**
     * Imports the identification from files.
     *
     * @param idFiles the identification files to import the Ids from
     * @param spectrumFiles the files where the corresponding spectra can be
     * imported
     * @param searchParameters the search parameters
     * @param annotationPreferences the annotation preferences to use for PTM
     * scoring
     * @param processingPreferences the processing preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param projectDetails the project details
     * @param backgroundThread boolean indicating whether the import should be
     * done in a background thread (GUI mode) or in the current thread (command
     * line mode).
     */
    public void importFiles(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, ProcessingPreferences processingPreferences, PTMScoringPreferences ptmScoringPreferences,
            SpectrumCountingPreferences spectrumCountingPreferences, SequenceMatchingPreferences sequenceMatchingPreferences, ProjectDetails projectDetails, boolean backgroundThread) {

        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles, spectrumFiles, idFilter, searchParameters, annotationPreferences,
                processingPreferences, ptmScoringPreferences, spectrumCountingPreferences, sequenceMatchingPreferences, projectDetails);

        if (backgroundThread) {
            idProcessor.execute();
        } else {
            idProcessor.importFiles();
        }
    }

    /**
     * Imports sequences from a FASTA file.
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param fastaFile FASTA file to process
     */
    public void importSequences(WaitingHandler waitingHandler, File fastaFile) {

        try {
            waitingHandler.appendReport("Importing sequences from " + fastaFile.getName() + ".", true, true);
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            sequenceFactory.loadFastaFile(fastaFile, waitingHandler);

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);

            UtilitiesUserPreferences userPreferences = UtilitiesUserPreferences.loadUserPreferences();
            int memoryPreference = userPreferences.getMemoryPreference();
            long fileSize = fastaFile.length();
            long nSequences = sequenceFactory.getNTargetSequences();
            if (!sequenceFactory.isDefaultReversed()) {
                nSequences = sequenceFactory.getNSequences();
            }
            long sequencesPerMb = 1048576 * nSequences / fileSize;
            long availableCachSize = 3 * memoryPreference * sequencesPerMb / 4;
            if (availableCachSize > nSequences) {
                availableCachSize = nSequences;
            } else {
                waitingHandler.appendReport("Warning: PeptideShaker cannot load your FASTA file into memory. This will slow down the processing. "
                        + "Note that using large large databases also reduces the search engine efficiency. "
                        + "Try to either (i) use a smaller database, (ii) increase the memory provided to PeptideShaker, or (iii) improve the reading speed by using an SSD disc. "
                        + "(See also http://code.google.com/p/compomics-utilities/wiki/ProteinInference.)", true, true);

            }
            int cacheSize = (int) availableCachSize;
            sequenceFactory.setnCache(cacheSize);

            try {
                proteinTree = sequenceFactory.getDefaultProteinTree(waitingHandler);
            } catch (SQLException e) {
                waitingHandler.appendReport("Database " + sequenceFactory.getCurrentFastaFile().getName() + " could not be accessed, make sure that the file is not used by another program.", true, true);
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }

            if (!waitingHandler.isRunCanceled()) {
                waitingHandler.appendReport("FASTA file import completed.", true, true);
                waitingHandler.increasePrimaryProgressCounter();
            } else {
                sequenceFactory.clearFactory();
            }

        } catch (FileNotFoundException e) {
            System.err.println("File " + fastaFile + " was not found. Please select a different FASTA file.");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("File " + fastaFile + " was not found. Please select a different FASTA file.", true, true);
        } catch (IOException e) {
            System.err.println("An error occured while indexing " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occured while indexing " + fastaFile + ": " + e.getMessage(), true, true);
        } catch (SQLException e) {
            System.err.println("An error occured while indexing " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occured while indexing " + fastaFile + ": " + e.getMessage(), true, true);
        } catch (InterruptedException e) {
            System.err.println("An error occured while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occured while loading " + fastaFile + ": " + e.getMessage(), true, true);
        } catch (IllegalArgumentException e) {
            System.err.println("An error occured while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport(e.getLocalizedMessage() + " Please refer to http://code.google.com/p/peptide-shaker/#Troubleshooting", true, true);
        } catch (ClassNotFoundException e) {
            System.err.println("An error occured while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("Serialization issue while processing the FASTA file. Please delete the .fasta.cui file and retry. "
                    + "If the error occurs again please report bug using our issue tracker: http://code.google.com/p/peptide-shaker/issues/list.", true, true);
        } catch (NullPointerException e) {
            System.err.println("An error occured while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occurred when importing the sequences. "
                    + "Please check the Search Parameters. See the log file for details. "
                    + "If the error persists please let us know using our issue tracker: http://code.google.com/p/peptide-shaker/issues/list.", true, true);
        }
    }

    /**
     * Worker which loads identification from a file and processes them while
     * giving feedback to the user.
     */
    private class IdProcessorFromFile extends SwingWorker {

        /**
         * The identification file reader factory of compomics utilities.
         */
        private IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
        /**
         * The list of identification files.
         */
        private ArrayList<File> idFiles;
        /**
         * The FASTA file.
         */
        private File fastaFile;
        /**
         * A list of spectrum files (can be empty, no spectrum will be
         * imported).
         */
        private HashMap<String, File> spectrumFiles;
        /**
         * The identification filter.
         */
        private IdFilter idFilter;
        /**
         * The search parameters.
         */
        private SearchParameters searchParameters;
        /**
         * The annotation preferences to use for PTM scoring.
         */
        private AnnotationPreferences annotationPreferences;
        /**
         * The processing preferences.
         */
        private ProcessingPreferences processingPreferences;
        /**
         * The PTM scoring preferences.
         */
        private PTMScoringPreferences ptmScoringPreferences;
        /**
         * The project details
         */
        private ProjectDetails projectDetails;
        /**
         * The spectrum counting preferences.
         */
        private SpectrumCountingPreferences spectrumCountingPreferences;
        /**
         * The sequence matching preferences.
         */
        private SequenceMatchingPreferences sequenceMatchingPreferences;
        /**
         * The number of retained first hits.
         */
        private long nRetained = 0;
        /**
         * The number of spectra.
         */
        private long nSpectra = 0;
        /**
         * The number of first hits.
         */
        private long nPSMs = 0;
        /**
         * The number of secondary hits.
         */
        private long nSecondary = 0;
        /**
         * List of the mgf files used.
         */
        private ArrayList<String> mgfUsed = new ArrayList<String>();
        /**
         * Map of the missing mgf files indexed by identification file.
         */
        private HashMap<File, String> missingMgfFiles = new HashMap<File, String>();
        /**
         * The input map.
         */
        private InputMap inputMap = new InputMap();
        /**
         * List of one hit wonders.
         */
        private ArrayList<String> singleProteinList = new ArrayList<String>();
        /**
         * Map of proteins found several times with the number of times they
         * appeared as first hit.
         */
        private HashMap<String, Integer> proteinCount = new HashMap<String, Integer>();
        /**
         * Boolean indicating whether we can display GUI stuff.
         */
        private boolean hasGUI = false;
        /**
         * The database connection.
         */
        private Identification identification;
        /**
         * Indicates whether the check for X!Tandem modifications was done.
         */
        private boolean xTandemPtmsCheck = false;
        /**
         * A peptide to protein mapper.
         */
        private PeptideMapper peptideMapper;
        /**
         * A tag to protein mapper.
         */
        private TagMapper tagMapper = null;

        /**
         * Constructor for a worker importing matches from a list of files.
         *
         * @param idFiles list of identification files from where matches should
         * be imported
         * @param spectrumFiles list of spectrum files where the searched
         * spectra can be found
         * @param idFilter the matches filter to use
         * @param searchParameters the identification parameters
         * @param annotationPreferences the annotation preferences
         * @param processingPreferences the processing preferences
         * @param ptmScoringPreferences the PTM localization scoring preferences
         * @param spectrumCountingPreferences the spectrum counting preferences
         * @param sequenceMatchingPreferences the sequence matching preferences
         * @param projectDetails the project details
         */
        public IdProcessorFromFile(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, IdFilter idFilter,
                SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ProcessingPreferences processingPreferences,
                PTMScoringPreferences ptmScoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences, SequenceMatchingPreferences sequenceMatchingPreferences, ProjectDetails projectDetails) {

            this.idFiles = new ArrayList<File>();
            HashMap<String, File> filesMap = new HashMap<String, File>();

            for (File file : idFiles) {
                filesMap.put(file.getName(), file);
            }

            ArrayList<String> names = new ArrayList<String>(filesMap.keySet());
            Collections.sort(names);

            // Process sequencing files first, they need much more memory. TODO: make something more generic?
            for (String name : names) {
                if (name.endsWith("tags")) {
                    this.idFiles.add(filesMap.get(name));
                }
            }
            for (String name : names) {
                File file = filesMap.get(name);
                if (!this.idFiles.contains(file)) {
                    this.idFiles.add(file);
                }
            }

            this.spectrumFiles = new HashMap<String, File>();
            this.fastaFile = searchParameters.getFastaFile();
            this.idFilter = idFilter;
            this.searchParameters = searchParameters;
            this.annotationPreferences = annotationPreferences;
            this.processingPreferences = processingPreferences;
            this.ptmScoringPreferences = ptmScoringPreferences;
            this.spectrumCountingPreferences = spectrumCountingPreferences;
            this.projectDetails = projectDetails;
            this.sequenceMatchingPreferences = sequenceMatchingPreferences;

            for (File file : spectrumFiles) {
                this.spectrumFiles.put(file.getName(), file);
            }

            tagMapper = new TagMapper(proteinTree, searchParameters, sequenceMatchingPreferences, annotationPreferences, exceptionHandler);
            peptideMapper = new PeptideMapper(sequenceMatchingPreferences, idFilter, waitingHandler, exceptionHandler);
        }

        @Override
        protected Object doInBackground() throws Exception {
            hasGUI = true;
            return importFiles();
        }

        /**
         * Imports the identifications from the files given to the worker.
         *
         * @return 0 if success, 1 if not
         */
        public int importFiles() {

            try {
                importSequences(waitingHandler, fastaFile);

                if (waitingHandler.isRunCanceled()) {
                    return 1;
                }

                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                waitingHandler.appendReport("Establishing local database connection.", true, true);

                identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
                identification.setIsDB(true);

                connectToIdDb(identification);

                waitingHandler.increasePrimaryProgressCounter();

                if (!waitingHandler.isRunCanceled()) {

                    waitingHandler.appendReport("Reading identification files.", true, true);

                    for (File idFile : idFiles) {
                        importPsms(idFile);

                        if (waitingHandler.isRunCanceled()) {
                            identification.close();
                            return 1;
                        }
                    }

                    while (!missingMgfFiles.isEmpty()) {
                        if (hasGUI) {
                            new MgfFilesNotFoundDialog((WaitingDialog) waitingHandler, missingMgfFiles);
                            if (waitingHandler.isRunCanceled()) {
                                identification.close();
                                sequenceFactory.clearFactory();
                                return 1;
                            }
                        } else {
                            String missingFiles = "";
                            boolean first = true;
                            for (File mgfFile : missingMgfFiles.keySet()) {
                                if (first) {
                                    first = false;
                                } else {
                                    missingFiles += ", ";
                                }
                                missingFiles += mgfFile.getName();
                            }
                            waitingHandler.appendReport("MGF files missing: " + missingFiles, true, true);
                            identification.close();
                            sequenceFactory.clearFactory();
                            return 1;
                        }
                        waitingHandler.appendReport("Processing files with the new input.", true, true);
                        ArrayList<File> filesToProcess = new ArrayList<File>(missingMgfFiles.keySet());

                        for (String mgfName : missingMgfFiles.values()) {
                            File newFile = spectrumFactory.getSpectrumFileFromIdName(mgfName);
                            spectrumFiles.put(newFile.getName(), newFile);
                            projectDetails.addSpectrumFile(newFile);
                        }
                        missingMgfFiles.clear();
                        for (File idFile : filesToProcess) {
                            importPsms(idFile);
                        }
                        if (waitingHandler.isRunCanceled()) {
                            identification.close();
                            sequenceFactory.clearFactory();
                            return 1;
                        }
                    }

                    // clear the objects not needed anymore
                    singleProteinList.clear();
                    sequenceFactory.emptyCache();

                    if (nRetained == 0) {
                        waitingHandler.appendReport("No identifications retained.", true, true);
                        waitingHandler.setRunCanceled();
                        identification.close();
                        sequenceFactory.clearFactory();
                        return 1;
                    }

                    waitingHandler.appendReport("File import completed. "
                            + nPSMs + " first hits imported (" + nSecondary + " secondary) from " + nSpectra + " spectra.", true, true);
                    waitingHandler.appendReport("[" + nRetained + " first hits passed the initial filtering]", true, true);
                    waitingHandler.increaseSecondaryProgressCounter(spectrumFiles.size() - mgfUsed.size());
                    peptideShaker.setProteinCountMap(proteinCount);
                    peptideShaker.processIdentifications(inputMap, waitingHandler, searchParameters, annotationPreferences,
                            idFilter, processingPreferences, ptmScoringPreferences, spectrumCountingPreferences, projectDetails, sequenceMatchingPreferences);
                }
            } catch (OutOfMemoryError error) {

                System.out.println("<CompomicsError>PeptideShaker ran out of memory! See the PeptideShaker log for details.</CompomicsError>");
                System.err.println("Ran out of memory!");
                System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
                System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
                System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");

                Runtime.getRuntime().gc();
                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Ran out of memory!", true, true);
                waitingHandler.setRunCanceled();

                if (waitingHandler instanceof WaitingDialog) {
                    JOptionPane.showMessageDialog((WaitingDialog) waitingHandler, JOptionEditorPane.getJOptionEditorPane(
                            "PeptideShaker used up all the available memory and had to be stopped.<br>"
                            + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                            + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                            + "Java Options). See also <a href=\"http://code.google.com/p/compomics-utilities/wiki/JavaTroubleShooting\">JavaTroubleShooting</a>."),
                            "Out Of Memory", JOptionPane.ERROR_MESSAGE);
                }

                error.printStackTrace();
                if (identification != null) {
                    try {
                        identification.close();
                        sequenceFactory.clearFactory();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return 1;

            } catch (Exception e) {
                waitingHandler.setRunCanceled();

                System.out.println("<CompomicsError>PeptideShaker processing failed. See the PeptideShaker log for details.</CompomicsError>");

                if (e instanceof NullPointerException) {
                    waitingHandler.appendReport("An error occured while loading the identification files.", true, true);
                    waitingHandler.appendReport("Please see the error log (Help Menu > Bug Report) for details.", true, true);
                } else if (FrameExceptionHandler.getExceptionType(e).equalsIgnoreCase("Protein not found")) {
                    waitingHandler.appendReport("An error occured while loading the identification files:", true, true);
                    waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
                    waitingHandler.appendReport("Please see http://code.google.com/p/searchgui/wiki/DatabaseHelp.", true, true);
                } else {
                    waitingHandler.appendReport("An error occured while loading the identification files:", true, true);
                    waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
                }

                e.printStackTrace();
                System.err.println("Free memory: " + Runtime.getRuntime().freeMemory());

                if (identification != null) {
                    try {
                        identification.close();
                        sequenceFactory.clearFactory();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                return 1;
            }

            return 0;
        }

        /**
         * Establishes a connection to the identification database.
         *
         * @param identification
         */
        private void connectToIdDb(Identification identification) {
            try {
                String dbFolder = PeptideShaker.getSerializationDirectory(getJarFilePath()).getAbsolutePath();
                identification.establishConnection(dbFolder, true, peptideShaker.getCache());
            } catch (SQLException e) {
                e.printStackTrace();
                waitingHandler.appendReport("The match database could not be created, serialized matches will be used instead. "
                        + "Please let us know using our issue tracker: http://code.google.com/p/peptide-shaker/issues/list.", true, true);
                identification.setIsDB(false);
            }
        }

        /**
         * Imports the PSMs from an identification file.
         *
         * @param idFile the identification file
         * @throws FileNotFoundException exception thrown whenever a file was
         * not found
         * @throws IOException exception thrown whenever an error occurred while
         * reading or writing a file
         * @throws SAXException exception thrown whenever an error occurred
         * while parsing an XML file
         * @throws MzMLUnmarshallerException exception thrown whenever an error
         * occurred while reading an mzML file
         * @throws OutOfMemoryError thrown if the parser if the id files runs
         * out of memory
         */
        public void importPsms(File idFile) throws FileNotFoundException, IOException, SAXException, MzMLUnmarshallerException, IllegalArgumentException, Exception, OutOfMemoryError {

            identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            waitingHandler.appendReport("Parsing " + idFile.getName() + ".", true, true);
            ArrayList<Integer> ignoredOMSSAModifications = new ArrayList<Integer>();

            IdfileReader fileReader = null;
            try {
                fileReader = readerFactory.getFileReader(idFile);
            } catch (OutOfMemoryError error) {
                waitingHandler.appendReport("Ran out of memory when parsing \'" + Util.getFileName(idFile) + "\'.", true, true);
                throw new OutOfMemoryError("Ran out of memory when parsing \'" + Util.getFileName(idFile) + "\'.");
            }

            if (fileReader == null) {
                waitingHandler.appendReport("Identification result file \'" + Util.getFileName(idFile) + "\' not recognized.", true, true);
                waitingHandler.setRunCanceled();
                return;
            }

            // Clear cache for sequencing files. TODO: make something more generic?
            if (idFile.getName().endsWith("tags") && !peptideShaker.getCache().isEmpty()) {
                peptideShaker.getCache().reduceMemoryConsumption(0.9, waitingHandler);
            }

            waitingHandler.setSecondaryProgressCounterIndeterminate(false);

            LinkedList<SpectrumMatch> idFileSpectrumMatches = null;
            try {
                if (!peptideMapper.isCanceled()) {
                    idFileSpectrumMatches = fileReader.getAllSpectrumMatches(waitingHandler, sequenceMatchingPreferences, true);
                } else {
                    idFileSpectrumMatches = fileReader.getAllSpectrumMatches(waitingHandler, null, true);
                }
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while loading spectrum matches from \'"
                        + Util.getFileName(idFile)
                        + "\'. This file will be ignored. Error: " + e.getMessage()
                        + " See resources/PeptideShaker.log for details.", true, true);
                e.printStackTrace();
            }

            // set the search engine name and version for this file
            HashMap<String, ArrayList<String>> software = fileReader.getSoftwareVersions();
            projectDetails.setIdentificationAlgorithmsForFile(Util.getFileName(idFile), software);

            fileReader.close();

            if (idFileSpectrumMatches != null) {

                boolean allLoaded = true;
                int numberOfMatches = idFileSpectrumMatches.size();
                waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                waitingHandler.appendReport("Loading spectra for " + idFile.getName() + ".", true, true);
                for (SpectrumMatch spectrumMatch : idFileSpectrumMatches) {
                    if (!importSpectrum(idFile, spectrumMatch, numberOfMatches)) {
                        allLoaded = false;
                        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
                        waitingHandler.appendReport(fileName + " missing.", true, true);
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }

                if (allLoaded) {

                    int progress = 0,
                            psmsRejected = 0,
                            proteinIssue = 0,
                            peptideIssue = 0,
                            precursorIssue = 0,
                            ptmIssue = 0;

                    // Map spectrum sequencing matches on protein sequences
                    tagMapper.mapTags(fileReader, waitingHandler, processingPreferences.getnThreads());

                    // Map the peptides on protein sequences
                    try {
                        if (!peptideMapper.isCanceled()) {
                            peptideMapper.mapPeptides(fileReader.getPeptidesMap(), sequenceMatchingPreferences, idFilter, processingPreferences.getnThreads(), waitingHandler);
                        }
                        if (peptideMapper.isCanceled()) {
                            fileReader.clearPeptidesMap();
                        }
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        fileReader.clearPeptidesMap();
                    }
                    // empty protein caches
                    if (MemoryConsumptionStatus.memoryUsed() > 0.8) {
                        ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(1, null);
                        sequenceFactory.reduceNodeCacheSize(1);
                    }

                    waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                    waitingHandler.appendReport("Importing PSMs from " + idFile.getName(), true, true);
                    HashSet<Integer> charges = new HashSet<Integer>();
                    double maxPeptideErrorPpm = 0, maxPeptideErrorDa = 0, maxTagErrorPpm = 0, maxTagErrorDa = 0;

                    while (!idFileSpectrumMatches.isEmpty()) {

                        // free memory if needed
                        if (MemoryConsumptionStatus.memoryUsed() > 0.9 && !peptideShaker.getCache().isEmpty()) {
                            peptideShaker.getCache().reduceMemoryConsumption(0.5, null);
                        }
                        // free memory if needed
                        if (MemoryConsumptionStatus.memoryUsed() > 0.9 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
                            ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
                        }
                        if (!MemoryConsumptionStatus.halfGbFree() && sequenceFactory.getNodesInCache() > 0) {
                            sequenceFactory.reduceNodeCacheSize(0.5);
                        }

                        SpectrumMatch match = idFileSpectrumMatches.pollLast();

                        for (int advocateId : match.getAdvocates()) {

                            if (advocateId == Advocate.xtandem.getIndex() && !xTandemPtmsCheck) {
                                verifyXTandemPtms();
                            }

                            nPSMs++;
                            nSecondary += match.getAllAssumptions().size() - 1;

                            String spectrumKey = match.getKey();
                            String fileName = Spectrum.getSpectrumFile(spectrumKey);
                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            if (spectrumKey.contains("4111")) {
                                int debug = 1;
                            }

                            for (SpectrumIdentificationAssumption assumption : match.getAllAssumptions()) {
                                if (assumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    if (!idFilter.validatePeptide(peptideAssumption.getPeptide(), sequenceMatchingPreferences)) {
                                        match.removeAssumption(assumption);
                                        peptideIssue++;
                                    }
                                }
                            }

                            if (!match.hasAssumption(advocateId)) {
                                psmsRejected++;
                            } else {

                                if (match.hasAssumption(advocateId)) {

                                    // Check whether there is a potential first hit which does not belong to the target and the decoy database
                                    ArrayList<Double> eValues = new ArrayList<Double>(match.getAllAssumptions(advocateId).keySet());
                                    Collections.sort(eValues);

                                    for (Double eValue : eValues) {

                                        ArrayList<SpectrumIdentificationAssumption> tempAssumptions
                                                = new ArrayList<SpectrumIdentificationAssumption>(match.getAllAssumptions(advocateId).get(eValue));

                                        for (SpectrumIdentificationAssumption assumption : tempAssumptions) {

                                            if (assumption instanceof PeptideAssumption) {

                                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                                Peptide peptide = peptideAssumption.getPeptide();
                                                String peptideSequence = peptide.getSequence();

                                                // map the algorithm specific modifications on utilities modifications
                                                // If there are not enough sites to put them all on the sequence, add an unknown modifcation
                                                // Note: this needs to be done for tag based assumptions as well since the protein mapping can return erroneous modifications for some pattern based PTMs
                                                ModificationProfile modificationProfile = searchParameters.getModificationProfile();

                                                boolean fixedPtmIssue = false;
                                                try {
                                                    ptmFactory.checkFixedModifications(modificationProfile, peptide, sequenceMatchingPreferences);
                                                } catch (IllegalArgumentException e) {
                                                    if (idFilter.removeUnknownPTMs()) {
                                                        // Exclude peptides with aberrant PTM mapping
                                                        System.out.println(e.getMessage());
                                                        match.removeAssumption(assumption);
                                                        ptmIssue++;
                                                        fixedPtmIssue = true;
                                                    } else {
                                                        throw e;
                                                    }
                                                }

                                                if (!fixedPtmIssue) {

                                                    HashMap<Integer, ArrayList<String>> expectedNames = new HashMap<Integer, ArrayList<String>>();
                                                    HashMap<ModificationMatch, ArrayList<String>> modNames = new HashMap<ModificationMatch, ArrayList<String>>();

                                                    for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                        HashMap<Integer, ArrayList<String>> tempNames = new HashMap<Integer, ArrayList<String>>();
                                                        if (modMatch.isVariable()) {
                                                            String sePTM = modMatch.getTheoreticPtm();
                                                            if (fileReader instanceof OMSSAIdfileReader) {
                                                                Integer omssaIndex = null;
                                                                try {
                                                                    omssaIndex = new Integer(sePTM);
                                                                } catch (Exception e) {
                                                                    waitingHandler.appendReport("Impossible to parse OMSSA modification " + sePTM + ".", true, true);
                                                                }
                                                                if (omssaIndex != null) {
                                                                    String omssaName = modificationProfile.getModification(omssaIndex);
                                                                    if (omssaName == null) {
                                                                        if (!ignoredOMSSAModifications.contains(omssaIndex)) {
                                                                            waitingHandler.appendReport("Impossible to find OMSSA modification of index "
                                                                                    + omssaIndex + ". The corresponding peptides will be ignored.", true, true);
                                                                            ignoredOMSSAModifications.add(omssaIndex);
                                                                        }
                                                                        omssaName = PTMFactory.unknownPTM.getName();
                                                                    }
                                                                    tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, omssaName, ptmMassTolerance, sequenceMatchingPreferences);
                                                                }
                                                            } else if (fileReader instanceof MascotIdfileReader
                                                                    || fileReader instanceof XTandemIdfileReader
                                                                    || fileReader instanceof MsAmandaIdfileReader
                                                                    || fileReader instanceof MzIdentMLIdfileReader
                                                                    || fileReader instanceof PepxmlIdfileReader) {
                                                                String[] parsedName = sePTM.split("@");
                                                                double seMass = 0;
                                                                try {
                                                                    seMass = new Double(parsedName[0]);
                                                                } catch (Exception e) {
                                                                    throw new IllegalArgumentException("Impossible to parse \'" + sePTM + "\' as a tagged modification.\n"
                                                                            + "Error encountered in peptide " + peptideSequence + " spectrum " + spectrumTitle + " in spectrum file " + fileName + ".\n"
                                                                            + "Identification file: " + idFile.getName());
                                                                }
                                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, seMass, ptmMassTolerance, sequenceMatchingPreferences);
                                                            } else if (fileReader instanceof DirecTagIdfileReader) {
                                                                PTM ptm = ptmFactory.getPTM(sePTM);
                                                                if (ptm == PTMFactory.unknownPTM) {
                                                                    throw new IllegalArgumentException("PTM not recognized spectrum " + spectrumTitle + " of file " + fileName + ".");
                                                                }
                                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, ptm.getMass(), ptmMassTolerance, sequenceMatchingPreferences);
                                                            } else {
                                                                throw new IllegalArgumentException("PTM mapping not implemented for the parsing of " + idFile.getName() + ".");
                                                            }

                                                            ArrayList<String> allNames = new ArrayList<String>();
                                                            for (ArrayList<String> namesAtAA : tempNames.values()) {
                                                                for (String name : namesAtAA) {
                                                                    if (!allNames.contains(name)) {
                                                                        allNames.add(name);
                                                                    }
                                                                }
                                                            }
                                                            modNames.put(modMatch, allNames);
                                                            for (int pos : tempNames.keySet()) {
                                                                ArrayList<String> namesAtPosition = expectedNames.get(pos);
                                                                if (namesAtPosition == null) {
                                                                    namesAtPosition = new ArrayList<String>(2);
                                                                    expectedNames.put(pos, namesAtPosition);
                                                                }
                                                                for (String ptmName : tempNames.get(pos)) {
                                                                    if (!namesAtPosition.contains(ptmName)) {
                                                                        namesAtPosition.add(ptmName);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // If a terminal modification cannot be elsewhere lock the terminus
                                                    ModificationMatch nTermModification = null;
                                                    for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                        if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                            double refMass = getRefMass(modMatch.getTheoreticPtm(), modificationProfile);
                                                            int modSite = modMatch.getModificationSite();
                                                            if (modSite == 1) {
                                                                ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                                if (expectedNamesAtSite != null) {
                                                                    ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                                                                    for (String ptmName : expectedNamesAtSite) {
                                                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                                                        if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracy()) {
                                                                            filteredNamesAtSite.add(ptmName);
                                                                        }
                                                                    }
                                                                    for (String modName : filteredNamesAtSite) {
                                                                        PTM ptm = ptmFactory.getPTM(modName);
                                                                        if (ptm.isNTerm()) {
                                                                            boolean otherPossibleMod = false;
                                                                            for (String tempName : modificationProfile.getAllNotFixedModifications()) {
                                                                                if (!tempName.equals(modName)) {
                                                                                    PTM tempPTM = ptmFactory.getPTM(tempName);
                                                                                    if (tempPTM.getMass() == ptm.getMass() && !tempPTM.isNTerm()) {
                                                                                        otherPossibleMod = true;
                                                                                        break;
                                                                                    }
                                                                                }
                                                                            }
                                                                            if (!otherPossibleMod) {
                                                                                nTermModification = modMatch;
                                                                                modMatch.setTheoreticPtm(modName);
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    if (nTermModification != null) {
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    ModificationMatch cTermModification = null;
                                                    for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                        if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName()) && modMatch != nTermModification) {
                                                            double refMass = getRefMass(modMatch.getTheoreticPtm(), modificationProfile);
                                                            int modSite = modMatch.getModificationSite();
                                                            if (modSite == peptideSequence.length()) {
                                                                ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                                if (expectedNamesAtSite != null) {
                                                                    ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                                                                    for (String ptmName : expectedNamesAtSite) {
                                                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                                                        if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracy()) {
                                                                            filteredNamesAtSite.add(ptmName);
                                                                        }
                                                                    }
                                                                    for (String modName : filteredNamesAtSite) {
                                                                        PTM ptm = ptmFactory.getPTM(modName);
                                                                        if (ptm.isCTerm()) {
                                                                            boolean otherPossibleMod = false;
                                                                            for (String tempName : modificationProfile.getAllNotFixedModifications()) {
                                                                                if (!tempName.equals(modName)) {
                                                                                    PTM tempPTM = ptmFactory.getPTM(tempName);
                                                                                    if (tempPTM.getMass() == ptm.getMass() && !tempPTM.isCTerm()) {
                                                                                        otherPossibleMod = true;
                                                                                        break;
                                                                                    }
                                                                                }
                                                                            }
                                                                            if (!otherPossibleMod) {
                                                                                cTermModification = modMatch;
                                                                                modMatch.setTheoreticPtm(modName);
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    if (cTermModification != null) {
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Map the modifications according to search engine localization
                                                    HashMap<Integer, ArrayList<String>> siteToPtmMap = new HashMap<Integer, ArrayList<String>>(); // Site to ptm name including termini
                                                    HashMap<Integer, ModificationMatch> siteToMatchMap = new HashMap<Integer, ModificationMatch>(); // Site to Modification match excluding termini
                                                    HashMap<ModificationMatch, Integer> matchToSiteMap = new HashMap<ModificationMatch, Integer>(); // Modification match to site excluding termini
                                                    boolean allMapped = true;

                                                    for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                        boolean mapped = false;
                                                        if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                            double refMass = getRefMass(modMatch.getTheoreticPtm(), modificationProfile);
                                                            int modSite = modMatch.getModificationSite();
                                                            boolean terminal = false;
                                                            ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                            if (expectedNamesAtSite != null) {
                                                                ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                                                                ArrayList<String> modificationAtSite = siteToPtmMap.get(modSite);
                                                                for (String ptmName : expectedNamesAtSite) {
                                                                    PTM ptm = ptmFactory.getPTM(ptmName);
                                                                    if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracy()
                                                                            && (modificationAtSite == null || !modificationAtSite.contains(ptmName))) {
                                                                        filteredNamesAtSite.add(ptmName);
                                                                    }
                                                                }
                                                                if (filteredNamesAtSite.size() == 1) {
                                                                    String ptmName = filteredNamesAtSite.get(0);
                                                                    PTM ptm = ptmFactory.getPTM(ptmName);
                                                                    if (ptm.isNTerm() && nTermModification == null) {
                                                                        nTermModification = modMatch;
                                                                        mapped = true;
                                                                    } else if (ptm.isCTerm() && cTermModification == null) {
                                                                        cTermModification = modMatch;
                                                                        mapped = true;
                                                                    } else if (!ptm.isNTerm() && !ptm.isCTerm()) {
                                                                        matchToSiteMap.put(modMatch, modSite);
                                                                        siteToMatchMap.put(modSite, modMatch);
                                                                        mapped = true;
                                                                    }
                                                                    if (mapped) {
                                                                        modMatch.setTheoreticPtm(ptmName);
                                                                        if (modificationAtSite == null) {
                                                                            modificationAtSite = new ArrayList<String>(2);
                                                                            siteToPtmMap.put(modSite, modificationAtSite);
                                                                        }
                                                                        modificationAtSite.add(ptmName);
                                                                    }
                                                                }
                                                                if (!mapped) {
                                                                    if (filteredNamesAtSite.isEmpty()) {
                                                                        filteredNamesAtSite = expectedNamesAtSite;
                                                                    }
                                                                    if (modSite == 1) {
                                                                        Double minDiff = null;
                                                                        String bestPtmName = null;
                                                                        for (String modName : filteredNamesAtSite) {
                                                                            PTM ptm = ptmFactory.getPTM(modName);
                                                                            if (ptm.isNTerm() && nTermModification == null) {
                                                                                double massError = Math.abs(refMass - ptm.getMass());
                                                                                if (massError <= searchParameters.getFragmentIonAccuracy()
                                                                                        && (minDiff == null || massError < minDiff)) {
                                                                                    bestPtmName = modName;
                                                                                    minDiff = massError;
                                                                                }
                                                                            }
                                                                        }
                                                                        if (bestPtmName != null) {
                                                                            nTermModification = modMatch;
                                                                            modMatch.setTheoreticPtm(bestPtmName);
                                                                            terminal = true;
                                                                            if (modificationAtSite == null) {
                                                                                modificationAtSite = new ArrayList<String>(2);
                                                                                siteToPtmMap.put(modSite, modificationAtSite);
                                                                            }
                                                                            modificationAtSite.add(bestPtmName);
                                                                            mapped = true;
                                                                        }
                                                                    } else if (modSite == peptideSequence.length()) {
                                                                        Double minDiff = null;
                                                                        String bestPtmName = null;
                                                                        for (String modName : filteredNamesAtSite) {
                                                                            PTM ptm = ptmFactory.getPTM(modName);
                                                                            if (ptm.isCTerm() && cTermModification == null) {
                                                                                double massError = Math.abs(refMass - ptm.getMass());
                                                                                if (massError <= searchParameters.getFragmentIonAccuracy()
                                                                                        && (minDiff == null || massError < minDiff)) {
                                                                                    bestPtmName = modName;
                                                                                    minDiff = massError;
                                                                                }
                                                                            }
                                                                        }
                                                                        if (bestPtmName != null) {
                                                                            cTermModification = modMatch;
                                                                            modMatch.setTheoreticPtm(bestPtmName);
                                                                            terminal = true;
                                                                            if (modificationAtSite == null) {
                                                                                modificationAtSite = new ArrayList<String>(2);
                                                                                siteToPtmMap.put(modSite, modificationAtSite);
                                                                            }
                                                                            modificationAtSite.add(bestPtmName);
                                                                            mapped = true;
                                                                        }
                                                                    }
                                                                    if (!terminal) {
                                                                        Double minDiff = null;
                                                                        String bestPtmName = null;
                                                                        for (String modName : filteredNamesAtSite) {
                                                                            PTM ptm = ptmFactory.getPTM(modName);
                                                                            if (!ptm.isCTerm() && !ptm.isNTerm() && modNames.get(modMatch).contains(modName) && !siteToMatchMap.containsKey(modSite)) {
                                                                                double massError = Math.abs(refMass - ptm.getMass());
                                                                                if (massError <= searchParameters.getFragmentIonAccuracy()
                                                                                        && (minDiff == null || massError < minDiff)) {
                                                                                    bestPtmName = modName;
                                                                                    minDiff = massError;
                                                                                }
                                                                            }
                                                                        }
                                                                        if (bestPtmName != null) {
                                                                            modMatch.setTheoreticPtm(bestPtmName);
                                                                            if (modificationAtSite == null) {
                                                                                modificationAtSite = new ArrayList<String>(2);
                                                                                siteToPtmMap.put(modSite, modificationAtSite);
                                                                            }
                                                                            modificationAtSite.add(bestPtmName);
                                                                            matchToSiteMap.put(modMatch, modSite);
                                                                            siteToMatchMap.put(modSite, modMatch);
                                                                            mapped = true;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (!mapped) {
                                                            allMapped = false;
                                                        }
                                                    }

                                                    if (!allMapped) {

                                                        // Try to correct incompatible localizations
                                                        HashMap<Integer, ArrayList<Integer>> remap = new HashMap<Integer, ArrayList<Integer>>();

                                                        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                            if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !matchToSiteMap.containsKey(modMatch) && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                                int modSite = modMatch.getModificationSite();
                                                                for (int candidateSite : expectedNames.keySet()) {
                                                                    if (!siteToMatchMap.containsKey(candidateSite)) {
                                                                        for (String modName : expectedNames.get(candidateSite)) {
                                                                            if (modNames.get(modMatch).contains(modName)) {
                                                                                PTM ptm = ptmFactory.getPTM(modName);
                                                                                if ((!ptm.isCTerm() || cTermModification == null)
                                                                                        && (!ptm.isNTerm() || nTermModification == null)) {
                                                                                    ArrayList<Integer> ptmSites = remap.get(modSite);
                                                                                    if (ptmSites == null) {
                                                                                        ptmSites = new ArrayList<Integer>(4);
                                                                                        remap.put(modSite, ptmSites);
                                                                                    }
                                                                                    if (!ptmSites.contains(candidateSite)) {
                                                                                        ptmSites.add(candidateSite);
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        HashMap<Integer, Integer> correctedIndexes = PtmSiteMapping.alignAll(remap);

                                                        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                            if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !matchToSiteMap.containsKey(modMatch) && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                                Integer modSite = correctedIndexes.get(modMatch.getModificationSite());
                                                                if (modSite != null) {
                                                                    if (expectedNames.containsKey(modSite)) {
                                                                        for (String modName : expectedNames.get(modSite)) {
                                                                            if (modNames.get(modMatch).contains(modName)) {
                                                                                ArrayList<String> taken = siteToPtmMap.get(modSite);
                                                                                if (taken == null || !taken.contains(modName)) {
                                                                                    matchToSiteMap.put(modMatch, modSite);
                                                                                    modMatch.setTheoreticPtm(modName);
                                                                                    modMatch.setModificationSite(modSite);
                                                                                    if (taken == null) {
                                                                                        taken = new ArrayList<String>(2);
                                                                                        siteToPtmMap.put(modSite, taken);
                                                                                    }
                                                                                    taken.add(modName);
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    matchToSiteMap.put(modMatch, modSite);
                                                                    modMatch.setTheoreticPtm(PTMFactory.unknownPTM.getName());
                                                                }
                                                                if (!matchToSiteMap.containsKey(modMatch)) {
                                                                    modMatch.setTheoreticPtm(PTMFactory.unknownPTM.getName());
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (idFilter.validateModifications(peptide, sequenceMatchingPreferences, searchParameters.getModificationProfile())) {
                                                        // Estimate the theoretic mass with the new modifications
                                                        peptide.estimateTheoreticMass();
                                                        if (!idFilter.validatePrecursor(peptideAssumption, spectrumKey, spectrumFactory)) {
                                                            match.removeAssumption(assumption);
                                                            precursorIssue++;
                                                        } else if (!idFilter.validateProteins(peptideAssumption.getPeptide(), sequenceMatchingPreferences)) {
                                                            // Check whether there is a potential first hit which does not belong to both the target and the decoy database
                                                            match.removeAssumption(assumption);
                                                            proteinIssue++;
                                                        }
                                                    } else {
                                                        match.removeAssumption(assumption);
                                                        ptmIssue++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (match.hasAssumption(advocateId)) {
                                    // try to find the best peptide hit
                                    PeptideAssumption firstPeptideHit = null;
                                    ArrayList<Double> eValues = new ArrayList<Double>(match.getAllAssumptions(advocateId).keySet());
                                    Collections.sort(eValues);

                                    for (Double eValue : eValues) {
                                        for (SpectrumIdentificationAssumption assumption : match.getAllAssumptions(advocateId).get(eValue)) {
                                            if (assumption instanceof PeptideAssumption) {
                                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                                firstPeptideHit = peptideAssumption;
                                                match.setFirstHit(advocateId, assumption);
                                                double precursorMz = spectrumFactory.getPrecursor(spectrumKey).getMz();
                                                double error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, true));

                                                if (error > maxPeptideErrorPpm) {
                                                    maxPeptideErrorPpm = error;
                                                }

                                                error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, false));

                                                if (error > maxPeptideErrorDa) {
                                                    maxPeptideErrorDa = error;
                                                }

                                                int currentCharge = assumption.getIdentificationCharge().value;

                                                if (!charges.contains(currentCharge)) {
                                                    charges.add(currentCharge);
                                                }
                                                ArrayList<String> accessions = peptideAssumption.getPeptide().getParentProteins(sequenceMatchingPreferences);
                                                for (String protein : accessions) {
                                                    Integer count = proteinCount.get(protein);
                                                    if (count != null) {
                                                        proteinCount.put(protein, count + 1);
                                                    } else {
                                                        int index = singleProteinList.indexOf(protein);
                                                        if (index != -1) {
                                                            singleProteinList.remove(index);
                                                            proteinCount.put(protein, 2);
                                                        } else {
                                                            singleProteinList.add(protein);
                                                        }
                                                    }
                                                }
                                                if (!processingPreferences.isScoringNeeded(advocateId)) {
                                                    inputMap.addEntry(advocateId, fileName, firstPeptideHit.getScore(), firstPeptideHit.getPeptide().isDecoy(sequenceMatchingPreferences));
                                                }
                                                identification.addSpectrumMatch(match, false); //@TODO: adapt to the different scores
                                                nRetained++;
                                                break;
                                            }
                                        }
                                        if (firstPeptideHit != null) {
                                            break;
                                        }
                                    }
                                    if (firstPeptideHit == null) {
                                        // Try to find the best tag hit
                                        TagAssumption firstTagHit = null;
                                        for (Double eValue : eValues) {
                                            for (SpectrumIdentificationAssumption assumption : match.getAllAssumptions(advocateId).get(eValue)) {
                                                if (assumption instanceof TagAssumption) {
                                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                                    firstTagHit = tagAssumption;
                                                    match.setFirstHit(advocateId, assumption);
                                                    double precursorMz = spectrumFactory.getPrecursor(spectrumKey).getMz();
                                                    double error = Math.abs(tagAssumption.getDeltaMass(precursorMz, true));

                                                    if (error > maxTagErrorPpm) {
                                                        maxTagErrorPpm = error;
                                                    }

                                                    error = Math.abs(assumption.getDeltaMass(precursorMz, false));

                                                    if (error > maxTagErrorDa) {
                                                        maxTagErrorDa = error;
                                                    }

                                                    int currentCharge = assumption.getIdentificationCharge().value;

                                                    if (!charges.contains(currentCharge)) {
                                                        charges.add(currentCharge);
                                                    }
                                                    identification.addSpectrumMatch(match, false); //@TODO: adapt to the different scores
                                                    nRetained++;
                                                    break;
                                                }
                                            }
                                            if (firstTagHit != null) {
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    psmsRejected++;
                                }
                            }

                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }

                            waitingHandler.setSecondaryProgressCounter(++progress);
                        }
                    }

                    metrics.addFoundCharges(new ArrayList<Integer>(charges));
                    if (maxPeptideErrorDa > metrics.getMaxPeptidePrecursorErrorDa()) {
                        metrics.setMaxPeptidePrecursorErrorDa(maxPeptideErrorDa);
                    }
                    if (maxPeptideErrorPpm > metrics.getMaxPeptidePrecursorErrorPpm()) {
                        metrics.setMaxPeptidePrecursorErrorPpm(maxPeptideErrorPpm);
                    }
                    if (maxTagErrorDa > metrics.getMaxTagPrecursorErrorDa()) {
                        metrics.setMaxTagPrecursorErrorDa(maxTagErrorDa);
                    }
                    if (maxTagErrorPpm > metrics.getMaxTagPrecursorErrorPpm()) {
                        metrics.setMaxTagPrecursorErrorPpm(maxTagErrorPpm);
                    }

                    // Free at least 0.5GB for the next parser if not anymore available
                    if (!MemoryConsumptionStatus.halfGbFree() && !peptideShaker.getCache().isEmpty()) {
                        waitingHandler.appendReport("PeptideShaker is encountering memory issues! "
                                + "See http://peptide-shaker.googlecode.com for help.", true, true);
                        waitingHandler.appendReport("Reducing Memory Consumption.", true, true);
                        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                        double share = ((double) 1073741824) / (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                        share = Math.min(share, 1);
                        peptideShaker.getCache().reduceMemoryConsumption(share, waitingHandler);
                        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                    }
                    if (!MemoryConsumptionStatus.halfGbFree() && sequenceFactory.getNodesInCache() > 0) {
                        sequenceFactory.reduceNodeCacheSize(0.5);
                    }
                    projectDetails.addIdentificationFiles(idFile);

                    double sharePsmsRejected = 100.0 * psmsRejected / numberOfMatches;

                    if (psmsRejected > 0) {
                        waitingHandler.appendReport(psmsRejected + " PSMs (" + Util.roundDouble(sharePsmsRejected, 1) + "%) excluded by the import filters:", true, true);

                        String padding = "    ";

                        int totalAssumptionsRejected = proteinIssue + peptideIssue + precursorIssue + ptmIssue;

                        double share = 100 * ((double) proteinIssue) / totalAssumptionsRejected;
                        if (share >= 1) {
                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide mapping to both target and decoy.", true, true);
                        }
                        share = 100 * ((double) peptideIssue) / totalAssumptionsRejected;
                        if (share >= 1) {
                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ".", true, true);
                        }
                        share = 100 * ((double) precursorIssue) / totalAssumptionsRejected;
                        if (share >= 1) {
                            String unit;
                            if (searchParameters.isPrecursorAccuracyTypePpm()) {
                                unit = "ppm";
                            } else {
                                unit = "Da";
                            }
                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide mass deviation bigger than " + idFilter.getMaxMzDeviation() + " " + unit + ".", true, true);
                        }
                        share = 100 * ((double) ptmIssue) / totalAssumptionsRejected;
                        if (share >= 1) {
                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1) + "% unrecognized modifications.", true, true);
                        }
                    }
                    // inform the user in case more than 75% of the hits were rejected by the filters
                    if (sharePsmsRejected > 75) {
                        String report = "Warning: More than 75% of the PSMs were rejected by the loading filters when importing the matches.";
                        double meanRejected = sharePsmsRejected / 4;
                        if (proteinIssue > meanRejected) {
                            report += " Apparently your database contains a high share of shared peptides between the target and decoy sequences. Please verify your database";
                            if (software.keySet().contains(Advocate.mascot.getName())) {
                                report += " and make sure that you use Mascot with the 'decoy' option disabled.";
                            }
                            report += ".";
                        }
                        if (peptideIssue > meanRejected) {
                            report += " Please verify that your peptide selection criteria are not too restrictive.";
                        }
                        if (precursorIssue > meanRejected) {
                            report += " Please verify that your precursor selection criteria are not too restrictive.";
                        }
                        if (ptmIssue > meanRejected) {
                            report += " Apparently your data contains modifications which are not recognized by PeptideShaker. Please verify the search parameters provided when creating the project.";
                            if (software.keySet().contains(Advocate.mascot.getName())) {
                                report += " When using Mascot alone, you need to specify the search parameters manually when creating the project. We recommend the complementary use of SearchGUI when possible.";
                            }
                        }
                        waitingHandler.appendReport(report, true, true);
                    }
                }
            }

            waitingHandler.increasePrimaryProgressCounter();
        }

        /**
         * Verifies that the modifications targeted by the quick acetyl and
         * quick pyrolidone are included in the search parameters.
         */
        private void verifyXTandemPtms() {
            ModificationProfile modificationProfile = searchParameters.getModificationProfile();
            IdentificationAlgorithmParameter algorithmParameter = searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex());
            if (algorithmParameter != null) {
                XtandemParameters xtandemParameters = (XtandemParameters) algorithmParameter;
                if (xtandemParameters.isProteinQuickAcetyl() && !modificationProfile.contains("acetylation of protein n-term")) {
                    PTM ptm = PTMFactory.getInstance().getPTM("acetylation of protein n-term");
                    modificationProfile.addVariableModification(ptm);
                }
                String[] pyroMods = {"pyro-cmc", "pyro-glu from n-term e", "pyro-glu from n-term q"};
                if (xtandemParameters.isQuickPyrolidone()) {
                    for (String ptmName : pyroMods) {
                        if (!modificationProfile.getVariableModifications().contains(ptmName)) {
                            PTM ptm = PTMFactory.getInstance().getPTM(ptmName);
                            modificationProfile.addVariableModification(ptm);
                        }
                    }
                }
            }
            xTandemPtmsCheck = true;
        }

        /**
         * Checks whether the spectrum file needed for the given spectrum match
         * is loaded and if the spectrum is present. Try to load it from the
         * factory otherwise.
         *
         * @param idFile the identification file
         * @param spectrumMatch the spectrum match
         * @param numberOfMatches the number of matches expected for this
         * identification file
         *
         * @return indicates whether the spectrum is imported, false if the file
         * was not found
         */
        private boolean importSpectrum(File idFile, SpectrumMatch spectrumMatch, int numberOfMatches) {

            String spectrumKey = spectrumMatch.getKey();
            String fileName = Spectrum.getSpectrumFile(spectrumKey);
            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

            // remap wrong spectrum file names
            if (spectrumFactory.getSpectrumFileFromIdName(fileName) != null) {
                fileName = spectrumFactory.getSpectrumFileFromIdName(fileName).getName();
                spectrumMatch.setKey(Spectrum.getSpectrumKey(fileName, spectrumTitle));
                spectrumKey = spectrumMatch.getKey();
            }

            // import the mgf file needed if not done already
            if (!mgfUsed.contains(fileName)) {
                File spectrumFile = spectrumFiles.get(fileName);
                if (spectrumFile != null && spectrumFile.exists()) {
                    importSpectra(fileName);
                    waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                    waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                    mgfUsed.add(fileName);
                    projectDetails.addSpectrumFile(spectrumFile);
                    nSpectra += spectrumFactory.getNSpectra(fileName);
                } else {
                    missingMgfFiles.put(idFile, fileName);
                    waitingHandler.appendReport(fileName + " not found.", true, true);
                    return false;
                }
            }

            // remap missing spectrum titles
            if (spectrumFactory.fileLoaded(fileName) && !spectrumFactory.spectrumLoaded(spectrumKey)) {
                String oldTitle = Spectrum.getSpectrumTitle(spectrumKey);
                Integer spectrumNumber = spectrumMatch.getSpectrumNumber();
                if (spectrumNumber == null) {
                    try {
                        spectrumNumber = new Integer(oldTitle);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (spectrumNumber == null) {
                    String errorMessage = "Spectrum \'" + oldTitle + "\' not found in file " + fileName + ".";
                    waitingHandler.appendReport(errorMessage, true, true);
                    waitingHandler.setRunCanceled();
                    throw new IllegalArgumentException(errorMessage);
                }
                spectrumTitle = spectrumFactory.getSpectrumTitle(fileName, spectrumNumber);
                spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                spectrumMatch.setKey(spectrumKey);
                if (!spectrumFactory.spectrumLoaded(spectrumKey)) {
                    spectrumTitle = spectrumNumber + "";
                    spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                    spectrumMatch.setKey(spectrumKey);
                    if (spectrumFactory.fileLoaded(fileName) && !spectrumFactory.spectrumLoaded(spectrumKey)) {
                        String errorMessage = "Spectrum \'" + oldTitle + "\' number " + spectrumTitle + " not found in file " + fileName + ".";
                        waitingHandler.appendReport(errorMessage, true, true);
                        waitingHandler.setRunCanceled();
                        throw new IllegalArgumentException(errorMessage);
                    }
                }
            }
            return true;
        }

        /**
         * Verify that the spectra are imported and imports spectra from the
         * desired spectrum file if necessary.
         *
         * @param targetFileName the spectrum file
         */
        public void importSpectra(String targetFileName) {

            File spectrumFile = spectrumFiles.get(targetFileName);

            try {
                waitingHandler.appendReport("Importing " + targetFileName, true, true);
                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                waitingHandler.resetSecondaryProgressCounter();
                spectrumFactory.addSpectra(spectrumFile, waitingHandler);

                // @TODO: check for duplicate spectrum titles and show the warning in the lower right corner of the main frame
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.increasePrimaryProgressCounter();
                waitingHandler.appendReport(targetFileName + " imported.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("Spectrum files import failed when trying to import " + targetFileName + ".", true, true);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("FileImporter.class").getPath(), "PeptideShaker");
    }

    /**
     * Returns the mass indicated by the identification algorithm for the given
     * ptm. 0 if not found.
     *
     * @param sePtmName the name according to the identification algorithm
     * @param modificationProfile the modification profile of the identification
     *
     * @return the mass of the ptm
     */
    private double getRefMass(String sePtmName, ModificationProfile modificationProfile) {
        Double refMass = 0.0;
        // Try utilities modifications
        PTM refPtm = ptmFactory.getPTM(sePtmName);
        if (refPtm == PTMFactory.unknownPTM) {
            // Try mass@AA
            int atIndex = sePtmName.indexOf("@");
            if (atIndex > 0) {
                refMass = new Double(sePtmName.substring(0, atIndex));
            } else {
                // Try OMSSA indexes
                try {
                    int omssaIndex = new Integer(sePtmName);
                    String omssaName = modificationProfile.getModification(omssaIndex);
                    if (omssaName != null) {
                        refPtm = ptmFactory.getPTM(omssaName);
                        if (refPtm != PTMFactory.unknownPTM) {
                            refMass = refPtm.getMass();
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        } else {
            refMass = refPtm.getMass();
        }
        return refMass;
    }

    /**
     * Returns the temp folder name to use when unzipping a zip file.
     *
     * @param fileName the name of the zip file
     *
     * @return the folder name associated to the zip file
     */
    public static String getTempFolderName(String fileName) {
        return Util.removeExtension(fileName) + "_" + FileImporter.tempFolderName;
    }
}
