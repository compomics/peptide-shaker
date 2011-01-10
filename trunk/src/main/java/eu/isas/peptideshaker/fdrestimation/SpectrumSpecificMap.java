package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Marc Vaudel
 */
public class SpectrumSpecificMap {

    private double fdrThreshold;
    private int nHits;
    private double fdr;
    private double fnr;
    private HashMap<Integer, TargetDecoyMap> psmsMaps = new HashMap<Integer, TargetDecoyMap>();
    private HashMap<Integer, Integer> grouping = new HashMap<Integer, Integer>();

    /**
     * @TODO: JavaDoc missing
     */
    public SpectrumSpecificMap() {
    }

    /**
     * Return the number of hits.
     *
     * @return the number of hits
     */
    public int getNHits() {
        return nHits;
    }

    /**
     * Returns the FRD value.
     *
     * @return the FRD value
     */
    public double getFdr() {
        return fdr;
    }

    /**
     * Returns the FDR threshold.
     *
     * @return the FDR threshold
     */
    public double getFdrThreshold() {
        return fdrThreshold;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public double getFnr() {
        return fnr;
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public String getMethod() {
        return "Specific FDR";
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param newThreshold
     */
    public void getResults(double newThreshold) {
        if (fdrThreshold != newThreshold) {
            fdrThreshold = newThreshold;
            process();
        }
    }

    /**
     * @TODO: JavaDoc missing
     */
    private void process() {
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param waitingDialog
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        for (int charge : psmsMaps.keySet()) {
            if (!grouping.keySet().contains(charge)) {
                psmsMaps.get(charge).estimateProbabilities(waitingDialog);
            }
        }
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param spectrumMatch
     * @param score
     * @return
     */
    public double getProbability(SpectrumMatch spectrumMatch, double score) {
        int key = getKey(spectrumMatch);
        if (grouping.containsKey(key)) {
            key = grouping.get(key);
        }

        return psmsMaps.get(key).getProbability(score);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param probabilityScore
     * @param spectrumMatch
     */
    public void addPoint(double probabilityScore, SpectrumMatch spectrumMatch) {
        int key = getKey(spectrumMatch);
        if (!psmsMaps.containsKey(key)) {
            psmsMaps.put(key, new TargetDecoyMap("Charge " + key + " psms"));
        }
        psmsMaps.get(key).put(probabilityScore, spectrumMatch.getBestAssumption().isDecoy());
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param waitingDialog
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
     * @TODO: JavaDoc missing
     * 
     * @param spectrumMatch
     * @return
     */
    private Integer getKey(SpectrumMatch spectrumMatch) {
        return spectrumMatch.getSpectrum().getPrecursor().getCharge().value;
    }
}
