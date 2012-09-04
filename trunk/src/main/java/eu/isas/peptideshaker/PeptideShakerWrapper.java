package eu.isas.peptideshaker;

import com.compomics.software.CompomicsWrapper;
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

        // get the version number set in the pom file
        String jarFileName = "PeptideShaker-" + new Properties().getVersion() + ".jar";
        String path = this.getClass().getResource("PeptideShakerWrapper.class").getPath();
                path = path.substring(5, path.indexOf(jarFileName));
                path = path.replace("%20", " ");
                path = path.replace("%5b", "[");
                path = path.replace("%5d", "]");
        File jarFile = new File(path, jarFileName);
        // get the splash 
        String splash = "peptide-shaker-splash.png";
        String mainClass = "eu.isas.peptideshaker.gui.PeptideShakerGUI";

        launchTool("PeptideShaker", jarFile, splash, mainClass);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new PeptideShakerWrapper();
    }
}
