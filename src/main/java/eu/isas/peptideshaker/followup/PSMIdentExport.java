package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.Ms2PipUtils;
import eu.isas.peptideshaker.utils.PercolatorUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Export for PSM identifications.
 *
 * @author Dafni Skiadopoulou
 */
public class PSMIdentExport {

    /**
     * Export the PSM identifications.
     *
     * @param psmIdentifiersFile The file to write the export.
     * @param identification The identification object containing the matches.
     * @param modificationParameters The modification parameters.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void psmIdentExport(
            File psmIdentifiersFile,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {
        // Export PSM identifiers file
        waitingHandler.setWaitingText("Exporting PSMs Identifiers - Writing export");

        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

        try ( SimpleFileWriter writer = new SimpleFileWriter(psmIdentifiersFile, true)) {

            String header = String.join("\t", "PSMId", "SpectrumTitle", "SpectrumFilename", "Proteins", "Position", "Sequence", "SequenceWithMods");

            writer.writeLine(header);

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            SpectrumMatch spectrumMatch;

            //Add spectra filenames to the export
            String[] spectraFilenames = spectrumProvider.getOrderedFileNamesWithoutExtensions();
            String spectraFilenamesString = spectraFilenames[0];
            for (int i = 1; i < spectraFilenames.length; i++) {
                String fileNameWithoutExtension = spectraFilenames[i];
                spectraFilenamesString = String.join(";", spectraFilenamesString, fileNameWithoutExtension);
            }

            final String spectraFilenamesStringFinal = spectraFilenamesString;

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
                                        spectraFilenamesStringFinal,
                                        tempSpectrumMatch,
                                        peptideAssumption,
                                        processedPSMs,
                                        modificationParameters,
                                        sequenceProvider,
                                        sequenceMatchingParameters,
                                        writingSemaphore,
                                        writer
                                )
                        );
            }

        }

    }

    /**
     * Write the peptide candidates.
     *
     * @param spectrumFilenames The spectrum file names.
     * @param spectrumMatch The spectrum match.
     * @param peptideAssumption The peptide assumption.
     * @param processedPSMs The processed PSMs.
     * @param modificationParameters The modification parameters.
     * @param sequenceProvider The sequence provider.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param writingSemaphore The writing semaphore.
     * @param writer The writer.
     */
    private static void writePeptideCandidate(
            String spectrumFilenames,
            SpectrumMatch spectrumMatch,
            PeptideAssumption peptideAssumption,
            HashSet<String> processedPSMs,
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            SimpleSemaphore writingSemaphore,
            SimpleFileWriter writer
    ) {
        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        // PSM id
        long spectrumKey = spectrumMatch.getKey();
        //Peptide peptide = peptideAssumption.getPeptide();
        //long peptideKey = peptide.getMatchingKey();
        //String psmID = String.join("_", String.valueOf(spectrumKey), String.valueOf(peptideKey)); 

        String spectrumTitle = spectrumMatch.getSpectrumTitle();
        Peptide peptide = peptideAssumption.getPeptide();

        //Get MS2PIP id
        // Get peptide data
        String peptideData = Ms2PipUtils.getPeptideData(
                peptideAssumption,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );
        // Get corresponding key
        long peptideMs2PipKey = Ms2PipUtils.getPeptideKey(peptideData);
        String peptideID = Long.toString(peptideMs2PipKey);

        String psmID = String.join("_", String.valueOf(spectrumKey), peptideID);

        // Get proteins
        TreeMap<String, int[]> proteinMaps = peptide.getProteinMapping();

        StringBuilder proteins = new StringBuilder();
        StringBuilder positions = new StringBuilder();

        for (Map.Entry<String, int[]> entry : proteinMaps.entrySet()) {
            String proteinID = entry.getKey();
            int[] proteinPositions = entry.getValue();

            //proteins.append(proteinID).append(";");
            for (int i = 0; i < proteinPositions.length; i++) {
                proteins.append(proteinID).append(",");
                positions.append(proteinPositions[i]).append(",");
            }
            positions.setLength(positions.length() - 1);
            positions.append(";");
            proteins.setLength(proteins.length() - 1);
            proteins.append(";");
        }

        proteins.setLength(proteins.length() - 1);
        positions.setLength(positions.length() - 1);

        String peptideSequence = peptide.getSequence();

        String peptideSeqWithMods = PercolatorUtils.getSequenceWithModifications(
                peptide,
                modificationParameters,
                sequenceProvider,
                sequenceMatchingParameters,
                modificationFactory
        );

        // Get PSM data
        String psmData = String.join(
                "\t",
                psmID,
                spectrumTitle,
                spectrumFilenames,
                proteins.toString(),
                positions.toString(),
                peptideSequence,
                peptideSeqWithMods
        );

        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPSMs.contains(psmData)) {

            writer.writeLine(psmData);

            processedPSMs.add(psmData);

        }

        writingSemaphore.release();

    }

}
