package eu.isas.peptideshaker.export.section_generators;

import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.IdFilter;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.InputFilterFeatures;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class outputs the annotation related export features.
 *
 * @author Marc Vaudel
 */
public class InputFilterSection {

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
    public InputFilterSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param idFilter the identification used for this project
     * @param progressDialog the progress dialog
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(IdFilter idFilter, ProgressDialogX progressDialog) throws IOException {

        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Exporting Input Filters. Please Wait...");

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
            InputFilterFeatures inputFilterFeatures = (InputFilterFeatures) exportFeature;

            switch (inputFilterFeatures) {
                case mascot_max_evalue:
                    writer.write(idFilter.getMascotMaxEvalue() + "");
                    break;
                case max_mz_deviation:
                    writer.write(idFilter.getMaxMzDeviation() + "");
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
                case omssa_max_evalue:
                    writer.write(idFilter.getOmssaMaxEvalue() + "");
                    break;
                case unknown_PTM:
                    if (idFilter.removeUnknownPTMs()) {
                        writer.write("Yes");
                    } else {
                        writer.write("No");
                    }
                    break;
                case xtandem_max_evalue:
                    writer.write(idFilter.getXtandemMaxEvalue() + "");
                    break;
                default:
                    writer.write("Not implemented");
            }

            writer.newLine();
            line++;
        }
    }
}
