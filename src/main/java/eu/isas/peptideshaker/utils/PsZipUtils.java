package eu.isas.peptideshaker.utils;

import com.compomics.util.io.IoUtil;

/**
 * This class contains information and methods to zip and unzip files from PeptideShaker.
 * Files will be unzipped to a folder constructed based on the static fields:
 * unzipParentFolder/unzipSubFolder/yourfolder_tempFolderName
 *
 * @author Marc Vaudel
 */
public class PsZipUtils {
    
    /**
     * The folder to use when unzipping files.
     */
    private static String unzipParentFolder = null;
    /**
     * The sub folder where the information will be stored.
     */
    private static final String unzipSubFolder = ".PeptideShaker_unzip_temp";
    /**
     * Suffix for folders where the content of zip files should be extracted.
     */
    public final static String tempFolderName = "PeptideShaker_temp";
    /**
     * Returns the parent folder where to unzip files. Null if not set.
     * 
     * @return the parent folder where to unzip files.
     */
    public static String getUnzipParentFolder() {
        return unzipParentFolder;
    }
    /**
     * Returns the sub-folder where to unzip files. Null if not set.
     * 
     * @return the sub-folder where to unzip files
     */
    public static String getUnzipSubFolder() {
        return unzipSubFolder;
    }
    
    /**
     * Sets the parent folder where to unzip files to.
     * 
     * @param newFolder the parent folder where to unzip files to
     */
    public static void setUnzipParentFolder(String newFolder) {
        unzipParentFolder = newFolder;
    }

    /**
     * Returns the temp folder name to use when unzipping a zip file.
     *
     * @param fileName the name of the zip file
     *
     * @return the folder name associated to the zip file
     */
    public static String getTempFolderName(String fileName) {
        return IoUtil.removeExtension(fileName) + "_" + tempFolderName;
    }
}
