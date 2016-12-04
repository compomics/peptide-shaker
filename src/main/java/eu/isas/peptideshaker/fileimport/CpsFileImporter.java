package eu.isas.peptideshaker.fileimport;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.db.ObjectsDB;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.compression.TarUtils;
import eu.isas.peptideshaker.parameters.PeptideShakerSettings;
import eu.isas.peptideshaker.utils.CpsParent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
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
     * @param dbFolder the path where to store the database
     * @param waitingHandler the waiting handler
     *
     * @throws FileNotFoundException thrown if the file to import cannot be
     * found
     * @throws IOException thrown if there is a problem extracting or loading
     * the file
     * @throws ClassNotFoundException thrown if there is a problem loading the
     * experiment data
     * @throws org.apache.commons.compress.archivers.ArchiveException exception
     * thrown whenever an error occurred while untaring the file
     */
    public CpsFileImporter(File cpsFile, File dbFolder, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException, ArchiveException {

        if (waitingHandler != null) {
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(100);
        }

        TarUtils.extractFile(cpsFile, dbFolder, waitingHandler);

        File experimentFile = new File(dbFolder, MsExperiment.experimentObjectName);
        experiment = ExperimentIO.loadExperiment(experimentFile);
    }

    /**
     * Returns the PeptideShaker settings saved in the given database.
     *
     * @param objectsDB the database containing the settings
     *
     * @return the PeptideShaker settings
     *
     * @throws SQLException exception thrown whenever an error occurs while
     * querying the database
     * @throws IOException exception thrown whenever an error occurs while
     * querying the database
     * @throws ClassNotFoundException exception thrown whenever an error occurs
     * while deserializing the settings object
     * @throws InterruptedException exception thrown whenever an threading issue
     * occurs while querying the database.
     */
    public PeptideShakerSettings getPeptideShakerSettings(ObjectsDB objectsDB) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        PeptideShakerSettings peptideShakerSettings = new PeptideShakerSettings();
        peptideShakerSettings = (PeptideShakerSettings) objectsDB.retrieveObject(CpsParent.settingsTableName, PeptideShakerSettings.nameInCpsSettingsTable, true, false);
        peptideShakerSettings.getIdentificationParameters().getSearchParameters().getDigestionPreferences(); // Backward compatibility check
        return peptideShakerSettings;
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
