package eu.isas.peptideshaker.scoring;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.HashMap;

/**
 * Map containing the target/decoy information of every spectrum file separately.
 *
 * @author Marc Vaudel
 */
public class FractionsMap {

    /**
     * The target/decoy information.
     */
    private HashMap<String, TargetDecoyMap> targetDecoyMaps = new HashMap<String, TargetDecoyMap>();

    /**
     * Constructor.
     */
    public FractionsMap() {
    }
}
