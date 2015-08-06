package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import eu.isas.peptideshaker.export.exportfeatures.PsSearchFeature;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the search parameters related export features.
 *
 * @author Marc Vaudel
 */
public class PsSearchParametersSection {

    /**
     * The features to export.
     */
    private ArrayList<PsSearchFeature> searchFeatures;
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
    public PsSearchParametersSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        searchFeatures = new ArrayList<PsSearchFeature>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsSearchFeature) {
                searchFeatures.add((PsSearchFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as search parameter feature.");
            }
        }
        Collections.sort(searchFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param searchParameters the search parameters of this project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(SearchParameters searchParameters, WaitingHandler waitingHandler) throws IOException {

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

        for (PsSearchFeature exportFeature : searchFeatures) {
            if (indexes) {
                writer.write(line + "");
                writer.addSeparator();
            }
            writer.write(exportFeature.getTitle());
            writer.addSeparator();
            switch (exportFeature) {
                case database:
                    String fastaFileName = Util.getFileName(searchParameters.getFastaFile());
                    writer.write(fastaFileName);
                    break;
                case enzyme:
                    writer.write(searchParameters.getEnzyme().getName());
                    break;
                case fixed_modifications:
                    String modifications = "";
                    for (String modification : searchParameters.getPtmSettings().getFixedModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case variable_modifications:
                    modifications = "";
                    for (String modification : searchParameters.getPtmSettings().getVariableModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case refinement_variable_modifications:
                    modifications = "";
                    for (String modification : searchParameters.getPtmSettings().getRefinementVariableModifications()) {
                        if (!modifications.equals("")) {
                            modifications += ", ";
                        }
                        modifications += modification;
                    }
                    writer.write(modifications);
                    break;
                case refinement_fixed_modifications:
                    modifications = "";
                    for (String modification : searchParameters.getPtmSettings().getRefinementFixedModifications()) {
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
