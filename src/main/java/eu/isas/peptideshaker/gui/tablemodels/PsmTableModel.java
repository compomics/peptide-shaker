package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.util.ArrayList;

/**
 * Table model for a set of peptide to spectrum matches.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsmTableModel extends SelfUpdatingTableModel {

    /**
     * The identification of this project.
     */
    private final Identification identification;
    /**
     * The display features generator.
     */
    private final DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The ID input map.
     */
    private final InputMap inputMap;
    /**
     * The exception handler catches exceptions.
     */
    private final ExceptionHandler exceptionHandler;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * A list of ordered PSM keys.
     */
    private long[] psmKeys = null;
    /**
     * Indicates whether the scores should be displayed instead of the
     * confidence
     */
    private boolean showScores = false;

    /**
     * Constructor for an empty table.
     */
    public PsmTableModel() {

        this.identification = null;
        this.displayFeaturesGenerator = null;
        this.identificationParameters = null;
        this.psmKeys = new long[0];
        this.showScores = true;
        this.exceptionHandler = null;
        this.inputMap = null;

    }

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object containing the matches
     * @param displayFeaturesGenerator the display features generator
     * @param identificationParameters the identification parameters
     * @param psmKeys the PSM keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     * @param exceptionHandler handler for the exceptions
     */
    public PsmTableModel(Identification identification, DisplayFeaturesGenerator displayFeaturesGenerator, IdentificationParameters identificationParameters,
            long[] psmKeys, boolean displayScores, ExceptionHandler exceptionHandler) {

        this.identification = identification;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.identificationParameters = identificationParameters;
        this.psmKeys = psmKeys;
        this.showScores = displayScores;
        this.exceptionHandler = exceptionHandler;

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) identification.getUrParam(pSMaps);
        this.inputMap = pSMaps.getInputMap();

    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param psmKeys the PSM keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     */
    public void updateDataModel(long[] psmKeys, boolean displayScores) {

        this.psmKeys = psmKeys;
        this.showScores = displayScores;

    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        psmKeys = null;
    }

    @Override
    public int getRowCount() {

        return psmKeys == null ? 0 : psmKeys.length;

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
                return "ID";
            case 3:
                return "Sequence";
            case 4:
                return "Charge";
            case 5:
                return "m/z Error";
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

        if (viewIndex < psmKeys.length) {

            if (column == 0) {
                return viewIndex + 1;
            }

            if (isScrolling) {
                return null;
            }

            if (!isSelfUpdating()) {
                dataMissingAtRow(row);
                return DisplayParameters.LOADING_MESSAGE;
            }

            long psmKey = psmKeys[viewIndex];
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);

            switch (column) {
                case 1:
                    PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getStarred();

                case 2:
                    return SpectrumIdentificationPanel.isBestPsmEqualForAllIdSoftware(spectrumMatch, identificationParameters.getSequenceMatchingParameters(), inputMap.getInputAlgorithmsSorted().size());

                case 3:
                    return displayFeaturesGenerator.getTaggedPeptideSequence(spectrumMatch, true, true, true);

                case 4:
                    if (spectrumMatch.getBestPeptideAssumption() != null) {

                        return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();

                    } else if (spectrumMatch.getBestTagAssumption() != null) {

                        return spectrumMatch.getBestTagAssumption().getIdentificationCharge();

                    } else {

                        throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");

                    }

                case 5:
                    String spectrumKey = spectrumMatch.getSpectrumKey();
                    Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                    SearchParameters searchParameters = identificationParameters.getSearchParameters();

                    if (spectrumMatch.getBestPeptideAssumption() != null) {

                        return Math.abs(spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));

                    } else if (spectrumMatch.getBestTagAssumption() != null) {

                        return Math.abs(spectrumMatch.getBestTagAssumption().getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));

                    } else {

                        throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");

                    }

                case 6:
                    psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                    return showScores ? psParameter.getScore() : psParameter.getConfidence();

                case 7:
                    psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
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

        boolean canceled = rows.stream()
                .map(i -> identification.getSpectrumMatch(psmKeys[i]))
                .anyMatch(dummy -> waitingHandler.isRunCanceled());

        return canceled ? 0 : rows.size();

    }
}
