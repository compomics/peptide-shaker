package eu.isas.peptideshaker.fileimport;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideVariantMatches;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.search_engine_mapping.ModificationLocalizationMapper;
import com.compomics.util.experiment.identification.modification.search_engine_mapping.ModificationNameMapper;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.validation.percolator.PercolatorFeature;
import com.compomics.util.experiment.identification.validation.percolator.PercolatorFeaturesCache;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.PercolatorUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Runnable for the import of PSMs.
 *
 * @author Marc Vaudel
 */
public class PsmImportRunnable implements Runnable {

    /**
     * Size of the batches to use when adding objects to the database.
     */
    public static final int BATCH_SIZE = 100000;
    /**
     * The modification factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * Iterator for the spectrum matches to import.
     */
    private final ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchQueue;
    /**
     * Map of the objects to add to the database.
     */
    private final HashMap<Long, Object> matchesToAdd = new HashMap<>(BATCH_SIZE);
    /**
     * The number of first hits.
     */
    private long nPSMs = 0;
    /**
     * The total number of peptide assumptions.
     */
    private long nPeptideAssumptionsTotal = 0;
    /**
     * The number of PSMs which were rejected due to a peptide issue.
     */
    private int peptideIssue = 0;
    /**
     * The number of PSMs which were rejected due to a modification parsing
     * issue.
     */
    private int modificationIssue = 0;
    /**
     * The id file reader where the PSMs are from.
     */
    private final IdfileReader fileReader;
    /**
     * The identification file where the PSMs are from.
     */
    private final File idFile;
    /**
     * List of ignored modifications.
     */
    private final HashSet<Integer> ignoredModifications = new HashSet<>(2);
    /**
     * Map of the number of times proteins appeared as first hit.
     */
    private final HashMap<String, Integer> proteinCount = new HashMap<>(10000);
    /**
     * The identification object database.
     */
    private final Identification identification;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The processing parameters.
     */
    private final ProcessingParameters processingParameters;
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The FASTA mapper used to map peptides to proteins.
     */
    private final FastaMapper fastaMapper;
    /**
     * The waiting handler to display feedback to the user.
     */
    private final WaitingHandler waitingHandler;
    /**
     * Exception handler.
     */
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructor.
     *
     * @param spectrumMatchQueue the spectrum matches iterator to use
     * @param identificationParameters the identification parameters
     * @param processingParameters the processing parameters
     * @param fileReader the reader of the file which the matches are imported
     * from
     * @param idFile the file which the matches are imported from
     * @param identification the identification object where to store the
     * matches
     * @param sequenceProvider the protein sequence provider
     * @param fastaMapper the FASTA mapper used to map peptides to proteins
     * @param waitingHandler The waiting handler to display feedback to the
     * user.
     * @param exceptionHandler The handler of exceptions.
     */
    public PsmImportRunnable(
            ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchQueue,
            IdentificationParameters identificationParameters,
            ProcessingParameters processingParameters,
            IdfileReader fileReader,
            File idFile,
            Identification identification,
            SequenceProvider sequenceProvider,
            FastaMapper fastaMapper,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) {

        this.spectrumMatchQueue = spectrumMatchQueue;
        this.identificationParameters = identificationParameters;
        this.processingParameters = processingParameters;
        this.fileReader = fileReader;
        this.idFile = idFile;
        this.identification = identification;
        this.sequenceProvider = sequenceProvider;
        this.fastaMapper = fastaMapper;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;

    }

    @Override
    public void run() {

        try {

            SpectrumMatch spectrumMatch;
            while ((spectrumMatch = spectrumMatchQueue.poll()) != null) {

                importPsm(spectrumMatch);

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                waitingHandler.increaseSecondaryProgressCounter();

            }

            if (!matchesToAdd.isEmpty()) {

                identification.addSpectrumMatches(
                        matchesToAdd,
                        waitingHandler,
                        false
                );

            }

        } catch (Exception e) {

            waitingHandler.setRunCanceled();
            exceptionHandler.catchException(e);

        }
    }

