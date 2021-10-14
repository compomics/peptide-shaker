package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.io.flat.SimpleFileWriter;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.Ms2PipUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Export training files for ms2pip.
 *
 * @author Marc Vaudel
 * @author Dafni Skiadopoulou
 */
public class Ms2PipExport {

    /**
     * Exports ms2pip config and peprec files. Returns an ArrayList of the files
     * written.
     *
     * @param peprecFile The file where to write the peptides.
     * @param models The names of the models to write config files for.
     * @param identification The identification object containing the matches.
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     *
     * @return An ArrayList of the files exported.
     */
    public static ArrayList<File> ms2pipExport(
            File peprecFile,
            String[] models,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        File destinationFolder = peprecFile.getParentFile();

        // reset the progress bar
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ms2pipExport(
                peprecFile,
                identification,
                searchParameters.getModificationParameters(),
                sequenceMatchingParameters,
                sequenceProvider,
                spectrumProvider,
                waitingHandler
        );

        ArrayList<File> configFiles = new ArrayList<>(models.length);

        for (String model : models) {

            File configFile = models.length == 1 ? new File(destinationFolder, "config.txt") : new File(destinationFolder, model + "_config.txt");

            writeConfigFile(configFile, model, searchParameters);

        }

        configFiles.add(peprecFile);

        return configFiles;

    }

    /**
     * Writes a config file for a given model.
     *
     * @param configFile The file where to write.
     * @param model The name of the model.
     * @param searchParameters The search parameters.
     */
    public static void writeConfigFile(
            File configFile,
            String model,
            SearchParameters searchParameters
    ) {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        try (SimpleFileWriter writer = new SimpleFileWriter(configFile, false)) {

            writer.writeLine("model=" + model);
            writer.writeLine("out=mgf");
            writer.writeLine("frag_error=" + searchParameters.getFragmentIonAccuracyInDaltons());

            for (String modName : searchParameters.getModificationParameters().getAllModifications()) {

                Modification modification = modificationFactory.getModification(modName);

                CvTerm cvTerm = modification.getUnimodCvTerm();

                if (cvTerm == null) {

                    throw new IllegalArgumentException("No Unimod id found for modification " + modName + ".");

                }

                String unimodName = cvTerm.getName();
                double mass = modification.getMass();

                ModificationType modificationType = modification.getModificationType();

                switch (modificationType) {

                    case modaa:
                    case modcaa_peptide:
                    case modcaa_protein:
                    case modnaa_peptide:
                    case modnaa_protein:

                        for (Character targetAa : modification.getPattern().getAminoAcidsAtTarget()) {

                            writer.writeLine(
                                    String.join("",
                                            "ptm=", unimodName, "," + Double.toString(mass), ",opt,", targetAa.toString()
                                    )
                            );

                        }

                        break;

                    case modn_peptide:
                    case modn_protein:

                        writer.writeLine(
                                String.join("",
                                        "ptm=", unimodName, "," + Double.toString(mass), ",opt,N-term"
                                )
                        );

                        break;

                    case modc_peptide:
                    case modc_protein:

                        writer.writeLine(
                                String.join("",
                                        "ptm=", unimodName, "," + Double.toString(mass), ",opt,C-term"
                                )
                        );

                        break;

                    default:

                        throw new UnsupportedOperationException("Modification type " + modificationType + " not supported.");

                }
            }
        }
    }

    /**
     * Exports a ms2pip training file for the given spectrum file.
     *
     * @param peprecFile The file where to write the export.
     * @param identification The identification object containing the matches.
     * @param modificationParameters The modification parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     */
    public static void ms2pipExport(
            File peprecFile,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        try (SimpleFileWriter writer = new SimpleFileWriter(peprecFile, true)) {

            writer.writeLine("spec_id modifications peptide charge");

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);

            HashSet<Long> processedPeptideKeys = new HashSet<>();
            SimpleSemaphore writingSemaphore = new SimpleSemaphore(1);

            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                // Display progress
                if (waitingHandler != null) {

                    waitingHandler.increaseSecondaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }
                }

                // Export all candidate peptides
                spectrumMatch.getAllPeptideAssumptions()
                        .parallel()
                        .forEach(
                                peptideAssumption -> writePeptideCandidate(
                                        peptideAssumption,
                                        modificationParameters,
                                        sequenceProvider,
                                        sequenceMatchingParameters,
                                        modificationFactory,
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
            ModificationParameters modificationParameters,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationFactory modificationFactory,
            HashSet<Long> processedPeptides,
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

        // Export if not done already
        writingSemaphore.acquire();

        if (!processedPeptides.contains(peptideKey)) {

            String line = String.join(" ", Long.toString(peptideKey), peptideData);

            writer.writeLine(line);
            
            processedPeptides.add(peptideKey);

        }

        writingSemaphore.release();

    }
}
