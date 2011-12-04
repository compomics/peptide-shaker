package eu.isas.peptideshaker.preferences;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * This class contains the details about a project
 *
 * @author Marc Vaudel
 */
public class ProjectDetails implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = -2635206350852992221L;
    /**
     * List of the identification files loaded
     */
    private ArrayList<File> identificationFiles;
    /**
     * The database file loaded
     */
    private File dbFile;
    /**
     * When the project was created
     */
    private Date creationDate;

    /**
     * Constructor
     */
    public ProjectDetails() {
    }

    /**
     * Getter for the database file
     * @return the database file
     */
    public File getDbFile() {
        return dbFile;
    }

    /**
     * Setter for the database file
     * @param dbFile the database file
     */
    public void setDbFile(File dbFile) {
        this.dbFile = dbFile;
    }

    /**
     * Getter for all identification files loaded
     * @return all identification files loaded
     */
    public ArrayList<File> getIdentificationFiles() {
        return identificationFiles;
    }

    /**
     * Setter for the identification files loaded
     * @param identificationFiles all identification files loaded
     */
    public void setIdentificationFiles(ArrayList<File> identificationFiles) {
        this.identificationFiles = identificationFiles;
    }

    /**
     * Getter for the creation date of the project
     * @return the creation date of the project
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Setter the creation date of the project
     * @param creationDate the creation date of the project
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
