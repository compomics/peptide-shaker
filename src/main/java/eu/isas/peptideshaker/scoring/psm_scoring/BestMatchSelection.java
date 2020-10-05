package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
     * The sequence provider
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The spectrum provider.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * The tie breaker for equally scoring matches.
     */
    private final TieBreaker tieBreaker;
    /**
     * The peptide assumptions filter.
     */
    private final PeptideAssumptionFilter peptideAssumptionFilter;
    /**
     * The sequence matching parameters.
     */
    private final SequenceMatchingParameters sequenceMatchingParameters;
    /**
     * The search parameters.
     */
    private final SearchParameters searchParameters;
    /**
     * The modification parameters.
     */
    private final ModificationParameters modificationParameters;
    /**
     * The sequence matching parameters for modifications.
     */
    private final SequenceMatchingParameters modificationSequenceMatchingParameters;
    /**
     * The FASTA parameters.
     */
    private final FastaParameters fastaParameters;

    /**
     * Constructor.
     *
     * @param proteinCount Map of the occurrence of the protein accessions.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param identificationParameters The identification parameters.
     * @param peptideSpectrumAnnotator The peptide spectrum annotator.
     */
    public BestMatchSelection(
            HashMap<String, Integer> proteinCount,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator
    ) {

        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;
        this.peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        this.sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        this.searchParameters = identificationParameters.getSearchParameters();
        this.modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        this.modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        this.fastaParameters = identificationParameters.getFastaParameters();

        this.tieBreaker = new TieBreaker(
                proteinCount,
                identificationParameters,
                peptideSpectrumAnnotator,
                sequenceProvider,
                spectrumProvider
        );

    }

    /**
     * Selects the best hit.
     *
     * @param spectrumMatch The spectrum match.
     * @param inputMap The input map.
     * @param psmTargetDecoyMap The PSM target decoy map.
     * @param identification The identification class.
     */
    public void selectBestHit(
            SpectrumMatch spectrumMatch,
            InputMap inputMap,
            TargetDecoyMap psmTargetDecoyMap,
            Identification identification
    ) {

        boolean multiSE = inputMap.isMultipleAlgorithms();

        PSParameter psmParameter = new PSParameter();
        psmParameter.setMatchValidationLevel(MatchValidationLevel.none);
        spectrumMatch.addUrParam(psmParameter);

        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();

        HashSet<Long> ids = new HashSet<>(2);
        ArrayList<PeptideAssumption> assumptions = new ArrayList<>(4);
        double bestP = 1.0;

        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptionMap = spectrumMatch.getPeptideAssumptionsMap();

        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry1 : assumptionMap.entrySet()) {

            int searchEngine1 = entry1.getKey();
            TreeMap<Double, ArrayList<PeptideAssumption>> advocate1Map = entry1.getValue();

            searchEngine1loop:

            for (Entry<Double, ArrayList<PeptideAssumption>> advocate1Entry : advocate1Map.entrySet()) {

                for (PeptideAssumption peptideAssumption1 : advocate1Entry.getValue()) {

                    Peptide peptide1 = peptideAssumption1.getPeptide();
                    long id = peptide1.getMatchingKey(sequenceMatchingParameters);

                    if (!ids.contains(id)) {

                        ids.add(id);

                        if (peptide1.getProteinMapping() != null
                                && !peptide1.getProteinMapping().isEmpty()
                                && peptideAssumptionFilter.validatePeptide(
                                        peptide1,
                                        sequenceMatchingParameters,
                                        searchParameters.getDigestionParameters()
                                )
                                && peptideAssumptionFilter.validateModifications(
                                        peptide1,
                                        sequenceMatchingParameters,
                                        modificationSequenceMatchingParameters,
                                        searchParameters.getModificationParameters()
                                )
                                && peptideAssumptionFilter.validatePrecursor(
                                        peptideAssumption1,
                                        spectrumFile,
                                        spectrumTitle,
                                        spectrumProvider,
                                        searchParameters
                                )
                                && peptideAssumptionFilter.validateProteins(
                                        peptide1,
                                        sequenceMatchingParameters,
                                        sequenceProvider
                                )) {

                            PSParameter psParameter1 = (PSParameter) peptideAssumption1.getUrParam(PSParameter.dummy);

                            double p = multiSE && fastaParameters.isTargetDecoy()
                                    ? psParameter1.getProbability()
                                    : peptideAssumption1.getScore(); // @TODO: why use the score?

                            searchEngine2loop:

                            for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry2 : assumptionMap.entrySet()) {

                                int searchEngine2 = entry2.getKey();

                                if (searchEngine1 != searchEngine2) {

                                    TreeMap<Double, ArrayList<PeptideAssumption>> advocate2Map = entry2.getValue();

                                    for (Entry<Double, ArrayList<PeptideAssumption>> advocate2Entry : advocate2Map.entrySet()) {

                                        for (PeptideAssumption peptideAssumption2 : advocate2Entry.getValue()) {

                                            if (peptideAssumption1.getPeptide()
                                                    .isSameSequenceAndModificationStatus(
                                                            peptideAssumption2.getPeptide(),
                                                            sequenceMatchingParameters
                                                    )) {

                                                PSParameter psParameter2 = (PSParameter) peptideAssumption2.getUrParam(PSParameter.dummy);
                                                p *= psParameter2.getProbability();

                                                break searchEngine2loop;

                                            }
                                        }
                                    }
                                }
                            }

                            psmTargetDecoyMap.put(
                                    p,
                                    PeptideUtils.isDecoy(
                                            peptide1,
                                            sequenceProvider
                                    )
                            );

                            if (p <= bestP) {

                                if (p < bestP) {

                                    bestP = p;
                                    assumptions.clear();

                                }

                                assumptions.add(peptideAssumption1);

                            }
                        }
                    }
                }
            }
        }

        if (!assumptions.isEmpty()) {

            PeptideAssumption bestPeptideAssumption;

            bestPeptideAssumption = getBestMatch(
                    spectrumFile,
                    spectrumTitle,
                    assumptions
            );

            psmParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);

            if (multiSE) {

                // try to find the most likely modification localization based on the search engine results
                ArrayList<PeptideAssumption> inspectedAssumptions = new ArrayList<>(1);
                HashMap<Long, TreeSet<Double>> assumptionPEPs = new HashMap<>(1);
                long bestAssumptionKey = bestPeptideAssumption.getPeptide().getMatchingKey(sequenceMatchingParameters);

                assumptionsLoop:

                for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptionsEmtry : assumptionMap.entrySet()) {

                    TreeMap<Double, ArrayList<PeptideAssumption>> advocateMap = assumptionsEmtry.getValue();

                    for (Entry<Double, ArrayList<PeptideAssumption>> advocateEntry : advocateMap.entrySet()) {

                        for (PeptideAssumption peptideAssumption : advocateEntry.getValue()) {

                            long assumptionKey = peptideAssumption.getPeptide().getMatchingKey(sequenceMatchingParameters);

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
                                .map(
                                        modMatch -> new ModificationMatch(
                                                modMatch.getModification(),
                                                modMatch.getSite()
                                        )
                                )
                                .toArray(ModificationMatch[]::new)
                );
            }

            psPeptide.setProteinMapping(
                    sePeptide.getProteinMapping()
            );
            psPeptide.estimateTheoreticMass(
                    modificationParameters,
                    sequenceProvider,
                    modificationSequenceMatchingParameters
            );

            PeptideAssumption psAssumption = new PeptideAssumption(
                    psPeptide,
                    1,
                    Advocate.peptideShaker.getIndex(),
                    bestPeptideAssumption.getIdentificationCharge(),
                    bestP
            );

            spectrumMatch.setBestPeptideAssumption(psAssumption);
            identification.updateObject(spectrumMatch.getKey(), spectrumMatch);

            psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
            psmParameter.setScore(bestP);

            PSParameter matchParameter = (PSParameter) bestPeptideAssumption.getUrParam(psmParameter);
            psmParameter.setAlgorithmDeltaPEP(matchParameter.getAlgorithmDeltaPEP());
            psmParameter.setDeltaPEP(matchParameter.getDeltaPEP());

        }

        // map of the tag first hits: score -> assumptions
        TreeMap<Double, ArrayList<TagAssumption>> tagAssumptions = spectrumMatch.getAllTagAssumptions()
                .collect(
                        Collectors.groupingBy(
                                TagAssumption::getScore,
                                TreeMap::new,
                                Collectors.toCollection(ArrayList::new)
                        )
                );

        if (!tagAssumptions.isEmpty()) {

            Entry<Double, ArrayList<TagAssumption>> firstEntry = tagAssumptions.firstEntry();
            double bestEvalue = firstEntry.getKey();
            TagAssumption bestAssumption = firstEntry.getValue().get(0);
            identification.updateObject(spectrumMatch.getKey(), spectrumMatch);
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
     * Returns the best match for the given spectrum among the given peptide
     * assumptions.
     *
     * @param spectrumFile The file name of the spectrum.
     * @param spectrumTitle The title of the spectrum.
     * @param assumptions A list of peptide assumptions.
     *
     * @return The best match.
     */
    public PeptideAssumption getBestMatch(
            String spectrumFile,
            String spectrumTitle,
            ArrayList<PeptideAssumption> assumptions
    ) {

        return getBestMatch(
                spectrumFile,
                spectrumTitle,
                assumptions,
                false
        );

    }

    /**
     * Returns the best match for the given spectrum among the given peptide
     * assumptions.
     *
     * @param spectrumFile The file name of the spectrum.
     * @param spectrumTitle The title of the spectrum.
     * @param assumptions A list of peptide assumptions.
     * @param silentFail If true, no exception will be thrown if ties cannot be
     * broken and the first of the best hits will be returned.
     *
     * @return The best match.
     */
    public PeptideAssumption getBestMatch(
            String spectrumFile,
            String spectrumTitle,
            ArrayList<PeptideAssumption> assumptions,
            boolean silentFail
    ) {

        PeptideAssumption bestPeptideAssumption = assumptions.get(0);

        for (int i = 1; i < assumptions.size(); i++) {

            PeptideAssumption peptideAssumption = assumptions.get(i);

            try {
                bestPeptideAssumption = tieBreaker.getBestPeptideAssumption(
                        spectrumFile,
                        spectrumTitle,
                        bestPeptideAssumption,
                        peptideAssumption,
                        silentFail
                );
            } catch (Exception e) {
                bestPeptideAssumption = tieBreaker.getBestPeptideAssumption(
                        spectrumFile,
                        spectrumTitle,
                        bestPeptideAssumption,
                        peptideAssumption,
                        silentFail
                );
            }

        }

        return bestPeptideAssumption;

    }
}
