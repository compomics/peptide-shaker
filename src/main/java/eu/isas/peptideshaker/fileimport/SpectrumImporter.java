package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.File;
import java.util.ArrayList;
import javax.swing.SwingWorker;

/**
 * This class will import spectra from the supported files
 *
 * @author Marc
 */
public class SpectrumImporter {

    /**
     * The investigated proteomicAnalysis
     */
    private ProteomicAnalysis proteomicAnalysis;
    /**
     * Boolean indicating whether only identified spectra should be imported
     */
    private boolean onlyIdentified;
    /**
     * Boolean indicating whether the run is completed
     */
    private boolean runFinished = false;
    /**
     * The parent peptide shaker
     */
    private PeptideShaker parent;

    /**
     * Constructor for the spectrum importer
     * @param parent            The parent peptide shaker
     * @param proteomicAnalysis The current proteomic analysis
     * @param onlyIdentified    Boolean indicating whether only identified spectra should be imported
     */
    public SpectrumImporter(PeptideShaker parent, ProteomicAnalysis proteomicAnalysis, boolean onlyIdentified) {
        this.parent = parent;
        this.proteomicAnalysis = proteomicAnalysis;
        this.onlyIdentified = onlyIdentified;
    }

    /**
     * Imports spectra from various spectrum files
     * @param waitingDialog Dialog displaying feedback to the user
     * @param spectrumFiles The spectrum files
     */
    public void importSpectra(WaitingDialog waitingDialog, ArrayList<File> spectrumFiles) {
        SpectrumProcessor spectrumProcessor = new SpectrumProcessor(waitingDialog, spectrumFiles);
        spectrumProcessor.execute();
    }

    /**
     * Returns a boolean indicating whether the run is finished
     * @return a boolean indicating whether the run is finished
     */
    public boolean isRunFinished() {
        return runFinished;
    }

    /**
     * Worker which processes spectra and gives feedback to the user.
     */
    private class SpectrumProcessor extends SwingWorker {

        /**
         * List of spectrum files
         */
        private ArrayList<File> spectrumFiles;
        /**
         * Dialog displaying feedback to the user
         */
        private WaitingDialog waitingDialog;

        /**
         * Constructor for the worker
         * @param waitingDialog Dialog displaying feedback to the user
         * @param spectrumFiles List of spectrum files
         */
        public SpectrumProcessor(WaitingDialog waitingDialog, ArrayList<File> spectrumFiles) {
            this.waitingDialog = waitingDialog;
            this.spectrumFiles = spectrumFiles;
        }

        @Override
        protected Object doInBackground() throws Exception {
            String fileName = "";
            try {

//                does not seem to work...
//                if (parent.needQueue()) {
//                    queue();
//                }

                // @TODO: has to be a better way of doing this...
                // wait until the identifications are imported
                while (parent.needQueue()) {
                    int counter=0;

                    for (int i=0; i<100000; i++) {
                        counter++;
                    }
                }

                waitingDialog.appendReport("Importing spectra.");
                proteomicAnalysis.clearSpectrumCollection();
                for (File spectrumFile : spectrumFiles) {
                    fileName = spectrumFile.getName();
                    waitingDialog.appendReport("Loading " + fileName);
                    if (onlyIdentified) {
                        proteomicAnalysis.getSpectrumCollection().addIdentifiedSpectra(spectrumFile, (Ms2Identification) proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION));
                    } else {
                        proteomicAnalysis.getSpectrumCollection().addSpectra(spectrumFile);
                    }
                }
            } catch (InterruptedException e) {
                waitingDialog.appendReport("Synchronization issue between identification and spectra import. Import failed.");
                waitingDialog.setRunCanceled();
                e.printStackTrace();
                return 1;
            } catch (Exception e) {
                waitingDialog.appendReport("Spectrum files import failed when trying to import " + fileName + ".");
                proteomicAnalysis.clearSpectrumCollection();
                e.printStackTrace();
                return 1;
            }
            waitingDialog.appendReport("Spectra import completed.");
            runFinished = true;
            parent.setRunFinished(waitingDialog);
            return 0;
        }

        /**
         * Method used to synchronize the workers
         * @throws InterruptedException exception thrown by the wait() method
         */
        private synchronized void queue() throws InterruptedException {
            parent.queue(this);
            wait();
        }
    }
}
