package eu.isas.peptideshaker.parameters;

import com.compomics.util.IdObject;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.preferences.PSProcessingPreferences;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;

/**
 * This class will be used to save all settings needed in PeptideShaker.
 *
 * @author Marc Vaudel
 */
public class PeptideShakerSettings extends IdObject implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -3531908843597367812L;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * The spectrum counting preferences.
     */
    private SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * The GUI filter preferences.
     */
    private FilterPreferences filterPreferences;
    /**
     * The display preferences.
     */
    private DisplayPreferences displayPreferences;
    /**
     * The project details.
     */
    private ProjectDetails projectDetails;
    /**
     * The metrics saved when loading the files.
     */
    private Metrics metrics;
    /**
     * The gene maps.
     */
    protected GeneMaps geneMaps;
    /**
     * The identification features generator with features in cache.
     */
    private IdentificationFeaturesCache identificationFeaturesCache;
    /**
     * Information about the protocol used.
     */
    private ShotgunProtocol shotgunProtocol;
    /**
     * The name of the object when stored in settings table of a cps file.
     */
    public static final String nameInCpsSettingsTable = "PeptideShaker";

    /**
     * Blank constructor.
     */
    public PeptideShakerSettings() {
    }

    /**
     * Constructor for a PeptideShaker Settings class.
     *
     * @param shotgunProtocol information about the protocol used
     * @param identificationParameters the parameters used for identification
     * @param spectrumCountingPreferences The spectrum counting preferences
     * @param projectDetails The project details
     * @param filterPreferences The filter preferences
     * @param displayPreferences The display preferences
     * @param metrics The metrics saved when loading the files
     * @param geneMaps The gene maps
     * @param identificationFeaturesCache The identification features cache
     */
    public PeptideShakerSettings(ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            SpectrumCountingPreferences spectrumCountingPreferences,
            ProjectDetails projectDetails,
            FilterPreferences filterPreferences,
            DisplayPreferences displayPreferences,
            Metrics metrics,
            GeneMaps geneMaps,
            IdentificationFeaturesCache identificationFeaturesCache) {
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.projectDetails = projectDetails;
        this.filterPreferences = filterPreferences;
        this.displayPreferences = displayPreferences;
        this.geneMaps = geneMaps;
        this.metrics = metrics;
        this.identificationFeaturesCache = identificationFeaturesCache;
    }

    /**
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public IdentificationParameters getIdentificationParameters() {
        zooActivateRead();
        return identificationParameters;
    }

    /**
     * Sets the identification parameters.
     *
     * @param identificationParameters the identification parameters
     */
    public void setIdentificationParameters(IdentificationParameters identificationParameters) {
        zooActivateWrite();
        setModified(true);
        this.identificationParameters = identificationParameters;
    }

    /**
     * Returns the spectrum counting preferences of the project.
     *
     * @return the spectrum counting preferences of the project
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        zooActivateRead();
        return spectrumCountingPreferences;
    }
    
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences){
        zooActivateWrite();
        setModified(true);
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }
    
    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        zooActivateRead();
        return projectDetails;
    }
    
    public void setProjectDetails(ProjectDetails projectDetails){
        zooActivateWrite();
        setModified(true);
        this.projectDetails = projectDetails;
    }

    /**
     * Returns the GUI display preferences.
     *
     * @return the GUI display preferences
     */
    public FilterPreferences getFilterPreferences() {
        zooActivateRead();
        return filterPreferences;
    }
    
    public void setFilterPreferences(FilterPreferences filterPreferences){
        zooActivateWrite();
        setModified(true);
        this.filterPreferences = filterPreferences;
    }

    /**
     * Returns the GUI display preferences.
     *
     * @return the GUI display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        zooActivateRead();
        return displayPreferences;
    }
    
    public void setDisplayPreferences(DisplayPreferences displayPreferences){
        zooActivateWrite();
        setModified(true);
        this.displayPreferences = displayPreferences;
    }

    /**
     * Returns information about the protocol used.
     *
     * @return information about the protocol used
     */
    public ShotgunProtocol getShotgunProtocol() {
        zooActivateRead();
        return shotgunProtocol;
    }

    /**
     * Sets information about the protocol used.
     *
     * @param shotgunProtocol information about the protocol used
     */
    public void setShotgunProtocol(ShotgunProtocol shotgunProtocol) {
        zooActivateWrite();
        setModified(true);
        this.shotgunProtocol = shotgunProtocol;
    }

    /**
     * Returns the metrics saved when loading the files.
     *
     * @return the metrics saved when loading the files
     */
    public Metrics getMetrics() {
        zooActivateRead();
        if (metrics == null) {
            metrics = new Metrics();
        }
        return metrics;
    }
    
    public void setMetrics(Metrics metrics){
        zooActivateWrite();
        setModified(true);
        this.metrics = metrics;
    }
    

    /**
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        zooActivateRead();
        if (geneMaps == null) {
            geneMaps = new GeneMaps();
        }
        return geneMaps;
    }
    
    public void setGeneMaps(GeneMaps geneMaps){
        zooActivateWrite();
        setModified(true);
        this.geneMaps = geneMaps;
    }

    /**
     * Returns the identification features cache used by the identification
     * features generator before saving the file. Null for versions older than
     * 0.18.0.
     *
     * @return the identification features cache
     */
    public IdentificationFeaturesCache getIdentificationFeaturesCache() {
        zooActivateRead();
        return identificationFeaturesCache;
    }
    
    public void setIdentificationFeaturesCache(IdentificationFeaturesCache identificationFeaturesCache){
        zooActivateWrite();
        setModified(true);
        this.identificationFeaturesCache = identificationFeaturesCache;
    }

    @Override
    public String getParameterKey() {
        return "PeptideShaker|2";
    }
}
