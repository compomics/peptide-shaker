package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.preferencesdialogs.AnnotationPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.FeaturesPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.FollowupPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.SearchPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.SpectrumCountingPreferencesDialog;
import eu.isas.peptideshaker.gui.tabpanels.AnnotationPanel;
import eu.isas.peptideshaker.gui.tabpanels.OverviewPanel;
import eu.isas.peptideshaker.gui.tabpanels.ProteinStructurePanel;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.gui.tabpanels.QCPanel;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.gui.tabpanels.StatsPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSSettings;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.SearchParameters;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main frame of the PeptideShaker.
 *
 * @author  Harald Barsnes
 * @author  Marc Vaudel
 */
public class PeptideShakerGUI extends javax.swing.JFrame implements ProgressDialogParent {

    /**
     * The Overview tab index.
     */
    private final int OVER_VIEW_TAB_INDEX = 0;
    /**
     * The SpectrumID tab index.
     */
    private final int SPECTRUM_ID_TAB_INDEX = 1;
    /**
     * The Modifications tab index.
     */
    private final int MODIFICATIONS_TAB_INDEX = 2;
    /**
     * The Structures tab index.
     */
    private final int STRUCTURES_TAB_INDEX = 3;
    /**
     * The Annotation tab index.
     */
    private final int ANNOTATION_TAB_INDEX = 4;
    /**
     * The Validation tab index.
     */
    private final int VALIDATION_TAB_INDEX = 5;
    /**
     * The decimal format use for the score and confidence columns.
     */
    private DecimalFormat scoreAndConfidenceDecimalFormat = new DecimalFormat("0");

