package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.data.Chromosome;
import no.uib.jsparklines.data.XYDataPoint;
import no.uib.jsparklines.extra.ChromosomeTableCellRenderer;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesTwoValueBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Model for the protein table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinTableModel extends SelfUpdatingTableModel {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * The identification features generator provides identification information
     * on the matches.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator provides display features.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The exception handler catches exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The identification parameters used for the search.
     */
    private SearchParameters searchParameters;
    /**
     * The list of the keys of the protein matches being displayed.
     */
    private ArrayList<String> proteinKeys = null;
    /**
     * if true the scores will be shown
     */
    private boolean showScores = false;

    /**
     * Constructor which sets a new empty table.
     *
     */
    public ProteinTableModel() {
    }

    /**
     * Constructor. 
     * Warning: when changing this method please update reporter as well!
     *
     * @param identification the identification containing the protein
     * information
     * @param identificationFeaturesGenerator the identification features
     * generator generating the features of the identification
     * @param displayFeaturesGenerator the display features generator generating
     * the display elements
     * @param searchParameters the identification parameters
     * @param exceptionHandler an exception handler catching exceptions
     * @param proteinKeys the keys of the protein matches to display
     */
    public ProteinTableModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, DisplayFeaturesGenerator displayFeaturesGenerator, SearchParameters searchParameters,
            ExceptionHandler exceptionHandler, ArrayList<String> proteinKeys) {
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.searchParameters = searchParameters;
        this.exceptionHandler = exceptionHandler;
        this.proteinKeys = proteinKeys;
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table. 
     * Warning: when changing this method please update reporter as well!
     *
     * @param identification the identification containing the protein
     * information
     * @param identificationFeaturesGenerator the identification features
     * generator generating the features of the identification
     * @param displayFeaturesGenerator the display features generator generating
     * the display elements
     * @param searchParameters the identification parameters
     * @param exceptionHandler an exception handler catching exceptions
     * @param proteinKeys the keys of the protein matches to display
     */
    public void updateDataModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, DisplayFeaturesGenerator displayFeaturesGenerator, SearchParameters searchParameters,
            ExceptionHandler exceptionHandler, ArrayList<String> proteinKeys) {
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.searchParameters = searchParameters;
        this.exceptionHandler = exceptionHandler;
        this.proteinKeys = proteinKeys;
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
        return 13;
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
                return "Chr";
            case 6:
                return "Coverage";
            case 7:
                return "#Peptides";
            case 8:
                return "#Spectra";
            case 9:
                return "MS2 Quant.";
            case 10:
                return "MW";
            case 11:
                if (showScores) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 12:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        if (proteinKeys != null) {

            try {
                boolean useDB = !isSelfUpdating();
                int viewIndex = getViewIndex(row);
                String proteinKey = proteinKeys.get(viewIndex);
                switch (column) {
                    case 0:
                        return viewIndex + 1;
                    case 1:
                        PSParameter pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                        if (!useDB && pSParameter == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        return pSParameter.isStarred();
                    case 2:
                        pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                        if (!useDB && pSParameter == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        return pSParameter.getProteinInferenceClass();
                    case 3:
                        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && proteinMatch == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        return displayFeaturesGenerator.addDatabaseLink(proteinMatch.getMainMatch());
                    case 4:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && proteinMatch == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        String description = "";
                        try {
                            description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription();
                        } catch (Exception e) {
                            exceptionHandler.catchException(e);
                        }
                        return description;
                    case 5:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && proteinMatch == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        String geneName = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getGeneName();
                        String chromosomeNumber = geneFactory.getChromosomeForGeneName(geneName);

                        return new Chromosome(chromosomeNumber);
                    case 6:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && (!identificationFeaturesGenerator.sequenceCoverageInCache(proteinKey)
                                || !identificationFeaturesGenerator.observableCoverageInCache(proteinKey))
                                && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        double sequenceCoverage;
                        try {
                            sequenceCoverage = 100 * identificationFeaturesGenerator.getSequenceCoverage(proteinKey, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                        } catch (Exception e) {
                            exceptionHandler.catchException(e);
                            return Double.NaN;
                        }
                        double possibleCoverage = 100;
                        try {
                            possibleCoverage = 100 * identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                        } catch (Exception e) {
                            exceptionHandler.catchException(e);
                        }
                        return new XYDataPoint(sequenceCoverage, possibleCoverage - sequenceCoverage, true);
                    case 7:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && (proteinMatch == null
                                || !identificationFeaturesGenerator.nValidatedPeptidesInCache(proteinKey)
                                && !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        double nConfidentPeptides = identificationFeaturesGenerator.getNConfidentPeptides(proteinKey);
                        double nDoubtfulPeptides = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey) - nConfidentPeptides;

                        ArrayList<Double> values = new ArrayList<Double>();
                        values.add(nConfidentPeptides);
                        values.add(nDoubtfulPeptides);
                        values.add(proteinMatch.getPeptideCount() - nConfidentPeptides - nDoubtfulPeptides);
                        return values;
                    case 8:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB
                                && (!identificationFeaturesGenerator.nValidatedSpectraInCache(proteinKey)
                                || !identificationFeaturesGenerator.nSpectraInCache(proteinKey))
                                && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        double nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectra(proteinKey);
                        double nDoubtfulSpectra = identificationFeaturesGenerator.getNValidatedSpectra(proteinKey) - nConfidentSpectra;
                        int nSpectra = identificationFeaturesGenerator.getNSpectra(proteinKey);

                        values = new ArrayList<Double>();
                        values.add(nConfidentSpectra);
                        values.add(nDoubtfulSpectra);
                        values.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                        return values;
                    case 9:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && !identificationFeaturesGenerator.spectrumCountingInCache(proteinKey)
                                && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        return identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                    case 10:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && proteinMatch == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        String mainMatch = proteinMatch.getMainMatch();
                        Protein currentProtein = sequenceFactory.getProtein(mainMatch);
                        if (currentProtein != null) {
                            return sequenceFactory.computeMolecularWeight(mainMatch);
                        } else {
                            return null;
                        }
                    case 11:
                        pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                        if (!useDB && pSParameter == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        if (pSParameter != null) {
                            if (showScores) {
                                return pSParameter.getProteinScore();
                            } else {
                                return pSParameter.getProteinConfidence();
                            }
                        } else {
                            return null;
                        }
                    case 12:
                        pSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB);
                        if (!useDB && pSParameter == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        if (pSParameter != null) {
                            return pSParameter.getMatchValidationLevel().getIndex();
                        } else {
                            return null;
                        }
                    default:
                        return "";
                }
            } catch (SQLNonTransientConnectionException e) {
                // this one can be ignored i think?
                return null;
            } catch (Exception e) {
                exceptionHandler.catchException(e);
                return null;
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

    @Override
    protected void catchException(Exception e) {
        setSelfUpdating(false);
        exceptionHandler.catchException(e);
    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, boolean interrupted) {
        ArrayList<String> tempKeys = new ArrayList<String>();
        for (int i : rows) {
            String proteinKey = proteinKeys.get(i);
            tempKeys.add(proteinKey);
        }
        try {
            loadProteins(tempKeys);

            for (int i : rows) {
                String proteinKey = proteinKeys.get(i);
                identificationFeaturesGenerator.getSequenceCoverage(proteinKey, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                if (interrupted) {
                    loadProteins(tempKeys);
                    return i;
                }
                identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                if (interrupted) {
                    loadProteins(tempKeys);
                    return i;
                }
                identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
                if (interrupted) {
                    loadProteins(tempKeys);
                    return i;
                }
                identificationFeaturesGenerator.getNValidatedSpectra(proteinKey);
                if (interrupted) {
                    loadProteins(tempKeys);
                    return i;
                }
                identificationFeaturesGenerator.getNSpectra(proteinKey);
                if (interrupted) {
                    loadProteins(tempKeys);
                    return i;
                }
                identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                if (interrupted) {
                    loadProteins(tempKeys);
                    return i;
                }
            }
        } catch (Exception e) {
            catchException(e);
            return rows.get(0);
        }
        return rows.get(rows.size() - 1);
    }

    /**
     * Loads the protein matches and matches parameters in cache.
     *
     * @param keys the keys of the matches to load
     * @throws SQLException
     * @throws IOException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void loadProteins(ArrayList<String> keys) throws SQLException, IOException, IOException, ClassNotFoundException, InterruptedException {
        identification.loadProteinMatches(keys, null);
        identification.loadProteinMatchParameters(keys, new PSParameter(), null);
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        try {
            if (column == 1
                    || column == 2
                    || column == 11
                    || column == 12) {
                identification.loadProteinMatchParameters(proteinKeys, new PSParameter(), null);
            } else if (column == 3
                    || column == 4
                    || column == 5
                    || column == 6
                    || column == 7
                    || column == 8
                    || column == 9
                    || column == 10) {
                identification.loadProteinMatches(proteinKeys, null);
            }
        } catch (Exception e) {
            catchException(e);
        }
    }

    /**
     * Set up the properties of the protein table.
     * Warning: when changing this method please update reporter as well!
     *
     * @TODO: really did not know where to put this...
     *
     * @param proteinTable the protein table
     * @param sparklineColor the sparkline color to use
     * @param sparklineColorNotValidated the sparkline color for not validated
     * stuffs
     * @param parentClass the parent class used to get icons
     * @param sparklineColorNotFound the sparkline color for not found stuffs
     * @param scoreAndConfidenceDecimalFormat the decimal format for score and
     * confidence
     * @param maxProteinKeyLength the longest protein key to display
     */
    public static void setProteinTableProperties(JTable proteinTable, Color sparklineColor, Color sparklineColorNotValidated, Color sparklineColorNotFound, DecimalFormat scoreAndConfidenceDecimalFormat, Class parentClass, Integer maxProteinKeyLength) {

        // the index column
        proteinTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);

        proteinTable.getColumn("Chr").setMaxWidth(50);
        proteinTable.getColumn("Chr").setMinWidth(50);

        try {
            proteinTable.getColumn("Confidence").setMaxWidth(90);
            proteinTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            proteinTable.getColumn("Score").setMaxWidth(90);
            proteinTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        proteinTable.getColumn("").setMaxWidth(30);
        proteinTable.getColumn("").setMinWidth(30);

        // the selected columns
        proteinTable.getColumn("  ").setMaxWidth(30);
        proteinTable.getColumn("  ").setMinWidth(30);

        // the protein inference column
        proteinTable.getColumn("PI").setMaxWidth(37);
        proteinTable.getColumn("PI").setMinWidth(37);

        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, sparklineColor);
        proteinInferenceColorMap.put(PSParameter.RELATED, Color.YELLOW);
        proteinInferenceColorMap.put(PSParameter.RELATED_AND_UNRELATED, Color.ORANGE);
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the protein inference tooltip map
        HashMap<Integer, String> proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Single Protein");
        proteinInferenceTooltipMap.put(PSParameter.RELATED, "Related Proteins");
        proteinInferenceTooltipMap.put(PSParameter.RELATED_AND_UNRELATED, "Related and Unrelated Proteins");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Unrelated Proteins");

        proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        proteinTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(sparklineColor, proteinInferenceColorMap, proteinInferenceTooltipMap));

        // use a gray color for no decoy searches
        Color nonValidatedColor = sparklineColorNotValidated;
        if (!SequenceFactory.getInstance().concatenatedTargetDecoy()) {
            nonValidatedColor = sparklineColorNotFound;
        }
        ArrayList<Color> sparklineColors = new ArrayList<Color>();
        sparklineColors.add(sparklineColor);
        sparklineColors.add(new Color(255, 204, 0));
        sparklineColors.add(nonValidatedColor);

        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, false));
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, false));
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        proteinTable.getColumn("MS2 Quant.").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        proteinTable.getColumn("MW").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());

        proteinTable.getColumn("Chr").setCellRenderer(new ChromosomeTableCellRenderer());

        try {
            proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, scoreAndConfidenceDecimalFormat);
        } catch (IllegalArgumentException e) {
            proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, scoreAndConfidenceDecimalFormat);
        }

        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesTwoValueBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0,
                sparklineColor, sparklineColorNotFound, true));
        ((JSparklinesTwoValueBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0.00"));
        proteinTable.getColumn("").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(parentClass), MatchValidationLevel.getTooltipMap()));
        proteinTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(parentClass.getResource("/icons/star_yellow.png")),
                new ImageIcon(parentClass.getResource("/icons/star_grey.png")),
                new ImageIcon(parentClass.getResource("/icons/star_grey.png")),
                "Starred", null, null));

        // set the preferred size of the accession column
        Integer width = getPreferredAccessionColumnWidth(proteinTable, proteinTable.getColumn("Accession").getModelIndex(), 6, maxProteinKeyLength);

        if (width != null) {
            proteinTable.getColumn("Accession").setMinWidth(width);
            proteinTable.getColumn("Accession").setMaxWidth(width);
        } else {
            proteinTable.getColumn("Accession").setMinWidth(15);
            proteinTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }
    }

    /**
     * Gets the preferred width of the column specified by colIndex. The column
     * will be just wide enough to show the column head and the widest cell in
     * the column. Margin pixels are added to the left and right (resulting in
     * an additional width of 2*margin pixels. Returns null if the max width
     * cannot be set.
     * Warning: when changing this method please update reporter as well!
     *
     * @param table the table
     * @param colIndex the colum index
     * @param margin the margin to add
     * @param maxProteinKeyLength the maximal protein key length
     *
     * @return the preferred width of the column
     */
    public static Integer getPreferredAccessionColumnWidth(JTable table, int colIndex, int margin, Integer maxProteinKeyLength) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(colIndex);

        // get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        int width = comp.getPreferredSize().width;

        // get maximum width of column data
        if (maxProteinKeyLength == null || maxProteinKeyLength > (table.getColumnName(colIndex).length() + margin)) {
            return null;
        }

        // add margin
        width += 2 * margin;

        return width;
    }
}