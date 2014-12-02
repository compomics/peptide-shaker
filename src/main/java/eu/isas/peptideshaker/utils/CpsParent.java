package eu.isas.peptideshaker.utils;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.ProteinInferencePreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.CpsExporter;
import eu.isas.peptideshaker.fileimport.CpsFileImporter;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import eu.isas.peptideshaker.preferences.UserPreferences;
import eu.isas.peptideshaker.preferences.UserPreferencesParent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Implementing this abstract class allows interacting with a cps files.
 *
 * @author Marc Vaudel
 */
public class CpsParent extends UserPreferencesParent {

    /**
     * The identification.
     */
    protected Identification identification;
    /**
     * The identification features generator.
     */
    protected IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The spectrum counting preferences.
     */
    protected SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * The project details.
     */
    protected ProjectDetails projectDetails;
    /**
     * The processing preferences.
     */
    protected ProcessingPreferences processingPreferences;
    /**
     * The metrics stored during processing.
     */
    protected Metrics metrics;
    /**
     * The MS experiment class.
     */
    protected MsExperiment experiment;
    /**
     * The sample.
     */
    protected Sample sample;
    /**
     * The replicate number.
     */
    protected int replicateNumber;
    /**
     * The proteomic analysis.
     */
    protected ProteomicAnalysis proteomicAnalysis;
    /**
     * The cache used to store objects.
     */
    protected ObjectsCache objectsCache;
    /**
     * The filter preferences.
     */
    protected FilterPreferences filterPreferences = new FilterPreferences();
    /**
     * The display preferences.
     */
    protected DisplayPreferences displayPreferences = new DisplayPreferences();
    /**
     * Information on the protocol used.
     */
    protected ShotgunProtocol shotgunProtocol;
    /**
     * The identification parameters.
     */
    protected IdentificationParameters identificationParameters;
    /**
     * The currently loaded cps file.
     */
    protected File cpsFile = null;

    /**
     * Loads the information from a cps file.
     *
     * @param jarFilePath the path to the jar file
     * @param waitingHandler a waiting handler displaying feedback to the user.
     * Ignored if null
     *
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     * @throws SQLException thrown if SQLException occurs
     * @throws ClassNotFoundException thrown if ClassNotFoundException occurs
     */
    public void loadCpsFile(String jarFilePath, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException, SQLException {

        CpsFileImporter cpsFileImporter = new CpsFileImporter(cpsFile, jarFilePath, waitingHandler);

        // close any open connection to an identification database
        if (identification != null) {
            identification.close();
        }

        // Get the experiment data
        experiment = cpsFileImporter.getExperiment();
        ArrayList<Sample> samples = cpsFileImporter.getSamples();
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("No sample found for the experiment " + experiment.getReference());
        }
        sample = samples.get(0);
        if (samples.size() > 1) { // pretty unlikely to happen for now
            String message = samples.size() + " samples found in experiment " + experiment.getReference() + ", sample " + sample.getReference() + " selected by default.";
            if (waitingHandler != null) {
                waitingHandler.appendReport(message, true, true);
            }
        }
        ArrayList<Integer> replicates = cpsFileImporter.getReplicates(sample);
        if (replicates == null || replicates.isEmpty()) {
            throw new IllegalArgumentException("No replicate found for the sample " + sample.getReference() + " of experiment " + experiment.getReference());
        }
        replicateNumber = replicates.get(0);
        if (replicates.size() > 1) { // pretty unlikely to happen for now
            if (waitingHandler != null) {
                waitingHandler.appendReport(replicates.size() + " replicates found in sample " + sample.getReference()
                        + " of experiment " + experiment.getReference() + ", replicate " + sample.getReference() + " selected by default.", true, true);
            }
        }
        proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);

