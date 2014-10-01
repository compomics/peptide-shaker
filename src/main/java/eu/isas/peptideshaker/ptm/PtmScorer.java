package eu.isas.peptideshaker.ptm;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.identification.ptm.ptmscores.AScore;
import com.compomics.util.experiment.identification.ptm.ptmscores.PhosphoRS;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PsmPTMMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class scores the PSM PTMs using the scores implemented in compomics
 * utilities.
 *
 * @author Marc Vaudel
 */
public class PtmScorer {

    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The protein sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The PSM PTM localization conflict map.
     */
    private PsmPTMMap psmPTMMap;

    /**
     * Constructor.
     *
     * @param psmPTMMap the PSM PTM score map
     */
    public PtmScorer(PsmPTMMap psmPTMMap) {
        this.psmPTMMap = psmPTMMap;
    }

    /**
     * Scores the PTM locations using the delta score.
     *
     * @param identification identification object containing the identification
     * matches
     * @param spectrumMatch the spectrum match of interest
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * reading/writing the an identification match
     */
    public void attachDeltaScore(Identification identification, SpectrumMatch spectrumMatch, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        HashMap<String, ArrayList<Integer>> modificationProfiles = new HashMap<String, ArrayList<Integer>>();
        PSPtmScores ptmScores = new PSPtmScores();

        if (spectrumMatch.getUrParam(new PSPtmScores()) != null) {
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
        }

        PSParameter psParameter = new PSParameter();
        double p1 = 1;
        Peptide psPeptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption instanceof PeptideAssumption) {
                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                Peptide sePeptide = peptideAssumption.getPeptide();
                if (psPeptide.isSameSequence(sePeptide, sequenceMatchingPreferences) && psPeptide.sameModificationsAs(sePeptide)) {
                    psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                    double ptemp = psParameter.getSearchEngineProbability();
                    if (ptemp < p1) {
                        p1 = ptemp;
                    }
                }
            }
        }

        String mainSequence = psPeptide.getSequence();
        ArrayList<String> modifications = new ArrayList<String>();

        for (ModificationMatch modificationMatch : psPeptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                String modificationName = modificationMatch.getTheoreticPtm();
                if (!modifications.contains(modificationName)) {
                    modifications.add(modificationName);
                    modificationProfiles.put(modificationName, new ArrayList<Integer>());
                }
                modificationProfiles.get(modificationName).add(modificationMatch.getModificationSite());
            }
        }

        if (!modifications.isEmpty()) {

            for (String modName : modifications) {

                PTM ptm1 = ptmFactory.getPTM(modName);

                for (int modSite : modificationProfiles.get(modName)) {

                    double refP = 1, secondaryP = 1;

                    for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions()) {

                        if (assumption instanceof PeptideAssumption) {

                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;

                            if (peptideAssumption.getPeptide().getSequence().equals(mainSequence)) {

                                boolean modificationAtSite = false, modificationFound = false;

                                for (ModificationMatch modMatch : peptideAssumption.getPeptide().getModificationMatches()) {

                                    PTM ptm2 = ptmFactory.getPTM(modMatch.getTheoreticPtm());

                                    if (ptm1.getMass() == ptm2.getMass()) {

                                        modificationFound = true;
                                        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                        double p = psParameter.getSearchEngineProbability();

                                        if (modMatch.getModificationSite() == modSite) {

                                            modificationAtSite = true;

                                            if (p < refP) {
                                                refP = p;
                                            }
                                        }
                                    }
                                }

                                if (!modificationAtSite && modificationFound) {
                                    psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                    double p = psParameter.getSearchEngineProbability();
                                    if (p < secondaryP) {
                                        secondaryP = p;
                                    }
                                }
                            }
                        }
                    }

                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    if (ptmScoring == null) {
                        ptmScoring = new PtmScoring(modName);
                        ptmScores.addPtmScoring(modName, ptmScoring);
                    }

                    if (secondaryP < refP) {
                        secondaryP = refP;
                    }

                    double deltaScore = (secondaryP - refP) * 100;
                    ptmScoring.setDeltaScore(modSite, deltaScore);
                }

                spectrumMatch.addUrParam(ptmScores);
                identification.updateSpectrumMatch(spectrumMatch);
            }
        }
    }

    /**
     * Attaches the selected probabilistic PTM score.
     *
     * @param identification identification object containing the identification
     * matches
     * @param spectrumMatch the spectrum match studied, the A-score will be
     * calculated for the best assumption
     * @param searchParameters the identification parameters
     * @param annotationPreferences the annotation preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * computing the score
     */
    private void attachProbabilisticScore(Identification identification, SpectrumMatch spectrumMatch, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, PTMScoringPreferences scoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        ModificationProfile ptmProfile = searchParameters.getModificationProfile();

        PSPtmScores ptmScores = new PSPtmScores();
        if (spectrumMatch.getUrParam(new PSPtmScores()) != null) {
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
        }

        HashMap<Double, ArrayList<PTM>> modifications = new HashMap<Double, ArrayList<PTM>>();
        HashMap<Double, Integer> nMod = new HashMap<Double, Integer>();
        HashMap<Double, ModificationMatch> modificationMatches = new HashMap<Double, ModificationMatch>();
        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                PTM refPTM = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                double ptmMass = refPTM.getMass();
                if (!modifications.containsKey(ptmMass)) {
                    ArrayList<PTM> ptms = new ArrayList<PTM>();
                    for (String ptm : ptmProfile.getSimilarNotFixedModifications(ptmMass)) {
                        ptms.add(ptmFactory.getPTM(ptm));
                    }
                    modifications.put(ptmMass, ptms);
                    nMod.put(ptmMass, 1);
                } else {
                    nMod.put(ptmMass, nMod.get(ptmMass) + 1);
                }
                modificationMatches.put(ptmMass, modificationMatch);
            }
        }

        if (!modifications.isEmpty()) {

            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey());
            annotationPreferences.setCurrentSettings(spectrumMatch.getBestPeptideAssumption(), true, sequenceMatchingPreferences);

            for (Double ptmMass : modifications.keySet()) {
                HashMap<Integer, Double> scores = null;
                if (scoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore && nMod.get(ptmMass) == 1) {
                    scores = AScore.getAScore(peptide, modifications.get(ptmMass), spectrum, annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                            spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
                            searchParameters.getFragmentIonAccuracy(), scoringPreferences.isProbabilisticScoreNeutralLosses(), sequenceMatchingPreferences);
                } else if (scoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                    scores = PhosphoRS.getSequenceProbabilities(peptide, modifications.get(ptmMass), spectrum, annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                            spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
                            searchParameters.getFragmentIonAccuracy(), scoringPreferences.isProbabilisticScoreNeutralLosses(), sequenceMatchingPreferences);
                }
                if (scores != null) {
                    // remap to searched PTMs
                    PTM mappedModification = null;
                    String peptideSequence = peptide.getSequence();
                    for (int site : scores.keySet()) {
                        if (site == 0) {
                            // N-term ptm
                            for (PTM ptm : modifications.get(ptmMass)) {
                                if (ptm.isNTerm() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(1)) {
                                    mappedModification = ptm;
                                    break;
                                }
                            }
                            if (mappedModification == null) {
                                throw new IllegalArgumentException("Could not map the PTM of mass " + ptmMass + " on the N-terminus of the peptide " + peptideSequence + ".");
                            }
                        } else if (site == peptideSequence.length() + 1) {
                            // C-term ptm
                            for (PTM ptm : modifications.get(ptmMass)) {
                                if (ptm.isCTerm() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(peptideSequence.length())) {
                                    mappedModification = ptm;
                                    break;
                                }
                            }
                            if (mappedModification == null) {
                                throw new IllegalArgumentException("Could not map the PTM of mass " + ptmMass + " on the C-terminus of the peptide " + peptideSequence + ".");
                            }
                        } else {
                            for (PTM ptm : modifications.get(ptmMass)) {
                                if (peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(site)) {
                                    mappedModification = ptm;
                                    break;
                                }
                            }
                            if (mappedModification == null) {
                                throw new IllegalArgumentException("Could not map the PTM of mass " + ptmMass + " at site " + site + " in peptide " + peptide.getSequence() + ".");
                            }
                        }

                        String ptmName = mappedModification.getName();

                        PtmScoring ptmScoring = ptmScores.getPtmScoring(ptmName);

                        if (ptmScoring == null) {
                            ptmScoring = new PtmScoring(ptmName);
                            ptmScores.addPtmScoring(ptmName, ptmScoring);
                        }

                        ptmScoring.setProbabilisticScore(site, scores.get(site));

                    }
                }
            }

            spectrumMatch.addUrParam(ptmScores);
            identification.updateSpectrumMatch(spectrumMatch);
        }
    }

    /**
     * Attaches scores to possible PTM locations to spectrum matches.
     *
     * @param identification identification object containing the identification
     * matches
     * @param psmSpecificMap the PSM specific target/decoy scoring map
     * @param inspectedSpectra the spectra to inspect
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception
     */
    public void scorePSMPTMs(Identification identification, PsmSpecificMap psmSpecificMap, ArrayList<String> inspectedSpectra, WaitingHandler waitingHandler, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        int max = inspectedSpectra.size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        PSParameter psParameter = new PSParameter();
        for (String spectrumKey : inspectedSpectra) {
            waitingHandler.increaseSecondaryProgressCounter();
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            if (spectrumMatch.getBestPeptideAssumption() != null) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                scorePTMs(identification, spectrumMatch, searchParameters, annotationPreferences, ptmScoringPreferences, sequenceMatchingPreferences);
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Scores PTM locations for a desired spectrumMatch.
     *
     * @param identification identification object containing the identification
     * matches
     * @param spectrumMatch The spectrum match of interest
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param scoringPreferences the PTM scoring preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * reading/writing the an identification match
     */
    public void scorePTMs(Identification identification, SpectrumMatch spectrumMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences,
            PTMScoringPreferences scoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        attachDeltaScore(identification, spectrumMatch, sequenceMatchingPreferences);
        
        if (scoringPreferences.isProbabilitsticScoreCalculation()) {
            attachProbabilisticScore(identification, spectrumMatch, searchParameters, annotationPreferences, scoringPreferences, sequenceMatchingPreferences);
        }

        PSPtmScores ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

        if (ptmScores != null) {

            Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

            ArrayList<Double> modificationMasses = new ArrayList<Double>();
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                if (!modificationMasses.contains(ptm.getMass())) {
                    modificationMasses.add(ptm.getMass());
                }
            }

            for (double ptmMass : modificationMasses) {

                int nPtm = peptide.getNVariableModifications(ptmMass);
                HashMap<Double, ArrayList<Integer>> dSitesMap = new HashMap<Double, ArrayList<Integer>>();
                HashMap<Double, ArrayList<Integer>> pSitesMap = new HashMap<Double, ArrayList<Integer>>();
                HashMap<Integer, Double> pScores = new HashMap<Integer, Double>();

                for (String modification : ptmScores.getScoredPTMs()) {

                    PTM ptm = ptmFactory.getPTM(modification);

                    if (ptm.getMass() == ptmMass) {

                        PtmScoring ptmScoring = ptmScores.getPtmScoring(modification);

                        for (int site : ptmScoring.getDSites()) {

                            double score = ptmScoring.getDeltaScore(site);
                            ArrayList<Integer> sites = dSitesMap.get(score);

                            if (sites == null) {
                                sites = new ArrayList<Integer>();
                                dSitesMap.put(score, sites);
                            }

                            sites.add(site);
                        }

                        for (int site : ptmScoring.getProbabilisticSites()) {

                            double score = ptmScoring.getProbabilisticScore(site);
                            ArrayList<Integer> sites = pSitesMap.get(score);

                            if (sites == null) {
                                sites = new ArrayList<Integer>();
                                pSitesMap.put(score, sites);
                            }

                            sites.add(site);

                            if (!pScores.containsKey(site)) {
                                pScores.put(site, score);
                            } else {
                                throw new IllegalArgumentException("Duplicate PTM score found at site " + site
                                        + " for peptide " + peptide.getSequence() + " in spectrum " + spectrumMatch.getKey() + ".");
                            }
                        }
                    }
                }

                ArrayList<Integer> dSites = new ArrayList<Integer>(nPtm);
                ArrayList<Double> scores = new ArrayList<Double>(dSitesMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                int cpt = 0;

                for (double score : scores) {

                    ArrayList<Integer> sites = dSitesMap.get(score);

                    if (sites.size() >= nPtm - cpt) {
                        dSites.addAll(sites);
                        cpt += sites.size();
                    } else {
                        Collections.shuffle(sites);
                        for (Integer site : sites) {
                            if (cpt == nPtm) {
                                break;
                            }
                            dSites.add(site);
                        }
                    }

                    if (cpt == nPtm) {
                        break;
                    }
                }

                ArrayList<Integer> pSites = new ArrayList<Integer>(nPtm);
                scores = new ArrayList<Double>(pSitesMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                cpt = 0;

                for (double score : scores) {

                    ArrayList<Integer> sites = pSitesMap.get(score);

                    if (sites.size() >= nPtm - cpt) {
                        pSites.addAll(sites);
                        cpt += sites.size();
                    } else {
                        Collections.shuffle(sites);
                        for (Integer site : sites) {
                            if (cpt == nPtm) {
                                break;
                            }
                            pSites.add(site);
                        }
                    }
                    if (cpt == nPtm) {
                        break;
                    }
                }

                if (dSites.size() < nPtm) {
                    throw new IllegalArgumentException("found less D-scores than PTMs for modification of mass "
                            + ptmMass + " in peptide " + peptide.getSequence() + " in spectrum " + spectrumMatch.getKey() + ".");
                }

                for (Integer site : pSites) {
                    boolean conflict = !dSites.contains(site);
                    psmPTMMap.addPoint(ptmMass, -pScores.get(site), spectrumMatch, conflict);
                }
            }
        }
    }

    /**
     * Computes the statistics on localization confidence (beta).
     *
     * @param waitingHandler waiting handler displaying progress to the user
     *
     * @param psmError the desired PSM localization error rate
     */
    public void computeLocalizationStatistics(WaitingHandler waitingHandler, double psmError) {

        waitingHandler.setWaitingText("Estimating Localization Error Rates. Please Wait...");

        for (Double ptmMass : psmPTMMap.getModificationsScored()) {

            for (int mapKey : psmPTMMap.getKeys(ptmMass).keySet()) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
                TargetDecoyMap currentMap = psmPTMMap.getTargetDecoyMap(ptmMass, mapKey);
                TargetDecoyResults currentResults = currentMap.getTargetDecoyResults();
                currentResults.setInputType(1);
                currentResults.setUserInput(psmError);
                currentResults.setClassicalEstimators(true);
                currentResults.setClassicalValidation(true);
                currentResults.setFdrLimit(psmError);
                currentMap.getTargetDecoySeries().getFDRResults(currentResults);
            }
        }
    }

    /**
     * Scores the PTMs for a peptide match.
     *
     * @param identification identification object containing the identification
     * matches
     * @param peptideMatch the peptide match of interest
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param scoringPreferences the PTM scoring preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserializing a match
     */
    public void scorePTMs(Identification identification, PeptideMatch peptideMatch, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, PTMScoringPreferences scoringPreferences) throws Exception {

        PSPtmScores peptideScores = new PSPtmScores();
        PSParameter psParameter = new PSParameter();
        HashMap<Double, Integer> variableModifications = new HashMap<Double, Integer>();

        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                if (ptm.getType() == PTM.MODAA) {
                    Double ptmMass = ptm.getMass();
                    Integer nPtm = variableModifications.get(ptmMass);
                    if (nPtm == null) {
                        variableModifications.put(ptmMass, 1);
                    } else {
                        variableModifications.put(ptmMass, nPtm + 1);
                    }
                }
            }
        }

        if (variableModifications.size() > 0) {

            int validationLevel = MatchValidationLevel.not_validated.getIndex();
            double bestConfidence = 0;
            ArrayList<String> bestKeys = new ArrayList<String>();

            identification.loadSpectrumMatches(peptideMatch.getSpectrumMatches(), null);
            identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);

            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                int tempValidationLevel = psParameter.getMatchValidationLevel().getIndex();
                if (tempValidationLevel >= validationLevel) {
                    if (tempValidationLevel != validationLevel) {
                        bestKeys.clear();
                        validationLevel = tempValidationLevel;
                    }
                    bestKeys.add(spectrumKey);
                } else if (tempValidationLevel == MatchValidationLevel.not_validated.getIndex()) {
                    if (psParameter.getPsmConfidence() > bestConfidence) {
                        bestConfidence = psParameter.getPsmConfidence();
                        bestKeys.clear();
                        bestKeys.add(spectrumKey);
                    } else if (psParameter.getPsmConfidence() == bestConfidence) {
                        bestKeys.add(spectrumKey);
                    }
                }
            }

            identification.loadSpectrumMatches(bestKeys, null);
            identification.loadSpectrumMatchParameters(bestKeys, psParameter, null);

            HashMap<Double, ArrayList<Integer>> confidentSites = new HashMap<Double, ArrayList<Integer>>();

            // Map confident sites
            for (String spectrumKey : bestKeys) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                for (String ptmName : psmScores.getScoredPTMs()) {
                    PtmScoring psmScoring = psmScores.getPtmScoring(ptmName);
                    PtmScoring peptideScoring = peptideScores.getPtmScoring(ptmName);
                    if (peptideScoring == null) {
                        peptideScoring = new PtmScoring(ptmName);
                        peptideScores.addPtmScoring(ptmName, peptideScoring);
                    }
                    for (int site : psmScoring.getAllPtmLocations()) {
                        double psmDScore = psmScoring.getDeltaScore(site);
                        double peptideDScore = peptideScoring.getDeltaScore(site);
                        if (peptideDScore < psmDScore) {
                            peptideScoring.setDeltaScore(site, psmDScore);
                        }
                        double psmPScore = psmScoring.getProbabilisticScore(site);
                        double peptidePScore = peptideScoring.getProbabilisticScore(site);
                        if (peptidePScore < psmPScore) {
                            peptideScoring.setProbabilisticScore(site, psmPScore);
                        }
                        int psmValidationLevel = psmScoring.getLocalizationConfidence(site);
                        int peptideValidationLevel = peptideScoring.getLocalizationConfidence(site);
                        if (peptideValidationLevel < psmValidationLevel) {
                            peptideScoring.setSiteConfidence(site, psmValidationLevel);
                        }
                    }
                }

                for (int confidentSite : psmScores.getConfidentSites()) {
                    for (String ptmName : psmScores.getConfidentModificationsAt(confidentSite)) {
                        PTM ptm = ptmFactory.getPTM(ptmName);
                        Double ptmMass = ptm.getMass();
                        ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                        if (ptmConfidentSites == null) {
                            ptmConfidentSites = new ArrayList<Integer>();
                            confidentSites.put(ptmMass, ptmConfidentSites);
                        }
                        if (!ptmConfidentSites.contains(confidentSite)) {
                            ptmConfidentSites.add(confidentSite);
                            peptideScores.addConfidentModificationSite(ptmName, confidentSite);
                        }
                    }
                }
            }

            boolean enoughSites = true;
            for (double ptmMass : variableModifications.keySet()) {
                int nPtms = variableModifications.get(ptmMass);
                int nConfident = 0;
                ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                if (ptmConfidentSites != null) {
                    nConfident = ptmConfidentSites.size();
                }
                if (nConfident < nPtms) {
                    enoughSites = false;
                    break;
                }
            }
            if (!enoughSites) {

                HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousSites = new HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>>();

                for (String spectrumKey : bestKeys) {

                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                    for (int representativeSite : psmScores.getRepresentativeSites()) {
                        HashMap<Integer, ArrayList<String>> ambiguousMappingAtSite = psmScores.getAmbiguousPtmsAtRepresentativeSite(representativeSite);
                        for (int site : ambiguousMappingAtSite.keySet()) {
                            for (String ptmName : ambiguousMappingAtSite.get(site)) {
                                PTM ptm = ptmFactory.getPTM(ptmName);
                                Double ptmMass = ptm.getMass();
                                ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                                if (ptmConfidentSites == null || !ptmConfidentSites.contains(site)) {
                                    double probabilisticScore = 0.0;
                                    double dScore = 0.0;
                                    PtmScoring ptmScoring = psmScores.getPtmScoring(ptmName);
                                    if (ptmScoring != null) {
                                        probabilisticScore = ptmScoring.getProbabilisticScore(site);
                                        dScore = ptmScoring.getDeltaScore(site);
                                    }
                                    HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> ptmScoreMap = ambiguousSites.get(ptmMass);
                                    if (ptmScoreMap == null) {
                                        ptmScoreMap = new HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>();
                                        ambiguousSites.put(ptmMass, ptmScoreMap);
                                    }
                                    HashMap<Double, HashMap<Integer, ArrayList<String>>> pScoreMap = ptmScoreMap.get(probabilisticScore);
                                    if (pScoreMap == null) {
                                        pScoreMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
                                        ptmScoreMap.put(probabilisticScore, pScoreMap);
                                    }
                                    HashMap<Integer, ArrayList<String>> dScoreMap = pScoreMap.get(dScore);
                                    if (dScoreMap == null) {
                                        dScoreMap = new HashMap<Integer, ArrayList<String>>();
                                        pScoreMap.put(dScore, dScoreMap);
                                    }
                                    ArrayList<String> modifications = dScoreMap.get(site);
                                    if (modifications == null) {
                                        modifications = new ArrayList<String>();
                                        dScoreMap.put(site, modifications);
                                    }
                                    modifications.add(ptmName);
                                }
                            }
                        }
                    }
                }

                for (double ptmMass : variableModifications.keySet()) {
                    int nPtm = variableModifications.get(ptmMass);
                    int nConfident = 0;
                    ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                    if (ptmConfidentSites != null) {
                        nConfident = ptmConfidentSites.size();
                    }
                    if (nConfident < nPtm) {
                        int nRepresentatives = nPtm - nConfident;
                        HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> ambiguousSitesScores = ambiguousSites.get(ptmMass);
                        if (ambiguousSitesScores == null) {
                            throw new IllegalArgumentException("Not enough site possibilities found in PSMs for PTM of mass " + ptmMass + " in peptide " + peptideMatch.getKey() + ".");
                        }
                        HashMap<Integer, HashMap<Integer, ArrayList<String>>> representativeToSecondaryMap = getRepresentativeToSecondaryMap(ambiguousSitesScores, nRepresentatives);
                        for (int representativeSite : representativeToSecondaryMap.keySet()) {
                            peptideScores.addAmbiguousModificationSites(representativeSite, representativeToSecondaryMap.get(representativeSite));
                        }
                    }
                }
            }

            peptideMatch.addUrParam(peptideScores);
            identification.updatePeptideMatch(peptideMatch);
        }
    }

    /**
     * Returns a representative to secondary sites map (representative site ->
     * secondary site -> list of ptm names) based on an ambiguous sites scores
     * map (probabilistic score -> delta score -> site -> list of ptm names).
     *
     * @param ambiguousSitesScores a map of the ambiguous sites scores
     * @param nRepresentatives the number of representative sites allowed
     *
     * @return a representative to secondary sites map
     */
    private HashMap<Integer, HashMap<Integer, ArrayList<String>>> getRepresentativeToSecondaryMap(HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> ambiguousSitesScores, int nRepresentatives) {

        HashMap<Integer, ArrayList<String>> representativeSites = new HashMap<Integer, ArrayList<String>>();
        HashMap<Integer, ArrayList<String>> secondarySites = new HashMap<Integer, ArrayList<String>>();
        ArrayList<Double> pScores = new ArrayList<Double>(ambiguousSitesScores.keySet());
        Collections.sort(pScores, Collections.reverseOrder());
        for (double pScore : pScores) {
            HashMap<Double, HashMap<Integer, ArrayList<String>>> dScoresMap = ambiguousSitesScores.get(pScore);
            ArrayList<Double> dScores = new ArrayList<Double>(dScoresMap.keySet());
            Collections.sort(dScores, Collections.reverseOrder());
            for (double dScore : dScores) {
                HashMap<Integer, ArrayList<String>> siteMap = dScoresMap.get(dScore);
                ArrayList<Integer> sites = new ArrayList<Integer>(siteMap.keySet());
                Collections.sort(sites);
                for (int site : sites) {
                    if (representativeSites.size() < nRepresentatives) {
                        for (String ptmName : siteMap.get(site)) {
                            ArrayList<String> modifications = representativeSites.get(site);
                            if (modifications == null) {
                                modifications = new ArrayList<String>();
                                representativeSites.put(site, modifications);
                            }
                            modifications.add(ptmName);
                        }
                    } else {
                        for (String ptmName : siteMap.get(site)) {
                            ArrayList<String> modifications = secondarySites.get(site);
                            if (modifications == null) {
                                modifications = new ArrayList<String>();
                                secondarySites.put(site, modifications);
                            }
                            modifications.add(ptmName);
                        }
                    }
                }
            }
        }
        HashMap<Integer, HashMap<Integer, ArrayList<String>>> representativeToSecondaryMap = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
        for (int representativeSite : representativeSites.keySet()) {
            HashMap<Integer, ArrayList<String>> siteMap = new HashMap<Integer, ArrayList<String>>();
            siteMap.put(representativeSite, representativeSites.get(representativeSite));
            representativeToSecondaryMap.put(representativeSite, siteMap);
        }
        for (int secondarySite : secondarySites.keySet()) {
            Integer distance = null;
            Integer representativeSite = null;
            for (int tempRepresentativeSite : representativeToSecondaryMap.keySet()) {
                int tempDistance = Math.abs(secondarySite - tempRepresentativeSite);
                if (representativeSite == null || tempDistance < distance || tempDistance == distance && tempRepresentativeSite < representativeSite) {
                    representativeSite = tempRepresentativeSite;
                    distance = tempDistance;
                }
            }
            HashMap<Integer, ArrayList<String>> representativeSiteMap = representativeToSecondaryMap.get(representativeSite);
            representativeSiteMap.put(secondarySite, secondarySites.get(secondarySite));
        }
        return representativeToSecondaryMap;
    }

    /**
     * Scores PTMs in a protein match.
     *
     * @param identification identification object containing the identification
     * matches
     * @param proteinMatch the protein match
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param scorePeptides if true peptide scores will be recalculated
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserilalizing a match
     */
    public void scorePTMs(Identification identification, ProteinMatch proteinMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences,
            boolean scorePeptides, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        PSParameter psParameter = new PSParameter();
        Protein protein = null;
        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), psParameter, null);

        HashMap<Integer, ArrayList<String>> confidentSites = new HashMap<Integer, ArrayList<String>>();
        HashMap<Integer, HashMap<Integer, ArrayList<String>>> ambiguousSites = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();

        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
            if (psParameter.getMatchValidationLevel().isValidated() && Peptide.isModified(peptideKey)) {
                PeptideMatch peptideMath = identification.getPeptideMatch(peptideKey);
                String peptideSequence = Peptide.getSequence(peptideKey);
                if (peptideMath.getUrParam(new PSPtmScores()) == null || scorePeptides) {
                    scorePTMs(identification, peptideMath, searchParameters, annotationPreferences, ptmScoringPreferences);
                }
                PSPtmScores peptideScores = (PSPtmScores) peptideMath.getUrParam(new PSPtmScores());
                if (peptideScores != null) {

                    if (protein == null) {
                        protein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                    }
                    ArrayList<Integer> peptideStart = protein.getPeptideStart(peptideSequence,
                            sequenceMatchingPreferences);
                    for (int confidentSite : peptideScores.getConfidentSites()) {
                        for (int peptideTempStart : peptideStart) {
                            int siteOnProtein = peptideTempStart + confidentSite - 2;
                            ArrayList<String> modificationsAtSite = confidentSites.get(siteOnProtein);
                            if (modificationsAtSite == null) {
                                modificationsAtSite = new ArrayList<String>();
                                confidentSites.put(siteOnProtein, modificationsAtSite);
                            }
                            for (String ptmName : peptideScores.getConfidentModificationsAt(confidentSite)) {
                                if (!modificationsAtSite.contains(ptmName)) {
                                    modificationsAtSite.add(ptmName);
                                }
                            }
                        }
                    }
                    for (int representativeSite : peptideScores.getRepresentativeSites()) {
                        HashMap<Integer, ArrayList<String>> peptideAmbiguousSites = peptideScores.getAmbiguousPtmsAtRepresentativeSite(representativeSite);
                        for (int peptideTempStart : peptideStart) {
                            int proteinRepresentativeSite = peptideTempStart + representativeSite - 2;
                            HashMap<Integer, ArrayList<String>> proteinAmbiguousSites = ambiguousSites.get(proteinRepresentativeSite);
                            if (proteinAmbiguousSites == null) {
                                proteinAmbiguousSites = new HashMap<Integer, ArrayList<String>>(peptideAmbiguousSites.size());
                                ambiguousSites.put(proteinRepresentativeSite, proteinAmbiguousSites);
                            }
                            for (int peptideSite : peptideAmbiguousSites.keySet()) {
                                int siteOnProtein = peptideTempStart + peptideSite - 2;
                                proteinAmbiguousSites.put(siteOnProtein, peptideAmbiguousSites.get(peptideSite));
                            }
                        }
                    }
                }
            }
        }

        // remove ambiguous sites where a confident was found and merge overlapping groups
        PSPtmScores proteinScores = new PSPtmScores();
        ArrayList<Integer> representativeSites = new ArrayList<Integer>(ambiguousSites.keySet());
        Collections.sort(representativeSites);

        for (Integer representativeSite : representativeSites) {
            HashMap<Integer, ArrayList<String>> secondarySitesMap = ambiguousSites.get(representativeSite);
            ArrayList<Integer> secondarySites = new ArrayList<Integer>(secondarySitesMap.keySet());
            for (int secondarySite : secondarySites) {
                ArrayList<String> confidentPtms = confidentSites.get(secondarySite);
                if (confidentPtms != null) {
                    boolean samePtm = false;
                    for (String modification : confidentPtms) {
                        PTM confidentPtm = ptmFactory.getPTM(modification);
                        for (String secondaryModification : secondarySitesMap.get(secondarySite)) {
                            PTM secondaryPtm = ptmFactory.getPTM(secondaryModification);
                            if (secondaryPtm.getMass() == confidentPtm.getMass()) {
                                samePtm = true;
                                break;
                            }
                        }
                        if (samePtm) {
                            break;
                        }
                    }
                    if (samePtm) {
                        ambiguousSites.remove(representativeSite);
                        break;
                    }
                }
                if (secondarySite != representativeSite) {
                    ArrayList<Integer> tempRepresentativeSites = new ArrayList<Integer>(ambiguousSites.keySet());
                    Collections.sort(tempRepresentativeSites);
                    for (Integer previousSite : tempRepresentativeSites) {
                        if (previousSite >= representativeSite) {
                            break;
                        }
                        if (previousSite == secondarySite) {
                            HashMap<Integer, ArrayList<String>> previousSites = ambiguousSites.get(previousSite);
                            ArrayList<String> previousPtms = previousSites.get(previousSite);
                            boolean samePtm = false;
                            for (String modification : previousPtms) {
                                PTM previousPtm = ptmFactory.getPTM(modification);
                                for (String secondaryModification : secondarySitesMap.get(secondarySite)) {
                                    PTM secondaryPtm = ptmFactory.getPTM(secondaryModification);
                                    if (secondaryPtm.getMass() == previousPtm.getMass()) {
                                        samePtm = true;
                                        break;
                                    }
                                }
                                if (samePtm) {
                                    break;
                                }
                            }
                            if (samePtm) {
                                for (int tempSecondarySite : secondarySitesMap.keySet()) {
                                    if (!previousSites.containsKey(secondarySite)) {
                                        previousSites.put(tempSecondarySite, secondarySitesMap.get(tempSecondarySite));
                                    }
                                }
                                ambiguousSites.remove(representativeSite);
                            }
                        }
                    }
                }
            }
        }

        for (int confidentSite : confidentSites.keySet()) {
            for (String modificationName : confidentSites.get(confidentSite)) {
                proteinScores.addConfidentModificationSite(modificationName, confidentSite);
            }
        }

        for (int representativeSite : ambiguousSites.keySet()) {
            proteinScores.addAmbiguousModificationSites(representativeSite, ambiguousSites.get(representativeSite));
        }

        proteinMatch.addUrParam(proteinScores);
        identification.updateProteinMatch(proteinMatch);
    }

    /**
     * Scores the PTMs of all PSMs contained in an identification object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scorePsmPtms(Identification identification, WaitingHandler waitingHandler, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        waitingHandler.setWaitingText("Scoring Peptide PTMs. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        for (String spectrumFileName : identification.getSpectrumFiles()) {
            identification.loadSpectrumMatches(spectrumFileName, null);
            for (String spectrumKey : identification.getSpectrumIdentification(spectrumFileName)) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    scorePTMs(identification, spectrumMatch, searchParameters, annotationPreferences, ptmScoringPreferences, sequenceMatchingPreferences);
                }
                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Scores the PTMs of all peptide matches contained in an identification
     * object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scorePeptidePtms(Identification identification, WaitingHandler waitingHandler, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, PTMScoringPreferences ptmScoringPreferences) throws Exception {

        waitingHandler.setWaitingText("Scoring Peptide PTMs. Please Wait...");

        int max = identification.getPeptideIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        identification.loadPeptideMatches(null);

        for (String peptideKey : identification.getPeptideIdentification()) {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            scorePTMs(identification, peptideMatch, searchParameters, annotationPreferences, ptmScoringPreferences);
            waitingHandler.increaseSecondaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Scores the PTMs of all protein matches contained in an identification
     * object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param metrics if provided, metrics on proteins will be saved while
     * iterating the matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scoreProteinPtms(Identification identification, Metrics metrics, WaitingHandler waitingHandler, SearchParameters searchParameters,
            AnnotationPreferences annotationPreferences, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {
        scoreProteinPtms(identification, metrics, waitingHandler, searchParameters, annotationPreferences, ptmScoringPreferences, new IdFilter(), null, sequenceMatchingPreferences);
    }

    /**
     * Scores the PTMs of all protein matches contained in an identification
     * object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param metrics if provided, metrics on proteins will be saved while
     * iterating the matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param idFilter the identification filter, needed only to get max values
     * @param spectrumCountingPreferences the spectrum counting preferences. If
     * not null, the maximum spectrum counting value will be stored in the
     * Metrics.
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scoreProteinPtms(Identification identification, Metrics metrics, WaitingHandler waitingHandler, SearchParameters searchParameters, AnnotationPreferences annotationPreferences,
            PTMScoringPreferences ptmScoringPreferences, IdFilter idFilter, SpectrumCountingPreferences spectrumCountingPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        waitingHandler.setWaitingText("Scoring Protein PTMs. Please Wait...");

        int max = identification.getProteinIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);
        identification.loadProteinMatches(null);
        identification.loadProteinMatchParameters(new PSParameter(), null);

        // If needed, while we are iterating proteins, we will take the maximal spectrum counting value and number of validated proteins as well.
        int nValidatedProteins = 0;
        int nConfidentProteins = 0;
        PSParameter psParameter = new PSParameter();
        double tempSpectrumCounting, maxSpectrumCounting = 0;
        Enzyme enzyme = searchParameters.getEnzyme();
        int maxPepLength = idFilter.getMaxPepLength();

        for (String proteinKey : identification.getProteinIdentification()) {
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            scorePTMs(identification, proteinMatch, searchParameters, annotationPreferences, false, ptmScoringPreferences, sequenceMatchingPreferences);

            if (metrics != null) {
                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    nValidatedProteins++;
                    if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                        nConfidentProteins++;
                    }
                }
                if (spectrumCountingPreferences != null) {
                    tempSpectrumCounting = IdentificationFeaturesGenerator.estimateSpectrumCounting(identification, sequenceFactory,
                            proteinKey, spectrumCountingPreferences, enzyme, maxPepLength, sequenceMatchingPreferences);
                    if (tempSpectrumCounting > maxSpectrumCounting) {
                        maxSpectrumCounting = tempSpectrumCounting;
                    }
                }
            }
            waitingHandler.increaseSecondaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }
        if (metrics != null) {
            metrics.setMaxSpectrumCounting(maxSpectrumCounting);
            metrics.setnValidatedProteins(nValidatedProteins);
            metrics.setnConfidentProteins(nConfidentProteins);
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Infers the PTM localization and its confidence for the best match of
     * every spectrum
     *
     * @param identification identification object containing the identification
     * matches
     * @param waitingHandler waiting handler displaying progress to the user
     * @param ptmScoringPreferences the PTM scoring preferences
     * @param searchParameters the search parameters used
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws SQLException exception thrown whenever a problem occurred while
     * interacting with the database
     * @throws IOException exception thrown whenever a problem occurred while
     * writing/reading the database or the FASTA file
     * @throws ClassNotFoundException exception thrown whenever a problem
     * occurred while deserializing an object from the database
     * @throws IllegalArgumentException exception thrown whenever an error
     * occurred while reading a protein sequence
     * @throws InterruptedException exception thrown whenever an error occurred
     * while reading a protein sequence
     */
    public void peptideInference(Identification identification, PTMScoringPreferences ptmScoringPreferences, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, IllegalArgumentException, InterruptedException {

        waitingHandler.setWaitingText("Peptide Inference. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        HashMap<String, HashMap<String, ArrayList<String>>> confidentPeptideInference = new HashMap<String, HashMap<String, ArrayList<String>>>();
        HashMap<String, HashMap<String, ArrayList<String>>> notConfidentPeptideInference = new HashMap<String, HashMap<String, ArrayList<String>>>();

        for (String spectrumFileName : identification.getSpectrumFiles()) {
            identification.loadSpectrumMatches(spectrumFileName, null);
            for (String spectrumKey : identification.getSpectrumIdentification(spectrumFileName)) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                if (spectrumMatch.getBestPeptideAssumption() != null) {
                    boolean variableAA = false;
                    for (ModificationMatch modificationMatch : spectrumMatch.getBestPeptideAssumption().getPeptide().getModificationMatches()) {
                        if (modificationMatch.isVariable()) {
                            String modName = modificationMatch.getTheoreticPtm();
                            PTM ptm = ptmFactory.getPTM(modName);
                            if (ptm.getType() == PTM.MODAA) {
                                variableAA = true;
                                break;
                            }
                        }
                    }
                    if (variableAA) {
                        ptmInference(spectrumMatch, ptmScoringPreferences, searchParameters, sequenceMatchingPreferences);
                        boolean confident = true;
                        for (ModificationMatch modMatch : spectrumMatch.getBestPeptideAssumption().getPeptide().getModificationMatches()) {
                            if (modMatch.isVariable()) {
                                String modName = modMatch.getTheoreticPtm();
                                PTM ptm = ptmFactory.getPTM(modName);
                                if (ptm.getType() == PTM.MODAA) {
                                    if (!modMatch.isConfident()) {
                                        HashMap<String, ArrayList<String>> fileMap = notConfidentPeptideInference.get(spectrumFileName);
                                        if (fileMap == null) {
                                            fileMap = new HashMap<String, ArrayList<String>>();
                                            notConfidentPeptideInference.put(spectrumFileName, fileMap);
                                        }
                                        ArrayList<String> spectra = fileMap.get(modName);
                                        if (spectra == null) {
                                            spectra = new ArrayList<String>();
                                            fileMap.put(modName, spectra);
                                        }
                                        spectra.add(spectrumKey);
                                        confident = false;
                                    } else {
                                        HashMap<String, ArrayList<String>> modMap = confidentPeptideInference.get(modName);
                                        if (modMap == null) {
                                            modMap = new HashMap<String, ArrayList<String>>();
                                            confidentPeptideInference.put(modName, modMap);
                                        }
                                        String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                                        ArrayList<String> spectra = modMap.get(sequence);
                                        if (spectra == null) {
                                            spectra = new ArrayList<String>();
                                            modMap.put(sequence, spectra);
                                        }
                                        spectra.add(spectrumKey);
                                    }
                                }
                            }
                        }
                        identification.updateSpectrumMatch(spectrumMatch);
                        if (confident) {
                            waitingHandler.increaseSecondaryProgressCounter();
                        }
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    } else {
                        waitingHandler.increaseSecondaryProgressCounter();
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    }
                }
            }
        }
        // try to infer the modification site based on any related peptide
        for (String spectrumFile : notConfidentPeptideInference.keySet()) {

            HashSet<String> progress = new HashSet<String>();

            for (String modification : notConfidentPeptideInference.get(spectrumFile).keySet()) {

                ArrayList<String> spectrumKeys = notConfidentPeptideInference.get(spectrumFile).get(modification);

                identification.loadSpectrumMatches(spectrumKeys, null);

                for (String spectrumKey : spectrumKeys) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                    String sequence = peptide.getSequence();
                    String notConfidentKey = peptide.getKey();
                    int nMod = Peptide.getModificationCount(notConfidentKey, modification);
                    ArrayList<Integer> tempLocalizations, oldLocalizations = Peptide.getNModificationLocalized(notConfidentKey, modification);
                    ArrayList<Integer> newLocalizationCandidates = new ArrayList<Integer>();

                    if (confidentPeptideInference.containsKey(modification)) {

                        // See if we can explain this peptide by another already identified peptides with the same number of modifications (the two peptides will be merged)
                        ArrayList<String> keys = confidentPeptideInference.get(modification).get(sequence);

                        if (keys != null) {
                            for (String tempKey : keys) {
                                SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getKey();
                                if (Peptide.getModificationCount(secondaryKey, modification) == nMod) {
                                    tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, modification);
                                    for (int localization : tempLocalizations) {
                                        if (!oldLocalizations.contains(localization) && !newLocalizationCandidates.contains(localization)) {
                                            newLocalizationCandidates.add(localization);
                                        }
                                    }
                                }
                            }
                            if (oldLocalizations.size() + newLocalizationCandidates.size() < nMod) {
                                // we cannot merge this peptide, see whether we can explain the remaining modifications using peptides with the same sequence but other modification profile
                                for (String tempKey : keys) {
                                    SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                    String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getKey();
                                    tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, modification);
                                    for (int localization : tempLocalizations) {
                                        if (!oldLocalizations.contains(localization) && !newLocalizationCandidates.contains(localization)) {
                                            newLocalizationCandidates.add(localization);
                                        }
                                    }
                                }
                            }
                        }
                        if (oldLocalizations.size() + newLocalizationCandidates.size() < nMod) {
                            // There are still unexplained sites, let's see if we find a related peptide which can help. That is uggly but the results should be cool.
                            for (String otherSequence : confidentPeptideInference.get(modification).keySet()) {

                                // @TODO: are semi-specific, top-down, whole protein and no enzyme handled correctly??
                                Enzyme enzyme = searchParameters.getEnzyme();
                                if (enzyme.isSemiSpecific() || enzyme.getNmissedCleavages(sequence) == enzyme.getNmissedCleavages(otherSequence)) {
                                    if (!sequence.equals(otherSequence) && sequence.contains(otherSequence)) {
                                        for (String tempKey : confidentPeptideInference.get(modification).get(otherSequence)) {
                                            SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                            String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getKey();
                                            tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, modification);
                                            int tempIndex, ref = 0;
                                            String tempSequence = sequence;
                                            while ((tempIndex = tempSequence.indexOf(otherSequence)) >= 0) {
                                                ref += tempIndex;
                                                for (int localization : tempLocalizations) {
                                                    int shiftedLocalization = ref + localization;
                                                    if (!oldLocalizations.contains(shiftedLocalization) && !newLocalizationCandidates.contains(shiftedLocalization)) {
                                                        newLocalizationCandidates.add(shiftedLocalization);
                                                        PTM ptm = ptmFactory.getPTM(modification);
                                                        if (!peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(shiftedLocalization)) {
                                                            throw new IllegalArgumentException("Wrong PTM site inference: " + modification + " at position " + shiftedLocalization + " on " + sequence + " in spectrum " + spectrumKey + " when using related sequence " + otherSequence + " modified at " + localization + ".");
                                                        }
                                                    }
                                                }
                                                tempSequence = tempSequence.substring(tempIndex + 1);
                                                ref++;
                                            }
                                        }
                                    } else if (!sequence.equals(otherSequence) && otherSequence.contains(sequence)) {
                                        for (String tempKey : confidentPeptideInference.get(modification).get(otherSequence)) {
                                            SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                            String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getKey();
                                            tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, modification);
                                            int tempIndex, ref = 0;
                                            String tempSequence = otherSequence;
                                            while ((tempIndex = tempSequence.indexOf(sequence)) >= 0) {
                                                ref += tempIndex;
                                                for (int localization : tempLocalizations) {
                                                    int shiftedLocalization = localization - ref;
                                                    if (shiftedLocalization > 0 && shiftedLocalization <= sequence.length()
                                                            && !oldLocalizations.contains(shiftedLocalization) && !newLocalizationCandidates.contains(shiftedLocalization)) {
                                                        newLocalizationCandidates.add(shiftedLocalization);
                                                        PTM ptm = ptmFactory.getPTM(modification);
                                                        if (!peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(shiftedLocalization)) {
                                                            throw new IllegalArgumentException("Wrong PTM site inference: " + modification + " at position " + shiftedLocalization + " on " + sequence + " in spectrum " + spectrumKey + " when using related sequence " + otherSequence + " modified at " + localization + ".");
                                                        }
                                                    }
                                                }
                                                tempSequence = tempSequence.substring(tempIndex + 1);
                                                ref++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Map the most likely inferred sites
                        if (!newLocalizationCandidates.isEmpty()) {
                            HashMap<Integer, ModificationMatch> nonConfidentMatches = new HashMap<Integer, ModificationMatch>();
                            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                if (modificationMatch.getTheoreticPtm().equals(modification) && !modificationMatch.isConfident()) {
                                    nonConfidentMatches.put(modificationMatch.getModificationSite(), modificationMatch);
                                }
                            }
                            HashMap<Integer, Integer> mapping = PtmSiteMapping.align(nonConfidentMatches.keySet(), newLocalizationCandidates);
                            for (Integer oldLocalization : mapping.keySet()) {
                                ModificationMatch modificationMatch = nonConfidentMatches.get(oldLocalization);
                                Integer newLocalization = mapping.get(oldLocalization);
                                if (modificationMatch != null && newLocalization != null && newLocalization != oldLocalization) {
                                    PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                                    if (!peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(newLocalization)) {
                                        throw new IllegalArgumentException("Wrong PTM site inference: " + modificationMatch.getTheoreticPtm()
                                                + " at position " + newLocalization + " on " + sequence + " in spectrum " + spectrumKey + ".");
                                    }
                                    modificationMatch.setInferred(true);
                                    modificationMatch.setModificationSite(newLocalization);
                                    PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                    psmScores.changeRepresentativeSite(modification, oldLocalization, newLocalization);
                                }
                            }
                        }
                        identification.updateSpectrumMatch(spectrumMatch);
                    }
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    if (!progress.contains(spectrumKey)) {
                        progress.add(spectrumKey);
                        waitingHandler.increaseSecondaryProgressCounter();
                    }
                }
            }
        }
    }

    /**
     * Infers the modification site of every PSM based on the PTM scores and the
     * FLR settings. The FLR must have been calculated before.
     *
     * @param spectrumMatch the spectrum match inspected
     * @param ptmScorer the PTM scorer used to score PTM sites
     * @param ptmScoringPreferences the PTM scoring preferences as set by the
     * user
     * @param searchParameters the identification parameters
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading a protein sequence
     * @throws IllegalArgumentException exception thrown whenever an error
     * occurred while reading a protein sequence
     * @throws InterruptedException exception thrown whenever an error occurred
     * while reading a protein sequence
     */
    private void ptmInference(SpectrumMatch spectrumMatch, PTMScoringPreferences ptmScoringPreferences, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences)
            throws IOException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        Peptide psPeptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        ModificationProfile modificationProfile = searchParameters.getModificationProfile();
        PSPtmScores ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
        HashMap<Double, ArrayList<ModificationMatch>> modMatchesMap = new HashMap<Double, ArrayList<ModificationMatch>>();
        HashMap<Double, HashMap<Integer, String>> possiblePositions = new HashMap<Double, HashMap<Integer, String>>();
        for (ModificationMatch modificationMatch : psPeptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                String modName = modificationMatch.getTheoreticPtm();
                PTM ptm = ptmFactory.getPTM(modName);
                if (ptm.getType() == PTM.MODAA) {
                    double ptmMass = ptm.getMass();
                    ArrayList<ModificationMatch> ptmOccurence = modMatchesMap.get(ptmMass);
                    if (ptmOccurence == null) {
                        ptmOccurence = new ArrayList<ModificationMatch>();
                        modMatchesMap.put(ptmMass, ptmOccurence);
                    }
                    ptmOccurence.add(modificationMatch);

                    HashMap<Integer, String> ptmPossibleSites = possiblePositions.get(ptmMass);
                    if (ptmPossibleSites == null) {
                        ptmPossibleSites = new HashMap<Integer, String>();
                        possiblePositions.put(ptmMass, ptmPossibleSites);
                    }
                    for (String similarPtmName : modificationProfile.getSimilarNotFixedModifications(ptmMass)) {
                        PTM similarPtm = ptmFactory.getPTM(similarPtmName);
                        for (int pos : psPeptide.getPotentialModificationSites(similarPtm, sequenceMatchingPreferences)) {
                            ptmPossibleSites.put(pos, similarPtmName);
                        }
                    }
                } else {
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    ptmScoring.setSiteConfidence(modificationMatch.getModificationSite(), PtmScoring.VERY_CONFIDENT);
                    modificationMatch.setConfident(true);
                }
            }
        }

        for (double ptmMass : modMatchesMap.keySet()) {
            ArrayList<ModificationMatch> ptmMatches = modMatchesMap.get(ptmMass);
            HashMap<Integer, String> ptmPotentialSites = possiblePositions.get(ptmMass);
            HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> ambiguousSitesMap = new HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>();
            HashMap<Integer, String> confidentSites = new HashMap<Integer, String>();
            int nPTMs = ptmMatches.size();
            if (ptmPotentialSites.size() < ptmMatches.size()) {
                throw new IllegalArgumentException("The occurence of modification of mass " + ptmMass + " (" + ptmMatches.size()
                        + ") is higher than the number of possible sites (" + ptmPotentialSites.size() + ") on sequence " + psPeptide.getSequence()
                        + " in spectrum " + spectrumMatch.getKey() + ".");
            } else if (ptmPotentialSites.size() == ptmMatches.size()) {
                for (ModificationMatch modMatch : ptmMatches) {
                    String modName = modMatch.getTheoreticPtm();
                    int site = modMatch.getModificationSite();
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    ptmScoring.setSiteConfidence(site, PtmScoring.VERY_CONFIDENT);
                    modMatch.setConfident(true);
                    confidentSites.put(site, modName);
                }
            } else if (!ptmScoringPreferences.isProbabilitsticScoreCalculation()
                    || ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore && ptmMatches.size() > 1) {
                double pScore = 0; // no probabilistic score in that case
                for (ModificationMatch modificationMatch : ptmMatches) {
                    String modName = modificationMatch.getTheoreticPtm();
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    ArrayList<Integer> orderedDSites = new ArrayList<Integer>(ptmScoring.getDSites());
                    Collections.sort(orderedDSites);
                    for (int site : orderedDSites) {
                        if (site == modificationMatch.getModificationSite()) {
                            double dScore = ptmScoring.getDeltaScore(site);
                            if (dScore == 0) {
                                ptmScoring.setSiteConfidence(site, PtmScoring.RANDOM);
                                modificationMatch.setConfident(false);
                            } else if (dScore <= 95) {
                                ptmScoring.setSiteConfidence(site, PtmScoring.DOUBTFUL);
                                modificationMatch.setConfident(false);
                            } else {
                                ptmScoring.setSiteConfidence(site, PtmScoring.CONFIDENT);
                                modificationMatch.setConfident(true);
                                confidentSites.put(site, modName);
                            }
                            if (!modificationMatch.isConfident()) {
                                HashMap<Double, HashMap<Integer, ArrayList<String>>> pScoreAmbiguousMap = ambiguousSitesMap.get(pScore);
                                if (pScoreAmbiguousMap == null) {
                                    pScoreAmbiguousMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
                                    ambiguousSitesMap.put(pScore, pScoreAmbiguousMap);
                                }
                                HashMap<Integer, ArrayList<String>> dScoreAmbiguousMap = pScoreAmbiguousMap.get(dScore);
                                if (dScoreAmbiguousMap == null) {
                                    dScoreAmbiguousMap = new HashMap<Integer, ArrayList<String>>();
                                    pScoreAmbiguousMap.put(dScore, dScoreAmbiguousMap);
                                }
                                ArrayList<String> modifications = dScoreAmbiguousMap.get(site);
                                if (modifications == null) {
                                    modifications = new ArrayList<String>();
                                    dScoreAmbiguousMap.put(site, modifications);
                                }
                                modifications.add(modName);
                            }
                        }
                    }
                }
            } else {
                HashMap<Double, HashMap<Double, ArrayList<Integer>>> scoreToSiteMap = new HashMap<Double, HashMap<Double, ArrayList<Integer>>>();
                for (int site : ptmPotentialSites.keySet()) {
                    String modName = ptmPotentialSites.get(site);
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    double pScore = 0;
                    double dScore = 0;
                    if (ptmScoring != null) {
                        pScore = ptmScoring.getProbabilisticScore(site);
                        dScore = ptmScoring.getDeltaScore(site);
                    }
                    HashMap<Double, ArrayList<Integer>> pScoreMap = scoreToSiteMap.get(pScore);
                    if (pScoreMap == null) {
                        pScoreMap = new HashMap<Double, ArrayList<Integer>>();
                        scoreToSiteMap.put(pScore, pScoreMap);
                    }
                    ArrayList<Integer> dScoreSites = pScoreMap.get(dScore);
                    if (dScoreSites == null) {
                        dScoreSites = new ArrayList<Integer>();
                        pScoreMap.put(dScore, dScoreSites);
                    }
                    dScoreSites.add(site);
                }
                ArrayList<Double> pScores = new ArrayList<Double>(scoreToSiteMap.keySet());
                Collections.sort(pScores, Collections.reverseOrder());
                int key = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                TargetDecoyMap currentMap = psmPTMMap.getTargetDecoyMap(ptmMass, key);
                if (currentMap == null) {
                    throw new IllegalArgumentException("No FLR map found for PTM of mass " + ptmMass + " in PSMs of charge " + key + ".");
                }
                int cpt = 0;
                double doubtfulThreshold;
                if (ptmScoringPreferences.isEstimateFlr()) {
                    doubtfulThreshold = -currentMap.getTargetDecoyResults().getScoreLimit();
                } else {
                    doubtfulThreshold = ptmScoringPreferences.getProbabilisticScoreThreshold();
                }
                double randomThreshold = 0;
                if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                    randomThreshold = (100.0 * nPTMs) / ptmPotentialSites.size();
                }
                for (Double pScore : pScores) {
                    HashMap<Double, ArrayList<Integer>> pScoreMap = scoreToSiteMap.get(pScore);
                    ArrayList<Double> dScores = new ArrayList<Double>(pScoreMap.keySet());
                    Collections.sort(dScores, Collections.reverseOrder());
                    for (Double dScore : dScores) {
                        ArrayList<Integer> sites = pScoreMap.get(dScore);
                        Collections.sort(sites);
                        boolean enoughPtms = nPTMs - cpt >= sites.size();
                        for (int site : sites) {
                            String modName = ptmPotentialSites.get(site);
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                            if (ptmScoring == null) {
                                ptmScoring = new PtmScoring(modName);
                                ptmScores.addPtmScoring(modName, ptmScoring);
                            }
                            ModificationMatch modificationMatch = null;
                            if (cpt < nPTMs) {
                                modificationMatch = ptmMatches.get(cpt);
                                modificationMatch.setModificationSite(site);
                                modificationMatch.setTheoreticPtm(modName);
                            }
                            if (pScore <= randomThreshold || !enoughPtms) {
                                ptmScoring.setSiteConfidence(site, PtmScoring.RANDOM);
                                if (modificationMatch != null) {
                                    modificationMatch.setConfident(false);
                                }
                            } else if (pScore <= doubtfulThreshold) {
                                ptmScoring.setSiteConfidence(site, PtmScoring.DOUBTFUL);
                                if (modificationMatch != null) {
                                    modificationMatch.setConfident(false);
                                }
                            } else {
                                ptmScoring.setSiteConfidence(site, PtmScoring.VERY_CONFIDENT);
                                if (modificationMatch != null) {
                                    modificationMatch.setConfident(true);
                                }
                                confidentSites.put(site, modName);
                            }
                            if (modificationMatch == null || !modificationMatch.isConfident()) {
                                HashMap<Double, HashMap<Integer, ArrayList<String>>> pScoreAmbiguousMap = ambiguousSitesMap.get(pScore);
                                if (pScoreAmbiguousMap == null) {
                                    pScoreAmbiguousMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>();
                                    ambiguousSitesMap.put(pScore, pScoreAmbiguousMap);
                                }
                                HashMap<Integer, ArrayList<String>> dScoreAmbiguousMap = pScoreAmbiguousMap.get(dScore);
                                if (dScoreAmbiguousMap == null) {
                                    dScoreAmbiguousMap = new HashMap<Integer, ArrayList<String>>();
                                    pScoreAmbiguousMap.put(dScore, dScoreAmbiguousMap);
                                }
                                ArrayList<String> modifications = dScoreAmbiguousMap.get(site);
                                if (modifications == null) {
                                    modifications = new ArrayList<String>();
                                    dScoreAmbiguousMap.put(site, modifications);
                                }
                                modifications.add(modName);
                            }
                        }
                        cpt++;
                    }
                }
            }
            // Select the best scoring ambiguous sites as representative PTM sites
            ArrayList<Integer> ptmConfidentSites = new ArrayList<Integer>(confidentSites.keySet());
            for (int site : ptmConfidentSites) {
                ptmScores.addConfidentModificationSite(confidentSites.get(site), site);
            }
            int nConfident = ptmConfidentSites.size();
            if (nConfident < nPTMs) {
                int nRepresentatives = nPTMs - nConfident;
                HashMap<Integer, HashMap<Integer, ArrayList<String>>> representativeToSecondaryMap = getRepresentativeToSecondaryMap(ambiguousSitesMap, nRepresentatives);
                for (int representativeSite : representativeToSecondaryMap.keySet()) {
                    ptmScores.addAmbiguousModificationSites(representativeSite, representativeToSecondaryMap.get(representativeSite));
                }
            }
        }
    }

    /**
     * Returns the PSM PTM map.
     *
     * @return the PSM PTM map
     */
    public PsmPTMMap getPsmPTMMap() {
        return psmPTMMap;
    }

    /**
     * Sets the PSM PTM map.
     *
     * @param psmPTMMap the PSM PTM map
     */
    public void setPsmPTMMap(PsmPTMMap psmPTMMap) {
        this.psmPTMMap = psmPTMMap;
    }

}
