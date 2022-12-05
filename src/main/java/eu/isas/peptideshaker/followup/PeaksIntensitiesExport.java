/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.Ms2PipUtils;
import eu.isas.peptideshaker.utils.PercolatorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/**
 *
 * @author Dafni Skiadopoulou
 */
public class PeaksIntensitiesExport {
    
    /**
     * @param peaksIntensitiesFile The file to write the export.
     * @param ms2pipFile The file with ms2pip results.
     * @param psmIDsFile The file with the PSM ids.
     * @param identification the identification
     * @param sequenceMatchingParameters The sequence matching parameters.
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
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ){
        
        HashMap<String, ArrayList<Spectrum>> fragmentationPrediction = null;

        if (ms2pipFile != null) {

            waitingHandler.setWaitingText("Exporting mass spectra peaks intensities - Parsing ms2pip results");

            fragmentationPrediction = PercolatorExport.getIntensitiesPrediction(ms2pipFile);
            
        }
        
        ArrayList<String> psmIDs = null;

        if (psmIDsFile != null) {

            waitingHandler.setWaitingText("Exporting mass spectra peaks intensities - Parsing ms2pip results");

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
                spectrumProvider,
                waitingHandler
        );
        
    }
    
    /**
     * @param psmIDsFile The file with the PSM ids.
     */
    private static ArrayList<String> getPSMids(
            File psmIDsFile
    ){
        ArrayList<String> psmIDs = new ArrayList<>();
        
        try (SimpleFileReader reader = SimpleFileReader.getFileReader(psmIDsFile)) {
         
            String PSMid = "";

            while ((PSMid = reader.readLine()) != null) {
                psmIDs.add(PSMid);
            }
        }
        
        return psmIDs;
    }
    
    /**
     * @param peaksIntensitiesFile The file to write the export.
     * @param fragmentationPrediction the map of spectrumKey to fragmentation predictions.
     * @param psmIDs the list of PSM ids to be used for the export.
     * @param identification the identification
     * @param modificationParameters The modification parameters.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
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
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ){
        
        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try (SimpleFileWriter writer = new SimpleFileWriter(peaksIntensitiesFile, true)) {
            
            String header = "PSMId,measuredLabel,matchedLabel,mz,intensity";
            writer.writeLine(header);

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            HashSet<Long> processedPeptideKeys = new HashSet<>();

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
                                        modificationFactory,
                                        processedPeptideKeys,
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
     * @param fragmentationPrediction the map of spectrumKey to fragmentation predictions.
     * @param peptideAssumption The peptide assumption to write.
     * @param modificationParameters The modification parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param modificationFactory The factory containing the modification
     * details.
     * @param processedPeptides The keys of the peptides already processed.
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
            ModificationFactory modificationFactory,
            HashSet<Long> processedPeptides,
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
        
        if (!psmIDs.contains(psmID)){
            return;
        }
        
        ArrayList<Spectrum> predictedSpectra = fragmentationPrediction.get(String.valueOf(peptideKey));
        if (predictedSpectra == null){
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
        for (int i=0; i<aligned_peaks.size(); i++){
            if (aligned_peaks.get(i).get(0) != -1){
                matchedPeaks.add(aligned_peaks.get(i));
                measuredAlignedIndices.add(aligned_peaks.get(i).get(0));
                predictedAlignedIndices.add(aligned_peaks.get(i).get(1));
            }
        }
        
        ArrayList<Spectrum> spectraScaledIntensities = PercolatorUtils.scaleIntensities(measuredSpectrum, predictedSpectrum, matchedPeaks);
        
        Spectrum measuredScaledSpectrum = spectraScaledIntensities.get(0);
        Spectrum predictedScaledSpectrum = spectraScaledIntensities.get(1);
        
        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPeptides.contains(peptideKey)) {

            //String line = String.join(" ", Long.toString(peptideKey), peptideData);

            //writer.writeLine(line);
            
            double[] measuredMz = measuredScaledSpectrum.mz;
            double[] measuredIntensities = measuredScaledSpectrum.intensity;
            
            for (int i = 0; i < measuredMz.length; i++) {
                int matchedLabel = 0;
                if (measuredAlignedIndices.contains(i)){
                    matchedLabel = 1;
                }
                
                String line = String.join(",", psmID, "1", String.valueOf(matchedLabel), String.valueOf(measuredMz[i]), String.valueOf(measuredIntensities[i]));
                writer.writeLine(line);
                
            }
            
            double[] predMz = predictedScaledSpectrum.mz;
            double[] predIntensities = predictedScaledSpectrum.intensity;
            
            for (int i = 0; i < predMz.length; i++) {
                int matchedLabel = 0;
                if (predictedAlignedIndices.contains(i)){
                    matchedLabel = 1;
                }
                
                String line = String.join(",", psmID, "-1", String.valueOf(matchedLabel), String.valueOf(predMz[i]), String.valueOf(predIntensities[i]));
                writer.writeLine(line);
                
            }
            
            
            processedPeptides.add(peptideKey);

        }

        writingSemaphore.release();

    }
    
}
