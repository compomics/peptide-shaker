package eu.isas.peptideshaker.preferences;

/**
 * This class contains the spectrum annotation preferences
 *
 * @author Marc
 */
public class AnnotationPreferences {

    private boolean mostIntensePeaks;

    public AnnotationPreferences() {
        mostIntensePeaks = true;
    }

    public void annotateMostIntensePeaks(boolean annotateMostIntensePeaks) {
        mostIntensePeaks = annotateMostIntensePeaks;
    }

    public boolean shallAnnotateMostIntensePeaks() {
        return mostIntensePeaks;
    }
}
