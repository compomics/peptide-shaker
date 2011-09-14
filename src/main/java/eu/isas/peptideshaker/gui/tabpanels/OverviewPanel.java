package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
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

        intensitySlider.setValue((int) (peptideShakerGUI.getAnnotationPreferences().getAnnotationIntensityLimit() * 100));

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
        peptideTableToolTips.add("Number of Spectra");
        peptideTableToolTips.add("Peptide Score");
        peptideTableToolTips.add("Peptide Confidence");
        peptideTableToolTips.add("Validated");

        psmTableToolTips = new ArrayList<String>();
        psmTableToolTips.add(null);
        psmTableToolTips.add("Peptide Sequence");
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
        ionTableAnnotationMenuPanel = new javax.swing.JPanel();
        bubblePlotTabJPanel = new javax.swing.JPanel();
        bubbleJPanel = new javax.swing.JPanel();
        bubblePlotJToolBar = new javax.swing.JToolBar();
        bubbleAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumSplitPane = new javax.swing.JSplitPane();
        sequenceFragmentIonPlotsJPanel = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();
        intensitySlider = new javax.swing.JSlider();
        accuracySlider = new javax.swing.JSlider();

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
        coverageTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                coverageTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                coverageTableMouseExited(evt);
            }
        });
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
                " ", "PI", "Sequence", "Start", "End", "#Spectra", "Score", "Confidence", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
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
                " ", "Sequence", "Charge", "Mass Error", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
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
        psmTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                psmTableMouseMoved(evt);
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
        spectrumJTabbedPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumJTabbedPaneMouseWheelMoved(evt);
            }
        });
        spectrumJTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spectrumJTabbedPaneStateChanged(evt);
            }
        });

        fragmentIonJPanel.setBackground(new java.awt.Color(255, 255, 255));

        fragmentIonsJScrollPane.setOpaque(false);

        ionTableJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        ionTableJToolBar.setBorder(null);
        ionTableJToolBar.setFloatable(false);
        ionTableJToolBar.setRollover(true);
        ionTableJToolBar.setBorderPainted(false);

        ionTableAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(ionTableAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        ionTableJToolBar.add(ionTableAnnotationMenuPanel);

        javax.swing.GroupLayout fragmentIonJPanelLayout = new javax.swing.GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                    .addComponent(ionTableJToolBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE))
                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
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

        bubbleAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(bubbleAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        bubblePlotJToolBar.add(bubbleAnnotationMenuPanel);

        javax.swing.GroupLayout bubblePlotTabJPanelLayout = new javax.swing.GroupLayout(bubblePlotTabJPanel);
        bubblePlotTabJPanel.setLayout(bubblePlotTabJPanelLayout);
        bubblePlotTabJPanelLayout.setHorizontalGroup(
            bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE))
        );
        bubblePlotTabJPanelLayout.setVerticalGroup(
            bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                .addContainerGap(273, Short.MAX_VALUE)
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                    .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                    .addGap(25, 25, 25)))
        );

        spectrumJTabbedPane.addTab("Bubble Plot", bubblePlotTabJPanel);

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

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
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumJTabbedPane.setSelectedIndex(2);

        intensitySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        intensitySlider.setPaintTicks(true);
        intensitySlider.setToolTipText("Annotation Level");
        intensitySlider.setValue(25);
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

        javax.swing.GroupLayout spectrumMainPanelLayout = new javax.swing.GroupLayout(spectrumMainPanel);
        spectrumMainPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumMainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(accuracySlider, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        spectrumMainPanelLayout.setVerticalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumMainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spectrumJTabbedPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                    .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                        .addGap(29, 29, 29)
                        .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                        .addGap(59, 59, 59)))
                .addContainerGap())
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
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link. Or shows a tooltip with modification details is over 
     * the sequence column.
     *
     * @param evt
     */
    private void peptideTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseMoved
        int row = peptideTable.rowAtPoint(evt.getPoint());
        int column = peptideTable.columnAtPoint(evt.getPoint());

        if (peptideTable.getValueAt(row, column) != null) {
            if (column == peptideTable.getColumn("PI").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                peptideTable.setToolTipText(null);
            } else if (column == peptideTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                String sequence = (String) peptideTable.getValueAt(row, column);

                if (sequence.indexOf("<span") != -1) {
                    try {
                        String peptideKey = peptideTableMap.get(getPeptideKey(row));
                        Peptide peptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                        String tooltip = peptideShakerGUI.getPeptideModificationTooltipAsHtml(peptide);
                        peptideTable.setToolTipText(tooltip);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    peptideTable.setToolTipText(null);
                }
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideTable.setToolTipText(null);
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            peptideTable.setToolTipText(null);
        }
    }//GEN-LAST:event_peptideTableMouseMoved

    /**
     * Try to get the protein sequence index from the protein sequence model.
     * 
     * @param evt 
     */
    private void coverageTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseMoved

        if (currentProteinSequence != null) {

            int residueNumber = convertPointToResidueNumber(evt.getPoint().getX());

            String tooltipText = "<html>";

            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                if (residueNumber >= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex())
                        && residueNumber <= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex())
                        && (Boolean) peptideTable.getValueAt(i, peptideTable.getColumnCount() - 1)) {

                    String peptideKey = peptideTableMap.get(getPeptideKey(i));
                    String modifiedSequence = "";

                    try {
                        modifiedSequence = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    tooltipText += peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex()) + " - "
                            + modifiedSequence
                            + " - " + peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex()) + "<br>";
                }
            }

            if (!tooltipText.equalsIgnoreCase("<html>")) {
                coverageTable.setToolTipText(tooltipText);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                coverageTable.setToolTipText(null);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_coverageTableMouseMoved

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
private void spectrumJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spectrumJTabbedPaneStateChanged

    if (peptideShakerGUI.getAnnotationMenuBar() != null) {

        int index = spectrumJTabbedPane.getSelectedIndex();

        if (index == 0) {
            ionTableAnnotationMenuPanel.removeAll();
            ionTableAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
            peptideShakerGUI.updateAnnotationMenuBarVisableOptions(false, false, true);
        } else if (index == 1) {
            bubbleAnnotationMenuPanel.removeAll();
            bubbleAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
            peptideShakerGUI.updateAnnotationMenuBarVisableOptions(false, true, false);
        } else if (index == 2) {
            spectrumAnnotationMenuPanel.removeAll();
            spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
            peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false);
        }
    }
}//GEN-LAST:event_spectrumJTabbedPaneStateChanged

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
private void spectrumJTabbedPaneMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumJTabbedPaneMouseWheelMoved

    // @TODO: figure out why the strange special cases are needed... if not included the slider gets stuck at the given values
    
    if (evt.isAltDown()) {
        if (evt.getWheelRotation() > 0) { // Down
            intensitySlider.setValue(intensitySlider.getValue() - 1);
        } else { // Up
            if (intensitySlider.getValue() == 28) {
                intensitySlider.setValue(intensitySlider.getValue() + 2);
            } else if (intensitySlider.getValue() == 56) {
                intensitySlider.setValue(intensitySlider.getValue() + 3);
            } else {
                intensitySlider.setValue(intensitySlider.getValue() + 1);
            }
        }
    } else {
        if (evt.getWheelRotation() > 0) { // Down
            accuracySlider.setValue(accuracySlider.getValue() - 1);
        } else { // Up
            if (accuracySlider.getValue() == 28) {
                accuracySlider.setValue(accuracySlider.getValue() + 2);
            } else if (accuracySlider.getValue() == 56) {
                accuracySlider.setValue(accuracySlider.getValue() + 3);
            } else {
                accuracySlider.setValue(accuracySlider.getValue() + 1);
            }
        }
    }
}//GEN-LAST:event_spectrumJTabbedPaneMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     * 
     * @param evt 
     */
