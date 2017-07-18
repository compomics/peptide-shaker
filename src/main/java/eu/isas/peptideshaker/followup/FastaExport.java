package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Export proteins in the FASTA format.
 *
 * @author Marc Vaudel
 */
public class FastaExport {

    /**
     * Exports the proteins of interest in a text file of the FASTA format. Non
     * validated protein mode iterates all proteins in the original FASTA file
     * (size in the sequence factory). Validated protein mode iterates only
     * validated proteins (size in the identification features generator).
     *
     * @param destinationFile the file where to write
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel
     * the process
     * @param filterPreferences the filter preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public static void exportFasta(File destinationFile, Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, ExportType exportType, WaitingHandler waitingHandler, FilterPreferences filterPreferences)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        export(destinationFile, identification, identificationFeaturesGenerator, exportType, waitingHandler, filterPreferences, false);
    }

    /**
     * Exports the accessions proteins of interest in a text file. Non validated
     * protein mode iterates all proteins in the original FASTA file (size in
     * the sequence factory). Validated protein mode iterates only validated
     * proteins (size in the identification features generator).
     *
     * @param destinationFile the file where to write
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel
     * the process
     * @param filterPreferences the filter preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public static void exportAccessions(File destinationFile, Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, ExportType exportType, WaitingHandler waitingHandler, FilterPreferences filterPreferences)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        export(destinationFile, identification, identificationFeaturesGenerator, exportType, waitingHandler, filterPreferences, true);
    }

    /**
     * Exports the proteins of interest in a text file of the given format. Non
     * validated protein mode iterates all proteins in the original FASTA file
     * (size in the sequence factory). Validated protein mode iterates only
     * validated proteins (size in the identification features generator).
     *
     * @param destinationFile the file where to write
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel
     * the process
     * @param filterPreferences the filter preferences
     * @param accessionOnly if true only the accession of the protein will be
     * exported, if false the entire information in FASTA format
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public static void export(File destinationFile, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ExportType exportType, WaitingHandler waitingHandler, FilterPreferences filterPreferences, boolean accessionOnly) throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        FileWriter f = new FileWriter(destinationFile);

        try {

            BufferedWriter b = new BufferedWriter(f);

            try {
                if (exportType == ExportType.non_validated) {

                    PSParameter psParameter = new PSParameter();
                    identification.loadProteinMatchParameters(psParameter, waitingHandler, false);

                    for (String accession : sequenceFactory.getAccessions()) {

                        if (!sequenceFactory.isDecoyAccession(accession)) {

                            ArrayList<String> matches = new ArrayList<String>(identification.getProteinMap().get(accession));

                            if (matches != null) {
                                boolean validated = false;
                                for (String match : matches) {
                                    psParameter = (PSParameter) identification.getProteinMatchParameter(match, psParameter);
                                    if (psParameter.getMatchValidationLevel().isValidated()) {
                                        validated = true;
                                        break;
                                    }
                                }
                                if (!validated) {
                                    writeAccession(b, accession, sequenceFactory, accessionOnly);
                                }
                            }
                        }
                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                break;
                            }
                            waitingHandler.increaseSecondaryProgressCounter();
                        }
                    }
                } else {

                    ArrayList<String> exported = new ArrayList<String>();

                    ArrayList<String> proteinMatches = identificationFeaturesGenerator.getValidatedProteins(waitingHandler, filterPreferences);
                    ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(proteinMatches, null, false, null, false, null, waitingHandler);
                    while (proteinMatchesIterator.hasNext()) {
                        ProteinMatch proteinMatch = proteinMatchesIterator.next();
                        ArrayList<String> accessions = new ArrayList<String>();
                        if (exportType == ExportType.validated_main_accession) {
                            accessions.add(proteinMatch.getMainMatch());
                        } else if (exportType == ExportType.validated_all_accessions) {
                            accessions.addAll(proteinMatch.getTheoreticProteinsAccessions());
                        }
                        for (String accession : accessions) {
                            if (!exported.contains(accession)) {
                                writeAccession(b, accession, sequenceFactory, accessionOnly);
                                exported.add(accession);
                            }
                        }
                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                break;
                            }
                            waitingHandler.increaseSecondaryProgressCounter();
                        }
                    }
                }
            } finally {
                b.close();
            }
        } finally {
            f.close();
        }

    }

    /**
     * Writes the desired information about a given accession.
     *
     * @param b the stream where to write
     * @param accession the accession of interest
     * @param sequenceFactory the sequence factory
     * @param accessionOnly indicate whether only the accession shall be written
     * or the entire protein details in FASTA format
     *
     * @throws IOException thrown if an IOException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws FileNotFoundException thrown if a FileNotFoundException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    private static void writeAccession(BufferedWriter b, String accession, SequenceFactory sequenceFactory, boolean accessionOnly)
            throws IOException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException {

        if (accessionOnly) {
            b.write(accession);
            b.newLine();
        } else {
            b.write(sequenceFactory.getHeader(accession).getRawHeader());
            b.newLine();
            b.write(sequenceFactory.getProtein(accession).getSequence());
            b.newLine();
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
