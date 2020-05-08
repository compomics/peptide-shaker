package eu.isas.peptideshaker.processing;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.protein_inference.PeptideChecker;
import eu.isas.peptideshaker.ptm.ModificationLocalizationScorer;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class processes PSMs.
 *
 * @author Marc Vaudel
 */
public class PsmProcessorRunnable implements Runnable {

    /**
     * Iterator for the spectrum matches to import.
     */
    private final ConcurrentLinkedQueue<Long> spectrumMatchKeysQueue;
    /**
     * The identification object.
     */
    private final Identification identification;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The input map.
     */
    private final InputMap inputMap;
    /**
     * The best match selection module.
     */
    private final BestMatchSelection bestMatchSelection;
    /**
     * The validator which will take care of the matches validation
     */
    private final MatchesValidator matchesValidator;
    /**
     * The PTM scorer responsible for scoring PTM localization.
     */
    private final ModificationLocalizationScorer modificationLocalizationScorer;
    /**
     * A provider for protein sequences.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * A provider for spectra.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * The spectrum annotator.
     */
    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
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
     * @param spectrumMatchKeysIterator Iterator for the spectrum match keys.
     * @param identification The identification object.
     * @param identificationParameters The identification parameters.
     * @param inputMap The input map.
     * @param matchesValidator The matches validator.
     * @param modificationLocalizationScorer The post-translational modification localization scorer.
     * @param sequenceProvider The protein sequences provider.
     * @param spectrumProvider The spectrum provider.
     * @param proteinCount The protein count.
     * @param waitingHandler The waiting handler.
     * @param exceptionHandler The exception handler.
     */
    public PsmProcessorRunnable(
            ConcurrentLinkedQueue<Long> spectrumMatchKeysIterator,
            Identification identification,
            IdentificationParameters identificationParameters,
            InputMap inputMap,
            MatchesValidator matchesValidator,
            ModificationLocalizationScorer modificationLocalizationScorer,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            HashMap<String, Integer> proteinCount,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) {

        this.spectrumMatchKeysQueue = spectrumMatchKeysIterator;
        this.identification = identification;
        this.identificationParameters = identificationParameters;
        this.inputMap = inputMap;
        this.matchesValidator = matchesValidator;
        this.modificationLocalizationScorer = modificationLocalizationScorer;
        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;

        this.bestMatchSelection = new BestMatchSelection(
                proteinCount,
                sequenceProvider,
                spectrumProvider,
                identificationParameters,
                peptideSpectrumAnnotator
        );
    }

    @Override
    public void run() {

        try {

            Long spectrumMatchKey;
            while ((spectrumMatchKey = spectrumMatchKeysQueue.poll()) != null) {

                processPsm(spectrumMatchKey);

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                waitingHandler.increaseSecondaryProgressCounter();

            }

        } catch (Exception e) {

            waitingHandler.setRunCanceled();
            exceptionHandler.catchException(e);

        }
    }

    /**
     * Saves assumption probabilities, selects best hit, scores modification
     * localization, and refines protein mapping accordingly for the given
     * spectrum match.
     *
     * @param spectrumMatchKey the key of the spectrum match to process
     * @param inputMap the input map
     * @param bestMatchSelection best match selection object
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     */
    private void processPsm(
            long spectrumMatchKey
    ) {

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumMatchKey);

        attachAssumptionsProbabilities(spectrumMatch);

        bestMatchSelection.selectBestHit(
                spectrumMatch, 
                inputMap, 
                matchesValidator.getPsmMap(),
                identification
        );

