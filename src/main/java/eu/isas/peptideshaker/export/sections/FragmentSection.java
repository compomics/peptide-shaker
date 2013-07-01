/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Ion.IonType;
import static com.compomics.util.experiment.biology.Ion.IonType.ELEMENTARY_ION;
import static com.compomics.util.experiment.biology.Ion.IonType.GLYCON;
import static com.compomics.util.experiment.biology.Ion.IonType.IMMONIUM_ION;
import static com.compomics.util.experiment.biology.Ion.IonType.PEPTIDE_FRAGMENT_ION;
import static com.compomics.util.experiment.biology.Ion.IonType.PRECURSOR_ION;
import static com.compomics.util.experiment.biology.Ion.IonType.REPORTER_ION;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.ElementaryIon;
import com.compomics.util.experiment.biology.ions.Glycon;
import com.compomics.util.experiment.biology.ions.ImmoniumIon;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.PrecursorIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures;
import static eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures.fragment_type;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeatures;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.a_score;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.accessions;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.confidence;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.d_score;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.decoy;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.fixed_ptms;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.hidden;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.identification_charge;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.isotope;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.localization_confidence;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.max_intensity;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.modified_sequence;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.mz;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.mz_error;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.rt;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.score;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.sequence;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.spectrum_charge;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.spectrum_file;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.spectrum_scan_number;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.spectrum_title;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.starred;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.theoretical_mass;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.total_spectrum_intensity;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.validated;
import static eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.variable_ptms;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
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
public class FragmentSection {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The features to export.
     */
    private ArrayList<ExportFeature> exportFeatures;
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
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public FragmentSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
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
    public void writeSection(SpectrumMatch spectrumMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, String linePrefix, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressDialogIndeterminate(true);
        }

        if (header) {
            writeHeader();
        }

        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey());

        SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();

        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                annotationPreferences.getNeutralLosses(),
                annotationPreferences.getValidatedCharges(),
                spectrumMatch.getBestAssumption().getIdentificationCharge().value,
                spectrum,
                spectrumMatch.getBestAssumption().getPeptide(),
                spectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                annotationPreferences.getFragmentIonAccuracy(), false);

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
            waitingHandler.resetSecondaryProgressBar();
            waitingHandler.setMaxSecondaryProgressValue(annotations.size());
        }

        for (double mz : mzs) {
            for (IonMatch ionMatch : sortedAnnotation.get(mz)) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressValue();
                }
                if (indexes) {
                    if (linePrefix != null) {
                        writer.write(linePrefix);
                    }
                    writer.write(line + separator);
                }
                for (ExportFeature exportFeature : exportFeatures) {
                    FragmentFeatures fragmentFeatures = (FragmentFeatures) exportFeature;

                    switch (fragmentFeatures) {
                        case annotation:
                            writer.write(ionMatch.getPeakAnnotation() + separator);
                            break;
                        case fragment_type:
                            writer.write(Ion.getTypeAsString(ionMatch.ion.getType()) + separator);
                            break;
                        case fragment_subType:
                            writer.write(ionMatch.ion.getSubTypeAsString() + separator);
                            break;
                        case fragment_name:
                            writer.write(ionMatch.ion.getName() + separator);
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
        for (ExportFeature exportFeature : exportFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.write(separator);
            }
            writer.write(exportFeature.getTitle());
        }
        writer.newLine();
    }
}
