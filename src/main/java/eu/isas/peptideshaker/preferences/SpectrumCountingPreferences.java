/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class contains the spectrum counting preferences
 *
 * @author marc
 */
public class SpectrumCountingPreferences implements Serializable {
    
    public static final int NSAF = 0;
    
    public static final int EMPAI = 1;
    
    private int selectedMethod;
    
    private boolean validatedHits;
    
    public SpectrumCountingPreferences() {
        
    }

    public int getSelectedMethod() {
        return selectedMethod;
    }

    public void setSelectedMethod(int selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    public boolean isValidatedHits() {
        return validatedHits;
    }

    public void setValidatedHits(boolean validatedHits) {
        this.validatedHits = validatedHits;
    }
}
