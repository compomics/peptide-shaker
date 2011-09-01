package eu.isas.peptideshaker.preferences;

import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class contains the spectrum annotation preferences
 *
 * @author Marc
 */
public class AnnotationPreferences implements Serializable {

    /**
     * The intensity limit used when only the most intense peaks are to be 
     * annotated.
     */
    private double intensityLimit = 0.75;
    /**
     * boolean indicating whether only most intense peaks should be annotated
     */
    private boolean mostIntensePeaks;
    /**
     * Shall Peptide-Shaker use default annotation
     */
    private boolean defaultAnnotation;
    /**
     * The types of ions to annotate
     */
    private ArrayList<PeptideFragmentIonType> ionTypes = new ArrayList<PeptideFragmentIonType>();
    /**
     * The neutral losses searched for
     */
    private HashMap<NeutralLoss, Integer> neutralLosses = new HashMap<NeutralLoss, Integer>();
    /**
     * Shall neutral losses be only considered for ions containing amino acids of interest?
     */
    private boolean neutralLossesSequenceDependant;
    /**
     * the maximal fragment charge to be searched for
     */
    private ArrayList<Integer> selectedCharges = new ArrayList<Integer>();
    /**
     * m/z tolerance used for peak matching
     */
    private double mzTolerance;
    /**
     * The currently inspected peptide
     */
    private Peptide currentPeptide;
    /**
     * The charge of the currently inspected precursor
     */
    private int currentPrecursorCharge = 0;

    /**
     * Constructor
     */
    public AnnotationPreferences() {
    }

    /**
     * Sets the annotation settings for the current peptide and precursor charge
     */
    public void setCurrentSettings(Peptide currentPeptide, int currentPrecursorCharge) {
        boolean changed = false;
        if (this.currentPeptide == null 
                || !currentPeptide.isSameAs(this.currentPeptide) 
                || !currentPeptide.sameModificationsAs(this.currentPeptide)) {
            this.currentPeptide = currentPeptide;
            changed = true;
        }
        if (currentPrecursorCharge != this.currentPrecursorCharge) {
            this.currentPrecursorCharge = currentPrecursorCharge;
            changed = true;
        }
        if (changed) {
            updateSettings();
        }
    }

    /**
     * Updates the neutral losses and charge annotation settings
     */
    public void updateSettings() {
        if (defaultAnnotation) {
            selectedCharges.clear();
            for (int charge = 1; charge < currentPrecursorCharge; charge++) {
                selectedCharges.add(charge);
            }
            neutralLosses = SpectrumAnnotator.getDefaultLosses(currentPeptide);
        } else if (neutralLossesSequenceDependant) {
            neutralLosses = SpectrumAnnotator.getDefaultLosses(currentPeptide, new ArrayList<NeutralLoss>(neutralLosses.keySet()));
        }
    }

    /**
     * 
     * @param neutralLossesSequenceDependant 
     */
    public void setNeutralLossesSequenceDependant(boolean neutralLossesSequenceDependant) {
        this.neutralLossesSequenceDependant = neutralLossesSequenceDependant;
    }

    /**
     * returns whether neutral losses are considered only for amino acids of interest or not.
     * @return a boolean indicating whether neutral losses are considered only for amino acids of interest or not.
     */
    public boolean areNeutralLossesSequenceDependant() {
        return neutralLossesSequenceDependant;
    }

    /**
     * Returns the fragment ion charges considered for the desired precursor charge
     * @TODO rewrite this method as soon as the GUI has a better handling of charges
     * @return the fragment ion charges considered 
     */
    public ArrayList<Integer> getValidatedCharges() {
        return selectedCharges;
    }

    public void clearCharges() {
        selectedCharges.clear();
    }

    public void addSelectedCharge(int selectedCharge) {
        selectedCharges.add(selectedCharge);
    }

    /**
     * clears the considered neutral losses
     */
    public void clearNeutralLosses() {
        neutralLosses.clear();
    }

    /**
     * returns the considered neutral losses
     * @return the considered neutral losses 
     */
    public HashMap<NeutralLoss, Integer> getNeutralLosses() {
        return neutralLosses;
    }

    /**
     * adds a neutral loss
     * @param neutralLoss a new neutral loss
     */
    public void addNeutralLoss(NeutralLoss neutralLoss) {
        neutralLosses.put(neutralLoss, 1);
    }

    /**
     * returns the type of ions annotated
     * @return the type of ions annotated 
     */
    public ArrayList<PeptideFragmentIonType> getIonTypes() {
        return ionTypes;
    }

    /**
     * Clears the ion types annotated
     */
    public void clearIonTypes() {
        ionTypes.clear();
    }

    /**
     * Adds a new ion type to annotate
     * @param ionType a new ion type to annotate
     */
    public void addIonType(PeptideFragmentIonType ionType) {
        this.ionTypes.add(ionType);
    }

    /**
     * sets whether the default Peptide-Shaker annotation should be used
     * @param useDefaultAnnotation a boolean indicating whether the default Peptide-Shaker annotation should be used
     */
    public void useDefaultAnnotation(boolean defaultAnnotation) {
        this.defaultAnnotation = defaultAnnotation;
        if (defaultAnnotation) {
            neutralLossesSequenceDependant = true;
        }
    }

    /**
     * Returns whether the Peptide-Shaker default annotation should be used
     * @return a boolean indicating whether the Peptide-Shaker default annotation should be used
     */
    public boolean useDefaultAnnotation() {
        return defaultAnnotation;
    }

    /**
     * Sets whether only most intense peaks should be annotated
     * @param annotateMostIntensePeaks boolean indicating whether only most intense peaks should be annotated
     */
    public void annotateMostIntensePeaks(boolean annotateMostIntensePeaks) {
        mostIntensePeaks = annotateMostIntensePeaks;
    }

    /**
     * Returns whether only most intense peaks should be annotated
     * @return a boolean indicating whether only most intense peaks should be annotated
     */
    public boolean shallAnnotateMostIntensePeaks() {
        return mostIntensePeaks;
    }

    /**
     * Returns the m/z tolerance
     * @return the m/z tolerance
     */
    public double getMzTolerance() {
        return mzTolerance;
    }

    /**
     * Sets the m/z tolerance
     * @param mzTolerance the m/z tolerance
     */
    public void setMzTolerance(double mzTolerance) {
        this.mzTolerance = mzTolerance;
    }
    
    /**
     * Returns the current precursor charge
     * @return the current precursor charge
     */
    public int getCurrentPrecursorCharge() {
        return currentPrecursorCharge;
    }

    /**
     * Returns the intensity limit.
     * 
     * @return the intensityLimit
     */
    public double getAnnotationIntensityLimit() {
        return intensityLimit;
    }

    /**
     * Sets the intensity limit.
     * 
     * @param intensityLimit the intensityLimit to set
     */
    public void setAnnotationIntensityLimit(double intensityLimit) {
        this.intensityLimit = intensityLimit;
    }
}
