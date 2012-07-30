package eu.isas.peptideshaker.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class contains metrics from the dataset for later use.
 *
 * @TODO for the next release without backward compatibility we could rename it
 * into something more descriptive like DatasetMetrics
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Metrics implements Serializable {

    /**
     * Serial number for versions compatibility.
     */
    static final long serialVersionUID = 5905881057533649517L; // @TODO: have to be updated??
    /**
     * The maximal precursor error in Da in all PSMs (only the best hit per
     * spectrum).
     */
    private double maxPrecursorErrorDa = 0;
    /**
     * The maximal precursor error in ppm in all PSMs (only the best hit per
     * spectrum).
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
     * The ordered list of protein keys as estimated during the import process
     */
    private ArrayList<String> proteinKeys;
    /**
     * The max protein key length.
     */
    private Integer maxProteinKeyLength = null;
    /**
     * Amount of validated proteins
     */
    private int nValidatedProteins = -1;
    /**
     * List of modifications found in the dataset
     */
    private ArrayList<String> foundModifications = null;
    /**
     * The psm matches for each fraction for each peptide. Key: 'fraction
     * name'_'peptide key'. Values: arraylist of spectrum keys.
     */
    private HashMap<String, ArrayList<String>> fractionPsmMatches;
    /**
     * The observed average molecular masses in kDa for each fraction.
     */
    private HashMap<String, Double> observedFractionalMasses; // @TODO: this one can be removed, but doing so results in backwards compatability issues...
    /**
     * The observed molecular masses in kDa for each fraction.
     */
    private HashMap<String, ArrayList<Double>> observedFractionalMassesAll;
    /**
     * The maximum number of peptides detected at the fraction level.
     */
    private Integer maxValidatedPeptidesPerFraction = null;
    /**
     * The maximum number of spectra at the fraction level.
     */
    private Integer maxValidatedSpectraPerFraction = null;
    /**
     * The maximum protein average precursor intensity.
     */
    private Double maxProteinAveragePrecursorIntensity = null;

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
     * Clears the found charges
     */
    public void clearFoundCharges() {
        foundCharges.clear();
    }

    /**
     * Adds a new charge to the list of found charges.
     *
     * @param foundCharges the new charge to add
     */
    public void addFoundCharges(ArrayList<Integer> foundCharges) {
        for (int newCharge : foundCharges) {
            if (!this.foundCharges.contains(newCharge)) {
                this.foundCharges.add(newCharge);
            }
        }
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
     * Returns the molecular weight of the fattest protein in the dataset.
     *
     * @return the molecular weight of the fattest protein in the dataset
     */
    public Double getMaxMW() {
        return maxMW;
    }

    /**
     * Sets the molecular weight of the fattest protein in the dataset.
     *
     * @param maxMW the molecular weight of the fattest protein in the dataset
     */
    public void setMaxMW(Double maxMW) {
        this.maxMW = maxMW;
    }

    /**
     * Returns the maximal amount of peptides in the proteins of the dataset.
     *
     * @return the maximal amount of peptides in the proteins of the dataset
     */
    public Integer getMaxNPeptides() {
        return maxNPeptides;
    }

    /**
     * Sets the maximal amount of peptides in the proteins of the dataset.
     *
     * @param maxNPeptides the maximal amount of peptides in the proteins of the
     * dataset
     */
    public void setMaxNPeptides(Integer maxNPeptides) {
        this.maxNPeptides = maxNPeptides;
    }

    /**
     * Returns the the maximal amount of psms in the proteins of the dataset.
     *
     * @return the the maximal amount of psms in the proteins of the dataset
     */
    public Integer getMaxNSpectra() {
        return maxNSpectra;
    }

    /**
     * Sets the the maximal amount of psms in the proteins of the dataset.
     *
     * @param maxNSpectra the the maximal amount of psms in the proteins of the
     * dataset
     */
    public void setMaxNSpectra(Integer maxNSpectra) {
        this.maxNSpectra = maxNSpectra;
    }

    /**
     * Returns the maximal spectrum counting value of the proteins of the
     * dataset.
     *
     * @return the maximal spectrum counting value of the proteins of the
     * dataset
     */
    public Double getMaxSpectrumCounting() {
        return maxSpectrumCounting;
    }

    /**
     * Sets the maximal spectrum counting value of the proteins of the dataset.
     *
     * @param maxSpectrumCounting the maximal spectrum counting value of the
     * proteins of the dataset
     */
    public void setMaxSpectrumCounting(Double maxSpectrumCounting) {
        this.maxSpectrumCounting = maxSpectrumCounting;
    }

    /**
     * Returns the list of ordered protein keys.
     *
     * @return the list of ordered protein keys
     */
    public ArrayList<String> getProteinKeys() {
        return proteinKeys;
    }

    /**
     * Sets the list of ordered protein keys.
     *
     * @param proteinKeys the list of ordered protein keys
     */
    public void setProteinKeys(ArrayList<String> proteinKeys) {
        this.proteinKeys = proteinKeys;
    }

    /**
     * Sets the max protein key length.
     *
     * @param maxProteinKeyLength the length to set
     */
    public void setMaxProteinKeyLength(Integer maxProteinKeyLength) {
        this.maxProteinKeyLength = maxProteinKeyLength;
    }

    /**
     * Returns the max protein key length.
     *
     * @return the max protein key length
     */
    public Integer getMaxProteinKeyLength() {
        if (maxProteinKeyLength != null) {
            return maxProteinKeyLength;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Returns the number of validated proteins.
     *
     * @return the number of validated proteins
     */
    public Integer getnValidatedProteins() {
        return nValidatedProteins;
    }

    /**
     * Sets the number of validated proteins.
     *
     * @param nValidatedProteins the number of validated proteins
     */
    public void setnValidatedProteins(int nValidatedProteins) {
        this.nValidatedProteins = nValidatedProteins;
    }

    /**
     * Returns the list of modifications found in the dataset.
     *
     * @return the list of modifications found in the dataset
     */
    public ArrayList<String> getFoundModifications() {
        return foundModifications;
    }

    /**
     * Sets the list of modifications found in the dataset.
     *
     * @param foundModifications the list of modifications found in the dataset
     */
    public void setFoundModifications(ArrayList<String> foundModifications) {
        this.foundModifications = foundModifications;
    }

    /**
     * Set the fraction psm matches. Key: 'fraction name'_'peptide key'. Values:
     * arraylist of spectrum keys.
     *
     * @param fractionPsmMatches the fraction psm matches
     */
    public void setFractionPsmMatches(HashMap<String, ArrayList<String>> fractionPsmMatches) {
        this.fractionPsmMatches = fractionPsmMatches;
    }

    /**
     * Returns the list of fraction psm matches. Key: 'fraction name'_'peptide
     * key'. Values: arraylist of spectrum keys.
     *
     * @return he list of fraction psm matches
     */
    public HashMap<String, ArrayList<String>> getFractionPsmMatches() {
        if (fractionPsmMatches != null) {
            return fractionPsmMatches;
        } else {
            return new HashMap<String, ArrayList<String>>();
        }
    }

    /**
     * Returns the observed average molecular masses in kDa for each fraction. The key 
     * is the file path of the fraction.
     * 
     * @return the observed average molecular masses for each fraction
     */
    public HashMap<String, Double> getObservedFractionalMasses() {
        if (observedFractionalMasses != null) {
            return observedFractionalMasses;
        } else {
            return new HashMap<String, Double>();
        }  
    }

    /**
     * Set the observed average molecular masses for each fraction in kDa. The key 
     * is the file path of the fraction.
     * 
     * @param observedFractionalMasses the observedFractionalMasses to set
     */
    public void setObservedFractionalMasses(HashMap<String, Double> observedFractionalMasses) {
        this.observedFractionalMasses = observedFractionalMasses;
    }
    
    /**
     * Returns the observed molecular masses in kDa for each fraction. The key 
     * is the file path of the fraction.
     * 
     * @return the observed average molecular masses for each fraction
     */
    public HashMap<String, ArrayList<Double>> getObservedFractionalMassesAll() {
        if (observedFractionalMassesAll != null) {
            return observedFractionalMassesAll;
        } else {
            return new HashMap<String, ArrayList<Double>>();
        }  
    }

    /**
     * Set the observed molecular masses for each fraction in kDa. The key 
     * is the file path of the fraction.
     * 
     * @param observedFractionalMassesAll the observedFractionalMasses to set
     */
    public void setObservedFractionalMassesAll(HashMap<String, ArrayList<Double>> observedFractionalMassesAll) {
        this.observedFractionalMassesAll = observedFractionalMassesAll;
    }

    /**
     * Returns the maximum validated peptides at the fraction level.
     * 
     * @return the maxValidatedPeptidesPerFraction
     */
    public Integer getMaxValidatedPeptidesPerFraction() {
        return maxValidatedPeptidesPerFraction;
    }

    /**
     * Set the maximum validated peptides at the fraction level.
     * 
     * @param maxValidatedPeptidesPerFraction the maxValidatedPeptidesPerFraction to set
     */
    public void setMaxValidatedPeptidesPerFraction(Integer maxValidatedPeptidesPerFraction) {
        this.maxValidatedPeptidesPerFraction = maxValidatedPeptidesPerFraction;
    }

    /**
     * Returns the maximum validated spectra at the fraction level.
     * 
     * @return the maxValidatedSpectraPerFraction
     */
    public Integer getMaxValidatedSpectraPerFraction() {
        return maxValidatedSpectraPerFraction;
    }

    /**
     * Set the maximum validated spectra at the fraction level.
     * 
     * @param maxValidatedSpectraPerFraction the maxValidatedSpectraPerFraction to set
     */
    public void setMaxValidatedSpectraPerFraction(Integer maxValidatedSpectraPerFraction) {
        this.maxValidatedSpectraPerFraction = maxValidatedSpectraPerFraction;
    }

    /**
     * Returns the maximum protein average precursor intensity.
     * 
     * @return the maxProteinAveragePrecursorIntensity
     */
    public Double getMaxProteinAveragePrecursorIntensity() {
        return maxProteinAveragePrecursorIntensity;
    }

    /**
     * Set the maximum protein average precursor intensity.
     * 
     * @param maxProteinAveragePrecursorIntensity the maxProteinAveragePrecursorIntensity to set
     */
    public void setMaxProteinAveragePrecursorIntensity(Double maxProteinAveragePrecursorIntensity) {
        this.maxProteinAveragePrecursorIntensity = maxProteinAveragePrecursorIntensity;
    }
}
