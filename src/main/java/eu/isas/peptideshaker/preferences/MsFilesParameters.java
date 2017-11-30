package eu.isas.peptideshaker.preferences;

import com.compomics.util.db.object.DbObject;
import com.compomics.util.db.object.ObjectsDB;
import java.io.File;
import java.util.HashMap;

/**
 * This class holds details on the mass spectrometry input files.
 *
 * @author Marc Vaudel
 */
public class MsFilesParameters extends DbObject {
    
    /**
     * Map of the mass spectrometry files.
     */
    private final HashMap<String, File> msFilesMap;
    
    /**
     * Constructor.
     * 
     * @param msFilesMap the mass spectrometry files of the project
     */
    public MsFilesParameters(HashMap<String, File> msFilesMap) {
        
        this.msFilesMap = msFilesMap;
        
    }

    /**
     * Returns the mass spectrometry files map.
     * 
     * @return the mass spectrometry files map
     */
    public HashMap<String, File> getMsFilesMap() {
        
        ObjectsDB.increaseRWCounter(); zooActivateRead(); ObjectsDB.decreaseRWCounter();
        
        return msFilesMap;
        
    }

}
