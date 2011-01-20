package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.identification.Identification;
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

    private ProteomicAnalysis proteomicAnalysis;
    private boolean onlyIdentified;
    private boolean runFinished = false;
    private PeptideShaker parent;

    public SpectrumImporter(PeptideShaker parent, ProteomicAnalysis proteomicAnalysis, boolean onlyIdentified) {
        this.parent = parent;
        this.proteomicAnalysis = proteomicAnalysis;
        this.onlyIdentified = onlyIdentified;
    }

    public void importSpectra(WaitingDialog waitingDialog, ArrayList<File> spectrumFiles) {
        SpectrumProcessor spectrumProcessor = new SpectrumProcessor(waitingDialog, spectrumFiles);
        spectrumProcessor.execute();
    }

    public boolean isRunFinished() {
        return runFinished;
    }

    /**
     * Worker which processes spectra and gives feedback to the user.
     */
    private class SpectrumProcessor extends SwingWorker {

        private ArrayList<File> spectrumFiles;
        private WaitingDialog waitingDialog;

        public SpectrumProcessor(WaitingDialog waitingDialog, ArrayList<File> spectrumFiles) {
            this.waitingDialog = waitingDialog;
            this.spectrumFiles = spectrumFiles;
        }

        @Override
        protected Object doInBackground() throws Exception {
            String fileName = "";
            try {
                if (parent.needQueue()) {
                    queue();
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

        private synchronized void queue() throws InterruptedException {
            parent.queue(this);
            wait();
        }
    }
}
