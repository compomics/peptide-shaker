package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.data.ArrrayListDataPoints;
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
    public ProteinGoTableModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> proteins) {
        setUpTableModel(peptideShakerGUI, proteins);
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param peptideShakerGUI
     * @param proteins
     */
    public void updateDataModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> proteins) {
        setUpTableModel(peptideShakerGUI, proteins);
    }

    /**
     * Set up the table model.
     *
     * @param peptideShakerGUI
     */
    private void setUpTableModel(PeptideShakerGUI peptideShakerGUI, ArrayList<String> proteins) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        if (identification != null) {
            this.proteins = proteins;
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

        if (!proteins.isEmpty()) {

            try {
                String proteinKey = proteins.get(row);

                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                        String mainMatch = proteinMatch.getMainMatch();
                        return peptideShakerGUI.getDisplayFeaturesGenerator().addDatabaseLink(mainMatch);
                    case 2:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        String description = "";
                        try {
                            description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription();
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                        }
                        return description;
                    case 3:
                        double sequenceCoverage;
                        try {
                            sequenceCoverage = 100 * peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey, PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
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
                    case 4:
                        try {
                            proteinMatch = identification.getProteinMatch(proteinKey);
                            double nConfidentPeptides = peptideShakerGUI.getIdentificationFeaturesGenerator().getNConfidentPeptides(proteinKey);
                            double nDoubtfulPeptides = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey) - nConfidentPeptides;

                            ArrayList<Double> doubleValues = new ArrayList<Double>();
                            doubleValues.add(nConfidentPeptides);
                            doubleValues.add(nDoubtfulPeptides);
                            doubleValues.add(proteinMatch.getPeptideCount() - nConfidentPeptides - nDoubtfulPeptides);
                            ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues);
                            return arrrayListDataPoints;
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                            return Double.NaN;
                        }
                    case 5:
                        try {
                            double nConfidentSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNConfidentSpectra(proteinKey);
                            double nDoubtfulSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey) - nConfidentSpectra;
                            int nSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getNSpectra(proteinKey);

                            ArrayList<Double> doubleValues = new ArrayList<Double>();
                            doubleValues.add(nConfidentSpectra);
                            doubleValues.add(nDoubtfulSpectra);
                            doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                            ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues);
                            return arrrayListDataPoints;
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                            return Double.NaN;
                        }
                    case 6:
                        try {
                            return peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey);
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                            return Double.NaN;
                        }
                    case 7:
                        PSParameter pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter());
                        if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                            return pSParameter.getProteinScore();
                        } else {
                            return pSParameter.getProteinConfidence();
                        }
                    case 8:
                        pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter());
                        return pSParameter.getMatchValidationLevel().getIndex();
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                return "";
            }
        } else {
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
}
