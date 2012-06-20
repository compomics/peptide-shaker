package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.table.DefaultTableModel;

/**
 * This table model displays the protein confidence in every fraction.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinFractionTableModel extends DefaultTableModel {

    /**
     * The main GUI class.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * A list of ordered protein keys.
     */
    private ArrayList<String> proteinKeys = null;
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
     */
    public ProteinFractionTableModel(PeptideShakerGUI peptideShakerGUI) {
        setUpTableModel(peptideShakerGUI);
        modelInitiated = true;
    }

    /**
     * Constructor which sets a new empty table.
     *
     */
    public ProteinFractionTableModel() {
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
            proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(null);
        }
        
        fileNames = new ArrayList<String>();
        
        for (String filePath : peptideShakerGUI.getSearchParameters().getSpectrumFiles()) {
            String fileName = Util.getFileName(filePath);
            fileNames.add(fileName);
            fractionNames.put(fileName, fileName);
        }
        
        Collections.sort(fileNames);
    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteinKeys = null;
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
        return fileNames.size() + 6;
    }
    
    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return " ";
        } else if (column == 1) {
            return "Accession";
        } else if (column == 2) {
            return "Description";
        } else if (column > 2 && column - 3 < fileNames.size()) {
            return fractionNames.get(fileNames.get(column - 3));
        } else if (column == fileNames.size() + 3) {
            return "MW";
        } else if (column == fileNames.size() + 4) {
            return "Confidence";
        } else if (column == fileNames.size() + 5) {
            return "  ";
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
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKeys.get(row));
                return peptideShakerGUI.getIdentificationFeaturesGenerator().addDatabaseLink(proteinMatch.getMainMatch());
            } else if (column == 2) {
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKeys.get(row));
                String description = "";
                try {
                    description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
                return description;
            } else if (column > 2 && column - 3 < fileNames.size()) {
                
                if (peptideShakerGUI.getProteinFractionsPanel().isProteinConfidenceSelected()) {
                    
                    String fraction = fileNames.get(column - 3);
                    PSParameter pSParameter = new PSParameter();
                    String proteinKey = proteinKeys.get(row);
                    pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, pSParameter);
                    if (pSParameter.getFractions() != null && pSParameter.getFractions().contains(fraction)) {
                        return pSParameter.getFractionConfidence(fraction);
                    } else {
                        return null;
                    }
                    
                } else if (peptideShakerGUI.getProteinFractionsPanel().isProteinPeptideCountSelected()) {

                    String fraction = fractionNames.get(fileNames.get(column - 3));
                    int validatedPeptideCounter = 0;
                    PSParameter pSParameter = new PSParameter();
                    String proteinKey = proteinKeys.get(row);
                    ArrayList<String> peptideKeys = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey).getPeptideMatches();
                    
                    for (int j = 0; j < peptideKeys.size(); j++) {
                        
                        String peptideKey = peptideKeys.get(j);
                        pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideKey, pSParameter);
                        
                        if (pSParameter.getFractions() != null && pSParameter.getFractions().contains(fraction)) {
                            if (pSParameter.isValidated()) {
                                validatedPeptideCounter++;
                            }
                        }
                    }
                    
                    return validatedPeptideCounter;
                    
                } else if (peptideShakerGUI.getProteinFractionsPanel().isProteinSpectumCountSelected()) {

                    // get the psms per fraction
                    HashMap<String, ArrayList<String>> fractionPsmMatches = peptideShakerGUI.getMetrics().getFractionPsmMatches();
                    
                    String fraction = fractionNames.get(fileNames.get(column - 3));
                    int validatedSpectraCounter = 0;
                    PSParameter pSParameter = new PSParameter();
                    
                    String proteinKey = proteinKeys.get(row);
                    ArrayList<String> peptideKeys = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey).getPeptideMatches();
                    
                    for (int j = 0; j < peptideKeys.size(); j++) {
                        
                        String currentPeptideKey = peptideKeys.get(j);
                        
                        if (fractionPsmMatches.get(fraction + "_" + currentPeptideKey) != null) {
                            ArrayList<String> spectrumKeys = fractionPsmMatches.get(fraction + "_" + currentPeptideKey);

                            for (int k = 0; k < spectrumKeys.size(); k++) {
                                
                                pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKeys.get(k), new PSParameter());
                                
                                if (pSParameter.isValidated()) {
                                    validatedSpectraCounter++;
                                }
                            }
                        }
                    }
                    
                    return validatedSpectraCounter;
                    
                } else { // intensity selected
                    
                    // get the psms per fraction
                    HashMap<String, ArrayList<String>> fractionPsmMatches = peptideShakerGUI.getMetrics().getFractionPsmMatches();
                    
                    String fraction = fractionNames.get(fileNames.get(column - 3));
                    double intensitySum = 0.0;
                    PSParameter pSParameter = new PSParameter();
                    
                    String proteinKey = proteinKeys.get(row);
                    ArrayList<String> peptideKeys = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey).getPeptideMatches();
                    
                    for (int j = 0; j < peptideKeys.size(); j++) {
                        
                        String currentPeptideKey = peptideKeys.get(j);
                        
                        if (fractionPsmMatches.get(fraction + "_" + currentPeptideKey) != null) {
                            ArrayList<String> spectrumKeys = fractionPsmMatches.get(fraction + "_" + currentPeptideKey);

                            for (int k = 0; k < spectrumKeys.size(); k++) {
                                
                                pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKeys.get(k), new PSParameter());
                                
                                if (pSParameter.isValidated()) {    
                                    intensitySum += spectrumFactory.getPrecursor(spectrumKeys.get(k)).getIntensity();
                                }
                            }
                        }
                    }
                    
                    return intensitySum;
                }
                
            } else if (column == fileNames.size() + 3) {
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKeys.get(row));
                Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                if (currentProtein != null) {
                    return currentProtein.computeMolecularWeight() / 1000;
                } else {
                    return null;
                }
            } else if (column == fileNames.size() + 4) {
                String proteinKey = proteinKeys.get(row);
                PSParameter pSParameter = new PSParameter();
                pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, pSParameter);
                return pSParameter.getProteinConfidence();
            } else if (column == fileNames.size() + 5) {
                String proteinKey = proteinKeys.get(row);
                PSParameter pSParameter = new PSParameter();
                pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, pSParameter);
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
