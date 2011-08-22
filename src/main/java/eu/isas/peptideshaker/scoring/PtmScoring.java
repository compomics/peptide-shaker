package eu.isas.peptideshaker.scoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class contains score about the PTM localization scoring
 *
 * @author Marc
 */
public class PtmScoring implements Serializable {

    /**
     * The delta scores indexed by the modification location possibility
     */
    private HashMap<String, Double> deltaScores = new HashMap<String, Double>();
    /**
     * The A scores indexed by the modification location possibility
     */
    private HashMap<String, Double> aScores = new HashMap<String, Double>();
    /**
     * The name of the modification of interest
     */
    private String ptmName;
    /**
     * The separator used to separate locations in the modification location key
     */
    public static final String separator = "|";

    /**
     * Constructor
     * @param ptmName the name of the PTM of interest
     */
    public PtmScoring(String ptmName) {
        this.ptmName = ptmName;
    }

    /**
     * Returns the name of the inspected protein
     * @return the name of the inspected protein 
     */
    public String getName() {
        return ptmName;
    }

    /**
     * Adds a delta score for the given locations combination
     * @param locations     the combination of locations
     * @param deltaScore    The corresponding delta score
     */
    public void addDeltaScore(ArrayList<Integer> locations, double deltaScore) {
        String locationsKey = getKey(locations);
        addDeltaScore(locationsKey, deltaScore);
    }


    /**
     * Adds an A-score for the given locations combination
     * @param locations     the combination of locations
     * @param deltaScore    The corresponding A-score
     */
    public void addAScore(ArrayList<Integer> locations, double aScore) {
        String locationsKey = getKey(locations);
        addAScore(locationsKey, aScore);
    }


    /**
     * Adds a delta score for the given locations combination given as a key
     * @param locations     the combination of locations
     * @param deltaScore    The corresponding delta score
     */
    public void addDeltaScore(String locationsKey, double deltaScore) {
        if (!deltaScores.containsKey(locationsKey)
                || deltaScores.get(locationsKey) < deltaScore) {
            deltaScores.put(locationsKey, deltaScore);
        }
    }


    /**
     * Adds an A score for the given locations combination given as a key
     * @param locations     the combination of locations
     * @param deltaScore    The corresponding A-score
     */
    public void addAScore(String locationsKey, double aScore) {
        if (!aScores.containsKey(locationsKey)
                || aScores.get(locationsKey) > aScore) {
            aScores.put(locationsKey, aScore);
        }
    }

    /**
     * Returns the implemented locations possibilities for delta scores
     * @return the implemented locations possibilities for delta scores
     */
    public ArrayList<String> getDeltaScorelocations() {
        return new ArrayList<String>(deltaScores.keySet());
    }
    
    /**
     * Returns the best scoring modification profile based on the delta score
     * @return the best scoring modification profile based on the delta score
     */
    public String getBestDeltaScoreLocations() {
        String bestKey = null;
        for (String key : deltaScores.keySet()) {
            if (bestKey == null || deltaScores.get(bestKey) < deltaScores.get(key)) {
                bestKey = key;
            }
        }
        return bestKey;
    }

    /**
     * Returns the implemented locations for the A-score
     * @return the implemented locations for the A-score
     */
    public ArrayList<String> getAScorePostions() {
        return new ArrayList<String>(aScores.keySet());
    }

    /**
     * Returns the delta score for the specified locations possibility given as a key
     * @param locationsKey the locations possibility given as a key
     * @return the delta score 
     */
    public double getDeltaScore(String locationsKey) {
        return deltaScores.get(locationsKey);
    }

    /**
     * Returns the A-score for the specified locations possibility given as a key
     * @param locationsKey the locations possibility given as a key
     * @return the A-score 
     */
    public double getAScore(String locationsKey) {
        return aScores.get(locationsKey);
    }

    /**
     * adds all scorings from another score
     * @param anotherScore another score
     */
    public void addAll(PtmScoring anotherScore) {
        for (String positions : anotherScore.getDeltaScorelocations()) {
            addDeltaScore(positions, anotherScore.getDeltaScore(positions));
        }
        for (String positions : anotherScore.getAScorePostions()) {
            addAScore(positions, anotherScore.getAScore(positions));
        }
    }

    /**
     * Returns the modification locations from a key
     * @param locationsKey the modification locations key
     * @return the modification locations as an ArrayList containing all possible locations
     */
    public static ArrayList<Integer> getLocations(String locationsKey) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if (locationsKey.length() > 1) {
            String tempKey = locationsKey.substring(0, locationsKey.lastIndexOf(separator));
            int index;
            while (tempKey.length() > 1) {
                index = tempKey.lastIndexOf(separator);
                result.add(new Integer(tempKey.substring(index + 1)));
                if (index > -1) {
                    tempKey = tempKey.substring(0, index);
                } else {
                    break;
                }
            }
            result.add(new Integer(tempKey));
        }
        return result;
    }

    /**
     * Returns the key corresponding to modification possible locations
     * @param locations the possible modification locations in the sequence
     * @return the corresponding key
     */
    public static String getKey(ArrayList<Integer> locations) {
        Collections.sort(locations);
        String result = "";
        for (int loc : locations) {
            result += loc + separator;
        }
        return result;
    }
}
