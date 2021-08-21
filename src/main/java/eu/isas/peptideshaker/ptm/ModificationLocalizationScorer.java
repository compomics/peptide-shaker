package eu.isas.peptideshaker.ptm;

import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationProvider;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.ModificationLocalizationScore;
import com.compomics.util.experiment.identification.modification.scores.PhosphoRS;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.utils.ModificationUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.peptide_shaker.PSModificationScores;
import com.compomics.util.experiment.identification.peptide_shaker.ModificationScoring;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class scores the PSM PTMs using the scores implemented in compomics
 * utilities.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ModificationLocalizationScorer extends ExperimentObject {

    /**
     * The compomics modification factory.
     */
    private ModificationFactory modificationFactory = ModificationFactory.getInstance();

    /**
     * Constructor.
     */
    public ModificationLocalizationScorer() {

    }

    /**
     * Scores the modification locations using the delta score.
     *
     * @param identification Identification object containing the matches.
     * @param spectrumMatch The spectrum match of interest.
     * @param sequenceMatchingParameters The sequence matching preferences.
     * @param modificationProvider The modification provider to use.
     */
    public void attachDeltaScore(
            Identification identification,
            SpectrumMatch spectrumMatch,
            SequenceMatchingParameters sequenceMatchingParameters,
            ModificationProvider modificationProvider
    ) {

        PSModificationScores modificationScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);

        if (modificationScores == null) {

            modificationScores = new PSModificationScores();
            spectrumMatch.addUrParam(modificationScores);

        }

        Peptide psPeptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = spectrumMatch.getPeptideAssumptionsMap();

        String mainSequence = psPeptide.getSequence();

        HashMap<String, List<Integer>> modificationParameters = Arrays.stream(psPeptide.getVariableModifications())
                .collect(
                        Collectors.groupingBy(
                                ModificationMatch::getModification,
                                HashMap::new,
                                Collectors.mapping(ModificationMatch::getSite, Collectors.toList())
                        )
                );

        for (Entry<String, List<Integer>> entry : modificationParameters.entrySet()) {

            String modName = entry.getKey();
            List<Integer> sites = entry.getValue();

            Modification modification1 = modificationProvider.getModification(modName);

            for (int modSite : sites) {

                double refP = 1, secondaryP = 1;

                for (TreeMap<Double, ArrayList<PeptideAssumption>> algorithmAssumptions : assumptions.values()) {

                    for (ArrayList<PeptideAssumption> assumptionsAtScore : algorithmAssumptions.values()) {

                        for (PeptideAssumption peptideAssumption : assumptionsAtScore) {

                            if (peptideAssumption.getPeptide().getSequence().equals(mainSequence)) {

                                boolean modificationAtSite = false, modificationFound = false;

                                Peptide peptide = peptideAssumption.getPeptide();

                                for (ModificationMatch modMatch : peptide.getVariableModifications()) {

                                    Modification modification2 = modificationProvider.getModification(modMatch.getModification());

                                    if (modification1.getMass() == modification2.getMass()) {

                                        modificationFound = true;
                                        PSParameter psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
                                        double p = psParameter.getProbability();

                                        if (modMatch.getSite() == modSite) {

                                            modificationAtSite = true;

                                            if (p < refP) {

                                                refP = p;

                                            }
                                        }
                                    }
                                }

                                if (!modificationAtSite && modificationFound) {

                                    PSParameter psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
                                    double p = psParameter.getProbability();

                                    if (p < secondaryP) {

                                        secondaryP = p;

                                    }
                                }
                            }
                        }
                    }
                }

                ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                if (modificationScoring == null) {

                    modificationScoring = new ModificationScoring(modName);
                    modificationScores.addModificationScoring(modName, modificationScoring);

                }

                if (secondaryP < refP) {
                    secondaryP = refP;
                }

                double deltaScore = (secondaryP - refP) * 100;
                modificationScoring.setDeltaScore(modSite, deltaScore);
            }

            identification.updateObject(spectrumMatch.getKey(), spectrumMatch);

        }
    }

    /**
     * Attaches the selected probabilistic modification score.
     *
     * @param spectrumMatch The spectrum match studied, the score will be
     * calculated for the best assumption only.
     * @param sequenceProvider The protein sequence provider to use.
     * @param spectrumProvider The spectrum provider to use.
     * @param modificationProvider The modification provider to use.
     * @param identificationParameters The identification parameters.
     * @param peptideSpectrumAnnotator The peptide spectrum annotator to use.
     * @param identification The identification object containing the matches.
     */
    private void attachProbabilisticScore(
            SpectrumMatch spectrumMatch,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            ModificationProvider modificationProvider,
            IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            Identification identification
    ) {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        AnnotationParameters annotationParameters = identificationParameters.getAnnotationParameters();
        ModificationLocalizationParameters scoringParameters = identificationParameters.getModificationLocalizationParameters();
        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = scoringParameters.getSequenceMatchingParameters();
        ModificationParameters modificationParameters = searchParameters.getModificationParameters();
        PSModificationScores modificationScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);

        if (modificationScores != null) {

            modificationScores = new PSModificationScores();
            spectrumMatch.addUrParam(modificationScores);

        }

        HashMap<Double, ArrayList<Modification>> modificationsMap = new HashMap<>(1);
        HashMap<Double, Integer> nMod = new HashMap<>(1);
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();
        Peptide peptide = bestPeptideAssumption.getPeptide();

        for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

            Modification refMod = modificationProvider.getModification(modificationMatch.getModification());
            double modMass = refMod.getMass();

            if (!modificationsMap.containsKey(modMass)) {

                ArrayList<Modification> modifications = modificationFactory.getSameMassNotFixedModifications(modMass, searchParameters).stream()
                        .map(
                                modification -> modificationProvider.getModification(modification)
                        )
                        .collect(
                                Collectors.toCollection(ArrayList::new)
                        );

                modificationsMap.put(modMass, modifications);
                nMod.put(modMass, 1);

            } else {

                nMod.put(modMass, nMod.get(modMass) + 1);

            }
        }

        if (!modificationsMap.isEmpty()) {

            String spectrumFile = spectrumMatch.getSpectrumFile();
            String spectrumTitle = spectrumMatch.getSpectrumTitle();
            Spectrum spectrum = spectrumProvider.getSpectrum(
                    spectrumFile,
                    spectrumTitle
            );
            SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                    spectrumFile,
                    spectrumTitle,
                    bestPeptideAssumption,
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters,
                    peptideSpectrumAnnotator
            );

            for (double modMass : modificationsMap.keySet()) {

                HashMap<Integer, Double> scores = null;

                if (scoringParameters.getSelectedProbabilisticScore() == ModificationLocalizationScore.PhosphoRS) {

                    scores = PhosphoRS.getSequenceProbabilities(
                            peptide,
                            modificationsMap.get(modMass),
                            modificationParameters,
                            spectrum,
                            sequenceProvider,
                            annotationParameters,
                            specificAnnotationParameters,
                            scoringParameters.isProbabilisticScoreNeutralLosses(),
                            sequenceMatchingParameters,
                            modificationSequenceMatchingParameters,
                            peptideSpectrumAnnotator
                    );

                    if (scores == null) {

                        throw new IllegalArgumentException(
                                "An error occurred while scoring spectrum "
                                + spectrumTitle
                                + " of file "
                                + spectrumFile
                                + " with PhosphoRS."
                        ); // Most likely a compatibility issue with utilities

                    }
                }

                if (scores != null) {

                    // remap to searched modifications
                    Modification mappedModification = null;
                    String peptideSequence = peptide.getSequence();

                    for (int site : scores.keySet()) {

                        if (site == 0) {

                            // N-term mod
                            for (Modification modification : modificationsMap.get(modMass)) {

                                if (modification.getModificationType().isNTerm()) {

                                    mappedModification = modification;
                                    break;

                                }
                            }

                            if (mappedModification == null) {

                                throw new IllegalArgumentException(
                                        "Could not map the PTM of mass "
                                        + modMass
                                        + " on the N-terminus of the peptide "
                                        + peptideSequence + "."
                                );

                            }

                        } else if (site == peptideSequence.length() + 1) {

                            // C-term mod
                            for (Modification modification : modificationsMap.get(modMass)) {

                                if (modification.getModificationType().isCTerm()) {

                                    mappedModification = modification;
                                    break;

                                }
                            }

                            if (mappedModification == null) {

                                throw new IllegalArgumentException(
                                        "Could not map the PTM of mass "
                                        + modMass
                                        + " on the C-terminus of the peptide "
                                        + peptideSequence + "."
                                );

                            }

                        } else {

                            for (Modification modification : modificationsMap.get(modMass)) {

                                mappedModification = modification;
                                break;

                            }

                            if (mappedModification == null) {

                                throw new IllegalArgumentException(
                                        "Could not map the PTM of mass "
                                        + modMass
                                        + " at site "
                                        + site
                                        + " in peptide "
                                        + peptide.getSequence()
                                        + "."
                                );

                            }
                        }

                        String modName = mappedModification.getName();

                        ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                        if (modificationScoring == null) {

                            modificationScoring = new ModificationScoring(modName);
                            modificationScores.addModificationScoring(modName, modificationScoring);

                        }

                        modificationScoring.setProbabilisticScore(site, scores.get(site));

                    }
                }
            }

            identification.updateObject(spectrumMatch.getKey(), spectrumMatch);

        }
    }

    /**
     * Scores PTM locations for a desired spectrum match.
     *
     * @param identification Identification object containing the matches.
     * @param spectrumMatch The spectrum match to score.
     * @param sequenceProvider The protein sequence provider to use.
     * @param spectrumProvider The spectrum provider to use.
     * @param modificationProvider The modification provider to use.
     * @param identificationParameters The parameters used for identification.
     * @param waitingHandler The waiting handler to use to display progress and
     * allow canceling.
     * @param peptideSpectrumAnnotator The spectrum annotator to use.
     */
    public void scorePTMs(
            Identification identification,
            SpectrumMatch spectrumMatch,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            ModificationProvider modificationProvider,
            IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator
    ) {

        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        attachDeltaScore(
                identification,
                spectrumMatch,
                sequenceMatchingParameters,
                modificationProvider
        );

        ModificationLocalizationParameters scoringParameters = identificationParameters.getModificationLocalizationParameters();

        if (scoringParameters.isProbabilisticScoreCalculation()) {

            attachProbabilisticScore(
                    spectrumMatch,
                    sequenceProvider,
                    spectrumProvider,
                    modificationProvider,
                    identificationParameters,
                    peptideSpectrumAnnotator,
                    identification
            );
        }
    }

    /**
     * Scores the PTMs for a peptide match.
     *
     * @param identification identification object containing the identification
     * matches
     * @param peptideMatch The peptide match to score.
     * @param identificationParameters The identification parameters.
     * @param modificationProvider The modification provider to use.
     * @param waitingHandler The waiting handler to use, ignored if null.
     */
    public void scorePTMs(
            Identification identification,
            PeptideMatch peptideMatch,
            IdentificationParameters identificationParameters,
            ModificationProvider modificationProvider,
            WaitingHandler waitingHandler
    ) {

        Peptide peptide = peptideMatch.getPeptide();
        String peptideSequence = peptide.getSequence();
        ModificationMatch[] originalMatches = peptide.getVariableModifications();

        if (originalMatches.length == 0) {
            return;
        }

        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        ModificationParameters modificationSettings = identificationParameters.getSearchParameters().getModificationParameters();

        PSModificationScores peptideScores = new PSModificationScores();

        HashMap<Double, Integer> variableModifications = new HashMap<>(peptide.getNVariableModifications());
        HashMap<Double, HashMap<Integer, HashSet<String>>> inferredSites = new HashMap<>(peptide.getNVariableModifications());
        Modification nTermModConfident = null;
        Modification cTermModConfident = null;

        long originalKey = peptide.getMatchingKey(sequenceMatchingParameters);
        ArrayList<ModificationMatch> newModificationMatches = new ArrayList<>(originalMatches.length);

        for (ModificationMatch modificationMatch : originalMatches) {

            String modName = modificationMatch.getModification();
            Modification modification = modificationProvider.getModification(modName);
            double modMass = modification.getMass();
            boolean maybeNotTerminal = modification.getModificationType() == ModificationType.modaa;

            if (!maybeNotTerminal) {

                for (String otherPtmName : modificationSettings.getAllNotFixedModifications()) {

                    if (!otherPtmName.equals(modName)) {

                        Modification tempMod = modificationProvider.getModification(otherPtmName);

                        if (tempMod.getMass() == modMass && modification.getModificationType() != tempMod.getModificationType()) {

                            maybeNotTerminal = true;
                            break;

                        }
                    }
                }
            }

            if (maybeNotTerminal) {

                Integer nMod = variableModifications.get(modMass);

                if (nMod == null) {

                    variableModifications.put(modMass, 1);

                } else {

                    variableModifications.put(modMass, nMod + 1);

                }

                if (modificationMatch.getInferred()) {

                    Integer modificationSite;

                    if (modification.getModificationType().isCTerm()) {

                        modificationSite = peptideSequence.length() + 1;

                    } else if (modification.getModificationType().isNTerm()) {

                        modificationSite = 0;

                    } else {

                        modificationSite = modificationMatch.getSite();

                    }

                    HashMap<Integer, HashSet<String>> modificationInferredSites = inferredSites.get(modMass);

                    if (modificationInferredSites == null) {

                        modificationInferredSites = new HashMap<>(1);
                        inferredSites.put(modMass, modificationInferredSites);

                    }

                    HashSet<String> modificationsAtSite = modificationInferredSites.get(modificationSite);

                    if (modificationsAtSite == null) {

                        modificationsAtSite = new HashSet<>(1);
                        modificationInferredSites.put(modificationSite, modificationsAtSite);

                    }

                    modificationsAtSite.add(modName);

                }

            } else {

                newModificationMatches.add(modificationMatch);

                if (modification.getModificationType().isCTerm()) {

                    if (cTermModConfident != null) {

                        throw new IllegalArgumentException("Multiple PTMs on termini not supported.");

                    }

                    cTermModConfident = modificationProvider.getModification(modName);

                } else if (modification.getModificationType().isNTerm()) {

                    if (nTermModConfident != null) {

                        throw new IllegalArgumentException("Multiple PTMs on termini not supported.");

                    }

                    nTermModConfident = modificationProvider.getModification(modName);

                } else {

                    throw new IllegalArgumentException("Non-terminal PTM should be of type PTM.MODAA.");

                }
            }
        }

        if (variableModifications.isEmpty()) {

            if (cTermModConfident != null || nTermModConfident != null) {

                if (cTermModConfident != null) {

                    peptideScores.addConfidentModificationSite(cTermModConfident.getName(), peptideSequence.length() + 1);

                }
                if (nTermModConfident != null) {

                    peptideScores.addConfidentModificationSite(nTermModConfident.getName(), 0);

                }

                peptideMatch.addUrParam(peptideScores);

            }

            return;
        }

        // Map confident sites
        HashMap<Double, ArrayList<ModificationMatch>> newMatches = new HashMap<>(variableModifications.size());
        HashMap<Double, ArrayList<Integer>> confidentSites = new HashMap<>(variableModifications.size());

        for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            PSModificationScores psmScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);

            for (String modName : psmScores.getScoredModifications()) {

                ModificationScoring psmScoring = psmScores.getModificationScoring(modName);
                ModificationScoring peptideScoring = peptideScores.getModificationScoring(modName);

                if (peptideScoring == null) {

                    peptideScoring = new ModificationScoring(modName);
                    peptideScores.addModificationScoring(modName, peptideScoring);

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

            for (int refSite : psmScores.getConfidentSites()) {

                for (String modName : psmScores.getConfidentModificationsAt(refSite)) {

                    int site = refSite;
                    Modification modification = modificationProvider.getModification(modName);
                    Double modMass = modification.getMass();
                    Integer occurrence = variableModifications.get(modMass);

                    if (occurrence != null) {

                        ArrayList<Integer> modificationConfidentSites = confidentSites.get(modMass);

                        if (modificationConfidentSites == null) {

                            modificationConfidentSites = new ArrayList<>(1);
                            confidentSites.put(modMass, modificationConfidentSites);

                        }

                        int nSitesOccupied = modificationConfidentSites.size();

                        if (nTermModConfident != null && modification.getMass() == nTermModConfident.getMass()) {

                            nSitesOccupied++;

                        }

                        if (cTermModConfident != null && modification.getMass() == cTermModConfident.getMass()) {

                            nSitesOccupied++;

                        }

                        if (nSitesOccupied < occurrence
                                && (modification.getModificationType() == ModificationType.modaa && !modificationConfidentSites.contains(site)
                                || site == 0 && nTermModConfident == null && modification.getModificationType().isNTerm()
                                || site == peptideSequence.length() + 1 && cTermModConfident == null && modification.getModificationType().isCTerm())) {

                            if (modification.getModificationType().isCTerm()) {

                                cTermModConfident = modification;

                            } else if (modification.getModificationType().isNTerm()) {

                                nTermModConfident = modification;

                            } else {

                                modificationConfidentSites.add(site);

                            }

                            peptideScores.addConfidentModificationSite(modName, refSite);
                            ModificationMatch newMatch = new ModificationMatch(modName, refSite);
                            newMatch.setConfident(true);
                            ArrayList<ModificationMatch> newModMatches = newMatches.get(modMass);

                            if (newModMatches == null) {

                                newModMatches = new ArrayList<>(occurrence);
                                newMatches.put(modMass, newModMatches);

                            }

                            newModMatches.add(newMatch);

                            if (newModMatches.size() > occurrence) {

                                throw new IllegalArgumentException("More sites than modifications on peptide " + peptideMatch.getKey() + " for PTM of mass " + modMass + ".");

                            }

                            HashMap<Integer, HashSet<String>> modificationInferredSites = inferredSites.get(modMass);

                            if (modificationInferredSites != null) {

                                HashSet<String> modificationsAtSite = modificationInferredSites.get(site);

                                if (modificationsAtSite != null) {

                                    modificationsAtSite.remove(modName);

                                    if (modificationsAtSite.isEmpty()) {

                                        modificationInferredSites.remove(site);

                                        if (modificationInferredSites.isEmpty()) {

                                            inferredSites.remove(modMass);

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

        for (double modMass : variableModifications.keySet()) {

            int nMods = variableModifications.get(modMass);
            int nConfident = 0;
            ArrayList<Integer> modConfidentSites = confidentSites.get(modMass);

            if (modConfidentSites != null) {

                nConfident = modConfidentSites.size();

            }

            if (nConfident < nMods) {

                enoughSites = false;
                break;

            }
        }

        if (!enoughSites) {

            TreeMap<Double, TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>>> ambiguousSites = new TreeMap<>();

            // Map ambiguous sites
            for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PSModificationScores psmScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);

                for (int representativeSite : psmScores.getRepresentativeSites()) {

                    HashMap<Integer, HashSet<String>> ambiguousMappingAtSite = psmScores.getAmbiguousModificationsAtRepresentativeSite(representativeSite);

                    for (Entry<Integer, HashSet<String>> entry : ambiguousMappingAtSite.entrySet()) {

                        int refSite = entry.getKey();

                        for (String modName : entry.getValue()) {

                            int site = refSite;
                            Modification modification = modificationProvider.getModification(modName);
                            Double modMass = modification.getMass();
                            Integer occurrence = variableModifications.get(modMass);

                            if (occurrence != null) {

                                ArrayList<Integer> modificationConfidentSites = confidentSites.get(modMass);
                                int nSitesOccupied = 0;

                                if (modificationConfidentSites != null) {

                                    nSitesOccupied = modificationConfidentSites.size();

                                }

                                if (nTermModConfident != null && modification.getMass() == nTermModConfident.getMass()) {

                                    nSitesOccupied++;

                                }

                                if (cTermModConfident != null && modification.getMass() == cTermModConfident.getMass()) {

                                    nSitesOccupied++;

                                }

                                if (nSitesOccupied < occurrence
                                        && (modification.getModificationType() == ModificationType.modaa && modificationConfidentSites == null
                                        || modification.getModificationType() == ModificationType.modaa && !modificationConfidentSites.contains(site)
                                        || site == 0 && nTermModConfident == null && modification.getModificationType().isNTerm()
                                        || site == peptideSequence.length() + 1 && cTermModConfident == null && modification.getModificationType().isNTerm())) {

                                    double probabilisticScore = 0.0;
                                    double dScore = 0.0;
                                    ModificationScoring modificationScoring = psmScores.getModificationScoring(modName);

                                    if (modificationScoring != null) {

                                        probabilisticScore = modificationScoring.getProbabilisticScore(refSite);
                                        dScore = modificationScoring.getDeltaScore(refSite);

                                    }

                                    TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>> pScoreMap = ambiguousSites.get(probabilisticScore);

                                    if (pScoreMap == null) {

                                        pScoreMap = new TreeMap<>();
                                        ambiguousSites.put(probabilisticScore, pScoreMap);

                                    }

                                    TreeMap<Double, HashMap<Integer, HashSet<String>>> dScoreMap = pScoreMap.get(dScore);

                                    if (dScoreMap == null) {

                                        dScoreMap = new TreeMap<>();
                                        pScoreMap.put(dScore, dScoreMap);

                                    }

                                    HashMap<Integer, HashSet<String>> modificationMap = dScoreMap.get(modMass);

                                    if (modificationMap == null) {

                                        modificationMap = new HashMap<>(1);
                                        dScoreMap.put(modMass, modificationMap);

                                    }

                                    HashSet<String> modifications = modificationMap.get(site);

                                    if (modifications == null) {

                                        modifications = new HashSet<>(1);
                                        modificationMap.put(site, modifications);

                                    }

                                    if (!modifications.contains(modName)) {

                                        modifications.add(modName);

                                    }
                                }
                            }
                        }
                    }
                }
            }

            HashMap<Double, Integer> nRepresentativesMap = new HashMap<>();

            for (double modMass : variableModifications.keySet()) {

                int nMod = variableModifications.get(modMass);
                int nConfident = 0;
                ArrayList<Integer> modificationConfidentSites = confidentSites.get(modMass);

                if (modificationConfidentSites != null) {

                    nConfident = modificationConfidentSites.size();

                }

                if (nTermModConfident != null) {

                    double nTermMass = nTermModConfident.getMass();

                    if (nTermMass == modMass) {

                        nConfident++;

                    }
                }

                if (cTermModConfident != null) {

                    double nTermMass = cTermModConfident.getMass();

                    if (nTermMass == modMass) {

                        nConfident++;

                    }
                }

                if (nConfident < nMod) {

                    int nRepresentatives = nMod - nConfident;

                    if (nRepresentatives > 0) {

                        nRepresentativesMap.put(modMass, nRepresentatives);

                    }
                }
            }

            HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<String>>>> representativeToSecondaryMap;

            representativeToSecondaryMap = getRepresentativeToSecondaryMap(
                    ambiguousSites,
                    nRepresentativesMap,
                    inferredSites
            );

            for (Double modMass : representativeToSecondaryMap.keySet()) {

                HashMap<Integer, HashMap<Integer, HashSet<String>>> representativesAtMass = representativeToSecondaryMap.get(modMass);
                HashMap<Integer, HashSet<String>> modificationInferredSites = inferredSites.get(modMass);

                for (int representativeSite : representativesAtMass.keySet()) {

                    HashMap<Integer, HashSet<String>> siteToModMap = representativesAtMass.get(representativeSite);

                    for (String modName : siteToModMap.get(representativeSite)) {

                        ModificationMatch newMatch = new ModificationMatch(modName, representativeSite);
                        newMatch.setConfident(false);

                        if (modificationInferredSites != null && modificationInferredSites.containsKey(representativeSite)) {

                            newMatch.setInferred(true);

                        }

                        ArrayList<ModificationMatch> modificatoinMatchesTemp = newMatches.get(modMass);

                        if (modificatoinMatchesTemp == null) {

                            modificatoinMatchesTemp = new ArrayList<>(1);
                            newMatches.put(modMass, modificatoinMatchesTemp);

                        }

                        modificatoinMatchesTemp.add(newMatch);

                        if (modificatoinMatchesTemp.size() > variableModifications.get(modMass)) {

                            throw new IllegalArgumentException(
                                    "More sites than modifications on peptide "
                                    + peptideMatch.getKey()
                                    + " for modification of mass "
                                    + modMass + "."
                            );
                        }
                    }

                    if (representativeSite != 0 && representativeSite != peptideSequence.length() + 1) {

                        peptideScores.addAmbiguousModificationSites(representativeSite, siteToModMap);

                    }
                }
            }
        }

        for (ArrayList<ModificationMatch> modificationMatches : newMatches.values()) {

            newModificationMatches.addAll(modificationMatches);

        }

        if (cTermModConfident != null) {

            peptideScores.addConfidentModificationSite(cTermModConfident.getName(), peptideSequence.length() + 1);

        }
        if (nTermModConfident != null) {

            peptideScores.addConfidentModificationSite(nTermModConfident.getName(), 0);

        }

        double mass = peptide.getMass();
        peptide.setVariableModifications(newModificationMatches.toArray(new ModificationMatch[newModificationMatches.size()]));
        peptide.setMass(mass);

        peptideMatch.addUrParam(peptideScores);

        long newKey = peptide.getMatchingKey(sequenceMatchingParameters);

        if (newKey != originalKey) {

            if (identification.getPeptideIdentification().contains(newKey)) {

                throw new IllegalArgumentException(
                        "Attempting to create duplicate peptide key: "
                        + newKey
                        + " from peptide "
                        + originalKey
                        + "."
                );

            }
            identification.removeObject(originalKey);
            identification.addObject(newKey, peptideMatch);
        } else {
            identification.updateObject(originalKey, peptideMatch);
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
    private HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<String>>>> getRepresentativeToSecondaryMap(
            TreeMap<Double, TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>>> ambiguousScoreToSiteMap,
            HashMap<Double, Integer> nRepresentatives
    ) {

        return getRepresentativeToSecondaryMap(
                ambiguousScoreToSiteMap,
                nRepresentatives,
                null
        );

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
    private HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<String>>>> getRepresentativeToSecondaryMap(
            TreeMap<Double, TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>>> ambiguousScoreToSiteMap,
            HashMap<Double, Integer> nRepresentativesMap,
            HashMap<Double, HashMap<Integer, HashSet<String>>> preferentialSites
    ) {

        int nMasses = nRepresentativesMap.size();
        HashMap<Double, Integer> nToSelectMap = new HashMap<>(nMasses);
        HashMap<Double, Integer> nSelectedMap = new HashMap<>(nMasses);

        for (double modMass : nRepresentativesMap.keySet()) {

            int nRepresentatives = nRepresentativesMap.get(modMass);
            int nPreferential = 0;

            if (preferentialSites != null) {

                HashMap<Integer, HashSet<String>> sites = preferentialSites.get(modMass);

                if (sites != null) {

                    nPreferential = sites.size();

                }
            }

            int toSelect = Math.max(nRepresentatives - nPreferential, 0);
            nToSelectMap.put(modMass, toSelect);
            nSelectedMap.put(modMass, 0);

        }

        HashMap<Double, HashSet<Integer>> possibleSites = new HashMap<>(nMasses);

        for (TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>> mapAtPscore : ambiguousScoreToSiteMap.values()) {

            for (TreeMap<Double, HashMap<Integer, HashSet<String>>> mapAtDScore : mapAtPscore.values()) {

                for (double modMass : mapAtDScore.keySet()) {

                    HashSet<Integer> modificationSites = possibleSites.get(modMass);

                    if (modificationSites == null) {

                        modificationSites = new HashSet<>(2);
                        possibleSites.put(modMass, modificationSites);

                    }

                    Set<Integer> sitesAtScore = mapAtDScore.get(modMass).keySet();
                    modificationSites.addAll(sitesAtScore);

                }
            }
        }

        HashMap<Double, HashMap<Integer, HashSet<String>>> representativeSites = new HashMap<>(nMasses);
        HashMap<Double, HashMap<Integer, HashSet<String>>> secondarySites = new HashMap<>(nMasses);

        for (TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>> dScoresMap : ambiguousScoreToSiteMap.descendingMap().values()) {

            for (TreeMap<Double, HashMap<Integer, HashSet<String>>> modMap : dScoresMap.descendingMap().values()) {

                for (Entry<Double, HashMap<Integer, HashSet<String>>> entry : modMap.entrySet()) {

                    double modMass = entry.getKey();
                    HashMap<Integer, HashSet<String>> modPreferentialSites = null;

                    if (preferentialSites != null) {

                        modPreferentialSites = preferentialSites.get(modMass);

                    }

                    int toSelect = nToSelectMap.containsKey(modMass) ? nToSelectMap.get(modMass) : 0;
                    int nSelected = nSelectedMap.containsKey(modMass) ? nSelectedMap.get(modMass) : 0;
                    int nNeeded = toSelect - nSelected;

                    if (nNeeded > 0) {

                        HashSet<Integer> modSites = possibleSites.get(modMass);

                        if (modSites == null || modSites.size() < nNeeded) {

                            throw new IllegalArgumentException("Not enough sites (" + possibleSites.size() + " where " + nNeeded + " needed) found for modification mass " + modMass + ".");

                        }
                    }

                    HashMap<Integer, HashSet<String>> siteMap = entry.getValue();
                    HashSet<Integer> prioritizedSites = new HashSet<>(0);

                    if (modPreferentialSites != null) {

                        for (Entry<Integer, HashSet<String>> entry2 : modPreferentialSites.entrySet()) {

                            int preferentialSite = entry2.getKey();

                            if (!prioritizedSites.contains(preferentialSite)) {

                                prioritizedSites.add(preferentialSite);

                            }

                            HashSet<String> preferentialModsAtSite = entry2.getValue();
                            HashSet<String> modificationsAtSite = siteMap.get(preferentialSite);

                            if (modificationsAtSite == null) {

                                siteMap.put(preferentialSite, preferentialModsAtSite);

                            } else {

                                for (String modName : preferentialModsAtSite) {

                                    if (!modificationsAtSite.contains(modName)) {

                                        modificationsAtSite.add(modName);

                                    }
                                }
                            }
                        }
                    }

                    int[] sites = Stream.concat(prioritizedSites.stream()
                            .sorted(),
                            siteMap.keySet().stream()
                                    .filter(site -> !prioritizedSites.contains(site)))
                            .mapToInt(a -> a)
                            .toArray();

                    for (int site : sites) {

                        boolean referenceOtherMod = false;

                        for (double tempMass : representativeSites.keySet()) {

                            if (representativeSites.get(tempMass).keySet().contains(site)) {

                                referenceOtherMod = true;
                                break;

                            }
                        }

                        boolean preferentialForMod;
                        boolean preferentialSiteOtherMod = false;
                        preferentialForMod = modPreferentialSites != null && modPreferentialSites.containsKey(site);

                        if (!preferentialSiteOtherMod && preferentialSites != null) {

                            for (Entry<Double, HashMap<Integer, HashSet<String>>> entry2 : preferentialSites.entrySet()) {

                                double tempMass = entry2.getKey();

                                if (tempMass != modMass) {

                                    HashMap<Integer, HashSet<String>> preferentialSitesOtherPtm = entry2.getValue();

                                    if (preferentialSitesOtherPtm.containsKey(site)) {

                                        preferentialSiteOtherMod = true;
                                        break;

                                    }
                                }
                            }
                        }

                        boolean blockingOtherMod = false;
                        HashSet<Double> tempMasses = new HashSet<>(possibleSites.keySet());

                        for (Double tempMass : tempMasses) {

                            if (tempMass != modMass) {

                                Integer tempToSelect = nToSelectMap.get(tempMass);
                                Integer tempSelected = nSelectedMap.get(tempMass);

                                if (tempToSelect != null && tempSelected != null) {

                                    int toMapForPtm = tempToSelect - tempSelected;

                                    if (toMapForPtm > 0) {

                                        HashSet<Integer> tempSites = possibleSites.get(tempMass);

                                        if (tempSites == null) {

                                            throw new IllegalArgumentException("No sites found for PTM of mass " + modMass + ".");

                                        }

                                        if (tempSites.size() == toMapForPtm && tempSites.contains(site)) {

                                            blockingOtherMod = true;
                                            break;

                                        }
                                    }
                                }
                            }
                        }

                        if (!referenceOtherMod && !blockingOtherMod
                                && (preferentialForMod || !preferentialSiteOtherMod && nSelected < toSelect)) {

                            HashMap<Integer, HashSet<String>> representativeSitesForModMass = representativeSites.get(modMass);

                            if (representativeSitesForModMass == null) {

                                representativeSitesForModMass = new HashMap<>(nRepresentativesMap.get(modMass));
                                representativeSites.put(modMass, representativeSitesForModMass);

                            }

                            HashSet<String> modificationsAtSite = siteMap.get(site);

                            if (modificationsAtSite == null) {

                                throw new IllegalArgumentException("No modification found at site " + site + ".");

                            }

                            for (String modName : modificationsAtSite) {

                                HashSet<String> modifications = representativeSitesForModMass.get(site);

                                if (modifications == null) {

                                    modifications = new HashSet<>(1);
                                    representativeSitesForModMass.put(site, modifications);

                                    if (!preferentialForMod) {

                                        nSelected++;

                                    }
                                }

                                if (!modifications.contains(modName)) {

                                    modifications.add(modName);

                                }
                            }

                            for (double tempMass : tempMasses) {

                                HashSet<Integer> tempSites = possibleSites.get(tempMass);
                                tempSites.remove(site);

                                if (tempSites.isEmpty()) {

                                    possibleSites.remove(tempMass);

                                }
                            }

                        } else {

                            HashMap<Integer, HashSet<String>> secondarySitesForPtmMass = secondarySites.get(modMass);

                            if (secondarySitesForPtmMass == null) {

                                int size = 1;
                                HashSet<Integer> modificationSites = possibleSites.get(modMass);

                                if (modificationSites != null) {

                                    size = modificationSites.size() - toSelect + nSelected;

                                }

                                secondarySitesForPtmMass = new HashMap<>(size);
                                secondarySites.put(modMass, siteMap);

                            }

                            HashSet<String> modificationsAtSite = siteMap.get(site);

                            if (modificationsAtSite == null) {

                                throw new IllegalArgumentException("No PTM found at site " + site + ".");

                            }

                            for (String modName : modificationsAtSite) {

                                HashSet<String> modifications = secondarySitesForPtmMass.get(site);

                                if (modifications == null) {

                                    modifications = new HashSet<>(1);
                                    secondarySitesForPtmMass.put(site, modifications);

                                }

                                if (!modifications.contains(modName)) {

                                    modifications.add(modName);

                                }
                            }
                        }
                    }

                    nSelectedMap.put(modMass, nSelected);

                }
            }
        }

        for (double modMass : nToSelectMap.keySet()) {

            Integer nSelected = nSelectedMap.get(modMass);
            int nToSelect = nToSelectMap.get(modMass);

            if (nSelected == null || nSelected < nToSelect) {

                System.out.println("nToSelectMap: " + nToSelect);

                for (double modMasss : nToSelectMap.keySet()) {

                    System.out.println(modMasss + " -> " + nToSelectMap.get(modMass));

                }

                System.out.println("nSelectedMap: " + nSelected);

                for (double modMasss : nSelectedMap.keySet()) {

                    System.out.println(modMasss + " -> " + nSelectedMap.get(modMass));

                }

                double p1 = nToSelectMap.keySet().iterator().next();
                double p2 = nSelectedMap.keySet().iterator().next();
                System.out.println(p1 + " / " + p2 + " / " + (p1 == p2));

                throw new IllegalArgumentException("Not enough representative modification sites found.");

            } else if (nSelected > nToSelect) {

                throw new IllegalArgumentException("Selected more representative sites than necessary.");

            }
        }

        HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<String>>>> representativeToSecondaryMap = new HashMap<>(representativeSites.size());

        for (double modMass : representativeSites.keySet()) {

            HashMap<Integer, HashMap<Integer, HashSet<String>>> mapAtMass = new HashMap<>(1);
            representativeToSecondaryMap.put(modMass, mapAtMass);
            HashMap<Integer, HashSet<String>> representativeSitesAtMass = representativeSites.get(modMass);

            for (int representativeSite : representativeSitesAtMass.keySet()) {

                HashMap<Integer, HashSet<String>> siteMap = new HashMap<>(1);
                siteMap.put(representativeSite, representativeSitesAtMass.get(representativeSite));
                mapAtMass.put(representativeSite, siteMap);

            }

            HashMap<Integer, HashSet<String>> secondarySitesAtMass = secondarySites.get(modMass);

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

                    HashMap<Integer, HashSet<String>> representativeSiteMap = mapAtMass.get(representativeSite);
                    representativeSiteMap.put(secondarySite, secondarySitesAtMass.get(secondarySite));

                }
            }
        }

        return representativeToSecondaryMap;

    }

    /**
     * Scores PTMs in a protein match.
     *
     * @param identification The identification object containing the matches.
     * @param proteinMatch The protein match.
     * @param identificationParameters The identification parameters.
     * @param scorePeptides If true, peptides will be scored as well.
     * @param modificationProvider The modification provider to use.
     * @param waitingHandler The waiting handler to sue, ignored if null.
     */
    public void scorePTMs(
            Identification identification,
            ProteinMatch proteinMatch,
            IdentificationParameters identificationParameters,
            boolean scorePeptides,
            ModificationProvider modificationProvider,
            WaitingHandler waitingHandler
    ) {

        HashMap<Integer, ArrayList<String>> confidentSites = new HashMap<>();
        HashMap<Integer, HashMap<Integer, HashSet<String>>> ambiguousSites = new HashMap<>();

        for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            Peptide peptide = peptideMatch.getPeptide();
            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

            if (psParameter.getMatchValidationLevel().isValidated() && peptide.getNVariableModifications() > 0) {

                PSModificationScores peptideScores = (PSModificationScores) peptideMatch.getUrParam(PSModificationScores.dummy);

                if (peptideScores == null || scorePeptides) {

                    scorePTMs(
                            identification,
                            peptideMatch,
                            identificationParameters,
                            modificationProvider,
                            waitingHandler
                    );
                    peptideScores = (PSModificationScores) peptideMatch.getUrParam(PSModificationScores.dummy);

                }

                if (peptideScores != null) {

                    int[] peptideStart = peptide.getProteinMapping().get(proteinMatch.getLeadingAccession());

                    for (int confidentSite : peptideScores.getConfidentSites()) {

                        for (int peptideTempStart : peptideStart) {

                            int siteOnProtein = peptideTempStart + confidentSite - 1;
                            ArrayList<String> modificationsAtSite = confidentSites.get(siteOnProtein);

                            if (modificationsAtSite == null) {

                                modificationsAtSite = new ArrayList<>();
                                confidentSites.put(siteOnProtein, modificationsAtSite);

                            }

                            for (String modName : peptideScores.getConfidentModificationsAt(confidentSite)) {

                                if (!modificationsAtSite.contains(modName)) {

                                    modificationsAtSite.add(modName);

                                }
                            }
                        }
                    }

                    for (int representativeSite : peptideScores.getRepresentativeSites()) {

                        HashMap<Integer, HashSet<String>> peptideAmbiguousSites = peptideScores.getAmbiguousModificationsAtRepresentativeSite(representativeSite);

                        for (int peptideTempStart : peptideStart) {

                            int proteinRepresentativeSite = peptideTempStart + representativeSite - 1;
                            HashMap<Integer, HashSet<String>> proteinAmbiguousSites = ambiguousSites.get(proteinRepresentativeSite);

                            if (proteinAmbiguousSites == null) {

                                proteinAmbiguousSites = new HashMap<>(peptideAmbiguousSites.size());
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
        PSModificationScores proteinScores = new PSModificationScores();
        ArrayList<Integer> representativeSites = new ArrayList<>(ambiguousSites.keySet());
        Collections.sort(representativeSites);

        for (Integer representativeSite : representativeSites) {

            HashMap<Integer, HashSet<String>> secondarySitesMap = ambiguousSites.get(representativeSite);
            ArrayList<Integer> secondarySites = new ArrayList<>(secondarySitesMap.keySet());

            for (int secondarySite : secondarySites) {

                ArrayList<String> confidentModifications = confidentSites.get(secondarySite);

                if (confidentModifications != null) {

                    boolean sameModification = confidentModifications.stream()
                            .map(modName -> modificationProvider.getModification(modName))
                            .anyMatch(confidentModification -> secondarySitesMap.get(secondarySite).stream()
                            .map(modName -> modificationProvider.getModification(modName))
                            .anyMatch(secondaryModification -> secondaryModification.getMass() == confidentModification.getMass()));

                    if (sameModification) {

                        ambiguousSites.remove(representativeSite);
                        break;

                    }
                }

                if (secondarySite != representativeSite) {

                    ArrayList<Integer> tempRepresentativeSites = new ArrayList<>(ambiguousSites.keySet());
                    Collections.sort(tempRepresentativeSites);

                    for (Integer previousSite : tempRepresentativeSites) {

                        if (previousSite >= representativeSite) {

                            break;

                        }

                        if (previousSite == secondarySite) {

                            HashMap<Integer, HashSet<String>> previousSites = ambiguousSites.get(previousSite);
                            HashSet<String> previousModifications = previousSites.get(previousSite);
                            boolean sameModification = previousModifications.stream()
                                    .map(modName -> modificationProvider.getModification(modName))
                                    .anyMatch(previousModification -> secondarySitesMap.get(secondarySite).stream()
                                    .map(modName -> modificationProvider.getModification(modName))
                                    .anyMatch(secondaryModification -> secondaryModification.getMass() == previousModification.getMass()));

                            if (sameModification) {

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

            for (String modName : confidentSites.get(confidentSite)) {

                proteinScores.addConfidentModificationSite(modName, confidentSite);

            }
        }

        for (int representativeSite : ambiguousSites.keySet()) {

            proteinScores.addAmbiguousModificationSites(representativeSite, ambiguousSites.get(representativeSite));

        }

        proteinMatch.addUrParam(proteinScores);

    }

    /**
     * Scores the PTMs of all peptide matches contained in an identification
     * object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param modificationProvider The modification provider to use.
     * @param waitingHandler the handler displaying feedback to the user
     * @param identificationParameters the identification parameters
     */
    public void scorePeptidePtms(
            Identification identification,
            ModificationProvider modificationProvider,
            WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters
    ) {

        waitingHandler.setWaitingText("Scoring Peptide Modification Localization. Please Wait...");

        int max = identification.getPeptideIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        identification.getPeptideIdentification().stream()
                .map(key -> identification.getPeptideMatch(key))
                .forEach(peptideMatch -> {

                    scorePTMs(
                            identification,
                            peptideMatch,
                            identificationParameters,
                            modificationProvider,
                            waitingHandler
                    );

                    waitingHandler.increaseSecondaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                });

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Infers the modification site of the best peptide assumption of the given
     * spectrum match.
     *
     * @param spectrumMatch The spectrum match.
     * @param sequenceProvider The sequence provider to use.
     * @param modificationProvider The modification provider to use.
     * @param identificationParameters The identification parameters.
     */
    public void modificationSiteInference(
            SpectrumMatch spectrumMatch,
            SequenceProvider sequenceProvider,
            ModificationProvider modificationProvider,
            IdentificationParameters identificationParameters
    ) {

        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

        int nVariableModifications = peptide.getNVariableModifications();

        if (nVariableModifications > 0) {

            SearchParameters searchParameters = identificationParameters.getSearchParameters();
            PSModificationScores modificationScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);
            HashMap<Double, ArrayList<ModificationMatch>> modMatchesMap = new HashMap<>(nVariableModifications);
            HashMap<Double, HashMap<Integer, String>> possiblePositions = new HashMap<>(nVariableModifications);
            HashMap<Double, HashMap<Integer, ArrayList<String>>> confidentSites = new HashMap<>(nVariableModifications);

            for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

                String modName = modificationMatch.getModification();
                Modification modification = modificationProvider.getModification(modName);
                double modMass = modification.getMass();
                ArrayList<ModificationMatch> modOccurence = modMatchesMap.get(modMass);

                if (modOccurence == null) {

                    modOccurence = new ArrayList<>(1);
                    modMatchesMap.put(modMass, modOccurence);

                }

                HashMap<Integer, String> modPossibleSites = possiblePositions.get(modMass);

                if (modPossibleSites == null) {

                    modPossibleSites = new HashMap<>(1);
                    possiblePositions.put(modMass, modPossibleSites);

                }

                boolean maybeNotTerminal = modification.getModificationType() == ModificationType.modaa;

                if (!maybeNotTerminal) {

                    ArrayList<String> expectedModifications
                            = modificationFactory.getExpectedVariableModifications(searchParameters);

                    for (String otherModName : expectedModifications) {

                        if (!otherModName.equals(modName)) {

                            Modification otherModification = modificationProvider.getModification(otherModName);

                            if (otherModification.getMass() == modMass && modification.getModificationType() != otherModification.getModificationType()) {

                                maybeNotTerminal = true;
                                break;

                            }
                        }
                    }
                }

                if (maybeNotTerminal) {

                    modOccurence.add(modificationMatch);

                    for (String similarModName : modificationFactory.getSameMassNotFixedModifications(modMass, searchParameters)) {

                        Modification similarModification = modificationProvider.getModification(similarModName);

                        if (modification.getMass() == modMass) {

                            int[] possibleSites = ModificationUtils.getPossibleModificationSites(
                                    peptide,
                                    similarModification,
                                    sequenceProvider,
                                    identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters()
                            );

                            for (int pos : possibleSites) {

                                modPossibleSites.put(pos, similarModName);

                            }
                        }
                    }

                } else {

                    ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                    if (modificationScoring == null) {

                        modificationScoring = new ModificationScoring(modName);
                        modificationScores.addModificationScoring(modName, modificationScoring);

                    }

                    modificationScoring.setSiteConfidence(modificationMatch.getSite(), ModificationScoring.VERY_CONFIDENT);
                    modificationMatch.setConfident(true);
                    HashMap<Integer, ArrayList<String>> modificationSites = confidentSites.get(modification.getMass());

                    if (modificationSites == null) {

                        modificationSites = new HashMap<>(1);
                        confidentSites.put(modMass, modificationSites);

                    }

                    int site = modificationMatch.getSite();
                    ArrayList<String> modNames = modificationSites.get(site);

                    if (modNames == null) {

                        modNames = new ArrayList<>(1);
                        modificationSites.put(site, modNames);

                    }

                    modNames.add(modName);

                }
            }

            ModificationLocalizationParameters modificationScoringParameters = identificationParameters.getModificationLocalizationParameters();
            Set<Double> modMasses = modMatchesMap.keySet();
            TreeMap<Double, TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>>> ambiguousScoreToSiteMap = new TreeMap<>(); // p score -> d-score -> Map mod mass -> site -> list of modifications
            HashMap<Double, Integer> nRepresentativesMap = new HashMap<>(modMasses.size());
            ArrayList<ModificationMatch> assignedModifications = new ArrayList<>(peptide.getNVariableModifications());
            TreeMap<Double, TreeMap<Double, TreeMap<Double, TreeSet<Integer>>>> scoreToSiteMap = new TreeMap<>(); // p-score -> d-score -> mod mass -> list of posssible sites

            for (double modMass : modMasses) {

                ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modMass);
                int nMods = modificationMatches.size();
                HashMap<Integer, String> modificationPossibleSites = possiblePositions.get(modMass);
                int nPossibleSites = modificationPossibleSites.size();
                HashMap<Integer, ArrayList<String>> modificationConfidentSites = confidentSites.get(modMass);

                if (modificationConfidentSites == null) {

                    modificationConfidentSites = new HashMap<>(1);
                    confidentSites.put(modMass, modificationConfidentSites);

                }

                if (nPossibleSites < nMods) {

                    throw new IllegalArgumentException(
                            "The occurence of modification of mass " + modMass + " (" + modificationMatches.size()
                            + ") is higher than the number of possible sites (" + modificationPossibleSites.size() + ") on sequence " + peptide.getSequence()
                            + " in spectrum " + spectrumMatch.getKey() + "."
                    );

                } else if (modificationPossibleSites.size() == modificationMatches.size()) {

                    for (ModificationMatch modMatch : modificationMatches) {

                        String modName = modMatch.getModification();
                        int site = modMatch.getSite();
                        ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                        if (modificationScoring == null) {

                            modificationScoring = new ModificationScoring(modName);
                            modificationScores.addModificationScoring(modName, modificationScoring);

                        }

                        modificationScoring.setSiteConfidence(site, ModificationScoring.VERY_CONFIDENT);
                        modMatch.setConfident(true);
                        ArrayList<String> modificationsAtAA = modificationConfidentSites.get(site);

                        if (modificationsAtAA == null) {

                            modificationsAtAA = new ArrayList<>(1);
                            modificationConfidentSites.put(site, modificationsAtAA);

                        }

                        modificationsAtAA.add(modName);
                        assignedModifications.add(modMatch);

                    }

                } else if (!modificationScoringParameters.isProbabilisticScoreCalculation()) {

                    double pScore = 0; // no probabilistic score in that case

                    for (ModificationMatch modificationMatch : modificationMatches) {

                        String modName = modificationMatch.getModification();
                        ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                        int site = modificationMatch.getSite();

                        Double dScore = modificationScoring.getDeltaScore(site);

                        if (dScore == 0.0) {

                            modificationScoring.setSiteConfidence(site, ModificationScoring.RANDOM);
                            modificationMatch.setConfident(false);

                        } else if (dScore <= 95) {

                            modificationScoring.setSiteConfidence(site, ModificationScoring.DOUBTFUL);
                            modificationMatch.setConfident(false);

                        } else {

                            modificationScoring.setSiteConfidence(site, ModificationScoring.CONFIDENT);
                            modificationMatch.setConfident(true);
                            ArrayList<String> modificationsAtAA = modificationConfidentSites.get(site);

                            if (modificationsAtAA == null) {

                                modificationsAtAA = new ArrayList<>(1);
                                modificationConfidentSites.put(site, modificationsAtAA);

                            }

                            modificationsAtAA.add(modName);

                        }

                        if (!modificationMatch.getConfident()) {

                            TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>> pScoreAmbiguousMap = ambiguousScoreToSiteMap.get(pScore);

                            if (pScoreAmbiguousMap == null) {

                                pScoreAmbiguousMap = new TreeMap<>();
                                ambiguousScoreToSiteMap.put(pScore, pScoreAmbiguousMap);

                            }

                            TreeMap<Double, HashMap<Integer, HashSet<String>>> dScoreAmbiguousMap = pScoreAmbiguousMap.get(dScore);

                            if (dScoreAmbiguousMap == null) {

                                dScoreAmbiguousMap = new TreeMap<>();
                                pScoreAmbiguousMap.put(dScore, dScoreAmbiguousMap);

                            }

                            HashMap<Integer, HashSet<String>> massAmbiguousMap = dScoreAmbiguousMap.get(modMass);

                            if (massAmbiguousMap == null) {

                                massAmbiguousMap = new HashMap<>(nPossibleSites);
                                dScoreAmbiguousMap.put(modMass, massAmbiguousMap);

                            }

                            HashSet<String> modifications = massAmbiguousMap.get(site);

                            if (modifications == null) {

                                modifications = new HashSet<>(1);
                                massAmbiguousMap.put(site, modifications);

                            }

                            modifications.add(modName);

                        }

                        assignedModifications.add(modificationMatch);

                    }

                } else {

                    for (int site : modificationPossibleSites.keySet()) {

                        String modName = modificationPossibleSites.get(site);
                        ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);
                        double pScore = 0;
                        double dScore = 0;

                        if (modificationScoring != null) {

                            pScore = modificationScoring.getProbabilisticScore(site);
                            dScore = modificationScoring.getDeltaScore(site);

                        }

                        TreeMap<Double, TreeMap<Double, TreeSet<Integer>>> pScoreMap = scoreToSiteMap.get(pScore);

                        if (pScoreMap == null) {

                            pScoreMap = new TreeMap<>();
                            scoreToSiteMap.put(pScore, pScoreMap);

                        }

                        TreeMap<Double, TreeSet<Integer>> dScoreMap = pScoreMap.get(dScore);

                        if (dScoreMap == null) {

                            dScoreMap = new TreeMap<>();
                            pScoreMap.put(dScore, dScoreMap);

                        }

                        TreeSet<Integer> dScoreSites = dScoreMap.get(modMass);

                        if (dScoreSites == null) {

                            dScoreSites = new TreeSet<>();
                            dScoreMap.put(modMass, dScoreSites);

                        }

                        dScoreSites.add(site);

                    }
                }
            }
            
            // Assign modifications to the sites passing the threshold.
            if (!scoreToSiteMap.isEmpty()) {

                HashMap<Double, Integer> processedSitesMap = new HashMap<>(modMasses.size());

                for (Entry<Double, TreeMap<Double, TreeMap<Double, TreeSet<Integer>>>> entry1 : scoreToSiteMap.descendingMap().entrySet()) {

                    double pScore = entry1.getKey();
                    TreeMap<Double, TreeMap<Double, TreeSet<Integer>>> pScoreMap = entry1.getValue();

                    for (Entry<Double, TreeMap<Double, TreeSet<Integer>>> entry2 : pScoreMap.descendingMap().entrySet()) {

                        double dScore = entry2.getKey();
                        TreeMap<Double, TreeSet<Integer>> dScoreMap = entry2.getValue();

                        for (Entry<Double, TreeSet<Integer>> entry : dScoreMap.entrySet()) {

                            double modificationMass = entry.getKey();

                            ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modificationMass);
                            HashMap<Integer, String> modificationPossibleSites = possiblePositions.get(modificationMass);
                            HashMap<Integer, ArrayList<String>> modificationConfidentSites = confidentSites.get(modificationMass);
                            int nMods = modificationMatches.size();
                            int nPossibleSites = modificationPossibleSites.size();

                            double randomScoreThreshold = modificationScoringParameters.getSelectedProbabilisticScore().getRandomThreshold(nMods, nPossibleSites);
                            double confidenceThreshold = modificationScoringParameters.getProbabilisticScoreThreshold();

                            TreeSet<Integer> sites = dScoreMap.get(modificationMass);

                            Integer nAssignedSites = processedSitesMap.get(modificationMass);

                            if (nAssignedSites == null) {

                                nAssignedSites = 0;

                            }

                            for (int site : sites) {

                                String modName = modificationPossibleSites.get(site);
                                ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                                if (modificationScoring == null) {

                                    modificationScoring = new ModificationScoring(modName);
                                    modificationScores.addModificationScoring(modName, modificationScoring);

                                }

                                ModificationMatch modificationMatch = null;

                                if (nAssignedSites < nMods) {

                                    boolean alreadyOccupied = false;

                                    for (ModificationMatch assignedMatch : assignedModifications) {

                                        if (assignedMatch.getSite() == site) {

                                            alreadyOccupied = true;
                                            break;

                                        }
                                    }

                                    if (!alreadyOccupied) {

                                        if (pScore >= confidenceThreshold) {

                                            modificationMatch = modificationMatches.get(nAssignedSites);
                                            modificationMatch.setSite(site);
                                            modificationMatch.setModification(modName);
                                            assignedModifications.add(modificationMatch);

                                            modificationScoring.setSiteConfidence(site, ModificationScoring.VERY_CONFIDENT);
                                            modificationMatch.setConfident(true);

                                            ArrayList<String> modificationsAtAA = modificationConfidentSites.get(site);

                                            if (modificationsAtAA == null) {

                                                modificationsAtAA = new ArrayList<>(1);
                                                modificationConfidentSites.put(site, modificationsAtAA);

                                            }

                                            modificationsAtAA.add(modName);

                                            nAssignedSites++;
                                            processedSitesMap.put(modificationMass, nAssignedSites);

                                        } else if (pScore > randomScoreThreshold) {

                                            modificationScoring.setSiteConfidence(site, ModificationScoring.DOUBTFUL);

                                        } else {

                                            modificationScoring.setSiteConfidence(site, ModificationScoring.RANDOM);

                                        }
                                    }
                                }

                                if (modificationMatch == null) {

                                    TreeMap<Double, TreeMap<Double, HashMap<Integer, HashSet<String>>>> pScoreAmbiguousMap = ambiguousScoreToSiteMap.get(pScore);

                                    if (pScoreAmbiguousMap == null) {

                                        pScoreAmbiguousMap = new TreeMap<>();
                                        ambiguousScoreToSiteMap.put(pScore, pScoreAmbiguousMap);

                                    }

                                    TreeMap<Double, HashMap<Integer, HashSet<String>>> dScoreAmbiguousMap = pScoreAmbiguousMap.get(dScore);

                                    if (dScoreAmbiguousMap == null) {

                                        dScoreAmbiguousMap = new TreeMap<>();
                                        pScoreAmbiguousMap.put(dScore, dScoreAmbiguousMap);

                                    }

                                    HashMap<Integer, HashSet<String>> massAmbiguousMap = dScoreAmbiguousMap.get(modificationMass);

                                    if (massAmbiguousMap == null) {

                                        massAmbiguousMap = new HashMap<>(nPossibleSites);
                                        dScoreAmbiguousMap.put(modificationMass, massAmbiguousMap);

                                    }

                                    HashSet<String> modifications = massAmbiguousMap.get(site);

                                    if (modifications == null) {

                                        modifications = new HashSet<>(1);
                                        massAmbiguousMap.put(site, modifications);

                                    }

                                    modifications.add(modName);

                                }
                            }
                        }
                    }
                }
            }

            // See whether there are sites that are not confidently assigned.
            for (double modMass : confidentSites.keySet()) {

                HashMap<Integer, ArrayList<String>> modConfidentSitesMap = confidentSites.get(modMass);
                ArrayList<Integer> modConfidentSites = new ArrayList<>(modConfidentSitesMap.keySet());
                int nConfident = 0;

                for (int site : modConfidentSites) {

                    ArrayList<String> modifications = modConfidentSitesMap.get(site);

                    for (String modName : modifications) {

                        modificationScores.addConfidentModificationSite(modName, site);

                    }

                    nConfident += modifications.size();

                }

                modConfidentSites.size();
                ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modMass);

                int nMods = modificationMatches.size();

                if (nConfident < nMods) {

                    int nRepresentatives = nMods - nConfident;

                    if (nRepresentatives > 0) {

                        nRepresentativesMap.put(modMass, nRepresentatives);

                    }
                }
            }

            // Create ambiguous site groups and assign the remaining modifications to the best scoring sites.
            if (!nRepresentativesMap.isEmpty()) {

                HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<String>>>> representativeToSecondaryMap = getRepresentativeToSecondaryMap(ambiguousScoreToSiteMap, nRepresentativesMap);

                for (Entry<Double, HashMap<Integer, HashMap<Integer, HashSet<String>>>> entry1 : representativeToSecondaryMap.entrySet()) {

                    double modMass = entry1.getKey();
                    HashMap<Integer, HashMap<Integer, HashSet<String>>> massMap = entry1.getValue();

                    ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modMass);
                    int nMods = modificationMatches.size();

                    int nRepresentativeSites = nRepresentativesMap.get(modMass);

                    if (nRepresentativeSites != massMap.size()) {

                        throw new IllegalArgumentException("Unexpected number of representative sites.");

                    }

                    int nConfident = nMods - nRepresentativeSites;

                    int modMatchIndex = nConfident;

                    for (Entry<Integer, HashMap<Integer, HashSet<String>>> entry : massMap.entrySet()) {

                        int representativeSite = entry.getKey();
                        HashMap<Integer, HashSet<String>> sites = entry.getValue();
                        modificationScores.addAmbiguousModificationSites(representativeSite, sites);

                        ModificationMatch modificationMatch = modificationMatches.get(modMatchIndex);

                        String modName = null;

                        for (String possibleModName : sites.get(representativeSite)) {

                            Modification possibleModification = modificationProvider.getModification(possibleModName);

                            if (possibleModification.getMass() == modMass) {

                                modName = possibleModName;
                                break;

                            }
                        }

                        if (modName == null) {

                            throw new IllegalArgumentException("No candidate modification of mass " + modMass + " found at site " + representativeSite + " on peptide " + peptide.getSequence() + ".");

                        }

                        modificationMatch.setModification(modName);
                        modificationMatch.setSite(representativeSite);
                        modificationMatch.setConfident(false);

                        modMatchIndex++;

                    }
                }
            }
        }
    }
}
