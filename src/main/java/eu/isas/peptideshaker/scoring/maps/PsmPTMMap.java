package eu.isas.peptideshaker.scoring.maps;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class is used for the estimation of the FLR.
 *
 * @author Marc Vaudel
 */
public class PsmPTMMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -2287016074184132951L;
    /**
     * Map of PSM maps.
     */
    private HashMap<Double, HashMap<Integer, TargetDecoyMap>> psmMaps = new HashMap<>();
    /**
     * Map used to group charges together in order to ensure statistical
     * relevance.
     */
    private HashMap<Double, HashMap<Integer, Integer>> grouping = new HashMap<>();

    /**
     * Constructor.
     */
    public PsmPTMMap() {
    }

    /**
     * Estimate the posterior error probabilities of the PSM PTMs.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        int max = getMapsSize();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        for (Double ptmMass : psmMaps.keySet()) {
            HashMap<Integer, TargetDecoyMap> map = psmMaps.get(ptmMass);
            for (Integer charge : map.keySet()) {
                waitingHandler.increaseSecondaryProgressCounter();
                if (!grouping.get(ptmMass).containsKey(charge)) {
                    map.get(charge).estimateProbabilities(waitingHandler);
                }
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Returns the probability of the given modification at the given charge and
     * A-score.
     *
     * @param ptmMass the modification
     * @param specificKey the charge of the match of interest
     * @param score the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(Double ptmMass, Integer specificKey, double score) {
        int key = getCorrectedKey(ptmMass, specificKey);
        return psmMaps.get(ptmMass).get(key).getProbability(score);
    }

    /**
     * Returns the probability of the given modification at the given charge and
     * A-score.
     *
     * @param ptmMass the modification
     * @param specificKey the charge of the match
     * @param score the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(Double ptmMass, String specificKey, double score) {
        Integer keyAsInteger;
        try {
            keyAsInteger = new Integer(specificKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("PSM maps are indexed by charge. Input: " + specificKey);
        }
        return getProbability(ptmMass, keyAsInteger, score);
    }

    /**
     * Adds a point representing the modifications of the corresponding spectrum
     * match at a given score.
     *
     * @param ptmMass the mass of the modification
     * @param probabilityScore the estimated score
     * @param spectrumMatch the spectrum match of interest
     * @param conflict boolean indicating whether the two scores are conflicting
     */
    public void addPoint(double ptmMass, double probabilityScore, SpectrumMatch spectrumMatch, boolean conflict) {
        HashMap<Integer, TargetDecoyMap> psmMap = psmMaps.get(ptmMass);
        HashMap<Integer, Integer> psmGrouping = grouping.get(ptmMass);
        if (psmMap == null) {
            psmMap = new HashMap<>(4);
            psmMaps.put(ptmMass, psmMap);
        }
        if (psmGrouping == null) {
            psmGrouping = new HashMap<>(4);
            grouping.put(ptmMass, psmGrouping);
        }
        int key = getKey(spectrumMatch);
        TargetDecoyMap targetDecoyMap = psmMap.get(key);
        if (targetDecoyMap == null) {
            targetDecoyMap = new TargetDecoyMap();
            psmMap.put(key, targetDecoyMap);
        }
        targetDecoyMap.put(probabilityScore, conflict);
    }

    /**
     * Returns a map of keys from maps presenting a suspicious input.
     * modification mass &gt; charge.
     * 
     * @param minimalFDR the minimal FDR which should be achievable
     *
     * @return a list of keys from maps presenting a suspicious input
     */
    public HashMap<Double, String> suspiciousInput(Double minimalFDR) {
        HashMap<Double, String> result = new HashMap<>();
        for (double ptmMass : psmMaps.keySet()) {
            for (Integer key : psmMaps.get(ptmMass).keySet()) {
                TargetDecoyMap targetDecoyMap = psmMaps.get(ptmMass).get(key);
                if (targetDecoyMap.suspiciousInput(minimalFDR) && !grouping.get(ptmMass).containsKey(key)) {
                    result.put(ptmMass, getGroupKey(ptmMass, key));
                }
            }
        }
        return result;
    }

    /**
     * This method groups the statistically non significant PSMs with the ones
     * having a charge directly smaller.
     * 
     * @param minimalFDR the minimal FDR which should be achievable
     */
    public void clean(Double minimalFDR) {

        for (double ptmMass : psmMaps.keySet()) {

            ArrayList<Integer> charges = new ArrayList(psmMaps.get(ptmMass).keySet());
            Collections.sort(charges);
            int ref = 0;

            for (int charge : charges) {
                TargetDecoyMap targetDecoyMap = psmMaps.get(ptmMass).get(charge);
                if (targetDecoyMap.suspiciousInput(minimalFDR)) {
                    ref = charge;
                } else if (ref == 0) {
                    ref = charge;
                } else {
                    if (!grouping.containsKey(ptmMass)) {
                        grouping.put(ptmMass, new HashMap<>());
                    }
                    grouping.get(ptmMass).put(charge, ref);
                }
            }

            if (grouping.containsKey(ptmMass)) {
                for (int charge : grouping.get(ptmMass).keySet()) {
                    ref = grouping.get(ptmMass).get(charge);
                    psmMaps.get(ptmMass).get(ref).addAll(psmMaps.get(ptmMass).get(charge));
                }
            }
        }
    }

    /**
     * Returns a map of the keys: charge &gt; group name for the given
     * modification.
     *
     * @param ptmMass the modification mass of interest
     * @return a map of the keys: charge &gt; group name
     */
    public HashMap<Integer, String> getKeys(Double ptmMass) {
        HashMap<Integer, String> result = new HashMap<>();
        for (int key : psmMaps.get(ptmMass).keySet()) {
            if (!grouping.get(ptmMass).containsKey(key)) {
                result.put(key, getGroupKey(ptmMass, key));
            }
        }
        return result;
    }

    /**
     * Return a key of the selected charge group indexed by the main charge for
     * the given modification.
     *
     * @param ptmMass the modification mass of interest
     * @param mainCharge the selected charge
     * @return key of the corresponding charge group
     */
    private String getGroupKey(Double ptmMass, Integer mainCharge) {
        String tempKey = mainCharge + "";
        for (int mergedKey : grouping.get(ptmMass).keySet()) {
            if (grouping.get(ptmMass).get(mergedKey) == mainCharge) {
                tempKey += ", " + mergedKey;
            }
        }
        return tempKey;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match after curation for the given modification.
     *
     * @param ptmMass the modification mass of interest
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(Double ptmMass, int specificKey) {
        HashMap<Integer, Integer> groupingAtPtmMass = grouping.get(ptmMass);
        if (groupingAtPtmMass == null) {
            throw new IllegalArgumentException(ptmMass + " not present in the PSM PTM grouping mapping keys.");
        }
        Integer result = groupingAtPtmMass.get(specificKey);
        if (result == null) {
            return specificKey;
        }
        return result;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match after curation.
     *
     * @param ptmMass the modification mass of interest
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(Double ptmMass, String specificKey) {
        Integer keeyAsInteger;
        try {
            keeyAsInteger = new Integer(specificKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("PSM maps are indexed by charge. Input: " + specificKey);
        }
        return getCorrectedKey(ptmMass, keeyAsInteger);
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
     * Returns the desired target decoy map. Here a decoy indicates a PSM
     * localization conflict.
     *
     * @param ptmMass the modification mass of interest
     * @param specificKey the key of the desired map
     * @return the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(Double ptmMass, int specificKey) {
        int correctedKey = getCorrectedKey(ptmMass, specificKey);
        return psmMaps.get(ptmMass).get(correctedKey);
    }

    /**
     * Returns the overall number of points across all maps.
     *
     * @return the overall number of points across all maps.
     */
    public int getMapsSize() {
        int result = 0;
        for (Double ptmMass : psmMaps.keySet()) {
            for (TargetDecoyMap targetDecoyMap : psmMaps.get(ptmMass).values()) {
                result += targetDecoyMap.getMapSize();
            }
        }
        return result;
    }

    /**
     * Returns a list of all modifications loaded in the map.
     *
     * @return a list of all modifications loaded in the map
     */
    public ArrayList<Double> getModificationsScored() {
        return new ArrayList<>(psmMaps.keySet());
    }
}
