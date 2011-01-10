package eu.isas.peptideshaker.idimport;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdfileReader;
import com.compomics.util.experiment.identification.IdfileReaderFactory;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
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
 * @TODO: JavaDoc missing
 *
 * @author  Marc Vaudel
 * @author  Harald Barsnes
 */
public class IdImporter {

    private PeptideShakerGUI peptideShaker;
    private Identification identification;
    private IdFilter idFilter;
    private WaitingDialog waitingDialog;
    private final String MODIFICATION_FILE = "conf/peptideshaker_mods.xml";
    private final String USER_MODIFICATION_FILE = "conf/peptideshaker_usermods.xml";
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * @TODO: JavaDoc missing
     *
     * @param peptideShaker
     * @param waitingDialog
     * @param identification
     * @param idFilter
     */
    public IdImporter(PeptideShakerGUI peptideShaker, WaitingDialog waitingDialog, Identification identification, IdFilter idFilter) {
        this.peptideShaker = peptideShaker;
        this.waitingDialog = waitingDialog;
        this.identification = identification;
        this.idFilter = idFilter;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param peptideShaker
     * @param waitingDialog
     * @param identification
     */
    public IdImporter(PeptideShakerGUI peptideShaker, WaitingDialog waitingDialog, Identification identification) {
        this.peptideShaker = peptideShaker;
        this.waitingDialog = waitingDialog;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param idFiles
     */
    public void importFiles(ArrayList<File> idFiles) {
        IdProcessorFromFile idProcessor = new IdProcessorFromFile(idFiles);
        idProcessor.execute();

        waitingDialog.appendReport("Importing identifications.");
        waitingDialog.setVisible(true);
    }

    /**
     * @TODO: JavaDoc missing
     */
    public void importIdentifications() {
        IdProcessor idProcessor = new IdProcessor();
        idProcessor.execute();

        waitingDialog.appendReport("Importing identifications.");
        waitingDialog.setVisible(true);
    }

    /**
     * @TODO: JavaDoc missing
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
                peptideShaker.processIdentifications(inputMap, waitingDialog, identification);
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
     * @TODO: JavaDoc missing
     */
    private class IdProcessorFromFile extends SwingWorker {

        private IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
        private ArrayList<File> idFiles;

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

                peptideShaker.processIdentifications(inputMap, waitingDialog, identification);

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
