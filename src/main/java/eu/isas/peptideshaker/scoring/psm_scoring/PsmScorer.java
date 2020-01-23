package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.psm_scoring.PsmScore;
import com.compomics.util.experiment.identification.psm_scoring.PsmScoresEstimator;
import com.compomics.util.experiment.identification.psm_scoring.psm_scores.HyperScore;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.PsmScoringParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.math.HistogramUtils;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.math.util.FastMath;

/**
 * This class scores peptide spectrum matches.
 *
 * @author Marc Vaudel
 */
public class PsmScorer {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * A sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The fasta parameters.
     */
    private final FastaParameters fastaParameters;

    /**
     * The psm scores estimator to use when socring the psms.
     */
    private final PsmScoresEstimator psmScoresEstimator = new PsmScoresEstimator();

    /**
     * Constructor.
     *
     * @param sequenceProvider the sequence provider
     * @param fastaParameters the fasta parsing parameters
     */
    public PsmScorer(SequenceProvider sequenceProvider, FastaParameters fastaParameters) {

        this.sequenceProvider = sequenceProvider;
        this.fastaParameters = fastaParameters;

    }

    /**
     * Scores the PSMs contained in an identification object.
     *
     * @param identification the object containing the identification matches
     * @param inputMap the input map scores
     * @param processingPreferences the processing preferences
     * @param identificationParameters identification parameters used
     * @param sequenceProvider a sequence provider
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler a handler for exceptions
     *
     * @throws java.lang.InterruptedException exception thrown if a thread is
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if the
     * process times out
     */
    public void estimateIntermediateScores(
            Identification identification,
            InputMap inputMap,
            ProcessingParameters processingPreferences,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) throws InterruptedException, TimeoutException {

        // Remove the intensity filter during scoring
        AnnotationParameters annotationSettings = identificationParameters.getAnnotationParameters();
        double intensityThreshold = annotationSettings.getAnnotationIntensityLimit();
        annotationSettings.setIntensityLimit(0);

        waitingHandler.setWaitingText("Scoring PSMs. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ExecutorService pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());
        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(null);
        ArrayList<PsmScorerRunnable> psmScorerRunnables = new ArrayList<>(processingPreferences.getnThreads());

        for (int i = 1; i <= processingPreferences.getnThreads() && !waitingHandler.isRunCanceled(); i++) {

            PsmScorerRunnable runnable = new PsmScorerRunnable(psmIterator, identification, inputMap, identificationParameters, waitingHandler, exceptionHandler);
            psmScorerRunnables.add(runnable);
            pool.submit(runnable);

        }

        if (waitingHandler.isRunCanceled()) {

            pool.shutdownNow();
            return;

        }

        pool.shutdown();

        if (!pool.awaitTermination(identification.getSpectrumIdentificationSize(), TimeUnit.MINUTES)) {

            throw new TimeoutException("PSM scoring timed out. Please contact the developers.");

        }

        ArrayList<HashMap<Double, Integer>> aHistograms = new ArrayList<>(processingPreferences.getnThreads());
        ArrayList<HashMap<Double, Integer>> bHistograms = new ArrayList<>(processingPreferences.getnThreads());
        HashMap<Long, ArrayList<Integer>> missingValuesMap = new HashMap<>();

        for (PsmScorerRunnable runnable : psmScorerRunnables) {

            HashMap<Long, ArrayList<Integer>> currentMissingValuesMap = runnable.getMissingEValues();
            missingValuesMap.putAll(currentMissingValuesMap);
            HyperScore hyperScore = runnable.getHyperScore();
            aHistograms.add(hyperScore.getAs());
            bHistograms.add(hyperScore.getBs());

        }

        if (!missingValuesMap.isEmpty()) {

            HashMap<Double, Integer> aHistogram = HistogramUtils.mergeHistograms(aHistograms);
            HashMap<Double, Integer> bHistogram = HistogramUtils.mergeHistograms(bHistograms);

            double defaultA = aHistogram.isEmpty() ? Double.NaN : HistogramUtils.getMedianValue(aHistogram);
            double defaultB = bHistogram.isEmpty() ? Double.NaN : HistogramUtils.getMedianValue(bHistogram);

            long[] spectrumKeys = missingValuesMap.keySet().stream().mapToLong(Long::longValue).toArray();
            psmIterator = identification.getSpectrumMatchesIterator(spectrumKeys, null);
            pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());

            for (int i = 1; i <= processingPreferences.getnThreads() && !waitingHandler.isRunCanceled(); i++) {

                MissingEValueEstimatorRunnable runnable = new MissingEValueEstimatorRunnable(
                        missingValuesMap,
                        defaultA,
                        defaultB,
                        psmIterator,
                        inputMap,
                        identificationParameters,
                        waitingHandler,
                        exceptionHandler);
                pool.submit(runnable);

            }

            if (waitingHandler.isRunCanceled()) {

                pool.shutdownNow();
                return;

            }

            pool.shutdown();

            if (!pool.awaitTermination(identification.getSpectrumIdentificationSize(), TimeUnit.MINUTES)) {

                throw new TimeoutException("PSM scoring timed out. Please contact the developers.");

            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // Restaure intensity threshold
        annotationSettings.setIntensityLimit(intensityThreshold);

    }

    /**
     * Scores the PSMs contained in an identification object.
     *
     * @param identification the object containing the identification matches
     * @param spectrumMatch a spectrum match containing the peptides and
     * spectrum to score
     * @param inputMap the input map scores
     * @param identificationParameters identification parameters used
     * @param peptideSpectrumAnnotator the spectrum annotator to use
     * @param hyperScore the object to use to compute the hyperscore
     * @param waitingHandler the handler displaying feedback to the user
     *
     * @return a list of advocates where no e-values could be found
     */
    public ArrayList<Integer> estimateIntermediateScores(
            Identification identification,
            SpectrumMatch spectrumMatch,
            InputMap inputMap,
            IdentificationParameters identificationParameters,
            PeptideSpectrumAnnotator peptideSpectrumAnnotator,
            HyperScore hyperScore,
            WaitingHandler waitingHandler
    ) {

        AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();

        PsmScoringParameters psmScoringPreferences = identificationParameters.getPsmScoringParameters();

        String spectrumKey = spectrumMatch.getSpectrumKey();
        String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);

        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = spectrumMatch.getPeptideAssumptionsMap();

        ArrayList<Integer> missingEvalue = new ArrayList<>(0);

        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry1 : assumptions.entrySet()) {

            int advocateIndex = entry1.getKey();

            if (psmScoringPreferences.isScoringNeeded(advocateIndex)) {

                HashSet<Integer> scoresForAdvocate = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                TreeMap<Double, ArrayList<PeptideAssumption>> algorthmAssumptions = entry1.getValue();

                // the hyperscore requires a second pass for the e-value estimation
                ArrayList<Double> hyperScores = null;
                ArrayList<PSParameter> hyperScoreParameters = null;
                ArrayList<Boolean> hyperScoreDecoys = null;

                if (scoresForAdvocate.contains(PsmScore.hyperScore.index)) {

                    hyperScores = new ArrayList<>(algorthmAssumptions.size());
                    hyperScoreParameters = new ArrayList<>(algorthmAssumptions.size());
                    hyperScoreDecoys = new ArrayList<>(algorthmAssumptions.size());

                }

                for (Entry<Double, ArrayList<PeptideAssumption>> entry2 : algorthmAssumptions.entrySet()) {

                    for (PeptideAssumption peptideAssumption : entry2.getValue()) {

                        PSParameter assumptionParameter = new PSParameter();

                        Peptide peptide = peptideAssumption.getPeptide();
                        boolean decoy = PeptideUtils.isDecoy(peptide, sequenceProvider);

                        Spectrum spectrum = spectrumFactory.getSpectrum(spectrumKey);

                        for (Integer scoreIndex : scoresForAdvocate) {

                            double score;

                            if (scoreIndex.equals(PsmScore.native_score.index)) {

                                score = peptideAssumption.getScore();

                            } else {

                                ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                                SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                                SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrum.getSpectrumKey(),
                                        peptideAssumption, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters, peptideSpectrumAnnotator);
                                score = psmScoresEstimator.getDecreasingScore(peptide, peptideAssumption.getIdentificationCharge(), spectrum, identificationParameters,
                                        specificAnnotationPreferences, modificationParameters, sequenceProvider, modificationSequenceMatchingParameters, peptideSpectrumAnnotator, scoreIndex);

                            }

                            assumptionParameter.setIntermediateScore(scoreIndex, score);

                            if (scoreIndex.equals(PsmScore.hyperScore.index)) {

                                hyperScores.add(-score);
                                hyperScoreParameters.add(assumptionParameter);
                                hyperScoreDecoys.add(decoy);

                            } else {

                                inputMap.setIntermediateScore(spectrumFileName, advocateIndex, scoreIndex, score, decoy, psmScoringPreferences);

                            }

                        }

                        peptideAssumption.addUrParam(assumptionParameter);

                    }
                }

                if (scoresForAdvocate.contains(PsmScore.hyperScore.index)) {

                    HashMap<Double, Double> eValuesMap = hyperScore.getEValueMap(hyperScores);

                    if (eValuesMap != null) {

                        for (int i = 0; i < hyperScores.size(); i++) {

                            double score = hyperScores.get(i);
                            PSParameter psParameter = hyperScoreParameters.get(i);
                            boolean decoy = hyperScoreDecoys.get(i);
                            double eValue = eValuesMap.get(score);
                            psParameter.setIntermediateScore(PsmScore.hyperScore.index, eValue);
                            inputMap.setIntermediateScore(spectrumFileName, advocateIndex, PsmScore.hyperScore.index, score, decoy, psmScoringPreferences);

                        }

                    } else {

                        missingEvalue.add(advocateIndex);

                    }
                }
            }
        }

