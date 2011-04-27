package eu.isas.peptideshaker.fdrestimation;

import java.util.HashMap;

/**
 * This class brings together posterior validation results at a selected threshold
 *
 * @author Marc
 */
public class PosteriorValidationPoint {

    /**
     * The size of the possible peptide result space
     */
    public int resultSpaceSize = 0;
    /**
     * The overall size of the peptide result space
     */
    public int peptideSpaceSize = 0;
    /**
     * The specific size of the peptide result space
     */
    public HashMap<String, Integer> specificPeptideSpaceSize = new HashMap<String, Integer>();
    /**
     * The overall size of the PSM result space
     */
    public int psmSpaceSize = 0;
    /**
     * The specific size of the PSM result space
     */
    public HashMap<Integer, Integer> specificPsmSpaceSize = new HashMap<Integer, Integer>();
    /**
     * The number of peptides validated at pep = 1
     */
    public int peptideP1 = 0;
    /**
     * The number of PSMs validated at pep=1
     */
    public int psmP1 = 0;

/**
 * Constructor
 */
    public PosteriorValidationPoint() {
        
    }

    public void addPeptide(String peptideKey) {
        peptideSpaceSize++;
        if (!specificPeptideSpaceSize.containsKey(peptideKey)) {
            specificPeptideSpaceSize.put(peptideKey, 0);
        }
        specificPeptideSpaceSize.put(peptideKey, specificPeptideSpaceSize.get(peptideKey)+1);
    }

    public void addPsm(Integer psmKey) {
        psmSpaceSize++;
        if (!specificPsmSpaceSize.containsKey(psmKey)) {
            specificPsmSpaceSize.put(psmKey, 0);
        }
        specificPsmSpaceSize.put(psmKey, specificPsmSpaceSize.get(psmKey)+1);
    }


}
