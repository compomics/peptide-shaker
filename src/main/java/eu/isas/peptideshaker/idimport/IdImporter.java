package eu.isas.peptideshaker.idimport;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fdrestimation.InputMap;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
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
    private PeptideShaker identificationShaker;
    /**
     * The identification processed
     */
    private Identification identification;
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
     * @param identification        the identification to process
     * @param idFilter              The identification filter to use
     */
    public IdImporter(PeptideShaker identificationShaker, WaitingDialog waitingDialog, Identification identification, IdFilter idFilter) {
        this.identificationShaker = identificationShaker;
        this.waitingDialog = waitingDialog;
        this.identification = identification;
        this.idFilter = idFilter;
    }

    /**
     * Constructor for an import without filtering
     * @param identificationShaker
     * @param waitingDialog
     * @param identification
     */
    public IdImporter(PeptideShaker identificationShaker, WaitingDialog waitingDialog, Identification identification) {
        this.identificationShaker = identificationShaker;
        this.waitingDialog = waitingDialog;
        this.identification = identification;
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
        waitingDialog.setVisible(true);
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
                InputMap inputMap = new InputMap();
                for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                    for (int searchEngine : spectrumMatch.getAdvocates()) {
                        inputMap.addEntry(searchEngine, spectrumMatch.getFirstHit(searchEngine).getEValue(), spectrumMatch.getFirstHit(searchEngine).isDecoy());
                    }
                }
                identificationShaker.processIdentifications(inputMap, waitingDialog);
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

            try {
                waitingDialog.appendReport("Reading identification files.");
                InputMap inputMap = new InputMap();

                HashSet<SpectrumMatch> tempSet;

                for (File idFile : idFiles) {

                    waitingDialog.appendReport("Reading file: " + idFile.getName());
                    waitingDialog.appendReportNewLineNoDate();

                    waitingDialog.appendReportProgressCounter();
                    int searchEngine = readerFactory.getSearchEngine(idFile);

                    waitingDialog.appendReportProgressCounter();
                    IdfileReader fileReader = readerFactory.getFileReader(idFile);

                    waitingDialog.appendReportProgressCounter();
                    tempSet = fileReader.getAllSpectrumMatches();

                    waitingDialog.appendReportProgressCounter();
                    Iterator<SpectrumMatch> matchIt = tempSet.iterator();
                    SpectrumMatch match;

                    waitingDialog.appendReportProgressCounter();
                    
                    while (matchIt.hasNext()) {

                        if (nTotal % 100 == 0) {
                            waitingDialog.appendReportProgressCounter();
                        }

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

                    waitingDialog.appendReportProgressCounter();
                    waitingDialog.appendReportEndLine();
                }

                if (nRetained == 0) {
                    waitingDialog.appendReport("No identification retained.");
                    waitingDialog.setRunFinished();
                    return 1;
                }

                waitingDialog.appendReport("Identification file(s) import completed. "
                        + nTotal + " identifications imported, " + nRetained + " identifications retained.");

                identificationShaker.processIdentifications(inputMap, waitingDialog);

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
