package eu.isas.peptideshaker.processing;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.modifications.ModificationProvider;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.ptm.ModificationLocalizationScorer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Scores modification localization on proteins, estimates spectrum counting and
 * summary statistics values.
 *
 * @author Marc Vaudel
 */
public class ProteinProcessor {

    /**
     * the identification object.
     */
    private final Identification identification;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The identification features generator.
     */
    private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;

    /**
     * Constructor.
     *
     * @param identification the identification
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param sequenceProvider the sequence provider
     */
    public ProteinProcessor(
            Identification identification,
            IdentificationParameters identificationParameters,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider
    ) {

        this.identification = identification;
        this.identificationParameters = identificationParameters;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.sequenceProvider = sequenceProvider;

    }

    /**
     * Scores the PTMs of all protein matches contained in an identification
     * object, estimates spectrum counting and summary statistics.
     *
     * @param modificationLocalizationScorer The modification localization
     * scorer to use.
     * @param metrics If provided, metrics on proteins will be saved while
     * iterating the matches.
     * @param modificationProvider The modification provider to use.
     * @param waitingHandler The handler displaying feedback to the user.
     * @param exceptionHandler The exception handler to use.
     * @param processingParameters The processing parameters.
     *
     * @throws java.lang.InterruptedException exception thrown if a thread gets
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if the
     * operation times out
     */
    public void processProteins(
            ModificationLocalizationScorer modificationLocalizationScorer,
            Metrics metrics,
            ModificationProvider modificationProvider,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler,
            ProcessingParameters processingParameters
    ) throws InterruptedException, TimeoutException {

        waitingHandler.setWaitingText("Scoring Protein Modification Localization. Please Wait...");

        int max = identification.getProteinIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        // validate the proteins
        ExecutorService pool = Executors.newFixedThreadPool(processingParameters.getnThreads());

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        ArrayList<ProteinRunnable> runnables = new ArrayList<>(processingParameters.getnThreads());

        for (int i = 1; i <= processingParameters.getnThreads(); i++) {

            ProteinRunnable runnable = new ProteinRunnable(
                    proteinMatchesIterator,
                    modificationLocalizationScorer,
                    modificationProvider,
                    waitingHandler,
                    exceptionHandler
            );
            pool.submit(runnable);
            runnables.add(runnable);

        }

        if (waitingHandler.isRunCanceled()) {

            pool.shutdownNow();
        }

        pool.shutdown();

        if (!pool.awaitTermination(identification.getProteinIdentification().size(), TimeUnit.MINUTES)) {
            throw new InterruptedException("Protein matches validation timed out. Please contact the developers.");
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        if (metrics != null) {

            metrics.setMaxSpectrumCounting(
                    runnables.stream()
                            .mapToDouble(ProteinRunnable::getMaxSpectrumCounting)
                            .sum()
            );
            metrics.setnValidatedProteins(
                    runnables.stream()
                            .mapToInt(ProteinRunnable::getnValidatedProteins)
                            .sum()
            );
            metrics.setnConfidentProteins(
                    runnables.stream()
                            .mapToInt(ProteinRunnable::getnConfidentProteins)
                            .sum()
            );
            metrics.setMaxNPeptides(
                    runnables.stream()
                            .mapToInt(ProteinRunnable::getMaxPeptides)
                            .max()
                            .orElse(0)
            );
            metrics.setMaxNPsms(
                    runnables.stream()
                            .mapToInt(ProteinRunnable::getMaxPsms)
                            .max()
                            .orElse(0)
            );
            metrics.setMaxMW(
                    runnables.stream()
                            .mapToDouble(ProteinRunnable::getMaxMW)
                            .max()
                            .orElse(0.0)
            );
            metrics.setMaxProteinAccessionLength(
                    runnables.stream()
                            .mapToInt(ProteinRunnable::getMaxProteinAccessionLength)
                            .max()
                            .orElse(0)
            );

            TreeMap<Double, TreeMap<Integer, TreeMap<Integer, TreeSet<Long>>>> orderMap1 = new TreeMap<>();

            for (int i = 0; i < runnables.size(); i++) {

                HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<Long>>>> threadMap1 = runnables.get(i).getOrderMap();

                for (Entry<Double, HashMap<Integer, HashMap<Integer, HashSet<Long>>>> entry1 : threadMap1.entrySet()) {

                    double key1 = entry1.getKey();
                    HashMap<Integer, HashMap<Integer, HashSet<Long>>> threadMap2 = entry1.getValue();
                    TreeMap<Integer, TreeMap<Integer, TreeSet<Long>>> orderMap2 = orderMap1.get(key1);

                    if (orderMap2 == null) {

                        orderMap2 = new TreeMap<>();
                        orderMap1.put(key1, orderMap2);

                    }

                    for (Entry<Integer, HashMap<Integer, HashSet<Long>>> entry2 : threadMap2.entrySet()) {

                        int key2 = entry2.getKey();
                        HashMap<Integer, HashSet<Long>> threadMap3 = entry2.getValue();
                        TreeMap<Integer, TreeSet<Long>> orderMap3 = orderMap2.get(key2);

                        if (orderMap3 == null) {

                            orderMap3 = new TreeMap<>();
                            orderMap2.put(key2, orderMap3);

                        }

                        for (Entry<Integer, HashSet<Long>> entry3 : threadMap3.entrySet()) {

                            int key3 = entry3.getKey();
                            HashSet<Long> threadSet = entry3.getValue();
                            TreeSet<Long> orderedSet = orderMap3.get(key3);

                            if (orderedSet == null) {

                                orderedSet = new TreeSet<>();
                                orderMap3.put(key3, orderedSet);

                            }

                            orderedSet.addAll(threadSet);

                        }
                    }
                }
            }

            long[] proteinKeys = orderMap1.values().stream()
                    .flatMap(map -> map.values().stream())
                    .flatMap(map -> map.values().stream())
                    .flatMap(set -> set.stream())
                    .mapToLong(a -> a)
                    .toArray();

            metrics.setProteinKeys(proteinKeys);

        }
    }

    /**
     * Runnable validating protein matches.
     *
     * @author Marc Vaudel
     */
    private class ProteinRunnable implements Runnable {

        /**
         * An iterator for the protein matches.
         */
        private final ProteinMatchesIterator proteinMatchesIterator;
        /**
         * The PTM scorer responsible for scoring PTM localization.
         */
        private final ModificationLocalizationScorer modificationLocalizationScorer;
        /**
         * The modification provider to use.
         */
        private final ModificationProvider modificationProvider;
        /**
         * The waiting handler.
         */
        private final WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private final ExceptionHandler exceptionHandler;
        /**
         * The number of validated proteins.
         */
        private int nValidatedProteins = 0;
        /**
         * The number of confident proteins.
         */
        private int nConfidentProteins = 0;
        /**
         * The maximum spectrum counting value among proteins.
         */
        private double maxSpectrumCounting = 0.0;
        /**
         * A map of the ordered proteins.
         */
        private HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<Long>>>> orderMap = new HashMap<>();
        /**
         * The max mw among the proteins.
         */
        private double maxMW = 0;
        /**
         * The max protein key length among the proteins.
         */
        private int maxProteinAccessionLength = 6;
        /**
         * The max number of peptides among the proteins.
         */
        private int maxPeptides = 0;
        /**
         * The max number of PSMs among the proteins.
         */
        private int maxPsms = 0;

        /**
         * Constructor.
         *
         * @param proteinMatchesIterator The protein matches iterator to use.
         * @param modificationLocalizationScorer The modification localization
         * scorer to use.
         * @param modificationProvider The modification provider to use.
         * @param waitingHandler The waiting handler to use.
         * @param exceptionHandler The exception handler to use.
         */
        public ProteinRunnable(
                ProteinMatchesIterator proteinMatchesIterator,
                ModificationLocalizationScorer modificationLocalizationScorer,
                ModificationProvider modificationProvider,
                WaitingHandler waitingHandler,
                ExceptionHandler exceptionHandler
        ) {

            this.proteinMatchesIterator = proteinMatchesIterator;
            this.modificationLocalizationScorer = modificationLocalizationScorer;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
            this.modificationProvider = modificationProvider;

        }

        @Override
        public void run() {
            try {

                ProteinMatch proteinMatch;
                while ((proteinMatch = proteinMatchesIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    long proteinKey = proteinMatch.getKey();
                    modificationLocalizationScorer.scorePTMs(
                            identification,
                            proteinMatch,
                            identificationParameters,
                            false,
                            modificationProvider, 
                            waitingHandler
                    );

                    PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                    if (!proteinMatch.isDecoy()) {

                        double score = psParameter.getScore();

                        HashMap<Integer, HashMap<Integer, HashSet<Long>>> scoreMap = orderMap.get(score);

                        if (scoreMap == null) {

                            scoreMap = new HashMap<>(1);
                            orderMap.put(score, scoreMap);

                        }

                        int nPeptides = proteinMatch.getPeptideMatchesKeys().length;

                        HashMap<Integer, HashSet<Long>> nPeptidesMap = scoreMap.get(-nPeptides);

                        if (nPeptidesMap == null) {

                            nPeptidesMap = new HashMap<>(1);
                            scoreMap.put(-nPeptides, nPeptidesMap);

                            if (nPeptides > maxPeptides) {

                                maxPeptides = nPeptides;

                            }
                        }

                        int nPsms = Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                                .mapToObj(peptideKey -> identification.getPeptideMatch(peptideKey))
                                .mapToInt(peptideMatch -> peptideMatch.getSpectrumCount())
                                .sum();

                        HashSet<Long> nSpectraList = nPeptidesMap.get(-nPsms);

                        if (nSpectraList == null) {

                            nSpectraList = new HashSet<>(1);
                            nPeptidesMap.put(-nPsms, nSpectraList);

                            if (nPsms > maxPsms) {

                                maxPsms = nPsms;

                            }
                        }

                        nSpectraList.add(proteinMatch.getKey());

                        // Get leading protein accession
                        String mainAccession = proteinMatch.getLeadingAccession();

                        // Save maximal mw
                        String proteinSequence = sequenceProvider.getSequence(mainAccession);
                        double mw = ProteinUtils.computeMolecularWeight(proteinSequence);

                        if (mw > maxMW) {

                            maxMW = mw;

                        }

                        // save the length of the longest protein accession number
                        if (mainAccession.length() > maxProteinAccessionLength) {

                            maxProteinAccessionLength = proteinMatch.getLeadingAccession().length();

                        }

                        if (psParameter.getMatchValidationLevel().isValidated()) {

                            nValidatedProteins++;

                            if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {

                                nConfidentProteins++;

                            }
                        }
                    }

                    if (identificationFeaturesGenerator != null) {

                        double tempSpectrumCounting = identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey);

                        if (tempSpectrumCounting > maxSpectrumCounting) {

                            maxSpectrumCounting = tempSpectrumCounting;

                        }
                    }

                    waitingHandler.increaseSecondaryProgressCounter();

                }
            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

        /**
         * Returns the number of validated proteins.
         *
         * @return the number of validated proteins
         */
        public int getnValidatedProteins() {
            return nValidatedProteins;
        }

        /**
         * Returns the number of confident proteins.
         *
         * @return the number of confident proteins
         */
        public int getnConfidentProteins() {
            return nConfidentProteins;
        }

        /**
         * Returns the maximal spectrum counting value.
         *
         * @return the maximal spectrum counting value
         */
        public double getMaxSpectrumCounting() {
            return maxSpectrumCounting;
        }

        /**
         * Returns the order map of proteins.
         *
         * @return the order map of proteins
         */
        public HashMap<Double, HashMap<Integer, HashMap<Integer, HashSet<Long>>>> getOrderMap() {
            return orderMap;
        }

        /**
         * Returns the max mw among proteins.
         *
         * @return the max mw among proteins
         */
        public double getMaxMW() {
            return maxMW;
        }

        /**
         * Returns the max length of protein keys among proteins.
         *
         * @return the max length of protein keys among proteins
         */
        public int getMaxProteinAccessionLength() {
            return maxProteinAccessionLength;
        }

        /**
         * Returns the max number of peptides among proteins.
         *
         * @return the max number of peptides among proteins
         */
        public int getMaxPeptides() {
            return maxPeptides;
        }

        /**
         * Returns the max number of PSMs among proteins.
         *
         * @return the max number of PSMs among proteins
         */
        public int getMaxPsms() {
            return maxPsms;
        }
    }
}
