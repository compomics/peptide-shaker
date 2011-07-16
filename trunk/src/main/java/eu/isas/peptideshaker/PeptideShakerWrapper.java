package eu.isas.peptideshaker;

import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.utils.Properties;
import java.io.*;
import javax.swing.JOptionPane;

/**
 * A wrapper class used to start the jar file with parameters. The parameters
 * are read from the JavaOptions file in the Properties folder.
 *
 * @author  Harald Barsnes
 */
public class PeptideShakerWrapper {

    /**
     * If set to true debug output will be written to the screen.
     */
    private boolean debug = false;
    /**
     * The name of the jar file. Must be equal to the name
     * given in the pom file.
     */
    private String jarFileName = "PeptideShaker-";

    /**
     * Starts the launcher by calling the launch method. Use this as the
     * main class in the jar file.
     */
    public PeptideShakerWrapper() {

        // get the version number set in the pom file
        jarFileName = jarFileName + new Properties().getVersion() + ".jar";

        UtilitiesGUIDefaults.setLookAndFeel();

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

        String temp = "", cmdLine, path;

        path = this.getClass().getResource("PeptideShakerWrapper.class").getPath();
        path = path.substring(5, path.indexOf(jarFileName));
        path = path.replace("%20", " ");

        File javaOptions = new File(path + "conf/JavaOptions.txt");

        String options = "", currentOption;

        if (javaOptions.exists()) {

            try {
                FileReader f = new FileReader(javaOptions);
                BufferedReader b = new BufferedReader(f);

                currentOption = b.readLine();

                while (currentOption != null) {
                    if (!currentOption.startsWith("#")) {
                        options += currentOption + " ";
                    }
                    currentOption = b.readLine();
                }

                b.close();
                f.close();

            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            options = "-Xms128M -Xmx768M";
        }

        File tempFile = new File(path);

        String javaHome = System.getProperty("java.home") + File.separator +
                "bin" + File.separator;

        String quote = "";

        if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
            quote = "\"";
        }

        if (debug) {
            JOptionPane.showMessageDialog(null, "original java.home: " + javaHome);
        }
        
        // try to force the use of 64 bit Java if available
        if (javaHome.lastIndexOf(" (x86)") != -1) {
            
            // Java 32 bit home looks like this:    C:\Program Files (x86)\Java\jre6\bin\javaw.exe
            // Java 64 bit home looks like this:    C:\Program Files\Java\jre6\bin\javaw.exe
            
            String tempJavaHome = javaHome.replaceAll(" \\(x86\\)", "");
            
            if (debug) {
                JOptionPane.showMessageDialog(null, "temp java.home: " + tempJavaHome);
            }
            
            if (new File(tempJavaHome).exists()) {
                javaHome = tempJavaHome;
            }
        }
        
        if (debug) {
            JOptionPane.showMessageDialog(null, "new java.home: " + javaHome);
        }
        
        cmdLine = javaHome + "java " + options + " -cp "
                + quote + new File(tempFile, jarFileName).getAbsolutePath() + quote
                + " eu.isas.peptideshaker.gui.PeptideShakerGUI";

        if (debug) {
            System.out.println(cmdLine);
        }

        try {
            Process p = Runtime.getRuntime().exec(cmdLine);

            InputStream stderr = p.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;

            temp += "<ERROR>\n\n";

            if (debug) {
                System.out.println("<ERROR>");
            }

            line = br.readLine();

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
                File logFile = new File("conf", "PeptideShaker.log");
                FileWriter f = new FileWriter(logFile, true);
                f.write("\n\n" + temp + "\n\n");
                f.close();
                
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Failed to start PeptideShaker.\n\n" +
                        "Inspect the log file for details: conf/PeptideShaker.log.\n\n" +
                        "Then go to Troubleshooting at http://peptide-shaker.googlecode.com.",
                        "PeptideShaker - Startup Failed", JOptionPane.ERROR_MESSAGE);

                System.exit(0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the
     * main class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new PeptideShakerWrapper();
    }
}

