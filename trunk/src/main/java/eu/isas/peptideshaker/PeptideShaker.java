package eu.isas.peptideshaker;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.AdvocateFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.ptm.PTMLocationScores;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.gui.WaitingDialog;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.fileimport.FileImporter;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.preferences.SearchParameters;
import eu.isas.peptideshaker.utils.Metrics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
     * The experiment conducted.
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed.
     */
    private Sample sample;
    /**
     * The replicate number.
     */
    private int replicateNumber;
    /**
     * The psm map.
     */
    private PsmSpecificMap psmMap;
    /**
     * The peptide map
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The protein map.
     */
    private ProteinMap proteinMap;
    /**
     * The id importer will import and process the identifications.
     */
    private FileImporter fileImporter = null;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The location of the folder used for serialization of matches.
     */
    public final static String SERIALIZATION_DIRECTORY = "matches";
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Metrics to be picked when loading the identification.
     */
    private Metrics metrics = new Metrics();

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
        psmMap = new PsmSpecificMap();
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
     * @param waitingDialog         A dialog to display the feedback
     * @param idFilter              The identification filter to use
     * @param idFiles               The files to import
     * @param spectrumFiles         The corresponding spectra (can be empty: spectra will not be loaded)
     * @param fastaFile             The database file in the fasta format
     * @param searchParameters      The search parameters
     * @param annotationPreferences The annotation preferences to use for PTM scoring
     * @param projectDetails        The project details
     */
    public void importFiles(WaitingDialog waitingDialog, IdFilter idFilter, ArrayList<File> idFiles, ArrayList<File> spectrumFiles,
            File fastaFile, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ProjectDetails projectDetails) {

        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, new Ms2Identification());
        Identification identification = analysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identification.setInMemory(false);
        identification.setAutomatedMemoryManagement(true);
        identification.setSerializationDirectory(SERIALIZATION_DIRECTORY);
        fileImporter = new FileImporter(this, waitingDialog, analysis, idFilter);
        fileImporter.importFiles(idFiles, spectrumFiles, fastaFile, searchParameters, annotationPreferences);
    }

    /**
     * This method processes the identifications and fills the peptide shaker maps
     *
     * @param inputMap          The input map
     * @param waitingDialog     A dialog to display the feedback
     * @param searchParameters
     * @param annotationPreferences
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws Exception  
     */
    public void processIdentifications(InputMap inputMap, WaitingDialog waitingDialog, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, IdFilter idFilter)
            throws IllegalArgumentException, IOException, Exception {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        if (!identification.memoryCheck()) {
            waitingDialog.appendReport("PeptideShaker is encountering memory issues! See http://peptide-shaker.googlecode.com for help.");
        }
        try {
            waitingDialog.appendReport("Computing assumptions probabilities.");
            inputMap.estimateProbabilities(waitingDialog);
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Adding assumptions probabilities.");
            attachAssumptionsProbabilities(inputMap, waitingDialog);
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Selecting best hit per spectrum.");
            setFirstHit(waitingDialog, idFilter);
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Generating PSM map.");
            fillPsmMap(inputMap, waitingDialog);
            psmMap.cure();
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Computing PSM probabilities.");
            psmMap.estimateProbabilities(waitingDialog);
            waitingDialog.appendReport("Generating Peptide map.");
            attachSpectrumProbabilities();
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Building peptides and proteins.");
            identification.buildPeptidesAndProteins();
            fillPeptideMaps();
            peptideMap.cure();
            waitingDialog.appendReport("Computing peptide probabilities.");
            peptideMap.estimateProbabilities(waitingDialog);
            attachPeptideProbabilities();
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Computing protein probabilities.");
            fillProteinMap(waitingDialog);
            proteinMap.estimateProbabilities(waitingDialog);
            attachProteinProbabilities();
            waitingDialog.increaseProgressValue();
        } catch (Exception e) {
            throw e;
        }

        try {
            waitingDialog.appendReport("Trying to resolve protein inference issues.");
            cleanProteinGroups(waitingDialog);
            waitingDialog.increaseProgressValue();
        } catch (IllegalArgumentException e) {
            waitingDialog.appendReport("An error occured while trying to resolve protein inference issues.");
            throw e;
        } catch (IOException e) {
            waitingDialog.appendReport("An error occured while trying to resolve protein inference issues.");
            throw e;
        }

        waitingDialog.appendReport("Correcting protein probabilities.");
        proteinMap.estimateProbabilities(waitingDialog);
        attachProteinProbabilities();
        waitingDialog.increaseProgressValue();

        waitingDialog.appendReport("Validating identifications at 1% FDR.");
        fdrValidation(waitingDialog);
        waitingDialog.increaseProgressValue();
        waitingDialog.appendReport("Scoring PTMs in peptides.");
        scorePeptidePtms(waitingDialog, searchParameters, annotationPreferences);
        waitingDialog.increaseProgressValue();
        waitingDialog.appendReport("Scoring PTMs in proteins.");
        scoreProteinPtms(waitingDialog, searchParameters, annotationPreferences);
        waitingDialog.increaseProgressValue();

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

                report += "The following identification classes retieved non robust statistical estimations, "
                        + "we advice to control the quality of the corresponding matches: \n";

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
        identification.addUrParam(new PSMaps(proteinMap, psmMap, peptideMap));
        waitingDialog.setRunFinished();
    }

    /**
     * Makes a preliminary validation of hits. By default a 1% FDR is used for all maps
     * 
     * @param waitingDialog a reference to the waiting dialog
     */
    public void fdrValidation(WaitingDialog waitingDialog) {

        TargetDecoyMap currentMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults currentResults = currentMap.getTargetDecoyResults();
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(1.0);
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        int max = peptideMap.getKeys().size() + psmMap.getKeys().keySet().size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);

        for (String mapKey : peptideMap.getKeys()) {
            waitingDialog.increaseSecondaryProgressValue();
            currentMap = peptideMap.getTargetDecoyMap(mapKey);
            currentResults = currentMap.getTargetDecoyResults();
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(1.0);
            currentMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        for (int mapKey : psmMap.getKeys().keySet()) {
            waitingDialog.increaseSecondaryProgressValue();
            currentMap = psmMap.getTargetDecoyMap(mapKey);
            currentResults = currentMap.getTargetDecoyResults();
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(1.0);
            currentMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        waitingDialog.setSecondaryProgressDialogIntermediate(true);

        validateIdentifications();
    }

    /**
     * Processes the identifications if a change occured in the psm map
     *
     * @throws Exception    Exception thrown whenever it is attempted to attach more
     *                      than one identification per search engine per spectrum
     */
    public void spectrumMapChanged() throws Exception {

        // @TODO: add progress bars?

        peptideMap = new PeptideSpecificMap();
        proteinMap = new ProteinMap();
        attachSpectrumProbabilities();
        fillPeptideMaps();
        peptideMap.cure();
        peptideMap.estimateProbabilities(null);
        attachPeptideProbabilities();
        fillProteinMap(null);
        proteinMap.estimateProbabilities(null);
        attachProteinProbabilities();
        cleanProteinGroups(null);
    }

    /**
     * Processes the identifications if a change occured in the peptide map
     * @throws Exception    Exception thrown whenever it is attempted to attach
     *                      more than one identification per search engine per spectrum
     */
    public void peptideMapChanged() throws Exception {

        // @TODO: add progress bars?

        proteinMap = new ProteinMap();
        attachPeptideProbabilities();
        fillProteinMap(null);
        proteinMap.estimateProbabilities(null);
        attachProteinProbabilities();
        cleanProteinGroups(null);
    }

    /**
     * Processes the identifications if a change occured in the protein map
     */
    public void proteinMapChanged() {

        // @TODO: add progress bar?

        attachProteinProbabilities();
    }

    /**
     * This method will flag validated identifications
     */
    public void validateIdentifications() {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();

        double proteinThreshold = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getScoreLimit();
        for (String proteinKey : identification.getProteinIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            if (psParameter.getProteinProbabilityScore() <= proteinThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }

        double peptideThreshold;
        for (String peptideKey : identification.getPeptideIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            peptideThreshold = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSecificMapKey())).getTargetDecoyResults().getScoreLimit();
            if (psParameter.getPeptideProbabilityScore() <= peptideThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }

        double psmThreshold;
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
            psmThreshold = psmMap.getTargetDecoyMap(psmMap.getCorrectedKey(psParameter.getSecificMapKey())).getTargetDecoyResults().getScoreLimit();
            if (psParameter.getPsmProbabilityScore() <= psmThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }
    }

    /**
     * When two different sequences result in the same score for a given search engine, this method will retain the peptide belonging to the protein leading to the most spectra.
     * This method is typically useful for similar peptides issues.
     * 
     * @param waitingDialog waiting dialog used to display the progress
     */
    private void setFirstHit(WaitingDialog waitingDialog, IdFilter idFilter) throws Exception {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        int max = identification.getSpectrumIdentification().size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);

        ArrayList<String> conflictingPSMs = new ArrayList<String>();
        HashMap<String, Integer> spectrumCounting = new HashMap<String, Integer>();
        ArrayList<PeptideAssumption> assumptions;
        Precursor precursor;
        boolean conflict;
        SpectrumMatch spectrumMatch;
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            conflict = false;
            for (int se : spectrumMatch.getAdvocates()) {
                assumptions = spectrumMatch.getAllAssumptions(se).get(spectrumMatch.getFirstHit(se).getEValue());
                if (assumptions.size() > 1) {
                    precursor = spectrumFactory.getPrecursor(spectrumKey);
                    for (PeptideAssumption peptideAssumption : assumptions) {
                        if (idFilter.validateId(peptideAssumption, precursor.getMz())) {
                            if (!peptideAssumption.getPeptide().getSequence().equals(spectrumMatch.getFirstHit(se).getPeptide().getSequence())) {
                                conflict = true;
                            }
                            for (String accession : peptideAssumption.getPeptide().getParentProteins()) {
                                if (!spectrumCounting.containsKey(accession)) {
                                    spectrumCounting.put(accession, 0);
                                }
                                spectrumCounting.put(accession, spectrumCounting.get(accession) + 1);
                            }
                        }
                    }
                }
            }
            if (conflict) {
                conflictingPSMs.add(spectrumMatch.getKey());
            } else {
                waitingDialog.increaseSecondaryProgressValue();
            }
        }
        SpectrumMatch conflictingPSM;
        PeptideAssumption bestAssumption;
        int maxCount;
        for (String conflictKey : conflictingPSMs) {
            conflictingPSM = identification.getSpectrumMatch(conflictKey);
            maxCount = 0;
            precursor = spectrumFactory.getPrecursor(conflictKey);
            for (int se : conflictingPSM.getAdvocates()) {
                bestAssumption = conflictingPSM.getFirstHit(se);
                for (String accession : bestAssumption.getPeptide().getParentProteins()) {
                    if (spectrumCounting.get(accession) > maxCount) {
                        maxCount = spectrumCounting.get(accession);
                    }
                }
                for (PeptideAssumption peptideAssumption : conflictingPSM.getAllAssumptions(se).get(conflictingPSM.getFirstHit(se).getEValue())) {
                    if (!peptideAssumption.getPeptide().getSequence().equals(conflictingPSM.getFirstHit(se).getPeptide().getSequence())) {
                        if (idFilter.validateId(peptideAssumption, precursor.getMz())) {
                            for (String accession : peptideAssumption.getPeptide().getParentProteins()) {
                                if (spectrumCounting.get(accession) > maxCount) {
                                    bestAssumption = peptideAssumption;
                                    maxCount = spectrumCounting.get(accession);
                                }
                            }
                            conflictingPSM.setFirstHit(se, bestAssumption);
                            identification.setMatchChanged(conflictingPSM);
                        }
                    }
                }
            }
            waitingDialog.increaseSecondaryProgressValue();
        }

        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Fills the psm specific map
     *
     * @param inputMap       The input map
     * @param waitingDialog waiting dialog used to display the progress
     */
    private void fillPsmMap(InputMap inputMap, WaitingDialog waitingDialog) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        int max = identification.getSpectrumIdentification().size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);
        HashMap<String, Double> identifications;
        HashMap<Double, PeptideAssumption> peptideAssumptions;
        PSParameter psParameter;
        PeptideAssumption peptideAssumption, bestAssumption;
        SpectrumMatch spectrumMatch;
        ArrayList<Integer> charges = new ArrayList<Integer>();
        int currentCharge;
        double precursorMz, error, maxErrorPpm = 0, maxErrorDa = 0;
        if (inputMap.isMultipleSearchEngines()) {
            for (String spectrumKey : identification.getSpectrumIdentification()) {
                psParameter = new PSParameter();
                identifications = new HashMap<String, Double>();
                peptideAssumptions = new HashMap<Double, PeptideAssumption>();
                String id;
                double p, pScore = 1;
                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                    p = psParameter.getSearchEngineProbability();
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
                bestAssumption = peptideAssumptions.get(pMin);
                spectrumMatch.setBestAssumption(bestAssumption);
                psParameter.setSpectrumProbabilityScore(pScore);
                psParameter.setSecificMapKey(psmMap.getKey(spectrumMatch) + "");
                identification.addMatchParameter(spectrumKey, psParameter);
                identification.setMatchChanged(spectrumMatch);
                psmMap.addPoint(pScore, spectrumMatch);
                waitingDialog.increaseSecondaryProgressValue();
                currentCharge = bestAssumption.getIdentificationCharge().value;
                if (!charges.contains(currentCharge)) {
                    charges.add(currentCharge);
                }
                precursorMz = spectrumFactory.getPrecursor(spectrumKey, false).getMz();
                error = bestAssumption.getDeltaMass(precursorMz, true);
                if (error > maxErrorPpm) {
                    maxErrorPpm = error;
                }
                error = bestAssumption.getDeltaMass(precursorMz, false);
                if (error > maxErrorDa) {
                    maxErrorDa = error;
                }
            }
        } else {
            double eValue;
            for (String spectrumKey : identification.getSpectrumIdentification()) {
                psParameter = new PSParameter();
                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    eValue = peptideAssumption.getEValue();
                    psParameter.setSpectrumProbabilityScore(eValue);
                    spectrumMatch.setBestAssumption(peptideAssumption);
                    identification.setMatchChanged(spectrumMatch);
                    psmMap.addPoint(eValue, spectrumMatch);
                }
                psParameter.setSecificMapKey(psmMap.getKey(spectrumMatch) + "");
                identification.addMatchParameter(spectrumKey, psParameter);
                waitingDialog.increaseSecondaryProgressValue();
                currentCharge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                if (!charges.contains(currentCharge)) {
                    charges.add(currentCharge);
                }
                precursorMz = spectrumFactory.getPrecursor(spectrumKey, false).getMz();
                error = spectrumMatch.getBestAssumption().getDeltaMass(precursorMz, true);
                if (error > maxErrorPpm) {
                    maxErrorPpm = error;
                }
                error = spectrumMatch.getBestAssumption().getDeltaMass(precursorMz, false);
                if (error > maxErrorDa) {
                    maxErrorDa = error;
                }
            }
        }
        metrics.setFoundCharges(charges);
        metrics.setMaxPrecursorErrorDa(maxErrorDa);
        metrics.setMaxPrecursorErrorPpm(maxErrorPpm);
        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Attaches the spectrum posterior error probabilities to the peptide assumptions
     * 
     * @param inputMap 
     * @param waitingDialog a reference to the waiting dialog
     */
    private void attachAssumptionsProbabilities(InputMap inputMap, WaitingDialog waitingDialog) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        ArrayList<Double> eValues;
        double previousP, newP;
        SpectrumMatch spectrumMatch;

        int max = identification.getSpectrumIdentification().size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);

        for (String spectrumKey : identification.getSpectrumIdentification()) {

            waitingDialog.increaseSecondaryProgressValue();

            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            for (int searchEngine : spectrumMatch.getAdvocates()) {
                eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(searchEngine).keySet());
                Collections.sort(eValues);
                previousP = 0;
                for (double eValue : eValues) {
                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(searchEngine).get(eValue)) {
                        psParameter = new PSParameter();
                        newP = inputMap.getProbability(searchEngine, eValue);
                        if (newP > previousP) {
                            psParameter.setSearchEngineProbability(newP);
                            previousP = newP;
                        } else {
                            psParameter.setSearchEngineProbability(previousP);
                        }
                        peptideAssumption.addUrParam(psParameter);
                    }
                }
            }
            identification.setMatchChanged(spectrumMatch);
        }

        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum matches
     */
    private void attachSpectrumProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
            psParameter.setPsmProbability(psmMap.getProbability(psParameter.getSecificMapKey(), psParameter.getPsmProbabilityScore()));
        }
    }

    /**
     * Attaches scores to possible PTM locations to spectrum matches 
     * 
     * @param inspectedSpectra
     * @param waitingDialog a reference to the waiting dialog
     * @param searchParameters 
     * @param annotationPreferences 
     * @throws Exception  
     */
    public void scorePSMPTMs(ArrayList<String> inspectedSpectra, WaitingDialog waitingDialog, SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        SpectrumMatch spectrumMatch;

        int max = inspectedSpectra.size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);

        for (String spectrumKey : inspectedSpectra) {
            waitingDialog.increaseSecondaryProgressValue();
            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            scorePTMs(spectrumMatch, searchParameters, annotationPreferences);
        }

        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Scores the PTMs of all validated peptides.
     * @param waitingDialog         waiting dialog which provides feedback to the user
     * @param searchParameters      the search parameters
     * @param annotationPreferences the annotation preferences
     * @throws Exception    exception thrown whenever a problem occurred while deserializing a match
     */
    public void scorePeptidePtms(WaitingDialog waitingDialog, SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PeptideMatch peptideMatch;

        int max = identification.getPeptideIdentification().size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);

        for (String peptideKey : identification.getPeptideIdentification()) {
            peptideMatch = identification.getPeptideMatch(peptideKey);
            scorePTMs(peptideMatch, searchParameters, annotationPreferences);
            waitingDialog.increaseSecondaryProgressValue();
        }

        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Scores the PTMs of all validated proteins.
     * @param waitingDialog         waiting dialog which provides feedback to the user
     * @param searchParameters      the search parameters
     * @param annotationPreferences the annotation preferences
     * @throws Exception    exception thrown whenever a problem occurred while deserializing a match
     */
    public void scoreProteinPtms(WaitingDialog waitingDialog, SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        ProteinMatch proteinMatch;

        int max = identification.getProteinIdentification().size();
        waitingDialog.setSecondaryProgressDialogIntermediate(false);
        waitingDialog.setMaxSecondaryProgressValue(max);

        for (String proteinKey : identification.getProteinIdentification()) {
            proteinMatch = identification.getProteinMatch(proteinKey);
            scorePTMs(proteinMatch, searchParameters, annotationPreferences, false);
            waitingDialog.increaseSecondaryProgressValue();
        }

        waitingDialog.setSecondaryProgressDialogIntermediate(true);
    }

    /**
     * Scores ptms in a protein match
     * @param proteinMatch          the protein match
     * @param searchParameters      the search parameters
     * @param annotationPreferences the annotation preferences
     * @param scorePeptides         if true peptide scores will be recalculated
     * @throws Exception exception thrown whenever an error occurred while deserilalizing a match
     */
    public void scorePTMs(ProteinMatch proteinMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences, boolean scorePeptides) throws Exception {
        PSPtmScores peptideScores = new PSPtmScores();
        PSPtmScores proteinScores = new PSPtmScores();
        PtmScoring ptmScoring;
        PSParameter psParameter = new PSParameter();
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PeptideMatch peptideMath;
        String peptideSequence, proteinSequence = null;
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            if (psParameter.isValidated() && Peptide.isModified(peptideKey)) {
                peptideMath = identification.getPeptideMatch(peptideKey);
                peptideSequence = Peptide.getSequence(peptideKey);
                if (peptideMath.getUrParam(proteinScores) == null || scorePeptides) {
                    scorePTMs(peptideMath, searchParameters, annotationPreferences);
                }
                peptideScores = (PSPtmScores) peptideMath.getUrParam(proteinScores);
                if (peptideScores != null) {
                    for (String modification : peptideScores.getScoredPTMs()) {
                        if (proteinSequence == null) {
                            proteinSequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
                        }
                        ptmScoring = peptideScores.getPtmScoring(modification);
                        for (int pos : getProteinModificationIndexes(proteinSequence, peptideSequence, ptmScoring.getPtmLocation())) {
                            proteinScores.addMainModificationSite(modification, pos);
                        }
                        for (int pos : getProteinModificationIndexes(proteinSequence, peptideSequence, ptmScoring.getSecondaryPtmLocations())) {
                            proteinScores.addSecondaryModificationSite(modification, pos);
                        }
                    }
                }
            }
        }
        proteinMatch.addUrParam(proteinScores);
        identification.setMatchChanged(proteinMatch);
    }

    /**
     * Returns the protein indexes of a modification found in a peptide
     * @param proteinSequence   The protein sequence
     * @param peptideSequence   The peptide sequence
     * @param positionInPeptide The position(s) of the modification in the peptide sequence
     * @return the possible modification sites in a protein
     */
    public static ArrayList<Integer> getProteinModificationIndexes(String proteinSequence, String peptideSequence, ArrayList<Integer> positionInPeptide) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        String tempSequence = proteinSequence;

        while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
            int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
            for (int pos : positionInPeptide) {
                result.add(peptideTempStart + pos - 2);
            }
            tempSequence = proteinSequence.substring(0, peptideTempStart);
        }
        return result;
    }

    /**
     * Scores the PTMs for a peptide match
     * 
     * @param peptideMatch the peptide match of interest
     * @param searchParameters 
     * @param annotationPreferences 
     * @throws Exception   exception thrown whenever an error occurred while deserializing a match
     */
    public void scorePTMs(PeptideMatch peptideMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSPtmScores psmScores, peptideScores = new PSPtmScores();
        PtmScoring spectrumScoring;
        PSParameter psParameter = new PSParameter();
        ArrayList<String> variableModifications = new ArrayList<String>();
        SpectrumMatch spectrumMatch;
        PTM ptm;
        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                if (ptm.getType() == PTM.MODAA
                        && !variableModifications.contains(modificationMatch.getTheoreticPtm())) {
                    variableModifications.add(modificationMatch.getTheoreticPtm());
                }
            }
        }
        if (variableModifications.size() > 0) {
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
                if (psParameter.isValidated()) {
                    spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    scorePTMs(spectrumMatch, searchParameters, annotationPreferences);
                    for (String modification : variableModifications) {
                        if (!peptideScores.containsPtm(modification)) {
                            peptideScores.addPtmScoring(modification, new PtmScoring(modification));
                        }
                        psmScores = (PSPtmScores) spectrumMatch.getUrParam(peptideScores);
                        if (psmScores != null) {
                            spectrumScoring = psmScores.getPtmScoring(modification);
                            if (spectrumScoring != null) {
                                peptideScores.getPtmScoring(modification).addAll(spectrumScoring);
                            }
                        }
                    }
                }
            }
            for (String modification : variableModifications) {
                PtmScoring scoring = peptideScores.getPtmScoring(modification);
                if (scoring != null) {
                    for (int mainLocation : scoring.getPtmLocation()) {
                        peptideScores.addMainModificationSite(modification, mainLocation);
                    }
                    for (int secondaryLocation : scoring.getSecondaryPtmLocations()) {
                        peptideScores.addSecondaryModificationSite(modification, secondaryLocation);
                    }
                }
            }
            peptideMatch.addUrParam(peptideScores);
            identification.setMatchChanged(peptideMatch);
        }
    }

    /**
     * Scores PTM locations for a desired spectrumMatch
     * 
     * @param spectrumMatch The spectrum match of interest
     * @param searchParameters 
     * @param annotationPreferences 
     * @throws Exception    exception thrown whenever an error occurred while reading/writing the an identification match
     */
    public void scorePTMs(SpectrumMatch spectrumMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws Exception {
        attachDeltaScore(spectrumMatch);
        attachAScore(spectrumMatch, searchParameters, annotationPreferences);
        PSPtmScores ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
        if (ptmScores != null) {
            PtmScoring ptmScoring;
            for (String modification : ptmScores.getScoredPTMs()) {
                ptmScoring = ptmScores.getPtmScoring(modification);
                String bestAKey = ptmScoring.getBestAScoreLocations();
                String bestDKey = ptmScoring.getBestDeltaScoreLocations();
                String retainedKey;
                int confidence = PtmScoring.RANDOM;
                if (bestAKey != null) {
                    retainedKey = bestAKey;
                    if (ptmScoring.getAScore(bestAKey) <= 50) {
                        if (bestAKey.equals(bestDKey)) {
                            confidence = PtmScoring.DOUBTFUL;
                            if (ptmScoring.getDeltaScore(bestDKey) > 50) {
                                confidence = PtmScoring.CONFIDENT;
                            }
                        }
                    } else if (bestAKey.equals(bestDKey)) {
                        confidence = PtmScoring.VERY_CONFIDENT;
                    } else {
                        confidence = PtmScoring.CONFIDENT;
                    }

                } else {
                    retainedKey = bestDKey;
                    if (ptmScoring.getDeltaScore(bestDKey) > 50) {
                        confidence = PtmScoring.CONFIDENT;
                    } else {
                        confidence = PtmScoring.DOUBTFUL;
                    }
                }
                if (retainedKey != null) {
                    ptmScoring.setPtmSite(retainedKey, confidence);
                } else {
                    int debug = 1;
                }
            }
        }
    }

    /**
     * Scores the PTM locations using the delta score
     * @param spectrumMatch the spectrum match of interest
     * @throws Exception    exception thrown whenever an error occurred while reading/writing the an identification match
     */
    private void attachDeltaScore(SpectrumMatch spectrumMatch) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        PSParameter psParameter = new PSParameter();
        double p1, p2;
        String mainSequence, modificationName;
        ArrayList<String> modifications;
        HashMap<String, ArrayList<Integer>> modificationProfiles = new HashMap<String, ArrayList<Integer>>();
        PSPtmScores ptmScores;
        PtmScoring ptmScoring;
        ptmScores = new PSPtmScores();
        if (spectrumMatch.getUrParam(ptmScores) != null) {
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
        }
        psParameter = (PSParameter) spectrumMatch.getBestAssumption().getUrParam(psParameter);
        p1 = psParameter.getSearchEngineProbability();
        PTM ptm;
        if (p1 < 1) {
            mainSequence = spectrumMatch.getBestAssumption().getPeptide().getSequence();
            p2 = 1;
            modifications = new ArrayList<String>();
            for (ModificationMatch modificationMatch : spectrumMatch.getBestAssumption().getPeptide().getModificationMatches()) {
                if (modificationMatch.isVariable()) {
                    ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                    if (ptm.getType() == PTM.MODAA) {
                        modificationName = modificationMatch.getTheoreticPtm();
                        if (!modifications.contains(modificationName)) {
                            modifications.add(modificationName);
                            modificationProfiles.put(modificationName, new ArrayList<Integer>());
                        }
                        modificationProfiles.get(modificationName).add(modificationMatch.getModificationSite());
                    }
                }
            }
            if (!modifications.isEmpty()) {
                for (String mod : modifications) {
                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                        if (peptideAssumption.getRank() > 1
                                && peptideAssumption.getPeptide().getSequence().equals(mainSequence)) {
                            boolean newLocation = false;
                            for (ModificationMatch modMatch : peptideAssumption.getPeptide().getModificationMatches()) {
                                if (modMatch.getTheoreticPtm().equals(mod)
                                        && !modificationProfiles.get(mod).contains(modMatch.getModificationSite())) {
                                    newLocation = true;
                                    break;
                                }
                            }
                            if (newLocation) {
                                psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                if (psParameter.getSearchEngineProbability() < p2) {
                                    p2 = psParameter.getSearchEngineProbability();
                                }
                            }
                        }
                    }
                    ptmScoring = ptmScores.getPtmScoring(mod);
                    if (ptmScoring == null) {
                        ptmScoring = new PtmScoring(mod);
                    }
                    ptmScoring.addDeltaScore(modificationProfiles.get(mod), (p2 - p1) * 100);
                    ptmScores.addPtmScoring(mod, ptmScoring);
                }
                spectrumMatch.addUrParam(ptmScores);
                identification.setMatchChanged(spectrumMatch);
            }
        }
    }

    private void attachAScore(SpectrumMatch spectrumMatch, SearchParameters searchParameters, AnnotationPreferences annotationPreferences) throws Exception {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        PSParameter psParameter = new PSParameter();
        double p1;
        String modificationName;
        HashMap<String, PTM> modifications;
        PSPtmScores ptmScores;
        PtmScoring ptmScoring;
        ptmScores = new PSPtmScores();
        if (spectrumMatch.getUrParam(ptmScores) != null) {
            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(ptmScores);
        }
        psParameter = (PSParameter) spectrumMatch.getBestAssumption().getUrParam(psParameter);
        p1 = psParameter.getSearchEngineProbability();
        PTM ptm;
        if (p1 < 1) {
            modifications = new HashMap<String, PTM>();
            HashMap<String, Integer> nMod = new HashMap<String, Integer>();
            for (ModificationMatch modificationMatch : spectrumMatch.getBestAssumption().getPeptide().getModificationMatches()) {
                if (modificationMatch.isVariable()) {
                    ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                    if (ptm.getType() == PTM.MODAA) {
                        modificationName = modificationMatch.getTheoreticPtm();
                        if (!modifications.keySet().contains(modificationName)) {
                            modifications.put(modificationName, ptm);
                            nMod.put(modificationName, 1);
                        } else {
                            nMod.put(modificationName, nMod.get(modificationName) + 1);
                        }
                    }
                }
            }
            if (!modifications.isEmpty()) {
                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey());
                annotationPreferences.setCurrentSettings(spectrumMatch.getBestAssumption().getPeptide(), spectrumMatch.getBestAssumption().getIdentificationCharge().value, true);
                for (String mod : modifications.keySet()) {
                    if (nMod.get(mod) == 1) {
                        HashMap<ArrayList<Integer>, Double> aScores = PTMLocationScores.getAScore(spectrumMatch.getBestAssumption().getPeptide(),
                                modifications.get(mod), nMod.get(mod), spectrum, annotationPreferences.getIonTypes(),
                                annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                                spectrumMatch.getBestAssumption().getIdentificationCharge().value,
                                searchParameters.getFragmentIonAccuracy());
                        ptmScoring = ptmScores.getPtmScoring(mod);
                        if (ptmScoring == null) {
                            ptmScoring = new PtmScoring(mod);
                        }
                        for (ArrayList<Integer> modificationProfile : aScores.keySet()) {
                            ptmScoring.addAScore(modificationProfile, aScores.get(modificationProfile));
                        }
                        ptmScores.addPtmScoring(mod, ptmScoring);
                    }
                }
                spectrumMatch.addUrParam(ptmScores);
                identification.setMatchChanged(spectrumMatch);
            }
        }
    }

    /**
     * Fills the peptide specific map
     */
    private void fillPeptideMaps() throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        PeptideMatch peptideMatch;
        for (String peptideKey : identification.getPeptideIdentification()) {
            probaScore = 1;
            peptideMatch = identification.getPeptideMatch(peptideKey);
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
                probaScore = probaScore * psParameter.getPsmProbability();
            }
            psParameter = new PSParameter();
            psParameter.setPeptideProbabilityScore(probaScore);
            psParameter.setSecificMapKey(peptideMap.getKey(peptideMatch));
            identification.addMatchParameter(peptideKey, psParameter);
            peptideMap.addPoint(probaScore, peptideMatch);
        }
    }

    /**
     * Attaches the peptide posterior error probabilities to the peptide matches
     */
    private void attachPeptideProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (String peptideKey : identification.getPeptideIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            psParameter.setPeptideProbability(peptideMap.getProbability(psParameter.getSecificMapKey(), psParameter.getPeptideProbabilityScore()));
        }
    }

    /**
     * Fills the protein map
     * 
     * @param waitingDialog a reference to the waiting dialog
     */
    private void fillProteinMap(WaitingDialog waitingDialog) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        ProteinMatch proteinMatch;

        int max = identification.getProteinIdentification().size();

        if (waitingDialog != null) {
            waitingDialog.setSecondaryProgressDialogIntermediate(false);
            waitingDialog.setMaxSecondaryProgressValue(max);
        }

        for (String proteinKey : identification.getProteinIdentification()) {

            if (waitingDialog != null) {
                waitingDialog.increaseSecondaryProgressValue();
            }

            probaScore = 1;
            proteinMatch = identification.getProteinMatch(proteinKey);
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                probaScore = probaScore * psParameter.getPeptideProbability();
            }
            psParameter = new PSParameter();
            psParameter.setProteinProbabilityScore(probaScore);
            identification.addMatchParameter(proteinKey, psParameter);
            proteinMap.addPoint(probaScore, proteinMatch.isDecoy());
        }

        if (waitingDialog != null) {
            waitingDialog.setSecondaryProgressDialogIntermediate(true);
        }
    }

    /**
     * Attaches the protein posterior error probability to the protein matches
     */
    private void attachProteinProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        double proteinProbability;
        for (String proteinKey : identification.getProteinIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            proteinProbability = proteinMap.getProbability(psParameter.getProteinProbabilityScore());
            psParameter.setProteinProbability(proteinProbability);
        }
    }

    /**
     * Solves protein inference issues when possible.
     * @throws Exception    exception thrown whenever it is attempted to attach two different spectrum matches to the same spectrum from the same search engine.
     */
    private void cleanProteinGroups(WaitingDialog waitingDialog) throws IOException, IllegalArgumentException {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        boolean better;
        ProteinMatch proteinShared, proteinUnique;
        double sharedProteinProbabilityScore, uniqueProteinProbabilityScore;
        ArrayList<String> toRemove = new ArrayList<String>();

        int max = 2 * identification.getProteinIdentification().size();

        if (waitingDialog != null) {
            waitingDialog.setSecondaryProgressDialogIntermediate(false);
            waitingDialog.setMaxSecondaryProgressValue(max);
        }

        for (String proteinSharedKey : identification.getProteinIdentification()) {

            if (ProteinMatch.getNProteins(proteinSharedKey) > 1) {
                psParameter = (PSParameter) identification.getMatchParameter(proteinSharedKey, psParameter);
                sharedProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                if (sharedProteinProbabilityScore < 1) {
                    better = false;
                    for (String proteinUniqueKey : identification.getProteinIdentification()) {
                        if (ProteinMatch.contains(proteinSharedKey, proteinUniqueKey)) {
                            psParameter = (PSParameter) identification.getMatchParameter(proteinUniqueKey, psParameter);
                            uniqueProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                            proteinUnique = identification.getProteinMatch(proteinUniqueKey);
                            proteinShared = identification.getProteinMatch(proteinSharedKey);
                            for (String sharedPeptideKey : proteinShared.getPeptideMatches()) {
                                proteinUnique.addPeptideMatch(sharedPeptideKey);
                            }
                            identification.setMatchChanged(proteinUnique);
                            if (uniqueProteinProbabilityScore <= sharedProteinProbabilityScore) {
                                better = true;
                            }
                        }
                    }
                    if (better) {
                        toRemove.add(proteinSharedKey);
                    } else if (waitingDialog != null) {
                        waitingDialog.increaseSecondaryProgressValue();
                    }
                }
            }
        }


        for (String proteinKey : toRemove) {
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            proteinMap.removePoint(psParameter.getProteinProbabilityScore(), ProteinMatch.isDecoy(proteinKey));
            identification.removeMatch(proteinKey);
            if (waitingDialog != null) {
                waitingDialog.increaseSecondaryProgressValue();
            }
        }

        int nSolved = toRemove.size();
        int nGroups = 0;
        int nLeft = 0;
        String mainKey = null;
        boolean similarityFound, allSimilar;
        ArrayList<String> primaryDescription, secondaryDescription, accessions;

        for (String proteinKey : identification.getProteinIdentification()) {
            accessions = new ArrayList<String>(Arrays.asList(ProteinMatch.getAccessions(proteinKey)));
            Collections.sort(accessions);
            mainKey = accessions.get(0);
            if (accessions.size() > 1) {
                similarityFound = false;
                allSimilar = false;
                psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
                for (int i = 0; i < accessions.size() - 1; i++) {
                    primaryDescription = parseDescription(accessions.get(i));
                    for (int j = i + 1; j < accessions.size(); j++) {
                        secondaryDescription = parseDescription(accessions.get(j));
                        if (getSimilarity(primaryDescription, secondaryDescription)) {
                            similarityFound = true;
                            mainKey = accessions.get(i);
                            break;
                        }
                    }
                    if (similarityFound) {
                        break;
                    }
                }
                if (similarityFound) {
                    allSimilar = true;
                    for (String key : accessions) {
                        if (!mainKey.equals(key)) {
                            primaryDescription = parseDescription(mainKey);
                            secondaryDescription = parseDescription(key);
                            if (!getSimilarity(primaryDescription, secondaryDescription)) {
                                allSimilar = false;
                                break;
                            }
                        }
                    }
                }
                if (!similarityFound) {
                    psParameter.setGroupClass(PSParameter.UNRELATED);
                    nGroups++;
                    nLeft++;

                    ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                        psParameter.setGroupClass(PSParameter.UNRELATED);
                    }

                } else if (!allSimilar) {
                    psParameter.setGroupClass(PSParameter.ISOFORMS_UNRELATED);
                    nGroups++;
                    nSolved++;

                    ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                        psParameter.setGroupClass(PSParameter.ISOFORMS_UNRELATED);
                    }

                } else {
                    psParameter.setGroupClass(PSParameter.ISOFORMS);
                    nGroups++;
                    nSolved++;

                    ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                    String mainMatch = proteinMatch.getMainMatch();
                    PeptideMatch peptideMatch;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        boolean unrelated = false;
                        for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                            if (!proteinKey.contains(protein)) {
                                primaryDescription = parseDescription(mainMatch);
                                secondaryDescription = parseDescription(protein);
                                if (!getSimilarity(primaryDescription, secondaryDescription)) {
                                    unrelated = true;
                                    break;
                                }
                            }
                        }
                        if (unrelated) {
                            psParameter.setGroupClass(PSParameter.ISOFORMS_UNRELATED);
                        } else {
                            psParameter.setGroupClass(PSParameter.ISOFORMS);
                        }
                    }
                }
            } else {
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                String mainMatch = proteinMatch.getMainMatch();
                PeptideMatch peptideMatch;
                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                    peptideMatch = identification.getPeptideMatch(peptideKey);
                    boolean unrelated = false;
                    boolean otherProtein = false;
                    for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                        if (!proteinKey.contains(protein)) {
                            otherProtein = true;
                            primaryDescription = parseDescription(mainMatch);
                            secondaryDescription = parseDescription(protein);
                            if (primaryDescription == null || secondaryDescription == null || !getSimilarity(primaryDescription, secondaryDescription)) {
                                unrelated = true;
                                break;
                            }
                        }
                    }
                    if (otherProtein) {
                        psParameter.setGroupClass(PSParameter.ISOFORMS);
                    }
                    if (unrelated) {
                        psParameter.setGroupClass(PSParameter.UNRELATED);
                    }
                }
            }
            if (ProteinMatch.getNProteins(proteinKey) > 1) {
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                if (!proteinMatch.getMainMatch().equals(mainKey)) {
                    proteinMatch.setMainMatch(mainKey);
                    identification.setMatchChanged(proteinMatch);
                }
            }
            if (waitingDialog != null) {
                waitingDialog.increaseSecondaryProgressValue();
            }
        }

        if (waitingDialog != null) {
            waitingDialog.setSecondaryProgressDialogIntermediate(true);
            waitingDialog.appendReport(nSolved + " conflicts resolved. " + nGroups + " protein groups remaining (" + nLeft + " suspicious).");
        }
    }

    /**
     * Parses a protein description retaining only words longer than 3 characters
     * @param proteinAccession the accession of the inspected protein
     * @return description words longer than 3 characters
     */
    private ArrayList<String> parseDescription(String proteinAccession) throws IOException, IllegalArgumentException {
        String description = sequenceFactory.getHeader(proteinAccession).getDescription();

        if (description == null) {
            return new ArrayList<String>();
        }

        ArrayList<String> result = new ArrayList<String>();
        for (String component : description.split(" ")) {
            if (component.length() > 3) {
                result.add(component);
            }
        }
        return result;
    }

    /**
     * Simplistic method comparing protein descriptions. Returns true if both descriptions are of same length and present more than half similar words.
     * @param primaryDescription    The parsed description of the first protein
     * @param secondaryDescription  The parsed description of the second protein
     * @return  a boolean indicating whether the descriptions are similar
     */
    private boolean getSimilarity(ArrayList<String> primaryDescription, ArrayList<String> secondaryDescription) {
        if (primaryDescription.size() == secondaryDescription.size()) {
            int nMatch = 0;
            for (int i = 0; i < primaryDescription.size(); i++) {
                if (primaryDescription.get(i).equals(secondaryDescription.get(i))) {
                    nMatch++;
                }
            }
            if (nMatch >= primaryDescription.size() / 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the metrics picked-up while loading the files.
     * 
     * @return the metrics picked-up while loading the files
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Replaces the needed PTMs by PeptideShaker PTMs in the factory.
     * 
     * @param searchParameters the search parameters containing the modification profile to use
     */
    public static void setPeptideShakerPTMs(SearchParameters searchParameters) {
        ArrayList<String> residues, utilitiesNames;
        PTMFactory ptmFactory = PTMFactory.getInstance();
        PTM sePtm, newPTM;
        for (String peptideShakerName : searchParameters.getModificationProfile().getPeptideShakerNames()) {
            residues = new ArrayList<String>();
            utilitiesNames = new ArrayList<String>();
            int modType = -1;
            double mass = -1;
            for (String utilitiesName : searchParameters.getModificationProfile().getUtilitiesNames()) {
                if (peptideShakerName.equals(searchParameters.getModificationProfile().getPeptideShakerName(utilitiesName))) {
                    sePtm = ptmFactory.getPTM(utilitiesName);
                    for (String aa : sePtm.getResidues()) {
                        if (!residues.contains(aa)) {
                            residues.add(aa);
                        }
                    }
                    if (modType == -1) {
                        modType = sePtm.getType();
                    } else if (sePtm.getType() != modType) {
                        modType = PTM.MODAA; // case difficult to handle so use the default AA option
                    }
                    mass = sePtm.getMass();
                    utilitiesNames.add(utilitiesName);
                }
            }
            for (String utilitiesName : utilitiesNames) {
                newPTM = new PTM(modType, peptideShakerName, searchParameters.getModificationProfile().getShortName(peptideShakerName), mass, residues);
                ptmFactory.replacePTM(utilitiesName, newPTM);
            }
        }
    }
}
