package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.scoring.FractionsMap;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;

/**
 * This class contains all scoring maps from PeptideShaker and will be used to
 * store the information.
 *
 * @author Marc Vaudel
 */
public class PSMaps implements UrParameter {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = -7582248988590322280L;
    /**
     * The protein map
     */
    private ProteinMap proteinMap;
    /**
     * The PSM map
     */
    private PsmSpecificMap PsmSpecificMap;
    /**
     * The peptide map
     */
    private PeptideSpecificMap PeptideSpecificMap;
    /**
     * The target decoy map of all search engine scores
     */
    private InputMap inputMap;
    /**
     * The fractions map
     */
    private FractionsMap fractionsMap; // @TODO: this object should be made serializable

    /**
     * constructor
     */
    public PSMaps() {
    }

    /**
     * Constructor for the PSMaps.
     *
     * @param proteinMap The protein map
     * @param PsmSpecificMap The PSM map
     * @param PeptideSpecificMap The peptide map
     * @param inputMap The target decoy map of all search engine scores
     */
    public PSMaps(ProteinMap proteinMap, PsmSpecificMap PsmSpecificMap, PeptideSpecificMap PeptideSpecificMap, InputMap inputMap) {
        this.proteinMap = proteinMap;
        this.PeptideSpecificMap = PeptideSpecificMap;
        this.PsmSpecificMap = PsmSpecificMap;
        this.inputMap = inputMap;
    }

    /**
     * Getter for the peptide map.
     *
     * @return the peptide map
     */
    public PeptideSpecificMap getPeptideSpecificMap() {
        return PeptideSpecificMap;
    }

    /**
     * Getter for the psm map.
     *
     * @return the psm map
     */
    public PsmSpecificMap getPsmSpecificMap() {
        return PsmSpecificMap;
    }

    /**
     * getter for the protein map.
     *
     * @return the protein map
     */
    public ProteinMap getProteinMap() {
        return proteinMap;
    }
    
    /**
     * Returns the target decoy map of all search engine scores.
     * 
     * @return the target decoy map of all search engine scores
     */
    public InputMap getInputMap() {
        return inputMap;
    }

    /**
     * Getter for the fractions map.
     *
     * @deprecated not implemented
     * @return the fractions map
     */
    public FractionsMap getFractionsMap() {
        return fractionsMap;
    }

    @Override
    public String getFamilyName() {
        return "PeptideShaker";
    }

    @Override
    public int getIndex() {
        return 1;
    }
}
