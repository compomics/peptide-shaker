package eu.isas.peptideshaker.scoring;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class contains the various peptides matches sorted according to their
 * variable modification status.
 *
 * @author Marc Vaudel
 */
public class PeptideSpecificMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 1464466551122518117L;
    /**
     * The peptide target/decoy maps indexed by the modification profile of the
     * peptide.
     */
    private HashMap<String, TargetDecoyMap> peptideMaps = new HashMap<String, TargetDecoyMap>();
    /**
     * The indexes of the maps which have been put to the dustbin.
     */
    private ArrayList<String> groupedMaps = new ArrayList<String>();
    /**
     * The index of the dustbin.
     */
    public final static String DUSTBIN = "OTHER";
    /**
     * Separator for the key construction.
     */
    public final static String SEPARATOR = "_cus_";

    /**
     * Constructor.
     */
    public PeptideSpecificMap() {
    }

    /**
     * Estimate the posterior error probabilities.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        waitingHandler.setWaitingText("Estimating Probabilities. Please Wait...");

        int max = getNEntries();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        for (String modifications : peptideMaps.keySet()) {

            waitingHandler.increaseSecondaryProgressCounter();

            if (!groupedMaps.contains(modifications)) {
                peptideMaps.get(modifications).estimateProbabilities(waitingHandler);
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Returns the posterior error probability of a peptide match at the given
     * score.
     *
     * @param peptideMatchKey the peptide match
     * @param score the score of the match
     * @return the posterior error probability
     */
    public double getProbability(String peptideMatchKey, double score) {
        peptideMatchKey = getCorrectedKey(peptideMatchKey);
        return peptideMaps.get(peptideMatchKey).getProbability(score);
    }

    /**
     * Adds a point in the peptide specific map.
     *
     * @param probabilityScore The estimated peptide probabilistic score
     * @param peptideMatch The corresponding peptide match
     * @param sequenceMatchingPreferences The sequence matching preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     */
    public void addPoint(double probabilityScore, PeptideMatch peptideMatch, SequenceMatchingPreferences sequenceMatchingPreferences) 
            throws IOException, InterruptedException, SQLException, ClassNotFoundException {
        String key = getKey(peptideMatch);
        if (!peptideMaps.containsKey(key)) {
            peptideMaps.put(key, new TargetDecoyMap());
        }
        peptideMaps.get(key).put(probabilityScore, peptideMatch.getTheoreticPeptide().isDecoy(sequenceMatchingPreferences));
    }

    /**
     * Returns a list of keys from maps presenting a suspicious input.
     *
     * @param initialFDR the minimal FDR requested for a group
     *
     * @return a list of keys from maps presenting a suspicious input
     */
    public ArrayList<String> suspiciousInput(Double initialFDR) {
        ArrayList<String> result = new ArrayList<String>();
        for (String key : peptideMaps.keySet()) {
            if (!groupedMaps.contains(key) && peptideMaps.get(key).suspiciousInput(initialFDR)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * This method puts all the small peptide groups in the dustbin to be
     * analyzed together.
     *
     * @param initialFDR the minimal FDR requested for a group
     */
    public void clean(Double initialFDR) {
        if (peptideMaps.size() > 1) {
            peptideMaps.put(DUSTBIN, new TargetDecoyMap());
            for (String key : peptideMaps.keySet()) {
                if (!key.equals(DUSTBIN)) {
                    TargetDecoyMap peptideMap = peptideMaps.get(key);
                    if (peptideMap.suspiciousInput(initialFDR)) {
                        groupedMaps.add(key);
                        peptideMaps.get(DUSTBIN).addAll(peptideMap);
                    }
                }
            }
        }
    }

    /**
     * This method returns the indexing key of a peptide match after curation.
     *
     * @param specificKey the considered peptide match
     * @return the corresponding key
     */
    public String getCorrectedKey(String specificKey) {
        if (groupedMaps.contains(specificKey)) {
            return DUSTBIN;
        }
        return specificKey;
    }

    /**
     * This method returns the indexing key of a peptide match. Note that the
     * peptide variable modifications must be in the PTM factory.
     *
     * @param peptideMatch the considered peptide match
     * @return the corresponding key
     */
    public String getKey(PeptideMatch peptideMatch) {
        PTMFactory ptmFactory = PTMFactory.getInstance();
        PTM ptm;
        ArrayList<Double> modificationMasses = new ArrayList<Double>();
        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.getTheoreticPtm() != null && modificationMatch.isVariable()) {
                ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                modificationMasses.add(ptm.getMass());
            }
        }
        Collections.sort(modificationMasses);
        String key = "";
        for (Double mass : modificationMasses) {
            if (!key.equals("")) {
                key += SEPARATOR;
            }
            key += mass;
        }
        return key;
    }

    /**
     * Returns the statistically retained peptide groups.
     *
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
     * Returns the desired target decoy map.
     *
     * @param key the key of the desired map
     * @return the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(String key) {
        return peptideMaps.get(key);
    }

    /**
     * Returns the number of entries of the map.
     *
     * @return the number of entries of the map
     */
    public int getNEntries() {
        int result = 0;
        for (TargetDecoyMap targetDecoyMap : peptideMaps.values()) {
            result += targetDecoyMap.getMapSize();
        }
        return result;
    }

    /**
     * Returns an intelligible string for the key of the map.
     *
     * @param modificationProfile the modification profile of the identification
     * procedure
     * @param key the key of interest
     * @return an intelligible string for the key of the map
     */
    public static String getKeyName(PtmSettings modificationProfile, String key) {

        if (key.equals("")) {
            return "Unmodified";
        } else if (key.equals(PeptideSpecificMap.DUSTBIN)) {
            return "Other";
        } else {

            PTMFactory ptmFactory = PTMFactory.getInstance();
            String result = "";
            String[] split = key.split(SEPARATOR);
            boolean shortNames = split.length > 1;

            for (String massString : split) {

                if (!result.equals("")) {
                    result += ", ";
                }

                boolean found = false;

                try {
                    Double mass = new Double(massString);
                    for (String ptmName : modificationProfile.getAllNotFixedModifications()) {
                        PTM ptm = ptmFactory.getPTM(ptmName);
                        if (mass == ptm.getMass()) {
                            if (shortNames && ptm.getShortName() != null) {
                                result += ptm.getShortName();
                            } else {
                                result += ptm.getName();
                            }
                            found = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }

                if (!found) {
                    result += massString + " PTM";
                }
            }

            return result;
        }
    }
}
