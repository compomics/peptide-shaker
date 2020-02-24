package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.gui.filtering.FilterParameters;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.io.flat.SimpleFileWriter;
import static eu.isas.peptideshaker.followup.ProgenesisExport.ExportType.values;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class exports identifications in an inclusion list.
 *
 * @author Marc Vaudel
 */
public class InclusionListExport {

    /**
     * Writes an inclusion list based on the validated PSMs of the validated
     * peptides of the validated proteins.
     *
     * @param destinationFile the file where to write the inclusion list
     * @param identification the identification object containing all matches
     * and match parameters
     * @param identificationFeaturesGenerator the identification features
     * generator calculating identification metrics on the fly
     * @param spectrumProvider the spectrum provider
     * @param proteinFilters the inclusion list protein filters
     * @param peptideFilters the inclusion list peptide filters
     * @param exportFormat the export format
     * @param searchParameters the identification parameters
     * @param rtWindow the window to use for retention time
     * @param waitingHandler waiting handler displaying progress to the user
     * (can be null)
     * @param filterPreferences the general filtering preferences of this
     * project
     *
     * @throws IOException thrown if an error occurred while writing the file
     */
    public static void exportInclusionList(
            File destinationFile,
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SpectrumProvider spectrumProvider,
            ArrayList<Integer> proteinFilters,
            ArrayList<PeptideFilterType> peptideFilters,
            ExportFormat exportFormat,
            SearchParameters searchParameters,
            double rtWindow,
            WaitingHandler waitingHandler,
            FilterParameters filterPreferences
    ) throws IOException {

        if (waitingHandler != null) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.setWaitingText("Inclusion List - Writing File. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());
        }

        try ( SimpleFileWriter writer = new SimpleFileWriter(destinationFile, false)) {

            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);

            ProteinMatch proteinMatch;
            while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                PSParameter proteinParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                if (!proteinFilters.contains(proteinParameter.getProteinInferenceGroupClass())) {

                    ArrayList<Long> peptideMatches = new ArrayList<>();

                    for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                        if (peptideParameter.getMatchValidationLevel().isValidated()) {

                            boolean passesFilter = true;

                            for (PeptideFilterType filterType : peptideFilters) {

                                String sequence = peptideMatch.getPeptide().getSequence();

                                if (filterType == PeptideFilterType.degenerated) {

                                    if (peptideParameter.getProteinInferenceGroupClass() != PSParameter.NOT_GROUP) {

                                        passesFilter = false;
                                        break;

                                    }

                                } else if (filterType == PeptideFilterType.miscleaved) {

                                    Integer peptideMinMissedCleavages = null;
                                    DigestionParameters digestionPreferences = searchParameters.getDigestionParameters();

                                    if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                                        for (Enzyme enzyme : digestionPreferences.getEnzymes()) {

                                            int tempMissedCleavages = enzyme.getNmissedCleavages(sequence);

                                            if (peptideMinMissedCleavages == null || tempMissedCleavages < peptideMinMissedCleavages) {

                                                peptideMinMissedCleavages = tempMissedCleavages;

                                            }
                                        }
                                    }

                                    if (peptideMinMissedCleavages != null && peptideMinMissedCleavages > 0) {

                                        passesFilter = false;
                                        break;

                                    }

                                } else if (filterType == PeptideFilterType.reactive) {

                                    if (sequence.contains("M")
                                            || sequence.contains("C")
                                            || sequence.contains("W")
                                            || sequence.contains("NG")
                                            || sequence.contains("DG")
                                            || sequence.contains("QG")
                                            || sequence.startsWith("N")
                                            || sequence.startsWith("Q")) {

                                        passesFilter = false;
                                        break;

                                    }
                                }
                            }

                            if (passesFilter) {

                                peptideMatches.add(peptideKey);

                            }
                        }
                    }

                    if (!peptideMatches.isEmpty()) {

                        for (long peptideKey : peptideMatches) {

                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                            ArrayList<SpectrumMatch> validatedPsms = new ArrayList<>(peptideMatch.getSpectrumCount());
                            ArrayList<Double> retentionTimes = new ArrayList<>(peptideMatch.getSpectrumCount());

                            for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);

                                if (spectrumMatch.getBestPeptideAssumption() != null) {

                                    PSParameter spectrumParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                                    if (spectrumParameter.getMatchValidationLevel().isValidated()) {

                                        validatedPsms.add(spectrumMatch);
                                        retentionTimes.add(
                                                spectrumProvider.getPrecursorRt(
                                                        spectrumMatch.getSpectrumFile(),
                                                        spectrumMatch.getSpectrumTitle()
                                                )
                                        );

                                    }
                                }
                            }

