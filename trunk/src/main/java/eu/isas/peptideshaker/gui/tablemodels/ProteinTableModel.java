package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.data.XYDataPoint;

/**
 * Model for a the protein table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinTableModel extends DefaultTableModel {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The main GUI class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * A list of ordered protein keys.
     */
    private ArrayList<String> proteinKeys = null;
    /**
     * Indicates that some data is missing
     */
    private boolean dataMissing = false;
    /**
     * Indicates whether data in DB shall be used
     */
    private boolean useDB = false;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     */
    public ProteinTableModel(PeptideShakerGUI peptideShakerGUI) {
        setUpTableModel(peptideShakerGUI);
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI) {
        setUpTableModel(peptideShakerGUI);
    }

    /**
     * Set up the table model.
     *
     * @param peptideShakerGUI
     */
    private void setUpTableModel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        if (identification != null) {
            try {
                if (peptideShakerGUI.getDisplayPreferences().showValidatedProteinsOnly()) {
                    proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getValidatedProteins(); // show validated proteins only
                } else {
                    proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(null, peptideShakerGUI.getFilterPreferences()); // show all proteins
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteinKeys = null;
    }

    /**
     * Constructor which sets a new empty table.
     *
     */
    public ProteinTableModel() {
    }

    @Override
    public int getRowCount() {
        if (proteinKeys != null) {
            return proteinKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 12;
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
                return "Accession";
            case 4:
                return "Description";
            case 5:
                return "Coverage";
            case 6:
                return "#Peptides";
            case 7:
                return "#Spectra";
            case 8:
                return "MS2 Quant.";
            case 9:
                return "MW";
            case 10:
                if (peptideShakerGUI != null && peptideShakerGUI.getDisplayPreferences().showScores()) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 11:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            int rowCachingSize = 30;

            // preload data if the row interval has changed
//            if (startPosition == -1 || (row < startPosition || row > startPosition + rowCachingSize)) {
//
//                if (row != 0 && column != 4) {
//
//                    int max = Math.min(getRowCount() - 1, startPosition + rowCachingSize);
//
//                    startPosition = row;
//                    endPosition = max;
//
//                    System.out.println("preload data: " + startPosition + " - " + endPosition);
//
//                    ArrayList<String> tempProteinKeys = new ArrayList<String>(rowCachingSize);
//
//                    for (int i = row; i < max; i++) {
//                        tempProteinKeys.add(proteinKeys.get(i));
//                    }
//
//                    // preload the protein matches
//                    identification.loadProteinMatches(tempProteinKeys, null);
//                    
//                    System.out.println("protein matches loaded " + tempProteinKeys.size());
//
//                    // preload the protein match parameters
//                    PSParameter pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKeys.get(row), new PSParameter());
//                    identification.loadProteinMatchParameters(tempProteinKeys, pSParameter, null);
//                    
//                    System.out.println("protein match paramaters loaded " + tempProteinKeys.size());
//
//                    // preload the peptide matches and peptide match parameters
//                    ArrayList<String> tempPeptideKeys = new ArrayList<String>();
//
//                    for (String currentProteinKey : tempProteinKeys) {
//                        ArrayList<String> tempPeptideMatches = identification.getProteinMatch(currentProteinKey).getPeptideMatches();
//
//                        for (String currentPeptideMatch : tempPeptideMatches) {
//                            if (!tempPeptideKeys.contains(currentPeptideMatch)) {
//                                tempPeptideKeys.add(currentPeptideMatch);
//                            }
//                        }
//                    }
//
//                    PSParameter peptidePSParameter = new PSParameter();
//                    identification.loadPeptideMatches(tempPeptideKeys, null);
//                    
//                    System.out.println("peptide matches loaded " + tempPeptideKeys.size());
//                    
//                    identification.loadPeptideMatchParameters(tempPeptideKeys, peptidePSParameter, null);
//                    
//                    System.out.println("peptide match parameters loaded " + tempPeptideKeys.size());
//
//                    // preload the spectrum matches and spectrum match parameters
//                    ArrayList<String> tempSpectrumKeys = new ArrayList<String>();
//
//                    for (String currentPepideKey : tempPeptideKeys) {
//
//                        ArrayList<String> tempSpectrumMatches = identification.getPeptideMatch(currentPepideKey).getSpectrumMatches();
//
//                        for (String currentSpectrumMatch : tempSpectrumMatches) {
//                            if (!tempSpectrumKeys.contains(currentSpectrumMatch)) {
//                                tempSpectrumKeys.add(currentSpectrumMatch);
//                            }
//                        }
//                    }
//
//                    PSParameter spectrumPSParameter = new PSParameter();
//                    identification.loadSpectrumMatches(tempSpectrumKeys, null);
//                    
//                    System.out.println("spectrum matches loaded " + tempSpectrumKeys.size());
//                    
//                    identification.loadSpectrumMatchParameters(tempSpectrumKeys, spectrumPSParameter, null);
//                    
//                     System.out.println("spectrum match parameters loaded " + tempSpectrumKeys.size());
//
//                    startPosition = row;
//                    
//                    System.out.println("done preload data: " + startPosition + " - " + endPosition);
//                }
//            }

            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    String proteinKey = proteinKeys.get(row);
                    PSParameter pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.isStarred();
                case 2:
                    proteinKey = proteinKeys.get(row);
                    pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return pSParameter.getProteinInferenceClass();
                case 3:
                    proteinKey = proteinKeys.get(row);
                    ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB && proteinMatch == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return peptideShakerGUI.getDisplayFeaturesGenerator().addDatabaseLink(proteinMatch.getMainMatch());
                case 4:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB && proteinMatch == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    String description = "";
                    try {
                        description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                    return description;
                case 5:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB 
                            && !peptideShakerGUI.getIdentificationFeaturesGenerator().sequenceCoverageInCache(proteinKey)
                            && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    double sequenceCoverage;
                    try {
                        sequenceCoverage = 100 * peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return Double.NaN;
                    }
                    double possibleCoverage = 100;
                    try {
                        possibleCoverage = 100 * peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                    return new XYDataPoint(sequenceCoverage, possibleCoverage - sequenceCoverage, true);
                case 6:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB 
                            && !peptideShakerGUI.getIdentificationFeaturesGenerator().nValidatedPeptidesInCache(proteinKey)
                            && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    int nValidatedPeptides = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey);
                    return new XYDataPoint(nValidatedPeptides, proteinMatch.getPeptideCount() - nValidatedPeptides, false);
                case 7:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB 
                            && (!peptideShakerGUI.getIdentificationFeaturesGenerator().nValidatedSpectraInCache(proteinKey))
                            && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    int nValidatedSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey);
                    int nSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNSpectra(proteinKey);
                    return new XYDataPoint(nValidatedSpectra, nSpectra - nValidatedSpectra, false);
                case 8:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB 
                            && !peptideShakerGUI.getIdentificationFeaturesGenerator().spectrumCountingInCache(proteinKey)
                            && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey);
                case 9:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                    if (!useDB && proteinMatch == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    String mainMatch = proteinMatch.getMainMatch();
                    Protein currentProtein = sequenceFactory.getProtein(mainMatch);
                    if (currentProtein != null) {
                        return sequenceFactory.computeMolecularWeight(mainMatch);
                    } else {
                        return null;
                    }
                case 10:
                    proteinKey = proteinKeys.get(row);
                    pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    if (pSParameter != null) {
                        if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                            return pSParameter.getProteinScore();
                        } else {
                            return pSParameter.getProteinConfidence();
                        }
                    } else {
                        return null;
                    }
                case 11:
                    proteinKey = proteinKeys.get(row);
                    pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                    if (!useDB && pSParameter == null) {
                        dataMissing = true;
                        return DisplayPreferences.LOADING_MESSAGE;
                    }
                    if (pSParameter != null) {
                        return pSParameter.isValidated();
                    } else {
                        return null;
                    }
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
     * Resets whether data is missing
     * @param dataMissing 
     */
    public void setDataMissing(boolean dataMissing) {
        this.dataMissing = dataMissing;
    }
    
    /**
     * indicates whether data is missing
     * @return 
     */
    public boolean isDataMissing() {
        return dataMissing;
    }
    
    /**
     * Sets whether or not data shall be looked for in the database. If false only the cache will be used
     * @param useDB 
     */
    public void useDB(boolean useDB) {
        this.useDB = useDB;
    }
    
    /**
     * Indicates whether the table model uses the DB
     * @return 
     */
    public boolean isDB() {
        return useDB;
    }
}
