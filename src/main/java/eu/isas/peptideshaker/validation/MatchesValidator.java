package eu.isas.peptideshaker.validation;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ValidationQCPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import com.compomics.util.experiment.filtering.FilterItemComparator;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.FractionSettings;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.filtering.items.AssumptionFilterItem;
import eu.isas.peptideshaker.filtering.items.PeptideFilterItem;
import eu.isas.peptideshaker.filtering.items.ProteinFilterItem;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.maps.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
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
 * This class validates the quality of identification matches.
 *
 * @author Marc Vaudel
 */
public class MatchesValidator {

    /**
     * The PSM target decoy map.
     */
    private PsmSpecificMap psmMap;
    /**
     * The peptide target decoy map.
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The protein target decoy map.
     */
    private ProteinMap proteinMap;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The protein sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * Constructor.
     *
     * @param psmMap the PSM target decoy map
     * @param peptideMap the peptide target decoy map
     * @param proteinMap the protein target decoy map
     */
    public MatchesValidator(PsmSpecificMap psmMap, PeptideSpecificMap peptideMap, ProteinMap proteinMap) {
        this.psmMap = psmMap;
        this.peptideMap = peptideMap;
        this.proteinMap = proteinMap;
    }

    /**
     * Validates the identification matches comprised in an identification
     * object based on the target/decoy strategy and quality control metrics
     * based on given FDR thresholds.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided, metrics on fractions will be saved while
     * iterating the matches
     * @param geneMaps the gene maps
     * @param waitingHandler a waiting handler displaying progress to the user
     * and allowing canceling the process
     * @param exceptionHandler handler for exceptions
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator providing information about the matches
     * @param inputMap the input target/decoy map
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param processingPreferences the processing preferences
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void validateIdentifications(Identification identification, Metrics metrics, GeneMaps geneMaps, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler,
            IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator, InputMap inputMap,
            SpectrumCountingPreferences spectrumCountingPreferences, ProcessingPreferences processingPreferences) throws SQLException, IOException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        IdMatchValidationPreferences validationPreferences = identificationParameters.getIdValidationPreferences();

        waitingHandler.setWaitingText("Finding FDR Thresholds. Please Wait...");

        TargetDecoyMap currentMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults currentResults = currentMap.getTargetDecoyResults();
        currentResults.setInputType(1);
        currentResults.setUserInput(validationPreferences.getDefaultProteinFDR());
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(validationPreferences.getDefaultProteinFDR());
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        ArrayList<TargetDecoyMap> psmMaps = psmMap.getTargetDecoyMaps(),
                inputMaps = inputMap.getTargetDecoyMaps();

        int totalProgress = peptideMap.getKeys().size() + psmMaps.size() + inputMap.getNalgorithms();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(totalProgress);

        for (String mapKey : peptideMap.getKeys()) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.increaseSecondaryProgressCounter();
            currentMap = peptideMap.getTargetDecoyMap(mapKey);
            currentResults = currentMap.getTargetDecoyResults();
            currentResults.setInputType(1);
            currentResults.setUserInput(validationPreferences.getDefaultPeptideFDR());
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(validationPreferences.getDefaultPeptideFDR());
            currentMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        for (TargetDecoyMap targetDecoyMap : psmMaps) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.increaseSecondaryProgressCounter();
            currentResults = targetDecoyMap.getTargetDecoyResults();
            currentResults.setInputType(1);
            currentResults.setUserInput(validationPreferences.getDefaultPsmFDR());
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(validationPreferences.getDefaultPsmFDR());
            targetDecoyMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        for (TargetDecoyMap targetDecoyMap : inputMaps) {
            if (waitingHandler.isRunCanceled()) {
                return;
            }
            waitingHandler.increaseSecondaryProgressCounter();
            currentResults = targetDecoyMap.getTargetDecoyResults();
            currentResults.setInputType(1);
            currentResults.setUserInput(validationPreferences.getDefaultPsmFDR());
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(validationPreferences.getDefaultPsmFDR());
            targetDecoyMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        validateIdentifications(identification, metrics, geneMaps, inputMap, waitingHandler, exceptionHandler,
                identificationFeaturesGenerator, identificationParameters, spectrumCountingPreferences, processingPreferences);

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
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
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param processingPreferences the processing preferences
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void validateIdentifications(Identification identification, Metrics metrics, GeneMaps geneMaps, InputMap inputMap,
            WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters,
            SpectrumCountingPreferences spectrumCountingPreferences, ProcessingPreferences processingPreferences)
            throws SQLException, IOException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Match Validation and Quality Control. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size()
                    + identification.getPeptideIdentification().size()
                    + 2 * identification.getSpectrumIdentificationSize());
        }

        // validate the spectrum matches
        if (inputMap != null) {
            inputMap.resetAdvocateContributions();
        }

        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
        Double intensityLimit = annotationPreferences.getAnnotationIntensityLimit();
        annotationPreferences.setIntensityLimit(0);

        ExecutorService pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());

        PsmIterator psmIterator = identification.getPsmIterator(waitingHandler);

        ArrayList<PsmValidatorRunnable> psmRunnables = new ArrayList<PsmValidatorRunnable>(processingPreferences.getnThreads());
        for (int i = 1; i <= processingPreferences.getnThreads(); i++) {
            PsmValidatorRunnable runnable = new PsmValidatorRunnable(psmIterator, identification, identificationFeaturesGenerator, geneMaps, identificationParameters, waitingHandler, exceptionHandler, inputMap, false, true);
            pool.submit(runnable);
            psmRunnables.add(runnable);
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }
        }
        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            pool.shutdownNow();
            return;
        }
        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM validation timed out. Please contact the developers.");
        }

        HashMap<String, ArrayList<Double>> precursorMzDeviations = new HashMap<String, ArrayList<Double>>();
        for (PsmValidatorRunnable runnable : psmRunnables) {
            precursorMzDeviations.putAll(runnable.getThreadPrecursorMzDeviations());
        }

        for (String spectrumFileName : precursorMzDeviations.keySet()){
            ArrayList<Double> precursorMzDeviationsFile = precursorMzDeviations.get(spectrumFileName);
            if (precursorMzDeviations.size() >= 100) {
                Collections.sort(precursorMzDeviationsFile);
                identificationFeaturesGenerator.setMassErrorDistribution(spectrumFileName, precursorMzDeviationsFile);
            } else {
                // There are not enough precursors, disable probabilistic precursor filter
                for (Filter filter : validationQCPreferences.getPsmFilters()) {
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

        pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());

        psmIterator = identification.getPsmIterator(waitingHandler);

        for (int i = 1; i <= processingPreferences.getnThreads(); i++) {
            PsmValidatorRunnable runnable = new PsmValidatorRunnable(psmIterator, identification, identificationFeaturesGenerator, geneMaps, identificationParameters, waitingHandler, exceptionHandler, inputMap, true, false);
            pool.submit(runnable);
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }
        }
        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            pool.shutdownNow();
            return;
        }
        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM validation timed out. Please contact the developers.");
        }

        annotationPreferences.setIntensityLimit(intensityLimit);

        // validate the peptides
        pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());
        ArrayList<PeptideValidatorRunnable> peptideRunnables = new ArrayList<PeptideValidatorRunnable>(processingPreferences.getnThreads());

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

        for (int i = 1; i <= processingPreferences.getnThreads(); i++) {
            PeptideValidatorRunnable runnable = new PeptideValidatorRunnable(peptideMatchesIterator, identification, identificationFeaturesGenerator, geneMaps, identificationParameters, waitingHandler, exceptionHandler, metrics);
            pool.submit(runnable);
            peptideRunnables.add(runnable);
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }
        }
        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            pool.shutdownNow();
            return;
        }
        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM validation timed out. Please contact the developers.");
        }

        HashMap<String, Integer> validatedTotalPeptidesPerFraction = new HashMap<String, Integer>();
        ArrayList<Double> validatedPeptideLengths = new ArrayList<Double>();
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

//        ObjectsDB.setDebugInteractions(true);
        
        // validate the proteins
        pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        ArrayList<ProteinValidatorRunnable> proteinRunnables = new ArrayList<ProteinValidatorRunnable>(processingPreferences.getnThreads());
        for (int i = 1; i <= processingPreferences.getnThreads(); i++) {
            ProteinValidatorRunnable runnable = new ProteinValidatorRunnable(proteinMatchesIterator, identification, identificationFeaturesGenerator, geneMaps, metrics, identificationParameters, spectrumCountingPreferences, waitingHandler, exceptionHandler);
            pool.submit(runnable);
            proteinRunnables.add(runnable);
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }
        }
        if (waitingHandler != null && waitingHandler.isRunCanceled()) {
            pool.shutdownNow();
            return;
        }
        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM validation timed out. Please contact the developers.");
        }

        double totalSpectrumCounting = 0;
        for (ProteinValidatorRunnable runnable : proteinRunnables) {
            totalSpectrumCounting += runnable.getTotalSpectrumCounting();
        }
        metrics.setTotalSpectrumCounting(totalSpectrumCounting);

        double totalSpectrumCountingMass = 0;
        for (ProteinValidatorRunnable runnable : proteinRunnables) {
            totalSpectrumCountingMass += runnable.getTotalSpectrumCountingMass();
        }
        metrics.setTotalSpectrumCountingMass(totalSpectrumCountingMass);
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
     * @param proteinKey the key of the protein match of interest
     * @param identificationParameters the identification parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     * @throws org.apache.commons.math.MathException Exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, ProteinMap proteinMap, String proteinKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();
        TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
        double fdrLimit = targetDecoyResults.getFdrLimit();
        double nTargetLimit = 100.0 / fdrLimit;
        double proteinThreshold = targetDecoyResults.getScoreLimit();
        double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
        double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + margin;

        if (proteinConfidentThreshold > 100) {
            proteinConfidentThreshold = 100;
        }

        boolean noValidated = proteinMap.getTargetDecoyMap().getTargetDecoyResults().noValidated();
        updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, geneMaps, identificationParameters,
                targetDecoyMap, proteinThreshold, nTargetLimit, proteinConfidentThreshold, noValidated, proteinKey);
    }

    /**
     * Updates the validation status of a protein match. If the match was
     * manually validated nothing will be changed.
     *
     * @param identification the identification object
     * @param targetDecoyMap the protein level target/decoy map
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
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     * @throws org.apache.commons.math.MathException Exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public static void updateProteinMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, TargetDecoyMap targetDecoyMap, double scoreThreshold, double nTargetLimit,
            double confidenceThreshold, boolean noValidated,
            String proteinKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter)((ProteinMatch)identification.retrieveObject(proteinKey)).getUrParam(psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (!psParameter.getManualValidation()) {

            if (sequenceFactory.concatenatedTargetDecoy()) {

                if (!noValidated && psParameter.getProteinProbabilityScore() <= scoreThreshold) {
                    boolean filtersPassed = true;
                    for (Filter filter : validationQCPreferences.getProteinFilters()) {
                        ProteinFilter proteinFilter = (ProteinFilter) filter;
                        boolean validation = proteinFilter.isValidated(proteinKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, null);
                        psParameter.setQcResult(filter.getName(), validation);
                        if (!validation) {
                            filtersPassed = false;
                        }
                    }
                    boolean confidenceThresholdPassed = psParameter.getProteinConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                    boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;

                    boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();

                    if (filtersPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
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
     * @param geneMaps the gene maps
     * @param identificationParameters the identification parameters
     * @param peptideKey the key of the peptide match of interest
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     * @throws org.apache.commons.math.MathException Exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public static void updatePeptideMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, PeptideSpecificMap peptideMap, String peptideKey)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter)((PeptideMatch)identification.retrieveObject(peptideKey)).getUrParam(psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {
            TargetDecoyMap targetDecoyMap = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSpecificMapKey()));
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double fdrLimit = targetDecoyResults.getFdrLimit();
            double nTargetLimit = 100.0 / fdrLimit;
            double peptideThreshold = targetDecoyResults.getScoreLimit();
            double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
            double confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;
            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }
            boolean noValidated = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSpecificMapKey())).getTargetDecoyResults().noValidated();
            if (!noValidated && psParameter.getPeptideProbabilityScore() <= peptideThreshold) {
                boolean filtersPassed = true;
                for (Filter filter : validationQCPreferences.getPeptideFilters()) {
                    PeptideFilter peptideFilter = (PeptideFilter) filter;
                    boolean validation = peptideFilter.isValidated(peptideKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, null);
                    psParameter.setQcResult(filter.getName(), validation);
                    if (!validation) {
                        filtersPassed = false;
                    }
                }
                boolean confidenceThresholdPassed = psParameter.getPeptideConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;

                boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();

                if (filtersPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
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
     * @param spectrumKey the key of the spectrum match of interest
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param applyQCFilters if true quality control filters will be used
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     * @throws org.apache.commons.math.MathException Exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public static void updateSpectrumMatchValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            PsmSpecificMap psmMap, String spectrumKey, boolean applyQCFilters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter)((SpectrumMatch)identification.retrieveObject(spectrumKey)).getUrParam(psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            Integer charge = new Integer(psParameter.getSpecificMapKey());
            String fileName = Spectrum.getSpectrumFile(spectrumKey);
            TargetDecoyMap targetDecoyMap = psmMap.getTargetDecoyMap(charge, fileName);
            double psmThreshold = 0;
            double confidenceThreshold = 100;
            boolean noValidated = true;
            double nTargetLimit = 100;

            if (targetDecoyMap != null) {
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double fdrLimit = targetDecoyResults.getFdrLimit();
                nTargetLimit = 100.0 / fdrLimit;
                psmThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;
                if (confidenceThreshold > 100) {
                    confidenceThreshold = 100;
                }

                noValidated = targetDecoyResults.noValidated();
            }

            if (!noValidated && psParameter.getPsmProbabilityScore() <= psmThreshold) {

                boolean filtersPassed = true;

                if (applyQCFilters) {

                    for (Filter filter : validationQCPreferences.getPsmFilters()) {
                        PsmFilter psmFilter = (PsmFilter) filter;
                        boolean validated = psmFilter.isValidated(spectrumKey, identification, geneMaps, identificationFeaturesGenerator, identificationParameters, peptideSpectrumAnnotator);
                        psParameter.setQcResult(psmFilter.getName(), validated);
                        if (!validated) {
                            filtersPassed = false;
                        }
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getPsmConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;

                boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();

                if (filtersPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
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
     * @param inputMap the target decoy map of all search engine scores
     * @param spectrumKey the key of the inspected spectrum
     * @param peptideAssumption the peptide assumption of interest
     * @param peptideSpectrumAnnotator a spectrum annotator, can be null
     * @param identificationParameters the identification parameters
     * @param applyQCFilters boolean indicating whether QC filters should be
     * applied
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     * @throws org.apache.commons.math.MathException Exception thrown whenever
     * an error occurred while doing statistics on a distribution
     */
    public static void updatePeptideAssumptionValidationLevel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            InputMap inputMap, String spectrumKey, PeptideAssumption peptideAssumption, boolean applyQCFilters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {

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
                double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;
                if (confidenceThreshold > 100) {
                    confidenceThreshold = 100;
                }

                noValidated = targetDecoyResults.noValidated();
            }

