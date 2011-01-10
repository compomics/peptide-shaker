package eu.isas.peptideshaker.idimport;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.PeptideAssumption;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class IdFilter {

    private int minPepLength;
    private int maxPepLength;
    private double mascotMaxEvalue;
    private double omssaMaxEvalue;
    private double xtandemMaxEvalue;

    /**
     * @TODO: JavaDoc missing
     *
     * @param minPepLength
     * @param maxPepLength
     * @param mascotMaxEvalue
     * @param omssaMaxEvalue
     * @param xtandemMaxEvalue
     */
    public IdFilter(int minPepLength, int maxPepLength, double mascotMaxEvalue, double omssaMaxEvalue, double xtandemMaxEvalue) {
        this.minPepLength = minPepLength;
        this.maxPepLength = maxPepLength;
        this.mascotMaxEvalue = mascotMaxEvalue;
        this.omssaMaxEvalue = omssaMaxEvalue;
        this.xtandemMaxEvalue = xtandemMaxEvalue;
    }

    /**
     * @TODO: JavaDoc missing
     * 
     * @param assumption
     * @return
     */
    public boolean validateId(PeptideAssumption assumption) {
        int pepLength = assumption.getPeptide().getSequence().length();
        if (pepLength > maxPepLength || pepLength < minPepLength) {
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
