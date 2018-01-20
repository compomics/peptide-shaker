package eu.isas.peptideshaker.parameters;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
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
     * The difference in identification algorithm level PEP with the next best
     * peptide assumption.
     */
    private double algorithmDeltaPEP;
    /**
     * The difference in identification algorithm level PEP with the next best
     * peptide assumption with sequence difference across all search engines.
     */
    private double deltaPEP;
    /**
     * The score of the match.
     */
    private double score;
    /**
     * The probability of the match.
     */
    private double probability;
    /**
     * The validation level of a given match.
     */
    private MatchValidationLevel matchValidationLevel;
    /**
     * Boolean indicating whether the validation confidence was manually
     * updated.
     */
    private boolean manualValidation = false;
    /**
     * Boolean indicating whether this is a hidden match.
     */
    private boolean hidden = false;
    /**
     * Boolean indicating whether this is a starred match.
     */
    private boolean starred = false;
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
     * Returns the match probability.
     *
     * @return the match probability
     */
    public double getProbability() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return probability;

    }

    /**
     * Set the probability.
     *
     * @param probability the new peptide posterior error probability
     */
    public void setProbability(double probability) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.probability = probability;

    }

    public void setGroupClass(int groupClass) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.proteinInferenceGroupClass = groupClass;

    }

    /**
     * Returns the score.
     *
     * @return the score
     */
    public double getScore() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return score;

    }
    
    /**
     * Returns the log transformed score.
     * 
     * @return the log transformed score
     */
    public double getTransformedScore() {
        
        return transformScore(getScore());
        
    }

    /**
     * Set the peptide score.
     *
     * @param score the score
     */
    public void setScore(double score) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.score = score;

    }

    /**
     * Returns the confidence.
     *
     * @return the confidence
     */
    public double getConfidence() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        double confidence = 100.0 * (1 - probability);

        return confidence < 0.0 ? 0.0 : confidence;

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

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

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

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

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.deltaPEP = deltaPEP;

    }

    /**
     * Sets the qc filters.
     *
     * @param qcFilters the qc filters
     */
    public void setQcFilters(HashMap<String, Boolean> qcFilters) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.qcFilters = qcFilters;

    }

    /**
     * Returns the validation level of the match.
     *
     * @return the validation level of the match
     */
    public MatchValidationLevel getMatchValidationLevel() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return matchValidationLevel;

    }

    /**
     * Sets the validation level of the match.
     *
     * @param matchValidationLevel the validation level of the match
     */
    public void setMatchValidationLevel(MatchValidationLevel matchValidationLevel) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.matchValidationLevel = matchValidationLevel;

    }

    /**
     * Hide/Unhide a match.
     *
     * @param hidden boolean indicating whether the match should be hidden
     */
    public void setHidden(boolean hidden) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.hidden = hidden;

    }

    /**
     * Returns whether a match is hidden or not.
     *
     * @return boolean indicating whether a match is hidden or not
     */
    public boolean getHidden() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return hidden;

    }

    /**
     * Star/Unstar a match.
     *
     * @param starred boolean indicating whether the match should be starred
     */
    public void setStarred(boolean starred) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.starred = starred;

    }

    /**
     * Returns whether a match is starred or not.
     *
     * @return boolean indicating whether a match is starred or not
     */
    public boolean getStarred() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return starred;

    }

    /**
     * Returns the protein inference class of the protein match.
     *
     * @return the protein inference class of the protein match.
     */
    public int getProteinInferenceGroupClass() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return proteinInferenceGroupClass;

    }

    /**
     * Returns the protein inference class as a string for the given
     * integer-based class
     *
     * @return the group class description
     */
    public String getProteinInferenceClassAsString() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return getProteinInferenceClassAsString(proteinInferenceGroupClass);

    }

    /**
     * Returns the protein inference class as a string for the given
     * integer-based class.
     *
     * @param matchClass the protein inference class as integer (see static
     * fields)
     *
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

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.proteinInferenceGroupClass = groupClass;

    }

    /**
     * Returns the number of validated peptides per fraction.
     *
     * @return the number of validated peptides per fraction
     */
    public HashMap<String, Integer> getValidatedPeptidesPerFraction() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return validatedPeptidesPerFraction;

    }

    /**
     * Returns the number of validated spectra per fraction.
     *
     * @return the number of validated spectra per fraction
     */
    public HashMap<String, Integer> getValidatedSpectraPerFraction() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return validatedSpectraPerFraction;
    }

    /**
     * Sets the fraction confidence.
     *
     * @param fraction the fraction
     * @param confidence the confidence
     */
    public void setFractionScore(String fraction, Double confidence) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (fractionScore == null) {

            fractionScore = new HashMap<>(2);

        }

        fractionScore.put(fraction, confidence);

    }

    /**
     * Sets the fraction score map.
     *
     * @param fractionScore the fraction score map
     */
    public void setFractionScore(HashMap<String, Double> fractionScore) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.fractionScore = fractionScore;

    }

    /**
     * Returns the fraction score. Null if not found.
     *
     * @param fraction the fraction
     * @return the fraction score
     */
    public Double getFractionScore(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

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
    public Set<String> getFractions() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return fractionScore == null ? null
                : fractionScore.keySet();

    }

    /**
     * Return the fractions where this match was found. Null if not found.
     *
     * @return the fractions where this match was found
     */
    public HashMap<String, Double> getFractionScore() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return fractionScore;

    }

    /**
     * Sets the fraction confidence.
     *
     * @param fraction the fraction
     * @param confidence the confidence
     */
    public void setFractionPEP(String fraction, Double confidence) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (fractionPEP == null) {

            fractionPEP = new HashMap<>(2);

        }

        fractionPEP.put(fraction, confidence);

    }

    public void setFractionPEP(HashMap<String, Double> fractionPEP) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.fractionPEP = fractionPEP;

    }

    /**
     * Returns the fraction pep. null if not found.
     *
     * @param fraction the fraction
     * @return the fraction pep
     */
    public Double getFractionPEP(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return fractionPEP == null ? null : fractionPEP.get(fraction);

    }

    /**
     * Returns the fraction pep map.
     *
     * @return the fraction pep map
     */
    public HashMap<String, Double> getFractionPEP() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return fractionPEP;

    }

    /**
     * Returns the fraction confidence.
     *
     * @param fraction the fraction
     *
     * @return the fraction confidence
     */
    public Double getFractionConfidence(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return fractionPEP == null || fractionPEP.get(fraction) == null ? null
                : 100 * (1 - fractionPEP.get(fraction));

    }

    /**
     * Get the number of validated peptides in the given fraction.
     *
     * @param fraction the fraction
     * @return the number of validated peptides in the given fraction
     */
    public Integer getFractionValidatedPeptides(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return validatedPeptidesPerFraction == null ? 0
                : validatedPeptidesPerFraction.get(fraction);

    }

    /**
     * Get the number of validated peptides in the given fraction.
     *
     * @param validatedPeptidesPerFraction the validated peptides per fraction
     * map
     */
    public void setValidatedPeptidesPerFraction(HashMap<String, Integer> validatedPeptidesPerFraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.validatedPeptidesPerFraction = validatedPeptidesPerFraction;

    }

    /**
     * Get the number of validated spectra in the given fraction.
     *
     * @param fraction the fraction
     * @return the number of validated spectra in the given fraction
     */
    public Integer getFractionValidatedSpectra(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return validatedSpectraPerFraction == null ? 0
                : validatedSpectraPerFraction.get(fraction);

    }

    /**
     * Get the number of validated spectra in the given fraction.
     *
     * @param validatedSpectraPerFraction the validated spectra per fraction map
     */
    public void setValidatedSpectraPepFraction(HashMap<String, Integer> validatedSpectraPerFraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.validatedSpectraPerFraction = validatedSpectraPerFraction;

    }

    /**
     * Get the precursor intensity in the given fraction.
     *
     * @param fraction the fraction
     * @return the precursor intensity in the given fraction
     */
    public ArrayList<Double> getPrecursorIntensityPerFraction(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return precursorIntensityPerFraction == null ? new ArrayList<>(0)
                : precursorIntensityPerFraction.get(fraction);

    }

    /**
     * Returns the precursor intensity per fraction map.
     *
     * @return the precursor intensity per fraction map
     */
    public HashMap<String, ArrayList<Double>> getPrecursorIntensityPerFraction() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return precursorIntensityPerFraction;

    }

    /**
     * Sets the precursor intensity per fraction map.
     *
     * @param precursorIntensityAveragePerFraction the precursor intensity per
     * fraction map
     */
    public void setPrecursorIntensityAveragePerFraction(HashMap<String, Double> precursorIntensityAveragePerFraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.precursorIntensityAveragePerFraction = precursorIntensityAveragePerFraction;

    }

    /**
     * Sets the summed precursor intensity per fraction map.
     *
     * @param precursorIntensitySummedPerFraction the summed precursor intensity
     * per fraction map
     */
    public void setPrecursorIntensitySummedPerFraction(HashMap<String, Double> precursorIntensitySummedPerFraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.precursorIntensitySummedPerFraction = precursorIntensitySummedPerFraction;

    }

    /**
     * Get the precursor intensity in the given fraction.
     *
     * @param precursorIntensityPerFraction the precursor intensities per
     * fraction map
     */
    public void setPrecursorIntensityPerFraction(HashMap<String, ArrayList<Double>> precursorIntensityPerFraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.precursorIntensityPerFraction = precursorIntensityPerFraction;

        for (Entry<String, ArrayList<Double>> entry : precursorIntensityPerFraction.entrySet()) {

            String fraction = entry.getKey();
            double sum = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            if (precursorIntensitySummedPerFraction != null) {

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return precursorIntensityAveragePerFraction == null ? null
                : precursorIntensityAveragePerFraction.get(fraction);

    }

    /**
     * Returns the fraction precursor intensity average map.
     *
     * @return the fraction precursor intensity average map
     */
    public HashMap<String, Double> getPrecursorIntensityAveragePerFraction() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return precursorIntensityAveragePerFraction;

    }

    /**
     * Get the summed precursor intensity in the given fraction.
     *
     * @param fraction the fraction
     * @return the summed precursor intensity in the given fraction
     */
    public Double getPrecursorIntensitySummedPerFraction(String fraction) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return precursorIntensitySummedPerFraction == null ? null
                : precursorIntensitySummedPerFraction.get(fraction);

    }

    /**
     * Returns the fraction summed intensity map.
     *
     * @return the fraction summed intensity map
     */
    public HashMap<String, Double> getPrecursorIntensitySummedPerFraction() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return precursorIntensitySummedPerFraction;

    }

    /**
     * Indicates whether the match validation was manually inspected.
     *
     * @return a boolean indicating whether the match validation was manually
     * inspected
     */
    public boolean getManualValidation() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return manualValidation;

    }

    /**
     * Sets whether the match validation was manually inspected.
     *
     * @param manualValidation a boolean indicating whether the match validation
     * was manually inspected
     */
    public void setManualValidation(boolean manualValidation) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.manualValidation = manualValidation;

    }

    /**
     * Sets whether the match passed a quality control check.
     *
     * @param criterion the QC criterion
     * @param validated boolean indicating whether the test was passed
     */
    public void setQcResult(String criterion, boolean validated) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (qcFilters == null) {

            qcFilters = new HashMap<>(1);

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return qcFilters == null ? null
                : qcFilters.get(criterion);

    }

    /**
     * Returns the list of qc checks made for this match.
     *
     * @return the list of qc checks made for this match in a set
     */
    public Set<String> getQcCriteria() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return qcFilters == null ? new HashSet<>(0)
                : qcFilters.keySet();
    }

    /**
     * Returns the qc filters map.
     *
     * @return the qc filters map
     */
    public HashMap<String, Boolean> getQcFilters() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return qcFilters;

    }

    /**
     * Resets the results of the QC filters.
     */
    public void resetQcResults() {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (qcFilters == null) {

            qcFilters = new HashMap<>(1);

        } else {

            qcFilters.clear();

        }
    }

    /**
     * Indicates whether QC filters were implemented for this match.
     *
     * @return a boolean indicating whether QC filters were implemented for this
     * match
     */
    public boolean hasQcFilters() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return qcFilters != null && !qcFilters.isEmpty();

    }

    /**
     * Adds an intermediate score.
     *
     * @param scoreId the index of the score
     * @param score the value of the score
     */
    public void setIntermediateScore(Integer scoreId, Double score) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (intermediateScores == null) {

            createIntermediateScoreMap();

        }

        intermediateScores.put(scoreId, score);

    }

    public void setIntermediateScores(HashMap<Integer, Double> intermediateScores) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        this.intermediateScores = intermediateScores;

    }

    /**
     * Instantiates the intermediate scores map if null.
     */
    public synchronized void createIntermediateScoreMap() {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        if (intermediateScores == null) {

            intermediateScores = new HashMap<>(1);

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return intermediateScores == null ? null :
                intermediateScores.get(scoreId);
        
    }

    /**
     * Returns the intermediate scores map.
     * 
     * @return the intermediate scores map
     */
    public HashMap<Integer, Double> getIntermediateScores() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return intermediateScores;

    }

    /**
     * Returns a score from a raw score where the score = -10*log(rawScore). The
     * maximum score is 100 and raw scores smaller or equal to zero have a score
     * of 100.
     *
     * @param rawScore the raw score
     *
     * @return the score
     */
    public static double transformScore(double rawScore) {

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
