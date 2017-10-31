package eu.isas.peptideshaker;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.maps.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.maps.PsmPTMMap;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import eu.isas.peptideshaker.fileimport.FileImporter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.messages.FeedBack;
import com.compomics.util.parameters.identification.advanced.FractionParameters;
import com.compomics.util.parameters.identification.advanced.IdMatchValidationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.parameters.PSParameter;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.identification.advanced.PsmScoringParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.tools.UtilitiesUserParameters;
import com.compomics.util.waiting.Duration;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.protein_inference.ProteinInference;
import eu.isas.peptideshaker.ptm.PtmScorer;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import eu.isas.peptideshaker.scoring.psm_scoring.PsmScorer;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.peptideshaker.validation.MatchesValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
    private ProjectParameters projectParameters;
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
     * User preferences file.
     */
    private static String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences.cpf";
    /**
     * Default PeptideShaker modifications.
     */
    public static final String PEPTIDESHAKER_CONFIGURATION_FILE = "PeptideShaker_configuration.txt";
    /**
     * The location of the folder used for the database.
     */
    private static String DATABASE_DIRECTORY = "matches";
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
     * The compomics PTM factory.
     */
    private ModificationFactory ptmFactory = ModificationFactory.getInstance();
    /**
     * Metrics to be picked when loading the identification.
     */
    private Metrics metrics = new Metrics();
    /**
     * The gene maps.
     */
    private GeneMaps geneMaps = new GeneMaps();
    /**
     * An identification features generator which will compute figures on the
     * identification matches and keep some of them in memory.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * List of warnings collected while working on the data.
     */
    private HashMap<String, FeedBack> warnings = new HashMap<>();
    /**
     * If true, a warning will be displayed when encountering memory issues.
     */
    private boolean memoryWarning = true;
    /**
     * Object used to monitor the duration of the project creation.
     */
    private Duration projectCreationDuration;
    /**
     * Connection to the database.
     */
    private ObjectsDB objectsDB;
    /**
     * The identification object.
     */
    private Identification identification;

    /**
     * Empty constructor for instantiation purposes.
     */
    private PeptideShaker() {
    }

    /**
     * Constructor without mass specification. Calculation will be done on new
     * maps which will be retrieved as compomics utilities parameters.
     *
     * @param projectParameters the experiment conducted
     */
    public PeptideShaker(ProjectParameters projectParameters) {
        this.projectParameters = projectParameters;
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
     * @param projectParameters the experiment conducted
     * @param psMaps the peptide shaker maps
     */
    public PeptideShaker(ProjectParameters projectParameters, PSMaps psMaps) {
        this.projectParameters = projectParameters;
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
     * @param identificationParameters identification parameters
     * @param projectDetails the project details
     * @param processingPreferences the initial processing preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param backgroundThread boolean indicating whether the import should be
     * done in a background thread (GUI mode) or in the current thread (command
     * line mode).
     */
    public void importFiles(WaitingHandler waitingHandler, ArrayList<File> idFiles, ArrayList<File> spectrumFiles,
            IdentificationParameters identificationParameters, ProjectDetails projectDetails,
            ProcessingParameters processingPreferences, SpectrumCountingPreferences spectrumCountingPreferences, boolean backgroundThread) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        projectCreationDuration = new Duration();
        projectCreationDuration.start();

        waitingHandler.appendReport("Import process for " + projectParameters.getProjectUniqueName(), true, true);
        waitingHandler.appendReportEndLine();

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String dbName = projectParameters.getProjectUniqueName() + df.format(projectParameters.getCreationTime()) + ".psDB";

        objectsDB = new ObjectsDB(PeptideShaker.getMatchesFolder().getAbsolutePath(), dbName);
        identification = new Identification(objectsDB);
        identification.addObject(ProjectParameters.nameForDatabase, projectParameters);

        fileImporter = new FileImporter(this, waitingHandler, identificationParameters, metrics);
        fileImporter.importFiles(idFiles, spectrumFiles, processingPreferences, spectrumCountingPreferences, projectDetails, backgroundThread);
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
     * @param processingPreferences the processing preferences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param projectDetails the project details
     *
     * @throws Exception exception thrown whenever an error occurred while
     * loading the identification files
     */
    public void processIdentifications(InputMap inputMap, HashMap<String, Integer> proteinCount, WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler, IdentificationParameters identificationParameters,
            ProcessingParameters processingPreferences, SpectrumCountingPreferences spectrumCountingPreferences, ProjectDetails projectDetails)
            throws Exception {

        identification.getObjectsDB().commit();

        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, identificationParameters, metrics, spectrumCountingPreferences);

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        PsmScoringParameters psmScoringPreferences = identificationParameters.getPsmScoringPreferences();
        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();

        ArrayList<Integer> usedAlgorithms = projectDetails.getIdentificationAlgorithms();
        if (psmScoringPreferences.isScoringNeeded(usedAlgorithms)) {

            PsmScorer psmScorer = new PsmScorer();

            waitingHandler.appendReport("Estimating PSM scores.", true, true);
            psmScorer.estimateIntermediateScores(identification, inputMap, processingPreferences, identificationParameters, waitingHandler, exceptionHandler);

            if (psmScoringPreferences.isTargetDecoyNeededForPsmScoring(usedAlgorithms)) {
                if (fastaParameters.isTargetDecoy()) {
                    waitingHandler.appendReport("Estimating intermediate scores probabilities.", true, true);
                    psmScorer.estimateIntermediateScoreProbabilities(identification, inputMap, processingPreferences, waitingHandler);
                } else {
                    waitingHandler.appendReport("No decoy sequences found. Impossible to estimate intermediate scores probabilities.", true, true);
                }
            }

            waitingHandler.appendReport("Scoring PSMs.", true, true);
            psmScorer.scorePsms(identification, inputMap, processingPreferences, identificationParameters, waitingHandler);
        }
        identification.getObjectsDB().commit();
        System.gc();

        if (fastaParameters.isTargetDecoy()) {
            waitingHandler.appendReport("Computing assumptions probabilities.", true, true);
        } else {
            waitingHandler.appendReport("Importing assumptions scores.", true, true);
        }
        inputMap.estimateProbabilities(waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();

        if (fastaParameters.isTargetDecoy()) {
            waitingHandler.appendReport("Saving assumptions probabilities.", true, true);
        } else {
            waitingHandler.appendReport("No decoy sequences found. Impossible to estimate assumptions probabilities.", true, true);
        }
        attachAssumptionsProbabilities(inputMap, fastaParameters, identificationParameters.getSequenceMatchingPreferences(), waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        

        waitingHandler.appendReport("Selecting best peptide per spectrum.", true, true);
        BestMatchSelection bestMatchSelection = new BestMatchSelection(identification, proteinCount, matchesValidator, metrics);
        bestMatchSelection.selectBestHitAndFillPsmMap(inputMap, waitingHandler, identificationParameters);
        IdMatchValidationParameters idMatchValidationPreferences = identificationParameters.getIdValidationPreferences();
        if (idMatchValidationPreferences.getMergeSmallSubgroups()) {
            matchesValidator.getPsmMap().clean(idMatchValidationPreferences.getDefaultPsmFDR() / 100);
        }
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        
        

        if (fastaParameters.isTargetDecoy()) {
            waitingHandler.appendReport("Computing PSM probabilities.", true, true);
        } else {
            waitingHandler.appendReport("No decoy sequences found. Impossible to estimate PSM probabilities.", true, true);
        }
        matchesValidator.getPsmMap().estimateProbabilities(waitingHandler);
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();

        String report = "Scoring PTMs in PSMs (D-score";
        ModificationLocalizationParameters ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
        if (ptmScoringPreferences.isProbabilisticScoreCalculation()) {
            report += " and " + ptmScoringPreferences.getSelectedProbabilisticScore().getName();
        }
        report += ")";
        waitingHandler.appendReport(report, true, true);
        ptmScorer.scorePsmPtms(identification, waitingHandler, exceptionHandler, identificationParameters, metrics, processingPreferences);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        
        

        if (ptmScoringPreferences.isEstimateFlr()) {
            waitingHandler.appendReport("Thresholding PTM localizations.", true, true);
            PsmPTMMap psmPTMMap = ptmScorer.getPsmPTMMap();
            if (idMatchValidationPreferences.getMergeSmallSubgroups()) {
                psmPTMMap.clean(ptmScoringPreferences.getFlrThreshold() / 100);
            }
            psmPTMMap.estimateProbabilities(waitingHandler);
            ptmScorer.computeLocalizationStatistics(waitingHandler, ptmScoringPreferences.getFlrThreshold());
        }
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        if (ptmScoringPreferences.getAlignNonConfidentPTMs()) {
            waitingHandler.appendReport("Resolving peptide inference issues.", true, true);
            ptmScorer.peptideInference(identification, identificationParameters, waitingHandler);
            waitingHandler.increasePrimaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        
        waitingHandler.appendReport("Saving probabilities, building peptides and proteins.", true, true);
        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(identificationParameters.getSequenceMatchingPreferences(), fastaParameters, waitingHandler); // @TODO: this is very slow if memory is full!!
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        ProteinInference proteinInference = new ProteinInference();
        if (identificationParameters.getProteinInferencePreferences().getSimplifyGroups()) {
            waitingHandler.appendReport("Simplifying protein groups.", true, true);
            proteinInference.removeRedundantGroups(identification, identificationParameters, identificationFeaturesGenerator, waitingHandler);
            waitingHandler.increasePrimaryProgressCounter();
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Generating peptide map.", true, true);
        matchesValidator.fillPeptideMaps(identification, metrics, waitingHandler, identificationParameters);
        if (idMatchValidationPreferences.getMergeSmallSubgroups()) {
            matchesValidator.getPeptideMap().clean(identificationParameters.getIdValidationPreferences().getDefaultPeptideFDR() / 100);
        }
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        if (fastaParameters.isTargetDecoy()) {
            waitingHandler.appendReport("Computing peptide probabilities.", true, true);
        } else {
            waitingHandler.appendReport("No decoy sequences found. Impossible to estimate peptide probabilities.", true, true);
        }
        matchesValidator.getPeptideMap().estimateProbabilities(waitingHandler);
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Saving peptide probabilities.", true, true);
        matchesValidator.attachPeptideProbabilities(identification, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Generating protein map.", true, true);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Resolving protein inference issues, inferring peptide and protein PI status.", true, true); // could be slow
        proteinInference.retainBestScoringGroups(identification, metrics, matchesValidator.getProteinMap(), identificationParameters, identificationFeaturesGenerator, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        if (fastaParameters.isTargetDecoy()) {
            waitingHandler.appendReport("Correcting protein probabilities.", true, true);
        } else {
            waitingHandler.appendReport("No decoy sequences found. Impossible to estimate protein probabilities.", true, true);
        }
        matchesValidator.getProteinMap().estimateProbabilities(waitingHandler);
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Saving protein probabilities.", true, true);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, identificationParameters.getFractionSettings());
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        if (fastaParameters.isTargetDecoy()) {
            if (idMatchValidationPreferences.getDefaultPsmFDR() == 1
                    && idMatchValidationPreferences.getDefaultPeptideFDR() == 1
                    && idMatchValidationPreferences.getDefaultProteinFDR() == 1) {
                waitingHandler.appendReport("Validating identifications at 1% FDR, quality control of matches.", true, true);
            } else {
                waitingHandler.appendReport("Validating identifications, quality control of matches.", true, true);
            }
        } else {
            waitingHandler.appendReport("No decoy sequences found. Impossible to estimate FDRs.", true, true);
        }
        matchesValidator.validateIdentifications(identification, metrics, geneMaps, waitingHandler, exceptionHandler, identificationParameters, identificationFeaturesGenerator, inputMap, spectrumCountingPreferences, processingPreferences);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Scoring PTMs in peptides.", true, true);
        ptmScorer.scorePeptidePtms(identification, waitingHandler, identificationParameters);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

        waitingHandler.appendReport("Scoring PTMs in proteins.", true, true);
        ptmScorer.scoreProteinPtms(identification, metrics, waitingHandler, identificationParameters, identificationFeaturesGenerator);
        waitingHandler.increasePrimaryProgressCounter();
        if (waitingHandler.isRunCanceled()) {
            return;
        }
        identification.getObjectsDB().commit();
        System.gc();
        
        
        

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
                    detailedReport += PeptideSpecificMap.getKeyName(identificationParameters.getSearchParameters().getModificationParameters(), fraction);
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
    public void spectrumMapChanged(Identification identification, WaitingHandler waitingHandler, ProcessingParameters processingPreferences,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters) throws Exception {
        PeptideSpecificMap peptideMap = new PeptideSpecificMap();
        ProteinMap proteinMap = new ProteinMap();
        matchesValidator.setPeptideMap(peptideMap);
        matchesValidator.setProteinMap(proteinMap);
        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getSearchParameters().getFastaParameters(), waitingHandler);
        matchesValidator.fillPeptideMaps(identification, metrics, waitingHandler, identificationParameters);
        peptideMap.clean(identificationParameters.getIdValidationPreferences().getDefaultPeptideFDR() / 100);
        peptideMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachPeptideProbabilities(identification, waitingHandler);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, identificationParameters.getFractionSettings());
        ProteinInference proteinInference = new ProteinInference();
        proteinInference.retainBestScoringGroups(identification, metrics, proteinMap, identificationParameters, identificationFeaturesGenerator, waitingHandler);
    }

    /**
     * Processes the identifications if a change occurred in the peptide map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param identificationParameters the identification parameters
     *
     * @throws Exception exception thrown whenever it is attempted to attach
     * more than one identification per search engine per spectrum
     */
    public void peptideMapChanged(Identification identification, WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters) throws Exception {
        ProteinMap proteinMap = new ProteinMap();
        matchesValidator.setProteinMap(proteinMap);
        matchesValidator.attachPeptideProbabilities(identification, waitingHandler);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, identificationParameters.getFractionSettings());
        ProteinInference proteinInference = new ProteinInference();
        proteinInference.retainBestScoringGroups(identification, metrics, proteinMap, identificationParameters, identificationFeaturesGenerator, waitingHandler);
    }

    /**
     * Processes the identifications if a change occurred in the protein map.
     *
     * @param waitingHandler the waiting handler
     * @param fractionSettings the fraction settings
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     */
    public void proteinMapChanged(WaitingHandler waitingHandler, FractionParameters fractionSettings) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        matchesValidator.attachProteinProbabilities(identification, metrics, waitingHandler, fractionSettings);
    }

    /**
     * Attaches the spectrum posterior error probabilities to the peptide
     * assumptions.
     *
     * @param inputMap map of the input scores
     * @param fastaParameters the fasta parsing parameters
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachAssumptionsProbabilities(InputMap inputMap, FastaParameters fastaParameters, SequenceMatchingParameters sequenceMatchingPreferences, WaitingHandler waitingHandler) throws Exception {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        }

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            // Peptides
            HashMap<Integer, HashMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptionsMap = spectrumMatch.getPeptideAssumptionsMap();

            HashMap<Double, ArrayList<PSParameter>> pepToParameterMap = new HashMap<>();

            for (int searchEngine : peptideAssumptionsMap.keySet()) {

                HashMap<Double, ArrayList<PeptideAssumption>> seMapping = peptideAssumptionsMap.get(searchEngine);
                ArrayList<Double> eValues = new ArrayList<>(seMapping.keySet());
                Collections.sort(eValues);
                double previousP = 0;
                ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<>();
                PeptideAssumption previousAssumption = null;

                for (double eValue : eValues) {

                    for (PeptideAssumption assumption : seMapping.get(eValue)) {

                        PSParameter psParameter = new PSParameter();
                        psParameter = (PSParameter) assumption.getUrParam(psParameter);

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

                            psParameter.setSearchEngineProbability(pep);

                            ArrayList<PSParameter> pSParameters = pepToParameterMap.get(pep);

                            if (pSParameters == null) {

                                pSParameters = new ArrayList<>(1);
                                pepToParameterMap.put(pep, pSParameters);

                            }

                            pSParameters.add(psParameter);

                            if (previousAssumption != null) {

                                boolean same = false;
                                Peptide newPeptide = assumption.getPeptide();
                                Peptide previousPeptide = previousAssumption.getPeptide();

                                if (newPeptide.isSameSequenceAndModificationStatus(previousPeptide, sequenceMatchingPreferences)) {

                                    same = true;
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
            ArrayList<PSParameter> previousParameters = new ArrayList<>();
            ArrayList<Double> peps = new ArrayList<>(pepToParameterMap.keySet());
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

            if (waitingHandler != null) {
                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }

            // Assumptions
            HashMap<Integer, HashMap<Double, ArrayList<TagAssumption>>> tagAssumptionsMap = spectrumMatch.getTagAssumptionsMap();

            for (int searchEngine : tagAssumptionsMap.keySet()) {

                HashMap<Double, ArrayList<TagAssumption>> seMapping = tagAssumptionsMap.get(searchEngine);
                ArrayList<Double> eValues = new ArrayList<>(seMapping.keySet());
                Collections.sort(eValues);
                double previousP = 0;
                ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<>();
                TagAssumption previousAssumption = null;

                for (double eValue : eValues) {

                    for (TagAssumption assumption : seMapping.get(eValue)) {

                        PSParameter psParameter = new PSParameter();
                        psParameter = (PSParameter) assumption.getUrParam(psParameter);

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

                            psParameter.setSearchEngineProbability(pep);

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
     * @param fastaParameters the fasta parsing parameters
     * @param waitingHandler the handler displaying feedback to the user
     * 
     * @throws InterruptedException exception thrown if a thread gets interrupted
     */
    private void attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(SequenceMatchingParameters sequenceMatchingPreferences, FastaParameters fastaParameters, WaitingHandler waitingHandler) throws InterruptedException {

        waitingHandler.setWaitingText("Attaching Spectrum Probabilities - Building Peptides and Proteins. Please Wait...");

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        PSParameter psParameter = new PSParameter();

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);

            if (spectrumMatch.getBestPeptideAssumption() == null) {
                continue;
            }

            if (fastaParameters.isTargetDecoy()) {
                Integer charge = null;
                try {
                    charge = new Integer(psParameter.getSpecificMapKey());
                } catch (Exception E) {
                    System.out.println("charge not found: " + spectrumMatch.getKey());
                }

                String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
                psParameter.setPsmProbability(matchesValidator.getPsmMap().getProbability(fileName, charge, psParameter.getPsmProbabilityScore()));
                
            } else {
                
                psParameter.setPsmProbability(1.0);
            
            }

            identification.buildPeptidesAndProteins(spectrumMatch, sequenceMatchingPreferences);

            waitingHandler.increaseSecondaryProgressCounter();
            
            if (waitingHandler.isRunCanceled()) {
            
                return;
            
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
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        return geneMaps;
    }

    public Identification getIdentification() {
        return identification;
    }

    /**
     * Sets the gene maps.
     *
     * @param geneMaps the new gene maps
     */
    public void setGeneMaps(GeneMaps geneMaps) {
        this.geneMaps = geneMaps;
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
        ArrayList<String> toCheck = ModificationFactory.getInstance().loadBackedUpModifications(searchParameters, true);
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
        return DATABASE_DIRECTORY;
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
     * @return the matches directory parent
     */
    public static File getMatchesDirectoryParentFile() {
        String matchesParentDirectory = PeptideShaker.getMatchesDirectoryParent();
        if (matchesParentDirectory.equals("resources")) {
            return new File(getJarFilePath(), matchesParentDirectory);
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
     * Returns the path to the matches folder according to the user path
     * settings.
     *
     * @return the path to the match folder according to the user path settings
     */
    public static File getMatchesFolder() {
        return new File(getMatchesDirectoryParentFile(), PeptideShaker.getMatchesDirectorySubPath());
    }

    /**
     * Instantiates the spectrum, sequence, and PTM factories with caches
     * adapted to the memory available as set in the user preferences.
     *
     * @param utilitiesUserPreferences the user preferences
     */
    public static void instantiateFacories(UtilitiesUserParameters utilitiesUserPreferences) {
        
        int nSpectra;
        
        if (utilitiesUserPreferences.getMemoryParameter() > 32000) {
        
            nSpectra = 100000000;
            
        } else if (utilitiesUserPreferences.getMemoryParameter() > 16000) {
            
            nSpectra = 50000000;
        
        } else if (utilitiesUserPreferences.getMemoryParameter() > 8000) {
        
            nSpectra = 10000000;
        
        } else if (utilitiesUserPreferences.getMemoryParameter() > 4000) {
        
            nSpectra = 5000000;
        
        } else {
        
            nSpectra = 1000000;
        
        }
        
        EnzymeFactory.getInstance();
        ModificationFactory.getInstance();
        SpectrumFactory.getInstance(nSpectra);
        
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
}
