package eu.isas.peptideshaker.preferences;

import java.io.Serializable;

/**
 * This class contains the spectrum annotation preferences
 *
 * @author Marc
 */
public class AnnotationPreferences implements Serializable {

    /**
     * boolean indicating whether only most intense peaks should be annotated
     */
    private boolean mostIntensePeaks;

    /**
     * Constructor
     */
    public AnnotationPreferences() {
        mostIntensePeaks = true;
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
}
