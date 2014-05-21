package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.FastaIndex;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class generates the text needed to for the identification section of the
 * methods section of a paper.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class PeptideShakerMethods {

    /**
     * Returns the search engines usage details.
     *
     * @param projectDetails the PeptideShaker project details
     *
     * @return the search engines usage details
     */
    public static String getSearchEnginesText(ProjectDetails projectDetails) {
        String text = "Peak lists obtained from MS/MS spectra were identified using ";

        try {
            ArrayList<Integer> searchEngines = projectDetails.getIdentificationAlgorithms();
            Collections.sort(searchEngines);
            HashMap<String, ArrayList<String>> algorithmToVersionMap = projectDetails.getAlgorithmNameToVersionsMap();

            for (int i = 0; i < searchEngines.size(); i++) {
                if (i > 0) {
                    if (i == searchEngines.size() - 1) {
                        text += " and ";
                    } else {
                        text += ", ";
                    }
                }
                Advocate advocate = Advocate.getAdvocate(searchEngines.get(i));
                String ref = advocate.getPmid();
                if (ref == null) {
                    ref = "add reference here";
                }
                text += advocate.getName() + " [ref PMID " + ref + "] ";

                ArrayList<String> versions = algorithmToVersionMap.get(advocate.getName());

                if (versions == null || versions.isEmpty()) {
                    text += "[add version here]";
                } else if (versions.size() == 1) {
                    text += "version " + versions.get(0);
                } else {
                    text += "versions ";
                    Collections.sort(versions);
                    for (int j = 0; j < versions.size(); j++) {
                        if (j > 0) {
                            if (j == versions.size() - 1) {
                                text += " and ";
                            } else {
                                text += ", ";
                            }
                        }
                        text += versions.get(j);
                    }
                }
                text += ".";
            }
        } catch (Exception e) {
            // A backward compatibility issue occurred
            text += "[add the search eninges used here].";
        }
        return text;
    }

    /**
     * Returns the SearchGUI usage details.
     *
     * @return the SearchGUI usage details
     */
    public static String getSearchGUIText() {
        String text = "The search was conducted using SearchGUI [ref PMID  21337703] version [add version here].";
        return text;
    }

    /**
     * Returns the database usage details.
     *
     * @return the database usage details
     */
    public static String getDatabaseText() {
        String text = "Protein identification was conducted against a concatenated target/decoy [ref PMID 20013364] version of the ";

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        FastaIndex fastaIndex = sequenceFactory.getCurrentFastaIndex();

        ArrayList<String> species = fastaIndex.getSpecies();
        if (species == null || species.isEmpty()) {
            text += "[add species here]";
        } else {
            for (int i = 0; i < species.size(); i++) {
                if (i > 0) {
                    if (i == species.size() - 1) {
                        text += " and ";
                    } else {
                        text += ", ";
                    }
                }
                text += species;
            }
        }
        text += " complement of the ";

        String dbType = fastaIndex.getDatabaseType().getFullName();
        if (dbType == null) {
            dbType = "[add database full name here]";
        }

        String dbRef = fastaIndex.getDatabaseType().getPmid();
        if (dbRef == null) {
            dbRef = "add reference here";
        } else {
            dbRef = "ref PMID " + dbRef;
        }

        text += dbType + " [" + dbRef + " ] (version of [add version here] , " + fastaIndex.getNTarget() + " (target) sequences).";

        return text;
    }

    /**
     * Returns the decoy sequences creation details.
     *
     * @return the decoy sequences creation details
     */
    public static String getDecoyType() {
        String text = "The decoy sequences were created by reversing the target sequences in SearchGUI. ";
        return text;
    }

    /**
     * Returns the identification settings details.
     *
     * @param searchParameters the search parameters
     *
     * @return the identification settings details
     */
    public static String getIdentificationSettings(SearchParameters searchParameters) {
        String text = "Identification settings were as follows:";
        text += searchParameters.getEnzyme().getName() + " with a maximum of " + searchParameters.getnMissedCleavages() + " missed cleavages; ";
        String msToleranceUnit;
        if (searchParameters.isPrecursorAccuracyTypePpm()) {
            msToleranceUnit = "ppm";
        } else {
            msToleranceUnit = "Da";
        }
        String msmsToleranceUnit = "Da";
        text += searchParameters.getPrecursorAccuracy() + " " + msToleranceUnit + " as MS and " + searchParameters.getFragmentIonAccuracy() + " " + msmsToleranceUnit + " as MS/MS tolerances; ";
        PTMFactory ptmFactory = PTMFactory.getInstance();
        ArrayList<String> fixedPtmsNames = searchParameters.getModificationProfile().getFixedModifications();
        if (!fixedPtmsNames.isEmpty()) {
            text += "fixed modifications: ";
            for (int i = 0; i < fixedPtmsNames.size(); i++) {
                if (i > 0) {
                    if (i == fixedPtmsNames.size() - 1) {
                        text += " and ";
                    } else {
                        text += ", ";
                    }
                }
                String ptmName = fixedPtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text += ptmName + " (" + sign + ptm.getMass() + " Da)";
            }
        }
        ArrayList<String> variablePtmsNames = searchParameters.getModificationProfile().getVariableModifications();
        if (!variablePtmsNames.isEmpty()) {
            text += "variable modifications: ";
            for (int i = 0; i < variablePtmsNames.size(); i++) {
                if (i > 0) {
                    if (i == variablePtmsNames.size() - 1) {
                        text += " and ";
                    } else {
                        text += ", ";
                    }
                }
                String ptmName = variablePtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text += ptmName + " (" + sign + ptm.getMass() + " Da)";
            }
        }
        ArrayList<String> refinementFixedPtmsNames = searchParameters.getModificationProfile().getRefinementFixedModifications();
        if (!refinementFixedPtmsNames.isEmpty()) {
            text += "refinement search fixed modifications: ";
            for (int i = 0; i < refinementFixedPtmsNames.size(); i++) {
                if (i > 0) {
                    if (i == refinementFixedPtmsNames.size() - 1) {
                        text += " and ";
                    } else {
                        text += ", ";
                    }
                }
                String ptmName = refinementFixedPtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text += ptmName + " (" + sign + ptm.getMass() + " Da)";
            }
        }
        ArrayList<String> refinementVariablePtmsNames = searchParameters.getModificationProfile().getRefinementVariableModifications();
        if (!refinementVariablePtmsNames.isEmpty()) {
            text += "refinement search variable modifications: ";
            for (int i = 0; i < refinementVariablePtmsNames.size(); i++) {
                if (i > 0) {
                    if (i == refinementVariablePtmsNames.size() - 1) {
                        text += " and ";
                    } else {
                        text += ", ";
                    }
                }
                String ptmName = refinementVariablePtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text += ptmName + " (" + sign + ptm.getMass() + " Da)";
            }
        }
        text += ". All algorithms specific settings are listed in the Certificate of Analysis available as supplementary information.";
        return text;
    }

    /**
     * Returns the PeptideShaker usage details.
     *
     * @return the PeptideShaker usage details
     */
    public static String getPeptideShaker() {
        String text = "Peptides and proteins were infered from the spectrum identification results using PeptideShaker version [add PeptideShaker version here] (http://peptide-shaker.googlecode.com).";
        return text;
    }

    /**
     * Returns the validation thresholds used.
     *
     * @param processingPreferences the processing preferences
     *
     * @return the validation thresholds used
     */
    public static String getValidation(ProcessingPreferences processingPreferences) {
        double psmFDR = processingPreferences.getPsmFDR();
        double peptideFDR = processingPreferences.getPeptideFDR();
        double proteinFDR = processingPreferences.getProteinFDR();
        boolean sameThreshold = psmFDR == peptideFDR && peptideFDR == proteinFDR;
        String text;
        if (sameThreshold) {
            text = "Peptide Spectrum Matches (PSMs), peptides and proteins were validated at a " + psmFDR + "% False Discovery Rate (FDR) estimated using the decoy hit distribution. ";
        } else {
            text = "Peptide Spectrum Matches (PSMs), peptides and proteins were validated at a " + psmFDR + "%, " + peptideFDR + "%, and " + proteinFDR + "% False Discovery Rate (FDR) estimated using the decoy hit distribution, respectively. ";
        }
        text += "All validation thresholds are listed in the Certificate of Analysis available as supplementary information.";
        return text;
    }

    /**
     * Returns the PTM scoring methods used.
     *
     * @param ptmScoringPreferences the PTM localization scoring preferences
     *
     * @return the PTM scoring methods used
     */
    public static String getPtmScoring(PTMScoringPreferences ptmScoringPreferences) {
        String text = "Post-translational Modification (PTM) localization was scored using the D-score [ref PMID  21337703] ";
        if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
            if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                text += "and the A-score [ref PMID  16964243] ";
            } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                text += "and the phosphoRS score [ref PMID  22073976] ";
            } else {
                throw new IllegalArgumentException("Export not implemented for score " + ptmScoringPreferences.getSelectedProbabilisticScore().getName() + ".");
            }
            if (!ptmScoringPreferences.isEstimateFlr()) {
                text += "with a threshold of " + ptmScoringPreferences.getProbabilisticScoreThreshold() + " ";
            }
        }
        text += "as implemented in the compomics utilities package [ref PMID 21385435].";
        if (ptmScoringPreferences.isProbabilitsticScoreCalculation() && !ptmScoringPreferences.isEstimateFlr()) {
            if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                text += " An A-score above ";
            } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                text += "A phosphoRS score above ";
            } else {
                throw new IllegalArgumentException("Export not implemented for score " + ptmScoringPreferences.getSelectedProbabilisticScore().getName() + ".");
            }
            text += "was considered as a confident localization.";
        }
        return text;
    }

    /**
     * Returns the gene annotation method usage details.
     *
     * @return the gene annotation method usage details
     */
    public static String getGeneAnnoration() {
        String text = "TODO!";
        return text;
    }

    /**
     * Returns the spectrum counting method usage details.
     *
     * @param spectrumCountingPreferences the spectrum counting preferences used
     *
     * @return the spectrum counting method usage details
     */
    public static String getSpectrumCounting(SpectrumCountingPreferences spectrumCountingPreferences) {
        String text = "Spectrum counting abundance indexes were estimated using the ";
        if (spectrumCountingPreferences.getSelectedMethod() == SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) {
            text += "emPAI index [ref PMID 15958392].";
        } else {
            text += "Normalized Spectrum Abundance Factor [ref PMID 15282323] adapted for better handling of protein inference issues and peptide detectability.";
        }
        return text;
    }

    /**
     * Returns the ProteomeXchange upload details.
     *
     * @return the ProteomeXchange upload details
     */
    public static String getProteomeXchage() {
        String text = "The mass spectrometry data along with the identification results have been deposited to the ProteomeXchange Consortium [ref PMID 24727771] via the PRIDE partner repository [ref PMID 16041671] with the dataset identifiers [add dataset identifiers here]. Note that during the review process, the data can be accessed with the following credentials upon login to the pride website (http://www.ebi.ac.uk/pride/archive/login):\n"
                + "- Username: [add username here]\n"
                + "- Password: [add password here]\n";
        return text;
    }
}
