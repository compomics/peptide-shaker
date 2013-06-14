/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.utils;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
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
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Implementing this abstract class allows interacting with a cps files
 *
 * @author Marc
 */
public abstract class CpsParent extends UserPreferencesParent {

    /**
     * The identification.
     */
    protected Identification identification;
    /**
     * The identification features generator.
     */
    protected IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The identification filter used.
     */
    protected IdFilter idFilter;
    /**
     * The annotation preferences to use.
     */
    protected AnnotationPreferences annotationPreferences;
    /**
     * The spectrum counting preferences.
     */
    protected SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * The PTM scoring preferences.
     */
    protected PTMScoringPreferences ptmScoringPreferences;
    /**
     * The project details.
     */
    protected ProjectDetails projectDetails;
    /**
     * The search parameters.
     */
    protected SearchParameters searchParameters;
    /**
     * The processing preferences.
     */
    protected ProcessingPreferences processingPreferences;
    /**
     * The metrics stored during processing.
     */
    protected Metrics metrics;
    /**
     * The gene preferences.
     */
    protected GenePreferences genePreferences = new GenePreferences();
    /**
     * The MS experiment class
     */
    protected MsExperiment experiment;
    /**
     * The sample
     */
    protected Sample sample;
    /**
     * The replicate number
     */
    protected int replicateNumber;
    /**
     * The proteomic analysis
     */
    protected ProteomicAnalysis proteomicAnalysis;
    /**
     * The cache used to store objects
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
     * The currently loaded cps file
     */
    protected File cpsFile = null;

    /**
     * Loads the information from a cps file
     *
     * @param cpsFile the cps file
     * @param waitingHandler a waiting handler displaying feedback to the user.
     * Ignored if null
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public void loadCpsFile(WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException, SQLException {

        CpsFileImporter cpsFileImporter = new CpsFileImporter(cpsFile, waitingHandler);

        // closeFiles any open connection to an identification database
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
                waitingHandler.appendReport(replicates.size() + " replicates found in sample " + sample.getReference() + " of experiment " + experiment.getReference() + ", replicate " + sample.getReference() + " selected by default.", true, true);
            }
        }
        proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);

        // Get PeptideShaker settings
        PeptideShakerSettings experimentSettings = cpsFileImporter.getExperimentSettings();
        idFilter = experimentSettings.getIdFilter();
        annotationPreferences = experimentSettings.getAnnotationPreferences();
        spectrumCountingPreferences = experimentSettings.getSpectrumCountingPreferences();
        ptmScoringPreferences = experimentSettings.getPTMScoringPreferences();
        projectDetails = experimentSettings.getProjectDetails();
        searchParameters = experimentSettings.getSearchParameters();
        processingPreferences = experimentSettings.getProcessingPreferences();
        metrics = experimentSettings.getMetrics();
        genePreferences = experimentSettings.getGenePreferences();
        if (experimentSettings.getFilterPreferences() != null) {
            filterPreferences = experimentSettings.getFilterPreferences();
        } else {
            filterPreferences = new FilterPreferences();
        }
        if (experimentSettings.getDisplayPreferences() != null) {
            displayPreferences = experimentSettings.getDisplayPreferences();
            displayPreferences.compatibilityCheck(searchParameters.getModificationProfile());
        } else {
            displayPreferences = new DisplayPreferences();
            displayPreferences.setDefaultSelection(searchParameters.getModificationProfile());
        }

        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            waitingHandler.setRunFinished();
            return;
        }

        // Get identification details and set up caches
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        if (identification.getSpectrumIdentificationMap() == null) {
            // 0.18 version, needs update of the spectrum mapping
            identification.updateSpectrumMapping();
        }
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, searchParameters, idFilter, metrics, spectrumCountingPreferences);
        if (experimentSettings.getIdentificationFeaturesCache() != null) {
            identificationFeaturesGenerator.setIdentificationFeaturesCache(experimentSettings.getIdentificationFeaturesCache());
        }
        objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);
        String dbFolder = new File(getJarFilePath(), PeptideShaker.SERIALIZATION_DIRECTORY).getAbsolutePath();
        identification.establishConnection(dbFolder, false, objectsCache);
        loadUserPreferences();
        userPreferences.addRecentProject(cpsFile);
        saveUserPreferences();

    }

    /**
     * Saves the project in the cps file
     *
     * @param waitingHandler waiting handler displaying feedback to the user.
     * can be null.
     *
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws ArchiveException
     */
    public void saveProject(WaitingHandler waitingHandler, boolean emptyCache) throws IOException, SQLException, FileNotFoundException, ArchiveException {
        CpsExporter.saveAs(cpsFile, waitingHandler, experiment, identification, searchParameters,
                annotationPreferences, spectrumCountingPreferences, projectDetails, metrics,
                processingPreferences, identificationFeaturesGenerator.getIdentificationFeaturesCache(),
                ptmScoringPreferences, genePreferences, objectsCache, emptyCache, idFilter);

        loadUserPreferences();
        userPreferences.addRecentProject(cpsFile);
        saveUserPreferences();
    }

