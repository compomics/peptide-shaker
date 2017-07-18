package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public static void exportInclusionList(File destinationFile, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ArrayList<Integer> proteinFilters, ArrayList<PeptideFilterType> peptideFilters, ExportFormat exportFormat, SearchParameters searchParameters, double rtWindow,
            WaitingHandler waitingHandler, FilterPreferences filterPreferences) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        FileWriter f = new FileWriter(destinationFile);

        try {
            BufferedWriter b = new BufferedWriter(f);
            try {
                SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.setWaitingText("Inclusion List - Writing File. Please Wait...");
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());
                }

                PSParameter psParameter = new PSParameter();
                ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
                parameters.add(psParameter);
                ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

                ProteinMatch proteinMatch;
                while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                    String proteinMatchKey = proteinMatch.getKey();
                    psParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, psParameter);

                    if (!proteinFilters.contains(psParameter.getProteinInferenceClass())) {

                        ArrayList<String> peptideMatches = new ArrayList<String>();

                        for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            if (psParameter.getMatchValidationLevel().isValidated()) {
                                boolean passesFilter = true;
                                for (PeptideFilterType filterType : peptideFilters) {
                                    String sequence = Peptide.getSequence(peptideKey);
                                    if (filterType == PeptideFilterType.degenerated) {
                                        if (psParameter.getProteinInferenceClass() != PSParameter.NOT_GROUP) {
                                            passesFilter = false;
                                            break;
                                        }
                                    } else if (filterType == PeptideFilterType.miscleaved) {

                                        Integer peptideMinMissedCleavages = null;
                                        DigestionPreferences digestionPreferences = searchParameters.getDigestionPreferences();
                                        if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
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
                            for (String peptideKey : peptideMatches) {
                                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                                ArrayList<String> validatedPsms = new ArrayList<String>();
                                for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
                                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                                    if (psParameter.getMatchValidationLevel().isValidated()) {
                                        validatedPsms.add(spectrumKey);
                                    }
                                }
                                if (!validatedPsms.isEmpty()) {
                                    ArrayList<Double> retentionTimes = new ArrayList<Double>();
                                    for (String spectrumKey : validatedPsms) {
                                        retentionTimes.add(spectrumFactory.getPrecursor(spectrumKey).getRt());
                                    }
                                    for (String spectrumKey : validatedPsms) {
                                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                                            String line = getInclusionListLine(spectrumMatch, retentionTimes, rtWindow, exportFormat, searchParameters);
                                            b.write(line);
                                            b.newLine();
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
            } finally {
                b.close();
            }
        } finally {
            f.close();
        }
    }

    /**
     * Returns a line to be output in an inclusion list according to the user's
     * input.
     *
     * @param spectrumKey The key of the spectrum
     * @param retentionTimes The retention times found for this peptide
     * @param rtWindow the retention time window set by the user
     * @param exportFormat the export format to use
     * @param searchParameters the search parameters used for the search
     *
     * @return a line to be appended in the inclusion list
     * @throws Exception exception thrown whenever a problem was encountered
     * while reading the spectrum file
     */
    private static String getInclusionListLine(SpectrumMatch spectrumMatch, ArrayList<Double> retentionTimes, double rtWindow,
            ExportFormat exportFormat, SearchParameters searchParameters) throws IOException, MzMLUnmarshallerException {

        String spectrumKey = spectrumMatch.getKey();

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

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
                return precursor.getMz() + "\t" + rtMin + "\t" + rtMax;
            case ABI:
                index = (int) (0.5 * retentionTimes.size());
                double rtInMin = retentionTimes.get(index) / 60;
                return rtInMin + "\t" + precursor.getMz();
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
                    double deltaMZ = searchParameters.getPrecursorAccuracy() / 1000000 * precursor.getMz();
                    double mzMin = precursor.getMz() - deltaMZ;
                    double mzMax = precursor.getMz() + deltaMZ;
                    return rt + "," + range + "," + mzMin + "," + mzMax;
                } else { // Dalton
                    double deltaMZ = searchParameters.getPrecursorAccuracy() / spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                    double mzMin = precursor.getMz() - deltaMZ;
                    double mzMax = precursor.getMz() + deltaMZ;
                    return rt + "," + range + "," + mzMin + "," + mzMax;
                }
            case MassLynx:
                index = (int) (0.5 * retentionTimes.size());
                rt = retentionTimes.get(index);
                return precursor.getMz() + "," + rt;
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
        private ExportFormat(int index, String description, String extension) {
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
        public static ExportFormat getTypeFromIndex(int index) {
            if (index == Thermo.index) {
                return Thermo;
            } else if (index == ABI.index) {
                return ABI;
            } else if (index == Bruker.index) {
                return Bruker;
            } else if (index == MassLynx.index) {
                return MassLynx;
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
            return new String[]{
                Thermo.description,
                ABI.description,
                Bruker.description,
                MassLynx.description
            };
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
        public static File verifyFileExtension(File destinationFile, ExportFormat exportFormat) {
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
        private PeptideFilterType(int index, String description) {
            this.index = index;
            this.description = description;
        }

        /**
         * Returns the parameter type corresponding to a given index.
         *
         * @param index the index of interest
         * @return the parameter type corresponding to a given index
         */
        public static PeptideFilterType getTypeFromIndex(int index) {
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
            return new String[]{
                miscleaved.description,
                reactive.description,
                degenerated.description
            };
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
