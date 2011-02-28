package eu.isas.peptideshaker.gui;

import eu.isas.peptideshaker.gui.preferencesdialogs.IdentificationPreferencesDialog;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumCollection;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.PsmSpecificMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyMap;
import eu.isas.peptideshaker.gui.preferencesdialogs.AnnotationPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.DigestionPreferencesDialog;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.IdentificationPreferences;
import eu.isas.peptideshaker.utils.Properties;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main frame of the PeptideShaker.
 *
 * @author  Harald Barsnes
 * @author  Marc Vaudel
 */
public class PeptideShakerGUI extends javax.swing.JFrame implements ProgressDialogParent {

    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    /**
     * The xml file containing the enzymes
     */
    private static final String ENZYME_FILE = "conf/peptideshaker_enzymes.xml";
    /**
     * The compomics experiment
     */
    private MsExperiment experiment = null;
    /**
     * The investigated sample
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The specific target/decoy map at the psm level
     */
    private PsmSpecificMap psmMap;
    /**
     * The specific target/decoy map at the peptide level
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The target/decoy map at the protein level
     */
    private TargetDecoyMap proteinMap;
    /**
     * The identification preferences
     */
    private IdentificationPreferences identificationPreferences;
    /**
     * The annotation preferences
     */
    private AnnotationPreferences annotationPreferences = new AnnotationPreferences();
    /**
     * the enzyme used for digestion
     */
    private Enzyme selectedEnzyme;
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(110, 196, 97);
    /**
     * Compomics experiment saver and opener
     */
    private ExperimentIO experimentIO = new ExperimentIO();
    /**
     * A simple progress dialog.
     */
    private static ProgressDialog progressDialog;
    /**
     * If set to true the progress stopped and the simple progress dialog
     * disposed.
     */
    private boolean cancelProgress = false;
    private HashMap<String, String> proteinTableIndexes = new HashMap<String, String>();
    private HashMap<String, String> peptideTableIndexes = new HashMap<String, String>();
    private HashMap<String, String> psmTableIndexes = new HashMap<String, String>();
    private Identification identification;
    private SpectrumCollection spectrumCollection;
    private SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();

    /**
     * The main method used to start PeptideShaker
     * 
     * @param args
     */
    public static void main(String[] args) {

        // update the look and feel after adding the panels
        setLookAndFeel();

        new PeptideShakerGUI();
    }

    /**
     * Creates a new PeptideShaker frame.
     */
    public PeptideShakerGUI() {

        // set up the ErrorLog
        setUpLogFile();

        initComponents();

        // set up the table column properties
        setColumnProperies();

        // disable the Quantification and PTM Analysis tabs for now
        resultsJTabbedPane.setEnabledAt(4, false);
        resultsJTabbedPane.setEnabledAt(5, false);

        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        spectraScrollPane.getViewport().setOpaque(false);
        coverageScrollPane.getViewport().setOpaque(false);

        allProteinsJScrollPane.getViewport().setOpaque(false);
        allPeptidesJScrollPane.getViewport().setOpaque(false);
        allSpectraJScrollPane.getViewport().setOpaque(false);

        proteinTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set the title of the frame and add the icon
        setTitle(this.getTitle() + " " + new Properties().getVersion());
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

        this.setExtendedState(MAXIMIZED_BOTH);

        setLocationRelativeTo(null);
        setVisible(true);

        loadEnzymes();
        setDefaultPreferences();

        // open the OpenDialog
        new OpenDialog(this, true);
    }

