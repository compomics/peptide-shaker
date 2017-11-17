package eu.isas.peptideshaker.parameters;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import eu.isas.peptideshaker.preferences.FilterParameters;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingParameters;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.Serializable;

/**
 * This class will be used to save all settings needed in PeptideShaker.
 *
 * @author Marc Vaudel
 */
public class PeptideShakerParameters extends DbObject implements UrParameter, Serializable {

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
    private SpectrumCountingParameters spectrumCountingPreferences;
    /**
     * The GUI filter preferences.
     */
    private FilterParameters filterParameters;
    /**
     * The display preferences.
     */
    private DisplayParameters displayParameters;
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
     * The key of the object when stored in settings table of a cps file.
     */
    public static final long key = ExperimentObject.asLong("PeptideShaker");

    /**
     * Blank constructor.
     */
    public PeptideShakerParameters() {
    }

    /**
     * Constructor for a PeptideShaker Settings class.
     *
     * @param identificationParameters the parameters used for identification
     * @param spectrumCountingPreferences The spectrum counting preferences
     * @param projectDetails The project details
     * @param filterPreferences The filter preferences
     * @param displayPreferences The display preferences
     * @param metrics The metrics saved when loading the files
     * @param geneMaps The gene maps
     * @param identificationFeaturesCache The identification features cache
     */
    public PeptideShakerParameters(IdentificationParameters identificationParameters,
            SpectrumCountingParameters spectrumCountingPreferences,
            ProjectDetails projectDetails,
            FilterParameters filterPreferences,
            DisplayParameters displayPreferences,
            Metrics metrics,
            GeneMaps geneMaps,
            IdentificationFeaturesCache identificationFeaturesCache) {
        
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.projectDetails = projectDetails;
        this.filterParameters = filterPreferences;
        this.displayParameters = displayPreferences;
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
    public SpectrumCountingParameters getSpectrumCountingPreferences() {
    
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        
        return spectrumCountingPreferences;
    
    }
    
    public void setSpectrumCountingPreferences(SpectrumCountingParameters spectrumCountingPreferences){
    
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
     * Returns the GUI display parameters.
     *
     * @return the GUI display parameters
     */
    public FilterParameters getFilterParameters() {
        
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        
        return filterParameters;
    
    }
    
    /**
     * Sets the filter parameters.
     * 
     * @param filterParameters the filter parameters
     */
    public void setFilterParameters(FilterParameters filterParameters){
        
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        
        this.filterParameters = filterParameters;
    
    }

    /**
     * Returns the GUI display parameters.
     *
     * @return the GUI display parameters
     */
    public DisplayParameters getDisplayParameters() {
        
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        
        return displayParameters;
    
    }
    
    /**
     * Sets the display parameters.
     * 
     * @param displayParameters 
     */
    public void setDisplayParameters(DisplayParameters displayParameters){
        
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        
        this.displayParameters = displayParameters;
    
    }

    /**
     * Returns the metrics saved when loading the files.
     *
     * @return the metrics saved when loading the files
     */
    public Metrics getMetrics() {
        
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        
        return metrics;
    
    }
    
    /**
     * Sets the metrics saved when loading the files.
     * 
     * @param metrics the metrics saved when loading the files
     */
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
        
        return geneMaps;
    
    }
    
    /**
     * Sets the gene maps.
     * 
     * @param geneMaps the gene maps
     */
    public void setGeneMaps(GeneMaps geneMaps){
        
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        
        this.geneMaps = geneMaps;
    
    }

    /**
     * Returns the identification features cache used by the identification
     * features generator before saving the file.
     *
     * @return the identification features cache
     */
    public IdentificationFeaturesCache getIdentificationFeaturesCache() {
        
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        
        return identificationFeaturesCache;
    
    }
    
    /**
     * Sets the identification features cache used by the identification
     * features generator before saving the file.
     * 
     * @param identificationFeaturesCache the identification features cache used by the identification
     * features generator before saving the file
     */
    public void setIdentificationFeaturesCache(IdentificationFeaturesCache identificationFeaturesCache){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.identificationFeaturesCache = identificationFeaturesCache;
    }

    @Override
    public long getParameterKey() {
        return getId();
    }
}
