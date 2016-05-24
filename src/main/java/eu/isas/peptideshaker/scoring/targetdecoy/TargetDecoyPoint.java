package eu.isas.peptideshaker.scoring.targetdecoy;

import java.io.Serializable;

/**
 * This class represents a target/decoy hit in its simplest form.
 *
 * @author Marc Vaudel
 */
public class TargetDecoyPoint implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = 1030681250987827768L;
    /**
     * The number of target hits at this point.
     * @TODO make private
     */
    public int nTarget = 0;
    /**
     * The number of decoy hits at this point.
     * @TODO make private
     */
    public int nDecoy = 0;
    /**
     * The posterior error probability associated to this point.
     * @TODO make private
     */
    public double p;

    /**
     * Constructor.
     */
    public TargetDecoyPoint() {
    }
    
    /**
     * Increases the target counter.
     */
    public synchronized void increaseTarget() {
        nTarget++;
    }
    
    /**
     * Increases the decoy counter.
     */
    public synchronized void increaseDecoy() {
        nDecoy++;
    }
    
    /**
     * Decreases the target counter.
     */
    public synchronized void decreaseTarget() {
        nTarget--;
    }
    
    /**
     * Decreases the decoy counter.
     */
    public synchronized void decreaseDecoy() {
        nDecoy--;
    }
}
