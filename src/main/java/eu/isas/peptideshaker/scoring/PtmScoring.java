package eu.isas.peptideshaker.scoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains score about the PTM localization scoring.
 *
 * @author Marc Vaudel
 */
public class PtmScoring implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -3357368272501542941L;
    /**
     * Index indicating that the modification was not found.
     */
    public static final int NOT_FOUND = -1;
    /**
     * Index for a random location choice.
     */
    public static final int RANDOM = 0;
    /**
     * Index for a doubtful assignment.
     */
    public static final int DOUBTFUL = 1;
    /**
     * Index for a confident assignment.
     */
    public static final int CONFIDENT = 2;
    /**
     * Index for a very confident assignment.
     */
    public static final int VERY_CONFIDENT = 3;
    /**
     * Amino acid specific delta score. 1 is the first amino acid.
     */
    private HashMap<Integer, Double> deltaScoresAtAA;
    /**
     * Amino acid specific probabilistic score.
     */
    private HashMap<Integer, Double> probabilisticScoresAtAA;
    /**
     * The name of the modification of interest. 1 is the first amino acid.
     */
    private String ptmName;
    /**
     * The separator used to separate locations in the modification location
     * key.
     */
    public static final String separator = "|";
    /**
     * The retained PTM site assignment.
     */
    private HashMap<Integer, Integer> ptmLocationAtAA;

    /**
     * Default constructor Constructor.
     */
    public PtmScoring() {
    }

    /**
     * Constructor.
     *
     * @param ptmName the name of the PTM of interest.
     */
    public PtmScoring(String ptmName) {
        this.ptmName = ptmName;
    }

    /**
     * Returns the name of the inspected protein.
     *
     * @return the name of the inspected protein
     */
    public String getName() {
        return ptmName;
    }

    /**
     * Returns a set of sites where a score is available.
     *
     * @return a set of sites where a score is available
     */
    public Set<Integer> getScoredSites() {
        Set<Integer> dSites = getDSites();
        Set<Integer> pSites = getProbabilisticSites();
        Set<Integer> result = new HashSet<Integer>(dSites.size() + pSites.size());
        result.addAll(dSites);
        result.addAll(pSites);
        return result;
    }

    /**
     * Sets the delta score at a given site. First amino acid is 1.
     *
     * @param site the modification site
     * @param score the delta score
     */
    public void setDeltaScore(int site, double score) {
        if (deltaScoresAtAA == null) {
            deltaScoresAtAA = new HashMap<Integer, Double>(1);
        }
        deltaScoresAtAA.put(site, score);
    }

    /**
     * Returns the delta score at a given site. First amino acid is 1.
     *
     * @param site the site of interest
     * @return the attached delta score. 0 if not found.
     */
    public double getDeltaScore(int site) {
        if (deltaScoresAtAA == null) {
            return 0.0;
        }
        Double score = deltaScoresAtAA.get(site);
        if (score == null) {
            return 0.0;
        } else {
            return score;
        }
    }

    /**
     * Sets the probabilistic score at a given site. First amino acid is 1.
     *
     * @param site the modification site
     * @param score the delta score
     */
    public void setProbabilisticScore(int site, double score) {
        if (probabilisticScoresAtAA == null) {
            probabilisticScoresAtAA = new HashMap<Integer, Double>(1);
        }
        probabilisticScoresAtAA.put(site, score);
    }

    /**
     * Returns the probabilistic score at a given site. First amino acid is 1.
     *
     * @param site the site of interest
     * @return the attached probabilistic score. 0 if not found.
     */
    public double getProbabilisticScore(int site) {
        if (probabilisticScoresAtAA == null) {
            return 0.0;
        }
        Double score = probabilisticScoresAtAA.get(site);
        if (score == null) {
            return 0.0;
        } else {
            return score;
        }
    }

    /**
     * Returns a set of sites where a probabilistic score is available
     *
     * @return a set of sites where a probabilistic score is available
     */
    public Set<Integer> getProbabilisticSites() {
        if (probabilisticScoresAtAA == null) {
            return new HashSet<Integer>(0);
        }
        return probabilisticScoresAtAA.keySet();
    }

    /**
     * Returns an ordered list of sites where the probabilistic score was used.
     * Sites are ordered by decreasing score. In order to reduce systematic
     * error, if sites score equally their order is random.
     *
     * @return a list of sites where the probabilistic score was used
     */
    public ArrayList<Integer> getOrderedProbabilisticSites() {
        HashMap<Double, ArrayList<Integer>> siteMap = new HashMap<Double, ArrayList<Integer>>(1);
        if (probabilisticScoresAtAA != null) {
            for (int site : probabilisticScoresAtAA.keySet()) {
                double score = probabilisticScoresAtAA.get(site);
                ArrayList<Integer> sitesAtAA = siteMap.get(score);
                if (sitesAtAA == null) {
                    sitesAtAA = new ArrayList<Integer>();
                    siteMap.put(score, sitesAtAA);
                }
                sitesAtAA.add(site);
            }
        }
        ArrayList<Double> scores = new ArrayList<Double>(siteMap.keySet());
        Collections.sort(scores, Collections.reverseOrder());
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (double score : scores) {
            ArrayList<Integer> sites = siteMap.get(score);
            if (sites.size() > 2) {
                Collections.shuffle(sites);
            }
            result.addAll(sites);
        }
        return result;
    }

    /**
     * Returns a set of sites where a D-score is available
     *
     * @return a set of sites where a D-score is available
     */
    public Set<Integer> getDSites() {
        if (deltaScoresAtAA == null) {
            return new HashSet<Integer>(0);
        }
        return deltaScoresAtAA.keySet();
    }

    /**
     * Returns an ordered list of sites where the D-score was used. Sites are
     * ordered by decreasing score. In order to reduce systematic error, if
     * sites score equally their order is random.
     *
     * @return a list of sites where the D-score was used
     */
    public ArrayList<Integer> getOrderedDSites() {
        HashMap<Double, ArrayList<Integer>> siteMap = new HashMap<Double, ArrayList<Integer>>(1);
        if (deltaScoresAtAA != null) {
            for (int site : deltaScoresAtAA.keySet()) {
                double score = deltaScoresAtAA.get(site);
                ArrayList<Integer> sitesAtAA = siteMap.get(score);
                if (sitesAtAA == null) {
                    sitesAtAA = new ArrayList<Integer>();
                    siteMap.put(score, sitesAtAA);
                }
                sitesAtAA.add(site);
            }
        }
        ArrayList<Double> scores = new ArrayList<Double>(siteMap.keySet());
        Collections.sort(scores, Collections.reverseOrder());
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (double score : scores) {
            ArrayList<Integer> sites = siteMap.get(score);
            if (sites.size() > 2) {
                Collections.shuffle(sites);
            }
            result.addAll(sites);
        }
        return result;
    }

    /**
     * Adds all scorings from another score if better.
     *
     * @param anotherScore another score
     */
    public void addAll(PtmScoring anotherScore) {
        for (int position : anotherScore.getDSites()) {
            double newScore = anotherScore.getDeltaScore(position);
            if (getDeltaScore(position) < newScore) {
                setDeltaScore(position, newScore);
            }
        }
        for (int position : anotherScore.getProbabilisticSites()) {
            double newScore = anotherScore.getProbabilisticScore(position);
            if (getProbabilisticScore(position) < newScore) {
                setProbabilisticScore(position, newScore);
            }
        }
        HashMap<Integer, Integer> map = anotherScore.getPtmLocationAtAA();
        if (ptmLocationAtAA == null) {
            ptmLocationAtAA = new HashMap<Integer, Integer>(map.size());
        }
        for (int otherSite : map.keySet()) {
            Integer currentSiteConfidence = ptmLocationAtAA.get(otherSite);
            if (currentSiteConfidence == null) {
                ptmLocationAtAA.put(otherSite, map.get(otherSite));
            } else {
                ptmLocationAtAA.put(otherSite, Math.max(currentSiteConfidence, map.get(otherSite)));
            }
        }
    }

    /**
     * Sets the confidence level of a modification site. 1 is the first amino
     * acid.
     *
     * @param site the modification site
     * @param confidenceLevel the confidence level
     */
    public void setSiteConfidence(int site, int confidenceLevel) {
        if (ptmLocationAtAA == null) {
            ptmLocationAtAA = new HashMap<Integer, Integer>(1);
        }
        ptmLocationAtAA.put(site, confidenceLevel);
    }

    /**
     * Returns the map of the localization. site &gt; confidence level.
     *
     * @return the map of the localization
     */
    public HashMap<Integer, Integer> getPtmLocationAtAA() {
        if (ptmLocationAtAA == null) {
            return new HashMap<Integer, Integer>(0);
        }
        return ptmLocationAtAA;
    }

    /**
     * Returns the sites of all localized PTMs.
     *
     * @return the sites of all localized PTMs
     */
    public Set<Integer> getAllPtmLocations() {
        if (ptmLocationAtAA == null) {
            return new HashSet<Integer>(0);
        }
        return ptmLocationAtAA.keySet();
    }

    /**
     * Returns sites of all localized PTMs ordered increasingly.
     *
     * @return sites of all localized PTMs ordered increasingly
     */
    public ArrayList<Integer> getOrderedPtmLocations() {
        ArrayList<Integer> result = new ArrayList<Integer>(getAllPtmLocations());
        Collections.sort(result);
        return result;
    }

    /**
     * Returns the confidence of the PTM localization.
     *
     * @param site the modification site
     *
     * @return the confidence of the localization as indexed by the static
     * fields
     */
    public int getLocalizationConfidence(int site) {
        if (ptmLocationAtAA == null) {
            return NOT_FOUND;
        }
        Integer confidence = ptmLocationAtAA.get(site);
        if (confidence == null) {
            confidence = NOT_FOUND;
        }
        return confidence;
    }

    /**
     * Returns the minimal confidence among the PTM sites of this scoring.
     *
     * @return the minimal confidence among the PTM sites of this scoring
     */
    public int getMinimalLocalizationConfidence() {
        if (ptmLocationAtAA == null || ptmLocationAtAA.isEmpty()) {
            return NOT_FOUND;
        }
        int minConfidence = VERY_CONFIDENT;
        for (int confidence : ptmLocationAtAA.values()) {
            if (confidence < minConfidence) {
                confidence = minConfidence;
            }
        }
        return minConfidence;
    }

    /**
     * Returns the confidently and very confidently localized PTMs.
     *
     * @return the confidently and very confidently localized PTMs
     */
    public ArrayList<Integer> getConfidentPtmLocations() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        result.addAll(getPtmLocations(CONFIDENT));
        result.addAll(getPtmLocations(VERY_CONFIDENT));
        return result;
    }

    /**
     * Returns the not found, randomly or doubtfully localized PTMs.
     *
     * @return the not found, randomly or doubtfully localized PTMs
     */
    public ArrayList<Integer> getSecondaryPtmLocations() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        result.addAll(getPtmLocations(NOT_FOUND));
        result.addAll(getPtmLocations(RANDOM));
        result.addAll(getPtmLocations(DOUBTFUL));
        return result;
    }

    /**
     * Returns the PTM locations at a given confidence level (see static
     * fields).
     *
     * @param confidenceLevel the confidence level
     *
     * @return the PTM locations at the given confidence level
     */
    public ArrayList<Integer> getPtmLocations(int confidenceLevel) {
        ArrayList<Integer> result = new ArrayList<Integer>(1);
        if (ptmLocationAtAA != null) {
            for (int site : ptmLocationAtAA.keySet()) {
                if (confidenceLevel == ptmLocationAtAA.get(site)) {
                    result.add(site);
                }
            }
        }
        return result;
    }

    /**
     * Convenience method returning all confidence levels as string.
     *
     * @return an array with all confidence levels as string
     */
    public static String[] getPossibleConfidenceLevels() {
        String[] result = new String[5];
        result[0] = "Not Found";
        result[1] = "Random";
        result[2] = "Doubtful";
        result[3] = "Confident";
        result[4] = "Very Confident";
        return result;
    }

    /**
     * Convenience method returning the given confidence level as a string.
     *
     * @param index the confidence level
     * @return the corresponding string
     */
    public static String getConfidenceLevel(int index) {
        switch (index) {
            case -1:
                return "Not Found";
            case 0:
                return "Random";
            case 1:
                return "Doubtful";
            case 2:
                return "Confident";
            case 3:
                return "Very Confident";
            default:
                return "";
        }
    }
}
