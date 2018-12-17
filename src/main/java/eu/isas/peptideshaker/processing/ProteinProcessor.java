package eu.isas.peptideshaker.processing;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.ptm.ModificationLocalizationScorer;
import java.util.ArrayList;
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
     * Constructor.
     *
     * @param identification the identification
     * @param identificationParameters the identification parameters
     * @param identificationFeaturesGenerator the identification features
     * generator
     */
    public ProteinProcessor(Identification identification, IdentificationParameters identificationParameters, IdentificationFeaturesGenerator identificationFeaturesGenerator) {

        this.identification = identification;
        this.identificationParameters = identificationParameters;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
    }

    /**
     * Scores the PTMs of all protein matches contained in an identification
     * object, estimates spectrum counting and summary statistics.
     *
     * @param modificationLocalizationScorer modification localization scorer
     * @param metrics if provided, metrics on proteins will be saved while
     * iterating the matches
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler an exception handler
     * @param processingParameters the processing parameters
     *
     * @throws java.lang.InterruptedException exception thrown if a thread gets
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if the
     * operation times out
     */
    public void processProteins(ModificationLocalizationScorer modificationLocalizationScorer, Metrics metrics,
            WaitingHandler waitingHandler, ExceptionHandler exceptionHandler, ProcessingParameters processingParameters) throws InterruptedException, TimeoutException {

        waitingHandler.setWaitingText("Scoring Protein Modification Localization. Please Wait...");

        int max = identification.getProteinIdentification().size();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(max);

        // validate the proteins
        ExecutorService pool = Executors.newFixedThreadPool(processingParameters.getnThreads());

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);
        ArrayList<ProteinRunnable> runnables = new ArrayList<>(processingParameters.getnThreads());

        for (int i = 1; i <= processingParameters.getnThreads(); i++) {

            ProteinRunnable runnable = new ProteinRunnable(proteinMatchesIterator, modificationLocalizationScorer, waitingHandler, exceptionHandler);
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

        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

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
         * Constructor.
         *
         * @param proteinMatchesIterator the protein matches iterator
         * @param modificationLocalizationScorer the modification localization
         * scorer
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public ProteinRunnable(ProteinMatchesIterator proteinMatchesIterator, ModificationLocalizationScorer modificationLocalizationScorer,
                WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {

            this.proteinMatchesIterator = proteinMatchesIterator;
            this.modificationLocalizationScorer = modificationLocalizationScorer;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;

        }

        @Override
        public void run() {
            try {

                ProteinMatch proteinMatch;
                while ((proteinMatch = proteinMatchesIterator.next()) != null && !waitingHandler.isRunCanceled()) {

                    long proteinKey = proteinMatch.getKey();
                    modificationLocalizationScorer.scorePTMs(identification, proteinMatch, identificationParameters, false, waitingHandler);

                    PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                    if (psParameter.getMatchValidationLevel().isValidated()) {

                        nValidatedProteins++;

                        if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {

                            nConfidentProteins++;

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

    }
}
