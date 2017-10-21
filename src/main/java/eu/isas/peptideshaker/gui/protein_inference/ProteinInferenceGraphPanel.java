package eu.isas.peptideshaker.gui.protein_inference;

import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.io.file.LastSelectedFolder;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.BasicEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.apache.commons.collections15.Transformer;

/**
 * A panel to display protein inference graphs.
 *
 * @author Harald Barsnes
 */
public class ProteinInferenceGraphPanel extends javax.swing.JPanel {

    /**
     * The nodes.
     */
    private ArrayList<String> nodes;
    /**
     * The edges: the keys are the node labels and the elements the list of
     * objects.
     */
    private HashMap<String, ArrayList<String>> edges;
    /**
     * The labels of the currently selected nodes.
     */
    private ArrayList<String> selectedNodes = new ArrayList<>();
    /**
     * The labels of the currently selected neighbor nodes.
     */
    private ArrayList<String> selectedNeighborNodes = new ArrayList<>();
    /**
     * The labels of the currently selected edges.
     */
    private ArrayList<String> selectedEdges = new ArrayList<>();
    /**
     * Set if if the peptide labels are to be shown.
     */
    private boolean showPeptideLabels = false;
    /**
     * Set if if the protein labels are to be shown.
     */
    private boolean showProteinLabels = false;
    /**
     * The edge properties: the keys are the edge names.
     */
    private HashMap<String, String> edgeProperties;
    /**
     * The node properties: the keys are the node names.
     */
    private HashMap<String, String> nodeProperties;
    /**
     * The node tooltips: the keys are the node names.
     */
    private HashMap<String, String> nodeToolTips;
    /**
     * The graph.
     */
    private UndirectedSparseGraph<String, String> graph;
    /**
     * The visualization viewer.
     */
    private VisualizationViewer visualizationViewer;
    /**
     * The parent dialog.
     */
    private JDialog parentDialog;
    /**
     * The parent panel.
     */
    private JPanel parentPanel;
    /**
     * The normal icon.
     */
    private Image normalIcon;
    /**
     * The waiting icon.
     */
    private Image waitingIcon;
    /**
     * The last folder opened by the user.
     */
    private LastSelectedFolder lastSelectedFolder;

    /**
     * Creates a new ProteinInferenceGraphPanel.
     *
     * @param parentDialog the parent dialog
     * @param parentPanel the parent panel
     * @param normalIcon the normal icon
     * @param waitingIcon the waiting icon
     * @param lastSelectedFolder the last selected folder
     * @param nodes the protein and peptide nodes
     * @param edges the edges, key is the starting node and the element is all
     * the ending nodes
     * @param nodeProperties the node properties
     * @param edgeProperties the edge properties
     * @param nodeToolTips the node tooltips
     * @param selectedNodes the list of selected nodes
     */
    public ProteinInferenceGraphPanel(JDialog parentDialog, JPanel parentPanel, Image normalIcon, Image waitingIcon, LastSelectedFolder lastSelectedFolder, 
            ArrayList<String> nodes, HashMap<String, ArrayList<String>> edges, HashMap<String, String> nodeProperties, HashMap<String, String> edgeProperties, 
            HashMap<String, String> nodeToolTips, ArrayList<String> selectedNodes) {
        initComponents();

        this.parentDialog = parentDialog;
        this.parentPanel = parentPanel;
        this.normalIcon = normalIcon;
        this.waitingIcon = waitingIcon;
        this.lastSelectedFolder = lastSelectedFolder;
        this.nodes = nodes;
        this.edges = edges;
        this.nodeProperties = nodeProperties;
        this.edgeProperties = edgeProperties;
        this.nodeToolTips = nodeToolTips;

        visualizationViewer = setUpGraph(parentPanel);

        // select the proteins part of the current protein group
        for (String tempNode : nodes) {
            if (selectedNodes != null && selectedNodes.contains(tempNode)) {
                visualizationViewer.getPickedVertexState().pick(tempNode, true);
            }
        }

        graphPanel.add(visualizationViewer);

        ScalingControl scaler = new CrossoverScalingControl();
        scaler.scale(visualizationViewer, 0.9f, visualizationViewer.getCenter());
    }

