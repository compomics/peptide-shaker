package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.table.DefaultTableModel;

/**
 * This table model shows a fraction view of the peptides given in the
 * constructor
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideFractionTableModel extends DefaultTableModel {

    /**
     * The main GUI class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * A list of ordered peptide keys.
     */
    private ArrayList<String> peptideKeys = null;
    /**
     * A list of ordered file names.
     */
    private ArrayList<String> fileNames = new ArrayList<String>();
    /**
     * A map of all fraction names.
     */
    private HashMap<String, String> fractionNames = new HashMap<String, String>();
    /**
     * Set to true as soon as the real model is initiated. False means that only 
     * the dummy constructor has been used.
     */
    private boolean modelInitiated = false;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param peptideKeys the peptide keys
     */
    public PeptideFractionTableModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> peptideKeys) {
        setUpTableModel(peptideShakerGUI, peptideKeys);
        modelInitiated = true;
    }
    
    /**
     * Constructor which sets a new empty table.
     *
     */
    public PeptideFractionTableModel() {
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI
     * @param peptideKeys
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> peptideKeys) {
        setUpTableModel(peptideShakerGUI, peptideKeys);
    }

    /**
     * Set up the table model.
     *
     * @param peptideShakerGUI
     * @param peptideKeys
     */
    private void setUpTableModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> peptideKeys) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        this.peptideKeys = peptideKeys;

        fileNames = new ArrayList<String>();
        
        for (String filePath : peptideShakerGUI.getSearchParameters().getSpectrumFiles()) {
            String fileName = Util.getFileName(filePath);
            fileNames.add(fileName);
            fractionNames.put(fileName, fileName);
        }
        
        Collections.sort(fileNames);
    }

    /**
     * Reset the peptide keys.
     */
    public void reset() {
        peptideKeys = null;
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
        return fileNames.size() + 4;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return " ";
        } else if (column == 1) {
            return "Sequence";
        } else if (column > 1 && column - 2 < fileNames.size()) {
            return fractionNames.get(fileNames.get(column - 2));
        } else if (column == fileNames.size() + 2) {
            return "Confidence";
        } else if (column == fileNames.size() + 3) {
            return "";
        } else {
            return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            if (column == 0) {
                return row + 1;
            } else if (column == 1) {
                String peptideKey = peptideKeys.get(row);
                return peptideShakerGUI.getIdentificationFeaturesGenerator().getColoredPeptideSequence(peptideKey, true);
            } else if (column > 1 && column - 2 < fileNames.size()) {
                String fraction = fileNames.get(column - 2);
                PSParameter pSParameter = new PSParameter();
                String peptideKey = peptideKeys.get(row);
                pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
                if (pSParameter.getFractions() != null && pSParameter.getFractions().contains(fraction)) {
                    return pSParameter.getFractionConfidence(fraction);
                } else {
                    return 0.0;
                }
            } else if (column == fileNames.size() + 2) {
                String peptideKey = peptideKeys.get(row);
                PSParameter pSParameter = new PSParameter();
                pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
                return pSParameter.getPeptideConfidence();
            } else if (column == fileNames.size() + 3) {
                String peptideKey = peptideKeys.get(row);
                PSParameter pSParameter = new PSParameter();
                pSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, pSParameter);
                return pSParameter.isValidated();
            } else {
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
     * Returns true if the real model has been iniitated.
     * 
     * @return the modelInitiated
     */
    public boolean isModelInitiated() {
        return modelInitiated;
    }

    /**
     * Set if the real model has been initiated.
     * 
     * @param modelInitiated the modelInitiated to set
     */
    public void setModelInitiated(boolean modelInitiated) {
        this.modelInitiated = modelInitiated;
    }
}
