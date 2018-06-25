package eu.isas.peptideshaker.validation;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.ValidationQcParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.filtering.AssumptionFilter;
import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.advanced.FractionParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.experiment.identification.filtering.PeptideFilter;
import com.compomics.util.experiment.identification.filtering.ProteinFilter;
import com.compomics.util.experiment.identification.filtering.PsmFilter;
import com.compomics.util.experiment.identification.filtering.items.AssumptionFilterItem;
import com.compomics.util.experiment.identification.filtering.items.PeptideFilterItem;
import com.compomics.util.experiment.identification.filtering.items.ProteinFilterItem;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import com.compomics.util.experiment.identification.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.parameters.identification.advanced.IdMatchValidationParameters;
import com.compomics.util.parameters.peptide_shaker.ProjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * This class validates the quality of identification matches.
 *
 * @author Marc Vaudel
 */
public class MatchesValidator {

    /**
     * The PSM target decoy map.
     */
    private TargetDecoyMap psmMap;
    /**
     * The peptide target decoy map.
     */
    private TargetDecoyMap peptideMap;
    /**
     * The protein target decoy map.
     */
    private TargetDecoyMap proteinMap;
    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    /**
     * Constructor.
     *
     * @param psmMap the PSM target decoy map
     * @param peptideMap the peptide target decoy map
     * @param proteinMap the protein target decoy map
     */
    public MatchesValidator(TargetDecoyMap psmMap, TargetDecoyMap peptideMap, TargetDecoyMap proteinMap) {

        this.psmMap = psmMap;
        this.peptideMap = peptideMap;
        this.proteinMap = proteinMap;

    }

    /**
     * This method validates the identification matches of an identification
     * object. Target Decoy thresholds must be set.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided, metrics on fractions will be saved while
     * iterating the matches
     * @param geneMaps the gene maps
     * @param inputMap the target decoy map of all search engine scores
     * @param waitingHandler a waiting handler displaying progress to the user
     * and allowing canceling the process
     * @param exceptionHandler a handler for exceptions
     * @param identificationFeaturesGenerator an identification features
     * generator computing information about the identification matches
     * @param sequenceProvider a protein sequence provider
     * @param proteinDetailsProvider a protein details provider
     * @param identificationParameters the identification parameters
     * @param projectType the project type
     * @param processingParameters the processing parameters
     *
     * @throws java.lang.InterruptedException exception thrown if a thread gets
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if the
     * operation times out
     */
    public void validateIdentifications(Identification identification, Metrics metrics, InputMap inputMap,
            WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps, IdentificationParameters identificationParameters,
            ProjectType projectType, ProcessingParameters processingParameters) throws InterruptedException, TimeoutException {

        IdMatchValidationParameters validationParameters = identificationParameters.getIdValidationParameters();

        waitingHandler.setWaitingText("Finding FDR Thresholds. Please Wait...");

        TargetDecoyMap currentMap = psmMap;
        TargetDecoyResults currentResults = currentMap.getTargetDecoyResults();
        currentResults.setInputType(1);
        currentResults.setUserInput(validationParameters.getDefaultPsmFDR());
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(validationParameters.getDefaultPsmFDR());
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        currentMap = peptideMap;
        currentResults = currentMap.getTargetDecoyResults();
        currentResults.setInputType(1);
        currentResults.setUserInput(validationParameters.getDefaultPeptideFDR());
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(validationParameters.getDefaultPeptideFDR());
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        currentMap = proteinMap;
        currentResults = currentMap.getTargetDecoyResults();
        currentResults.setInputType(1);
        currentResults.setUserInput(validationParameters.getDefaultProteinFDR());
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(validationParameters.getDefaultProteinFDR());
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        ValidationQcParameters validationQCParameters = validationParameters.getValidationQCParameters();

        waitingHandler.setWaitingText("Match Validation and Quality Control. Please Wait...");
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size()
                + identification.getPeptideIdentification().size()
                + 2 * identification.getSpectrumIdentificationSize());

        // validate the spectrum matches
        if (inputMap != null) {

            inputMap.resetAdvocateContributions();

        }

        AnnotationParameters annotationParameters = identificationParameters.getAnnotationParameters();
        Double intensityLimit = annotationParameters.getAnnotationIntensityLimit();
        annotationParameters.setIntensityLimit(0);

        ExecutorService pool = Executors.newFixedThreadPool(processingParameters.getnThreads());

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        ArrayList<PsmValidatorRunnable> psmRunnables = new ArrayList<>(processingParameters.getnThreads());

        for (int i = 1; i <= processingParameters.getnThreads(); i++) {

            PsmValidatorRunnable runnable = new PsmValidatorRunnable(psmIterator, identification, identificationFeaturesGenerator,
                    sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters, waitingHandler,
                    exceptionHandler, inputMap, false, true);
            pool.submit(runnable);
            psmRunnables.add(runnable);

        }

        if (waitingHandler.isRunCanceled()) {

            pool.shutdownNow();
            return;

        }

        pool.shutdown();

        if (!pool.awaitTermination(identification.getSpectrumIdentificationSize(), TimeUnit.MINUTES)) {

            throw new TimeoutException("Spectrum matches validation timed out. Please contact the developers.");

        }

        HashMap<String, ArrayList<Double>> precursorMzDeviations = new HashMap<>();

        for (PsmValidatorRunnable runnable : psmRunnables) {

            precursorMzDeviations.putAll(runnable.getThreadPrecursorMzDeviations());

        }

        for (String spectrumFileName : precursorMzDeviations.keySet()) {

            ArrayList<Double> precursorMzDeviationsFile = precursorMzDeviations.get(spectrumFileName);

            if (precursorMzDeviations.size() >= 100) {

                Collections.sort(precursorMzDeviationsFile);
                identificationFeaturesGenerator.setMassErrorDistribution(spectrumFileName, precursorMzDeviationsFile);

            } else {

                // There are not enough precursors, disable probabilistic precursor filter
                if (validationQCParameters.getPsmFilters() != null) {
                    for (Filter filter : validationQCParameters.getPsmFilters()) {

                        PsmFilter psmFilter = (PsmFilter) filter;

                        if (psmFilter.getItemsNames().contains(AssumptionFilterItem.precrusorMzErrorStat.name)) {

                            psmFilter.removeFilterItem(AssumptionFilterItem.precrusorMzErrorStat.name);
                            SearchParameters searchParameters = identificationParameters.getSearchParameters();

                            if (searchParameters.isPrecursorAccuracyTypePpm()) {

                                psmFilter.setFilterItem(AssumptionFilterItem.precrusorMzErrorPpm.name, FilterItemComparator.lowerOrEqual, searchParameters.getPrecursorAccuracy());

                            } else {

                                psmFilter.setFilterItem(AssumptionFilterItem.precrusorMzErrorDa.name, FilterItemComparator.lowerOrEqual, searchParameters.getPrecursorAccuracy());

                            }
                        }

                        AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();

                        if (assumptionFilter.getItemsNames().contains(AssumptionFilterItem.precrusorMzErrorStat.name)) {

                            assumptionFilter.removeFilterItem(AssumptionFilterItem.precrusorMzErrorStat.name);
                            SearchParameters searchParameters = identificationParameters.getSearchParameters();

                            if (searchParameters.isPrecursorAccuracyTypePpm()) {

                                assumptionFilter.setFilterItem(AssumptionFilterItem.precrusorMzErrorPpm.name, FilterItemComparator.lowerOrEqual, searchParameters.getPrecursorAccuracy());

                            } else {

                                assumptionFilter.setFilterItem(AssumptionFilterItem.precrusorMzErrorDa.name, FilterItemComparator.lowerOrEqual, searchParameters.getPrecursorAccuracy());

                            }
                        }
                    }
                }
            }
        }

