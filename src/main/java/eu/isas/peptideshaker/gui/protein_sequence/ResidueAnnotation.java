package eu.isas.peptideshaker.gui.protein_sequence;

/**
 * Annotation for a given protein residue.
 * 
 * @author Harald Barsnes
 */
public class ResidueAnnotation {

    /**
     * The residue annotation as a string.
     */
    private final String annotation;
    /**
     * A unique (external) identifier for the annotaton.
     */
    private final String identifier;
    /**
     * If true the given annotation is clickable.
     */
    private final boolean clickable;

    /**
     * Create a new ResidueAnnotation object.
     *
     * @param annotation the residue annotation as a string
     * @param identifier a unique (external) identifier for the annotaton
     * @param clickable if true the given annotation is clickable
     */
    public ResidueAnnotation(String annotation, String identifier, boolean clickable) {
        this.annotation = annotation;
        this.identifier = identifier;
        this.clickable = clickable;
    }

    /**
     * Returns the annotation as a string.
     * 
     * @return the annotation
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Returns the (externally) unique identifier.
     * 
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns true if the given annotation is clickable.
     * 
     * @return the clickable
     */
    public boolean isClickable() {
        return clickable;
    }
}
