package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
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
     * The identification features generator.
     */
    private IdentificationFeaturesGenerator featuresGenerator;
    /**
     * A list of ordered protein keys.
     */
    private ArrayList<String> proteinKeys = null;

    /**
     * Constructor which sets a new table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param progressDialog a progressdialog to display the progress to the
     * user
     */
    public ProteinTableModel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        if (identification != null) {
            featuresGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();
            proteinKeys = featuresGenerator.getSortedProteinKeys(null);
        }
    }
    
    public void reset() {
        proteinKeys = null;
    }

    /**
     * Constructor which sets a new empty table.
     *
     * @param peptideShakerGUI instance of the main GUI class
     * @param progressDialog a progressdialog to display the progress to the
     * user
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
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    String proteinKey = proteinKeys.get(row);
                    PSParameter pSParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                    return pSParameter.isStarred();
                case 2:
                    proteinKey = proteinKeys.get(row);
                    pSParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                    return pSParameter.getGroupClass();
                case 3:
                    proteinKey = proteinKeys.get(row);
                    ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                    return featuresGenerator.addDatabaseLink(proteinMatch.getMainMatch());
                case 4:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey);
                    String description = "";
                    try {
                        description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                    return description;
                case 5:
                    proteinKey = proteinKeys.get(row);
                    double sequenceCoverage = 100 * featuresGenerator.getSequenceCoverage(proteinKey);
                    double possibleCoverage = 100 * featuresGenerator.getObservableCoverage(proteinKey);
                    return new XYDataPoint(sequenceCoverage, possibleCoverage - sequenceCoverage, true);
                case 6:
                    proteinKey = proteinKeys.get(row);
                    int nValidatedPeptides = featuresGenerator.getNValidatedPeptides(proteinKey);
                    proteinMatch = identification.getProteinMatch(proteinKey);
                    return new XYDataPoint(nValidatedPeptides, proteinMatch.getPeptideCount() - nValidatedPeptides, false);
                case 7:
                    proteinKey = proteinKeys.get(row);
                    int nValidatedSpectra = featuresGenerator.getNValidatedSpectra(proteinKey);
                    int nSpectra = featuresGenerator.getNSpectra(proteinKey);
                    return new XYDataPoint(nValidatedSpectra, nSpectra - nValidatedSpectra, false);
                case 8:
                    proteinKey = proteinKeys.get(row);
                    return featuresGenerator.getSpectrumCounting(proteinKey);
                case 9:
                    proteinKey = proteinKeys.get(row);
                    proteinMatch = identification.getProteinMatch(proteinKey);
                    Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                    if (currentProtein != null) {
                        return currentProtein.computeMolecularWeight() / 1000;
                    } else {
                        return null;
                    }
                case 10:
                    if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                        proteinKey = proteinKeys.get(row);
                        pSParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return pSParameter.getProteinScore();
                    } else {
                        proteinKey = proteinKeys.get(row);
                        pSParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return pSParameter.getProteinConfidence();
                    }
                case 11:
                    proteinKey = proteinKeys.get(row);
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
        return (new Double(0.0)).getClass();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
