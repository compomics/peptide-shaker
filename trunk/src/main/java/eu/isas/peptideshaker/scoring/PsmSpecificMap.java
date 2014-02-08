package eu.isas.peptideshaker.scoring;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.PsmFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.RowFilter;

/**
 * This map will store target decoy informations about the psms grouped
 * according to their precursor charge.
 *
 * @author Marc Vaudel
 */
public class PsmSpecificMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 746516685643358198L;
    /**
     * The map of the psm target/decoy maps indexed by the psm charge.
     */
    private HashMap<Integer, TargetDecoyMap> psmsMaps = new HashMap<Integer, TargetDecoyMap>();
    /**
     * Map used to group charges together in order to ensure statistical.
     * relevance
     */
    private HashMap<Integer, Integer> grouping = new HashMap<Integer, Integer>();
    /**
     * The filters to use to flag doubtful matches.
     */
    private ArrayList<PsmFilter> doubtfulMatchesFilters = getDefaultPsmFilters();

    /**
     * Constructor.
     */
    public PsmSpecificMap() {
    }

    /**
     * Returns the filters used to flag doubtful matches.
     * 
     * @return the filters used to flag doubtful matches
     */
    public ArrayList<PsmFilter> getDoubtfulMatchesFilters() {
        if (doubtfulMatchesFilters == null) { // Backward compatibility check for projects without filters
            doubtfulMatchesFilters = new ArrayList<PsmFilter>();
        }
        return doubtfulMatchesFilters;
    }

    /**
     * Sets the filters used to flag doubtful matches.
     * 
     * @param doubtfulMatchesFilters the filters used to flag doubtful matches
     */
    public void setDoubtfulMatchesFilters(ArrayList<PsmFilter> doubtfulMatchesFilters) {
        this.doubtfulMatchesFilters = doubtfulMatchesFilters;
    }
    
    /**
     * Adds a PSM filter to the list of doubtful matches filters.
     * 
     * @param psmFilter the new filter to add
     */
    public void addDoubtfulMatchesFilter(PsmFilter psmFilter) {
        this.doubtfulMatchesFilters.add(psmFilter);
    }

    /**
     * Estimate the posterior error probabilities of the PSMs.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        int max = getMapsSize();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        for (Integer charge : psmsMaps.keySet()) {

            waitingHandler.increaseSecondaryProgressCounter();

            if (!grouping.containsKey(charge)) {
                psmsMaps.get(charge).estimateProbabilities(waitingHandler);
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Returns the probability of the given spectrum match at the given score.
     *
     * @param specificKey the charge of the match of interest
     * @param score the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(int specificKey, double score) {
        int key = getCorrectedKey(specificKey);
        return psmsMaps.get(key).getProbability(score);
    }

    /**
     * Returns the probability of the given spectrum match at the given score.
     *
     * @param specificKey the charge of the match
     * @param score the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(String specificKey, double score) {
        Integer keeyAsInteger;
        try {
            keeyAsInteger = new Integer(specificKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("PSM maps are indexed by charge. Input: " + specificKey);
        }
        return getProbability(keeyAsInteger, score);
    }

    /**
     * Adds a point representing the corresponding spectrum match at a given
     * score.
     *
     * @param probabilityScore the estimated score
     * @param spectrumMatch the spectrum match of interest
     */
    public void addPoint(double probabilityScore, SpectrumMatch spectrumMatch) {
        int key = getKey(spectrumMatch);
        if (!psmsMaps.containsKey(key)) {
            psmsMaps.put(key, new TargetDecoyMap());
        }
        psmsMaps.get(key).put(probabilityScore, spectrumMatch.getBestPeptideAssumption().getPeptide().isDecoy());
    }

    /**
     * Returns a list of keys from maps presenting a suspicious input.
     *
     * @return a list of keys from maps presenting a suspicious input
     */
    public ArrayList<String> suspiciousInput() {
        ArrayList<String> result = new ArrayList<String>();
        for (Integer key : psmsMaps.keySet()) {
            if (!grouping.containsKey(key) && psmsMaps.get(key).suspiciousInput() && !grouping.containsKey(key)) {
                result.add(getGroupKey(key));
            }
        }
        return result;
    }

    /**
     * This method groups the statistically non significant psms with the ones
     * having a charge directly smaller.
     */
    public void cure() {
        ArrayList<Integer> charges = new ArrayList(psmsMaps.keySet());
        Collections.sort(charges);
        int ref = 0;
        for (int charge : charges) {
            if (psmsMaps.get(charge).getnMax() >= 100 && psmsMaps.get(charge).getnTargetOnly() >= 100) {
                ref = charge;
            } else if (ref == 0) {
                ref = charge;
            } else {
                grouping.put(charge, ref);
            }
        }
        for (int charge : grouping.keySet()) {
            ref = grouping.get(charge);
            psmsMaps.get(ref).addAll(psmsMaps.get(charge));
        }
    }

    /**
     * Returns a map of the keys: charge -> group name.
     *
     * @return a map of the keys: charge -> group name
     */
    public HashMap<Integer, String> getKeys() {
        HashMap<Integer, String> result = new HashMap<Integer, String>();
        for (int key : psmsMaps.keySet()) {
            if (!grouping.containsKey(key)) {
                result.put(key, getGroupKey(key));
            }
        }
        return result;
    }

    /**
     * Return a key of the selected charge group indexed by the main charge.
     *
     * @param mainCharge the selected charge
     * @return key of the corresponding charge group
     */
    private String getGroupKey(Integer mainCharge) {
        String tempKey = mainCharge + "";
        for (int mergedKey : grouping.keySet()) {
            if (grouping.get(mergedKey) == mainCharge) {
                tempKey += ", " + mergedKey;
            }
        }
        return tempKey;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match after curation.
     *
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(int specificKey) {
        if (grouping.containsKey(specificKey)) {
            return grouping.get(specificKey);
        }
        return specificKey;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match after curation.
     *
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(String specificKey) {
        Integer keeyAsInteger;
        try {
            keeyAsInteger = new Integer(specificKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("PSM maps are indexed by charge. Input: " + specificKey);
        }
        return getCorrectedKey(keeyAsInteger);
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match.
     *
     * @param spectrumMatch the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getKey(SpectrumMatch spectrumMatch) {
        try {
            return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns the desired target decoy map.
     *
     * @param key the key of the desired map
     * @return the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(int key) {
        return psmsMaps.get(key);
    }

    /**
     * Returns the overall number of points across all maps.
     *
     * @return the overall number of points across all maps.
     */
    public int getMapsSize() {
        int result = 0;
        for (TargetDecoyMap targetDecoyMap : psmsMaps.values()) {
            result += targetDecoyMap.getMapSize();
        }
        return result;
    }

    /**
     * Returns the maximal precursor charge observed in the identified spectra.
     *
     * @return the maximal precursor charge observed in the identified spectra
     */
    public int getMaxCharge() {
        int maxCharge = 0;
        for (int charge : psmsMaps.keySet()) {
            if (charge > maxCharge) {
                maxCharge = charge;
            }
        }
        return maxCharge;
    }
    
    /**
     * Returns the default filters for setting a match as doubtful.
     * 
     * @return the default filters for setting a match as doubtful
     */
    public static ArrayList<PsmFilter> getDefaultPsmFilters() {
        ArrayList<PsmFilter> filters = new ArrayList<PsmFilter>();
        
        PsmFilter psmFilter = new PsmFilter(">40% Fragment Ion Sequence Coverage");
        psmFilter.setDescription(">40% sequence coverage by fragment ions");
        psmFilter.setSequenceCoverage(40.0);
        psmFilter.setSequenceCoverageComparison(RowFilter.ComparisonType.AFTER);
//        filters.add(psmFilter);
        
        return filters;
    }
}
