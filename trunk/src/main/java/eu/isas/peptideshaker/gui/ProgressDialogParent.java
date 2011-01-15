package eu.isas.peptideshaker.gui;

/**
 * Interface that makes it simpler to let the cancellation of the progress 
 * bar propagate to the parent frame or dialog that opened the progrss bar.
 *
 * @author Harald Barsnes
 */
public interface ProgressDialogParent {

    /**
     * Cancel the process in the frame or dialog that opened the progress bar.
     */
    public void cancelProgress();
}
