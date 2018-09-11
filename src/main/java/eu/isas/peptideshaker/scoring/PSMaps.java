package eu.isas.peptideshaker.scoring;

import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.db.object.DbObject;
import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;

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
     * The PSM level map.
     */
    private TargetDecoyMap psmMap;
    /**
     * The peptide level map.
     */
    private TargetDecoyMap peptideMap;
    /**
     * The protein level map.
     */
    private TargetDecoyMap proteinMap;
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
     * @param inputMap the input map
     * @param psmMap the PSM level map
     * @param peptideMap the peptide level map
     * @param proteinMap the protein level map
     */
    public PSMaps(InputMap inputMap, TargetDecoyMap psmMap, TargetDecoyMap peptideMap, TargetDecoyMap proteinMap) {
        
        this.inputMap = inputMap;
        this.psmMap = psmMap;
        this.peptideMap = peptideMap;
        this.proteinMap = proteinMap;
        
    }

    /**
     * Returns the target decoy map of all search engine scores.
     *
     * @return the target decoy map of all search engine scores
     */
    public InputMap getInputMap() {
        
        readDBMode();
        
        return inputMap;
    
    }

    /**
     * Returns the target decoy map at the psm level.
     *
     * @return the target decoy map at the psm level
     */
    public TargetDecoyMap getPsmMap() {
        
        readDBMode();
        
        return psmMap;
    
    }

    /**
     * Returns the target decoy map at the peptide level.
     *
     * @return the target decoy map at the peptide level
     */
    public TargetDecoyMap getPeptideMap() {
        
        readDBMode();
        
        return peptideMap;
    
    }

    /**
     * Returns the target decoy map at the protein level.
     *
     * @return the target decoy map at the protein level
     */
    public TargetDecoyMap getProteinMap() {
        
        readDBMode();
        
        return proteinMap;
    
    }

    @Override
    public long getParameterKey() {
        return getId();
    }
}
