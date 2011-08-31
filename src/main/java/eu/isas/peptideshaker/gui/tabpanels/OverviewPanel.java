package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.gui.spectrum.FragmentIonTable;
import com.compomics.util.gui.spectrum.IntensityHistogram;
import com.compomics.util.gui.spectrum.MassErrorBubblePlot;
import com.compomics.util.gui.spectrum.MassErrorPlot;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.HelpWindow;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.ProteinInferenceDialog;
import eu.isas.peptideshaker.gui.ProteinInferencePeptideLevelDialog;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The overview panel displaying the proteins, the peptides and the spectra.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class OverviewPanel extends javax.swing.JPanel {

    /**
     * A reference to the protein score column.
     */
    private TableColumn proteinScoreColumn;
    /**
     * A reference to the peptide score column.
     */
    private TableColumn peptideScoreColumn;
    /**
     * The current sequence coverage;
     */
    private int[] coverage;
    /**
     * The current protein sequence;
     */
    private String currentProteinSequence;
    /**
     * The maximum sequence length for display in the sequence coverage panel.
     */
    private final int MAX_SEQUENCE_LENGTH = 6000;
    /**
     * If true, the protein selection in the overview tab is mirrored in 
     * the protein table in the protein structure tab.
     */
    private boolean updateProteinStructurePanel = true;
    /**
     * The maximum mz value in the current list of PSMs. Needed to make sure that
     * the PSMs for the same peptide all use the same mz range.
     */
    private double maxPsmMzValue = Double.MIN_VALUE;
    /**
     * The current spectrum panel.
     */
    private SpectrumPanel spectrum;
    /**
     * boolean indicating whether the spectrum shall be displayed
     */
    private boolean displaySpectrum;
    /**
     * Boolean indicating whether the sequence coverage shall be displayed
     */
    private boolean displayCoverage;
    /**
     * Boolean indicating whether the protein table shall be displayed
     */
    private boolean displayProteins;
    /**
     * Boolean indicating whether the PSMs shall be displayed
     */
    private boolean displayPeptidesAndPSMs;
    /**
     * A mapping of the protein table entries
     */
    private HashMap<Integer, String> proteinTableMap = new HashMap<Integer, String>();
    /**
     * A mapping of the peptide table entries
     */
    private HashMap<Integer, String> peptideTableMap = new HashMap<Integer, String>();
    /**
     * A mapping of the psm table entries
     */
    private HashMap<Integer, String> psmTableMap = new HashMap<Integer, String>();
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The peptide table column header tooltips.
     */
    private ArrayList<String> peptideTableToolTips;
    /**
     * The PMS table column header tooltips.
     */
    private ArrayList<String> psmTableToolTips;
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * Creates a new OverviewPanel.
     *
     * @param parent the PeptideShaker parent frame.
     */
    public OverviewPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent;
        this.displayCoverage = false;

        initComponents();

        coverageTableScrollPane.setBorder(null);

        proteinScoreColumn = proteinTable.getColumn("Score");
        peptideScoreColumn = peptideTable.getColumn("Score");

        setTableProperties();

        // make sure that the scroll panes are see-through
        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        spectraScrollPane.getViewport().setOpaque(false);
        fragmentIonsJScrollPane.getViewport().setOpaque(false);
        coverageTableScrollPane.getViewport().setOpaque(false);

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumJTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        updateSeparators();
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // set table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmTable.setAutoCreateRowSorter(true);

        // the index column
        proteinTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMaxWidth(50);
        psmTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);
        psmTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn("Start").setMinWidth(50);
        peptideTable.getColumn("Start").setMaxWidth(50);
        peptideTable.getColumn("End").setMinWidth(50);
        peptideTable.getColumn("End").setMaxWidth(50);

        // the validated column
        proteinTable.getColumn("").setMaxWidth(30);
        peptideTable.getColumn("").setMaxWidth(30);
        psmTable.getColumn("").setMaxWidth(30);
        proteinTable.getColumn("").setMinWidth(30);
        peptideTable.getColumn("").setMinWidth(30);
        psmTable.getColumn("").setMinWidth(30);

        // the protein inference column
        proteinTable.getColumn("PI").setMaxWidth(35);
        proteinTable.getColumn("PI").setMinWidth(35);
        peptideTable.getColumn("PI").setMaxWidth(35);
        peptideTable.getColumn("PI").setMinWidth(35);

        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor()); // NOT_GROUP
        proteinInferenceColorMap.put(PSParameter.ISOFORMS, Color.ORANGE); // ISOFORMS
        proteinInferenceColorMap.put(PSParameter.ISOFORMS_UNRELATED, Color.BLUE); // ISOFORMS_UNRELATED
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED); // UNRELATED

        // set up the protein inference tooltip map
        HashMap<Integer, String> proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Single Protein");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS, "Isoforms");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS_UNRELATED, "Unrelated Isoforms");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Unrelated Proteins");

        proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        proteinTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), proteinInferenceColorMap, proteinInferenceTooltipMap));
        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("Spectrum Counting").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Spectrum Counting").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() + 5, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).setMinimumChartValue(5d);
        proteinTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        // set up the peptide inference color map
        HashMap<Integer, Color> peptideInferenceColorMap = new HashMap<Integer, Color>();
        peptideInferenceColorMap.put(0, peptideShakerGUI.getSparklineColor());
        peptideInferenceColorMap.put(1, Color.ORANGE);
        peptideInferenceColorMap.put(2, Color.BLUE);
        peptideInferenceColorMap.put(3, Color.RED);

        // set up the peptide inference tooltip map
        HashMap<Integer, String> peptideInferenceTooltipMap = new HashMap<Integer, String>();
        peptideInferenceTooltipMap.put(0, "Unique to Protein/Protein Group");
        peptideInferenceTooltipMap.put(1, "Maps to 2 Proteins/Protein Groups");
        peptideInferenceTooltipMap.put(2, "Maps to 3-5 Proteins/Protein Groups");
        peptideInferenceTooltipMap.put(3, "Maps to >5 Proteins/Protein Groups");

        peptideTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), peptideInferenceColorMap, peptideInferenceTooltipMap));
        peptideTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        peptideTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() + 5, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        peptideTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        psmTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        psmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        psmTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        coverageTable.getColumn(" ").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.proteinSequence, PlotOrientation.HORIZONTAL, 0.0, 100d));
        ((JSparklinesTableCellRenderer) coverageTable.getColumn(" ").getCellRenderer()).setBackgroundColor(Color.WHITE);

        try {
            proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth() + 5, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            peptideTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth() + 5, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            // ignore error
        }


        // set up the table header tooltips
        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Protein Inference");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Protein Seqeunce Coverage (%)");
        proteinTableToolTips.add("Number of Peptides");
        proteinTableToolTips.add("Number of Spectra");
        proteinTableToolTips.add("Protein Spectrum Counting Score");
        proteinTableToolTips.add("Protein Score");
        proteinTableToolTips.add("Protein Confidence");
        proteinTableToolTips.add("Validated");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Protein Inference");
        peptideTableToolTips.add("Peptide Sequence");
        peptideTableToolTips.add("Peptide Start Index");
        peptideTableToolTips.add("Peptide End Index");
        peptideTableToolTips.add("Peptide Modifications");
        peptideTableToolTips.add("Number of Spectra");
        peptideTableToolTips.add("Peptide Score");
        peptideTableToolTips.add("Peptide Confidence");
        peptideTableToolTips.add("Validated");

        psmTableToolTips = new ArrayList<String>();
        psmTableToolTips.add(null);
        psmTableToolTips.add("Peptide Sequence");
        psmTableToolTips.add("Peptide Modifications");
        psmTableToolTips.add("Precursor Charge");
        psmTableToolTips.add("Mass Error");
        psmTableToolTips.add("Validated");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ionTableButtonGroup = new javax.swing.ButtonGroup();
        overviewJPanel = new javax.swing.JPanel();
        overviewJSplitPane = new javax.swing.JSplitPane();
        proteinsJPanel = new javax.swing.JPanel();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) proteinTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        coverageJSplitPane = new javax.swing.JSplitPane();
        sequenceCoverageJPanel = new javax.swing.JPanel();
        coverageTableScrollPane = new javax.swing.JScrollPane();
        coverageTable = new javax.swing.JTable();
        peptidesPsmSpectrumFragmentIonsJSplitPane = new javax.swing.JSplitPane();
        peptidesPsmJSplitPane = new javax.swing.JSplitPane();
        peptidesJPanel = new javax.swing.JPanel();
        peptideScrollPane = new javax.swing.JScrollPane();
        peptideTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) peptideTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        psmJPanel = new javax.swing.JPanel();
        spectraScrollPane = new javax.swing.JScrollPane();
        psmTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) psmTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumMainPanel = new javax.swing.JPanel();
        spectrumJTabbedPane = new javax.swing.JTabbedPane();
        fragmentIonJPanel = new javax.swing.JPanel();
        fragmentIonsJScrollPane = new javax.swing.JScrollPane();
        ionTableJToolBar = new javax.swing.JToolBar();
        aIonTableToggleButton = new javax.swing.JToggleButton();
        bIonTableToggleButton = new javax.swing.JToggleButton();
        cIonTableToggleButton = new javax.swing.JToggleButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        xIonTableToggleButton = new javax.swing.JToggleButton();
        yIonTableToggleButton = new javax.swing.JToggleButton();
        zIonTableToggleButton = new javax.swing.JToggleButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        h2oTableToggleButton = new javax.swing.JToggleButton();
        nh3TableToggleButton = new javax.swing.JToggleButton();
        otherTableToggleButton = new javax.swing.JToggleButton();
        jSeparator14 = new javax.swing.JToolBar.Separator();
        oneChargeTableToggleButton = new javax.swing.JToggleButton();
        twoChargesTableToggleButton = new javax.swing.JToggleButton();
        moreThanTwoChargesTableToggleButton = new javax.swing.JToggleButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        barsIonTableToggleButton = new javax.swing.JToggleButton();
        mzValuesIonTableToggleButton = new javax.swing.JToggleButton();
        jSeparator18 = new javax.swing.JToolBar.Separator();
        ionTableHelpJButton = new javax.swing.JButton();
        bubblePlotTabJPanel = new javax.swing.JPanel();
        bubbleJPanel = new javax.swing.JPanel();
        bubblePlotJToolBar = new javax.swing.JToolBar();
        aIonBubblePlotToggleButton = new javax.swing.JToggleButton();
        bIonBubblePlotToggleButton = new javax.swing.JToggleButton();
        cIonBubblePlotToggleButton = new javax.swing.JToggleButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        xIonBubblePlotToggleButton = new javax.swing.JToggleButton();
        yIonBubblePlotToggleButton = new javax.swing.JToggleButton();
        zIonBubblePlotToggleButton = new javax.swing.JToggleButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        h2oBubblePlotToggleButton = new javax.swing.JToggleButton();
        nh3BubblePlotToggleButton = new javax.swing.JToggleButton();
        otherBubblePlotToggleButton = new javax.swing.JToggleButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        oneChargeBubblePlotToggleButton = new javax.swing.JToggleButton();
        twoChargesBubblePlotToggleButton = new javax.swing.JToggleButton();
        moreThanTwoChargesBubblePlotToggleButton = new javax.swing.JToggleButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        barsBubblePlotToggleButton = new javax.swing.JToggleButton();
        jSeparator17 = new javax.swing.JToolBar.Separator();
        bubblePlotHelpJButton = new javax.swing.JButton();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        aIonToggleButton = new javax.swing.JToggleButton();
        bIonToggleButton = new javax.swing.JToggleButton();
        cIonToggleButton = new javax.swing.JToggleButton();
        xIonToggleButton = new javax.swing.JToggleButton();
        yIonToggleButton = new javax.swing.JToggleButton();
        zIonToggleButton = new javax.swing.JToggleButton();
        otherToggleButton = new javax.swing.JToggleButton();
        jSeparator12 = new javax.swing.JToolBar.Separator();
        h2oToggleButton = new javax.swing.JToggleButton();
        nh3ToggleButton = new javax.swing.JToggleButton();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        oneChargeToggleButton = new javax.swing.JToggleButton();
        twoChargesToggleButton = new javax.swing.JToggleButton();
        moreThanTwoChargesToggleButton = new javax.swing.JToggleButton();
        jSeparator15 = new javax.swing.JToolBar.Separator();
        allToggleButton = new javax.swing.JToggleButton();
        jSeparator16 = new javax.swing.JToolBar.Separator();
        spectrumHelpJButton = new javax.swing.JButton();
        spectrumSplitPane = new javax.swing.JSplitPane();
        sequenceFragmentIonPlotsJPanel = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));

        overviewJPanel.setBackground(new java.awt.Color(255, 255, 255));
        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));

        overviewJSplitPane.setBorder(null);
        overviewJSplitPane.setDividerLocation(300);
        overviewJSplitPane.setDividerSize(0);
        overviewJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        overviewJSplitPane.setResizeWeight(0.5);
        overviewJSplitPane.setOpaque(false);

        proteinsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsJPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "PI", "Accession", "Description", "Coverage", "#Peptides", "#Spectra", "Spectrum Counting", "Score", "Confidence", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        proteinTable.setOpaque(false);
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
        proteinScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout proteinsJPanelLayout = new javax.swing.GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsJPanelLayout);
        proteinsJPanelLayout.setHorizontalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 925, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinsJPanelLayout.setVerticalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addContainerGap())
        );

        overviewJSplitPane.setTopComponent(proteinsJPanel);

        coverageJSplitPane.setBorder(null);
        coverageJSplitPane.setDividerLocation(350);
        coverageJSplitPane.setDividerSize(0);
        coverageJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        coverageJSplitPane.setResizeWeight(1.0);
        coverageJSplitPane.setOpaque(false);

        sequenceCoverageJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Sequence Coverage"));
        sequenceCoverageJPanel.setOpaque(false);

        coverageTableScrollPane.setOpaque(false);

        coverageTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                " "
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        coverageTable.setEnabled(false);
        coverageTable.setOpaque(false);
        coverageTable.setTableHeader(null);
        coverageTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                coverageTableMouseMoved(evt);
            }
        });
        coverageTableScrollPane.setViewportView(coverageTable);

        javax.swing.GroupLayout sequenceCoverageJPanelLayout = new javax.swing.GroupLayout(sequenceCoverageJPanel);
        sequenceCoverageJPanel.setLayout(sequenceCoverageJPanelLayout);
        sequenceCoverageJPanelLayout.setHorizontalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 925, Short.MAX_VALUE)
                .addContainerGap())
        );
        sequenceCoverageJPanelLayout.setVerticalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        coverageJSplitPane.setRightComponent(sequenceCoverageJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setBorder(null);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(450);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setResizeWeight(0.5);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setOpaque(false);

        peptidesPsmJSplitPane.setBorder(null);
        peptidesPsmJSplitPane.setDividerLocation(150);
        peptidesPsmJSplitPane.setDividerSize(0);
        peptidesPsmJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptidesPsmJSplitPane.setResizeWeight(0.5);
        peptidesPsmJSplitPane.setOpaque(false);

        peptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidesJPanel.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "PI", "Sequence", "Start", "End", "Modifications", "#Spectra", "Score", "Confidence", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        peptideTable.setOpaque(false);
        peptideTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        peptideTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptideTableMouseMoved(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout peptidesJPanelLayout = new javax.swing.GroupLayout(peptidesJPanel);
        peptidesJPanel.setLayout(peptidesJPanelLayout);
        peptidesJPanelLayout.setHorizontalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesJPanelLayout.setVerticalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmJSplitPane.setTopComponent(peptidesJPanel);

        psmJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches"));
        psmJPanel.setOpaque(false);

        spectraScrollPane.setOpaque(false);

        psmTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Modifications", "Charge", "Mass Error", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Boolean.class
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
        psmTable.setOpaque(false);
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                psmTableMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
            }
        });
        psmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmTableKeyReleased(evt);
            }
        });
        spectraScrollPane.setViewportView(psmTable);

        javax.swing.GroupLayout psmJPanelLayout = new javax.swing.GroupLayout(psmJPanel);
        psmJPanel.setLayout(psmJPanelLayout);
        psmJPanelLayout.setHorizontalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmJPanelLayout.setVerticalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmJSplitPane.setRightComponent(psmJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setLeftComponent(peptidesPsmJSplitPane);

        spectrumMainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumMainPanel.setOpaque(false);

        spectrumJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        fragmentIonJPanel.setBackground(new java.awt.Color(255, 255, 255));

        fragmentIonsJScrollPane.setOpaque(false);

        ionTableJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        ionTableJToolBar.setBorder(null);
        ionTableJToolBar.setFloatable(false);
        ionTableJToolBar.setRollover(true);
        ionTableJToolBar.setBorderPainted(false);
        ionTableJToolBar.setPreferredSize(new java.awt.Dimension(0, 25));

        aIonTableToggleButton.setText("a");
        aIonTableToggleButton.setToolTipText("a-ions");
        aIonTableToggleButton.setFocusable(false);
        aIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aIonTableToggleButton.setMinimumSize(new java.awt.Dimension(25, 21));
        aIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        aIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(aIonTableToggleButton);

        bIonTableToggleButton.setSelected(true);
        bIonTableToggleButton.setText("b");
        bIonTableToggleButton.setToolTipText("b-ions");
        bIonTableToggleButton.setFocusable(false);
        bIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        bIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(bIonTableToggleButton);

        cIonTableToggleButton.setText("c");
        cIonTableToggleButton.setToolTipText("c-ions");
        cIonTableToggleButton.setFocusable(false);
        cIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        cIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(cIonTableToggleButton);
        ionTableJToolBar.add(jSeparator6);

        xIonTableToggleButton.setText("x");
        xIonTableToggleButton.setToolTipText("x-ions");
        xIonTableToggleButton.setFocusable(false);
        xIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        xIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        xIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        xIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(xIonTableToggleButton);

        yIonTableToggleButton.setSelected(true);
        yIonTableToggleButton.setText("y");
        yIonTableToggleButton.setToolTipText("y-ions");
        yIonTableToggleButton.setFocusable(false);
        yIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        yIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(yIonTableToggleButton);

        zIonTableToggleButton.setText("z");
        zIonTableToggleButton.setToolTipText("z-ions");
        zIonTableToggleButton.setFocusable(false);
        zIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        zIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(zIonTableToggleButton);
        ionTableJToolBar.add(jSeparator7);

        h2oTableToggleButton.setText("H2O");
        h2oTableToggleButton.setToolTipText("<html>Water Loss</html>");
        h2oTableToggleButton.setFocusable(false);
        h2oTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        h2oTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        h2oTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        h2oTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h2oTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(h2oTableToggleButton);

        nh3TableToggleButton.setText("NH3");
        nh3TableToggleButton.setToolTipText("Ammonia Loss");
        nh3TableToggleButton.setFocusable(false);
        nh3TableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nh3TableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        nh3TableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nh3TableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nh3TableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(nh3TableToggleButton);

        otherTableToggleButton.setText("Oth.");
        otherTableToggleButton.setToolTipText("Other: Precursor and Immonium Ions");
        otherTableToggleButton.setEnabled(false);
        otherTableToggleButton.setFocusable(false);
        otherTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        otherTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        otherTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        otherTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(otherTableToggleButton);
        ionTableJToolBar.add(jSeparator14);

        oneChargeTableToggleButton.setSelected(true);
        oneChargeTableToggleButton.setText("+");
        oneChargeTableToggleButton.setToolTipText("Single Charge");
        oneChargeTableToggleButton.setFocusable(false);
        oneChargeTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        oneChargeTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        oneChargeTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        oneChargeTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oneChargeTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(oneChargeTableToggleButton);

        twoChargesTableToggleButton.setText("++");
        twoChargesTableToggleButton.setToolTipText("Double Charge");
        twoChargesTableToggleButton.setFocusable(false);
        twoChargesTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        twoChargesTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        twoChargesTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        twoChargesTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoChargesTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(twoChargesTableToggleButton);

        moreThanTwoChargesTableToggleButton.setText(">2 ");
        moreThanTwoChargesTableToggleButton.setToolTipText("More Than Two Charges");
        moreThanTwoChargesTableToggleButton.setEnabled(false);
        moreThanTwoChargesTableToggleButton.setFocusable(false);
        moreThanTwoChargesTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moreThanTwoChargesTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        moreThanTwoChargesTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moreThanTwoChargesTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(moreThanTwoChargesTableToggleButton);
        ionTableJToolBar.add(jSeparator5);

        barsIonTableToggleButton.setSelected(true);
        barsIonTableToggleButton.setText("Int");
        barsIonTableToggleButton.setToolTipText("Bar charts with peak intensities");
        barsIonTableToggleButton.setFocusable(false);
        barsIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        barsIonTableToggleButton.setMinimumSize(new java.awt.Dimension(33, 21));
        barsIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        barsIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        barsIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                barsIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(barsIonTableToggleButton);

        mzValuesIonTableToggleButton.setText("m/z");
        mzValuesIonTableToggleButton.setToolTipText("Traditional ion table with m/z values");
        mzValuesIonTableToggleButton.setFocusable(false);
        mzValuesIonTableToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        mzValuesIonTableToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        mzValuesIonTableToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        mzValuesIonTableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mzValuesIonTableToggleButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(mzValuesIonTableToggleButton);
        ionTableJToolBar.add(jSeparator18);

        ionTableHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        ionTableHelpJButton.setToolTipText("Help");
        ionTableHelpJButton.setBorder(null);
        ionTableHelpJButton.setBorderPainted(false);
        ionTableHelpJButton.setContentAreaFilled(false);
        ionTableHelpJButton.setFocusable(false);
        ionTableHelpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ionTableHelpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ionTableHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ionTableHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ionTableHelpJButtonMouseExited(evt);
            }
        });
        ionTableHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ionTableHelpJButtonActionPerformed(evt);
            }
        });
        ionTableJToolBar.add(ionTableHelpJButton);

        javax.swing.GroupLayout fragmentIonJPanelLayout = new javax.swing.GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                    .addComponent(ionTableJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE))
                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ionTableJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Ion Table", fragmentIonJPanel);

        bubblePlotTabJPanel.setBackground(new java.awt.Color(255, 255, 255));

        bubbleJPanel.setOpaque(false);
        bubbleJPanel.setLayout(new javax.swing.BoxLayout(bubbleJPanel, javax.swing.BoxLayout.LINE_AXIS));

        bubblePlotJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        bubblePlotJToolBar.setBorder(null);
        bubblePlotJToolBar.setFloatable(false);
        bubblePlotJToolBar.setRollover(true);
        bubblePlotJToolBar.setBorderPainted(false);

        aIonBubblePlotToggleButton.setText("a");
        aIonBubblePlotToggleButton.setToolTipText("a-ions");
        aIonBubblePlotToggleButton.setFocusable(false);
        aIonBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aIonBubblePlotToggleButton.setMinimumSize(new java.awt.Dimension(25, 21));
        aIonBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        aIonBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aIonBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(aIonBubblePlotToggleButton);

        bIonBubblePlotToggleButton.setSelected(true);
        bIonBubblePlotToggleButton.setText("b");
        bIonBubblePlotToggleButton.setToolTipText("b-ions");
        bIonBubblePlotToggleButton.setFocusable(false);
        bIonBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bIonBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        bIonBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bIonBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(bIonBubblePlotToggleButton);

        cIonBubblePlotToggleButton.setText("c");
        cIonBubblePlotToggleButton.setToolTipText("c-ions");
        cIonBubblePlotToggleButton.setFocusable(false);
        cIonBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cIonBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        cIonBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cIonBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(cIonBubblePlotToggleButton);
        bubblePlotJToolBar.add(jSeparator8);

        xIonBubblePlotToggleButton.setText("x");
        xIonBubblePlotToggleButton.setToolTipText("x-ions");
        xIonBubblePlotToggleButton.setFocusable(false);
        xIonBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        xIonBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        xIonBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        xIonBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(xIonBubblePlotToggleButton);

        yIonBubblePlotToggleButton.setSelected(true);
        yIonBubblePlotToggleButton.setText("y");
        yIonBubblePlotToggleButton.setToolTipText("y-ions");
        yIonBubblePlotToggleButton.setFocusable(false);
        yIonBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yIonBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        yIonBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yIonBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(yIonBubblePlotToggleButton);

        zIonBubblePlotToggleButton.setText("z");
        zIonBubblePlotToggleButton.setToolTipText("z-ions");
        zIonBubblePlotToggleButton.setFocusable(false);
        zIonBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zIonBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        zIonBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zIonBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(zIonBubblePlotToggleButton);
        bubblePlotJToolBar.add(jSeparator9);

        h2oBubblePlotToggleButton.setText("H2O");
        h2oBubblePlotToggleButton.setToolTipText("<html>Water Loss</html>");
        h2oBubblePlotToggleButton.setFocusable(false);
        h2oBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        h2oBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        h2oBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        h2oBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h2oBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(h2oBubblePlotToggleButton);

        nh3BubblePlotToggleButton.setText("NH3");
        nh3BubblePlotToggleButton.setToolTipText("Ammonia Loss");
        nh3BubblePlotToggleButton.setFocusable(false);
        nh3BubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nh3BubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        nh3BubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nh3BubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nh3BubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(nh3BubblePlotToggleButton);

        otherBubblePlotToggleButton.setText("Oth.");
        otherBubblePlotToggleButton.setToolTipText("Other: Precursor and Immonium Ions");
        otherBubblePlotToggleButton.setFocusable(false);
        otherBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        otherBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        otherBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        otherBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(otherBubblePlotToggleButton);
        bubblePlotJToolBar.add(jSeparator10);

        oneChargeBubblePlotToggleButton.setSelected(true);
        oneChargeBubblePlotToggleButton.setText("+");
        oneChargeBubblePlotToggleButton.setToolTipText("Single Charge");
        oneChargeBubblePlotToggleButton.setFocusable(false);
        oneChargeBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        oneChargeBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        oneChargeBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        oneChargeBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oneChargeBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(oneChargeBubblePlotToggleButton);

        twoChargesBubblePlotToggleButton.setText("++");
        twoChargesBubblePlotToggleButton.setToolTipText("Double Charge");
        twoChargesBubblePlotToggleButton.setFocusable(false);
        twoChargesBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        twoChargesBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        twoChargesBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        twoChargesBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoChargesBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(twoChargesBubblePlotToggleButton);

        moreThanTwoChargesBubblePlotToggleButton.setText(">2 ");
        moreThanTwoChargesBubblePlotToggleButton.setToolTipText("More Than Two Charges");
        moreThanTwoChargesBubblePlotToggleButton.setFocusable(false);
        moreThanTwoChargesBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moreThanTwoChargesBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        moreThanTwoChargesBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moreThanTwoChargesBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(moreThanTwoChargesBubblePlotToggleButton);
        bubblePlotJToolBar.add(jSeparator4);

        barsBubblePlotToggleButton.setSelected(true);
        barsBubblePlotToggleButton.setText("Bars");
        barsBubblePlotToggleButton.setToolTipText("Add bars highlighting the fragment ion types");
        barsBubblePlotToggleButton.setFocusable(false);
        barsBubblePlotToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        barsBubblePlotToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        barsBubblePlotToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        barsBubblePlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                barsBubblePlotToggleButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(barsBubblePlotToggleButton);
        bubblePlotJToolBar.add(jSeparator17);

        bubblePlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        bubblePlotHelpJButton.setToolTipText("Help");
        bubblePlotHelpJButton.setBorder(null);
        bubblePlotHelpJButton.setBorderPainted(false);
        bubblePlotHelpJButton.setContentAreaFilled(false);
        bubblePlotHelpJButton.setFocusable(false);
        bubblePlotHelpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bubblePlotHelpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bubblePlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                bubblePlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                bubblePlotHelpJButtonMouseExited(evt);
            }
        });
        bubblePlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bubblePlotHelpJButtonActionPerformed(evt);
            }
        });
        bubblePlotJToolBar.add(bubblePlotHelpJButton);

        javax.swing.GroupLayout bubblePlotTabJPanelLayout = new javax.swing.GroupLayout(bubblePlotTabJPanel);
        bubblePlotTabJPanel.setLayout(bubblePlotTabJPanelLayout);
        bubblePlotTabJPanelLayout.setHorizontalGroup(
            bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE))
        );
        bubblePlotTabJPanelLayout.setVerticalGroup(
            bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                .addContainerGap(270, Short.MAX_VALUE)
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                    .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                    .addGap(33, 33, 33)))
        );

        spectrumJTabbedPane.addTab("Bubble Plot", bubblePlotTabJPanel);

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        aIonToggleButton.setText("a");
        aIonToggleButton.setToolTipText("a-ions");
        aIonToggleButton.setFocusable(false);
        aIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aIonToggleButton.setMinimumSize(new java.awt.Dimension(25, 21));
        aIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        aIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(aIonToggleButton);

        bIonToggleButton.setSelected(true);
        bIonToggleButton.setText("b");
        bIonToggleButton.setToolTipText("b-ions");
        bIonToggleButton.setFocusable(false);
        bIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        bIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(bIonToggleButton);

        cIonToggleButton.setText("c");
        cIonToggleButton.setToolTipText("c-ions");
        cIonToggleButton.setFocusable(false);
        cIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        cIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(cIonToggleButton);

        xIonToggleButton.setText("x");
        xIonToggleButton.setToolTipText("x-ions");
        xIonToggleButton.setFocusable(false);
        xIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        xIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        xIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        xIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(xIonToggleButton);

        yIonToggleButton.setSelected(true);
        yIonToggleButton.setText("y");
        yIonToggleButton.setToolTipText("y-ions");
        yIonToggleButton.setFocusable(false);
        yIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        yIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(yIonToggleButton);

        zIonToggleButton.setText("z");
        zIonToggleButton.setToolTipText("z-ions");
        zIonToggleButton.setFocusable(false);
        zIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        zIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(zIonToggleButton);

        otherToggleButton.setText("Oth.");
        otherToggleButton.setToolTipText("Other: Precursor and Immonium Ions");
        otherToggleButton.setFocusable(false);
        otherToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        otherToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        otherToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        otherToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(otherToggleButton);
        spectrumJToolBar.add(jSeparator12);

        h2oToggleButton.setText("H2O");
        h2oToggleButton.setToolTipText("Water Loss");
        h2oToggleButton.setFocusable(false);
        h2oToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        h2oToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        h2oToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        h2oToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h2oToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(h2oToggleButton);

        nh3ToggleButton.setText("NH3");
        nh3ToggleButton.setToolTipText("Ammonia Loss");
        nh3ToggleButton.setFocusable(false);
        nh3ToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nh3ToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        nh3ToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nh3ToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nh3ToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(nh3ToggleButton);
        spectrumJToolBar.add(jSeparator13);

        oneChargeToggleButton.setSelected(true);
        oneChargeToggleButton.setText("+");
        oneChargeToggleButton.setToolTipText("Single Charge");
        oneChargeToggleButton.setFocusable(false);
        oneChargeToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        oneChargeToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        oneChargeToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        oneChargeToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oneChargeToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(oneChargeToggleButton);

        twoChargesToggleButton.setText("++");
        twoChargesToggleButton.setToolTipText("Double Charge");
        twoChargesToggleButton.setFocusable(false);
        twoChargesToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        twoChargesToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        twoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        twoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(twoChargesToggleButton);

        moreThanTwoChargesToggleButton.setText(">2 ");
        moreThanTwoChargesToggleButton.setToolTipText("More Than Two Charges");
        moreThanTwoChargesToggleButton.setFocusable(false);
        moreThanTwoChargesToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moreThanTwoChargesToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        moreThanTwoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moreThanTwoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(moreThanTwoChargesToggleButton);
        spectrumJToolBar.add(jSeparator15);

        allToggleButton.setText("All");
        allToggleButton.setToolTipText("Display all peaks or just the annotated peaks");
        allToggleButton.setFocusable(false);
        allToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        allToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        allToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        allToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(allToggleButton);
        spectrumJToolBar.add(jSeparator16);

        spectrumHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        spectrumHelpJButton.setToolTipText("Help");
        spectrumHelpJButton.setBorder(null);
        spectrumHelpJButton.setBorderPainted(false);
        spectrumHelpJButton.setContentAreaFilled(false);
        spectrumHelpJButton.setFocusable(false);
        spectrumHelpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        spectrumHelpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
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
        spectrumJToolBar.add(spectrumHelpJButton);

        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(80);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spectrumSplitPane.setOpaque(false);

        sequenceFragmentIonPlotsJPanel.setOpaque(false);
        sequenceFragmentIonPlotsJPanel.setLayout(new javax.swing.BoxLayout(sequenceFragmentIonPlotsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumSplitPane.setTopComponent(sequenceFragmentIonPlotsJPanel);

        spectrumPanel.setOpaque(false);
        spectrumPanel.setLayout(new java.awt.BorderLayout());
        spectrumSplitPane.setRightComponent(spectrumPanel);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumJTabbedPane.setSelectedIndex(2);

        javax.swing.GroupLayout spectrumMainPanelLayout = new javax.swing.GroupLayout(spectrumMainPanel);
        spectrumMainPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumMainPanelLayout.setVerticalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumJTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
        );

        peptidesPsmSpectrumFragmentIonsJSplitPane.setRightComponent(spectrumMainPanel);

        coverageJSplitPane.setLeftComponent(peptidesPsmSpectrumFragmentIonsJSplitPane);

        overviewJSplitPane.setRightComponent(coverageJSplitPane);

        javax.swing.GroupLayout overviewJPanelLayout = new javax.swing.GroupLayout(overviewJPanel);
        overviewJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 957, Short.MAX_VALUE)
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 720, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 977, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(overviewJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 977, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 742, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(overviewJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 742, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @see #proteinTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            proteinTableMouseReleased(null);
        }
}//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Resizes the coverage map according to the new width of the screen.
     *
     * @param evt
     */
    /**
     * Updates tha tables according to the currently selected peptide.
     *
     * @param evt
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased

        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            int row = peptideTable.getSelectedRow();

            if (row != -1) {
                updatePsmSelection(row);

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        int row = psmTable.getSelectedRow();
                        updateSpectrum(row, true);
                    }
                });

                // set the currently selected peptide index
                if (updateProteinStructurePanel) {
                    peptideShakerGUI.setSelectedPeptideIndex((Integer) peptideTable.getValueAt(row, 0));
                }
            }
        }
}//GEN-LAST:event_peptideTableKeyReleased

    /**
     * Updates tha tables according to the currently selected PSM.
     *
     * @param evt
     */
    private void psmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmTableKeyReleased

        if (evt == null || evt.getKeyCode() != KeyEvent.VK_RIGHT || evt.getKeyCode() == KeyEvent.VK_LEFT) {

            int row = psmTable.getSelectedRow();

            if (row != -1) {
                updateSpectrum(row, false);
                String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(row, 0));
                peptideShakerGUI.selectSpectrum(spectrumKey);
            }
        }
}//GEN-LAST:event_psmTableKeyReleased

    /**
     * Updates the spectrum and variability panels with the currently selected
     * fragment ions.
     *
     * @param evt
     */
    private void aIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonToggleButtonActionPerformed
        if (spectrum != null) {
            // update the spectrum plots
            psmTableKeyReleased(null);
        }

        aIonBubblePlotToggleButton.setSelected(aIonToggleButton.isSelected());
        aIonTableToggleButton.setSelected(aIonToggleButton.isSelected());
}//GEN-LAST:event_aIonToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void bIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        bIonBubblePlotToggleButton.setSelected(bIonToggleButton.isSelected());
        bIonTableToggleButton.setSelected(bIonToggleButton.isSelected());
}//GEN-LAST:event_bIonToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void cIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        cIonBubblePlotToggleButton.setSelected(cIonToggleButton.isSelected());
        cIonTableToggleButton.setSelected(cIonToggleButton.isSelected());
}//GEN-LAST:event_cIonToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void xIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        xIonBubblePlotToggleButton.setSelected(xIonToggleButton.isSelected());
        xIonTableToggleButton.setSelected(xIonToggleButton.isSelected());
}//GEN-LAST:event_xIonToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        yIonBubblePlotToggleButton.setSelected(yIonToggleButton.isSelected());
        yIonTableToggleButton.setSelected(yIonToggleButton.isSelected());
}//GEN-LAST:event_yIonToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void zIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        zIonBubblePlotToggleButton.setSelected(zIonToggleButton.isSelected());
        zIonTableToggleButton.setSelected(zIonToggleButton.isSelected());
}//GEN-LAST:event_zIonToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void h2oToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        h2oBubblePlotToggleButton.setSelected(h2oToggleButton.isSelected());
        h2oTableToggleButton.setSelected(h2oToggleButton.isSelected());
}//GEN-LAST:event_h2oToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void nh3ToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3ToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        nh3BubblePlotToggleButton.setSelected(nh3ToggleButton.isSelected());
        nh3TableToggleButton.setSelected(nh3ToggleButton.isSelected());
}//GEN-LAST:event_nh3ToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void otherToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        otherBubblePlotToggleButton.setSelected(otherToggleButton.isSelected());
}//GEN-LAST:event_otherToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void oneChargeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        oneChargeBubblePlotToggleButton.setSelected(oneChargeToggleButton.isSelected());
        oneChargeTableToggleButton.setSelected(oneChargeToggleButton.isSelected());
}//GEN-LAST:event_oneChargeToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void twoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        twoChargesBubblePlotToggleButton.setSelected(twoChargesToggleButton.isSelected());
        twoChargesTableToggleButton.setSelected(twoChargesToggleButton.isSelected());
}//GEN-LAST:event_twoChargesToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void moreThanTwoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
        moreThanTwoChargesBubblePlotToggleButton.setSelected(moreThanTwoChargesToggleButton.isSelected());
}//GEN-LAST:event_moreThanTwoChargesToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonBubblePlotToggleButtonActionPerformed
        aIonToggleButton.setSelected(aIonBubblePlotToggleButton.isSelected());
        aIonTableToggleButton.setSelected(aIonBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_aIonBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void bIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonBubblePlotToggleButtonActionPerformed
        bIonToggleButton.setSelected(bIonBubblePlotToggleButton.isSelected());
        bIonTableToggleButton.setSelected(bIonBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_bIonBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void cIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonBubblePlotToggleButtonActionPerformed
        cIonToggleButton.setSelected(cIonBubblePlotToggleButton.isSelected());
        cIonTableToggleButton.setSelected(cIonBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_cIonBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void xIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonBubblePlotToggleButtonActionPerformed
        xIonToggleButton.setSelected(xIonBubblePlotToggleButton.isSelected());
        xIonTableToggleButton.setSelected(xIonBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_xIonBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonBubblePlotToggleButtonActionPerformed
        yIonToggleButton.setSelected(yIonBubblePlotToggleButton.isSelected());
        yIonTableToggleButton.setSelected(yIonBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_yIonBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void zIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonBubblePlotToggleButtonActionPerformed
        zIonToggleButton.setSelected(zIonBubblePlotToggleButton.isSelected());
        zIonTableToggleButton.setSelected(zIonBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_zIonBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void h2oBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oBubblePlotToggleButtonActionPerformed
        h2oToggleButton.setSelected(h2oBubblePlotToggleButton.isSelected());
        h2oTableToggleButton.setSelected(h2oBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_h2oBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void nh3BubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3BubblePlotToggleButtonActionPerformed
        nh3ToggleButton.setSelected(nh3BubblePlotToggleButton.isSelected());
        nh3TableToggleButton.setSelected(nh3BubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_nh3BubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void otherBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherBubblePlotToggleButtonActionPerformed
        otherToggleButton.setSelected(otherBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_otherBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void oneChargeBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeBubblePlotToggleButtonActionPerformed
        oneChargeToggleButton.setSelected(oneChargeBubblePlotToggleButton.isSelected());
        oneChargeTableToggleButton.setSelected(oneChargeBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_oneChargeBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void twoChargesBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesBubblePlotToggleButtonActionPerformed
        twoChargesToggleButton.setSelected(twoChargesBubblePlotToggleButton.isSelected());
        twoChargesTableToggleButton.setSelected(twoChargesBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_twoChargesBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void moreThanTwoChargesBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesBubblePlotToggleButtonActionPerformed
        moreThanTwoChargesToggleButton.setSelected(moreThanTwoChargesBubblePlotToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_moreThanTwoChargesBubblePlotToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void barsBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_barsBubblePlotToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_barsBubblePlotToggleButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * html link.
     *
     * @param evt
     */
    private void proteinTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseMoved
        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.columnAtPoint(evt.getPoint());

        if (column == proteinTable.getColumn("Accession").getModelIndex() && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinTable.getColumn("PI").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Changes the cursor back to the default cursor a hand.
     *
     * @param evt
     */
    private void proteinTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinTableMouseExited

    /**
     * Update the type of the ion table.
     */
    private void barsIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_barsIonTableToggleButtonActionPerformed

        if (barsIonTableToggleButton.isSelected()) {
            mzValuesIonTableToggleButton.setSelected(false);
        } else {
            mzValuesIonTableToggleButton.setSelected(true);
        }

        int row = psmTable.getSelectedRow();

        if (row != -1) {

            String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(row, 0));

            if (displaySpectrum) {

                try {
                    MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                    if (currentSpectrum != null) {
                        HashSet<Peak> peaks = currentSpectrum.getPeakList();

                        if (peaks == null || peaks.isEmpty()) {
                            // do nothing, peaks list not found
                        } else {

                            // get the spectrum annotations
                            String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                            Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();

                            // create and display the fragment ion table
                            if (psmTable.getSelectedRowCount() == 1 && mzValuesIonTableToggleButton.isSelected()) {
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, peptideShakerGUI.getIonsCurrentlyMatched(), peptideShakerGUI.getAnnotationPreferences().getIonTypes(),
                                        oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected()));
                            } else {
                                ArrayList<ArrayList<IonMatch>> allAnnotations = getAnnotationsForAllSelectedSpectra();
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, allAnnotations, getSelectedSpectra(), peptideShakerGUI.getAnnotationPreferences().getIonTypes(),
                                        oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected()));
                            }
                        }
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        }
    }//GEN-LAST:event_barsIonTableToggleButtonActionPerformed

    /**
     * Update the type of the ion table.
     */
    private void mzValuesIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mzValuesIonTableToggleButtonActionPerformed

        if (mzValuesIonTableToggleButton.isSelected()) {
            barsIonTableToggleButton.setSelected(false);
        } else {
            barsIonTableToggleButton.setSelected(true);
        }

        barsIonTableToggleButtonActionPerformed(null);
    }//GEN-LAST:event_mzValuesIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void aIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonTableToggleButtonActionPerformed
        aIonToggleButton.setSelected(aIonTableToggleButton.isSelected());
        aIonBubblePlotToggleButton.setSelected(aIonTableToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_aIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void bIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonTableToggleButtonActionPerformed
        bIonToggleButton.setSelected(bIonTableToggleButton.isSelected());
        bIonBubblePlotToggleButton.setSelected(bIonTableToggleButton.isSelected());
        bIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_bIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void cIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonTableToggleButtonActionPerformed
        cIonToggleButton.setSelected(cIonTableToggleButton.isSelected());
        cIonBubblePlotToggleButton.setSelected(cIonTableToggleButton.isSelected());
        cIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_cIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void xIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonTableToggleButtonActionPerformed
        xIonToggleButton.setSelected(xIonTableToggleButton.isSelected());
        xIonBubblePlotToggleButton.setSelected(xIonTableToggleButton.isSelected());
        xIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_xIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void yIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonTableToggleButtonActionPerformed
        yIonToggleButton.setSelected(yIonTableToggleButton.isSelected());
        yIonBubblePlotToggleButton.setSelected(yIonTableToggleButton.isSelected());
        yIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_yIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void zIonTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonTableToggleButtonActionPerformed
        zIonToggleButton.setSelected(zIonTableToggleButton.isSelected());
        zIonBubblePlotToggleButton.setSelected(zIonTableToggleButton.isSelected());
        zIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_zIonTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void h2oTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oTableToggleButtonActionPerformed
        h2oToggleButton.setSelected(h2oTableToggleButton.isSelected());
        h2oBubblePlotToggleButton.setSelected(h2oTableToggleButton.isSelected());
        h2oToggleButtonActionPerformed(null);
    }//GEN-LAST:event_h2oTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void nh3TableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3TableToggleButtonActionPerformed
        nh3ToggleButton.setSelected(nh3TableToggleButton.isSelected());
        nh3BubblePlotToggleButton.setSelected(nh3TableToggleButton.isSelected());
        nh3ToggleButtonActionPerformed(null);
    }//GEN-LAST:event_nh3TableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void oneChargeTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeTableToggleButtonActionPerformed
        oneChargeToggleButton.setSelected(oneChargeTableToggleButton.isSelected());
        oneChargeBubblePlotToggleButton.setSelected(oneChargeTableToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_oneChargeTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void twoChargesTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesTableToggleButtonActionPerformed
        twoChargesToggleButton.setSelected(twoChargesTableToggleButton.isSelected());
        twoChargesBubblePlotToggleButton.setSelected(twoChargesTableToggleButton.isSelected());
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_twoChargesTableToggleButtonActionPerformed

    /**
     * Updates the spectrum, bubble plot and ion table.
     * 
     * @param evt 
     */
    private void psmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseReleased
        int row = psmTable.getSelectedRow();

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            updateSpectrum(row, false);

            String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(row, 0));
            peptideShakerGUI.selectSpectrum(spectrumKey);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_psmTableMouseReleased

    /**
     * Updates the tables according to the currently selected protein.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased
        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // set the currently selected protein index
            peptideShakerGUI.setSelectedProteinIndex((Integer) proteinTable.getValueAt(row, 0));

            // set the accession number in the annotation tab
            String accessionNumber = (String) proteinTable.getValueAt(row, proteinTable.getColumn("Accession").getModelIndex());

            if (accessionNumber.lastIndexOf("a href") != -1) {
                accessionNumber = accessionNumber.substring(accessionNumber.lastIndexOf("\">") + 2);
                accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf("<"));
            }

            peptideShakerGUI.setSelectedProteinAccession(accessionNumber);

            // update the peptide selection
            updatedPeptideSelection(row);

            // update the sequence coverage map
            updateSequenceCoverageMap(row);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            // open protein link in web browser
            if (column == proteinTable.getColumn("Accession").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) proteinTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }

            // open the protein inference dialog
            if (column == proteinTable.getColumn("PI").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                String proteinKey = proteinTableMap.get(getProteinKey(row));
                new ProteinInferenceDialog(peptideShakerGUI, proteinKey, peptideShakerGUI.getIdentification());
            }
        }
    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Updates the tables according to the currently selected peptide.
     *
     * @param evt
     */
    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased
        int row = peptideTable.getSelectedRow();
        int column = peptideTable.getSelectedColumn();

        if (row != -1) {

            updatePsmSelection(row);

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    int row = psmTable.getSelectedRow();
                    updateSpectrum(row, true);
                }
            });

            // set the currently selected peptide index
            if (updateProteinStructurePanel) {
                peptideShakerGUI.setSelectedPeptideIndex((Integer) peptideTable.getValueAt(row, 0));
            }

            // open the protein inference at the petide level dialog
            if (column == peptideTable.getColumn("PI").getModelIndex()) {
                try {
                    String proteinKey = proteinTableMap.get(getProteinKey(proteinTable.getSelectedRow()));
                    String peptideKey = peptideTableMap.get(getPeptideKey(row));
                    PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                    ArrayList<String> allProteins = new ArrayList<String>();

                    // allProteins.add(currentProteinMatch.getMainMatch());
                    List<String> proteinMatchAccessions = Arrays.asList(ProteinMatch.getAccessions(proteinKey));
                    for (String protein : currentPeptideMatch.getTheoreticPeptide().getParentProteins()) {
                        if (!proteinMatchAccessions.contains(protein)) {
                            allProteins.add(protein);
                        }
                    }

                    new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, currentPeptideMatch.getTheoreticPeptide().getSequence(), allProteins);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        }
    }//GEN-LAST:event_peptideTableMouseReleased

    /**
     * Update the spectrum to show all or just the annotated ions.
     * 
     * @param evt 
     */
    private void allToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allToggleButtonActionPerformed
        if (spectrum != null) {
            // update the spectrum plots
            psmTableKeyReleased(null);
        }
    }//GEN-LAST:event_allToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void otherTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherTableToggleButtonActionPerformed
        // currently disabled
        // @TODO: add support for displaying the immonium and precursor information?
    }//GEN-LAST:event_otherTableToggleButtonActionPerformed

    /**
     * @see #aIonBubblePlotToggleButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void moreThanTwoChargesTableToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesTableToggleButtonActionPerformed
        // currently disabled
        // @TODO: should this be implemented?
    }//GEN-LAST:event_moreThanTwoChargesTableToggleButtonActionPerformed

    /**
     * Updates the PSM selection and corresponding plots.
     * 
     * @param evt 
     */
    private void psmTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseClicked

        int row = psmTable.rowAtPoint(evt.getPoint());

        if (row != -1) {
            updateSpectrum(row, false);
            String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(row, 0));
            peptideShakerGUI.selectSpectrum(spectrumKey);

            if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 2) {
                peptideShakerGUI.openSpectrumIdTab();
            }
        }
    }//GEN-LAST:event_psmTableMouseClicked

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
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/SpectrumPanel.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_spectrumHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void bubblePlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bubblePlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_bubblePlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void bubblePlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bubblePlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_bubblePlotHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void bubblePlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bubblePlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/BubblePlot.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_bubblePlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void ionTableHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ionTableHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_ionTableHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void ionTableHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ionTableHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ionTableHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void ionTableHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ionTableHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/IonTable.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ionTableHelpJButtonActionPerformed

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link.
     *
     * @param evt
     */
    private void peptideTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseMoved
        int row = peptideTable.rowAtPoint(evt.getPoint());
        int column = peptideTable.columnAtPoint(evt.getPoint());

        if (peptideTable.getValueAt(row, column) != null) {
            if (column == peptideTable.getColumn("PI").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_peptideTableMouseMoved

    /**
     * Try to get the protein sequence index from the protein sequence model.
     * 
     * @param evt 
     */
    private void coverageTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseMoved

        // @TODO: not working perfectly yet...

        double paddingInPercent = 0.0115; // best so far: 0.012
        double xValue = evt.getPoint().getX();
        double cellWidth = coverageTable.getWidth();

        double width = (xValue - cellWidth * paddingInPercent) / (cellWidth - 2 * cellWidth * paddingInPercent);

        if (currentProteinSequence != null) {

            int residueNumber = (int) (width * currentProteinSequence.length());

            String tooltipText = "<html>";

            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                if (residueNumber >= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex())
                        && residueNumber <= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex())) {

                    String modifications = (String) peptideTable.getValueAt(i, peptideTable.getColumn("Modifications").getModelIndex());

                    if (modifications == null) {
                        modifications = "";
                    } else {
                        modifications = " (" + modifications + ")";
                    }

                    tooltipText += peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex()) + " - "
                            + peptideTable.getValueAt(i, peptideTable.getColumn("Sequence").getModelIndex())
                            + " - " + peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex()) + modifications + "<br>";
                }
            }

            if (!tooltipText.equalsIgnoreCase("<html>")) {
                coverageTable.setToolTipText(tooltipText);
            } else {
                if (residueNumber > 0 && residueNumber <= currentProteinSequence.length()) {
                    coverageTable.setToolTipText(residueNumber + ": " + currentProteinSequence.substring(residueNumber, residueNumber + 1));
                }
            }
        }
    }//GEN-LAST:event_coverageTableMouseMoved
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton aIonBubblePlotToggleButton;
    private javax.swing.JToggleButton aIonTableToggleButton;
    private javax.swing.JToggleButton aIonToggleButton;
    private javax.swing.JToggleButton allToggleButton;
    private javax.swing.JToggleButton bIonBubblePlotToggleButton;
    private javax.swing.JToggleButton bIonTableToggleButton;
    private javax.swing.JToggleButton bIonToggleButton;
    private javax.swing.JToggleButton barsBubblePlotToggleButton;
    private javax.swing.JToggleButton barsIonTableToggleButton;
    private javax.swing.JPanel bubbleJPanel;
    private javax.swing.JButton bubblePlotHelpJButton;
    private javax.swing.JToolBar bubblePlotJToolBar;
    private javax.swing.JPanel bubblePlotTabJPanel;
    private javax.swing.JToggleButton cIonBubblePlotToggleButton;
    private javax.swing.JToggleButton cIonTableToggleButton;
    private javax.swing.JToggleButton cIonToggleButton;
    private javax.swing.JSplitPane coverageJSplitPane;
    private javax.swing.JTable coverageTable;
    private javax.swing.JScrollPane coverageTableScrollPane;
    private javax.swing.JPanel fragmentIonJPanel;
    private javax.swing.JScrollPane fragmentIonsJScrollPane;
    private javax.swing.JToggleButton h2oBubblePlotToggleButton;
    private javax.swing.JToggleButton h2oTableToggleButton;
    private javax.swing.JToggleButton h2oToggleButton;
    private javax.swing.ButtonGroup ionTableButtonGroup;
    private javax.swing.JButton ionTableHelpJButton;
    private javax.swing.JToolBar ionTableJToolBar;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator12;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator14;
    private javax.swing.JToolBar.Separator jSeparator15;
    private javax.swing.JToolBar.Separator jSeparator16;
    private javax.swing.JToolBar.Separator jSeparator17;
    private javax.swing.JToolBar.Separator jSeparator18;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JToggleButton moreThanTwoChargesBubblePlotToggleButton;
    private javax.swing.JToggleButton moreThanTwoChargesTableToggleButton;
    private javax.swing.JToggleButton moreThanTwoChargesToggleButton;
    private javax.swing.JToggleButton mzValuesIonTableToggleButton;
    private javax.swing.JToggleButton nh3BubblePlotToggleButton;
    private javax.swing.JToggleButton nh3TableToggleButton;
    private javax.swing.JToggleButton nh3ToggleButton;
    private javax.swing.JToggleButton oneChargeBubblePlotToggleButton;
    private javax.swing.JToggleButton oneChargeTableToggleButton;
    private javax.swing.JToggleButton oneChargeToggleButton;
    private javax.swing.JToggleButton otherBubblePlotToggleButton;
    private javax.swing.JToggleButton otherTableToggleButton;
    private javax.swing.JToggleButton otherToggleButton;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JSplitPane overviewJSplitPane;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JSplitPane peptidesPsmJSplitPane;
    private javax.swing.JSplitPane peptidesPsmSpectrumFragmentIonsJSplitPane;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JPanel psmJPanel;
    private javax.swing.JTable psmTable;
    private javax.swing.JPanel sequenceCoverageJPanel;
    private javax.swing.JPanel sequenceFragmentIonPlotsJPanel;
    private javax.swing.JScrollPane spectraScrollPane;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JTabbedPane spectrumJTabbedPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JPanel spectrumMainPanel;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JSplitPane spectrumSplitPane;
    private javax.swing.JToggleButton twoChargesBubblePlotToggleButton;
    private javax.swing.JToggleButton twoChargesTableToggleButton;
    private javax.swing.JToggleButton twoChargesToggleButton;
    private javax.swing.JToggleButton xIonBubblePlotToggleButton;
    private javax.swing.JToggleButton xIonTableToggleButton;
    private javax.swing.JToggleButton xIonToggleButton;
    private javax.swing.JToggleButton yIonBubblePlotToggleButton;
    private javax.swing.JToggleButton yIonTableToggleButton;
    private javax.swing.JToggleButton yIonToggleButton;
    private javax.swing.JToggleButton zIonBubblePlotToggleButton;
    private javax.swing.JToggleButton zIonTableToggleButton;
    private javax.swing.JToggleButton zIonToggleButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Select the given protein index in the protein table.
     * 
     * @param proteinIndex the protein index to select
     */
    public void setSelectedProteinIndex(Integer proteinIndex) {

        boolean indexFound = false;

        for (int i = 0; i < proteinTable.getRowCount() && !indexFound; i++) {
            if (((Integer) proteinTable.getValueAt(i, 0)).intValue() == proteinIndex.intValue()) {
                indexFound = true;
                proteinTable.setRowSelectionInterval(i, i);
                proteinTable.scrollRectToVisible(proteinTable.getCellRect(i, 0, false));
            }
        }

        updateProteinStructurePanel = false;
        proteinTableMouseReleased(null);
        updateProteinStructurePanel = true;
    }

    /**
     * Select the given peptide index in the peptide table.
     * 
     * @param peptideIndex the peptide index to select
     */
    public void setSelectedPeptideIndex(Integer peptideIndex) {

        boolean indexFound = false;

        for (int i = 0; i < peptideTable.getRowCount() && !indexFound; i++) {
            if (((Integer) peptideTable.getValueAt(i, 0)).intValue() == peptideIndex.intValue()) {
                indexFound = true;
                peptideTable.setRowSelectionInterval(i, i);
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(i, 0, false));
            }
        }

        updateProteinStructurePanel = false;
        peptideTableMouseReleased(null);
        updateProteinStructurePanel = true;
    }

    /**
     * Displays or hide sparklines in the tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Spectrum Counting").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            // ignore error
        }

        proteinTable.revalidate();
        proteinTable.repaint();

        peptideTable.revalidate();
        peptideTable.repaint();

        psmTable.revalidate();
        psmTable.repaint();
    }

    /**
     * Returns the peptide key for the given row.
     *
     * @param row
     * @return
     */
    private Integer getPeptideKey(int row) {
        return (Integer) peptideTable.getValueAt(row, 0);
    }

    /**
     * Returns the protein key for the given row.
     *
     * @param row
     * @return
     */
    private Integer getProteinKey(int row) {
        return (Integer) proteinTable.getValueAt(row, 0);
    }

    /**
     * Returns a list of keys of the displayed proteins
     * @return a list of keys of the displayed proteins 
     */
    public ArrayList<String> getDisplayedProteins() {
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < proteinTable.getRowCount(); i++) {
            result.add(proteinTableMap.get(getProteinKey(i)));
        }
        return new ArrayList<String>(proteinTableMap.values());
    }

    /**
     * Returns a list of keys of the displayed peptides
     * @return a list of keys of the displayed peptides 
     */
    public ArrayList<String> getDisplayedPeptides() {
        return new ArrayList<String>(peptideTableMap.values());
    }

    /**
     * Returns a list of keys of the displayed PSMs
     * @return a list of keys of the displayed PSMs
     */
    public ArrayList<String> getDisplayedPsms() {
        return new ArrayList<String>(psmTableMap.values());
    }

    /**
     * Updates the split panel divider location for the protein panel.
     */
    private void updateProteinTableSeparator() {

        if (displayProteins) {
            overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() / 100 * 30);
        } else {
            overviewJSplitPane.setDividerLocation(0);
        }

        if (!displayPeptidesAndPSMs && !displaySpectrum) {
            overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() / 100 * 70);
            coverageJSplitPane.setDividerLocation(0);
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateSequenceCoverageSeparator();
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
            }
        });
    }

    /**
     * Updates the split panel divider location for the peptide/psm and spectrum panel.
     */
    private void updatePeptidesAndPsmsSeparator() {

        if (displayPeptidesAndPSMs && displaySpectrum) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(peptidesPsmSpectrumFragmentIonsJSplitPane.getWidth() / 2);
        } else if (displayPeptidesAndPSMs && !displaySpectrum) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(peptidesPsmSpectrumFragmentIonsJSplitPane.getWidth());
        } else if (!displayPeptidesAndPSMs && displaySpectrum) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(0);
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateSequenceCoverageSeparator();
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
            }
        });
    }

    /**
     * Updates the split panel divider location for the coverage pane.
     */
    private void updateSequenceCoverageSeparator() {

        if (displayCoverage) {
            coverageJSplitPane.setDividerLocation(coverageJSplitPane.getHeight() - 75);
        } else {
            coverageJSplitPane.setDividerLocation(Integer.MAX_VALUE);
        }

        if (!displayPeptidesAndPSMs && !displaySpectrum) {

            if (!displayCoverage) {
                overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight());
            } else {
                overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() - 75);
                coverageJSplitPane.setDividerLocation(0);
            }
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
            }
        });
    }

    /**
     * Method called whenever the component is resized to maintain the look of the GUI
     */
    public void updateSeparators() {

        updateProteinTableSeparator();
        updatePeptidesAndPsmsSeparator();
        peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateSequenceCoverageSeparator();
                overviewJPanel.revalidate();
                overviewJPanel.repaint();
                updateBubblePlot();
            }
        });
    }

    /**
     * Sets the whether the protein coverage and the spectrum shall be displayed
     * 
     * @param displayProteins           boolean indicating whether the proteins shall be displayed
     * @param displayPeptidesAndPSMs    boolean indicating whether the peptides and psms shall be displayed
     * @param displayCoverage           boolean indicating whether the protein coverage shall be displayed
     * @param displaySpectrum           boolean indicating whether the spectrum shall be displayed
     */
    public void setDisplayOptions(boolean displayProteins, boolean displayPeptidesAndPSMs, boolean displayCoverage, boolean displaySpectrum) {
        this.displayProteins = displayProteins;
        this.displayPeptidesAndPSMs = displayPeptidesAndPSMs;
        this.displayCoverage = displayCoverage;
        this.displaySpectrum = displaySpectrum;
    }

    /**
     * Updated the bubble plot with the current PSMs.
     */
    public void updateBubblePlot() {

        if (peptideTable.getSelectedRow() != -1 && displaySpectrum) {
            try {
                ArrayList<String> selectedIndexes = new ArrayList<String>();

                // get the list of currently selected psms
                ArrayList<String> selectedPsmKeys = new ArrayList<String>();

                int[] selectedRows = psmTable.getSelectedRows();

                for (int i = 0; i < selectedRows.length; i++) {
                    selectedPsmKeys.add(psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0)));
                    selectedIndexes.add(psmTable.getValueAt(selectedRows[i], 0) + " "
                            + psmTable.getValueAt(selectedRows[i], psmTable.getColumn("Charge").getModelIndex()) + "+");
                }

                ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();
                ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();
                SpectrumAnnotator miniAnnotator = new SpectrumAnnotator();

                String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                PeptideMatch selectedPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

                for (String spectrumKey : selectedPeptideMatch.getSpectrumMatches()) {

                    if (selectedPsmKeys.contains(spectrumKey)) {

                        MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                        if (currentSpectrum != null) {
                            annotationPreferences.setCurrentSettings(selectedPeptideMatch.getTheoreticPeptide(), currentSpectrum.getPrecursor().getCharge().value);
                            ArrayList<IonMatch> annotations = miniAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                    annotationPreferences.getNeutralLosses(),
                                    annotationPreferences.getValidatedCharges(),
                                    currentSpectrum,
                                    selectedPeptideMatch.getTheoreticPeptide(),
                                    currentSpectrum.getIntensityLimit(annotationPreferences.shallAnnotateMostIntensePeaks()),
                                    annotationPreferences.getMzTolerance());
                            allAnnotations.add(annotations);
                            allSpectra.add(currentSpectrum);
                        }
                    }
                }

                // @TODO: rewrite the charge selection below when the new ion selection gui has been implemented!

                double bubbleScale = annotationPreferences.getMzTolerance() * 10 * peptideShakerGUI.getBubbleScale();

                if (peptideShakerGUI.useRelativeError()) {
                    bubbleScale = annotationPreferences.getMzTolerance() * 10000 * peptideShakerGUI.getBubbleScale();
                }

                MassErrorBubblePlot massErrorBubblePlot = new MassErrorBubblePlot(
                        selectedIndexes,
                        allAnnotations, annotationPreferences.getIonTypes(), allSpectra, annotationPreferences.getMzTolerance(),
                        bubbleScale,
                        annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                        annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4)),
                        selectedIndexes.size() == 1, barsBubblePlotToggleButton.isSelected(),
                        peptideShakerGUI.useRelativeError());

                bubbleJPanel.removeAll();
                bubbleJPanel.add(massErrorBubblePlot);
                bubbleJPanel.revalidate();
                bubbleJPanel.repaint();
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Updates the sequence coverage pane.
     *
     * @param proteinAccession
     */
    private void updateSequenceCoverage(String proteinAccession) {

        ArrayList<Integer> selectedPeptideStart = new ArrayList<Integer>();
        ArrayList<Integer> selectedPeptideEnd = new ArrayList<Integer>();

        try {
            currentProteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();

            ((TitledBorder) sequenceCoverageJPanel.getBorder()).setTitle("Protein Sequence Coverage ("
                    + Util.roundDouble((Double) proteinTable.getValueAt(proteinTable.getSelectedRow(), proteinTable.getColumn("Coverage").getModelIndex()), 2)
                    + "%, " + currentProteinSequence.length() + " AA)");
            sequenceCoverageJPanel.repaint();

            if (currentProteinSequence.length() < MAX_SEQUENCE_LENGTH) {

                String tempSequence = currentProteinSequence;

                if (peptideTable.getSelectedRow() != -1) {

                    String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                    String peptideSequence = Peptide.getSequence(peptideKey);

                    int startIndex = 0;
                    while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                        startIndex = tempSequence.lastIndexOf(peptideSequence) + 1;
                        selectedPeptideStart.add(startIndex);
                        selectedPeptideEnd.add(startIndex + peptideSequence.length());
                        tempSequence = currentProteinSequence.substring(0, startIndex);
                    }
                }

                // an array containing the coverage index for each residue
                coverage = new int[currentProteinSequence.length() + 1];

                PSParameter pSParameter = new PSParameter();
                // iterate the peptide table and store the coverage for each validated peptide
                for (int i = 0; i < peptideTable.getRowCount(); i++) {
                    String peptideKey = peptideTableMap.get(getPeptideKey(i));
                    pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, pSParameter);
                    if (pSParameter.isValidated()) {
                        String peptideSequence = Peptide.getSequence(peptideKey);
                        tempSequence = currentProteinSequence;

                        while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                            int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                            int peptideTempEnd = peptideTempStart + peptideSequence.length();
                            for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                                coverage[j]++;
                            }
                            tempSequence = currentProteinSequence.substring(0, peptideTempStart);
                        }
                    }
                }

                // create the coverage plot
                ArrayList<JSparklinesDataSeries> sparkLineDataSeriesCoverage = new ArrayList<JSparklinesDataSeries>();

                for (int i = 0; i < coverage.length; i++) {

                    boolean covered = coverage[i] > 0;
                    int counter = 1;

                    if (covered) {
                        while (i + 1 < coverage.length && coverage[i + 1] > 0) {
                            counter++;
                            i++;

                            // we need to start a new peptide in order to highlight
                            if (selectedPeptideEnd.contains(new Integer(i + 1)) || selectedPeptideStart.contains(new Integer(i + 1))) {
                                break;
                            }
                        }
                    } else {
                        while (i + 1 < coverage.length && coverage[i + 1] == 0) {
                            counter++;
                            i++;
                        }
                    }

                    ArrayList<Double> data = new ArrayList<Double>();
                    data.add(new Double(counter));

                    JSparklinesDataSeries sparklineDataseries;

                    if (covered) {

                        if (selectedPeptideEnd.contains(new Integer(i + 1))) {
                            sparklineDataseries = new JSparklinesDataSeries(data, new Color(255, 0, 0), null);
                        } else {
                            sparklineDataseries = new JSparklinesDataSeries(data, peptideShakerGUI.getSparklineColor(), null);
                        }

                    } else {
                        sparklineDataseries = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                    }

                    sparkLineDataSeriesCoverage.add(sparklineDataseries);
                }

                JSparklinesDataset dataset = new JSparklinesDataset(sparkLineDataSeriesCoverage);
                coverageTable.setValueAt(dataset, 0, 0);
            } else {
                ((TitledBorder) sequenceCoverageJPanel.getBorder()).setTitle("Protein Sequence Coverage ("
                        + Util.roundDouble((Double) proteinTable.getValueAt(proteinTable.getSelectedRow(), proteinTable.getColumn("Coverage").getModelIndex()), 2)
                        + "%, " + currentProteinSequence.length() + " AA)" + " - Too long to display...");
                sequenceCoverageJPanel.repaint();
            }
        } catch (Exception e) {
            int debug = 1;
            e.printStackTrace();
        }
    }

    /**
     * Updates the spectrum annotation. Used when the user updates the annotation 
     * accuracy.
     */
    public void updateSpectrum() {
        updateSpectrum(psmTable.getSelectedRow(), false);
    }

    /**
     * Update the spectrum to the currently selected PSM.
     *
     * @param row           the row index of the PSM
     * @param resetMzRange  if true the mz range is reset, if false the current zoom range is kept
     */
    private void updateSpectrum(int row, boolean resetMzRange) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(row, 0));

            if (displaySpectrum) {

                try {
                    MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                    if (currentSpectrum != null) {
                        HashSet<Peak> peaks = currentSpectrum.getPeakList();

                        if (peaks == null || peaks.isEmpty()) {
                            // do nothing, peaks list not found
                        } else {

                            double lowerMzZoomRange = 0;
                            double upperMzZoomRange = maxPsmMzValue;

                            if (spectrum != null && spectrum.getXAxisZoomRangeLowerValue() != 0 && !resetMzRange) {
                                lowerMzZoomRange = spectrum.getXAxisZoomRangeLowerValue();
                                upperMzZoomRange = spectrum.getXAxisZoomRangeUpperValue();
                            }

                            // add the data to the spectrum panel
                            Precursor precursor = currentSpectrum.getPrecursor();
                            spectrum = new SpectrumPanel(
                                    currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                                    precursor.getMz(), precursor.getCharge().toString(),
                                    "", 40, false, false, false, 2, false);
                            spectrum.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getMzTolerance());
                            spectrum.setBorder(null);

                            // get the spectrum annotations
                            String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                            Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                            SpectrumAnnotator spectrumAnnotator = peptideShakerGUI.getSpectrumAnnorator();
                            AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
                            annotationPreferences.setCurrentSettings(currentPeptide, currentSpectrum.getPrecursor().getCharge().value);
                            ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                    annotationPreferences.getNeutralLosses(),
                                    annotationPreferences.getValidatedCharges(),
                                    currentSpectrum, currentPeptide,
                                    currentSpectrum.getIntensityLimit(annotationPreferences.shallAnnotateMostIntensePeaks()),
                                    annotationPreferences.getMzTolerance());
                            spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                            spectrum.rescale(lowerMzZoomRange, upperMzZoomRange);
                            updateAnnotationButtons();

                            // show all or just the annotated peaks
                            spectrum.showAnnotatedPeaksOnly(!allToggleButton.isSelected());

                            // add the spectrum panel to the frame
                            spectrumPanel.removeAll();
                            spectrumPanel.add(spectrum);
                            spectrumPanel.revalidate();
                            spectrumPanel.repaint();

                            // create and display the fragment ion table
                            if (psmTable.getSelectedRowCount() == 1 && mzValuesIonTableToggleButton.isSelected()) {
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, annotations, annotationPreferences.getIonTypes(),
                                        oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected()));
                            } else {
                                ArrayList<ArrayList<IonMatch>> allAnnotations = getAnnotationsForAllSelectedSpectra();
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, allAnnotations, getSelectedSpectra(), annotationPreferences.getIonTypes(),
                                        oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected()));
                            }

                            // create the sequence fragment ion view
                            sequenceFragmentIonPlotsJPanel.removeAll();
                            SequenceFragmentationPanel sequenceFragmentationPanel =
                                    new SequenceFragmentationPanel(currentPeptide.getNTerminal() + "-" + currentPeptide.getSequence() + "-" + currentPeptide.getCTerminal(), 
                                            annotations, true);
                            sequenceFragmentationPanel.setMinimumSize(new Dimension(sequenceFragmentationPanel.getPreferredSize().width, sequenceFragmentationPanel.getHeight()));
                            sequenceFragmentationPanel.setOpaque(false);
                            sequenceFragmentIonPlotsJPanel.add(sequenceFragmentationPanel);

                            // create the intensity histograms
                            sequenceFragmentIonPlotsJPanel.add(new IntensityHistogram(
                                    annotations, annotationPreferences.getIonTypes(), currentSpectrum,
                                    peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks(),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4))));

                            // @TODO: rewrite the charge selection above and below when the new ion selection gui has been implemented!

                            // create the miniature mass error plot
                            MassErrorPlot massErrorPlot = new MassErrorPlot(
                                    annotations, annotationPreferences.getIonTypes(), currentSpectrum,
                                    annotationPreferences.getMzTolerance(),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4)),
                                    peptideShakerGUI.useRelativeError());

                            if (massErrorPlot.getNumberOfDataPointsInPlot() > 0) {
                                sequenceFragmentIonPlotsJPanel.add(massErrorPlot);
                            }

                            // update the UI
                            sequenceFragmentIonPlotsJPanel.revalidate();
                            sequenceFragmentIonPlotsJPanel.repaint();

                            // update the bubble plot
                            updateBubblePlot();

                            // disable the spectrum tab if more than one psm is selected
                            spectrumJTabbedPane.setEnabledAt(2, psmTable.getSelectedRowCount() == 1);
                            peptideShakerGUI.enableSpectrumExport(psmTable.getSelectedRowCount() == 1);

                            // move to the bubble plot tab if more than one psm is selected and the spectrum tab was selected
                            if (psmTable.getSelectedRowCount() > 1 && spectrumJTabbedPane.getSelectedIndex() == 2) {
                                spectrumJTabbedPane.setSelectedIndex(1);
                            }

                            if (psmTable.getSelectedRowCount() > 1) {
                                spectrumJTabbedPane.setToolTipTextAt(2, "Available for single spectrum selection only");
                                mzValuesIonTableToggleButton.setEnabled(false);
                                mzValuesIonTableToggleButton.setSelected(false);
                                barsIonTableToggleButton.setSelected(true);
                            } else {
                                spectrumJTabbedPane.setToolTipTextAt(2, null);
                                mzValuesIonTableToggleButton.setEnabled(true);
                            }

                            // update the panel border title
                            if (psmTable.getSelectedRowCount() == 1) {

                                int selectedRow = peptideTable.getSelectedRow();
                                int start = (Integer) peptideTable.getValueAt(selectedRow, peptideTable.getColumn("Start").getModelIndex()) - 1;
                                int end = (Integer) peptideTable.getValueAt(selectedRow, peptideTable.getColumn("End").getModelIndex());

                                String before = "";
                                String after = "";

                                // @TODO: make the number of residues before/after up to the user?

                                if (start - 2 >= 0) {
                                    before = currentProteinSequence.substring(start - 2, start) + " - ";
                                } else if (start - 1 >= 0) {
                                    before = currentProteinSequence.substring(start - 1, start) + " - ";
                                }

                                if (end + 2 <= currentProteinSequence.length()) {
                                    after = " - " + currentProteinSequence.substring(end, end + 2);
                                } else if (end + 1 <= currentProteinSequence.length()) {
                                    after = " - " + currentProteinSequence.substring(end, end + 1);
                                }

                                ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                                        "Spectrum & Fragment Ions (" + before + currentPeptide.getSequence() + after
                                        + "   " + precursor.getCharge().toString() + "   "
                                        + Util.roundDouble(precursor.getMz(), 4) + " m/z)");
                            } else {

                                // get the current charges
                                ArrayList<Integer> currentCharges = new ArrayList<Integer>();
                                int[] selectedRows = psmTable.getSelectedRows();

                                for (int i = 0; i < selectedRows.length; i++) {
                                    Integer tempCharge = (Integer) psmTable.getValueAt(selectedRows[i], psmTable.getColumn("Charge").getModelIndex());

                                    if (!currentCharges.contains(tempCharge)) {
                                        currentCharges.add(tempCharge);
                                    }
                                }

                                Collections.sort(currentCharges);

                                String chargeStates = currentCharges.get(0) + "+";

                                for (int i = 1; i < currentCharges.size(); i++) {
                                    chargeStates += " and " + currentCharges.get(i) + "+";
                                }

                                ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                                        "Spectrum & Fragment Ions (" + currentPeptide.getSequence()
                                        + "   " + chargeStates
                                        + "   " + selectedRows.length + " spectra)");
                            }

                            spectrumMainPanel.revalidate();
                            spectrumMainPanel.repaint();
                        }
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        } else {

            // nothing to display, empty previous results
            spectrumPanel.removeAll();
            spectrumPanel.revalidate();
            spectrumPanel.repaint();

            sequenceFragmentIonPlotsJPanel.removeAll();
            sequenceFragmentIonPlotsJPanel.revalidate();
            sequenceFragmentIonPlotsJPanel.repaint();

            fragmentIonsJScrollPane.setViewportView(null);
            fragmentIonsJScrollPane.revalidate();
            fragmentIonsJScrollPane.repaint();

            bubbleJPanel.removeAll();
            bubbleJPanel.revalidate();
            bubbleJPanel.repaint();

            ((TitledBorder) spectrumMainPanel.getBorder()).setTitle("Spectrum & Fragment Ions");
            spectrumMainPanel.repaint();
        }
    }

    /**
     * @TODO: remove this method once we have an adapted GUI
     */
    private void updateAnnotationButtons() {
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        aIonToggleButton.setSelected(false);
        bIonToggleButton.setSelected(false);
        cIonToggleButton.setSelected(false);
        xIonToggleButton.setSelected(false);
        yIonToggleButton.setSelected(false);
        zIonToggleButton.setSelected(false);
        otherToggleButton.setSelected(false);
        for (PeptideFragmentIonType ionType : annotationPreferences.getIonTypes()) {
            if (ionType == PeptideFragmentIonType.A_ION) {
                aIonToggleButton.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.B_ION) {
                bIonToggleButton.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.C_ION) {
                cIonToggleButton.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.X_ION) {
                xIonToggleButton.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.Y_ION) {
                yIonToggleButton.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.Z_ION) {
                zIonToggleButton.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.IMMONIUM
                    || ionType == PeptideFragmentIonType.PRECURSOR_ION) {
                otherToggleButton.setSelected(true);
            }
        }

        h2oToggleButton.setSelected(false);
        nh3ToggleButton.setSelected(false);
        for (NeutralLoss neutralLoss : annotationPreferences.getNeutralLosses().keySet()) {
            if (neutralLoss.isSameAs(NeutralLoss.H2O)) {
                h2oToggleButton.setSelected(true);
            } else if (neutralLoss.isSameAs(NeutralLoss.NH3)) {
                h2oToggleButton.setSelected(true);
            }
        }

        oneChargeToggleButton.setSelected(false);
        twoChargesToggleButton.setSelected(false);
        moreThanTwoChargesToggleButton.setSelected(false);
        for (int charge : annotationPreferences.getValidatedCharges()) {
            if (charge == 1) {
                oneChargeToggleButton.setSelected(true);
            } else if (charge == 2) {
                twoChargesToggleButton.setSelected(true);
            } else if (charge > 2) {
                moreThanTwoChargesToggleButton.setSelected(true);
            }
        }
    }

    /**
     * Update the PSM selection according to the currently selected peptide.
     *
     * @param row the row index of the selected peptide
     */
    private void updatePsmSelection(int row) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            try {

                // update the sequence coverage map
                String proteinKey = proteinTableMap.get(getProteinKey(proteinTable.getSelectedRow()));

                updateSequenceCoverage(proteinKey);

                while (psmTable.getRowCount() > 0) {
                    ((DefaultTableModel) psmTable.getModel()).removeRow(0);
                }

                spectrumPanel.removeAll();
                spectrumPanel.revalidate();
                spectrumPanel.repaint();

                String peptideKey = peptideTableMap.get(getPeptideKey(row));

                int index = 1;
                psmTableMap = new HashMap<Integer, String>();

                double maxMassError = Double.MIN_VALUE;
                double maxCharge = Double.MIN_VALUE;

                maxPsmMzValue = Double.MIN_VALUE;

                int validatedPsmCounter = 0;

                PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                SpectrumMatch spectrumMatch;
                for (String spectrumKey : currentPeptideMatch.getSpectrumMatches()) {
                    spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();
                    if (peptideAssumption.getPeptide().isSameAs(currentPeptideMatch.getTheoreticPeptide())) {
                        String modifications = "";
                        for (ModificationMatch mod : peptideAssumption.getPeptide().getModificationMatches()) {
                            if (mod.isVariable()) {
                                modifications += mod.getTheoreticPtm().getName() + ", ";
                            }
                        }

                        if (modifications.length() > 0) {
                            modifications = modifications.substring(0, modifications.length() - 2);
                        } else {
                            modifications = null;
                        }

                        MSnSpectrum tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                        if (tempSpectrum != null) {
                            PSParameter probabilities = new PSParameter();
                            peptideShakerGUI.getIdentification().getMatchParameter(spectrumKey, probabilities);

                            ((DefaultTableModel) psmTable.getModel()).addRow(new Object[]{
                                        index,
                                        peptideAssumption.getPeptide().getSequence(),
                                        modifications,
                                        tempSpectrum.getPrecursor().getCharge().value,
                                        peptideAssumption.getDeltaMass(),
                                        probabilities.isValidated()
                                    });

                            psmTableMap.put(index, spectrumKey);
                            index++;

                            if (probabilities.isValidated()) {
                                validatedPsmCounter++;
                            }

                            if (maxMassError < peptideAssumption.getDeltaMass()) {
                                maxMassError = peptideAssumption.getDeltaMass();
                            }

                            if (maxCharge < tempSpectrum.getPrecursor().getCharge().value) {
                                maxCharge = tempSpectrum.getPrecursor().getCharge().value;
                            }

                            if (tempSpectrum.getPeakList() != null && maxPsmMzValue < tempSpectrum.getMaxMz()) {
                                maxPsmMzValue = tempSpectrum.getMaxMz();
                            }
                        }
                    }
                }

                ((TitledBorder) psmJPanel.getBorder()).setTitle("Peptide-Spectrum Matches (" + validatedPsmCounter + "/" + psmTable.getRowCount() + ")");
                psmJPanel.repaint();

                ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).setMaxValue(maxMassError);
                ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).setMaxValue(maxCharge);

                // select the first spectrum in the table
                if (psmTable.getRowCount() > 0) {
                    psmTable.setRowSelectionInterval(0, 0);
                    psmTableKeyReleased(null);
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Updates the peptide selection according to the currently selected protein.
     *
     * @param row the row index of the protein
     */
    private void updatedPeptideSelection(int row) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            try {

                while (peptideTable.getRowCount() > 0) {
                    ((DefaultTableModel) peptideTable.getModel()).removeRow(0);
                }

                while (psmTable.getRowCount() > 0) {
                    ((DefaultTableModel) psmTable.getModel()).removeRow(0);
                }

                spectrumPanel.removeAll();
                spectrumPanel.revalidate();
                spectrumPanel.repaint();

                String proteinKey = proteinTableMap.get(getProteinKey(row));
                peptideTableMap = new HashMap<Integer, String>();

                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                HashMap<Double, ArrayList<PeptideMatch>> peptideMap = new HashMap<Double, ArrayList<PeptideMatch>>();
                PSParameter probabilities = new PSParameter();
                double peptideProbabilityScore;
                PeptideMatch peptideMatch;
                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, probabilities);
                    peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {
                        peptideMap.put(peptideProbabilityScore, new ArrayList<PeptideMatch>());
                    }
                    peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                    peptideMap.get(peptideProbabilityScore).add(peptideMatch);
                }

                ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
                Collections.sort(scores);

                double maxSpectra = Double.MIN_VALUE;

                int index = 0;
                int validatedPeptideCounter = 0;

                for (double score : scores) {
                    for (PeptideMatch currentMatch : peptideMap.get(score)) {

                        String modifications = "";

                        for (ModificationMatch mod : currentMatch.getTheoreticPeptide().getModificationMatches()) {
                            if (mod.isVariable()) {
                                modifications += mod.getTheoreticPtm().getName() + ", ";
                            }
                        }

                        if (modifications.length() > 0) {
                            modifications = modifications.substring(0, modifications.length() - 2);
                        } else {
                            modifications = null;
                        }

                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(currentMatch.getKey(), probabilities);

                        ArrayList<String> otherProteins = new ArrayList<String>();
                        List<String> proteinProteins = Arrays.asList(ProteinMatch.getAccessions(proteinKey));
                        for (String accession : currentMatch.getTheoreticPeptide().getParentProteins()) {
                            if (!proteinProteins.contains(accession)) {
                                otherProteins.add(accession);
                            }
                        }

                        // find and add the peptide start and end indexes
                        int peptideStart = 0;
                        int peptideEnd = 0;
                        String peptideSequence = currentMatch.getTheoreticPeptide().getSequence();
                        try {
                            String proteinAccession = proteinMatch.getMainMatch();
                            String proteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();
                            peptideStart = proteinSequence.lastIndexOf(peptideSequence) + 1;
                            peptideEnd = peptideStart + peptideSequence.length() - 1;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        int proteinInferenceType = 0;

                        if (otherProteins.size() == 1) {
                            proteinInferenceType = 1;
                        } else if (otherProteins.size() > 1 && otherProteins.size() <= 4) {
                            proteinInferenceType = 2;
                        } else if (otherProteins.size() > 4) {
                            proteinInferenceType = 3;
                        }

                        ((DefaultTableModel) peptideTable.getModel()).addRow(new Object[]{
                                    index + 1,
                                    proteinInferenceType,
                                    peptideSequence,
                                    peptideStart,
                                    peptideEnd,
                                    modifications,
                                    currentMatch.getSpectrumCount(),
                                    probabilities.getPeptideScore(),
                                    probabilities.getPeptideConfidence(),
                                    probabilities.isValidated()
                                });

                        if (probabilities.isValidated()) {
                            validatedPeptideCounter++;
                        }

                        if (maxSpectra < currentMatch.getSpectrumCount()) {
                            maxSpectra = currentMatch.getSpectrumCount();
                        }

                        peptideTableMap.put(index + 1, currentMatch.getKey());
                        index++;

                    }
                }

                ((TitledBorder) peptidesJPanel.getBorder()).setTitle("Peptides (" + validatedPeptideCounter + "/" + peptideTable.getRowCount() + ")");
                peptidesJPanel.repaint();


                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);

                // select the first peptide in the table
                if (peptideTable.getRowCount() > 0) {
                    peptideTable.setRowSelectionInterval(0, 0);
                    peptideTable.scrollRectToVisible(peptideTable.getCellRect(0, 0, false));
                    peptideTableKeyReleased(null);
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Displays the results in the result tables.
     * 
     * @param progressDialogX a progress dialog. Can be null.
     */
    public void displayResults(ProgressDialogX progressDialogX) {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        try {
            int index = 0, maxPeptides = 0, maxSpectra = 0;
            double sequenceCoverage = 0;
            double spectrumCounting = 0, maxSpectrumCounting = 0;
            String description = "";

            // sort the proteins according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
            HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap =
                    new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
            ArrayList<Double> scores = new ArrayList<Double>();
            PSParameter probabilities = new PSParameter();
            ProteinMatch proteinMatch;
            double score;
            int nPeptides, nSpectra;

            for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
                if (!SequenceFactory.isDecoy(proteinKey)) {
                proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, probabilities);
                score = probabilities.getProteinProbabilityScore();
                nPeptides = -proteinMatch.getPeptideMatches().size();
                nSpectra = -peptideShakerGUI.getNSpectra(proteinMatch);

                if (!orderMap.containsKey(score)) {
                    orderMap.put(score, new HashMap<Integer, HashMap<Integer, ArrayList<String>>>());
                    scores.add(score);
                }

                if (!orderMap.get(score).containsKey(nPeptides)) {
                    orderMap.get(score).put(nPeptides, new HashMap<Integer, ArrayList<String>>());
                }

                if (!orderMap.get(score).get(nPeptides).containsKey(nSpectra)) {
                    orderMap.get(score).get(nPeptides).put(nSpectra, new ArrayList<String>());
                }

                orderMap.get(score).get(nPeptides).get(nSpectra).add(proteinKey);
                } else if (progressDialogX != null) {
                    progressDialogX.incrementValue();
                }
            }

            Collections.sort(scores);
            proteinTableMap = new HashMap<Integer, String>();
            // add the proteins to the table
            ArrayList<Integer> nP, nS;
            ArrayList<String> keys;

            int validatedProteinsCounter = 0;

            for (double currentScore : scores) {

                nP = new ArrayList(orderMap.get(currentScore).keySet());
                Collections.sort(nP);

                for (int currentNP : nP) {

                    nS = new ArrayList(orderMap.get(currentScore).get(currentNP).keySet());
                    Collections.sort(nS);

                    for (int currentNS : nS) {

                        keys = orderMap.get(currentScore).get(currentNP).get(currentNS);
                        Collections.sort(keys);

                        for (String proteinKey : keys) {
                            proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                            probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, probabilities);

                            try {
                                Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

                                if (peptideShakerGUI.getSearchParameters().getEnzyme() == null) {
                                    throw new IllegalArgumentException("Unknown enzyme!");
                                }

                                if (currentProtein == null) {
                                    throw new IllegalArgumentException("Protein not found! Accession: " + proteinMatch.getMainMatch());
                                }

                                spectrumCounting = peptideShakerGUI.getSpectrumCounting(proteinMatch);
                                description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                                sequenceCoverage = 100 * peptideShakerGUI.estimateSequenceCoverage(proteinMatch, currentProtein.getSequence());
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                                e.printStackTrace();
                            }
                                ((DefaultTableModel) proteinTable.getModel()).addRow(new Object[]{
                                            index + 1,
                                            probabilities.getGroupClass(),
                                            peptideShakerGUI.addDatabaseLink(proteinMatch.getMainMatch()),
                                            description,
                                            sequenceCoverage,
                                            -currentNP,
                                            -currentNS,
                                            spectrumCounting,
                                            probabilities.getProteinScore(),
                                            probabilities.getProteinConfidence(),
                                            probabilities.isValidated()
                                        });

                                proteinTableMap.put(index + 1, proteinKey);
                                index++;

                                if (probabilities.isValidated()) {
                                    validatedProteinsCounter++;
                                }
                            if (maxSpectrumCounting < spectrumCounting) {
                                maxSpectrumCounting = spectrumCounting;
                            }
                            if (progressDialogX != null) {
                                progressDialogX.incrementValue();
                            }
                        }
                        if (maxSpectra < -currentNS) {
                            maxSpectra = -currentNS;
                        }
                    }
                    if (maxPeptides < -currentNP) {
                        maxPeptides = -currentNP;
                    }
                }
            }

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    // set the preferred size of the accession column
                    int width = peptideShakerGUI.getPreferredColumnWidth(proteinTable, proteinTable.getColumn("Accession").getModelIndex(), 6);
                    proteinTable.getColumn("Accession").setMinWidth(width);
                    proteinTable.getColumn("Accession").setMaxWidth(width);
                }
            });

            ((TitledBorder) proteinsJPanel.getBorder()).setTitle("Proteins (" + validatedProteinsCounter + "/" + proteinTable.getRowCount() + ")");
            proteinsJPanel.repaint();

            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Spectrum Counting").getCellRenderer()).setMaxValue(maxSpectrumCounting);

            try {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
            } catch (IllegalArgumentException e) {
                // ignore error
            }

            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);

            // select the first row
            if (proteinTable.getRowCount() > 0) {
                proteinTable.setRowSelectionInterval(0, 0);
                proteinTableMouseReleased(null);
                proteinTable.requestFocus();
            }

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the sequence coverage map according to the selected protein (and peptide).
     *
     * @param row       the row index of the protein
     * @param column    the column index in the protein table
     */
    private void updateSequenceCoverageMap(int row) {
        try {
            String proteinKey = proteinTableMap.get(getProteinKey(row));
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            updateSequenceCoverage(proteinMatch.getMainMatch());
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Returns the protein table.
     * 
     * @return the protein table
     */
    public JTable getProteinTable() {
        return proteinTable;
    }

    /**
     * Returns the peptide table.
     * 
     * @return the peptide table
     */
    public JTable getPeptideTable() {
        return peptideTable;
    }

    /**
     * Make sure that the currently selected protein in the protein table is 
     * displayed in the other tables.
     * 
     * @param updateProteinSelection if true the protein selection will be updated
     */
    public void updateProteinSelection(boolean updateProteinSelection) {

        if (updateProteinSelection) {
            proteinTableMouseReleased(null);
        }

        int validatedProteinCounter = 0;

        for (int i = 0; i < proteinTable.getRowCount(); i++) {
            if ((Boolean) proteinTable.getValueAt(i, proteinTable.getColumnCount() - 1)) {
                validatedProteinCounter++;
            }
        }

        ((TitledBorder) proteinsJPanel.getBorder()).setTitle("Proteins (" + validatedProteinCounter + "/" + proteinTable.getRowCount() + ")");
        proteinsJPanel.repaint();

        // if required, clear the peptide and psm tables, and the spectrum and protein sequence displays
        if (proteinTable.getRowCount() == 0) {

            while (peptideTable.getRowCount() > 0) {
                ((DefaultTableModel) peptideTable.getModel()).removeRow(0);
            }

            while (psmTable.getRowCount() > 0) {
                ((DefaultTableModel) psmTable.getModel()).removeRow(0);
            }

            ((TitledBorder) peptidesJPanel.getBorder()).setTitle("Peptides");
            peptidesJPanel.repaint();

            ((TitledBorder) psmJPanel.getBorder()).setTitle("Peptide-Spectrum Matches");
            psmJPanel.repaint();

            spectrumPanel.removeAll();
            sequenceFragmentIonPlotsJPanel.removeAll();
            fragmentIonsJScrollPane.removeAll();
            bubbleJPanel.removeAll();

            spectrumJTabbedPane.revalidate();
            spectrumJTabbedPane.repaint();

            coverageTable.setValueAt(null, 0, 0);
            ((TitledBorder) sequenceCoverageJPanel.getBorder()).setTitle("Protein Sequence Coverage");
            sequenceCoverageJPanel.repaint();
        }
    }

    /**
     * Returns an arraylist of the spectrum annotations for all the selected PSMs.
     * 
     * @return an arraylist of the spectrum annotations 
     * @throws MzMLUnmarshallerException 
     */
    private ArrayList<ArrayList<IonMatch>> getAnnotationsForAllSelectedSpectra() throws MzMLUnmarshallerException {

        ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();

        int[] selectedRows = psmTable.getSelectedRows();

        SpectrumAnnotator miniAnnotator = new SpectrumAnnotator();
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        try {
            for (int i = 0; i < selectedRows.length; i++) {

                String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0));
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                if (currentSpectrum != null) {
                    // get the spectrum annotations
                    String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                    Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                    annotationPreferences.setCurrentSettings(currentPeptide, currentSpectrum.getPrecursor().getCharge().value);
                    ArrayList<IonMatch> annotations = miniAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(annotationPreferences.shallAnnotateMostIntensePeaks()),
                            annotationPreferences.getMzTolerance());
                    allAnnotations.add(annotations);
                }
            }
            return allAnnotations;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return null;
        }
    }

    /**
     * Returns an arraylist of all the selected spectra in the PSM table.
     * 
     * @return an arraylist of all the selected spectra
     * @throws MzMLUnmarshallerException 
     */
    private ArrayList<MSnSpectrum> getSelectedSpectra() throws MzMLUnmarshallerException {

        ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();

        int[] selectedRows = psmTable.getSelectedRows();
        MSnSpectrum tempSpectrum;

        for (int i = 0; i < selectedRows.length; i++) {
            String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0));
            tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
            if (tempSpectrum != null) {
                allSpectra.add(tempSpectrum);
            }
        }

        return allSpectra;
    }

    /**
     * Returns the spectrum panel.
     * 
     * @return the spectrum panel
     */
    public Component getSpectrum() {

        if (spectrumJTabbedPane.isEnabledAt(2)) {
            spectrumJTabbedPane.setSelectedIndex(2);
            return (Component) spectrumPanel.getComponent(0);
        }

        return null;
    }

    /**
     * Returns the bubble plot.
     * 
     * @return the bubble plot
     */
    public Component getBubblePlot() {

        if (spectrumJTabbedPane.isEnabledAt(1)) {
            spectrumJTabbedPane.setSelectedIndex(1);
            return ((MassErrorBubblePlot) bubbleJPanel.getComponent(0)).getChartPanel();
        }

        return null;
    }

    /**
     * Returns true of the spectrum tab is enabled.
     * 
     * @return true of the spectrum tab is enabled
     */
    public boolean isSpectrumEnabled() {
        return spectrumJTabbedPane.isEnabledAt(2);
    }

    /**
     * Enable or disable the separators.
     * 
     * @param showSeparators if true the separators are enabled
     */
    public void showSeparators(boolean showSeparators) {

        int dividerSize = 5;

        // @TODO: the coverage splitter sometimes causes problems and has therefore been disabled!!
        //        (the reason has to be the redrawing of the coverage panel...)

        if (showSeparators) {
            //overviewJSplitPane.setDividerSize(dividerSize);
            //coverageJSplitPane.setDividerSize(dividerSize);
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(dividerSize);
            //peptidesPsmJSplitPane.setDividerSize(dividerSize);
        } else {
            //overviewJSplitPane.setDividerSize(0);
            //coverageJSplitPane.setDividerSize(0);
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
            //peptidesPsmJSplitPane.setDividerSize(0);
        }
    }

    /**
     * Update the main match for the given row in the protein table.
     * 
     * @param mainMatch             the accession of the protein match to use
     * @param proteinInferenceType  the protein inference group type
     */
    public void updateMainMatch(String mainMatch, int proteinInferenceType) {
        proteinTable.setValueAt(peptideShakerGUI.addDatabaseLink(mainMatch), proteinTable.getSelectedRow(), proteinTable.getColumn("Accession").getModelIndex());
        proteinTable.setValueAt(proteinInferenceType, proteinTable.getSelectedRow(), proteinTable.getColumn("PI").getModelIndex());
        String description = "unknown protein";
        try {
            description = sequenceFactory.getHeader(mainMatch).getDescription();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        proteinTable.setValueAt(description, proteinTable.getSelectedRow(), proteinTable.getColumn("Description").getModelIndex());
    }

    /**
     * Returns the current sequence coverage.
     * 
     * @return the current sequence coverage
     */
    public int[] getCoverage() {
        return coverage;
    }

    /**
     * Hides or displays the score columns in the protein and peptide tables.
     * 
     * @param hide if true the score columns are hidden.
     */
    public void hideScores(boolean hide) {

        try {
            if (hide) {
                proteinTable.removeColumn(proteinTable.getColumn("Score"));
                peptideTable.removeColumn(peptideTable.getColumn("Score"));
            } else {
                proteinTable.addColumn(proteinScoreColumn);
                proteinTable.moveColumn(10, 8);

                peptideTable.addColumn(peptideScoreColumn);
                peptideTable.moveColumn(9, 7);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
