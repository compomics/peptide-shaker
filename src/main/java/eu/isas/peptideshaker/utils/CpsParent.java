package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesCache;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.Util;
import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationKeys;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.spectrumcounting.SpectrumCountingMethod;
import com.compomics.util.gui.file_handling.TempFilesManager;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.export.CpsExporter;
import eu.isas.peptideshaker.parameters.PeptideShakerParameters;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import com.compomics.util.gui.filtering.FilterParameters;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import eu.isas.peptideshaker.preferences.UserParameters;
import eu.isas.peptideshaker.preferences.UserPreferencesParent;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.parameters.peptide_shaker.ProjectType;
import eu.isas.peptideshaker.scoring.PSMaps;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

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
     * The identification parameters.
     */
    protected IdentificationParameters identificationParameters;
    /**
     * The spectrum counting preferences.
     */
    protected SpectrumCountingParameters spectrumCountingParameters;
    /**
     * The project details.
     */
    protected ProjectDetails projectDetails;
    /**
     * The metrics stored during processing.
     */
    protected Metrics metrics;
    /**
     * The sequence provider.
     */
    protected SequenceProvider sequenceProvider;
    /**
     * The protein details provider.
     */
    protected ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The gene maps.
     */
    protected GeneMaps geneMaps;
    /**
     * The filter parameters.
     */
    protected FilterParameters filterParameters = new FilterParameters();
    /**
     * The display parameters.
     */
    protected DisplayParameters displayParameters = new DisplayParameters();
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
    public static final String psParametersTableName = "PeptideShaker_experiment_parameters";
    /**
     * All parameters of a project
     */
    public ProjectParameters projectParameters;
    /**
     * The project type.
     */
    protected ProjectType projectType;

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
     * @param dbFolder the folder where to extract the project
     * @param waitingHandler a waiting handler displaying feedback to the user.
     * Ignored if null
     *
     * @throws IOException thrown if an error occurred while reading the file
     */
    public void loadCpsFromZipFile(File zipFile, File dbFolder, WaitingHandler waitingHandler) throws IOException {

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

                if (file.getName().toLowerCase().endsWith(".psdb")) {

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
     */
    public void loadCpsFile(File dbFolder, WaitingHandler waitingHandler) throws IOException {

        // close any open connection to an identification database
        if (identification != null) {

            identification.close();

        }
        
        // create the matches folder if it does not exist
        if (!dbFolder.exists()) {
            if (!dbFolder.mkdirs()) {
                throw new IOException("Impossible to create folder " + dbFolder.getAbsolutePath() + ".");
            }
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String dbName = "tempDB-" + df.format(new Date()) + ".psdb";

        File destinationFile = new File(dbFolder.getAbsolutePath(), dbName);

        Util.copyFile(cpsFile, destinationFile);

        ObjectsDB objectsDB = new ObjectsDB(dbFolder.getAbsolutePath(), destinationFile.getName(), false);
        PeptideShakerParameters psParameters = (PeptideShakerParameters) objectsDB.retrieveObject(PeptideShakerParameters.key);

        projectParameters = (ProjectParameters) objectsDB.retrieveObject(ProjectParameters.key);
        identification = new Identification(objectsDB);

        // load identification attributes
        IdentificationKeys identificationKeys = (IdentificationKeys) objectsDB.retrieveObject(IdentificationKeys.key);
        identification.setIdentificationKeys(identificationKeys);

        PSMaps psMaps = new PSMaps();
        psMaps = (PSMaps) objectsDB.retrieveObject(psMaps.getParameterKey());
        identification.addUrParam(psMaps);

        // get PeptideShaker settings
        identificationParameters = psParameters.getIdentificationParameters();
        spectrumCountingParameters = psParameters.getSpectrumCountingPreferences();
        projectDetails = psParameters.getProjectDetails();
        metrics = psParameters.getMetrics();
        geneMaps = psParameters.getGeneMaps();
        filterParameters = psParameters.getFilterParameters();
        displayParameters = psParameters.getDisplayParameters();
        sequenceProvider = psParameters.getSequenceProvider();
        proteinDetailsProvider = psParameters.getProteinDetailsProvider();
        projectType = psParameters.getProjectType();

        // set up caches
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, identificationParameters, sequenceProvider, metrics, spectrumCountingParameters);
        IdentificationFeaturesCache identificationFeaturesCache = psParameters.getIdentificationFeaturesCache();

        if (identificationFeaturesCache != null) {

            identificationFeaturesGenerator.setIdentificationFeaturesCache(psParameters.getIdentificationFeaturesCache());
            identificationFeaturesCache.setReadOnly(false);

        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {

            waitingHandler.setRunFinished();
            return;

        }

        loadUserParameters();
        userPreferences.addRecentProject(cpsFile);
        saveUserParameters();

    }

    /**
     * Saves the project in the cps file.
     *
     * @param waitingHandler waiting handler displaying feedback to the user.
     * can be null.
     * @param emptyCache if true the cache will be emptied
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while writing the file
     */
    public void saveProject(WaitingHandler waitingHandler, boolean emptyCache) throws IOException {

        CpsExporter.saveAs(cpsFile, waitingHandler, identification, identificationParameters, sequenceProvider, proteinDetailsProvider,
                spectrumCountingParameters, projectDetails, filterParameters, metrics, geneMaps, projectType,
                identificationFeaturesGenerator.getIdentificationFeaturesCache(), emptyCache, displayParameters, dbFolder);

        loadUserParameters();
        userPreferences.addRecentProject(cpsFile);
        saveUserParameters();

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
     * whenever an error occurred loading the spectrum files
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

        for (String spectrumFileName : projectDetails.getSpectrumFileNames()) {

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
     * Loads the FASTA file.
     *
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     * @return the FASTA summary
     *
     * @throws IOException exception thrown if an error occurred while reading
     * or writing the file
     */
    public FastaSummary loadFastaFile(WaitingHandler waitingHandler) throws IOException {

        return FastaSummary.getSummary(getProjectDetails().getFastaFile(), identificationParameters.getFastaParameters(), waitingHandler);

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
    public SpectrumCountingParameters getSpectrumCountingParameters() {

        return spectrumCountingParameters;

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
     * Returns the filter preferences.
     *
     * @return the filter preferences
     */
    public FilterParameters getFilterParameters() {

        return filterParameters;

    }

    /**
     * Returns the display preferences.
     *
     * @return the display preferences
     */
    public DisplayParameters getDisplayParameters() {

        return displayParameters;

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
    public void setSpectrumCountingParameters(SpectrumCountingParameters spectrumCountingPreferences) {

        this.spectrumCountingParameters = spectrumCountingPreferences;

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
     * Returns the project parameters.
     *
     * @return the project parameters
     */
    public ProjectParameters getProjectParameters() {

        return projectParameters;

    }

    /**
     * Sets the project parameters.
     *
     * @param projectParameters the project parameters
     */
    public void setProject(ProjectParameters projectParameters) {

        this.projectParameters = projectParameters;

    }

    /**
     * Sets the filter preferences.
     *
     * @param filterPreferences the filter preferences
     */
    public void setFilterParameters(FilterParameters filterPreferences) {

        this.filterParameters = filterPreferences;

    }

    /**
     * Sets the display preferences.
     *
     * @param displayPreferences the display preferences
     */
    public void setDisplayParameters(DisplayParameters displayPreferences) {

        this.displayParameters = displayPreferences;

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
    public UserParameters getUserParameters() {

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
     * Sets the default preferences.
     */
    public void setDefaultParameters() {

        SearchParameters searchParameters = new SearchParameters();
        identificationParameters = new IdentificationParameters(searchParameters);
        spectrumCountingParameters = new SpectrumCountingParameters();
        spectrumCountingParameters.setSelectedMethod(SpectrumCountingMethod.NSAF);
        spectrumCountingParameters.setMatchValidationLevel(MatchValidationLevel.doubtful.getIndex());

    }

    /**
     * Resets the feature generator.
     */
    public void resetIdentificationFeaturesGenerator() {

        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, identificationParameters, sequenceProvider, metrics, spectrumCountingParameters);

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
     * Returns the sequence provider.
     *
     * @return the sequence provider
     */
    public SequenceProvider getSequenceProvider() {

        return sequenceProvider;

    }

    /**
     * Sets the sequence provider.
     *
     * @param sequenceProvider the sequence provider
     */
    public void setSequenceProvider(SequenceProvider sequenceProvider) {

        this.sequenceProvider = sequenceProvider;

    }

    /**
     * Returns the protein details provider.
     *
     * @return the protein details provider
     */
    public ProteinDetailsProvider getProteinDetailsProvider() {

        return proteinDetailsProvider;

    }

    /**
     * Sets the protein details provider.
     *
     * @param proteinDetailsProvider the protein details provider
     */
    public void setProteinDetailsProvider(ProteinDetailsProvider proteinDetailsProvider) {

        this.proteinDetailsProvider = proteinDetailsProvider;

    }

    /**
     * Returns the project type.
     *
     * @return the project type
     */
    public ProjectType getProjectType() {
        return projectType;
    }

    /**
     * Sets the project type.
     *
     * @param projectType the project type
     */
    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    /**
     * Returns an extended HTML project report.
     *
     * @param waitingHandlerReport the progress report, if null the report from
     * the project details will be used
     *
     * @return an extended HTML project report
     */
    public String getExtendedProjectReport(String waitingHandlerReport) {

        StringBuilder report = new StringBuilder();

        if (projectDetails != null && getIdentification() != null) {

            report.append("<html><br>");
            report.append("<b>Experiment</b>: ").append(projectParameters.getProjectUniqueName()).append("<br>");

            if (projectDetails.getCreationDate() != null) {

                report.append("<b>Creation Date:</b> ").append(projectDetails.getCreationDate()).append("<br><br>");

            }

            report.append("<b>Identification Files</b>:<br>");

            for (String idFilePath : projectDetails.getIdentificationFiles()) {

                File idFile = new File(idFilePath);

                report.append(idFile.getAbsolutePath()).append(" - ");
                HashMap<String, ArrayList<String>> versions = projectDetails.getIdentificationAlgorithmsForFile(idFile.getName());
                ArrayList<String> software = new ArrayList<>(versions.keySet());
                Collections.sort(software);
                boolean first = true;

                for (String softwareName : software) {

                    if (first) {

                        first = false;

                    } else {

                        report.append(", ");

                    }

                    report.append(softwareName);
                    ArrayList<String> algorithmVersions = versions.get(softwareName);

                    if (algorithmVersions != null && !algorithmVersions.isEmpty()) {

                        report.append(" - (");
                        boolean firstVersion = true;

                        for (String version : algorithmVersions) {

                            if (firstVersion) {

                                firstVersion = false;

                            } else {

                                report.append(", ");

                            }
                            if (version != null) {

                                report.append(version);

                            } else {

                                report.append("unknown version");

                            }
                        }

                        report.append(")");

                    }
                }

                report.append("<br>");
            }

            report.append("<br><b>Spectrum Files:</b><br>");

            for (String mgfFileNames : projectDetails.getSpectrumFileNames()) {

                report.append(projectDetails.getSpectrumFile(mgfFileNames).getAbsolutePath()).append("<br>");

            }

            report.append("<br><b>FASTA File:</b><br>");
            report.append(projectDetails.getFastaFile()).append("<br>");

            report.append("<br><br><b>Report:</b><br>");

            if (waitingHandlerReport == null) {

                waitingHandlerReport = projectDetails.getReport();

            }

            if (waitingHandlerReport.lastIndexOf("<br>") == -1) {

                report.append("<pre>").append(waitingHandlerReport).append("</pre>");

            } else {

                report.append(waitingHandlerReport);

            }

            report.append("</html>");

        } else {

            report.append("<html><br>");

            report.append("<b>Report:</b><br>");
            if (waitingHandlerReport != null) {
                if (waitingHandlerReport.lastIndexOf("<br>") == -1) {
                    report.append("<pre>").append(waitingHandlerReport).append("</pre>");
                } else {
                    report.append(waitingHandlerReport);
                }
            }

            report.append("</html>");

        }

        return report.toString();

    }
}
