package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.psm_scoring.PsmScore;
import com.compomics.util.experiment.identification.psm_scoring.PsmScoresEstimator;
import com.compomics.util.experiment.identification.psm_scoring.psm_scores.HyperScore;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PsmScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.math.HistogramUtils;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math.util.FastMath;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class scores peptide spectrum matches.
 *
 * @author Marc Vaudel
 */
public class PsmScorer {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The protein sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The psm scores estimator to use when socring the psms.
     */
    private PsmScoresEstimator psmScoresEstimator = new PsmScoresEstimator();

    /**
     * Scores the PSMs contained in an identification object.
     *
     * @param identification the object containing the identification matches
     * @param inputMap the input map scores
     * @param processingPreferences the processing preferences
     * @param identificationParameters identification parameters used
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler a handler for exceptions
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public void estimateIntermediateScores(Identification identification, InputMap inputMap, ProcessingPreferences processingPreferences,
            IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler)
            throws SQLException, IOException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        // Remove the intensity filter during scoring
        AnnotationSettings annotationSettings = identificationParameters.getAnnotationPreferences();
        double intensityThreshold = annotationSettings.getAnnotationIntensityLimit();
        annotationSettings.setIntensityLimit(0);
        
        waitingHandler.setWaitingText("Scoring PSMs. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        ExecutorService pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());
        PsmIterator psmIterator = identification.getPsmIterator(null);
        ArrayList<PsmScorerRunnable> psmScorerRunnables = new ArrayList<PsmScorerRunnable>(processingPreferences.getnThreads());
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
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM scoring timed out. Please contact the developers.");
        }

        ArrayList<HashMap<Double, Integer>> aHistograms = new ArrayList<HashMap<Double, Integer>>(processingPreferences.getnThreads());
        ArrayList<HashMap<Double, Integer>> bHistograms = new ArrayList<HashMap<Double, Integer>>(processingPreferences.getnThreads());
        HashMap<String, ArrayList<Integer>> missingValuesMap = new HashMap<String, ArrayList<Integer>>();
        for (PsmScorerRunnable runnable : psmScorerRunnables) {
            HashMap<String, ArrayList<Integer>> currentMissingValuesMap = runnable.getMissingEValues();
            missingValuesMap.putAll(currentMissingValuesMap);
            HyperScore hyperScore = runnable.getHyperScore();
            aHistograms.add(hyperScore.getAs());
            bHistograms.add(hyperScore.getBs());
        }
        if (!missingValuesMap.isEmpty()) {
            HashMap<Double, Integer> aHistogram = HistogramUtils.mergeHistograms(aHistograms);
            HashMap<Double, Integer> bHistogram = HistogramUtils.mergeHistograms(bHistograms);
            Double defaultA = null;
            if (!aHistogram.isEmpty()) {
                defaultA = HistogramUtils.getMedianValue(aHistogram);
            }
            Double defaultB = null;
            if (!bHistogram.isEmpty()) {
                defaultB = HistogramUtils.getMedianValue(bHistogram);
            }
            ArrayList<String> spectrumKeys = new ArrayList<String>(missingValuesMap.keySet());
            psmIterator = identification.getPsmIterator(spectrumKeys, null);
            pool = Executors.newFixedThreadPool(processingPreferences.getnThreads());
            for (int i = 1; i <= processingPreferences.getnThreads() && !waitingHandler.isRunCanceled(); i++) {
                MissingEValueEstimatorRunnable runnable = new MissingEValueEstimatorRunnable(missingValuesMap, defaultA, defaultB, psmIterator, identification, inputMap, identificationParameters, waitingHandler, exceptionHandler);
                pool.submit(runnable);
            }
            if (waitingHandler.isRunCanceled()) {
                pool.shutdownNow();
                return;
            }
            pool.shutdown();
            if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
                throw new InterruptedException("PSM scoring timed out. Please contact the developers.");
            }
        }
        
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // Restaure intensity scoring
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
     *
     * @throws IOException thrown if an exception occurred while reading or
     * writing a file
     * @throws InterruptedException thrown if a threading exception occurred
     * @throws SQLException thrown if an SQL exception occurred while retrieving
     * or storing an object from the database
     * @throws ClassNotFoundException thrown if a casting exception occurred
     * while retrieving an object from the database
     * @throws MzMLUnmarshallerException thrown if an exception occurred while
     * reading a spectrum from an mzml file
     */
    public ArrayList<Integer> estimateIntermediateScores(Identification identification, SpectrumMatch spectrumMatch, InputMap inputMap,
            IdentificationParameters identificationParameters, PeptideSpectrumAnnotator peptideSpectrumAnnotator, HyperScore hyperScore, WaitingHandler waitingHandler)
            throws SQLException, IOException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        PsmScoringPreferences psmScoringPreferences = identificationParameters.getPsmScoringPreferences();

