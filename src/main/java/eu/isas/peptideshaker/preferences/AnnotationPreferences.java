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
     * If true, the automatic y-axis zoom excludes background peaks. False 
     * includes all peaks in the auto zoom.
     */
    private boolean yAxisZoomExcludesBackgroundPeaks = true;
    /**
     * If true, the ion table is shown as an intensity versionm, false displays 
     * the standard Mascot version.
     */
    private boolean intensityIonTable = true;
    /**
     * If true, bars are shown in the bubble plot highlighting the ions.
     */
    private boolean showBars = false;
    /**
     * If true, all peaks are shown, false displays the annotated peaks, and 
     * the non-annotated in the background.
     */
    private boolean showAllPeaks = false;
    /**
     * The intensity limit used when only the most intense peaks are to be 
     * annotated.
     */
    private double intensityLimit = 0.25;
    /**
     * Shall PeptideShaker use automatic annotation
     */
    private boolean automaticAnnotation;
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
     * Fragment ion accuracy used for peak matching.
     */
    private double fragmentIonAccuracy;
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
     * Sets the annotation settings for the current peptide and precursor charge.
     * 
     * @param currentPeptide
     * @param currentPrecursorCharge  
     * @param newSpectrum
     */
    public void setCurrentSettings(Peptide currentPeptide, int currentPrecursorCharge, boolean newSpectrum) {
        
        this.currentPeptide = currentPeptide;
        this.currentPrecursorCharge = currentPrecursorCharge;
        
        if (newSpectrum && automaticAnnotation) {
            resetAutomaticAnnotation();
        } else if (neutralLossesSequenceDependant) {
            neutralLosses = SpectrumAnnotator.getDefaultLosses(currentPeptide);
        }
    }

    /**
     * Updates the neutral losses and charge annotation settings
     */
    public void resetAutomaticAnnotation() {
         
        selectedCharges.clear();
        for (int charge = 1; charge < currentPrecursorCharge; charge++) {
            selectedCharges.add(charge);
        }

        neutralLosses = SpectrumAnnotator.getDefaultLosses(currentPeptide);
    }

    /**
     * Returns whether neutral losses are considered only for amino acids of interest or not.
     * 
     * @return a boolean indicating whether neutral losses are considered only for amino acids of interest or not.
     */
    public boolean areNeutralLossesSequenceDependant() {
        return neutralLossesSequenceDependant;
    }

    public void setNeutralLossesSequenceDependant(boolean neutralLossesSequenceDependant) {
        this.neutralLossesSequenceDependant = neutralLossesSequenceDependant;
    }
    
    /**
     * Returns the fragment ion charges considered for the desired precursor charge
     * 
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
     * Sets whether the default PeptideShaker annotation should be used.
     * 
     * @param automaticAnnotation a boolean indicating whether the default PeptideShaker annotation should be used
     */
    public void useAutomaticAnnotation(boolean automaticAnnotation) {
        this.automaticAnnotation = automaticAnnotation;
        
        if (automaticAnnotation) {
            neutralLossesSequenceDependant = true;
        }
    }

    /**
     * Returns whether PeptideShaker should automatically set the annotations.
     * 
     * @return a boolean indicating whether PeptideShaker should automatically set the annotations
     */
    public boolean useAutomaticAnnotation() {
        return automaticAnnotation;
    }

    /**
     * Returns the fragment ion accuracy.
     * 
     * @return the fragment ion accuracy
     */
    public double getFragmentIonAccuracy() {
        return fragmentIonAccuracy;
    }

    /**
     * Sets the fragment ion accuracy.
     * 
     * @param fragmentIonAccuracy the fragment ion accuracy
     */
    public void setFragmentIonAccuracy(double fragmentIonAccuracy) {
        this.fragmentIonAccuracy = fragmentIonAccuracy;
    }
    
    /**
     * Returns the current precursor charge
     * @return the current precursor charge
     */
    public int getCurrentPrecursorCharge() {
        return currentPrecursorCharge;
    }

    /**
     * Returns the intensity limit. [0.0 - 1.0], where 1.0 means that all peaks 
     * are considered for annotations, while 0.3 means that only the 30% most 
     * intense peaks are considered for annotations.
     * 
     * @return the intensityLimit
     */
    public double getAnnotationIntensityLimit() {
        return intensityLimit;
    }

    /**
     * Sets the annotation level. [0.0 - 1.0], where 1.0 means that all peaks 
     * are considered for annotations, while 0.3 means that only the 30% most 
     * intense peaks are considered for annotations.
     * 
     * @param intensityLimit the intensityLimit to set
     */
    public void setAnnotationLevel(double intensityLimit) {
        this.intensityLimit = intensityLimit;
    }
    
    /**
     * If true, all peaks are shown, false displays the annotated peaks, and 
     * the non-annotated in the background.
     * 
     * @return true if all peaks are to be shown
     */
    public boolean showAllPeaks() {
        return showAllPeaks;
    }
    
    /**
     * Set if all peaks or just the annotated ones are to be shown.
     * 
     * @param showAllPeaks 
     */
    public void setShowAllPeaks(boolean showAllPeaks) {
        this.showAllPeaks = showAllPeaks;
    }
    
    /**
     * If true, bars are shown in the bubble plot highlighting the ions.
     * 
     * @return true if bars are to be shown in the bubble plot
     */
    public boolean showBars() {
        return showBars;
    }
    
    /**
     * Set if the bars in the bubble plot are to be shown or not
     * 
     * @param showBars 
     */
    public void setShowBars(boolean showBars) {
        this.showBars = showBars;
    }
    
    /**
     * If true, the ion table is shown as an intensity versionm, false displays 
     * the standard Mascot version.
     * 
     * @return if true, the ion table is shown as an intensity versionm, false displays 
     *         the standard Mascot version
     */
    public boolean useIntensityIonTable() {
        return intensityIonTable;
    }
    
    /**
     * Set if the intensity or m/z ion table should be shown.
     * 
     * @param intensityIonTable 
     */
    public void setIntensityIonTable(boolean intensityIonTable) {
        this.intensityIonTable = intensityIonTable;
    }
    
    /**
     * Returns true if the automatic y-axis zoom excludes background peaks. False 
     * if includes all peaks.
     * 
     * @return true if the automatic y-axis zoom excludes background peaks
     */
    public boolean yAxisZoomExcludesBackgroundPeaks() {
        return yAxisZoomExcludesBackgroundPeaks;
    }

    /**
     * Set if the automatic y-axis zoom only considers the anotated peaks.
     * 
     * @param yAxisZoomExcludesBackgroundPeaks
     */
    public void setYAxisZoomExcludesBackgroundPeaks(boolean yAxisZoomExcludesBackgroundPeaks) {
        this.yAxisZoomExcludesBackgroundPeaks = yAxisZoomExcludesBackgroundPeaks;
    }
}
