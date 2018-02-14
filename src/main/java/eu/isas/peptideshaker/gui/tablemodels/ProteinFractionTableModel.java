package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.util.ArrayList;
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
     * The identification of this project.
     */
    private Identification identification;
    /**
     * A list of ordered protein keys.
     */
    private long[] proteinKeys = null;
    /**
     * A list of ordered file names.
     */
    private ArrayList<String> fileNames = new ArrayList<>();
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
     */
    public ProteinFractionTableModel() {
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI) {
        setUpTableModel(peptideShakerGUI);
    }

    /**
     * Set up the table model.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     */
    private void setUpTableModel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        fileNames = new ArrayList<>();

        if (identification != null) {
            try {
                if (peptideShakerGUI.getDisplayParameters().showValidatedProteinsOnly()) {
                    proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getValidatedProteins(peptideShakerGUI.getFilterParameters()); // show validated proteins only
                } else {
                    proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(null, peptideShakerGUI.getFilterParameters()); // show all proteins
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }

            for (String spectrumFileName : identification.getSpectrumFiles()) {
                fileNames.add(spectrumFileName);
            }
        }
    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteinKeys = null;
    }

    @Override
    public int getRowCount() {
        return proteinKeys == null ? 0 : proteinKeys.length;
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
            return fileNames.get(column - 3);
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

        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKeys[row]);
        if (column == 0) {
            return row + 1;
        } else if (column == 1) {
            return peptideShakerGUI.getDisplayFeaturesGenerator().getDatabaseLink(proteinMatch.getLeadingAccession());
        } else if (column == 2) {
            return peptideShakerGUI.getProteinDetailsProvider().getSimpleDescription(proteinMatch.getLeadingAccession());
        } else if (column > 2 && column - 3 < fileNames.size()) {
            String fraction = fileNames.get(column - 3);
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) proteinMatch.getUrParam(psParameter);
            if (psParameter.getFractionScore() != null && psParameter.getFractions().contains(fraction)) {
                return psParameter.getFractionConfidence(fraction);
            } else {
                return 0.0;
            }
        } else if (column == fileNames.size() + 3) {
            String mainMatch = proteinMatch.getLeadingAccession();
            ProteinUtils.computeMolecularWeight(peptideShakerGUI.getSequenceProvider().getSequence(mainMatch));
        } else if (column == fileNames.size() + 4) {
            PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
            return psParameter.getConfidence();
        } else if (column == fileNames.size() + 5) {
            PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
            psParameter = (PSParameter) proteinMatch.getUrParam(psParameter);
            return psParameter.getMatchValidationLevel().getIndex();
        }
        return "";
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