private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
    peptideShakerGUI.getAnnotationPreferences().setAnnotationLevel(intensitySlider.getValue() / 100.0);
    peptideShakerGUI.updateSpectrumAnnotations();
    peptideShakerGUI.setDataSaved(false);
}//GEN-LAST:event_intensitySliderStateChanged

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
private void intensitySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_intensitySliderMouseWheelMoved
    spectrumJTabbedPaneMouseWheelMoved(evt);
}//GEN-LAST:event_intensitySliderMouseWheelMoved

    /**
     * Opens the selected peptide in the coverage table.
     * 
     * @param evt 
     */
private void coverageTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseClicked

    if (currentProteinSequence != null) {

        int residueNumber = convertPointToResidueNumber(evt.getPoint().getX());

        ArrayList<Integer> peptideIndexes = new ArrayList<Integer>();

        for (int i = 0; i < peptideTable.getRowCount(); i++) {
            if (residueNumber >= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex())
                    && residueNumber <= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex())
                    && (Boolean) peptideTable.getValueAt(i, peptideTable.getColumnCount() - 1)) {
                peptideIndexes.add(i);
            }
        }

        if (!peptideIndexes.isEmpty()) {

            if (peptideIndexes.size() == 1) {
                peptideTable.setRowSelectionInterval(peptideIndexes.get(0), peptideIndexes.get(0));
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(peptideIndexes.get(0), peptideIndexes.get(0), false));
                peptideTableKeyReleased(null);
            } else {
                JPopupMenu peptidesPopupMenu = new JPopupMenu();

                // needs to be made final to be used below
                final ArrayList<Integer> tempPeptideIndexes = peptideIndexes;

                for (int i = 0; i < tempPeptideIndexes.size(); i++) {

                    String peptideKey = peptideTableMap.get(getPeptideKey(tempPeptideIndexes.get(i)));
                    String modifiedSequence = "";

                    try {
                        modifiedSequence = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String text = "<html>" + (i + 1) + ": " + peptideTable.getValueAt(tempPeptideIndexes.get(i), peptideTable.getColumn("Start").getModelIndex()) + " - "
                            + modifiedSequence
                            + " - " + peptideTable.getValueAt(tempPeptideIndexes.get(i), peptideTable.getColumn("End").getModelIndex())
                            + "</html>";

                    final int tempInt = i;

                    JMenuItem menuItem = new JMenuItem(text);
                    menuItem.addActionListener(new java.awt.event.ActionListener() {

                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            peptideTable.setRowSelectionInterval(tempPeptideIndexes.get(tempInt), tempPeptideIndexes.get(tempInt));
                            peptideTable.scrollRectToVisible(peptideTable.getCellRect(tempPeptideIndexes.get(tempInt), tempPeptideIndexes.get(tempInt), false));
                            peptideTableKeyReleased(null);
                        }
                    });

                    peptidesPopupMenu.add(menuItem);
                }

                peptidesPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }
}//GEN-LAST:event_coverageTableMouseClicked

    /**
     * Switch the mouse cursor back to the default cursor.
     * 
     * @param evt 
     */
