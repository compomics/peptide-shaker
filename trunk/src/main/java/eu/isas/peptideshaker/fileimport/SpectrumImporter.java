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
     * The parent peptide shaker
     */
    private PeptideShaker parent;

    /**
     * Constructor for the spectrum importer
     * @param parent            The parent peptide shaker
     * @param proteomicAnalysis The current proteomic analysis
     */
    public SpectrumImporter(PeptideShaker parent, ProteomicAnalysis proteomicAnalysis) {
        this.parent = parent;
        this.proteomicAnalysis = proteomicAnalysis;
    }

    /**
     * Imports spectra from various spectrum files
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
            parent.setRunFinished(waitingDialog);
    }
}
