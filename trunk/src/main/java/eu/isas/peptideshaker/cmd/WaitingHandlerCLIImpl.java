package eu.isas.peptideshaker.cmd;

import eu.isas.peptideshaker.gui.interfaces.WaitingHandler;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * This class is an implementation of the WaitingHandler interface to be used
 * when operating through the Command Line Interface.
 *
 * @author Kenny Helsens
 */
public class WaitingHandlerCLIImpl implements WaitingHandler {

    /**
     * Make use of the System Printstreams to log the messages.
     */
    private PrintStream err = System.err;
    private PrintStream out = System.out;
    /**
     * The running value of the progress monitor.
     */
    private int iRunningProgressValue = 0;
    private int iRunningProgressValueSecondary = 0;
    /** 
     * The upper value of the progress monitor.
     */
    private int iMaxProgressValue = 1;
    private int iMaxProgressValueSecondary = 1;
    private boolean boolSecondaryIntermediate = false;
    private boolean boolFinished = false;
    private boolean boolCanceled = false;
    protected String iReport = "";

    /**
     * Set the maximum value of the progress bar.
     *
     * @param maxProgressValue the max value
     */
    @Override
    public void setMaxProgressValue(int maxProgressValue) {
        iMaxProgressValue = maxProgressValue;
    }

    /**
     * Increase the progress bar value by one "counter".
     */
    @Override
    public void increaseProgressValue() {
        iRunningProgressValue++;
    }

    /**
     * Increase the progress bar value by the given amount.
     *
     * @param amount the amount to increase the value by
     */
    @Override
    public void increaseProgressValue(int amount) {
        iRunningProgressValue = iRunningProgressValue + amount;
    }

    /**
     * Set the maximum value of the secondary progress bar. And resets the value
     * to 0.
     *
     * @param maxProgressValue the max value
     */
    @Override
    public void setMaxSecondaryProgressValue(int maxProgressValue) {
        iMaxProgressValueSecondary = maxProgressValue;
        resetSecondaryProgressBar();

    }

    /**
     * Reset the secondary progress bar value to 0.
     */
    @Override
    public void resetSecondaryProgressBar() {
        iRunningProgressValueSecondary = 0;
    }

    /**
     * Increase the secondary progress bar value by one "counter".
     */
    @Override
    public void increaseSecondaryProgressValue() {
        iRunningProgressValueSecondary++;
    }

    /**
     * Sets the secondary progress bar to the given value.
     *
     * @param value the progress value
     */
    @Override
    public void setSecondaryProgressValue(int value) {
        iRunningProgressValue = value;
    }

    /**
     * Increase the secondary progress bar value by the given amount.
     *
     * @param amount the amount to increase the value by
     */
    @Override
    public void increaseSecondaryProgressValue(int amount) {
        iRunningProgressValueSecondary = iRunningProgressValueSecondary + amount;
    }

    /**
     * Sets the secondary progress bar to intermediate or not.
     *
     * @param intermediate if true, set to intermediate
     */
    @Override
    public void setSecondaryProgressDialogIntermediate(boolean intermediate) {
        boolSecondaryIntermediate = intermediate;
    }

    /**
     * Set the analysis as finished.
     */
    @Override
    public void setRunFinished() {
        boolFinished = true;
    }

    /**
     * Set the analysis as canceled.
     */
    @Override
    public void setRunCanceled() {
        boolCanceled = true;
    }

    /**
     * Append text to the report.
     *
     * @param report the text to append
     */
    @Override
    public void appendReport(String report) {
        iReport = iReport + report;
        out.append(report + "\n");
    }

    /**
     * Append two tabs to the report. No new line.
     */
    @Override
    public void appendReportNewLineNoDate() {
        iReport = iReport + "\n";
        out.append("\n");
    }

    /**
     * Append a new line to the report.
     */
    @Override
    public void appendReportEndLine() {
        iReport = iReport + "\n";
        out.append("\n");
    }

    /**
     * Returns true if the run is canceled.
     *
     * @return true if the run is canceled
     */
    @Override
    public boolean isRunCanceled() {
        return boolCanceled;
    }

    /**
     * Returns the secondary progress bar for updates from external processes.
     *
     * @return the secondary progress bar, can be null
     */
    @Override
    public JProgressBar getSecondaryProgressBar() {
        return null;
    }

    /**
     * Display a given message to the user separatly from the main output. For
     * example a warning or error message. Usually in a separate dialog if a
     * grahical waiting handler is used.
     *
     * @param message the message to display
     * @param title the title of the message
     * @param messageType the message type in the, e.g.,
     * JOptionPane.INFORMATION_MESSAGE
     */
    @Override
    public void displayMessage(String message, String title, int messageType) {
        out.print(title);
        out.print("\n");
        out.print(message);
        out.print("\n");
    }

    /**
     * Display a given html containing message to the user separatly from the
     * main output. For example a warning or error message. Usually in a
     * separate dialog if a grahical waiting handler is used. The html links
     * should be clickable.
     *
     * @param messagePane
     * @param title
     * @param messageType
     */
    @Override
    public void displayHtmlMessage(JEditorPane messagePane, String title, int messageType) {
        displayMessage(messagePane.getText(), title, messageType);
    }

    /**
     * Display a message that mgf files are missing.
     *
     * @param missingMgfFiles
     */
    @Override
    public void displayMissingMgfFilesMessage(HashMap<File, String> missingMgfFiles) {
        StringBuilder lFiles = new StringBuilder();
        for (String lMissingFile : missingMgfFiles.values()) {
            lFiles.append(lMissingFile).append(",");
        }

        displayMessage("MGF files missing", lFiles.toString(), 1);
    }

    /**
     * Sets the text describing what is currently waited for.
     *
     * @param text a text describing what is currently waited for
     */
    @Override
    public void setWaitingText(String text) {
        displayMessage("WaitingMessage", text, 1);

    }
}
