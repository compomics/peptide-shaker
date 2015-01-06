package eu.isas.peptideshaker.preferences;

import com.compomics.util.preferences.UtilitiesPathPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.PSExportFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class sets the path preferences for the files to read/write.
 *
 * @author Marc Vaudel
 */
public class PeptideShakerPathPreferences {

    /**
     * Default name for the path configuration file.
     */
    public static final String configurationFileName = "resources/conf/paths.txt";

    /**
     * Enum of the paths which can be set in PeptideShaker.
     */
    public enum PeptideShakerPathKey {

        /**
         * Directory where identification matches are temporarily saved to
         * reduce the memory footprint.
         */
        matchesDirectory("peptideshaker_matches_directory", "Directory where identification matches are temporarily saved to reduce the memory footprint.", "", true),
        /**
         * Folder containing the PeptideShaker user preferences file.
         */
        peptideShakerPreferences("peptideshaker_user_preferences", "Folder containing the PeptideShaker user preferences file.", "", true),
        /**
         * Folder containing the user custom exports file.
         */
        peptideShakerExports("peptideshaker_exports", "Folder containing the user custom exports file.", "", true);
        /**
         * The key used to refer to this path.
         */
        private String id;
        /**
         * The description of the path usage.
         */
        private String description;
        /**
         * The default sub directory or file to use in case all paths should be
         * included in a single directory.
         */
        private String defaultSubDirectory;
        /**
         * Indicates whether the path should be a folder.
         */
        private boolean isDirectory;

        /**
         * Constructor.
         *
         * @param id the id used to refer to this path key
         * @param description the description of the path usage
         * @param defaultSubDirectory the sub directory to use in case all paths
         * should be included in a single directory
         * @param isDirectory boolean indicating whether a folder is expected
         */
        private PeptideShakerPathKey(String id, String description, String defaultSubDirectory, boolean isDirectory) {
            this.id = id;
            this.description = description;
            this.defaultSubDirectory = defaultSubDirectory;
            this.isDirectory = isDirectory;
        }

        /**
         * Returns the id of the path.
         *
         * @return the id of the path
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the description of the path.
         *
         * @return the description of the path
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the key from its id. Null if not found.
         *
         * @param id the id of the key of interest
         *
         * @return the key of interest
         */
        public static PeptideShakerPathKey getKeyFromId(String id) {
            for (PeptideShakerPathKey pathKey : values()) {
                if (pathKey.id.equals(id)) {
                    return pathKey;
                }
            }
            return null;
        }
    }

    /**
     * Loads the path preferences from a text file.
     *
     * @param inputFile the file to load the path preferences from
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     */
    public static void loadPathPreferencesFromFile(File inputFile) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.equals("") && !line.startsWith("#")) {
                    loadPathPreferenceFromLine(line);
                }
            }
        } finally {
            br.close();
        }
    }

    /**
     * Loads a path to be set from a line.
     *
     * @param line the line where to read the path from
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void loadPathPreferenceFromLine(String line) throws FileNotFoundException, IOException {
        String id = UtilitiesPathPreferences.getPathID(line);
        if (id.equals("")) {
            throw new IllegalArgumentException("Impossible to parse path in " + line + ".");
        }
        PeptideShakerPathKey peptideShakerPathKey = PeptideShakerPathKey.getKeyFromId(id);
        if (peptideShakerPathKey == null) {
            UtilitiesPathPreferences.loadPathPreferenceFromLine(line);
        } else {
            String path = UtilitiesPathPreferences.getPath(line);
            File file = new File(path);
            if (!file.exists()) {
                throw new FileNotFoundException("File " + path + " not found.");
            }
            if (peptideShakerPathKey.isDirectory && !file.isDirectory()) {
                throw new FileNotFoundException("Found a file when expecting a directory for " + peptideShakerPathKey.id + ".");
            }
            setPathPreference(peptideShakerPathKey, path);
        }
    }

    /**
     * Sets the path according to the given key and path.
     *
     * @param peptideShakerPathKey the key of the path
     * @param path the path to be set
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void setPathPreference(PeptideShakerPathKey peptideShakerPathKey, String path) throws IOException {
        switch (peptideShakerPathKey) {
            case matchesDirectory:
                PeptideShaker.setMatchesDirectoryParent(path);
                return;
            case peptideShakerExports:
                PSExportFactory.setSerializationFolder(path);
                return;
            case peptideShakerPreferences:
                PeptideShaker.setUserPreferencesFolder(path);
                return;
            default:
                throw new UnsupportedOperationException("Path " + peptideShakerPathKey.id + " not implemented.");
        }
    }

    /**
     * Sets all the paths inside a given folder.
     *
     * @param path the path of the folder where to redirect all paths.
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void setAllPathsIn(String path) throws FileNotFoundException, IOException {
        for (PeptideShakerPathKey peptideShakerPathKey : PeptideShakerPathKey.values()) {
            String subDirectory = peptideShakerPathKey.defaultSubDirectory;
            File newFile = new File(path, subDirectory);
            if (!newFile.exists()) {
                newFile.mkdirs();
            }
            if (!newFile.exists()) {
                throw new FileNotFoundException(newFile.getAbsolutePath() + " could not be created.");
            }
            setPathPreference(peptideShakerPathKey, newFile.getAbsolutePath());
        }
        UtilitiesPathPreferences.setAllPathsIn(path);
    }

    /**
     * Writes all path configurations to the given file.
     *
     * @param file the destination file
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void writeConfigurationToFile(File file) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        try {
            writeConfigurationToFile(bw);
        } finally {
            bw.close();
        }
    }

    /**
     * Writes the configuration file using the provided buffered writer.
     *
     * @param bw the writer to use for writing.
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void writeConfigurationToFile(BufferedWriter bw) throws IOException {
        for (PeptideShakerPathKey pathKey : PeptideShakerPathKey.values()) {
            writePathToFile(bw, pathKey);
        }
        UtilitiesPathPreferences.writeConfigurationToFile(bw);
    }

    /**
     * Writes the path of interest using the provided buffered writer.
     *
     * @param bw the writer to use for writing
     * @param pathKey the key of the path of interest
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void writePathToFile(BufferedWriter bw, PeptideShakerPathKey pathKey) throws IOException {

        bw.write(pathKey.id + UtilitiesPathPreferences.separator);

        switch (pathKey) {
            case matchesDirectory:
                bw.write(PeptideShaker.getMatchesDirectoryParent());
                break;
            case peptideShakerExports:
                bw.write(PSExportFactory.getSerializationFolder());
                break;
            case peptideShakerPreferences:
                bw.write(PeptideShaker.getUserPreferencesFolder());
                break;
            default:
                throw new UnsupportedOperationException("Path " + pathKey.id + " not implemented.");
        }

        bw.newLine();
    }
}