package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;


/**
 * Peptide-Shaker compomics utilities experiment customizable parameter.
 * This parameter will be added to spectrum, peptide and protein matches to score them, indicate the estimated posterior error probability associated and flag whether they have been validated or not.
 *
 * @author Marc Vaudel
 */
public class PSParameter implements UrParameter {

    /**
     * Posterior error probability estimated for the search engine results (used only in the case of a multiple search engine study)
     */
    private double searchEngineProbability;
    /**
     * Probabilistic score for a peptide to spectrum match in the dataset
     */
    private double spectrumProbabilityScore;
    /**
     * Spectrum posterior error probability
     */
    private double spectrumProbability;
    /**
     * Probabilistic score for a peptide match
     */
    private double peptideProbabilityScore;
    /**
     * Peptide Posterior error probability
     */
    private double peptideProbability;
    /**
     * Probabilistic score for a protein match
     */
    private double proteinProbabilityScore;
    /**
     * Protein posterior error probability
     */
    private double proteinProbability;
    /**
     * Protein corrected probability
     */
    private double proteinCorrectedProbability;
    /**
     * Boolean indicating whether a match is validated or not at the selected threshold
     */
    private boolean validated = false;

    /**
     * Constructor
     */
    public PSParameter() {

    }

    /**
     * Returns the peptide probability.
     *
     * @return the peptide probability
     */
    public double getPeptideProbability() {
        return peptideProbability;
    }

    /**
     * Set the peptide probability.
     *
     * @param peptideProbability the new peptide probability
     */
    public void setPeptideProbability(double peptideProbability) {
        this.peptideProbability = peptideProbability;
    }

    /**
     * Returns the peptide Probabilistic score.
     *
     * @return the peptide Probabilistic score
     */
    public double getPeptideProbabilityScore() {
        return peptideProbabilityScore;
    }

    /**
     * Set the peptide Probabilistic score.
     *
     * @param peptideProbabilityScore the new peptide Probabilistic score
     */
    public void setPeptideProbabilityScore(double peptideProbabilityScore) {
        this.peptideProbabilityScore = peptideProbabilityScore;
    }

    /**
     * Returns the protein corrected probability.
     *
     * @return the protein corrected probability
     */
    public double getProteinCorrectedProbability() {
        return proteinCorrectedProbability;
    }

    /**
     * Set the protein corrected probability.
     *
     * @param proteinCorrectedProbability the new protein corrected probability
     */
    public void setProteinCorrectedProbability(double proteinCorrectedProbability) {
        this.proteinCorrectedProbability = proteinCorrectedProbability;
    }

    /**
     * Returns the protein probability.
     *
     * @return the protein probability
     */
    public double getProteinProbability() {
        return proteinProbability;
    }

    /**
     * Set the protein probability.
     *
     * @param proteinProbability the new protein probability
     */
    public void setProteinProbability(double proteinProbability) {
        this.proteinProbability = proteinProbability;
    }

    /**
     * Returns the protein Probabilistic score.
     *
     * @return the protein Probabilistic score
     */
    public double getProteinProbabilityScore() {
        return proteinProbabilityScore;
    }

    /**
     * Set the protein Probabilistic score
     *
     * @param proteinProbabilityScore the new protein Probabilistic score
     */
    public void setProteinProbabilityScore(double proteinProbabilityScore) {
        this.proteinProbabilityScore = proteinProbabilityScore;
    }

    /**
     * Returns the search engine probability.
     *
     * @return the search engine probability
     */
    public double getSearchEngineProbability() {
        return searchEngineProbability;
    }

    /**
     * Set the search engine probability.
     *
     * @param searchEngineProbability the new search engine probability
     */
    public void setSearchEngineProbability(double searchEngineProbability) {
        this.searchEngineProbability = searchEngineProbability;
    }

    /**
     * Returns the spectrum probability.
     *
     * @return the spectrum probability
     */
    public double getSpectrumProbability() {
        return spectrumProbability;
    }

    /**
     * Set the the spectrum probability.
     *
     * @param spectrumProbability the new the spectrum probability
     */
    public void setSpectrumProbability(double spectrumProbability) {
        this.spectrumProbability = spectrumProbability;
    }

    /**
     * Returns the the spectrum Probabilistic score.
     *
     * @return the spectrum Probabilistic score
     */
    public double getSpectrumProbabilityScore() {
        return spectrumProbabilityScore;
    }

    /**
     * Set the spectrum Probabilistic score
     *
     * @param spectrumProbabilityScore the new spectrum Probabilistic score
     */
    public void setSpectrumProbabilityScore(double spectrumProbabilityScore) {
        this.spectrumProbabilityScore = spectrumProbabilityScore;
    }
    
    /**
     * Un/Validates a match
     * @param validated boolean indicating whether the match should be validated
     */
    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    /**
     * Returns whether a match is validated or not
     * @return boolean indicating whether a match is validated or not
     */
    public boolean isValidated() {
        return validated;
    }


    @Override
    public String getFamilyName() {
        return "PeptideShaker";
    }

    @Override
    public int getIndex() {
        return 0;
    }
}
