package eu.isas.peptideshaker.cmd;

import com.compomics.software.settings.UtilitiesPathParameters;
import eu.isas.peptideshaker.preferences.PeptideShakerPathParameters;
import java.io.File;
import java.util.HashMap;
import org.apache.commons.cli.CommandLine;

/**
 * Parses the command line and retrieves the user input.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PathSettingsCLIInputBean {

    /**
     * The path set to the temp folder.
     */
    private String tempFolder = "";
    /**
     * The specific paths sets for every option.
     */
    private final HashMap<String, String> paths = new HashMap<>();
    /**
     * The folder where to save the logs.
     */
    private File logFolder = null;
    /**
     * Whether a log file is to be used. Setting this option to false, sends all
     * of the logs to the standard output instead.
     */
    private Boolean useLogFile = null;

    /**
     * Construct a FollowUpCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     */
    public PathSettingsCLIInputBean(CommandLine aLine) {

        if (aLine.hasOption(PathSettingsCLIParams.USE_LOG_FOLDER.id)) {
            
            String input = aLine.getOptionValue(PathSettingsCLIParams.USE_LOG_FOLDER.id);
            int index = Integer.parseInt(input.trim());
            useLogFile = index == 1;

        }

        if (aLine.hasOption(PathSettingsCLIParams.LOG_FOLDER.id)) {
            logFolder = new File(aLine.getOptionValue(PathSettingsCLIParams.LOG_FOLDER.id));
        }

        if (aLine.hasOption(PathSettingsCLIParams.ALL.id)) {
            tempFolder = aLine.getOptionValue(PathSettingsCLIParams.ALL.id);
        }

        for (PeptideShakerPathParameters.PeptideShakerPathKey peptideShakerPathKey : PeptideShakerPathParameters.PeptideShakerPathKey.values()) {
            String id = peptideShakerPathKey.getId();
            if (aLine.hasOption(id)) {
                paths.put(id, aLine.getOptionValue(id));
            }
        }
        for (UtilitiesPathParameters.UtilitiesPathKey utilitiesPathKey : UtilitiesPathParameters.UtilitiesPathKey.values()) {
            String id = utilitiesPathKey.getId();
            if (aLine.hasOption(id)) {
                paths.put(id, aLine.getOptionValue(id));
            }
        }
    }

    /**
     * Returns the temp folder, an empty string if not set.
     *
     * @return the temp folder
     */
    public String getTempFolder() {
        return tempFolder;
    }

    /**
     * Returns the specific paths provided by the user in a map: Path id &gt;
     * path.
     *
     * @return the specific paths provided by the user
     */
    public HashMap<String, String> getPaths() {
        return paths;
    }

    /**
     * Indicates whether the user gave some path configuration input.
     *
     * @return a boolean indicating whether the user gave some path
     * configuration input.
     */
    public boolean hasInput() {
        return !tempFolder.equals("")
                || !paths.isEmpty()
                || logFolder != null
                || useLogFile != null;
    }

    /**
     * Returns the folder where to save the log files.
     *
     * @return the folder where to save the log files
     */
    public File getLogFolder() {
        return logFolder;
    }

    /**
     * Return true if a log file is to be used. False, sends the logs to the
     * standard output.
     *
     * @return true if a log file is to be used, false sends the logs to the
     * standard output
     */
    public boolean useLogFile() {
        if (useLogFile == null) {
            return true;
        }
        return useLogFile;
    }
}
