package eu.isas.peptideshaker;

import java.io.File;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * A wrapper class used to check if the jar file is attempted started from
 * within an unzipped zip file. If yes, a warning dialog is displayed telling
 * the user to unzip first. If not within a zip file the jar file is started as
 * normal.
 *
 * @author Harald Barsnes
 */
public class PeptideShakerZipFileChecker {

    /**
     * Checks if the jar file is started from within an unzipped zip file. If
     * yes, a warning dialog is displayed telling the user to unzip first. If
     * not within a zip file the jar file is started as normal.
     *
     * @param args the command line arguments
     */
    public PeptideShakerZipFileChecker(String[] args) {

        String operatingSystem = System.getProperty("os.name").toLowerCase();

        // only perform the check on windows systems
        if (operatingSystem.contains("windows")) {

            // try to set the look and feel
            try {
                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // ignore error, use default look and feel
            }

            // get the current location
            URL tempPath = this.getClass().getResource("PeptideShakerZipFileChecker.class");

            // get the zip temp directory
            String winTemp = new File(System.getProperty("java.io.tmpdir")).getPath();
            winTemp = winTemp.replaceAll("\\\\", "/");

            // check if inside the temp directory
            if (tempPath.getPath().contains(winTemp)) {
                JOptionPane.showMessageDialog(null,
                        "PeptideShaker was started from within the zip file. Unzip and try again.",
                        "PeptideShaker Startup Error", JOptionPane.WARNING_MESSAGE);
            } else {
                new PeptideShakerWrapper(args);
            }
        } else {
            new PeptideShakerWrapper(args);
        }
    }

    /**
     * Start PeptideShakerZipFileChecker.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new PeptideShakerZipFileChecker(args);
    }
}
