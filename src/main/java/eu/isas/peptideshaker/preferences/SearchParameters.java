package eu.isas.peptideshaker.preferences;

import com.compomics.util.experiment.biology.Enzyme;
import java.io.File;
import java.util.HashMap;

/**
 * This class will compile the search options
 *
 * @author Marc
 */
public class SearchParameters {

    /**
     * The ms2 ion tolerance
     */
    private double fragmentIonMZTolerance = 0;
    /**
     * The expected modifications
     */
    private HashMap<String, Integer> expectedModifications = new HashMap<String, Integer>();
    /**
     * The enzyme used for digestion
     */
    private Enzyme enzyme;
    /**
     * The allowed number of missed cleavages
     */
    private int nMissedCleavages;
    /**
     * The sequence database file used for identification
     */
    private File fastaFile;
    /**
     * The searchGUI file loaded
     */
    private File parametersFile;
    /**
     * Constructor
     */
    public SearchParameters() {
    }

    /**
     * Returns the expected modifications map (Name -> maximal number of occurrences)
     * @return the expected modifications map (Name -> maximal number of occurrences)
     */
    public HashMap<String, Integer> getExpectedModifications() {
        return expectedModifications;
    }

    /**
     * Adds a modification to the expected modifications map
     * @param modificationName  The modification name
     * @param maximalOccurence  The maximal number of occurrences expected for the modification in a peptide
     */
    public void addExpectedModifications(String modificationName, int maximalOccurence) {
        expectedModifications.put(modificationName, maximalOccurence);
    }

    /**
     * Returns the ms2 ion m/z tolerance
     * @return the ms2 ion m/z tolerance
     */
    public double getFragmentIonMZTolerance() {
        return fragmentIonMZTolerance;
    }

    /**
     * Sets the fragment ion m/z tolerance
     * @param fragmentIonMZTolerance
     */
    public void setFragmentIonMZTolerance(double fragmentIonMZTolerance) {
        this.fragmentIonMZTolerance = fragmentIonMZTolerance;
    }


    /**
     * Returns the enzyme used for digestion
     * @return the enzyme used for digestion
     */
    public Enzyme getEnzyme() {
        return enzyme;
    }

    /**
     * Sets the enzyme used for digestion
     * @param enzyme the enzyme used for digestion
     */
    public void setEnzyme(Enzyme enzyme) {
        this.enzyme = enzyme;
    }

    /**
     * Returns the parameters file loaded
     * @return the parameters file loaded
     */
    public File getParametersFile() {
        return parametersFile;
    }

    /**
     * Sets the parameter file loaded
     * @param parametersFile the parameter file loaded
     */
    public void setParametersFile(File parametersFile) {
        this.parametersFile = parametersFile;
    }

    /**
     * Returns the sequence database file used for identification
     * @return the sequence database file used for identification
     */
    public File getFastaFile() {
        return fastaFile;
    }

    /**
     * Sets the sequence database file used for identification
     * @param fastaFile  the sequence database file used for identification
     */
    public void setFastaFile(File fastaFile) {
        this.fastaFile = fastaFile;
    }

    /**
     * Returns the allowed number of missed cleavages
     * @return the allowed number of missed cleavages
     */
    public int getnMissedCleavages() {
        return nMissedCleavages;
    }

    /**
     * Sets the allowed number of missed cleavages
     * @param nMissedCleavages  the allowed number of missed cleavages
     */
    public void setnMissedCleavages(int nMissedCleavages) {
        this.nMissedCleavages = nMissedCleavages;
    }

}
