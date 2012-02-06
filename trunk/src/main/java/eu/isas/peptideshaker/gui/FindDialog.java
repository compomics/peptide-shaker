package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * The main filter dialog.
 * 
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class FindDialog extends javax.swing.JDialog {

    /**
     * The main GUI instance
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The current protein filter
     */
    private ProteinFilter proteinFilter = null;
    /**
     * The current peptide filter
     */
    private PeptideFilter peptideFilter = null;
    /**
     * The current PSM filter
     */
    private PsmFilter psmFilter = null;
    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The peptide table column header tooltips.
     */
    private ArrayList<String> peptideTableToolTips;
    /**
     * The psm table column header tooltips.
     */
    private ArrayList<String> psmTableToolTips;
    /**
     * The modifications table column header tooltips.
     */
    private ArrayList<String> ptmTableToolTips;
    /**
     * The spectrum files table column header tooltips.
     */
    private ArrayList<String> spectrumFilesTableToolTips;
    /**
     * The current filter type.
     */
    private FilterType currentFilterType = FilterType.STAR;

    /**
     * The supported filter types.
     */
    public enum FilterType {

        STAR, HIDE
    }
    /**
     * A reference to the filter selection dialog.
     */
    private FiltersDialog filterDialog;

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI the main GUI
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI) {
        new FindDialog(peptideShakerGUI, null, null, null, null, 0, FilterType.STAR);
    }

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI  the main GUI
     * @param filterDialog      a reference to the filter selection dialog
     * @param selectedTab       the tab to select
     * @param filterType        the current filter type 
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI, FiltersDialog filterDialog, int selectedTab, FilterType filterType) {
        new FindDialog(peptideShakerGUI, filterDialog, null, null, null, selectedTab, filterType);
    }

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI  the main GUI
     * @param filterDialog      a reference to the filter selection dialog
     * @param proteinFilter     the protein filter to edit (can be null)
     * @param peptideFilter     the peptide filter to edit (can be null)
     * @param psmFilter         the psm filter to edit (can be null)
     * @param filterType        the current filter type 
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI, FiltersDialog filterDialog, ProteinFilter proteinFilter, PeptideFilter peptideFilter, PsmFilter psmFilter, FilterType filterType) {
        new FindDialog(peptideShakerGUI, filterDialog, proteinFilter, peptideFilter, psmFilter, 0, filterType);
    }

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI  the main GUI
     * @param filterDialog      a reference to the filter selection dialog
     * @param proteinFilter     the protein filter to edit (can be null)
     * @param peptideFilter     the peptide filter to edit (can be null)
     * @param psmFilter         the psm filter to edit (can be null)
     * @param selectedTab       the tab to select
     * @param filterType        the current filter type 
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI, FiltersDialog filterDialog, ProteinFilter proteinFilter, PeptideFilter peptideFilter, PsmFilter psmFilter, int selectedTab, FilterType filterType) {
        super(peptideShakerGUI, true);
        this.peptideShakerGUI = peptideShakerGUI;
        this.currentFilterType = filterType;
        identification = peptideShakerGUI.getIdentification();
        this.filterDialog = filterDialog;

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        initComponents();

        proteinPICmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinPiComparisonCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        spectrumCountingCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinCoverageCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        nPeptidesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinNSpectraCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinScoreCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinConfidenceCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        peptidePICmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptidePiComparisonCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptideNSpectraCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptideScoreCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptideConfidenceCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        precursorRTCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        precursorMzCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        precursorErrorCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        psmConfidenceCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        proteinSplitPane.setDividerLocation(0.5);
        peptidesSplitPane.setDividerLocation(0.5);
        psmSplitPane.setDividerLocation(0.5);

        // make sure that the scroll panes are see-through
        proteinScrollPane.getViewport().setOpaque(false);
        modificationsScrollPane.getViewport().setOpaque(false);
        filesScrollPane.getViewport().setOpaque(false);
        peptidesScrollPane.getViewport().setOpaque(false);
        psmTableScrollPane.getViewport().setOpaque(false);

        // set table properties
        setUpTables();

        for (String ptm : peptideShakerGUI.getFoundModifications()) {

            if (ptm.equalsIgnoreCase(PtmPanel.NO_MODIFICATION)) {
                ((DefaultTableModel) modificationTable.getModel()).addRow(new Object[]{
                            true,
                            Color.lightGray,
                            PtmPanel.NO_MODIFICATION});
            } else {
                ((DefaultTableModel) modificationTable.getModel()).addRow(new Object[]{
                            true,
                            peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(ptm),
                            ptm});
            }
        }

        for (String file : peptideShakerGUI.getSearchParameters().getSpectrumFiles()) {
            ((DefaultTableModel) spectrumFilesTable.getModel()).addRow(new Object[]{
                        true,
                        Util.getFileName(file)
                    });
        }

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmTable.setAutoCreateRowSorter(true);

        this.psmFilter = psmFilter;
        this.peptideFilter = peptideFilter;
        this.proteinFilter = proteinFilter;

        if (proteinFilter != null) {
            filterTypeJTabbedPane.setSelectedIndex(0);
        } else if (peptideFilter != null) {
            filterTypeJTabbedPane.setSelectedIndex(1);
        } else if (psmFilter != null) {
            filterTypeJTabbedPane.setSelectedIndex(2);
        } else {
            filterTypeJTabbedPane.setSelectedIndex(selectedTab);
        }

        if (proteinFilter != null) {
            fillProteinTab();
            filterProteins();
        }

        if (peptideFilter != null) {
            fillPeptideTab();
            filterPeptides();
        }

        if (psmFilter != null) {
            fillPsmTab();
            filterPsms();
        }


        // @TODO: the max precursor m/z ought to be stored in the same way as max charge etc

        // find the max m/z-value
        double maxMzValue = Double.MIN_VALUE;

        for (int i = 0; i < psmTable.getRowCount(); i++) {
            if (maxMzValue < (Double) psmTable.getValueAt(i, psmTable.getColumn("m/z").getModelIndex())) {
                maxMzValue = (Double) psmTable.getValueAt(i, psmTable.getColumn("m/z").getModelIndex());
            }
        }

        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("m/z").getCellRenderer()).setMaxValue(maxMzValue);


        ((TitledBorder) proteinTablePanel.getBorder()).setTitle("Filtered Proteins (" + proteinTable.getRowCount() + ")");
        proteinTablePanel.revalidate();
        proteinTablePanel.repaint();

        ((TitledBorder) peptideTablePanel.getBorder()).setTitle("Filtered Peptides (" + peptideTable.getRowCount() + ")");
        peptideTablePanel.revalidate();
        peptideTablePanel.repaint();

        ((TitledBorder) psmTablePanel.getBorder()).setTitle("Filtered PSMs (" + psmTable.getRowCount() + ")");
        psmTablePanel.revalidate();
        psmTablePanel.repaint();

        peptideTable.revalidate();
        peptideTable.repaint();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Setup the tables.
     */
    private void setUpTables() {
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peptideTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        psmTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmTable.setAutoCreateRowSorter(true);

        // set up the table header tooltips
        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Starred");
        proteinTableToolTips.add("Hidden");
        proteinTableToolTips.add("Protein Inference Class");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Isoforms");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Protein Coverage (%)");
        proteinTableToolTips.add("Number of Peptides");
        proteinTableToolTips.add("Number of Spectra");
        proteinTableToolTips.add("Protein MS2 Quantification");
        proteinTableToolTips.add("Protein Score");
        proteinTableToolTips.add("Protein Confidence");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Starred");
        peptideTableToolTips.add("Hidden");
        peptideTableToolTips.add("Protein Inference Class");
        peptideTableToolTips.add("Protein Accession Numbers");
        peptideTableToolTips.add("Protein Descriptions");
        peptideTableToolTips.add("Peptide Sequence");
        peptideTableToolTips.add("Number of Spectra");
        peptideTableToolTips.add("Peptide Score");
        peptideTableToolTips.add("Peptide Confidence");
        peptideTableToolTips.add("Peptide Key");

        psmTableToolTips = new ArrayList<String>();
        psmTableToolTips.add(null);
        psmTableToolTips.add("Starred");
        psmTableToolTips.add("Hidden");
        psmTableToolTips.add("Spectrum File");
        psmTableToolTips.add("Spectrum Title");
        psmTableToolTips.add("Retention Time");
        psmTableToolTips.add("Precursor m/z");
        psmTableToolTips.add("Precurrsor Charge");
        psmTableToolTips.add("Mass Error");
        psmTableToolTips.add("PSM Confidence");

        ptmTableToolTips = new ArrayList<String>();
        ptmTableToolTips.add(null);
        ptmTableToolTips.add("Modification Color");
        ptmTableToolTips.add("Modification Name");

        spectrumFilesTableToolTips = new ArrayList<String>();
        spectrumFilesTableToolTips.add(null);
        spectrumFilesTableToolTips.add("File Name");

        // setup the column widths
        proteinTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);
        proteinTable.getColumn("S").setMaxWidth(30);
        proteinTable.getColumn("S").setMinWidth(30);
        proteinTable.getColumn("H").setMaxWidth(30);
        proteinTable.getColumn("H").setMinWidth(30);
        proteinTable.getColumn("PI").setMaxWidth(35);
        proteinTable.getColumn("PI").setMinWidth(35);

        peptideTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn("S").setMaxWidth(30);
        peptideTable.getColumn("S").setMinWidth(30);
        peptideTable.getColumn("H").setMaxWidth(30);
        peptideTable.getColumn("H").setMinWidth(30);
        peptideTable.getColumn("PI").setMaxWidth(35);
        peptideTable.getColumn("PI").setMinWidth(35);

        psmTable.getColumn(" ").setMaxWidth(50);
        psmTable.getColumn(" ").setMinWidth(50);
        psmTable.getColumn("S").setMaxWidth(30);
        psmTable.getColumn("S").setMinWidth(30);
        psmTable.getColumn("H").setMaxWidth(30);
        psmTable.getColumn("H").setMinWidth(30);

        modificationTable.getColumn(" ").setMaxWidth(30);
        modificationTable.getColumn(" ").setMinWidth(30);
        modificationTable.getColumn("  ").setMaxWidth(40);
        modificationTable.getColumn("  ").setMinWidth(40);

        spectrumFilesTable.getColumn(" ").setMaxWidth(30);
        spectrumFilesTable.getColumn(" ").setMinWidth(30);


        // add cell renderers
        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor()); // NOT_GROUP
        proteinInferenceColorMap.put(PSParameter.ISOFORMS, Color.YELLOW); // ISOFORMS
        proteinInferenceColorMap.put(PSParameter.ISOFORMS_UNRELATED, Color.ORANGE); // ISOFORMS_UNRELATED
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED); // UNRELATED

        // set up the protein inference tooltip map
        HashMap<Integer, String> proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Single Protein");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS, "Isoforms");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS_UNRELATED, "Unrelated Isoforms");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Unrelated Proteins");

        //proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        proteinTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), proteinInferenceColorMap, proteinInferenceTooltipMap));
        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("MS2 Quant.").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).setMinimumChartValue(5d);
        proteinTable.getColumn("S").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));
        proteinTable.getColumn("H").setCellRenderer(new NimbusCheckBoxRenderer());

        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNPeptides());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNSpectra());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxSpectrumCounting());

        // set up the peptide inference color map
        HashMap<Integer, Color> peptideInferenceColorMap = new HashMap<Integer, Color>();
        peptideInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor());
        peptideInferenceColorMap.put(PSParameter.ISOFORMS, Color.YELLOW);
        peptideInferenceColorMap.put(PSParameter.ISOFORMS_UNRELATED, Color.ORANGE);
        peptideInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the peptide inference tooltip map
        HashMap<Integer, String> peptideInferenceTooltipMap = new HashMap<Integer, String>();
        peptideInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Unique to a single protein");
        peptideInferenceTooltipMap.put(PSParameter.ISOFORMS, "Belongs to a group of isoforms");
        peptideInferenceTooltipMap.put(PSParameter.ISOFORMS_UNRELATED, "Belongs to a group of isoforms and unrelated proteins");
        peptideInferenceTooltipMap.put(PSParameter.UNRELATED, "Belongs to unrelated proteins");

        peptideTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), peptideInferenceColorMap, peptideInferenceTooltipMap));
        peptideTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        peptideTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        peptideTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        peptideTable.getColumn("S").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));
        peptideTable.getColumn("H").setCellRenderer(new NimbusCheckBoxRenderer());

        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNSpectra()); // @TODO: this is not the correct max value


        psmTable.getColumn("S").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));
        psmTable.getColumn("H").setCellRenderer(new NimbusCheckBoxRenderer());
        psmTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        psmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        psmTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesIntervalChartTableCellRenderer) psmTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        psmTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                peptideShakerGUI.getSearchParameters().getPrecursorAccuracy(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).setMaxValue(
                peptideShakerGUI.getSearchParameters().getPrecursorAccuracy());
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).setMaxValue(
                (double) ((PSMaps) peptideShakerGUI.getIdentification().getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge());
        psmTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        spectrumFilesTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());

        modificationTable.getColumn(" ").setCellRenderer(new NimbusCheckBoxRenderer());
        modificationTable.getColumn("  ").setCellRenderer(new JSparklinesColorTableCellRenderer());
    }

    /**
     * Fills the protein tab
     */
    private void fillProteinTab() {
        if (proteinFilter.getIdentifierRegex() != null) {
            proteinAccessionTxt.setText(proteinFilter.getIdentifierRegex());
        }
        if (proteinFilter.getPi() < 5) {
            proteinPICmb.setSelectedIndex(proteinFilter.getPi() + 1);
            proteinPiComparisonCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getPiComparison()));
        }
        if (proteinFilter.getSpectrumCounting() != null) {
            spectrumCountingTxt.setText(proteinFilter.getSpectrumCounting() + "");
            spectrumCountingCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getSpectrumCountingComparison()));
        }
        if (proteinFilter.getProteinCoverage() != null) {
            proteinCoverageTxt.setText(proteinFilter.getProteinCoverage() + "");
            proteinCoverageCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getProteinCoverageComparison()));
        }
        if (proteinFilter.getnPeptides() != null) {
            nPeptidesTxt.setText(proteinFilter.getnPeptides() + "");
            nPeptidesCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getnPeptidesComparison()));
        }
        if (proteinFilter.getProteinNSpectra() != null) {
            proteinsNSpectraTxt.setText(proteinFilter.getProteinNSpectra() + "");
            proteinNSpectraCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getnSpectraComparison()));
        }
        if (proteinFilter.getProteinScore() != null) {
            proteinScoreTxt.setText(proteinFilter.getProteinScore() + "");
            proteinScoreCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getProteinScoreComparison()));
        }
        if (proteinFilter.getProteinConfidence() != null) {
            proteinConfidenceTxt.setText(proteinFilter.getProteinConfidence() + "");
            proteinConfidenceCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getProteinConfidenceComparison()));
        }
        boolean first = true;
        String text = "";
        for (String key : proteinFilter.getManualValidation()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }
        proteinManualValidationTxt.setText(text);
        first = true;
        text = "";
        for (String key : proteinFilter.getExceptions()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }
        proteinExceptionsTxt.setText(text);
    }

    /**
     * Fills the peptide tab
     */
    private void fillPeptideTab() {
        if (peptideFilter.getProtein() != null) {
            peptideProteinTxt.setText(peptideFilter.getProtein());
        }
        if (peptideFilter.getSequence() != null) {
            peptideSequenceTxt.setText(peptideFilter.getSequence());
        }
        if (peptideFilter.getPi() < 5) {
            peptidePICmb.setSelectedIndex(peptideFilter.getPi() + 1);
            peptidePiComparisonCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPiComparison()));
        }
        if (peptideFilter.getNSpectra() != null) {
            peptideNSpectraTxt.setText(peptideFilter.getNSpectra() + "");
            peptideNSpectraCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getnSpectraComparison()));
        }
        if (peptideFilter.getPeptideScore() != null) {
            peptideScoreTxt.setText(peptideFilter.getPeptideScore() + "");
            proteinScoreCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPeptideScoreComparison()));
        }
        if (peptideFilter.getPeptideConfidence() != null) {
            peptideConfidenceTxt.setText(peptideFilter.getPeptideConfidence() + "");
            peptideConfidenceCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPeptideConfidenceComparison()));
        }

        for (int row = 0; row < modificationTable.getRowCount(); row++) {
            if (peptideFilter.getModificationStatus().contains(
                    (String) modificationTable.getValueAt(row, 2))) {
                modificationTable.setValueAt(true, row, 0);
            } else {
                modificationTable.setValueAt(false, row, 0);
            }
        }
        boolean first = true;
        String text = "";
        for (String key : peptideFilter.getManualValidation()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }
        peptideManualValidationTxt.setText(text);
        first = true;
        text = "";
        for (String accession : peptideFilter.getExceptions()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += accession;
        }
        peptideExceptionsTxt.setText(text);
    }

    /**
     * Fills the psm tab
     */
    private void fillPsmTab() {
        if (psmFilter.getPrecursorRT() != null) {
            precursorRTTxt.setText(psmFilter.getPrecursorRT() + "");
            precursorRTCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPrecursorRTComparison()));
        }
        if (psmFilter.getPrecursorMz() != null) {
            precursorMzTxt.setText(psmFilter.getPrecursorMz() + "");
            precursorMzCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPrecursorMzComparison()));
        }
        if (psmFilter.getPrecursorMzError() != null) {
            precursorErrorTxt.setText(psmFilter.getPrecursorMzError() + "");
            precursorErrorCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPrecursorMzErrorComparison()));
        }
        if (psmFilter.getPsmConfidence() != null) {
            psmConfidenceTxt.setText(psmFilter.getPsmConfidence() + "");
            psmConfidenceCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPsmConfidenceComparison()));
        }

        charge2CheckBox.setSelected(psmFilter.getCharges().contains(2));
        charge3CheckBox.setSelected(psmFilter.getCharges().contains(3));
        charge4CheckBox.setSelected(psmFilter.getCharges().contains(4));
        chargeOver4CheckBox.setSelected(psmFilter.getCharges().contains(5));

        for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
            if (psmFilter.getFileNames().contains(
                    (String) spectrumFilesTable.getValueAt(row, 1))) {
                spectrumFilesTable.setValueAt(true, row, 0);
            } else {
                spectrumFilesTable.setValueAt(false, row, 0);
            }
        }
        boolean first = true;
        String text = "";
        for (String key : psmFilter.getManualValidation()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }
        psmManualValidationTxt.setText(text);
        first = true;
        text = "";
        for (String accession : psmFilter.getExceptions()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += accession;
        }
        psmExceptionsTxt.setText(text);
    }

    /**
     * Validates the input
     * @return a boolean indicating whether the input is valid
     */
    public boolean validateInput() {
        if (!proteinCoverageTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(proteinCoverageTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein coverage.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!spectrumCountingTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(spectrumCountingTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for spectrum counting.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!nPeptidesTxt.getText().trim().equals("")) {
            try {
                Integer test = new Integer(nPeptidesTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for number of peptides.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinsNSpectraTxt.getText().trim().equals("")) {
            try {
                Integer test = new Integer(proteinsNSpectraTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein number of spectra.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinScoreTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(proteinScoreTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein score.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinConfidenceTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(proteinConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideNSpectraTxt.getText().trim().equals("")) {
            try {
                Integer test = new Integer(peptideNSpectraTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide number of spectra.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideScoreTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(peptideScoreTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide score.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideConfidenceTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(peptideConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorRTTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(precursorRTTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor retention time.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorMzTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(precursorMzTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor m/z.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorErrorTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(precursorErrorTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor error.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!psmConfidenceTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(psmConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for PSM confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience method returning the comparison type based on the selected item in the < = > combo boxes
     * @param selectedItem the index of the item selected
     * @return the corresponding comparison type
     */
    private ComparisonType getComparisonType(int selectedItem) {
        switch (selectedItem) {
            case 0:
                return ComparisonType.EQUAL;
            case 1:
                return ComparisonType.NOT_EQUAL;
            case 2:
                return ComparisonType.BEFORE;
            case 3:
                return ComparisonType.AFTER;
            default:
                return ComparisonType.EQUAL;
        }
    }

    /**
     * Convenience method returning the combo box index based on a ComparisonType
     * @param comparisonType the comparison type
     * @return the corresponding index to select
     */
    private int getComparisonIndex(ComparisonType comparisonType) {
        switch (comparisonType) {
            case EQUAL:
                return 0;
            case NOT_EQUAL:
                return 1;
            case BEFORE:
                return 2;
            case AFTER:
                return 3;
            default:
                return 0;
        }
    }

    /**
     * Filters the protein table according to the current filter settings.
     */
    private void filterProteins() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            // protein accession filter
            // @TODO can we make this not case sensitive?
            String text = proteinAccessionTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    List<RowFilter<Object, Object>> accessionFilters = new ArrayList<RowFilter<Object, Object>>();
                    accessionFilters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Accession").getModelIndex()));
                    accessionFilters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Isoforms").getModelIndex()));
                    accessionFilters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Description").getModelIndex()));
                    filters.add(RowFilter.orFilter(accessionFilters));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for protein accession/description.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if (proteinPICmb.getSelectedIndex() > 0) {
                int pi = proteinPICmb.getSelectedIndex() - 1;
                filters.add(RowFilter.numberFilter(getComparisonType(proteinPiComparisonCmb.getSelectedIndex()), pi, proteinTable.getColumn("PI").getModelIndex()));
            }

            text = spectrumCountingTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(spectrumCountingCmb.getSelectedIndex()), value, proteinTable.getColumn("MS2 Quant.").getModelIndex()));
            }

            text = proteinCoverageTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinCoverageCmb.getSelectedIndex()), value, proteinTable.getColumn("Coverage").getModelIndex()));
            }

            text = nPeptidesTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(nPeptidesCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("#Peptides").getModelIndex()));
            }

            text = proteinsNSpectraTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinNSpectraCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("#Spectra").getModelIndex()));
            }

            text = proteinScoreTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinScoreCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("Score").getModelIndex()));
            }

            text = proteinConfidenceTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinConfidenceCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("Confidence").getModelIndex()));
            }

            // set the filters to the table
            RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
            ((TableRowSorter) proteinTable.getRowSorter()).setRowFilter(allFilters);

            ((TitledBorder) proteinTablePanel.getBorder()).setTitle("Filtered Proteins (" + proteinTable.getRowCount() + ")");
            proteinTablePanel.revalidate();
            proteinTablePanel.repaint();

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Creates the protein filter according to the users input
     */
    public void createProteinFilter() {
        if (validateInput()) {
            Integer pi = null;
            if (proteinPICmb.getSelectedIndex() != 0) {
                pi = proteinPICmb.getSelectedIndex() - 1;
            }
            if (proteinFilter == null) {
                proteinFilter = new ProteinFilter("find protein filter");
            }
            if (!proteinAccessionTxt.getText().trim().equals("")) {
                proteinFilter.setIdentifierRegex(proteinAccessionTxt.getText().trim());
            }
            if (pi != null) {
                proteinFilter.setPi(pi);
                proteinFilter.setPiComparison(getComparisonType(proteinPiComparisonCmb.getSelectedIndex()));
            }
            if (!spectrumCountingTxt.getText().trim().equals("")) {
                proteinFilter.setSpectrumCounting(new Double(spectrumCountingTxt.getText().trim()));
                proteinFilter.setSpectrumCountingComparison(getComparisonType(spectrumCountingCmb.getSelectedIndex()));
            }
            if (!proteinCoverageTxt.getText().trim().equals("")) {
                proteinFilter.setProteinCoverage(new Double(proteinCoverageTxt.getText().trim()));
                proteinFilter.setProteinCoverageComparison(getComparisonType(proteinCoverageCmb.getSelectedIndex()));
            }
            if (!nPeptidesTxt.getText().trim().equals("")) {
                proteinFilter.setnPeptides(new Integer(nPeptidesTxt.getText().trim()));
                proteinFilter.setnPeptidesComparison(getComparisonType(nPeptidesCmb.getSelectedIndex()));
            }
            if (!proteinsNSpectraTxt.getText().trim().equals("")) {
                proteinFilter.setProteinNSpectra(new Integer(proteinsNSpectraTxt.getText().trim()));
                proteinFilter.setnSpectraComparison(getComparisonType(proteinNSpectraCmb.getSelectedIndex()));
            }
            if (!proteinScoreTxt.getText().trim().equals("")) {
                proteinFilter.setProteinScore(new Double(proteinScoreTxt.getText().trim()));
                proteinFilter.setProteinScoreComparison(getComparisonType(proteinScoreCmb.getSelectedIndex()));
            }
            if (!proteinConfidenceTxt.getText().trim().equals("")) {
                proteinFilter.setProteinConfidence(new Double(proteinConfidenceTxt.getText().trim()));
                proteinFilter.setProteinConfidenceComparison(getComparisonType(proteinConfidenceCmb.getSelectedIndex()));
            }
            proteinFilter.setManualValidation(parseAccessions(proteinManualValidationTxt.getText()));
            proteinFilter.setExceptions(parseAccessions(proteinExceptionsTxt.getText()));
        }
    }

    /**
     * Table model for the protein table
     */
    private class ProteinTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return identification.getProteinIdentification().size();
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
                    return "S";
                case 2:
                    return "H";
                case 3:
                    return "PI";
                case 4:
                    return "Accession";
                case 5:
                    return "Isoforms";
                case 6:
                    return "Description";
                case 7:
                    return "Coverage";
                case 8:
                    return "#Peptides";
                case 9:
                    return "#Spectra";
                case 10:
                    return "MS2 Quant.";
                case 11:
                    return "Score";
                case 12:
                    return "Confidence";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String proteinKey = identification.getProteinIdentification().get(row);
                PSParameter psParameter;
                ProteinMatch proteinMatch;

                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.isHidden();
                    case 3:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.getGroupClass();
                    case 4:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return proteinMatch.getMainMatch();
                    case 5:
                        String otherAccessions = "";
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        for (String accession : ProteinMatch.getAccessions(proteinKey)) {
                            if (!accession.equals(proteinMatch.getMainMatch())) {
                                otherAccessions += accession + " ";
                            }
                        }
                        return otherAccessions;
                    case 6:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                    case 7:
                        return 100 * peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                    case 8:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return proteinMatch.getPeptideCount();
                    case 9:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        int cpt = 0;
                        PeptideMatch peptideMatch;
                        for (String peptideKey : proteinMatch.getPeptideMatches()) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            cpt += peptideMatch.getSpectrumCount();
                                }
                        return cpt;
                    case 10:
                        return peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey);
                    case 11:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.getProteinScore();
                    case 12:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.getProteinConfidence();
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
            return (new Double(0.0)).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {

            if (columnIndex == 1 || columnIndex == 2) {
                return true;
            }

            return false;
        }
    }

    /**
     * Filters the peptide table according to the current filter settings.
     */
    private void filterPeptides() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            // protein filter
            String text = peptideProteinTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    List<RowFilter<Object, Object>> accessionFilters = new ArrayList<RowFilter<Object, Object>>();
                    accessionFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Proteins").getModelIndex()));
                    accessionFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Descriptions").getModelIndex()));
                    filters.add(RowFilter.orFilter(accessionFilters));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for protein accession/description.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            text = peptideSequenceTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    filters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Sequence").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for peptide proteins.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if (peptidePICmb.getSelectedIndex() > 0) {
                int pi = peptidePICmb.getSelectedIndex() - 1;
                filters.add(RowFilter.numberFilter(getComparisonType(peptidePiComparisonCmb.getSelectedIndex()), pi, peptideTable.getColumn("PI").getModelIndex()));
            }

            text = peptideNSpectraTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(peptideNSpectraCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("#Spectra").getModelIndex()));
            }

            text = peptideScoreTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(peptideScoreCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("Score").getModelIndex()));
            }

            text = peptideConfidenceTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(peptideConfidenceCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("Confidence").getModelIndex()));
            }

            List<RowFilter<Object, Object>> ptmFilters = new ArrayList<RowFilter<Object, Object>>();
            List<RowFilter<Object, Object>> noPtmFilters = new ArrayList<RowFilter<Object, Object>>();
            boolean noPTMSelected = false;
            for (int row = 0; row < modificationTable.getRowCount(); row++) {
                text = (String) modificationTable.getValueAt(row, 2);
                if (!text.equals(PtmPanel.NO_MODIFICATION)) {
                    if ((Boolean) modificationTable.getValueAt(row, 0)) {
                        ptmFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("peptideKey").getModelIndex()));
                    }
                    noPtmFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("peptideKey").getModelIndex()));
                } else if ((Boolean) modificationTable.getValueAt(row, 0)) {
                    noPTMSelected = true;
                }
            }
            if (noPTMSelected) {
                ptmFilters.add(RowFilter.notFilter(RowFilter.orFilter(noPtmFilters)));
            }
            filters.add(RowFilter.orFilter(ptmFilters));

            // set the filters to the table
            RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
            ((TableRowSorter) peptideTable.getRowSorter()).setRowFilter(allFilters);

            ((TitledBorder) peptideTablePanel.getBorder()).setTitle("Filtered Peptides (" + peptideTable.getRowCount() + ")");
            peptideTablePanel.revalidate();
            peptideTablePanel.repaint();

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Creates a peptide filter based on the users input
     */
    public void createPeptideFilter() {
        if (validateInput()) {
            Integer pi = null;
            if (peptidePICmb.getSelectedIndex() != 0) {
                pi = peptidePICmb.getSelectedIndex() - 1;
            }
            ArrayList<String> modifications = new ArrayList<String>();
            for (int row = 0; row < modificationTable.getRowCount(); row++) {
                if ((Boolean) modificationTable.getValueAt(row, 0)) {
                    modifications.add((String) modificationTable.getValueAt(row, 2));
                }
            }
            if (peptideFilter == null) {
                peptideFilter = new PeptideFilter("find peptide filter", modifications);
            } else {
                peptideFilter.setModificationStatus(modifications);
            }
            if (!peptideSequenceTxt.getText().trim().equals("")) {
                peptideFilter.setSequence(peptideSequenceTxt.getText().trim());
            }
            if (!peptideProteinTxt.getText().trim().equals("")) {
                peptideFilter.setProtein(peptideProteinTxt.getText().trim());
            }
            if (pi != null) {
                peptideFilter.setPi(pi);
            }
            if (!peptideNSpectraTxt.getText().trim().equals("")) {
                peptideFilter.setNSpectra(new Integer(peptideNSpectraTxt.getText().trim()));
                peptideFilter.setnSpectraComparison(getComparisonType(peptideNSpectraCmb.getSelectedIndex()));
            }
            if (!peptideScoreTxt.getText().trim().equals("")) {
                peptideFilter.setPeptideScore(new Double(peptideScoreTxt.getText().trim()));
                peptideFilter.setPeptideScoreComparison(getComparisonType(peptideScoreCmb.getSelectedIndex()));
            }
            if (!peptideConfidenceTxt.getText().trim().equals("")) {
                peptideFilter.setPeptideConfidence(new Double(peptideConfidenceTxt.getText().trim()));
                peptideFilter.setPeptideConfidenceComparison(getComparisonType(peptideConfidenceCmb.getSelectedIndex()));
            }
            peptideFilter.setManualValidation(parseAccessions(peptideManualValidationTxt.getText()));
            peptideFilter.setExceptions(parseAccessions(peptideExceptionsTxt.getText()));
        }
    }

    /**
     * Table model for the peptide table
     * @TODO: Can we hide the last column?
     */
    private class PeptideTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return identification.getPeptideIdentification().size();
        }

        @Override
        public int getColumnCount() {
            return 11;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "S";
                case 2:
                    return "H";
                case 3:
                    return "PI";
                case 4:
                    return "Proteins";
                case 5:
                    return "Descriptions";
                case 6:
                    return "Sequence";
                case 7:
                    return "#Spectra";
                case 8:
                    return "Score";
                case 9:
                    return "Confidence";
                case 10:
                    return "peptideKey";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String peptideKey = identification.getPeptideIdentification().get(row);
                PSParameter psParameter;
                PeptideMatch peptideMatch;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.isHidden();
                    case 3:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getGroupClass();
                    case 4:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        String accessions = "";
                        for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                            accessions += accession + " ";
                        }
                        return accessions;
                    case 5:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        String descriptions = "";
                        for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                            descriptions += sequenceFactory.getHeader(accession).getDescription() + " ";
                        }
                        return descriptions;
                    case 6:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 7:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideMatch.getSpectrumCount();
                    case 8:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getPeptideScore();
                    case 9:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getPeptideConfidence();
                    case 10:
                        return peptideKey;
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
            return (new Double(0.0)).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {

            if (columnIndex == 1 || columnIndex == 2) {
                return true;
            }

            return false;
        }
    }

    /**
     * Filters the psm table according to the current filter settings.
     */
    private void filterPsms() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            String text = precursorRTTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(precursorRTCmb.getSelectedIndex()), value,
                        psmTable.getColumn("RT").getModelIndex()));
            }

            text = precursorMzTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(precursorMzCmb.getSelectedIndex()), value,
                        psmTable.getColumn("m/z").getModelIndex()));
            }

            text = precursorErrorTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(precursorErrorCmb.getSelectedIndex()), value,
                        psmTable.getColumn("Mass Error").getModelIndex()));
            }


            text = psmConfidenceTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(psmConfidenceCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("Confidence").getModelIndex()));
            }

            List<RowFilter<Object, Object>> chargeFilters = new ArrayList<RowFilter<Object, Object>>();

            if (charge2CheckBox.isSelected()) {
                chargeFilters.add(RowFilter.numberFilter(ComparisonType.EQUAL, 2,
                        psmTable.getColumn("Charge").getModelIndex()));
            }
            if (charge3CheckBox.isSelected()) {
                chargeFilters.add(RowFilter.numberFilter(ComparisonType.EQUAL, 3,
                        psmTable.getColumn("Charge").getModelIndex()));
            }
            if (charge4CheckBox.isSelected()) {
                chargeFilters.add(RowFilter.numberFilter(ComparisonType.EQUAL, 4,
                        psmTable.getColumn("Charge").getModelIndex()));
            }
            if (chargeOver4CheckBox.isSelected()) {
                chargeFilters.add(RowFilter.numberFilter(ComparisonType.AFTER, 4,
                        psmTable.getColumn("Charge").getModelIndex()));
            }

            filters.add(RowFilter.orFilter(chargeFilters));

            List<RowFilter<Object, Object>> filesFilters = new ArrayList<RowFilter<Object, Object>>();
            for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
                if ((Boolean) spectrumFilesTable.getValueAt(row, 0)) {
                    text = (String) spectrumFilesTable.getValueAt(row, 1);
                    filesFilters.add(RowFilter.regexFilter(text, psmTable.getColumn("File").getModelIndex()));
                }
            }

            filters.add(RowFilter.orFilter(filesFilters));

            // set the filters to the table
            RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
            ((TableRowSorter) psmTable.getRowSorter()).setRowFilter(allFilters);

            ((TitledBorder) psmTablePanel.getBorder()).setTitle("Filtered PSMs (" + psmTable.getRowCount() + ")");
            psmTablePanel.revalidate();
            psmTablePanel.repaint();

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Creates the PSM filter based on the users input
     */
    public void createPsmFilter() {

        if (validateInput()) {
            ArrayList<Integer> charges = new ArrayList<Integer>();

            if (charge2CheckBox.isSelected()) {
                charges.add(2);
            }
            if (charge3CheckBox.isSelected()) {
                charges.add(3);
            }
            if (charge4CheckBox.isSelected()) {
                charges.add(4);
            }
            if (chargeOver4CheckBox.isSelected()) {
                charges.add(5);
            }

            ArrayList<String> files = new ArrayList<String>();
            for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
                if ((Boolean) spectrumFilesTable.getValueAt(row, 0)) {
                    files.add((String) spectrumFilesTable.getValueAt(row, 1));
                }
            }
            if (psmFilter == null) {
                psmFilter = new PsmFilter("find psm filter", charges, files);
            } else {
                psmFilter.setCharges(charges);
                psmFilter.setFileNames(files);
            }
            if (!precursorRTTxt.getText().trim().equals("")) {
                psmFilter.setPrecursorRT(new Double(precursorRTTxt.getText().trim()));
                psmFilter.setPrecursorRTComparison(getComparisonType(precursorRTCmb.getSelectedIndex()));
            }
            if (!precursorMzTxt.getText().trim().equals("")) {
                psmFilter.setPrecursorMz(new Double(precursorMzTxt.getText().trim()));
                psmFilter.setPrecursorMzComparison(getComparisonType(precursorMzCmb.getSelectedIndex()));
            }
            if (!precursorErrorTxt.getText().trim().equals("")) {
                psmFilter.setPrecursorMzError(new Double(precursorErrorTxt.getText().trim()));
                psmFilter.setPrecursorMzErrorComparison(getComparisonType(precursorErrorCmb.getSelectedIndex()));
            }
            if (!psmConfidenceTxt.getText().trim().equals("")) {
                psmFilter.setPsmConfidence(new Double(psmConfidenceTxt.getText().trim()));
                psmFilter.setPsmConfidenceComparison(getComparisonType(psmConfidenceCmb.getSelectedIndex()));
            }
            psmFilter.setManualValidation(parseAccessions(psmManualValidationTxt.getText()));
            psmFilter.setExceptions(parseAccessions(psmExceptionsTxt.getText()));
        }
    }

    /**
     * Table model for the psm table
     */
    private class PsmTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return identification.getSpectrumIdentification().size();
        }

        @Override
        public int getColumnCount() {
            return 10;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "S";
                case 2:
                    return "H";
                case 3:
                    return "File";
                case 4:
                    return "Title";
                case 5:
                    return "RT";
                case 6:
                    return "m/z";
                case 7:
                    return "Charge";
                case 8:
                    return "Mass Error";
                case 9:
                    return "Confidence";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String spectrumKey = identification.getSpectrumIdentification().get(row);
                PSParameter psParameter;
                SpectrumMatch spectrumMatch;
                Precursor precursor;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                        return psParameter.isHidden();
                    case 3:
                        return Spectrum.getSpectrumFile(spectrumKey);
                    case 4:
                        return Spectrum.getSpectrumTitle(spectrumKey);
                    case 5:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return precursor.getRt();
                    case 6:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return precursor.getMz();
                    case 7:
                        spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        return spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                    case 8:
                        spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()));
                    case 9:
                        psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                        return psParameter.getPsmConfidence();
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
            return (new Double(0.0)).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {

            if (columnIndex == 1 || columnIndex == 2) {
                return true;
            }

            return false;
        }
    }

    /**
     * Indicates whether something was input for the protein filter
     * @return a boolean indicating whether something was input for the protein filter
     */
    private boolean proteinInput() {
        return !proteinAccessionTxt.getText().trim().equals("")
                || proteinPICmb.getSelectedIndex() > 0
                || !spectrumCountingTxt.getText().trim().equals("")
                || !proteinCoverageTxt.getText().trim().equals("")
                || !nPeptidesTxt.getText().trim().equals("")
                || !proteinsNSpectraTxt.getText().trim().equals("")
                || !proteinScoreTxt.getText().trim().equals("")
                || !proteinCoverageTxt.getText().trim().equals("");
    }

    /**
     * Indicates whether something was input for the peptide filter
     * @return a boolean indicating whether something was input for the peptide filter
     */
    private boolean peptideInput() {
        if (!peptideProteinTxt.getText().trim().equals("")
                || !peptideSequenceTxt.getText().trim().equals("")
                || peptidePICmb.getSelectedIndex() > 0
                || !peptideNSpectraTxt.getText().trim().equals("")
                || !peptideScoreTxt.getText().trim().equals("")
                || !peptideConfidenceTxt.getText().trim().equals("")) {
            return true;
        }
        for (int row = 0; row < modificationTable.getRowCount(); row++) {
            if (!(Boolean) modificationTable.getValueAt(row, 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether something was input for the psm filter
     * @return a boolean indicating whether something was input for the psm filter
     */
    private boolean psmInput() {
        if (!precursorRTTxt.getText().trim().equals("")
                || !precursorMzTxt.getText().trim().equals("")
                || !precursorErrorTxt.getText().trim().equals("")
                || !psmConfidenceTxt.getText().trim().equals("")) {
            return true;
        }

        if (charge2CheckBox.isSelected()
                || charge3CheckBox.isSelected()
                || charge4CheckBox.isSelected()
                || chargeOver4CheckBox.isSelected()) {
            return true;
        }

        for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
            if (!(Boolean) spectrumFilesTable.getValueAt(row, 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method parsing keys in the manual validation/exception text fields
     * @param text  the text in the text field
     * @return      a list of the parsed keys
     */
    private ArrayList<String> parseAccessions(String text) {
        ArrayList<String> result = new ArrayList<String>();
        String[] split = text.split(";"); //todo allow other separators
        for (String part : split) {
            if (!part.trim().equals("")) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectPtmsJPopupMenu = new javax.swing.JPopupMenu();
        selectAllPtmsMenuItem = new javax.swing.JMenuItem();
        deselectAllPtmsMenuItem = new javax.swing.JMenuItem();
        selectFilesJPopupMenu = new javax.swing.JPopupMenu();
        selectAllFilesMenuItem = new javax.swing.JMenuItem();
        deselectAllFilesMenuItem = new javax.swing.JMenuItem();
        backgroundPanel = new javax.swing.JPanel();
        exitButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        filterTypeJTabbedPane = new javax.swing.JTabbedPane();
        proteinsPanel = new javax.swing.JPanel();
        proteinTablePanel = new javax.swing.JPanel();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable =         new JTable() {
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
        proteinFilterParamsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        spectrumCountingTxt = new javax.swing.JTextField();
        proteinCoverageCmb = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        proteinAccessionTxt = new javax.swing.JTextField();
        proteinPICmb = new javax.swing.JComboBox();
        proteinConfidenceTxt = new javax.swing.JTextField();
        proteinConfidenceCmb = new javax.swing.JComboBox();
        proteinScoreTxt = new javax.swing.JTextField();
        proteinScoreCmb = new javax.swing.JComboBox();
        proteinsNSpectraTxt = new javax.swing.JTextField();
        proteinNSpectraCmb = new javax.swing.JComboBox();
        nPeptidesTxt = new javax.swing.JTextField();
        nPeptidesCmb = new javax.swing.JComboBox();
        proteinCoverageTxt = new javax.swing.JTextField();
        spectrumCountingCmb = new javax.swing.JComboBox();
        proteinPiComparisonCmb = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        proteinSplitPane = new javax.swing.JSplitPane();
        proteinManualValidationPanel = new javax.swing.JPanel();
        proteinManualValidationScrollPane = new javax.swing.JScrollPane();
        proteinManualValidationTxt = new javax.swing.JTextArea();
        proteinExceptionsPanel = new javax.swing.JPanel();
        proteinExceptionsJScrollPane = new javax.swing.JScrollPane();
        proteinExceptionsTxt = new javax.swing.JTextArea();
        peptidesPanel = new javax.swing.JPanel();
        peptideFilterParamsPanel = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        peptideProteinTxt = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        peptidePICmb = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        peptideNSpectraTxt = new javax.swing.JTextField();
        peptideNSpectraCmb = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        peptideConfidenceTxt = new javax.swing.JTextField();
        peptideConfidenceCmb = new javax.swing.JComboBox();
        peptideScoreCmb = new javax.swing.JComboBox();
        peptideScoreTxt = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        peptideSequenceTxt = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        peptidePiComparisonCmb = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        modificationsScrollPane = new javax.swing.JScrollPane();
        modificationTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) ptmTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        peptidesSplitPane = new javax.swing.JSplitPane();
        peptidesManualValidationPanel = new javax.swing.JPanel();
        peptidesManualValidationScrollPane = new javax.swing.JScrollPane();
        peptideManualValidationTxt = new javax.swing.JTextArea();
        peptidesExceptionsPanel = new javax.swing.JPanel();
        peptidesExceptionsScrollPane = new javax.swing.JScrollPane();
        peptideExceptionsTxt = new javax.swing.JTextArea();
        peptideTablePanel = new javax.swing.JPanel();
        peptidesScrollPane = new javax.swing.JScrollPane();
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
        psmPanel = new javax.swing.JPanel();
        psmFilterParamsPanel = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        precursorRTTxt = new javax.swing.JTextField();
        precursorRTCmb = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        psmConfidenceTxt = new javax.swing.JTextField();
        psmConfidenceCmb = new javax.swing.JComboBox();
        jLabel24 = new javax.swing.JLabel();
        precursorMzTxt = new javax.swing.JTextField();
        precursorMzCmb = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        precursorErrorTxt = new javax.swing.JTextField();
        precursorErrorCmb = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        filesScrollPane = new javax.swing.JScrollPane();
        spectrumFilesTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) spectrumFilesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jLabel2 = new javax.swing.JLabel();
        charge2CheckBox = new javax.swing.JCheckBox();
        charge3CheckBox = new javax.swing.JCheckBox();
        charge4CheckBox = new javax.swing.JCheckBox();
        chargeOver4CheckBox = new javax.swing.JCheckBox();
        psmSplitPane = new javax.swing.JSplitPane();
        psmManualValidationPanel = new javax.swing.JPanel();
        psmManualValidationScrollPane = new javax.swing.JScrollPane();
        psmManualValidationTxt = new javax.swing.JTextArea();
        psmExceptionsPanel = new javax.swing.JPanel();
        psmExceptinosScrollPane = new javax.swing.JScrollPane();
        psmExceptionsTxt = new javax.swing.JTextArea();
        psmTablePanel = new javax.swing.JPanel();
        psmTableScrollPane = new javax.swing.JScrollPane();
        psmTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) psmTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        openDialogHelpJButton = new javax.swing.JButton();

        selectAllPtmsMenuItem.setText("Select All");
        selectAllPtmsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllPtmsMenuItemActionPerformed(evt);
            }
        });
        selectPtmsJPopupMenu.add(selectAllPtmsMenuItem);

        deselectAllPtmsMenuItem.setText("Deselect All");
        deselectAllPtmsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectAllPtmsMenuItemActionPerformed(evt);
            }
        });
        selectPtmsJPopupMenu.add(deselectAllPtmsMenuItem);

        selectAllFilesMenuItem.setText("Select All");
        selectAllFilesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllFilesMenuItemActionPerformed(evt);
            }
        });
        selectFilesJPopupMenu.add(selectAllFilesMenuItem);

        deselectAllFilesMenuItem.setText("Deselect All");
        deselectAllFilesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectAllFilesMenuItemActionPerformed(evt);
            }
        });
        selectFilesJPopupMenu.add(deselectAllFilesMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Create Filter");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        filterTypeJTabbedPane.setBackground(new java.awt.Color(230, 230, 230));

        proteinsPanel.setOpaque(false);

        proteinTablePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filtered Proteins"));
        proteinTablePanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new ProteinTable());
        proteinTable.setOpaque(false);
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
        });
        proteinScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout proteinTablePanelLayout = new javax.swing.GroupLayout(proteinTablePanel);
        proteinTablePanel.setLayout(proteinTablePanelLayout);
        proteinTablePanelLayout.setHorizontalGroup(
            proteinTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinTablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane)
                .addContainerGap())
        );
        proteinTablePanelLayout.setVerticalGroup(
            proteinTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinTablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinFilterParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));
        proteinFilterParamsPanel.setOpaque(false);

        jLabel1.setText("Identifier:");

        jLabel3.setText("PI Status:");

        jLabel4.setText("MS2 Quant.:");

        jLabel5.setText("#Peptides:");

        jLabel6.setText("#Spectra:");

        jLabel7.setText("Score:");

        jLabel8.setText("Coverage:");

        jLabel9.setText("Confidence:");

        spectrumCountingTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        spectrumCountingTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumCountingTxtKeyReleased(evt);
            }
        });

        proteinCoverageCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinCoverageCmb.setSelectedIndex(3);
        proteinCoverageCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinCoverageCmbActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel10.setText("RegExp");

        proteinAccessionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinAccessionTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinAccessionTxtActionPerformed(evt);
            }
        });
        proteinAccessionTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinAccessionTxtKeyReleased(evt);
            }
        });

        proteinPICmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Requirement", "Single Protein", "Isoforms", "Isoforms/Unrelated Proteins", "Unrelated Proteins" }));
        proteinPICmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinPICmbActionPerformed(evt);
            }
        });
        proteinPICmb.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinPICmbKeyReleased(evt);
            }
        });

        proteinConfidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinConfidenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinConfidenceTxtKeyReleased(evt);
            }
        });

        proteinConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinConfidenceCmb.setSelectedIndex(3);
        proteinConfidenceCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinConfidenceCmbActionPerformed(evt);
            }
        });

        proteinScoreTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinScoreTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinScoreTxtKeyReleased(evt);
            }
        });

        proteinScoreCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinScoreCmb.setSelectedIndex(3);
        proteinScoreCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinScoreCmbActionPerformed(evt);
            }
        });

        proteinsNSpectraTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinsNSpectraTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinsNSpectraTxtKeyReleased(evt);
            }
        });

        proteinNSpectraCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinNSpectraCmb.setSelectedIndex(3);
        proteinNSpectraCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinNSpectraCmbActionPerformed(evt);
            }
        });

        nPeptidesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nPeptidesTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                nPeptidesTxtKeyReleased(evt);
            }
        });

        nPeptidesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        nPeptidesCmb.setSelectedIndex(3);
        nPeptidesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nPeptidesCmbActionPerformed(evt);
            }
        });

        proteinCoverageTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinCoverageTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinCoverageTxtKeyReleased(evt);
            }
        });

        spectrumCountingCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        spectrumCountingCmb.setSelectedIndex(3);
        spectrumCountingCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumCountingCmbActionPerformed(evt);
            }
        });

        proteinPiComparisonCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=" }));
        proteinPiComparisonCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinPiComparisonCmbActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout proteinFilterParamsPanelLayout = new javax.swing.GroupLayout(proteinFilterParamsPanel);
        proteinFilterParamsPanel.setLayout(proteinFilterParamsPanelLayout);
        proteinFilterParamsPanelLayout.setHorizontalGroup(
            proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinAccessionTxt)
                    .addComponent(proteinPICmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(spectrumCountingTxt)
                    .addComponent(proteinCoverageTxt))
                .addGap(18, 18, 18)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(proteinPiComparisonCmb, 0, 48, Short.MAX_VALUE)
                    .addComponent(spectrumCountingCmb, 0, 48, Short.MAX_VALUE)
                    .addComponent(proteinCoverageCmb, 0, 48, Short.MAX_VALUE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7)
                    .addComponent(jLabel9))
                .addGap(23, 23, 23)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nPeptidesTxt)
                    .addComponent(proteinsNSpectraTxt)
                    .addComponent(proteinScoreTxt)
                    .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nPeptidesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        proteinFilterParamsPanelLayout.setVerticalGroup(
            proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nPeptidesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(nPeptidesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(proteinsNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel6)
                                    .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(proteinScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel7)
                                    .addComponent(proteinScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(proteinConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel10)
                                .addComponent(proteinAccessionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(proteinPICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3)
                                .addComponent(proteinPiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel4)
                                .addComponent(spectrumCountingTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(spectrumCountingCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(proteinCoverageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel8)
                                .addComponent(proteinCoverageCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel9)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        proteinSplitPane.setBorder(null);
        proteinSplitPane.setDividerLocation(400);
        proteinSplitPane.setDividerSize(-1);
        proteinSplitPane.setResizeWeight(0.5);
        proteinSplitPane.setOpaque(false);

        proteinManualValidationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));
        proteinManualValidationPanel.setOpaque(false);

        proteinManualValidationTxt.setColumns(20);
        proteinManualValidationTxt.setRows(1);
        proteinManualValidationScrollPane.setViewportView(proteinManualValidationTxt);

        javax.swing.GroupLayout proteinManualValidationPanelLayout = new javax.swing.GroupLayout(proteinManualValidationPanel);
        proteinManualValidationPanel.setLayout(proteinManualValidationPanelLayout);
        proteinManualValidationPanelLayout.setHorizontalGroup(
            proteinManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinManualValidationPanelLayout.setVerticalGroup(
            proteinManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinSplitPane.setLeftComponent(proteinManualValidationPanel);

        proteinExceptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exception(s)"));
        proteinExceptionsPanel.setOpaque(false);

        proteinExceptionsTxt.setColumns(20);
        proteinExceptionsTxt.setRows(1);
        proteinExceptionsJScrollPane.setViewportView(proteinExceptionsTxt);

        javax.swing.GroupLayout proteinExceptionsPanelLayout = new javax.swing.GroupLayout(proteinExceptionsPanel);
        proteinExceptionsPanel.setLayout(proteinExceptionsPanelLayout);
        proteinExceptionsPanelLayout.setHorizontalGroup(
            proteinExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinExceptionsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinExceptionsPanelLayout.setVerticalGroup(
            proteinExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinExceptionsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinSplitPane.setRightComponent(proteinExceptionsPanel);

        javax.swing.GroupLayout proteinsPanelLayout = new javax.swing.GroupLayout(proteinsPanel);
        proteinsPanel.setLayout(proteinsPanelLayout);
        proteinsPanelLayout.setHorizontalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(proteinSplitPane, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinTablePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proteinFilterParamsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        proteinsPanelLayout.setVerticalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinFilterParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinTablePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        filterTypeJTabbedPane.addTab("Proteins", proteinsPanel);

        peptidesPanel.setOpaque(false);

        peptideFilterParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));
        peptideFilterParamsPanel.setOpaque(false);

        jLabel12.setText("Protein:");

        peptideProteinTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideProteinTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideProteinTxtKeyReleased(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel13.setText("RegExp");

        jLabel14.setText("PI Status:");

        peptidePICmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Requirement", "Single Protein", "Isoforms", "Isoforms/Unrelated Proteins", "Unrelated Proteins" }));
        peptidePICmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidePICmbActionPerformed(evt);
            }
        });
        peptidePICmb.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidePICmbKeyReleased(evt);
            }
        });

        jLabel15.setText("#Spectra");

        peptideNSpectraTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideNSpectraTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideNSpectraTxtKeyReleased(evt);
            }
        });

        peptideNSpectraCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideNSpectraCmb.setSelectedIndex(3);
        peptideNSpectraCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideNSpectraCmbActionPerformed(evt);
            }
        });

        jLabel16.setText("Confidence:");

        peptideConfidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideConfidenceTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideConfidenceTxtActionPerformed(evt);
            }
        });
        peptideConfidenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideConfidenceTxtKeyReleased(evt);
            }
        });

        peptideConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideConfidenceCmb.setSelectedIndex(3);
        peptideConfidenceCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideConfidenceCmbActionPerformed(evt);
            }
        });

        peptideScoreCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideScoreCmb.setSelectedIndex(3);
        peptideScoreCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideScoreCmbActionPerformed(evt);
            }
        });

        peptideScoreTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideScoreTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideScoreTxtKeyReleased(evt);
            }
        });

        jLabel17.setText("Score:");

        jLabel19.setText("Sequence:");

        peptideSequenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideSequenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideSequenceTxtKeyReleased(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel20.setText("RegExp");

        peptidePiComparisonCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=" }));
        peptidePiComparisonCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidePiComparisonCmbActionPerformed(evt);
            }
        });
        peptidePiComparisonCmb.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidePiComparisonCmbKeyReleased(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Modifications"));
        jPanel1.setOpaque(false);

        modificationTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "  ", "PTM"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        modificationTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                modificationTableMouseReleased(evt);
            }
        });
        modificationsScrollPane.setViewportView(modificationTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout peptideFilterParamsPanelLayout = new javax.swing.GroupLayout(peptideFilterParamsPanel);
        peptideFilterParamsPanel.setLayout(peptideFilterParamsPanelLayout);
        peptideFilterParamsPanelLayout.setHorizontalGroup(
            peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(jLabel12)
                            .addComponent(jLabel19)
                            .addComponent(jLabel14)
                            .addComponent(jLabel15))
                        .addGap(25, 25, 25))
                    .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(18, 18, 18)))
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptideConfidenceTxt)
                    .addComponent(peptideScoreTxt)
                    .addComponent(peptideNSpectraTxt)
                    .addComponent(peptideProteinTxt, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(peptideSequenceTxt, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(peptidePICmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addComponent(peptidePiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(22, 22, 22))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addGap(29, 29, 29))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addGap(29, 29, 29))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(peptideScoreCmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(peptideNSpectraCmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, 48, Short.MAX_VALUE))
                        .addGap(22, 22, 22)))
                .addGap(26, 26, 26)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptideFilterParamsPanelLayout.setVerticalGroup(
            peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(peptideProteinTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(peptideSequenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(peptidePICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptidePiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(peptideNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(peptideScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel17)
                            .addComponent(peptideScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(peptideConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        peptidesSplitPane.setBorder(null);
        peptidesSplitPane.setDividerLocation(350);
        peptidesSplitPane.setDividerSize(-1);
        peptidesSplitPane.setResizeWeight(0.5);
        peptidesSplitPane.setOpaque(false);

        peptidesManualValidationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));
        peptidesManualValidationPanel.setOpaque(false);

        peptideManualValidationTxt.setColumns(20);
        peptideManualValidationTxt.setRows(1);
        peptidesManualValidationScrollPane.setViewportView(peptideManualValidationTxt);

        javax.swing.GroupLayout peptidesManualValidationPanelLayout = new javax.swing.GroupLayout(peptidesManualValidationPanel);
        peptidesManualValidationPanel.setLayout(peptidesManualValidationPanelLayout);
        peptidesManualValidationPanelLayout.setHorizontalGroup(
            peptidesManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesManualValidationPanelLayout.setVerticalGroup(
            peptidesManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesSplitPane.setLeftComponent(peptidesManualValidationPanel);

        peptidesExceptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exception(s)"));
        peptidesExceptionsPanel.setOpaque(false);

        peptideExceptionsTxt.setColumns(20);
        peptideExceptionsTxt.setRows(1);
        peptidesExceptionsScrollPane.setViewportView(peptideExceptionsTxt);

        javax.swing.GroupLayout peptidesExceptionsPanelLayout = new javax.swing.GroupLayout(peptidesExceptionsPanel);
        peptidesExceptionsPanel.setLayout(peptidesExceptionsPanelLayout);
        peptidesExceptionsPanelLayout.setHorizontalGroup(
            peptidesExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidesExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesExceptionsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesExceptionsPanelLayout.setVerticalGroup(
            peptidesExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesExceptionsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesSplitPane.setRightComponent(peptidesExceptionsPanel);

        peptideTablePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filtered Peptides"));
        peptideTablePanel.setOpaque(false);

        peptideTable.setModel(new PeptideTable());
        peptideTable.setOpaque(false);
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        peptidesScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout peptideTablePanelLayout = new javax.swing.GroupLayout(peptideTablePanel);
        peptideTablePanel.setLayout(peptideTablePanelLayout);
        peptideTablePanelLayout.setHorizontalGroup(
            peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptideTablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesScrollPane)
                .addContainerGap())
        );
        peptideTablePanelLayout.setVerticalGroup(
            peptideTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptideTablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout peptidesPanelLayout = new javax.swing.GroupLayout(peptidesPanel);
        peptidesPanel.setLayout(peptidesPanelLayout);
        peptidesPanelLayout.setHorizontalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptideFilterParamsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(peptidesSplitPane)
                    .addComponent(peptideTablePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        peptidesPanelLayout.setVerticalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideFilterParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptidesSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideTablePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        filterTypeJTabbedPane.addTab("Peptides", peptidesPanel);

        psmPanel.setOpaque(false);

        psmFilterParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));
        psmFilterParamsPanel.setOpaque(false);

        jLabel22.setText("Precursor RT:");

        precursorRTTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorRTTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorRTTxtKeyReleased(evt);
            }
        });

        precursorRTCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorRTCmb.setSelectedIndex(3);
        precursorRTCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorRTCmbActionPerformed(evt);
            }
        });

        jLabel23.setText("Confidence:");

        psmConfidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        psmConfidenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmConfidenceTxtKeyReleased(evt);
            }
        });

        psmConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        psmConfidenceCmb.setSelectedIndex(3);
        psmConfidenceCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmConfidenceCmbActionPerformed(evt);
            }
        });

        jLabel24.setText("Precursor m/z:");

        precursorMzTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorMzTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorMzTxtKeyReleased(evt);
            }
        });

        precursorMzCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorMzCmb.setSelectedIndex(3);
        precursorMzCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorMzCmbActionPerformed(evt);
            }
        });

        jLabel25.setText("Precursor Error:");

        precursorErrorTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorErrorTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorErrorTxtKeyReleased(evt);
            }
        });

        precursorErrorCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorErrorCmb.setSelectedIndex(3);
        precursorErrorCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorErrorCmbActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Files"));
        jPanel2.setOpaque(false);

        spectrumFilesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "File"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        spectrumFilesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumFilesTableMouseReleased(evt);
            }
        });
        filesScrollPane.setViewportView(spectrumFilesTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel2.setText("Charge:");

        charge2CheckBox.setSelected(true);
        charge2CheckBox.setText("2");
        charge2CheckBox.setIconTextGap(6);
        charge2CheckBox.setOpaque(false);
        charge2CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                charge2CheckBoxActionPerformed(evt);
            }
        });

        charge3CheckBox.setSelected(true);
        charge3CheckBox.setText("3");
        charge3CheckBox.setIconTextGap(6);
        charge3CheckBox.setOpaque(false);
        charge3CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                charge3CheckBoxActionPerformed(evt);
            }
        });

        charge4CheckBox.setSelected(true);
        charge4CheckBox.setText("4");
        charge4CheckBox.setIconTextGap(6);
        charge4CheckBox.setOpaque(false);
        charge4CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                charge4CheckBoxActionPerformed(evt);
            }
        });

        chargeOver4CheckBox.setSelected(true);
        chargeOver4CheckBox.setText(">4");
        chargeOver4CheckBox.setIconTextGap(6);
        chargeOver4CheckBox.setOpaque(false);
        chargeOver4CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chargeOver4CheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout psmFilterParamsPanelLayout = new javax.swing.GroupLayout(psmFilterParamsPanel);
        psmFilterParamsPanel.setLayout(psmFilterParamsPanelLayout);
        psmFilterParamsPanelLayout.setHorizontalGroup(
            psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel24)
                    .addComponent(jLabel25)
                    .addComponent(jLabel23)
                    .addComponent(jLabel22)
                    .addComponent(jLabel2))
                .addGap(21, 21, 21)
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(psmFilterParamsPanelLayout.createSequentialGroup()
                        .addComponent(charge2CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(charge3CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(charge4CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(chargeOver4CheckBox))
                    .addComponent(precursorRTTxt)
                    .addComponent(precursorMzTxt)
                    .addComponent(precursorErrorTxt)
                    .addComponent(psmConfidenceTxt))
                .addGap(18, 18, 18)
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(precursorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        psmFilterParamsPanelLayout.setVerticalGroup(
            psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(precursorRTTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(precursorMzTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(precursorErrorTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(psmConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23)
                    .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(charge2CheckBox)
                    .addComponent(charge3CheckBox)
                    .addComponent(charge4CheckBox)
                    .addComponent(chargeOver4CheckBox)))
            .addGroup(psmFilterParamsPanelLayout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setBorder(null);
        psmSplitPane.setDividerLocation(350);
        psmSplitPane.setDividerSize(-1);
        psmSplitPane.setResizeWeight(0.5);
        psmSplitPane.setOpaque(false);

        psmManualValidationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));
        psmManualValidationPanel.setOpaque(false);

        psmManualValidationScrollPane.setOpaque(false);

        psmManualValidationTxt.setColumns(20);
        psmManualValidationTxt.setRows(1);
        psmManualValidationScrollPane.setViewportView(psmManualValidationTxt);

        javax.swing.GroupLayout psmManualValidationPanelLayout = new javax.swing.GroupLayout(psmManualValidationPanel);
        psmManualValidationPanel.setLayout(psmManualValidationPanelLayout);
        psmManualValidationPanelLayout.setHorizontalGroup(
            psmManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmManualValidationPanelLayout.setVerticalGroup(
            psmManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setLeftComponent(psmManualValidationPanel);

        psmExceptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exception(s)"));
        psmExceptionsPanel.setOpaque(false);

        psmExceptinosScrollPane.setOpaque(false);

        psmExceptionsTxt.setColumns(20);
        psmExceptionsTxt.setRows(1);
        psmExceptinosScrollPane.setViewportView(psmExceptionsTxt);

        javax.swing.GroupLayout psmExceptionsPanelLayout = new javax.swing.GroupLayout(psmExceptionsPanel);
        psmExceptionsPanel.setLayout(psmExceptionsPanelLayout);
        psmExceptionsPanelLayout.setHorizontalGroup(
            psmExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmExceptinosScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmExceptionsPanelLayout.setVerticalGroup(
            psmExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmExceptinosScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setRightComponent(psmExceptionsPanel);

        psmTablePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filtered PSMs"));
        psmTablePanel.setOpaque(false);

        psmTable.setModel(new PsmTable());
        psmTable.setOpaque(false);
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
            }
        });
        psmTableScrollPane.setViewportView(psmTable);

        javax.swing.GroupLayout psmTablePanelLayout = new javax.swing.GroupLayout(psmTablePanel);
        psmTablePanel.setLayout(psmTablePanelLayout);
        psmTablePanelLayout.setHorizontalGroup(
            psmTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmTablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmTableScrollPane)
                .addContainerGap())
        );
        psmTablePanelLayout.setVerticalGroup(
            psmTablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmTablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout psmPanelLayout = new javax.swing.GroupLayout(psmPanel);
        psmPanel.setLayout(psmPanelLayout);
        psmPanelLayout.setHorizontalGroup(
            psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(psmTablePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(psmSplitPane)
                    .addComponent(psmFilterParamsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        psmPanelLayout.setVerticalGroup(
            psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmFilterParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmTablePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        filterTypeJTabbedPane.addTab("PSMs", psmPanel);

        openDialogHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        openDialogHelpJButton.setToolTipText("Help");
        openDialogHelpJButton.setBorder(null);
        openDialogHelpJButton.setBorderPainted(false);
        openDialogHelpJButton.setContentAreaFilled(false);
        openDialogHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseExited(evt);
            }
        });
        openDialogHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDialogHelpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(filterTypeJTabbedPane))
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(saveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exitButton)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {exitButton, saveButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filterTypeJTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 673, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(saveButton)
                    .addComponent(exitButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed

        if (filterDialog != null) {
            filterDialog.updateFilters();
        }

        dispose();
    }//GEN-LAST:event_exitButtonActionPerformed

    private void psmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseReleased
        int row = psmTable.getSelectedRow();

        if (row != -1) {
            String spectrumKey = identification.getSpectrumIdentification().get(row);
            peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, spectrumKey);
            peptideShakerGUI.updateSelectionInCurrentTab();
        }
}//GEN-LAST:event_psmTableMouseReleased

    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased
        int row = peptideTable.getSelectedRow();

        if (row != -1) {
            String peptideKey = identification.getPeptideIdentification().get(row);
            peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, peptideKey, PeptideShakerGUI.NO_SELECTION);
            peptideShakerGUI.updateSelectionInCurrentTab();
        }
}//GEN-LAST:event_peptideTableMouseReleased

    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased
        int row = proteinTable.getSelectedRow();

        if (row != -1) {
            String proteinKey = identification.getProteinIdentification().get(row);
            peptideShakerGUI.setSelectedItems(proteinKey, PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION);
            peptideShakerGUI.updateSelectionInCurrentTab();
        }
}//GEN-LAST:event_proteinTableMouseReleased

    private void proteinAccessionTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinAccessionTxtActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinAccessionTxtActionPerformed

    private void proteinAccessionTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinAccessionTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinAccessionTxtKeyReleased

    private void proteinPICmbKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinPICmbKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinPICmbKeyReleased

    private void spectrumCountingTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumCountingTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_spectrumCountingTxtKeyReleased

    private void proteinCoverageTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinCoverageTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinCoverageTxtKeyReleased

    private void nPeptidesTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nPeptidesTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_nPeptidesTxtKeyReleased

    private void proteinsNSpectraTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinsNSpectraTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinsNSpectraTxtKeyReleased

    private void proteinScoreTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinScoreTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinScoreTxtKeyReleased

    private void proteinConfidenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinConfidenceTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinConfidenceTxtKeyReleased

    private void proteinPiComparisonCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinPiComparisonCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinPiComparisonCmbActionPerformed

    private void spectrumCountingCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumCountingCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_spectrumCountingCmbActionPerformed

    private void proteinCoverageCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinCoverageCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinCoverageCmbActionPerformed

    private void nPeptidesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nPeptidesCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_nPeptidesCmbActionPerformed

    private void proteinNSpectraCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinNSpectraCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinNSpectraCmbActionPerformed

    private void proteinScoreCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinScoreCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinScoreCmbActionPerformed

    private void proteinConfidenceCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinConfidenceCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinConfidenceCmbActionPerformed

    private void peptideProteinTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideProteinTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideProteinTxtKeyReleased

    private void peptideSequenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideSequenceTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideSequenceTxtKeyReleased

    private void peptidePICmbKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidePICmbKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptidePICmbKeyReleased

    private void peptidePiComparisonCmbKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidePiComparisonCmbKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptidePiComparisonCmbKeyReleased

    private void peptideNSpectraTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideNSpectraTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideNSpectraTxtKeyReleased

    private void peptideNSpectraCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideNSpectraCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideNSpectraCmbActionPerformed

    private void peptidePiComparisonCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidePiComparisonCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptidePiComparisonCmbActionPerformed

    private void peptideScoreTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideScoreTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideScoreTxtKeyReleased

    private void peptideScoreCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideScoreCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideScoreCmbActionPerformed

    private void peptideConfidenceTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideConfidenceTxtActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideConfidenceTxtActionPerformed

    private void peptideConfidenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideConfidenceTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideConfidenceTxtKeyReleased

    private void peptideConfidenceCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideConfidenceCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideConfidenceCmbActionPerformed

    private void modificationTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationTableMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            selectPtmsJPopupMenu.show(modificationTable, evt.getX(), evt.getY());
        } else {
            filterPeptides();
        }
    }//GEN-LAST:event_modificationTableMouseReleased

    private void precursorRTTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorRTTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_precursorRTTxtKeyReleased

    private void precursorMzTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorMzTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_precursorMzTxtKeyReleased

    private void precursorErrorTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorErrorTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_precursorErrorTxtKeyReleased

    private void psmConfidenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmConfidenceTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_psmConfidenceTxtKeyReleased

    private void precursorRTCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorRTCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursorRTCmbActionPerformed

    private void precursorMzCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorMzCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursorMzCmbActionPerformed

    private void precursorErrorCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorErrorCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursorErrorCmbActionPerformed

    private void psmConfidenceCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmConfidenceCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_psmConfidenceCmbActionPerformed

    private void spectrumFilesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumFilesTableMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            selectFilesJPopupMenu.show(spectrumFilesTable, evt.getX(), evt.getY());
        } else {
            filterPsms();
        }
    }//GEN-LAST:event_spectrumFilesTableMouseReleased

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed

        if (proteinFilter != null) {
            createProteinFilter();
            if (peptideShakerGUI.getFilterPreferences().getProteinHideFilters().containsKey(proteinFilter.getName())) {
                peptideShakerGUI.getFilterPreferences().addHidingFilter(proteinFilter);
            } else {
                peptideShakerGUI.getFilterPreferences().addStarringFilter(proteinFilter);
            }
        }
        if (peptideFilter != null) {
            createPeptideFilter();
            if (peptideShakerGUI.getFilterPreferences().getPeptideHideFilters().containsKey(peptideFilter.getName())) {
                peptideShakerGUI.getFilterPreferences().addHidingFilter(peptideFilter);
            } else {
                peptideShakerGUI.getFilterPreferences().addStarringFilter(peptideFilter);
            }
        }
        if (psmFilter != null) {
            createPsmFilter();
            if (peptideShakerGUI.getFilterPreferences().getPsmHideFilters().containsKey(psmFilter.getName())) {
                peptideShakerGUI.getFilterPreferences().addHidingFilter(psmFilter);
            } else {
                peptideShakerGUI.getFilterPreferences().addStarringFilter(psmFilter);
            }
        }

        // if filters have been updated, close dialog
        if (proteinFilter != null || peptideFilter != null || psmFilter != null) {
            exitButtonActionPerformed(null);
        }

        ProteinFilter newProteinFilter = null;
        PeptideFilter newPeptideFilter = null;
        PsmFilter newPsmFilter = null;

        if (proteinFilter == null && filterTypeJTabbedPane.getSelectedIndex() == 0) {
            if (!proteinInput()) {
                JOptionPane.showMessageDialog(this, "There seems to be no filters added.", "Empty Filter?", JOptionPane.INFORMATION_MESSAGE);
            } else {
                createProteinFilter();
                newProteinFilter = proteinFilter;
                new CreateFilterDialog(peptideShakerGUI, newProteinFilter, newPeptideFilter, newPsmFilter, currentFilterType);
                exitButtonActionPerformed(null);
            }
        } else if (peptideFilter == null && filterTypeJTabbedPane.getSelectedIndex() == 1) {
            if (!peptideInput()) {
                JOptionPane.showMessageDialog(this, "There seems to be no filters added.", "Empty Filter?", JOptionPane.INFORMATION_MESSAGE);
            } else {
                createPeptideFilter();
                newPeptideFilter = peptideFilter;
                new CreateFilterDialog(peptideShakerGUI, newProteinFilter, newPeptideFilter, newPsmFilter, currentFilterType);
                exitButtonActionPerformed(null);
            }
        } else if (psmFilter == null && filterTypeJTabbedPane.getSelectedIndex() == 2) {
            if (!psmInput()) {
                JOptionPane.showMessageDialog(this, "There seems to be no filters added.", "Empty Filter?", JOptionPane.INFORMATION_MESSAGE);
            } else {
                createPsmFilter();
                newPsmFilter = psmFilter;
                new CreateFilterDialog(peptideShakerGUI, newProteinFilter, newPeptideFilter, newPsmFilter, currentFilterType);
                exitButtonActionPerformed(null);
            }
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void charge2CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_charge2CheckBoxActionPerformed
        filterPsms();
    }//GEN-LAST:event_charge2CheckBoxActionPerformed

    private void charge3CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_charge3CheckBoxActionPerformed
        filterPsms();
    }//GEN-LAST:event_charge3CheckBoxActionPerformed

    private void charge4CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_charge4CheckBoxActionPerformed
        filterPsms();
    }//GEN-LAST:event_charge4CheckBoxActionPerformed

    private void chargeOver4CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chargeOver4CheckBoxActionPerformed
        filterPsms();
    }//GEN-LAST:event_chargeOver4CheckBoxActionPerformed

    private void proteinPICmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinPICmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinPICmbActionPerformed

    private void peptidePICmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidePICmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptidePICmbActionPerformed

    private void selectAllPtmsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllPtmsMenuItemActionPerformed
        for (int i = 0; i < modificationTable.getRowCount(); i++) {
            modificationTable.setValueAt(true, i, modificationTable.getColumn(" ").getModelIndex());
        }
        filterPeptides();
    }//GEN-LAST:event_selectAllPtmsMenuItemActionPerformed

    private void deselectAllPtmsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectAllPtmsMenuItemActionPerformed

        for (int i = 0; i < modificationTable.getRowCount(); i++) {
            modificationTable.setValueAt(false, i, modificationTable.getColumn(" ").getModelIndex());
        }
        filterPeptides();
    }//GEN-LAST:event_deselectAllPtmsMenuItemActionPerformed

    private void selectAllFilesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllFilesMenuItemActionPerformed
        for (int i = 0; i < spectrumFilesTable.getRowCount(); i++) {
            spectrumFilesTable.setValueAt(true, i, spectrumFilesTable.getColumn(" ").getModelIndex());
        }
        filterPsms();
    }//GEN-LAST:event_selectAllFilesMenuItemActionPerformed

    private void deselectAllFilesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectAllFilesMenuItemActionPerformed
        for (int i = 0; i < spectrumFilesTable.getRowCount(); i++) {
            spectrumFilesTable.setValueAt(false, i, spectrumFilesTable.getColumn(" ").getModelIndex());
        }
        filterPsms();
    }//GEN-LAST:event_deselectAllFilesMenuItemActionPerformed

    /**
     * Change the cursor icon to a hand icon.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor icon to the default icon.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/FindDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JCheckBox charge2CheckBox;
    private javax.swing.JCheckBox charge3CheckBox;
    private javax.swing.JCheckBox charge4CheckBox;
    private javax.swing.JCheckBox chargeOver4CheckBox;
    private javax.swing.JMenuItem deselectAllFilesMenuItem;
    private javax.swing.JMenuItem deselectAllPtmsMenuItem;
    private javax.swing.JButton exitButton;
    private javax.swing.JScrollPane filesScrollPane;
    private javax.swing.JTabbedPane filterTypeJTabbedPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable modificationTable;
    private javax.swing.JScrollPane modificationsScrollPane;
    private javax.swing.JComboBox nPeptidesCmb;
    private javax.swing.JTextField nPeptidesTxt;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JComboBox peptideConfidenceCmb;
    private javax.swing.JTextField peptideConfidenceTxt;
    private javax.swing.JTextArea peptideExceptionsTxt;
    private javax.swing.JPanel peptideFilterParamsPanel;
    private javax.swing.JTextArea peptideManualValidationTxt;
    private javax.swing.JComboBox peptideNSpectraCmb;
    private javax.swing.JTextField peptideNSpectraTxt;
    private javax.swing.JComboBox peptidePICmb;
    private javax.swing.JComboBox peptidePiComparisonCmb;
    private javax.swing.JTextField peptideProteinTxt;
    private javax.swing.JComboBox peptideScoreCmb;
    private javax.swing.JTextField peptideScoreTxt;
    private javax.swing.JTextField peptideSequenceTxt;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel peptideTablePanel;
    private javax.swing.JPanel peptidesExceptionsPanel;
    private javax.swing.JScrollPane peptidesExceptionsScrollPane;
    private javax.swing.JPanel peptidesManualValidationPanel;
    private javax.swing.JScrollPane peptidesManualValidationScrollPane;
    private javax.swing.JPanel peptidesPanel;
    private javax.swing.JScrollPane peptidesScrollPane;
    private javax.swing.JSplitPane peptidesSplitPane;
    private javax.swing.JComboBox precursorErrorCmb;
    private javax.swing.JTextField precursorErrorTxt;
    private javax.swing.JComboBox precursorMzCmb;
    private javax.swing.JTextField precursorMzTxt;
    private javax.swing.JComboBox precursorRTCmb;
    private javax.swing.JTextField precursorRTTxt;
    private javax.swing.JTextField proteinAccessionTxt;
    private javax.swing.JComboBox proteinConfidenceCmb;
    private javax.swing.JTextField proteinConfidenceTxt;
    private javax.swing.JComboBox proteinCoverageCmb;
    private javax.swing.JTextField proteinCoverageTxt;
    private javax.swing.JScrollPane proteinExceptionsJScrollPane;
    private javax.swing.JPanel proteinExceptionsPanel;
    private javax.swing.JTextArea proteinExceptionsTxt;
    private javax.swing.JPanel proteinFilterParamsPanel;
    private javax.swing.JPanel proteinManualValidationPanel;
    private javax.swing.JScrollPane proteinManualValidationScrollPane;
    private javax.swing.JTextArea proteinManualValidationTxt;
    private javax.swing.JComboBox proteinNSpectraCmb;
    private javax.swing.JComboBox proteinPICmb;
    private javax.swing.JComboBox proteinPiComparisonCmb;
    private javax.swing.JComboBox proteinScoreCmb;
    private javax.swing.JTextField proteinScoreTxt;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JSplitPane proteinSplitPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JPanel proteinTablePanel;
    private javax.swing.JTextField proteinsNSpectraTxt;
    private javax.swing.JPanel proteinsPanel;
    private javax.swing.JComboBox psmConfidenceCmb;
    private javax.swing.JTextField psmConfidenceTxt;
    private javax.swing.JScrollPane psmExceptinosScrollPane;
    private javax.swing.JPanel psmExceptionsPanel;
    private javax.swing.JTextArea psmExceptionsTxt;
    private javax.swing.JPanel psmFilterParamsPanel;
    private javax.swing.JPanel psmManualValidationPanel;
    private javax.swing.JScrollPane psmManualValidationScrollPane;
    private javax.swing.JTextArea psmManualValidationTxt;
    private javax.swing.JPanel psmPanel;
    private javax.swing.JSplitPane psmSplitPane;
    private javax.swing.JTable psmTable;
    private javax.swing.JPanel psmTablePanel;
    private javax.swing.JScrollPane psmTableScrollPane;
    private javax.swing.JButton saveButton;
    private javax.swing.JMenuItem selectAllFilesMenuItem;
    private javax.swing.JMenuItem selectAllPtmsMenuItem;
    private javax.swing.JPopupMenu selectFilesJPopupMenu;
    private javax.swing.JPopupMenu selectPtmsJPopupMenu;
    private javax.swing.JComboBox spectrumCountingCmb;
    private javax.swing.JTextField spectrumCountingTxt;
    private javax.swing.JTable spectrumFilesTable;
    // End of variables declaration//GEN-END:variables
}
