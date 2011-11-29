package eu.isas.peptideshaker.scoring;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This map will store target decoy informations about the psms grouped according to their precursor charge.
 *
 * @author Marc Vaudel
 */
public class PsmSpecificMap implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = 746516685643358198L;
    /**
     * The map of the psm target/decoy maps indexed by the psm charge
     */
    private HashMap<Integer, TargetDecoyMap> psmsMaps = new HashMap<Integer, TargetDecoyMap>();
    /**
     * Map used to group charges together in order to ensure statistical relevance
     */
    private HashMap<Integer, Integer> grouping = new HashMap<Integer, Integer>();

    /**
     * Constructor
     */
    public PsmSpecificMap() {
    }

    /**
     * Estimate the posterior error probabilities of the psms.
     * 
     * @param waitingDialog a reference to the waiting dialog
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        
        int max = getMapsSize();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);
        
        for (int charge : psmsMaps.keySet()) {
            
            waitingDialog.increaseSecondaryProgressValue();
            
            if (!grouping.keySet().contains(charge)) {
                psmsMaps.get(charge).estimateProbabilities(waitingDialog);
            }
        }
        
        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Returns the probability of the given spectrum match at the given score
     * @param specificKey   the spectrum match of interest
     * @param score         the corresponding score
     * @return              the probability of the given spectrum match at the given score
     */
    public double getProbability(int specificKey, double score) {
        specificKey = getCorrectedKey(specificKey);
        return psmsMaps.get(specificKey).getProbability(score);
    }

    /**
     * Returns the probability of the given spectrum match at the given score
     * @param specificKey   the spectrum match of interest
     * @param score         the corresponding score
     * @return              the probability of the given spectrum match at the given score
     */
    public double getProbability(String specificKey, double score) {
        return getProbability(new Integer(specificKey), score);
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
     * Returns the key (here the charge) associated to the corresponding spectrum match after curation.
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
     * Returns the key (here the charge) associated to the corresponding spectrum match after curation.
     * 
     * @param specificKey the spectrum match of interest
     * @return the corresponding key
     */
    public Integer getCorrectedKey(String specificKey) {
        return getCorrectedKey(new Integer(specificKey));
    }

    /**
     * Returns the key (here the charge) associated to the corresponding spectrum match.
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
     * Returns the desired target decoy map.
     * 
     * @param key   the key of the desired map
     * @return      the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(int key) {
        return psmsMaps.get(key);
    }
    
    /**
     * Returns the overall number of points in the map.
     * 
     * @return the overall number of points in the map
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
}
