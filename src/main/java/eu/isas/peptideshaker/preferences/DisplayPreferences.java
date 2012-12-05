package eu.isas.peptideshaker.preferences;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This class contains the display preferences for the current project.
 *
 * @author Marc Vaudel
 */
public class DisplayPreferences implements Serializable {

    /**
     * The serial number for serialization compatibility.
     */
    static final long serialVersionUID = -7923024103783392695L;
    /**
     * Show/hide the hidden proteins.
     */
    private boolean showHiddenProteins = true;
    /**
     * Show/hide the hidden proteins.
     */
    private boolean showScores = false;
    /**
     * The number of aa surrounding a peptide.
     */
    private Integer nAASurroundingPeptides = 2;
    /**
     * The displayed PTMs.
     */
    private HashMap<String, Boolean> displayedPTMs = new HashMap<String, Boolean>();

    /**
     * Constructor.
     */
    public DisplayPreferences() {
    }

    /**
     * Sets whether hidden proteins should be displayed.
     *
     * @param showHiddenProteins a boolean indicating whether hidden proteins
     * should be displayed
     */
    public void showHiddenProteins(boolean showHiddenProteins) {
        this.showHiddenProteins = showHiddenProteins;
    }

    /**
     * Returns whether hidden proteins should be displayed.
     *
     * @return true if the hidden proteins should be displayed
     */
    public boolean showHiddenProteins() {
        return showHiddenProteins;
    }

    /**
     * Sets whether scores should be displayed.
     *
     * @param showScores a boolean indicating whether scores should be displayed
     */
    public void showScores(boolean showScores) {
        this.showScores = showScores;
    }

    /**
     * Returns whether scores should be displayed.
     *
     * @return true of the scores are to be displayed
     */
    public boolean showScores() {
        return showScores;
    }

    /**
     * Returns the number of amino acids surrounding a peptide sequence (1 by
     * default).
     *
     * @return the number of amino acids surrounding a peptide sequence
     */
    public int getnAASurroundingPeptides() {
        if (nAASurroundingPeptides == null) {
            nAASurroundingPeptides = 2;
        }
        return nAASurroundingPeptides;
    }

    /**
     * Sets the number of amino acids surrounding a peptide sequence.
     *
     * @param nAASurroundingPeptides the number of amino acids surrounding a
     * peptide sequence
     */
    public void setnAASurroundingPeptides(int nAASurroundingPeptides) {
        this.nAASurroundingPeptides = nAASurroundingPeptides;
    }

    /**
     * Sets whether a PTM shall be displayed on the sequences or not.
     *
     * @param ptmName the name of the ptm
     * @param displayed a boolean indicating whether the PTM shall be displayed
     */
    public void setDisplayedPTM(String ptmName, boolean displayed) {
        if (displayedPTMs == null) {
            // Backward compatibility check
            displayedPTMs = new HashMap<String, Boolean>();
        }
        displayedPTMs.put(ptmName, displayed);
    }

    /**
     * Indicates whether a PTM shall be displayed on the interface.
     *
     * @param ptmName the name of the PTM
     * @return a boolean indicating whether the PTM shall be displayed
     */
    public boolean isDisplayedPTM(String ptmName) {
        if (displayedPTMs == null) {
            throw new IllegalArgumentException("The displayed PTM map is not set for this project.");
        }
        Boolean result = displayedPTMs.get(ptmName);
        if (result == null) {
            result = false;
            setDisplayedPTM(ptmName, result);
        }
        return result;
    }

    /**
     * Sets the variable modifications visible.
     *
     * @param modificationProfile the modification profile
     */
    public void setDefaultSelection(com.compomics.util.preferences.ModificationProfile modificationProfile) {
        for (String ptm : modificationProfile.getAllNotFixedModifications()) {
            setDisplayedPTM(ptm, true);
        }
    }

    /**
     * Verifies that the current object version has a display map and sets
     * variable modifications visible if not.
     *
     * @param modificationProfile the modification profile
     */
    public void compatibilityCheck(com.compomics.util.preferences.ModificationProfile modificationProfile) {
        if (displayedPTMs == null) {
            setDefaultSelection(modificationProfile);
        }
    }
}
