package eu.isas.peptideshaker.fileimport;

import eu.isas.peptideshaker.gui.MgfFilesNotFoundDialog;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.mascotdatfile.util.io.MascotIdfileReader;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
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
     * This boolean sets the FileImporter to be aware that PeptideShaker is
     * working in CLI.
     */
    private static boolean boolCLI = false;
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
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance(100000);
    /**
     * Peptide to protein map: peptide sequence -> protein accession.
     */
    private HashMap<String, ArrayList<String>> sharedPeptides = new HashMap<String, ArrayList<String>>();
    /**
     * Peptide to protein map: peptide sequence -> protein accession.
     */
    private HashMap<String, ArrayList<String>> foundSharedPeptides = new HashMap<String, ArrayList<String>>();
    /**
     * db processing disabled if no X!Tandem file is selected.
     */
    private boolean needPeptideMap = false;
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
     * The search parameters.
     */
    private SearchParameters searchParameters;
    /**
     * The mass tolerance to be used to match PTMs from search engines and
     * expected PTMs. 0.01 by default, as far as I can remember it is the mass
     * resolution in X!Tandem result files.
     */
    public static final double ptmMassTolerance = 0.01;

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
    public FileImporter(PeptideShaker identificationShaker, WaitingHandler waitingHandler, ProteomicAnalysis proteomicAnalysis, IdFilter idFilter, Metrics metrics, SearchParameters searchParameters) {
        this.peptideShaker = identificationShaker;
        this.waitingHandler = waitingHandler;
        this.proteomicAnalysis = proteomicAnalysis;
        this.idFilter = idFilter;
        this.metrics = metrics;
        this.searchParameters = searchParameters;
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
     */
    public void importFiles(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, ProcessingPreferences processingPreferences, PTMScoringPreferences ptmScoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences) {

        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles, spectrumFiles, idFilter, searchParameters, annotationPreferences, processingPreferences, ptmScoringPreferences, spectrumCountingPreferences);

        if (boolCLI) {
            // CLI mode needs to call the SwingWorker's running method directly.
            try {
                idProcessor.doInBackground();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

        } else {
            // GUI mode
            idProcessor.execute();
        }
    }

    /**
     * Imports sequences from a fasta file.
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param proteomicAnalysis The proteomic analysis to attach the database to
     * @param fastaFile FASTA file to process
     * @param idFilter the identification filter
     * @param searchParameters the search parameters
     */
    public void importSequences(WaitingHandler waitingHandler, ProteomicAnalysis proteomicAnalysis, File fastaFile, IdFilter idFilter, SearchParameters searchParameters) {

        try {
            waitingHandler.appendReport("Importing sequences from " + fastaFile.getName() + ".", true, true);
            waitingHandler.setSecondaryProgressDialogIndeterminate(false);
            sequenceFactory.loadFastaFile(fastaFile, waitingHandler);

            waitingHandler.resetSecondaryProgressBar();
            waitingHandler.setSecondaryProgressDialogIndeterminate(true);

            if (needPeptideMap) {
                if (2 * sequenceFactory.getNTargetSequences() < sequenceFactory.getnCache()) { // @TODO: should this be overrideable by the user??
                    waitingHandler.appendReport("Creating peptide to protein map.", true, true);

                    Enzyme enzyme = searchParameters.getEnzyme();
                    if (enzyme == null) {
                        throw new NullPointerException("Enzyme not found");
                    }
                    int nMissedCleavages = searchParameters.getnMissedCleavages();
                    int nMin = idFilter.getMinPepLength();
                    int nMax = idFilter.getMaxPepLength();
                    sharedPeptides = new HashMap<String, ArrayList<String>>();
                    HashMap<String, String> tempMap = new HashMap<String, String>();

                    int numberOfSequences = sequenceFactory.getAccessions().size();

                    waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                    waitingHandler.setMaxSecondaryProgressValue(numberOfSequences);

                    for (String proteinKey : sequenceFactory.getAccessions()) {

                        waitingHandler.increaseSecondaryProgressValue();

                        String sequence = sequenceFactory.getProtein(proteinKey).getSequence();

                        for (String peptide : enzyme.digest(sequence, nMissedCleavages, nMin, nMax)) {
                            ArrayList<String> proteins = sharedPeptides.get(peptide);
                            if (proteins != null) {
                                proteins.add(proteinKey);
                            } else {
                                String tempProtein = tempMap.get(peptide);
                                if (tempProtein != null) {
                                    ArrayList<String> tempList = new ArrayList<String>(2);
                                    tempList.add(tempProtein);
                                    tempList.add(proteinKey);
                                    sharedPeptides.put(peptide, tempList);
                                } else {
                                    tempMap.put(peptide, proteinKey);
                                }
                            }
                        }
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    }
                    tempMap.clear();

                    waitingHandler.setSecondaryProgressDialogIndeterminate(true);
                } else {
                    waitingHandler.appendReport("The database is too large to be parsed into peptides. Note that X!Tandem peptides might present protein inference issues.", true, true);
                }
            }

            waitingHandler.appendReport("FASTA file import completed.", true, true);
            waitingHandler.increaseProgressValue();

        } catch (FileNotFoundException e) {
            waitingHandler.appendReport("File " + fastaFile + " was not found. Please select a different FASTA file.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } catch (IOException e) {
            waitingHandler.appendReport("An error occured while loading " + fastaFile + ".", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } catch (InterruptedException e) {
            waitingHandler.appendReport("An error occured while loading " + fastaFile + ".", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } catch (IllegalArgumentException e) {
            waitingHandler.appendReport(e.getLocalizedMessage() + "\n" + "Please refer to the troubleshooting section at http://peptide-shaker.googlecode.com.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } catch (ClassNotFoundException e) {
            waitingHandler.appendReport("Serialization issue while processing the FASTA file. Please delete the .fasta.cui file and retry.\n"
                    + "If the error occurs again please report bug at http://peptide-shaker.googlecode.com.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        } catch (NullPointerException e) {

            // @TODO: this might not the only null pointer that can oocur?

            waitingHandler.appendReport("The enzyme to use was not found.\n"
                    + "Please verify the Search Parameters given while creating the project.\n"
                    + "If the enzyme does not appear, verify that it is implemented in peptideshaker_enzymes.xml located in the conf folder of the PeptideShaker folder.\n\n"
                    + "If the error persists please report bug at http://peptide-shaker.googlecode.com.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        }
    }

    /**
     * Returns the list of proteins which contain in their sequence the given
     * peptide sequence.
     *
     * @param peptideSequence the tested peptide sequence
     * @param waitingHandler the handler displaying feedback to the user
     * @return a list of corresponding proteins found in the database
     */
    private ArrayList<String> getProteins(String peptideSequence, WaitingHandler waitingHandler) {

        // @TODO: the use of contains(...) below is very slow!! using something like suffix trees should be a lot faster

        ArrayList<String> result = foundSharedPeptides.get(peptideSequence);

        if (result == null) {
            result = sharedPeptides.get(peptideSequence);

            boolean inspectAll = 2 * sequenceFactory.getNTargetSequences() < sequenceFactory.getnCache() && needPeptideMap;

            if (result == null) {
                result = new ArrayList<String>();
                if (inspectAll) {
                    try {
                        for (String proteinKey : sequenceFactory.getAccessions()) {
                            if (sequenceFactory.getProtein(proteinKey).getSequence().contains(peptideSequence)) {
                                result.add(proteinKey);
                            }
                            if (waitingHandler.isRunCanceled()) {
                                return new ArrayList<String>();
                            }
                        }
                    } catch (IOException e) {
                        waitingHandler.appendReport("An error occured while accessing the FASTA file."
                                + "\nProtein to peptide link will be incomplete. Please restart the analysis.", true, true);
                        e.printStackTrace();
                        waitingHandler.setRunCanceled();
                    } catch (InterruptedException e) {
                        waitingHandler.appendReport("An error occured while accessing the FASTA file."
                                + "\nProtein to peptide link will be incomplete. Please restart the analysis.", true, true);
                        e.printStackTrace();
                        waitingHandler.setRunCanceled();
                    } catch (IllegalArgumentException e) {
                        waitingHandler.appendReport(e.getLocalizedMessage() + "\n" + "Please refer to the troubleshooting section at http://peptide-shaker.googlecode.com."
                                + "\nProtein to peptide link will be incomplete. Please restart the analysis.", true, true);
                        e.printStackTrace();
                        waitingHandler.setRunCanceled();
                    }
                    sharedPeptides.put(peptideSequence, result);
                }
            } else {
                foundSharedPeptides.put(peptideSequence, result);
            }
        }
        return result;
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
         * The fasta file.
         */
        private File fastaFile;
        /**
         * A list of spectrum files (can be empty, no spectrum will be imported).
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
         * Constructor of the worker.
         *
         * @param idFiles ArrayList containing the identification files
         */
        public IdProcessorFromFile(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, IdFilter idFilter,
                SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ProcessingPreferences processingPreferences, PTMScoringPreferences ptmScoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences) {

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

            for (File file : spectrumFiles) {
                this.spectrumFiles.put(file.getName(), file);
            }
        }

        @Override
        protected Object doInBackground() throws Exception {

            waitingHandler.appendReport("Establishing database connection.", true, true);

            Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            identification.setIsDB(true);

            try {
                identification.establishConnection(PeptideShaker.SERIALIZATION_DIRECTORY, true, peptideShaker.getCache());
            } catch (SQLException e) {
                e.printStackTrace();
                waitingHandler.appendReport("The match database could not be created, serialized matches will be used instead. Please contact the developers.", true, true);
                identification.setIsDB(false);
            }

            waitingHandler.increaseProgressValue();

            for (File idFile : idFiles) {
                int searchEngine = readerFactory.getSearchEngine(idFile);
                if (searchEngine == Advocate.XTANDEM) {
                    needPeptideMap = true;
                    break;
                }
            }

            importSequences(waitingHandler, proteomicAnalysis, fastaFile, idFilter, searchParameters);

            try {

                waitingHandler.appendReport("Reading identification files.", true, true);

                for (File idFile : idFiles) {
                    importPsms(idFile);
                }

                while (!missingMgfFiles.isEmpty()) {
                    if (boolCLI) {
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
                        waitingHandler.displayMessage("MGF files missing", missingFiles, 1);
                    } else {
                        new MgfFilesNotFoundDialog((WaitingDialog) waitingHandler, missingMgfFiles);
                        if (waitingHandler.isRunCanceled()) {
                            return 1;
                        }
                    }
                    waitingHandler.appendReport("Processing files with the new input.", true, true);
                    ArrayList<File> filesToProcess = new ArrayList<File>(missingMgfFiles.keySet());

                    for (String mgfName : missingMgfFiles.values()) {
                        File newFile = spectrumFactory.getSpectrumFileFromIdName(mgfName);
                        spectrumFiles.put(newFile.getName(), newFile);
                    }
                    missingMgfFiles.clear();
                    for (File idFile : filesToProcess) {
                        importPsms(idFile);
                    }
                    if (waitingHandler.isRunCanceled()) {
                        return 1;
                    }
                }

                // clear the objects not needed anymore
                sharedPeptides.clear();
                foundSharedPeptides.clear();
                singleProteinList.clear();

                if (nRetained == 0) {
                    waitingHandler.appendReport("No identifications retained.", true, true);
                    waitingHandler.setRunCanceled();
                    return 1;
                }

                waitingHandler.appendReport("File import completed. "
                        + nPSMs + " first hits imported (" + nSecondary + " secondary) from " + nSpectra + " spectra.", true, true);
                waitingHandler.appendReport("[" + nRetained + " first hits passed the initial filtering]", true, true);
                waitingHandler.increaseSecondaryProgressValue(spectrumFiles.size() - mgfUsed.size());
                peptideShaker.setProteinCountMap(proteinCount);
                peptideShaker.processIdentifications(inputMap, waitingHandler, searchParameters, annotationPreferences, idFilter, processingPreferences, ptmScoringPreferences, spectrumCountingPreferences);

            } catch (Exception e) {
                waitingHandler.appendReport("An error occured while loading the identification files:", true, true);
                waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
                waitingHandler.setRunCanceled();
                e.printStackTrace();
            } catch (OutOfMemoryError error) {
                System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
                Runtime.getRuntime().gc();
                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Ran out of memory!", true, true);
                waitingHandler.setRunCanceled();
                JOptionPane.showMessageDialog(null,
                        "The task used up all the available memory and had to be stopped.\n"
                        + "You can increase the memory allocated to PeptideShaker under Edit -> Java Options.\n"
                        + "More help can be found at our website http://peptide-shaker.googlecode.com.",
                        "Out Of Memory Error",
                        JOptionPane.ERROR_MESSAGE);
                error.printStackTrace();
            }

            return 0;
        }

        /**
         * Imports the psms from an identification file.
         *
         * @param idFile the identification file
         * @throws FileNotFoundException exception thrown whenever a file was
         * not found
         * @throws IOException exception thrown whenever an error occurred while
         * reading or writing a file
         * @throws SAXException exception thrown whenever an error occurred
         * while parsing an xml file
         * @throws MzMLUnmarshallerException exception thrown whenever an error
         * occurred while reading an mzML file
         */
        public void importPsms(File idFile) throws FileNotFoundException, IOException, SAXException, MzMLUnmarshallerException, IllegalArgumentException, Exception {

            boolean idReport, goodFirstHit, unknown = false;
            Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            waitingHandler.setSecondaryProgressDialogIndeterminate(true);
            waitingHandler.appendReport("Parsing " + idFile.getName() + ".", true, true);
            IdfileReader fileReader;

            int searchEngine = readerFactory.getSearchEngine(idFile);

            if (searchEngine == Advocate.MASCOT && idFile.length() > mascotMaxSize * 1048576) {
                fileReader = new MascotIdfileReader(idFile, true);
            } else {
                fileReader = readerFactory.getFileReader(idFile, null);
            }

            waitingHandler.setSecondaryProgressDialogIndeterminate(false);
            HashSet<SpectrumMatch> tempSet = fileReader.getAllSpectrumMatches(waitingHandler);
            fileReader.close();
            Iterator<SpectrumMatch> matchIt = tempSet.iterator();

            int numberOfMatches = tempSet.size();
            int progress = 0;
            waitingHandler.setMaxSecondaryProgressValue(numberOfMatches);
            idReport = false;
            ArrayList<Integer> charges = new ArrayList<Integer>();
            double maxErrorPpm = 0, maxErrorDa = 0;

            while (matchIt.hasNext()) {

                SpectrumMatch match = matchIt.next();
                nPSMs++;
                nSecondary += match.getAllAssumptions().size() - 1;

                PeptideAssumption firstHit = match.getFirstHit(searchEngine);
                String spectrumKey = match.getKey();
                String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                String fileName = Spectrum.getSpectrumFile(spectrumKey);
                if (spectrumFactory.getSpectrumFileFromIdName(fileName) != null) {
                    fileName = spectrumFactory.getSpectrumFileFromIdName(fileName).getName();
                    match.setKey(Spectrum.getSpectrumKey(fileName, spectrumTitle));
                    spectrumKey = match.getKey();
                }
                if (spectrumFactory.fileLoaded(fileName)
                        && !spectrumFactory.spectrumLoaded(spectrumKey)) {
                    String oldTitle = Spectrum.getSpectrumTitle(spectrumKey);
                    spectrumTitle = match.getSpectrumNumber() + "";
                    spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                    match.setKey(spectrumKey);
                    if (spectrumFactory.fileLoaded(fileName)
                            && !spectrumFactory.spectrumLoaded(spectrumKey)) {
                        waitingHandler.appendReport("Spectrum " + oldTitle + " number " + spectrumTitle + " not found in file " + fileName + ".", true, true);
                        waitingHandler.setRunCanceled();
                        return;
                    }
                }

                if (!mgfUsed.contains(fileName)) {
                    if (spectrumFiles.containsKey(fileName)) {
                        importSpectra(fileName, searchParameters);
                        waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                        waitingHandler.setMaxSecondaryProgressValue(numberOfMatches);
                        mgfUsed.add(fileName);
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

                goodFirstHit = false;
                ArrayList<PeptideAssumption> allAssumptions = match.getAllAssumptions(searchEngine).get(firstHit.getEValue());

                for (PeptideAssumption assumption : allAssumptions) {
                    if (idFilter.validateId(assumption, spectrumKey)) {
                        if (!goodFirstHit) {
                            match.setFirstHit(searchEngine, assumption);
                        }
                        double precursorMz = spectrumFactory.getPrecursor(spectrumKey).getMz();
                        goodFirstHit = true;
                        double error = Math.abs(assumption.getDeltaMass(precursorMz, true));

                        if (error > maxErrorPpm) {
                            maxErrorPpm = error;
                        }

                        error = Math.abs(assumption.getDeltaMass(precursorMz, false));

                        if (error > maxErrorDa) {
                            maxErrorDa = error;
                        }

                        int currentCharge = assumption.getIdentificationCharge().value;

                        if (!charges.contains(currentCharge)) {
                            charges.add(currentCharge);
                        }

                        Peptide peptide = assumption.getPeptide();
                        String peptideSequence = peptide.getSequence();
                        if (searchEngine == Advocate.XTANDEM) {
                            ArrayList<String> proteins = getProteins(peptideSequence, waitingHandler);
                            if (!proteins.isEmpty()) {
                                peptide.setParentProteins(proteins);
                            }
                        }

                        for (String protein : peptide.getParentProteins()) {
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
                    }
                }

                if (!goodFirstHit) {
                    matchIt.remove();
                } else {
                    // change the search engine modifications into expected modifications
                    for (PeptideAssumption assumptions : match.getAllAssumptions()) {
                        Peptide peptide = assumptions.getPeptide();
                        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                            String expectedPTM, sePTM = modMatch.getTheoreticPtm();
                            if (sePTM.equals(PTMFactory.unknownPTM.getName())) {
                                if (!unknown) {
                                    waitingHandler.appendReport("An unknown modification was encountered when parsing PSM " + spectrumTitle + " in file " + fileName + " and might impair further processing."
                                            + "\nPlease make sure that all modifications are loaded in the search parameters and reload the data.", true, true);
                                    unknown = true;
                                }
                            } else {
                                ArrayList<String> expectedNames = ptmFactory.getExpectedPTMs(searchParameters.getModificationProfile(), peptide, sePTM);
                                if (expectedNames.isEmpty()) {
                                    String[] parsedName = sePTM.split("@");
                                    double seMass = 0;
                                    try {
                                        seMass = new Double(parsedName[0]);
                                    } catch (Exception e) {
                                        throw new IllegalArgumentException("Impossible to parse " + sePTM + " as an X!Tandem modification.\n"
                                                + "Error encountered in spectrum " + spectrumTitle + " in file " + fileName + ".");
                                    }
                                    expectedNames = ptmFactory.getExpectedPTMs(searchParameters.getModificationProfile(), peptide, seMass, ptmMassTolerance);
                                }

                                if (!expectedNames.isEmpty()) {
                                    expectedPTM = expectedNames.get(0);
                                } else {
                                    expectedPTM = PTMFactory.unknownPTM.getName();
                                }
                                modMatch.setTheoreticPtm(expectedPTM);
                            }
                        }
                    }

                    if (idFilter.validateId(firstHit, spectrumKey)) {
                        inputMap.addEntry(searchEngine, firstHit.getEValue(), firstHit.isDecoy());
                        identification.addSpectrumMatch(match);
                        nRetained++;
                    }
                }

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                waitingHandler.setSecondaryProgressValue(++progress);
            }
            metrics.addFoundCharges(charges);
            if (maxErrorDa > metrics.getMaxPrecursorErrorDa()) {
                metrics.setMaxPrecursorErrorDa(maxErrorDa);
            }
            if (maxErrorPpm > metrics.getMaxPrecursorErrorPpm()) {
                metrics.setMaxPrecursorErrorPpm(maxErrorPpm);
            }


            // Free at least 1GB for the next parser if not anymore available
            // (not elegant so most likely not optimal)
            if (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() < 1073741824) {
                System.gc();
                if (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() < 1073741824) {
                    waitingHandler.appendReport("Reducing Memory Consumption.", true, true);
                    waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                    double share = ((double) 1073741824) / Runtime.getRuntime().totalMemory();
                    share = Math.min(share, 1);
                    peptideShaker.getCache().reduceMemoryConsumption(share, waitingHandler);
                    System.gc();
                    waitingHandler.setSecondaryProgressDialogIndeterminate(true);
                }
            }

            waitingHandler.increaseProgressValue();
        }

        /**
         * Verify that the spectra are imported and imports spectra from the
         * desired spectrum file if necessary.
         *
         * @param targetFileName the spectrum file
         * @param searchParameters the search parameters
         */
        public void importSpectra(String targetFileName, SearchParameters searchParameters) {

            File spectrumFile = spectrumFiles.get(targetFileName);

            try {
                waitingHandler.appendReport("Importing " + targetFileName, true, true);
                waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                waitingHandler.resetSecondaryProgressBar();
                spectrumFactory.addSpectra(spectrumFile, waitingHandler);
                waitingHandler.resetSecondaryProgressBar();
                waitingHandler.increaseProgressValue();
                waitingHandler.appendReport(targetFileName + " imported.", true, true);
            } catch (Exception e) {
                waitingHandler.appendReport("Spectrum files import failed when trying to import " + targetFileName + ".", true, true);
                e.printStackTrace();
            }
        }
    }

    /**
     * Needs to be set to TRUE when working in CLI modus to avoid the
     * SwingWorker backgroundthread.
     *
     * @param status CLI mode : TRUE GUI mode : FALSE
     */
    public static void setCLIMode(boolean status) {
        boolCLI = status;
    }

    /**
     * Returns whether the FileImporter is working in CLI modus.
     *
     * @return true if the file importer is working in command line mode
     */
    public static boolean isCLIMode() {
        return boolCLI;
    }
}
