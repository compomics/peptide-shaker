package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;

import java.io.File;
import java.util.HashSet;

/**
 *
 * @author Dafni Skiadopoulou
 */
public class PSMIdentExport {
    
    /**
     * @param psmIdentifiersFile The file to write the export.
     * @param identification The identification object containing the matches.
     * @param waitingHandler The waiting handler.
     */
    public static void psmIdentExport(
            File psmIdentifiersFile,
            Identification identification,
            WaitingHandler waitingHandler
    ){
        // Export PSM identifiers file
        waitingHandler.setWaitingText("Exporting PSMs Identifiers - Writing export");
        
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        
        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try (SimpleFileWriter writer = new SimpleFileWriter(psmIdentifiersFile, true)){
            
            String header = String.join("\t","PSMId","SpectrumTitle");
            
            writer.writeLine(header);
            
            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            SpectrumMatch spectrumMatch;
            
            while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                // Make sure that there is no duplicate in the export
                HashSet<String> processedPSMs = new HashSet<>();

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
                                        processedPSMs,
                                        writingSemaphore,
                                        writer
                                )
                        );
            }
            
        }
        
    }
    
    private static void writePeptideCandidate(
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            HashSet<String> processedPSMs,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ){
        // PSM id
        long spectrumKey = spectrumMatch.getKey();
        Peptide peptide = peptideAssumption.getPeptide();
        long peptideKey = peptide.getMatchingKey();
        String psmID = String.join("_", String.valueOf(spectrumKey), String.valueOf(peptideKey)); 
        
        String spectrumTitle = spectrumMatch.getSpectrumTitle();
        
        // Get PSM data
        String psmData = String.join("\t",psmID, spectrumTitle);
        
        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPSMs.contains(psmData)) {

            writer.writeLine(psmData);

            processedPSMs.add(psmData);

        }
        
        writingSemaphore.release();
        
    }
    
}
