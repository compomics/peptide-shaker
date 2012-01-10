/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import javax.swing.RowFilter.ComparisonType;

/**
 * This class provides information whether a hit should be hidden or starred
 *
 * @author Marc
 */
public class StarHider {
    
    /**
     * PeptideShakerGUI instance
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        
    /**
     * Constructor
     * @param peptideShakerGUI the peptideShakerGUI main class
     */
    public StarHider(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        
    }

    /**
     * Updates the star/hide status of all identification items
     */
    public void starHide() {
        final ProgressDialogX progressDialog = new ProgressDialogX(peptideShakerGUI, peptideShakerGUI, true);
        progressDialog.doNothingOnClose();
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Hiding/Starring Items. Please Wait...");
        
        new Thread("Star/Hide") {

            @Override
            public void run() {
        Identification identification = peptideShakerGUI.getIdentification();
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(identification.getProteinIdentification().size());
                ProteinMatch proteinMatch;
                PeptideMatch peptideMatch;
                boolean peptideSurvived, psmSurvived;
                PSParameter psParameter = new PSParameter();
                for (String proteinKey : identification.getProteinIdentification()) {
                    proteinMatch = identification.getProteinMatch(proteinKey);
                    peptideSurvived = false;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        psmSurvived = false;
                        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                            psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
                            if (isPsmHidden(spectrumKey)) {
                                psParameter.setHidden(true);
                            } else {
                                psParameter.setHidden(false);
                                psmSurvived = true;
                            }
                            psParameter.setStarred(isPsmStarred(spectrumKey));
                        }
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                        if (!psmSurvived) {
                            psParameter.setHidden(true);
                        } else if (isPeptideHidden(peptideKey)) {
                            psParameter.setHidden(true);
                        } else {
                            psParameter.setHidden(false);
                            peptideSurvived = true;
                        }
                        psParameter.setStarred(isPeptideStarred(peptideKey));
                    }
                    psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
                    if (!peptideSurvived) {
                        psParameter.setHidden(true);
                    } else {
                        psParameter.setHidden(isProteinHidden(proteinKey));
                    }
                    psParameter.setStarred(isProteinStarred(proteinKey));
                    progressDialog.incrementValue();
                }
                progressDialog.setVisible(false);
                progressDialog.dispose();
            }
        }.start();

        progressDialog.setVisible(true);
    }

    /**
     * Stars a protein match
     * @param match the key of the match
     */
    public void starProtein(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        boolean validated = false;
        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
            if (matchFilter.getExceptions().contains(match)) {
                matchFilter.removeException(match);
            }
            if (isValidated(match, matchFilter)) {
                validated = true;
            }
        }
        if (!validated) {
            ProteinFilter proteinFilter;
            if (!filterPreferences.getProteinStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                proteinFilter = new ProteinFilter(MatchFilter.MANUAL_SELECTION);
                proteinFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getProteinStarFilters().put(proteinFilter.getName(), proteinFilter);
            } else {
                proteinFilter = filterPreferences.getProteinStarFilters().get(MatchFilter.MANUAL_SELECTION);
            }
            proteinFilter.addManualValidation(match);
        }
        psParameter.setStarred(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unstars a protein match
     * @param match the key of the match
     */
    public void unStarProtein(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
            if (matchFilter.getManualValidation().contains(match)) {
                matchFilter.removeManualValidation(match);
            }
            if (isValidated(match, matchFilter)) {
                matchFilter.addException(match);
            }
        }
        psParameter.setStarred(false);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Hides a protein match
     * @param match the key of the match
     */
    public void hideProtein(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        boolean validated = false;
        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
            if (matchFilter.getExceptions().contains(match)) {
                matchFilter.removeException(match);
            }
            if (isValidated(match, matchFilter)) {
                validated = true;
            }
        }
        if (!validated) {
            ProteinFilter proteinFilter;
            if (!filterPreferences.getProteinHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                proteinFilter = new ProteinFilter(MatchFilter.MANUAL_SELECTION);
                proteinFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getProteinHideFilters().put(proteinFilter.getName(), proteinFilter);
            } else {
                proteinFilter = filterPreferences.getProteinHideFilters().get(MatchFilter.MANUAL_SELECTION);
            }
            proteinFilter.addManualValidation(match);
        }
        psParameter.setHidden(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unhides a protein match
     * @param match the key of the match
     */
    public void unHideProtein(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
            if (matchFilter.getManualValidation().contains(match)) {
                matchFilter.removeManualValidation(match);
            }
            if (isValidated(match, matchFilter)) {
                matchFilter.addException(match);
            }
        }
        psParameter.setHidden(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Stars a peptide match
     * @param match the key of the match
     */
    public void starPeptide(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        boolean validated = false;
        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
            if (matchFilter.getExceptions().contains(match)) {
                matchFilter.removeException(match);
            }
            if (isValidated(match, matchFilter)) {
                validated = true;
            }
        }
        if (!validated) {
            PeptideFilter peptideFilter;
            if (!filterPreferences.getPeptideStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                peptideFilter = new PeptideFilter(MatchFilter.MANUAL_SELECTION, peptideShakerGUI.getFoundModifications());
                peptideFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPeptideStarFilters().put(peptideFilter.getName(), peptideFilter);
            } else {
                peptideFilter = filterPreferences.getPeptideStarFilters().get(MatchFilter.MANUAL_SELECTION);
            }
            peptideFilter.addManualValidation(match);
        }
        psParameter.setStarred(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unstars a peptide match
     * @param match the key of the match
     */
    public void unStarPeptide(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
            if (matchFilter.getManualValidation().contains(match)) {
                matchFilter.removeManualValidation(match);
            }
            if (isValidated(match, matchFilter)) {
                matchFilter.addException(match);
            }
        }
        psParameter.setStarred(false);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Hides a peptide match
     * @param match the key of the match
     */
    public void hidePeptide(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        boolean validated = false;
        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
            if (matchFilter.getExceptions().contains(match)) {
                matchFilter.removeException(match);
            }
            if (isValidated(match, matchFilter)) {
                validated = true;
            }
        }
        if (!validated) {
            PeptideFilter peptideFilter;
            if (!filterPreferences.getPeptideHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                peptideFilter = new PeptideFilter(MatchFilter.MANUAL_SELECTION,  peptideShakerGUI.getFoundModifications());
                peptideFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPeptideHideFilters().put(peptideFilter.getName(), peptideFilter);
            } else {
                peptideFilter = filterPreferences.getPeptideHideFilters().get(MatchFilter.MANUAL_SELECTION);
            }
            peptideFilter.addManualValidation(match);
        }
        psParameter.setHidden(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unhides a peptide match
     * @param match the key of the match
     */
    public void unHidePeptide(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
            if (matchFilter.getManualValidation().contains(match)) {
                matchFilter.removeManualValidation(match);
            }
            if (isValidated(match, matchFilter)) {
                matchFilter.addException(match);
            }
        }
        psParameter.setHidden(false);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Stars a PSM match
     * @param match the key of the match
     */
    public void starPsm(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        boolean validated = false;
        if (!validated) {
            for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
                if (matchFilter.getExceptions().contains(match)) {
                    matchFilter.removeException(match);
                }
                if (isValidated(match, matchFilter)) {
                    validated = true;
                }
            }
            PsmFilter psmFilter;
            if (!filterPreferences.getPsmStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                psmFilter = new PsmFilter(MatchFilter.MANUAL_SELECTION, peptideShakerGUI.getSearchedCharges(), peptideShakerGUI.getSearchParameters().getSpectrumFiles());
                psmFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPsmStarFilters().put(psmFilter.getName(), psmFilter);
            } else {
                psmFilter = filterPreferences.getPsmStarFilters().get(MatchFilter.MANUAL_SELECTION);
            }
            psmFilter.addManualValidation(match);
        }
        psParameter.setStarred(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unstars a PSM match
     * @param match the key of the match
     */
    public void unStarPsm(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
            if (matchFilter.getManualValidation().contains(match)) {
                matchFilter.removeManualValidation(match);
            }
            if (isValidated(match, matchFilter)) {
                matchFilter.addException(match);
            }
        }
        psParameter.setStarred(false);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Hides a PSM match
     * @param match the key of the match
     */
    public void hidePsm(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        boolean validated = false;
        if (!validated) {
            for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
                if (matchFilter.getExceptions().contains(match)) {
                    matchFilter.removeException(match);
                }
                if (isValidated(match, matchFilter)) {
                    validated = true;
                }
            }
            PsmFilter psmFilter;
            if (!filterPreferences.getPsmHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                psmFilter = new PsmFilter(MatchFilter.MANUAL_SELECTION, peptideShakerGUI.getSearchedCharges(), peptideShakerGUI.getSearchParameters().getSpectrumFiles());
                psmFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPsmHideFilters().put(psmFilter.getName(), psmFilter);
            } else {
                psmFilter = filterPreferences.getPsmHideFilters().get(MatchFilter.MANUAL_SELECTION);
            }
            psmFilter.addManualValidation(match);
        }
        psParameter.setHidden(true);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unhides a psm match
     * @param match the key of the match
     */
    public void unHidePsm(String match) {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
            if (matchFilter.getManualValidation().contains(match)) {
                matchFilter.removeManualValidation(match);
            }
            if (isValidated(match, matchFilter)) {
                matchFilter.addException(match);
            }
        }
        psParameter.setHidden(false);
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Tests whether a protein match should be hidden according to the implemented filters
     * @param match the key of the match
     * @return a boolean indicating whether a protein match should be hidden according to the implemented filters
     */
    public boolean isProteinHidden(String match) {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
            if (matchFilter.isActive() && isValidated(match, matchFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a peptide match should be hidden according to the implemented filters
     * @param match the key of the match
     * @return a boolean indicating whether a protein match should be hidden according to the implemented filters
     */
    public boolean isPeptideHidden(String match) {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
            if (matchFilter.isActive() && isValidated(match, matchFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a psm match should be hidden according to the implemented filters
     * @param match the key of the match
     * @return a boolean indicating whether a protein match should be hidden according to the implemented filters
     */
    public boolean isPsmHidden(String match) {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
            if (matchFilter.isActive() && isValidated(match, matchFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a protein match should be starred according to the implemented filters
     * @param match the key of the match
     * @return a boolean indicating whether a protein match should be hidden according to the implemented filters
     */
    public boolean isProteinStarred(String match) {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
            if (matchFilter.isActive() && isValidated(match, matchFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a peptide match should be starred according to the implemented filters
     * @param match the key of the match
     * @return a boolean indicating whether a protein match should be hidden according to the implemented filters
     */
    public boolean isPeptideStarred(String match) {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
            if (matchFilter.isActive() && isValidated(match, matchFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a PSM match should be starred according to the implemented filters
     * @param match the key of the match
     * @return a boolean indicating whether a protein match should be hidden according to the implemented filters
     */
    public boolean isPsmStarred(String match) {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
            if (matchFilter.isActive() && isValidated(match, matchFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a protein match is validated by a given filter
     * @param match         the key of the protein match
     * @param proteinFilter the filter
     * @return a boolean indicating whether a protein match is validated by a given filter
     */
    public boolean isValidated(String match, ProteinFilter proteinFilter) {
        try {
            if (proteinFilter.getExceptions().contains(match)) {
                return false;
            }
            if (proteinFilter.getManualValidation().size() > 0) {
                if (proteinFilter.getManualValidation().contains(match)) {
                    return true;
                } else {
                    return false;
                }
            }
            if (proteinFilter.getIdentifierRegex() != null) {
                if (match.split(proteinFilter.getIdentifierRegex()).length == 1) {
                    boolean found = false;
                    for (String accession : ProteinMatch.getAccessions(match)) {
                        String test = "test" + sequenceFactory.getHeader(accession).getDescription() + "test";
                        if (test.split(proteinFilter.getIdentifierRegex()).length > 1) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }
            PSParameter psParameter = new PSParameter();
        Identification identification = peptideShakerGUI.getIdentification();
            psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
            if (proteinFilter.getPi() != 5) {
                if (proteinFilter.getPiComparison() == ComparisonType.NOT_EQUAL
                        && psParameter.getGroupClass() == proteinFilter.getPi()) {
                    return false;
                } else if (proteinFilter.getPiComparison() == ComparisonType.EQUAL
                        && psParameter.getGroupClass() != proteinFilter.getPi()) {
                    return false;
                }
            }
            if (proteinFilter.getProteinScore() != null) {
                if (proteinFilter.getProteinScoreComparison() == ComparisonType.AFTER) {
                    if (psParameter.getProteinScore() <= proteinFilter.getProteinScore()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinScoreComparison() == ComparisonType.BEFORE) {
                    if (psParameter.getProteinScore() >= proteinFilter.getProteinScore()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinScoreComparison() == ComparisonType.EQUAL) {
                    if (psParameter.getProteinScore() != proteinFilter.getProteinScore()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinScoreComparison() == ComparisonType.NOT_EQUAL) {
                    if (psParameter.getProteinScore() == proteinFilter.getProteinScore()) {
                        return false;
                    }
                }
            }
            if (proteinFilter.getProteinConfidence() != null) {
                if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.AFTER) {
                    if (psParameter.getProteinConfidence() <= proteinFilter.getProteinConfidence()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.BEFORE) {
                    if (psParameter.getProteinConfidence() >= proteinFilter.getProteinConfidence()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.EQUAL) {
                    if (psParameter.getProteinConfidence() != proteinFilter.getProteinConfidence()) {
                        return false;
                    }
                } else if (proteinFilter.getProteinConfidenceComparison() == ComparisonType.NOT_EQUAL) {
                    if (psParameter.getProteinConfidence() == proteinFilter.getProteinConfidence()) {
                        return false;
                    }
                }
            }
            if (proteinFilter.getnPeptides() != null
                    || proteinFilter.getProteinNSpectra() != null
                    || proteinFilter.getProteinCoverage() != null
                    || proteinFilter.getSpectrumCounting() != null) {
                ProteinMatch proteinMatch = identification.getProteinMatch(match);

                if (proteinFilter.getnPeptides() != null) {
                    if (proteinFilter.getnPeptidesComparison() == ComparisonType.AFTER) {
                        if (proteinMatch.getPeptideMatches().size() <= proteinFilter.getnPeptides()) {
                            return false;
                        }
                    } else if (proteinFilter.getnPeptidesComparison() == ComparisonType.BEFORE) {
                        if (proteinMatch.getPeptideMatches().size() >= proteinFilter.getnPeptides()) {
                            return false;
                        }
                    } else if (proteinFilter.getnPeptidesComparison() == ComparisonType.EQUAL) {
                        if (proteinMatch.getPeptideMatches().size() != proteinFilter.getnPeptides()) {
                            return false;
                        }
                    } else if (proteinFilter.getnPeptidesComparison() == ComparisonType.NOT_EQUAL) {
                        if (proteinMatch.getPeptideMatches().size() == proteinFilter.getnPeptides()) {
                            return false;
                        }
                    }
                }
                IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();
                if (proteinFilter.getProteinNSpectra() != null) {
                    if (proteinFilter.getnSpectraComparison() == ComparisonType.AFTER) {
                        if (identificationFeaturesGenerator.getNSpectra(match) <= proteinFilter.getProteinNSpectra()) {
                            return false;
                        }
                    } else if (proteinFilter.getnSpectraComparison() == ComparisonType.BEFORE) {
                        if (identificationFeaturesGenerator.getNSpectra(match) >= proteinFilter.getProteinNSpectra()) {
                            return false;
                        }
                    } else if (proteinFilter.getnSpectraComparison() == ComparisonType.EQUAL) {
                        if (identificationFeaturesGenerator.getNSpectra(match) != proteinFilter.getProteinNSpectra()) {
                            return false;
                        }
                    } else if (proteinFilter.getnSpectraComparison() == ComparisonType.NOT_EQUAL) {
                        if (identificationFeaturesGenerator.getNSpectra(match) == proteinFilter.getProteinNSpectra()) {
                            return false;
                        }
                    }
                }
                if (proteinFilter.getProteinCoverage() != null) {
                    double sequenceCoverage = 100 * identificationFeaturesGenerator.getSequenceCoverage(match);
                    if (proteinFilter.getProteinCoverageComparison() == ComparisonType.AFTER) {
                        if (sequenceCoverage <= proteinFilter.getProteinCoverage()) {
                            return false;
                        }
                    } else if (proteinFilter.getProteinCoverageComparison() == ComparisonType.BEFORE) {
                        if (sequenceCoverage >= proteinFilter.getProteinCoverage()) {
                            return false;
                        }
                    } else if (proteinFilter.getProteinCoverageComparison() == ComparisonType.EQUAL) {
                        if (sequenceCoverage != proteinFilter.getProteinCoverage()) {
                            return false;
                        }
                    } else if (proteinFilter.getProteinCoverageComparison() == ComparisonType.NOT_EQUAL) {
                        if (sequenceCoverage == proteinFilter.getProteinCoverage()) {
                            return false;
                        }
                    }
                }
                if (proteinFilter.getSpectrumCounting() != null) {
                    double spectrumCounting = identificationFeaturesGenerator.getSpectrumCounting(match);
                    if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.AFTER) {
                        if (spectrumCounting <= proteinFilter.getSpectrumCounting()) {
                            return false;
                        }
                    } else if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.BEFORE) {
                        if (spectrumCounting >= proteinFilter.getSpectrumCounting()) {
                            return false;
                        }
                    } else if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.EQUAL) {
                        if (spectrumCounting != proteinFilter.getSpectrumCounting()) {
                            return false;
                        }
                    } else if (proteinFilter.getSpectrumCountingComparison() == ComparisonType.NOT_EQUAL) {
                        if (spectrumCounting == proteinFilter.getSpectrumCounting()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            peptideShakerGUI.catchException(e);
            return false;
        }
    }

    /**
     * Tests whether a peptide match is validated by a given filter
     * @param match         the key of the peptide match
     * @param peptideFilter the filter
     * @return a boolean indicating whether a peptide match is validated by a given filter
     */
    public boolean isValidated(String match, PeptideFilter peptideFilter) {
        try {
            if (peptideFilter.getExceptions().contains(match)) {
                return false;
            }
            if (peptideFilter.getManualValidation().size() > 0) {
                if (peptideFilter.getManualValidation().contains(match)) {
                    return true;
                } else {
                    return false;
                }
            }
            PSParameter psParameter = new PSParameter();
            boolean found = false;
            for (String ptm : peptideFilter.getModificationStatus()) {
                if (ptm.equals(PtmPanel.NO_MODIFICATION)) {
                    if (!Peptide.isModified(match)) {
                        found = true;
                        break;
                    }
                } else {
                    if (Peptide.isModified(match, ptm)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        Identification identification = peptideShakerGUI.getIdentification();
            psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);
            if (peptideFilter.getPi() != 5) {
                if (peptideFilter.getPiComparison() == ComparisonType.NOT_EQUAL
                        && psParameter.getGroupClass() == peptideFilter.getPi()) {
                    return false;
                } else if (peptideFilter.getPiComparison() == ComparisonType.EQUAL
                        && psParameter.getGroupClass() != peptideFilter.getPi()) {
                    return false;
                }
            }
            if (peptideFilter.getPeptideScore() != null) {
                if (peptideFilter.getPeptideScoreComparison() == ComparisonType.AFTER) {
                    if (psParameter.getPeptideScore() <= peptideFilter.getPeptideScore()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideScoreComparison() == ComparisonType.BEFORE) {
                    if (psParameter.getPeptideScore() >= peptideFilter.getPeptideScore()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideScoreComparison() == ComparisonType.EQUAL) {
                    if (psParameter.getPeptideScore() != peptideFilter.getPeptideScore()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideScoreComparison() == ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPeptideScore() == peptideFilter.getPeptideScore()) {
                        return false;
                    }
                }
            }
            if (peptideFilter.getPeptideConfidence() != null) {
                if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.AFTER) {
                    if (psParameter.getPeptideConfidence() <= peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.BEFORE) {
                    if (psParameter.getPeptideConfidence() >= peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.EQUAL) {
                    if (psParameter.getPeptideConfidence() != peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                } else if (peptideFilter.getPeptideConfidenceComparison() == ComparisonType.NOT_EQUAL) {
                    if (psParameter.getPeptideConfidence() == peptideFilter.getPeptideConfidence()) {
                        return false;
                    }
                }
            }
            if (peptideFilter.getNSpectra() != null
                    || peptideFilter.getProtein() != null) {
                PeptideMatch peptideMatch = identification.getPeptideMatch(match);
                if (peptideFilter.getNSpectra() != null) {
                    if (peptideFilter.getnSpectraComparison() == ComparisonType.AFTER) {
                        if (peptideMatch.getSpectrumCount() <= peptideFilter.getNSpectra()) {
                            return false;
                        }
                    } else if (peptideFilter.getnSpectraComparison() == ComparisonType.BEFORE) {
                        if (peptideMatch.getSpectrumCount() >= peptideFilter.getNSpectra()) {
                            return false;
                        }
                    } else if (peptideFilter.getnSpectraComparison() == ComparisonType.EQUAL) {
                        if (peptideMatch.getSpectrumCount() != peptideFilter.getNSpectra()) {
                            return false;
                        }
                    } else if (peptideFilter.getnSpectraComparison() == ComparisonType.NOT_EQUAL) {
                        if (peptideMatch.getSpectrumCount() != peptideFilter.getNSpectra()) {
                            return false;
                        }
                    }
                }
                if (peptideFilter.getProtein() != null) {
                    found = false;
                    for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                        if (accession.split(peptideFilter.getProtein()).length > 1) {
                            found = true;
                            break;
                        }
                        if (sequenceFactory.getHeader(accession).getDescription() != null
                                && sequenceFactory.getHeader(accession).getDescription().split(peptideFilter.getProtein()).length > 1) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            peptideShakerGUI.catchException(e);
            return false;
        }
    }

    /**
     * Tests whether a psm match is validated by a given filter
     * @param match         the key of the psm match
     * @param psmFilter the filter
     * @return a boolean indicating whether a psm match is validated by a given filter
     */
    public boolean isValidated(String match, PsmFilter psmFilter) {
        try {
            if (psmFilter.getExceptions().contains(match)) {
                return false;
            }
            if (psmFilter.getManualValidation().size() > 0) {
                if (psmFilter.getManualValidation().contains(match)) {
                    return true;
                } else {
                    return false;
                }
            }
        Identification identification = peptideShakerGUI.getIdentification();
            PSParameter psParameter = new PSParameter();
            if (psmFilter.getPsmScore() != null
                    || psmFilter.getPsmConfidence() != null) {
                psParameter = (PSParameter) identification.getMatchParameter(match, psParameter);

                if (psmFilter.getPsmScore() != null) {
                    if (psmFilter.getPsmScoreComparison() == ComparisonType.AFTER) {
                        if (psParameter.getPsmScore() <= psmFilter.getPsmScore()) {
                            return false;
                        }
                    } else if (psmFilter.getPsmScoreComparison() == ComparisonType.BEFORE) {
                        if (psParameter.getPsmScore() >= psmFilter.getPsmScore()) {
                            return false;
                        }
                    } else if (psmFilter.getPsmScoreComparison() == ComparisonType.EQUAL) {
                        if (psParameter.getPsmScore() != psmFilter.getPsmScore()) {
                            return false;
                        }
                    } else if (psmFilter.getPsmScoreComparison() == ComparisonType.NOT_EQUAL) {
                        if (psParameter.getPsmScore() == psmFilter.getPsmScore()) {
                            return false;
                        }
                    }
                }
                if (psmFilter.getPsmConfidence() != null) {
                    if (psmFilter.getPsmConfidenceComparison() == ComparisonType.AFTER) {
                        if (psParameter.getPsmConfidence() <= psmFilter.getPsmConfidence()) {
                            return false;
                        }
                    } else if (psmFilter.getPsmConfidenceComparison() == ComparisonType.BEFORE) {
                        if (psParameter.getPsmConfidence() >= psmFilter.getPsmConfidence()) {
                            return false;
                        }
                    } else if (psmFilter.getPsmConfidenceComparison() == ComparisonType.EQUAL) {
                        if (psParameter.getPsmConfidence() != psmFilter.getPsmConfidence()) {
                            return false;
                        }
                    } else if (psmFilter.getPsmConfidenceComparison() == ComparisonType.NOT_EQUAL) {
                        if (psParameter.getPsmConfidence() == psmFilter.getPsmConfidence()) {
                            return false;
                        }
                    }
                }
            }
            if (psmFilter.getPrecursorMz() != null
                    || psmFilter.getPrecursorRT() != null
                    || psmFilter.getPrecursorMzError() != null) {
                Precursor precursor = peptideShakerGUI.getPrecursor(match);
                if (psmFilter.getPrecursorMz() != null) {
                    if (psmFilter.getPrecursorMzComparison() == ComparisonType.AFTER) {
                        if (precursor.getMz() <= psmFilter.getPrecursorMz()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorMzComparison() == ComparisonType.BEFORE) {
                        if (precursor.getMz() >= psmFilter.getPrecursorMz()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorMzComparison() == ComparisonType.EQUAL) {
                        if (precursor.getMz() != psmFilter.getPrecursorMz()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorMzComparison() == ComparisonType.NOT_EQUAL) {
                        if (precursor.getMz() == psmFilter.getPrecursorMz()) {
                            return false;
                        }
                    }
                }
                if (psmFilter.getPrecursorRT() != null) {
                    if (psmFilter.getPrecursorRTComparison() == ComparisonType.AFTER) {
                        if (precursor.getRt() <= psmFilter.getPrecursorRT()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorRTComparison() == ComparisonType.BEFORE) {
                        if (precursor.getRt() >= psmFilter.getPrecursorRT()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorRTComparison() == ComparisonType.EQUAL) {
                        if (precursor.getRt() != psmFilter.getPrecursorRT()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorRTComparison() == ComparisonType.NOT_EQUAL) {
                        if (precursor.getRt() == psmFilter.getPrecursorRT()) {
                            return false;
                        }
                    }
                }
                if (psmFilter.getPrecursorMzError() != null) {
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(match);
                    double error = Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()));
                    if (psmFilter.getPrecursorMzErrorComparison() == ComparisonType.AFTER) {
                        if (error <= psmFilter.getPrecursorMzError()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorMzErrorComparison() == ComparisonType.BEFORE) {
                        if (error >= psmFilter.getPrecursorMzError()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorMzErrorComparison() == ComparisonType.EQUAL) {
                        if (error != psmFilter.getPrecursorMzError()) {
                            return false;
                        }
                    } else if (psmFilter.getPrecursorMzErrorComparison() == ComparisonType.NOT_EQUAL) {
                        if (error == psmFilter.getPrecursorMzError()) {
                            return false;
                        }
                    }
                }
            }
            if (psmFilter.getCharges().size() != peptideShakerGUI.getSearchedCharges().size()) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(match);
                int charge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                if (!psmFilter.getCharges().contains(charge)) {
                    return false;
                }
            }
            if (!psmFilter.getFileNames().contains(Spectrum.getSpectrumFile(match))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            peptideShakerGUI.catchException(e);
            return false;
        }
    }
    
}
