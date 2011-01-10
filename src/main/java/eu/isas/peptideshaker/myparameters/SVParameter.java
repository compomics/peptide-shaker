package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.utils.UrParameter;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class SVParameter implements UrParameter {

    private double searchEngineProbability;
    private double spectrumProbabilityScore;
    private double spectrumProbability;
    private double peptideProbabilityScore;
    private double peptideProbability;
    private double peptideSpecificProbability;
    private double proteinProbabilityScore;
    private double proteinProbability;

    /**
     * @TODO: JavaDoc missing
     */
    public SVParameter() {

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
     * Returns the peptide probability score.
     *
     * @return the peptide probability score
     */
    public double getPeptideProbabilityScore() {
        return peptideProbabilityScore;
    }

    /**
     * Set the peptide probability score.
     *
     * @param peptideProbabilityScore the new peptide probability score
     */
    public void setPeptideProbabilityScore(double peptideProbabilityScore) {
        this.peptideProbabilityScore = peptideProbabilityScore;
    }

    /**
     * Returns the peptide specific probability.
     *
     * @return the peptide specific probability
     */
    public double getPeptideSpecificProbability() {
        return peptideSpecificProbability;
    }

    /**
     * Set the peptide specific probability
     *
     * @param peptideSpecificProbability the new peptide specific probability
     */
    public void setPeptideSpecificProbability(double peptideSpecificProbability) {
        this.peptideSpecificProbability = peptideSpecificProbability;
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
     * Returns the protein probability score.
     *
     * @return the protein probability score
     */
    public double getProteinProbabilityScore() {
        return proteinProbabilityScore;
    }

    /**
     * Set the protein probability score
     *
     * @param proteinProbabilityScore the new protein probability score
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
     * Returns the the spectrum probability score.
     *
     * @return the spectrum probability score
     */
    public double getSpectrumProbabilityScore() {
        return spectrumProbabilityScore;
    }

    /**
     * Set the spectrum probability score
     *
     * @param spectrumProbabilityScore the new spectrum probability score
     */
    public void setSpectrumProbabilityScore(double spectrumProbabilityScore) {
        this.spectrumProbabilityScore = spectrumProbabilityScore;
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
