package eu.isas.peptideshaker.preferences;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class will compile the search options
 *
 * @author Marc
 */
public class SearchParameters implements Serializable {

    /**
     * The ms2 ion tolerance
     */
    private double fragmentIonMZTolerance = 0;
    /**
     * The expected modifications as a map Name -> modification family. Modified peptides will be grouped according to this parameter.
     */
    private HashMap<String, String> modificationProfile = new HashMap<String, String>();
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
     * The list of spectrum files
     */
    private ArrayList<String> spectrumFiles = new ArrayList<String>();
    /**
     * The first kind of ions searched for (typically a, b or c)
     */
    private PeptideFragmentIonType ionSearched1;
    /**
     * The second kind of ions searched for (typically x, y or z)
     */
    private PeptideFragmentIonType ionSearched2;
    /**
     * Convenience Array for ion type selection
     */
    private String[] ions = {"a", "b", "c", "x", "y", "z"};

    /**
     * Constructor
     */
    public SearchParameters() {
    }

    /**
     * Returns a list containing the path of all spectrum files
     * @return a list containing the path of all spectrum files 
     */
    public ArrayList<String> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Adds a spectrum file to the list
     * @param spectrumFile a spectrum file
     */
    public void addSpectrumFile(String spectrumFile) {
        spectrumFiles.add(spectrumFile);
    }

    /**
     * Clears the list of spectrum files
     */
    public void clearSpectrumFilesList() {
        spectrumFiles = new ArrayList<String>();
    }

    /**
     * Returns the expected modifications map (Name -> modification family)
     * @return the expected modifications map (Name -> modification family)
     */
    public HashMap<String, String> getModificationProfile() {
        return modificationProfile;
    }

    /**
     * Adds a modification to the expected modifications map
     * @param modificationName  The modification name
     * @param modificationFamily  The family of the modification. Modified peptides will be grouped according to this parameter.
     */
    public void addExpectedModification(String modificationName, String modificationFamily) {
        modificationProfile.put(modificationName, modificationFamily);
    }

    /**
     * Clears the modification profile
     */
    public void clearModificationProfile() {
        modificationProfile.clear();
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

    /**
     * Getter for the first kind of ion searched
     * @return the first kind of ion searched
     */
    public PeptideFragmentIonType getIonSearched1() {
        return ionSearched1;
    }

    /**
     * Setter for the first kind of ion searched
     * @param ionSearched1 the first kind of ion searched 
     */
    public void setIonSearched1(String ionSearched1) {
        if (ionSearched1.equals("a")) {
            this.ionSearched1 = PeptideFragmentIonType.A_ION;
        } else if (ionSearched1.equals("b")) {
            this.ionSearched1 = PeptideFragmentIonType.B_ION;
        } else if (ionSearched1.equals("c")) {
            this.ionSearched1 = PeptideFragmentIonType.C_ION;
        } else if (ionSearched1.equals("x")) {
            this.ionSearched1 = PeptideFragmentIonType.X_ION;
        } else if (ionSearched1.equals("y")) {
            this.ionSearched1 = PeptideFragmentIonType.Y_ION;
        } else if (ionSearched1.equals("z")) {
            this.ionSearched1 = PeptideFragmentIonType.Z_ION;
        }
    }

    /**
     * Getter for the second kind of ion searched
     * @return the second kind of ion searched
     */
    public PeptideFragmentIonType getIonSearched2() {
        return ionSearched2;
    }

    /**
     * Setter for the second kind of ion searched
     * @param ionSearched1 the second kind of ion searched 
     */
    public void setIonSearched2(String ionSearched2) {
        if (ionSearched2.equals("a")) {
            this.ionSearched2 = PeptideFragmentIonType.A_ION;
        } else if (ionSearched2.equals("b")) {
            this.ionSearched2 = PeptideFragmentIonType.B_ION;
        } else if (ionSearched2.equals("c")) {
            this.ionSearched2 = PeptideFragmentIonType.C_ION;
        } else if (ionSearched2.equals("x")) {
            this.ionSearched2 = PeptideFragmentIonType.X_ION;
        } else if (ionSearched2.equals("y")) {
            this.ionSearched2 = PeptideFragmentIonType.Y_ION;
        } else if (ionSearched2.equals("z")) {
            this.ionSearched2 = PeptideFragmentIonType.Z_ION;
        }
    }

    /**
     * Getter for the list of ion symbols used
     * @return the list of ion symbols used
     */
    public String[] getIons() {
        return ions;
    }
}
