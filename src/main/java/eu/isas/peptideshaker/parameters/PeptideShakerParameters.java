package eu.isas.peptideshaker.parameters;

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
public class PeptideShakerParameters extends ExperimentObject implements UrParameter {

    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The spectrum counting preferences.
     */
    private final SpectrumCountingParameters spectrumCountingPreferences;
    /**
     * The GUI filter preferences.
     */
    private final FilterParameters filterParameters;
    /**
     * The display preferences.
     */
    private final DisplayParameters displayParameters;
    /**
     * The project details.
     */
    private final ProjectDetails projectDetails;
    /**
     * The metrics saved when loading the files.
     */
    private final Metrics metrics;
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
    private final GeneMaps geneMaps;
    /**
     * The identification features generator with features in cache.
     */
    private final IdentificationFeaturesCache identificationFeaturesCache;
    /**
     * The type of project.
     */
    private final ProjectType projectType;
    /**
     * The key of the object when stored in settings table of a psdb file.
     */
    public static final long KEY = ExperimentObject.asLong("PeptideShaker_parameters");
    /**
     * The path for the FM index.
     */
    private String fmIndexPath;

    /**
     * Empty default constructor.
     */
    public PeptideShakerParameters() {
        identificationParameters = null;
        spectrumCountingPreferences = null;
        projectDetails = null;
        filterParameters = null;
        displayParameters = null;
        metrics = null;
        sequenceProvider = null;
        proteinDetailsProvider = null;
        geneMaps = null;
        identificationFeaturesCache = null;
        projectType = null;
        fmIndexPath = "";
    }

    /**
     * Constructor for a PeptideShaker Settings class.
     *
     * @param identificationParameters the parameters used for identification
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     * @param filterPreferences the filter preferences
     * @param displayPreferences the display preferences
     * @param metrics the metrics saved when loading the files
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param geneMaps the gene maps
     * @param projectType the project type
     * @param identificationFeaturesCache the identification features cache
     */
    public PeptideShakerParameters(
            IdentificationParameters identificationParameters,
            SpectrumCountingParameters spectrumCountingPreferences,
            ProjectDetails projectDetails,
            FilterParameters filterPreferences,
            DisplayParameters displayPreferences,
            Metrics metrics,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            GeneMaps geneMaps,
            ProjectType projectType,
            IdentificationFeaturesCache identificationFeaturesCache
    ) {

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

        return identificationParameters;

    }

    /**
     * Returns the spectrum counting preferences of the project.
     *
     * @return the spectrum counting preferences of the project
     */
    public SpectrumCountingParameters getSpectrumCountingPreferences() {

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
     * Returns the GUI display parameters.
     *
     * @return the GUI display parameters
     */
    public FilterParameters getFilterParameters() {

        return filterParameters;

    }

    /**
     * Returns the GUI display parameters.
     *
     * @return the GUI display parameters
     */
    public DisplayParameters getDisplayParameters() {

        return displayParameters;

    }

    /**
     * Returns the metrics saved when loading the files.
     *
     * @return the metrics saved when loading the files
     */
    public Metrics getMetrics() {

        return metrics;

    }

    /**
     * Returns the sequence provider saved when loading the files.
     *
     * @return the sequence provider saved when loading the files
     */
    public SequenceProvider getSequenceProvider() {

        return sequenceProvider;

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
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {

        return geneMaps;

    }

    /**
     * Returns the identification features cache used by the identification
     * features generator before saving the file.
     *
     * @return the identification features cache
     */
    public IdentificationFeaturesCache getIdentificationFeaturesCache() {

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

        return projectType;
    }

    /**
     * Cleans the SequenceProvider and ProteinDetailsProvider, please use only
     * temporary or if you know what you do
     */
    public void cleanProviders() {
        sequenceProvider = null;
        proteinDetailsProvider = null;
    }

    /**
     * Setter for the sequence provider
     *
     * @param sequenceProvider the sequence provider instance
     */
    public void setSequenceProvider(SequenceProvider sequenceProvider) {
        this.sequenceProvider = sequenceProvider;
    }

    /**
     * Setter for the protein details provider
     *
     * @param proteinDetailsProvider the protein details provider instance
     */
    public void setProteinDetailsProvider(ProteinDetailsProvider proteinDetailsProvider) {
        this.proteinDetailsProvider = proteinDetailsProvider;
    }

}
