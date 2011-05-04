package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumCollection;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This map will store target decoy informations about the psms grouped according to their precursor charge.
 *
 * @author Marc
 */
public class PsmSpecificMap implements Serializable {

    /**
     * The map of the psm target/decoy maps indexed by the psm charge
     */
    private HashMap<Integer, TargetDecoyMap> psmsMaps = new HashMap<Integer, TargetDecoyMap>();
    /**
     * Map used to group charges together in order to ensure statistical relevance
     */
    private HashMap<Integer, Integer> grouping = new HashMap<Integer, Integer>();
    /**
     * The spectrum collection where spectra are stored
     */
    private SpectrumCollection spectrumCollection;

    /**
     * Constructor
     * @param spectrumCollection the spectrum collection where spectra are stored
     */
    public PsmSpecificMap(SpectrumCollection spectrumCollection) {
        this.spectrumCollection = spectrumCollection;
    }

    /**
     * estimate the posterior error probabilities of the psms
     */
    public void estimateProbabilities() {
        for (int charge : psmsMaps.keySet()) {
            if (!grouping.keySet().contains(charge)) {
                psmsMaps.get(charge).estimateProbabilities();
            }
        }
    }

    /**
     * Returns the probability of the given spectrum match at the given score
     * @param spectrumMatch the spectrum match of interest
     * @param score         the corresponding score
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(SpectrumMatch spectrumMatch, double score) {
        int key = getKey(spectrumMatch);
        if (grouping.containsKey(key)) {
            key = grouping.get(key);
        }
        return psmsMaps.get(key).getProbability(score);
    }

    /**
     * Adds a point representing the corresponding spectrum match at a given score
     * @param probabilityScore the estimated score
     * @param spectrumMatch    the spectrum match of interest
     */
    public void addPoint(double probabilityScore, SpectrumMatch spectrumMatch) {
        int key = getKey(spectrumMatch);
        if (!psmsMaps.containsKey(key)) {
            psmsMaps.put(key, new TargetDecoyMap());
        }
        psmsMaps.get(key).put(probabilityScore, spectrumMatch.getBestAssumption().isDecoy());
    }

    /**
     * Returns a list of keys from maps presenting a suspicious input
     * @return a list of keys from maps presenting a suspicious input
     */
    public ArrayList<String> suspiciousInput() {
        ArrayList<String> result = new ArrayList<String>();
        for (Integer key : psmsMaps.keySet()) {
            if (psmsMaps.get(key).suspiciousInput() && !grouping.containsKey(key)) {
                result.add(getGroupKey(key));
            }
        }
        return result;
    }

    /**
     * This method groups the statistically non significant psms with the ones having a charge directly smaller
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
     * Returns a map of the keys: charge -> group name
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
     * Return a key of the selected charge group indexed by the main charge
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
     * Returns the key (here the charge) associated to the corresponding spectrum match after curation
     * @param spectrumMatch the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(SpectrumMatch spectrumMatch) {
        Integer key = getKey(spectrumMatch);
        if (grouping.containsKey(key)) {
            return grouping.get(key);
        }
        return key;
    }

    /**
     * Returns the key (here the charge) associated to the corresponding spectrum match
     * @param spectrumMatch the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getKey(SpectrumMatch spectrumMatch) {
        try {
            return ((MSnSpectrum) spectrumCollection.getSpectrum(2, spectrumMatch.getKey())).getPrecursor().getCharge().value;
        } catch (Exception e) {
            // At this point no mzML file should be loaded
            return 0;
        }
    }

    /**
     * Returns the desired target decoy map
     * @param key   the key of the desired map
     * @return      the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(int key) {
        return psmsMaps.get(key);
    }
}
