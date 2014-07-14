package eu.isas.peptideshaker.export.sections;

import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.PtmScoringFeature;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the project related export features.
 *
 * @author Marc Vaudel
 */
public class PtmScoringSection {

    /**
     * The features to export.
     */
    private ArrayList<PtmScoringFeature> ptmScoringFeatures;
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
    public PtmScoringSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        ptmScoringFeatures = new ArrayList<PtmScoringFeature>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PtmScoringFeature) {
                ptmScoringFeatures.add((PtmScoringFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as PTM scoring feature.");
            }
        }
        Collections.sort(ptmScoringFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param ptmcoringPreferences the PTM scoring preferences of this project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(PTMScoringPreferences ptmcoringPreferences, WaitingHandler waitingHandler) throws IOException {

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

        for (PtmScoringFeature ptmScoringFeature : ptmScoringFeatures) {
            if (indexes) {
                writer.write(line + separator);
            }
            for (String title : ptmScoringFeature.getTitles()) {
                writer.write(title + separator);
            }
            switch (ptmScoringFeature) {
                case aScore:
                    if (ptmcoringPreferences.isProbabilitsticScoreCalculation()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                case flr:
                    writer.write(ptmcoringPreferences.getFlrThreshold() + "");
                    break;
                case neutral_losses:
                    if (ptmcoringPreferences.isProbabilisticScoreNeutralLosses()) {
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
