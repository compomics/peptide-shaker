package eu.isas.peptideshaker.scoring;

import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.HashMap;

/**
 * Map containing the target/decoy information of every spectrum file separately.
 *
 * @deprecated has never been used
 * @author Marc Vaudel
 */
public class FractionsMap {

    /**
     * The target/decoy information.
 * @deprecated has never been used
     */
    private HashMap<String, TargetDecoyMap> targetDecoyMaps = new HashMap<String, TargetDecoyMap>();

    /**
     * Constructor.
     */
    public FractionsMap() {
    }
}
