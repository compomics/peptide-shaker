package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.gui.ExportGraphicsDialog;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanel;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanelParent;
import eu.isas.peptideshaker.gui.protein_sequence.ResidueAnnotation;
import eu.isas.peptideshaker.gui.tablemodels.PeptideFractionTableModel;
import eu.isas.peptideshaker.gui.tablemodels.ProteinFractionTableModel;
import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Displays information about which fractions the peptides and proteins were
 * detected in.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinFractionsPanel extends javax.swing.JPanel implements ProgressDialogParent, ProteinSequencePanelParent {

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
     * If true the progress bar is disposed of.
     */
    private static boolean cancelProgress = false;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

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

        Collections.sort(fileNames);

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

        for (int i = 3; i < proteinTable.getColumnCount() - 3; i++) {
            if (peptideShakerGUI.showSparklines()) {
                proteinTable.getColumn(proteinTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                        true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            } else {
                proteinTable.getColumn(proteinTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

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

        for (int i = 2; i < peptideTable.getColumnCount() - 2; i++) {
            if (peptideShakerGUI.showSparklines()) {
                peptideTable.getColumn(peptideTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                        true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            } else {
                peptideTable.getColumn(peptideTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

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

        progressDialog = new ProgressDialogX(this, true);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Loading Fractions. Please Wait...");
        progressDialog.setUnstoppable(true);

        new Thread(new Runnable() {

            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {

            @Override
            public void run() {

                // change the peptide shaker icon to a "waiting version"
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

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
                        dm.fireTableDataChanged();
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

                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                        progressDialog.dispose();
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

            ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();
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

            Collections.sort(fileNames);
            PSParameter pSParameter = new PSParameter();
            DefaultCategoryDataset peptidePlotDataset = new DefaultCategoryDataset();

            int row = proteinTable.getSelectedRow();
            int proteinIndex = proteinTable.convertRowIndexToModel(row);
            String proteinKey = proteinKeys.get(proteinIndex);
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            String currentProteinSequence = "";

            currentProteinSequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();

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
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }

                    if (pSParameter.getFractions() != null && pSParameter.getFractions().contains(fraction)) {
                        if (pSParameter.isValidated()) {
                            validatedPeptideCounter++;

                            String peptideSequence = Peptide.getSequence(peptideKey);
                            String tempSequence = currentProteinSequence;

                            while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                                int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                                int peptideTempEnd = peptideTempStart + peptideSequence.length();
                                for (int k = peptideTempStart; k < peptideTempEnd; k++) {
                                    coverage[i][k]++;
                                }
                                tempSequence = currentProteinSequence.substring(0, peptideTempStart);
                            }
                        } else {
                            notValidatedPeptideCounter++;
                        }
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



            // spectrum plot
            DefaultCategoryDataset spectrumPlotDataset = new DefaultCategoryDataset();

            // get the psms per fraction
            HashMap<String, ArrayList<String>> fractionPsmMatches = peptideShakerGUI.getMetrics().getFractionPsmMatches();

            for (int i = 0; i < fileNames.size(); i++) {
                try {

                    String fraction = fileNames.get(i);
                    int validatedSpectraCounter = 0;
                    int notValidatedSpectraCounter = 0;

                    for (int j = 0; j < peptideKeys.size(); j++) {

                        String currentPeptideKey = peptideKeys.get(j);

                        if (fractionPsmMatches.get(fraction + "_" + currentPeptideKey) != null) {
                            ArrayList<String> spectrumKeys = fractionPsmMatches.get(fraction + "_" + currentPeptideKey);

                            for (int k = 0; k < spectrumKeys.size(); k++) {
                                pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKeys.get(k), new PSParameter());

                                if (pSParameter.isValidated()) {
                                    validatedSpectraCounter++;
                                } else {
                                    notValidatedSpectraCounter++;
                                }
                            }
                        }
                    }

                    spectrumPlotDataset.addValue(validatedSpectraCounter, "Validated Spectra", "" + (i + 1));
                    spectrumPlotDataset.addValue(notValidatedSpectraCounter, "Not Validated Spectra", "" + (i + 1));
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            // create the peptide chart
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

        disclamierPanel = new javax.swing.JPanel();
        disclaimerLabel = new javax.swing.JLabel();
        proteinPeptideSplitPane = new javax.swing.JSplitPane();
        peptidesLayeredPane = new javax.swing.JLayeredPane();
        peptidePanel = new javax.swing.JPanel();
        peptidesTabbedPane = new javax.swing.JTabbedPane();
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

        peptidePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidePanel.setOpaque(false);

        peptidesTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        spectraPlotOuterPanel.setOpaque(false);

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
                .addComponent(spectraPlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesTabbedPane.addTab("Spectrum Plot", spectraPlotOuterPanel);

        peptidePlotOuterPanel.setOpaque(false);

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
                .addComponent(peptidePlotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesTabbedPane.addTab("Peptide Plot", peptidePlotOuterPanel);

        coverageTablePanel.setOpaque(false);

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
            .addGap(0, 153, Short.MAX_VALUE)
            .addGroup(coverageTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, coverageTablePanelLayout.createSequentialGroup()
                    .addComponent(coverageTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesTabbedPane.addTab("Coverage", coverageTablePanel);

        peptideTablePanel.setOpaque(false);

        peptideTable.setModel(new eu.isas.peptideshaker.gui.tablemodels.PeptideFractionTableModel());
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
            .addGap(0, 153, Short.MAX_VALUE)
            .addGroup(peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptideTablePanelLayout.createSequentialGroup()
                    .addComponent(peptideTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesTabbedPane.addTab("Peptide Table", peptideTablePanel);

        peptidesTabbedPane.setSelectedIndex(3);

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

        peptidePanel.setBounds(0, 0, 940, 230);
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
        peptidesHelpJButton.setBounds(930, 0, 10, 25);
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
        exportPeptidesJButton.setBounds(920, 0, 10, 25);
        peptidesLayeredPane.add(exportPeptidesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPeptidesBackgroundPanel);
        contextMenuPeptidesBackgroundPanel.setLayout(contextMenuPeptidesBackgroundPanelLayout);
        contextMenuPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPeptidesBackgroundPanel.setBounds(910, 0, 40, 20);
        peptidesLayeredPane.add(contextMenuPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        proteinPeptideSplitPane.setRightComponent(peptidesLayeredPane);

        proteinPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinPanel.setOpaque(false);

        proteinTable.setModel(new eu.isas.peptideshaker.gui.tablemodels.ProteinFractionTableModel());
        proteinTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
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
                .addComponent(proteinTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinPanel.setBounds(0, 0, 940, 240);
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
        proteinsHelpJButton.setBounds(930, 0, 10, 25);
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
        exportProteinsJButton.setBounds(920, 0, 10, 25);
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
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuProteinsBackgroundPanel.setBounds(910, 0, 40, 20);
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
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        peptidesLayeredPane.getComponent(2).getWidth(),
                        peptidesLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                peptidesLayeredPane.getComponent(3).setBounds(0, 0, peptidesLayeredPane.getWidth(), peptidesLayeredPane.getHeight());
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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contextMenuPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.JTable coverageTable;
    private javax.swing.JPanel coverageTablePanel;
    private javax.swing.JScrollPane coverageTableScrollPane;
    private javax.swing.JLabel disclaimerLabel;
    private javax.swing.JPanel disclamierPanel;
    private javax.swing.JButton exportPeptidesJButton;
    private javax.swing.JButton exportProteinsJButton;
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
    private javax.swing.JPanel spectraPlotOuterPanel;
    private javax.swing.JPanel spectraPlotPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        for (int i = 3; i < proteinTable.getColumnCount() - 3; i++) {
            if (peptideShakerGUI.showSparklines()) {
                proteinTable.getColumn(proteinTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                        true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            } else {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn(proteinTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

        try {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (NullPointerException e) {
            // ignore
        }

        for (int i = 2; i < peptideTable.getColumnCount() - 2; i++) {
            if (peptideShakerGUI.showSparklines()) {
                peptideTable.getColumn(peptideTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                        true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            } else {
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

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

            // change the peptide shaker icon to a "waiting version"
            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

            progressDialog = new ProgressDialogX(peptideShakerGUI, this, true);
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
                            Util.tableToFile(proteinTable, "\t", progressDialog, cancelProgress, true, writer);
                        } else if (tableIndex == TableIndex.PEPTIDE_TABLE) {
                            Util.tableToFile(peptideTable, "\t", progressDialog, cancelProgress, true, writer);
                        }

                        writer.close();

                        progressDialog.dispose();

                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                        if (!cancelProgress) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + selectedFile.getAbsolutePath(), "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (IOException e) {
                        progressDialog.dispose();
                        // change the peptide shaker icon back to the default version
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                        JOptionPane.showMessageDialog(null, "An error occured when exporting the table content.", "Export Failed", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }

                    cancelProgress = false;
                }
            }.start();


            // change the peptide shaker icon back to the default version
            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        }
    }

    @Override
    public void cancelProgress() {
        cancelProgress = true;
    }

    @Override
    public void annotationClicked(ArrayList<ResidueAnnotation> allAnnotation, ChartMouseEvent cme) {
        // do nothing, not supported
    }
}
