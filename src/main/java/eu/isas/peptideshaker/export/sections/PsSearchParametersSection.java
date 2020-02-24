package eu.isas.peptideshaker.export.sections;

import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsSearchFeature;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * This class outputs the search parameters related export features.
 *
 * @author Marc Vaudel
 */
public class PsSearchParametersSection {

    /**
     * The features to export.
     */
    private final EnumSet<PsSearchFeature> searchFeatures;
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
    public PsSearchParametersSection(
            ArrayList<ExportFeature> exportFeatures,
            boolean indexes,
            boolean header,
            ExportWriter writer
    ) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        searchFeatures = EnumSet.noneOf(PsSearchFeature.class);

        for (ExportFeature exportFeature : exportFeatures) {

            if (exportFeature instanceof PsSearchFeature) {

                searchFeatures.add((PsSearchFeature) exportFeature);

            } else {

                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as search parameter feature.");

            }
        }
    }

    /**
     * Writes the desired section.
     *
     * @param searchParameters the search parameters of this project
     * @param projectDetails the project details
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(
            SearchParameters searchParameters,
            ProjectDetails projectDetails,
            WaitingHandler waitingHandler
    ) throws IOException {

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

                writer.write(Integer.toString(line));
                writer.addSeparator();

            }

            writer.write(exportFeature.getTitle());
            writer.addSeparator();

            switch (exportFeature) {

                case database:

                    String fastaFileName = IoUtil.getFileName(projectDetails.getFastaFile());
                    writer.write(fastaFileName);
                    break;

                case cleavage:

                    DigestionParameters digestionPreferences = searchParameters.getDigestionParameters();
                    writer.write(digestionPreferences.getCleavageParameter().toString());
                    break;

                case enzyme:

                    digestionPreferences = searchParameters.getDigestionParameters();

                    if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                        String enzymeString = digestionPreferences.getEnzymes().stream()
                                .map(
                                        enzyme -> enzyme.getName()
                                )
                                .collect(
                                        Collectors.joining(", ")
                                );
                        writer.write(enzymeString);

                    }

                    break;

                case mc:

                    digestionPreferences = searchParameters.getDigestionParameters();

                    if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                        String nMissedCleavagesString = digestionPreferences.getEnzymes().stream()
                                .map(
                                        enzyme -> digestionPreferences.getnMissedCleavages(
                                                enzyme.getName()
                                        )
                                                .toString()
                                )
                                .collect(
                                        Collectors.joining(", ")
                                );
                        writer.write(nMissedCleavagesString);

                    }
                    break;
                case specificity:

                    digestionPreferences = searchParameters.getDigestionParameters();

                    if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                        String specificityString = digestionPreferences.getEnzymes().stream()
                                .map(
                                        enzyme -> digestionPreferences.getSpecificity(
                                                enzyme.getName()
                                        ).name
                                )
                                .collect(
                                        Collectors.joining(", ")
                                );
                        writer.write(specificityString);

                    }

                    break;

                case fixed_modifications:

                    writer.write(
                            searchParameters.getModificationParameters().getFixedModifications().stream()
                                    .collect(
                                            Collectors.joining(", ")
                                    )
                    );
                    break;
                case variable_modifications:

                    writer.write(
                            searchParameters.getModificationParameters().getVariableModifications().stream()
                                    .collect(
                                            Collectors.joining(", ")
                                    )
                    );
                    break;

                case refinement_variable_modifications:

                    writer.write(
                            searchParameters.getModificationParameters().getRefinementVariableModifications().stream()
                                    .collect(
                                            Collectors.joining(", ")
                                    )
                    );
                    break;

                case refinement_fixed_modifications:

                    writer.write(
                            searchParameters.getModificationParameters().getRefinementFixedModifications().stream()
                                    .collect(
                                            Collectors.joining(", ")
                                    )
                    );
                    break;

                case forward_ion:
                    writer.write(
                            searchParameters.getForwardIons().stream()
                                    .map(
                                            ion -> ion.toString()
                                    )
                                    .collect(
                                            Collectors.joining(", ")
                                    )
                    );
                    break;

                case rewind_ion:
                    writer.write(
                            searchParameters.getRewindIons().stream()
                                    .map(
                                            ion -> ion.toString()
                                    )
                                    .collect(
                                            Collectors.joining(", ")
                                    )
                    );
                    break;

                case fragment_tolerance:
                    writer.write(
                            Double.toString(
                                    searchParameters.getFragmentIonAccuracy()
                            )
                    );
                    break;
                    
                case precursor_tolerance_unit:
                    writer.write(
                            searchParameters.getPrecursorAccuracyType().toString()
                    );
                    break;
                    
                case fragment_tolerance_unit:
                    writer.write(
                            searchParameters.getFragmentAccuracyType().toString()
                    );
                    break;
                    
                case precursor_tolerance:
                    writer.write(
                            Double.toString(
                                    searchParameters.getPrecursorAccuracy()
                            )
                    );
                    break;
                    
                default:
                    writer.write("Not implemented");
            
            }
            
            writer.newLine();
            line++;
        
        }
    }
}
