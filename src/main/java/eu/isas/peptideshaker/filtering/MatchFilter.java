/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.identification.IdentificationMatch;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Abstract representing a filter
 *
 * @author marc
 */
public abstract class MatchFilter implements Serializable {
    
    /**
     * Name of the filter
     */
    protected String name;
    /**
     * Description of the filter
     */
    protected String description = "";
    /**
     * boolean indicating whether the filter is active
     */
    private boolean active = true;
    /**
     * The key of the manually validated matches
     */
    private ArrayList<String> manualValidation = new ArrayList<String>();
    /**
     * The exceptions to the rule
     */
    private ArrayList<String> exceptions = new ArrayList<String>();
    /**
     * Enum for the type of possible filter
     */
    public enum FilterType {
        /**
         * a Protein filter
         */
        PROTEIN,
        /**
         * a Peptide filter
         */
        PEPTIDE,
        /**
         * a PSM filter
         */
        PSM
    }
    /**
     * Convenience enum for the comparison with a given threshold
     */
    public enum ComparisonType {
        SMALLER_THAN,
        EQUAL,
        BIGGER_THAN
    }
    /**
     * The type of filter
     */
    protected FilterType filterType;
    /**
     * Return the name of the filter
     * @return the name of the filter
     */
    public String getName() {
        return name;
    }
    /**
     * Return the type of the filter
     * @return the type of the filter
     */
    public FilterType getType() {
        return filterType;
    }

    /**
     * Returns the description of the filter
     * @return the description of the filter
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the filter
     * @param description the description of the filter
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Indicates whether the filter is active
     * @return a boolean indicating whether the filter is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the filter is active
     * @param active a boolean indicating whether the filter is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    /**
     * Returns the exceptions to the rule
     * @return the exceptions to the rule 
     */
    public ArrayList<String> getExceptions() {
        return exceptions;
    }
    /**
     * Returns the manually validated items
     * @return the manually validated items
     */
    public ArrayList<String> getManualValidation() {
        return manualValidation;
    }
    /**
     * Adds a manually validated Match
     * @param matchKey the key of the match to add
     */
    public void addManualValidation(String matchKey) {
        manualValidation.add(matchKey);
    }
    /**
     * Adds an exception
     * @param matchKey the key of the exception to add
     */
    public void addException(String matchKey) {
        exceptions.add(matchKey);
    }
    /**
     * Removes a manually validated Match
     * @param matchKey the key of the match to remove
     */
    public void removeManualValidation(String matchKey) {
        manualValidation.remove(matchKey);
    }
    /**
     * Removes an exception
     * @param matchKey the key of the exception to remove
     */
    public void removeException(String matchKey) {
        exceptions.remove(matchKey);
    }
}
