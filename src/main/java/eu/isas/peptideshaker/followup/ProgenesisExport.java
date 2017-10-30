package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
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
import java.util.HashMap;
import java.util.HashSet;

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
    public static final String SEPARATOR = "\t";

    /**
     * Writes a file containing the PSMs in a Progenesis compatible format.
     * Note: proteins must be set for every exported peptide.
     *
     * @param destinationFile the destination file
     * @param identification the identification
     * @param exportType the type of export
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     * @param targetedPTMs the PTMs of interest in case of a PTM export. Ignored
     * otherwise.
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    public static void writeProgenesisExport(File destinationFile, Identification identification, ExportType exportType,
            WaitingHandler waitingHandler, ArrayList<String> targetedPTMs, SequenceMatchingParameters sequenceMatchingPreferences)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        if (exportType == ExportType.confident_ptms) {
            if (targetedPTMs == null || targetedPTMs.isEmpty()) {
                throw new IllegalArgumentException("No modification provided for the Progenesis PTM export.");
            }
        }

        PSParameter psParameter = new PSParameter();

        if (exportType == ExportType.validated_psms_peptides || exportType == ExportType.validated_psms_peptides_proteins || exportType == ExportType.confident_ptms) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Progenesis Export - Loading Peptides. Please Wait...");
            }
            identification.loadObjects(PeptideMatch.class, waitingHandler, true);
        }
        if (exportType == ExportType.validated_psms_peptides_proteins || exportType == ExportType.confident_ptms) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Progenesis Export - Loading Proteins. Please Wait...");
            }
            identification.loadObjects(ProteinMatch.class, waitingHandler, true);
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            return;
        }

        FileWriter f = new FileWriter(destinationFile);
        try {
            BufferedWriter writer = new BufferedWriter(f);
            try {
                writer.write("sequence" + SEPARATOR);
                writer.write("modif" + SEPARATOR);
                writer.write("score" + SEPARATOR);
                writer.write("main AC" + SEPARATOR);
                writer.write("description" + SEPARATOR);
                writer.write("compound" + SEPARATOR);
                writer.write("jobid" + SEPARATOR);
                writer.write("pmkey" + SEPARATOR);
                writer.newLine();


                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Loading PSMs. Please Wait...");
                }
                identification.loadObjects(SpectrumMatch.class, waitingHandler, true);
                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                SpectrumMatchesIterator psmIterator = identification.getPsmIterator(waitingHandler);
                    SpectrumMatch spectrumMatch;
                while ((spectrumMatch = psmIterator.next()) != null) {

                    String spectrumKey = spectrumMatch.getKey();

                    if (identification.matchExists(spectrumKey)) {
                        psParameter = (PSParameter)spectrumMatch.getUrParam(psParameter);

                        if (psParameter.getMatchValidationLevel().isValidated()) {

                            if (spectrumMatch.getBestPeptideAssumption() != null) {
                                Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

                                if (exportType != ExportType.confident_ptms || isTargetedPeptide(peptide, targetedPTMs)) {

                                    boolean decoy = false;
                                    for (String protein : peptide.getParentProteins(sequenceMatchingPreferences)) {
                                        if (SequenceFactory.getInstance().isDecoyAccession(protein)) {
                                            decoy = true;
                                            break;
                                        }
                                    }
                                    if (!decoy) {
                                        if (exportType == ExportType.validated_psms) {
                                            writePsm(writer, spectrumKey, identification, sequenceMatchingPreferences);
                                        } else {
                                            String peptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                                            psParameter = (PSParameter)((PeptideMatch)identification.retrieveObject(peptideKey)).getUrParam(psParameter);
                                            if (psParameter.getMatchValidationLevel().isValidated()) {
                                                if (exportType == ExportType.validated_psms_peptides) {
                                                    writePsm(writer, spectrumKey, identification, sequenceMatchingPreferences);
                                                } else {
                                                    ArrayList<String> accessions = new ArrayList<>();
                                                    for (String accession : peptide.getParentProteins(sequenceMatchingPreferences)) {
                                                        HashSet<String> groups = identification.getProteinMap().get(accession);
                                                        if (groups != null) {
                                                            for (String group : groups) {
                                                                psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(group)).getUrParam(psParameter);
                                                                if (psParameter.getMatchValidationLevel().isValidated()) {
                                                                    for (String groupAccession : ProteinMatch.getAccessions(group)) {
                                                                        if (!accessions.contains(groupAccession)) {
                                                                            accessions.add(groupAccession);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (!accessions.isEmpty()) {
                                                        writePsm(writer, spectrumKey, accessions, identification, sequenceMatchingPreferences);
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
            } finally {
                writer.close();
            }
        } finally {
            f.close();
        }
    }

    /**
     * Indicates whether the given peptide contains any of the targeted PTMs and
     * if yes whether all are confidently localized.
     *
     * @param peptide the peptide of interest
     * @param targetedPTMs the targeted PTMs
     *
     * @return true if the peptide contains one or more of the targeted PTMs and
     * false if one of the targeted PTMs is not confidently localized
     */
    private static boolean isTargetedPeptide(Peptide peptide, ArrayList<String> targetedPTMs) {
        boolean found = false, confident = true;
        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                if (targetedPTMs.contains(modificationMatch.getModification())) {
                    found = true;
                    if (!modificationMatch.getConfident()) {
                        confident = false;
                        break;
                    }
                }
            }
        }
        return found && confident;
    }

    /**
     * Writes the lines corresponding to a PSM in the export file in the
     * Progenesis format.
     *
     * @param writer the writer
     * @param spectrumKey the key of the PSM to export
     * @param identification the identification
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    private static void writePsm(BufferedWriter writer, String spectrumKey, Identification identification, SequenceMatchingParameters sequenceMatchingPreferences)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        writePsm(writer, spectrumKey, null, identification, sequenceMatchingPreferences);
    }

    /**
     * Writes the lines corresponding to a PSM in the export file in the
     * Progenesis format. Note: proteins must be set for every exported peptide.
     *
     * @param writer the writer
     * @param spectrumKey the key of the PSM to export
     * @param accessions the accessions corresponding to that peptide according
     * to protein inference. If null all proteins will be reported.
     * @param identification the identification
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    private static void writePsm(BufferedWriter writer, String spectrumKey, ArrayList<String> accessions, Identification identification, SequenceMatchingParameters sequenceMatchingPreferences)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        SpectrumMatch spectrumMatch = (SpectrumMatch)identification.retrieveObject(spectrumKey);
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter)spectrumMatch.getUrParam(psParameter);
        PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();

        if (accessions == null) {
            accessions = bestAssumption.getPeptide().getParentProteins(sequenceMatchingPreferences);
        }

        for (String protein : accessions) {

            // peptide sequence
            writer.write(bestAssumption.getPeptide().getSequence() + SEPARATOR);

            // modifications
            HashMap<String, ArrayList<Integer>> modMap = new HashMap<>();
            Peptide peptide = bestAssumption.getPeptide();
            if (peptide.isModified()) {
                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                    if (modificationMatch.getVariable()) {
                        if (!modMap.containsKey(modificationMatch.getModification())) {
                            modMap.put(modificationMatch.getModification(), new ArrayList<>());
                        }
                        modMap.get(modificationMatch.getModification()).add(modificationMatch.getModificationSite());
                    }
                }
            }

            ArrayList<String> mods = new ArrayList<>(modMap.keySet());

            for (int i = 0; i < bestAssumption.getPeptide().getSequence().length() + 1; i++) {

                String allMods = "";

                for (int k = 0; k < mods.size(); k++) {

                    String tempMod = mods.get(k);

                    if (modMap.get(tempMod).contains(Integer.valueOf(i))) {

                        if (allMods.length() > 0) {
                            allMods += ", ";
                        }

                        allMods += tempMod;
                    }
                }

                writer.write(allMods + ":");
            }

            writer.write(SEPARATOR);

            // score
            writer.write(psParameter.getPsmConfidence() + SEPARATOR);

            // main AC
            writer.write(protein + SEPARATOR);

            // description
            String description = SequenceFactory.getInstance().getHeader(protein).getSimpleProteinDescription();
            writer.write(description + SEPARATOR);

            // compound
            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
            // correct for the intensity tag introduced in the newest version of Progenesis
            int intensityIndex = spectrumTitle.indexOf(" (intensity=");
            if (intensityIndex > -1) {
                spectrumTitle = spectrumTitle.substring(0, intensityIndex);
            }
            writer.write(spectrumTitle + SEPARATOR);

            // jobid
            writer.write("N/A" + SEPARATOR);

            // pmkey
            writer.write("N/A" + SEPARATOR);

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
