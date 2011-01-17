package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This map will store basic informations about the psms grouped according to their mass.
 *
 * @author Marc
 */
public class PsmSpecificMap implements Serializable {

    /**
     * The user specified FDR threshold
     */
    private double fdrThreshold;
    /**
     * The number of hits retained at the specified threshold
     */
    private int nHits;
    /**
     * The FDR estimated at the specified threshold
     */
    private double fdr;
    /**
     * The FNR estimated at the specified threshold
     */
    private double fnr;
    /**
     * The map of the psm target/decoy maps indexed by the psm charge
     */
    private HashMap<Integer, TargetDecoyMap> psmsMaps = new HashMap<Integer, TargetDecoyMap>();
    /**
     * Map used to group charges together in order to ensure statistical relevance
     */
    private HashMap<Integer, Integer> grouping = new HashMap<Integer, Integer>();

    /**
     * constructor
     */
    public PsmSpecificMap() {
    }

    /**
     * Returns the number of hits at the implemented threshold
     * @return the number of hits at the implemented threshold
     */
    public int getNHits() {
        return nHits;
    }

    /**
     * Returns the estimated FDR at the implemented threshold
     * @return the estimated FDR at the implemented threshold
     */
    public double getFdr() {
        return fdr;
    }

    /**
     * Returns the implemented threshold
     * @return the implemented threshold
     */
    public double getFdrThreshold() {
        return fdrThreshold;
    }

    /**
     * Returns the estimated FNR at the implemented threshold
     * @return Returns the estimated FNR at the implemented threshold
     */
    public double getFnr() {
        return fnr;
    }

    /**
     * Estimate the FDR, FNR, number of hits at a new threshold
     * @param newThreshold  the new threshold to implement
     */
    public void getResults(double newThreshold) {
        this.fdrThreshold = newThreshold;
        for (TargetDecoyMap targetDecoyMap : psmsMaps.values()) {
            targetDecoyMap.getResults(fdrThreshold);
        }
    }

    /**
     * estimate the posterior error probabilities of the psms
     * @param waitingDialog the waiting dialog will display feedback to the user
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        for (int charge : psmsMaps.keySet()) {
            if (!grouping.keySet().contains(charge)) {
                psmsMaps.get(charge).estimateProbabilities(waitingDialog);
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
            psmsMaps.put(key, new TargetDecoyMap("Charge " + key + " psms"));
        }
        psmsMaps.get(key).put(probabilityScore, spectrumMatch.getBestAssumption().isDecoy());
    }

    /**
     * Returns the score limit estimated for the implemented threshold for the selected spectrum match
     * @param spectrumMatch the spectrum match of interest
     * @return the score limit for this kind of spectrum match at the implemented threshold
     */
    public double getScoreLimit(SpectrumMatch spectrumMatch) {
        int key = getKey(spectrumMatch);
        if (grouping.containsKey(key)) {
            key = grouping.get(key);
        }
        return psmsMaps.get(key).getScoreLimit();
    }

    /**
     * This method groups the statistically non significant psms with the ones having a charge directly smaller
     * @param waitingDialog the waiting dialog which will display feedback to the user
     */
    public void cure(WaitingDialog waitingDialog) {
        ArrayList<Integer> charges = new ArrayList(psmsMaps.keySet());
        Collections.sort(charges);
        int ref = 0;
        for (int charge : charges) {
            if (psmsMaps.get(charge).getnMax() >= 100) {
                ref = charge;
                break;
            }
        }
        for (int charge : charges) {
            if (psmsMaps.get(charge).getnMax() >= 100) {
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
            waitingDialog.appendReport("Charge " + charge + " is analyzed together with charge " + ref + ".");
        }
    }

    /**
     * Sets whether probabilistic thresholds should be applied when recommended
     * @param probabilistic boolean indicating whether probabilistic thresholds should be applied when recommended
     */
    public void setProbabilistic(boolean probabilistic) {
        for (TargetDecoyMap targetDecoyMap : psmsMaps.values()) {
            targetDecoyMap.setProbabilistic(probabilistic);
        }
    }

    /**
     * Returns the key (here the charge) associated to the corresponding spectrum match
     * @param spectrumMatch the spectrum match of interest
     * @return the corresponding key
     */
    private Integer getKey(SpectrumMatch spectrumMatch) {
        return spectrumMatch.getSpectrum().getPrecursor().getCharge().value;
    }
}
