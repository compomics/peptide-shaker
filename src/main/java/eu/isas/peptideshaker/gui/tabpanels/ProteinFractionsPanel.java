package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.gui.ExportGraphicsDialog;
import eu.isas.peptideshaker.gui.FractionDetailsDialog;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanel;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanelParent;
import eu.isas.peptideshaker.gui.protein_sequence.ResidueAnnotation;
import eu.isas.peptideshaker.gui.tablemodels.PeptideFractionTableModel;
import eu.isas.peptideshaker.gui.tablemodels.ProteinFractionTableModel;
import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.extra.ChartPanelTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.util.GradientColorCoding;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

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
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of protein keys.
     */
    private ArrayList<String> proteinKeys;
    /**
     * A list of the peptides in the peptide table.
     */
    private ArrayList<String> peptideKeys = new ArrayList<String>();
    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The peptide table column header tooltips.
     */
    private ArrayList<String> peptideTableToolTips;
    /**
     * The coverage table column header tooltips.
     */
    private ArrayList<String> coverageTableToolTips;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * True if the fraction order has been okey'ed by the user.
     */
    private boolean fractionOrderSet = false;

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        PROTEIN_TABLE, PEPTIDE_TABLE
    };

    /**
     * Creates a new ProteinFractionsPanel.
     *
     * @param peptideShakerGUI
     */
    public ProteinFractionsPanel(PeptideShakerGUI peptideShakerGUI) {
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        setupGui();
        setTableProperties();
    }

    /**
     * Set up the GUI.
     */
    private void setupGui() {

        // make the tabs in the tabbed pane go from right to left
        peptidesTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // set main table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);

        // make sure that the scroll panes are see-through
        proteinTableScrollPane.getViewport().setOpaque(false);
        peptideTableScrollPane.getViewport().setOpaque(false);
        coverageTableScrollPane.getViewport().setOpaque(false);

        // set up the table header tooltips
        setUpTableHeaderToolTips();

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(proteinTable.getTableHeader().getBackground());
        proteinTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);
        JPanel peptideCorner = new JPanel();
        peptideCorner.setBackground(peptideTable.getTableHeader().getBackground());
        peptideTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, peptideCorner);

        addHeatMapGradientColors();

        chartTypeComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        dataTypeComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        gradientColorPanel.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
        gradientColorMinValueJLabel.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
        gradientColorMaxValueJLabel.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
        gradientSeparator.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
    }

    /**
     * Set up the table header tooltips.
     */
    private void setUpTableHeaderToolTips() {
        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");

        ArrayList<String> fileNames = new ArrayList<String>();

        for (String filePath : peptideShakerGUI.getSearchParameters().getSpectrumFiles()) {
            String fileName = Util.getFileName(filePath);
            fileNames.add(fileName);
        }

        for (int i = 0; i < fileNames.size(); i++) {
            proteinTableToolTips.add(fileNames.get(i));
        }

        proteinTableToolTips.add("Protein Molecular Weight (kDa)");
        proteinTableToolTips.add("Protein Confidence");
        proteinTableToolTips.add("Validated");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Peptide Sequence");

        for (int i = 0; i < fileNames.size(); i++) {
            peptideTableToolTips.add(fileNames.get(i));
        }

        peptideTableToolTips.add("Peptide Confidence");
        peptideTableToolTips.add("Validated");

        coverageTableToolTips = new ArrayList<String>();
        coverageTableToolTips.add(null);
        coverageTableToolTips.add("Fraction");
        coverageTableToolTips.add("Sequence Coverage");
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {
        setProteinTableProperties();
        setPeptideTableProperties();
        setCoverageTableProperties();
    }

    /**
     * Set up the properties of the protein table.
     */
    private void setProteinTableProperties() {

        // the index column
        proteinTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);

        proteinTable.getColumn("Confidence").setMaxWidth(90);
        proteinTable.getColumn("Confidence").setMinWidth(90);

        // the validated column
        proteinTable.getColumn("  ").setMaxWidth(30);
        proteinTable.getColumn("  ").setMinWidth(30);

        proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));

        proteinTable.getColumn("MW").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        proteinTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        updateProteinTableSparklines();

        // set the preferred size of the accession column
        Integer width = peptideShakerGUI.getPreferredAccessionColumnWidth(proteinTable, proteinTable.getColumn("Accession").getModelIndex(), 6);

        if (width != null) {
            proteinTable.getColumn("Accession").setMinWidth(width);
            proteinTable.getColumn("Accession").setMaxWidth(width);
        } else {
            proteinTable.getColumn("Accession").setMinWidth(15);
            proteinTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }

        // make sure that the user is made aware that the tool is doing something during the sorting of the protein table
        proteinTable.getRowSorter().addRowSorterListener(new RowSorterListener() {

            @Override
            public void sorterChanged(RowSorterEvent e) {
                if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    proteinTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
                } else if (e.getType() == RowSorterEvent.Type.SORTED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    proteinTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        });
    }

    /**
     * Set up the properties of the peptide table.
     */
    private void setPeptideTableProperties() {

        // the index column
        peptideTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);

        peptideTable.getColumn("Confidence").setMaxWidth(90);
        peptideTable.getColumn("Confidence").setMinWidth(90);

        // the validated column
        peptideTable.getColumn("").setMaxWidth(30);
        peptideTable.getColumn("").setMinWidth(30);

        peptideTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        peptideTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        updatePeptideTableSparklines();

        // make sure that the user is made aware that the tool is doing something during the sorting of the peptide table
        peptideTable.getRowSorter().addRowSorterListener(new RowSorterListener() {

            @Override
            public void sorterChanged(RowSorterEvent e) {
                if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    peptideTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
                } else if (e.getType() == RowSorterEvent.Type.SORTED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    peptideTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        });
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
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Loading Fractions. Please Wait...");
        progressDialog.setUnstoppable(true);

        SwingUtilities.invokeLater(new Runnable() {

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

                proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(progressDialog);

                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Loading Fractions. Please Wait...");

                // update the table model
                if (proteinTable.getModel() instanceof ProteinFractionTableModel
                        && ((ProteinFractionTableModel) proteinTable.getModel()).isModelInitiated()) {
                    ((ProteinFractionTableModel) proteinTable.getModel()).updateDataModel(peptideShakerGUI);
                } else {
                    ProteinFractionTableModel proteinTableModel = new ProteinFractionTableModel(peptideShakerGUI);
                    proteinTable.setModel(proteinTableModel);
                }

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        DefaultTableModel dm = (DefaultTableModel) proteinTable.getModel();
                        dm.fireTableStructureChanged();
                        updateSelection();
                        proteinTable.requestFocus();

                        setProteinTableProperties();
                        showSparkLines(peptideShakerGUI.showSparklines());
                        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxMW());
                        setUpTableHeaderToolTips();

                        peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, true);

                        // enable the contextual export options
                        exportProteinsJButton.setEnabled(true);
                        exportPeptidesJButton.setEnabled(true);

                        progressDialog.setRunFinished();
                    }
                });
            }
        }.start();
    }

    /**
     * Update the selection.
     */
    public void updatePeptideTable() {

        try {
            int row = proteinTable.getSelectedRow();
            int proteinIndex = proteinTable.convertRowIndexToModel(row);
            String proteinKey = proteinKeys.get(proteinIndex);
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            peptideKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getSortedPeptideKeys(proteinKey);

            // update the table model
            if (peptideTable.getModel() instanceof PeptideFractionTableModel
                    && ((PeptideFractionTableModel) peptideTable.getModel()).isModelInitiated()) {
                ((PeptideFractionTableModel) peptideTable.getModel()).updateDataModel(peptideShakerGUI, peptideKeys);
            } else {
                PeptideFractionTableModel peptideTableModel = new PeptideFractionTableModel(peptideShakerGUI, peptideKeys);
                peptideTable.setModel(peptideTableModel);
            }

            ((DefaultTableModel) peptideTable.getModel()).fireTableStructureChanged();
            setPeptideTableProperties();
            setCoverageTableProperties();
            showSparkLines(peptideShakerGUI.showSparklines());

            // update the border titles
            ((TitledBorder) proteinPanel.getBorder()).setTitle("Proteins ("
                    + peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedProteins() + "/" + proteinTable.getRowCount() + ")");
            proteinPanel.repaint();

            int nValidatedPeptides = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey);
            ((TitledBorder) peptidePanel.getBorder()).setTitle("Peptides (" + nValidatedPeptides + "/" + proteinMatch.getPeptideCount() + ")");
            peptidePanel.repaint();

            // select the first row in the table
            if (peptideTable.getRowCount() > 0) {
                peptideTable.setRowSelectionInterval(0, 0);
                updatePlots();
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            peptideKeys = new ArrayList<String>();
            ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();
        }
    }

    /**
     * Update the peptide counts plot.
     */
    private void updatePlots() {

        try {

            // @TODO: this method should be split into smaller methods...

            ArrayList<String> fileNames = new ArrayList<String>();

            for (String filePath : peptideShakerGUI.getSearchParameters().getSpectrumFiles()) {
                String fileName = Util.getFileName(filePath);
                fileNames.add(fileName);
            }

            PSParameter pSParameter = new PSParameter();
            DefaultCategoryDataset peptidePlotDataset = new DefaultCategoryDataset();

            int row = proteinTable.getSelectedRow();
            int proteinIndex = proteinTable.convertRowIndexToModel(row);
            String proteinKey = proteinKeys.get(proteinIndex);
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            String currentProteinSequence = "";

            try {
                currentProteinSequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int[][] coverage = new int[fileNames.size()][currentProteinSequence.length() + 1];


            // get the chart data
            for (int i = 0; i < fileNames.size(); i++) {

                String fraction = fileNames.get(i);
                int validatedPeptideCounter = 0;
                int notValidatedPeptideCounter = 0;

                for (int j = 0; j < peptideKeys.size(); j++) {

                    String peptideKey = peptideKeys.get(j);
                    try {
                        pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideKey, pSParameter);

                        if (pSParameter.getFractions() != null && pSParameter.getFractions().contains(fraction)) {
                            if (pSParameter.isValidated()) {
                                validatedPeptideCounter++;

                                String peptideSequence = Peptide.getSequence(peptideKey);
                                String tempSequence = currentProteinSequence;

                                boolean includePeptide = false;

                                if (coverageShowAllPeptidesJRadioButtonMenuItem.isSelected()) {
                                    includePeptide = true;
                                } else if (coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.isSelected()) {
                                    if (peptideSequence.endsWith("R") || peptideSequence.endsWith("K")) { // @TODO: this test should be made more generic!!!
                                        includePeptide = true;
                                    }
                                } else if (coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.isSelected()) {
                                    if (!peptideSequence.endsWith("R") && !peptideSequence.endsWith("K")) { // @TODO: this test should be made more generic!!!
                                        includePeptide = true;
                                    }
                                }

                                if (includePeptide) {

                                    while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                                        int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                                        int peptideTempEnd = peptideTempStart + peptideSequence.length();
                                        for (int k = peptideTempStart; k < peptideTempEnd; k++) {
                                            coverage[i][k]++;
                                        }
                                        tempSequence = currentProteinSequence.substring(0, peptideTempStart);
                                    }
                                }
                            } else {
                                notValidatedPeptideCounter++;
                            }
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                }

                peptidePlotDataset.addValue(validatedPeptideCounter, "Validated Peptides", "" + (i + 1));
                peptidePlotDataset.addValue(notValidatedPeptideCounter, "Not Validated Peptides", "" + (i + 1));
            }

            // update the coverage table
            DefaultTableModel coverageTableModel = (DefaultTableModel) coverageTable.getModel();
            coverageTableModel.getDataVector().removeAllElements();


            for (int i = 0; i < fileNames.size(); i++) {

                // create the coverage plot
                ArrayList<JSparklinesDataSeries> sparkLineDataSeriesCoverage = new ArrayList<JSparklinesDataSeries>();

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


                    ArrayList<Double> data = new ArrayList<Double>();
                    data.add(new Double(sequenceCounter));

                    JSparklinesDataSeries sparklineDataseries;

                    if (covered) {
                        sparklineDataseries = new JSparklinesDataSeries(data, peptideShakerGUI.getSparklineColor(), null);
                    } else {
                        sparklineDataseries = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                    }


                    sparkLineDataSeriesCoverage.add(sparklineDataseries);
                }

                ChartPanel coverageChart = new ProteinSequencePanel(Color.WHITE).getSequencePlot(this, new JSparklinesDataset(sparkLineDataSeriesCoverage),
                        new HashMap<Integer, ArrayList<ResidueAnnotation>>(), true, true);

                ((DefaultTableModel) coverageTable.getModel()).addRow(new Object[]{(i + 1), fileNames.get(i), coverageChart});
            }

            // create the peptide chart
            JFreeChart chart = ChartFactory.createBarChart(null, "Fraction", "#Peptides", peptidePlotDataset, PlotOrientation.VERTICAL, false, true, true);
            ChartPanel chartPanel = new ChartPanel(chart);

            // set up the renderer
            StackedBarRenderer renderer = new StackedBarRenderer();
            renderer.setShadowVisible(false);
            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
            renderer.setSeriesPaint(1, Color.RED);
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



            // spectrum and intensity plots
            DefaultCategoryDataset spectrumPlotDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset intensityPlotDataset = new DefaultCategoryDataset();

            // get the psms per fraction
            HashMap<String, ArrayList<String>> fractionPsmMatches = peptideShakerGUI.getMetrics().getFractionPsmMatches();

            for (int i = 0; i < fileNames.size(); i++) {

                String fraction = fileNames.get(i);
                int validatedSpectraCounter = 0;
                int notValidatedSpectraCounter = 0;
                double intensitySum = 0.0;
                int intensityCounter = 0;

                for (int j = 0; j < peptideKeys.size(); j++) {

                    String currentPeptideKey = peptideKeys.get(j);

                    if (fractionPsmMatches.get(fraction + "_" + currentPeptideKey) != null) {
                        ArrayList<String> spectrumKeys = fractionPsmMatches.get(fraction + "_" + currentPeptideKey);

                        for (int k = 0; k < spectrumKeys.size(); k++) {
                            pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKeys.get(k), new PSParameter());

                            if (pSParameter.isValidated()) {
                                validatedSpectraCounter++;
                                intensitySum += spectrumFactory.getPrecursor(spectrumKeys.get(k)).getIntensity();
                                intensityCounter++;
                            } else {
                                notValidatedSpectraCounter++;
                            }
                        }
                    }
                }

                spectrumPlotDataset.addValue(validatedSpectraCounter, "Validated Spectra", "" + (i + 1));
                spectrumPlotDataset.addValue(notValidatedSpectraCounter, "Not Validated Spectra", "" + (i + 1));

                if (intensitySum > 0) {
                    intensityPlotDataset.addValue(intensitySum / intensityCounter, "Total Intensity", "" + (i + 1));
                } else {
                    intensityPlotDataset.addValue(0.0, "Total Intensity", "" + (i + 1));
                }
            }

            // create the spectrum chart
            chart = ChartFactory.createBarChart(null, "Fraction", "#Spectra", spectrumPlotDataset, PlotOrientation.VERTICAL, false, true, true);
            chartPanel = new ChartPanel(chart);

            // set up the renderer
            renderer = new StackedBarRenderer();
            renderer.setShadowVisible(false);
            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
            renderer.setSeriesPaint(1, Color.RED);
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
            chart = ChartFactory.createBarChart(null, "Fraction", "Total Intensity", intensityPlotDataset, PlotOrientation.VERTICAL, false, true, true);
            chartPanel = new ChartPanel(chart);

            // set up the renderer
            renderer = new StackedBarRenderer();
            renderer.setShadowVisible(false);
            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            renderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
            renderer.setSeriesPaint(1, Color.RED);
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


            // molecular mass plot
            DefaultCategoryDataset mwPlotDataset = new DefaultCategoryDataset();

            HashMap<String, Double> molecularWeights = peptideShakerGUI.getSearchParameters().getFractionMolecularWeights();
            ArrayList<String> spectrumFiles = peptideShakerGUI.getSearchParameters().getSpectrumFiles();

            for (int i = 0; i < spectrumFiles.size(); i++) {

                Double mw = null;

                if (molecularWeights != null) {
                    mw = molecularWeights.get(spectrumFiles.get(i));
                }

                mwPlotDataset.addValue(mw, "Expected MW", "" + (i + 1));
                mwPlotDataset.addValue(peptideShakerGUI.getMetrics().getObservedFractionalMasses().get(Util.getFileName(spectrumFiles.get(i))), "Observed MW", "" + (i + 1));
            }

            // create the mw chart
            chart = ChartFactory.createBarChart(null, "Fraction", "Expected MW", mwPlotDataset, PlotOrientation.VERTICAL, false, true, true);
            chartPanel = new ChartPanel(chart);

            // set up the renderer
            BarRenderer barRenderer = new BarRenderer();
            barRenderer.setShadowVisible(false);
            barRenderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            barRenderer.setSeriesPaint(0, peptideShakerGUI.getSparklineColor());
            barRenderer.setSeriesPaint(1, Color.RED);
            chart.getCategoryPlot().setRenderer(barRenderer);

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

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Update the selected protein and peptide.
     */
    public void updateSelection() {

        int proteinRow = 0;
        String proteinKey = peptideShakerGUI.getSelectedProteinKey();
        String peptideKey = peptideShakerGUI.getSelectedPeptideKey();
        String psmKey = peptideShakerGUI.getSelectedPsmKey();

        if (proteinKey.equals(PeptideShakerGUI.NO_SELECTION)
                && peptideKey.equals(PeptideShakerGUI.NO_SELECTION)
                && !psmKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            if (peptideShakerGUI.getIdentification().matchExists(psmKey)) {
                try {
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(psmKey);
                    peptideKey = spectrumMatch.getBestAssumption().getPeptide().getKey();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    return;
                }
            } else {
                peptideShakerGUI.resetSelectedItems();
            }
        }

        if (proteinKey.equals(PeptideShakerGUI.NO_SELECTION)
                && !peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            for (String possibleKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
                try {
                    ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(possibleKey);
                    if (proteinMatch.getPeptideMatches().contains(peptideKey)) {
                        proteinKey = possibleKey;
                        peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
                        break;
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    return;
                }
            }
        }

        if (!proteinKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            proteinRow = getProteinRow(proteinKey);
        }

        if (proteinKeys.isEmpty()) {
            // For the silly people like me who happen to hide all proteins
            clearData();
            return;
        }

        if (proteinRow == -1) {
            peptideShakerGUI.resetSelectedItems();
        } else if (proteinTable.getSelectedRow() != proteinRow) {
            proteinTable.setRowSelectionInterval(proteinRow, proteinRow);
            proteinTable.scrollRectToVisible(proteinTable.getCellRect(proteinRow, 0, false));
            updatePeptideTable();
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                int peptideRow = 0;
                String peptideKey = peptideShakerGUI.getSelectedPeptideKey();
                if (!peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                    peptideRow = getPeptideRow(peptideKey);
                }

                if (peptideTable.getSelectedRow() != peptideRow && peptideRow != -1) {
                    peptideTable.setRowSelectionInterval(peptideRow, peptideRow);
                    peptideTable.scrollRectToVisible(peptideTable.getCellRect(peptideRow, 0, false));
                    //peptideTableMouseReleased(null);
                }
            }
        });
    }

    /**
     * Returns the row of a desired protein.
     *
     * @param proteinKey the key of the protein
     * @return the row of the desired protein
     */
    private int getProteinRow(String proteinKey) {
        int modelIndex = proteinKeys.indexOf(proteinKey);
        if (modelIndex >= 0) {
            return proteinTable.convertRowIndexToView(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * Returns the row of a desired peptide.
     *
     * @param peptideKey the key of the peptide
     * @return the row of the desired peptide
     */
    private int getPeptideRow(String peptideKey) {
        int modelIndex = peptideKeys.indexOf(peptideKey);
        if (modelIndex >= 0) {
            return peptideTable.convertRowIndexToView(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * Clear all the data.
     */
    public void clearData() {

        proteinKeys.clear();
        peptideKeys.clear();

        DefaultTableModel peptideTableModel = (DefaultTableModel) peptideTable.getModel();
        peptideTableModel.getDataVector().removeAllElements();
        ProteinTableModel proteinTableModel = (ProteinTableModel) proteinTable.getModel();
        proteinTableModel.reset();

        peptideTableModel.fireTableDataChanged();
        proteinTableModel.fireTableDataChanged();

        ((TitledBorder) proteinPanel.getBorder()).setTitle("Proteins");
        proteinPanel.repaint();
        ((TitledBorder) peptidePanel.getBorder()).setTitle("Peptides");
        peptidePanel.repaint();
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
        disclamierPanel = new javax.swing.JPanel();
        disclaimerLabel = new javax.swing.JLabel();
        proteinPeptideSplitPane = new javax.swing.JSplitPane();
        peptidesLayeredPane = new javax.swing.JLayeredPane();
        peptidePanel = new javax.swing.JPanel();
        peptidesTabbedPane = new javax.swing.JTabbedPane();
        intensityPlotOuterPanel = new javax.swing.JPanel();
        intensityPlotPanel = new javax.swing.JPanel();
        spectraPlotOuterPanel = new javax.swing.JPanel();
        spectraPlotPanel = new javax.swing.JPanel();
        peptidePlotOuterPanel = new javax.swing.JPanel();
        peptidePlotPanel = new javax.swing.JPanel();
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
        mwPanel = new javax.swing.JPanel();
        mwPlotPanel = new javax.swing.JPanel();
        peptideTablePanel = new javax.swing.JPanel();
        peptideTableScrollPane = new javax.swing.JScrollPane();
        peptideTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) peptideTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
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
        jSeparator1 = new javax.swing.JSeparator();
        gradientColorMinValueJLabel = new javax.swing.JLabel();
        gradientColorPanel = new javax.swing.JPanel();
        gradientColorMaxValueJLabel = new javax.swing.JLabel();
        gradientSeparator = new javax.swing.JSeparator();
        chartTypeComboBox = new javax.swing.JComboBox();
        dataTypeComboBox = new javax.swing.JComboBox();
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
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.setText("Truncated Peptides");
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

        disclamierPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Disclaimer"));
        disclamierPanel.setOpaque(false);

        disclaimerLabel.setText("<html>\nThe values above are estimations of the confidence of a peptide/protein if found in a fraction alone in the context of the whole analysis.<br>\n<i>These are <u><b>not</b></u> equal the confidence in the peptide/protein identifications when processing the fractions independently!</i><br><br>\nIndependant fractions (like different donors, measurements) or replicates should be processed separately.<br>\nTo ensure comparable fractions, verify that the PSM PEP against score plots are similar for all the fractions in the <i>Validation</i> tab.\n</html>");

        javax.swing.GroupLayout disclamierPanelLayout = new javax.swing.GroupLayout(disclamierPanel);
        disclamierPanel.setLayout(disclamierPanelLayout);
        disclamierPanelLayout.setHorizontalGroup(
            disclamierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(disclamierPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(disclaimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(268, Short.MAX_VALUE))
        );
        disclamierPanelLayout.setVerticalGroup(
            disclamierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(disclamierPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(disclaimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        proteinPeptideSplitPane.setBorder(null);
        proteinPeptideSplitPane.setDividerLocation(200);
        proteinPeptideSplitPane.setDividerSize(0);
        proteinPeptideSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        proteinPeptideSplitPane.setResizeWeight(0.5);
        proteinPeptideSplitPane.setOpaque(false);

        peptidePanel.setBackground(new java.awt.Color(255, 255, 255));
        peptidePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));

        peptidesTabbedPane.setBackground(new java.awt.Color(255, 255, 255));
        peptidesTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

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
                .addComponent(intensityPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesTabbedPane.addTab("Intensity Plot", intensityPlotOuterPanel);

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
                .addComponent(spectraPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesTabbedPane.addTab("Spectrum Plot", spectraPlotOuterPanel);

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
                .addComponent(peptidePlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesTabbedPane.addTab("Peptide Plot", peptidePlotOuterPanel);

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
            .addGap(0, 203, Short.MAX_VALUE)
            .addGroup(coverageTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, coverageTablePanelLayout.createSequentialGroup()
                    .addComponent(coverageTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesTabbedPane.addTab("Coverage", coverageTablePanel);

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
                .addComponent(mwPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesTabbedPane.addTab("Molecular Weight", mwPanel);

        peptideTablePanel.setBackground(new java.awt.Color(255, 255, 255));

        peptideTableScrollPane.setOpaque(false);

        peptideTable.setModel(new eu.isas.peptideshaker.gui.tablemodels.PeptideFractionTableModel());
        peptideTable.setOpaque(false);
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideTableScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout peptideTablePanelLayout = new javax.swing.GroupLayout(peptideTablePanel);
        peptideTablePanel.setLayout(peptideTablePanelLayout);
        peptideTablePanelLayout.setHorizontalGroup(
            peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 903, Short.MAX_VALUE)
            .addGroup(peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(peptideTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE))
        );
        peptideTablePanelLayout.setVerticalGroup(
            peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 203, Short.MAX_VALUE)
            .addGroup(peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptideTablePanelLayout.createSequentialGroup()
                    .addComponent(peptideTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesTabbedPane.addTab("Peptide Table", peptideTablePanel);

        peptidesTabbedPane.setSelectedIndex(5);

        javax.swing.GroupLayout peptidePanelLayout = new javax.swing.GroupLayout(peptidePanel);
        peptidePanel.setLayout(peptidePanelLayout);
        peptidePanelLayout.setHorizontalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesTabbedPane)
                .addContainerGap())
        );
        peptidePanelLayout.setVerticalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesTabbedPane)
                .addContainerGap())
        );

        peptidePanel.setBounds(0, 0, 940, 280);
        peptidesLayeredPane.add(peptidePanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        peptidesLayeredPane.add(peptidesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        peptidesLayeredPane.add(exportPeptidesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        peptidesLayeredPane.add(sequenceCoverageOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        peptidesLayeredPane.add(contextMenuPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        proteinPeptideSplitPane.setRightComponent(peptidesLayeredPane);

        proteinPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinPanel.setOpaque(false);

        proteinTableScrollPane.setOpaque(false);

        proteinTable.setModel(new eu.isas.peptideshaker.gui.tablemodels.ProteinFractionTableModel());
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

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        gradientColorMinValueJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gradientColorMinValueJLabel.setText("0%");

        gradientColorPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        gradientColorPanel.setToolTipText("Heat Map Color Coding");
        gradientColorPanel.setLayout(new java.awt.BorderLayout());

        gradientColorMaxValueJLabel.setText("100%");

        gradientSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);

        chartTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Bar Chart", "Heat Map" }));
        chartTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chartTypeComboBoxActionPerformed(evt);
            }
        });

        dataTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Confidence", "#Peptides", "#Spectra", "Intensity" }));
        dataTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataTypeComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout proteinPanelLayout = new javax.swing.GroupLayout(proteinPanel);
        proteinPanel.setLayout(proteinPanelLayout);
        proteinPanelLayout.setHorizontalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(proteinTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 908, Short.MAX_VALUE)
                    .addGroup(proteinPanelLayout.createSequentialGroup()
                        .addComponent(gradientColorMinValueJLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gradientColorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gradientColorMaxValueJLabel)
                        .addGap(18, 18, 18)
                        .addComponent(gradientSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chartTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dataTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        proteinPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {gradientColorMaxValueJLabel, gradientColorMinValueJLabel});

        proteinPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {chartTypeComboBox, dataTypeComboBox, gradientColorPanel});

        proteinPanelLayout.setVerticalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(gradientColorMinValueJLabel)
                    .addComponent(gradientColorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gradientColorMaxValueJLabel)
                    .addComponent(gradientSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chartTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dataTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disclamierPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proteinPeptideSplitPane))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinPeptideSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 481, Short.MAX_VALUE)
                .addGap(7, 7, 7)
                .addComponent(disclamierPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the selection.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased
        updatePeptideTable();

        int row = proteinTable.getSelectedRow();
        int proteinIndex = proteinTable.convertRowIndexToModel(row);
        int column = proteinTable.getSelectedColumn();

        if (proteinIndex != -1) {
            // remember the selection
            newItemSelection();
        }

        if (evt != null && evt.getButton() == MouseEvent.BUTTON1 && proteinIndex != -1) {

            // open protein link in web browser
            if (column == proteinTable.getColumn("Accession").getModelIndex()
                    && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) proteinTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Update the selection.
     *
     * @param evt
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased
        updatePeptideTable();

        int row = proteinTable.getSelectedRow();
        int proteinIndex = proteinTable.convertRowIndexToModel(row);

        if (proteinIndex != -1) {
            // remember the selection
            newItemSelection();
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

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinTable.getColumn("Description").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            if (peptideShakerGUI.getPreferredWidthOfCell(proteinTable, row, column) > proteinTable.getColumn("Description").getWidth()) {
                proteinTable.setToolTipText("" + proteinTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Remember the selection.
     *
     * @param evt
     */
    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased
        // remember the selection
        newItemSelection();
    }//GEN-LAST:event_peptideTableMouseReleased

    /**
     * Remember the selection.
     *
     * @param evt
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased
        // remember the selection
        newItemSelection();
    }//GEN-LAST:event_peptideTableKeyReleased

    /**
     * Update the layered panes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        proteinPeptideSplitPane.setDividerLocation(0.5);

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
                peptidesLayeredPane.getComponent(0).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        peptidesLayeredPane.getComponent(0).getWidth(),
                        peptidesLayeredPane.getComponent(0).getHeight());

                peptidesLayeredPane.getComponent(1).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        peptidesLayeredPane.getComponent(1).getWidth(),
                        peptidesLayeredPane.getComponent(1).getHeight());

                peptidesLayeredPane.getComponent(2).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(2).getWidth() - 34,
                        0,
                        peptidesLayeredPane.getComponent(2).getWidth(),
                        peptidesLayeredPane.getComponent(2).getHeight());

                peptidesLayeredPane.getComponent(3).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        peptidesLayeredPane.getComponent(3).getWidth(),
                        peptidesLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                peptidesLayeredPane.getComponent(4).setBounds(0, 0, peptidesLayeredPane.getWidth(), peptidesLayeredPane.getHeight());
                peptidesLayeredPane.revalidate();
                peptidesLayeredPane.repaint();
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FractionsTab.html"));
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FractionsTab.html"), "#Peptides");
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
        if (peptidesTabbedPane.getSelectedIndex() == 0) {
            new ExportGraphicsDialog(peptideShakerGUI, true, (ChartPanel) peptidePlotPanel.getComponent(0));
        } else {
            copyTableContentToClipboardOrFile(TableIndex.PEPTIDE_TABLE);
        }
    }//GEN-LAST:event_exportPeptidesJButtonActionPerformed

    /**
     * Update the data type in the protein table.
     *
     * @param evt
     */
    private void chartTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chartTypeComboBoxActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updateProteinTableSparklines();
            updatePeptideTableSparklines();
            proteinTable.revalidate();
            proteinTable.repaint();
            peptideTable.revalidate();
            peptideTable.repaint();
        }

        gradientColorPanel.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
        gradientColorMinValueJLabel.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
        gradientColorMaxValueJLabel.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
        gradientSeparator.setVisible(chartTypeComboBox.getSelectedIndex() == 1);
    }//GEN-LAST:event_chartTypeComboBoxActionPerformed

    /**
     * Update the data type in the protein table.
     *
     * @param evt
     */
    private void dataTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dataTypeComboBoxActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updateProteinTableSparklines();
            updatePeptideTableSparklines();
            proteinTable.revalidate();
            proteinTable.repaint();
            peptideTable.revalidate();
            peptideTable.repaint();
        }
    }//GEN-LAST:event_dataTypeComboBoxActionPerformed

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
            try {
                updatePlots();
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox chartTypeComboBox;
    private javax.swing.JPanel contextMenuPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.ButtonGroup coveragePeptideTypesButtonGroup;
    private javax.swing.JRadioButtonMenuItem coverageShowAllPeptidesJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem;
    private javax.swing.JTable coverageTable;
    private javax.swing.JPanel coverageTablePanel;
    private javax.swing.JScrollPane coverageTableScrollPane;
    private javax.swing.JComboBox dataTypeComboBox;
    private javax.swing.JLabel disclaimerLabel;
    private javax.swing.JPanel disclamierPanel;
    private javax.swing.JButton exportPeptidesJButton;
    private javax.swing.JButton exportProteinsJButton;
    private javax.swing.JLabel gradientColorMaxValueJLabel;
    private javax.swing.JLabel gradientColorMinValueJLabel;
    private javax.swing.JPanel gradientColorPanel;
    private javax.swing.JSeparator gradientSeparator;
    private javax.swing.JPanel intensityPlotOuterPanel;
    private javax.swing.JPanel intensityPlotPanel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel mwPanel;
    private javax.swing.JPanel mwPlotPanel;
    private javax.swing.JPanel peptidePanel;
    private javax.swing.JPanel peptidePlotOuterPanel;
    private javax.swing.JPanel peptidePlotPanel;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel peptideTablePanel;
    private javax.swing.JScrollPane peptideTableScrollPane;
    private javax.swing.JButton peptidesHelpJButton;
    private javax.swing.JLayeredPane peptidesLayeredPane;
    private javax.swing.JTabbedPane peptidesTabbedPane;
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
    // End of variables declaration//GEN-END:variables

    /**
     * Returns true if protein confidence is selected as the data type.
     *
     * @return true if protein confidence is selected as the data type
     */
    public boolean isProteinConfidenceSelected() {
        return dataTypeComboBox.getSelectedIndex() == 0;
    }

    /**
     * Returns true if peptide count is selected as the data type.
     *
     * @return true if peptide count is selected as the data type
     */
    public boolean isProteinPeptideCountSelected() {
        return dataTypeComboBox.getSelectedIndex() == 1;
    }

    /**
     * Returns true if spectrum count is selected as the data type.
     *
     * @return true if spectrum count is selected as the data type
     */
    public boolean isProteinSpectumCountSelected() {
        return dataTypeComboBox.getSelectedIndex() == 2;
    }

    /**
     * Returns true if intensity is selected as the data type.
     *
     * @return true if intensityis selected as the data type
     */
    public boolean isIntensitySelected() {
        return dataTypeComboBox.getSelectedIndex() == 3;
    }

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        updateProteinTableSparklines();

        try {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (NullPointerException e) {
            // ignore
        }

        updatePeptideTableSparklines();

        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);


        proteinTable.revalidate();
        proteinTable.repaint();

        peptideTable.revalidate();
        peptideTable.repaint();
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected protein,
     * peptide and psm.
     */
    public void newItemSelection() {

        String proteinKey = PeptideShakerGUI.NO_SELECTION;
        String peptideKey = PeptideShakerGUI.NO_SELECTION;
        String psmKey = PeptideShakerGUI.NO_SELECTION;

        if (proteinTable.getSelectedRow() != -1) {
            proteinKey = proteinKeys.get(proteinTable.convertRowIndexToModel(proteinTable.getSelectedRow()));
        }
        if (peptideTable.getSelectedRow() != -1) {
            peptideKey = peptideKeys.get(peptideTable.convertRowIndexToModel(peptideTable.getSelectedRow()));
        }

        peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
    }

    /**
     * Returns a list of keys of the displayed proteins.
     *
     * @return a list of keys of the displayed proteins
     */
    public ArrayList<String> getDisplayedProteins() {
        return proteinKeys;
    }

    /**
     * Returns a list of keys of the displayed peptides.
     *
     * @return a list of keys of the displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        return peptideKeys;
    }

    /**
     * Export the table contents to the clipboard.
     *
     * @param index
     */
    private void copyTableContentToClipboardOrFile(TableIndex index) {

        final TableIndex tableIndex = index;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);

        if (selectedFile != null) {

            progressDialog = new ProgressDialogX(peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setIndeterminate(true);

            new Thread(new Runnable() {

                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore error
                    }
                    progressDialog.setTitle("Exporting to File. Please Wait...");
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {

                @Override
                public void run() {

                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));

                        if (tableIndex == TableIndex.PROTEIN_TABLE) {
                            Util.tableToFile(proteinTable, "\t", progressDialog, true, writer);
                        } else if (tableIndex == TableIndex.PEPTIDE_TABLE) {
                            Util.tableToFile(peptideTable, "\t", progressDialog, true, writer);
                        }

                        writer.close();

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + selectedFile.getAbsolutePath(), "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (IOException e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(null, "An error occured when exporting the table content.", "Export Failed", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    @Override
    public void annotationClicked(ArrayList<ResidueAnnotation> allAnnotation, ChartMouseEvent cme) {
        // do nothing, not supported
    }

    /**
     * Returns the list of peptide keys.
     *
     * @return the list of peptide keys.
     */
    public ArrayList<String> getPeptideKeys() {
        return peptideKeys;
    }

    /**
     * Updates the protein table sparklines.
     */
    private void updateProteinTableSparklines() {

        double maxValue = 100;

        if (isProteinPeptideCountSelected()) {
            maxValue = peptideShakerGUI.getMetrics().getMaxNPeptides();
        } else if (isProteinSpectumCountSelected()) {
            maxValue = peptideShakerGUI.getMetrics().getMaxNSpectra();
        } else if (isIntensitySelected()) {
            maxValue = spectrumFactory.getMaxIntensity();
        }

        for (int i = 3; i < proteinTable.getColumnCount() - 3; i++) {

            proteinTable.getColumn(proteinTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxValue, peptideShakerGUI.getSparklineColor()));

            if (peptideShakerGUI.showSparklines()) {
                if (chartTypeComboBox.getSelectedIndex() == 0) { // bar chart
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                            true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
                } else { // heat map
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showAsHeatMap(GradientColorCoding.ColorGradient.GreenWhiteBlue);
                }
            } else {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }
    }

    /**
     * Updates the peptide table sparklines.
     */
    private void updatePeptideTableSparklines() {

        double maxValue = 100;

        for (int i = 2; i < peptideTable.getColumnCount() - 2; i++) {

            peptideTable.getColumn(peptideTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxValue, peptideShakerGUI.getSparklineColor()));

            if (peptideShakerGUI.showSparklines()) {
                if (chartTypeComboBox.getSelectedIndex() == 0) { // bar chart
                    ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                            true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
                } else { // heat map
                    ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showAsHeatMap(GradientColorCoding.ColorGradient.GreenWhiteBlue);
                }
            } else {
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }
    }

    /**
     * Show the heat map gradient color coding..
     */
    private void addHeatMapGradientColors() {

        final Color startColor = Color.WHITE;
        final Color endColor = Color.BLUE;

        JPanel gradientJPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, getHeight() / 2,
                        startColor,
                        getWidth(), getHeight() / 2,
                        endColor);

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }
        };

        gradientJPanel.setOpaque(false);
        gradientColorPanel.add(gradientJPanel);
    }
}
