package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.pdbfinder.FindPdbForUniprotAccessions;
import com.compomics.util.pdbfinder.pdb.PdbBlock;
import com.compomics.util.pdbfinder.pdb.PdbParameter;
import eu.isas.peptideshaker.export.FeaturesGenerator;
import eu.isas.peptideshaker.gui.ExportGraphicsDialog;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.ProteinInferenceDialog;
import eu.isas.peptideshaker.gui.ProteinInferencePeptideLevelDialog;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
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

/**
 * The Protein Structures tab.
 * 
 * @author Harald Barsnes
 */
public class ProteinStructurePanel extends javax.swing.JPanel implements ProgressDialogParent {

    /**
     * It true the tab has been initiated, i.e., the data displayed at leaat once. 
     * False means that the tab has to be loaded from scratch.
     */
    private boolean tabInitiated = false;
    /**
     * Peptide keys that can be mapped to the current pdb file.
     */
    private ArrayList<String> peptidePdbArray;

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        PROTEIN_TABLE, PEPTIDE_TABLE, PDB_MATCHES, PDB_CHAINS
    };
    /**
     * If true, labels are shown for the modifications in the 3D structure.
     */
    private boolean showModificationLabels = true;
    /**
     * If true, the 3D model will be spinning. 
     */
    private boolean spinModel = true;
    /**
     * If true the ribbon model is used.
     */
    private boolean ribbonModel = true;
    /**
     * If true the backbone model is used.
     */
    private boolean backboneModel = false;
    /**
     * A reference to the protein score column.
     */
    private TableColumn proteinScoreColumn;
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
    private HashMap<Integer, String> peptideTableMap = new HashMap<Integer, String>();
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
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /** 
     * Creates a new ProteinPanel.
     * 
     * @param peptideShakerGUI the PeptideShaker main frame
     */
    public ProteinStructurePanel(PeptideShakerGUI peptideShakerGUI) {
        initComponents();
        proteinScoreColumn = proteinTable.getColumn("Score");
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
        proteinTableToolTips.add("Protein Inference");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Protein Seqeunce Coverage (%)");
        proteinTableToolTips.add("Number of Peptides");
        proteinTableToolTips.add("Number of Spectra");
        proteinTableToolTips.add("Protein MS2 Quantification");
        proteinTableToolTips.add("Protein Score");
        proteinTableToolTips.add("Protein Confidence");
        proteinTableToolTips.add("Validated");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Protein Inference");
        peptideTableToolTips.add("Peptide Sequence");
        peptideTableToolTips.add("Peptide Start Index");
        peptideTableToolTips.add("Peptide End Index");
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
        peptideTable.getColumn("PI").setMaxWidth(35);
        peptideTable.getColumn("PI").setMinWidth(35);

        proteinTable.getColumn("Confidence").setMaxWidth(90);
        proteinTable.getColumn("Confidence").setMinWidth(90);
        proteinTable.getColumn("Score").setMaxWidth(90);
        proteinTable.getColumn("Score").setMinWidth(90);

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
        proteinTable.getColumn("MS2 Quantification").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quantification").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
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
        peptideTable.getColumn("PDB").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/pdb.png")),
                null,
                "Mapped to PDB Structure", null));
        peptideTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        try {
            proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            // ignore error
        }

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
        pdbStructureLayeredPane = new javax.swing.JLayeredPane();
        pdbOuterPanel = new javax.swing.JPanel();
        pdbLayeredPane = new javax.swing.JLayeredPane();
        pdbPanel = new javax.swing.JPanel();
        labelsJButton = new javax.swing.JButton();
        ribbonJButton = new javax.swing.JButton();
        backboneJButton = new javax.swing.JButton();
        playJButton = new javax.swing.JButton();
        pdbStructureHelpJButton = new javax.swing.JButton();
        exportPdbStructureJButton = new javax.swing.JButton();
        contextMenuPdbStructureBackgroundPanel = new javax.swing.JPanel();
        proteinsJPanel = new javax.swing.JPanel();
        proteinsLayeredPane = new javax.swing.JLayeredPane();
        proteinsPanel = new javax.swing.JPanel();
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
        proteinsHelpJButton = new javax.swing.JButton();
        exportProteinsJButton = new javax.swing.JButton();
        contextMenuProteinsBackgroundPanel = new javax.swing.JPanel();
        peptidesJPanel = new javax.swing.JPanel();
        peptidesLayeredPane = new javax.swing.JLayeredPane();
        peptidesPanel = new javax.swing.JPanel();
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
        peptidesHelpJButton = new javax.swing.JButton();
        exportPeptidesJButton = new javax.swing.JButton();
        contextMenuPeptidesBackgroundPanel = new javax.swing.JPanel();
        pdbMatchesJPanel = new javax.swing.JPanel();
        pdbMatchesLayeredPane = new javax.swing.JLayeredPane();
        pdbMatchesPanel = new javax.swing.JPanel();
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
        pdbMatchesHelpJButton = new javax.swing.JButton();
        exportPdbMatchesJButton = new javax.swing.JButton();
        contextMenuPdbMatchesBackgroundPanel = new javax.swing.JPanel();
        pdbChainsJPanel = new javax.swing.JPanel();
        pdbChainsLayeredPane = new javax.swing.JLayeredPane();
        pdbChainsPanel = new javax.swing.JPanel();
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
        pdbChainHelpJButton = new javax.swing.JButton();
        exportPdbChainsJButton = new javax.swing.JButton();
        contextMenuPdbChainsBackgroundPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        pdbStructureJPanel.setOpaque(false);

        pdbOuterPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PDB Structure"));
        pdbOuterPanel.setOpaque(false);

        pdbLayeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                pdbLayeredPaneComponentResized(evt);
            }
        });

        pdbPanel.setLayout(new javax.swing.BoxLayout(pdbPanel, javax.swing.BoxLayout.LINE_AXIS));
        pdbPanel.setBounds(0, 0, 435, 440);
        pdbLayeredPane.add(pdbPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/labels_selected.png"))); // NOI18N
        labelsJButton.setToolTipText("Hide Modification Labels");
        labelsJButton.setBorder(null);
        labelsJButton.setBorderPainted(false);
        labelsJButton.setContentAreaFilled(false);
        labelsJButton.setFocusable(false);
        labelsJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        labelsJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        labelsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                labelsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                labelsJButtonMouseExited(evt);
            }
        });
        labelsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                labelsJButtonActionPerformed(evt);
            }
        });
        labelsJButton.setBounds(0, 0, 25, 25);
        pdbLayeredPane.add(labelsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        ribbonJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ribbon_selected.png"))); // NOI18N
        ribbonJButton.setToolTipText("Ribbon Model");
        ribbonJButton.setBorder(null);
        ribbonJButton.setBorderPainted(false);
        ribbonJButton.setContentAreaFilled(false);
        ribbonJButton.setFocusable(false);
        ribbonJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ribbonJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        ribbonJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ribbonJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ribbonJButtonMouseExited(evt);
            }
        });
        ribbonJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ribbonJButtonActionPerformed(evt);
            }
        });
        ribbonJButton.setBounds(0, 0, 25, 25);
        pdbLayeredPane.add(ribbonJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        backboneJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/backbone.png"))); // NOI18N
        backboneJButton.setToolTipText("Backbone Model");
        backboneJButton.setBorder(null);
        backboneJButton.setBorderPainted(false);
        backboneJButton.setContentAreaFilled(false);
        backboneJButton.setFocusable(false);
        backboneJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        backboneJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        backboneJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                backboneJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                backboneJButtonMouseExited(evt);
            }
        });
        backboneJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backboneJButtonActionPerformed(evt);
            }
        });
        backboneJButton.setBounds(0, 0, 25, 25);
        pdbLayeredPane.add(backboneJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        playJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pause.png"))); // NOI18N
        playJButton.setToolTipText("Stop Rotation");
        playJButton.setBorder(null);
        playJButton.setBorderPainted(false);
        playJButton.setContentAreaFilled(false);
        playJButton.setFocusable(false);
        playJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        playJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        playJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                playJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                playJButtonMouseExited(evt);
            }
        });
        playJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playJButtonActionPerformed(evt);
            }
        });
        playJButton.setBounds(0, 0, 21, 21);
        pdbLayeredPane.add(playJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout pdbOuterPanelLayout = new javax.swing.GroupLayout(pdbOuterPanel);
        pdbOuterPanel.setLayout(pdbOuterPanelLayout);
        pdbOuterPanelLayout.setHorizontalGroup(
            pdbOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 448, Short.MAX_VALUE)
            .addGroup(pdbOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pdbOuterPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(pdbLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        pdbOuterPanelLayout.setVerticalGroup(
            pdbOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 463, Short.MAX_VALUE)
            .addGroup(pdbOuterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pdbOuterPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(pdbLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        pdbOuterPanel.setBounds(0, 0, 460, 490);
        pdbStructureLayeredPane.add(pdbOuterPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        pdbStructureHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        pdbStructureHelpJButton.setToolTipText("Help");
        pdbStructureHelpJButton.setBorder(null);
        pdbStructureHelpJButton.setBorderPainted(false);
        pdbStructureHelpJButton.setContentAreaFilled(false);
        pdbStructureHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        pdbStructureHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pdbStructureHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pdbStructureHelpJButtonMouseExited(evt);
            }
        });
        pdbStructureHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pdbStructureHelpJButtonActionPerformed(evt);
            }
        });
        pdbStructureHelpJButton.setBounds(440, 0, 10, 25);
        pdbStructureLayeredPane.add(pdbStructureHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPdbStructureJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPdbStructureJButton.setToolTipText("Export");
        exportPdbStructureJButton.setBorder(null);
        exportPdbStructureJButton.setBorderPainted(false);
        exportPdbStructureJButton.setContentAreaFilled(false);
        exportPdbStructureJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPdbStructureJButton.setEnabled(false);
        exportPdbStructureJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPdbStructureJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPdbStructureJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPdbStructureJButtonMouseExited(evt);
            }
        });
        exportPdbStructureJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPdbStructureJButtonActionPerformed(evt);
            }
        });
        exportPdbStructureJButton.setBounds(430, 0, 10, 25);
        pdbStructureLayeredPane.add(exportPdbStructureJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPdbStructureBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPdbStructureBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPdbStructureBackgroundPanel);
        contextMenuPdbStructureBackgroundPanel.setLayout(contextMenuPdbStructureBackgroundPanelLayout);
        contextMenuPdbStructureBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPdbStructureBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPdbStructureBackgroundPanelLayout.setVerticalGroup(
            contextMenuPdbStructureBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPdbStructureBackgroundPanel.setBounds(420, 0, 30, 20);
        pdbStructureLayeredPane.add(contextMenuPdbStructureBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout pdbStructureJPanelLayout = new javax.swing.GroupLayout(pdbStructureJPanel);
        pdbStructureJPanel.setLayout(pdbStructureJPanelLayout);
        pdbStructureJPanelLayout.setHorizontalGroup(
            pdbStructureJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pdbStructureLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
        );
        pdbStructureJPanelLayout.setVerticalGroup(
            pdbStructureJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pdbStructureLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
        );

        proteinsJPanel.setOpaque(false);

        proteinsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "PI", "Accession", "Description", "Coverage", "#Peptides", "#Spectra", "MS2 Quantification", "Score", "Confidence", ""
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

        javax.swing.GroupLayout proteinsPanelLayout = new javax.swing.GroupLayout(proteinsPanel);
        proteinsPanel.setLayout(proteinsPanelLayout);
        proteinsPanelLayout.setHorizontalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 938, Short.MAX_VALUE)
            .addGroup(proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 918, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        proteinsPanelLayout.setVerticalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 243, Short.MAX_VALUE)
            .addGroup(proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        proteinsPanel.setBounds(0, 0, 950, 270);
        proteinsLayeredPane.add(proteinsPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        proteinsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        proteinsHelpJButton.setToolTipText("Help");
        proteinsHelpJButton.setBorder(null);
        proteinsHelpJButton.setBorderPainted(false);
        proteinsHelpJButton.setContentAreaFilled(false);
        proteinsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        proteinsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseExited(evt);
            }
        });
        proteinsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinsHelpJButtonActionPerformed(evt);
            }
        });
        proteinsHelpJButton.setBounds(930, 0, 10, 25);
        proteinsLayeredPane.add(proteinsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setToolTipText("Copy to Clipboard");
        exportProteinsJButton.setBorder(null);
        exportProteinsJButton.setBorderPainted(false);
        exportProteinsJButton.setContentAreaFilled(false);
        exportProteinsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setEnabled(false);
        exportProteinsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportProteinsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseExited(evt);
            }
        });
        exportProteinsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProteinsJButtonActionPerformed(evt);
            }
        });
        exportProteinsJButton.setBounds(920, 0, 10, 25);
        proteinsLayeredPane.add(exportProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuProteinsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuProteinsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setLayout(contextMenuProteinsBackgroundPanelLayout);
        contextMenuProteinsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuProteinsBackgroundPanelLayout.setVerticalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuProteinsBackgroundPanel.setBounds(920, 0, 30, 20);
        proteinsLayeredPane.add(contextMenuProteinsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout proteinsJPanelLayout = new javax.swing.GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsJPanelLayout);
        proteinsJPanelLayout.setHorizontalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 957, Short.MAX_VALUE)
        );
        proteinsJPanelLayout.setVerticalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
        );

        peptidesJPanel.setOpaque(false);

        peptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidesPanel.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "PI", "Sequence", "Start", "End", "PDB", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class
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

        javax.swing.GroupLayout peptidesPanelLayout = new javax.swing.GroupLayout(peptidesPanel);
        peptidesPanel.setLayout(peptidesPanelLayout);
        peptidesPanelLayout.setHorizontalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 468, Short.MAX_VALUE)
            .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        peptidesPanelLayout.setVerticalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 133, Short.MAX_VALUE)
            .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesPanel.setBounds(0, 0, 480, 160);
        peptidesLayeredPane.add(peptidesPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        peptidesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        peptidesHelpJButton.setToolTipText("Help");
        peptidesHelpJButton.setBorder(null);
        peptidesHelpJButton.setBorderPainted(false);
        peptidesHelpJButton.setContentAreaFilled(false);
        peptidesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        peptidesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptidesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesHelpJButtonMouseExited(evt);
            }
        });
        peptidesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidesHelpJButtonActionPerformed(evt);
            }
        });
        peptidesHelpJButton.setBounds(460, 0, 10, 25);
        peptidesLayeredPane.add(peptidesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPeptidesJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setToolTipText("Copy to Clipboard");
        exportPeptidesJButton.setBorder(null);
        exportPeptidesJButton.setBorderPainted(false);
        exportPeptidesJButton.setContentAreaFilled(false);
        exportPeptidesJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setEnabled(false);
        exportPeptidesJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPeptidesJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPeptidesJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPeptidesJButtonMouseExited(evt);
            }
        });
        exportPeptidesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPeptidesJButtonActionPerformed(evt);
            }
        });
        exportPeptidesJButton.setBounds(450, 0, 10, 25);
        peptidesLayeredPane.add(exportPeptidesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPeptidesBackgroundPanel);
        contextMenuPeptidesBackgroundPanel.setLayout(contextMenuPeptidesBackgroundPanelLayout);
        contextMenuPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPeptidesBackgroundPanel.setBounds(440, 0, 30, 20);
        peptidesLayeredPane.add(contextMenuPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout peptidesJPanelLayout = new javax.swing.GroupLayout(peptidesJPanel);
        peptidesJPanel.setLayout(peptidesJPanelLayout);
        peptidesJPanelLayout.setHorizontalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)
        );
        peptidesJPanelLayout.setVerticalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
        );

        pdbMatchesJPanel.setOpaque(false);

        pdbMatchesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PDB Matches"));
        pdbMatchesPanel.setOpaque(false);

        pdbJScrollPane.setOpaque(false);

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
        pdbMatchesJTable.setOpaque(false);
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

        javax.swing.GroupLayout pdbMatchesPanelLayout = new javax.swing.GroupLayout(pdbMatchesPanel);
        pdbMatchesPanel.setLayout(pdbMatchesPanelLayout);
        pdbMatchesPanelLayout.setHorizontalGroup(
            pdbMatchesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 468, Short.MAX_VALUE)
            .addGroup(pdbMatchesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pdbMatchesPanelLayout.createSequentialGroup()
                    .addGap(8, 8, 8)
                    .addComponent(pdbJScrollPane)
                    .addGap(8, 8, 8)))
        );
        pdbMatchesPanelLayout.setVerticalGroup(
            pdbMatchesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 123, Short.MAX_VALUE)
            .addGroup(pdbMatchesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pdbMatchesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(pdbJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        pdbMatchesPanel.setBounds(0, 0, 480, 150);
        pdbMatchesLayeredPane.add(pdbMatchesPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        pdbMatchesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        pdbMatchesHelpJButton.setToolTipText("Help");
        pdbMatchesHelpJButton.setBorder(null);
        pdbMatchesHelpJButton.setBorderPainted(false);
        pdbMatchesHelpJButton.setContentAreaFilled(false);
        pdbMatchesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        pdbMatchesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pdbMatchesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pdbMatchesHelpJButtonMouseExited(evt);
            }
        });
        pdbMatchesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pdbMatchesHelpJButtonActionPerformed(evt);
            }
        });
        pdbMatchesHelpJButton.setBounds(460, 0, 10, 25);
        pdbMatchesLayeredPane.add(pdbMatchesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPdbMatchesJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPdbMatchesJButton.setToolTipText("Copy to Clipboard");
        exportPdbMatchesJButton.setBorder(null);
        exportPdbMatchesJButton.setBorderPainted(false);
        exportPdbMatchesJButton.setContentAreaFilled(false);
        exportPdbMatchesJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPdbMatchesJButton.setEnabled(false);
        exportPdbMatchesJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPdbMatchesJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPdbMatchesJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPdbMatchesJButtonMouseExited(evt);
            }
        });
        exportPdbMatchesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPdbMatchesJButtonActionPerformed(evt);
            }
        });
        exportPdbMatchesJButton.setBounds(450, 0, 10, 25);
        pdbMatchesLayeredPane.add(exportPdbMatchesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPdbMatchesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPdbMatchesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPdbMatchesBackgroundPanel);
        contextMenuPdbMatchesBackgroundPanel.setLayout(contextMenuPdbMatchesBackgroundPanelLayout);
        contextMenuPdbMatchesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPdbMatchesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPdbMatchesBackgroundPanelLayout.setVerticalGroup(
            contextMenuPdbMatchesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPdbMatchesBackgroundPanel.setBounds(440, 0, 30, 20);
        pdbMatchesLayeredPane.add(contextMenuPdbMatchesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout pdbMatchesJPanelLayout = new javax.swing.GroupLayout(pdbMatchesJPanel);
        pdbMatchesJPanel.setLayout(pdbMatchesJPanelLayout);
        pdbMatchesJPanelLayout.setHorizontalGroup(
            pdbMatchesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pdbMatchesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)
        );
        pdbMatchesJPanelLayout.setVerticalGroup(
            pdbMatchesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pdbMatchesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
        );

        pdbChainsJPanel.setOpaque(false);

        pdbChainsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PDB Chains"));
        pdbChainsPanel.setOpaque(false);

        pdbChainsJScrollPane.setOpaque(false);

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
        pdbChainsJTable.setOpaque(false);
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

        javax.swing.GroupLayout pdbChainsPanelLayout = new javax.swing.GroupLayout(pdbChainsPanel);
        pdbChainsPanel.setLayout(pdbChainsPanelLayout);
        pdbChainsPanelLayout.setHorizontalGroup(
            pdbChainsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 468, Short.MAX_VALUE)
            .addGroup(pdbChainsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pdbChainsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(pdbChainsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        pdbChainsPanelLayout.setVerticalGroup(
            pdbChainsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 133, Short.MAX_VALUE)
            .addGroup(pdbChainsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pdbChainsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(pdbChainsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        pdbChainsPanel.setBounds(0, 0, 480, 160);
        pdbChainsLayeredPane.add(pdbChainsPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        pdbChainHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        pdbChainHelpJButton.setToolTipText("Help");
        pdbChainHelpJButton.setBorder(null);
        pdbChainHelpJButton.setBorderPainted(false);
        pdbChainHelpJButton.setContentAreaFilled(false);
        pdbChainHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        pdbChainHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                pdbChainHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                pdbChainHelpJButtonMouseExited(evt);
            }
        });
        pdbChainHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pdbChainHelpJButtonActionPerformed(evt);
            }
        });
        pdbChainHelpJButton.setBounds(460, 0, 10, 25);
        pdbChainsLayeredPane.add(pdbChainHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPdbChainsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPdbChainsJButton.setToolTipText("Copy to Clipboard");
        exportPdbChainsJButton.setBorder(null);
        exportPdbChainsJButton.setBorderPainted(false);
        exportPdbChainsJButton.setContentAreaFilled(false);
        exportPdbChainsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPdbChainsJButton.setEnabled(false);
        exportPdbChainsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPdbChainsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPdbChainsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPdbChainsJButtonMouseExited(evt);
            }
        });
        exportPdbChainsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPdbChainsJButtonActionPerformed(evt);
            }
        });
        exportPdbChainsJButton.setBounds(450, 0, 10, 25);
        pdbChainsLayeredPane.add(exportPdbChainsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPdbChainsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPdbChainsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPdbChainsBackgroundPanel);
        contextMenuPdbChainsBackgroundPanel.setLayout(contextMenuPdbChainsBackgroundPanelLayout);
        contextMenuPdbChainsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPdbChainsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPdbChainsBackgroundPanelLayout.setVerticalGroup(
            contextMenuPdbChainsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPdbChainsBackgroundPanel.setBounds(440, 0, 30, 20);
        pdbChainsLayeredPane.add(contextMenuPdbChainsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout pdbChainsJPanelLayout = new javax.swing.GroupLayout(pdbChainsJPanel);
        pdbChainsJPanel.setLayout(pdbChainsJPanelLayout);
        pdbChainsJPanelLayout.setHorizontalGroup(
            pdbChainsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pdbChainsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)
        );
        pdbChainsJPanelLayout.setVerticalGroup(
            pdbChainsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pdbChainsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
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
            try {
                // find and store the protein sequence for later use
                String proteinKey = proteinTableMap.get(getProteinKey(proteinTable.getSelectedRow()));
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                String proteinAccession = proteinMatch.getMainMatch();
                try {
                    proteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    proteinSequence = "";
                }

                // set the currently selected protein index
                peptideShakerGUI.setSelectedProteinIndex((Integer) proteinTable.getValueAt(row, 0));

                // set the accession number in the annotation tab
                peptideShakerGUI.setSelectedProteinAccession(proteinAccession);

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

                    ((TitledBorder) pdbOuterPanel.getBorder()).setTitle("PDB Structure");
                    pdbOuterPanel.repaint();
                }

                // update the peptide selection
                updatedPeptideSelection(row);
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
                    new ProteinInferenceDialog(peptideShakerGUI, proteinKey, peptideShakerGUI.getIdentification());
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
        try {
            int row = peptideTable.getSelectedRow();
            int column = peptideTable.getSelectedColumn();

            if (row != -1) {
                if (pdbMatchesJTable.getSelectedRow() != -1) {
                    updatePeptideToPdbMapping();
                }

                // set the currently selected peptide index
                if (updateOverviewPanel) {
                    peptideShakerGUI.setSelectedPeptideIndex((Integer) peptideTable.getValueAt(row, 0));
                }

                // open the protein inference at the petide level dialog
                if (evt != null && column == peptideTable.getColumn("PI").getModelIndex()) {

                    String proteinKey = proteinTableMap.get(getProteinKey(proteinTable.getSelectedRow()));

                    String peptideKey = peptideTableMap.get(getPeptideKey(row));

                    new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, proteinKey);
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
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

            String tempPdbFile = (String) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getColumn("PDB").getModelIndex());

            if (currentlyDisplayedPdbFile.equalsIgnoreCase(tempPdbFile)) {
                loadStructure = false;
            }
        }

        if (loadStructure) {

            DefaultTableModel dm = (DefaultTableModel) pdbChainsJTable.getModel();
            dm.getDataVector().removeAllElements();

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

                ((TitledBorder) pdbOuterPanel.getBorder()).setTitle("PDB Structure");
                pdbOuterPanel.repaint();
            }

            // get the protein length
            int proteinSequenceLength = proteinSequence.length();

            if (pdbMatchesJTable.getSelectedRow() != -1) {

                currentlyDisplayedPdbFile = (String) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getColumn("PDB").getModelIndex());

                // open protein link in web browser
                if (pdbMatchesJTable.getSelectedColumn() == pdbMatchesJTable.getColumn("PDB").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getSelectedColumn())).lastIndexOf("<html>") != -1) {

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
                    temp[0] = new XYDataPoint(chains[j].getStart_protein(), chains[j].getEnd_protein());
                    temp[1] = new XYDataPoint(1, proteinSequenceLength);

                    ((DefaultTableModel) pdbChainsJTable.getModel()).addRow(new Object[]{
                                (j + 1),
                                chains[j].getBlock(),
                                temp,
                                (((double) chains[j].getEnd_protein() - chains[j].getStart_protein()) / proteinSequenceLength) * 100
                            });
                }

                ((JSparklinesIntervalChartTableCellRenderer) pdbChainsJTable.getColumn("PDB-Protein").getCellRenderer()).setMaxValue(proteinSequenceLength);

                if (pdbChainsJTable.getRowCount() > 0) {
                    ((TitledBorder) pdbChainsPanel.getBorder()).setTitle("PDB Chains (" + pdbChainsJTable.getRowCount() + ")");
                } else {
                    ((TitledBorder) pdbChainsPanel.getBorder()).setTitle("PDB Chains");
                }

                pdbChainsPanel.repaint();

                if (pdbChainsJTable.getRowCount() > 0) {
                    pdbChainsJTable.setRowSelectionInterval(0, 0);
                    pdbChainsJTable.scrollRectToVisible(pdbChainsJTable.getCellRect(0, 0, false));
                    pdbChainsJTableMouseReleased(null);
                }
            } else {
                ((TitledBorder) pdbChainsPanel.getBorder()).setTitle("PDB Chains");
                pdbChainsPanel.repaint();
            }
        } else {

            // open protein link in web browser
            if (pdbMatchesJTable.getSelectedColumn() == pdbMatchesJTable.getColumn("PDB").getModelIndex() && evt.getButton() == MouseEvent.BUTTON1
                    && ((String) pdbMatchesJTable.getValueAt(pdbMatchesJTable.getSelectedRow(), pdbMatchesJTable.getSelectedColumn())).lastIndexOf("<html>") != -1) {

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
                    if (ribbonModel) {
                        jmolPanel.getViewer().evalString("select all; ribbon only;");
                    } else if (backboneModel) {
                        jmolPanel.getViewer().evalString("select all; backbone only; backbone 100;");
                    }
                    spinModel(spinModel);
                    jmolStructureShown = true;

                    ((TitledBorder) pdbOuterPanel.getBorder()).setTitle("PDB Structure (" + lParam.getPdbaccession() + ")");
                    pdbOuterPanel.repaint();

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

        if (column == pdbMatchesJTable.getColumn("PDB").getModelIndex() && pdbMatchesJTable.getValueAt(row, column) != null) {

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

    /**
     * Resizes the components in the PDB Structure layered pane if the layred 
     * pane is resized.
     * 
     * @param evt 
     */
    private void pdbLayeredPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_pdbLayeredPaneComponentResized

        int componentIndex = 0;

        // move the icons
        pdbLayeredPane.getComponent(componentIndex++).setBounds(
                pdbLayeredPane.getWidth() - pdbLayeredPane.getComponent(0).getWidth() * componentIndex - 10,
                pdbLayeredPane.getComponent(0).getHeight() / 10 - 2,
                pdbLayeredPane.getComponent(0).getWidth(),
                pdbLayeredPane.getComponent(0).getHeight());

        pdbLayeredPane.getComponent(componentIndex++).setBounds(
                pdbLayeredPane.getWidth() - pdbLayeredPane.getComponent(0).getWidth() * componentIndex - 15,
                pdbLayeredPane.getComponent(0).getHeight() / 10 - 2,
                pdbLayeredPane.getComponent(0).getWidth(),
                pdbLayeredPane.getComponent(0).getHeight());

        pdbLayeredPane.getComponent(componentIndex++).setBounds(
                pdbLayeredPane.getWidth() - pdbLayeredPane.getComponent(0).getWidth() * componentIndex - 10,
                pdbLayeredPane.getComponent(0).getHeight() / 10 - 2,
                pdbLayeredPane.getComponent(0).getWidth(),
                pdbLayeredPane.getComponent(0).getHeight());

        pdbLayeredPane.getComponent(componentIndex++).setBounds(
                pdbLayeredPane.getWidth() - pdbLayeredPane.getComponent(0).getWidth() * componentIndex - 15,
                pdbLayeredPane.getComponent(0).getHeight() / 10,
                pdbLayeredPane.getComponent(0).getWidth(),
                pdbLayeredPane.getComponent(0).getHeight());

        // resize the plot area
        pdbLayeredPane.getComponent(componentIndex++).setBounds(0, 0, pdbLayeredPane.getWidth(), pdbLayeredPane.getHeight());
        pdbLayeredPane.revalidate();
        pdbLayeredPane.repaint();
    }//GEN-LAST:event_pdbLayeredPaneComponentResized

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
                        peptideShakerGUI.catchException(e);
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
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void playJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_playJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void playJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_playJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_playJButtonMouseExited

    /**
     * Start/stop the rotation of the structure.
     * 
     * @param evt 
     */
    private void playJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playJButtonActionPerformed

        spinModel = !spinModel;
        spinModel(spinModel);

        if (spinModel) {
            playJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pause.png")));
            playJButton.setToolTipText("Stop Rotation");
        } else {
            playJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/play.png")));
            playJButton.setToolTipText("Start Rotation");
        }
    }//GEN-LAST:event_playJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void ribbonJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ribbonJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_ribbonJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void ribbonJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ribbonJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ribbonJButtonMouseExited

    /**
     * Change the model type to ribbon.
     * 
     * @param evt 
     */
    private void ribbonJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ribbonJButtonActionPerformed
        ribbonModel = true;
        backboneModel = false;
        updateModelType();

        if (backboneModel) {
            backboneJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/backbone_selected.png")));
            ribbonJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ribbon.png")));
        } else {
            backboneJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/backbone.png")));
            ribbonJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ribbon_selected.png")));
        }
    }//GEN-LAST:event_ribbonJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void backboneJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_backboneJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_backboneJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void backboneJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_backboneJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_backboneJButtonMouseExited

    /**
     * Change the model type to backbone.
     * 
     * @param evt 
     */
    private void backboneJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backboneJButtonActionPerformed
        ribbonModel = false;
        backboneModel = true;
        updateModelType();

        if (backboneModel) {
            backboneJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/backbone_selected.png")));
            ribbonJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ribbon.png")));
        } else {
            backboneJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/backbone.png")));
            ribbonJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ribbon_selected.png")));
        }
    }//GEN-LAST:event_backboneJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void labelsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelsJButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_labelsJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void labelsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelsJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_labelsJButtonMouseExited

    /**
     * Set if the modification labels are to be shown or not.
     * 
     * @param evt 
     */
    private void labelsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_labelsJButtonActionPerformed
        showModificationLabels = !showModificationLabels;

        if (showModificationLabels) {
            labelsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/labels_selected.png")));
            labelsJButton.setToolTipText("Hide Modification Labels");
        } else {
            labelsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/labels.png")));
            labelsJButton.setToolTipText("Show Modification Labels");
        }

        if (pdbMatchesJTable.getSelectedRow() != -1 && peptideTable.getSelectedRow() != -1) {
            updatePeptideToPdbMapping();
        }
    }//GEN-LAST:event_labelsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportProteinsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseEntered

    private void exportProteinsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportProteinsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProteinsJButtonActionPerformed
        copyTableContentToClipboard(TableIndex.PROTEIN_TABLE);
    }//GEN-LAST:event_exportProteinsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void proteinsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void proteinsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void proteinsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PDB.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonActionPerformed

    /**
     * Update the layered panes.
     * 
     * @param evt 
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                // move the icons
                proteinsLayeredPane.getComponent(0).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        proteinsLayeredPane.getComponent(0).getWidth(),
                        proteinsLayeredPane.getComponent(0).getHeight());

                proteinsLayeredPane.getComponent(1).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        proteinsLayeredPane.getComponent(1).getWidth(),
                        proteinsLayeredPane.getComponent(1).getHeight());

                proteinsLayeredPane.getComponent(2).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        proteinsLayeredPane.getComponent(2).getWidth(),
                        proteinsLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                proteinsLayeredPane.getComponent(3).setBounds(0, 0, proteinsLayeredPane.getWidth(), proteinsLayeredPane.getHeight());
                proteinsLayeredPane.revalidate();
                proteinsLayeredPane.repaint();


                // move the icons
                peptidesLayeredPane.getComponent(0).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        peptidesLayeredPane.getComponent(0).getWidth(),
                        peptidesLayeredPane.getComponent(0).getHeight());

                peptidesLayeredPane.getComponent(1).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        peptidesLayeredPane.getComponent(1).getWidth(),
                        peptidesLayeredPane.getComponent(1).getHeight());

                peptidesLayeredPane.getComponent(2).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        peptidesLayeredPane.getComponent(2).getWidth(),
                        peptidesLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                peptidesLayeredPane.getComponent(3).setBounds(0, 0, peptidesLayeredPane.getWidth(), peptidesLayeredPane.getHeight());
                peptidesLayeredPane.revalidate();
                peptidesLayeredPane.repaint();


                // move the icons
                pdbMatchesLayeredPane.getComponent(0).setBounds(
                        pdbMatchesLayeredPane.getWidth() - pdbMatchesLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        pdbMatchesLayeredPane.getComponent(0).getWidth(),
                        pdbMatchesLayeredPane.getComponent(0).getHeight());

                pdbMatchesLayeredPane.getComponent(1).setBounds(
                        pdbMatchesLayeredPane.getWidth() - pdbMatchesLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        pdbMatchesLayeredPane.getComponent(1).getWidth(),
                        pdbMatchesLayeredPane.getComponent(1).getHeight());

                pdbMatchesLayeredPane.getComponent(2).setBounds(
                        pdbMatchesLayeredPane.getWidth() - pdbMatchesLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        pdbMatchesLayeredPane.getComponent(2).getWidth(),
                        pdbMatchesLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                pdbMatchesLayeredPane.getComponent(3).setBounds(0, 0, pdbMatchesLayeredPane.getWidth(), pdbMatchesLayeredPane.getHeight());
                pdbMatchesLayeredPane.revalidate();
                pdbMatchesLayeredPane.repaint();


                // move the icons
                pdbChainsLayeredPane.getComponent(0).setBounds(
                        pdbChainsLayeredPane.getWidth() - pdbChainsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        pdbChainsLayeredPane.getComponent(0).getWidth(),
                        pdbChainsLayeredPane.getComponent(0).getHeight());

                pdbChainsLayeredPane.getComponent(1).setBounds(
                        pdbChainsLayeredPane.getWidth() - pdbChainsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        pdbChainsLayeredPane.getComponent(1).getWidth(),
                        pdbChainsLayeredPane.getComponent(1).getHeight());

                pdbChainsLayeredPane.getComponent(2).setBounds(
                        pdbChainsLayeredPane.getWidth() - pdbChainsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        pdbChainsLayeredPane.getComponent(2).getWidth(),
                        pdbChainsLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                pdbChainsLayeredPane.getComponent(3).setBounds(0, 0, pdbChainsLayeredPane.getWidth(), pdbChainsLayeredPane.getHeight());
                pdbChainsLayeredPane.revalidate();
                pdbChainsLayeredPane.repaint();


                // move the icons
                pdbStructureLayeredPane.getComponent(0).setBounds(
                        pdbStructureLayeredPane.getWidth() - pdbStructureLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        pdbStructureLayeredPane.getComponent(0).getWidth(),
                        pdbStructureLayeredPane.getComponent(0).getHeight());

                pdbStructureLayeredPane.getComponent(1).setBounds(
                        pdbStructureLayeredPane.getWidth() - pdbStructureLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        pdbStructureLayeredPane.getComponent(1).getWidth(),
                        pdbStructureLayeredPane.getComponent(1).getHeight());

                pdbStructureLayeredPane.getComponent(2).setBounds(
                        pdbStructureLayeredPane.getWidth() - pdbStructureLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        pdbStructureLayeredPane.getComponent(2).getWidth(),
                        pdbStructureLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                pdbStructureLayeredPane.getComponent(3).setBounds(0, 0, pdbStructureLayeredPane.getWidth(), pdbStructureLayeredPane.getHeight());
                pdbStructureLayeredPane.revalidate();
                pdbStructureLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void peptidesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void peptidesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void peptidesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PDB.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportPeptidesJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPeptidesJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportPeptidesJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPeptidesJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportPeptidesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonActionPerformed
        copyTableContentToClipboard(TableIndex.PEPTIDE_TABLE);
    }//GEN-LAST:event_exportPeptidesJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void pdbMatchesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbMatchesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pdbMatchesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void pdbMatchesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbMatchesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbMatchesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void pdbMatchesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdbMatchesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PDB.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbMatchesHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportPdbMatchesJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPdbMatchesJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPdbMatchesJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportPdbMatchesJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPdbMatchesJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPdbMatchesJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportPdbMatchesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPdbMatchesJButtonActionPerformed
        copyTableContentToClipboard(TableIndex.PDB_MATCHES);
    }//GEN-LAST:event_exportPdbMatchesJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void pdbChainHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbChainHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pdbChainHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void pdbChainHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbChainHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbChainHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void pdbChainHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdbChainHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PDB.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbChainHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportPdbChainsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPdbChainsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPdbChainsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportPdbChainsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPdbChainsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPdbChainsJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportPdbChainsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPdbChainsJButtonActionPerformed
        copyTableContentToClipboard(TableIndex.PDB_CHAINS);
    }//GEN-LAST:event_exportPdbChainsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void pdbStructureHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbStructureHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_pdbStructureHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void pdbStructureHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pdbStructureHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbStructureHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void pdbStructureHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdbStructureHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PDB.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_pdbStructureHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportPdbStructureJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPdbStructureJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPdbStructureJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportPdbStructureJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPdbStructureJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPdbStructureJButtonMouseExited

    /**
     * Export the pdb structure.
     * 
     * @param evt 
     */
    private void exportPdbStructureJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPdbStructureJButtonActionPerformed
        new ExportGraphicsDialog(peptideShakerGUI, true, pdbPanel);

        // @TODO: use Jmol's export options...
    }//GEN-LAST:event_exportPdbStructureJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backboneJButton;
    private javax.swing.JPanel contextMenuPdbChainsBackgroundPanel;
    private javax.swing.JPanel contextMenuPdbMatchesBackgroundPanel;
    private javax.swing.JPanel contextMenuPdbStructureBackgroundPanel;
    private javax.swing.JPanel contextMenuPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.JButton exportPdbChainsJButton;
    private javax.swing.JButton exportPdbMatchesJButton;
    private javax.swing.JButton exportPdbStructureJButton;
    private javax.swing.JButton exportPeptidesJButton;
    private javax.swing.JButton exportProteinsJButton;
    private javax.swing.JButton labelsJButton;
    private javax.swing.JButton pdbChainHelpJButton;
    private javax.swing.JPanel pdbChainsJPanel;
    private javax.swing.JScrollPane pdbChainsJScrollPane;
    private javax.swing.JTable pdbChainsJTable;
    private javax.swing.JLayeredPane pdbChainsLayeredPane;
    private javax.swing.JPanel pdbChainsPanel;
    private javax.swing.JScrollPane pdbJScrollPane;
    private javax.swing.JLayeredPane pdbLayeredPane;
    private javax.swing.JButton pdbMatchesHelpJButton;
    private javax.swing.JPanel pdbMatchesJPanel;
    private javax.swing.JTable pdbMatchesJTable;
    private javax.swing.JLayeredPane pdbMatchesLayeredPane;
    private javax.swing.JPanel pdbMatchesPanel;
    private javax.swing.JPanel pdbOuterPanel;
    private javax.swing.JPanel pdbPanel;
    private javax.swing.JButton pdbStructureHelpJButton;
    private javax.swing.JPanel pdbStructureJPanel;
    private javax.swing.JLayeredPane pdbStructureLayeredPane;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JButton peptidesHelpJButton;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JLayeredPane peptidesLayeredPane;
    private javax.swing.JPanel peptidesPanel;
    private javax.swing.JButton playJButton;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JButton proteinsHelpJButton;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JLayeredPane proteinsLayeredPane;
    private javax.swing.JPanel proteinsPanel;
    private javax.swing.JButton ribbonJButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Select the given protein index in the protein table.
     * 
     * @param proteinIndex the protein index to select
     */
    public void setSelectedProteinIndex(Integer proteinIndex) {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        boolean indexFound = false;

        for (int i = 0; i < proteinTable.getRowCount() && !indexFound; i++) {
            if (((Integer) proteinTable.getValueAt(i, 0)).intValue() == proteinIndex.intValue()) {
                indexFound = true;
                proteinTable.setRowSelectionInterval(i, i);
                proteinTable.scrollRectToVisible(proteinTable.getCellRect(i, 0, false));
            }
        }

        updateOverviewPanel = false;
        proteinTableMouseReleased(null);
        updateOverviewPanel = true;

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Select the given peptide index in the peptide table.
     * 
     * @param peptideIndex the peptide index to select
     */
    public void setSelectedPeptideIndex(Integer peptideIndex) {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        boolean indexFound = false;

        for (int i = 0; i < peptideTable.getRowCount() && !indexFound; i++) {
            if (((Integer) peptideTable.getValueAt(i, 0)).intValue() == peptideIndex.intValue()) {
                indexFound = true;
                peptideTable.setRowSelectionInterval(i, i);
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(i, 0, false));

                peptideTableMouseReleased(null);
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns a list of keys of the displayed proteins
     * @return a list of keys of the displayed proteins 
     */
    public ArrayList<String> getDisplayedProteins() {
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
     * Returns the peptide key for the given row.
     *
     * @param row   the given row
     * @return      the peptide key
     */
    private Integer getPeptideKey(int row) {
        return (Integer) peptideTable.getValueAt(row, 0);
    }

    /**
     * Updates the peptide selection according to the currently selected protein.
     *
     * @param row   the row index of the protein
     */
    private void updatedPeptideSelection(int row) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            try {
                DefaultTableModel dm = (DefaultTableModel) peptideTable.getModel();
                dm.getDataVector().removeAllElements();

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

                int index = 0;
                int validatedPeptideCounter = 0;
                ArrayList<Integer> nSpectra;
                ArrayList<String> keys;
                PeptideMatch currentMatch;

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
                                String tempProteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();
                                peptideStart = tempProteinSequence.lastIndexOf(peptideSequence) + 1;
                                peptideEnd = peptideStart + peptideSequence.length() - 1;
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
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
                                        false,
                                        probabilities.isValidated()
                                    });

                            if (probabilities.isValidated()) {
                                validatedPeptideCounter++;
                            }

                            peptideTableMap.put(index + 1, currentMatch.getKey());
                            index++;
                        }
                    }
                }

                ((TitledBorder) peptidesPanel.getBorder()).setTitle("Peptides (" + validatedPeptideCounter + "/" + peptideTable.getRowCount() + ")");
                peptidesPanel.repaint();

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
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI, peptideShakerGUI, true);
        progressDialog.doNothingOnClose();

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Updating Data. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {

            @Override
            public void run() {

                // change the peptide shaker icon to a "waiting version"
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                // update spectrum counting column header tooltip
                if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.EMPAI) {
                    proteinTableToolTips.set(proteinTable.getColumn("MS2 Quantification").getModelIndex(), "Protein MS2 Quantification - emPAI");
                } else if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.NSAF) {
                    proteinTableToolTips.set(proteinTable.getColumn("MS2 Quantification").getModelIndex(), "Protein MS2 Quantification - NSAF");
                } else {
                    proteinTableToolTips.set(proteinTable.getColumn("MS2 Quantification").getModelIndex(), "Protein MS2 Quantification");
                }

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

                    progressDialog.setTitle("Getting Identifications. Please Wait...");
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(peptideShakerGUI.getIdentification().getProteinIdentification().size());
                    progressDialog.setValue(0);

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

                        } else {
                            progressDialog.incrementValue();
                        }
                    }

                    Collections.sort(scores);
                    proteinTableMap = new HashMap<Integer, String>();
                    // add the proteins to the table
                    ArrayList<Integer> nP, nS;
                    ArrayList<String> keys;

                    int validatedProteinsCounter = 0;

                    progressDialog.setTitle("Updating Protein Table. Please Wait...");
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(scores.size());
                    progressDialog.setValue(0);

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

                                    if (maxSpectra < -currentNS) {
                                        maxSpectra = -currentNS;
                                    }
                                }
                                if (maxPeptides < -currentNP) {
                                    maxPeptides = -currentNP;
                                }
                            }
                        }

                        progressDialog.incrementValue();
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

                    ((TitledBorder) proteinsPanel.getBorder()).setTitle("Proteins (" + validatedProteinsCounter + "/" + proteinTable.getRowCount() + ")");
                    proteinsPanel.repaint();

                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quantification").getCellRenderer()).setMaxValue(maxSpectrumCounting);
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);

                    try {
                        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
                    } catch (IllegalArgumentException e) {
                        // ignore error
                    }

                    // enable the contextual export options
                    exportPdbStructureJButton.setEnabled(true);
                    exportProteinsJButton.setEnabled(true);
                    exportPeptidesJButton.setEnabled(true);
                    exportPdbMatchesJButton.setEnabled(true);
                    exportPdbChainsJButton.setEnabled(true);

                    tabInitiated = true;
                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    updateSelectedProteinAndPeptide(peptideShakerGUI.getSelectedProteinIndex(), peptideShakerGUI.getSelectedPeptideIndex());

                    // return the peptide shaker icon to the standard version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                } catch (Exception e) {
                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                    peptideShakerGUI.catchException(e);
                }
            }
        }.start();
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
    private void updatePdbTable(String aProteinKey) {

        final String proteinKey = aProteinKey;

        progressDialog = new ProgressDialogX(peptideShakerGUI, this, true);
        progressDialog.setIndeterminate(true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setTitle("Getting PDB Data. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("ExtractThread") {

            @Override
            public void run() {

                try {
                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                    // get the accession number of the main match
                    ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                    String tempAccession = proteinMatch.getMainMatch();

                    // find the pdb matches
                    uniProtPdb = new FindPdbForUniprotAccessions(tempAccession);

                    // delete the previous matches
                    DefaultTableModel dm = (DefaultTableModel) pdbMatchesJTable.getModel();
                    dm.getDataVector().removeAllElements();

                    dm = (DefaultTableModel) pdbChainsJTable.getModel();
                    dm.getDataVector().removeAllElements();

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
                        ((TitledBorder) pdbMatchesPanel.getBorder()).setTitle("PDB Matches - Not Available Without Internet Connection!");
                    } else {
                        ((TitledBorder) pdbMatchesPanel.getBorder()).setTitle("PDB Matches (" + pdbMatchesJTable.getRowCount() + ")");
                    }

                    pdbMatchesPanel.repaint();

                    ((TitledBorder) pdbChainsPanel.getBorder()).setTitle("PDB Chains");
                    pdbChainsPanel.repaint();

                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                    // return the peptide shaker icon to the standard version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    // return the peptide shaker icon to the standard version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        }.start();
    }

    @Override
    public void cancelProgress() {
        // do nothing
    }

    /**
     * Updates the model type if the jmol structure is currently visible.
     */
    public void updateModelType() {
        if (jmolStructureShown) {
            if (ribbonModel) {
                jmolPanel.getViewer().evalString("select all; ribbon; backbone off");
            } else if (backboneModel) {
                jmolPanel.getViewer().evalString("select all; backbone 100; ribbon off");
            }
        }
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
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quantification").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            // ignore error
        }

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

        peptidePdbArray = new ArrayList<String>();

        // iterate the peptide table and highlight the covered areas
        for (int i = 0; i < peptideTable.getRowCount(); i++) {
            String peptideKey = peptideTableMap.get(getPeptideKey(i));
            String peptideSequence = Peptide.getSequence(peptideKey);
            String tempSequence = proteinSequence;

            while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                int peptideTempEnd = peptideTempStart + peptideSequence.length() - 1;

                jmolPanel.getViewer().evalString(
                        "select resno >=" + (peptideTempStart - chains[selectedChainIndex - 1].getDifference())
                        + " and resno <=" + (peptideTempEnd - chains[selectedChainIndex - 1].getDifference())
                        + " and chain = " + currentChain + "; color green");

                tempSequence = proteinSequence.substring(0, peptideTempEnd - 1);

                if (chainSequence.indexOf(peptideSequence) != -1) {
                    peptideTable.setValueAt(true, i, peptideTable.getColumn("PDB").getModelIndex());
                    peptidePdbArray.add(peptideKey);
                }
            }
        }


        // highlight the selected peptide
        String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
        String peptideSequence = Peptide.getSequence(peptideKey);
        String tempSequence = proteinSequence;
        PTMFactory pmtFactory = PTMFactory.getInstance();
        PTM ptm;

        while (tempSequence.lastIndexOf(peptideSequence) >= 0) {

            int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
            int peptideTempEnd = peptideTempStart + peptideSequence.length() - 1;

            jmolPanel.getViewer().evalString(
                    "select resno >=" + (peptideTempStart - chains[selectedChainIndex - 1].getDifference())
                    + " and resno <=" + (peptideTempEnd - chains[selectedChainIndex - 1].getDifference())
                    + " and chain = " + currentChain + "; color blue");

            tempSequence = proteinSequence.substring(0, peptideTempEnd - 1);
        }


        // remove old labels
        jmolPanel.getViewer().evalString("select all; label off");


        // annotate the modified covered residues
        for (int i = 0; i < peptideTable.getRowCount(); i++) {
            peptideKey = peptideTableMap.get(getPeptideKey(i));
            peptideSequence = Peptide.getSequence(peptideKey);
            tempSequence = proteinSequence;

            ArrayList<ModificationMatch> modifications = new ArrayList<ModificationMatch>();

            try {
                modifications = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModificationMatches();
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                e.printStackTrace();
            }

            while (tempSequence.lastIndexOf(peptideSequence) >= 0) {

                int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                int peptideTempEnd = peptideTempStart + peptideSequence.length() - 1;

                int peptideIndex = 0;

                for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                    for (int k = 0; k < modifications.size(); k++) {
                        ptm = pmtFactory.getPTM(modifications.get(k).getTheoreticPtm());
                        if (ptm.getType() == PTM.MODAA && modifications.get(k).isVariable()) {
                            if (modifications.get(k).getModificationSite() == (peptideIndex + 1)) {

                                Color ptmColor = peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors().get(
                                        modifications.get(k).getTheoreticPtm());

                                jmolPanel.getViewer().evalString(
                                        "select resno =" + (j - chains[selectedChainIndex - 1].getDifference())
                                        + " and chain = " + currentChain + "; color ["
                                        + ptmColor.getRed() + "," + ptmColor.getGreen() + "," + ptmColor.getBlue() + "]");

                                if (showModificationLabels) {
                                    jmolPanel.getViewer().evalString(
                                            "select resno =" + (j - chains[selectedChainIndex - 1].getDifference())
                                            + " and chain = " + currentChain + " and *.ca; color ["
                                            + ptmColor.getRed() + "," + ptmColor.getGreen() + "," + ptmColor.getBlue() + "];"
                                            + "label " + modifications.get(k).getTheoreticPtm());
                                }
                            }
                        }
                    }

                    peptideIndex++;
                }

                tempSequence = proteinSequence.substring(0, peptideTempEnd - 1);
            }
        }

        // resort the peptide table, required if sorted on the pdb column and the structure is changed
        peptideTable.getRowSorter().allRowsChanged();

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the main match for the given row in the protein table.
     * 
     * @param mainMatch             the protein match to use
     * @param proteinInferenceType  the protein inference group type
     */
    public void updateMainMatch(String mainMatch, int proteinInferenceType) {
        proteinTable.setValueAt(peptideShakerGUI.addDatabaseLink(mainMatch), proteinTable.getSelectedRow(), proteinTable.getColumn("Accession").getModelIndex());
        proteinTable.setValueAt(proteinInferenceType, proteinTable.getSelectedRow(), proteinTable.getColumn("PI").getModelIndex());
        String description = "";
        try {
            description = sequenceFactory.getHeader(mainMatch).getDescription();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        proteinTable.setValueAt(description, proteinTable.getSelectedRow(), proteinTable.getColumn("Description").getModelIndex());
    }

    /**
     * Turns the spinning of the model on or off.
     * 
     * @param spin if true the spinning is turned on.
     */
    public void spinModel(boolean spin) {

        if (spin) {
            jmolPanel.getViewer().evalString("set spin y 20; spin");
        } else {
            jmolPanel.getViewer().evalString("spin off");
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
     * Hides or displays the score columns in the protein and peptide tables.
     * 
     * @param hide if true the score columns are hidden.
     */
    public void hideScores(boolean hide) {

        try {
            if (hide) {
                proteinTable.removeColumn(proteinTable.getColumn("Score"));
            } else {
                proteinTable.addColumn(proteinScoreColumn);
                proteinTable.moveColumn(10, 8);
            }
        } catch (IllegalArgumentException e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
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
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        // update the 3D structure
        peptideTableMouseReleased(null);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the protein inference type for the currently selected peptide.
     * 
     * @param proteinInferenceType 
     */
    public void updatePeptideProteinInference(int proteinInferenceType) {
        peptideTable.setValueAt(proteinInferenceType, peptideTable.getSelectedRow(), peptideTable.getColumn("PI").getModelIndex());
    }

    /**
     * Export the table contents to the clipboard.
     * 
     * @param index 
     */
    private void copyTableContentToClipboard(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.PROTEIN_TABLE
                || tableIndex == TableIndex.PEPTIDE_TABLE
                || tableIndex == TableIndex.PDB_MATCHES
                || tableIndex == TableIndex.PDB_CHAINS) {

            progressDialog = new ProgressDialogX(peptideShakerGUI, peptideShakerGUI, true);
            progressDialog.doNothingOnClose();

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIndeterminate(true);
                    progressDialog.setTitle("Copying to Clipboard. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {

                @Override
                public void run() {
                    try {
                        String clipboardString = "";
                        FeaturesGenerator outputGenerator = new FeaturesGenerator(peptideShakerGUI);

                        if (tableIndex == TableIndex.PROTEIN_TABLE) {
                            ArrayList<String> selectedProteins = getDisplayedProteins();
                            clipboardString = outputGenerator.getProteinsOutput(
                                    progressDialog, selectedProteins, true, false, true, true,
                                    true, true, true, true, true,
                                    true, true, true, true);
                        } else if (tableIndex == TableIndex.PEPTIDE_TABLE) {
                            ArrayList<String> selectedPeptides = getDisplayedPeptides();
                            clipboardString = outputGenerator.getPeptidesOutput(
                                    progressDialog, selectedPeptides, peptidePdbArray, true, false, true, true,
                                    true, true, true, true, true, true, true);
                        } else if (tableIndex == TableIndex.PDB_MATCHES) {
                            clipboardString = Util.tableToText(pdbMatchesJTable, "\t", progressDialog, true);
                        } else if (tableIndex == TableIndex.PDB_CHAINS) {

                            clipboardString += "\tChain\tPDB-Start\tPDB-End\tCoverage\n";

                            for (int i = 0; i < pdbChainsJTable.getRowCount(); i++) {
                                clipboardString += pdbChainsJTable.getValueAt(i, 0) + "\t";
                                clipboardString += pdbChainsJTable.getValueAt(i, 1) + "\t";
                                XYDataPoint[] pdbCoverage = (XYDataPoint[]) pdbChainsJTable.getValueAt(i, 2);
                                clipboardString += pdbCoverage[0].getX() + "\t" + pdbCoverage[0].getY() + "\t";
                                clipboardString += pdbChainsJTable.getValueAt(i, 3) + "\n";
                            }
                        }

                        StringSelection stringSelection = new StringSelection(clipboardString);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stringSelection, peptideShakerGUI);

                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);

                    } catch (Exception e) {
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
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
     * Update the selected protein and peptide.
     * 
     * @param aSelectedProteinIndex     the selected protein index
     * @param aSelectedPeptideIndex     the selected peptide index
     */
    public void updateSelectedProteinAndPeptide(int aSelectedProteinIndex, int aSelectedPeptideIndex) {

        final int selectedProteinIndex = aSelectedProteinIndex;
        final int selectedPeptideIndex = aSelectedPeptideIndex;
        int proteinRow = proteinTable.getSelectedRow();

        if (proteinRow != -1) {

            int currentProteinIndex = (Integer) proteinTable.getValueAt(proteinRow, 0);

            if (currentProteinIndex != selectedProteinIndex) {

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        setSelectedProteinIndex(selectedProteinIndex);
                    }
                });
            }
        } else {
            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setSelectedProteinIndex(selectedProteinIndex);
                }
            });
        }

        if (selectedPeptideIndex != -1) {

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {

                    int peptideRow = getPeptideTable().getSelectedRow();

                    if (peptideRow != -1) {

                        int currentPeptideIndex = (Integer) peptideTable.getValueAt(peptideRow, 0);

                        if (currentPeptideIndex != selectedPeptideIndex) {
                            setSelectedPeptideIndex(selectedPeptideIndex);
                        }
                    } else {
                        setSelectedPeptideIndex(selectedPeptideIndex);
                    }
                }
            });
        }
    }
}
