package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoySeries;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.maps.SpecificTargetDecoyMap;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ExtendedCategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockFrame;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;

/**
 * This panel displays statistical information about the dataset.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ValidationPanel extends javax.swing.JPanel {

    /**
     * It true the tab has been initiated, i.e., the data has been displayed at
     * least once. False means that the tab has to be loaded from scratch.
     */
    private boolean tabInitiated = false;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * If true the data has been (re-)loaded with the current threshold setting.
     */
    private boolean dataValidated = true;
    /**
     * If true the data has been (re-loaded) with the current PEP window size.
     */
    private boolean pepWindowApplied = true;
    /**
     * The main peptide shaker GUI.
     */
    private final PeptideShakerGUI peptideShakerGUI;
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
     * The PSMs map: # in the list &gt; map key.
     */
    private final HashMap<Integer, HashMap<Integer, String>> psmMap = new HashMap<>();
    /**
     * The peptide map: # in the list &gt; map key.
     */
    private final HashMap<Integer, String> peptideMap = new HashMap<>();
    /**
     * The confidence plot.
     */
    private final XYPlot confidencePlot = new XYPlot();
    /**
     * The target/decoy plot.
     */
    private CategoryPlot targetDecoyPlot = new CategoryPlot();
    /**
     * The benefit/cost plot.
     */
    private final XYPlot costBenefitPlot = new XYPlot();
    /**
     * The last threshold input.
     */
    private final HashMap<Integer, Double> lastThresholds = new HashMap<>();
    /**
     * The last threshold type 0 &gt; confidence 1 &gt; FDR 2 &gt; FNR
     */
    private final HashMap<Integer, Integer> lastThresholdTypes = new HashMap<>();
    /**
     * The original threshold input.
     */
    private final HashMap<Integer, Double> originalThresholds = new HashMap<>();
    /**
     * The original threshold type 0 &gt; confidence 1 &gt; FDR 2 &gt; FNR.
     */
    private final HashMap<Integer, Integer> originalThresholdTypes = new HashMap<>();
    /**
     * The confidence threshold marker.
     */
    private final ValueMarker confidenceMarker = new ValueMarker(1);
    /**
     * Map keeping track of probabilities modifications.
     */
    private final HashMap<Integer, Boolean> modifiedMaps = new HashMap<>();
    /**
     * The score log axis.
     */
    private final NumberAxis scoreAxis;
    /**
     * The highlighting to use for FNR.
     */
    private final Color fnrHighlightColor = new Color(0, 255, 0, 15);
    /**
     * The highlighting to use for FDR.
     */
    private final Color fdrHighlightColor = new Color(255, 0, 0, 15);

    /**
     * Create a new StatsPanel.
     *
     * @param parent the PeptideShaker parent frame.
     */
    public ValidationPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent;

        initComponents();

        // correct the color for the upper right corner
        JPanel groupSelectionCorner = new JPanel();
        groupSelectionCorner.setBackground(groupSelectionTable.getTableHeader().getBackground());
        groupSelectionScrollPaneScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, groupSelectionCorner);

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
        scoreAxis = new NumberAxis("Score");
        NumberAxis confidenceAxis = new NumberAxis("Confidence [%]");
        confidenceAxis.setAutoRangeIncludesZero(true);
        confidencePlot.setDomainAxis(scoreAxis);
        confidencePlot.setRangeAxis(0, confidenceAxis);
        confidencePlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        confidencePlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        confidenceMarker.setPaint(Color.red);
        confidenceMarker.setStroke(new BasicStroke(LINE_WIDTH));
        confidencePlot.addDomainMarker(confidenceMarker);

        // Initialize target/decoy plot
        // @TODO: do something here?
        // Initialize cost/benefit plot
        NumberAxis benefitAxis = new NumberAxis("Coverage (1-FNR) [%]");
        NumberAxis costAxis = new NumberAxis("False Discovery Rate (FDR) [%]");
        benefitAxis.setAutoRangeIncludesZero(true);
        costAxis.setAutoRangeIncludesZero(true);
        costBenefitPlot.setDomainAxis(costAxis);
        costBenefitPlot.setRangeAxis(0, benefitAxis);
        costBenefitPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
        costBenefitPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

        fdrCombo1.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        thresholdTypeCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        formComponentResized(null);
    }

    /**
     * Update the plot sizes.
     */
    public void updatePlotSizes() {
        formComponentResized(null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
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
        totalTpLabel = new javax.swing.JLabel();
        nTotalTxt = new javax.swing.JTextField();
        validatedHitsLabel = new javax.swing.JLabel();
        nValidatedTxt = new javax.swing.JTextField();
        fpLabel = new javax.swing.JLabel();
        tpLabel = new javax.swing.JLabel();
        nTPlTxt = new javax.swing.JTextField();
        nFPTxt = new javax.swing.JTextField();
        fdrLabel = new javax.swing.JLabel();
        fnrLabel = new javax.swing.JLabel();
        fdrTxt = new javax.swing.JTextField();
        fnrTxt = new javax.swing.JTextField();
        datasetPropertiesLabel = new javax.swing.JLabel();
        resolutionLabel = new javax.swing.JLabel();
        resolutionTxt = new javax.swing.JTextField();
        validationResultsLabel = new javax.swing.JLabel();
        confidenceLabel = new javax.swing.JLabel();
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
        thresholdOptimizationTab = new javax.swing.JPanel();
        leftPlotSplitPane = new javax.swing.JSplitPane();
        confidencePanel = new javax.swing.JPanel();
        confidencePlotLayeredPane = new javax.swing.JLayeredPane();
        confidenceChartPanel = new javax.swing.JPanel();
        confidencePlotHelpJButton = new javax.swing.JButton();
        confidencePlotExportJButton = new javax.swing.JButton();
        rightPlotSplitPane = new javax.swing.JSplitPane();
        fdrFnrPanel = new javax.swing.JPanel();
        fdrPlotLayeredPane = new javax.swing.JLayeredPane();
        targetDecoyChartPanel = new javax.swing.JPanel();
        fdrFnrPlotHelpJButton = new javax.swing.JButton();
        fdrFnrPlotExportJButton = new javax.swing.JButton();
        costBenefitPanel = new javax.swing.JPanel();
        costBenefitPlotLayeredPane = new javax.swing.JLayeredPane();
        costBenefitChartPanel = new javax.swing.JPanel();
        costBenefitPlotHelpJButton = new javax.swing.JButton();
        costBenefitPlotExportJButton = new javax.swing.JButton();
        parametersJPanel = new javax.swing.JPanel();
        thresholdInput = new javax.swing.JTextField();
        thresholdOptimizationLabel = new javax.swing.JLabel();
        validateButton = new javax.swing.JButton();
        advancedSettingsLabel = new javax.swing.JLabel();
        pepBinSizeLabel = new javax.swing.JLabel();
        windowTxt = new javax.swing.JTextField();
        applyButton = new javax.swing.JButton();
        fdrCombo1 = new javax.swing.JComboBox();
        thresholdTypeCmb = new javax.swing.JComboBox();
        thresholdLabel = new javax.swing.JLabel();
        thresholdHelpJButton = new javax.swing.JButton();
        thresholdTypeLabel = new javax.swing.JLabel();
        fdrMetricLabel = new javax.swing.JLabel();
        estimatorHelpJButton = new javax.swing.JButton();
        thresholdResetJButton = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        groupListJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Group Selection"));
        groupListJPanel.setMinimumSize(new java.awt.Dimension(200, 0));
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
        groupSelectionLayeredPane.add(groupSelectionHelpJButton);
        groupSelectionHelpJButton.setBounds(170, 130, 27, 25);
        groupSelectionLayeredPane.setLayer(groupSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        groupSelectionLayeredPane.add(groupSelectionScrollPaneScrollPane);
        groupSelectionScrollPaneScrollPane.setBounds(0, 0, 200, 170);

        javax.swing.GroupLayout groupListJPanelLayout = new javax.swing.GroupLayout(groupListJPanel);
        groupListJPanel.setLayout(groupListJPanelLayout);
        groupListJPanelLayout.setHorizontalGroup(
            groupListJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupListJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupSelectionLayeredPane)
                .addContainerGap())
        );
        groupListJPanelLayout.setVerticalGroup(
            groupListJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, groupListJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupSelectionLayeredPane)
                .addContainerGap())
        );

        idSummaryJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Summary"));
        idSummaryJPanel.setOpaque(false);

        totalTpLabel.setText("Total TP");
        totalTpLabel.setToolTipText("Total number of true positives");

        nTotalTxt.setEditable(false);
        nTotalTxt.setBackground(new java.awt.Color(245, 245, 245));
        nTotalTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nTotalTxt.setToolTipText("Total number of true positives");

        validatedHitsLabel.setText("# Validated Hits");
        validatedHitsLabel.setToolTipText("Number of validated hits");

        nValidatedTxt.setEditable(false);
        nValidatedTxt.setBackground(new java.awt.Color(245, 245, 245));
        nValidatedTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nValidatedTxt.setToolTipText("Number of validated hits");

        fpLabel.setText("# FP");
        fpLabel.setToolTipText("Number of false positives");

        tpLabel.setText("# TP");
        tpLabel.setToolTipText("Number of true positives");

        nTPlTxt.setEditable(false);
        nTPlTxt.setBackground(new java.awt.Color(245, 245, 245));
        nTPlTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nTPlTxt.setToolTipText("Number of true positives");

        nFPTxt.setEditable(false);
        nFPTxt.setBackground(new java.awt.Color(245, 245, 245));
        nFPTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nFPTxt.setToolTipText("Number of false positives");

        fdrLabel.setText("FDR");
        fdrLabel.setToolTipText("False Discovery Rate");

        fnrLabel.setText("FNR");
        fnrLabel.setToolTipText("False Negative Rate");

        fdrTxt.setEditable(false);
        fdrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fdrTxt.setToolTipText("False Discovery Rate");

        fnrTxt.setEditable(false);
        fnrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fnrTxt.setToolTipText("False Negative Rate");

        datasetPropertiesLabel.setFont(datasetPropertiesLabel.getFont().deriveFont((datasetPropertiesLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        datasetPropertiesLabel.setText("Dataset Properties");

        resolutionLabel.setText("Resolution");
        resolutionLabel.setToolTipText("Confidence estimation resolution");

        resolutionTxt.setEditable(false);
        resolutionTxt.setBackground(new java.awt.Color(245, 245, 245));
        resolutionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        resolutionTxt.setToolTipText("Confidence estimation resolution");

        validationResultsLabel.setFont(validationResultsLabel.getFont().deriveFont((validationResultsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        validationResultsLabel.setText("Validation Results");

        confidenceLabel.setText("Confidence");
        confidenceLabel.setToolTipText("Minimum Confidence");

        confidenceTxt.setEditable(false);
        confidenceTxt.setBackground(new java.awt.Color(245, 245, 245));
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
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(datasetPropertiesLabel)
                    .addComponent(validationResultsLabel)
                    .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(totalTpLabel)
                            .addComponent(validatedHitsLabel)
                            .addComponent(fpLabel)
                            .addComponent(tpLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(nValidatedTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(nTotalTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(nFPTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nTPlTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(validatedHitsHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(totalTPHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(falsePositivesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(truePositivesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(27, 27, 27)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(confidenceLabel)
                            .addComponent(resolutionLabel)
                            .addComponent(fdrLabel)
                            .addComponent(fnrLabel))
                        .addGap(9, 9, 9)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                                .addComponent(fnrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fnrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                                .addComponent(fdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fdrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(resolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(confidenceTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(confidenceHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nMaxHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        idSummaryJPanelLayout.setVerticalGroup(
            idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(datasetPropertiesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(totalTpLabel)
                    .addComponent(nTotalTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(totalTPHelpJButton)
                    .addComponent(resolutionLabel)
                    .addComponent(resolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nMaxHelpJButton))
                .addGap(18, 18, 18)
                .addComponent(validationResultsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(validatedHitsLabel)
                    .addComponent(nValidatedTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(validatedHitsHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(confidenceLabel)
                    .addComponent(confidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(confidenceHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(fpLabel)
                    .addComponent(nFPTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(falsePositivesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fdrLabel)
                    .addComponent(fdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fdrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(tpLabel)
                    .addComponent(nTPlTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(truePositivesHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fnrLabel)
                    .addComponent(fnrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fnrHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        idSummaryJPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {confidenceHelpJButton, confidenceTxt, falsePositivesHelpJButton, fdrHelpJButton, fdrTxt, fnrHelpJButton, fnrTxt, nFPTxt, nTPlTxt, nValidatedTxt, truePositivesHelpJButton, validatedHitsHelpJButton});

        optimizationJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Validation Plots"));
        optimizationJPanel.setOpaque(false);

        thresholdOptimizationTab.setBackground(new java.awt.Color(255, 255, 255));

        leftPlotSplitPane.setBorder(null);
        leftPlotSplitPane.setDividerLocation(leftPlotSplitPane.getWidth() / 3);
        leftPlotSplitPane.setDividerSize(0);
        leftPlotSplitPane.setResizeWeight(0.5);
        leftPlotSplitPane.setOpaque(false);

        confidencePanel.setOpaque(false);

        confidenceChartPanel.setOpaque(false);
        confidenceChartPanel.setLayout(new javax.swing.BoxLayout(confidenceChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        confidencePlotLayeredPane.add(confidenceChartPanel);
        confidenceChartPanel.setBounds(0, 0, 500, 460);

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
        confidencePlotLayeredPane.add(confidencePlotHelpJButton);
        confidencePlotHelpJButton.setBounds(480, 0, 10, 25);
        confidencePlotLayeredPane.setLayer(confidencePlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        confidencePlotLayeredPane.add(confidencePlotExportJButton);
        confidencePlotExportJButton.setBounds(460, 0, 10, 25);
        confidencePlotLayeredPane.setLayer(confidencePlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout confidencePanelLayout = new javax.swing.GroupLayout(confidencePanel);
        confidencePanel.setLayout(confidencePanelLayout);
        confidencePanelLayout.setHorizontalGroup(
            confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confidencePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(confidencePlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                .addContainerGap())
        );
        confidencePanelLayout.setVerticalGroup(
            confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confidencePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(confidencePlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
                .addContainerGap())
        );

        leftPlotSplitPane.setLeftComponent(confidencePanel);

        rightPlotSplitPane.setBorder(null);
        rightPlotSplitPane.setDividerLocation(rightPlotSplitPane.getWidth() / 2);
        rightPlotSplitPane.setDividerSize(0);
        rightPlotSplitPane.setResizeWeight(0.5);
        rightPlotSplitPane.setOpaque(false);

        fdrFnrPanel.setOpaque(false);

        targetDecoyChartPanel.setOpaque(false);
        targetDecoyChartPanel.setLayout(new javax.swing.BoxLayout(targetDecoyChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        fdrPlotLayeredPane.add(targetDecoyChartPanel);
        targetDecoyChartPanel.setBounds(0, 3, 320, 450);

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
        fdrPlotLayeredPane.add(fdrFnrPlotHelpJButton);
        fdrFnrPlotHelpJButton.setBounds(300, 10, 10, 25);
        fdrPlotLayeredPane.setLayer(fdrFnrPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        fdrPlotLayeredPane.add(fdrFnrPlotExportJButton);
        fdrFnrPlotExportJButton.setBounds(290, 10, 10, 25);
        fdrPlotLayeredPane.setLayer(fdrFnrPlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout fdrFnrPanelLayout = new javax.swing.GroupLayout(fdrFnrPanel);
        fdrFnrPanel.setLayout(fdrFnrPanelLayout);
        fdrFnrPanelLayout.setHorizontalGroup(
            fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrFnrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fdrPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                .addContainerGap())
        );
        fdrFnrPanelLayout.setVerticalGroup(
            fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fdrFnrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fdrPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
                .addContainerGap())
        );

        rightPlotSplitPane.setLeftComponent(fdrFnrPanel);

        costBenefitPanel.setOpaque(false);

        costBenefitChartPanel.setOpaque(false);
        costBenefitChartPanel.setLayout(new javax.swing.BoxLayout(costBenefitChartPanel, javax.swing.BoxLayout.LINE_AXIS));
        costBenefitPlotLayeredPane.add(costBenefitChartPanel);
        costBenefitChartPanel.setBounds(0, -4, 326, 450);

        costBenefitPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        costBenefitPlotHelpJButton.setToolTipText("Help");
        costBenefitPlotHelpJButton.setBorder(null);
        costBenefitPlotHelpJButton.setBorderPainted(false);
        costBenefitPlotHelpJButton.setContentAreaFilled(false);
        costBenefitPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        costBenefitPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                costBenefitPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                costBenefitPlotHelpJButtonMouseExited(evt);
            }
        });
        costBenefitPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                costBenefitPlotHelpJButtonActionPerformed(evt);
            }
        });
        costBenefitPlotLayeredPane.add(costBenefitPlotHelpJButton);
        costBenefitPlotHelpJButton.setBounds(300, 10, 10, 25);
        costBenefitPlotLayeredPane.setLayer(costBenefitPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        costBenefitPlotExportJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        costBenefitPlotExportJButton.setToolTipText("Export");
        costBenefitPlotExportJButton.setBorder(null);
        costBenefitPlotExportJButton.setBorderPainted(false);
        costBenefitPlotExportJButton.setContentAreaFilled(false);
        costBenefitPlotExportJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        costBenefitPlotExportJButton.setEnabled(false);
        costBenefitPlotExportJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        costBenefitPlotExportJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                costBenefitPlotExportJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                costBenefitPlotExportJButtonMouseExited(evt);
            }
        });
        costBenefitPlotExportJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                costBenefitPlotExportJButtonActionPerformed(evt);
            }
        });
        costBenefitPlotLayeredPane.add(costBenefitPlotExportJButton);
        costBenefitPlotExportJButton.setBounds(290, 10, 10, 25);
        costBenefitPlotLayeredPane.setLayer(costBenefitPlotExportJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout costBenefitPanelLayout = new javax.swing.GroupLayout(costBenefitPanel);
        costBenefitPanel.setLayout(costBenefitPanelLayout);
        costBenefitPanelLayout.setHorizontalGroup(
            costBenefitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, costBenefitPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(costBenefitPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                .addContainerGap())
        );
        costBenefitPanelLayout.setVerticalGroup(
            costBenefitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(costBenefitPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(costBenefitPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
                .addContainerGap())
        );

        rightPlotSplitPane.setRightComponent(costBenefitPanel);

        leftPlotSplitPane.setRightComponent(rightPlotSplitPane);

        javax.swing.GroupLayout thresholdOptimizationTabLayout = new javax.swing.GroupLayout(thresholdOptimizationTab);
        thresholdOptimizationTab.setLayout(thresholdOptimizationTabLayout);
        thresholdOptimizationTabLayout.setHorizontalGroup(
            thresholdOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(leftPlotSplitPane)
        );
        thresholdOptimizationTabLayout.setVerticalGroup(
            thresholdOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(leftPlotSplitPane)
        );

        javax.swing.GroupLayout optimizationJPanelLayout = new javax.swing.GroupLayout(optimizationJPanel);
        optimizationJPanel.setLayout(optimizationJPanelLayout);
        optimizationJPanelLayout.setHorizontalGroup(
            optimizationJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optimizationJPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(thresholdOptimizationTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(13, 13, 13))
        );
        optimizationJPanelLayout.setVerticalGroup(
            optimizationJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optimizationJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(thresholdOptimizationTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        thresholdOptimizationLabel.setFont(thresholdOptimizationLabel.getFont().deriveFont((thresholdOptimizationLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        thresholdOptimizationLabel.setText("Threshold Optimization");

        validateButton.setBackground(new java.awt.Color(0, 153, 0));
        validateButton.setForeground(new java.awt.Color(255, 255, 255));
        validateButton.setText("Apply");
        validateButton.setToolTipText("Apply the current thresholds");
        validateButton.setEnabled(false);
        validateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateButtonActionPerformed(evt);
            }
        });

        advancedSettingsLabel.setFont(advancedSettingsLabel.getFont().deriveFont((advancedSettingsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        advancedSettingsLabel.setText("Advanced Settings");

        pepBinSizeLabel.setText("PEP Bin Size");
        pepBinSizeLabel.setToolTipText("Posterior Error Probability window");

        windowTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        windowTxt.setToolTipText("Posterior Error Probability estimation bin size");
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

        applyButton.setBackground(new java.awt.Color(0, 153, 0));
        applyButton.setForeground(new java.awt.Color(255, 255, 255));
        applyButton.setText("Apply");
        applyButton.setToolTipText("Apply the current PEP window");
        applyButton.setEnabled(false);
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        fdrCombo1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Decoy", "PEP" }));
        fdrCombo1.setToolTipText("Estimator type");
        fdrCombo1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrCombo1ActionPerformed(evt);
            }
        });

        thresholdTypeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Confidence", "FDR", "FNR" }));
        thresholdTypeCmb.setToolTipText("Threshold type");
        thresholdTypeCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thresholdTypeCmbActionPerformed(evt);
            }
        });

        thresholdLabel.setText("Threshold (%)");
        thresholdLabel.setToolTipText("Threshold in percent");

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

        thresholdTypeLabel.setText("Type");

        fdrMetricLabel.setText("FDR Metric");

        estimatorHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        estimatorHelpJButton.setToolTipText("Help");
        estimatorHelpJButton.setBorder(null);
        estimatorHelpJButton.setBorderPainted(false);
        estimatorHelpJButton.setContentAreaFilled(false);
        estimatorHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        estimatorHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                estimatorHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                estimatorHelpJButtonMouseExited(evt);
            }
        });
        estimatorHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                estimatorHelpJButtonActionPerformed(evt);
            }
        });

        thresholdResetJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/reset_gray.png"))); // NOI18N
        thresholdResetJButton.setToolTipText("Reset to last applied settings");
        thresholdResetJButton.setBorder(null);
        thresholdResetJButton.setBorderPainted(false);
        thresholdResetJButton.setContentAreaFilled(false);
        thresholdResetJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/reset.png"))); // NOI18N
        thresholdResetJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                thresholdResetJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                thresholdResetJButtonMouseExited(evt);
            }
        });
        thresholdResetJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thresholdResetJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout parametersJPanelLayout = new javax.swing.GroupLayout(parametersJPanel);
        parametersJPanel.setLayout(parametersJPanelLayout);
        parametersJPanelLayout.setHorizontalGroup(
            parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(advancedSettingsLabel)
                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                            .addComponent(thresholdOptimizationLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(thresholdResetJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                            .addGap(10, 10, 10)
                            .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(parametersJPanelLayout.createSequentialGroup()
                                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(thresholdLabel)
                                        .addComponent(thresholdTypeLabel))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                                            .addComponent(thresholdInput, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(validateButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(thresholdTypeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(thresholdHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(parametersJPanelLayout.createSequentialGroup()
                                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(pepBinSizeLabel)
                                        .addComponent(fdrMetricLabel))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                                            .addComponent(windowTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(applyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(estimatorHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(fdrCombo1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        parametersJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {applyButton, fdrCombo1, thresholdInput, thresholdTypeCmb, validateButton, windowTxt});

        parametersJPanelLayout.setVerticalGroup(
            parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(thresholdOptimizationLabel)
                    .addComponent(thresholdResetJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(thresholdTypeLabel)
                    .addComponent(thresholdTypeCmb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(thresholdInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(thresholdLabel))
                    .addComponent(validateButton)
                    .addComponent(thresholdHelpJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(advancedSettingsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fdrMetricLabel)
                    .addComponent(fdrCombo1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(applyButton)
                            .addComponent(windowTxt)
                            .addComponent(pepBinSizeLabel))
                        .addGap(13, 13, 13))
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addComponent(estimatorHelpJButton)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        parametersJPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {thresholdInput, validateButton});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(optimizationJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(groupListJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idSummaryJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(parametersJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(parametersJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(idSummaryJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(groupListJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            int selectedGroup = groupSelectionTable.getSelectedRow();
            int thresholdType = thresholdTypeCmb.getSelectedIndex();
            double lastThreshold = new Double(thresholdInput.getText());
            applyThreshold(selectedGroup, lastThreshold, thresholdType);
            updateDisplayedComponents();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold.", "Threshold Error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_thresholdInputActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void windowTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_windowTxtActionPerformed
        try {
            Integer newWindow = new Integer(windowTxt.getText());

            if (newWindow < 0) {
                JOptionPane.showMessageDialog(this, "Please verify the given PEP bin size. Has to be a positive value.", "Window Error", JOptionPane.WARNING_MESSAGE);
            } else {
                currentTargetDecoyMap.setWindowSize(newWindow);

                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
                progressDialog.setTitle("Recalculating. Please Wait...");
                progressDialog.setPrimaryProgressCounterIndeterminate(false);

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            progressDialog.setVisible(true);
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    }
                }, "ProgressDialog").start();

                new Thread("RecalculateThread") {
                    @Override
                    public void run() {

                        currentTargetDecoyMap.estimateProbabilities(progressDialog);

                        if (!progressDialog.isRunCanceled()) {
                            targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
                            int thresholdType = thresholdTypeCmb.getSelectedIndex();
                            double lastThreshold = new Double(thresholdInput.getText());
                            updateResults(thresholdType, lastThreshold);
                            updateDisplayedComponents();
                            modifiedMaps.put(groupSelectionTable.getSelectedRow(), true);
                            applyButton.setEnabled(true);
                            pepWindowApplied = false;
                        }

                        progressDialog.setRunFinished();
                    }
                }.start();
            }
        } catch (Exception e) {
            if (currentTargetDecoyMap != null) {
                JOptionPane.showMessageDialog(this, "Please verify the given PEP bin size.", "Window Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_windowTxtActionPerformed

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

            peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
            peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
            peptideShakerGUI.setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
            peptideShakerGUI.setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
            peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
            peptideShakerGUI.setUpdated(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX, false);
            peptideShakerGUI.setDataSaved(false);

            applyButton.setEnabled(false);
            pepWindowApplied = true;

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_applyButtonActionPerformed

    /**
     * Resizes the plot sizes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        leftPlotSplitPane.setDividerLocation(leftPlotSplitPane.getWidth() / 3);
        rightPlotSplitPane.setDividerLocation(rightPlotSplitPane.getWidth() / 2);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                leftPlotSplitPane.setDividerLocation(leftPlotSplitPane.getWidth() / 3);
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
                costBenefitPlotLayeredPane.getComponent(0).setBounds(
                        costBenefitPlotLayeredPane.getWidth() - costBenefitPlotLayeredPane.getComponent(0).getWidth() - 10,
                        costBenefitPlotLayeredPane.getComponent(0).getHeight() / 2 - 12,
                        costBenefitPlotLayeredPane.getComponent(0).getWidth(),
                        costBenefitPlotLayeredPane.getComponent(0).getHeight());

                costBenefitPlotLayeredPane.getComponent(1).setBounds(
                        costBenefitPlotLayeredPane.getWidth() - costBenefitPlotLayeredPane.getComponent(0).getWidth() - 20,
                        costBenefitPlotLayeredPane.getComponent(1).getHeight() / 2 - 12,
                        costBenefitPlotLayeredPane.getComponent(1).getWidth(),
                        costBenefitPlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                costBenefitPlotLayeredPane.getComponent(2).setBounds(0, 0, costBenefitPlotLayeredPane.getWidth(), costBenefitPlotLayeredPane.getHeight());
                costBenefitPlotLayeredPane.revalidate();
                costBenefitPlotLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Validate the data using the current settings.
     *
     * @param evt
     */
    private void validateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateButtonActionPerformed

        double lastThreshold = new Double(thresholdInput.getText());

        thresholdInputActionPerformed(null);

        if (lastThreshold < 0 || lastThreshold > 100) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold. Interval: [0, 100].", "Threshold Error", JOptionPane.WARNING_MESSAGE);
        } else {

            progressDialog = new ProgressDialogX(peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setTitle("Recalculating. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(false);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("RecalculateThread") {
                @Override
                public void run() {

                    try {
                        PSMaps pSMaps = new PSMaps();
                        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);

                        MatchesValidator matchesValidator = new MatchesValidator(pSMaps.getPsmSpecificMap(), pSMaps.getPeptideSpecificMap(), pSMaps.getProteinMap());
                        matchesValidator.validateIdentifications(peptideShakerGUI.getIdentification(), peptideShakerGUI.getMetrics(), peptideShakerGUI.getGeneMaps(), pSMaps.getInputMap(), progressDialog, peptideShakerGUI.getExceptionHandler(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(), peptideShakerGUI.getSpectrumCountingParameters(), peptideShakerGUI.getProcessingPreferences());

                        progressDialog.setPrimaryProgressCounterIndeterminate(true);

                        if (!progressDialog.isRunCanceled()) {
                            // update the other tabs
                            peptideShakerGUI.getMetrics().setnValidatedProteins(-1);
                            peptideShakerGUI.getMetrics().setnConfidentProteins(-1);
                            peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
                            peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
                            peptideShakerGUI.setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
                            peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
                            peptideShakerGUI.setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
                            peptideShakerGUI.setUpdated(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX, false);
                            dataValidated = true;
                            validateButton.setEnabled(false);
                            double input = new Double(thresholdInput.getText());
                            int inputType = thresholdTypeCmb.getSelectedIndex();
                            TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();
                            currentResults.setUserInput(input);
                            currentResults.setInputType(inputType);
                            int selectedGroup = groupSelectionTable.getSelectedRow();
                            originalThresholds.put(selectedGroup, input);
                            originalThresholdTypes.put(selectedGroup, inputType);
                            peptideShakerGUI.setDataSaved(false);
                        } else {
                            // @TODO: ideally the validation settings ought to be reset as well..?
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }

                    progressDialog.setRunFinished();
                }
            }.start();
        }
    }//GEN-LAST:event_validateButtonActionPerformed

    /**
     * Opens a help dialog.
     *
     * @param evt
     */
    private void thresholdHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thresholdHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Validation_Parameters",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_thresholdHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     *
     * @param evt
     */
    private void costBenefitPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_costBenefitPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Coverage_Plot",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_costBenefitPlotHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     *
     * @param evt
     */
    private void fdrFnrPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrFnrPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Target_Decoy_Plot",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fdrFnrPlotHelpJButtonActionPerformed

    /**
     * Opens a help dialog.
     *
     * @param evt
     */
    private void confidencePlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confidencePlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Confidence_Plot",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_confidencePlotHelpJButtonActionPerformed

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
    private void costBenefitPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_costBenefitPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_costBenefitPlotHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void costBenefitPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_costBenefitPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_costBenefitPlotHelpJButtonMouseEntered

    /**
     * Update the threshold setting.
     *
     * @param evt
     */
    private void thresholdInputKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_thresholdInputKeyReleased
        try {
            double lastThreshold = new Double(thresholdInput.getText());

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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Group_Selection",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            groupSelectionChanged();
        }
    }//GEN-LAST:event_groupSelectionTableKeyReleased

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
        new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, confidenceChartPanel, peptideShakerGUI.getLastSelectedFolder());
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
        new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, targetDecoyChartPanel, peptideShakerGUI.getLastSelectedFolder());
    }//GEN-LAST:event_fdrFnrPlotExportJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void costBenefitPlotExportJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_costBenefitPlotExportJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_costBenefitPlotExportJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void costBenefitPlotExportJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_costBenefitPlotExportJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_costBenefitPlotExportJButtonMouseExited

    /**
     * Export the plot to figure format.
     *
     * @param evt
     */
    private void costBenefitPlotExportJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_costBenefitPlotExportJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, costBenefitChartPanel, peptideShakerGUI.getLastSelectedFolder());
    }//GEN-LAST:event_costBenefitPlotExportJButtonActionPerformed

    /**
     * Opens the help dialog.
     *
     * @param evt
     */
    private void confidenceHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confidenceHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
     * Opens the help dialog.
     *
     * @param evt
     */
    private void fdrHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
     * Opens the help dialog.
     *
     * @param evt
     */
    private void fnrHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fnrHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
     * Opens the help dialog.
     *
     * @param evt
     */
    private void truePositivesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_truePositivesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
     * Opens the help dialog.
     *
     * @param evt
     */
    private void falsePositivesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_falsePositivesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
     * Opens the help dialog.
     *
     * @param evt
     */
    private void validatedHitsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validatedHitsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
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
     * Opens the help dialog.
     *
     * @param evt
     */
    private void totalTPHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_totalTPHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Identification_Summary",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_totalTPHelpJButtonActionPerformed

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
    private void totalTPHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_totalTPHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_totalTPHelpJButtonMouseEntered

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void estimatorHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_estimatorHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_estimatorHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void estimatorHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_estimatorHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_estimatorHelpJButtonMouseExited

    /**
     * Opens the help dialog.
     *
     * @param evt
     */
    private void estimatorHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_estimatorHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/Validation.html"), "#Validation_Parameters",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Validation - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_estimatorHelpJButtonActionPerformed

    /**
     * Enable the validate button.
     *
     * @param evt
     */
    private void thresholdTypeCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thresholdTypeCmbActionPerformed
        validateButton.setEnabled(true);
    }//GEN-LAST:event_thresholdTypeCmbActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void thresholdResetJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_thresholdResetJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_thresholdResetJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void thresholdResetJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_thresholdResetJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_thresholdResetJButtonMouseExited

    /**
     * Reset all thresholds.
     *
     * @param evt
     */
    private void thresholdResetJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thresholdResetJButtonActionPerformed
        resetAllThresholds();
    }//GEN-LAST:event_thresholdResetJButtonActionPerformed

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

            applyButton.setEnabled(true);
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            int thresholdType = thresholdTypeCmb.getSelectedIndex();
            double lastThreshold = new Double(thresholdInput.getText());
            updateResults(thresholdType, lastThreshold);
            updateDisplayedComponents();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_fdrCombo1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel advancedSettingsLabel;
    private javax.swing.JButton applyButton;
    private javax.swing.JPanel confidenceChartPanel;
    private javax.swing.JButton confidenceHelpJButton;
    private javax.swing.JLabel confidenceLabel;
    private javax.swing.JPanel confidencePanel;
    private javax.swing.JButton confidencePlotExportJButton;
    private javax.swing.JButton confidencePlotHelpJButton;
    private javax.swing.JLayeredPane confidencePlotLayeredPane;
    private javax.swing.JTextField confidenceTxt;
    private javax.swing.JPanel costBenefitChartPanel;
    private javax.swing.JPanel costBenefitPanel;
    private javax.swing.JButton costBenefitPlotExportJButton;
    private javax.swing.JButton costBenefitPlotHelpJButton;
    private javax.swing.JLayeredPane costBenefitPlotLayeredPane;
    private javax.swing.JLabel datasetPropertiesLabel;
    private javax.swing.JButton estimatorHelpJButton;
    private javax.swing.JButton falsePositivesHelpJButton;
    private javax.swing.JComboBox fdrCombo1;
    private javax.swing.JPanel fdrFnrPanel;
    private javax.swing.JButton fdrFnrPlotExportJButton;
    private javax.swing.JButton fdrFnrPlotHelpJButton;
    private javax.swing.JButton fdrHelpJButton;
    private javax.swing.JLabel fdrLabel;
    private javax.swing.JLabel fdrMetricLabel;
    private javax.swing.JLayeredPane fdrPlotLayeredPane;
    private javax.swing.JTextField fdrTxt;
    private javax.swing.JButton fnrHelpJButton;
    private javax.swing.JLabel fnrLabel;
    private javax.swing.JTextField fnrTxt;
    private javax.swing.JLabel fpLabel;
    private javax.swing.JPanel groupListJPanel;
    private javax.swing.JButton groupSelectionHelpJButton;
    private javax.swing.JLayeredPane groupSelectionLayeredPane;
    private javax.swing.JScrollPane groupSelectionScrollPaneScrollPane;
    private javax.swing.JTable groupSelectionTable;
    private javax.swing.JPanel idSummaryJPanel;
    private javax.swing.JSplitPane leftPlotSplitPane;
    private javax.swing.JTextField nFPTxt;
    private javax.swing.JButton nMaxHelpJButton;
    private javax.swing.JTextField nTPlTxt;
    private javax.swing.JTextField nTotalTxt;
    private javax.swing.JTextField nValidatedTxt;
    private javax.swing.JPanel optimizationJPanel;
    private javax.swing.JPanel parametersJPanel;
    private javax.swing.JLabel pepBinSizeLabel;
    private javax.swing.JLabel resolutionLabel;
    private javax.swing.JTextField resolutionTxt;
    private javax.swing.JSplitPane rightPlotSplitPane;
    private javax.swing.JPanel targetDecoyChartPanel;
    private javax.swing.JButton thresholdHelpJButton;
    private javax.swing.JTextField thresholdInput;
    private javax.swing.JLabel thresholdLabel;
    private javax.swing.JLabel thresholdOptimizationLabel;
    private javax.swing.JPanel thresholdOptimizationTab;
    private javax.swing.JButton thresholdResetJButton;
    private javax.swing.JComboBox thresholdTypeCmb;
    private javax.swing.JLabel thresholdTypeLabel;
    private javax.swing.JButton totalTPHelpJButton;
    private javax.swing.JLabel totalTpLabel;
    private javax.swing.JLabel tpLabel;
    private javax.swing.JButton truePositivesHelpJButton;
    private javax.swing.JButton validateButton;
    private javax.swing.JButton validatedHitsHelpJButton;
    private javax.swing.JLabel validatedHitsLabel;
    private javax.swing.JLabel validationResultsLabel;
    private javax.swing.JTextField windowTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * This method displays results on the panel.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Updating Validation Data. Please Wait...");

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

                groupSelectionTable.setEnabled(true);

                // empty the group table
                DefaultTableModel dm = (DefaultTableModel) groupSelectionTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                PSMaps pSMaps = new PSMaps();
                pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);

                int cpt = 0;

                modifiedMaps.put(cpt, false);
                ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{cpt + 1, "Proteins"});
                TargetDecoyMap targetDecoyMap = pSMaps.getProteinMap().getTargetDecoyMap();
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                originalThresholdTypes.put(cpt, targetDecoyResults.getInputType());
                originalThresholds.put(cpt, targetDecoyResults.getUserInput());

                ArrayList<String> peptideKeys = pSMaps.getPeptideSpecificMap().getKeys();
                if (peptideKeys.size() == 1) {
                    String key = peptideKeys.get(0);
                    peptideMap.put(++cpt, key);
                    modifiedMaps.put(cpt, false);
                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{cpt + 1, "Peptides"});
                    targetDecoyMap = pSMaps.getPeptideSpecificMap().getTargetDecoyMap(key);
                    targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                    originalThresholdTypes.put(cpt, targetDecoyResults.getInputType());
                    originalThresholds.put(cpt, targetDecoyResults.getUserInput());
                } else {
                    for (String peptideKey : peptideKeys) {

                        if (progressDialog.isRunCanceled()) {
                            break;
                        }

                        peptideMap.put(++cpt, peptideKey);
                        modifiedMaps.put(cpt, false);

                        String title = PeptideSpecificMap.getKeyName(peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationParameters(), peptideKey);
                        ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{cpt + 1, "Peptides (" + title + ")"});
                    }
                }

                SpecificTargetDecoyMap psmSpecificMap = pSMaps.getPsmSpecificMap();
                ArrayList<Integer> foundCharges = new ArrayList<>(4);
                for (Integer charge : psmSpecificMap.getPossibleCategories()) {
                    for (String file : psmSpecificMap.getFilesAtCategory(charge)) {
                        if (!psmSpecificMap.isFileGrouped(charge, file)) {
                            foundCharges.add(charge);
                            if (progressDialog.isRunCanceled()) {
                                break;
                            }
                            HashMap<Integer, String> psmKey = new HashMap<>();
                            psmKey.put(charge, file);
                            psmMap.put(++cpt, psmKey);
                            modifiedMaps.put(cpt, false);
                            targetDecoyMap = pSMaps.getPsmSpecificMap().getTargetDecoyMap(charge, file);
                            targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                            originalThresholdTypes.put(cpt, targetDecoyResults.getInputType());
                            originalThresholds.put(cpt, targetDecoyResults.getUserInput());
                        }
                    }
                }
                HashMap<Integer, ArrayList<Integer>> groupedCharges = new HashMap<>();
                for (int charge : psmSpecificMap.getCategoriesFromGroupedFiles()) {
                    int correctedCharge = psmSpecificMap.getCorrectedCharge(charge);
                    if (correctedCharge == charge) {
                        HashMap<Integer, String> psmKey = new HashMap<>();
                        psmKey.put(charge, null);
                        psmMap.put(++cpt, psmKey);
                        modifiedMaps.put(cpt, false);
                        targetDecoyMap = pSMaps.getPsmSpecificMap().getTargetDecoyMap(charge, null);
                        targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                        originalThresholdTypes.put(cpt, targetDecoyResults.getInputType());
                        originalThresholds.put(cpt, targetDecoyResults.getUserInput());
                    } else {
                        ArrayList<Integer> charges = groupedCharges.get(correctedCharge);
                        if (charges == null) {
                            charges = new ArrayList<>(4);
                            groupedCharges.put(correctedCharge, charges);
                        }
                        charges.add(charge);
                    }
                }

                if (psmMap.size() > 1) {
                    for (int index : psmMap.keySet()) {
                        for (int charge : psmMap.get(index).keySet()) {
                            String file = psmMap.get(index).get(charge);
                            if (file != null) {
                                if (peptideShakerGUI.getIdentification().getSpectrumFiles().size() > 1) {
                                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{index + 1, "PSMs (Charge " + charge + " of file " + file + ")"});
                                } else {
                                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{index + 1, "PSMs (Charge " + charge + ")"});
                                }
                            } else if (foundCharges.contains(charge)) {
                                if (groupedCharges.containsKey(charge)) {
                                    ArrayList<Integer> groupCharges = groupedCharges.get(charge);
                                    Collections.sort(groupCharges);
                                    String chargeTxt = "PSMs (Other Charge " + charge + " and Charge ";
                                    for (int subCharge : groupCharges) {
                                        if (!chargeTxt.equals("")) {
                                            chargeTxt += ", ";
                                        }
                                        chargeTxt += subCharge;
                                    }
                                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{index + 1, chargeTxt + ")"});
                                } else {
                                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{index + 1, "PSMs (Other Charge " + charge + ")"});
                                }
                            } else {
                                ArrayList<Integer> groupCharges = new ArrayList<>();
                                groupCharges.add(charge);
                                if (groupedCharges.containsKey(charge)) {
                                    groupCharges.addAll(groupedCharges.get(charge));
                                }
                                Collections.sort(groupCharges);
                                String chargeTxt = "";
                                for (int subCharge : groupCharges) {
                                    if (!chargeTxt.equals("")) {
                                        chargeTxt += ", ";
                                    }
                                    chargeTxt += subCharge;
                                }
                                ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{index + 1, "PSMs (Charge " + chargeTxt + ")"});
                            }
                        }
                    }
                } else {
                    ((DefaultTableModel) groupSelectionTable.getModel()).addRow(new Object[]{cpt, "PSMs"});
                }

                if (groupSelectionTable.getRowCount() > 0) {
                    groupSelectionTable.setRowSelectionInterval(0, 0);
                }

                if (!progressDialog.isRunCanceled()) {
                    groupSelectionChanged();

                    // enable the contextual export options
                    confidencePlotExportJButton.setEnabled(true);
                    fdrFnrPlotExportJButton.setEnabled(true);
                    costBenefitPlotExportJButton.setEnabled(true);

                    tabInitiated = true;
                }

                progressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Returns the target decoy map corresponding to the given group selection.
     *
     * @param selectedGroup the index of the group of interest
     *
     * @return the corresponding target/decoy map
     */
    private TargetDecoyMap getTargetDecoyMap(int selectedGroup) {

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);

        if (selectedGroup == 0) {
            return pSMaps.getProteinMap().getTargetDecoyMap();
        } else if (peptideMap.containsKey(selectedGroup)) {

            return pSMaps.getPeptideSpecificMap().getTargetDecoyMap(peptideMap.get(selectedGroup));
        } else if (psmMap.containsKey(selectedGroup)) {
            HashMap<Integer, String> psmKey = psmMap.get(selectedGroup);
            for (int charge : psmKey.keySet()) {
                return pSMaps.getPsmSpecificMap().getTargetDecoyMap(charge, psmKey.get(charge));
            }
        }
        throw new IllegalArgumentException("Target decoy map not found for selection " + selectedGroup + ".");
    }

    /**
     * Method called whenever a new group selection occurred.
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
            HashMap<Integer, String> psmKey = psmMap.get(selectedGroup);
            for (int charge : psmKey.keySet()) {
                currentTargetDecoyMap = pSMaps.getPsmSpecificMap().getTargetDecoyMap(charge, psmKey.get(charge));
            }
        } else {
            // this should not happen...
            clearScreen();
            return;
        }

        applyButton.setEnabled(modifiedMaps.get(selectedGroup));
        pepWindowApplied = !modifiedMaps.get(selectedGroup);
        double pmin = currentTargetDecoyMap.getResolution();
        resolutionTxt.setText(Util.roundDouble(pmin, 2) + " %");
        targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
        updateDisplayedComponents();
    }

    /**
     * Updates the displayed results whenever a new threshold is given.
     */
    private void updateResults(int thresholdType, double threshold) {

        if (currentTargetDecoyMap != null) {

            TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();

            switch (thresholdType) {
                case 0:
                    currentResults.setConfidenceLimit(threshold);
                    targetDecoySeries.getConfidenceResults(currentResults);
                    break;
                case 1:
                    currentResults.setFdrLimit(threshold);
                    targetDecoySeries.getFDRResults(currentResults);
                    break;
                case 2:
                    currentResults.setFnrLimit(threshold);
                    targetDecoySeries.getFNRResults(currentResults);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Updates the updates the different components of the GUI with the new
     * results.
     */
    private void updateDisplayedComponents() {

        if (currentTargetDecoyMap != null) {

            TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();
            int selectedGroup = groupSelectionTable.getSelectedRow();
            if (!lastThresholds.containsKey(selectedGroup)) {
                lastThresholds.put(selectedGroup, currentResults.getUserInput());
                lastThresholdTypes.put(selectedGroup, currentResults.getInputType());
            }
            nTotalTxt.setText(Util.roundDouble(currentResults.getnTPTotal(), 2) + "");
            nValidatedTxt.setText(Util.roundDouble(currentResults.getN(), 2) + "");
            nFPTxt.setText(Util.roundDouble(currentResults.getnFP(), 2) + "");
            nTPlTxt.setText(Util.roundDouble(currentResults.getnTP(), 2) + "");
            confidenceTxt.setText(Util.roundDouble(currentResults.getConfidenceLimit(), 2) + " %");
            fdrTxt.setText(Util.roundDouble(currentResults.getFdrLimit(), 2) + " %");
            fnrTxt.setText(Util.roundDouble(currentResults.getFnrLimit(), 2) + " %");
            windowTxt.setText(currentTargetDecoyMap.getWindowSize() + "");
            thresholdTypeCmb.setSelectedIndex(lastThresholdTypes.get(selectedGroup));
            thresholdInput.setText(lastThresholds.get(selectedGroup) + "");

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

        updateConfidenceChart();
        updateTargteDecoyChart();
        updateCostBenefitChart();
        setMarkers();

        // find the smallest x-axis value used
        double[] scores = targetDecoySeries.getScores();
        double minScore = scores[0];

        for (double score : scores) {
            if (score > 0) {
                minScore = score;
                break;
            }
        }

        // set the lower range for the log axis
        if (minScore > 0) {
//            scoreAxis.setSmallestValue(minScore);
//            scoreAxisTargetDecoy.setSmallestValue(minScore);
        }

        confidencePanel.revalidate();
        confidencePanel.repaint();
        fdrFnrPanel.revalidate();
        fdrFnrPanel.repaint();
        costBenefitChartPanel.revalidate();
        costBenefitChartPanel.repaint();
    }

    /**
     * Sets the threshold marker.
     */
    private void setMarkers() {

        TargetDecoyResults currentResults = currentTargetDecoyMap.getTargetDecoyResults();

        confidenceMarker.setValue(currentResults.getLogScoreLimit());

        double[] fdr = {currentResults.getFdrLimit()};
        double[] benefit = {100 - currentResults.getFnrLimit()};

        DefaultXYDataset benefitData = new DefaultXYDataset();
        double[][] benefitSeries = {fdr, benefit};
        benefitData.addSeries("Threshold", benefitSeries);
        costBenefitPlot.setDataset(0, benefitData);
        costBenefitPlot.mapDatasetToRangeAxis(0, 0);

        XYLineAndShapeRenderer benefitRendrer = new XYLineAndShapeRenderer();
        benefitRendrer.setSeriesShapesVisible(0, true);
        benefitRendrer.setSeriesLinesVisible(0, false);
        benefitRendrer.setSeriesPaint(0, Color.RED);
        benefitRendrer.setSeriesShape(0, DefaultDrawingSupplier.createStandardSeriesShapes()[1]);
        benefitRendrer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        costBenefitPlot.setRenderer(0, benefitRendrer);
    }

    /**
     * Updates the confidence chart.
     */
    private void updateConfidenceChart() {

        DefaultXYDataset confidenceData = new DefaultXYDataset();

        // get the x and y values for the plot
        double[] scores = targetDecoySeries.getScoresLog();
        double[] confidences = targetDecoySeries.getConfidenceLog();

        // test for valid values
        boolean enoughData = scores.length > 2;

        if (!enoughData) {
            // clear the chart
            confidenceChartPanel.removeAll();
            confidenceChartPanel.revalidate();
            confidenceChartPanel.repaint();
        } else {

            // extend the dataset by two elements to be able to make the vertical drop
            double[][] confidenceSeries = new double[2][scores.length + 2];
            double[][] tempSeries = new double[2][scores.length + 2];

            // get the location of the vertical red line
            double confidenceValue = confidenceMarker.getValue();

            int index = 0;
            double minorShift = 0.0000000001;

            // add all the values up to the red line
            while (index < scores.length && scores[index] < confidenceValue) {

                double currentScore = scores[index];
                if (index > 0) {
                    double previousScore = scores[index - 1];
                    if (Double.compare(previousScore, currentScore) == 0) {
                        currentScore += minorShift; // avoid that two consecutive x-values are identical as the XYDifferenceRenderer does not like this
                    }
                }

                confidenceSeries[0][index] = currentScore;
                confidenceSeries[1][index] = confidences[index];
                tempSeries[0][index] = currentScore;
                tempSeries[1][index] = 0;
                index++;
            }

            if (index < scores.length) {

                // add the special cases surrounding the red line
                confidenceSeries[0][index] = confidenceValue - minorShift;
                confidenceSeries[1][index] = confidences[index];
                confidenceSeries[0][index + 1] = confidenceValue;
                confidenceSeries[1][index + 1] = confidences[index];
                confidenceSeries[0][index + 2] = confidenceValue + minorShift;
                confidenceSeries[1][index + 2] = confidences[index];

                tempSeries[0][index] = confidenceValue - minorShift;
                tempSeries[1][index] = 0;
                index++;
                tempSeries[0][index] = confidenceValue;
                tempSeries[1][index] = 50;
                index++;
                tempSeries[0][index] = confidenceValue + minorShift;
                tempSeries[1][index] = 100;
                index++;

                // add the values after the red line
                while (index < scores.length + 2) {

                    double currentScore = scores[index - 2];
                    double previousScore = scores[index - 3];
                    if (Double.compare(previousScore, currentScore) == 0) {
                        currentScore += minorShift; // avoid that two consecutive x-values are identical as the XYDifferenceRenderer does not like this
                    }

                    confidenceSeries[0][index] = currentScore;
                    confidenceSeries[1][index] = confidences[index - 2];
                    tempSeries[0][index] = currentScore;
                    tempSeries[1][index] = 100;
                    index++;
                }
            }

            // add the series to the dataset
            confidenceData.addSeries("Confidence", confidenceSeries);
            confidenceData.addSeries("Area", tempSeries);
            confidencePlot.setDataset(0, confidenceData);

            // set up the renderer
            XYDifferenceRenderer confidenceRendrer = new XYDifferenceRenderer(fnrHighlightColor, fdrHighlightColor, false);
            confidenceRendrer.setSeriesPaint(0, Color.blue);
            confidenceRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
            confidenceRendrer.setSeriesStroke(1, new BasicStroke(0));
            confidenceRendrer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
            confidencePlot.setRenderer(confidenceRendrer);

            // set up the chart
            JFreeChart confidenceChart = new JFreeChart(confidencePlot);
            ChartPanel chartPanel = new ChartPanel(confidenceChart);
            confidenceChart.setTitle("Score vs. Confidence");

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
    }

    /**
     * Updates the Target/Decoy chart.
     */
    private void updateTargteDecoyChart() {

        DefaultCategoryDataset targetDecoyDataset = new DefaultCategoryDataset();

        // get the histogram data
        double[] bins = targetDecoySeries.getTdBins();
        double[] nTarget = targetDecoySeries.getnTarget();
        double[] nDecoy = targetDecoySeries.getnDecoy();

        String[] categories = new String[bins.length];

        for (int i = 0; i < bins.length; i++) {
            double tempScore = bins[i];
            String category = ((int) (Math.round(tempScore))) + "";
            categories[i] = category;
            targetDecoyDataset.addValue(nTarget[i], "Target", categories[i]);
            targetDecoyDataset.addValue(nDecoy[i], "Decoy", categories[i]);
        }

        // prepare for hiding every second category label
        final String[] extendedCategories = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            if (i % 2 == 0) {
                extendedCategories[i] = categories[i];
            } else {
                extendedCategories[i] = " ";
            }
        }

        // create the plot
        targetDecoyPlot = new CategoryPlot(targetDecoyDataset, new CategoryAxis("Score"), new NumberAxis("Frequency"), new BarRenderer());

        // hide every second category label
        final ExtendedCategoryAxis extendedCategoryAxis = new ExtendedCategoryAxis("Score");
        for (int i = 0; i < categories.length; i++) {
            extendedCategoryAxis.addSubLabel(categories[i], extendedCategories[i]);
        }
        Font theFont = extendedCategoryAxis.getTickLabelFont();
        extendedCategoryAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 0));
        extendedCategoryAxis.setSubLabelFont(theFont);
        targetDecoyPlot.setDomainAxis(extendedCategoryAxis);

        // remove the space before and after the plots on the x-axis
        targetDecoyPlot.getDomainAxis().setLowerMargin(0.0);
        targetDecoyPlot.getDomainAxis().setUpperMargin(0.0);

        // create the chart
        JFreeChart targetDecoyChart = new JFreeChart(targetDecoyPlot);

        // set the renderer
        BarRenderer renderer = new BarRenderer();
        //StackedBarRenderer renderer = new StackedBarRenderer();
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, new Color(110, 196, 97, 225));
        renderer.setSeriesPaint(1, new Color(255, 0, 0));
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        renderer.setItemMargin(0.0);
        ((CategoryPlot) targetDecoyChart.getPlot()).setRenderer(renderer);
        CategoryAxis axis = targetDecoyPlot.getDomainAxis();
        //axis.setCategoryMargin(0);

        // add the confidence marker
        double confidenceValue = confidenceMarker.getValue();

        for (String categorie : categories) {
            int value = Integer.parseInt(categorie);
            if (value < confidenceValue) {
                targetDecoyPlot.addDomainMarker(new CategoryMarker(categorie, fdrHighlightColor, new BasicStroke(4)), Layer.BACKGROUND);
            } else {
                targetDecoyPlot.addDomainMarker(new CategoryMarker(categorie, fnrHighlightColor, new BasicStroke(4)), Layer.BACKGROUND);
            }
        }

        // set the chart title
        ChartPanel chartPanel = new ChartPanel(targetDecoyChart);
        targetDecoyChart.setTitle("Target vs. Decoy");

        // set background color
        targetDecoyChart.getPlot().setBackgroundPaint(Color.WHITE);
        targetDecoyChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        targetDecoyChartPanel.removeAll();
        targetDecoyChartPanel.add(chartPanel);
        targetDecoyChartPanel.revalidate();
        targetDecoyChartPanel.repaint();
    }

    /**
     * Updates the cost benefit chart.
     */
    private void updateCostBenefitChart() {
        DefaultXYDataset benefitData = new DefaultXYDataset();
        double[][] benefitSeries = {targetDecoySeries.getProbaFDR(), targetDecoySeries.getProbaBenefit()};
        benefitData.addSeries("Possible Coverage", benefitSeries);
        costBenefitPlot.setDataset(1, benefitData);
        costBenefitPlot.mapDatasetToRangeAxis(1, 0);

        XYLineAndShapeRenderer benefitRendrer = new XYLineAndShapeRenderer();
        benefitRendrer.setSeriesShapesVisible(0, false);
        benefitRendrer.setSeriesLinesVisible(0, true);
        benefitRendrer.setSeriesPaint(0, Color.blue);
        benefitRendrer.setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        benefitRendrer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        costBenefitPlot.setRenderer(1, benefitRendrer);

        JFreeChart benefitCostChart = new JFreeChart(costBenefitPlot);
        ChartPanel chartPanel = new ChartPanel(benefitCostChart);
        benefitCostChart.setTitle("FDR vs. Coverage");

        // set background color
        benefitCostChart.getPlot().setBackgroundPaint(Color.WHITE);
        benefitCostChart.setBackgroundPaint(Color.WHITE);
        chartPanel.setBackground(Color.WHITE);

        costBenefitChartPanel.removeAll();
        costBenefitChartPanel.add(chartPanel);
        costBenefitChartPanel.revalidate();
        costBenefitChartPanel.repaint();
    }

    /**
     * Removes the charts.
     */
    private void clearCharts() {
        confidenceChartPanel.removeAll();
        confidenceChartPanel.revalidate();
        confidenceChartPanel.repaint();
        targetDecoyChartPanel.removeAll();
        targetDecoyChartPanel.revalidate();
        targetDecoyChartPanel.repaint();
        costBenefitChartPanel.removeAll();
        costBenefitChartPanel.revalidate();
        costBenefitChartPanel.repaint();
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
        resolutionTxt.setText("");
        clearCharts();
    }

    /**
     * Recalculates probabilities for peptides and proteins.
     */
    private void recalculatePeptidesAndProteins() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setTitle("Recalculating. Please Wait...");
        progressDialog.setPrimaryProgressCounterIndeterminate(false);

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("RecalculateThread") {
            @Override
            public void run() {

                PSMaps pSMaps = new PSMaps();
                pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
                PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getProjectParameters(), pSMaps);

                try {
                    miniShaker.spectrumMapChanged(peptideShakerGUI.getIdentification(), progressDialog, peptideShakerGUI.getProcessingPreferences(), peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(peptideShakerGUI, JOptionEditorPane.getJOptionEditorPane(
                            "An identification conflict occured. If you can reproduce the error <br>"
                            + "Please <a href=\"https://github.com/compomics/peptide-shaker/issues\">contact the developers</a>."),
                            "Identification Conflict", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }

                // update the tracking of probabilities modifications
                for (int key : modifiedMaps.keySet()) {
                    modifiedMaps.put(key, false);
                }

                progressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Recalculates probabilities for proteins only.
     */
    private void recalculateProteins() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setTitle("Recalculating. Please Wait...");
        progressDialog.setPrimaryProgressCounterIndeterminate(false);

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("RecalculateThread") {
            @Override
            public void run() {

                PSMaps pSMaps = new PSMaps();
                pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
                PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getProjectParameters(), pSMaps);

                try {
                    miniShaker.peptideMapChanged(peptideShakerGUI.getIdentification(), progressDialog, peptideShakerGUI.getIdentificationParameters());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(peptideShakerGUI, JOptionEditorPane.getJOptionEditorPane(
                            "An identification conflict occured. If you can reproduce the error <br>"
                            + "Please <a href=\"https://github.com/compomics/peptide-shaker/issues\">contact the developers</a>."),
                            "Identification Conflict", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }

                modifiedMaps.put(0, false);
                for (int key : peptideMap.keySet()) {
                    modifiedMaps.put(key, false);
                }

                progressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Apply the new protein settings.
     */
    private void applyProteins() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setTitle("Recalculating. Please Wait...");
        progressDialog.setPrimaryProgressCounterIndeterminate(false);

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("RecalculateThread") {
            @Override
            public void run() {

                try {
                    PSMaps pSMaps = new PSMaps();
                    pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
                    PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getProjectParameters(), pSMaps);

                    miniShaker.proteinMapChanged(progressDialog, peptideShakerGUI.getIdentificationParameters().getFractionParameters());
                    modifiedMaps.put(0, false);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }

                progressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Update the separators if the frame size changes.
     */
    public void updateSeparators() {
        formComponentResized(null);
    }

    /**
     * Returns true of the data has been reloaded with the currently selected
     * threshold.
     *
     * @return true of the data has been reloaded with the currently selected
     * threshold
     */
    public boolean thresholdUpdated() {
        return dataValidated;
    }

    /**
     * Returns true of the data has been reloaded with the currently selected
     * PEP window.
     *
     * @return true of the data has been reloaded with the currently selected
     * PEP window
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

    /**
     * Returns true if the tab has been loaded at least once.
     *
     * @return true if the tab has been loaded at least once
     */
    public boolean isInitiated() {
        return tabInitiated;
    }

    /**
     * Applies the threshold set in thresholdInput to the currently selected
     * group.
     */
    private void applyThreshold(int selectedGroup, double threshold, int thresholdType) {

        if (threshold < 0 || threshold > 100) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold. Interval: [0, 100].", "Threshold Error", JOptionPane.WARNING_MESSAGE);
        } else {
            currentTargetDecoyMap = getTargetDecoyMap(selectedGroup);
            targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
            lastThresholds.put(selectedGroup, threshold);
            lastThresholdTypes.put(selectedGroup, thresholdType);
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            updateResults(thresholdType, threshold);
            validateButton.setEnabled(threshold != originalThresholds.get(selectedGroup) || thresholdType != originalThresholdTypes.get(selectedGroup));
            dataValidated = threshold == originalThresholds.get(selectedGroup) && thresholdType == originalThresholdTypes.get(selectedGroup);
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Resets all thresholds to the last validated value.
     */
    public void resetAllThresholds() {
        for (int i = 0; i < groupSelectionTable.getRowCount(); i++) {
            resetThreshold(i);
        }
        groupSelectionChanged();
    }

    /**
     * Resets the threshold of the given group to the last validated value.
     *
     * @param groupSelection the index of the group of interest
     */
    public void resetThreshold(int groupSelection) {
        currentTargetDecoyMap = getTargetDecoyMap(groupSelection);
        targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
        double threshold = originalThresholds.get(groupSelection);
        int thresholdType = originalThresholdTypes.get(groupSelection);
        if (groupSelection == groupSelectionTable.getSelectedRow()) {
            thresholdInput.setText(threshold + "");
            thresholdTypeCmb.setSelectedIndex(thresholdType);
        }
        applyThreshold(groupSelection, threshold, thresholdType);
    }
}
