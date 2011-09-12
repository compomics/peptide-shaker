package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.AdvocateFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class will generate the output as requested by the user
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FeaturesGenerator {

    /**
     * The main gui
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The corresponding identification
     */
    private Identification identification;
    /**
     * The separator (tab by default)
     */
    public static final String SEPARATOR = "\t";
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    /**
     * Constructor
     * @param peptideShakerGUI
     */
    public FeaturesGenerator(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
    }

    /**
     * Returns the desired protein output based on the elements needed as provided in arguments
     * 
     * @param progressDialog the progress dialog (can be null)
     * @param proteinKeys 
     * @param onlyValidated
     * @param accession
     * @param piDetails
     * @param description
     * @param nPeptides
     * @param emPAI
     * @param sequenceCoverage 
     * @param nSpectra
     * @param nsaf
     * @param score
     * @param confidence
     * @return
     * @throws Exception  
     */
    public String getProteinsOutput(ProgressDialogX progressDialog, ArrayList<String> proteinKeys, boolean onlyValidated, boolean accession, boolean piDetails,
            boolean description, boolean nPeptides, boolean emPAI, boolean sequenceCoverage, boolean nSpectra, boolean nsaf,
            boolean score, boolean confidence) throws Exception {

        if (proteinKeys == null) {
            proteinKeys = identification.getProteinIdentification();
        }
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(proteinKeys.size());
        }

        String result = "";
        if (accession) {
            result += "Accession" + SEPARATOR;
        }
        if (piDetails) {
            result += "Protein Inference Class" + SEPARATOR;
            result += "Other Protein(s)" + SEPARATOR;
        }
        if (description) {
            result += "Description" + SEPARATOR;
        }
        if (sequenceCoverage) {
            result += "Sequence Coverage" + SEPARATOR;
        }
        if (nPeptides) {
            result += "#Validated Peptides" + SEPARATOR;
        }
        if (emPAI) {
            result += "emPAI" + SEPARATOR;
        }
        if (nSpectra) {
            result += "#Validated Spectra" + SEPARATOR;
        }
        if (nsaf) {
            result += "NSAF" + SEPARATOR;
        }
        if (score) {
            result += "Score" + SEPARATOR;
        }
        if (confidence) {
            result += "Confidence" + SEPARATOR;
        }
        if (!onlyValidated) {
            result += "Validated" + SEPARATOR;
            result += "Decoy" + SEPARATOR;
        }
        result += "\n";

        PSParameter proteinPSParameter = new PSParameter();
        PSParameter secondaryPSParameter = new PSParameter();
        int cpt, progress = 0;
        ProteinMatch proteinMatch;
        for (String proteinKey : proteinKeys) {
            proteinPSParameter = (PSParameter) identification.getMatchParameter(proteinKey, proteinPSParameter);
            if (!onlyValidated || proteinPSParameter.isValidated() && !ProteinMatch.isDecoy(proteinKey)) {
                proteinMatch = identification.getProteinMatch(proteinKey);
                if (accession) {
                    result += proteinMatch.getMainMatch() + SEPARATOR;
                }
                if (piDetails) {
                    result += proteinPSParameter.getGroupName() + SEPARATOR;
                    for (String otherProtein : proteinMatch.getTheoreticProteinsAccessions()) {
                        boolean first = true;
                        if (!otherProtein.equals(proteinMatch.getMainMatch())) {
                            if (first) {
                                first = false;
                            } else {
                                result += ", ";
                            }
                            result += otherProtein;
                        }
                    }
                    result += SEPARATOR;
                }
                if (description) {
                    result += sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription() + SEPARATOR;
                }
                if (sequenceCoverage) {
                    result += peptideShakerGUI.estimateSequenceCoverage(proteinMatch, sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence()) + SEPARATOR;
                }
                if (nPeptides || emPAI) {
                    Protein mainMatch = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                    cpt = 0;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        secondaryPSParameter = (PSParameter) identification.getMatchParameter(peptideKey, secondaryPSParameter);
                        if (secondaryPSParameter.isValidated()) {
                            cpt++;
                        }
                    }
                    if (nPeptides) {
                        result += cpt + SEPARATOR;
                    }
                    if (emPAI) {
                        double pai = cpt;
                        pai = pai / mainMatch.getNPossiblePeptides(peptideShakerGUI.getSearchParameters().getEnzyme());
                        double empai = Math.pow(10, pai) - 1;
                        result += empai + SEPARATOR;
                    }
                }
                if (nSpectra || nsaf) {
                    Protein mainMatch = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                    cpt = 0;
                    PeptideMatch peptideMatch;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                            secondaryPSParameter = (PSParameter) identification.getMatchParameter(spectrumKey, secondaryPSParameter);
                            if (secondaryPSParameter.isValidated()) {
                                cpt++;
                            }
                        }
                    }
                    if (nSpectra) {
                        result += cpt + SEPARATOR;
                    }
                    if (nsaf) {
                        double index = cpt;
                        index = index / mainMatch.getSequence().length();
                        result += index + SEPARATOR;
                    }
                }
                if (score) {
                    result += proteinPSParameter.getProteinScore() + SEPARATOR;
                }
                if (confidence) {
                    result += proteinPSParameter.getProteinConfidence() + SEPARATOR;
                }
                if (!onlyValidated) {
                    if (proteinPSParameter.isValidated()) {
                        result += 1 + SEPARATOR;
                    } else {
                        result += 0 + SEPARATOR;
                    }
                    if (proteinMatch.isDecoy()) {
                        result += 1 + SEPARATOR;
                    } else {
                        result += 0 + SEPARATOR;
                    }
                }
                result += "\n";
            }
        }
        progress++;
        if (progressDialog != null) {
            progressDialog.setValue(progress);
        }
        return result;
    }

    /**
     * Returns the peptide output based on the given arguments.
     * 
     * @param progressDialog the progress dialog (can be null)
     * @param peptideKeys
     * @param onlyValidated
     * @param accession
     * @param location 
     * @param sequence
     * @param modifications
     * @param nSpectra
     * @param ptmLocations 
     * @param score
     * @param confidence
     * @return
     * @throws Exception  
     */
    public String getPeptidesOutput(ProgressDialogX progressDialog, ArrayList<String> peptideKeys, boolean onlyValidated, boolean accession,
            boolean location, boolean sequence, boolean modifications, boolean ptmLocations,
            boolean nSpectra, boolean score, boolean confidence) throws Exception {

        if (peptideKeys == null) {
            peptideKeys = identification.getPeptideIdentification();
        }
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(peptideKeys.size());
        }


        String result = "";
        if (accession) {
            result += "Protein(s)" + SEPARATOR;
        }
        if (location) {
            result += "Peptide Start" + SEPARATOR;
        }
        if (sequence) {
            result += "Sequence" + SEPARATOR;
        }
        if (modifications || ptmLocations) {
            result += "Variable Modification" + SEPARATOR;
            result += "Modification Location" + SEPARATOR;
        }
        if (nSpectra) {
            result += "#Validated Spectra" + SEPARATOR;
        }
        if (score) {
            result += "Score" + SEPARATOR;
        }
        if (confidence) {
            result += "Confidence" + SEPARATOR;
        }
        if (!onlyValidated) {
            result += "Validated" + SEPARATOR;
            result += "Decoy" + SEPARATOR;
        }
        result += "\n";

        PSParameter peptidePSParameter = new PSParameter();
        PSParameter secondaryPSParameter = new PSParameter();
        PSPtmScores ptmScores = new PSPtmScores();
        Peptide peptide;
        PeptideMatch peptideMatch;
        int progress = 0;
        for (String peptideKey : peptideKeys) {
            peptideMatch = identification.getPeptideMatch(peptideKey);
            peptidePSParameter = (PSParameter) identification.getMatchParameter(peptideKey, peptidePSParameter);
            if (!onlyValidated || peptidePSParameter.isValidated() && !peptideMatch.isDecoy()) {
                peptide = peptideMatch.getTheoreticPeptide();
                if (accession) {
                    boolean first = true;
                    for (String protein : peptide.getParentProteins()) {
                        if (first) {
                            first = false;
                        } else {
                            result += ", ";
                        }
                        result += protein;
                    }
                    result += SEPARATOR;
                }
                if (location) {
                    if (peptide.getParentProteins().size() == 1) {
                        ArrayList<Integer> positions = new ArrayList<Integer>();
                        String tempSequence = sequenceFactory.getProtein(peptide.getParentProteins().get(0)).getSequence();
                        int index = tempSequence.indexOf(peptide.getSequence());
                        while (index >= 0 && tempSequence.length() > 1) {
                            positions.add(index);
                            tempSequence = tempSequence.substring(index + peptide.getSequence().length());
                            index = tempSequence.indexOf(peptide.getSequence());
                        }
                        boolean first = true;
                        for (int position : positions) {
                            if (first) {
                                first = false;
                            } else {
                                result += ", ";
                            }
                            result += position;
                        }
                    }
                    result += SEPARATOR;
                }
                if (sequence) {
                    result += peptide.getSequence() + SEPARATOR;
                }
                if (modifications || ptmLocations) {
                    ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
                    boolean first = true;
                    for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                        if (modificationMatch.isVariable()) {
                            if (first) {
                                first = false;
                            } else {
                                result += ", ";
                            }
                            String modName = modificationMatch.getTheoreticPtm().getName();
                            result += modName + "(" + modificationMatch + ")";
                        }
                    }
                    result += SEPARATOR;
                    result += "Not implemented" + SEPARATOR;
                }
                if (nSpectra) {
                    int cpt = 0;
                    for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                        secondaryPSParameter = (PSParameter) identification.getMatchParameter(spectrumKey, secondaryPSParameter);
                        if (secondaryPSParameter.isValidated()) {
                            cpt++;
                        }
                    }
                    result += cpt + SEPARATOR;
                }
                if (score) {
                    result += peptidePSParameter.getPeptideScore() + SEPARATOR;
                }
                if (confidence) {
                    result += peptidePSParameter.getPeptideConfidence() + SEPARATOR;
                }
                if (!onlyValidated) {
                    if (peptidePSParameter.isValidated()) {
                        result += 1 + SEPARATOR;
                    } else {
                        result += 0 + SEPARATOR;
                    }
                    if (peptideMatch.isDecoy()) {
                        result += 1 + SEPARATOR;
                    } else {
                        result += 0 + SEPARATOR;
                    }
                }
                result += "\n";
            }
            progress++;
            if (progressDialog != null) {
                progressDialog.setValue(progress);
            }
        }
        return result;
    }

    /**
     * returns the PSM output based on the given argument
     * 
     * @param progressDialog the progress dialog (can be null)
     * @param psmKeys
     * @param onlyValidated
     * @param accessions
     * @param sequence
     * @param modification
     * @param location
     * @param file
     * @param title
     * @param precursor
     * @param score
     * @param confidence
     * @return
     * @throws Exception 
     */
    public String getPSMsOutput(ProgressDialogX progressDialog, ArrayList<String> psmKeys, boolean onlyValidated, boolean accessions, boolean sequence, boolean modification,
            boolean location, boolean file, boolean title, boolean precursor, boolean score, boolean confidence) throws Exception {

        if (psmKeys == null) {
            psmKeys = identification.getSpectrumIdentification();
        }
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(psmKeys.size());
        }


        String result = "";
        if (accessions) {
            result += "Protein(s)" + SEPARATOR;
        }
        if (sequence) {
            result += "Sequence" + SEPARATOR;
        }
        if (modification || location) {
            result += "Variable Modification(s)" + SEPARATOR;
            result += "Modification Location" + SEPARATOR;
        }
        if (file) {
            result += "Spectrum File" + SEPARATOR;
        }
        if (title) {
            result += "Spectrum Title" + SEPARATOR;
        }
        if (precursor) {
            result += "Precursor m/z" + SEPARATOR;
            result += "Precursor Charge" + SEPARATOR;
            result += "Precursor Retention Time" + SEPARATOR;
            result += "Peptide Theoretic Mass" + SEPARATOR;
            result += "Mass Error [ppm]" + SEPARATOR;
        }
        if (score) {
            result += "Score" + SEPARATOR;
        }
        if (confidence) {
            result += "Confidence" + SEPARATOR;
        }
        if (!onlyValidated) {
            result += "Validated" + SEPARATOR;
            result += "Decoy" + SEPARATOR;
        }
        result += "\n";

        PSParameter psParameter = new PSParameter();
        PeptideAssumption bestAssumption;
        SpectrumMatch spectrumMatch;
        int progress = 0;
        for (String psmKey : psmKeys) {
            spectrumMatch = identification.getSpectrumMatch(psmKey);
            psParameter = (PSParameter) identification.getMatchParameter(psmKey, psParameter);
            bestAssumption = spectrumMatch.getBestAssumption();
            if (!onlyValidated || psParameter.isValidated() && !bestAssumption.isDecoy()) {
                if (accessions) {
                    boolean first = true;
                    for (String protein : bestAssumption.getPeptide().getParentProteins()) {
                        if (first) {
                            first = false;
                        } else {
                            result += ", ";
                        }
                        result += protein;
                    }
                    result += SEPARATOR;
                }
                if (sequence) {
                    result += bestAssumption.getPeptide().getSequence() + SEPARATOR;
                }
                if (modification || location) {
                    boolean first = true;
                    for (ModificationMatch modificationMatch : bestAssumption.getPeptide().getModificationMatches()) {
                        if (modificationMatch.isVariable()) {
                            if (first) {
                                first = false;
                            } else {
                                result += ", ";
                            }
                            String modName = modificationMatch.getTheoreticPtm().getName();
                            result += modName + "(" + modificationMatch + ")";
                        }
                    }
                    result += SEPARATOR;
                    result += "Not implemented" + SEPARATOR;
                }
                if (file) {
                    result += Spectrum.getSpectrumFile(spectrumMatch.getKey()) + SEPARATOR;
                }
                if (title) {
                    result += Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR;
                }
                if (precursor) {
                    Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                    result += prec.getMz() + SEPARATOR;
                    result += prec.getCharge() + SEPARATOR;
                    result += prec.getRt() + SEPARATOR;
                    result += bestAssumption.getPeptide().getMass() + SEPARATOR;
                    result += bestAssumption.getDeltaMass() + SEPARATOR;
                }
                if (score) {
                    result += psParameter.getPsmScore() + SEPARATOR;
                }
                if (confidence) {
                    result += psParameter.getPsmConfidence() + SEPARATOR;
                }
                if (!onlyValidated) {
                    if (psParameter.isValidated()) {
                        result += 1 + SEPARATOR;
                    } else {
                        result += 0 + SEPARATOR;
                    }
                    if (bestAssumption.isDecoy()) {
                        result += 1 + SEPARATOR;
                    } else {
                        result += 0 + SEPARATOR;
                    }
                }
                result += "\n";
            }
            progress++;
            if (progressDialog != null) {
                progressDialog.setValue(progress);
            }
        }
        return result;
    }

    /**
     * Returns the assumption output based on the given arguments
     * 
     * @param progressDialog the progress dialog (can be null)
     * @param psmKeys 
     * @param onlyValidated 
     * @param accession
     * @param sequence
     * @param modifications
     * @param locations
     * @param file
     * @param title
     * @param precursor
     * @param scores
     * @param confidence
     * @return
     * @throws Exception 
     */
    public String getAssumptionsOutput(ProgressDialogX progressDialog, ArrayList<String> psmKeys, boolean onlyValidated,
            boolean accession, boolean sequence, boolean modifications, boolean locations,
            boolean file, boolean title, boolean precursor, boolean scores, boolean confidence) throws Exception {

        if (psmKeys == null) {
            psmKeys = identification.getSpectrumIdentification();
        }
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(psmKeys.size());
        }

        String result = "";
        result += "Search Engine" + SEPARATOR;
        result += "Rank" + SEPARATOR;
        if (accession) {
            result += "Accession" + SEPARATOR;
        }
        if (sequence) {
            result += "Sequence" + SEPARATOR;
        }
        if (modifications || locations) {
            result += "Variable modifications" + SEPARATOR;
            result += "Not implemented" + SEPARATOR;
        }
        if (file) {
            result += "Spectrum File" + SEPARATOR;
        }
        if (title) {
            result += "Spectrum Title" + SEPARATOR;
        }
        if (precursor) {
            result += "Prectursor m/z" + SEPARATOR;
            result += "Precursor charge" + SEPARATOR;
            result += "Precursor RT" + SEPARATOR;
            result += "Peptide theoretic mass" + SEPARATOR;
            result += "Mass error [ppm]" + SEPARATOR;
        }
        if (scores) {
            result += "Mascot e-value" + SEPARATOR;
            result += "OMSSA e-value" + SEPARATOR;
            result += "X!Tandem e-value" + SEPARATOR;
        }
        if (confidence) {
            result += "Confidence" + SEPARATOR;
        }
        result += "Retained as main PSM" + SEPARATOR;
        result += "Decoy" + SEPARATOR;
        result += "\n";

        PSParameter psParameter = new PSParameter();
        int rank, progress = 0;
        SpectrumMatch spectrumMatch;
        for (String spectrumKey : psmKeys) {
            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
            if (!onlyValidated || psParameter.isValidated()) {
                for (int se : spectrumMatch.getAdvocates()) {
                    ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(se).keySet());
                    Collections.sort(eValues);
                    rank = 1;
                    for (double eValue : eValues) {
                        for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                            result += AdvocateFactory.getInstance().getAdvocate(se).getName() + SEPARATOR;
                            result += rank + SEPARATOR;
                            if (accession) {
                                boolean first = true;
                                for (String protein : peptideAssumption.getPeptide().getParentProteins()) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        result += ", ";
                                    }
                                    result += protein;
                                }
                                result += SEPARATOR;
                            }
                            if (sequence) {
                                result += peptideAssumption.getPeptide().getSequence() + SEPARATOR;
                            }
                            if (modifications || locations) {
                                boolean first = true;
                                for (ModificationMatch modificationMatch : peptideAssumption.getPeptide().getModificationMatches()) {
                                    if (modificationMatch.isVariable()) {
                                        if (first) {
                                            first = false;
                                        } else {
                                            result += ", ";
                                        }
                                        String modName = modificationMatch.getTheoreticPtm().getName();
                                        result += modName + "(" + modificationMatch + ")";
                                    }
                                }
                                result += SEPARATOR;
                                result += "Not implemented" + SEPARATOR;
                            }
                            if (file) {
                                result += Spectrum.getSpectrumFile(spectrumMatch.getKey()) + SEPARATOR;
                            }
                            if (title) {
                                result += Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR;
                            }
                            if (precursor) {
                                Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                                result += prec.getMz() + SEPARATOR;
                                result += prec.getCharge() + SEPARATOR;
                                result += prec.getRt() + SEPARATOR;
                                result += peptideAssumption.getPeptide().getMass() + SEPARATOR;
                                result += peptideAssumption.getDeltaMass() + SEPARATOR;
                            }
                            if (scores) {
                                if (se == Advocate.MASCOT) {
                                    result += eValue;
                                }
                                result += SEPARATOR;
                                if (se == Advocate.OMSSA) {
                                    result += eValue;
                                }
                                result += SEPARATOR;
                                if (se == Advocate.XTANDEM) {
                                    result += eValue;
                                }
                                result += SEPARATOR;
                            }
                            if (confidence) {
                                psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                result += psParameter.getSearchEngineConfidence() + SEPARATOR;
                            }
                            if (peptideAssumption.getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                                result += 1 + SEPARATOR;
                            } else {
                                result += 0 + SEPARATOR;
                            }
                            if (peptideAssumption.isDecoy()) {
                                result += 1 + SEPARATOR;
                            } else {
                                result += 0 + SEPARATOR;
                            }
                            result += "\n";
                            rank++;
                        }
                    }
                }
            }
            progress++;
            if (progressDialog != null) {
                progressDialog.setValue(progress);
            }
        }
        return result;
    }
}
