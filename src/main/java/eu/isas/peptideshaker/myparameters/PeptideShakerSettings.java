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
     * The PTM scoring preferences.
     *
     * @deprecated use identificationParameters instead
     */
    private PTMScoringPreferences ptmScoringPreferences;
    /**
     * The gene preferences.
     *
     * @deprecated use identificationParameters instead
     */
    private GenePreferences genePreferences;
    /**
     * The sequence matching preferences.
     *
     * @deprecated use identificationParameters instead
     */
    private SequenceMatchingPreferences sequenceMatchingPreferences;
    /**
     * The identification filters.
     *
     * @deprecated use identificationParameters instead
     */
    private IdFilter idFilter;
    /**
     * The utilities annotation preferences.
     *
     * @deprecated use identificationParameters instead
     */
    private AnnotationPreferences utilitiesAnnotationPreferences = null;
    /**
     * The search parameters (versions older than 0.19) still present for
     * backward compatibility.
     *
     * @deprecated use utilitiesSearchParamters
     */
    private eu.isas.peptideshaker.preferences.SearchParameters searchParameters;
    /**
     * The parameters linked to the search.
     *
     * @deprecated use
     */
    private SearchParameters utiltiesSearchParameters;

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
        if (identificationParameters == null) {

            identificationParameters = new IdentificationParameters();

            SearchParameters searchParameters = getSearchParameters();
            identificationParameters.setSearchParameters(searchParameters);
            identificationParameters.setAnnotationPreferences(getAnnotationPreferences());
            identificationParameters.setIdFilter(getIdFilter());
            identificationParameters.setIdValidationPreferences(new IdMatchValidationPreferences());
            ProteinInferencePreferences proteinInferencePreferences = new ProteinInferencePreferences();
            proteinInferencePreferences.setProteinSequenceDatabase(searchParameters.getFastaFile());
            identificationParameters.setProteinInferencePreferences(proteinInferencePreferences);
            identificationParameters.setPsmScoringPreferences(new PsmScoringPreferences());
            identificationParameters.setPtmScoringPreferences(getPTMScoringPreferences());

            GenePreferences genePreferences = getGenePreferences();
            // backwards compatability for the gene preferences
            if (genePreferences.getCurrentSpecies() == null) {
                genePreferences = new GenePreferences();
            }
            if (genePreferences.getCurrentSpecies() != null && genePreferences.getCurrentSpeciesType() == null) {
                genePreferences.setCurrentSpeciesType("Vertebrates");
            }
            identificationParameters.setGenePreferences(genePreferences);

            // backwards compatability for the sequence matching preferences
            SequenceMatchingPreferences sequenceMatchingPreferences = getSequenceMatchingPreferences();
            if (sequenceMatchingPreferences == null) {
                sequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching(searchParameters);
            }
            identificationParameters.setSequenceMatchingPreferences(sequenceMatchingPreferences);
        }
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
     * Returns the annotation preferences.
     *
     * @deprecated use identificationParameters instead
     *
     * @return the annotation preferences
     */
    public AnnotationPreferences getAnnotationPreferences() {
        if (utilitiesAnnotationPreferences == null) {
            // most likely a compatibility issue, reset the annotation preferences
            utilitiesAnnotationPreferences = new AnnotationPreferences();
        }
        return utilitiesAnnotationPreferences;
    }

    /**
     * Returns the parameters linked to the search.
     *
     * @deprecated use identificationParameters instead
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
     * Returns the gene preferences.
     *
     * @deprecated use identificationParameters instead
     *
     * @return the gene preferences
     */
    public GenePreferences getGenePreferences() {
        if (genePreferences == null) {
            genePreferences = new GenePreferences();
        }
        return genePreferences;
    }

    /**
     * Set the gene preferences.
     *
     * @deprecated use identificationParameters instead
     *
     * @param genePreferences
     */
    public void setGenePreferences(GenePreferences genePreferences) {
        this.genePreferences = genePreferences;
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
     * @deprecated use identificationParameters instead
     *
     * @return the PTM scoring preferences
     */
    public PTMScoringPreferences getPTMScoringPreferences() {
        if (ptmScoringPreferences == null) {
            // backward compatibility check
            ptmScoringPreferences = new PTMScoringPreferences();
            ptmScoringPreferences.setFlrThreshold(1);
            ptmScoringPreferences.setProbabilisticScoreNeutralLosses(true);
            try {
                ptmScoringPreferences.setProbabilitsticScoreCalculation(processingPreferences.isAScoreCalculated());
            } catch (Exception e) {
                ptmScoringPreferences.setProbabilitsticScoreCalculation(true);
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

    /**
     * Returns the ID filters.
     *
     * @deprecated use identificationParameters instead
     *
     * @return the idFilter
     */
    public IdFilter getIdFilter() {
        return idFilter;
    }

    /**
     * Sets the ID filter.
     *
     * @deprecated use identificationParameters instead
     *
     * @param idFilter the idFilter to set
     */
    public void setIdFilter(IdFilter idFilter) {
        this.idFilter = idFilter;
    }

    /**
     * Returns the sequence matching preferences.
     *
     * @deprecated use identificationParameters instead
     *
     * @return the sequence matching preferences
     */
    public SequenceMatchingPreferences getSequenceMatchingPreferences() {
        return sequenceMatchingPreferences;
    }

    /**
     * Sets the sequence matching preferences.
     *
     * @deprecated use identificationParameters instead
     *
     * @param sequenceMatchingPreferences the sequence matching preferences
     */
    public void setSequenceMatchingPreferences(SequenceMatchingPreferences sequenceMatchingPreferences) {
        this.sequenceMatchingPreferences = sequenceMatchingPreferences;
    }
}
