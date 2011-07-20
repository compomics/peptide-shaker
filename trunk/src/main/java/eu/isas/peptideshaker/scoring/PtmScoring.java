package eu.isas.peptideshaker.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class contains score about the PTM localization scoring
 *
 * @author marc
 */
public class PtmScoring {

    private HashMap<String, Double> deltaScores = new HashMap<String, Double>();
    private HashMap<String, Double> aScores = new HashMap<String, Double>();
    private String ptmName;
    public static final String separator = "|";

    public PtmScoring(String ptmName) {
        this.ptmName = ptmName;
    }

    public String getName() {
        return ptmName;
    }

    public void addDeltaScore(ArrayList<Integer> locations, double deltaScore) {
        String locationsKey = getKey(locations);
        if (!deltaScores.containsKey(locationsKey)
                || deltaScores.get(locationsKey) < deltaScore) {
            deltaScores.put(locationsKey, deltaScore);
        }
    }

    public void addAScore(ArrayList<Integer> locations, double aScore) {
        String locationsKey = getKey(locations);
        if (!aScores.containsKey(locationsKey)
                || aScores.get(locationsKey) > aScore) {
            aScores.put(locationsKey, aScore);
        }
    }

    public void addDeltaScore(String locationsKey, double deltaScore) {
        if (!deltaScores.containsKey(locationsKey)
                || deltaScores.get(locationsKey) < deltaScore) {
            deltaScores.put(locationsKey, deltaScore);
        }
    }

    public void addAScore(String locationsKey, double aScore) {
        if (!aScores.containsKey(locationsKey)
                || aScores.get(locationsKey) > aScore) {
            aScores.put(locationsKey, aScore);
        }
    }

    public ArrayList<String> getDeltaScorelocations() {
        return new ArrayList<String>(deltaScores.keySet());
    }

    public ArrayList<String> getAScorePostions() {
        return new ArrayList<String>(aScores.keySet());
    }

    public double getDeltaScore(String locationsKey) {
        return deltaScores.get(locationsKey);
    }

    public double getAScore(String locationsKey) {
        return aScores.get(locationsKey);
    }

    public void addAll(PtmScoring anotherScore) {
        for (String positions : anotherScore.getDeltaScorelocations()) {
            addDeltaScore(positions, anotherScore.getDeltaScore(positions));
        }
        for (String positions : anotherScore.getAScorePostions()) {
            addAScore(positions, anotherScore.getAScore(positions));
        }
    }

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
        }
        return result;
    }

    public static String getKey(ArrayList<Integer> locations) {
        Collections.sort(locations);
        String result = "";
        for (int loc : locations) {
            result += loc + separator;
        }
        return result;
    }
}
