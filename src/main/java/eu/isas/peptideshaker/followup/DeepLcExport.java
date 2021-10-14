package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.DeepLcUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Export for RT prediction using DeepLC.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class DeepLcExport {

    /**
     * Exports DeepLC training files for each of the spectrum files. Returns an
     * ArrayList of the files exported.
     *
     * @param destinationStem The stem to use for the path.
     * @param identification The identification object containing the matches.
     * @param modificationParameters The modification parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     *
     * @return An ArrayList of the files exported.
     */
    public static ArrayList<File> deepLcExport(
            String destinationStem,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        HashMap<String, HashSet<Long>> spectrumIdentificationMap = identification.getSpectrumIdentification();

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        spectrumIdentificationMap.entrySet()
                .parallelStream()
                .forEach(
                        entry -> deepLcExport(
                                getDestinationFile(
                                        destinationStem,
                                        entry.getKey(),
                                        spectrumIdentificationMap.size() > 1
                                ),
                                getConfidentHitsDestinationFile(
                                        destinationStem,
                                        entry.getKey(),
                                        spectrumIdentificationMap.size() > 1
                                ),
                                entry.getValue(),
                                identification,
                                modificationParameters,
                                sequenceMatchingParameters,
                                sequenceProvider,
                                spectrumProvider,
                                waitingHandler
                        )
                );

        return getExportedFiles(destinationStem, identification);

    }

    /**
     * Returns an ArrayList of all the files that will be written by the export.
     *
     * @param destinationStem The stem to use for the path.
     * @param identification The identification object containing the matches.
     *
     * @return An ArrayList of all the files that will be written by the export.
     */
    public static ArrayList<File> getExportedFiles(
            String destinationStem,
            Identification identification
    ) {

        HashMap<String, HashSet<Long>> spectrumIdentificationMap = identification.getSpectrumIdentification();

        ArrayList<File> result = new ArrayList<>(0);

        result.addAll(
                spectrumIdentificationMap.keySet()
                        .stream()
                        .map(
                                spectrumFile -> getDestinationFile(
                                        destinationStem,
                                        spectrumFile,
                                        spectrumIdentificationMap.size() > 1
                                )
                        )
                        .collect(
                                Collectors.toCollection(ArrayList::new)
                        )
        );

        result.addAll(
                spectrumIdentificationMap.keySet()
                        .stream()
                        .map(
                                spectrumFile -> getDestinationFile(
                                        destinationStem,
                                        spectrumFile,
                                        spectrumIdentificationMap.size() > 1
                                )
                        )
                        .collect(
                                Collectors.toCollection(ArrayList::new)
                        )
        );

        return result;
    }

    /**
     * Returns the file where to write the export.
     *
     * @param destinationStem The stem to use for the path.
     * @param spectrumFile The name of the spectrum file.
     * @param addSuffix A boolean indicating whehter the name of the spectrum
     * file should be appended to the stem.
     *
     * @return The file where to write the export.
     */
    public static File getDestinationFile(
            String destinationStem,
            String spectrumFile,
            boolean addSuffix
    ) {

        if (!addSuffix) {

            if (!destinationStem.endsWith(".gz")) {

                destinationStem = destinationStem + ".gz";

            }

            return new File(destinationStem);

        } else {

            String path = String.join("_", destinationStem, spectrumFile, "deeplc_export.gz");

            return new File(path);

        }
    }

    /**
     * Returns the file where to write the export for confident hits.
     *
     * @param destinationStem The stem to use for the path.
     * @param spectrumFile The name of the spectrum file.
     * @param addSuffix A boolean indicating whehter the name of the spectrum
     * file should be appended to the stem.
     *
     * @return The file where to write the export.
     */
    public static File getConfidentHitsDestinationFile(
            String destinationStem,
            String spectrumFile,
            boolean addSuffix
    ) {

        if (!addSuffix) {

            if (!destinationStem.endsWith(".gz")) {

                destinationStem = destinationStem + "_confident.gz";

            } else {

                destinationStem = destinationStem.substring(0, destinationStem.length() - 3) + "_confident.gz";

            }

            return new File(destinationStem);

        } else {

            String path = String.join("_", destinationStem, spectrumFile, "deeplc_export_confident.gz");

            return new File(path);

        }
    }

    /**
     * Exports a DeepLC training file for the given spectrum file.
     *
     * @param destinationFile The file where to write the export.
     * @param confidentHitsDestinationFile The file where to write the export
     * for confident hits.
     * @param keys The keys of the spectrum matches.
     * @param identification The identification object containing the matches.
     * @param modificationParameters The modification parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void deepLcExport(
            File destinationFile,
            File confidentHitsDestinationFile,
            HashSet<Long> keys,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        HashSet<Long> processedPeptideKeys = new HashSet<>();
        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try (SimpleFileWriter writer = new SimpleFileWriter(destinationFile, true)) {

            writer.writeLine("seq,modifications,rt");

            try (SimpleFileWriter writerConfident = new SimpleFileWriter(confidentHitsDestinationFile, true)) {

                writerConfident.writeLine("seq,modifications,rt");

                long[] spectrumKeys = keys.stream().mapToLong(a -> a).toArray();

                SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(spectrumKeys, waitingHandler);

                SpectrumMatch spectrumMatch;

                while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                    // Display progress
                    if (waitingHandler != null) {

                        waitingHandler.increaseSecondaryProgressCounter();

                        if (waitingHandler.isRunCanceled()) {

                            return;

                        }
                    }

                    // Measured retention time
                    String spectrumFile = spectrumMatch.getSpectrumFile();
                    String spectrumTitle = spectrumMatch.getSpectrumTitle();
                    Precursor precursor = spectrumProvider.getPrecursor(spectrumFile, spectrumTitle);
                    double retentionTime = precursor.rt;

                    // Export all candidate peptides
                    spectrumMatch.getAllPeptideAssumptions()
                            .parallel()
                            .forEach(
                                    peptideAssumption -> writePeptideCandidate(
                                            peptideAssumption,
                                            retentionTime,
                                            modificationParameters,
                                            sequenceProvider,
                                            sequenceMatchingParameters,
                                            modificationFactory,
                                            processedPeptideKeys,
                                            writingSemaphore,
                                            writer
                                    )
                            );

                    // Check whether the spectrum yielded a confident peptide
                    if (spectrumMatch.getBestPeptideAssumption() != null
                            && ((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                                    .getMatchValidationLevel()
                                    .isValidated()) {

                        // Export the confident peptide to the confident peptides file
                        writePeptideCandidate(
                                spectrumMatch.getBestPeptideAssumption(),
                                retentionTime,
                                modificationParameters,
                                sequenceProvider,
                                sequenceMatchingParameters,
                                modificationFactory,
                                processedPeptideKeys,
                                writingSemaphore,
                                writer
                        );

                    }
                }
            }
        }
    }

    /**
     * Writes a peptide candidate to the export if not done already.
     *
     * @param peptideAssumption The peptide assumption to write.
     * @param modificationParameters The modification parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param processedPeptides The keys of the peptides already processed.
     * @param writingSemaphore A semaphore to synchronize the writing to the set
     * of already processed peptides.
     * @param writer The writer to use.
     */
    private static void writePeptideCandidate(
            PeptideAssumption peptideAssumption,
            double retentionTime,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            HashSet<Long> processedPeptides,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {

        // Get peptide data
        String peptideData = DeepLcUtils.getPeptideData(
                peptideAssumption,
                retentionTime,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );

        // Get corresponding key
        long peptideKey = DeepLcUtils.getPeptideKey(peptideData);

        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPeptides.contains(peptideKey)) {

            String line = String.join(",", Long.toString(peptideKey), peptideData);

            writer.writeLine(line);

        }

        writingSemaphore.release();

    }
}
