package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumAnnotator.SpectrumAnnotationMap;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.events.RescalingEvent;
import com.compomics.util.gui.interfaces.SpectrumPanelListener;
import com.compomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.ComponentOrientation;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The PTM tab.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmPanel extends javax.swing.JPanel {

    /**
     * The selected peptides table column header tooltips.
     */
    private ArrayList<String> selectedPeptidesTableToolTips;
    /**
     * The related peptides table column header tooltips.
     */
    private ArrayList<String> relatedPeptidesTableToolTips;
    /**
     * A hashmap of both the linked spectra.
     */
    private HashMap<Integer, SpectrumPanel> linkedSpectrumPanels;
    /**
     * The spectrum annotator
     */
    private SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
    /**
     * The current spectrum annotations.
     */
    private Vector<DefaultSpectrumAnnotation> currentAnnotations;
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
    private final String NO_MODIFICATION = "No modification";
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
     * Creates a new PTM tab.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public PtmPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        relatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsModifiedTableJScrollPane.getViewport().setOpaque(false);
        peptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsRelatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        primarySelectionJScrollPane.getViewport().setOpaque(false);
        secondarySelectionJScrollPane.getViewport().setOpaque(false);
        
        setTableProperties ();

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
    
    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties () {
        
        peptidesTable.getColumn(" ").setMaxWidth(50);
        relatedPeptidesTable.getColumn(" ").setMaxWidth(50);
        selectedPsmTable.getColumn("Rank").setMaxWidth(50);
        relatedPsmTable.getColumn("Rank").setMaxWidth(50);
        
        peptidesTable.getTableHeader().setReorderingAllowed(false);
        relatedPeptidesTable.getTableHeader().setReorderingAllowed(false);
        primarySelectionTable.getTableHeader().setReorderingAllowed(false);
        selectedPsmTable.getTableHeader().setReorderingAllowed(false);
        secondarySelectionTable.getTableHeader().setReorderingAllowed(false);
        relatedPsmTable.getTableHeader().setReorderingAllowed(false);
        
        peptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        peptidesTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        relatedPeptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        relatedPeptidesTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        
        // set up the table header tooltips
        selectedPeptidesTableToolTips = new ArrayList<String>();
        selectedPeptidesTableToolTips.add(null);
        selectedPeptidesTableToolTips.add("Mapping Protein(s)");
        selectedPeptidesTableToolTips.add("Peptide Sequence");
        selectedPeptidesTableToolTips.add("Peptide Modifications");
        selectedPeptidesTableToolTips.add("Peptide Score");
        selectedPeptidesTableToolTips.add("Peptide Confidence");
        
        // set up the table header tooltips
        relatedPeptidesTableToolTips = new ArrayList<String>();
        relatedPeptidesTableToolTips.add(null);
        relatedPeptidesTableToolTips.add("Mapping Protein(s)");
        relatedPeptidesTableToolTips.add("Peptide Sequence");
        relatedPeptidesTableToolTips.add("Peptide Modifications");
        relatedPeptidesTableToolTips.add("Peptide Score");
        relatedPeptidesTableToolTips.add("Peptide Confidence");
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
        jScrollPane1 = new javax.swing.JScrollPane();
        modificationsList = new javax.swing.JList();
        peptideTablesJSplitPane = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
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
        jPanel2 = new javax.swing.JPanel();
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
        psmSpectraSplitPane = new javax.swing.JSplitPane();
        psmSplitPane = new javax.swing.JSplitPane();
        jPanel7 = new javax.swing.JPanel();
        psmsModifiedTableJScrollPane = new javax.swing.JScrollPane();
        selectedPsmTable = new javax.swing.JTable();
        primarySelectionJScrollPane = new javax.swing.JScrollPane();
        primarySelectionTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        psmsRelatedPeptidesTableJScrollPane = new javax.swing.JScrollPane();
        relatedPsmTable = new javax.swing.JTable();
        secondarySelectionJScrollPane = new javax.swing.JScrollPane();
        secondarySelectionTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        spectrumTabbedPane = new javax.swing.JTabbedPane();
        fragmentIonsJPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        aIonToggleButton = new javax.swing.JToggleButton();
        bIonToggleButton = new javax.swing.JToggleButton();
        cIonToggleButton = new javax.swing.JToggleButton();
        xIonToggleButton = new javax.swing.JToggleButton();
        yIonToggleButton = new javax.swing.JToggleButton();
        zIonToggleButton = new javax.swing.JToggleButton();
        h2oToggleButton = new javax.swing.JToggleButton();
        nh3ToggleButton = new javax.swing.JToggleButton();
        otherToggleButton = new javax.swing.JToggleButton();
        oneChargeToggleButton = new javax.swing.JToggleButton();
        twoChargesToggleButton = new javax.swing.JToggleButton();
        moreThanTwoChargesToggleButton = new javax.swing.JToggleButton();
        spectrumChartJPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jPanel8.setOpaque(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Variable Modifications"));
        jPanel1.setOpaque(false);

        modificationsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                modificationsListMouseClicked(evt);
            }
        });
        modificationsList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                modificationsListKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(modificationsList);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setBorder(null);
        peptideTablesJSplitPane.setDividerLocation(170);
        peptideTablesJSplitPane.setDividerSize(0);
        peptideTablesJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptideTablesJSplitPane.setResizeWeight(0.5);
        peptideTablesJSplitPane.setOpaque(false);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Peptides"));
        jPanel4.setOpaque(false);

        peptidesTableJScrollPane.setOpaque(false);

        peptidesTable.setModel(new PeptideTable()
        );
        peptidesTable.setOpaque(false);
        peptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptidesTableMouseClicked(evt);
            }
        });
        peptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidesTableKeyReleased(evt);
            }
        });
        peptidesTableJScrollPane.setViewportView(peptidesTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setLeftComponent(jPanel4);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Related Peptides"));
        jPanel2.setOpaque(false);

        relatedPeptidesTableJScrollPane.setOpaque(false);

        relatedPeptidesTable.setModel(new RelatedPeptidesTable());
        relatedPeptidesTable.setOpaque(false);
        relatedPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        relatedPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseClicked(evt);
            }
        });
        relatedPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPeptidesTableKeyReleased(evt);
            }
        });
        relatedPeptidesTableJScrollPane.setViewportView(relatedPeptidesTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setRightComponent(jPanel2);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideTablesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 838, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(peptideTablesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
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
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectedPsmTableMouseClicked(evt);
            }
        });
        selectedPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                selectedPsmTableKeyReleased(evt);
            }
        });
        psmsModifiedTableJScrollPane.setViewportView(selectedPsmTable);

        primarySelectionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Peptide", "Confidence"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        primarySelectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                primarySelectionTableMouseReleased(evt);
            }
        });
        primarySelectionTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                primarySelectionTableKeyReleased(evt);
            }
        });
        primarySelectionJScrollPane.setViewportView(primarySelectionTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(primarySelectionJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(primarySelectionJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
                    .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE))
                .addContainerGap())
        );

        psmSplitPane.setLeftComponent(jPanel7);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("PSMs - Secondary Selection"));
        jPanel3.setOpaque(false);

        psmsRelatedPeptidesTableJScrollPane.setOpaque(false);

        relatedPsmTable.setModel(new RelatedPsmsTable());
        relatedPsmTable.setOpaque(false);
        relatedPsmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                relatedPsmTableMouseClicked(evt);
            }
        });
        relatedPsmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPsmTableKeyReleased(evt);
            }
        });
        psmsRelatedPeptidesTableJScrollPane.setViewportView(relatedPsmTable);

        secondarySelectionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Peptide", "Confidence"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        secondarySelectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                secondarySelectionTableMouseReleased(evt);
            }
        });
        secondarySelectionTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                secondarySelectionTableKeyReleased(evt);
            }
        });
        secondarySelectionJScrollPane.setViewportView(secondarySelectionTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(secondarySelectionJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmsRelatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(psmsRelatedPeptidesTableJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                    .addComponent(secondarySelectionJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE))
                .addContainerGap())
        );

        psmSplitPane.setRightComponent(jPanel3);

        psmSpectraSplitPane.setLeftComponent(psmSplitPane);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        jPanel5.setOpaque(false);

        spectrumTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        fragmentIonsJPanel.setOpaque(false);

        javax.swing.GroupLayout fragmentIonsJPanelLayout = new javax.swing.GroupLayout(fragmentIonsJPanel);
        fragmentIonsJPanel.setLayout(fragmentIonsJPanelLayout);
        fragmentIonsJPanelLayout.setHorizontalGroup(
            fragmentIonsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 537, Short.MAX_VALUE)
        );
        fragmentIonsJPanelLayout.setVerticalGroup(
            fragmentIonsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 423, Short.MAX_VALUE)
        );

        spectrumTabbedPane.addTab("Ions", fragmentIonsJPanel);

        spectrumJPanel.setOpaque(false);

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);

        aIonToggleButton.setText("a");
        aIonToggleButton.setFocusable(false);
        aIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aIonToggleButton.setMinimumSize(new java.awt.Dimension(25, 21));
        aIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        aIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(aIonToggleButton);

        bIonToggleButton.setSelected(true);
        bIonToggleButton.setText("b");
        bIonToggleButton.setFocusable(false);
        bIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        bIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(bIonToggleButton);

        cIonToggleButton.setText("c");
        cIonToggleButton.setFocusable(false);
        cIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        cIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(cIonToggleButton);

        xIonToggleButton.setText("x");
        xIonToggleButton.setFocusable(false);
        xIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        xIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        xIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        xIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(xIonToggleButton);

        yIonToggleButton.setSelected(true);
        yIonToggleButton.setText("y");
        yIonToggleButton.setFocusable(false);
        yIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        yIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(yIonToggleButton);

        zIonToggleButton.setText("z");
        zIonToggleButton.setFocusable(false);
        zIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zIonToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        zIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(zIonToggleButton);

        h2oToggleButton.setText("H2O");
        h2oToggleButton.setFocusable(false);
        h2oToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        h2oToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        h2oToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        h2oToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h2oToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(h2oToggleButton);

        nh3ToggleButton.setText("NH3");
        nh3ToggleButton.setFocusable(false);
        nh3ToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nh3ToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        nh3ToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nh3ToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nh3ToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(nh3ToggleButton);

        otherToggleButton.setText("Oth.");
        otherToggleButton.setFocusable(false);
        otherToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        otherToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        otherToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        otherToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(otherToggleButton);

        oneChargeToggleButton.setSelected(true);
        oneChargeToggleButton.setText("+");
        oneChargeToggleButton.setFocusable(false);
        oneChargeToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        oneChargeToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        oneChargeToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        oneChargeToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oneChargeToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(oneChargeToggleButton);

        twoChargesToggleButton.setText("++");
        twoChargesToggleButton.setFocusable(false);
        twoChargesToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        twoChargesToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        twoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        twoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(twoChargesToggleButton);

        moreThanTwoChargesToggleButton.setText(">2 ");
        moreThanTwoChargesToggleButton.setFocusable(false);
        moreThanTwoChargesToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moreThanTwoChargesToggleButton.setPreferredSize(new java.awt.Dimension(39, 25));
        moreThanTwoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moreThanTwoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(moreThanTwoChargesToggleButton);

        spectrumChartJPanel.setOpaque(false);
        spectrumChartJPanel.setLayout(new javax.swing.BoxLayout(spectrumChartJPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumTabbedPane.setSelectedIndex(1);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
        );

        psmSpectraSplitPane.setRightComponent(jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(psmSpectraSplitPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1074, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmSpectraSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the peptide table.
     *
     * @param evt
     */
    private void modificationsListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationsListMouseClicked
        updatePeptideTable();
    }//GEN-LAST:event_modificationsListMouseClicked

    /**
     * Update the related peptides and modified peptide psms tables.
     *
     * @param evt
     */
    private void peptidesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseClicked
        updateRelatedPeptidesTable();
        updatePeptideFamilies();
        updateSelectedPsmTable();
        updateRelatedPsmTable();
    }//GEN-LAST:event_peptidesTableMouseClicked

    /**
     * Update the related peptides psm table.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseClicked
        updatePeptideFamilies();
        updateRelatedPsmTable();
    }//GEN-LAST:event_relatedPeptidesTableMouseClicked

    /**
     * Update the peptide table.
     *
     * @param evt
     */
    private void modificationsListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_modificationsListKeyReleased
        updatePeptideTable();
    }//GEN-LAST:event_modificationsListKeyReleased

    /**
     * @see #peptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void peptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidesTableKeyReleased
        peptidesTableMouseClicked(null);
    }//GEN-LAST:event_peptidesTableKeyReleased

    /**
     * @see #relatedPeptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void relatedPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPeptidesTableKeyReleased
        relatedPeptidesTableMouseClicked(null);
    }//GEN-LAST:event_relatedPeptidesTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void selectedPsmTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedPsmTableMouseClicked
        updateSpectra();
    }//GEN-LAST:event_selectedPsmTableMouseClicked

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void selectedPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_selectedPsmTableKeyReleased
        updateSpectra();
    }//GEN-LAST:event_selectedPsmTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void relatedPsmTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmTableMouseClicked
        updateSpectra();
    }//GEN-LAST:event_relatedPsmTableMouseClicked

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void relatedPsmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPsmTableKeyReleased
        updateSpectra();
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
                updateUI();
            }
        });

    }//GEN-LAST:event_formComponentResized

    private void primarySelectionTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_primarySelectionTableMouseReleased
        updateSelectedPsmTable();
        updateSpectra();
    }//GEN-LAST:event_primarySelectionTableMouseReleased

    private void primarySelectionTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_primarySelectionTableKeyReleased
        primarySelectionTableMouseReleased(null);
    }//GEN-LAST:event_primarySelectionTableKeyReleased

    private void secondarySelectionTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_secondarySelectionTableMouseReleased
        updateRelatedPsmTable();
        updateSpectra();
    }//GEN-LAST:event_secondarySelectionTableMouseReleased

    private void secondarySelectionTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_secondarySelectionTableKeyReleased
        secondarySelectionTableMouseReleased(null);
    }//GEN-LAST:event_secondarySelectionTableKeyReleased

    private void aIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_aIonToggleButtonActionPerformed

    private void bIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_bIonToggleButtonActionPerformed

    private void cIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_cIonToggleButtonActionPerformed

    private void xIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_xIonToggleButtonActionPerformed

    private void yIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_yIonToggleButtonActionPerformed

    private void zIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_zIonToggleButtonActionPerformed

    private void h2oToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_h2oToggleButtonActionPerformed

    private void nh3ToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3ToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_nh3ToggleButtonActionPerformed

    private void otherToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_otherToggleButtonActionPerformed

    private void oneChargeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_oneChargeToggleButtonActionPerformed

    private void twoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_twoChargesToggleButtonActionPerformed

    private void moreThanTwoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesToggleButtonActionPerformed
        updateSpectra();
}//GEN-LAST:event_moreThanTwoChargesToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton aIonToggleButton;
    private javax.swing.JToggleButton bIonToggleButton;
    private javax.swing.JToggleButton cIonToggleButton;
    private javax.swing.JPanel fragmentIonsJPanel;
    private javax.swing.JToggleButton h2oToggleButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList modificationsList;
    private javax.swing.JToggleButton moreThanTwoChargesToggleButton;
    private javax.swing.JToggleButton nh3ToggleButton;
    private javax.swing.JToggleButton oneChargeToggleButton;
    private javax.swing.JToggleButton otherToggleButton;
    private javax.swing.JSplitPane peptideTablesJSplitPane;
    private javax.swing.JTable peptidesTable;
    private javax.swing.JScrollPane peptidesTableJScrollPane;
    private javax.swing.JScrollPane primarySelectionJScrollPane;
    private javax.swing.JTable primarySelectionTable;
    private javax.swing.JSplitPane psmSpectraSplitPane;
    private javax.swing.JSplitPane psmSplitPane;
    private javax.swing.JScrollPane psmsModifiedTableJScrollPane;
    private javax.swing.JScrollPane psmsRelatedPeptidesTableJScrollPane;
    private javax.swing.JTable relatedPeptidesTable;
    private javax.swing.JScrollPane relatedPeptidesTableJScrollPane;
    private javax.swing.JTable relatedPsmTable;
    private javax.swing.JScrollPane secondarySelectionJScrollPane;
    private javax.swing.JTable secondarySelectionTable;
    private javax.swing.JTable selectedPsmTable;
    private javax.swing.JPanel spectrumChartJPanel;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JTabbedPane spectrumTabbedPane;
    private javax.swing.JToggleButton twoChargesToggleButton;
    private javax.swing.JToggleButton xIonToggleButton;
    private javax.swing.JToggleButton yIonToggleButton;
    private javax.swing.JToggleButton zIonToggleButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays or hide sparklines in tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumbers(!showSparkLines);

        peptidesTable.revalidate();
        peptidesTable.repaint();

        relatedPeptidesTable.revalidate();
        relatedPeptidesTable.repaint();
    }

    /**
     * Creates the peptide map.
     */
    private void createPeptideMap() {

        String modificationName;
        boolean modified;
        ArrayList<String> accountedModifications;

        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {

            modified = false;
            accountedModifications = new ArrayList<String>();

            for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {

                if (modificationMatch.isVariable()) {

                    modificationName = modificationMatch.getTheoreticPtm().getName();

                    if (!accountedModifications.contains(modificationName)) {

                        if (!peptideMap.containsKey(modificationName)) {
                            peptideMap.put(modificationName, new ArrayList<String>());
                        }

                        peptideMap.get(modificationName).add(peptideMatch.getKey());
                        modified = true;
                        accountedModifications.add(modificationName);
                    }
                }
            }

            if (!modified) {
                if (!peptideMap.containsKey(NO_MODIFICATION)) {
                    peptideMap.put(NO_MODIFICATION, new ArrayList<String>());
                }
                peptideMap.get(NO_MODIFICATION).add(peptideMatch.getKey());
            }
        }
    }

    /**
     * Displays the results.
     */
    public void displayResults() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        this.identification = peptideShakerGUI.getIdentification();
        createPeptideMap();
        String[] modifications = new String[peptideMap.size()];
        int cpt = 0;

        for (String modification : peptideMap.keySet()) {
            modifications[cpt] = modification;
            cpt++;
        }

        Arrays.sort(modifications);
        modificationsList.setListData(modifications);
        modificationsList.setSelectedIndex(0);
        updatePeptideTable();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Updates the peptide table.
     */
    private void updatePeptideTable() {

        HashMap<Double, ArrayList<String>> scoreToPeptideMap = new HashMap<Double, ArrayList<String>>();
        PeptideMatch peptideMatch;
        PSParameter probabilities = new PSParameter();
        double p;

        for (String peptideKey : peptideMap.get((String) modificationsList.getSelectedValue())) {

            peptideMatch = identification.getPeptideIdentification().get(peptideKey);

            if (!peptideMatch.isDecoy()) {

                probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
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

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptidesTable.revalidate();
                peptidesTable.repaint();
                peptidesTable.setRowSelectionInterval(0, 0);
                updateRelatedPeptidesTable();
                updatePeptideFamilies();
                updateSelectedPsmTable();
                updateRelatedPsmTable();
            }
        });
    }

    /**
     * Updates the related peptides table.
     */
    private void updateRelatedPeptidesTable() {

        HashMap<Double, ArrayList<String>> scoreToKeyMap = new HashMap<Double, ArrayList<String>>();
        PeptideMatch selectedPeptide = identification.getPeptideIdentification().get(displayedPeptides.get(peptidesTable.getSelectedRow()));
        String currentSequence, referenceSequence = selectedPeptide.getTheoreticPeptide().getSequence();
        PSParameter probabilities = new PSParameter();
        double p;

        for (PeptideMatch currentPeptide : identification.getPeptideIdentification().values()) {

            currentSequence = currentPeptide.getTheoreticPeptide().getSequence();

            if (currentSequence.contains(referenceSequence) || referenceSequence.contains(currentSequence)) {

                if (!currentPeptide.getKey().equals(selectedPeptide.getKey())) {

                    probabilities = (PSParameter) currentPeptide.getUrParam(probabilities);
                    p = probabilities.getPeptideProbability();

                    if (!scoreToKeyMap.containsKey(p)) {
                        scoreToKeyMap.put(p, new ArrayList<String>());
                    }

                    scoreToKeyMap.get(p).add(currentPeptide.getKey());
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
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                relatedPeptidesTable.revalidate();
                relatedPeptidesTable.repaint();
            }
        });
    }

    /**
     * Updates the tables containing the peptide families
     */
    private void updatePeptideFamilies() {
        psmsMap = new HashMap<String, ArrayList<String>>();
        confidenceMap = new HashMap<String, Double>();
        PeptideMatch peptideMatch = identification.getPeptideIdentification().get(displayedPeptides.get(peptidesTable.getSelectedRow()));
        String sequence = peptideMatch.getTheoreticPeptide().getSequence();
        String familyKey, mainKey;
        PSParameter probabilities = (PSParameter) peptideMatch.getUrParam(new PSParameter());
        for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
            mainKey = getModificationFamily(spectrumMatch.getBestAssumption().getPeptide());
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
            peptideMatch = identification.getPeptideIdentification().get(relatedPeptides.get(relatedPeptidesTable.getSelectedRow()));
            psmsMap.put("Related Peptide", new ArrayList<String>(peptideMatch.getSpectrumMatches().keySet()));
        }

        while (primarySelectionTable.getRowCount() > 0) {
            ((DefaultTableModel) primarySelectionTable.getModel()).removeRow(0);
        }
        while (secondarySelectionTable.getRowCount() > 0) {
            ((DefaultTableModel) secondarySelectionTable.getModel()).removeRow(0);
        }
        if (!relatedPeptides.isEmpty()) {
            ((DefaultTableModel) secondarySelectionTable.getModel()).addRow(new Object[]{
                        "Related Peptide",
                        null
                    });
        }
        ArrayList<String> keys = new ArrayList<String>(confidenceMap.keySet()); // Maybe we want it sorted by confidence? Not sure...
        Collections.sort(keys);
        for (String key : keys) {
            ((DefaultTableModel) primarySelectionTable.getModel()).addRow(new Object[]{
                        key,
                        confidenceMap.get(key)
                    });
            ((DefaultTableModel) secondarySelectionTable.getModel()).addRow(new Object[]{
                        key,
                        confidenceMap.get(key)
                    });
        }
        primarySelectionTable.setRowSelectionInterval(0, 0);
        if (!relatedPeptides.isEmpty() || secondarySelectionTable.getRowCount() == 1) {
            secondarySelectionTable.setRowSelectionInterval(0, 0);
        } else {
            secondarySelectionTable.setRowSelectionInterval(0, 1);
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


        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                selectedPsmTable.revalidate();
                selectedPsmTable.repaint();
                // TODO: verify that this is at the right place
                selectedPsmTable.setRowSelectionInterval(0, 0);
                updateSpectra();
            }
        });
    }

    /**
     * Update the related peptides PSM table.
     */
    private void updateRelatedPsmTable() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                relatedPsmTable.revalidate();
                relatedPsmTable.repaint();
                // TODO: verify that this is at the right place
                if (!relatedPeptides.isEmpty() || secondarySelectionTable.getRowCount() > 1) {
                    relatedPsmTable.setRowSelectionInterval(0, 0);
                    updateSpectra();
                }
            }
        });
    }

    /**
     * Update the spectra according to the currently selected psms.
     */
    private void updateSpectra() {

        linkedSpectrumPanels = new HashMap<Integer, SpectrumPanel>();
        spectrumChartJPanel.removeAll();
        spectrumChartJPanel.revalidate();
        spectrumChartJPanel.repaint();

        try {
            if (selectedPsmTable.getSelectedRow() != -1) {
                String familyKey = (String) primarySelectionTable.getValueAt(primarySelectionTable.getSelectedRow(), 0);
                String spectrumKey = psmsMap.get(familyKey).get(selectedPsmTable.getSelectedRow());
                peptideShakerGUI.selectSpectrum(spectrumKey);
                
                MSnSpectrum currentSpectrum = (MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(
                        2, spectrumKey);

                Precursor precursor = currentSpectrum.getPrecursor();
                spectrumA = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), precursor.getCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrumA.setBorder(null);

                // get the spectrum annotations
                SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumIdentification().get(spectrumKey);
                Peptide currentPeptide = spectrumMatch.getBestAssumption().getPeptide();
                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                    if (getModificationFamily(peptideAssumption.getPeptide()).equals(familyKey)) {
                        currentPeptide = peptideAssumption.getPeptide();
                        break;
                    }
                }
                SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                        currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
                        currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));

                // add the spectrum annotations
                currentAnnotations = spectrumAnnotator.getSpectrumAnnotations(annotations);
                spectrumA.setAnnotations(filterAnnotations(currentAnnotations));

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

            if (relatedPsmTable.getSelectedRow() != -1) {
                String familyKey = (String) secondarySelectionTable.getValueAt(secondarySelectionTable.getSelectedRow(), 0);

                String spectrumKey = psmsMap.get(familyKey).get(relatedPsmTable.getSelectedRow());
                MSnSpectrum currentSpectrum = (MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(
                        2, spectrumKey);

                Precursor precursor = currentSpectrum.getPrecursor();
                spectrumB = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), precursor.getCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrumB.setBorder(null);

                // get the spectrum annotations                
                SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumIdentification().get(spectrumKey);
                Peptide currentPeptide = spectrumMatch.getBestAssumption().getPeptide();
                if (!familyKey.equals("Related Peptide")) {
                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                        if (getModificationFamily(peptideAssumption.getPeptide()).equals(familyKey)) {
                            currentPeptide = peptideAssumption.getPeptide();
                            break;
                        }
                    }
                }

                SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                        currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
                        currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));

                // add the spectrum annotations
                currentAnnotations = spectrumAnnotator.getSpectrumAnnotations(annotations);
                spectrumB.setAnnotations(filterAnnotations(currentAnnotations));

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
                spectrumA.rescale(0, spectrumB.getMaxXAxisValue());

                spectrumChartJPanel.add(spectrumB);
            }

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    spectrumChartJPanel.revalidate();
                    spectrumChartJPanel.repaint();
                }
            });

        } catch (MzMLUnmarshallerException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Filters the annotations and returns the annotations matching the currently selected list.
     *
     * @param annotations the annotations to be filtered
     * @return the filtered annotations
     */
    private Vector<DefaultSpectrumAnnotation> filterAnnotations(Vector<DefaultSpectrumAnnotation> annotations) {

        return SpectrumPanel.filterAnnotations(annotations, getCurrentFragmentIonTypes(),
                h2oToggleButton.isSelected(),
                nh3ToggleButton.isSelected(),
                oneChargeToggleButton.isSelected(),
                twoChargesToggleButton.isSelected(),
                moreThanTwoChargesToggleButton.isSelected());
    }
    
    /**
     * Returns an arraylist of the currently selected fragment ion types.
     *
     * @return an arraylist of the currently selected fragment ion types
     */
    private ArrayList<PeptideFragmentIonType> getCurrentFragmentIonTypes() {

        ArrayList<PeptideFragmentIonType> fragmentIontypes = new ArrayList<PeptideFragmentIonType>();

        if (aIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.A_ION);
            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.AH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.ANH3_ION);
            }
        }

        if (bIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.B_ION);
            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.BH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.BNH3_ION);
            }
        }

        if (cIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.C_ION);
        }

        if (xIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.X_ION);
        }

        if (yIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.Y_ION);
            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.YH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.YNH3_ION);
            }
        }

        if (zIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.Z_ION);
        }

        if (otherToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.IMMONIUM);
            fragmentIontypes.add(PeptideFragmentIonType.MH_ION);

            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.MHH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.MHNH3_ION);
            }
        }

        return fragmentIontypes;
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
                return "Peptide Sequence";
            } else if (column == 3) {
                return "Modification(s)";
            } else if (column == 4) {
                return "Score";
            } else if (column == 5) {
                return "Confidence [%]";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            if (column == 0) {
                return new Double(row + 1);
            } else if (column == 1) {
                String result = "";
                for (Protein protein : identification.getPeptideIdentification().get(displayedPeptides.get(row)).getTheoreticPeptide().getParentProteins()) {
                    result += protein.getAccession() + " ";
                }
                return result;
            } else if (column == 2) {
                return identification.getPeptideIdentification().get(displayedPeptides.get(row)).getTheoreticPeptide().getSequence();
            } else if (column == 3) {
                String result = "";
                for (ModificationMatch modificationMatch : identification.getPeptideIdentification().get(displayedPeptides.get(row)).getTheoreticPeptide().getModificationMatches()) {
                    if (modificationMatch.isVariable()) {
                        result += modificationMatch.getTheoreticPtm().getName() + " ";
                    }
                }

                if (result.length() == 0) {
                    result = null;
                }

                return result;
            } else if (column == 4) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(displayedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideScore();
            } else if (column == 5) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(displayedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideConfidence();
            } else {
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
                return "Peptide Sequence";
            } else if (column == 3) {
                return "Modification(s)";
            } else if (column == 4) {
                return "Score";
            } else if (column == 5) {
                return "Confidence [%]";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return new Double(row + 1);
            } else if (column == 1) {
                String result = "";
                for (Protein protein : identification.getPeptideIdentification().get(relatedPeptides.get(row)).getTheoreticPeptide().getParentProteins()) {
                    result += protein.getAccession() + " ";
                }
                return result;
            } else if (column == 2) {
                return identification.getPeptideIdentification().get(relatedPeptides.get(row)).getTheoreticPeptide().getSequence();
            } else if (column == 3) {
                String result = "";
                for (ModificationMatch modificationMatch : identification.getPeptideIdentification().get(relatedPeptides.get(row)).getTheoreticPeptide().getModificationMatches()) {
                    if (modificationMatch.isVariable()) {
                        result += modificationMatch.getTheoreticPtm().getName() + " ";
                    }
                }

                if (result.length() == 0) {
                    result = null;
                }

                return result;
            } else if (column == 4) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(relatedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideScore();
            } else if (column == 5) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(relatedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideConfidence();
            } else {
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
     * Table model for the modified peptide PSMs table
     */
    private class SelectedPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (primarySelectionTable.getRowCount()==0) {
                return 0;
            }
            String familyKey = (String) primarySelectionTable.getValueAt(primarySelectionTable.getSelectedRow(), 0);
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
                return "Peptide Sequence";
            } else if (column == 2) {
                return "Modification(s)";
            } else if (column == 3) {
                return "Precursor m/z";
            } else if (column == 4) {
                return "Precursor Charge";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String familyKey = (String) primarySelectionTable.getValueAt(primarySelectionTable.getSelectedRow(), 0);
            SpectrumMatch spectrumMatch = identification.getSpectrumIdentification().get(psmsMap.get(familyKey).get(row));
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
                    for (double eValue : eValues) {
                        if (getModificationFamily(spectrumMatch.getAllAssumptions(seKey).get(eValue).getPeptide()).equals(familyKey)) {
                            if (seKey == Advocate.MASCOT) {
                                result += "M" + cpt + " ";
                            } else if (seKey == Advocate.OMSSA) {
                                result += "O" + cpt + " ";
                            } else if (seKey == Advocate.XTANDEM) {
                                result += "X" + cpt + " ";
                            }
                            break;
                        }
                        cpt++;
                    }
                }
                return result;
            } else if (column == 1) {
                // Not sure that it will wlways be the best assumption, might have to iterate assumptions if the wrong sequence is displayed
                return spectrumMatch.getBestAssumption().getPeptide().getSequence();
            } else if (column == 2) {
                return familyKey;
            } else if (column == 3) {
                try {
                return ((MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(2, spectrumMatch.getKey())).getPrecursor().getMz();
                } catch (MzMLUnmarshallerException e) {
                    return "";
                }
            } else if (column == 4) {
                try {
                return ((MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(2, spectrumMatch.getKey())).getPrecursor().getCharge().toString();
                } catch (MzMLUnmarshallerException e) {
                    return "";
                }
            } else {
                return psmsMap.get(familyKey).get(row);
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
            if (secondarySelectionTable.getRowCount()==0) {
                return 0;
            }
            String familyKey = (String) secondarySelectionTable.getValueAt(secondarySelectionTable.getSelectedRow(), 0);
            return psmsMap.get(familyKey).size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                if (secondarySelectionTable.getRowCount() > 0
                        && ((String) secondarySelectionTable.getValueAt(secondarySelectionTable.getSelectedRow(), 0)).equals("Related Peptide")) {
                    return " ";
                }
                return "Rank";
            } else if (column == 1) {
                return "Peptide Sequence";
            } else if (column == 2) {
                return "Modification(s)";
            } else if (column == 3) {
                return "Precursor m/z";
            } else if (column == 4) {
                return "Precursor Charge";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String familyKey = (String) secondarySelectionTable.getValueAt(secondarySelectionTable.getSelectedRow(), 0);
            SpectrumMatch spectrumMatch = identification.getSpectrumIdentification().get(psmsMap.get(familyKey).get(row));
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
                    for (double eValue : eValues) {
                        if (getModificationFamily(spectrumMatch.getAllAssumptions(seKey).get(eValue).getPeptide()).equals(familyKey)) {
                            if (seKey == Advocate.MASCOT) {
                                result += "M" + cpt + " ";
                            } else if (seKey == Advocate.OMSSA) {
                                result += "O" + cpt + " ";
                            } else if (seKey == Advocate.XTANDEM) {
                                result += "X" + cpt + " ";
                            }
                            break;
                        }
                        cpt++;
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
                return ((MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(2, spectrumMatch.getKey())).getPrecursor().getMz();
                } catch (MzMLUnmarshallerException e) {
                    return "";
                }
            } else if (column == 4) {
                try {
                return ((MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(2, spectrumMatch.getKey())).getPrecursor().getCharge().toString();
                } catch (MzMLUnmarshallerException e) {
                    return "";
                }
            } else {
                return psmsMap.get(familyKey).get(row);
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
}
