package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;

/**
 * This class achieves a pre-filtering of the identifications
 *
 * @author Marc Vaudel
 */
public class IdFilter {

    /**
     * The minimal peptide length allowed
     */
    private int minPepLength;
    /**
     * The maximal peptide length allowed
     */
    private int maxPepLength;
    /**
     * Mascot maximal e-value allowed
     */
    private double mascotMaxEvalue;
    /**
     * OMSSA maximal e-value allowed
     */
    private double omssaMaxEvalue;
    /**
     * X!Tandem maximal e-value allowed
     */
    private double xtandemMaxEvalue;
    /**
     * The maximal mass deviation allowed
     */
    private double maxMassDeviation;
    /**
     * Boolean indicating the unit of the allowed mass deviation (true: ppm, false: Da)
     */
    private boolean isPpm;

    /**
     * Constructor for an Identification filter
     *
     * @param minPepLength      The minimal peptide length allowed
     * @param maxPepLength      The maximal peptide length allowed
     * @param mascotMaxEvalue   The maximal Mascot e-value allowed
     * @param omssaMaxEvalue    The maximal OMSSA e-value allowed
     * @param xtandemMaxEvalue  The maximal X!Tandem e-value allowed
     * @param maxMassDeviation  The maximal mass deviation allowed
     * @param isPpm             Boolean indicating the unit of the allowed mass deviation (true: ppm, false: Da)
     */
    public IdFilter(int minPepLength, int maxPepLength, double mascotMaxEvalue, double omssaMaxEvalue, double xtandemMaxEvalue, double maxMassDeviation, boolean isPpm) {
        this.minPepLength = minPepLength;
        this.maxPepLength = maxPepLength;
        this.mascotMaxEvalue = mascotMaxEvalue;
        this.omssaMaxEvalue = omssaMaxEvalue;
        this.xtandemMaxEvalue = xtandemMaxEvalue;
        this.maxMassDeviation = maxMassDeviation;
        this.isPpm = isPpm;
    }
    
    /**
     * Returns the minimum peptide length.
     * 
     * @return the minimum peptide length
     */
    public int getMinPeptideLength () {
        return minPepLength;
    }
    
    /**
     * Returns the maxium peptide length.
     * 
     * @return the maxium peptide length
     */
    public int getMaxPeptideLength () {
        return maxPepLength;
    }

    /**
     * Validates a peptide assumption
     * 
     * @param assumption the considered peptide assumption
     * @return a boolean indicating whether the given assumption passes the filter
     */
    public boolean validateId(PeptideAssumption assumption) {
        if (isPpm) {
            if (Math.abs(assumption.getDeltaMass(isPpm)) > maxMassDeviation && maxMassDeviation > 0) {
                return false;
            } else if (Math.abs(assumption.getPeptide().getMass()-assumption.getMeasuredMass()) > 2) {
                int debug = 1;
            }
        } else {
            if (Math.abs(assumption.getMeasuredMass() - assumption.getPeptide().getMass()) > maxMassDeviation && maxMassDeviation > 0) {
                return false;
            }
        }
        
        int pepLength = assumption.getPeptide().getSequence().length();
        
        if ((pepLength > maxPepLength && maxPepLength != 0) || pepLength < minPepLength) {
            return false;
        }
        
        int searchEngine = assumption.getAdvocate();
        double eValue = assumption.getEValue();
        
        if (searchEngine == Advocate.MASCOT && eValue > mascotMaxEvalue
                || searchEngine == Advocate.OMSSA && eValue > omssaMaxEvalue
                || searchEngine == Advocate.XTANDEM && eValue > xtandemMaxEvalue) {
            return false;
        }
        
        boolean target = false;
        boolean decoy = false;
        
        for (String protein : assumption.getPeptide().getParentProteins()) {
            if (SequenceFactory.isDecoy(protein)) {
                decoy = true;
            } else {
                target = true;
            }
        }
        
        if (target && decoy) {
            return false;
        }
        
        return true;
    }
}
