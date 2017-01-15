package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import eu.isas.peptideshaker.gui.MgfFilesNotFoundDialog;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.WaitingDialogExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.genes.GeneFactory;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.protein_inference.PeptideMapperType;
import com.compomics.util.gui.JOptionEditorPane;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PeptideVariantsPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.protein_inference.PeptideMapper;
import eu.isas.peptideshaker.protein_inference.TagMapper;
import eu.isas.peptideshaker.scoring.maps.InputMap;
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
     * A dialog to display feedback to the user.
     */
    private WaitingHandler waitingHandler;
    /**
     * An exception handler to handle exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * If a Mascot dat file is bigger than this size, an indexed parsing will be
     * used.
     */
    public static final double MASCOT_MAX_SIZE = 400;
    /**
     * Metrics of the dataset picked-up while loading the data.
     */
    private Metrics metrics;
    /**
     * The mass tolerance to be used to match PTMs from search engines and
     * expected PTMs. 0.01 by default, as far as I can remember it is the mass
     * resolution in X!Tandem result files.
     */
    public static final double PTM_MASS_TOLERANCE = 0.01;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;

    /**
     * Constructor for the importer.
     *
     * @param identificationShaker the identification shaker which will load the
     * data into the maps and do the preliminary calculations
     * @param waitingHandler The handler displaying feedback to the user
     * @param proteomicAnalysis The current proteomic analysis
     * @param identificationParameters the identification parameters
     * @param metrics metrics of the dataset to be saved for the GUI
     */
    public FileImporter(PeptideShaker identificationShaker, WaitingHandler waitingHandler, ProteomicAnalysis proteomicAnalysis,
            IdentificationParameters identificationParameters, Metrics metrics) {
        this.peptideShaker = identificationShaker;
        this.waitingHandler = waitingHandler;
        this.proteomicAnalysis = proteomicAnalysis;
        this.identificationParameters = identificationParameters;
        this.metrics = metrics;
        if (waitingHandler instanceof WaitingDialog) {
            exceptionHandler = new WaitingDialogExceptionHandler((WaitingDialog) waitingHandler, "https://github.com/compomics/peptide-shaker/issues");
        } else {
            exceptionHandler = new CommandLineExceptionHandler();
        }
    }

    /**
     * Imports the identification from files.
     *
     * @param idFiles the identification files to import the IDs from
     * @param spectrumFiles the files where the corresponding spectra can be
     * imported
     * @param processingPreferences the processing preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     * @param backgroundThread boolean indicating whether the import should be
     * done in a background thread (GUI mode) or in the current thread (command
     * line mode).
     */
    public void importFiles(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, ProcessingPreferences processingPreferences,
            SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, boolean backgroundThread) {

        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles, spectrumFiles, identificationParameters, processingPreferences, spectrumCountingPreferences, projectDetails);

        if (backgroundThread) {
            idProcessor.execute();
        } else {
            idProcessor.importFiles();
        }
    }

    /**
     * Imports sequences from a FASTA file.
     *
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param peptideVariantsPreferences the peptide variants preferences set by
     * the user
     * @param waitingHandler the handler displaying feedback to the user and
     * allowing canceling the import
     * @param exceptionHandler handler for exceptions
     * @param fastaFile FASTA file to process
     * @param ptmSettings the PTM settings
     */
    public void importSequences(SequenceMatchingPreferences sequenceMatchingPreferences, PeptideVariantsPreferences peptideVariantsPreferences, WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler, File fastaFile, PtmSettings ptmSettings) {

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
            int nSequences = sequenceFactory.getNSequences();
            int cacheSizeInMb = nSequences * 112 / 1048576; // 112 is the size taken by one protein
            if (!sequenceFactory.isDefaultReversed() || cacheSizeInMb < memoryPreference / 4) {
                nSequences = sequenceFactory.getNSequences();
                sequenceFactory.setDecoyInMemory(true);
            } else {
                nSequences = sequenceFactory.getNTargetSequences();
                sequenceFactory.setDecoyInMemory(false);
            }
            long availableCachSize = 1048576l * memoryPreference / 112; // 112 is the size taken by one protein
            availableCachSize *= 0.75;
            if (availableCachSize > nSequences) {
                availableCachSize = nSequences;
            } else {
                waitingHandler.appendReport("Warning: PeptideShaker cannot load your FASTA file into memory. This will slow down the processing. "
                        + "Note that using large large databases also reduces the search engine efficiency. "
                        + "Try to either (i) use a smaller database, (ii) increase the memory provided to PeptideShaker, or (iii) improve the reading speed by using an SSD disc. "
                        + "(See also http://compomics.github.io/compomics-utilities/wiki/proteininference.html.)", true, true);

            }
            int cacheSize = (int) availableCachSize;
            sequenceFactory.setnCache(cacheSize);

            try {
                sequenceFactory.getDefaultPeptideMapper(sequenceMatchingPreferences, ptmSettings, peptideVariantsPreferences, waitingHandler, exceptionHandler);
            } catch (SQLException e) {
                waitingHandler.appendReport("Database " + sequenceFactory.getCurrentFastaFile().getName()
                        + " could not be accessed, make sure that the file is not used by another "
                        + "program and that you have not run out of diskspace.", true, true);
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
            System.err.println("An error occurred while indexing " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occurred while indexing " + fastaFile + ": " + e.getMessage(), true, true);
        } catch (SQLException e) {
            System.err.println("An error occurred while indexing " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occurred while indexing " + fastaFile + ": " + e.getMessage(), true, true);
        } catch (InterruptedException e) {
            System.err.println("An error occurred while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occurred while loading " + fastaFile + ": " + e.getMessage(), true, true);
        } catch (IllegalArgumentException e) {
            System.err.println("An error occurred while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport(e.getLocalizedMessage() + " Please refer to http://compomics.github.io/projects/peptide-shaker.html#troubleshooting", true, true);
        } catch (ClassNotFoundException e) {
            System.err.println("An error occurred while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("Serialization issue while processing the FASTA file. Please delete the .fasta.cui file and retry. "
                    + "If the error occurs again please report bug using our issue tracker: https://github.com/compomics/peptide-shaker/issues.", true, true);
        } catch (NullPointerException e) {
            System.err.println("An error occurred while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occurred when importing the sequences. "
                    + "Please check the Search Parameters. See the log file for details. "
                    + "If the error persists please let us know using our issue tracker: https://github.com/compomics/peptide-shaker/issues.", true, true);
        }
    }

    /**
     * Imports the gene information for this project.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while establishing the connection
     */
    public void importGenes() throws IOException, InterruptedException {
        GeneFactory geneFactory = GeneFactory.getInstance();
        GenePreferences genePreferences = identificationParameters.getGenePreferences();
        GeneMaps geneMaps = geneFactory.getGeneMaps(genePreferences, waitingHandler);
        peptideShaker.setGeneMaps(geneMaps);
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
         * A list of spectrum files (can be empty, no spectrum will be
         * imported).
         */
        private HashMap<String, File> spectrumFiles;
        /**
         * The processing preferences.
         */
        private ProcessingPreferences processingPreferences;
        /**
         * The project details
         */
        private ProjectDetails projectDetails;
        /**
         * The spectrum counting preferences.
         */
        private SpectrumCountingPreferences spectrumCountingPreferences;
        /**
         * The number of retained first hits.
         */
        private long nRetained = 0;
        /**
         * The number of spectra.
         */
        private long nSpectra = 0;
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
         * Boolean indicating whether we can display GUI stuff.
         */
        private boolean hasGUI = false;
        /**
         * The database connection.
         */
        private Identification identification;
        /**
         * A peptide to protein mapper.
         */
        private PeptideMapper peptideMapper;
        /**
         * A tag to protein mapper.
         */
        private TagMapper tagMapper = null;
        /**
         * List of one hit wonders.
         */
        private HashSet<String> singleProteinList = new HashSet<String>();
        /**
         * Map of proteins found several times with the number of times they
         * appeared as first hit.
         */
        private HashMap<String, Integer> proteinCount = new HashMap<String, Integer>();
        /**
         * The number of first hits.
         */
        private long nPSMs = 0;
        /**
         * The number of secondary hits.
         */
        private long nSecondary = 0;
        /**
         * The identification parameters.
         */
        private IdentificationParameters identificationParameters;

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
        public IdProcessorFromFile(ArrayList<File> idFiles, ArrayList<File> spectrumFiles,
                IdentificationParameters identificationParameters, ProcessingPreferences processingPreferences,
                SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails) {

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
            this.identificationParameters = identificationParameters;
            this.processingPreferences = processingPreferences;
            this.spectrumCountingPreferences = spectrumCountingPreferences;
            this.projectDetails = projectDetails;

            for (File file : spectrumFiles) {
                this.spectrumFiles.put(file.getName(), file);
            }

            UtilitiesUserPreferences userPreferences = UtilitiesUserPreferences.loadUserPreferences();
            if (userPreferences.getMemoryPreference() > 2000) {
                peptideMapper = new PeptideMapper(identificationParameters, waitingHandler, exceptionHandler);
            }
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
                importSequences(identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPeptideVariantsPreferences(), waitingHandler, exceptionHandler,
                        identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase(),
                        identificationParameters.getSearchParameters().getPtmSettings());

                if (waitingHandler.isRunCanceled()) {
                    return 1;
                }

                GenePreferences genePreferences = identificationParameters.getGenePreferences();
                if (genePreferences.getUseGeneMapping()) {
                    waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                    waitingHandler.appendReport("Importing gene mappings.", true, true);
                    importGenes();
                } else {
                    peptideShaker.setGeneMaps(new GeneMaps());
                }

                if (waitingHandler.isRunCanceled()) {
                    return 1;
                }

                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                waitingHandler.appendReport("Establishing local database connection.", true, true);

                identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

                connectToIdDb(identification);

                waitingHandler.increasePrimaryProgressCounter();

                if (!waitingHandler.isRunCanceled()) {

                    waitingHandler.appendReport("Reading identification files.", true, true);

                    for (File idFile : idFiles) {
                        importPsms(idFile);

                        if (waitingHandler.isRunCanceled()) {
                            try {
                                identification.close();
                            } catch (Exception e) {
                                e.printStackTrace(); // Let derby crash
                            }
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
                    peptideShaker.processIdentifications(inputMap, proteinCount, waitingHandler, exceptionHandler, identificationParameters, processingPreferences, spectrumCountingPreferences, projectDetails);
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
                            + "Java Options). See also <a href=\"http://compomics.github.io/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>."),
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
                    waitingHandler.appendReport("An error occurred while loading the identification files.", true, true);
                    waitingHandler.appendReport("Please see the error log (Help Menu > Bug Report) for details.", true, true);
                } else if (FrameExceptionHandler.getExceptionType(e).equalsIgnoreCase("Protein not found")) {
                    waitingHandler.appendReport("An error occurred while loading the identification files:", true, true);
                    waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
                    waitingHandler.appendReport("Please see http://compomics.github.io/searchgui/wiki/databasehelp.html.", true, true);
                } else {
                    waitingHandler.appendReport("An error occurred while loading the identification files:", true, true);
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
         * @param identification the identifications
         *
         * @throws IOException thrown of IOException occurs exception thrown
         * whenever an error occurred while reading or writing a file
         * @throws SQLException thrown of SQLException occurs exception thrown
         * whenever an error occurred while interacting with the database
         * @throws java.lang.ClassNotFoundException exception thrown whenever an
         * error occurred while deserializing an object
         * @throws java.lang.InterruptedException exception thrown whenever a
         * threading error occurred while establishing the connection
         */
        private void connectToIdDb(Identification identification) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
            String dbFolder = PeptideShaker.getMatchesFolder().getAbsolutePath();
            identification.establishConnection(dbFolder, true, peptideShaker.getCache());
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

            waitingHandler.setSecondaryProgressCounterIndeterminate(false);

            LinkedList<SpectrumMatch> idFileSpectrumMatches = null;
            try {
                if (peptideMapper != null && !peptideMapper.isCanceled()) {
                    idFileSpectrumMatches = fileReader.getAllSpectrumMatches(waitingHandler, identificationParameters.getSearchParameters(), identificationParameters.getSequenceMatchingPreferences(), true);
                } else {
                    idFileSpectrumMatches = fileReader.getAllSpectrumMatches(waitingHandler, identificationParameters.getSearchParameters(), null, true);
                }
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while loading spectrum matches from \'"
                        + Util.getFileName(idFile)
                        + "\'. This file will be ignored. Error: " + e.toString()
                        + " See resources/PeptideShaker.log for details.", true, true);
                e.printStackTrace();
            }

            // set the search engine name and version for this file
            HashMap<String, ArrayList<String>> software = fileReader.getSoftwareVersions();
            projectDetails.setIdentificationAlgorithmsForFile(Util.getFileName(idFile), software);

            // check for unsupported software
            if (!software.isEmpty()) {
                for (String advocateName : software.keySet()) {
                    Advocate advocate = Advocate.getAdvocate(advocateName);
                    if (advocate == null || advocate.getType() == Advocate.AdvocateType.unknown) {
                        waitingHandler.appendReport("Warning: The software used to generate " + idFile.getName() + " was not recognized by PeptideShaker. "
                                + "Please create an issue on the tool website and we will add support for the software used. "
                                + "github.com/compomics/peptide-shaker/issues", true, true);
                        return;
                    }
                }
            }

            fileReader.close();

            if (idFileSpectrumMatches != null && !waitingHandler.isRunCanceled()) {

                if (idFileSpectrumMatches.isEmpty()) {
                    waitingHandler.appendReport("No PSM found in " + idFile.getName() + ".", true, true);
                } else {

                    boolean allLoaded = true;
                    int numberOfMatches = idFileSpectrumMatches.size();
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                    waitingHandler.appendReport("Loading spectra for " + idFile.getName() + ".", true, true);
                    for (SpectrumMatch spectrumMatch : idFileSpectrumMatches) {
                        // Verify that the spectrum is in the provided mgf files
                        if (!importSpectrum(idFile, spectrumMatch, numberOfMatches)) {
                            allLoaded = false;
                        }
                        waitingHandler.increaseSecondaryProgressCounter();
                    }

                    if (allLoaded) {

                        // if any map spectrum sequencing matches on protein sequences
                        if (fileReader.hasDeNovoTags()) {
                            if (tagMapper == null) {
                                tagMapper = new TagMapper(identificationParameters, exceptionHandler);
                            }
                            tagMapper.mapTags(fileReader, identification, waitingHandler, processingPreferences.getnThreads());
                        }

                        // Batch map the peptides on protein sequences
                        if (peptideMapper != null && !peptideMapper.isCanceled()) {
                            try {
                                // Get map of peptides likely to need protein mapping
                                waitingHandler.resetSecondaryProgressCounter();
                                waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                                waitingHandler.appendReport("Collecting peptides to map.", true, true);
                                HashMap<String, LinkedList<Peptide>> peptideMap = PeptideMapper.getPeptideMap(fileReader, idFileSpectrumMatches, identification, identificationParameters, waitingHandler);
                                if (!peptideMapper.isCanceled()) {
                                    peptideMapper.mapPeptides(peptideMap, processingPreferences.getnThreads(), waitingHandler);
                                }
                                if (peptideMapper.isCanceled()) {
                                    peptideMap.clear();
                                }
                            } catch (OutOfMemoryError e) {
                                // Skip batch mapping and empty caches
                                SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
                                if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
                                    ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(1, null);
                                    sequenceFactory.getDefaultPeptideMapper().emptyCache();
                                }
                                peptideMapper.setCanceled(true);
                            }
                        }
                        // empty protein caches
                        if (MemoryConsumptionStatus.memoryUsed() > 0.8) {
                            SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
                            if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
                                ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(1, null);
                                sequenceFactory.emptyCache();
                            }
                        }

                        waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                        waitingHandler.appendReport("Importing PSMs from " + idFile.getName(), true, true);

                        PsmImporter psmImporter = new PsmImporter(peptideShaker.getCache(), identificationParameters, processingPreferences, fileReader, idFile, identification,
                                inputMap, proteinCount, singleProteinList, exceptionHandler);
                        psmImporter.importPsms(idFileSpectrumMatches, processingPreferences.getnThreads(), waitingHandler);

                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }

                        nPSMs += psmImporter.getnPSMs();
                        nSecondary += psmImporter.getnSecondary();
                        nRetained += psmImporter.getnRetained();

                        metrics.addFoundCharges(psmImporter.getCharges());
                        if (psmImporter.getMaxPeptideErrorDa() > metrics.getMaxPeptidePrecursorErrorDa()) {
                            metrics.setMaxPeptidePrecursorErrorDa(psmImporter.getMaxPeptideErrorDa());
                        }
                        if (psmImporter.getMaxPeptideErrorPpm() > metrics.getMaxPeptidePrecursorErrorPpm()) {
                            metrics.setMaxPeptidePrecursorErrorPpm(psmImporter.getMaxPeptideErrorPpm());
                        }
                        if (psmImporter.getMaxTagErrorDa() > metrics.getMaxTagPrecursorErrorDa()) {
                            metrics.setMaxTagPrecursorErrorDa(psmImporter.getMaxTagErrorDa());
                        }
                        if (psmImporter.getMaxTagErrorPpm() > metrics.getMaxTagPrecursorErrorPpm()) {
                            metrics.setMaxTagPrecursorErrorPpm(psmImporter.getMaxTagErrorPpm());
                        }

                        // Free at least 0.5GB for the next parser if not anymore available
                        if (!MemoryConsumptionStatus.halfGbFree() && !peptideShaker.getCache().isEmpty()) {
                            waitingHandler.appendReport("PeptideShaker is encountering memory issues! "
                                    + "See http://compomics.github.io/projects/peptide-shaker.html for help.", true, true);
                            waitingHandler.appendReport("Reducing Memory Consumption.", true, true);
                            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                            double share = ((double) 1073741824) / (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                            share = Math.min(share, 1);
                            peptideShaker.getCache().reduceMemoryConsumption(share, waitingHandler);
                            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                        }
                        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
                        if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
                            ProteinTree proteinTree = (ProteinTree) sequenceFactory.getDefaultPeptideMapper();
                            if (!MemoryConsumptionStatus.halfGbFree() && proteinTree.getNodesInCache() > 0) {
                                proteinTree.reduceNodeCacheSize(0.5);
                            }
                        }
                        projectDetails.addIdentificationFiles(idFile);

                        int psmsRejected = psmImporter.getPsmsRejected();
                        int noProteins = psmImporter.getMissingProteins();
                        int proteinIssue = psmImporter.getProteinIssue();
                        int peptideIssue = psmImporter.getPeptideIssue();
                        int precursorIssue = psmImporter.getPrecursorIssue();
                        int ptmIssue = psmImporter.getPtmIssue();
                        int totalAssumptionsRejected = noProteins + proteinIssue + peptideIssue + precursorIssue + ptmIssue;

                        double sharePsmsRejected = 100.0 * psmsRejected / numberOfMatches;

                        if (psmsRejected > 0) {
                            waitingHandler.appendReport(psmsRejected + " identified spectra (" + Util.roundDouble(sharePsmsRejected, 1) + "%) did not present a valid peptide.", true, true);
                            waitingHandler.appendReport(totalAssumptionsRejected + " of the best scoring peptides were excluded by the import filters:", true, true);

                            String padding = "    ";
                            PeptideAssumptionFilter idFilter = identificationParameters.getPeptideAssumptionFilter();

                            double share = 100 * ((double) noProteins) / totalAssumptionsRejected;
                            if (share >= 1) {
                                waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                        + "% peptide not matching to the database.", true, true);
                            }
                            share = 100 * ((double) proteinIssue) / totalAssumptionsRejected;
                            if (share >= 1) {
                                waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                        + "% peptide mapping to both target and decoy.", true, true);
                            }
                            share = 100 * ((double) peptideIssue) / totalAssumptionsRejected;
                            if (share >= 1) {
                                if (identificationParameters.getPeptideAssumptionFilter().getMinMissedCleavages() != null
                                        || identificationParameters.getPeptideAssumptionFilter().getMaxMissedCleavages() != null) {

                                    Integer minMissedCleavages = idFilter.getMinMissedCleavages();
                                    Integer maxMissedCleavages = idFilter.getMaxMissedCleavages();

                                    if (minMissedCleavages == null) {
                                        minMissedCleavages = 0;
                                    }
                                    if (maxMissedCleavages != null) {
                                        waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                                + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ",", true, true);
                                        waitingHandler.appendReport(padding + "    or number of missed cleavage sites outside of the range [" + minMissedCleavages + "-" + maxMissedCleavages + "].", true, true);
                                    } else {
                                        waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                                + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ",", true, true);
                                        waitingHandler.appendReport(padding + "    or number of missed cleavage sites lower than " + minMissedCleavages + ".", true, true);
                                    }
                                } else {
                                    waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                            + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ".", true, true);
                                }
                            }
                            share = 100 * ((double) precursorIssue) / totalAssumptionsRejected;
                            if (share >= 1) {
                                waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                        + "% peptide presenting high mass or isotopic deviation.", true, true);
                            }
                            share = 100 * ((double) ptmIssue) / totalAssumptionsRejected;
                            if (share >= 1) {
                                waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1) + "% unrecognized modifications.", true, true);
                            }
                        }
                        // inform the user in case search engine results could not be mapped to the database
                        boolean allSearchEngines = true;
                        for (String advocateName : software.keySet()) {
                            Advocate advocate = Advocate.getAdvocate(advocateName);
                            if (advocate.getType() != Advocate.AdvocateType.search_engine) {
                                allSearchEngines = false;
                                break;
                            }
                        }
                        if (allSearchEngines && noProteins > 0) {
                            String report = "Some peptides could not be mapped to the database. Please verify the following:" + System.getProperty("line.separator");
                            if (software.keySet().contains(Advocate.mascot.getName())) {
                                report += "- Make sure that Mascot was not used using the 'decoy' option.";
                            }
                            report
                                    += "- The protein sequence database must be the same or contain the database used for the search." + System.getProperty("line.separator")
                                    + "- When using the 'REVERSED' tag, decoy sequences must be reversed versions of the target sequences, use the 'DECOY' tag otherwise." + System.getProperty("line.separator")
                                    + "- When using in house databases make sure that the format is recognized by search engines and PeptideShaker (more details at http://compomics.github.io/searchgui/wiki/databasehelp.html)." + System.getProperty("line.separator")
                                    + "The problematic spectra can be inspected in the Spectrum ID tab. In case of doubt please contact the developers.";
                            waitingHandler.appendReport(report, true, true);
                        }

                        // inform the user in case more than 75% of the hits were rejected by the filters
                        if (sharePsmsRejected > 75) {
                            String report = "Warning: More than 75% of the PSMs did not pass the import filters." + System.getProperty("line.separator");
                            double meanRejected = sharePsmsRejected / 4;
                            if (!allSearchEngines && noProteins > meanRejected) {
                                report += " PeptideShaker did not manage to map most peptides to the database. Please verify your database." + System.getProperty("line.separator");
                            }
                            if (proteinIssue > meanRejected) {
                                report += " Apparently your database contains a high degree of shared peptides between the target and decoy sequences. Please verify your database";
                                if (software.keySet().contains(Advocate.mascot.getName())) {
                                    report += " and make sure that you use Mascot with the 'decoy' option disabled.";
                                }
                                report += "." + System.getProperty("line.separator");
                            }
                            if (peptideIssue > meanRejected) {
                                report += " Please verify that your peptide selection criteria are not too restrictive." + System.getProperty("line.separator");
                            }
                            if (precursorIssue > meanRejected) {
                                report += " Please verify that your precursor selection criteria are not too restrictive." + System.getProperty("line.separator");
                            }
                            if (ptmIssue > meanRejected) {
                                report += " Apparently your data contains modifications which are not recognized by PeptideShaker. Please verify the search parameters provided when creating the project." + System.getProperty("line.separator");
                                if (software.keySet().contains(Advocate.mascot.getName())) {
                                    report += " When using Mascot alone, you need to specify the search parameters manually when creating the project. We recommend the complementary use of SearchGUI when possible." + System.getProperty("line.separator");
                                }
                            }
                            waitingHandler.appendReport(report, true, true);
                        }
                    }
                }
            }

            waitingHandler.increasePrimaryProgressCounter();
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

            // import the mgf file if not done already
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
                    if (!missingMgfFiles.containsKey(idFile)) {
                        missingMgfFiles.put(idFile, fileName);
                        waitingHandler.appendReport(fileName + " not found.", true, true);
                    }
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
}
