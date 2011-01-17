package eu.isas.peptideshaker.fdrestimation;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import eu.isas.peptideshaker.gui.ModificationDialog;
import eu.isas.peptideshaker.gui.WaitingDialog;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class contains the various peptides matches sorted 
 * 
 * @author Marc Vaudel
 */
public class PeptideSpecificMap implements Serializable {

    /**
     * The user specified fdr threshold
     */
    private double fdrThreshold;
    /**
     * The number of hits at this threshold
     */
    private int nHits;
    /**
     * The estimated FDR
     */
    private double fdr;
    /**
     * The estimated FNR
     */
    private double fnr;
    /**
     * The peptide target/decoy maps indexed by the modification profile of the peptide.
     */
    private HashMap<String, TargetDecoyMap> peptideMaps = new HashMap<String, TargetDecoyMap>();
    /**
     * The indexes of the maps which have been put to the dustbin
     */
    private ArrayList<String> groupedMaps = new ArrayList<String>();
    /**
     * The index of the dustbin
     */
    public final static String DUSTBIN = "OTHER";
    /**
     * index correction map for unexpected modifications
     */
    private HashMap<String, String> nameCorrectionMap = new HashMap<String, String>();

    /**
     * Constructor
     */
    public PeptideSpecificMap() {
    }

    /**
     * Return the number of hits at the defined FDR.
     *
     * @return the number of hits at the defined FDR
     */
    public int getNHits() {
        return nHits;
    }

    /**
     * Returns the estimated FDR.
     *
     * @return the estimated FDR 
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
     * Returns the estimated FNR
     *
     * @return the estimated FNR
     */
    public double getFnr() {
        return fnr;
    }

    /**
     * Estimates FDR, FNR and number of hits for the peptide map at the given threshold
     *
     * @param newThreshold the new threshold
     */
    public void getResults(double newThreshold) {
        this.fdrThreshold = newThreshold;
        for (TargetDecoyMap targetDecoyMap : peptideMaps.values()) {
            targetDecoyMap.getResults(fdrThreshold);
        }
    }

    /**
     * returns the method used to process peptides
     *
     * @return
    public String getMethod() {
        return "Specific FDR";
    }
     */

    /**
     * estimate the posterior error probabilities
     *
     * @param waitingDialog The dialog which display the information while processing
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        for (String modifications : peptideMaps.keySet()) {
            if (!groupedMaps.contains(modifications)) {
                peptideMaps.get(modifications).estimateProbabilities(waitingDialog);
            }
        }
    }

    /**
     * Returns the posterior error probability of a peptide match at the given score
     *
     * @param peptideMatch the peptide match
     * @param score        the score of the match
     * @return the posterior error probability
     */
    public double getProbability(PeptideMatch peptideMatch, double score) {
        String key = getKey(peptideMatch);
        if (groupedMaps.contains(key)) {
            return peptideMaps.get(DUSTBIN).getProbability(score);
        }
        return peptideMaps.get(key).getProbability(score);
    }

    /**
     * Adds a point in the peptide specific map.
     *
     * @param probabilityScore The estimated peptide probabilistic score
     * @param peptideMatch     The corresponding peptide match
     */
    public void addPoint(double probabilityScore, PeptideMatch peptideMatch) {
        String key = getKey(peptideMatch);
        if (!peptideMaps.containsKey(key)) {
            peptideMaps.put(key, new TargetDecoyMap(key + "peptides"));
        }
        peptideMaps.get(key).put(probabilityScore, peptideMatch.isDecoy());
    }

    /**
     * This methods puts all the small peptide groups in the dustbin to be analyzed together.
     *
     * @param waitingDialog The waiting dialog will display the feedback
     */
    public void cure(WaitingDialog waitingDialog) {
        if (peptideMaps.size() > 1) {
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
    }

    /**
     * Returns the score limit for the given peptide match at the selected FDR threshold
     *
     * @param peptideMatch the given peptide match
     * @return the score threshold
     */
    public double getScoreLimit(PeptideMatch peptideMatch) {
        String key = getKey(peptideMatch);
        if (groupedMaps.contains(key)) {
            key = DUSTBIN;
        }
        return peptideMaps.get(key).getScoreLimit();
    }

    /**
     * Sets whether probabilistic thresholds should be applied when recommended
     * @param probabilistic boolean indicating whether probabilistic thresholds should be applied when recommended
     */
    public void setProbabilistic(boolean probabilistic) {
        for (TargetDecoyMap targetDecoyMap : peptideMaps.values()) {
            targetDecoyMap.setProbabilistic(probabilistic);
        }
    }

    /**
     * This method returns the indexing key of a peptide match
     *
     * @param peptideMatch  the considered peptide match
     * @return the corresponding key
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
