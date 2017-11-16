package eu.isas.peptideshaker.utils;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
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
     * The sequence factory.
     */
    private final SequenceFactory sequenceFactory = SequenceFactory.getInstance();
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

                    PSParameter psParameter = new PSParameter();
                    ArrayList<UrParameter> parameters = new ArrayList<>(1);
                    parameters.add(psParameter);
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
                    if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
                        throw new InterruptedException("Hiding/Starring matches timed out. Please contact the developers.");
                    }

                    HashMap<String, ArrayList<Double>> fractionMW = new HashMap<>();
                    for (StarHiderRunnable starHiderRunnable : runnables) {
                        HashMap<String, ArrayList<Double>> threadFractionMW = starHiderRunnable.getThreadFractionMW();
                        for (String fraction : threadFractionMW.keySet()) 
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void starProtein(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void unStarProtein(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);

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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void hideProtein(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void unHideProtein(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void starPeptide(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) ((PeptideMatch) identification.retrieveObject(matchKey)).getUrParam(psParameter);
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void unStarPeptide(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void hidePeptide(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void unHidePeptide(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void starPsm(String matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void unStarPsm(String matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void hidePsm(String matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public void unHidePsm(String matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {

        Identification identification = peptideShakerGUI.getIdentification();
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public boolean isProteinHidden(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a peptide match should be hidden according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public boolean isPeptideHidden(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {
                return true;
            }
        }

        return false;
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public boolean isPsmHidden(String matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a protein match should be starred according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public boolean isProteinStarred(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a peptide match should be starred according to the
     * implemented filters.
     *
     * @param matchKey the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public boolean isPeptideStarred(String matchKey) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), null)) {
                return true;
            }
        }

        return false;
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
     *
     * @throws IOException thrown whenever an error occurs while reading or
     * writing a file.
     * @throws InterruptedException thrown whenever a threading error occurs
     * while processing the match.
     * @throws SQLException thrown whenever an error occurs while interacting
     * with a back-end database.
     * @throws ClassNotFoundException thrown whenever an error occurs while
     * deserilalizing an object from a database.
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown whenever
     * an error occurs while reading an mzML file.
     * @throws org.apache.commons.math.MathException thrown whenever an error
     * occurs while making statistics on a distribution.
     */
    public boolean isPsmStarred(String matchKey, PeptideSpectrumAnnotator peptideSpectrumAnnotator) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException, MathException {
        FilterParameters filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(matchKey, peptideShakerGUI.getIdentification(), peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideSpectrumAnnotator)) {
                return true;
            }
        }
        return false;

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

                PSParameter psParameter = new PSParameter();

                while (proteinMatchesIterator.hasNext() && !progressDialog.isRunCanceled()) {

                    ProteinMatch proteinMatch = proteinMatchesIterator.next();

                    String proteinKey = proteinMatch.getKey();

                    if (progressDialog.isRunCanceled()) {
                        break;
                    }

                    boolean peptidePassed = false;

                    for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                        if (progressDialog.isRunCanceled()) {
                            break;
                        }

                        PeptideMatch peptideMatch = (PeptideMatch) identification.retrieveObject(peptideKey);
                        boolean psmpassed = false;

                        for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            psParameter = (PSParameter) ((SpectrumMatch) identification.retrieveObject(spectrumKey)).getUrParam(psParameter);

                            if (isPsmHidden(spectrumKey, peptideSpectrumAnnotator)) {
                                psParameter.setHidden(true);
                            } else {
                                psParameter.setHidden(false);
                                psmpassed = true;
                            }

                            psParameter.setStarred(isPsmStarred(spectrumKey, peptideSpectrumAnnotator));
                        }

                        psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);

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

                    psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(proteinKey)).getUrParam(psParameter);

                    if (!peptidePassed) {
                        psParameter.setHidden(true);
                    } else {
                        psParameter.setHidden(isProteinHidden(proteinKey));
                    }

                    psParameter.setStarred(isProteinStarred(proteinKey));

                    // update the observed fractional molecular weights per fraction
                    if (!psParameter.getHidden() && psParameter.getMatchValidationLevel().isValidated() && !proteinMatch.isDecoy()) {

                        Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getLeadingAccession());

                        for (String fraction : psParameter.getFractions()) {

                            // set the fraction molecular weights
                            if (psParameter.getFractionConfidence(fraction) > peptideShakerGUI.getIdentificationParameters().getFractionSettings().getProteinConfidenceMwPlots()) {
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
