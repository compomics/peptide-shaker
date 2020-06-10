package eu.isas.peptideshaker;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import eu.isas.peptideshaker.fileimport.FileImporter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.advanced.FractionParameters;
import com.compomics.util.parameters.identification.advanced.IdMatchValidationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.identification.advanced.PsmScoringParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.waiting.Duration;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import eu.isas.peptideshaker.protein_inference.ProteinInference;
import eu.isas.peptideshaker.ptm.ModificationLocalizationScorer;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.PsmScorer;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.protein_inference.PeptideAndProteinBuilder;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.quantification.spectrumcounting.ScalingFactorsEstimators;
import com.compomics.util.parameters.peptide_shaker.ProjectType;
import eu.isas.peptideshaker.processing.ProteinProcessor;
import eu.isas.peptideshaker.processing.PsmProcessor;
import eu.isas.peptideshaker.protein_inference.GroupSimplification;
import com.compomics.util.experiment.identification.peptide_inference.PeptideInference;
import eu.isas.peptideshaker.validation.MatchesValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * This class will be responsible for the identification import and the
 * associated calculations.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShaker {

    /**
     * Default time out in days.
     */
    public static final int TIMEOUT_DAYS = 365;
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
    private final ModificationLocalizationScorer modificationLocalizationScorer = new ModificationLocalizationScorer();
    /**
     * The id importer will import and process the identifications.
     */
    private FileImporter fileImporter = null;
    /**
     * User preferences file.
     */
    private static String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences_2.0.cpf";
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
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * Metrics to be picked when loading the identification.
     */
    private final Metrics metrics = new Metrics();
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
     * A provider for protein sequences.
     */
    private SequenceProvider sequenceProvider;
    /**
     * A provider for protein details.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private HashMap<String, Integer> proteinCount;
    /**
     * The input map.
     */
    private InputMap inputMap;

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

    }

    /**
     * Imports identification results from result files.
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param idFiles the files to import
     * @param spectrumProvider the spectrum provider
     * @param identificationParameters identification parameters
     * @param projectDetails the project details
     * @param processingPreferences the initial processing preferences
     * @param exceptionHandler the exception handler
     *
     * @return 0 if the import went fine, 1 otherwise
     */
    public int importFiles(
            WaitingHandler waitingHandler,
            ArrayList<File> idFiles,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            ProjectDetails projectDetails,
            ProcessingParameters processingPreferences,
            ExceptionHandler exceptionHandler
    ) {

        projectCreationDuration = new Duration();
        projectCreationDuration.start();

        waitingHandler.appendReport("Import process for " + projectParameters.getProjectUniqueName(), true, true);
        waitingHandler.appendReportEndLine();

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String dbName = projectParameters.getProjectUniqueName() + df.format(projectParameters.getCreationTime()) + ".psdb";

        objectsDB = new ObjectsDB(PeptideShaker.getMatchesFolder().getAbsolutePath(), dbName);
        identification = new Identification(objectsDB);
        identification.addObject(ProjectParameters.key, projectParameters);

        fileImporter = new FileImporter(
                identification,
                identificationParameters,
                processingPreferences,
                metrics,
                projectDetails,
                spectrumProvider,
                waitingHandler,
                exceptionHandler
        );
        int outcome = fileImporter.importFiles(
                idFiles
        );

        if (outcome == 0) {

            geneMaps = fileImporter.getGeneMaps();
            sequenceProvider = fileImporter.getSequenceProvider();
            proteinDetailsProvider = fileImporter.getProteinDetailsProvider();
            inputMap = fileImporter.getInputMap();
            proteinCount = fileImporter.getProteinCount();

            return 0;

        } else {

            return 1;

        }
    }

    /**
     * Creates a PeptideShaker project.
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler handler for exceptions
     * @param identificationParameters the identification parameters
     * @param processingParameters the processing parameters
     * @param projectType the project type
     * @param spectrumCountingParameters the spectrum counting parameters
     * @param spectrumProvider the spectrum provider
     * @param setWaitingHandlerFinshedWhenDone if true, the waiting handler is
     * set to finished when the project is created
     * @param projectDetails the project details
     *
     * @throws java.lang.InterruptedException exception thrown if a thread gets
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if a
     * process times out
     * @throws java.io.IOException if an exception occurs when parsing files
     */
    public void createProject(
            IdentificationParameters identificationParameters,
            ProcessingParameters processingParameters,
            SpectrumCountingParameters spectrumCountingParameters,
            SpectrumProvider spectrumProvider,
            ProjectDetails projectDetails,
            ProjectType projectType,
            WaitingHandler waitingHandler,
            boolean setWaitingHandlerFinshedWhenDone,
            ExceptionHandler exceptionHandler
    ) throws InterruptedException, TimeoutException, IOException {

        identification.getObjectsDB().commit();

        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(
                identification,
                identificationParameters,
                sequenceProvider,
                spectrumProvider,
                metrics,
                spectrumCountingParameters
        );
        matchesValidator = new MatchesValidator(
                new TargetDecoyMap(),
                new TargetDecoyMap(),
                new TargetDecoyMap()
        );

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        PsmScoringParameters psmScoringPreferences = identificationParameters.getPsmScoringParameters();
        FastaParameters fastaParameters = identificationParameters.getFastaParameters();
        FastaSummary fastaSummary = FastaSummary.getSummary(
                projectDetails.getFastaFile(),
                fastaParameters,
                waitingHandler
        );

        // set the background species
        identificationParameters.getGeneParameters().setBackgroundSpeciesFromFastaSummary(fastaSummary);

        ArrayList<Integer> usedAlgorithms = projectDetails.getIdentificationAlgorithms();

        if (psmScoringPreferences.isScoringNeeded(usedAlgorithms)) {

            waitingHandler.appendReport("Estimating PSM scores.", true, true);

            PsmScorer psmScorer = new PsmScorer(
                    fastaParameters,
                    sequenceProvider,
                    spectrumProvider
            );
            psmScorer.estimateIntermediateScores(
                    identification,
                    inputMap,
                    processingParameters,
                    identificationParameters,
                    waitingHandler,
                    exceptionHandler
            );

            if (psmScoringPreferences.isTargetDecoyNeededForPsmScoring(usedAlgorithms)) {

                if (fastaParameters.isTargetDecoy()) {

                    waitingHandler.appendReport("Estimating intermediate scores probabilities.", true, true);
                    psmScorer.estimateIntermediateScoreProbabilities(
                            identification,
                            inputMap,
                            processingParameters,
                            waitingHandler
                    );

                } else {

                    waitingHandler.appendReport(
                            "No decoy sequences found. Impossible to "
                            + "estimate intermediate scores probabilities.",
                            true,
                            true
                    );

                }
            }

            waitingHandler.appendReport("Scoring PSMs.", true, true);
            psmScorer.scorePsms(
                    identification,
                    inputMap,
                    processingParameters,
                    identificationParameters,
                    waitingHandler
            );
        }

        identification.getObjectsDB().commit();
        System.gc();

        if (fastaParameters.isTargetDecoy()) {

            waitingHandler.appendReport(
                    "Computing assumptions probabilities.",
                    true,
                    true
            );

        } else {

            waitingHandler.appendReport(
                    "Importing assumptions scores.",
                    true,
                    true
            );

        }

        inputMap.estimateProbabilities(waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        identification.getObjectsDB().commit();
        System.gc();

        waitingHandler.appendReport(
                "Saving assumptions probabilities, selecting best match, scoring modification localization.",
                true,
                true
        );

        PsmProcessor psmProcessor = new PsmProcessor(identification);
        psmProcessor.processPsms(
                inputMap,
                identificationParameters,
                matchesValidator,
                modificationLocalizationScorer,
                sequenceProvider,
                spectrumProvider,
                modificationFactory,
                proteinCount,
                processingParameters.getnThreads(),
                waitingHandler,
                exceptionHandler
        );
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        identification.getObjectsDB().commit();
        System.gc();

        waitingHandler.appendReport(
                "Computing PSM probabilities.",
                true,
                true
        );

        matchesValidator.getPsmMap().estimateProbabilities(waitingHandler);

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        identification.getObjectsDB().commit();
        System.gc();

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            PeptideInference peptideInference = new PeptideInference();

            ModificationLocalizationParameters modificationScoringPreferences = identificationParameters.getModificationLocalizationParameters();

            if (modificationScoringPreferences.getAlignNonConfidentModifications()) {

                waitingHandler.appendReport("Resolving peptide inference issues.", true, true);

                peptideInference.peptideInference(
                        identification,
                        identificationParameters,
                        sequenceProvider,
                        modificationFactory,
                        waitingHandler
                );

                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }
            }

            identification.getObjectsDB().commit();
            System.gc();

        }

        String reportTxt = "Saving probabilities";
        String waitingTitle = "Saving Probabilities.";;
        switch (projectType) {
            case psm:
                reportTxt += ".";
                break;
            case peptide:
                reportTxt += ", building peptides.";
                waitingTitle += " Building Peptides.";
                break;
            default:
                reportTxt += ", building peptides and proteins.";
                waitingTitle += " Building Peptides and Proteins.";
        }

        waitingHandler.appendReport(reportTxt, true, true);
        waitingHandler.setWaitingText(waitingTitle + " Please Wait...");

        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(
                sequenceProvider,
                identificationParameters.getSequenceMatchingParameters(),
                projectType,
                fastaParameters,
                waitingHandler
        );
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        identification.getObjectsDB().commit();
        System.gc();

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            waitingHandler.appendReport(
                    "Generating peptide map.",
                    true,
                    true
            );
            matchesValidator.fillPeptideMaps(
                    identification,
                    metrics,
                    waitingHandler,
                    identificationParameters,
                    sequenceProvider,
                    spectrumProvider
            );

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            identification.getObjectsDB().commit();
            System.gc();

            waitingHandler.appendReport(
                    "Computing peptide probabilities.",
                    true,
                    true
            );

            matchesValidator.getPeptideMap().estimateProbabilities(waitingHandler);

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            identification.getObjectsDB().commit();
            System.gc();

            waitingHandler.appendReport(
                    "Saving peptide probabilities.",
                    true,
                    true
            );
            matchesValidator.attachPeptideProbabilities(
                    identification,
                    fastaParameters,
                    waitingHandler
            );
            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            identification.getObjectsDB().commit();
            System.gc();

            if (projectType == ProjectType.protein) {

                if (identificationParameters.getProteinInferenceParameters().getSimplifyGroups()) {

                    waitingHandler.appendReport(
                            "Simplifying protein groups.",
                            true,
                            true
                    );

                    GroupSimplification groupSimplification = new GroupSimplification();
                    groupSimplification.removeRedundantGroups(
                            identification,
                            identificationParameters,
                            sequenceProvider,
                            proteinDetailsProvider,
                            waitingHandler
                    );
                    waitingHandler.increasePrimaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }

                identification.getObjectsDB().commit();
                System.gc();

                ProteinInference proteinInference = new ProteinInference();
                waitingHandler.appendReport(
                        "Mapping shared peptides.",
                        true,
                        true
                );
                proteinInference.distributeSharedPeptides(
                        identification,
                        waitingHandler
                );
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport(
                        "Generating protein map.",
                        true,
                        true
                );
                matchesValidator.fillProteinMap(
                        identification,
                        spectrumProvider,
                        waitingHandler
                );
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport(
                        "Selecting leading proteins, inferring peptide and protein inference status.",
                        true,
                        true
                );
                proteinInference.inferPiStatus(
                        identification,
                        metrics,
                        matchesValidator.getProteinMap(),
                        identificationParameters,
                        sequenceProvider,
                        proteinDetailsProvider,
                        waitingHandler
                );
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport(
                        "Computing protein probabilities.",
                        true,
                        true
                );

                matchesValidator.getProteinMap().estimateProbabilities(waitingHandler);

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport(
                        "Saving protein probabilities.",
                        true,
                        true
                );
                matchesValidator.attachProteinProbabilities(
                        identification,
                        sequenceProvider,
                        fastaParameters,
                        metrics,
                        waitingHandler,
                        identificationParameters.getFractionParameters()
                );
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

            }
        }

        if (fastaParameters.isTargetDecoy()) {

            IdMatchValidationParameters idMatchValidationParameters = identificationParameters.getIdValidationParameters();

            if (idMatchValidationParameters.getDefaultPsmFDR() == 1
                    && idMatchValidationParameters.getDefaultPeptideFDR() == 1
                    && idMatchValidationParameters.getDefaultProteinFDR() == 1) {

                waitingHandler.appendReport(
                        "Validating identifications at 1% FDR, quality control of matches.",
                        true,
                        true
                );

            } else {

                waitingHandler.appendReport(
                        "Validating identifications, quality control of matches.",
                        true,
                        true
                );

            }
        } else {

            waitingHandler.appendReport(
                    "Quality control of matches.",
                    true,
                    true
            );

        }

        matchesValidator.validateIdentifications(
                identification,
                metrics,
                inputMap,
                waitingHandler,
                exceptionHandler,
                identificationFeaturesGenerator,
                sequenceProvider,
                proteinDetailsProvider,
                spectrumProvider,
                geneMaps,
                identificationParameters,
                projectType,
                processingParameters
        );
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        identification.getObjectsDB().commit();
        System.gc();

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            waitingHandler.appendReport(
                    "Scoring PTMs in peptides.",
                    true,
                    true
            );
            modificationLocalizationScorer.scorePeptidePtms(
                    identification,
                    modificationFactory,
                    waitingHandler,
                    identificationParameters
            );
            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return;
            }

            identification.getObjectsDB().commit();
            System.gc();

            if (projectType == ProjectType.protein) {

                waitingHandler.appendReport(
                        "Estimating spectrum counting scaling values.",
                        true,
                        true
                );

                ScalingFactorsEstimators scalingFactors = new ScalingFactorsEstimators(spectrumCountingParameters);
                scalingFactors.estimateScalingFactors(
                        identification,
                        metrics,
                        sequenceProvider,
                        identificationFeaturesGenerator,
                        waitingHandler,
                        exceptionHandler,
                        processingParameters
                );

                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport(
                        "Scoring PTMs in proteins, gathering summary metrics.",
                        true,
                        true
                );
                ProteinProcessor proteinProcessor = new ProteinProcessor(
                        identification,
                        identificationParameters,
                        identificationFeaturesGenerator,
                        sequenceProvider
                );
                proteinProcessor.processProteins(
                        modificationLocalizationScorer,
                        metrics,
                        modificationFactory,
                        waitingHandler,
                        exceptionHandler,
                        processingParameters
                );
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                identification.getObjectsDB().commit();
                System.gc();

            }
        }

        projectCreationDuration.end();
        String report = "Identification processing completed (" + projectCreationDuration.toString() + ").";

        waitingHandler.appendReport(
                report,
                true,
                true
        );
        waitingHandler.appendReportEndLine();
        waitingHandler.appendReportEndLine();
        identification.addUrParam(
                new PSMaps(
                        inputMap,
                        matchesValidator.getPsmMap(),
                        matchesValidator.getPeptideMap(),
                        matchesValidator.getProteinMap()
                )
        );

        if (setWaitingHandlerFinshedWhenDone) {
            waitingHandler.setRunFinished();
        }
    }

    /**
     * Processes the identifications if a change occurred in the PSM map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param processingPreferences the processing preferences
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     * @param spectrumProvider the spectrum provider
     * @param projectType the project type
     */
    public void spectrumMapChanged(
            Identification identification,
            WaitingHandler waitingHandler,
            ProcessingParameters processingPreferences,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            ProjectType projectType
    ) {

        FastaParameters fastaParameters = identificationParameters.getFastaParameters();
        FractionParameters fractionParameters = identificationParameters.getFractionParameters();

        TargetDecoyMap peptideMap = new TargetDecoyMap();
        TargetDecoyMap proteinMap = new TargetDecoyMap();
        matchesValidator.setPeptideMap(peptideMap);
        matchesValidator.setProteinMap(proteinMap);
        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(
                sequenceProvider,
                identificationParameters.getSequenceMatchingParameters(),
                projectType,
                fastaParameters,
                waitingHandler
        );
        matchesValidator.fillPeptideMaps(
                identification,
                metrics,
                waitingHandler,
                identificationParameters,
                sequenceProvider,
                spectrumProvider
        );
        peptideMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachPeptideProbabilities(
                identification,
                fastaParameters,
                waitingHandler
        );
        matchesValidator.fillProteinMap(
                identification,
                spectrumProvider,
                waitingHandler
        );
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(
                identification,
                sequenceProvider,
                fastaParameters,
                metrics,
                waitingHandler,
                fractionParameters
        );
    }

    /**
     * Processes the identifications if a change occurred in the peptide map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     * @param spectrumProvider the spectrum provider
     */
    public void peptideMapChanged(
            Identification identification,
            WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider
    ) {

        FastaParameters fastaParameters = identificationParameters.getFastaParameters();
        FractionParameters fractionParameters = identificationParameters.getFractionParameters();

        TargetDecoyMap proteinMap = new TargetDecoyMap();
        matchesValidator.setProteinMap(proteinMap);
        matchesValidator.attachPeptideProbabilities(
                identification,
                fastaParameters,
                waitingHandler
        );
        matchesValidator.fillProteinMap(
                identification,
                spectrumProvider,
                waitingHandler
        );
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(
                identification,
                sequenceProvider,
                fastaParameters,
                metrics,
                waitingHandler,
                fractionParameters
        );

    }

    /**
     * Processes the identifications if a change occurred in the protein map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     */
    public void proteinMapChanged(
            Identification identification,
            WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider
    ) {

        FastaParameters fastaParameters = identificationParameters.getFastaParameters();
        FractionParameters fractionParameters = identificationParameters.getFractionParameters();

        matchesValidator.attachProteinProbabilities(
                identification,
                sequenceProvider,
                fastaParameters,
                metrics,
                waitingHandler,
                fractionParameters
        );

    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum
     * matches and creates peptides and proteins.
     *
     * @param sequenceProvider a protein sequence provider
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param projectType the project type
     * @param fastaParameters the FASTA parsing parameters
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingPreferences,
            ProjectType projectType,
            FastaParameters fastaParameters,
            WaitingHandler waitingHandler
    ) {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        try (PeptideAndProteinBuilder peptideAndProteinBuilder = new PeptideAndProteinBuilder(identification)) {

            identification.getSpectrumIdentification().values().stream()
                    .flatMap(keys -> keys.stream())
                    .parallel()
                    .map(
                            key -> identification.getSpectrumMatch(key)
                    )
                    .forEach(
                            spectrumMatch -> attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(
                                    spectrumMatch,
                                    peptideAndProteinBuilder,
                                    sequenceProvider,
                                    sequenceMatchingPreferences,
                                    projectType,
                                    fastaParameters,
                                    waitingHandler
                            )
                    );
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum match
     * and creates peptides and proteins.
     *
     * @param spectrumMatch the spectrum match to process
     * @param sequenceProvider a protein sequence provider
     * @param peptideAndProteinBuilder a peptide and protein builder
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param projectType the project type
     * @param fastaParameters the FASTA parsing parameters
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(
            SpectrumMatch spectrumMatch,
            PeptideAndProteinBuilder peptideAndProteinBuilder,
            SequenceProvider sequenceProvider,
            SequenceMatchingParameters sequenceMatchingPreferences,
            ProjectType projectType,
            FastaParameters fastaParameters,
            WaitingHandler waitingHandler
    ) {

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

        if (spectrumMatch.getBestPeptideAssumption() == null) {
            return;
        }

        if (fastaParameters.isTargetDecoy()) {

            double probability = matchesValidator.getPsmMap().getProbability(psParameter.getScore());
            psParameter.setProbability(probability);

        } else {

            psParameter.setProbability(1.0);

        }

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            peptideAndProteinBuilder.buildPeptidesAndProteins(
                    spectrumMatch,
                    sequenceMatchingPreferences,
                    sequenceProvider,
                    projectType == ProjectType.protein
            );

        }
        identification.updateObject(spectrumMatch.getKey(), spectrumMatch);

        waitingHandler.increaseSecondaryProgressCounter();

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

    /**
     * Returns the identification object.
     *
     * @return the identification object
     */
    public Identification getIdentification() {

        return identification;

    }

    /**
     * Returns the sequence provider.
     *
     * @return the sequence provider
     */
    public SequenceProvider getSequenceProvider() {

        return sequenceProvider;

    }

    /**
     * Returns the protein details provider.
     *
     * @return the protein details provider
     */
    public ProteinDetailsProvider getProteinDetailsProvider() {
        return proteinDetailsProvider;
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

        return matchesParentDirectory.equals("resources")
                ? new File(getJarFilePath(), matchesParentDirectory) : new File(matchesParentDirectory);

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
    public static void instantiateFacories(
            UtilitiesUserParameters utilitiesUserPreferences
    ) {

        EnzymeFactory.getInstance();
        ModificationFactory.getInstance();

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
     * Retrieves the path to the jar file.
     *
     * @return the path to the jar file
     */
    public static String getJarFilePath() {

        return CompomicsWrapper.getJarFilePath((new PeptideShaker()).getClass().getResource("PeptideShaker.class").getPath(), "PeptideShaker");

    }
}
