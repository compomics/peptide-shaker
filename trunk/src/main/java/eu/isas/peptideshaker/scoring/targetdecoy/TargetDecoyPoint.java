package eu.isas.peptideshaker.scoring.targetdecoy;

import java.io.Serializable;

/**
 * This class represents a target/decoy hit in its simplest form
 *
 * @author Marc
 */
public class TargetDecoyPoint implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = 1030681250987827768L;
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