    /**
     * Redo the layout of the graph. Node selection is retained.
     */
    private void updateGraphLayout() {

        visualizationViewer = setUpGraph(parentPanel);
        graphPanel.removeAll();

        ScalingControl scaler = new CrossoverScalingControl();
        scaler.scale(visualizationViewer, 0.9f, visualizationViewer.getCenter());

        // select the proteins part of the current protein group
        for (String tempNode : nodes) {
            if (selectedNodes != null && selectedNodes.contains(tempNode)) {
                visualizationViewer.getPickedVertexState().pick(tempNode, true);
            } else {
                visualizationViewer.getPickedVertexState().pick(tempNode, false);
            }
        }

        graphPanel.add(visualizationViewer);
        graphPanel.revalidate();
        graphPanel.repaint();
    }

    /**
     * Set up the graph.
     *
     * @param parentPanel the parent panel
     * @return the visualization viewer
     */
    private VisualizationViewer setUpGraph(JPanel parentPanel) {

        graph = new UndirectedSparseGraph<>();

        // add all the nodes
        for (String node : nodes) {
            graph.addVertex(node);
        }

        // add the vertexes
        Iterator<String> startNodeKeys = edges.keySet().iterator();

        while (startNodeKeys.hasNext()) {

            String startNode = startNodeKeys.next();

            for (String endNode : edges.get(startNode)) {
                graph.addEdge(startNode + "|" + endNode, startNode, endNode);
            }
        }

        // create the visualization viewer
        VisualizationViewer<String, String> vv = new VisualizationViewer<>(new FRLayout<>(graph),
                new Dimension(parentPanel.getWidth() - 20, parentPanel.getHeight() - 100));
        vv.setBackground(Color.WHITE);

        // set the vertex label transformer
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<String, String>() {
            @Override
            public String transform(String arg0) {
                return arg0;
            }
        });

        // set the edge label transformer
        vv.getRenderContext().setEdgeLabelTransformer(new Transformer<String, String>() {
            @Override
            public String transform(String arg0) {
                return arg0;
            }
        });

        // set the vertex renderer
        vv.getRenderer().setVertexRenderer(new ProteinInferenceVertexRenderer());

        // set the edge label renderer
        vv.getRenderer().setEdgeLabelRenderer(new BasicEdgeLabelRenderer<String, String>() {

            @Override
            public void labelEdge(RenderContext<String, String> rc, Layout<String, String> layout, String e, String label) {
                // do nothing
            }
        });

        // set the vertex label renderer
        vv.getRenderer().setVertexLabelRenderer(new BasicVertexLabelRenderer<String, String>() {

            @Override
            public void labelVertex(RenderContext<String, String> rc, Layout<String, String> layout, String v, String label) {
                if (label.startsWith("Peptide") && showPeptideLabels) {
                    String fullTooltip = nodeToolTips.get(label);
                    super.labelVertex(rc, layout, v, fullTooltip.substring(0, fullTooltip.indexOf("<br>")));
                }
                if (label.startsWith("Protein") && showProteinLabels) {
                    super.labelVertex(rc, layout, v, label.substring(label.indexOf(" ") + 1));
                }
            }
        });

        // set the edge format
        vv.getRenderContext().setEdgeDrawPaintTransformer(edgePaint);
        vv.getRenderContext().setEdgeStrokeTransformer(edgeStroke);

        // set the mouse interaction mode
        final DefaultModalGraphMouse<String, Number> graphMouse = new DefaultModalGraphMouse<>();
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
        vv.setGraphMouse(graphMouse);

