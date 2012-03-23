/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.peptideshaker.scoring;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.HashMap;

/**
 * Map containing the target/decoy information of every spectrum file separately
 *
 * @author marc
 */
public class FractionsMap {

    private HashMap<String, TargetDecoyMap> targetDecoyMaps = new HashMap<String, TargetDecoyMap>();
    
    public FractionsMap() {
        
    }
    
    
    
    
}
