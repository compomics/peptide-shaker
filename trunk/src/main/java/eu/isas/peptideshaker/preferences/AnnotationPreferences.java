package eu.isas.peptideshaker.preferences;

/**
 * This class contains the spectrum annotation preferences
 *
 * @author Marc
 */
public class AnnotationPreferences {

    private double mzTolerance;
    private boolean mostIntensePeaks;

    public AnnotationPreferences() {
        mzTolerance = 0.5;
        mostIntensePeaks = true;
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
