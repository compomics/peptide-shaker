package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fdrestimation.InputMap;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.File;
import java.util.ArrayList;
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
public class IdImporter {

    /**
     * The class which will load the information into the various maps and do the associated calculations
     */
    private PeptideShaker peptideShaker;
    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
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
     * Constructor for the importer
     *
     * @param identificationShaker  the identification shaker which will load the data into the maps and do the preliminary calculations
     * @param waitingDialog         A dialog to display feedback to the user
     * @param experiment            the experiment conducted
     * @param sample                the sample analyzed
     * @param replicateNumber       the replicate number
     * @param idFilter              The identification filter to use
     */
    public IdImporter(PeptideShaker identificationShaker, WaitingDialog waitingDialog, MsExperiment experiment, Sample sample, int replicateNumber, IdFilter idFilter) {
        this.peptideShaker = identificationShaker;
        this.waitingDialog = waitingDialog;
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.idFilter = idFilter;
    }

    /**
     * Constructor for an import without filtering
     * @param identificationShaker
     * @param waitingDialog
     * @param experiment            the experiment conducted
     * @param sample                the sample analyzed
     * @param replicateNumber       the replicate number
     */
    public IdImporter(PeptideShaker identificationShaker, WaitingDialog waitingDialog, MsExperiment experiment, Sample sample, int replicateNumber) {
        this.peptideShaker = identificationShaker;
        this.waitingDialog = waitingDialog;
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
    }

    /**
     * Imports the identification from files
     *
     * @param idFiles the identification files to import the Ids from
     */
    public void importFiles(ArrayList<File> idFiles) {
        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles);
        idProcessor.execute();

        waitingDialog.appendReport("Importing identifications.");
    }

    /**
     * Imports the identifications from another stream
     */
    public void importIdentifications() {
        IdProcessor idProcessor = new IdProcessor();
        idProcessor.execute();

        waitingDialog.appendReport("Importing identifications.");
        waitingDialog.setVisible(true);
    }

    /**
     * Worker which processes identifications and gives feedback to the user.
     */
    private class IdProcessor extends SwingWorker {

        @Override
        protected Object doInBackground() throws Exception {
            try {
                Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
                InputMap inputMap = new InputMap();
                for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                    for (int searchEngine : spectrumMatch.getAdvocates()) {
                        inputMap.addEntry(searchEngine, spectrumMatch.getFirstHit(searchEngine).getEValue(), spectrumMatch.getFirstHit(searchEngine).isDecoy());
                    }
                }
                peptideShaker.processIdentifications(inputMap, waitingDialog);
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while loading the identifications:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCanceled();
                e.printStackTrace();
            }
            return 0;
        }
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
         * Constructor of the worker
         * @param idFiles ArrayList containing the identification files
         */
        public IdProcessorFromFile(ArrayList<File> idFiles) {
            this.idFiles = idFiles;
            try {
                ptmFactory.importModifications(new File(MODIFICATION_FILE));
            } catch (Exception e) {
                waitingDialog.appendReport("Failed importing modifications from " + MODIFICATION_FILE);
                waitingDialog.setRunCanceled();
            }
            try {
                ptmFactory.importModifications(new File(USER_MODIFICATION_FILE));
            } catch (Exception e) {
                waitingDialog.appendReport("Failed importing modifications from " + USER_MODIFICATION_FILE);
                waitingDialog.setRunCanceled();
            }
        }

        @Override
        protected Object doInBackground() throws Exception {

            int nTotal = 0;
            int nRetained = 0;

            ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
            Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            try {
                waitingDialog.appendReport("Reading identification files.");
                InputMap inputMap = new InputMap();

                HashSet<SpectrumMatch> tempSet;
                IdfileReader fileReader;

                for (File idFile : idFiles) {

                    waitingDialog.appendReport("Reading file: " + idFile.getName());

                    int searchEngine = readerFactory.getSearchEngine(idFile);

                    fileReader = readerFactory.getFileReader(idFile, proteomicAnalysis.getSpectrumCollection());

                    tempSet = fileReader.getAllSpectrumMatches();

                    Iterator<SpectrumMatch> matchIt = tempSet.iterator();
                    SpectrumMatch match;

                    while (matchIt.hasNext()) {

                        match = matchIt.next();
                        nTotal++;
                        if (!idFilter.validateId(match.getFirstHit(searchEngine))) {
                            matchIt.remove();
                        } else {
                            inputMap.addEntry(searchEngine, match.getFirstHit(searchEngine).getEValue(), match.getFirstHit(searchEngine).isDecoy());
                            identification.addSpectrumMatch(match);
                            nRetained++;
                        }
                        if (waitingDialog.isRunCanceled()) {
                            return 1;
                        }
                    }

                    waitingDialog.increaseProgressValue();
                }

                if (nRetained == 0) {
                    waitingDialog.appendReport("No identifications retained.");
                    waitingDialog.setRunCanceled();
                    waitingDialog.setRunFinished();
                    return 1;
                }

                waitingDialog.appendReport("Identification file(s) import completed. "
                        + nTotal + " identifications imported, " + nRetained + " identifications retained.");

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
}