        return missingEvalue;

    }

    /**
     * Estimates the probabilities associated to the intermediate PSM scores.
     *
     * @param identification the object containing the identification matches
     * @param inputMap the input map scores
     * @param processingPreferences the processing preferences
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void estimateIntermediateScoreProbabilities(
            Identification identification,
            InputMap inputMap,
            ProcessingParameters processingPreferences,
            WaitingHandler waitingHandler
    ) {

        int totalProgress = 0;

        for (String spectrumFileName : identification.getSpectrumIdentification().keySet()) {

            for (int advocateIndex : inputMap.getIntermediateScoreInputAlgorithms(spectrumFileName)) {

                ArrayList<Integer> scores = new ArrayList<>(); //@TODO: implement scores

                if (scores.size() > 1) {

                    for (int scoreIndex : scores) {

                        TargetDecoyMap targetDecoyMap = inputMap.getIntermediateScoreMap(spectrumFileName, advocateIndex, scoreIndex);
                        totalProgress += targetDecoyMap.getMapSize();

                    }
                }
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setMaxSecondaryProgressCounter(totalProgress);

        for (String spectrumFileName : identification.getSpectrumIdentification().keySet()) {

            for (int advocateIndex : inputMap.getIntermediateScoreInputAlgorithms(spectrumFileName)) {

                ArrayList<Integer> scores = new ArrayList<>(); //@TODO: implement scores

                if (scores.size() > 1) {

                    for (int scoreIndex : scores) {

                        TargetDecoyMap targetDecoyMap = inputMap.getIntermediateScoreMap(spectrumFileName, advocateIndex, scoreIndex);
                        targetDecoyMap.estimateProbabilities(waitingHandler);

                        if (waitingHandler.isRunCanceled()) {

                            return;

                        }
                    }
                }
            }
        }
    }

    /**
     * Attaches a score to the PSMs.
     *
     * @param identification the object containing the identification matches
     * @param inputMap the input map scores
     * @param processingPreferences the processing preferences
     * @param identificationParameters the identification parameters
     * @param waitingHandler the handler displaying feedback to the user
     */
    public void scorePsms(
            Identification identification,
            InputMap inputMap,
            ProcessingParameters processingPreferences,
            IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler
    ) {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        PsmScoringParameters psmScoringPreferences = identificationParameters.getPsmScoringParameters();

        PSParameter psParameter = new PSParameter();

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);
        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = psmIterator.next()) != null) {

            String spectrumKey = spectrumMatch.getSpectrumKey();
            String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);

            HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = spectrumMatch.getPeptideAssumptionsMap();

            for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry1 : assumptions.entrySet()) {

                int advocateIndex = entry1.getKey();

                if (psmScoringPreferences.isScoringNeeded(advocateIndex)) {

                    HashSet<Integer> scoresForAdvocate = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                    if (!scoresForAdvocate.isEmpty()) {

                        TreeMap<Double, ArrayList<PeptideAssumption>> advocateAssumptions = entry1.getValue();

                        for (double eValue : advocateAssumptions.keySet()) {

                            for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(eValue)) {

                                if (assumption instanceof PeptideAssumption) {

                                    psParameter = (PSParameter) assumption.getUrParam(psParameter);

                                    double score = 1.0;

                                    if (scoresForAdvocate.size() == 1 || !fastaParameters.isTargetDecoy()) {

                                        score = psParameter.getIntermediateScore(scoresForAdvocate.iterator().next());

                                    } else {

                                        for (int scoreIndex : scoresForAdvocate) {

                                            TargetDecoyMap targetDecoyMap = inputMap.getIntermediateScoreMap(spectrumFileName, advocateIndex, scoreIndex);
                                            Double intermediateScore = psParameter.getIntermediateScore(scoreIndex);

                                            if (intermediateScore != null) {

                                                double p = targetDecoyMap.getProbability(intermediateScore);
                                                score *= (1.0 - p);

                                            }
                                        }

                                        score = 1 - score;

                                    }

                                    assumption.setScore(score);

                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    Peptide peptide = peptideAssumption.getPeptide();
                                    boolean decoy = PeptideUtils.isDecoy(peptide, sequenceProvider);
                                    inputMap.addEntry(advocateIndex, spectrumFileName, assumption.getScore(), decoy);

                                }
                            }
                        }
                    }
                }
            }

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            waitingHandler.increaseSecondaryProgressCounter();

        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Runnable scoring PSMs.
     *
     * @author Marc Vaudel
     */
    private class PsmScorerRunnable implements Runnable {

        /**
         * An iterator for the PSMs.
         */
        private final SpectrumMatchesIterator psmIterator;
        /**
         * The identification.
         */
        private final Identification identification;
        /**
         * The input map.
         */
        private final InputMap inputMap;
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
         * The peptide spectrum annotator.
         */
        private final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
        /**
         * The object used to estimate the hyper score.
         */
        private final HyperScore hyperScore = new HyperScore();
        /**
         * Map of the advocates missing a hyperscore e-value for every spectrum.
         */
        private final HashMap<Long, ArrayList<Integer>> missingEValues = new HashMap<>();

        /**
         * Constructor.
         *
         * @param psmIterator an iterator of the PSMs to iterate
         * @param identification the identification containing all matches
         * @param inputMap the input map used to store the scores
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public PsmScorerRunnable(
                SpectrumMatchesIterator psmIterator,
                Identification identification,
                InputMap inputMap,
                IdentificationParameters identificationParameters,
                WaitingHandler waitingHandler,
                ExceptionHandler exceptionHandler
        ) {

            this.psmIterator = psmIterator;
            this.identification = identification;
            this.inputMap = inputMap;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;

        }

        @Override
        public void run() {
            try {
                boolean increaseProgress = true;
                SpectrumMatch spectrumMatch;

                while ((spectrumMatch = psmIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    ArrayList<Integer> advocatesMissingEValues = estimateIntermediateScores(identification, spectrumMatch, inputMap, identificationParameters, peptideSpectrumAnnotator, hyperScore, waitingHandler);

                    if (!advocatesMissingEValues.isEmpty()) {

                        missingEValues.put(spectrumMatch.getKey(), advocatesMissingEValues);
                        increaseProgress = !increaseProgress;

                    } else {

                        increaseProgress = true;

                    }

                    if (increaseProgress && waitingHandler != null && !waitingHandler.isRunCanceled()) {

                        waitingHandler.increaseSecondaryProgressCounter();

                    }
                }

            } catch (Exception e) {

                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();

            }
        }

        /**
         * Returns the hyperscore used to score on this thread.
         *
         * @return the hyperscore used to score on this thread
         */
        public HyperScore getHyperScore() {

            return hyperScore;

        }

        /**
         * Returns the missing values map on this thread.
         *
         * @return the missing values map on this thread
         */
        public HashMap<Long, ArrayList<Integer>> getMissingEValues() {

            return missingEValues;

        }

    }

    /**
     * Runnable estimating the missing e-values.
     *
     * @author Marc Vaudel
     */
    private class MissingEValueEstimatorRunnable implements Runnable {

        /**
         * An iterator for the PSMs.
         */
        private final SpectrumMatchesIterator psmIterator;
        /**
         * The input map
         */
        private final InputMap inputMap;
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
         * Map of the advocates missing a hyperscore e-value for every spectrum.
         */
        private final HashMap<Long, ArrayList<Integer>> missingEValues;
        /**
         * Default values for the a coefficient.
         */
        private final double defaultA;
        /**
         * Default values for the b coefficient.
         */
        private final double defaultB;

        /**
         * Constructor.
         *
         * @param missingEValuesMap a map for the missing values
         * @param defaultA default values for the a coefficient
         * @param defaultB default values for the b coefficient
         * @param psmIterator An iterator of the PSMs to iterate
         * @param identification the identification containing all matches
         * @param inputMap the input map used to store the scores
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public MissingEValueEstimatorRunnable(
                HashMap<Long, ArrayList<Integer>> missingEValuesMap,
                double defaultA,
                double defaultB,
                SpectrumMatchesIterator psmIterator,
                InputMap inputMap,
                IdentificationParameters identificationParameters,
                WaitingHandler waitingHandler,
                ExceptionHandler exceptionHandler
        ) {

            this.missingEValues = missingEValuesMap;
            this.defaultA = defaultA;
            this.defaultB = defaultB;
            this.psmIterator = psmIterator;
            this.inputMap = inputMap;
            this.identificationParameters = identificationParameters;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;

        }

        @Override
        public void run() {

            try {

                boolean increaseProgress = true;
                PsmScoringParameters psmScoringPreferences = identificationParameters.getPsmScoringParameters();
                SpectrumMatch spectrumMatch;

                while ((spectrumMatch = psmIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    ArrayList<Integer> advocates = missingEValues.get(spectrumMatch.getKey());

                    if (advocates != null) {

                        String spectrumKey = spectrumMatch.getSpectrumKey();
                        String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);
                        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = spectrumMatch.getPeptideAssumptionsMap();

                        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : assumptions.entrySet()) {

                            int advocateIndex = entry.getKey();

                            if (psmScoringPreferences.isScoringNeeded(advocateIndex)) {

                                TreeMap<Double, ArrayList<PeptideAssumption>> originalAssumptions = entry.getValue();

                                long nMatches = originalAssumptions.values().stream()
                                        .flatMap(ArrayList::stream)
                                        .count();

                                for (Entry<Double, ArrayList<PeptideAssumption>> entry2 : originalAssumptions.entrySet()) {

                                    for (PeptideAssumption peptideAssumption : entry2.getValue()) {

                                        Peptide peptide = peptideAssumption.getPeptide();
                                        boolean decoy = PeptideUtils.isDecoy(peptide, sequenceProvider);
                                        PSParameter psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
                                        double hyperScore = -psParameter.getIntermediateScore(PsmScore.hyperScore.index);

                                        if (!Double.isNaN(defaultA) && !Double.isNaN(defaultB)) {

                                            double eValue;

                                            if (hyperScore > 0) {

                                                hyperScore = FastMath.log10(hyperScore);
                                                eValue = HyperScore.getInterpolation(hyperScore, defaultA, defaultB);

                                            } else {

                                                eValue = nMatches;

                                            }
                                            psParameter.setIntermediateScore(PsmScore.hyperScore.index, eValue);
                                            inputMap.setIntermediateScore(spectrumFileName, advocateIndex, PsmScore.hyperScore.index, eValue, decoy, psmScoringPreferences);

                                        } else {

                                            inputMap.setIntermediateScore(spectrumFileName, advocateIndex, PsmScore.hyperScore.index, -hyperScore, decoy, psmScoringPreferences);

                                        }
                                    }
                                }
                            }
                        }

                        increaseProgress = !increaseProgress;

                        if (increaseProgress && waitingHandler != null && !waitingHandler.isRunCanceled()) {

                            waitingHandler.increaseSecondaryProgressCounter();

                        }
                    }
                }
            } catch (Exception e) {

                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();

            }
        }
    }
}
