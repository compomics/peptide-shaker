package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class contains the display preferences for the current project.
 *
 * @author Marc Vaudel
 */
public class DisplayPreferences implements Serializable {

    /**
     * the serial number for serialization compatibility
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
     * Show/hide sliders
     */
    private boolean showSliders = false;

    /**
     * Constructor
     */
    public DisplayPreferences() {
    }

    /**
     * Sets whether hidden proteins should be displayed
     * @param showHiddenProteins a boolean indicating whether hidden proteins should be displayed
     */
    public void showHiddenProteins(boolean showHiddenProteins) {
        this.showHiddenProteins = showHiddenProteins;
    }

    /**
     * Returns whether hidden proteins should be displayed
     * @return true if the hidden proteins should be displayed
     */
    public boolean showHiddenProteins() {
        return showHiddenProteins;
    }

    /**
     * Sets whether scores should be displayed
     * @param showScores a boolean indicating whether scores should be displayed
     */
    public void showScores(boolean showScores) {
        this.showScores = showScores;
    }

    /**
     * Returns whether scores should be displayed
     * @return true of the scores are to be displayed
     */
    public boolean showScores() {
        return showScores;
    }

    /**
     * Returns whether sliders should be displayed
     * @return  whether sliders should be displayed
     */
    public boolean showSliders() {
        return showSliders;
    }

    /**
     * Sets whether sliders should be displayed
     * @param showSliders  whether sliders should be displayed
     */
    public void setShowSliders(boolean showSliders) {
        this.showSliders = showSliders;
    }
}
