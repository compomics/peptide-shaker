package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Export proteins in the FASTA format.
 *
 * @author Marc Vaudel
 */
public class FastaExport {

    /**
     * Exports the proteins of interest in a text file of the given format. Non
     * validated protein mode iterates all proteins in the original FASTA file
     * (size in the sequence factory). Validated protein mode iterates only
     * validated proteins (size in the identification features generator).
     *
     * @param destinationFile the file where to write
     * @param fastaFile the original fasta file
     * @param sequenceProvider the sequence provider
     * @param identification the identification
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel
     * the process
     * @param accessionOnly if true only the accession of the protein will be
     * exported, if false the entire information in FASTA format
     *
     * @throws IOException thrown if an error occurs while writing the file.
     */
    public static void export(File destinationFile, File fastaFile, SequenceProvider sequenceProvider,
            Identification identification, ExportType exportType, WaitingHandler waitingHandler, boolean accessionOnly) throws IOException {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(destinationFile))) {

            for (String accession : sequenceProvider.getAccessions()) {

                if (include(accession, exportType, identification)) {

                    if (accessionOnly) {

                        bw.write(accession);
                        bw.newLine();

                    } else {

                        bw.write(sequenceProvider.getHeader(accession));
                        bw.newLine();
                        bw.write(sequenceProvider.getSequence(accession));
                        bw.newLine();

                    }
                }
            }
        }
    }

    /**
     * Returns a boolean indicating whether the given accession should be
     * included in the export.
     *
     * @param accession the accession
     * @param exportType the export type
     * @param identification the identification
     *
     * @return a boolean indicating whether the given accession should be
     * included in the export
     */
    private static boolean include(String accession, ExportType exportType, Identification identification) {

        switch (exportType) {

            case non_validated:
                return identification.getProteinMap().get(accession).stream()
                        .map(key -> (PSParameter) identification.getProteinMatch(key).getUrParam(PSParameter.dummy))
                        .allMatch(psParameter -> !psParameter.getMatchValidationLevel().isValidated());

            case validated_all_accessions:
                return identification.getProteinMap().get(accession).stream()
                        .map(key -> (PSParameter) identification.getProteinMatch(key).getUrParam(PSParameter.dummy))
                        .anyMatch(psParameter -> psParameter.getMatchValidationLevel().isValidated());

            case validated_main_accession:
                return identification.getProteinMap().get(accession).stream()
                        .map(key -> identification.getProteinMatch(key))
                        .filter(proteinMatch -> proteinMatch.getLeadingAccession().equals(accession))
                        .map(proteinMatch -> (PSParameter) proteinMatch.getUrParam(PSParameter.dummy))
                        .anyMatch(psParameter -> psParameter.getMatchValidationLevel().isValidated());

            default:
                throw new UnsupportedOperationException("Export not implemented for " + exportType + ".");

        }
    }

    /**
     * Enum of the different types of export implemented.
     */
    public enum ExportType {

        /**
         * Exports the main accession of validated protein groups.
         */
        validated_main_accession(0, "Main Accession of Validated Protein Groups"),
        /**
         * Exports all accessions of validated protein groups.
         */
        validated_all_accessions(1, "All Accessions of Validated Protein Groups"),
        /**
         * Exports accessions which cannot be mapped to a protein group.
         */
        non_validated(2, "Non-Validated Accessions");
        /**
         * Index for the export type.
         */
        public final int index;
        /**
         * Description of the export.
         */
        public final String description;

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
         * @return the export type
         */
        public static ExportType getTypeFromIndex(int index) {

            if (index == validated_main_accession.index) {

                return validated_main_accession;

            } else if (index == validated_all_accessions.index) {

                return validated_all_accessions;

            } else if (index == non_validated.index) {

                return non_validated;

            } else {

                throw new IllegalArgumentException("Export type " + index + " not implemented.");

            }
        }

        /**
         * Returns all possibilities descriptions in an array of string. Tip:
         * the position in the array corresponds to the type index.
         *
         * @return all possibilities descriptions in an array of string
         */
        public static String[] getPossibilities() {
            return new String[]{validated_main_accession.description, validated_all_accessions.description, non_validated.description};
        }

        /**
         * Returns a description of the command line arguments.
         *
         * @return a description of the command line arguments
         */
        public static String getCommandLineOptions() {
            return validated_main_accession.index + ": " + validated_main_accession.description + " (default), "
                    + validated_all_accessions.index + ": " + validated_all_accessions.description + ", "
                    + non_validated.index + ": " + non_validated.description + ".";
        }
    }
}
