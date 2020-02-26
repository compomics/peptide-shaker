package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.filtering.MatchFilter;
import com.compomics.util.experiment.identification.filtering.PeptideFilter;
import com.compomics.util.experiment.identification.filtering.ProteinFilter;
import com.compomics.util.experiment.identification.filtering.PsmFilter;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.gui.filtering.FilterParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class provides information whether a hit should be hidden or starred.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class StarHider {

    /**
     * The identification.
     */
    private final Identification identification;
    /**
     * The filter preferences
     */
    private final FilterParameters filterPreferences;
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The spectrum provider.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * The protein details provider.
     */
    private final ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The gene maps.
     */
    private final GeneMaps geneMaps;
    /**
     * The identification features generator.
     */
    private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The metrics.
     */
    private final Metrics metrics;
    /**
     * The progress dialog.
     */
    private final ProgressDialogX progressDialog;
    /**
     * The number of threads.
     */
    private final int nThreads;
    /**
     * The exception handler.
     */
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructor.
     *
     * @param identification The identification.
     * @param filterParameters The filter parameters.
     * @param sequenceProvider The sequence provider.
     * @param proteinDetailsProvider The protein details provider.
     * @param spectrumProvider The spectrum provider.
     * @param geneMaps The gene maps.
     * @param identificationFeaturesGenerator The identification features
     * generator.
     * @param identificationParameters The identification parameters.
     * @param metrics The metrics.
     * @param progressDialog The progress dialog.
     * @param nThreads The number of threads.
     * @param exceptionHandler The exception handler.
     */
    public StarHider(
            Identification identification,
            FilterParameters filterParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SpectrumProvider spectrumProvider,
            GeneMaps geneMaps,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters,
            Metrics metrics,
            ProgressDialogX progressDialog,
            int nThreads,
            ExceptionHandler exceptionHandler
    ) {

        this.identification = identification;
        this.filterPreferences = filterParameters;
        this.sequenceProvider = sequenceProvider;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.spectrumProvider = spectrumProvider;
        this.geneMaps = geneMaps;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.identificationParameters = identificationParameters;
        this.metrics = metrics;
        this.progressDialog = progressDialog;
        this.nThreads = nThreads;
        this.exceptionHandler = exceptionHandler;

    }

    /**
     * Updates the star/hide status of all identification items.
     */
    public void starHide() {

        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Hiding/Starring Matches. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("Star/Hide") {
            @Override
            public void run() {

                try {

                    ExecutorService pool = Executors.newFixedThreadPool(nThreads);

                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size());

                    ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(progressDialog);

                    ArrayList<StarHiderRunnable> runnables = new ArrayList<>(nThreads);

                    for (int i = 1; i <= nThreads && !progressDialog.isRunCanceled(); i++) {

                        StarHiderRunnable starHiderRunnable = new StarHiderRunnable(proteinMatchesIterator, progressDialog);
                        pool.submit(starHiderRunnable);
                        runnables.add(starHiderRunnable);

                    }

                    if (progressDialog.isRunCanceled()) {

                        pool.shutdownNow();
                        return;

                    }

                    pool.shutdown();

                    if (!pool.awaitTermination(identification.getProteinIdentification().size(), TimeUnit.MINUTES)) {

                        throw new TimeoutException("Hiding/Starring matches timed out. Please contact the developers.");

                    }

                    HashMap<String, ArrayList<Double>> fractionMW = new HashMap<>();

                    for (StarHiderRunnable starHiderRunnable : runnables) {

                        HashMap<String, ArrayList<Double>> threadFractionMW = starHiderRunnable.getThreadFractionMW();

                        for (String fraction : threadFractionMW.keySet()) {

                            ArrayList<Double> mws = fractionMW.get(fraction),
                                    threadMws = threadFractionMW.get(fraction);

                            if (mws == null) {

                                fractionMW.put(fraction, threadMws);

                            } else {

                                mws.addAll(threadMws);

                            }
                        }
                    }

                    // set the observed fractional molecular weights per fraction
                    metrics.setObservedFractionalMassesAll(fractionMW);

                    progressDialog.setRunFinished();

                } catch (Exception e) {

                    exceptionHandler.catchException(e);

                }
            }

        }.start();
    }

    /**
     * Stars a protein match.
     *
     * @param matchKey the key of the match
     */
    public void starProtein(
            long matchKey
    ) {

        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                validated = true;

            }
        }

        if (!validated) {

            ProteinFilter proteinFilter;

            if (!filterPreferences.getProteinStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {

                proteinFilter = new ProteinFilter(MatchFilter.MANUAL_SELECTION);
                proteinFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getProteinStarFilters().put(proteinFilter.getName(), proteinFilter);

            } else {

                proteinFilter = filterPreferences.getProteinStarFilters().get(MatchFilter.MANUAL_SELECTION);

            }

            proteinFilter.addManualValidation(matchKey);

        }

        psParameter.setStarred(true);

    }

    /**
     * Unstars a protein match.
     *
     * @param matchKey the key of the match
     */
    public void unStarProtein(long matchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setStarred(false);

    }

    /**
     * Hides a protein match.
     *
     * @param matchKey the key of the match
     */
    public void hideProtein(long matchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                validated = true;

            }
        }

        if (!validated) {

            ProteinFilter proteinFilter;

            if (!filterPreferences.getProteinHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {

                proteinFilter = new ProteinFilter(MatchFilter.MANUAL_SELECTION);
                proteinFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getProteinHideFilters().put(proteinFilter.getName(), proteinFilter);

            } else {

                proteinFilter = filterPreferences.getProteinHideFilters().get(MatchFilter.MANUAL_SELECTION);

            }

            proteinFilter.addManualValidation(matchKey);

        }

        psParameter.setHidden(true);

    }

    /**
     * Unhides a protein match.
     *
     * @param matchKey the key of the match
     */
    public void unHideProtein(long matchKey) {

        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setHidden(true);

    }

    /**
     * Stars a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void starPeptide(long matchKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                validated = true;

            }
        }

        if (!validated) {

            PeptideFilter peptideFilter;
            if (!filterPreferences.getPeptideStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {

                peptideFilter = new PeptideFilter(MatchFilter.MANUAL_SELECTION);
                peptideFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPeptideStarFilters().put(peptideFilter.getName(), peptideFilter);

            } else {

                peptideFilter = filterPreferences.getPeptideStarFilters().get(MatchFilter.MANUAL_SELECTION);

            }

            peptideFilter.addManualValidation(matchKey);

        }

        psParameter.setStarred(true);

    }

    /**
     * Unstars a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void unStarPeptide(long matchKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setStarred(false);

    }

    /**
     * Hides a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void hidePeptide(long matchKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                validated = true;

            }
        }

        if (!validated) {

            PeptideFilter peptideFilter;

            if (!filterPreferences.getPeptideHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {

                peptideFilter = new PeptideFilter(MatchFilter.MANUAL_SELECTION);
                peptideFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPeptideHideFilters().put(peptideFilter.getName(), peptideFilter);

            } else {

                peptideFilter = filterPreferences.getPeptideHideFilters().get(MatchFilter.MANUAL_SELECTION);

            }

            peptideFilter.addManualValidation(matchKey);

        }

        psParameter.setHidden(true);

    }

    /**
     * Unhides a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void unHidePeptide(long matchKey) {

        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setHidden(false);

    }

    /**
     * Stars a PSM match.
     *
     * @param matchKey the key of the match
     */
    public void starPsm(long matchKey) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                validated = true;

            }
        }

        if (!validated) {

            PsmFilter psmFilter;
            if (!filterPreferences.getPsmStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {

                psmFilter = new PsmFilter(MatchFilter.MANUAL_SELECTION);
                psmFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPsmStarFilters().put(psmFilter.getName(), psmFilter);

            } else {

                psmFilter = filterPreferences.getPsmStarFilters().get(MatchFilter.MANUAL_SELECTION);

            }

            psmFilter.addManualValidation(matchKey);

        }

        psParameter.setStarred(true);

    }

    /**
     * Unstars a PSM match.
     *
     * @param matchKey the key of the match
     */
    public void unStarPsm(long matchKey) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setStarred(false);

    }

    /**
     * Hides a PSM match.
     *
     * @param matchKey the key of the match
     */
    public void hidePsm(long matchKey) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                validated = true;

            }
        }

        if (!validated) {

            PsmFilter psmFilter;
            if (!filterPreferences.getPsmHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {

                psmFilter = new PsmFilter(MatchFilter.MANUAL_SELECTION);
                psmFilter.setDescription("Manual selection via the graphical interface");
                filterPreferences.getPsmHideFilters().put(psmFilter.getName(), psmFilter);

            } else {

                psmFilter = filterPreferences.getPsmHideFilters().get(MatchFilter.MANUAL_SELECTION);

            }

            psmFilter.addManualValidation(matchKey);

        }

        psParameter.setHidden(true);

    }

    /**
     * Unhides a psm match.
     *
     * @param matchKey the key of the match
     */
    public void unHidePsm(long matchKey) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(
                    matchKey,
                    identification,
                    geneMaps,
                    identificationFeaturesGenerator,
                    identificationParameters,
                    sequenceProvider,
                    proteinDetailsProvider,
                    spectrumProvider
            )) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setHidden(false);

    }

    /**
     * Tests whether a protein match should be hidden according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isProteinHidden(long matchKey) {

        return filterPreferences.getProteinHideFilters().values().stream()
                .anyMatch(
                        matchFilter -> matchFilter.isActive()
                        && matchFilter.isValidated(
                                matchKey,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider
                        )
                );
    }

    /**
     * Tests whether a peptide match should be hidden according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isPeptideHidden(long matchKey) {

        return filterPreferences.getPeptideHideFilters().values().stream()
                .anyMatch(
                        matchFilter -> matchFilter.isActive()
                        && matchFilter.isValidated(
                                matchKey,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider
                        )
                );
    }

    /**
     * Tests whether a PSM match should be hidden according to the implemented
     * filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isPsmHidden(long matchKey) {

        return filterPreferences.getPsmHideFilters().values().stream()
                .anyMatch(
                        matchFilter -> matchFilter.isActive()
                        && matchFilter.isValidated(
                                matchKey,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider
                        )
                );

    }

    /**
     * Tests whether a protein match should be starred according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isProteinStarred(long matchKey) {

        return filterPreferences.getProteinStarFilters().values().stream()
                .anyMatch(
                        matchFilter -> matchFilter.isActive()
                        && matchFilter.isValidated(
                                matchKey,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider
                        )
                );

    }

    /**
     * Tests whether a peptide match should be starred according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isPeptideStarred(long matchKey) {

        return filterPreferences.getPeptideStarFilters().values().stream()
                .anyMatch(
                        matchFilter -> matchFilter.isActive()
                        && matchFilter.isValidated(
                                matchKey,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider
                        )
                );

    }

    /**
     * Tests whether a PSM match should be starred according to the implemented
     * filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isPsmStarred(long matchKey) {

        return filterPreferences.getPsmStarFilters().values().stream()
                .anyMatch(
                        matchFilter -> matchFilter.isActive()
                        && matchFilter.isValidated(
                                matchKey,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                identificationParameters,
                                sequenceProvider,
                                proteinDetailsProvider,
                                spectrumProvider
                        )
                );
    }

    /**
     * Runnable processing matches.
     *
     * @author Marc Vaudel
     */
    private class StarHiderRunnable implements Runnable {

        /**
         * The waiting handler.
         */
        private final WaitingHandler waitingHandler;
        /**
         * The fraction mw map for this thread
         */
        private final HashMap<String, ArrayList<Double>> threadFractionMW = new HashMap<>();
        /**
         * An iterator for the protein matches
         */
        private final ProteinMatchesIterator proteinMatchesIterator;
        /**
         * The spectrum annotator to use for this thread
         */
        private final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        /**
         * Constructor.
         *
         * @param proteinMatchesIterator an iterator of the protein matches to
         * inspect
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public StarHiderRunnable(
                ProteinMatchesIterator proteinMatchesIterator,
                WaitingHandler waitingHandler
        ) {

            this.proteinMatchesIterator = proteinMatchesIterator;
            this.waitingHandler = waitingHandler;

        }

        @Override
        public void run() {
            try {

                ProteinMatch proteinMatch;
                while ((proteinMatch = proteinMatchesIterator.next()) != null && !progressDialog.isRunCanceled()) {

                    long proteinKey = proteinMatch.getKey();

                    boolean peptidePassed = false;

                    for (long peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        boolean psmpassed = false;

                        for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                            if (isPsmHidden(spectrumKey)) {

                                psParameter.setHidden(true);

                            } else {

                                psParameter.setHidden(false);
                                psmpassed = true;

                            }

                            psParameter.setStarred(isPsmStarred(spectrumKey));

                        }

                        PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                        if (!psmpassed) {

                            psParameter.setHidden(true);

                        } else if (isPeptideHidden(peptideKey)) {

                            psParameter.setHidden(true);

                        } else {

                            psParameter.setHidden(false);
                            peptidePassed = true;

                        }

                        psParameter.setStarred(isPeptideStarred(peptideKey));

                    }

                    PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

                    if (!peptidePassed) {

                        psParameter.setHidden(true);

                    } else {

                        psParameter.setHidden(isProteinHidden(proteinKey));

                    }

                    psParameter.setStarred(isProteinStarred(proteinKey));

                    // update the observed fractional molecular weights per fraction
                    if (!psParameter.getHidden() && psParameter.getMatchValidationLevel().isValidated() && !proteinMatch.isDecoy()) {

                        String proteinSequence = sequenceProvider.getSequence(proteinMatch.getLeadingAccession());
                        double proteinMW = ProteinUtils.computeMolecularWeight(proteinSequence);

                        for (String fraction : psParameter.getFractions()) {

                            // set the fraction molecular weights
                            if (psParameter.getFractionConfidence(fraction) > identificationParameters.getFractionParameters().getProteinConfidenceMwPlots()) {

                                if (threadFractionMW.containsKey(fraction)) {

                                    threadFractionMW.get(fraction).add(proteinMW);

                                } else {

                                    ArrayList<Double> mw = new ArrayList<>();
                                    mw.add(proteinMW);
                                    threadFractionMW.put(fraction, mw);

                                }
                            }
                        }
                    }

                    progressDialog.increasePrimaryProgressCounter();

                    if (progressDialog.isRunCanceled()) {

                        break;

                    }
                }

            } catch (Exception e) {

                exceptionHandler.catchException(e);
                progressDialog.setRunCanceled();

            }
        }

        /**
         * Returns the map of molecular weights per fraction of the non hidden
         * proteins found in this thread.
         *
         * @return the map of molecular weights per fraction of the non hidden
         * proteins found in this thread
         */
        public HashMap<String, ArrayList<Double>> getThreadFractionMW() {

            return threadFractionMW;

        }
    }
}
