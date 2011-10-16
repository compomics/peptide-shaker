package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.ExportGraphicsDialog;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoySeries;
import eu.isas.peptideshaker.gui.HelpWindow;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSMaps;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockFrame;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.RectangleEdge;

/**
 * This panel displays statistical information about the dataset.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class StatsPanel extends javax.swing.JPanel {

    /**
     * If true the data has been (re-)loaded with the current thresold setting.
     */
    private boolean dataValidated = true;
    /**
     * If true the data has been (re-loaded) with the current PEP window size.
     */
    private boolean pepWindowApplied = true;
    /**
     * The main peptide shaker gui.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The default line width for the line plots.
     */
    private final int LINE_WIDTH = 2;
    /**
     * The currently displayed Target Decoy map.
     */
    private TargetDecoyMap currentTargetDecoyMap;
    /**
     * The Target Decoy metrics series of the currently displayed map.
     */
    private TargetDecoySeries targetDecoySeries;
    /**
     * The psms map: # in the list -> map key.
     */
    private HashMap<Integer, Integer> psmMap = new HashMap<Integer, Integer>();
    /**
     * The peptide map: # in the list -> map key.
     */
    private HashMap<Integer, String> peptideMap = new HashMap<Integer, String>();
    /**
     * The confidence plot.
     */
    private XYPlot confidencePlot = new XYPlot();
    /**
     * The fdr fnr plot.
     */
    private XYPlot fdrFnrPlot = new XYPlot();
    /**
     * The PEP plot.
     */
    private XYPlot pepPlot = new XYPlot();
    /**
     * The FDR/FNR plot.
     */
    private XYPlot fdrPlot = new XYPlot();
    /**
     * The Benefit/cost plot.
     */
    private XYPlot benefitCostPlot = new XYPlot();
    /**
     * The last threshold input.
     */
    private double lastThreshold = 1;
    /**
     * The last threshold type
     * 0 -> confidence
     * 1 -> FDR
     * 2 -> FNR
     */
    private int lastThresholdType = 1;
    /**
     * The confidence threshold marker.
     */
    private ValueMarker confidenceMarker = new ValueMarker(1);
    /**
     * Map keeping track of probabilities modifications.
     */
    private HashMap<Integer, Boolean> modifiedMaps = new HashMap<Integer, Boolean>();
    /**
     * The score log axis.
     */
    private LogAxis scoreAxis;
    /**
     * The classical FDR axis in the FDRs plot
     */
    private NumberAxis classicalAxis;
    /**
     * The probabilistic FDR axis in the FDRs plot
     */
    private NumberAxis probaAxis;
    /**
     * The highlighting to use for FNR.
     */
    private Color fnrHighlightColor = new Color(0, 255, 0, 15);
    /**
     * The highlighting to use for FDR.
     */
    private Color fdrHighlightColor = new Color(255, 0, 0, 15);

    /**
     * Create a new StatsPanel.
     *
     * @param parent the PeptideShaker parent frame.
     */
    public StatsPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent;

        initComponents();

        // add the default values to the group selection
        ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{1, "Proteins"});
        ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{2, "Peptides"});
        ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{3, "PSMs"});

        groupSelectionScrollPaneScrollPane.getViewport().setOpaque(false);

        // the index column
        groupSelectionTable.getColumn(" ").setMaxWidth(30);
        groupSelectionTable.getColumn(" ").setMinWidth(30);

        // set table properties
        groupSelectionTable.getTableHeader().setReorderingAllowed(false);

        // for some reason background highlighting with alpha values does not work on the backup look and feel...
        if (UIManager.getLookAndFeel().getName().equalsIgnoreCase("Nimbus")) {
            fdrTxt.setBackground(fdrHighlightColor);
            fnrTxt.setBackground(fnrHighlightColor);
        } else {
            fdrTxt.setBackground(confidenceTxt.getBackground());
            fnrTxt.setBackground(confidenceTxt.getBackground());
        }

        // Initialize confidence plot
        scoreAxis = new LogAxis("Probabilistic Score");
        NumberAxis confidenceAxis = new NumberAxis("Confidence [%]");
        confidenceAxis.setAutoRangeIncludesZero(true);
        confidencePlot.setDomainAxis(scoreAxis);
        confidencePlot.setRangeAxis(0, confidenceAxis);
        confidencePlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        confidencePlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        confidenceMarker.setPaint(Color.red);
        confidenceMarker.setStroke(new BasicStroke(LINE_WIDTH));
        confidencePlot.addDomainMarker(confidenceMarker);

        // Initialize PEP plot
        NumberAxis pepAxis = new NumberAxis("PEP [%]");
        pepAxis.setAutoRangeIncludesZero(true);
        pepPlot.setDomainAxis(scoreAxis);
        pepPlot.setRangeAxis(0, pepAxis);
        pepPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        pepPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        confidenceMarker.setPaint(Color.red);
        confidenceMarker.setStroke(new BasicStroke(LINE_WIDTH));
        pepPlot.addDomainMarker(confidenceMarker);

        // Initialize FDRs plot
        classicalAxis = new NumberAxis("Classical FDR [%]");
        probaAxis = new NumberAxis("Probabilistic FDR [%]");
        classicalAxis.setAutoRangeIncludesZero(true);
        probaAxis.setAutoRangeIncludesZero(true);
        fdrPlot.setDomainAxis(classicalAxis);
        fdrPlot.setRangeAxis(0, probaAxis);
        fdrPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        fdrPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

        // Initialize FDR/FNR plot
        NumberAxis fdrAxis = new NumberAxis("FDR - FNR [%]");
        fdrAxis.setAutoRangeIncludesZero(true);
        fdrFnrPlot.setDomainAxis(scoreAxis);
        fdrFnrPlot.setRangeAxis(0, fdrAxis);
        fdrFnrPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        fdrFnrPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

        // Initialize benefit/cost plot
        NumberAxis benefitAxis = new NumberAxis("Benefit (1-FNR) [%]");
        NumberAxis costAxis = new NumberAxis("Cost (FDR) [%]");
        benefitAxis.setAutoRangeIncludesZero(true);
        costAxis.setAutoRangeIncludesZero(true);
        benefitCostPlot.setDomainAxis(costAxis);
        benefitCostPlot.setRangeAxis(0, benefitAxis);
        benefitCostPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        benefitCostPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

        fdrCombo1.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        thresholdTypeCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // make the tabs in the spectrum tabbed pane go from right to left
        optimizationTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        formComponentResized(null);
    }

    /**
     * Update the plot sizes.
     */
    public void updatePlotSizes() {
        formComponentResized(null);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        groupListJPanel = new javax.swing.JPanel();
        groupSelectionLayeredPane = new javax.swing.JLayeredPane();
        groupSelectionHelpJButton = new javax.swing.JButton();
        groupSelectionScrollPaneScrollPane = new javax.swing.JScrollPane();
        groupSelectionTable = new javax.swing.JTable();
        idSummaryJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        nTotalTxt = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        nValidatedTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        nTPlTxt = new javax.swing.JTextField();
        nFPTxt = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        fdrTxt = new javax.swing.JTextField();
        fnrTxt = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        nMaxTxt = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        confidenceTxt = new javax.swing.JTextField();
        totalTPHelpJButton = new javax.swing.JButton();
        validatedHitsHelpJButton = new javax.swing.JButton();
        falsePositivesHelpJButton = new javax.swing.JButton();
        truePositivesHelpJButton = new javax.swing.JButton();
        nMaxHelpJButton = new javax.swing.JButton();
        confidenceHelpJButton = new javax.swing.JButton();
        fdrHelpJButton = new javax.swing.JButton();
        fnrHelpJButton = new javax.swing.JButton();
        optimizationJPanel = new javax.swing.JPanel();
        optimizationTabbedPane = new javax.swing.JTabbedPane();
        estimatorOptimizationTab = new javax.swing.JPanel();
        estimatorsPlotSplitPane = new javax.swing.JSplitPane();
        pepPanel = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        sensitivitySlider1 = new javax.swing.JSlider();
        pepPlotLayeredPane = new javax.swing.JLayeredPane();
        pepChartPanel = new javax.swing.JPanel();
        pepPlotHelpJButton = new javax.swing.JButton();
        pepPlotExportJButton = new javax.swing.JButton();
        fdrsPanel = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        sensitivitySlider2 = new javax.swing.JSlider();
        jLabel35 = new javax.swing.JLabel();
        fdrsPlotLayeredPane = new javax.swing.JLayeredPane();
        fdrsChartPanel = new javax.swing.JPanel();
        fdrsPlotHelpJButton = new javax.swing.JButton();
        fdrPlotExportJButton = new javax.swing.JButton();
        thresholdOptimizationTab = new javax.swing.JPanel();
        leftPlotSplitPane = new javax.swing.JSplitPane();
        confidencePanel = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        confidenceSlider = new javax.swing.JSlider();
        jLabel26 = new javax.swing.JLabel();
        confidencePlotLayeredPane = new javax.swing.JLayeredPane();
        confidenceChartPanel = new javax.swing.JPanel();
        confidencePlotHelpJButton = new javax.swing.JButton();
        confidencePlotExportJButton = new javax.swing.JButton();
        rightPlotSplitPane = new javax.swing.JSplitPane();
        fdrFnrPanel = new javax.swing.JPanel();
        jLabel28 = new javax.swing.JLabel();
        fdrSlider1 = new javax.swing.JSlider();
        jLabel29 = new javax.swing.JLabel();
        fdrPlotLayeredPane = new javax.swing.JLayeredPane();
        fdrFnrChartPanel = new javax.swing.JPanel();
        fdrFnrPlotHelpJButton = new javax.swing.JButton();
        fdrFnrPlotExportJButton = new javax.swing.JButton();
        benefitCostPanel = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        fdrSlider2 = new javax.swing.JSlider();
        jLabel33 = new javax.swing.JLabel();
        benefitPlotLayeredPane = new javax.swing.JLayeredPane();
        benefitCostChartPanel = new javax.swing.JPanel();
        benefitPlotHelpJButton = new javax.swing.JButton();
        benefitPlotExportJButton = new javax.swing.JButton();
        parametersJPanel = new javax.swing.JPanel();
        thresholdInput = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        validateButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        windowTxt = new javax.swing.JTextField();
        applyButton = new javax.swing.JButton();
        fdrCombo1 = new javax.swing.JComboBox();
        thresholdTypeCmb = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        thresholdHelpJButton = new javax.swing.JButton();
        pepHelpJButton = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        groupListJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Group Selection"));
        groupListJPanel.setOpaque(false);

        groupSelectionLayeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                groupSelectionLayeredPaneComponentResized(evt);
            }
        });

        groupSelectionHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        groupSelectionHelpJButton.setToolTipText("Help");
        groupSelectionHelpJButton.setBorder(null);
        groupSelectionHelpJButton.setBorderPainted(false);
        groupSelectionHelpJButton.setContentAreaFilled(false);
        groupSelectionHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        groupSelectionHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                groupSelectionHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                groupSelectionHelpJButtonMouseExited(evt);
            }
        });
        groupSelectionHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupSelectionHelpJButtonActionPerformed(evt);
            }
        });
        groupSelectionHelpJButton.setBounds(170, 130, 27, 25);
        groupSelectionLayeredPane.add(groupSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        groupSelectionScrollPaneScrollPane.setOpaque(false);

        groupSelectionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Type"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        groupSelectionTable.setOpaque(false);
        groupSelectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                groupSelectionTableMouseReleased(evt);
            }
        });
        groupSelectionTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                groupSelectionTableKeyReleased(evt);
            }
        });
        groupSelectionScrollPaneScrollPane.setViewportView(groupSelectionTable);

        groupSelectionScrollPaneScrollPane.setBounds(0, 0, 200, 170);
        groupSelectionLayeredPane.add(groupSelectionScrollPaneScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout groupListJPanelLayout = new javax.swing.GroupLayout(groupListJPanel);
        groupListJPanel.setLayout(groupListJPanelLayout);
        groupListJPanelLayout.setHorizontalGroup(
            groupListJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupListJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupSelectionLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addContainerGap())
        );
        groupListJPanelLayout.setVerticalGroup(
            groupListJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, groupListJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupSelectionLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                .addContainerGap())
        );

        idSummaryJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Summary"));
        idSummaryJPanel.setOpaque(false);

        jLabel2.setText("Total TP:");
        jLabel2.setToolTipText("Total number of true positives");

        nTotalTxt.setBackground(new java.awt.Color(245, 245, 245));
        nTotalTxt.setEditable(false);
        nTotalTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nTotalTxt.setToolTipText("Total number of true positives");

        jLabel1.setText("# Validated Hits:");
        jLabel1.setToolTipText("Number of validated hits");

        nValidatedTxt.setBackground(new java.awt.Color(245, 245, 245));
        nValidatedTxt.setEditable(false);
        nValidatedTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nValidatedTxt.setToolTipText("Number of validated hits");

        jLabel3.setText("# FP:");
        jLabel3.setToolTipText("Number of false positives");

        jLabel10.setText("# TP:");
        jLabel10.setToolTipText("Number of true positives");

        nTPlTxt.setBackground(new java.awt.Color(245, 245, 245));
        nTPlTxt.setEditable(false);
        nTPlTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nTPlTxt.setToolTipText("Number of true positives");

        nFPTxt.setBackground(new java.awt.Color(245, 245, 245));
        nFPTxt.setEditable(false);
        nFPTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nFPTxt.setToolTipText("Number of false positives");

        jLabel13.setText("FDR:");
        jLabel13.setToolTipText("False Discovery Rate");

        jLabel36.setText("FNR:");
        jLabel36.setToolTipText("False Negative Rate");

        fdrTxt.setEditable(false);
        fdrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fdrTxt.setToolTipText("False Discovery Rate");

        fnrTxt.setEditable(false);
        fnrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fnrTxt.setToolTipText("False Negative Rate");

        jLabel6.setFont(jLabel6.getFont().deriveFont((jLabel6.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel6.setText("Dataset Properties");

        jLabel20.setText("Nmax:");
        jLabel20.setToolTipText("Maximum number of target hits between two subsequent decoy hits");

        nMaxTxt.setBackground(new java.awt.Color(245, 245, 245));
        nMaxTxt.setEditable(false);
        nMaxTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nMaxTxt.setToolTipText("Maximum number of target hits between two subsequent decoy hits");

        jLabel7.setFont(jLabel7.getFont().deriveFont((jLabel7.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel7.setText("Validation Results");

        jLabel23.setText("Confidence:");
        jLabel23.setToolTipText("Minimum Confidence");

        confidenceTxt.setBackground(new java.awt.Color(245, 245, 245));
        confidenceTxt.setEditable(false);
        confidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        confidenceTxt.setToolTipText("Minimum Confidence");

        totalTPHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        totalTPHelpJButton.setToolTipText("Help");
        totalTPHelpJButton.setBorder(null);
        totalTPHelpJButton.setBorderPainted(false);
        totalTPHelpJButton.setContentAreaFilled(false);
        totalTPHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        totalTPHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                totalTPHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                totalTPHelpJButtonMouseExited(evt);
            }
        });
        totalTPHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                totalTPHelpJButtonActionPerformed(evt);
            }
        });

        validatedHitsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        validatedHitsHelpJButton.setToolTipText("Help");
        validatedHitsHelpJButton.setBorder(null);
        validatedHitsHelpJButton.setBorderPainted(false);
        validatedHitsHelpJButton.setContentAreaFilled(false);
        validatedHitsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        validatedHitsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                validatedHitsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                validatedHitsHelpJButtonMouseExited(evt);
            }
        });
        validatedHitsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validatedHitsHelpJButtonActionPerformed(evt);
            }
        });

        falsePositivesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        falsePositivesHelpJButton.setToolTipText("Help");
        falsePositivesHelpJButton.setBorder(null);
        falsePositivesHelpJButton.setBorderPainted(false);
        falsePositivesHelpJButton.setContentAreaFilled(false);
        falsePositivesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        falsePositivesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                falsePositivesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                falsePositivesHelpJButtonMouseExited(evt);
            }
        });
        falsePositivesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                falsePositivesHelpJButtonActionPerformed(evt);
            }
        });

        truePositivesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        truePositivesHelpJButton.setToolTipText("Help");
        truePositivesHelpJButton.setBorder(null);
        truePositivesHelpJButton.setBorderPainted(false);
        truePositivesHelpJButton.setContentAreaFilled(false);
        truePositivesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        truePositivesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                truePositivesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                truePositivesHelpJButtonMouseExited(evt);
            }
        });
        truePositivesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                truePositivesHelpJButtonActionPerformed(evt);
            }
        });

        nMaxHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        nMaxHelpJButton.setToolTipText("Help");
        nMaxHelpJButton.setBorder(null);
        nMaxHelpJButton.setBorderPainted(false);
        nMaxHelpJButton.setContentAreaFilled(false);
        nMaxHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        nMaxHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                nMaxHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                nMaxHelpJButtonMouseExited(evt);
            }
        });
        nMaxHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nMaxHelpJButtonActionPerformed(evt);
            }
        });

        confidenceHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        confidenceHelpJButton.setToolTipText("Help");
        confidenceHelpJButton.setBorder(null);
        confidenceHelpJButton.setBorderPainted(false);
        confidenceHelpJButton.setContentAreaFilled(false);
        confidenceHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        confidenceHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                confidenceHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                confidenceHelpJButtonMouseExited(evt);
            }
        });
        confidenceHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confidenceHelpJButtonActionPerformed(evt);
            }
        });

        fdrHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        fdrHelpJButton.setToolTipText("Help");
        fdrHelpJButton.setBorder(null);
        fdrHelpJButton.setBorderPainted(false);
        fdrHelpJButton.setContentAreaFilled(false);
        fdrHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        fdrHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fdrHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fdrHelpJButtonMouseExited(evt);
            }
        });
        fdrHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrHelpJButtonActionPerformed(evt);
            }
        });

        fnrHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        fnrHelpJButton.setToolTipText("Help");
        fnrHelpJButton.setBorder(null);
        fnrHelpJButton.setBorderPainted(false);
        fnrHelpJButton.setContentAreaFilled(false);
        fnrHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        fnrHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fnrHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fnrHelpJButtonMouseExited(evt);
            }
        });
        fnrHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fnrHelpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout idSummaryJPanelLayout = new javax.swing.GroupLayout(idSummaryJPanel);
        idSummaryJPanel.setLayout(idSummaryJPanelLayout);
        idSummaryJPanelLayout.setHorizontalGroup(
            idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel6)
                    .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(nValidatedTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                            .addComponent(nFPTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                            .addComponent(nTPlTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                            .addComponent(nTotalTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(falsePositivesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(validatedHitsHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(truePositivesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(totalTPHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel36)
                            .addComponent(jLabel13)
                            .addComponent(jLabel23)
                            .addComponent(jLabel20))
                        .addGap(9, 9, 9)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(nMaxTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                            .addComponent(confidenceTxt, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(fnrTxt)
                                .addComponent(fdrTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fdrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fnrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(confidenceHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nMaxHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel7))
                .addContainerGap())
        );
        idSummaryJPanelLayout.setVerticalGroup(
            idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(nTotalTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(totalTPHelpJButton)
                    .addComponent(jLabel20)
                    .addComponent(nMaxTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nMaxHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(nValidatedTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(validatedHitsHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)
                    .addComponent(jLabel23)
                    .addComponent(confidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(confidenceHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel3)
                    .addComponent(nFPTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(falsePositivesHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)
                    .addComponent(jLabel13)
                    .addComponent(fdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fdrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(truePositivesHelpJButton, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.PREFERRED_SIZE, 22, Short.MAX_VALUE)
                    .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(nTPlTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10))
                    .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(fnrHelpJButton, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                        .addComponent(fnrTxt, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel36)))
                .addContainerGap())
        );

        optimizationJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Optimization"));
        optimizationJPanel.setOpaque(false);

        optimizationTabbedPane.setBackground(new java.awt.Color(255, 255, 255));
        optimizationTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        estimatorOptimizationTab.setBackground(new java.awt.Color(255, 255, 255));

        estimatorsPlotSplitPane.setBorder(null);
        estimatorsPlotSplitPane.setDividerLocation(estimatorsPlotSplitPane.getWidth() / 2);
        estimatorsPlotSplitPane.setDividerSize(0);
        estimatorsPlotSplitPane.setResizeWeight(0.5);
        estimatorsPlotSplitPane.setOpaque(false);

        pepPanel.setOpaque(false);

        jLabel30.setText("Sensitivity");

        jLabel27.setText("Robustness");

        sensitivitySlider1.setToolTipText("" + sensitivitySlider1.getValue());
        sensitivitySlider1.setEnabled(false);
        sensitivitySlider1.setOpaque(false);
        sensitivitySlider1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sensitivitySlider1MouseReleased(evt);
            }
        });
        sensitivitySlider1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                sensitivitySlider1MouseDragged(evt);
            }
        });

        pepChartPanel.setOpaque(false);
        pepChartPanel.setLayout(new javax.swing.BoxLayout(pepChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        pepChartPanel.setBounds(0, 90, 587, 168);
        pepPlotLayeredPane.add(pepChartPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        pepPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        pepPlotHelpJButton.setToolTipText("Help");
        pepPlotHelpJButton.setBorder(null);
        pepPlotHelpJButton.setBorderPainted(false);
        pepPlotHelpJButton.setContentAreaFilled(false);
        pepPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        pepPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pepPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pepPlotHelpJButtonMouseExited(evt);
            }
        });
        pepPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pepPlotHelpJButtonActionPerformed(evt);
            }
        });
        pepPlotHelpJButton.setBounds(577, 10, 10, 25);
        pepPlotLayeredPane.add(pepPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        pepPlotExportJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        pepPlotExportJButton.setToolTipText("Export");
        pepPlotExportJButton.setBorder(null);
        pepPlotExportJButton.setBorderPainted(false);
        pepPlotExportJButton.setContentAreaFilled(false);
        pepPlotExportJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        pepPlotExportJButton.setEnabled(false);
        pepPlotExportJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        pepPlotExportJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pepPlotExportJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pepPlotExportJButtonMouseExited(evt);
            }
        });
        pepPlotExportJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pepPlotExportJButtonActionPerformed(evt);
            }
        });
        pepPlotExportJButton.setBounds(550, 10, 10, 25);
        pepPlotLayeredPane.add(pepPlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout pepPanelLayout = new javax.swing.GroupLayout(pepPanel);
        pepPanel.setLayout(pepPanelLayout);
        pepPanelLayout.setHorizontalGroup(
            pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pepPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pepPlotLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                    .addGroup(pepPanelLayout.createSequentialGroup()
                        .addComponent(jLabel30)
                        .addGap(18, 18, 18)
                        .addComponent(sensitivitySlider1, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel27)))
                .addContainerGap())
        );
        pepPanelLayout.setVerticalGroup(
            pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pepPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pepPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel30)
                    .addComponent(sensitivitySlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addContainerGap())
        );

        estimatorsPlotSplitPane.setLeftComponent(pepPanel);

        fdrsPanel.setOpaque(false);

        jLabel34.setText("Sensitivity");

        sensitivitySlider2.setEnabled(false);
        sensitivitySlider2.setOpaque(false);
        sensitivitySlider2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sensitivitySlider2MouseReleased(evt);
            }
        });
        sensitivitySlider2.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                sensitivitySlider2MouseDragged(evt);
            }
        });

        jLabel35.setText("Robustness");

        fdrsChartPanel.setOpaque(false);
        fdrsChartPanel.setLayout(new javax.swing.BoxLayout(fdrsChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        fdrsChartPanel.setBounds(0, 80, 588, 173);
        fdrsPlotLayeredPane.add(fdrsChartPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        fdrsPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        fdrsPlotHelpJButton.setToolTipText("Help");
        fdrsPlotHelpJButton.setBorder(null);
        fdrsPlotHelpJButton.setBorderPainted(false);
        fdrsPlotHelpJButton.setContentAreaFilled(false);
        fdrsPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        fdrsPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fdrsPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fdrsPlotHelpJButtonMouseExited(evt);
            }
        });
        fdrsPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrsPlotHelpJButtonActionPerformed(evt);
            }
        });
        fdrsPlotHelpJButton.setBounds(570, 10, 10, 25);
        fdrsPlotLayeredPane.add(fdrsPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        fdrPlotExportJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        fdrPlotExportJButton.setToolTipText("Export");
        fdrPlotExportJButton.setBorder(null);
        fdrPlotExportJButton.setBorderPainted(false);
        fdrPlotExportJButton.setContentAreaFilled(false);
        fdrPlotExportJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        fdrPlotExportJButton.setEnabled(false);
        fdrPlotExportJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        fdrPlotExportJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fdrPlotExportJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fdrPlotExportJButtonMouseExited(evt);
            }
        });
        fdrPlotExportJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrPlotExportJButtonActionPerformed(evt);
            }
        });
        fdrPlotExportJButton.setBounds(550, 10, 10, 25);
        fdrsPlotLayeredPane.add(fdrPlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout fdrsPanelLayout = new javax.swing.GroupLayout(fdrsPanel);
        fdrsPanel.setLayout(fdrsPanelLayout);
        fdrsPanelLayout.setHorizontalGroup(
            fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fdrsPlotLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                    .addGroup(fdrsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addGap(18, 18, 18)
                        .addComponent(sensitivitySlider2, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel35)))
                .addContainerGap())
        );
        fdrsPanelLayout.setVerticalGroup(
            fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fdrsPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(sensitivitySlider2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel35)
                    .addComponent(jLabel34))
                .addContainerGap())
        );

        estimatorsPlotSplitPane.setRightComponent(fdrsPanel);

        javax.swing.GroupLayout estimatorOptimizationTabLayout = new javax.swing.GroupLayout(estimatorOptimizationTab);
        estimatorOptimizationTab.setLayout(estimatorOptimizationTabLayout);
        estimatorOptimizationTabLayout.setHorizontalGroup(
            estimatorOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(estimatorsPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1215, Short.MAX_VALUE)
        );
        estimatorOptimizationTabLayout.setVerticalGroup(
            estimatorOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(estimatorsPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
        );

        optimizationTabbedPane.addTab("Estimators", estimatorOptimizationTab);

        thresholdOptimizationTab.setBackground(new java.awt.Color(255, 255, 255));

        leftPlotSplitPane.setBorder(null);
        leftPlotSplitPane.setDividerLocation(leftPlotSplitPane.getWidth() / 3);
        leftPlotSplitPane.setDividerSize(0);
        leftPlotSplitPane.setResizeWeight(0.5);
        leftPlotSplitPane.setOpaque(false);

        confidencePanel.setOpaque(false);

        jLabel25.setText("Quantity");

        confidenceSlider.setToolTipText("Confidence Threshold");
        confidenceSlider.setEnabled(false);
        confidenceSlider.setOpaque(false);
        confidenceSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                confidenceSliderMouseReleased(evt);
            }
        });
        confidenceSlider.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                confidenceSliderMouseDragged(evt);
            }
        });

        jLabel26.setText("Quality");

        confidenceChartPanel.setOpaque(false);
        confidenceChartPanel.setLayout(new javax.swing.BoxLayout(confidenceChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        confidenceChartPanel.setBounds(0, 0, 500, 460);
        confidencePlotLayeredPane.add(confidenceChartPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        confidencePlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        confidencePlotHelpJButton.setToolTipText("Help");
        confidencePlotHelpJButton.setBorder(null);
        confidencePlotHelpJButton.setBorderPainted(false);
        confidencePlotHelpJButton.setContentAreaFilled(false);
        confidencePlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        confidencePlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                confidencePlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                confidencePlotHelpJButtonMouseExited(evt);
            }
        });
        confidencePlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confidencePlotHelpJButtonActionPerformed(evt);
            }
        });
        confidencePlotHelpJButton.setBounds(480, 0, 10, 25);
        confidencePlotLayeredPane.add(confidencePlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        confidencePlotExportJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        confidencePlotExportJButton.setToolTipText("Export");
        confidencePlotExportJButton.setBorder(null);
        confidencePlotExportJButton.setBorderPainted(false);
        confidencePlotExportJButton.setContentAreaFilled(false);
        confidencePlotExportJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        confidencePlotExportJButton.setEnabled(false);
        confidencePlotExportJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        confidencePlotExportJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                confidencePlotExportJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                confidencePlotExportJButtonMouseExited(evt);
            }
        });
        confidencePlotExportJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confidencePlotExportJButtonActionPerformed(evt);
            }
        });
        confidencePlotExportJButton.setBounds(460, 0, 10, 25);
        confidencePlotLayeredPane.add(confidencePlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout confidencePanelLayout = new javax.swing.GroupLayout(confidencePanel);
        confidencePanel.setLayout(confidencePanelLayout);
        confidencePanelLayout.setHorizontalGroup(
            confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confidencePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(confidencePlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
                    .addGroup(confidencePanelLayout.createSequentialGroup()
                        .addComponent(jLabel25)
                        .addGap(18, 18, 18)
                        .addComponent(confidenceSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel26)))
                .addContainerGap())
        );
        confidencePanelLayout.setVerticalGroup(
            confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, confidencePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(confidencePlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(confidenceSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel25)))
                .addContainerGap())
        );

        leftPlotSplitPane.setLeftComponent(confidencePanel);

        rightPlotSplitPane.setBorder(null);
        rightPlotSplitPane.setDividerLocation(rightPlotSplitPane.getWidth() / 2);
        rightPlotSplitPane.setDividerSize(0);
        rightPlotSplitPane.setResizeWeight(0.5);
        rightPlotSplitPane.setOpaque(false);

        fdrFnrPanel.setOpaque(false);

        jLabel28.setText("Quality");

        fdrSlider1.setToolTipText("FDR Threshold");
        fdrSlider1.setEnabled(false);
        fdrSlider1.setOpaque(false);
        fdrSlider1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                fdrSlider1MouseReleased(evt);
            }
        });
        fdrSlider1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                fdrSlider1MouseDragged(evt);
            }
        });

        jLabel29.setText("Quantity");

        fdrFnrChartPanel.setOpaque(false);
        fdrFnrChartPanel.setLayout(new javax.swing.BoxLayout(fdrFnrChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        fdrFnrChartPanel.setBounds(0, 3, 320, 450);
        fdrPlotLayeredPane.add(fdrFnrChartPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        fdrFnrPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        fdrFnrPlotHelpJButton.setToolTipText("Help");
        fdrFnrPlotHelpJButton.setBorder(null);
        fdrFnrPlotHelpJButton.setBorderPainted(false);
        fdrFnrPlotHelpJButton.setContentAreaFilled(false);
        fdrFnrPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        fdrFnrPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fdrFnrPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fdrFnrPlotHelpJButtonMouseExited(evt);
            }
        });
        fdrFnrPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrFnrPlotHelpJButtonActionPerformed(evt);
            }
        });
        fdrFnrPlotHelpJButton.setBounds(300, 10, 10, 25);
        fdrPlotLayeredPane.add(fdrFnrPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        fdrFnrPlotExportJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        fdrFnrPlotExportJButton.setToolTipText("Export");
        fdrFnrPlotExportJButton.setBorder(null);
        fdrFnrPlotExportJButton.setBorderPainted(false);
        fdrFnrPlotExportJButton.setContentAreaFilled(false);
        fdrFnrPlotExportJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        fdrFnrPlotExportJButton.setEnabled(false);
        fdrFnrPlotExportJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        fdrFnrPlotExportJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fdrFnrPlotExportJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fdrFnrPlotExportJButtonMouseExited(evt);
            }
        });
        fdrFnrPlotExportJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrFnrPlotExportJButtonActionPerformed(evt);
            }
        });
        fdrFnrPlotExportJButton.setBounds(290, 10, 10, 25);
        fdrPlotLayeredPane.add(fdrFnrPlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout fdrFnrPanelLayout = new javax.swing.GroupLayout(fdrFnrPanel);
        fdrFnrPanel.setLayout(fdrFnrPanelLayout);
        fdrFnrPanelLayout.setHorizontalGroup(
            fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrFnrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fdrPlotLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
                    .addGroup(fdrFnrPanelLayout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addGap(18, 18, 18)
                        .addComponent(fdrSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel29)))
                .addContainerGap())
        );
        fdrFnrPanelLayout.setVerticalGroup(
            fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrFnrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fdrPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(fdrSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel29)
                        .addComponent(jLabel28)))
                .addContainerGap())
        );

        rightPlotSplitPane.setLeftComponent(fdrFnrPanel);

        benefitCostPanel.setOpaque(false);

        jLabel32.setText("Quality");

        fdrSlider2.setToolTipText("FDR Threshold");
        fdrSlider2.setEnabled(false);
        fdrSlider2.setOpaque(false);
        fdrSlider2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                fdrSlider2MouseReleased(evt);
            }
        });
        fdrSlider2.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                fdrSlider2MouseDragged(evt);
            }
        });

        jLabel33.setText("Quantity");

        benefitCostChartPanel.setOpaque(false);
        benefitCostChartPanel.setLayout(new javax.swing.BoxLayout(benefitCostChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        benefitCostChartPanel.setBounds(0, -4, 326, 450);
        benefitPlotLayeredPane.add(benefitCostChartPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        benefitPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        benefitPlotHelpJButton.setToolTipText("Help");
        benefitPlotHelpJButton.setBorder(null);
        benefitPlotHelpJButton.setBorderPainted(false);
        benefitPlotHelpJButton.setContentAreaFilled(false);
        benefitPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        benefitPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                benefitPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                benefitPlotHelpJButtonMouseExited(evt);
            }
        });
        benefitPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                benefitPlotHelpJButtonActionPerformed(evt);
            }
        });
        benefitPlotHelpJButton.setBounds(300, 10, 10, 25);
        benefitPlotLayeredPane.add(benefitPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        benefitPlotExportJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        benefitPlotExportJButton.setToolTipText("Export");
        benefitPlotExportJButton.setBorder(null);
        benefitPlotExportJButton.setBorderPainted(false);
        benefitPlotExportJButton.setContentAreaFilled(false);
        benefitPlotExportJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        benefitPlotExportJButton.setEnabled(false);
        benefitPlotExportJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        benefitPlotExportJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                benefitPlotExportJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                benefitPlotExportJButtonMouseExited(evt);
            }
        });
        benefitPlotExportJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                benefitPlotExportJButtonActionPerformed(evt);
            }
        });
        benefitPlotExportJButton.setBounds(290, 10, 10, 25);
        benefitPlotLayeredPane.add(benefitPlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout benefitCostPanelLayout = new javax.swing.GroupLayout(benefitCostPanel);
        benefitCostPanel.setLayout(benefitCostPanelLayout);
        benefitCostPanelLayout.setHorizontalGroup(
            benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, benefitCostPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(benefitPlotLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                    .addGroup(benefitCostPanelLayout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addGap(18, 18, 18)
                        .addComponent(fdrSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel33)))
                .addContainerGap())
        );
        benefitCostPanelLayout.setVerticalGroup(
            benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, benefitCostPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(benefitPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel32)
                    .addComponent(fdrSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel33))
                .addContainerGap())
        );

        rightPlotSplitPane.setRightComponent(benefitCostPanel);

        leftPlotSplitPane.setRightComponent(rightPlotSplitPane);

        javax.swing.GroupLayout thresholdOptimizationTabLayout = new javax.swing.GroupLayout(thresholdOptimizationTab);
        thresholdOptimizationTab.setLayout(thresholdOptimizationTabLayout);
        thresholdOptimizationTabLayout.setHorizontalGroup(
            thresholdOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(leftPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1215, Short.MAX_VALUE)
        );
        thresholdOptimizationTabLayout.setVerticalGroup(
            thresholdOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(leftPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
        );

        optimizationTabbedPane.addTab("Thresholds", thresholdOptimizationTab);

        optimizationTabbedPane.setSelectedIndex(1);

        javax.swing.GroupLayout optimizationJPanelLayout = new javax.swing.GroupLayout(optimizationJPanel);
        optimizationJPanel.setLayout(optimizationJPanelLayout);
        optimizationJPanelLayout.setHorizontalGroup(
            optimizationJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optimizationJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(optimizationTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1220, Short.MAX_VALUE)
                .addContainerGap())
        );
        optimizationJPanelLayout.setVerticalGroup(
            optimizationJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, optimizationJPanelLayout.createSequentialGroup()
                .addComponent(optimizationTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE)
                .addContainerGap())
        );

        parametersJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));
        parametersJPanel.setOpaque(false);

        thresholdInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        thresholdInput.setToolTipText("Threshold in percent");
        thresholdInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thresholdInputActionPerformed(evt);
            }
        });
        thresholdInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                thresholdInputKeyReleased(evt);
            }
        });

        jLabel4.setFont(jLabel4.getFont().deriveFont((jLabel4.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel4.setText("Threshold Optimization");

        validateButton.setText("Validate");
        validateButton.setToolTipText("Reload the data with the current threshold setting");
        validateButton.setEnabled(false);
        validateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateButtonActionPerformed(evt);
            }
        });

        jLabel5.setFont(jLabel5.getFont().deriveFont((jLabel5.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel5.setText("Estimator Optimization");

        jLabel22.setText("PEP Window:");
        jLabel22.setToolTipText("Posterior Error Probability window");

        windowTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        windowTxt.setToolTipText("Posterior Error Probability window");
        windowTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                windowTxtActionPerformed(evt);
            }
        });
        windowTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                windowTxtKeyReleased(evt);
            }
        });

        applyButton.setText("Apply");
        applyButton.setToolTipText("Reload the data with the current PEP window");
        applyButton.setEnabled(false);
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        fdrCombo1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Classical", "Probabilistic" }));
        fdrCombo1.setToolTipText("Estimator type");
        fdrCombo1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrCombo1ActionPerformed(evt);
            }
        });

        thresholdTypeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Confidence", "FDR", "FNR" }));
        thresholdTypeCmb.setToolTipText("Threshold type");

        jLabel8.setText("Threshold (%):");
        jLabel8.setToolTipText("Threshold in percent");

        thresholdHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        thresholdHelpJButton.setToolTipText("Help");
        thresholdHelpJButton.setBorder(null);
        thresholdHelpJButton.setBorderPainted(false);
        thresholdHelpJButton.setContentAreaFilled(false);
        thresholdHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        thresholdHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                thresholdHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                thresholdHelpJButtonMouseExited(evt);
            }
        });
        thresholdHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thresholdHelpJButtonActionPerformed(evt);
            }
        });

        pepHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        pepHelpJButton.setToolTipText("Help");
        pepHelpJButton.setBorder(null);
        pepHelpJButton.setBorderPainted(false);
        pepHelpJButton.setContentAreaFilled(false);
        pepHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        pepHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pepHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pepHelpJButtonMouseExited(evt);
            }
        });
        pepHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pepHelpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout parametersJPanelLayout = new javax.swing.GroupLayout(parametersJPanel);
        parametersJPanel.setLayout(parametersJPanelLayout);
        parametersJPanelLayout.setHorizontalGroup(
            parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(thresholdInput, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(thresholdTypeCmb, 0, 110, Short.MAX_VALUE)
                            .addComponent(validateButton, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(thresholdHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel5)
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel22)
                        .addGap(18, 18, 18)
                        .addComponent(windowTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(applyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fdrCombo1, 0, 110, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pepHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(199, Short.MAX_VALUE))
        );

        parametersJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {applyButton, fdrCombo1, thresholdTypeCmb, validateButton});

        parametersJPanelLayout.setVerticalGroup(
            parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, parametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel8)
                    .addComponent(thresholdInput, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
                    .addComponent(thresholdTypeCmb)
                    .addComponent(thresholdHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addComponent(validateButton)
                        .addGap(11, 11, 11)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(windowTxt)
                                .addComponent(jLabel22))
                            .addComponent(fdrCombo1, javax.swing.GroupLayout.Alignment.LEADING)))
                    .addComponent(pepHelpJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(applyButton)
                .addGap(34, 34, 34))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(optimizationJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(groupListJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idSummaryJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(parametersJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(groupListJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(idSummaryJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(parametersJPanel, 0, 219, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(optimizationJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void thresholdInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thresholdInputActionPerformed
        try {
            lastThreshold = new Double(thresholdInput.getText());

            if (lastThreshold < 0 || lastThreshold > 100) {
                JOptionPane.showMessageDialog(this, "Please verify the given threshold. Interval: [0, 100].", "Threshold Error", JOptionPane.WARNING_MESSAGE);
            } else {
                lastThresholdType = thresholdTypeCmb.getSelectedIndex();
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                updateResults();
                updateDisplayedComponents();
                validateButton.setEnabled(true);
                dataValidated = false;
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold.", "Threshold Error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_thresholdInputActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrCombo1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrCombo1ActionPerformed

        if (currentTargetDecoyMap != null) {
            TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();

            if (fdrCombo1.getSelectedIndex() == 0) {
                currentResults.setClassicalEstimators(true);
            } else {
                currentResults.setClassicalEstimators(false);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            updateResults();
            updateDisplayedComponents();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_fdrCombo1ActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void confidenceSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidenceSliderMouseReleased
        lastThreshold = new Double(confidenceSlider.getValue());
        lastThresholdType = 0;
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        updateResults();
        updateDisplayedComponents();
        validateButton.setEnabled(true);
        dataValidated = false;
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_confidenceSliderMouseReleased

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void confidenceSliderMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidenceSliderMouseDragged
        thresholdTypeCmb.setSelectedIndex(0);
        thresholdInput.setText(confidenceSlider.getValue() + "");
    }//GEN-LAST:event_confidenceSliderMouseDragged

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrSlider1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrSlider1MouseDragged
        thresholdTypeCmb.setSelectedIndex(1);
        thresholdInput.setText(fdrSlider1.getValue() + "");
    }//GEN-LAST:event_fdrSlider1MouseDragged

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrSlider1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrSlider1MouseReleased
        lastThreshold = new Double(fdrSlider1.getValue());
        lastThresholdType = 1;
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        updateResults();
        updateDisplayedComponents();
        validateButton.setEnabled(true);
        dataValidated = false;
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrSlider1MouseReleased

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void windowTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_windowTxtActionPerformed
        try {
            Integer newWindow = new Integer(windowTxt.getText());

            if (newWindow < 0) {
                JOptionPane.showMessageDialog(this, "Please verify the given window size. Has to be a positive value.", "Window Error", JOptionPane.WARNING_MESSAGE);
            } else {
                currentTargetDecoyMap.setWindowSize(newWindow);
                currentTargetDecoyMap.estimateProbabilities(null);
                targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                updateResults();
                updateDisplayedComponents();
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                //modifiedMaps.put(groupList.getSelectedIndex(), true);
                modifiedMaps.put(groupSelectionTable.getSelectedRow(), true);
                applyButton.setEnabled(true);
                pepWindowApplied = false;
            }
        } catch (Exception e) {
            if (currentTargetDecoyMap != null) {
                JOptionPane.showMessageDialog(this, "Please verify the given window size.", "Window Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_windowTxtActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void sensitivitySlider1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sensitivitySlider1MouseDragged
        if (currentTargetDecoyMap != null) {
            Double newWindow = Math.pow(10, sensitivitySlider1.getValue() / 50.0 - 1) * currentTargetDecoyMap.getnMax();
            windowTxt.setText(newWindow.intValue() + "");
            sensitivitySlider2.setValue(sensitivitySlider1.getValue());
        }
    }//GEN-LAST:event_sensitivitySlider1MouseDragged

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void sensitivitySlider1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sensitivitySlider1MouseReleased
        windowTxtActionPerformed(null);
    }//GEN-LAST:event_sensitivitySlider1MouseReleased

    /**
     * Apply the validations/optimizations.
     *
     * @param evt
     */
    private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed

        Integer newWindow = new Integer(windowTxt.getText());

        if (newWindow < 0) {
            JOptionPane.showMessageDialog(this, "Please verify the given window size. Has to be a positive value.", "Window Error", JOptionPane.WARNING_MESSAGE);
        } else {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            if (groupSelectionTable.getSelectedRow() == 0) {
                applyProteins();
            } else if (peptideMap.keySet().contains(groupSelectionTable.getSelectedRow())) {
                recalculateProteins();
            } else {
                recalculatePeptidesAndProteins();
            }

            applyButton.setEnabled(false);
            pepWindowApplied = true;

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            // reload the data to update the scores
            reloadData();
        }
    }//GEN-LAST:event_applyButtonActionPerformed

    /**
     * Resizes the plot sizes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                leftPlotSplitPane.setDividerLocation(leftPlotSplitPane.getWidth() / 3);
                estimatorsPlotSplitPane.setDividerLocation(estimatorsPlotSplitPane.getWidth() / 2);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                rightPlotSplitPane.setDividerLocation(rightPlotSplitPane.getWidth() / 2);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                optimizationJPanel.revalidate();
                optimizationJPanel.repaint();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                // move the icons
                confidencePlotLayeredPane.getComponent(0).setBounds(
                        confidencePlotLayeredPane.getWidth() - confidencePlotLayeredPane.getComponent(0).getWidth() - 10,
                        confidencePlotLayeredPane.getComponent(0).getHeight() / 2 - 12,
                        confidencePlotLayeredPane.getComponent(0).getWidth(),
                        confidencePlotLayeredPane.getComponent(0).getHeight());
                
                confidencePlotLayeredPane.getComponent(1).setBounds(
                        confidencePlotLayeredPane.getWidth() - confidencePlotLayeredPane.getComponent(0).getWidth() - 20,
                        confidencePlotLayeredPane.getComponent(1).getHeight() / 2 - 12,
                        confidencePlotLayeredPane.getComponent(1).getWidth(),
                        confidencePlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                confidencePlotLayeredPane.getComponent(2).setBounds(0, 0, confidencePlotLayeredPane.getWidth(), confidencePlotLayeredPane.getHeight());
                confidencePlotLayeredPane.revalidate();
                confidencePlotLayeredPane.repaint();


                // move the icons
                fdrPlotLayeredPane.getComponent(0).setBounds(
                        fdrPlotLayeredPane.getWidth() - fdrPlotLayeredPane.getComponent(0).getWidth() - 10,
                        fdrPlotLayeredPane.getComponent(0).getHeight() / 2 - 12,
                        fdrPlotLayeredPane.getComponent(0).getWidth(),
                        fdrPlotLayeredPane.getComponent(0).getHeight());
                
                fdrPlotLayeredPane.getComponent(1).setBounds(
                        fdrPlotLayeredPane.getWidth() - fdrPlotLayeredPane.getComponent(0).getWidth() - 20,
                        fdrPlotLayeredPane.getComponent(1).getHeight() / 2 - 12,
                        fdrPlotLayeredPane.getComponent(1).getWidth(),
                        fdrPlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                fdrPlotLayeredPane.getComponent(2).setBounds(0, 0, fdrPlotLayeredPane.getWidth(), fdrPlotLayeredPane.getHeight());
                fdrPlotLayeredPane.revalidate();
                fdrPlotLayeredPane.repaint();


                // move the icons
                benefitPlotLayeredPane.getComponent(0).setBounds(
                        benefitPlotLayeredPane.getWidth() - benefitPlotLayeredPane.getComponent(0).getWidth() - 10,
                        benefitPlotLayeredPane.getComponent(0).getHeight() / 2 - 12,
                        benefitPlotLayeredPane.getComponent(0).getWidth(),
                        benefitPlotLayeredPane.getComponent(0).getHeight());
                
                benefitPlotLayeredPane.getComponent(1).setBounds(
                        benefitPlotLayeredPane.getWidth() - benefitPlotLayeredPane.getComponent(0).getWidth() - 20,
                        benefitPlotLayeredPane.getComponent(1).getHeight() / 2 - 12,
                        benefitPlotLayeredPane.getComponent(1).getWidth(),
                        benefitPlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                benefitPlotLayeredPane.getComponent(2).setBounds(0, 0, benefitPlotLayeredPane.getWidth(), benefitPlotLayeredPane.getHeight());
                benefitPlotLayeredPane.revalidate();
                benefitPlotLayeredPane.repaint();


                // move the icons
                pepPlotLayeredPane.getComponent(0).setBounds(
                        pepPlotLayeredPane.getWidth() - pepPlotLayeredPane.getComponent(0).getWidth() - 10,
                        pepPlotLayeredPane.getComponent(0).getHeight() / 2 - 12,
                        pepPlotLayeredPane.getComponent(0).getWidth(),
                        pepPlotLayeredPane.getComponent(0).getHeight());
                
                pepPlotLayeredPane.getComponent(1).setBounds(
                        pepPlotLayeredPane.getWidth() - pepPlotLayeredPane.getComponent(1).getWidth() - 20,
                        pepPlotLayeredPane.getComponent(1).getHeight() / 2 - 12,
                        pepPlotLayeredPane.getComponent(1).getWidth(),
                        pepPlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                pepPlotLayeredPane.getComponent(2).setBounds(0, 0, pepPlotLayeredPane.getWidth(), pepPlotLayeredPane.getHeight());
                pepPlotLayeredPane.revalidate();
                pepPlotLayeredPane.repaint();


                // move the icons
                fdrsPlotLayeredPane.getComponent(0).setBounds(
                        fdrsPlotLayeredPane.getWidth() - fdrsPlotLayeredPane.getComponent(0).getWidth() - 10,
                        fdrsPlotLayeredPane.getComponent(0).getHeight() / 2 - 12,
                        fdrsPlotLayeredPane.getComponent(0).getWidth(),
                        fdrsPlotLayeredPane.getComponent(0).getHeight());
                
                fdrsPlotLayeredPane.getComponent(1).setBounds(
                        fdrsPlotLayeredPane.getWidth() - fdrsPlotLayeredPane.getComponent(0).getWidth() - 20,
                        fdrsPlotLayeredPane.getComponent(1).getHeight() / 2 - 12,
                        fdrsPlotLayeredPane.getComponent(1).getWidth(),
                        fdrsPlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                fdrsPlotLayeredPane.getComponent(2).setBounds(0, 0, fdrsPlotLayeredPane.getWidth(), fdrsPlotLayeredPane.getHeight());
                fdrsPlotLayeredPane.revalidate();
                fdrsPlotLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Validate the data using the current settings.
     *
     * @param evt
     */
    private void validateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateButtonActionPerformed

        lastThreshold = new Double(thresholdInput.getText());

        if (lastThreshold < 0 || lastThreshold > 100) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold. Interval: [0, 100].", "Threshold Error", JOptionPane.WARNING_MESSAGE);
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            PSMaps pSMaps = new PSMaps();
            pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
            PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getExperiment(), peptideShakerGUI.getSample(), peptideShakerGUI.getReplicateNumber(), pSMaps);
            miniShaker.validateIdentifications();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            // update the other tabs
            reloadData();
            validateButton.setEnabled(false);
            dataValidated = true;
        }
    }//GEN-LAST:event_validateButtonActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrSlider2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrSlider2MouseReleased
        fdrSlider1.setValue(fdrSlider2.getValue());
        fdrSlider1MouseReleased(null);
    }//GEN-LAST:event_fdrSlider2MouseReleased

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrSlider2MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrSlider2MouseDragged
        fdrSlider1.setValue(fdrSlider2.getValue());
        fdrSlider1MouseDragged(null);
    }//GEN-LAST:event_fdrSlider2MouseDragged

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void sensitivitySlider2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sensitivitySlider2MouseReleased
        sensitivitySlider1MouseReleased(null);
    }//GEN-LAST:event_sensitivitySlider2MouseReleased

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void sensitivitySlider2MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sensitivitySlider2MouseDragged
        sensitivitySlider1.setValue(sensitivitySlider2.getValue());
        sensitivitySlider1MouseDragged(null);
    }//GEN-LAST:event_sensitivitySlider2MouseDragged

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void thresholdHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thresholdHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Setting_the_Threshold");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_thresholdHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void pepHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Optimize_Estimator_Accuracy");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void totalTPHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_totalTPHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_totalTPHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void benefitPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_benefitPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#ROC_plot");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_benefitPlotHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void fdrFnrPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrFnrPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#FDR_FNR_plot");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrFnrPlotHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void confidencePlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confidencePlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Confidence_plot");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_confidencePlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void totalTPHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_totalTPHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_totalTPHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void totalTPHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_totalTPHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_totalTPHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void confidencePlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidencePlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_confidencePlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void confidencePlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidencePlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_confidencePlotHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void pepPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pepPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void pepPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepPlotHelpJButtonMouseExited

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void pepPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#PEP_plot");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepPlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void fdrsPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrsPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_fdrsPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void fdrsPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrsPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrsPlotHelpJButtonMouseExited

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void fdrsPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrsPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#FDR_plot");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrsPlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void thresholdHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_thresholdHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_thresholdHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void thresholdHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_thresholdHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_thresholdHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void pepHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pepHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void pepHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void fdrFnrPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrFnrPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_fdrFnrPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void fdrFnrPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrFnrPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrFnrPlotHelpJButtonMouseExited

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void benefitPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benefitPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_benefitPlotHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void benefitPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benefitPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_benefitPlotHelpJButtonMouseEntered

    /**
     * Update the threshold setting.
     * 
     * @param evt 
     */
    private void thresholdInputKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_thresholdInputKeyReleased
        try {
            lastThreshold = new Double(thresholdInput.getText());

            if (lastThreshold < 0 || lastThreshold > 100) {
                JOptionPane.showMessageDialog(this, "Please verify the given threshold. Interval: [0, 100].", "Threshold Error", JOptionPane.WARNING_MESSAGE);
            } else {
                validateButton.setEnabled(true);
                dataValidated = false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold.", "Threshold Error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_thresholdInputKeyReleased

    /**
     * Update the PEP window setting.
     * 
     * @param evt 
     */
    private void windowTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_windowTxtKeyReleased
        try {
            Integer newWindow = new Integer(windowTxt.getText());

            if (newWindow < 0) {
                JOptionPane.showMessageDialog(this, "Please verify the given window size. Has to be a positive value.", "Window Error", JOptionPane.WARNING_MESSAGE);
            } else {
                applyButton.setEnabled(true);
                pepWindowApplied = false;
            }
        } catch (Exception e) {
            if (currentTargetDecoyMap != null) {
                JOptionPane.showMessageDialog(this, "Please verify the given window size.", "Window Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_windowTxtKeyReleased

    private void nMaxHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nMaxHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_nMaxHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void nMaxHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nMaxHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_nMaxHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void nMaxHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nMaxHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_nMaxHelpJButtonMouseEntered

    private void confidenceHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confidenceHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_confidenceHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void confidenceHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidenceHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_confidenceHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void confidenceHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidenceHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_confidenceHelpJButtonMouseEntered

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void fdrHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_fdrHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void fdrHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_fdrHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void fdrHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_fdrHelpJButtonMouseEntered

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void fnrHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fnrHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_fnrHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void fnrHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fnrHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_fnrHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void fnrHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fnrHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_fnrHelpJButtonMouseEntered

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void truePositivesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_truePositivesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_truePositivesHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void truePositivesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_truePositivesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_truePositivesHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void truePositivesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_truePositivesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_truePositivesHelpJButtonMouseEntered

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void falsePositivesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_falsePositivesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_falsePositivesHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void falsePositivesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_falsePositivesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_falsePositivesHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void falsePositivesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_falsePositivesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_falsePositivesHelpJButtonMouseEntered

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void validatedHitsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validatedHitsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_summary");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_validatedHitsHelpJButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void validatedHitsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_validatedHitsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_validatedHitsHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void validatedHitsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_validatedHitsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_validatedHitsHelpJButtonMouseEntered

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void groupSelectionHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_groupSelectionHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_groupSelectionHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void groupSelectionHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_groupSelectionHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_groupSelectionHelpJButtonMouseExited

    /**
     * Opens a help dialog.
     * 
     * @param evt 
     */
    private void groupSelectionHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupSelectionHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Group_Selection");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_groupSelectionHelpJButtonActionPerformed

    /**
     * Resize the group selection layered pane components.
     * 
     * @param evt 
     */
    private void groupSelectionLayeredPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_groupSelectionLayeredPaneComponentResized

        // move the help icon
        groupSelectionLayeredPane.getComponent(0).setBounds(
                groupSelectionLayeredPane.getWidth() - groupSelectionLayeredPane.getComponent(0).getWidth() - 8,
                groupSelectionLayeredPane.getHeight() - 35,
                groupSelectionLayeredPane.getComponent(0).getWidth(),
                groupSelectionLayeredPane.getComponent(0).getHeight());

        // resize the plot area
        groupSelectionLayeredPane.getComponent(1).setBounds(0, 0, groupSelectionLayeredPane.getWidth(), groupSelectionLayeredPane.getHeight());
        groupSelectionLayeredPane.revalidate();
        groupSelectionLayeredPane.repaint();

    }//GEN-LAST:event_groupSelectionLayeredPaneComponentResized

    /**
     * Update the group selection.
     *
     * @param evt
     */
    private void groupSelectionTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_groupSelectionTableMouseReleased
        if (peptideShakerGUI.getIdentification() != null) {
            groupSelectionChanged();
        }
    }//GEN-LAST:event_groupSelectionTableMouseReleased

    /**
     * Updates the group selection.
     *
     * @param evt
     */
    private void groupSelectionTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_groupSelectionTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            groupSelectionChanged();
        }
    }//GEN-LAST:event_groupSelectionTableKeyReleased

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void pepPlotExportJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepPlotExportJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pepPlotExportJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void pepPlotExportJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pepPlotExportJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pepPlotExportJButtonMouseExited

    /**
     * Export the plot to figure format.
     * 
     * @param evt 
     */
    private void pepPlotExportJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pepPlotExportJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, true, pepChartPanel);
    }//GEN-LAST:event_pepPlotExportJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void fdrPlotExportJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrPlotExportJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_fdrPlotExportJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void fdrPlotExportJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrPlotExportJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrPlotExportJButtonMouseExited

    /**
     * Export the plot to figure format.
     * 
     * @param evt 
     */
    private void fdrPlotExportJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrPlotExportJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, true, fdrsChartPanel);
    }//GEN-LAST:event_fdrPlotExportJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void confidencePlotExportJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidencePlotExportJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_confidencePlotExportJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void confidencePlotExportJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidencePlotExportJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_confidencePlotExportJButtonMouseExited

    /**
     * Export the plot to figure format.
     * 
     * @param evt 
     */
    private void confidencePlotExportJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confidencePlotExportJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, true, confidenceChartPanel);
    }//GEN-LAST:event_confidencePlotExportJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void fdrFnrPlotExportJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrFnrPlotExportJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_fdrFnrPlotExportJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void fdrFnrPlotExportJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrFnrPlotExportJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrFnrPlotExportJButtonMouseExited

    /**
     * Export the plot to figure format.
     * 
     * @param evt 
     */
    private void fdrFnrPlotExportJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrFnrPlotExportJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, true, fdrFnrChartPanel);
    }//GEN-LAST:event_fdrFnrPlotExportJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void benefitPlotExportJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benefitPlotExportJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_benefitPlotExportJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void benefitPlotExportJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_benefitPlotExportJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_benefitPlotExportJButtonMouseExited

    /**
     * Export the plot to figure format.
     * 
     * @param evt 
     */
    private void benefitPlotExportJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_benefitPlotExportJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, true, benefitCostChartPanel);
    }//GEN-LAST:event_benefitPlotExportJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyButton;
    private javax.swing.JPanel benefitCostChartPanel;
    private javax.swing.JPanel benefitCostPanel;
    private javax.swing.JButton benefitPlotExportJButton;
    private javax.swing.JButton benefitPlotHelpJButton;
    private javax.swing.JLayeredPane benefitPlotLayeredPane;
    private javax.swing.JPanel confidenceChartPanel;
    private javax.swing.JButton confidenceHelpJButton;
    private javax.swing.JPanel confidencePanel;
    private javax.swing.JButton confidencePlotExportJButton;
    private javax.swing.JButton confidencePlotHelpJButton;
    private javax.swing.JLayeredPane confidencePlotLayeredPane;
    private javax.swing.JSlider confidenceSlider;
    private javax.swing.JTextField confidenceTxt;
    private javax.swing.JPanel estimatorOptimizationTab;
    private javax.swing.JSplitPane estimatorsPlotSplitPane;
    private javax.swing.JButton falsePositivesHelpJButton;
    private javax.swing.JComboBox fdrCombo1;
    private javax.swing.JPanel fdrFnrChartPanel;
    private javax.swing.JPanel fdrFnrPanel;
    private javax.swing.JButton fdrFnrPlotExportJButton;
    private javax.swing.JButton fdrFnrPlotHelpJButton;
    private javax.swing.JButton fdrHelpJButton;
    private javax.swing.JButton fdrPlotExportJButton;
    private javax.swing.JLayeredPane fdrPlotLayeredPane;
    private javax.swing.JSlider fdrSlider1;
    private javax.swing.JSlider fdrSlider2;
    private javax.swing.JTextField fdrTxt;
    private javax.swing.JPanel fdrsChartPanel;
    private javax.swing.JPanel fdrsPanel;
    private javax.swing.JButton fdrsPlotHelpJButton;
    private javax.swing.JLayeredPane fdrsPlotLayeredPane;
    private javax.swing.JButton fnrHelpJButton;
    private javax.swing.JTextField fnrTxt;
    private javax.swing.JPanel groupListJPanel;
    private javax.swing.JButton groupSelectionHelpJButton;
    private javax.swing.JLayeredPane groupSelectionLayeredPane;
    private javax.swing.JScrollPane groupSelectionScrollPaneScrollPane;
    private javax.swing.JTable groupSelectionTable;
    private javax.swing.JPanel idSummaryJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSplitPane leftPlotSplitPane;
    private javax.swing.JTextField nFPTxt;
    private javax.swing.JButton nMaxHelpJButton;
    private javax.swing.JTextField nMaxTxt;
    private javax.swing.JTextField nTPlTxt;
    private javax.swing.JTextField nTotalTxt;
    private javax.swing.JTextField nValidatedTxt;
    private javax.swing.JPanel optimizationJPanel;
    private javax.swing.JTabbedPane optimizationTabbedPane;
    private javax.swing.JPanel parametersJPanel;
    private javax.swing.JPanel pepChartPanel;
    private javax.swing.JButton pepHelpJButton;
    private javax.swing.JPanel pepPanel;
    private javax.swing.JButton pepPlotExportJButton;
    private javax.swing.JButton pepPlotHelpJButton;
    private javax.swing.JLayeredPane pepPlotLayeredPane;
    private javax.swing.JSplitPane rightPlotSplitPane;
    private javax.swing.JSlider sensitivitySlider1;
    private javax.swing.JSlider sensitivitySlider2;
    private javax.swing.JButton thresholdHelpJButton;
    private javax.swing.JTextField thresholdInput;
    private javax.swing.JPanel thresholdOptimizationTab;
    private javax.swing.JComboBox thresholdTypeCmb;
    private javax.swing.JButton totalTPHelpJButton;
    private javax.swing.JButton truePositivesHelpJButton;
    private javax.swing.JButton validateButton;
    private javax.swing.JButton validatedHitsHelpJButton;
    private javax.swing.JTextField windowTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * This method displays results on the panel
     */
    public void displayResults() {

        groupSelectionTable.setEnabled(true);
        confidenceSlider.setEnabled(true);
        fdrSlider1.setEnabled(true);
        fdrSlider2.setEnabled(true);
        sensitivitySlider1.setEnabled(true);
        sensitivitySlider2.setEnabled(true);

        // empty the group table
        while (groupSelectionTable.getRowCount() > 0) {
            ((DefaultTableModel) groupSelectionTable.getModel()).removeRow(0);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        ArrayList<String> peptideKeys = pSMaps.getPeptideSpecificMap().getKeys();
        HashMap<Integer, String> psmKeys = pSMaps.getPsmSpecificMap().getKeys();

        int cpt = 0;

        modifiedMaps.put(cpt, false);
        ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, "Proteins"});

        if (peptideKeys.size() == 1) {
            peptideMap.put(cpt, peptideKeys.get(0));
            modifiedMaps.put(cpt, false);
            ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, "Peptides"});
        } else {
            for (String pepitdeKey : peptideKeys) {
                peptideMap.put(cpt, pepitdeKey);
                modifiedMaps.put(cpt, false);

                if (pepitdeKey.equals("")) {
                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, "Not modified Peptides"});
                } else if (pepitdeKey.equals(PeptideSpecificMap.DUSTBIN)) {
                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, "Other Peptides"});
                } else {
                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, pepitdeKey + " Peptides"});
                }
            }
        }
        for (Integer psmKey : psmKeys.keySet()) {
            psmMap.put(cpt, psmKey);
            modifiedMaps.put(cpt, false);
            if (psmKeys.size() > 1) {
                ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, "Charge " + psmKeys.get(psmKey) + " PSMs"});
            } else {
                ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{++cpt, "PSMs"});
            }
        }

        if (groupSelectionTable.getRowCount() > 0) {
            groupSelectionTable.setRowSelectionInterval(0, 0);
        }

        groupSelectionChanged();

        // enable the contextual export options
        pepPlotExportJButton.setEnabled(true);
        fdrPlotExportJButton.setEnabled(true);
        confidencePlotExportJButton.setEnabled(true);
        fdrFnrPlotExportJButton.setEnabled(true);
        benefitPlotExportJButton.setEnabled(true);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Method called whenever a new group selection occurred
     */
    private void groupSelectionChanged() {
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        int selectedGroup = groupSelectionTable.getSelectedRow();

        if (selectedGroup == 0) {
            currentTargetDecoyMap = pSMaps.getProteinMap().getTargetDecoyMap();
            boolean found = false;
            // Verify that probabilities are up to date
            for (int key : psmMap.keySet()) {
                if (modifiedMaps.get(key)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                int outcome = JOptionPane.showConfirmDialog(this,
                        "Probabilities modifications at the PSM level will influence protein results.\n"
                        + "Recalculate probabilities?", "Apply Changes?", JOptionPane.YES_NO_OPTION);
                if (outcome == JOptionPane.YES_OPTION) {
                    recalculatePeptidesAndProteins();
                }
            } else {
                for (int key : peptideMap.keySet()) {
                    if (modifiedMaps.get(key)) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    int outcome = JOptionPane.showConfirmDialog(this,
                            "Probabilities modifications at the peptide level will influence protein results.\n"
                            + "Recalculate probabilities?", "Apply Changes", JOptionPane.YES_NO_OPTION);
                    if (outcome == JOptionPane.YES_OPTION) {
                        recalculateProteins();
                    }
                }
            }
        } else if (peptideMap.containsKey(selectedGroup)) {

            currentTargetDecoyMap = pSMaps.getPeptideSpecificMap().getTargetDecoyMap(peptideMap.get(selectedGroup));
            boolean found = false;

            for (int key : psmMap.keySet()) {
                if (modifiedMaps.get(key)) {
                    found = true;
                    break;
                }
            }

            if (found) {
                int outcome = JOptionPane.showConfirmDialog(this,
                        "Probabilities modifications at the PSM level will influence peptide results.\n"
                        + "Recalculate probabilities?", "Apply Changes?", JOptionPane.YES_NO_OPTION);
                if (outcome == JOptionPane.YES_OPTION) {
                    recalculatePeptidesAndProteins();
                }
            }
        } else if (psmMap.containsKey(selectedGroup)) {
            currentTargetDecoyMap = pSMaps.getPsmSpecificMap().getTargetDecoyMap(psmMap.get(selectedGroup));
        } else {
            // This should not happen
            clearScreen();
            return;
        }

        applyButton.setEnabled(modifiedMaps.get(selectedGroup));
        pepWindowApplied = !modifiedMaps.get(selectedGroup);

        nMaxTxt.setText(currentTargetDecoyMap.getnMax() + "");
        targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
        updateDisplayedComponents();
    }

    /**
     * Updates the displayed results whenever a new threshold is given.
     */
    private void updateResults() {

        if (currentTargetDecoyMap != null) {

            TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();

            if (lastThresholdType == 0) {
                currentResults.setConfidenceLimit(lastThreshold);
                targetDecoySeries.getConfidenceResults(currentResults);
            } else if (lastThresholdType == 1) {
                currentResults.setFdrLimit(lastThreshold);
                targetDecoySeries.getFDRResults(currentResults);
            } else if (lastThresholdType == 2) {
                currentResults.setFnrLimit(lastThreshold);
                targetDecoySeries.getFNRResults(currentResults);
            }
        }
    }

    /**
     * Updates the updates the different components of the gui with the new results.
     */
    private void updateDisplayedComponents() {

        if (currentTargetDecoyMap != null) {

            TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();
            nTotalTxt.setText(Util.roundDouble(currentResults.getnTPTotal(), 2) + "");
            nValidatedTxt.setText(Util.roundDouble(currentResults.getN(), 2) + "");
            nFPTxt.setText(Util.roundDouble(currentResults.getnFP(), 2) + "");
            nTPlTxt.setText(Util.roundDouble(currentResults.getnTP(), 2) + "");
            confidenceTxt.setText(Util.roundDouble(currentResults.getConfidenceLimit(), 2) + " %");
            fdrTxt.setText(Util.roundDouble(currentResults.getFdrLimit(), 2) + " %");
            fnrTxt.setText(Util.roundDouble(currentResults.getFnrLimit(), 2) + " %");
            confidenceSlider.setValue(currentResults.getConfidenceLimit().intValue());
            fdrSlider1.setValue(currentResults.getFdrLimit().intValue());
            fdrSlider2.setValue(currentResults.getFdrLimit().intValue());
            windowTxt.setText(currentTargetDecoyMap.getWindowSize() + "");
            Double newPosition = 50 * (Math.log10(currentTargetDecoyMap.getWindowSize() / (double) currentTargetDecoyMap.getnMax()) + 1);
            sensitivitySlider1.setValue(newPosition.intValue());
            thresholdTypeCmb.setSelectedIndex(lastThresholdType);
            thresholdInput.setText(lastThreshold + "");

            if (currentResults.isClassicalEstimators()) {
                fdrCombo1.setSelectedIndex(0);
            } else {
                fdrCombo1.setSelectedIndex(1);
            }

            updateCharts();
        }
    }

    /**
     * Updates the statistical charts.
     */
    private void updateCharts() {

        updatePepChart();
        updateFDRFNRChart();
        updateFDRsChart();
        updateConfidenceChart();
        updateCostBenefitChart();
        setMarkers();

        // find the smallest x-axis value used
        double[] scores = targetDecoySeries.getScores();
        double[] pep = targetDecoySeries.getPEP();
        double minScore = scores[0];
        double maxFDR = targetDecoySeries.getClassicalFDR()[pep.length - 1];

        for (double score : scores) {
            if (score > 0) {
                minScore = score;
                break;
            }
        }
        for (int index = pep.length - 1; index >= 0; index--) {
            if (pep[index] < 100) {
                maxFDR = Math.max(targetDecoySeries.getClassicalFDR()[index], targetDecoySeries.getProbaFDR()[index]);
                break;
            }
        }

        // set the lower range for the log axis and max for the fdr axis
        if (minScore > 0) {
            scoreAxis.setSmallestValue(minScore);
        }

        classicalAxis.setRange(0, maxFDR);
        probaAxis.setRange(0, maxFDR);

        confidencePanel.revalidate();
        confidencePanel.repaint();
        pepPanel.revalidate();
        pepPanel.repaint();
        fdrsPanel.revalidate();
        fdrsPanel.repaint();
        fdrFnrPanel.revalidate();
        fdrFnrPanel.repaint();
        benefitCostChartPanel.revalidate();
        benefitCostChartPanel.repaint();
    }

    /**
     * Sets the threshold marker.
     */
    private void setMarkers() {

        TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();

        confidenceMarker.setValue(currentResults.getScoreLimit());

        double[] score = {currentResults.getScoreLimit()};
        double[] fdr = {currentResults.getFdrLimit()};
        double[] fnr = {currentResults.getFnrLimit()};
        double[] benefit = {100 - currentResults.getFnrLimit()};

        DefaultXYDataset fdrData = new DefaultXYDataset();
        double[][] fdrSeries = {score, fdr};
        fdrData.addSeries("Retained FDR ", fdrSeries);
        fdrFnrPlot.setDataset(3, fdrData);
        fdrFnrPlot.mapDatasetToRangeAxis(3, 0);

        DefaultXYDataset probaFnrData = new DefaultXYDataset();
        double[][] probaFnrSeries = {score, fnr};
        probaFnrData.addSeries("Retained FNR ", probaFnrSeries);
        fdrFnrPlot.setDataset(4, probaFnrData);
        fdrFnrPlot.mapDatasetToRangeAxis(4, 0);

        XYLineAndShapeRenderer fdrRendrer = new XYLineAndShapeRenderer();
        fdrRendrer.setSeriesShapesVisible(0, true);
        fdrRendrer.setSeriesLinesVisible(0, false);
        fdrRendrer.setSeriesShape(0, DefaultDrawingSupplier.createStandardSeriesShapes()[1]);

        if (currentResults.isClassicalEstimators()) {
            fdrRendrer.setSeriesPaint(0, Color.blue);
        } else {
            fdrRendrer.setSeriesPaint(0, Color.green);
        }

        fdrFnrPlot.setRenderer(3, fdrRendrer);

        XYLineAndShapeRenderer probaFnrRendrer = new XYLineAndShapeRenderer();
        probaFnrRendrer.setSeriesShapesVisible(0, true);
        probaFnrRendrer.setSeriesLinesVisible(0, false);
        probaFnrRendrer.setSeriesPaint(0, Color.RED);
        probaFnrRendrer.setSeriesShape(0, DefaultDrawingSupplier.createStandardSeriesShapes()[2]);
        fdrFnrPlot.setRenderer(4, probaFnrRendrer);


        DefaultXYDataset benefitData = new DefaultXYDataset();
        double[][] benefitSeries = {fdr, benefit};
        benefitData.addSeries("Retained Benefit/Cost", benefitSeries);
        benefitCostPlot.setDataset(1, benefitData);
        benefitCostPlot.mapDatasetToRangeAxis(1, 0);

        XYLineAndShapeRenderer benefitRendrer = new XYLineAndShapeRenderer();
        benefitRendrer.setSeriesShapesVisible(0, true);
        benefitRendrer.setSeriesLinesVisible(0, false);
        benefitRendrer.setSeriesPaint(0, Color.blue);
        benefitRendrer.setSeriesShape(0, DefaultDrawingSupplier.createStandardSeriesShapes()[1]);
        benefitCostPlot.setRenderer(1, benefitRendrer);
    }

    /**
     * Updates the confidence chart.
     */
    private void updateConfidenceChart() {

        DefaultXYDataset confidenceData = new DefaultXYDataset();

        // get the x and y values for the plot
        double[] scores = targetDecoySeries.getScores();
        double[] confidences = targetDecoySeries.getConfidence();

        // extend the dataset by two elements to be able to make the vertical drop
        double[][] confidenceSeries = new double[2][scores.length + 2];
        double[][] tempSeries = new double[2][scores.length + 2];

        // get the location of the vertical red line
        double confidenceValue = confidenceMarker.getValue();

        int index = 0;

        // add all the values up to the red line
        while (index < scores.length && scores[index] < confidenceValue) {
            confidenceSeries[0][index] = scores[index];
            confidenceSeries[1][index] = confidences[index];
            tempSeries[0][index] = scores[index];
            tempSeries[1][index] = 100;
            index++;
        }

        if (index < scores.length) {

            double minorShift = 0.0000000001;

            // add the special cases surrounding the red line
            confidenceSeries[0][index] = confidenceValue - minorShift;
            confidenceSeries[1][index] = confidences[index];
            confidenceSeries[0][index + 1] = confidenceValue;
            confidenceSeries[1][index + 1] = confidences[index];
            confidenceSeries[0][index + 2] = confidenceValue + minorShift;
            confidenceSeries[1][index + 2] = confidences[index];

            tempSeries[0][index] = confidenceValue - minorShift;
            tempSeries[1][index] = 100;
            index++;
            tempSeries[0][index] = confidenceValue;
            tempSeries[1][index] = 50;
            index++;
            tempSeries[0][index] = confidenceValue + minorShift;
            tempSeries[1][index] = 0;
            index++;

            // add the values after the red line
            while (index < scores.length + 2) {

                confidenceSeries[0][index] = scores[index - 2];
                confidenceSeries[1][index] = confidences[index - 2];

                tempSeries[0][index] = scores[index - 2];
                tempSeries[1][index] = 0;
                index++;
            }
        }

        // add the series to the dataset
        confidenceData.addSeries("Confidence", confidenceSeries);
        confidenceData.addSeries("Area", tempSeries);
        confidencePlot.setDataset(0, confidenceData);

        // setup the renderer
        XYDifferenceRenderer confidenceRendrer = new XYDifferenceRenderer(fnrHighlightColor, fdrHighlightColor, false);
        confidenceRendrer.setSeriesPaint(0, Color.blue);
        confidenceRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        confidenceRendrer.setSeriesStroke(1, new BasicStroke(0));
        confidencePlot.setRenderer(0, confidenceRendrer);
        confidencePlot.setRenderer(1, confidenceRendrer);

        JFreeChart confidenceChart = new JFreeChart(confidencePlot);
        ChartPanel chartPanel = new ChartPanel(confidenceChart);
        confidenceChart.setTitle("Confidence");

        // remove the temp 'Area' dataset from the legend
        LegendItemCollection legendItemsOld = confidencePlot.getLegendItems();
        final LegendItemCollection legendItemsNew = new LegendItemCollection();
        legendItemsNew.add(legendItemsOld.get(0));

        LegendItemSource source = new LegendItemSource() {

            LegendItemCollection lic = new LegendItemCollection();

            {
                lic.addAll(legendItemsNew);
            }

            public LegendItemCollection getLegendItems() {
                return lic;
            }
        };

        // get the old legend frame
        BlockFrame tempFrame = confidenceChart.getLegend().getFrame();

        // remove the old legend
        confidenceChart.removeLegend();

        // add the new legend
        confidenceChart.addLegend(new LegendTitle(source));
        confidenceChart.getLegend().setFrame(tempFrame);
        confidenceChart.getLegend().setPosition(RectangleEdge.BOTTOM);

        // set background color
        confidenceChart.getPlot().setBackgroundPaint(Color.WHITE);
        confidenceChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        // add the chart to the panel
        confidenceChartPanel.removeAll();
        confidenceChartPanel.add(chartPanel);
        confidenceChartPanel.revalidate();
        confidenceChartPanel.repaint();
    }

    /**
     * Updates the pep chart.
     */
    private void updatePepChart() {

        DefaultXYDataset pepData = new DefaultXYDataset();
        double[][] pepSeries = {targetDecoySeries.getScores(), targetDecoySeries.getPEP()};
        pepData.addSeries("PEP", pepSeries);
        pepPlot.setDataset(0, pepData);
        pepPlot.mapDatasetToRangeAxis(0, 0);

        XYLineAndShapeRenderer pepRendrer = new XYLineAndShapeRenderer();
        pepRendrer.setSeriesShapesVisible(0, false);
        pepRendrer.setSeriesLinesVisible(0, true);
        pepRendrer.setSeriesPaint(0, Color.blue);
        pepRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        pepPlot.setRenderer(0, pepRendrer);

        JFreeChart pepChart = new JFreeChart(pepPlot);
        ChartPanel chartPanel = new ChartPanel(pepChart);
        pepChart.setTitle("PEP Estimation");

        // set background color
        pepChart.getPlot().setBackgroundPaint(Color.WHITE);
        pepChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        pepChartPanel.removeAll();
        pepChartPanel.add(chartPanel);
        pepChartPanel.revalidate();
        pepChartPanel.repaint();
    }

    /**
     * Updates the FDR estimators comparison chart.
     */
    private void updateFDRsChart() {
        DefaultXYDataset fdrsData = new DefaultXYDataset();
        double[][] fdrsSeries = {targetDecoySeries.getClassicalFDR(), targetDecoySeries.getProbaFDR()};
        fdrsData.addSeries("Probabilistic FDR", fdrsSeries);
        fdrPlot.setDataset(0, fdrsData);
        fdrPlot.mapDatasetToRangeAxis(0, 0);

        DefaultXYDataset refData = new DefaultXYDataset();
        double[][] refSeries = {targetDecoySeries.getClassicalFDR(), targetDecoySeries.getClassicalFDR()};
        refData.addSeries("x=y", refSeries);
        fdrPlot.setDataset(1, refData);
        fdrPlot.mapDatasetToRangeAxis(1, 0);

        XYLineAndShapeRenderer fdrsRendrer = new XYLineAndShapeRenderer();
        fdrsRendrer.setSeriesShapesVisible(0, false);
        fdrsRendrer.setSeriesLinesVisible(0, true);
        fdrsRendrer.setSeriesPaint(0, Color.blue);
        fdrsRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        fdrPlot.setRenderer(0, fdrsRendrer);

        XYLineAndShapeRenderer refRendrer = new XYLineAndShapeRenderer();
        refRendrer.setSeriesShapesVisible(0, false);
        refRendrer.setSeriesLinesVisible(0, true);
        refRendrer.setSeriesPaint(0, Color.black);
        refRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        fdrPlot.setRenderer(1, refRendrer);

        JFreeChart fdrChart = new JFreeChart(fdrPlot);
        ChartPanel chartPanel = new ChartPanel(fdrChart);
        fdrChart.setTitle("FDR Estimation");

        // set background color
        fdrChart.getPlot().setBackgroundPaint(Color.WHITE);
        fdrChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        fdrsChartPanel.removeAll();
        fdrsChartPanel.add(chartPanel);
        fdrsChartPanel.revalidate();
        fdrsChartPanel.repaint();
    }

    /**
     * Updates the FDR/FNR chart.
     */
    private void updateFDRFNRChart() {
        DefaultXYDataset classicalFdrData = new DefaultXYDataset();
        double[][] classicalFdrSeries = {targetDecoySeries.getScores(), targetDecoySeries.getClassicalFDR()};
        classicalFdrData.addSeries("Classical FDR ", classicalFdrSeries);
        fdrFnrPlot.setDataset(0, classicalFdrData);
        fdrFnrPlot.mapDatasetToRangeAxis(0, 0);

        DefaultXYDataset probaFdrData = new DefaultXYDataset();
        double[][] probaFdrSeries = {targetDecoySeries.getScores(), targetDecoySeries.getProbaFDR()};
        probaFdrData.addSeries("Probabilistic FDR ", probaFdrSeries);
        fdrFnrPlot.setDataset(1, probaFdrData);
        fdrFnrPlot.mapDatasetToRangeAxis(1, 0);

        DefaultXYDataset probaFnrData = new DefaultXYDataset();
        double[][] probaFnrSeries = {targetDecoySeries.getScores(), targetDecoySeries.getProbaFNR()};
        probaFnrData.addSeries("Probabilistic FNR ", probaFnrSeries);
        fdrFnrPlot.setDataset(2, probaFnrData);
        fdrFnrPlot.mapDatasetToRangeAxis(2, 0);

        XYLineAndShapeRenderer classicalFdrRendrer = new XYLineAndShapeRenderer();
        classicalFdrRendrer.setSeriesShapesVisible(0, false);
        classicalFdrRendrer.setSeriesLinesVisible(0, true);
        classicalFdrRendrer.setSeriesPaint(0, Color.blue);
        classicalFdrRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        fdrFnrPlot.setRenderer(0, classicalFdrRendrer);

        XYLineAndShapeRenderer probaFdrRendrer = new XYLineAndShapeRenderer();
        probaFdrRendrer.setSeriesShapesVisible(0, false);
        probaFdrRendrer.setSeriesLinesVisible(0, true);
        probaFdrRendrer.setSeriesPaint(0, Color.GREEN);
        probaFdrRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        fdrFnrPlot.setRenderer(1, probaFdrRendrer);

        XYLineAndShapeRenderer probaFnrRendrer = new XYLineAndShapeRenderer();
        probaFnrRendrer.setSeriesShapesVisible(0, false);
        probaFnrRendrer.setSeriesLinesVisible(0, true);
        probaFnrRendrer.setSeriesPaint(0, Color.RED);
        probaFnrRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        fdrFnrPlot.setRenderer(2, probaFnrRendrer);

        JFreeChart fdrChart = new JFreeChart(fdrFnrPlot);
        ChartPanel chartPanel = new ChartPanel(fdrChart);
        fdrChart.setTitle("FDR/FNR");

        // set background color
        fdrChart.getPlot().setBackgroundPaint(Color.WHITE);
        fdrChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        fdrFnrChartPanel.removeAll();
        fdrFnrChartPanel.add(chartPanel);
        fdrFnrChartPanel.revalidate();
        fdrFnrChartPanel.repaint();
    }

    /**
     * Updates the cost benefit chart.
     */
    private void updateCostBenefitChart() {
        DefaultXYDataset benefitData = new DefaultXYDataset();
        double[][] benefitSeries = {targetDecoySeries.getProbaFDR(), targetDecoySeries.getProbaBenefit()};
        benefitData.addSeries("Benefit", benefitSeries);
        benefitCostPlot.setDataset(0, benefitData);
        benefitCostPlot.mapDatasetToRangeAxis(0, 0);

        XYLineAndShapeRenderer benefitRendrer = new XYLineAndShapeRenderer();
        benefitRendrer.setSeriesShapesVisible(0, false);
        benefitRendrer.setSeriesLinesVisible(0, true);
        benefitRendrer.setSeriesPaint(0, Color.blue);
        benefitRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        benefitCostPlot.setRenderer(0, benefitRendrer);

        JFreeChart benefitCostChart = new JFreeChart(benefitCostPlot);
        ChartPanel chartPanel = new ChartPanel(benefitCostChart);
        benefitCostChart.setTitle("Benefit/Cost");

        // set background color
        benefitCostChart.getPlot().setBackgroundPaint(Color.WHITE);
        benefitCostChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        benefitCostChartPanel.removeAll();
        benefitCostChartPanel.add(chartPanel);
        benefitCostChartPanel.revalidate();
        benefitCostChartPanel.repaint();
    }

    /**
     * Removes the charts.
     */
    private void clearCharts() {
        confidenceChartPanel.removeAll();
        confidenceChartPanel.revalidate();
        confidenceChartPanel.repaint();
        fdrFnrChartPanel.removeAll();
        fdrFnrChartPanel.revalidate();
        fdrFnrChartPanel.repaint();
        benefitCostChartPanel.removeAll();
        benefitCostChartPanel.revalidate();
        benefitCostChartPanel.repaint();
    }

    /**
     * Clears the GUI.
     */
    private void clearScreen() {
        nTotalTxt.setText("");
        nValidatedTxt.setText("");
        nFPTxt.setText("");
        nTPlTxt.setText("");
        thresholdInput.setText("");
        confidenceTxt.setText("");
        fdrTxt.setText("");
        fnrTxt.setText("");
        nMaxTxt.setText("");
        clearCharts();
    }

    /**
     * Recalculates probabilities for peptides and proteins.
     */
    private void recalculatePeptidesAndProteins() {
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getExperiment(), peptideShakerGUI.getSample(), peptideShakerGUI.getReplicateNumber(), pSMaps);

        try {
            miniShaker.spectrumMapChanged();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An identification conflict occured. If you can reproduce the error please contact the developers.",
                    "Identification Conflict", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }

        for (int key : modifiedMaps.keySet()) {
            modifiedMaps.put(key, false);
        }
    }

    /**
     * Recalculates probabilities for proteins only.
     */
    private void recalculateProteins() {
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getExperiment(), peptideShakerGUI.getSample(), peptideShakerGUI.getReplicateNumber(), pSMaps);

        try {
            miniShaker.peptideMapChanged();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An identification conflict occured. If you can reproduce the error please contact the developers.",
                    "Identification Conflict", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }

        modifiedMaps.put(0, false);
        for (int key : peptideMap.keySet()) {
            modifiedMaps.put(key, false);
        }
    }

    /**
     * Apply the new protein settings.
     */
    private void applyProteins() {
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getExperiment(), peptideShakerGUI.getSample(), peptideShakerGUI.getReplicateNumber(), pSMaps);
        miniShaker.proteinMapChanged();
        modifiedMaps.put(0, false);
    }

    /**
     * Update the separators if the frame size changes.
     */
    public void updateSeparators() {
        formComponentResized(null);
    }

    /**
     * Reload the data to update validation status and scores.
     */
    private void reloadData() {
        peptideShakerGUI.reloadData();
    }

    /**
     * Returns true of the data has been reloaded with the currently selected threshold.
     * 
     * @return true of the data has been reloaded with the currently selected threshold
     */
    public boolean thresholdUpdated() {
        return dataValidated;
    }

    /**
     * Returns true of the data has been reloaded with the currently selected PEP window.
     * 
     * @return true of the data has been reloaded with the currently selected PEP window
     */
    public boolean pepWindowApplied() {
        return pepWindowApplied;
    }

    /**
     * Revalidates the data using the currently selected threshold.
     */
    public void revalidateData() {
        validateButtonActionPerformed(null);
    }

    /**
     * Reloads the data using the currently selected PEP window.
     */
    public void applyPepWindow() {
        applyButtonActionPerformed(null);
    }
}