        // Get PeptideShaker settings
        PeptideShakerSettings experimentSettings = cpsFileImporter.getExperimentSettings();
        identificationParameters = experimentSettings.getIdentificationParameters();
        spectrumCountingPreferences = experimentSettings.getSpectrumCountingPreferences();
        projectDetails = experimentSettings.getProjectDetails();
        HashMap<Integer, Advocate> userAdvocateMapping = projectDetails.getUserAdvocateMapping();
        if (userAdvocateMapping != null) {
            Advocate.setUserAdvocates(userAdvocateMapping);
        }
        processingPreferences = experimentSettings.getProcessingPreferences();
        metrics = experimentSettings.getMetrics();
        filterPreferences = experimentSettings.getFilterPreferences();
        displayPreferences = experimentSettings.getDisplayPreferences();
        shotgunProtocol = experimentSettings.getShotgunProtocol();

        // backwards compatability for the filter preferences
        if (filterPreferences == null) {
            filterPreferences = new FilterPreferences();
        }

        // backwards compatability for the display preferences
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        if (displayPreferences != null) {
            displayPreferences.compatibilityCheck(searchParameters.getModificationProfile());
        } else {
            displayPreferences = new DisplayPreferences();
            displayPreferences.setDefaultSelection(searchParameters.getModificationProfile());
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            waitingHandler.setRunFinished();
            return;
        }

        // Backward compatibility for the shotgun protocol
        if (shotgunProtocol == null) {
            shotgunProtocol = ShotgunProtocol.inferProtocolFromSearchSettings(searchParameters);
        }

