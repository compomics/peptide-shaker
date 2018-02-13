package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.util.ArrayList;
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
     * The identification of this project.
     */
    private final Identification identification;
    /**
     * The display features generator.
     */
    private final DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * A list of ordered peptide keys.
     */
    private ArrayList<Long> peptideKeys = null;
    /**
     * A list of ordered file names.
     */
    private ArrayList<String> fileNames = new ArrayList<>();

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object
     * @param displayFeaturesGenerator the display features generator
     * @param peptideKeys the peptide keys
     * @param fileNames a list of ordered file names
     */
    public PeptideFractionTableModel(Identification identification, DisplayFeaturesGenerator displayFeaturesGenerator, ArrayList<Long> peptideKeys, ArrayList<String> fileNames) {

        this.identification = identification;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.peptideKeys = peptideKeys;
        this.fileNames = fileNames;

    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideKeys the peptide keys
     */
    public void updateDataModel(ArrayList<Long> peptideKeys) {

        this.peptideKeys = peptideKeys;

    }

    /**
     * Reset the peptide keys.
     */
    public void reset() {
        peptideKeys = null;
    }

    @Override
    public int getRowCount() {

        return peptideKeys == null ? 0 : peptideKeys.size();

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

            return fileNames.get(column - 2);

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

        long peptideKey = peptideKeys.get(row);
        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

        if (column == 0) {

            return row + 1;

        } else if (column == 1) {

            return displayFeaturesGenerator.getTaggedPeptideSequence(peptideMatch, true, true, true);

        } else if (column > 1) {
            
                PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

            if (column - 2 < fileNames.size()) {

                String fraction = fileNames.get(column - 2);
                
                if (psParameter.getFractionScore() != null) {
                    
                    Double confidence = psParameter.getFractionConfidence(fraction);
                    
                    if (confidence != null) {
                        
                        return confidence;
                        
                    }
                }

                return  0.0;
                
            } else if (column == fileNames.size() + 2) {

                return psParameter.getConfidence();

            } else if (column == fileNames.size() + 3) {

                return psParameter.getMatchValidationLevel().getIndex();

            }
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
}
