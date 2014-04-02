package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import java.util.ArrayList;

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
                return "ID";
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
            int viewIndex = getViewIndex(row);

            if (viewIndex < psmKeys.size()) { // escape possible null pointer

                String psmKey = psmKeys.get(viewIndex);
                boolean useDB = !isSelfUpdating();

                switch (column) {
                    case 0:
                        return viewIndex + 1;
                    case 1:
                        PSParameter psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        return psParameter.isStarred();
                    case 2:
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey, useDB && !isScrolling);
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        return SpectrumIdentificationPanel.isBestPsmEqualForAllIdSoftware(spectrumMatch, peptideShakerGUI.getSearchParameters());
                    case 3:
                        spectrumMatch = identification.getSpectrumMatch(psmKey, useDB && !isScrolling);
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(spectrumMatch, true, true, true);
                    case 4:
                        spectrumMatch = identification.getSpectrumMatch(psmKey, useDB && !isScrolling);
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            return spectrumMatch.getBestTagAssumption().getIdentificationCharge().value;
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");
                        }
                    case 5:
                        spectrumMatch = identification.getSpectrumMatch(psmKey, useDB && !isScrolling);
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        Precursor precursor = peptideShakerGUI.getPrecursor(psmKey);
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return Math.abs(spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()));
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            return Math.abs(spectrumMatch.getBestTagAssumption().getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()));
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");
                        }
                    case 6:
                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        if (psParameter != null) {
                            if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                                return psParameter.getPsmScore();
                            } else {
                                return psParameter.getPsmConfidence();
                            }
                        } else {
                            return null;
                        }
                    case 7:
                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        if (psParameter != null) {
                            return psParameter.getMatchValidationLevel().getIndex();
                        } else {
                            return null;
                        }
                    default:
                        return null;
                }
            } else {
                return null;
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

    @Override
    protected void catchException(Exception e) {
        setSelfUpdating(false);
        peptideShakerGUI.catchException(e);
    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, boolean interrupted) {
        try {
            ArrayList<String> tempPsmKeys = new ArrayList<String>();
            for (int i : rows) {
                tempPsmKeys.add(psmKeys.get(i));
            }
            if (interrupted) {
                return rows.get(0);
            }
            identification.loadSpectrumMatches(tempPsmKeys, null);
            if (interrupted) {
                return rows.get(0);
            }
            identification.loadSpectrumMatchParameters(tempPsmKeys, new PSParameter(), null);
            return rows.get(rows.size() - 1);
        } catch (Exception e) {
            catchException(e);
            return rows.get(0);
        }
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        try {
            if (column == 1
                    || column == 6
                    || column == 7) {
                identification.loadSpectrumMatchParameters(psmKeys, new PSParameter(), null);
            } else if (column == 2
                    || column == 3
                    || column == 4
                    || column == 5) {
                identification.loadSpectrumMatches(psmKeys, null);
            }
        } catch (Exception e) {
            catchException(e);
        }
    }
}
