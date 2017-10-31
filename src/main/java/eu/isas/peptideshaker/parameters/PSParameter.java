package eu.isas.peptideshaker.parameters;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math.util.FastMath;

/**
 * PeptideShaker compomics utilities experiment customizable parameter. This
 * parameter will be added to spectrum, peptide and protein matches to score
 * them, indicate the estimated posterior error probability associated and flag
 * whether they have been validated or not.
 *
 * @author Marc Vaudel
 */
public class PSParameter extends DbObject implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 2846587135366515967L;
    /**
     * Posterior error probability estimated for the search engine results.
     */
    private double searchEngineProbability;
    /**
     * The difference in identification algorithm level PEP with the next best
     * peptide assumption with sequence difference for a given search engine.
     */
    private Double algorithmDeltaPEP;
    /**
     * The difference in identification algorithm level PEP with the next best
     * peptide assumption with sequence difference across all search engines.
     */
    private Double deltaPEP;
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
     * The validation level of a given match.
     */
    private MatchValidationLevel matchValidationLevel;
    /**
     * Boolean indicating whether the validation confidence was manually
     * updated.
     */
    private Boolean manualValidation = false;
    /**
     * Boolean indicating whether this is a hidden match.
     */
    private boolean hidden = false;
    /**
     * Boolean indicating whether this is a starred match.
     */
    private boolean starred = false; // @TODO would be nice to change this into a symbol/color
    /**
     * the key in the corresponding specific map.
     */
    private String specificMapKey;
    /**
     * Protein groups can belong to the following groups according to the static
     * field indexing.
     */
    private int proteinInferenceGroupClass = NOT_GROUP;
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
     * Static index for a protein group: 2 - related and a unrelated proteins or
     * peptide shared by related and unrelated proteins (not necessarily unique
     * to the group).
     */
    public static final int RELATED_AND_UNRELATED = 2;
    /**
     * Static index for a protein group: 3 - unrelated proteins proteins or
     * peptide shared by unrelated proteins.
     */
    public static final int UNRELATED = 3;
    /**
     * The fraction confidence map.
     */
    private HashMap<String, Double> fractionPEP = null;
    /**
     * The fraction confidence map.
     */
    private HashMap<String, Double> fractionScore = null;
    /**
     * The number of validated peptides per fraction.
     */
    private HashMap<String, Integer> validatedPeptidesPerFraction = null;
    /**
     * The number of validated spectra per fraction.
     */
    private HashMap<String, Integer> validatedSpectraPerFraction = null;
    /**
     * The precursor intensity per fraction.
     */
    private HashMap<String, ArrayList<Double>> precursorIntensityPerFraction = null;
    /**
     * The average precursor intensity per fraction.
     */
    private HashMap<String, Double> precursorIntensityAveragePerFraction = null;
    /**
     * The summed precursor intensity per fraction.
     */
    private HashMap<String, Double> precursorIntensitySummedPerFraction = null;
    /**
     * The results of the validation quality filters.
     */
    private HashMap<String, Boolean> qcFilters = null;
    /**
     * Map of the intermediate scores. Score index &gt; value
     */
    private HashMap<Integer, Double> intermediateScores;
    /**
     * An empty parameter used for instantiation.
     */
    public static final PSParameter dummy = new PSParameter();

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
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return peptideProbability;
    }
    
    public void setGroupClass(int groupClass){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.proteinInferenceGroupClass = groupClass;
    }

    /**
     * Set the peptide posterior error probability. Note: if
     * PsmScores.scoreRoundingDecimal is not null the scored will be floored
     * accordingly.
     *
     * @param peptideProbability the new peptide posterior error probability
     */
    public void setPeptideProbability(double peptideProbability) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.peptideProbability = peptideProbability;
    }

    /**
     * Returns the peptide Probabilistic score.
     *
     * @return the peptide Probabilistic score
     */
    public double getPeptideProbabilityScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return peptideProbabilityScore;
    }

    /**
     * Returns the peptide score.
     *
     * @return the peptide score
     */
    public double getPeptideScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return getScore(peptideProbabilityScore);
    }

    /**
     * Returns the peptide confidence.
     *
     * @return the peptide confidence
     */
    public double getPeptideConfidence() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        double confidence = 100.0 * (1 - peptideProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
    }

    /**
     * Set the peptide Probabilistic score. Note: if
     * PsmScores.scoreRoundingDecimal is not null the scored will be floored
     * accordingly.
     *
     * @param peptideProbabilityScore the new peptide Probabilistic score
     */
    public void setPeptideProbabilityScore(double peptideProbabilityScore) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.peptideProbabilityScore = peptideProbabilityScore;
    }

    /**
     * Returns the protein posterior error probability.
     *
     * @return the protein posterior error probability
     */
    public double getProteinProbability() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinProbability;
    }

    /**
     * Set the protein posterior error probability. Note: if
     * PsmScores.scoreRoundingDecimal is not null the scored will be floored
     * accordingly.
     *
     * @param proteinProbability the new protein posterior error probability
     */
    public void setProteinProbability(double proteinProbability) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.proteinProbability = proteinProbability;
    }

    /**
     * Returns the protein Probabilistic score.
     *
     * @return the protein Probabilistic score
     */
    public double getProteinProbabilityScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinProbabilityScore;
    }

    /**
     * Returns the protein score.
     *
     * @return the protein score
     */
    public double getProteinScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return getScore(proteinProbabilityScore);
    }

    /**
     * Returns the protein confidence.
     *
     * @return the protein confidence
     */
    public double getProteinConfidence() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        double confidence = 100.0 * (1 - proteinProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
    }

    /**
     * Set the protein Probabilistic score. Note: if
     * PsmScores.scoreRoundingDecimal is not null the scored will be floored
     * accordingly.
     *
     * @param proteinProbabilityScore the new protein Probabilistic score
     */
    public void setProteinProbabilityScore(double proteinProbabilityScore) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.proteinProbabilityScore = proteinProbabilityScore;
    }

    /**
     * Returns the search engine posterior error probability.
     *
     * @return the search engine posterior error probability
     */
    public double getSearchEngineProbability() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return searchEngineProbability;
    }

    /**
     * Set the search engine posterior error probability. Note: if
     * PsmScores.scoreRoundingDecimal is not null the scored will be floored
     * accordingly.
     *
     * @param searchEngineProbability the new search engine posterior error
     * probability
     */
    public void setSearchEngineProbability(double searchEngineProbability) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.searchEngineProbability = searchEngineProbability;
    }

    /**
     * Returns the difference in identification algorithm level PEP with the
     * next best peptide assumption with sequence difference for the given
     * search engine.
     *
     * @return the difference in identification algorithm level PEP with the
     * next best peptide assumption with sequence difference for the given
     * search engine
     */
    public Double getAlgorithmDeltaPEP() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return algorithmDeltaPEP;
    }

    /**
     * Sets the difference in identification algorithm level PEP with the next
     * best peptide assumption with sequence difference for the given search
     * engine.
     *
     * @param deltaPEP the difference in identification algorithm level PEP with
     * the next best peptide assumption with sequence difference for the given
     * search engine
     */
    public void setAlgorithmDeltaPEP(Double deltaPEP) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.algorithmDeltaPEP = deltaPEP;
    }

    /**
     * Returns the difference in identification algorithm level PEP with the
     * next best peptide assumption with sequence difference across all search
     * engines.
     *
     * @return the difference in identification algorithm level PEP with the
     * next best peptide assumption with sequence difference across all search
     * engines
     */
    public Double getDeltaPEP() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return deltaPEP;
    }

    /**
     * Sets the difference in identification algorithm level PEP with the next
     * best peptide assumption with sequence difference across all search
     * engines.
     *
     * @param deltaPEP the difference in identification algorithm level PEP with
     * the next best peptide assumption with sequence difference across all
     * search engines
     */
    public void setDeltaPEP(Double deltaPEP) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.deltaPEP = deltaPEP;
    }

    /**
     * Returns the search engine confidence.
     *
     * @return the search engine confidence
     */
    public double getSearchEngineConfidence() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
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
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return psmProbability;
    }

    /**
     * Set the the PSM posterior error probability. Note: if
     * PsmScores.scoreRoundingDecimal is not null the scored will be floored
     * accordingly.
     *
     * @param psmProbability the new the PSM posterior error probability
     */
    public void setPsmProbability(double psmProbability) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.psmProbability = psmProbability;
    }
    
    public void setPsmProbabilityScore(double psmProbabilityScore) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.psmProbabilityScore = psmProbabilityScore;
    }
    
    public void setQcFilters(HashMap<String, Boolean> qcFilters){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.qcFilters = qcFilters;
    }

    /**
     * Returns the the PSM Probabilistic score.
     *
     * @return the PSM Probabilistic score
     */
    public double getPsmProbabilityScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return psmProbabilityScore;
    }

    /**
     * Set the PSM Probabilistic score. Note: if PsmScores.scoreRoundingDecimal
     * is not null the scored will be floored accordingly.
     *
     * @param psmProbabilityScore the new PSM Probabilistic score
     */
    public void setSpectrumProbabilityScore(double psmProbabilityScore) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.psmProbabilityScore = psmProbabilityScore;
    }

    /**
     * Returns the PSM score.
     *
     * @return the PSM score
     */
    public double getPsmScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return getScore(psmProbabilityScore);
    }

    /**
     * Returns the PSM confidence.
     *
     * @return the PSM confidence
     */
    public double getPsmConfidence() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        double confidence = 100.0 * (1 - psmProbability);
        if (confidence <= 0) {
            confidence = 0;
        }
        return confidence;
    }

    /**
     * Returns the validation level of the match.
     *
     * @return the validation level of the match
     */
    public MatchValidationLevel getMatchValidationLevel() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return matchValidationLevel;
    }

    /**
     * Sets the validation level of the match.
     *
     * @param matchValidationLevel the validation level of the match
     */
    public void setMatchValidationLevel(MatchValidationLevel matchValidationLevel) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.matchValidationLevel = matchValidationLevel;
    }

    /**
     * Hide/Unhide a match.
     *
     * @param hidden boolean indicating whether the match should be hidden
     */
    public void setHidden(boolean hidden) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.hidden = hidden;
    }

    /**
     * Returns whether a match is hidden or not.
     *
     * @return boolean indicating whether a match is hidden or not
     */
    public boolean getHidden() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return hidden;
    }

    /**
     * Star/Unstar a match.
     *
     * @param starred boolean indicating whether the match should be starred
     */
    public void setStarred(boolean starred) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.starred = starred;
    }

    /**
     * Returns whether a match is starred or not.
     *
     * @return boolean indicating whether a match is starred or not
     */
    public boolean getStarred() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return starred;
    }

    /**
     * Returns the protein inference class of the protein match.
     *
     * @return the protein inference class of the protein match.
     */
    public int getProteinInferenceGroupClass() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinInferenceGroupClass;
    }

    /**
     * Returns the protein inference class as a string for the given
     * integer-based class
     *
     * @return the group class description
     */
    public String getProteinInferenceClassAsString() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return getProteinInferenceClassAsString(proteinInferenceGroupClass);
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
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.proteinInferenceGroupClass = groupClass;
    }

    /**
     * Returns the match key in the corresponding specific map.
     *
     * @return the match key in the corresponding specific map
     */
    public String getSpecificMapKey() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return specificMapKey;
    }
    
    public HashMap<String, Integer> getValidatedPeptidesPerFraction(){
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return validatedPeptidesPerFraction;
    }
    
    public HashMap<String, Integer> getValidatedSpectraPerFraction(){
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return validatedSpectraPerFraction;
    }

    /**
     * Sets the match key in the corresponding specific map.
     *
     * @param specificMapKey the match key in the corresponding specific map
     */
    public void setSpecificMapKey(String specificMapKey) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.specificMapKey = specificMapKey;
    }

    /**
     * Sets the fraction confidence.
     *
     * @param fraction the fraction
     * @param confidence the confidence
     */
    public void setFractionScore(String fraction, Double confidence) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        if (fractionScore == null) {
            fractionScore = new HashMap<>(2);
        }
        fractionScore.put(fraction, confidence);
    }
    
    public void setFractionScore(HashMap<String, Double> fractionScore){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.fractionScore = fractionScore;
    }

    /**
     * Returns the fraction score. Null if not found.
     *
     * @param fraction the fraction
     * @return the fraction score
     */
    public Double getFractionScore(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (fractionScore == null) {
            return null;
        }
        return fractionScore.get(fraction);
    }

    /**
     * Return the fractions where this match was found. Null if not found.
     *
     * @return the fractions where this match was found
     */
    public Set<String> getFractions(){
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (fractionScore != null) {
            return fractionScore.keySet();
        } else {
            return null;
        }
    }

    /**
     * Return the fractions where this match was found. Null if not found.
     *
     * @return the fractions where this match was found
     */
    public HashMap<String, Double> getFractionScore() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return fractionScore;
    }

    /**
     * Sets the fraction confidence.
     *
     * @param fraction the fraction
     * @param confidence the confidence
     */
    public void setFractionPEP(String fraction, Double confidence) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        if (fractionPEP == null) {
            fractionPEP = new HashMap<>(2);
        }
        fractionPEP.put(fraction, confidence);
    }
    
    public void setFractionPEP(HashMap<String, Double> fractionPEP){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.fractionPEP = fractionPEP;
    }

    /**
     * Returns the fraction pep. null if not found.
     *
     * @param fraction the fraction
     * @return the fraction pep
     */
    public Double getFractionPEP(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (fractionPEP == null) {
            return null;
        }
        return fractionPEP.get(fraction);
    }
    
    public HashMap<String, Double> getFractionPEP(){
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return fractionPEP;
    }

    /**
     * Returns the fraction confidence.
     *
     * @param fraction the fraction
     * @return the fraction confidence
     */
    public Double getFractionConfidence(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (fractionPEP == null || fractionPEP.get(fraction) == null) {
            return null;
        }
        return 100 * (1 - fractionPEP.get(fraction));
    }

    /**
     * Get the number of validated peptides in the given fraction.
     *
     * @param fraction the fraction
     * @return the number of validated peptides in the given fraction
     */
    public Integer getFractionValidatedPeptides(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (validatedPeptidesPerFraction != null) {
            return validatedPeptidesPerFraction.get(fraction);
        } else {
            return 0;
        }
    }

    /**
     * Get the number of validated peptides in the given fraction.
     *
     * @param validatedPeptidesPerFraction the validated peptides per fraction
     * map
     */
    public void setValidatedPeptidesPerFraction(HashMap<String, Integer> validatedPeptidesPerFraction) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.validatedPeptidesPerFraction = validatedPeptidesPerFraction;
    }

    /**
     * Get the number of validated spectra in the given fraction.
     *
     * @param fraction the fraction
     * @return the number of validated spectra in the given fraction
     */
    public Integer getFractionValidatedSpectra(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (validatedSpectraPerFraction != null) {
            return validatedSpectraPerFraction.get(fraction);
        } else {
            return 0;
        }
    }

    /**
     * Get the number of validated spectra in the given fraction.
     *
     * @param validatedSpectraPerFraction the validated spectra per fraction map
     */
    public void setValidatedSpectraPepFraction(HashMap<String, Integer> validatedSpectraPerFraction) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.validatedSpectraPerFraction = validatedSpectraPerFraction;
    }

    /**
     * Get the precursor intensity in the given fraction.
     *
     * @param fraction the fraction
     * @return the precursor intensity in the given fraction
     */
    public ArrayList<Double> getPrecursorIntensityPerFraction(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (precursorIntensityPerFraction != null) {
            return precursorIntensityPerFraction.get(fraction);
        } else {
            return new ArrayList<>();
        }
    }
    
    public HashMap<String, ArrayList<Double>> getPrecursorIntensityPerFraction() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return precursorIntensityPerFraction;
    }
    
    
    public void setPrecursorIntensityAveragePerFraction(HashMap<String, Double> precursorIntensityAveragePerFraction) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.precursorIntensityAveragePerFraction = precursorIntensityAveragePerFraction;
    }
    
    
    public void setPrecursorIntensitySummedPerFraction(HashMap<String, Double> precursorIntensitySummedPerFraction) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.precursorIntensitySummedPerFraction = precursorIntensitySummedPerFraction;
    }

    /**
     * Get the precursor intensity in the given fraction.
     *
     * @param precursorIntensityPerFraction the precursor intensities per fraction map
     */
    public void setPrecursorIntensityPerFraction(HashMap<String, ArrayList<Double>> precursorIntensityPerFraction) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.precursorIntensityPerFraction = precursorIntensityPerFraction;

        // calculate the average precursor intensities
        for (String fraction : precursorIntensityPerFraction.keySet()) {

            Double sum = 0.0;

            for (Double intensity : precursorIntensityPerFraction.get(fraction)) {
                sum += intensity;
            }

            if (precursorIntensitySummedPerFraction != null) { //@TODO: is always null?
                precursorIntensitySummedPerFraction.put(fraction, sum);
            }

            if (precursorIntensityAveragePerFraction == null) {
                precursorIntensityAveragePerFraction = new HashMap<>(2);
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
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (precursorIntensityAveragePerFraction != null) {
            return precursorIntensityAveragePerFraction.get(fraction);
        } else {
            return null;
        }
    }
    
    public HashMap<String, Double> getPrecursorIntensityAveragePerFraction() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return precursorIntensityAveragePerFraction;
    }

    /**
     * Get the summed precursor intensity in the given fraction.
     *
     * @param fraction the fraction
     * @return the summed precursor intensity in the given fraction
     */
    public Double getPrecursorIntensitySummedPerFraction(String fraction) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (precursorIntensitySummedPerFraction != null) {
            return precursorIntensitySummedPerFraction.get(fraction);
        } else {
            return null;
        }
    }
    
    public HashMap<String, Double> getPrecursorIntensitySummedPerFraction() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return precursorIntensitySummedPerFraction;
    }
    
    /**
     * Indicates whether the match validation was manually inspected.
     *
     * @return a boolean indicating whether the match validation was manually
     * inspected
     */
    public Boolean getManualValidation() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (manualValidation == null) {
            manualValidation = false;
        }
        return manualValidation;
    }

    /**
     * Sets whether the match validation was manually inspected.
     *
     * @param manualValidation a boolean indicating whether the match validation
     * was manually inspected
     */
    public void setManualValidation(Boolean manualValidation) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.manualValidation = manualValidation;
    }

    /**
     * Sets whether the match passed a quality control check.
     *
     * @param criterion the QC criterion
     * @param validated boolean indicating whether the test was passed
     */
    public void setQcResult(String criterion, boolean validated) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        if (qcFilters == null) {
            qcFilters = new HashMap<>();
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
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (qcFilters == null) {
            return null;
        }
        return qcFilters.get(criterion);
    }

    /**
     * Returns the list of qc checks made for this match.
     *
     * @return the list of qc checks made for this match in a set
     */
    public Set<String> getQcCriteria() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (qcFilters == null) {
            return new HashSet<>();
        }
        return qcFilters.keySet();
    }
    
    public HashMap<String, Boolean> getQcFilters(){
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return qcFilters;
    }

    /**
     * Resets the results of the QC filters.
     */
    public void resetQcResults() {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        if (qcFilters == null) {
            qcFilters = new HashMap<>();
        }
        qcFilters.clear();
    }

    /**
     * Indicates whether QC filters were implemented for this match.
     *
     * @return a boolean indicating whether QC filters were implemented for this
     * match
     */
    public boolean hasQcFilters() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return qcFilters != null;
    }

    /**
     * Adds an intermediate score.
     *
     * @param scoreId the index of the score
     * @param score the value of the score
     */
    public void setIntermediateScore(Integer scoreId, Double score) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        if (intermediateScores == null) {
            createIntermediateScoreMap();
        }
        intermediateScores.put(scoreId, score);
    }
    
    public void setIntermediateScores(HashMap<Integer, Double> intermediateScores){
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.intermediateScores = intermediateScores;
    }
    
    /**
     * Instantiates the intermediate scores map if null.
     */
    public synchronized void createIntermediateScoreMap() {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        if (intermediateScores == null) {
            intermediateScores = new HashMap<>();
        }
    }

    /**
     * Returns the desired intermediate score. Null if not found.
     *
     * @param scoreId the index of the score
     *
     * @return the intermediate score
     */
    public Double getIntermediateScore(int scoreId) {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        if (intermediateScores == null) {
            return null;
        }
        return intermediateScores.get(scoreId);
    }
    
    public HashMap<Integer, Double> getIntermediateScores() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return intermediateScores;
    }
        
    
    /**
     * Returns a score from a raw score where the score = -10*log(rawScore). The maximum score is 100 and raw scores smaller or equal to zero have a score of 100.
     * 
     * @param rawScore the raw score
     * 
     * @return the score
     */
    public static Double getScore(Double rawScore) {
        double score;
        if (rawScore <= 0) {
            score = 100;
        } else {
            score = -10 * FastMath.log10(rawScore);
            if (score > 100) {
                score = 100;
            }
        }
        return score;
    }

    @Override
    public long getParameterKey() {
        
        return serialVersionUID;
        
}
}
