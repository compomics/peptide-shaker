package eu.isas.peptideshaker.ptm;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PsmPTMMap;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class localizes the PTMs based on PTM localization scores
 *
 * @author Marc
 */
public class PtmLocalizer {
    
    /**
     * The PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    

    /**
     * Infers the PTM localization and its confidence for the best match of
     * every spectrum
     *
     * @param identification identification object containing the identification matches
     * @param ptmScorer the PTM scorer used to score PTM sites
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
    public void peptideInference(Identification identification, PtmScorer ptmScorer, PTMScoringPreferences ptmScoringPreferences, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler)
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
                        ptmInference(spectrumMatch, ptmScorer, ptmScoringPreferences, searchParameters, sequenceMatchingPreferences);
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
    private void ptmInference(SpectrumMatch spectrumMatch, PtmScorer ptmScorer, PTMScoringPreferences ptmScoringPreferences, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences)
            throws IOException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        PsmPTMMap psmPTMMap = ptmScorer.getPsmPTMMap();
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
            HashMap<Double, ArrayList<Integer>> scoreToAmbiguousSitesMap = new HashMap<Double, ArrayList<Integer>>();
            HashMap<Integer, ArrayList<String>> temporaryAmbiguousSites = new HashMap<Integer, ArrayList<String>>();
            HashMap<String, ArrayList<Integer>> confidentSites = new HashMap<String, ArrayList<Integer>>();
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
                    ArrayList<Integer> ptmConfidentSites = confidentSites.get(modName);
                    if (ptmConfidentSites == null) {
                        ptmConfidentSites = new ArrayList<Integer>();
                        confidentSites.put(modName, ptmConfidentSites);
                    }
                    ptmConfidentSites.add(site);
                }
            } else if (!ptmScoringPreferences.isProbabilitsticScoreCalculation()
                    || ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore && ptmMatches.size() > 1) {
                for (ModificationMatch modificationMatch : ptmMatches) {
                    String modName = modificationMatch.getTheoreticPtm();
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    ArrayList<Integer> orderedDSites = new ArrayList<Integer>(ptmScoring.getDSites());
                    Collections.sort(orderedDSites);
                    for (int site : orderedDSites) {
                        if (site == modificationMatch.getModificationSite()) {
                            double score = ptmScoring.getDeltaScore(site);
                            if (score == 0) {
                                ptmScoring.setSiteConfidence(site, PtmScoring.RANDOM);
                                modificationMatch.setConfident(false);
                            } else if (score <= 95) {
                                ptmScoring.setSiteConfidence(site, PtmScoring.DOUBTFUL);
                                modificationMatch.setConfident(false);
                            } else {
                                ptmScoring.setSiteConfidence(site, PtmScoring.CONFIDENT);
                                modificationMatch.setConfident(true);
                                ArrayList<Integer> ptmConfidentSites = confidentSites.get(modName);
                                if (ptmConfidentSites == null) {
                                    ptmConfidentSites = new ArrayList<Integer>();
                                    confidentSites.put(modName, ptmConfidentSites);
                                }
                                ptmConfidentSites.add(site);
                            }
                            if (!modificationMatch.isConfident()) {
                                ArrayList<Integer> sitesAtScore = scoreToAmbiguousSitesMap.get(score);
                                if (sitesAtScore == null) {
                                    sitesAtScore = new ArrayList<Integer>();
                                    scoreToAmbiguousSitesMap.put(score, sitesAtScore);
                                }
                                sitesAtScore.add(site);
                                ArrayList<String> modificationsAtSite = temporaryAmbiguousSites.get(site);
                                if (modificationsAtSite == null) {
                                    modificationsAtSite = new ArrayList<String>();
                                    temporaryAmbiguousSites.put(site, modificationsAtSite);
                                }
                                modificationsAtSite.add(modName);
                            }
                        }
                    }
                }
            } else {
                HashMap<Double, ArrayList<Integer>> scoreToSiteMap = new HashMap<Double, ArrayList<Integer>>();
                for (int site : ptmPotentialSites.keySet()) {
                    String modName = ptmPotentialSites.get(site);
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                    double score = 0;
                    if (ptmScoring != null) {
                        score = ptmScoring.getProbabilisticScore(site);
                    }
                    ArrayList<Integer> sitesAtScore = scoreToSiteMap.get(score);
                    if (sitesAtScore == null) {
                        sitesAtScore = new ArrayList<Integer>();
                        scoreToSiteMap.put(score, sitesAtScore);
                    }
                    sitesAtScore.add(site);
                }
                ArrayList<Double> scores = new ArrayList<Double>(scoreToSiteMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
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
                for (Double score : scores) {
                    ArrayList<Integer> sites = scoreToSiteMap.get(score);
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
                        if (score <= randomThreshold || !enoughPtms) {
                            ptmScoring.setSiteConfidence(site, PtmScoring.RANDOM);
                            if (modificationMatch != null) {
                                modificationMatch.setConfident(false);
                            }
                        } else if (score <= doubtfulThreshold) {
                            ptmScoring.setSiteConfidence(site, PtmScoring.DOUBTFUL);
                            if (modificationMatch != null) {
                                modificationMatch.setConfident(false);
                            }
                        } else {
                            ptmScoring.setSiteConfidence(site, PtmScoring.VERY_CONFIDENT);
                            if (modificationMatch != null) {
                                modificationMatch.setConfident(false);
                            }
                        }
                        if (modificationMatch == null || !modificationMatch.isConfident()) {
                            ArrayList<Integer> sitesAtScore = scoreToAmbiguousSitesMap.get(score);
                            if (sitesAtScore == null) {
                                sitesAtScore = new ArrayList<Integer>();
                                scoreToAmbiguousSitesMap.put(score, sitesAtScore);
                            }
                            sitesAtScore.add(site);
                            ArrayList<String> modificationsAtSite = temporaryAmbiguousSites.get(site);
                            if (modificationsAtSite == null) {
                                modificationsAtSite = new ArrayList<String>();
                                temporaryAmbiguousSites.put(site, modificationsAtSite);
                            }
                            modificationsAtSite.add(modName);
                        }
                        cpt++;
                    }
                }
            }
            // Select the best scoring ambiguous sites as representative PTM sites
            ArrayList<Double> scores = new ArrayList<Double>(scoreToAmbiguousSitesMap.keySet());
            Collections.sort(scores, Collections.reverseOrder());
            ArrayList<Integer> ambiguousRepresentativeSites = new ArrayList<Integer>();
            for (double score : scores) {
                ArrayList<Integer> sites = scoreToAmbiguousSitesMap.get(score);
                Collections.sort(sites);
                for (Integer site : sites) {
                    ambiguousRepresentativeSites.add(site);
                    if (ambiguousRepresentativeSites.size() == nPTMs) {
                        break;
                    }
                }
                if (ambiguousRepresentativeSites.size() == nPTMs) {
                    break;
                }
            }
            // Map ambiguous sites to their closest representative site
            HashMap<Integer, HashMap<Integer, ArrayList<String>>> ambiguousSiteMap = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
            for (Integer site : temporaryAmbiguousSites.keySet()) {
                Integer distance = null;
                Integer representativeSite = null;
                for (int representativeSiteCandidate : ambiguousRepresentativeSites) {
                    if (representativeSite == null || Math.abs(site - representativeSiteCandidate) < distance) {
                        representativeSite = representativeSiteCandidate;
                        distance = Math.abs(site - representativeSiteCandidate);
                        if (distance == 0) {
                            break;
                        }
                    }
                }
                HashMap<Integer, ArrayList<String>> representativeSiteSites = ambiguousSiteMap.get(representativeSite);
                if (representativeSiteSites == null) {
                    representativeSiteSites = new HashMap<Integer, ArrayList<String>>();
                    ambiguousSiteMap.put(representativeSite, representativeSiteSites);
                }
                representativeSiteSites.put(site, temporaryAmbiguousSites.get(site));
            }
            // Store PTM site confidence in the PTMScores object
            for (String ptmName : confidentSites.keySet()) {
                for (int site : confidentSites.get(ptmName)) {
                    ptmScores.addConfidentModificationSite(ptmName, site);
                }
            }
            for (int representativeSite : ambiguousRepresentativeSites) {
                HashMap<Integer, ArrayList<String>> RepresentativeSiteGroups = ambiguousSiteMap.get(representativeSite);
                if (RepresentativeSiteGroups == null) {
                    throw new IllegalArgumentException("No ambiguous PTM mapping found for representative site " + representativeSite + ".");
                }
                ptmScores.addAmbiguousModificationSites(representativeSite, RepresentativeSiteGroups);
            }
        }
    }

}
