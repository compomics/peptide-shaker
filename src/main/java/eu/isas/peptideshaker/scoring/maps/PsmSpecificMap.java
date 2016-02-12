package eu.isas.peptideshaker.scoring.maps;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This map will store target decoy informations about the PSMs grouped
 * according to their precursor charge.
 *
 * @author Marc Vaudel
 */
public class PsmSpecificMap implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 746516685643358198L;
    /**
     * The map of the PSM target/decoy maps indexed by the PSM charge.
     */
    private HashMap<Integer, TargetDecoyMap> psmsMaps = new HashMap<Integer, TargetDecoyMap>();
    /**
     * The map of the PSM target/decoy maps indexed by the PSM file and charge.
     */
    private HashMap<Integer, HashMap<String, TargetDecoyMap>> fileSpecificPsmsMaps = new HashMap<Integer, HashMap<String, TargetDecoyMap>>();
    /**
     * Map used to group charges together in order to ensure statistical.
     * relevance.
     */
    private HashMap<Integer, Integer> grouping = new HashMap<Integer, Integer>();
    /**
     * Map used to group charges together in order to ensure statistical.
     * relevance grouped per file.
     */
    private HashMap<Integer, ArrayList<String>> fileSpecificGrouping = new HashMap<Integer, ArrayList<String>>();

    /**
     * Constructor.
     */
    public PsmSpecificMap() {
    }

    /**
     * Estimate the posterior error probabilities of the PSMs.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        int max = getMapsSize();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        if (fileSpecificPsmsMaps != null) {
            for (Integer charge : fileSpecificPsmsMaps.keySet()) {
                ArrayList<String> groupedFiles = fileSpecificGrouping.get(charge);
                for (String file : fileSpecificPsmsMaps.get(charge).keySet()) {
                    if (groupedFiles == null || !groupedFiles.contains(file)) {
                        fileSpecificPsmsMaps.get(charge).get(file).estimateProbabilities(waitingHandler);
                    }
                }
            }
        }

        for (Integer charge : psmsMaps.keySet()) {

            waitingHandler.increaseSecondaryProgressCounter();

            if (!grouping.containsKey(charge)) {
                psmsMaps.get(charge).estimateProbabilities(waitingHandler);
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Returns the probability of the given spectrum match at the given score.
     *
     * @param file the file scored
     * @param charge the charge scored
     * @param score the corresponding score
     *
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(String file, int charge, double score) {
        boolean groupedFile = false;
        if (fileSpecificGrouping != null) {
            ArrayList<String> groupedFiles = fileSpecificGrouping.get(charge);
            if (groupedFiles != null && groupedFiles.contains(file)) {
                groupedFile = true;
            }
        } else {
            groupedFile = true;
        }
        if (groupedFile) {
            Integer key = grouping.get(charge);
            if (key == null) {
                key = charge;
            }
            TargetDecoyMap targetDecoyMap = psmsMaps.get(key);
            if (targetDecoyMap == null) {
                return 1;
            }
            return targetDecoyMap.getProbability(score);
        }
        HashMap<String, TargetDecoyMap> specificMap = fileSpecificPsmsMaps.get(charge);
        if (specificMap == null) {
            return 1;
        }
        TargetDecoyMap targetDecoyMap = specificMap.get(file);
        if (targetDecoyMap == null) {
            return 1;
        }
        return targetDecoyMap.getProbability(score);
    }

    /**
     * Adds a point representing the corresponding spectrum match at a given
     * score.
     *
     * @param probabilityScore the estimated score
     * @param spectrumMatch the spectrum match of interest
     * @param sequenceMatchingPreferences The sequence matching preferences
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     */
    public void addPoint(double probabilityScore, SpectrumMatch spectrumMatch, SequenceMatchingPreferences sequenceMatchingPreferences)
            throws IOException, InterruptedException, SQLException, ClassNotFoundException {

        int charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
        HashMap<String, TargetDecoyMap> fileMapping = fileSpecificPsmsMaps.get(charge);
        if (fileMapping == null) {
            fileMapping = new HashMap<String, TargetDecoyMap>();
            fileSpecificPsmsMaps.put(charge, fileMapping);
        }
        String file = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        TargetDecoyMap targetDecoyMap = fileMapping.get(file);
        if (targetDecoyMap == null) {
            targetDecoyMap = new TargetDecoyMap();
            fileMapping.put(file, targetDecoyMap);
        }
        targetDecoyMap.put(probabilityScore, spectrumMatch.getBestPeptideAssumption().getPeptide().isDecoy(sequenceMatchingPreferences));
    }

    /**
     * This method groups the statistically non significant PSMs between files
     * and with the ones having a charge directly smaller until statistical
     * significance is reached.
     *
     * @param minimalFDR the minimal FDR which should be achievable
     */
    public void clean(double minimalFDR) {
        ArrayList<Integer> charges = new ArrayList(fileSpecificPsmsMaps.keySet());
        Collections.sort(charges);
        int ref = 0;
        for (Integer charge : charges) {
            ArrayList<String> nonSignificantFiles = new ArrayList<String>();
            TargetDecoyMap tempMap = new TargetDecoyMap();
            for (String file : fileSpecificPsmsMaps.get(charge).keySet()) {
                TargetDecoyMap targetDecoyMap = fileSpecificPsmsMaps.get(charge).get(file);
                if (targetDecoyMap.suspiciousInput(minimalFDR)) {
                    nonSignificantFiles.add(file);
                    tempMap.addAll(targetDecoyMap);
                }
            }
            if (nonSignificantFiles.isEmpty()) {
                ref = 0;
            } else {
                // The following code groups all files in case statistical significance is not achieved
//                if (nonSignificantFiles.size() < fileSpecificPsmsMaps.get(charge).size() && (tempMap.getnMax() < 100 || tempMap.getnTargetOnly() < 100)) {
//                    for (String file : fileSpecificPsmsMaps.get(charge).keySet()) {
//                        if (!nonSignificantFiles.contains(file)) {
//                            TargetDecoyMap targetDecoyMap = fileSpecificPsmsMaps.get(charge).get(file);
//                            nonSignificantFiles.add(file);
//                            tempMap.addAll(targetDecoyMap);
//                        }
//                    }
//                }
                psmsMaps.put(charge, tempMap);
                fileSpecificGrouping.put(charge, nonSignificantFiles);
                if (tempMap.suspiciousInput(minimalFDR)) {
                    if (ref > 0) {
                        psmsMaps.get(ref).addAll(tempMap);
                        grouping.put(charge, ref);
                    } else {
                        ref = charge;
                    }
                } else {
                    ref = 0;
                }
            }
        }
    }

    /**
     * Returns the desired target decoy map.
     *
     * @param charge the identified charge of the PSM
     * @param spectrumFile the name of the spectrum file
     *
     * @return the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(int charge, String spectrumFile) {
        if (fileSpecificGrouping != null && spectrumFile != null) {
            ArrayList<String> nonSignificantFiles = fileSpecificGrouping.get(charge);
            if (nonSignificantFiles == null || !nonSignificantFiles.contains(spectrumFile)) {
                HashMap<String, TargetDecoyMap> chargeMapping = fileSpecificPsmsMaps.get(charge);
                if (chargeMapping != null) {
                    return chargeMapping.get(spectrumFile);
                }
            }
        }
        Integer correctedCharge = grouping.get(charge);
        if (correctedCharge == null) {
            correctedCharge = charge;
        }
        return psmsMaps.get(correctedCharge);
    }

    /**
     * Indicates whether the given file was grouped for the given charge.
     *
     * @param charge the charge of interest
     * @param fileName the name of the file
     *
     * @return a boolean indicating whether the given file was grouped for the
     * given charge
     */
    public boolean isFileGrouped(int charge, String fileName) {
        if (fileSpecificPsmsMaps == null) {
            return true;
        }
        ArrayList<String> groupedFiles = fileSpecificGrouping.get(charge);
        return groupedFiles != null && groupedFiles.contains(fileName);
    }

    /**
     * For grouped files, returns the reference charge of the group.
     *
     * @param charge the charge of the match
     *
     * @return the charge of the group for grouped files, the original charge if
     * not found
     */
    public Integer getCorrectedCharge(int charge) {
        Integer correctedCharge = grouping.get(charge);
        if (correctedCharge == null) {
            return charge;
        }
        return correctedCharge;
    }

    /**
     * Returns the charges found in the map
     *
     * @return the charges found in the map
     */
    public ArrayList<Integer> getPossibleCharges() {
        if (fileSpecificPsmsMaps != null) {
            return new ArrayList<Integer>(fileSpecificPsmsMaps.keySet());
        } else {
            return new ArrayList<Integer>(grouping.keySet());
        }
    }

    /**
     * Returns a list of charges from grouped files.
     *
     * @return a list of charges from grouped files
     */
    public ArrayList<Integer> getChargesFromGroupedFiles() {
        return new ArrayList<Integer>(psmsMaps.keySet());
    }

    /**
     * Returns a list of grouped charges from grouped files.
     *
     * @return a list of grouped charges from grouped files
     */
    public HashSet<Integer> getGroupedCharges() {
        HashSet<Integer> result = new HashSet<Integer>();
        for (int charge : psmsMaps.keySet()) {
            Integer correctedCharge = grouping.get(charge);
            if (correctedCharge == null) {
                correctedCharge = charge;
            }
            result.add(correctedCharge);
        }
        return result;
    }

    /**
     * Returns the map of grouped charges indexed by representative charge.
     * 
     * @return the map of grouped charges indexed by representative charge
     */
    public HashMap<Integer, ArrayList<Integer>> getChargeGroupingMap() {
        HashMap<Integer, ArrayList<Integer>> result = new HashMap<Integer, ArrayList<Integer>>(4);
        for (Integer charge : getChargesFromGroupedFiles()) {
            Integer correctedCharge = getCorrectedCharge(charge);
            if (!correctedCharge.equals(charge)) {
                ArrayList<Integer> secondaryCharges = result.get(correctedCharge);
                if (secondaryCharges == null) {
                    secondaryCharges = new ArrayList<Integer>(1);
                    result.put(correctedCharge, secondaryCharges);
                }
                secondaryCharges.add(charge);
            } else if (!result.containsKey(charge)) {
                result.put(charge, new ArrayList<Integer>(1));
            }
        }
        return result;
    }

    /**
     * Returns the files at the given charge, an empty list if not found.
     *
     * @param charge the charge of interest
     *
     * @return the files at the given charge
     */
    public ArrayList<String> getFilesAtCharge(int charge) {
        if (fileSpecificPsmsMaps != null) {
            HashMap<String, TargetDecoyMap> chargeMap = fileSpecificPsmsMaps.get(charge);
            if (chargeMap != null) {
                return new ArrayList<String>(chargeMap.keySet());
            }
        }
        return new ArrayList<String>(0);
    }

    /**
     * Returns the overall number of points across all maps.
     *
     * @return the overall number of points across all maps.
     */
    public int getMapsSize() {
        int result = 0;
        for (TargetDecoyMap targetDecoyMap : psmsMaps.values()) {
            result += targetDecoyMap.getMapSize();
        }
        for (HashMap<String, TargetDecoyMap> mapping : fileSpecificPsmsMaps.values()) {
            for (TargetDecoyMap targetDecoyMap : mapping.values()) {
                result += targetDecoyMap.getMapSize();
            }
        }
        return result;
    }

    /**
     * Returns a list of the target decoy maps used for scoring.
     *
     * @return a list of the target decoy maps used for scoring
     */
    public ArrayList<TargetDecoyMap> getTargetDecoyMaps() {
        ArrayList<TargetDecoyMap> result = new ArrayList<TargetDecoyMap>();
        for (int charge : fileSpecificPsmsMaps.keySet()) {
            ArrayList<String> nonSignificantFiles = fileSpecificGrouping.get(charge);
            for (String file : fileSpecificPsmsMaps.get(charge).keySet()) {
                if (nonSignificantFiles == null || !nonSignificantFiles.contains(file)) {
                    result.add(fileSpecificPsmsMaps.get(charge).get(file));
                }
            }
        }
        for (int charge : getGroupedCharges()) {
            TargetDecoyMap map = psmsMaps.get(charge);
            result.add(map);
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
        if (fileSpecificPsmsMaps != null) {
            for (int charge : fileSpecificPsmsMaps.keySet()) {
                if (charge > maxCharge) {
                    maxCharge = charge;
                }
            }
        } else {
            for (int charge : psmsMaps.keySet()) {
                if (charge > maxCharge) {
                    maxCharge = charge;
                }
            }
        }
        return maxCharge;
    }
}
