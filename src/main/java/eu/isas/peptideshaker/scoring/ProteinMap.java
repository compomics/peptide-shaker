package eu.isas.peptideshaker.scoring;

import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.RowFilter;

/**
 * This map will be used to score protein matches and solve protein inference
 * problems
 *
 * @author Marc Vaudel
 */
public class ProteinMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -2438674334416191482L;
    /**
     * The protein target/decoy map.
     */
    private TargetDecoyMap proteinMatchMap = new TargetDecoyMap();
    /**
     * The filters to use to flag doubtful matches
     */
    private ArrayList<ProteinFilter> doubtfulMatchesFilters = getDefaultProteinFilters();

    /**
     * Constructor.
     */
    public ProteinMap() {
    }

    /**
     * Returns the filters used to flag doubtful matches.
     * 
     * @return the filters used to flag doubtful matches
     */
    public ArrayList<ProteinFilter> getDoubtfulMatchesFilters() {
        if (doubtfulMatchesFilters == null) { // Backward compatibility check for projects without filters
            doubtfulMatchesFilters = new ArrayList<ProteinFilter>();
        }
        return doubtfulMatchesFilters;
    }

    /**
     * Sets the filters used to flag doubtful matches.
     * 
     * @param doubtfulMatchesFilters the filters used to flag doubtful matches
     */
    public void setDoubtfulMatchesFilters(ArrayList<ProteinFilter> doubtfulMatchesFilters) {
        this.doubtfulMatchesFilters = doubtfulMatchesFilters;
    }

    /**
     * Estimate the posterior error probabilities.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        waitingHandler.setWaitingText("Estimating Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(proteinMatchMap.getMapSize());

        proteinMatchMap.estimateProbabilities(waitingHandler);

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Adds a point in the target/decoy map.
     *
     * @param probabilityScore The estimated protein probabilistic score
     * @param isDecoy a boolean indicating whether the protein is decoy
     */
    public void addPoint(double probabilityScore, boolean isDecoy) {
        proteinMatchMap.put(probabilityScore, isDecoy);
    }

    /**
     * Removes a point in the target/decoy map.
     *
     * @param probabilityScore The estimated protein probabilistic score
     * @param isDecoy a boolean indicating whether the protein is decoy
     */
    public void removePoint(double probabilityScore, boolean isDecoy) {
        proteinMatchMap.remove(probabilityScore, isDecoy);
    }

    /**
     * Returns the posterior error probability of a peptide match at the given
     * score.
     *
     * @param score the score of the match
     * @return the posterior error probability
     */
    public double getProbability(double score) {
        return proteinMatchMap.getProbability(score);
    }

    /**
     * Returns a boolean indicating if a suspicious input was detected.
     *
     * @return a boolean indicating if a suspicious input was detected
     */
    public boolean suspicousInput() {
        return proteinMatchMap.suspiciousInput();
    }

    /**
     * Returns the target decoy map.
     *
     * @return the target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap() {
        return proteinMatchMap;
    }
    
    /**
     * Returns the default filters for setting a match as doubtful.
     * 
     * @return the default filters for setting a match as doubtful
     */
    public static ArrayList<ProteinFilter> getDefaultProteinFilters() {
        ArrayList<ProteinFilter> filters = new ArrayList<ProteinFilter>();
        
        ProteinFilter proteinFilter = new ProteinFilter("n confident peptides");
        proteinFilter.setnConfidentPeptides(1);
        proteinFilter.setnConfidentPeptidesComparison(RowFilter.ComparisonType.AFTER);
        filters.add(proteinFilter);
        
        proteinFilter = new ProteinFilter("n confident spectra");
        proteinFilter.setProteinNConfidentSpectra(1);
        proteinFilter.setnConfidentSpectraComparison(RowFilter.ComparisonType.AFTER);
        filters.add(proteinFilter);
        
        return filters;
    }
}
