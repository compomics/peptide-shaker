package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Table model for a set of peptide to spectrum matches.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsmTableModel extends SelfUpdatingTableModel {

    /**
     * The main GUI class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * A list of ordered PSM keys.
     */
    private ArrayList<String> psmKeys = null;
    /**
     * Indicates whether data in DB shall be used.
     */
    private boolean useDB = false;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param psmKeys
     */
    public PsmTableModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> psmKeys) {
        setUpTableModel(peptideShakerGUI, psmKeys);
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI
     * @param psmKeys
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> psmKeys) {
        setUpTableModel(peptideShakerGUI, psmKeys);
    }

    /**
     * Set up the table model.
     *
     * @param peptideShakerGUI
     */
    private void setUpTableModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> psmKeys) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        this.psmKeys = psmKeys;
    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        psmKeys = null;
    }

    /**
     * Constructor which sets a new empty table.
     *
     */
    public PsmTableModel() {
    }

    @Override
    public int getRowCount() {
        if (psmKeys != null) {
            return psmKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return " ";
            case 1:
                return "  ";
            case 2:
                return "SE";
            case 3:
                return "Sequence";
            case 4:
                return "Charge";
            case 5:
                return "Mass Error";
            case 6:
                if (peptideShakerGUI != null && peptideShakerGUI.getDisplayPreferences().showScores()) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 7:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    String psmKey = psmKeys.get(row);
                    PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.isStarred();
                case 2:
                    psmKey = psmKeys.get(row);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey, useDB);
                    if (!useDB && spectrumMatch == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return SpectrumIdentificationPanel.isBestPsmEqualForAllSearchEngines(spectrumMatch);
                case 3:
                    psmKey = psmKeys.get(row);
                    spectrumMatch = identification.getSpectrumMatch(psmKey, useDB);
                    if (!useDB && spectrumMatch == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    PeptideAssumption bestAssumption = spectrumMatch.getBestAssumption();
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(bestAssumption.getPeptide(), true, true, true);
                case 4:
                    psmKey = psmKeys.get(row);
                    spectrumMatch = identification.getSpectrumMatch(psmKey, useDB);
                    if (!useDB && spectrumMatch == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                case 5:
                    psmKey = psmKeys.get(row);
                    spectrumMatch = identification.getSpectrumMatch(psmKey, useDB);
                    if (!useDB && spectrumMatch == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    bestAssumption = spectrumMatch.getBestAssumption();
                    Precursor precursor = peptideShakerGUI.getPrecursor(psmKey);
                    return Math.abs(bestAssumption.getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()));
                case 6:
                    psmKey = psmKeys.get(row);
                    pSParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                        return pSParameter.getPsmScore();
                    } else {
                        return pSParameter.getPsmConfidence();
                    }
                case 7:
                    psmKey = psmKeys.get(row);
                    pSParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.isValidated();
                default:
                    return "";
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return "";
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex) != null) {
                return getValueAt(i, columnIndex).getClass();
            }
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Sets whether or not data shall be looked for in the database. If false
     * only the cache will be used.
     *
     * @param useDB
     */
    public void useDB(boolean useDB) {
        this.useDB = useDB;
    }

    @Override
    protected void catchException(Exception e) {
        useDB = false;
        catchException(e);
    }

    @Override
    protected int loadDataForRows(int start, int end, boolean interrupted) {
        try {
            ArrayList<String> tempPsmKeys = new ArrayList<String>();
            for (int i = start; i <= end; i++) {
                tempPsmKeys.add(psmKeys.get(i));
            }
            if (interrupted) {
                return start;
            }
            identification.loadSpectrumMatches(tempPsmKeys, null);
            if (interrupted) {
                return start;
            }
            identification.loadSpectrumMatchParameters(tempPsmKeys, new PSParameter(), null);
            return end;
        } catch (Exception e) {
            catchException(e);
            return start;
        }
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        try {
            ArrayList<String> reversedList = new ArrayList(psmKeys);
            Collections.reverse(reversedList);
            if (column == 1
                    || column == 6
                    || column == 7) {
                identification.loadSpectrumMatchParameters(reversedList, new PSParameter(), null);
            } else if (column == 2
                    || column == 3
                    || column == 4
                    || column == 5) {
                identification.loadSpectrumMatches(reversedList, null);
            }
            for (String psmKey : reversedList) {
                if (column == 5) {
                    peptideShakerGUI.getPrecursor(psmKey);
                }
                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressValue();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            catchException(e);
        }
    }
}
