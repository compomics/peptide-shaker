package eu.isas.peptideshaker.export.sections;

import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import eu.isas.peptideshaker.export.exportfeatures.PsInputFilterFeature;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the annotation related export features.
 *
 * @author Marc Vaudel
 */
public class PsInputFilterSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsInputFilterFeature> exportFeatures;
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
    public PsInputFilterSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        this.exportFeatures = new ArrayList<>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsInputFilterFeature) {
                this.exportFeatures.add((PsInputFilterFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as input filter feature.");
            }
        }
        Collections.sort(this.exportFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param idFilter the identification used for this project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(PeptideAssumptionFilter idFilter, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            if (indexes) {
                writer.addSeparator();
            }
            writer.writeHeaderText("Parameter");
            writer.addSeparator();
            writer.writeHeaderText("Value");
            writer.newLine();
        }

        int line = 1;

        for (ExportFeature exportFeature : exportFeatures) {

            if (indexes) {
                writer.write(line + "");
                writer.addSeparator();
            }

            writer.write(exportFeature.getTitle());
            writer.addSeparator();
            PsInputFilterFeature inputFilterFeatures = (PsInputFilterFeature) exportFeature;

            switch (inputFilterFeatures) {
                case max_mz_deviation:
                    double value = idFilter.getMaxMzDeviation();
                    if (value == -1) {
                        writer.write("none");
                    } else {
                        writer.write(value + "");
                    }
                    break;
                case max_mz_deviation_unit:
                    if (idFilter.isIsPpm()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                case max_peptide_length:
                    writer.write(idFilter.getMaxPepLength() + "");
                    break;
                case min_peptide_length:
                    writer.write(idFilter.getMinPepLength() + "");
                    break;
                case unknown_PTM:
                    if (idFilter.removeUnknownPTMs()) {
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
