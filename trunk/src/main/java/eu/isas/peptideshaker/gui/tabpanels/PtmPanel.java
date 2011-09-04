package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.gui.events.RescalingEvent;
import com.compomics.util.gui.interfaces.SpectrumPanelListener;
import com.compomics.util.gui.protein.SequenceModificationPanel;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.scoring.PtmScoring;
import com.compomics.util.gui.protein.ModificationProfile;
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
import java.util.Iterator;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * The PTM tab.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmPanel extends javax.swing.JPanel {

    /**
     * A reference to the selected peptides score column.
     */
    private TableColumn selectedPeptidesScoreColumn;
    /**
     * A reference to the related peptides score column.
     */
    private TableColumn relatedPeptidesScoreColumn;
    /**
     * The selected peptides table column header tooltips.
     */
    private ArrayList<String> selectedPeptidesTableToolTips;
    /**
     * The related peptides table column header tooltips.
     */
    private ArrayList<String> relatedPeptidesTableToolTips;
    /**
     * The selected psms table column header tooltips.
     */
    private ArrayList<String> selectedPsmsTableToolTips;
    /**
     * The related psms table column header tooltips.
     */
    private ArrayList<String> relatedPsmsTableToolTips;
    /**
     * A hashmap of both the linked spectra.
     */
    private HashMap<Integer, SpectrumPanel> linkedSpectrumPanels;
    /**
     * The spectrum annotator for the first spectrum
     */
    private SpectrumAnnotator annotatorA = new SpectrumAnnotator();
    /**
     * The spectrum annotator for the second spectrum
     */
    private SpectrumAnnotator annotatorB = new SpectrumAnnotator();
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * Map of all peptide keys indexed by their modification status
     */
    private HashMap<String, ArrayList<String>> peptideMap = new HashMap<String, ArrayList<String>>();
    /**
     * The modification name for no modification
     */
    private final String NO_MODIFICATION = "no modification";
    /**
     * The displayed identification
     */
    private Identification identification;
    /**
     * The keys of the peptides currently displayed
     */
    private ArrayList<String> displayedPeptides = new ArrayList<String>();
    /**
     * The keys of the related peptides currently displayed
     */
    private ArrayList<String> relatedPeptides = new ArrayList<String>();
    /**
     * A map of all psm keys sorted by family type: related peptide, modification position
     */
    private HashMap<String, ArrayList<String>> psmsMap = new HashMap<String, ArrayList<String>>();
    /**
     * A map of psm family confidence
     */
    private HashMap<String, Double> confidenceMap = new HashMap<String, Double>();
    /**
     * The current spectrum panel for the upper psm.
     */
    private SpectrumPanel spectrumA;
    /**
     * The current spectrum panel for lower psm.
     */
    private SpectrumPanel spectrumB;
    /**
     * The PTM color coding map for the cell renderer.
     */
    HashMap<Integer, Color> ptmColorMap;
    /**
     * The PTM color coding map.
     */
    HashMap<String, Color> ptmColors;
    /**
     * The PTM color tooltips.
     */
    HashMap<Integer, String> ptmColorToolTips;

    /**
     * Creates a new PTM tab.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public PtmPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        ptmColors = new HashMap<String, Color>();

        selectedPeptidesScoreColumn = peptidesTable.getColumn("Score");
        relatedPeptidesScoreColumn = relatedPeptidesTable.getColumn("Score");

        relatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsModifiedTableJScrollPane.getViewport().setOpaque(false);
        peptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsRelatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        ptmJScrollPane.getViewport().setOpaque(false);

        primarySelectionJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        secondarySelectionJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        spectrumTabbedPane.setEnabledAt(0, false);

        // @TODO: remove hardcoding of ptm color!!

        // set up the ptm color map
        ptmColorMap = new HashMap<Integer, Color>();
        ptmColorMap.put(0, Color.MAGENTA);
        ptmColorMap.put(1, Color.ORANGE);
        ptmColorMap.put(2, Color.BLUE);
        ptmColorMap.put(3, Color.RED);
        ptmColorMap.put(4, Color.GREEN);
        ptmColorMap.put(5, Color.PINK);
        ptmColorMap.put(6, Color.CYAN);

        // @TODO: remove hardcoding of ptm color tooltips!!

        ptmColorToolTips = new HashMap<Integer, String>();
        ptmColorToolTips.put(0, "PTM Color");
        ptmColorToolTips.put(1, "PTM Color");
        ptmColorToolTips.put(2, "PTM Color");
        ptmColorToolTips.put(3, "PTM Color");
        ptmColorToolTips.put(4, "PTM Color");
        ptmColorToolTips.put(5, "PTM Color");
        ptmColorToolTips.put(6, "PTM Color");
        ptmColorToolTips.put(100, "PTM Color");

        setTableProperties();

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        ptmJTable.getColumn(" ").setMaxWidth(35);
        ptmJTable.getColumn(" ").setMinWidth(35);
        ptmJTable.getColumn("MC").setMaxWidth(35);
        ptmJTable.getColumn("MC").setMinWidth(35);

        peptidesTable.getColumn(" ").setMaxWidth(60);
        peptidesTable.getColumn(" ").setMinWidth(60);
        relatedPeptidesTable.getColumn(" ").setMaxWidth(60);
        relatedPeptidesTable.getColumn(" ").setMinWidth(60);
        peptidesTable.getColumn("  ").setMaxWidth(30);
        peptidesTable.getColumn("  ").setMinWidth(30);
        relatedPeptidesTable.getColumn("  ").setMaxWidth(30);
        relatedPeptidesTable.getColumn("  ").setMinWidth(30);

        selectedPsmTable.getColumn("Rank").setMaxWidth(65);
        selectedPsmTable.getColumn("Rank").setMinWidth(65);
        relatedPsmTable.getColumn("Rank").setMaxWidth(65);
        relatedPsmTable.getColumn("Rank").setMinWidth(65);

        peptidesTable.getTableHeader().setReorderingAllowed(false);
        relatedPeptidesTable.getTableHeader().setReorderingAllowed(false);
        selectedPsmTable.getTableHeader().setReorderingAllowed(false);
        relatedPsmTable.getTableHeader().setReorderingAllowed(false);

        peptidesTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        peptidesTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth(), peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        peptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        relatedPeptidesTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        relatedPeptidesTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth(), peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        relatedPeptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        selectedPsmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) selectedPsmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);

        relatedPsmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPsmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);

        try {
            peptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth(), peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            relatedPeptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth(), peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            // ignore error
        }

        // ptm color coding
        ptmJTable.getColumn("MC").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(new Color(240, 240, 240), ptmColorMap, ptmColorToolTips));

        // set up the table header tooltips
        selectedPeptidesTableToolTips = new ArrayList<String>();
        selectedPeptidesTableToolTips.add(null);
        selectedPeptidesTableToolTips.add("Mapping Protein(s)");
        selectedPeptidesTableToolTips.add("Peptide Sequence");
        selectedPeptidesTableToolTips.add("Peptide Modifications");
        selectedPeptidesTableToolTips.add("Peptide Score");
        selectedPeptidesTableToolTips.add("Peptide Confidence");
        selectedPeptidesTableToolTips.add("Validated");

        relatedPeptidesTableToolTips = new ArrayList<String>();
        relatedPeptidesTableToolTips.add(null);
        relatedPeptidesTableToolTips.add("Mapping Protein(s)");
        relatedPeptidesTableToolTips.add("Peptide Sequence");
        relatedPeptidesTableToolTips.add("Peptide Modifications");
        relatedPeptidesTableToolTips.add("Peptide Score");
        relatedPeptidesTableToolTips.add("Peptide Confidence");
        selectedPeptidesTableToolTips.add("Validated");

        selectedPsmsTableToolTips = new ArrayList<String>();
        selectedPsmsTableToolTips.add("Search Engine Ranks");
        selectedPsmsTableToolTips.add("Peptide Sequence");
        selectedPsmsTableToolTips.add("Peptide Modifications");
        selectedPsmsTableToolTips.add("Precursor m/z");
        selectedPsmsTableToolTips.add("Precursor Charge");

        relatedPsmsTableToolTips = new ArrayList<String>();
        relatedPsmsTableToolTips.add("Search Engine Ranks");
        relatedPsmsTableToolTips.add("Peptide Sequence");
        relatedPsmsTableToolTips.add("Peptide Modifications");
        relatedPsmsTableToolTips.add("Precursor m/z");
        relatedPsmsTableToolTips.add("Precursor Charge");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel8 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        ptmJScrollPane = new javax.swing.JScrollPane();
        ptmJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) selectedPsmsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        peptideTablesJSplitPane = new javax.swing.JSplitPane();
        selectedPeptidesJPanel = new javax.swing.JPanel();
        selectedPeptidesJSplitPane = new javax.swing.JSplitPane();
        peptidesTableJScrollPane = new javax.swing.JScrollPane();
        peptidesTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) selectedPeptidesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        modificationProfileSelectedPeptideJPanel = new javax.swing.JPanel();
        relatedPeptidesJPanel = new javax.swing.JPanel();
        relatedPeptidesJSplitPane = new javax.swing.JSplitPane();
        relatedPeptidesTableJScrollPane = new javax.swing.JScrollPane();
        relatedPeptidesTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) relatedPeptidesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        modificationProfileRelatedPeptideJPanel = new javax.swing.JPanel();
        psmSpectraSplitPane = new javax.swing.JSplitPane();
        psmSplitPane = new javax.swing.JSplitPane();
        jPanel7 = new javax.swing.JPanel();
        psmsModifiedTableJScrollPane = new javax.swing.JScrollPane();
        selectedPsmTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) selectedPsmsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        primarySelectionJComboBox = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        psmsRelatedPeptidesTableJScrollPane = new javax.swing.JScrollPane();
        relatedPsmTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) relatedPsmsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        secondarySelectionJComboBox = new javax.swing.JComboBox();
        spectrumAndFragmentIonPanel = new javax.swing.JPanel();
        spectrumTabbedPane = new javax.swing.JTabbedPane();
        fragmentIonsJPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumChartJPanel = new javax.swing.JPanel();
        intensitySlider = new javax.swing.JSlider();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jPanel8.setOpaque(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Variable Modifications"));
        jPanel1.setOpaque(false);

        ptmJScrollPane.setOpaque(false);

        ptmJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "MC", "PTM"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ptmJTable.setOpaque(false);
        ptmJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        ptmJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                ptmJTableMouseReleased(evt);
            }
        });
        ptmJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                ptmJTableKeyReleased(evt);
            }
        });
        ptmJScrollPane.setViewportView(ptmJTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setBorder(null);
        peptideTablesJSplitPane.setDividerLocation(170);
        peptideTablesJSplitPane.setDividerSize(0);
        peptideTablesJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptideTablesJSplitPane.setResizeWeight(0.5);
        peptideTablesJSplitPane.setOpaque(false);

        selectedPeptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Peptides"));
        selectedPeptidesJPanel.setOpaque(false);

        selectedPeptidesJSplitPane.setBorder(null);
        selectedPeptidesJSplitPane.setDividerLocation(400);
        selectedPeptidesJSplitPane.setDividerSize(0);
        selectedPeptidesJSplitPane.setResizeWeight(0.5);
        selectedPeptidesJSplitPane.setOpaque(false);

        peptidesTableJScrollPane.setOpaque(false);

        peptidesTable.setModel(new PeptideTable()
        );
        peptidesTable.setOpaque(false);
        peptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptidesTableMouseReleased(evt);
            }
        });
        peptidesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptidesTableMouseMoved(evt);
            }
        });
        peptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidesTableKeyReleased(evt);
            }
        });
        peptidesTableJScrollPane.setViewportView(peptidesTable);

        selectedPeptidesJSplitPane.setLeftComponent(peptidesTableJScrollPane);

        modificationProfileSelectedPeptideJPanel.setBackground(new java.awt.Color(255, 255, 255));
        modificationProfileSelectedPeptideJPanel.setOpaque(false);
        modificationProfileSelectedPeptideJPanel.setLayout(new java.awt.GridLayout(3, 1));
        selectedPeptidesJSplitPane.setRightComponent(modificationProfileSelectedPeptideJPanel);

        javax.swing.GroupLayout selectedPeptidesJPanelLayout = new javax.swing.GroupLayout(selectedPeptidesJPanel);
        selectedPeptidesJPanel.setLayout(selectedPeptidesJPanelLayout);
        selectedPeptidesJPanelLayout.setHorizontalGroup(
            selectedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, selectedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
                .addContainerGap())
        );
        selectedPeptidesJPanelLayout.setVerticalGroup(
            selectedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setLeftComponent(selectedPeptidesJPanel);

        relatedPeptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Related Peptides"));
        relatedPeptidesJPanel.setOpaque(false);

        relatedPeptidesJSplitPane.setBorder(null);
        relatedPeptidesJSplitPane.setDividerLocation(400);
        relatedPeptidesJSplitPane.setDividerSize(0);
        relatedPeptidesJSplitPane.setOpaque(false);

        relatedPeptidesTableJScrollPane.setOpaque(false);

        relatedPeptidesTable.setModel(new RelatedPeptidesTable());
        relatedPeptidesTable.setOpaque(false);
        relatedPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        relatedPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseReleased(evt);
            }
        });
        relatedPeptidesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseMoved(evt);
            }
        });
        relatedPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPeptidesTableKeyReleased(evt);
            }
        });
        relatedPeptidesTableJScrollPane.setViewportView(relatedPeptidesTable);

        relatedPeptidesJSplitPane.setLeftComponent(relatedPeptidesTableJScrollPane);

        modificationProfileRelatedPeptideJPanel.setBackground(new java.awt.Color(255, 255, 255));
        modificationProfileRelatedPeptideJPanel.setOpaque(false);
        modificationProfileRelatedPeptideJPanel.setLayout(new java.awt.GridLayout(3, 1));
        relatedPeptidesJSplitPane.setRightComponent(modificationProfileRelatedPeptideJPanel);

        javax.swing.GroupLayout relatedPeptidesJPanelLayout = new javax.swing.GroupLayout(relatedPeptidesJPanel);
        relatedPeptidesJPanel.setLayout(relatedPeptidesJPanelLayout);
        relatedPeptidesJPanelLayout.setHorizontalGroup(
            relatedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, relatedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
                .addContainerGap())
        );
        relatedPeptidesJPanelLayout.setVerticalGroup(
            relatedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(relatedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setRightComponent(relatedPeptidesJPanel);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideTablesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 765, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptideTablesJSplitPane)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        psmSpectraSplitPane.setBorder(null);
        psmSpectraSplitPane.setDividerLocation(500);
        psmSpectraSplitPane.setDividerSize(0);
        psmSpectraSplitPane.setResizeWeight(0.5);
        psmSpectraSplitPane.setOpaque(false);

        psmSplitPane.setBorder(null);
        psmSplitPane.setDividerLocation(175);
        psmSplitPane.setDividerSize(0);
        psmSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        psmSplitPane.setResizeWeight(0.5);
        psmSplitPane.setOpaque(false);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("PSMs - Primary Selection"));
        jPanel7.setOpaque(false);

        psmsModifiedTableJScrollPane.setOpaque(false);

        selectedPsmTable.setModel(new SelectedPsmsTable());
        selectedPsmTable.setOpaque(false);
        selectedPsmTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        selectedPsmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                selectedPsmTableMouseReleased(evt);
            }
        });
        selectedPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                selectedPsmTableKeyReleased(evt);
            }
        });
        psmsModifiedTableJScrollPane.setViewportView(selectedPsmTable);

        primarySelectionJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                primarySelectionJComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                    .addComponent(primarySelectionJComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, 468, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(primarySelectionJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setLeftComponent(jPanel7);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("PSMs - Secondary Selection"));
        jPanel3.setOpaque(false);

        psmsRelatedPeptidesTableJScrollPane.setOpaque(false);

        relatedPsmTable.setModel(new RelatedPsmsTable());
        relatedPsmTable.setOpaque(false);
        relatedPsmTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        relatedPsmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedPsmTableMouseReleased(evt);
            }
        });
        relatedPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPsmTableKeyReleased(evt);
            }
        });
        psmsRelatedPeptidesTableJScrollPane.setViewportView(relatedPsmTable);

        secondarySelectionJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondarySelectionJComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(psmsRelatedPeptidesTableJScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                    .addComponent(secondarySelectionJComboBox, javax.swing.GroupLayout.Alignment.TRAILING, 0, 468, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(secondarySelectionJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmsRelatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setRightComponent(jPanel3);

        psmSpectraSplitPane.setLeftComponent(psmSplitPane);

        spectrumAndFragmentIonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumAndFragmentIonPanel.setOpaque(false);
        spectrumAndFragmentIonPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumAndFragmentIonPanelMouseWheelMoved(evt);
            }
        });

        spectrumTabbedPane.setBackground(new java.awt.Color(255, 255, 255));
        spectrumTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        fragmentIonsJPanel.setOpaque(false);

        javax.swing.GroupLayout fragmentIonsJPanelLayout = new javax.swing.GroupLayout(fragmentIonsJPanel);
        fragmentIonsJPanel.setLayout(fragmentIonsJPanelLayout);
        fragmentIonsJPanelLayout.setHorizontalGroup(
            fragmentIonsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 504, Short.MAX_VALUE)
        );
        fragmentIonsJPanelLayout.setVerticalGroup(
            fragmentIonsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 390, Short.MAX_VALUE)
        );

        spectrumTabbedPane.addTab("Ions", fragmentIonsJPanel);

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumChartJPanel.setOpaque(false);
        spectrumChartJPanel.setLayout(new javax.swing.BoxLayout(spectrumChartJPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumTabbedPane.setSelectedIndex(1);

        intensitySlider.setMaximum(99);
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

        javax.swing.GroupLayout spectrumAndFragmentIonPanelLayout = new javax.swing.GroupLayout(spectrumAndFragmentIonPanel);
        spectrumAndFragmentIonPanel.setLayout(spectrumAndFragmentIonPanelLayout);
        spectrumAndFragmentIonPanelLayout.setHorizontalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        spectrumAndFragmentIonPanelLayout.setVerticalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                .addGap(35, 35, 35))
        );

        psmSpectraSplitPane.setRightComponent(spectrumAndFragmentIonPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(psmSpectraSplitPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1078, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmSpectraSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the peptide table.
     *
     * @param evt
     */
    /**
     * Update the peptide table.
     *
     * @param evt
     */
    /**
     * @see #peptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void peptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidesTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            peptidesTableMouseReleased(null);

            try {
                PeptideMatch peptideMatch = identification.getPeptideMatch(displayedPeptides.get(peptidesTable.getSelectedRow()));
                updateModificationProfile(peptideMatch, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_peptidesTableKeyReleased

    /**
     * @see #relatedPeptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void relatedPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPeptidesTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            relatedPeptidesTableMouseReleased(null);

            try {
                PeptideMatch peptideMatch = identification.getPeptideMatch(relatedPeptides.get(relatedPeptidesTable.getSelectedRow()));
                updateModificationProfile(peptideMatch, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_relatedPeptidesTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void selectedPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_selectedPsmTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            updateSpectra();
        }
    }//GEN-LAST:event_selectedPsmTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void relatedPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPsmTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            updateSpectra();
        }
    }//GEN-LAST:event_relatedPsmTableKeyReleased

    /**
     * Resize the panels after frame resizing.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // update the splitters
        peptideTablesJSplitPane.setDividerLocation(peptideTablesJSplitPane.getHeight() / 2);
        psmSpectraSplitPane.setDividerLocation(psmSpectraSplitPane.getWidth() / 2);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                psmSplitPane.setDividerLocation(psmSplitPane.getHeight() / 2);
                selectedPeptidesJSplitPane.setDividerLocation(selectedPeptidesJSplitPane.getWidth() / 2);
                updateUI();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Update the primary psm table and then update the spectra.
     * 
     * @param evt 
     */
    private void primarySelectionJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_primarySelectionJComboBoxActionPerformed
        updateSelectedPsmTable();
        updateSpectra();
    }//GEN-LAST:event_primarySelectionJComboBoxActionPerformed

    /**
     * Update the secondary psm table and then update the spectra.
     * 
     * @param evt 
     */
    private void secondarySelectionJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondarySelectionJComboBoxActionPerformed
        updateRelatedPsmTable();
        updateSpectra();
    }//GEN-LAST:event_secondarySelectionJComboBoxActionPerformed

    /**
     * Update the related peptides and modified peptide psms tables.
     *
     * @param evt
     */
    private void peptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseReleased
        updateRelatedPeptidesTable();
        updatePeptideFamilies();
        updateSelectedPsmTable();
        updateRelatedPsmTable();

        if (evt != null) {

            int row = peptidesTable.rowAtPoint(evt.getPoint());
            int column = peptidesTable.columnAtPoint(evt.getPoint());

            if (row != -1) {

                try {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(displayedPeptides.get(peptidesTable.getSelectedRow()));
                    updateModificationProfile(peptideMatch, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (column == 1) {

                    // open protein links in web browser
                    if (evt.getButton() == MouseEvent.BUTTON1
                            && ((String) peptidesTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                        peptideShakerGUI.openProteinLinks((String) peptidesTable.getValueAt(row, column));
                    }
                }
            }
        }
    }//GEN-LAST:event_peptidesTableMouseReleased

    /**
     * Update the related peptides psm table.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseReleased
        updatePeptideFamilies();
        updateRelatedPsmTable();

        if (evt != null) {

            int row = relatedPeptidesTable.rowAtPoint(evt.getPoint());
            int column = relatedPeptidesTable.columnAtPoint(evt.getPoint());

            if (row != -1) {

                try {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(relatedPeptides.get(relatedPeptidesTable.getSelectedRow()));
                    updateModificationProfile(peptideMatch, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (column == 1) {

                    // open protein links in web browser
                    if (evt.getButton() == MouseEvent.BUTTON1
                            && ((String) relatedPeptidesTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                        peptideShakerGUI.openProteinLinks((String) relatedPeptidesTable.getValueAt(row, column));
                    }
                }
            }
        }
    }//GEN-LAST:event_relatedPeptidesTableMouseReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void selectedPsmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedPsmTableMouseReleased
        updateSpectra();
    }//GEN-LAST:event_selectedPsmTableMouseReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void relatedPsmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmTableMouseReleased
        updateSpectra();
    }//GEN-LAST:event_relatedPsmTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link.
     *
     * @param evt
     */
    private void peptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseMoved
        int row = peptidesTable.rowAtPoint(evt.getPoint());
        int column = peptidesTable.columnAtPoint(evt.getPoint());

        if (column == 1 && peptidesTable.getValueAt(row, column) != null) {

            String tempValue = (String) peptidesTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("a href=") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_peptidesTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptidesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesTableMouseExited

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedPeptidesTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseMoved
        int row = relatedPeptidesTable.rowAtPoint(evt.getPoint());
        int column = relatedPeptidesTable.columnAtPoint(evt.getPoint());

        if (column == 1 && relatedPeptidesTable.getValueAt(row, column) != null) {

            String tempValue = (String) relatedPeptidesTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("a href=") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_relatedPeptidesTableMouseMoved

    /**
     * Update the peptide table.
     * 
     * @param evt 
     */
private void ptmJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseReleased
    updatePeptideTable();
}//GEN-LAST:event_ptmJTableMouseReleased

/**
     * Update the peptide table.
     * 
     * @param evt 
     */
private void ptmJTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ptmJTableKeyReleased
    updatePeptideTable();
}//GEN-LAST:event_ptmJTableKeyReleased

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
private void intensitySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_intensitySliderMouseWheelMoved
    spectrumAndFragmentIonPanelMouseWheelMoved(evt);
}//GEN-LAST:event_intensitySliderMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     * 
     * @param evt 
     */
private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
    peptideShakerGUI.getAnnotationPreferences().setAnnotationIntensityLimit(((Integer) intensitySlider.getValue()) / 100.0);
    peptideShakerGUI.updateAllAnnotations();
    peptideShakerGUI.setDataSaved(false);
}//GEN-LAST:event_intensitySliderStateChanged

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
private void spectrumAndFragmentIonPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumAndFragmentIonPanelMouseWheelMoved
    if (evt.getWheelRotation() > 0) { // Down
        intensitySlider.setValue(intensitySlider.getValue() - 1);
    } else { // Up
        intensitySlider.setValue(intensitySlider.getValue() + 1);
    }
}//GEN-LAST:event_spectrumAndFragmentIonPanelMouseWheelMoved
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel fragmentIonsJPanel;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel modificationProfileRelatedPeptideJPanel;
    private javax.swing.JPanel modificationProfileSelectedPeptideJPanel;
    private javax.swing.JSplitPane peptideTablesJSplitPane;
    private javax.swing.JTable peptidesTable;
    private javax.swing.JScrollPane peptidesTableJScrollPane;
    private javax.swing.JComboBox primarySelectionJComboBox;
    private javax.swing.JSplitPane psmSpectraSplitPane;
    private javax.swing.JSplitPane psmSplitPane;
    private javax.swing.JScrollPane psmsModifiedTableJScrollPane;
    private javax.swing.JScrollPane psmsRelatedPeptidesTableJScrollPane;
    private javax.swing.JScrollPane ptmJScrollPane;
    private javax.swing.JTable ptmJTable;
    private javax.swing.JPanel relatedPeptidesJPanel;
    private javax.swing.JSplitPane relatedPeptidesJSplitPane;
    private javax.swing.JTable relatedPeptidesTable;
    private javax.swing.JScrollPane relatedPeptidesTableJScrollPane;
    private javax.swing.JTable relatedPsmTable;
    private javax.swing.JComboBox secondarySelectionJComboBox;
    private javax.swing.JPanel selectedPeptidesJPanel;
    private javax.swing.JSplitPane selectedPeptidesJSplitPane;
    private javax.swing.JTable selectedPsmTable;
    private javax.swing.JPanel spectrumAndFragmentIonPanel;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JPanel spectrumChartJPanel;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JTabbedPane spectrumTabbedPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns a list of the keys of the proteins of the currently displayed peptides
     * @return a list of the keys of the proteins of the currently displayed peptides
     */
    public ArrayList<String> getDisplayedProteinMatches() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            for (String peptideKey : getDisplayedPeptides()) {
                ArrayList<String> proteins = identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getParentProteins();
                for (String protein : proteins) {
                    for (String proteinMatchKey : identification.getProteinMap().get(protein)) {
                        if (!result.contains(proteinMatchKey)) {
                            result.add(proteinMatchKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return result;
    }

    /**
     * Returns a list of the keys of the currently displayed peptides
     * @return a list of the keys of the currently displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        ArrayList<String> result = new ArrayList<String>(displayedPeptides);
        result.addAll(relatedPeptides);
        return result;
    }

    /**
     * Returns a list of the psms keys of the currently displayed assumptions
     * @return a list of the psms keys of the currently displayed assumptions
     */
    public ArrayList<String> getDisplayedAssumptions() {
        ArrayList<String> result = new ArrayList<String>();
        for (ArrayList<String> psmList : psmsMap.values()) {
            result.addAll(psmList);
        }
        return result;
    }

    /**
     * Displays or hide sparklines in tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) selectedPsmTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPsmTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            // ignore error
        }

        peptidesTable.revalidate();
        peptidesTable.repaint();

        relatedPeptidesTable.revalidate();
        relatedPeptidesTable.repaint();

        selectedPsmTable.revalidate();
        selectedPsmTable.repaint();

        relatedPsmTable.revalidate();
        relatedPsmTable.repaint();
    }

    /**
     * Creates the peptide map.
     * 
     * @param progressDialog a progress dialog. Can be null.
     */
    private void createPeptideMap(ProgressDialogX progressDialogX) {

        boolean modified;
        ArrayList<String> accountedModifications;

        for (String peptideKey : identification.getPeptideIdentification()) {

            modified = false;
            accountedModifications = new ArrayList<String>();

            for (String modificationName : Peptide.getModificationFamily(peptideKey)) {

                if (!accountedModifications.contains(modificationName)) {

                    if (!peptideMap.containsKey(modificationName)) {
                        peptideMap.put(modificationName, new ArrayList<String>());
                    }

                    peptideMap.get(modificationName).add(peptideKey);
                    modified = true;
                    accountedModifications.add(modificationName);
                }
            }

            if (!modified) {
                if (!peptideMap.containsKey(NO_MODIFICATION)) {
                    peptideMap.put(NO_MODIFICATION, new ArrayList<String>());
                }
                peptideMap.get(NO_MODIFICATION).add(peptideKey);
            }
            if (progressDialogX != null) {
                progressDialogX.incrementValue();
            }
        }
    }

    /**
     * Displays the results.
     * 
     * @param progressDialog a progress dialog. Can be null.
     */
    public void displayResults(ProgressDialogX progressDialog) {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        this.identification = peptideShakerGUI.getIdentification();
        createPeptideMap(progressDialog);
        String[] modifications = new String[peptideMap.size()];
        int cpt = 0;

        for (String modification : peptideMap.keySet()) {
            modifications[cpt] = modification;
            cpt++;
        }

        Arrays.sort(modifications);

        while (ptmJTable.getRowCount() > 0) {
            ((DefaultTableModel) ptmJTable.getModel()).removeRow(0);
        }

        for (int i = 0; i < modifications.length; i++) {

            if (!modifications[i].equalsIgnoreCase("no modification")) {
                ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{
                            (i + 1),
                            i,
                            modifications[i]
                        });

                Color ptmColor = Color.lightGray;

                if (ptmColorMap.containsKey(i)) {
                    ptmColor = ptmColorMap.get(i);
                }

                ptmColors.put(modifications[i], ptmColor);
            }
        }

        ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{
                    (ptmJTable.getRowCount() + 2),
                    100,
                    "no modification",});

        ptmColors.put("no modification", Color.lightGray);

        if (ptmJTable.getRowCount() > 0) {
            ptmJTable.setRowSelectionInterval(0, 0);
            ptmJTable.scrollRectToVisible(ptmJTable.getCellRect(0, 0, false));
            updatePeptideTable();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Updates the peptide table.
     */
    private void updatePeptideTable() {
        try {
            HashMap<Double, ArrayList<String>> scoreToPeptideMap = new HashMap<Double, ArrayList<String>>();
            PeptideMatch peptideMatch;
            PSParameter probabilities = new PSParameter();
            double p;

            for (String peptideKey : peptideMap.get((String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), 2))) {

                peptideMatch = identification.getPeptideMatch(peptideKey);

                if (!peptideMatch.isDecoy()) {

                    probabilities = (PSParameter) identification.getMatchParameter(peptideKey, probabilities);
                    p = probabilities.getPeptideProbability();

                    if (!scoreToPeptideMap.containsKey(p)) {
                        scoreToPeptideMap.put(p, new ArrayList<String>());
                    }

                    scoreToPeptideMap.get(p).add(peptideKey);
                }
            }

            ArrayList<Double> scores = new ArrayList<Double>(scoreToPeptideMap.keySet());
            Collections.sort(scores);
            displayedPeptides = new ArrayList<String>();

            for (double score : scores) {
                displayedPeptides.addAll(scoreToPeptideMap.get(score));
            }

            peptidesTable.revalidate();
            peptidesTable.repaint();

            if (peptidesTable.getRowCount() > 0) {
                peptidesTable.setRowSelectionInterval(0, 0);
                peptidesTable.scrollRectToVisible(peptidesTable.getCellRect(0, 0, false));

                try {
                    peptideMatch = identification.getPeptideMatch(displayedPeptides.get(peptidesTable.getSelectedRow()));
                    updateModificationProfile(peptideMatch, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                updateRelatedPeptidesTable();
                updatePeptideFamilies();
                updateRelatedPsmTable();
            } else {
                modificationProfileSelectedPeptideJPanel.removeAll();
                modificationProfileSelectedPeptideJPanel.revalidate();
                modificationProfileSelectedPeptideJPanel.repaint();

                modificationProfileRelatedPeptideJPanel.removeAll();
                modificationProfileRelatedPeptideJPanel.revalidate();
                modificationProfileRelatedPeptideJPanel.repaint();

                relatedPeptides = new ArrayList<String>();
                relatedPeptidesTable.revalidate();
                relatedPeptidesTable.repaint();
            }

            updateSelectedPsmTable();

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Updates the related peptides table.
     */
    private void updateRelatedPeptidesTable() {

        HashMap<Double, ArrayList<String>> scoreToKeyMap = new HashMap<Double, ArrayList<String>>();
        String peptideKey = displayedPeptides.get(peptidesTable.getSelectedRow());
        String currentSequence, referenceSequence = Peptide.getSequence(peptideKey);
        PSParameter probabilities = new PSParameter();
        double p;

        for (String newKey : identification.getPeptideIdentification()) {

            currentSequence = Peptide.getSequence(newKey);

            if (currentSequence.contains(referenceSequence) || referenceSequence.contains(currentSequence)) {

                if (!newKey.equals(peptideKey)) {

                    probabilities = (PSParameter) identification.getMatchParameter(newKey, probabilities);
                    p = probabilities.getPeptideProbability();

                    if (!scoreToKeyMap.containsKey(p)) {
                        scoreToKeyMap.put(p, new ArrayList<String>());
                    }

                    scoreToKeyMap.get(p).add(newKey);
                }
            }
        }

        relatedPeptides = new ArrayList<String>();
        ArrayList<Double> scores = new ArrayList<Double>(scoreToKeyMap.keySet());
        Collections.sort(scores);

        for (Double score : scores) {
            relatedPeptides.addAll(scoreToKeyMap.get(score));
        }

        if (relatedPeptides.size() > 0) {
            relatedPeptidesTable.setRowSelectionInterval(0, 0);
            relatedPeptidesTable.scrollRectToVisible(relatedPeptidesTable.getCellRect(0, 0, false));

            try {
                PeptideMatch peptideMatch = identification.getPeptideMatch(relatedPeptides.get(relatedPeptidesTable.getSelectedRow()));
                updateModificationProfile(peptideMatch, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            modificationProfileRelatedPeptideJPanel.removeAll();
        }

        relatedPeptidesTable.revalidate();
        relatedPeptidesTable.repaint();
    }

    /**
     * Updates the tables containing the peptide families
     */
    private void updatePeptideFamilies() {
        try {
            psmsMap = new HashMap<String, ArrayList<String>>();
            confidenceMap = new HashMap<String, Double>();
            String peptideKey = displayedPeptides.get(peptidesTable.getSelectedRow());
            String sequence = Peptide.getSequence(peptideKey);
            String familyKey, mainKey;
            PSParameter probabilities = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            SpectrumMatch spectrumMatch;
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                mainKey = getModificationFamily(peptideMatch.getTheoreticPeptide());
                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                    if (peptideAssumption.getPeptide().getSequence().equals(sequence)) {
                        familyKey = getModificationFamily(peptideAssumption.getPeptide());
                        if (!psmsMap.containsKey(familyKey)) {
                            psmsMap.put(familyKey, new ArrayList<String>());
                        }
                        if (!psmsMap.get(familyKey).contains(spectrumMatch.getKey())) {
                            psmsMap.get(familyKey).add(spectrumMatch.getKey());
                        }
                        if (familyKey.equals(mainKey)) {
                            // confidence does not take the PTM position into account at this stage of the development!
                            if (!confidenceMap.containsKey(mainKey)) {
                                confidenceMap.put(mainKey, probabilities.getPeptideConfidence());
                            }
                        } else {
                            // confidence not known at this stage of the development
                            confidenceMap.put(familyKey, 0.0);
                        }
                    }
                }
            }

            if (!relatedPeptides.isEmpty()) {
                peptideMatch = identification.getPeptideMatch(relatedPeptides.get(relatedPeptidesTable.getSelectedRow()));
                psmsMap.put("Related Peptide", new ArrayList<String>(peptideMatch.getSpectrumMatches()));
            }

            ArrayList<String> keys = new ArrayList<String>(confidenceMap.keySet()); // Maybe we want it sorted by confidence? Not sure...
            Collections.sort(keys);

            Vector<String> primarySelections = new Vector<String>();
            Vector<String> secondarySelections = new Vector<String>();

            if (!relatedPeptides.isEmpty()) {
                secondarySelections.add("Related Peptide");
            }

            for (String key : keys) {

                String temp = key;

                if (key.equalsIgnoreCase("")) {
                    temp = "Unmodified";
                }

                primarySelections.add(temp + " | " + confidenceMap.get(key) + "%");
                secondarySelections.add(temp + " | " + confidenceMap.get(key) + "%");
            }

            primarySelectionJComboBox.setModel(new DefaultComboBoxModel(primarySelections));
            secondarySelectionJComboBox.setModel(new DefaultComboBoxModel(secondarySelections));
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Returns the modification family key of a given peptide
     * @param peptide   the inspected peptide
     * @return  the modification family key
     */
    private String getModificationFamily(Peptide peptide) {
        HashMap<String, ArrayList<Integer>> modificationStatus = new HashMap<String, ArrayList<Integer>>();
        String name;
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                name = modificationMatch.getTheoreticPtm().getName();
                if (!modificationStatus.containsKey(name)) {
                    modificationStatus.put(name, new ArrayList<Integer>());
                }
                modificationStatus.get(name).add(modificationMatch.getModificationSite());
            }
        }
        ArrayList<String> names = new ArrayList<String>(modificationStatus.keySet());
        Collections.sort(names);
        boolean firstPos;
        String result = "";
        ArrayList<Integer> positions;
        for (String key : names) {
            positions = modificationStatus.get(key);
            Collections.sort(positions);
            result += key + " (";
            firstPos = true;
            for (int pos : positions) {
                if (!firstPos) {
                    result += ", ";
                } else {
                    firstPos = false;
                }
                result += pos + "";
            }
            result += ") ";
        }
        return result;
    }

    /**
     * Update the modified peptides PSM table.
     */
    private void updateSelectedPsmTable() {

        selectedPsmTable.revalidate();
        selectedPsmTable.repaint();

        if (selectedPsmTable.getRowCount() > 0) {
            selectedPsmTable.setRowSelectionInterval(0, 0);
            selectedPsmTable.scrollRectToVisible(selectedPsmTable.getCellRect(0, 0, false));
            updateSpectra();
        }
    }

    public void updateAnnotations() {
    }

    /**
     * Update the related peptides PSM table.
     */
    private void updateRelatedPsmTable() {

        relatedPsmTable.revalidate();
        relatedPsmTable.repaint();

        if (!relatedPeptides.isEmpty() || secondarySelectionJComboBox.getItemCount() > 1) {
            relatedPsmTable.setRowSelectionInterval(0, 0);
            relatedPsmTable.scrollRectToVisible(relatedPsmTable.getCellRect(0, 0, false));
            updateSpectra();
        }
    }

    /**
     * Update the spectra according to the currently selected psms.
     */
    public void updateSpectra() {
        try {
            linkedSpectrumPanels = new HashMap<Integer, SpectrumPanel>();
            spectrumChartJPanel.removeAll();
            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();

            AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

            if (selectedPsmTable.getSelectedRow() != -1 && primarySelectionJComboBox.getSelectedIndex() != -1) {
                String familyKey = convertComboBoxSelectionToFamilyKey((String) primarySelectionJComboBox.getSelectedItem());
                String spectrumKey = psmsMap.get(familyKey).get(selectedPsmTable.getSelectedRow());
                peptideShakerGUI.selectSpectrum(spectrumKey);

                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                    Precursor precursor = currentSpectrum.getPrecursor();
                    spectrumA = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                            precursor.getMz(), precursor.getCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrumA.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getMzTolerance());
                    spectrumA.setBorder(null);

                    // get the spectrum annotations
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    Peptide currentPeptide = spectrumMatch.getBestAssumption().getPeptide();
                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                        if (getModificationFamily(peptideAssumption.getPeptide()).equals(familyKey)) {
                            currentPeptide = peptideAssumption.getPeptide();
                            break;
                        }
                    }

                    annotationPreferences.setCurrentSettings(currentPeptide, currentSpectrum.getPrecursor().getCharge().value);
                    ArrayList<IonMatch> annotations = annotatorA.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(annotationPreferences.shallAnnotateMostIntensePeaks(), annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getMzTolerance());

                    // add the spectrum annotations
                    spectrumA.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                    spectrumA.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());

                    linkedSpectrumPanels.put(new Integer(0), spectrumA);

                    spectrumA.addSpectrumPanelListener(new SpectrumPanelListener() {

                        public void rescaled(RescalingEvent rescalingEvent) {
                            SpectrumPanel source = (SpectrumPanel) rescalingEvent.getSource();
                            double minMass = rescalingEvent.getMinMass();
                            double maxMass = rescalingEvent.getMaxMass();

                            Iterator<Integer> iterator = linkedSpectrumPanels.keySet().iterator();

                            while (iterator.hasNext()) {
                                SpectrumPanel currentSpectrumPanel = linkedSpectrumPanels.get(iterator.next());
                                if (currentSpectrumPanel != source) {
                                    currentSpectrumPanel.rescale(minMass, maxMass, false);
                                    currentSpectrumPanel.repaint();
                                }
                            }
                        }
                    });

                    spectrumChartJPanel.add(spectrumA);
                }
            }

            if (relatedPsmTable.getSelectedRow() != -1 && secondarySelectionJComboBox.getSelectedIndex() != -1) {
                String familyKey = convertComboBoxSelectionToFamilyKey((String) secondarySelectionJComboBox.getSelectedItem());
                String spectrumKey = psmsMap.get(familyKey).get(relatedPsmTable.getSelectedRow());
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                    Precursor precursor = currentSpectrum.getPrecursor();
                    spectrumB = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                            precursor.getMz(), precursor.getCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrumB.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getMzTolerance());
                    spectrumB.setBorder(null);

                    // get the spectrum annotations                
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    Peptide currentPeptide = spectrumMatch.getBestAssumption().getPeptide();
                    if (!familyKey.equals("Related Peptide")) {
                        for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                            if (getModificationFamily(peptideAssumption.getPeptide()).equals(familyKey)) {
                                currentPeptide = peptideAssumption.getPeptide();
                                break;
                            }
                        }
                    }
                    annotationPreferences.setCurrentSettings(currentPeptide, currentSpectrum.getPrecursor().getCharge().value);
                    ArrayList<IonMatch> annotations = annotatorB.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(annotationPreferences.shallAnnotateMostIntensePeaks(), annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getMzTolerance());

                    // add the spectrum annotations
                    spectrumB.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                    spectrumB.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());

                    linkedSpectrumPanels.put(new Integer(1), spectrumB);

                    spectrumB.addSpectrumPanelListener(new SpectrumPanelListener() {

                        public void rescaled(RescalingEvent rescalingEvent) {
                            SpectrumPanel source = (SpectrumPanel) rescalingEvent.getSource();
                            double minMass = rescalingEvent.getMinMass();
                            double maxMass = rescalingEvent.getMaxMass();

                            Iterator<Integer> iterator = linkedSpectrumPanels.keySet().iterator();

                            while (iterator.hasNext()) {
                                SpectrumPanel currentSpectrumPanel = linkedSpectrumPanels.get(iterator.next());
                                if (currentSpectrumPanel != source) {
                                    currentSpectrumPanel.rescale(minMass, maxMass, false);
                                    currentSpectrumPanel.repaint();
                                }
                            }
                        }
                    });

                    // make sure that the two spectra have the same x-axis range
                    if (spectrumA != null) {
                        spectrumA.rescale(0, spectrumB.getMaxXAxisValue());
                    }

                    spectrumChartJPanel.add(spectrumB);
                }
            }

            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Returns the content of a Modification Profile cell for a desired peptide.
     * 
     * @param peptide   The sequence of the peptide
     * @param scores    The PTM scores
     * @return          The modification profile
     */
    private ArrayList<ModificationProfile> getModificationProfile(Peptide peptide, PSPtmScores scores) {

        ArrayList<ModificationProfile> profiles = new ArrayList<ModificationProfile>();

        if (scores != null) {
            for (String ptmName : scores.getScoredPTMs()) {

                ModificationProfile tempProfile = new ModificationProfile(ptmName, new double[peptide.getSequence().length()][2], ptmColors.get(ptmName));

                //result += ptmName + " ";
                PtmScoring locationScoring = scores.getPtmScoring(ptmName);
                String bestLocation = "";
                double bestScore = 0;
                for (String key : locationScoring.getDeltaScorelocations()) {
                    if (locationScoring.getDeltaScore(key) > bestScore) {
                        bestLocation = key;
                        bestScore = locationScoring.getDeltaScore(key);
                    }
                }
                ArrayList<Integer> modificationSites = PtmScoring.getLocations(bestLocation);
                for (int aa = 1; aa <= peptide.getSequence().length(); aa++) {
                    if (modificationSites.contains(aa)) {
                        tempProfile.getProfile()[aa - 1][ModificationProfile.DELTA_SCORE_ROW_INDEX] = bestScore;
                        //tempProfile.getProfile()[aa - 1][ModificationProfile.A_SCORE_ROW_INDEX] = bestScore * 0.5; // @TODO: insert the real a-score here!!!
                    }
                }

                profiles.add(tempProfile);
            }
        }

        return profiles;
    }

    /**
     * Table model for the peptide table.
     */
    private class PeptideTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return displayedPeptides.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {

            if (column == 0) {
                return " ";
            } else if (column == 1) {
                return "Protein(s)";
            } else if (column == 2) {
                return "Sequence";
            } else if (column == 3) {
                return "Score";
            } else if (column == 4) {
                return "Confidence";
            } else if (column == 5) {
                return "  ";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                if (column == 0) {
                    return new Double(row + 1);
                } else if (column == 1) {
                    String result = peptideShakerGUI.addDatabaseLinks(
                            identification.getPeptideMatch(displayedPeptides.get(row)).getTheoreticPeptide().getParentProteins());
                    return result;
                } else if (column == 2) {
                    return identification.getPeptideMatch(displayedPeptides.get(row)).getTheoreticPeptide().getSequence();
                } else if (column == 3) {
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getMatchParameter(displayedPeptides.get(row), probabilities);
                    return probabilities.getPeptideScore();
                } else if (column == 4) {
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getMatchParameter(displayedPeptides.get(row), probabilities);
                    return probabilities.getPeptideConfidence();
                } else if (column == 5) {
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getMatchParameter(displayedPeptides.get(row), probabilities);
                    return probabilities.isValidated();
                } else {
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
     * Table model for the related peptides table.
     */
    private class RelatedPeptidesTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return relatedPeptides.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return " ";
            } else if (column == 1) {
                return "Protein(s)";
            } else if (column == 2) {
                return "Sequence";
            } else if (column == 3) {
                return "Score";
            } else if (column == 4) {
                return "Confidence";
            } else if (column == 5) {
                return "  ";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                if (column == 0) {
                    return new Double(row + 1);
                } else if (column == 1) {
                    String result = peptideShakerGUI.addDatabaseLinks(
                            identification.getPeptideMatch(relatedPeptides.get(row)).getTheoreticPeptide().getParentProteins());
                    return result;
                } else if (column == 2) {
                    return identification.getPeptideMatch(relatedPeptides.get(row)).getTheoreticPeptide().getSequence();
                } else if (column == 3) {
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getMatchParameter(relatedPeptides.get(row), probabilities);
                    return probabilities.getPeptideScore();
                } else if (column == 4) {
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getMatchParameter(relatedPeptides.get(row), probabilities);
                    return probabilities.getPeptideConfidence();
                } else if (column == 5) {
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getMatchParameter(relatedPeptides.get(row), probabilities);
                    return probabilities.isValidated();
                } else {
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
     * Table model for the selected peptide PSMs table
     */
    private class SelectedPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (primarySelectionJComboBox.getItemCount() == 0) {
                return 0;
            }
            String familyKey = convertComboBoxSelectionToFamilyKey((String) primarySelectionJComboBox.getSelectedItem());
            return psmsMap.get(familyKey).size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Rank";
            } else if (column == 1) {
                return "Sequence";
            } else if (column == 2) {
                return "Modification Profile";
            } else if (column == 3) {
                return "m/z";
            } else if (column == 4) {
                return "Charge";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            String familyKey = convertComboBoxSelectionToFamilyKey((String) primarySelectionJComboBox.getSelectedItem());
            try {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmsMap.get(familyKey).get(row));
                if (column == 0) {
                    String result = "";
                    int cpt;
                    ArrayList<Double> eValues;
                    ArrayList<Integer> searchEngines = new ArrayList<Integer>(spectrumMatch.getAdvocates());
                    Collections.sort(searchEngines);
                    for (int seKey : searchEngines) {
                        cpt = 1;
                        eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(seKey).keySet());
                        Collections.sort(eValues);
                        boolean found = false;
                        for (double eValue : eValues) {
                            for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(seKey).get(eValue)) {
                                if (getModificationFamily(peptideAssumption.getPeptide()).equals(familyKey)) {
                                    if (seKey == Advocate.MASCOT) {
                                        result += "M" + cpt + " ";
                                    } else if (seKey == Advocate.OMSSA) {
                                        result += "O" + cpt + " ";
                                    } else if (seKey == Advocate.XTANDEM) {
                                        result += "X" + cpt + " ";
                                    }
                                    found = true;
                                    break;
                                }
                                cpt++;
                            }
                            if (found == true) {
                                break;
                            }
                        }
                    }
                    return result;
                } else if (column == 1) {
                    // Not sure that it will always be the best assumption, might have to iterate assumptions if the wrong sequence is displayed
                    return spectrumMatch.getBestAssumption().getPeptide().getSequence();
                } else if (column == 2) {
                    PSPtmScores scores = new PSPtmScores();
                    scores = (PSPtmScores) spectrumMatch.getUrParam(scores);
                    return getModificationProfile(spectrumMatch.getBestAssumption().getPeptide(), scores);
                } else if (column == 3) {
                    try {
                        return peptideShakerGUI.getPrecursor(spectrumMatch.getKey()).getMz();
                    } catch (Exception e) {
                        return "";
                    }
                } else if (column == 4) {
                    try {
                        return peptideShakerGUI.getPrecursor(spectrumMatch.getKey()).getCharge().value;
                    } catch (Exception e) {
                        return "";
                    }
                } else {
                    return psmsMap.get(familyKey).get(row);
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
     * Table model for the related peptide PSMs table.
     */
    private class RelatedPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (secondarySelectionJComboBox.getItemCount() == 0) {
                return 0;
            }
            String familyKey = convertComboBoxSelectionToFamilyKey((String) secondarySelectionJComboBox.getSelectedItem());
            return psmsMap.get(familyKey).size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                if (secondarySelectionJComboBox.getItemCount() > 0
                        && ((String) secondarySelectionJComboBox.getSelectedItem()).equals("Related Peptide")) {
                    return " ";
                }
                return "Rank";
            } else if (column == 1) {
                return "Sequence";
            } else if (column == 2) {
                return "Modification(s)";
            } else if (column == 3) {
                return "m/z";
            } else if (column == 4) {
                return "Charge";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String familyKey = convertComboBoxSelectionToFamilyKey((String) secondarySelectionJComboBox.getSelectedItem());
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmsMap.get(familyKey).get(row));
                if (column == 0) {
                    if (familyKey.equals("Related Peptide")) {
                        return row + 1;
                    } else {
                        String result = "";
                        int cpt;
                        ArrayList<Double> eValues;
                        ArrayList<Integer> searchEngines = new ArrayList<Integer>(spectrumMatch.getAdvocates());
                        Collections.sort(searchEngines);
                        for (int seKey : searchEngines) {
                            cpt = 1;
                            eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(seKey).keySet());
                            Collections.sort(eValues);
                            boolean found = false;
                            for (double eValue : eValues) {
                                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(seKey).get(eValue)) {
                                    if (getModificationFamily(peptideAssumption.getPeptide()).equals(familyKey)) {
                                        if (seKey == Advocate.MASCOT) {
                                            result += "M" + cpt + " ";
                                        } else if (seKey == Advocate.OMSSA) {
                                            result += "O" + cpt + " ";
                                        } else if (seKey == Advocate.XTANDEM) {
                                            result += "X" + cpt + " ";
                                        }
                                        found = true;
                                        break;
                                    }
                                    cpt++;
                                }
                                if (found == true) {
                                    break;
                                }
                            }
                        }
                        return result;
                    }
                } else if (column == 1) {
                    // Not sure that it will wlways be the best assumption, might have to iterate assumptions if the wrong sequence is displayed
                    return spectrumMatch.getBestAssumption().getPeptide().getSequence();
                } else if (column == 2) {
                    if (familyKey.equals("Related Peptide")) {
                        return getModificationFamily(spectrumMatch.getBestAssumption().getPeptide());
                    }
                    return familyKey;
                } else if (column == 3) {
                    try {
                        return peptideShakerGUI.getPrecursor(spectrumMatch.getKey()).getMz();
                    } catch (Exception e) {
                        return "";
                    }
                } else if (column == 4) {
                    try {
                        return peptideShakerGUI.getPrecursor(spectrumMatch.getKey()).getCharge().value;
                    } catch (Exception e) {
                        return "";
                    }
                } else {
                    return psmsMap.get(familyKey).get(row);
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
     * Converts the item in the comb box to the correct family key.
     * 
     * @param itemText  the item text
     * @return          the family key
     */
    private String convertComboBoxSelectionToFamilyKey(String itemText) {

        String familyKey = itemText;

        if (familyKey.lastIndexOf("|") != -1) {
            familyKey = familyKey.substring(0, familyKey.lastIndexOf("|"));
        }

        if (!familyKey.equalsIgnoreCase("Related Peptide")) {
            familyKey = familyKey.substring(0, familyKey.length() - 1);
        }

        if (familyKey.equalsIgnoreCase("Unmodified")) {
            familyKey = "";
        }

        return familyKey;
    }

    /**
     * Returns the spectrum panel.
     * 
     * @return the spectrum panel
     */
    public Component getSpectrum() {
        return (Component) spectrumJPanel.getComponent(1);
    }
    
    /**
     * Returns the current spectrum as an mgf string.
     * 
     * @return the current spectrum as an mgf string
     */
    public String getSpectrumAsMgf() {
        
        String spectrumAsMgf = "";
        
        if (selectedPsmTable.getSelectedRow() != -1 && primarySelectionJComboBox.getSelectedIndex() != -1) {
            String familyKey = convertComboBoxSelectionToFamilyKey((String) primarySelectionJComboBox.getSelectedItem());
            String spectrumKey = psmsMap.get(familyKey).get(selectedPsmTable.getSelectedRow());
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
            spectrumAsMgf += currentSpectrum.asMgf();
        }
        
        if (relatedPsmTable.getSelectedRow() != -1 && secondarySelectionJComboBox.getSelectedIndex() != -1) {
            String familyKey = convertComboBoxSelectionToFamilyKey((String) secondarySelectionJComboBox.getSelectedItem());
            String spectrumKey = psmsMap.get(familyKey).get(relatedPsmTable.getSelectedRow());
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
            spectrumAsMgf += currentSpectrum.asMgf();
        }
        
        if (!spectrumAsMgf.isEmpty()) {
            return spectrumAsMgf;
        }
                
        return null;
    }

    /**
     * Hides or displays the score columns in the protein and peptide tables.
     * 
     * @param hide if true the score columns are hidden.
     */
    public void hideScores(boolean hide) {

        try {
            if (hide) {
                peptidesTable.removeColumn(peptidesTable.getColumn("Score"));
                relatedPeptidesTable.removeColumn(relatedPeptidesTable.getColumn("Score"));
            } else {
                peptidesTable.addColumn(selectedPeptidesScoreColumn);
                peptidesTable.moveColumn(5, 3);

                relatedPeptidesTable.addColumn(relatedPeptidesScoreColumn);
                relatedPeptidesTable.moveColumn(5, 3);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the modification profile to the currently selected peptide.
     * 
     * @param peptideMatch              the peptide match to create the profile for
     * @param selectedPeptideProfile    if true the selected peptide profile is updated, otherwise the releated peptide profile is updated   
     */
    private void updateModificationProfile(PeptideMatch peptideMatch, boolean selectedPeptideProfile) {

        try {
            PSPtmScores scores = new PSPtmScores();
            scores = (PSPtmScores) peptideMatch.getUrParam(scores);
            ArrayList<ModificationProfile> profiles = getModificationProfile(peptideMatch.getTheoreticPeptide(), scores);

            SequenceModificationPanel sequenceModificationPanel =
                    new SequenceModificationPanel(peptideMatch.getTheoreticPeptide().getNTerminal() + "-"
                    + peptideMatch.getTheoreticPeptide().getSequence()
                    + "-" + peptideMatch.getTheoreticPeptide().getCTerminal(),
                    profiles, true);

            if (selectedPeptideProfile) {
                modificationProfileSelectedPeptideJPanel.removeAll();
                sequenceModificationPanel.setOpaque(false);
                sequenceModificationPanel.setMinimumSize(new Dimension(sequenceModificationPanel.getPreferredSize().width, sequenceModificationPanel.getHeight()));
                JPanel tempPanel = new JPanel();
                tempPanel.setOpaque(false);
                modificationProfileSelectedPeptideJPanel.add(tempPanel);
                modificationProfileSelectedPeptideJPanel.add(sequenceModificationPanel);
                modificationProfileSelectedPeptideJPanel.revalidate();
                modificationProfileSelectedPeptideJPanel.repaint();
            } else {
                modificationProfileRelatedPeptideJPanel.removeAll();
                sequenceModificationPanel.setOpaque(false);
                sequenceModificationPanel.setMinimumSize(new Dimension(sequenceModificationPanel.getPreferredSize().width, sequenceModificationPanel.getHeight()));
                JPanel tempPanel = new JPanel();
                tempPanel.setOpaque(false);
                modificationProfileRelatedPeptideJPanel.add(tempPanel);
                modificationProfileRelatedPeptideJPanel.add(sequenceModificationPanel);
                modificationProfileRelatedPeptideJPanel.revalidate();
                modificationProfileRelatedPeptideJPanel.repaint();
            }

            double selectedPeptideProfileWidth = 1 - (sequenceModificationPanel.getPreferredSize().getWidth() / selectedPeptidesJSplitPane.getSize().getWidth());
            double relatedPeptideProfileWidth = 1 - (sequenceModificationPanel.getPreferredSize().getWidth() / relatedPeptidesJSplitPane.getSize().getWidth());

            double splitterLocation = Math.min(selectedPeptideProfileWidth, relatedPeptideProfileWidth);

            selectedPeptidesJSplitPane.setDividerLocation(splitterLocation);
            selectedPeptidesJSplitPane.revalidate();
            selectedPeptidesJSplitPane.repaint();

            relatedPeptidesJSplitPane.setDividerLocation(splitterLocation);
            relatedPeptidesJSplitPane.revalidate();
            relatedPeptidesJSplitPane.repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes sure that the annotation menu bar is visible.
     */
    public void showSpectrumAnnotationMenu() {
        spectrumAnnotationMenuPanel.removeAll();
        spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
        peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false);
    }

    /**
     * Set the intensity slider value.
     * 
     * @param value the intensity slider value
     */
    public void setIntensitySliderValue(int value) {
        intensitySlider.setValue(value);
    }
}
