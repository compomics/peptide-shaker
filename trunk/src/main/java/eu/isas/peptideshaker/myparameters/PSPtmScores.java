/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class contains the scores for the locations of the possible modifications.
 *
 * @author Marc
 */
public class PSPtmScores implements UrParameter {
    
    //@TODO: serialization number
    /**
     * A map containing all scores indexed by the modification of interest
     */
    private HashMap<String, PtmScoring> ptmMap = new HashMap<String, PtmScoring>();
    
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
    
    @Override
    public String getFamilyName() {
        return "PeptideShaker";
    }

    @Override
    public int getIndex() {
        return 3;
    }
}
