package eu.isas.peptideshaker;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.fdrestimation.InputMap;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.PsmSpecificMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyMap;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.WaitingDialog;
import eu.isas.peptideshaker.idimport.IdFilter;
import eu.isas.peptideshaker.idimport.IdImporter;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class will be responsible for the identification import and the associated calculations
 * @author Marc
 */
public class IdentificationShaker {

    /**
     * The experiment conducted
     */
    MsExperiment experiment;
    /**
     * The sample analyzed
     */
    Sample sample;
    /**
     * The replicate number
     */
    int replicateNumber;

    /**
     * The psm map
     */
    PsmSpecificMap psmMap;
    /**
     * The peptide map
     */
    PeptideSpecificMap peptideMap;
    /**
     * The protein map
     */
    TargetDecoyMap proteinMap;


    /**
     * Main constructor
     */
    public IdentificationShaker() {
    }

    /**
     * Method used to import identification from identification result files
     * @param waitingDialog     A dialog to display the feedback
     * @param experiment        The experiment conducted
     * @param sample            The sample measured
     * @param replicateNumber   The replicate number
     * @param idFilter          The identification filter to use
     * @param idFiles           The files to import
     */
    public void importIdentifications(WaitingDialog waitingDialog, MsExperiment experiment, Sample sample, int replicateNumber, IdFilter idFilter, ArrayList<File> idFiles) {

        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;

        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        Ms2Identification identification = new Ms2Identification();
        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, identification);
        IdImporter idImporter = new IdImporter(this, waitingDialog, identification, idFilter);
        idImporter.importFiles(idFiles);
    }

    /**
     * Method for processing of results from utilities data (no file). From ms_lims for instance.
     * @param waitingDialog     A dialog to display the feedback
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     */
    public void processIdentifications(WaitingDialog waitingDialog, MsExperiment experiment, Sample sample, int replicateNumber) {

        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        IdImporter idImporter = new IdImporter(this, waitingDialog, identification);
        idImporter.importIdentifications();
    }

    /**
     * This method processes the identifications and fills the peptide shaker maps
     *
     * @param inputMap          The input map
     * @param waitingDialog     A dialog to display the feedback
     */
    public void processIdentifications(InputMap inputMap, WaitingDialog waitingDialog) {
        if (inputMap.isMultipleSearchEngines()) {
            inputMap.computeProbabilities(waitingDialog);
        }
        waitingDialog.appendReport("Computing spectrum probabilities.");
        psmMap = new PsmSpecificMap();
        fillPsmMap(inputMap);
        psmMap.cure(waitingDialog);
        psmMap.estimateProbabilities(waitingDialog);
        attachSpectrumProbabilities();
        waitingDialog.appendReport("Computing peptide probabilities.");
        peptideMap = new PeptideSpecificMap();
        fillPeptideMaps();
        peptideMap.cure(waitingDialog);
        peptideMap.estimateProbabilities(waitingDialog);
        attachPeptideProbabilities();
        waitingDialog.appendReport("Computing protein probabilities.");
        proteinMap = new TargetDecoyMap("protein");
        fillProteinMap();
        proteinMap.estimateProbabilities(waitingDialog);
        attachProteinProbabilities();
        waitingDialog.appendReport("Identification processing completed.");
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identification.addUrParam(new PSMaps(proteinMap, psmMap, peptideMap));
        waitingDialog.setRunFinished();
    }

    /**
     * Fills the psm specific map
     *
     * @param inputMap       The input map
     */
    private void fillPsmMap(InputMap inputMap) {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        HashMap<String, Double> identifications;
        HashMap<Double, PeptideAssumption> peptideAssumptions;
        PSParameter psParameter;
        PeptideAssumption peptideAssumption;
        if (inputMap.isMultipleSearchEngines()) {
            for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                psParameter = new PSParameter();
                identifications = new HashMap<String, Double>();
                peptideAssumptions = new HashMap<Double, PeptideAssumption>();
                String id;
                double p, pScore = 1;
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    p = inputMap.getProbability(searchEngine, peptideAssumption.getEValue());
                    pScore = pScore * p;
                    id = peptideAssumption.getPeptide().getIndex();
                    if (identifications.containsKey(id)) {
                        p = identifications.get(id) * p;
                        identifications.put(id, p);
                        peptideAssumptions.put(p, peptideAssumption);
                    } else {
                        identifications.put(id, p);
                        peptideAssumptions.put(p, peptideAssumption);
                    }
                }
                double pMin = Collections.min(identifications.values());
                psParameter.setSpectrumProbabilityScore(pScore);
                spectrumMatch.addUrParam(psParameter);
                spectrumMatch.setBestAssumption(peptideAssumptions.get(pMin));
                psmMap.addPoint(pScore, spectrumMatch);
            }
        } else {
            double eValue;
            for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                psParameter = new PSParameter();
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    eValue = peptideAssumption.getEValue();
                    psParameter.setSpectrumProbabilityScore(eValue);
                    spectrumMatch.setBestAssumption(peptideAssumption);
                    psmMap.addPoint(eValue, spectrumMatch);
                }
                spectrumMatch.addUrParam(psParameter);
            }
        }
    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum matches
     */
    private void attachSpectrumProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
            psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);
            psParameter.setSpectrumProbability(psmMap.getProbability(spectrumMatch, psParameter.getSpectrumProbabilityScore()));
        }
    }

    /**
     * Fills the peptide specific map
     */
    private void fillPeptideMaps() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {
            probaScore = 1;
            for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
                if (spectrumMatch.getBestAssumption().getPeptide().isSameAs(peptideMatch.getTheoreticPeptide())) {
                    psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);
                    probaScore = probaScore * psParameter.getSpectrumProbability();
                }
            }
            psParameter = new PSParameter();
            psParameter.setPeptideProbabilityScore(probaScore);
            peptideMatch.addUrParam(psParameter);
            peptideMap.addPoint(probaScore, peptideMatch);
        }
    }

    /**
     * Attaches the peptide posterior error probabilities to the peptide matches
     */
    private void attachPeptideProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {
            psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);
            psParameter.setPeptideProbability(peptideMap.getProbability(peptideMatch, psParameter.getPeptideProbabilityScore()));
        }
    }

    /**
     * fills the protein map
     */
    private void fillProteinMap() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            probaScore = 1;
            for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                if (peptideMatch.getTheoreticPeptide().getParentProteins().size() == 1) {
                    psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);
                    probaScore = probaScore * psParameter.getPeptideProbability();
                }
            }
            psParameter = new PSParameter();
            psParameter.setProteinProbabilityScore(probaScore);
            proteinMatch.addUrParam(psParameter);
            proteinMap.put(probaScore, proteinMatch.isDecoy());
        }
    }

    /**
     * Attaches the protein posterior error probability to the protein matches
     */
    private void attachProteinProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            psParameter = (PSParameter) proteinMatch.getUrParam(psParameter);
            psParameter.setProteinProbability(proteinMap.getProbability(psParameter.getProteinProbabilityScore()));
        }
    }

}
