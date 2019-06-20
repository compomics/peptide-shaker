package eu.isas.peptideshaker.gui.protein_inference;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.experiment.io.biology.protein.Header;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.google.common.collect.Sets;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.data.Chromosome;
import no.uib.jsparklines.extra.ChromosomeTableCellRenderer;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * This dialog allows the user to resolve manually some protein inference
 * issues.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinInferenceDialog extends javax.swing.JDialog {

    /**
     * The inspected protein match.
     */
    private ProteinMatch inspectedMatch;
    /**
     * The protein accessions.
     */
    private String[] accessions;
    /**
     * The detected unique matches (if any).
     */
    private long[] uniqueMatches;
    /**
     * Associated matches presenting the same proteins or a share.
     */
    private long[] associatedMatches;
    /**
     * The PeptideShaker parent frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The gene maps
     */
    private GeneMaps geneMaps;
    /**
     * The candidate protein table column header tooltips.
     */
    private ArrayList<String> candidateProteinsTableToolTips;
    /**
     * The unique hits table column header tooltips.
     */
    private ArrayList<String> uniqueHitsTableToolTips;
    /**
     * The related hits table column header tooltips.
     */
    private ArrayList<String> relatedHitsTableToolTips;
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The selected main match.
     */
    private String previousMainMatch;

    /**
     * Creates new form ProteinInferenceDialog.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     * @param geneMaps the gene maps
     * @param matchKey the match key
     * @param identification the identifications
     */
    public ProteinInferenceDialog(PeptideShakerGUI peptideShakerGUI, GeneMaps geneMaps, long matchKey, Identification identification) {
        super(peptideShakerGUI, true);

        this.identification = identification;
        this.peptideShakerGUI = peptideShakerGUI;
        this.geneMaps = geneMaps;

        try {

            this.inspectedMatch = identification.getProteinMatch(matchKey);
            previousMainMatch = this.inspectedMatch.getLeadingAccession();

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            this.dispose();
        }

        accessions = inspectedMatch.getAccessions();

        uniqueMatches = Arrays.stream(accessions)
                .mapToLong(accession -> ExperimentObject.asLong(accession))
                .filter(key -> identification.getProteinIdentification().contains(key))
                .toArray();

        associatedMatches = Arrays.stream(accessions)
                .flatMap(accession -> identification.getProteinMap().get(accession).stream())
                .distinct()
                .mapToLong(key -> key)
                .toArray();

        initComponents();

        // make the tabs go from right to left
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // make sure that the scroll panes are see-through
        proteinMatchJScrollPane.getViewport().setOpaque(false);
        uniqueHitsJScrollPane.getViewport().setOpaque(false);
        relatedHitsJScrollPane.getViewport().setOpaque(false);

        groupClassJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        PSParameter psParameter = new PSParameter();
        try {
            psParameter = (PSParameter) this.inspectedMatch.getUrParam(psParameter);
            // the index should be set in the design according to the PSParameter class static fields!
            groupClassJComboBox.setSelectedIndex(psParameter.getProteinInferenceGroupClass());
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        // set up the table column properties
        setColumnProperies();

        // update the panel border with the nubmer of rows in the table
        ((TitledBorder) proteinMatchJPanel.getBorder()).setTitle("Candidate Proteins (" + accessions.length + ")");
        ((TitledBorder) uniqueHitsJPanel.getBorder()).setTitle("Unique Hits (" + uniqueMatches.length + ")");
        ((TitledBorder) relatedHitsJPanel.getBorder()).setTitle("Related Hits (" + associatedMatches.length + ")");

        // set up the protein inference graph
        drawGraph();

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Draw the protein inference graph.
     */
    private void drawGraph() {

        ArrayList<String> nodes = new ArrayList<>();
        HashMap<String, String> nodeProperties = new HashMap<>();
        HashMap<String, String> edgeProperties = new HashMap<>();
        HashMap<String, String> nodeToolTips = new HashMap<>();
        HashMap<String, ArrayList<String>> edges = new HashMap<>();
        ArrayList<String> selectedNodes = new ArrayList<>();

        long[] peptideKeys = inspectedMatch.getPeptideMatchesKeys();

        try {
            for (long tempPeptideKey : peptideKeys) {
                addPeptide(tempPeptideKey, nodeToolTips, nodes, nodeProperties, selectedNodes, edges, edgeProperties);
            }
        } catch (Exception e) {
            e.printStackTrace(); // @TODO: better error handling!
        }

        graphInnerPanel.add(new ProteinInferenceGraphPanel(this, graphInnerPanel, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(),
                peptideShakerGUI.getLastSelectedFolder(), nodes, edges, nodeProperties, edgeProperties, nodeToolTips, selectedNodes));
    }

    /**
     * Add a peptide and the corresponding proteins to the graph.
     *
     * @param peptideKey the peptide key
     * @param nodeToolTips the node tool tips
     * @param nodes the nodes
     * @param nodeProperties the node properties
     * @param selectedNodes the selected nodes
     * @param edges the edges
     * @param edgeProperties the edge properties
     */
    private void addPeptide(long peptideKey, HashMap<String, String> nodeToolTips, ArrayList<String> nodes, HashMap<String, String> nodeProperties,
            ArrayList<String> selectedNodes, HashMap<String, ArrayList<String>> edges, HashMap<String, String> edgeProperties) {

        ProteinDetailsProvider proteinDetailsProvider = peptideShakerGUI.getProteinDetailsProvider();
        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
        String peptideNodeName = "Peptide " + peptideKey;

        if (!nodes.contains(peptideNodeName)) {

            HashSet<String> accessionsSet = Sets.newHashSet(accessions);

            // get the match validation level
            PSParameter peptideMatchParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
            String matchValidationLevel = "Validation: " + peptideMatchParameter.getMatchValidationLevel();

            // get the peptide node tooltip
            String peptideTooltip = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideMatch, true, false, true);
            peptideTooltip = "<html>" + peptideTooltip + "<br><br>" + matchValidationLevel + "</html>";
            nodeToolTips.put(peptideNodeName, peptideTooltip);

            // add the node
            nodes.add(peptideNodeName);
            nodeProperties.put(peptideNodeName, "" + peptideMatchParameter.getMatchValidationLevel().getIndex());

            // iterate the proteins
            for (String tempProteinAccession : peptideMatch.getPeptide().getProteinMapping().keySet()) {

                String proteinNodeKey = "Protein " + tempProteinAccession;

                if (!nodes.contains(proteinNodeKey)) {

                    // add the node
                    nodes.add(proteinNodeKey);

                    if (accessionsSet.contains(tempProteinAccession)) {

                        selectedNodes.add(proteinNodeKey);

                    }

                    String nodeProperty = "";

                    // get the match validation level
                    long tempKey = ExperimentObject.asLong(tempProteinAccession);
                    ProteinMatch proteinMatch = identification.getProteinMatch(tempKey);

                    if (proteinMatch != null) {

                        PSParameter proteinMatchParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                        nodeProperty += proteinMatchParameter.getMatchValidationLevel().getIndex();
                        matchValidationLevel = "Validation: " + proteinMatchParameter.getMatchValidationLevel();

                    } else {

                        nodeProperty += -1;
                        matchValidationLevel = "Validation: (not available)";

                    }

                    // get the protein evidence level
                    String proteinEvidenceLevel = null;
                    Integer level = proteinDetailsProvider.getProteinEvidence(tempProteinAccession);

                    if (level != null) {

                        nodeProperty += "|" + level;

                        try {

                            proteinEvidenceLevel = "Evidence: " + Header.getProteinEvidencAsString(level);

                        } catch (NumberFormatException e) {
                            // ignore
                        }

                    }

                    if (proteinEvidenceLevel == null) {

                        proteinEvidenceLevel = "Evidence: (not available)";

                    }

                    // add the node property
                    nodeProperties.put(proteinNodeKey, nodeProperty);

                    // add the tooltip
                    nodeToolTips.put(proteinNodeKey, "<html>" + tempProteinAccession
                            + "<br>" + proteinDetailsProvider.getSimpleDescription(tempProteinAccession)
                            + "<br><br>" + matchValidationLevel
                            + "<br>" + proteinEvidenceLevel
                            + "<html>");

                    // add any new peptides for the proteins
                    if (proteinMatch != null) {

                        long[] secondaryPeptides = proteinMatch.getPeptideMatchesKeys();

                        for (long tempPeptideKey : secondaryPeptides) {

                            addPeptide(tempPeptideKey, nodeToolTips, nodes, nodeProperties, selectedNodes, edges, edgeProperties);

                        }

                    } else {

                        if (identification.getProteinMap().containsKey(tempProteinAccession)) {
                        
                            identification.getProteinMap().get(tempProteinAccession).stream()
                                    .flatMap(key -> Arrays.stream(identification.getProteinMatch(key).getPeptideMatchesKeys()).boxed())
                                    .distinct()
                                    .forEach(tempPeptideKey -> addPeptide(tempPeptideKey, nodeToolTips, nodes, nodeProperties, selectedNodes, edges, edgeProperties));
                        }
                    }
                }

                ArrayList<String> tempEdges = edges.get(peptideNodeName);
                if (tempEdges == null) {
                    tempEdges = new ArrayList<>();
                }
                if (!tempEdges.contains(proteinNodeKey)) {
                    tempEdges.add(proteinNodeKey);

                    boolean enzymatic = false;
                    DigestionParameters digestionPreferences = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getDigestionParameters();

                    if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                        enzymatic = PeptideUtils.isEnzymatic(peptideMatch.getPeptide(), peptideShakerGUI.getSequenceProvider(), digestionPreferences.getEnzymes());

                    }

                    edgeProperties.put(peptideNodeName + "|" + proteinNodeKey, Boolean.toString(enzymatic));

                }

                edges.put(peptideNodeName, tempEdges);

            }
        }
    }

    /**
     * Set the properties for the columns in the results tables.
     */
    private void setColumnProperies() {

        proteinMatchTable.getTableHeader().setReorderingAllowed(false);
        uniqueHitsTable.getTableHeader().setReorderingAllowed(false);
        relatedHitsTable.getTableHeader().setReorderingAllowed(false);

        proteinMatchTable.getColumn("  ").setMinWidth(50);
        proteinMatchTable.getColumn("  ").setMaxWidth(50);
        proteinMatchTable.getColumn("Gene").setMinWidth(90);
        proteinMatchTable.getColumn("Gene").setMaxWidth(90);
        proteinMatchTable.getColumn("Chr").setMinWidth(50);
        proteinMatchTable.getColumn("Chr").setMaxWidth(50);
        proteinMatchTable.getColumn("Evidence").setMinWidth(90);
        proteinMatchTable.getColumn("Evidence").setMaxWidth(90);
        proteinMatchTable.getColumn("Enz").setMinWidth(50);
        proteinMatchTable.getColumn("Enz").setMaxWidth(50);

        // set the preferred size of the accession column
        Integer width = ProteinTableModel.getPreferredAccessionColumnWidth(proteinMatchTable, proteinMatchTable.getColumn("Accession").getModelIndex(), 2, peptideShakerGUI.getMetrics().getMaxProteinAccessionLength());
        if (width != null) {
            proteinMatchTable.getColumn("Accession").setMinWidth(width);
            proteinMatchTable.getColumn("Accession").setMaxWidth(width);
        } else {
            proteinMatchTable.getColumn("Accession").setMinWidth(15);
            proteinMatchTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }

        // the validated column
        uniqueHitsTable.getColumn(" ").setMaxWidth(30);
        relatedHitsTable.getColumn(" ").setMaxWidth(30);
        uniqueHitsTable.getColumn(" ").setMinWidth(30);
        relatedHitsTable.getColumn(" ").setMinWidth(30);

        proteinMatchTable.getColumn("").setMaxWidth(50);
        uniqueHitsTable.getColumn("").setMaxWidth(50);
        relatedHitsTable.getColumn("").setMaxWidth(50);
        proteinMatchTable.getColumn("").setMinWidth(50);
        uniqueHitsTable.getColumn("").setMinWidth(50);
        relatedHitsTable.getColumn("").setMinWidth(50);

        // the score and confidence columns
        uniqueHitsTable.getColumn("Confidence").setMaxWidth(90);
        uniqueHitsTable.getColumn("Confidence").setMinWidth(90);

        relatedHitsTable.getColumn("Confidence").setMaxWidth(90);
        relatedHitsTable.getColumn("Confidence").setMinWidth(90);

        // change the cell renderer to fix a problem in Nimbus and alternating row colors
        proteinMatchTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());

        proteinMatchTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(
                TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        proteinMatchTable.getColumn("Chr").setCellRenderer(new ChromosomeTableCellRenderer(Color.WHITE, Color.BLACK));
        proteinMatchTable.getColumn("Enz").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green-new.png")),
                null,
                "Enzymatic", "Not Enzymatic"));

        uniqueHitsTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(
                TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        uniqueHitsTable.getColumn(" ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        uniqueHitsTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) uniqueHitsTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);

        relatedHitsTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(
                TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        relatedHitsTable.getColumn(" ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        relatedHitsTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedHitsTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);

        // set up the table header tooltips
        candidateProteinsTableToolTips = new ArrayList<>();
        candidateProteinsTableToolTips.add(null);
        candidateProteinsTableToolTips.add("Protein Group Representative");
        candidateProteinsTableToolTips.add("Protein Accession");
        candidateProteinsTableToolTips.add("Protein Description");
        candidateProteinsTableToolTips.add("Gene Name");
        candidateProteinsTableToolTips.add("Chromosome Number");
        candidateProteinsTableToolTips.add("Protein Evidence Level");
        candidateProteinsTableToolTips.add("Contains Enzymatic Peptides");

        uniqueHitsTableToolTips = new ArrayList<>();
        uniqueHitsTableToolTips.add(null);
        uniqueHitsTableToolTips.add("Protein Accession(s)");
        uniqueHitsTableToolTips.add("Protein Confidence");
        uniqueHitsTableToolTips.add("Validated");

        relatedHitsTableToolTips = new ArrayList<>();
        relatedHitsTableToolTips.add(null);
        relatedHitsTableToolTips.add("Protein Accession(s)");
        relatedHitsTableToolTips.add("Protein Confidence");
        relatedHitsTableToolTips.add("Validated");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        detailsPanel = new javax.swing.JPanel();
        proteinMatchJPanel = new javax.swing.JPanel();
        proteinMatchJScrollPane = new javax.swing.JScrollPane();
        proteinMatchTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) candidateProteinsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        uniqueHitsJPanel = new javax.swing.JPanel();
        uniqueHitsJScrollPane = new javax.swing.JScrollPane();
        uniqueHitsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) uniqueHitsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        relatedHitsJPanel = new javax.swing.JPanel();
        relatedHitsJScrollPane = new javax.swing.JScrollPane();
        relatedHitsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) relatedHitsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        groupDetalsJPanel = new javax.swing.JPanel();
        groupClassJComboBox = new javax.swing.JComboBox();
        helpJButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        graphPanel = new javax.swing.JPanel();
        graphInnerPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Protein Inference - Protein Level");
        setMinimumSize(new java.awt.Dimension(800, 650));

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        tabbedPane.setBackground(new java.awt.Color(230, 230, 230));
        tabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        detailsPanel.setBackground(new java.awt.Color(230, 230, 230));
        detailsPanel.setOpaque(false);

        proteinMatchJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Candidate Proteins"));
        proteinMatchJPanel.setOpaque(false);

        proteinMatchTable.setModel(new MatchTable());
        proteinMatchTable.setOpaque(false);
        proteinMatchTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinMatchTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinMatchTableMouseReleased(evt);
            }
        });
        proteinMatchTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                proteinMatchTableMouseMoved(evt);
            }
        });
        proteinMatchJScrollPane.setViewportView(proteinMatchTable);

        javax.swing.GroupLayout proteinMatchJPanelLayout = new javax.swing.GroupLayout(proteinMatchJPanel);
        proteinMatchJPanel.setLayout(proteinMatchJPanelLayout);
        proteinMatchJPanelLayout.setHorizontalGroup(
            proteinMatchJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinMatchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinMatchJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 852, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinMatchJPanelLayout.setVerticalGroup(
            proteinMatchJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinMatchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinMatchJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                .addContainerGap())
        );

        uniqueHitsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Unique Hits"));
        uniqueHitsJPanel.setOpaque(false);

        uniqueHitsTable.setModel(new UniqueMatches());
        uniqueHitsTable.setOpaque(false);
        uniqueHitsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                uniqueHitsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                uniqueHitsTableMouseReleased(evt);
            }
        });
        uniqueHitsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                uniqueHitsTableMouseMoved(evt);
            }
        });
        uniqueHitsJScrollPane.setViewportView(uniqueHitsTable);

        javax.swing.GroupLayout uniqueHitsJPanelLayout = new javax.swing.GroupLayout(uniqueHitsJPanel);
        uniqueHitsJPanel.setLayout(uniqueHitsJPanelLayout);
        uniqueHitsJPanelLayout.setHorizontalGroup(
            uniqueHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uniqueHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(uniqueHitsJScrollPane)
                .addContainerGap())
        );
        uniqueHitsJPanelLayout.setVerticalGroup(
            uniqueHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uniqueHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(uniqueHitsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                .addGap(14, 14, 14))
        );

        relatedHitsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Related Hits"));
        relatedHitsJPanel.setOpaque(false);

        relatedHitsTable.setModel(new AssociatedMatches());
        relatedHitsTable.setOpaque(false);
        relatedHitsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                relatedHitsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedHitsTableMouseReleased(evt);
            }
        });
        relatedHitsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                relatedHitsTableMouseMoved(evt);
            }
        });
        relatedHitsJScrollPane.setViewportView(relatedHitsTable);

        javax.swing.GroupLayout relatedHitsJPanelLayout = new javax.swing.GroupLayout(relatedHitsJPanel);
        relatedHitsJPanel.setLayout(relatedHitsJPanelLayout);
        relatedHitsJPanelLayout.setHorizontalGroup(
            relatedHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(relatedHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedHitsJScrollPane)
                .addContainerGap())
        );
        relatedHitsJPanelLayout.setVerticalGroup(
            relatedHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(relatedHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedHitsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                .addContainerGap())
        );

        groupDetalsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Group Type"));
        groupDetalsJPanel.setOpaque(false);

        groupClassJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Single Protein", "Related Proteins", "Related and Unrelated Proteins", "Unrelated Proteins" }));

        helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        helpJButton.setToolTipText("Help");
        helpJButton.setBorder(null);
        helpJButton.setBorderPainted(false);
        helpJButton.setContentAreaFilled(false);
        helpJButton.setFocusable(false);
        helpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        helpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        helpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                helpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                helpJButtonMouseExited(evt);
            }
        });
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpJButtonActionPerformed(evt);
            }
        });

        okButton.setText("Update");
        okButton.setToolTipText("<html>\nUpdate the protein group type and<br>\nthe protein group representative\n</html>");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout groupDetalsJPanelLayout = new javax.swing.GroupLayout(groupDetalsJPanel);
        groupDetalsJPanel.setLayout(groupDetalsJPanelLayout);
        groupDetalsJPanelLayout.setHorizontalGroup(
            groupDetalsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupDetalsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupClassJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(helpJButton)
                .addGap(18, 18, 18))
        );
        groupDetalsJPanelLayout.setVerticalGroup(
            groupDetalsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupDetalsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(groupDetalsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(groupClassJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okButton)
                    .addComponent(helpJButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout detailsPanelLayout = new javax.swing.GroupLayout(detailsPanel);
        detailsPanel.setLayout(detailsPanelLayout);
        detailsPanelLayout.setHorizontalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinMatchJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(uniqueHitsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(relatedHitsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(groupDetalsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        detailsPanelLayout.setVerticalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupDetalsJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinMatchJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uniqueHitsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(relatedHitsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPane.addTab("Group Details", detailsPanel);

        graphPanel.setBackground(new java.awt.Color(230, 230, 230));
        graphPanel.setOpaque(false);

        graphInnerPanel.setBackground(new java.awt.Color(255, 255, 255));
        graphInnerPanel.setLayout(new javax.swing.BoxLayout(graphInnerPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout graphPanelLayout = new javax.swing.GroupLayout(graphPanel);
        graphPanel.setLayout(graphPanelLayout);
        graphPanelLayout.setHorizontalGroup(
            graphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(graphPanelLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(graphInnerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 874, Short.MAX_VALUE)
                .addGap(15, 15, 15))
        );
        graphPanelLayout.setVerticalGroup(
            graphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, graphPanelLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(graphInnerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE)
                .addGap(15, 15, 15))
        );

        tabbedPane.addTab("Protein Graph", graphPanel);

        tabbedPane.setSelectedIndex(1);

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane)
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane)
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

    /**
     * Update the protein table according to the protein inference selection.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        PSParameter psParameter = new PSParameter();

        try {
            psParameter = (PSParameter) ((ProteinMatch) identification.retrieveObject(inspectedMatch.getKey())).getUrParam(psParameter);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            this.dispose();
            return;
        }
        if (!inspectedMatch.getLeadingAccession().equals(previousMainMatch)
                || groupClassJComboBox.getSelectedIndex() != psParameter.getProteinInferenceGroupClass()) {
            try {
                psParameter.setProteinInferenceClass(groupClassJComboBox.getSelectedIndex());
                peptideShakerGUI.updateMainMatch(inspectedMatch.getLeadingAccession(), groupClassJComboBox.getSelectedIndex());
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            peptideShakerGUI.setDataSaved(false);
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Sets the main match if the main match column is selected, or opens the
     * protein web link if the accession number column is selected.
     *
     * @param evt
     */
    private void proteinMatchTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinMatchTableMouseReleased

        int row = proteinMatchTable.rowAtPoint(evt.getPoint());
        int column = proteinMatchTable.columnAtPoint(evt.getPoint());

        if (row != -1) {

            if (column == 1) {
                try {
                    inspectedMatch.setLeadingAccession(accessions[row]);
                    peptideShakerGUI.getIdentificationFeaturesGenerator().updateCoverableAA(inspectedMatch.getKey());
                    peptideShakerGUI.getIdentificationFeaturesGenerator().updateSequenceCoverage(inspectedMatch.getKey());
                    peptideShakerGUI.getIdentificationFeaturesGenerator().updateObservableCoverage(inspectedMatch.getKey());
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
                proteinMatchTable.revalidate();
                proteinMatchTable.repaint();
            } else if (column == 2) {

                // open protein link in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) proteinMatchTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) proteinMatchTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }

            // update the selection in the graph
            // @TODO: selected the currently selected nodes in the graph
        }
    }//GEN-LAST:event_proteinMatchTableMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_helpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_helpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/ProteinInference.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Protein Inference - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_helpJButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void proteinMatchTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinMatchTableMouseMoved
        int row = proteinMatchTable.rowAtPoint(evt.getPoint());
        int column = proteinMatchTable.columnAtPoint(evt.getPoint());

        proteinMatchTable.setToolTipText(null);

        if (column == 2 && proteinMatchTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinMatchTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinMatchTable.getColumn("Description").getModelIndex() && proteinMatchTable.getValueAt(row, column) != null) {
            if (GuiUtilities.getPreferredWidthOfCell(proteinMatchTable, row, column) > proteinMatchTable.getColumn("Description").getWidth()) {
                proteinMatchTable.setToolTipText("" + proteinMatchTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinMatchTableMouseMoved

    /**
     * Changes the cursor back to the default cursor a hand.
     *
     * @param evt
     */
    private void proteinMatchTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinMatchTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinMatchTableMouseExited

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void uniqueHitsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uniqueHitsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_uniqueHitsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void uniqueHitsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uniqueHitsTableMouseMoved
        int row = uniqueHitsTable.rowAtPoint(evt.getPoint());
        int column = uniqueHitsTable.columnAtPoint(evt.getPoint());

        if (column == 1 && uniqueHitsTable.getValueAt(row, column) != null) {

            String tempValue = (String) uniqueHitsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("a href=") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_uniqueHitsTableMouseMoved

    /**
     * Open the protein html links.
     *
     * @param evt
     */
    private void uniqueHitsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uniqueHitsTableMouseReleased
        int row = uniqueHitsTable.getSelectedRow();
        int column = uniqueHitsTable.getSelectedColumn();

        if (row != -1) {

            if (column == 1) {

                // open protein links in web browser
                if (evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) uniqueHitsTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) uniqueHitsTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_uniqueHitsTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void relatedHitsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedHitsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedHitsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void relatedHitsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedHitsTableMouseMoved
        int row = relatedHitsTable.rowAtPoint(evt.getPoint());
        int column = relatedHitsTable.columnAtPoint(evt.getPoint());

        if (column == 1 && relatedHitsTable.getValueAt(row, column) != null) {

            String tempValue = (String) relatedHitsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("a href=") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_relatedHitsTableMouseMoved

    /**
     * Open the protein html links.
     *
     * @param evt
     */
    private void relatedHitsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedHitsTableMouseReleased
        int row = relatedHitsTable.getSelectedRow();
        int column = relatedHitsTable.getSelectedColumn();

        if (row != -1) {

            if (column == 1) {

                // open protein links in web browser
                if (evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) relatedHitsTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) relatedHitsTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_relatedHitsTableMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JPanel detailsPanel;
    private javax.swing.JPanel graphInnerPanel;
    private javax.swing.JPanel graphPanel;
    private javax.swing.JComboBox groupClassJComboBox;
    private javax.swing.JPanel groupDetalsJPanel;
    private javax.swing.JButton helpJButton;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel proteinMatchJPanel;
    private javax.swing.JScrollPane proteinMatchJScrollPane;
    private javax.swing.JTable proteinMatchTable;
    private javax.swing.JPanel relatedHitsJPanel;
    private javax.swing.JScrollPane relatedHitsJScrollPane;
    private javax.swing.JTable relatedHitsTable;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JPanel uniqueHitsJPanel;
    private javax.swing.JScrollPane uniqueHitsJScrollPane;
    private javax.swing.JTable uniqueHitsTable;
    // End of variables declaration//GEN-END:variables

    /**
     * Table model for the protein match table.
     */
    private class MatchTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return accessions.length;
        }

        @Override
        public int getColumnCount() {
            return 8;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "  ";
                case 2:
                    return "Accession";
                case 3:
                    return "Description";
                case 4:
                    return "Gene";
                case 5:
                    return "Chr";
                case 6:
                    return "Evidence";
                case 7:
                    return "Enz";
                default:
                    return " ";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return inspectedMatch.getLeadingAccession().equals(accessions[row]);
                case 2:
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getDatabaseLink(accessions[row]);
                case 3:
                    try {

                        String description = peptideShakerGUI.getProteinDetailsProvider().getSimpleDescription(accessions[row]);
                        // if description is not set, return the accession instead - fix for home made fasta headers
                        if (description == null || description.trim().isEmpty()) {
                            description = inspectedMatch.getLeadingAccession();
                        }
                        return description;
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return "Database Error";
                    }
                case 4:
                    try {
                        return peptideShakerGUI.getProteinDetailsProvider().getGeneName(accessions[row]);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return "Database Error";
                    }
                case 5:
                    try {
                        String geneName = peptideShakerGUI.getProteinDetailsProvider().getGeneName(accessions[row]);
                        String chromosomeNumber = geneMaps.getChromosome(geneName);
                        return new Chromosome(chromosomeNumber);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return "Database Error";
                    }
                case 6:
                    try {
                        Integer level = peptideShakerGUI.getProteinDetailsProvider().getProteinEvidence(accessions[row]);
                        if (level != null) {
                            try {
                                return Header.getProteinEvidencAsString(level);
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return "Database Error";
                    }
                case 7:
                    try {
                        return peptideShakerGUI.getIdentificationFeaturesGenerator().hasEnzymaticPeptides(inspectedMatch.getKey());
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return "Database Error";
                    }
                default:
                    return " ";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex < 4) {
                return getValueAt(0, columnIndex).getClass();
            } else {
                return String.class; // we need this as the gene name and protein evidence can be null
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }
    }

    /**
     * Table model for the unique matches table.
     */
    private class UniqueMatches extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return uniqueMatches.length;
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "Protein(s)";
                case 2:
                    return "Confidence";
                case 3:
                    return " ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            ProteinMatch proteinMatch = identification.getProteinMatch(uniqueMatches[row]);
            PSParameter psParameter = (PSParameter) (proteinMatch).getUrParam(PSParameter.dummy);

            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getDatabaseLinks(proteinMatch.getAccessions());
                case 2:
                    return psParameter.getConfidence();
                case 3:
                    return psParameter.getMatchValidationLevel().getIndex();
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    /**
     * Table model for the associated matches table.
     */
    private class AssociatedMatches extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return associatedMatches.length;
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "Protein(s)";
                case 2:
                    return "Confidence";
                case 3:
                    return " ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            ProteinMatch proteinMatch = identification.getProteinMatch(uniqueMatches[row]);
            PSParameter psParameter = (PSParameter) (proteinMatch).getUrParam(PSParameter.dummy);

            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return peptideShakerGUI.getDisplayFeaturesGenerator().getDatabaseLinks(proteinMatch.getAccessions());
                case 2:
                    return psParameter.getConfidence();
                case 3:
                    return psParameter.getMatchValidationLevel().getIndex();
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
