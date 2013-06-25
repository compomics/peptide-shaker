package eu.isas.peptideshaker.export.sections;

import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.ProjectFeatures;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class outputs the project related export features.
 *
 * @author Marc Vaudel
 */
public class ProjectSection {

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
     * Constructor
     *
     * @param exportFeatures the features to export in this section
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public ProjectSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        this.exportFeatures = exportFeatures;
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param experiment the experiment name
     * @param sample the sample name
     * @param replicateNumber the replicate number
     * @param projectDetails the details of this project
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     */
    public void writeSection(String experiment, String sample, int replicateNumber, ProjectDetails projectDetails, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressDialogIndeterminate(true);
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
            ProjectFeatures projectFeatures = (ProjectFeatures) exportFeature;
            switch (projectFeatures) {
                case date:
                    writer.write(projectDetails.getCreationDate() + "");
                    break;
                case experiment:
                    writer.write(experiment);
                    break;
                case peptide_shaker:
                    writer.write(projectDetails.getPeptideShakerVersion());
                    break;
                case replicate:
                    writer.write(replicateNumber + "");
                    break;
                case sample:
                    writer.write(sample);
                    break;
                case search_engines:
                    ArrayList<String> searchEngines = new ArrayList<String>();
                    IdfileReaderFactory idFileReaderFactory = IdfileReaderFactory.getInstance();
                    ArrayList<File> idFiles = projectDetails.getIdentificationFiles();
                    for (File idFile : idFiles) {
                        String searchEngine = SearchEngine.getName(idFileReaderFactory.getSearchEngine(idFile));
                        if (!searchEngines.contains(searchEngine)) {
                            searchEngines.add(searchEngine);
                        }
                    }
                    Collections.sort(searchEngines);
                    for (int i = 0 ; i < searchEngines.size() ; i++) {
                        if (i>0) {
                            if (i == searchEngines.size()-1) {
                                writer.write(" and ");
                            } else {
                                writer.write(",");
                            }
                        }
                        writer.write(searchEngines.get(i));
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
