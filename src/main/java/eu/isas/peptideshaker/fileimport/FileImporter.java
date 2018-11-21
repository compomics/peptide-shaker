package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import eu.isas.peptideshaker.gui.MgfFilesNotFoundDialog;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.IdfileReaderFactory;
import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
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
import eu.isas.peptideshaker.PeptideShaker;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
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
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * A provider for protein sequences.
     */
    private SequenceProvider sequenceProvider;
    /**
     * A provider for protein details.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
    /**
     * Summary information on the fasta file.
     */
    private FastaSummary fastaSummary;
    /**
     * Metrics of the dataset picked-up while loading the data.
     */
    private final Metrics metrics;
    /**
     * The mass tolerance to be used to match modifications from search engines
     * and expected modifications. 0.01 by default, the mass resolution in
     * X!Tandem result files.
     */
    public static final double MOD_MASS_TOLERANCE = 0.01;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The identification file reader factory of compomics utilities.
     */
    private final IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
    /**
     * A list of spectrum files (can be empty, no spectrum will be imported).
     */
    private final HashMap<String, File> spectrumFiles = new HashMap<>();
    /**
     * The processing preferences.
     */
    private final ProcessingParameters processingParameters;
    /**
     * The project details
     */
    private final ProjectDetails projectDetails;
    /**
     * The number of retained first hits.
     */
    private long nRetained = 0;
    /**
     * The number of spectra.
     */
    private long nSpectra = 0;
    /**
     * List of the mgf files used.
     */
    private final ArrayList<String> mgfUsed = new ArrayList<>();
    /**
     * Map of the missing mgf files indexed by identification file.
     */
    private final HashMap<File, String> missingMgfFiles = new HashMap<>();
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
    private Identification identification;
    /**
     * A fasta file mapper.
     */
    private FastaMapper fastaMapper;
    /**
     * A tag to protein mapper.
     */
    private final TagMapper tagMapper;
    /**
     * List of one hit wonders.
     */
    private final HashSet<String> singleProteinList = new HashSet<>();
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private final HashMap<String, Integer> proteinCount = new HashMap<>();
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
     * @param identification the identification where to store the matches
     * @param waitingHandler The handler displaying feedback to the user
     * @param processingParameters the processing parameters
     * @param identificationParameters the identification parameters
     * @param projectDetails the project details
     * @param metrics metrics of the dataset to be saved for the GUI
     * @param exceptionHandler the exception handler
     */
    public FileImporter(Identification identification, IdentificationParameters identificationParameters, ProcessingParameters processingParameters,
            Metrics metrics, ProjectDetails projectDetails, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {

        this.identificationParameters = identificationParameters;
        this.metrics = metrics;
        this.processingParameters = processingParameters;
        this.projectDetails = projectDetails;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;

        tagMapper = new TagMapper(identificationParameters, exceptionHandler);

    }

    /**
     * Imports the identifications from the files.
     *
     * @param idFiles the identification files
     * @param spectrumFiles the spectrum files
     *
     * @return 0 if success, 1 if not
     */
    public int importFiles(ArrayList<File> idFiles, ArrayList<File> spectrumFiles) {

        ArrayList<File> sortedIdFiles = idFiles.stream()
                .collect(Collectors.groupingBy(File::getName, TreeMap::new, Collectors.toList()))
                .values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        for (File spectrumFile : spectrumFiles) {

            this.spectrumFiles.put(spectrumFile.getName(), spectrumFile);
            importSpectra(spectrumFile.getName());

        }

        try {

            importSequences(identificationParameters.getSequenceMatchingParameters(),
                    identificationParameters.getSearchParameters(), identificationParameters.getPeptideVariantsParameters(),
                    waitingHandler, exceptionHandler);

            if (waitingHandler.isRunCanceled()) {

                return 1;

            }

            GeneParameters genePreferences = identificationParameters.getGeneParameters();

            if (genePreferences.getUseGeneMapping()) {

                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                waitingHandler.appendReport("Importing gene mappings.", true, true);
                importGenes();

            } else {

                geneMaps = new GeneMaps();

            }

            if (waitingHandler.isRunCanceled()) {

                return 1;

            }

            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            waitingHandler.appendReport("Establishing local database connection.", true, true);

            waitingHandler.increasePrimaryProgressCounter();

            if (!waitingHandler.isRunCanceled()) {

                waitingHandler.appendReport("Reading identification files.", true, true);

                for (File idFile : sortedIdFiles) {

                    importPsms(idFile);

                    if (waitingHandler.isRunCanceled()) {

                        try {

                            identification.close();

                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                        return 1;
                    }
                }

                while (!missingMgfFiles.isEmpty()) {

                    if (hasGUI) {

                        new MgfFilesNotFoundDialog((WaitingDialog) waitingHandler, missingMgfFiles);

                    } else {

                        String missingFiles = missingMgfFiles.keySet().stream()
                                .map(File::getName)
                                .sorted()
                                .collect(Collectors.joining(", "));

                        waitingHandler.appendReport("MGF files missing: " + missingFiles, true, true);
                        identification.close();
                        return 1;

                    }

                    waitingHandler.appendReport("Processing files with the new input.", true, true);
                    ArrayList<File> filesToProcess = new ArrayList<>(missingMgfFiles.keySet());

                    for (String mgfName : missingMgfFiles.values()) {

                        File newFile = spectrumFactory.getSpectrumFileFromIdName(mgfName);
                        this.spectrumFiles.put(newFile.getName(), newFile);
                        projectDetails.addSpectrumFile(newFile);

                    }

                    missingMgfFiles.clear();

                    for (File idFile : filesToProcess) {

                        importPsms(idFile);

                    }

                    if (waitingHandler.isRunCanceled()) {

                        identification.close();
                        return 1;

                    }
                }

                // clear the objects not needed anymore
                singleProteinList.clear();

                if (nRetained == 0) {

                    waitingHandler.appendReport("No identifications retained.", true, true);
                    waitingHandler.setRunCanceled();
                    identification.close();

                    return 1;

                }

                waitingHandler.appendReport("File import completed. "
                        + nPSMs + " first hits imported (" + nTotal + " total) from " + nSpectra + " spectra.", true, true);
                waitingHandler.appendReport("[" + nRetained + " first hits passed the initial filtering]", true, true);
                waitingHandler.increaseSecondaryProgressCounter(spectrumFiles.size() - mgfUsed.size());

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

                JOptionPane.showMessageDialog((WaitingDialog) waitingHandler, JOptionEditorPane.getJOptionEditorPane(
                        "PeptideShaker used up all the available memory and had to be stopped.<br>"
                        + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                        + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                        + "Java Options). See also <a href=\"https://compomics.github.io/projects/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>."),
                        "Out Of Memory", JOptionPane.ERROR_MESSAGE);

            }

            error.printStackTrace();

            if (identification != null) {

                try {

                    identification.close();

                } catch (Exception e) {

                    e.printStackTrace();

                }
            }

            return 1;

        } catch (Exception e) {

            waitingHandler.setRunCanceled();

            System.out.println("<CompomicsError>PeptideShaker processing failed. See the PeptideShaker log for details.</CompomicsError>");

            if (e instanceof NullPointerException) {

                waitingHandler.appendReport("An error occurred while loading the identification files.", true, true);
                waitingHandler.appendReport("Please see the error log (Help Menu > Bug Report) for details.", true, true);

            } else if (FrameExceptionHandler.getExceptionType(e).equalsIgnoreCase("Protein not found")) {

                waitingHandler.appendReport("An error occurred while loading the identification files:", true, true);
                waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
                waitingHandler.appendReport("Please see https://compomics.github.io/projects/searchgui/wiki/databasehelp.html.", true, true);

            } else {

                waitingHandler.appendReport("An error occurred while loading the identification files:", true, true);
                waitingHandler.appendReport(e.getLocalizedMessage(), true, true);

            }

            e.printStackTrace();
            System.err.println("Free memory: " + Runtime.getRuntime().freeMemory());

            if (identification != null) {

                try {

                    identification.close();

                } catch (Exception ex) {

                    ex.printStackTrace();

                }
            }

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
     */
    public void importPsms(File idFile) throws IOException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        waitingHandler.appendReport("Parsing " + idFile.getName() + ".", true, true);

        IdfileReader fileReader = null;
        try {

            fileReader = readerFactory.getFileReader(idFile);

        } catch (OutOfMemoryError error) {

            waitingHandler.appendReport("Ran out of memory when parsing \'" + Util.getFileName(idFile) + "\'.", true, true);
            throw new OutOfMemoryError("Ran out of memory when parsing \'" + Util.getFileName(idFile) + "\'.");

        }

        if (fileReader == null) {

            waitingHandler.appendReport("Identification result file \'" + Util.getFileName(idFile) + "\' not recognized.", true, true);
            waitingHandler.setRunCanceled();
            return;

        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        LinkedList<SpectrumMatch> idFileSpectrumMatches = null;

        try {

            idFileSpectrumMatches = fileReader.getAllSpectrumMatches(waitingHandler, identificationParameters.getSearchParameters(), identificationParameters.getSequenceMatchingParameters(), true);

        } catch (Exception e) {

            waitingHandler.appendReport("An error occurred while loading spectrum matches from \'"
                    + Util.getFileName(idFile)
                    + "\'. This file will be ignored. Error: " + e.toString()
                    + " See resources/PeptideShaker.log for details.", true, true);
            e.printStackTrace();

        }

        // set the search engine name and version for this file
        HashMap<String, ArrayList<String>> software = fileReader.getSoftwareVersions();
        projectDetails.setIdentificationAlgorithmsForFile(Util.getFileName(idFile), software);

        // check for unsupported software
        if (!software.isEmpty()) {

            for (String advocateName : software.keySet()) {

                Advocate advocate = Advocate.getAdvocate(advocateName);

                if (advocate == null || advocate.getType() == Advocate.AdvocateType.unknown) {

                    waitingHandler.appendReport("Warning: The software used to generate " + idFile.getName() + " was not recognized by PeptideShaker. "
                            + "Please create an issue on the tool website and we will add support for the software used. "
                            + "github.com/compomics/peptide-shaker/issues", true, true);
                    return;

                }
            }
        }

        fileReader.close();

        if (idFileSpectrumMatches != null && !waitingHandler.isRunCanceled()) {

            if (idFileSpectrumMatches.isEmpty()) {

                waitingHandler.appendReport("No PSM found in " + idFile.getName() + ".", true, true);

            } else {

                boolean allLoaded = true;
                int numberOfMatches = idFileSpectrumMatches.size();
                waitingHandler.resetSecondaryProgressCounter();
                waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                waitingHandler.appendReport("Loading spectra for " + idFile.getName() + ".", true, true);

                for (SpectrumMatch spectrumMatch : idFileSpectrumMatches) {

                    // Verify that the spectrum is in the provided mgf files
                    if (!importSpectrum(idFile, spectrumMatch, numberOfMatches)) {

                        allLoaded = false;

                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }

                if (allLoaded) {

                    // if any map spectrum sequencing matches on protein sequences
                    if (fileReader.hasDeNovoTags()) {

                        waitingHandler.resetSecondaryProgressCounter();
                        waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                        waitingHandler.appendReport("Mapping tags to peptides.", true, true);
                        tagMapper.mapTags(idFileSpectrumMatches, fastaMapper, waitingHandler);

                    }

                    waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);
                    waitingHandler.appendReport("Importing PSMs from " + idFile.getName(), true, true);

                    PsmImporter psmImporter = new PsmImporter(identificationParameters, fileReader, idFile, identification, inputMap, proteinCount, singleProteinList, sequenceProvider, fastaMapper);
                    psmImporter.importPsms(idFileSpectrumMatches, processingParameters.getnThreads(), waitingHandler);

                    if (waitingHandler.isRunCanceled()) {

                        return;

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

                    double sharePsmsRejected = 100.0 * psmsRejected / numberOfMatches;

                    if (psmsRejected > 0) {

                        waitingHandler.appendReport(psmsRejected + " identified spectra (" + Util.roundDouble(sharePsmsRejected, 1) + "%) did not present a valid peptide.", true, true);
                        waitingHandler.appendReport(totalAssumptionsRejected + " of the best scoring peptides were excluded by the import filters:", true, true);

                        String padding = "    ";
                        PeptideAssumptionFilter idFilter = identificationParameters.getPeptideAssumptionFilter();

                        double share = 100 * ((double) noProteins) / totalAssumptionsRejected;

                        if (share >= 1) {

                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide not matching to the database.", true, true);

                        }

                        share = 100 * ((double) proteinIssue) / totalAssumptionsRejected;

                        if (share >= 1) {

                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide mapping to both target and decoy.", true, true);

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

                                    waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                            + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ",", true, true);
                                    waitingHandler.appendReport(padding + "    or number of missed cleavage sites outside of the range [" + minMissedCleavages + "-" + maxMissedCleavages + "].", true, true);

                                } else {

                                    waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                            + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ",", true, true);
                                    waitingHandler.appendReport(padding + "    or number of missed cleavage sites lower than " + minMissedCleavages + ".", true, true);

                                }

                            } else {

                                waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                        + "% peptide length less than " + idFilter.getMinPepLength() + " or greater than " + idFilter.getMaxPepLength() + ".", true, true);

                            }
                        }

                        share = 100 * ((double) precursorIssue) / totalAssumptionsRejected;

                        if (share >= 1) {

                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1)
                                    + "% peptide presenting high mass or isotopic deviation.", true, true);

                        }

                        share = 100 * ((double) ptmIssue) / totalAssumptionsRejected;

                        if (share >= 1) {

                            waitingHandler.appendReport(padding + "- " + Util.roundDouble(share, 1) + "% unrecognized modifications.", true, true);

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
                                + "- When using in house databases make sure that the format is recognized by search engines and PeptideShaker (more details at https://compomics.github.io/projects/searchgui/wiki/databasehelp.html)." + System.getProperty("line.separator")
                                + "The problematic spectra can be inspected in the Spectrum ID tab. In case of doubt please contact the developers.";
                        waitingHandler.appendReport(report, true, true);

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

                        waitingHandler.appendReport(report, true, true);

                    }
                }
            }
        }

        waitingHandler.increasePrimaryProgressCounter();
    }

    /**
     * Checks whether the spectrum file needed for the given spectrum match is
     * loaded and if the spectrum is present. Try to load it from the factory
     * otherwise.
     *
     * @param idFile the identification file
     * @param spectrumMatch the spectrum match
     * @param numberOfMatches the number of matches expected for this
     * identification file
     *
     * @return indicates whether the spectrum is imported, false if the file was
     * not found
     */
    private boolean importSpectrum(File idFile, SpectrumMatch spectrumMatch, int numberOfMatches) {

        String spectrumKey = spectrumMatch.getSpectrumKey();
        String fileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        // remap wrong spectrum file names
        if (spectrumFactory.getSpectrumFileFromIdName(fileName) != null) {

            fileName = spectrumFactory.getSpectrumFileFromIdName(fileName).getName();
            spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
            spectrumMatch.setSpectrumKey(spectrumKey);

        }

        // import the mgf file if not done already
        if (!mgfUsed.contains(fileName)) {

            File spectrumFile = spectrumFiles.get(fileName);

            if (spectrumFile != null && spectrumFile.exists()) {

                importSpectra(fileName);
                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxSecondaryProgressCounter(numberOfMatches);

            } else {

                if (!missingMgfFiles.containsKey(idFile)) {

                    missingMgfFiles.put(idFile, fileName);
                    waitingHandler.appendReport(fileName + " not found.", true, true);

                }

                return false;

            }
        }

        // remap missing spectrum titles
        if (spectrumFactory.fileLoaded(fileName) && !spectrumFactory.spectrumLoaded(spectrumKey)) {

            String oldTitle = Spectrum.getSpectrumTitle(spectrumKey);
            Integer spectrumNumber = spectrumMatch.getSpectrumNumber();

            if (spectrumNumber == null) {

                try {

                    spectrumNumber = new Integer(oldTitle);

                } catch (Exception e) {
                    // ignore
                }
            }

            if (spectrumNumber == null) {

                String errorMessage = "Spectrum \'" + oldTitle + "\' not found in file " + fileName + ".";
                waitingHandler.appendReport(errorMessage, true, true);
                waitingHandler.setRunCanceled();
                throw new IllegalArgumentException(errorMessage);

            }

            spectrumTitle = spectrumFactory.getSpectrumTitle(fileName, spectrumNumber);
            spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
            spectrumMatch.setSpectrumKey(spectrumKey);

            if (!spectrumFactory.spectrumLoaded(spectrumKey)) {

                spectrumTitle = spectrumNumber + "";
                spectrumKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);
                spectrumMatch.setSpectrumKey(spectrumKey);

                if (spectrumFactory.fileLoaded(fileName) && !spectrumFactory.spectrumLoaded(spectrumKey)) {

                    String errorMessage = "Spectrum \'" + oldTitle + "\' number " + spectrumTitle + " not found in file " + fileName + ".";
                    waitingHandler.appendReport(errorMessage, true, true);
                    waitingHandler.setRunCanceled();
                    throw new IllegalArgumentException(errorMessage);

                }
            }
        }

        return true;

    }

    /**
     * Verify that the spectra are imported and imports spectra from the desired
     * spectrum file if necessary.
     *
     * @param targetFileName the spectrum file
     */
    public void importSpectra(String targetFileName) {

        File spectrumFile = spectrumFiles.get(targetFileName);

        try {

            waitingHandler.appendReport("Importing " + targetFileName, true, true);
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.resetSecondaryProgressCounter();
            spectrumFactory.addSpectra(spectrumFile, waitingHandler);
            mgfUsed.add(spectrumFile.getName());
            nSpectra += spectrumFactory.getNSpectra(spectrumFile.getName());
            projectDetails.addSpectrumFile(spectrumFile);
            identification.addFraction(spectrumFile.getName());

            // @TODO: check for duplicate spectrum titles and show the warning in the lower right corner of the main frame
            if (waitingHandler.isRunCanceled()) {

                return;

            }

            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.increasePrimaryProgressCounter();
            waitingHandler.appendReport(targetFileName + " imported.", true, true);

        } catch (Exception e) {

            waitingHandler.appendReport("Spectrum files import failed when trying to import " + targetFileName + ".", true, true);
            e.printStackTrace();

        }
    }

    /**
     * Imports sequences from a FASTA file and sets the sequence provider and
     * protein details provider fields.
     *
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param searchParameters the search parameters
     * @param peptideVariantsPreferences the peptide variants preferences set by
     * the user
     * @param waitingHandler the handler displaying feedback to the user and
     * allowing canceling the import
     * @param exceptionHandler handler for exceptions
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading the fasta file
     */
    public void importSequences(SequenceMatchingParameters sequenceMatchingPreferences, SearchParameters searchParameters, PeptideVariantsParameters peptideVariantsPreferences, WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler) throws IOException {

        File fastaFile = searchParameters.getFastaFile();
        FastaParameters fastaParameters = searchParameters.getFastaParameters();

        waitingHandler.appendReport("Importing sequences from " + fastaFile.getName() + ".", true, true);
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        fastaSummary = FastaSummary.getSummary(fastaFile, fastaParameters, waitingHandler);

        FMIndex fmIndex = new FMIndex(fastaFile, fastaParameters, waitingHandler, true, searchParameters.getModificationParameters(), peptideVariantsPreferences);

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
            JOptionPane.showMessageDialog(null, "An error occurred while loading the gene mappings.", "Gene Mapping File Error", JOptionPane.ERROR_MESSAGE);
        }

        GeneParameters genePreferences = identificationParameters.getGeneParameters();
        geneMaps = geneFactory.getGeneMaps(genePreferences, fastaSummary, sequenceProvider, proteinDetailsProvider, waitingHandler);

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

    public InputMap getInputMap() {
        return inputMap;
    }

    public HashMap<String, Integer> getProteinCount() {
        return proteinCount;
    }

}
