package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.SearchFeatures;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class outputs the search parameters related export features.
 *
 * @author Marc Vaudel
 */
public class SearchParametersSection {

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
    public SearchParametersSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param searchParameters the search parameters of this project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(SearchParameters searchParameters, WaitingHandler waitingHandler) throws IOException {

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
            SearchFeatures searchFeatures = (SearchFeatures) exportFeature;
            switch (searchFeatures) {
                case database:
                    String fastaFileName = Util.getFileName(searchParameters.getFastaFile());
                    writer.write(fastaFileName);
                    break;
                case enzyme:
                    writer.write(searchParameters.getEnzyme().getName());
                    break;
                case fixed_modifications:
                    String modifications = "";
                    for (String modification : searchParameters.getModificationProfile().getFixedModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case variable_modifications:
                    modifications = "";
                    for (String modification : searchParameters.getModificationProfile().getVariableModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case refinement_variable_modifications:
                    modifications = "";
                    for (String modification : searchParameters.getModificationProfile().getRefinementVariableModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case refinement_fixed_modifications:
                    modifications = "";
                    for (String modification : searchParameters.getModificationProfile().getRefinementFixedModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case forward_ion:
                    String ionName = PeptideFragmentIon.getSubTypeAsString(searchParameters.getIonSearched1());
                    writer.write(ionName);
                    break;
                case fragment_tolerance:
                    writer.write(searchParameters.getFragmentIonAccuracy() + "");
                    break;
                case precursor_accuracy_unit:
                    if (searchParameters.isPrecursorAccuracyTypePpm()) {
                        writer.write("ppm");
                    } else {
                        writer.write("Da");
                    }
                    break;
                case precursor_tolerance:
                    writer.write(searchParameters.getPrecursorAccuracy() + "");
                    break;
                case rewind_ion:
                    ionName = PeptideFragmentIon.getSubTypeAsString(searchParameters.getIonSearched2());
                    writer.write(ionName);
                    break;
                default:
                    writer.write("Not implemented");
            }
            writer.newLine();
            line++;
        }
    }
}
