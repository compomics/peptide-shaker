package eu.isas.peptideshaker.scoring;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
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
     * Map of PSM maps.
     */
    private HashMap<String, HashMap<Integer, TargetDecoyMap>> psmMaps = new HashMap<String, HashMap<Integer, TargetDecoyMap>>();
    /**
     * Map used to group charges together in order to ensure statistical
     * relevance.
     */
    private HashMap<String, HashMap<Integer, Integer>> grouping = new HashMap<String, HashMap<Integer, Integer>>();

    /**
     * Constructor.
     */
    public PsmPTMMap() {
    }

    /**
     * Estimate the posterior error probabilities of the psm ptms.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {
        int max = getMapsSize();
        waitingHandler.setSecondaryProgressDialogIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressValue(max);

        for (String modification : psmMaps.keySet()) {
            HashMap<Integer, TargetDecoyMap> map = psmMaps.get(modification);
            for (Integer charge : map.keySet()) {
                waitingHandler.increaseSecondaryProgressValue();
                if (!grouping.get(modification).containsKey(charge)) {
                    map.get(charge).estimateProbabilities(waitingHandler);
                }
            }
        }
        waitingHandler.setSecondaryProgressDialogIndeterminate(true);
    }

    /**
     * Returns the probability of the given modification at the given charge and
     * A-score.
     *
     * @param modification the modification
     * @param specificKey the charge of the match of interest
     * @param score the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(String modification, Integer specificKey, double score) {
        int key = getCorrectedKey(modification, specificKey);
        return psmMaps.get(modification).get(key).getProbability(score);
    }

    /**
     * Returns the probability of the given modification at the given charge and
     * A-score.
     *
     * @param modification the modification
     * @param specificKey the charge of the match
     * @param score the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(String modification, String specificKey, double score) {
        Integer keeyAsInteger;
        try {
            keeyAsInteger = new Integer(specificKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("PSM maps are indexed by charge. Input: " + specificKey);
        }
        return getProbability(modification, keeyAsInteger, score);
    }

    /**
     * Adds a point representing the modifications of the corresponding spectrum
     * match at a given score.
     *
     * @param modification the modification
     * @param probabilityScore the estimated score
     * @param spectrumMatch the spectrum match of interest
     */
    public void addPoint(String modification, double probabilityScore, SpectrumMatch spectrumMatch) {
        if (!psmMaps.containsKey(modification)) {
            psmMaps.put(modification, new HashMap<Integer, TargetDecoyMap>());
        }
        int key = getKey(spectrumMatch);
        if (!psmMaps.get(modification).containsKey(key)) {
            psmMaps.get(modification).put(key, new TargetDecoyMap());
        }
        PSPtmScores ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
        PtmScoring ptmScoring = ptmScores.getPtmScoring(modification);
        psmMaps.get(modification).get(key).put(probabilityScore, false);
        if (ptmScoring.isConflict()) {
            psmMaps.get(modification).get(key).put(probabilityScore, ptmScoring.isConflict());
        }
    }

    /**
     * Returns a map of keys from maps presenting a suspicious input.
     * modification -> charge.
     *
     * @return a list of keys from maps presenting a suspicious input
     */
    public HashMap<String, String> suspiciousInput() {
        HashMap<String, String> result = new HashMap<String, String>();
        for (String modification : psmMaps.keySet()) {
            for (Integer key : psmMaps.get(modification).keySet()) {
                if (psmMaps.get(modification).get(key).suspiciousInput() && !grouping.get(modification).containsKey(key)) {
                    result.put(modification, getGroupKey(modification, key));
                }
            }
        }
        return result;
    }

    /**
     * This method groups the statistically non significant psms with the ones
     * having a charge directly smaller.
     */
    public void cure() {
        for (String modification : psmMaps.keySet()) {
            ArrayList<Integer> charges = new ArrayList(psmMaps.get(modification).keySet());
            Collections.sort(charges);
            int ref = 0;
            for (int charge : charges) {
                if (psmMaps.get(modification).get(charge).getnMax() >= 100 && psmMaps.get(modification).get(charge).getnTargetOnly() >= 100) {
                    ref = charge;
                } else if (ref == 0) {
                    ref = charge;
                } else {
                    if (!grouping.containsKey(modification)) {
                        grouping.put(modification, new HashMap<Integer, Integer>());
                    }
                    grouping.get(modification).put(charge, ref);
                }
            }
            if (grouping.containsKey(modification)) {
                for (int charge : grouping.get(modification).keySet()) {
                    ref = grouping.get(modification).get(charge);
                    psmMaps.get(modification).get(ref).addAll(psmMaps.get(modification).get(charge));
                }
            }
        }
    }

    /**
     * Returns a map of the keys: charge -> group name for the given
     * modification.
     *
     * @param modification the modification of interest
     * @return a map of the keys: charge -> group name
     */
    public HashMap<Integer, String> getKeys(String modification) {
        HashMap<Integer, String> result = new HashMap<Integer, String>();
        for (int key : psmMaps.get(modification).keySet()) {
            if (!grouping.get(modification).containsKey(key)) {
                result.put(key, getGroupKey(modification, key));
            }
        }
        return result;
    }

    /**
     * Return a key of the selected charge group indexed by the main charge for
     * the given modification.
     *
     * @param modification the modification of interest
     * @param mainCharge the selected charge
     * @return key of the corresponding charge group
     */
    private String getGroupKey(String modification, Integer mainCharge) {
        String tempKey = mainCharge + "";
        for (int mergedKey : grouping.get(modification).keySet()) {
            if (grouping.get(modification).get(mergedKey) == mainCharge) {
                tempKey += ", " + mergedKey;
            }
        }
        return tempKey;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match after curation for the given modification.
     *
     * @param modification the modification of interest
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(String modification, int specificKey) {
        if (grouping.get(modification).containsKey(specificKey)) {
            return grouping.get(modification).get(specificKey);
        }
        return specificKey;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding
     * spectrum match after curation.
     *
     * @param modification the modification of interest
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(String modification, String specificKey) {
        Integer keeyAsInteger;
        try {
            keeyAsInteger = new Integer(specificKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("PSM maps are indexed by charge. Input: " + specificKey);
        }
        return getCorrectedKey(modification, keeyAsInteger);
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
            return spectrumMatch.getBestAssumption().getIdentificationCharge().value;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns the desired target decoy map. Here a decoy indicates a PSM
     * localization conflict.
     *
     * @param modification the name of the modification
     * @param key the key of the desired map
     * @return the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(String modification, int key) {
        return psmMaps.get(modification).get(key);
    }

    /**
     * Returns the overall number of points across all maps.
     *
     * @return the overall number of points across all maps.
     */
    public int getMapsSize() {
        int result = 0;
        for (String modification : psmMaps.keySet()) {
            for (TargetDecoyMap targetDecoyMap : psmMaps.get(modification).values()) {
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
    public ArrayList<String> getModificationsScored() {
        return new ArrayList<String>(psmMaps.keySet());
    }
}
