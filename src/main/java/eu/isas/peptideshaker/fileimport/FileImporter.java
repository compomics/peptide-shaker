package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.IdfileReaderFactory;
import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.biology.genes.ProteinGeneDetailsProvider;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.identification.protein_inference.fm_index.FMIndex;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.parameters.identification.advanced.GeneParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.PeptideVariantsParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.protein_inference.TagMapper;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.io.IoUtil;
import eu.isas.peptideshaker.PeptideShaker;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * This class is responsible for the import of identifications.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FileImporter {

    /**
     * A dialog to display feedback to the user.
     */
    private final WaitingHandler waitingHandler;
    /**
     * An exception handler to handle exceptions.
     */
    private final ExceptionHandler exceptionHandler;
    /**
     * A provider for protein sequences.
     */
    private SequenceProvider sequenceProvider;
    /**
     * A provider for protein details.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The spectrum provider.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * Map of the spectra loaded.
     */
    private final HashMap<String, HashSet<String>> loadedSpectraMap;
    /**
     * Summary information on the FASTA file.
     */
    private FastaSummary fastaSummary;
    /**
     * Metrics of the data set extracted while loading the data.
     */
    private final Metrics metrics;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The identification file reader factory of compomics utilities.
     */
    private final IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
    /**
     * The processing parameters.
     */
    private final ProcessingParameters processingParameters;
    /**
     * The project details.
     */
    private final ProjectDetails projectDetails;
    /**
     * The number of retained first hits.
     */
    private long nRetained = 0;
    /**
     * The input map.
     */
    private final InputMap inputMap = new InputMap();
    /**
     * Boolean indicating whether we can display GUI stuff.
     */
    private boolean hasGUI = false;
    /**
     * The database connection.
     */
    private final Identification identification;
    /**
     * A FASTA file mapper.
     */
    private FastaMapper fastaMapper;
    /**
     * A tag to protein mapper.
     */
    private final TagMapper tagMapper;
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private final HashMap<String, Integer> proteinCount = new HashMap<>(10000);
    /**
     * The number of first hits.
     */
    private long nPSMs = 0;
    /**
     * The total number of hits.
     */
    private long nTotal = 0;
    /**
     * The genes maps.
     */
    private GeneMaps geneMaps;

    /**
     * Constructor for the importer.
     *
     * @param identification The identification where to store the matches.
     * @param processingParameters The processing parameters.
     * @param identificationParameters The identification parameters.
     * @param projectDetails The project details.
     * @param metrics The metrics of the data set to be saved.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The handler displaying feedback to the user.
     * @param exceptionHandler The exception handler.
     */
    public FileImporter(
            Identification identification,
            IdentificationParameters identificationParameters,
            ProcessingParameters processingParameters,
            Metrics metrics,
            ProjectDetails projectDetails,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) {

        this.identificationParameters = identificationParameters;
        this.metrics = metrics;
        this.processingParameters = processingParameters;
        this.projectDetails = projectDetails;
        this.spectrumProvider = spectrumProvider;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;

        tagMapper = new TagMapper(identificationParameters, exceptionHandler);

        loadedSpectraMap = new HashMap<>(spectrumProvider.getOrderedFileNamesWithoutExtensions().length);

        for (String fileNameWithoutExtension : spectrumProvider.getOrderedFileNamesWithoutExtensions()) {

            loadedSpectraMap.put(
                    fileNameWithoutExtension,
                    Arrays.stream(spectrumProvider.getSpectrumTitles(fileNameWithoutExtension))
                            .collect(
                                    Collectors.toCollection(HashSet::new)
                            )
            );
        }
    }

    /**
     * Imports the identifications from the files.
     *
     * @param idFiles the identification files
     *
     * @return 0 if success, 1 if not
     */
    public int importFiles(
            ArrayList<File> idFiles
    ) {

        ArrayList<File> sortedIdFiles = idFiles.stream()
                .collect(
                        Collectors.groupingBy(
                                File::getName,
                                TreeMap::new,
                                Collectors.toList()
                        )
                )
                .values().stream()
                .flatMap(
                        List::stream
                )
                .distinct()
                .collect(
                        Collectors.toCollection(ArrayList::new)
                );

        try {

            importSequences(
                    identificationParameters.getSequenceMatchingParameters(),
                    identificationParameters.getSearchParameters(),
                    identificationParameters.getFastaParameters(),
                    identificationParameters.getPeptideVariantsParameters(),
                    waitingHandler,
                    exceptionHandler
            );

            if (waitingHandler.isRunCanceled()) {

                return 1;

            }

            GeneParameters geneParameters = identificationParameters.getGeneParameters();

            if (geneParameters.getUseGeneMapping()) {

                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                waitingHandler.appendReport(
                        "Importing gene mappings.",
                        true,
                        true
                );
                importGenes();

            } else {

                geneMaps = new GeneMaps();

            }

            if (waitingHandler.isRunCanceled()) {

                return 1;

            }

            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            waitingHandler.appendReport(
                    "Establishing local database connection.",
                    true,
                    true
            );

            waitingHandler.increasePrimaryProgressCounter();

            if (!waitingHandler.isRunCanceled()) {

                waitingHandler.appendReport(
                        "Reading identification files.",
                        true,
                        true
                );

                for (File idFile : sortedIdFiles) {

                    importPsms(idFile);

                    if (waitingHandler.isRunCanceled()) {

                        return 1;
                    }
                }

                if (nRetained == 0) {

                    waitingHandler.appendReport(
                            "No identification results.",
                            true,
                            true
                    );

                    waitingHandler.setRunCanceled();

                    return 1;

                }

                // get the total number of spectra
                int nSpectra = 0;

                for (String spectrumFileName : identification.getFractions()) {
                    nSpectra += spectrumProvider.getSpectrumTitles(spectrumFileName).length;
                }

                waitingHandler.appendReport(
                        "File import completed. "
                        + nPSMs + " first hits imported (" + nTotal + " total) from " + nSpectra + " spectra.",
                        true,
                        true
                );

                waitingHandler.appendReport(
                        "[" + nRetained + " first hits passed the initial filtering]",
                        true,
                        true
                );

            }

        } catch (OutOfMemoryError error) {

            System.out.println("<CompomicsError>PeptideShaker ran out of memory! See the PeptideShaker log for details.</CompomicsError>");
            System.err.println("Ran out of memory!");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");

            Runtime.getRuntime().gc();
            waitingHandler.appendReportEndLine();
            waitingHandler.appendReport("Ran out of memory!", true, true);
            waitingHandler.setRunCanceled();

            if (waitingHandler instanceof WaitingDialog) {

                JOptionPane.showMessageDialog(
                        (WaitingDialog) waitingHandler, JOptionEditorPane.getJOptionEditorPane(
                                "PeptideShaker used up all the available memory and had to be stopped.<br>"
                                + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                                + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                                + "Java Options). See also <a href=\"https://compomics.github.io/projects/compomics-utilities/wiki/JavaTroubleShooting.html\">JavaTroubleShooting</a>."
                        ),
                        "Out Of Memory",
                        JOptionPane.ERROR_MESSAGE
                );

            }

            error.printStackTrace();

            return 1;

        } catch (Exception e) {

            waitingHandler.setRunCanceled();

            System.out.println("<CompomicsError>PeptideShaker processing failed. See the PeptideShaker log for details.</CompomicsError>");

            if (e instanceof NullPointerException) {

                waitingHandler.appendReport(
                        "An error occurred while loading the identification files.",
                        true,
                        true
                );
                waitingHandler.appendReport(
                        "Please see the error log (Help Menu > Bug Report) for details.",
                        true,
                        true
                );

            } else if (FrameExceptionHandler.getExceptionType(e).equalsIgnoreCase("Protein not found")) {

                waitingHandler.appendReport(
                        "An error occurred while loading the identification files:",
                        true,
                        true
                );
                waitingHandler.appendReport(
                        e.getLocalizedMessage(),
                        true,
                        true
                );
                waitingHandler.appendReport(
                        "Please see https://compomics.github.io/projects/searchgui/wiki/DatabaseHelp.html.",
                        true,
                        true
                );

            } else {

                waitingHandler.appendReport(
                        "An error occurred while loading the identification files:",
                        true,
                        true
                );
                waitingHandler.appendReport(
                        e.getLocalizedMessage(),
                        true,
                        true
                );

            }

            e.printStackTrace();
            System.err.println("Free memory: " + Runtime.getRuntime().freeMemory());

            return 1;
        }

        return 0;
    }

    /**
     * Imports the PSMs from an identification file.
     *
     * @param idFile the identification file
     *
     * @throws java.io.IOException exception thrown if an error occurred when
     * parsing the file
     * @throws java.lang.InterruptedException Exception thrown if a thread is
     * interrupted.
     * @throws java.util.concurrent.TimeoutException Exception thrown if the
     * process timed out.
     */
    public void importPsms(
            File idFile
    ) throws IOException, InterruptedException, TimeoutException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        waitingHandler.appendReport(
                "Parsing " + idFile.getName() + ".",
                true,
                true
        );

        IdfileReader fileReader = null;

        try {

            fileReader = readerFactory.getFileReader(idFile);

        } catch (OutOfMemoryError error) {

            waitingHandler.appendReport(
                    "Ran out of memory when parsing \'" + IoUtil.getFileName(idFile) + "\'.",
                    true,
                    true
            );

            throw new OutOfMemoryError("Ran out of memory when parsing \'" + IoUtil.getFileName(idFile) + "\'.");

        }

        if (fileReader == null) {

            waitingHandler.appendReport(
                    "Identification result file \'" + IoUtil.getFileName(idFile) + "\' not recognized.",
                    true,
                    true
            );

            waitingHandler.setRunCanceled();
            return;

        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        ArrayList<SpectrumMatch> idFileSpectrumMatches = null;

        try {

            idFileSpectrumMatches = fileReader.getAllSpectrumMatches(
                    spectrumProvider,
                    waitingHandler,
                    identificationParameters.getSearchParameters(),
                    identificationParameters.getSequenceMatchingParameters(),
                    true
            );

        } catch (Exception e) {

            waitingHandler.appendReport(
                    "An error occurred while loading spectrum matches from \'"
                    + IoUtil.getFileName(idFile)
                    + "\'. This file will be ignored. Error: " + e.toString()
                    + " See resources/PeptideShaker.log for details.",
                    true,
                    true
            );

            e.printStackTrace();

        }

        // set the search engine name and version for this file
        HashMap<String, ArrayList<String>> software = fileReader.getSoftwareVersions();
        projectDetails.setIdentificationAlgorithmsForFile(IoUtil.getFileName(idFile), software);

        // check for unsupported software
        if (!software.isEmpty()) {

            for (String advocateName : software.keySet()) {

                Advocate advocate = Advocate.getAdvocate(advocateName);

                if (advocate == null || advocate.getType() == Advocate.AdvocateType.unknown) {

                    waitingHandler.appendReport(
                            "Warning: The software used to generate " + idFile.getName() + " was not recognized by PeptideShaker. "
                            + "Please create an issue on the tool website and we will add support for the software used. "
                            + "github.com/compomics/peptide-shaker/issues",
                            true,
                            true
                    );

                    return;

                }
            }
        }

        fileReader.close();

        if (idFileSpectrumMatches != null && !waitingHandler.isRunCanceled()) {

            int nMatches = idFileSpectrumMatches.size();

            if (nMatches == 0) {

                waitingHandler.appendReport(
                        "No PSM found in " + idFile.getName() + ".",
                        true,
                        true
                );

            } else {

                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(nMatches);
                waitingHandler.appendReport(
                        "Checking spectra for " + idFile.getName() + ".",
                        true,
                        true
                );

                HashSet<String> importedFileNames = new HashSet<>(1);

                for (SpectrumMatch spectrumMatch : idFileSpectrumMatches) {

                    HashSet<String> titles = loadedSpectraMap.get(spectrumMatch.getSpectrumFile());

                    if (titles == null) {

                        waitingHandler.appendReport(
                                "Spectrum file named \'" + spectrumMatch.getSpectrumFile()
                                + "\' required to parse \'" + IoUtil.getFileName(idFile) + "\' not found.",
                                true,
                                true
                        );

                        waitingHandler.setRunCanceled();
                        return;

                    }

                    importedFileNames.add(spectrumMatch.getSpectrumFile());
                    String spectrumTitle = spectrumMatch.getSpectrumTitle();

                    if (!titles.contains(spectrumTitle)) {

                        waitingHandler.appendReport(
                                "Spectrum with title \'"
                                + spectrumTitle
                                + "\' in file named \'"
                                + spectrumMatch.getSpectrumFile()
                                + "\' required to parse \'"
                                + IoUtil.getFileName(idFile)
                                + "\' not found.",
                                true,
                                true
                        );

                        waitingHandler.setRunCanceled();
                        return;

                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }

                for (String tempSpectrumFileName : importedFileNames) {

                    projectDetails.addSpectrumFilePath(spectrumProvider.getFilePaths().get(tempSpectrumFileName));
                    identification.addFraction(tempSpectrumFileName);

                }

                // if any de novo tag, map spectrum sequence matches to protein sequences
                if (fileReader.hasDeNovoTags()) {

                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(nMatches);
                    waitingHandler.appendReport(
                            "Mapping tags to peptides.",
                            true,
                            true
                    );

                    tagMapper.mapTags(
                            idFileSpectrumMatches,
                            fastaMapper,
                            waitingHandler
                    );

                }

                waitingHandler.setMaxSecondaryProgressCounter(2 * nMatches);
                waitingHandler.appendReport(
                        "Importing PSMs from " + idFile.getName(),
                        true,
                        true
                );

                PsmImporter psmImporter = new PsmImporter();

                psmImporter.importPsms(
                        idFileSpectrumMatches,
                        identification,
                        identificationParameters,
                        inputMap,
                        fileReader,
                        idFile,
                        sequenceProvider,
                        spectrumProvider,
                        fastaMapper,
                        processingParameters,
                        waitingHandler,
                        exceptionHandler
                );

                if (waitingHandler.isRunCanceled()) {

                    return;

                }

                for (Map.Entry<String, Integer> entry : psmImporter.getProteinCount().entrySet()) {

                    String accession = entry.getKey();
                    int fileCount = entry.getValue();
                    Integer count = proteinCount.get(accession);

                    if (count != null) {

                        proteinCount.put(accession, count + fileCount);

                    } else {

                        proteinCount.put(accession, fileCount);

                    }

                }

                nPSMs += psmImporter.getnPSMs();
                nTotal += psmImporter.getnPeptideAssumptionsTotal();
                nRetained += psmImporter.getnRetained();

                metrics.addFoundCharges(psmImporter.getCharges());

                if (psmImporter.getMaxPeptideErrorDa() > metrics.getMaxPeptidePrecursorErrorDa()) {

                    metrics.setMaxPeptidePrecursorErrorDa(psmImporter.getMaxPeptideErrorDa());

                }

                if (psmImporter.getMaxPeptideErrorPpm() > metrics.getMaxPeptidePrecursorErrorPpm()) {

                    metrics.setMaxPeptidePrecursorErrorPpm(psmImporter.getMaxPeptideErrorPpm());

                }

                if (psmImporter.getMaxTagErrorDa() > metrics.getMaxTagPrecursorErrorDa()) {

                    metrics.setMaxTagPrecursorErrorDa(psmImporter.getMaxTagErrorDa());

                }

                if (psmImporter.getMaxTagErrorPpm() > metrics.getMaxTagPrecursorErrorPpm()) {

                    metrics.setMaxTagPrecursorErrorPpm(psmImporter.getMaxTagErrorPpm());

                }

                projectDetails.addIdentificationFiles(idFile);

                int psmsRejected = psmImporter.getPsmsRejected();
                int noProteins = psmImporter.getMissingProteins();
                int proteinIssue = psmImporter.getProteinIssue();
                int peptideIssue = psmImporter.getPeptideIssue();
                int precursorIssue = psmImporter.getPrecursorIssue();
                int ptmIssue = psmImporter.getModificationIssue();
                int totalAssumptionsRejected = noProteins + proteinIssue + peptideIssue + precursorIssue + ptmIssue;

                double sharePsmsRejected = 100.0 * psmsRejected / nMatches;

                if (psmsRejected > 0) {

                    waitingHandler.appendReport(
                            psmsRejected + " identified spectra (" + Util.roundDouble(sharePsmsRejected, 1) + "%) did not present a valid peptide.",
                            true,
                            true
                    );

                    waitingHandler.appendReport(
                            totalAssumptionsRejected + " of the best scoring peptides were excluded by the import filters:",
                            true,
                            true
                    );

                    String padding = "    ";
                    PeptideAssumptionFilter idFilter = identificationParameters.getPeptideAssumptionFilter();

                    double share = 100 * ((double) noProteins) / totalAssumptionsRejected;

                    if (share >= 1) {

                        waitingHandler.appendReport(
                                padding + "- " + Util.roundDouble(share, 1)
                                + "% peptide not matching to the database.",
                                true,
                                true
                        );

                    }

                    share = 100 * ((double) proteinIssue) / totalAssumptionsRejected;

                    if (share >= 1) {

                        waitingHandler.appendReport(
                                padding + "- " + Util.roundDouble(share, 1)
                                + "% peptide mapping to both target and decoy.",
                                true,
                                true
                        );

                    }

                    share = 100 * ((double) peptideIssue) / totalAssumptionsRejected;

                    if (share >= 1) {

                        if (identificationParameters.getPeptideAssumptionFilter().getMinMissedCleavages() != null
                                || identificationParameters.getPeptideAssumptionFilter().getMaxMissedCleavages() != null) {

                            Integer minMissedCleavages = idFilter.getMinMissedCleavages();
                            Integer maxMissedCleavages = idFilter.getMaxMissedCleavages();

                            if (minMissedCleavages == null) {

                                minMissedCleavages = 0;

                            }

                            if (maxMissedCleavages != null) {

                                waitingHandler.appendReport(
                                        padding + "- " + Util.roundDouble(share, 1)
                                        + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ",",
                                        true,
                                        true
                                );

                                waitingHandler.appendReport(
                                        padding + "    or number of missed cleavage sites outside of the range [" + minMissedCleavages + "-" + maxMissedCleavages + "].",
                                        true,
                                        true
                                );

                            } else {

                                waitingHandler.appendReport(
                                        padding + "- " + Util.roundDouble(share, 1)
                                        + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ",",
                                        true,
                                        true
                                );

                                waitingHandler.appendReport(
                                        padding + "    or number of missed cleavage sites lower than " + minMissedCleavages + ".",
                                        true,
                                        true
                                );
                            }

                        } else {

                            waitingHandler.appendReport(
                                    padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ".",
                                    true,
                                    true
                            );

                        }

                    }

                    share = 100 * ((double) precursorIssue) / totalAssumptionsRejected;

                    if (share >= 1) {

                        waitingHandler.appendReport(
                                padding + "- " + Util.roundDouble(share, 1)
                                + "% peptide presenting high mass or isotopic deviation.",
                                true,
                                true
                        );

                    }

                    share = 100 * ((double) ptmIssue) / totalAssumptionsRejected;

                    if (share >= 1) {

                        waitingHandler.appendReport(
                                padding + "- " + Util.roundDouble(share, 1) + "% unrecognized modifications.",
                                true,
                                true
                        );

                    }

                }

                // inform the user in case search engine results could not be mapped to the database
                boolean allSearchEngines = true;

                for (String advocateName : software.keySet()) {

                    Advocate advocate = Advocate.getAdvocate(advocateName);

                    if (advocate.getType() != Advocate.AdvocateType.search_engine) {

                        allSearchEngines = false;
                        break;

                    }

                }

                if (allSearchEngines && noProteins > 0) {

                    String report = "Some peptides could not be mapped to the database. Please verify the following:" + System.getProperty("line.separator");

                    if (software.keySet().contains(Advocate.mascot.getName())) {

                        report += "- Make sure that Mascot was not used using the 'decoy' option.";

                    }

                    report += "- The protein sequence database must be the same or contain the database used for the search." + System.getProperty("line.separator")
                            + "- When using the 'REVERSED' tag, decoy sequences must be reversed versions of the target sequences, use the 'DECOY' tag otherwise." + System.getProperty("line.separator")
                            + "- When using in house databases make sure that the format is recognized by search engines and PeptideShaker (more details at https://compomics.github.io/projects/searchgui/wiki/DatabaseHelp.html)." + System.getProperty("line.separator")
                            + "The problematic spectra can be inspected in the Spectrum ID tab. In case of doubt please contact the developers.";

                    waitingHandler.appendReport(
                            report,
                            true,
                            true
                    );

                }

                // inform the user in case more than 75% of the hits were rejected by the filters
                if (sharePsmsRejected > 75) {

                    String report = "Warning: More than 75% of the PSMs did not pass the import filters." + System.getProperty("line.separator");
                    double meanRejected = sharePsmsRejected / 4;

                    if (!allSearchEngines && noProteins > meanRejected) {

                        report += " PeptideShaker did not manage to map most peptides to the database. Please verify your database." + System.getProperty("line.separator");

                    }

                    if (proteinIssue > meanRejected) {

                        report += " Apparently your database contains a high degree of shared peptides between the target and decoy sequences. Please verify your database";

                        if (software.keySet().contains(Advocate.mascot.getName())) {

                            report += " and make sure that you use Mascot with the 'decoy' option disabled.";

                        }

                        report += "." + System.getProperty("line.separator");

                    }

                    if (peptideIssue > meanRejected) {

                        report += " Please verify that your peptide selection criteria are not too restrictive." + System.getProperty("line.separator");

                    }

                    if (precursorIssue > meanRejected) {

                        report += " Please verify that your precursor selection criteria are not too restrictive." + System.getProperty("line.separator");

                    }

                    if (ptmIssue > meanRejected) {

                        report += " Apparently your data contains modifications which are not recognized by PeptideShaker. Please verify the search parameters provided when creating the project." + System.getProperty("line.separator");

                        if (software.keySet().contains(Advocate.mascot.getName())) {

                            report += " When using Mascot alone, you need to specify the search parameters manually when creating the project. We recommend the complementary use of SearchGUI when possible." + System.getProperty("line.separator");

                        }
                    }

                    waitingHandler.appendReport(
                            report,
                            true,
                            true
                    );

                }
            }
        }

        waitingHandler.increasePrimaryProgressCounter();

    }

    /**
     * Imports sequences from a FASTA file and sets the sequence provider and
     * protein details provider fields.
     *
     * @param sequenceMatchingParameters the sequence matching parameters
     * @param searchParameters the search parameters
     * @param fastaParameters the FASTA parameters
     * @param peptideVariantsParameters the peptide variants parameters set by
     * the user
     * @param waitingHandler the handler displaying feedback to the user and
     * allowing canceling the import
     * @param exceptionHandler handler for exceptions
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading the FASTA file
     */
    public void importSequences(
            SequenceMatchingParameters sequenceMatchingParameters,
            SearchParameters searchParameters,
            FastaParameters fastaParameters,
            PeptideVariantsParameters peptideVariantsParameters,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) throws IOException {

        String fastaFilePath = projectDetails.getFastaFile();
        File fastaFile = new File(fastaFilePath);

        waitingHandler.appendReport(
                "Importing sequences from " + fastaFile.getName() + ".",
                true,
                true
        );
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        fastaSummary = FastaSummary.getSummary(
                fastaFilePath,
                fastaParameters,
                waitingHandler
        );

        FMIndex fmIndex = new FMIndex(
                fastaFile,
                fastaParameters,
                waitingHandler,
                true,
                peptideVariantsParameters,
                searchParameters
        );

        sequenceProvider = fmIndex;
        fastaMapper = fmIndex;
        proteinDetailsProvider = fmIndex;

    }

    /**
     * Imports the gene information for this project.
     */
    public void importGenes() {

        ProteinGeneDetailsProvider geneFactory = new ProteinGeneDetailsProvider();

        try {

            geneFactory.initialize(PeptideShaker.getJarFilePath());

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "An error occurred while loading the gene mappings.",
                    "Gene Mapping File Error",
                    JOptionPane.ERROR_MESSAGE
            );

        }

        GeneParameters geneParameters = identificationParameters.getGeneParameters();

        geneMaps = geneFactory.getGeneMaps(
                geneParameters,
                fastaSummary,
                sequenceProvider,
                proteinDetailsProvider,
                waitingHandler
        );

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
     * Returns the sequence provider.
     *
     * @return the sequence provider
     */
    public SequenceProvider getSequenceProvider() {
        return sequenceProvider;
    }

    /**
     * Returns the details provider.
     *
     * @return the details provider
     */
    public ProteinDetailsProvider getProteinDetailsProvider() {
        return proteinDetailsProvider;
    }

    /**
     * Returns the fasta mapper.
     *
     * @return the fasta mapper
     */
    public FastaMapper getFastaMapper() {
        return fastaMapper;
    }

    /**
     * Returns the input map.
     *
     * @return the input map
     */
    public InputMap getInputMap() {
        return inputMap;
    }

    /**
     * Returns the occurrence of proteins.
     *
     * @return the occurrence of proteins
     */
    public HashMap<String, Integer> getProteinCount() {
        return proteinCount;
    }

}
