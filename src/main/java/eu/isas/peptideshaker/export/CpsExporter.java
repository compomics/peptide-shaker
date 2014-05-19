package eu.isas.peptideshaker.export;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.TarUtils;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdFilter;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
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
     * Saves the given data in a cps file. Here the GUI preferences will be null
     * objects.
     *
     * @param destinationFile the destination cps file
     * @param waitingHandler a waiting handler used to cancel the saving
     * @param experiment the experiment to save
     * @param identification the identification to save
     * @param searchParameters the search parameters
     * @param annotationPreferences the annotation preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     * @param metrics the dataset metrics
     * @param processingPreferences the processing preferences
     * @param identificationFeaturesCache the identification features cache
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param genePreferences the gene preferences
     * @param objectsCache the object cache
     * @param emptyCache a boolean indicating whether the object cache should be
     * emptied
     * @param idFilter the identifications filter
     * @param jarFilePath the path to the jar file
     *
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws ArchiveException
     */
    public static void saveAs(File destinationFile, WaitingHandler waitingHandler, MsExperiment experiment, Identification identification, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails,
            Metrics metrics, ProcessingPreferences processingPreferences, IdentificationFeaturesCache identificationFeaturesCache, PTMScoringPreferences ptmScoringPreferences,
            GenePreferences genePreferences, ObjectsCache objectsCache, boolean emptyCache, IdFilter idFilter, String jarFilePath) throws IOException, SQLException, FileNotFoundException, ArchiveException {
        saveAs(destinationFile, waitingHandler, experiment, identification, searchParameters, annotationPreferences, spectrumCountingPreferences, projectDetails,
                null, metrics, processingPreferences, identificationFeaturesCache, ptmScoringPreferences, genePreferences, objectsCache, emptyCache, null, idFilter, jarFilePath);
    }

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
     * @param genePreferences the gene preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param objectsCache the object cache
     * @param emptyCache a boolean indicating whether the object cache should be
     * emptied
     * @param idFilter the identifications filter
     * @param jarFilePath the path to the jar file
     *
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws ArchiveException
     */
    public static void saveAs(File destinationFile, WaitingHandler waitingHandler, MsExperiment experiment, Identification identification, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails, FilterPreferences filterPreferences,
            Metrics metrics, ProcessingPreferences processingPreferences, IdentificationFeaturesCache identificationFeaturesCache, PTMScoringPreferences ptmScoringPreferences,
            GenePreferences genePreferences, ObjectsCache objectsCache, boolean emptyCache, DisplayPreferences displayPreferences, IdFilter idFilter, String jarFilePath)
            throws IOException, SQLException, FileNotFoundException, ArchiveException {

        // set the experiment parameters
        experiment.addUrParam(new PeptideShakerSettings(searchParameters, annotationPreferences, spectrumCountingPreferences,
                projectDetails, filterPreferences, displayPreferences, metrics, processingPreferences,
                identificationFeaturesCache, ptmScoringPreferences, genePreferences, idFilter));
        // Save the objects in cache
        objectsCache.saveCache(waitingHandler, emptyCache);
        // close connection
        identification.close();

        File matchesFolder = PeptideShaker.getSerializationDirectory(jarFilePath);

        // transfer all files in the match directory
        if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
            waitingHandler.setPrimaryProgressCounterIndeterminate(true);
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            File experimentFile = new File(matchesFolder, PeptideShaker.experimentObjectName);
            ExperimentIO.save(experimentFile, experiment);
        }

        identification.establishConnection(matchesFolder.getAbsolutePath(), false, objectsCache);

        // tar everything in the current cps file
        if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
            TarUtils.tarFolder(matchesFolder, destinationFile, waitingHandler);
        }
    }
}
