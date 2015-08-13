package eu.isas.peptideshaker;

import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.maps.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.maps.PsmPTMMap;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.io.ConfigurationFile;
import com.compomics.util.memory.MemoryConsumptionStatus;
import eu.isas.peptideshaker.fileimport.FileImporter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.messages.FeedBack;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.parameters.PSParameter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.PsmScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.Duration;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.protein_inference.ProteinInference;
import eu.isas.peptideshaker.ptm.PtmScorer;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.*;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import eu.isas.peptideshaker.scoring.psm_scoring.PsmScorer;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.peptideshaker.validation.MatchesValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class will be responsible for the identification import and the
 * associated calculations.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShaker {

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
     * The validator which will take care of the matches validation
     */
    private MatchesValidator matchesValidator;
    /**
     * The PTM scorer responsible for scoring PTM localization.
     */
    private PtmScorer ptmScorer;
    /**
     * The id importer will import and process the identifications.
     */
    private FileImporter fileImporter = null;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The XML file containing the enzymes.
     */
    public static final String ENZYME_FILE = "resources/conf/peptideshaker_enzymes.xml";
    /**
     * User preferences file.
     */
    private static String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences.cpf";
    /**
     * Default PeptideShaker modifications.
     */
    public static final String PEPTIDESHAKER_CONFIGURATION_FILE = "PeptideShaker_configuration.txt";
    /**
     * The location of the folder used for serialization of matches.
     */
    private static String SERIALIZATION_DIRECTORY = "matches";
    /**
     * Folder where the data files are stored by default. Should be the same as
     * in SearchGUI.
     */
    public static String DATA_DIRECTORY = "data";
    /**
     * The parent directory of the serialization directory. An empty string if
     * not set.
     */
    private static String SERIALIZATION_PARENT_DIRECTORY = "resources";
    /**
     * The name of the serialized experiment
     */
    public final static String experimentObjectName = "experiment";
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Metrics to be picked when loading the identification.
     */
    private Metrics metrics = new Metrics();
    /**
     * An identification features generator which will compute figures on the
     * identification matches and keep some of them in memory.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * A cache where the objects will be saved.
     */
    private ObjectsCache objectsCache;
    /**
     * List of warnings collected while working on the data.
     */
    private HashMap<String, FeedBack> warnings = new HashMap<String, FeedBack>();
    /**
     * If true, a warning will be displayed when encountering memory issues.
     */
    private boolean memoryWarning = true;
    /**
     * Object used to monitor the duration of the project creation.
     */
    private Duration projectCreationDuration;

    /**
     * Empty constructor for instantiation purposes.
     */
    private PeptideShaker() {
    }

    /**
     * Constructor without mass specification. Calculation will be done on new
     * maps which will be retrieved as compomics utilities parameters.
     *
     * @param experiment the experiment conducted
     * @param sample the sample analyzed
     * @param replicateNumber the replicate number
     */
    public PeptideShaker(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        PsmSpecificMap psmMap = new PsmSpecificMap();
        PeptideSpecificMap peptideMap = new PeptideSpecificMap();
        ProteinMap proteinMap = new ProteinMap();
        matchesValidator = new MatchesValidator(psmMap, peptideMap, proteinMap);
        PsmPTMMap psmPTMMap = new PsmPTMMap();
        ptmScorer = new PtmScorer(psmPTMMap);
    }

    /**
     * Constructor with map specifications.
     *
     * @param experiment the experiment conducted
     * @param sample the sample analyzed
     * @param replicateNumber the replicate number
     * @param psMaps the peptide shaker maps
     */
    public PeptideShaker(MsExperiment experiment, Sample sample, int replicateNumber, PSMaps psMaps) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        matchesValidator = new MatchesValidator(psMaps.getPsmSpecificMap(), psMaps.getPeptideSpecificMap(), psMaps.getProteinMap());
        ptmScorer = new PtmScorer(psMaps.getPsmPTMMap());
    }

    /**
     * Method used to import identification from identification result files.
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param idFiles the files to import
     * @param spectrumFiles the corresponding spectra (can be empty: spectra
     * will not be loaded)
     * @param shotgunProtocol information about the protocol used
     * @param identificationParameters identification parameters
     * @param projectDetails the project details
     * @param processingPreferences the initial processing preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param backgroundThread boolean indicating whether the import should be
     * done in a background thread (GUI mode) or in the current thread (command
     * line mode).
     */
    public void importFiles(WaitingHandler waitingHandler, ArrayList<File> idFiles, ArrayList<File> spectrumFiles,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ProjectDetails projectDetails,
            ProcessingPreferences processingPreferences, SpectrumCountingPreferences spectrumCountingPreferences, boolean backgroundThread) {

        projectCreationDuration = new Duration();
        projectCreationDuration.start();
        
        waitingHandler.appendReport("Import process for " + experiment.getReference() + " (Sample: " + sample.getReference() + ", Replicate: " + replicateNumber + ")", true, true);
        waitingHandler.appendReportEndLine();

        objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);

        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, new Ms2Identification(getIdentificationReference()));

        fileImporter = new FileImporter(this, waitingHandler, analysis, shotgunProtocol, identificationParameters, metrics);
        fileImporter.importFiles(idFiles, spectrumFiles, processingPreferences, spectrumCountingPreferences, projectDetails, backgroundThread);
    }

    /**
     * Returns the object cache.
     *
     * @return the object cache
     */
    public ObjectsCache getCache() {
        return objectsCache;
    }

    /**
     * Returns the reference identifying the identification under process.
     *
     * @return a String identifying the identification under process
     */
    public String getIdentificationReference() {
        return Identification.getDefaultReference(experiment.getReference(), sample.getReference(), replicateNumber);
    }

    /**
     * This method processes the identifications and fills the PeptideShaker
     * maps.
     *
     * @param inputMap the input map
     * @param proteinCount map of proteins found several times with the number
     * of times they appeared as first hit
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler handler for exceptions
     * @param identificationParameters the identification parameters
     * @param shotgunProtocol information on the shotgun protocol
     * @param processingPreferences the processing preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     *
     * @throws Exception exception thrown whenever an error occurred while
     * loading the identification files
     */
    public void processIdentifications(InputMap inputMap, HashMap<String, Integer> proteinCount, WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            ProcessingPreferences processingPreferences, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails)
            throws Exception {
        
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, shotgunProtocol, identificationParameters, metrics, spectrumCountingPreferences);

        if (!objectsCache.memoryCheck() && memoryWarning) {
            waitingHandler.appendReport("PeptideShaker is encountering memory issues! See http://peptide-shaker.googlecode.com for help.", true, true);
            waitingHandler.appendReport("You can edit the memory given to the tool via the \"Edit\" > \"Java Settings\" menu.", true, true);
            waitingHandler.appendReport("See http://peptide-shaker.googlecode.com for help.", true, true);
            memoryWarning = false;
        }
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        PsmScoringPreferences psmScoringPreferences = identificationParameters.getPsmScoringPreferences();

        ArrayList<Integer> usedAlgorithms = projectDetails.getIdentificationAlgorithms();
        if (psmScoringPreferences.isScoringNeeded(usedAlgorithms)) {

            PsmScorer psmScorer = new PsmScorer();

            waitingHandler.appendReport("Estimating PSM scores.", true, true);
            psmScorer.estimateIntermediateScores(identification, inputMap, processingPreferences, shotgunProtocol, identificationParameters, waitingHandler);

            if (psmScoringPreferences.isTargetDecoyNeededForPsmScoring(usedAlgorithms)) {
                if (sequenceFactory.concatenatedTargetDecoy()) {
                    waitingHandler.appendReport("Estimating intermediate scores probabilities.", true, true);
                    psmScorer.estimateIntermediateScoreProbabilities(identification, inputMap, processingPreferences, waitingHandler);
                } else {
                    waitingHandler.appendReport("No decoy sequences found. Impossible to estimate intermediate scores probabilities.", true, true);
                }
            }

            waitingHandler.appendReport("Scoring PSMs.", true, true);
            psmScorer.scorePsms(identification, inputMap, processingPreferences, identificationParameters, waitingHandler);
        }

        waitingHandler.appendReport("Computing assumptions probabilities.", true, true);
        if (sequenceFactory.concatenatedTargetDecoy()) {
            inputMap.estimateProbabilities(waitingHandler);
        }
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Saving assumptions probabilities.", true, true);
        attachAssumptionsProbabilities(inputMap, identificationParameters.getSequenceMatchingPreferences(), waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Selecting best peptide per spectrum.", true, true);
        BestMatchSelection bestMatchSelection = new BestMatchSelection(identification, proteinCount, matchesValidator, metrics);
        bestMatchSelection.selectBestHitAndFillPsmMap(inputMap, waitingHandler, shotgunProtocol, identificationParameters);
        IdMatchValidationPreferences idMatchValidationPreferences = identificationParameters.getIdValidationPreferences();
        if (idMatchValidationPreferences.getGroupSmallSubgroups()) {
            matchesValidator.getPsmMap().clean(idMatchValidationPreferences.getDefaultPsmFDR() / 100);
        }
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        if (MemoryConsumptionStatus.memoryUsed() > 0.9) {
            metrics.clearSpectrumKeys();
        }

        waitingHandler.appendReport("Computing PSM probabilities.", true, true);
        matchesValidator.getPsmMap().estimateProbabilities(waitingHandler);
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        String report = "Scoring PTMs in PSMs (D-score";
        PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
        if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
            report += " and " + ptmScoringPreferences.getSelectedProbabilisticScore().getName();
        }
        report += ")";
        waitingHandler.appendReport(report, true, true);
        ptmScorer.scorePsmPtms(identification, waitingHandler, exceptionHandler, identificationParameters, metrics, processingPreferences);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        if (MemoryConsumptionStatus.memoryUsed() > 0.9) {
            metrics.clearSpectrumKeys();
        }

        if (ptmScoringPreferences.isEstimateFlr()) {
            waitingHandler.appendReport("Thresholding PTM localizations.", true, true);
            PsmPTMMap psmPTMMap = ptmScorer.getPsmPTMMap();
            if (idMatchValidationPreferences.getGroupSmallSubgroups()) {
                psmPTMMap.clean(ptmScoringPreferences.getFlrThreshold() / 100);
            }
            psmPTMMap.estimateProbabilities(waitingHandler);
            ptmScorer.computeLocalizationStatistics(waitingHandler, ptmScoringPreferences.getFlrThreshold());
        }
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Resolving peptide inference issues.", true, true);
        ptmScorer.peptideInference(identification, identificationParameters, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        if (MemoryConsumptionStatus.memoryUsed() > 0.9) {
            metrics.clearSpectrumKeys();
        }
        waitingHandler.appendReport("Saving probabilities, building peptides and proteins.", true, true);
        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(identificationParameters.getSequenceMatchingPreferences(), waitingHandler); // @TODO: this is very slow if memory is full!!
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Simplifying protein groups.", true, true);
        ProteinInference proteinInference = new ProteinInference();
        proteinInference.removeRedundantGroups(identification, shotgunProtocol, identificationParameters, identificationFeaturesGenerator, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Generating peptide map.", true, true); // slow?
        matchesValidator.fillPeptideMaps(identification, metrics, waitingHandler, identificationParameters);
        if (idMatchValidationPreferences.getGroupSmallSubgroups()) {
            matchesValidator.getPeptideMap().clean(identificationParameters.getIdValidationPreferences().getDefaultPeptideFDR() / 100);
        }
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Computing peptide probabilities.", true, true); // should be fast
        matchesValidator.getPeptideMap().estimateProbabilities(waitingHandler);
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Saving peptide probabilities.", true, true); // could be slow
        matchesValidator.attachPeptideProbabilities(identification, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Generating protein map.", true, true); // could be slow
        matchesValidator.fillProteinMap(identification, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Resolving protein inference issues, inferring peptide and protein PI status.", true, true); // could be slow
        proteinInference.retainBestScoringGroups(identification, metrics, matchesValidator.getProteinMap(), shotgunProtocol, identificationParameters, identificationFeaturesGenerator, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Correcting protein probabilities.", true, true);
        matchesValidator.getProteinMap().estimateProbabilities(waitingHandler);
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Saving protein probabilities.", true, true);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, processingPreferences);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        if (idMatchValidationPreferences.getDefaultPsmFDR() == 1
                && idMatchValidationPreferences.getDefaultPeptideFDR() == 1
                && idMatchValidationPreferences.getDefaultProteinFDR() == 1) {
            waitingHandler.appendReport("Validating identifications at 1% FDR, quality control of matches.", true, true);
        } else {
            waitingHandler.appendReport("Validating identifications, quality control of matches.", true, true);
        }
        matchesValidator.validateIdentifications(identification, metrics, waitingHandler, exceptionHandler, shotgunProtocol, identificationParameters, identificationFeaturesGenerator, inputMap, spectrumCountingPreferences, processingPreferences);
        waitingHandler.increasePrimaryProgressCounter();
        metrics.clearSpectrumKeys();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Scoring PTMs in peptides.", true, true);
        ptmScorer.scorePeptidePtms(identification, waitingHandler, identificationParameters);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        waitingHandler.appendReport("Scoring PTMs in proteins.", true, true);
        ptmScorer.scoreProteinPtms(identification, metrics, waitingHandler, shotgunProtocol, identificationParameters, identificationFeaturesGenerator);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }

        projectCreationDuration.end();
        report = "Identification processing completed (" + projectCreationDuration.toString() + ").";

        // get the detailed report
        ArrayList<Integer> suspiciousInput = inputMap.suspiciousInput(identificationParameters.getIdValidationPreferences().getDefaultPsmFDR() / 100);
        //ArrayList<String> suspiciousPsms = matchesValidator.getPsmMap().suspiciousInput(); // @TODO: what happend to this one..?
        ArrayList<String> suspiciousPeptides = matchesValidator.getPeptideMap().suspiciousInput(identificationParameters.getIdValidationPreferences().getDefaultPeptideFDR() / 100);
        boolean suspiciousProteins = matchesValidator.getProteinMap().suspicousInput(identificationParameters.getIdValidationPreferences().getDefaultProteinFDR() / 100);

        if (suspiciousInput.size() > 0
                //|| suspiciousPsms.size() > 0 // @TODO: re-add!
                || suspiciousPeptides.size() > 0
                || suspiciousProteins) {

            String detailedReport = "";

            boolean firstLine = true;

            for (int searchEngine : suspiciousInput) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    detailedReport += ", ";
                }
                detailedReport += Advocate.getAdvocate(searchEngine).getName();
            }

            if (suspiciousInput.size() > 0) {
                detailedReport += " identifications.<br>";
            }

//            firstLine = true;
//
//            if (psmMap.getKeys().size() == 1) { // @TODO: re-add!
//                detailedReport += "PSMs.<br>";
//            } else {
//                for (String fraction : suspiciousPsms) {
//                    if (firstLine) {
//                        firstLine = false;
//                    } else {
//                        detailedReport += ", ";
//                    }
//                    detailedReport += fraction;
//                }
//                if (suspiciousPsms.size() > 0) {
//                    detailedReport += " charged PSMs.<br>";
//                }
//            }
            if (matchesValidator.getPeptideMap().getKeys().size() == 1) {
                detailedReport += "Peptides.<br>";
            } else {
                firstLine = true;
                for (String fraction : suspiciousPeptides) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        detailedReport += "<br>";
                    }
                    detailedReport += PeptideSpecificMap.getKeyName(identificationParameters.getSearchParameters().getPtmSettings(), fraction);
                    if (suspiciousPeptides.size() > 0) {
                        detailedReport += " peptides.<br>";
                    }
                }
            }

            if (suspiciousProteins) {
                detailedReport += "Proteins.<br>";
            }

            if (detailedReport.length() > 0) {
                detailedReport = "The following identification classes resulted in non robust statistical estimators, the confidence estimation and validation will be inaccurate for these matches:<br><br>"
                        + detailedReport
                        + "<br>You can inspect this in the <i>Validation</i> tab.";
                //addWarning(new FeedBack(FeedBack.FeedBackType.WARNING, "Non robust statistical estimations", new ArrayList<String>(), detailedReport)); // @TODO: re-add later
            }
        }

        waitingHandler.appendReport(report, true, true);
        waitingHandler.appendReportEndLine();
        waitingHandler.appendReportEndLine();
        identification.addUrParam(new PSMaps(matchesValidator.getPsmMap(), matchesValidator.getPeptideMap(), matchesValidator.getProteinMap(), inputMap, ptmScorer.getPsmPTMMap()));
        waitingHandler.setRunFinished();
    }

    /**
     * Processes the identifications if a change occurred in the PSM map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param processingPreferences the processing preferences
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     *
     * @throws Exception exception thrown whenever it is attempted to attach
     * more than one identification per search engine per spectrum
     */
    public void spectrumMapChanged(Identification identification, WaitingHandler waitingHandler, ProcessingPreferences processingPreferences,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws Exception {
        PeptideSpecificMap peptideMap = new PeptideSpecificMap();
        ProteinMap proteinMap = new ProteinMap();
        matchesValidator.setPeptideMap(peptideMap);
        matchesValidator.setProteinMap(proteinMap);
        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(identificationParameters.getSequenceMatchingPreferences(), waitingHandler);
        matchesValidator.fillPeptideMaps(identification, metrics, waitingHandler, identificationParameters);
        peptideMap.clean(identificationParameters.getIdValidationPreferences().getDefaultPeptideFDR() / 100);
        peptideMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachPeptideProbabilities(identification, waitingHandler);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, processingPreferences);
        ProteinInference proteinInference = new ProteinInference();
        proteinInference.retainBestScoringGroups(identification, metrics, proteinMap, shotgunProtocol, identificationParameters, identificationFeaturesGenerator, waitingHandler);
    }

    /**
     * Processes the identifications if a change occurred in the peptide map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param processingPreferences the processing preferences
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     *
     * @throws Exception exception thrown whenever it is attempted to attach
     * more than one identification per search engine per spectrum
     */
    public void peptideMapChanged(Identification identification, WaitingHandler waitingHandler, ProcessingPreferences processingPreferences,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws Exception {
        ProteinMap proteinMap = new ProteinMap();
        matchesValidator.setProteinMap(proteinMap);
        matchesValidator.attachPeptideProbabilities(identification, waitingHandler);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, processingPreferences);
        ProteinInference proteinInference = new ProteinInference();
        proteinInference.retainBestScoringGroups(identification, metrics, proteinMap, shotgunProtocol, identificationParameters, identificationFeaturesGenerator, waitingHandler);
    }

    /**
     * Processes the identifications if a change occurred in the protein map.
     *
     * @param waitingHandler the waiting handler
     * @param processingPreferences the processing preferences
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     */
    public void proteinMapChanged(WaitingHandler waitingHandler, ProcessingPreferences processingPreferences) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, processingPreferences);
    }

    /**
     * Attaches the spectrum posterior error probabilities to the peptide
     * assumptions.
     *
     * @param inputMap map of the input scores
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachAssumptionsProbabilities(InputMap inputMap, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler) throws Exception {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        }

        for (String spectrumFileName : identification.getSpectrumFiles()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, null, true, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();
                HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = identification.getAssumptions(spectrumKey);

                HashMap<Double, ArrayList<PSParameter>> pepToParameterMap = new HashMap<Double, ArrayList<PSParameter>>();

                for (int searchEngine : assumptionsMap.keySet()) {

                    HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> seMapping = assumptionsMap.get(searchEngine);
                    ArrayList<Double> eValues = new ArrayList<Double>(seMapping.keySet());
                    Collections.sort(eValues);
                    double previousP = 0;
                    ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<PSParameter>();
                    SpectrumIdentificationAssumption previousAssumption = null;

                    for (double eValue : eValues) {

                        for (SpectrumIdentificationAssumption assumption : seMapping.get(eValue)) {
                            PSParameter psParameter = new PSParameter();
                            psParameter = (PSParameter) assumption.getUrParam(psParameter);
                            if (psParameter == null) {
                                psParameter = new PSParameter();
                            }

                            if (sequenceFactory.concatenatedTargetDecoy()) {

                                double newP = inputMap.getProbability(searchEngine, eValue);
                                double pep = previousP;

                                if (newP > previousP) {
                                    pep = newP;
                                    previousP = newP;
                                }

                                psParameter.setSearchEngineProbability(pep);

                                ArrayList<PSParameter> pSParameters = pepToParameterMap.get(pep);
                                if (pSParameters == null) {
                                    pSParameters = new ArrayList<PSParameter>(1);
                                    pepToParameterMap.put(pep, pSParameters);
                                }
                                pSParameters.add(psParameter);

                                if (previousAssumption != null) {
                                    boolean same = false;
                                    if ((assumption instanceof PeptideAssumption) && (previousAssumption instanceof PeptideAssumption)) {
                                        Peptide newPeptide = ((PeptideAssumption) assumption).getPeptide();
                                        Peptide previousPeptide = ((PeptideAssumption) previousAssumption).getPeptide();
                                        if (newPeptide.isSameSequenceAndModificationStatus(previousPeptide, sequenceMatchingPreferences)) {
                                            same = true;
                                        }
                                    } else if ((assumption instanceof TagAssumption) && (previousAssumption instanceof TagAssumption)) {
                                        Tag newTag = ((TagAssumption) assumption).getTag();
                                        Tag previousTag = ((TagAssumption) previousAssumption).getTag();
                                        if (newTag.isSameSequenceAndModificationStatusAs(previousTag, sequenceMatchingPreferences)) {
                                            same = true;
                                        }
                                    }

                                    if (!same) {
                                        for (PSParameter previousParameter : previousAssumptionsParameters) {
                                            double deltaPEP = pep - previousParameter.getSearchEngineProbability();
                                            previousParameter.setAlgorithmDeltaPEP(deltaPEP);
                                        }
                                        previousAssumptionsParameters.clear();
                                    }
                                }
                                previousAssumption = assumption;
                                previousAssumptionsParameters.add(psParameter);

                            } else {
                                psParameter.setSearchEngineProbability(1.0);
                            }

                            assumption.addUrParam(psParameter);
                        }
                    }

                    for (PSParameter previousParameter : previousAssumptionsParameters) {
                        double deltaPEP = 1 - previousParameter.getSearchEngineProbability();
                        previousParameter.setAlgorithmDeltaPEP(deltaPEP);
                    }
                }

                // Compute the delta pep score accross all search engines
                Double previousPEP = null;
                ArrayList<PSParameter> previousParameters = new ArrayList<PSParameter>();
                ArrayList<Double> peps = new ArrayList<Double>(pepToParameterMap.keySet());
                Collections.sort(peps);
                for (double pep : peps) {
                    if (previousPEP != null) {
                        for (PSParameter previousParameter : previousParameters) {
                            double delta = pep - previousPEP;
                            previousParameter.setDeltaPEP(delta);
                        }
                    }
                    previousParameters = pepToParameterMap.get(pep);
                    previousPEP = pep;
                }
                for (PSParameter previousParameter : previousParameters) {
                    double delta = 1 - previousParameter.getSearchEngineProbability();
                    previousParameter.setDeltaPEP(delta);
                }

                identification.updateAssumptions(spectrumKey, assumptionsMap);

                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressCounter();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }
        }

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }
    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum
     * matches.
     *
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, IllegalArgumentException, Exception {

        waitingHandler.setWaitingText("Attaching Spectrum Probabilities - Building Peptides and Proteins. Please Wait...");

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        for (String spectrumFileName : identification.getSpectrumFiles()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, parameters, false, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (sequenceFactory.concatenatedTargetDecoy()) {
                    Integer charge = new Integer(psParameter.getSpecificMapKey());
                    String fileName = Spectrum.getSpectrumFile(spectrumKey);
                    psParameter.setPsmProbability(matchesValidator.getPsmMap().getProbability(fileName, charge, psParameter.getPsmProbabilityScore()));
                } else {
                    psParameter.setPsmProbability(1.0);
                }

                identification.updateSpectrumMatchParameter(spectrumKey, psParameter);
                identification.buildPeptidesAndProteins(spectrumKey, sequenceMatchingPreferences);

                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
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
     * Returns the identification features generator used when loading the
     * files.
     *
     * @return the identification features generator used when loading the files
     */
    public IdentificationFeaturesGenerator getIdentificationFeaturesGenerator() {
        return identificationFeaturesGenerator;
    }

    /**
     * Adds a warning to the feedback list. If a feedback with the same title is
     * already loaded it will be ignored.
     *
     * @param feedback the feedback
     */
    public void addWarning(FeedBack feedback) {
        warnings.put(feedback.getTitle(), feedback);
    }

    /**
     * Returns the warnings.
     *
     * @return the warnings
     */
    public HashMap<String, FeedBack> getWarnings() {
        return warnings;
    }

    /**
     * Verifies that the modifications backed-up in the search parameters are
     * loaded and returns an error message if one was already loaded, null
     * otherwise.
     *
     * @param searchParameters the search parameters to load
     * @return an error message if one was already loaded, null otherwise
     */
    public static String loadModifications(SearchParameters searchParameters) {
        String error = null;
        ArrayList<String> toCheck = PTMFactory.getInstance().loadBackedUpModifications(searchParameters, true);
        if (!toCheck.isEmpty()) {
            error = "The definition of the following PTM(s) seems to have changed and were overwritten:\n";
            for (int i = 0; i < toCheck.size(); i++) {
                if (i > 0) {
                    if (i < toCheck.size() - 1) {
                        error += ", ";
                    } else {
                        error += " and ";
                    }
                }
                error += toCheck.get(i);
            }
            error += ".\nPlease verify the definition of the PTM(s) in the modifications editor.";
        }
        return error;
    }

    /**
     * Returns the default experiment file.
     *
     * @return the default experiment file
     */
    public static String getDefaultExperimentFileName() {
        return PeptideShaker.experimentObjectName;
    }

    /**
     * Returns the file used for user preferences storage.
     *
     * @return the file used for user preferences storage
     */
    public static String getUserPreferencesFile() {
        return USER_PREFERENCES_FILE;
    }

    /**
     * Returns the folder used for user preferences storage.
     *
     * @return the folder used for user preferences storage
     */
    public static String getUserPreferencesFolder() {
        File tempFile = new File(getUserPreferencesFile());
        return tempFile.getParent();
    }

    /**
     * Sets the file used for user preferences storage.
     *
     * @param userPreferencesFolder the folder used for user preferences storage
     */
    public static void setUserPreferencesFolder(String userPreferencesFolder) {
        File tempFile = new File(userPreferencesFolder, "userpreferences.cpf");
        PeptideShaker.USER_PREFERENCES_FILE = tempFile.getAbsolutePath();
    }

    /**
     * Returns the directory used to store the identification matches.
     *
     * @return the directory used to store the identification matches
     */
    public static String getMatchesDirectorySubPath() {
        return SERIALIZATION_DIRECTORY;
    }

    /**
     * Returns the matches directory parent. An empty string if not set. Can be
     * a relative path.
     *
     * @return the matches directory parent
     */
    public static String getMatchesDirectoryParent() {
        return SERIALIZATION_PARENT_DIRECTORY;
    }

    /**
     * Returns the matches directory parent. An empty string if not set.
     *
     * @param jarFilePath the path to the jar file
     *
     * @return the matches directory parent
     */
    public static File getMatchesDirectoryParent(String jarFilePath) {
        String matchesParentDirectory = PeptideShaker.getMatchesDirectoryParent();
        if (matchesParentDirectory.equals("resources")) {
            return new File(jarFilePath, matchesParentDirectory);
        } else {
            return new File(matchesParentDirectory);
        }
    }

    /**
     * Sets the matches directory parent.
     *
     * @param matchesDirectoryParent the matches directory parent
     * @throws IOException thrown of an exception occurs
     */
    public static void setMatchesDirectoryParent(String matchesDirectoryParent) throws IOException {
        PeptideShaker.SERIALIZATION_PARENT_DIRECTORY = matchesDirectoryParent;
        File serializationFolder = new File(matchesDirectoryParent, PeptideShaker.getMatchesDirectorySubPath());
        if (!serializationFolder.exists()) {
            serializationFolder.mkdirs();
            if (!serializationFolder.exists()) {
                throw new IOException("Impossible to create folder " + serializationFolder.getAbsolutePath() + ".");
            }
        }
    }

    /**
     * Returns the path to the match folder according to the user path settings.
     *
     * @param jarFilePath the path to the jar file
     *
     * @return the path to the match folder according to the user path settings
     */
    public static File getSerializationDirectory(String jarFilePath) {
        return new File(getMatchesDirectoryParent(jarFilePath), PeptideShaker.getMatchesDirectorySubPath());
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public static String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = (new PeptideShaker()).getClass().getClassLoader().getResourceAsStream("peptide-shaker.properties");
            p.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p.getProperty("peptide-shaker.version");
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public static String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath((new PeptideShaker()).getClass().getResource("PeptideShaker.class").getPath(), "PeptideShaker");
    }

    /**
     * Returns the configuration file.
     *
     * @return the configuration file
     */
    public static ConfigurationFile getConfigurationFile() {
        File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator); // @TODO: make this more generic?
        File file = new File(folder, PEPTIDESHAKER_CONFIGURATION_FILE);
        return new ConfigurationFile(file);
    }
}
