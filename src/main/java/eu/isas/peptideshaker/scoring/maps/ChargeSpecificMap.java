package eu.isas.peptideshaker.scoring.maps;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This stores target decoy maps grouped by charge.
 *
 * @author Marc Vaudel
 */
public class ChargeSpecificMap extends DbObject implements Serializable {

    /**
     * Serial version UID for backward compatibility.
     */
    static final long serialVersionUID = 746516685643358198L;
    /**
     * The map of the PSM target/decoy maps indexed by the PSM charge.
     */
    private final HashMap<Integer, TargetDecoyMap> chargeMaps = new HashMap<>(4);
    /**
     * The map of the PSM target/decoy maps indexed by the PSM file and charge.
     */
    private final HashMap<Integer, HashMap<String, TargetDecoyMap>> fileSpecificChargeMaps = new HashMap<>(4);
    /**
     * Map used to group charges together.
     */
    private final HashMap<Integer, Integer> grouping = new HashMap<>(4);
    /**
     * Map used to group charges together per file.
     */
    private final HashMap<Integer, HashSet<String>> fileSpecificGrouping = new HashMap<>(4);

    /**
     * Constructor.
     */
    public ChargeSpecificMap() {
    }

    /**
     * Estimate the posterior error probabilities of the PSMs.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        int max = getMapsSize();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        fileSpecificChargeMaps.entrySet().parallelStream()
                .forEach(entry1 -> entry1.getValue().entrySet().parallelStream()
                .filter(entry2 -> !fileSpecificGrouping.containsKey(entry1.getKey()) || fileSpecificGrouping.get(entry1.getKey()).contains(entry2.getKey()))
                .forEach(entry2 -> entry2.getValue().estimateProbabilities(waitingHandler)));

        chargeMaps.entrySet().parallelStream()
                .filter(entry -> !grouping.containsKey(entry.getKey()))
                .forEach(entry -> entry.getValue().estimateProbabilities(waitingHandler));

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        HashSet<String> groupedFiles = fileSpecificGrouping.get(charge);

        if (groupedFiles != null && groupedFiles.contains(file)) {

            Integer key = grouping.get(charge);

            if (key == null) {

                key = charge;

            }

            TargetDecoyMap targetDecoyMap = chargeMaps.get(key);

            if (targetDecoyMap == null) {

                return 1.0;

            }

            return targetDecoyMap.getProbability(score);

        }

        HashMap<String, TargetDecoyMap> specificMap = fileSpecificChargeMaps.get(charge);

        if (specificMap == null) {

            return 1.0;

        }

        TargetDecoyMap targetDecoyMap = specificMap.get(file);

        if (targetDecoyMap == null) {

            return 1.0;

        }

        return targetDecoyMap.getProbability(score);

    }

    /**
     * Adds a point representing the corresponding spectrum match at a given
     * score.
     *
     * @param fileName the name of the spectrum file
     * @param charge the charge of the match
     * @param score the score
     * @param decoy whether the match is target or decoy
     */
    public void addPoint(String fileName, int charge, double score, boolean decoy) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        HashMap<String, TargetDecoyMap> fileMapping = fileSpecificChargeMaps.get(charge);

        if (fileMapping == null) {

            fileMapping = new HashMap<>(4);
            fileSpecificChargeMaps.put(charge, fileMapping);

        }

        TargetDecoyMap targetDecoyMap = fileMapping.get(fileName);

        if (targetDecoyMap == null) {

            targetDecoyMap = new TargetDecoyMap();
            fileMapping.put(fileName, targetDecoyMap);

        }

