package eu.isas.peptideshaker.recalibration;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.RecalibrationUtils;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.IdentificationParameters;
import java.util.HashMap;

/**
 * This class recalibrates spectra.
 *
 * @author Marc Vaudel
 */
public class SpectrumRecalibrator {

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
    public void clearErrors(
            String spectrumFileName
    ) {

        runMzDeviationMap.remove(spectrumFileName);

    }

    /**
     * Returns the mz deviation statistics class for the spectrum file of
     * interest. Null if not estimated.
     *
     * @param spectrumFileName name of the spectrum file
     * @return the mz deviation statistics
     */
    public RunMzDeviation getRunMzDeviations(
            String spectrumFileName
    ) {

        return runMzDeviationMap.get(spectrumFileName);

    }

    /**
     * Estimates the file m/z errors and displays the progress in a waiting
     * handler. Shall be done before calibration. The information generated can
     * be cleared from the mapping using clearErrors(String spectrumFileName).
     *
     * The progress will only be updated, max value is the number of spectra
     *
     * @param spectrumFileNameWithoutExtension the name of the file of the run
     * @param identification the corresponding identification
     * @param sequenceProvider the sequence provider
     * @param spectrumProvider the spectrum provider
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler displaying the progress and
     * allowing the user to cancel the process. Can be null
     */
    public void estimateErrors(
            String spectrumFileNameWithoutExtension,
            Identification identification,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler
    ) {

        RunMzDeviation fileErrors = new RunMzDeviation(
                spectrumFileNameWithoutExtension,
                identification,
                sequenceProvider,
                spectrumProvider,
                identificationParameters,
                waitingHandler
        );

        runMzDeviationMap.put(spectrumFileNameWithoutExtension, fileErrors);

    }

    /**
     * Recalibrates a spectrum.
     *
     * @param fileName the name of the file where to find the spectrum
     * @param spectrumTitle the title of the spectrum
     * @param spectrumProvider the spectrum provider
     * @param recalibratePrecursor boolean indicating whether precursors shall
     * be recalibrated
     * @param recalibrateFragmentIons boolean indicating whether fragment ions
     * shall be recalibrated
     *
     * @return a recalibrated spectrum
     */
    public Spectrum recalibrateSpectrum(
            String fileName,
            String spectrumTitle,
            SpectrumProvider spectrumProvider,
            boolean recalibratePrecursor,
            boolean recalibrateFragmentIons
    ) {

        RunMzDeviation runError = runMzDeviationMap.get(fileName);
        if (runError == null) {
            throw new IllegalArgumentException("No m/z deviation statistics found for spectrum file " + fileName + ".");
        }

        Spectrum originalSpectrum = spectrumProvider.getSpectrum(
                fileName,
                spectrumTitle
        );
        double mzCorrection = recalibratePrecursor ? runError.getPrecursorMzCorrection(
                originalSpectrum.precursor.mz,
                originalSpectrum.precursor.rt)
                : 0.0;

        Precursor newPrecursor = RecalibrationUtils.getRecalibratedPrecursor(originalSpectrum.precursor, mzCorrection, 0.0);
        
        double[] newFragmentMz = recalibrateFragmentIons ?
                runError.recalibrateFragmentMz(
                        originalSpectrum.precursor.rt, 
                        originalSpectrum.mz
                )
                : originalSpectrum.mz;

        return new Spectrum(
                newPrecursor, 
                newFragmentMz, 
                originalSpectrum.intensity,
                originalSpectrum.getSpectrumLevel()
        );
    }
}
