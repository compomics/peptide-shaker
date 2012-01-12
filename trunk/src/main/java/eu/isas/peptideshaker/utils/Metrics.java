package eu.isas.peptideshaker.utils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class contains metrics from the dataset for later use.
 *
 * @author Marc Vaudel
 */
public class Metrics implements Serializable {

    /**
     * Serial number for versions compatibility.
     */
    static final long serialVersionUID = 5905881057533649517L;
    /**
     * The maximal precursor error in Da in all PSMs (only the best hit per spectrum).
     */
    private double maxPrecursorErrorDa = 0;
    /**
     * The maximal precursor error in ppm in all PSMs (only the best hit per spectrum).
     */
    private double maxPrecursorErrorPpm = 0;
    /**
     * The chares found in all PSMs (only the best hit per spectrum).
     */
    private ArrayList<Integer> foundCharges = new ArrayList<Integer>();

    /**
     * Constructor.
     */
    public Metrics() {
    }

    /**
     * Returns the found charges.
     * 
     * @return the found charges.
     */
    public ArrayList<Integer> getFoundCharges() {
        if (foundCharges.isEmpty()) {
            // code for backward compatibility, quite uggly I agree...
            foundCharges.add(2);
            foundCharges.add(3);
            foundCharges.add(4);
        }
        return foundCharges;
    }

    /**
     * Set the list of charges found.
     * 
     * @param foundCharges the charges to set
     */
    public void setFoundCharges(ArrayList<Integer> foundCharges) {
        this.foundCharges = foundCharges;
    }

    /**
     * Return the max precursor mass error in Dalton.
     * 
     * @return the max precursor mass error in Dalton
     */
    public double getMaxPrecursorErrorDa() {
        return maxPrecursorErrorDa;
    }

    /**
     * Set the max precursor mass error in Dalton.
     * 
     * @param maxPrecursorErrorDa the mass error to set 
     */
    public void setMaxPrecursorErrorDa(double maxPrecursorErrorDa) {
        this.maxPrecursorErrorDa = maxPrecursorErrorDa;
    }

    /**
     * Returns the max precursor mass error in ppm.
     * 
     * @return the max precursor mass error in ppm
     */
    public double getMaxPrecursorErrorPpm() {
        return maxPrecursorErrorPpm;
    }

    /**
     * Set the max precursor mass error in ppm.
     * 
     * @param maxPrecursorErrorPpm the max precursor mass error in ppm
     */
    public void setMaxPrecursorErrorPpm(double maxPrecursorErrorPpm) {
        this.maxPrecursorErrorPpm = maxPrecursorErrorPpm;
    }
}
