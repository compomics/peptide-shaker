package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.UtilitiesPathParameters;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import static eu.isas.peptideshaker.cmd.PeptideShakerCLI.redirectErrorStream;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Allows the user to set the path settings in command line.
 *
 * @author Marc Vaudel
 */
public class PathSettingsCLI {

    /**
     * The input bean containing the user parameters.
     */
    private final PathSettingsCLIInputBean pathSettingsCLIInputBean;
    /**
     * Waiting handler used to keep track of the progress.
     */
    private WaitingHandler waitingHandler;

    /**
     * Constructor.
     *
     * @param pathSettingsCLIInputBean an input bean containing the user
     * parameters
     */
    public PathSettingsCLI(PathSettingsCLIInputBean pathSettingsCLIInputBean) {
        this.pathSettingsCLIInputBean = pathSettingsCLIInputBean;
    }

    public Object call() {
        waitingHandler = new WaitingHandlerCLIImpl();
        setPathSettings();
        if (!waitingHandler.isRunCanceled()) {
            System.exit(0);
            return 0;
        } else {
            System.exit(1);
            return 1;
        }
    }

    /**
     * Sets the path settings according to the pathSettingsCLIInputBean.
     */
    public void setPathSettings() {

        if (waitingHandler == null) {
            waitingHandler = new WaitingHandlerCLIImpl();
        }

        if (pathSettingsCLIInputBean.getLogFolder() != null) {
            redirectErrorStream(pathSettingsCLIInputBean.getLogFolder());
        }

        String path = pathSettingsCLIInputBean.getTempFolder();
        if (!path.equals("")) {
            try {
                PeptideShakerPathPreferences.setAllPathsIn(path);
            } catch (Exception e) {
                System.out.println("An error occurred when setting the temporary folder path.");
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        HashMap<String, String> pathInput = pathSettingsCLIInputBean.getPaths();
        for (String id : pathInput.keySet()) {
            try {
                PeptideShakerPathPreferences.PeptideShakerPathKey peptideShakerPathKey = PeptideShakerPathPreferences.PeptideShakerPathKey.getKeyFromId(id);
                if (peptideShakerPathKey == null) {
                    UtilitiesPathParameters.UtilitiesPathKey utilitiesPathKey = UtilitiesPathParameters.UtilitiesPathKey.getKeyFromId(id);
                    if (utilitiesPathKey == null) {
                        System.out.println("Path id " + id + " not recognized.");
                    } else {
                        UtilitiesPathParameters.setPathParameter(utilitiesPathKey, pathInput.get(id));
                    }
                } else {
                    PeptideShakerPathPreferences.setPathPreference(peptideShakerPathKey, pathInput.get(id));
                }
            } catch (Exception e) {
                System.out.println("An error occurred when setting the path " + id + ".");
                e.printStackTrace();
                waitingHandler.setRunCanceled();
            }
        }

        // Write path file preference
        File destinationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathParameters.configurationFileName);
        try {
            PeptideShakerPathPreferences.writeConfigurationToFile(destinationFile);
        } catch (Exception e) {
            System.out.println("An error occurred when saving the path preference to " + destinationFile.getAbsolutePath() + ".");
            e.printStackTrace();
            waitingHandler.setRunCanceled();
        }

        if (!waitingHandler.isRunCanceled()) {
            System.out.println("Path configuration completed.");
        }
    }

    /**
     * PeptideShaker path settings CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The PeptideShaker path settings command line allows setting the path of every configuration file created by PeptideShaker or set a temporary folder where all files will be stored." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://compomics.github.io/projects/peptide-shaker.html and http://compomics.github.io/projects/peptide-shaker/wiki/peptideshakercli.html." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/peptide-shaker." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator");
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            Options lOptions = new Options();
            PathSettingsCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (args.length == 0) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "========================================" + System.getProperty("line.separator"));
                lPrintWriter.print("PeptideShaker Path Settings - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("========================================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(PathSettingsCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                PathSettingsCLIInputBean cliInputBean = new PathSettingsCLIInputBean(line);
                PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(cliInputBean);
                pathSettingsCLI.call();
            }
        } catch (OutOfMemoryError e) {
            System.out.println("PeptideShaker used up all the memory and had to be stopped. See the PeptideShaker log for details.");
            System.err.println("Ran out of memory!");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("PeptideShaker processing failed. See the PeptideShaker log for details.");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "PathSettingsCLI{"
                + ", cliInputBean=" + pathSettingsCLIInputBean
                + '}';
    }
}
