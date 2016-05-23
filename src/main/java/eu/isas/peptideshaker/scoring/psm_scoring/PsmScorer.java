package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.psm_scoring.PsmScore;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PsmScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
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
     * Scores the PSMs contained in an identification object.
     *
     * @param identification the object containing the identification matches
     * @param inputMap the input map scores
     * @param processingPreferences the processing preferences
     * @param shotgunProtocol information on the protocol used
     * @param identificationParameters identification parameters used
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
    public void estimateIntermediateScores(Identification identification, InputMap inputMap, ProcessingPreferences processingPreferences,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, WaitingHandler waitingHandler)
            throws SQLException, IOException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        PsmScoringPreferences psmScoringPreferences = identificationParameters.getPsmScoringPreferences();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        //HashMap<Integer, BufferedWriter> brs = new HashMap<Integer, BufferedWriter>();
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, null, false, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();

                HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);

                for (int advocateIndex : assumptions.keySet()) {

                    HashSet<Integer> scoresForAdvocate = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                    if (scoresForAdvocate != null) {

                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateIndex);

                        for (double eValue : advocateAssumptions.keySet()) {
                            for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(eValue)) {

                                if (assumption instanceof PeptideAssumption) {

                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    PSParameter psParameter = new PSParameter();
                                    MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                                    for (int scoreIndex : scoresForAdvocate) {

                                        Peptide peptide = peptideAssumption.getPeptide();
                                        boolean decoy = peptide.isDecoy(sequenceMatchingPreferences);
                                        double score;

                                        if (scoreIndex == PsmScore.native_score.index) {
                                            score = peptideAssumption.getScore();
                                        } else {
                                            SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                                            score = PsmScore.getDecreasingScore(peptide, peptideAssumption.getIdentificationCharge().value, spectrum, shotgunProtocol, identificationParameters, specificAnnotationPreferences, scoreIndex);
                                        }

                                        psParameter.setIntermediateScore(scoreIndex, score);
                                        inputMap.setIntermediateScore(spectrumFileName, advocateIndex, scoreIndex, score, decoy);

//                                    try {
//                                        BufferedWriter br = brs.get(scoreIndex);
//                                        if (br == null) {
//                                            PsmScores psmScores = PsmScores.getScore(scoreIndex);
//                                            br = new BufferedWriter(new FileWriter(new File("D:\\projects\\PeptideShaker\\rescoring", psmScores.name + ".txt")));
//                                            brs.put(scoreIndex, br);
//                                            br.write("Title\tPeptide\tScore\tDecoy");
//                                        br.newLine();
//                                        }
//                                        if (decoy) {
//                                            br.write(Spectrum.getSpectrumTitle(spectrumKey) + "\t" + peptide.getKey() + "\t" + score + "\t" + 1);
//                                        } else {
//                                            br.write(Spectrum.getSpectrumTitle(spectrumKey) + "\t" + peptide.getKey() + "\t" + score + "\t" + 0);
//                                        }
//                                        br.newLine();
//
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
                                    }

                                    assumption.addUrParam(psParameter);
                                }
                            }
                        }
                    }
                }

                identification.updateAssumptions(spectrumKey, assumptions);

                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }
        }

//        for (BufferedWriter br : brs.values()) {
//            br.close();
//        }
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
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

//        BufferedWriter br = new BufferedWriter(new FileWriter(new File("D:\\projects\\PeptideShaker\\rescoring", "combination.txt")));
//        br.write("Title\tPeptide\tScore\tDecoy");
//        br.newLine();
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, null, false, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();

                HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);

                for (int advocateIndex : assumptions.keySet()) {

                    HashSet<Integer> scoresForAdvocate = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                    if (scoresForAdvocate != null) {

                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(advocateIndex);

                        for (double eValue : advocateAssumptions.keySet()) {
                            for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(eValue)) {

                                if (assumption instanceof PeptideAssumption) {

                                    psParameter = (PSParameter) assumption.getUrParam(psParameter);

                                    double score = 1;

                                    HashSet<Integer> scores = psmScoringPreferences.getScoreForAlgorithm(advocateIndex);

                                    if (scores.size() == 1 || !sequenceFactory.concatenatedTargetDecoy()) {
                                        score = psParameter.getIntermediateScore(scores.iterator().next());
                                    } else {
                                        for (int scoreIndex : scores) {
                                            TargetDecoyMap targetDecoyMap = inputMap.getIntermediateScoreMap(spectrumFileName, advocateIndex, scoreIndex);
                                            Double intermediateScore = psParameter.getIntermediateScore(scoreIndex);
                                            if (intermediateScore != null) {
                                                double p = targetDecoyMap.getProbability(intermediateScore);
                                                score *= p;
                                            }
                                        }
                                    }

                                    assumption.setScore(score);

                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    Peptide peptide = peptideAssumption.getPeptide();
                                    boolean decoy = peptide.isDecoy(sequenceMatchingPreferences);
                                    inputMap.addEntry(advocateIndex, spectrumFileName, assumption.getScore(), decoy);

//                                if (decoy) {
//                                    br.write(Spectrum.getSpectrumTitle(spectrumKey) + "\t" + peptide.getKey() + "\t" + score + "\t" + 1);
//                                } else {
//                                    br.write(Spectrum.getSpectrumTitle(spectrumKey) + "\t" + peptide.getKey() + "\t" + score + "\t" + 0);
//                                }
//                                br.newLine();
                                }
                            }
                        }
                    }
                }

                identification.updateAssumptions(spectrumKey, assumptions);
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }
        }

//        br.close();
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }
}