        // Get identification details and set up caches
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        if (identification.getSpectrumIdentificationMap() == null) {
            // 0.18 version, needs update of the spectrum mapping
            identification.updateSpectrumMapping();
        }
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, shotgunProtocol, identificationParameters, metrics, spectrumCountingPreferences);
        if (experimentSettings.getIdentificationFeaturesCache() != null) {
            identificationFeaturesGenerator.setIdentificationFeaturesCache(experimentSettings.getIdentificationFeaturesCache());
        }
        objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);
        String dbFolder = PeptideShaker.getSerializationDirectory(jarFilePath).getAbsolutePath();
        identification.establishConnection(dbFolder, false, objectsCache);
        loadUserPreferences();
        userPreferences.addRecentProject(cpsFile);
        saveUserPreferences();
    }

    /**
     * Saves the project in the cps file.
     *
     * @param waitingHandler waiting handler displaying feedback to the user.
     * can be null.
     * @param emptyCache if true the cache will be emptied
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     * @throws SQLException thrown if SQLException occurs
     * @throws ArchiveException thrown if ArchiveException occurs
     */
    public void saveProject(WaitingHandler waitingHandler, boolean emptyCache) throws IOException, SQLException, FileNotFoundException, ArchiveException {
        CpsExporter.saveAs(cpsFile, waitingHandler, experiment, identification, shotgunProtocol, identificationParameters,
                spectrumCountingPreferences, projectDetails, metrics,
                processingPreferences, identificationFeaturesGenerator.getIdentificationFeaturesCache(),
                objectsCache, emptyCache, PeptideShaker.getJarFilePath());

        loadUserPreferences();
        userPreferences.addRecentProject(cpsFile);
        saveUserPreferences();
    }

    /**
     * Loads the FASTA file in the sequence factory.
     *
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null.
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     * @throws ClassNotFoundException thrown if ClassNotFoundException occurs
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadFastaFile(WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {
        return loadFastaFile(null, waitingHandler);
    }

    /**
     * Loads the FASTA file in the sequence factory.
     *
     * @param folder a folder to look into, the user last selected folder for
     * instance, can be null
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     * @throws ClassNotFoundException thrown if ClassNotFoundException occurs
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadFastaFile(File folder, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();

        // Load fasta file
        ProteinInferencePreferences proteinInferencePreferences = identificationParameters.getProteinInferencePreferences();
        File providedFastaLocation = proteinInferencePreferences.getProteinSequenceDatabase();
        String fileName = providedFastaLocation.getName();
        File projectFolder = cpsFile.getParentFile();
        File dataFolder = new File(projectFolder, "data");

        if (providedFastaLocation.exists()) {
            sequenceFactory.loadFastaFile(providedFastaLocation, waitingHandler);
        } else if (folder != null && new File(folder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(folder, fileName), waitingHandler);
            proteinInferencePreferences.setProteinSequenceDatabase(new File(folder, fileName));
        } else if (new File(projectFolder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(projectFolder, fileName), waitingHandler);
            proteinInferencePreferences.setProteinSequenceDatabase(new File(projectFolder, fileName));
        } else if (new File(dataFolder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(dataFolder, fileName), waitingHandler);
            proteinInferencePreferences.setProteinSequenceDatabase(new File(dataFolder, fileName));
        } else {
            return false;
        }

        return true;
    }

    /**
     * Loads the spectra in the spectrum factory.
     *
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null.
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadSpectrumFiles(WaitingHandler waitingHandler) throws FileNotFoundException, IOException {
        return loadSpectrumFiles(null, waitingHandler);
    }

    /**
     * Loads the spectra in the spectrum factory.
     *
     * @param folder a folder to look into, the user last selected folder for
     * instance, can be null
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadSpectrumFiles(File folder, WaitingHandler waitingHandler) throws FileNotFoundException, IOException {

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        for (String spectrumFileName : identification.getSpectrumFiles()) {
            File providedSpectrumLocation = projectDetails.getSpectrumFile(spectrumFileName);
            File projectFolder = cpsFile.getParentFile();
            File dataFolder = new File(projectFolder, "data");
            // try to locate the spectrum file
            if (providedSpectrumLocation == null || !providedSpectrumLocation.exists()) {
                File fileInProjectFolder = new File(projectFolder, spectrumFileName);
                File fileInDataFolder = new File(dataFolder, spectrumFileName);
                File fileInGivenFolder = new File(folder, spectrumFileName);
                if (fileInProjectFolder.exists()) {
                    projectDetails.addSpectrumFile(fileInProjectFolder);
                } else if (fileInDataFolder.exists()) {
                    projectDetails.addSpectrumFile(fileInDataFolder);
                } else if (fileInGivenFolder.exists()) {
                    projectDetails.addSpectrumFile(fileInDataFolder);
                } else {
                    return false;
                }
            }
            File mgfFile = projectDetails.getSpectrumFile(spectrumFileName);
            spectrumFactory.addSpectra(mgfFile, waitingHandler);
        }

        return true;
    }

    /**
     * Loads the spectra in the spectrum factory.
     *
     * @param spectrumFileName the name of the spectrum file to load
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     * @throws IOException thrown of IOException occurs
     * @throws FileNotFoundException thrown if FileNotFoundException occurs
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadSpectrumFile(String spectrumFileName, WaitingHandler waitingHandler) throws FileNotFoundException, IOException {

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        File providedSpectrumLocation = projectDetails.getSpectrumFile(spectrumFileName);
        File projectFolder = cpsFile.getParentFile();
        File dataFolder = new File(projectFolder, "data");

        // try to locate the spectrum file
        if (providedSpectrumLocation == null || !providedSpectrumLocation.exists()) {
            File fileInProjectFolder = new File(projectFolder, spectrumFileName);
            File fileInDataFolder = new File(dataFolder, spectrumFileName);

            if (fileInProjectFolder.exists()) {
                projectDetails.addSpectrumFile(fileInProjectFolder);
            } else if (fileInDataFolder.exists()) {
                projectDetails.addSpectrumFile(fileInDataFolder);
            } else {
                return false;
            }
        }

        File mgfFile = projectDetails.getSpectrumFile(spectrumFileName);
        spectrumFactory.addSpectra(mgfFile, waitingHandler);

        return true;
    }

    /**
     * Imports the gene mapping.
     *
     * @param waitingHandler the waiting handler
     * @param jarFilePath the path to the jar file
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadGeneMappings(String jarFilePath, WaitingHandler waitingHandler) {
        GenePreferences genePreferences;
        if (identificationParameters == null) {
            genePreferences = new GenePreferences();
        } else {
            genePreferences = identificationParameters.getGenePreferences();
        }
        return genePreferences.loadGeneMappings(jarFilePath, waitingHandler);
    }

    /**
     * Returns the identification object.
     *
     * @return the identification object
     */
    public Identification getIdentification() {
        return identification;
    }

    /**
     * Returns the identification features generator object.
     *
     * @return the identification features generator object
     */
    public IdentificationFeaturesGenerator getIdentificationFeaturesGenerator() {
        return identificationFeaturesGenerator;
    }

    /**
     * Returns the spectrum counting preferences.
     *
     * @return the spectrum counting preferences
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return spectrumCountingPreferences;
    }

    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        return projectDetails;
    }

    /**
     * Returns the processing preferences.
     *
     * @return the processing preferences
     */
    public ProcessingPreferences getProcessingPreferences() {
        return processingPreferences;
    }

    /**
     * Returns the metrics object.
     *
     * @return the metrics object
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Returns the experiment object.
     *
     * @return the experiment object
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicate number
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * Returns the proteomics analysis object.
     *
     * @return the proteomics analysis object
     */
    public ProteomicAnalysis getProteomicAnalysis() {
        return proteomicAnalysis;
    }

    /**
     * Returns the object cache.
     *
     * @return the object cache
     */
    public ObjectsCache getObjectsCache() {
        return objectsCache;
    }

    /**
     * Returns the filter preferences.
     *
     * @return the filter preferences
     */
    public FilterPreferences getFilterPreferences() {
        return filterPreferences;
    }

    /**
     * Returns the display preferences.
     *
     * @return the display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        return displayPreferences;
    }

    /**
     * Returns the cps file.
     *
     * @return the cps file
     */
    public File getCpsFile() {
        return cpsFile;
    }

    /**
     * Set the identification feature generator.
     *
     * @param identificationFeaturesGenerator the identification feature
     * generator
     */
    public void setIdentificationFeaturesGenerator(IdentificationFeaturesGenerator identificationFeaturesGenerator) {
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
    }

    /**
     * Set the spectrum counting preferences.
     *
     * @param spectrumCountingPreferences the spectrum counting preferences
     */
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }

    /**
     * Set the project details.
     *
     * @param projectDetails the project details
     */
    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
    }

    /**
     * Set the processing preferences.
     *
     * @param processingPreferences the processing preferences
     */
    public void setProcessingPreferences(ProcessingPreferences processingPreferences) {
        this.processingPreferences = processingPreferences;
    }

    /**
     * Set the metrics.
     *
     * @param metrics the metrics
     */
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Set the objects cache.
     *
     * @param objectsCache the objects cache
     */
    public void setObjectsCache(ObjectsCache objectsCache) {
        this.objectsCache = objectsCache;
    }

    /**
     * Set the filter preferences.
     *
     * @param filterPreferences the filter preferences
     */
    public void setFilterPreferences(FilterPreferences filterPreferences) {
        this.filterPreferences = filterPreferences;
    }

    /**
     * Set the display preferences.
     *
     * @param displayPreferences the display preferences
     */
    public void setDisplayPreferences(DisplayPreferences displayPreferences) {
        this.displayPreferences = displayPreferences;
    }

    /**
     * Set the cps file.
     *
     * @param cpsFile the cps file
     */
    public void setCpsFile(File cpsFile) {
        this.cpsFile = cpsFile;
    }

    /**
     * Returns the user preferences.
     *
     * @return the user preferences
     */
    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    /**
     * Set the identification object.
     *
     * @param identification the identification object
     */
    public void setIdentification(Identification identification) {
        this.identification = identification;
    }

    /**
     * Sets the project.
     *
     * @param experiment the experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
     */
    public void setProject(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
    }

    /**
     * Set the default preferences.
     */
    public void setDefaultPreferences() {
        SearchParameters searchParameters = new SearchParameters();
        identificationParameters = IdentificationParameters.getDefaultIdentificationParameters(searchParameters);
        spectrumCountingPreferences = new SpectrumCountingPreferences();
        spectrumCountingPreferences.setSelectedMethod(SpectralCountingMethod.NSAF);
        spectrumCountingPreferences.setValidatedHits(true);
        processingPreferences = new ProcessingPreferences();
    }

    /**
     * Resets the feature generator.
     */
    public void resetIdentificationFeaturesGenerator() {
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, shotgunProtocol, identificationParameters, metrics, spectrumCountingPreferences);
    }

    /**
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public IdentificationParameters getIdentificationParameters() {
        return identificationParameters;
    }

    /**
     * Sets new identification parameters.
     *
     * @param identificationParameters the new identification parameters
     */
    public void setIdentificationParameters(IdentificationParameters identificationParameters) {
        this.identificationParameters = identificationParameters;
    }

    /**
     * Returns information on the protocol used.
     *
     * @return information on the protocol used
     */
    public ShotgunProtocol getShotgunProtocol() {
        return shotgunProtocol;
    }

    /**
     * Sets the shotgun protocol.
     *
     * @param shotgunProtocol the shotgun protocol
     */
    public void setShotgunProtocol(ShotgunProtocol shotgunProtocol) {
        this.shotgunProtocol = shotgunProtocol;
    }

    /**
     * Returns an extended HTML project report.
     *
     * @param waitingHandlerReport the progress report, if null the report from
     * the project details will be used
     * @return an extended HTML project report
     */
    public String getExtendedProjectReport(String waitingHandlerReport) {

        String report = null;

        if (projectDetails != null && getIdentification() != null) {

            report = "<html><br>";
            report += "<b>Experiment</b>: " + experiment.getReference() + "<br>";
            report += "<b>Sample:</b> " + sample.getReference() + "<br>";
            report += "<b>Replicate number:</b> " + replicateNumber + "<br><br>";

            report += "<b>Creation Date:</b> " + projectDetails.getCreationDate() + "<br><br>";

            report += "<b>Identification Files</b>:<br>";
            for (File idFile : projectDetails.getIdentificationFiles()) {
                report += idFile.getAbsolutePath() + " - ";
                HashMap<String, ArrayList<String>> versions = projectDetails.getIdentificationAlgorithmsForFile(idFile.getName());
                ArrayList<String> software = new ArrayList<String>(versions.keySet());
                Collections.sort(software);
                boolean first = true;
                for (String softwareName : software) {
                    if (first) {
                        first = false;
                    } else {
                        report += ", ";
                    }
                    report += softwareName;
                    ArrayList<String> algorithmVersions = versions.get(softwareName);
                    if (algorithmVersions != null && !algorithmVersions.isEmpty()) {
                        report += " - (";
                        boolean firstVersion = true;
                        for (String version : algorithmVersions) {
                            if (firstVersion) {
                                firstVersion = false;
                            } else {
                                report += ", ";
                            }
                            if (version != null) {
                                report += version;
                            } else {
                                report += "unknown version";
                            }
                        }
                        report += ")";
                    }
                }

                report += "<br>";
            }

            report += "<br><b>Spectrum Files:</b><br>";
            for (String mgfFileNames : getIdentification().getSpectrumFiles()) {
                report += projectDetails.getSpectrumFile(mgfFileNames).getAbsolutePath() + "<br>";
            }

            report += "<br><b>FASTA File (identification):</b><br>";
            report += identificationParameters.getSearchParameters().getFastaFile().getAbsolutePath() + "<br>";

            report += "<br><b>FASTA File (protein inference):</b><br>";
            report += identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase().getAbsolutePath() + "<br>";

            report += "<br><br><b>Report:</b><br>";
            if (waitingHandlerReport == null) {
                waitingHandlerReport = projectDetails.getReport();
            }

            if (waitingHandlerReport.lastIndexOf("<br>") == -1) {
                report += "<pre>" + waitingHandlerReport + "</pre>";
            } else {
                report += waitingHandlerReport;
            }

            report += "</html>";
        } else {
            report = "<html><br>";

            report += "<b>Report:</b><br>";
            if (waitingHandlerReport != null) {
                if (waitingHandlerReport.lastIndexOf("<br>") == -1) {
                    report += "<pre>" + waitingHandlerReport + "</pre>";
                } else {
                    report += waitingHandlerReport;
                }
            }

            report += "</html>";
        }

        return report;
    }
}
