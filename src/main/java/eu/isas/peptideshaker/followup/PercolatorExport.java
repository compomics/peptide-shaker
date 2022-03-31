package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.DeepLcUtils;
import eu.isas.peptideshaker.utils.PercolatorUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Export for Percolator.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class PercolatorExport {

    /**
     * Exports a Percolator training file for each of the spectrum files.
     * Returns an ArrayList of the files exported.
     *
     * @param destinationFile The file to use to write the file.
     * @param deepLcFile The deepLC results.
     * @param ms2pipFile The ms2pip results.
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void percolatorExport(
            File destinationFile,
            File deepLcFile,
            File ms2pipFile,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        // Parse retention time prediction
        HashMap<String, ArrayList<Double>> rtPrediction = null;

        if (deepLcFile != null) {

            waitingHandler.setWaitingText("Exporting Percolator output - Parsing DeepLC results");

            rtPrediction = getRtPrediction(deepLcFile);

        }

        // Parse fragmentation prediction
        HashMap<String, ArrayList<Spectrum>> fragmentationPrediction = null;

        if (deepLcFile != null) {

            waitingHandler.setWaitingText("Exporting Percolator output - Parsing ms2pip results");

            //@TODO 
        }

        // Export Percolator training file
        waitingHandler.setWaitingText("Exporting Percolator output - Writing export");

        percolatorExport(
                destinationFile,
                rtPrediction,
                fragmentationPrediction,
                identification,
                searchParameters,
                sequenceMatchingParameters,
                annotationParameters,
                modificationLocalizationParameters,
                sequenceProvider,
                spectrumProvider,
                waitingHandler
        );
    }

    /**
     * Parses the Rt prediction from DeepLC.
     *
     * Expected format: ,seq,modifications,predicted_tr
     * 0,NSVNGTFPAEPMKGPIAMQSGPKPLFR,12|Oxidation,3878.9216854262777
     *
     * @param deepLcFile
     * @return
     */
    private static HashMap<String, ArrayList<Double>> getRtPrediction(
            File deepLcFile
    ) {

        HashMap<String, ArrayList<Double>> result = new HashMap<>();

        try (SimpleFileReader reader = SimpleFileReader.getFileReader(deepLcFile)) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                //String[] lineSplit = line.split("\t");
                String[] lineSplit = line.split(",");

                String key = String.join(",", lineSplit[1], lineSplit[2]);

                double rt = Double.parseDouble(lineSplit[4]);

                ArrayList<Double> rtsForPeptide = result.get(key);

                if (rtsForPeptide == null) {

                    rtsForPeptide = new ArrayList<>(1);

                }

                rtsForPeptide.add(rt);

            }
        }

        return result;

    }

    /**
     * Exports a Percolator training file.
     *
     * @param destinationFile The file where to write the export.
     * @param rtPrediction The retention time prediction.
     * @param fragmentationPrediction The fragmentation prediction.
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void percolatorExport(
            File destinationFile,
            HashMap<String, ArrayList<Double>> rtPrediction,
            HashMap<String, ArrayList<Spectrum>> fragmentationPrediction,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try (SimpleFileWriter writer = new SimpleFileWriter(destinationFile, true)) {
            
            Boolean rtPredictionsAvailable = rtPrediction != null;
            
            String header = PercolatorUtils.getHeader(searchParameters, rtPredictionsAvailable);

            writer.writeLine(header);

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                // Make sure that there is no duplicate in the export
                HashSet<Long> processedPeptideKeys = new HashSet<>();

                // Display progress
                if (waitingHandler != null) {

                    waitingHandler.increaseSecondaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }
                }

                // Export all candidate peptides
                SpectrumMatch tempSpectrumMatch = spectrumMatch;
                tempSpectrumMatch.getAllPeptideAssumptions()
                        .parallel()
                        .forEach(
                                peptideAssumption -> writePeptideCandidate(
                                        tempSpectrumMatch,
                                        peptideAssumption,
                                        rtPrediction,
                                        searchParameters,
                                        sequenceProvider,
                                        sequenceMatchingParameters,
                                        annotationParameters,
                                        modificationLocalizationParameters,
                                        modificationFactory,
                                        spectrumProvider,
                                        processedPeptideKeys,
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
     * @param spectrumMatch The spectrum match where the peptide was found.
     * @param peptideAssumption The peptide assumption.
     * @param rtPrediction The retention time predictions for all peptides.
     * @param searchParameters The parameters of the search.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param spectrumProvider The spectrum provider.
     * @param processedPeptides The keys of the peptides already processed.
     * @param writingSemaphore A semaphore to synchronize the writing to the set
     * of already processed peptides.
     * @param writer The writer to use.
     */
    private static void writePeptideCandidate(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            HashMap<String, ArrayList<Double>> rtPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            HashSet<Long> processedPeptides,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {

        // Get Percolator key
        ArrayList<Double> predictedRts = null;

        if (rtPrediction != null) {

            String deepLcKey = String.join(",",
                    peptideAssumption.getPeptide().getSequence(),
                    DeepLcUtils.getModifications(peptideAssumption.getPeptide(), searchParameters.getModificationParameters(), sequenceProvider, sequenceMatchingParameters, modificationFactory)
            );
            predictedRts = rtPrediction.get(deepLcKey);

        }

        // Get peptide data
        
        Boolean rtPredictionsAvailable = rtPrediction != null;
        
        String peptideData = PercolatorUtils.getPeptideData(
                spectrumMatch,
                peptideAssumption,
                rtPredictionsAvailable,
                predictedRts,
                searchParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                annotationParameters,
                modificationLocalizationParameters,
                modificationFactory,
                spectrumProvider
        );

        // Get identifiers
        long peptideKey = DeepLcUtils.getPeptideKey(peptideData);

        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPeptides.contains(peptideKey)) {

            writer.writeLine(peptideData);

            processedPeptides.add(peptideKey);

        }

        writingSemaphore.release();

    }
}
