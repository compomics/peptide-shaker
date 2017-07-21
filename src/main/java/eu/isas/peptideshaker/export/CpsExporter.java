package eu.isas.peptideshaker.export;

import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.parameters.PeptideShakerSettings;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.*;
import java.sql.SQLException;
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
    public static void saveAs(File destinationFile, WaitingHandler waitingHandler, Identification identification, ShotgunProtocol shotgunProtocol,
            IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, FilterPreferences filterPreferences,
            Metrics metrics, GeneMaps geneMaps, IdentificationFeaturesCache identificationFeaturesCache, boolean emptyCache,
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

            // transfer all files in the match directory
            if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
                waitingHandler.setPrimaryProgressCounterIndeterminate(true);
                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            }

            if (waitingHandler == null || !waitingHandler.isRunCanceled()) {
                identification.close();
                Util.copyFile(identification.getObjectsDB().getDbFile(), destinationFile);
                identification.getObjectsDB().establishConnection();
            }

        } finally {
            // Restore the project navigability
            identificationFeaturesCache.setReadOnly(false);
        }
    }
}
