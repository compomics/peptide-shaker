package eu.isas.peptideshaker.fileimport;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.io.identification.idfilereaders.MascotIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.XTandemIdfileReader;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.gui.parameters.identification.IdentificationAlgorithmParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.parameters.identification.tool_specific.AndromedaParameters;
import com.compomics.util.parameters.identification.tool_specific.OmssaParameters;
import com.compomics.util.parameters.identification.tool_specific.XtandemParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.ModificationSiteMapping;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.AndromedaIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.MsAmandaIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.MzIdentMLIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.PepxmlIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.TideIdfileReader;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.experiment.identification.matches.PeptideVariantMatches;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.experiment.identification.utils.ModificationUtils;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.io.identification.idfilereaders.NovorIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.OnyaseIdfileReader;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.threading.SimpleArrayIterator;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import de.proteinms.omxparser.util.OMSSAIdfileReader;
import static eu.isas.peptideshaker.PeptideShaker.TIMEOUT_DAYS;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import static eu.isas.peptideshaker.fileimport.FileImporter.MOD_MASS_TOLERANCE;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class can be used to import PSMs from search engine results.
 *
 * @author Marc Vaudel
 */
public class PsmImporter {

    /**
     * Indicates whether the check for X!Tandem modifications was done.
     */
    private static boolean xTandemModsCheck = false;
    /**
     * Mutex for the X!Tandem modifications check.
     */
    private final static SimpleSemaphore xTandemModsCheckMutex = new SimpleSemaphore(1);
    /**
     * The number of first hits.
     */
    private long nPSMs = 0;
    /**
     * The total number of peptide assumptions.
     */
    private long nPeptideAssumptionsTotal = 0;
    /**
     * The progress of the import.
     */
    private int progress = 0;
    /**
     * The number of PSMs which did not pass the import filters.
     */
    private int psmsRejected = 0;
    /**
     * The number of PSMs which were rejected due to a protein issue.
     */
    private int proteinIssue = 0;
    /**
     * The number of PSMs which were rejected due to a peptide issue.
     */
    private int peptideIssue = 0;
    /**
     * The number of PSMs which were rejected due to a precursor issue.
     */
    private int precursorIssue = 0;
    /**
     * The number of PSMs which were rejected due to a modification parsing
     * issue.
     */
    private int modificationIssue = 0;
    /**
     * The number of retained first hits.
     */
    private int nRetained = 0;
    /**
     * The number of peptides where no protein was found.
     */
    private int missingProteins = 0;
    /**
     * The maximal peptide mass error found in ppm.
     */
    private double maxPeptideErrorPpm = 0;
    /**
     * The maximal peptide mass error found in Da.
     */
    private double maxPeptideErrorDa = 0;
    /**
     * The maximal tag mass error found in ppm.
     */
    private double maxTagErrorPpm = 0;
    /**
     * The maximal tag mass error found in Da.
     */
    private double maxTagErrorDa = 0;
    /**
     * List of charges found.
     */
    private final HashSet<Integer> charges = new HashSet<>();
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private HashMap<String, Integer> proteinCount;

    /**
     * Constructor.
     */
    public PsmImporter() {

    }

    /**
     * Imports PSMs.
     *
     * @param spectrumMatches the PSMs to import
     * @param identification the identification object
     * @param identificationParameters the identification parameters
     * @param inputMap the input map
     * @param fileReader the file reader
     * @param idFile the identification file
     * @param sequenceProvider the sequence provider
     * @param fastaMapper the sequence mapper
     * @param nThreads the number of threads to use
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     * @param exceptionHandler The handler of exceptions.
     *
     * @throws java.lang.InterruptedException Exception thrown if a thread is
     * interrupted.
     * @throws java.util.concurrent.TimeoutException Exception thrown if the
     * process timed out.
     */
    public void importPsms(
            SpectrumMatch[] spectrumMatches,
            Identification identification,
            IdentificationParameters identificationParameters,
            InputMap inputMap,
            IdfileReader fileReader,
            File idFile,
            SequenceProvider sequenceProvider,
            FastaMapper fastaMapper,
            int nThreads,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
            
    ) throws InterruptedException, TimeoutException {

        SimpleArrayIterator<SpectrumMatch> spectrumMatchIterator = new SimpleArrayIterator<>(spectrumMatches);

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);

        ArrayList<PsmImportRunnable> runnables = new ArrayList<>(nThreads);

        for (int i = 0; i < nThreads; i++) {

            runnables.add(
                    new PsmImportRunnable(
                            spectrumMatchIterator,
                            identificationParameters,
                            fileReader,
                            idFile,
                            identification,
                            inputMap,
                            sequenceProvider,
                            fastaMapper,
                            waitingHandler,
                            exceptionHandler
                    )
            );
        }

        runnables.forEach(
                worker -> pool.submit(worker)
        );

        pool.shutdown();

        if (!pool.awaitTermination(TIMEOUT_DAYS, TimeUnit.DAYS)) {

            throw new TimeoutException("Analysis timed out (time out: " + TIMEOUT_DAYS + " days)");

        }

