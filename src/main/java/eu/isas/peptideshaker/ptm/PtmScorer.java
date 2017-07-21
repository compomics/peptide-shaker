package eu.isas.peptideshaker.ptm;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.identification.ptm.ptmscores.AScore;
import com.compomics.util.experiment.identification.ptm.ptmscores.PhosphoRS;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.maps.PsmPTMMap;
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
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     */
    public void attachDeltaScore(Identification identification, SpectrumMatch spectrumMatch, SequenceMatchingPreferences sequenceMatchingPreferences) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        HashMap<String, ArrayList<Integer>> modificationProfiles = new HashMap<String, ArrayList<Integer>>();
        PSPtmScores ptmScores = new PSPtmScores();

        if (spectrumMatch.getUrParam(ptmScores) != null) {
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
        }

        PSParameter psParameter = new PSParameter();
        double p1 = 1;
        Peptide psPeptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = ((SpectrumMatch)identification.retrieveObject(spectrumMatch.getKey())).getAssumptionsMap();
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

        if (psPeptide.isModified()) {
            for (ModificationMatch modificationMatch : psPeptide.getModificationMatches()) {
                if (modificationMatch.getVariable()) {
                    String modificationName = modificationMatch.getTheoreticPtm();
                    if (!modifications.contains(modificationName)) {
                        modifications.add(modificationName);
                        modificationProfiles.put(modificationName, new ArrayList<Integer>());
                    }
                    modificationProfiles.get(modificationName).add(modificationMatch.getModificationSite());
                }
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

                                        Peptide peptide = peptideAssumption.getPeptide();
                                        if (peptide.isModified()) {
                                            for (ModificationMatch modMatch : peptide.getModificationMatches()) {

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
                    ptmScoring.setDeltaScore(modSite, deltaScore);
                }

                spectrumMatch.addUrParam(ptmScores);
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
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     * @throws org.apache.commons.math.MathException thrown whenever a math
     * error occurred while computing the PTM scores
     */
    private void attachProbabilisticScore(Identification identification, SpectrumMatch spectrumMatch, IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, InterruptedException, ClassNotFoundException, SQLException, MzMLUnmarshallerException, MathException {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
        PTMScoringPreferences scoringPreferences = identificationParameters.getPtmScoringPreferences();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences(),
                ptmSequenceMatchingPreferences = scoringPreferences.getSequenceMatchingPreferences();

        PtmSettings ptmProfile = searchParameters.getPtmSettings();

        PSPtmScores ptmScores = new PSPtmScores();
        if (spectrumMatch.getUrParam(ptmScores) != null) {
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
        }

        HashMap<Double, ArrayList<PTM>> modifications = new HashMap<Double, ArrayList<PTM>>();
        HashMap<Double, Integer> nMod = new HashMap<Double, Integer>();
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();
        Peptide peptide = bestPeptideAssumption.getPeptide();

        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                if (modificationMatch.getVariable()) {
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
                }
            }
        }

        if (!modifications.isEmpty()) {

            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey());
            SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), bestPeptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());

            for (Double ptmMass : modifications.keySet()) {
                HashMap<Integer, Double> scores = null;
                if (scoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore && nMod.get(ptmMass) == 1) {
                    scores = AScore.getAScore(peptide, modifications.get(ptmMass), spectrum, annotationPreferences, specificAnnotationPreferences,
                            scoringPreferences.isProbabilisticScoreNeutralLosses(), sequenceMatchingPreferences, ptmSequenceMatchingPreferences, peptideSpectrumAnnotator);
                    if (scores == null) {
                        throw new IllegalArgumentException("An error occurred while scoring spectrum " + spectrum.getSpectrumTitle() + "of file " + spectrum.getFileName() + " with the A-score."); // Most likely a compatibility issue with utilities
                    }
                } else if (scoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                    scores = PhosphoRS.getSequenceProbabilities(peptide, modifications.get(ptmMass), spectrum, annotationPreferences, specificAnnotationPreferences,
                            scoringPreferences.isProbabilisticScoreNeutralLosses(), sequenceMatchingPreferences,
                            ptmSequenceMatchingPreferences, peptideSpectrumAnnotator);
                    if (scores == null) {
                        throw new IllegalArgumentException("An error occurred while scoring spectrum " + spectrum.getSpectrumTitle() + "of file " + spectrum.getFileName() + " with PhosphoRS."); // Most likely a compatibility issue with utilities
                    }
                }
                if (scores != null) {
                    // remap to searched PTMs
                    PTM mappedModification = null;
                    String peptideSequence = peptide.getSequence();
                    for (int site : scores.keySet()) {
                        if (site == 0) {
                            // N-term ptm
                            for (PTM ptm : modifications.get(ptmMass)) {
                                if (ptm.isNTerm() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences, ptmSequenceMatchingPreferences).contains(1)) {
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
                                if (ptm.isCTerm() && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences, ptmSequenceMatchingPreferences).contains(peptideSequence.length())) {
                                    mappedModification = ptm;
                                    break;
                                }
                            }
                            if (mappedModification == null) {
                                throw new IllegalArgumentException("Could not map the PTM of mass " + ptmMass + " on the C-terminus of the peptide " + peptideSequence + ".");
                            }
                        } else {
                            for (PTM ptm : modifications.get(ptmMass)) {
                                if (peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences, ptmSequenceMatchingPreferences).contains(site)) {
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
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with a file
     * @throws SQLException thrown whenever an error occurred while interacting
     * with the database
     * @throws ClassNotFoundException thrown whenever an error occurred while
     * deserializing a match from the database
     * @throws InterruptedException thrown whenever a threading error occurred
     * while interacting with the database
     * @throws MzMLUnmarshallerException thrown whenever an error occurred while
     * reading an mzML file
     * @throws org.apache.commons.math.MathException thrown whenever a math
     * error occurred while computing the PTM scores
     */
    public void scorePTMs(Identification identification, SpectrumMatch spectrumMatch, IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler, PeptideSpectrumAnnotator peptideSpectrumAnnotator)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        attachDeltaScore(identification, spectrumMatch, sequenceMatchingPreferences);

        PTMScoringPreferences scoringPreferences = identificationParameters.getPtmScoringPreferences();

        if (scoringPreferences.isProbabilitsticScoreCalculation()) {
            attachProbabilisticScore(identification, spectrumMatch, identificationParameters, peptideSpectrumAnnotator);
        }

        PSPtmScores ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

        if (ptmScores != null) {

            Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

            ArrayList<Double> modificationMasses = new ArrayList<Double>(peptide.getNModifications());
            if (peptide.isModified()) {
                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                    if (modificationMatch.getVariable()) {
                        PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                        if (!modificationMasses.contains(ptm.getMass())) {
                            modificationMasses.add(ptm.getMass());
                        }
                    }
                }
            }

            for (double ptmMass : modificationMasses) {

                int nPtm = peptide.getNVariableModifications(ptmMass);
                HashMap<Double, ArrayList<Integer>> dSitesMap = new HashMap<Double, ArrayList<Integer>>(nPtm);
                HashMap<Double, ArrayList<Integer>> pSitesMap = new HashMap<Double, ArrayList<Integer>>(nPtm);
                HashMap<Integer, Double> pScores = new HashMap<Integer, Double>(nPtm);

                for (String modification : ptmScores.getScoredPTMs()) {

                    PTM ptm = ptmFactory.getPTM(modification);

                    if (ptm.getMass() == ptmMass) {

                        PtmScoring ptmScoring = ptmScores.getPtmScoring(modification);

                        for (int site : ptmScoring.getDSites()) {

                            double score = ptmScoring.getDeltaScore(site);
                            ArrayList<Integer> sites = dSitesMap.get(score);

                            if (sites == null) {
                                sites = new ArrayList<Integer>(nPtm);
                                dSitesMap.put(score, sites);
                            }

                            sites.add(site);
                        }

                        for (int site : ptmScoring.getProbabilisticSites()) {

                            double score = ptmScoring.getProbabilisticScore(site);
                            ArrayList<Integer> sites = pSitesMap.get(score);

                            if (sites == null) {
                                sites = new ArrayList<Integer>(nPtm);
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
     * @param identificationParameters the identification parameters
     * @param waitingHandler the waiting handler, can be null
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserializing a match
     */
    public void scorePTMs(Identification identification, PeptideMatch peptideMatch, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws Exception {

        Peptide peptide = peptideMatch.getTheoreticPeptide();
        String peptideSequence = peptide.getSequence();
        ArrayList<ModificationMatch> originalMatches = peptide.getModificationMatches();
        if (originalMatches == null) {
            return;
        }

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        PtmSettings ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();

        PSPtmScores peptideScores = new PSPtmScores();
        PSParameter psParameter = new PSParameter();

        HashMap<Double, Integer> variableModifications = new HashMap<Double, Integer>(peptide.getNModifications());
        HashMap<Double, HashMap<Integer, ArrayList<String>>> inferredSites = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(peptide.getNModifications());
        PTM nTermPtmConfident = null;
        PTM cTermPtmConfident = null;

        String originalKey = peptide.getMatchingKey(sequenceMatchingPreferences);
        ArrayList<ModificationMatch> newModificationMatches = new ArrayList<ModificationMatch>(originalMatches.size());

        for (ModificationMatch modificationMatch : originalMatches) {
            if (modificationMatch.getVariable()) {
                String modName = modificationMatch.getTheoreticPtm();
                PTM ptm = ptmFactory.getPTM(modName);
                double ptmMass = ptm.getMass();
                boolean maybeNotTerminal = ptm.getType() == PTM.MODAA;
                if (!maybeNotTerminal) {
                    for (String otherPtmName : ptmSettings.getAllNotFixedModifications()) {
                        if (!otherPtmName.equals(modName)) {
                            PTM ptm2 = ptmFactory.getPTM(otherPtmName);
                            if (ptm2.getMass() == ptmMass && ptm.getType() != ptm2.getType()) {
                                maybeNotTerminal = true;
                                break;
                            }
                        }
                    }
                }
                if (maybeNotTerminal) {
                    Integer nPtm = variableModifications.get(ptmMass);
                    if (nPtm == null) {
                        variableModifications.put(ptmMass, 1);
                    } else {
                        variableModifications.put(ptmMass, nPtm + 1);
                    }
                    if (modificationMatch.getInferred()) {
                        Integer modificationSite;
                        if (ptm.isCTerm()) {
                            modificationSite = peptideSequence.length() + 1;
                        } else if (ptm.isNTerm()) {
                            modificationSite = 0;
                        } else {
                            modificationSite = modificationMatch.getModificationSite();
                        }
                        HashMap<Integer, ArrayList<String>> ptmInferredSites = inferredSites.get(ptmMass);
                        if (ptmInferredSites == null) {
                            ptmInferredSites = new HashMap<Integer, ArrayList<String>>(1);
                            inferredSites.put(ptmMass, ptmInferredSites);
                        }
                        ArrayList<String> modificationsAtSite = ptmInferredSites.get(modificationSite);
                        if (modificationsAtSite == null) {
                            modificationsAtSite = new ArrayList<String>(1);
                            ptmInferredSites.put(modificationSite, modificationsAtSite);
                        }
                        modificationsAtSite.add(modName);
                    }
                } else {
                    newModificationMatches.add(modificationMatch);
                    if (ptm.isCTerm()) {
                        if (cTermPtmConfident != null) {
                            throw new IllegalArgumentException("Multiple PTMs on termini not supported.");
                        }
                        cTermPtmConfident = ptmFactory.getPTM(modName);
                    } else if (ptm.isNTerm()) {
                        if (nTermPtmConfident != null) {
                            throw new IllegalArgumentException("Multiple PTMs on termini not supported.");
                        }
                        nTermPtmConfident = ptmFactory.getPTM(modName);
                    } else {
                        throw new IllegalArgumentException("Non-terminal PTM should be of type PTM.MODAA.");
                    }
                }
            } else {
                newModificationMatches.add(modificationMatch);
            }
        }

        if (variableModifications.isEmpty()) {
            return;
        }

        HashMap<Double, ArrayList<ModificationMatch>> newMatches = new HashMap<Double, ArrayList<ModificationMatch>>(variableModifications.size());

        HashMap<Double, ArrayList<Integer>> confidentSites = new HashMap<Double, ArrayList<Integer>>(variableModifications.size());

        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(new PSParameter());
        PsmIterator psmIterator = identification.getPsmIterator(peptideMatch.getSpectrumMatchesKeys(), waitingHandler);
        
        // Map confident sites
        while (psmIterator.hasNext()) {
            SpectrumMatch spectrumMatch = psmIterator.next();
            psParameter = (PSParameter)spectrumMatch.getUrParam(psParameter);
            PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

            for (String ptmName : psmScores.getScoredPTMs()) {
                PtmScoring psmScoring = psmScores.getPtmScoring(ptmName);
                PtmScoring peptideScoring = peptideScores.getPtmScoring(ptmName);
                if (peptideScoring == null) {
                    peptideScoring = new PtmScoring(ptmName);
                    peptideScores.addPtmScoring(ptmName, peptideScoring);
                }
                for (int site : psmScoring.getScoredSites()) {
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

            for (Integer refSite : psmScores.getConfidentSites()) {
                for (String ptmName : psmScores.getConfidentModificationsAt(refSite)) {
                    int site = refSite;
                    PTM ptm = ptmFactory.getPTM(ptmName);
                    Double ptmMass = ptm.getMass();
                    Integer occurrence = variableModifications.get(ptmMass);
                    if (occurrence != null) {
                        ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                        if (ptmConfidentSites == null) {
                            ptmConfidentSites = new ArrayList<Integer>(1);
                            confidentSites.put(ptmMass, ptmConfidentSites);
                        }
                        int nSitesOccupied = ptmConfidentSites.size();
                        if (nTermPtmConfident != null && ptm.getMass() == nTermPtmConfident.getMass()) {
                            nSitesOccupied++;
                        }
                        if (cTermPtmConfident != null && ptm.getMass() == cTermPtmConfident.getMass()) {
                            nSitesOccupied++;
                        }
                        if (nSitesOccupied < occurrence
                                && (ptm.getType() == PTM.MODAA && !ptmConfidentSites.contains(site)
                                || site == 1 && nTermPtmConfident == null && ptm.isNTerm()
                                || site == peptideSequence.length() && cTermPtmConfident == null && ptm.isCTerm())) {
                            if (ptm.isCTerm()) {
                                cTermPtmConfident = ptm;
                                site = site + 1;
                            } else if (ptm.isNTerm()) {
                                nTermPtmConfident = ptm;
                                site = 0;
                            } else {
                                ptmConfidentSites.add(site);
                            }
                            peptideScores.addConfidentModificationSite(ptmName, refSite);
                            ModificationMatch newMatch = new ModificationMatch(ptmName, true, refSite);
                            newMatch.setConfident(true);
                            ArrayList<ModificationMatch> newPtmMatches = newMatches.get(ptmMass);
                            if (newPtmMatches == null) {
                                newPtmMatches = new ArrayList<ModificationMatch>(occurrence);
                                newMatches.put(ptmMass, newPtmMatches);
                            }
                            newPtmMatches.add(newMatch);
                            if (newPtmMatches.size() > occurrence) {
                                throw new IllegalArgumentException("More sites than PTMs on peptide " + peptideMatch.getKey() + " for PTM of mass " + ptmMass + ".");
                            }
                            HashMap<Integer, ArrayList<String>> ptmInferredSites = inferredSites.get(ptmMass);
                            if (ptmInferredSites != null) {
                                ArrayList<String> ptmsAtSite = ptmInferredSites.get(site);
                                if (ptmsAtSite != null) {
                                    ptmsAtSite.remove(ptmName);
                                    if (ptmsAtSite.isEmpty()) {
                                        ptmInferredSites.remove(site);
                                        if (ptmInferredSites.isEmpty()) {
                                            inferredSites.remove(ptmMass);
                                        }
                                    }
                                }
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

            HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousSites = new HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>>(originalMatches.size());

            psmIterator = identification.getPsmIterator(peptideMatch.getSpectrumMatchesKeys(), waitingHandler);

            // Map ambiguous sites
            while (psmIterator.hasNext()) {
                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();
                psParameter = (PSParameter)spectrumMatch.getUrParam(psParameter);
                PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                for (int representativeSite : psmScores.getRepresentativeSites()) {
                    HashMap<Integer, ArrayList<String>> ambiguousMappingAtSite = psmScores.getAmbiguousPtmsAtRepresentativeSite(representativeSite);
                    int mappingSize = ambiguousMappingAtSite.size();
                    for (int refSite : ambiguousMappingAtSite.keySet()) {
                        for (String ptmName : ambiguousMappingAtSite.get(refSite)) {
                            int site = refSite;
                            PTM ptm = ptmFactory.getPTM(ptmName);
                            Double ptmMass = ptm.getMass();
                            Integer occurrence = variableModifications.get(ptmMass);
                            if (occurrence != null) {
                                ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                                int nSitesOccupied = 0;
                                if (ptmConfidentSites != null) {
                                    nSitesOccupied = ptmConfidentSites.size();
                                }
                                if (nTermPtmConfident != null && ptm.getMass() == nTermPtmConfident.getMass()) {
                                    nSitesOccupied++;
                                }
                                if (cTermPtmConfident != null && ptm.getMass() == cTermPtmConfident.getMass()) {
                                    nSitesOccupied++;
                                }
                                if (nSitesOccupied < occurrence
                                        && (ptm.getType() == PTM.MODAA && ptmConfidentSites == null
                                        || ptm.getType() == PTM.MODAA && !ptmConfidentSites.contains(site)
                                        || site == 1 && nTermPtmConfident == null && ptm.isNTerm()
                                        || site == peptideSequence.length() && cTermPtmConfident == null && ptm.isCTerm())) {
                                    if (ptm.isCTerm()) {
                                        site = site + 1;
                                    } else if (ptm.isNTerm()) {
                                        site = 0;
                                    }
                                    double probabilisticScore = 0.0;
                                    double dScore = 0.0;
                                    PtmScoring ptmScoring = psmScores.getPtmScoring(ptmName);
                                    if (ptmScoring != null) {
                                        probabilisticScore = ptmScoring.getProbabilisticScore(refSite);
                                        dScore = ptmScoring.getDeltaScore(refSite);
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
            }

            HashMap<Double, Integer> nRepresentativesMap = new HashMap<Double, Integer>();
            for (Double ptmMass : variableModifications.keySet()) {
                int nPtm = variableModifications.get(ptmMass);
                int nConfident = 0;
                ArrayList<Integer> ptmConfidentSites = confidentSites.get(ptmMass);
                if (ptmConfidentSites != null) {
                    nConfident = ptmConfidentSites.size();
                }
                if (nTermPtmConfident != null) {
                    double nTermMass = nTermPtmConfident.getMass();
                    if (nTermMass == ptmMass) {
                        nConfident++;
                    }
                }
                if (cTermPtmConfident != null) {
                    double nTermMass = cTermPtmConfident.getMass();
                    if (nTermMass == ptmMass) {
                        nConfident++;
                    }
                }
                if (nConfident < nPtm) {
                    int nRepresentatives = nPtm - nConfident;
                    if (nRepresentatives > 0) {
                        nRepresentativesMap.put(ptmMass, nRepresentatives);
                    }
                }
            }

            HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> representativeToSecondaryMap = getRepresentativeToSecondaryMap(ambiguousSites, nRepresentativesMap, inferredSites);;

            for (Double ptmMass : representativeToSecondaryMap.keySet()) {
                HashMap<Integer, HashMap<Integer, ArrayList<String>>> representativesAtMass = representativeToSecondaryMap.get(ptmMass);
                HashMap<Integer, ArrayList<String>> ptmInferredSites = inferredSites.get(ptmMass);
                for (int representativeSite : representativesAtMass.keySet()) {
                    HashMap<Integer, ArrayList<String>> siteToPtmMap = representativesAtMass.get(representativeSite);
                    for (String ptmName : siteToPtmMap.get(representativeSite)) {
                        int site = representativeSite;
                        if (site == 0) {
                            site = 1;
                        } else if (site == peptideSequence.length() + 1) {
                            site = peptideSequence.length();
                        }
                        ModificationMatch newMatch = new ModificationMatch(ptmName, true, site);
                        newMatch.setConfident(false);
                        if (ptmInferredSites != null && ptmInferredSites.containsKey(representativeSite)) {
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
                    if (representativeSite != 0 && representativeSite != peptideSequence.length() + 1) {
                        siteToPtmMap.remove(0);
                        siteToPtmMap.remove(peptideSequence.length() + 1);
                        peptideScores.addAmbiguousModificationSites(representativeSite, siteToPtmMap);
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
                throw new IllegalArgumentException("Attempting to create duplicate peptide key: " + newKey + " from peptide " + originalKey + ".");
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
    private HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> getRepresentativeToSecondaryMap(HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousScoreToSiteMap, HashMap<Double, Integer> nRepresentativesMap, HashMap<Double, HashMap<Integer, ArrayList<String>>> preferentialSites) {

        int nMasses = nRepresentativesMap.size();
        HashMap<Double, Integer> nToSelectMap = new HashMap<Double, Integer>(nMasses);
        HashMap<Double, Integer> nSelectedMap = new HashMap<Double, Integer>(nMasses);
        for (Double ptmMass : nRepresentativesMap.keySet()) {
            int nRepresentatives = nRepresentativesMap.get(ptmMass);
            int nPreferential = 0;
            if (preferentialSites != null) {
                HashMap<Integer, ArrayList<String>> sites = preferentialSites.get(ptmMass);
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
                        ptmSites = new HashSet<Integer>(2);
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
                    HashMap<Integer, ArrayList<String>> preferentialSitesForPtm = null;
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
                    if (preferentialSitesForPtm != null) {
                        for (Integer preferentialSite : preferentialSitesForPtm.keySet()) {
                            if (!sites.contains(preferentialSite)) {
                                sites.add(preferentialSite);
                            }
                            ArrayList<String> preferentialPtmsAtSite = preferentialSitesForPtm.get(preferentialSite);
                            ArrayList<String> ptmsAtSite = siteMap.get(preferentialSite);
                            if (ptmsAtSite == null) {
                                siteMap.put(preferentialSite, preferentialPtmsAtSite);
                            } else {
                                for (String ptmName : preferentialPtmsAtSite) {
                                    if (!ptmsAtSite.contains(ptmName)) {
                                        ptmsAtSite.add(ptmName);
                                    }
                                }
                            }
                        }
                    }
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
                        preferentialForPtm = preferentialSitesForPtm != null && preferentialSitesForPtm.containsKey(site);
                        if (!preferentialSiteOtherPtm && preferentialSites != null) {
                            for (Double tempMass : preferentialSites.keySet()) {
                                if (tempMass != ptmMass) {
                                    HashMap<Integer, ArrayList<String>> preferentialSitesOtherPtm = preferentialSites.get(ptmMass);
                                    if (preferentialSitesOtherPtm != null && preferentialSitesOtherPtm.containsKey(site)) {
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
                            ArrayList<String> ptmsAtSite = siteMap.get(site);
                            if (ptmsAtSite == null) {
                                throw new IllegalArgumentException("No PTM found at site " + site + ".");
                            }
                            for (String ptmName : ptmsAtSite) {
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
                            ArrayList<String> ptmsAtSite = siteMap.get(site);
                            if (ptmsAtSite == null) {
                                throw new IllegalArgumentException("No PTM found at site " + site + ".");
                            }
                            for (String ptmName : ptmsAtSite) {
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
     * @param waitingHandler the waiting handler, can be null
     *
     * @throws Exception exception thrown whenever an error occurred while
     * deserilalizing a match
     */
    public void scorePTMs(Identification identification, ProteinMatch proteinMatch, IdentificationParameters identificationParameters, boolean scorePeptides, WaitingHandler waitingHandler) throws Exception {

        PSParameter psParameter = new PSParameter();
        Protein protein = null;

        HashMap<Integer, ArrayList<String>> confidentSites = new HashMap<Integer, ArrayList<String>>();
        HashMap<Integer, HashMap<Integer, ArrayList<String>>> ambiguousSites = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();

        ArrayList<String> peptideKeys = new ArrayList<String>(proteinMatch.getPeptideMatchesKeys());

        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(new PSParameter());
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(peptideKeys, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            psParameter = (PSParameter)peptideMatch.getUrParam(psParameter);
            if (psParameter.getMatchValidationLevel().isValidated() && Peptide.isModified(peptideKey)) {
                String peptideSequence = Peptide.getSequence(peptideKey);
                if (peptideMatch.getUrParam(new PSPtmScores()) == null || scorePeptides) {
                    scorePTMs(identification, peptideMatch, identificationParameters, waitingHandler);
                }
                PSPtmScores peptideScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
                if (peptideScores != null) {

                    if (protein == null) {
                        protein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                    }
                    ArrayList<Integer> peptideStart = protein.getPeptideStart(peptideSequence,
                            identificationParameters.getSequenceMatchingPreferences());
                    for (int confidentSite : peptideScores.getConfidentSites()) {
                        for (int peptideTempStart : peptideStart) {
                            int siteOnProtein = peptideTempStart + confidentSite - 1;
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
                            int proteinRepresentativeSite = peptideTempStart + representativeSite - 1;
                            HashMap<Integer, ArrayList<String>> proteinAmbiguousSites = ambiguousSites.get(proteinRepresentativeSite);
                            if (proteinAmbiguousSites == null) {
                                proteinAmbiguousSites = new HashMap<Integer, ArrayList<String>>(peptideAmbiguousSites.size());
                                ambiguousSites.put(proteinRepresentativeSite, proteinAmbiguousSites);
                            }
                            for (int peptideSite : peptideAmbiguousSites.keySet()) {
                                int siteOnProtein = peptideTempStart + peptideSite - 1;
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
                            if (secondaryPtm.getMass() == confidentPtm.getMass()) { // @TODO: compare against the accuracy
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
                                    if (secondaryPtm.getMass() == previousPtm.getMass()) { // @TODO: compare against the accuracy
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

        PsmIterator psmIterator = identification.getPsmIterator(null);
        for (int i = 1; i <= processingPreferences.getnThreads() && !waitingHandler.isRunCanceled(); i++) {
            PsmPtmScorerRunnable runnable = new PsmPtmScorerRunnable(psmIterator, identification, identificationParameters, waitingHandler, exceptionHandler);
            pool.submit(runnable);
        }
        if (waitingHandler.isRunCanceled()) {
            pool.shutdownNow();
            return;
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

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        while (peptideMatchesIterator.hasNext()) {
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            scorePTMs(identification, peptideMatch, identificationParameters, waitingHandler);
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
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator identification features generator
     * used to generate metrics which will be stored for later reuse
     *
     * @throws Exception exception thrown whenever a problem occurred while
     * deserializing a match
     */
    public void scoreProteinPtms(Identification identification, Metrics metrics, WaitingHandler waitingHandler, IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator) throws Exception {

        waitingHandler.setWaitingText("Scoring Protein PTMs. Please Wait...");

        int max = identification.getProteinIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        // If needed, while we are iterating proteins, we will take the maximal spectrum counting value and number of validated proteins as well.
        int nValidatedProteins = 0;
        int nConfidentProteins = 0;
        double tempSpectrumCounting, maxSpectrumCounting = 0;

        PSParameter psParameter = new PSParameter();
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();

            scorePTMs(identification, proteinMatch, identificationParameters, false, waitingHandler);

            if (metrics != null) {
                psParameter = (PSParameter)proteinMatch.getUrParam(psParameter);
                if (psParameter.getMatchValidationLevel().isValidated()) {
                    nValidatedProteins++;
                    if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                        nConfidentProteins++;
                    }
                }
                if (identificationFeaturesGenerator != null) {
                    tempSpectrumCounting = identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey);
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

        SequenceMatchingPreferences ptmSequenceMatchingPreferences = identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences(),
                sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        PtmSettings ptmSettings = searchParameters.getPtmSettings();

        // PSMs with confidently localized PTMs in a map: PTM mass -> peptide sequence -> spectrum keys
        HashMap<Double, HashMap<String, HashSet<String>>> confidentPeptideInference = new HashMap<Double, HashMap<String, HashSet<String>>>();
        // PSMs with ambiguously localized PTMs in a map: File -> PTM mass -> spectrum keys
        HashMap<Double, HashSet<String>> notConfidentPeptideInference = new HashMap<Double, HashSet<String>>();

        PsmIterator psmIterator = identification.getPsmIterator(waitingHandler);

        while (psmIterator.hasNext()) {

            SpectrumMatch spectrumMatch = psmIterator.next();
            String spectrumKey = spectrumMatch.getKey();
            if (spectrumMatch.getBestPeptideAssumption() != null) {
                boolean variableAA = false;
                Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                if (peptide.isModified()) {
                    for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                        if (modificationMatch.getVariable()) {
                            String modName = modificationMatch.getTheoreticPtm();
                            PTM ptm = ptmFactory.getPTM(modName);
                            if (ptm.getType() == PTM.MODAA) {
                                variableAA = true;
                                break;
                            } else {
                                double ptmMass = ptm.getMass();
                                for (String otherPtmName : ptmSettings.getAllNotFixedModifications()) {
                                    if (!otherPtmName.equals(modName)) {
                                        PTM ptm2 = ptmFactory.getPTM(otherPtmName);
                                        if (ptm2.getMass() == ptmMass && ptm.getType() != ptm2.getType()) {
                                            variableAA = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (variableAA) {
                    boolean confident = true;
                    for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                        if (modMatch.getVariable()) {
                            String modName = modMatch.getTheoreticPtm();
                            PTM ptm = ptmFactory.getPTM(modName);
                            double ptmMass = ptm.getMass();
                            boolean maybeNotTerminal = ptm.getType() == PTM.MODAA;
                            if (!maybeNotTerminal) {
                                for (String otherPtmName : ptmSettings.getAllNotFixedModifications()) {
                                    if (!otherPtmName.equals(modName)) {
                                        PTM ptm2 = ptmFactory.getPTM(otherPtmName);
                                        if (ptm2.getMass() == ptmMass && ptm.getType() != ptm2.getType()) {
                                            maybeNotTerminal = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (maybeNotTerminal) {
                                if (!modMatch.getConfident()) {
                                    HashSet<String> spectra = notConfidentPeptideInference.get(ptmMass);
                                    if (spectra == null) {
                                        spectra = new HashSet<String>(2);
                                        notConfidentPeptideInference.put(ptmMass, spectra);
                                    }
                                    spectra.add(spectrumKey);
                                    confident = false;
                                } else {
                                    HashMap<String, HashSet<String>> modMap = confidentPeptideInference.get(ptmMass);
                                    if (modMap == null) {
                                        modMap = new HashMap<String, HashSet<String>>(2);
                                        confidentPeptideInference.put(ptmMass, modMap);
                                    }
                                    String sequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence();
                                    HashSet<String> spectra = modMap.get(sequence);
                                    if (spectra == null) {
                                        spectra = new HashSet<String>(2);
                                        modMap.put(sequence, spectra);
                                    }
                                    spectra.add(spectrumKey);
                                }
                            }
                        }
                    }
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

        HashSet<String> progress = new HashSet<String>();

        for (Double ptmMass : notConfidentPeptideInference.keySet()) {

            ArrayList<String> spectrumKeys = new ArrayList<String>(notConfidentPeptideInference.get(ptmMass));
            psmIterator = identification.getPsmIterator(spectrumKeys, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();

                Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                String sequence = peptide.getSequence();
                String notConfidentKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                int nMod = Peptide.getModificationCount(notConfidentKey, ptmMass);
                ArrayList<Integer> tempLocalizations, oldLocalizations = Peptide.getNModificationLocalized(notConfidentKey, ptmMass);
                ArrayList<Integer> newLocalizationCandidates = new ArrayList<Integer>(oldLocalizations.size());

                HashMap<String, HashSet<String>> ptmConfidentPeptides = confidentPeptideInference.get(ptmMass);

                if (ptmConfidentPeptides != null) {

                    // See if we can explain this peptide by another already identified peptide with the same number of modifications (the two peptides will be merged)
                    HashSet<String> keys = ptmConfidentPeptides.get(sequence);

                    if (keys != null) {
                        for (String tempKey : keys) {
                            SpectrumMatch secondaryMatch = (SpectrumMatch)identification.retrieveObject(tempKey);
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
                                SpectrumMatch secondaryMatch = (SpectrumMatch)identification.retrieveObject(tempKey);
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
                        HashMap<String, HashSet<String>> confidentAtMass = confidentPeptideInference.get(ptmMass);
                        for (String otherSequence : confidentAtMass.keySet()) {
                            if (!sequence.equals(otherSequence) && sequence.contains(otherSequence)) {
                                for (String tempKey : confidentAtMass.get(otherSequence)) {
                                    SpectrumMatch secondaryMatch = (SpectrumMatch)identification.retrieveObject(tempKey);
                                    String secondaryKey = secondaryMatch.getBestPeptideAssumption().getPeptide().getMatchingKey(sequenceMatchingPreferences);
                                    tempLocalizations = Peptide.getNModificationLocalized(secondaryKey, ptmMass);
                                    int tempIndex, ref = 0;
                                    String tempSequence = sequence;
                                    while ((tempIndex = tempSequence.indexOf(otherSequence)) >= 0) {
                                        ref += tempIndex;
                                        for (int localization : tempLocalizations) {
                                            int shiftedLocalization = ref + localization;
                                            if (!oldLocalizations.contains(shiftedLocalization) && !newLocalizationCandidates.contains(shiftedLocalization)) {
                                                boolean siteOccupied = false;
                                                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                                    PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                                                    if (ptm.getMass() != ptmMass && modificationMatch.getModificationSite() == shiftedLocalization) { // @TODO: compare against the accuracy
                                                        siteOccupied = true;
                                                    }
                                                }
                                                boolean candidatePtm = false;
                                                if (!siteOccupied) {
                                                    for (String ptmName : searchParameters.getPtmSettings().getAllNotFixedModifications()) {
                                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                                        if (ptm.getMass() == ptmMass && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences, ptmSequenceMatchingPreferences).contains(shiftedLocalization)) { // @TODO: compare against the accuracy
                                                            candidatePtm = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (candidatePtm && !siteOccupied) {
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
                                    SpectrumMatch secondaryMatch = (SpectrumMatch)identification.retrieveObject(tempKey);
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
                                                boolean siteOccupied = false;
                                                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                                    PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                                                    if (ptm.getMass() != ptmMass && modificationMatch.getModificationSite() == shiftedLocalization) { // @TODO: compare against the accuracy
                                                        siteOccupied = true;
                                                    }
                                                }
                                                boolean candidatePtm = false;
                                                if (!siteOccupied) {
                                                    for (String ptmName : searchParameters.getPtmSettings().getAllNotFixedModifications()) {
                                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                                        if (ptm.getMass() == ptmMass && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences, ptmSequenceMatchingPreferences).contains(shiftedLocalization)) { // @TODO: compare against the accuracy
                                                            candidatePtm = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (candidatePtm && !siteOccupied) {
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
                            if (ptm.getMass() == ptmMass && !modificationMatch.getConfident()) { // @TODO: compare against the accuracy
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
                                if (!newLocalization.equals(oldLocalization)) {
                                    String ptmCandidateName = null;
                                    for (String ptmName : searchParameters.getPtmSettings().getAllNotFixedModifications()) {
                                        PTM ptm = ptmFactory.getPTM(ptmName);
                                        if (ptm.getMass() == ptmMass && peptide.getPotentialModificationSites(ptm, sequenceMatchingPreferences, ptmSequenceMatchingPreferences).contains(newLocalization)) { // @TODO: compare against the accuracy
                                            ptmCandidateName = ptm.getName();
                                            break;
                                        }
                                    }
                                    if (ptmCandidateName == null) {
                                        throw new IllegalArgumentException("No PTM found for site " + newLocalization + " on  peptide " + peptide.getSequence() + " in spectrum " + spectrumKey + ".");
                                    }
                                    modificationMatch.setModificationSite(newLocalization);
                                    modificationMatch.setTheoreticPtm(ptmCandidateName);
                                    PSPtmScores psmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                    psmScores.changeRepresentativeSite(ptmCandidateName, oldLocalization, newLocalization);
                                }
                                modificationMatch.setInferred(true);
                            }
                        }
                        peptide.resetKeysCaches();
                    }
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

        Peptide psPeptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        
        if (psPeptide.isModified()) {
            
            SearchParameters searchParameters = identificationParameters.getSearchParameters();
            PtmSettings modificationProfile = searchParameters.getPtmSettings();
            PSPtmScores ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
            HashMap<Double, ArrayList<ModificationMatch>> modMatchesMap = new HashMap<Double, ArrayList<ModificationMatch>>(psPeptide.getNModifications());
            HashMap<Double, HashMap<Integer, String>> possiblePositions = new HashMap<Double, HashMap<Integer, String>>(psPeptide.getNModifications());
            HashMap<Double, HashMap<Integer, ArrayList<String>>> confidentSites = new HashMap<Double, HashMap<Integer, ArrayList<String>>>(psPeptide.getNModifications());

            for (ModificationMatch modificationMatch : psPeptide.getModificationMatches()) {
                if (modificationMatch.getVariable()) {
                    String modName = modificationMatch.getTheoreticPtm();
                    PTM ptm = ptmFactory.getPTM(modName);
                    double ptmMass = ptm.getMass();
                    ArrayList<ModificationMatch> ptmOccurence = modMatchesMap.get(ptmMass);
                    if (ptmOccurence == null) {
                        ptmOccurence = new ArrayList<ModificationMatch>();
                        modMatchesMap.put(ptmMass, ptmOccurence);
                    }
                    HashMap<Integer, String> ptmPossibleSites = possiblePositions.get(ptmMass);
                    if (ptmPossibleSites == null) {
                        ptmPossibleSites = new HashMap<Integer, String>();
                        possiblePositions.put(ptmMass, ptmPossibleSites);
                    }
                    boolean maybeNotTerminal = ptm.getType() == PTM.MODAA;
                    if (!maybeNotTerminal) {
                        for (String otherPtmName : modificationProfile.getAllNotFixedModifications()) {
                            if (!otherPtmName.equals(modName)) {
                                PTM ptm2 = ptmFactory.getPTM(otherPtmName);
                                if (ptm2.getMass() == ptmMass && ptm.getType() != ptm2.getType()) {
                                    maybeNotTerminal = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (maybeNotTerminal) {
                        ptmOccurence.add(modificationMatch);
                        for (String similarPtmName : modificationProfile.getSimilarNotFixedModifications(ptmMass)) {
                            PTM similarPtm = ptmFactory.getPTM(similarPtmName);
                            for (int pos : psPeptide.getPotentialModificationSites(similarPtm, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences())) {
                                ptmPossibleSites.put(pos, similarPtmName);
                            }
                        }
                    } else {
                        PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                        ptmScoring.setSiteConfidence(modificationMatch.getModificationSite(), PtmScoring.VERY_CONFIDENT);
                        modificationMatch.setConfident(true);
                        HashMap<Integer, ArrayList<String>> ptmSites = confidentSites.get(ptm.getMass());
                        if (ptmSites == null) {
                            ptmSites = new HashMap<Integer, ArrayList<String>>(1);
                            confidentSites.put(ptmMass, ptmSites);
                        }
                        Integer site = psPeptide.getSequence().length();
                        ArrayList<String> ptmNames = ptmSites.get(site);
                        if (ptmNames == null) {
                            ptmNames = new ArrayList<String>(1);
                            ptmSites.put(site, ptmNames);
                        }
                        ptmNames.add(modName);

                    }
                }
            }

            PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
            Set<Double> ptmMasses = modMatchesMap.keySet();
            HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>> ambiguousScoreToSiteMap = new HashMap<Double, HashMap<Double, HashMap<Double, HashMap<Integer, ArrayList<String>>>>>(ptmMasses.size()); // p score -> d-score -> Map PTM mass -> site -> list of modifications
            HashMap<Double, Integer> nRepresentativesMap = new HashMap<Double, Integer>(ptmMasses.size());
            ArrayList<ModificationMatch> assignedPtms = new ArrayList<ModificationMatch>(psPeptide.getModificationMatches().size());
            HashMap<Double, HashMap<Double, HashMap<Double, ArrayList<Integer>>>> scoreToSiteMap = new HashMap<Double, HashMap<Double, HashMap<Double, ArrayList<Integer>>>>(ptmMasses.size()); // p-score -> d-score -> PTM mass -> list of posssible sites

            for (double ptmMass : ptmMasses) {

                ArrayList<ModificationMatch> ptmMatches = modMatchesMap.get(ptmMass);
                int nPTMs = ptmMatches.size();
                HashMap<Integer, String> ptmPotentialSites = possiblePositions.get(ptmMass);
                int nPotentialSites = ptmPotentialSites.size();
                HashMap<Integer, ArrayList<String>> ptmConfidentSites = confidentSites.get(ptmMass);

                if (ptmConfidentSites == null) {
                    ptmConfidentSites = new HashMap<Integer, ArrayList<String>>(1);
                    confidentSites.put(ptmMass, ptmConfidentSites);
                }

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
                        ArrayList<String> ptmsAtAA = ptmConfidentSites.get(site);
                        if (ptmsAtAA == null) {
                            ptmsAtAA = new ArrayList<String>(1);
                            ptmConfidentSites.put(site, ptmsAtAA);
                        }
                        ptmsAtAA.add(modName);
                        assignedPtms.add(modMatch);
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
                                    ArrayList<String> ptmsAtAA = ptmConfidentSites.get(site);
                                    if (ptmsAtAA == null) {
                                        ptmsAtAA = new ArrayList<String>(1);
                                        ptmConfidentSites.put(site, ptmsAtAA);
                                    }
                                    ptmsAtAA.add(modName);
                                }
                                if (!modificationMatch.getConfident()) {
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
                                assignedPtms.add(modificationMatch);
                            }
                        }
                    }
                } else {
                    for (int site : ptmPotentialSites.keySet()) {

                        String modName = ptmPotentialSites.get(site);
                        PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                        double pScore = 0;
                        double dScore = 0;

                        if (ptmScoring != null) {
                            pScore = ptmScoring.getProbabilisticScore(site);
                            dScore = ptmScoring.getDeltaScore(site);
                        }

                        HashMap<Double, HashMap<Double, ArrayList<Integer>>> pScoreMap = scoreToSiteMap.get(pScore);
                        if (pScoreMap == null) {
                            pScoreMap = new HashMap<Double, HashMap<Double, ArrayList<Integer>>>(1);
                            scoreToSiteMap.put(pScore, pScoreMap);
                        }

                        HashMap<Double, ArrayList<Integer>> dScoreMap = pScoreMap.get(dScore);
                        if (dScoreMap == null) {
                            dScoreMap = new HashMap<Double, ArrayList<Integer>>(1);
                            pScoreMap.put(dScore, dScoreMap);
                        }

                        ArrayList<Integer> dScoreSites = dScoreMap.get(ptmMass);
                        if (dScoreSites == null) {
                            dScoreSites = new ArrayList<Integer>(1);
                            dScoreMap.put(ptmMass, dScoreSites);
                        }

                        dScoreSites.add(site);
                    }
                }
            }

            if (!scoreToSiteMap.isEmpty()) {

                ArrayList<Double> pScores = new ArrayList<Double>(scoreToSiteMap.keySet());
                Collections.sort(pScores, Collections.reverseOrder());
                int flrKey = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                HashMap<Double, Integer> ptmAssignedSitesCount = new HashMap<Double, Integer>(ptmMasses.size());

                for (Double pScore : pScores) {

                    HashMap<Double, HashMap<Double, ArrayList<Integer>>> pScoreMap = scoreToSiteMap.get(pScore);
                    ArrayList<Double> dScores = new ArrayList<Double>(pScoreMap.keySet());
                    Collections.sort(dScores, Collections.reverseOrder());

                    for (Double dScore : dScores) {

                        HashMap<Double, ArrayList<Integer>> dScoreMap = pScoreMap.get(dScore);
                        ArrayList<Double> ptmMassesAtScore = new ArrayList<Double>(dScoreMap.keySet());
                        Collections.sort(ptmMassesAtScore);

                        for (Double ptmMass : ptmMassesAtScore) {

                            ArrayList<ModificationMatch> ptmMatches = modMatchesMap.get(ptmMass);
                            HashMap<Integer, String> ptmPotentialSites = possiblePositions.get(ptmMass);
                            HashMap<Integer, ArrayList<String>> ptmConfidentSites = confidentSites.get(ptmMass);
                            int nPTMs = ptmMatches.size(), nPotentialSites = ptmPotentialSites.size();

                            TargetDecoyMap currentMap = psmPTMMap.getTargetDecoyMap(ptmMass, flrKey);
                            if (currentMap == null) {
                                throw new IllegalArgumentException("No FLR map found for PTM of mass " + ptmMass + " in PSMs of charge " + flrKey + ".");
                            }

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

                            ArrayList<Integer> sites = dScoreMap.get(ptmMass);
                            Collections.sort(sites);

                            Integer nAssignedSites = ptmAssignedSitesCount.get(ptmMass);
                            if (nAssignedSites == null) {
                                nAssignedSites = 0;
                            }

                            for (int site : sites) {

                                String modName = ptmPotentialSites.get(site);
                                PtmScoring ptmScoring = ptmScores.getPtmScoring(modName);
                                if (ptmScoring == null) {
                                    ptmScoring = new PtmScoring(modName);
                                    ptmScores.addPtmScoring(modName, ptmScoring);
                                }

                                ModificationMatch modificationMatch = null;

                                if (nAssignedSites < nPTMs) {

                                    boolean alreadyOccupied = false;

                                    for (ModificationMatch assignedMatch : assignedPtms) {
                                        if (assignedMatch.getModificationSite() == site) {
                                            alreadyOccupied = true;
                                            break;
                                        }
                                    }

                                    if (!alreadyOccupied) {

                                        modificationMatch = ptmMatches.get(nAssignedSites);
                                        modificationMatch.setModificationSite(site);
                                        modificationMatch.setTheoreticPtm(modName);
                                        assignedPtms.add(modificationMatch);

                                        if (pScore <= randomThreshold) {
                                            ptmScoring.setSiteConfidence(site, PtmScoring.RANDOM);
                                            modificationMatch.setConfident(false);
                                        } else if (pScore <= doubtfulThreshold) {
                                            ptmScoring.setSiteConfidence(site, PtmScoring.DOUBTFUL);
                                            modificationMatch.setConfident(false);
                                        } else {
                                            ptmScoring.setSiteConfidence(site, PtmScoring.VERY_CONFIDENT);
                                            modificationMatch.setConfident(true);
                                            ArrayList<String> ptmsAtAA = ptmConfidentSites.get(site);
                                            if (ptmsAtAA == null) {
                                                ptmsAtAA = new ArrayList<String>(1);
                                                ptmConfidentSites.put(site, ptmsAtAA);
                                            }
                                            ptmsAtAA.add(modName);
                                        }
                                        nAssignedSites++;
                                        ptmAssignedSitesCount.put(ptmMass, nAssignedSites);
                                    }
                                }

                                if (modificationMatch == null || !modificationMatch.getConfident()) {

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
                            }
                        }
                    }
                }
            }

            for (Double ptmMass : confidentSites.keySet()) {

                // Select the best scoring ambiguous sites as representative PTM sites
                HashMap<Integer, ArrayList<String>> ptmConfidentSitesMap = confidentSites.get(ptmMass);
                ArrayList<Integer> ptmConfidentSites = new ArrayList<Integer>(ptmConfidentSitesMap.keySet());
                int nConfident = 0;

                for (int site : ptmConfidentSites) {
                    ArrayList<String> ptms = ptmConfidentSitesMap.get(site);
                    for (String ptmName : ptms) {
                        ptmScores.addConfidentModificationSite(ptmName, site);
                    }
                    nConfident += ptms.size();
                }

                ptmConfidentSites.size();
                ArrayList<ModificationMatch> ptmMatches = modMatchesMap.get(ptmMass);

                int nPTMs = ptmMatches.size();
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

            psPeptide.resetKeysCaches();
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
        private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        /**
         * Constructor.
         *
         * @param psmIterator a PSM iterator
         * @param identification the identification containing the matches
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public PsmPtmScorerRunnable(PsmIterator psmIterator, Identification identification,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.psmIterator = psmIterator;
            this.identification = identification;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void run() {
            try {
                while (psmIterator.hasNext() && !waitingHandler.isRunCanceled()) {
                    SpectrumMatch spectrumMatch = psmIterator.next();
                    if (spectrumMatch != null && spectrumMatch.getBestPeptideAssumption() != null) {
                        scorePTMs(identification, spectrumMatch, identificationParameters, waitingHandler, peptideSpectrumAnnotator);
                        ptmSiteInference(spectrumMatch, identificationParameters);
                    }
                    if (waitingHandler != null && !waitingHandler.isRunCanceled()) {
                        waitingHandler.increaseSecondaryProgressCounter();
                    }
                }
            } catch (Exception e) {
                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();
            }
        }
    }
}
