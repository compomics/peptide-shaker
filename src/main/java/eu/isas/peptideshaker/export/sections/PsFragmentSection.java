package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.ions.Ion;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.spectra.MSnSpectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import static eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature.fragment_number;
import static eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature.fragment_type;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM related export features.
 *
 * @author Marc Vaudel
 */
public class PsFragmentSection {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The features to export.
     */
    private ArrayList<PsFragmentFeature> fragmentFeatures;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsFragmentSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {

        this.indexes = indexes;
        this.header = header;
        this.writer = writer;

        fragmentFeatures = new ArrayList<>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsFragmentFeature) {
                PsFragmentFeature fragmentFeature = (PsFragmentFeature) exportFeature;
                fragmentFeatures.add(fragmentFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as fragment feature.");
            }
        }
        Collections.sort(fragmentFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param spectrumKey the key of the spectrum
     * @param spectrumIdentificationAssumption the spectrum identification of
     * interest
     * @param identificationParameters the identification parameters
     * @param linePrefix the line prefix
     * @param waitingHandler the waiting handler
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
     * @throws org.apache.commons.math.MathException exception thrown if a math
     * exception occurred when estimating the noise level
     */
    public void writeSection(String spectrumKey, SpectrumIdentificationAssumption spectrumIdentificationAssumption, IdentificationParameters identificationParameters,
            String linePrefix, WaitingHandler waitingHandler) throws IOException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        ArrayList<IonMatch> annotations;
        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
        AnnotationParameters annotationPreferences = identificationParameters.getAnnotationPreferences();
        SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, spectrumIdentificationAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
        if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
            PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
            annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences,
                    spectrum, peptideAssumption.getPeptide());
        } else if (spectrumIdentificationAssumption instanceof TagAssumption) {
            TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
            TagSpectrumAnnotator spectrumAnnotator = new TagSpectrumAnnotator();
            annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences,
                    spectrum, tagAssumption.getTag());
        } else {
            throw new UnsupportedOperationException("Export not implemented for spectrum identification of type " + spectrumIdentificationAssumption.getClass() + ".");
        }

        HashMap<Double, ArrayList<IonMatch>> sortedAnnotation = new HashMap<>(annotations.size());
        for (IonMatch ionMatch : annotations) {
            double mz = ionMatch.peak.mz;
            if (!sortedAnnotation.containsKey(mz)) {
                sortedAnnotation.put(mz, new ArrayList<>());
            }
            sortedAnnotation.get(mz).add(ionMatch);
        }
        ArrayList<Double> mzs = new ArrayList<>(sortedAnnotation.keySet());
        Collections.sort(mzs);

        int line = 1;

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(annotations.size());
        }

        for (double mz : mzs) {
            for (IonMatch ionMatch : sortedAnnotation.get(mz)) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }
                if (indexes) {
                    if (linePrefix != null) {
                        writer.write(linePrefix);
                    }
                    writer.write(line + "");
                    writer.addSeparator();
                }
                for (PsFragmentFeature fragmentFeature : fragmentFeatures) {

                    switch (fragmentFeature) {
                        case annotation:
                            writer.write(ionMatch.getPeakAnnotation());
                            break;
                        case fragment_type:
                            writer.write(Ion.getTypeAsString(ionMatch.ion.getType()));
                            break;
                        case fragment_subType:
                            writer.write(ionMatch.ion.getSubTypeAsString());
                            break;
                        case fragment_number:
                            Ion ion = ionMatch.ion;
                            if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                                writer.write(((PeptideFragmentIon) ion).getNumber() + "");
                            }
                            break;
                        case fragment_losses:
                            writer.write(ionMatch.ion.getNeutralLossesAsString());
                            break;
                        case fragment_name:
                            ion = ionMatch.ion;
                            String name;
                            if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                                name = ((PeptideFragmentIon) ion).getNameWithNumber();
                            } else {
                                name = ion.getName();
                            }
                            writer.write(name);
                            break;
                        case fragment_charge:
                            writer.write(ionMatch.charge + "");
                            break;
                        case theoretic_mz:
                            writer.write(ionMatch.ion.getTheoreticMz(ionMatch.charge) + "");
                            break;
                        case mz:
                            writer.write(ionMatch.peak.mz + "");
                            break;
                        case intensity:
                            writer.write(ionMatch.peak.intensity + "");
                            break;
                        case error_Da:
                            writer.write(ionMatch.getAbsoluteError() + "");
                            break;
                        case error_ppm:
                            writer.write(ionMatch.getRelativeError() + "");
                            break;
                        default:
                            writer.write("Not implemented");
                    }
                    writer.addSeparator();
                }
                writer.newLine();
                line++;
            }
        }
    }

    /**
     * Writes the header of this section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        boolean firstColumn = true;
        for (PsFragmentFeature fragmentFeature : fragmentFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(fragmentFeature.getTitle());
        }
        writer.newLine();
    }
}
