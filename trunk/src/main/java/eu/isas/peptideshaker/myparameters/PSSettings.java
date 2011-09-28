package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SearchParameters;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;

/**
 * This class will be used to save all settings needed in PeptideShaker
 *
 * @author vaudel
 */
public class PSSettings implements UrParameter {

    /**
     * serial version UID for post-serialization compatibility
     */
        static final long serialVersionUID = -3531908843597367812L;
    /**
     * The parameters linked to the search
     */
    private SearchParameters searchParameters;
    /**
     * The annotation preferences
     */
    private AnnotationPreferences annotationPreferences;
    /**
     * The spectrum counting preferences
     */
    private SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * The project details
     */
    private ProjectDetails projectDetails;
    
    /**
     * Blank constructor
     */
    public PSSettings() {
        
    }
    
    /**
     * Constructor for a Peptide Shaker Settings class
     * @param searchParameters              The parameters linked to the search
     * @param annotationPreferences         The annotation preferences
     * @param identificationPreferences     The identification preferences
     * @param spectrumCountingPreferences   The spectrum counting preferences
     * @param projectDetails                The project details
     */
    public PSSettings(SearchParameters searchParameters, 
            AnnotationPreferences annotationPreferences, 
            SpectrumCountingPreferences spectrumCountingPreferences,
            ProjectDetails projectDetails) {
        this.searchParameters = searchParameters;
        this.annotationPreferences = annotationPreferences;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.projectDetails = projectDetails;
    }

    /**
     * Returns the annotation preferences
     * @return the annotation preferences 
     */
    public AnnotationPreferences getAnnotationPreferences() {
        return annotationPreferences;
    }

    /**
     * Returns the parameters linked to the search
     * @return the parameters linked to the search 
     */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }
    
    /**
     * Returns the spectrum counting preferences of the project
     * @return the spectrum counting preferences of the project
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return spectrumCountingPreferences;
    }
    
    /**
     * Returns the project details
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        return projectDetails;
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
