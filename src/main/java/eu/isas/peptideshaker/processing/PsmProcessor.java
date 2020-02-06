package eu.isas.peptideshaker.processing;

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
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Iterates the spectrum matches and saves assumption probabilities, selects
 * best hits, scores modification localization, and refines protein mapping
 * accordingly.
 *
 * @author Marc Vaudel
 */
public class PsmProcessor {
    
    /**
     * The identification object.
     */
    private final Identification identification;
    /**
     * A provider for protein sequences.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private final HashMap<String, Integer> proteinCount;
    /**
     * The validator which will take care of the matches validation
     */
    private final MatchesValidator matchesValidator;
    /**
     * The PTM scorer responsible for scoring PTM localization.
     */
    private final ModificationLocalizationScorer modificationLocalizationScorer;

    /**
     * Constructor.
     * 
     * @param identification the identification 
     * @param sequenceProvider a sequence provider
     * @param proteinCount the protein count map
     * @param matchesValidator the matches validator
     * @param modificationLocalizationScorer the modification localization scorer
     */
    public PsmProcessor(
            Identification identification, 
            SequenceProvider sequenceProvider, 
            HashMap<String, Integer> proteinCount,
            MatchesValidator matchesValidator, 
            ModificationLocalizationScorer modificationLocalizationScorer
    ) {
        
        this.identification = identification;
        this.sequenceProvider = sequenceProvider;
        this.proteinCount = proteinCount;
        this.matchesValidator = matchesValidator;
        this.modificationLocalizationScorer = modificationLocalizationScorer;
        
    }

    /**
     * Iterates the spectrum matches and saves assumption probabilities, selects
     * best hits, scores modification localization, and refines protein mapping
     * accordingly.
     *
     * @param inputMap the input map
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     */
    public void processPsms(
            InputMap inputMap, 
            IdentificationParameters identificationParameters, 
            WaitingHandler waitingHandler
    ) {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        
        BestMatchSelection bestMatchSelection = new BestMatchSelection(proteinCount, sequenceProvider, identificationParameters, new PeptideSpectrumAnnotator());
        
        identification.fillSpectrumIdentification();

        identification.getSpectrumIdentification().values().stream()
                .flatMap(HashSet::stream)
                .map(key -> identification.getSpectrumMatch(key)) // @TODO: should use batches instead of one key at the time!
                .forEach(spectrumMatch -> processPsm(
                        spectrumMatch, 
                        inputMap, 
                        bestMatchSelection, 
                        identificationParameters, 
                        waitingHandler
                ));
        
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Saves assumption probabilities, selects best hit, scores modification
     * localization, and refines protein mapping accordingly for the given
     * spectrum match.
     *
     * @param spectrumMatch the spectrum match to process
     * @param inputMap the input map
     * @param bestMatchSelection best match selection object
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     */
    private void processPsm(
            SpectrumMatch spectrumMatch, 
            InputMap inputMap,
            BestMatchSelection bestMatchSelection, 
            IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler
    ) {

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        FastaParameters fastaParameters = identificationParameters.getFastaParameters();
        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        attachAssumptionsProbabilities(spectrumMatch, inputMap, fastaParameters, sequenceMatchingParameters, waitingHandler);

        bestMatchSelection.selectBestHit(spectrumMatch, inputMap, matchesValidator.getPsmMap());

        if (spectrumMatch.getBestPeptideAssumption() != null) {

            // Score modification localization
            modificationLocalizationScorer.scorePTMs(identification, spectrumMatch, sequenceProvider, identificationParameters, waitingHandler, peptideSpectrumAnnotator);

            // Set modification sites
            modificationLocalizationScorer.modificationSiteInference(spectrumMatch, sequenceProvider, identificationParameters);

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

        waitingHandler.increaseSecondaryProgressCounter();

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
            SpectrumMatch spectrumMatch, 
            InputMap inputMap, 
            FastaParameters fastaParameters, 
            SequenceMatchingParameters sequenceMatchingPreferences, 
            WaitingHandler waitingHandler
    ) {

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

                            if (!newPeptide.isSameSequenceAndModificationStatus(previousPeptide, sequenceMatchingPreferences)) {

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

                            if (newTag.isSameSequenceAndModificationStatusAs(previousTag, sequenceMatchingPreferences)) {

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
