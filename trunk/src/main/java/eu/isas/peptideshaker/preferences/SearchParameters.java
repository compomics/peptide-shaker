package eu.isas.peptideshaker.preferences;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class will compile the search options
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SearchParameters implements Serializable {

    /**
     * serial version UID for post-serialization compatibility
     */
    static final long serialVersionUID = 5456658018168469122L;
    /**
     * Precursor accuracy types.
     */
    public enum PrecursorAccuracyType {PPM, DA};
    /**
     * The current precursor accuracy type,
     */
    private PrecursorAccuracyType currentPrecursorAccuracyType = PrecursorAccuracyType.PPM;
    /**
     * The ms2 ion tolerance
     */
    private double fragmentIonMZTolerance = 0.5;
    /**
     * The expected modifications. Modified peptides will be grouped and displayed according to this classification.
     */
    private ModificationProfile modificationProfile = new ModificationProfile();
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
     * The precursor tolerance
     */
    private double precursorTolerance;

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
     * Sets a new list of spectrum files
     * @param spectrumFiles the new list of spectrum files
     */
    public void setSpectrumFiles(ArrayList<String> spectrumFiles) {
        this.spectrumFiles = spectrumFiles;
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
     * Returns the modification profile of the project
     * @return the modification profile of the project
     */
    public ModificationProfile getModificationProfile() {
        return modificationProfile;
    }

    /**
     * sets the modification profile of the project
     * @param modificationProfile  The modification profile
     */
    public void setModificationProfile(ModificationProfile modificationProfile) {
        this.modificationProfile = modificationProfile;
    }

    /**
     * Returns the ms2 ion m/z tolerance
     * @return the ms2 ion m/z tolerance
     */
    public double getFragmentIonAccuracy() {
        return fragmentIonMZTolerance;
    }

    /**
     * Sets the fragment ion m/z tolerance
     * @param fragmentIonMZTolerance
     */
    public void setFragmentIonAccuracy(double fragmentIonMZTolerance) {
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
     * @param ionSearched2 the second kind of ion searched 
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

    /**
     * Returns the precursor tolerance
     * @return the precursor tolerance 
     */
    public double getPrecursorAccuracy() {
        return precursorTolerance;
    }

    /**
     * Sets the precursor tolerance
     * @param precursorTolerance the precursor tolerance
     */
    public void setPrecursorAccuracy(double precursorTolerance) {
        this.precursorTolerance = precursorTolerance;
    }

    /**
     * Returns the precursor accuracy type.
     * 
     * @return the precursor accuracy type 
     */
    public PrecursorAccuracyType getPrecursorAccuracyType() {
        return currentPrecursorAccuracyType;
    }

    /**
     * Sets the precursor accuracy type.
     * 
     * @param currentPrecursorAccuracyType the precursor accuracy type
     */
    public void setPrecursorAccuracyType(PrecursorAccuracyType currentPrecursorAccuracyType) {
        this.currentPrecursorAccuracyType = currentPrecursorAccuracyType;
    }
    
    /**
     * Returns true if the current precursor accuracy type is ppm.
     * 
     * @return true if the current precursor accuracy type is ppm
     */
    public boolean isPrecursorAccuracyTypePpm() {
        return currentPrecursorAccuracyType == PrecursorAccuracyType.PPM;
    }
}
