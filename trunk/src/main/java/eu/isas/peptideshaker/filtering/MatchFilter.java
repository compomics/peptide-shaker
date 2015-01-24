package eu.isas.peptideshaker.filtering;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.RowFilter;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Abstract representing a filter.
 *
 * @author Marc Vaudel
 */
public abstract class MatchFilter implements Serializable, Filter {

    /**
     * Serial number for serialization compatibility.
     */
    static final long serialVersionUID = 7413446840381260115L;
    /**
     * Name of the filter.
     */
    protected String name;
    /**
     * Description of the filter.
     */
    protected String description = "";
    /**
     * Description of the condition to meet to pass the filter.
     */
    protected String condition = "";
    /**
     * Report when the filter is passed.
     */
    protected String reportPassed = "";
    /**
     * Report when the filter is not passed.
     */
    protected String reportFailed = "";
    /**
     * Boolean indicating whether the filter is active.
     */
    private boolean active = true;
    /**
     * The key of the manually validated matches.
     */
    private ArrayList<String> manualValidation = new ArrayList<String>();
    /**
     * The exceptions to the rule.
     */
    private ArrayList<String> exceptions = new ArrayList<String>();
    /**
     * Validation level.
     */
    private Integer validationLevel = null;
    /**
     * The type of comparison to be used for the confidence.
     */
    private RowFilter.ComparisonType validationComparison = RowFilter.ComparisonType.EQUAL;

    /**
     * Name of the manual selection filter.
     */
    public static final String MANUAL_SELECTION = "manual selection";

    /**
     * Enum for the type of possible filter.
     */
    public enum FilterType {

        /**
         * Protein filter.
         */
        PROTEIN,
        /**
         * Peptide filter.
         */
        PEPTIDE,
        /**
         * PSM filter.
         */
        PSM,
        /**
         * Peptide Assumption filter.
         */
        ASSUMPTION
    }
    /**
     * The type of filter.
     */
    protected FilterType filterType;

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getCondition() {
        return condition;
    }
    
    @Override
    public String getReport(boolean filterPassed) {
        if (filterPassed) {
            return reportPassed;
        } else {
            return reportFailed;
        }
    }

    /**
     * Sets the name of the filter.
     *
     * @param newName the name to be given to the filter
     */
    public void setName(String newName) {
        name = newName;
    }

    /**
     * Sets the description of the filter.
     *
     * @param description the description of the filter
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the description of the condition to meet.
     * 
     * @param condition the description of the condition to meet
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Sets the report when the filter is passed.
     * 
     * @param reportPassed the report when the filter is passed
     */
    public void setReportPassed(String reportPassed) {
        this.reportPassed = reportPassed;
    }

    /**
     * Sets the report when the filter is not passed.
     * 
     * @param reportFailed the report when the filter is not passed
     */
    public void setReportFailed(String reportFailed) {
        this.reportFailed = reportFailed;
    }
    
    

    /**
     * Return the type of the filter.
     *
     * @return the type of the filter
     */
    public FilterType getType() {
        return filterType;
    }

    /**
     * Indicates whether the filter is active.
     *
     * @return a boolean indicating whether the filter is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the filter is active.
     *
     * @param active a boolean indicating whether the filter is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns the exceptions to the rule.
     *
     * @return the exceptions to the rule
     */
    public ArrayList<String> getExceptions() {
        return exceptions;
    }

    /**
     * Returns the manually validated items.
     *
     * @return the manually validated items
     */
    public ArrayList<String> getManualValidation() {
        return manualValidation;
    }

    /**
     * Adds a manually validated Match.
     *
     * @param matchKey the key of the match to add
     */
    public void addManualValidation(String matchKey) {
        manualValidation.add(matchKey);
    }

    /**
     * Sets the list of manually validated keys.
     *
     * @param manualValidation list of manually validated keys
     */
    public void setManualValidation(ArrayList<String> manualValidation) {
        this.manualValidation = manualValidation;
    }

    /**
     * Adds an exception.
     *
     * @param matchKey the key of the exception to add
     */
    public void addException(String matchKey) {
        exceptions.add(matchKey);
    }

    /**
     * Sets the excepted matches.
     *
     * @param exceptions the excepted matches
     */
    public void setExceptions(ArrayList<String> exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * Removes a manually validated Match.
     *
     * @param matchKey the key of the match to remove
     */
    public void removeManualValidation(String matchKey) {
        manualValidation.remove(matchKey);
    }

    /**
     * Removes an exception.
     *
     * @param matchKey the key of the exception to remove
     */
    public void removeException(String matchKey) {
        exceptions.remove(matchKey);
    }

    /**
     * Returns the validation level used for filtering.
     * 
     * @return the validation level used for filtering
     */
    public Integer getValidationLevel() {
        return validationLevel;
    }

    /**
     * Sets the validation level used for filtering.
     * 
     * @param validationLevel the validation level used for filtering
     */
    public void setValidationLevel(Integer validationLevel) {
        this.validationLevel = validationLevel;
    }

    /**
     * Returns the comparison type used for validation level comparison.
     * 
     * @return the comparison type used for validation level comparison
     */
    public RowFilter.ComparisonType getValidationComparison() {
        return validationComparison;
    }

    /**
     * Sets the comparison type used for validation level comparison.
     * 
     * @param validationComparison the comparison type used for validation level comparison
     */
    public void setValidationComparison(RowFilter.ComparisonType validationComparison) {
        this.validationComparison = validationComparison;
    }
    
    @Override
    public abstract MatchFilter clone();

    /**
     * Tests whether a match is validated by this filter.
     *
     * @param matchKey the key of the match
     * @param identification the identification where to get the information
     * from
     * @param identificationFeaturesGenerator the identification features
     * generator providing identification features
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     *
     * @return a boolean indicating whether a match is validated by a given
     * filter
     *
     * @throws IOException thrown if an IOException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public abstract boolean isValidated(String matchKey, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException;
}
