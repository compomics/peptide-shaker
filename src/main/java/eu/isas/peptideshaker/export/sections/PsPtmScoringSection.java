package eu.isas.peptideshaker.export.sections;

import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import eu.isas.peptideshaker.export.exportfeatures.PsPtmScoringFeature;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the project related export features.
 *
 * @author Marc Vaudel
 */
public class PsPtmScoringSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsPtmScoringFeature> ptmScoringFeatures;
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
    public PsPtmScoringSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        ptmScoringFeatures = new ArrayList<>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPtmScoringFeature) {
                ptmScoringFeatures.add((PsPtmScoringFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as PTM scoring feature.");
            }
        }
        Collections.sort(ptmScoringFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param ptmScoringPreferences the PTM scoring preferences of this project
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(ModificationLocalizationParameters ptmScoringPreferences, WaitingHandler waitingHandler) throws IOException {

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

        for (PsPtmScoringFeature ptmScoringFeature : ptmScoringFeatures) {
            if (indexes) {
                writer.write(Integer.toString(line));
                writer.addSeparator();
            }
            writer.write(ptmScoringFeature.getTitle() + "");
            writer.addSeparator();
            switch (ptmScoringFeature) {
                case probabilitstic_score:
                    writer.write(ptmScoringPreferences.getSelectedProbabilisticScore().getName());
                    break;
                case threshold:
                    writer.write(Double.toString(ptmScoringPreferences.getProbabilisticScoreThreshold()));
                    break;
                case neutral_losses:
                    if (ptmScoringPreferences.isProbabilisticScoreNeutralLosses()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                default:
                    writer.write("Not implemented");
            }
            writer.newLine();
            line++;
        }
    }
}
