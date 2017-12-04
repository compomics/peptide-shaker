package eu.isas.peptideshaker.scoring;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.maps.PsmPTMMap;
import eu.isas.peptideshaker.scoring.maps.ChargeSpecificMap;

/**
 * This class contains all scoring maps from PeptideShaker and will be used to
 * store the information.
 *
 * @author Marc Vaudel
 */
public class PSMaps extends DbObject implements UrParameter {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -7582248988590322280L;
    /**
     * The protein map.
     */
    private ProteinMap proteinMap;
    /**
     * The PSM map.
     */
    private ChargeSpecificMap PsmSpecificMap;
    /**
     * The peptide map.
     */
    private PeptideSpecificMap PeptideSpecificMap;
    /**
     * The PSM level PTM map.
     */
    private PsmPTMMap psmPTMMap;
    /**
     * The target decoy map of all search engine scores.
     */
    private InputMap inputMap;

    /**
     * Constructor.
     */
    public PSMaps() {
    }

    /**
     * Constructor for the PSMaps.
     *
     * @param proteinMap the protein map
     * @param PsmSpecificMap the PSM map
     * @param PeptideSpecificMap the peptide map
     * @param inputMap the target decoy map of all search engine scores
     * @param psmPTMMap the PSM level PTM scoring map
     */
    public PSMaps(ChargeSpecificMap PsmSpecificMap, PeptideSpecificMap PeptideSpecificMap, ProteinMap proteinMap, InputMap inputMap, PsmPTMMap psmPTMMap) {
        this.proteinMap = proteinMap;
        this.PeptideSpecificMap = PeptideSpecificMap;
        this.PsmSpecificMap = PsmSpecificMap;
        this.inputMap = inputMap;
        this.psmPTMMap = psmPTMMap;
    }

    /**
     * Getter for the peptide map.
     *
     * @return the peptide map
     */
    public PeptideSpecificMap getPeptideSpecificMap() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return PeptideSpecificMap;
    }

    /**
     * Getter for the PSM map.
     *
     * @return the PSM map
     */
    public ChargeSpecificMap getPsmSpecificMap() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return PsmSpecificMap;
    }

    /**
     * Getter for the protein map.
     *
     * @return the protein map
     */
    public ProteinMap getProteinMap() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return proteinMap;
    }

    /**
     * Returns the target decoy map of all search engine scores.
     *
     * @return the target decoy map of all search engine scores
     */
    public InputMap getInputMap() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return inputMap;
    }

    /**
     * Returns the PSM level PTM scoring map.
     * 
     * @return the PSM level PTM scoring map
     */
    public PsmPTMMap getPsmPTMMap() {
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        return psmPTMMap;
    }

    /**
     * Sets the PSM level PTM scoring map.
     * 
     * @param psmPTMMap the PSM level PTM scoring map
     */
    public void setPsmPTMMap(PsmPTMMap psmPTMMap) {
        ObjectsDB.increaseRWCounter(); zooActivateWrite(); ObjectsDB.decreaseRWCounter();
        this.psmPTMMap = psmPTMMap;
    }

    @Override
    public long getParameterKey() {
        return getId();
    }
}
