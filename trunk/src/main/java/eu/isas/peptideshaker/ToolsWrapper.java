package eu.isas.peptideshaker;

import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.RelimsSetupDialog;
import eu.isas.peptideshaker.gui.ReporterSetupDialog;
import eu.isas.peptideshaker.gui.SearchGuiSetupDialog;
import java.io.*;
import javax.swing.JOptionPane;

/**
 * A wrapper class used to start Relims, SearchGUI or Eeporter from within
 * PeptideShaker.
 *
 * @author Harald Barsnes
 */
public class ToolsWrapper {

    /**
     * The tools supported by this wrapper.
     */
    public enum ToolType {

        SEARCHGUI, REPORTER, RELIMS
    };
    /**
     * The current tool being wrapped.
     */
    private ToolType currentToolType;
    /**
     * If set to true debug output will be written to the screen.
     */
    private boolean debug = false;
    /**
     * A reference to PeptideShakerGUI.
     */
    private PeptideShakerGUI peptideShakerGUI;

    /**
     * Starts the launcher by calling the launch method.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     * @param tooltype the tool to start
     */
    public ToolsWrapper(PeptideShakerGUI peptideShakerGUI, ToolType tooltype) {

        this.peptideShakerGUI = peptideShakerGUI;
        currentToolType = tooltype;

        try {
            launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches the jar file with parameters to the jvm.
     *
     * @throws java.lang.Exception
     */
    private void launch() throws Exception {

        String toolName;
        
        // check if the tool is installed at the given path
        if (currentToolType == ToolType.SEARCHGUI) {
            
            toolName = "SearchGUI";
            
            if (!new File(peptideShakerGUI.getUtilitiesUserPreferences().getSearchGuiPath()).exists()) {
                JOptionPane.showMessageDialog(peptideShakerGUI,
                        "SearchGUI installation not found!\n"
                        + "Check Edit > Tools > SearchGUI", "SearchGUI Error", JOptionPane.WARNING_MESSAGE);
                new SearchGuiSetupDialog(peptideShakerGUI, true);
                return;
            }
        } else if (currentToolType == ToolType.RELIMS) {
            
            toolName = "Relims";
            
            if (!new File(peptideShakerGUI.getUtilitiesUserPreferences().getRelimsPath()).exists()) {
                JOptionPane.showMessageDialog(peptideShakerGUI,
                        "Relims installation not found!\n"
                        + "Check Edit > Tools > Relims", "Relims Error", JOptionPane.WARNING_MESSAGE);
                new RelimsSetupDialog(peptideShakerGUI, true);
                return;
            }
        } else { // currentToolType == ToolType.REPORTER
            
            toolName = "Reporter";
            
            if (!new File(peptideShakerGUI.getUtilitiesUserPreferences().getReporterPath()).exists()) {
                JOptionPane.showMessageDialog(peptideShakerGUI,
                        "Reporter installation not found!\n"
                        + "Check Edit > Tools > Reporter", "Reporter Error", JOptionPane.WARNING_MESSAGE);
                new ReporterSetupDialog(peptideShakerGUI, true);
                return;
            }
        }

        String quote = "";

        if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
            quote = "\"";
        }

        String cmdLine; 
        
        if (currentToolType == ToolType.SEARCHGUI) {
            cmdLine = "java -jar " + quote + peptideShakerGUI.getUtilitiesUserPreferences().getSearchGuiPath() + quote;
        } else if (currentToolType == ToolType.RELIMS) {
            cmdLine = "java -jar " + quote + peptideShakerGUI.getUtilitiesUserPreferences().getRelimsPath() + quote;
        } else {  // currentToolType == ToolType.REPORTER
            cmdLine = "java -jar " + quote + peptideShakerGUI.getUtilitiesUserPreferences().getReporterPath() + quote;
        }

        if (debug) {
            System.out.println(cmdLine);
        }

        try {
            Process p = Runtime.getRuntime().exec(cmdLine);

            InputStream stderr = p.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);

            String temp = "<ERROR>" + System.getProperty("line.separator") + System.getProperty("line.separator");

            if (debug) {
                System.out.println("<ERROR>");
            }

            String line = br.readLine();

            boolean error = false;

            while (line != null) {

                if (debug) {
                    System.out.println(line);
                }

                temp += line + System.getProperty("line.separator");
                line = br.readLine();
                error = true;
            }

            if (debug) {
                System.out.println("</ERROR>");
            }

            temp += System.getProperty("line.separator") + "The command line executed:" + System.getProperty("line.separator");
            temp += cmdLine + System.getProperty("line.separator");
            temp += System.getProperty("line.separator") + "</ERROR>" + System.getProperty("line.separator");
            int exitVal = p.waitFor();

            if (debug) {
                System.out.println("Process exitValue: " + exitVal);
            }

            if (error) {
                File logFile = new File(peptideShakerGUI.getJarFilePath() + "/resources/conf", toolName + ".log");
                FileWriter f = new FileWriter(logFile, true);
                f.write(System.getProperty("line.separator") + System.getProperty("line.separator") + temp + System.getProperty("line.separator") + System.getProperty("line.separator"));
                f.close();

                javax.swing.JOptionPane.showMessageDialog(null,
                        "Failed to start " + toolName + ".\n\n"
                        + "Inspect the log file for details: resources/conf/" + toolName + ".log.\n\n"
                        + "Then go to Troubleshooting at http://peptide-shaker.googlecode.com.",
                        toolName + " - Startup Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }
    }
}
