/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Export proteins in the fasta format
 *
 * @author Marc
 */
public class FastaExport {

    
    /**
     * Exports the proteins of interest in a text file of the fasta format.
     * non validated protein mode iterates all proteins in the original fasta file (size in the sequence factory)
     * validated protein mode iterates only validated proteins (size in the identification features generator)
     * 
     * @param destinationFile the file where to write
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features generator
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel the process
     * @param accessionOnly if true only the accession of the protein will be exported, if false the entire information in Fasta format
     * 
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     */
    public static void exportFasta(File destinationFile, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, ExportType exportType, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        export(destinationFile, identification, identificationFeaturesGenerator, exportType, waitingHandler, false);
    }
    
    /**
     * Exports the accessions proteins of interest in a text file.
     * non validated protein mode iterates all proteins in the original fasta file (size in the sequence factory)
     * validated protein mode iterates only validated proteins (size in the identification features generator)
     * 
     * @param destinationFile the file where to write
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features generator
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel the process
     * 
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     */
    public static void exportAccessions(File destinationFile, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, ExportType exportType, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        export(destinationFile, identification, identificationFeaturesGenerator, exportType, waitingHandler, true);
    }
    
    
    /**
     * Exports the proteins of interest in a text file of the given format.
     * non validated protein mode iterates all proteins in the original fasta file (size in the sequence factory)
     * validated protein mode iterates only validated proteins (size in the identification features generator)
     * 
     * @param destinationFile the file where to write
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features generator
     * @param exportType the export type (see enum below)
     * @param waitingHandler waiting handler used to display progress and cancel the process
     * @param accessionOnly if true only the accession of the protein will be exported, if false the entire information in Fasta format
     * 
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     */
    public static void export(File destinationFile, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, ExportType exportType, WaitingHandler waitingHandler, boolean accessionOnly) throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();

        FileWriter f = new FileWriter(destinationFile);
        try {
            BufferedWriter b = new BufferedWriter(f);
            try {

                if (exportType == ExportType.non_validated) {

                    PSParameter psParameter = new PSParameter();
                    identification.loadProteinMatchParameters(psParameter, waitingHandler);
                    
                    for (String accession : sequenceFactory.getAccessions()) {

                        if (!SequenceFactory.isDecoy(accession)) {

                            ArrayList<String> matches = identification.getProteinMap().get(accession);

                            if (matches != null) {

                                boolean validated = false;
                                for (String match : matches) {
                                    psParameter = (PSParameter) identification.getProteinMatchParameter(match, psParameter);
                                    if (psParameter.isValidated()) {
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
                                waitingHandler.increaseSecondaryProgressValue();
                            }
                    }
                } else {

                    if (exportType == ExportType.validated_main_accession) {
                        identification.loadProteinMatches(identificationFeaturesGenerator.getValidatedProteins(), waitingHandler);
                    }

                    ArrayList<String> exported = new ArrayList<String>();

                    for (String matchKey : identificationFeaturesGenerator.getValidatedProteins()) {
                        ArrayList<String> accessions = new ArrayList<String>();
                        if (exportType == ExportType.validated_main_accession) {
                            ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
                            accessions.add(proteinMatch.getMainMatch());
                        } else if (exportType == ExportType.validated_all_accessions) {
                            accessions.addAll(Arrays.asList(ProteinMatch.getAccessions(matchKey)));
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
                                waitingHandler.increaseSecondaryProgressValue();
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
     * Writes the desired information about a given accession
     *
     * @param b the stream where to write
     * @param accession the accession of interest
     * @param sequenceFactory the sequence factory
     * @param accessionOnly indicate whether only the accession shall be written
     * or the entire protein details in fasta format
     *
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     */
    private static void writeAccession(BufferedWriter b, String accession, SequenceFactory sequenceFactory, boolean accessionOnly) throws IOException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException {
        if (accessionOnly) {
            b.write(accession);
            b.newLine();
        } else {
            b.write(sequenceFactory.getHeader(accession).toString());
            b.newLine();
            b.write(sequenceFactory.getProtein(accession).getSequence());
            b.newLine();
        }
    }

    /**
     * Enum of the different types of export implemented
     */
    public enum ExportType {

        /**
         * Exports the main accession of validated protein groups
         */
        validated_main_accession(0, "Main accession of validated protein groups"),
        /**
         * Exports all accessions of validated protein groups
         */
        validated_all_accessions(1, "All accessions of validated protein groups"),
        /**
         * Exports accessions which cannot be mapped to a protein group
         */
        non_validated(2, "Non-validated accessions");
        /**
         * index for the export type
         */
        public int index;
        /**
         * Description of the export
         */
        public String description;

        /**
         * constructor
         *
         * @param index
         */
        private ExportType(int index, String description) {
            this.index = index;
            this.description = description;
        }

        /**
         * Returns the export type corresponding to a given index
         *
         * @param index the index of interest
         * @return
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
         * @return
         */
        public static String[] getPossibilities() {
            return new String[]{validated_main_accession.description, validated_all_accessions.description, non_validated.description};
        }

        /**
         * Returns a description of the command line arguments
         *
         * @return
         */
        public static String getCommandLineOptions() {
            return validated_main_accession.index + ":" + validated_main_accession.description + " (default), "
                    + validated_all_accessions.index + ":" + validated_all_accessions.description + ", "
                    + non_validated.index + ":" + non_validated.description + ".";
        }
    }
}
