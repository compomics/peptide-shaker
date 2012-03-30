package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class contains the spectrum counting preferences
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumCountingPreferences implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = -8925515082376046312L;

    /**
     * The spectrum counting methods.
     */
    public enum SpectralCountingMethod {

        NSAF, EMPAI
    };
    /**
     * The currently selected spectrum counting method.
     */
    private SpectralCountingMethod selectedMethod;
    /**
     * If true, only validated hits are counted.
     */
    private boolean validatedHits;

    /**
     * Default constructor.
     */
    public SpectrumCountingPreferences() {
        // Set default preferences
        selectedMethod = SpectralCountingMethod.NSAF;
        validatedHits = true;
    }

    /**
     * Returns the current spectrum counting method.
     * 
     * @return the current spectrum counting method
     */
    public SpectralCountingMethod getSelectedMethod() {
        return selectedMethod;
    }

    /**
     * Set the current spectrum counting method.
     * 
     * @param selectedMethod 
     */
    public void setSelectedMethod(SpectralCountingMethod selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    /**
     * Returns true if only validated hits are to be counted.
     * 
     * @return true if only validated hits are to be counted
     */
    public boolean isValidatedHits() {
        return validatedHits;
    }

    /**
     * Set if only validated hits are to be counted.
     * 
     * @param validatedHits 
     */
    public void setValidatedHits(boolean validatedHits) {
        this.validatedHits = validatedHits;
    }
    
    /**
     * Compares two spectrum counting preferences
     * @param anotherSpectrumCountingPreferences another spectrum counting preferences
     * @return a boolean indicating whether the other spectrum counting preferences is the same as this one
     */
    public boolean isSameAs(SpectrumCountingPreferences anotherSpectrumCountingPreferences) {
        return anotherSpectrumCountingPreferences.getSelectedMethod() == selectedMethod
                && anotherSpectrumCountingPreferences.isValidatedHits() == validatedHits;
    }
}
