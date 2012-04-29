package eu.isas.peptideshaker;

import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.SearchGuiSetupDialog;
import java.io.*;
import javax.swing.JOptionPane;

/**
 * A wrapper class used to start the SearchGUI from within PeptideShaker.
 *
 * @author Harald Barsnes
 */
public class SearchGUIWrapper {

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
     */
    public SearchGUIWrapper(PeptideShakerGUI peptideShakerGUI) {

        this.peptideShakerGUI = peptideShakerGUI;

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

        // check if searchgui file exists
        if (!new File(peptideShakerGUI.getUserPreferences().getSearchGuiPath()).exists()) {
            JOptionPane.showMessageDialog(peptideShakerGUI,
                    "SearchGUI installation not found!\n"
                    + "Check Edit > SearchGUI Settings", "SearchGUI Error", JOptionPane.WARNING_MESSAGE);
            new SearchGuiSetupDialog(peptideShakerGUI, true);
            return;
        }

        String quote = "";

        if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
            quote = "\"";
        }

        String cmdLine = "java -jar " + quote + peptideShakerGUI.getUserPreferences().getSearchGuiPath() + quote;

        if (debug) {
            System.out.println(cmdLine);
        }

        try {
            Process p = Runtime.getRuntime().exec(cmdLine);

            InputStream stderr = p.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);

            String temp = "<ERROR>\n\n";

            if (debug) {
                System.out.println("<ERROR>");
            }

            String line = br.readLine();

            boolean error = false;

            while (line != null) {

                if (debug) {
                    System.out.println(line);
                }

                temp += line + "\n";
                line = br.readLine();
                error = true;
            }

            if (debug) {
                System.out.println("</ERROR>");
            }

            temp += "\nThe command line executed:\n";
            temp += cmdLine + "\n";
            temp += "\n</ERROR>\n";
            int exitVal = p.waitFor();

            if (debug) {
                System.out.println("Process exitValue: " + exitVal);
            }

            if (error) {
                File logFile = new File(peptideShakerGUI.getJarFilePath() + "/resources/conf", "SearchGUI.log");
                FileWriter f = new FileWriter(logFile, true);
                f.write("\n\n" + temp + "\n\n");
                f.close();

                javax.swing.JOptionPane.showMessageDialog(null,
                        "Failed to start SearchGUI.\n\n"
                        + "Inspect the log file for details: resources/conf/SearchGUI.log.\n\n"
                        + "Then go to Troubleshooting at http://searchgui.googlecode.com.",
                        "SearchGUI - Startup Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }
    }
}
