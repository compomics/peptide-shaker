package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.mass_spectrometry.mgf.MgfFileIterator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class can be used to export spectra.
 *
 * @author Marc Vaudel
 */
public class SpectrumExporter {

    /**
     * The identification.
     */
    private final Identification identification;
    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    /**
     * Constructor.
     *
     * @param identification The identification of this project
     */
    public SpectrumExporter(Identification identification) {

        this.identification = identification;

    }

    /**
     * Exports the spectra from different categories of PSMs according to the
     * export type. Export format is mgf.
     *
     * @param destinationFolder the folder where to write the spectra
     * @param waitingHandler waiting handler used to display progress and cancel
     * the process. Can be null.
     * @param exportType the type of PSM to export
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @return ArrayList files containing exported spectra
     * 
     * @throws IOException thrown if an error occurred while writing the file
     */
    public ArrayList<File> exportSpectra(File destinationFolder, WaitingHandler waitingHandler, ExportType exportType, SequenceMatchingParameters sequenceMatchingPreferences)
            throws IOException {

        ArrayList<String> spectrumFileNames = spectrumFactory.getMgfFileNames();
        ArrayList<File> destinationFiles = new ArrayList<File>();

        for (int i = 0; i < spectrumFileNames.size(); i++) {

            String fileName = spectrumFileNames.get(i);
            File spectrumFile = spectrumFactory.getMgfFileFromName(fileName);

            if (waitingHandler != null) {

                waitingHandler.setWaitingText("Exporting Spectra. Please Wait... (" + (i + 1) + "/" + spectrumFileNames.size() + ")");

                // reset the progress bar
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(spectrumFactory.getSpectrumTitles(fileName).size());
            }

            MgfFileIterator mgfFileIterator = new MgfFileIterator(spectrumFile);
            File destinationFile = getDestinationFile(destinationFolder, fileName, exportType);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(destinationFile))) {

                while (mgfFileIterator.hasNext()) {

                    Spectrum spectrum = mgfFileIterator.next();
                    String spectrumKey = spectrum.getSpectrumKey();

                    if (include(spectrumKey, identification, sequenceMatchingPreferences, exportType)) {

                        bw.write(spectrum.asMgf());

                    }

                    if (waitingHandler != null) {

                        if (waitingHandler.isRunCanceled()) {

                            return null;

                        }

                        waitingHandler.increaseSecondaryProgressCounter();

                    }
                }
            }
            destinationFiles.add(destinationFile);
        }
        return destinationFiles;
    }

    /**
     * Returns the destination file for the given export.
     *
     * @param destinationFolder the destination folder
     * @param originalFileName the original file name
     * @param exportType the export type
     *
     * @return the destination file
     */
    public static File getDestinationFile(File destinationFolder, String originalFileName, ExportType exportType) {

        return new File(destinationFolder, Util.removeExtension(originalFileName) + getSuffix(exportType) + ".mgf");

    }

    /**
     * Indicates whether a spectrum should be exported.
     * 
     * @param spectrumKey the key of the spectrum
     * @param identification the identification object
     * @param sequenceMatchingParameters the sequence matching preferences
     * @param exportType the export type
     * 
     * @return a boolean indicating whether a spectrum should be exported
     */
    public static boolean include(String spectrumKey, Identification identification, SequenceMatchingParameters sequenceMatchingParameters, ExportType exportType) {

        long spectrumMatchKey = ExperimentObject.asLong(spectrumKey);
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumMatchKey);

        switch (exportType) {

            case non_validated_psms:

                return spectrumMatch == null || !((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated();

            case non_validated_peptides:

                if (spectrumMatch == null || spectrumMatch.getBestPeptideAssumption() == null) {

                    return true;

                }

                long peptideMatchKey = spectrumMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingParameters);
                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideMatchKey);

                return !((PSParameter) peptideMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated();

            case non_validated_proteins:

                if (spectrumMatch == null || spectrumMatch.getBestPeptideAssumption() == null) {

                    return true;

                }

                return !identification.getProteinMatches(spectrumMatch.getBestPeptideAssumption().getPeptide().getKey()).stream()
                        .anyMatch(key -> ((PSParameter) identification.getProteinMatch(key).getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated());

            case validated_psms:

                return spectrumMatch != null && ((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated();

            case validated_psms_peptides:

                if (spectrumMatch == null || spectrumMatch.getBestPeptideAssumption() == null || !((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated()) {

                    return false;

                }

                peptideMatchKey = spectrumMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingParameters);
                peptideMatch = identification.getPeptideMatch(peptideMatchKey);

                return ((PSParameter) peptideMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated();

            case validated_psms_peptides_proteins:

                if (spectrumMatch == null || spectrumMatch.getBestPeptideAssumption() == null || !((PSParameter) spectrumMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated()) {

                    return false;

                }

                peptideMatchKey = spectrumMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingParameters);
                peptideMatch = identification.getPeptideMatch(peptideMatchKey);

                if (!((PSParameter) peptideMatch.getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated()) {

                    return false;

                }

                return !identification.getProteinMatches(spectrumMatch.getBestPeptideAssumption().getPeptide().getKey()).stream()
                        .anyMatch(key -> ((PSParameter) identification.getProteinMatch(key).getUrParam(PSParameter.dummy))
                        .getMatchValidationLevel().isValidated());

            default:
                throw new UnsupportedOperationException("Export " + exportType + " not implemented.");
        }
    }

    /**
     * Returns the suffix for a spectrum file name.
     *
     * @param exportType the type of PSM to be exported
     * @return the suffix for a spectrum file name
     */
    public static String getSuffix(ExportType exportType) {

        switch (exportType) {

            case non_validated_psms:
                return "_non_validated_PSMs";

            case non_validated_peptides:
                return "_non_validated_peptides";

            case non_validated_proteins:
                return "_non_validated_proteins";

            case validated_psms:
                return "_validated_PSMs";

            case validated_psms_peptides:
                return "_validated_PSMs-peptides";

            case validated_psms_peptides_proteins:
                return "_validated_PSMs-peptides-proteins";

            default:
                throw new IllegalArgumentException("Export type " + exportType + " not supported.");
        }
    }

    /**
     * Enum of the different types of export implemented.
     */
    public enum ExportType {

        /**
         * Exports the spectra of non-validated PSMs.
         */
        non_validated_psms(0, "Spectra of Non-Validated PSMs"),
        /**
         * Exports the spectra of PSMs of non-validated peptides.
         */
        non_validated_peptides(1, "Spectra of Non-Validated Peptides"),
        /**
         * Exports the spectra of PSMs of non-validated proteins.
         */
        non_validated_proteins(2, "Spectra of Non-Validated Proteins"),
        /**
         * Exports the spectra of validated PSMs.
         */
        validated_psms(3, "Spectra of Validated PSMs"),
        /**
         * Exports the spectra of validated PSMs of validated peptides.
         */
        validated_psms_peptides(4, "Spectra of Validated PSMs of Validated Peptides"),
        /**
         * Exports the spectra of validated PSMs of validated peptides of
         * validated proteins.
         */
        validated_psms_peptides_proteins(5, "Spectra of validated PSMs of Validated Peptides of Validated Proteins");
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
         * @return the export type corresponding to a given index
         */
        public static ExportType getTypeFromIndex(int index) {

            if (index == non_validated_psms.index) {

                return non_validated_psms;

            } else if (index == non_validated_peptides.index) {

                return non_validated_peptides;

            } else if (index == non_validated_proteins.index) {

                return non_validated_proteins;

            } else if (index == non_validated_peptides.index) {

                return non_validated_peptides;

            } else if (index == validated_psms.index) {

                return validated_psms;

            } else if (index == validated_psms_peptides.index) {

                return validated_psms_peptides;

            } else if (index == validated_psms_peptides_proteins.index) {

                return validated_psms_peptides_proteins;

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
                non_validated_psms.description,
                non_validated_peptides.description,
                non_validated_proteins.description,
                validated_psms.description,
                validated_psms_peptides.description,
                validated_psms_peptides_proteins.description
            };
        }

        /**
         * Returns a description of the command line arguments.
         *
         * @return a description of the command line arguments
         */
        public static String getCommandLineOptions() {

            return non_validated_psms.index + ": " + non_validated_psms.description + " (default), "
                    + non_validated_peptides.index + ": " + non_validated_peptides.description + ", "
                    + non_validated_proteins.index + ": " + non_validated_proteins.description + ", "
                    + validated_psms.index + ": " + validated_psms.description + ", "
                    + validated_psms_peptides.index + ": " + validated_psms_peptides.description + ", "
                    + validated_psms_peptides_proteins.index + ": " + validated_psms_peptides_proteins.description + ".";
        }
    }
}
