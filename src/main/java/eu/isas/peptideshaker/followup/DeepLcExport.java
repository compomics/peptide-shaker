package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
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
import com.compomics.util.pride.CvTerm;
import com.compomics.util.waiting.WaitingHandler;
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
     * Exports DeepLC training files for each of the spectrum files. Returns an ArrayList of the files exported.
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

        spectrumIdentificationMap.entrySet()
                .parallelStream()
                .forEach(
                        entry -> deepLcExport(
                                getDestinationFile(
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

        return spectrumIdentificationMap.keySet()
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
                );
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
     * Exports a DeepLC training file for the given spectrum file.
     *
     * @param destinationFile The file where to write the export.
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
            HashSet<Long> keys,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        try (SimpleFileWriter writer = new SimpleFileWriter(destinationFile, true)) {

            writer.writeLine("seq,modifications,rt");

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

                // Export only validated hits
                PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                if (psParameter.getMatchValidationLevel().isValidated()) {

                    StringBuilder line = new StringBuilder();

                    // Peptide sequence
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                    Peptide peptide = peptideAssumption.getPeptide();
                    String peptideSequence = peptide.getSequence();

                    line.append(peptideSequence)
                            .append(',');

                    // Fixed modifications
                    String[] fixedModifications = peptide.getFixedModifications(modificationParameters, sequenceProvider, sequenceMatchingParameters);

                    StringBuilder modificationSites = new StringBuilder();

                    for (int i = 0; i < fixedModifications.length; i++) {

                        if (fixedModifications[i] != null) {

                            int site = i;

                            if (i == 0) {

                                site = 1;

                            } else if (i == fixedModifications.length - 1) {

                                site = peptideSequence.length();

                            }

                            String modName = fixedModifications[i];
                            Modification modification = modificationFactory.getModification(modName);
                            CvTerm cvTerm = modification.getUnimodCvTerm();
                            String unimodName = cvTerm.getName();

                            if (modificationSites.length() > 0) {

                                modificationSites.append('|');

                            }

                            modificationSites.append(site)
                                    .append('|')
                                    .append(unimodName);

                        }
                    }

                    // Variable modifications
                    for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

                        int site = modificationMatch.getSite();

                        String modName = modificationMatch.getModification();
                        Modification modification = modificationFactory.getModification(modName);
                        CvTerm cvTerm = modification.getUnimodCvTerm();
                        String unimodName = cvTerm.getName();

                        if (modificationSites.length() > 0) {

                            modificationSites.append('|');

                        }

                        modificationSites.append(site)
                                .append('|')
                                .append(unimodName);

                    }

                    line.append(modificationSites)
                            .append(',');

                    // Measured retention time
                    String spectrumFile = spectrumMatch.getSpectrumFile();
                    String spectrumTitle = spectrumMatch.getSpectrumTitle();
                    Precursor precursor = spectrumProvider.getPrecursor(spectrumFile, spectrumTitle);

                    line.append(precursor.rt);

                    // Write to file
                    writer.writeLine(line.toString());

                }
            }
        }
    }
}