        pool = Executors.newFixedThreadPool(processingParameters.getnThreads());

        psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        for (int i = 1; i <= processingParameters.getnThreads(); i++) {

            PsmValidatorRunnable runnable = new PsmValidatorRunnable(psmIterator, identification, identificationFeaturesGenerator,
                    sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters, waitingHandler,
                    exceptionHandler, inputMap, true, false);
            pool.submit(runnable);

        }

        if (waitingHandler.isRunCanceled()) {

            pool.shutdownNow();
            return;

        }

        pool.shutdown();

        if (!pool.awaitTermination(identification.getSpectrumIdentificationSize(), TimeUnit.MINUTES)) {

            throw new TimeoutException("Spectrum matches validation timed out. Please contact the developers.");

        }

        annotationParameters.setIntensityLimit(intensityLimit);

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            // validate the peptides
            pool = Executors.newFixedThreadPool(processingParameters.getnThreads());
            ArrayList<PeptideValidatorRunnable> peptideRunnables = new ArrayList<>(processingParameters.getnThreads());

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

            for (int i = 1; i <= processingParameters.getnThreads(); i++) {

                PeptideValidatorRunnable runnable = new PeptideValidatorRunnable(peptideMatchesIterator, identification, identificationFeaturesGenerator,
                        sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters, waitingHandler, exceptionHandler, metrics);
                pool.submit(runnable);
                peptideRunnables.add(runnable);

            }

            if (waitingHandler.isRunCanceled()) {

                pool.shutdownNow();
                return;

            }

            pool.shutdown();

            if (!pool.awaitTermination(identification.getPeptideIdentification().size(), TimeUnit.MINUTES)) {

                throw new InterruptedException("Peptide matches validation timed out. Please contact the developers.");

            }

            HashMap<String, Integer> validatedTotalPeptidesPerFraction = new HashMap<>();
            ArrayList<Double> validatedPeptideLengths = new ArrayList<>();

            for (PeptideValidatorRunnable runnable : peptideRunnables) {

                HashMap<String, Integer> threadValidatedTotalPeptidesPerFraction = runnable.getValidatedTotalPeptidesPerFraction();

                for (String fraction : threadValidatedTotalPeptidesPerFraction.keySet()) {

                    Integer nValidated = validatedTotalPeptidesPerFraction.get(fraction);

                    if (nValidated == null) {

                        nValidated = 0;

                    }

                    nValidated += threadValidatedTotalPeptidesPerFraction.get(fraction);
                    validatedTotalPeptidesPerFraction.put(fraction, nValidated);

                }

                validatedPeptideLengths.addAll(runnable.getValidatedPeptideLengths());

            }

            if (validatedPeptideLengths.size() >= 100) {

                NonSymmetricalNormalDistribution lengthDistribution = NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistribution(validatedPeptideLengths);
                metrics.setPeptideLengthDistribution(lengthDistribution);

            }

            metrics.setTotalPeptidesPerFraction(validatedTotalPeptidesPerFraction);