        // add a key listener
        vv.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
                    for (String tempNode : nodes) {
                        visualizationViewer.getPickedVertexState().pick(tempNode, true);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    for (String tempNode : nodes) {
                        visualizationViewer.getPickedVertexState().pick(tempNode, false);
                    }
                }
                super.keyReleased(e);
            }

        });

        // set the vertex tooltips
        vv.setVertexToolTipTransformer(
                new ToStringLabeller<String>() {

                    @Override
                    public String transform(String v) {
                        if (nodeToolTips != null && nodeToolTips.get(v) != null) {
                            return super.transform(nodeToolTips.get(v));
                        } else {
                            return super.transform(v.substring(v.indexOf(" ") + 1));
                        }
                    }
                }
        );

        // attach the listener that will print when the vertices selection changes
        final PickedState<String> pickedState = vv.getPickedVertexState();
        pickedState.addItemListener(
                new ItemListener() {

                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        Object subject = e.getItem();
                        if (subject instanceof String) {
                            String vertex = (String) subject;
                            if (pickedState.isPicked(vertex)) {
                                if (!selectedNodes.contains(vertex)) {
                                    selectedNodes.add(vertex);
                                }
                            } else {
                                selectedNodes.remove(vertex);
                            }
                        }
                        updateNodeSelection();
                    }
                }
        );

        return vv;
    }

    /**
     * Update the node selection.
     */
    private void updateNodeSelection() {

        selectedNeighborNodes = new ArrayList<>();
        selectedEdges = new ArrayList<>();

        // get the list of all neighbors of selected nodes and the edges involved
        for (String node : selectedNodes) {

            Collection<String> neighbors = graph.getNeighbors(node);

            for (String tempNode : neighbors) {
                if (allRadioButton.isSelected()) {
                    if (!selectedNeighborNodes.contains(tempNode)) {
                        selectedNeighborNodes.add(tempNode);
                    }
                } else {
                    if (graph.getNeighbors(tempNode).size() == 1) {
                        if (!selectedNeighborNodes.contains(tempNode)) {
                            selectedNeighborNodes.add(tempNode);
                        }
                    }
                }
            }

            Collection<String> inEdges = graph.getInEdges(node);
            for (String tempEdge : inEdges) {

                String[] tempNodes = tempEdge.split("\\|");

                if ((selectedNodes.contains(tempNodes[0]) && selectedNeighborNodes.contains(tempNodes[1]))
                        || selectedNodes.contains(tempNodes[1]) && selectedNeighborNodes.contains(tempNodes[0])) {
                    if (!selectedEdges.contains(tempEdge)) {
                        selectedEdges.add(tempEdge);
                    }
                }
            }

            Collection<String> outEdges = graph.getOutEdges(node);
            for (String tempEdge : outEdges) {
                String[] tempNodes = tempEdge.split("\\|");

                if ((selectedNodes.contains(tempNodes[0]) && selectedNeighborNodes.contains(tempNodes[1]))
                        || selectedNodes.contains(tempNodes[1]) && selectedNeighborNodes.contains(tempNodes[0])) {
                    if (!selectedEdges.contains(tempEdge)) {
                        selectedEdges.add(tempEdge);
                    }
                }
            }
        }

        int proteinCount = 0;
        int peptideCount = 0;
        for (String tempNode : selectedNodes) {
            if (tempNode.startsWith("Protein")) {
                proteinCount++;
            } else if (tempNode.startsWith("Peptide")) {
                peptideCount++;
            }
        }
        for (String tempNode : selectedNeighborNodes) {
            if (!selectedNodes.contains(tempNode)) {
                if (tempNode.startsWith("Protein")) {
                    proteinCount++;
                } else if (tempNode.startsWith("Peptide")) {
                    peptideCount++;
                }
            }
        }

        proteinCountValueLabel.setText("<html><a href>" + proteinCount + "</html>");
        peptideCountValueLabel.setText("<html><a href>" + peptideCount + "</html>");
    }

    /**
     * The color formatting for the edges.
     */
    private Transformer<String, Paint> edgePaint = new Transformer<String, Paint>() {
        public Paint transform(String s) {
            if (selectedEdges.isEmpty()) {
                return new Color(100, 100, 100, 100);
            } else if (selectedEdges.contains(s)) {
                return new Color(100, 100, 100, 255);
            } else {
                return new Color(100, 100, 100, 100);
            }
        }
    };

    /**
     * The stroke type for the edges.
     */
    private Transformer<String, Stroke> edgeStroke = new Transformer<String, Stroke>() {
        float dash[] = {10.0f};

        public Stroke transform(String s) {

            String edgeProperty = edgeProperties.get(s);

            if (edgeProperty != null) {
                if (Boolean.parseBoolean(edgeProperty)) {
                    return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10.0f);
                } else {
                    return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
                }
            }

            return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        }
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        annotationButtonGroup = new javax.swing.ButtonGroup();
        selectionButtonGroup = new javax.swing.ButtonGroup();
        selectionPanel = new javax.swing.JPanel();
        proteinCountLabel = new javax.swing.JLabel();
        proteinCountValueLabel = new javax.swing.JLabel();
        peptideCountLabel = new javax.swing.JLabel();
        peptideCountValueLabel = new javax.swing.JLabel();
        layoutLabel = new javax.swing.JLabel();
        exportLabel = new javax.swing.JLabel();
        legendLabel = new javax.swing.JLabel();
        helpLabel = new javax.swing.JLabel();
        selectAllLabel = new javax.swing.JLabel();
        showProteinLabelsLabel = new javax.swing.JLabel();
        showPeptideLabelsLabel = new javax.swing.JLabel();
        graphPanel = new javax.swing.JPanel();
        settingsPanel = new javax.swing.JPanel();
        evidenceRadioButton = new javax.swing.JRadioButton();
        validationStatusRadioButton = new javax.swing.JRadioButton();
        nodeTypeRadioButton = new javax.swing.JRadioButton();
        allRadioButton = new javax.swing.JRadioButton();
        uniqueRadioButton = new javax.swing.JRadioButton();
        highlightCheckBox = new javax.swing.JCheckBox();

        selectionPanel.setBackground(new java.awt.Color(255, 255, 255));

        proteinCountLabel.setText("#Proteins:");

        proteinCountValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        proteinCountValueLabel.setText("0");
        proteinCountValueLabel.setMinimumSize(new java.awt.Dimension(14, 14));
        proteinCountValueLabel.setPreferredSize(new java.awt.Dimension(14, 14));
        proteinCountValueLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinCountValueLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinCountValueLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinCountValueLabelMouseReleased(evt);
            }
        });

        peptideCountLabel.setText("#Peptides:");

        peptideCountValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        peptideCountValueLabel.setText("0");
        peptideCountValueLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptideCountValueLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideCountValueLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideCountValueLabelMouseReleased(evt);
            }
        });

        layoutLabel.setText("<html><a href>Layout</html>");
        layoutLabel.setToolTipText("Redo the graph layout");
        layoutLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                layoutLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                layoutLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                layoutLabelMouseReleased(evt);
            }
        });

        exportLabel.setText("<html><a href>Export</html>");
        exportLabel.setToolTipText("Export graph as image");
        exportLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportLabelMouseReleased(evt);
            }
        });

        legendLabel.setText("<html><a href>Legend</html>");
        legendLabel.setToolTipText("Show graph legend");
        legendLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                legendLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                legendLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                legendLabelMouseReleased(evt);
            }
        });

        helpLabel.setText("<html><a href>Help</html>");
        helpLabel.setToolTipText("Show graph help");
        helpLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                helpLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                helpLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                helpLabelMouseReleased(evt);
            }
        });

        selectAllLabel.setText("<html><a href>Select All</html>");
        selectAllLabel.setToolTipText("Select all proteins and peptides");
        selectAllLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseReleased(evt);
            }
        });

        showProteinLabelsLabel.setText("<html><a href>Protein Labels</html>");
        showProteinLabelsLabel.setToolTipText("Show the protein labels");
        showProteinLabelsLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                showProteinLabelsLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                showProteinLabelsLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                showProteinLabelsLabelMouseReleased(evt);
            }
        });

        showPeptideLabelsLabel.setText("<html><a href>Peptide Labels</html>");
        showPeptideLabelsLabel.setToolTipText("Show the peptide labels");
        showPeptideLabelsLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                showPeptideLabelsLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                showPeptideLabelsLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                showPeptideLabelsLabelMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout selectionPanelLayout = new javax.swing.GroupLayout(selectionPanel);
        selectionPanel.setLayout(selectionPanelLayout);
        selectionPanelLayout.setHorizontalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addComponent(layoutLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(exportLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(legendLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(helpLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(selectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addComponent(showProteinLabelsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(showPeptideLabelsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinCountLabel)
                    .addComponent(peptideCountLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinCountValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(peptideCountValueLabel))
                .addContainerGap())
        );

        selectionPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {peptideCountValueLabel, proteinCountValueLabel});

        selectionPanelLayout.setVerticalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinCountLabel)
                    .addComponent(proteinCountValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(layoutLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(helpLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(legendLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectAllLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peptideCountLabel)
                    .addComponent(peptideCountValueLabel)
                    .addComponent(showProteinLabelsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(showPeptideLabelsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        graphPanel.setBackground(new java.awt.Color(255, 255, 255));
        graphPanel.setLayout(new javax.swing.BoxLayout(graphPanel, javax.swing.BoxLayout.LINE_AXIS));

        settingsPanel.setBackground(new java.awt.Color(255, 255, 255));

        annotationButtonGroup.add(evidenceRadioButton);
        evidenceRadioButton.setText("Protein Evidence");
        evidenceRadioButton.setToolTipText("Protein Evidence (from UniProt)");
        evidenceRadioButton.setOpaque(false);
        evidenceRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                evidenceRadioButtonActionPerformed(evt);
            }
        });

        annotationButtonGroup.add(validationStatusRadioButton);
        validationStatusRadioButton.setText("Validation Status");
        validationStatusRadioButton.setToolTipText("Peptide and Protein Validation Status");
        validationStatusRadioButton.setOpaque(false);
        validationStatusRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationStatusRadioButtonActionPerformed(evt);
            }
        });

        annotationButtonGroup.add(nodeTypeRadioButton);
        nodeTypeRadioButton.setSelected(true);
        nodeTypeRadioButton.setText("Molecule Type");
        nodeTypeRadioButton.setToolTipText("Peptide or Protein");
        nodeTypeRadioButton.setOpaque(false);
        nodeTypeRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodeTypeRadioButtonActionPerformed(evt);
            }
        });

        selectionButtonGroup.add(allRadioButton);
        allRadioButton.setSelected(true);
        allRadioButton.setText("All Neighbors");
        allRadioButton.setToolTipText("Select all neighbors");
        allRadioButton.setOpaque(false);
        allRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allRadioButtonActionPerformed(evt);
            }
        });

        selectionButtonGroup.add(uniqueRadioButton);
        uniqueRadioButton.setText("Unique Only");
        uniqueRadioButton.setToolTipText("Select unique neighbors only");
        uniqueRadioButton.setOpaque(false);
        uniqueRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uniqueRadioButtonActionPerformed(evt);
            }
        });

        highlightCheckBox.setText("Highlight");
        highlightCheckBox.setToolTipText("Highlight the selected peptides and proteins");
        highlightCheckBox.setOpaque(false);
        highlightCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highlightCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(allRadioButton)
                    .addComponent(validationStatusRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(evidenceRadioButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nodeTypeRadioButton))
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(uniqueRadioButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(highlightCheckBox)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        settingsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {allRadioButton, evidenceRadioButton, highlightCheckBox, nodeTypeRadioButton, uniqueRadioButton, validationStatusRadioButton});

        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(evidenceRadioButton)
                    .addComponent(validationStatusRadioButton)
                    .addComponent(nodeTypeRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(uniqueRadioButton)
                    .addComponent(allRadioButton)
                    .addComponent(highlightCheckBox))
                .addGap(5, 5, 5))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(selectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(selectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the graph.
     *
     * @param evt
     */
    private void allRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allRadioButtonActionPerformed
        updateNodeSelection();
        this.repaint();
    }//GEN-LAST:event_allRadioButtonActionPerformed

    /**
     * Update the graph.
     *
     * @param evt
     */
    private void uniqueRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uniqueRadioButtonActionPerformed
        updateNodeSelection();
        this.repaint();
    }//GEN-LAST:event_uniqueRadioButtonActionPerformed

    /**
     * Update the graph.
     *
     * @param evt
     */
    private void validationStatusRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationStatusRadioButtonActionPerformed
        this.repaint();
    }//GEN-LAST:event_validationStatusRadioButtonActionPerformed

    /**
     * Update the graph.
     *
     * @param evt
     */
    private void evidenceRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_evidenceRadioButtonActionPerformed
        this.repaint();
    }//GEN-LAST:event_evidenceRadioButtonActionPerformed

    /**
     * Update the graph.
     *
     * @param evt
     */
    private void nodeTypeRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nodeTypeRadioButtonActionPerformed
        this.repaint();
    }//GEN-LAST:event_nodeTypeRadioButtonActionPerformed

    /**
     * Update the graph.
     *
     * @param evt
     */
    private void highlightCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highlightCheckBoxActionPerformed
        this.repaint();
    }//GEN-LAST:event_highlightCheckBoxActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void proteinCountValueLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinCountValueLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_proteinCountValueLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void proteinCountValueLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinCountValueLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinCountValueLabelMouseExited

    /**
     * Open the ProteinInferenceGraphSelectionDialog.
     *
     * @param evt
     */
    private void proteinCountValueLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinCountValueLabelMouseReleased
        new ProteinInferenceGraphSelectionDialog(parentDialog, true, getSelectedValuesAsString());
    }//GEN-LAST:event_proteinCountValueLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void peptideCountValueLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideCountValueLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptideCountValueLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideCountValueLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideCountValueLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideCountValueLabelMouseExited

    /**
     * Open the ProteinInferenceGraphSelectionDialog.
     *
     * @param evt
     */
    private void peptideCountValueLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideCountValueLabelMouseReleased
        new ProteinInferenceGraphSelectionDialog(parentDialog, true, getSelectedValuesAsString());
    }//GEN-LAST:event_peptideCountValueLabelMouseReleased

    private void layoutLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_layoutLabelMouseReleased
        updateGraphLayout();
    }//GEN-LAST:event_layoutLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void layoutLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_layoutLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_layoutLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void layoutLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_layoutLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_layoutLabelMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportLabelMouseExited

    /**
     * Open the export graphics menu.
     *
     * @param evt
     */
    private void exportLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportLabelMouseReleased
        new ExportGraphicsDialog(parentDialog, normalIcon, waitingIcon, true, (Component) graphPanel, lastSelectedFolder);
    }//GEN-LAST:event_exportLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void helpLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_helpLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void helpLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpLabelMouseExited

    /**
     * Open the protein inference graph help.
     *
     * @param evt
     */
    private void helpLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLabelMouseReleased
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(parentDialog, getClass().getResource("/helpFiles/ProteinInferenceGraph.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Protein Inference - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void legendLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_legendLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_legendLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void legendLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_legendLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_legendLabelMouseExited

    /**
     * Show the graph color legend.
     *
     * @param evt
     */
    private void legendLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_legendLabelMouseReleased
        new ProteinInferenceGraphLegendDialog(parentDialog, false);
    }//GEN-LAST:event_legendLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void selectAllLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_selectAllLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void selectAllLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_selectAllLabelMouseExited

    /**
     * Select all the proteins and peptides.
     * 
     * @param evt 
     */
    private void selectAllLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseReleased
        for (String tempNode : nodes) {
            visualizationViewer.getPickedVertexState().pick(tempNode, true);
        }
    }//GEN-LAST:event_selectAllLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void showProteinLabelsLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_showProteinLabelsLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_showProteinLabelsLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void showProteinLabelsLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_showProteinLabelsLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_showProteinLabelsLabelMouseExited

    /**
     * Update the plot.
     * 
     * @param evt 
     */
    private void showProteinLabelsLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_showProteinLabelsLabelMouseReleased
        showProteinLabels = !showProteinLabels;
        this.repaint();
    }//GEN-LAST:event_showProteinLabelsLabelMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void showPeptideLabelsLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_showPeptideLabelsLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_showPeptideLabelsLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void showPeptideLabelsLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_showPeptideLabelsLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_showPeptideLabelsLabelMouseExited

    /**
     * Update the plot.
     * 
     * @param evt 
     */
    private void showPeptideLabelsLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_showPeptideLabelsLabelMouseReleased
        showPeptideLabels = !showPeptideLabels;
        this.repaint();
    }//GEN-LAST:event_showPeptideLabelsLabelMouseReleased

    /**
     * Get the selected node as an HTML string.
     *
     * @return the selected node as an HTML string
     */
    private String getSelectedValuesAsString() {

        String proteinSelection = "";
        String peptideSelection = "";

        int proteinCount = 0;
        int peptideCount = 0;

        for (String tempNode : selectedNodes) { // @TODO: clean up and reformat this code
            if (tempNode.startsWith("Protein")) {
                if (!proteinSelection.isEmpty()) {
                    proteinSelection += "<br>";
                }
                if (nodeToolTips != null && nodeToolTips.containsKey(tempNode)) {
                    proteinSelection += ++proteinCount + ": " + convertHtmlTooltip(nodeToolTips.get(tempNode));
                } else {
                    proteinSelection += ++proteinCount + ": " + tempNode.substring(tempNode.indexOf(" ") + 1);
                }
            } else if (tempNode.startsWith("Peptide")) {
                if (!peptideSelection.isEmpty()) {
                    peptideSelection += "<br>";
                }
                if (nodeToolTips != null && nodeToolTips.containsKey(tempNode)) {
                    peptideSelection += ++peptideCount + ": " + convertHtmlTooltip(nodeToolTips.get(tempNode));
                } else {
                    peptideSelection += ++peptideCount + ": " + tempNode.substring(tempNode.indexOf(" ") + 1);
                }
            }
        }

        for (String tempNode : selectedNeighborNodes) {
            if (!selectedNodes.contains(tempNode)) {
                if (tempNode.startsWith("Protein")) {
                    if (!proteinSelection.isEmpty()) {
                        proteinSelection += "<br>";
                    }
                    if (nodeToolTips != null && nodeToolTips.containsKey(tempNode)) {
                        proteinSelection += ++proteinCount + ": " + convertHtmlTooltip(nodeToolTips.get(tempNode));
                    } else {
                        proteinSelection += ++proteinCount + ": " + tempNode.substring(tempNode.indexOf(" ") + 1);
                    }
                } else if (tempNode.startsWith("Peptide")) {
                    if (!peptideSelection.isEmpty()) {
                        peptideSelection += "<br>";
                    }
                    if (nodeToolTips != null && nodeToolTips.containsKey(tempNode)) {
                        peptideSelection += ++peptideCount + ": " + convertHtmlTooltip(nodeToolTips.get(tempNode));
                    } else {
                        peptideSelection += ++peptideCount + ": " + tempNode.substring(tempNode.indexOf(" ") + 1);
                    }
                }
            }
        }

        return "<html><b>Proteins:</b><br>" + proteinSelection + "<br><br><b>Peptides:</b><br>" + peptideSelection + "</html>";
    }

    /**
     * Replaces the HTML tags in a node tooltip.
     *
     * @param tooltipAsHtml the original HTML tooltip
     * @return the new tooltip without HTML tags
     */
    private String convertHtmlTooltip(String tooltipAsHtml) {
        String temp = tooltipAsHtml.replaceAll(Pattern.quote("<br>"), " - ");
        temp = temp.replaceAll(Pattern.quote("-  -"), "-");
        temp = temp.replaceAll(Pattern.quote("<html>"), "");
        temp = temp.replaceAll(Pattern.quote("</html>"), "");
        return temp;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton allRadioButton;
    private javax.swing.ButtonGroup annotationButtonGroup;
    private javax.swing.JRadioButton evidenceRadioButton;
    private javax.swing.JLabel exportLabel;
    private javax.swing.JPanel graphPanel;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JCheckBox highlightCheckBox;
    private javax.swing.JLabel layoutLabel;
    private javax.swing.JLabel legendLabel;
    private javax.swing.JRadioButton nodeTypeRadioButton;
    private javax.swing.JLabel peptideCountLabel;
    private javax.swing.JLabel peptideCountValueLabel;
    private javax.swing.JLabel proteinCountLabel;
    private javax.swing.JLabel proteinCountValueLabel;
    private javax.swing.JLabel selectAllLabel;
    private javax.swing.ButtonGroup selectionButtonGroup;
    private javax.swing.JPanel selectionPanel;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel showPeptideLabelsLabel;
    private javax.swing.JLabel showProteinLabelsLabel;
    private javax.swing.JRadioButton uniqueRadioButton;
    private javax.swing.JRadioButton validationStatusRadioButton;
    // End of variables declaration//GEN-END:variables

    /**
     * The protein inference vertex renderer.
     */
    public class ProteinInferenceVertexRenderer implements Renderer.Vertex<String, String> {

        @Override
        public void paintVertex(RenderContext<String, String> rc, Layout<String, String> layout, String vertex) {
            GraphicsDecorator graphicsContext = rc.getGraphicsContext();
            Point2D center = layout.transform(vertex);
            Shape shape = null;
            Color color = null;

            int alpha = 255;

            // check if the node is selected or should be semi-transparent
            if (!selectedNeighborNodes.isEmpty()) {
                if (!selectedNeighborNodes.contains(vertex) && !selectedNodes.contains(vertex)) {
                    alpha = 50;
                }
            } else {
                if (!selectedNodes.contains(vertex)) {
                    alpha = 50;
                }
            }

            // draw a highlight to indicate the selected nodes
            if (selectedNodes.contains(vertex) && highlightCheckBox.isSelected()) {
                if (vertex.startsWith("Protein")) {
                    shape = new Ellipse2D.Double(center.getX() - 22, center.getY() - 22, 44, 44);
                } else if (vertex.startsWith("Peptide")) {
                    shape = new Ellipse2D.Double(center.getX() - 14, center.getY() - 14, 28, 28);
                }
                graphicsContext.setPaint(Color.ORANGE);
                graphicsContext.fill(shape);
            }

            // draw a highlight indicating the validation or evidence level
            boolean highlightAdded = false;
            if (!nodeTypeRadioButton.isSelected()) {

                if (nodeProperties.get(vertex) != null) {

                    highlightAdded = true;
                    String[] properties = nodeProperties.get(vertex).split("\\|");

                    if (validationStatusRadioButton.isSelected()) {
                        int validationLevel = Integer.parseInt(properties[0]);

                        if (validationLevel == 0) { // not validated
                            color = new Color(255, 0, 0, alpha);
                        } else if (validationLevel == 1) { // doubtful
                            color = new Color(255, 204, 0, alpha);
                        } else if (validationLevel == 2) { // confident
                            color = new Color(110, 196, 97, alpha);
                        } else { // unknown...
                            color = new Color(200, 200, 200, alpha);
                        }
                    } else if (properties.length > 1) {
                        int evidenceLevel = Integer.parseInt(properties[1]);

                        if (evidenceLevel == 1) { // protein
                            color = new Color(110, 196, 97, alpha);
                        } else if (evidenceLevel == 2) { // transcript
                            color = new Color(255, 204, 0, alpha);
                        } else if (evidenceLevel == 3) { // homology
                            color = new Color(110, 196, 197, alpha);
                        } else if (evidenceLevel == 4) { // predicted
                            color = new Color(247, 53, 233, alpha);
                        } else if (evidenceLevel == 5) { // uncertain
                            color = new Color(255, 0, 0, alpha);
                        } else { // unknown...
                            color = new Color(200, 200, 200, alpha);
                        }
                    } else {
                        color = new Color(200, 200, 200, alpha);
                    }
                }
            }

            // draw the actual vertex
            if (vertex.startsWith("Protein")) {
                shape = new Ellipse2D.Double(center.getX() - 18, center.getY() - 18, 36, 36);
                if (!highlightAdded) {
                    color = new Color(255, 0, 0, alpha);
                }
            } else if (vertex.startsWith("Peptide")) {
                shape = new Ellipse2D.Double(center.getX() - 10, center.getY() - 10, 20, 20);
                if (!highlightAdded) {
                    color = new Color(0, 0, 255, alpha);
                }
            }

            graphicsContext.setPaint(color);
            graphicsContext.fill(shape);

            // draw a thin border around the vertex
            color = new Color(150, 150, 150, alpha);
            graphicsContext.setPaint(color);
            graphicsContext.draw(shape);

            // draw a thin border around the highlight
            if (selectedNodes.contains(vertex) && highlightCheckBox.isSelected()) {

                if (vertex.startsWith("Protein")) {
                    shape = new Ellipse2D.Double(center.getX() - 22, center.getY() - 22, 44, 44);
                } else if (vertex.startsWith("Peptide")) {
                    shape = new Ellipse2D.Double(center.getX() - 14, center.getY() - 14, 28, 28);
                }

                color = new Color(150, 150, 150, alpha);
                graphicsContext.setPaint(color);
                graphicsContext.draw(shape);
            }
        }
    }
}
