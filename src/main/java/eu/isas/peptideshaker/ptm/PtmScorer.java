package eu.isas.peptideshaker.ptm;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.ptm.ptmscores.AScore;
import com.compomics.util.experiment.identification.ptm.ptmscores.PhosphoRS;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class scores the PSM PTMs using the scores implemented in compomics utilities
 *
 * @author Marc
 */
public class PtmScorer {

    /**
     * The PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The protein sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The PSM PTM localization conflict map.
     */
    private PsmPTMMap psmPTMMap;

    /**
     * Constructor
     * 
     * @param psmPTMMap the PSM PTM score map
     */
    public PtmScorer(PsmPTMMap psmPTMMap) {
        this.psmPTMMap = psmPTMMap;
    }
    /**
     * Scores the PTM locations using the delta score.
     *
     * @param identification identification object containing the identification matches
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
     * @param identification identification object containing the identification matches
     * @param spectrumMatch the spectrum match studied, the A-score will be
     * calculated for the best assumption
     * @param searchParameters the identification parameters
     * @param annotationPreferences the annotation preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * computing the score
     */
    private void attachProbabilisticScore(Identification identification, SpectrumMatch spectrumMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, PTMScoringPreferences scoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

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
     * @param identification identification object containing the identification matches
     * @param psmSpecificMap the PSM specific target/decoy scoring map
     * @param inspectedSpectra the spectra to inspect
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param ptmScoringPreferences the prm scoring preferences
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
     * @param identification identification object containing the identification matches
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
     * @param identification identification object containing the identification matches
     * @param peptideMatch the peptide match of interest
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param scoringPreferences the PTM scoring preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserializing a match
     */
    public void scorePTMs(Identification identification, PeptideMatch peptideMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, PTMScoringPreferences scoringPreferences) throws Exception {

        PSPtmScores peptideScores = new PSPtmScores();
        PSParameter psParameter = new PSParameter();
        ArrayList<String> variableModifications = new ArrayList<String>();

        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                if (ptm.getType() == PTM.MODAA && !variableModifications.contains(modificationMatch.getTheoreticPtm())) {
                    variableModifications.add(modificationMatch.getTheoreticPtm());
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

            for (String spectrumKey : bestKeys) {

                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                for (String modification : variableModifications) {

                    if (!peptideScores.containsPtm(modification)) {
                        peptideScores.addPtmScoring(modification, new PtmScoring(modification));
                    }

                    PtmScoring spectrumScoring = psmScores.getPtmScoring(modification);
                    if (spectrumScoring != null) {
                        peptideScores.getPtmScoring(modification).addAll(spectrumScoring);
                    }
                }

                for (int confidentSite : psmScores.getConfidentSites()) {
                    for (String ptmName : psmScores.getConfidentModificationsAt(confidentSite)) {
                        peptideScores.addConfidentModificationSite(ptmName, confidentSite);
                    }
                }
                for (int representativeSite : psmScores.getRepresentativeSites()) {
                    HashMap<Integer, ArrayList<String>> ambiguousSites = psmScores.getAmbiguousPtmsAtRepresentativeSite(representativeSite);
                    peptideScores.addAmbiguousModificationSites(representativeSite, ambiguousSites);
                }
            }

            peptideMatch.addUrParam(peptideScores);
            identification.updatePeptideMatch(peptideMatch);
        }
    }

    /**
     * Scores ptms in a protein match.
     *
     * @param identification identification object containing the identification matches
     * @param proteinMatch the protein match
     * @param searchParameters the search preferences containing the m/z
     * tolerances
     * @param annotationPreferences the spectrum annotation preferences
     * @param scorePeptides if true peptide scores will be recalculated
     * @param ptmScoringPreferences the prm scoring preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserilalizing a match
     */
    public void scorePTMs(Identification identification, ProteinMatch proteinMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences,
            boolean scorePeptides, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

        PSPtmScores proteinScores = new PSPtmScores();
        PSParameter psParameter = new PSParameter();
        Protein protein = null;
        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), psParameter, null);

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
                            for (String ptmName : peptideScores.getConfidentModificationsAt(confidentSite)) {
                                proteinScores.addConfidentModificationSite(ptmName, siteOnProtein);
                            }
                        }
                    }
                    for (int representativeSite : peptideScores.getRepresentativeSites()) {
                        HashMap<Integer, ArrayList<String>> ambiguousSites = peptideScores.getAmbiguousPtmsAtRepresentativeSite(representativeSite);
                        for (int peptideTempStart : peptideStart) {
                            HashMap<Integer, ArrayList<String>> proteinAmbiguousSites = new HashMap<Integer, ArrayList<String>>(ambiguousSites.size());
                            for (int peptideSite : ambiguousSites.keySet()) {
                                int siteOnProtein = peptideTempStart + peptideSite - 2;
                                proteinAmbiguousSites.put(siteOnProtein, ambiguousSites.get(peptideSite));
                            }
                            int representativeSiteOnProtein = peptideTempStart + representativeSite - 2;
                            proteinScores.addAmbiguousModificationSites(representativeSiteOnProtein, proteinAmbiguousSites);
                        }
                    }
                }
            }
        }

        proteinMatch.addUrParam(proteinScores);
        identification.updateProteinMatch(proteinMatch);
    }
    
    /**
     * Scores the PTMs of all PSMs contained in an identification object.
     *
     * @param identification identification object containing the identification matches
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
    public void scorePsmPtms(Identification identification, WaitingHandler waitingHandler, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {

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
     * Scores the PTMs of all peptide matches contained in an identification object.
     *
     * @param identification identification object containing the identification matches
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
     * Scores the PTMs of all protein matches contained in an identification object.
     *
     * @param identification identification object containing the identification matches
     * @param metrics if provided, metrics on proteins will be saved while iterating the matches
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
    public void scoreProteinPtms(Identification identification, Metrics metrics, WaitingHandler waitingHandler, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, PTMScoringPreferences ptmScoringPreferences, SequenceMatchingPreferences sequenceMatchingPreferences) throws Exception {
        scoreProteinPtms(identification, metrics, waitingHandler, searchParameters, annotationPreferences, ptmScoringPreferences, new IdFilter(), null, sequenceMatchingPreferences);
    }

    /**
     * Scores the PTMs of all protein matches contained in an identification object.
     *
     * @param identification identification object containing the identification matches
     * @param metrics if provided, metrics on proteins will be saved while iterating the matches
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
