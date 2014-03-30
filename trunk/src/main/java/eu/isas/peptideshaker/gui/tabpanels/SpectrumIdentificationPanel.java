package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.XYPlottingDialog;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.VennDiagram;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Component;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.text.DecimalFormat;
import java.util.Iterator;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * The Spectrum ID panel.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumIdentificationPanel extends javax.swing.JPanel {

    /**
     * Turns of the gradient painting for the bar charts.
     */
    static {
        BarRenderer.setDefaultBarPainter(new StandardBarPainter());
    }
    /**
     * The Venn diagram advocate colors.
     */
    private HashMap<Advocate, Color> advocateVennColors;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * Needed in order to not update the file selection too many times.
     */
    private boolean updateSelection = true;

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        SPECTRUM_FILES, PSM_TABLES
    };
    /**
     * Static index for the ID software agreement: no psm found.
     */
    public static final int NO_ID = 0;
    /**
     * Static index for the ID software agreement: the ID software have
     * different top ranking peptides.
     */
    public static final int CONFLICT = 1;
    /**
     * Static index for the ID software agreement: one or more of the softwares
     * did not identify the spectrum, while one or more of the others did.
     */
    public static final int PARTIALLY_MISSING = 2;
    /**
     * Static index for the ID software agreement: the ID softwares all have the
     * same top ranking peptide without accounting for modification
     * localization.
     */
    public static final int AGREEMENT = 3;
    /**
     * Static index for the ID software agreement: the ID softwares all have the
     * same top ranking peptide.
     */
    public static final int AGREEMENT_WITH_MODS = 4;
    /**
     * The peptide sequence tooltips for the search results table.
     */
    private ArrayList<String> searchResultsTablePeptideTooltips = null;
    /**
     * The peptide sequence tooltips for the search results table.
     */
    private String peptideShakerJTablePeptideTooltip = null;
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
    /**
     * The ID software table column header tooltips.
     */
    private ArrayList<String> idSoftwareTableToolTips;
    /**
     * The spectrum table column header tooltips.
     */
    private ArrayList<String> spectrumTableToolTips;
    /**
     * The peptide shaker table column header tooltips.
     */
    private ArrayList<String> peptideShakerTableToolTips;
    /**
     * The ID results table column header tooltips.
     */
    private ArrayList<String> idResultsTableToolTips;
    /**
     * The spectrum annotator for ID software specific results.
     */
    private PeptideSpectrumAnnotator specificAnnotator = new PeptideSpectrumAnnotator();
    /**
     * The list of search results.
     */
    private ArrayList<PeptideAssumption> searchResultsPeptideKeys = new ArrayList<PeptideAssumption>();
    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The file currently selected.
     */
    private String fileSelected = null;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The advocates used.
     */
    private ArrayList<Advocate> advocatesUsed;
    /**
     * The search engine color map.
     */
    private HashMap<Integer, java.awt.Color> searchEnginesColorMap;
    /**
     * The number of validated PSMs per mgf file. // @TODO: create this map when
     * loading the data...
     */
    private HashMap<String, Integer> numberOfValidatedPsmsMap;

    /**
     * Create a new SpectrumIdentificationPanel.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public SpectrumIdentificationPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();
        formComponentResized(null);

        spectrumTableJScrollPane.getViewport().setOpaque(false);
        peptideShakerJScrollPane.getViewport().setOpaque(false);
        idResultsTableJScrollPane.getViewport().setOpaque(false);

        fileNamesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        setUpGUI();
        setTableProperties();
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {
        ((TitledBorder) idSoftwarePanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum Identification Overview" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        ((TitledBorder) psmsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide-Spectrum Matches" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        ((TitledBorder) spectrumPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum & Fragment Ions" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        ((TitledBorder) spectrumSelectionPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum Selection" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);

        spectrumSelectionDialog.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        spectrumSelectionDialog.setModal(true);

        // set up the search engines color map
        searchEnginesColorMap = new HashMap<Integer, java.awt.Color>();
        searchEnginesColorMap.put(Advocate.PeptideShaker.getIndex(), peptideShakerGUI.getSparklineColor());
        searchEnginesColorMap.put(Advocate.XTandem.getIndex(), new java.awt.Color(153, 255, 255));
        searchEnginesColorMap.put(Advocate.OMSSA.getIndex(), new java.awt.Color(153, 153, 255));
        searchEnginesColorMap.put(Advocate.Mascot.getIndex(), new java.awt.Color(255, 153, 255));
        searchEnginesColorMap.put(Advocate.MSGF.getIndex(), new java.awt.Color(205, 92, 92));
        searchEnginesColorMap.put(Advocate.msAmanda.getIndex(), new java.awt.Color(216, 191, 216));

        // the venn diagram colors
        advocateVennColors = new HashMap<Advocate, Color>();
        advocateVennColors.put(Advocate.XTandem, Color.PALETURQUOISE);
        advocateVennColors.put(Advocate.OMSSA, Color.MEDIUMSLATEBLUE);
        advocateVennColors.put(Advocate.Mascot, Color.PINK);
        advocateVennColors.put(Advocate.MSGF, Color.INDIANRED);
        advocateVennColors.put(Advocate.msAmanda, Color.THISTLE);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // correct the color for the upper right corner
        JPanel spectrumCorner = new JPanel();
        spectrumCorner.setBackground(spectrumTable.getTableHeader().getBackground());
        spectrumTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, spectrumCorner);
        JPanel idResultsCorner = new JPanel();
        idResultsCorner.setBackground(searchResultsTable.getTableHeader().getBackground());
        idResultsTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, idResultsCorner);

        peptideShakerJTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        searchResultsTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));

        peptideShakerJTable.getTableHeader().setReorderingAllowed(false);
        spectrumTable.getTableHeader().setReorderingAllowed(false);
        searchResultsTable.getTableHeader().setReorderingAllowed(false);

        spectrumTable.setAutoCreateRowSorter(true);

        // make sure that the user is made aware that the tool is doing something during sorting of the spectrum table
        spectrumTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            @Override
            public void sorterChanged(RowSorterEvent e) {

                if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    spectrumTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                } else if (e.getType() == RowSorterEvent.Type.SORTED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    spectrumTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        });

        peptideShakerJTable.getColumn(" ").setMinWidth(30);
        peptideShakerJTable.getColumn(" ").setMaxWidth(30);
        peptideShakerJTable.getColumn("  ").setMinWidth(30);
        peptideShakerJTable.getColumn("  ").setMaxWidth(30);
        spectrumTable.getColumn(" ").setMinWidth(50);
        spectrumTable.getColumn(" ").setMaxWidth(50);
        spectrumTable.getColumn("  ").setMinWidth(30);
        spectrumTable.getColumn("  ").setMaxWidth(30);
        spectrumTable.getColumn("Confidence").setMaxWidth(90);
        spectrumTable.getColumn("Confidence").setMinWidth(90);

        searchResultsTable.getColumn(" ").setMinWidth(30);
        searchResultsTable.getColumn(" ").setMaxWidth(30);
        searchResultsTable.getColumn("SE").setMinWidth(37);
        searchResultsTable.getColumn("SE").setMaxWidth(37);
        searchResultsTable.getColumn("Rnk").setMinWidth(37);
        searchResultsTable.getColumn("Rnk").setMaxWidth(37);
        searchResultsTable.getColumn("  ").setMinWidth(30);
        searchResultsTable.getColumn("  ").setMaxWidth(30);

        peptideShakerJTable.getColumn("ID").setMaxWidth(37);
        peptideShakerJTable.getColumn("ID").setMinWidth(37);
        spectrumTable.getColumn("ID").setMaxWidth(37);
        spectrumTable.getColumn("ID").setMinWidth(37);

        peptideShakerJTable.getColumn("Confidence").setMaxWidth(90);
        peptideShakerJTable.getColumn("Confidence").setMinWidth(90);
        searchResultsTable.getColumn("Confidence").setMaxWidth(90);
        searchResultsTable.getColumn("Confidence").setMinWidth(90);
        searchResultsTable.getColumn("Charge").setMaxWidth(90);
        searchResultsTable.getColumn("Charge").setMinWidth(90);

        // set up the psm color map
        HashMap<Integer, java.awt.Color> softwareAgreementColorMap = new HashMap<Integer, java.awt.Color>();
        softwareAgreementColorMap.put(AGREEMENT_WITH_MODS, peptideShakerGUI.getSparklineColor()); // id softwares agree with PTM certainty
        softwareAgreementColorMap.put(AGREEMENT, java.awt.Color.CYAN); // id softwares agree on peptide but not ptm certainty
        softwareAgreementColorMap.put(CONFLICT, java.awt.Color.YELLOW); // id softwares don't agree
        softwareAgreementColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some id softwares id'ed some didn't

        // set up the psm tooltip map
        HashMap<Integer, String> idSoftwareTooltipMap = new HashMap<Integer, String>();
        idSoftwareTooltipMap.put(AGREEMENT_WITH_MODS, "ID Software Agree");
        idSoftwareTooltipMap.put(AGREEMENT, "ID Software Agree - PTM Certainty Issues");
        idSoftwareTooltipMap.put(CONFLICT, "ID Software Disagree");
        idSoftwareTooltipMap.put(PARTIALLY_MISSING, "First Hit(s) Missing");

        peptideShakerJTable.getColumn("ID").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, softwareAgreementColorMap, idSoftwareTooltipMap));
        peptideShakerJTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        // set up the search engines tooltip map
        HashMap<Integer, String> searchEnginesTooltipMap = new HashMap<Integer, String>();
        searchEnginesTooltipMap.put(Advocate.PeptideShaker.getIndex(), Advocate.PeptideShaker.getName());
        searchEnginesTooltipMap.put(Advocate.XTandem.getIndex(), Advocate.XTandem.getName());
        searchEnginesTooltipMap.put(Advocate.OMSSA.getIndex(), Advocate.OMSSA.getName());
        searchEnginesTooltipMap.put(Advocate.Mascot.getIndex(), Advocate.Mascot.getName());
        searchEnginesTooltipMap.put(Advocate.MSGF.getIndex(), Advocate.MSGF.getName());
        searchEnginesTooltipMap.put(Advocate.msAmanda.getIndex(), Advocate.msAmanda.getName());

        searchResultsTable.getColumn("SE").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), searchEnginesColorMap, searchEnginesTooltipMap));

        searchResultsTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        searchResultsTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);

        // set up the psm color map
        HashMap<Integer, java.awt.Color> idSoftwareSpectrumLevelColorMap = new HashMap<Integer, java.awt.Color>();
        idSoftwareSpectrumLevelColorMap.put(AGREEMENT_WITH_MODS, peptideShakerGUI.getSparklineColor()); // id softwares agree with PTM certainty
        idSoftwareSpectrumLevelColorMap.put(AGREEMENT, java.awt.Color.CYAN); // id softwares agree on peptide but not ptm certainty
        idSoftwareSpectrumLevelColorMap.put(CONFLICT, java.awt.Color.YELLOW); // id softwares don't agree
        idSoftwareSpectrumLevelColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some id softwares id'ed some didn't
        idSoftwareSpectrumLevelColorMap.put(NO_ID, java.awt.Color.lightGray); // no psm

        // set up the psm tooltip map
        HashMap<Integer, String> idSoftwareSpectrumLevelTooltipMap = new HashMap<Integer, String>();
        idSoftwareSpectrumLevelTooltipMap.put(AGREEMENT_WITH_MODS, "ID Software Agree");
        idSoftwareSpectrumLevelTooltipMap.put(AGREEMENT, "ID Software Agree - PTM Certainty Issues");
        idSoftwareSpectrumLevelTooltipMap.put(CONFLICT, "ID Software Disagree");
        idSoftwareSpectrumLevelTooltipMap.put(PARTIALLY_MISSING, "ID Software(s) Missing");
        idSoftwareSpectrumLevelTooltipMap.put(NO_ID, "(No PSM)");

        spectrumTable.getColumn("ID").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, idSoftwareSpectrumLevelColorMap, idSoftwareSpectrumLevelTooltipMap));
        spectrumTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 4d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Int").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1000d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

        spectrumTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        spectrumTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        spectrumTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        // set up the table header tooltips
        idSoftwareTableToolTips = new ArrayList<String>();
        idSoftwareTableToolTips.add(null);
        idSoftwareTableToolTips.add("Identification Software");
        idSoftwareTableToolTips.add("Validated Peptide-Spectrum Matches");
        idSoftwareTableToolTips.add("Unique Pepttide-Spectrum Matches");
        idSoftwareTableToolTips.add("Unassigned Spectra");
        idSoftwareTableToolTips.add("Identificaiton Rate (%)");

        spectrumTableToolTips = new ArrayList<String>();
        spectrumTableToolTips.add(null);
        spectrumTableToolTips.add("ID Software Agreement");
        spectrumTableToolTips.add("Spectrum Title");
        spectrumTableToolTips.add("Precursor m/z");
        spectrumTableToolTips.add("Precursor Charge");
        spectrumTableToolTips.add("Precursor Intensity");
        spectrumTableToolTips.add("Precursor Retention Time");
        spectrumTableToolTips.add("Peptide Sequence");
        spectrumTableToolTips.add("Mapping Protein(s)");
        spectrumTableToolTips.add("Peptide-Spectrum Match Confidence");
        spectrumTableToolTips.add("Validated");

        peptideShakerTableToolTips = new ArrayList<String>();
        peptideShakerTableToolTips.add(null);
        peptideShakerTableToolTips.add("ID Software Agreement");
        peptideShakerTableToolTips.add("Peptide Sequence");
        peptideShakerTableToolTips.add("Mapping Protein(s)");
        peptideShakerTableToolTips.add("Peptide Confidence");
        peptideShakerTableToolTips.add("Validated");

        idResultsTableToolTips = new ArrayList<String>();
        idResultsTableToolTips.add("Peptide Rank");
        idResultsTableToolTips.add("Search Engine / Identification Software");
        idResultsTableToolTips.add("Search Engine Rank / Identification Software Rank");
        idResultsTableToolTips.add("Peptide Sequence");
        idResultsTableToolTips.add("Precursor Charge");
        idResultsTableToolTips.add("Peptide Confidence");
        idResultsTableToolTips.add("Validated");
    }

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        spectrumTable.revalidate();
        spectrumTable.repaint();

        peptideShakerJTable.revalidate();
        peptideShakerJTable.repaint();

        searchResultsTable.revalidate();
        searchResultsTable.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        spectrumSelectionDialog = new javax.swing.JDialog();
        backgroundPanel = new javax.swing.JPanel();
        fileNamesCmb = new javax.swing.JComboBox();
        spectrumSelectionPsmSplitPane = new javax.swing.JSplitPane();
        spectrumSelectionJPanel = new javax.swing.JPanel();
        spectrumSelectionLayeredPane = new javax.swing.JLayeredPane();
        spectrumSelectionPanel = new javax.swing.JPanel();
        spectrumTableJScrollPane = new javax.swing.JScrollPane();
        spectrumTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) spectrumTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumSelectionOptionsJButton = new javax.swing.JButton();
        spectrumSelectionHelpJButton = new javax.swing.JButton();
        exportSpectrumSelectionJButton = new javax.swing.JButton();
        contextMenuSpectrumSelectionBackgroundPanel = new javax.swing.JPanel();
        psmAndSpectrumSplitPane = new javax.swing.JSplitPane();
        psmsJPanel = new javax.swing.JPanel();
        psmsLayeredPane = new javax.swing.JLayeredPane();
        psmsPanel = new javax.swing.JPanel();
        peptideShakerJScrollPane = new javax.swing.JScrollPane();
        peptideShakerJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) peptideShakerTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        idResultsPanel = new javax.swing.JPanel();
        spectrumIdResultsLabel = new javax.swing.JLabel();
        idResultsTableJScrollPane = new javax.swing.JScrollPane();
        searchResultsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) idResultsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        colorLegendLabel = new javax.swing.JLabel();
        psmsHelpJButton = new javax.swing.JButton();
        exportPsmsJButton = new javax.swing.JButton();
        contextMenuPsmsBackgroundPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumLayeredPane = new javax.swing.JLayeredPane();
        spectrumPanel = new javax.swing.JPanel();
        slidersSplitPane = new javax.swing.JSplitPane();
        slidersPanel = new javax.swing.JPanel();
        accuracySlider = new javax.swing.JSlider();
        intensitySlider = new javax.swing.JSlider();
        spectrumJPanel1 = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumChartPanel = new javax.swing.JPanel();
        spectrumHelpJButton = new javax.swing.JButton();
        exportSpectrumJButton = new javax.swing.JButton();
        contextMenuSpectrumBackgroundPanel = new javax.swing.JPanel();
        idSoftwareJPanel = new javax.swing.JPanel();
        idSoftwareJLayeredPane = new javax.swing.JLayeredPane();
        idSoftwarePanel = new javax.swing.JPanel();
        vennDiagramButton = new javax.swing.JButton();
        overviewPlotsPanel = new javax.swing.JPanel();
        idSoftwareHelpJButton = new javax.swing.JButton();
        exportIdPerformancePerformanceJButton = new javax.swing.JButton();
        contextMenuIdSoftwareBackgroundPanel = new javax.swing.JPanel();

        spectrumSelectionDialog.setTitle("Spectrum Selection");
        spectrumSelectionDialog.setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        fileNamesCmb.setBorder(null);
        fileNamesCmb.setMinimumSize(new java.awt.Dimension(200, 20));
        fileNamesCmb.setPreferredSize(new java.awt.Dimension(400, 20));
        fileNamesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNamesCmbActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNamesCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNamesCmb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout spectrumSelectionDialogLayout = new javax.swing.GroupLayout(spectrumSelectionDialog.getContentPane());
        spectrumSelectionDialog.getContentPane().setLayout(spectrumSelectionDialogLayout);
        spectrumSelectionDialogLayout.setHorizontalGroup(
            spectrumSelectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumSelectionDialogLayout.setVerticalGroup(
            spectrumSelectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        spectrumSelectionPsmSplitPane.setBorder(null);
        spectrumSelectionPsmSplitPane.setDividerLocation(340);
        spectrumSelectionPsmSplitPane.setDividerSize(0);
        spectrumSelectionPsmSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spectrumSelectionPsmSplitPane.setResizeWeight(0.5);
        spectrumSelectionPsmSplitPane.setOpaque(false);

        spectrumSelectionJPanel.setOpaque(false);

        spectrumSelectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Selection"));
        spectrumSelectionPanel.setOpaque(false);
        spectrumSelectionPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumSelectionPanelMouseReleased(evt);
            }
        });
        spectrumSelectionPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                spectrumSelectionPanelMouseMoved(evt);
            }
        });

        spectrumTableJScrollPane.setOpaque(false);

        spectrumTable.setModel(new SpectrumTable());
        spectrumTable.setOpaque(false);
        spectrumTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        spectrumTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumTableMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                spectrumTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumTableMouseExited(evt);
            }
        });
        spectrumTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                spectrumTableMouseMoved(evt);
            }
        });
        spectrumTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumTableKeyReleased(evt);
            }
        });
        spectrumTableJScrollPane.setViewportView(spectrumTable);

        javax.swing.GroupLayout spectrumSelectionPanelLayout = new javax.swing.GroupLayout(spectrumSelectionPanel);
        spectrumSelectionPanel.setLayout(spectrumSelectionPanelLayout);
        spectrumSelectionPanelLayout.setHorizontalGroup(
            spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1238, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumSelectionPanelLayout.setVerticalGroup(
            spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addContainerGap())
        );

        spectrumSelectionLayeredPane.add(spectrumSelectionPanel);
        spectrumSelectionPanel.setBounds(0, 0, 1270, 320);

        spectrumSelectionOptionsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        spectrumSelectionOptionsJButton.setToolTipText("Spectrum File Selection");
        spectrumSelectionOptionsJButton.setBorder(null);
        spectrumSelectionOptionsJButton.setBorderPainted(false);
        spectrumSelectionOptionsJButton.setContentAreaFilled(false);
        spectrumSelectionOptionsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        spectrumSelectionOptionsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_black.png"))); // NOI18N
        spectrumSelectionOptionsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumSelectionOptionsJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumSelectionOptionsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumSelectionOptionsJButtonMouseExited(evt);
            }
        });
        spectrumSelectionLayeredPane.add(spectrumSelectionOptionsJButton);
        spectrumSelectionOptionsJButton.setBounds(1230, 0, 10, 19);
        spectrumSelectionLayeredPane.setLayer(spectrumSelectionOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        spectrumSelectionHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        spectrumSelectionHelpJButton.setToolTipText("Help");
        spectrumSelectionHelpJButton.setBorder(null);
        spectrumSelectionHelpJButton.setBorderPainted(false);
        spectrumSelectionHelpJButton.setContentAreaFilled(false);
        spectrumSelectionHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        spectrumSelectionHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumSelectionHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumSelectionHelpJButtonMouseExited(evt);
            }
        });
        spectrumSelectionHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumSelectionHelpJButtonActionPerformed(evt);
            }
        });
        spectrumSelectionLayeredPane.add(spectrumSelectionHelpJButton);
        spectrumSelectionHelpJButton.setBounds(1250, 0, 10, 19);
        spectrumSelectionLayeredPane.setLayer(spectrumSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSpectrumSelectionJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumSelectionJButton.setToolTipText("Copy to File");
        exportSpectrumSelectionJButton.setBorder(null);
        exportSpectrumSelectionJButton.setBorderPainted(false);
        exportSpectrumSelectionJButton.setContentAreaFilled(false);
        exportSpectrumSelectionJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumSelectionJButton.setEnabled(false);
        exportSpectrumSelectionJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSpectrumSelectionJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSpectrumSelectionJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSpectrumSelectionJButtonMouseExited(evt);
            }
        });
        exportSpectrumSelectionJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumSelectionJButtonActionPerformed(evt);
            }
        });
        spectrumSelectionLayeredPane.add(exportSpectrumSelectionJButton);
        exportSpectrumSelectionJButton.setBounds(1240, 0, 10, 19);
        spectrumSelectionLayeredPane.setLayer(exportSpectrumSelectionJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSpectrumSelectionBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSpectrumSelectionBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSpectrumSelectionBackgroundPanel);
        contextMenuSpectrumSelectionBackgroundPanel.setLayout(contextMenuSpectrumSelectionBackgroundPanelLayout);
        contextMenuSpectrumSelectionBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSpectrumSelectionBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuSpectrumSelectionBackgroundPanelLayout.setVerticalGroup(
            contextMenuSpectrumSelectionBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        spectrumSelectionLayeredPane.add(contextMenuSpectrumSelectionBackgroundPanel);
        contextMenuSpectrumSelectionBackgroundPanel.setBounds(1230, 0, 30, 19);
        spectrumSelectionLayeredPane.setLayer(contextMenuSpectrumSelectionBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumSelectionJPanelLayout = new javax.swing.GroupLayout(spectrumSelectionJPanel);
        spectrumSelectionJPanel.setLayout(spectrumSelectionJPanelLayout);
        spectrumSelectionJPanelLayout.setHorizontalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumSelectionLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1284, Short.MAX_VALUE)
        );
        spectrumSelectionJPanelLayout.setVerticalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumSelectionLayeredPane)
        );

        spectrumSelectionPsmSplitPane.setTopComponent(spectrumSelectionJPanel);

        psmAndSpectrumSplitPane.setBorder(null);
        psmAndSpectrumSplitPane.setDividerLocation(675);
        psmAndSpectrumSplitPane.setDividerSize(0);
        psmAndSpectrumSplitPane.setResizeWeight(0.5);
        psmAndSpectrumSplitPane.setOpaque(false);

        psmsJPanel.setOpaque(false);

        psmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches"));
        psmsPanel.setOpaque(false);

        peptideShakerJScrollPane.setOpaque(false);

        peptideShakerJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "ID", "Sequence", "Protein(s)", "Confidence", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        peptideShakerJTable.setFocusable(false);
        peptideShakerJTable.setOpaque(false);
        peptideShakerJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseExited(evt);
            }
        });
        peptideShakerJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseMoved(evt);
            }
        });
        peptideShakerJScrollPane.setViewportView(peptideShakerJTable);

        idResultsPanel.setOpaque(false);

        spectrumIdResultsLabel.setFont(spectrumIdResultsLabel.getFont().deriveFont((spectrumIdResultsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        spectrumIdResultsLabel.setText("Spectrum Identification Results");

        idResultsTableJScrollPane.setMinimumSize(new java.awt.Dimension(23, 87));
        idResultsTableJScrollPane.setOpaque(false);

        searchResultsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "SE", "Rnk", "Sequence", "Charge", "Confidence", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        searchResultsTable.setOpaque(false);
        searchResultsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        searchResultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseExited(evt);
            }
        });
        searchResultsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseMoved(evt);
            }
        });
        searchResultsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchResultsTableKeyReleased(evt);
            }
        });
        idResultsTableJScrollPane.setViewportView(searchResultsTable);

        javax.swing.GroupLayout idResultsPanelLayout = new javax.swing.GroupLayout(idResultsPanel);
        idResultsPanel.setLayout(idResultsPanelLayout);
        idResultsPanelLayout.setHorizontalGroup(
            idResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(idResultsTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 618, Short.MAX_VALUE)
            .addGroup(idResultsPanelLayout.createSequentialGroup()
                .addComponent(spectrumIdResultsLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        idResultsPanelLayout.setVerticalGroup(
            idResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idResultsPanelLayout.createSequentialGroup()
                .addComponent(spectrumIdResultsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(idResultsTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
        );

        colorLegendLabel.setText(" ");

        javax.swing.GroupLayout psmsPanelLayout = new javax.swing.GroupLayout(psmsPanel);
        psmsPanel.setLayout(psmsPanelLayout);
        psmsPanelLayout.setHorizontalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmsPanelLayout.createSequentialGroup()
                .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(psmsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(idResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(peptideShakerJScrollPane)))
                    .addGroup(psmsPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(colorLegendLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        psmsPanelLayout.setVerticalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(idResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorLegendLabel)
                .addGap(6, 6, 6))
        );

        psmsLayeredPane.add(psmsPanel);
        psmsPanel.setBounds(0, 0, 650, 400);

        psmsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        psmsHelpJButton.setToolTipText("Help");
        psmsHelpJButton.setBorder(null);
        psmsHelpJButton.setBorderPainted(false);
        psmsHelpJButton.setContentAreaFilled(false);
        psmsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        psmsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                psmsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                psmsHelpJButtonMouseExited(evt);
            }
        });
        psmsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmsHelpJButtonActionPerformed(evt);
            }
        });
        psmsLayeredPane.add(psmsHelpJButton);
        psmsHelpJButton.setBounds(630, 0, 10, 19);
        psmsLayeredPane.setLayer(psmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmsJButton.setToolTipText("Copy to File");
        exportPsmsJButton.setBorder(null);
        exportPsmsJButton.setBorderPainted(false);
        exportPsmsJButton.setContentAreaFilled(false);
        exportPsmsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmsJButton.setEnabled(false);
        exportPsmsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPsmsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPsmsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPsmsJButtonMouseExited(evt);
            }
        });
        exportPsmsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPsmsJButtonActionPerformed(evt);
            }
        });
        psmsLayeredPane.add(exportPsmsJButton);
        exportPsmsJButton.setBounds(620, 0, 10, 19);
        psmsLayeredPane.setLayer(exportPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPsmsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPsmsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setLayout(contextMenuPsmsBackgroundPanelLayout);
        contextMenuPsmsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPsmsBackgroundPanelLayout.setVerticalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        psmsLayeredPane.add(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setBounds(610, 0, 30, 19);
        psmsLayeredPane.setLayer(contextMenuPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout psmsJPanelLayout = new javax.swing.GroupLayout(psmsJPanel);
        psmsJPanel.setLayout(psmsJPanelLayout);
        psmsJPanelLayout.setHorizontalGroup(
            psmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmsJPanelLayout.createSequentialGroup()
                .addComponent(psmsLayeredPane)
                .addContainerGap())
        );
        psmsJPanelLayout.setVerticalGroup(
            psmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
        );

        psmAndSpectrumSplitPane.setLeftComponent(psmsJPanel);

        spectrumJPanel.setOpaque(false);
        spectrumJPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumJPanelMouseWheelMoved(evt);
            }
        });

        spectrumPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumPanel.setMinimumSize(new java.awt.Dimension(200, 200));
        spectrumPanel.setOpaque(false);

        slidersSplitPane.setBorder(null);
        slidersSplitPane.setDividerLocation(558);
        slidersSplitPane.setDividerSize(0);
        slidersSplitPane.setOpaque(false);

        slidersPanel.setOpaque(false);

        accuracySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        accuracySlider.setPaintTicks(true);
        accuracySlider.setToolTipText("Annotation Accuracy");
        accuracySlider.setValue(100);
        accuracySlider.setOpaque(false);
        accuracySlider.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                accuracySliderMouseWheelMoved(evt);
            }
        });
        accuracySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                accuracySliderStateChanged(evt);
            }
        });

        intensitySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        intensitySlider.setPaintTicks(true);
        intensitySlider.setToolTipText("Annotation Intensity Level");
        intensitySlider.setValue(75);
        intensitySlider.setOpaque(false);
        intensitySlider.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                intensitySliderMouseWheelMoved(evt);
            }
        });
        intensitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                intensitySliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout slidersPanelLayout = new javax.swing.GroupLayout(slidersPanel);
        slidersPanel.setLayout(slidersPanelLayout);
        slidersPanelLayout.setHorizontalGroup(
            slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, slidersPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accuracySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );
        slidersPanelLayout.setVerticalGroup(
            slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, slidersPanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addGap(30, 30, 30)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                .addContainerGap())
        );

        slidersSplitPane.setRightComponent(slidersPanel);

        spectrumJPanel1.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumChartPanel.setOpaque(false);
        spectrumChartPanel.setLayout(new javax.swing.BoxLayout(spectrumChartPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout spectrumJPanel1Layout = new javax.swing.GroupLayout(spectrumJPanel1);
        spectrumJPanel1.setLayout(spectrumJPanel1Layout);
        spectrumJPanel1Layout.setHorizontalGroup(
            spectrumJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumJPanel1Layout.setVerticalGroup(
            spectrumJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanel1Layout.createSequentialGroup()
                .addComponent(spectrumChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        slidersSplitPane.setLeftComponent(spectrumJPanel1);

        javax.swing.GroupLayout spectrumPanelLayout = new javax.swing.GroupLayout(spectrumPanel);
        spectrumPanel.setLayout(spectrumPanelLayout);
        spectrumPanelLayout.setHorizontalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumPanelLayout.setVerticalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumPanelLayout.createSequentialGroup()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );

        spectrumLayeredPane.add(spectrumPanel);
        spectrumPanel.setBounds(0, 0, 590, 420);

        spectrumHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        spectrumHelpJButton.setToolTipText("Help");
        spectrumHelpJButton.setBorder(null);
        spectrumHelpJButton.setBorderPainted(false);
        spectrumHelpJButton.setContentAreaFilled(false);
        spectrumHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        spectrumHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumHelpJButtonMouseExited(evt);
            }
        });
        spectrumHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumHelpJButtonActionPerformed(evt);
            }
        });
        spectrumLayeredPane.add(spectrumHelpJButton);
        spectrumHelpJButton.setBounds(600, 0, 10, 19);
        spectrumLayeredPane.setLayer(spectrumHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSpectrumJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumJButton.setToolTipText("Export");
        exportSpectrumJButton.setBorder(null);
        exportSpectrumJButton.setBorderPainted(false);
        exportSpectrumJButton.setContentAreaFilled(false);
        exportSpectrumJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumJButton.setEnabled(false);
        exportSpectrumJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSpectrumJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseExited(evt);
            }
        });
        spectrumLayeredPane.add(exportSpectrumJButton);
        exportSpectrumJButton.setBounds(590, 0, 10, 19);
        spectrumLayeredPane.setLayer(exportSpectrumJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSpectrumBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSpectrumBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setLayout(contextMenuSpectrumBackgroundPanelLayout);
        contextMenuSpectrumBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuSpectrumBackgroundPanelLayout.setVerticalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        spectrumLayeredPane.add(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setBounds(590, 0, 30, 19);
        spectrumLayeredPane.setLayer(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
        );

        psmAndSpectrumSplitPane.setRightComponent(spectrumJPanel);

        spectrumSelectionPsmSplitPane.setBottomComponent(psmAndSpectrumSplitPane);

        idSoftwareJPanel.setOpaque(false);

        idSoftwarePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Identification Overview"));
        idSoftwarePanel.setOpaque(false);

        vennDiagramButton.setBackground(new java.awt.Color(255, 255, 255));
        vennDiagramButton.setBorderPainted(false);
        vennDiagramButton.setContentAreaFilled(false);
        vennDiagramButton.setFocusable(false);

        overviewPlotsPanel.setOpaque(false);
        overviewPlotsPanel.setLayout(new javax.swing.BoxLayout(overviewPlotsPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout idSoftwarePanelLayout = new javax.swing.GroupLayout(idSoftwarePanel);
        idSoftwarePanel.setLayout(idSoftwarePanelLayout);
        idSoftwarePanelLayout.setHorizontalGroup(
            idSoftwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSoftwarePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewPlotsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1012, Short.MAX_VALUE)
                .addGap(246, 246, 246))
            .addGroup(idSoftwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(idSoftwarePanelLayout.createSequentialGroup()
                    .addContainerGap(1029, Short.MAX_VALUE)
                    .addComponent(vennDiagramButton, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );
        idSoftwarePanelLayout.setVerticalGroup(
            idSoftwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSoftwarePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewPlotsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(idSoftwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(idSoftwarePanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(vennDiagramButton, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        idSoftwareJLayeredPane.add(idSoftwarePanel);
        idSoftwarePanel.setBounds(0, 10, 1280, 150);

        idSoftwareHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        idSoftwareHelpJButton.setToolTipText("Help");
        idSoftwareHelpJButton.setBorder(null);
        idSoftwareHelpJButton.setBorderPainted(false);
        idSoftwareHelpJButton.setContentAreaFilled(false);
        idSoftwareHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        idSoftwareHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                idSoftwareHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                idSoftwareHelpJButtonMouseExited(evt);
            }
        });
        idSoftwareHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idSoftwareHelpJButtonActionPerformed(evt);
            }
        });
        idSoftwareJLayeredPane.add(idSoftwareHelpJButton);
        idSoftwareHelpJButton.setBounds(1270, 0, 10, 19);
        idSoftwareJLayeredPane.setLayer(idSoftwareHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportIdPerformancePerformanceJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportIdPerformancePerformanceJButton.setToolTipText("Copy to File");
        exportIdPerformancePerformanceJButton.setBorder(null);
        exportIdPerformancePerformanceJButton.setBorderPainted(false);
        exportIdPerformancePerformanceJButton.setContentAreaFilled(false);
        exportIdPerformancePerformanceJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportIdPerformancePerformanceJButton.setEnabled(false);
        exportIdPerformancePerformanceJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportIdPerformancePerformanceJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportIdPerformancePerformanceJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportIdPerformancePerformanceJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportIdPerformancePerformanceJButtonMouseExited(evt);
            }
        });
        idSoftwareJLayeredPane.add(exportIdPerformancePerformanceJButton);
        exportIdPerformancePerformanceJButton.setBounds(1260, 0, 10, 19);
        idSoftwareJLayeredPane.setLayer(exportIdPerformancePerformanceJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuIdSoftwareBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuIdSoftwareBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuIdSoftwareBackgroundPanel);
        contextMenuIdSoftwareBackgroundPanel.setLayout(contextMenuIdSoftwareBackgroundPanelLayout);
        contextMenuIdSoftwareBackgroundPanelLayout.setHorizontalGroup(
            contextMenuIdSoftwareBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuIdSoftwareBackgroundPanelLayout.setVerticalGroup(
            contextMenuIdSoftwareBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        idSoftwareJLayeredPane.add(contextMenuIdSoftwareBackgroundPanel);
        contextMenuIdSoftwareBackgroundPanel.setBounds(1260, 0, 30, 19);
        idSoftwareJLayeredPane.setLayer(contextMenuIdSoftwareBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout idSoftwareJPanelLayout = new javax.swing.GroupLayout(idSoftwareJPanel);
        idSoftwareJPanel.setLayout(idSoftwareJPanelLayout);
        idSoftwareJPanelLayout.setHorizontalGroup(
            idSoftwareJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(idSoftwareJLayeredPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1284, Short.MAX_VALUE)
        );
        idSoftwareJPanelLayout.setVerticalGroup(
            idSoftwareJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(idSoftwareJLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1304, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(idSoftwareJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectrumSelectionPsmSplitPane)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 938, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(761, Short.MAX_VALUE)
                    .addComponent(idSoftwareJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectrumSelectionPsmSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 747, Short.MAX_VALUE)
                    .addGap(180, 180, 180)))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Reset the divider between the spectrum table and the spectrum.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        spectrumSelectionPsmSplitPane.setDividerLocation((spectrumSelectionPsmSplitPane.getHeight() / 100) * 34);
        psmAndSpectrumSplitPane.setDividerLocation(psmAndSpectrumSplitPane.getWidth() / 2);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // move the icons
                psmsLayeredPane.getComponent(0).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        psmsLayeredPane.getComponent(0).getWidth(),
                        psmsLayeredPane.getComponent(0).getHeight());

                psmsLayeredPane.getComponent(1).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        psmsLayeredPane.getComponent(1).getWidth(),
                        psmsLayeredPane.getComponent(1).getHeight());

                psmsLayeredPane.getComponent(2).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        psmsLayeredPane.getComponent(2).getWidth(),
                        psmsLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                psmsLayeredPane.getComponent(3).setBounds(0, 0, psmsLayeredPane.getWidth(), psmsLayeredPane.getHeight());
                psmsLayeredPane.revalidate();
                psmsLayeredPane.repaint();

                // move the icons
                idSoftwareJLayeredPane.getComponent(0).setBounds(
                        idSoftwareJLayeredPane.getWidth() - idSoftwareJLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        idSoftwareJLayeredPane.getComponent(0).getWidth(),
                        idSoftwareJLayeredPane.getComponent(0).getHeight());

                idSoftwareJLayeredPane.getComponent(1).setBounds(
                        idSoftwareJLayeredPane.getWidth() - idSoftwareJLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        idSoftwareJLayeredPane.getComponent(1).getWidth(),
                        idSoftwareJLayeredPane.getComponent(1).getHeight());

                idSoftwareJLayeredPane.getComponent(2).setBounds(
                        idSoftwareJLayeredPane.getWidth() - idSoftwareJLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        idSoftwareJLayeredPane.getComponent(2).getWidth(),
                        idSoftwareJLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                idSoftwareJLayeredPane.getComponent(3).setBounds(0, 0, idSoftwareJLayeredPane.getWidth(), idSoftwareJLayeredPane.getHeight());
                idSoftwareJLayeredPane.revalidate();
                idSoftwareJLayeredPane.repaint();

                // move the icons
                spectrumSelectionLayeredPane.getComponent(0).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(0).getWidth() - 34,
                        0,
                        spectrumSelectionLayeredPane.getComponent(0).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(0).getHeight());

                spectrumSelectionLayeredPane.getComponent(1).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(1).getWidth() - 10,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(1).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(1).getHeight());

                spectrumSelectionLayeredPane.getComponent(2).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(2).getWidth() - 20,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(2).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(2).getHeight());

                spectrumSelectionLayeredPane.getComponent(3).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(3).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                spectrumSelectionLayeredPane.getComponent(4).setBounds(0, 0, spectrumSelectionLayeredPane.getWidth(), spectrumSelectionLayeredPane.getHeight());
                spectrumSelectionLayeredPane.revalidate();
                spectrumSelectionLayeredPane.repaint();

                // move the icons
                spectrumLayeredPane.getComponent(0).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        spectrumLayeredPane.getComponent(0).getWidth(),
                        spectrumLayeredPane.getComponent(0).getHeight());

                spectrumLayeredPane.getComponent(1).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        spectrumLayeredPane.getComponent(1).getWidth(),
                        spectrumLayeredPane.getComponent(1).getHeight());

                spectrumLayeredPane.getComponent(2).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        spectrumLayeredPane.getComponent(2).getWidth(),
                        spectrumLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                spectrumLayeredPane.getComponent(3).setBounds(0, 0, spectrumLayeredPane.getWidth(), spectrumLayeredPane.getHeight());
                spectrumLayeredPane.revalidate();
                spectrumLayeredPane.repaint();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // set the sliders split pane divider location
                        if (peptideShakerGUI.getUserPreferences().showSliders()) {
                            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth() - 30);
                        } else {
                            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth());
                        }
                    }
                });
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Update the id results PSM selection.
     *
     * @param evt
     */
    private void searchResultsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchResultsTableKeyReleased
        searchResultsTableMouseClicked(null);
    }//GEN-LAST:event_searchResultsTableKeyReleased

    private void searchResultsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseClicked
        if (searchResultsTable.getSelectedRow() != -1) {
            updateSpectrum();
        }
    }//GEN-LAST:event_searchResultsTableMouseClicked

    /**
     * Update the id results PSM selection.
     *
     * @param evt
     */
    private void searchResultsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseReleased
        if (searchResultsTable.getSelectedRow() != -1) {

            updateSpectrum();

            // open protein links in web browser
            if (evt != null) {
                int row = searchResultsTable.rowAtPoint(evt.getPoint());
                int column = searchResultsTable.columnAtPoint(evt.getPoint());

                if (column == 1) {

                    // open protein links in web browser
                    if (evt.getButton() == MouseEvent.BUTTON1
                            && ((String) searchResultsTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                        peptideShakerGUI.openProteinLinks((String) searchResultsTable.getValueAt(row, column));
                    }
                }
            }
        }
    }//GEN-LAST:event_searchResultsTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseMoved
        int row = peptideShakerJTable.rowAtPoint(evt.getPoint());
        int column = peptideShakerJTable.columnAtPoint(evt.getPoint());

        if (peptideShakerJTable.getValueAt(row, column) != null) {
            if (column == peptideShakerJTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) peptideShakerJTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                peptideShakerJTable.setToolTipText(null);

            } else if (column == peptideShakerJTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                peptideShakerJTable.setToolTipText(peptideShakerJTablePeptideTooltip);
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideShakerJTable.setToolTipText(null);
            }
        }
    }//GEN-LAST:event_peptideShakerJTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerJTableMouseExited

    /**
     * Opens the protein web links if the protein(s) column is selcted.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseReleased

        int row = peptideShakerJTable.rowAtPoint(evt.getPoint());
        int column = peptideShakerJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == 1) {

                // open protein links in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) peptideShakerJTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) peptideShakerJTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_peptideShakerJTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void searchResultsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchResultsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void searchResultsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseMoved
        int row = searchResultsTable.rowAtPoint(evt.getPoint());
        int column = searchResultsTable.columnAtPoint(evt.getPoint());

        if (searchResultsTable.getValueAt(row, column) != null) {

            if (column == searchResultsTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                searchResultsTable.setToolTipText(searchResultsTablePeptideTooltips.get(row));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                searchResultsTable.setToolTipText(null);
            }
        } else {
            searchResultsTable.setToolTipText(null);
        }
    }//GEN-LAST:event_searchResultsTableMouseMoved

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void intensitySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_intensitySliderMouseWheelMoved
        spectrumJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_intensitySliderMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     *
     * @param evt
     */
    private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
        peptideShakerGUI.getAnnotationPreferences().setAnnotationLevel(((Integer) intensitySlider.getValue()) / 100.0);
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
        intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");
        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_intensitySliderStateChanged

    /**
     * Updates the slider values when the user scrolls.
     *
     * @param evt
     */
    private void spectrumJPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumJPanelMouseWheelMoved

        // @TODO: figure out why the strange special cases are needed... 
        //          if not included the slider gets stuck at given values depending on the current max value
        if (evt.isControlDown()) {
            if (evt.getWheelRotation() > 0) { // Down
                accuracySlider.setValue(accuracySlider.getValue() - 1);
            } else { // Up
                int oldValue = accuracySlider.getValue();
                int newValue = accuracySlider.getValue() + 1;
                accuracySlider.setValue(newValue);

                while (oldValue == accuracySlider.getValue()) {
                    accuracySlider.setValue(newValue++);
                }
            }
        } else {
            if (evt.getWheelRotation() > 0) { // Down
                intensitySlider.setValue(intensitySlider.getValue() - 1);
            } else { // Up
                int oldValue = intensitySlider.getValue();
                int newValue = intensitySlider.getValue() + 1;
                intensitySlider.setValue(newValue);

                while (oldValue == intensitySlider.getValue()) {
                    intensitySlider.setValue(newValue++);
                }
            }
        }

        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_spectrumJPanelMouseWheelMoved

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void accuracySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracySliderMouseWheelMoved
        spectrumJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved

    /**
     * Update the fragment ion annotation accuracy.
     *
     * @param evt
     */
    private void accuracySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_accuracySliderStateChanged
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
        peptideShakerGUI.getAnnotationPreferences().setFragmentIonAccuracy(accuracy);
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
        accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_accuracySliderStateChanged

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void psmsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_psmsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void psmsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void psmsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#PSM",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportPsmsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPsmsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPsmsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportPsmsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPsmsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPsmsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportPsmsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPsmsJButtonActionPerformed
        copyTableContentToClipboardOrFile(TableIndex.PSM_TABLES);
    }//GEN-LAST:event_exportPsmsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void idSoftwareHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_idSoftwareHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_idSoftwareHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void idSoftwareHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_idSoftwareHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_idSoftwareHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void idSoftwareHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idSoftwareHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#IdSoftwarePerformance",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_idSoftwareHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportIdPerformancePerformanceJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportIdPerformancePerformanceJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportIdPerformancePerformanceJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportIdPerformancePerformanceJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportIdPerformancePerformanceJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportIdPerformancePerformanceJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void spectrumSelectionHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_spectrumSelectionHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumSelectionHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumSelectionHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void spectrumSelectionHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumSelectionHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#SpectrumSelection",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumSelectionHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportSpectrumSelectionJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumSelectionJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportSpectrumSelectionJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportSpectrumSelectionJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumSelectionJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportSpectrumSelectionJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportSpectrumSelectionJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumSelectionJButtonActionPerformed
        copyTableContentToClipboardOrFile(TableIndex.SPECTRUM_FILES);
    }//GEN-LAST:event_exportSpectrumSelectionJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void spectrumHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void spectrumHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#Spectrum",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportSpectrumJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportSpectrumJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportIdPerformancePerformanceJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportIdPerformancePerformanceJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Plots");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI, true, overviewPlotsPanel);
            }
        });

        popupMenu.add(menuItem);

        popupMenu.show(exportIdPerformancePerformanceJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportIdPerformancePerformanceJButtonMouseReleased

    /**
     * Export the spectrum.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Spectrum");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerGUI.exportSpectrumAsFigure();
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Spectrum As MGF");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerGUI.exportSpectrumAsMgf();
            }
        });

        popupMenu.add(menuItem);

        popupMenu.show(exportSpectrumJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportSpectrumJButtonMouseReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void spectrumTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumTableKeyReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableKeyReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void spectrumTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseMoved

        int row = spectrumTable.rowAtPoint(evt.getPoint());
        int column = spectrumTable.columnAtPoint(evt.getPoint());

        if (spectrumTable.getValueAt(row, column) != null) {
            if (column == spectrumTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) spectrumTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                spectrumTable.setToolTipText(null);

            } else if (column == spectrumTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                String spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumFactory.getSpectrumTitles(fileSelected).get(spectrumTable.convertRowIndexToModel(row)));

                // check if we ought to show a tooltip with mod details
                if (identification.matchExists(spectrumKey)) {
                    try {
                        DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        String tooltip = displayFeaturesGenerator.getPeptideModificationTooltipAsHtml(spectrumMatch.getBestPeptideAssumption().getPeptide());
                        spectrumTable.setToolTipText(tooltip);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                }
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                spectrumTable.setToolTipText(null);
            }
        }
    }//GEN-LAST:event_spectrumTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumTableMouseExited

    /**
     * Show the statisics popup menu.
     *
     * @param evt
     */
    private void spectrumTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3 && spectrumTable.getRowCount() > 0) {

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Statistics (beta)");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new XYPlottingDialog(peptideShakerGUI, spectrumTable, spectrumTableToolTips,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
                }
            });
            popupMenu.add(menuItem);
            popupMenu.show(spectrumTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_spectrumTableMouseClicked

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void spectrumTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseReleased

        int row = spectrumTable.getSelectedRow();
        int column = spectrumTable.getSelectedColumn();

        if (evt.getButton() == MouseEvent.BUTTON1 && row != -1 && column != -1) {

            // open protein link in web browser
            if (column == spectrumTable.getColumn("Protein(s)").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) spectrumTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) spectrumTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }

        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableMouseReleased

    /**
     * Updates the spectrum table based on the currently selected mgf file.
     *
     * @param evt
     */
    private void fileNamesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNamesCmbActionPerformed

        spectrumSelectionDialog.setVisible(false);

        if (updateSelection) {
            clearItemSelection();
            fileSelectionChanged();
        }
    }//GEN-LAST:event_fileNamesCmbActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void spectrumSelectionOptionsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionOptionsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_spectrumSelectionOptionsJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumSelectionOptionsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionOptionsJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumSelectionOptionsJButtonMouseExited

    /**
     * Open the spectrum file selection drop down menu.
     *
     * @param evt
     */
    private void spectrumSelectionOptionsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionOptionsJButtonMouseReleased
        spectrumSelectionDialog.setSize(400, 65);
        spectrumSelectionDialog.setLocationRelativeTo(spectrumSelectionOptionsJButton);
        spectrumSelectionDialog.setVisible(true);
    }//GEN-LAST:event_spectrumSelectionOptionsJButtonMouseReleased

    private void spectrumSelectionPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionPanelMouseReleased
        if (evt.getX() > 180 && evt.getX() < 400 && evt.getY() < 25) {
            spectrumSelectionDialog.setSize(400, 65);
            spectrumSelectionDialog.setLocation(evt.getLocationOnScreen());
            spectrumSelectionDialog.setVisible(true);
        }
    }//GEN-LAST:event_spectrumSelectionPanelMouseReleased

    /**
     * Shows a tooltip when the mouse is over the spectrum selection link.
     *
     * @param evt
     */
    private void spectrumSelectionPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionPanelMouseMoved
        if (evt.getX() > 180 && evt.getX() < 400 && evt.getY() < 25) {
            spectrumSelectionPanel.setToolTipText("Select Spectrum File");
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            spectrumSelectionPanel.setToolTipText(null);
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_spectrumSelectionPanelMouseMoved

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel colorLegendLabel;
    private javax.swing.JPanel contextMenuIdSoftwareBackgroundPanel;
    private javax.swing.JPanel contextMenuPsmsBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumSelectionBackgroundPanel;
    private javax.swing.JButton exportIdPerformancePerformanceJButton;
    private javax.swing.JButton exportPsmsJButton;
    private javax.swing.JButton exportSpectrumJButton;
    private javax.swing.JButton exportSpectrumSelectionJButton;
    private javax.swing.JComboBox fileNamesCmb;
    private javax.swing.JPanel idResultsPanel;
    private javax.swing.JScrollPane idResultsTableJScrollPane;
    private javax.swing.JButton idSoftwareHelpJButton;
    private javax.swing.JLayeredPane idSoftwareJLayeredPane;
    private javax.swing.JPanel idSoftwareJPanel;
    private javax.swing.JPanel idSoftwarePanel;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel overviewPlotsPanel;
    private javax.swing.JScrollPane peptideShakerJScrollPane;
    private javax.swing.JTable peptideShakerJTable;
    private javax.swing.JSplitPane psmAndSpectrumSplitPane;
    private javax.swing.JButton psmsHelpJButton;
    private javax.swing.JPanel psmsJPanel;
    private javax.swing.JLayeredPane psmsLayeredPane;
    private javax.swing.JPanel psmsPanel;
    private javax.swing.JTable searchResultsTable;
    private javax.swing.JPanel slidersPanel;
    private javax.swing.JSplitPane slidersSplitPane;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JPanel spectrumChartPanel;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JLabel spectrumIdResultsLabel;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JPanel spectrumJPanel1;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JLayeredPane spectrumLayeredPane;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JDialog spectrumSelectionDialog;
    private javax.swing.JButton spectrumSelectionHelpJButton;
    private javax.swing.JPanel spectrumSelectionJPanel;
    private javax.swing.JLayeredPane spectrumSelectionLayeredPane;
    private javax.swing.JButton spectrumSelectionOptionsJButton;
    private javax.swing.JPanel spectrumSelectionPanel;
    private javax.swing.JSplitPane spectrumSelectionPsmSplitPane;
    private javax.swing.JTable spectrumTable;
    private javax.swing.JScrollPane spectrumTableJScrollPane;
    private javax.swing.JButton vennDiagramButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays the results in the panel.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Data. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {
            @Override
            public void run() {

                try {
                    identification = peptideShakerGUI.getIdentification();

                    // now we have data and can update the jsparklines depending on this
                    spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                            (double) ((PSMaps) identification.getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge(), peptideShakerGUI.getSparklineColor()));
                    spectrumTable.getColumn("Int").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                            SpectrumFactory.getInstance().getMaxIntensity(), peptideShakerGUI.getSparklineColor()));
                    spectrumTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                            SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 20);
                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).setLogScale(true);
                    ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
                    ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

                    ArrayList<String> spectrumFileNames = identification.getSpectrumFiles();
                    String[] filesArray = new String[spectrumFileNames.size()];
                    int cpt = 0;

                    for (String tempName : spectrumFileNames) {
                        filesArray[cpt++] = tempName;
                    }

                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setMaxPrimaryProgressCounter(identification.getSpectrumIdentificationSize());
                    progressDialog.setValue(0);

                    // get the list of id software used
                    IdfileReaderFactory idFileReaderFactory = IdfileReaderFactory.getInstance(); // @TODO: this should be done when the files are loaded?
                    ArrayList<File> idFiles = peptideShakerGUI.getProjectDetails().getIdentificationFiles();
                    advocatesUsed = new ArrayList<Advocate>();

                    for (int i = 0; i < idFiles.size(); i++) {
                        if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == Advocate.OMSSA.getIndex()) {
                            if (!advocatesUsed.contains(Advocate.OMSSA)) {
                                advocatesUsed.add(Advocate.OMSSA);
                            }
                        } else if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == Advocate.XTandem.getIndex()) {
                            if (!advocatesUsed.contains(Advocate.XTandem)) {
                                advocatesUsed.add(Advocate.XTandem);
                            }
                        } else if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == Advocate.Mascot.getIndex()) {
                            if (!advocatesUsed.contains(Advocate.Mascot)) {
                                advocatesUsed.add(Advocate.Mascot);
                            }
                        } else if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == Advocate.MSGF.getIndex()) {
                            if (!advocatesUsed.contains(Advocate.MSGF)) {
                                advocatesUsed.add(Advocate.MSGF);
                            }
                        } else if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == Advocate.msAmanda.getIndex()) {
                            if (!advocatesUsed.contains(Advocate.msAmanda)) {
                                advocatesUsed.add(Advocate.msAmanda);
                            }
                        }
                    }

                    // order the advocates to have the same order is in the overview plots
                    Collections.sort(advocatesUsed);

                    // update the advocates color legend
                    ArrayList<Advocate> usedAdvocatedAndPeptideShaker = new ArrayList<Advocate>();
                    usedAdvocatedAndPeptideShaker.addAll(advocatesUsed);
                    usedAdvocatedAndPeptideShaker.add(Advocate.PeptideShaker);
                    String colorLegend = "<html>";
                    for (Advocate tempAdvocate : usedAdvocatedAndPeptideShaker) {
                        colorLegend += "<font color=\"rgb(" + searchEnginesColorMap.get(tempAdvocate.getIndex()).getRed() + ","
                                + searchEnginesColorMap.get(tempAdvocate.getIndex()).getGreen() + ","
                                + searchEnginesColorMap.get(tempAdvocate.getIndex()).getBlue() + ")\">&#9632;</font> "
                                + tempAdvocate.getName() + " &nbsp;";
                    }
                    colorLegend += "</html>";
                    colorLegendLabel.setText(colorLegend);

                    HashMap<Advocate, Double> totalAdvocateId = new HashMap<Advocate, Double>();
                    HashMap<Advocate, Double> uniqueAdvocateId = new HashMap<Advocate, Double>();

                    for (Advocate tempAdvocate : advocatesUsed) {
                        totalAdvocateId.put(tempAdvocate, 0.0);
                        uniqueAdvocateId.put(tempAdvocate, 0.0);
                    }

                    int dataA = 0, dataB = 0, dataC = 0, dataAB = 0, dataAC = 0, dataBC = 0, dataABC = 0;
                    int totalNumberOfSpectra = 0, totalPeptideShakerIds = 0;

                    // venn diagram data
                    ArrayList<Advocate> vennDiagramAdvocates = new ArrayList<Advocate>(); // @TODO: should be possible to change by the user...
                    for (int i = 0; i < advocatesUsed.size() && i < 3; i++) {
                        vennDiagramAdvocates.add(advocatesUsed.get(i));
                    }

                    int fileCounter = 1;
                    PSParameter probabilities = new PSParameter();

                    numberOfValidatedPsmsMap = new HashMap<String, Integer>();

                    for (String fileName : filesArray) {

                        int numberOfValidatedPsms = 0;
                        totalNumberOfSpectra += spectrumFactory.getNSpectra(fileName);

                        progressDialog.setTitle("Loading Spectrum Information. Please Wait... (" + fileCounter + "/" + filesArray.length + ")");
                        identification.loadSpectrumMatchParameters(fileName, probabilities, progressDialog);
                        progressDialog.setTitle("Loading Spectrum Matches. Please Wait... (" + fileCounter + "/" + filesArray.length + ")");
                        identification.loadSpectrumMatches(fileName, progressDialog);
                        progressDialog.setTitle("Loading Data. Please Wait... (" + fileCounter++ + "/" + filesArray.length + ") ");

                        for (String spectrumKey : identification.getSpectrumIdentification(fileName)) {
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            ArrayList<Advocate> currentAdvocates = new ArrayList<Advocate>();
                            probabilities = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, probabilities);

                            if (probabilities.getMatchValidationLevel().isValidated()) {

                                totalPeptideShakerIds++;
                                numberOfValidatedPsms++;

                                for (Advocate tempAdvocate : advocatesUsed) {
                                    if (spectrumMatch.getFirstHit(tempAdvocate.getIndex()) != null) {
                                        PeptideAssumption firstHit = (PeptideAssumption) spectrumMatch.getFirstHit(tempAdvocate.getIndex());
                                        if (firstHit.getPeptide().isSameSequenceAndModificationStatus(spectrumMatch.getBestPeptideAssumption().getPeptide(),
                                                PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy())) {
                                            currentAdvocates.add(tempAdvocate);
                                        }
                                    }
                                }
                            }

                            // get the venn diagram data
                            if (vennDiagramAdvocates.size() == 3) {
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(0))
                                        && currentAdvocates.contains(vennDiagramAdvocates.get(1))
                                        && currentAdvocates.contains(vennDiagramAdvocates.get(2))) {
                                    dataABC++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(0)) && currentAdvocates.contains(vennDiagramAdvocates.get(1))) {
                                    dataAB++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(0)) && currentAdvocates.contains(vennDiagramAdvocates.get(2))) {
                                    dataAC++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(1)) && currentAdvocates.contains(vennDiagramAdvocates.get(2))) {
                                    dataBC++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(0))) {
                                    dataA++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(1))) {
                                    dataB++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(2))) {
                                    dataC++;
                                }
                            } else if (vennDiagramAdvocates.size() == 2) {
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(0)) && currentAdvocates.contains(vennDiagramAdvocates.get(1))) {
                                    dataAB++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(0))) {
                                    dataA++;
                                }
                                if (currentAdvocates.contains(vennDiagramAdvocates.get(1))) {
                                    dataB++;
                                }
                            }

                            // overview plot data
                            for (Advocate tempAdvocate : advocatesUsed) {
                                if (currentAdvocates.contains(tempAdvocate)) {
                                    totalAdvocateId.put(tempAdvocate, totalAdvocateId.get(tempAdvocate) + 1);

                                    if (currentAdvocates.size() == 1) {
                                        uniqueAdvocateId.put(tempAdvocate, uniqueAdvocateId.get(tempAdvocate) + 1);
                                    }
                                }
                            }

                            progressDialog.increasePrimaryProgressCounter();
                        }

                        numberOfValidatedPsmsMap.put(fileName, numberOfValidatedPsms);
                    }

                    if (!progressDialog.isRunCanceled()) {

                        progressDialog.setPrimaryProgressCounterIndeterminate(true);
                        progressDialog.setTitle("Updating Tables. Please Wait...");

                        // update the venn diagram
                        updateVennDiagram(dataA, dataB, dataC,
                                dataAB, dataAC, dataBC, dataABC,
                                vennDiagramAdvocates);

                        // add the peptide shaker results
                        totalAdvocateId.put(Advocate.PeptideShaker, (double) totalPeptideShakerIds);
                        uniqueAdvocateId.put(Advocate.PeptideShaker, 0.0);

                        // update the id software performance plots
                        updateOverviewPlots(totalAdvocateId, uniqueAdvocateId, totalNumberOfSpectra);

                        showSparkLines(peptideShakerGUI.showSparklines());
                        progressDialog.setTitle("Updating Spectrum Table. Please Wait...");
                        fileNamesCmb.setModel(new DefaultComboBoxModel(filesArray));

                        // update the slider tooltips
                        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
                        accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
                        intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");

                        //formComponentResized(null);
                        // enable the contextual export options
                        exportIdPerformancePerformanceJButton.setEnabled(true);
                        exportSpectrumSelectionJButton.setEnabled(true);
                        exportSpectrumJButton.setEnabled(true);
                        exportPsmsJButton.setEnabled(true);

                        peptideShakerGUI.setUpdated(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX, true);
                    }

                    boolean processCancelled = progressDialog.isRunCanceled();

                    progressDialog.setRunFinished();

                    if (!processCancelled) {
                        fileSelectionChanged();
                    }

                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    peptideShakerGUI.catchException(e);
                }
            }
        }.start();
    }

    /**
     * Create a Venn diagram and add it to the given button.
     *
     * @param diagramButton the button to add the diagram to
     * @param a the size of A
     * @param b the size of B
     * @param c the size of C
     * @param ab the overlap of A and B
     * @param ac the overlap of A and C
     * @param bc the overlap of B and C
     * @param abc the number of values in A, B and C
     * @param titleA the title of dataset A
     * @param titleB the title of dataset B
     * @param titleC the title of dataset C
     */
    private void updateThreeWayVennDiagram(JButton diagramButton, int a, int b, int c, int ab, int ac, int bc, int abc,
            String titleA, String titleB, String titleC, Color colorA, Color colorB, Color colorC) {

        double maxValue = Math.max(Math.max(a, b), c);
        if (maxValue < 1) {
            maxValue = 1;
        }

        // @TODO: move this method to utilities?
        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, c / maxValue, ab / maxValue, ac / maxValue, bc / maxValue, abc / maxValue);

        // @TODO: remove the hardcoding below!!!
        if (diagramButton.getWidth() == 0) {
            chart.setSize(173, 105);
        } else {
            chart.setSize(diagramButton.getWidth(), diagramButton.getHeight() - 10);
        }

        chart.setCircleLegends(titleA, titleB, titleC);
        chart.setCircleColors(colorA, colorB, colorC);

        try {
            diagramButton.setText("");
            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));

            if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
                diagramButton.setToolTipText("Not available in off line mode");
            } else {
                diagramButton.setIcon(icon);

                diagramButton.setToolTipText("<html>"
                        + titleA + ": " + a + "<br>"
                        + titleB + ": " + b + "<br>"
                        + titleC + ": " + c + "<br><br>"
                        + titleA + " & " + titleB + ": " + ab + "<br>"
                        + titleA + " & " + titleC + ": " + ac + "<br>"
                        + titleB + " & " + titleC + ": " + bc + "<br><br>"
                        + titleA + " & " + titleB + " & " + titleC + ": " + abc
                        + "</html>");
            }
        } catch (IOException e) {
            e.printStackTrace();
            diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
            diagramButton.setToolTipText("Not available due to an error occuring");
        }
    }

    /**
     * Create a Venn diagram and add it to the given button.
     *
     * @param diagramButton the button to add the diagram to
     * @param a the size of A
     * @param b the size of B
     * @param ab the overlap of A and B
     * @param titleA the title of dataset A
     * @param titleB the title of dataset B
     */
    private void updateTwoWayVennDiagram(JButton diagramButton, int a, int b, int ab, String titleA, String titleB, Color colorA, Color colorB) {

        double maxValue = Math.max(a, b);
        if (maxValue < 1) {
            maxValue = 1;
        }

        // @TODO: move this method to utilities?
        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, 0, ab / maxValue, 0, 0, 0);

        // @TODO: remove the hardcoding below!!!
        try {
            if (diagramButton.getWidth() == 0) {
                chart.setSize(173, 105);
            } else {
                chart.setSize(diagramButton.getWidth(), diagramButton.getHeight() - 10);
            }

            chart.setCircleLegends(titleA, titleB, "");
            chart.setCircleColors(colorA, colorB, Color.newColor(Util.color2Hex(diagramButton.getBackground())));

            diagramButton.setText("");
            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));

            if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
                diagramButton.setToolTipText("Not available in off line mode");
            } else {
                diagramButton.setIcon(icon);

                diagramButton.setToolTipText("<html>"
                        + titleA + ": " + a + "<br>"
                        + titleB + ": " + b + "<br>"
                        + titleA + " & " + titleB + ": " + ab
                        + "</html>");
            }
        } catch (IOException e) {
            e.printStackTrace();
            diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
            diagramButton.setToolTipText("Not available due to an error occuring");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
            diagramButton.setToolTipText("Not available due to an error occuring");
        }
    }

    /**
     * Method called whenever the file selection changed.
     */
    private void fileSelectionChanged() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Updating Spectrum Table. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                DefaultTableModel dm = (DefaultTableModel) spectrumTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                fileSelected = (String) fileNamesCmb.getSelectedItem();
                double maxMz = spectrumFactory.getMaxMz(fileSelected);
                try {
                    progressDialog.setTitle("Loading Spectrum Information for " + fileSelected + ". Please Wait...");
                    identification.loadSpectrumMatchParameters(fileSelected, new PSParameter(), progressDialog);
                    identification.loadSpectrumMatches(fileSelected, progressDialog);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                if (!progressDialog.isRunCanceled()) {
                    ((TitledBorder) spectrumSelectionPanel.getBorder()).setTitle("<html>" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING_HTML + "Spectrum Selection ("
                            + numberOfValidatedPsmsMap.get(fileSelected) + "/" + spectrumFactory.getNSpectra(fileSelected) + " - "
                            + "<a href=\"dummy\">" + fileSelected + "</a>)"
                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING_HTML + "</html>");
                    spectrumSelectionPanel.repaint();

                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).setMaxValue(maxMz);

                    updateSelection();
                    peptideShakerGUI.mgfFileSelectionChanged(fileSelected);
                    //repaint();
                    spectrumTable.requestFocus();
                }

                progressDialog.setRunFinished();
            }
        });
    }

    /**
     * Returns the key of the currently selected spectrum.
     *
     * @return the key of the currently selected spectrum
     */
    public String getSelectedSpectrumKey() {
        return Spectrum.getSpectrumKey(fileSelected, spectrumFactory.getSpectrumTitles(fileSelected).get(spectrumTable.convertRowIndexToModel(spectrumTable.getSelectedRow())));
    }

    /**
     * Updates the spectrum selected according to the user's last selection.
     */
    public void updateSelection() {

        String spectrumKey = peptideShakerGUI.getSelectedPsmKey();

        if (spectrumKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            spectrumTable.setRowSelectionInterval(0, 0);
            spectrumTable.scrollRectToVisible(spectrumTable.getCellRect(0, 0, false));
            spectrumSelectionChanged();
        } else {
            selectSpectrum(spectrumKey);
        }
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected PSM.
     */
    private void newItemSelection() {
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, getSelectedSpectrumKey());
    }

    /**
     * Clears the currently selected PSM.
     */
    private void clearItemSelection() {
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION);
    }

    /**
     * Select the given spectrum.
     */
    private void selectSpectrum(String spectrumKey) {

        // change the peptide shaker icon to a "waiting version"
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        String fileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        if (!((String) fileNamesCmb.getSelectedItem()).equalsIgnoreCase(fileName)) {
            updateSelection = false;
            fileNamesCmb.setSelectedItem(fileName);
            updateSelection = true;
            fileSelected = (String) fileNamesCmb.getSelectedItem();
        }

        int line = spectrumFactory.getSpectrumTitles(fileSelected).indexOf(spectrumTitle);

        if (line >= 0) {

            // @TODO: this does not work when the table is sorted!!!
            spectrumTable.setRowSelectionInterval(line, line);
            spectrumTable.scrollRectToVisible(spectrumTable.getCellRect(line, 0, false));
            spectrumSelectionChanged();
        }

        // change the peptide shaker icon to a "waiting version"
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
    }

    /**
     * Method called whenever the spectrum selection changed.
     */
    private void spectrumSelectionChanged() {

        if (spectrumTable.getSelectedRow() != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                // empty the tables
                DefaultTableModel dm = (DefaultTableModel) peptideShakerJTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                dm = (DefaultTableModel) searchResultsTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                searchResultsTablePeptideTooltips = new ArrayList<String>();
                String key = getSelectedSpectrumKey();

                if (identification.matchExists(key)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(key);
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getSpectrumMatchParameter(key, probabilities);

                    // fill peptide shaker table
                    DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                    String proteins = displayFeaturesGenerator.addDatabaseLinks(spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy()));

                    ((DefaultTableModel) peptideShakerJTable.getModel()).addRow(new Object[]{
                        1,
                        isBestPsmEqualForAllIdSoftwares(spectrumMatch, peptideShakerGUI.getSearchParameters()),
                        displayFeaturesGenerator.getTaggedPeptideSequence(spectrumMatch, true, true, true),
                        proteins,
                        probabilities.getPsmConfidence(),
                        probabilities.getMatchValidationLevel().getIndex()
                    });

                    peptideShakerJTablePeptideTooltip = displayFeaturesGenerator.getPeptideModificationTooltipAsHtml(spectrumMatch.getBestPeptideAssumption().getPeptide());
                    searchResultsPeptideKeys = new ArrayList<PeptideAssumption>();

                    // add the search results
                    for (Advocate tempAdvocate : advocatesUsed) {
                        if (spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()) != null) {
                            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()).keySet());
                            Collections.sort(eValues);
                            for (double eValue : eValues) {
                                for (SpectrumIdentificationAssumption currentAssumption : spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()).get(eValue)) {
                                    addIdResultsToTable(currentAssumption, probabilities, tempAdvocate);
                                }
                            }
                        }
                    }

                    // correct table index column
                    for (int i = 0; i < searchResultsTable.getRowCount(); i++) {
                        ((DefaultTableModel) searchResultsTable.getModel()).setValueAt(i + 1, i, 0);
                    }

                    ((DefaultTableModel) peptideShakerJTable.getModel()).fireTableDataChanged();
                    ((DefaultTableModel) searchResultsTable.getModel()).fireTableDataChanged();

                    // select one of the matches
                    if (searchResultsTable.getRowCount() > 0) {
                        searchResultsTable.setRowSelectionInterval(0, 0);
                    }

                    peptideShakerJTable.revalidate();
                    peptideShakerJTable.repaint();
                    searchResultsTable.revalidate();
                    searchResultsTable.repaint();
                }

                newItemSelection();

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //update the spectrum
                        updateSpectrum();
                    }
                });

            } catch (Exception e) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Update the spectrum based on the currently selected PSM.
     */
    public void updateSpectrum() {

        if (spectrumTable.getSelectedRow() != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                spectrumChartPanel.removeAll();

                String key = getSelectedSpectrumKey();
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(key);
                SpectrumPanel tempSpectrumPanel = null;
                AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

                if (currentSpectrum != null) {
                    Precursor precursor = currentSpectrum.getPrecursor();
                    String charge;
                    if (identification.matchExists(currentSpectrumKey)) {
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(currentSpectrumKey);
                        charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString();
                    } else {
                        charge = precursor.getPossibleChargesAsString();
                    }
                    if (currentSpectrum.getMzValuesAsArray().length > 0 && currentSpectrum.getIntensityValuesAsArray().length > 0) {
                        tempSpectrumPanel = new SpectrumPanel(
                                currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                                precursor.getMz(), charge,
                                "", 40, false, false, false, 2, false);
                        tempSpectrumPanel.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                        tempSpectrumPanel.setDeltaMassWindow(annotationPreferences.getFragmentIonAccuracy());
                        tempSpectrumPanel.setBorder(null);
                        tempSpectrumPanel.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                        tempSpectrumPanel.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                        tempSpectrumPanel.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                        tempSpectrumPanel.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());
                    }
                }

                if (identification.matchExists(key)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(key);

                    int forwardIon = peptideShakerGUI.getSearchParameters().getIonSearched1();
                    int rewindIon = peptideShakerGUI.getSearchParameters().getIonSearched2();

                    if (currentSpectrum != null && tempSpectrumPanel != null) {

                        if (currentSpectrum.getMzValuesAsArray().length > 0 && currentSpectrum.getIntensityValuesAsArray().length > 0) {

                            if (searchResultsTable.getSelectedRow() != -1) {
                                PeptideAssumption currentPeptideAssumption = searchResultsPeptideKeys.get(searchResultsTable.getSelectedRow());

                                if (currentPeptideAssumption != null) {
                                    annotationPreferences.setCurrentSettings(currentPeptideAssumption, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()),
                                            PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
                                    ArrayList<IonMatch> annotations = specificAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                            annotationPreferences.getNeutralLosses(),
                                            annotationPreferences.getValidatedCharges(),
                                            currentPeptideAssumption.getIdentificationCharge().value,
                                            currentSpectrum, currentPeptideAssumption.getPeptide(),
                                            currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                            annotationPreferences.getFragmentIonAccuracy(), false, annotationPreferences.isHighResolutionAnnotation());
                                    currentSpectrumKey = spectrumMatch.getKey();

                                    // add the spectrum annotations
                                    tempSpectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                                    tempSpectrumPanel.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                                    tempSpectrumPanel.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                                    // add de novo sequencing
                                    tempSpectrumPanel.addAutomaticDeNovoSequencing(currentPeptideAssumption.getPeptide(), annotations,
                                            forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                            annotationPreferences.showForwardIonDeNovoTags(),
                                            annotationPreferences.showRewindIonDeNovoTags());

                                    peptideShakerGUI.updateAnnotationMenus(currentPeptideAssumption.getIdentificationCharge().value, currentPeptideAssumption.getPeptide());

                                    // update the spectrum title
                                    String modifiedSequence = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(currentPeptideAssumption.getPeptide(), false, false, true);
                                    ((TitledBorder) spectrumPanel.getBorder()).setTitle(
                                            PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                            + "Spectrum & Fragment Ions (" + modifiedSequence
                                            + "   " + currentPeptideAssumption.getIdentificationCharge().toString() + "   "
                                            + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 2) + " m/z)"
                                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                                    spectrumPanel.repaint();
                                }
                            }
                        }
                    }
                } else {
                    // update the spectrum title
                    ((TitledBorder) spectrumPanel.getBorder()).setTitle(
                            PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                            + "Spectrum & Fragment Ions ("
                            + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 2) + " m/z)"
                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                    spectrumPanel.repaint();
                }

                if (tempSpectrumPanel != null) {
                    spectrumChartPanel.add(tempSpectrumPanel);
                }

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            } catch (Exception e) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideShakerGUI.catchException(e);
            }
        }

        spectrumChartPanel.revalidate();
        spectrumChartPanel.repaint();
    }

    /**
     * Returns the spectrum panel.
     *
     * @return the spectrum panel
     */
    public Component getSpectrum() {
        return (Component) spectrumChartPanel.getComponent(0);
    }

    /**
     * Returns the current spectrum as an mgf string.
     *
     * @return the current spectrum as an mgf string
     */
    public String getSpectrumAsMgf() {

        if (spectrumTable.getSelectedRow() != -1) {
            String spectrumKey = getSelectedSpectrumKey();
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

            if (currentSpectrum != null) {
                return currentSpectrum.asMgf();
            }
        }

        return null;
    }

    /**
     * Makes sure that the annotation menu bar is visible.
     */
    public void showSpectrumAnnotationMenu() {
        spectrumAnnotationMenuPanel.removeAll();
        spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
        peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false, false);
    }

    /**
     * Set the intensity slider value.
     *
     * @param value the intensity slider value
     */
    public void setIntensitySliderValue(int value) {
        intensitySlider.setValue(value);
    }

    /**
     * Set the accuracy slider value.
     *
     * @param value the accuracy slider value
     */
    public void setAccuracySliderValue(int value) {
        accuracySlider.setValue(value);
    }

    /**
     * Update the PTM color coding.
     */
    public void updatePtmColors() {
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        spectrumSelectionChanged();
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Export the table contents to the clipboard.
     *
     * @param index
     */
    private void copyTableContentToClipboardOrFile(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.SPECTRUM_FILES || tableIndex == TableIndex.PSM_TABLES) {

            // get the file to send the output to
            final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);

            if (selectedFile != null) {
                try {
                    final BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));

                    progressDialog = new ProgressDialogX(peptideShakerGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);
                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Copying to File. Please Wait...");

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
                                if (tableIndex == TableIndex.SPECTRUM_FILES) {
                                    Util.tableToFile(spectrumTable, "\t", progressDialog, true, writer);
                                } else if (tableIndex == TableIndex.PSM_TABLES) {
                                    Util.tableToFile(searchResultsTable, "\t", progressDialog, true, writer);
                                }

                                writer.close();

                                boolean processCancelled = progressDialog.isRunCanceled();

                                progressDialog.setRunFinished();

                                if (!processCancelled) {
                                    JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to file:\n" + selectedFile.getPath(), "Copied to File", JOptionPane.INFORMATION_MESSAGE);
                                }

                            } catch (IOException e) {
                                progressDialog.setRunFinished();
                                JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } catch (IOException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method called whenever the component is resized to maintain the look of
     * the GUI.
     */
    public void updateSeparators() {
        // set the sliders split pane divider location
        if (peptideShakerGUI.getUserPreferences().showSliders()) {
            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth() - 30);
        } else {
            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth());
        }
    }

    /**
     * Updates and displays the current spectrum slider tooltip.
     */
    private void updateSpectrumSliderToolTip() {
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();

        spectrumJPanel.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " Da<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumJPanel, 0, 0, 0,
                        spectrumJPanel.getWidth() - 150, spectrumJPanel.getY() + 20, // X-Y of the mouse for the tool tip
                        0, false));
    }

    /**
     * Returns true if all the used id software agree on the top PSM without
     * accounting for modification localization, false otherwise.
     *
     * @param spectrumMatch the PSM to check
     * @param searchParameters the parameters used for the search
     *
     * @return true if all the used id software agree on the top PSM
     */
    public static int isBestPsmEqualForAllIdSoftwares(SpectrumMatch spectrumMatch, SearchParameters searchParameters) {

        // @TODO: the values should be stored and resued?
        HashMap<Advocate, Peptide> peptides = new HashMap<Advocate, Peptide>();
        HashMap<Advocate, Integer> charges = new HashMap<Advocate, Integer>();
        ArrayList<Advocate> tempUsedAdvocates = new ArrayList<Advocate>();

        for (Advocate tempAdvocate : Advocate.values()) {
            if (spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()) != null) {
                ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()).keySet());
                Collections.sort(eValues);

                if (eValues.size() > 0) {
                    if (spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()).get(eValues.get(0)).size() > 0) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumMatch.getAllAssumptions(tempAdvocate.getIndex()).get(eValues.get(0)).get(0);
                        peptides.put(tempAdvocate, peptideAssumption.getPeptide());
                        charges.put(tempAdvocate, peptideAssumption.getIdentificationCharge().value);

                        if (!tempUsedAdvocates.contains(tempAdvocate)) {
                            tempUsedAdvocates.add(tempAdvocate);
                        }
                    }
                }
            }
        }

        // check if all advocates are used
        boolean allAdvocatesFound = tempUsedAdvocates.size() == peptides.size();

        if (peptides.isEmpty()) {
            return NO_ID; // no ids found
        } else if (allAdvocatesFound && peptides.size() == 1) {
            return AGREEMENT_WITH_MODS; // only one search engine used
        } else {

            if (allAdvocatesFound) {

                Iterator<Advocate> iterator = peptides.keySet().iterator();
                Advocate firstAdvocate = iterator.next();
                Peptide firstPeptide = peptides.get(firstAdvocate);
                int firstCharge = charges.get(firstAdvocate);
                boolean sameSequenceAndModificationStatus = true;
                boolean sameModifications = true;
                boolean sameCharge = true;

                // iterate all the peptides and charges
                while (iterator.hasNext() && sameSequenceAndModificationStatus && sameModifications && sameCharge) {

                    Advocate currentAdvocate = iterator.next();

                    // check for same same sequence, modification and charge status
                    Peptide currentPeptide = peptides.get(currentAdvocate);
                    sameSequenceAndModificationStatus = firstPeptide.isSameSequenceAndModificationStatus(currentPeptide, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

                    // check the charge
                    int currentCharge = charges.get(currentAdvocate);
                    sameCharge = currentCharge == firstCharge;

                    // check the general modification properties
                    sameModifications = firstPeptide.sameModificationsAs(currentPeptide);
                }

                if (sameSequenceAndModificationStatus && sameCharge) {
                    if (sameModifications) {
                        return AGREEMENT_WITH_MODS;
                    } else {
                        return AGREEMENT;
                    }
                } else {
                    return CONFLICT;
                }
            } else {
                return PARTIALLY_MISSING;
            }
        }
    }

    /**
     * Table model for the table listing all spectra.
     */
    private class SpectrumTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (fileSelected != null) {
                return spectrumFactory.getNSpectra(fileSelected);
            } else {
                return 0;
            }
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
                    return "ID";
                case 2:
                    return "Title";
                case 3:
                    return "m/z";
                case 4:
                    return "Charge";
                case 5:
                    return "Int";
                case 6:
                    return "RT";
                case 7:
                    return "Sequence";
                case 8:
                    return "Protein(s)";
                case 9:
                    return "Confidence";
                case 10:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            try {
                String spectrumTitle = spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                String spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumTitle);

                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        int idSoftwareAgreement;
                        if (!identification.matchExists(spectrumKey)) {
                            idSoftwareAgreement = NO_ID;
                        } else {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            idSoftwareAgreement = isBestPsmEqualForAllIdSoftwares(spectrumMatch, peptideShakerGUI.getSearchParameters());
                        }
                        return idSoftwareAgreement;
                    case 2:
                        return spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                    case 3:
                        Precursor precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        if (precursor != null) {
                            return precursor.getMz();
                        } else {
                            return null;
                        }
                    case 4:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        Integer charge = null;
                        if (precursor != null && !precursor.getPossibleCharges().isEmpty()) {
                            charge = precursor.getPossibleCharges().get(0).value; // @TODO: find a way of displaying multiple charges!!!
                        }
                        return charge;
                    case 5:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        if (precursor != null) {
                            return precursor.getIntensity();
                        } else {
                            return null;
                        }
                    case 6:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        if (precursor != null) {
                            return precursor.getRt();
                        } else {
                            return null;
                        }
                    case 7:
                        if (identification.matchExists(spectrumKey)) {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                            return displayFeaturesGenerator.getTaggedPeptideSequence(spectrumMatch.getBestPeptideAssumption().getPeptide(), true, true, true);
                        } else {
                            return null;
                        }
                    case 8:
                        if (identification.matchExists(spectrumKey)) {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                            String proteins = displayFeaturesGenerator.addDatabaseLinks(spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(
                                    PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy()));
                            return proteins;
                        } else {
                            return null;
                        }
                    case 9:
                        if (identification.matchExists(spectrumKey)) {
                            PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
                            if (pSParameter != null) {
                                return pSParameter.getPsmConfidence();
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    case 10:
                        if (identification.matchExists(spectrumKey)) {
                            PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
                            if (pSParameter != null) {
                                return pSParameter.getMatchValidationLevel().getIndex();
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
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
            return false;
        }
    }

    /**
     * Update the Venn diagram.
     *
     * @param dataA the data in group A
     * @param dataB the data in group B
     * @param dataC the data in group C
     * @param dataAB the data in group AB
     * @param dataAC the data in group AC
     * @param dataBC the data in group BC
     * @param dataABC the data in group ABC
     * @param vennDiagramAdvocates the advocates
     */
    private void updateVennDiagram(int dataA, int dataB, int dataC, int dataAB, int dataAC, int dataBC, int dataABC, ArrayList<Advocate> vennDiagramAdvocates) {
        if (vennDiagramAdvocates.size() == 3) {
            updateThreeWayVennDiagram(vennDiagramButton, dataA, dataB, dataC,
                    dataAB, dataAC, dataBC, dataABC, vennDiagramAdvocates.get(0).getName(), vennDiagramAdvocates.get(1).getName(), vennDiagramAdvocates.get(2).getName(),
                    advocateVennColors.get(vennDiagramAdvocates.get(0)), advocateVennColors.get(vennDiagramAdvocates.get(1)), advocateVennColors.get(vennDiagramAdvocates.get(2)));
        } else if (vennDiagramAdvocates.size() == 2) {
            updateTwoWayVennDiagram(vennDiagramButton, dataA, dataB, dataAB,
                    vennDiagramAdvocates.get(0).getName(), vennDiagramAdvocates.get(1).getName(),
                    advocateVennColors.get(vennDiagramAdvocates.get(0)), advocateVennColors.get(vennDiagramAdvocates.get(1)));
        } else {
            vennDiagramButton.setText(null);
            vennDiagramButton.setToolTipText(null);
            vennDiagramButton.setIcon(null);
        }
    }

    /**
     * Updates the ID software overview plots.
     */
    private void updateOverviewPlots(final HashMap<Advocate, Double> totalAdvocateId, final HashMap<Advocate, Double> uniqueAdvocateId, final int totalNumberOfSpectra) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                overviewPlotsPanel.removeAll();

                // create the number of psms plot
                createPlot(totalAdvocateId, "#PSMs", false);

                // create the number of unique psms plot
                createPlot(uniqueAdvocateId, "#Unique PSMs", false);

                // create the number of unassigned plot
                HashMap<Advocate, Double> unassignedAdvocate = new HashMap<Advocate, Double>();
                for (Advocate tempAdvocate : advocatesUsed) {
                    if (totalAdvocateId.containsKey(tempAdvocate)) {
                        unassignedAdvocate.put(tempAdvocate, totalNumberOfSpectra - totalAdvocateId.get(tempAdvocate));
                    }
                }
                unassignedAdvocate.put(Advocate.PeptideShaker, totalNumberOfSpectra - totalAdvocateId.get(Advocate.PeptideShaker));
                createPlot(unassignedAdvocate, "#Unassigned", false);

                // create the id rate plot
                HashMap<Advocate, Double> idRateAdvocate = new HashMap<Advocate, Double>();
                for (Advocate tempAdvocate : advocatesUsed) {
                    if (totalAdvocateId.containsKey(tempAdvocate)) {
                        idRateAdvocate.put(tempAdvocate, ((double) totalAdvocateId.get(tempAdvocate) / totalNumberOfSpectra) * 100);
                    }
                }
                idRateAdvocate.put(Advocate.PeptideShaker, ((double) totalAdvocateId.get(Advocate.PeptideShaker) / totalNumberOfSpectra) * 100);
                createPlot(idRateAdvocate, "ID Rate (%)", true);

                overviewPlotsPanel.revalidate();
                overviewPlotsPanel.repaint();
            }
        });
    }

    /**
     * Add the given assumption to the table.
     *
     * @param currentAssumption
     * @param aProbabilities
     * @param software
     */
    private void addIdResultsToTable(SpectrumIdentificationAssumption currentAssumption, PSParameter aProbabilities, Advocate software) {

        PeptideAssumption peptideAssumption = (PeptideAssumption) currentAssumption;
        PSParameter probabilities = (PSParameter) currentAssumption.getUrParam(aProbabilities);
        double confidence = probabilities.getSearchEngineConfidence();
        int currentRowNumber = 0;
        boolean addRowAtBottom = true;

        // find the correct row to insert the match
        if (searchResultsTable.getRowCount() > 0) {
            for (int i = 0; i < searchResultsTable.getRowCount(); i++) {
                if (confidence > (Double) searchResultsTable.getValueAt(i, 5)) {
                    currentRowNumber = i;
                    addRowAtBottom = false;
                    break;
                }
            }
        }

        // simple validation
        Integer validationType = probabilities.getMatchValidationLevel().getIndex();

        Object[] rowData = new Object[]{
            currentRowNumber,
            software.getIndex(),
            currentAssumption.getRank(),
            peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideAssumption.getPeptide(), true, true, true),
            currentAssumption.getIdentificationCharge().value,
            confidence,
            validationType
        };

        if (addRowAtBottom) {
            ((DefaultTableModel) searchResultsTable.getModel()).addRow(rowData);
            searchResultsTablePeptideTooltips.add(peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(peptideAssumption.getPeptide()));
            searchResultsPeptideKeys.add(peptideAssumption);
        } else {
            ((DefaultTableModel) searchResultsTable.getModel()).insertRow(currentRowNumber, rowData);
            searchResultsTablePeptideTooltips.add(currentRowNumber, peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(peptideAssumption.getPeptide()));
            searchResultsPeptideKeys.add(currentRowNumber, peptideAssumption);
        }
    }

    /**
     * Create an overview plot.
     *
     * @param data the data to plot
     * @param xAxisLabel the xAxis label
     * @param roundDecimals if true, the decimals in the labels are rounded to
     * one decimal
     */
    private void createPlot(HashMap<Advocate, Double> data, String xAxisLabel, boolean roundDecimals) {

        DefaultCategoryDataset psmDataset = new DefaultCategoryDataset();
        for (Advocate tempAdvocate : advocatesUsed) {
            if (advocatesUsed.contains(tempAdvocate)) {
                psmDataset.addValue(data.get(tempAdvocate), tempAdvocate, xAxisLabel);
            }
        }
        psmDataset.addValue(data.get(Advocate.PeptideShaker), Advocate.PeptideShaker, xAxisLabel);

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, psmDataset, PlotOrientation.VERTICAL, false, false, false);
        CategoryPlot plot = chart.getCategoryPlot();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(java.awt.Color.WHITE);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLowerMargin(0.15);
        rangeAxis.setUpperMargin(0.15);
        plot.setOutlineVisible(false);
        BarRenderer renderer = new BarRenderer();
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        int dataSeriesCounter = 0;
        for (Advocate tempAdvocate : advocatesUsed) {
            if (advocatesUsed.contains(tempAdvocate)) {
                renderer.setSeriesPaint(dataSeriesCounter++, searchEnginesColorMap.get(tempAdvocate.getIndex()));
            }
        }
        renderer.setSeriesPaint(dataSeriesCounter, searchEnginesColorMap.get(Advocate.PeptideShaker.getIndex()));
        if (roundDecimals) {
            renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator(StandardCategoryItemLabelGenerator.DEFAULT_LABEL_FORMAT_STRING, new DecimalFormat("0.0")));
        } else {
            renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        }
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        renderer.setBaseItemLabelsVisible(true);
        plot.setRenderer(renderer);

        // add the plot to the chart
        overviewPlotsPanel.add(chartPanel);
    }
}
