package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class contains the scores for the locations of the possible modifications.
 *
 * @author Marc Vaudel
 */
public class PSPtmScores implements UrParameter {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = 7450340838299319636L;
    /**
     * A map containing all scores indexed by the modification of interest for a peptide or a PSM
     */
    private HashMap<String, PtmScoring> ptmMap = new HashMap<String, PtmScoring>();
    /**
     * A list of all modifications validated in a protein
     */
    private HashMap<Integer, ArrayList<String>> mainModificationSites = new HashMap<Integer, ArrayList<String>>();
    /**
     * A list of all secondary modifications in a protein
     */
    private HashMap<Integer, ArrayList<String>> secondaryModificationSites = new HashMap<Integer, ArrayList<String>>();

    /**
     * Constructor
     */
    public PSPtmScores() {
    }

    /**
     * Adds a scoring result for the modification of interest
     * @param ptmName       the modification of interest
     * @param ptmScoring    the corresponding scoring
     */
    public void addPtmScoring(String ptmName, PtmScoring ptmScoring) {
        ptmMap.put(ptmName, ptmScoring);
    }

    /**
     * Returns the ptm scoring for the desired modification (null if none found).
     * @param ptmName   the modification of interest
     * @return the scoring
     */
    public PtmScoring getPtmScoring(String ptmName) {
        return ptmMap.get(ptmName);
    }

    /**
     * indicates whether a modification has been already scored
     * @param ptmName the modification of interest
     * @return  a boolean indicating whether the modification is in the map
     */
    public boolean containsPtm(String ptmName) {
        return ptmMap.containsKey(ptmName);
    }

    /**
     * Returns a list of scored modifications
     * @return a list of scored modifications
     */
    public ArrayList<String> getScoredPTMs() {
        return new ArrayList<String>(ptmMap.keySet());
    }
    
    /**
     * Adds a main modification site and removes the corresponding secondary modification site if found
     * @param modification      the modification
     * @param modificationSite  the modification site
     */
    public void addMainModificationSite(String modification, int modificationSite) {
        if (!mainModificationSites.containsKey(modificationSite)) {
            mainModificationSites.put(modificationSite, new ArrayList<String>());
        }
        if (!mainModificationSites.get(modificationSite).contains(modification)) {
            mainModificationSites.get(modificationSite).add(modification);
        }
        if (secondaryModificationSites.containsKey(modificationSite)
                && secondaryModificationSites.get(modificationSite).contains(modification)) {
            secondaryModificationSites.get(modificationSite).remove(modification);
        }
    }
    
    /**
     * Adds a secondary modification site if not present in the main matches
     * @param modification      the modification to add
     * @param modificationSite  the modification site
     */
    public void addSecondaryModificationSite(String modification, int modificationSite) {
        if (!mainModificationSites.containsKey(modificationSite)
                || !mainModificationSites.get(modificationSite).contains(modification)) {
            if (!secondaryModificationSites.containsKey(modificationSite)) {
                secondaryModificationSites.put(modificationSite, new ArrayList<String>());
            }
            if (!secondaryModificationSites.get(modificationSite).contains(modification)) {
                secondaryModificationSites.get(modificationSite).add(modification);
            }
        }
    }
    
    /**
     * Returns the main potential modifications at the given amino acid index
     * @param aa the index in the sequence (0 is first amino acid)
     * @return a list containing all potential modifications as main match, an empty list if none foud
     */
    public ArrayList<String> getMainModificationsAt(int aa) {
        if (mainModificationSites.containsKey(aa)) {
            return mainModificationSites.get(aa);
        } else {
            return new ArrayList<String>();
        }
    }
    
    /**
     * Returns the secondary potential modifications at the given amino acid index
     * @param aa the index in the sequence (0 is first amino acid)
     * @return a list containing all potential modifications as main match, an empty list if none found
     */
    public ArrayList<String> getSecondaryModificationsAt(int aa) {
        if (secondaryModificationSites.containsKey(aa)) {
            return secondaryModificationSites.get(aa);
        } else {
            return new ArrayList<String>();
        }
    }

    /**
     * Returns the map of the main modification sites
     * @return the map of the main modification sites
     */
    public HashMap<Integer, ArrayList<String>> getMainModificationSites() {
        return mainModificationSites;
    }

    /**
     * Returns the map of the secondary modification sites
     * @return the map of the secondary modification sites
     */
    public HashMap<Integer, ArrayList<String>> getSecondaryModificationSites() {
        return secondaryModificationSites;
    }
    
    @Override
    public String getFamilyName() {
        return "PeptideShaker";
    }

    @Override
    public int getIndex() {
        return 3;
    }
}
