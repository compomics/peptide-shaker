package eu.isas.peptideshaker.export;

import com.compomics.util.db.object.objects.BlobObject;
import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import eu.isas.peptideshaker.preferences.FilterParameters;
import eu.isas.peptideshaker.parameters.PeptideShakerSettings;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.*;
import java.sql.SQLException;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * This class exports a PeptideShaker project as cpsx file.
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
     * @throws IOException thrown whenever an error occurred while reading or
     * writing a file
     * @throws java.lang.InterruptedException exception thrown if a thread is
     * interrupted while saving the project
     */
    public static void saveAs(File destinationFile, WaitingHandler waitingHandler, Identification identification, ShotgunProtocol shotgunProtocol,
            IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, FilterParameters filterPreferences,
            Metrics metrics, GeneMaps geneMaps, IdentificationFeaturesCache identificationFeaturesCache, boolean emptyCache,
            DisplayParameters displayPreferences, File dbFolder) throws IOException, InterruptedException {

        identificationFeaturesCache.setReadOnly(true);

        try {

            // save the user advocates
            projectDetails.setUserAdvocateMapping(Advocate.getUserAdvocates());

            // add all necessary data and parameters into the db for export
            if (!identification.contains(PeptideShakerSettings.nameInCpsSettingsTable)) {

                PeptideShakerSettings peptideShakerSettings = new PeptideShakerSettings(shotgunProtocol, identificationParameters, spectrumCountingPreferences,
                        projectDetails, filterPreferences, displayPreferences, metrics, geneMaps, identificationFeaturesCache);

                BlobObject blobObject = new BlobObject(peptideShakerSettings);
                identification.addObject(PeptideShakerSettings.nameInCpsSettingsTable, blobObject);

            }

            PSMaps psMaps = new PSMaps();
            long psMapsIdentKey = ExperimentObject.asLong(psMaps.getParameterKey() + "_identification");

            if (!identification.contains(psMapsIdentKey)) {

                identification.addObject(psMapsIdentKey, identification.getUrParam(psMaps));

            }

            // transfer all files in the match directory
            if (waitingHandler != null && !waitingHandler.isRunCanceled()) {

                waitingHandler.setPrimaryProgressCounterIndeterminate(true);
                waitingHandler.setSecondaryProgressCounterIndeterminate(true);

            }

            if (waitingHandler == null || !waitingHandler.isRunCanceled()) {

                identification.getObjectsDB().close(false);
                Util.copyFile(identification.getObjectsDB().getDbFile(), destinationFile);
                identification.getObjectsDB().establishConnection(false);

            }

        } finally {

            // Restore the project navigability
            identificationFeaturesCache.setReadOnly(false);

        }
    }
}
