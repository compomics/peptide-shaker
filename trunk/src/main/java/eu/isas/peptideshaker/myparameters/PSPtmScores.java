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
 *
 * @author vaudel
 */
public class PSPtmScores implements UrParameter {
    

    private HashMap<String, PtmScoring> ptmMap = new HashMap<String, PtmScoring>();
    
    public PSPtmScores() {
        
    }
    
    public void addPtmScoring(String ptmName, PtmScoring ptmScoring) {
        ptmMap.put(ptmName, ptmScoring);
    }
    
    public PtmScoring getPtmScoring(String ptmName) {
        return ptmMap.get(ptmName);
    }
    
    public boolean containsPtm(String ptmName) {
        return ptmMap.containsKey(ptmName);
    }
    
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
