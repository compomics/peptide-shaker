package eu.isas.peptideshaker.followup;

import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.ElementaryIon;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class exports a text file to create a swath library.
 *
 * @author Marc Vaudel
 */
public class SwathExport {

    /**
     * The separator (tab by default).
     */
    public static final String SEPARATOR = "\t";

    /**
     * Writes a text export containing the information for a swath library.
     * Note: ions with neutral losses are skipped.
     *
     * @param destinationFile the destination file where to write the
     * information
     * @param identification the identification containing the identification
     * results
     * @param exportType the type of export
     * @param waitingHandler a waiting handler to display progress and cancel
     * the process
     * @param targetedPTMs the targeted PTMs in case of a PTM export
     * @param annotationPreferences the spectrum annotation preferences
     * @param sequenceMatchingPreferences the sequence matching preferences for
     * peptide to protein mapping
     * @param ptmSequenceMatchingPreferences the sequence matching preferences
     * for PTM to peptide mapping
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown if a math exception occurred when estimating the noise level in spectra
     */
    public static void writeSwathExport(File destinationFile, Identification identification, ExportType exportType, WaitingHandler waitingHandler,
            ArrayList<String> targetedPTMs, AnnotationSettings annotationPreferences, SequenceMatchingPreferences sequenceMatchingPreferences, SequenceMatchingPreferences ptmSequenceMatchingPreferences)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (exportType == ExportType.confident_ptms) {
            if (targetedPTMs == null || targetedPTMs.isEmpty()) {
                throw new IllegalArgumentException("No modification provided for the Progenesis PTM export.");
            }
        }

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();

