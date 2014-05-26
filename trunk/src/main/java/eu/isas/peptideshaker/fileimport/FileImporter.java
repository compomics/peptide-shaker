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
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.identification_parameters.PepnovoParameters;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.identification.tags.Tag;
import com.compomics.util.experiment.identification.tags.TagComponent;
import com.compomics.util.experiment.io.identifications.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MzIdentMLIdfileReader;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.JOptionEditorPane;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import de.proteinms.omxparser.util.OMSSAIdfileReader;
import de.proteinms.xtandemparser.parser.XTandemIdfileReader;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.BufferedWriter;
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
    public ProteinTree proteinTree;

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
     * @param projectDetails the project details
     * @param backgroundThread boolean indicating whether the import should be
     * done in a background thread (GUI mode) or in the current thread (command
     * line mode).
     */
    public void importFiles(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, ProcessingPreferences processingPreferences, PTMScoringPreferences ptmScoringPreferences,
            SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, boolean backgroundThread) {

        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles, spectrumFiles, idFilter, searchParameters, annotationPreferences,
                processingPreferences, ptmScoringPreferences, spectrumCountingPreferences, projectDetails);

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
     * @param searchParameters the search parameters
     */
    public void importSequences(WaitingHandler waitingHandler, File fastaFile, SearchParameters searchParameters) {

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
                        + "(See also <a href=\"https://code.google.com/p/compomics-utilities/wiki/ProteinInference\">Protein Inference</a>).", true, true);

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
            waitingHandler.appendReport(e.getLocalizedMessage() + " Please refer to the <a href=\"https://code.google.com/p/peptide-shaker/#Troubleshooting\">troubleshooting section</a>.", true, true);
        } catch (ClassNotFoundException e) {
            System.err.println("An error occured while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("Serialization issue while processing the FASTA file. Please delete the .fasta.cui file and retry. "
                    + "If the error occurs again please report bug using our <a href=\"https://code.google.com/p/peptide-shaker/issues/list\">issue tracker</a>.", true, true);
        } catch (NullPointerException e) {
            System.err.println("An error occured while loading " + fastaFile + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            waitingHandler.appendReport("An error occurred when importing the sequences. "
                    + "Please check the Search Parameters. See the log file for details. "
                    + "If the error persists please let us know using our <a href=\"https://code.google.com/p/peptide-shaker/issues/list\">issue tracker</a>.", true, true);
        }
    }

    /**
     * Remaps the PTMs for a given tag based on the search parameters.
     *
     * @param tag the tag with original algorithm PTMs
     * @param searchParameters the parameters used for the identification
     * @param advocateId the ID of the advocate
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void mapPtmsForTag(Tag tag, SearchParameters searchParameters, int advocateId) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        // add the fixed PTMs
        ptmFactory.checkFixedModifications(searchParameters.getModificationProfile(), tag, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

        // rename the variable modifications
        for (TagComponent tagComponent : tag.getContent()) {
            if (tagComponent instanceof AminoAcidPattern) {

                AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;

                for (int aa : aminoAcidPattern.getModificationIndexes()) {
                    for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(aa)) {
                        if (modificationMatch.isVariable()) {
                            if (advocateId == Advocate.pepnovo.getIndex()) {
                                String pepnovoPtmName = modificationMatch.getTheoreticPtm();
                                PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocateId);
                                String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("PepNovo+ PTM " + pepnovoPtmName + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
                            } else if (advocateId == Advocate.direcTag.getIndex()) {
                                Integer directagIndex = new Integer(modificationMatch.getTheoreticPtm());
                                String utilitiesPtmName = searchParameters.getModificationProfile().getVariableModifications().get(directagIndex);
                                if (utilitiesPtmName == null) {
                                    throw new IllegalArgumentException("DirecTag PTM " + directagIndex + " not recognized.");
                                }
                                modificationMatch.setTheoreticPtm(utilitiesPtmName);
                                PTM ptm = ptmFactory.getPTM(utilitiesPtmName);
                                ArrayList<AminoAcid> aaAtTarget = ptm.getPattern().getAminoAcidsAtTarget();
                                if (aaAtTarget.size() > 1) {
                                    throw new IllegalArgumentException("More than one amino acid can be targeted by the modification " + ptm + ", tag duplication required.");
                                }
                                int aaIndex = aa - 1;
                                aminoAcidPattern.setTargeted(aaIndex, aaAtTarget);
                            } else {
                                Advocate notImplemented = Advocate.getAdvocate(advocateId);
                                if (notImplemented == null) {
                                    throw new IllegalArgumentException("Advocate of id " + advocateId + " not recognized.");
                                }
                                throw new IllegalArgumentException("PTM mapping not implemented for " + Advocate.getAdvocate(advocateId).getName() + ".");
                            }
                        }
                    }
                }
            }
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
         * Constructor of the worker.
         *
         * @param idFiles ArrayList containing the identification files
         */
        public IdProcessorFromFile(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, IdFilter idFilter,
                SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ProcessingPreferences processingPreferences,
                PTMScoringPreferences ptmScoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails) {

            this.idFiles = new ArrayList<File>();
            HashMap<String, File> filesMap = new HashMap<String, File>();

            for (File file : idFiles) {
                filesMap.put(file.getName(), file);
            }

            ArrayList<String> names = new ArrayList<String>(filesMap.keySet());
            Collections.sort(names);

            for (String name : names) {
                this.idFiles.add(filesMap.get(name));
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

            for (File file : spectrumFiles) {
                this.spectrumFiles.put(file.getName(), file);
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
        public synchronized int importFiles() {

            try {
                importSequences(waitingHandler, fastaFile, searchParameters);

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
                    sequenceFactory.emptyCache(); // @TODO: should only be used in extreme cases!!

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
                            idFilter, processingPreferences, ptmScoringPreferences, spectrumCountingPreferences);
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
                } else if (ExceptionHandler.getExceptionType(e).equalsIgnoreCase("Protein not found")) {
                    waitingHandler.appendReport("An error occured while loading the identification files:", true, true);
                    waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
                    waitingHandler.appendReport("Please see the <a href=\"http://code.google.com/p/searchgui/wiki/DatabaseHelp\">Database help page</a>.", true, true);
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
                        + "Please let us know using our <a href=\"https://code.google.com/p/peptide-shaker/issues/list\">issue tracker</a>.", true, true);
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

            boolean idReport;
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

            // set the search engine name and version for this file
            HashMap<String, ArrayList<String>> software = fileReader.getSoftwareVersions();
            projectDetails.setIdentificationAlgorithmsForFile(Util.getFileName(idFile), software);

            waitingHandler.setSecondaryProgressCounterIndeterminate(false);

            HashSet<SpectrumMatch> tempSet = null;
            try {
                tempSet = fileReader.getAllSpectrumMatches(waitingHandler);
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while loading spectrum matches from \'"
                        + Util.getFileName(idFile)
                        + "\'. This file will be ignored. Error: " + e.getMessage()
                        + " See resources/PeptideShaker.log for details.", true, true);
                e.printStackTrace();
            }
            fileReader.close();

            if (tempSet != null) {

                int numberOfMatches = tempSet.size(),
                        progress = 0,
                        rejected = 0,
                        proteinIssue = 0,
                        peptideIssue = 0,
                        precursorIssue = 0,
                        ptmIssue = 0;
                waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                idReport = false;
                ArrayList<Integer> charges = new ArrayList<Integer>();
                double maxPeptideErrorPpm = 0, maxPeptideErrorDa = 0, maxTagErrorPpm = 0, maxTagErrorDa = 0;

                ArrayDeque<SpectrumMatch> queue = new ArrayDeque<SpectrumMatch>(tempSet);
                tempSet.clear();

                while (!queue.isEmpty()) {

                    // free memory if needed
                    if (memoryUsed() > 0.8 && !peptideShaker.getCache().isEmpty()) {
                        peptideShaker.getCache().reduceMemoryConsumption(0.5, waitingHandler);
                    }
                    if (!halfGbFree() && sequenceFactory.getNodesInCache() > 0) {
                        sequenceFactory.reduceNodeCacheSize(0.5);
                    }

                    SpectrumMatch match = queue.pollLast();

                    for (int advocateId : match.getAdvocates()) {

                        nPSMs++;
                        nSecondary += match.getAllAssumptions().size() - 1;

                        String spectrumKey = match.getKey();
                        String fileName = Spectrum.getSpectrumFile(spectrumKey);
                        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

                        // remap wrong spectrum file names
                        if (spectrumFactory.getSpectrumFileFromIdName(fileName) != null) {
                            fileName = spectrumFactory.getSpectrumFileFromIdName(fileName).getName();
                            match.setKey(Spectrum.getSpectrumKey(fileName, spectrumTitle));
                            spectrumKey = match.getKey();
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
                                break;
                            }
                        }
                        if (!idReport) {
                            waitingHandler.appendReport("Importing PSMs from " + idFile.getName(), true, true);
                            idReport = true;
                        }

                        // remap missing spectrum titles
                        if (spectrumFactory.fileLoaded(fileName) && !spectrumFactory.spectrumLoaded(spectrumKey)) {
                            String oldTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            Integer spectrumNumber = match.getSpectrumNumber();
                            if (spectrumNumber == null) {
                                waitingHandler.appendReport("Spectrum \'" + oldTitle + "\' not found in file " + fileName + ".", true, true);
                                waitingHandler.setRunCanceled();
                                return;
                            }
                            spectrumTitle = spectrumFactory.getSpectrumTitle(fileName, spectrumNumber);
                            spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                            match.setKey(spectrumKey);
                            if (!spectrumFactory.spectrumLoaded(spectrumKey)) {
                                spectrumTitle = spectrumNumber + "";
                                spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                                match.setKey(spectrumKey);
                                if (spectrumFactory.fileLoaded(fileName) && !spectrumFactory.spectrumLoaded(spectrumKey)) {
                                    waitingHandler.appendReport("Spectrum \'" + oldTitle + "\' number " + spectrumTitle + " not found in file " + fileName + ".", true, true);
                                    waitingHandler.setRunCanceled();
                                    return;
                                }
                            }
                        }

                        // Map spectrum sequencing matches on protein sequences
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = match.getAllAssumptions(advocateId);
                        if (assumptionsMap != null) {
                            ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                            for (double score : scores) {
                                ArrayList<SpectrumIdentificationAssumption> tempAssumptions = new ArrayList<SpectrumIdentificationAssumption>(assumptionsMap.get(score));
                                for (SpectrumIdentificationAssumption assumption : tempAssumptions) {
                                    if (assumption instanceof TagAssumption) {
                                        TagAssumption tagAssumption = (TagAssumption) assumption;
                                        mapPtmsForTag(tagAssumption.getTag(), searchParameters, advocateId);
                                        try {
                                            HashMap<Peptide, HashMap<String, ArrayList<Integer>>> proteinMapping = proteinTree.getProteinMapping(tagAssumption.getTag(), PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy(), searchParameters.getModificationProfile().getFixedModifications(), searchParameters.getModificationProfile().getVariableModifications(), true, true);
                                            for (Peptide peptide : proteinMapping.keySet()) {
                                                PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, tagAssumption.getRank(), advocateId, assumption.getIdentificationCharge(), score, assumption.getIdentificationFile());
                                                peptideAssumption.addUrParam(tagAssumption);
                                                match.addHit(advocateId, peptideAssumption, true);
                                            }
                                        } catch (Exception e) {
                                            waitingHandler.appendReport("An error occurred while mapping tag " + tagAssumption.getTag().asSequence() + " of spectrum " + spectrumTitle + " onto the database.", idReport, idReport);
                                            throw e;
                                        }
                                    }
                                }
                            }
                        }

                        for (SpectrumIdentificationAssumption assumption : match.getAllAssumptions()) {
                            if (assumption instanceof PeptideAssumption) {
                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                if (!idFilter.validatePeptideAssumption(peptideAssumption)) {
                                    match.removeAssumption(assumption);
                                    peptideIssue++;
                                }
                            }
                        }

                        if (!match.hasAssumption(advocateId)) {
                            rejected++;
                        } else {

                            if (match.hasAssumption(advocateId)) {

                                // Check whether there is a potential first hit which does not belong to the target and the decoy database
                                boolean targetOrDecoy = false;
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

                                            ptmFactory.checkFixedModifications(modificationProfile, peptide, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
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
                                                            tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, omssaName, PeptideShaker.MATCHING_TYPE, ptmMassTolerance, searchParameters.getFragmentIonAccuracy());
                                                        }
                                                    } else if (fileReader instanceof MascotIdfileReader
                                                            || fileReader instanceof XTandemIdfileReader
                                                            || fileReader instanceof MzIdentMLIdfileReader) {
                                                        String[] parsedName = sePTM.split("@");
                                                        double seMass = 0;
                                                        try {
                                                            seMass = new Double(parsedName[0]);
                                                        } catch (Exception e) {
                                                            throw new IllegalArgumentException("Impossible to parse \'" + sePTM + "\' as a tagged modification.\n"
                                                                    + "Error encountered in peptide " + peptideSequence + " spectrum " + spectrumTitle + " in file " + fileName + ".");
                                                        }
                                                        tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, seMass, ptmMassTolerance, searchParameters.getFragmentIonAccuracy(), PeptideShaker.MATCHING_TYPE);
                                                    } else if (!(fileReader instanceof DirecTagIdfileReader)) {
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
                                                        if (expectedNames.containsKey(pos)) {
                                                            expectedNames.get(pos).addAll(tempNames.get(pos));
                                                        } else {
                                                            expectedNames.put(pos, tempNames.get(pos));
                                                        }
                                                    }
                                                }
                                            }

                                            // Map the modifications according to search engine localization
                                            HashMap<Integer, ModificationMatch> ptmMappingRegular = new HashMap<Integer, ModificationMatch>();
                                            HashMap<ModificationMatch, Integer> ptmMappingGoofy = new HashMap<ModificationMatch, Integer>();

                                            // If a terminal modification cannot be elsewhere lock the terminus
                                            ModificationMatch nTermModification = null;
                                            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                    int modSite = modMatch.getModificationSite();
                                                    if (modSite == 1) {
                                                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                        if (expectedNamesAtSite != null) {
                                                            for (String modName : expectedNamesAtSite) {
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
                                                if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                    int modSite = modMatch.getModificationSite();
                                                    if (modSite == peptideSequence.length()) {
                                                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                        if (expectedNamesAtSite != null) {
                                                            for (String modName : expectedNamesAtSite) {
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

                                            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                    int modSite = modMatch.getModificationSite();
                                                    boolean terminal = false;
                                                    if (modSite == 1) {
                                                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                        if (expectedNamesAtSite != null) {
                                                            for (String modName : expectedNamesAtSite) {
                                                                PTM ptm = ptmFactory.getPTM(modName);
                                                                if (ptm.isNTerm() && nTermModification == null) {
                                                                    nTermModification = modMatch;
                                                                    modMatch.setTheoreticPtm(modName);
                                                                    terminal = true;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    } else if (modSite == peptideSequence.length()) {
                                                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                        if (expectedNamesAtSite != null) {
                                                            for (String modName : expectedNamesAtSite) {
                                                                PTM ptm = ptmFactory.getPTM(modName);
                                                                if (ptm.isCTerm() && cTermModification == null) {
                                                                    cTermModification = modMatch;
                                                                    modMatch.setTheoreticPtm(modName);
                                                                    terminal = true;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (!terminal) {
                                                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                                        if (expectedNamesAtSite != null) {
                                                            for (String modName : expectedNamesAtSite) {
                                                                PTM ptm = ptmFactory.getPTM(modName);
                                                                if (ptm.isNTerm() && nTermModification == null) {
                                                                    nTermModification = modMatch;
                                                                    modMatch.setTheoreticPtm(modName);
                                                                } else if (ptm.isCTerm() && cTermModification == null) {
                                                                    cTermModification = modMatch;
                                                                    modMatch.setTheoreticPtm(modName);
                                                                } else if (modNames.get(modMatch).contains(modName) && !ptmMappingRegular.containsKey(modSite)) {
                                                                    ptmMappingRegular.put(modSite, modMatch);
                                                                    ptmMappingGoofy.put(modMatch, modSite);
                                                                    modMatch.setTheoreticPtm(modName);
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Try to correct incompatible localizations
                                            HashMap<Integer, ArrayList<Integer>> remap = new HashMap<Integer, ArrayList<Integer>>();

                                            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !ptmMappingGoofy.containsKey(modMatch) && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                    int modSite = modMatch.getModificationSite();
                                                    for (int candidateSite : expectedNames.keySet()) {
                                                        if (!ptmMappingRegular.containsKey(candidateSite)) {
                                                            for (String modName : expectedNames.get(candidateSite)) {
                                                                if (modNames.get(modMatch).contains(modName)) {
                                                                    if (!remap.containsKey(modSite)) {
                                                                        remap.put(modSite, new ArrayList<Integer>());
                                                                    }
                                                                    if (!remap.get(modSite).contains(candidateSite)) {
                                                                        remap.get(modSite).add(candidateSite);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            HashMap<Integer, Integer> correctedIndexes = PtmSiteMapping.alignAll(remap);

                                            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                                if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !ptmMappingGoofy.containsKey(modMatch) && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                                    Integer modSite = correctedIndexes.get(modMatch.getModificationSite());
                                                    if (modSite != null) {
                                                        if (expectedNames.containsKey(modSite)) {
                                                            for (String modName : expectedNames.get(modSite)) {
                                                                if (modNames.get(modMatch).contains(modName)) {
                                                                    ptmMappingGoofy.put(modMatch, modSite);
                                                                    modMatch.setTheoreticPtm(modName);
                                                                    modMatch.setModificationSite(modSite);
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        ptmMappingGoofy.put(modMatch, modSite);
                                                        modMatch.setTheoreticPtm(PTMFactory.unknownPTM.getName());
                                                    }
                                                    if (!ptmMappingGoofy.containsKey(modMatch)) {
                                                        modMatch.setTheoreticPtm(PTMFactory.unknownPTM.getName());
                                                    }
                                                }
                                            }

                                            if (idFilter.validateModifications(peptide, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy(), searchParameters.getModificationProfile())) {
                                                // Estimate the theoretic mass with the new modifications
                                                peptide.estimateTheoreticMass();
                                                if (!idFilter.validatePrecursor(peptideAssumption, spectrumKey, spectrumFactory)) {
                                                    match.removeAssumption(assumption);
                                                    precursorIssue++;
                                                } else if (!targetOrDecoy) {
                                                    // Check whether there is a potential first hit which does not belong to the target and the decoy database
                                                    if (!idFilter.validateProteins(peptideAssumption.getPeptide(), PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                                                        match.removeAssumption(assumption);
                                                        proteinIssue++;
                                                    } else {
                                                        targetOrDecoy = true;
                                                    }
                                                }
                                            } else {
                                                match.removeAssumption(assumption);
                                                ptmIssue++;
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
                                            ArrayList<String> accessions = peptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
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
                                            inputMap.addEntry(advocateId, fileName, firstPeptideHit.getScore(), firstPeptideHit.getPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy()));
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
                                rejected++;
                            }
                        }

                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }

                        waitingHandler.setSecondaryProgressCounter(++progress);
                    }
                }

                metrics.addFoundCharges(charges);
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
                if (!halfGbFree() && !peptideShaker.getCache().isEmpty()) {
                    waitingHandler.appendReport("PeptideShaker is encountering memory issues! "
                            + "See <a href=\"http://peptide-shaker.googlecode.com\">http://peptide-shaker.googlecode.com</a> for help.", true, true);
                    waitingHandler.appendReport("Reducing Memory Consumption.", true, true);
                    waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                    double share = ((double) 1073741824) / (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                    share = Math.min(share, 1);
                    peptideShaker.getCache().reduceMemoryConsumption(share, waitingHandler);
                    waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                }
                if (!halfGbFree() && sequenceFactory.getNodesInCache() > 0) {
                    sequenceFactory.reduceNodeCacheSize(0.5);
                }
                projectDetails.addIdentificationFiles(idFile);

                double total = proteinIssue + peptideIssue + precursorIssue + ptmIssue;
                double proteinIssueShare = 100.0 * proteinIssue / total;
                double peptideIssueShare = 100.0 * peptideIssue / total;
                double precursorIssueShare = 100.0 * precursorIssue / total;
                double ptmIssueShare = 100.0 * ptmIssue / total;
                double share = 100.0 * rejected / numberOfMatches;

                if (rejected > 0) {
                    waitingHandler.appendReport(rejected + " matches (" + Util.roundDouble(share, 1) + "%) excluded by the import filters:", true, true);

                    String padding = "&nbsp;&nbsp;&nbsp;&nbsp;";

                    if (waitingHandler instanceof WaitingHandlerCLIImpl) {
                        padding = "    ";
                    }

                    if (proteinIssueShare > 0) {
                        waitingHandler.appendReport(padding + "- " + proteinIssue
                                + " (" + Util.roundDouble(proteinIssueShare, 1) + "%) mapped in target and decoy.", true, true);
                    }
                    if (peptideIssueShare > 0) {
                        waitingHandler.appendReport(padding + "- " + peptideIssue
                                + " (" + Util.roundDouble(peptideIssueShare, 1) + "%) size or e-value out of boundary.", true, true);
                    }
                    if (precursorIssueShare > 0) {
                        waitingHandler.appendReport(padding + "- " + precursorIssue
                                + " (" + Util.roundDouble(precursorIssueShare, 1) + "%) high precursor deviation.", true, true);
                    }
                    if (ptmIssueShare > 0) {
                        waitingHandler.appendReport(padding + "- " + ptmIssue + " (" + Util.roundDouble(ptmIssueShare, 1) + "%) unrecognized modifications.", true, true);
                    }
                }
                // inform the user in case more than 75% of the hits were rejected by the filters
                if (share > 75) {
                    String report = "Warning: More than 75% of the PSMs were rejected by the loading filters when importing the matches.";
                    double meanRejected = total / 4;
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

            waitingHandler.increasePrimaryProgressCounter();
        }

        /**
         * Indicates whether a GB of memory is free.
         *
         * @return a boolean indicating whether a GB of memory is free
         */
        public boolean halfGbFree() {
            return Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) > 536870912;
        }

        /**
         * Returns the share of memory being used.
         *
         * @return the share of memory being used
         */
        public double memoryUsed() {
            return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().maxMemory();
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
     * Write a filter.
     *
     * @param bw
     * @param fileName
     * @param spectrumTitle
     * @param peptideAssumption
     * @throws IOException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    private void writeFilter(BufferedWriter bw, String fileName, String spectrumTitle, PeptideAssumption peptideAssumption)
            throws IOException, InterruptedException, SQLException, ClassNotFoundException, MzMLUnmarshallerException {

        bw.write(fileName + "\t" + spectrumTitle + "\t");
        boolean first = true;
        for (String accession : peptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, 0.5)) {
            if (first) {
                first = false;
            } else {
                bw.write(", ");
            }
            bw.write(accession);
        }
        bw.write("\t" + peptideAssumption.getPeptide().getSequence() + "\t");
        first = true;
        for (ModificationMatch modificationMatch : peptideAssumption.getPeptide().getModificationMatches()) {
            if (first) {
                first = false;
            } else {
                bw.write(", ");
            }
            bw.write(modificationMatch.getTheoreticPtm());
        }
        bw.write("\t" + peptideAssumption.getScore());
        Precursor prec = spectrumFactory.getPrecursor(fileName, spectrumTitle);
        bw.write("\t" + peptideAssumption.getDeltaMass(prec.getMz(), true, true) + "\t");
    }
}
