package eu.isas.peptideshaker.utils;

import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * This class contains metrics from the dataset for later use.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Metrics implements Serializable {

    /**
     * Serial number for versions compatibility.
     */
    static final long serialVersionUID = 5905881057533649517L;
    /**
     * The maximal peptide precursor error in Da in all PSMs (only the best
     * peptide hit per spectrum).
     */
    private double maxPrecursorErrorDa = 0;
    /**
     * The maximal peptide precursor error in ppm in all PSMs (only the best
     * peptide hit per spectrum).
     */
    private double maxPrecursorErrorPpm = 0;
    /**
     * The maximal tag precursor error in Da in all PSMs (only the best tag hit
     * per spectrum when no peptide is found).
     */
    private double maxTagPrecursorErrorDa = 0;
    /**
     * The maximal tag precursor error in ppm in all PSMs (only the best tag hit
     * per spectrum when no peptide is found).
     */
    private double maxTagPrecursorErrorPpm = 0;
    /**
     * The charges found in all PSMs (only the best hit per spectrum). Charges are ordered.
     */
    private int[] foundCharges = new int[0];
    /**
     * The maximal amount of peptides in the proteins of the dataset.
     */
    private Integer maxNPeptides = null;
    /**
     * The maximal amount of spectra in the proteins of the dataset.
     */
    private Integer maxNSpectra = null;
    /**
     * The maximal spectrum counting in the proteins of the dataset.
     */
    private Double maxSpectrumCounting = null;
    /**
     * The weight of the fattest protein in the dataset.
     */
    private Double maxMW = null;
    /**
     * The ordered list of protein keys as estimated during the import process.
     */
    private long[] proteinKeys;
    /**
     * The max protein key length.
     */
    private Integer maxProteinKeyLength = null;
    /**
     * Number of validated proteins.
     */
    private int nValidatedProteins = -1;
    /**
     * Number of validated confident proteins.
     */
    private Integer nConfidentProteins = -1;
    /**
     * List of modifications found in the dataset.
     */
    private TreeSet<String> foundModifications = null;
    /**
     * The PSM matches for each fraction for each peptide. Key: 'fraction
     * name'_'peptide key'. Values: arraylist of spectrum keys.
     */
    private HashMap<String, ArrayList<Long>> fractionPsmMatches;
    /**
     * The total number of peptides per fraction.
     */
    private HashMap<String, Integer> totalPeptidesPerFractions;
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
     * The maximum protein summed precursor intensity.
     */
    private Double maxProteinSummedPrecursorIntensity = null;
    /**
     * The distribution of peptide validated lengths.
     */
    private NonSymmetricalNormalDistribution peptideLengthDistribution = null;
    /**
     * The sum of all spectrum counting indexes.
     */
    private Double totalSpectrumCounting = null;
    /**
     * The sum of all spectrum counting masses.
     */
    private Double totalSpectrumCountingMass = null;

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
    public int[] getFoundCharges() {
        return foundCharges;
    }

    /**
     * Clears the found charges.
     */
    public void clearFoundCharges() {
        foundCharges = new int[0];
    }

    /**
     * Adds a new charge to the list of found charges.
     *
     * @param foundCharges the new charge to add
     */
    public void addFoundCharges(HashSet<Integer> foundCharges) {
        this.foundCharges = IntStream.concat(Arrays.stream(this.foundCharges), foundCharges.stream().mapToInt(Function.identity()))
                .distinct()
                .sorted()
                .toArray();
    }
    
    /**
     * Returns the maximal charge among found charges.
     * 
     * @return the maximal charge
     */
    public int getMaxCharge() {
        
        return foundCharges[foundCharges.length-1];
        
    }

    /**
     * Return the max peptide precursor mass error in Dalton.
     *
     * @return the max peptide precursor mass error in Dalton
     */
    public double getMaxPeptidePrecursorErrorDa() {
        return maxPrecursorErrorDa;
    }

    /**
     * Set the max peptide precursor mass error in Dalton.
     *
     * @param maxPeptidePrecursorErrorDa the mass error to set
     */
    public void setMaxPeptidePrecursorErrorDa(double maxPeptidePrecursorErrorDa) {
        this.maxPrecursorErrorDa = maxPeptidePrecursorErrorDa;
    }

    /**
     * Returns the max peptide precursor mass error in ppm.
     *
     * @return the max peptide precursor mass error in ppm
     */
    public double getMaxPeptidePrecursorErrorPpm() {
        return maxPrecursorErrorPpm;
    }

    /**
     * Set the max peptide precursor mass error in ppm.
     *
     * @param maxPeptidePrecursorErrorPpm the max peptide precursor mass error
     * in ppm
     */
    public void setMaxPeptidePrecursorErrorPpm(double maxPeptidePrecursorErrorPpm) {
        this.maxPrecursorErrorPpm = maxPeptidePrecursorErrorPpm;
    }

    /**
     * Return the max tag precursor mass error in Dalton.
     *
     * @return the max tag precursor mass error in Dalton
     */
    public double getMaxTagPrecursorErrorDa() {
        return maxTagPrecursorErrorDa;
    }

    /**
     * Set the max tag precursor mass error in Dalton.
     *
     * @param maxTagPrecursorErrorDa the mass error to set
     */
    public void setMaxTagPrecursorErrorDa(double maxTagPrecursorErrorDa) {
        this.maxTagPrecursorErrorDa = maxTagPrecursorErrorDa;
    }

    /**
     * Returns the max tag precursor mass error in ppm.
     *
     * @return the max tag precursor mass error in ppm
     */
    public double getMaxTagPrecursorErrorPpm() {
        return maxTagPrecursorErrorPpm;
    }

    /**
     * Set the max tag precursor mass error in ppm.
     *
     * @param maxTagPrecursorErrorPpm the max tag precursor mass error in ppm
     */
    public void setMaxTagPrecursorErrorPpm(double maxTagPrecursorErrorPpm) {
        this.maxTagPrecursorErrorPpm = maxTagPrecursorErrorPpm;
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
     * Returns the the maximal amount of PSMs in the proteins of the dataset.
     *
     * @return the the maximal amount of PSMs in the proteins of the dataset
     */
    public Integer getMaxNSpectra() {
        return maxNSpectra;
    }

    /**
     * Sets the the maximal amount of PSMs in the proteins of the dataset.
     *
     * @param maxNSpectra the the maximal amount of PSMs in the proteins of the
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
    public long[] getProteinKeys() {
        return proteinKeys;
    }

    /**
     * Sets the list of ordered protein keys.
     *
     * @param proteinKeys the list of ordered protein keys
     */
    public void setProteinKeys(long[] proteinKeys) {
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
     * Returns the number of confident proteins.
     *
     * @return the number of confident proteins
     */
    public Integer getnConfidentProteins() {
        return nConfidentProteins;
    }

    /**
     * Sets the number of confident proteins.
     *
     * @param nConfidentProteins the number of confident proteins
     */
    public void setnConfidentProteins(int nConfidentProteins) {
        this.nConfidentProteins = nConfidentProteins;
    }

    /**
     * Returns the names of the variable modifications found in the dataset.
     *
     * @return the names of the variable modifications found in the dataset
     */
    public TreeSet<String> getFoundModifications() {
        
        return foundModifications;
        
    }

    /**
     * Sets the list of variable modifications found in the dataset.
     *
     * @param foundModifications the list of variable modifications found in the
     * dataset
     */
    public void setFoundModifications(TreeSet<String> foundModifications) {
        
        this.foundModifications = foundModifications;
        
    }

    /**
     * Set the fraction PSM matches. Key: 'fraction name'_'peptide key'. Values:
     * arraylist of spectrum keys.
     *
     * @param fractionPsmMatches the fraction PSM matches
     */
    public void setFractionPsmMatches(HashMap<String, ArrayList<Long>> fractionPsmMatches) {
        
        this.fractionPsmMatches = fractionPsmMatches;
    
    }

    /**
     * Returns the list of fraction PSM matches. Key: 'fraction name'_'peptide
     * key'. Values: arraylist of spectrum keys.
     *
     * @return he list of fraction PSM matches
     */
    public HashMap<String, ArrayList<Long>> getFractionPsmMatches() {
        
            return fractionPsmMatches;
            
    }

    /**
     * Set the total number of peptides per fraction.
     *
     * @param totalPeptidesPerFractions the total number of peptides per
     * fraction map
     */
    public void setTotalPeptidesPerFraction(HashMap<String, Integer> totalPeptidesPerFractions) {
        this.totalPeptidesPerFractions = totalPeptidesPerFractions;
    }

    /**
     * Returns the total number of peptides per fraction. Null if the values
     * have not been set.
     *
     * @return the total number of peptides per fraction, null if the values
     * have not been set
     */
    public HashMap<String, Integer> getTotalPeptidesPerFraction() {
        return totalPeptidesPerFractions;
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
            return new HashMap<>();
        }
    }

    /**
     * Set the observed molecular masses for each fraction in kDa. The key is
     * the file path of the fraction.
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
     * @param maxValidatedPeptidesPerFraction the
     * maxValidatedPeptidesPerFraction to set
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
     * @param maxValidatedSpectraPerFraction the maxValidatedSpectraPerFraction
     * to set
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
     * @param maxProteinAveragePrecursorIntensity the
     * maxProteinAveragePrecursorIntensity to set
     */
    public void setMaxProteinAveragePrecursorIntensity(Double maxProteinAveragePrecursorIntensity) {
        this.maxProteinAveragePrecursorIntensity = maxProteinAveragePrecursorIntensity;
    }

    /**
     * Returns the maximum summed protein precursor intensity.
     *
     * @return the maxProteinSummedPrecursorIntensity
     */
    public Double getMaxProteinSummedPrecursorIntensity() {
        return maxProteinSummedPrecursorIntensity;
    }

    /**
     * Set the maximum summed protein precursor intensity.
     *
     * @param maxProteinSummedPrecursorIntensity the
     * maxProteinSummedPrecursorIntensity to set
     */
    public void setMaxProteinSummedPrecursorIntensity(Double maxProteinSummedPrecursorIntensity) {
        this.maxProteinSummedPrecursorIntensity = maxProteinSummedPrecursorIntensity;
    }

    /**
     * Returns the distribution of validated peptide lengths. Null if not set.
     *
     * @return the distribution of validated peptide lengths
     */
    public NonSymmetricalNormalDistribution getPeptideLengthDistribution() {
        return peptideLengthDistribution;
    }

    /**
     * Sets the distribution of validated peptide lengths.
     *
     * @param peptideLengthDistribution the distribution of validated peptide
     * lengths
     */
    public void setPeptideLengthDistribution(NonSymmetricalNormalDistribution peptideLengthDistribution) {
        this.peptideLengthDistribution = peptideLengthDistribution;
    }

    /**
     * Returns the total spectrum counting masses.
     *
     * @return the total spectrum counting masses
     */
    public Double getTotalSpectrumCountingMass() {
        return totalSpectrumCountingMass;
    }

    /**
     * Sets the total spectrum counting value.
     *
     * @param totalSpectrumCountingMass the total spectrum counting masses
     */
    public void setTotalSpectrumCountingMass(double totalSpectrumCountingMass) {
        this.totalSpectrumCountingMass = totalSpectrumCountingMass;
    }

    /**
     * Returns the total spectrum counting.
     * 
     * @return the total spectrum counting
     */
    public Double getTotalSpectrumCounting() {
        return totalSpectrumCounting;
    }

    /**
     * Sets the total spectrum counting.
     * 
     * @param totalSpectrumCountingIndexes the total spectrum counting
     */
    public void setTotalSpectrumCounting(Double totalSpectrumCountingIndexes) {
        this.totalSpectrumCounting = totalSpectrumCountingIndexes;
    }

}
