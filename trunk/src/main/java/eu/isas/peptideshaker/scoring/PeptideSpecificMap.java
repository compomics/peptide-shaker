package eu.isas.peptideshaker.scoring;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
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
 * This class contains the various peptides matches sorted according to their variable modification status
 * 
 * @author Marc Vaudel
 */
public class PeptideSpecificMap implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = 1464466551122518117L;
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
     * Estimate the posterior error probabilities.
     * 
     * @param waitingDialog a reference to the waiting dialog
     */
    public void estimateProbabilities(WaitingDialog waitingDialog) {
        
        int max = peptideMaps.keySet().size();
        
        if (waitingDialog != null) {
            waitingDialog.setSecondaryProgressDialogIntermediate(false);
            waitingDialog.setMaxSecondaryProgressValue(max);
        }
        
        
        for (String modifications : peptideMaps.keySet()) {
            
            if (waitingDialog != null) {
                waitingDialog.increaseSecondaryProgressValue();
            }
            
            if (!groupedMaps.contains(modifications)) {
                peptideMaps.get(modifications).estimateProbabilities();
            }
        }
        
        if (waitingDialog != null) {
            waitingDialog.setSecondaryProgressDialogIntermediate(true);
        }
    }

    /**
     * Returns the posterior error probability of a peptide match at the given score
     *
     * @param peptideMatchKey   the peptide match
     * @param score             the score of the match
     * @return                  the posterior error probability
     */
    public double getProbability(String peptideMatchKey, double score) {
        peptideMatchKey = getCorrectedKey(peptideMatchKey);
        return peptideMaps.get(peptideMatchKey).getProbability(score);
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
            peptideMaps.put(key, new TargetDecoyMap());
        }
        peptideMaps.get(key).put(probabilityScore, peptideMatch.isDecoy());
    }

    /**
     * Returns a list of keys from maps presenting a suspicious input
     * @return a list of keys from maps presenting a suspicious input
     */
    public ArrayList<String> suspiciousInput() {
        ArrayList<String> result = new ArrayList<String>();
        for (String key : peptideMaps.keySet()) {
            if (peptideMaps.get(key).suspiciousInput()) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * This method puts all the small peptide groups in the dustbin to be analyzed together.
     *
     */
    public void cure() {
        if (peptideMaps.size() > 1) {
            peptideMaps.put(DUSTBIN, new TargetDecoyMap());
            for (String key : peptideMaps.keySet()) {
                if (!key.equals(DUSTBIN)) {
                    TargetDecoyMap peptideMap = peptideMaps.get(key);
                    if (peptideMap.getnMax() < 100 || peptideMap.getnTargetOnly() < 100) {
                        groupedMaps.add(key);
                        peptideMaps.get(DUSTBIN).addAll(peptideMap);
                    }
                }
            }
        }
    }

    /**
     * This method returns the indexing key of a peptide match after curation
     *
     * @param specificKey   the considered peptide match
     * @return              the corresponding key
     */
    public String getCorrectedKey(String specificKey) {
        if (groupedMaps.contains(specificKey)) {
            return DUSTBIN;
        }
        return specificKey;
    }

    /**
     * This method returns the indexing key of a peptide match
     *
     * @param peptideMatch  the considered peptide match
     * @return the corresponding key
     */
    public String getKey(PeptideMatch peptideMatch) {
        ArrayList<String> modifications = new ArrayList<String>();
        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.getTheoreticPtm() != null
                    && modificationMatch.isVariable()) {
                    modifications.add(modificationMatch.getTheoreticPtm());
            }
        }
        Collections.sort(modifications);
        String key = "";
        for (String modification : modifications) {
            key += modification + "_";
        }
        return key;
    }

    /**
     * Returns the statistically retained peptide groups
     * @return the statistically retained peptide groups
     */
    public ArrayList<String> getKeys() {
        ArrayList<String> results = new ArrayList<String>();
        for (String key : peptideMaps.keySet()) {
            if (!groupedMaps.contains(key)) {
                results.add(key);
            }
        }
        return results;
    }
    
    /**
     * Returns the desired target decoy map
     * @param key   the key of the desired map
     * @return      the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(String key) {
        return peptideMaps.get(key);
    }
}
