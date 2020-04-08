package eu.isas.peptideshaker.utils;

/**
 * Utilities for the handling of cms files.
 *
 * @author Marc Vaudel
 */
public class CmsUtils {
    
    /**
     * The folder to use when creating cms files.
     */
    private static String parentFolder = null;
    /**
     * The sub folder where the cms files should be stored.
     */
    private static final String SUB_FOLDER = ".PeptideShaker_cms_temp";
    /**
     * Returns the parent folder where to write cms files. Null if not set.
     * 
     * @return The parent folder where to write cms files.
     */
    public static String getParentFolder() {
        return parentFolder;
    }
    /**
     * Returns the sub-folder where to write cms files. Null if not set.
     * 
     * @return the sub-folder where to write cms files
     */
    public static String getSubFolder() {
        return SUB_FOLDER;
    }
    
    /**
     * Sets the parent folder where to write cms files to.
     * 
     * @param newParentFolder The parent folder where to write cms files to.
     */
    public static void setParentFolder(
            String newParentFolder
    ) {
        parentFolder = newParentFolder;
    }

}
