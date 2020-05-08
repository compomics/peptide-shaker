package eu.isas.peptideshaker.stirred;

import com.compomics.software.log.CliLogger;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.fm_index.FMIndex;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.identification.writers.SimpleMzIdentMLExporter;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.stirred.modules.IdImporter;
import eu.isas.peptideshaker.stirred.modules.StirRunnable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

/**
 * This class imports the results of a search engine from SearchGUI in the
 * utilities model.
 *
 * @author Marc Vaudel
 */
public class Stirred {

    /**
     * The name of the software to annotate in the mzIdentML file.
     */
    public final String SOFTWARE_NAME = "PeptideShaker";
    /**
     * The version of the software to annotate in the mzIdentML file.
     */
    public final String SOFTWARE_VERSION = PeptideShaker.getVersion();
    /**
     * The URL of the software to annotate in the mzIdentML file.
     */
    public final String SOFTWARE_URL = "https://compomics.github.io/projects/peptide-shaker.html";
    /**
     * The compomics PTM factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * The search engine results file.
     */
    private final File searchEngineResultsFile;
    /**
     * The spectrum file.
     */
    private final File spectrumFile;
    /**
     * The fasta file.
     */
    private final File fastaFile;
    /**
     * The output file.
     */
    private final File ouputFile;
    /**
     * The identification parameters file.
     */
    private final File identificationParametersFile;
    /**
     * The temp folder to use for temp files.
     */
    private final File tempFolder;
    /**
     * The logger for CLI feedback.
     */
    private final CliLogger cliLogger;
    /**
     * The waiting handler to use.
     */
    private final WaitingHandler waitingHandler = new WaitingHandlerCLIImpl();
    /**
     * The number of threads to use.
     */
    private final int nThreads;
    /**
     * Timeout time in days.
     */
    private final int timeOutDays;
    /**
     * The first name of the contact to annotate in the mzIdentML file.
     */
    private final String contactFirstName;
    /**
     * The last name of the contact to annotate in the mzIdentML file.
     */
    private final String contactLastName;
    /**
     * The address of the contact to annotate in the mzIdentML file.
     */
    private final String contactAddress;
    /**
     * The email of the contact to annotate in the mzIdentML file.
     */
    private final String contactEmail;
    /**
     * The name of the organization of the contact to annotate in the mzIdentML file.
     */
    private final String contactOrganizationName;
    /**
     * The address of the organization of the contact to annotate in the mzIdentML file.
     */
    private final String contactOrganizationAddress;
    /**
     * The email of the organization of the contact to annotate in the mzIdentML file.
     */
    private final String contactOrganizationEmail;

    /**
     * Constructor.
     * 
     * @param searchEngineResultsFile The search engine results file.
     * @param spectrumFile The spectrum file.
     * @param fastaFile The fasta file.
     * @param ouputFile The output file.
     * @param identificationParametersFile The identification parameters file.
     * @param tempFolder The temp folder to use for temp files.
     * @param cliLogger The temp folder to use for temp files.
     * @param nThreads The number of threads to use.
     * @param timeOutDays Timeout time in days.
     * @param contactFirstName The first name of the contact to annotate in the mzIdentML file.
     * @param contactLastName The last name of the contact to annotate in the mzIdentML file.
     * @param contactAddress The address of the contact to annotate in the mzIdentML file.
     * @param contactEmail The email of the contact to annotate in the mzIdentML file.
     * @param contactOrganizationName The name of the organization of the contact to annotate in the mzIdentML file.
     * @param contactOrganizationAddress The address of the organization of the contact to annotate in the mzIdentML file.
     * @param contactOrganizationEmail The email of the organization of the contact to annotate in the mzIdentML file.
     */
    public Stirred(
            File searchEngineResultsFile,
            File spectrumFile,
            File fastaFile,
            File ouputFile,
            File identificationParametersFile,
            File tempFolder,
            CliLogger cliLogger,
            int nThreads,
            int timeOutDays,
            String contactFirstName,
            String contactLastName,
            String contactAddress,
            String contactEmail,
            String contactOrganizationName,
            String contactOrganizationAddress,
            String contactOrganizationEmail
    ) {

        this.searchEngineResultsFile = searchEngineResultsFile;
        this.spectrumFile = spectrumFile;
        this.fastaFile = fastaFile;
        this.ouputFile = ouputFile;
        this.identificationParametersFile = identificationParametersFile;
        this.tempFolder = tempFolder;
        this.cliLogger = cliLogger;
        this.nThreads = nThreads;
        this.timeOutDays = timeOutDays;
        this.contactFirstName = contactFirstName;
        this.contactLastName = contactLastName;
        this.contactAddress = contactAddress;
        this.contactEmail = contactEmail;
        this.contactOrganizationName = contactOrganizationName;
        this.contactOrganizationAddress = contactOrganizationAddress;
        this.contactOrganizationEmail = contactOrganizationEmail;

    }