    /**
     * Loads the fasta file in the sequence factory
     *
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadFastaFile(WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {
        return loadFastaFile(null, waitingHandler);
    }

    /**
     * Loads the fasta file in the sequence factory
     *
     * @param folder a folder to look into, the user last selected folder for
     * instance, can be null
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadFastaFile(File folder, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();

        // Load fasta file
        File providedFastaLocation = searchParameters.getFastaFile();
        String fileName = providedFastaLocation.getName();
        File projectFolder = cpsFile.getParentFile();
        File dataFolder = new File(projectFolder, "data");
        if (providedFastaLocation.exists()) {
            sequenceFactory.loadFastaFile(providedFastaLocation);
        } else if (folder != null && new File(folder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(projectFolder, fileName));
            searchParameters.setFastaFile(new File(projectFolder, fileName));
        } else if (new File(projectFolder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(projectFolder, fileName));
            searchParameters.setFastaFile(new File(projectFolder, fileName));
        } else if (new File(dataFolder, fileName).exists()) {
            sequenceFactory.loadFastaFile(new File(dataFolder, fileName));
            searchParameters.setFastaFile(new File(dataFolder, fileName));
        } else {
            return false;
        }
        return true;
    }

    /**
     * Loads the spectra in the spectrum factory
     *
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     *
     * @throws FileNotFoundException
     * @throws IOException
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadSpectrumFiles(WaitingHandler waitingHandler) throws FileNotFoundException, IOException {
        return loadSpectrumFiles(null, waitingHandler);
    }

    /**
     * Loads the spectra in the spectrum factory
     *
     * @param folder a folder to look into, the user last selected folder for
     * instance, can be null
     * @param waitingHandler a waiting handler displaying progress to the user.
     * Can be null
     *
     * @throws FileNotFoundException
     * @throws IOException
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
     * Imports the gene mapping.
     *
     * @TODO: we might want to split this method?
     *
     * @return a boolean indicating whether the loading was successful
     */
    public boolean loadGeneMappings(WaitingHandler waitingHandler) {

        boolean success = true;
        try {
            genePreferences.createDefaultGeneMappingFiles(
                    new File(getJarFilePath(), "resources/conf/gene_ontology/ensembl_versions"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/go_domains"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/species"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/hsapiens_gene_ensembl_go_mappings"),
                    new File(getJarFilePath(), "resources/conf/gene_ontology/hsapiens_gene_ensembl_gene_mappings"));
            genePreferences.loadSpeciesAndGoDomains();
        } catch (IOException e) {
            waitingHandler.appendReport("An error occurred while attempting to create the gene preferences.", true, true);
            e.printStackTrace();
            success = false;
        }


        if (genePreferences.getCurrentSpecies() != null && genePreferences.getSpeciesMap() != null && new File(genePreferences.getGeneMappingFolder(),
                genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GENE_MAPPING_FILE_SUFFIX).exists()) {
            try {
                GeneFactory geneFactory = GeneFactory.getInstance();
                geneFactory.initialize(new File(genePreferences.getGeneMappingFolder(),
                        genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GENE_MAPPING_FILE_SUFFIX), null);
            } catch (Exception e) {
                waitingHandler.appendReport("Unable to load the gene mapping file.", true, true);
                e.printStackTrace();
                success = false;
            }
        }

        if (genePreferences.getCurrentSpecies() != null && genePreferences.getSpeciesMap() != null && new File(genePreferences.getGeneMappingFolder(),
                genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GO_MAPPING_FILE_SUFFIX).exists()) {
            try {
                GOFactory goFactory = GOFactory.getInstance();
                goFactory.initialize(new File(genePreferences.getGeneMappingFolder(),
                        genePreferences.getSpeciesMap().get(genePreferences.getCurrentSpecies()) + genePreferences.GO_MAPPING_FILE_SUFFIX), null);
            } catch (Exception e) {
                waitingHandler.appendReport("Unable to load the gene ontology mapping file.", true, true);
                e.printStackTrace();
                success = false;
            }
        }

        return success;
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public abstract String getJarFilePath();

    public Identification getIdentification() {
        return identification;
    }

    public IdentificationFeaturesGenerator getIdentificationFeaturesGenerator() {
        return identificationFeaturesGenerator;
    }

    public IdFilter getIdFilter() {
        return idFilter;
    }

    public AnnotationPreferences getAnnotationPreferences() {
        return annotationPreferences;
    }

    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return spectrumCountingPreferences;
    }

    public PTMScoringPreferences getPtmScoringPreferences() {
        return ptmScoringPreferences;
    }

    public ProjectDetails getProjectDetails() {
        return projectDetails;
    }

    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    public ProcessingPreferences getProcessingPreferences() {
        return processingPreferences;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public GenePreferences getGenePreferences() {
        return genePreferences;
    }

    public MsExperiment getExperiment() {
        return experiment;
    }

    public Sample getSample() {
        return sample;
    }

    public int getReplicateNumber() {
        return replicateNumber;
    }

    public ProteomicAnalysis getProteomicAnalysis() {
        return proteomicAnalysis;
    }

    public ObjectsCache getObjectsCache() {
        return objectsCache;
    }

    public FilterPreferences getFilterPreferences() {
        return filterPreferences;
    }

    public DisplayPreferences getDisplayPreferences() {
        return displayPreferences;
    }

    public File getCpsFile() {
        return cpsFile;
    }

    public void setIdentificationFeaturesGenerator(IdentificationFeaturesGenerator identificationFeaturesGenerator) {
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
    }

    public void setIdFilter(IdFilter idFilter) {
        this.idFilter = idFilter;
    }

    public void setAnnotationPreferences(AnnotationPreferences annotationPreferences) {
        this.annotationPreferences = annotationPreferences;
    }

    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }

    public void setPtmScoringPreferences(PTMScoringPreferences ptmScoringPreferences) {
        this.ptmScoringPreferences = ptmScoringPreferences;
    }

    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
    }

    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
    }

    public void setProcessingPreferences(ProcessingPreferences processingPreferences) {
        this.processingPreferences = processingPreferences;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public void setGenePreferences(GenePreferences genePreferences) {
        this.genePreferences = genePreferences;
    }

    public void setObjectsCache(ObjectsCache objectsCache) {
        this.objectsCache = objectsCache;
    }

    public void setFilterPreferences(FilterPreferences filterPreferences) {
        this.filterPreferences = filterPreferences;
    }

    public void setDisplayPreferences(DisplayPreferences displayPreferences) {
        this.displayPreferences = displayPreferences;
    }

    public void setCpsFile(File cpsFile) {
        this.cpsFile = cpsFile;
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public void setIdentification(Identification identification) {
        this.identification = identification;
    }

    /**
     * Sets the project
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
     * Resets the preferences
     */
    public void clearPreferences() {
        annotationPreferences = new AnnotationPreferences();
        spectrumCountingPreferences = new SpectrumCountingPreferences();
        ptmScoringPreferences = new PTMScoringPreferences();
        filterPreferences = new FilterPreferences();
        displayPreferences = new DisplayPreferences();
        searchParameters = new SearchParameters();
        processingPreferences = new ProcessingPreferences();
        genePreferences = new GenePreferences();
        idFilter = new IdFilter();
    }

    /**
     * Set the default preferences.
     */
    public void setDefaultPreferences() {
        searchParameters = new SearchParameters();
        annotationPreferences = new AnnotationPreferences();
        annotationPreferences.setAnnotationLevel(0.75);
        annotationPreferences.useAutomaticAnnotation(true);
        spectrumCountingPreferences = new SpectrumCountingPreferences();
        spectrumCountingPreferences.setSelectedMethod(SpectralCountingMethod.NSAF);
        spectrumCountingPreferences.setValidatedHits(true);
        processingPreferences = new ProcessingPreferences();
        ptmScoringPreferences = new PTMScoringPreferences();
        genePreferences = new GenePreferences();
        idFilter = new IdFilter();
    }

    /**
     * Resets the feature generator.
     */
    public void resetFeatureGenerator() {
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, searchParameters, idFilter, metrics, spectrumCountingPreferences);
    }
}