            if (!noValidated && peptideAssumption.getScore() <= seThreshold) {

                boolean filtersPassed = true;

                if (applyQCFilters) {

                    for (Filter filter : validationQCPreferences.getPsmFilters()) {
                        PsmFilter psmFilter = (PsmFilter) filter;
                        AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
                        boolean validated = assumptionFilter.isValidated(spectrumKey, peptideAssumption, identification, identificationFeaturesGenerator, identificationParameters, peptideSpectrumAnnotator);
                        psParameter.setQcResult(filter.getName(), validated);
                        if (!validated) {
                            filtersPassed = false;
                        }
                    }
                }

                boolean confidenceThresholdPassed = psParameter.getPsmConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;

                boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();

                if (filtersPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
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
     * Updates the validation status of a tag assumption. If the match was
     * manually validated nothing will be changed.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param identificationParameters the identification parameters
     * @param inputMap the target decoy map of all search engine scores
     * @param spectrumKey the key of the inspected spectrum
     * @param tagAssumption the tag assumption of interest
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public static void updateTagAssumptionValidationLevel(IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, InputMap inputMap, String spectrumKey, TagAssumption tagAssumption)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) tagAssumption.getUrParam(psParameter);
        psParameter.resetQcResults();
        ValidationQCPreferences validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();

        if (sequenceFactory.concatenatedTargetDecoy()) {

            TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(tagAssumption.getAdvocate());
            double seThreshold = 0;
            double confidenceThreshold = 100;
            boolean noValidated = true;
            double nTargetLimit = 100;

            if (targetDecoyMap != null) {
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double desiredThreshold = targetDecoyResults.getUserInput();
                nTargetLimit = 100.0 / desiredThreshold;
                seThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
                confidenceThreshold = targetDecoyResults.getConfidenceLimit() + margin;
                if (confidenceThreshold > 100) {
                    confidenceThreshold = 100;
                }

                noValidated = targetDecoyResults.noValidated();
            }

            if (!noValidated && tagAssumption.getScore() <= seThreshold) {

                boolean filtersPassed = true;

                //@TODO: implement QC filters for tags
//                if (applyQCFilters) {
//
//                    for (Filter filter : validationQCPreferences.getPsmFilters()) {
//                    PsmFilter psmFilter = (PsmFilter) filter;
//                    AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
//                    boolean validated = assumptionFilter.isValidated(spectrumKey, tagAssumption, identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideSpectrumAnnotator);
//                    psParameter.setQcResult(filter.getName(), validated);
//                        if (!validated) {
//                            filtersPassed = false;
//                        }
//                    }
//                }
                boolean confidenceThresholdPassed = psParameter.getPsmConfidence() >= confidenceThreshold; //@TODO: not sure whether we should include all 100% confidence hits by default?

                boolean enoughHits = !validationQCPreferences.isFirstDecoy() || targetDecoyMap.getnTargetOnly() > nTargetLimit;

                boolean enoughSequences = !validationQCPreferences.isDbSize() || sequenceFactory.hasEnoughSequences();

                if (filtersPassed && confidenceThresholdPassed && enoughHits && enoughSequences) {
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
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void fillPeptideMaps(Identification identification, Metrics metrics, WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Filling Peptide Maps. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size() * 2);

        HashSet<String> foundModifications = new HashSet<String>();
        HashMap<String, ArrayList<String>> fractionPsmMatches = new HashMap<String, ArrayList<String>>();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        int nFractions = identification.getSpectrumFiles().size();

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();

            for (String modification : Peptide.getModificationFamily(peptideKey)) {
                if (!foundModifications.contains(modification)) {
                    foundModifications.add(modification);
                }
            }

            double probaScore = 1;
            HashMap<String, Double> fractionScores = new HashMap<String, Double>(nFractions);

            // get the global and fraction level peptide scores
            for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                psParameter = (PSParameter)((SpectrumMatch)identification.retrieveObject(spectrumKey)).getUrParam(psParameter);
                probaScore = probaScore * psParameter.getPsmProbability();

                if (nFractions > 1) {
                    String fraction = Spectrum.getSpectrumFile(spectrumKey);

                    Double fractionScore = fractionScores.get(fraction);
                    boolean change = false;
                    if (fractionScore == null) {
                        fractionScore = 1.0;
                        change = true;
                    }
                    Double tempScore = psParameter.getPsmProbability();
                    if (tempScore != 1.0) {
                        fractionScore *= tempScore;
                        change = true;
                    }
                    if (change) {
                        fractionScores.put(fraction, fractionScore);
                    }

                    String fractionKey = fraction + "_" + peptideKey;
                    ArrayList<String> spectrumMatches = fractionPsmMatches.get(fractionKey);
                    if (spectrumMatches == null) {
                        spectrumMatches = new ArrayList<String>(1);
                        fractionPsmMatches.put(fractionKey, spectrumMatches);
                    }
                    spectrumMatches.add(spectrumKey);
                }
            }
            if (nFractions == 1) {
                String spectrumFile = identification.getSpectrumFiles().get(0);
                fractionScores.put(spectrumFile, probaScore);
                String fractionKey = spectrumFile + "_" + peptideKey;
                fractionPsmMatches.put(fractionKey, new ArrayList<String>(peptideMatch.getSpectrumMatchesKeys()));
            }

            psParameter = new PSParameter();

            // set the fraction scores
            for (String fractionName : fractionScores.keySet()) {
                psParameter.setFractionScore(fractionName, fractionScores.get(fractionName));
            }

            // Set the global score and grouping key
            psParameter.setPeptideProbabilityScore(probaScore);
            String peptideValidationGroup = "";
            if (identificationParameters.getIdValidationPreferences().getSeparatePeptides()) {
                psParameter.setSpecificMapKey(peptideValidationGroup);
            }
            peptideMatch.addUrParam(psParameter);
            peptideMap.addPoint(psParameter.getPeptideProbabilityScore(), peptideMatch, identificationParameters.getSequenceMatchingPreferences());

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
            metrics.setFoundModifications(new ArrayList<String>(foundModifications));
        }
    }

    /**
     * Attaches the peptide posterior error probabilities to the peptide
     * matches.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while mapping the peptides to the proteins.
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database.
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object from the database.
     */
    public void attachPeptideProbabilities(Identification identification, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Attaching Peptide Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getPeptideIdentification().size());

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            psParameter = (PSParameter)peptideMatch.getUrParam(psParameter);

            if (sequenceFactory.concatenatedTargetDecoy()) {
                psParameter.setPeptideProbability(peptideMap.getProbability(psParameter.getSpecificMapKey(), psParameter.getPeptideProbabilityScore()));
            } else {
                psParameter.setPeptideProbability(1.0);
            }
            Set<String> fractions = psParameter.getFractions();
            if (fractions == null) {
                throw new IllegalArgumentException("Fractions not found for peptide " + peptideKey + ".");
            }
            for (String fraction : fractions) {
                if (sequenceFactory.concatenatedTargetDecoy()) {
                    psParameter.setFractionPEP(fraction, peptideMap.getProbability(psParameter.getSpecificMapKey(), psParameter.getFractionScore(fraction)));
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
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file.
     */
    public void fillProteinMap(Identification identification, WaitingHandler waitingHandler) throws Exception {

        waitingHandler.setWaitingText("Filling Protein Map. Please Wait...");

        int totalProgress = identification.getProteinIdentification().size();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(totalProgress);

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        int nFractions = identification.getSpectrumFiles().size();

        while (proteinMatchesIterator.hasNext()) {

            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();

            waitingHandler.increaseSecondaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }

            HashMap<String, Double> fractionScores = new HashMap<String, Double>(nFractions);
            double probaScore = 1;

            if (proteinMatch == null) {
                throw new IllegalArgumentException("Protein match " + proteinKey + " not found.");
            }

            // get the global and fraction level scores
            for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                psParameter = (PSParameter)((PeptideMatch)identification.retrieveObject(peptideKey)).getUrParam(psParameter);
                probaScore = probaScore * psParameter.getPeptideProbability();

                if (nFractions > 1) {
                    for (String fraction : psParameter.getFractions()) {

                        Double fractionScore = fractionScores.get(fraction);
                        boolean change = false;
                        if (fractionScore == null) {
                            fractionScore = 1.0;
                            change = true;
                        }
                        Double peptideScore = psParameter.getFractionPEP(fraction);
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
                String spectrumFile = identification.getSpectrumFiles().get(0);
                fractionScores.put(spectrumFile, probaScore);
            }

            psParameter = new PSParameter();

            // set the fraction scores
            for (String fractionName : fractionScores.keySet()) {
                psParameter.setFractionScore(fractionName, fractionScores.get(fractionName));
            }

            // Set the global score
            psParameter.setProteinProbabilityScore(probaScore);
            proteinMatch.addUrParam(psParameter);
            proteinMap.addPoint(psParameter.getProteinProbabilityScore(), proteinMatch.isDecoy());
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Attaches the protein posterior error probability to the protein matches.
     *
     * @param identification the identification class containing the matches to
     * validate
     * @param metrics if provided fraction information
     * @param waitingHandler the handler displaying feedback to the user
     * @param fractionSettings the fraction settings
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the matches database
     * @throws IOException exception thrown whenever an error occurred while
     * interacting with the matches database or when an error is encountered
     * while reading the FASTA file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing a match from the database
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while retrieving the match or when an error is encountered while
     * reading the FASTA file
     */
    public void attachProteinProbabilities(Identification identification, Metrics metrics, WaitingHandler waitingHandler,
            FractionSettings fractionSettings) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        waitingHandler.setWaitingText("Attaching Protein Probabilities. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getProteinIdentification().size());

        HashMap<String, ArrayList<Double>> fractionMW = new HashMap<String, ArrayList<Double>>();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();
            Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());

            psParameter = (PSParameter)proteinMatch.getUrParam(psParameter);
            if (sequenceFactory.concatenatedTargetDecoy()) {
                double proteinProbability = proteinMap.getProbability(psParameter.getProteinProbabilityScore());
                psParameter.setProteinProbability(proteinProbability);
            } else {
                psParameter.setProteinProbability(1.0);
            }

            for (String fraction : psParameter.getFractions()) {
                if (sequenceFactory.concatenatedTargetDecoy()) {
                    psParameter.setFractionPEP(fraction, proteinMap.getProbability(psParameter.getFractionScore(fraction)));
                } else {
                    psParameter.setFractionPEP(fraction, 1.0);
                }

                // set the fraction molecular weights
                if (!proteinMatch.isDecoy() && psParameter.getFractionConfidence(fraction) > fractionSettings.getProteinConfidenceMwPlots()) {
                    ArrayList<Double> mw = fractionMW.get(fraction);
                    if (mw == null) {
                        mw = new ArrayList<Double>(1);
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
    public PsmSpecificMap getPsmMap() {
        return psmMap;
    }

    /**
     * Sets the PSM scoring specific map.
     *
     * @param psmMap the PSM scoring specific map
     */
    public void setPsmMap(PsmSpecificMap psmMap) {
        this.psmMap = psmMap;
    }

    /**
     * Returns the peptide scoring specific map.
     *
     * @return the peptide scoring specific map
     */
    public PeptideSpecificMap getPeptideMap() {
        return peptideMap;
    }

    /**
     * Sets the peptide scoring specific map.
     *
     * @param peptideMap the peptide scoring specific map
     */
    public void setPeptideMap(PeptideSpecificMap peptideMap) {
        this.peptideMap = peptideMap;
    }

    /**
     * Returns the protein scoring map.
     *
     * @return the protein scoring map
     */
    public ProteinMap getProteinMap() {
        return proteinMap;
    }

    /**
     * Sets the protein scoring map.
     *
     * @param proteinMap the protein scoring map
     */
    public void setProteinMap(ProteinMap proteinMap) {
        this.proteinMap = proteinMap;
    }

    /**
     * Sets the default matches quality control filters.
     *
     * @param validationQCPreferences the default matches quality control
     * filters
     */
    public static void setDefaultMatchesQCFilters(ValidationQCPreferences validationQCPreferences) {

        ArrayList<Filter> psmFilters = new ArrayList<Filter>(3);
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
        validationQCPreferences.setPsmFilters(psmFilters);

        ArrayList<Filter> peptideFilters = new ArrayList<Filter>(1);
        PeptideFilter peptideFilter = new PeptideFilter("One confident PSM");
        peptideFilter.setDescription("Number of confident PSMs filter");
        peptideFilter.setFilterItem(PeptideFilterItem.nConfidentPSMs.name, FilterItemComparator.higherOrEqual, 1);
        peptideFilters.add(peptideFilter);
        validationQCPreferences.setPeptideFilters(peptideFilters);

        ArrayList<Filter> proteinFilters = new ArrayList<Filter>(1);
        ProteinFilter proteinFilter = new ProteinFilter(">=2 confident peptides");
        proteinFilter.setDescription("Number of confident peptides filter");
        proteinFilter.setFilterItem(ProteinFilterItem.nConfidentPeptides.name, FilterItemComparator.higherOrEqual, 2);
        proteinFilters.add(proteinFilter);
        proteinFilter = new ProteinFilter(">=2 confident spectra");
        proteinFilter.setDescription("Number of confident spectra filter");
        proteinFilter.setFilterItem(ProteinFilterItem.nConfidentPSMs.name, FilterItemComparator.higherOrEqual, 2);
        proteinFilters.add(proteinFilter);
        validationQCPreferences.setProteinFilters(proteinFilters);
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
        private PsmIterator psmIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The gene maps.
         */
        private GeneMaps geneMaps;
        /**
         * The identification features generator used to estimate, store and
         * retrieve identification features.
         */
        private IdentificationFeaturesGenerator identificationFeaturesGenerator;
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
         * List used to store precursor m/z deviations of matches currently
         * validated.
         */
        private HashMap<String, ArrayList<Double>> threadPrecursorMzDeviations = new HashMap<String, ArrayList<Double>>(128);
        /**
         * If not null, information on search engine agreement will be stored in
         * the input map.
         */
        private InputMap inputMap;
        /**
         * If true, quality control filters will be applied to the matches.
         */
        private boolean applyQCFilters;
        /**
         * If true, advocate contributions will be stored in the input map.
         */
        private boolean storeContributions;

        /**
         * Constructor.
         *
         * @param psmIterator a PSM iterator
         * @param identification the identification containing the matches
         * @param identificationFeaturesGenerator the identification features
         * generator used to estimate, store and retrieve identification
         * features
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
        public PsmValidatorRunnable(PsmIterator psmIterator, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, InputMap inputMap, boolean applyQCFilters, boolean storeContributions) {
            this.psmIterator = psmIterator;
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
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
                while (psmIterator.hasNext() && !waitingHandler.isRunCanceled()) {
                    SpectrumMatch spectrumMatch = psmIterator.next();
                    if (spectrumMatch != null) {

                        String spectrumKey = spectrumMatch.getKey();
                        String spectrumFile = spectrumMatch.getSpectrumFile();

                        updateSpectrumMatchValidationLevel(identification, identificationFeaturesGenerator, geneMaps, identificationParameters, peptideSpectrumAnnotator, psmMap, spectrumKey, applyQCFilters);

                        // update assumption validation level
                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = spectrumMatch.getAssumptionsMap();
                        for (HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmMap : assumptions.values()) {
                            for (ArrayList<SpectrumIdentificationAssumption> scoreList : algorithmMap.values()) {
                                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : scoreList) {
                                    if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                                        updatePeptideAssumptionValidationLevel(identification, identificationFeaturesGenerator, identificationParameters, peptideSpectrumAnnotator, inputMap, spectrumKey, (PeptideAssumption) spectrumIdentificationAssumption, applyQCFilters);
                                    } else if (spectrumIdentificationAssumption instanceof TagAssumption) {
                                        updateTagAssumptionValidationLevel(identificationFeaturesGenerator, identificationParameters, inputMap, spectrumKey, (TagAssumption) spectrumIdentificationAssumption);
                                    } else {
                                        throw new UnsupportedOperationException("Validation not implemented for assumption of class " + spectrumIdentificationAssumption.getClass() + ".");
                                    }
                                }
                            }
                        }

                        // update search engine agreement
                        PSParameter psParameter = new PSParameter();
                        psParameter = (PSParameter)spectrumMatch.getUrParam(psParameter);

                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                        if (peptideAssumption != null) {

                            if (psParameter.getMatchValidationLevel().isValidated() && !peptideAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {
                                double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
                                SearchParameters searchParameters = identificationParameters.getSearchParameters();
                                double precursorMzError = peptideAssumption.getDeltaMass(precursorMz, searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                                if(!threadPrecursorMzDeviations.containsKey(spectrumFile)) threadPrecursorMzDeviations.put(spectrumFile, new ArrayList<Double>());
                                threadPrecursorMzDeviations.get(spectrumFile).add(precursorMzError);

                                if (inputMap != null && storeContributions) {

                                    Peptide bestPeptide = peptideAssumption.getPeptide();
                                    HashSet<Integer> agreementAdvocates = new HashSet<Integer>();

                                    for (int advocateId : assumptions.keySet()) {
                                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateId);
                                        if (advocateAssumptions != null) {
                                            ArrayList<Double> eValues = new ArrayList<Double>(advocateAssumptions.keySet());
                                            Collections.sort(eValues);
                                            for (SpectrumIdentificationAssumption firstHit : advocateAssumptions.get(eValues.get(0))) {
                                                if (firstHit instanceof PeptideAssumption) {
                                                    Peptide advocatePeptide = ((PeptideAssumption) firstHit).getPeptide();
                                                    if (bestPeptide.isSameSequenceAndModificationStatus(advocatePeptide, identificationParameters.getSequenceMatchingPreferences())) {
                                                        agreementAdvocates.add(advocateId);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    boolean unique = agreementAdvocates.size() == 1;

                                    String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);

                                    for (int advocateId : agreementAdvocates) {
                                        inputMap.addAdvocateContribution(advocateId, spectrumFileName, unique);
                                    }

                                    inputMap.addPeptideShakerHit(spectrumFileName, agreementAdvocates.isEmpty());
                                }
                            }
                        }
                    }
                    if (waitingHandler != null) {
                        waitingHandler.increaseSecondaryProgressCounter();
                    }
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
        private PeptideMatchesIterator peptideMatchesIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The gene maps.
         */
        private GeneMaps geneMaps;
        /**
         * The identification features generator used to estimate, store and
         * retrieve identification features.
         */
        private IdentificationFeaturesGenerator identificationFeaturesGenerator;
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
         * List used to store the length of the validated peptides.
         */
        private ArrayList<Double> validatedPeptideLengths = new ArrayList<Double>();
        /**
         * Map used to store the number of validated peptides per fraction.
         */
        private HashMap<String, Integer> validatedTotalPeptidesPerFraction = new HashMap<String, Integer>();
        /**
         * The object used to store metrics on the project.
         */
        private Metrics metrics;

        /**
         * Constructor.
         *
         * @param psmIterator a peptide matches iterator
         * @param identification the identification containing the matches
         * @param identificationFeaturesGenerator the identification features
         * generator used to estimate, store and retrieve identification
         * features
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
        public PeptideValidatorRunnable(PeptideMatchesIterator peptideMatchesIterator, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, Metrics metrics) {
            this.peptideMatchesIterator = peptideMatchesIterator;
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
            this.geneMaps = geneMaps;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.metrics = metrics;
        }

        @Override
        public void run() {
            try {
                while (peptideMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {

                    PeptideMatch peptideMatch = peptideMatchesIterator.next();

                    if (peptideMatch != null) {

                        String peptideKey = peptideMatch.getKey();

                        updatePeptideMatchValidationLevel(identification, identificationFeaturesGenerator, geneMaps, identificationParameters, peptideMap, peptideKey);

                        // set the fraction details
                        PSParameter psParameter = new PSParameter();
                        psParameter = (PSParameter)((PeptideMatch)identification.retrieveObject(peptideKey)).getUrParam(psParameter);

                        if (psParameter.getMatchValidationLevel().isValidated()) {
                            double length = Peptide.getSequence(peptideKey).length();
                            validatedPeptideLengths.add(length);
                        }

                        // @TODO: could be a better more elegant way of doing this?
                        HashMap<String, Integer> validatedPsmsPerFraction = new HashMap<String, Integer>(psParameter.getFractionScore().size());
                        HashMap<String, ArrayList<Double>> precursorIntensitesPerFractionPeptideLevel = new HashMap<String, ArrayList<Double>>(psParameter.getFractionScore().size());

                        for (String fractionName : psParameter.getFractions()) {

                            ArrayList<Double> precursorIntensities = new ArrayList<Double>();

                            String peptideFractionKey = fractionName + "_" + peptideKey;
                            if (metrics.getFractionPsmMatches().get(peptideFractionKey) != null) {
                                ArrayList<String> spectrumKeys = metrics.getFractionPsmMatches().get(peptideFractionKey);

                                for (String spectrumKey : spectrumKeys) {

                                    PSParameter psParameter2 = (PSParameter)((SpectrumMatch)identification.retrieveObject(spectrumKey)).getUrParam(psParameter);
                                    if (psParameter2.getMatchValidationLevel().isValidated()) {
                                        if (validatedPsmsPerFraction.containsKey(fractionName)) {
                                            Integer value = validatedPsmsPerFraction.get(fractionName);
                                            validatedPsmsPerFraction.put(fractionName, value + 1);
                                        } else {
                                            validatedPsmsPerFraction.put(fractionName, 1);
                                        }
                                        if (SpectrumFactory.getInstance().getPrecursor(spectrumKey).getIntensity() > 0) {
                                            // @TODO: replace by an mgf index map? (have to add intensity map to the index first...)
                                            precursorIntensities.add(SpectrumFactory.getInstance().getPrecursor(spectrumKey).getIntensity());
                                        }
                                    }
                                    if (waitingHandler != null) {
                                        if (waitingHandler.isRunCanceled()) {
                                            return;
                                        }
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

                        if (waitingHandler != null) {
                            waitingHandler.increaseSecondaryProgressCounter();
                        }

                    }
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
                Integer value = validatedTotalPeptidesPerFraction.get(fractionName);
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
        private ProteinMatchesIterator proteinMatchesIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The gene maps.
         */
        private GeneMaps geneMaps;
        /**
         * The identification features generator used. to estimate, store and
         * retrieve identification features
         */
        private IdentificationFeaturesGenerator identificationFeaturesGenerator;
        /**
         * The identification parameters.
         */
        private IdentificationParameters identificationParameters;
        /**
         * The spectrum counting preferences.
         */
        private SpectrumCountingPreferences spectrumCountingPreferences;
        /**
         * The waiting handler.
         */
        private WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private ExceptionHandler exceptionHandler;
        /**
         * The validation QC preferences.
         */
        private ValidationQCPreferences validationQCPreferences;
        /**
         * The total spectrum counting mass contribution of the proteins
         * according to the validation level specified in the preferences.
         */
        private double totalSpectrumCountingMass = 0;
        /**
         * The total spectrum counting contribution of the proteins according to
         * the validation level specified in the preferences.
         */
        private double totalSpectrumCounting = 0;
        /**
         * The object used to store metrics on the project.
         */
        private Metrics metrics;

        /**
         * Constructor.
         *
         * @param proteinMatchesIterator a protein matches iterator
         * @param identification the identification containing the matches
         * @param identificationFeaturesGenerator the identification features
         * generator used to estimate, store and retrieve identification
         * features
         * @param geneMaps the gene maps
         * @param metrics the object used to store metrics on the project
         * @param identificationParameters the identification parameters
         * @param spectrumCountingPreferences the spectrum counting preferences
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public ProteinValidatorRunnable(ProteinMatchesIterator proteinMatchesIterator, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps, Metrics metrics,
                IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.proteinMatchesIterator = proteinMatchesIterator;
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
            this.geneMaps = geneMaps;
            this.metrics = metrics;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.validationQCPreferences = identificationParameters.getIdValidationPreferences().getValidationQCPreferences();
            this.spectrumCountingPreferences = spectrumCountingPreferences;
        }

        @Override
        public void run() {
            try {

                TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double desiredThreshold = targetDecoyResults.getUserInput();
                double nTargetLimit = 100.0 / desiredThreshold;
                double proteinThreshold = targetDecoyResults.getScoreLimit();
                double margin = validationQCPreferences.getConfidenceMargin() * targetDecoyMap.getResolution();
                double proteinConfidentThreshold = targetDecoyResults.getConfidenceLimit() + margin;
                if (proteinConfidentThreshold > 100) {
                    proteinConfidentThreshold = 100;
                }
                boolean noValidated = proteinMap.getTargetDecoyMap().getTargetDecoyResults().noValidated();
                int maxValidatedSpectraFractionLevel = 0;
                int maxValidatedPeptidesFractionLevel = 0;
                double maxProteinAveragePrecursorIntensity = 0.0, maxProteinSummedPrecursorIntensity = 0.0;

                while (proteinMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {
                    ProteinMatch proteinMatch = proteinMatchesIterator.next();
                    if (proteinMatch != null) {

                        String proteinKey = proteinMatch.getKey();
                        updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, geneMaps, identificationParameters,
                                targetDecoyMap, proteinThreshold, nTargetLimit, proteinConfidentThreshold, noValidated, proteinKey);

                        // set the fraction details
                        PSParameter psParameter = new PSParameter();
                        psParameter = (PSParameter)proteinMatch.getUrParam(psParameter);
                        
                        if (psParameter == null) {
                            System.out.println("Null parameter: " + proteinKey);
                        } else if (psParameter.getMatchValidationLevel() == null) {
                            System.out.println("Null validation level: " + proteinKey);
                        }

                        if (!proteinMatch.isDecoy() && psParameter.getMatchValidationLevel().getIndex() >= spectrumCountingPreferences.getMatchValidationLevel()) {
                            double tempSpectrumCounting = identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                            increaseSpectrumCounting(tempSpectrumCounting);
                            double molecularWeight = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                            double massContribution = molecularWeight * tempSpectrumCounting;
                            increaseSpectrumCountingMass(massContribution);
                        }
                        // Load the coverage in cache
                        if (!proteinMatch.isDecoy() && psParameter.getMatchValidationLevel().isValidated()) {
                            identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                        }

                        // @TODO: could be a better more elegant way of doing this?
                        HashMap<String, Integer> validatedPsmsPerFraction = new HashMap<String, Integer>();
                        HashMap<String, Integer> validatedPeptidesPerFraction = new HashMap<String, Integer>();
                        HashMap<String, ArrayList<Double>> precursorIntensitesPerFractionProteinLevel = new HashMap<String, ArrayList<Double>>();
                        ArrayList<String> peptideKeys = proteinMatch.getPeptideMatchesKeys();

                        for (String currentPeptideKey : peptideKeys) {

                            PSParameter psParameter2 = (PSParameter)((PeptideMatch)identification.retrieveObject(currentPeptideKey)).getUrParam(psParameter);

                            for (String fraction : psParameter2.getFractions()) {

                                if (psParameter2.getFractionValidatedSpectra(fraction) != null) {
                                    if (validatedPsmsPerFraction.containsKey(fraction)) {
                                        Integer value = validatedPsmsPerFraction.get(fraction);
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
                                        precursorIntensitesPerFractionProteinLevel.put(fraction, new ArrayList<Double>(peptideIntensities));
                                    }
                                }

                                if (psParameter2.getMatchValidationLevel().isValidated()) {
                                    if (validatedPeptidesPerFraction.containsKey(fraction)) {
                                        Integer value = validatedPeptidesPerFraction.get(fraction);
                                        validatedPeptidesPerFraction.put(fraction, value + 1);
                                    } else {
                                        validatedPeptidesPerFraction.put(fraction, 1);
                                    }

                                    if (validatedPeptidesPerFraction.get(fraction) > maxValidatedPeptidesFractionLevel) {
                                        maxValidatedPeptidesFractionLevel = validatedPeptidesPerFraction.get(fraction);
                                    }
                                }
                            }

                            if (waitingHandler != null) {
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
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

                        if (waitingHandler != null) {
                            waitingHandler.increaseSecondaryProgressCounter();
                        }
                    }
                }

                // set the max values in the metrics
                setMaxValues(maxValidatedPeptidesFractionLevel, maxValidatedSpectraFractionLevel, maxProteinAveragePrecursorIntensity, maxProteinSummedPrecursorIntensity);

            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

        /**
         * Increases the mass contribution of a protein.
         *
         * @param massContribution the mass contribution of a protein
         */
        private synchronized void increaseSpectrumCountingMass(Double massContribution) {
            totalSpectrumCountingMass += massContribution;
        }

        /**
         * Increases the spectrum counting contribution of a protein.
         *
         * @param spectrumCounting the spectrum counting contribution of a
         * protein
         */
        private synchronized void increaseSpectrumCounting(Double spectrumCounting) {
            totalSpectrumCounting += spectrumCounting;
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
         * Returns the spectrum counting mass contribution of the validated
         * proteins.
         *
         * @return the spectrum counting mass contribution of the validated
         * proteins
         */
        public double getTotalSpectrumCountingMass() {
            return totalSpectrumCountingMass;
        }

        /**
         * Returns the spectrum counting contribution of the proteins iterated
         * by this runnable.
         *
         * @return the spectrum counting contribution of the proteins iterated
         * by this runnable
         */
        public double getTotalSpectrumCounting() {
            return totalSpectrumCounting;
        }
    }
}
