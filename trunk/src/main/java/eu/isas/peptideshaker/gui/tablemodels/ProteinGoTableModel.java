package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.data.XYDataPoint;

/**
 * Model for a the GO mappings protein table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinGoTableModel extends DefaultTableModel {

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
     * A list of the protein keys.
     */
    private ArrayList<String> proteins = null;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param proteins  
     */
    public ProteinGoTableModel(PeptideShakerGUI peptideShakerGUI, HashSet<String> proteins) {
        setUpTableModel(peptideShakerGUI, proteins);
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI
     * @param proteins  
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI, HashSet<String> proteins) {
        setUpTableModel(peptideShakerGUI, proteins);
    }

    /**
     * Set up the table model.
     *
     * @param peptideShakerGUI
     */
    private void setUpTableModel(PeptideShakerGUI peptideShakerGUI, HashSet<String> proteins) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        if (identification != null) {
            this.proteins = new ArrayList<String>(proteins);
        }
    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteins = null;
    }

    @Override
    public int getRowCount() {
        if (proteins != null) {
            return proteins.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 9;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return " ";
            case 1:
                return "Accession";
            case 2:
                return "Description";
            case 3:
                return "Coverage";
            case 4:
                return "#Peptides";
            case 5:
                return "#Spectra";
            case 6:
                return "MS2 Quant.";
            case 7:
                if (peptideShakerGUI != null && peptideShakerGUI.getDisplayPreferences().showScores()) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 8:
                return "  ";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            String proteinKey = proteins.get(row);
            
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().addDatabaseLink(proteinKey);
                case 2:
                    ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                    String description = "";
                    try {
                        description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                    return description;
                case 3:
                    double sequenceCoverage = 100 * peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                    double possibleCoverage = 100 * peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey);
                    return new XYDataPoint(sequenceCoverage, possibleCoverage - sequenceCoverage, true);
                case 4:
                    int nValidatedPeptides = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey);
                    proteinMatch = identification.getProteinMatch(proteinKey);
                    return new XYDataPoint(nValidatedPeptides, proteinMatch.getPeptideCount() - nValidatedPeptides, false);
                case 5:
                    int nValidatedSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey);
                    int nSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNSpectra(proteinKey);
                    return new XYDataPoint(nValidatedSpectra, nSpectra - nValidatedSpectra, false);
                case 6:
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey);
                case 7:
                    PSParameter pSParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                    if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                        return pSParameter.getProteinScore();
                    } else {
                        return pSParameter.getProteinConfidence();
                    }
                case 8:
                    pSParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
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
}
