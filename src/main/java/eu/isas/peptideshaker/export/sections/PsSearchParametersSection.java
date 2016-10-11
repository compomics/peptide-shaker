package eu.isas.peptideshaker.export.sections;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.preferences.DigestionPreferences;
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
                case cleavage:
                    DigestionPreferences digestionPreferences = searchParameters.getDigestionPreferences();
                    writer.write(digestionPreferences.getCleavagePreference().toString());
                    break;
                case enzyme:
                    digestionPreferences = searchParameters.getDigestionPreferences();
                    StringBuilder content = new StringBuilder();
                    if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                        for (Enzyme enzyme : digestionPreferences.getEnzymes()) {
                            if (content.length() > 0) {
                                content.append(", ");
                            }
                            content.append(enzyme.getName());
                        }
                    }
                    writer.write(content.toString());
                    break;
                case mc:
                    digestionPreferences = searchParameters.getDigestionPreferences();
                    content = new StringBuilder();
                    if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                        for (Enzyme enzyme : digestionPreferences.getEnzymes()) {
                            String enzymeName = enzyme.getName();
                            if (content.length() > 0) {
                                content.append(", ");
                            }
                            Integer mc = digestionPreferences.getnMissedCleavages(enzymeName);
                            content.append(mc);
                        }
                    }
                    writer.write(content.toString());
                    break;
                case specificity:
                    digestionPreferences = searchParameters.getDigestionPreferences();
                    content = new StringBuilder();
                    if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                        for (Enzyme enzyme : digestionPreferences.getEnzymes()) {
                            String enzymeName = enzyme.getName();
                            if (content.length() > 0) {
                                content.append(", ");
                            }
                            DigestionPreferences.Specificity specificity = digestionPreferences.getSpecificity(enzymeName);
                            content.append(specificity);
                        }
                    }
                    writer.write(content.toString());
                    break;
                case fixed_modifications:
                    content = new StringBuilder();
                    for (String modification : searchParameters.getPtmSettings().getFixedModifications()) {
                        if (content.length() > 0) {
                            content.append(", ");
                        }
                        content.append(modification);
                    }
                    writer.write(content.toString());
                    break;
                case variable_modifications:
                    content = new StringBuilder();
                    for (String modification : searchParameters.getPtmSettings().getVariableModifications()) {
                        if (content.length() > 0) {
                            content.append(", ");
                        }
                        content.append(modification);
                    }
                    writer.write(content.toString());
                    break;
                case refinement_variable_modifications:
                    content = new StringBuilder();
                    for (String modification : searchParameters.getPtmSettings().getRefinementVariableModifications()) {
                        if (content.length() > 0) {
                            content.append(", ");
                        }
                        content.append(modification);
                    }
                    writer.write(content.toString());
                    break;
                case refinement_fixed_modifications:
                    content = new StringBuilder();
                    for (String modification : searchParameters.getPtmSettings().getRefinementFixedModifications()) {
                        if (content.length() > 0) {
                            content.append(", ");
                        }
                        content.append(modification);
                    }
                    writer.write(content.toString());
                    break;
                case forward_ion:
                    content = new StringBuilder();
                    for (Integer ion : searchParameters.getForwardIons()) {
                        if (content.length() > 0) {
                            content.append(", ");
                        }
                        content.append(PeptideFragmentIon.getSubTypeAsString(ion));
                    }
                    writer.write(content.toString());
                    break;
                case rewind_ion:
                    content = new StringBuilder();
                    for (Integer ion : searchParameters.getRewindIons()) {
                        if (content.length() > 0) {
                            content.append(", ");
                        }
                        content.append(PeptideFragmentIon.getSubTypeAsString(ion));
                    }
                    writer.write(content.toString());
                    break;
                case fragment_tolerance:
                    writer.write(searchParameters.getFragmentIonAccuracy() + "");
                    break;
                case precursor_tolerance_unit:
                    writer.write(searchParameters.getPrecursorAccuracyType().toString());
                    break;
                case fragment_tolerance_unit:
                    writer.write(searchParameters.getFragmentAccuracyType().toString());
                    break;
                case precursor_tolerance:
                    writer.write(searchParameters.getPrecursorAccuracy() + "");
                    break;
                default:
                    writer.write("Not implemented");
            }
            writer.newLine();
            line++;
        }
    }
}
