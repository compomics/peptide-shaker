package eu.isas.peptideshaker;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.AdvocateFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.fdrestimation.InputMap;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.ProteinMap;
import eu.isas.peptideshaker.fdrestimation.PsmSpecificMap;
import eu.isas.peptideshaker.fileimport.FastaImporter;
import eu.isas.peptideshaker.gui.WaitingDialog;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.fileimport.IdImporter;
import eu.isas.peptideshaker.fileimport.SpectrumImporter;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class will be responsible for the identification import and the associated calculations
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShaker {

    /**
     * If set to true, detailed information is sent to the waiting dialog.
     */
    private boolean detailedReport = false;
    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The psm map
     */
    private PsmSpecificMap psmMap;
    /**
     * The peptide map
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The protein map
     */
    private ProteinMap proteinMap;
    /**
     * The id importer will import and process the identifications
     */
    private IdImporter idImporter = null;
    /**
     * The spectrum importer will import spectra
     */
    private SpectrumImporter spectrumImporter;
    /**
     * The fasta importer will import sequences from a fasta file
     */
    private FastaImporter fastaImporter;
    /**
     * Boolean indicating whether the processing of identifications is finished
     */
    private boolean idProcessingFinished = false;

    /**
     * Constructor without mass specification. Calculation will be done on new maps
     * which will be retrieved as compomics utilities parameters.
     *
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     */
    public PeptideShaker(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        psmMap = new PsmSpecificMap(experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSpectrumCollection());
        peptideMap = new PeptideSpecificMap();
        proteinMap = new ProteinMap();
    }

    /**
     * Constructor with map specifications.
     *
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     * @param psMaps            the peptide shaker maps
     */
    public PeptideShaker(MsExperiment experiment, Sample sample, int replicateNumber, PSMaps psMaps) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.psmMap = psMaps.getPsmSpecificMap();
        this.peptideMap = psMaps.getPeptideSpecificMap();
        this.proteinMap = psMaps.getProteinMap();
    }

    /**
     * Method used to import identification from identification result files
     *
     * @param waitingDialog     A dialog to display the feedback
     * @param idFilter          The identification filter to use
     * @param idFiles           The files to import
     * @param spectrumFiles     The corresponding spectra (can be empty: spectra will not be loaded)
     */
    public void importIdentifications(WaitingDialog waitingDialog, IdFilter idFilter, ArrayList<File> idFiles, ArrayList<File> spectrumFiles) {
        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        Ms2Identification identification = new Ms2Identification();
        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, identification);
        idImporter = new IdImporter(this, waitingDialog, analysis, idFilter);
        idImporter.importFiles(idFiles, spectrumFiles);
    }

    /**
     * Method used to import identified spectra from files
     *
     * @param waitingDialog     A dialog to display feedback to the user
     * @param spectrumFiles     The files to import
     */
    public void importSpectra(WaitingDialog waitingDialog, ArrayList<File> spectrumFiles) {
        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        spectrumImporter = new SpectrumImporter(this, analysis);
        spectrumImporter.importSpectra(waitingDialog, spectrumFiles);
    }

    /**
     * Sets whether the the import was finished
     *
     * @param waitingDialog the dialog which displays feedback to the user
     */
    public void setRunFinished(WaitingDialog waitingDialog) {
        if (idImporter != null) {
            if (!idProcessingFinished) {
                return;
            }
        }
        if (fastaImporter != null) {
            if (!fastaImporter.isRunFinished()) {
                return;
            }
        }
        waitingDialog.setRunFinished();
    }

    /**
     * Method used to import sequences from a fasta file
     *
     * @param waitingDialog     A dialog to display feedback to the user
     * @param file              the file to import
     * @param databaseName
     * @param databseVersion
     * @param stringBefore
     * @param stringAfter
     */
    public void importFasta(WaitingDialog waitingDialog, File file, String databaseName, String databseVersion, String stringBefore, String stringAfter) {
        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        fastaImporter = new FastaImporter(this);
        fastaImporter.importSpectra(waitingDialog, analysis, file, databaseName, databseVersion, stringBefore, stringAfter);
    }

    /**
     * Method for processing of results from utilities data (no file). From ms_lims for instance.
     *
     * @param waitingDialog     A dialog to display the feedback
     */
    public void processIdentifications(WaitingDialog waitingDialog) {
        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        idImporter = new IdImporter(this, waitingDialog, analysis);
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
            inputMap.estimateProbabilities();
        }
        waitingDialog.appendReport("Computing spectrum probabilities.");
        fillPsmMap(inputMap);
        psmMap.cure();
        psmMap.estimateProbabilities();
        attachSpectrumProbabilities();
        waitingDialog.appendReport("Computing peptide probabilities.");
        fillPeptideMaps();
        peptideMap.cure();
        peptideMap.estimateProbabilities();
        attachPeptideProbabilities();
        waitingDialog.appendReport("Computing protein probabilities.");
        fillProteinMap();
        proteinMap.estimateProbabilities();
        attachProteinProbabilities();
        waitingDialog.appendReport("Trying to resolve protein inference issues.");
        try {
            cleanProteinGroups(waitingDialog);
        } catch (Exception e) {
            waitingDialog.appendReport("An error occured while trying to resolve protein inference issues.");
        }
        String report = "Identification processing completed.\n\n";
        ArrayList<Integer> suspiciousInput = inputMap.suspiciousInput();
        ArrayList<String> suspiciousPsms = psmMap.suspiciousInput();
        ArrayList<String> suspiciousPeptides = peptideMap.suspiciousInput();
        boolean suspiciousProteins = proteinMap.suspicousInput();
        if (suspiciousInput.size() > 0
                || suspiciousPsms.size() > 0
                || suspiciousPeptides.size() > 0
                || suspiciousProteins) {

            // @TODO: display this in a separate dialog??

            if (detailedReport) {

                report += "The following identification classes retieved non robust statistical estimations, we advice to control the quality of the corresponding matches: \n";

                boolean firstLine = true;
                for (int searchEngine : suspiciousInput) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        report += ", ";
                    }
                    report += AdvocateFactory.getInstance().getAdvocate(searchEngine).getName();
                }
                if (suspiciousInput.size() > 0) {
                    report += " identifications.\n";
                }

                firstLine = true;
                for (String fraction : suspiciousPsms) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        report += ", ";
                    }
                    report += fraction;
                }
                report += " charged spectra.\n";

                firstLine = true;
                for (String fraction : suspiciousPeptides) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        report += ", ";
                    }
                    report += fraction;
                }
                report += " modified peptides.\n";

                if (suspiciousProteins) {
                    report += "proteins. \n";
                }
            }
        }

        waitingDialog.appendReport(report);
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identification.addUrParam(new PSMaps(proteinMap, psmMap, peptideMap));
        idProcessingFinished = true;
        setRunFinished(waitingDialog);
    }

    /**
     * Processes the identifications if a change occured in the psm map
     *
     * @throws Exception    Exception thrown whenever it is attempted to attach more
     *                      than one identification per search engine per spectrum
     */
    public void spectrumMapChanged() throws Exception {
        peptideMap = new PeptideSpecificMap();
        proteinMap = new ProteinMap();
        attachSpectrumProbabilities();
        fillPeptideMaps();
        peptideMap.cure();
        peptideMap.estimateProbabilities();
        attachPeptideProbabilities();
        fillProteinMap();
        proteinMap.estimateProbabilities();
        attachProteinProbabilities();
        cleanProteinGroups(null);
    }

    /**
     * Processes the identifications if a change occured in the peptide map
     * @throws Exception    Exception thrown whenever it is attempted to attach
     *                      more than one identification per search engine per spectrum
     */
    public void peptideMapChanged() throws Exception {
        proteinMap = new ProteinMap();
        attachPeptideProbabilities();
        fillProteinMap();
        proteinMap.estimateProbabilities();
        attachProteinProbabilities();
        cleanProteinGroups(null);
    }

    /**
     * Processes the identifications if a change occured in the protein map
     */
    public void proteinMapChanged() {
        attachProteinProbabilities();
    }

    /**
     * This method will flag validated identifications
     */
    public void validateIdentifications() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();

        double proteinThreshold = 0;
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            psParameter = (PSParameter) proteinMatch.getUrParam(psParameter);
            if (psParameter.getProteinProbabilityScore() <= proteinThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }

        double peptideThreshold;
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {
            peptideThreshold = 0;
            psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);
            if (psParameter.getPeptideProbabilityScore() <= peptideThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }

        double psmThreshold;
        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
            psmThreshold = 0;
            psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);
            if (psParameter.getPsmProbabilityScore() <= psmThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }
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
                    id = peptideAssumption.getPeptide().getKey();
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
            psParameter.setPsmProbability(psmMap.getProbability(spectrumMatch, psParameter.getPsmProbabilityScore()));
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
                    probaScore = probaScore * psParameter.getPsmProbability();
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
     * Fills the protein map
     */
    private void fillProteinMap() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            probaScore = 1;
            for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);
                probaScore = probaScore * psParameter.getPeptideProbability();
            }
            psParameter = new PSParameter();
            psParameter.setProteinProbabilityScore(probaScore);
            proteinMatch.addUrParam(psParameter);
            proteinMap.addPoint(probaScore, proteinMatch.isDecoy());
        }
    }

    /**
     * Attaches the protein posterior error probability to the protein matches
     */
    private void attachProteinProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        double proteinProbability;
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            psParameter = (PSParameter) proteinMatch.getUrParam(psParameter);
            proteinProbability = proteinMap.getProbability(psParameter.getProteinProbabilityScore());
            psParameter.setProteinProbability(proteinProbability);
        }
    }

    /**
     * Solves protein inference issues when possible.
     * @throws Exception    exception thrown whenever it is attempted to attach two different spectrum matches to the same spectrum from the same search engine.
     */
    private void cleanProteinGroups(WaitingDialog waitingDialog) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        boolean better;
        ProteinMatch proteinShared;
        double sharedProteinProbabilityScore, uniqueProteinProbabilityScore;
        ArrayList<String> toRemove = new ArrayList<String>();
        int nLeft = 0;
        for (String proteinSharedKey : identification.getProteinIdentification().keySet()) {
            proteinShared = identification.getProteinIdentification().get(proteinSharedKey);
            psParameter = (PSParameter) proteinShared.getUrParam(psParameter);
            sharedProteinProbabilityScore = psParameter.getProteinProbabilityScore();
            if (proteinShared.getNProteins() > 1 && sharedProteinProbabilityScore < 1) {
                proteinShared = identification.getProteinIdentification().get(proteinSharedKey);
                psParameter = (PSParameter) proteinShared.getUrParam(psParameter);
                sharedProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                better = false;
                for (ProteinMatch proteinUnique : identification.getProteinIdentification().values()) {
                    if (proteinShared.contains(proteinUnique)) {
                        psParameter = (PSParameter) proteinUnique.getUrParam(psParameter);
                        uniqueProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                        for (PeptideMatch sharedPeptide : proteinShared.getPeptideMatches().values()) {
                            proteinUnique.addPeptideMatch(sharedPeptide);
                        }
                        if (uniqueProteinProbabilityScore <= sharedProteinProbabilityScore) {
                            better = true;
                        }
                    }
                }
                if (better) {
                    toRemove.add(proteinSharedKey);
                } else {
                    nLeft++;
                }
            }
        }
        for (String proteinKey : toRemove) {
            proteinShared = identification.getProteinIdentification().get(proteinKey);
            psParameter = (PSParameter) proteinShared.getUrParam(psParameter);
            proteinMap.removePoint(psParameter.getProteinProbabilityScore(), proteinShared.isDecoy());
            identification.getProteinIdentification().remove(proteinKey);
        }
        if (waitingDialog != null) {
            waitingDialog.appendReport(toRemove.size() + " conflicts resolved. " + nLeft + " protein groups remaining.");
        }
    }
}
