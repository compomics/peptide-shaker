package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class can be used to export spectra.
 *
 * @author Marc Vaudel
 */
public class SpectrumExporter {

    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

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
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public void exportSpectra(File destinationFolder, WaitingHandler waitingHandler, ExportType exportType, SequenceMatchingPreferences sequenceMatchingPreferences)
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        PSParameter psParameter = new PSParameter();

        if (exportType == ExportType.non_validated_peptides
                || exportType == ExportType.validated_psms_peptides
                || exportType == ExportType.validated_psms_peptides_proteins) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Exporting Spectra - Loading Peptides. Please Wait...");
            }
            identification.loadPeptideMatchParameters(psParameter, waitingHandler, true);
        }
        if (exportType == ExportType.non_validated_proteins
                || exportType == ExportType.validated_psms_peptides_proteins) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Exporting Spectra - Loading Proteins. Please Wait...");
            }
            identification.loadProteinMatchParameters(psParameter, waitingHandler, true);
        }

        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        for (int i = 0; i < spectrumFactory.getMgfFileNames().size(); i++) {

            String mgfFile = spectrumFactory.getMgfFileNames().get(i);

            FileWriter f = new FileWriter(new File(destinationFolder, getFileName(mgfFile, exportType)));

            try {
                BufferedWriter b = new BufferedWriter(f);
                try {
                    if (waitingHandler != null) {
                        waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait... (" + (i + 1) + "/" + spectrumFactory.getMgfFileNames().size() + ")");
                        // reset the progress bar
                        waitingHandler.resetSecondaryProgressCounter();
                        if (exportType == ExportType.non_validated_psms
                                || exportType == ExportType.non_validated_peptides
                                || exportType == ExportType.non_validated_proteins) {
                            waitingHandler.setMaxSecondaryProgressCounter(spectrumFactory.getSpectrumTitles(mgfFile).size());
                        } else {
                            waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentification(mgfFile).size());
                        }
                    }

                    // Export the identified spectra
                    PsmIterator psmIterator = identification.getPsmIterator(mgfFile, parameters, false, waitingHandler);
                    SpectrumMatch spectrumMatch;

                    while ((spectrumMatch = psmIterator.next()) != null) {

                        String spectrumKey = spectrumMatch.getKey();

                        if (shallExport(spectrumMatch, exportType, sequenceMatchingPreferences)) {
                            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                            b.write(spectrum.asMgf());
                        }
                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                            waitingHandler.increaseSecondaryProgressCounter();
                        }
                    }

                    if (exportType == ExportType.non_validated_psms
                            || exportType == ExportType.non_validated_peptides
                            || exportType == ExportType.non_validated_proteins) {
                        HashSet<String> identifiedSpectra = identification.getSpectrumIdentification(mgfFile);
                        for (String spectrumTitle : spectrumFactory.getSpectrumTitles(mgfFile)) {
                            String spectrumKey = Spectrum.getSpectrumKey(mgfFile, spectrumTitle);
                            if (!identifiedSpectra.contains(spectrumKey)) {
                                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(mgfFile, spectrumTitle);
                                b.write(spectrum.asMgf());
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
                    b.close();
                }
            } finally {
                f.close();
            }
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
     * Returns the name of the file to write.
     *
     * @param fileName the original file name
     * @param exportType the type of PSM to be exported
     * @return the name of the file
     */
    public static String getFileName(String fileName, ExportType exportType) {
        String tempName = fileName.substring(0, fileName.lastIndexOf("."));
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return tempName + getSuffix(exportType) + extension;
    }

    /**
     * Indicates whether a spectrum shall be exported according to the export
     * type number. Note: proteins must be set for every exported peptide.
     *
     * @param spectrumKey the key of the spectrum
     * @param exportType the export type number
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @return whether a spectrum shall be exported
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     */
    private boolean shallExport(SpectrumMatch spectrumMatch, ExportType exportType, SequenceMatchingPreferences sequenceMatchingPreferences)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        PSParameter psParameter = new PSParameter();
        String spectrumKey = spectrumMatch.getKey();

        switch (exportType) {
            case non_validated_psms:
            case validated_psms:
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        boolean decoy = false;
                        for (String accession : spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(sequenceMatchingPreferences)) {
                            if (sequenceFactory.isDecoyAccession(accession)) {
                                decoy = true;
                                break;
                            }
                        }
                        return !decoy && exportType == ExportType.validated_psms;
                    } else {
                        return false;
                    }
                }
                return exportType == ExportType.non_validated_psms;
            case non_validated_peptides:
            case validated_psms_peptides:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                    boolean decoy = false;
                    for (String accession : peptide.getParentProteins(sequenceMatchingPreferences)) {
                        if (sequenceFactory.isDecoyAccession(accession)) {
                            decoy = true;
                            break;
                        }
                    }
                    if (!decoy) {
                        String peptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                        if (exportType == ExportType.non_validated_peptides || ((PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter)).getMatchValidationLevel().isValidated()) {
                            if (psParameter.getMatchValidationLevel().isValidated()) {
                                return exportType == ExportType.validated_psms_peptides;
                            }
                        }
                    }
                }
                return exportType == ExportType.non_validated_peptides;
            case non_validated_proteins:
            case validated_psms_peptides_proteins:
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                    boolean decoy = false;
                    for (String accession : peptide.getParentProteins(sequenceMatchingPreferences)) {
                        if (sequenceFactory.isDecoyAccession(accession)) {
                            decoy = true;
                            break;
                        }
                    }
                    if (!decoy) {
                        String peptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                        if (exportType == ExportType.non_validated_proteins
                                || ((PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter)).getMatchValidationLevel().isValidated()
                                && ((PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter)).getMatchValidationLevel().isValidated()) {
                            ArrayList<String> proteins = peptide.getParentProteins(sequenceMatchingPreferences);
                            for (String accession : proteins) {
                                HashSet<String> proteinKeys = identification.getProteinMap().get(accession);
                                if (proteinKeys != null) {
                                    identification.loadProteinMatchParameters(new ArrayList<String>(proteinKeys), psParameter, null, true);
                                    for (String proteinKey : proteinKeys) {
                                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                                        if (psParameter.getMatchValidationLevel().isValidated()) {
                                            return exportType == ExportType.validated_psms_peptides_proteins;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return exportType == ExportType.non_validated_proteins;
                }
                return true;
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
