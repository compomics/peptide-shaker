package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.pdbfinder.FindPdbForUniprotAccessions;
import com.compomics.util.pdbfinder.pdb.PdbBlock;
import com.compomics.util.pdbfinder.pdb.PdbParameter;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.ProteinInferenceDialog;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.data.XYDataPoint;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The Protein Structures tab.
 * 
 * @author Harald Barsnes
 */
public class ProteinStructurePanel extends javax.swing.JPanel implements ProgressDialogParent {

    /**
     * The currently displayed PDB file.
     */
    private String currentlyDisplayedPdbFile;
    /**
     * If true, the protein selection in the protein structure tab is mirrored in 
     * the protein table in the overview tab.
     */
    private boolean updateOverviewPanel = true;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * The UniProt to PDB finder.
     */
    private FindPdbForUniprotAccessions uniProtPdb;
    /**
     * The PeptideShaker main frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The Jmol panel.
     */
    private JmolPanel jmolPanel;
    /**
     * A mapping of the protein table entries
     */
    private HashMap<Integer, String> proteinTableMap = new HashMap<Integer, String>();
    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The peptide table column header tooltips.
     */
    private ArrayList<String> peptideTableToolTips;
    /**
     * The pdb files table column header tooltips.
     */
    private ArrayList<String> pdbTableToolTips;
    /**
     * The pdb chains table column header tooltips.
     */
    private ArrayList<String> pdbChainsTableToolTips;
    /**
     * A mapping of the peptide table entries
     */
    private HashMap<String, String> peptideTableMap = new HashMap<String, String>();
    /**
     * If true Jmol is currently displaying a structure.
     */
    private boolean jmolStructureShown = false;
    /**
     * The current PDB chains.
     */
    private PdbBlock[] chains;
    /**
     * The amino acid sequence of the current chain.
     */
    private String chainSequence;
    /**
     * The current protein sequence.
     */
    private String proteinSequence;

    /** 
     * Creates a new ProteinPanel.
     * 
     * @param peptideShakerGUI the PeptideShaker main frame
     */
    public ProteinStructurePanel(PeptideShakerGUI peptideShakerGUI) {
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;

        jmolPanel = new JmolPanel();
        pdbPanel.add(jmolPanel);

        setTableProperties();

        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        pdbJScrollPane.getViewport().setOpaque(false);
        pdbChainsJScrollPane.getViewport().setOpaque(false);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // set up the table header tooltips
        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Inference");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Protein Seqeunce Coverage (%)");
        proteinTableToolTips.add("Protein emPAI Score");
        proteinTableToolTips.add("Number of Peptides");
        proteinTableToolTips.add("Number of Spectra");
        proteinTableToolTips.add("Protein Score");
        proteinTableToolTips.add("Protein Confidence");
        proteinTableToolTips.add("Validated");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Peptide Sequence");
        peptideTableToolTips.add("Peptide Start Index");
        peptideTableToolTips.add("Peptide End Index");
        peptideTableToolTips.add("Peptide Modifications");
        peptideTableToolTips.add("Alternative Protein Mappings for Peptide");
        peptideTableToolTips.add("In PDB Sequence");
        peptideTableToolTips.add("Validated");

        pdbTableToolTips = new ArrayList<String>();
        pdbTableToolTips.add(null);
        pdbTableToolTips.add("PDB Accession Number");
        pdbTableToolTips.add("PDB Title");
        pdbTableToolTips.add("Type of Structure");
        pdbTableToolTips.add("Number of Chains");

        pdbChainsTableToolTips = new ArrayList<String>();
        pdbChainsTableToolTips.add(null);
        pdbChainsTableToolTips.add("Chain Label");
        pdbChainsTableToolTips.add("Protein-PDB Alignment");
        pdbChainsTableToolTips.add("Protein Coverage for PDB Sequence");

        proteinTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMaxWidth(50);
        pdbMatchesJTable.getColumn(" ").setMaxWidth(50);
        pdbChainsJTable.getColumn(" ").setMaxWidth(50);
        pdbMatchesJTable.getColumn("PDB").setMaxWidth(50);
        pdbChainsJTable.getColumn("Chain").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);
        pdbMatchesJTable.getColumn(" ").setMinWidth(50);
        pdbChainsJTable.getColumn(" ").setMinWidth(50);
        pdbMatchesJTable.getColumn("PDB").setMinWidth(50);
        pdbChainsJTable.getColumn("Chain").setMinWidth(50);

        peptideTable.getColumn("PDB").setMinWidth(50);
        peptideTable.getColumn("PDB").setMaxWidth(50);
        peptideTable.getColumn("Start").setMinWidth(50);
        peptideTable.getColumn("Start").setMaxWidth(50);
        peptideTable.getColumn("End").setMinWidth(50);
        peptideTable.getColumn("End").setMaxWidth(50);

        pdbMatchesJTable.getColumn("Chains").setMinWidth(100);
        pdbMatchesJTable.getColumn("Chains").setMaxWidth(100);

        // the validated column
        proteinTable.getColumn("").setMaxWidth(30);
        peptideTable.getColumn("").setMaxWidth(30);
        proteinTable.getColumn("").setMinWidth(30);
        peptideTable.getColumn("").setMinWidth(30);

        // the protein inference column
        proteinTable.getColumn("PI").setMaxWidth(35);
        proteinTable.getColumn("PI").setMinWidth(35);

        // set table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        pdbMatchesJTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        pdbMatchesJTable.setAutoCreateRowSorter(true);

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
        proteinTable.getColumn("emPAI").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        proteinTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).setMinimumChartValue(5d);
        proteinTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        peptideTable.getColumn("PDB").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/pdb.png")),
                null,
                "Mapped to PDB Structure", null));
        peptideTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        pdbMatchesJTable.getColumn("PDB").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        pdbMatchesJTable.getColumn("Chains").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) pdbMatchesJTable.getColumn("Chains").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        pdbChainsJTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) pdbChainsJTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        pdbChainsJTable.getColumn("PDB-Protein").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(
                PlotOrientation.HORIZONTAL, 0.0, 100.0, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pdbStructureJPanel = new javax.swing.JPanel();
        pdbPanel = new javax.swing.JPanel();
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
        pdbMatchesJPanel = new javax.swing.JPanel();
        pdbJScrollPane = new javax.swing.JScrollPane();
        pdbMatchesJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) pdbTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        pdbChainsJPanel = new javax.swing.JPanel();
        pdbChainsJScrollPane = new javax.swing.JScrollPane();
        pdbChainsJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) pdbChainsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };

        setBackground(new java.awt.Color(255, 255, 255));

        pdbStructureJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PDB Structure"));
        pdbStructureJPanel.setOpaque(false);

        pdbPanel.setLayout(new javax.swing.BoxLayout(pdbPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout pdbStructureJPanelLayout = new javax.swing.GroupLayout(pdbStructureJPanel);
        pdbStructureJPanel.setLayout(pdbStructureJPanelLayout);
        pdbStructureJPanelLayout.setHorizontalGroup(
            pdbStructureJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pdbStructureJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdbPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                .addContainerGap())
        );
        pdbStructureJPanelLayout.setVerticalGroup(
            pdbStructureJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pdbStructureJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdbPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsJPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "PI", "Description", "Coverage", "emPAI", "#Peptides", "#Spectra", "Score", "Confidence [%]", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
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

        peptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidesJPanel.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Start", "End", "Modifications", "Other Protein(s)", "PDB", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
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
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
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
                .addComponent(peptideScrollPane)
                .addContainerGap())
        );
        peptidesJPanelLayout.setVerticalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addContainerGap())
        );

        pdbMatchesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PDB Matches"));
        pdbMatchesJPanel.setOpaque(false);

        pdbMatchesJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "PDB", "Title", "Type", "Chains"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
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
        pdbMatchesJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        pdbMatchesJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pdbMatchesJTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                pdbMatchesJTableMouseReleased(evt);
            }
        });
        pdbMatchesJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                pdbMatchesJTableMouseMoved(evt);
            }
        });
        pdbMatchesJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                pdbMatchesJTableKeyReleased(evt);
            }
        });
        pdbJScrollPane.setViewportView(pdbMatchesJTable);

        javax.swing.GroupLayout pdbMatchesJPanelLayout = new javax.swing.GroupLayout(pdbMatchesJPanel);
        pdbMatchesJPanel.setLayout(pdbMatchesJPanelLayout);
        pdbMatchesJPanelLayout.setHorizontalGroup(
            pdbMatchesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pdbMatchesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdbJScrollPane)
                .addContainerGap())
        );
        pdbMatchesJPanelLayout.setVerticalGroup(
            pdbMatchesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pdbMatchesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdbJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addContainerGap())
        );

        pdbChainsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PDB Chains"));
        pdbChainsJPanel.setOpaque(false);

        pdbChainsJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Chain", "PDB-Protein", "Coverage"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Object.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        pdbChainsJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        pdbChainsJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                pdbChainsJTableMouseReleased(evt);
            }
        });
        pdbChainsJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                pdbChainsJTableKeyReleased(evt);
            }
        });
        pdbChainsJScrollPane.setViewportView(pdbChainsJTable);

        javax.swing.GroupLayout pdbChainsJPanelLayout = new javax.swing.GroupLayout(pdbChainsJPanel);
        pdbChainsJPanel.setLayout(pdbChainsJPanelLayout);
        pdbChainsJPanelLayout.setHorizontalGroup(
            pdbChainsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pdbChainsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdbChainsJScrollPane)
                .addContainerGap())
        );
        pdbChainsJPanelLayout.setVerticalGroup(
            pdbChainsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pdbChainsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdbChainsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pdbMatchesJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(pdbChainsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(peptidesJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pdbStructureJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(proteinsJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinsJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pdbMatchesJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pdbChainsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(peptidesJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(pdbStructureJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pdbStructureJPanel.getAccessibleContext().setAccessibleName("Protein Details");
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Makes sure the cursor changes back to the default cursor when leaving the 
     * protein accession number column.
     * 
     * @param evt 
     */
    private void proteinTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_proteinTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * html link.
     *
     * @param evt
     */
    private void proteinTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseMoved
        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.columnAtPoint(evt.getPoint());

        if (column == 1 && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == 2 && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
}//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Update the protein selection and the corresponding tables.
     * 
     * @param evt 
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased
        proteinTableMouseReleased(null);
}//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Updates the PDB structure.
     * 
     * @param evt 
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased
        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            peptideTableMouseReleased(null);
        }
}//GEN-LAST:event_peptideTableKeyReleased

    /**
     * Update the PDB structure shown in the Jmol panel.
     * 
     * @param evt 
     */
    private void pdbMatchesJTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pdbMatchesJTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            pdbMatchesJTableMouseReleased(null);
        }
    }//GEN-LAST:event_pdbMatchesJTableKeyReleased

    /**
     * Update the protein selection and the corresponding tables.
     * 
     * @param evt 
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased
        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // find and store the protein sequence for later use
            String proteinKey = proteinTableMap.get(getProteinKey(proteinTable.getSelectedRow()));
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinIdentification().get(proteinKey);
            String proteinAccession = proteinMatch.getMainMatch().getAccession();
            proteinSequence = peptideShakerGUI.getSequenceDataBase().getProtein(proteinAccession).getSequence();

            // update the selection in the Overview panel
            if (updateOverviewPanel) {
                peptideShakerGUI.setSelectedProteinIndex((Integer) proteinTable.getValueAt(row, 0), true);
            }

            // update the pdb file table
            updatePdbTable(proteinTableMap.get(getProteinKey(row)));

            // empty the jmol panel
            if (jmolStructureShown) {
                jmolPanel = new JmolPanel();
                pdbPanel.removeAll();
                pdbPanel.add(jmolPanel);
                pdbPanel.revalidate();
                pdbPanel.repaint();
                jmolStructureShown = false;
                currentlyDisplayedPdbFile = null;

                ((TitledBorder) pdbStructureJPanel.getBorder()).setTitle("PDB Structure");
                pdbStructureJPanel.repaint();
            }

            // update the peptide selection
            updatedPeptideSelection(row);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            // open protein link in web browser
            if (column == 1 && evt != null && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) proteinTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }

            // open the protein inference dialog
            if (column == 2 && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                new ProteinInferenceDialog(peptideShakerGUI, proteinTable, row, proteinMatch, peptideShakerGUI.getIdentification(), peptideShakerGUI.getSequenceDataBase());
            }
        }
    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Updates the PDB structure.
     * 
     * @param evt 
     */
    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        int row = peptideTable.getSelectedRow();

        if (row != -1) {
            if (pdbMatchesJTable.getSelectedRow() != -1) {
                updatePeptideToPdbMapping();
            }

            // select the same peptide in the protein structure tab
            if (updateOverviewPanel) {
                peptideShakerGUI.setSelectedPeptideIndex((Integer) peptideTable.getValueAt(row, 0), true);
            }
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideTableMouseReleased

    /**
     * Update the PDB structure shown in the Jmol panel.
     * 
     * @param evt 
     */
    private void pdbMatchesJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbMatchesJTableMouseReleased

        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        boolean loadStructure = true;

        if (pdbMatchesJTable.getSelectedRow() != -1 && currentlyDisplayedPdbFile != null) {

            String tempPdbFile = (String) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), 1);

            if (currentlyDisplayedPdbFile.equalsIgnoreCase(tempPdbFile)) {
                loadStructure = false;
            }
        }

        if (loadStructure) {

            while (pdbChainsJTable.getRowCount() > 0) {
                ((DefaultTableModel) pdbChainsJTable.getModel()).removeRow(0);
            }

            // clear the peptide to pdb mappings in the peptide table
            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                peptideTable.setValueAt(false, i, peptideTable.getColumn("PDB").getModelIndex());
            }

            // select the first peptide in the table again
            if (peptideTable.getRowCount() > 0) {
                peptideTable.setRowSelectionInterval(0, 0);
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(0, 0, false));
            }

            // empty the jmol panel
            if (jmolStructureShown) {
                jmolPanel = new JmolPanel();
                pdbPanel.removeAll();
                pdbPanel.add(jmolPanel);
                pdbPanel.revalidate();
                pdbPanel.repaint();
                jmolStructureShown = false;
                currentlyDisplayedPdbFile = null;

                ((TitledBorder) pdbStructureJPanel.getBorder()).setTitle("PDB Structure");
                pdbStructureJPanel.repaint();
            }

            // get the protein length
            int proteinSequenceLength = proteinSequence.length();

            if (pdbMatchesJTable.getSelectedRow() != -1) {

                currentlyDisplayedPdbFile = (String) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), 1);

                // open protein link in web browser
                if (pdbMatchesJTable.getSelectedColumn() == 1 && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) proteinTable.getValueAt(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getSelectedColumn())).lastIndexOf("<html>") != -1) {

                    String temp = currentlyDisplayedPdbFile.substring(currentlyDisplayedPdbFile.indexOf("\"") + 1);
                    currentlyDisplayedPdbFile = temp.substring(0, temp.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(currentlyDisplayedPdbFile);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                // get the pdb file
                int selectedPdbTableIndex = (Integer) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), 0);
                PdbParameter lParam = uniProtPdb.getPdbs().get(selectedPdbTableIndex - 1);
                chains = lParam.getBlocks();

                // add the chain information to the table
                for (int j = 0; j < chains.length; j++) {

                    XYDataPoint[] temp = new XYDataPoint[2];
                    temp[0] = new XYDataPoint(chains[j].getStart_block(), chains[j].getEnd_block());
                    temp[1] = new XYDataPoint(0, proteinSequenceLength);

                    ((DefaultTableModel) pdbChainsJTable.getModel()).addRow(new Object[]{
                                (j + 1),
                                chains[j].getBlock(),
                                temp,
                                (((double) chains[j].getEnd_protein() - chains[j].getStart_protein()) / proteinSequenceLength) * 100
                            });
                }

                ((JSparklinesIntervalChartTableCellRenderer) pdbChainsJTable.getColumn("PDB-Protein").getCellRenderer()).setMaxValue(proteinSequenceLength);

                if (pdbChainsJTable.getRowCount() > 0) {
                    ((TitledBorder) pdbChainsJPanel.getBorder()).setTitle("PDB Chains (" + pdbChainsJTable.getRowCount() + ")");
                } else {
                    ((TitledBorder) pdbChainsJPanel.getBorder()).setTitle("PDB Chains");
                }

                pdbChainsJPanel.repaint();

                if (pdbChainsJTable.getRowCount() > 0) {
                    pdbChainsJTable.setRowSelectionInterval(0, 0);
                    pdbChainsJTable.scrollRectToVisible(pdbChainsJTable.getCellRect(0, 0, false));
                    pdbChainsJTableMouseReleased(null);
                }
            } else {
                ((TitledBorder) pdbChainsJPanel.getBorder()).setTitle("PDB Chains");
                pdbChainsJPanel.repaint();
            }
        } else {
            
            // open protein link in web browser
            if (pdbMatchesJTable.getSelectedColumn() == 1 && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) proteinTable.getValueAt(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getSelectedColumn())).lastIndexOf("<html>") != -1) {

                String temp = currentlyDisplayedPdbFile.substring(currentlyDisplayedPdbFile.indexOf("\"") + 1);
                currentlyDisplayedPdbFile = temp.substring(0, temp.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(currentlyDisplayedPdbFile);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbMatchesJTableMouseReleased

    /**
     * Update the PDB structure with the currnet chain selection.
     * 
     * @param evt 
     */
    private void pdbChainsJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbChainsJTableMouseReleased

        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (jmolStructureShown) {
            updatePeptideToPdbMapping();
        } else {

            progressDialog = new ProgressDialogX(peptideShakerGUI, this, true);
            progressDialog.setIndeterminate(true);
            progressDialog.doNothingOnClose();

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setTitle("Loading PDB Structure. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog2").start();

            new Thread("StructureThread") {

                @Override
                public void run() {

                    progressDialog.setIndeterminate(true);

                    int selectedPdbIndex = (Integer) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), 0);
                    PdbParameter lParam = uniProtPdb.getPdbs().get(selectedPdbIndex - 1);

                    String link = "http://www.rcsb.org/pdb/files/" + lParam.getPdbaccession() + ".pdb";

                    jmolPanel.getViewer().openFile(link);
                    jmolPanel.getViewer().evalString("ribbon ONLY");
                    jmolStructureShown = true;

                    ((TitledBorder) pdbStructureJPanel.getBorder()).setTitle("PDB Structure (" + lParam.getPdbaccession() + ")");
                    pdbStructureJPanel.repaint();

                    progressDialog.setTitle("Mapping Peptides. Please Wait...");

                    // get the chains
                    chains = lParam.getBlocks();
                    int selectedChainIndex = (Integer) pdbChainsJTable.getValueAt(pdbChainsJTable.getSelectedRow(), 0);
                    chainSequence = chains[selectedChainIndex - 1].getBlockSequence(lParam.getPdbaccession());

                    // update the peptide to pdb mappings
                    updatePeptideToPdbMapping();

                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                    setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }.start();
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbChainsJTableMouseReleased

    /**
     * Update the PDB structure with the currently selected chain.
     * 
     * @param evt 
     */
    private void pdbChainsJTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pdbChainsJTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            updatePeptideToPdbMapping();
        }
    }//GEN-LAST:event_pdbChainsJTableKeyReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * html link.
     *
     * @param evt
     */
    private void pdbMatchesJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbMatchesJTableMouseMoved
        int row = pdbMatchesJTable.rowAtPoint(evt.getPoint());
        int column = pdbMatchesJTable.columnAtPoint(evt.getPoint());

        if (column == 1 && pdbMatchesJTable.getValueAt(row, column) != null) {

            String tempValue = (String) pdbMatchesJTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_pdbMatchesJTableMouseMoved

    /**
     * Changes the cursor back to the default cursor a hand.
     *
     * @param evt
     */
    private void pdbMatchesJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbMatchesJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbMatchesJTableMouseExited
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pdbChainsJPanel;
    private javax.swing.JScrollPane pdbChainsJScrollPane;
    private javax.swing.JTable pdbChainsJTable;
    private javax.swing.JScrollPane pdbJScrollPane;
    private javax.swing.JPanel pdbMatchesJPanel;
    private javax.swing.JTable pdbMatchesJTable;
    private javax.swing.JPanel pdbPanel;
    private javax.swing.JPanel pdbStructureJPanel;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JPanel proteinsJPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Select the given protein index in the protein table.
     * 
     * @param proteinIndex the protein index to select
     */
    public void setSelectedProteinIndex(Integer proteinIndex) {

        boolean indexFound = false;

        for (int i = 0; i < proteinTable.getRowCount() && !indexFound; i++) {
            if ((Integer) proteinTable.getValueAt(i, 0) == proteinIndex) {
                indexFound = true;
                proteinTable.setRowSelectionInterval(i, i);
                proteinTable.scrollRectToVisible(proteinTable.getCellRect(i, 0, false));
            }
        }

        updateOverviewPanel = false;
        proteinTableMouseReleased(null);
        updateOverviewPanel = true;
    }

    /**
     * Select the given peptide index in the peptide table.
     * 
     * @param peptideIndex the peptide index to select
     */
    public void setSelectedPeptideIndex(Integer peptideIndex) {

        boolean indexFound = false;

        for (int i = 0; i < peptideTable.getRowCount() && !indexFound; i++) {
            if ((Integer) peptideTable.getValueAt(i, 0) == peptideIndex) {
                indexFound = true;
                peptideTable.setRowSelectionInterval(i, i);
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(i, 0, false));

                if (pdbMatchesJTable.getSelectedRow() != -1) {
                    pdbMatchesJTable.removeRowSelectionInterval(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getSelectedRow());
                }

                if (pdbChainsJTable.getSelectedRow() != -1) {
                    pdbChainsJTable.removeRowSelectionInterval(pdbChainsJTable.getSelectedRow(), pdbChainsJTable.getSelectedRow());
                }

                // empty the jmol panel
                if (jmolStructureShown) {
                    jmolPanel = new JmolPanel();
                    pdbPanel.removeAll();
                    pdbPanel.add(jmolPanel);
                    pdbPanel.revalidate();
                    pdbPanel.repaint();
                    jmolStructureShown = false;
                    currentlyDisplayedPdbFile = null;

                    ((TitledBorder) pdbStructureJPanel.getBorder()).setTitle("PDB Structure");
                    pdbStructureJPanel.repaint();
                }
            }
        }
    }

    /**
     * Returns the peptide key for the given row.
     *
     * @param row   the given row
     * @return      the peptide key
     */
    private String getPeptideKey(int row) {
        return (String) peptideTable.getValueAt(row, 1) + (String) peptideTable.getValueAt(row, 4);
    }

    /**
     * Updates the peptide selection according to the currently selected protein.
     *
     * @param row   the row index of the protein
     */
    private void updatedPeptideSelection(int row) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            while (peptideTable.getRowCount() > 0) {
                ((DefaultTableModel) peptideTable.getModel()).removeRow(0);
            }

            String proteinKey = proteinTableMap.get(getProteinKey(row));
            peptideTableMap = new HashMap<String, String>();

            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinIdentification().get(proteinKey);
            HashMap<Double, ArrayList<PeptideMatch>> peptideMap = new HashMap<Double, ArrayList<PeptideMatch>>();
            PSParameter probabilities = new PSParameter();
            double peptideProbabilityScore;

            for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
                peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                if (!peptideMap.containsKey(peptideProbabilityScore)) {
                    peptideMap.put(peptideProbabilityScore, new ArrayList<PeptideMatch>());
                }

                peptideMap.get(peptideProbabilityScore).add(peptideMatch);
            }

            ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
            Collections.sort(scores);

            int index = 0;
            int validatedPeptideCounter = 0;

            for (double score : scores) {
                for (PeptideMatch peptideMatch : peptideMap.get(score)) {

                    ArrayList<String> proteinAccessions = new ArrayList<String>();

                    for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                        proteinAccessions.add(protein.getAccession());
                    }

                    String modifications = "";

                    for (ModificationMatch mod : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
                        if (mod.isVariable()) {
                            modifications += mod.getTheoreticPtm().getName() + ", ";
                        }
                    }

                    if (modifications.length() > 0) {
                        modifications = modifications.substring(0, modifications.length() - 2);
                    } else {
                        modifications = null;
                    }

                    probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
                    String otherProteins = "";
                    boolean newProtein;

                    for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {

                        newProtein = true;

                        for (String referenceAccession : proteinMatch.getTheoreticProteinsAccessions()) {
                            if (proteinMatch.getTheoreticProtein(referenceAccession).getAccession().equals(protein.getAccession())) {
                                newProtein = false;
                            }
                        }

                        if (newProtein) {
                            otherProteins += protein.getAccession() + " ";
                        }
                    }

                    String peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();
                    int peptideStart = proteinSequence.lastIndexOf(peptideSequence) + 1;
                    int peptideEnd = peptideStart + peptideSequence.length() - 1;

                    ((DefaultTableModel) peptideTable.getModel()).addRow(new Object[]{
                                index + 1,
                                peptideSequence,
                                peptideStart,
                                peptideEnd,
                                modifications,
                                otherProteins,
                                false,
                                probabilities.isValidated()
                            });

                    if (probabilities.isValidated()) {
                        validatedPeptideCounter++;
                    }

                    peptideTableMap.put(peptideMatch.getTheoreticPeptide().getSequence() + modifications, peptideMatch.getKey());
                    index++;
                }
            }

            ((TitledBorder) peptidesJPanel.getBorder()).setTitle("Peptides (" + validatedPeptideCounter + "/" + peptideTable.getRowCount() + ")");
            peptidesJPanel.repaint();

            // select the first peptide in the table
            if (peptideTable.getRowCount() > 0) {
                peptideTable.setRowSelectionInterval(0, 0);
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(0, 0, false));
                peptideTableKeyReleased(null);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Displays the results in the result tables.
     * 
     * @throws MzMLUnmarshallerException
     */
    public void displayResults() throws MzMLUnmarshallerException {

        // @TODO: Merge with the similar method in the OverviewPanel class...

        ProteomicAnalysis proteomicAnalysis = peptideShakerGUI.getProteomicanalysis();
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        int index = 0, maxPeptides = 0, maxSpectra = 0;
        double sequenceCoverage = 0;
        double emPAI = 0, maxEmPAI = 0;
        String description = "";
        SequenceDataBase db = proteomicAnalysis.getSequenceDataBase();

        // sort the proteins according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
        HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap =
                new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>(); // Maps are my passion
        ArrayList<Double> scores = new ArrayList<Double>();
        PSParameter probabilities = new PSParameter();
        ProteinMatch proteinMatch;
        double score;
        int nPeptides, nSpectra;

        for (String key : peptideShakerGUI.getIdentification().getProteinIdentification().keySet()) {

            proteinMatch = peptideShakerGUI.getIdentification().getProteinIdentification().get(key);
            probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);
            score = probabilities.getProteinProbabilityScore();
            nPeptides = -proteinMatch.getPeptideMatches().size();
            nSpectra = -proteinMatch.getSpectrumCount();

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

            orderMap.get(score).get(nPeptides).get(nSpectra).add(key);
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

                        proteinMatch = peptideShakerGUI.getIdentification().getProteinIdentification().get(proteinKey);
                        probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);

                        Protein currentProtein = db.getProtein(proteinMatch.getMainMatch().getAccession());
                        int nPossible = currentProtein.getNPossiblePeptides(peptideShakerGUI.getSearchParameters().getEnzyme());
                        emPAI = (Math.pow(10, ((double) proteinMatch.getPeptideCount()) / ((double) nPossible))) - 1;
                        description = db.getProteinHeader(proteinMatch.getMainMatch().getAccession()).getDescription();
                        sequenceCoverage = 100 * peptideShakerGUI.estimateSequenceCoverage(proteinMatch, currentProtein.getSequence());

                        // only add non-decoy matches to the overview
                        if (!proteinMatch.isDecoy()) {
                            ((DefaultTableModel) proteinTable.getModel()).addRow(new Object[]{
                                        index + 1,
                                        peptideShakerGUI.addDatabaseLink(proteinMatch.getMainMatch()),
                                        probabilities.getGroupClass(),
                                        description,
                                        sequenceCoverage,
                                        emPAI,
                                        proteinMatch.getPeptideCount(),
                                        proteinMatch.getSpectrumCount(),
                                        probabilities.getProteinScore(),
                                        probabilities.getProteinConfidence(),
                                        probabilities.isValidated()
                                    });
                            proteinTableMap.put(index + 1, proteinKey);
                            index++;

                            if (probabilities.isValidated()) {
                                validatedProteinsCounter++;
                            }
                        }

                        if (maxPeptides < proteinMatch.getPeptideMatches().size()) {
                            maxPeptides = proteinMatch.getPeptideMatches().size();
                        }

                        if (maxSpectra < proteinMatch.getSpectrumCount()) {
                            maxSpectra = proteinMatch.getSpectrumCount();
                        }

                        if (maxEmPAI < emPAI) {
                            maxEmPAI = emPAI;
                        }
                    }
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
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).setMaxValue(maxEmPAI);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence [%]").getCellRenderer()).setMaxValue(100.0);

        // select the first row
        if (proteinTable.getRowCount() > 0) {
            proteinTable.setRowSelectionInterval(0, 0);
            proteinTableMouseReleased(null);
            proteinTable.requestFocus();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns the protein key for the given row.
     *
     * @param row   the row to get the key for
     * @return      the protein key
     */
    private Integer getProteinKey(int row) {
        return (Integer) proteinTable.getValueAt(row, 0);
    }

    /**
     * Update the PDB table according to the selected protein in the protein 
     * table.
     * 
     * @param proteinKey the current protein key
     */
    private void updatePdbTable(String proteinKey) {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // get the accession number of the main match
        ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinIdentification().get(proteinKey);
        String tempAccession = proteinMatch.getMainMatch().getAccession();

        // find the pdb matches
        uniProtPdb = new FindPdbForUniprotAccessions(tempAccession);

        // delete the previous matches
        while (pdbMatchesJTable.getRowCount() > 0) {
            ((DefaultTableModel) pdbMatchesJTable.getModel()).removeRow(0);
        }

        while (pdbChainsJTable.getRowCount() > 0) {
            ((DefaultTableModel) pdbChainsJTable.getModel()).removeRow(0);
        }

        // clear the peptide to pdb mappings in the peptide table
        for (int i = 0; i < peptideTable.getRowCount(); i++) {
            peptideTable.setValueAt(false, i, peptideTable.getColumn("PDB").getModelIndex());
        }

        int maxNumberOfChains = 1;

        // add the new matches to the pdb table
        for (int i = 0; i < uniProtPdb.getPdbs().size(); i++) {
            PdbParameter lParam = uniProtPdb.getPdbs().get(i);

            ((DefaultTableModel) pdbMatchesJTable.getModel()).addRow(new Object[]{
                        i + 1,
                        addPdbDatabaseLink(lParam.getPdbaccession()),
                        lParam.getTitle(),
                        lParam.getExperiment_type(),
                        lParam.getBlocks().length});

            if (lParam.getBlocks().length > maxNumberOfChains) {
                maxNumberOfChains = lParam.getBlocks().length;
            }
        }

        ((JSparklinesBarChartTableCellRenderer) pdbMatchesJTable.getColumn("Chains").getCellRenderer()).setMaxValue(maxNumberOfChains);

        if (!uniProtPdb.urlWasRead()) {
            ((TitledBorder) pdbMatchesJPanel.getBorder()).setTitle("PDB Matches - Not Available Without Internet Connection!");
        } else {
            ((TitledBorder) pdbMatchesJPanel.getBorder()).setTitle("PDB Matches (" + pdbMatchesJTable.getRowCount() + ")");
        }

        pdbMatchesJPanel.repaint();

        ((TitledBorder) pdbChainsJPanel.getBorder()).setTitle("PDB Chains");
        pdbChainsJPanel.repaint();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void cancelProgress() {
        // do nothing
    }

    /**
     * A simple class for displaying a Jmol viewer in a JPanel.
     */
    public class JmolPanel extends JPanel {

        /**
         * The JmolViewer.
         */
        private JmolViewer viewer;
        /**
         * The current size of the JPanel.
         */
        private final Dimension currentSize = new Dimension();
        /**
         * The current rectangle of the JPanel.
         */
        private final Rectangle rectClip = new Rectangle();

        /**
         * Create a new JmolPanel.
         */
        JmolPanel() {
            JmolAdapter adapter = new SmarterJmolAdapter();
            viewer = JmolViewer.allocateViewer(this, adapter);
        }

        /**
         * Returns the JmolViewer.
         * 
         * @return the JmolViewer
         */
        public JmolViewer getViewer() {
            return viewer;
        }

        /**
         * Executes the given command line on the Jmol instance.
         * 
         * @param rasmolScript the command line to execute
         */
        public void executeCmd(String rasmolScript) {
            viewer.evalString(rasmolScript);
        }

        @Override
        public void paint(Graphics g) {
            getSize(currentSize);
            g.getClipBounds(rectClip);
            viewer.renderScreenImage(g, currentSize, rectClip);
        }
    }

    /**
     * Displays or hide sparklines in the tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence [%]").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) pdbMatchesJTable.getColumn("Chains").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) pdbChainsJTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) pdbChainsJTable.getColumn("PDB-Protein").getCellRenderer()).showNumbers(!showSparkLines);

        proteinTable.revalidate();
        proteinTable.repaint();

        peptideTable.revalidate();
        peptideTable.repaint();

        pdbMatchesJTable.revalidate();
        pdbMatchesJTable.repaint();
    }

    /**
     * Transforms the PDB accesion number into an HTML link to the 
     * PDB. Note that this is a complete HTML with HTML and a href tags, 
     * where the main use is to include it in the PDB tables.
     * 
     * @param protein   the PDB accession number to get the link for
     * @return          the transformed accession number
     */
    private String addPdbDatabaseLink(String pdbAccession) {

        return "<html><a href=\"" + getPDBAccesionLink(pdbAccession)
                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                + pdbAccession + "</font></a></html>";
    }

    /**
     * Returns the PDB accession number as a web link to the given 
     * structure at http://www.rcsb.org.
     * 
     * @param pdbAccession  the PDB accession number
     * @return              the PDB accession web link
     */
    public String getPDBAccesionLink(String pdbAccession) {
        return "http://www.rcsb.org/pdb/explore/explore.do?structureId=" + pdbAccession;
    }

    /**
     * Update the peptide to PDB mappings.
     */
    private void updatePeptideToPdbMapping() {

        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // clear the old mappings
        for (int i = 0; i < peptideTable.getRowCount(); i++) {
            peptideTable.setValueAt(false, i, peptideTable.getColumn("PDB").getModelIndex());
        }

        jmolPanel.getViewer().evalString("select all; color grey");

        // update the peptide selection
        int selectedChainIndex = (Integer) pdbChainsJTable.getValueAt(pdbChainsJTable.getSelectedRow(), 0);
        String currentChain = chains[selectedChainIndex - 1].getBlock();

        // iterate the peptide table and store the coverage for each peptide
        for (int i = 0; i < peptideTable.getRowCount(); i++) {
            String peptideKey = peptideTableMap.get(getPeptideKey(i));
            String peptideSequence = peptideShakerGUI.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide().getSequence();
            String tempSequence = proteinSequence;

            while (tempSequence.lastIndexOf(peptideSequence) >= 0 && chainSequence.lastIndexOf(peptideSequence) >= 0) {
                int peptideTempStart = tempSequence.lastIndexOf(peptideSequence);
                int peptideTempEnd = peptideTempStart + peptideSequence.length();
                jmolPanel.getViewer().evalString(
                        "select resno >=" + peptideTempStart
                        + " and resno <=" + peptideTempEnd
                        + " and chain = " + currentChain + "; color orange");
                tempSequence = proteinSequence.substring(0, peptideTempStart);

                peptideTable.setValueAt(true, i, peptideTable.getColumn("PDB").getModelIndex());
            }
        }

        // highlight the selected peptide
        String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
        String peptideSequence = peptideShakerGUI.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide().getSequence();
        String tempSequence = proteinSequence;


        while (tempSequence.lastIndexOf(peptideSequence) >= 0 && chainSequence.lastIndexOf(peptideSequence) >= 0) {

            int peptideTempStart = tempSequence.lastIndexOf(peptideSequence);
            int peptideTempEnd = peptideTempStart + peptideSequence.length();

            jmolPanel.getViewer().evalString(
                    "select resno >=" + peptideTempStart
                    + " and resno <=" + peptideTempEnd
                    + " and chain = " + currentChain + "; color blue");
            tempSequence = proteinSequence.substring(0, peptideTempStart);
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }
}
