package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.data.XYDataPoint;

/**
 * Table model for a set of peptide matches
 *
 * @author marc
 */
public class PeptideTableModel extends DefaultTableModel {
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
     * The main accession of the protein match to which the list of peptides is attached
     */
    private String proteinAccession;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     */
    public PeptideTableModel(PeptideShakerGUI peptideShakerGUI, String proteinAccession, ArrayList<String> peptideKeys) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        this.peptideKeys = peptideKeys;
        this.proteinAccession = proteinAccession;
    }
    
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
                    PSParameter pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                    return pSParameter.isStarred();
                case 2:
                    peptideKey = peptideKeys.get(row);
                    pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                    return pSParameter.getGroupClass();
                case 3:
                    peptideKey = peptideKeys.get(row);
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().getColoredPeptideSequence(peptideKey, true);
                case 4:
                    peptideKey = peptideKeys.get(row);
                    String report = "";
                    ArrayList<Integer> indexes;
                    try {
                    indexes = peptideShakerGUI.getIdentificationFeaturesGenerator().getPeptideStart(proteinAccession, Peptide.getSequence(peptideKey));
                    }catch (IOException e) {
                        peptideShakerGUI.catchException(e);
                        return "IO Exception";
                    }
                    boolean first = true;
                    for (int index : indexes) {
                        if (first) {
                            first = false;
                        } else {
                            report += ", ";
                        }
                        report += index;
                    }
                    return report;
                case 5:
                    int nValidated = 0;
                    peptideKey = peptideKeys.get(row);
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                    pSParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                    if (pSParameter.isValidated()) {
                        nValidated++;
                    }
                    }
                    int nSpectra = peptideMatch.getSpectrumMatches().size();
                    return new XYDataPoint(nValidated, nSpectra - nValidated, false);
                case 6:
                        peptideKey = peptideKeys.get(row);
                        pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                    if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                        return pSParameter.getProteinScore();
                    } else {
                        return pSParameter.getProteinConfidence();
                    }
                case 7:
                        peptideKey = peptideKeys.get(row);
                        pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
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
