package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.Ms2PipUtils;
import eu.isas.peptideshaker.utils.PercolatorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Export for peak intensities.
 *
 * @author Dafni Skiadopoulou
 */
public class PeaksIntensitiesExport {

    /**
     * Export the peak intensities.
     *
     * @param peaksIntensitiesFile The file to write the export.
     * @param ms2pipFile The file with ms2pip results.
     * @param psmIDsFile The file with the PSM ids.
     * @param identification the identification
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The spectrum annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param modificationParameters The modification parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void peaksIntensitiesExport(
            File peaksIntensitiesFile,
            File ms2pipFile,
            File psmIDsFile,
            Identification identification,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        HashMap<String, ArrayList<Spectrum>> fragmentationPrediction = null;

        if (ms2pipFile != null) {

            waitingHandler.setWaitingText("Exporting mass spectra peaks intensities - Parsing ms2pip results");

            fragmentationPrediction = PercolatorExport.getIntensitiesPrediction(ms2pipFile);

        }

        ArrayList<String> psmIDs = null;

        if (psmIDsFile != null) {

            waitingHandler.setWaitingText("Exporting mass spectra peaks intensities - Parsing PSM IDs");

            psmIDs = getPSMids(psmIDsFile);

        }

        peaksIntensitiesExport(
                peaksIntensitiesFile,
                fragmentationPrediction,
                psmIDs,
                identification,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                annotationParameters,
                modificationLocalizationParameters,
                spectrumProvider,
                waitingHandler
        );

    }

    /**
     * Returns the PSM identifications.
     *
     * @param psmIDsFile The file with the PSM ids.
     */
    private static ArrayList<String> getPSMids(
            File psmIDsFile
    ) {

        ArrayList<String> psmIDs = new ArrayList<>();

        try ( SimpleFileReader reader = SimpleFileReader.getFileReader(psmIDsFile)) {

            String psmId;

            while ((psmId = reader.readLine()) != null) {
                psmIDs.add(psmId);
            }
        }

        return psmIDs;
    }

