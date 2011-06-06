package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.protein.Enzyme;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fdrestimation.InputMap;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * This class is responsible for the import of identifications
 *
 * @author  Marc Vaudel
 * @author  Harald Barsnes
 */
public class FileImporter {
  
    /**
     * The class which will load the information into the various maps and do the associated calculations
     */
    private PeptideShaker peptideShaker;
    /**
     * The current proteomicAnalysis
     */
    private ProteomicAnalysis proteomicAnalysis;
    /**
     * The identification filter to use
     */
    private IdFilter idFilter;
    /**
     * A dialog to display feedback to the user
     */
    private WaitingDialog waitingDialog;
    /**
     * The location of the modification file
     */
    private final String MODIFICATION_FILE = "conf/peptideshaker_mods.xml";
    /**
     * The location of the user modification file
     */
    private final String USER_MODIFICATION_FILE = "conf/peptideshaker_usermods.xml";
    /**
     * The modification factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * TODO: JavaDoc missing...
     */
    private HashMap<String, ArrayList<String>> sequences = new HashMap<String, ArrayList<String>>();
    /**
     * If true, all the sequence to protein mapping is redone, i.e., the search 
     * engines' protein inference is not trusted.
     */
    private boolean redoProteinInference = true;

    /**
     * Constructor for the importer
     *
     * @param identificationShaker  the identification shaker which will load the data into the maps and do the preliminary calculations
     * @param waitingDialog         A dialog to display feedback to the user
     * @param proteomicAnalysis     The current proteomic analysis
     * @param idFilter              The identification filter to use
     */
    public FileImporter(PeptideShaker identificationShaker, WaitingDialog waitingDialog, ProteomicAnalysis proteomicAnalysis, IdFilter idFilter) {
        this.peptideShaker = identificationShaker;
        this.waitingDialog = waitingDialog;
        this.proteomicAnalysis = proteomicAnalysis;
        this.idFilter = idFilter;
    }

    /**
     * Constructor for an import without filtering
     * @param identificationShaker  the parent identification shaker
     * @param waitingDialog         a dialog to give feedback to the user
     * @param proteomicAnalysis     the current proteomic analysis
     */
    public FileImporter(PeptideShaker identificationShaker, WaitingDialog waitingDialog, ProteomicAnalysis proteomicAnalysis) {
        this.peptideShaker = identificationShaker;
        this.waitingDialog = waitingDialog;
        this.proteomicAnalysis = proteomicAnalysis;
    }