        if (exportType == ExportType.validated_psms_peptides || exportType == ExportType.validated_psms_peptides_proteins || exportType == ExportType.confident_ptms) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Progenesis Export - Loading Peptides. Please Wait...");
            }
            identification.loadPeptideMatchParameters(psParameter, waitingHandler, true);
        }
        if (exportType == ExportType.validated_psms_peptides_proteins || exportType == ExportType.confident_ptms) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Progenesis Export - Loading Proteins. Please Wait...");
            }
            identification.loadProteinMatchParameters(psParameter, waitingHandler, true);
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            return;
        }

        FileWriter f = new FileWriter(destinationFile);
        try {
            BufferedWriter writer = new BufferedWriter(f);

            try {
                writer.write("Q1" + SEPARATOR);
                writer.write("Q3" + SEPARATOR);
                writer.write("RT_detected" + SEPARATOR);
                writer.write("isotype" + SEPARATOR);
                writer.write("uniprot_id" + SEPARATOR);
                writer.write("relative_intensity" + SEPARATOR);
                writer.write("stripped_sequence" + SEPARATOR);
                writer.write("modification_sequence" + SEPARATOR);
                writer.write("prec_z" + SEPARATOR);
                writer.write("protein_name" + SEPARATOR);
                writer.write("frg_type" + SEPARATOR);
                writer.write("frg_z" + SEPARATOR);
                writer.write("frg_nr" + SEPARATOR);
                writer.newLine();

                for (int i = 0; i < spectrumFactory.getMgfFileNames().size(); i++) {

                    String mgfFile = spectrumFactory.getMgfFileNames().get(i);

                    if (waitingHandler != null) {
                        waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                        // reset the progress bar
                        waitingHandler.resetSecondaryProgressCounter();
                        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                    }

                    PsmIterator psmIterator = identification.getPsmIterator(mgfFile, parameters, false, waitingHandler);

                    while (psmIterator.hasNext()) {

                        SpectrumMatch spectrumMatch = psmIterator.next();
                        String spectrumKey = spectrumMatch.getKey();

                        if (identification.matchExists(spectrumKey)) {
                            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

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
                                                writePsm(writer, spectrumKey, identification, sequenceMatchingPreferences, ptmSequenceMatchingPreferences, annotationPreferences, spectrumAnnotator);
                                            } else {
                                                String peptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                                                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                                                if (psParameter.getMatchValidationLevel().isValidated()) {
                                                    if (exportType == ExportType.validated_psms_peptides) {
                                                        writePsm(writer, spectrumKey, identification, sequenceMatchingPreferences, ptmSequenceMatchingPreferences, annotationPreferences, spectrumAnnotator);
                                                    } else {
                                                        ArrayList<String> accessions = new ArrayList<String>();
                                                        for (String accession : peptide.getParentProteins(sequenceMatchingPreferences)) {
                                                            HashSet<String> groups = identification.getProteinMap().get(accession);
                                                            if (groups != null) {
                                                                for (String group : groups) {
                                                                    psParameter = (PSParameter) identification.getProteinMatchParameter(group, psParameter);
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
                                                            writePsm(writer, spectrumKey, accessions, identification, sequenceMatchingPreferences, ptmSequenceMatchingPreferences, annotationPreferences, spectrumAnnotator);
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
                if (targetedPTMs.contains(modificationMatch.getTheoreticPtm())) {
                    found = true;
                    if (!modificationMatch.isConfident()) {
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
     * @param sequenceMatchingPreferences the sequence matching preferences for
     * peptide to protein mapping
     * @param ptmSequenceMatchingPreferences the sequence matching preferences
     * for PTM to peptide mapping
     * @param annotationPreferences the annotation preferences to use for
     * spectrum annotation
     * @param spectrumAnnotator the spectrum annotator to use
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown if a math exception occurred when estimating the noise level in spectra
     */
    private static void writePsm(BufferedWriter writer, String spectrumKey, Identification identification, SequenceMatchingPreferences sequenceMatchingPreferences,
            SequenceMatchingPreferences ptmSequenceMatchingPreferences, AnnotationSettings annotationPreferences, PeptideSpectrumAnnotator spectrumAnnotator)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {
        writePsm(writer, spectrumKey, null, identification, sequenceMatchingPreferences, ptmSequenceMatchingPreferences, annotationPreferences, spectrumAnnotator);
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
     * @param sequenceMatchingPreferences the sequence matching preferences for
     * peptide to protein mapping
     * @param ptmSequenceMatchingPreferences the sequence matching preferences
     * for PTM to peptide mapping
     * @param annotationPreferences the annotation preferences to use for
     * spectrum annotation
     * @param spectrumAnnotator the spectrum annotator to use
     *
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown if a math exception occurred when estimating the noise level in spectra
     */
    private static void writePsm(BufferedWriter writer, String spectrumKey, ArrayList<String> accessions, Identification identification,
            SequenceMatchingPreferences sequenceMatchingPreferences, SequenceMatchingPreferences ptmSequenceMatchingPreferences,
            AnnotationSettings annotationPreferences, PeptideSpectrumAnnotator spectrumAnnotator)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();
        Peptide peptide = bestAssumption.getPeptide();
        MSnSpectrum spectrum = (MSnSpectrum) SpectrumFactory.getInstance().getSpectrum(spectrumKey);

        if (accessions == null) {
            accessions = peptide.getParentProteins(sequenceMatchingPreferences);
        }
        PTMFactory ptmFactory = PTMFactory.getInstance();
        PtmToPrideMap ptmToPrideMap = null;
        // get the psi-mod mappings
        try {
            PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance();
            ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String accession : accessions) {

            SpecificAnnotationSettings specificAnnotationPreferences
                    = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), bestAssumption, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);
            ArrayList<IonMatch> matches = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences,
                    (MSnSpectrum) spectrum, bestAssumption.getPeptide(), false);

            for (IonMatch ionMatch : matches) {

                if (ionMatch.ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {

                    PeptideFragmentIon peptideFragmentIon = (PeptideFragmentIon) ionMatch.ion;

                    if (!peptideFragmentIon.hasNeutralLosses()) {

                        //Q1
                        Charge charge = bestAssumption.getIdentificationCharge();
                        double theoreticPrecMz = (peptide.getMass() + charge.value * ElementaryIon.proton.getTheoreticMass()) / charge.value;
                        writer.write(theoreticPrecMz + SEPARATOR);

                        //Q3
                        double theoreticFragMz = ionMatch.ion.getTheoreticMz(charge.value);
                        writer.write(theoreticFragMz + SEPARATOR);

                        // RT_detected
                        double rt = spectrum.getPrecursor().getRt();
                        writer.write(rt + SEPARATOR);

                        // isotope
                        writer.write("Light" + SEPARATOR);

                        // uniprot_id
                        writer.write(accession + SEPARATOR);

                        // relative intensity
                        double intensity = ionMatch.peak.intensity; //@TODO: normalize in some way?
                        writer.write(intensity + SEPARATOR);

                        // sequence
                        String sequence = peptide.getSequence();
                        writer.write(sequence + SEPARATOR);

                        // modified sequence
                        String modifiedSequence = "";
                        for (int aa = 0; aa < sequence.length(); aa++) {
                            modifiedSequence += sequence.charAt(aa);
                            if (peptide.isModified()) {
                                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                    if (modificationMatch.getModificationSite() == aa + 1) {
                                        String ptmName = modificationMatch.getTheoreticPtm();
                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                        CvTerm cvTerm = ptm.getCvTerm();
                                        if (cvTerm != null) {
                                            modifiedSequence += "[" + cvTerm.getName() + "]";
                                        } else {
                                            modifiedSequence += "[" + ptm.getShortName() + "]";
                                        }
                                    }
                                }
                            }
                        }
                        writer.write(modifiedSequence + SEPARATOR);

                        // prec_z
                        writer.write(charge.value + SEPARATOR);

                        // protein name
                        writer.write(SequenceFactory.getInstance().getHeader(accession).getDescriptionProteinName() + SEPARATOR);

                        // fragment type
                        writer.write(peptideFragmentIon.getSubTypeAsString() + SEPARATOR);

                        // fragment z
                        writer.write(ionMatch.charge + SEPARATOR);

                        // fragment number
                        writer.write(peptideFragmentIon.getNumber() + SEPARATOR);

                        writer.newLine();
                    }
                }
            }
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
