package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyResults;
import eu.isas.peptideshaker.fdrestimation.TargetDecoySeries;
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
     * The FDR axis in the FDRs plot
     */
    private NumberAxis classicalAxis;

    /**
     * Create a new StatsPanel.
     *
     * @param parent the PeptideShaker parent frame.
     */
    public StatsPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent; 

        initComponents();

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
        NumberAxis probaAxis = new NumberAxis("Probabilistic FDR [%]");
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
        groupListJScrollPane = new javax.swing.JScrollPane();
        groupList = new javax.swing.JList();
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
        optimizationJPanel = new javax.swing.JPanel();
        optimizationTabbedPane = new javax.swing.JTabbedPane();
        estimatorOptimizationTab = new javax.swing.JPanel();
        estimatorsPlotSplitPane = new javax.swing.JSplitPane();
        pepPanel = new javax.swing.JPanel();
        pepChartPanel = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        sensitivitySlider1 = new javax.swing.JSlider();
        fdrsPanel = new javax.swing.JPanel();
        fdrsChartPanel = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        sensitivitySlider2 = new javax.swing.JSlider();
        jLabel35 = new javax.swing.JLabel();
        thresholdOptimizationTab = new javax.swing.JPanel();
        leftPlotSplitPane = new javax.swing.JSplitPane();
        confidencePanel = new javax.swing.JPanel();
        confidenceChartPanel = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        confidenceSlider = new javax.swing.JSlider();
        jLabel26 = new javax.swing.JLabel();
        rightPlotSplitPane = new javax.swing.JSplitPane();
        fdrFnrPanel = new javax.swing.JPanel();
        fdrFnrChartPanel = new javax.swing.JPanel();
        jLabel28 = new javax.swing.JLabel();
        fdrSlider1 = new javax.swing.JSlider();
        jLabel29 = new javax.swing.JLabel();
        benefitCostPanel = new javax.swing.JPanel();
        benefitCostChartPanel = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        fdrSlider2 = new javax.swing.JSlider();
        jLabel33 = new javax.swing.JLabel();
        parametersJPanel = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        confidenceInput = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        fdrInput = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        fnrInput = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        validateButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        windowTxt = new javax.swing.JTextField();
        applyButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        fdrCombo1 = new javax.swing.JComboBox();
        jLabel31 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        groupListJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Group Selection"));
        groupListJPanel.setOpaque(false);

        groupList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Proteins", "Peptides", "PSMs" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        groupList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                groupListMousePressed(evt);
            }
        });
        groupList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                groupListKeyReleased(evt);
            }
        });
        groupListJScrollPane.setViewportView(groupList);

        javax.swing.GroupLayout groupListJPanelLayout = new javax.swing.GroupLayout(groupListJPanel);
        groupListJPanel.setLayout(groupListJPanelLayout);
        groupListJPanelLayout.setHorizontalGroup(
            groupListJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupListJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupListJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addContainerGap())
        );
        groupListJPanelLayout.setVerticalGroup(
            groupListJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupListJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupListJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                .addContainerGap())
        );

        idSummaryJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Summary"));
        idSummaryJPanel.setOpaque(false);

        jLabel2.setText("Total TP:");

        nTotalTxt.setEditable(false);
        nTotalTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel1.setText("# Validated Hits:");

        nValidatedTxt.setEditable(false);
        nValidatedTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel3.setText("# FP:");

        jLabel10.setText("# TP:");

        nTPlTxt.setEditable(false);
        nTPlTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        nFPTxt.setEditable(false);
        nFPTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel13.setText("FDR:");

        jLabel36.setText("FNR:");

        fdrTxt.setEditable(false);

        fnrTxt.setEditable(false);

        jLabel6.setText("Dataset properties:");

        jLabel20.setText("Nmax:");

        nMaxTxt.setEditable(false);
        nMaxTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel7.setText("Validation Results:");

        jLabel23.setText("Lowest confidence:");

        confidenceTxt.setEditable(false);

        javax.swing.GroupLayout idSummaryJPanelLayout = new javax.swing.GroupLayout(idSummaryJPanel);
        idSummaryJPanel.setLayout(idSummaryJPanelLayout);
        idSummaryJPanelLayout.setHorizontalGroup(
            idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabel2))
                            .addGroup(idSummaryJPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel10))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(nTotalTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(nValidatedTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(nFPTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(nTPlTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel36)
                            .addComponent(jLabel13)
                            .addComponent(jLabel23))
                        .addGap(10, 10, 10)
                        .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(fdrTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(fnrTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(nMaxTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(confidenceTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)))
                    .addComponent(jLabel7))
                .addContainerGap())
        );
        idSummaryJPanelLayout.setVerticalGroup(
            idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, idSummaryJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(nTotalTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20)
                    .addComponent(nMaxTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(nValidatedTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23)
                    .addComponent(confidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(nFPTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(fdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(idSummaryJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(nTPlTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel36)
                    .addComponent(fnrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        optimizationJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Optimization"));
        optimizationJPanel.setOpaque(false);

        optimizationTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        estimatorOptimizationTab.setOpaque(false);

        estimatorsPlotSplitPane.setBorder(null);
        estimatorsPlotSplitPane.setDividerLocation(estimatorsPlotSplitPane.getWidth() / 2);
        estimatorsPlotSplitPane.setDividerSize(0);
        estimatorsPlotSplitPane.setResizeWeight(0.5);
        estimatorsPlotSplitPane.setOpaque(false);

        pepPanel.setOpaque(false);

        pepChartPanel.setOpaque(false);
        pepChartPanel.setLayout(new javax.swing.BoxLayout(pepChartPanel, javax.swing.BoxLayout.LINE_AXIS));

        jLabel30.setText("Sensitivity");

        jLabel27.setText("Robustness");

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

        javax.swing.GroupLayout pepPanelLayout = new javax.swing.GroupLayout(pepPanel);
        pepPanel.setLayout(pepPanelLayout);
        pepPanelLayout.setHorizontalGroup(
            pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pepPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pepPanelLayout.createSequentialGroup()
                        .addComponent(pepChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                        .addGap(10, 10, 10))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pepPanelLayout.createSequentialGroup()
                        .addComponent(jLabel30)
                        .addGap(18, 18, 18)
                        .addComponent(sensitivitySlider1, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel27)
                        .addContainerGap())))
        );
        pepPanelLayout.setVerticalGroup(
            pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pepPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pepChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel30)
                    .addComponent(sensitivitySlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addContainerGap())
        );

        estimatorsPlotSplitPane.setLeftComponent(pepPanel);

        fdrsPanel.setOpaque(false);

        fdrsChartPanel.setOpaque(false);
        fdrsChartPanel.setLayout(new javax.swing.BoxLayout(fdrsChartPanel, javax.swing.BoxLayout.LINE_AXIS));

        jLabel34.setText("Sensitivity");

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

        javax.swing.GroupLayout fdrsPanelLayout = new javax.swing.GroupLayout(fdrsPanel);
        fdrsPanel.setLayout(fdrsPanelLayout);
        fdrsPanelLayout.setHorizontalGroup(
            fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fdrsChartPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
                    .addGroup(fdrsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addGap(18, 18, 18)
                        .addComponent(sensitivitySlider2, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel35)))
                .addContainerGap())
        );
        fdrsPanelLayout.setVerticalGroup(
            fdrsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fdrsChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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
            .addComponent(estimatorsPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1112, Short.MAX_VALUE)
        );
        estimatorOptimizationTabLayout.setVerticalGroup(
            estimatorOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(estimatorsPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 337, Short.MAX_VALUE)
        );

        optimizationTabbedPane.addTab("Estimators", estimatorOptimizationTab);

        thresholdOptimizationTab.setOpaque(false);

        leftPlotSplitPane.setBorder(null);
        leftPlotSplitPane.setDividerLocation(leftPlotSplitPane.getWidth() / 3);
        leftPlotSplitPane.setDividerSize(0);
        leftPlotSplitPane.setResizeWeight(0.5);
        leftPlotSplitPane.setOpaque(false);

        confidencePanel.setOpaque(false);

        confidenceChartPanel.setOpaque(false);
        confidenceChartPanel.setLayout(new javax.swing.BoxLayout(confidenceChartPanel, javax.swing.BoxLayout.LINE_AXIS));

        jLabel25.setText("Quantity");

        confidenceSlider.setToolTipText("Confidence Threshold");
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

        javax.swing.GroupLayout confidencePanelLayout = new javax.swing.GroupLayout(confidencePanel);
        confidencePanel.setLayout(confidencePanelLayout);
        confidencePanelLayout.setHorizontalGroup(
            confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confidencePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(confidenceChartPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
                    .addGroup(confidencePanelLayout.createSequentialGroup()
                        .addComponent(jLabel25)
                        .addGap(18, 18, 18)
                        .addComponent(confidenceSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel26)))
                .addContainerGap())
        );
        confidencePanelLayout.setVerticalGroup(
            confidencePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, confidencePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(confidenceChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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

        fdrFnrChartPanel.setOpaque(false);
        fdrFnrChartPanel.setLayout(new javax.swing.BoxLayout(fdrFnrChartPanel, javax.swing.BoxLayout.LINE_AXIS));

        jLabel28.setText("Quality");

        fdrSlider1.setToolTipText("FDR Threshold");
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

        javax.swing.GroupLayout fdrFnrPanelLayout = new javax.swing.GroupLayout(fdrFnrPanel);
        fdrFnrPanel.setLayout(fdrFnrPanelLayout);
        fdrFnrPanelLayout.setHorizontalGroup(
            fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrFnrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fdrFnrChartPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                    .addGroup(fdrFnrPanelLayout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addGap(18, 18, 18)
                        .addComponent(fdrSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel29)))
                .addContainerGap())
        );
        fdrFnrPanelLayout.setVerticalGroup(
            fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fdrFnrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fdrFnrChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(fdrFnrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(fdrSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel29)
                    .addComponent(jLabel28))
                .addContainerGap())
        );

        rightPlotSplitPane.setLeftComponent(fdrFnrPanel);

        benefitCostPanel.setOpaque(false);

        benefitCostChartPanel.setOpaque(false);
        benefitCostChartPanel.setLayout(new javax.swing.BoxLayout(benefitCostChartPanel, javax.swing.BoxLayout.LINE_AXIS));

        jLabel32.setText("Quality");

        fdrSlider2.setToolTipText("FDR Threshold");
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

        javax.swing.GroupLayout benefitCostPanelLayout = new javax.swing.GroupLayout(benefitCostPanel);
        benefitCostPanel.setLayout(benefitCostPanelLayout);
        benefitCostPanelLayout.setHorizontalGroup(
            benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, benefitCostPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(benefitCostChartPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                    .addGroup(benefitCostPanelLayout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addGap(18, 18, 18)
                        .addComponent(fdrSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel33)))
                .addContainerGap())
        );
        benefitCostPanelLayout.setVerticalGroup(
            benefitCostPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, benefitCostPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(benefitCostChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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
            .addComponent(leftPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1112, Short.MAX_VALUE)
        );
        thresholdOptimizationTabLayout.setVerticalGroup(
            thresholdOptimizationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(leftPlotSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 337, Short.MAX_VALUE)
        );

        optimizationTabbedPane.addTab("Thresholds", thresholdOptimizationTab);

        optimizationTabbedPane.setSelectedIndex(1);

        javax.swing.GroupLayout optimizationJPanelLayout = new javax.swing.GroupLayout(optimizationJPanel);
        optimizationJPanel.setLayout(optimizationJPanelLayout);
        optimizationJPanelLayout.setHorizontalGroup(
            optimizationJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optimizationJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(optimizationTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1117, Short.MAX_VALUE)
                .addContainerGap())
        );
        optimizationJPanelLayout.setVerticalGroup(
            optimizationJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optimizationJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(optimizationTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                .addContainerGap())
        );

        parametersJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));
        parametersJPanel.setOpaque(false);

        jLabel21.setText("Confidence:");

        confidenceInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        confidenceInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confidenceInputActionPerformed(evt);
            }
        });

        jLabel18.setText("%");

        jLabel12.setText("FDR:");

        fdrInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fdrInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrInputActionPerformed(evt);
            }
        });

        jLabel14.setText("%");

        jLabel24.setText("FNR:");

        fnrInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fnrInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fnrInputActionPerformed(evt);
            }
        });

        jLabel15.setText("%");

        jLabel4.setText("Desired threshold:");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        validateButton.setText("Validate");
        validateButton.setToolTipText("Reload the data with the current validation settings");
        validateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Estimator Optimization:");

        jLabel22.setText("PEP Window:");

        windowTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        windowTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                windowTxtActionPerformed(evt);
            }
        });

        applyButton.setText("Apply");
        applyButton.setToolTipText("Reload the data with the current validation settings");
        applyButton.setEnabled(false);
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        fdrCombo1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Classical", "Probabilistic" }));
        fdrCombo1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrCombo1ActionPerformed(evt);
            }
        });

        jLabel31.setText("Estimator:");

        javax.swing.GroupLayout parametersJPanelLayout = new javax.swing.GroupLayout(parametersJPanel);
        parametersJPanel.setLayout(parametersJPanelLayout);
        parametersJPanelLayout.setHorizontalGroup(
            parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel4)
                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                            .addGap(10, 10, 10)
                            .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel21)
                                .addComponent(jLabel12)
                                .addComponent(jLabel24))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(fnrInput, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                .addComponent(fdrInput, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                .addComponent(confidenceInput, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel14)
                                .addComponent(jLabel18)
                                .addComponent(jLabel15))))
                    .addComponent(validateButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel5)
                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                            .addComponent(applyButton)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(cancelButton)))
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(windowTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE))
                    .addGroup(parametersJPanelLayout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addGap(18, 18, 18)
                        .addComponent(fdrCombo1, 0, 87, Short.MAX_VALUE)))
                .addGap(91, 91, 91))
        );

        parametersJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {applyButton, cancelButton});

        parametersJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {confidenceInput, fdrInput, fnrInput});

        parametersJPanelLayout.setVerticalGroup(
            parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametersJPanelLayout.createSequentialGroup()
                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, parametersJPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(parametersJPanelLayout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(18, 18, 18)
                                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel18)
                                    .addComponent(confidenceInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel21))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel12)
                                    .addComponent(jLabel14)
                                    .addComponent(fdrInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(fnrInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel24)
                                    .addComponent(jLabel15)))
                            .addGroup(parametersJPanelLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addGap(18, 18, 18)
                                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel22)
                                    .addComponent(windowTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel31)
                                    .addComponent(fdrCombo1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(18, 34, Short.MAX_VALUE)
                        .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(parametersJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(cancelButton)
                                .addComponent(applyButton))
                            .addComponent(validateButton)))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(optimizationJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(groupListJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idSummaryJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                        .addComponent(parametersJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(idSummaryJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                        .addComponent(groupListJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(parametersJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(optimizationJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the group selection.
     *
     * @param evt
     */
    private void groupListMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_groupListMousePressed
        groupSelectionChanged();
    }//GEN-LAST:event_groupListMousePressed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void confidenceInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confidenceInputActionPerformed
        try {
            lastThreshold = new Double(confidenceInput.getText());
            lastThresholdType = 0;
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            updateResults();
            updateDisplayedComponents();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold.", "Import error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_confidenceInputActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrInputActionPerformed
        try {
            lastThreshold = new Double(fdrInput.getText());
            lastThresholdType = 1;
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            updateResults();
            updateDisplayedComponents();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold.", "Import error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_fdrInputActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fnrInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fnrInputActionPerformed
        try {
            lastThreshold = new Double(fnrInput.getText());
            lastThresholdType = 2;
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            updateResults();
            updateDisplayedComponents();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given threshold.", "Import error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_fnrInputActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrCombo1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrCombo1ActionPerformed
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
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_confidenceSliderMouseReleased

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void confidenceSliderMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_confidenceSliderMouseDragged
        confidenceInput.setText(confidenceSlider.getValue() + "");
    }//GEN-LAST:event_confidenceSliderMouseDragged

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void fdrSlider1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fdrSlider1MouseDragged
        fdrInput.setText(fdrSlider1.getValue() + "");
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
            currentTargetDecoyMap.setWindowSize(newWindow);
            currentTargetDecoyMap.estimateProbabilities();
            targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            updateResults();
            updateDisplayedComponents();
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            modifiedMaps.put(groupList.getSelectedIndex(), true);
            applyButton.setEnabled(true);
            cancelButton.setEnabled(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the given window size.", "Import error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_windowTxtActionPerformed

    /**
     * Updates the plots.
     *
     * @param evt
     */
    private void sensitivitySlider1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sensitivitySlider1MouseDragged
        Double newWindow = Math.pow(10, sensitivitySlider1.getValue() / 50.0 - 1) * currentTargetDecoyMap.getnMax();
        windowTxt.setText(newWindow.intValue() + "");
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

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (groupList.getSelectedIndex() == 0) {
            applyProteins();
        } else if (peptideMap.keySet().contains(groupList.getSelectedIndex())) {
            recalculateProteins();
        } else {
            recalculatePeptidesAndProteins();
        }

        applyButton.setEnabled(false);
        cancelButton.setEnabled(false);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        // reload the data to update the scores
        reloadData();
    }//GEN-LAST:event_applyButtonActionPerformed

    /**
     * Cancel the validations/optimizations.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        windowTxt.setText(currentTargetDecoyMap.getnMax() + "");
        windowTxtActionPerformed(null);
        applyButton.setEnabled(false);
        cancelButton.setEnabled(false);
        modifiedMaps.put(groupList.getSelectedIndex(), false);
    }//GEN-LAST:event_cancelButtonActionPerformed

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
    }//GEN-LAST:event_formComponentResized

    /**
     * Updates the group selection.
     *
     * @param evt
     */
    private void groupListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_groupListKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            groupSelectionChanged();
        }
    }//GEN-LAST:event_groupListKeyReleased

    /**
     * Validate the data using the current settings.
     *
     * @param evt
     */
    private void validateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getExperiment(), peptideShakerGUI.getSample(), peptideShakerGUI.getReplicateNumber(), pSMaps);
        miniShaker.validateIdentifications();
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        // update the other tabs
        reloadData();
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
     * Updates the plots.
     *
     * @param evt
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyButton;
    private javax.swing.JPanel benefitCostChartPanel;
    private javax.swing.JPanel benefitCostPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel confidenceChartPanel;
    private javax.swing.JTextField confidenceInput;
    private javax.swing.JPanel confidencePanel;
    private javax.swing.JSlider confidenceSlider;
    private javax.swing.JTextField confidenceTxt;
    private javax.swing.JPanel estimatorOptimizationTab;
    private javax.swing.JSplitPane estimatorsPlotSplitPane;
    private javax.swing.JComboBox fdrCombo1;
    private javax.swing.JPanel fdrFnrChartPanel;
    private javax.swing.JPanel fdrFnrPanel;
    private javax.swing.JTextField fdrInput;
    private javax.swing.JSlider fdrSlider1;
    private javax.swing.JSlider fdrSlider2;
    private javax.swing.JTextField fdrTxt;
    private javax.swing.JPanel fdrsChartPanel;
    private javax.swing.JPanel fdrsPanel;
    private javax.swing.JTextField fnrInput;
    private javax.swing.JTextField fnrTxt;
    private javax.swing.JList groupList;
    private javax.swing.JPanel groupListJPanel;
    private javax.swing.JScrollPane groupListJScrollPane;
    private javax.swing.JPanel idSummaryJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSplitPane leftPlotSplitPane;
    private javax.swing.JTextField nFPTxt;
    private javax.swing.JTextField nMaxTxt;
    private javax.swing.JTextField nTPlTxt;
    private javax.swing.JTextField nTotalTxt;
    private javax.swing.JTextField nValidatedTxt;
    private javax.swing.JPanel optimizationJPanel;
    private javax.swing.JTabbedPane optimizationTabbedPane;
    private javax.swing.JPanel parametersJPanel;
    private javax.swing.JPanel pepChartPanel;
    private javax.swing.JPanel pepPanel;
    private javax.swing.JSplitPane rightPlotSplitPane;
    private javax.swing.JSlider sensitivitySlider1;
    private javax.swing.JSlider sensitivitySlider2;
    private javax.swing.JPanel thresholdOptimizationTab;
    private javax.swing.JButton validateButton;
    private javax.swing.JTextField windowTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * This method displays results on the panel
     */
    public void displayResults() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        ArrayList<String> peptideKeys = pSMaps.getPeptideSpecificMap().getKeys();
        HashMap<Integer, String> psmKeys = pSMaps.getPsmSpecificMap().getKeys();

        int nMaps = 1 + peptideKeys.size() + psmKeys.size();
        int cpt = 0;
        String[] listContent = new String[nMaps];
        listContent[cpt] = "Proteins";
        modifiedMaps.put(cpt, false);
        cpt++;
        if (peptideKeys.size() == 1) {
            peptideMap.put(cpt, peptideKeys.get(0));
            listContent[cpt] = "Peptides";
            modifiedMaps.put(cpt, false);
            cpt++;
        } else {
            for (String pepitdeKey : peptideKeys) {
                peptideMap.put(cpt, pepitdeKey);
                if (pepitdeKey.equals("")) {
                    listContent[cpt] = "Not modified Peptides";
                } else if (pepitdeKey.equals(PeptideSpecificMap.DUSTBIN)) {
                    listContent[cpt] = "Other Peptides";
                } else {
                    listContent[cpt] = pepitdeKey + " Peptides";
                }
                modifiedMaps.put(cpt, false);
                cpt++;
            }
        }
        for (Integer psmKey : psmKeys.keySet()) {
            psmMap.put(cpt, psmKey);
            if (psmKeys.size() > 1) {
                listContent[cpt] = psmKeys.get(psmKey) + " PSMs";
            } else {
                listContent[cpt] = "PSMs";
            }
            modifiedMaps.put(cpt, false);
            cpt++;
        }

        groupList.setListData(listContent);
        groupList.setSelectedIndex(0);

        groupSelectionChanged();
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Method called whenever a new group selection occurred
     */
    private void groupSelectionChanged() {
        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
        int selectedGroup = groupList.getSelectedIndex();

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
                        + "Recalculate probabilities?", "Non Applied Changes", JOptionPane.YES_NO_OPTION);
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
                            + "Recalculate probabilities?", "Non Applied Changes", JOptionPane.YES_NO_OPTION);
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
                        + "Recalculate probabilities?", "Non Applied Changes", JOptionPane.YES_NO_OPTION);
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
        cancelButton.setEnabled(modifiedMaps.get(selectedGroup));
        nMaxTxt.setText(currentTargetDecoyMap.getnMax() + "");
        targetDecoySeries = currentTargetDecoyMap.getTargetDecoySeries();
        updateDisplayedComponents();
    }

    /**
     * Updates the displayed results whenever a new threshold is given.
     */
    private void updateResults() {

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

    /**
     * Updates the updates the different components of the gui with the new results.
     */
    private void updateDisplayedComponents() {

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
        windowTxt.setText(currentTargetDecoyMap.getWindowSize() + "");
        Double newPosition = 50 * (Math.log10(currentTargetDecoyMap.getWindowSize() / (double) currentTargetDecoyMap.getnMax()) + 1);
        sensitivitySlider1.setValue(newPosition.intValue());
        if (lastThresholdType==0) {
            confidenceInput.setText(lastThreshold + "");
            fdrInput.setText("");
            fnrInput.setText("");
        } else if (lastThresholdType==1) {
            confidenceInput.setText("");
            fdrInput.setText(lastThreshold + "");
            fnrInput.setText("");
        } else {
            confidenceInput.setText("");
            fdrInput.setText("");
            fnrInput.setText(lastThreshold + "");
        }
        if (currentResults.isClassicalEstimators()) {
            fdrCombo1.setSelectedIndex(0);
        } else {
            fdrCombo1.setSelectedIndex(1);
        }
        updateCharts();
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
        double[] fdrs = targetDecoySeries.getProbaFDR();
        double[] pep = targetDecoySeries.getPEP();
        double minScore = scores[0];
        double maxFDR = fdrs[fdrs.length-1];

        for (double score : scores) {
            if (score > 0) {
                minScore = score;
                break;
            }
        }
        for (int index = fdrs.length-1 ; index >= 0 ; index--) {
            if (pep[index] < 1) {
                maxFDR = fdrs[index];
                break;
            }
        }

        // set the lower range for the log axis and max for the fdr axis
        scoreAxis.setSmallestValue(minScore);
        classicalAxis.setRange(0, maxFDR);

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
        XYDifferenceRenderer confidenceRendrer = new XYDifferenceRenderer(
                new Color(0, 255, 0, 15), new Color(255, 0, 0, 15), false);
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
        confidenceInput.setText("");
        fdrInput.setText("");
        fnrInput.setText("");
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
}
