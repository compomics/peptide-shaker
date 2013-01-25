package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class achieves a pre-filtering of the identifications.
 *
 * @author Marc Vaudel
 */
public class IdFilter implements Serializable {

    /**
     * The minimal peptide length allowed.
     */
    private int minPepLength;
    /**
     * The maximal peptide length allowed.
     */
    private int maxPepLength;
    /**
     * Mascot maximal e-value allowed.
     */
    private double mascotMaxEvalue;
    /**
     * OMSSA maximal e-value allowed.
     */
    private double omssaMaxEvalue;
    /**
     * X!Tandem maximal e-value allowed.
     */
    private double xtandemMaxEvalue;
    /**
     * The maximal m/z deviation allowed.
     */
    private double maxMassDeviation;
    /**
     * Boolean indicating the unit of the allowed m/z deviation (true: ppm,
     * false: Da).
     */
    private boolean isPpm;
    /**
     * Boolean indicating whether peptides presenting unknown PTMs should be
     * ignored.
     */
    private boolean unknownPtm;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);

    /**
     * Constructor with default settings.
     */
    public IdFilter() {
        minPepLength = 6;
        maxPepLength = 30;
        mascotMaxEvalue = 100;
        omssaMaxEvalue = 100;
        xtandemMaxEvalue = 100;
        maxMassDeviation = 10;
        isPpm = true;
        unknownPtm = true;
    }

    /**
     * Constructor for an Identification filter.
     *
     * @param minPepLength The minimal peptide length allowed
     * @param maxPepLength The maximal peptide length allowed
     * @param mascotMaxEvalue The maximal Mascot e-value allowed
     * @param omssaMaxEvalue The maximal OMSSA e-value allowed
     * @param xtandemMaxEvalue The maximal X!Tandem e-value allowed
     * @param maxMzDeviation The maximal m/z deviation allowed
     * @param isPpm Boolean indicating the unit of the allowed m/z deviation
     * (true: ppm, false: Da)
     * @param unknownPTM Shall peptides presenting unknownPTMs be ignored
     */
    public IdFilter(int minPepLength, int maxPepLength, double mascotMaxEvalue, double omssaMaxEvalue, double xtandemMaxEvalue, double maxMzDeviation, boolean isPpm, boolean unknownPTM) {
        this.minPepLength = minPepLength;
        this.maxPepLength = maxPepLength;
        this.mascotMaxEvalue = mascotMaxEvalue;
        this.omssaMaxEvalue = omssaMaxEvalue;
        this.xtandemMaxEvalue = xtandemMaxEvalue;
        this.maxMassDeviation = maxMzDeviation;
        this.isPpm = isPpm;
        this.unknownPtm = unknownPTM;
    }

    /**
     * Validates the peptide assumption based on the peptide length and maximal
     * e-values allowed.
     *
     * @param assumption the assumption to validate
     * @return a boolean indicating whether the assumption passed the test
     */
    public boolean validatePeptideAssumption(PeptideAssumption assumption) {

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
        if (assumption.getPeptide().getParentProteins().size() > 1) {
            boolean target = false;
            boolean decoy = false;
            ArrayList<String> parentProteins = assumption.getPeptide().getParentProteins();
            for (String protein : parentProteins) {
                if (SequenceFactory.isDecoy(protein)) {
                    decoy = true;
                } else {
                    target = true;
                }
            }
            if (target && decoy) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the modifications of a peptide.
     *
     * @param peptide the peptide of interest
     * @return a boolean indicating whether the peptide passed the test
     */
    public boolean validateModifications(Peptide peptide) {

        // check it it's an unknown peptide
        if (unknownPtm) {
            ArrayList<ModificationMatch> modificationMatches = peptide.getModificationMatches();
            for (ModificationMatch modMatch : modificationMatches) {
                if (modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                    return false;
                }
            }
        }

        PTMFactory ptmFactory = PTMFactory.getInstance();

        // check if there are more ptms than ptm sites
        HashMap<String, ArrayList<ModificationMatch>> modMatches = new HashMap<String, ArrayList<ModificationMatch>>();
        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            if (modMatch.isVariable()) {
                String modName = modMatch.getTheoreticPtm();
                PTM ptm = ptmFactory.getPTM(modName);
                if (ptm.getType() == PTM.MODAA) {
                    if (!modMatches.containsKey(modName)) {
                        modMatches.put(modName, new ArrayList<ModificationMatch>());
                    }
                    modMatches.get(modName).add(modMatch);
                }
            }
        }

        ArrayList<ModificationMatch> modificationMatches = peptide.getModificationMatches();

        for (String modName : modMatches.keySet()) {
            for (ModificationMatch modMatch : modificationMatches) {
                try {
                    ArrayList<Integer> possiblePositions = peptide.getPotentialModificationSites(ptmFactory.getPTM(modMatch.getTheoreticPtm()));
                    if (possiblePositions.size() < modMatches.get(modName).size()) {
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates the mass deviation of a peptide assumption.
     *
     * @param assumption the considered peptide assumption
     * @param spectrumKey the key of the spectrum used to get the precursor the
     * precursor should be accessible via the spectrum factory
     *
     * @return a boolean indicating whether the given assumption passes the
     * filter
     * @throws IOException
     * @throws MzMLUnmarshallerException
     */
    public boolean validatePrecursor(PeptideAssumption assumption, String spectrumKey) throws IOException, MzMLUnmarshallerException {

        Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

        if (Math.abs(assumption.getDeltaMass(precursor.getMz(), isPpm)) > maxMassDeviation && maxMassDeviation > 0) {
            return false;
        }

        return true;
    }

    /**
     * Returns a boolean indicating whether unkown PTMs shall be removed.
     *
     * @return a boolean indicating whether unkown PTMs shall be removed
     */
    public boolean removeUnknownPTMs() {
        return unknownPtm;
    }

    /**
     * Indicates whether the mass tolerance is in ppm (true) or Dalton (false).
     *
     * @return a boolean indicating whether the mass tolerance is in ppm (true)
     * or Dalton (false)
     */
    public boolean isIsPpm() {
        return isPpm;
    }

    /**
     * Sets whether the mass tolerance is in ppm (true) or Dalton (false).
     *
     * @param isPpm a boolean indicating whether the mass tolerance is in ppm
     * (true) or Dalton (false)
     */
    public void setIsPpm(boolean isPpm) {
        this.isPpm = isPpm;
    }

    /**
     * Returns the maximal Mascot e-value allowed.
     *
     * @return the maximal Mascot e-value allowed
     */
    public double getMascotMaxEvalue() {
        return mascotMaxEvalue;
    }

    /**
     * Sets the maximal Mascot e-value allowed.
     *
     * @param mascotMaxEvalue the maximal Mascot e-value allowed
     */
    public void setMascotMaxEvalue(double mascotMaxEvalue) {
        this.mascotMaxEvalue = mascotMaxEvalue;
    }

    /**
     * Returns the maximal m/z deviation allowed.
     *
     * @return the maximal mass deviation allowed
     */
    public double getMaxMzDeviation() {
        return maxMassDeviation;
    }

    /**
     * Sets the maximal m/z deviation allowed.
     *
     * @param maxMzDeviation the maximal mass deviation allowed
     */
    public void setMaxMzDeviation(double maxMzDeviation) {
        this.maxMassDeviation = maxMzDeviation;
    }

    /**
     * Returns the maximal peptide length allowed.
     *
     * @return the maximal peptide length allowed
     */
    public int getMaxPepLength() {
        return maxPepLength;
    }

    /**
     * Sets the maximal peptide length allowed.
     *
     * @param maxPepLength the maximal peptide length allowed
     */
    public void setMaxPepLength(int maxPepLength) {
        this.maxPepLength = maxPepLength;
    }

    /**
     * Returns the maximal peptide length allowed.
     *
     * @return the maximal peptide length allowed
     */
    public int getMinPepLength() {
        return minPepLength;
    }

    /**
     * Sets the maximal peptide length allowed.
     *
     * @param minPepLength the maximal peptide length allowed
     */
    public void setMinPepLength(int minPepLength) {
        this.minPepLength = minPepLength;
    }

    /**
     * Returns the OMSSA maximal e-value allowed.
     *
     * @return the OMSSA maximal e-value allowed
     */
    public double getOmssaMaxEvalue() {
        return omssaMaxEvalue;
    }

    /**
     * Sets the OMSSA maximal e-value allowed.
     *
     * @param omssaMaxEvalue the OMSSA maximal e-value allowed
     */
    public void setOmssaMaxEvalue(double omssaMaxEvalue) {
        this.omssaMaxEvalue = omssaMaxEvalue;
    }

    /**
     * Returns the maximal X!Tandem e-value allowed.
     *
     * @return the OMSSA maximal e-value allowed
     */
    public double getXtandemMaxEvalue() {
        return xtandemMaxEvalue;
    }

    /**
     * Sets the OMSSA maximal e-value allowed.
     *
     * @param xtandemMaxEvalue the OMSSA maximal e-value allowed
     */
    public void setXtandemMaxEvalue(double xtandemMaxEvalue) {
        this.xtandemMaxEvalue = xtandemMaxEvalue;
    }
}
