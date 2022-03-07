package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
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
import java.util.stream.Collectors;

/**
 * Export for Percolator.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class PercolatorExport {

    /**
     * Exports a Percolator training file for each of the spectrum files.Returns an ArrayList of the files exported.
     *
     * @param destinationFile The file to use to write the file.
     * @param deepLcFile The deepLC results.
     * @param rtObsPredsFile The file to write RT observed and predicted values per PSM.
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
        HashMap<String, Spectrum> fragmentationPrediction = null;

        if (ms2pipFile != null) {

            waitingHandler.setWaitingText("Exporting Percolator output - Parsing ms2pip results");

            fragmentationPrediction = getIntensitiesPrediction(ms2pipFile);
            
        }

        // Export Percolator training file
        waitingHandler.setWaitingText("Exporting Percolator output - Writing export");

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

        try (SimpleFileReader reader = SimpleFileReader.getFileReader(deepLcFile)) {

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
                
                if (result.get(key) == null){
                    
                    ArrayList<Double> rtsForPeptide = new ArrayList<>(1);
                    rtsForPeptide.add(rt);
                    result.put(key, rtsForPeptide);
                    
                }
                else{
                    
                    result.get(key).add(rt);
                    
                }

            }
        }
        
        return result;

    }
    
    /**
     * Parses the peaks intensities prediction from MS2PIP.
     *
     * Expected format: spec_id,charge,ion,ionnumber,mz,prediction
     * 2238942014911164193,3,B,1,138.066,0
     *
     * @param ms2pipFile File with spectra fragmentation predictions from MS2PIP.
     * 
     * @return Map with pairs (Ms2PipKey, Spectrum)
     */
    public static HashMap<String, Spectrum> getIntensitiesPrediction(
            File ms2pipFile
    ) {

        //HashMap<String, ArrayList<Spectrum>> result = new HashMap<>();
        HashMap<String, Spectrum> result = new HashMap<>();

        try (SimpleFileReader reader = SimpleFileReader.getFileReader(ms2pipFile)) {

            String line = reader.readLine();
            
            //read 1st line of data outside while loop
            String firstDataLine = reader.readLine();
            String[] firstDataLineSplit = firstDataLine.split(",");

            String spectrumKey = firstDataLineSplit[0];

            double firstMz = Double.parseDouble(firstDataLineSplit[4]);
            double firstPrediction = Double.parseDouble(firstDataLineSplit[5]);
            
            ArrayList<Double> mzs = new ArrayList<>();
            ArrayList<Double> predictions = new ArrayList<>();
            mzs.add(firstMz);
            predictions.add(firstPrediction);

            while ((line = reader.readLine()) != null) {

                String[] lineSplit = line.split(",");

                String key = lineSplit[0];
                
                double mz = Double.parseDouble(lineSplit[4]);
                double prediction = Double.parseDouble(lineSplit[5]);

                if (key.equals(spectrumKey)){
                    mzs.add(mz);
                    predictions.add(prediction);
                }
                else{
                    
                    ArrayList<Double> mzsUnsorted = new ArrayList<>(mzs);
                    Collections.sort(mzs);
                    
                    double[] mzsArray = new double[mzs.size()];
                    double[] predictionsArray = new double[mzs.size()];
                    for (int i = 0; i < predictionsArray.length; i++) {
                       mzsArray[i] = mzs.get(i);
                       int index = mzsUnsorted.indexOf(mzs.get(i));
                       predictionsArray[i] = predictions.get(index); 
                    }
                    
                    Spectrum predictedSpectrum = new Spectrum(null, mzsArray, predictionsArray);
                    result.put(spectrumKey, predictedSpectrum);
                    
                    
                    mzs = new ArrayList<>();
                    predictions = new ArrayList<>();
                    
                    mzs.add(mz);
                    predictions.add(prediction);
                    
                    spectrumKey = key;
                }

            }
            
            ArrayList<Double> mzsUnsorted = new ArrayList<Double>(mzs);
            Collections.sort(mzs);

            double[] mzsArray = new double[mzs.size()];
            double[] predictionsArray = new double[mzs.size()];
            for (int i = 0; i < predictionsArray.length; i++) {
               //mzsArray[i] = mzs.get(i).doubleValue();  // java 1.4 style
               // or:
               mzsArray[i] = mzs.get(i);
               int index = mzsUnsorted.indexOf(mzs.get(i));
               predictionsArray[i] = predictions.get(index); 
            }

            Spectrum predictedSpectrum = new Spectrum(null, mzsArray, predictionsArray);
            result.put(spectrumKey, predictedSpectrum);
            
        }
        
        return result;

    }

    /**
     * Exports a Percolator training file.
     *
     * @param destinationFile The file where to write the export.
     * @param rtObsPredsFile The file to write RT observed and predicted values per PSM.
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
     */
    public static void percolatorExport(
            File destinationFile,
            File rtObsPredsFile,
            HashMap<String, ArrayList<Double>> rtPrediction,
            HashMap<String, Spectrum> fragmentationPrediction,
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
        
        /*if (rtObsPredsFile != null){
            
            // Export Percolator training file
            waitingHandler.setWaitingText("Exporting RT observed and predicted values - Writing export");

            RTValuesExport(
                    rtObsPredsFile,
                    rtPrediction,
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
            
        }*/

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try (SimpleFileWriter writer = new SimpleFileWriter(destinationFile, true)) {
            
            Boolean rtPredictionsAvailable = rtPrediction != null;
            Boolean spectraPredictionsAvailable = fragmentationPrediction != null;
            
            HashMap<String, ArrayList<Double>> allRTvalues = null;
            if (rtPredictionsAvailable){
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
            
            String header = PercolatorUtils.getHeader(searchParameters, rtPredictionsAvailable, spectraPredictionsAvailable);

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
                
                final HashMap<String, ArrayList<Double>> allRTs = allRTvalues;
                Boolean rtFileWriterFlag = false;
                
                // Export all candidate peptides
                SpectrumMatch tempSpectrumMatch = spectrumMatch;
                tempSpectrumMatch.getAllPeptideAssumptions()
                        .parallel()
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
                                        processedPeptideKeys,
                                        writingSemaphore,
                                        writer
                                )
                        );
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
    ){
        
        if (rtPrediction == null){
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
     * @param rtObsPredsFile The file to write RT observed and predicted values per PSM.
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
        HashMap<String, ArrayList<Double>> rtPrediction = new HashMap<String, ArrayList<Double>>();

        if (deepLcFile != null) {

            waitingHandler.setWaitingText("Exporting Percolator output - Parsing DeepLC results");

            rtPrediction = getRtPrediction(deepLcFile);

        }
        else{
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
        try (SimpleFileWriter writer = new SimpleFileWriter(rtObsPredsFile, true)) {
            
            String header = PercolatorUtils.getRTValuesHeader();

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

                Boolean rtFileWriterFlag = true;
                final HashMap<String, ArrayList<Double>> allRTs = allRTvalues;
                
                // Export all candidate peptides
                SpectrumMatch tempSpectrumMatch = spectrumMatch;
                tempSpectrumMatch.getAllPeptideAssumptions()
                        .parallel()
                        .forEach(
                                peptideAssumption -> writePeptideCandidate(
                                        tempSpectrumMatch,
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
                                        processedPeptideKeys,
                                        writingSemaphore,
                                        writer
                                )
                        );
            }
            
        }
        
    }
    
    private static HashMap<String, ArrayList<Double>> getAllObservedPredictedRTScaled(
            Identification identification,
            HashMap<String, ArrayList<Double>> rtPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ){
        
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
        Iterator entries = allRTvalues.entrySet().iterator();
        while (entries.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) entries.next();
            String key = (String)entry.getKey();
            ArrayList<Double> values = (ArrayList<Double>)entry.getValue();
            allDeepLCkeys.add(key);
            allObservedRTs.add(values.get(0));
            allPredictedRTs.add(values.get(1));
        }
                
        HashMap<String, ArrayList<Double>> allRTsCenterScale = comparePeptideRTCenterScale(
                allDeepLCkeys,
                allObservedRTs,
                allPredictedRTs
        );
        Iterator newEntries = allRTvalues.entrySet().iterator();
        while (newEntries.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) newEntries.next();
            String key = (String)entry.getKey();
            ArrayList<Double> values = (ArrayList<Double>)entry.getValue();            
            values.addAll(allRTsCenterScale.get(key));
            allRTvalues.put(key, values);
        }
        
        return allRTvalues;
    }
    
    private static String comparePeptideRTranks(){
        return "";
    }
    
    private static HashMap<String, ArrayList<Double>> comparePeptideRTCenterScale(
            ArrayList<String> allDeepLCkeys,
            ArrayList<Double> allObservedRTs,
            ArrayList<Double> allPredictedRTs
    ){
        
        HashMap<String, ArrayList<Double>> allRTsCenterScale = new HashMap<>();
        
        double minObs = allObservedRTs.get(0);
        double maxObs = allObservedRTs.get(0);
        for (int i=1; i<allObservedRTs.size(); i++){
            double value = allObservedRTs.get(i);
            if (value < minObs){
                minObs = value;
            }
            if (value > maxObs){
                maxObs = value;
            }
        }
        
        double minPreds = allPredictedRTs.get(0);
        double maxPreds = allPredictedRTs.get(0);
        for (int i=1; i<allPredictedRTs.size(); i++){
            double value = allPredictedRTs.get(i);
            if (value < minPreds){
                minPreds = value;
            }
            if (value > maxPreds){
                maxPreds = value;
            }
        }
        
        for (int i=0; i<allPredictedRTs.size(); i++){
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
    ){
        
        ArrayList<Double> predictedRts;
        
        String deepLcKey = String.join(",",
                    peptideAssumption.getPeptide().getSequence(),
                    DeepLcUtils.getModifications(peptideAssumption.getPeptide(), searchParameters.getModificationParameters(), sequenceProvider, sequenceMatchingParameters, modificationFactory)
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
    ){
        
        String deepLcKey = String.join(",",
                    peptideAssumption.getPeptide().getSequence(),
                    DeepLcUtils.getModifications(peptideAssumption.getPeptide(), searchParameters.getModificationParameters(), sequenceProvider, sequenceMatchingParameters, modificationFactory)
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

        if (predictedRts == null){
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
     * @param fragmentationPrediction The mass spectrum predictions for all peptides.
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
            HashMap<String, ArrayList<Double>> allRTvalues,
            Boolean rtFileWriterFlag,
            HashMap<String, Spectrum> fragmentationPrediction,
            SearchParameters searchParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            AnnotationParameters annotationParameters,
            ModificationLocalizationParameters modificationLocalizationParameters,
            ModificationFactory modificationFactory,
            ModificationParameters modificationParameters,
            SpectrumProvider spectrumProvider,
            HashSet<Long> processedPeptides,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {

        // Get peptide RTs
        Boolean rtPredictionsAvailable = allRTvalues != null;
        ArrayList<Double> peptideRTs = null;

        if (rtPredictionsAvailable) {
            String deepLcKey = String.join(",",
                    peptideAssumption.getPeptide().getSequence(),
                    DeepLcUtils.getModifications(peptideAssumption.getPeptide(), searchParameters.getModificationParameters(), sequenceProvider, sequenceMatchingParameters, modificationFactory)
            );
            peptideRTs = allRTvalues.get(deepLcKey);
        }
        
        //Get peptide's predicted spectrum
        Spectrum predictedSpectrum = null;
        Boolean spectraPredictionsAvailable = fragmentationPrediction != null;
        if ( spectraPredictionsAvailable ){
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
            if (predictedSpectrum == null){
                System.out.println("Missing MS2PIP prediction for peptide: " + Long.toString(peptideKey));
                return;
            }
        }
        
        
        

        // Get peptide data
        String peptideData;
        
        if (rtFileWriterFlag && rtPredictionsAvailable){
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
        }
        else{
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
