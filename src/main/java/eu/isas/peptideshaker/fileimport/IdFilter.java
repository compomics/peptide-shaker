package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.PeptideAssumption;

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
     * The maximal mass deviation allowed in ppm
     */
    private double maxMassDeviation;

    /**
     * Constructor for an Identification filter
     *
     * @param minPepLength      The minimal peptide length allowed
     * @param maxPepLength      The maximal peptide length allowed
     * @param mascotMaxEvalue   The maximal Mascot e-value allowed
     * @param omssaMaxEvalue    The maximal OMSSA e-value allowed
     * @param xtandemMaxEvalue  The maximal X!Tandem e-value allowed
     * @param maxMassDeviation  The maximal mass deviation allowed
     */
    public IdFilter(int minPepLength, int maxPepLength, double mascotMaxEvalue, double omssaMaxEvalue, double xtandemMaxEvalue, double maxMassDeviation) {
        this.minPepLength = minPepLength;
        this.maxPepLength = maxPepLength;
        this.mascotMaxEvalue = mascotMaxEvalue;
        this.omssaMaxEvalue = omssaMaxEvalue;
        this.xtandemMaxEvalue = xtandemMaxEvalue;
        this.maxMassDeviation = maxMassDeviation;
    }

    /**
     * Validates a peptide assumption
     * 
     * @param assumption the considered peptide assumption
     * @return a boolean indicating whether the given assumption passes the filter
     */
    public boolean validateId(PeptideAssumption assumption) {
        if (assumption.getDeltaMass() > maxMassDeviation && maxMassDeviation > 0) {
            return false;
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
        for (Protein protein : assumption.getPeptide().getParentProteins()) {
            if (protein.isDecoy()) {
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
