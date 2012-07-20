package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * PeptideShaker compomics utilities experiment customizable parameter. This
 * parameter will be added to spectrum, peptide and protein matches to score
 * them, indicate the estimated posterior error probability associated and flag
 * whether they have been validated or not.
 *
 * @author Marc Vaudel
 */
public class PSParameter implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 2846587135366515967L; // @TODO: has to be updated??
    /**
     * Posterior error probability estimated for the search engine results.
     */
    private double searchEngineProbability;
    /**
     * Probabilistic score for a peptide to spectrum match in the dataset.
     */
    private double psmProbabilityScore;
    /**
     * Spectrum posterior error probability.
     */
    private double psmProbability;
    /**
     * Probabilistic score for a peptide match.
     */
    private double peptideProbabilityScore;
    /**
     * Peptide Posterior error probability.
     */
    private double peptideProbability;
    /**
     * Probabilistic score for a protein match.
     */
    private double proteinProbabilityScore;
    /**
     * Protein posterior error probability.
     */
    private double proteinProbability;
    /**
     * Boolean indicating whether a match is validated or not at the selected
     * threshold.
     */
    private boolean validated = false;
    /**
     * Boolean indicating whether this is a hidden match.
     */
    private boolean hidden = false;
    /**
     * Boolean indicating whether this is a starred match.
     */
    private boolean starred = false; // @TODO would be nice to change this into a color
    /**
     * the key in the corresponding specific map.
     */
    private String secificMapKey; // yes, we know about the typo, but cannot change it for backwards compatability reasons...
    /**
     * Protein groups can belong to the following groups according to the static
     * field indexing.
     */
    private int groupClass = NOT_GROUP;
    /**
     * Static index for a protein inference group: 0- not a protein group or
     * unique peptide of single protein group.
     */
    public static final int NOT_GROUP = 0;
    /**
     * Static index for a protein group: 1- isoforms or peptide of isoform
     * groups (not necessarily unique to the group).
     */
    public static final int ISOFORMS = 1;
    /**
     * Static index for a protein group: 2- isoforms and a few unrelated
     * proteins (less than 50%) or peptide shared by isoforms and non isoforms
     * (not necessarily unique to the group).
     */
    public static final int ISOFORMS_UNRELATED = 2;
    /**
     * Static index for a protein group: 3- unrelated proteins proteins or
     * peptide shared by unrelated proteins.
     */
    public static final int UNRELATED = 3;
    /**
     * The fraction confidence map.
     */
    private HashMap<String, Double> fractionPEP = new HashMap<String, Double>();
    /**
     * The fraction confidence map.
     */
    private HashMap<String, Double> fractionScore = new HashMap<String, Double>();
    /**
     * The number of validated peptides per fraction.
     */
    private HashMap<String, Integer> validatedPeptidesPerFraction = new HashMap<String, Integer>();
    /**
     * The number of validated spectra per fraction.
     */
    private HashMap<String, Integer> validatedSpectraPerFraction = new HashMap<String, Integer>();
    /**
     * The precursor intensity per fraction.
     */
    private HashMap<String, ArrayList<Double>> precursorIntensityPerFraction = new HashMap<String, ArrayList<Double>>();
    /**
     * The average precursor intensity per fraction.
     */
    private HashMap<String, Double> precursorIntensityAveragePerFraction = new HashMap<String, Double>();

    /**
     * Constructor.
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
        double confidence = 100.0 * (1 - peptideProbability);
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
        double confidence = 100.0 * (1 - proteinProbability);
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
        double confidence = 100.0 * (1 - searchEngineProbability);
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
        double confidence = 100.0 * (1 - psmProbability);
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
     * Returns the group class description.
     *
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
     * Returns the match key in the corresponding specific map.
     *
     * @return the match key in the corresponding specific map
     */
    public String getSecificMapKey() { // yes, we know about the typo, but cannot change it for backwards compatability reasons...
        return secificMapKey;
    }

    /**
     * Sets the match key in the corresponding specific map.
     *
     * @param specificMapKey the match key in the corresponding specific map
     */
    public void setSecificMapKey(String specificMapKey) { // yes, we know about the typo, but cannot change it for backwards compatability reasons...
        this.secificMapKey = specificMapKey;
    }

    /**
     * Sets the fraction confidence.
     *
     * @param fraction the fraction
     * @param confidence the confidence
     */
    public void setFractionScore(String fraction, double confidence) {
        fractionScore.put(fraction, confidence);
    }

    /**
     * Returns the fraction score. Null if not found.
     *
     * @param fraction
     * @return the fraction score
     */
    public double getFractionScore(String fraction) {
        return fractionScore.get(fraction);
    }

    /**
     * Return the fractions where this match was found. Null if not found.
     *
     * @return the fractions where this match was found
     */
    public Set<String> getFractions() {
        if (fractionScore != null) {
            return fractionScore.keySet();
        } else {
            return null;
        }
    }

    /**
     * Sets the fraction confidence.
     *
     * @param fraction the fraction
     * @param confidence the confidence
     */
    public void setFractionPEP(String fraction, double confidence) {
        fractionPEP.put(fraction, confidence);
    }

    /**
     * Returns the fraction pep. null if not found.
     *
     * @param fraction
     * @return the fraction pep
     */
    public double getFractionPEP(String fraction) {
        return fractionPEP.get(fraction);
    }

    /**
     * Returns the fraction confidence.
     *
     * @param fraction
     * @return the fraction confidence
     */
    public double getFractionConfidence(String fraction) {
        return 100 * (1 - fractionPEP.get(fraction));
    }
    
    /**
     * Get the number of validated peptides in the given fraction.
     * 
     * @param fraction the fraction
     * @return the number of validated peptides in the given fraction
     */
    public Integer getFractionValidatedPeptides(String fraction) {
        if (validatedPeptidesPerFraction != null) {
            return validatedPeptidesPerFraction.get(fraction);
        } else {
            return 0;
        }   
    }
    
    /**
     * Get the number of validated peptides in the given fraction.
     * 
     * @param validatedPeptidesPerFraction 
     */
    public void setFractionValidatedPeptides(HashMap<String, Integer> validatedPeptidesPerFraction) {
        this.validatedPeptidesPerFraction = validatedPeptidesPerFraction;
    }
    
    /**
     * Get the number of validated spectra in the given fraction.
     * 
     * @param fraction the fraction
     * @return the number of validated spectra in the given fraction
     */
    public Integer getFractionValidatedSpectra(String fraction) {
        if (validatedSpectraPerFraction != null) {
            return validatedSpectraPerFraction.get(fraction);
        } else {
            return 0;
        }   
    }
    
    /**
     * Get the number of validated spectra in the given fraction.
     * 
     * @param validatedSpectraPerFraction 
     */
    public void setFractionValidatedSpectra(HashMap<String, Integer> validatedSpectraPerFraction) {
        this.validatedSpectraPerFraction = validatedSpectraPerFraction;
    }
    
    /**
     * Get the precursor intensity in the given fraction.
     * 
     * @param fraction the fraction
     * @return the precursor intensity in the given fraction
     */
    public ArrayList<Double> getPrecursorIntensityPerFraction(String fraction) {
        if (precursorIntensityPerFraction != null) {
            return precursorIntensityPerFraction.get(fraction);
        } else {
            return new ArrayList<Double>();
        }   
    }
    
    /**
     * Get the precursor intensity in the given fraction.
     * 
     * @param precursorIntensityPerFraction 
     */
    public void setPrecursorIntensityPerFraction(HashMap<String, ArrayList<Double>> precursorIntensityPerFraction) {
        this.precursorIntensityPerFraction = precursorIntensityPerFraction;
        
        // calculate the average precursor intensities
        for (String fraction : precursorIntensityPerFraction.keySet()) {
            
            Double sum = 0.0;
            
            for (Double intensity : precursorIntensityPerFraction.get(fraction)) {
                sum += intensity;
            }
            
            if (sum > 0) {
                precursorIntensityAveragePerFraction.put(fraction, sum / precursorIntensityPerFraction.get(fraction).size());
            } else {
                precursorIntensityAveragePerFraction.put(fraction, null);
            }   
        }
    }
    
    /**
     * Get the average precursor intensity in the given fraction.
     * 
     * @param fraction the fraction
     * @return the average precursor intensity in the given fraction
     */
    public Double getPrecursorIntensityAveragePerFraction(String fraction) {
        if (precursorIntensityAveragePerFraction != null) {
            return precursorIntensityAveragePerFraction.get(fraction);
        } else {
            return null;
        }  
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
