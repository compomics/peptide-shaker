package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import eu.isas.peptideshaker.filtering.MatchFilter;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class achieves a pre-filtering of the identifications
 *
 * @author Marc Vaudel
 */
public class IdFilter implements Serializable {

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
     * Constructor with default settings
     */
    public IdFilter() {
        minPepLength = 6;
        maxPepLength = 20;
        mascotMaxEvalue = 10;
        omssaMaxEvalue = 10;
        xtandemMaxEvalue = 10;
        maxMassDeviation = 10;
        isPpm = true;
    }
    
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
     * Validates a peptide assumption
     * 
     * @param assumption the considered peptide assumption
     * @return a boolean indicating whether the given assumption passes the filter
     */
    public boolean validateId(PeptideAssumption assumption, double precursorMass) {
        if (Math.abs(assumption.getDeltaMass(precursorMass, isPpm)) > maxMassDeviation && maxMassDeviation > 0) {
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

    /**
     * Indicates whether the mass tolerance is in ppm (true) or Dalton (false)
     * @return a boolean indicating whether the mass tolerance is in ppm (true) or Dalton (false)
     */
    public boolean isIsPpm() {
        return isPpm;
    }

    /**
     * Sets whether the mass tolerance is in ppm (true) or Dalton (false)
     * @param isPpm a boolean indicating whether the mass tolerance is in ppm (true) or Dalton (false)
     */
    public void setIsPpm(boolean isPpm) {
        this.isPpm = isPpm;
    }

    /**
     * Returns the maximal Mascot e-value allowed
     * @return the maximal Mascot e-value allowed 
     */
    public double getMascotMaxEvalue() {
        return mascotMaxEvalue;
    }

    /**
     * Sets  the maximal Mascot e-value allowed
     * @param mascotMaxEvalue  the maximal Mascot e-value allowed
     */
    public void setMascotMaxEvalue(double mascotMaxEvalue) {
        this.mascotMaxEvalue = mascotMaxEvalue;
    }

    /**
     * Returns the maximal mass deviation allowed
     * @return the maximal mass deviation allowed 
     */
    public double getMaxMassDeviation() {
        return maxMassDeviation;
    }

    /**
     * Sets  the maximal mass deviation allowed
     * @param maxMassDeviation  the maximal mass deviation allowed
     */
    public void setMaxMassDeviation(double maxMassDeviation) {
        this.maxMassDeviation = maxMassDeviation;
    }

    /**
     * Returns the maximal peptide length allowed
     * @return the maximal peptide length allowed 
     */
    public int getMaxPepLength() {
        return maxPepLength;
    }

    /**
     * Sets the maximal peptide length allowed
     * @param maxPepLength  the maximal peptide length allowed
     */
    public void setMaxPepLength(int maxPepLength) {
        this.maxPepLength = maxPepLength;
    }

    /**
     * Returns the maximal peptide length allowed
     * @return the maximal peptide length allowed 
     */
    public int getMinPepLength() {
        return minPepLength;
    }

    /**
     * Sets the maximal peptide length allowed
     * @param minPepLength  the maximal peptide length allowed
     */
    public void setMinPepLength(int minPepLength) {
        this.minPepLength = minPepLength;
    }

    /**
     * Returns the OMSSA maximal e-value allowed
     * @return the OMSSA maximal e-value allowed 
     */
    public double getOmssaMaxEvalue() {
        return omssaMaxEvalue;
    }

    /**
     * Sets the OMSSA maximal e-value allowed
     * @param omssaMaxEvalue  the OMSSA maximal e-value allowed
     */
    public void setOmssaMaxEvalue(double omssaMaxEvalue) {
        this.omssaMaxEvalue = omssaMaxEvalue;
    }

    /**
     * Returns the maximal X!Tandem e-value allowed
     * @return  the OMSSA maximal e-value allowed
     */
    public double getXtandemMaxEvalue() {
        return xtandemMaxEvalue;
    }

    /**
     * Sets the OMSSA maximal e-value allowed
     * @param xtandemMaxEvalue  the OMSSA maximal e-value allowed
     */
    public void setXtandemMaxEvalue(double xtandemMaxEvalue) {
        this.xtandemMaxEvalue = xtandemMaxEvalue;
    }
}
