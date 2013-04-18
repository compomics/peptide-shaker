package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import no.uib.jsparklines.data.StartIndexes;
import no.uib.jsparklines.data.XYDataPoint;

/**
 * Table model for a set of peptide matches.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideTableModel extends SelfUpdatingTableModel {

    /**
     * The main GUI class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * A list of ordered peptide keys.
     */
    private ArrayList<String> peptideKeys = null;
    /**
     * The main accession of the protein match to which the list of peptides is
     * attached.
     */
    private String proteinAccession;
    /**
     * Indicates whether data in DB shall be used.
     */
    private boolean useDB = false;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param proteinAccession
     * @param peptideKeys
     */
    public PeptideTableModel(PeptideShakerGUI peptideShakerGUI, String proteinAccession, ArrayList<String> peptideKeys) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        this.peptideKeys = peptideKeys;
        this.proteinAccession = proteinAccession;
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI
     * @param proteinAccession
     * @param peptideKeys
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI, String proteinAccession, ArrayList<String> peptideKeys) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        this.peptideKeys = peptideKeys;
        this.proteinAccession = proteinAccession;
    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        peptideKeys = null;
    }

    /**
     * Constructor which sets a new empty table.
     *
     */
    public PeptideTableModel() {
    }

    @Override
    public int getRowCount() {
        if (peptideKeys != null) {
            return peptideKeys.size();
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
                return "PI";
            case 3:
                return "Sequence";
            case 4:
                return "Start";
            case 5:
                return "#Spectra";
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
                    String peptideKey = peptideKeys.get(row);
                    PSParameter pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.isStarred();
                case 2:
                    peptideKey = peptideKeys.get(row);
                    pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.getProteinInferenceClass();
                case 3:
                    peptideKey = peptideKeys.get(row);
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey, useDB);
                    if (!useDB && peptideMatch == null) {
                        dataMissingAtRow(row);
                        return Peptide.getSequence(peptideKey);
                    }
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideKey, true, true, true);
                case 4:
                    peptideKey = peptideKeys.get(row);
                    ArrayList<Integer> indexes;
                    try {
                        Protein currentProtein = sequenceFactory.getProtein(proteinAccession);
                        indexes = currentProtein.getPeptideStart(Peptide.getSequence(peptideKey));
                    } catch (IOException e) {
                        peptideShakerGUI.catchException(e);
                        return "IO Exception";
                    }
                    Collections.sort(indexes);
                    return new StartIndexes(indexes); // note: have to be "packed" like this in order to be able to sort on the first index if multiple indexes
                case 5:
                    peptideKey = peptideKeys.get(row);
                    peptideMatch = identification.getPeptideMatch(peptideKey, useDB);
                    if (!useDB
                            && (peptideMatch == null || !peptideShakerGUI.getIdentificationFeaturesGenerator().nValidatedSpectraForPeptideInCache(peptideKey))
                            && (peptideMatch == null || !identification.peptideDetailsInCache(peptideKey))) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    int nValidatedSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectraForPeptide(peptideKey);
                    int nSpectra = peptideMatch.getSpectrumMatches().size();
                    return new XYDataPoint(nValidatedSpectra, nSpectra - nValidatedSpectra, false);
                case 6:
                    peptideKey = peptideKeys.get(row);
                    pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                        return pSParameter.getPeptideScore();
                    } else {
                        return pSParameter.getPeptideConfidence();
                    }
                case 7:
                    peptideKey = peptideKeys.get(row);
                    pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.isValidated();
                default:
                    return "";
            }
        } catch (SQLNonTransientConnectionException e) {
            // this one can be ignored i think?
            return null;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return null;
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
        useDB = true;
        peptideShakerGUI.catchException(e);
    }

    @Override
    protected int loadDataForRows(int start, int end, boolean interrupted) {
        ArrayList<String> tempKeys = new ArrayList<String>();
        for (int i = start; i <= end; i++) {
            String peptideKey = peptideKeys.get(i);
            tempKeys.add(peptideKey);
        }
        try {
            loadPeptideObjects(tempKeys);

            for (String peptideKey : tempKeys) {
                if (interrupted) {
                    loadPeptideObjects(tempKeys);
                    return start;
                }
                peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectraForPeptide(peptideKey);
                loadPeptideObjects(tempKeys);
            }
        } catch (Exception e) {
            catchException(e);
            return start;
        }
        return end;
    }

    /**
     * Loads the peptide matches and peptide parameters in cache.
     *
     * @param keys the keys to load
     * @throws SQLException
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void loadPeptideObjects(ArrayList<String> keys) throws SQLException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        identification.loadPeptideMatches(keys, null);
        identification.loadPeptideMatchParameters(keys, new PSParameter(), null);
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        ArrayList<String> reversedList = new ArrayList(peptideKeys);
        Collections.reverse(reversedList);
        try {
        if (column == 1
            || column == 2
                    || column == 6
                    || column == 7) {
        identification.loadPeptideMatchParameters(reversedList, new PSParameter(), null);
        } else if (column == 3
                || column == 4
                || column == 5) {
            identification.loadPeptideMatches(reversedList, null);
        }
        for (String peptideKey : reversedList) {
            if (column == 5) {
                peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectraForPeptide(peptideKey);
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
