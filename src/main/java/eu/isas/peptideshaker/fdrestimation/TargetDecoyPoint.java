package eu.isas.peptideshaker.fdrestimation;

import java.io.Serializable;

/**
 * This class represents a target/decoy hit in its simplest form
 *
 * @author Marc
 */
public class TargetDecoyPoint implements Serializable {

    /**
     * The number of target hits at this point
     */
    public int nTarget = 0;
    /**
     * The number of decoy hits at this point
     */
    public int nDecoy = 0;
    /**
     * The posterior error probability associated to this point
     */
    public double p;

    /**
     * constructor
     */
    public TargetDecoyPoint() {
    }
}
