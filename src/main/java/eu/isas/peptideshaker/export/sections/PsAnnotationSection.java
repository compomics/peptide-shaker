package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.ions.NeutralLoss;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import eu.isas.peptideshaker.export.exportfeatures.PsAnnotationFeature;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the annotation related export features.
 *
 * @author Marc Vaudel
 */
public class PsAnnotationSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsAnnotationFeature> annotationFeatures;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private final boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private final boolean header;
    /**
     * The writer used to send the output to file.
     */
    private final ExportWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsAnnotationSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        this.annotationFeatures = new ArrayList<>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsAnnotationFeature) {
                annotationFeatures.add((PsAnnotationFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as annotation feature.");
            }
        }
        Collections.sort(this.annotationFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param annotationPreferences the annotation preferences of the project
     * @param waitingHandler the waiting handler
     * 
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(AnnotationParameters annotationPreferences, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            if (indexes) {
                writer.writeHeaderText("");
                writer.addSeparator();
            }
            writer.writeHeaderText("Parameter");
            writer.addSeparator();
            writer.writeHeaderText("Value");
            writer.newLine();
        }
        int line = 1;

        for (ExportFeature exportFeature : annotationFeatures) {

            if (indexes) {
                writer.write(Integer.toString(line));
                writer.addSeparator();
            }

            writer.write(exportFeature.getTitle());
            writer.addSeparator();

            PsAnnotationFeature annotationFeature = (PsAnnotationFeature) exportFeature;
            switch (annotationFeature) {
                case automatic_annotation:
                    if (annotationPreferences.isAutomaticAnnotation()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                case fragment_ion_accuracy:
                    writer.write(annotationPreferences.getFragmentIonAccuracy() + "");
                    break;
                case intensity_limit:
                    writer.write(annotationPreferences.getAnnotationIntensityLimit() + "");
                    break;
                case neutral_losses:
                    String neutralLosses = "";
                    for (NeutralLoss neutralLoss : annotationPreferences.getNeutralLosses()) {
                        if (!neutralLosses.equals("")) {
                            neutralLosses += ", ";
                        }
                        neutralLosses += neutralLoss.name;
                    }
                    writer.write(neutralLosses);
                    break;
                case neutral_losses_sequence_dependence:
                    if (annotationPreferences.areNeutralLossesSequenceAuto()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                case selected_ions:
                    String ions = "";
                    for (int fragmentType : annotationPreferences.getFragmentIonTypes()) {
                        if (!ions.equals("")) {
                            ions += ", ";
                        }
                        ions += PeptideFragmentIon.getSubTypeAsString(fragmentType);
                    }
                    writer.write(ions);
                    break;
                default:
                    writer.write("Not implemented");
            }
            writer.newLine();
            line++;
        }
    }
}
