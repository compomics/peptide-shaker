package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This class contains the method for PSM best hit selection.
 *
 * @author Marc Vaudel
 */
public class BestMatchSelection {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * Map indicating how often a protein was found in a search engine first hit
     * whenever this protein was found more than one time.
     */
    private final HashMap<String, Integer> proteinCount;
    /**
     * The sequence provider
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The FASTA parameters.
     */
    private final FastaParameters fastaParameters;

    /**
     * Constructor.
     *
     * @param proteinCount a map of proteins found multiple times
     * @param sequenceProvider the sequence provider
     * @param fastaParameters the FASTA parameters
     */
    public BestMatchSelection(
            HashMap<String, Integer> proteinCount,
            SequenceProvider sequenceProvider,
            FastaParameters fastaParameters) {

        this.proteinCount = proteinCount;
        this.sequenceProvider = sequenceProvider;
        this.fastaParameters = fastaParameters;

    }

    /**
     * Selects the best hit.
     *
     * @param spectrumMatch the spectrum match
     * @param inputMap the input map
     * @param psmTargetDecoyMap the PSM target decoy map
     * @param waitingHandler the handler displaying feedback to the user
     * @param identificationParameters the identification parameters
     */
    public void selectBestHit(
            SpectrumMatch spectrumMatch,
            InputMap inputMap,
            TargetDecoyMap psmTargetDecoyMap,
            WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters
    ) {

        PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
        boolean multiSE = inputMap.isMultipleAlgorithms();

        PeptideAssumptionFilter peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();
        SequenceMatchingParameters ptmSequenceMatchingPreferences = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        PSParameter psmParameter = new PSParameter();
        psmParameter.setMatchValidationLevel(MatchValidationLevel.none);
        spectrumMatch.addUrParam(psmParameter);

        String spectrumKey = spectrumMatch.getSpectrumKey();

        // map of the peptide first hits for this spectrum: score -> max protein count -> max search engine votes -> amino acids annotated -> min mass deviation -> peptide sequence
        TreeMap<Double, TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>>>>> validAssumptions = new TreeMap<>();

        HashSet<Long> ids = new HashSet<>(2);

        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = spectrumMatch.getPeptideAssumptionsMap();

        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry1 : assumptions.entrySet()) {

            int searchEngine1 = entry1.getKey();
            TreeMap<Double, ArrayList<PeptideAssumption>> advocate1Map = entry1.getValue();

            searchEngine1loop:

            for (Entry<Double, ArrayList<PeptideAssumption>> advocate1Entry : advocate1Map.entrySet()) {

                for (PeptideAssumption peptideAssumption1 : advocate1Entry.getValue()) {

                    Peptide peptide1 = peptideAssumption1.getPeptide();
                    long id = peptide1.getKey();

                    if (!ids.contains(id)) {

                        ids.add(id);

                        if (peptide1.getProteinMapping() != null
                                && !peptide1.getProteinMapping().isEmpty()
                                && peptideAssumptionFilter.validatePeptide(peptide1, sequenceMatchingPreferences, searchParameters.getDigestionParameters())
                                && peptideAssumptionFilter.validateModifications(peptide1, sequenceMatchingPreferences, ptmSequenceMatchingPreferences, searchParameters.getModificationParameters())
                                && peptideAssumptionFilter.validatePrecursor(peptideAssumption1, spectrumKey, spectrumFactory, searchParameters)
                                && peptideAssumptionFilter.validateProteins(peptide1, sequenceMatchingPreferences, sequenceProvider)) {

                            PSParameter psParameter1 = (PSParameter) peptideAssumption1.getUrParam(PSParameter.dummy);

                            double p = multiSE && fastaParameters.isTargetDecoy()
                                    ? psParameter1.getProbability()
                                    : peptideAssumption1.getScore();

                            int nSE = 1;
                            int proteinCountMax = peptideAssumption1.getPeptide().getProteinMapping().navigableKeySet().stream()
                                    .filter(accession -> proteinCount.containsKey(accession))
                                    .mapToInt(accession -> proteinCount.get(accession))
                                    .max()
                                    .orElse(1);

                            searchEngine2loop:

                            for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry2 : assumptions.entrySet()) {

                                int searchEngine2 = entry2.getKey();

                                if (searchEngine1 != searchEngine2) {

                                    TreeMap<Double, ArrayList<PeptideAssumption>> advocate2Map = entry2.getValue();

                                    for (Entry<Double, ArrayList<PeptideAssumption>> advocate2Entry : advocate2Map.entrySet()) {

                                        for (PeptideAssumption peptideAssumption2 : advocate2Entry.getValue()) {

                                            if (peptideAssumption1.getPeptide().isSameSequenceAndModificationStatus(peptideAssumption2.getPeptide(),
                                                    sequenceMatchingPreferences)) {

                                                PSParameter psParameter2 = (PSParameter) peptideAssumption2.getUrParam(PSParameter.dummy);
                                                p *= psParameter2.getProbability();
                                                nSE++;

                                                break searchEngine2loop;

                                            }
                                        }
                                    }
                                }
                            }

                            psmTargetDecoyMap.put(p, PeptideUtils.isDecoy(peptide1, sequenceProvider));

                            TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>>>> pMap = validAssumptions.get(p);

                            if (pMap == null) {

                                pMap = new TreeMap<>();
                                validAssumptions.put(p, pMap);

                            }

                            TreeMap<Integer, TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>>> proteinMaxMap = pMap.get(proteinCountMax);

                            if (proteinMaxMap == null) {

                                proteinMaxMap = new TreeMap<>();
                                pMap.put(proteinCountMax, proteinMaxMap);

                            }

                            TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>> nSeMap = proteinMaxMap.get(nSE);

                            if (nSeMap == null) {

                                nSeMap = new TreeMap<>();
                                proteinMaxMap.put(nSE, nSeMap);
                                TreeMap<Double, TreeMap<Long, PeptideAssumption>> coverageMap = new TreeMap<>();
                                nSeMap.put(-1, coverageMap);
                                TreeMap<Long, PeptideAssumption> assumptionMap = new TreeMap<>();
                                coverageMap.put(-1.0, assumptionMap);
                                assumptionMap.put(peptideAssumption1.getPeptide().getKey(), peptideAssumption1);

                            } else {

                                Spectrum spectrum = (Spectrum) spectrumFactory.getSpectrum(spectrumKey);

                                TreeMap<Double, TreeMap<Long, PeptideAssumption>> coverageMap = nSeMap.get(-1);

                                if (coverageMap != null) {

                                    TreeMap<Long, PeptideAssumption> assumptionMap = coverageMap.get(-1.0);

                                    if (assumptionMap.size() > 1) {

                                        throw new IllegalArgumentException("Tie during best match selection in spectrum " + spectrumMatch.getKey() + ".");

                                    }

                                    PeptideAssumption peptideAssumption = assumptionMap.firstEntry().getValue();
                                    Peptide peptide = peptideAssumption.getPeptide();
                                    SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(
                                            spectrum.getSpectrumKey(),
                                            peptideAssumption,
                                            modificationParameters,
                                            sequenceProvider,
                                            modificationSequenceMatchingParameters,
                                            spectrumAnnotator
                                    );
                                    Map<Integer, ArrayList<IonMatch>> coveredAminoAcids = spectrumAnnotator.getCoveredAminoAcids(
                                            annotationPreferences,
                                            specificAnnotationPreferences,
                                            spectrum,
                                            peptide,
                                            modificationParameters,
                                            sequenceProvider,
                                            modificationSequenceMatchingParameters,
                                            true
                                    );
                                    int nIons = coveredAminoAcids.size();
                                    nSeMap.put(nIons, coverageMap);
                                    nSeMap.remove(-1);

                                }

                                Peptide peptide = peptideAssumption1.getPeptide();
                                SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(
                                        spectrum.getSpectrumKey(),
                                        peptideAssumption1,
                                        modificationParameters,
                                        sequenceProvider,
                                        modificationSequenceMatchingParameters,
                                        spectrumAnnotator
                                );
                                Map<Integer, ArrayList<IonMatch>> coveredAminoAcids = spectrumAnnotator.getCoveredAminoAcids(
                                        annotationPreferences,
                                        specificAnnotationPreferences,
                                        spectrum,
                                        peptide,
                                        modificationParameters,
                                        sequenceProvider,
                                        modificationSequenceMatchingParameters,
                                        true
                                );
                                int nIons = coveredAminoAcids.size();

                                coverageMap = nSeMap.get(nIons);

                                if (coverageMap == null) {

                                    coverageMap = new TreeMap<>();
                                    TreeMap<Long, PeptideAssumption> assumptionMap = new TreeMap<>();
                                    assumptionMap.put(peptideAssumption1.getPeptide().getKey(), peptideAssumption1);
                                    coverageMap.put(-1.0, assumptionMap);
                                    nSeMap.put(nIons, coverageMap);

                                } else {

                                    TreeMap<Long, PeptideAssumption> assumptionMap = coverageMap.get(-1.0);

                                    if (assumptionMap != null) {

                                        if (assumptionMap.size() > 1) {

                                            throw new IllegalArgumentException("Tie during best match selection in spectrum " + spectrumMatch.getKey() + ".");

                                        }

                                        PeptideAssumption peptideAssumption = assumptionMap.firstEntry().getValue();
                                        double massError = Math.abs(
                                                peptideAssumption.getDeltaMass(
                                                        spectrum.getPrecursor().getMz(),
                                                        searchParameters.isPrecursorAccuracyTypePpm(),
                                                        searchParameters.getMinIsotopicCorrection(),
                                                        searchParameters.getMaxIsotopicCorrection()
                                                ));
                                        coverageMap.put(massError, assumptionMap);
                                        coverageMap.remove(-1.0);

                                    }

                                    double massError = Math.abs(peptideAssumption1.getDeltaMass(
                                            spectrum.getPrecursor().getMz(),
                                            searchParameters.isPrecursorAccuracyTypePpm(),
                                            searchParameters.getMinIsotopicCorrection(),
                                            searchParameters.getMaxIsotopicCorrection()
                                    ));
                                    assumptionMap = coverageMap.get(massError);

                                    if (assumptionMap == null) {

                                        assumptionMap = new TreeMap<>();
                                        coverageMap.put(massError, assumptionMap);

                                    }

                                    assumptionMap.put(
                                            peptideAssumption1.getPeptide().getKey(),
                                            peptideAssumption1
                                    );

                                }
                            }

                            break searchEngine1loop;

                        }
                    }
                }
            }
        }

        if (!validAssumptions.isEmpty()) {

            PeptideAssumption bestPeptideAssumption = null;
            double retainedP = 0;

            selectionMainLoop:

            for (Entry<Double, TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>>>>> peptideAssumptionsEntry : validAssumptions.entrySet()) {

                retainedP = peptideAssumptionsEntry.getKey();
                TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>>>> pMap = peptideAssumptionsEntry.getValue();

                for (TreeMap<Integer, TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>>> proteinMaxMap : pMap.descendingMap().values()) {

                    for (TreeMap<Integer, TreeMap<Double, TreeMap<Long, PeptideAssumption>>> nSeMap : proteinMaxMap.descendingMap().values()) {

                        for (TreeMap<Double, TreeMap<Long, PeptideAssumption>> coverageMap : nSeMap.descendingMap().values()) {

                            for (TreeMap<Long, PeptideAssumption> assumptionsMap : coverageMap.values()) {

                                for (Entry<Long, PeptideAssumption> assumptionsEntry : assumptionsMap.entrySet()) {

                                    bestPeptideAssumption = assumptionsEntry.getValue();

                                    break selectionMainLoop;

                                }
                            }
                        }
                    }
                }
            }

            if (bestPeptideAssumption != null) {

                psmParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);

                if (multiSE) {

                    // try to find the most likely modification localization based on the search engine results
                    ArrayList<PeptideAssumption> inspectedAssumptions = new ArrayList<>(1);
                    HashMap<Long, TreeSet<Double>> assumptionPEPs = new HashMap<>(1);
                    long bestAssumptionKey = bestPeptideAssumption.getPeptide().getMatchingKey(sequenceMatchingPreferences);

                    assumptionsLoop:

                    for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptionsEmtry : assumptions.entrySet()) {

                        TreeMap<Double, ArrayList<PeptideAssumption>> advocateMap = assumptionsEmtry.getValue();

                        for (Entry<Double, ArrayList<PeptideAssumption>> advocateEntry : advocateMap.entrySet()) {

                            for (PeptideAssumption peptideAssumption : advocateEntry.getValue()) {

                                long assumptionKey = peptideAssumption.getPeptide().getMatchingKey(sequenceMatchingPreferences);

                                if (assumptionKey == bestAssumptionKey) {

                                    boolean found2 = false;
                                    PSParameter assumptionParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);

                                    for (PeptideAssumption assumption1 : inspectedAssumptions) {

                                        if (assumption1.getPeptide().sameModificationsAs(peptideAssumption.getPeptide())) {

                                            found2 = true;
                                            long key = assumption1.getPeptide().getKey();
                                            TreeSet<Double> peps = assumptionPEPs.get(key);
                                            peps.add(assumptionParameter.getProbability());
                                            break;

                                        }
                                    }

                                    if (!found2) {

                                        TreeSet<Double> peps = new TreeSet<>();
                                        peps.add(assumptionParameter.getProbability());
                                        long key = peptideAssumption.getPeptide().getKey();
                                        assumptionPEPs.put(key, peps);

                                    }

                                    break assumptionsLoop;

                                }
                            }
                        }
                    }

                    Double bestSeP = null;
                    int nSe = -1;

                    for (PeptideAssumption peptideAssumption : inspectedAssumptions) {

                        TreeSet<Double> peps = assumptionPEPs.get(peptideAssumption.getPeptide().getKey());
                        double sep = peps.first();

                        if (bestSeP == null || bestSeP > sep) {

                            bestSeP = sep;
                            nSe = peps.size();
                            bestPeptideAssumption = peptideAssumption;

                        } else if (peps.size() > nSe) {

                            if (Math.abs(sep - bestSeP) <= 1e-10) {

                                nSe = peps.size();
                                bestPeptideAssumption = peptideAssumption;

                            }
                        }
                    }
                }

                // create a PeptideShaker match based on the best search engine match
                Peptide sePeptide = bestPeptideAssumption.getPeptide();
                Peptide psPeptide = new Peptide(sePeptide.getSequence());

                ModificationMatch[] seModificationMatches = sePeptide.getVariableModifications();

                if (seModificationMatches.length > 0) {

                    psPeptide.setVariableModifications(
                            Arrays.stream(seModificationMatches)
                                    .map(modMatch -> new ModificationMatch(modMatch.getModification(), modMatch.getSite()))
                                    .toArray(ModificationMatch[]::new)
                    );
                }

                psPeptide.setProteinMapping(sePeptide.getProteinMapping());
                psPeptide.estimateTheoreticMass(modificationParameters, sequenceProvider, modificationSequenceMatchingParameters);

                PeptideAssumption psAssumption = new PeptideAssumption(psPeptide, 1, Advocate.peptideShaker.getIndex(), bestPeptideAssumption.getIdentificationCharge(), retainedP);

                spectrumMatch.setBestPeptideAssumption(psAssumption);

                psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                psmParameter.setScore(retainedP);

                PSParameter matchParameter = (PSParameter) bestPeptideAssumption.getUrParam(psmParameter);
                psmParameter.setAlgorithmDeltaPEP(matchParameter.getAlgorithmDeltaPEP());
                psmParameter.setDeltaPEP(matchParameter.getDeltaPEP());

            }
        }

        // map of the tag first hits: score -> assumptions
        TreeMap<Double, ArrayList<TagAssumption>> tagAssumptions = spectrumMatch.getAllTagAssumptions()
                .collect(
                        Collectors.groupingBy(
                                TagAssumption::getScore,
                                TreeMap::new,
                                Collectors.toCollection(ArrayList::new)
                        ));

        if (!tagAssumptions.isEmpty()) {

            Entry<Double, ArrayList<TagAssumption>> firstEntry = tagAssumptions.firstEntry();
            double bestEvalue = firstEntry.getKey();
            TagAssumption bestAssumption = firstEntry.getValue().get(0);
            spectrumMatch.setBestTagAssumption(bestAssumption);

            if (spectrumMatch.getBestPeptideAssumption() == null) {

                psmParameter = new PSParameter();

                if (!multiSE) {

                    psmParameter.setScore(bestEvalue);

                }

                psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                PSParameter matchParameter = (PSParameter) bestAssumption.getUrParam(psmParameter);

                psmParameter.setAlgorithmDeltaPEP(matchParameter.getAlgorithmDeltaPEP());
                psmParameter.setDeltaPEP(matchParameter.getDeltaPEP());

            }
        }
    }

    /**
     * Selects a first hit in a list of equally scoring peptide matches. The
     * selection is made based on: 1 - The occurrence of the protein detection
     * as given in the proteinCount map 2 - The sequence coverage by fragment
     * ions 3 - The precursor mass error.
     *
     * If no best hit is found, the first one sorted alphabetically is retained.
     *
     * @param spectrumKey the key of the spectrum
     * @param firstHits list of equally scoring peptide matches
     * @param proteinCount map of the number of peptides for every protein
     * @param sequenceProvider the sequence provider
     * @param identificationParameters the identification parameters
     * @param spectrumAnnotator the spectrum annotator to use
     *
     * @return a first hit from the list of equally scoring peptide matches
     */
    public static PeptideAssumption getBestHit(
            String spectrumKey,
            ArrayList<PeptideAssumption> firstHits,
            HashMap<String, Integer> proteinCount,
            SequenceProvider sequenceProvider,
            IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator spectrumAnnotator
    ) {

        if (firstHits.size() == 1) {
            return firstHits.get(0);
        }

        int maxProteins = 0;
        ArrayList<PeptideAssumption> bestPeptideAssumptions = new ArrayList<>(firstHits.size());

        for (PeptideAssumption peptideAssumption : firstHits) {

            Peptide peptide = peptideAssumption.getPeptide();
            TreeMap<String, int[]> proteinMapping = peptide.getProteinMapping();

            if (proteinMapping != null) {

                for (String accession : proteinMapping.keySet()) {

                    Integer count = proteinCount.get(accession);

                    if (count != null) {

                        if (count > maxProteins) {

                            maxProteins = count;
                            bestPeptideAssumptions.clear();
                            bestPeptideAssumptions.add(peptideAssumption);

                        } else if (count.equals(maxProteins)) {

                            bestPeptideAssumptions.add(peptideAssumption);

                        }
                    }
                }
            }
        }

        if (bestPeptideAssumptions.size() == 1) {

            return bestPeptideAssumptions.get(0);

        } else if (!bestPeptideAssumptions.isEmpty()) {

            firstHits = bestPeptideAssumptions;
            bestPeptideAssumptions = new ArrayList<>(firstHits.size());

        }

        Spectrum spectrum = SpectrumFactory.getInstance().getSpectrum(spectrumKey);
        int maxCoveredAminoAcids = 0;
        AnnotationParameters annotationParameters = identificationParameters.getAnnotationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        for (PeptideAssumption peptideAssumption : firstHits) {

            Peptide peptide = peptideAssumption.getPeptide();
            SpecificAnnotationParameters specificAnnotationPreferences = annotationParameters.getSpecificAnnotationParameters(
                    spectrum.getSpectrumKey(),
                    peptideAssumption,
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters,
                    spectrumAnnotator
            );
            Map<Integer, ArrayList<IonMatch>> coveredAminoAcids = spectrumAnnotator.getCoveredAminoAcids(
                    annotationParameters,
                    specificAnnotationPreferences,
                    spectrum,
                    peptide,
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters,
                    true
            );
            int nAas = coveredAminoAcids.size();

            if (nAas > maxCoveredAminoAcids) {

                maxCoveredAminoAcids = nAas;
                bestPeptideAssumptions.clear();
                bestPeptideAssumptions.add(peptideAssumption);

            } else if (nAas == maxCoveredAminoAcids) {

                bestPeptideAssumptions.add(peptideAssumption);

            }
        }

        if (bestPeptideAssumptions.size() == 1) {

            return bestPeptideAssumptions.get(0);

        } else if (!bestPeptideAssumptions.isEmpty()) {

            firstHits = bestPeptideAssumptions;
            bestPeptideAssumptions = new ArrayList<>(firstHits.size());

        }

        double minMassError = identificationParameters.getPeptideAssumptionFilter().getMaxMzDeviation();

        if (minMassError == -1.0) {

            minMassError = identificationParameters.getSearchParameters().getPrecursorAccuracy();

        }

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        for (PeptideAssumption peptideAssumption : firstHits) {

            double massError = Math.abs(
                    peptideAssumption.getDeltaMass(
                            spectrum.getPrecursor().getMz(),
                            searchParameters.isPrecursorAccuracyTypePpm(),
                            searchParameters.getMinIsotopicCorrection(),
                            searchParameters.getMaxIsotopicCorrection()
                    ));

            if (massError < minMassError) {

                minMassError = massError;
                bestPeptideAssumptions.clear();
                bestPeptideAssumptions.add(peptideAssumption);

            } else if (massError == minMassError) {

                bestPeptideAssumptions.add(peptideAssumption);

            }
        }

        if (bestPeptideAssumptions.size() == 1) {

            return bestPeptideAssumptions.get(0);

        } else if (bestPeptideAssumptions.isEmpty()) {

            bestPeptideAssumptions = firstHits;

        }

        return bestPeptideAssumptions.stream()
                .collect(Collectors.groupingBy(peptideAssumption -> peptideAssumption.getPeptide().getSequence(), TreeMap::new, Collectors.toList()))
                .firstEntry()
                .getValue()
                .get(0);

    }
}