    /**
     * Imports a PSM.
     *
     * @param spectrumMatch the spectrum match to import
     */
    private void importPsm(
            SpectrumMatch spectrumMatch
    ) {

        nPSMs++;

        importAssumptions(spectrumMatch);

        if (spectrumMatch.hasPeptideAssumption() || spectrumMatch.hasTagAssumption()) {

            long spectrumMatchKey = spectrumMatch.getKey();

            SpectrumMatch dbMatch = identification.getSpectrumMatch(spectrumMatchKey);

            if (dbMatch != null) {

                mergePeptideAssumptions(
                        spectrumMatch.getPeptideAssumptionsMap(),
                        dbMatch.getPeptideAssumptionsMap()
                );
                mergeTagAssumptions(
                        spectrumMatch.getTagAssumptionsMap(),
                        dbMatch.getTagAssumptionsMap()
                );

            } else {

                matchesToAdd.put(spectrumMatch.getKey(), spectrumMatch);

                if (matchesToAdd.size() == BATCH_SIZE) {

                    identification.addSpectrumMatches(
                            matchesToAdd,
                            waitingHandler,
                            false
                    );

                    matchesToAdd.clear();

                }

            }
        }
    }

    /**
     * Extracts the assumptions and adds them to the provided map.
     *
     * @param matchAssumptions the match assumptions
     * @param combinedAssumptions the combined assumptions
     */
    private void mergeTagAssumptions(
            HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> matchAssumptions,
            HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> combinedAssumptions
    ) {

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<TagAssumption>>> entry : matchAssumptions.entrySet()) {

            int algorithm = entry.getKey();
            TreeMap<Double, ArrayList<TagAssumption>> algorithmMap = entry.getValue();
            TreeMap<Double, ArrayList<TagAssumption>> combinedAlgorithmMap = combinedAssumptions.get(algorithm);

            if (combinedAlgorithmMap == null) {

                combinedAssumptions.put(algorithm, algorithmMap);

            } else {

                for (Map.Entry<Double, ArrayList<TagAssumption>> entry2 : algorithmMap.entrySet()) {

                    double score = entry2.getKey();
                    ArrayList<TagAssumption> scoreAssumptions = entry2.getValue();
                    ArrayList<TagAssumption> combinedScoreAssumptions = combinedAlgorithmMap.get(score);

                    if (combinedScoreAssumptions == null) {

                        combinedAlgorithmMap.put(score, scoreAssumptions);

                    } else {

                        combinedScoreAssumptions.addAll(scoreAssumptions);

                    }
                }
            }
        }
    }

    /**
     * Extracts the assumptions and adds them to the provided map.
     *
     * @param matchAssumptions the match assumptions
     * @param combinedAssumptions the combined assumptions
     */
    private void mergePeptideAssumptions(
            HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> matchAssumptions,
            HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> combinedAssumptions
    ) {

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : matchAssumptions.entrySet()) {

            int algorithm = entry.getKey();
            TreeMap<Double, ArrayList<PeptideAssumption>> algorithmMap = entry.getValue();
            TreeMap<Double, ArrayList<PeptideAssumption>> combinedAlgorithmMap = combinedAssumptions.get(algorithm);

            if (combinedAlgorithmMap == null) {

                combinedAssumptions.put(algorithm, algorithmMap);

            } else {

                for (Map.Entry<Double, ArrayList<PeptideAssumption>> entry2 : algorithmMap.entrySet()) {

                    double score = entry2.getKey();
                    ArrayList<PeptideAssumption> scoreAssumptions = entry2.getValue();
                    ArrayList<PeptideAssumption> combinedScoreAssumptions = combinedAlgorithmMap.get(score);

                    if (combinedScoreAssumptions == null) {

                        combinedAlgorithmMap.put(score, scoreAssumptions);

                    } else {

                        combinedScoreAssumptions.addAll(scoreAssumptions);

                    }
                }
            }
        }
    }

    /**
     * Import the assumptions. Maps algorithm specific modifications to the
     * generic objects. Relocates aberrant modifications and removes assumptions
     * where not all modifications are mapped. Verifies whether there is a best
     * match for the spectrum according to the search engine score.
     *
     * @param spectrumMatch the spectrum match to import
     * @param assumptions the assumptions to import
     * @param peptideSpectrumAnnotator the spectrum annotator to use to annotate
     * spectra
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     */
    private void importAssumptions(
            SpectrumMatch spectrumMatch
    ) {

        PeptideAssumptionFilter peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptions = spectrumMatch.getPeptideAssumptionsMap();

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : peptideAssumptions.entrySet()) {

            int advocateId = entry.getKey();

            if (advocateId == Advocate.xtandem.getIndex()) {
                PsmImporter.verifyXTandemModifications(identificationParameters);
            }

            TreeMap<Double, ArrayList<PeptideAssumption>> assumptionsForAdvocate = entry.getValue();
            TreeSet<Double> scores = new TreeSet<>(assumptionsForAdvocate.keySet());

            for (double score : scores) {

                ArrayList<PeptideAssumption> oldAssumptions = assumptionsForAdvocate.get(score);
                ArrayList<PeptideAssumption> newAssumptions = new ArrayList<>(oldAssumptions.size());

                nPeptideAssumptionsTotal += oldAssumptions.size();

                for (PeptideAssumption peptideAssumption : oldAssumptions) {

                    Peptide peptide = peptideAssumption.getPeptide();
                    String peptideSequence = peptide.getSequence();

                    // Ignore peptides that are too long or too short
                    if (peptideSequence.length() >= peptideAssumptionFilter.getMinPepLength()
                            && peptideSequence.length() <= peptideAssumptionFilter.getMaxPepLength()) {

                        // Map peptide to protein
                        proteinMapping(peptide);

                        // map the algorithm-specific modifications to utilities modifications
                        // If there are not enough sites to put them all on the sequence, add an unknown modification
                        // Note: this needs to be done for tag based assumptions as well since the protein mapping can 
                        // return erroneous modifications for some pattern based modifications
                        ModificationParameters modificationParameters = searchParameters.getModificationParameters();

                        ModificationMatch[] modificationMatches = peptide.getVariableModifications();

                        HashMap<Integer, ArrayList<String>> expectedNames = new HashMap<>(modificationMatches.length);
                        HashMap<ModificationMatch, ArrayList<String>> modNames = new HashMap<>(modificationMatches.length);

                        for (ModificationMatch modMatch : modificationMatches) {

                            HashMap<Integer, HashSet<String>> tempNames = ModificationNameMapper.getPossibleModificationNames(
                                    peptide,
                                    modMatch,
                                    fileReader,
                                    searchParameters,
                                    modificationSequenceMatchingParameters,
                                    sequenceProvider,
                                    modificationFactory
                            );

                            HashSet<String> allNames = tempNames.values()
                                    .stream()
                                    .flatMap(
                                            nameList -> nameList.stream()
                                    )
                                    .collect(
                                            Collectors.toCollection(HashSet::new)
                                    );

                            modNames.put(modMatch, new ArrayList<>(allNames));

                            for (int pos : tempNames.keySet()) {

                                ArrayList<String> namesAtPosition = expectedNames.get(pos);

                                if (namesAtPosition == null) {
                                    namesAtPosition = new ArrayList<>(2);
                                    expectedNames.put(pos, namesAtPosition);
                                }

                                for (String modName : tempNames.get(pos)) {
                                    if (!namesAtPosition.contains(modName)) {
                                        namesAtPosition.add(modName);
                                    }
                                }
                            }
                        }

                        if (peptide.getVariableModifications().length > 0) {

                            ModificationLocalizationMapper.modificationLocalization(
                                    peptide,
                                    expectedNames,
                                    modNames,
                                    identificationParameters,
                                    fileReader,
                                    modificationFactory
                            );

                        }

                        if (peptideAssumptionFilter.validateModifications(peptide,
                                sequenceMatchingParameters,
                                modificationSequenceMatchingParameters,
                                searchParameters.getModificationParameters()
                        )) {

                            // Set peptide key
                            peptide.setKey(
                                    Peptide.getKey(peptide.getSequence(),
                                            peptide.getVariableModifications()
                                    )
                            );

                            // Estimate mass
                            peptide.getMass(modificationParameters,
                                    sequenceProvider,
                                    modificationSequenceMatchingParameters
                            );

                            // Add new assumption
                            newAssumptions.add(peptideAssumption);

                            // Get protein count
                            for (String protein : peptide.getProteinMapping().navigableKeySet()) {

                                Integer count = proteinCount.get(protein);

                                if (count != null) {
                                    proteinCount.put(protein, count + 1);
                                } else {
                                    proteinCount.put(protein, 1);
                                }
                            }

                            // Cache the Percolator features
                            if (processingParameters.cachePercolatorFeatures()) {

                                PercolatorFeaturesCache percolatorFeaturesCache = (PercolatorFeaturesCache) peptideAssumption.getUrParam(PercolatorFeaturesCache.dummy);

                                if (percolatorFeaturesCache == null) {

                                    percolatorFeaturesCache = new PercolatorFeaturesCache();
                                    peptideAssumption.addUrParam(percolatorFeaturesCache);

                                }

                                boolean[] enzymaticity = PercolatorUtils.getEnzymaticityFeature(
                                        peptideAssumption,
                                        searchParameters,
                                        sequenceProvider
                                );
                                percolatorFeaturesCache.cache.put(PercolatorFeature.enzymaticity, enzymaticity);

                            }

                        } else {
                            modificationIssue++;
                        }
                    } else {
                        peptideIssue++;
                    }
                }

                if (!newAssumptions.isEmpty()) {
                    assumptionsForAdvocate.put(score, newAssumptions);
                } else {
                    assumptionsForAdvocate.remove(score);

                }
            }
        }
    }

    /**
     * Maps the peptide sequence to the FASTA file.
     *
     * @param peptide the peptide to map
     */
    private void proteinMapping(
            Peptide peptide
    ) {

        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();

        ArrayList<PeptideProteinMapping> peptideProteinMappings = fastaMapper.getProteinMapping(peptide.getSequence(), sequenceMatchingPreferences);
        HashMap<String, HashMap<String, int[]>> sequenceIndexes = PeptideProteinMapping.getPeptideProteinIndexesMap(peptideProteinMappings);

        TreeMap<String, int[]> proteinIndexes;

        if (sequenceIndexes.size() == 1) {

            proteinIndexes = new TreeMap<>(sequenceIndexes.values().stream().findAny().get());

        } else {

            proteinIndexes = new TreeMap<>();

            for (HashMap<String, int[]> tempIndexes : sequenceIndexes.values()) {

                for (Map.Entry<String, int[]> entry : tempIndexes.entrySet()) {

                    String accession = entry.getKey();
                    int[] newIndexes = entry.getValue();
                    int[] currentIndexes = proteinIndexes.get(accession);

                    if (currentIndexes == null) {

                        proteinIndexes.put(accession, newIndexes);

                    } else {

                        int[] mergedIndexes = IntStream.concat(Arrays.stream(currentIndexes), Arrays.stream(newIndexes))
                                .distinct()
                                .sorted()
                                .toArray();
                        proteinIndexes.put(accession, mergedIndexes);

                    }
                }
            }
        }

        peptide.setProteinMapping(proteinIndexes);

        HashMap<String, HashMap<Integer, PeptideVariantMatches>> variantMatches = PeptideProteinMapping.getVariantMatches(peptideProteinMappings);
        peptide.setVariantMatches(variantMatches);

    }

    /**
     * Returns the number of PSMs processed.
     *
     * @return the number of PSMs processed
     */
    public long getnPSMs() {

        return nPSMs;

    }

    /**
     * Returns the total number of peptide assumptions parsed.
     *
     * @return the total number of peptide assumptions parsed
     */
    public long getnPeptideAssumptionsTotal() {
        return nPeptideAssumptionsTotal;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * peptide issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * peptide issue
     */
    public int getPeptideIssue() {
        return peptideIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * modification parsing issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * modification parsing issue
     */
    public int getModificationIssue() {
        return modificationIssue;
    }

    /**
     * Returns the occurrence of each protein.
     *
     * @return the occurrence of each protein
     */
    public HashMap<String, Integer> getProteinCount() {
        return proteinCount;
    }
}
