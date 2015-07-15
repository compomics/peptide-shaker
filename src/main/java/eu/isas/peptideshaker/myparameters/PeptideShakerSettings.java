package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProteinInferencePreferences;
import com.compomics.util.preferences.PsmScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import eu.isas.peptideshaker.preferences.*;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;

/**
 * This class will be used to save all settings needed in PeptideShaker.
 *
 * @author Marc Vaudel
 */
public class PeptideShakerSettings implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -3531908843597367812L;
    /**
     * The initial processing preferences.
     */
    private ProcessingPreferences processingPreferences;
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
     * The identification features generator with features in cache.
     */
    private IdentificationFeaturesCache identificationFeaturesCache;
    /**
     * Information about the protocol used.
     */
    private ShotgunProtocol shotgunProtocol;

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
     * @param processingPreferences The processing preferences
     * @param identificationFeaturesCache The identification features cache
     */
    public PeptideShakerSettings(ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            SpectrumCountingPreferences spectrumCountingPreferences,
            ProjectDetails projectDetails,
            FilterPreferences filterPreferences,
            DisplayPreferences displayPreferences,
            Metrics metrics,
            ProcessingPreferences processingPreferences,
            IdentificationFeaturesCache identificationFeaturesCache) {
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.projectDetails = projectDetails;
        this.filterPreferences = filterPreferences;
        this.displayPreferences = displayPreferences;
        this.metrics = metrics;
        this.processingPreferences = processingPreferences;
        this.identificationFeaturesCache = identificationFeaturesCache;
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
     * Sets the identification parameters.
     *
     * @param identificationParameters the identification parameters
     */
    public void setIdentificationParameters(IdentificationParameters identificationParameters) {
        this.identificationParameters = identificationParameters;
    }

    /**
     * Returns the spectrum counting preferences of the project.
     *
     * @return the spectrum counting preferences of the project
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
     * Returns the GUI display preferences.
     *
     * @return the GUI display preferences
     */
    public FilterPreferences getFilterPreferences() {
        return filterPreferences;
    }

    /**
     * Returns the GUI display preferences.
     *
     * @return the GUI display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        return displayPreferences;
    }

    /**
     * Returns information about the protocol used.
     * 
     * @return information about the protocol used
     */
    public ShotgunProtocol getShotgunProtocol() {
        return shotgunProtocol;
    }

    /**
     * Sets information about the protocol used.
     * 
     * @param shotgunProtocol information about the protocol used
     */
    public void setShotgunProtocol(ShotgunProtocol shotgunProtocol) {
        this.shotgunProtocol = shotgunProtocol;
    }

    /**
     * Returns the initial processing preferences.
     *
     * @return the initial processing preferences
     */
    public ProcessingPreferences getProcessingPreferences() {
        if (processingPreferences == null) {
            processingPreferences = new ProcessingPreferences();
        }
        return processingPreferences;
    }

    /**
     * Sets the initial processing preferences.
     *
     * @param processingPreferences the initial processing preferences
     */
    public void setProcessingPreferences(ProcessingPreferences processingPreferences) {
        this.processingPreferences = processingPreferences;
    }

    /**
     * Returns the metrics saved when loading the files.
     *
     * @return the metrics saved when loading the files
     */
    public Metrics getMetrics() {
        if (metrics == null) {
            metrics = new Metrics();
        }
        return metrics;
    }

    /**
     * Returns the identification features cache used by the identification
     * features generator before saving the file. Null for versions older than
     * 0.18.0.
     *
     * @return the identification features cache
     */
    public IdentificationFeaturesCache getIdentificationFeaturesCache() {
        return identificationFeaturesCache;
    }

    @Override
    public String getFamilyName() {
        return "PeptideShaker";
    }

    @Override
    public int getIndex() {
        return 2;
    }
}