    /**
     * Export the peak intensities.
     *
     * @param peaksIntensitiesFile The file to write the export.
     * @param fragmentationPrediction the map of predicted spectrum key to
     * fragmentation predictions.
     * @param psmIDs the list of PSM ids to be used for the export.
     * @param identification the identification
     * @param modificationParameters The modification parameters.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The spectrum annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void peaksIntensitiesExport(
            File peaksIntensitiesFile,
            HashMap<String, ArrayList<Spectrum>> fragmentationPrediction,
            ArrayList<String> psmIDs,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        // reset the progress bar
        if (waitingHandler != null) {

            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        }

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try ( SimpleFileWriter writer = new SimpleFileWriter(peaksIntensitiesFile, true)) {

            String header = "PSMId,measuredLabel,matchedLabel,mz,intensity,ion";
            writer.writeLine(header);

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                // Display progress
                if (waitingHandler != null) {

                    waitingHandler.increaseSecondaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }
                }

                SpectrumMatch spectrumMatchFinal = spectrumMatch;

                // Export all candidate peptides
                spectrumMatch.getAllPeptideAssumptions()
                        .parallel()
                        .forEach(
                                peptideAssumption -> writePeptideCandidate(
                                        fragmentationPrediction,
                                        psmIDs,
                                        peptideAssumption,
                                        modificationParameters,
                                        sequenceProvider,
                                        sequenceMatchingParameters,
                                        annotationParameters,
                                        modificationLocalizationParameters,
                                        modificationFactory,
                                        spectrumProvider,
                                        spectrumMatchFinal,
                                        writingSemaphore,
                                        writer
                                )
                        );
            }

        }

    }

    /**
     * Writes a peptide candidate to the export if not done already.
     *
     * @param fragmentationPrediction the map of predicted spectrum key to
     * fragmentation predictions.
     * @param peptideAssumption The peptide assumption to write.
     * @param modificationParameters The modification parameters.
     * @param annotationParameters The annotation parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param processedPsms The keys of the peptides already processed.
     * @param spectrumProvider The spectrum provider.
     * @param spectrumMatch The spectrum match.
     * @param writingSemaphore A semaphore to synchronize the writing to the set
     * of already processed peptides.
     * @param writer The writer to use.
     */
    private static void writePeptideCandidate(
            HashMap<String, ArrayList<Spectrum>> fragmentationPrediction,
            ArrayList<String> psmIDs,
            PeptideAssumption peptideAssumption,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            SpectrumMatch spectrumMatch,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {

        // Get peptide data
        String peptideData = Ms2PipUtils.getPeptideData(
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );

        // Get corresponding key
        long peptideKey = Ms2PipUtils.getPeptideKey(peptideData);

        // PSM id
        long spectrumKey = spectrumMatch.getKey();
        String peptideID = Long.toString(peptideKey);
        String psmID = String.join("_", String.valueOf(spectrumKey), peptideID);

        if (!psmIDs.contains(psmID)) {
            return;
        }

        ArrayList<Spectrum> predictedSpectra = fragmentationPrediction.get(peptideID);

        if (predictedSpectra == null) {
            System.out.println("No MS2PIP prediction for PSM with ID: " + psmID);
            return;
        }

        Spectrum predictedSpectrum = predictedSpectra.get(0);

        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();

        // Get measured spectrum
        Spectrum measuredSpectrum = spectrumProvider.getSpectrum(spectrumFile, spectrumTitle);

        ArrayList<ArrayList<Integer>> aligned_peaks = PercolatorUtils.getAlignedPeaks(measuredSpectrum, predictedSpectrum);

        ArrayList<ArrayList<Integer>> matchedPeaks = new ArrayList<>();
        ArrayList<Integer> measuredAlignedIndices = new ArrayList<>();
        ArrayList<Integer> predictedAlignedIndices = new ArrayList<>();
        for (int i = 0; i < aligned_peaks.size(); i++) {
            if (aligned_peaks.get(i).get(0) != -1) {
                matchedPeaks.add(aligned_peaks.get(i));
                measuredAlignedIndices.add(aligned_peaks.get(i).get(0));
                predictedAlignedIndices.add(aligned_peaks.get(i).get(1));
            }
        }

        ArrayList<Spectrum> spectraScaledIntensities = PercolatorUtils.scaleIntensities(measuredSpectrum, predictedSpectrum, matchedPeaks);

        Spectrum measuredScaledSpectrum = spectraScaledIntensities.get(0);
        Spectrum predictedScaledSpectrum = spectraScaledIntensities.get(1);

        // Get spectrum annotation
        PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
        SequenceMatchingParameters modificationSequenceMatchingParameters = modificationLocalizationParameters.getSequenceMatchingParameters();

        SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                spectrumFile,
                spectrumTitle,
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                peptideSpectrumAnnotator
        );

        IonMatch[] matches = peptideSpectrumAnnotator.getSpectrumAnnotation(
                annotationParameters,
                specificAnnotationParameters,
                spectrumFile,
                spectrumTitle,
                measuredSpectrum,
                peptideAssumption.getPeptide(),
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                false
        );

        HashMap<Double, String> annotationMap = new HashMap<>(matches.length);

        for (IonMatch match : matches) {

            double mz = match.peakMz;
            String label = match.getPeakAnnotation();

            String currentLabel = annotationMap.get(mz);

            if (currentLabel == null) {

                currentLabel = label;

            } else {

                currentLabel = String.join(",", label);

            }

            annotationMap.put(mz, currentLabel);

        }

        peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        matches = peptideSpectrumAnnotator.getSpectrumAnnotation(
                annotationParameters,
                specificAnnotationParameters,
                spectrumFile,
                spectrumTitle,
                predictedSpectrum,
                peptideAssumption.getPeptide(),
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters,
                false
        );

        for (IonMatch match : matches) {

            double mz = match.peakMz;
            String label = match.getPeakAnnotation();

            String currentLabel = annotationMap.get(mz);

            if (currentLabel == null) {

                currentLabel = label;

            } else {

                currentLabel = String.join(",", label);

            }

            annotationMap.put(mz, currentLabel);

        }

        // Export if not done already
        writingSemaphore.acquire();

        //String line = String.join(" ", Long.toString(peptideKey), peptideData);
        //writer.writeLine(line);
        double[] measuredMz = measuredScaledSpectrum.mz;
        double[] measuredIntensities = measuredScaledSpectrum.intensity;

        for (int i = 0; i < measuredMz.length; i++) {

            String annotation = annotationMap.get(measuredMz[i]);

            if (annotation == null) {

                annotation = "";

            }

            String matchedLabel = measuredAlignedIndices.contains(i) ? "1" : "0";

            String line = String.join(
                    ",",
                    psmID,
                    "1",
                    matchedLabel,
                    String.valueOf(measuredMz[i]),
                    String.valueOf(measuredIntensities[i]),
                    annotation
            );

            writer.writeLine(line);

        }

        double[] predMz = predictedScaledSpectrum.mz;
        double[] predIntensities = predictedScaledSpectrum.intensity;

        for (int i = 0; i < predMz.length; i++) {

            String annotation = annotationMap.get(predMz[i]);

            if (annotation == null) {

                annotation = "";

            }

            String matchedLabel = measuredAlignedIndices.contains(i) ? "1" : "0";

            String line = String.join(
                    ",",
                    psmID,
                    "-1",
                    matchedLabel,
                    String.valueOf(predMz[i]),
                    String.valueOf(predIntensities[i]),
                    annotation
            );

            writer.writeLine(line);

        }

        writingSemaphore.release();

    }

}
