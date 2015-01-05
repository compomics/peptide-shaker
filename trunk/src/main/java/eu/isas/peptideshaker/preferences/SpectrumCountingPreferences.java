package eu.isas.peptideshaker.preferences;

import com.compomics.util.experiment.units.MetricsPrefix;
import com.compomics.util.experiment.units.StandardUnit;
import com.compomics.util.experiment.units.UnitOfMeasurement;
import java.io.Serializable;

/**
 * This class contains the spectrum counting preferences.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumCountingPreferences implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -8925515082376046312L;
    /**
     * The reference total mass to use for normalization in μg.
     */
    private Double referenceMass = 2.0;
    /**
     * The unit to use for normalization
     */
    private UnitOfMeasurement unit = new UnitOfMeasurement(StandardUnit.mol, MetricsPrefix.femto);
    /**
     * Indicates whether the spectrum counting index should be normalized.
     */
    private Boolean normalize = true;

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
     * Creates new preferences based on other spectrum counting preferences.
     * 
     * @param otherSpectrumCountingPreferences the other spectrum counting preferences
     */
    public SpectrumCountingPreferences(SpectrumCountingPreferences otherSpectrumCountingPreferences) {
        this.selectedMethod = otherSpectrumCountingPreferences.getSelectedMethod();
        this.validatedHits = otherSpectrumCountingPreferences.isValidatedHits();
        this.normalize = otherSpectrumCountingPreferences.getNormalize();
        this.referenceMass = otherSpectrumCountingPreferences.getReferenceMass();
        this.unit = otherSpectrumCountingPreferences.getUnit();
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
     * @param selectedMethod the spectral counting method
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
     * @param validatedHits if the only validated hits are to be counted
     */
    public void setValidatedHits(boolean validatedHits) {
        this.validatedHits = validatedHits;
    }

    /**
     * Compares two spectrum counting preferences.
     *
     * @param anotherSpectrumCountingPreferences another spectrum counting
     * preferences
     * @return a boolean indicating whether the other spectrum counting
     * preferences is the same as this one
     */
    public boolean isSameAs(SpectrumCountingPreferences anotherSpectrumCountingPreferences) {
        return anotherSpectrumCountingPreferences.getSelectedMethod() == selectedMethod
                && anotherSpectrumCountingPreferences.isValidatedHits() == validatedHits;
    }

    /**
     * Returns the reference total mass to use for normalization.
     * 
     * @return the reference total mass to use for normalization in μg
     */
    public Double getReferenceMass() {
        if (referenceMass == null) { // Backward compatibility
            referenceMass = 2.0;
        }
        return referenceMass;
    }

    /**
     * Sets the reference total mass to use for normalization.
     * 
     * @param referenceMass the reference total mass to use for normalization in μg
     */
    public void setReferenceMass(Double referenceMass) {
        this.referenceMass = referenceMass;
    }

    /**
     * Returns the unit used for normalization.
     * 
     * @return the unit used for normalization
     */
    public UnitOfMeasurement getUnit() {
        if (unit == null) { // Backward compatibility
            unit = new UnitOfMeasurement(StandardUnit.mol, MetricsPrefix.femto);
        }
        return unit;
    }

    /**
     * Sets the unit used for normalization.
     * 
     * @param unit the unit used for normalization
     */
    public void setUnit(UnitOfMeasurement unit) {
        this.unit = unit;
    }

    /**
     * Indicates whether the spectrum counting index should be normalized.
     * 
     * @return true if the spectrum counting index should be normalized
     */
    public Boolean getNormalize() {
        if (normalize == null) { // Backward compatibility
            normalize = false;
        }
        return normalize;
    }

    /**
     * Sets whether the spectrum counting index should be normalized.
     * 
     * @param normalize a boolean indicating whether the spectrum counting index should be normalized
     */
    public void setNormalize(Boolean normalize) {
        this.normalize = normalize;
    }
    
    
    
}
