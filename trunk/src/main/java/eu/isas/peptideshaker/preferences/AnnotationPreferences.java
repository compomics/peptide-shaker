package eu.isas.peptideshaker.preferences;

import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import java.util.HashMap;

/**
 * This class contains the spectrum annotation preferences
 *
 * @author Marc
 */
public class AnnotationPreferences {

    private HashMap<Integer, Boolean> selectedIons;
    private double mzTolerance;
    private boolean mostIntensePeaks;

    public AnnotationPreferences() {
        selectedIons = new HashMap<Integer, Boolean>();
        selectedIons.put(PeptideFragmentIon.A_ION, true);
        selectedIons.put(PeptideFragmentIon.AH2O_ION, true);
        selectedIons.put(PeptideFragmentIon.ANH3_ION, true);
        selectedIons.put(PeptideFragmentIon.B_ION, true);
        selectedIons.put(PeptideFragmentIon.BNH3_ION, true);
        selectedIons.put(PeptideFragmentIon.BH2O_ION, true);
        selectedIons.put(PeptideFragmentIon.C_ION, true);
        selectedIons.put(PeptideFragmentIon.C_ION, true);
        selectedIons.put(PeptideFragmentIon.X_ION, true);
        selectedIons.put(PeptideFragmentIon.Y_ION, true);
        selectedIons.put(PeptideFragmentIon.YH2O_ION, true);
        selectedIons.put(PeptideFragmentIon.YNH3_ION, true);
        selectedIons.put(PeptideFragmentIon.Z_ION, true);
        selectedIons.put(PeptideFragmentIon.MH_ION, true);
        selectedIons.put(PeptideFragmentIon.MHH2O_ION, true);
        selectedIons.put(PeptideFragmentIon.MHNH3_ION, true);
        mzTolerance = 0.5;
        mostIntensePeaks = true;
    }

    public boolean isSelected(int ionType) {
        return selectedIons.get(ionType);
    }

    public void setSelected(int ionType, boolean selected) {
        selectedIons.put(ionType, selected);
    }

    public double getTolerance() {
        return mzTolerance;
    }

    public void setTolerance(double mzTolerance) {
        this.mzTolerance = mzTolerance;
    }

    public void annotateMostIntensePeaks(boolean annotateMostIntensePeaks) {
        mostIntensePeaks = annotateMostIntensePeaks;
    }

    public boolean shallAnnotateMostIntensePeaks() {
        return mostIntensePeaks;
    }
}
