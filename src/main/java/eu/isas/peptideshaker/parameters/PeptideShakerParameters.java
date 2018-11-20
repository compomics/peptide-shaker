package eu.isas.peptideshaker.parameters;

import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import com.compomics.util.gui.filtering.FilterParameters;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesCache;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.parameters.peptide_shaker.ProjectType;

/**
 * This class stores parameters for a PeptideShaker project.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShakerParameters extends DbObject implements UrParameter {

    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters ;
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
     * The sequence provider.
     */
    private SequenceProvider sequenceProvider;
    /**
     * The protein details provider.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The gene maps.
     */
    protected GeneMaps geneMaps;
    /**
     * The identification features generator with features in cache.
     */
    private IdentificationFeaturesCache identificationFeaturesCache;
    /**
     * The type of project.
     */
    private ProjectType projectType;
    /**
     * The key of the object when stored in settings table of a cps file.
     */
    public static final long key = ExperimentObject.asLong("PeptideShaker_parameters");

    /**
     * Empty default constructor.
     */
    public PeptideShakerParameters () {
        
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
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param geneMaps The gene maps
     * @param projectType the project type
     * @param identificationFeaturesCache The identification features cache
     */
    public PeptideShakerParameters(IdentificationParameters identificationParameters,
            SpectrumCountingParameters spectrumCountingPreferences,
            ProjectDetails projectDetails,
            FilterParameters filterPreferences,
            DisplayParameters displayPreferences,
            Metrics metrics,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            GeneMaps geneMaps,
            ProjectType projectType,
            IdentificationFeaturesCache identificationFeaturesCache) {
        
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.projectDetails = projectDetails;
        this.filterParameters = filterPreferences;
        this.displayParameters = displayPreferences;
        this.metrics = metrics;
        this.sequenceProvider = sequenceProvider;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.geneMaps = geneMaps;
        this.projectType = projectType;
        this.identificationFeaturesCache = identificationFeaturesCache;
    
    }

    /**
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public IdentificationParameters getIdentificationParameters() {
        
        readDBMode();
        
        return identificationParameters;
    
    }

    /**
     * Returns the spectrum counting preferences of the project.
     *
     * @return the spectrum counting preferences of the project
     */
    public SpectrumCountingParameters getSpectrumCountingPreferences() {
    
        readDBMode();
        
        return spectrumCountingPreferences;
    
    }
    
    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        
        readDBMode();
        
        return projectDetails;
        
    }

    /**
     * Returns the GUI display parameters.
     *
     * @return the GUI display parameters
     */
    public FilterParameters getFilterParameters() {
        
        readDBMode();
        
        return filterParameters;
    
    }

    /**
     * Returns the GUI display parameters.
     *
     * @return the GUI display parameters
     */
    public DisplayParameters getDisplayParameters() {
        
        readDBMode();
        
        return displayParameters;
    
    }

    /**
     * Returns the metrics saved when loading the files.
     *
     * @return the metrics saved when loading the files
     */
    public Metrics getMetrics() {
        
        readDBMode();
        
        return metrics;
    
    }

    /**
     * Returns the sequence provider saved when loading the files.
     *
     * @return the sequence provider saved when loading the files
     */
    public SequenceProvider getSequenceProvider() {
        
        readDBMode();
        
        return sequenceProvider;
    
    }

    /**
     * Returns the protein details provider.
     * 
     * @return the protein details provider
     */
    public ProteinDetailsProvider getProteinDetailsProvider() {
        
        readDBMode();
        
        return proteinDetailsProvider;
        
    }
    
    /**
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        
        readDBMode();
        
        return geneMaps;
    
    }

    /**
     * Returns the identification features cache used by the identification
     * features generator before saving the file.
     *
     * @return the identification features cache
     */
    public IdentificationFeaturesCache getIdentificationFeaturesCache() {
        
        readDBMode();
        
        return identificationFeaturesCache;
    
    }

    @Override
    public long getParameterKey() {
        return getId();
    }

    /**
     * Returns the project type.
     * 
     * @return the project type
     */
    public ProjectType getProjectType() {
        
        readDBMode();
        
        return projectType;
    }
    
    
}
