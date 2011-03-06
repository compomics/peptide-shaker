package eu.isas.peptideshaker.gui;

import eu.isas.peptideshaker.gui.preferencesdialogs.IdentificationPreferencesDialog;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Atom;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.FragmentFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
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
import com.compomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.CsvExporter;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.PsmSpecificMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyMap;
import eu.isas.peptideshaker.gui.preferencesdialogs.AnnotationPreferencesDialog;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.IdentificationPreferences;
import eu.isas.peptideshaker.utils.AlignedTableCellRenderer;
import eu.isas.peptideshaker.utils.FragmentIonTableCellRenderer;
import eu.isas.peptideshaker.utils.Properties;
import java.awt.Color;
import java.awt.FontMetrics;
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
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
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
     * The current spectrum annotations.
     */
    private Vector<DefaultSpectrumAnnotation> currentAnnotations;
    /**
     * The current spectrum panel.
     */
    private SpectrumPanel spectrum;

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
        fragmentIonsJScrollPane.getViewport().setOpaque(false);

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
        fragmentIonsJTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmsTable.getTableHeader().setReorderingAllowed(false);
        fragmentIonsJTable.getTableHeader().setReorderingAllowed(false);

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

        fragmentIonsJTable.getColumn(" ").setMaxWidth(40);
        fragmentIonsJTable.getColumn("  ").setMaxWidth(40);
        fragmentIonsJTable.getColumn("AA").setMaxWidth(40);

        int labelWidth = 35;

        allProteinsJTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allProteinsJTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allProteinsJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("p-score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allProteinsJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allProteinsJTable.getColumn("PEP").getCellRenderer()).showNumberAndChart(true, labelWidth);

        allPeptidesJTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allPeptidesJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("p-score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allPeptidesJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allPeptidesJTable.getColumn("PEP").getCellRenderer()).showNumberAndChart(true, labelWidth);

        allSpectraJTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mass Error").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("Mascot Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot Score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("Mascot E-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("Mascot E-value").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("OMSSA E-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("OMSSA E-value").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("X!Tandem E-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("X!Tandem E-value").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("p-score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("p-score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        allSpectraJTable.getColumn("PEP").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) allSpectraJTable.getColumn("PEP").getCellRenderer()).showNumberAndChart(true, labelWidth);

        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("emPAI").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, labelWidth);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).setMinimumChartValue(5d);

        allProteinsJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
        allPeptidesJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));
        allSpectraJTable.getColumn("Decoy").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), null));

        // centrally align the columns in the fragment ions table
        fragmentIonsJTable.getColumn(" ").setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));
        fragmentIonsJTable.getColumn("  ").setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));
        fragmentIonsJTable.getColumn("AA").setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));

        // centrally align the column headers in the fragment ions table
        TableCellRenderer renderer = fragmentIonsJTable.getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);
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
        proteinsJPanel = new javax.swing.JPanel();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable = new javax.swing.JTable();
        sequenceCoverageJPanel = new javax.swing.JPanel();
        coverageScrollPane = new javax.swing.JScrollPane();
        coverageEditorPane = new javax.swing.JEditorPane();
        peptidesPsmSpectrumFragmentIonsJSplitPane = new javax.swing.JSplitPane();
        peptidesPsmJSplitPane = new javax.swing.JSplitPane();
        peptidesJPanel = new javax.swing.JPanel();
        peptideScrollPane = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        psmJPanel = new javax.swing.JPanel();
        spectraScrollPane = new javax.swing.JScrollPane();
        psmsTable = new javax.swing.JTable();
        spectrumFragmentIonJSplitPane = new javax.swing.JSplitPane();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        fragmentIonsLabel = new javax.swing.JLabel();
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
        fragmentIonJPanel = new javax.swing.JPanel();
        fragmentIonsJScrollPane = new javax.swing.JScrollPane();
        fragmentIonsJTable = new javax.swing.JTable();
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
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        identificationOptionsMenu = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        fragmentIonsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PeptideShaker");
        setMinimumSize(new java.awt.Dimension(1240, 800));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        gradientPanel.setOpaque(false);

        resultsJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.RIGHT);

        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));

        proteinsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsJPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description", "Coverage", "emPAI", "#Peptides", "#Spectra", "Score"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class
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

        javax.swing.GroupLayout proteinsJPanelLayout = new javax.swing.GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsJPanelLayout);
        proteinsJPanelLayout.setHorizontalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinsJPanelLayout.setVerticalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE)
                .addContainerGap())
        );

        sequenceCoverageJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sequence Coverage"));
        sequenceCoverageJPanel.setOpaque(false);

        coverageScrollPane.setOpaque(false);

        coverageEditorPane.setContentType("text/html");
        coverageEditorPane.setEditable(false);
        coverageEditorPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                coverageEditorPaneComponentResized(evt);
            }
        });
        coverageScrollPane.setViewportView(coverageEditorPane);

        javax.swing.GroupLayout sequenceCoverageJPanelLayout = new javax.swing.GroupLayout(sequenceCoverageJPanel);
        sequenceCoverageJPanel.setLayout(sequenceCoverageJPanelLayout);
        sequenceCoverageJPanelLayout.setHorizontalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
                .addContainerGap())
        );
        sequenceCoverageJPanelLayout.setVerticalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmSpectrumFragmentIonsJSplitPane.setBorder(null);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(450);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setResizeWeight(0.5);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setOpaque(false);

        peptidesPsmJSplitPane.setBorder(null);
        peptidesPsmJSplitPane.setDividerLocation(170);
        peptidesPsmJSplitPane.setDividerSize(0);
        peptidesPsmJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptidesPsmJSplitPane.setResizeWeight(0.5);
        peptidesPsmJSplitPane.setOpaque(false);

        peptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidesJPanel.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Modifications", "#Spectra", "Score"
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

        javax.swing.GroupLayout peptidesJPanelLayout = new javax.swing.GroupLayout(peptidesJPanel);
        peptidesJPanel.setLayout(peptidesJPanelLayout);
        peptidesJPanelLayout.setHorizontalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesJPanelLayout.setVerticalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmJSplitPane.setTopComponent(peptidesJPanel);

        psmJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches"));
        psmJPanel.setOpaque(false);

        spectraScrollPane.setOpaque(false);

        psmsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Modifications", "Charge", "Mass Error", "File", "Title"
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

        javax.swing.GroupLayout psmJPanelLayout = new javax.swing.GroupLayout(psmJPanel);
        psmJPanel.setLayout(psmJPanelLayout);
        psmJPanelLayout.setHorizontalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmJPanelLayout.setVerticalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmJSplitPane.setRightComponent(psmJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setLeftComponent(peptidesPsmJSplitPane);

        spectrumFragmentIonJSplitPane.setBorder(null);
        spectrumFragmentIonJSplitPane.setDividerLocation(200);
        spectrumFragmentIonJSplitPane.setDividerSize(0);
        spectrumFragmentIonJSplitPane.setResizeWeight(0.5);
        spectrumFragmentIonJSplitPane.setMinimumSize(new java.awt.Dimension(0, 0));
        spectrumFragmentIonJSplitPane.setOpaque(false);
        spectrumFragmentIonJSplitPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                spectrumFragmentIonJSplitPaneComponentResized(evt);
            }
        });

        spectrumJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum"));
        spectrumJPanel.setOpaque(false);

        spectrumPanel.setOpaque(false);
        spectrumPanel.setLayout(new java.awt.BorderLayout());

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);

        fragmentIonsLabel.setFont(fragmentIonsLabel.getFont().deriveFont(fragmentIonsLabel.getFont().getStyle() | java.awt.Font.BOLD));
        fragmentIonsLabel.setText(" Ions: ");
        spectrumJToolBar.add(fragmentIonsLabel);

        aIonToggleButton.setText("a");
        aIonToggleButton.setFocusable(false);
        aIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aIonToggleButton.setMinimumSize(new java.awt.Dimension(25, 21));
        aIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        bIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        cIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        xIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        yIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        zIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        h2oToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        nh3ToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        otherToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        oneChargeToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        twoChargesToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
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
        moreThanTwoChargesToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        moreThanTwoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moreThanTwoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(moreThanTwoChargesToggleButton);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spectrumPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                    .addComponent(spectrumJToolBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 171, Short.MAX_VALUE))
                .addContainerGap())
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        spectrumFragmentIonJSplitPane.setLeftComponent(spectrumJPanel);

        fragmentIonJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fragment Ions"));
        fragmentIonJPanel.setOpaque(false);

        fragmentIonsJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "b+", "b++", "AA", "y++", "y+", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class
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
        fragmentIonsJScrollPane.setViewportView(fragmentIonsJTable);

        javax.swing.GroupLayout fragmentIonJPanelLayout = new javax.swing.GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                .addContainerGap())
        );

        spectrumFragmentIonJSplitPane.setRightComponent(fragmentIonJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setRightComponent(spectrumFragmentIonJSplitPane);

        javax.swing.GroupLayout overviewJPanelLayout = new javax.swing.GroupLayout(overviewJPanel);
        overviewJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(peptidesPsmSpectrumFragmentIonsJSplitPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 899, Short.MAX_VALUE)
                    .addComponent(proteinsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sequenceCoverageJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptidesPsmSpectrumFragmentIonsJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sequenceCoverageJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        resultsJTabbedPane.addTab("Overview", overviewJPanel);

        allProteinsJPanel.setOpaque(false);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("All Proteins"));
        jPanel6.setOpaque(false);

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
        allProteinsJScrollPane.setViewportView(allProteinsJTable);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allProteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allProteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 703, Short.MAX_VALUE)
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
        allPeptidesJScrollPane.setViewportView(allPeptidesJTable);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 887, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel8Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allPeptidesJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 725, Short.MAX_VALUE)
            .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel8Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allPeptidesJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 703, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        javax.swing.GroupLayout allPeptidesJPanelLayout = new javax.swing.GroupLayout(allPeptidesJPanel);
        allPeptidesJPanel.setLayout(allPeptidesJPanelLayout);
        allPeptidesJPanelLayout.setHorizontalGroup(
            allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 919, Short.MAX_VALUE)
            .addGroup(allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(allPeptidesJPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        allPeptidesJPanelLayout.setVerticalGroup(
            allPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 774, Short.MAX_VALUE)
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
        allSpectraJScrollPane.setViewportView(allSpectraJTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 887, Short.MAX_VALUE)
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allSpectraJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 725, Short.MAX_VALUE)
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(allSpectraJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 703, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        javax.swing.GroupLayout allSpectraJPanelLayout = new javax.swing.GroupLayout(allSpectraJPanel);
        allSpectraJPanel.setLayout(allSpectraJPanelLayout);
        allSpectraJPanelLayout.setHorizontalGroup(
            allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 919, Short.MAX_VALUE)
            .addGroup(allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(allSpectraJPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        allSpectraJPanelLayout.setVerticalGroup(
            allSpectraJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 774, Short.MAX_VALUE)
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
        ptmAnalysisScrollPane.setViewportView(ptmAnalysisJTable);

        resultsJTabbedPane.addTab("PTM Analysis", ptmAnalysisScrollPane);

        javax.swing.GroupLayout gradientPanelLayout = new javax.swing.GroupLayout(gradientPanel);
        gradientPanel.setLayout(gradientPanelLayout);
        gradientPanelLayout.setHorizontalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1007, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1007, Short.MAX_VALUE))
        );
        gradientPanelLayout.setVerticalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 779, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(resultsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 779, Short.MAX_VALUE))
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

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        annotationPreferencesMenu.setText("Spectrum Annotations");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        editMenu.add(annotationPreferencesMenu);

        identificationOptionsMenu.setText("Identification Options");
        identificationOptionsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                identificationOptionsMenuActionPerformed(evt);
            }
        });
        editMenu.add(identificationOptionsMenu);

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

        fragmentIonsJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        fragmentIonsJCheckBoxMenuItem.setMnemonic('F');
        fragmentIonsJCheckBoxMenuItem.setText("Fragment Ion Table");
        fragmentIonsJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fragmentIonsJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(fragmentIonsJCheckBoxMenuItem);

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

        // @TODO: the code below did not work and had to be removed. but should be fixed and put back in?

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

        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!sparklinesJCheckBoxMenuItem.isSelected());

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
    }//GEN-LAST:event_proteinTableMouseClicked

    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased
        proteinTableMouseClicked(null);
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

    private void annotationPreferencesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationPreferencesMenuActionPerformed
        new AnnotationPreferencesDialog(this, true, annotationPreferences);
    }//GEN-LAST:event_annotationPreferencesMenuActionPerformed

    /**
     * Makes sure that the sequence coverage area is rescaled to fit the new size
     * of the frame.
     *
     * @param evt
     */
    private void coverageEditorPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_coverageEditorPaneComponentResized
        proteinTableMouseClicked(null);
    }//GEN-LAST:event_coverageEditorPaneComponentResized

    /**
     * Makes sure that the spectrum fragment table or the spectrum panel is shown.
     *
     * @param evt
     */
    private void fragmentIonsJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fragmentIonsJCheckBoxMenuItemActionPerformed

        if (fragmentIonsJCheckBoxMenuItem.isSelected()) {
            spectrumFragmentIonJSplitPane.setDividerLocation(0);
        } else {
            spectrumFragmentIonJSplitPane.setDividerLocation(Integer.MAX_VALUE);
        }
    }//GEN-LAST:event_fragmentIonsJCheckBoxMenuItemActionPerformed

    /**
     * Set the default divider locations of the split panes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(peptidesPsmSpectrumFragmentIonsJSplitPane.getWidth() / 2);
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
                fragmentIonsJCheckBoxMenuItemActionPerformed(null);
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void aIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonToggleButtonActionPerformed
        if (spectrum != null) {
            // update the ion coverage annotations
            spectrum.setAnnotations(filterAnnotations(currentAnnotations));
            spectrumPanel.validate();
            spectrumPanel.repaint();
        }
    }//GEN-LAST:event_aIonToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void bIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_bIonToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void cIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_cIonToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void xIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_xIonToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void yIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_yIonToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void zIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_zIonToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void h2oToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_h2oToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void nh3ToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3ToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_nh3ToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void otherToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_otherToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void oneChargeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_oneChargeToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void twoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_twoChargesToggleButtonActionPerformed

    /**
     * Update the annotations in the spectrum panel.
     *
     * @param evt
     */
    private void moreThanTwoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
    }//GEN-LAST:event_moreThanTwoChargesToggleButtonActionPerformed

    private void spectrumFragmentIonJSplitPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_spectrumFragmentIonJSplitPaneComponentResized
        formComponentResized(null);
    }//GEN-LAST:event_spectrumFragmentIonJSplitPaneComponentResized

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory
     */
    private void loadEnzymes() {
        try {
            EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
            enzymeFactory.importEnzymes(new File(ENZYME_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossible to load enzyme file.", "Wrong enzyme file.", JOptionPane.ERROR_MESSAGE);
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

    /**
     * Returns the current enzyme.
     *
     * @return the current enzyme
     */
    public Enzyme getSelectedEnzyme() {
        return selectedEnzyme;
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

                    // add the data to the spectrum panel
                    Precursor precursor = currentSpectrum.getPrecursor();
                    spectrum = new SpectrumPanel(
                            mzValues, intValues, precursor.getMz(), precursor.getCharge().toString(),
                            "", 50, false, false, false, 2, false);

                    // get the spectrum annotations
                    String peptideKey = getPeptideKey(peptideTable.getSelectedRow());
                    Peptide currentPeptide = identification.getPeptideIdentification().get(peptideKey).getTheoreticPeptide();
                    HashMap<String, HashMap<Integer, IonMatch>> annotations = spectrumAnnotator.annotateSpectrum(
                            currentPeptide, currentSpectrum, annotationPreferences.getTolerance(), getIntensityLimit(currentSpectrum));

                    // the fragment ions annotations
                    currentAnnotations = new Vector();

                    Iterator<String> ionTypeIterator = annotations.keySet().iterator();

                    while (ionTypeIterator.hasNext()) {
                        String ionType = ionTypeIterator.next();

                        HashMap<Integer, IonMatch> chargeMap = annotations.get(ionType);
                        Iterator<Integer> chargeIterator = chargeMap.keySet().iterator();

                        while (chargeIterator.hasNext()) {
                            Integer currentCharge = chargeIterator.next();
                            IonMatch ionMatch = chargeMap.get(currentCharge);

                            PeptideFragmentIon fragmentIon = ((PeptideFragmentIon) ionMatch.ion);

                            // set up the peak annotation
                            String annotation = fragmentIon.getIonType();

                            // add fragment ion number
                            if (!fragmentIon.getIonType().equalsIgnoreCase("Prec")
                                    && !fragmentIon.getIonType().equalsIgnoreCase("i")
                                    && !fragmentIon.getIonType().equalsIgnoreCase("Prec-loss")) {
                                annotation += fragmentIon.getNumber();
                            }

                            // add charge and any neutral losses
                            annotation += getChargeAsFormattedString(currentCharge) // @TODO: should be possible to use the getChargeAsFormattedString in Charge??
                                    + fragmentIon.getNeutralLoss();

                            currentAnnotations.add(new DefaultSpectrumAnnotation(ionMatch.peak.mz, ionMatch.getError(),
                                    SpectrumPanel.determineColorOfPeak(fragmentIon.getIonType() + fragmentIon.getNeutralLoss()),
                                    annotation));
                        }
                    }

                    // add the spectrum annotations
                    spectrum.setAnnotations(filterAnnotations(currentAnnotations));

                    spectrumPanel.removeAll();
                    spectrumPanel.add(spectrum);
                    spectrumPanel.revalidate();
                    spectrumPanel.repaint();


                    // clear the fragment ion table
                    while (fragmentIonsJTable.getRowCount() > 0) {
                        ((DefaultTableModel) fragmentIonsJTable.getModel()).removeRow(0);
                    }

                    String peptideSequence = currentPeptide.getSequence();

                    // add the peptide sequence and numbers to the table
                    for (int i = 0; i < peptideSequence.length(); i++) {
                        ((DefaultTableModel) fragmentIonsJTable.getModel()).addRow(new Object[]{
                                    (i + 1),
                                    null,
                                    null,
                                    peptideSequence.charAt(i),
                                    null,
                                    null,
                                    peptideSequence.length() - i
                                });
                    }

                    // get all singly and doubly charged  b and y fragmentions for the peptide
                    FragmentFactory fragmentFactory = FragmentFactory.getInstance();
                    ArrayList<PeptideFragmentIon> fragmentIons = fragmentFactory.getFragmentIons(currentPeptide);

                    // add the theoretical masses to the table
                    for (PeptideFragmentIon fragmentIon : fragmentIons) {

                        if (fragmentIon.getType() == PeptideFragmentIon.B_ION || fragmentIon.getType() == PeptideFragmentIon.Y_ION) {
                            double fragmentMzChargeOne = (fragmentIon.theoreticMass + 1 * Atom.H.mass) / 1;
                            double fragmentMzChargeTwo = (fragmentIon.theoreticMass + 2 * Atom.H.mass) / 2;

                            int fragmentNumber = fragmentIon.getNumber();

                            if (fragmentIon.getType() == PeptideFragmentIon.B_ION) {
                                fragmentIonsJTable.setValueAt(fragmentMzChargeOne, fragmentNumber - 1, fragmentIonsJTable.getColumn("b+").getModelIndex());
                                fragmentIonsJTable.setValueAt(fragmentMzChargeTwo, fragmentNumber - 1, fragmentIonsJTable.getColumn("b++").getModelIndex());
                            } else {
                                fragmentIonsJTable.setValueAt(fragmentMzChargeOne, peptideSequence.length() - fragmentNumber, fragmentIonsJTable.getColumn("y+").getModelIndex());
                                fragmentIonsJTable.setValueAt(fragmentMzChargeTwo, peptideSequence.length() - fragmentNumber, fragmentIonsJTable.getColumn("y++").getModelIndex());
                            }
                        }
                    }

                    // see which ions are detected in the spectrum
                    ionTypeIterator = annotations.keySet().iterator();

                    ArrayList<Integer> bIonsSinglyCharged = new ArrayList<Integer>();
                    ArrayList<Integer> bIonsDoublyCharged = new ArrayList<Integer>();
                    ArrayList<Integer> yIonsSinglyCharged = new ArrayList<Integer>();
                    ArrayList<Integer> yIonsDoublyCharged = new ArrayList<Integer>();

                    while (ionTypeIterator.hasNext()) {
                        String ionType = ionTypeIterator.next();

                        HashMap<Integer, IonMatch> chargeMap = annotations.get(ionType);
                        Iterator<Integer> chargeIterator = chargeMap.keySet().iterator();

                        while (chargeIterator.hasNext()) {
                            Integer currentCharge = chargeIterator.next();

                            if (currentCharge == 1 || currentCharge == 2) {
                                IonMatch ionMatch = chargeMap.get(currentCharge);

                                PeptideFragmentIon fragmentIon = ((PeptideFragmentIon) ionMatch.ion);

                                if (fragmentIon.getType() == PeptideFragmentIon.B_ION || fragmentIon.getType() == PeptideFragmentIon.Y_ION) {
                                    int fragmentNumber = fragmentIon.getNumber();

                                    if (fragmentIon.getType() == PeptideFragmentIon.B_ION) {
                                        if (currentCharge == 1) {
                                            bIonsSinglyCharged.add(fragmentNumber - 1);
                                        } else {
                                            bIonsDoublyCharged.add(fragmentNumber - 1);
                                        }
                                    } else {
                                        if (currentCharge == 1) {
                                            yIonsSinglyCharged.add(peptideSequence.length() - fragmentNumber);
                                        } else {
                                            yIonsDoublyCharged.add(peptideSequence.length() - fragmentNumber);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // highlight the detected fragment ions in the table
                    fragmentIonsJTable.getColumn("b+").setCellRenderer(new FragmentIonTableCellRenderer(bIonsSinglyCharged, Color.BLUE, Color.WHITE));
                    fragmentIonsJTable.getColumn("b++").setCellRenderer(new FragmentIonTableCellRenderer(bIonsDoublyCharged, Color.BLUE, Color.WHITE));
                    fragmentIonsJTable.getColumn("y+").setCellRenderer(new FragmentIonTableCellRenderer(yIonsSinglyCharged, Color.RED, Color.WHITE));
                    fragmentIonsJTable.getColumn("y++").setCellRenderer(new FragmentIonTableCellRenderer(yIonsDoublyCharged, Color.RED, Color.WHITE));
                }
            } catch (MzMLUnmarshallerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Filters the annotations and returns the annotations matching the currently selected list.
     *
     * @param annotations the annotations to be filtered
     * @return the filtered annotations
     */
    private Vector<DefaultSpectrumAnnotation> filterAnnotations(Vector<DefaultSpectrumAnnotation> annotations) {

        Vector<DefaultSpectrumAnnotation> filteredAnnotations = new Vector();

        for (int i = 0; i < annotations.size(); i++) {
            String currentLabel = annotations.get(i).getLabel();

            boolean useAnnotation = true;

            // check ion type
            if (currentLabel.startsWith("a")) {
                if (!aIonToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("b")) {
                if (!bIonToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("c")) {
                if (!cIonToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("x")) {
                if (!xIonToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("y")) {
                if (!yIonToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            } else if (currentLabel.startsWith("z")) {
                if (!zIonToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            } else {
                if (!otherToggleButton.isSelected()) {
                    useAnnotation = false;
                }
            }

            // check neutral losses
            if (useAnnotation) {
                if (currentLabel.lastIndexOf("-H2O") != -1 || currentLabel.lastIndexOf("-H20") != -1) {
                    if (!h2oToggleButton.isSelected()) {
                        useAnnotation = false;
                    }
                }

                if (currentLabel.lastIndexOf("-NH3") != -1) {
                    if (!nh3ToggleButton.isSelected()) {
                        useAnnotation = false;
                    }
                }
            }

            // check ion charge
            if (useAnnotation) {
                if (currentLabel.lastIndexOf("+") == -1) {

                    // test needed to be able to show ions in the "other" group
                    if (currentLabel.startsWith("a") || currentLabel.startsWith("b") || currentLabel.startsWith("c")
                            || currentLabel.startsWith("x") || currentLabel.startsWith("y") || currentLabel.startsWith("z")) {
                        if (!oneChargeToggleButton.isSelected()) {
                            useAnnotation = false;
                        }
                    }
                } else if (currentLabel.lastIndexOf("+++") != -1) {
                    if (!moreThanTwoChargesToggleButton.isSelected()) {
                        useAnnotation = false;
                    }
                } else if (currentLabel.lastIndexOf("++") != -1) {
                    if (!twoChargesToggleButton.isSelected()) {
                        useAnnotation = false;
                    }
                }
            }

            if (useAnnotation) {
                filteredAnnotations.add(annotations.get(i));
            }
        }

        return filteredAnnotations;
    }

    /**
     * Returns the charge as a string of +. One for each charge.
     * A charge of 1 however returns the empty string.
     *
     * @return  the charge as a string of +
     */
    public static String getChargeAsFormattedString(int charge) {

        String temp = "";

        for (int i = 0; i < charge; i++) {
            temp += "+";
        }

        if (charge == 1) {
            temp = "";
        }

        return temp;
    }

    private double getIntensityLimit(MSnSpectrum mSnSpectrum) {
        if (annotationPreferences.shallAnnotateMostIntensePeaks()) {
            ArrayList<Double> intensities = new ArrayList<Double>();
            for (Peak peak : mSnSpectrum.getPeakList()) {
                intensities.add(peak.intensity);
            }
            Collections.sort(intensities);
            int index = 3 * (intensities.size() - 1) / 4;
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
                            description,
                            sequenceCoverage,
                            emPAI,
                            proteinMatch.getPeptideMatches().size(),
                            proteinMatch.getSpectrumCount(),
                            probabilities.getProteinProbabilityScore()});
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
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(maxProteinScore);

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

        // select the first row
        if (proteinTable.getRowCount() > 0) {
            proteinTable.setRowSelectionInterval(0, 0);
            proteinTableMouseClicked(null);
            proteinTable.requestFocus();
        }

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
    private javax.swing.JToggleButton aIonToggleButton;
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
    private javax.swing.JToggleButton bIonToggleButton;
    private javax.swing.JToggleButton cIonToggleButton;
    private javax.swing.JEditorPane coverageEditorPane;
    private javax.swing.JScrollPane coverageScrollPane;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JPanel fragmentIonJPanel;
    private javax.swing.JCheckBoxMenuItem fragmentIonsJCheckBoxMenuItem;
    private javax.swing.JScrollPane fragmentIonsJScrollPane;
    private javax.swing.JTable fragmentIonsJTable;
    private javax.swing.JLabel fragmentIonsLabel;
    private javax.swing.JPanel gradientPanel;
    private javax.swing.JToggleButton h2oToggleButton;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem identificationOptionsMenu;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JToggleButton moreThanTwoChargesToggleButton;
    private javax.swing.JToggleButton nh3ToggleButton;
    private javax.swing.JToggleButton oneChargeToggleButton;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JToggleButton otherToggleButton;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JSplitPane peptidesPsmJSplitPane;
    private javax.swing.JSplitPane peptidesPsmSpectrumFragmentIonsJSplitPane;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JPanel psmJPanel;
    private javax.swing.JTable psmsTable;
    private javax.swing.JTable ptmAnalysisJTable;
    private javax.swing.JScrollPane ptmAnalysisScrollPane;
    private javax.swing.JScrollPane quantificationJScrollPane;
    private javax.swing.JTable quantificationJTable;
    private javax.swing.JTabbedPane resultsJTabbedPane;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JPanel sequenceCoverageJPanel;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JScrollPane spectraScrollPane;
    private javax.swing.JSplitPane spectrumFragmentIonJSplitPane;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JToggleButton twoChargesToggleButton;
    private javax.swing.JMenu viewJMenu;
    private javax.swing.JToggleButton xIonToggleButton;
    private javax.swing.JToggleButton yIonToggleButton;
    private javax.swing.JToggleButton zIonToggleButton;
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
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
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

        // see how many amino acids we have room for
        FontMetrics fm = coverageEditorPane.getGraphics().getFontMetrics();
        double temp = coverageEditorPane.getWidth() / (fm.stringWidth("W"));
        int numberOfAminoAcidsPerRow = (int) temp / 10;
        numberOfAminoAcidsPerRow *= 10;
        numberOfAminoAcidsPerRow += 10;

        ArrayList<Integer> referenceMarkers = new ArrayList<Integer>();

        // iterate the coverage table and create the formatted sequence string
        for (int i = 1; i < coverage.length; i++) {

            // add residue number and line break
            if (i % numberOfAminoAcidsPerRow == 1 || i == 1) {
                sequenceTable += "</tr><tr><td><font color=black><a name=\"" + i + "\">" + i + "</a></font></td>";
                referenceMarkers.add(i);
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

            boolean referenceMarkerFound = false;

            for (int i = 0; i < referenceMarkers.size() - 1 && !referenceMarkerFound; i++) {
                if (selectedPeptideStart >= referenceMarkers.get(i) && selectedPeptideStart < referenceMarkers.get(i + 1)) {
                    coverageEditorPane.scrollToReference(referenceMarkers.get(i).toString());
                    referenceMarkerFound = true;
                }
            }

            if (!referenceMarkerFound) {
                coverageEditorPane.scrollToReference(referenceMarkers.get(referenceMarkers.size() - 1).toString());
            }

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
