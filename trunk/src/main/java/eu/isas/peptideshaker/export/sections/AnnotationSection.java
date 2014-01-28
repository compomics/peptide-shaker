package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.AnnotationFeatures;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class outputs the annotation related export features.
 *
 * @author Marc Vaudel
 */
public class AnnotationSection {

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
    public AnnotationSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param annotationPreferences the annotation preferences of the project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(AnnotationPreferences annotationPreferences, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            if (indexes) {
                writer.write(separator);
            }
            writer.write("Parameter" + separator + "Value");
            writer.newLine();
        }
        int line = 1;

        for (ExportFeature exportFeature : exportFeatures) {

            if (indexes) {
                writer.write(line + separator);
            }

            writer.write(exportFeature.getTitle() + separator);
            AnnotationFeatures annotationFeature = (AnnotationFeatures) exportFeature;

            switch (annotationFeature) {
                case automatic_annotation:
                    if (annotationPreferences.useAutomaticAnnotation()) {
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
                    for (NeutralLoss neutralLoss : annotationPreferences.getNeutralLosses().getAccountedNeutralLosses()) {
                        if (!neutralLosses.equals("")) {
                            neutralLosses += ", ";
                        }
                        neutralLosses += neutralLoss.name;
                    }
                    writer.write(neutralLosses);
                    break;
                case neutral_losses_sequence_dependence:
                    if (annotationPreferences.areNeutralLossesSequenceDependant()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                case selected_charges:
                    String charges = "";
                    for (int charge : annotationPreferences.getValidatedCharges()) {
                        if (!charges.equals("")) {
                            charges += ", ";
                        }
                        charges += charge;
                    }
                    writer.write(charges);
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
