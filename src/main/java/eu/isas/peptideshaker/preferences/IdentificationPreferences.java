package eu.isas.peptideshaker.preferences;

/**
 * This class will contain all user preferences for the identification processing
 *
 * @author Marc
 */
public class IdentificationPreferences {

    /**
     * The FDR protein threshold to use.
     */
    private double proteinThreshold;
    /**
     * The FDR peptide threshold to use
     */
    private double peptideThreshold;
    /**
     * The FDR psm threshold to use
     */
    private double psmThreshold;
    /**
     * Shall probabilistic FDR be used
     */
    private boolean probabilisticFDR;
    /**
     * Shall validate peptides and PSMs after protein validation
     */
    private boolean aPosterioriValidation;

    /**
     * Constructor
     * @param proteinThreshold      The protein threshold to be used
     * @param peptideThreshold      The peptide threshold to be used
     * @param psmThreshold          The psm threshold to be used
     * @param probabilisticFDR      Boolean indicating whether probabilistic FDR should be used
     * @param aPosterioriValidation    Boolean indicating whether peptides and PSMs should be a posteriori validated
     */
    public IdentificationPreferences(double proteinThreshold, double peptideThreshold, double psmThreshold, boolean probabilisticFDR, boolean aPosterioriValidation) {
        this.proteinThreshold = proteinThreshold;
        this.peptideThreshold = peptideThreshold;
        this.psmThreshold = psmThreshold;
        this.probabilisticFDR = probabilisticFDR;
        this.aPosterioriValidation = aPosterioriValidation;
    }

    /**
     * Getter for the peptide threshold
     * @return the peptide threshold
     */
    public double getPeptideThreshold() {
        return peptideThreshold;
    }

    /**
     * Returns true if probabilistic FDR should be used when recommended
     * @return true if probabilistic FDR should be used when recommended
     */
    public boolean useProbabilisticFDR() {
        return probabilisticFDR;
    }

    /**
     * Getter for the protein Threshold
     * @return the protein threshold
     */
    public double getProteinThreshold() {
        return proteinThreshold;
    }

    /**
     * Getter for the psm threshold
     * @return the psm threshold
     */
    public double getPsmThreshold() {
        return psmThreshold;
    }

    /**
     * Returns true if the peptides and PSMs shall be validated after protein validation
     * @return true if the peptides and PSMs shall be validated after protein validation
     */
    public boolean aPosterioriValidation() {
        return aPosterioriValidation;
    }

}
