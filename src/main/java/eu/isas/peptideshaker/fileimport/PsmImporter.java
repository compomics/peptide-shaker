package eu.isas.peptideshaker.fileimport;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.gui.parameters.identification.IdentificationAlgorithmParameter;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.tool_specific.XtandemParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.threading.SimpleSemaphore;
import com.compomics.util.waiting.WaitingHandler;
import static eu.isas.peptideshaker.PeptideShaker.TIMEOUT_DAYS;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final static SimpleSemaphore XTANDEM_MODS_CHECK_MUTEX = new SimpleSemaphore(1);
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
    private HashMap<String, Integer> proteinCount = new HashMap<>(10000);

    /**
     * Constructor.
     */
    public PsmImporter() {

    }

    /**
     * Imports PSMs.
     *
     * @param spectrumMatches The PSMs to import.
     * @param identification The identification object.
     * @param identificationParameters The identification parameters.
     * @param inputMap The input map.
     * @param fileReader The file reader.
     * @param idFile The identification file.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param fastaMapper The sequence mapper.
     * @param nThreads The number of threads to use.
     * @param waitingHandler The waiting handler to display progress and allow
     * canceling the import.
     * @param exceptionHandler The handler of exceptions.
     *
     * @throws java.lang.InterruptedException Exception thrown if a thread is
     * interrupted.
     * @throws java.util.concurrent.TimeoutException Exception thrown if the
     * process timed out.
     */
    public void importPsms(
            ArrayList<SpectrumMatch> spectrumMatches,
            Identification identification,
            IdentificationParameters identificationParameters,
            InputMap inputMap,
            IdfileReader fileReader,
            File idFile,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            FastaMapper fastaMapper,
            int nThreads,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) 
            throws InterruptedException, TimeoutException {

        ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchQueue = new ConcurrentLinkedQueue<>(spectrumMatches);

        ExecutorService importPool = Executors.newFixedThreadPool(nThreads);

        ArrayList<PsmImportRunnable> importRunnables = new ArrayList<>(nThreads);

        for (int i = 0; i < nThreads; i++) {

            importRunnables.add(new PsmImportRunnable(
                            spectrumMatchQueue,
                            identificationParameters,
                            fileReader,
                            idFile,
                            identification,
                            sequenceProvider,
                            fastaMapper,
                            waitingHandler,
                            exceptionHandler
                    )
            );
        }

        importRunnables.forEach(
                worker -> importPool.submit(worker)
        );

        importPool.shutdown();

        if (!importPool.awaitTermination(TIMEOUT_DAYS, TimeUnit.DAYS)) {

            throw new TimeoutException(
                    "Analysis timed out (time out: " + TIMEOUT_DAYS + " days)"
            );
        }

        // Gather metrics from each thread
        for (PsmImportRunnable runnable : importRunnables) {

            nPSMs += runnable.getnPSMs();
            nPeptideAssumptionsTotal += runnable.getnPeptideAssumptionsTotal();
            peptideIssue += runnable.getPeptideIssue();
            modificationIssue += runnable.getModificationIssue();

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

        spectrumMatchQueue = new ConcurrentLinkedQueue<>(spectrumMatches);

        ExecutorService firstHitPool = Executors.newFixedThreadPool(nThreads);

        ArrayList<PsmFirstHitRunnable> firstHitRunnables = new ArrayList<>(nThreads);

        for (int i = 0; i < nThreads; i++) {

            firstHitRunnables.add(
                    new PsmFirstHitRunnable(
                            spectrumMatchQueue,
                            identificationParameters,
                            sequenceProvider,
                            spectrumProvider,
                            inputMap,
                            proteinCount,
                            waitingHandler,
                            exceptionHandler
                    )
            );
        }

        firstHitRunnables.forEach(
                worker -> firstHitPool.submit(worker)
        );

        firstHitPool.shutdown();

        if (!firstHitPool.awaitTermination(TIMEOUT_DAYS, TimeUnit.DAYS)) {

            throw new TimeoutException(
                    "Analysis timed out (time out: " + TIMEOUT_DAYS + " days)"
            );

        }

        // Gather metrics from each thread
        for (PsmFirstHitRunnable runnable : firstHitRunnables) {

            psmsRejected += runnable.getPsmsRejected();
            proteinIssue += runnable.getProteinIssue();
            peptideIssue += runnable.getPeptideIssue();
            precursorIssue += runnable.getPrecursorIssue();
            nRetained += runnable.getnRetained();
            missingProteins += runnable.getMissingProteins();
            maxPeptideErrorPpm = Math.max(maxPeptideErrorPpm, runnable.getMaxPeptideErrorPpm());
            maxPeptideErrorDa = Math.max(maxPeptideErrorDa, runnable.getMaxPeptideErrorDa());
            maxTagErrorPpm = Math.max(maxTagErrorPpm, runnable.getMaxPeptideErrorPpm());
            maxTagErrorDa = Math.max(maxTagErrorDa, runnable.getMaxPeptideErrorDa());
            charges.addAll(runnable.getCharges());

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

            XTANDEM_MODS_CHECK_MUTEX.acquire();

            if (!xTandemModsCheck) {

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

            }

            XTANDEM_MODS_CHECK_MUTEX.release();

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