        // Gather metrics from each thread
        for (PsmImportRunnable runnable : runnables) {

            nPSMs += runnable.getnPSMs();
            nPeptideAssumptionsTotal += runnable.getnPeptideAssumptionsTotal();
            psmsRejected += runnable.getPsmsRejected();
            proteinIssue += runnable.getProteinIssue();
            peptideIssue += runnable.getPeptideIssue();
            precursorIssue += runnable.getPrecursorIssue();
            modificationIssue += runnable.getModificationIssue();
            nRetained += runnable.getnRetained();
            missingProteins += runnable.getMissingProteins();
            maxPeptideErrorPpm = Math.max(maxPeptideErrorPpm, runnable.getMaxPeptideErrorPpm());
            maxPeptideErrorDa = Math.max(maxPeptideErrorDa, runnable.getMaxPeptideErrorDa());
            maxTagErrorPpm = Math.max(maxTagErrorPpm, runnable.getMaxPeptideErrorPpm());
            maxTagErrorDa = Math.max(maxTagErrorDa, runnable.getMaxPeptideErrorDa());
            charges.addAll(runnable.getCharges());

            for (Entry<String, Integer> entry : runnable.getProteinCount().entrySet()) {

                String accession = entry.getKey();
                int threadCount = entry.getValue();
                Integer count = proteinCount.get(accession);

                if (count != null) {

                    proteinCount.put(accession, count + threadCount);

                } else {

                    proteinCount.put(accession, threadCount);

                }
            }
        }
    }

    /**
     * Verifies that the modifications targeted by the quick acetyl and quick
     * pyrolidone are included in the identification parameters.
     *
     * @param identificationParameters the identification parameters
     */
    public static void verifyXTandemModifications(
            IdentificationParameters identificationParameters
    ) {

        if (!xTandemModsCheck) {

            xTandemModsCheckMutex.acquire();

            SearchParameters searchParameters = identificationParameters.getSearchParameters();
            ModificationParameters modificationProfile = searchParameters.getModificationParameters();
            IdentificationAlgorithmParameter algorithmParameter = searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex());

            if (algorithmParameter != null) {

                XtandemParameters xtandemParameters = (XtandemParameters) algorithmParameter;

                if (xtandemParameters.isProteinQuickAcetyl() && !modificationProfile.contains("Acetylation of protein N-term")) {

                    Modification modification = ModificationFactory.getInstance().getModification("Acetylation of protein N-term");

                    if (!modificationProfile.getRefinementVariableModifications().contains(modification.getName())) {

                        modificationProfile.addRefinementVariableModification(modification);

                    }
                }

                String[] pyroMods = {"Pyrolidone from E", "Pyrolidone from Q", "Pyrolidone from carbamidomethylated C"};

                if (xtandemParameters.isQuickPyrolidone()) {

                    for (String modName : pyroMods) {

                        if (!modificationProfile.getRefinementVariableModifications().contains(modName)) {

                            Modification modification = ModificationFactory.getInstance().getModification(modName);
                            modificationProfile.addRefinementVariableModification(modification);

                        }
                    }
                }
            }

            xTandemModsCheck = true;

            xTandemModsCheckMutex.release();

        }
    }

    /**
     * Returns the number of PSMs processed.
     *
     * @return the number of PSMs processed
     */
    public long getnPSMs() {

        return nPSMs;

    }

    /**
     * Returns the total number of peptide assumptions parsed.
     *
     * @return the total number of peptide assumptions parsed
     */
    public long getnPeptideAssumptionsTotal() {
        return nPeptideAssumptionsTotal;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters.
     *
     * @return the number of PSMs which did not pass the import filters
     */
    public int getPsmsRejected() {
        return psmsRejected;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * protein issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * protein issue
     */
    public int getProteinIssue() {
        return proteinIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * peptide issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * peptide issue
     */
    public int getPeptideIssue() {
        return peptideIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * precursor issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * precursor issue
     */
    public int getPrecursorIssue() {
        return precursorIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * modification parsing issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * modification parsing issue
     */
    public int getModificationIssue() {
        return modificationIssue;
    }

    /**
     * Returns the number of PSMs where a protein was missing.
     *
     * @return the number of PSMs where a protein was missing
     */
    public int getMissingProteins() {
        return missingProteins;
    }

    /**
     * Returns the number of PSMs retained after filtering.
     *
     * @return the number of PSMs retained after filtering
     */
    public int getnRetained() {
        return nRetained;
    }

    /**
     * Returns the different charges found.
     *
     * @return the different charges found
     */
    public HashSet<Integer> getCharges() {
        return charges;
    }

    /**
     * Returns the maximal peptide mass error found in ppm.
     *
     * @return the maximal peptide mass error found in ppm
     */
    public double getMaxPeptideErrorPpm() {
        return maxPeptideErrorPpm;
    }

    /**
     * Returns the maximal peptide mass error found in Da.
     *
     * @return the maximal peptide mass error found in Da
     */
    public double getMaxPeptideErrorDa() {
        return maxPeptideErrorDa;
    }

    /**
     * Returns the maximal tag mass error found in ppm.
     *
     * @return the maximal tag mass error found in ppm
     */
    public double getMaxTagErrorPpm() {
        return maxTagErrorPpm;
    }

    /**
     * Returns the maximal tag mass error found in Da.
     *
     * @return the maximal tag mass error found in Da
     */
    public double getMaxTagErrorDa() {
        return maxTagErrorDa;
    }

    /**
     * Returns the occurrence of each protein.
     *
     * @return the occurrence of each protein
     */
    public HashMap<String, Integer> getProteinCount() {
        return proteinCount;
    }
}