            if (projectType == ProjectType.protein) {

                // validate the proteins
                pool = Executors.newFixedThreadPool(processingParameters.getnThreads());

                ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
                ArrayList<ProteinValidatorRunnable> proteinRunnables = new ArrayList<>(processingParameters.getnThreads());

                for (int i = 1; i <= processingParameters.getnThreads(); i++) {

                    ProteinValidatorRunnable runnable = new ProteinValidatorRunnable(proteinMatchesIterator, identification, identificationFeaturesGenerator,
                            sequenceProvider, proteinDetailsProvider, geneMaps, metrics, identificationParameters,
                            waitingHandler, exceptionHandler);
                    pool.submit(runnable);
                    proteinRunnables.add(runnable);

                }

                if (waitingHandler.isRunCanceled()) {

                    pool.shutdownNow();
                    return;

                }

                pool.shutdown();

                if (!pool.awaitTermination(identification.getProteinIdentification().size(), TimeUnit.MINUTES)) {

                    throw new InterruptedException("Protein matches validation timed out. Please contact the developers.");

                }

                long[] validatedTargetProteinKeys = proteinRunnables.stream()
                        .flatMap(runnable -> runnable.getValidatedProteinMatches().stream())
                        .mapToLong(a -> a)
                        .toArray();

                metrics.setValidatedTargetProteinKeys(validatedTargetProteinKeys);

            }
        }

    }

    /**
     * Updates the validation status of a protein match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param geneMaps the gene maps
     * @param proteinMap the protein level target/decoy scoring map
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param sequenceProvider a protein sequence provider
     * @param proteinDetailsProvider a protein details provider
     * @param proteinKey the key of the protein match of interest
     * @param identificationParameters the identification parameters
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            GeneMaps geneMaps, IdentificationParameters identificationParameters,
            TargetDecoyMap proteinMap, long proteinKey) {

        ValidationQcParameters validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();

        TargetDecoyResults targetDecoyResults = proteinMap.getTargetDecoyResults();
        double fdrLimit = targetDecoyResults.getFdrLimit();
        double nTargetLimit = 100.0 / fdrLimit;
        double proteinThreshold = targetDecoyResults.getScoreLimit();
        double margin = validationQCParameters.getConfidenceMargin() * proteinMap.getResolution();
        double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + margin;

        if (proteinConfidentThreshold > 100) {

            proteinConfidentThreshold = 100;

        }

        boolean noValidated = proteinMap.getTargetDecoyResults().noValidated();

        updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator,
                sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters,
                proteinMap, proteinThreshold, nTargetLimit, proteinConfidentThreshold, noValidated, proteinKey);

    }

    /**
     * Updates the validation status of a protein match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param targetDecoyMap the protein level target/decoy map
     * @param sequenceProvider a protein sequence provider
     * @param proteinDetailsProvider a protein details provider
     * @param geneMaps the gene maps
     * @param scoreThreshold the validation score doubtfulThreshold
     * @param confidenceThreshold the confidence doubtfulThreshold after which a
     * match should be considered as confident
     * @param noValidated boolean indicating whether no validation was actually
     * conducted
     * @param nTargetLimit the limit in number of target hits before the first
     * decoy hit
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinKey the key of the protein match of interest
     * @param identificationParameters the identification parameters
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, TargetDecoyMap targetDecoyMap,
            double scoreThreshold, double nTargetLimit,
            double confidenceThreshold, boolean noValidated, long proteinKey) {

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(proteinKey)).getUrParam(psParameter);
        psParameter.resetQcResults();
        ValidationQcParameters validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();

        if (!psParameter.getManualValidation()) {

            if (identificationParameters.getSearchParameters().getFastaParameters().isTargetDecoy()) {

                if (!noValidated && psParameter.getScore() <= scoreThreshold) {

                    boolean filtersPassed = true;

                    if (validationQCParameters.getProteinFilters() != null) {
                        for (Filter filter : validationQCParameters.getProteinFilters()) {

                            ProteinFilter proteinFilter = (ProteinFilter) filter;
                            boolean validation = proteinFilter.isValidated(proteinKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, sequenceProvider, proteinDetailsProvider);
                            psParameter.setQcResult(filter.getName(), validation);

                            if (!validation) {

                                filtersPassed = false;

                            }
                        }
                    }

                    boolean confidenceThresholdPassed = psParameter.getConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                    if (filtersPassed && confidenceThresholdPassed) {

                        psParameter.setMatchValidationLevel(MatchValidationLevel.confident);

                    } else {

                        psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);

                    }
                } else {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);

                }
            } else {

                psParameter.setMatchValidationLevel(MatchValidationLevel.none);

            }
        }
    }

    /**
     * Updates the validation status of a peptide match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param peptideMap the peptide level target/decoy scoring map
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param sequenceProvider a protein sequence provider
     * @param proteinDetailsProvider a protein details provider
     * @param geneMaps the gene maps
     * @param identificationParameters the identification parameters
     * @param peptideKey the key of the peptide match of interest
     */
    public static void updatePeptideMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, TargetDecoyMap peptideMap, long peptideKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
        PSParameter psParameter = (PSParameter) ((PeptideMatch) peptideMatch).getUrParam(PSParameter.dummy);
        psParameter.resetQcResults();
        ValidationQcParameters validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();

        if (identificationParameters.getSearchParameters().getFastaParameters().isTargetDecoy()) {

            TargetDecoyResults targetDecoyResults = peptideMap.getTargetDecoyResults();
            double fdrLimit = targetDecoyResults.getFdrLimit();
            double nTargetLimit = 100.0 / fdrLimit;
            double peptideThreshold = targetDecoyResults.getScoreLimit();
            double margin = validationQCParameters.getConfidenceMargin() * peptideMap.getResolution();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;

            if (confidenceThreshold > 100) {

                confidenceThreshold = 100;

            }

            boolean noValidated = peptideMap.getTargetDecoyResults().noValidated();

            if (!noValidated && psParameter.getScore() <= peptideThreshold) {

                boolean filtersPassed = true;

                if (validationQCParameters.getPeptideFilters() != null) {
                    for (Filter filter : validationQCParameters.getPeptideFilters()) {

                        PeptideFilter peptideFilter = (PeptideFilter) filter;
                        boolean validation = peptideFilter.isValidated(peptideKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, sequenceProvider, proteinDetailsProvider);
                        psParameter.setQcResult(filter.getName(), validation);

                        if (!validation) {

                            filtersPassed = false;

                        }
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                if (filtersPassed && confidenceThresholdPassed) {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);

                } else {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);

                }
            } else {

                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);

            }
        } else {

            psParameter.setMatchValidationLevel(MatchValidationLevel.none);

        }
    }

    /**
     * Updates the validation status of a spectrum match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param geneMaps the gene maps
     * @param psmMap the PSM level target/decoy scoring map
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param sequenceProvider a protein sequence provider
     * @param proteinDetailsProvider a protein details provider
     * @param spectrumMatchKey the key of the spectrum match of interest
     * @param applyQCFilters if true quality control filters will be used
     */
    public static void updateSpectrumMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps,
            IdentificationParameters identificationParameters,
            TargetDecoyMap psmMap, long spectrumMatchKey, boolean applyQCFilters) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumMatchKey);
        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
        psParameter.resetQcResults();
        ValidationQcParameters validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();

        if (identificationParameters.getSearchParameters().getFastaParameters().isTargetDecoy()) {

            double psmThreshold = 0;
            double confidenceThreshold = 100;
            boolean noValidated = true;
            double nTargetLimit = 100;

            if (psmMap != null) {

                TargetDecoyResults targetDecoyResults = psmMap.getTargetDecoyResults();
                double fdrLimit = targetDecoyResults.getFdrLimit();
                nTargetLimit = 100.0 / fdrLimit;
                psmThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCParameters.getConfidenceMargin() * psmMap.getResolution();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;

                if (confidenceThreshold > 100) {

                    confidenceThreshold = 100;

                }

                noValidated = targetDecoyResults.noValidated();

            }

            if (!noValidated && psParameter.getScore() <= psmThreshold) {

                boolean filtersPassed = true;

                if (applyQCFilters && validationQCParameters.getPsmFilters() != null) {

                    for (Filter filter : validationQCParameters.getPsmFilters()) {

                        PsmFilter psmFilter = (PsmFilter) filter;
                        boolean validated = psmFilter.isValidated(spectrumMatchKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, sequenceProvider, proteinDetailsProvider);
                        psParameter.setQcResult(psmFilter.getName(), validated);

                        if (!validated) {

                            filtersPassed = false;

                        }
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                if (filtersPassed && confidenceThresholdPassed) {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);

                } else {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);

                }

            } else {

                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);

            }

        } else {

            psParameter.setMatchValidationLevel(MatchValidationLevel.none);

        }
    }

    /**
     * Updates the validation status of a peptide assumption. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object containing the match to
     * filter
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param sequenceProvider a protein sequence provider
     * @param proteinDetailsProvider a protein details provider
     * @param inputMap the target decoy map of all search engine scores
     * @param spectrumMatchKey the key of the spectrum match having this
     * assumption
     * @param peptideAssumption the peptide assumption of interest
     * @param identificationParameters the identification parameters
     * @param applyQCFilters boolean indicating whether QC filters should be
     * applied
     */
    public static void updatePeptideAssumptionValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, IdentificationParameters identificationParameters,
            InputMap inputMap, long spectrumMatchKey, PeptideAssumption peptideAssumption, boolean applyQCFilters) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumMatchKey);
        PSParameter psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
        ValidationQcParameters validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();

        if (identificationParameters.getSearchParameters().getFastaParameters().isTargetDecoy()) {

            TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(peptideAssumption.getAdvocate());
            double seThreshold = 0;
            double confidenceThreshold = 100;
            boolean noValidated = true;
            double nTargetLimit = 100;

            if (targetDecoyMap != null) {

                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double desiredThreshold = targetDecoyResults.getUserInput();
                nTargetLimit = 100.0 / desiredThreshold;
                seThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCParameters.getConfidenceMargin() * targetDecoyMap.getResolution();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;

                if (confidenceThreshold > 100) {

                    confidenceThreshold = 100;

                }

                noValidated = targetDecoyResults.noValidated();

            }

            if (!noValidated && peptideAssumption.getScore() <= seThreshold) {

                boolean filtersPassed = true;

                if (applyQCFilters) {

                    for (Filter filter : validationQCParameters.getPsmFilters()) {

                        PsmFilter psmFilter = (PsmFilter) filter;
                        AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
                        boolean validated = assumptionFilter.isValidated(spectrumMatchKey, spectrumMatch.getSpectrumKey(), peptideAssumption, identification, sequenceProvider, identificationFeaturesGenerator, identificationParameters);
                        psParameter.setQcResult(filter.getName(), validated);

                        if (!validated) {

                            filtersPassed = false;

                        }
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                if (filtersPassed && confidenceThresholdPassed) {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.confident);

                } else {

                    psParameter.setMatchValidationLevel(MatchValidationLevel.doubtful);

                }

            } else {

                psParameter.setMatchValidationLevel(MatchValidationLevel.not_validated);

            }

        } else {

            psParameter.setMatchValidationLevel(MatchValidationLevel.none);

        }
    }

    /**
     * Fills the peptide specific map.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided fraction information and found modifications
     * will be saved while iterating the matches
     * @param identificationParameters the identification parameters
     * @param waitingHandler the handler displaying feedback to the user
     * @param sequenceProvider the sequence provider
     */
    public void fillPeptideMaps(Identification identification, Metrics metrics, WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider) {

        waitingHandler.setWaitingText("Filling Peptide Maps. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size() * 2);

        HashSet<String> foundModifications = new HashSet<>();
        HashMap<String, ArrayList<Long>> fractionPsmMatches = new HashMap<>();

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        int nFractions = identification.getSpectrumIdentification().size();
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            long peptideKey = peptideMatch.getKey();

            foundModifications.addAll(
                    Arrays.stream(peptideMatch.getPeptide().getVariableModifications())
                            .map(ModificationMatch::getModification)
                            .collect(Collectors.toSet()));

            double probaScore = 1.0;
            HashMap<String, Double> fractionScores = new HashMap<>(nFractions);

            // get the global and fraction level peptide scores
            for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PSParameter psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                probaScore *= psmParameter.getProbability();

                if (nFractions > 1) {

                    String fraction = Spectrum.getSpectrumFile(spectrumMatch.getSpectrumKey());

                    Double fractionScore = fractionScores.get(fraction);
                    boolean change = false;

                    if (fractionScore == null) {

                        fractionScore = 1.0;
                        change = true;

                    }

                    double tempScore = psmParameter.getProbability();

                    if (tempScore != 1.0 && fractionScore != 0.0) {

                        fractionScore *= tempScore;
                        change = true;

                    }

                    if (change) {

                        fractionScores.put(fraction, fractionScore);

                    }

                    String peptideKeyString = Long.toString(peptideKey);
                    StringBuilder fractionKeyB = new StringBuilder(fraction.length() + peptideKeyString.length() + 1);
                    fractionKeyB.append(fraction).append('_').append(peptideKeyString);
                    String fractionKey = fractionKeyB.toString();

                    ArrayList<Long> spectrumMatches = fractionPsmMatches.get(fractionKey);

                    if (spectrumMatches == null) {

                        spectrumMatches = new ArrayList<>(1);
                        fractionPsmMatches.put(fractionKey, spectrumMatches);

                    }

                    spectrumMatches.add(spectrumKey);

                }
            }

            if (nFractions == 1) {

                String spectrumFile = spectrumFactory.getMgfFileNames().get(0);
                fractionScores.put(spectrumFile, probaScore);

                String peptideKeyString = Long.toString(peptideKey);
                StringBuilder fractionKeyB = new StringBuilder(spectrumFile.length() + peptideKeyString.length() + 1);
                fractionKeyB.append(spectrumFile).append('_').append(peptideKeyString);
                String fractionKey = fractionKeyB.toString();

                fractionPsmMatches.put(fractionKey, Arrays.stream(peptideMatch.getSpectrumMatchesKeys())
                        .boxed()
                        .collect(Collectors.toCollection(ArrayList::new)));

            }

            PSParameter peptideParameter = new PSParameter();

            // set the fraction scores
            for (String fractionName : fractionScores.keySet()) {

                peptideParameter.setFractionScore(fractionName, fractionScores.get(fractionName));

            }

            // Set the global score and grouping key
            peptideParameter.setScore(probaScore);

            peptideMatch.addUrParam(peptideParameter);
            peptideMap.put(peptideParameter.getScore(), PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider));

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        if (metrics != null) {

            // set the fraction psm matches
            metrics.setFractionPsmMatches(fractionPsmMatches);

            // set the ptms
            metrics.setFoundModifications(new TreeSet<>(foundModifications));

        }
    }

    /**
     * Attaches the peptide posterior error probabilities to the peptide
     * matches.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param fastaParameters the fasta file parameters
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void attachPeptideProbabilities(Identification identification, FastaParameters fastaParameters, WaitingHandler waitingHandler) {

        waitingHandler.setWaitingText("Attaching Peptide Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size());

        PSParameter psParameter = new PSParameter();
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            long peptideKey = peptideMatch.getKey();
            psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);

            if (fastaParameters.isTargetDecoy()) {

                double probability = peptideMap.getProbability(psParameter.getScore());
                psParameter.setProbability(probability);

            } else {

                psParameter.setProbability(1.0);

            }

            Set<String> fractions = psParameter.getFractions();

            if (fractions == null) {

                throw new IllegalArgumentException("Fractions not found for peptide " + peptideKey + ".");

            }

            for (String fraction : fractions) {

                if (fastaParameters.isTargetDecoy()) {

                    double probability = peptideMap.getProbability(psParameter.getFractionScore(fraction));
                    psParameter.setFractionPEP(fraction, probability);

                } else {

                    psParameter.setFractionPEP(fraction, 1.0);

                }
            }

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Fills the protein map.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void fillProteinMap(Identification identification, WaitingHandler waitingHandler) {

        waitingHandler.setWaitingText("Filling Protein Map. Please Wait...");

        int totalProgress = identification.getProteinIdentification().size();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(totalProgress);

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        int nFractions = identification.getSpectrumIdentification().size();
        ProteinMatch proteinMatch;

        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            long proteinKey = proteinMatch.getKey();

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            HashMap<String, Double> fractionScores = new HashMap<>(nFractions);
            double probaScore = 1.0;

            if (proteinMatch == null) {

                throw new IllegalArgumentException("Protein match " + proteinKey + " not found.");

            }

            // get the global and fraction level scores
            for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                probaScore = probaScore * psParameter.getProbability();

                if (nFractions > 1) {

                    for (String fraction : psParameter.getFractions()) {

                        Double fractionScore = fractionScores.get(fraction);

                        boolean change = false;

                        if (fractionScore == null) {

                            fractionScore = 1.0;
                            change = true;

                        }

                        double peptideScore = psParameter.getFractionPEP(fraction);

                        if (peptideScore != 1.0) {

                            fractionScore *= peptideScore;
                            change = true;

                        }

                        if (change) {

                            fractionScores.put(fraction, fractionScore);

                        }
                    }
                }
            }

            if (nFractions == 1) {

                String spectrumFile = spectrumFactory.getMgfFileNames().get(0);
                fractionScores.put(spectrumFile, probaScore);

            }

            PSParameter proteinParameter = new PSParameter();

            // set the fraction scores
            for (String fractionName : fractionScores.keySet()) {

                proteinParameter.setFractionScore(fractionName, fractionScores.get(fractionName));

            }

            // Set the global score
            proteinParameter.setScore(probaScore);
            proteinMatch.addUrParam(proteinParameter);
            proteinMap.put(proteinParameter.getScore(), proteinMatch.isDecoy());

        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Attaches the protein posterior error probability to the protein matches.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param sequenceProvider the sequence provider
     * @param fastaParameters the FASTA file parameters
     * @param metrics if provided fraction information
     * @param waitingHandler the handler displaying feedback to the user
     * @param fractionParameters the fraction parameters
     */
    public void attachProteinProbabilities(Identification identification, SequenceProvider sequenceProvider, FastaParameters fastaParameters, Metrics metrics, WaitingHandler waitingHandler,
            FractionParameters fractionParameters) {

        waitingHandler.setWaitingText("Attaching Protein Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());

        HashMap<String, ArrayList<Double>> fractionMW = new HashMap<>();

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        ProteinMatch proteinMatch;

        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            double proteinMW = ProteinUtils.computeMolecularWeight(
                    sequenceProvider.getSequence(proteinMatch.getLeadingAccession()));

            PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

            if (fastaParameters.isTargetDecoy()) {

                double proteinProbability = proteinMap.getProbability(psParameter.getScore());
                psParameter.setProbability(proteinProbability);

            } else {

                psParameter.setProbability(1.0);

            }

            for (String fraction : psParameter.getFractions()) {

                if (fastaParameters.isTargetDecoy()) {

                    psParameter.setFractionPEP(fraction, proteinMap.getProbability(psParameter.getFractionScore(fraction)));

                } else {

                    psParameter.setFractionPEP(fraction, 1.0);

                }

                // set the fraction molecular weights
                if (!proteinMatch.isDecoy() && psParameter.getFractionConfidence(fraction) > fractionParameters.getProteinConfidenceMwPlots()) {

                    ArrayList<Double> mw = fractionMW.get(fraction);

                    if (mw == null) {

                        mw = new ArrayList<>(1);
                        fractionMW.put(fraction, mw);

                    }

                    mw.add(proteinMW);

                }
            }

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }
        }

        if (metrics != null) {

            // set the observed fractional molecular weights per fraction
            metrics.setObservedFractionalMassesAll(fractionMW);

        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Returns the PSM scoring specific map.
     *
     * @return the PSM scoring specific map
     */
    public TargetDecoyMap getPsmMap() {

        return psmMap;

    }

    /**
     * Sets the PSM scoring specific map.
     *
     * @param psmMap the PSM scoring specific map
     */
    public void setPsmMap(TargetDecoyMap psmMap) {

        this.psmMap = psmMap;

    }

    /**
     * Returns the peptide scoring specific map.
     *
     * @return the peptide scoring specific map
     */
    public TargetDecoyMap getPeptideMap() {

        return peptideMap;

    }

    /**
     * Sets the peptide scoring specific map.
     *
     * @param peptideMap the peptide scoring specific map
     */
    public void setPeptideMap(TargetDecoyMap peptideMap) {

        this.peptideMap = peptideMap;

    }

    /**
     * Returns the protein scoring map.
     *
     * @return the protein scoring map
     */
    public TargetDecoyMap getProteinMap() {

        return proteinMap;

    }

    /**
     * Sets the protein scoring map.
     *
     * @param proteinMap the protein scoring map
     */
    public void setProteinMap(TargetDecoyMap proteinMap) {

        this.proteinMap = proteinMap;

    }

    /**
     * Sets the default matches quality control filters.
     *
     * @param validationQCParameters the default matches quality control filters
     */
    public static void setDefaultMatchesQCFilters(ValidationQcParameters validationQCParameters) {

        ArrayList<Filter> psmFilters = new ArrayList<>(2);
        PsmFilter psmFilter = new PsmFilter("Fragment Ion Sequence Coverage");
        psmFilter.setDescription("Sequence coverage filter by fragment ions");
        psmFilter.setFilterItem(AssumptionFilterItem.sequenceCoverage.name, FilterItemComparator.higherOrEqual, 30);
        psmFilter.getAssumptionFilter().setFilterItem(AssumptionFilterItem.sequenceCoverage.name, FilterItemComparator.higherOrEqual, 30);
        psmFilters.add(psmFilter);
        psmFilter = new PsmFilter("Mass deviation");
        psmFilter.setDescription("Precursor m/z deviation probability");
        psmFilter.setFilterItem(AssumptionFilterItem.precrusorMzErrorStat.name, FilterItemComparator.higherOrEqual, 0.0001);
        psmFilter.getAssumptionFilter().setFilterItem(AssumptionFilterItem.precrusorMzErrorStat.name, FilterItemComparator.higherOrEqual, 0.0001);
        psmFilters.add(psmFilter);
        validationQCParameters.setPsmFilters(psmFilters);

        ArrayList<Filter> peptideFilters = new ArrayList<>(1);
        PeptideFilter peptideFilter = new PeptideFilter("One confident PSM");
        peptideFilter.setDescription("Number of confident PSMs filter");
        peptideFilter.setFilterItem(PeptideFilterItem.nConfidentPSMs.name, FilterItemComparator.higherOrEqual, 1);
        peptideFilters.add(peptideFilter);
        validationQCParameters.setPeptideFilters(peptideFilters);

        ArrayList<Filter> proteinFilters = new ArrayList<>(2);
        ProteinFilter proteinFilter = new ProteinFilter(">=2 confident peptides");
        proteinFilter.setDescription("Number of confident peptides filter");
        proteinFilter.setFilterItem(ProteinFilterItem.nConfidentPeptides.name, FilterItemComparator.higherOrEqual, 2);
        proteinFilters.add(proteinFilter);
        proteinFilter = new ProteinFilter(">=2 confident spectra");
        proteinFilter.setDescription("Number of confident spectra filter");
        proteinFilter.setFilterItem(ProteinFilterItem.nConfidentPSMs.name, FilterItemComparator.higherOrEqual, 2);
        proteinFilters.add(proteinFilter);
        validationQCParameters.setProteinFilters(proteinFilters);

    }

    /**
     * Runnable validating PSMs.
     *
     * @author Marc Vaudel
     */
    private class PsmValidatorRunnable implements Runnable {

        /**
         * An iterator for the PSMs.
         */
        private final SpectrumMatchesIterator psmIterator;
        /**
         * The identification.
         */
        private final Identification identification;
        /**
         * The identification features generator used to estimate, store and
         * retrieve identification features.
         */
        private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
        /**
         * The sequence provider.
         */
        private final SequenceProvider sequenceProvider;
        /**
         * The protein details provider.
         */
        private final ProteinDetailsProvider proteinDetailsProvider;
        /**
         * The gene maps.
         */
        private final GeneMaps geneMaps;
        /**
         * The identification parameters.
         */
        private final IdentificationParameters identificationParameters;
        /**
         * The waiting handler.
         */
        private final WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private final ExceptionHandler exceptionHandler;
        /**
         * List used to store precursor m/z deviations of matches currently
         * validated.
         */
        private final HashMap<String, ArrayList<Double>> threadPrecursorMzDeviations = new HashMap<>(128);
        /**
         * If not null, information on search engine agreement will be stored in
         * the input map.
         */
        private final InputMap inputMap;
        /**
         * If true, quality control filters will be applied to the matches.
         */
        private final boolean applyQCFilters;
        /**
         * If true, advocate contributions will be stored in the input map.
         */
        private final boolean storeContributions;

        /**
         * Constructor.
         *
         * @param psmIterator a PSM iterator
         * @param identification the identification containing the matches
         * @param identificationFeaturesGenerator the identification features
         * generator used to estimate, store and retrieve identification
         * features
         * @param sequenceProvider a protein sequence provider
         * @param proteinDetailsProvider a protein details provider
         * @param geneMaps the gene maps
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         * @param inputMap if provided information on search engine agreement
         * will be stored in the input map
         * @param applyQCFilters boolean indicating whether quality control
         * filters should be used
         * @param storeContributions boolean indicating whether advocate
         * contributions should be stored.
         */
        public PsmValidatorRunnable(SpectrumMatchesIterator psmIterator, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
                SequenceProvider sequenceProvider,
                ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, InputMap inputMap, boolean applyQCFilters, boolean storeContributions) {

            this.psmIterator = psmIterator;
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
            this.sequenceProvider = sequenceProvider;
            this.proteinDetailsProvider = proteinDetailsProvider;
            this.geneMaps = geneMaps;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.inputMap = inputMap;
            this.applyQCFilters = applyQCFilters;
            this.storeContributions = storeContributions;

        }

        @Override
        public void run() {
            try {

                SpectrumMatch spectrumMatch;
                while ((spectrumMatch = psmIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    long spectrumKey = spectrumMatch.getKey();
                    String spectrumFileName = Spectrum.getSpectrumFile(spectrumMatch.getSpectrumKey());

                    if (spectrumMatch.getBestPeptideAssumption() == null) {

                        continue;

                    }

                    updateSpectrumMatchValidationLevel(identification, identificationFeaturesGenerator, sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters, psmMap, spectrumKey, applyQCFilters);

                    // update assumption validation level
                    HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = spectrumMatch.getPeptideAssumptionsMap();

                    for (TreeMap<Double, ArrayList<PeptideAssumption>> algorithmMap : assumptions.values()) {

                        for (ArrayList<PeptideAssumption> scoreList : algorithmMap.values()) {

                            for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : scoreList) {

                                updatePeptideAssumptionValidationLevel(identification, identificationFeaturesGenerator, sequenceProvider, proteinDetailsProvider, identificationParameters, inputMap, spectrumKey, (PeptideAssumption) spectrumIdentificationAssumption, applyQCFilters);

                            }
                        }
                    }

                    // update search engine agreement
                    PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (peptideAssumption != null) {

                        if (psParameter.getMatchValidationLevel().isValidated() && !PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider)) {

                            double precursorMz = spectrumFactory.getPrecursorMz(spectrumMatch.getSpectrumKey());
                            SearchParameters searchParameters = identificationParameters.getSearchParameters();
                            double precursorMzError = peptideAssumption.getDeltaMass(precursorMz, searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());

                            ArrayList<Double> fileDeviations = threadPrecursorMzDeviations.get(spectrumFileName);

                            if (fileDeviations == null) {

                                fileDeviations = new ArrayList<>();
                                threadPrecursorMzDeviations.put(spectrumFileName, fileDeviations);

                            }

                            fileDeviations.add(precursorMzError);

                            if (inputMap != null && storeContributions) {

                                Peptide bestPeptide = peptideAssumption.getPeptide();
                                HashSet<Integer> agreementAdvocates = new HashSet<>();

                                for (int advocateId : assumptions.keySet()) {

                                    TreeMap<Double, ArrayList<PeptideAssumption>> advocateAssumptions = assumptions.get(advocateId);
                                    ArrayList<PeptideAssumption> firstHits = advocateAssumptions.firstEntry().getValue();

                                    for (PeptideAssumption firstHit : firstHits) {

                                        Peptide advocatePeptide = firstHit.getPeptide();

                                        if (bestPeptide.isSameSequenceAndModificationStatus(advocatePeptide, identificationParameters.getSequenceMatchingParameters())) {

                                            agreementAdvocates.add(advocateId);
                                            break;

                                        }
                                    }
                                }

                                boolean unique = agreementAdvocates.size() == 1;

                                for (int advocateId : agreementAdvocates) {

                                    inputMap.addAdvocateContribution(advocateId, spectrumFileName, unique);

                                }

                                inputMap.addPeptideShakerHit(spectrumFileName, agreementAdvocates.isEmpty());

                            }
                        }
                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }
            } catch (Exception e) {
                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();
            }
        }

        /**
         * Returns the precursor m/z deviations of the validated PSMs.
         *
         * @return the precursor m/z deviations of the validated PSMs
         */
        public HashMap<String, ArrayList<Double>> getThreadPrecursorMzDeviations() {
            return threadPrecursorMzDeviations;
        }
    }

    /**
     * Runnable validating peptides.
     *
     * @author Marc Vaudel
     */
    private class PeptideValidatorRunnable implements Runnable {

        /**
         * An iterator for the peptide matches.
         */
        private final PeptideMatchesIterator peptideMatchesIterator;
        /**
         * The identification.
         */
        private final Identification identification;
        /**
         * The identification features generator used to estimate, store and
         * retrieve identification features.
         */
        private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
        /**
         * The sequence provider.
         */
        private final SequenceProvider sequenceProvider;
        /**
         * The protein details provider.
         */
        private final ProteinDetailsProvider proteinDetailsProvider;
        /**
         * The gene maps.
         */
        private final GeneMaps geneMaps;
        /**
         * The identification parameters.
         */
        private final IdentificationParameters identificationParameters;
        /**
         * The waiting handler.
         */
        private final WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private final ExceptionHandler exceptionHandler;
        /**
         * List used to store the length of the validated peptides.
         */
        private final ArrayList<Double> validatedPeptideLengths = new ArrayList<>();
        /**
         * Map used to store the number of validated peptides per fraction.
         */
        private final HashMap<String, Integer> validatedTotalPeptidesPerFraction = new HashMap<>();
        /**
         * The object used to store metrics on the project.
         */
        private final Metrics metrics;

        /**
         * Constructor.
         *
         * @param psmIterator a peptide matches iterator
         * @param identification the identification containing the matches
         * @param identificationFeaturesGenerator the identification features
         * generator used to estimate, store and retrieve identification
         * features
         * @param sequenceProvider a protein sequence provider
         * @param proteinDetailsProvider a protein details provider
         * @param geneMaps the gene maps
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         * @param validatedPeptideLengths list used to store the length of the
         * validated peptides
         * @param validatedTotalPeptidesPerFraction map used to store the number
         * of validated peptides per fraction
         * @param metrics the object used to store metrics on the project
         */
        public PeptideValidatorRunnable(PeptideMatchesIterator peptideMatchesIterator, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
                SequenceProvider sequenceProvider,
                ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, Metrics metrics) {

            this.peptideMatchesIterator = peptideMatchesIterator;
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
            this.sequenceProvider = sequenceProvider;
            this.proteinDetailsProvider = proteinDetailsProvider;
            this.geneMaps = geneMaps;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.metrics = metrics;

        }

        @Override
        public void run() {
            try {

                PeptideMatch peptideMatch;

                while ((peptideMatch = peptideMatchesIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    long peptideKey = peptideMatch.getKey();

                    updatePeptideMatchValidationLevel(identification, identificationFeaturesGenerator, sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters, peptideMap, peptideKey);

                    // set the fraction details
                    PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                    if (psParameter.getMatchValidationLevel().isValidated()) {

                        double length = peptideMatch.getPeptide().getSequence().length();
                        validatedPeptideLengths.add(length);

                    }

                    // @TODO: could be a better more elegant way of doing this?
                    HashMap<String, Integer> validatedPsmsPerFraction = new HashMap<>(psParameter.getFractionScore().size());
                    HashMap<String, ArrayList<Double>> precursorIntensitesPerFractionPeptideLevel = new HashMap<>(psParameter.getFractionScore().size());

                    for (String fractionName : psParameter.getFractions()) {

                        ArrayList<Double> precursorIntensities = new ArrayList<>();

                        String peptideKeyString = Long.toString(peptideKey);
                        StringBuilder fractionKeyB = new StringBuilder(fractionName.length() + peptideKeyString.length() + 1);
                        fractionKeyB.append(fractionName).append('_').append(peptideKeyString);
                        String fractionKey = fractionKeyB.toString();

                        ArrayList<Long> spectrumKeys = metrics.getFractionPsmMatches().get(fractionKey);

                        if (spectrumKeys != null) {

                            for (long spectrumKey : spectrumKeys) {

                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                                PSParameter psParameter2 = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                                if (psParameter2.getMatchValidationLevel().isValidated()) {

                                    if (validatedPsmsPerFraction.containsKey(fractionName)) {

                                        int value = validatedPsmsPerFraction.get(fractionName);
                                        validatedPsmsPerFraction.put(fractionName, value + 1);

                                    } else {

                                        validatedPsmsPerFraction.put(fractionName, 1);

                                    }

                                    double intensity = SpectrumFactory.getInstance().getPrecursor(spectrumMatch.getSpectrumKey()).getIntensity();

                                    if (intensity > 0) {

                                        precursorIntensities.add(intensity);

                                    }
                                }

                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                            }
                        }

                        precursorIntensitesPerFractionPeptideLevel.put(fractionName, precursorIntensities);

                        // save the total number of peptides per fraction
                        if (psParameter.getMatchValidationLevel().isValidated()) {

                            addValidatedPeptideForFraction(fractionName);

                        }
                    }

                    // set the number of validated spectra per fraction for each peptide
                    psParameter.setValidatedSpectraPepFraction(validatedPsmsPerFraction);
                    psParameter.setPrecursorIntensityPerFraction(precursorIntensitesPerFractionPeptideLevel);

                    waitingHandler.increaseSecondaryProgressCounter();

                }

            } catch (Exception e) {

                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();

            }
        }

        /**
         * Adds a validated peptide to the validatedTotalPeptidesPerFraction
         * map.
         *
         * @param fractionName the name of the fraction
         */
        private synchronized void addValidatedPeptideForFraction(String fractionName) {

            if (validatedTotalPeptidesPerFraction.containsKey(fractionName)) {

                int value = validatedTotalPeptidesPerFraction.get(fractionName);
                validatedTotalPeptidesPerFraction.put(fractionName, value + 1);

            } else {

                validatedTotalPeptidesPerFraction.put(fractionName, 1);

            }
        }

        /**
         * Returns a list of the lengths of the peptides validated.
         *
         * @return a list of the lengths of the peptides validated
         */
        public ArrayList<Double> getValidatedPeptideLengths() {

            return validatedPeptideLengths;

        }

        /**
         * Returns a map of the number of peptides validated per fraction.
         *
         * @return a map of the number of peptides validated per fraction
         */
        public HashMap<String, Integer> getValidatedTotalPeptidesPerFraction() {

            return validatedTotalPeptidesPerFraction;

        }
    }

    /**
     * Runnable validating protein matches.
     *
     * @author Marc Vaudel
     */
    private class ProteinValidatorRunnable implements Runnable {

        /**
         * An iterator for the protein matches.
         */
        private final ProteinMatchesIterator proteinMatchesIterator;
        /**
         * The identification.
         */
        private final Identification identification;
        /**
         * The identification features generator used. to estimate, store and
         * retrieve identification features
         */
        private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
        /**
         * The sequence provider.
         */
        private final SequenceProvider sequenceProvider;
        /**
         * The protein details provider.
         */
        private final ProteinDetailsProvider proteinDetailsProvider;
        /**
         * The gene maps.
         */
        private final GeneMaps geneMaps;
        /**
         * The identification parameters.
         */
        private final IdentificationParameters identificationParameters;
        /**
         * The waiting handler.
         */
        private final WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private final ExceptionHandler exceptionHandler;
        /**
         * The validation QC parameters.
         */
        private final ValidationQcParameters validationQCParameters;
        /**
         * The object used to store metrics on the project.
         */
        private final Metrics metrics;
        /**
         * Keep track of the validated target protein matches.
         */
        private HashSet<Long> validatedProteinMatches = new HashSet<>();

        /**
         * Constructor.
         *
         * @param proteinMatchesIterator a protein matches iterator
         * @param identification the identification containing the matches
         * @param identificationFeaturesGenerator the identification features
         * generator used to estimate, store and retrieve identification
         * features
         * @param sequenceProvider a protein sequence provider
         * @param proteinDetailsProvider a protein details provider
         * @param geneMaps the gene maps
         * @param metrics the object used to store metrics on the project
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public ProteinValidatorRunnable(ProteinMatchesIterator proteinMatchesIterator, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
                SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, GeneMaps geneMaps, Metrics metrics,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {

            this.proteinMatchesIterator = proteinMatchesIterator;
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
            this.sequenceProvider = sequenceProvider;
            this.proteinDetailsProvider = proteinDetailsProvider;
            this.geneMaps = geneMaps;
            this.metrics = metrics;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.validationQCParameters = identificationParameters.getIdValidationParameters().getValidationQCParameters();

        }

        @Override
        public void run() {
            try {

                TargetDecoyResults targetDecoyResults = proteinMap.getTargetDecoyResults();
                double desiredThreshold = targetDecoyResults.getUserInput();
                double nTargetLimit = 100.0 / desiredThreshold;
                double proteinThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCParameters.getConfidenceMargin() * proteinMap.getResolution();
                double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + margin;

                if (proteinConfidentThreshold > 100) {
                    proteinConfidentThreshold = 100;
                }

                boolean noValidated = proteinMap.getTargetDecoyResults().noValidated();
                int maxValidatedSpectraFractionLevel = 0;
                int maxValidatedPeptidesFractionLevel = 0;
                double maxProteinAveragePrecursorIntensity = 0.0, maxProteinSummedPrecursorIntensity = 0.0;

                ProteinMatch proteinMatch;
                while ((proteinMatch = proteinMatchesIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    long proteinKey = proteinMatch.getKey();
                    updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, sequenceProvider, proteinDetailsProvider, geneMaps, identificationParameters,
                            proteinMap, proteinThreshold, nTargetLimit, proteinConfidentThreshold, noValidated, proteinKey);

                    PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                    // Load the coverage in cache
                    if (!proteinMatch.isDecoy() && psParameter.getMatchValidationLevel().isValidated()) {

                        identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                        validatedProteinMatches.add(proteinKey);

                    }

                    // set the fraction details
                    // @TODO: could be a better more elegant way of doing this?
                    HashMap<String, Integer> validatedPsmsPerFraction = new HashMap<>();
                    HashMap<String, Integer> validatedPeptidesPerFraction = new HashMap<>();
                    HashMap<String, ArrayList<Double>> precursorIntensitesPerFractionProteinLevel = new HashMap<>();

                    for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        PSParameter psParameter2 = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                        for (String fraction : psParameter2.getFractions()) {

                            if (psParameter2.getFractionValidatedSpectra(fraction) != null) {

                                if (validatedPsmsPerFraction.containsKey(fraction)) {

                                    int value = validatedPsmsPerFraction.get(fraction);
                                    validatedPsmsPerFraction.put(fraction, value + psParameter2.getFractionValidatedSpectra(fraction));

                                } else {

                                    validatedPsmsPerFraction.put(fraction, psParameter2.getFractionValidatedSpectra(fraction));

                                }

                                if (validatedPsmsPerFraction.get(fraction) > maxValidatedSpectraFractionLevel) {

                                    maxValidatedSpectraFractionLevel = validatedPsmsPerFraction.get(fraction);

                                }
                            }

                            ArrayList<Double> peptideIntensities = psParameter2.getPrecursorIntensityPerFraction(fraction);

                            if (peptideIntensities != null) {

                                ArrayList<Double> proteinIntensities = precursorIntensitesPerFractionProteinLevel.get(fraction);

                                if (proteinIntensities != null) {

                                    proteinIntensities.addAll(peptideIntensities);

                                } else {

                                    precursorIntensitesPerFractionProteinLevel.put(fraction, new ArrayList<>(peptideIntensities));

                                }
                            }

                            if (psParameter2.getMatchValidationLevel().isValidated()) {

                                if (validatedPeptidesPerFraction.containsKey(fraction)) {

                                    int value = validatedPeptidesPerFraction.get(fraction);
                                    validatedPeptidesPerFraction.put(fraction, value + 1);

                                } else {

                                    validatedPeptidesPerFraction.put(fraction, 1);

                                }

                                if (validatedPeptidesPerFraction.get(fraction) > maxValidatedPeptidesFractionLevel) {

                                    maxValidatedPeptidesFractionLevel = validatedPeptidesPerFraction.get(fraction);

                                }
                            }
                        }

                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    }

                    // set the number of validated spectra and peptides per fraction for each protein
                    if (psParameter.getFractionScore().size() > 1) {

                        psParameter.setValidatedSpectraPepFraction(validatedPsmsPerFraction);
                        psParameter.setValidatedPeptidesPerFraction(validatedPeptidesPerFraction);
                        psParameter.setPrecursorIntensityPerFraction(precursorIntensitesPerFractionProteinLevel);

                        for (String fraction : psParameter.getFractions()) {

                            if (psParameter.getPrecursorIntensityAveragePerFraction(fraction) != null) {

                                if (psParameter.getPrecursorIntensityAveragePerFraction(fraction) > maxProteinAveragePrecursorIntensity) {

                                    maxProteinAveragePrecursorIntensity = psParameter.getPrecursorIntensityAveragePerFraction(fraction);

                                }

                                if (psParameter.getPrecursorIntensitySummedPerFraction(fraction) != null && psParameter.getPrecursorIntensitySummedPerFraction(fraction) > maxProteinSummedPrecursorIntensity) {

                                    maxProteinAveragePrecursorIntensity = psParameter.getPrecursorIntensitySummedPerFraction(fraction);

                                }
                            }
                        }
                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }

                // set the max values in the metrics
                setMaxValues(maxValidatedPeptidesFractionLevel, maxValidatedSpectraFractionLevel, maxProteinAveragePrecursorIntensity, maxProteinSummedPrecursorIntensity);

            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

        /**
         * Sets the max values in the metrics.
         *
         * @param maxValidatedPeptidesFractionLevel the maximal number of
         * validated peptides in a fraction
         * @param maxValidatedSpectraFractionLevel the maximal number of
         * validated spectra in a fraction
         * @param maxProteinAveragePrecursorIntensity the maximal protein
         * average intensity
         * @param maxProteinSummedPrecursorIntensity the maximal protein summed
         * intensity
         */
        private synchronized void setMaxValues(int maxValidatedPeptidesFractionLevel, int maxValidatedSpectraFractionLevel, double maxProteinAveragePrecursorIntensity, double maxProteinSummedPrecursorIntensity) {

            if (metrics.getMaxValidatedPeptidesPerFraction() != null && maxValidatedPeptidesFractionLevel > metrics.getMaxValidatedPeptidesPerFraction()) {

                metrics.setMaxValidatedPeptidesPerFraction(maxValidatedPeptidesFractionLevel);

            }

            if (metrics.getMaxValidatedSpectraPerFraction() != null && maxValidatedSpectraFractionLevel > metrics.getMaxValidatedSpectraPerFraction()) {

                metrics.setMaxValidatedSpectraPerFraction(maxValidatedSpectraFractionLevel);

            }

            if (metrics.getMaxProteinAveragePrecursorIntensity() != null && maxProteinAveragePrecursorIntensity > metrics.getMaxProteinAveragePrecursorIntensity()) {

                metrics.setMaxProteinAveragePrecursorIntensity(maxProteinAveragePrecursorIntensity);

            }

            if (metrics.getMaxProteinSummedPrecursorIntensity() != null && maxProteinSummedPrecursorIntensity > metrics.getMaxProteinSummedPrecursorIntensity()) {

                metrics.setMaxProteinSummedPrecursorIntensity(maxProteinSummedPrecursorIntensity);

            }
        }

        /**
         * Returns the keys of the validated target protein matches.
         *
         * @return the keys of the validated target protein matches
         */
        public HashSet<Long> getValidatedProteinMatches() {
            return validatedProteinMatches;
        }
    }
}
