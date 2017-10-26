package eu.isas.peptideshaker.recalibration;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Peak;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.IdentificationParameters;
import java.io.IOException;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class recalibrates spectra.
 *
 * @author Marc Vaudel
 */
public class SpectrumRecalibrator {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * Map of the runs errors.
     */
    private final HashMap<String, RunMzDeviation> runMzDeviationMap = new HashMap<>();

    /**
     * Constructor.
     */
    public SpectrumRecalibrator() {
    }

    /**
     * Clears the loaded error statistics for the given file name in order to
     * save memory.
     *
     * @param spectrumFileName the spectrum file name
     */
    public void clearErrors(String spectrumFileName) {
        runMzDeviationMap.remove(spectrumFileName);
    }

    /**
     * Returns the mz deviation statistics class for the spectrum file of
     * interest. Null if not estimated.
     *
     * @param spectrumFileName name of the spectrum file
     * @return the mz deviation statistics
     */
    public RunMzDeviation getRunMzDeviations(String spectrumFileName) {
        return runMzDeviationMap.get(spectrumFileName);
    }

    /**
     * Estimates the file m/z errors and displays the progress in a waiting
     * handler. Shall be done before calibration. The information generated can
     * be cleared from the mapping using clearErrors(String spectrumFileName).
     *
     * The progress will only be updated, max value is the number of spectra
     *
     * @param spectrumFileName the name of the file of the run
     * @param identification the corresponding identification
     * @param sequenceProvider a provider for the protein sequences
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler displaying the progress and
     * allowing the user to cancel the process. Can be null
     *
     * @throws InterruptedException exception thrown whenever a thread got interrupted
     */
    public void estimateErrors(String spectrumFileName, Identification identification, SequenceProvider sequenceProvider, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws InterruptedException {
        RunMzDeviation fileErrors = new RunMzDeviation(spectrumFileName, identification, sequenceProvider, identificationParameters, waitingHandler);
        runMzDeviationMap.put(spectrumFileName, fileErrors);
    }

    /**
     * Recalibrates a spectrum.
     *
     * @param fileName the name of the file where to find the spectrum
     * @param spectrumTitle the title of the spectrum
     * @param recalibratePrecursor boolean indicating whether precursors shall
     * be recalibrated
     * @param recalibrateFragmentIons boolean indicating whether fragment ions
     * shall be recalibrated
     *
     * @return a recalibrated spectrum
     */
    public Spectrum recalibrateSpectrum(String fileName, String spectrumTitle, boolean recalibratePrecursor, boolean recalibrateFragmentIons) {

        RunMzDeviation runError = runMzDeviationMap.get(fileName);
        if (runError == null) {
            throw new IllegalArgumentException("No m/z deviation statistics found for spectrum file " + fileName + ".");
        }

        Spectrum spectrum = spectrumFactory.getSpectrum(fileName, spectrumTitle);
        Precursor precursor = spectrum.getPrecursor();
        double precursorMz = precursor.getMz();
        double precursorRT = precursor.getRt();
        double correction = 0.0;

        if (recalibratePrecursor) {
            correction = runError.getPrecursorMzCorrection(precursorMz, precursorRT);
        }

        Precursor newPrecursor = spectrum.getPrecursor().getRecalibratedPrecursor(correction, 0.0);
        HashMap<Double, Peak> peakList = spectrum.getPeakMap();

        if (recalibrateFragmentIons) {
            peakList = runError.recalibratePeakList(precursorRT, spectrum.getPeakMap());
        }

        return new Spectrum(2, newPrecursor, spectrumTitle, peakList, fileName);
    }
}
