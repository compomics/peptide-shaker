package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import static eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature.fragment_number;
import static eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature.fragment_type;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
     * The separator used to separate columns.
     */
    private String separator;
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
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param separator the separator
     * @param indexes show indexes
     * @param header show header
     * @param writer the writer
     */
    public PsFragmentSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {

        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;

        fragmentFeatures = new ArrayList<PsFragmentFeature>(exportFeatures.size());
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
     * @param spectrumMatch the spectrum match of interest
     * @param searchParameters the search parameters
     * @param annotationPreferences the annotation preferences
     * @param linePrefix the line prefix
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(SpectrumMatch spectrumMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences,
            String linePrefix, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey());

        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();

        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                annotationPreferences.getNeutralLosses(),
                annotationPreferences.getValidatedCharges(),
                spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
                spectrum,
                spectrumMatch.getBestPeptideAssumption().getPeptide(),
                spectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                annotationPreferences.getFragmentIonAccuracy(), false, annotationPreferences.isHighResolutionAnnotation());

        HashMap<Double, ArrayList<IonMatch>> sortedAnnotation = new HashMap<Double, ArrayList<IonMatch>>();
        for (IonMatch ionMatch : annotations) {
            double mz = ionMatch.peak.mz;
            if (!sortedAnnotation.containsKey(mz)) {
                sortedAnnotation.put(mz, new ArrayList<IonMatch>());
            }
            sortedAnnotation.get(mz).add(ionMatch);
        }
        ArrayList<Double> mzs = new ArrayList<Double>(sortedAnnotation.keySet());
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
                    writer.write(line + separator);
                }
                for (PsFragmentFeature fragmentFeature : fragmentFeatures) {

                    switch (fragmentFeature) {
                        case annotation:
                            writer.write(ionMatch.getPeakAnnotation() + separator);
                            break;
                        case fragment_type:
                            writer.write(Ion.getTypeAsString(ionMatch.ion.getType()) + separator);
                            break;
                        case fragment_subType:
                            writer.write(ionMatch.ion.getSubTypeAsString() + separator);
                            break;
                        case fragment_number:
                            Ion ion = ionMatch.ion;
                            if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                                writer.write(((PeptideFragmentIon) ion).getNumber() + "");
                            }
                            writer.write(separator);
                            break;
                        case fragment_losses:
                            writer.write(ionMatch.ion.getNeutralLossesAsString() + separator);
                            break;
                        case fragment_name:
                            ion = ionMatch.ion;
                            String name;
                            if (ion.getType() == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                                name = ((PeptideFragmentIon) ion).getNameWithNumber();
                            } else {
                                name = ion.getName();
                            }
                            writer.write(name + separator);
                            break;
                        case fragment_charge:
                            writer.write(ionMatch.charge.value + separator);
                            break;
                        case theoretic_mz:
                            writer.write(ionMatch.ion.getTheoreticMz(ionMatch.charge.value) + separator);
                            break;
                        case mz:
                            writer.write(ionMatch.peak.mz + separator);
                            break;
                        case intensity:
                            writer.write(ionMatch.peak.intensity + separator);
                            break;
                        case error_Da:
                            writer.write(ionMatch.getAbsoluteError() + separator);
                            break;
                        case error_ppm:
                            writer.write(ionMatch.getRelativeError() + separator);
                            break;
                        default:
                            writer.write("Not implemented" + separator);
                    }
                }
                writer.newLine();
                line++;
            }
        }
    }

    /**
     * Writes the header of this section.
     *
     * @throws IOException
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.write(separator);
        }
        boolean firstColumn = true;
        for (PsFragmentFeature fragmentFeature : fragmentFeatures) {
            for (String title : fragmentFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title + separator);
            }
        }
        writer.newLine();
    }
}
