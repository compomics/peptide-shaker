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
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumCollection;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.VennDiagram;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The Spectrum ID panel.
 * 
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumIdentificationPanel extends javax.swing.JPanel {

    /**
     * The search engine table column header tooltips.
     */
    private ArrayList<String> searchEngineTableToolTips;
    /**
     * The spectrum table column header tooltips.
     */
    private ArrayList<String> spectrumTableToolTips;
    /**
     * The peptide shaker table column header tooltips.
     */
    private ArrayList<String> peptideShakerTableToolTips;
    /**
     * The OMSSA table column header tooltips.
     */
    private ArrayList<String> omssaTableToolTips;
    /**
     * The X!Tandem table column header tooltips.
     */
    private ArrayList<String> xTandemTableToolTips;
    /**
     * The Mascot table column header tooltips.
     */
    private ArrayList<String> mascotTableToolTips;
    /**
     * The spectrum annotator
     */
    private SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
    /**
     * The list of OMSSA peptide keys.
     */
    private HashMap<Integer, String> omssaPeptideKeys = new HashMap<Integer, String>();
    /**
     * The list of X!Tandem peptide keys.
     */
    private HashMap<Integer, String> xtandemPeptideKeys = new HashMap<Integer, String>();
    /**
     * The list of Mascot peptide keys.
     */
    private HashMap<Integer, String> mascotPeptideKeys = new HashMap<Integer, String>();
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The spectrum collection
     */
    private SpectrumCollection spectrumCollection;
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The spectra indexed by their file name
     */
    private HashMap<String, ArrayList<String>> filesMap = new HashMap<String, ArrayList<String>>();

    /**
     * Create a new SpectrumIdentificationPanel.
     * 
     * @param peptideShakerGUI  the PeptideShaker parent frame
     */
    public SpectrumIdentificationPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        searchEnginetableJScrollPane.getViewport().setOpaque(false);
        spectrumTableJScrollPane.getViewport().setOpaque(false);
        peptideShakerJScrollPane.getViewport().setOpaque(false);
        xTandemTableJScrollPane.getViewport().setOpaque(false);
        mascotTableJScrollPane.getViewport().setOpaque(false);
        omssaTableJScrollPane.getViewport().setOpaque(false);

        fileNamesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        setTableProperties();
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        peptideShakerJTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), new ImageIcon(this.getClass().getResource("/icons/Error_3.png"))));

        searchEngineTable.getTableHeader().setReorderingAllowed(false);
        peptideShakerJTable.getTableHeader().setReorderingAllowed(false);
        spectrumTable.getTableHeader().setReorderingAllowed(false);
        omssaTable.getTableHeader().setReorderingAllowed(false);
        mascotTable.getTableHeader().setReorderingAllowed(false);
        xTandemTable.getTableHeader().setReorderingAllowed(false);

        spectrumTable.setAutoCreateRowSorter(true);
        searchEngineTable.setAutoCreateRowSorter(true);
        omssaTable.setAutoCreateRowSorter(true);
        mascotTable.setAutoCreateRowSorter(true);
        xTandemTable.setAutoCreateRowSorter(true);

        peptideShakerJTable.getColumn(" ").setMinWidth(30);
        peptideShakerJTable.getColumn(" ").setMaxWidth(30);
        peptideShakerJTable.getColumn("  ").setMinWidth(30);
        peptideShakerJTable.getColumn("  ").setMaxWidth(30);
        searchEngineTable.getColumn(" ").setMinWidth(30);
        searchEngineTable.getColumn(" ").setMaxWidth(30);
        spectrumTable.getColumn(" ").setMinWidth(50);
        spectrumTable.getColumn(" ").setMaxWidth(50);

        omssaTable.getColumn(" ").setMinWidth(30);
        omssaTable.getColumn(" ").setMaxWidth(30);
        mascotTable.getColumn(" ").setMinWidth(30);
        mascotTable.getColumn(" ").setMaxWidth(30);
        xTandemTable.getColumn(" ").setMinWidth(30);
        xTandemTable.getColumn(" ").setMaxWidth(30);

        peptideShakerJTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        peptideShakerJTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        searchEngineTable.getColumn("Validated PSMs").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Unique PSMs").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("OMSSA").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("X!Tandem").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Mascot").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("All").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        spectrumTable.getColumn("Title").setMinWidth(200);
        spectrumTable.getColumn("Title").setMaxWidth(200);

        spectrumTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        // set up the table header tooltips
        searchEngineTableToolTips = new ArrayList<String>();
        searchEngineTableToolTips.add(null);
        searchEngineTableToolTips.add("Search Engine");
        searchEngineTableToolTips.add("Number of Validated Peptide-Spectrum Matches");
        searchEngineTableToolTips.add("Number of Unique Pepttide-Spectrum Matches");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches with OMSSA");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches with X!Tandem");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches with Mascot");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches All Search Engines");

        spectrumTableToolTips = new ArrayList<String>();
        spectrumTableToolTips.add(null);
        spectrumTableToolTips.add("Spectrum Title");
        spectrumTableToolTips.add("Precursor m/z");
        spectrumTableToolTips.add("Precursor Charge");
        spectrumTableToolTips.add("Precursor Retention Time");

        peptideShakerTableToolTips = new ArrayList<String>();
        peptideShakerTableToolTips.add(null);
        peptideShakerTableToolTips.add("Mapping Protein(s)");
        peptideShakerTableToolTips.add("Peptide Sequence");
        peptideShakerTableToolTips.add("Peptide Modifications");
        peptideShakerTableToolTips.add("Peptide Score");
        peptideShakerTableToolTips.add("Peptide Confidence");
        peptideShakerTableToolTips.add("Delta P");
        peptideShakerTableToolTips.add("Validated");

        omssaTableToolTips = new ArrayList<String>();
        omssaTableToolTips.add("Search Engine Peptide Rank");
        omssaTableToolTips.add("Mapping Protein(s)");
        omssaTableToolTips.add("Peptide Sequence");
        omssaTableToolTips.add("Peptide Modifications");
        omssaTableToolTips.add("Peptide e-value");
        omssaTableToolTips.add("Peptide Confidence");

        xTandemTableToolTips = new ArrayList<String>();
        xTandemTableToolTips.add("Search Engine Peptide Rank");
        xTandemTableToolTips.add("Mapping Protein(s)");
        xTandemTableToolTips.add("Peptide Sequence");
        xTandemTableToolTips.add("Peptide Modifications");
        xTandemTableToolTips.add("Peptide e-value");
        xTandemTableToolTips.add("Peptide Confidence");

        mascotTableToolTips = new ArrayList<String>();
        mascotTableToolTips.add("Search Engine Peptide Rank");
        mascotTableToolTips.add("Mapping Protein(s)");
        mascotTableToolTips.add("Peptide Sequence");
        mascotTableToolTips.add("Peptide Modifications");
        mascotTableToolTips.add("Peptide e-value");
        mascotTableToolTips.add("Peptide Confidence");
    }

    /**
     * Displays or hide sparklines in the tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        searchEngineTable.revalidate();
        searchEngineTable.repaint();

        spectrumTable.revalidate();
        spectrumTable.repaint();

        peptideShakerJTable.revalidate();
        peptideShakerJTable.repaint();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        searchEnginetableJScrollPane = new javax.swing.JScrollPane();
        searchEngineTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) searchEngineTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        vennDiagramButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        peptideShakerJScrollPane = new javax.swing.JScrollPane();
        peptideShakerJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) peptideShakerTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jLabel1 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        omssaTableJScrollPane = new javax.swing.JScrollPane();
        omssaTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) omssaTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jLabel3 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        xTandemTableJScrollPane = new javax.swing.JScrollPane();
        xTandemTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) xTandemTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        mascotTableJScrollPane = new javax.swing.JScrollPane();
        mascotTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) mascotTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumJSplitPane = new javax.swing.JSplitPane();
        spectrumSelectionJPanel = new javax.swing.JPanel();
        fileNamesCmb = new javax.swing.JComboBox();
        spectrumTableJScrollPane = new javax.swing.JScrollPane();
        spectrumTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) spectrumTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jPanel3 = new javax.swing.JPanel();
        spectrumChartPanel = new javax.swing.JPanel();
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

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Engine Performance"));
        jPanel1.setOpaque(false);

        searchEngineTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Search Engine", "Validated PSMs", "Unique PSMs", "OMSSA", "X!Tandem", "Mascot", "All"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
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
        searchEnginetableJScrollPane.setViewportView(searchEngineTable);

        vennDiagramButton.setBorderPainted(false);
        vennDiagramButton.setContentAreaFilled(false);
        vennDiagramButton.setFocusable(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchEnginetableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1101, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(vennDiagramButton, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchEnginetableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(vennDiagramButton, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide to Spectrum Matches"));
        jPanel4.setOpaque(false);

        peptideShakerJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Modifications", "Score", "Confidence", "delta p", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
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
        peptideShakerJScrollPane.setViewportView(peptideShakerJTable);

        jLabel1.setText("PeptideShaker:");

        jPanel5.setOpaque(false);

        omssaTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Peptide", "Modification(s)", "e-value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class
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
        omssaTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        omssaTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                omssaTableMouseClicked(evt);
            }
        });
        omssaTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                omssaTableKeyReleased(evt);
            }
        });
        omssaTableJScrollPane.setViewportView(omssaTable);

        jLabel3.setText("OMSSA:");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addContainerGap(376, Short.MAX_VALUE))
            .addComponent(omssaTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(omssaTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
        );

        jPanel7.setOpaque(false);

        jLabel4.setText("X!Tandem:");

        xTandemTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Peptide", "Modification(s)", "e-value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class
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
        xTandemTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        xTandemTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                xTandemTableMouseClicked(evt);
            }
        });
        xTandemTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                xTandemTableKeyReleased(evt);
            }
        });
        xTandemTableJScrollPane.setViewportView(xTandemTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addContainerGap(365, Short.MAX_VALUE))
            .addComponent(xTandemTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xTandemTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
        );

        jPanel8.setOpaque(false);

        jLabel2.setText("Mascot:");

        mascotTableJScrollPane.setMinimumSize(new java.awt.Dimension(23, 87));

        mascotTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Peptide(s)", "Modification(s)", "e-value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class
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
        mascotTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        mascotTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                mascotTableMouseClicked(evt);
            }
        });
        mascotTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                mascotTableKeyReleased(evt);
            }
        });
        mascotTableJScrollPane.setViewportView(mascotTable);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addContainerGap(378, Short.MAX_VALUE))
            .addComponent(mascotTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mascotTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1284, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        spectrumJSplitPane.setBorder(null);
        spectrumJSplitPane.setDividerLocation(600);
        spectrumJSplitPane.setDividerSize(0);
        spectrumJSplitPane.setResizeWeight(0.5);
        spectrumJSplitPane.setOpaque(false);

        spectrumSelectionJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Selection"));
        spectrumSelectionJPanel.setOpaque(false);

        fileNamesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "File Name" }));
        fileNamesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNamesCmbActionPerformed(evt);
            }
        });

        spectrumTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Title", "m/z", "Charge", "RT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Double.class
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
        spectrumTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumTableMouseReleased(evt);
            }
        });
        spectrumTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumTableKeyReleased(evt);
            }
        });
        spectrumTableJScrollPane.setViewportView(spectrumTable);

        javax.swing.GroupLayout spectrumSelectionJPanelLayout = new javax.swing.GroupLayout(spectrumSelectionJPanel);
        spectrumSelectionJPanel.setLayout(spectrumSelectionJPanelLayout);
        spectrumSelectionJPanelLayout.setHorizontalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumSelectionJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 568, Short.MAX_VALUE)
                    .addComponent(fileNamesCmb, 0, 568, Short.MAX_VALUE))
                .addContainerGap())
        );
        spectrumSelectionJPanelLayout.setVerticalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumSelectionJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNamesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addContainerGap())
        );

        spectrumJSplitPane.setLeftComponent(spectrumSelectionJPanel);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum"));
        jPanel3.setOpaque(false);

        spectrumChartPanel.setBackground(new java.awt.Color(255, 255, 255));
        spectrumChartPanel.setLayout(new javax.swing.BoxLayout(spectrumChartPanel, javax.swing.BoxLayout.Y_AXIS));

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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spectrumChartPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
                    .addComponent(spectrumJToolBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(spectrumChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        spectrumJSplitPane.setRightComponent(jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(spectrumJSplitPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1316, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fileNamesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNamesCmbActionPerformed
        fileSelectionChanged();
    }//GEN-LAST:event_fileNamesCmbActionPerformed

    private void spectrumTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableMouseReleased

    private void spectrumTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumTableKeyReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableKeyReleased

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                spectrumJSplitPane.setDividerLocation(spectrumJSplitPane.getWidth() / 2);
            }
        });
    }//GEN-LAST:event_formComponentResized

    private void omssaTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_omssaTableMouseClicked

        if (omssaTable.getSelectedRow() != -1) {

            if (xTandemTable.getSelectedRow() != -1) {
                xTandemTable.removeRowSelectionInterval(xTandemTable.getSelectedRow(), xTandemTable.getSelectedRow());
            }

            if (mascotTable.getSelectedRow() != -1) {
                mascotTable.removeRowSelectionInterval(mascotTable.getSelectedRow(), mascotTable.getSelectedRow());
            }

            updateSpectrum();
        }

    }//GEN-LAST:event_omssaTableMouseClicked

    private void omssaTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_omssaTableKeyReleased
        omssaTableMouseClicked(null);
    }//GEN-LAST:event_omssaTableKeyReleased

    private void xTandemTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_xTandemTableMouseClicked
        if (xTandemTable.getSelectedRow() != -1) {

            if (mascotTable.getSelectedRow() != -1) {
                mascotTable.removeRowSelectionInterval(mascotTable.getSelectedRow(), mascotTable.getSelectedRow());
            }

            if (omssaTable.getSelectedRow() != -1) {
                omssaTable.removeRowSelectionInterval(omssaTable.getSelectedRow(), omssaTable.getSelectedRow());
            }

            updateSpectrum();
        }
    }//GEN-LAST:event_xTandemTableMouseClicked

    private void xTandemTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_xTandemTableKeyReleased
        xTandemTableMouseClicked(null);
    }//GEN-LAST:event_xTandemTableKeyReleased

    private void mascotTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mascotTableKeyReleased
        mascotTableMouseClicked(null);
    }//GEN-LAST:event_mascotTableKeyReleased

    private void mascotTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mascotTableMouseClicked
        if (mascotTable.getSelectedRow() != -1) {

            if (xTandemTable.getSelectedRow() != -1) {
                xTandemTable.removeRowSelectionInterval(xTandemTable.getSelectedRow(), xTandemTable.getSelectedRow());
            }

            if (omssaTable.getSelectedRow() != -1) {
                omssaTable.removeRowSelectionInterval(omssaTable.getSelectedRow(), omssaTable.getSelectedRow());
            }

            updateSpectrum();
        }
    }//GEN-LAST:event_mascotTableMouseClicked

    private void aIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_aIonToggleButtonActionPerformed

    private void bIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_bIonToggleButtonActionPerformed

    private void cIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_cIonToggleButtonActionPerformed

    private void xIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_xIonToggleButtonActionPerformed

    private void yIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_yIonToggleButtonActionPerformed

    private void zIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_zIonToggleButtonActionPerformed

    private void h2oToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_h2oToggleButtonActionPerformed

    private void nh3ToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3ToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_nh3ToggleButtonActionPerformed

    private void otherToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_otherToggleButtonActionPerformed

    private void oneChargeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_oneChargeToggleButtonActionPerformed

    private void twoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_twoChargesToggleButtonActionPerformed

    private void moreThanTwoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesToggleButtonActionPerformed
        updateSpectrum();
}//GEN-LAST:event_moreThanTwoChargesToggleButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton aIonToggleButton;
    private javax.swing.JToggleButton bIonToggleButton;
    private javax.swing.JToggleButton cIonToggleButton;
    private javax.swing.JComboBox fileNamesCmb;
    private javax.swing.JToggleButton h2oToggleButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JTable mascotTable;
    private javax.swing.JScrollPane mascotTableJScrollPane;
    private javax.swing.JToggleButton moreThanTwoChargesToggleButton;
    private javax.swing.JToggleButton nh3ToggleButton;
    private javax.swing.JTable omssaTable;
    private javax.swing.JScrollPane omssaTableJScrollPane;
    private javax.swing.JToggleButton oneChargeToggleButton;
    private javax.swing.JToggleButton otherToggleButton;
    private javax.swing.JScrollPane peptideShakerJScrollPane;
    private javax.swing.JTable peptideShakerJTable;
    private javax.swing.JTable searchEngineTable;
    private javax.swing.JScrollPane searchEnginetableJScrollPane;
    private javax.swing.JPanel spectrumChartPanel;
    private javax.swing.JSplitPane spectrumJSplitPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JPanel spectrumSelectionJPanel;
    private javax.swing.JTable spectrumTable;
    private javax.swing.JScrollPane spectrumTableJScrollPane;
    private javax.swing.JToggleButton twoChargesToggleButton;
    private javax.swing.JButton vennDiagramButton;
    private javax.swing.JToggleButton xIonToggleButton;
    private javax.swing.JTable xTandemTable;
    private javax.swing.JScrollPane xTandemTableJScrollPane;
    private javax.swing.JToggleButton yIonToggleButton;
    private javax.swing.JToggleButton zIonToggleButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays the results on the panel
     */
    public void displayResults() {
        spectrumCollection = peptideShakerGUI.getSpectrumCollection();
        identification = peptideShakerGUI.getIdentification();
        int m = 0;
        int o = 0;
        int x = 0;
        int mo = 0;
        int mx = 0;
        int ox = 0;
        int omx = 0;
        boolean mascot, omssa, xTandem;
        PSParameter probabilities = new PSParameter();

        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
            mascot = false;
            omssa = false;
            xTandem = false;
            probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);
            if (probabilities.isValidated()) {
                if (spectrumMatch.getFirstHit(Advocate.MASCOT) != null) {
                    if (spectrumMatch.getFirstHit(Advocate.MASCOT).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                        mascot = true;
                    }
                }
                if (spectrumMatch.getFirstHit(Advocate.OMSSA) != null) {
                    if (spectrumMatch.getFirstHit(Advocate.OMSSA).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                        omssa = true;
                    }
                }
                if (spectrumMatch.getFirstHit(Advocate.XTANDEM) != null) {
                    if (spectrumMatch.getFirstHit(Advocate.XTANDEM).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                        xTandem = true;
                    }
                }
            }

            if (mascot && omssa && xTandem) {
                omx++;
            } else if (mascot && omssa) {
                mo++;
            } else if (omssa && xTandem) {
                ox++;
            } else if (mascot && xTandem) {
                mx++;
            } else if (mascot) {
                m++;
            } else if (omssa) {
                o++;
            } else if (xTandem) {
                x++;
            }
        }

        int nMascot = omx + mo + mx + m;
        int nOMSSA = omx + mo + ox + o;
        int nXTandem = omx + mx + ox + x;

        double biggestValue = Math.max(Math.max(nMascot, nOMSSA), nXTandem);

        updateVennDiagram(vennDiagramButton, nOMSSA, nXTandem, nMascot,
                (ox + omx), (mo + omx), (mx + omx), omx,
                "OMSSA", "X!Tandem", "Mascot");

        ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                    1, "OMSSA",
                    nOMSSA, o, nOMSSA, ox + omx, mo + omx, omx
                });
        ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                    2, "X!Tandem",
                    nXTandem, x, ox + omx, nXTandem, mx + omx, omx
                });
        ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                    3, "Mascot",
                    nMascot, m, mo + omx, mx + omx, nMascot, omx
                });


        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).setMaxValue(biggestValue);

        searchEngineTable.revalidate();
        searchEngineTable.repaint();

        String fileName;
        for (String key : spectrumCollection.getAllKeys()) {
            fileName = Spectrum.getSpectrumFile(key);
            if (!filesMap.containsKey(fileName)) {
                filesMap.put(fileName, new ArrayList<String>());
            }
            filesMap.get(fileName).add(key);
        }
        String[] filesArray = new String[filesMap.keySet().size()];
        int cpt = 0;
        for (String tempName : filesMap.keySet()) {
            filesArray[cpt] = tempName;
            cpt++;
        }
        fileNamesCmb.setModel(new DefaultComboBoxModel(filesArray));
        fileSelectionChanged();
    }

    /**
     * Create a Venn diagram and add it to the given button.
     * 
     * @param diagramButton     the button to add the diagram to
     * @param a                 the size of A
     * @param b                 the size of B
     * @param c                 the size of C
     * @param ab                the overlapp of A and B
     * @param ac                the overlapp of A and C
     * @param bc                the overlapp of B and C
     * @param abc               the number of values in A, B and C
     * @param titleA            the title of dataset A
     * @param titleB            the title of dataset B
     * @param titleC            the title of dataset C
     */
    private void updateVennDiagram(JButton diagramButton, int a, int b, int c, int ab, int ac, int bc, int abc,
            String titleA, String titleB, String titleC) {

        double maxValue = Math.max(Math.max(a, b), c);

        // @TODO: add test of internet is not available
        // @TODO: move this method to utilities?

        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, c / maxValue, ab / maxValue, ac / maxValue, bc / maxValue, abc / maxValue);
        chart.setSize(diagramButton.getWidth(), diagramButton.getHeight());
        chart.setCircleLegends(titleA, titleB, titleC);
        chart.setCircleColors(Color.YELLOW, Color.RED, Color.BLUE);
        chart.setBackgroundFill(Fills.newSolidFill(Color.WHITE));

        try {
            diagramButton.setText("");
            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));
            diagramButton.setIcon(icon);

            diagramButton.setToolTipText("<html>"
                    + titleA + ": " + a + "<br>"
                    + titleB + ": " + b + "<br>"
                    + titleC + ": " + c + "<br><br>"
                    + titleA + " & " + titleB + ": " + ab + "<br>"
                    + titleA + " & " + titleC + ": " + ac + "<br>"
                    + titleB + " & " + titleC + ": " + bc + "<br><br>"
                    + titleA + " & " + titleB + " & " + titleC + ": " + abc
                    + "</html>");

        } catch (IOException e) {
            e.printStackTrace();
            diagramButton.setText("<Not Available>");
        }
    }

    /**
     * Method called whenever the file selection changed
     */
    private void fileSelectionChanged() {
        try {
            while (spectrumTable.getRowCount() > 0) {
                ((DefaultTableModel) spectrumTable.getModel()).removeRow(0);
            }

            String fileSelected = (String) fileNamesCmb.getSelectedItem();

            int maxCharge = Integer.MIN_VALUE;
            double maxMz = Double.MIN_VALUE;

            double lLowRT = Double.MAX_VALUE;
            double lHighRT = Double.MIN_VALUE;

            int counter = 0;

            for (String spectrumKey : filesMap.get(fileSelected)) {

                MSnSpectrum spectrum = (MSnSpectrum) spectrumCollection.getSpectrum(spectrumKey);
                Precursor precursor = spectrum.getPrecursor();
                
                double retentionTime = precursor.getRt();
                
                if (retentionTime == -1) {
                    retentionTime = 0;
                }
                
                ((DefaultTableModel) spectrumTable.getModel()).addRow(new Object[]{
                            ++counter,
                            spectrum.getSpectrumTitle(),
                            precursor.getMz(),
                            precursor.getCharge().value,
                            retentionTime
                        });

                if (precursor.getCharge().value > maxCharge) {
                    maxCharge = precursor.getCharge().value;
                }

                if (lLowRT > retentionTime) {
                    lLowRT = retentionTime;
                }

                if (lHighRT < retentionTime) {
                    lHighRT = retentionTime;
                }

                if (precursor.getMz() > maxMz) {
                    maxMz = precursor.getMz();
                }
            }

            //lLowRT -= 1.0;
            //double widthOfMarker = (lHighRT / lLowRT) * 4; // @TODO: switch this back on later??

            lLowRT = 100;
            double widthOfMarker = 200;

            ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).setMaxValue(maxCharge);
            ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).setMaxValue(maxMz);

            JSparklinesIntervalChartTableCellRenderer lRTCellRenderer = new JSparklinesIntervalChartTableCellRenderer(
                    PlotOrientation.HORIZONTAL, lLowRT - widthOfMarker / 2, lHighRT + widthOfMarker / 2, widthOfMarker,
                    peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor());
            spectrumTable.getColumn("RT").setCellRenderer(lRTCellRenderer);
            lRTCellRenderer.showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

            spectrumTable.setRowSelectionInterval(0, 0);
            spectrumSelectionChanged();
        } catch (MzMLUnmarshallerException e) {
            JOptionPane.showMessageDialog(this, "Error while importing mzML data.", "Peak Lists Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Select the given spectrum.
     * 
     * @param spectrumKey 
     */
    public void selectSpectrum(String spectrumKey) {
        String fileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        fileNamesCmb.setSelectedItem(fileName);

        // We might want something faster here
        for (int i = 0; i < spectrumTable.getRowCount(); i++) {
            if (((String) spectrumTable.getValueAt(i, 1)).equals(spectrumTitle)) {
                spectrumTable.setRowSelectionInterval(i, i);
                spectrumTable.scrollRectToVisible(spectrumTable.getCellRect(i, 0, false));
                break;
            }
        }

        spectrumSelectionChanged();
    }

    /**
     * Method called whenever the spectrum selection changed
     */
    private void spectrumSelectionChanged() {

        if (spectrumTable.getSelectedRow() != -1) {

            String key = Spectrum.getSpectrumKey((String) fileNamesCmb.getSelectedItem(), (String) spectrumTable.getValueAt(spectrumTable.getSelectedRow(), 1));
            SpectrumMatch spectrumMatch = identification.getSpectrumIdentification().get(key);
            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);

            while (peptideShakerJTable.getRowCount() > 0) {
                ((DefaultTableModel) peptideShakerJTable.getModel()).removeRow(0);
            }
            while (omssaTable.getRowCount() > 0) {
                ((DefaultTableModel) omssaTable.getModel()).removeRow(0);
            }
            while (mascotTable.getRowCount() > 0) {
                ((DefaultTableModel) mascotTable.getModel()).removeRow(0);
            }
            while (xTandemTable.getRowCount() > 0) {
                ((DefaultTableModel) xTandemTable.getModel()).removeRow(0);
            }

            // Fill peptide shaker table
            String proteins = "";
            for (Protein protein : spectrumMatch.getBestAssumption().getPeptide().getParentProteins()) {
                proteins += protein.getAccession() + " ";
            }
            String modifications = "";
            boolean firstline = true;
            for (ModificationMatch modificationMatch : spectrumMatch.getBestAssumption().getPeptide().getModificationMatches()) {
                if (modificationMatch.isVariable()) {
                    if (!firstline) {
                        modifications += ", ";
                    } else {
                        firstline = false;
                    }
                    modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                }
            }
            ((DefaultTableModel) peptideShakerJTable.getModel()).addRow(new Object[]{
                        1,
                        proteins,
                        spectrumMatch.getBestAssumption().getPeptide().getSequence(),
                        modifications,
                        probabilities.getPsmScore(),
                        probabilities.getPsmConfidence(),
                        0,
                        probabilities.isValidated()
                    });

            // Fill Mascot table

            if (spectrumMatch.getAllAssumptions(Advocate.MASCOT) != null) {
                ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.MASCOT).keySet());
                Collections.sort(eValues);
                int rank = 0;
                for (double eValue : eValues) {
                    for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValue)) {
                        proteins = "";
                        for (Protein protein : currentAssumption.getPeptide().getParentProteins()) {
                            proteins += protein.getAccession() + " ";
                        }
                        modifications = "";
                        firstline = true;
                        for (ModificationMatch modificationMatch : currentAssumption.getPeptide().getModificationMatches()) {
                            if (modificationMatch.isVariable()) {
                                if (!firstline) {
                                    modifications += ", ";
                                } else {
                                    firstline = false;
                                }
                                modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                            }
                        }
                        ((DefaultTableModel) mascotTable.getModel()).addRow(new Object[]{
                                    ++rank,
                                    proteins,
                                    currentAssumption.getPeptide().getSequence(),
                                    modifications,
                                    currentAssumption.getEValue(),
                                    //0 //@TODO: temporarily removed
                                });

                        mascotPeptideKeys.put(rank, currentAssumption.getPeptide().getKey());
                    }
                }
            }

            // Fill OMSSA table

            omssaPeptideKeys = new HashMap<Integer, String>();

            if (spectrumMatch.getAllAssumptions(Advocate.OMSSA) != null) {
                ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.OMSSA).keySet());
                Collections.sort(eValues);
                int rank = 0;
                for (double eValue : eValues) {
                    for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValue)) {
                        proteins = "";
                        for (Protein protein : currentAssumption.getPeptide().getParentProteins()) {
                            proteins += protein.getAccession() + " ";
                        }
                        modifications = "";
                        firstline = true;
                        for (ModificationMatch modificationMatch : currentAssumption.getPeptide().getModificationMatches()) {
                            if (modificationMatch.isVariable()) {
                                if (!firstline) {
                                    modifications += ", ";
                                } else {
                                    firstline = false;
                                }
                                modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                            }
                        }
                        ((DefaultTableModel) omssaTable.getModel()).addRow(new Object[]{
                                    ++rank,
                                    proteins,
                                    currentAssumption.getPeptide().getSequence(),
                                    modifications,
                                    currentAssumption.getEValue(),
                                    //0 //@TODO: temporarily removed
                                });

                        omssaPeptideKeys.put(rank, currentAssumption.getPeptide().getKey());
                    }
                }
            }

            // Fill X!Tandem table

            xtandemPeptideKeys = new HashMap<Integer, String>();

            if (spectrumMatch.getAllAssumptions(Advocate.XTANDEM) != null) {
                ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.XTANDEM).keySet());
                Collections.sort(eValues);
                int rank = 0;
                for (double eValue : eValues) {
                    for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValue)) {
                        proteins = "";
                        for (Protein protein : currentAssumption.getPeptide().getParentProteins()) {
                            proteins += protein.getAccession() + " ";
                        }
                        modifications = "";
                        firstline = true;
                        for (ModificationMatch modificationMatch : currentAssumption.getPeptide().getModificationMatches()) {
                            if (modificationMatch.isVariable()) {
                                if (!firstline) {
                                    modifications += ", ";
                                } else {
                                    firstline = false;
                                }
                                modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                            }
                        }
                        ((DefaultTableModel) xTandemTable.getModel()).addRow(new Object[]{
                                    ++rank,
                                    proteins,
                                    currentAssumption.getPeptide().getSequence(),
                                    modifications,
                                    currentAssumption.getEValue(),
                                    //0 //@TODO: temporarily removed
                                });

                        xtandemPeptideKeys.put(rank, currentAssumption.getPeptide().getKey());
                    }
                }
            }


            // select one of the matches
            if (omssaTable.getRowCount() > 0) {
                omssaTable.setRowSelectionInterval(0, 0);
            } else if (xTandemTable.getRowCount() > 0) {
                xTandemTable.setRowSelectionInterval(0, 0);
            } else if (mascotTable.getRowCount() > 0) {
                mascotTable.setRowSelectionInterval(0, 0);
            }

            peptideShakerJTable.revalidate();
            peptideShakerJTable.repaint();
            mascotTable.revalidate();
            mascotTable.repaint();
            xTandemTable.revalidate();
            xTandemTable.repaint();
            omssaTable.revalidate();
            omssaTable.repaint();

            //update the spectrum
            updateSpectrum();
        }
    }

    private void updateSpectrum() {

        if (spectrumTable.getSelectedRow() != -1) {

            spectrumChartPanel.removeAll();

            try {
                String key = Spectrum.getSpectrumKey((String) fileNamesCmb.getSelectedItem(), (String) spectrumTable.getValueAt(spectrumTable.getSelectedRow(), 1));
                SpectrumMatch spectrumMatch = identification.getSpectrumIdentification().get(key);
                MSnSpectrum currentSpectrum = (MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(spectrumMatch.getKey());
                Precursor precursor = currentSpectrum.getPrecursor();

                if (currentSpectrum.getMzValuesAsArray().length > 0) {

                    SpectrumPanel spectrum = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                            precursor.getMz(), precursor.getCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrum.setDeltaMassWindow(peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance());
                    spectrum.setBorder(null);


                    // omssa annotation (if any)
                    if (omssaTable.getSelectedRow() != -1) {

                        ArrayList<Double> omssaEValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.OMSSA).keySet());
                        Collections.sort(omssaEValues);
                        Peptide currentPeptide = null;
                        int cpt = 0;
                        boolean found = false;
                        for (double eValue : omssaEValues) {
                            for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValue)) {
                                if (cpt == omssaTable.getSelectedRow()) {
                                    currentPeptide = peptideAssumption.getPeptide();
                                    found = true;
                                    break;
                                }
                                cpt++;
                            }
                            if (found) {
                                break;
                            }
                        }
                        SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                                currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
                                currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));

                        // add the spectrum annotations
                        spectrum.setAnnotations(filterAnnotations(spectrumAnnotator.getSpectrumAnnotations(annotations)));
                    }

                    // xtandem annotation (if any)
                    if (xTandemTable.getSelectedRow() != -1) {

                        ArrayList<Double> xTandemEValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.XTANDEM).keySet());
                        Collections.sort(xTandemEValues);
                        Peptide currentPeptide = null;
                        int cpt = 0;
                        boolean found = false;
                        for (double eValue : xTandemEValues) {
                            for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValue)) {
                                if (cpt == xTandemTable.getSelectedRow()) {
                                    currentPeptide = peptideAssumption.getPeptide();
                                    found = true;
                                    break;
                                }
                                cpt++;
                            }
                            if (found) {
                                break;
                            }
                        }
                        SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                                currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
                                currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));

                        // add the spectrum annotations
                        spectrum.setAnnotations(filterAnnotations(spectrumAnnotator.getSpectrumAnnotations(annotations)));
                    }

                    // mascot annotation (if any)
                    if (mascotTable.getSelectedRow() != -1) {

                        ArrayList<Double> mascotEValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.MASCOT).keySet());
                        Collections.sort(mascotEValues);
                        Peptide currentPeptide = null;
                        int cpt = 0;
                        boolean found = false;
                        for (double eValue : mascotEValues) {
                            for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValue)) {
                                if (cpt == mascotTable.getSelectedRow()) {
                                    currentPeptide = peptideAssumption.getPeptide();
                                    found = true;
                                    break;
                                }
                                cpt++;
                            }
                            if (found) {
                                break;
                            }
                        }
                        SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                                currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
                                currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));

                        // add the spectrum annotations
                        spectrum.setAnnotations(filterAnnotations(spectrumAnnotator.getSpectrumAnnotations(annotations)));
                    }

                    spectrumChartPanel.add(spectrum);
                }

            } catch (MzMLUnmarshallerException e) {
                e.printStackTrace();
            }
        }

        spectrumChartPanel.revalidate();
        spectrumChartPanel.repaint();
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
}
