package eu.isas.peptideshaker.ptm;

import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationProvider;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.ModificationLocalizationScore;
import com.compomics.util.experiment.identification.modification.peptide_mapping.ModificationPeptideMapping;
import com.compomics.util.experiment.identification.modification.peptide_mapping.performance.HistoneExample;
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
import com.compomics.util.experiment.identification.utils.PeptideUtils;
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
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * Localization score offset for modification that are confidently
     * localized.
     */
    public final int CONFIDENT_OFFSET = 200;
    /**
     * Localization score offset for modification that are inferred.
     */
    public final int INFERRED_OFFSET = 100;

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
                            peptideSpectrumAnnotator,
                            modificationProvider
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
     * @param sequenceProvider The sequence matching parameters.
     * @param waitingHandler The waiting handler to use, ignored if null.
     */
    public void scorePTMs(
            Identification identification,
            PeptideMatch peptideMatch,
            IdentificationParameters identificationParameters,
            ModificationProvider modificationProvider,
            SequenceProvider sequenceProvider,
            WaitingHandler waitingHandler
    ) {

        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        ModificationParameters modificationParameters = searchParameters.getModificationParameters();
        ModificationLocalizationParameters ModificationLocalizationParameters = identificationParameters.getModificationLocalizationParameters();

        Peptide peptide = peptideMatch.getPeptide();
        ModificationMatch[] originalMatches = peptide.getVariableModifications();

        if (originalMatches.length == 0) {
            return;
        }

        long originalKey = peptide.getMatchingKey(sequenceMatchingParameters);

        HashMap<Double, Integer> modificationOccurence = new HashMap<>(originalMatches.length);

        for (ModificationMatch modMatch : originalMatches) {

            String modName = modMatch.getModification();
            Modification modification = modificationProvider.getModification(modName);
            double modMass = modification.getMass();

            Integer occurrence = modificationOccurence.get(modMass);

            if (occurrence == null) {

                modificationOccurence.put(modMass, 1);

            } else {

                modificationOccurence.put(modMass, occurrence + 1);

            }
        }

        // Gather the scores from the different PSMs
        PSModificationScores peptideScores = new PSModificationScores();

        HashMap<Double, HashMap<Integer, Double>> modificationToSiteToScore = new HashMap<>(originalMatches.length);
        HashMap<Double, HashMap<Integer, String>> modificationToSiteToName = new HashMap<>(originalMatches.length);

        for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            PSModificationScores psmScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);

            for (String modName : psmScores.getScoredModifications()) {

                Modification modification = modificationProvider.getModification(modName);
                double modMass = modification.getMass();

                if (modificationOccurence.containsKey(modMass)) {

                    HashMap<Integer, Double> siteToScore = modificationToSiteToScore.get(modMass);
                    HashMap<Integer, String> siteToName = modificationToSiteToName.get(modMass);

                    if (siteToScore == null) {

                        siteToScore = new HashMap<>(2);
                        modificationToSiteToScore.put(modMass, siteToScore);

                        siteToName = new HashMap<>(2);
                        modificationToSiteToName.put(modMass, siteToName);

                    }

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

                        double psmScore = ModificationLocalizationParameters.isProbabilisticScoreCalculation() ? psmPScore : psmDScore;

                        Double currentScore = siteToScore.get(site);

                        if (currentScore == null) {

                            siteToScore.put(site, psmScore);
                            siteToName.put(site, modName);

                        } else {

                            siteToScore.put(site, Math.max(currentScore, psmScore));

                        }
                    }
                }
            }

            for (ModificationMatch modificationMatch : spectrumMatch.getBestPeptideAssumption().getPeptide().getVariableModifications()) {

                if (modificationMatch.getConfident()) {

                    double modMass = modificationProvider.getModification(modificationMatch.getModification()).getMass();

                    if (modificationOccurence.containsKey(modMass)) {

                        int site = modificationMatch.getSite();

                        HashMap<Integer, Double> siteToScore = modificationToSiteToScore.get(modMass);
                        double currentScore = siteToScore.get(site);
                        siteToScore.put(site, currentScore + CONFIDENT_OFFSET);

                    }
                } else if (modificationMatch.getInferred()) {

                    double modMass = modificationProvider.getModification(modificationMatch.getModification()).getMass();

                    if (modificationOccurence.containsKey(modMass)) {

                        int site = modificationMatch.getSite();

                        HashMap<Integer, Double> siteToScore = modificationToSiteToScore.get(modMass);
                        double currentScore = siteToScore.get(site);
                        siteToScore.put(site, currentScore + INFERRED_OFFSET);

                    }
                }
            }
        }

        HashMap<Double, int[]> modificationToPossibleSiteMap = new HashMap<>(modificationToSiteToScore.size());

        for (Entry<Double, HashMap<Integer, Double>> entry : modificationToSiteToScore.entrySet()) {

            double modMass = entry.getKey();
            int[] possibleSites = entry.getValue().keySet().stream()
                    .mapToInt(
                            a -> a
                    )
                    .toArray();

            modificationToPossibleSiteMap.put(modMass, possibleSites);

        }

            // Map the modifications to the best scoring sites
        HashMap<Double, TreeSet<Integer>> mapping = ModificationPeptideMapping.mapModifications(modificationToPossibleSiteMap, modificationOccurence, modificationToSiteToScore);

        // Update the modifications of the peptide accordingly
        ModificationMatch[] newModificationMatches = new ModificationMatch[originalMatches.length];

        int modI = 0;

        for (Entry<Double, TreeSet<Integer>> mappingEntry : mapping.entrySet()) {

            double modMass = mappingEntry.getKey();

            for (int site : mappingEntry.getValue()) {

                String modName = modificationToSiteToName.get(modMass).get(site);

                ModificationMatch modificationMatch = new ModificationMatch(modName, site);

                double score = modificationToSiteToScore.get(modMass).get(site);

                if (score > CONFIDENT_OFFSET) {

                    modificationMatch.setConfident(true);

                } else if (score > INFERRED_OFFSET) {

                    modificationMatch.setInferred(true);

                }

                newModificationMatches[modI] = modificationMatch;
                modI++;

            }
        }

        if (modI < originalMatches.length) {

            throw new IllegalArgumentException(modI + " modifications found where " + originalMatches.length + " needed.");

        }

        peptide.setVariableModifications(newModificationMatches);

        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        double mass = peptide.getMass(
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters
        );
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
     * Scores PTMs in a protein match.
     *
     * @param identification The identification object containing the matches.
     * @param proteinMatch The protein match.
     * @param identificationParameters The identification parameters.
     * @param scorePeptides If true, peptides will be scored as well.
     * @param modificationProvider The modification provider to use.
     * @param sequenceProvider The sequence provider to use.
     * @param waitingHandler The waiting handler to sue, ignored if null.
     */
    public void scorePTMs(
            Identification identification,
            ProteinMatch proteinMatch,
            IdentificationParameters identificationParameters,
            boolean scorePeptides,
            ModificationProvider modificationProvider,
            SequenceProvider sequenceProvider,
            WaitingHandler waitingHandler
    ) {

        // Gather the modifications from the peptides
        HashMap<Integer, HashMap<Double, String>> confidentSites = new HashMap<>();
        HashMap<Integer, HashMap<Double, String>> ambiguousSites = new HashMap<>();

        for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            Peptide peptide = peptideMatch.getPeptide();
            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

            if (psParameter.getMatchValidationLevel().isValidated() && peptide.getNVariableModifications() > 0) {

                if (scorePeptides) {

                    scorePTMs(
                            identification,
                            peptideMatch,
                            identificationParameters,
                            modificationProvider,
                            sequenceProvider,
                            waitingHandler
                    );

                }

                int[] peptideStart = peptide.getProteinMapping().get(proteinMatch.getLeadingAccession());

                for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

                    String modName = modificationMatch.getModification();
                    double modMass = modificationProvider.getModification(modName).getMass();

                    int siteOnPeptide = PeptideUtils.getModifiedAaIndex(modificationMatch.getSite(), peptide.getSequence().length());

                    if (modificationMatch.getConfident()) {

                        for (int peptideTempStart : peptideStart) {

                            int siteOnProtein = peptideTempStart + siteOnPeptide - 1;

                            HashMap<Double, String> modificationsAtSite = confidentSites.get(siteOnProtein);

                            if (modificationsAtSite == null) {

                                modificationsAtSite = new HashMap<>(1);
                                confidentSites.put(siteOnProtein, modificationsAtSite);

                            }

                            modificationsAtSite.put(modMass, modName);

                        }
                    } else {

                        for (int peptideTempStart : peptideStart) {

                            int siteOnProtein = peptideTempStart + siteOnPeptide - 1;

                            HashMap<Double, String> modificationsAtSite = ambiguousSites.get(siteOnProtein);

                            if (modificationsAtSite == null) {

                                modificationsAtSite = new HashMap<>(1);
                                ambiguousSites.put(siteOnProtein, modificationsAtSite);

                            }

                            modificationsAtSite.put(modMass, modName);

                        }
                    }
                }
            }
        }
        
        if (confidentSites.isEmpty() && ambiguousSites.isEmpty()) {
            
            return;
            
        }

        // Create protein modification matches
        ArrayList<ModificationMatch> modificationsList = new ArrayList<>(confidentSites.size());

        for (Entry<Integer, HashMap<Double, String>> entry1 : confidentSites.entrySet()) {

            int site = entry1.getKey();

            for (Entry<Double, String> entry2 : entry1.getValue().entrySet()) {

                double mass = entry2.getKey();
                String modName = entry2.getValue();

                ModificationMatch modificationMatch = new ModificationMatch(modName, site);
                modificationMatch.setConfident(true);
                modificationsList.add(modificationMatch);

            }
        }

        for (Entry<Integer, HashMap<Double, String>> entry1 : ambiguousSites.entrySet()) {

            int site = entry1.getKey();
            HashMap<Double, String> confidentModificationsAtSite = confidentSites.get(site);

            for (Entry<Double, String> entry2 : entry1.getValue().entrySet()) {

                double mass = entry2.getKey();
                String modName = entry2.getValue();

                if (confidentModificationsAtSite == null || !confidentModificationsAtSite.containsKey(mass)) {

                    ModificationMatch modificationMatch = new ModificationMatch(modName, site);
                    modificationMatch.setConfident(false);
                    modificationsList.add(modificationMatch);

                }
            }
        }

        ModificationMatch[] modificationMatches = modificationsList.stream().toArray(ModificationMatch[]::new);
        proteinMatch.setVariableModifications(modificationMatches);

    }

    /**
     * Scores the PTMs of all peptide matches contained in an identification
     * object.
     *
     * @param identification identification object containing the identification
     * matches
     * @param modificationProvider The modification provider to use.
     * @param sequenceProvider The sequence provider to use.
     * @param waitingHandler the handler displaying feedback to the user
     * @param identificationParameters the identification parameters
     */
    public void scorePeptidePtms(
            Identification identification,
            ModificationProvider modificationProvider,
            SequenceProvider sequenceProvider,
            WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters
    ) {

        waitingHandler.setWaitingText("Scoring Peptide Modification Localization. Please Wait...");

        int max = identification.getPeptideIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        identification.getPeptideIdentification().stream()
                .map(
                        key -> identification.getPeptideMatch(key)
                )
                .forEach(
                        peptideMatch -> {

                            // Aggregate PSM scores into peptide scores
                            scorePTMs(
                                    identification,
                                    peptideMatch,
                                    identificationParameters,
                                    modificationProvider,
                                    sequenceProvider,
                                    waitingHandler
                            );

                            // Check that there is only one variable modification per residue
                            peptideMatch.getPeptide().getIndexedVariableModifications();

                            waitingHandler.increaseSecondaryProgressCounter();

                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                        }
                );

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

            for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

                String modName = modificationMatch.getModification();
                Modification modification = modificationProvider.getModification(modName);
                double modMass = modification.getMass();

                ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modMass);

                if (modificationMatches == null) {

                    modificationMatches = new ArrayList<>(1);
                    modMatchesMap.put(modMass, modificationMatches);

                }

                HashMap<Integer, String> modPossibleSites = possiblePositions.get(modMass);

                if (modPossibleSites == null) {

                    modPossibleSites = new HashMap<>(1);
                    possiblePositions.put(modMass, modPossibleSites);

                }

                modificationMatches.add(modificationMatch);

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
            }

            // Gather the modifications that need to be distributed according to their score, assign the sites to the others
            ModificationLocalizationParameters modificationScoringParameters = identificationParameters.getModificationLocalizationParameters();
            Set<Double> modMasses = modMatchesMap.keySet();
            ArrayList<ModificationMatch> assignedModifications = new ArrayList<>(peptide.getNVariableModifications());
            HashMap<Double, int[]> modificationToPossibleSiteMap = new HashMap<>(nVariableModifications);
            HashMap<Double, Integer> modificationOccurrenceMap = new HashMap<>(nVariableModifications);
            HashMap<Double, HashMap<Integer, Double>> modificationToSiteToScore = new HashMap<>(nVariableModifications);

            for (double modMass : modMasses) {

                ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modMass);
                int nMods = modificationMatches.size();
                HashMap<Integer, String> modificationPossibleSites = possiblePositions.get(modMass);
                int nPossibleSites = modificationPossibleSites.size();

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

                        assignedModifications.add(modMatch);

                    }

                } else {

                    int[] sites = modificationPossibleSites.keySet().stream()
                            .mapToInt(a -> a)
                            .toArray();

                    modificationToPossibleSiteMap.put(modMass, sites);

                    modificationOccurrenceMap.put(modMass, nMods);

                    HashMap<Integer, Double> siteToScoreMap = modificationToSiteToScore.get(modMass);

                    if (siteToScoreMap == null) {

                        siteToScoreMap = new HashMap<>(sites.length);
                        modificationToSiteToScore.put(modMass, siteToScoreMap);

                    }

                    for (int site : sites) {

                        String modName = modificationPossibleSites.get(site);
                        ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                        double score = 0;

                        if (modificationScoring != null) {

                            score = modificationScoringParameters.isProbabilisticScoreCalculation()
                                    ? modificationScoring.getProbabilisticScore(site) : modificationScoring.getDeltaScore(site);

                        }

                        siteToScoreMap.put(site, score);

                    }
                }
            }

            if (!modificationToSiteToScore.isEmpty()) {

                /*System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.println("Peptide sequence: " + peptide.getSequence());
                
                for (Double modID: modificationToPossibleSiteMap.keySet()) {
                    
                    int[] sites = modificationToPossibleSiteMap.get(modID);
                    
                    ArrayList<ModificationMatch> modificationMatches_toprint = modMatchesMap.get(modID);
                    ArrayList<String> modNames_toprint = new ArrayList<>();
                    for (int i = 0; i < modificationMatches_toprint.size(); i++){
                        String modName_toprint = modificationMatches_toprint.get(i).getModification();
                        modNames_toprint.add(modName_toprint);
                    }
                    
                    //display array
                    System.out.println(modNames_toprint);
                    System.out.println( Arrays.toString(sites));
                
                }
                
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");*/
                // Distribute modifications among acceptor sites based on their score
                HashMap<Double, TreeSet<Integer>> matchedSiteByModification = ModificationPeptideMapping.mapModifications(
                        modificationToPossibleSiteMap,
                        modificationOccurrenceMap,
                        modificationToSiteToScore
                );
                
                HistoneExample.exportHistoneData(peptide.getSequence(), modificationToPossibleSiteMap, modificationOccurrenceMap, modificationToSiteToScore, matchedSiteByModification, spectrumMatch.getBestPeptideAssumption().getRawScore());

                /*for (Double modID: matchedSiteByModification.keySet()) {
                    
                    TreeSet<Integer> sites = matchedSiteByModification.get(modID);
                
                    //convert TreeSet to an array
                    Integer[] array = sites.toArray( new Integer[sites.size()] );
                    ArrayList<ModificationMatch> modificationMatches_toprint = modMatchesMap.get(modID);
                    ArrayList<String> modNames_toprint = new ArrayList<>();
                    for (int i = 0; i < modificationMatches_toprint.size(); i++){
                        String modName_toprint = modificationMatches_toprint.get(i).getModification();
                        modNames_toprint.add(modName_toprint);
                    }
                    
                    //display array
                    System.out.println(modNames_toprint);
                    System.out.println( Arrays.toString(array) );
                
                }
                
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");*/
                // Assign confidence levels to the sites mapped
                for (Entry<Double, TreeSet<Integer>> entry : matchedSiteByModification.entrySet()) {

                    double modificationMass = entry.getKey();
                    int[] sortedSelectedSites = entry.getValue().stream().mapToInt(a -> a).toArray();
                    int[] sortedPossibleSites = modificationToSiteToScore.get(modificationMass).keySet().stream().mapToInt(a -> a).toArray();

                    ArrayList<ModificationMatch> modificationMatches = modMatchesMap.get(modificationMass);

                    if (sortedSelectedSites.length > modificationMatches.size()) {

                        throw new IllegalArgumentException("More sites than modifications found when assigning confidence levels at mass " + modificationMass + ".");

                    }

                    HashMap<Integer, String> modificationPossibleSites = possiblePositions.get(modificationMass);
                    int nMods = modificationMatches.size();
                    int nPossibleSites = modificationPossibleSites.size();

                    double randomScoreThreshold = modificationScoringParameters.getSelectedProbabilisticScore().getRandomThreshold(nMods, nPossibleSites);
                    double confidenceThreshold = modificationScoringParameters.getProbabilisticScoreThreshold();
                    double dThreshold = modificationScoringParameters.getDScoreThreshold();

                    for (int siteI = 0; siteI < sortedSelectedSites.length; siteI++) {

                        int site = sortedSelectedSites[siteI];

                        String modName = modificationPossibleSites.get(site);

                        ModificationScoring modificationScoring = modificationScores.getModificationScoring(modName);

                        if (modificationScoring == null) {

                            modificationScoring = new ModificationScoring(modName);
                            modificationScores.addModificationScoring(modName, modificationScoring);

                        }

                        ModificationMatch modificationMatch = new ModificationMatch(modName, site);
                        assignedModifications.add(modificationMatch);

                        double score = modificationToSiteToScore.get(modificationMass).get(site);

                        if (modificationScoringParameters.isProbabilisticScoreCalculation() && score >= confidenceThreshold
                                || !modificationScoringParameters.isProbabilisticScoreCalculation() && score >= dThreshold) {

                            modificationScoring.setSiteConfidence(site, ModificationScoring.CONFIDENT);
                            modificationMatch.setConfident(true);

                        } else if (modificationScoringParameters.isProbabilisticScoreCalculation() && score > randomScoreThreshold) {

                            modificationScoring.setSiteConfidence(site, ModificationScoring.DOUBTFUL);
                            modificationMatch.setConfident(false);

                        } else {

                            modificationScoring.setSiteConfidence(site, ModificationScoring.RANDOM);
                            modificationMatch.setConfident(false);

                        }
                    }
                }
            }

            ModificationMatch[] modificationMatches = assignedModifications.stream().toArray(ModificationMatch[]::new);

            peptide.setVariableModifications(modificationMatches);

        }
    }
}