    public void run() throws InterruptedException, TimeoutException, IOException {

        // Import identification parameters
        cliLogger.logMessage("Importing identification parameters file");
        IdentificationParameters identificationParameters = IdentificationParameters.getIdentificationParameters(identificationParametersFile);

        // Import fasta file
        cliLogger.logMessage("Importing protein sequences");
        FMIndex fmIndex = new FMIndex(
                fastaFile,
                identificationParameters.getFastaParameters(),
                waitingHandler,
                true,
                identificationParameters.getSearchParameters().getModificationParameters(),
                identificationParameters.getPeptideVariantsParameters()
        );
        FastaSummary fastaSummary = FastaSummary.getSummary(
                fastaFile.getAbsolutePath(),
                identificationParameters.getFastaParameters(),
                null
        );

        // Import spectrum file
        cliLogger.logMessage("Importing spectra");
        MsFileHandler msFileHandler = new MsFileHandler();
        msFileHandler.register(
                fastaFile,
                tempFolder,
                waitingHandler
        );

        // Import identification results
        IdImporter idImporter = new IdImporter(
                searchEngineResultsFile,
                cliLogger
        );
        ArrayList<SpectrumMatch> spectrumMatches = idImporter.loadSpectrumMatches(
                identificationParameters,
                msFileHandler,
                waitingHandler
        );
        HashMap<String, ArrayList<String>> softwareVersions = idImporter.getIdFileReader().getSoftwareVersions();

        // Stir peptides and export
        try ( SimpleMzIdentMLExporter simpleMzIdentMLExporter = new SimpleMzIdentMLExporter(
                SOFTWARE_NAME,
                SOFTWARE_VERSION,
                SOFTWARE_URL,
                tempFolder,
                ouputFile,
                spectrumFile,
                searchEngineResultsFile,
                softwareVersions,
                fastaFile,
                identificationParameters,
                fmIndex,
                fmIndex,
                msFileHandler,
                modificationFactory,
                fastaSummary,
                contactFirstName,
                contactLastName,
                contactAddress,
                contactEmail,
                contactOrganizationName,
                contactOrganizationAddress,
                contactOrganizationEmail
        )) {

            ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchesQueue = new ConcurrentLinkedQueue<>(spectrumMatches);

            ExecutorService pool = Executors.newFixedThreadPool(nThreads);

            IntStream.range(0, nThreads)
                    .mapToObj(
                            i -> new StirRunnable(
                                    spectrumMatchesQueue,
                                    idImporter.getIdFileReader(),
                                    IoUtil.getFileName(spectrumFile),
                                    simpleMzIdentMLExporter,
                                    identificationParameters,
                                    fmIndex,
                                    fmIndex,
                                    msFileHandler
                            )
                    )
                    .forEach(
                            worker -> pool.submit(worker)
                    );

            pool.shutdown();

            if (!pool.awaitTermination(timeOutDays, TimeUnit.DAYS)) {

                throw new TimeoutException("Analysis timed out (time out: " + timeOutDays + " days)");

            }
        }
    }
}
