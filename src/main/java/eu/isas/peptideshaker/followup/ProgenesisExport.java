package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class exports identifications for post-processing with Non-Linear
 * Progenesis.
 *
 * @author Marc Vaudel
 */
public class ProgenesisExport {

    /**
     * The separator (tab by default).
     */
    public static final char SEPARATOR = '\t';

    /**
     * Writes a file containing the PSMs in a Progenesis compatible format.
     *
     * @param destinationFile the destination file
     * @param sequenceProvider a sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param identification the identification
     * @param exportType the type of export
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     * @param targetedModifications the modifications of interest in case of a
     * PTM export. Ignored otherwise.
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws IOException thrown if an error occurred while writing the file
     */
    public static void writeProgenesisExport(File destinationFile, SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider, Identification identification, ExportType exportType,
            WaitingHandler waitingHandler, HashSet<String> targetedModifications, SequenceMatchingParameters sequenceMatchingPreferences)
            throws IOException {

        if (exportType == ExportType.confident_ptms) {

            if (targetedModifications == null || targetedModifications.isEmpty()) {

                throw new IllegalArgumentException("No modification provided for the Progenesis PTM export.");

            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile))) {

            writer.write("sequence");
            writer.write(SEPARATOR);
            writer.write("modif");
            writer.write(SEPARATOR);
            writer.write("score");
            writer.write(SEPARATOR);
            writer.write("main AC");
            writer.write(SEPARATOR);
            writer.write("description");
            writer.write(SEPARATOR);
            writer.write("compound");
            writer.write(SEPARATOR);
            writer.write("jobid");
            writer.write(SEPARATOR);
            writer.write("pmkey");
            writer.newLine();

            if (waitingHandler != null) {

                waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                // reset the progress bar
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

            }

            SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);
            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = psmIterator.next()) != null) {

                if (spectrumMatch.getBestPeptideAssumption() != null) {

                    PSParameter spectrumMatchParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                    if (spectrumMatchParameter.getMatchValidationLevel().isValidated()) {

                        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

                        if (exportType != ExportType.confident_ptms || isTargetedPeptide(peptide, targetedModifications)) {

                            if (!PeptideUtils.isDecoy(peptide, sequenceProvider)) {

                                if (exportType == ExportType.validated_psms) {

                                    writeSpectrumMatch(writer, spectrumMatch, proteinDetailsProvider);

                                } else {

                                    long peptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                                    PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                                    if (peptideParameter.getMatchValidationLevel().isValidated()) {

                                        if (exportType == ExportType.validated_psms_peptides) {

                                            writeSpectrumMatch(writer, spectrumMatch, proteinDetailsProvider);

                                        } else {

                                            for (long proteinMatchKey : identification.getProteinMatches(peptide)) {

                                                ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
                                                PSParameter proteinParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                                                if (proteinParameter.getMatchValidationLevel().isValidated()) {

                                                    writeSpectrumMatch(writer, spectrumMatch, proteinDetailsProvider);
                                                    break;

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (waitingHandler != null) {

                    if (waitingHandler.isRunCanceled()) {

                        return;

                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }
            }
        }
    }

    /**
     * Indicates whether the given peptide contains any of the targeted
     * modifications and if yes whether all are confidently localized.
     *
     * @param peptide the peptide of interest
     * @param targetedModifications the targeted modifications
     *
     * @return true if the peptide contains one or more of the targeted
     * modifications confidently localized
     */
    private static boolean isTargetedPeptide(Peptide peptide, HashSet<String> targetedModifications) {

        return Arrays.stream(peptide.getVariableModifications())
                .anyMatch(modificationMatch -> targetedModifications.contains(modificationMatch.getModification())
                && modificationMatch.getConfident());

    }

    /**
     * Writes the lines corresponding to a PSM in the export file in the
     * Progenesis format.
     *
     * @param writer the writer
     * @param spectrumMatch the spectrum match to export
     * @param proteinDetailsProvider the protein details provider
     * @param identification the identification
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws IOException thrown if an error occurred while writing the file
     */
    private static void writeSpectrumMatch(BufferedWriter writer, SpectrumMatch spectrumMatch, ProteinDetailsProvider proteinDetailsProvider)
            throws IOException {

        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
        PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();
        Peptide peptide = bestAssumption.getPeptide();

        for (String accession : peptide.getProteinMapping().navigableKeySet()) {

            // peptide sequence
            String sequence = peptide.getSequence();
            writer.write(sequence + SEPARATOR);

            // modifications
            HashMap<Integer, HashSet<String>> modMap = new HashMap<>();

            for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

                int site = modificationMatch.getSite();
                HashSet<String> modNames = modMap.get(site);

                if (modNames == null) {

                    modNames = new HashSet<>(1);
                    modMap.put(site, modNames);

                }

                modNames.add(modificationMatch.getModification());

            }

            for (int i = 0; i < sequence.length() + 1; i++) {

                HashSet<String> modsAtI = modMap.get(i + 1);

                if (modsAtI != null) {

                    writer.write(modsAtI.stream()
                            .sorted()
                            .collect(Collectors.joining(",")));

                }

                writer.write(':');

            }

            writer.write(SEPARATOR);

            // score
            writer.write(Double.toString(psParameter.getConfidence()));
            writer.write(SEPARATOR);

            // main AC
            writer.write(accession);
            writer.write(SEPARATOR);

            // description
            writer.write(proteinDetailsProvider.getSimpleDescription(accession));
            writer.write(SEPARATOR);

            // compound
            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getSpectrumKey());

// correct for the intensity tag introduced in the newest version of Progenesis
            int intensityIndex = spectrumTitle.indexOf(" (intensity=");

            if (intensityIndex > -1) {

                spectrumTitle = spectrumTitle.substring(0, intensityIndex);

            }

            writer.write(spectrumTitle);
            writer.write(SEPARATOR);

            // jobid
            writer.write("N/A");
            writer.write(SEPARATOR);

            // pmkey
            writer.write("N/A");
            writer.write(SEPARATOR);

            writer.newLine();

        }
    }

    /**
     * Enum of the different types of export implemented.
     */
    public enum ExportType {

        /**
         * Exports the spectra of validated PSMs of validated peptides of
         * validated proteins.
         */
        validated_psms_peptides_proteins(0, "Validated PSMs of Validated Peptides of Validated Proteins"),
        /**
         * Exports the spectra of validated PSMs of validated peptides.
         */
        validated_psms_peptides(1, "Validated PSMs of Validated Peptides"),
        /**
         * Exports the spectra of validated PSMs.
         */
        validated_psms(2, "Validated PSMs"),
        /**
         * Exports the Confidently localized PTMs of Validated PSMs of Validated
         * Peptides of Validated Proteins
         */
        confident_ptms(3, "Confidently localized PTMs of Validated PSMs of Validated Peptides of Validated Proteins");
        /**
         *
         * Index for the export type.
         */
        public int index;
        /**
         * Description of the export.
         */
        public String description;

        /**
         * Constructor.
         *
         * @param index
         */
        private ExportType(int index, String description) {
            this.index = index;
            this.description = description;
        }

        /**
         * Returns the export type corresponding to a given index.
         *
         * @param index the index of interest
         *
         * @return the export type
         */
        public static ExportType getTypeFromIndex(int index) {

            if (index == validated_psms.index) {

                return validated_psms;

            } else if (index == validated_psms_peptides.index) {

                return validated_psms_peptides;

            } else if (index == validated_psms_peptides_proteins.index) {

                return validated_psms_peptides_proteins;

            } else if (index == confident_ptms.index) {

                return confident_ptms;

            } else {

                throw new IllegalArgumentException("Export type index " + index + " not implemented.");

            }
            //Note: don't forget to add new enums in the following methods
        }

        /**
         * Returns all possibilities descriptions in an array of string. Tip:
         * the position in the array corresponds to the type index.
         *
         * @return all possibilities descriptions in an array of string
         */
        public static String[] getPossibilities() {

            return new String[]{
                validated_psms_peptides_proteins.description,
                validated_psms_peptides.description,
                validated_psms.description,
                confident_ptms.description
            };
        }

        /**
         * Returns a description of the command line arguments.
         *
         * @return a description of the command line arguments
         */
        public static String getCommandLineOptions() {

            return validated_psms_peptides_proteins.index + ": " + validated_psms_peptides_proteins.description + ", "
                    + validated_psms_peptides.index + ": " + validated_psms_peptides.description + ", "
                    + validated_psms.index + ": " + validated_psms.description + ","
                    + confident_ptms.index + ":" + confident_ptms.description + ".";
        }
    }
}
