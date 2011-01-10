package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import eu.isas.peptideshaker.gui.ModificationDialog;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Marc Vaudel
 */
public class PeptideSpecificMap {

    private double fdrThreshold;
    private int nHits;
    private double fdr;
    private double fnr;
    private HashMap<String, TargetDecoyMap> peptideMaps = new HashMap<String, TargetDecoyMap>();
    private ArrayList<String> groupedMaps = new ArrayList<String>();
    public final static String DUSTBIN = "OTHER";
    private HashMap<String, String> nameCorrectionMap = new HashMap<String, String>();

    /**
     * @TODO: JavaDoc missing
     */
    public PeptideSpecificMap() {
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
        for (String modifications : peptideMaps.keySet()) {
            if (!groupedMaps.contains(modifications)) {
                peptideMaps.get(modifications).estimateProbabilities(waitingDialog);
            }
        }
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param peptideMatch
     * @param score
     * @return
     */
    public double getProbability(PeptideMatch peptideMatch, double score) {
        String key = getKey(peptideMatch);
        if (groupedMaps.contains(key)) {
            return peptideMaps.get(DUSTBIN).getProbability(score);
        }
        return peptideMaps.get(key).getProbability(score);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param probabilityScore
     * @param peptideMatch
     */
    public void addPoint(double probabilityScore, PeptideMatch peptideMatch) {
        String key = getKey(peptideMatch);
        if (!peptideMaps.containsKey(key)) {
            peptideMaps.put(key, new TargetDecoyMap(key + "peptides"));
        }
        peptideMaps.get(key).put(probabilityScore, peptideMatch.isDecoy());
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param waitingDialog
     */
    public void cure(WaitingDialog waitingDialog) {
        peptideMaps.put(DUSTBIN, new TargetDecoyMap(DUSTBIN + " peptides"));
        for (String key : peptideMaps.keySet()) {
            if (!key.equals(DUSTBIN)) {
                TargetDecoyMap peptideMap = peptideMaps.get(key);
                if (peptideMap.getnMax() < 10) {
                    groupedMaps.add(key);
                    peptideMaps.get(DUSTBIN).addAll(peptideMap);
                }
            }
        }
        String output = "";
        for (String modifications : groupedMaps) {
            output += modifications + ", ";
        }
        waitingDialog.appendReport(output + "modified peptides are analyzed together as " + DUSTBIN + " peptides.");
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param peptideMatch
     * @return
     */
    private String getKey(PeptideMatch peptideMatch) {
        ArrayList<String> modifications = new ArrayList<String>();
        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.getTheoreticPtm() != null
                    && modificationMatch.isVariable()) {
                if (modificationMatch.getTheoreticPtm().getType() == PTM.MODMAX) {
                    String newName = modificationMatch.getTheoreticPtm().getName();
                    if (!nameCorrectionMap.containsKey(newName)) {
                        ModificationDialog modificationDialog = new ModificationDialog(null, true, modificationMatch.getTheoreticPtm());
                        modificationDialog.setVisible(true);
                        if (!modificationDialog.isCanceled()) {
                            nameCorrectionMap.put(newName, modificationDialog.getSelectedModification());
                        } else {
                            nameCorrectionMap.put(newName, newName);
                        }
                    }
                    modifications.add(nameCorrectionMap.get(newName));
                } else {
                    modifications.add(modificationMatch.getTheoreticPtm().getName());
                }
            }
        }
        Collections.sort(modifications);
        String key = "";
        for (String modification : modifications) {
            key += modification + "_";
        }
        return key;
    }
}
