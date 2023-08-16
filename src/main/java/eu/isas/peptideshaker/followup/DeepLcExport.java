package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.DeepLcUtils;
import eu.isas.peptideshaker.utils.Ms2PipUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * Export for RT prediction using DeepLC.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class DeepLcExport {

    /**
     * Exports DeepLC training files for each of the spectrum files.Returns an
 ArrayList of the files exported.
     *
     * @param destinationStem The stem to use for the path.
     * @param percolatorBenchmarkResultsFile The file containing Percolator results for all PSMs.
     * @param rtApexFile The file containing rt apex information for all PSMs.
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
            File percolatorBenchmarkResultsFile,
            File rtApexFile,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {
        
        HashMap<String, Double> confidenceScores = null;
        if (percolatorBenchmarkResultsFile != null){
            confidenceScores = getPercolatorResults(percolatorBenchmarkResultsFile);
        }
        
        HashMap<String, Double> rtApex = null;
        if (rtApexFile != null){
            rtApex = getRtApexInfo(rtApexFile);
        }

        HashMap<String, HashSet<Long>> spectrumIdentificationMap = identification.getSpectrumIdentification();

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        HashMap<String, Double> confScores = confidenceScores;
        HashMap<String, Double> retentionTimeApex = rtApex;
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
                                confScores,
                                retentionTimeApex,
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
     * Parses the confidence scores from Percolator.
     *
     * Expected format (tab delimited): PSMId   score   q-value   posterior_error_prob   peptide   proteinIds
     * 644797219919995671_-2456456211135484764   5.52233   2.92539e-05   3.80375e-05   -.TINQSLLTPLHVEID.-   -
     *
     * @param percResultsFile
     * @return
     */
    private static HashMap<String, Double> getPercolatorResults(
            File percResultsFile
    ) {

        HashMap<String, Double> result = new HashMap<>();

        try (SimpleFileReader reader = SimpleFileReader.getFileReader(percResultsFile)) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.split("\t");

                String key = lineSplit[0];
                
                double q_value = Double.parseDouble(lineSplit[2]);
                
                if (result.get(key) == null){
                    
                    result.put(key, q_value);
                    
                }

            }
        }
        
        return result;

    }
    
    /**
     * Parses the rt apex information for all PSMs.
     *
     * Expected format (comma delimited): PSMId   rt_apex
     * 644797219919995671_-2456456211135484764   1435.52233   
     *
     * @param rtApexFile
     * @return Map of PSMIds to rt apex
     */
    public static HashMap<String, Double> getRtApexInfo(
            File rtApexFile
    ) {

        HashMap<String, Double> result = new HashMap<>();

        try (SimpleFileReader reader = SimpleFileReader.getFileReader(rtApexFile)) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.split(",");

                String key = lineSplit[0];
                
                double rtApex = Double.parseDouble(lineSplit[1]);
                
                if (result.get(key) == null){
                    
                    result.put(key, rtApex);
                    
                }

            }
        }
        
        return result;

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
     * @param confidenceScores Confidence score for each PSM.
     * @param rtApex RT apex for each PSM
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
            HashMap<String, Double> confidenceScores,
            HashMap<String, Double>  rtApex,
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
        
        HashSet<Long> processedConfidentPeptideKeys = new HashSet<>();

        try (SimpleFileWriter writer = new SimpleFileWriter(destinationFile, true)) {

            writer.writeLine("seq,modifications,tr");

            try (SimpleFileWriter writerConfident = new SimpleFileWriter(confidentHitsDestinationFile, true)) {

                writerConfident.writeLine("seq,modifications,tr");

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
                    SpectrumMatch tempSpectrumMatch = spectrumMatch;
                    tempSpectrumMatch.getAllPeptideAssumptions()
                            .parallel()
                            .forEach(
                                    peptideAssumption -> writePeptideCandidate(
                                            null,
                                            rtApex,
                                            peptideAssumption,
                                            retentionTime,
                                            tempSpectrumMatch,
                                            modificationParameters,
                                            sequenceProvider,
                                            sequenceMatchingParameters,
                                            modificationFactory,
                                            processedPeptideKeys,
                                            writingSemaphore,
                                            writer
                                    )
                            );
                    
                    // Export all confident candidate peptides
                    //SpectrumMatch tempSpectrumMatch2 = spectrumMatch;
                    tempSpectrumMatch.getAllPeptideAssumptions()
                            .parallel()
                            .forEach(
                                    peptideAssumption -> writePeptideCandidate(
                                            confidenceScores,
                                            rtApex,
                                            peptideAssumption,
                                            retentionTime,
                                            tempSpectrumMatch,
                                            modificationParameters,
                                            sequenceProvider,
                                            sequenceMatchingParameters,
                                            modificationFactory,
                                            processedConfidentPeptideKeys,
                                            writingSemaphore,
                                            writerConfident
                                    )
                            );

                    // Check whether the spectrum yielded a confident peptide
                    
                    /*if (spectrumMatch.getBestPeptideAssumption() != null
                            && ((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                                    .getMatchValidationLevel()
                                    .isValidated()) {

                        // Export the confident peptide to the confident peptides file
                        writePeptideCandidate(
                                null,
                                spectrumMatch.getBestPeptideAssumption(),
                                retentionTime,
                                null,
                                modificationParameters,
                                sequenceProvider,
                                sequenceMatchingParameters,
                                modificationFactory,
                                processedPeptideKeys,
                                writingSemaphore,
                                writerConfident
                        );

                    }*/
                    
                }
            }
        }
    }

    /**
     * Writes a peptide candidate to the export if not done already.
     *
     * @param confidenceScores Percolator score with standard features for all PSMs.
     * @param rtApex RT apex info for all PSMs
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
            HashMap<String, Double> confidenceScores,
            HashMap<String, Double> rtApex,
            PeptideAssumption peptideAssumption,
            double retentionTime,
            SpectrumMatch spectrumMatch,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            HashSet<Long> processedPeptides,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {
        
        // PSM id
        long spectrumKey = spectrumMatch.getKey();            
        // Get peptide data
        String peptideDataMs2Pip = Ms2PipUtils.getPeptideData(
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );
        // Get corresponding key
        long peptideMs2PipKey = Ms2PipUtils.getPeptideKey(peptideDataMs2Pip);
        String peptideID = Long.toString(peptideMs2PipKey);

        String psmID = String.join("_", String.valueOf(spectrumKey), peptideID);
        
        if (confidenceScores != null){
            double q_value = confidenceScores.get(psmID);
            if (q_value > 0.01){
                return;
            }
        }
        
        // Get peptide data
        String peptideData = "";
        
        //Check if rt apex info is available
        if (rtApex != null){
            double retentionTimeApex = rtApex.get(psmID);
            peptideData = DeepLcUtils.getPeptideData(
                peptideAssumption,
                retentionTimeApex,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
            );
        }
        else{
            DeepLcUtils.getPeptideData(
                peptideAssumption,
                retentionTime,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
            );
        }

        // Get corresponding key
        long peptideKey = DeepLcUtils.getPeptideKey(peptideData);
        
        //Check if rt apex info is available
        if (rtApex != null){
            double retentionTimeApex = rtApex.get(psmID);
            peptideData = DeepLcUtils.getPeptideData(
                peptideAssumption,
                retentionTimeApex,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
            );
        }

        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPeptides.contains(peptideKey)) {

            writer.writeLine(peptideData);
            
            processedPeptides.add(peptideKey);

        }

        writingSemaphore.release();

    }
}
