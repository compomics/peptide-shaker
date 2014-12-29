package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.data.StartIndexes;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;

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
     * @param proteinAccession the protein accession
     * @param peptideKeys the peptide keys
     *
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IllegalArgumentException thrown if an IllegalArgumentException
     * occurs
     * @throws SQLException thrown if an SQLException occurs
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
     * @param peptideShakerGUI instance of the main GUI class
     * @param proteinAccession the protein accession
     * @param peptideKeys the peptide keys
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
                    PSParameter psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
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
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
                    if (psParameter == null) {
                        if (isScrolling()) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                    }
                    return psParameter.getProteinInferenceClass();
                case 3:
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey, useDB && !isScrolling);
                    if (peptideMatch == null) {
                        if (isScrolling()) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return Peptide.getSequence(peptideKey);
                        }
                    }
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideMatch, true, true, true);
                case 4:
                    if (isScrolling) {
                        return null;
                    }
                    ArrayList<Integer> indexes;
                    if (sequenceFactory == null) {
                        return null;
                    }
                    try {
                        Protein currentProtein = sequenceFactory.getProtein(proteinAccession);
                        String peptideSequence = Peptide.getSequence(peptideKey);
                        indexes = currentProtein.getPeptideStart(peptideSequence,
                                peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                    } catch (IOException e) {
                        peptideShakerGUI.catchException(e);
                        return "IO Exception";
                    }
                    Collections.sort(indexes);
                    return new StartIndexes(indexes); // note: have to be "packed" like this in order to be able to resetSorting on the first index if multiple indexes
                case 5:
                    if (isScrolling) {
                        return null;
                    }
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

                    ArrayList<Double> doubleValues = new ArrayList<Double>();
                    doubleValues.add(nConfidentSpectra);
                    doubleValues.add(nDoubtfulSpectra);
                    doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                    ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                    return arrrayListDataPoints;
                case 6:
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
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
                            return psParameter.getPeptideScore();
                        } else {
                            return psParameter.getPeptideConfidence();
                        }
                    } else {
                        return null;
                    }
                case 7:
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
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
        } catch (SQLNonTransientConnectionException e) {
            // connection has been closed
            return rows.get(0); // @TODO: is this the correct thing to return in this case..? 
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
