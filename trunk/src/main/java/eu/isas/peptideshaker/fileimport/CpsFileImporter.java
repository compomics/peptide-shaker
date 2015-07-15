package eu.isas.peptideshaker.fileimport;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.compression.TarUtils;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * The cps file importer extracts the information contained in a cps file.
 *
 * @author Marc Vaudel
 */
public class CpsFileImporter {

    /**
     * The experiment object.
     */
    private MsExperiment experiment;

    /**
     * Constructor.
     *
     * @param cpsFile the cps file
     * @param jarFilePath the path to the jar file
     * @param waitingHandler the waiting handler
     *
     * @throws FileNotFoundException thrown if the file to import cannot be
     * found
     * @throws IOException thrown if there is a problem extracting or loading
     * the file
     * @throws ClassNotFoundException thrown if there is a problem loading the
     * experiment data
     * @throws org.apache.commons.compress.archivers.ArchiveException exception thrown whenever an error occurred while untaring the file
     */
    public CpsFileImporter(File cpsFile, String jarFilePath, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException, ArchiveException {

        File matchFolderParent = PeptideShaker.getMatchesDirectoryParent(jarFilePath);
        File matchFolder = PeptideShaker.getSerializationDirectory(jarFilePath);

        // empty the existing files in the matches folder
        if (matchFolder.exists()) {
            for (File file : matchFolder.listFiles()) {
                if (file.isDirectory()) {
                    boolean deleted = Util.deleteDir(file);

                    if (!deleted) {
                        System.out.println("Failed to delete folder: " + file.getPath());
                    }
                } else {
                    boolean deleted = file.delete();

                    if (!deleted) {
                        System.out.println("Failed to delete file: " + file.getPath());
                    }
                }
            }
        }

        File experimentFile = new File(matchFolder, PeptideShaker.getDefaultExperimentFileName());

        if (waitingHandler != null) {
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(100);
        }

            TarUtils.extractFile(cpsFile, matchFolderParent, waitingHandler);

        experiment = ExperimentIO.loadExperiment(experimentFile);
    }

    /**
     * Returns the experiment settings as imported from the cps file.
     *
     * @return the experiment settings as imported from the cps file
     */
    public PeptideShakerSettings getExperimentSettings() {

        PeptideShakerSettings experimentSettings = new PeptideShakerSettings();
            experimentSettings = (PeptideShakerSettings) experiment.getUrParam(experimentSettings);

        return experimentSettings;
    }

    /**
     * Returns the samples.
     *
     * @return the samples
     */
    public ArrayList<Sample> getSamples() {
        return new ArrayList(experiment.getSamples().values());
    }

    /**
     * Returns the replicates for a given sample.
     *
     * @param sample the sample
     * @return the replicates
     */
    public ArrayList<Integer> getReplicates(Sample sample) {
        return new ArrayList(experiment.getAnalysisSet(sample).getReplicateNumberList());
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("CpsFileImporter.class").getPath(), "PeptideShaker");
    }
}
