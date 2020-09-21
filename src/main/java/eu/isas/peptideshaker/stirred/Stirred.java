package eu.isas.peptideshaker.stirred;

import com.compomics.software.log.CliLogger;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.fm_index.FMIndex;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.identification.writers.SimpleMzIdentMLExporter;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.experiment.io.temp.TempFilesManager;
import com.compomics.util.io.IoUtil;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.Duration;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fileimport.PsmImporter;
import eu.isas.peptideshaker.stirred.modules.IdImporter;
import eu.isas.peptideshaker.stirred.modules.StirRunnable;
import eu.isas.peptideshaker.utils.PsZipUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
    private final File inputFile;
    /**
     * The spectrum file.
     */
    private final File spectrumFile;
    /**
     * The FASTA file.
     */
    private final File fastaFile;
    /**
     * The output file.
     */
    private final File ouputFolder;
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
     * The name of the organization of the contact to annotate in the mzIdentML
     * file.
     */
    private final String contactOrganizationName;
    /**
     * The address of the organization of the contact to annotate in the
     * mzIdentML file.
     */
    private final String contactOrganizationAddress;
    /**
     * The email of the organization of the contact to annotate in the mzIdentML
     * file.
     */
    private final String contactOrganizationEmail;
    /**
     * The total duration
     */
    private final Duration totalDuration = new Duration();

    /**
     * Constructor.
     *
     * @param inputFile The input file.
     * @param spectrumFile The spectrum file.
     * @param fastaFile The fasta file.
     * @param ouputFolder The output folder.
     * @param identificationParametersFile The identification parameters file.
     * @param tempFolder The temp folder to use for temp files.
     * @param cliLogger The temp folder to use for temp files.
     * @param nThreads The number of threads to use.
     * @param timeOutDays Timeout time in days.
     * @param contactFirstName The first name of the contact to annotate in the
     * mzIdentML file.
     * @param contactLastName The last name of the contact to annotate in the
     * mzIdentML file.
     * @param contactAddress The address of the contact to annotate in the
     * mzIdentML file.
     * @param contactEmail The email of the contact to annotate in the mzIdentML
     * file.
     * @param contactOrganizationName The name of the organization of the
     * contact to annotate in the mzIdentML file.
     * @param contactOrganizationAddress The address of the organization of the
     * contact to annotate in the mzIdentML file.
     * @param contactOrganizationEmail The email of the organization of the
     * contact to annotate in the mzIdentML file.
     */
    public Stirred(
            File inputFile,
            File spectrumFile,
            File fastaFile,
            File ouputFolder,
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

        this.inputFile = inputFile;
        this.spectrumFile = spectrumFile;
        this.fastaFile = fastaFile;
        this.ouputFolder = ouputFolder;
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

    /**
     * Runs the stirred process.
     *
     * @throws InterruptedException Exception thrown if a thread is interrupted.
     * @throws TimeoutException Exception thrown if the process times out.
     * @throws IOException Exception thrown if an error occurred while reading
     * or writing a file.
     */
    public void run() throws InterruptedException, TimeoutException, IOException {

        // Start
        cliLogger.logMessage("Stirred process start");
        totalDuration.start();

        String inputFileName = inputFile.getName();

        if (inputFileName.toLowerCase().endsWith(".zip")) {

            // SearchGUI zip file
            File unzipFolder = new File(tempFolder, PsZipUtils.getUnzipSubFolder());
            unzipFolder.mkdir();
            TempFilesManager.registerTempFolder(unzipFolder);
            ZipUtils.unzip(inputFile, unzipFolder, null);

            // Get the files
            File dataFasta = fastaFile;
            File dataIdentificationParametersFile = identificationParametersFile;
            ArrayList<File> spectrumFiles = new ArrayList<>();
            ArrayList<File> searchEngineResultsFiles = new ArrayList<>();

            if (spectrumFile != null) {

                spectrumFiles.add(spectrumFile);

            }

            // List the files to inspect
            ArrayList<File> files = Arrays.stream(unzipFolder.listFiles())
                    .collect(
                            Collectors.toCollection(ArrayList::new)
                    );

            File sguiInputFile = new File(unzipFolder, "searchGUI_input.txt");

            if (sguiInputFile.exists()) {

                SimpleFileReader reader = SimpleFileReader.getFileReader(sguiInputFile);

                String line;
                while ((line = reader.readLine()) != null) {

                    if (line.charAt(0) != '#') {

                        files.add(new File(line));

                    }
                }
            }

            File dataFolder = new File(unzipFolder, PeptideShaker.DATA_DIRECTORY);

            if (dataFolder.exists() && dataFolder.isDirectory()) {

                files.addAll(
                        Arrays.stream(dataFolder.listFiles())
                                .collect(
                                        Collectors.toCollection(ArrayList::new)
                                )
                );
            }

            // Inspect all files
            for (File file : files) {

                String fileNameLowerCase = file.getName().toLowerCase();

                if (fastaFile == null) {

                    if (fileNameLowerCase.endsWith(".fasta")) {

                        if (dataFasta != null) {

                            throw new IllegalArgumentException("More than one fasta file provided in the data folder, please specify the file to use via the command line option.");

                        }

                        dataFasta = file;

                    }
                }

                if (identificationParametersFile == null) {

                    if (fileNameLowerCase.endsWith(".par")) {

                        if (dataIdentificationParametersFile != null) {

                            throw new IllegalArgumentException("More than one identification parameters file provided in the data folder, please specify the file to use via the command line option.");

                        }

                        dataIdentificationParametersFile = file;

                    }
                }

                if (spectrumFile == null) {

                    if (fileNameLowerCase.endsWith(".mzml")
                            || fileNameLowerCase.endsWith(".mgf")) {

                        spectrumFiles.add(file);

                    }
                }

                if (fileNameLowerCase.endsWith(".omx")
                        || fileNameLowerCase.endsWith(".t.xml")
                        || fileNameLowerCase.endsWith(".pep.xml")
                        || fileNameLowerCase.endsWith(".dat")
                        || fileNameLowerCase.endsWith(".mzid")
                        || fileNameLowerCase.endsWith(".ms-amanda.csv")
                        || fileNameLowerCase.endsWith(".res")
                        || fileNameLowerCase.endsWith(".tide-search.target.txt")
                        || fileNameLowerCase.endsWith(".tags")
                        || fileNameLowerCase.endsWith(".pnovo.txt")
                        || fileNameLowerCase.endsWith(".novor.csv")
                        || fileNameLowerCase.endsWith(".psm")
                        || fileNameLowerCase.endsWith(".omx.gz")
                        || fileNameLowerCase.endsWith(".t.xml.gz")
                        || fileNameLowerCase.endsWith(".pep.xml.gz")
                        || fileNameLowerCase.endsWith(".mzid.gz")
                        || fileNameLowerCase.endsWith(".ms-amanda.csv.gz")
                        || fileNameLowerCase.endsWith(".res.gz")
                        || fileNameLowerCase.endsWith(".tide-search.target.txt.gz")
                        //                            || fileNameLowerCase.endsWith(".tags.gz")
                        //                            || fileNameLowerCase.endsWith(".pnovo.txt.gz")
                        //                            || fileNameLowerCase.endsWith(".novor.csv.gz")
                        || fileNameLowerCase.endsWith(".psm.gz")) {

                    searchEngineResultsFiles.add(file);

                }
            }

            // Sanity check
            if (searchEngineResultsFiles.isEmpty()) {

                throw new FileNotFoundException("No identification results found in " + inputFile + ".");

            }
            if (spectrumFiles.isEmpty()) {

                throw new FileNotFoundException("No spectrum file found in " + inputFile + ".");

            }
            if (dataFasta == null) {

                throw new FileNotFoundException("No fasta file found in " + inputFile + ".");

            }
            if (dataIdentificationParametersFile == null) {

                throw new FileNotFoundException("No identification parameters file found in " + inputFile + ".");

            }

            // Import identification parameters
            cliLogger.logMessage("    Importing identification parameters file from " + dataIdentificationParametersFile);
            IdentificationParameters identificationParameters = IdentificationParameters.getIdentificationParameters(dataIdentificationParametersFile);

            // Import fasta file
            cliLogger.logMessage("    Importing protein sequences from " + dataFasta);
            FMIndex fmIndex = new FMIndex(
                    dataFasta,
                    identificationParameters.getFastaParameters(),
                    null,
                    true,
                    identificationParameters.getSearchParameters(),
                    identificationParameters.getPeptideVariantsParameters()  
            );
            FastaSummary fastaSummary = FastaSummary.getSummary(
                    dataFasta.getAbsolutePath(),
                    identificationParameters.getFastaParameters(),
                    null
            );

            // Import spectrum files
            MsFileHandler msFileHandler = new MsFileHandler();

            for (File spectrumFile : spectrumFiles) {

                cliLogger.logMessage("    Importing spectra from " + spectrumFile);
                msFileHandler.register(
                        spectrumFile,
                        tempFolder,
                        null
                );

            }

            File finalFasta = dataFasta;

            // Process search engine results in parallel
            searchEngineResultsFiles.stream()
                    .parallel()
                    .forEach(
                            searchEngineResultsFile -> {
                                try {

                                    // Create output file
                                    File outputFile = new File(ouputFolder, IoUtil.removeExtension(searchEngineResultsFile.getName()) + ".stirred.mzid.gz");

                                    // Run on the provided files
                                    process(
                                            searchEngineResultsFile,
                                            finalFasta,
                                            outputFile,
                                            fmIndex,
                                            fastaSummary,
                                            msFileHandler,
                                            identificationParameters
                                    );
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );

        } else {

            // Sanity check
            if (spectrumFile == null || !spectrumFile.exists()) {

                throw new FileNotFoundException("Spectrum file '" + spectrumFile + "' not found.");

            }
            if (fastaFile == null || !fastaFile.exists()) {

                throw new FileNotFoundException("Fasta file '" + fastaFile + "' not found.");

            }
            if (identificationParametersFile == null || !identificationParametersFile.exists()) {

                throw new FileNotFoundException("Fasta file '" + identificationParametersFile + "' not found.");

            }

            // Import identification parameters
            cliLogger.logMessage("    Importing identification parameters file from " + identificationParametersFile);
            IdentificationParameters identificationParameters = IdentificationParameters.getIdentificationParameters(identificationParametersFile);

            // Import fasta file
            cliLogger.logMessage("    Importing protein sequences from " + fastaFile);
            FMIndex fmIndex = new FMIndex(
                    fastaFile,
                    identificationParameters.getFastaParameters(),
                    null,
                    true,
                    identificationParameters.getSearchParameters(),
                    identificationParameters.getPeptideVariantsParameters()
            );
            FastaSummary fastaSummary = FastaSummary.getSummary(
                    fastaFile.getAbsolutePath(),
                    identificationParameters.getFastaParameters(),
                    null
            );

            // Import spectrum file
            cliLogger.logMessage("    Importing spectra from " + spectrumFile);
            MsFileHandler msFileHandler = new MsFileHandler();
            msFileHandler.register(
                    spectrumFile,
                    tempFolder,
                    null
            );

            // Create output file
            File outputFile = new File(ouputFolder, IoUtil.removeExtension(inputFileName) + ".stirred.mzid.gz");

            // Run on the provided files
            process(
                    inputFile,
                    fastaFile,
                    outputFile,
                    fmIndex,
                    fastaSummary,
                    msFileHandler,
                    identificationParameters
            );

        }

        // Done
        totalDuration.end();
        cliLogger.logMessage("Stirred process completed (" + totalDuration.toString() + ")");

    }

    /**
     * Runs the stirred process.
     *
     * @param searchEngineResultsFile The search engine results file.
     * @param fastaFile The fasta file.
     * @param ouputFile The output file.
     * @param fmIndex The FM index of the fasta file.
     * @param fastaSummary The summary information on the fasta file.
     * @param msFileHandler The mass spectrometry file handler to use.
     * @param identificationParameters The identification parameters.
     *
     * @throws InterruptedException Exception thrown if a thread is interrupted.
     * @throws TimeoutException Exception thrown if the process times out.
     * @throws IOException Exception thrown if an error occurred while reading
     * or writing a file.
     */
    public void process(
            File searchEngineResultsFile,
            File fastaFile,
            File ouputFile,
            FMIndex fmIndex,
            FastaSummary fastaSummary,
            MsFileHandler msFileHandler,
            IdentificationParameters identificationParameters
    ) throws InterruptedException, TimeoutException, IOException {

        // Start
        String idFileName = searchEngineResultsFile.getName();
        cliLogger.logMessage("Starting stirred process of " + idFileName);

        // Import identification results
        cliLogger.logMessage(idFileName + ": Parsing identification results");
        IdImporter idImporter = new IdImporter(
                searchEngineResultsFile,
                cliLogger
        );
        ArrayList<SpectrumMatch> spectrumMatches = idImporter.loadSpectrumMatches(
                identificationParameters,
                msFileHandler,
                null
        );
        HashMap<String, ArrayList<String>> softwareVersions = idImporter.getIdFileReader().getSoftwareVersions();

        // Get the spectrum files and engines that were used for the identification
        TreeSet<String> fileNames = new TreeSet<>();
        HashSet<Integer> advocates = new HashSet<>();

        for (SpectrumMatch spectrumMatch : spectrumMatches) {

            fileNames.add(spectrumMatch.getSpectrumFile());

            advocates.addAll(spectrumMatch.getAdvocates());

        }

        ArrayList<File> spectrumFiles = new ArrayList<>(fileNames.size());

        for (String fileName : fileNames) {

            String filePath = msFileHandler.getFilePaths().get(fileName);

            if (filePath == null) {

                throw new FileNotFoundException("Spectrum file " + fileName + " used to create " + searchEngineResultsFile + " not found.");

            }

            spectrumFiles.add(new File(filePath));

        }

        // Check that X!Tandem quick modification searches are in the search parameters.
        if (advocates.contains(Advocate.xtandem.getIndex())) {

            PsmImporter.verifyXTandemModifications(identificationParameters);

        }

        // Stir peptides and export
        cliLogger.logMessage(idFileName + ": Processing peptides from " + spectrumMatches.size() + " spectra");
        
        
        try ( SimpleMzIdentMLExporter simpleMzIdentMLExporter = new SimpleMzIdentMLExporter(
                SOFTWARE_NAME,
                SOFTWARE_VERSION,
                SOFTWARE_URL,
                tempFolder,
                ouputFile,
                spectrumFiles,
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
                contactOrganizationEmail,
                false
        )) {

            ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchesQueue = new ConcurrentLinkedQueue<>(spectrumMatches);

            ExecutorService pool = Executors.newFixedThreadPool(nThreads);

            ArrayList<StirRunnable> runnables = new ArrayList<>(nThreads);

            for (int i = 0; i < nThreads; i++) {

                StirRunnable runnable = new StirRunnable(
                        spectrumMatchesQueue,
                        idImporter.getIdFileReader(),
                        simpleMzIdentMLExporter,
                        identificationParameters,
                        fmIndex,
                        fmIndex,
                        msFileHandler,
                        cliLogger
                );
                runnables.add(runnable);
                pool.submit(runnable);

            }

            pool.shutdown();

            if (!pool.awaitTermination(timeOutDays, TimeUnit.DAYS)) {

                throw new TimeoutException("Analysis timed out (time out: " + timeOutDays + " days)");

            }

            int nPeptides = runnables.stream()
                    .mapToInt(
                            StirRunnable::getnPeptides
                    )
                    .sum();
            int nModificationIssues = runnables.stream()
                    .mapToInt(
                            StirRunnable::getnModificationIssues
                    )
                    .sum();
            double percentModificationIssues = Util.roundDouble(100.0 * nModificationIssues / nPeptides, 1);

            cliLogger.logMessage(idFileName + ": " + nPeptides + " peptides processed.");

            if (nModificationIssues > 0) {

                cliLogger.logMessage(idFileName + ": " + nModificationIssues + " peptides (" + percentModificationIssues + "%) excluded due to unrecognized modification.");

            }
        }

        // Done
        cliLogger.logMessage(idFileName + ": completed.");

    }
}
