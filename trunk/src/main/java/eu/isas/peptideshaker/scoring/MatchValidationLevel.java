/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.scoring;

/**
 * enum listing the different levels of match validation
 *
 * @author Marc
 */
public enum MatchValidationLevel {

    none(-1, "Ø"),
    not_validated(0, "Not Validated"),
    doubtful(1, "Doubtful"),
    confident(2, "Confident");
    /**
     * The index associated to this possibility. The higher the id, the better the confidence.
     */
    private int index;
    /**
     * The name of this possibility
     */
    private String name;

    /**
     * Constructor
     *
     * @param index the index associated to this possibility
     * @param name the name of this possibility
     */
    private MatchValidationLevel(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * Returns the index associated to this possibility
     * @return the index associated to this possibility
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the name of this possibility
     * @return the name of this possibility
     */
    public String getName() {
        return name;
    }
    /**
     * Indicates whether this level is considered as validated.
     * 
     * @return a boolean indicating whether this level is considered as validated.
     */
    public boolean isValidated() {
        return this == doubtful || this == confident;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
}
