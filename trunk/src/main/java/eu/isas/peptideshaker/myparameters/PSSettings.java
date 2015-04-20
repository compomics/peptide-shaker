package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.*;
import eu.isas.peptideshaker.utils.IdentificationFeaturesCache;
import eu.isas.peptideshaker.utils.Metrics;

/**
 * This class will be used to save all settings needed in PeptideShaker.
 *
 * @author Marc Vaudel
 * @deprecated use eu.isas.peptideshaker.myparameter.PeptideShakerSettings
 * instead
 */
public class PSSettings implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -3531908843597367812L;
    /**
     * The search parameters (versions older than 0.19) still present for
     * backward compatibility.
     *
     * @deprecated use utilitiesSearchParamters
     */
    private eu.isas.peptideshaker.preferences.SearchParameters searchParameters;
    /**
     * The parameters linked to the search.
     */
    private SearchParameters utiltiesSearchParameters;
    /**
     * The initial processing preferences.
     */
    private ProcessingPreferences processingPreferences;
    /**
     * The utilities annotation preferences.
     */
    private AnnotationPreferences utilitiesAnnotationPreferences = null;
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
     * The PTM scoring preferences.
     */
    private PTMScoringPreferences ptmScoringPreferences;

    /**
     * Blank constructor.
     */
    public PSSettings() {
    }

    /**
     * Constructor for a PeptideShaker Settings class.
     *
     * @param searchParameters The parameters linked to the search
     * @param annotationPreferences The annotation preferences
     * @param spectrumCountingPreferences The spectrum counting preferences
     * @param projectDetails The project details
     * @param filterPreferences The filter preferences
     * @param displayPreferences The display preferences
     * @param metrics The metrics saved when loading the files
     * @param processingPreferences The processing preferences
     * @param identificationFeaturesCache The identification features cache
     * @param ptmScoringPreferences The PTM scoring preferences
     */
    public PSSettings(SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences,
            SpectrumCountingPreferences spectrumCountingPreferences,
            ProjectDetails projectDetails,
            FilterPreferences filterPreferences,
            DisplayPreferences displayPreferences,
            Metrics metrics,
            ProcessingPreferences processingPreferences,
            IdentificationFeaturesCache identificationFeaturesCache,
            PTMScoringPreferences ptmScoringPreferences) {
        this.utiltiesSearchParameters = searchParameters;
        this.utilitiesAnnotationPreferences = annotationPreferences;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.projectDetails = projectDetails;
        this.filterPreferences = filterPreferences;
        this.displayPreferences = displayPreferences;
        this.metrics = metrics;
        this.processingPreferences = processingPreferences;
        this.identificationFeaturesCache = identificationFeaturesCache;
        this.ptmScoringPreferences = ptmScoringPreferences;
    }

    /**
     * Returns the annotation preferences.
     *
     * @return the annotation preferences
     */
    public AnnotationPreferences getAnnotationPreferences() {
        if (utilitiesAnnotationPreferences == null) {
            // most likely a compatibility issue, reset the annotation preferences
            utilitiesAnnotationPreferences = new AnnotationPreferences();
            utilitiesAnnotationPreferences.addNeutralLoss(NeutralLoss.H2O);
            utilitiesAnnotationPreferences.addNeutralLoss(NeutralLoss.NH3);
        }
        return utilitiesAnnotationPreferences;
    }

    /**
     * Returns the parameters linked to the search.
     *
     * @return the parameters linked to the search
     */
    public SearchParameters getSearchParameters() {
        if (utiltiesSearchParameters == null) {
            utiltiesSearchParameters = searchParameters.getUpdatedVersion();
        }
        return utiltiesSearchParameters;
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
     * Returns the gui display preferences.
     *
     * @return the gui display preferences
     */
    public FilterPreferences getFilterPreferences() {
        return filterPreferences;
    }

    /**
     * Returns the gui display preferences.
     *
     * @return the gui display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        return displayPreferences;
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

    /**
     * Returns the PTM scoring preferences.
     *
     * @return the PTM scoring preferences
     */
    public PTMScoringPreferences getPTMScoringPreferences() {
        if (ptmScoringPreferences == null) {
            // backward compatibility check
            ptmScoringPreferences = new PTMScoringPreferences();
            ptmScoringPreferences.setFlrThreshold(1);
            ptmScoringPreferences.setaScoreNeutralLosses(true);
            try {
                ptmScoringPreferences.setaScoreCalculation(processingPreferences.isAScoreCalculated());
            } catch (Exception e) {
                ptmScoringPreferences.setaScoreCalculation(true);
            }
        }
        return ptmScoringPreferences;
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
