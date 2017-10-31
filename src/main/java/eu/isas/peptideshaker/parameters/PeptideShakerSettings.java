package eu.isas.peptideshaker.parameters;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.Serializable;

/**
 * This class will be used to save all settings needed in PeptideShaker.
 *
 * @author Marc Vaudel
 */
public class PeptideShakerSettings extends DbObject implements UrParameter, Serializable {

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
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return identificationParameters;
    }

    /**
     * Sets the identification parameters.
     *
     * @param identificationParameters the identification parameters
     */
    public void setIdentificationParameters(IdentificationParameters identificationParameters) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.identificationParameters = identificationParameters;
    }

    /**
     * Returns the spectrum counting preferences of the project.
     *
     * @return the spectrum counting preferences of the project
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return spectrumCountingPreferences;
    }
    
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }
    
    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return projectDetails;
    }
    
    public void setProjectDetails(ProjectDetails projectDetails){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.projectDetails = projectDetails;
    }

    /**
     * Returns the GUI display preferences.
     *
     * @return the GUI display preferences
     */
    public FilterPreferences getFilterPreferences() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return filterPreferences;
    }
    
    public void setFilterPreferences(FilterPreferences filterPreferences){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.filterPreferences = filterPreferences;
    }

    /**
     * Returns the GUI display preferences.
     *
     * @return the GUI display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return displayPreferences;
    }
    
    public void setDisplayPreferences(DisplayPreferences displayPreferences){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.displayPreferences = displayPreferences;
    }

    /**
     * Returns information about the protocol used.
     *
     * @return information about the protocol used
     */
    public ShotgunProtocol getShotgunProtocol() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return shotgunProtocol;
    }

    /**
     * Sets information about the protocol used.
     *
     * @param shotgunProtocol information about the protocol used
     */
    public void setShotgunProtocol(ShotgunProtocol shotgunProtocol) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.shotgunProtocol = shotgunProtocol;
    }

    /**
     * Returns the metrics saved when loading the files.
     *
     * @return the metrics saved when loading the files
     */
    public Metrics getMetrics() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (metrics == null) {
            metrics = new Metrics();
        }
        return metrics;
    }
    
    public void setMetrics(Metrics metrics){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.metrics = metrics;
    }
    

    /**
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (geneMaps == null) {
            geneMaps = new GeneMaps();
        }
        return geneMaps;
    }
    
    public void setGeneMaps(GeneMaps geneMaps){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
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
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return identificationFeaturesCache;
    }
    
    public void setIdentificationFeaturesCache(IdentificationFeaturesCache identificationFeaturesCache){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.identificationFeaturesCache = identificationFeaturesCache;
    }

    @Override
    public long getParameterKey() {
        return getId();
    }
}
