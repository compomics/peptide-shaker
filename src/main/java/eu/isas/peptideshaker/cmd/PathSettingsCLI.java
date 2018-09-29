package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import static eu.isas.peptideshaker.cmd.PeptideShakerCLI.redirectErrorStream;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Allows the user to set the path settings in command line.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PathSettingsCLI {

    /**
     * The input bean containing the user parameters.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;
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
        
        // set the PeptideShaker log file
        if (pathSettingsCLIInputBean.getLogFolder() == null) {
            PeptideShakerGUI peptideShakerGUI = new PeptideShakerGUI(); // dummy object
            peptideShakerGUI.setUpLogFile(false); // redirect the error stream to the PeptideShaker log file
        }

        if (pathSettingsCLIInputBean.hasInput()) {

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
                        UtilitiesPathPreferences.UtilitiesPathKey utilitiesPathKey = UtilitiesPathPreferences.UtilitiesPathKey.getKeyFromId(id);
                        if (utilitiesPathKey == null) {
                            System.out.println("Path id " + id + " not recognized.");
                        } else {
                            UtilitiesPathPreferences.setPathPreference(utilitiesPathKey, pathInput.get(id));
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
            File destinationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
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

        } else {
            try {
                File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
                if (pathConfigurationFile.exists()) {
                    PeptideShakerPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
                }
            } catch (Exception e) {
                System.out.println("An error occurred when setting the path configurations. Default paths will be used.");
                e.printStackTrace();
            }
        }

        // test the temp paths
        try {
            ArrayList<PathKey> errorKeys = PeptideShakerPathPreferences.getErrorKeys();
            if (!errorKeys.isEmpty()) {
                System.out.println("Failed to write in the following configuration folders. Please use a temporary folder, "
                        + "the path configuration command line, or edit the configuration paths from the graphical interface.");
                for (PathKey pathKey : errorKeys) {
                    System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load the path configurations. Default paths will be used.");
            e.printStackTrace();
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

    /**
     * If the arguments contains changes to the paths these arguments will be
     * extracted and the paths updated, before the remaining non-path options
     * are returned for further processing.
     *
     * @param args the command line arguments
     * @return a list of all non-path related arguments
     * @throws ParseException if a ParseException occurs
     */
    public static String[] extractAndUpdatePathOptions(String[] args) throws ParseException {

        ArrayList<String> allPathOptions = PathSettingsCLIParams.getOptionIDs();

        ArrayList<String> pathSettingArgs = new ArrayList<String>();
        ArrayList<String> nonPathSettingArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {

            String currentArg = args[i];

            boolean pathOption = allPathOptions.contains(currentArg);

            if (pathOption) {
                pathSettingArgs.add(currentArg);
            } else {
                nonPathSettingArgs.add(currentArg);
            }

            // check if the argument has a parameter
            if (i + 1 < args.length) {
                String nextArg = args[i + 1];
                if (!nextArg.startsWith("-")) {
                    if (pathOption) {
                        pathSettingArgs.add(args[++i]);
                    } else {
                        nonPathSettingArgs.add(args[++i]);
                    }
                }
            }
        }

        String[] pathSettingArgsAsList = pathSettingArgs.toArray(new String[pathSettingArgs.size()]);
        String[] nonPathSettingArgsAsList = nonPathSettingArgs.toArray(new String[nonPathSettingArgs.size()]);

        // update the paths if needed
        Options pathOptions = new Options();
        PathSettingsCLIParams.createOptionsCLI(pathOptions);
        BasicParser parser = new BasicParser();
        CommandLine line = parser.parse(pathOptions, pathSettingArgsAsList);
        PathSettingsCLIInputBean pathSettingsCLIInputBean = new PathSettingsCLIInputBean(line);
        PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
        pathSettingsCLI.setPathSettings();

        return nonPathSettingArgsAsList;
    }
}
