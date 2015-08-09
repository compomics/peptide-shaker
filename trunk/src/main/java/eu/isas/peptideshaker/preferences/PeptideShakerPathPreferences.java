package eu.isas.peptideshaker.preferences;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.PSExportFactory;
import eu.isas.peptideshaker.utils.PsZipUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class sets the path preferences for the files to read/write.
 *
 * @author Marc Vaudel
 */
public class PeptideShakerPathPreferences {

    /**
     * Enum of the paths which can be set in PeptideShaker.
     */
    public enum PeptideShakerPathKey implements PathKey {

        /**
         * Directory where identification matches are temporarily saved to
         * reduce the memory footprint.
         */
        matchesDirectory("peptideshaker_matches_directory", "Folder where identification matches are temporarily saved to reduce the memory footprint.", "", true),
        /**
         * Folder containing the PeptideShaker user preferences file.
         */
        peptideShakerPreferences("peptideshaker_user_preferences", "Folder containing the PeptideShaker user preferences file.", "", true),
        /**
         * Folder containing the user custom exports file.
         */
        peptideShakerExports("peptideshaker_exports", "Folder containing the user custom exports file.", "", true),
        /**
         * The folder to use when unzipping files.
         */
        unzipFolder("unzip", "Folder to use when unzipping files", "", true);
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

        @Override
        public String getId() {
            return id;
        }

        @Override
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
            if (!path.equals(UtilitiesPathPreferences.defaultPath)) {
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
    }

    /**
     * Sets the path according to the given key and path.
     *
     * @param peptideShakerPathKey the key of the path
     *
     * @return returns the path used
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static String getPathPreference(PeptideShakerPathKey peptideShakerPathKey) throws IOException {
        switch (peptideShakerPathKey) {
            case matchesDirectory:
                return PeptideShaker.getMatchesDirectoryParent();
            case peptideShakerExports:
                return PSExportFactory.getSerializationFolder();
            case peptideShakerPreferences:
                return PeptideShaker.getUserPreferencesFolder();
            case unzipFolder:
                return PsZipUtils.getUnzipParentFolder();
            default:
                throw new UnsupportedOperationException("Path " + peptideShakerPathKey.id + " not implemented.");
        }
    }

    /**
     * Sets the path according to the given key and path.
     *
     * @param peptideShakerPathKey the key of the path
     * @param path the path to be set
     *
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
            case unzipFolder:
                PsZipUtils.setUnzipParentFolder(path);
                return;
            default:
                throw new UnsupportedOperationException("Path " + peptideShakerPathKey.id + " not implemented.");
        }
    }

    /**
     * Sets the path according to the given key and path.
     *
     * @param pathKey the key of the path
     * @param path the path to be set
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void setPathPreferences(PathKey pathKey, String path) throws IOException {
        if (pathKey instanceof PeptideShakerPathKey) {
            PeptideShakerPathKey peptideShakerPathKey = (PeptideShakerPathKey) pathKey;
            PeptideShakerPathPreferences.setPathPreference(peptideShakerPathKey, path);
        } else if (pathKey instanceof UtilitiesPathPreferences.UtilitiesPathKey) {
            UtilitiesPathPreferences.UtilitiesPathKey utilitiesPathKey = (UtilitiesPathPreferences.UtilitiesPathKey) pathKey;
            UtilitiesPathPreferences.setPathPreference(utilitiesPathKey, path);
        } else {
            throw new UnsupportedOperationException("Path " + pathKey.getId() + " not implemented.");
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
                String toWrite = PeptideShaker.getMatchesDirectoryParent();
                if (toWrite == null) {
                    toWrite = UtilitiesPathPreferences.defaultPath;
                }
                bw.write(toWrite);
                break;
            case peptideShakerExports:
                toWrite = PSExportFactory.getSerializationFolder();
                if (toWrite == null) {
                    toWrite = UtilitiesPathPreferences.defaultPath;
                }
                bw.write(toWrite);
                break;
            case peptideShakerPreferences:
                toWrite = PeptideShaker.getUserPreferencesFolder();
                if (toWrite == null) {
                    toWrite = UtilitiesPathPreferences.defaultPath;
                }
                bw.write(toWrite);
                break;
            case unzipFolder:
                toWrite = PsZipUtils.getUnzipParentFolder();
                if (toWrite == null) {
                    toWrite = UtilitiesPathPreferences.defaultPath;
                }
                bw.write(toWrite);
                break;
            default:
                throw new UnsupportedOperationException("Path " + pathKey.id + " not implemented.");
        }

        bw.newLine();
    }

    /**
     * Returns a list containing the keys of the paths where the tool is not
     * able to write.
     *
     * @return a list containing the keys of the paths where the tool is not
     * able to write
     *
     * @throws IOException exception thrown whenever an error occurred while
     * loading the path configuration
     */
    public static ArrayList<PathKey> getErrorKeys() throws IOException {
        ArrayList<PathKey> result = new ArrayList<PathKey>();
        for (PeptideShakerPathKey peptideShakerPathKey : PeptideShakerPathKey.values()) {
            String folder = PeptideShakerPathPreferences.getPathPreference(peptideShakerPathKey);
            if (folder != null && !UtilitiesPathPreferences.testPath(folder)) {
                result.add(peptideShakerPathKey);
            }
        }
        result.addAll(UtilitiesPathPreferences.getErrorKeys());
        return result;
    }
}