    /**
     * Imports the identification from files.
     *
     * @param idFiles           the identification files to import the Ids from
     * @param spectrumFiles     the files where the corresponding spectra can be imported
     * @param fastaFile         the FASTA file to use
     * @param enzyme            the enzyme to use
     */
    public void importFiles(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, File fastaFile, Enzyme enzyme) { 
        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles, spectrumFiles, fastaFile, enzyme, idFilter);
        idProcessor.execute();
    }

    /**
     * Import spectra from spectrum files.
     * 
     * @param spectrumFiles
     */
    public void importFiles(ArrayList<File> spectrumFiles) {
        SpectrumProcessor spectrumProcessor = new SpectrumProcessor(spectrumFiles);
        spectrumProcessor.execute();
    }

    /**
     * Imports sequences from a fasta file
     *
     * @param waitingDialog     Dialog displaying feedback to the user
     * @param proteomicAnalysis The proteomic analysis to attach the database to
     * @param fastaFile         FASTA file to process
     * @param enzyme            the enzyme to use
     * @param idFilter          the identification filter
     */
    public void importSequences(WaitingDialog waitingDialog, ProteomicAnalysis proteomicAnalysis, File fastaFile, Enzyme enzyme, IdFilter idFilter) {

        try {
            waitingDialog.appendReport("Importing sequences from " + fastaFile.getName() + ".");
            SequenceDataBase db = proteomicAnalysis.getSequenceDataBase();
            db.importDataBase(fastaFile, enzyme, idFilter.getMinPeptideLength(), idFilter.getMaxPeptideLength());
            waitingDialog.appendReport("FASTA file import completed.");
            waitingDialog.increaseProgressValue();
        } catch (FileNotFoundException e) {
            waitingDialog.appendReport("File " + fastaFile + " was not found. Please select a different FASTA file.");
            e.printStackTrace();
        } catch (Exception e) {
            waitingDialog.appendReport("An error occured while loading " + fastaFile + ". Please select a different FASTA file.");
            e.printStackTrace();
        }
    }

    /**
     * Imports spectra from various spectrum files.
     * 
     * @param waitingDialog Dialog displaying feedback to the user
     * @param spectrumFiles The spectrum files
     */
    public void importSpectra(WaitingDialog waitingDialog, ArrayList<File> spectrumFiles) {

        String fileName = "";

        try {
            waitingDialog.appendReport("Importing spectra.");
            proteomicAnalysis.clearSpectrumCollection();

            for (File spectrumFile : spectrumFiles) {
                fileName = spectrumFile.getName();
                waitingDialog.appendReport("Loading " + fileName);
                proteomicAnalysis.getSpectrumCollection().addIdentifiedSpectra(spectrumFile, (Ms2Identification) proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION));
                waitingDialog.increaseProgressValue();
            }
        } catch (InterruptedException e) {
            waitingDialog.appendReport("Synchronization issue between identification and spectra import. Import failed.");
            waitingDialog.setRunCanceled();
            e.printStackTrace();
        } catch (Exception e) {
            waitingDialog.appendReport("Spectrum files import failed when trying to import " + fileName + ".");
            proteomicAnalysis.clearSpectrumCollection();
            e.printStackTrace();
        }
        waitingDialog.appendReport("Spectra import completed.");
    }

    /**
     * Returns the list of proteins which contain in their sequence the given peptide sequence.
     * 
     * @param sequence  the tested peptide sequence
     * @return          a list of corresponding proteins found in the database
     */
    private ArrayList<String> getProteins(String sequence, boolean useProteinMap) {

        if (sequences.keySet().contains(sequence)) {
            return sequences.get(sequence);
        }

        ArrayList<String> result = new ArrayList<String>();
        SequenceDataBase db = proteomicAnalysis.getSequenceDataBase();
        
        if (useProteinMap) {
        
            ArrayList<String> temp = db.getSequenceToProteinMap().get(sequence);

            if (temp != null) {
                result = temp;
            }
        } else {
            
            // note: this is the old slow option, not recommended

            for (String proteinKey : db.getProteinList()) {
                
                if (db.getProtein(proteinKey).getSequence().contains(sequence)) {
                    result.add(proteinKey);
                }
            }
        }
        
        sequences.put(sequence, result);
        return result;
    }

    /**
     * Worker which loads identification from a file and processes them while giving feedback to the user.
     */
    private class IdProcessorFromFile extends SwingWorker {

        /**
         * The identification file reader factory of compomics utilities
         */
        private IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
        /**
         * The list of identification files
         */
        private ArrayList<File> idFiles;
        /**
         * The fasta file
         */
        private File fastaFile;
        /**
         * A list of spectrum files (can be empty, no spectrum will be imported)
         */
        private HashMap<String, File> spectrumFiles;
        /**
         * The enzyme used to cleave the FASTA sequences to create a sequence 
         * to protein map.
         */
        private Enzyme enzyme;
        /**
         * The identification filter.
         */
        private IdFilter idFilter;

        /**
         * Constructor of the worker
         * @param idFiles ArrayList containing the identification files
         */
        public IdProcessorFromFile(ArrayList<File> idFiles, ArrayList<File> spectrumFiles, File fastaFile, Enzyme enzyme, IdFilter idFilter) {

            this.idFiles = idFiles;
            this.spectrumFiles = new HashMap<String, File>();
            this.fastaFile = fastaFile;
            this.enzyme = enzyme;
            this.idFilter = idFilter;

            for (File file : spectrumFiles) {
                this.spectrumFiles.put(file.getName(), file);
            }

            try {
                ptmFactory.importModifications(new File(MODIFICATION_FILE));
            } catch (Exception e) {
                waitingDialog.appendReport("Failed importing modifications from " + MODIFICATION_FILE);
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            }

            try {
                ptmFactory.importModifications(new File(USER_MODIFICATION_FILE));
            } catch (Exception e) {
                waitingDialog.appendReport("Failed importing modifications from " + USER_MODIFICATION_FILE);
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground() throws Exception {

            int nTotal = 0;
            int nRetained = 0;

            Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            importSequences(waitingDialog, proteomicAnalysis, fastaFile, enzyme, idFilter);

            try {
                waitingDialog.appendReport("Reading identification files.");
                InputMap inputMap = new InputMap();

                ArrayList<String> mgfNeeded = new ArrayList<String>();
                
                for (File idFile : idFiles) {

                    waitingDialog.appendReport("Reading file: " + idFile.getName());

                    int searchEngine = readerFactory.getSearchEngine(idFile);
                    IdfileReader fileReader = readerFactory.getFileReader(idFile, proteomicAnalysis.getSpectrumCollection());
                    HashSet<SpectrumMatch> tempSet = fileReader.getAllSpectrumMatches();

                    Iterator<SpectrumMatch> matchIt = tempSet.iterator();

                    while (matchIt.hasNext()) {

                        SpectrumMatch match = matchIt.next();
                        nTotal++;
                        
                        PeptideAssumption firstHit = match.getFirstHit(searchEngine);

                        if (!idFilter.validateId(firstHit)) {
                            matchIt.remove();
                        } else {
                            inputMap.addEntry(searchEngine, firstHit.getEValue(), firstHit.isDecoy());

                            // re-do the sequence to protein mapping
                            if (redoProteinInference) {

                                Peptide peptide = firstHit.getPeptide();
                                ArrayList<Protein> proteins = new ArrayList<Protein>();
                                
                                for (String proteinKey : getProteins(peptide.getSequence(), enzyme != null)) {
                                    proteins.add(new Protein(proteinKey, proteinKey.contains("REV")));
                                }
                                
                                if (!proteins.isEmpty()) {
                                    peptide.setParentProteins(proteins);
                                }
                            }

                            identification.addSpectrumMatch(match);
                            String mgfName = Spectrum.getSpectrumFile(match.getKey());

                            if (!mgfNeeded.contains(mgfName)) {
                                mgfNeeded.add(mgfName);
                            }

                            nRetained++;
                        }

                        if (waitingDialog.isRunCanceled()) {
                            return 1;
                        }
                    }

                    waitingDialog.increaseProgressValue();
                }
                
                // clear the sequence to peptide map as it is no longer needed
                proteomicAnalysis.getSequenceDataBase().emptySequenceToProteinMap();

                if (nRetained == 0) {
                    waitingDialog.appendReport("No identifications retained.");
                    waitingDialog.setRunFinished();
                    return 1;
                }

                waitingDialog.appendReport("Identification file(s) import completed. "
                        + nTotal + " identifications imported, " + nRetained + " identifications retained.");

                ArrayList<String> mgfMissing = new ArrayList<String>();
                ArrayList<String> mgfNames = new ArrayList<String>(spectrumFiles.keySet());
                ArrayList<File> mgfImported = new ArrayList<File>();

                for (String mgfFile : mgfNeeded) {
                    if (!mgfNames.contains(mgfFile)) {
                        mgfMissing.add(mgfFile);
                    } else {
                        mgfImported.add(spectrumFiles.get(mgfFile));
                    }
                }

                if (mgfMissing.isEmpty()) {
                    waitingDialog.increaseProgressValue(mgfNames.size() - mgfImported.size());
                    importSpectra(waitingDialog, mgfImported);
                } else {
                    waitingDialog.increaseProgressValue(mgfNames.size());
                }

                peptideShaker.processIdentifications(inputMap, waitingDialog);

            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while loading the identification files:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            } catch (OutOfMemoryError error) {
                Runtime.getRuntime().gc();
                waitingDialog.appendReportEndLine();
                waitingDialog.appendReport("Ran out of memory!");
                waitingDialog.setRunCanceled();
                JOptionPane.showMessageDialog(null,
                        "The task used up all the available memory and had to be stopped.\n"
                        + "Memory boundaries are set in ../conf/JavaOptions.txt.",
                        "Out Of Memory Error",
                        JOptionPane.ERROR_MESSAGE);

                System.out.println("Ran out of memory!");
                error.printStackTrace();
            }

            return 0;
        }
    }

    /**
     * Worker which loads spectra from files assuming that ids are already loaded them while giving feedback to the user.
     */
    private class SpectrumProcessor extends SwingWorker {

        /**
         * A list of spectrum files (can be empty, no spectrum will be imported)
         */
        private HashMap<String, File> spectrumFiles;

        /**
         * Constructor of the worker
         * @param spectrumFiles ArrayList containing the spectrum files
         */
        public SpectrumProcessor(ArrayList<File> spectrumFiles) {

            this.spectrumFiles = new HashMap<String, File>();

            for (File file : spectrumFiles) {
                this.spectrumFiles.put(file.getName(), file);
            }

            try {
                ptmFactory.importModifications(new File(MODIFICATION_FILE));
            } catch (Exception e) {
                waitingDialog.appendReport("Failed importing modifications from " + MODIFICATION_FILE);
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            }

            try {
                ptmFactory.importModifications(new File(USER_MODIFICATION_FILE));
            } catch (Exception e) {
                waitingDialog.appendReport("Failed importing modifications from " + USER_MODIFICATION_FILE);
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground() throws Exception {

            Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

            try {
                ArrayList<String> mgfNeeded = new ArrayList<String>();
                String newFile;

                for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                    newFile = Spectrum.getSpectrumFile(spectrumMatch.getKey());
                    if (!mgfNeeded.contains(newFile)) {
                        mgfNeeded.add(newFile);
                    }
                }

                waitingDialog.increaseProgressValue();

                ArrayList<String> mgfMissing = new ArrayList<String>();
                ArrayList<String> mgfNames = new ArrayList<String>(spectrumFiles.keySet());
                ArrayList<File> mgfImported = new ArrayList<File>();

                for (String mgfFile : mgfNeeded) {
                    if (!mgfNames.contains(mgfFile)) {
                        mgfMissing.add(mgfFile);
                    } else {
                        mgfImported.add(spectrumFiles.get(mgfFile));
                    }
                }

                if (mgfMissing.isEmpty()) {
                    for (int i = mgfImported.size(); i < mgfNames.size(); i++) {
                        waitingDialog.increaseProgressValue();
                    }
                    importSpectra(waitingDialog, mgfImported);
                } else {
                    for (int i = 0; i < mgfNames.size(); i++) {
                        waitingDialog.increaseProgressValue();
                    }
                }

                waitingDialog.appendReport("File import finished.\n\n");
                waitingDialog.setRunFinished();

            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while loading the identification files:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            } catch (OutOfMemoryError error) {
                Runtime.getRuntime().gc();
                waitingDialog.appendReportEndLine();
                waitingDialog.appendReport("Ran out of memory!");
                waitingDialog.setRunCanceled();
                JOptionPane.showMessageDialog(null,
                        "The task used up all the available memory and had to be stopped.\n"
                        + "Memory boundaries are set in ../conf/JavaOptions.txt.",
                        "Out Of Memory Error",
                        JOptionPane.ERROR_MESSAGE);

                System.out.println("Ran out of memory!");
                error.printStackTrace();
            }

            return 0;
        }
    }
}