                            if (!validatedPsms.isEmpty()) {

                                for (SpectrumMatch spectrumMatch : validatedPsms) {

                                    double precursorMz = spectrumProvider.getPrecursorMz(
                                            spectrumMatch.getSpectrumFile(),
                                            spectrumMatch.getSpectrumTitle()
                                    );

                                    String line = getInclusionListLine(
                                            spectrumMatch,
                                            retentionTimes,
                                            rtWindow,
                                            precursorMz,
                                            exportFormat,
                                            searchParameters
                                    );

                                    writer.writeLine(line);

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
     * Returns a line to be output in an inclusion list according to the user's
     * input.
     *
     * @param spectrumKey The key of the spectrum
     * @param retentionTimes The retention times found for this peptide
     * @param rtWindow the retention time window set by the user
     * @param precursorMz the precursor m/z
     * @param exportFormat the export format to use
     * @param searchParameters the search parameters used for the search
     *
     * @return a line to be appended in the inclusion list
     */
    private static String getInclusionListLine(
            SpectrumMatch spectrumMatch,
            ArrayList<Double> retentionTimes,
            double rtWindow,
            double precursorMz,
            ExportFormat exportFormat,
            SearchParameters searchParameters
    ) {

        switch (exportFormat) {

            case Thermo:
                int index = (int) (0.25 * retentionTimes.size());
                double rtMin = retentionTimes.get(index) / 60;
                index = (int) (0.75 * retentionTimes.size());
                double rtMax = retentionTimes.get(index) / 60;

                if (rtMax - rtMin < rtWindow / 60) {

                    index = (int) (0.5 * retentionTimes.size());
                    rtMin = (retentionTimes.get(index) - rtWindow / 2) / 60;
                    rtMax = (retentionTimes.get(index) + rtWindow / 2) / 60;

                }

                return String.join("\t",
                        Double.toString(precursorMz),
                        Double.toString(rtMin),
                        Double.toString(rtMax)
                );

            case ABI:
                index = (int) (0.5 * retentionTimes.size());
                double rtInMin = retentionTimes.get(index) / 60;

                return String.join("\t",
                        Double.toString(rtInMin),
                        Double.toString(precursorMz)
                );

            case Bruker:
                index = (int) 0.5 * retentionTimes.size();
                double rt = retentionTimes.get(index);
                int index25 = (int) (0.25 * retentionTimes.size());
                int index75 = (int) (0.75 * retentionTimes.size());
                double range = retentionTimes.get(index75) - retentionTimes.get(index25);

                if (range < rtWindow) {

                    range = rtWindow;

                }

                if (searchParameters.getPrecursorAccuracyType() == SearchParameters.MassAccuracyType.PPM) {

                    double deltaMZ = searchParameters.getPrecursorAccuracy() / 1000000 * precursorMz;
                    double mzMin = precursorMz - deltaMZ;
                    double mzMax = precursorMz + deltaMZ;

                    return String.join(",",
                            Double.toString(rt),
                            Double.toString(range),
                            Double.toString(mzMin),
                            Double.toString(mzMax)
                    );

                } else { // Dalton

                    double deltaMZ = searchParameters.getPrecursorAccuracy() / spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();
                    double mzMin = precursorMz - deltaMZ;
                    double mzMax = precursorMz + deltaMZ;

                    return String.join(",",
                            Double.toString(rt),
                            Double.toString(range),
                            Double.toString(mzMin),
                            Double.toString(mzMax)
                    );

                }

            case MassLynx:
                index = (int) (0.5 * retentionTimes.size());
                rt = retentionTimes.get(index);

                return String.join(",",
                        Double.toString(precursorMz),
                        Double.toString(rt)
                );

            default:
                return "";

        }
    }

    /**
     * Enum of the different types of export implemented.
     */
    public enum ExportFormat {

        /**
         * Thermo format.
         */
        Thermo(0, "Thermo", "txt"),
        /**
         * ABI format.
         */
        ABI(1, "ABI", "txt"),
        /**
         * Bruker format.
         */
        Bruker(2, "Bruker", "csv"),
        /**
         * MassLynx format.
         */
        MassLynx(3, "MassLynx", "txt");
        /**
         * Index for the export type.
         */
        public int index;
        /**
         * Description of the export.
         */
        public String description;
        /**
         * The extension of the file.
         */
        public String extension;

        /**
         * Constructor.
         *
         * @param index the index number of the parameter
         * @param description the description of the parameter
         * @param extension the extension of the file
         */
        private ExportFormat(
                int index,
                String description,
                String extension
        ) {
            this.index = index;
            this.description = description;
            this.extension = extension;
        }

        /**
         * Returns the export type corresponding to a given index.
         *
         * @param index the index of interest
         * @return the export type corresponding to a given index
         */
        public static ExportFormat getTypeFromIndex(
                int index
        ) {
            for (ExportFormat format : values()) {
                if (index == format.index) {
                    return format;
                }
            }

            throw new IllegalArgumentException("Export format index " + index + "not implemented.");
            //Note: don't forget to add new enums in the following methods
        }

        /**
         * Returns all possibilities descriptions in an array of string. Tip:
         * the position in the array corresponds to the type index.
         *
         * @return all possibilities descriptions in an array of string
         */
        public static String[] getPossibilities() {
            return Arrays.stream(values())
                    .map(
                            value -> value.description
                    )
                    .toArray(String[]::new);
        }

        /**
         * Returns a description of the command line arguments.
         *
         * @return a description of the command line arguments
         */
        public static String getCommandLineOptions() {
            return Thermo.index + ": " + Thermo.description + " (default), "
                    + ABI.index + ": " + ABI.description + ", "
                    + Bruker.index + ": " + Bruker.description + ", "
                    + MassLynx.index + ": " + MassLynx.description + ".";
        }

        /**
         * Verifies that the file extension is chosen according to the
         * manufacturers specification and adds the extension if missing.
         *
         * @param destinationFile the destination file
         * @param exportFormat the export format
         * @return returns a file with updated extension
         */
        public static File verifyFileExtension(
                File destinationFile,
                ExportFormat exportFormat
        ) {
            if (!destinationFile.getName().endsWith(exportFormat.extension)) {
                return new File(destinationFile.getParent(), destinationFile.getName() + exportFormat.extension);
            }
            return destinationFile;
        }
    }

    /**
     * Enum of the peptide filters implemented.
     */
    public enum PeptideFilterType {

        /**
         * Miscleaved Peptides.
         */
        miscleaved(0, "Miscleaved Peptides"),
        /**
         * Reactive Peptides.
         */
        reactive(1, "Reactive Peptides"),
        /**
         * Degenerated Peptides.
         */
        degenerated(2, "Degenerated Peptides");
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
         * @param index the index number of the parameter
         * @param description the description of the parameter
         */
        private PeptideFilterType(
                int index,
                String description
        ) {
            this.index = index;
            this.description = description;
        }

        /**
         * Returns the parameter type corresponding to a given index.
         *
         * @param index the index of interest
         * @return the parameter type corresponding to a given index
         */
        public static PeptideFilterType getTypeFromIndex(
                int index
        ) {
            if (index == miscleaved.index) {
                return miscleaved;
            } else if (index == reactive.index) {
                return reactive;
            } else if (index == degenerated.index) {
                return degenerated;
            } else {
                throw new IllegalArgumentException("Export format index " + index + "not implemented.");
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
            return Arrays.stream(values())
                    .map(
                            value -> value.description
                    )
                    .toArray(String[]::new);
        }

        /**
         * Returns a description of the command line arguments.
         *
         * @return a description of the command line arguments
         */
        public static String getCommandLineOptions() {
            return miscleaved.index + ": " + miscleaved.description + ", "
                    + reactive.index + ": " + reactive.description + ", "
                    + degenerated.index + ": " + degenerated.description + ".";
        }
    }

    /**
     * Returns a description of the command line arguments for the protein
     * filters.
     *
     * @return a description of the command line arguments
     */
    public static String getProteinFiltersCommandLineOptions() {
        return PSParameter.RELATED + ": " + PSParameter.getProteinInferenceClassAsString(PSParameter.RELATED) + ", "
                + PSParameter.RELATED_AND_UNRELATED + ": " + PSParameter.getProteinInferenceClassAsString(PSParameter.RELATED_AND_UNRELATED) + ", "
                + PSParameter.UNRELATED + ": " + PSParameter.getProteinInferenceClassAsString(PSParameter.UNRELATED) + ".";
    }
}
