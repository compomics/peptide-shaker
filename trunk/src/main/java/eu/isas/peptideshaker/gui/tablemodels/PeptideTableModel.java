package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import no.uib.jsparklines.data.StartIndexes;

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
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param proteinAccession
     * @param peptideKeys
     * @throws IOException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public PeptideTableModel(PeptideShakerGUI peptideShakerGUI, String proteinAccession, ArrayList<String> peptideKeys)
            throws IOException, InterruptedException, ClassNotFoundException, IllegalArgumentException, SQLException {
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
            boolean useDB = !isSelfUpdating();
            int viewIndex = getViewIndex(row);

            if (viewIndex >= peptideKeys.size()) {
                return null;
            }

            String peptideKey = peptideKeys.get(viewIndex);

            switch (column) {
                case 0:
                    return viewIndex + 1;
                case 1:
                    PSParameter pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.isStarred();
                case 2:
                    pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.getProteinInferenceClass();
                case 3:
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey, useDB);
                    if (!useDB && peptideMatch == null) {
                        dataMissingAtRow(row);
                        return Peptide.getSequence(peptideKey);
                    }
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideMatch, true, true, true);
                case 4:
                    ArrayList<Integer> indexes;
                    if (sequenceFactory == null) {
                        return null;
                    }
                    try {
                        Protein currentProtein = sequenceFactory.getProtein(proteinAccession);
                        String peptideSequence = Peptide.getSequence(peptideKey);
                        indexes = currentProtein.getPeptideStart(peptideSequence,
                                PeptideShaker.MATCHING_TYPE,
                                peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
                    } catch (IOException e) {
                        peptideShakerGUI.catchException(e);
                        return "IO Exception";
                    }
                    Collections.sort(indexes);
                    return new StartIndexes(indexes); // note: have to be "packed" like this in order to be able to resetSorting on the first index if multiple indexes
                case 5:
                    peptideMatch = identification.getPeptideMatch(peptideKey, useDB);
                    if (!useDB
                            && (peptideMatch == null || !peptideShakerGUI.getIdentificationFeaturesGenerator().nValidatedSpectraForPeptideInCache(peptideKey))
                            && (peptideMatch == null || !identification.peptideDetailsInCache(peptideKey))) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }

                    double nConfidentSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNConfidentSpectraForPeptide(peptideKey);
                    double nDoubtfulSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectraForPeptide(peptideKey) - nConfidentSpectra;
                    int nSpectra = peptideMatch.getSpectrumMatches().size();

                    ArrayList<Double> values = new ArrayList<Double>();
                    values.add(nConfidentSpectra);
                    values.add(nDoubtfulSpectra);
                    values.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                    return values;

                case 6:
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
                    pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.getMatchValidationLevel().getIndex();
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

    @Override
    protected void catchException(Exception e) {
        setSelfUpdating(false);
        peptideShakerGUI.catchException(e);
    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, boolean interrupted) {
        ArrayList<String> tempKeys = new ArrayList<String>();
        for (int i : rows) {
            if (i < peptideKeys.size()) {
                String peptideKey = peptideKeys.get(i);
                tempKeys.add(peptideKey);
            }
        }
        try {
            loadPeptideObjects(tempKeys);

            for (String peptideKey : tempKeys) {
                if (interrupted) {
                    loadPeptideObjects(tempKeys);
                    return rows.get(0);
                }
                peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectraForPeptide(peptideKey);
                loadPeptideObjects(tempKeys);
            }
        } catch (Exception e) {
                catchException(e);
            return rows.get(0);
        }
        return rows.get(rows.size() - 1);
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
        try {
            if (column == 1
                    || column == 2
                    || column == 6
                    || column == 7) {
                identification.loadPeptideMatchParameters(peptideKeys, new PSParameter(), null);
            } else if (column == 3
                    || column == 4
                    || column == 5) {
                identification.loadPeptideMatches(peptideKeys, null);
            }
        } catch (Exception e) {
                catchException(e);
        }
    }
}
