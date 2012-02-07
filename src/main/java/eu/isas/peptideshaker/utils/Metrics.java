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
     * The maximal amount of peptides in the proteins of the dataset
     */
    private Integer maxNPeptides = null;
    /**
     * The maximal amount of spectra in the proteins of the dataset
     */
    private Integer maxNSpectra = null;
    /**
     * The maximal spectrum counting in the proteins of the dataset
     */
    private Double maxSpectrumCounting = null;
    /**
     * The weight of the fattest protein in the dataset
     */
    private Double maxMW = null;
    /**
     * The ordered list of protein keys
     */
    private ArrayList<String> proteinKeys;

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

    /**
     * Returns the molecular weight of the fattest protein in the dataset
     * @return the molecular weight of the fattest protein in the dataset
     */
    public Double getMaxMW() {
        return maxMW;
    }

    /**
     * Sets the molecular weight of the fattest protein in the dataset
     * @param maxMW the molecular weight of the fattest protein in the dataset
     */
    public void setMaxMW(Double maxMW) {
        this.maxMW = maxMW;
    }

    /**
     * Returns the maximal amount of peptides in the proteins of the dataset
     * @return the maximal amount of peptides in the proteins of the dataset
     */
    public Integer getMaxNPeptides() {
        return maxNPeptides;
    }

    /**
     * Sets the maximal amount of peptides in the proteins of the dataset
     * @param maxNPeptides the maximal amount of peptides in the proteins of the dataset
     */
    public void setMaxNPeptides(Integer maxNPeptides) {
        this.maxNPeptides = maxNPeptides;
    }

    /**
     * Returns the the maximal amount of psms in the proteins of the dataset
     * @return the the maximal amount of psms in the proteins of the dataset
     */
    public Integer getMaxNSpectra() {
        return maxNSpectra;
    }

    /**
     * Sets the the maximal amount of psms in the proteins of the dataset
     * @param maxNSpectra the the maximal amount of psms in the proteins of the dataset
     */
    public void setMaxNSpectra(Integer maxNSpectra) {
        this.maxNSpectra = maxNSpectra;
    }

    /**
     * Returns the maximal spectrum counting value of the proteins of the dataset
     * @return the maximal spectrum counting value of the proteins of the dataset
     */
    public Double getMaxSpectrumCounting() {
        return maxSpectrumCounting;
    }

    /**
     * Sets the maximal spectrum counting value of the proteins of the dataset
     * @param maxSpectrumCounting the maximal spectrum counting value of the proteins of the dataset
     */
    public void setMaxSpectrumCounting(Double maxSpectrumCounting) {
        this.maxSpectrumCounting = maxSpectrumCounting;
    }

    /**
     * Returns the list of ordered protein keys
     * @return the list of ordered protein keys
     */
    public ArrayList<String> getProteinKeys() {
        return proteinKeys;
    }

    /**
     * Sets the list of ordered protein keys
     * @param proteinKeys the list of ordered protein keys
     */
    public void setProteinKeys(ArrayList<String> proteinKeys) {
        this.proteinKeys = proteinKeys;
    }
    
}
