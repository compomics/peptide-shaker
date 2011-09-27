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
import javax.swing.JColorChooser;
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
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;
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
     * The currently selected row in the PTM table.
     */
    private int currentPtmRow = -1;
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
     * The spectrum annotator for the first spectrum
     */
    private SpectrumAnnotator annotator = new SpectrumAnnotator();
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
     * boolean indicating whether the related peptide is selected
     */
    private boolean relatedSelected = false;
    /**
     * A map of psm family confidence
     */
    private HashMap<String, Double> confidenceMap = new HashMap<String, Double>();
    /**
     * The current spectrum panel for the upper psm.
     */
    private SpectrumPanel spectrum;

    /**
     * Creates a new PTM tab.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public PtmPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        selectedPeptidesScoreColumn = peptidesTable.getColumn("Peptide Score");
        relatedPeptidesScoreColumn = relatedPeptidesTable.getColumn("Peptide Score");

        relatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsModifiedTableJScrollPane.getViewport().setOpaque(false);
        peptidesTableJScrollPane.getViewport().setOpaque(false);
        ptmJScrollPane.getViewport().setOpaque(false);

        spectrumTabbedPane.setEnabledAt(0, false);

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
        peptidesTable.getColumn("PI").setMaxWidth(35);
        relatedPeptidesTable.getColumn("PI").setMaxWidth(35);

        peptidesTable.getColumn("Peptide Confidence").setMaxWidth(90);
        peptidesTable.getColumn("Peptide Confidence").setMinWidth(90);
        relatedPeptidesTable.getColumn("Peptide Confidence").setMaxWidth(90);
        relatedPeptidesTable.getColumn("Peptide Confidence").setMinWidth(90);

        peptidesTable.getTableHeader().setReorderingAllowed(false);
        relatedPeptidesTable.getTableHeader().setReorderingAllowed(false);
        selectedPsmTable.getTableHeader().setReorderingAllowed(false);

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

        peptidesTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), proteinInferenceColorMap, proteinInferenceTooltipMap));
        relatedPeptidesTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), proteinInferenceColorMap, proteinInferenceTooltipMap));

        // set up the PTM confidence color map
        HashMap<Integer, Color> ptmConfidenceColorMap = new HashMap<Integer, Color>();
        ptmConfidenceColorMap.put(PtmScoring.NOT_FOUND, Color.GRAY);
        ptmConfidenceColorMap.put(PtmScoring.RANDOM, Color.RED);
        ptmConfidenceColorMap.put(PtmScoring.DOUBTFUL, Color.ORANGE);
        ptmConfidenceColorMap.put(PtmScoring.CONFIDENT, Color.YELLOW);
        ptmConfidenceColorMap.put(PtmScoring.VERY_CONFIDENT, Color.GREEN);

        // set up the PTM confidence tooltip map
        HashMap<Integer, String> ptmConfidenceTooltipMap = new HashMap<Integer, String>();
        ptmConfidenceTooltipMap.put(PtmScoring.RANDOM, "Random assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.DOUBTFUL, "Doubtful assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.CONFIDENT, "Confident assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.VERY_CONFIDENT, "Very confident assignment");

        peptidesTable.getColumn("PTM confidence").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        relatedPeptidesTable.getColumn("PTM confidence").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        selectedPsmTable.getColumn("PTM confidence").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), ptmConfidenceColorMap, ptmConfidenceTooltipMap));

        peptidesTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        peptidesTable.getColumn("Peptide Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Peptide Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth(), peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        peptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        relatedPeptidesTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        relatedPeptidesTable.getColumn("Peptide Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Peptide Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth(), peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        relatedPeptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        selectedPsmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) selectedPsmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);

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
        ptmJTable.getColumn("MC").setCellRenderer(new JSparklinesColorTableCellRenderer());

        // set up the table header tooltips
        selectedPeptidesTableToolTips = new ArrayList<String>();
        selectedPeptidesTableToolTips.add(null);
        selectedPeptidesTableToolTips.add("Mapping Protein(s)");
        selectedPeptidesTableToolTips.add("Protein Inference Class");
        selectedPeptidesTableToolTips.add("Peptide Sequence");
        selectedPeptidesTableToolTips.add("PTM Location Confidence");
        selectedPeptidesTableToolTips.add("Peptide Score");
        selectedPeptidesTableToolTips.add("Peptide Confidence");
        selectedPeptidesTableToolTips.add("Validated");

        relatedPeptidesTableToolTips = new ArrayList<String>();
        relatedPeptidesTableToolTips.add(null);
        relatedPeptidesTableToolTips.add("Mapping Protein(s)");
        relatedPeptidesTableToolTips.add("Protein Inference Class");
        relatedPeptidesTableToolTips.add("Peptide Sequence");
        relatedPeptidesTableToolTips.add("PTM Location Confidence");
        relatedPeptidesTableToolTips.add("Peptide Score");
        relatedPeptidesTableToolTips.add("Peptide Confidence");
        selectedPeptidesTableToolTips.add("Validated");

        selectedPsmsTableToolTips = new ArrayList<String>();
        selectedPsmsTableToolTips.add(null);
        selectedPsmsTableToolTips.add("Peptide Sequence");
        selectedPsmsTableToolTips.add("PTM Location Confidence");
        selectedPsmsTableToolTips.add("Precursor Charge");
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
        modificationProfilePsmPanel = new javax.swing.JPanel();
        spectrumAndFragmentIonPanel = new javax.swing.JPanel();
        spectrumTabbedPane = new javax.swing.JTabbedPane();
        fragmentIonsJPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumChartJPanel = new javax.swing.JPanel();
        intensitySlider = new javax.swing.JSlider();
        accuracySlider = new javax.swing.JSlider();

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
                java.lang.Integer.class, java.lang.Object.class, java.lang.String.class
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
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ptmJTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                ptmJTableMouseReleased(evt);
            }
        });
        ptmJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                ptmJTableMouseMoved(evt);
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
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

        psmSplitPane.setBorder(javax.swing.BorderFactory.createTitledBorder("PSM selection"));
        psmSplitPane.setDividerLocation(175);
        psmSplitPane.setDividerSize(0);
        psmSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        psmSplitPane.setResizeWeight(0.5);
        psmSplitPane.setOpaque(false);

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
        selectedPsmTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                selectedPsmTableMouseMoved(evt);
            }
        });
        selectedPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                selectedPsmTableKeyReleased(evt);
            }
        });
        psmsModifiedTableJScrollPane.setViewportView(selectedPsmTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setLeftComponent(jPanel7);

        modificationProfilePsmPanel.setOpaque(false);

        javax.swing.GroupLayout modificationProfilePsmPanelLayout = new javax.swing.GroupLayout(modificationProfilePsmPanel);
        modificationProfilePsmPanel.setLayout(modificationProfilePsmPanelLayout);
        modificationProfilePsmPanelLayout.setHorizontalGroup(
            modificationProfilePsmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 488, Short.MAX_VALUE)
        );
        modificationProfilePsmPanelLayout.setVerticalGroup(
            modificationProfilePsmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 263, Short.MAX_VALUE)
        );

        psmSplitPane.setRightComponent(modificationProfilePsmPanel);

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
            .addGap(0, 514, Short.MAX_VALUE)
        );
        fragmentIonsJPanelLayout.setVerticalGroup(
            fragmentIonsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 368, Short.MAX_VALUE)
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
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumTabbedPane.setSelectedIndex(1);

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

        javax.swing.GroupLayout spectrumAndFragmentIonPanelLayout = new javax.swing.GroupLayout(spectrumAndFragmentIonPanel);
        spectrumAndFragmentIonPanel.setLayout(spectrumAndFragmentIonPanelLayout);
        spectrumAndFragmentIonPanelLayout.setHorizontalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accuracySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        spectrumAndFragmentIonPanelLayout.setVerticalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                        .addGap(59, 59, 59))
                    .addGroup(spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                        .addComponent(spectrumTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
                        .addContainerGap())))
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
     * Update the related peptides and modified peptide psms tables.
     *
     * @param evt
     */
    private void peptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseReleased

        relatedSelected = false;
        updateRelatedPeptidesTable();
        updateSelectedPsmTable();

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
        relatedSelected = true;
        updateSelectedPsmTable();

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
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link. Or shows a tooltip with modification details is over 
     * the sequence column.
     *
     * @param evt
     */
    private void peptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseMoved
        int row = peptidesTable.rowAtPoint(evt.getPoint());
        int column = peptidesTable.columnAtPoint(evt.getPoint());

        if (peptidesTable.getValueAt(row, column) != null) {
            if (column == peptidesTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) peptidesTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                peptidesTable.setToolTipText(null);

            } else if (column == peptidesTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                try {
                    peptidesTable.setToolTipText(
                            peptideShakerGUI.getPeptideModificationTooltipAsHtml(identification.getPeptideMatch(displayedPeptides.get(row)).getTheoreticPeptide()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptidesTable.setToolTipText(null);
            }
        } else {
            peptidesTable.setToolTipText(null);
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
     * HTML link. Or shows a tooltip with modification details is over 
     * the sequence column.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseMoved
        int row = relatedPeptidesTable.rowAtPoint(evt.getPoint());
        int column = relatedPeptidesTable.columnAtPoint(evt.getPoint());

        if (relatedPeptidesTable.getValueAt(row, column) != null) {
            if (column == relatedPeptidesTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) relatedPeptidesTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                relatedPeptidesTable.setToolTipText(null);

            } else if (column == relatedPeptidesTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                try {
                    relatedPeptidesTable.setToolTipText(
                            peptideShakerGUI.getPeptideModificationTooltipAsHtml(identification.getPeptideMatch(relatedPeptides.get(row)).getTheoreticPeptide()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                relatedPeptidesTable.setToolTipText(null);
            }
        } else {
            relatedPeptidesTable.setToolTipText(null);
        }
    }//GEN-LAST:event_relatedPeptidesTableMouseMoved

    /**
     * Update the peptide table or opens a color chooser if the color column is clicked.
     * 
     * @param evt 
     */
private void ptmJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseReleased

    int row = ptmJTable.rowAtPoint(evt.getPoint());
    int column = ptmJTable.columnAtPoint(evt.getPoint());

    if (row != -1 && column == 1) {

        if (row != currentPtmRow) {
            updatePeptideTable();
        }

        Color newColor = JColorChooser.showDialog(this, "Pick a Color", (Color) ptmJTable.getValueAt(row, column));

        if (newColor != null) {

            // update the color in the table
            ptmJTable.setValueAt(newColor, row, column);

            // update the profiles with the new colors
            if (!((String) ptmJTable.getValueAt(row, 2)).equalsIgnoreCase("no modification")) {
                peptideShakerGUI.getSearchParameters().getModificationProfile().setColor(
                        (String) ptmJTable.getValueAt(row, 2), newColor);
                peptideShakerGUI.updatePtmColorCoding();
            }
        }
    } else {
        updatePeptideTable();
    }

    currentPtmRow = row;
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
    peptideShakerGUI.getAnnotationPreferences().setAnnotationLevel(((Integer) intensitySlider.getValue()) / 100.0);
    peptideShakerGUI.updateSpectrumAnnotations();
    peptideShakerGUI.setDataSaved(false);
}//GEN-LAST:event_intensitySliderStateChanged

    /**
     * Updates the slider values when the user scrolls.
     * 
     * @param evt 
     */
private void spectrumAndFragmentIonPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumAndFragmentIonPanelMouseWheelMoved

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
}//GEN-LAST:event_spectrumAndFragmentIonPanelMouseWheelMoved

    /**
     * Changes the cursor to a hand cursor if over the color column.
     * 
     * @param evt 
     */
private void ptmJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseMoved
    int row = ptmJTable.rowAtPoint(evt.getPoint());
    int column = ptmJTable.columnAtPoint(evt.getPoint());

    if (row != -1 && column == 1) {
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    } else {
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }
}//GEN-LAST:event_ptmJTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     * 
     * @param evt 
     */
private void ptmJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseExited
    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ptmJTableMouseExited

    /**
     * See if we ought to show a tooltip with modification details for the 
     * sequenence column.
     * 
     * @param evt 
     */
    private void selectedPsmTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedPsmTableMouseMoved

        int row = selectedPsmTable.rowAtPoint(evt.getPoint());
        int column = selectedPsmTable.columnAtPoint(evt.getPoint());

        if (selectedPsmTable.getValueAt(row, column) != null) {

            if (column == selectedPsmTable.getColumn("Sequence").getModelIndex()) {

                try {
                    String spectrumKey = identification.getPeptideMatch(getSelectedPeptide()).getSpectrumMatches().get(row);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    selectedPsmTable.setToolTipText(
                            peptideShakerGUI.getPeptideModificationTooltipAsHtml(spectrumMatch.getBestAssumption().getPeptide()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                selectedPsmTable.setToolTipText(null);
            }

        } else {
            selectedPsmTable.setToolTipText(null);
        }
    }//GEN-LAST:event_selectedPsmTableMouseMoved

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
        spectrumAndFragmentIonPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JPanel fragmentIonsJPanel;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel modificationProfilePsmPanel;
    private javax.swing.JPanel modificationProfileRelatedPeptideJPanel;
    private javax.swing.JPanel modificationProfileSelectedPeptideJPanel;
    private javax.swing.JSplitPane peptideTablesJSplitPane;
    private javax.swing.JTable peptidesTable;
    private javax.swing.JScrollPane peptidesTableJScrollPane;
    private javax.swing.JSplitPane psmSpectraSplitPane;
    private javax.swing.JSplitPane psmSplitPane;
    private javax.swing.JScrollPane psmsModifiedTableJScrollPane;
    private javax.swing.JScrollPane ptmJScrollPane;
    private javax.swing.JTable ptmJTable;
    private javax.swing.JPanel relatedPeptidesJPanel;
    private javax.swing.JSplitPane relatedPeptidesJSplitPane;
    private javax.swing.JTable relatedPeptidesTable;
    private javax.swing.JScrollPane relatedPeptidesTableJScrollPane;
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
    public ArrayList<String> getDisplayedPsms() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            for (String peptide : displayedPeptides) {
                result.addAll(identification.getPeptideMatch(peptide).getSpectrumMatches());
            }
            for (String peptide : relatedPeptides) {
                result.addAll(identification.getPeptideMatch(peptide).getSpectrumMatches());
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return result;
    }

    /**
     * Displays or hide sparklines in tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Peptide Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Peptide Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) selectedPsmTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Peptide Score").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Peptide Score").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            // ignore error
        }

        peptidesTable.revalidate();
        peptidesTable.repaint();

        relatedPeptidesTable.revalidate();
        relatedPeptidesTable.repaint();

        selectedPsmTable.revalidate();
        selectedPsmTable.repaint();

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
     * Returns the selected PTM name
     * @return the selected PTM name
     */
    public String getSelectedModification() {
        return (String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), 2);
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

            if (!modifications[i].equalsIgnoreCase(NO_MODIFICATION)) {
                ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{
                            (i + 1),
                            peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(modifications[i]),
                            modifications[i]
                        });
            }
        }

        ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{
                    (ptmJTable.getRowCount() + 2),
                    Color.lightGray,
                    NO_MODIFICATION});

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

        // @TODO: replace the waiting cursor with a progress bar dialog?

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

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

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
     * Update the spectra according to the currently selected psms.
     */
    public void updateSpectra() {
        try {

            spectrumChartJPanel.removeAll();
            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();

            AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

            if (selectedPsmTable.getSelectedRow() != -1) {

                String spectrumKey = identification.getPeptideMatch(getSelectedPeptide()).getSpectrumMatches().get(selectedPsmTable.getSelectedRow());
                peptideShakerGUI.selectSpectrum(spectrumKey);

                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                    Precursor precursor = currentSpectrum.getPrecursor();
                    spectrum = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                            precursor.getMz(), precursor.getCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrum.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getFragmentIonAccuracy());
                    spectrum.setBorder(null);

                    // get the spectrum annotations
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    Peptide currentPeptide = spectrumMatch.getBestAssumption().getPeptide();

                    annotationPreferences.setCurrentSettings(currentPeptide,
                            currentSpectrum.getPrecursor().getCharge().value, true);
                    ArrayList<IonMatch> annotations = annotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getFragmentIonAccuracy());

                    // add the spectrum annotations
                    spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                    spectrum.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                    spectrum.setYAxisZoomExcludesBackgroundPeaks(peptideShakerGUI.getAnnotationPreferences().yAxisZoomExcludesBackgroundPeaks());

                    spectrumChartJPanel.add(spectrum);
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

                Color ptmColor = peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(ptmName);
                ModificationProfile tempProfile = new ModificationProfile(ptmName, new double[peptide.getSequence().length()][2], ptmColor);

                PtmScoring locationScoring = scores.getPtmScoring(ptmName);
                // delta score
                String bestLocation = locationScoring.getBestDeltaScoreLocations();
                double bestScore;
                if (bestLocation != null) {
                    bestScore = locationScoring.getDeltaScore(bestLocation);
                    ArrayList<Integer> modificationSites = PtmScoring.getLocations(bestLocation);
                    for (int aa = 1; aa <= peptide.getSequence().length(); aa++) {
                        if (modificationSites.contains(aa)) {
                            tempProfile.getProfile()[aa - 1][ModificationProfile.DELTA_SCORE_ROW_INDEX] = bestScore;
                        }
                    }
                }
                // A-score
                bestLocation = locationScoring.getBestAScoreLocations();
                if (bestLocation != null) {
                    bestScore = locationScoring.getAScore(bestLocation);
                    ArrayList<Integer> modificationSites = PtmScoring.getLocations(bestLocation);
                    for (int aa = 1; aa <= peptide.getSequence().length(); aa++) {
                        if (modificationSites.contains(aa)) {
                            tempProfile.getProfile()[aa - 1][ModificationProfile.A_SCORE_ROW_INDEX] = bestScore;
                        }
                    }
                }
                profiles.add(tempProfile);
            }
        }

        return profiles;
    }

    /**
     * Returns the key of the selected peptide
     */
    private String getSelectedPeptide() {
        if (relatedSelected) {
            return relatedPeptides.get(relatedPeptidesTable.getSelectedRow());
        } else {
            return displayedPeptides.get(peptidesTable.getSelectedRow());
        }
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
            return 8;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Protein(s)";
                case 2:
                    return "PI";
                case 3:
                    return "Sequence";
                case 4:
                    return "PTM confidence";
                case 5:
                    return "Peptide Score";
                case 6:
                    return "Peptide Confidence";
                case 7:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                PSParameter probabilities;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        return peptideShakerGUI.addDatabaseLinks(
                                identification.getPeptideMatch(displayedPeptides.get(row)).getTheoreticPeptide().getParentProteins());
                    case 2:
                        return 0;
                    case 3:
                        return identification.getPeptideMatch(displayedPeptides.get(row)).getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 4:
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getPeptideMatch(displayedPeptides.get(row)).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 5:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.getPeptideScore();
                    case 6:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.getPeptideConfidence();
                    case 7:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.isValidated();
                    default:
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
            return 8;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Protein(s)";
                case 2:
                    return "PI";
                case 3:
                    return "Sequence";
                case 4:
                    return "PTM confidence";
                case 5:
                    return "Peptide Score";
                case 6:
                    return "Peptide Confidence";
                case 7:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                PSParameter probabilities;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        return peptideShakerGUI.addDatabaseLinks(
                                identification.getPeptideMatch(relatedPeptides.get(row)).getTheoreticPeptide().getParentProteins());
                    case 2:
                        return 0;
                    case 3:
                        return identification.getPeptideMatch(relatedPeptides.get(row)).getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 4:
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getPeptideMatch(relatedPeptides.get(row)).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 5:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.getPeptideScore();
                    case 6:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.getPeptideConfidence();
                    case 7:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.isValidated();
                    default:
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
            if (ptmJTable.getSelectedRow() != -1) {
                try {
                    return identification.getPeptideMatch(getSelectedPeptide()).getSpectrumCount();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    return 0;
                }
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) { //@TODO: add scores, confidence and validation?
                case 0:
                    return " ";
                case 1:
                    return "Sequence";
                case 2:
                    return "PTM confidence";
                case 3:
                    return "Charge";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String spectrumKey;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide()).getSpectrumMatches().get(row);
                        return identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 2:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide()).getSpectrumMatches().get(row);
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getSpectrumMatch(spectrumKey).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 3:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide()).getSpectrumMatches().get(row);
                        return peptideShakerGUI.getPrecursor(spectrumKey).getCharge().value;
                    default:
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
        try {
            String spectrumKey = identification.getPeptideMatch(getSelectedPeptide()).getSpectrumMatches().get(selectedPsmTable.getSelectedRow());
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
            spectrumAsMgf += currentSpectrum.asMgf();

            if (!spectrumAsMgf.isEmpty()) {
                return spectrumAsMgf;
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);

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
                peptidesTable.removeColumn(peptidesTable.getColumn("Peptide Score"));
                relatedPeptidesTable.removeColumn(relatedPeptidesTable.getColumn("Peptide Score"));
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

    /**
     * Set the accuracy slider value.
     * 
     * @param value the accuracy slider value
     */
    public void setAccuracySliderValue(int value) {
        accuracySlider.setValue(value);
    }

    /**
     * Update the PTM color coding.
     */
    public void updatePtmColors() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        for (int i = 0; i < ptmJTable.getRowCount(); i++) {
            ptmJTable.setValueAt(peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(
                    (String) ptmJTable.getValueAt(i, 2)), i, 1);
        }

        updateModificationProfiles();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Redraws the modification profiles. For example if the ptm colors are updated.
     */
    public void updateModificationProfiles() {
        try {
            if (peptidesTable.getSelectedRow() != -1) {
                updateModificationProfile(identification.getPeptideMatch(displayedPeptides.get(peptidesTable.getSelectedRow())), true);
            }
            if (relatedPeptidesTable.getSelectedRow() != -1) {
                updateModificationProfile(identification.getPeptideMatch(relatedPeptides.get(relatedPeptidesTable.getSelectedRow())), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