    /**
     * Turns of the gradient painting for the bar charts.
     */
    static {
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());
    }
    /**
     * If true the relative error (ppm) is used instead of the absolute error (Da).
     */
    private boolean useRelativeError = false;
    /**
     * The currently selected protein index.
     */
    private int selectedProteinIndex = -1;
    /**
     * The currently selected peptide index.
     */
    private int selectedPeptideIndex = -1;
    /**
     * If true, the latest changes have been saved.
     */
    private boolean dataSaved = true;
    /**
     * Ignore or consider the non-applied threshold change.
     */
    private boolean ignoreThresholdUpdate = false;
    /**
     * Ignore or consider the non-applied PEP window change.
     */
    private boolean ignorePepWindowUpdate = false;
    /**
     * The current protein filter values.
     */
    private String[] currentProteinFilterValues = {"", "", "", "", "", "", "", ""};
    /**
     * The current settings for the radio buttons for the protein filters.
     */
    private Integer[] currrentProteinFilterRadioButtonSelections = {0, 0, 0, 0, 0, 0};
    /**
     * The current protein inference filter selection.
     */
    private int currentProteinInferenceFilterSelection = 5;
    /**
     * The scaling value for the bubbles.
     */
    private double bubbleScale = 1;
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
     * modification file
     */
    private final String MODIFICATIONS_FILE = "conf/peptideshaker_mods.xml";
    /**
     * user modification file
     */
    private final String USER_MODIFICATIONS_FILE = "conf/peptideshaker_usermods.xml";
    /**
     * File containing the modification profile. By default default.psm in the conf folder.
     */
    private File profileFile = new File("conf/default.psm");
    /**
     * The compomics PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The compomics enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
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
     * The annotation preferences
     */
    private AnnotationPreferences annotationPreferences = new AnnotationPreferences();
    /**
     * The spectrum counting preferences
     */
    private SpectrumCountingPreferences spectrumCountingPreferences = new SpectrumCountingPreferences();
    /**
     * The parameters of the search
     */
    private SearchParameters searchParameters = new SearchParameters();
    /**
     * Compomics experiment saver and opener
     */
    private ExperimentIO experimentIO = new ExperimentIO();
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * The identification to display
     */
    private Identification identification;
    /**
     * The overview panel
     */
    private OverviewPanel overviewPanel;
    /**
     * The statistics panel
     */
    private StatsPanel statsPanel;
    /**
     * The PTM panel
     */
    private PtmPanel ptmPanel;
    /**
     * The Annotation panel
     */
    private AnnotationPanel annotationPanel;
    /**
     * The spectrum panel
     */
    private SpectrumIdentificationPanel spectrumIdentificationPanel;
    /**
     * The protein structure panel.
     */
    private ProteinStructurePanel proteinStructurePanel;
    /**
     * The QC panel
     */
    private QCPanel qcPanel;
    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The label with for the numbers in the jsparklines columns.
     */
    private int labelWidth = 50;
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(110, 196, 97);
    /**
     * The color to use for the HTML tags for the selected rows, in HTML color code.
     */
    private String selectedRowHtmlTagFontColor = "#FFFFFF";
    /**
     * The color to use for the HTML tags for the rows that are not selected, in HTML color code.
     */
    private String notSelectedRowHtmlTagFontColor = "#0101DF";
    /**
     * Boolean indicating whether spectra should be displayed or not
     */
    private boolean displaySpectrum = true;
    /**
     * The spectrum annotator
     */
    private SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
    /**
     * List of caught exceptions
     */
    private ArrayList<String> exceptionCaught = new ArrayList<String>();

    /**
     * The main method used to start PeptideShaker
     * 
     * @param args
     */
    public static void main(String[] args) {

        // update the look and feel after adding the panels
        UtilitiesGUIDefaults.setLookAndFeel();

        new PeptideShakerGUI();
    }

    /**
     * Creates a new PeptideShaker frame.
     */
    public PeptideShakerGUI() {

        // check for new version
        checkForNewVersion(getVersion());

        // set up the ErrorLog
        setUpLogFile();

        overviewPanel = new OverviewPanel(this);
        statsPanel = new StatsPanel(this);
        ptmPanel = new PtmPanel(this);
        spectrumIdentificationPanel = new SpectrumIdentificationPanel(this);
        proteinStructurePanel = new ProteinStructurePanel(this);
        annotationPanel = new AnnotationPanel(this);

        initComponents();

        setUpPanels(true);
        repaintPanels();

        // set the title
        setFrameTitle(null);

        // set the title of the frame and add the icon
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

        this.setExtendedState(MAXIMIZED_BOTH);

        loadEnzymes();
        loadModifications();
        setDefaultPreferences();

        setLocationRelativeTo(null);
        setVisible(true);

        // open the welcome dialog
        new WelcomeDialog(this, true);
    }

    /**
     * Add the experiment title to the frame title.
     *
     * @param experimentTitle the title to add
     */
    public void setFrameTitle(String experimentTitle) {

        if (experimentTitle != null) {
            this.setTitle("PeptideShaker " + getVersion() + " - " + experimentTitle);
        } else {
            this.setTitle("PeptideShaker " + getVersion());
        }
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

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("peptide-shaker.properties");
            p.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p.getProperty("peptide-shaker.version");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        modelButtonGroup = new javax.swing.ButtonGroup();
        gradientPanel = new javax.swing.JPanel();
        allTabsJTabbedPane = new javax.swing.JTabbedPane();
        overviewJPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        ptmJPanel = new javax.swing.JPanel();
        proteinStructureJPanel = new javax.swing.JPanel();
        annotationsJPanel = new javax.swing.JPanel();
        statsJPanel = new javax.swing.JPanel();
        qcJPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        newJMenuItem = new javax.swing.JMenuItem();
        openJMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        searchParametersMenu = new javax.swing.JMenuItem();
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        spectrumCountingMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        bubbleScaleJMenuItem = new javax.swing.JMenuItem();
        errorPlotTypeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        filterMenu = new javax.swing.JMenu();
        proteinFilterJMenuItem = new javax.swing.JMenuItem();
        exportJMenu = new javax.swing.JMenu();
        graphicsJMenu = new javax.swing.JMenu();
        spectrumOverviewJMenuItem = new javax.swing.JMenuItem();
        bubblePlotJMenuItem = new javax.swing.JMenuItem();
        spectrumSpectrumIdJMenuItem = new javax.swing.JMenuItem();
        spectrumModificationsJMenuItem = new javax.swing.JMenuItem();
        identificationFeaturesMenu = new javax.swing.JMenuItem();
        followUpAnalysisMenu = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        overViewTabViewMenu = new javax.swing.JMenu();
        proteinsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        peptidesAndPsmsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        spectrumJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sequenceCoverageJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        structureTabViewMenu = new javax.swing.JMenu();
        modelSpinJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        modelTypeMenu = new javax.swing.JMenu();
        ribbonJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        backboneJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        scoresJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        separatorsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PeptideShaker");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(1280, 750));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        gradientPanel.setBackground(new java.awt.Color(255, 255, 255));
        gradientPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

        allTabsJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.RIGHT);
        allTabsJTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                allTabsJTabbedPaneStateChanged(evt);
            }
        });

        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));
        overviewJPanel.setLayout(new javax.swing.BoxLayout(overviewJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Overview", overviewJPanel);

        spectrumJPanel.setLayout(new javax.swing.BoxLayout(spectrumJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Spectrum IDs", spectrumJPanel);

        ptmJPanel.setOpaque(false);
        ptmJPanel.setLayout(new javax.swing.BoxLayout(ptmJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Modifications", ptmJPanel);

        proteinStructureJPanel.setOpaque(false);
        proteinStructureJPanel.setLayout(new javax.swing.BoxLayout(proteinStructureJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("3D Structures", proteinStructureJPanel);

        annotationsJPanel.setOpaque(false);
        annotationsJPanel.setLayout(new javax.swing.BoxLayout(annotationsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Annotation", annotationsJPanel);

        statsJPanel.setOpaque(false);
        statsJPanel.setLayout(new javax.swing.BoxLayout(statsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Validation", statsJPanel);

        qcJPanel.setLayout(new javax.swing.BoxLayout(qcJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("QC Plots", qcJPanel);

        javax.swing.GroupLayout gradientPanelLayout = new javax.swing.GroupLayout(gradientPanel);
        gradientPanel.setLayout(gradientPanelLayout);
        gradientPanelLayout.setHorizontalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1260, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(allTabsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1260, Short.MAX_VALUE))
        );
        gradientPanelLayout.setVerticalGroup(
            gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 781, Short.MAX_VALUE)
            .addGroup(gradientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(allTabsJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 781, Short.MAX_VALUE))
        );

        menuBar.setBackground(new java.awt.Color(255, 255, 255));

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        newJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newJMenuItem.setMnemonic('N');
        newJMenuItem.setText("New Project");
        newJMenuItem.setToolTipText("Create a new PeptideShaker project");
        newJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(newJMenuItem);

        openJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openJMenuItem.setMnemonic('O');
        openJMenuItem.setText("Open Project");
        openJMenuItem.setToolTipText("Open an existing PeptideShaker project");
        openJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(openJMenuItem);
        fileJMenu.add(jSeparator2);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save As...");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(saveMenuItem);
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

        searchParametersMenu.setText("Search Parameters");
        searchParametersMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchParametersMenuActionPerformed(evt);
            }
        });
        editMenu.add(searchParametersMenu);

        annotationPreferencesMenu.setText("Spectrum Annotations");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        editMenu.add(annotationPreferencesMenu);

        spectrumCountingMenuItem.setText("Spectrum Counting");
        spectrumCountingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumCountingMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(spectrumCountingMenuItem);
        editMenu.add(jSeparator5);

        bubbleScaleJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        bubbleScaleJMenuItem.setText("Bubble Plot Scale");
        bubbleScaleJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bubbleScaleJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(bubbleScaleJMenuItem);

        errorPlotTypeCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        errorPlotTypeCheckBoxMenuItem.setSelected(true);
        errorPlotTypeCheckBoxMenuItem.setText("Absolute Mass Error Plot");
        errorPlotTypeCheckBoxMenuItem.setToolTipText("Plot the mass error in Da or ppm ");
        errorPlotTypeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorPlotTypeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(errorPlotTypeCheckBoxMenuItem);

        menuBar.add(editMenu);

        filterMenu.setMnemonic('L');
        filterMenu.setText("Filter");

        proteinFilterJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        proteinFilterJMenuItem.setText("Proteins");
        proteinFilterJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinFilterJMenuItemActionPerformed(evt);
            }
        });
        filterMenu.add(proteinFilterJMenuItem);

        menuBar.add(filterMenu);

        exportJMenu.setMnemonic('x');
        exportJMenu.setText("Export");

        graphicsJMenu.setMnemonic('g');
        graphicsJMenu.setText("Graphics");
        graphicsJMenu.setToolTipText("Export a graphics element (i.e., spectrum or plot)");
        graphicsJMenu.setEnabled(false);

        spectrumOverviewJMenuItem.setText("Spectrum");
        spectrumOverviewJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumOverviewJMenuItemActionPerformed(evt);
            }
        });
        graphicsJMenu.add(spectrumOverviewJMenuItem);

        bubblePlotJMenuItem.setText("Bubble Plot");
        bubblePlotJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bubblePlotJMenuItemActionPerformed(evt);
            }
        });
        graphicsJMenu.add(bubblePlotJMenuItem);

        spectrumSpectrumIdJMenuItem.setText("Spectrum");
        spectrumSpectrumIdJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumSpectrumIdJMenuItemActionPerformed(evt);
            }
        });
        graphicsJMenu.add(spectrumSpectrumIdJMenuItem);

        spectrumModificationsJMenuItem.setText("Spectrum");
        spectrumModificationsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumModificationsJMenuItemActionPerformed(evt);
            }
        });
        graphicsJMenu.add(spectrumModificationsJMenuItem);

        exportJMenu.add(graphicsJMenu);

        identificationFeaturesMenu.setText("Identification Features");
        identificationFeaturesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                identificationFeaturesMenuActionPerformed(evt);
            }
        });
        exportJMenu.add(identificationFeaturesMenu);

        followUpAnalysisMenu.setText("Follow-up Analysis");
        followUpAnalysisMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                followUpAnalysisMenuActionPerformed(evt);
            }
        });
        exportJMenu.add(followUpAnalysisMenu);

        menuBar.add(exportJMenu);

        viewJMenu.setMnemonic('V');
        viewJMenu.setText("View");

        overViewTabViewMenu.setText("Overview");

        proteinsJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        proteinsJCheckBoxMenuItem.setMnemonic('P');
        proteinsJCheckBoxMenuItem.setSelected(true);
        proteinsJCheckBoxMenuItem.setText("Proteins");
        proteinsJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinsJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        overViewTabViewMenu.add(proteinsJCheckBoxMenuItem);

        peptidesAndPsmsJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        peptidesAndPsmsJCheckBoxMenuItem.setMnemonic('E');
        peptidesAndPsmsJCheckBoxMenuItem.setSelected(true);
        peptidesAndPsmsJCheckBoxMenuItem.setText("Peptides & PSMs");
        peptidesAndPsmsJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidesAndPsmsJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        overViewTabViewMenu.add(peptidesAndPsmsJCheckBoxMenuItem);

        spectrumJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        spectrumJCheckBoxMenuItem.setMnemonic('S');
        spectrumJCheckBoxMenuItem.setSelected(true);
        spectrumJCheckBoxMenuItem.setText("Spectrum");
        spectrumJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        overViewTabViewMenu.add(spectrumJCheckBoxMenuItem);

        sequenceCoverageJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sequenceCoverageJCheckBoxMenuItem.setMnemonic('C');
        sequenceCoverageJCheckBoxMenuItem.setSelected(true);
        sequenceCoverageJCheckBoxMenuItem.setText("Sequence Coverage");
        sequenceCoverageJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequenceCoverageJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        overViewTabViewMenu.add(sequenceCoverageJCheckBoxMenuItem);

        viewJMenu.add(overViewTabViewMenu);

        structureTabViewMenu.setText("3D Structure");

        modelSpinJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        modelSpinJCheckBoxMenuItem.setMnemonic('R');
        modelSpinJCheckBoxMenuItem.setSelected(true);
        modelSpinJCheckBoxMenuItem.setText("Rotate");
        modelSpinJCheckBoxMenuItem.setToolTipText("Rotate the protein model");
        modelSpinJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modelSpinJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        structureTabViewMenu.add(modelSpinJCheckBoxMenuItem);

        modelTypeMenu.setText("Model");

        modelButtonGroup.add(ribbonJRadioButtonMenuItem);
        ribbonJRadioButtonMenuItem.setSelected(true);
        ribbonJRadioButtonMenuItem.setText("Ribbon");
        ribbonJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ribbonJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        modelTypeMenu.add(ribbonJRadioButtonMenuItem);

        modelButtonGroup.add(backboneJRadioButtonMenuItem);
        backboneJRadioButtonMenuItem.setText("Backbone");
        backboneJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backboneJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        modelTypeMenu.add(backboneJRadioButtonMenuItem);

        structureTabViewMenu.add(modelTypeMenu);

        viewJMenu.add(structureTabViewMenu);
        viewJMenu.add(jSeparator3);

        sparklinesJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sparklinesJCheckBoxMenuItem.setSelected(true);
        sparklinesJCheckBoxMenuItem.setText("JSparklines");
        sparklinesJCheckBoxMenuItem.setToolTipText("View sparklines or the underlying numbers");
        sparklinesJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sparklinesJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(sparklinesJCheckBoxMenuItem);

        scoresJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        scoresJCheckBoxMenuItem.setMnemonic('c');
        scoresJCheckBoxMenuItem.setText("Scores");
        scoresJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scoresJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(scoresJCheckBoxMenuItem);
        viewJMenu.add(jSeparator4);

        separatorsCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        separatorsCheckBoxMenuItem.setText("Separators");
        separatorsCheckBoxMenuItem.setToolTipText("Enable resizing of the components ");
        separatorsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                separatorsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(separatorsCheckBoxMenuItem);

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
            .addComponent(gradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 781, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a dialog where the identification files to analyzed are selected.
     *
     * @param evt
     */
    private void newJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newJMenuItemActionPerformed

        if (!dataSaved) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + experiment.getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                OpenDialog openDialog = new OpenDialog(this, true);
                openDialog.setVisible(true);
            } else if (value == JOptionPane.CANCEL_OPTION) {
                // do nothing
            } else { // no option
                OpenDialog openDialog = new OpenDialog(this, true);
                openDialog.setVisible(true);
            }
        } else {
            OpenDialog openDialog = new OpenDialog(this, true);
            openDialog.setVisible(true);
        }
    }//GEN-LAST:event_newJMenuItemActionPerformed

    /**
     * Closes the PeptideShaker
     *
     * @param evt
     */
    private void exitJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitJMenuItemActionPerformed
        close();
    }//GEN-LAST:event_exitJMenuItemActionPerformed

    /**
     * Updates the sparklines to show charts or numbers based on the current
     * selection of the menu item.
     *
     * @param evt
     */
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Save As...");
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("cps") || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "(Compomics Peptide Shaker format) *.cps";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getSelectedFile().getAbsolutePath();

            progressDialog = new ProgressDialogX(this, this, true);
            progressDialog.doNothingOnClose();

            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIndeterminate(true);
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
                        // change the peptide shaker icon to a "waiting version"
                        tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
                        experiment.addUrParam(new PSSettings(searchParameters, annotationPreferences, spectrumCountingPreferences));

                        String folderPath = selectedFile.substring(0, selectedFile.lastIndexOf("."));
                        File newFolder = new File(folderPath + "_cps");
                        if (newFolder.exists()) {
                            String[] fileList = newFolder.list();
                            progressDialog.setMax(fileList.length);
                            progressDialog.setTitle("Deleting old matches.");
                            File toDelete;
                            int cpt = 0;
                            for (String fileName : fileList) {
                                toDelete = new File(newFolder.getPath(), fileName);
                                toDelete.delete();
                                progressDialog.setValue(++cpt);
                            }
                            progressDialog.setIndeterminate(true);
                        }
                        newFolder.mkdir();

                        identification.save(newFolder, progressDialog);

                        progressDialog.setValue(99);
                        progressDialog.setMax(100);
                        experimentIO.save(newFile, experiment);

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        // return the peptide shaker icon to the standard version
                        tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                        JOptionPane.showMessageDialog(tempRef, "Identifications were successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                        dataSaved = true;
                    } catch (Exception e) {

                        // return the peptide shaker icon to the standard version
                        tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        JOptionPane.showMessageDialog(tempRef, "Failed saving the file.", "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    /**
     * Opens the Help dialog.
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
    private void annotationPreferencesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationPreferencesMenuActionPerformed
        new AnnotationPreferencesDialog(this);
    }//GEN-LAST:event_annotationPreferencesMenuActionPerformed

    /**
     * Resize the overview panel when the frame resizes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
        statsPanel.updateSeparators();
    }//GEN-LAST:event_formComponentResized

    /**
     * Open the SearchPreferencesDialog.
     *
     * @param evt
     */
    private void searchParametersMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchParametersMenuActionPerformed
        new SearchPreferencesDialog(this);
    }//GEN-LAST:event_searchParametersMenuActionPerformed

    /**
     * Show or hide the sparklines.
     *
     * @param evt
     */
    private void sparklinesJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sparklinesJCheckBoxMenuItemActionPerformed
        overviewPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        ptmPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        spectrumIdentificationPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        proteinStructurePanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
}//GEN-LAST:event_sparklinesJCheckBoxMenuItemActionPerformed

    /**
     * Resize the overview panel when the frame resizes.
     *
     * @param evt
     */
    private void sequenceCoverageJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequenceCoverageJCheckBoxMenuItemActionPerformed
        overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
}//GEN-LAST:event_sequenceCoverageJCheckBoxMenuItemActionPerformed

    /**
     * Resize the overview panel when the frame resizes.
     *
     * @param evt
     */
    private void spectrumJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumJCheckBoxMenuItemActionPerformed

        // the below code is a bit more complicated than the other resize panel
        // options due to the resizing didn't work otherwise...

        final boolean showProteins = proteinsJCheckBoxMenuItem.isSelected();
        final boolean showPeptidesAndPsms = peptidesAndPsmsJCheckBoxMenuItem.isSelected();
        final boolean showCoverage = sequenceCoverageJCheckBoxMenuItem.isSelected();
        final boolean showSpectrum = spectrumJCheckBoxMenuItem.isSelected();

        if (!showPeptidesAndPsms && !showSpectrum) {
            overviewPanel.setDisplayOptions(showProteins, true, showCoverage, false);
            overviewPanel.updateSeparators();

            overviewPanel.setDisplayOptions(showProteins, false, showCoverage, false);
            overviewPanel.updateSeparators();
        } else if (!showPeptidesAndPsms && showSpectrum) {
            overviewPanel.setDisplayOptions(showProteins, true, showCoverage, false);
            overviewPanel.updateSeparators();

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    overviewPanel.setDisplayOptions(showProteins, showPeptidesAndPsms, showCoverage, showSpectrum);
                    overviewPanel.updateSeparators();
                }
            });

        } else {
            overviewPanel.setDisplayOptions(showProteins, showPeptidesAndPsms, showCoverage, showSpectrum);
            overviewPanel.updateSeparators();
        }

    }//GEN-LAST:event_spectrumJCheckBoxMenuItemActionPerformed

    /**
     * Resize the overview panel when the frame resizes.
     *
     * @param evt
     */
    private void peptidesAndPsmsJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidesAndPsmsJCheckBoxMenuItemActionPerformed
        overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
}//GEN-LAST:event_peptidesAndPsmsJCheckBoxMenuItemActionPerformed

    /**
     * Resize the overview panel when the frame resizes.
     *
     * @param evt
     */
    private void proteinsJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinsJCheckBoxMenuItemActionPerformed
        overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
}//GEN-LAST:event_proteinsJCheckBoxMenuItemActionPerformed

    /**
     * Opens a dialog where the bubble scale factor can be selected.
     *
     * @param evt
     */
    private void bubbleScaleJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bubbleScaleJMenuItemActionPerformed

        String input = JOptionPane.showInputDialog(this, "Bubble Scale:", bubbleScale);

        if (input != null) {
            try {
                bubbleScale = new Double(input);
                overviewPanel.updateBubblePlot();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Bubble scale has to be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }//GEN-LAST:event_bubbleScaleJMenuItemActionPerformed

    /**
     * Opens the ProteinFilter dialog.
     * 
     * @param evt 
     */
    private void proteinFilterJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinFilterJMenuItemActionPerformed
        new ProteinFilter(this, true, currentProteinFilterValues, currrentProteinFilterRadioButtonSelections, currentProteinInferenceFilterSelection, true);
    }//GEN-LAST:event_proteinFilterJMenuItemActionPerformed

    /**
     * Export the spectrum in the Overview tab.
     * 
     * @param evt 
     */
    private void spectrumOverviewJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumOverviewJMenuItemActionPerformed
        new ExportGraphicsDialog(this, true, (Component) overviewPanel.getSpectrum());
}//GEN-LAST:event_spectrumOverviewJMenuItemActionPerformed

    /**
     * Update the menu items available on the export graphics menu to only 
     * show the ones for the current tab.
     * 
     * @param evt 
     */
    private void allTabsJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_allTabsJTabbedPaneStateChanged

        int selectedIndex = allTabsJTabbedPane.getSelectedIndex();

        // check if we have re-loaded the data using the current threshold and PEP window settings
        if (selectedIndex != VALIDATION_TAB_INDEX) {
            if (!statsPanel.thresholdUpdated() && !ignoreThresholdUpdate) {

                allTabsJTabbedPane.setSelectedIndex(VALIDATION_TAB_INDEX);

                int value = JOptionPane.showConfirmDialog(
                        this, "Do you want to revalidate your data using the current threshold?", "Revalidate Results?",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (value == JOptionPane.YES_OPTION) {
                    statsPanel.revalidateData();
                    allTabsJTabbedPane.setSelectedIndex(selectedIndex);
                } else if (value == JOptionPane.NO_OPTION) {
                    // reset the test, i.e., don't ask twice without changes in between
                    ignoreThresholdUpdate = true;
                    allTabsJTabbedPane.setSelectedIndex(selectedIndex);
                } else {
                    // cancel the move
                    allTabsJTabbedPane.setSelectedIndex(VALIDATION_TAB_INDEX);
                }
            } else if (!statsPanel.pepWindowApplied() && !ignorePepWindowUpdate) {

                allTabsJTabbedPane.setSelectedIndex(VALIDATION_TAB_INDEX);

                int value = JOptionPane.showConfirmDialog(
                        this, "Do you want to apply the changes to your data using the current PEP window?", "Apply Changes?",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (value == JOptionPane.YES_OPTION) {
                    statsPanel.applyPepWindow();
                    allTabsJTabbedPane.setSelectedIndex(selectedIndex);
                } else if (value == JOptionPane.NO_OPTION) {
                    // reset the test, i.e., don't ask twice without changes in between
                    ignorePepWindowUpdate = true;
                    allTabsJTabbedPane.setSelectedIndex(selectedIndex);
                } else {
                    // cancel the move
                    allTabsJTabbedPane.setSelectedIndex(VALIDATION_TAB_INDEX);
                }
            }
        } else {
            ignoreThresholdUpdate = false;
            ignorePepWindowUpdate = false;
        }

        // update the display of the spectra and the bubble plot export menu items
        spectrumOverviewJMenuItem.setVisible(false);
        spectrumOverviewJMenuItem.setEnabled(false);
        bubblePlotJMenuItem.setVisible(false);
        bubblePlotJMenuItem.setEnabled(false);
        spectrumSpectrumIdJMenuItem.setVisible(false);
        spectrumSpectrumIdJMenuItem.setEnabled(false);
        spectrumModificationsJMenuItem.setVisible(false);
        spectrumModificationsJMenuItem.setEnabled(false);

        if (displaySpectrum || experiment != null) {

            graphicsJMenu.setEnabled(true);

            switch (selectedIndex) {

                case 0:
                    if (overviewPanel.isSpectrumEnabled()) {
                        spectrumOverviewJMenuItem.setVisible(true);
                        spectrumOverviewJMenuItem.setEnabled(true);
                    }
                    bubblePlotJMenuItem.setVisible(true);
                    bubblePlotJMenuItem.setEnabled(true);
                    break;
                case 1:
                    spectrumSpectrumIdJMenuItem.setVisible(true);
                    spectrumSpectrumIdJMenuItem.setEnabled(true);
                    break;
                case 2:
                    spectrumModificationsJMenuItem.setVisible(true);
                    spectrumModificationsJMenuItem.setEnabled(true);
                    break;
                default:
                    graphicsJMenu.setEnabled(false);
                    break;
            }
        }


        // make sure that the same protein and peptide are selected in both 
        // the overview and protein structure tabs
        if (selectedIndex == 0) {

            if (selectedProteinIndex != -1) {

                int proteinRow = overviewPanel.getProteinTable().getSelectedRow();

                if (proteinRow != -1) {

                    int currentProteinIndex = (Integer) overviewPanel.getProteinTable().getValueAt(proteinRow, 0);

                    if (currentProteinIndex != selectedProteinIndex) {
                        overviewPanel.setSelectedProteinIndex(selectedProteinIndex);
                    }
                } else {
                    overviewPanel.setSelectedProteinIndex(selectedProteinIndex);
                }

                if (selectedPeptideIndex != -1) {

                    // invoke later to give time for components to update
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {

                            int peptideRow = overviewPanel.getPeptideTable().getSelectedRow();

                            if (peptideRow != -1) {

                                int currentPeptideIndex = (Integer) overviewPanel.getPeptideTable().getValueAt(peptideRow, 0);

                                if (currentPeptideIndex != selectedPeptideIndex) {
                                    overviewPanel.setSelectedPeptideIndex(selectedPeptideIndex);
                                }
                            } else {
                                overviewPanel.setSelectedPeptideIndex(selectedPeptideIndex);
                            }
                        }
                    });
                }
            }
        } else if (selectedIndex == 3) {

            if (selectedProteinIndex != -1) {

                int proteinRow = proteinStructurePanel.getProteinTable().getSelectedRow();

                if (proteinRow != -1) {

                    int currentProteinIndex = (Integer) proteinStructurePanel.getProteinTable().getValueAt(proteinRow, 0);

                    if (currentProteinIndex != selectedProteinIndex) {

                        // invoke later to give time for components to update
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                proteinStructurePanel.setSelectedProteinIndex(selectedProteinIndex);
                            }
                        });
                    }
                } else {
                    // invoke later to give time for components to update
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            proteinStructurePanel.setSelectedProteinIndex(selectedProteinIndex);
                        }
                    });
                }

                if (selectedPeptideIndex != -1) {

                    // invoke later to give time for components to update
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {

                            int peptideRow = proteinStructurePanel.getPeptideTable().getSelectedRow();

                            if (peptideRow != -1) {

                                int currentPeptideIndex = (Integer) proteinStructurePanel.getPeptideTable().getValueAt(peptideRow, 0);

                                if (currentPeptideIndex != selectedPeptideIndex) {
                                    proteinStructurePanel.setSelectedPeptideIndex(selectedPeptideIndex);
                                }
                            } else {
                                proteinStructurePanel.setSelectedPeptideIndex(selectedPeptideIndex);
                            }
                        }
                    });
                }
            }
        }

        // disable the protein filter option if a tab other than the overview tab is selected
        proteinFilterJMenuItem.setEnabled(selectedIndex == 0);
    }//GEN-LAST:event_allTabsJTabbedPaneStateChanged

    /**
     * Export the bubble plot.
     * 
     * @param evt 
     */
    private void bubblePlotJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bubblePlotJMenuItemActionPerformed
        new ExportGraphicsDialog(this, true, (Component) overviewPanel.getBubblePlot());
    }//GEN-LAST:event_bubblePlotJMenuItemActionPerformed

    /**
     * Export the spectrum in the Spectrum ID tab.
     * 
     * @param evt 
     */
    private void spectrumSpectrumIdJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumSpectrumIdJMenuItemActionPerformed
        new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
    }//GEN-LAST:event_spectrumSpectrumIdJMenuItemActionPerformed

    /**
     * Export the spectrum in the Modifications tab.
     * 
     * @param evt 
     */
    private void spectrumModificationsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumModificationsJMenuItemActionPerformed
        new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
    }//GEN-LAST:event_spectrumModificationsJMenuItemActionPerformed

    /**
     * Enable or disable the separators.
     * 
     * @param evt 
     */
    private void separatorsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_separatorsCheckBoxMenuItemActionPerformed
        overviewPanel.showSeparators(separatorsCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_separatorsCheckBoxMenuItemActionPerformed

    /**
     * Test if there are unsaved changes and if so asks the user if he/she 
     * wants to save these. If not closes the tool.
     * 
     * @param evt 
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Turn the model spin on or off.
     * 
     * @param evt 
     */
    private void modelSpinJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modelSpinJCheckBoxMenuItemActionPerformed
        proteinStructurePanel.spinModel(modelSpinJCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_modelSpinJCheckBoxMenuItemActionPerformed

    /**
     * Edit the use of relative error (ppm) or absolute error (Da) in the mass 
     * error plot.
     * 
     * @param evt 
     */
    private void errorPlotTypeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_errorPlotTypeCheckBoxMenuItemActionPerformed
        useRelativeError = !errorPlotTypeCheckBoxMenuItem.isSelected();
        overviewPanel.updateSpectrum();
    }//GEN-LAST:event_errorPlotTypeCheckBoxMenuItemActionPerformed

    /**
     * Extracts the protein accession numbers from the Overview or Structure 
     * tab and opens a dialog for exporting them to file.
     * 
     * @param evt 
     */
    /**
     * Extracts the list of proteins from the Overview or Structure 
     * tab and opens a dialog for exporting them to file.
     * 
     * @param evt 
     */
    /**
     * Extracts the list of peptides from the Overview or Structure 
     * tab and opens a dialog for exporting them to file.
     * 
     * @param evt 
     */
    /**
     * Export all peptide lists and add protein accession number to each line.
     * 
     * @param evt 
     */
    /**
     * Turns the hiding of the scores columns on or off.
     * 
     * @param evt 
     */
    private void scoresJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scoresJCheckBoxMenuItemActionPerformed
        overviewPanel.hideScores(!scoresJCheckBoxMenuItem.isSelected());
        proteinStructurePanel.hideScores(!scoresJCheckBoxMenuItem.isSelected());
        ptmPanel.hideScores(!scoresJCheckBoxMenuItem.isSelected());

        // make sure that the jsparklines are showing correctly
        sparklinesJCheckBoxMenuItemActionPerformed(null);
    }//GEN-LAST:event_scoresJCheckBoxMenuItemActionPerformed

    /**
     * Open a file chooser to open an existing PeptideShaker project.
     * 
     * @param evt 
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed

        boolean openProject = true;

        if (!dataSaved) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + experiment.getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                openProject = true;
            } else if (value == JOptionPane.CANCEL_OPTION) {
                openProject = false;
            } else { // no option
                // do nothing
            }
        }

        if (openProject) {

            JFileChooser fileChooser = new JFileChooser(getLastSelectedFolder());
            fileChooser.setDialogTitle("Open PeptideShaker Project");

            FileFilter filter = new FileFilter() {

                @Override
                public boolean accept(File myFile) {
                    return myFile.getName().toLowerCase().endsWith("cps")
                            || myFile.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "Supported formats: Peptide Shaker (.cps)";
                }
            };

            fileChooser.setFileFilter(filter);
            int returnVal = fileChooser.showDialog(this.getParent(), "Open");

            if (returnVal == JFileChooser.APPROVE_OPTION) {

                OpenDialog openDialog = new OpenDialog(this, true);

                openDialog.setSearchParamatersFiles(new ArrayList<File>());
                File newFile = fileChooser.getSelectedFile();
                setLastSelectedFolder(newFile.getAbsolutePath());

                if (!newFile.getName().toLowerCase().endsWith("cps")) {
                    JOptionPane.showMessageDialog(this, "Not a PeptideShaker file (.cps).",
                            "Wrong File.", JOptionPane.ERROR_MESSAGE);
                } else {

                    // get the properties files
                    for (File file : newFile.getParentFile().listFiles()) {
                        if (file.getName().toLowerCase().endsWith(".properties")) {
                            if (!openDialog.getSearchParametersFiles().contains(file)) {
                                openDialog.getSearchParametersFiles().add(file);
                            }
                        }
                    }

                    openDialog.isPsFile(true);
                    openDialog.importPeptideShakerFile(newFile);
                }
            }
        }
    }//GEN-LAST:event_openJMenuItemActionPerformed

    /**
     * Open the spectrum counting preferences dialog.
     * 
     * @param evt 
     */
    private void spectrumCountingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumCountingMenuItemActionPerformed
        new SpectrumCountingPreferencesDialog(this);
    }//GEN-LAST:event_spectrumCountingMenuItemActionPerformed

    /**
     * Set the 3d structure model type to ribbon.
     * 
     * @param evt 
     */
    private void ribbonJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ribbonJRadioButtonMenuItemActionPerformed
        proteinStructurePanel.setRibbonModel(ribbonJRadioButtonMenuItem.isSelected());
        proteinStructurePanel.setBackboneModel(backboneJRadioButtonMenuItem.isSelected());
        proteinStructurePanel.updateModelType();
    }//GEN-LAST:event_ribbonJRadioButtonMenuItemActionPerformed

    /**
     * Set the 3d structure model type to backbone.
     * 
     * @param evt 
     */
    private void backboneJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backboneJRadioButtonMenuItemActionPerformed
        proteinStructurePanel.setRibbonModel(ribbonJRadioButtonMenuItem.isSelected());
        proteinStructurePanel.setBackboneModel(backboneJRadioButtonMenuItem.isSelected());
        proteinStructurePanel.updateModelType();
    }//GEN-LAST:event_backboneJRadioButtonMenuItemActionPerformed

    private void identificationFeaturesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_identificationFeaturesMenuActionPerformed
        new FeaturesPreferencesDialog(this);
    }//GEN-LAST:event_identificationFeaturesMenuActionPerformed

    private void followUpAnalysisMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_followUpAnalysisMenuActionPerformed
        new FollowupPreferencesDialog(this);
    }//GEN-LAST:event_followUpAnalysisMenuActionPerformed

    /**
     * Returns if the 3D model is to be spinning or not.
     * 
     * @return true if the 3D model is to be spinning
     */
    public boolean spinModel() {
        return modelSpinJCheckBoxMenuItem.isSelected();
    }

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory
     */
    private void loadEnzymes() {
        try {
            enzymeFactory.importEnzymes(new File(ENZYME_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Not able to load the enzyme file.", "Wrong enzyme file.", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * This method will display results in all panels
     * 
     * @param iUpdateValidationTab if true the validation tab will be updated
     */
    public void displayResults(boolean iUpdateValidationTab) {

        final boolean updateValidationTab = iUpdateValidationTab;

        try {
            displaySpectrum = true;
            boolean displaySequence = true;
            boolean displayProteins = true;
            boolean displayPeptidesAndPSMs = true;

            sequenceCoverageJCheckBoxMenuItem.setSelected(displaySequence);
            spectrumJCheckBoxMenuItem.setSelected(displaySpectrum);

            overviewPanel.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displaySequence, displaySpectrum);
            overviewPanel.updateSeparators();
            statsPanel.updateSeparators();

            progressDialog = new ProgressDialogX(this, this, true);
            int max = 3*identification.getProteinIdentification().size()
                    + 2*identification.getPeptideIdentification().size()
                    + 2*identification.getSpectrumIdentification().size()
                    +1;
            progressDialog.setMax(max);
            progressDialog.doNothingOnClose();

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setTitle("Loading Data. Please wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("DisplayThread") {

                @Override
                public void run() {

                    if (displaySpectrum) {
                        progressDialog.setTitle("Loading spectrum identifications tab. Please wait...");
                        spectrumIdentificationPanel.displayResults(progressDialog);
                        progressDialog.setTitle("Loading PTM tab. Please wait...");
                        ptmPanel.displayResults(progressDialog);
                    } else {
                        spectrumJPanel.setEnabled(false);
                        ptmPanel.setEnabled(false);
                        progressDialog.setValue(identification.getPeptideIdentification().size() + identification.getSpectrumIdentification().size());
                    }


                    try {
                        progressDialog.setTitle("Loading overview tab. Please wait...");
                        overviewPanel.displayResults(progressDialog);

                        if (updateValidationTab) {
                        progressDialog.setTitle("Loading validation tab. Please wait...");
                            statsPanel.displayResults();
                        }
                        progressDialog.incrementValue();

                        progressDialog.setTitle("Loading structure tab. Please wait...");
                        proteinStructurePanel.displayResults(progressDialog);

                        progressDialog.setTitle("Loading QC tab. Please wait...");
                        qcPanel.displayResults(progressDialog);


                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "A problem occured when loading the data.\nSee /conf/PeptideShaker.log for more details.", "Loading Failed!", JOptionPane.ERROR_MESSAGE);
                    }

                    allTabsJTabbedPaneStateChanged(null);

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    saveMenuItem.setEnabled(true);
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "A problem occured while displaying results. Please send the log file to the developers.", "Display problem", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method called to disable the spectrum display
     */
    public void disableSpectrumDisplay() {
        spectrumJPanel.setEnabled(false);
        ptmPanel.setEnabled(false);
        overviewPanel.updateSeparators();
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
        ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JTabbedPane allTabsJTabbedPane;
    private javax.swing.JMenuItem annotationPreferencesMenu;
    private javax.swing.JPanel annotationsJPanel;
    private javax.swing.JRadioButtonMenuItem backboneJRadioButtonMenuItem;
    private javax.swing.JMenuItem bubblePlotJMenuItem;
    private javax.swing.JMenuItem bubbleScaleJMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem errorPlotTypeCheckBoxMenuItem;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenu exportJMenu;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JMenu filterMenu;
    private javax.swing.JMenuItem followUpAnalysisMenu;
    private javax.swing.JPanel gradientPanel;
    private javax.swing.JMenu graphicsJMenu;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem identificationFeaturesMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.ButtonGroup modelButtonGroup;
    private javax.swing.JCheckBoxMenuItem modelSpinJCheckBoxMenuItem;
    private javax.swing.JMenu modelTypeMenu;
    private javax.swing.JMenuItem newJMenuItem;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JMenu overViewTabViewMenu;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JCheckBoxMenuItem peptidesAndPsmsJCheckBoxMenuItem;
    private javax.swing.JMenuItem proteinFilterJMenuItem;
    private javax.swing.JPanel proteinStructureJPanel;
    private javax.swing.JCheckBoxMenuItem proteinsJCheckBoxMenuItem;
    private javax.swing.JPanel ptmJPanel;
    private javax.swing.JPanel qcJPanel;
    private javax.swing.JRadioButtonMenuItem ribbonJRadioButtonMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem scoresJCheckBoxMenuItem;
    private javax.swing.JMenuItem searchParametersMenu;
    private javax.swing.JCheckBoxMenuItem separatorsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sequenceCoverageJCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JMenuItem spectrumCountingMenuItem;
    private javax.swing.JCheckBoxMenuItem spectrumJCheckBoxMenuItem;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JMenuItem spectrumModificationsJMenuItem;
    private javax.swing.JMenuItem spectrumOverviewJMenuItem;
    private javax.swing.JMenuItem spectrumSpectrumIdJMenuItem;
    private javax.swing.JPanel statsJPanel;
    private javax.swing.JMenu structureTabViewMenu;
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

            if ((java.net.HttpURLConnection) downloadPage.openConnection() != null) {

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
            }
        } catch (UnknownHostException e) {
            // ignore exception
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
                String path = getJarFilePath() + "/conf/PeptideShaker.log";

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
                        null, "An error occured when trying to create the PeptideShaker log file.",
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
     * Set the default preferences.
     */
    private void setDefaultPreferences() {
        searchParameters = new SearchParameters();
        searchParameters.setEnzyme(enzymeFactory.getEnzyme("Trypsin"));
        searchParameters.setFragmentIonMZTolerance(0.5);
        searchParameters.setPrecursorToleranceUnit(0);
        searchParameters.setPrecursorTolerance(10);
        searchParameters.setIonSearched1("b");
        searchParameters.setIonSearched2("y");
        loadModificationProfile(profileFile);
        annotationPreferences.annotateMostIntensePeaks(true);
        annotationPreferences.useDefaultAnnotation(true);
        updateAnnotationPreferencesFromSearchSettings();
        spectrumCountingPreferences.setSelectedMethod(SpectrumCountingPreferences.NSAF);
        spectrumCountingPreferences.setValidatedHits(true);
    }

    /**
     * Updates the ions used for fragment annotation
     */
    public void updateAnnotationPreferencesFromSearchSettings() {
        annotationPreferences.clearIonTypes();
        annotationPreferences.addIonType(searchParameters.getIonSearched1());
        annotationPreferences.addIonType(searchParameters.getIonSearched2());
        annotationPreferences.addIonType(PeptideFragmentIonType.PRECURSOR_ION);
        annotationPreferences.addIonType(PeptideFragmentIonType.IMMONIUM);
        annotationPreferences.setMzTolerance(searchParameters.getFragmentIonMZTolerance());
    }

    /**
     * Returns the spectrum annotator
     * @return the spectrum annotator 
     */
    public SpectrumAnnotator getSpectrumAnnorator() {
        return spectrumAnnotator;
    }

    /**
     * Convenience method returning the current annotations without requesting the specification of the spectrum and peptide
     * @return the current annotations without requesting the specification of the spectrum and peptide
     * @throws MzMLUnmarshallerException exception thrown whenever an error occurred while reading the mzML file
     */
    public ArrayList<IonMatch> getIonsCurrentlyMatched() throws MzMLUnmarshallerException {
        return spectrumAnnotator.getCurrentAnnotation(annotationPreferences.getIonTypes(),
                annotationPreferences.getNeutralLosses(),
                annotationPreferences.getValidatedCharges());
    }

    /**
     * Updates the annotations on all panels
     */
    public void updateAnnotations() {
        overviewPanel.updateSpectrum();
        ptmPanel.updateSpectra();
        spectrumIdentificationPanel.updateSpectrum();
    }

    /**
     * Returns the modification profile file
     * @return the modification profile file 
     */
    public File getModificationProfileFile() {
        return profileFile;
    }

    /**
     * Sets the modification profile file
     * @param profileFile the modification profile file
     */
    public void setModificationProfileFile(File profileFile) {
        this.profileFile = profileFile;
    }

    /**
     * Loads the modification profile from the given file
     * @param aFile the given file
     */
    private void loadModificationProfile(File aFile) {
        try {
            FileInputStream fis = new FileInputStream(aFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            HashMap<String, String> modificationProfile = (HashMap<String, String>) in.readObject();
            in.close();
            for (String modificationName : modificationProfile.keySet()) {
                searchParameters.addExpectedModification(modificationName, modificationProfile.get(modificationName));
            }
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, aFile.getName() + " not found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
            searchParameters.clearModificationProfile();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading " + aFile.getName() + ".\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
            searchParameters.clearModificationProfile();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading " + aFile.getName() + ".\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
            searchParameters.clearModificationProfile();
        }
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicateNumber
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * Returns the identification displayed.
     * 
     * @return  the identification displayed
     */
    public Identification getIdentification() {
        return identification;
    }

    /**
     * Returns the desired spectrum
     * @param spectrumKey   the key of the spectrum
     * @return the desired spectrum
     */
    public MSnSpectrum getSpectrum(String spectrumKey) {
        String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
        try {
            return (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFile, spectrumTitle);
        } catch (Exception e) {
            catchException(e);
            return null;
        }
    }

    /**
     * Returns the precursor of a given spectrum
     * @param spectrumKey   the key of the given spectrum
     * @return  the precursor
     */
    public Precursor getPrecursor(String spectrumKey) {
        String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
        try {
            return spectrumFactory.getPrecursor(spectrumFile, spectrumTitle);
        } catch (Exception e) {
            catchException(e);
            return null;
        }
    }

    /**
     * Returns the annotation preferences as set by the user.
     *
     * @return the annotation preferences as set by the user
     */
    public AnnotationPreferences getAnnotationPreferences() {
        return annotationPreferences;
    }

    /**
     * Returns the spectrum counting preferences
     * @return the spectrum counting preferences 
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return spectrumCountingPreferences;
    }

    /**
     * Sets new spectrum counting preferences
     * @param spectrumCountingPreferences new spectrum counting preferences
     */
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        this.spectrumCountingPreferences = spectrumCountingPreferences;
    }

    /**
     * Returns the displayed proteomicAnalysis.
     *
     * @return the displayed proteomicAnalysis
     */
    public ProteomicAnalysis getProteomicanalysis() {
        return experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
    }

    /**
     * Returns the search parameters.
     *
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    /**
     * Updates the search parameters.
     *
     * @param searchParameters  the new search parameters
     */
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
    }

    /**
     * Updates the new annotation preferences.
     * 
     * @param annotationPreferences the new annotation preferences
     */
    public void setAnnotationPreferences(AnnotationPreferences annotationPreferences) {
        this.annotationPreferences = annotationPreferences;
    }

    /**
     * Loads the modifications from the modification file.
     */
    private void loadModifications() {
        try {
            ptmFactory.importModifications(new File(MODIFICATIONS_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(USER_MODIFICATIONS_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + USER_MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void cancelProgress() {
        // do nothing
    }

    /**
     * Returns the label width for the sparklines.
     *
     * @return the labelWidth
     */
    public int getLabelWidth() {
        return labelWidth;
    }

    /**
     * Set the label width for the sparklines.
     *
     * @param labelWidth the labelWidth to set
     */
    public void setLabelWidth(int labelWidth) {
        this.labelWidth = labelWidth;
    }

    /**
     * Get the sparklines color.
     *
     * @return the sparklineColor
     */
    public Color getSparklineColor() {
        return sparklineColor;
    }

    /**
     * Set the sparklines color.
     *
     * @param sparklineColor the sparklineColor to set
     */
    public void setSparklineColor(Color sparklineColor) {
        this.sparklineColor = sparklineColor;
    }

    /**
     * Returns the bubble plot scale value.
     *
     * @return the bubble plot scale value
     */
    public double getBubbleScale() {
        return bubbleScale;
    }

    /**
     * Selects the desired spectrum in the spectrum Id tab.
     * 
     * @param spectrumKey the key of the desired spectrum
     */
    public void selectSpectrum(String spectrumKey) {
        spectrumIdentificationPanel.selectSpectrum(spectrumKey);
    }

    /**
     * Opens the Spectrum ID tab.
     */
    public void openSpectrumIdTab() {
        allTabsJTabbedPane.setSelectedIndex(SPECTRUM_ID_TAB_INDEX);
    }

    /**
     * Returns the color to use for the HTML tags for the selected rows, in HTML color code.
     * 
     * @return the color to use for the HTML tags for the selected rows, in HTML color code
     */
    public String getSelectedRowHtmlTagFontColor() {
        return selectedRowHtmlTagFontColor;
    }

    /**
     * Returns the color to use for the HTML tags for the rows that are not selected, in HTML color code.
     * 
     * @return the color to use for the HTML tags for the rows that are not selected, in HTML color code
     */
    public String getNotSelectedRowHtmlTagFontColor() {
        return notSelectedRowHtmlTagFontColor;
    }

    /**
     * Returns the protein table from the overview panel.
     * 
     * @return the protein table from the overview panel
     */
    public JTable getOverviewProteinTable() {
        return overviewPanel.getProteinTable();
    }

    /**
     * Returns the current protein filter values.
     *
     * @return the current protein filter values
     */
    public String[] getCurrentProteinFilterValues() {
        return currentProteinFilterValues;
    }

    /**
     * Set the current protein filter values.
     *
     * @param currentProteinFilterValues the protein filter values to set
     */
    public void setCurrentProteinFilterValues(String[] currentProteinFilterValues) {
        this.currentProteinFilterValues = currentProteinFilterValues;
    }

    /**
     * Returns the current protein filter radio button settings.
     *
     * @return the current protein filter radio button settings
     */
    public Integer[] getCurrrentProteinFilterRadioButtonSelections() {
        return currrentProteinFilterRadioButtonSelections;
    }

    /**
     * Set the current protein filter radio button settings.
     *
     * @param currrentProteinFilterRadioButtonSelections the protein filter radio buttons to set
     */
    public void setCurrrentProteinFilterRadioButtonSelections(Integer[] currrentProteinFilterRadioButtonSelections) {
        this.currrentProteinFilterRadioButtonSelections = currrentProteinFilterRadioButtonSelections;
    }

    /**
     * Set the current protein inference filer selection.
     * 
     * @param currentProteinInferenceFilterSelection the protein inference filer selection to set
     */
    public void setCurrrentProteinInferenceFilterSelection(int currentProteinInferenceFilterSelection) {
        this.currentProteinInferenceFilterSelection = currentProteinInferenceFilterSelection;
    }

    /**
     * Returns the current protein inference selection as an int.
     * 
     * @return the current protein inference selection as an int (0-5)
     */
    public int getCurrrentProteinInferenceFilterSelection() {
        return currentProteinInferenceFilterSelection;
    }

    /**
     * Update the overview panel to make sure that the currently selected protein 
     * in the protein table is displayed in the other tables.
     * 
     * @param updateProteinSelection if true the protein selection will be updated
     */
    public void updateProteinTableSelection(boolean updateProteinSelection) {
        overviewPanel.updateProteinSelection(updateProteinSelection);
    }

    /**
     * Set the selected protein index in the overview or protein structure tabs. 
     * Used to make sure that the same protein is selected in both tabs.
     * 
     * @param selectedProteinIndex      the selected protein index
     */
    public void setSelectedProteinIndex(Integer selectedProteinIndex) {
        this.selectedProteinIndex = selectedProteinIndex;
    }

    /**
     * Set the selected protein accesssion number in the annotation tab.
     * 
     * @param selectedProteinAccession      the selected protein accession number
     */
    public void setSelectedProteinAccession(String selectedProteinAccession) {
        annotationPanel.setAccessionNumber(selectedProteinAccession);
    }

    /**
     * Set the selected peptide index in the overview or protein structure tabs. 
     * Used to make sure that the same peptide is selected in both tabs.
     * 
     * @param selectedPeptideIndex      the selected peptide index
     */
    public void setSelectedPeptideIndex(Integer selectedPeptideIndex) {
        this.selectedPeptideIndex = selectedPeptideIndex;
    }

    /**
     * Estimate the sequence coverage for the given protein.
     *
     * @param proteinMatch  the protein match
     * @param sequence      the protein sequence
     * @return              the estimated sequence coverage
     */
    public double estimateSequenceCoverage(ProteinMatch proteinMatch, String sequence) {

        // an array containing the coverage index for each residue
        int[] coverage = new int[sequence.length() + 1];
        int peptideTempStart, peptideTempEnd;
        String tempSequence, peptideSequence;
        PSParameter pSParameter = new PSParameter();
        PeptideMatch peptideMatch;
        // iterate the peptide table and store the coverage for each peptide
        for (String peptideKey : proteinMatch.getPeptideMatches()) {
            pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
            if (pSParameter.isValidated()) {
                tempSequence = sequence;
                peptideSequence = Peptide.getSequence(peptideKey);
                peptideTempStart = 0;
                while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                    peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                    peptideTempEnd = peptideTempStart + peptideSequence.length();
                    for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                        coverage[j] = 1;
                    }
                    tempSequence = sequence.substring(0, peptideTempStart);
                }
            }
        }

        double covered = 0.0;

        for (int aa : coverage) {
            covered += aa;
        }

        return covered / ((double) sequence.length());
    }

    /**
     * Transforms the protein accesion number into an HTML link to the 
     * corresponding database. Note that this is a complete HTML with 
     * HTML and a href tags, where the main use is to include it in the 
     * protein tables.
     * 
     * @param protein   the protein to get the database link for
     * @return          the transformed accession number
     */
    public String addDatabaseLink(String proteinAccession) {

        String accessionNumberWithLink = proteinAccession;
        try {
            if (sequenceFactory.getHeader(proteinAccession) != null) {

                // try to find the database from the SequenceDatabase
                String database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                // create the database link
                if (database != null) {

                    // @TODO: support more databases

                    if (database.equalsIgnoreCase("IPI") || database.equalsIgnoreCase("UNIPROT")) {
                        accessionNumberWithLink = "<html><a href=\"" + getUniProtAccessionLink(proteinAccession)
                                + "\"><font color=\"" + getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else {
                        // unknown database!
                    }
                }
            }
        } catch (Exception e) {
        }

        return accessionNumberWithLink;
    }

    /**
     * Transforms the protein accesion number into an HTML link to the 
     * corresponding database. Note that this is a complete HTML with 
     * HTML and a href tags, where the main use is to include it in the 
     * protein tables.
     * 
     * @param proteins  the list of proteins to get the database links for
     * @return          the transformed accession number
     */
    public String addDatabaseLinks(ArrayList<String> proteins) {

        if (proteins.isEmpty()) {
            return "";
        }

        String accessionNumberWithLink = "<html>";

        for (int i = 0; i < proteins.size(); i++) {

            String proteinAccession = proteins.get(i);
            try {
                if (!SequenceFactory.isDecoy(proteins.get(i)) && sequenceFactory.getHeader(proteinAccession) != null) {

                    // try to find the database from the SequenceDatabase
                    String database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                    // create the database link
                    if (database != null) {

                        // @TODO: support more databases

                        if (database.equalsIgnoreCase("IPI") || database.equalsIgnoreCase("UNIPROT")) {
                            accessionNumberWithLink += "<a href=\"" + getUniProtAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else {
                            // unknown database!
                            accessionNumberWithLink += "<font color=\"" + getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font>" + ", ";
                        }
                    }
                } else {
                    accessionNumberWithLink += proteinAccession + ", ";
                }
            } catch (Exception e) {
                accessionNumberWithLink += proteinAccession + ", ";
            }
        }

        // remove the last ', '
        accessionNumberWithLink = accessionNumberWithLink.substring(0, accessionNumberWithLink.length() - 2);
        accessionNumberWithLink += "</html>";

        return accessionNumberWithLink;
    }

    /**
     * Returns the protein accession number as a web link to the given 
     * protein at http://srs.ebi.ac.uk.
     * 
     * @param proteinAccession  the protein accession number
     * @param database          the protein database
     * @return                  the protein accession web link
     */
    public String getSrsAccessionLink(String proteinAccession, String database) {
        return "http://srs.ebi.ac.uk/srsbin/cgi-bin/wgetz?-e+%5b" + database + "-AccNumber:" + proteinAccession + "%5d";
    }

    /**
     * Returns the protein accession number as a web link to the given 
     * protein at http://www.uniprot.org/uniprot.
     * 
     * @param proteinAccession  the protein accession number
     * @return                  the protein accession web link
     */
    public String getUniProtAccessionLink(String proteinAccession) {
        return "http://www.uniprot.org/uniprot/" + proteinAccession;
    }

    /**
     * Clear the data from the previous experiment
     */
    public void clearData() {

        // reset the filter
        currentProteinFilterValues = new String[]{"", "", "", "", "", "", "", ""};

        // set up the tabs/panels
        setUpPanels(true);

        // repaint the panels
        repaintPanels();
    }

    /**
     * Reloads the data.
     */
    public void reloadData() {

        dataSaved = false;

        // set up the tabs/panels
        setUpPanels(false);

        // repaint the panels
        repaintPanels();

        // display the results
        displayResults(false);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                overviewPanel.updateSeparators();
                overviewJPanel.revalidate();
                overviewJPanel.repaint();
            }
        });
    }

    /**
     * Set up the different tabs/panels.
     */
    private void setUpPanels(boolean setupValidationTab) {

        if (setupValidationTab) {
            statsPanel = new StatsPanel(this);
            statsJPanel.removeAll();
            statsJPanel.add(statsPanel);
        }

        overviewPanel = new OverviewPanel(this);
        ptmPanel = new PtmPanel(this);
        spectrumIdentificationPanel = new SpectrumIdentificationPanel(this);
        proteinStructurePanel = new ProteinStructurePanel(this);
        annotationPanel = new AnnotationPanel(this);
        qcPanel = new QCPanel(this);

        overviewJPanel.removeAll();
        overviewJPanel.add(overviewPanel);

        ptmJPanel.removeAll();
        ptmJPanel.add(ptmPanel);

        spectrumJPanel.removeAll();
        spectrumJPanel.add(spectrumIdentificationPanel);

        proteinStructureJPanel.removeAll();
        proteinStructureJPanel.add(proteinStructurePanel);

        annotationsJPanel.removeAll();
        annotationsJPanel.add(annotationPanel);

        qcJPanel.removeAll();
        qcJPanel.add(qcPanel);

        // hide/show the score columns
        scoresJCheckBoxMenuItemActionPerformed(null);
    }

    /**
     * Repaint the tabs/panels.
     */
    private void repaintPanels() {

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                overviewPanel.updateSeparators();
                overviewJPanel.revalidate();
                overviewJPanel.repaint();
            }
        });

        statsPanel.updatePlotSizes();
        statsJPanel.revalidate();
        statsJPanel.repaint();

        ptmJPanel.revalidate();
        ptmJPanel.repaint();

        spectrumJPanel.revalidate();
        spectrumJPanel.repaint();

        proteinStructureJPanel.revalidate();
        proteinStructureJPanel.repaint();

        annotationsJPanel.revalidate();
        annotationsJPanel.repaint();

        qcPanel.revalidate();
        qcPanel.repaint();

    }

    /**
     * Returns the OverviewPanel.
     * 
     * @return the OverviewPanel
     */
    public OverviewPanel getOverviewPanel() {
        return overviewPanel;
    }

    /**
     * Returns the StatsPanel.
     * 
     * @return the StatsPanel
     */
    public StatsPanel getStatsPanel() {
        return statsPanel;
    }

    /**
     * Returns the PtmPanel.
     * 
     * @return the PtmPanel
     */
    public PtmPanel getPtmPanel() {
        return ptmPanel;
    }

    /**
     * Returns the ProteinStructurePanel.
     * 
     * @return the ProteinStructurePanel
     */
    public ProteinStructurePanel getProteinStructurePanel() {
        return proteinStructurePanel;
    }

    /**
     * Returns the SpectrumIdentificationPanel.
     * 
     * @return the SpectrumIdentificationPanel
     */
    public SpectrumIdentificationPanel getSpectrumIdentificationPanel() {
        return spectrumIdentificationPanel;
    }

    /**
     * Gets the preferred width of the column specified by vColIndex. The column
     * will be just wide enough to show the column head and the widest cell in the 
     * column. Margin pixels are added to the left and right (resulting in an additional 
     * width of 2*margin pixels.
     * 
     * @param table         the table
     * @param colIndex      the colum index
     * @param margin        the margin to add
     * @return              the prefereed width of the column 
     */
    public int getPreferredColumnWidth(JTable table, int colIndex, int margin) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(colIndex);
        int width = 0;

        // get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        width = comp.getPreferredSize().width;

        // get maximum width of column data
        for (int r = 0; r < table.getRowCount(); r++) {
            renderer = table.getCellRenderer(r, colIndex);
            comp = renderer.getTableCellRendererComponent(
                    table, table.getValueAt(r, colIndex), false, false, r, colIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        // add margin
        width += 2 * margin;

        return width;
    }

    /**
     * Enable or disable the spectrum export in the overview panel.
     * 
     * @param enable if true the spectrum export in the overview panel will be enabled
     */
    public void enableSpectrumExport(boolean enable) {
        if (spectrumOverviewJMenuItem.isVisible()) {
            spectrumOverviewJMenuItem.setEnabled(enable);
        }
    }

    /**
     * Update the main match for the given row in the protein table.
     * 
     * @param mainMatch             the protein match to use
     * @param proteinInferenceType  the protein inference group type
     */
    public void updateMainMatch(String mainMatch, int proteinInferenceType) {
        overviewPanel.updateMainMatch(mainMatch, proteinInferenceType);
        proteinStructurePanel.updateMainMatch(mainMatch, proteinInferenceType);
    }

    /**
     * Set whether the current data has been saved to a cps file or not.
     * 
     * @param dataSaved whether the current data has been saved to a cps file or not
     */
    public void setDataSaved(boolean dataSaved) {
        this.dataSaved = dataSaved;
    }

    /**
     * Returns the selected tab as indexed by the static fields
     * @return 
     */
    public int getSelectedTab() {
        return allTabsJTabbedPane.getSelectedIndex();
    }

    /**
     * Returns a list of keys of the currently displayed proteins
     * @return a list of keys of the currently displayed proteins
     */
    public ArrayList<String> getDisplayedProteins() {
        int selectedTab = getSelectedTab();
        switch (selectedTab) {
            case OVER_VIEW_TAB_INDEX:
                return overviewPanel.getDisplayedProteins();
            case STRUCTURES_TAB_INDEX:
                return proteinStructurePanel.getDisplayedProteins();
            case MODIFICATIONS_TAB_INDEX:
                return ptmPanel.getDisplayedProteinMatches();
            default:
                return null;
        }
    }

    /**
     * Returns a list of keys of the currently displayed peptides
     * @return a list of keys of the currently displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        int selectedTab = getSelectedTab();
        switch (selectedTab) {
            case OVER_VIEW_TAB_INDEX:
                return overviewPanel.getDisplayedPeptides();
            case STRUCTURES_TAB_INDEX:
                return proteinStructurePanel.getDisplayedPeptides();
            case MODIFICATIONS_TAB_INDEX:
                return ptmPanel.getDisplayedPeptides();
            default:
                return null;
        }
    }

    /**
     * Returns a list of keys of the currently displayed psms
     * @return a list of keys of the currently displayed psms
     */
    public ArrayList<String> getDisplayedPSMs() {
        int selectedTab = getSelectedTab();
        switch (selectedTab) {
            case OVER_VIEW_TAB_INDEX:
                return overviewPanel.getDisplayedPsms();
            case MODIFICATIONS_TAB_INDEX:
                return ptmPanel.getDisplayedAssumptions();
            default:
                return null;
        }
    }

    /**
     * Returns a list of keys of the currently displayed assumptions
     * @return a list of keys of the currently displayed assumptions
     */
    public ArrayList<String> getDisplayedAssumptions() {
        int selectedTab = getSelectedTab();
        switch (selectedTab) {
            case OVER_VIEW_TAB_INDEX:
                return overviewPanel.getDisplayedPsms();
            case MODIFICATIONS_TAB_INDEX:
                return ptmPanel.getDisplayedAssumptions();
            default:
                return null;
        }
    }

    /**
     * Opens one or more protein links in the default web browser.
     * 
     * @param links 
     */
    public void openProteinLinks(String links) {

        links = links.substring("<html><a href=\"".length());
        String[] allLinks = links.split("<a href=\"");

        int value = JOptionPane.YES_OPTION;

        if (allLinks.length > 5) {
            value = JOptionPane.showConfirmDialog(this,
                    "This will open " + allLinks.length + " tabs in your web browser. Continue?",
                    "Open Tabs?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }

        if (value == JOptionPane.YES_OPTION) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            for (int i = 0; i < allLinks.length; i++) {
                String link = allLinks[i];
                link = link.substring(0, link.indexOf("\""));
                BareBonesBrowserLaunch.openURL(link);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Returns true if the relative error (ppm) is used instead of the absolute error (Da).
     * 
     * @return true if the relative error (ppm) is used instead of the absolute error (Da)
     */
    public boolean useRelativeError() {
        return useRelativeError;
    }

    /**
     * Returns the decimal format used for the score and confidence columns.
     * 
     * @return the decimal format used for the score and confidence columns
     */
    public DecimalFormat getScoreAndConfidenceDecimalFormat() {
        return scoreAndConfidenceDecimalFormat;
    }

    /**
     * Returns the number of spectra where this protein was found independantly from the validation process.
     * @param proteinMatch the protein match of interest
     * @return the number of spectra where this protein was found
     */
    public int getNSpectra(ProteinMatch proteinMatch) {
        int result = 0;
        try {
            PeptideMatch peptideMatch;
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                peptideMatch = identification.getPeptideMatch(peptideKey);
                result += peptideMatch.getSpectrumCount();
            }
        } catch (Exception e) {
            catchException(e);
        }
        return result;
    }

    /**
     * Returns the spectrum counting score based on the user's settings
     * @param proteinMatch  the inspected protein match
     * @return the spectrum counting score
     */
    public double getSpectrumCounting(ProteinMatch proteinMatch) {
        double result;
        Enzyme enyzme = searchParameters.getEnzyme();
        PSParameter pSParameter = new PSParameter();
        Protein currentProtein = null;
        try {
            if (spectrumCountingPreferences.getSelectedMethod() == SpectrumCountingPreferences.NSAF) {
                currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                if (currentProtein == null) {
                    return 0.0;
                }
                result = 0;
                PeptideMatch peptideMatch;
                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    peptideMatch = identification.getPeptideMatch(peptideKey);
                    for (String spectrumMatchKey : peptideMatch.getSpectrumMatches()) {
                        pSParameter = (PSParameter) identification.getMatchParameter(spectrumMatchKey, pSParameter);
                        if (!spectrumCountingPreferences.isValidatedHits() || pSParameter.isValidated()) {
                            result = result + 1;
                        }
                    }
                }
                return result = result / currentProtein.getSequence().length();
            } else {
                if (spectrumCountingPreferences.isValidatedHits()) {
                    result = 0;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
                        if (pSParameter.isValidated()) {
                            result = result + 1;
                        }
                    }
                } else {
                    result = proteinMatch.getPeptideCount();
                }
                return result = Math.pow(10, result / currentProtein.getNPossiblePeptides(enyzme)) - 1;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Method called whenever an exception is caught
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        e.printStackTrace();
        if (!exceptionCaught.contains(e.getLocalizedMessage())) {
            exceptionCaught.add(e.getLocalizedMessage());
            JOptionPane.showMessageDialog(this,
                    "An error occured while reading "
                    + e.getLocalizedMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void close() {
        if (!dataSaved && experiment != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + experiment.getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
            } else if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        progressDialog = new ProgressDialogX(this, this, true);
        progressDialog.doNothingOnClose();

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Closing. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("ExportThread") {

            @Override
            public void run() {
                try {
                    File serializationFolder = new File(PeptideShaker.SERIALIZATION_DIRECTORY);
                    String[] files = serializationFolder.list();
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(files.length);
                    int cpt = 0;
                    for (String matchFile : files) {
                        if (matchFile.endsWith(Identification.EXTENTION)) {
                            File newFile = new File(serializationFolder.getPath(), matchFile);
                            newFile.delete();
                        }
                        progressDialog.setValue(++cpt);
                    }
                    spectrumFactory.closeFiles();
                    sequenceFactory.closeFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                progressDialog.setVisible(false);
                progressDialog.dispose();
                System.exit(0);
            }
        }.start();
    }
}
