/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.preferences;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * This class contains the details about a project
 *
 * @author marc
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
     * List of the spectrum files loaded
     */
    private ArrayList<File> spectrumFiles;
    /**
     * The database file loaded
     */
    private File dbFile;
    /**
     * The Mascot e-value limit used for the import
     */
    private double mascotEValue;
    /**
     * The OMSSA e-value limit used for the import
     */
    private double omssaEValue;
    /**
     * The X!Tandem e-value limit used for the import
     */
    private double xTandemEValue;
    /**
     * The precursor mass limit used for the import
     */
    private double precursorError;
    /**
     * The unit of the precursor limit used
     */
    private boolean precursorErrorPpm;
    /**
     * The minimal number of amino acid used
     */
    private int nAAmin;
    /**
     * The maximal number of amino acid used
     */
    private int naaMax;
    /**
     * The date of creation of the project
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
     * Getter for the maximal Mascot e-value allowed
     * @return the maximal Mascot e-value allowed
     */
    public double getMascotEValue() {
        return mascotEValue;
    }

    /**
     * Setter for the maximal Mascot e-value allowed
     * @param mascotEValue the maximal Mascot e-value allowed
     */
    public void setMascotEValue(double mascotEValue) {
        this.mascotEValue = mascotEValue;
    }

    /**
     * Getter for the minimal peptide length imported
     * @return the minimal peptide length imported
     */
    public int getnAAmin() {
        return nAAmin;
    }

    /**
     * Setter for the minimal peptide length imported
     * @param nAAmin the minimal peptide length imported
     */
    public void setnAAmin(int nAAmin) {
        this.nAAmin = nAAmin;
    }

    /**
     * Getter for the maximal peptide length imported
     * @return the maximal peptide length imported
     */
    public int getNaaMax() {
        return naaMax;
    }

    /**
     * Setter for the maximal peptide length imported
     * @param naaMax the maximal peptide length imported
     */
    public void setNaaMax(int naaMax) {
        this.naaMax = naaMax;
    }

    /**
     * Getter for the maximal OMSSA e-value imported
     * @return the maximal OMSSA e-value imported
     */
    public double getOmssaEValue() {
        return omssaEValue;
    }

    /**
     * Setter for the maximal OMSSA e-value imported
     * @param omssaEValue the maximal OMSSA e-value imported
     */
    public void setOmssaEValue(double omssaEValue) {
        this.omssaEValue = omssaEValue;
    }

    /**
     * Getter for the maximal precursor mass deviation allowed
     * @return the maximal precursor mass deviation allowed
     */
    public double getPrecursorError() {
        return precursorError;
    }

    /**
     * Setter for the maximal precursor mass deviation allowed
     * @param precursorError the maximal precursor mass deviation allowed
     */
    public void setPrecursorError(double precursorError) {
        this.precursorError = precursorError;
    }

    /**
     * Returns true if the maximal precursor mass deviation is in ppm
     * @return true if the maximal precursor mass deviation is in ppm
     */
    public boolean isPrecursorErrorPpm() {
        return precursorErrorPpm;
    }

    /**
     * Sets whether the maximal precursor mass deviation is in ppm
     * @param precursorErrorPpm a boolean indicating whether the maximal precursor mass deviation is in ppm
     */
    public void setPrecursorErrorPpm(boolean precursorErrorPpm) {
        this.precursorErrorPpm = precursorErrorPpm;
    }

    /**
     * Getter for the spectrum files used
     * @return the spectrum files used
     */
    public ArrayList<File> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Setter for the spectrum files used
     * @param spectrumFiles the spectrum files used
     */
    public void setSpectrumFiles(ArrayList<File> spectrumFiles) {
        this.spectrumFiles = spectrumFiles;
    }

    /**
     * Getter for the X!Tandem maximal e-value allowed during the import
     * @return the X!Tandem maximal e-value allowed during the import
     */
    public double getxTandemEValue() {
        return xTandemEValue;
    }

    /**
     * Setter for the X!Tandem maximal e-value allowed during the import
     * @param xTandemEValue the X!Tandem maximal e-value allowed during the import
     */
    public void setxTandemEValue(double xTandemEValue) {
        this.xTandemEValue = xTandemEValue;
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
