package eu.isas.peptideshaker.gui;

import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.gui.interfaces.WaitingHandler;
import java.io.File;
import java.util.HashMap;
import javax.swing.JEditorPane;
import javax.swing.JProgressBar;

/**
 * A wrapper needed to use a ProgressDialogX as a WaitingHandler.
 *
 * @author Harald Barsnes
 */
public class ProgressDialogWaitingHandler extends ProgressDialogX implements WaitingHandler {

    /**
     * Set to true if the progress is cancelled.
     */
    private boolean runCancelled = false;

    /**
     * Opens a new ProgressDialogWaitingHandler with a Frame as a parent. Note
     * that all changes set the value for the primary progress bar as there is
     * no secondary progressbar
     *
     * @param parent
     * @param progressDialogFrame
     * @param modal
     */
    public ProgressDialogWaitingHandler(java.awt.Frame parent, ProgressDialogParent progressDialogFrame, boolean modal) {
        super(parent, progressDialogFrame, modal);
    }

    /**
     * Opens a new ProgressDialogWaitingHandler with a JDialog as a parent.
     *
     * @param parent
     * @param progressDialogFrame
     * @param modal
     */
    public ProgressDialogWaitingHandler(javax.swing.JDialog parent, ProgressDialogParent progressDialogFrame, boolean modal) {
        super(parent, progressDialogFrame, modal);
    }

    @Override
    public void setMaxProgressValue(int maxProgressValue) {
        super.setValue(maxProgressValue);
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void increaseProgressValue() {
        super.incrementValue();
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void increaseProgressValue(int amount) {
        super.incrementValue(amount);
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void setMaxSecondaryProgressValue(int maxProgressValue) {
        super.setMax(maxProgressValue);
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void resetSecondaryProgressBar() {
        super.setValue(0);
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void increaseSecondaryProgressValue() {
        super.incrementValue();
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void setSecondaryProgressValue(int value) {
        super.setValue(value);
        // note that this set the value for the primary progress bar as there is no secondary progressbar
    }

    @Override
    public void increaseSecondaryProgressValue(int amount) {
        super.incrementValue(amount);
    }

    @Override
    public void setSecondaryProgressDialogIntermediate(boolean intermediate) {
        super.setIndeterminate(intermediate);
    }

    @Override
    public void setRunFinished() {
        // @TODO: need to implement this somehow?
    }

    @Override
    public void setRunCanceled() {
        // @TODO: need to implement this somehow?
    }

    @Override
    public void appendReport(String report) {
        // do nothing as there is no place to display the information
        // @TODO: display the information in a dialog?
    }

    @Override
    public void appendReportNewLineNoDate() {
        // do nothing as there is no place to display the information
    }

    @Override
    public void appendReportEndLine() {
        // do nothing as there is no place to display the information
    }

    @Override
    public boolean isRunCanceled() {
        return runCancelled;
    }

    @Override
    public JProgressBar getSecondaryProgressBar() {
        return super.getProgressBar();
        // note that this returns the primary progressbar as these is no secondary progressbar
    }

    @Override
    public void displayMessage(String message, String title, int messageType) {
        // do nothing as there is no place to display the information
        // @TODO: display the information in a dialog?
    }

    @Override
    public void displayHtmlMessage(JEditorPane messagePane, String title, int messageType) {
        // do nothing as there is no place to display the information
    }

    @Override
    public void displayMissingMgfFilesMessage(HashMap<File, String> missingMgfFiles) {
        // do nothing as there is no place to display the information
    }

    @Override
    public void setWaitingText(String text) {
        super.setTitle(text);
    }
}