private void coverageTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseExited
    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_coverageTableMouseExited

    /**
     * See if we ought to show a tooltip with modification details for the 
     * sequence column.
     * 
     * @param evt 
     */
    private void psmTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseMoved

        int row = psmTable.rowAtPoint(evt.getPoint());
        int column = psmTable.columnAtPoint(evt.getPoint());

        if (psmTable.getValueAt(row, column) != null) {
            if (column == psmTable.getColumn("Sequence").getModelIndex()) {

                // check if we ought to show a tooltip with mod details
                String sequence = (String) psmTable.getValueAt(row, column);

                if (sequence.indexOf("<span") != -1) {
                    try {
                        String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                        PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);

                        String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(row, 0));
                        SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();

                        if (peptideAssumption.getPeptide().isSameAs(currentPeptideMatch.getTheoreticPeptide())) {
                            Peptide peptide = peptideAssumption.getPeptide();
                            String tooltip = peptideShakerGUI.getPeptideModificationTooltipAsHtml(peptide);
                            psmTable.setToolTipText(tooltip);
                        } else {
                            // @TODO: do we have to do anything here??
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    psmTable.setToolTipText(null);
                }
            } else {
                psmTable.setToolTipText(null);
            }
        } else {
            psmTable.setToolTipText(null);
        }
    }//GEN-LAST:event_psmTableMouseMoved

    /**
     * Update the fragment ion annotation accuracy.
     * 
     * @param evt 
     */
    private void accuracySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_accuracySliderStateChanged
        peptideShakerGUI.getAnnotationPreferences().setFragmentIonAccuracy((accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
    }//GEN-LAST:event_accuracySliderStateChanged

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
    private void accuracySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracySliderMouseWheelMoved
        spectrumJTabbedPaneMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JPanel bubbleAnnotationMenuPanel;
    private javax.swing.JPanel bubbleJPanel;
    private javax.swing.JToolBar bubblePlotJToolBar;
    private javax.swing.JPanel bubblePlotTabJPanel;
    private javax.swing.JSplitPane coverageJSplitPane;
    private javax.swing.JTable coverageTable;
    private javax.swing.JScrollPane coverageTableScrollPane;
    private javax.swing.JPanel fragmentIonJPanel;
    private javax.swing.JScrollPane fragmentIonsJScrollPane;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel ionTableAnnotationMenuPanel;
    private javax.swing.JToolBar ionTableJToolBar;
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
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JTabbedPane spectrumJTabbedPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JPanel spectrumMainPanel;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JSplitPane spectrumSplitPane;
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
                            annotationPreferences.setCurrentSettings(
                                    selectedPeptideMatch.getTheoreticPeptide(),
                                    currentSpectrum.getPrecursor().getCharge().value, !currentSpectrumKey.equalsIgnoreCase(spectrumKey));
                            ArrayList<IonMatch> annotations = miniAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                    annotationPreferences.getNeutralLosses(),
                                    annotationPreferences.getValidatedCharges(),
                                    currentSpectrum,
                                    selectedPeptideMatch.getTheoreticPeptide(),
                                    currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                    annotationPreferences.getFragmentIonAccuracy());
                            allAnnotations.add(annotations);
                            allSpectra.add(currentSpectrum);

                            currentSpectrumKey = spectrumKey;
                        }
                    }
                }

                // @TODO: rewrite the charge selection below when the new ion selection gui has been implemented!

                double bubbleScale = annotationPreferences.getFragmentIonAccuracy() * 10 * peptideShakerGUI.getBubbleScale();

                if (peptideShakerGUI.useRelativeError()) {
                    bubbleScale = annotationPreferences.getFragmentIonAccuracy() * 10000 * peptideShakerGUI.getBubbleScale();
                }

                MassErrorBubblePlot massErrorBubblePlot = new MassErrorBubblePlot(
                        selectedIndexes,
                        allAnnotations, annotationPreferences.getIonTypes(), allSpectra, annotationPreferences.getFragmentIonAccuracy(),
                        bubbleScale,
                        annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                        annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4)),
                        selectedIndexes.size() == 1, annotationPreferences.showBars(),
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
                            spectrum.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getFragmentIonAccuracy());
                            spectrum.setBorder(null);

                            // get the spectrum annotations
                            String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                            Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                            SpectrumAnnotator spectrumAnnotator = peptideShakerGUI.getSpectrumAnnorator();
                            AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
                            annotationPreferences.setCurrentSettings(
                                    currentPeptide, currentSpectrum.getPrecursor().getCharge().value,
                                    !currentSpectrumKey.equalsIgnoreCase(spectrumKey));
                            ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                    annotationPreferences.getNeutralLosses(),
                                    annotationPreferences.getValidatedCharges(),
                                    currentSpectrum, currentPeptide,
                                    currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                    annotationPreferences.getFragmentIonAccuracy());
                            spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                            spectrum.rescale(lowerMzZoomRange, upperMzZoomRange);

                            if (!currentSpectrumKey.equalsIgnoreCase(spectrumKey)) {
                                if (annotationPreferences.useAutomaticAnnotation()) {
                                    annotationPreferences.setNeutralLossesSequenceDependant(true);
                                }
                            }

                            peptideShakerGUI.updateAnnotationMenus();


                            currentSpectrumKey = spectrumKey;

                            // show all or just the annotated peaks
                            spectrum.showAnnotatedPeaksOnly(!peptideShakerGUI.getAnnotationPreferences().showAllPeaks());

                            spectrum.setYAxisZoomExcludesBackgroundPeaks(peptideShakerGUI.getAnnotationPreferences().yAxisZoomExcludesBackgroundPeaks());

                            // add the spectrum panel to the frame
                            spectrumPanel.removeAll();
                            spectrumPanel.add(spectrum);
                            spectrumPanel.revalidate();
                            spectrumPanel.repaint();

                            // create and display the fragment ion table
                            if (psmTable.getSelectedRowCount() == 1 && !peptideShakerGUI.getAnnotationPreferences().useIntensityIonTable()) {
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, annotations, annotationPreferences.getIonTypes(),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(1)),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(2))));
                            } else {
                                ArrayList<ArrayList<IonMatch>> allAnnotations = getAnnotationsForAllSelectedSpectra();
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, allAnnotations, getSelectedSpectra(), annotationPreferences.getIonTypes(),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(1)),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(2))));
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
                                    peptideShakerGUI.getAnnotationPreferences().getAnnotationIntensityLimit(),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4))));

                            // @TODO: rewrite the charge selection above and below when the new ion selection gui has been implemented!

                            // create the miniature mass error plot
                            MassErrorPlot massErrorPlot = new MassErrorPlot(
                                    annotations, annotationPreferences.getIonTypes(), currentSpectrum,
                                    annotationPreferences.getFragmentIonAccuracy(),
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
                                peptideShakerGUI.getAnnotationPreferences().setIntensityIonTable(true);
                            } else {
                                spectrumJTabbedPane.setToolTipTextAt(2, null);
                            }

                            // update the panel border title
                            if (psmTable.getSelectedRowCount() == 1) {
                                updateSpectrumPanelBorderTitle(currentPeptide, currentSpectrum);
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

                        MSnSpectrum tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                        if (tempSpectrum != null) {
                            PSParameter probabilities = new PSParameter();
                            probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(spectrumKey, probabilities);

                            ((DefaultTableModel) psmTable.getModel()).addRow(new Object[]{
                                        index,
                                        peptideAssumption.getPeptide().getModifiedSequenceAsHtml(
                                        peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
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
                HashMap<Double, HashMap<Integer, HashMap<String, PeptideMatch>>> peptideMap = new HashMap<Double, HashMap<Integer, HashMap<String, PeptideMatch>>>();
                PSParameter probabilities = new PSParameter();
                double peptideProbabilityScore;
                PeptideMatch peptideMatch;
                int spectrumCount;
                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, probabilities);
                    peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {
                        peptideMap.put(peptideProbabilityScore, new HashMap<Integer, HashMap<String, PeptideMatch>>());
                    }
                    peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                    spectrumCount = peptideMatch.getSpectrumCount();
                    if (!peptideMap.get(peptideProbabilityScore).containsKey(spectrumCount)) {
                        peptideMap.get(peptideProbabilityScore).put(spectrumCount, new HashMap<String, PeptideMatch>());
                    }
                    peptideMap.get(peptideProbabilityScore).get(spectrumCount).put(peptideKey, peptideMatch);
                }

                ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
                Collections.sort(scores);
                ArrayList<Integer> nSpectra;
                ArrayList<String> keys;
                PeptideMatch currentMatch;

                double maxSpectra = 0;

                int index = 0;
                int validatedPeptideCounter = 0;

                for (double score : scores) {
                    nSpectra = new ArrayList<Integer>(peptideMap.get(score).keySet());
                    Collections.sort(nSpectra);

                    for (int i = nSpectra.size() - 1; i >= 0; i--) {
                        spectrumCount = nSpectra.get(i);
                        keys = new ArrayList<String>(peptideMap.get(score).get(spectrumCount).keySet());
                        Collections.sort(keys);
                        for (String key : keys) {
                            currentMatch = peptideMap.get(score).get(spectrumCount).get(key);
                            probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(key, probabilities);

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
                                        currentMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                                        peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                        peptideStart,
                                        peptideEnd,
                                        spectrumCount,
                                        probabilities.getPeptideScore(),
                                        probabilities.getPeptideConfidence(),
                                        probabilities.isValidated()
                                    });

                            if (probabilities.isValidated()) {
                                validatedPeptideCounter++;
                            }

                            if (maxSpectra < spectrumCount) {
                                maxSpectra = spectrumCount;
                            }

                            peptideTableMap.put(index + 1, currentMatch.getKey());
                            index++;
                        }
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
                    annotationPreferences.setCurrentSettings(currentPeptide,
                            currentSpectrum.getPrecursor().getCharge().value, !currentSpectrumKey.equalsIgnoreCase(spectrumKey));
                    ArrayList<IonMatch> annotations = miniAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getFragmentIonAccuracy());
                    allAnnotations.add(annotations);
                    currentSpectrumKey = spectrumKey;
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
     * Returns the current spectrum as an mgf string.
     * 
     * @return the current spectrum as an mgf string
     */
    public String getSpectrumAsMgf() {

        int[] selectedRows = psmTable.getSelectedRows();

        if (selectedRows.length > 0) {

            String spectraAsMgf = "";

            for (int i = 0; i < selectedRows.length; i++) {
                String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0));
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                spectraAsMgf += currentSpectrum.asMgf();
            }

            return spectraAsMgf;
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
                peptideTable.moveColumn(8, 6);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the current selected tab in the spectrum and fragment ions 
     * tabbed pane.
     * 
     * @return the current selected tab in the spectrum and fragment ions tabbed pane
     */
    public int getSelectedSpectrumTabIndex() {
        return spectrumJTabbedPane.getSelectedIndex();
    }

    /**
     * Makes sure that the annotation menu bar is shown in the currently visible 
     * spectrum and fragment ions tabbed pane.
     */
    public void showSpectrumAnnotationMenu() {
        spectrumJTabbedPaneStateChanged(null);
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
     * Converts the x-axis coordinate in the coverage table into a residue 
     * index in the sequence.
     * 
     * @param coverageTableX    the x-axis coordinate in the coverage table
     * @return                  the residue index
     */
    private int convertPointToResidueNumber(double coverageTableX) {

        // @TODO: does not work perfectly...

        double paddingInPercent = 0.0115; // @TODO: this hardcoded value should not be required!
        double cellWidth = coverageTable.getWidth();

        double width = (coverageTableX - cellWidth * paddingInPercent) / (cellWidth - 2 * cellWidth * paddingInPercent);

        return (int) (width * currentProteinSequence.length());
    }

    /**
     * Update the PTM color coding.
     */
    public void updatePtmColors() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        HashMap<String, Color> ptmColors = peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors();

        try {

            // update the peptide table
            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                String peptideKey = peptideTableMap.get(getPeptideKey(i));
                String modifiedSequence = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(ptmColors, true);
                peptideTable.setValueAt(modifiedSequence, i, peptideTable.getColumn("Sequence").getModelIndex());
            }

            // update the psm table
            if (peptideTable.getSelectedRow() != -1) {

                String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);

                for (int i = 0; i < psmTable.getRowCount(); i++) {

                    String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(i, 0));
                    PeptideAssumption peptideAssumption = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey).getBestAssumption();

                    if (peptideAssumption.getPeptide().isSameAs(currentPeptideMatch.getTheoreticPeptide())) {
                        String modifiedSequence = peptideAssumption.getPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                        psmTable.setValueAt(modifiedSequence, i, psmTable.getColumn("Sequence").getModelIndex());
                    } else {
                        // @TODO: do we need to do something here??
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the spectrum and fragment ions panel border title with information 
     * about the currently selected psm.
     * 
     * @param currentPeptide
     * @param currentSpectrum 
     */
    private void updateSpectrumPanelBorderTitle(Peptide currentPeptide, MSnSpectrum currentSpectrum) {

        int selectedRow = peptideTable.getSelectedRow();
        int start = (Integer) peptideTable.getValueAt(selectedRow, peptideTable.getColumn("Start").getModelIndex()) - 1;
        int end = (Integer) peptideTable.getValueAt(selectedRow, peptideTable.getColumn("End").getModelIndex());

        String before = "";
        String after = "";

        // @TODO: make the number of residues shown before/after up to the user?

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

        String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(psmTable.getSelectedRow(), 0));
        String modifiedSequence = "";

        try {
            PeptideAssumption peptideAssumption = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey).getBestAssumption();

            if (peptideAssumption.getPeptide().isSameAs(currentPeptide)) {
                modifiedSequence = peptideAssumption.getPeptide().getModifiedSequenceAsString(false);
            } else {
                // @TODO: do we need to do something here??
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                "Spectrum & Fragment Ions (" + before + modifiedSequence + after
                + "   " + currentSpectrum.getPrecursor().getCharge().toString() + "   "
                + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 4) + " m/z)");
    }
}
