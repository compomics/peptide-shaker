package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.protein_sequences.FastaIndex;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.PeptideShaker;
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
            text += advocate.getName() + " ";

            ArrayList<String> versions = algorithmToVersionMap.get(advocate.getName());

            if (versions == null || versions.isEmpty()) {
                text += "version [add version here]";
            } else if (versions.size() == 1) {
                if (versions.get(0) != null) {
                    text += "version " + versions.get(0);
                } else {
                    text += "version unknown";
                }
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
                    if (versions.get(0) != null) {
                        text += versions.get(j);
                    } else {
                        text += "unknown";
                    }
                }
            }

            text += " [PMID " + ref + "]";
        }

        text += ".";
        return text;
    }

    /**
     * Returns the SearchGUI usage details.
     *
     * @return the SearchGUI usage details
     */
    public static String getSearchGUIText() {
        return "The search was conducted using SearchGUI version [add version] [PMID  21337703].";
    }

    /**
     * Returns the database usage details.
     *
     * @return the database usage details
     */
    public static String getDatabaseText() {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();

        String text = "";

        if (sequenceFactory.concatenatedTargetDecoy()) {
            text += "Protein identification was conducted against a concatenated target/decoy [PMID 20013364] version of the ";
        } else {
            text += "Protein identification was conducted against a version of the ";
        }

        FastaIndex fastaIndex = sequenceFactory.getCurrentFastaIndex();

        HashMap<String, Integer> species = fastaIndex.getSpecies();
        if (species == null || species.isEmpty()) {
            text += "[add species]";
        } else {
            text += SpeciesFactory.getSpeciesDescription(species);
        }
        text += " complement of the ";

        String dbType = fastaIndex.getMainDatabaseType().getFullName();
        if (dbType == null) {
            dbType = "[add database full name]";
        }

        String dbRef = fastaIndex.getMainDatabaseType().getPmid();
        if (dbRef == null) {
            dbRef = "add reference";
        } else {
            dbRef = "PMID " + dbRef;
        }

        text += dbType + " [" + dbRef + "] (version of [add database version] , " + fastaIndex.getNTarget() + " (target) sequences).";

        return text;
    }

    /**
     * Returns the decoy sequences creation details.
     *
     * @return the decoy sequences creation details
     */
    public static String getDecoyType() {
        return "The decoy sequences were created by reversing the target sequences in SearchGUI. ";
    }

    /**
     * Returns the identification settings details.
     *
     * @param searchParameters the search parameters
     *
     * @return the identification settings details
     */
    public static String getIdentificationSettings(SearchParameters searchParameters) {
        StringBuilder text = new StringBuilder("The identification settings were as follows: ");
        DigestionPreferences digestionPreferences = searchParameters.getDigestionPreferences();
        switch (digestionPreferences.getCleavagePreference()) {
            case unSpecific:
                text.append("No cleavage specificity; ");
                break;
            case wholeProtein:
                text.append("No digestion; ");
                break;
            case enzyme:
                for (int i = 0; i < digestionPreferences.getEnzymes().size(); i++) {
                    Enzyme enzyme = digestionPreferences.getEnzymes().get(i);
                    String enzymeName = enzyme.getName();
                    if (i > 0) {
                        if (digestionPreferences.getEnzymes().size() == 2) {
                            text.append(" and ");
                        } else {
                            text.append(", ");
                            if (i == digestionPreferences.getEnzymes().size() - 1) {
                                text.append("and ");
                            }
                        }
                    }
                    text.append(enzymeName).append(", ").append(digestionPreferences.getSpecificity(enzymeName)).append(", with a maximum of ").append(digestionPreferences.getnMissedCleavages(enzymeName)).append(" missed cleavages");
                }
                break;
            default:
                throw new UnsupportedOperationException("Cleavage of type " + digestionPreferences.getCleavagePreference() + " not supported.");
        }
        text.append(searchParameters.getPrecursorAccuracy()).append(" ").append(searchParameters.getPrecursorAccuracyType()).append(" as MS1 and ").append(searchParameters.getFragmentIonAccuracy()).append(" ").append(searchParameters.getFragmentAccuracyType()).append(" as MS2 tolerances; ");
        PTMFactory ptmFactory = PTMFactory.getInstance();
        ArrayList<String> fixedPtmsNames = searchParameters.getPtmSettings().getFixedModifications();
        if (!fixedPtmsNames.isEmpty()) {
            text.append("fixed modifications: ");
            for (int i = 0; i < fixedPtmsNames.size(); i++) {
                if (i > 0) {
                    if (fixedPtmsNames.size() == 2) {
                        text.append(" and ");
                    } else {
                        text.append(", ");
                        if (i == fixedPtmsNames.size() - 1) {
                            text.append("and ");
                        }
                    }
                }
                String ptmName = fixedPtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getRoundedMass() < 0) {
                    sign = "";
                } else {
                    sign = "+";
                }
                text.append(ptmName).append(" (").append(sign).append(ptm.getRoundedMass()).append(" Da)");
            }

            text.append(", ");
        }
        ArrayList<String> variablePtmsNames = searchParameters.getPtmSettings().getVariableModifications();
        if (!variablePtmsNames.isEmpty()) {
            text.append(" variable modifications: ");
            for (int i = 0; i < variablePtmsNames.size(); i++) {
                if (i > 0) {
                    if (fixedPtmsNames.size() == 2) {
                        text.append(" and ");
                    } else {
                        text.append(", ");
                        if (i == fixedPtmsNames.size() - 1) {
                            text.append("and ");
                        }
                    }
                }
                String ptmName = variablePtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getRoundedMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text.append(ptmName).append(" (").append(sign).append(ptm.getRoundedMass()).append(" Da)");
            }

            text.append(", ");
        }
        ArrayList<String> refinementFixedPtmsNames = searchParameters.getPtmSettings().getRefinementFixedModifications();
        if (!refinementFixedPtmsNames.isEmpty()) {
            text.append("fixed modifications during refinement procedure: ");
            for (int i = 0; i < refinementFixedPtmsNames.size(); i++) {
                if (i > 0) {
                    if (fixedPtmsNames.size() == 2) {
                        text.append(" and ");
                    } else {
                        text.append(", ");
                        if (i == fixedPtmsNames.size() - 1) {
                            text.append("and ");
                        }
                    }
                }
                String ptmName = refinementFixedPtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getRoundedMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text.append(ptmName).append(" (").append(sign).append(ptm.getRoundedMass()).append(" Da)");
            }

            text.append(", ");
        }
        ArrayList<String> refinementVariablePtmsNames = searchParameters.getPtmSettings().getRefinementVariableModifications();
        if (!refinementVariablePtmsNames.isEmpty()) {
            text.append("variable modifications during refinement procedure: ");
            for (int i = 0; i < refinementVariablePtmsNames.size(); i++) {
                if (i > 0) {
                    if (fixedPtmsNames.size() == 2) {
                        text.append(" and ");
                    } else {
                        text.append(", ");
                        if (i == fixedPtmsNames.size() - 1) {
                            text.append("and ");
                        }
                    }
                }
                String ptmName = refinementVariablePtmsNames.get(i);
                PTM ptm = ptmFactory.getPTM(ptmName);
                String sign;
                if (ptm.getRoundedMass() < 0) {
                    sign = "-";
                } else {
                    sign = "+";
                }
                text.append(ptmName).append(" (").append(sign).append(ptm.getRoundedMass()).append(" Da)");
            }
        }
        text.append(". All algorithms specific settings are listed in the Certificate of Analysis available in the supplementary information.");
        return text.toString();
    }

    /**
     * Returns the PeptideShaker usage details.
     *
     * @return the PeptideShaker usage details
     */
    public static String getPeptideShaker() {
        return "Peptides and proteins were inferred from the spectrum identification results using PeptideShaker "
                + "version " + PeptideShaker.getVersion() + " [PMID 25574629]. ";
    }

    /**
     * Returns the validation thresholds used.
     *
     * @param idMatchValidationPreferences the match validation preferences
     *
     * @return the validation thresholds used
     */
    public static String getValidation(IdMatchValidationPreferences idMatchValidationPreferences) {
        double psmFDR = idMatchValidationPreferences.getDefaultPsmFDR();
        double peptideFDR = idMatchValidationPreferences.getDefaultPeptideFDR();
        double proteinFDR = idMatchValidationPreferences.getDefaultProteinFDR();
        boolean sameThreshold = psmFDR == peptideFDR && peptideFDR == proteinFDR;
        String text;
        if (sameThreshold) {
            text = "Peptide Spectrum Matches (PSMs), peptides and proteins were validated at a " + psmFDR + "% False Discovery Rate (FDR) estimated using the decoy hit distribution. ";
        } else {
            text = "Peptide Spectrum Matches (PSMs), peptides and proteins were validated at a " + psmFDR + "%, " + peptideFDR + "%, and " + proteinFDR + "% False Discovery Rate (FDR) estimated using the decoy hit distribution, respectively. ";
        }
        text += "All validation thresholds are listed in the Certificate of Analysis available in the supplementary information. ";
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
        String text = "Post-translational modification localizations were scored using the D-score [PMID 23307401] ";
        if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
            if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                text += "and the A-score [PMID 16964243] ";
            } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                text += "and the phosphoRS score [PMID 22073976] ";
            } else {
                throw new IllegalArgumentException("Export not implemented for score " + ptmScoringPreferences.getSelectedProbabilisticScore().getName() + ".");
            }
            if (!ptmScoringPreferences.isEstimateFlr()) {
                text += "with a threshold of " + ptmScoringPreferences.getProbabilisticScoreThreshold() + " ";
            }
        }
        text += "as implemented in the compomics-utilities package [PMID 21385435].";
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
        return text + " ";
    }

    /**
     * Returns the gene annotation method usage details.
     *
     * @return the gene annotation method usage details
     */
    public static String getGeneAnnotation() {
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
            text += "emPAI index [PMID 15958392].";
        } else {
            text += "Normalized Spectrum Abundance Factor [PMID 15282323] adapted for better handling of protein inference issues and peptide detectability.";
        }
        return text;
    }

    /**
     * Returns the ProteomeXchange upload details.
     *
     * @return the ProteomeXchange upload details
     */
    public static String getProteomeXchange() {
        String text = "The mass spectrometry data along with the identification results have been deposited to the "
                + "ProteomeXchange Consortium [PMID 24727771] via the PRIDE partner repository [PMID 16041671] "
                + "with the dataset identifiers [add dataset identifiers]. During the review process, "
                + "the data can be accessed with the following credentials upon login to the PRIDE website (http://www.ebi.ac.uk/pride/archive/login): "
                + "Username: [add username here], Password: [add password here].";
        return text;
    }
}
