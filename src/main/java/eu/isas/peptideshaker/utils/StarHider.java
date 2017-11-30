package eu.isas.peptideshaker.utils;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterParameters;
import java.awt.Toolkit;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class provides information whether a hit should be hidden or starred.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class StarHider {

    /**
     * PeptideShakerGUI instance.
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;

    /**
     * Constructor.
     *
     * @param peptideShakerGUI the peptideShakerGUI main class
     */
    public StarHider(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
    }

    /**
     * Updates the star/hide status of all identification items.
     */
    public void starHide() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
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

                    int nThreads = peptideShakerGUI.getProcessingPreferences().getnThreads();
                    ExecutorService pool = Executors.newFixedThreadPool(nThreads);

                    Identification identification = peptideShakerGUI.getIdentification();
                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size());

                    ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(progressDialog);

                    ArrayList<StarHiderRunnable> runnables = new ArrayList<>(nThreads);
                    for (int i = 1; i <= nThreads && !progressDialog.isRunCanceled(); i++) {
                        StarHiderRunnable starHiderRunnable = new StarHiderRunnable(proteinMatchesIterator, progressDialog, peptideShakerGUI.getExceptionHandler());
                        pool.submit(starHiderRunnable);
                        runnables.add(starHiderRunnable);
                    }
                    if (progressDialog.isRunCanceled()) {
                        pool.shutdownNow();
                        return;
                    }
                    pool.shutdown();
                    if (!pool.awaitTermination(identification.getProteinIdentification().size(), TimeUnit.MINUTES)) {
                        throw new InterruptedException("Hiding/Starring matches timed out. Please contact the developers.");
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
                    peptideShakerGUI.getMetrics().setObservedFractionalMassesAll(fractionMW);

                    progressDialog.setRunFinished();
                    peptideShakerGUI.updateTabbedPanes();

                } catch (Exception e) {

                    peptideShakerGUI.catchException(e);

                }
            }
        }.start();
    }

    /**
     * Stars a protein match.
     *
     * @param matchKey the key of the match
     */
    public void starProtein(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }
            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {
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
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Unstars a protein match.
     *
     * @param matchKey the key of the match
     */
    public void unStarProtein(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setStarred(false);
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Hides a protein match.
     *
     * @param matchKey the key of the match
     */
    public void hideProtein(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {

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
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Unhides a protein match.
     *
     * @param matchKey the key of the match
     */
    public void unHideProtein(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setHidden(true);
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Stars a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void starPeptide(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
        boolean validated = false;

        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
            if (matchFilter.getExceptions().contains(matchKey)) {
                matchFilter.removeException(matchKey);
            }
            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {
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
        peptideShakerGUI.setDataSaved(false);
    }

    /**
     * Unstars a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void unStarPeptide(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((PeptideMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);

        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setStarred(false);
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Hides a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void hidePeptide(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((PeptideMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
        boolean validated = false;

        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {

            if (matchFilter.getExceptions().contains(matchKey)) {

                matchFilter.removeException(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {

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
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Unhides a peptide match.
     *
     * @param matchKey the key of the match
     */
    public void unHidePeptide(long matchKey) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((PeptideMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);

        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setHidden(false);
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Stars a PSM match.
     *
     * @param matchKey the key of the match
     * @param peptideSpectrumAnnotator the spectrum annotator to use during
     * filtering
     */
    public void starPsm(long matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((SpectrumMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
        boolean validated = false;

        if (!validated) {

            for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {

                if (matchFilter.getExceptions().contains(matchKey)) {

                    matchFilter.removeException(matchKey);

                }

                if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator)) {

                    validated = true;

                }
            }

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
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Unstars a PSM match.
     *
     * @param matchKey the key of the match
     * @param peptideSpectrumAnnotator the spectrum annotator to use during
     * filtering
     */
    public void unStarPsm(long matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((SpectrumMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);

        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator)) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setStarred(false);
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Hides a PSM match.
     *
     * @param matchKey the key of the match
     * @param peptideSpectrumAnnotator the spectrum annotator to use during
     * filtering
     */
    public void hidePsm(long matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((SpectrumMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
        boolean validated = false;

        if (!validated) {

            for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {

                if (matchFilter.getExceptions().contains(matchKey)) {

                    matchFilter.removeException(matchKey);

                }

                if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator)) {

                    validated = true;

                }
            }

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
        peptideShakerGUI.setDataSaved(false);

    }

    /**
     * Unhides a psm match.
     *
     * @param matchKey the key of the match
     * @param peptideSpectrumAnnotator the spectrum annotator to use during
     * filtering
     */
    public void unHidePsm(long matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((SpectrumMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);

        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {

            if (matchFilter.getManualValidation().contains(matchKey)) {

                matchFilter.removeManualValidation(matchKey);

            }

            if (matchFilter.isValidated(matchKey, identification, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator)) {

                matchFilter.addException(matchKey);

            }
        }

        psParameter.setHidden(false);
        peptideShakerGUI.setDataSaved(false);

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

        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();

        return filterPreferences.getProteinHideFilters().values().stream()
                .anyMatch(matchFilter -> matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null));

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

        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();

        return filterPreferences.getPeptideHideFilters().values().stream()
                .anyMatch(matchFilter -> matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null));

    }

    /**
     * Tests whether a psm match should be hidden according to the implemented
     * filters.
     *
     * @param matchKey the key of the match
     * @param peptideSpectrumAnnotator the spectrum annotator to use during
     * filtering
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isPsmHidden(long matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) {

        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();

        return filterPreferences.getPsmHideFilters().values().stream()
                .anyMatch(matchFilter -> matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator));

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

        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();

        return filterPreferences.getProteinStarFilters().values().stream()
                .anyMatch(matchFilter -> matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null));

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

        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();

        return filterPreferences.getPeptideStarFilters().values().stream()
                .anyMatch(matchFilter -> matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null));

    }

    /**
     * Tests whether a PSM match should be starred according to the implemented
     * filters.
     *
     * @param matchKey the key of the match
     * @param peptideSpectrumAnnotator the spectrum annotator to use during
     * filtering
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     */
    public boolean isPsmStarred(long matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) {

        FilterParameters filterPreferences = peptideShakerGUI.getFilterParameters();

        return filterPreferences.getPeptideStarFilters().values().stream()
                .anyMatch(matchFilter -> matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator));

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
        private WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private ExceptionHandler exceptionHandler;
        /**
         * The fraction mw map for this thread
         */
        private HashMap<String, ArrayList<Double>> threadFractionMW = new HashMap<>();
        /**
         * An iterator for the protein matches
         */
        private ProteinMatchesIterator proteinMatchesIterator;
        /**
         * The spectrum annotator to use for this thread
         */
        private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        /**
         * Constructor.
         *
         * @param proteinMatchesIterator an iterator of the protein matches to
         * inspect
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         * @param exceptionHandler handler for exceptions
         */
        public StarHiderRunnable(ProteinMatchesIterator proteinMatchesIterator, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.proteinMatchesIterator = proteinMatchesIterator;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void run() {
            try {

                Identification identification = peptideShakerGUI.getIdentification();

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

                            if (isPsmHidden(spectrumKey, peptideSpectrumAnnotator)) {

                                psParameter.setHidden(true);

                            } else {

                                psParameter.setHidden(false);
                                psmpassed = true;

                            }

                            psParameter.setStarred(isPsmStarred(spectrumKey, peptideSpectrumAnnotator));

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

                        Double proteinMW = ProteinUtils.computeMolecularWeight(proteinMatch.getLeadingAccession());

                        for (String fraction : psParameter.getFractions()) {

                            // set the fraction molecular weights
                            if (psParameter.getFractionConfidence(fraction) > peptideShakerGUI.getIdentificationParameters().getFractionParameters().getProteinConfidenceMwPlots()) {

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
