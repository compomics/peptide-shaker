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
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.DeepLcUtils;
import eu.isas.peptideshaker.utils.Ms2PipUtils;
import eu.isas.peptideshaker.utils.PercolatorUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * @param rtObsPredsFile The file to write RT observed and predicted values
     * per PSM.
     * @param ms2pipFile The ms2pip results.
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * @param modificationParameters The modification parameters
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void percolatorExport(
            File destinationFile,
            File deepLcFile,
            File rtObsPredsFile,
            File ms2pipFile,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationParameters modificationParameters,
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

        if (ms2pipFile != null) {

            waitingHandler.setWaitingText("Exporting Percolator output - Parsing ms2pip results");

            fragmentationPrediction = getIntensitiesPrediction(ms2pipFile);

        }

        // Export Percolator training file
        waitingHandler.setWaitingText("Exporting Percolator output - Writing export");

        try {
            percolatorExport(
                    destinationFile,
                    rtObsPredsFile,
                    rtPrediction,
                    fragmentationPrediction,
                    identification,
                    searchParameters,
                    sequenceMatchingParameters,
                    annotationParameters,
                    modificationLocalizationParameters,
                    modificationParameters,
                    sequenceProvider,
                    spectrumProvider,
                    waitingHandler
            );
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(PercolatorExport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Parses the Rt prediction from DeepLC.
     *
     * Expected format: ,seq,modifications,predicted_tr
     * 0,NSVNGTFPAEPMKGPIAMQSGPKPLFR,12|Oxidation,3878.9216854262777
     *
     * @param deepLcFile File with RT predictions from DeepLC.
     * @return
     */
    private static HashMap<String, ArrayList<Double>> getRtPrediction(
            File deepLcFile
    ) {

        HashMap<String, ArrayList<Double>> result = new HashMap<>();

        try ( SimpleFileReader reader = SimpleFileReader.getFileReader(deepLcFile)) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.split(",");

                String key = String.join(",", lineSplit[1], lineSplit[2]);

                double rt = Double.parseDouble(lineSplit[4]);

                /*ArrayList<Double> rtsForPeptide = result.get(key);

                if (rtsForPeptide == null) {

                    rtsForPeptide = new ArrayList<>(1);

                }

                rtsForPeptide.add(rt);*/
                if (result.get(key) == null) {

                    ArrayList<Double> rtsForPeptide = new ArrayList<>(1);
                    rtsForPeptide.add(rt);
                    result.put(key, rtsForPeptide);

                } else {

                    result.get(key).add(rt);

                }

            }
        }

        return result;

    }

    /**
     * Parses the peaks intensities prediction from MS2PIP.
     *
     * Expected format:
     * predicted_spectrum_key,charge,ion,ionnumber,mz,prediction
     * 2238942014911164193,3,B,1,138.066,0
     *
     * @param ms2pipFile File with spectra fragmentation predictions from
     * MS2PIP.
     *
     * @return Map with pairs (Ms2PipKey, [Spectrum, bSpectrum, ySpectrum])
     */
    public static HashMap<String, ArrayList<Spectrum>> getIntensitiesPrediction(
            File ms2pipFile
    ) {

        HashMap<String, ArrayList<Spectrum>> result = new HashMap<>();

        try ( SimpleFileReader reader = SimpleFileReader.getFileReader(ms2pipFile)) {

            String line = reader.readLine();

            //read 1st line of data outside while loop
            String firstDataLine = reader.readLine();
            String[] firstDataLineSplit = firstDataLine.split(",");

            String predictedSpectrumKey = firstDataLineSplit[0];

            String firstIon = firstDataLineSplit[2];

            double firstMz = Double.parseDouble(firstDataLineSplit[4]);
            double firstPrediction = Double.parseDouble(firstDataLineSplit[5]);

            ArrayList<Double> mzs = new ArrayList<>();
            ArrayList<Double> predictions = new ArrayList<>();
            mzs.add(firstMz);
            predictions.add(firstPrediction);

            ArrayList<Double> mzsB = new ArrayList<>();
            ArrayList<Double> predictionsB = new ArrayList<>();
            ArrayList<Double> mzsY = new ArrayList<>();
            ArrayList<Double> predictionsY = new ArrayList<>();

            if ((firstIon.equals("B")) || (firstIon.equals("B2"))) {
                mzsB.add(firstMz);
                predictionsB.add(firstPrediction);
            } else if ((firstIon.equals("Y")) || (firstIon.equals("Y2"))) {
                mzsY.add(firstMz);
                predictionsY.add(firstPrediction);
            }

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.split(",");

                String key = lineSplit[0];

                String ion = lineSplit[2];

                double mz = Double.parseDouble(lineSplit[4]);
                double prediction = Double.parseDouble(lineSplit[5]);

                if (key.equals(predictedSpectrumKey)) {
                    mzs.add(mz);
                    predictions.add(prediction);

                    if ((ion.equals("B")) || (ion.equals("B2"))) {
                        mzsB.add(mz);
                        predictionsB.add(prediction);
                    } else if ((ion.equals("Y")) || (ion.equals("Y2"))) {
                        mzsY.add(mz);
                        predictionsY.add(prediction);
                    }

                } else {

                    //Create Spectrum object with all predicted peaks
                    ArrayList<Double> mzsUnsorted = new ArrayList<>(mzs);
                    Collections.sort(mzs);

                    double[] mzsArray = new double[mzs.size()];
                    double[] predictionsArray = new double[mzs.size()];
                    for (int i = 0; i < predictionsArray.length; i++) {
                        mzsArray[i] = mzs.get(i);
                        int index = mzsUnsorted.indexOf(mzs.get(i));
                        predictionsArray[i] = predictions.get(index);
                    }

                    Spectrum predictedSpectrum = new Spectrum(null, mzsArray, predictionsArray, 2);

                    //Create Spectrum object with all B predicted peaks
                    ArrayList<Double> mzsBUnsorted = new ArrayList<>(mzsB);
                    Collections.sort(mzsB);

                    double[] mzsBArray = new double[mzsB.size()];
                    double[] predictionsBArray = new double[mzsB.size()];
                    for (int i = 0; i < predictionsBArray.length; i++) {
                        mzsBArray[i] = mzsB.get(i);
                        int index = mzsBUnsorted.indexOf(mzsB.get(i));
                        predictionsBArray[i] = predictionsB.get(index);
                    }

                    Spectrum predictedBionSpectrum = new Spectrum(null, mzsBArray, predictionsBArray, 2);

                    //Create Spectrum object with all Y predicted peaks
                    ArrayList<Double> mzsYUnsorted = new ArrayList<>(mzsY);
                    Collections.sort(mzsY);

                    double[] mzsYArray = new double[mzsY.size()];
                    double[] predictionsYArray = new double[mzsY.size()];
                    for (int i = 0; i < predictionsYArray.length; i++) {
                        mzsYArray[i] = mzsY.get(i);
                        int index = mzsYUnsorted.indexOf(mzsY.get(i));
                        predictionsYArray[i] = predictionsY.get(index);
                    }

                    Spectrum predictedYionSpectrum = new Spectrum(null, mzsYArray, predictionsYArray, 2);

                    ArrayList<Spectrum> predictedSpectra = new ArrayList<>();
                    predictedSpectra.add(predictedSpectrum);
                    predictedSpectra.add(predictedBionSpectrum);
                    predictedSpectra.add(predictedYionSpectrum);

                    result.put(predictedSpectrumKey, predictedSpectra);

                    mzs = new ArrayList<>();
                    predictions = new ArrayList<>();

                    mzs.add(mz);
                    predictions.add(prediction);

                    mzsB = new ArrayList<>();
                    predictionsB = new ArrayList<>();
                    mzsY = new ArrayList<>();
                    predictionsY = new ArrayList<>();

                    if ((ion.equals("B")) || (ion.equals("B2"))) {
                        mzsB.add(mz);
                        predictionsB.add(prediction);
                    } else if ((ion.equals("Y")) || (ion.equals("Y2"))) {
                        mzsY.add(mz);
                        predictionsY.add(prediction);
                    }

                    predictedSpectrumKey = key;
                }

            }

            //Create Spectrum object with all predicted peaks
            ArrayList<Double> mzsUnsorted = new ArrayList<>(mzs);
            Collections.sort(mzs);

            double[] mzsArray = new double[mzs.size()];
            double[] predictionsArray = new double[mzs.size()];
            for (int i = 0; i < predictionsArray.length; i++) {
                mzsArray[i] = mzs.get(i);
                int index = mzsUnsorted.indexOf(mzs.get(i));
                predictionsArray[i] = predictions.get(index);
            }

            Spectrum predictedSpectrum = new Spectrum(null, mzsArray, predictionsArray, 2);

            //Create Spectrum object with all B predicted peaks
            ArrayList<Double> mzsBUnsorted = new ArrayList<>(mzsB);
            Collections.sort(mzsB);

            double[] mzsBArray = new double[mzsB.size()];
            double[] predictionsBArray = new double[mzsB.size()];
            for (int i = 0; i < predictionsBArray.length; i++) {
                mzsBArray[i] = mzsB.get(i);
                int index = mzsBUnsorted.indexOf(mzsB.get(i));
                predictionsBArray[i] = predictionsB.get(index);
            }

            Spectrum predictedBionSpectrum = new Spectrum(null, mzsBArray, predictionsBArray, 2);

            //Create Spectrum object with all Y predicted peaks
            ArrayList<Double> mzsYUnsorted = new ArrayList<>(mzsY);
            Collections.sort(mzsY);

            double[] mzsYArray = new double[mzsY.size()];
            double[] predictionsYArray = new double[mzsY.size()];
            for (int i = 0; i < predictionsYArray.length; i++) {
                mzsYArray[i] = mzsY.get(i);
                int index = mzsYUnsorted.indexOf(mzsY.get(i));
                predictionsYArray[i] = predictionsY.get(index);
            }

            Spectrum predictedYionSpectrum = new Spectrum(null, mzsYArray, predictionsYArray, 2);

            ArrayList<Spectrum> predictedSpectra = new ArrayList<>();
            predictedSpectra.add(predictedSpectrum);
            predictedSpectra.add(predictedBionSpectrum);
            predictedSpectra.add(predictedYionSpectrum);

            result.put(predictedSpectrumKey, predictedSpectra);

        }

        return result;

    }

    /**
     * Exports a Percolator training file.
     *
     * @param destinationFile The file where to write the export.
     * @param rtObsPredsFile The file to write RT observed and predicted values
     * per PSM.
     * @param rtPrediction The retention time prediction.
     * @param fragmentationPrediction The fragmentation prediction.
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * @param modificationParameters The modification parameters
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     *
     * @throws java.lang.InterruptedException Exception thrown if the execution
     * is interrupted.
     * @throws java.util.concurrent.ExecutionException Exception thrown if an
     * error occurred during concurrent execution.
     */
    public static void percolatorExport(
            File destinationFile,
            File rtObsPredsFile,
            HashMap<String, ArrayList<Double>> rtPrediction,
            HashMap<String, ArrayList<Spectrum>> fragmentationPrediction,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) throws InterruptedException, ExecutionException {

        //Hard-coded number of threads;
        int threadCount = 10;

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try ( SimpleFileWriter writer = new SimpleFileWriter(destinationFile, true)) {

            Boolean rtPredictionsAvailable = rtPrediction != null;
            Boolean spectraPredictionsAvailable = fragmentationPrediction != null;

            HashMap<String, ArrayList<Double>> allRTvalues = null;
            if (rtPredictionsAvailable) {
                allRTvalues = getAllObservedPredictedRT(
                        identification,
                        rtPrediction,
                        searchParameters,
                        sequenceProvider,
                        sequenceMatchingParameters,
                        modificationFactory,
                        spectrumProvider,
                        waitingHandler
                );
            }
            final HashMap<String, ArrayList<Double>> allRTs = allRTvalues;

            String header = PercolatorUtils.getHeader(searchParameters, rtPredictionsAvailable, spectraPredictionsAvailable);

            writer.writeLine(header);

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            // create a custom thread pool to manage the number of threads
            System.out.println("Creating a custom thread pool: " + threadCount + " threads.");
            ForkJoinPool customThreadPool = new ForkJoinPool(threadCount);

            // aggregate all the spectrum matches into a list -> iterate over it
            SpectrumMatch match;
            ArrayList<SpectrumMatch> allSpectrumMatches = new ArrayList<>();
            while ((match = spectrumMatchesIterator.next()) != null) {
                allSpectrumMatches.add(match);
            }
            System.out.println("Processing " + allSpectrumMatches.size() + " matches");

            //while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {
            // parallelize over all scans
            try {
                customThreadPool.submit(() -> allSpectrumMatches.parallelStream().forEach(spectrumMatch -> {
                    //SpectrumMatch spectrumMatch = spectrumMatchesIterator.next();
                    if (spectrumMatch == null) {
                        return;
                    }

                    // Make sure that there is no duplicate in the export
                    HashSet<Long> processedPsmKeys = new HashSet<>();

                    // Display progress
                    if (waitingHandler != null) {

                        waitingHandler.increaseSecondaryProgressCounter();

                        if (waitingHandler.isRunCanceled()) {

                            return;

                        }
                    }

                    Boolean rtFileWriterFlag = false;

                    // Export all candidate peptides
                    SpectrumMatch tempSpectrumMatch = spectrumMatch;
                    tempSpectrumMatch.getAllPeptideAssumptions()
                            .forEach(
                                    peptideAssumption -> writePeptideCandidate(
                                            tempSpectrumMatch,
                                            peptideAssumption,
                                            allRTs,
                                            rtFileWriterFlag,
                                            fragmentationPrediction,
                                            searchParameters,
                                            sequenceProvider,
                                            sequenceMatchingParameters,
                                            annotationParameters,
                                            modificationLocalizationParameters,
                                            modificationFactory,
                                            modificationParameters,
                                            spectrumProvider,
                                            processedPsmKeys,
                                            writingSemaphore,
                                            writer
                                    )
                            );
                })).join();

            } finally {
                System.out.println("Shutting down thread pool.");
                customThreadPool.shutdown();
            }

        }

    }

    /**
     *
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    private static HashMap<String, ArrayList<Double>> getAllObservedPredictedRT(
            Identification identification,
            HashMap<String, ArrayList<Double>> rtPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        if (rtPrediction == null) {
            return null;
        }

        HashMap<String, ArrayList<Double>> allRTvalues = new HashMap<>();

        SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

            final HashMap<String, ArrayList<Double>> rtPreds = rtPrediction;

            // Export all candidate peptides
            SpectrumMatch tempSpectrumMatch = spectrumMatch;
            tempSpectrumMatch.getAllPeptideAssumptions()
                    .parallel()
                    .forEach(
                            peptideAssumption -> addPeptideCandidateRT(
                                    allRTvalues,
                                    tempSpectrumMatch,
                                    peptideAssumption,
                                    rtPreds,
                                    searchParameters,
                                    sequenceProvider,
                                    sequenceMatchingParameters,
                                    modificationFactory,
                                    spectrumProvider
                            )
                    );

        }

        return allRTvalues;

    }

    /**
     *
     * @param deepLcFile The deepLC results.
     * @param rtObsPredsFile The file to write RT observed and predicted values
     * per PSM.
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization.
     * @param modificationParameters The modification parameters
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void RTValuesExport(
            File deepLcFile,
            File rtObsPredsFile,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        Boolean rtPredictionsAvailable = deepLcFile != null;

        // Parse retention time prediction
        HashMap<String, ArrayList<Double>> rtPrediction = new HashMap<>();

        if (deepLcFile != null) {

            waitingHandler.setWaitingText("Exporting Percolator output - Parsing DeepLC results");

            rtPrediction = getRtPrediction(deepLcFile);

        } else {
            return;
        }

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        HashMap<String, ArrayList<Double>> allRTvalues = getAllObservedPredictedRTScaled(
                identification,
                rtPrediction,
                searchParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory,
                spectrumProvider,
                waitingHandler
        );

        //Write to file RT observed and predicted values
        try ( SimpleFileWriter writer = new SimpleFileWriter(rtObsPredsFile, true)) {

            String header = PercolatorUtils.getRTValuesHeader();

            writer.writeLine(header);

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                // Make sure that there is no duplicate in the export
                HashSet<Long> processedPsmKeys = new HashSet<>();

                // Display progress
                if (waitingHandler != null) {

                    waitingHandler.increaseSecondaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }
                }

                Boolean rtFileWriterFlag = true;
                final HashMap<String, ArrayList<Double>> allRTs = allRTvalues;

                // Export all candidate peptides
                SpectrumMatch tempSpectrumMatch = spectrumMatch;
                tempSpectrumMatch.getAllPeptideAssumptions()
                        .parallel()
                        .forEach(peptideAssumption -> writePeptideCandidate(tempSpectrumMatch,
                        peptideAssumption,
                        //rtPreds,
                        allRTs,
                        rtFileWriterFlag,
                        null,
                        searchParameters,
                        sequenceProvider,
                        sequenceMatchingParameters,
                        annotationParameters,
                        modificationLocalizationParameters,
                        modificationFactory,
                        modificationParameters,
                        spectrumProvider,
                        processedPsmKeys,
                        writingSemaphore,
                        writer
                )
                        );
            }

        }

    }

    /**
     * Returns all observed predicted retention times scaled.
     *
     * @param identification The identification.
     * @param rtPrediction The RT prediction.
     * @param searchParameters The search parameters.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param modificationFactory The modification factory.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     *
     * @return All observed predicted retention times scaled.
     */
    private static HashMap<String, ArrayList<Double>> getAllObservedPredictedRTScaled(
            Identification identification,
            HashMap<String, ArrayList<Double>> rtPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        HashMap<String, ArrayList<Double>> allRTvalues = new HashMap<>();

        SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

            final HashMap<String, ArrayList<Double>> rtPreds = rtPrediction;

            // Export all candidate peptides
            SpectrumMatch tempSpectrumMatch = spectrumMatch;
            tempSpectrumMatch.getAllPeptideAssumptions()
                    .parallel()
                    .forEach(
                            peptideAssumption -> addPeptideCandidateRT(
                                    allRTvalues,
                                    tempSpectrumMatch,
                                    peptideAssumption,
                                    rtPreds,
                                    searchParameters,
                                    sequenceProvider,
                                    sequenceMatchingParameters,
                                    modificationFactory,
                                    spectrumProvider
                            )
                    );

        }

        ArrayList<String> allDeepLCkeys = new ArrayList<>();
        ArrayList<Double> allObservedRTs = new ArrayList<>();
        ArrayList<Double> allPredictedRTs = new ArrayList<>();

        for (HashMap.Entry entry : allRTvalues.entrySet()) {
            String key = (String) entry.getKey();
            ArrayList<Double> values = (ArrayList<Double>) entry.getValue();
            allDeepLCkeys.add(key);
            allObservedRTs.add(values.get(0));
            allPredictedRTs.add(values.get(1));
        }

        HashMap<String, ArrayList<Double>> allRTsCenterScale = comparePeptideRTCenterScale(
                allDeepLCkeys,
                allObservedRTs,
                allPredictedRTs
        );

        for (HashMap.Entry entry : allRTvalues.entrySet()) {
            String key = (String) entry.getKey();
            ArrayList<Double> values = (ArrayList<Double>) entry.getValue();
            values.addAll(allRTsCenterScale.get(key));
            allRTvalues.put(key, values);
        }

        return allRTvalues;
    }

    private static String comparePeptideRTranks() {
        return "";
    }

    private static HashMap<String, ArrayList<Double>> comparePeptideRTCenterScale(
            ArrayList<String> allDeepLCkeys,
            ArrayList<Double> allObservedRTs,
            ArrayList<Double> allPredictedRTs
    ) {

        HashMap<String, ArrayList<Double>> allRTsCenterScale = new HashMap<>();

        double minObs = allObservedRTs.get(0);
        double maxObs = allObservedRTs.get(0);

        for (int i = 1; i < allObservedRTs.size(); i++) {
            double value = allObservedRTs.get(i);
            if (value < minObs) {
                minObs = value;
            }
            if (value > maxObs) {
                maxObs = value;
            }
        }

        double minPreds = allPredictedRTs.get(0);
        double maxPreds = allPredictedRTs.get(0);

        for (int i = 1; i < allPredictedRTs.size(); i++) {

            double value = allPredictedRTs.get(i);

            if (value < minPreds) {
                minPreds = value;
            }

            if (value > maxPreds) {
                maxPreds = value;
            }

        }

        for (int i = 0; i < allPredictedRTs.size(); i++) {

            double scaledObsRT = (allObservedRTs.get(i) - minObs) / (maxObs - minObs);
            double scaledPredsRT = (allPredictedRTs.get(i) - minPreds) / (maxPreds - minPreds);

            ArrayList<Double> RTs = new ArrayList<Double>() {
                {
                    add(scaledObsRT);
                    add(scaledPredsRT);
                }
            };

            allRTsCenterScale.put(allDeepLCkeys.get(i), RTs);

        }

        return allRTsCenterScale;

    }

    private static ArrayList<Double> getPeptidePredictedRT(
            PeptideAssumption peptideAssumption,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            HashMap<String, ArrayList<Double>> rtPrediction
    ) {

        ArrayList<Double> predictedRts;

        String deepLcKey = String.join(",",
                peptideAssumption.getPeptide().getSequence(),
                DeepLcUtils.getModifications(
                        peptideAssumption.getPeptide(),
                        searchParameters.getModificationParameters(),
                        sequenceProvider,
                        sequenceMatchingParameters,
                        modificationFactory
                )
        );

        predictedRts = rtPrediction.get(deepLcKey);

        return predictedRts;
    }

    private static void addPeptideCandidateRT(
            HashMap<String, ArrayList<Double>> allRTvalues,
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            HashMap<String, ArrayList<Double>> rtPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider
    ) {

        String deepLcKey = String.join(",",
                peptideAssumption.getPeptide().getSequence(),
                DeepLcUtils.getModifications(
                        peptideAssumption.getPeptide(),
                        searchParameters.getModificationParameters(),
                        sequenceProvider,
                        sequenceMatchingParameters,
                        modificationFactory
                )
        );

        ArrayList<Double> predictedRts;
        predictedRts = getPeptidePredictedRT(
                peptideAssumption,
                searchParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory,
                rtPrediction
        );

        if (predictedRts == null) {
            ArrayList<Double> peptideRTs = new ArrayList<Double>();
            peptideRTs.add(1000000.0);
            allRTvalues.put(deepLcKey, peptideRTs);
            return;
        }

        ArrayList<Double> peptideRTs = PercolatorUtils.getPeptideObservedPredictedRT(
                spectrumMatch,
                predictedRts,
                spectrumProvider
        );

        allRTvalues.put(deepLcKey, peptideRTs);

    }

    /**
     * Writes a peptide candidate to the export if not done already.
     *
     * @param spectrumMatch The spectrum match where the peptide was found.
     * @param peptideAssumption The peptide assumption.
     * @param allRTvalues The retention time predictions for all peptides.
     * @param fragmentationPrediction The mass spectrum predictions for all
     * peptides.
     * @param searchParameters The parameters of the search.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param annotationParameters The annotation parameters.
     * @param modificationLocalizationParameters The modification localization
     * parameters.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param spectrumProvider The spectrum provider.
     * @param processedPsms The keys of the PSMs already processed.
     * @param writingSemaphore A semaphore to synchronize the writing to the set
     * of already processed peptides.
     * @param writer The writer to use.
     */
    private static void writePeptideCandidate(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            HashMap<String, ArrayList<Double>> allRTvalues,
            Boolean rtFileWriterFlag,
            HashMap<String, ArrayList<Spectrum>> fragmentationPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            ModificationParameters modificationParameters,
            SpectrumProvider spectrumProvider,
            HashSet<Long> processedPsms,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {

        // Get peptide RTs
        Boolean rtPredictionsAvailable = allRTvalues != null;
        ArrayList<Double> peptideRTs = null;

        if (rtPredictionsAvailable) {

            String deepLcKey = String.join(",",
                    peptideAssumption.getPeptide().getSequence(),
                    DeepLcUtils.getModifications(
                            peptideAssumption.getPeptide(),
                            searchParameters.getModificationParameters(),
                            sequenceProvider,
                            sequenceMatchingParameters,
                            modificationFactory
                    )
            );

            peptideRTs = allRTvalues.get(deepLcKey);

            //DeepLC prediction is missing
            if (peptideRTs == null) {
                System.out.println("Missing DeepLC prediction for peptide: " + deepLcKey);
                return;
            }
        }

        //Get peptide's predicted spectrum
        ArrayList<Spectrum> predictedSpectrum = null;
        Boolean spectraPredictionsAvailable = fragmentationPrediction != null;

        if (spectraPredictionsAvailable) {

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
            predictedSpectrum = fragmentationPrediction.get(Long.toString(peptideKey));

            //MS2PIP prediction is missing
            if (predictedSpectrum == null) {
                System.out.println("Missing MS2PIP prediction for peptide: " + Long.toString(peptideKey));
                return;
            }
        }

        // Get peptide data
        String peptideData;

        if (rtFileWriterFlag && rtPredictionsAvailable) {

            peptideData = PercolatorUtils.getPeptideRTData(
                    spectrumMatch,
                    peptideAssumption,
                    modificationParameters,
                    peptideRTs,
                    sequenceProvider,
                    spectrumProvider,
                    sequenceMatchingParameters,
                    modificationFactory
            );

        } else {

            peptideData = PercolatorUtils.getPeptideData(
                    spectrumMatch,
                    peptideAssumption,
                    peptideRTs,
                    predictedSpectrum,
                    searchParameters,
                    sequenceProvider,
                    sequenceMatchingParameters,
                    annotationParameters,
                    modificationLocalizationParameters,
                    modificationFactory,
                    spectrumProvider,
                    modificationParameters
            );
        }

        // Get identifiers
        long psmKey = PercolatorUtils.getPsmKey(peptideData);

        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPsms.contains(psmKey)) {

            writer.writeLine(peptideData);

            processedPsms.add(psmKey);

        }

        writingSemaphore.release();

    }
}
