package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import com.compomics.util.experiment.identification.IdentificationFeaturesGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;

/**
 * Model for a the GO mappings protein table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinGoTableModel extends DefaultTableModel {

    /**
     * The identification of this project.
     */
    private final Identification identification;
    /**
     * The protein details provider.
     */
    private final ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The identification features generator.
     */
    private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator.
     */
    private final DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * Boolean indicating whether scores should be displayed.
     */
    private boolean showScores = true;
    /**
     * A list of the protein keys.
     */
    private ArrayList<Long> proteinGroupKeys = null;

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object
     * @param proteinDetailsProvider the protein details provider
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param displayFeaturesGenerator the display features generator
     * @param proteinGroupKeys the keys of the protein groups to display
     * @param showScores boolean indicating whether scores should be displayed
     */
    public ProteinGoTableModel(Identification identification, ProteinDetailsProvider proteinDetailsProvider, IdentificationFeaturesGenerator identificationFeaturesGenerator, DisplayFeaturesGenerator displayFeaturesGenerator, ArrayList<Long> proteinGroupKeys, boolean showScores) {

        this.identification = identification;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.proteinGroupKeys = proteinGroupKeys;
        this.showScores = showScores;
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param proteinGroupKeys the keys of the protein groups to display
     */
    public void updateDataModel(ArrayList<Long> proteinGroupKeys) {

        this.proteinGroupKeys = proteinGroupKeys;

    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteinGroupKeys = null;
    }

    @Override
    public int getRowCount() {

        return proteinGroupKeys == null ? 0 : proteinGroupKeys.size();

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
                return showScores ? "Score" : "Confidence";
            case 8:
                return "  ";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        long proteinGroupKey = proteinGroupKeys.get(row);
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinGroupKey);

        switch (column) {

            case 0:
                return row + 1;

            case 1:
                String mainMatch = proteinMatch.getLeadingAccession();
                return displayFeaturesGenerator.getDatabaseLink(mainMatch);

            case 2:
                return proteinDetailsProvider.getSimpleDescription(proteinMatch.getLeadingAccession());

            case 3:
                HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinGroupKey);
                Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                Double sequenceCoverageNotValidated = 100 * sequenceCoverage.get(MatchValidationLevel.not_validated.getIndex()); //@TODO: this does not seem to be used?
                double possibleCoverage = 100.0 * identificationFeaturesGenerator.getObservableCoverage(proteinGroupKey);
                ArrayList<Double> doubleValues = new ArrayList<>(4);
                doubleValues.add(sequenceCoverageConfident);
                doubleValues.add(sequenceCoverageDoubtful);
                doubleValues.add(sequenceCoverageNotValidated);
                doubleValues.add(possibleCoverage - sequenceCoverageConfident - sequenceCoverageDoubtful - sequenceCoverageNotValidated);
                ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumExceptLastNumber);
                return arrrayListDataPoints;

            case 4:
                double nConfidentPeptides = identificationFeaturesGenerator.getNConfidentPeptides(proteinGroupKey);
                double nDoubtfulPeptides = identificationFeaturesGenerator.getNValidatedPeptides(proteinGroupKey) - nConfidentPeptides;

                doubleValues = new ArrayList<>(3);
                doubleValues.add(nConfidentPeptides);
                doubleValues.add(nDoubtfulPeptides);
                doubleValues.add(proteinMatch.getPeptideCount() - nConfidentPeptides - nDoubtfulPeptides);
                arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                return arrrayListDataPoints;

            case 5:
                double nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectra(proteinGroupKey);
                double nDoubtfulSpectra = identificationFeaturesGenerator.getNValidatedSpectra(proteinGroupKey) - nConfidentSpectra;
                int nSpectra = identificationFeaturesGenerator.getNSpectra(proteinGroupKey);

                doubleValues = new ArrayList<>(3);
                doubleValues.add(nConfidentSpectra);
                doubleValues.add(nDoubtfulSpectra);
                doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                return arrrayListDataPoints;

            case 6:
                return identificationFeaturesGenerator.getSpectrumCounting(proteinGroupKey);

            case 7:
                PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                return showScores ? psParameter.getScore() : psParameter.getConfidence();

            case 8:
                psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                return psParameter.getMatchValidationLevel().getIndex();

            default:
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
     * Returns the list of protein groups in the model.
     *
     * @return the list of protein groups in the model
     */
    public ArrayList<Long> getProteins() {
        return proteinGroupKeys;
    }
}
