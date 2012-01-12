/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.massspectrometry.Charge;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * this class contains metrics from the dataset for Harald
 *
 * @author Marc
 */
public class MetricsForHarald implements Serializable {
    
    /**
     * Serial number for versions compatibility
     */
    static final long serialVersionUID = 5905881057533649517L;
    /**
     * The maximal precursor error in Da in all PSMs (only the best hit per spectrum)
     */
    private double maxPrecursorErrorDa = 0;
    /**
     * The maximal precursor error in ppm in all PSMs (only the best hit per spectrum)
     */
    private double maxPrecursorErrorPpm = 0;
    /**
     * The chares found in all PSMs (only the best hit per spectrum)
     */
    private ArrayList<Integer> foundCharges = new ArrayList<Integer>();
    
    public MetricsForHarald() {
        
    }

    public ArrayList<Integer> getFoundCharges() {
        if (foundCharges.isEmpty()) {
            // code for backward compatibility, quite uggly I agree...
            foundCharges.add(2);
            foundCharges.add(3);
            foundCharges.add(4);
        }
        return foundCharges;
    }

    public void setFoundCharges(ArrayList<Integer> foundCharges) {
        this.foundCharges = foundCharges;
    }

    public double getMaxPrecursorErrorDa() {
        return maxPrecursorErrorDa;
    }

    public void setMaxPrecursorErrorDa(double maxPrecursorErrorDa) {
        this.maxPrecursorErrorDa = maxPrecursorErrorDa;
    }

    public double getMaxPrecursorErrorPpm() {
        return maxPrecursorErrorPpm;
    }

    public void setMaxPrecursorErrorPpm(double maxPrecursorErrorPpm) {
        this.maxPrecursorErrorPpm = maxPrecursorErrorPpm;
    }
}
