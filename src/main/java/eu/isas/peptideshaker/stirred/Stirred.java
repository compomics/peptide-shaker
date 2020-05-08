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

    public final String SOFTWARE_NAME = "PeptideShaker";
    public final String SOFTWARE_VERSION = PeptideShaker.getVersion();
    public final String SOFTWARE_URL = "https://compomics.github.io/projects/peptide-shaker.html";
    /**
     * The compomics PTM factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    private final File searchEngineResultsFile;
    private final File spectrumFile;
    private final File fastaFile;
    private final File ouputFile;
    private final File identificationParametersFile;
    private final File tempFolder;
    private final CliLogger cliLogger;
    private final WaitingHandler waitingHandler = new WaitingHandlerCLIImpl();
    private final int nThreads;
    private final int timeOutDays;
    private final String contactFirstName;
    private final String contactLastName;
    private final String contactAddress;
    private final String contactEmail;
    private final String contactOrganizationName;
    private final String contactOrganizationAddress;
    private final String contactOrganizationEmail;

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
