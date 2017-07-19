package eu.isas.peptideshaker.export;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.db.ObjectsDB;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.compression.TarUtils;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.parameters.PeptideShakerSettings;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.*;
import java.sql.SQLException;
import java.util.HashSet;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * This class will export an identification project as cps file.
 *
 * @author Marc Vaudel
 */
public class CpsExporter {

    /**
     * Saves the given data in a cps file.
     *
     * @param destinationFile the destination cps file
     * @param waitingHandler a waiting handler used to cancel the saving
     * @param experiment the experiment to save
     * @param identification the identification to save
     * @param shotgunProtocol information about the protocol used
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     * @param filterPreferences the filtering preferences
     * @param displayPreferences the display preferences
     * @param metrics the dataset
     * @param geneMaps the gene maps
     * @param identificationFeaturesCache the identification features cache
     * @param objectsCache the object cache
     * @param emptyCache a boolean indicating whether the object cache should be
     * emptied
     * @param dbFolder the path to the folder where the database is located
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws SQLException thrown of SQLException occurs exception thrown
     * whenever an error occurred while interacting with the database
     * @throws ArchiveException thrown of ArchiveException occurs exception
     * thrown whenever an error occurred while taring the project
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while saving the project
     */
    public static void saveAs(File destinationFile, WaitingHandler waitingHandler, MsExperiment experiment, Identification identification, ShotgunProtocol shotgunProtocol,
            IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, FilterPreferences filterPreferences,
            Metrics metrics, GeneMaps geneMaps, IdentificationFeaturesCache identificationFeaturesCache, ObjectsCache objectsCache, boolean emptyCache,
            DisplayPreferences displayPreferences, File dbFolder) throws IOException, SQLException, ArchiveException, ClassNotFoundException, InterruptedException {

        identificationFeaturesCache.setReadOnly(true);

        try {

            // save the user advocates
            projectDetails.setUserAdvocateMapping(Advocate.getUserAdvocates());

            // set the experiment parameters
            PeptideShakerSettings peptideShakerSettings = new PeptideShakerSettings(shotgunProtocol, identificationParameters, spectrumCountingPreferences,
                    projectDetails, filterPreferences, displayPreferences, metrics, geneMaps, identificationFeaturesCache);
            if (!identification.contains(PeptideShakerSettings.nameInCpsSettingsTable)) {
                identification.addObject(PeptideShakerSettings.nameInCpsSettingsTable, peptideShakerSettings);
            }

            // save the objects in cache
            objectsCache.saveCache(waitingHandler, emptyCache);
            objectsCache.setReadOnly(true);

            // close connection
            identification.close();

            // transfer all files in the match directory
            if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
                waitingHandler.setPrimaryProgressCounterIndeterminate(true);
                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            }

            if (waitingHandler == null || !waitingHandler.isRunCanceled()) {
                File experimentFile = new File(dbFolder, MsExperiment.experimentObjectName);
                ExperimentIO.save(experimentFile, experiment);
            }

            // tar everything in the current cps file
            /*
            if (waitingHandler == null || !waitingHandler.isRunCanceled()) {
                File logFolder = new File(objectsDB.getPath(), "log");
                HashSet<String> exceptions = new HashSet<String>(1);
                for (File file : logFolder.listFiles()) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".dat") && fileName.startsWith("log")) {
                        exceptions.add(file.getAbsolutePath());
                    }
                }
                TarUtils.tarFolderContent(dbFolder, destinationFile, exceptions, waitingHandler);
            }
            */

        } finally {
            // Restore the project navigability
            objectsCache.setReadOnly(false);
            identificationFeaturesCache.setReadOnly(false);
            /*
            if (!identification.isConnectionActive()) {
                identification.restoreConnection(dbFolder.getAbsolutePath(), false, objectsCache);
            }
            */
        }
    }
}
