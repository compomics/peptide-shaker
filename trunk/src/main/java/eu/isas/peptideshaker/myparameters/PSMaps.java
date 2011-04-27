package eu.isas.peptideshaker.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.ProteinMap;
import eu.isas.peptideshaker.fdrestimation.PsmSpecificMap;

/**
 * This class contains all scoring maps from PeptideShaker and will be used to store the information
 *
 * @author Marc
 */
public class PSMaps implements UrParameter {

    /**
     * The protein map
     */
    private ProteinMap proteinMap;
    /**
     * The Psm map
     */
    private PsmSpecificMap PsmSpecificMap;
    /**
     * The peptide map
     */
    private PeptideSpecificMap PeptideSpecificMap;

    /**
     * constructor
     */
    public PSMaps() {

    }

    /**
     * Constructor for the PSMaps
     * @param proteinMap            The protein map
     * @param PsmSpecificMap        The psm map
     * @param PeptideSpecificMap    the peptide map
     */
    public PSMaps(ProteinMap proteinMap, PsmSpecificMap PsmSpecificMap, PeptideSpecificMap PeptideSpecificMap) {
        this.proteinMap = proteinMap;
        this.PeptideSpecificMap = PeptideSpecificMap;
        this.PsmSpecificMap = PsmSpecificMap;
    }

    /**
     * Getter for the peptide map
     * @return the peptide map
     */
    public PeptideSpecificMap getPeptideSpecificMap() {
        return PeptideSpecificMap;
    }

    /**
     * Getter for the psm map
     * @return the psm map
     */
    public PsmSpecificMap getPsmSpecificMap() {
        return PsmSpecificMap;
    }

    /**
     * getter for the protein map
     * @return the protein map
     */
    public ProteinMap getProteinMap() {
        return proteinMap;
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
