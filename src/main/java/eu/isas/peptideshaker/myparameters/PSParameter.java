package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    static final long serialVersionUID = 2846587135366515967L;
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
     * @deprecated use matchValidationLevel instead
     */
    private boolean validated = false;
    /**
     * The validation level of a given match.
     */
    private MatchValidationLevel matchValidationLevel;
    /**
     * Boolean indicating whether the validation confidence was manually updated.
     */
    private Boolean manualValidation = false;
    /**
     * The reason why a match is flagged as doubtful.
     */
    private String reasonDoubtful = null;
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
     * Static index for a protein inference group: 0 - not a protein group or
     * unique peptide of single protein group.
     */
    public static final int NOT_GROUP = 0;
    /**
     * Static index for a protein group: 1 - related proteins or peptide from
     * related protein groups (not necessarily unique to the group).
     */
    public static final int RELATED = 1;
    /**
     * Static index for a protein group: 2 - related and a unrelated
     * proteins or peptide shared by related and unrelated proteins
     * (not necessarily unique to the group).
     */
    public static final int RELATED_AND_UNRELATED = 2;
    /**
     * Static index for a protein group: 3 - unrelated proteins proteins or
     * peptide shared by unrelated proteins.
     */
    public static final int UNRELATED = 3;
    /**
     * Static index for a protein group: 1 - isoforms or peptide of isoform
     * groups (not necessarily unique to the group).
     * 
     * @deprecated use RELATED instead
     */
    public static final int ISOFORMS = 1;
    /**
     * Static index for a protein group: 2 - isoforms and a few unrelated
     * proteins (less than 50%) or peptide shared by isoforms and non isoforms
     * (not necessarily unique to the group).
     * 
     * @deprecated use RELATED_AND_UNRELATED instead
     */
    public static final int ISOFORMS_UNRELATED = 2;
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
     * The summed precursor intensity per fraction.
     */
    private HashMap<String, Double> precursorIntensitySummedPerFraction = new HashMap<String, Double>();
    /**
     * The results of the validation quality filters
     */
    private HashMap<String, Boolean> qcFilters = new HashMap<String, Boolean>();

    /**
     * Constructor.
     */
    public PSParameter() {
    }

    /**
     * Returns the peptide posterior error probability.
     *
     * @return the peptide posterior error probability
     */
    public double getPeptideProbability() {
        return peptideProbability;
    }

    /**
     * Set the peptide posterior error probability.
     *
     * @param peptideProbability the new peptide posterior error probability
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
     * Returns the protein posterior error probability.
     *
     * @return the protein posterior error probability
     */
    public double getProteinProbability() {
        return proteinProbability;
    }

    /**
     * Set the protein posterior error probability.
     *
     * @param proteinProbability the new protein posterior error probability
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
     * Returns the search engine posterior error probability.
     *
     * @return the search engine posterior error probability
     */
    public double getSearchEngineProbability() {
        return searchEngineProbability;
    }

    /**
     * Set the search engine posterior error probability.
     *
     * @param searchEngineProbability the new search engine posterior error probability
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
     * Returns the PSM posterior error probability.
     *
     * @return the PSM posterior error probability
     */
    public double getPsmProbability() {
        return psmProbability;
    }

    /**
     * Set the the PSM posterior error probability.
     *
     * @param psmProbability the new the PSM posterior error probability
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
     * @deprecated use setMatchValidationLevel instead
     * @param validated boolean indicating whether the match should be validated
     */
    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    /**
     * Returns whether a match is validated or not.
     * 
     * @deprecated use getMatchValidationLevel instead
     * @return boolean indicating whether a match is validated or not
     */
    public boolean isValidated() {
        return validated;
    }

    /**
     * Returns the validation level of the match.
     * 
     * @return the validation level of the match
     */
    public MatchValidationLevel getMatchValidationLevel() {
        if (matchValidationLevel == null) { // Backward compatibility check
            if (validated) {
                matchValidationLevel = MatchValidationLevel.confident;
            } else {
                matchValidationLevel = MatchValidationLevel.not_validated;
            }
        }
        return matchValidationLevel;
    }

    /**
     * Sets the validation level of the match.
     * 
     * @param matchValidationLevel the validation level of the match
     */
    public void setMatchValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.matchValidationLevel = matchValidationLevel;
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
     * Returns the protein inference class of the protein match.
     *
     * @return the protein inference class of the protein match.
     */
    public int getProteinInferenceClass() {
        return groupClass;
    }

    /**
     * Returns the protein inference class as a string for the given
     * integer-based class
     *
     * @return the group class description
     */
    public String getProteinInferenceClassAsString() {
        return getProteinInferenceClassAsString(groupClass);
    }

    /**
     * Returns the protein inference class as a string for the given
     * integer-based class.
     *
     * @param matchClass the protein inference class as integer (see static
     * fields)
     * @return the group class description
     */
    public static String getProteinInferenceClassAsString(int matchClass) {
        switch (matchClass) {
            case NOT_GROUP:
                return "Single Protein";
            case RELATED:
                return "Related Proteins";
            case RELATED_AND_UNRELATED:
                return "Related and Unrelated Proteins";
            case UNRELATED:
                return "Unrelated Proteins";
            default:
                return "";
        }
    }

    /**
     * Sets the protein group class.
     *
     * @param groupClass the protein group class
     */
    public void setProteinInferenceClass(int groupClass) {
        this.groupClass = groupClass;
    }

    /**
     * Returns the match key in the corresponding specific map.
     *
     * @deprecated use method without typo
     * @return the match key in the corresponding specific map
     */
    public String getSecificMapKey() {
        return getSpecificMapKey();
    }

    /**
     * Sets the match key in the corresponding specific map.
     *
     * @deprecated use method without typo
     * @param specificMapKey the match key in the corresponding specific map
     */
    public void setSecificMapKey(String specificMapKey) {
        setSpecificMapKey(specificMapKey);
    }

    /**
     * Returns the match key in the corresponding specific map.
     *
     * @return the match key in the corresponding specific map
     */
    public String getSpecificMapKey() {
        return secificMapKey;
    }

    /**
     * Sets the match key in the corresponding specific map.
     *
     * @param specificMapKey the match key in the corresponding specific map
     */
    public void setSpecificMapKey(String specificMapKey) {
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

            if (precursorIntensitySummedPerFraction != null) {
                precursorIntensitySummedPerFraction.put(fraction, sum);
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

    /**
     * Get the summed precursor intensity in the given fraction.
     *
     * @param fraction the fraction
     * @return the summed precursor intensity in the given fraction
     */
    public Double getPrecursorIntensitySummedPerFraction(String fraction) {
        if (precursorIntensitySummedPerFraction != null) {
            return precursorIntensitySummedPerFraction.get(fraction);
        } else {
            return null;
        }
    }

    /**
     * Returns the reason why a match is set as doubtful. An empty string if none set.
     * 
     * @return the reason why a match is set as doubtful
     */
    public String getReasonDoubtful() {
        if (reasonDoubtful == null) {
            return "";
        }
        return reasonDoubtful;
    }

    /**
     * Sets the reason why a match is set as doubtful.
     * 
     * @param reasonDoubtful the reason why a match is set as doubtful
     */
    public void setReasonDoubtful(String reasonDoubtful) {
        this.reasonDoubtful = reasonDoubtful;
    }

    /**
     * Indicates whether the match validation was manually inspected.
     * 
     * @return a boolean indicating whether the match validation was manually inspected
     */
    public Boolean isManualValidation() {
        if (manualValidation == null) {
            manualValidation = false;
        }
        return manualValidation;
    }

    /**
     * Sets whether the match validation was manually inspected.
     * 
     * @param manualValidation a boolean indicating whether the match validation was manually inspected
     */
    public void setManualValidation(Boolean manualValidation) {
        this.manualValidation = manualValidation;
    }
    
    /**
     * Sets whether the match passed a quality control check.
     * 
     * @param criterion the QC criterion
     * @param validated boolean indicating whether the test was passed
     */
    public void setQcResult(String criterion, boolean validated) {
        if (qcFilters == null) {
            qcFilters = new HashMap<String, Boolean>();
        }
        qcFilters.put(criterion, validated);
    }
    
    /**
     * Indicates whether the given QC check was passed.
     * 
     * @param criterion the QC criterion
     * 
     * @return a boolean indicating whether the test was passed
     */
    public Boolean isQcPassed(String criterion) {
        if (qcFilters == null) {
            return null;
        }
       return  qcFilters.get(criterion);
    }
    
    /**
     * Returns the list of qc checks made for this match.
     * 
     * @return the list of qc checks made for this match in a set
     */
    public Set<String> getQcCriteria() {
        if (qcFilters == null) {
            return new HashSet<String>();
        }
        return qcFilters.keySet();
    }
    
    /**
     * Indicates whether QC filters were implemented for this match.
     * 
     * @return a boolean indicating whether QC filters were implemented for this match
     */
    public boolean hasQcFilters() {
        return qcFilters != null;
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
