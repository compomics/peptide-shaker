package eu.isas.peptideshaker.utils;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.db.ObjectsDB;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.preferences.FractionSettings;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PSProcessingPreferences;
import com.compomics.util.preferences.ProteinInferencePreferences;
import eu.isas.peptideshaker.export.CpsExporter;
import eu.isas.peptideshaker.fileimport.CpsFileImporter;
import eu.isas.peptideshaker.parameters.PeptideShakerSettings;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import eu.isas.peptideshaker.preferences.UserPreferences;
import eu.isas.peptideshaker.preferences.UserPreferencesParent;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
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
 * @author Harald Barsnes
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
     * The metrics stored during processing.
     */
    protected Metrics metrics;
    /**
     * The gene maps.
     */
    protected GeneMaps geneMaps;
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
     * The folder where the database is stored.
     */
    protected File dbFolder;
    /**
     * The currently loaded cps file.
     */
    protected File cpsFile = null;
    /**
     * The name of the table to use to store PeptideShaker experiment settings.
     */
    public static final String settingsTableName = "PeptideShaker_experiment_settings";

    /**
     * Empty constructor for instantiation purposes.
     */
    public CpsParent() {

    }

    /**
     * Constructor.
     *
     * @param dbFolder the folder where the database is stored.
     */
    public CpsParent(File dbFolder) {
        this.dbFolder = dbFolder;
    }

    /**
     * Loads the information from a cps file.
     *
     * @param zipFile the zip file containing the cps file
     * @param dbFolder the folder where to untar the project
     * @param waitingHandler a waiting handler displaying feedback to the user.
     * Ignored if null
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws SQLException thrown of SQLException occurs exception thrown
     * whenever an error occurred while interacting with the database
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while saving the project
     * @throws org.apache.commons.compress.archivers.ArchiveException exception
     * thrown whenever an error occurs while untaring the file
     */
    public void loadCpsFromZipFile(File zipFile, File dbFolder, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException, SQLException, InterruptedException, ArchiveException {

        String newName = PsZipUtils.getTempFolderName(zipFile.getName());
        String parentFolder = PsZipUtils.getUnzipParentFolder();
        if (parentFolder == null) {
            parentFolder = zipFile.getParent();
        }
        File parentFolderFile = new File(parentFolder, PsZipUtils.getUnzipSubFolder());
        File destinationFolder = new File(parentFolderFile, newName);
        destinationFolder.mkdir();
        TempFilesManager.registerTempFolder(parentFolderFile);

        waitingHandler.setWaitingText("Unzipping " + zipFile.getName() + ". Please Wait...");
        ZipUtils.unzip(zipFile, destinationFolder, waitingHandler);
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        if (!waitingHandler.isRunCanceled()) {
            for (File file : destinationFolder.listFiles()) {
                if (file.getName().toLowerCase().endsWith(".cpsx")) {
                    cpsFile = file;
                    loadCpsFile(dbFolder, waitingHandler);
                    return;
                }
            }
        }
    }

    /**
     * Loads the information from a cps file.
     *
     * @param dbFolder the folder where to untar the project
     * @param waitingHandler a waiting handler displaying feedback to the user.
     * Ignored if null
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws SQLException thrown of SQLException occurs exception thrown
     * whenever an error occurred while interacting with the database
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while saving the project
     * @throws org.apache.commons.compress.archivers.ArchiveException exception
     * thrown whenever an error occurs while untaring the file
     */
    public void loadCpsFile(File dbFolder, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException, SQLException, InterruptedException, ArchiveException {

        CpsFileImporter cpsFileImporter = new CpsFileImporter(cpsFile, dbFolder, waitingHandler);

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
        if (samples.size() > 1) {
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
        if (replicates.size() > 1) {
            if (waitingHandler != null) {
                waitingHandler.appendReport(replicates.size() + " replicates found in sample " + sample.getReference()
                        + " of experiment " + experiment.getReference() + ", replicate " + sample.getReference() + " selected by default.", true, true);
            }
        }
        proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);

        // Get identification and restore connection
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);
        objectsCache.setReadOnly(false);
        identification.restoreConnection(dbFolder.getAbsolutePath(), false, objectsCache);

        // Get PeptideShaker settings
        PeptideShakerSettings experimentSettings = cpsFileImporter.getPeptideShakerSettings(identification.getIdentificationDB().getObjectsDB());
        identificationParameters = experimentSettings.getIdentificationParameters();
        spectrumCountingPreferences = experimentSettings.getSpectrumCountingPreferences();
        projectDetails = experimentSettings.getProjectDetails();
        HashMap<Integer, Advocate> userAdvocateMapping = projectDetails.getUserAdvocateMapping();
        if (userAdvocateMapping != null) {
            Advocate.setUserAdvocates(userAdvocateMapping);
        }
        metrics = experimentSettings.getMetrics();
        geneMaps = experimentSettings.getGeneMaps();
        filterPreferences = experimentSettings.getFilterPreferences();
        displayPreferences = experimentSettings.getDisplayPreferences();
        shotgunProtocol = experimentSettings.getShotgunProtocol();

        // Backward compatibility for the fraction settings
        FractionSettings fractionSettings = identificationParameters.getFractionSettings();
        if (fractionSettings == null) {
            fractionSettings = new FractionSettings();
        }
        PSProcessingPreferences deprecatedProcessingPreferences = experimentSettings.getProcessingPreferences();
        if (deprecatedProcessingPreferences != null) {
            Double fractionMw = deprecatedProcessingPreferences.getProteinConfidenceMwPlots();
            if (fractionMw != null) {
                fractionSettings.setProteinConfidenceMwPlots(fractionMw);
            }
        }

        // Set up caches
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, identificationParameters, metrics, spectrumCountingPreferences);
        IdentificationFeaturesCache identificationFeaturesCache = experimentSettings.getIdentificationFeaturesCache();
        if (identificationFeaturesCache != null) {
            identificationFeaturesGenerator.setIdentificationFeaturesCache(experimentSettings.getIdentificationFeaturesCache());
            identificationFeaturesCache.setReadOnly(false);
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            waitingHandler.setRunFinished();
            return;
        }

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
    public void saveProject(WaitingHandler waitingHandler, boolean emptyCache) throws IOException, SQLException, ArchiveException, ClassNotFoundException, InterruptedException {
        CpsExporter.saveAs(cpsFile, waitingHandler, experiment, identification, shotgunProtocol, identificationParameters,
                spectrumCountingPreferences, projectDetails, filterPreferences, metrics, geneMaps,
                identificationFeaturesGenerator.getIdentificationFeaturesCache(),
                objectsCache, emptyCache, displayPreferences, dbFolder);

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
     *
     * @return a boolean indicating whether the loading was successful
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     */
    public boolean loadFastaFile(File folder, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException {

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
     *
     * @return a boolean indicating whether the loading was successful
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     */
    public boolean loadSpectrumFiles(WaitingHandler waitingHandler) throws IOException {
        return loadSpectrumFiles(null, waitingHandler);
    }

    /**
     * Loads the spectra in the spectrum factory.
     *
     * @param folder a folder to look into, the user last selected folder for
     * instance, can be null
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     *
     * @return a boolean indicating whether the loading was successful
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     */
    public boolean loadSpectrumFiles(File folder, WaitingHandler waitingHandler) throws IOException {

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
     * @param mgfFiles the list to add the detected mgf files to
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     *
     * @return a boolean indicating whether the loading was successful
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     */
    public boolean loadSpectrumFile(String spectrumFileName, ArrayList<File> mgfFiles, WaitingHandler waitingHandler) throws IOException {

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
        mgfFiles.add(mgfFile);

        return true;
    }

    /**
     * Returns the objects database used for this project.
     *
     * @return the objects database used for this project
     */
    public ObjectsDB getObjectsDB() {
        return identification.getIdentificationDB().getObjectsDB(); //@TODO: avoid using the identification object.
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
     * Returns the metrics object.
     *
     * @return the metrics object
     */
    public Metrics getMetrics() {
        return metrics;
    }
    
    /**
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        return geneMaps;
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
     * Sets the identification feature generator.
     *
     * @param identificationFeaturesGenerator the identification feature
     * generator
     */
    public void setIdentificationFeaturesGenerator(IdentificationFeaturesGenerator identificationFeaturesGenerator) {
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
    }

    /**
     * Sets the spectrum counting preferences.
     *
     * @param spectrumCountingPreferences the spectrum counting preferences
     */
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        if (identificationFeaturesGenerator != null) {
            identificationFeaturesGenerator.setSpectrumCountingPreferences(spectrumCountingPreferences);
        }
    }

    /**
     * Sets the project details.
     *
     * @param projectDetails the project details
     */
    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
    }

    /**
     * Sets the metrics.
     *
     * @param metrics the metrics
     */
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Sets the gene maps.
     *
     * @param geneMaps the gene maps
     */
    public void setGeneMaps(GeneMaps geneMaps) {
        this.geneMaps = geneMaps;
    }

    /**
     * Sets the objects cache.
     *
     * @param objectsCache the objects cache
     */
    public void setObjectsCache(ObjectsCache objectsCache) {
        this.objectsCache = objectsCache;
    }

    /**
     * Sets the filter preferences.
     *
     * @param filterPreferences the filter preferences
     */
    public void setFilterPreferences(FilterPreferences filterPreferences) {
        this.filterPreferences = filterPreferences;
    }

    /**
     * Sets the display preferences.
     *
     * @param displayPreferences the display preferences
     */
    public void setDisplayPreferences(DisplayPreferences displayPreferences) {
        this.displayPreferences = displayPreferences;
    }

    /**
     * Sets the cps file.
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
     * Sets the identification object.
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
     * Sets the default preferences.
     */
    public void setDefaultPreferences() {
        SearchParameters searchParameters = new SearchParameters();
        identificationParameters = new IdentificationParameters(searchParameters);
        spectrumCountingPreferences = new SpectrumCountingPreferences();
        spectrumCountingPreferences.setSelectedMethod(SpectralCountingMethod.NSAF);
        spectrumCountingPreferences.setMatchValidationLevel(MatchValidationLevel.doubtful.getIndex());
    }

    /**
     * Resets the feature generator.
     */
    public void resetIdentificationFeaturesGenerator() {
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, identificationParameters, metrics, spectrumCountingPreferences);
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
     * Returns the folder where the database is stored.
     *
     * @return the folder where the database is stored
     */
    public File getDbFolder() {
        return dbFolder;
    }

    /**
     * Sets the folder where the database is stored.
     *
     * @param dbFolder the folder where the database is stored
     */
    public void setDbFolder(File dbFolder) {
        this.dbFolder = dbFolder;
    }

    /**
     * Returns an extended HTML project report.
     *
     * @param waitingHandlerReport the progress report, if null the report from
     * the project details will be used
     * @return an extended HTML project report
     */
    public String getExtendedProjectReport(String waitingHandlerReport) {

        String report;

        if (projectDetails != null && getIdentification() != null) {

            report = "<html><br>";
            report += "<b>Experiment</b>: " + experiment.getReference() + "<br>";
            report += "<b>Sample:</b> " + sample.getReference() + "<br>";
            report += "<b>Replicate number:</b> " + replicateNumber + "<br><br>";

            if (projectDetails.getCreationDate() != null) {
                report += "<b>Creation Date:</b> " + projectDetails.getCreationDate() + "<br><br>";
            }

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
