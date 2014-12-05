package eu.isas.peptideshaker.utils;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import java.awt.Toolkit;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
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
        progressDialog.setTitle("Hiding/Starring Items. Please Wait...");

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
                    Identification identification = peptideShakerGUI.getIdentification();
                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size());

                    HashMap<String, ArrayList<Double>> fractionMW = new HashMap<String, ArrayList<Double>>();

                    PSParameter psParameter = new PSParameter();
                    ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
                    parameters.add(psParameter);
                    ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters);

                    while (proteinMatchesIterator.hasNext()) {
                        ProteinMatch proteinMatch = proteinMatchesIterator.next();
                        String proteinKey = proteinMatch.getKey();

                        if (progressDialog.isRunCanceled()) {
                            break;
                        }

                        boolean peptideSurvived = false;

                        for (String peptideKey : proteinMatch.getPeptideMatchesKeys()) {

                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                            boolean psmSurvived = false;

                            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {

                                if (progressDialog.isRunCanceled()) {
                                    break;
                                }

                                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                                if (isPsmHidden(spectrumKey)) {
                                    psParameter.setHidden(true);
                                } else {
                                    psParameter.setHidden(false);
                                    psmSurvived = true;
                                }

                                psParameter.setStarred(isPsmStarred(spectrumKey));
                                identification.updateSpectrumMatchParameter(spectrumKey, psParameter);
                            }

                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

                            if (!psmSurvived) {
                                psParameter.setHidden(true);
                            } else if (isPeptideHidden(peptideKey)) {
                                psParameter.setHidden(true);
                            } else {
                                psParameter.setHidden(false);
                                peptideSurvived = true;
                            }

                            psParameter.setStarred(isPeptideStarred(peptideKey));

                            identification.updatePeptideMatchParameter(peptideKey, psParameter);
                        }

                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

                        if (!peptideSurvived) {
                            psParameter.setHidden(true);
                        } else {
                            psParameter.setHidden(isProteinHidden(proteinKey));
                        }

                        psParameter.setStarred(isProteinStarred(proteinKey));

                        identification.updateProteinMatchParameter(proteinKey, psParameter);

                        // update the observed fractional molecular weights per fraction
                        if (!psParameter.isHidden() && psParameter.getMatchValidationLevel().isValidated() && !proteinMatch.isDecoy()) {

                            Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());

                            for (String fraction : psParameter.getFractions()) {

                                // set the fraction molecular weights
                                if (psParameter.getFractionConfidence(fraction) > peptideShakerGUI.getProcessingPreferences().getProteinConfidenceMwPlots()) {
                                    if (fractionMW.containsKey(fraction)) {
                                        fractionMW.get(fraction).add(proteinMW);
                                    } else {
                                        ArrayList<Double> mw = new ArrayList<Double>();
                                        mw.add(proteinMW);
                                        fractionMW.put(fraction, mw);
                                    }
                                }
                            }
                        }

                        progressDialog.increasePrimaryProgressCounter();
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
     * @param match the key of the match
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     */
    public void starProtein(String match) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getProteinMatchParameter(match, psParameter);
            boolean validated = false;

            for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
                if (matchFilter.getExceptions().contains(match)) {
                    matchFilter.removeException(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
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
                proteinFilter.addManualValidation(match);
            }

            psParameter.setStarred(true);
            identification.updateProteinMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Unstars a protein match.
     *
     * @param match the key of the match
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     */
    public void unStarProtein(String match) throws IOException, ClassNotFoundException, SQLException, InterruptedException {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getProteinMatchParameter(match, psParameter);

            for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
                if (matchFilter.getManualValidation().contains(match)) {
                    matchFilter.removeManualValidation(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                    matchFilter.addException(match);
                }
            }

            psParameter.setStarred(false);
            identification.updateProteinMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Hides a protein match.
     *
     * @param match the key of the match
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     */
    public void hideProtein(String match) throws IOException, ClassNotFoundException, SQLException, InterruptedException {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getProteinMatchParameter(match, psParameter);
            boolean validated = false;

            for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
                if (matchFilter.getExceptions().contains(match)) {
                    matchFilter.removeException(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
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
                proteinFilter.addManualValidation(match);
            }

            psParameter.setHidden(true);
            identification.updateProteinMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Unhides a protein match.
     *
     * @param match the key of the match
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     */
    public void unHideProtein(String match) throws IOException, ClassNotFoundException, SQLException, InterruptedException {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getProteinMatchParameter(match, psParameter);
            for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
                if (matchFilter.getManualValidation().contains(match)) {
                    matchFilter.removeManualValidation(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                    matchFilter.addException(match);
                }
            }

            psParameter.setHidden(true);
            identification.updateProteinMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Stars a peptide match.
     *
     * @param match the key of the match
     */
    public void starPeptide(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(match, psParameter);
            boolean validated = false;

            for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
                if (matchFilter.getExceptions().contains(match)) {
                    matchFilter.removeException(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator())) {
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
                peptideFilter.addManualValidation(match);
            }

            psParameter.setStarred(true);
            identification.updatePeptideMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Unstars a peptide match.
     *
     * @param match the key of the match
     */
    public void unStarPeptide(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(match, psParameter);

            for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
                if (matchFilter.getManualValidation().contains(match)) {
                    matchFilter.removeManualValidation(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator())) {
                    matchFilter.addException(match);
                }
            }

            psParameter.setStarred(false);
            identification.updatePeptideMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Hides a peptide match.
     *
     * @param match the key of the match
     */
    public void hidePeptide(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(match, psParameter);
            boolean validated = false;

            for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
                if (matchFilter.getExceptions().contains(match)) {
                    matchFilter.removeException(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator())) {
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
                peptideFilter.addManualValidation(match);
            }

            psParameter.setHidden(true);
            identification.updatePeptideMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Unhides a peptide match.
     *
     * @param match the key of the match
     */
    public void unHidePeptide(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(match, psParameter);

            for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
                if (matchFilter.getManualValidation().contains(match)) {
                    matchFilter.removeManualValidation(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator())) {
                    matchFilter.addException(match);
                }
            }

            psParameter.setHidden(false);
            identification.updatePeptideMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Stars a PSM match.
     *
     * @param match the key of the match
     */
    public void starPsm(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(match, psParameter);
            boolean validated = false;

            if (!validated) {
                for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
                    if (matchFilter.getExceptions().contains(match)) {
                        matchFilter.removeException(match);
                    }
                    if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                        validated = true;
                    }
                }
                PsmFilter psmFilter;
                if (!filterPreferences.getPsmStarFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                    psmFilter = new PsmFilter(MatchFilter.MANUAL_SELECTION);
                    psmFilter.setCharges(peptideShakerGUI.getMetrics().getFoundCharges());
                    psmFilter.setFileNames(peptideShakerGUI.getIdentification().getSpectrumFiles());
                    psmFilter.setDescription("Manual selection via the graphical interface");
                    filterPreferences.getPsmStarFilters().put(psmFilter.getName(), psmFilter);
                } else {
                    psmFilter = filterPreferences.getPsmStarFilters().get(MatchFilter.MANUAL_SELECTION);
                }
                psmFilter.addManualValidation(match);
            }

            psParameter.setStarred(true);
            identification.updateSpectrumMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Unstars a PSM match.
     *
     * @param match the key of the match
     */
    public void unStarPsm(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(match, psParameter);

            for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
                if (matchFilter.getManualValidation().contains(match)) {
                    matchFilter.removeManualValidation(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                    matchFilter.addException(match);
                }
            }

            psParameter.setStarred(false);
            identification.updateSpectrumMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Hides a PSM match.
     *
     * @param match the key of the match
     */
    public void hidePsm(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(match, psParameter);
            boolean validated = false;

            if (!validated) {
                for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
                    if (matchFilter.getExceptions().contains(match)) {
                        matchFilter.removeException(match);
                    }
                    if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                        validated = true;
                    }
                }
                PsmFilter psmFilter;
                if (!filterPreferences.getPsmHideFilters().containsKey(MatchFilter.MANUAL_SELECTION)) {
                    psmFilter = new PsmFilter(MatchFilter.MANUAL_SELECTION);
                    psmFilter.setCharges(peptideShakerGUI.getMetrics().getFoundCharges());
                    psmFilter.setFileNames(peptideShakerGUI.getIdentification().getSpectrumFiles());
                    psmFilter.setDescription("Manual selection via the graphical interface");
                    filterPreferences.getPsmHideFilters().put(psmFilter.getName(), psmFilter);
                } else {
                    psmFilter = filterPreferences.getPsmHideFilters().get(MatchFilter.MANUAL_SELECTION);
                }
                psmFilter.addManualValidation(match);
            }

            psParameter.setHidden(true);
            identification.updateSpectrumMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Unhides a psm match.
     *
     * @param match the key of the match
     */
    public void unHidePsm(String match) {

        try {
            Identification identification = peptideShakerGUI.getIdentification();
            FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(match, psParameter);

            for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
                if (matchFilter.getManualValidation().contains(match)) {
                    matchFilter.removeManualValidation(match);
                }
                if (matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                    matchFilter.addException(match);
                }
            }

            psParameter.setHidden(false);
            identification.updateSpectrumMatchParameter(match, psParameter);
            peptideShakerGUI.setDataSaved(false);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Tests whether a protein match should be hidden according to the
     * implemented filters.
     *
     * @param match the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public boolean isProteinHidden(String match) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (ProteinFilter matchFilter : filterPreferences.getProteinHideFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a peptide match should be hidden according to the
     * implemented filters.
     *
     * @param match the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     */
    public boolean isPeptideHidden(String match) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (PeptideFilter matchFilter : filterPreferences.getPeptideHideFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a psm match should be hidden according to the implemented
     * filters.
     *
     * @param match the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public boolean isPsmHidden(String match) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (PsmFilter matchFilter : filterPreferences.getPsmHideFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a protein match should be starred according to the
     * implemented filters.
     *
     * @param match the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     */
    public boolean isProteinStarred(String match) throws IOException, ClassNotFoundException, SQLException, InterruptedException, MzMLUnmarshallerException {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (ProteinFilter matchFilter : filterPreferences.getProteinStarFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a peptide match should be starred according to the
     * implemented filters.
     *
     * @param match the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public boolean isPeptideStarred(String match) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();

        for (PeptideFilter matchFilter : filterPreferences.getPeptideStarFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a PSM match should be starred according to the implemented
     * filters.
     *
     * @param match the key of the match
     *
     * @return a boolean indicating whether a protein match should be hidden
     * according to the implemented filters
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException
     * @throws java.lang.InterruptedException
     */
    public boolean isPsmStarred(String match) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        for (PsmFilter matchFilter : filterPreferences.getPsmStarFilters().values()) {
            if (matchFilter.isActive() && matchFilter.isValidated(match, peptideShakerGUI.getIdentification(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters())) {
                return true;
            }
        }
        return false;
    }
}
