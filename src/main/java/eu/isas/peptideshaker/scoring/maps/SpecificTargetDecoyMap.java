package eu.isas.peptideshaker.scoring.maps;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.waiting.WaitingHandler;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This stores target decoy maps grouped by file and category.
 *
 * @author Marc Vaudel
 */
public abstract class SpecificTargetDecoyMap extends DbObject implements Serializable {

    /**
     * Serial version UID for backward compatibility.
     */
    static final long serialVersionUID = 746516685643358198L;
    /**
     * The map of the target decoy maps indexed by file and category.
     */
    protected final HashMap<Integer, HashMap<String, TargetDecoyMap>> fileSpecificMaps = new HashMap<>(4);
    /**
     * Map used to group keys together per file.
     */
    protected final HashMap<Integer, HashSet<String>> fileSpecificGrouping = new HashMap<>(4);
    /**
     * The map of the target decoy maps indexed by category.
     */
    protected final HashMap<Integer, TargetDecoyMap> groupedMaps = new HashMap<>(4);
    /**
     * Map used to group keys together in the grouped maps.
     */
    protected final HashMap<Integer, Integer> grouping = new HashMap<>(4);

    /**
     * Constructor.
     */
    public SpecificTargetDecoyMap() {
    }

    /**
     * Estimate the posterior error probabilities of the PSMs.
     *
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateProbabilities(WaitingHandler waitingHandler) {

        
        writeDBMode();
        

        int max = getMapsSize();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(max);

        fileSpecificMaps.entrySet().parallelStream()
                .forEach(entry1 -> entry1.getValue().entrySet().parallelStream()
                .filter(entry2 -> !fileSpecificGrouping.containsKey(entry1.getKey()) || fileSpecificGrouping.get(entry1.getKey()).contains(entry2.getKey()))
                .forEach(entry2 -> entry2.getValue().estimateProbabilities(waitingHandler)));

        groupedMaps.entrySet().parallelStream()
                .filter(entry -> !grouping.containsKey(entry.getKey()))
                .forEach(entry -> entry.getValue().estimateProbabilities(waitingHandler));

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Returns the probability of the given spectrum match at the given score.
     *
     * @param file the file scored
     * @param category the category scored
     * @param score the corresponding score
     *
     * @return the probability of the given spectrum match at the given score
     */
    public double getProbability(String file, int category, double score) {

        
        readDBMode();
        

        HashSet<String> groupedFiles = fileSpecificGrouping.get(category);

        if (groupedFiles != null && groupedFiles.contains(file)) {

            Integer key = grouping.get(category);

            if (key == null) {

                key = category;

            }

            TargetDecoyMap targetDecoyMap = groupedMaps.get(key);

            if (targetDecoyMap == null) {

                return 1.0;

            }

            return targetDecoyMap.getProbability(score);

        }

        HashMap<String, TargetDecoyMap> specificMap = fileSpecificMaps.get(category);

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
     * @param category the category of the match
     * @param score the score
     * @param decoy whether the match is target or decoy
     */
    public void addPoint(String fileName, int category, double score, boolean decoy) {

        
        writeDBMode();
        

        HashMap<String, TargetDecoyMap> fileMapping = fileSpecificMaps.get(category);

        if (fileMapping == null) {

            fileMapping = new HashMap<>(4);
            fileSpecificMaps.put(category, fileMapping);

        }

        TargetDecoyMap targetDecoyMap = fileMapping.get(fileName);

        if (targetDecoyMap == null) {

            targetDecoyMap = new TargetDecoyMap();
            fileMapping.put(fileName, targetDecoyMap);

        }

        targetDecoyMap.put(score, decoy);

    }

    /**
     * Handles sparse maps.
     *
     * @param minimalFDR the minimal FDR which should be achievable
     */
    public abstract void clean(double minimalFDR);

    /**
     * Returns the desired target decoy map.
     *
     * @param category the identified category of the PSM
     * @param spectrumFile the name of the spectrum file
     *
     * @return the corresponding target decoy map
     */
    public TargetDecoyMap getTargetDecoyMap(int category, String spectrumFile) {

        
        readDBMode();
        

        if (spectrumFile != null) {

            if (isFileGrouped(category, spectrumFile)) {

                HashMap<String, TargetDecoyMap> categoryMapping = fileSpecificMaps.get(category);

                if (categoryMapping != null) {

                    return categoryMapping.get(spectrumFile);

                }
            }
        }

        int correctedCharge = getCorrectedCharge(category);

        return groupedMaps.get(correctedCharge);

    }

    /**
     * Indicates whether the given file was grouped for the given category.
     *
     * @param category the category of interest
     * @param fileName the name of the file
     *
     * @return a boolean indicating whether the given file was grouped for the
     * given category
     */
    public boolean isFileGrouped(int category, String fileName) {

        
        readDBMode();
        

        HashSet<String> groupedFiles = fileSpecificGrouping.get(category);

        return groupedFiles != null && groupedFiles.contains(fileName);

    }

    /**
     * For grouped files, returns the reference category of the group.
     *
     * @param category the category of the match
     *
     * @return the category of the group for grouped files, the original category if
     * not found
     */
    public int getCorrectedCharge(int category) {

        
        readDBMode();
        

        Integer correctedCharge = grouping.get(category);

        if (correctedCharge == null) {

            return category;

        }

        return correctedCharge;

    }

    /**
     * Returns the categories found in the map
     *
     * @return the categories found in the map
     */
    public Set<Integer> getPossibleCategories() {

        
        readDBMode();
        

        return fileSpecificMaps.keySet();

    }

    /**
     * Returns a list of categories from grouped files.
     *
     * @return a list of categories from grouped files
     */
    public Set<Integer> getCategoriesFromGroupedFiles() {
        
        
        readDBMode();
        
        
        return groupedMaps.keySet();
    
    }

    /**
     * Returns a list of grouped categories from grouped files.
     *
     * @return a list of grouped categories from grouped files
     */
    public int[] getGroupedCategories() {
        
        
        readDBMode();
        
        
        return groupedMaps.keySet().stream()
                .mapToInt(category -> getCorrectedCharge(category))
                .distinct()
                .toArray();
        
    }

    /**
     * Returns the map of grouped categories indexed by representative category.
     *
     * @return the map of grouped categories indexed by representative category
     */
    public HashMap<Integer, ArrayList<Integer>> getChargeGroupingMap() {
        
        
        readDBMode();
        
        
        HashMap<Integer, ArrayList<Integer>> result = new HashMap<>(4);
        
        for (int category : getCategoriesFromGroupedFiles()) {
        
            int correctedCategory = getCorrectedCharge(category);
            
            if (correctedCategory != category) {
                
                ArrayList<Integer> secondaryCategories = result.get(correctedCategory);
                
                if (secondaryCategories == null) {
                
                    secondaryCategories = new ArrayList<>(1);
                    result.put(correctedCategory, secondaryCategories);
                
                }
                
                secondaryCategories.add(category);
            
            } else if (!result.containsKey(category)) {
            
                result.put(category, new ArrayList<>(1));
            
            }
        }

        return result;

    }

    /**
     * Returns the files at the given category, an empty list if not found.
     *
     * @param category the category of interest
     *
     * @return the files at the given category
     */
    public TreeSet<String> getFilesAtCategory(int category) {
        
        
        readDBMode();
        
        
        HashMap<String, TargetDecoyMap> categoryMap = fileSpecificMaps.get(category);
            
          return  categoryMap == null ? new TreeSet<>() : new TreeSet<>(categoryMap.keySet());
            
    }

    /**
     * Returns the overall number of points across all maps.
     *
     * @return the overall number of points across all maps.
     */
    public int getMapsSize() {
        
        
        readDBMode();
        
        
        return groupedMaps.values().stream()
                .mapToInt(map -> map.getMapSize())
                .sum()
                + 
                fileSpecificMaps.values().stream()
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
        
        
        readDBMode();
        
        
        ArrayList<TargetDecoyMap> result = new ArrayList<>(0);
        
        result.addAll(fileSpecificMaps.entrySet().stream()
                        .flatMap(entry1 -> entry1.getValue().entrySet().stream()
                                .filter(entry2 -> !isFileGrouped(entry1.getKey(), entry2.getKey()))
                                .map(entry2 -> entry2.getValue()))
                        .collect(Collectors.toList()));
        
        result.addAll(Arrays.stream(getGroupedCategories())
                        .mapToObj(category -> groupedMaps.get(category))
                        .collect(Collectors.toList()));
        
        return result;
        
    }
}
