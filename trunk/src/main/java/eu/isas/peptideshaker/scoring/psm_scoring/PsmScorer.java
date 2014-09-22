package eu.isas.peptideshaker.scoring.psm_scoring;

import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.NeutralLossesMap;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.psm_scoring.PsmScores;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.InputMap;
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
 * @author Marc
 */
public class PsmScorer {

    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The protein sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * Scores the PSMs contained in an identification object.
     *
     * @param identification the object containing the identification matches
     * @param inputMap the input map scores
     * @param processingPreferences the processing preferences
     * @param annotationPreferences the annotation preferences
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the identification parameters
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public void estimateIntermediateScores(Identification identification, InputMap inputMap, ProcessingPreferences processingPreferences, AnnotationPreferences annotationPreferences,
            SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler) throws SQLException, IOException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        HashMap<Ion.IonType, HashSet<Integer>> iontypes = annotationPreferences.getIonTypes();
        NeutralLossesMap neutralLosses = annotationPreferences.getNeutralLosses();
        ArrayList<Integer> charges = annotationPreferences.getValidatedCharges();

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        //HashMap<Integer, BufferedWriter> brs = new HashMap<Integer, BufferedWriter>();
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            // batch load the spectrum matches
            identification.loadSpectrumMatches(spectrumFileName, null);

            for (String spectrumKey : identification.getSpectrumIdentification(spectrumFileName)) {

                waitingHandler.increaseSecondaryProgressCounter();
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);

                for (int advocateIndex : spectrumMatch.getAdvocates()) {
                    for (double eValue : spectrumMatch.getAllAssumptions(advocateIndex).keySet()) {
                        for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(advocateIndex).get(eValue)) {

                            if (assumption instanceof PeptideAssumption) {

                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                annotationPreferences.setCurrentSettings(peptideAssumption, true, sequenceMatchingPreferences);
                                PSParameter psParameter = new PSParameter();
                                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                                for (int scoreIndex : processingPreferences.getScores(advocateIndex)) {

                                    Peptide peptide = peptideAssumption.getPeptide();
                                    boolean decoy = peptide.isDecoy(sequenceMatchingPreferences);
                                    double score;

                                    if (scoreIndex == PsmScores.native_score.index) {
                                        score = peptideAssumption.getScore();
                                    } else {
                                        score = PsmScores.getDecreasingScore(peptide, spectrum, iontypes, neutralLosses, charges,
                                                peptideAssumption.getIdentificationCharge().value, searchParameters, scoreIndex);
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

                identification.updateSpectrumMatch(spectrumMatch);

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
     * Estimates the probabilities associated to the intermediate psm scores.
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
                ArrayList<Integer> scores = processingPreferences.getScores(advocateIndex);
                if (scores.size() > 1) {
                    for (int scoreIndex : scores) {
                        TargetDecoyMap targetDecoyMap = inputMap.getIntermediateScoreMap(spectrumFileName, advocateIndex, scoreIndex);
                        totalProgress += targetDecoyMap.getMapSize();
                    }
                }
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(totalProgress);

        for (String spectrumFileName : identification.getSpectrumFiles()) {
            for (int advocateIndex : inputMap.getIntermediateScoreInputAlgorithms(spectrumFileName)) {
                ArrayList<Integer> scores = processingPreferences.getScores(advocateIndex);
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
     * @param waitingHandler the handler displaying feedback to the user
     * @param searchParameters the identification parameters
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public void scorePsms(Identification identification, InputMap inputMap, ProcessingPreferences processingPreferences, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler)
            throws SQLException, IOException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        PSParameter psParameter = new PSParameter();

//        BufferedWriter br = new BufferedWriter(new FileWriter(new File("D:\\projects\\PeptideShaker\\rescoring", "combination.txt")));
//        br.write("Title\tPeptide\tScore\tDecoy");
//        br.newLine();
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            // batch load the spectrum matches
            identification.loadSpectrumMatches(spectrumFileName, null);

            for (String spectrumKey : identification.getSpectrumIdentification(spectrumFileName)) {

                waitingHandler.increaseSecondaryProgressCounter();

                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);

                for (int advocateIndex : spectrumMatch.getAdvocates()) {

                    for (double eValue : spectrumMatch.getAllAssumptions(advocateIndex).keySet()) {

                        for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(advocateIndex).get(eValue)) {

                            if (assumption instanceof PeptideAssumption) {

                                psParameter = (PSParameter) assumption.getUrParam(psParameter);

                                double score = 1;

                                ArrayList<Integer> scores = processingPreferences.getScores(advocateIndex);

                                if (scores.size() == 1 || !sequenceFactory.concatenatedTargetDecoy()) {
                                    score = psParameter.getIntermediateScore(scores.get(0));
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
                                inputMap.addEntry(advocateIndex, spectrumFileName, score, decoy);

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

                identification.updateSpectrumMatch(spectrumMatch);
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }
        }

//        br.close();
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }
}