        String spectrumKey = spectrumMatch.getKey();
        String spectrumFileName = spectrumMatch.getSpectrumFile();

        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = spectrumMatch.getAssumptionsMap();

        ArrayList<Integer> missingEvalue = new ArrayList<Integer>(0);

        for (Integer advocateIndex : assumptions.keySet()) {

            HashSet<Integer> scoresForAdvocate = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

            if (scoresForAdvocate != null) {

                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> originalAssumptions = assumptions.get(advocateIndex);

                // The hyperscore requires a second pass for the e-value estimation
                ArrayList<Double> hyperScores = null;
                ArrayList<PSParameter> hyperScoreParameters = null;
                ArrayList<Boolean> hyperScoreDecoys = null;
                if (scoresForAdvocate.contains(PsmScore.hyperScore.index)) {
                    hyperScores = new ArrayList<Double>(originalAssumptions.size());
                    hyperScoreParameters = new ArrayList<PSParameter>(originalAssumptions.size());
                    hyperScoreDecoys = new ArrayList<Boolean>(originalAssumptions.size());
                }

                for (Double originalScore : originalAssumptions.keySet()) {
                    for (SpectrumIdentificationAssumption assumption : originalAssumptions.get(originalScore)) {

                        if (assumption instanceof PeptideAssumption) {

                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                            Peptide peptide = peptideAssumption.getPeptide();
                            boolean decoy = peptide.isDecoy(sequenceMatchingPreferences);
                            PSParameter psParameter = new PSParameter();
                            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                            for (Integer scoreIndex : scoresForAdvocate) {

                                Double score;

                                if (scoreIndex.equals(PsmScore.native_score.index)) {
                                    score = peptideAssumption.getScore();
                                } else {
                                    SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                                    score = psmScoresEstimator.getDecreasingScore(peptide, peptideAssumption.getIdentificationCharge().value, spectrum, identificationParameters, specificAnnotationPreferences, peptideSpectrumAnnotator, scoreIndex);
                                }

                                psParameter.setIntermediateScore(scoreIndex, score);

                                if (scoreIndex.equals(PsmScore.hyperScore.index)) {
                                    hyperScores.add(-score);
                                    hyperScoreParameters.add(psParameter);
                                    hyperScoreDecoys.add(decoy);
                                } else {
                                    inputMap.setIntermediateScore(spectrumFileName, advocateIndex, scoreIndex, score, decoy, psmScoringPreferences);
                                }

                            }

                            assumption.addUrParam(psParameter);
                        }
                    }
                }
                if (scoresForAdvocate.contains(PsmScore.hyperScore.index)) {
                    HashMap<Double, Double> eValuesMap = hyperScore.getEValueMap(hyperScores);
                    if (eValuesMap != null) {
                        for (int i = 0; i < hyperScores.size(); i++) {
                            Double score = hyperScores.get(i);
                            PSParameter psParameter = hyperScoreParameters.get(i);
                            Boolean decoy = hyperScoreDecoys.get(i);
                            Double eValue = eValuesMap.get(score);
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
    public void estimateIntermediateScoreProbabilities(Identification identification, InputMap inputMap, ProcessingPreferences processingPreferences, WaitingHandler waitingHandler) {

        int totalProgress = 0;
        for (String spectrumFileName : identification.getSpectrumFiles()) {
            for (int advocateIndex : inputMap.getIntermediateScoreInputAlgorithms(spectrumFileName)) {
                ArrayList<Integer> scores = new ArrayList<Integer>(); //@TODO: implement scores
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

        for (String spectrumFileName : identification.getSpectrumFiles()) {
            for (int advocateIndex : inputMap.getIntermediateScoreInputAlgorithms(spectrumFileName)) {
                ArrayList<Integer> scores = new ArrayList<Integer>(); //@TODO: implement scores
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
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public void scorePsms(Identification identification, InputMap inputMap, ProcessingPreferences processingPreferences,
            IdentificationParameters identificationParameters, WaitingHandler waitingHandler)
            throws SQLException, IOException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        PsmScoringPreferences psmScoringPreferences = identificationParameters.getPsmScoringPreferences();

        PSParameter psParameter = new PSParameter();

        PsmIterator psmIterator = identification.getPsmIterator(waitingHandler);

        while (psmIterator.hasNext()) {

            SpectrumMatch spectrumMatch = psmIterator.next();
            String spectrumKey = spectrumMatch.getKey();
            String spectrumFileName = spectrumMatch.getSpectrumFile();

            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = spectrumMatch.getAssumptionsMap();

            for (int advocateIndex : assumptions.keySet()) {

                HashSet<Integer> scoresForAdvocate = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                if (scoresForAdvocate != null) {

                    HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateIndex);

                    for (double eValue : advocateAssumptions.keySet()) {
                        for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(eValue)) {

                            if (assumption instanceof PeptideAssumption) {

                                psParameter = (PSParameter) assumption.getUrParam(psParameter);

                                Double score = 1.0;

                                HashSet<Integer> scores = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                                if (scores.size() == 1 || !sequenceFactory.concatenatedTargetDecoy()) {
                                    score = psParameter.getIntermediateScore(scores.iterator().next());
                                } else {
                                    for (int scoreIndex : scores) {
                                        TargetDecoyMap targetDecoyMap = inputMap.getIntermediateScoreMap(spectrumFileName, advocateIndex, scoreIndex);
                                        Double intermediateScore = psParameter.getIntermediateScore(scoreIndex);
                                        if (intermediateScore != null) {
                                            Double p = targetDecoyMap.getProbability(intermediateScore);
                                            score *= (1.0 - p);
                                        }
                                    }
                                    score = 1 - score;
                                }

                                assumption.setScore(score);

                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                Peptide peptide = peptideAssumption.getPeptide();
                                boolean decoy = peptide.isDecoy(sequenceMatchingPreferences);
                                inputMap.addEntry(advocateIndex, spectrumFileName, assumption.getScore(), decoy);

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

//        br.close();
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
        private PsmIterator psmIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The input map
         */
        private InputMap inputMap;
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
         * The object used to estimate the hyper score.
         */
        private HyperScore hyperScore = new HyperScore();
        /**
         * Map of the advocates missing a hyperscore e-value for every spectrum.
         */
        private HashMap<String, ArrayList<Integer>> missingEValues = new HashMap<String, ArrayList<Integer>>();

        /**
         * Constructor.
         *
         * @param psmIterator An iterator of the PSMs to iterate
         * @param identification the identification containing all matches
         * @param inputMap the input map used to store the scores
         * @param identificationParameters the identification parameters
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public PsmScorerRunnable(PsmIterator psmIterator, Identification identification, InputMap inputMap,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
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
                while (psmIterator.hasNext() && !waitingHandler.isRunCanceled()) {
                    SpectrumMatch spectrumMatch = psmIterator.next();
                    if (spectrumMatch != null) {
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
        public HashMap<String, ArrayList<Integer>> getMissingEValues() {
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
        private PsmIterator psmIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The input map
         */
        private InputMap inputMap;
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
         * Map of the advocates missing a hyperscore e-value for every spectrum.
         */
        private HashMap<String, ArrayList<Integer>> missingEValues;
        /**
         * Default values for the a coefficient.
         */
        private Double defaultA;
        /**
         * Default values for the b coefficient.
         */
        private Double defaultB;

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
        public MissingEValueEstimatorRunnable(HashMap<String, ArrayList<Integer>> missingEValuesMap, Double defaultA, Double defaultB, PsmIterator psmIterator, Identification identification, InputMap inputMap,
                IdentificationParameters identificationParameters, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.missingEValues = missingEValuesMap;
            this.defaultA = defaultA;
            this.defaultB = defaultB;
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

                PSParameter psParameter = new PSParameter();
                boolean increaseProgress = true;
                PsmScoringPreferences psmScoringPreferences = identificationParameters.getPsmScoringPreferences();
                SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();

                while (psmIterator.hasNext() && !waitingHandler.isRunCanceled()) {

                    SpectrumMatch spectrumMatch = psmIterator.next();

                    if (spectrumMatch != null) {

                        String spectrumKey = spectrumMatch.getKey();
                        ArrayList<Integer> advocates = missingEValues.get(spectrumKey);

                        if (advocates != null) {

                            String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);
                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = spectrumMatch.getAssumptionsMap();

                            for (Integer advocateIndex : advocates) {

                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> originalAssumptions = assumptions.get(advocateIndex);
                                Double nMatches = null;
                                for (Double originalScore : originalAssumptions.keySet()) {
                                    for (SpectrumIdentificationAssumption assumption : originalAssumptions.get(originalScore)) {

                                        if (assumption instanceof PeptideAssumption) {

                                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                            Peptide peptide = peptideAssumption.getPeptide();
                                            boolean decoy = peptide.isDecoy(sequenceMatchingPreferences);
                                            psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                            Double hyperScore = -psParameter.getIntermediateScore(PsmScore.hyperScore.index);
                                            if (defaultA != null && defaultB != null) {
                                                Double eValue;
                                                if (hyperScore > 0) {
                                                    hyperScore = FastMath.log10(hyperScore);
                                                    eValue = HyperScore.getInterpolation(hyperScore, defaultA, defaultB);
                                                } else {
                                                    if (nMatches == null) {
                                                        nMatches = 0.0;
                                                        for (Double originalScoreTemp : originalAssumptions.keySet()) {
                                                            for (SpectrumIdentificationAssumption assumptionTemp : originalAssumptions.get(originalScoreTemp)) {
                                                                if (assumptionTemp instanceof PeptideAssumption) {
                                                                    nMatches += 1;
                                                                }
                                                            }
                                                        }
                                                    }
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
                }
            } catch (Exception e) {
                exceptionHandler.catchException(e);
                waitingHandler.setRunCanceled();
            }
        }
    }

}
