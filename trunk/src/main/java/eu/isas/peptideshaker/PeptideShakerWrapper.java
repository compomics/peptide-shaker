package eu.isas.peptideshaker;

import com.compomics.software.CompomicsWrapper;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.peptideshaker.utils.Properties;
import java.io.*;

/**
 * A wrapper class used to start the jar file with parameters. The parameters
 * are read from the JavaOptions file in the Properties folder.
 *
 * @author Harald Barsnes
 */
public class PeptideShakerWrapper extends CompomicsWrapper {

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     */
    public PeptideShakerWrapper() {
        this(null);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments (ignored if null)
     */
    public PeptideShakerWrapper(String[] args) {

        // get the version number set in the pom file
        String jarFileName = "PeptideShaker-" + new Properties().getVersion() + ".jar";
        String path = PeptideShaker.getJarFilePath();
        File jarFile = new File(path, jarFileName);
        // get the splash 
        String splash = "peptide-shaker-splash.png";
        String mainClass = "eu.isas.peptideshaker.gui.PeptideShakerGUI";
        // Set path for utilities preferences
        try {
            setPathConfiguration();
        } catch (Exception e) {
            System.out.println("Impossible to load path configuration, default will be used.");
        }
        launchTool("PeptideShaker", jarFile, splash, mainClass, args);
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), PeptideShakerPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new PeptideShakerWrapper(args);
    }
}