        if (spectrumMatch.getBestPeptideAssumption() != null) {

            // Score modification localization
            modificationLocalizationScorer.scorePTMs(
                    identification, 
                    spectrumMatch, 
                    sequenceProvider, 
                    spectrumProvider,
                    identificationParameters, 
                    waitingHandler, 
                    peptideSpectrumAnnotator
            );

            // Set modification sites
            modificationLocalizationScorer.modificationSiteInference(
                    spectrumMatch, 
                    sequenceProvider, 
                    identificationParameters
            );

            // Update protein mapping based on modification profile
            if (identificationParameters.getProteinInferenceParameters().isModificationRefinement()) {

                spectrumMatch.getAllPeptideAssumptions().forEach(
                        peptideAssumption -> PeptideChecker.checkPeptide(
                                peptideAssumption.getPeptide(),
                                sequenceProvider,
                                modificationSequenceMatchingParameters
                        ));
            }
        }
    }

    /**
     * Attaches the spectrum posterior error probabilities to the peptide
     * assumptions.
     *
     * @param inputMap map of the input scores
     * @param fastaParameters the FASTA parsing parameters
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachAssumptionsProbabilities(
            SpectrumMatch spectrumMatch
    ) {

        FastaParameters fastaParameters = identificationParameters.getFastaParameters();
        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();

        // Peptides
        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptionsMap = spectrumMatch.getPeptideAssumptionsMap();
        TreeMap<Double, ArrayList<PSParameter>> pepToParameterMap = new TreeMap<>();

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : peptideAssumptionsMap.entrySet()) {

            int searchEngine = entry.getKey();
            TreeMap<Double, ArrayList<PeptideAssumption>> seMapping = entry.getValue();
            double previousP = 0.0;
            ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<>(2);
            PeptideAssumption previousAssumption = null;

            for (Map.Entry<Double, ArrayList<PeptideAssumption>> entry2 : seMapping.entrySet()) {

                double eValue = entry2.getKey();
                ArrayList<PeptideAssumption> peptideAssumptions = entry2.getValue();

                for (PeptideAssumption assumption : peptideAssumptions) {

                    PSParameter psParameter = (PSParameter) assumption.getUrParam(PSParameter.dummy);

                    if (psParameter == null) {

                        psParameter = new PSParameter();

                    }

                    if (fastaParameters.isTargetDecoy()) {

                        double newP = inputMap.getProbability(searchEngine, eValue);
                        double pep = previousP;

                        if (newP > previousP) {

                            pep = newP;
                            previousP = newP;

                        }

                        psParameter.setProbability(pep);

                        ArrayList<PSParameter> pSParameters = pepToParameterMap.get(pep);

                        if (pSParameters == null) {

                            pSParameters = new ArrayList<>(1);
                            pepToParameterMap.put(pep, pSParameters);

                        }

                        pSParameters.add(psParameter);

                        if (previousAssumption != null) {

                            Peptide newPeptide = assumption.getPeptide();
                            Peptide previousPeptide = previousAssumption.getPeptide();

                            if (!newPeptide.isSameSequenceAndModificationStatus(previousPeptide, sequenceMatchingParameters)) {

                                for (PSParameter previousParameter : previousAssumptionsParameters) {

                                    double deltaPEP = pep - previousParameter.getProbability();
                                    previousParameter.setAlgorithmDeltaPEP(deltaPEP);

                                }

                                previousAssumptionsParameters.clear();

                            }
                        }

                        previousAssumption = assumption;
                        previousAssumptionsParameters.add(psParameter);

                    } else {

                        psParameter.setProbability(1.0);

                    }

                    assumption.addUrParam(psParameter);

                }
            }

            for (PSParameter previousParameter : previousAssumptionsParameters) {

                double deltaPEP = 1 - previousParameter.getProbability();
                previousParameter.setAlgorithmDeltaPEP(deltaPEP);

            }
        }

        // Compute the delta pep score accross all search engines
        double previousPEP = Double.NaN;
        ArrayList<PSParameter> previousParameters = new ArrayList<>();

        for (Map.Entry<Double, ArrayList<PSParameter>> entry : pepToParameterMap.entrySet()) {

            double pep = entry.getKey();

            if (!Double.isNaN(previousPEP)) {

                for (PSParameter previousParameter : previousParameters) {

                    double delta = pep - previousPEP;
                    previousParameter.setDeltaPEP(delta);

                }
            }

            previousParameters = entry.getValue();
            previousPEP = pep;

        }

        for (PSParameter previousParameter : previousParameters) {

            double delta = 1 - previousParameter.getProbability();
            previousParameter.setDeltaPEP(delta);

        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        // Assumptions
        HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> tagAssumptionsMap = spectrumMatch.getTagAssumptionsMap();

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<TagAssumption>>> entry : tagAssumptionsMap.entrySet()) {

            int algorithm = entry.getKey();
            TreeMap<Double, ArrayList<TagAssumption>> seMapping = entry.getValue();
            double previousP = 0;
            ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<>();
            TagAssumption previousAssumption = null;

            for (Map.Entry<Double, ArrayList<TagAssumption>> entry2 : seMapping.entrySet()) {

                double score = entry2.getKey();

                for (TagAssumption assumption : entry2.getValue()) {

                    PSParameter psParameter = (PSParameter) assumption.getUrParam(PSParameter.dummy);

                    if (psParameter == null) {

                        psParameter = new PSParameter();

                    }

                    if (fastaParameters.isTargetDecoy()) {

                        double newP = inputMap.getProbability(algorithm, score);
                        double pep = previousP;

                        if (newP > previousP) {

                            pep = newP;
                            previousP = newP;

                        }

                        psParameter.setProbability(pep);

                        ArrayList<PSParameter> pSParameters = pepToParameterMap.get(pep);

                        if (pSParameters == null) {

                            pSParameters = new ArrayList<>(1);
                            pepToParameterMap.put(pep, pSParameters);

                        }

                        pSParameters.add(psParameter);

                        if (previousAssumption != null) {

                            boolean same = false;
                            Tag newTag = ((TagAssumption) assumption).getTag();
                            Tag previousTag = previousAssumption.getTag();

                            if (newTag.isSameSequenceAndModificationStatusAs(previousTag, sequenceMatchingParameters)) {

                                same = true;

                            }

                            if (!same) {

                                for (PSParameter previousParameter : previousAssumptionsParameters) {

                                    double deltaPEP = pep - previousParameter.getProbability();
                                    previousParameter.setAlgorithmDeltaPEP(deltaPEP);

                                }

                                previousAssumptionsParameters.clear();

                            }
                        }

                        previousAssumption = assumption;
                        previousAssumptionsParameters.add(psParameter);

                    } else {

                        psParameter.setProbability(1.0);

                    }

                    assumption.addUrParam(psParameter);

                }
            }

            for (PSParameter previousParameter : previousAssumptionsParameters) {

                double deltaPEP = 1 - previousParameter.getProbability();
                previousParameter.setAlgorithmDeltaPEP(deltaPEP);

            }
        }
    }
}
