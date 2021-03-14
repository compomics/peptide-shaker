package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.gui.file_handling.FileAndFileFilter;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.gui.genes.GeneDetailsDialog;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import static com.compomics.util.experiment.personalization.ExperimentObject.NO_KEY;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import eu.isas.peptideshaker.gui.FractionDetailsDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanel;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanelParent;
import eu.isas.peptideshaker.gui.protein_sequence.ResidueAnnotation;
import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.gui.file_handling.FileChooserUtil;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportScheme;
import eu.isas.peptideshaker.export.PSExportFactory;
import com.compomics.util.io.export.features.peptideshaker.PsProteinFeature;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.extra.ChartPanelTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

/**
 * Displays information about which fractions the peptides and proteins were
 * detected in.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinFractionsPanel extends javax.swing.JPanel implements ProteinSequencePanelParent {

    /**
     * A reference to the main PeptideShakerGUI.
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of protein keys.
     */
    private long[] proteinKeys = new long[0];
    /**
     * A list of the peptides in the peptide table.
     */
    private long[] peptideKeys = new long[0];
    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The coverage table column header tooltips.
     */
    private ArrayList<String> coverageTableToolTips;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * True if the fraction order has been okey'ed by the user.
     */
    private boolean fractionOrderSet = false;
    /**
     * The default line width for the line plots.
     */
    private final int LINE_WIDTH = 4;

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        PROTEIN_TABLE
    };

    /**
     * Creates a new ProteinFractionsPanel.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     */
    public ProteinFractionsPanel(PeptideShakerGUI peptideShakerGUI) {
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        setUpGui();
        formComponentResized(null);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        // make the tabs in the tabbed pane go from right to left
        plotsTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // set main table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);

        // set the row sorter
        SelfUpdatingTableModel.addSortListener(proteinTable, new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true));

        // add scrolling listeners
        SelfUpdatingTableModel.addScrollListeners(proteinTable, proteinTableScrollPane, proteinTableScrollPane.getVerticalScrollBar());

        // make sure that the scroll panes are see-through
        proteinTableScrollPane.getViewport().setOpaque(false);
        coverageTableScrollPane.getViewport().setOpaque(false);

        // set up the table header tooltips
        setUpTableHeaderToolTips();

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(proteinTable.getTableHeader().getBackground());
        proteinTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);

        setTableProperties();
    }

    /**
     * Set up the table header tooltips.
     */
    private void setUpTableHeaderToolTips() {
        proteinTableToolTips = new ArrayList<>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Starred");
        proteinTableToolTips.add("Protein Inference Class");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Chromosome Number");
        proteinTableToolTips.add("Protein Sequence Coverage (%) (Confident / Doubtful / Not Validated / Possible)");
        proteinTableToolTips.add("Number of Peptides (Confident / Doubtful / Not Validated)");
        proteinTableToolTips.add("Number of Spectra (Confident / Doubtful / Not Validated)");
        proteinTableToolTips.add("MS2 Quantification");
        proteinTableToolTips.add("Protein Molecular Weight (kDa)");

        if (peptideShakerGUI.getDisplayParameters().showScores()) {
            proteinTableToolTips.add("Protein Score");
        } else {
            proteinTableToolTips.add("Protein Confidence");
        }

        proteinTableToolTips.add("Validated");

        coverageTableToolTips = new ArrayList<>();
        coverageTableToolTips.add(null);
        coverageTableToolTips.add("Fraction");
        coverageTableToolTips.add("Sequence Coverage");
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {
        setProteinTableProperties();
        setCoverageTableProperties();
    }

    /**
     * Set up the properties of the protein table.
     */
    private void setProteinTableProperties() {

        final int selectedRow = proteinTable.getSelectedRow();

        Integer maxProteinKeyLength = Integer.MAX_VALUE;
        if (peptideShakerGUI.getMetrics() != null) {
            maxProteinKeyLength = peptideShakerGUI.getMetrics().getMaxProteinAccessionLength();
        }

        ProteinTableModel.setProteinTableProperties(proteinTable, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColorNonValidated(),
                peptideShakerGUI.getSparklineColorNotFound(), peptideShakerGUI.getUtilitiesUserParameters().getSparklineColorDoubtful(),
                peptideShakerGUI.getScoreAndConfidenceDecimalFormat(), this.getClass(), maxProteinKeyLength);

        if (selectedRow != -1) {
            proteinTable.getModel().addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            proteinTable.setRowSelectionInterval(selectedRow, selectedRow);
                        }
                    });
                }
            });
        }
    }

    /**
     * Set up the properties of the coverage table.
     */
    private void setCoverageTableProperties() {

        // the index column
        coverageTable.getColumn(" ").setMaxWidth(50);
        coverageTable.getColumn(" ").setMinWidth(50);

        coverageTable.getColumn("Coverage").setCellRenderer(new ChartPanelTableCellRenderer());
    }

    /**
     * Display the results.
     */
    public void displayResults() {

        if (!fractionOrderSet) {
            fractionOrderSet = true;
            new FractionDetailsDialog(peptideShakerGUI, true);
        }

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Fractions. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        });

        new Thread("DisplayThread") {
            @Override
            public void run() {

                proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(progressDialog, peptideShakerGUI.getFilterParameters());

                // update the table model
                if (proteinTable.getModel() instanceof ProteinTableModel && ((ProteinTableModel) proteinTable.getModel()).isInstantiated()) {
                    ((ProteinTableModel) proteinTable.getModel()).updateDataModel(peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(),
                            peptideShakerGUI.getProteinDetailsProvider(), peptideShakerGUI.getSequenceProvider(),
                            peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getDisplayFeaturesGenerator(), proteinKeys);
                } else {
                    ProteinTableModel proteinTableModel = new ProteinTableModel(peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(),
                            peptideShakerGUI.getProteinDetailsProvider(), peptideShakerGUI.getSequenceProvider(), peptideShakerGUI.getGeneMaps(),
                            peptideShakerGUI.getDisplayFeaturesGenerator(), peptideShakerGUI.getExceptionHandler(), proteinKeys);
                    proteinTable.setModel(proteinTableModel);
                }

                setTableProperties();
                showSparkLines(peptideShakerGUI.showSparklines());
                ((DefaultTableModel) proteinTable.getModel()).fireTableDataChanged();

                updateSelection();
                proteinTable.requestFocus();

                setUpTableHeaderToolTips();
                updateProteinTableCellRenderers();

                peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, true);

                String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Proteins (";

                int nValidated = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedProteins();
                int nConfident = peptideShakerGUI.getIdentificationFeaturesGenerator().getNConfidentProteins();
                int nProteins = proteinTable.getRowCount();
                if (nConfident > 0) {
                    title += nValidated + "/" + nProteins + " - " + nConfident + " confident, " + (nValidated - nConfident) + " doubtful";
                } else {
                    title += nValidated + "/" + nProteins;
                }
                title += ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;

                // update the border titles
                ((TitledBorder) proteinPanel.getBorder()).setTitle(title);
                proteinPanel.repaint();

                plotsTabbedPane.setSelectedIndex(5);

                // enable the contextual export options
                exportProteinsJButton.setEnabled(true);
                exportPeptidesJButton.setEnabled(true);

                progressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Update the peptide counts plot.
     */
    private void updatePlots() {

        // @TODO: add progress bar
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        Identification identification = peptideShakerGUI.getIdentification();

        // @TODO: this method should be split into smaller methods...
        ArrayList<String> fileNames = new ArrayList<>();

        for (String fileName : identification.getFractions()) {
            fileNames.add(fileName);
        }

        DefaultCategoryDataset peptidePlotDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset spectrumPlotDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset intensityPlotDataset = new DefaultCategoryDataset();

        int[] selectedRows = proteinTable.getSelectedRows();

        // disable the coverage tab if more than one protein is selected
        plotsTabbedPane.setEnabledAt(2, selectedRows.length == 1);

        if (selectedRows.length > 1 && plotsTabbedPane.getSelectedIndex() == 2) {
            plotsTabbedPane.setSelectedIndex(5);
        }

        for (int row = 0; row < selectedRows.length; row++) {

            int currentRow = selectedRows[row];
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            int proteinIndex = tableModel.getViewIndex(currentRow);
            long proteinKey = proteinKeys[proteinIndex];
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            peptideKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getSortedPeptideKeys(proteinKey);
            PSParameter proteinPSParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

            // get the current protein information
            String currentAccession = proteinMatch.getLeadingAccession();
            String currentProteinSequence = peptideShakerGUI.getSequenceProvider().getSequence(currentAccession);
            String currentProteinDescription = peptideShakerGUI.getProteinDetailsProvider().getSimpleDescription(currentAccession);

            int[][] coverage = new int[fileNames.size()][currentProteinSequence.length() + 1];

            // get the chart data
            for (int i = 0; i < fileNames.size(); i++) {

                String fraction = fileNames.get(i);

                for (long peptideKey : peptideKeys) {

                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    Peptide peptide = peptideMatch.getPeptide();
                    PSParameter peptidePSParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                    if (peptidePSParameter.getFractionScore() != null && peptidePSParameter.getFractions().contains(fraction)) {
                        if (peptidePSParameter.getMatchValidationLevel().isValidated()) {

                            String peptideSequence = peptide.getSequence();

                            boolean includePeptide = false;

                            DigestionParameters digestionParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getDigestionParameters();

                            if (coverageShowAllPeptidesJRadioButtonMenuItem.isSelected() || digestionParameters.getCleavageParameter() != DigestionParameters.CleavageParameter.enzyme) {

                                includePeptide = true;

                            } else if (coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.isSelected()) {

                                includePeptide = PeptideUtils.isEnzymatic(peptide, currentAccession, currentProteinSequence, digestionParameters.getEnzymes());

                            } else if (coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.isSelected()) {

                                includePeptide = !PeptideUtils.isEnzymatic(peptide, currentAccession, currentProteinSequence, digestionParameters.getEnzymes());

                            }

                            if (includePeptide && selectedRows.length == 1) {

                                for (int startIndex : peptide.getProteinMapping().get(currentAccession)) {

                                    int peptideTempStart = startIndex;
                                    int peptideTempEnd = peptideTempStart + peptideSequence.length();

                                    for (int k = peptideTempStart; k < peptideTempEnd; k++) {

                                        coverage[i][k]++;

                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < fileNames.size(); i++) {

                String fraction = fileNames.get(i);

                if (selectedRows.length == 1) {
                    peptidePlotDataset.addValue(proteinPSParameter.getFractionValidatedPeptides(fraction), "Validated Peptides", "" + (i + 1));
                } else {
                    peptidePlotDataset.addValue(proteinPSParameter.getFractionValidatedPeptides(fraction), proteinMatch.getLeadingAccession()
                            + ": " + currentProteinDescription, "" + (i + 1));
                }
            }

            double longestFileName = "Fraction".length();

            // update the coverage table
            if (selectedRows.length == 1) {

                DefaultTableModel coverageTableModel = (DefaultTableModel) coverageTable.getModel();
                coverageTableModel.getDataVector().removeAllElements();

                for (int i = 0; i < fileNames.size(); i++) {

                    // create the coverage plot
                    ArrayList<JSparklinesDataSeries> sparkLineDataSeriesCoverage = new ArrayList<>();

                    for (int j = 0; j < currentProteinSequence.length(); j++) {

                        boolean covered = coverage[i][j] > 0;

                        int sequenceCounter = 1;

                        if (covered) {
                            while (j + 1 < coverage[0].length && coverage[i][j + 1] > 0) {
                                sequenceCounter++;
                                j++;
                            }
                        } else {
                            while (j + 1 < coverage[0].length && coverage[i][j + 1] == 0) {
                                sequenceCounter++;
                                j++;
                            }
                        }

                        ArrayList<Double> data = new ArrayList<>();
                        data.add(Double.valueOf(sequenceCounter));

                        JSparklinesDataSeries sparklineDataseries;

                        if (covered) {
                            sparklineDataseries = new JSparklinesDataSeries(data, peptideShakerGUI.getSparklineColor(), null);
                        } else {
                            sparklineDataseries = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                        }

                        sparkLineDataSeriesCoverage.add(sparklineDataseries);
                    }

                    ChartPanel coverageChart = new ProteinSequencePanel(Color.WHITE).getSequencePlot(this, new JSparklinesDataset(sparkLineDataSeriesCoverage),
                            new HashMap<>(), true, true);

                    ((DefaultTableModel) coverageTable.getModel()).addRow(new Object[]{(i + 1), fileNames.get(i), coverageChart});

                    if (fileNames.get(i).length() > longestFileName) {
                        longestFileName = fileNames.get(i).length();
                    }
                }
            }

            // set the preferred size of the fraction name column in the coverage table
            Integer width = peptideShakerGUI.getPreferredColumnWidth(coverageTable, coverageTable.getColumn("Fraction").getModelIndex(), 6);

            if (width != null) {
                coverageTable.getColumn("Fraction").setMinWidth(width);
                coverageTable.getColumn("Fraction").setMaxWidth(width);
            } else {
                coverageTable.getColumn("Fraction").setMinWidth(15);
                coverageTable.getColumn("Fraction").setMaxWidth(Integer.MAX_VALUE);
            }

            // get the psms per fraction
            for (int i = 0; i < fileNames.size(); i++) {
                String fraction = fileNames.get(i);

                if (selectedRows.length == 1) {
                    spectrumPlotDataset.addValue(proteinPSParameter.getFractionValidatedSpectra(fraction), "Validated Spectra", "" + (i + 1));
                    intensityPlotDataset.addValue(proteinPSParameter.getPrecursorIntensitySummedPerFraction(fraction), "Summed Intensity", "" + (i + 1));
                } else {
                    spectrumPlotDataset.addValue(proteinPSParameter.getFractionValidatedSpectra(fraction), proteinMatch.getLeadingAccession()
                            + ": " + currentProteinDescription, "" + (i + 1));
                    intensityPlotDataset.addValue(proteinPSParameter.getPrecursorIntensitySummedPerFraction(fraction), proteinMatch.getLeadingAccession()
                            + ": " + currentProteinDescription, "" + (i + 1));
                }
            }
        }

        // molecular mass plot
        DefaultBoxAndWhiskerCategoryDataset mwPlotDataset = new DefaultBoxAndWhiskerCategoryDataset();

        ArrayList<String> spectrumFiles = peptideShakerGUI.getIdentification().getFractions();

        for (int i = 0; i < spectrumFiles.size(); i++) {

            try {
                
                if (peptideShakerGUI.getMetrics().getObservedFractionalMassesAll().containsKey(spectrumFiles.get(i))) {

                    mwPlotDataset.add(peptideShakerGUI.getMetrics().getObservedFractionalMassesAll().get(spectrumFiles.get(i)), "Observed MW (kDa)", "" + (i + 1));

                } else {

                    mwPlotDataset.add(new ArrayList<>(0), "Observed MW (kDa)", "" + (i + 1));

                }
            } catch (ClassCastException e) {
                // do nothing, no data to show
            }
        }

        // total peptides per fraction plot
        DefaultCategoryDataset totalPeptidesPerFractionPlotDataset = new DefaultCategoryDataset();
        HashMap<String, Integer> totalPeptidesPerFraction = peptideShakerGUI.getMetrics().getTotalPeptidesPerFraction();

        for (int i = 0; i < spectrumFiles.size(); i++) {

            String spectrumKey = spectrumFiles.get(i);

            if (totalPeptidesPerFraction != null && totalPeptidesPerFraction.containsKey(spectrumKey)) {
                totalPeptidesPerFractionPlotDataset.addValue(totalPeptidesPerFraction.get(spectrumKey), "Total Peptide Count", "" + (i + 1));
            } else {
                totalPeptidesPerFractionPlotDataset.addValue(0, "Total Peptide Count", "" + (i + 1));
            }
        }

        // create the peptide chart
        JFreeChart chart = ChartFactory.createBarChart(null, "Fraction", "#Peptides", peptidePlotDataset, PlotOrientation.VERTICAL, false, true, true);
        ChartPanel chartPanel = new ChartPanel(chart);

        AbstractCategoryItemRenderer renderer;

        // set up the renderer
        //if (selectedRows.length == 1) {
        renderer = new BarRenderer();
        ((BarRenderer) renderer).setShadowVisible(false);
        renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
//            } else {
//                renderer = new LineAndShapeRenderer(true, false);
//                for (int i = 0; i < selectedRows.length; i++) {
//                    ((LineAndShapeRenderer) renderer).setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//                }
//            }

        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        chart.getCategoryPlot().setRenderer(renderer);

        // set background color
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        // hide the outline
        chart.getPlot().setOutlineVisible(false);

        // clear the peptide plot
        peptidePlotPanel.removeAll();

        // add the new plot
        peptidePlotPanel.add(chartPanel);

        // create the spectrum chart
        chart = ChartFactory.createBarChart(null, "Fraction", "#Spectra", spectrumPlotDataset, PlotOrientation.VERTICAL, false, true, true);
        chartPanel = new ChartPanel(chart);

        // set up the renderer
        //if (selectedRows.length == 1) {
        renderer = new BarRenderer();
        ((BarRenderer) renderer).setShadowVisible(false);
        renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
//            } else {
//                renderer = new LineAndShapeRenderer(true, false);
//                for (int i = 0; i < selectedRows.length; i++) {
//                    ((LineAndShapeRenderer) renderer).setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//                }
//            }

        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        chart.getCategoryPlot().setRenderer(renderer);

        // set background color
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        // hide the outline
        chart.getPlot().setOutlineVisible(false);

        // clear the peptide plot
        spectraPlotPanel.removeAll();

        // add the new plot
        spectraPlotPanel.add(chartPanel);

        // create the intensity chart
        chart = ChartFactory.createBarChart(null, "Fraction", "Summed Intensity", intensityPlotDataset, PlotOrientation.VERTICAL, false, true, true);
        chartPanel = new ChartPanel(chart);

        // set up the renderer
//            if (selectedRows.length == 1) {
        renderer = new BarRenderer();
        ((BarRenderer) renderer).setShadowVisible(false);
        renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
//            } else {
//                renderer = new LineAndShapeRenderer(true, false);
//                for (int i = 0; i < selectedRows.length; i++) {
//                    ((LineAndShapeRenderer) renderer).setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//                }
//            }

        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        chart.getCategoryPlot().setRenderer(renderer);

        // set background color
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        // hide the outline
        chart.getPlot().setOutlineVisible(false);

        // clear the peptide plot
        intensityPlotPanel.removeAll();

        // add the new plot
        intensityPlotPanel.add(chartPanel);

        // create the mw chart
        chart = ChartFactory.createBoxAndWhiskerChart(null, "Fraction", "Expected Molecular Weight (kDa)", mwPlotDataset, false);
        chartPanel = new ChartPanel(chart);

        // set up the renderer
        BoxAndWhiskerRenderer boxPlotRenderer = new BoxAndWhiskerRenderer();
        boxPlotRenderer.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        boxPlotRenderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
        boxPlotRenderer.setSeriesPaint(1, Color.RED);
        chart.getCategoryPlot().setRenderer(boxPlotRenderer);

        // set background color
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        // hide the outline
        chart.getPlot().setOutlineVisible(false);

        // clear the peptide plot
        mwPlotPanel.removeAll();

        // add the new plot
        mwPlotPanel.add(chartPanel);

        // create the total peptides count chart
        chart = ChartFactory.createBarChart(null, "Fraction", "Total Peptide Count", totalPeptidesPerFractionPlotDataset, PlotOrientation.VERTICAL, false, true, true);
        chartPanel = new ChartPanel(chart);
        renderer = new BarRenderer();
        ((BarRenderer) renderer).setShadowVisible(false);
        renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        chart.getCategoryPlot().setRenderer(renderer);

        // set background color
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        // hide the outline
        chart.getPlot().setOutlineVisible(false);

        // clear the peptide plot
        fractionsPlotPanel.removeAll();

        // add the new plot
        fractionsPlotPanel.add(chartPanel);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        plotsPanel.revalidate();
        plotsPanel.repaint();
    }

    /**
     * Update the selected protein and peptide.
     */
    public void updateSelection() {

        int proteinRow = 0;
        long proteinKey = peptideShakerGUI.getSelectedProteinKey();
        long peptideKey = peptideShakerGUI.getSelectedPeptideKey();
        String spectrumFile = peptideShakerGUI.getSelectedSpectrumFile();
        String spectrumTitle = peptideShakerGUI.getSelectedSpectrumTitle();
        
        Identification identification = peptideShakerGUI.getIdentification();

        if (proteinKey == NO_KEY
                && peptideKey == NO_KEY
                && spectrumFile != null
                && spectrumTitle != null
                ) {
            
            long psmKey = SpectrumMatch.getKey(spectrumFile, spectrumTitle);
            SpectrumMatch spectrumMatch = (SpectrumMatch) peptideShakerGUI.getIdentification().retrieveObject(psmKey);
            
            if (spectrumMatch != null && spectrumMatch.getBestPeptideAssumption() != null) {

                Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                peptideKey = peptide.getMatchingKey(peptideShakerGUI.getIdentificationParameters().getSequenceMatchingParameters());

            }
        }

        if (proteinKey == NO_KEY 
                && peptideKey != NO_KEY) {

            final long peptideKeyFinal = peptideKey;
            ProteinMatch tempProteinMatch = identification.getProteinIdentification().parallelStream()
                    .map(key -> identification.getProteinMatch(key))
                    .filter(proteinMatch -> Arrays.stream(proteinMatch.getPeptideMatchesKeys())
                    .anyMatch(key -> key == peptideKeyFinal))
                    .findAny()
                    .orElse(null);

            if (tempProteinMatch != null) {

                peptideShakerGUI.setSelectedItems(tempProteinMatch.getKey(), peptideKey, spectrumFile, spectrumTitle);

            }
        }

        if (proteinKey != NO_KEY) {

            proteinRow = getProteinRow(proteinKey);

        }

        if (proteinKeys.length == 0) {
            
            clearData();
            return;
        
        }

        if (proteinRow == -1) {
            
            peptideShakerGUI.resetSelectedItems();
        
        } else if (proteinTable.getSelectedRow() != proteinRow) {
        
            proteinTable.setRowSelectionInterval(proteinRow, proteinRow);
            proteinTable.scrollRectToVisible(proteinTable.getCellRect(proteinRow, 0, false));
            proteinTableKeyReleased(null);
        
        }
    }

    /**
     * Returns the row of a desired protein.
     *
     * @param proteinKey the key of the protein
     *
     * @return the row of the desired protein
     */
    private int getProteinRow(long proteinKey) {

        int modelIndex = IntStream.range(0, proteinKeys.length)
                .filter(i -> proteinKeys[i] == proteinKey)
                .findAny()
                .orElse(-1);

        return modelIndex == -1 ? -1 : ((SelfUpdatingTableModel) proteinTable.getModel()).getRowNumber(modelIndex);

    }

    /**
     * Clear all the data.
     */
    public void clearData() {

        proteinKeys = new long[0];
        peptideKeys = new long[0];

        ProteinTableModel proteinTableModel = (ProteinTableModel) proteinTable.getModel();
        proteinTableModel.reset();

        proteinTableModel.fireTableDataChanged();

        ((TitledBorder) proteinPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Proteins" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        proteinPanel.repaint();
        ((TitledBorder) plotsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptides" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        plotsPanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sequenceCoverageJPopupMenu = new javax.swing.JPopupMenu();
        coverageShowAllPeptidesJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        coveragePeptideTypesButtonGroup = new javax.swing.ButtonGroup();
        proteinPeptideSplitPane = new javax.swing.JSplitPane();
        plotsLayeredPane = new javax.swing.JLayeredPane();
        plotsPanel = new javax.swing.JPanel();
        plotsTabbedPane = new javax.swing.JTabbedPane();
        mwPanel = new javax.swing.JPanel();
        mwPlotPanel = new javax.swing.JPanel();
        totalPeptidesPerFractionPlotOuterPanel = new javax.swing.JPanel();
        fractionsPlotPanel = new javax.swing.JPanel();
        coverageTablePanel = new javax.swing.JPanel();
        coverageTableScrollPane = new javax.swing.JScrollPane();
        coverageTable =         new JTable() {

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) coverageTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        intensityPlotOuterPanel = new javax.swing.JPanel();
        intensityPlotPanel = new javax.swing.JPanel();
        spectraPlotOuterPanel = new javax.swing.JPanel();
        spectraPlotPanel = new javax.swing.JPanel();
        peptidePlotOuterPanel = new javax.swing.JPanel();
        peptidePlotPanel = new javax.swing.JPanel();
        peptidesHelpJButton = new javax.swing.JButton();
        exportPeptidesJButton = new javax.swing.JButton();
        sequenceCoverageOptionsJButton = new javax.swing.JButton();
        contextMenuPeptidesBackgroundPanel = new javax.swing.JPanel();
        proteinsLayeredPane = new javax.swing.JLayeredPane();
        proteinPanel = new javax.swing.JPanel();
        proteinTableScrollPane = new javax.swing.JScrollPane();
        proteinTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) proteinTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        proteinsHelpJButton = new javax.swing.JButton();
        exportProteinsJButton = new javax.swing.JButton();
        contextMenuProteinsBackgroundPanel = new javax.swing.JPanel();

        coveragePeptideTypesButtonGroup.add(coverageShowAllPeptidesJRadioButtonMenuItem);
        coverageShowAllPeptidesJRadioButtonMenuItem.setSelected(true);
        coverageShowAllPeptidesJRadioButtonMenuItem.setText("All Peptides");
        coverageShowAllPeptidesJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowAllPeptidesJRadioButtonMenuItem);

        coveragePeptideTypesButtonGroup.add(coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem);
        coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.setText("Enzymatic Peptides");
        coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem);

        coveragePeptideTypesButtonGroup.add(coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem);
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.setText("Non Enzymatic Peptides");
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem);

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        proteinPeptideSplitPane.setBorder(null);
        proteinPeptideSplitPane.setDividerLocation(200);
        proteinPeptideSplitPane.setDividerSize(0);
        proteinPeptideSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        proteinPeptideSplitPane.setResizeWeight(0.5);
        proteinPeptideSplitPane.setOpaque(false);

        plotsPanel.setBackground(new java.awt.Color(255, 255, 255));
        plotsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Plots"));

        plotsTabbedPane.setBackground(new java.awt.Color(255, 255, 255));
        plotsTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        plotsTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                plotsTabbedPaneStateChanged(evt);
            }
        });

        mwPanel.setBackground(new java.awt.Color(255, 255, 255));

        mwPlotPanel.setOpaque(false);
        mwPlotPanel.setLayout(new javax.swing.BoxLayout(mwPlotPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout mwPanelLayout = new javax.swing.GroupLayout(mwPanel);
        mwPanel.setLayout(mwPanelLayout);
        mwPanelLayout.setHorizontalGroup(
            mwPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mwPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        mwPanelLayout.setVerticalGroup(
            mwPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mwPanelLayout.createSequentialGroup()
                .addComponent(mwPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotsTabbedPane.addTab("Molecular Weight", mwPanel);

        totalPeptidesPerFractionPlotOuterPanel.setBackground(new java.awt.Color(255, 255, 255));

        fractionsPlotPanel.setOpaque(false);
        fractionsPlotPanel.setLayout(new javax.swing.BoxLayout(fractionsPlotPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout totalPeptidesPerFractionPlotOuterPanelLayout = new javax.swing.GroupLayout(totalPeptidesPerFractionPlotOuterPanel);
        totalPeptidesPerFractionPlotOuterPanel.setLayout(totalPeptidesPerFractionPlotOuterPanelLayout);
        totalPeptidesPerFractionPlotOuterPanelLayout.setHorizontalGroup(
            totalPeptidesPerFractionPlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(fractionsPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        totalPeptidesPerFractionPlotOuterPanelLayout.setVerticalGroup(
            totalPeptidesPerFractionPlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(totalPeptidesPerFractionPlotOuterPanelLayout.createSequentialGroup()
                .addComponent(fractionsPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotsTabbedPane.addTab("Fractions", totalPeptidesPerFractionPlotOuterPanel);

        coverageTablePanel.setBackground(new java.awt.Color(255, 255, 255));

        coverageTableScrollPane.setOpaque(false);

        coverageTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Fraction", "Coverage"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        coverageTable.setOpaque(false);
        coverageTableScrollPane.setViewportView(coverageTable);

        javax.swing.GroupLayout coverageTablePanelLayout = new javax.swing.GroupLayout(coverageTablePanel);
        coverageTablePanel.setLayout(coverageTablePanelLayout);
        coverageTablePanelLayout.setHorizontalGroup(
            coverageTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 903, Short.MAX_VALUE)
            .addGroup(coverageTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(coverageTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE))
        );
        coverageTablePanelLayout.setVerticalGroup(
            coverageTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 277, Short.MAX_VALUE)
            .addGroup(coverageTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, coverageTablePanelLayout.createSequentialGroup()
                    .addComponent(coverageTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        plotsTabbedPane.addTab("Coverage", coverageTablePanel);

        intensityPlotOuterPanel.setBackground(new java.awt.Color(255, 255, 255));

        intensityPlotPanel.setOpaque(false);
        intensityPlotPanel.setLayout(new javax.swing.BoxLayout(intensityPlotPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout intensityPlotOuterPanelLayout = new javax.swing.GroupLayout(intensityPlotOuterPanel);
        intensityPlotOuterPanel.setLayout(intensityPlotOuterPanelLayout);
        intensityPlotOuterPanelLayout.setHorizontalGroup(
            intensityPlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(intensityPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        intensityPlotOuterPanelLayout.setVerticalGroup(
            intensityPlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(intensityPlotOuterPanelLayout.createSequentialGroup()
                .addComponent(intensityPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotsTabbedPane.addTab("Intensities", intensityPlotOuterPanel);

        spectraPlotOuterPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectraPlotPanel.setOpaque(false);
        spectraPlotPanel.setLayout(new javax.swing.BoxLayout(spectraPlotPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout spectraPlotOuterPanelLayout = new javax.swing.GroupLayout(spectraPlotOuterPanel);
        spectraPlotOuterPanel.setLayout(spectraPlotOuterPanelLayout);
        spectraPlotOuterPanelLayout.setHorizontalGroup(
            spectraPlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectraPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        spectraPlotOuterPanelLayout.setVerticalGroup(
            spectraPlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectraPlotOuterPanelLayout.createSequentialGroup()
                .addComponent(spectraPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotsTabbedPane.addTab("Spectra", spectraPlotOuterPanel);

        peptidePlotOuterPanel.setBackground(new java.awt.Color(255, 255, 255));

        peptidePlotPanel.setOpaque(false);
        peptidePlotPanel.setLayout(new javax.swing.BoxLayout(peptidePlotPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout peptidePlotOuterPanelLayout = new javax.swing.GroupLayout(peptidePlotOuterPanel);
        peptidePlotOuterPanel.setLayout(peptidePlotOuterPanelLayout);
        peptidePlotOuterPanelLayout.setHorizontalGroup(
            peptidePlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptidePlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        peptidePlotOuterPanelLayout.setVerticalGroup(
            peptidePlotOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePlotOuterPanelLayout.createSequentialGroup()
                .addComponent(peptidePlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotsTabbedPane.addTab("Peptides", peptidePlotOuterPanel);

        javax.swing.GroupLayout plotsPanelLayout = new javax.swing.GroupLayout(plotsPanel);
        plotsPanel.setLayout(plotsPanelLayout);
        plotsPanelLayout.setHorizontalGroup(
            plotsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, plotsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(plotsTabbedPane)
                .addContainerGap())
        );
        plotsPanelLayout.setVerticalGroup(
            plotsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(plotsTabbedPane)
                .addContainerGap())
        );

        plotsPanel.setBounds(0, 0, 940, 350);
        plotsLayeredPane.add(plotsPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        peptidesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        peptidesHelpJButton.setToolTipText("Help");
        peptidesHelpJButton.setBorder(null);
        peptidesHelpJButton.setBorderPainted(false);
        peptidesHelpJButton.setContentAreaFilled(false);
        peptidesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        peptidesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptidesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesHelpJButtonMouseExited(evt);
            }
        });
        peptidesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidesHelpJButtonActionPerformed(evt);
            }
        });
        peptidesHelpJButton.setBounds(930, 0, 10, 19);
        plotsLayeredPane.add(peptidesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPeptidesJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setToolTipText("Export");
        exportPeptidesJButton.setBorder(null);
        exportPeptidesJButton.setBorderPainted(false);
        exportPeptidesJButton.setContentAreaFilled(false);
        exportPeptidesJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setEnabled(false);
        exportPeptidesJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPeptidesJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPeptidesJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPeptidesJButtonMouseExited(evt);
            }
        });
        exportPeptidesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPeptidesJButtonActionPerformed(evt);
            }
        });
        exportPeptidesJButton.setBounds(920, 0, 10, 19);
        plotsLayeredPane.add(exportPeptidesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        sequenceCoverageOptionsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        sequenceCoverageOptionsJButton.setToolTipText("Coverage Options");
        sequenceCoverageOptionsJButton.setBorder(null);
        sequenceCoverageOptionsJButton.setBorderPainted(false);
        sequenceCoverageOptionsJButton.setContentAreaFilled(false);
        sequenceCoverageOptionsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_black.png"))); // NOI18N
        sequenceCoverageOptionsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sequenceCoverageOptionsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sequenceCoverageOptionsJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sequenceCoverageOptionsJButtonMouseReleased(evt);
            }
        });
        sequenceCoverageOptionsJButton.setBounds(895, 5, 10, 19);
        plotsLayeredPane.add(sequenceCoverageOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPeptidesBackgroundPanel);
        contextMenuPeptidesBackgroundPanel.setLayout(contextMenuPeptidesBackgroundPanelLayout);
        contextMenuPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 50, Short.MAX_VALUE)
        );
        contextMenuPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        contextMenuPeptidesBackgroundPanel.setBounds(890, 0, 50, 19);
        plotsLayeredPane.add(contextMenuPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        proteinPeptideSplitPane.setRightComponent(plotsLayeredPane);

        proteinPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinPanel.setOpaque(false);

        proteinTableScrollPane.setOpaque(false);

        proteinTable.setModel(new eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel());
        proteinTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
        });
        proteinTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                proteinTableMouseMoved(evt);
            }
        });
        proteinTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinTableKeyReleased(evt);
            }
        });
        proteinTableScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout proteinPanelLayout = new javax.swing.GroupLayout(proteinPanel);
        proteinPanel.setLayout(proteinPanelLayout);
        proteinPanelLayout.setHorizontalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 908, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinPanelLayout.setVerticalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinPanel.setBounds(0, 0, 940, 200);
        proteinsLayeredPane.add(proteinPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        proteinsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        proteinsHelpJButton.setToolTipText("Help");
        proteinsHelpJButton.setBorder(null);
        proteinsHelpJButton.setBorderPainted(false);
        proteinsHelpJButton.setContentAreaFilled(false);
        proteinsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        proteinsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseExited(evt);
            }
        });
        proteinsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinsHelpJButtonActionPerformed(evt);
            }
        });
        proteinsHelpJButton.setBounds(930, 0, 10, 19);
        proteinsLayeredPane.add(proteinsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setToolTipText("Copy to File");
        exportProteinsJButton.setBorder(null);
        exportProteinsJButton.setBorderPainted(false);
        exportProteinsJButton.setContentAreaFilled(false);
        exportProteinsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setEnabled(false);
        exportProteinsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportProteinsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseExited(evt);
            }
        });
        exportProteinsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProteinsJButtonActionPerformed(evt);
            }
        });
        exportProteinsJButton.setBounds(920, 0, 10, 19);
        proteinsLayeredPane.add(exportProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuProteinsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuProteinsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setLayout(contextMenuProteinsBackgroundPanelLayout);
        contextMenuProteinsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuProteinsBackgroundPanelLayout.setVerticalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        contextMenuProteinsBackgroundPanel.setBounds(910, 0, 40, 19);
        proteinsLayeredPane.add(contextMenuProteinsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        proteinPeptideSplitPane.setLeftComponent(proteinsLayeredPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinPeptideSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 944, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinPeptideSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the selection.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (row != -1) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            int proteinIndex = tableModel.getViewIndex(row);

            if (proteinIndex != -1) {
                // remember the selection
                newItemSelection();
                updatePlots();
            }

            if (evt != null && evt.getButton() == MouseEvent.BUTTON1 && proteinIndex != -1) {

                long proteinKey = proteinKeys[proteinIndex];
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);

                // open the gene details dialog
                if (column == proteinTable.getColumn("Chr").getModelIndex()
                        && evt.getButton() == MouseEvent.BUTTON1) {

                    new GeneDetailsDialog(peptideShakerGUI, proteinMatch, peptideShakerGUI.getGeneMaps(), peptideShakerGUI.getProteinDetailsProvider());

                }

                // star/unstar the protein
                if (column == proteinTable.getColumn("  ").getModelIndex()) {
                    PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                    if (!psParameter.getStarred()) {
                        peptideShakerGUI.getStarHider().starProtein(proteinKey);
                    } else {
                        peptideShakerGUI.getStarHider().unStarProtein(proteinKey);
                    }
                    peptideShakerGUI.setDataSaved(false);
                }

                // open protein link in web browser
                if (column == proteinTable.getColumn("Accession").getModelIndex()
                        && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<a href=\"") != -1) {

                    String link = (String) proteinTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Update the selection.
     *
     * @param evt
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased

        int row = proteinTable.getSelectedRow();

        if (row != -1) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            int proteinIndex = tableModel.getViewIndex(row);

            if (proteinIndex != -1) {
                // remember the selection
                newItemSelection();
                updatePlots();
            }
        }
    }//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void proteinTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
     * link.
     *
     * @param evt
     */
    private void proteinTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseMoved
        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.columnAtPoint(evt.getPoint());

        proteinTable.setToolTipText(null);

        if (row != -1 && column != -1 && column == proteinTable.getColumn("Accession").getModelIndex() && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<a href=\"") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinTable.getColumn("Chr").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (column == proteinTable.getColumn("Description").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            if (GuiUtilities.getPreferredWidthOfCell(proteinTable, row, column) > proteinTable.getColumn("Description").getWidth()) {
                proteinTable.setToolTipText("" + proteinTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Update the layered panes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        proteinPeptideSplitPane.setDividerLocation(proteinPeptideSplitPane.getHeight() / 100 * 30);

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // move the icons
                proteinsLayeredPane.getComponent(0).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        proteinsLayeredPane.getComponent(0).getWidth(),
                        proteinsLayeredPane.getComponent(0).getHeight());

                proteinsLayeredPane.getComponent(1).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        proteinsLayeredPane.getComponent(1).getWidth(),
                        proteinsLayeredPane.getComponent(1).getHeight());

                proteinsLayeredPane.getComponent(2).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        proteinsLayeredPane.getComponent(2).getWidth(),
                        proteinsLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                proteinsLayeredPane.getComponent(3).setBounds(0, 0, proteinsLayeredPane.getWidth(), proteinsLayeredPane.getHeight());
                proteinsLayeredPane.revalidate();
                proteinsLayeredPane.repaint();

                // move the icons
                plotsLayeredPane.getComponent(0).setBounds(
                        plotsLayeredPane.getWidth() - plotsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        plotsLayeredPane.getComponent(0).getWidth(),
                        plotsLayeredPane.getComponent(0).getHeight());

                plotsLayeredPane.getComponent(1).setBounds(
                        plotsLayeredPane.getWidth() - plotsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        plotsLayeredPane.getComponent(1).getWidth(),
                        plotsLayeredPane.getComponent(1).getHeight());

                plotsLayeredPane.getComponent(2).setBounds(
                        plotsLayeredPane.getWidth() - plotsLayeredPane.getComponent(2).getWidth() - 34,
                        0,
                        plotsLayeredPane.getComponent(2).getWidth(),
                        plotsLayeredPane.getComponent(2).getHeight());

                plotsLayeredPane.getComponent(3).setBounds(
                        plotsLayeredPane.getWidth() - plotsLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        plotsLayeredPane.getComponent(3).getWidth(),
                        plotsLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                plotsLayeredPane.getComponent(4).setBounds(0, 0, plotsLayeredPane.getWidth(), plotsLayeredPane.getHeight());
                plotsLayeredPane.revalidate();
                plotsLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void proteinsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void proteinsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void proteinsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FractionsTab.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Fraction Analysis - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportProteinsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportProteinsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportProteinsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProteinsJButtonActionPerformed
        copyTableContentToClipboardOrFile(TableIndex.PROTEIN_TABLE);
    }//GEN-LAST:event_exportProteinsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void peptidesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptidesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void peptidesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FractionsTab.html"), "#Plots",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Fraction Analysis - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportPeptidesJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPeptidesJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportPeptidesJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPeptidesJButtonMouseExited

    /**
     * Export the table or plot.
     *
     * @param evt
     */
    private void exportPeptidesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonActionPerformed

        ChartPanel chartPanel = null;

        if (plotsTabbedPane.getSelectedIndex() == 0) {
            chartPanel = (ChartPanel) mwPlotPanel.getComponent(0);
        } else if (plotsTabbedPane.getSelectedIndex() == 1) {
            // not supported
        } else if (plotsTabbedPane.getSelectedIndex() == 2) {
            chartPanel = (ChartPanel) fractionsPlotPanel.getComponent(0);
        } else if (plotsTabbedPane.getSelectedIndex() == 3) {
            chartPanel = (ChartPanel) intensityPlotPanel.getComponent(0);
        } else if (plotsTabbedPane.getSelectedIndex() == 4) {
            chartPanel = (ChartPanel) spectraPlotPanel.getComponent(0);
        } else if (plotsTabbedPane.getSelectedIndex() == 5) {
            chartPanel = (ChartPanel) peptidePlotPanel.getComponent(0);
        }

        if (chartPanel != null) {
            new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, chartPanel, peptideShakerGUI.getLastSelectedFolder());
        }
    }//GEN-LAST:event_exportPeptidesJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void sequenceCoverageOptionsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoverageOptionsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_sequenceCoverageOptionsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void sequenceCoverageOptionsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoverageOptionsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_sequenceCoverageOptionsJButtonMouseExited

    /**
     * Show the sequence coverage options.
     *
     * @param evt
     */
    private void sequenceCoverageOptionsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoverageOptionsJButtonMouseReleased
        sequenceCoverageJPopupMenu.show(sequenceCoverageOptionsJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_sequenceCoverageOptionsJButtonMouseReleased

    /**
     * Update the sequence coverage maps.
     *
     * @param evt
     */
    private void coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed
        if (proteinTable.getSelectedRow() != -1) {
            updatePlots();
        }
    }//GEN-LAST:event_coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed

    /**
     * @see #coverageShowAllPeptidesJRadioButtonMenuItem
     */
    private void coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed
        coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(null);
    }//GEN-LAST:event_coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed

    /**
     * @see #coverageShowAllPeptidesJRadioButtonMenuItem
     */
    private void coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed
        coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(null);
    }//GEN-LAST:event_coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed

    /**
     * Disable the export option for the coverage tab.
     *
     * @param evt
     */
    private void plotsTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_plotsTabbedPaneStateChanged
        exportPeptidesJButton.setEnabled(plotsTabbedPane.getSelectedIndex() != 2);
    }//GEN-LAST:event_plotsTabbedPaneStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contextMenuPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.ButtonGroup coveragePeptideTypesButtonGroup;
    private javax.swing.JRadioButtonMenuItem coverageShowAllPeptidesJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem;
    private javax.swing.JTable coverageTable;
    private javax.swing.JPanel coverageTablePanel;
    private javax.swing.JScrollPane coverageTableScrollPane;
    private javax.swing.JButton exportPeptidesJButton;
    private javax.swing.JButton exportProteinsJButton;
    private javax.swing.JPanel fractionsPlotPanel;
    private javax.swing.JPanel intensityPlotOuterPanel;
    private javax.swing.JPanel intensityPlotPanel;
    private javax.swing.JPanel mwPanel;
    private javax.swing.JPanel mwPlotPanel;
    private javax.swing.JPanel peptidePlotOuterPanel;
    private javax.swing.JPanel peptidePlotPanel;
    private javax.swing.JButton peptidesHelpJButton;
    private javax.swing.JLayeredPane plotsLayeredPane;
    private javax.swing.JPanel plotsPanel;
    private javax.swing.JTabbedPane plotsTabbedPane;
    private javax.swing.JPanel proteinPanel;
    private javax.swing.JSplitPane proteinPeptideSplitPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JScrollPane proteinTableScrollPane;
    private javax.swing.JButton proteinsHelpJButton;
    private javax.swing.JLayeredPane proteinsLayeredPane;
    private javax.swing.JPopupMenu sequenceCoverageJPopupMenu;
    private javax.swing.JButton sequenceCoverageOptionsJButton;
    private javax.swing.JPanel spectraPlotOuterPanel;
    private javax.swing.JPanel spectraPlotPanel;
    private javax.swing.JPanel totalPeptidesPerFractionPlotOuterPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);

        if (!peptideShakerGUI.getDisplayParameters().showScores()) {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        } else {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        }

        proteinTable.revalidate();
        proteinTable.repaint();
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected protein,
     * peptide and PSM.
     */
    public void newItemSelection() {

        long proteinKey = NO_KEY;
        long peptideKey = NO_KEY;
        String spectrumFile = null;
        String spectrumTitle = null;

        if (proteinTable.getSelectedRow() != -1) {

            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            int index = tableModel.getViewIndex(proteinTable.getSelectedRow());
            proteinKey = proteinKeys[index];

            // try to select the "best" peptide for the selected peptide
            peptideKey = peptideShakerGUI.getDefaultPeptideSelection(proteinKey);

            // try to select the "best" psm for the selected peptide
            if (peptideKey != NO_KEY) {

                long psmKey = peptideShakerGUI.getDefaultPsmSelection(peptideKey);
                
                SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(psmKey);
                spectrumFile = spectrumMatch.getSpectrumFile();
                spectrumTitle = spectrumMatch.getSpectrumTitle();

            }
        }

        peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, spectrumFile, spectrumTitle);
    }

    /**
     * Returns a list of keys of the displayed proteins.
     *
     * @return a list of keys of the displayed proteins
     */
    public long[] getDisplayedProteins() {
        return proteinKeys;
    }

    /**
     * Returns a list of keys of the displayed peptides.
     *
     * @return a list of keys of the displayed peptides
     */
    public long[] getDisplayedPeptides() {
        return peptideKeys;
    }

    /**
     * Export the table contents to the file.
     *
     * @param index the table index
     */
    private void copyTableContentToClipboardOrFile(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.PROTEIN_TABLE) {

            HashMap<String, ArrayList<ExportFeature>> exportFeatures = new HashMap<>();
            ArrayList<ExportFeature> sectionContent = new ArrayList<>();

            String textFileFilterDescription = "Tab separated text file (.txt)";
            String gzipFileFilterDescription = "Gzipped tab separated text file (.gz)";
            String excelFileFilterDescription = "Excel Workbook (.xls)";
            String lastSelectedFolderPath = peptideShakerGUI.getLastSelectedFolder().getLastSelectedFolder();

            FileAndFileFilter selectedFileAndFilter = FileChooserUtil.getUserSelectedFile(
                    this, 
                    new String[]{".xls", ".txt", ".gz"},
                    new String[]{excelFileFilterDescription, textFileFilterDescription, gzipFileFilterDescription}, 
                    "Export Report", 
                    lastSelectedFolderPath, 
                    "Protein table", 
                    false, 
                    true, 
                    false, 
                    1
            );

            if (selectedFileAndFilter != null) {

                final File selectedFile = selectedFileAndFilter.getFile();
                final ExportFormat exportFormat;
                final boolean gzip;
                if (selectedFileAndFilter.getFileFilter().getDescription().equalsIgnoreCase(textFileFilterDescription)) {
                    exportFormat = ExportFormat.text;
                    gzip = false;
                } else if (selectedFileAndFilter.getFileFilter().getDescription().equalsIgnoreCase(gzipFileFilterDescription)) {
                    exportFormat = ExportFormat.text;
                    gzip = true;
                } else {
                    exportFormat = ExportFormat.excel;
                    gzip = false;
                }

                progressDialog = new ProgressDialogX(
                        peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true
                );
                progressDialog.setTitle("Exporting Data. Please Wait...");

                final String filePath = selectedFile.getPath();

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            progressDialog.setVisible(true);
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    }
                }, "ProgressDialog").start();

                new Thread("ExportThread") {
                    @Override
                    public void run() {

                        try {

                            sectionContent.add(PsProteinFeature.starred);
                            sectionContent.add(PsProteinFeature.pi);
                            sectionContent.add(PsProteinFeature.accession);
                            sectionContent.add(PsProteinFeature.protein_description);
                            sectionContent.add(PsProteinFeature.protein_group);
                            sectionContent.add(PsProteinFeature.descriptions);
                            sectionContent.add(PsProteinFeature.other_proteins);
                            sectionContent.add(PsProteinFeature.chromosome);
                            sectionContent.add(PsProteinFeature.coverage);
                            sectionContent.add(PsProteinFeature.confident_coverage);
                            sectionContent.add(PsProteinFeature.all_coverage);
                            sectionContent.add(PsProteinFeature.possible_coverage);
                            sectionContent.add(PsProteinFeature.validated_peptides);
                            sectionContent.add(PsProteinFeature.peptides);
                            sectionContent.add(PsProteinFeature.unique_peptides);
                            sectionContent.add(PsProteinFeature.unique_validated_peptides);
                            sectionContent.add(PsProteinFeature.validated_psms);
                            sectionContent.add(PsProteinFeature.psms);
                            sectionContent.add(PsProteinFeature.spectrum_counting_nsaf);
                            sectionContent.add(PsProteinFeature.spectrum_counting_empai);
                            sectionContent.add(PsProteinFeature.spectrum_counting_nsaf_percent);
                            sectionContent.add(PsProteinFeature.spectrum_counting_empai_percent);
                            sectionContent.add(PsProteinFeature.spectrum_counting_nsaf_ppm);
                            sectionContent.add(PsProteinFeature.spectrum_counting_empai_ppm);
                            sectionContent.add(PsProteinFeature.spectrum_counting_nsaf_fmol);
                            sectionContent.add(PsProteinFeature.spectrum_counting_empai_fmol);
                            sectionContent.add(PsProteinFeature.mw);
                            sectionContent.add(PsProteinFeature.confidence);
                            sectionContent.add(PsProteinFeature.validated);
                            exportFeatures.put(PsProteinFeature.type, sectionContent);

                            ExportScheme validatedProteinReport = new ExportScheme(
                                    "Protein Table", 
                                    false, 
                                    exportFeatures, 
                                    "\t", 
                                    true, 
                                    true, 
                                    0, 
                                    false, 
                                    false, 
                                    false
                            );

                            PSExportFactory.writeExport(
                                    validatedProteinReport, 
                                    selectedFile, 
                                    exportFormat, 
                                    gzip, 
                                    peptideShakerGUI.getProjectParameters().getProjectUniqueName(),
                                    peptideShakerGUI.getProjectDetails(), 
                                    peptideShakerGUI.getIdentification(),
                                    peptideShakerGUI.getIdentificationFeaturesGenerator(), 
                                    peptideShakerGUI.getGeneMaps(), 
                                    getDisplayedProteins(), 
                                    null, 
                                    null,
                                    peptideShakerGUI.getDisplayParameters().getnAASurroundingPeptides(), 
                                    peptideShakerGUI.getIdentificationParameters(),
                                    peptideShakerGUI.getSequenceProvider(), 
                                    peptideShakerGUI.getProteinDetailsProvider(), 
                                    peptideShakerGUI.getSpectrumProvider(),
                                    peptideShakerGUI.getSpectrumCountingParameters(), 
                                    progressDialog
                            );

                            boolean processCancelled = progressDialog.isRunCanceled();
                            progressDialog.setRunFinished();

                            if (!processCancelled) {
                                JOptionPane.showMessageDialog(
                                        peptideShakerGUI, 
                                        "Data copied to file:\n" + filePath, 
                                        "Data Exported", 
                                        JOptionPane.INFORMATION_MESSAGE
                                );
                            }
                        } catch (IllegalArgumentException e) {
                            if (e.getMessage().contains("Invalid row number (65536)")) {
                                progressDialog.setRunFinished();
                                JOptionPane.showMessageDialog(
                                        peptideShakerGUI,
                                        "An error occurred while generating the output. This format can contain only 65,535 lines.\n" // @TODO: update the excel export library?
                                        + "Please use a text export instead.", 
                                        "Output Error", 
                                        JOptionPane.ERROR_MESSAGE
                                );
                                e.printStackTrace();
                            } else {
                                progressDialog.setRunFinished();
                                JOptionPane.showMessageDialog(
                                        peptideShakerGUI, 
                                        "An error occurred while generating the output.", 
                                        "Output Error", 
                                        JOptionPane.ERROR_MESSAGE
                                );
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            progressDialog.setRunFinished();
                            JOptionPane.showMessageDialog(
                                    peptideShakerGUI, 
                                    "An error occurred while generating the output.", 
                                    "Output Error", 
                                    JOptionPane.ERROR_MESSAGE
                            );
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    }

    @Override
    public void annotationClicked(ArrayList<ResidueAnnotation> allAnnotation, ChartMouseEvent cme) {
        // do nothing, not supported
    }

    /**
     * Update the protein table cell renderers.
     */
    private void updateProteinTableCellRenderers() {

        if (peptideShakerGUI.getIdentification() != null) {

            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNPeptides());
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNPsms());
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxSpectrumCounting());
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxMW());

            if (!peptideShakerGUI.getDisplayParameters().showScores()) {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);
            } else {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
            }
        }
    }

    /**
     * Hides or displays the score column in the protein table.
     */
    public void updateScores() {

        ((ProteinTableModel) proteinTable.getModel()).showScores(peptideShakerGUI.getDisplayParameters().showScores());
        ((DefaultTableModel) proteinTable.getModel()).fireTableStructureChanged();
        setProteinTableProperties();

        if (peptideShakerGUI.getSelectedTab() == PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX) {
            this.updateSelection();
        }

        if (peptideShakerGUI.getDisplayParameters().showScores()) {
            proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Score");
        } else {
            proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Confidence");
        }

        updateProteinTableCellRenderers();
    }

    /**
     * Deactivates the self updating tables.
     *
     * @param selfUpdating boolean indicating whether the tables should update
     * their content
     */
    public void selfUpdating(boolean selfUpdating) {
        if (proteinTable.getModel() instanceof SelfUpdatingTableModel) {
            ((SelfUpdatingTableModel) proteinTable.getModel()).setSelfUpdating(selfUpdating);
        }
    }
}
