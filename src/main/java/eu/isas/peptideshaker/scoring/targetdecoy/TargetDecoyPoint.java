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
     */
    public int nTarget = 0; // @TODO make private
    /**
     * The number of decoy hits at this point.
     */
    public int nDecoy = 0; // @TODO make private
    /**
     * The posterior error probability associated to this point.
     */
    public double p; // @TODO make private

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
