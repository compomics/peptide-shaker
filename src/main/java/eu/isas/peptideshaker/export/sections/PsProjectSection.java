package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import eu.isas.peptideshaker.export.exportfeatures.PsProjectFeature;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class outputs the project related export features.
 *
 * @author Marc Vaudel
 */
public class PsProjectSection {

    /**
     * The features to export.
     */
    private final ArrayList<PsProjectFeature> projectFeatures;
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
     * Constructor
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public PsProjectSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        projectFeatures = new ArrayList<>(exportFeatures.size());
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsProjectFeature) {
                projectFeatures.add((PsProjectFeature) exportFeature);
            } else {
                throw new IllegalArgumentException("Impossible to export " + exportFeature.getClass().getName() + " as project feature.");
            }
        }
        Collections.sort(projectFeatures);
    }

    /**
     * Writes the desired section.
     *
     * @param experiment the experiment name
     * @param projectDetails the details of this project
     * @param waitingHandler the waiting handler
     * 
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file
     */
    public void writeSection(String experiment, ProjectDetails projectDetails, WaitingHandler waitingHandler) throws IOException {

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

        for (PsProjectFeature projectFeature : projectFeatures) {
            if (indexes) {
                writer.write(Integer.toString(line));
                writer.addSeparator();
            }
            writer.write(projectFeature.getTitle());
            writer.addSeparator();
            switch (projectFeature) {
                case date:
                    writer.write(projectDetails.getCreationDate() + "");
                    break;
                case experiment:
                    writer.write(experiment);
                    break;
                case peptide_shaker:
                    writer.write(projectDetails.getPeptideShakerVersion());
                    break;
                case identification_algorithms:
                    ArrayList<Integer> advocatesIds = projectDetails.getIdentificationAlgorithms();
                    Collections.sort(advocatesIds);
                    for (int i = 0; i < advocatesIds.size(); i++) {
                        if (i > 0) {
                            if (i == advocatesIds.size() - 1) {
                                writer.write(" and ");
                            } else {
                                writer.write(", ");
                            }
                        }
                        Integer advocateId = advocatesIds.get(i);
                        Advocate advocate = Advocate.getAdvocate(advocateId);
                        writer.write(advocate.getName());
                    }
                    break;
                case algorithms_versions:
                    advocatesIds = projectDetails.getIdentificationAlgorithms();
                    HashMap<String, ArrayList<String>> versions = projectDetails.getAlgorithmNameToVersionsMap();
                    Collections.sort(advocatesIds);
                    for (int i = 0; i < advocatesIds.size(); i++) {
                        if (i > 0) {
                            if (i == advocatesIds.size() - 1) {
                                writer.write(" and ");
                            } else {
                                writer.write(", ");
                            }
                        }
                        Integer advocateId = advocatesIds.get(i);
                        Advocate advocate = Advocate.getAdvocate(advocateId);
                        String advocateName = advocate.getName();
                        writer.write(advocateName + " (");
                        ArrayList<String> algorithmVersions = versions.get(advocateName);
                        if (algorithmVersions == null || algorithmVersions.isEmpty()) {
                            writer.write("unknown version)");
                        } else {
                            if (algorithmVersions.size() == 1) {
                                writer.write("version " + algorithmVersions.get(0) + ")");
                            } else {
                                writer.write("versions ");
                                for (int j = 0; j < algorithmVersions.size(); j++) {
                                    if (j > 0) {
                                        if (j == algorithmVersions.size() - 1) {
                                            writer.write(" and ");
                                        } else {
                                            writer.write(", ");
                                        }
                                    }
                                    writer.write(algorithmVersions.get(j));
                                }
                                writer.write(")");
                            }
                        }
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
