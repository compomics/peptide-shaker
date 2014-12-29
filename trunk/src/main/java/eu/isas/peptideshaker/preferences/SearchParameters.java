package eu.isas.peptideshaker.preferences;

import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.massspectrometry.Charge;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class will compile the search options.
 *
 * @deprecated use com.compomics.util.experiment.identification.SearchParameters
 * instead
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SearchParameters implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.IdentificationParameters
     * instead
     */
    static final long serialVersionUID = 5456658018168469122L;

    /**
     * Precursor accuracy types.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    public enum PrecursorAccuracyType {

        PPM, DA
    };
    /**
     * The current precursor accuracy type.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private PrecursorAccuracyType currentPrecursorAccuracyType = PrecursorAccuracyType.PPM;
    /**
     * The ms2 ion tolerance.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private double fragmentIonMZTolerance = 0.5;
    /**
     * The expected modifications. Modified peptides will be grouped and
     * displayed according to this classification.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * instead
     */
    private ModificationProfile utilitiesModificationProfile = new ModificationProfile();
    /**
     * The expected modifications. Modified peptides will be grouped and
     * displayed according to this classification.
     *
     * @deprecated just here for backward compatibility
     */
    private eu.isas.peptideshaker.preferences.ModificationProfile modificationProfile;
    /**
     * The enzyme used for digestion.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private Enzyme enzyme;
    /**
     * The allowed number of missed cleavages.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private int nMissedCleavages;
    /**
     * The sequence database file used for identification.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private File fastaFile;
    /**
     * The searchGUI file loaded.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private File parametersFile;
    /**
     * The list of spectrum files.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private ArrayList<String> spectrumFiles = new ArrayList<String>();
    /**
     * The list of fraction molecular weights. The key is the fraction file
     * path.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private HashMap<String, Double> fractionMolecularWeights = new HashMap<String, Double>();
    /**
     * The first kind of ions searched for (typically a, b or c).
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private Integer forwardIon;
    /**
     * The second kind of ions searched for (typically x, y or z).
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private Integer rewindIon;
    /**
     * Convenience array for ion type selection.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private String[] ions = {"a", "b", "c", "x", "y", "z"};
    /**
     * Convenience array for forward ion type selection.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private String[] forwardIons = {"a", "b", "c"};
    /**
     * Convenience array for rewind ion type selection.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private String[] rewindIons = {"x", "y", "z"};
    /**
     * The precursor tolerance.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    private double precursorTolerance;

    /**
     * Constructor
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    public SearchParameters() {
    }

    /**
     * Returns a list containing the path of all spectrum files.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return a list containing the path of all spectrum files
     */
    public ArrayList<String> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Sets a new list of spectrum files.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param spectrumFiles the new list of spectrum files
     */
    public void setSpectrumFiles(ArrayList<String> spectrumFiles) {
        this.spectrumFiles = spectrumFiles;
    }

    /**
     * Adds a spectrum file to the list.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param spectrumFile a spectrum file
     */
    public void addSpectrumFile(String spectrumFile) {
        spectrumFiles.add(spectrumFile);
    }

    /**
     * Clears the list of spectrum files.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    public void clearSpectrumFilesList() {
        spectrumFiles = new ArrayList<String>();
    }

    /**
     * Returns the modification profile of the project.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the modification profile of the project
     */
    public ModificationProfile getModificationProfile() {
        // Compatibility check
        if (utilitiesModificationProfile == null) {
            utilitiesModificationProfile = new ModificationProfile();
            for (String utilitesName : modificationProfile.getUtilitiesNames()) {
                String psName = modificationProfile.getFamilyName(utilitesName);
                PTM modification = PTMFactory.getInstance().getPTM(psName);
                utilitiesModificationProfile.addVariableModification(modification);
                if (modificationProfile.getShortName(psName) != null) {
                    modification.setShortName(modificationProfile.getShortName(psName)); // @TODO: replace by ptmFactory.setShortName(...)?
                }
                utilitiesModificationProfile.setColor(psName, modificationProfile.getColor(psName));
            }
        }
        return utilitiesModificationProfile;
    }

    /**
     * sets the modification profile of the project.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param modificationProfile The modification profile
     */
    public void setModificationProfile(ModificationProfile modificationProfile) {
        this.utilitiesModificationProfile = modificationProfile;
    }

    /**
     * Returns the ms2 ion m/z tolerance.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the ms2 ion m/z tolerance
     */
    public double getFragmentIonAccuracy() {
        return fragmentIonMZTolerance;
    }

    /**
     * Sets the fragment ion m/z tolerance.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param fragmentIonMZTolerance the fragment ion mz tolerance
     */
    public void setFragmentIonAccuracy(double fragmentIonMZTolerance) {
        this.fragmentIonMZTolerance = fragmentIonMZTolerance;
    }

    /**
     * Returns the enzyme used for digestion.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the enzyme used for digestion
     */
    public Enzyme getEnzyme() {
        return enzyme;
    }

    /**
     * Sets the enzyme used for digestion.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param enzyme the enzyme used for digestion
     */
    public void setEnzyme(Enzyme enzyme) {
        this.enzyme = enzyme;
    }

    /**
     * Returns the parameters file loaded.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the parameters file loaded
     */
    public File getParametersFile() {
        return parametersFile;
    }

    /**
     * Sets the parameter file loaded.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param parametersFile the parameter file loaded
     */
    public void setParametersFile(File parametersFile) {
        this.parametersFile = parametersFile;
    }

    /**
     * Returns the sequence database file used for identification.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the sequence database file used for identification
     */
    public File getFastaFile() {
        return fastaFile;
    }

    /**
     * Sets the sequence database file used for identification.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param fastaFile the sequence database file used for identification
     */
    public void setFastaFile(File fastaFile) {
        this.fastaFile = fastaFile;
    }

    /**
     * Returns the allowed number of missed cleavages.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the allowed number of missed cleavages
     */
    public int getnMissedCleavages() {
        return nMissedCleavages;
    }

    /**
     * Sets the allowed number of missed cleavages.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param nMissedCleavages the allowed number of missed cleavages
     */
    public void setnMissedCleavages(int nMissedCleavages) {
        this.nMissedCleavages = nMissedCleavages;
    }

    /**
     * Getter for the first kind of ion searched.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the first kind of ion searched
     */
    public Integer getIonSearched1() {
        return forwardIon;
    }

    /**
     * Setter for the first kind of ion searched.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param ionSearched1 the first kind of ion searched
     */
    public void setIonSearched1(String ionSearched1) {
        if (ionSearched1.equals("a")) {
            this.forwardIon = PeptideFragmentIon.A_ION;
        } else if (ionSearched1.equals("b")) {
            this.forwardIon = PeptideFragmentIon.B_ION;
        } else if (ionSearched1.equals("c")) {
            this.forwardIon = PeptideFragmentIon.C_ION;
        } else if (ionSearched1.equals("x")) {
            this.forwardIon = PeptideFragmentIon.X_ION;
        } else if (ionSearched1.equals("y")) {
            this.forwardIon = PeptideFragmentIon.Y_ION;
        } else if (ionSearched1.equals("z")) {
            this.forwardIon = PeptideFragmentIon.Z_ION;
        }
    }

    /**
     * Getter for the second kind of ion searched.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the second kind of ion searched
     */
    public Integer getIonSearched2() {
        return rewindIon;
    }

    /**
     * Setter for the second kind of ion searched.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param ionSearched2 the second kind of ion searched
     */
    public void setIonSearched2(String ionSearched2) {
        if (ionSearched2.equals("a")) {
            this.rewindIon = PeptideFragmentIon.A_ION;
        } else if (ionSearched2.equals("b")) {
            this.rewindIon = PeptideFragmentIon.B_ION;
        } else if (ionSearched2.equals("c")) {
            this.rewindIon = PeptideFragmentIon.C_ION;
        } else if (ionSearched2.equals("x")) {
            this.rewindIon = PeptideFragmentIon.X_ION;
        } else if (ionSearched2.equals("y")) {
            this.rewindIon = PeptideFragmentIon.Y_ION;
        } else if (ionSearched2.equals("z")) {
            this.rewindIon = PeptideFragmentIon.Z_ION;
        }
    }

    /**
     * Getter for the list of ion symbols used.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the list of ion symbols used
     */
    public String[] getIons() {
        return ions;
    }

    /**
     * Returns the list of forward ions.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the forwardIons
     */
    public String[] getForwardIons() {

        // neeed for backwards compatibility
        if (forwardIons == null) {
            forwardIons = new String[]{"a", "b", "c"};
        }

        return forwardIons;
    }

    /**
     * Returns the list of rewind ions.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the rewindIons
     */
    public String[] getRewindIons() {

        // neeed for backwards compatibility
        if (rewindIons == null) {
            rewindIons = new String[]{"x", "y", "z"};
        }

        return rewindIons;
    }

    /**
     * Returns the precursor tolerance.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the precursor tolerance
     */
    public double getPrecursorAccuracy() {
        return precursorTolerance;
    }

    /**
     * Sets the precursor tolerance.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param precursorTolerance the precursor tolerance
     */
    public void setPrecursorAccuracy(double precursorTolerance) {
        this.precursorTolerance = precursorTolerance;
    }

    /**
     * Returns the precursor accuracy type.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the precursor accuracy type
     */
    public PrecursorAccuracyType getPrecursorAccuracyType() {
        return currentPrecursorAccuracyType;
    }

    /**
     * Sets the precursor accuracy type.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param currentPrecursorAccuracyType the precursor accuracy type
     */
    public void setPrecursorAccuracyType(PrecursorAccuracyType currentPrecursorAccuracyType) {
        this.currentPrecursorAccuracyType = currentPrecursorAccuracyType;
    }

    /**
     * Returns true if the current precursor accuracy type is ppm.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return true if the current precursor accuracy type is ppm
     */
    public boolean isPrecursorAccuracyTypePpm() {
        return currentPrecursorAccuracyType == PrecursorAccuracyType.PPM;
    }

    /**
     * Update for the version older than 0.15.1.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     */
    public void updateVersion() {
        if (forwardIon == null) {
            forwardIon = PeptideFragmentIon.B_ION;
        }
        if (rewindIon == null) {
            rewindIon = PeptideFragmentIon.Y_ION;
        }
    }

    /**
     * Returns the user provided molecular weights of the fractions. The key is
     * the fraction file path.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @return the user provided molecular weights of the fractions
     */
    public HashMap<String, Double> getFractionMolecularWeights() {
        return fractionMolecularWeights;
    }

    /**
     * Set the user provided molecular weights of the fractions. The key is the
     * fraction file path.
     *
     * @deprecated use
     * com.compomics.util.experiment.identification.SearchParameters instead
     * @param fractionMolecularWeights the fractionMolecularWeights to set
     */
    public void setFractionMolecularWeights(HashMap<String, Double> fractionMolecularWeights) {
        this.fractionMolecularWeights = fractionMolecularWeights;
    }

    /**
     * Returns an updated version of the search parameters. The following values
     * are missing, the given arbitrary value will be given: min charge
     * searched: 2+ max charge searched: 4+ max eValue: 100 hitlist length: 100
     * charge for multiple fragments: 3+ max pep length: 20 min pep length: 6
     * estimate charge: true remove precursor: false scale precursor: false
     *
     * @return an updated version of the search parameters
     */
    public com.compomics.util.experiment.identification.SearchParameters getUpdatedVersion() {
        com.compomics.util.experiment.identification.SearchParameters updatedVersion = new com.compomics.util.experiment.identification.SearchParameters();

        updatedVersion.setModificationProfile(getModificationProfile());
        if (fragmentIonMZTolerance>0) {
            updatedVersion.setFragmentIonAccuracy(fragmentIonMZTolerance);
        }
        if (enzyme != null) {
            updatedVersion.setEnzyme(enzyme);
        }
        if (parametersFile != null) {
            updatedVersion.setParametersFile(parametersFile);
        }
        if (fastaFile != null) {
            updatedVersion.setFastaFile(fastaFile);
        }
        if (nMissedCleavages >= 0) {
            updatedVersion.setnMissedCleavages(nMissedCleavages);
        }
        if (forwardIon != null) {
            updatedVersion.setIonSearched1(PeptideFragmentIon.getSubTypeAsString(forwardIon));
        }
        if (rewindIon != null) {
            updatedVersion.setIonSearched2(PeptideFragmentIon.getSubTypeAsString(rewindIon));
        }
        if (precursorTolerance > 0) {
            updatedVersion.setPrecursorAccuracy(precursorTolerance);
        }
        if (currentPrecursorAccuracyType != null) {
            if (currentPrecursorAccuracyType == PrecursorAccuracyType.PPM) {
                updatedVersion.setPrecursorAccuracyType(com.compomics.util.experiment.identification.SearchParameters.MassAccuracyType.PPM);
            } else {
                updatedVersion.setPrecursorAccuracyType(com.compomics.util.experiment.identification.SearchParameters.MassAccuracyType.DA);
            }
        }
        if (fractionMolecularWeights != null) {
            updatedVersion.setFractionMolecularWeights(fractionMolecularWeights);
        }
        updatedVersion.setMaxChargeSearched(new Charge(Charge.PLUS, 4));
        updatedVersion.setMinChargeSearched(new Charge(Charge.PLUS, 2));
        updatedVersion.setMaxEValue(100.0);
        updatedVersion.setHitListLength(100);
        updatedVersion.setMinimalChargeForMultipleChargedFragments(new Charge(Charge.PLUS, 3));
        updatedVersion.setMaxPeptideLength(20);
        updatedVersion.setMinPeptideLength(6);
        updatedVersion.setEstimateCharge(true);
        updatedVersion.setRemovePrecursor(false);
        updatedVersion.setScalePrecursor(false);

        return updatedVersion;
    }
}
