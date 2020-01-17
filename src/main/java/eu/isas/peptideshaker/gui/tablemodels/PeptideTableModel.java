package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.data.StartIndexes;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;

/**
 * Table model for a set of peptide matches.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideTableModel extends SelfUpdatingTableModel {

    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The identification features generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The exception handler catches exceptions.
     */
    private final ExceptionHandler exceptionHandler;
    /**
     * A list of ordered peptide keys.
     */
    private long[] peptideKeys = null;
    /**
     * The main accession of the protein match to which the list of peptides is
     * attached.
     */
    private String proteinAccession;
    /**
     * Indicates whether the scores should be displayed instead of the
     * confidence
     */
    private boolean showScores = false;

    /**
     * Constructor for an empty table.
     */
    public PeptideTableModel() {

        this.identification = null;
        this.identificationFeaturesGenerator = null;
        this.displayFeaturesGenerator = null;
        this.peptideKeys = new long[0];
        this.proteinAccession = null;
        this.showScores = false;
        this.exceptionHandler = null;

    }

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object containing the matches
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param displayFeaturesGenerator the display features generator
     * @param proteinAccession the protein accession
     * @param peptideKeys the peptide keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     * @param exceptionHandler handler for the exceptions
     */
    public PeptideTableModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, DisplayFeaturesGenerator displayFeaturesGenerator,
            String proteinAccession, long[] peptideKeys, boolean displayScores, ExceptionHandler exceptionHandler) {

        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.peptideKeys = peptideKeys;
        this.proteinAccession = proteinAccession;
        this.showScores = displayScores;
        this.exceptionHandler = exceptionHandler;

    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param identification the identification object containing the matches
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param displayFeaturesGenerator the display features generator
     * @param proteinAccession the protein accession
     * @param peptideKeys the peptide keys
     */
    public void updateDataModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, DisplayFeaturesGenerator displayFeaturesGenerator,
            String proteinAccession, long[] peptideKeys) {

        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.peptideKeys = peptideKeys;
        this.proteinAccession = proteinAccession;

    }
    
    /**
     * Sets whether the scores should be displayed.
     *
     * @param showScores a boolean indicating whether the scores should be
     * displayed
     */
    public void showScores(boolean showScores) {
        this.showScores = showScores;
    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        peptideKeys = null;
    }

    @Override
    public int getRowCount() {

        return peptideKeys == null ? 0 : peptideKeys.length;

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
                return showScores ? "Score" : "Confidence";
            case 7:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        int viewIndex = getViewIndex(row);

        if (viewIndex < peptideKeys.length) {

            if (column == 0) {
                return viewIndex + 1;
            }

//            if (isScrolling) {
//                return null;
//            }
//
            if (!isSelfUpdating()) {
                dataMissingAtRow(row);
                return DisplayParameters.LOADING_MESSAGE;
            }
            
            long peptideKey = peptideKeys[viewIndex];
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

            switch (column) {
                case 1:
                    PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getStarred();

                case 2:
                    psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getProteinInferenceGroupClass();

                case 3:
                    return displayFeaturesGenerator.getTaggedPeptideSequence(peptideMatch, true, true, true);

                case 4:
                    int[] startIndexes = peptideMatch.getPeptide().getProteinMapping().get(proteinAccession);

                    return new StartIndexes(Arrays.stream(startIndexes)
                            .map(site -> site + 1)
                            .boxed()
                            .collect(Collectors.toCollection(ArrayList::new)));

                case 5:
                    double nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectraForPeptide(peptideKey);
                    double nDoubtfulSpectra = identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey) - nConfidentSpectra;
                    int nSpectra = peptideMatch.getSpectrumMatchesKeys().length;

                    ArrayList<Double> doubleValues = new ArrayList<>(3);
                    doubleValues.add(nConfidentSpectra);
                    doubleValues.add(nDoubtfulSpectra);
                    doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                    ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                    return arrrayListDataPoints;

                case 6:
                    psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return showScores ? psParameter.getTransformedScore() : psParameter.getConfidence();

                case 7:
                    psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getMatchValidationLevel().getIndex();

                default:
                    return null;
            }
        }

        return null;

    }

    /**
     * Indicates whether the table content was instantiated.
     *
     * @return a boolean indicating whether the table content was instantiated.
     */
    public boolean isInstantiated() {
        return identification != null;
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

    @Override
    protected void catchException(Exception e) {
        setSelfUpdating(false);
        exceptionHandler.catchException(e);
    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, WaitingHandler waitingHandler) {

        boolean canceled = rows.parallelStream()
                .map(i -> identification.getPeptideMatch(peptideKeys[i]))
                .map(peptideMatch -> identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideMatch.getKey()))
                .anyMatch(dummy -> waitingHandler.isRunCanceled());

        return canceled ? rows.get(0) : rows.get(rows.size() - 1);

    }
}
