package eu.isas.peptideshaker.export;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.TarUtils;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSSettings;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SearchParameters;
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
     * @param experiment the experiment to save 
     * @param identification the identification to save
     * @param searchParameters the search parameters
     * @param annotationPreferences the annotation preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     * @param filterPreferences the filtering preferences
     * @param displayPreferences the display preferences
     * @param metrics the dataset metrics
     * @param processingPreferences the processing preferences
     * @param identificationFeaturesCache the identification features cache
     * @param ptmScoringPreferences the ptm scoring preferences
     * @param objectsCache the object cache
     * @param emptyCache a boolean indicating whether the object cache should be
     * emptied
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws ArchiveException
     */
    public static void saveAs(File destinationFile, WaitingHandler waitingHandler, MsExperiment experiment, Identification identification, SearchParameters searchParameters, 
            AnnotationPreferences annotationPreferences, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, FilterPreferences filterPreferences, 
            DisplayPreferences displayPreferences, Metrics metrics, ProcessingPreferences processingPreferences, IdentificationFeaturesCache identificationFeaturesCache, 
            PTMScoringPreferences ptmScoringPreferences, ObjectsCache objectsCache, boolean emptyCache) throws IOException, SQLException, FileNotFoundException, ArchiveException {

        // set the experiment parameters
        experiment.addUrParam(new PSSettings(searchParameters, annotationPreferences, spectrumCountingPreferences,
                projectDetails, filterPreferences, displayPreferences, metrics, processingPreferences,
                identificationFeaturesCache, ptmScoringPreferences));

        objectsCache.saveCache(waitingHandler, emptyCache);
        identification.close();

        // transfer all files in the match directory
        if (!waitingHandler.isRunCanceled()) {
            waitingHandler.getPrimaryProgressBar().setIndeterminate(true);
            waitingHandler.getPrimaryProgressBar().setStringPainted(false);
            File experimentFile = new File(PeptideShaker.SERIALIZATION_DIRECTORY, PeptideShaker.experimentObjectName);
            ExperimentIO.save(experimentFile, experiment);
        }

        identification.establishConnection(PeptideShaker.SERIALIZATION_DIRECTORY, false, objectsCache);

        // tar everything in the current cps file file
        if (!waitingHandler.isRunCanceled()) {
            File matchesFolder = new File(PeptideShaker.SERIALIZATION_DIRECTORY);
            TarUtils.tarFolder(matchesFolder, destinationFile, waitingHandler);
        }
    }
}
