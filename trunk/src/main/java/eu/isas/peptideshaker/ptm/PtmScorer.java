package eu.isas.peptideshaker.ptm;

import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.ShotgunProtocol;
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
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.identification.ptm.ptmscores.AScore;
import com.compomics.util.experiment.identification.ptm.ptmscores.PhosphoRS;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PsmPTMMap;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
     * The number of decimals to which scores should be floored. Ignored if
     * null.
     */
    public static final Integer ptmScoreScale = 10;

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
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = identification.getAssumptions(spectrumMatch.getKey());
        for (Integer id : assumptionsMap.keySet()) {
            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmAssumptions = assumptionsMap.get(id);
            for (ArrayList<SpectrumIdentificationAssumption> assumptionsAtScore : algorithmAssumptions.values()) {
                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : assumptionsAtScore) {
                    if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
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

                    for (Integer id : assumptionsMap.keySet()) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmAssumptions = assumptionsMap.get(id);
                        for (ArrayList<SpectrumIdentificationAssumption> assumptionsAtScore : algorithmAssumptions.values()) {
                            for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : assumptionsAtScore) {
                                if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;

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
                    if (ptmScoreScale != null) {
                        deltaScore = Util.floorDouble(deltaScore, ptmScoreScale);
                    }
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
     * @param identificationParameters the identification parameters
     * @param peptideSpectrumAnnotator the peptide spectrum annotator
     *
     * @throws Exception exception thrown whenever an error occurred while
     * computing the score
     */
    private void attachProbabilisticScore(Identification identification, SpectrumMatch spectrumMatch, IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws Exception {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        AnnotationPreferences annotationPreferences = identificationParameters.getAnnotationPreferences();
        PTMScoringPreferences scoringPreferences = identificationParameters.getPtmScoringPreferences();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();

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
                            searchParameters.getFragmentIonAccuracy(), scoringPreferences.isProbabilisticScoreNeutralLosses(), sequenceMatchingPreferences, peptideSpectrumAnnotator, ptmScoreScale);
                } else if (scoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                    scores = PhosphoRS.getSequenceProbabilities(peptide, modifications.get(ptmMass), spectrum, annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                            spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
                            searchParameters.getFragmentIonAccuracy(), scoringPreferences.isProbabilisticScoreNeutralLosses(),
                            sequenceMatchingPreferences, peptideSpectrumAnnotator, ptmScoreScale);
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
     * Scores PTM locations for a desired spectrum match.
     *
     * @param identification identification object containing the identification
     * matches
     * @param spectrumMatch the spectrum match of interest
     * @param identificationParameters the parameters used for identification
     * @param waitingHandler waiting handler to display progress and allow
     * canceling
     * @param peptideSpectrumAnnotator the spectrum annotator
     *
     * @throws Exception exception thrown whenever an error occurred while
     * reading/writing the an identification match
     */
    public void scorePTMs(Identification identification, SpectrumMatch spectrumMatch, IdentificationParameters identificationParameters, 
            WaitingHandler waitingHandler, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws Exception {

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        attachDeltaScore(identification, spectrumMatch, sequenceMatchingPreferences);

        PTMScoringPreferences scoringPreferences = identificationParameters.getPtmScoringPreferences();

        if (scoringPreferences.isProbabilitsticScoreCalculation()) {
            attachProbabilisticScore(identification, spectrumMatch, identificationParameters, peptideSpectrumAnnotator);
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

        if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
            waitingHandler.increaseSecondaryProgressCounter();
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
     * @param identificationParameters the identification parameters
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserializing a match
     */
    public void scorePTMs(Identification identification, PeptideMatch peptideMatch, IdentificationParameters identificationParameters) throws Exception {

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();

        PSPtmScores peptideScores = new PSPtmScores();
        PSParameter psParameter = new PSParameter();
        HashMap<Double, Integer> variableModifications = new HashMap<Double, Integer>();

        Peptide peptide = peptideMatch.getTheoreticPeptide();
        ArrayList<ModificationMatch> originalMatches = peptide.getModificationMatches();
        ArrayList<ModificationMatch> newModificationMatches = new ArrayList<ModificationMatch>(originalMatches.size());
        HashMap<Double, ArrayList<Integer>> inferredSites = new HashMap<Double, ArrayList<Integer>>();

        String originalKey = peptide.getMatchingKey(sequenceMatchingPreferences);

        for (ModificationMatch modificationMatch : originalMatches) {
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
                    if (modificationMatch.isInferred()) {
                        Integer modificationSite = modificationMatch.getModificationSite();
                        ArrayList<Integer> ptmInferredSites = inferredSites.get(ptmMass);
                        if (ptmInferredSites == null) {
                            ptmInferredSites = new ArrayList<Integer>();
                            inferredSites.put(ptmMass, ptmInferredSites);
                            ptmInferredSites.add(modificationSite);
                        } else if (!ptmInferredSites.contains(modificationSite)) {
                            ptmInferredSites.add(modificationSite);
                        }
                    }
                } else {
                    newModificationMatches.add(modificationMatch);
                }
            } else {
                newModificationMatches.add(modificationMatch);
            }
        }

        HashMap<Double, ArrayList<ModificationMatch>> newMatches = new HashMap<Double, ArrayList<ModificationMatch>>(variableModifications.size());

        if (variableModifications.size() > 0) {

            ArrayList<String> bestKeys = new ArrayList<String>();

            identification.loadSpectrumMatches(peptideMatch.getSpectrumMatches(), null);
            identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);

            boolean validated = false;
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                MatchValidationLevel matchValidationLevel = psParameter.getMatchValidationLevel();
                if (matchValidationLevel.isValidated() && !validated) {
                    bestKeys.clear();
                    validated = true;
                }
                bestKeys.add(spectrumKey);
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

                for (Integer confidentSite : psmScores.getConfidentSites()) {
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
                            ModificationMatch newMatch = new ModificationMatch(ptmName, true, confidentSite);
                            newMatch.setConfident(true);
                            ArrayList<ModificationMatch> newPtmMatches = newMatches.get(ptmMass);
                            if (newPtmMatches == null) {
                                newPtmMatches = new ArrayList<ModificationMatch>();
                                newMatches.put(ptmMass, newPtmMatches);
                            }
                            newPtmMatches.add(newMatch);
                            if (newPtmMatches.size() > variableModifications.get(ptmMass)) {
                                throw new IllegalArgumentException("More sites than PTMs on peptide " + peptideMatch.getKey() + " for PTM of mass " + ptmMass + ".");
                            }
                            ArrayList<Integer> ptmInferredSites = inferredSites.get(ptmMass);
                            if (ptmInferredSites != null) {
                                ptmInferredSites.remove(confidentSite);
                                if (ptmInferredSites.isEmpty()) {
                                    inferredSites.remove(ptmMass);
                                }
                            }
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
                        int mappingSize = ambiguousMappingAtSite.size();
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
                                    HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> pScoreMap = ambiguousSites.get(probabilisticScore);
                                    if (pScoreMap == null) {
                                        pScoreMap = new HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>(mappingSize);
                                        ambiguousSites.put(probabilisticScore, pScoreMap);
                                    }
                                    HashMap<Double, HashMap<Integer, ArrayList<String>>> dScoreMap = pScoreMap.get(dScore);
                                    if (dScoreMap == null) {
                                        dScoreMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(mappingSize);
                                        pScoreMap.put(dScore, dScoreMap);
                                    }
                                    HashMap<Integer, ArrayList<String>> ptmMap = dScoreMap.get(ptmMass);
                                    if (ptmMap == null) {
                                        ptmMap = new HashMap<Integer, ArrayList<String>>(1);
                                        dScoreMap.put(ptmMass, ptmMap);
                                    }
                                    ArrayList<String> modifications = ptmMap.get(site);
                                    if (modifications == null) {
                                        modifications = new ArrayList<String>(1);
                                        ptmMap.put(site, modifications);
                                    }
                                    if (!modifications.contains(ptmName)) {
                                        modifications.add(ptmName);
                                    }
                                }
                            }
                        }
                    }
                }

                HashMap<Double, Integer> nRepresentativesMap = new HashMap<Double, Integer>();
                for (double ptmMass : variableModifications.keySet()) {
                    int nPtm = variableModifications.get(ptmMass);
                    int nConfident = 0;
                    ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                    if (ptmConfidentSites != null) {
                        nConfident = ptmConfidentSites.size();
                    }
                    if (nConfident < nPtm) {
                        int nRepresentatives = nPtm - nConfident;
                        if (nRepresentatives > 0) {
                            nRepresentativesMap.put(ptmMass, nRepresentatives);
                        }
                    }
                }

                HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> representativeToSecondaryMap = getRepresentativeToSecondaryMap(ambiguousSites, nRepresentativesMap, inferredSites);

                for (Double ptmMass : representativeToSecondaryMap.keySet()) {
                    HashMap<Integer, HashMap<Integer, ArrayList<String>>> representativesAtMass = representativeToSecondaryMap.get(ptmMass);
                    ArrayList<Integer> ptmInferredSites = inferredSites.get(ptmMass);
                    for (int representativeSite : representativesAtMass.keySet()) {
                        HashMap<Integer, ArrayList<String>> siteToPtmMap = representativesAtMass.get(representativeSite);
                        peptideScores.addAmbiguousModificationSites(representativeSite, siteToPtmMap);
                        for (String ptmName : siteToPtmMap.get(representativeSite)) {
                            ModificationMatch newMatch = new ModificationMatch(ptmName, true, representativeSite);
                            newMatch.setConfident(false);
                            if (ptmInferredSites != null && ptmInferredSites.contains(representativeSite)) {
                                newMatch.setInferred(true);
                            }
                            ArrayList<ModificationMatch> newPtmMatches = newMatches.get(ptmMass);
                            if (newPtmMatches == null) {
                                newPtmMatches = new ArrayList<ModificationMatch>();
                                newMatches.put(ptmMass, newPtmMatches);
                            }
                            newPtmMatches.add(newMatch);
                            if (newPtmMatches.size() > variableModifications.get(ptmMass)) {
                                throw new IllegalArgumentException("More sites than PTMs on peptide " + peptideMatch.getKey() + " for PTM of mass " + ptmMass + ".");
                            }
                        }
                    }
                }
            }

            for (ArrayList<ModificationMatch> modificationMatches : newMatches.values()) {
                newModificationMatches.addAll(modificationMatches);
            }
            peptide.setModificationMatches(newModificationMatches);

            peptideMatch.addUrParam(peptideScores);

            String newKey = peptide.getMatchingKey(sequenceMatchingPreferences);
            if (!newKey.equals(originalKey)) {
                if (identification.getPeptideIdentification().contains(newKey)) {
                    throw new IllegalArgumentException("Attempting to create duplicate peptide key: " + newKey + ".");
                }
                identification.updatePeptideMatch(originalKey, newKey, peptideMatch);
            } else {
                identification.updatePeptideMatch(peptideMatch);
            }
        }
    }

    /**
     * Returns a representative to secondary sites map (representative site &gt;
     * secondary site &gt; list of PTM names) based on an ambiguous sites scores
     * map (probabilistic score &gt; delta score &gt; site &gt; list of PTM
     * names).
     *
     * @param ambiguousSitesScores a map of the ambiguous sites scores
     * @param nRepresentatives the number of representative sites allowed
     *
     * @return a representative to secondary sites map
     */
    private HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> getRepresentativeToSecondaryMap(HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousScoreToSiteMap, HashMap<Double, Integer> nRepresentatives) {
        return getRepresentativeToSecondaryMap(ambiguousScoreToSiteMap, nRepresentatives, null);
    }

    /**
     * Returns a representative to secondary sites map (representative site &gt;
     * secondary site &gt; list of PTM names) based on an ambiguous sites scores
     * map (probabilistic score &gt; delta score &gt; site &gt; list of PTM
     * names).
     *
     * @param ambiguousSitesScores a map of the ambiguous sites scores
     * @param nRepresentatives the number of representative sites allowed
     * @param preferentialSites preferential sites to be selected as
     * representative
     *
     * @return a representative to secondary sites map
     */
    private HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> getRepresentativeToSecondaryMap(HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousScoreToSiteMap, HashMap<Double, Integer> nRepresentativesMap, HashMap<Double, ArrayList<Integer>> preferentialSites) {

        int nMasses = nRepresentativesMap.size();
        HashMap<Double, Integer> nToSelectMap = new HashMap<Double, Integer>(nMasses);
        HashMap<Double, Integer> nSelectedMap = new HashMap<Double, Integer>(nMasses);
        for (Double ptmMass : nRepresentativesMap.keySet()) {
            int nRepresentatives = nRepresentativesMap.get(ptmMass);
            int nPreferential = 0;
            if (preferentialSites != null) {
                ArrayList<Integer> sites = preferentialSites.get(ptmMass);
                if (sites != null) {
                    nPreferential = sites.size();
                }
            }
            int toSelect = Math.max(nRepresentatives - nPreferential, 0);
            nToSelectMap.put(ptmMass, toSelect);
            nSelectedMap.put(ptmMass, 0);
        }

        HashMap<Double, HashSet<Integer>> possibleSites = new HashMap<Double, HashSet<Integer>>(nMasses);
        for (HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> mapAtPscore : ambiguousScoreToSiteMap.values()) {
            for (HashMap<Double, HashMap<Integer, ArrayList<String>>> mapAtDScore : mapAtPscore.values()) {
                for (Double ptmMass : mapAtDScore.keySet()) {
                    HashSet<Integer> ptmSites = possibleSites.get(ptmMass);
                    if (ptmSites == null) {
                        ptmSites = new HashSet<Integer>();
                        possibleSites.put(ptmMass, ptmSites);
                    }
                    Set<Integer> sitesAtScore = mapAtDScore.get(ptmMass).keySet();
                    ptmSites.addAll(sitesAtScore);
                }
            }
        }

        HashMap<Double, HashMap<Integer, ArrayList<String>>> representativeSites = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(nMasses);
        HashMap<Double, HashMap<Integer, ArrayList<String>>> secondarySites = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(nMasses);
        ArrayList<Double> pScores = new ArrayList<Double>(ambiguousScoreToSiteMap.keySet());
        if (pScores.size() > 1) {
            Collections.sort(pScores, Collections.reverseOrder());
        }
        for (double pScore : pScores) {
            HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> dScoresMap = ambiguousScoreToSiteMap.get(pScore);
            ArrayList<Double> dScores = new ArrayList<Double>(dScoresMap.keySet());
            if (dScores.size() > 1) {
                Collections.sort(dScores, Collections.reverseOrder());
            }
            for (double dScore : dScores) {
                HashMap<Double, HashMap<Integer, ArrayList<String>>> ptmMap = dScoresMap.get(dScore);
                ArrayList<Double> ptmMasses = new ArrayList<Double>(ptmMap.keySet());
                if (ptmMasses.size() > 1) {
                    Collections.sort(ptmMasses);
                }
                for (double ptmMass : ptmMasses) {
                    ArrayList<Integer> preferentialSitesForPtm = null;
                    if (preferentialSites != null) {
                        preferentialSitesForPtm = preferentialSites.get(ptmMass);
                    }
                    Integer toSelect = nToSelectMap.get(ptmMass);
                    if (toSelect == null) {
                        toSelect = 0;
                    }
                    Integer nSelected = nSelectedMap.get(ptmMass);
                    if (nSelected == null) {
                        nSelected = 0;
                    }
                    int nNeeded = toSelect - nSelected;
                    if (nNeeded > 0) {
                        HashSet<Integer> ptmSites = possibleSites.get(ptmMass);
                        if (ptmSites == null || ptmSites.size() < nNeeded) {
                            throw new IllegalArgumentException("Not enough sites (" + possibleSites.size() + " where " + nNeeded + " needed) found for ptm mass " + ptmMass + ".");
                        }
                    }

                    HashMap<Integer, ArrayList<String>> siteMap = ptmMap.get(ptmMass);
                    ArrayList<Integer> sites = new ArrayList<Integer>(siteMap.keySet());
                    if (sites.size() > 1) {
                        Collections.sort(sites);
                    }
                    for (int site : sites) {
                        boolean referenceOtherPtm = false;
                        for (Double tempMass : representativeSites.keySet()) {
                            if (representativeSites.get(tempMass).keySet().contains(site)) {
                                referenceOtherPtm = true;
                                break;
                            }
                        }
                        boolean preferentialForPtm;
                        boolean preferentialSiteOtherPtm = false;
                        preferentialForPtm = preferentialSitesForPtm != null && preferentialSitesForPtm.contains(site);
                        if (!preferentialSiteOtherPtm && preferentialSites != null) {
                            for (Double tempMass : preferentialSites.keySet()) {
                                if (tempMass != ptmMass) {
                                    ArrayList<Integer> preferentialSitesOtherPtm = preferentialSites.get(ptmMass);
                                    if (preferentialSitesOtherPtm != null && preferentialSitesOtherPtm.contains(site)) {
                                        preferentialSiteOtherPtm = true;
                                        break;
                                    }
                                }
                            }
                        }
                        boolean blockingOtherPtm = false;
                        HashSet<Double> tempMasses = new HashSet<Double>(possibleSites.keySet());
                        for (Double tempMass : tempMasses) {
                            if (tempMass != ptmMass) {
                                Integer tempToSelect = nToSelectMap.get(tempMass);
                                Integer tempSelected = nSelectedMap.get(tempMass);
                                if (tempToSelect != null && tempSelected != null) {
                                    int toMapForPtm = tempToSelect - tempSelected;
                                    if (toMapForPtm > 0) {
                                        HashSet<Integer> tempSites = possibleSites.get(tempMass);
                                        if (tempSites == null) {
                                            throw new IllegalArgumentException("No sites found for PTM of mass " + ptmMass + ".");
                                        }
                                        if (tempSites.size() == toMapForPtm && tempSites.contains(site)) {
                                            blockingOtherPtm = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (!referenceOtherPtm && !blockingOtherPtm
                                && (preferentialForPtm || !preferentialSiteOtherPtm && nSelected < toSelect)) {
                            HashMap<Integer, ArrayList<String>> representativeSitesForPtmMass = representativeSites.get(ptmMass);
                            if (representativeSitesForPtmMass == null) {
                                representativeSitesForPtmMass = new HashMap<Integer, ArrayList<String>>(nRepresentativesMap.get(ptmMass));
                                representativeSites.put(ptmMass, representativeSitesForPtmMass);
                            }
                            for (String ptmName : siteMap.get(site)) {
                                ArrayList<String> modifications = representativeSitesForPtmMass.get(site);
                                if (modifications == null) {
                                    modifications = new ArrayList<String>(1);
                                    representativeSitesForPtmMass.put(site, modifications);
                                    if (!preferentialForPtm) {
                                        nSelected++;
                                    }
                                }
                                if (!modifications.contains(ptmName)) {
                                    modifications.add(ptmName);
                                }
                            }
                            for (Double tempMass : tempMasses) {
                                HashSet<Integer> tempSites = possibleSites.get(tempMass);
                                tempSites.remove(site);
                                if (tempSites.isEmpty()) {
                                    possibleSites.remove(tempMass);
                                }
                            }
                        } else {
                            HashMap<Integer, ArrayList<String>> secondarySitesForPtmMass = secondarySites.get(ptmMass);
                            if (secondarySitesForPtmMass == null) {
                                int size = 1;
                                HashSet<Integer> ptmSites = possibleSites.get(ptmMass);
                                if (ptmSites != null) {
                                    size = ptmSites.size() - toSelect + nSelected;
                                }
                                secondarySitesForPtmMass = new HashMap<Integer, ArrayList<String>>(size);
                                secondarySites.put(ptmMass, siteMap);
                            }
                            for (String ptmName : siteMap.get(site)) {
                                ArrayList<String> modifications = secondarySitesForPtmMass.get(site);
                                if (modifications == null) {
                                    modifications = new ArrayList<String>(1);
                                    secondarySitesForPtmMass.put(site, modifications);
                                }
                                if (!modifications.contains(ptmName)) {
                                    modifications.add(ptmName);
                                }
                            }
                        }
                    }
                    nSelectedMap.put(ptmMass, nSelected);
                }
            }
        }
        for (double ptmMass : nToSelectMap.keySet()) {
            Integer nSelected = nSelectedMap.get(ptmMass);
            Integer nToSelect = nToSelectMap.get(ptmMass);
            if (nSelected == null || nSelected < nToSelect) {
                throw new IllegalArgumentException("Not enough representative ptm sites found.");
            } else if (nSelected > nToSelect) {
                throw new IllegalArgumentException("Selected more representative sites than necessary.");
            }
        }
        HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> representativeToSecondaryMap = new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>(representativeSites.size());
        for (Double ptmMass : representativeSites.keySet()) {
            HashMap<Integer, HashMap<Integer, ArrayList<String>>> mapAtMass = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
            representativeToSecondaryMap.put(ptmMass, mapAtMass);
            HashMap<Integer, ArrayList<String>> representativeSitesAtMass = representativeSites.get(ptmMass);
            for (int representativeSite : representativeSitesAtMass.keySet()) {
                HashMap<Integer, ArrayList<String>> siteMap = new HashMap<Integer, ArrayList<String>>();
                siteMap.put(representativeSite, representativeSitesAtMass.get(representativeSite));
                mapAtMass.put(representativeSite, siteMap);
            }
            HashMap<Integer, ArrayList<String>> secondarySitesAtMass = secondarySites.get(ptmMass);
            if (secondarySitesAtMass != null) {
                for (int secondarySite : secondarySitesAtMass.keySet()) {
                    Integer distance = null;
                    Integer representativeSite = null;
                    for (int tempRepresentativeSite : representativeSitesAtMass.keySet()) {
                        int tempDistance = Math.abs(secondarySite - tempRepresentativeSite);
                        if (representativeSite == null || tempDistance < distance || tempDistance == distance && tempRepresentativeSite < representativeSite) {
                            representativeSite = tempRepresentativeSite;
                            distance = tempDistance;
                        }
                    }
                    HashMap<Integer, ArrayList<String>> representativeSiteMap = mapAtMass.get(representativeSite);
                    representativeSiteMap.put(secondarySite, secondarySitesAtMass.get(secondarySite));
                }
            }

        }
        return representativeToSecondaryMap;
    }

    /**
     * Scores PTMs in a protein match.
     *
     * @param identification identification object containing the identification
     * matches
     * @param proteinMatch the protein match
     * @param identificationParameters the identification parameters
     * @param scorePeptides boolean indicating whether peptides should be scored
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserilalizing a match
     */
    public void scorePTMs(Identification identification, ProteinMatch proteinMatch, IdentificationParameters identificationParameters, boolean scorePeptides) throws Exception {

        PSParameter psParameter = new PSParameter();
        Protein protein = null;
        identification.loadPeptideMatches(proteinMatch.getPeptideMatchesKeys(), null);
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatchesKeys(), psParameter, null);

        HashMap<Integer, ArrayList<String>> confidentSites = new HashMap<Integer, ArrayList<String>>();
        HashMap<Integer, HashMap<Integer, ArrayList<String>>> ambiguousSites = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();

        ArrayList<String> peptideKeys = new ArrayList<String>(proteinMatch.getPeptideMatchesKeys());
        for (String peptideKey : peptideKeys) {
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
            if (psParameter.getMatchValidationLevel().isValidated() && Peptide.isModified(peptideKey)) {
                PeptideMatch peptideMath = identification.getPeptideMatch(peptideKey);
                String peptideSequence = Peptide.getSequence(peptideKey);
                if (peptideMath.getUrParam(new PSPtmScores()) == null || scorePeptides) {
                    scorePTMs(identification, peptideMath, identificationParameters);
                }
                PSPtmScores peptideScores = (PSPtmScores) peptideMath.getUrParam(new PSPtmScores());
                if (peptideScores != null) {

                    if (protein == null) {
                        protein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                    }
                    ArrayList<Integer> peptideStart = protein.getPeptideStart(peptideSequence,
                            identificationParameters.getSequenceMatchingPreferences());
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
     * @param exceptionHandler handler for exceptions
     * @param identificationParameters the identification parameters
     * @param metrics the dataset metrics
     * @param processingPreferences the processing preferences
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scorePsmPtms(Identification identification, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, IdentificationParameters identificationParameters,
            Metrics metrics, ProcessingPreferences processingPreferences) throws Exception {

        waitingHandler.setWaitingText("Scoring PSM PTMs. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ExecutorService pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());
        HashMap<String, ArrayList<String>> spectrumKeysMap = identification.getSpectrumIdentificationMap();
        if (metrics != null && metrics.getGroupedSpectrumKeys() != null) {
            spectrumKeysMap = metrics.getGroupedSpectrumKeys();
        }

        for (String spectrumFileName : identification.getSpectrumFiles()) {
            ArrayList<String> spectrumKeys = spectrumKeysMap.get(spectrumFileName);
            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, spectrumKeys, null, true);
            for (int i = 1; i <= processingPreferences.getnThreads() && !waitingHandler.isRunCanceled(); i++) {
                PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
                PsmPtmScorerRunnable runnable = new PsmPtmScorerRunnable(psmIterator, peptideSpectrumAnnotator, identification, identificationParameters, waitingHandler, exceptionHandler);
                pool.submit(runnable);
            }
            if (waitingHandler.isRunCanceled()) {
                pool.shutdownNow();
                return;
            }
        }
        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM PTM scoring timed out. Please contact the developers.");
        }
    }

    /**
     * Scores the PTMs of all peptide matches contained in an identification
     * object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param identificationParameters the identification parameters
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scorePeptidePtms(Identification identification, WaitingHandler waitingHandler, IdentificationParameters identificationParameters) throws Exception {

        waitingHandler.setWaitingText("Scoring Peptide PTMs. Please Wait...");

        int max = identification.getPeptideIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        identification.loadPeptideMatches(null);

        ArrayList<String> peptideKeys = new ArrayList<String>(identification.getPeptideIdentification());
        for (String peptideKey : peptideKeys) {
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            scorePTMs(identification, peptideMatch, identificationParameters);
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
     * @param shotgunProtocol information on the protocol used
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scoreProteinPtms(Identification identification, Metrics metrics, WaitingHandler waitingHandler, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences) throws Exception {

        waitingHandler.setWaitingText("Scoring Protein PTMs. Please Wait...");

        int max = identification.getProteinIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        // If needed, while we are iterating proteins, we will take the maximal spectrum counting value and number of validated proteins as well.
        int nValidatedProteins = 0;
        int nConfidentProteins = 0;
        double tempSpectrumCounting, maxSpectrumCounting = 0;
        Enzyme enzyme = shotgunProtocol.getEnzyme();
        int maxPepLength = identificationParameters.getIdFilter().getMaxPepLength();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters);

        while (proteinMatchesIterator.hasNext()) {
            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();

            scorePTMs(identification, proteinMatch, identificationParameters, false);

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
                            proteinKey, spectrumCountingPreferences, enzyme, maxPepLength, identificationParameters.getSequenceMatchingPreferences());
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
     * every spectrum.
     *
     * @param identification identification object containing the identification
     * matches
     * @param waitingHandler waiting handler displaying progress to the user
     * @param identificationParameters the identification parameters
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
    public void peptideInference(Identification identification, IdentificationParameters identificationParameters, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, IllegalArgumentException, InterruptedException {

        waitingHandler.setWaitingText("Peptide Inference. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        // PSMs with confidently localized PTMs in a map: PTM mass -> peptide sequence -> spectrum keys
        HashMap<Double, HashMap<String, ArrayList<String>>> confidentPeptideInference = new HashMap<Double, HashMap<String, ArrayList<String>>>();
        // PSMs with ambiguously localized PTMs in a map: File -> PTM mass -> spectrum keys
        HashMap<String, HashMap<Double, ArrayList<String>>> notConfidentPeptideInference = new HashMap<String, HashMap<Double, ArrayList<String>>>();

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
                        ptmSiteInference(spectrumMatch, identificationParameters);
                        boolean confident = true;
                        for (ModificationMatch modMatch : spectrumMatch.getBestPeptideAssumption().getPeptide().getModificationMatches()) {
                            if (modMatch.isVariable()) {
                                String modName = modMatch.getTheoreticPtm();
                                PTM ptm = ptmFactory.getPTM(modName);
                                double ptmMass = ptm.getMass();
                                if (ptm.getType() == PTM.MODAA) {
                                    if (!modMatch.isConfident()) {
                                        HashMap<Double, ArrayList<String>> fileMap = notConfidentPeptideInference.get(spectrumFileName);
                                        if (fileMap == null) {
                                            fileMap = new HashMap<Double, ArrayList<String>>();
                                            notConfidentPeptideInference.put(spectrumFileName, fileMap);
                                        }
                                        ArrayList<String> spectra = fileMap.get(ptmMass);
                                        if (spectra == null) {
                                            spectra = new ArrayList<String>();
                                            fileMap.put(ptmMass, spectra);
                                        }
                                        spectra.add(spectrumKey);
                                        confident = false;
                                    } else {
                                        HashMap<String, ArrayList<String>> modMap = confidentPeptideInference.get(ptmMass);
                                        if (modMap == null) {
                                            modMap = new HashMap<String, ArrayList<String>>();
                                            confidentPeptideInference.put(ptmMass, modMap);
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
            HashMap<Double, ArrayList<String>> peptidesOfFile = notConfidentPeptideInference.get(spectrumFile);

            for (Double ptmMass : peptidesOfFile.keySet()) {

                ArrayList<String> spectrumKeys = peptidesOfFile.get(ptmMass);

                identification.loadSpectrumMatches(spectrumKeys, null);

                for (String spectrumKey : spectrumKeys) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                    String sequence = peptide.getSequence();
                    String notConfidentKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                    int nMod = Peptide.getModificationCount(notConfidentKey, ptmMass);
                    ArrayList<Integer> tempLocalizations, oldLocalizations = Peptide.getNModificationLocalized(notConfidentKey, ptmMass);
                    ArrayList<Integer> newLocalizationCandidates = new ArrayList<Integer>();

                    HashMap<String, ArrayList<String>> ptmConfidentPeptides = confidentPeptideInference.get(ptmMass);

                    if (ptmConfidentPeptides != null) {

                        // See if we can explain this peptide by another already identified peptide with the same number of modifications (the two peptides will be merged)
                        ArrayList<String> keys = ptmConfidentPeptides.get(sequence);

                        if (keys != null) {
                            for (String tempKey : keys) {
                                SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingPreferences);
                                if (Peptide.getModificationCount(secondaryKey, ptmMass) == nMod) {
                                    tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, ptmMass);
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
                                    String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingPreferences);
                                    tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, ptmMass);
                                    for (int localization : tempLocalizations) {
                                        if (!oldLocalizations.contains(localization) && !newLocalizationCandidates.contains(localization)) {
                                            newLocalizationCandidates.add(localization);
                                        }
                                    }
                                }
                            }
                        }
                        if (oldLocalizations.size() + newLocalizationCandidates.size() < nMod) {
                            // There are still unexplained sites, let's see if we find a related peptide which can help.
                            HashMap<String, ArrayList<String>> confidentAtMass = confidentPeptideInference.get(ptmMass);
                            for (String otherSequence : confidentAtMass.keySet()) {
                                if (!sequence.equals(otherSequence) && sequence.contains(otherSequence)) {
                                    for (String tempKey : confidentAtMass.get(otherSequence)) {
                                        SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                        String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingPreferences);
                                        tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, ptmMass);
                                        int tempIndex, ref = 0;
                                        String tempSequence = sequence;
                                        while ((tempIndex = tempSequence.indexOf(otherSequence)) >= 0) {
                                            ref += tempIndex;
                                            for (int localization : tempLocalizations) {
                                                int shiftedLocalization = ref + localization;
                                                if (!oldLocalizations.contains(shiftedLocalization) && !newLocalizationCandidates.contains(shiftedLocalization)) {
                                                    boolean candidatePtm = false;
                                                    for (String ptmName : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                                        if (ptm.getType() == PTM.MODAA && ptm.getMass() == ptm.getMass() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(shiftedLocalization)) {
                                                            candidatePtm = true;
                                                            break;
                                                        }
                                                    }
                                                    if (candidatePtm) {
                                                        newLocalizationCandidates.add(shiftedLocalization);
                                                    }
                                                }
                                            }
                                            tempSequence = tempSequence.substring(tempIndex + 1);
                                            ref++;
                                        }
                                    }
                                } else if (!sequence.equals(otherSequence) && otherSequence.contains(sequence)) {
                                    for (String tempKey : confidentAtMass.get(otherSequence)) {
                                        SpectrumMatch secondaryMatch = identification.getSpectrumMatch(tempKey);
                                        String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingPreferences);
                                        tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, ptmMass);
                                        int tempIndex, ref = 0;
                                        String tempSequence = otherSequence;
                                        while ((tempIndex = tempSequence.indexOf(sequence)) >= 0) {
                                            ref += tempIndex;
                                            for (int localization : tempLocalizations) {
                                                int shiftedLocalization = localization - ref;
                                                if (shiftedLocalization > 0 && shiftedLocalization <= sequence.length()
                                                        && !oldLocalizations.contains(shiftedLocalization) && !newLocalizationCandidates.contains(shiftedLocalization)) {
                                                    boolean candidatePtm = false;
                                                    for (String ptmName : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                                        if (ptm.getType() == PTM.MODAA && ptm.getMass() == ptm.getMass() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(shiftedLocalization)) {
                                                            candidatePtm = true;
                                                            break;
                                                        }
                                                    }
                                                    if (candidatePtm) {
                                                        newLocalizationCandidates.add(shiftedLocalization);
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
                        // Map the most likely inferred sites
                        if (!newLocalizationCandidates.isEmpty()) {
                            HashMap<Integer, ModificationMatch> nonConfidentMatches = new HashMap<Integer, ModificationMatch>();
                            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                String ptmName = modificationMatch.getTheoreticPtm();
                                PTM ptm = ptmFactory.getPTM(ptmName);
                                if (ptm.getMass() == ptmMass && !modificationMatch.isConfident()) {
                                    nonConfidentMatches.put(modificationMatch.getModificationSite(), modificationMatch);
                                }
                            }
                            HashMap<Integer, Integer> mapping = PtmSiteMapping.align(nonConfidentMatches.keySet(), newLocalizationCandidates);
                            for (Integer oldLocalization : mapping.keySet()) {
                                ModificationMatch modificationMatch = nonConfidentMatches.get(oldLocalization);
                                Integer newLocalization = mapping.get(oldLocalization);
                                if (modificationMatch == null) {
                                    throw new IllegalArgumentException("No modification match found at site " + oldLocalization + " in spectrum " + spectrumKey + ".");
                                }
                                if (newLocalization != null) {
                                    if (newLocalization.equals(oldLocalization)) {
                                        PTM ptmCandidate = null;
                                        for (String ptmName : searchParameters.getModificationProfile().getAllNotFixedModifications()) {
                                            PTM ptm = ptmFactory.getPTM(ptmName);
                                            if (ptm.getType() == PTM.MODAA && ptm.getMass() == ptm.getMass() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences).contains(newLocalization)) {
                                                ptmCandidate = ptm;
                                                break;
                                            }
                                        }
                                        if (ptmCandidate == null) {
                                            throw new IllegalArgumentException("No PTM found for site " + newLocalization + " on  peptide " + peptide.getSequence() + " in spectrum " + spectrumKey + ".");
                                        }
                                        modificationMatch.setModificationSite(newLocalization);
                                        PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                        psmScores.changeRepresentativeSite(ptmCandidate.getName(), oldLocalization, newLocalization);
                                    }
                                    modificationMatch.setInferred(true);
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
     * @param identificationParameters the identification parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading a protein sequence
     * @throws IllegalArgumentException exception thrown whenever an error
     * occurred while reading a protein sequence
     * @throws InterruptedException exception thrown whenever an error occurred
     * while reading a protein sequence
     */
    private void ptmSiteInference(SpectrumMatch spectrumMatch, IdentificationParameters identificationParameters)
            throws IOException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
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
                        for (int pos : psPeptide.getPotentialModificationSites(similarPtm, identificationParameters.getSequenceMatchingPreferences())) {
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

        PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
        Set<Double> ptmMasses = modMatchesMap.keySet();
        HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousScoreToSiteMap = new HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>>(ptmMasses.size()); // p score -> d-score -> Map PTM mass -> site -> list of modifications
        HashMap<Double, Integer> nRepresentativesMap = new HashMap<Double, Integer>();
        for (double ptmMass : ptmMasses) {
            ArrayList<ModificationMatch> ptmMatches = modMatchesMap.get(ptmMass);
            HashMap<Integer, String> ptmPotentialSites = possiblePositions.get(ptmMass);
            HashMap<Integer, String> confidentSites = new HashMap<Integer, String>();
            int nPTMs = ptmMatches.size(), nPotentialSites = ptmPotentialSites.size();
            if (nPotentialSites < nPTMs) {
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
                                HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> pScoreAmbiguousMap = ambiguousScoreToSiteMap.get(pScore);
                                if (pScoreAmbiguousMap == null) {
                                    pScoreAmbiguousMap = new HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>(1);
                                    ambiguousScoreToSiteMap.put(pScore, pScoreAmbiguousMap);
                                }
                                HashMap<Double, HashMap<Integer, ArrayList<String>>> dScoreAmbiguousMap = pScoreAmbiguousMap.get(dScore);
                                if (dScoreAmbiguousMap == null) {
                                    dScoreAmbiguousMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(nPotentialSites);
                                    pScoreAmbiguousMap.put(dScore, dScoreAmbiguousMap);
                                }
                                HashMap<Integer, ArrayList<String>> massAmbiguousMap = dScoreAmbiguousMap.get(ptmMass);
                                if (massAmbiguousMap == null) {
                                    massAmbiguousMap = new HashMap<Integer, ArrayList<String>>(nPotentialSites);
                                    dScoreAmbiguousMap.put(ptmMass, massAmbiguousMap);
                                }
                                ArrayList<String> modifications = massAmbiguousMap.get(site);
                                if (modifications == null) {
                                    modifications = new ArrayList<String>(1);
                                    massAmbiguousMap.put(site, modifications);
                                }
                                modifications.add(modName);
                            }
                        }
                    }
                }
            } else {
                HashMap<Double, HashMap<Double, ArrayList<Integer>>> scoreToSiteMap = new HashMap<Double, HashMap<Double, ArrayList<Integer>>>(nPotentialSites);
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
                        pScoreMap = new HashMap<Double, ArrayList<Integer>>(1);
                        scoreToSiteMap.put(pScore, pScoreMap);
                    }
                    ArrayList<Integer> dScoreSites = pScoreMap.get(dScore);
                    if (dScoreSites == null) {
                        dScoreSites = new ArrayList<Integer>(1);
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
                                HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>> pScoreAmbiguousMap = ambiguousScoreToSiteMap.get(pScore);
                                if (pScoreAmbiguousMap == null) {
                                    pScoreAmbiguousMap = new HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>(nPotentialSites);
                                    ambiguousScoreToSiteMap.put(pScore, pScoreAmbiguousMap);
                                }
                                HashMap<Double, HashMap<Integer, ArrayList<String>>> dScoreAmbiguousMap = pScoreAmbiguousMap.get(dScore);
                                if (dScoreAmbiguousMap == null) {
                                    dScoreAmbiguousMap = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(1);
                                    pScoreAmbiguousMap.put(dScore, dScoreAmbiguousMap);
                                }
                                HashMap<Integer, ArrayList<String>> massAmbiguousMap = dScoreAmbiguousMap.get(ptmMass);
                                if (massAmbiguousMap == null) {
                                    massAmbiguousMap = new HashMap<Integer, ArrayList<String>>(nPotentialSites);
                                    dScoreAmbiguousMap.put(ptmMass, massAmbiguousMap);
                                }
                                ArrayList<String> modifications = massAmbiguousMap.get(site);
                                if (modifications == null) {
                                    modifications = new ArrayList<String>(1);
                                    massAmbiguousMap.put(site, modifications);
                                }
                                modifications.add(modName);

                            }
                            cpt++;
                        }
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
                if (nRepresentatives > 0) {
                    nRepresentativesMap.put(ptmMass, nRepresentatives);
                }
            }
        }
        if (!nRepresentativesMap.isEmpty()) {
            HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> representativeToSecondaryMap = getRepresentativeToSecondaryMap(ambiguousScoreToSiteMap, nRepresentativesMap);
            for (Double ptmMass : representativeToSecondaryMap.keySet()) {
                HashMap<Integer, HashMap<Integer, ArrayList<String>>> massMap = representativeToSecondaryMap.get(ptmMass);
                for (int representativeSite : massMap.keySet()) {
                    ptmScores.addAmbiguousModificationSites(representativeSite, massMap.get(representativeSite));
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

    /**
     * Runnable scoring PSM PTMs.
     *
     * @author Marc Vaudel
     */
    private class PsmPtmScorerRunnable implements Runnable {

        /**
         * An iterator for the PSMs.
         */
        private PsmIterator psmIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The identification parameters.
         */
        private IdentificationParameters identificationParameters;
        /**
         * The waiting handler.
         */
        private WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private ExceptionHandler exceptionHandler;
        /**
         * The peptide spectrum annotator.
         */
        private PeptideSpectrumAnnotator peptideSpectrumAnnotator;

        /**
         * Constructor.
         *
         * @param psmIterator a PSM iterator
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public PsmPtmScorerRunnable(PsmIterator psmIterator, PeptideSpectrumAnnotator peptideSpectrumAnnotator, Identification identification,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.psmIterator = psmIterator;
            this.identification = identification;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.peptideSpectrumAnnotator = peptideSpectrumAnnotator;
        }

        @Override
        public void run() {
            try {
                while (psmIterator.hasNext() && !waitingHandler.isRunCanceled()) {
                    SpectrumMatch spectrumMatch = psmIterator.next();
                    if (spectrumMatch != null && spectrumMatch.getBestPeptideAssumption() != null) {
                        scorePTMs(identification, spectrumMatch, identificationParameters, waitingHandler, peptideSpectrumAnnotator);
                    }
                }
            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }
    }
}
