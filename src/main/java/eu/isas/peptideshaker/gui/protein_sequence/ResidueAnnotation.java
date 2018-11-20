package eu.isas.peptideshaker.gui.protein_sequence;

/**
 * Annotation for a given protein residue.
 * 
 * @author Harald Barsnes
 */
public class ResidueAnnotation {

    /**
     * Empty default constructor
     */
    public ResidueAnnotation() {
        annotation = "";
        identifier = 0;
        clickable = false;
    }

    /**
     * The residue annotation as a string.
     */
    public final String annotation;
    /**
     * A unique (external) identifier for the annotation.
     */
    public final long identifier;
    /**
     * If true the given annotation is clickable.
     */
    public final boolean clickable;

    /**
     * Create a new ResidueAnnotation object.
     *
     * @param annotation the residue annotation as a string
     * @param identifier a unique (external) identifier for the annotation
     * @param clickable if true the given annotation is clickable
     */
    public ResidueAnnotation(String annotation, long identifier, boolean clickable) {
        
        this.annotation = annotation;
        this.identifier = identifier;
        this.clickable = clickable;
        
    }
}