        targetDecoyMap.put(score, decoy);

    }

    /**
     * This method groups the maps between files and charges until a sufficient
     * number of hits is available.
     *
     * @param minimalFDR the minimal FDR which should be achievable
     */
    public void clean(double minimalFDR) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        TreeMap<Integer, HashMap<String, TargetDecoyMap>> sortedMap = new TreeMap<>(fileSpecificChargeMaps);

        int ref = 0;
        for (Entry<Integer, HashMap<String, TargetDecoyMap>> entry1 : sortedMap.entrySet()) {

            HashMap<String, TargetDecoyMap> subMap = entry1.getValue();
            HashSet<String> suspiciousFiles = new HashSet<>(0);
            TargetDecoyMap tempMap = new TargetDecoyMap();

            for (Entry<String, TargetDecoyMap> entry2 : subMap.entrySet()) {

                TargetDecoyMap targetDecoyMap = entry2.getValue();

                if (targetDecoyMap.suspiciousInput(minimalFDR)) {

                    suspiciousFiles.add(entry2.getKey());
                    tempMap.addAll(targetDecoyMap);

                }
            }

            if (suspiciousFiles.isEmpty()) {

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

                int charge = entry1.getKey();
                chargeMaps.put(charge, tempMap);
                fileSpecificGrouping.put(charge, suspiciousFiles);

                if (tempMap.suspiciousInput(minimalFDR)) {

                    if (ref > 0) {

                        chargeMaps.get(ref).addAll(tempMap);
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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        if (spectrumFile != null) {

            if (isFileGrouped(charge, spectrumFile)) {

                HashMap<String, TargetDecoyMap> chargeMapping = fileSpecificChargeMaps.get(charge);

                if (chargeMapping != null) {

                    return chargeMapping.get(spectrumFile);

                }
            }
        }

        int correctedCharge = getCorrectedCharge(charge);

        return chargeMaps.get(correctedCharge);

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

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        HashSet<String> groupedFiles = fileSpecificGrouping.get(charge);

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
    public int getCorrectedCharge(int charge) {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

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
    public Set<Integer> getPossibleCharges() {

        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();

        return fileSpecificChargeMaps.keySet();

    }

    /**
     * Returns a list of charges from grouped files.
     *
     * @return a list of charges from grouped files
     */
    public Set<Integer> getChargesFromGroupedFiles() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return chargeMaps.keySet();
    
    }

    /**
     * Returns a list of grouped charges from grouped files.
     *
     * @return a list of grouped charges from grouped files
     */
    public int[] getGroupedCharges() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return chargeMaps.keySet().stream()
                .mapToInt(charge -> getCorrectedCharge(charge))
                .distinct()
                .toArray();
        
    }

    /**
     * Returns the map of grouped charges indexed by representative charge.
     *
     * @return the map of grouped charges indexed by representative charge
     */
    public HashMap<Integer, ArrayList<Integer>> getChargeGroupingMap() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        HashMap<Integer, ArrayList<Integer>> result = new HashMap<>(4);
        
        for (int charge : getChargesFromGroupedFiles()) {
        
            int correctedCharge = getCorrectedCharge(charge);
            
            if (correctedCharge != charge) {
                
                ArrayList<Integer> secondaryCharges = result.get(correctedCharge);
                
                if (secondaryCharges == null) {
                
                    secondaryCharges = new ArrayList<>(1);
                    result.put(correctedCharge, secondaryCharges);
                
                }
                
                secondaryCharges.add(charge);
            
            } else if (!result.containsKey(charge)) {
            
                result.put(charge, new ArrayList<>(1));
            
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
    public TreeSet<String> getFilesAtCharge(int charge) {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        HashMap<String, TargetDecoyMap> chargeMap = fileSpecificChargeMaps.get(charge);
            
          return  chargeMap == null ? new TreeSet<>() : new TreeSet<>(chargeMap.keySet());
            
    }

    /**
     * Returns the overall number of points across all maps.
     *
     * @return the overall number of points across all maps.
     */
    public int getMapsSize() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return chargeMaps.values().stream()
                .mapToInt(map -> map.getMapSize())
                .sum()
                + fileSpecificChargeMaps.values().stream()
                .flatMap(map -> map.values().stream())
                .mapToInt(map -> map.getMapSize())
                .sum();
        
    }

    /**
     * Returns a list of the target decoy maps used for scoring.
     *
     * @return a list of the target decoy maps used for scoring
     */
    public ArrayList<TargetDecoyMap> getTargetDecoyMaps() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        ArrayList<TargetDecoyMap> result = new ArrayList<TargetDecoyMap>(0);
        
        result.addAll(
                fileSpecificChargeMaps.entrySet().stream()
                        .flatMap(entry1 -> entry1.getValue().entrySet().stream()
                                .filter(entry2 -> !isFileGrouped(entry1.getKey(), entry2.getKey()))
                                .map(entry2 -> entry2.getValue()))
                        .collect(Collectors.toList()));
        
        result.addAll(
                Arrays.stream(getGroupedCharges())
                        .mapToObj(charge -> chargeMaps.get(charge))
                        .collect(Collectors.toList()));
        
        return result;
        
    }

    /**
     * Returns the maximal precursor charge observed in the identified spectra.
     *
     * @return the maximal precursor charge observed in the identified spectra
     */
    public int getMaxCharge() {
        
        ObjectsDB.increaseRWCounter();
        zooActivateRead();
        ObjectsDB.decreaseRWCounter();
        
        return fileSpecificChargeMaps.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        
    }
}