    /**
     * Set the properties for the columns in the results tables.
     */
    private void setColumnProperies() {

        allProteinsJTable.getTableHeader().setReorderingAllowed(false);
        allPeptidesJTable.getTableHeader().setReorderingAllowed(false);
        allSpectraJTable.getTableHeader().setReorderingAllowed(false);
        quantificationJTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmsTable.getTableHeader().setReorderingAllowed(false);

        allProteinsJTable.setAutoCreateRowSorter(true);
        allPeptidesJTable.setAutoCreateRowSorter(true);
        allSpectraJTable.setAutoCreateRowSorter(true);
        quantificationJTable.setAutoCreateRowSorter(true);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmsTable.setAutoCreateRowSorter(true);

        allProteinsJTable.getColumn(" ").setMaxWidth(70);
        allPeptidesJTable.getColumn(" ").setMaxWidth(70);
        allSpectraJTable.getColumn(" ").setMaxWidth(70);
        quantificationJTable.getColumn(" ").setMaxWidth(70);

        proteinTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMaxWidth(50);
        psmsTable.getColumn(" ").setMaxWidth(50);

        allProteinsJTable.getColumn("Decoy").setMaxWidth(60);
        allPeptidesJTable.getColumn("Decoy").setMaxWidth(60);
        allSpectraJTable.getColumn("Decoy").setMaxWidth(60);

        allProteinsJTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, 40);
        allProteinsJTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, 40);
        allProteinsJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("p-score").getCellRenderer()).showNumberAndChart(true, 40);
        allProteinsJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("PEP").getCellRenderer()).showNumberAndChart(true, 40);

        allPeptidesJTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, 40);
        allPeptidesJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("p-score").getCellRenderer()).showNumberAndChart(true, 40);
        allPeptidesJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("PEP").getCellRenderer()).showNumberAndChart(true, 40);

        allSpectraJTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mass Error").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("Mascot Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot Score").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("Mascot E-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot E-value").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("OMSSA E-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("OMSSA E-value").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("X!Tandem E-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("X!Tandem E-value").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("p-score").getCellRenderer()).showNumberAndChart(true, 40);
        allSpectraJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("PEP").getCellRenderer()).showNumberAndChart(true, 40);

        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, 40);
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, 40);
        proteinTable.getColumn("emPAI").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumberAndChart(true, 40);
        proteinTable.getColumn("Protein Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Protein Score").getCellRenderer()).showNumberAndChart(true, 40);
        proteinTable.getColumn("Sequence Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Sequence Coverage").getCellRenderer()).showNumberAndChart(true, 40);

        allProteinsJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
        allPeptidesJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
        allSpectraJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
    }

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public String getLastSelectedFolder() {
        return lastSelectedFolder;
    }

    /**
     * Set the last selected folder.
     *
     * @param lastSelectedFolder the folder to set
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gradientPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0, 
                    getBackground().brighter().brighter(), 0, getHeight(),
                    getBackground().darker());

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }            
        }
        ;
        resultsJTabbedPane = new javax.swing.JTabbedPane();
        overviewJPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0, 
                    getBackground().brighter().brighter(), 0, getHeight(),
                    getBackground().darker());

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }            
        }
        ;
        jPanel2 = new javax.swing.JPanel();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        coverageScrollPane = new javax.swing.JScrollPane();
        coverageEditorPane = new javax.swing.JEditorPane();
        jPanel3 = new javax.swing.JPanel();
        peptideScrollPane = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        spectraScrollPane = new javax.swing.JScrollPane();
        psmsTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();
        allProteinsJPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0, 
                    getBackground().brighter().brighter(), 0, getHeight(),
                    getBackground().darker());

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }            
        }
        ;
        jPanel6 = new javax.swing.JPanel();
        allProteinsJScrollPane = new javax.swing.JScrollPane();
        allProteinsJTable = new javax.swing.JTable();
        allPeptidesJPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0, 
                    getBackground().brighter().brighter(), 0, getHeight(),
                    getBackground().darker());

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }            
        }
        ;
        jPanel8 = new javax.swing.JPanel();
        allPeptidesJScrollPane = new javax.swing.JScrollPane();
        allPeptidesJTable = new javax.swing.JTable();
        allSpectraJPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0, 
                    getBackground().brighter().brighter(), 0, getHeight(),
                    getBackground().darker());

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }            
        }
        ;
        jPanel7 = new javax.swing.JPanel();
        allSpectraJScrollPane = new javax.swing.JScrollPane();
        allSpectraJTable = new javax.swing.JTable();
        quantificationJScrollPane = new javax.swing.JScrollPane();
        quantificationJTable = new javax.swing.JTable();
        ptmAnalysisScrollPane = new javax.swing.JScrollPane();
        ptmAnalysisJTable = new javax.swing.JTable();
        menuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        openJMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        identificationOptionsMenu = new javax.swing.JMenuItem();
        digestionOptionMenu = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PeptideShaker");

        gradientPanel.setOpaque(false);

        resultsJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.RIGHT);

        overviewJPanel.setOpaque(false);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        jPanel2.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Sequence Coverage", "emPAI", "#Peptides", "#Spectra", "Protein Score", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Object.class
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
        proteinTable.setOpaque(false);
        proteinTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                proteinTableMouseClicked(evt);
            }
        });
        proteinTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinTableKeyReleased(evt);
            }
        });
        proteinScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Sequence Coverage"));
        jPanel1.setOpaque(false);

        coverageScrollPane.setOpaque(false);

        coverageEditorPane.setContentType("text/html");
        coverageEditorPane.setEditable(false);
        coverageScrollPane.setViewportView(coverageEditorPane);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        jPanel3.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Variable Modifications", "#Spectra", "Peptide Score"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class
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
        peptideTable.setOpaque(false);
        peptideTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptideTableMouseClicked(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide/Spectrum Matches"));
        jPanel4.setOpaque(false);

        spectraScrollPane.setOpaque(false);

        psmsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Modifications", "Precursor Charge", "Precursor Mass Error", "Spectrum File", "Spectrum Title"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class
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
        psmsTable.setOpaque(false);
        psmsTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        psmsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                psmsTableMouseClicked(evt);
            }
        });
        psmsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmsTableKeyReleased(evt);
            }
        });
        spectraScrollPane.setViewportView(psmsTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum / Fragment Ions"));
        jPanel5.setOpaque(false);

        spectrumPanel.setOpaque(false);
        spectrumPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout overviewJPanelLayout = new javax.swing.GroupLayout(overviewJPanel);
        overviewJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(overviewJPanelLayout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        resultsJTabbedPane.addTab("Overview", overviewJPanel);

        allProteinsJPanel.setOpaque(false);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("All Proteins"));
        jPanel6.setOpaque(false);

        allProteinsJScrollPane.setBorder(null);
        allProteinsJScrollPane.setOpaque(false);

        allProteinsJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein", "#Peptides", "#Spectra", "p-score", "PEP", "Decoy"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
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
        allProteinsJTable.setOpaque(false);
        allProteinsJTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        allProteinsJScrollPane.setViewportView(allProteinsJTable);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allProteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allProteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout allProteinsJPanelLayout = new javax.swing.GroupLayout(allProteinsJPanel);
        allProteinsJPanel.setLayout(allProteinsJPanelLayout);
        allProteinsJPanelLayout.setHorizontalGroup(
            allProteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(allProteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        allProteinsJPanelLayout.setVerticalGroup(
            allProteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(allProteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        resultsJTabbedPane.addTab("Proteins", allProteinsJPanel);

        allPeptidesJPanel.setOpaque(false);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("All Peptides"));
        jPanel8.setOpaque(false);

        allPeptidesJScrollPane.setOpaque(false);

        allPeptidesJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Variable Modification(s)", "#Spectra", "p-score", "PEP", "Decoy"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
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
        allPeptidesJTable.setOpaque(false);
        allPeptidesJTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        allPeptidesJScrollPane.setViewportView(allPeptidesJTable);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 854, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel8Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allPeptidesJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 633, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel8Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allPeptidesJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        javax.swing.GroupLayout allPeptidesJPanelLayout = new javax.swing.GroupLayout(allPeptidesJPanel);
        allPeptidesJPanel.setLayout(allPeptidesJPanelLayout);
        allPeptidesJPanelLayout.setHorizontalGroup(
            allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 890, Short.MAX_VALUE)
            .addGroup(allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(allPeptidesJPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        allPeptidesJPanelLayout.setVerticalGroup(
            allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 685, Short.MAX_VALUE)
            .addGroup(allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(allPeptidesJPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        resultsJTabbedPane.addTab("Peptides", allPeptidesJPanel);

        allSpectraJPanel.setOpaque(false);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("All Spectra"));
        jPanel7.setOpaque(false);

        allSpectraJScrollPane.setOpaque(false);

        allSpectraJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Variable Modification(s)", "Charge", "Spectrum", "Spectrum File", "Identification File(s)", "Mass Error", "Mascot Score", "Mascot E-value", "OMSSA E-value", "X!Tandem E-value", "p-score", "PEP", "Decoy"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        allSpectraJTable.setOpaque(false);
        allSpectraJTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        allSpectraJScrollPane.setViewportView(allSpectraJTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 854, Short.MAX_VALUE)
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allSpectraJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 633, Short.MAX_VALUE)
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allSpectraJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        javax.swing.GroupLayout allSpectraJPanelLayout = new javax.swing.GroupLayout(allSpectraJPanel);
        allSpectraJPanel.setLayout(allSpectraJPanelLayout);
        allSpectraJPanelLayout.setHorizontalGroup(
            allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 890, Short.MAX_VALUE)
            .addGroup(allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(allSpectraJPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        allSpectraJPanelLayout.setVerticalGroup(
            allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 685, Short.MAX_VALUE)
            .addGroup(allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(allSpectraJPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        resultsJTabbedPane.addTab("Spectra", allSpectraJPanel);

        quantificationJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        quantificationJTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        quantificationJScrollPane.setViewportView(quantificationJTable);

        resultsJTabbedPane.addTab("Quantification", quantificationJScrollPane);

        ptmAnalysisJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ptmAnalysisJTable.setSelectionBackground(new java.awt.Color(204, 204, 204));
        ptmAnalysisScrollPane.setViewportView(ptmAnalysisJTable);

        resultsJTabbedPane.addTab("PTM Analysis", ptmAnalysisScrollPane);

        javax.swing.GroupLayout gradientPanelLayout = new javax.swing.GroupLayout(gradientPanel);
        gradientPanel.setLayout(gradientPanelLayout);
        gradientPanelLayout.setHorizontalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 978, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 978, Short.MAX_VALUE))
        );
        gradientPanelLayout.setVerticalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 690, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 690, Short.MAX_VALUE))
        );

        menuBar.setBackground(new java.awt.Color(255, 255, 255));

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        openJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openJMenuItem.setMnemonic('O');
        openJMenuItem.setText("Open");
        openJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(openJMenuItem);
        fileJMenu.add(jSeparator2);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save As");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(saveMenuItem);

        exportMenuItem.setMnemonic('E');
        exportMenuItem.setText("Export");
        exportMenuItem.setEnabled(false);
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(exportMenuItem);
        fileJMenu.add(jSeparator1);

        exitJMenuItem.setMnemonic('x');
        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(exitJMenuItem);

        menuBar.add(fileJMenu);

        editMenu.setText("Edit");

        identificationOptionsMenu.setText("Identification Options");
        identificationOptionsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                identificationOptionsMenuActionPerformed(evt);
            }
        });
        editMenu.add(identificationOptionsMenu);

        digestionOptionMenu.setText("Digestion Options");
        digestionOptionMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                digestionOptionMenuActionPerformed(evt);
            }
        });
        editMenu.add(digestionOptionMenu);

        menuBar.add(editMenu);

        viewJMenu.setMnemonic('V');
        viewJMenu.setText("View");

        sparklinesJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sparklinesJCheckBoxMenuItem.setSelected(true);
        sparklinesJCheckBoxMenuItem.setText("JSparklines");
        sparklinesJCheckBoxMenuItem.setToolTipText("View sparklines or the underlying numbers");
        sparklinesJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sparklinesJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(sparklinesJCheckBoxMenuItem);

        annotationPreferencesMenu.setText("Spectrum Annotation Preferences");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        viewJMenu.add(annotationPreferencesMenu);

        menuBar.add(viewJMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        helpJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpJMenuItem.setMnemonic('H');
        helpJMenuItem.setText("Help");
        helpJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpJMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpJMenuItem);

        aboutJMenuItem.setMnemonic('A');
        aboutJMenuItem.setText("About");
        aboutJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutJMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutJMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(gradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(gradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a dialog where the identification files to analyzed are selected.
     *
     * @param evt
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed
//        if (experiment != null) {
//            new OpenDialog(this, true, experiment, sample, replicateNumber);
//        } else {
            new OpenDialog(this, true);
//        }
    }//GEN-LAST:event_openJMenuItemActionPerformed

    /**
     * Closes the PeptideShaker
     *
     * @param evt
     */
    private void exitJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitJMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitJMenuItemActionPerformed

    /**
     * Updates the sparklines to show charts or numbers based on the current
     * selection of the menu item.
     *
     * @param evt
     */
    private void sparklinesJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sparklinesJCheckBoxMenuItemActionPerformed
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("p-score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("PEP").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("p-score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("PEP").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Charge").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mass Error").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot Score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot E-value").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("OMSSA E-value").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("X!Tandem E-value").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("p-score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("PEP").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Sequence Coverage").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Protein Score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

        allProteinsJTable.revalidate();
        allProteinsJTable.repaint();

        allPeptidesJTable.revalidate();
        allPeptidesJTable.repaint();

        allSpectraJTable.revalidate();
        allSpectraJTable.repaint();

        proteinTable.revalidate();
        proteinTable.repaint();
    }//GEN-LAST:event_sparklinesJCheckBoxMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Save As...");
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getCurrentDirectory().getPath();

            cancelProgress = false;

            progressDialog = new ProgressDialog(this, this, true);
            progressDialog.doNothingOnClose();

            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(true);
                    progressDialog.setTitle("Saving. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("SaveThread") {

                @Override
                public void run() {

                    String selectedFile = fileChooser.getSelectedFile().getPath();

                    if (!selectedFile.endsWith(".cps")) {
                        selectedFile += ".cps";
                    }

                    File newFile = new File(selectedFile);
                    int outcome = JOptionPane.YES_OPTION;
                    if (newFile.exists()) {
                        outcome = JOptionPane.showConfirmDialog(progressDialog,
                                "Should " + selectedFile + " be overwritten?", "Selected File Already Exists",
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    }
                    if (outcome != JOptionPane.YES_OPTION) {
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        return;
                    }

                    try {
                        experimentIO.save(newFile, experiment);

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        JOptionPane.showMessageDialog(tempRef, "Identifications were successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        JOptionPane.showMessageDialog(tempRef, "Failed saving the file.", "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMenuItemActionPerformed
        final CsvExporter exporter = new CsvExporter(experiment, sample, replicateNumber, selectedEnzyme);
        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Result Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this, "Save");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getCurrentDirectory().getPath();

            // @TODO: add check for if a file is about to be overwritten

            cancelProgress = false;
            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            progressDialog = new ProgressDialog(this, this, true);
            progressDialog.doNothingOnClose();

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIntermidiate(true);
                    progressDialog.setTitle("Exporting. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {

                @Override
                public void run() {
                    boolean exported = exporter.exportResults(fileChooser.getSelectedFile());
                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    if (exported) {
                        JOptionPane.showMessageDialog(tempRef, "Identifications were successfully exported.", "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(tempRef, "Writing of spectrum file failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_exportMenuItemActionPerformed

    /**
     * Opens the help dialog.
     * 
     * @param evt
     */
    private void helpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJMenuItemActionPerformed
        new HelpWindow(this, getClass().getResource("/helpFiles/PeptideShaker.html"));
    }//GEN-LAST:event_helpJMenuItemActionPerformed

    /**
     * Opens the About dialog.
     *
     * @param evt
     */
    private void aboutJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJMenuItemActionPerformed
        new HelpWindow(this, getClass().getResource("/helpFiles/AboutPeptideShaker.html"));
    }//GEN-LAST:event_aboutJMenuItemActionPerformed

    /**
     * Opens the Identification Preference dialog.
     *
     * @param evt
     */
    private void identificationOptionsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_identificationOptionsMenuActionPerformed
        new IdentificationPreferencesDialog(this, identificationPreferences, true);
    }//GEN-LAST:event_identificationOptionsMenuActionPerformed

    private void proteinTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseClicked

        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.getSelectedColumn();

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // update the peptide selection
            updatedPeptideSelection(row);

            // update the sequence coverage map
            String proteinKey = getProteinKey(row);
            ProteinMatch proteinMatch = identification.getProteinIdentification().get(proteinKey);
            if (proteinMatch.getNProteins() == 1) {
                updateSequenceCoverage(proteinKey);
            } else {
                coverageEditorPane.setText("");
                if (column == 1) {
                    new ProteinInferenceDialog(this, true, proteinMatch, identification, experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSequenceDataBase());
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableMouseClicked

    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // update the peptide selection
            updatedPeptideSelection(row);

            // update the sequence coverage map
            String proteinKey = getProteinKey(row);
            ProteinMatch proteinMatch = identification.getProteinIdentification().get(proteinKey);
            if (proteinMatch.getNProteins() == 1) {
                updateSequenceCoverage(proteinKey);
            } else {
                coverageEditorPane.setText("");
                if (column == 1) {
                    new ProteinInferenceDialog(this, true, proteinMatch, identification, experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSequenceDataBase());
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableKeyReleased

    private void peptideTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseClicked

        int row = peptideTable.rowAtPoint(evt.getPoint());

        if (row != -1) {
            updatePsmSelection(row);
        }
    }//GEN-LAST:event_peptideTableMouseClicked

    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased
        int row = peptideTable.getSelectedRow();

        if (row != -1) {
            updatePsmSelection(row);
        }
    }//GEN-LAST:event_peptideTableKeyReleased

    private void psmsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsTableMouseClicked
        int row = psmsTable.rowAtPoint(evt.getPoint());

        if (row != -1) {
            updateSpectrum(row);
        }
    }//GEN-LAST:event_psmsTableMouseClicked

    private void psmsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmsTableKeyReleased
        int row = psmsTable.getSelectedRow();

        if (row != -1) {
            updateSpectrum(row);
        }
    }//GEN-LAST:event_psmsTableKeyReleased

    private void digestionOptionMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_digestionOptionMenuActionPerformed
        DigestionPreferencesDialog digestionPreferencesDialog = new DigestionPreferencesDialog(this, true, selectedEnzyme, this);
    }//GEN-LAST:event_digestionOptionMenuActionPerformed

    private void annotationPreferencesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationPreferencesMenuActionPerformed
        new AnnotationPreferencesDialog(this, true, annotationPreferences);
    }//GEN-LAST:event_annotationPreferencesMenuActionPerformed

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory
     */
    private void loadEnzymes() {
        try {
            EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
            enzymeFactory.importEnzymes(new File(ENZYME_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossible to load enzyme file.", "Wrong enzyme file.", JOptionPane.ERROR_MESSAGE);
            digestionOptionMenu.setEnabled(false);
            e.printStackTrace();
        }
    }

    private void addProteinRowKey(int row, String proteinKey) {
        proteinTableIndexes.put(getProteinRowKey(row), proteinKey);
    }

    private void addPeptideRowKey(int row, String peptideKey) {
        peptideTableIndexes.put(getPeptideRowKey(row), peptideKey);
    }

    private void addPsmRowKey(int row, String psmKey) {
        psmTableIndexes.put(getPsmRowKey(row), psmKey);
    }

    private String getProteinRowKey(int row) {
        return (String) proteinTable.getValueAt(row, 1);
    }

    private String getPeptideRowKey(int row) {

        String peptideKey = peptideTable.getValueAt(row, 1) + "_" + peptideTable.getValueAt(row, 2);

        if (peptideTable.getValueAt(row, 2) == null) {
            peptideKey = (String) peptideTable.getValueAt(row, 1);
        } else {
            peptideKey = peptideKey.replaceAll(", ", "_");
        }

        return peptideKey;
    }

    private String getPsmRowKey(int row) {
        return psmsTable.getValueAt(row, 5) + "_" + psmsTable.getValueAt(row, 6);
    }

    private void emptyProteinRowKeys() {
        proteinTableIndexes.clear();
    }

    private void emptyPeptideRowKeys() {
        peptideTableIndexes.clear();
    }

    private void emptyPsmsRowKeys() {
        psmTableIndexes.clear();
    }

    private String getProteinKey(int row) {
        return proteinTableIndexes.get(getProteinRowKey(row));
    }

    private String getPeptideKey(int row) {
        return peptideTableIndexes.get(getPeptideRowKey(row));
    }

    private String getPsmKey(int row) {
        return psmTableIndexes.get(getPsmRowKey(row));
    }

    /**
     * Sets the enzyme used for digestion
     * @param selectedEnzyme the selected enzyme
     */
    public void setSelectedEnzyme(Enzyme selectedEnzyme) {
        this.selectedEnzyme = selectedEnzyme;
    }

    private void updateSequenceCoverage(String proteinAccession) {
        SequenceDataBase db = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSequenceDataBase();
        if (db != null) {
            formatProteinSequence(db.getProtein(proteinAccession).getSequence());
        }
    }

    private void updateSpectrum(int row) {

        if (row != -1) {
            String spectrumKey = getPsmKey(row);

            try {
                // These spectra should be MS2 spectra
                MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumCollection.getSpectrum(2, spectrumKey);

                HashSet<Peak> peaks = currentSpectrum.getPeakList();

                if (peaks == null || peaks.isEmpty()) {
                     JOptionPane.showMessageDialog(this, "Peaks lists not imported.", "Peak Lists Error", JOptionPane.INFORMATION_MESSAGE);
                } else {

                    double[] mzValues = new double[peaks.size()];
                    double[] intValues = new double[peaks.size()];

                    Iterator<Peak> iterator = peaks.iterator();

                    int index = 0;

                    while (iterator.hasNext()) {

                        Peak peak = iterator.next();

                        mzValues[index] = peak.mz;
                        intValues[index++] = peak.intensity;
                    }

                    String peptideKey = getPeptideKey(peptideTable.getSelectedRow());
                    Peptide currentPeptide = identification.getPeptideIdentification().get(peptideKey).getTheoreticPeptide();
                    HashMap<Integer, HashMap<Integer, IonMatch>> annotations = spectrumAnnotator.annotateSpectrum(
                            currentPeptide, currentSpectrum, annotationPreferences.getTolerance(), getIntensityLimit(currentSpectrum));

                    // @TODO: verify that the provided informations to the spectrumpanel are correct
                    // @TODO: add ion matches to the spectrum
                    Precursor precursor = currentSpectrum.getPrecursor();
                    SpectrumPanel spectrum =
                            new SpectrumPanel(mzValues, intValues, precursor.getMz(), precursor.getCharge().toString(), "", 50, false, false, false, 2, false);

                    spectrumPanel.removeAll();
                    spectrumPanel.add(spectrum);
                    spectrumPanel.revalidate();
                    spectrumPanel.repaint();
                }

            } catch (MzMLUnmarshallerException e) {
                e.printStackTrace();
            }
        }
    }

    private double getIntensityLimit(MSnSpectrum mSnSpectrum) {
        if (annotationPreferences.shallAnnotateMostIntensePeaks()) {
            ArrayList<Double> intensities = new ArrayList<Double>();
            for (Peak peak : mSnSpectrum.getPeakList()) {
                intensities.add(peak.intensity);
            }
            Collections.sort(intensities);
            int index = 3*(intensities.size()-1)/4;
            return intensities.get(index);
        }
        return 0;
    }

    private void updatePsmSelection(int row) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // update the sequence coverage map
            String proteinKey = getProteinKey(proteinTable.getSelectedRow());
            ProteinMatch proteinMatch = identification.getProteinIdentification().get(proteinKey);
            if (proteinMatch.getNProteins() == 1) {
                updateSequenceCoverage(proteinKey);
            } else {
                coverageEditorPane.setText("");
            }

            String peptideKey = getPeptideKey(row);

            while (psmsTable.getRowCount() > 0) {
                ((DefaultTableModel) psmsTable.getModel()).removeRow(0);
            }
            emptyPsmsRowKeys();

            spectrumPanel.removeAll();
            spectrumPanel.revalidate();
            spectrumPanel.repaint();

            PeptideMatch currentPeptideMatch = identification.getPeptideIdentification().get(peptideKey);
            int index = 1;

            for (SpectrumMatch spectrumMatch : currentPeptideMatch.getSpectrumMatches().values()) {

                PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();

                String modifications = "";

                for (ModificationMatch mod : peptideAssumption.getPeptide().getModificationMatches()) {
                    if (mod.isVariable()) {
                        modifications += mod.getTheoreticPtm().getName() + ", ";
                    }
                }

                if (modifications.length() > 0) {
                    modifications = modifications.substring(0, modifications.length() - 2);
                } else {
                    modifications = null;
                }

                try {
                    String spectrumKey = spectrumMatch.getSpectrumKey();
                    MSnSpectrum spectrum = (MSnSpectrum) spectrumCollection.getSpectrum(2, spectrumKey);

                    ((DefaultTableModel) psmsTable.getModel()).addRow(new Object[]{
                                index,
                                peptideAssumption.getPeptide().getSequence(),
                                modifications,
                                spectrum.getPrecursor().getCharge(),
                                peptideAssumption.getDeltaMass(),
                                spectrum.getFileName(),
                                spectrum.getSpectrumTitle()
                            });
                    addPsmRowKey(index - 1, spectrumKey);
                    index++;

                } catch (MzMLUnmarshallerException e) {
                    e.printStackTrace();
                }
            }

            // select the first spectrum in the table
            if (psmsTable.getRowCount() > 0) {
                psmsTable.setRowSelectionInterval(0, 0);
                psmsTableKeyReleased(null);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Updates the peptide selection.
     *
     * @param row
     */
    private void updatedPeptideSelection(int row) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            while (peptideTable.getRowCount() > 0) {
                ((DefaultTableModel) peptideTable.getModel()).removeRow(0);
            }

            while (psmsTable.getRowCount() > 0) {
                ((DefaultTableModel) psmsTable.getModel()).removeRow(0);
            }

            emptyPsmsRowKeys();
            emptyPeptideRowKeys();

            spectrumPanel.removeAll();
            spectrumPanel.revalidate();
            spectrumPanel.repaint();

            String proteinKey = getProteinKey(row);

            ProteinMatch proteinMatch = identification.getProteinIdentification().get(proteinKey);

            int index = 0;

            for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {

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

                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);

                ((DefaultTableModel) peptideTable.getModel()).addRow(new Object[]{
                            index + 1,
                            peptideMatch.getTheoreticPeptide().getSequence(),
                            modifications,
                            peptideMatch.getSpectrumCount(),
                            probabilities.getPeptideProbabilityScore()
                        });
                addPeptideRowKey(index, peptideMatch.getTheoreticPeptide().getIndex());
                index++;
            }

            // select the first peptide in the table
            if (peptideTable.getRowCount() > 0) {
                peptideTable.setRowSelectionInterval(0, 0);
                peptideTableKeyReleased(null);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * This method sets the information of the project when opened
     * 
     * @param experiment        the experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     */
    public void setProject(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        spectrumCollection = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getSpectrumCollection();
    }

    /**
     * Sets new identification preferences
     * @param identificationPreferences the new identification preferences
     */
    public void setIdentificationPreferences(IdentificationPreferences identificationPreferences) {
        this.identificationPreferences = identificationPreferences;
    }

    /**
     * This method calls the peptide shaker to get fdr results
     */
    public void getFdrResults() {
        PSMaps psMaps = (PSMaps) identification.getUrParam(new PSMaps());
        PeptideShaker peptideShaker = new PeptideShaker(experiment, sample, replicateNumber, psMaps);
        peptideShaker.estimateThresholds(identificationPreferences);
        peptideShaker.validateIdentifications();
    }

    /**
     * Displays the results in the result tables.
     */
    public void displayResults() throws MzMLUnmarshallerException {

        ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        spectrumCollection = proteomicAnalysis.getSpectrumCollection();
        getFdrResults();
        emptyResultTables();
        emptyPeptideRowKeys();
        emptyProteinRowKeys();
        emptyPsmsRowKeys();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        int allProteinsIndex = 1, index = 1, maxPeptides = 0, maxSpectra = 0;
        double sequenceCoverage = 0;
        double emPAI = 0, maxEmPAI = 0, maxProteinScore = 0;
        String description = "";
        SequenceDataBase db = proteomicAnalysis.getSequenceDataBase();
        String proteinKey;

        // add the proteins to the table
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {

            proteinKey = proteinMatch.getKey();
            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);

            try {
                Protein currentProtein = db.getProtein(proteinKey);
                int nPossible = currentProtein.getNPossiblePeptides(selectedEnzyme);
                emPAI = (Math.pow(10, ((double) proteinMatch.getPeptideMatches().size()) / ((double) nPossible)));
                description = currentProtein.getDescription();
                sequenceCoverage = 100 * estimateSequenceCoverage(proteinMatch, currentProtein.getSequence());
            } catch (Exception e) {
                description = "";
                emPAI = 0;
                sequenceCoverage = 0;
            }

            ((DefaultTableModel) allProteinsJTable.getModel()).addRow(new Object[]{
                        allProteinsIndex++,
                        proteinKey,
                        proteinMatch.getPeptideMatches().size(),
                        proteinMatch.getSpectrumCount(),
                        probabilities.getProteinProbabilityScore(),
                        probabilities.getProteinCorrectedProbability(),
                        proteinMatch.isDecoy()
                    });

            // only add non-decoy matches to the overview
            if (!proteinMatch.isDecoy()) {
                ((DefaultTableModel) proteinTable.getModel()).addRow(new Object[]{
                            index,
                            proteinKey,
                            sequenceCoverage,
                            emPAI,
                            proteinMatch.getPeptideMatches().size(),
                            proteinMatch.getSpectrumCount(),
                            probabilities.getProteinProbabilityScore(),
                            description});
                addProteinRowKey(index - 1, proteinKey);
                index++;
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

            if (maxProteinScore < probabilities.getProteinProbabilityScore()) {
                maxProteinScore = probabilities.getProteinProbabilityScore();
            }
        }

        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);

        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).setMaxValue(maxEmPAI);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Protein Score").getCellRenderer()).setMaxValue(maxProteinScore);

        index = 1;
        maxSpectra = 1;

        // add the peptides to the table
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {

            String accessionNumbers = "";

            for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                accessionNumbers += protein.getAccession() + ", ";
            }

            accessionNumbers = accessionNumbers.substring(0, accessionNumbers.length() - 2);

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

            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);


            ((DefaultTableModel) allPeptidesJTable.getModel()).addRow(new Object[]{
                        index++,
                        accessionNumbers,
                        peptideMatch.getTheoreticPeptide().getSequence(),
                        modifications,
                        peptideMatch.getSpectrumMatches().size(),
                        probabilities.getPeptideProbabilityScore(),
                        probabilities.getPeptideProbability(),
                        peptideMatch.isDecoy()
                    });

            if (maxSpectra < peptideMatch.getSpectrumMatches().size()) {
                maxSpectra = peptideMatch.getSpectrumMatches().size();
            }
        }

        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);


        index = 1;
        int maxCharge = 0;
        double maxMassError = 0;
        double maxMascotScore = 0.0;
        double maxMascotEValue = 0.0;
        double maxOmssaEValue = 0.0;
        double maxXTandemEValue = 0.0;
        double maxPScore = 1;
        double maxPEP = 1;

        // add the spectra to the table
        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {

            Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();

            String accessionNumbers = "";

            for (Protein protein : bestAssumption.getParentProteins()) {
                accessionNumbers += protein.getAccession() + ", ";
            }

            accessionNumbers = accessionNumbers.substring(0, accessionNumbers.length() - 2);

            String modifications = "";

            for (ModificationMatch mod : bestAssumption.getModificationMatches()) {
                if (mod.isVariable()) {
                    modifications += mod.getTheoreticPtm().getName() + ", ";
                }
            }

            if (modifications.length() > 0) {
                modifications = modifications.substring(0, modifications.length() - 2);
            } else {
                modifications = null;
            }

            String assumptions = "";

            for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    assumptions += assumption.getFile() + ", ";
                }
            }

            if (assumptions.length() > 0) {
                assumptions = assumptions.substring(0, assumptions.length() - 2);
            } else {
                assumptions = null;
            }

            Double mascotScore = null;

            PeptideAssumption assumption = spectrumMatch.getFirstHit(Advocate.MASCOT);
            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    MascotScore score = (MascotScore) assumption.getUrParam(new MascotScore(0));
                    mascotScore = score.getScore();
                }
            }

            Double mascotEValue = null;

            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    mascotEValue = assumption.getEValue();
                }
            }

            Double omssaEValue = null;

            assumption = spectrumMatch.getFirstHit(Advocate.OMSSA);
            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    omssaEValue = assumption.getEValue();
                }
            }

            Double xTandemEValue = null;

            assumption = spectrumMatch.getFirstHit(Advocate.XTANDEM);
            if (assumption != null) {
                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                    xTandemEValue = assumption.getEValue();
                }
            }

            PSParameter probabilities = new PSParameter();
            probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);
            String spectrumKey = spectrumMatch.getSpectrumKey();
            MSnSpectrum spectrum = (MSnSpectrum) spectrumCollection.getSpectrum(2, spectrumKey);

            ((DefaultTableModel) allSpectraJTable.getModel()).addRow(new Object[]{
                        index++,
                        accessionNumbers,
                        bestAssumption.getSequence(),
                        modifications,
                        spectrum.getPrecursor().getCharge().value,
                        spectrum.getSpectrumTitle(),
                        spectrum.getFileName(),
                        assumptions,
                        spectrumMatch.getBestAssumption().getDeltaMass(),
                        mascotScore,
                        mascotEValue,
                        omssaEValue,
                        xTandemEValue,
                        probabilities.getSpectrumProbabilityScore(),
                        probabilities.getSpectrumProbability(),
                        spectrumMatch.getBestAssumption().isDecoy()
                    });

            if (maxCharge < spectrum.getPrecursor().getCharge().value) {
                maxCharge = spectrum.getPrecursor().getCharge().value;
            }

            if (maxMassError < spectrumMatch.getBestAssumption().getDeltaMass()) {
                maxMassError = spectrumMatch.getBestAssumption().getDeltaMass();
            }

            if (mascotScore != null && maxMascotScore < mascotScore) {
                maxMascotScore = mascotScore;
            }

            if (mascotEValue != null && maxMascotEValue < mascotEValue) {
                maxMascotEValue = mascotEValue;
            }

            if (omssaEValue != null && maxOmssaEValue < omssaEValue) {
                maxOmssaEValue = omssaEValue;
            }

            if (xTandemEValue != null && maxXTandemEValue < xTandemEValue) {
                maxXTandemEValue = xTandemEValue;
            }
        }

        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Charge").getCellRenderer()).setMaxValue(maxCharge);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mass Error").getCellRenderer()).setMaxValue(maxMassError);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot Score").getCellRenderer()).setMaxValue(maxMascotScore);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot E-value").getCellRenderer()).setMaxValue(maxMascotEValue);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("OMSSA E-value").getCellRenderer()).setMaxValue(maxOmssaEValue);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("X!Tandem E-value").getCellRenderer()).setMaxValue(maxXTandemEValue);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("p-score").getCellRenderer()).setMaxValue(maxPScore);
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("PEP").getCellRenderer()).setMaxValue(maxPEP);

        // enable the save and export menu items
        saveMenuItem.setEnabled(true);
        exportMenuItem.setEnabled(true);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Clear the result tables.
     */
    private void emptyResultTables() {
        while (allProteinsJTable.getRowCount() > 0) {
            ((DefaultTableModel) allProteinsJTable.getModel()).removeRow(0);
        }

        while (allPeptidesJTable.getRowCount() > 0) {
            ((DefaultTableModel) allPeptidesJTable.getModel()).removeRow(0);
        }

        while (allSpectraJTable.getRowCount() > 0) {
            ((DefaultTableModel) allSpectraJTable.getModel()).removeRow(0);
        }

        while (quantificationJTable.getRowCount() > 0) {
            ((DefaultTableModel) quantificationJTable.getModel()).removeRow(0);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JPanel allPeptidesJPanel;
    private javax.swing.JScrollPane allPeptidesJScrollPane;
    private javax.swing.JTable allPeptidesJTable;
    private javax.swing.JPanel allProteinsJPanel;
    private javax.swing.JScrollPane allProteinsJScrollPane;
    private javax.swing.JTable allProteinsJTable;
    private javax.swing.JPanel allSpectraJPanel;
    private javax.swing.JScrollPane allSpectraJScrollPane;
    private javax.swing.JTable allSpectraJTable;
    private javax.swing.JMenuItem annotationPreferencesMenu;
    private javax.swing.JEditorPane coverageEditorPane;
    private javax.swing.JScrollPane coverageScrollPane;
    private javax.swing.JMenuItem digestionOptionMenu;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JPanel gradientPanel;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem identificationOptionsMenu;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JTable psmsTable;
    private javax.swing.JTable ptmAnalysisJTable;
    private javax.swing.JScrollPane ptmAnalysisScrollPane;
    private javax.swing.JScrollPane quantificationJScrollPane;
    private javax.swing.JTable quantificationJTable;
    private javax.swing.JTabbedPane resultsJTabbedPane;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JScrollPane spectraScrollPane;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JMenu viewJMenu;
    // End of variables declaration//GEN-END:variables

    /**
     * Check if a newer version of reporter is available.
     *
     * @param currentVersion the version number of the currently running reporter
     */
    private static void checkForNewVersion(String currentVersion) {

        try {
            boolean deprecatedOrDeleted = false;
            URL downloadPage = new URL(
                    "http://code.google.com/p/peptide-shaker/downloads/detail?name=PeptideShaker-"
                    + currentVersion + ".zip");
            int respons = ((java.net.HttpURLConnection) downloadPage.openConnection()).getResponseCode();

            // 404 means that the file no longer exists, which means that
            // the running version is no longer available for download,
            // which again means that a never version is available.
            if (respons == 404) {
                deprecatedOrDeleted = true;
            } else {

                // also need to check if the available running version has been
                // deprecated (but not deleted)
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(downloadPage.openStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null && !deprecatedOrDeleted) {
                    if (inputLine.lastIndexOf("Deprecated") != -1
                            && inputLine.lastIndexOf("Deprecated Downloads") == -1
                            && inputLine.lastIndexOf("Deprecated downloads") == -1) {
                        deprecatedOrDeleted = true;
                    }
                }

                in.close();
            }

            // informs the user about an updated version of the tool, unless the user
            // is running a beta version
            if (deprecatedOrDeleted && currentVersion.lastIndexOf("beta") == -1) {
                int option = JOptionPane.showConfirmDialog(null,
                        "A newer version of PeptideShaker is available.\n"
                        + "Do you want to upgrade?",
                        "Upgrade Available",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com/");
                    System.exit(0);
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {
        if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = getJarFilePath() + "/conf/PeptideShakerLog.log";

                File file = new File(path);
                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    file.createNewFile();

                    FileWriter w = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(w);

                    bw.close();
                    w.close();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occured when trying to create the PeptideShaker Log.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    private String getJarFilePath() {
        String path = this.getClass().getResource("PeptideShakerGUI.class").getPath();

        if (path.lastIndexOf("/PeptideShaker-") != -1) {
            path = path.substring(5, path.lastIndexOf("/PeptideShaker-"));
            path = path.replace("%20", " ");
        } else {
            path = ".";
        }

        return path;
    }

    /**
     * Sets the look and feel of the PeptideShaker.
     * <p/>
     * Note that the GUI has been created with the following look and feel
     * in mind. If using a different look and feel you might need to tweak the GUI
     * to get the best appearance.
     */
    private static void setLookAndFeel() {

        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception, i.e. use default look and feel
        }
    }

    @Override
    public void cancelProgress() {
        cancelProgress = true;
    }

    /**
     * Set the default preferences.
     * TODO: Not sure that this ought to be hard coded
     */
    private void setDefaultPreferences() {
        identificationPreferences = new IdentificationPreferences(0.01, 0.01, 0.01, true, false);
        setSelectedEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
    }

    /**
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * @return the replicateNumber
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * Formats the protein sequence such that both the covered parts of the sequence
     * and the peptide selected in the peptide table is highlighted.
     */
    public void formatProteinSequence(String cleanSequence) {

        int selectedPeptideStart = -1;
        int selectedPeptideEnd = -1;

        // find the start end end indices for the currently selected peptide, if any
        if (peptideTable.getSelectedRow() != -1) {

            String peptideKey = getPeptideKey(peptideTable.getSelectedRow());
            String peptideSequence = identification.getPeptideIdentification().get(peptideKey).getTheoreticPeptide().getSequence();

            // @TODO: account for peptide redundancies
            selectedPeptideStart = cleanSequence.lastIndexOf(peptideSequence) + 1;
            selectedPeptideEnd = selectedPeptideStart + peptideSequence.length() - 1;
        }

        // an array containing the coverage index for each residue
        int[] coverage = new int[cleanSequence.length() + 1];

        // iterate the peptide table and store the coverage for each peptide
        for (int i = 0; i < peptideTable.getRowCount(); i++) {

            String peptideKey = getPeptideKey(i);
            String peptideSequence = identification.getPeptideIdentification().get(peptideKey).getTheoreticPeptide().getSequence();

            // @TODO: account for peptide redundancies
            int peptideTempStart = cleanSequence.lastIndexOf(peptideSequence) + 1;
            int peptideTempEnd = peptideTempStart + peptideSequence.length();

            for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                coverage[j]++;
            }
        }

        String sequenceTable = "", currentCellSequence = "";
        boolean selectedPeptide = false, coveredPeptide = false;
        double sequenceCoverage = 0;

        // iterate the coverage table and create the formatted sequence string
        for (int i = 1; i < coverage.length; i++) {

            // add indices per 50 residues
            if (i % 50 == 1 || i == 1) {
                sequenceTable += "</tr><tr><td height='20'><font size=2><a name=\"" + i + ".\"></a>" + i + ".</td>";

                int currentCharIndex = i;

                while (currentCharIndex + 10 < cleanSequence.length() && currentCharIndex + 10 < (i + 50)) {
                    sequenceTable += "<td height='20'><font size=2><a name=\""
                            + (currentCharIndex + 10) + ".\"></a>" + (currentCharIndex + 10) + ".</td>";
                    currentCharIndex += 10;
                }

                sequenceTable += "</tr><tr>";
            }

            // check if the current residues is covered
            if (coverage[i] > 0) {
                sequenceCoverage++;
                coveredPeptide = true;
            } else {
                coveredPeptide = false;
            }

            // check if the current residue is contained in the selected peptide
            if (i == selectedPeptideStart) {
                selectedPeptide = true;
            } else if (i == selectedPeptideEnd + 1) {
                selectedPeptide = false;
            }

            // highlight the covered and selected peptides
            if (selectedPeptide) {
                currentCellSequence += "<font color=red>" + cleanSequence.charAt(i - 1) + "</font>";
            } else if (coveredPeptide) {
                currentCellSequence += "<font color=blue>" + cleanSequence.charAt(i - 1) + "</font>";
            } else {
                currentCellSequence += "<font color=black>" + cleanSequence.charAt(i - 1) + "</font>";
            }

            // add the sequence to the formatted sequence
            if (i % 10 == 0) {
                sequenceTable += "<td><tt>" + currentCellSequence + "</tt></td>";
                currentCellSequence = "";
            }
        }

        // add remaining tags and complete the formatted sequence
        sequenceTable += "<td><tt>" + currentCellSequence + "</tt></td></table><font color=black>";
        String formattedSequence = "<html><body><table cellspacing='2'>" + sequenceTable + "</html></body>";

        // display the formatted sequence
        coverageEditorPane.setText(formattedSequence);
        coverageEditorPane.updateUI();

        // make sure that the currently selected peptide is visible
        if (selectedPeptideStart != -1) {
            coverageEditorPane.scrollToReference((selectedPeptideStart - selectedPeptideStart % 10 + 1) + ".");
        } else {
            coverageEditorPane.setCaretPosition(0);
        }
    }

    private double estimateSequenceCoverage(ProteinMatch proteinMatch, String sequence) {

        // an array containing the coverage index for each residue
        int[] coverage = new int[sequence.length() + 1];
        int peptideTempStart, peptideTempEnd;

        // iterate the peptide table and store the coverage for each peptide
        for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {

            String peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();

            // @TODO: account for peptide redundancies
            peptideTempStart = sequence.lastIndexOf(peptideSequence);
            peptideTempEnd = peptideTempStart + peptideSequence.length();

            for (int j = peptideTempStart; j <= peptideTempEnd; j++) {
                coverage[j] = 1;
            }
        }
        double covered = 0.0;
        for (int aa : coverage) {
            covered += aa;
        }
        return covered / ((double) sequence.length());
    }

    public void updateAnnotationPreferences(AnnotationPreferences annotationPreferences) {
        this.annotationPreferences = annotationPreferences;
    }
}
