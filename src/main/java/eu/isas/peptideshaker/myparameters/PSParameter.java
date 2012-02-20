package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;

/**
 * PeptideShaker compomics utilities experiment customizable parameter.
 * This parameter will be added to spectrum, peptide and protein matches to score 
 * them, indicate the estimated posterior error probability associated and flag 
 * whether they have been validated or not.
 *
 * @author Marc Vaudel
 */
public class PSParameter implements UrParameter {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = 2846587135366515967L;
    /**
     * Posterior error probability estimated for the search engine results
     */
    private double searchEngineProbability;
    /**
     * Probabilistic score for a peptide to spectrum match in the dataset
     */
    private double psmProbabilityScore;
    /**
     * Spectrum posterior error probability
     */
    private double psmProbability;
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
     * Boolean indicating whether a match is validated or not at the selected threshold
     */
    private boolean validated = false;
    /**
     * Boolean indicating whether this is a hidden match
     */
    private boolean hidden = false;
    /**
     * Boolean indicating whether this is a starred match
     */
    private boolean starred = false; // @TODO would be nice to change this into a color
    /**
     * the key in the corresponding specific map
     */
    private String secificMapKey;
    /**
     * Protein groups can belong to the following groups according to the static field indexing.
     */
    private int groupClass = NOT_GROUP;
    /**
     * Static index for a protein inference group:
     * 0- not a protein group or unique peptide of single protein group
     */
    public static final int NOT_GROUP = 0;
    /**
     * Static index for a protein group:
     * 1- isoforms or peptide of isoform groups (not necessarily unique to the group)
     */
    public static final int ISOFORMS = 1;
    /**
     * Static index for a protein group:
     * 2- isoforms and a few unrelated proteins (less than 50%) or peptide shared by isoforms and non isoforms (not necessarily unique to the group)
     */
    public static final int ISOFORMS_UNRELATED = 2;
    /**
     * Static index for a protein group:
     * 3- unrelated proteins proteins or peptide shared by unrelated proteins
     */
    public static final int UNRELATED = 3;

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
     * Returns the peptide score.
     * 
     * @return the peptide score
     */
    public double getPeptideScore() {
        double score;
        if (peptideProbabilityScore < Math.pow(10, -100)) {
            score = 100;
        } else {
            score = -10 * Math.log10(peptideProbabilityScore);
        }
        if (score <= 0) {
            score = 0;
        }
        return score;
    }

    /**
     * Returns the peptide confidence.
     * 
     * @return the peptide confidence
     */
    public double getPeptideConfidence() {
        double confidence = 100.0*(1-peptideProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
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
     * Returns the protein score.
     * 
     * @return the protein score
     */
    public double getProteinScore() {
        double score;
        if (proteinProbabilityScore < Math.pow(10, -100)) {
            score = 100;
        } else {
            score = -10 * Math.log10(proteinProbabilityScore);
        }
        if (score <= 0) {
            score = 0;
        }
        return score;
    }

    /**
     * Returns the protein confidence.
     * 
     * @return the protein confidence
     */
    public double getProteinConfidence() {
        double confidence = 100.0*(1-proteinProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
    }

    /**
     * Set the protein Probabilistic score.
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
     * Returns the search engine confidence.
     * 
     * @return the search engine confidence
     */
    public double getSearchEngineConfidence() {
        double confidence = 100.0*(1-searchEngineProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
    }

    /**
     * Returns the PSM probability.
     *
     * @return the PSM probability
     */
    public double getPsmProbability() {
        return psmProbability;
    }

    /**
     * Set the the PSM probability.
     *
     * @param psmProbability the new the PSM probability
     */
    public void setPsmProbability(double psmProbability) {
        this.psmProbability = psmProbability;
    }

    /**
     * Returns the the PSM Probabilistic score.
     *
     * @return the PSM Probabilistic score
     */
    public double getPsmProbabilityScore() {
        return psmProbabilityScore;
    }

    /**
     * Set the PSM Probabilistic score.
     *
     * @param psmProbabilityScore the new PSM Probabilistic score
     */
    public void setSpectrumProbabilityScore(double psmProbabilityScore) {
        this.psmProbabilityScore = psmProbabilityScore;
    }

    /**
     * Returns the PSM score.
     * 
     * @return the PSM score
     */
    public double getPsmScore() {
        double score;
        if (psmProbabilityScore < Math.pow(10, -100)) {
            score = 100;
        } else {
            score = -10 * Math.log10(psmProbabilityScore);
        }
        if (score <= 0) {
            score = 0;
        }
        return score;
    }

    /**
     * Returns the PSM confidence.
     * 
     * @return the PSM confidence
     */
    public double getPsmConfidence() {
        double confidence = 100.0*(1-psmProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
    }

    /**
     * Un/Validates a match.
     * 
     * @param validated boolean indicating whether the match should be validated
     */
    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    /**
     * Returns whether a match is validated or not.
     * 
     * @return boolean indicating whether a match is validated or not
     */
    public boolean isValidated() {
        return validated;
    }

    /**
     * Hide/Unhide a match.
     * 
     * @param hidden boolean indicating whether the match should be hidden
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Returns whether a match is hidden or not.
     * 
     * @return boolean indicating whether a match is hidden or not
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Star/Unstar a match.
     * 
     * @param starred boolean indicating whether the match should be starred
     */
    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    /**
     * Returns whether a match is starred or not.
     * 
     * @return boolean indicating whether a match is starred or not
     */
    public boolean isStarred() {
        return starred;
    }

    /**
     * Returns the protein group class.
     * 
     * @return the protein group class
     */
    public int getGroupClass() {
        return groupClass;
    }

    /**
     * Returns the group class description
     * @return the group class description 
     */
    public String getGroupName() {
        switch (groupClass) {
            case NOT_GROUP:
                return "Single Protein";
            case ISOFORMS:
                return "Isoforms";
            case ISOFORMS_UNRELATED:
                return "Isoforms and Unrelated protein(s)";
            case UNRELATED:
                return "Unrelated proteins";
            default:
                return "";
        }
    }

    /**
     * Sets the protein group class.
     * 
     * @param groupClass the protein group class
     */
    public void setGroupClass(int groupClass) {
        this.groupClass = groupClass;
    }

    /**
     * Returns the match key in the corresponding specific map
     * @return the match key in the corresponding specific map
     */
    public String getSecificMapKey() {
        return secificMapKey;
    }

    /**
     * Sets the match key in the corresponding specific map
     * @param secificMapKey the match key in the corresponding specific map
     */
    public void setSecificMapKey(String secificMapKey) {
        this.secificMapKey = secificMapKey;
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
