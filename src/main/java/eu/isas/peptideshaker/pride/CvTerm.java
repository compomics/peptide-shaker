
package eu.isas.peptideshaker.pride;

import java.io.Serializable;

/**
 * A simple CvTerm object.
 * 
 * @author Harald Barsnes
 */
public class CvTerm implements Serializable {
    
    /**
     * serialization number for backward compatibility
     */
    static final long serialVersionUID = -2890434198335005181L;
    /**
     * The ontology.
     */
    private String ontology;
    /**
     * The accession number.
     */
    private String accession;
    /**
     * The name/term.
     */
    private String name;
    /**
     * The value for the given term.
     */
    private String value;
    
    /**
     * Create a new CV term.
     * 
     * @param ontology
     * @param accession
     * @param name
     * @param value 
     */
    public CvTerm(String ontology, String accession, String name, String value) {
        this.ontology = ontology;
        this.accession = accession;
        this.name = name;
        this.value = value;
    }

    /**
     * @return the ontology
     */
    public String getOntology() {
        return ontology;
    }

    /**
     * @param ontology the ontology to set
     */
    public void setOntology(String ontology) {
        this.ontology = ontology;
    }

    /**
     * @return the accession
     */
    public String getAccession() {
        return accession;
    }

    /**
     * @param accession the accession to set
     */
    public void setAccession(String accession) {
        this.accession = accession;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}
