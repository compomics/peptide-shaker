package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.SearchParameters;

/**
 * This class will be used to save all settings needed in PeptideShaker
 *
 * @author vaudel
 */
public class PSSettings implements UrParameter {

    /**
     * The parameters linked to the search
     */
    private SearchParameters searchParameters;
    /**
     * The annotation preferences
     */
    private AnnotationPreferences annotationPreferences;
    
    /**
     * Blank constructor
     */
    public PSSettings() {
        
    }
    
    /**
     * Constructor for a Peptide Shaker Settings class
     * @param searchParameters          The parameters linked to the search
     * @param annotationPreferences     The annotation preferences
     * @param identificationPreferences The identification preferences
     */
    public PSSettings(SearchParameters searchParameters, AnnotationPreferences annotationPreferences) {
        this.searchParameters = searchParameters;
        this.annotationPreferences = annotationPreferences;
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

    @Override
    public String getFamilyName() {
        return "PeptideShaker";
    }

    @Override
    public int getIndex() {
        return 2;
    }
}
