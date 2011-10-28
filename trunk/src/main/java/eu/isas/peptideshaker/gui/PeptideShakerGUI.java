package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
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
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.gui.preferencesdialogs.AnnotationPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.FeaturesPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.FollowupPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.ImportSettingsDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.ProjectDetailsDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.SearchPreferencesDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.SpectrumCountingPreferencesDialog;
import eu.isas.peptideshaker.gui.tabpanels.AnnotationPanel;
import eu.isas.peptideshaker.gui.tabpanels.GOEAPanel;
import eu.isas.peptideshaker.gui.tabpanels.OverviewPanel;
import eu.isas.peptideshaker.gui.tabpanels.ProteinStructurePanel;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.gui.tabpanels.QCPanel;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.gui.tabpanels.StatsPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSSettings;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.ModificationProfile;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SearchParameters;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main frame of the PeptideShaker.
 *
 * @author  Harald Barsnes
 * @author  Marc Vaudel
 */
public class PeptideShakerGUI extends javax.swing.JFrame implements ProgressDialogParent, ClipboardOwner {

    /**
     * The current PeptideShaker cps file.
     */
    private File currentPSFile = null;
    /**
     * The currently selected protein accession number.
     */
    private String selectedProteinAccession = null;
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
     * The QC Plots tab index.
     */
    private final int QC_PLOTS_TAB_INDEX = 6;
    /**
     * The GO Analysis tab index.
     */
    private final int GO_ANALYSIS_TAB_INDEX = 7;
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
     * The identification filter used for this project
     */
    private IdFilter idFilter = new IdFilter();
    /**
     * The project details
     */
    private ProjectDetails projectDetails = null;
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
     * The GO Analysis panel
     */
    private GOEAPanel goPanel;
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

        // load the list of recently used projects
        loadRecentProjectsList();

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

        annotationMenuBar = new javax.swing.JMenuBar();
        splitterMenu5 = new javax.swing.JMenu();
        ionsMenu = new javax.swing.JMenu();
        aIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        bIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        cIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        xIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        yIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        zIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu = new javax.swing.JMenu();
        lossMenu = new javax.swing.JMenu();
        h2oIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        nh3IonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        h3po4IonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        hpo3IonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        ch4osIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        adaptCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu1 = new javax.swing.JMenu();
        otherMenu = new javax.swing.JMenu();
        precursorCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        immoniumCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu2 = new javax.swing.JMenu();
        chargeMenu = new javax.swing.JMenu();
        singleChargeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        doubleChargeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        moreThanTwoChargesCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu3 = new javax.swing.JMenu();
        settingsMenu = new javax.swing.JMenu();
        allCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        barsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        bubbleScaleJMenuItem = new javax.swing.JMenuItem();
        intensityIonTableRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        mzIonTableRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        automaticAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        errorPlotTypeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu4 = new javax.swing.JMenu();
        exportGraphicsMenu = new javax.swing.JMenu();
        exportSpectrumMenu = new javax.swing.JMenu();
        exportSpectrumGraphicsJMenuItem = new javax.swing.JMenuItem();
        exportSequenceFragmentationGraphicsJMenuItem = new javax.swing.JMenuItem();
        exportIntensityHistogramGraphicsJMenuItem = new javax.swing.JMenuItem();
        exportMassErrorPlotGraphicsJMenuItem = new javax.swing.JMenuItem();
        bubblePlotJMenuItem = new javax.swing.JMenuItem();
        exportSpectrumValuesJMenuItem = new javax.swing.JMenuItem();
        splitterMenu6 = new javax.swing.JMenu();
        helpJMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        splitterMenu7 = new javax.swing.JMenu();
        ionTableButtonGroup = new javax.swing.ButtonGroup();
        gradientPanel = new javax.swing.JPanel();
        allTabsJTabbedPane = new javax.swing.JTabbedPane();
        overviewJPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        ptmJPanel = new javax.swing.JPanel();
        proteinStructureJPanel = new javax.swing.JPanel();
        annotationsJPanel = new javax.swing.JPanel();
        statsJPanel = new javax.swing.JPanel();
        qcJPanel = new javax.swing.JPanel();
        goJPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        newJMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        openJMenuItem = new javax.swing.JMenuItem();
        openRecentJMenu = new javax.swing.JMenu();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        projectPropertiesMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        searchParametersMenu = new javax.swing.JMenuItem();
        importFilterMenu = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        spectrumCountingMenuItem = new javax.swing.JMenuItem();
        filterMenu = new javax.swing.JMenu();
        proteinFilterJMenuItem = new javax.swing.JMenuItem();
        exportJMenu = new javax.swing.JMenu();
        identificationFeaturesMenu = new javax.swing.JMenuItem();
        followUpAnalysisMenu = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        exportProjectMenuItem = new javax.swing.JMenuItem();
        exportPrideXmlMenuItem = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        overViewTabViewMenu = new javax.swing.JMenu();
        proteinsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        peptidesAndPsmsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        spectrumJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sequenceCoverageJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        scoresJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        aboutJMenuItem = new javax.swing.JMenuItem();

        annotationMenuBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        annotationMenuBar.setOpaque(false);

        splitterMenu5.setText("|");
        splitterMenu5.setEnabled(false);
        annotationMenuBar.add(splitterMenu5);

        ionsMenu.setText("Ions");
        ionsMenu.setEnabled(false);

        aIonCheckBoxMenuItem.setText("a");
        aIonCheckBoxMenuItem.setToolTipText("a-ions");
        aIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(aIonCheckBoxMenuItem);

        bIonCheckBoxMenuItem.setText("b");
        bIonCheckBoxMenuItem.setToolTipText("b-ions");
        bIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(bIonCheckBoxMenuItem);

        cIonCheckBoxMenuItem.setText("c");
        cIonCheckBoxMenuItem.setToolTipText("c-ions");
        cIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(cIonCheckBoxMenuItem);
        ionsMenu.add(jSeparator6);

        xIonCheckBoxMenuItem.setText("x");
        xIonCheckBoxMenuItem.setToolTipText("x-ions");
        xIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(xIonCheckBoxMenuItem);

        yIonCheckBoxMenuItem.setText("y");
        yIonCheckBoxMenuItem.setToolTipText("y-ions");
        yIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(yIonCheckBoxMenuItem);

        zIonCheckBoxMenuItem.setText("z");
        zIonCheckBoxMenuItem.setToolTipText("z-ions");
        zIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(zIonCheckBoxMenuItem);

        annotationMenuBar.add(ionsMenu);

        splitterMenu.setText("|");
        splitterMenu.setEnabled(false);
        annotationMenuBar.add(splitterMenu);

        lossMenu.setText("Loss");
        lossMenu.setEnabled(false);

        h2oIonCheckBoxMenuItem.setText("<html>H<sub>2</sub>O</html>");
        h2oIonCheckBoxMenuItem.setToolTipText("Water Loss");
        h2oIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h2oIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(h2oIonCheckBoxMenuItem);

        nh3IonCheckBoxMenuItem.setText("<html>NH<sub>3</sub></html>");
        nh3IonCheckBoxMenuItem.setToolTipText("Ammonia Loss");
        nh3IonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nh3IonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(nh3IonCheckBoxMenuItem);

        h3po4IonCheckBoxMenuItem.setText("<html>H<sub>3</sub>PO<sub>4</sub></html>");
        h3po4IonCheckBoxMenuItem.setToolTipText("Phospo Loss - Type 1");
        h3po4IonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h3po4IonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(h3po4IonCheckBoxMenuItem);

        hpo3IonCheckBoxMenuItem.setText("<html>HPO<sub>3</sub></html>");
        hpo3IonCheckBoxMenuItem.setToolTipText("Phospo Loss - Type 2");
        hpo3IonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hpo3IonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(hpo3IonCheckBoxMenuItem);

        ch4osIonCheckBoxMenuItem.setText("<html>CH<sub>4</sub>OS</html>");
        ch4osIonCheckBoxMenuItem.setToolTipText("Sulpho Loss");
        ch4osIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ch4osIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(ch4osIonCheckBoxMenuItem);
        lossMenu.add(jSeparator7);

        adaptCheckBoxMenuItem.setText("Adapt");
        adaptCheckBoxMenuItem.setToolTipText("Adapt losses to sequence and modifications");
        adaptCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adaptCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(adaptCheckBoxMenuItem);

        annotationMenuBar.add(lossMenu);

        splitterMenu1.setText("|");
        splitterMenu1.setEnabled(false);
        annotationMenuBar.add(splitterMenu1);

        otherMenu.setText("Other");
        otherMenu.setEnabled(false);

        precursorCheckBoxMenuItem.setText("Precursor");
        precursorCheckBoxMenuItem.setToolTipText("Precursor ions");
        precursorCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorCheckBoxMenuItemActionPerformed(evt);
            }
        });
        otherMenu.add(precursorCheckBoxMenuItem);

        immoniumCheckBoxMenuItem.setText("Immonium Ion");
        immoniumCheckBoxMenuItem.setToolTipText("Immonium ions");
        immoniumCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                immoniumCheckBoxMenuItemActionPerformed(evt);
            }
        });
        otherMenu.add(immoniumCheckBoxMenuItem);

        annotationMenuBar.add(otherMenu);

        splitterMenu2.setText("|");
        splitterMenu2.setEnabled(false);
        annotationMenuBar.add(splitterMenu2);

        chargeMenu.setText("Charge");
        chargeMenu.setEnabled(false);

        singleChargeCheckBoxMenuItem.setText("+");
        singleChargeCheckBoxMenuItem.setToolTipText("Single Charge");
        singleChargeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                singleChargeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        chargeMenu.add(singleChargeCheckBoxMenuItem);

        doubleChargeCheckBoxMenuItem.setText("++");
        doubleChargeCheckBoxMenuItem.setToolTipText("Double Charge");
        doubleChargeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doubleChargeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        chargeMenu.add(doubleChargeCheckBoxMenuItem);

        moreThanTwoChargesCheckBoxMenuItem.setText(">2");
        moreThanTwoChargesCheckBoxMenuItem.setToolTipText("More than two charges");
        moreThanTwoChargesCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesCheckBoxMenuItemActionPerformed(evt);
            }
        });
        chargeMenu.add(moreThanTwoChargesCheckBoxMenuItem);

        annotationMenuBar.add(chargeMenu);

        splitterMenu3.setText("|");
        splitterMenu3.setEnabled(false);
        annotationMenuBar.add(splitterMenu3);

        settingsMenu.setText("Settings");
        settingsMenu.setEnabled(false);

        allCheckBoxMenuItem.setText("Show All Peaks");
        allCheckBoxMenuItem.setToolTipText("Show all peaks or just the annotated peaks");
        allCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(allCheckBoxMenuItem);

        barsCheckBoxMenuItem.setText("Show Bars");
        barsCheckBoxMenuItem.setToolTipText("Add bars highlighting the fragment ion types");
        barsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                barsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(barsCheckBoxMenuItem);

        bubbleScaleJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        bubbleScaleJMenuItem.setText("Bubble Plot Scale");
        bubbleScaleJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bubbleScaleJMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(bubbleScaleJMenuItem);

        ionTableButtonGroup.add(intensityIonTableRadioButtonMenuItem);
        intensityIonTableRadioButtonMenuItem.setSelected(true);
        intensityIonTableRadioButtonMenuItem.setText("Intensity Ion Table");
        intensityIonTableRadioButtonMenuItem.setToolTipText("Bar charts with peak intensities");
        intensityIonTableRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intensityIonTableRadioButtonMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(intensityIonTableRadioButtonMenuItem);

        ionTableButtonGroup.add(mzIonTableRadioButtonMenuItem);
        mzIonTableRadioButtonMenuItem.setText("m/z Ion Table");
        mzIonTableRadioButtonMenuItem.setToolTipText("Traditional ion table with m/z values");
        mzIonTableRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mzIonTableRadioButtonMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(mzIonTableRadioButtonMenuItem);
        settingsMenu.add(jSeparator5);

        automaticAnnotationCheckBoxMenuItem.setSelected(true);
        automaticAnnotationCheckBoxMenuItem.setText("Automatic Annotation");
        automaticAnnotationCheckBoxMenuItem.setToolTipText("Use automatic annotation");
        automaticAnnotationCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                automaticAnnotationCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(automaticAnnotationCheckBoxMenuItem);

        errorPlotTypeCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        errorPlotTypeCheckBoxMenuItem.setSelected(true);
        errorPlotTypeCheckBoxMenuItem.setText("Absolute Mass Error Plot");
        errorPlotTypeCheckBoxMenuItem.setToolTipText("Plot the mass error in Da or ppm ");
        errorPlotTypeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorPlotTypeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(errorPlotTypeCheckBoxMenuItem);

        annotationMenuBar.add(settingsMenu);

        splitterMenu4.setText("|");
        splitterMenu4.setEnabled(false);
        annotationMenuBar.add(splitterMenu4);

        exportGraphicsMenu.setText("Export");
        exportGraphicsMenu.setEnabled(false);

        exportSpectrumMenu.setText("Figure");

        exportSpectrumGraphicsJMenuItem.setText("Spectrum");
        exportSpectrumGraphicsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumGraphicsJMenuItemActionPerformed(evt);
            }
        });
        exportSpectrumMenu.add(exportSpectrumGraphicsJMenuItem);

        exportSequenceFragmentationGraphicsJMenuItem.setText("Sequence Fragmentation");
        exportSequenceFragmentationGraphicsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSequenceFragmentationGraphicsJMenuItemActionPerformed(evt);
            }
        });
        exportSpectrumMenu.add(exportSequenceFragmentationGraphicsJMenuItem);

        exportIntensityHistogramGraphicsJMenuItem.setText("Intensity Histogram");
        exportIntensityHistogramGraphicsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportIntensityHistogramGraphicsJMenuItemActionPerformed(evt);
            }
        });
        exportSpectrumMenu.add(exportIntensityHistogramGraphicsJMenuItem);

        exportMassErrorPlotGraphicsJMenuItem.setText("Mass Error Plot");
        exportMassErrorPlotGraphicsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMassErrorPlotGraphicsJMenuItemActionPerformed(evt);
            }
        });
        exportSpectrumMenu.add(exportMassErrorPlotGraphicsJMenuItem);

        exportGraphicsMenu.add(exportSpectrumMenu);

        bubblePlotJMenuItem.setText("Bubble Plot");
        bubblePlotJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bubblePlotJMenuItemActionPerformed(evt);
            }
        });
        exportGraphicsMenu.add(bubblePlotJMenuItem);

        exportSpectrumValuesJMenuItem.setText("Spectrum as MGF");
        exportSpectrumValuesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumValuesJMenuItemActionPerformed(evt);
            }
        });
        exportGraphicsMenu.add(exportSpectrumValuesJMenuItem);

        annotationMenuBar.add(exportGraphicsMenu);

        splitterMenu6.setText("|");
        splitterMenu6.setEnabled(false);
        annotationMenuBar.add(splitterMenu6);

        helpJMenu.setText("Help");
        helpJMenu.setEnabled(false);

        helpMenuItem.setText("Help");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpJMenu.add(helpMenuItem);

        annotationMenuBar.add(helpJMenu);

        splitterMenu7.setText("|");
        splitterMenu7.setEnabled(false);
        annotationMenuBar.add(splitterMenu7);

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

        qcJPanel.setOpaque(false);
        qcJPanel.setLayout(new javax.swing.BoxLayout(qcJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("QC Plots", qcJPanel);

        goJPanel.setOpaque(false);
        goJPanel.setLayout(new javax.swing.BoxLayout(goJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("GO Analysis", goJPanel);

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
        fileJMenu.add(jSeparator8);

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

        openRecentJMenu.setText("Open Recent Project");
        fileJMenu.add(openRecentJMenu);
        fileJMenu.add(jSeparator2);

        projectPropertiesMenuItem.setText("Project Properties");
        projectPropertiesMenuItem.setEnabled(false);
        projectPropertiesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projectPropertiesMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(projectPropertiesMenuItem);
        fileJMenu.add(jSeparator1);

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
        fileJMenu.add(jSeparator9);

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

        importFilterMenu.setText("Import Filters");
        importFilterMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFilterMenuActionPerformed(evt);
            }
        });
        editMenu.add(importFilterMenu);
        editMenu.add(jSeparator4);

        annotationPreferencesMenu.setText("Spectrum Annotations");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        editMenu.add(annotationPreferencesMenu);

        spectrumCountingMenuItem.setText("MS2 Quantification");
        spectrumCountingMenuItem.setEnabled(false);
        spectrumCountingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumCountingMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(spectrumCountingMenuItem);

        menuBar.add(editMenu);

        filterMenu.setMnemonic('L');
        filterMenu.setText("Filter");

        proteinFilterJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        proteinFilterJMenuItem.setText("Proteins");
        proteinFilterJMenuItem.setEnabled(false);
        proteinFilterJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinFilterJMenuItemActionPerformed(evt);
            }
        });
        filterMenu.add(proteinFilterJMenuItem);

        menuBar.add(filterMenu);

        exportJMenu.setMnemonic('x');
        exportJMenu.setText("Export");

        identificationFeaturesMenu.setText("Identification Features");
        identificationFeaturesMenu.setEnabled(false);
        identificationFeaturesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                identificationFeaturesMenuActionPerformed(evt);
            }
        });
        exportJMenu.add(identificationFeaturesMenu);

        followUpAnalysisMenu.setText("Follow-Up Analysis");
        followUpAnalysisMenu.setEnabled(false);
        followUpAnalysisMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                followUpAnalysisMenuActionPerformed(evt);
            }
        });
        exportJMenu.add(followUpAnalysisMenu);
        exportJMenu.add(jSeparator10);

        exportProjectMenuItem.setText("PeptideShaker Project");
        exportProjectMenuItem.setToolTipText("Export a PeptideShaker project as a single zip file");
        exportProjectMenuItem.setEnabled(false);
        exportProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProjectMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportProjectMenuItem);

        exportPrideXmlMenuItem.setText("PRIDE XML");
        exportPrideXmlMenuItem.setToolTipText("Export a PeptideShaker project as a PRIDE XML file");
        exportPrideXmlMenuItem.setEnabled(false);
        exportPrideXmlMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPrideXmlMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportPrideXmlMenuItem);

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

        // reset enzymes, ptms and preferences
        loadEnzymes();
        loadModifications();
        setDefaultPreferences();

        if (!dataSaved && experiment != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + experiment.getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                NewDialog openDialog = new NewDialog(this, true);
                openDialog.setVisible(true);
            } else if (value == JOptionPane.CANCEL_OPTION) {
                // do nothing
            } else { // no option
                NewDialog openDialog = new NewDialog(this, true);
                openDialog.setVisible(true);
            }
        } else {
            NewDialog openDialog = new NewDialog(this, true);
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
                        experiment.addUrParam(new PSSettings(searchParameters, annotationPreferences, spectrumCountingPreferences, projectDetails));

                        String folderPath = selectedFile.substring(0, selectedFile.lastIndexOf("."));
                        File newFolder = new File(folderPath + "_cps");
                        if (newFolder.exists()) {
                            String[] fileList = newFolder.list();
                            progressDialog.setMax(fileList.length);
                            progressDialog.setTitle("Deleting Old Matches. Please Wait...");
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

                        updateRecentProjectsList(new File(selectedFile));

                        JOptionPane.showMessageDialog(tempRef, "Project successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
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
        new SearchPreferencesDialog(this, false);
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
     * Export the spectrum as a figure.
     * 
     * @param evt 
     */
    private void exportSpectrumGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumGraphicsJMenuItemActionPerformed

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, true, (Component) overviewPanel.getSpectrum());
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
        }
}//GEN-LAST:event_exportSpectrumGraphicsJMenuItemActionPerformed

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

        // make sure that the same protein and peptide are selected in both 
        // the overview and protein structure tabs
        if (selectedIndex == OVER_VIEW_TAB_INDEX) {

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
        } else if (selectedIndex == STRUCTURES_TAB_INDEX) {

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

        // update the basic protein annotation
        if (selectedIndex == ANNOTATION_TAB_INDEX) {
            annotationPanel.updateBasicProteinAnnotation(selectedProteinAccession);
        }

        // move the spectrum annotation menu bar and set the intensity slider value
        if (selectedIndex == OVER_VIEW_TAB_INDEX) {
            overviewPanel.showSpectrumAnnotationMenu();
            overviewPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
        } else if (selectedIndex == SPECTRUM_ID_TAB_INDEX) {
            spectrumIdentificationPanel.showSpectrumAnnotationMenu();
            spectrumIdentificationPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
        } else if (selectedIndex == MODIFICATIONS_TAB_INDEX) {
            ptmPanel.showSpectrumAnnotationMenu();
            ptmPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
        }

        updateSpectrumAnnotations();

        // disable the protein filter option if a tab other than the overview tab is selected
        proteinFilterJMenuItem.setEnabled(selectedIndex == OVER_VIEW_TAB_INDEX);
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
     * Test if there are unsaved changes and if so asks the user if he/she 
     * wants to save these. If not closes the tool.
     * 
     * @param evt 
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Edit the use of relative error (ppm) or absolute error (Da) in the mass 
     * error plot.
     * 
     * @param evt 
     */
    private void errorPlotTypeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_errorPlotTypeCheckBoxMenuItemActionPerformed
        useRelativeError = !errorPlotTypeCheckBoxMenuItem.isSelected();
        overviewPanel.updateSpectrum(); // @TODO: verify that this is correct!
    }//GEN-LAST:event_errorPlotTypeCheckBoxMenuItemActionPerformed

    /**
     * Turns the hiding of the scores columns on or off.
     * 
     * @param evt 
     */
    private void scoresJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scoresJCheckBoxMenuItemActionPerformed
        overviewPanel.hideScores(!scoresJCheckBoxMenuItem.isSelected());
        proteinStructurePanel.hideScores(!scoresJCheckBoxMenuItem.isSelected());

        // make sure that the jsparklines are showing correctly
        sparklinesJCheckBoxMenuItemActionPerformed(null);
    }//GEN-LAST:event_scoresJCheckBoxMenuItemActionPerformed

    /**
     * Open a file chooser to open an existing PeptideShaker project.
     * 
     * @param evt 
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed

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
            } else { // no option
                // do nothing
            }
        }


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

            File newFile = fileChooser.getSelectedFile();
            setLastSelectedFolder(newFile.getAbsolutePath());

            if (!newFile.getName().toLowerCase().endsWith("cps")) {
                JOptionPane.showMessageDialog(this, "Not a PeptideShaker file (.cps).",
                        "Wrong File.", JOptionPane.ERROR_MESSAGE);
            } else {
                updateRecentProjectsList(newFile);
                importPeptideShakerFile(newFile);
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

    private void identificationFeaturesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_identificationFeaturesMenuActionPerformed
        new FeaturesPreferencesDialog(this);
    }//GEN-LAST:event_identificationFeaturesMenuActionPerformed

    private void followUpAnalysisMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_followUpAnalysisMenuActionPerformed
        new FollowupPreferencesDialog(this);
    }//GEN-LAST:event_followUpAnalysisMenuActionPerformed

private void aIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_aIonCheckBoxMenuItemActionPerformed

private void bIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_bIonCheckBoxMenuItemActionPerformed

private void cIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_cIonCheckBoxMenuItemActionPerformed

private void xIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_xIonCheckBoxMenuItemActionPerformed

private void yIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_yIonCheckBoxMenuItemActionPerformed

private void zIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_zIonCheckBoxMenuItemActionPerformed

private void h2oIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_h2oIonCheckBoxMenuItemActionPerformed

private void nh3IonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3IonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_nh3IonCheckBoxMenuItemActionPerformed

private void h3po4IonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h3po4IonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_h3po4IonCheckBoxMenuItemActionPerformed

private void hpo3IonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hpo3IonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_hpo3IonCheckBoxMenuItemActionPerformed

private void ch4osIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ch4osIonCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_ch4osIonCheckBoxMenuItemActionPerformed

private void precursorCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_precursorCheckBoxMenuItemActionPerformed

private void immoniumCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_immoniumCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_immoniumCheckBoxMenuItemActionPerformed

private void singleChargeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_singleChargeCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_singleChargeCheckBoxMenuItemActionPerformed

private void doubleChargeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doubleChargeCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_doubleChargeCheckBoxMenuItemActionPerformed

private void moreThanTwoChargesCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_moreThanTwoChargesCheckBoxMenuItemActionPerformed

private void allCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_allCheckBoxMenuItemActionPerformed

private void barsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_barsCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_barsCheckBoxMenuItemActionPerformed

private void intensityIonTableRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intensityIonTableRadioButtonMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_intensityIonTableRadioButtonMenuItemActionPerformed

private void mzIonTableRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mzIonTableRadioButtonMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_mzIonTableRadioButtonMenuItemActionPerformed

    /**
     * Opens the wanted Help window.
     * 
     * @param evt 
     */
private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed

    int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

    setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

    if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
        int spectrumTabIndex = overviewPanel.getSelectedSpectrumTabIndex();

        if (spectrumTabIndex == 0) {
            new HelpWindow(this, getClass().getResource("/helpFiles/IonTable.html"));
        } else if (spectrumTabIndex == 1) {
            new HelpWindow(this, getClass().getResource("/helpFiles/BubblePlot.html"));
        } else if (spectrumTabIndex == 2) {
            new HelpWindow(this, getClass().getResource("/helpFiles/SpectrumPanel.html"));
        }
    } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
        new HelpWindow(this, getClass().getResource("/helpFiles/SpectrumPanel.html"));
    } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
        new HelpWindow(this, getClass().getResource("/helpFiles/SpectrumPanel.html"));
    }

    setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Save the current spectrum/spectra to an MGF file.
     */
private void exportSpectrumValuesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumValuesJMenuItemActionPerformed
    int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

    String spectrumAsMgf = null;

    if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
        spectrumAsMgf = overviewPanel.getSpectrumAsMgf();
    } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
        spectrumAsMgf = spectrumIdentificationPanel.getSpectrumAsMgf();
    } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
        spectrumAsMgf = ptmPanel.getSpectrumAsMgf();
    }

    if (spectrumAsMgf != null) {

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf") || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "(Mascot Generic Format) *.mgf";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            lastSelectedFolder = fileChooser.getSelectedFile().getAbsolutePath();

            String path = fileChooser.getSelectedFile().getAbsolutePath();

            if (!path.endsWith(".mgf")) {
                path += ".mgf";
            }

            int saveFile = JOptionPane.YES_OPTION;

            if (new File(path).exists()) {
                saveFile = JOptionPane.showConfirmDialog(progressDialog,
                        "Should " + path + " be overwritten?", "Overwrite",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            }

            if (saveFile == JOptionPane.YES_OPTION) {

                try {
                    FileWriter w = new FileWriter(path);
                    BufferedWriter bw = new BufferedWriter(w);
                    bw.write(spectrumAsMgf);
                    bw.close();
                    w.close();

                    JOptionPane.showMessageDialog(this, "Spectrum saved to " + path + ".",
                            "File Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "An error occured while saving " + path + ".\n"
                            + "See conf/PeptideShaker.log for details.", "Save Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }
}//GEN-LAST:event_exportSpectrumValuesJMenuItemActionPerformed

    /**
     * Set if the current annotation is to be used for all spectra.
     * 
     * @param evt 
     */
private void automaticAnnotationCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_automaticAnnotationCheckBoxMenuItemActionPerformed

    if (automaticAnnotationCheckBoxMenuItem.isSelected()) {
        adaptCheckBoxMenuItem.setSelected(true);
        annotationPreferences.resetAutomaticAnnotation();

        singleChargeCheckBoxMenuItem.setSelected(false);
        doubleChargeCheckBoxMenuItem.setSelected(false);
        moreThanTwoChargesCheckBoxMenuItem.setSelected(false);

        for (int charge : annotationPreferences.getValidatedCharges()) {
            if (charge == 1) {
                singleChargeCheckBoxMenuItem.setSelected(true);
            } else if (charge == 2) {
                doubleChargeCheckBoxMenuItem.setSelected(true);
            } else if (charge > 2) {
                moreThanTwoChargesCheckBoxMenuItem.setSelected(true);
            }
        }
    }

    updateAnnotationPreferences();
}//GEN-LAST:event_automaticAnnotationCheckBoxMenuItemActionPerformed

    /**
     * Update annotations.
     * 
     * @param evt 
     */
private void adaptCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adaptCheckBoxMenuItemActionPerformed
    updateAnnotationPreferences();
}//GEN-LAST:event_adaptCheckBoxMenuItemActionPerformed

private void projectPropertiesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projectPropertiesMenuItemActionPerformed
    new ProjectDetailsDialog(this);
}//GEN-LAST:event_projectPropertiesMenuItemActionPerformed

    /**
     * Export the sequence fragmentation as a figure.
     * 
     * @param evt 
     */
    private void exportSequenceFragmentationGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSequenceFragmentationGraphicsJMenuItemActionPerformed

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, true, (Component) overviewPanel.getSequenceFragmentationPlot());
        }
//        else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
//        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
//        }

        // @TODO: add export support for the other tabs
    }//GEN-LAST:event_exportSequenceFragmentationGraphicsJMenuItemActionPerformed

    /**
     * Export the intensity histogram as a figure.
     * 
     * @param evt 
     */
    private void exportIntensityHistogramGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportIntensityHistogramGraphicsJMenuItemActionPerformed
        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {

            ChartPanel chartPanel = overviewPanel.getIntensityHistogramPlot().getChartPanel();
            ChartPanel tempChartPanel = new ChartPanel(chartPanel.getChart());
            tempChartPanel.setBounds(new Rectangle(chartPanel.getBounds().width * 5, chartPanel.getBounds().height * 5));

            new ExportGraphicsDialog(this, true, tempChartPanel);
        }
//        else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
//        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
//        }

        // @TODO: add export support for the other tabs
    }//GEN-LAST:event_exportIntensityHistogramGraphicsJMenuItemActionPerformed

    /**
     * Export the mass error plot as a figure.
     * 
     * @param evt 
     */
    private void exportMassErrorPlotGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMassErrorPlotGraphicsJMenuItemActionPerformed
        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            if (overviewPanel.getMassErrorPlot() != null) {

                ChartPanel chartPanel = overviewPanel.getMassErrorPlot().getChartPanel();
                ChartPanel tempChartPanel = new ChartPanel(chartPanel.getChart());
                tempChartPanel.setBounds(new Rectangle(chartPanel.getBounds().width * 5, chartPanel.getBounds().height * 5));

                new ExportGraphicsDialog(this, true, tempChartPanel);
            } else {
                JOptionPane.showMessageDialog(this, "No mass error plot to export!", "Export Error", JOptionPane.INFORMATION_MESSAGE);
            }
        }
//        else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
//        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
//        }

        // @TODO: add export support for the other tabs
    }//GEN-LAST:event_exportMassErrorPlotGraphicsJMenuItemActionPerformed

    /**
     * Opens the filter settings dialog.
     * 
     * @param evt 
     */
    private void importFilterMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importFilterMenuActionPerformed
        new ImportSettingsDialog(this, null, false);
    }//GEN-LAST:event_importFilterMenuActionPerformed

    /**
     * Export the current PeptideShaker project as a single zip file.
     * 
     * @param evt 
     */
    private void exportProjectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProjectMenuItemActionPerformed

        if (dataSaved) {
            JOptionPane.showMessageDialog(this, "You first need to save the data.", "Unsaved Data", JOptionPane.INFORMATION_MESSAGE);

            // save the data first
            saveMenuItemActionPerformed(null);
        } else {

            if (currentPSFile != null) {

                final PeptideShakerGUI tempRef = this; // needed due to threading issues
                progressDialog = new ProgressDialogX(this, this, true);
                progressDialog.doNothingOnClose();

                new Thread(new Runnable() {

                    public void run() {
                        progressDialog.setIndeterminate(true);
                        progressDialog.setTitle("Exporting Project. Please Wait...");
                        progressDialog.setVisible(true);
                    }
                }, "ProgressDialog").start();

                new Thread("ExportThread") {

                    @Override
                    public void run() {

                        progressDialog.setTitle("Getting FASTA File. Please Wait...");
                        
                        File projectFolder = currentPSFile.getParentFile();
                        File fastaLocation = tempRef.getSearchParameters().getFastaFile();
                        ArrayList<String> dataFiles = new ArrayList<String>();
                        dataFiles.add(tempRef.getSearchParameters().getFastaFile().getAbsolutePath());

                        File indexFile = new File(fastaLocation.getParentFile(), fastaLocation.getName() + ".cui");

                        if (indexFile.exists()) {
                            dataFiles.add(indexFile.getAbsolutePath());
                        }

                        
                        progressDialog.setTitle("Getting Spectrum Files. Please Wait...");
                        progressDialog.setIndeterminate(false);
                        progressDialog.setValue(0);
                        progressDialog.setMax(getSearchParameters().getSpectrumFiles().size());

                        ArrayList<String> names = new ArrayList<String>();

                        for (String filePath : getSearchParameters().getSpectrumFiles()) {

                            progressDialog.incrementValue();

                            File spectrumLocation = new File(filePath);

                            if (spectrumLocation.exists() && !names.contains(spectrumLocation.getName())) {
                                
                                names.add(spectrumLocation.getName());
                                dataFiles.add(spectrumLocation.getAbsolutePath());

                                indexFile = new File(spectrumLocation.getParentFile(), spectrumLocation.getName() + ".cui");

                                if (indexFile.exists()) {
                                    dataFiles.add(indexFile.getAbsolutePath());
                                }
                            }
                        }


                        progressDialog.setTitle("Zipping Project. Please Wait...");
                        progressDialog.setIndeterminate(true);


                        // zip the project
                        String zipFileFileName = currentPSFile.getName() + "_project.zip";
                        File zipFile = new File(projectFolder, zipFileFileName);

                        try {
                            FileOutputStream fos = new FileOutputStream(zipFile);
                            ZipOutputStream out = new ZipOutputStream(fos);
                            BufferedInputStream origin = null;
                            final int BUFFER = 2048;
                            byte data[] = new byte[BUFFER];

                            progressDialog.setTitle("Zipping CPS File. Please Wait...");

                            // add the cps file
                            FileInputStream fi = new FileInputStream(currentPSFile);
                            origin = new BufferedInputStream(fi, BUFFER);
                            ZipEntry entry = new ZipEntry(currentPSFile.getName());
                            out.putNextEntry(entry);
                            int count;
                            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                                out.write(data, 0, count);
                            }
                            origin.close();


                            // add the cps folder
                            String cpsFolderName = currentPSFile.getName().substring(0, currentPSFile.getName().length() - 4) + "_cps";
                            File cpsFolder = new File(projectFolder, cpsFolderName);

                            String files[] = cpsFolder.list();

                            progressDialog.setTitle("Zipping CPS Folder. Please Wait...");
                            progressDialog.setIndeterminate(false);
                            progressDialog.setValue(0);
                            progressDialog.setMax(files.length);

                            for (int i = 0; i < files.length; i++) {

                                progressDialog.incrementValue();

                                fi = new FileInputStream(new File(cpsFolder, files[i]));
                                origin = new BufferedInputStream(fi, BUFFER);
                                entry = new ZipEntry(cpsFolderName + File.separator + files[i]);
                                out.putNextEntry(entry);
                                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                                    out.write(data, 0, count);
                                }
                                origin.close();
                            }


                            // add the data files
                            progressDialog.setTitle("Zipping FASTA and MGFs. Please Wait...");
                            progressDialog.setIndeterminate(false);
                            progressDialog.setValue(0);
                            progressDialog.setMax(dataFiles.size());

                            for (int i = 0; i < dataFiles.size(); i++) {

                                progressDialog.incrementValue();

                                fi = new FileInputStream(new File(dataFiles.get(i)));
                                origin = new BufferedInputStream(fi, BUFFER);
                                entry = new ZipEntry("data" + File.separator + new File(dataFiles.get(i)).getName());
                                out.putNextEntry(entry);
                                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                                    out.write(data, 0, count);
                                }
                                origin.close();
                            }


                            progressDialog.setIndeterminate(true);
                            progressDialog.setTitle("Cleaning Up. Please Wait...");

                            out.close();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            progressDialog.setVisible(false);
                            progressDialog.dispose();
                            JOptionPane.showMessageDialog(tempRef, "Could not zip files.", "Zip Error", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            progressDialog.setVisible(false);
                            progressDialog.dispose();
                            JOptionPane.showMessageDialog(tempRef, "Could not zip files.", "Zip Error", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        // get the size (in MB) of the zip file
                        final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                        double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);

                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(tempRef, "Project zipped to \'" + zipFileFileName + "\' (" + sizeOfZippedFile + "MB)", "Export Sucessful", JOptionPane.INFORMATION_MESSAGE);
                    }
                }.start();
            }
        }
    }//GEN-LAST:event_exportProjectMenuItemActionPerformed

    /**
     * Export the current PeptideShaker project as a PRIDE XML file.
     * 
     * @param evt 
     */
    private void exportPrideXmlMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPrideXmlMenuItemActionPerformed
        JOptionPane.showMessageDialog(this, "Not yet implemented.", "Not Implemented", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_exportPrideXmlMenuItemActionPerformed

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
            int max = 3 * identification.getProteinIdentification().size()
                    + 2 * identification.getPeptideIdentification().size()
                    + 2 * identification.getSpectrumIdentification().size()
                    + 1;
            progressDialog.setMax(max);
            progressDialog.doNothingOnClose();

            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setTitle("Loading Data. Please wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("DisplayThread") {

                @Override
                public void run() {

                    // change the peptide shaker icon to a "waiting version"
                    tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                    try {

                        if (displaySpectrum) {
                            progressDialog.setTitle("Loading Spectrum ID Tab. Please Wait...");
                            spectrumIdentificationPanel.displayResults(progressDialog);
                            progressDialog.setTitle("Loading Modifications Tab. Please Wait...");
                            ptmPanel.displayResults(progressDialog);
                        } else {
                            spectrumJPanel.setEnabled(false);
                            ptmPanel.setEnabled(false);
                            progressDialog.setValue(identification.getPeptideIdentification().size() + identification.getSpectrumIdentification().size());
                        }

                        progressDialog.setTitle("Loading Overview Tab. Please Wait...");
                        overviewPanel.displayResults(progressDialog);

                        if (updateValidationTab) {
                            progressDialog.setTitle("Loading Validation Tab. Please Wait...");
                            statsPanel.displayResults();
                        }
                        progressDialog.incrementValue();

                        progressDialog.setTitle("Loading Structure Tab. Please Wait...");
                        proteinStructurePanel.displayResults(progressDialog);

                        progressDialog.setTitle("Loading QC Plots Tab. Please Wait...");
                        qcPanel.displayResults(progressDialog);

                        progressDialog.setTitle("Loading GO Analysis Tab. Please Wait...");
                        goPanel.displayResults(progressDialog, false);


                        allTabsJTabbedPaneStateChanged(null);

                        // make sure that all panels are looking the way they should
                        repaintPanels();

                        progressDialog.setVisible(false);
                        progressDialog.dispose();

                        // return the peptide shaker icon to the standard version
                        tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                        // enable the menu items depending on a project being open
                        saveMenuItem.setEnabled(true);
                        proteinFilterJMenuItem.setEnabled(true);
                        identificationFeaturesMenu.setEnabled(true);
                        followUpAnalysisMenu.setEnabled(true);
                        exportProjectMenuItem.setEnabled(true);
                        exportPrideXmlMenuItem.setEnabled(true);
                        projectPropertiesMenuItem.setEnabled(true);
                        spectrumCountingMenuItem.setEnabled(true);
                        ionsMenu.setEnabled(true);
                        lossMenu.setEnabled(true);
                        otherMenu.setEnabled(true);
                        chargeMenu.setEnabled(true);
                        settingsMenu.setEnabled(true);
                        exportGraphicsMenu.setEnabled(true);
                        helpJMenu.setEnabled(true);

                        // return the peptide shaker icon to the standard version
                        tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    } catch (Exception e) {

                        // return the peptide shaker icon to the standard version
                        tempRef.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                        if (progressDialog != null) {
                            progressDialog.setVisible(false);
                            progressDialog.dispose();
                        }

                        e.printStackTrace();
                        catchException(e);
                        JOptionPane.showMessageDialog(null, "A problem occured when loading the data.\n"
                                + "See /conf/PeptideShaker.log for more details.", "Loading Failed!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "A problem occured while displaying results. Please send the log file to the developers.", "Display Problem", JOptionPane.ERROR_MESSAGE);
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
    private javax.swing.JCheckBoxMenuItem aIonCheckBoxMenuItem;
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JCheckBoxMenuItem adaptCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem allCheckBoxMenuItem;
    private javax.swing.JTabbedPane allTabsJTabbedPane;
    private javax.swing.JMenuBar annotationMenuBar;
    private javax.swing.JMenuItem annotationPreferencesMenu;
    private javax.swing.JPanel annotationsJPanel;
    private javax.swing.JCheckBoxMenuItem automaticAnnotationCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem bIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem barsCheckBoxMenuItem;
    private javax.swing.JMenuItem bubblePlotJMenuItem;
    private javax.swing.JMenuItem bubbleScaleJMenuItem;
    private javax.swing.JCheckBoxMenuItem cIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem ch4osIonCheckBoxMenuItem;
    private javax.swing.JMenu chargeMenu;
    private javax.swing.JCheckBoxMenuItem doubleChargeCheckBoxMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem errorPlotTypeCheckBoxMenuItem;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenu exportGraphicsMenu;
    private javax.swing.JMenuItem exportIntensityHistogramGraphicsJMenuItem;
    private javax.swing.JMenu exportJMenu;
    private javax.swing.JMenuItem exportMassErrorPlotGraphicsJMenuItem;
    private javax.swing.JMenuItem exportPrideXmlMenuItem;
    private javax.swing.JMenuItem exportProjectMenuItem;
    private javax.swing.JMenuItem exportSequenceFragmentationGraphicsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumGraphicsJMenuItem;
    private javax.swing.JMenu exportSpectrumMenu;
    private javax.swing.JMenuItem exportSpectrumValuesJMenuItem;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JMenu filterMenu;
    private javax.swing.JMenuItem followUpAnalysisMenu;
    private javax.swing.JPanel goJPanel;
    private javax.swing.JPanel gradientPanel;
    private javax.swing.JCheckBoxMenuItem h2oIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem h3po4IonCheckBoxMenuItem;
    private javax.swing.JMenu helpJMenu;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JCheckBoxMenuItem hpo3IonCheckBoxMenuItem;
    private javax.swing.JMenuItem identificationFeaturesMenu;
    private javax.swing.JCheckBoxMenuItem immoniumCheckBoxMenuItem;
    private javax.swing.JMenuItem importFilterMenu;
    private javax.swing.JRadioButtonMenuItem intensityIonTableRadioButtonMenuItem;
    private javax.swing.ButtonGroup ionTableButtonGroup;
    private javax.swing.JMenu ionsMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JMenu lossMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JCheckBoxMenuItem moreThanTwoChargesCheckBoxMenuItem;
    private javax.swing.JRadioButtonMenuItem mzIonTableRadioButtonMenuItem;
    private javax.swing.JMenuItem newJMenuItem;
    private javax.swing.JCheckBoxMenuItem nh3IonCheckBoxMenuItem;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JMenu openRecentJMenu;
    private javax.swing.JMenu otherMenu;
    private javax.swing.JMenu overViewTabViewMenu;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JCheckBoxMenuItem peptidesAndPsmsJCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem precursorCheckBoxMenuItem;
    private javax.swing.JMenuItem projectPropertiesMenuItem;
    private javax.swing.JMenuItem proteinFilterJMenuItem;
    private javax.swing.JPanel proteinStructureJPanel;
    private javax.swing.JCheckBoxMenuItem proteinsJCheckBoxMenuItem;
    private javax.swing.JPanel ptmJPanel;
    private javax.swing.JPanel qcJPanel;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem scoresJCheckBoxMenuItem;
    private javax.swing.JMenuItem searchParametersMenu;
    private javax.swing.JCheckBoxMenuItem sequenceCoverageJCheckBoxMenuItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JCheckBoxMenuItem singleChargeCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JMenuItem spectrumCountingMenuItem;
    private javax.swing.JCheckBoxMenuItem spectrumJCheckBoxMenuItem;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JMenu splitterMenu;
    private javax.swing.JMenu splitterMenu1;
    private javax.swing.JMenu splitterMenu2;
    private javax.swing.JMenu splitterMenu3;
    private javax.swing.JMenu splitterMenu4;
    private javax.swing.JMenu splitterMenu5;
    private javax.swing.JMenu splitterMenu6;
    private javax.swing.JMenu splitterMenu7;
    private javax.swing.JPanel statsJPanel;
    private javax.swing.JMenu viewJMenu;
    private javax.swing.JCheckBoxMenuItem xIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem yIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem zIonCheckBoxMenuItem;
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
    public String getJarFilePath() {
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
        searchParameters.setFragmentIonAccuracy(0.5);
        searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.PPM);
        searchParameters.setPrecursorAccuracy(10);
        searchParameters.setIonSearched1("b");
        searchParameters.setIonSearched2("y");
        loadModificationProfile(profileFile);
        annotationPreferences.setAnnotationLevel(0.75);
        annotationPreferences.useAutomaticAnnotation(true);
        updateAnnotationPreferencesFromSearchSettings();
        spectrumCountingPreferences.setSelectedMethod(SpectralCountingMethod.NSAF);
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
        annotationPreferences.setFragmentIonAccuracy(searchParameters.getFragmentIonAccuracy());
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
     * Updates the annotations in the selected tab.
     */
    public void updateSpectrumAnnotations() {

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            overviewPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
            overviewPanel.setAccuracySliderValue((int) ((annotationPreferences.getFragmentIonAccuracy() / searchParameters.getFragmentIonAccuracy()) * 100));
            overviewPanel.updateSpectrum();
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            spectrumIdentificationPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
            spectrumIdentificationPanel.setAccuracySliderValue((int) ((annotationPreferences.getFragmentIonAccuracy() / searchParameters.getFragmentIonAccuracy()) * 100));
            spectrumIdentificationPanel.updateSpectrum();
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            ptmPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
            ptmPanel.setAccuracySliderValue((int) ((annotationPreferences.getFragmentIonAccuracy() / searchParameters.getFragmentIonAccuracy()) * 100));
            ptmPanel.updateSpectrum();
        }
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

        // update the color coding in the other tabs
        updatePtmColorCoding();
    }

    /**
     * Update the color coding in all tabs.
     */
    public void updatePtmColorCoding() {
        ptmPanel.updatePtmColors();
        overviewPanel.updatePtmColors();
        spectrumIdentificationPanel.updatePtmColors();
        proteinStructurePanel.updatePtmColors();
    }

    /**
     * Loads the modification profile from the given file
     * @param aFile the given file
     */
    private void loadModificationProfile(File aFile) {
        try {
            FileInputStream fis = new FileInputStream(aFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            ModificationProfile modificationProfile = (ModificationProfile) in.readObject();
            in.close();
            searchParameters.setModificationProfile(modificationProfile);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, aFile.getName() + " not found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading " + aFile.getName() + ".\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading " + aFile.getName() + ".\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
        } catch (ClassCastException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading " + aFile.getName() + ".\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
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
        this.selectedProteinAccession = selectedProteinAccession;
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
     * @param proteinAccession   the protein to get the database link for
     * @return                   the transformed accession number
     */
    public String addDatabaseLink(String proteinAccession) {

        String accessionNumberWithLink = proteinAccession;
        try {
            if (sequenceFactory.getHeader(proteinAccession) != null) {

                // try to find the database from the SequenceDatabase
                DatabaseType databaseType = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                // create the database link
                if (databaseType != null) {

                    // @TODO: support more databases

                    if (databaseType == DatabaseType.IPI || databaseType == DatabaseType.UniProt) {
                        accessionNumberWithLink = "<html><a href=\"" + getUniProtAccessionLink(proteinAccession)
                                + "\"><font color=\"" + getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else {
                        // unknown database!
                    }
                }
            }
        } catch (Exception e) {
            catchException(e);
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
                    DatabaseType database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                    // create the database link
                    if (database != null) {

                        // @TODO: support more databases

                        if (database == DatabaseType.IPI || database == DatabaseType.UniProt) {
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
        goPanel = new GOEAPanel(this);

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

        goJPanel.removeAll();
        goJPanel.add(goPanel);

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

        goPanel.revalidate();
        goPanel.repaint();
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
        if (exportSpectrumGraphicsJMenuItem.isVisible()) {
            exportSpectrumGraphicsJMenuItem.setEnabled(enable);
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
     * Update the peptide protein inference.
     * 
     * @param proteinInferenceType 
     */
    public void updatePeptideProteinInference(int proteinInferenceType) {

        ptmPanel.updatePeptideTable();
        ptmPanel.updateRelatedPeptidesTable();

        //@TODO update overview and structure panels

//        if (allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX ||
//                allTabsJTabbedPane.getSelectedIndex() == STRUCTURES_TAB_INDEX) {
//            overviewPanel.updatePeptideProteinInference(proteinInferenceType);
//            proteinStructurePanel.updatePeptideProteinInference(proteinInferenceType);
//        }
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
     * Returns the selected tab as indexed by the static fields.
     * 
     * @return the selected tab as indexed by the static fields
     */
    public int getSelectedTab() {
        return allTabsJTabbedPane.getSelectedIndex();
    }

    /**
     * Returns a list of keys of the currently displayed proteins.
     * 
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
                return ptmPanel.getDisplayedPsms();
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
                return ptmPanel.getDisplayedPsms();
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

        try {
            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

            if (spectrumCountingPreferences.getSelectedMethod() == SpectralCountingMethod.NSAF) {

                if (currentProtein == null) {
                    return 0.0;
                }

                result = 0;

                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    for (String spectrumMatchKey : peptideMatch.getSpectrumMatches()) {
                        pSParameter = (PSParameter) identification.getMatchParameter(spectrumMatchKey, pSParameter);
                        if (!spectrumCountingPreferences.isValidatedHits() || pSParameter.isValidated()) {
                            result++;
                        }
                    }
                }

                return result / currentProtein.getSequence().length();

            } else { // emPAI

                if (spectrumCountingPreferences.isValidatedHits()) {
                    result = 0;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        pSParameter = (PSParameter) identification.getMatchParameter(peptideKey, pSParameter);
                        if (pSParameter.isValidated()) {
                            result++;
                        }
                    }
                } else {
                    result = proteinMatch.getPeptideCount();
                }

                return Math.pow(10, result / currentProtein.getNPossiblePeptides(enyzme)) - 1;
            }
        } catch (Exception e) {
            catchException(e);
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Returns the exception type.
     * 
     * @param e the exception to get the type fro
     * @return  the exception type as a string
     */
    private String getExceptionType(Exception e) {
        if (e.getLocalizedMessage() == null) {
            return "null pointer";
        } else if (e.getLocalizedMessage().startsWith("Protein not found")) {
            return "Protein not found";
        } else if (e.getLocalizedMessage().startsWith("Error while loading")
                || e.getLocalizedMessage().startsWith("Error while writing")) {
            return "Serialization";
        } else {
            return e.getLocalizedMessage();
        }
    }

    /**
     * Method called whenever an exception is caught
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        if (!exceptionCaught.contains(getExceptionType(e))) {
            e.printStackTrace();
            exceptionCaught.add(getExceptionType(e));
            if (getExceptionType(e).equals("Protein not found")) {
                JOptionPane.showMessageDialog(this,
                        e.getLocalizedMessage() + "\nPlease refer to the troubleshooting section in http://peptide-shaker.googlecode.com.\nThis message will appear only once.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } else if (getExceptionType(e).equals("Serialization")) {
                JOptionPane.showMessageDialog(this,
                        e.getLocalizedMessage() + "\nPlease refer to the troubleshooting section in http://peptide-shaker.googlecode.com.\nThis message will appear only once.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "An error occured: "
                        + e.getLocalizedMessage()
                        + ".\nPlease contact the developpers.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Closes the frame by first checking if the project ought to be saved.
     */
    private void close() {

        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

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

                progressDialog.dispose();
                System.exit(0);
            }
        }.start();
    }

    /**
     * Update the annotation menu bar with the current annotation preferences.
     */
    public void updateAnnotationMenus() {

        aIonCheckBoxMenuItem.setSelected(false);
        bIonCheckBoxMenuItem.setSelected(false);
        cIonCheckBoxMenuItem.setSelected(false);
        xIonCheckBoxMenuItem.setSelected(false);
        yIonCheckBoxMenuItem.setSelected(false);
        zIonCheckBoxMenuItem.setSelected(false);
        precursorCheckBoxMenuItem.setSelected(false);
        immoniumCheckBoxMenuItem.setSelected(false);

        for (PeptideFragmentIonType ionType : annotationPreferences.getIonTypes()) {
            if (ionType == PeptideFragmentIonType.A_ION) {
                aIonCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.B_ION) {
                bIonCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.C_ION) {
                cIonCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.X_ION) {
                xIonCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.Y_ION) {
                yIonCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.Z_ION) {
                zIonCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.PRECURSOR_ION) {
                precursorCheckBoxMenuItem.setSelected(true);
            } else if (ionType == PeptideFragmentIonType.IMMONIUM) {
                immoniumCheckBoxMenuItem.setSelected(true);
            }
        }

        h2oIonCheckBoxMenuItem.setSelected(false);
        nh3IonCheckBoxMenuItem.setSelected(false);
        hpo3IonCheckBoxMenuItem.setSelected(false);
        h3po4IonCheckBoxMenuItem.setSelected(false);
        ch4osIonCheckBoxMenuItem.setSelected(false);

        for (NeutralLoss neutralLoss : annotationPreferences.getNeutralLosses().keySet()) {
            if (neutralLoss.isSameAs(NeutralLoss.H2O)) {
                h2oIonCheckBoxMenuItem.setSelected(true);
            } else if (neutralLoss.isSameAs(NeutralLoss.NH3)) {
                nh3IonCheckBoxMenuItem.setSelected(true);
            } else if (neutralLoss.isSameAs(NeutralLoss.CH4OS)) {
                ch4osIonCheckBoxMenuItem.setSelected(true);
            } else if (neutralLoss.isSameAs(NeutralLoss.H3PO4)) {
                h3po4IonCheckBoxMenuItem.setSelected(true);
            } else if (neutralLoss.isSameAs(NeutralLoss.HPO3)) {
                hpo3IonCheckBoxMenuItem.setSelected(true);
            }
        }

        singleChargeCheckBoxMenuItem.setSelected(false);
        doubleChargeCheckBoxMenuItem.setSelected(false);
        moreThanTwoChargesCheckBoxMenuItem.setSelected(false);

        for (int charge : annotationPreferences.getValidatedCharges()) {
            if (charge == 1) {
                singleChargeCheckBoxMenuItem.setSelected(true);
            } else if (charge == 2) {
                doubleChargeCheckBoxMenuItem.setSelected(true);
            } else if (charge > 2) {
                moreThanTwoChargesCheckBoxMenuItem.setSelected(true);
            }
        }

        automaticAnnotationCheckBoxMenuItem.setSelected(annotationPreferences.useAutomaticAnnotation());
        adaptCheckBoxMenuItem.setSelected(annotationPreferences.areNeutralLossesSequenceDependant());

        // disable/enable the neutral loss options
        h2oIonCheckBoxMenuItem.setEnabled(!annotationPreferences.areNeutralLossesSequenceDependant());
        nh3IonCheckBoxMenuItem.setEnabled(!annotationPreferences.areNeutralLossesSequenceDependant());
        hpo3IonCheckBoxMenuItem.setEnabled(!annotationPreferences.areNeutralLossesSequenceDependant());
        h3po4IonCheckBoxMenuItem.setEnabled(!annotationPreferences.areNeutralLossesSequenceDependant());
        ch4osIonCheckBoxMenuItem.setEnabled(!annotationPreferences.areNeutralLossesSequenceDependant());

        allCheckBoxMenuItem.setSelected(annotationPreferences.showAllPeaks());

        barsCheckBoxMenuItem.setSelected(annotationPreferences.showBars());
        intensityIonTableRadioButtonMenuItem.setSelected(annotationPreferences.useIntensityIonTable());
    }

    /**
     * Save the current annotation preferences selected in the annotation menus.
     */
    public void updateAnnotationPreferences() {

        annotationPreferences.clearIonTypes();
        if (aIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.A_ION);
        }
        if (bIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.B_ION);
        }
        if (cIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.C_ION);
        }
        if (xIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.X_ION);
        }
        if (yIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.Y_ION);
        }
        if (zIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.Z_ION);
        }
        if (precursorCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.PRECURSOR_ION);
        }
        if (immoniumCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(PeptideFragmentIonType.IMMONIUM);
        }

        annotationPreferences.clearNeutralLosses();
        if (h2oIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addNeutralLoss(NeutralLoss.H2O);
        }
        if (nh3IonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addNeutralLoss(NeutralLoss.NH3);
        }
        if (h3po4IonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addNeutralLoss(NeutralLoss.H3PO4);
        }
        if (hpo3IonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addNeutralLoss(NeutralLoss.HPO3);
        }
        if (ch4osIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addNeutralLoss(NeutralLoss.CH4OS);
        }

        annotationPreferences.useAutomaticAnnotation(automaticAnnotationCheckBoxMenuItem.isSelected());
        annotationPreferences.setNeutralLossesSequenceDependant(adaptCheckBoxMenuItem.isSelected());

        annotationPreferences.clearCharges();
        if (singleChargeCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addSelectedCharge(1);
        }
        if (doubleChargeCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addSelectedCharge(2);
        }
        if (moreThanTwoChargesCheckBoxMenuItem.isSelected()) {
            int precursorCharge = annotationPreferences.getCurrentPrecursorCharge();
            if (precursorCharge > 2) {
                for (int charge = 3; charge < precursorCharge; charge++) {
                    annotationPreferences.addSelectedCharge(charge);
                }
            }
        }

        annotationPreferences.setShowAllPeaks(allCheckBoxMenuItem.isSelected());
        annotationPreferences.setShowBars(barsCheckBoxMenuItem.isSelected());
        annotationPreferences.setIntensityIonTable(intensityIonTableRadioButtonMenuItem.isSelected());

        updateSpectrumAnnotations();
        setDataSaved(false);
    }

    /**
     * Returns the annotation menu bar.
     * 
     * @return the annotation menu bar
     */
    public JMenuBar getAnnotationMenuBar() {
        return annotationMenuBar;
    }

    /**
     * Updates the visible menu items on the settings menu of the annotation menu bar.
     * 
     * @param showSpectrumOptions       if true, the spectrum options are shown
     * @param showBubblePlotOptions     if true, the bubble plot options are shown
     * @param showIonTableOptions       if true, the ion table options are shown
     */
    public void updateAnnotationMenuBarVisableOptions(boolean showSpectrumOptions, boolean showBubblePlotOptions, boolean showIonTableOptions) {
        allCheckBoxMenuItem.setVisible(showSpectrumOptions);
        exportSpectrumGraphicsJMenuItem.setVisible(showSpectrumOptions);
        exportSpectrumMenu.setVisible(showSpectrumOptions);

        // @TODO: remove this when the other tabs also use the extended spectrum panel!
        exportSequenceFragmentationGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportIntensityHistogramGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportMassErrorPlotGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);

        barsCheckBoxMenuItem.setVisible(showBubblePlotOptions);
        bubblePlotJMenuItem.setVisible(showBubblePlotOptions);
        bubbleScaleJMenuItem.setVisible(showBubblePlotOptions);

        intensityIonTableRadioButtonMenuItem.setVisible(showIonTableOptions);
        mzIonTableRadioButtonMenuItem.setVisible(showIonTableOptions);

        if (settingsMenu.isEnabled()) {
            exportGraphicsMenu.setEnabled(!showIonTableOptions);
        }
    }

    /**
     * Add the list of recently used files to the file menu.
     */
    private void loadRecentProjectsList() {

        String path = getJarFilePath() + "/conf/recently_opened_projects.txt";

        File file = new File(path);

        boolean fileExists = file.exists();

        if (!file.exists()) {
            try {
                fileExists = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (fileExists) {

            openRecentJMenu.removeAll();

            try {
                FileReader r = new FileReader(file);
                BufferedReader br = new BufferedReader(r);

                String line = br.readLine();
                int counter = 1;

                while (line != null) {
                    JMenuItem menuItem = new JMenuItem(counter++ + ": " + line);

                    final String filePath = line;
                    final PeptideShakerGUI temp = this;

                    menuItem.addActionListener(new java.awt.event.ActionListener() {

                        public void actionPerformed(java.awt.event.ActionEvent evt) {

                            if (!new File(filePath).exists()) {
                                JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
                            } else {
                                NewDialog openDialog = new NewDialog(temp, false);
                                openDialog.setSearchParamatersFiles(new ArrayList<File>());

                                // get the properties files
                                for (File file : new File(filePath).getParentFile().listFiles()) {
                                    if (file.getName().toLowerCase().endsWith(".properties")) {
                                        if (!openDialog.getSearchParametersFiles().contains(file)) {
                                            openDialog.getSearchParametersFiles().add(file);
                                        }
                                    }
                                }

                                importPeptideShakerFile(new File(filePath));
                                updateRecentProjectsList(new File(filePath));
                                lastSelectedFolder = new File(filePath).getAbsolutePath();
                            }
                        }
                    });

                    openRecentJMenu.add(menuItem);
                    line = br.readLine();
                }

                br.close();
                r.close();

                if (openRecentJMenu.getItemCount() == 0) {
                    JMenuItem menuItem = new JMenuItem("(empty)");
                    menuItem.setEnabled(false);
                    openRecentJMenu.add(menuItem);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add the given file to the top of the recent projects list.
     * 
     * @param file the file to add
     */
    private void updateRecentProjectsList(File file) {

        String path = getJarFilePath() + "/conf/recently_opened_projects.txt";

        File recentFiles = new File(path);

        boolean fileExists = recentFiles.exists();

        if (!recentFiles.exists()) {
            try {
                fileExists = recentFiles.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (fileExists) {

            try {
                // read the old list
                FileReader r = new FileReader(recentFiles);
                BufferedReader br = new BufferedReader(r);

                String line = br.readLine();
                String oldList = "";
                int counter = 0;

                while (line != null && counter < 9) {

                    if (!line.equalsIgnoreCase(file.getAbsolutePath())) {
                        oldList += line + "\n";
                    }

                    line = br.readLine();
                    counter++;
                }

                br.close();
                r.close();

                // write the new list
                FileWriter w = new FileWriter(recentFiles);
                BufferedWriter bw = new BufferedWriter(w);
                bw.write(file.getAbsolutePath() + "\n" + oldList);

                bw.close();
                w.close();

                // load the updated list
                loadRecentProjectsList();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a String with the HTML tooltip for the peptide indicating the 
     * modification details.
     * 
     * @param peptide
     * @return a String with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        String tooltip = "<html>";

        ArrayList<ModificationMatch> modifications = peptide.getModificationMatches();

        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        PTM ptm;

        for (int i = 0; i < modifications.size(); i++) {

            ptm = ptmFactory.getPTM(modifications.get(i).getTheoreticPtm());

            if (ptm.getType() == PTM.MODAA && modifications.get(i).isVariable()) { // @TODO: should only PTM.MODAA be included??

                int modSite = modifications.get(i).getModificationSite();
                String modName = modifications.get(i).getTheoreticPtm();
                char affectedResidue = peptide.getSequence().charAt(modSite - 1);
                Color ptmColor = searchParameters.getModificationProfile().getColor(modifications.get(i).getTheoreticPtm());

                if (!alreadyAnnotated.contains(modName + "_" + affectedResidue)) {
                    tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                            + affectedResidue
                            + "</span>"
                            + ": " + modName + "<br>";

                    alreadyAnnotated.add(modName + "_" + affectedResidue);
                }
            }
        }

        if (!tooltip.equalsIgnoreCase("<html>")) {
            tooltip += "</html>";
        } else {
            tooltip = null;
        }

        return tooltip;
    }

    /**
     * Returns the project details
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        return projectDetails;
    }

    /**
     * Sets the project details
     * @param projectDetails the project details
     */
    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
    }

    /**
     * Imports informations from a peptide shaker file.
     *
     * @param aPsFile    the peptide shaker file
     */
    public void importPeptideShakerFile(File aPsFile) {

        currentPSFile = aPsFile;

        final PeptideShakerGUI peptideShakerGUI = this; // needed due to threading issues
        progressDialog = new ProgressDialogX(this, this, true);
        progressDialog.doNothingOnClose();

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Importing Project. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("ImportThread") {

            @Override
            public void run() {

                try {
                    // reset enzymes, ptms and preferences
                    loadEnzymes();
                    loadModifications();
                    setDefaultPreferences();

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                    MsExperiment tempExperiment = experimentIO.loadExperiment(currentPSFile);
                    Sample tempSample = null;
                    int tempReplicate = -1;

                    PSSettings experimentSettings = new PSSettings();
                    experimentSettings = (PSSettings) tempExperiment.getUrParam(experimentSettings);
                    peptideShakerGUI.setAnnotationPreferences(experimentSettings.getAnnotationPreferences());
                    peptideShakerGUI.setSpectrumCountingPreferences(experimentSettings.getSpectrumCountingPreferences());
                    peptideShakerGUI.setProjectDetails(experimentSettings.getProjectDetails());
                    peptideShakerGUI.setSearchParameters(experimentSettings.getSearchParameters());
                    PeptideShaker.setPeptideShakerPTMs(searchParameters);

                    try {
                        File providedFastaLocation = experimentSettings.getSearchParameters().getFastaFile();
                        String fileName = providedFastaLocation.getName();
                        File projectFolder = currentPSFile.getParentFile();
                        File dataFolder = new File(projectFolder, "data");

                        // try to locate the FASTA file
                        if (providedFastaLocation.exists()) {
                            SequenceFactory.getInstance().loadFastaFile(providedFastaLocation);
                        } else if (new File(projectFolder, fileName).exists()) {
                            SequenceFactory.getInstance().loadFastaFile(new File(projectFolder, fileName));
                        } else if (new File(dataFolder, fileName).exists()) {
                            SequenceFactory.getInstance().loadFastaFile(new File(dataFolder, fileName));
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occured while reading:\n" + experimentSettings.getSearchParameters().getFastaFile() + ".\nPlease select the FASTA file manually.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);

                        JFileChooser fileChooser = new JFileChooser(getLastSelectedFolder());
                        fileChooser.setDialogTitle("Open FASTA File");

                        FileFilter filter = new FileFilter() {

                            @Override
                            public boolean accept(File myFile) {
                                return myFile.getName().toLowerCase().endsWith("fasta")
                                        || myFile.getName().toLowerCase().endsWith("fast")
                                        || myFile.getName().toLowerCase().endsWith("fas")
                                        || myFile.isDirectory();
                            }

                            @Override
                            public String getDescription() {
                                return "Supported formats: FASTA format (.fasta)";
                            }
                        };

                        fileChooser.setFileFilter(filter);
                        int returnVal = fileChooser.showDialog(peptideShakerGUI, "Open");

                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File fastaFile = fileChooser.getSelectedFile();
                            setLastSelectedFolder(fastaFile.getAbsolutePath());
                            searchParameters.setFastaFile(fastaFile);
                            try {
                                progressDialog.setTitle("Importing FASTA File. Please Wait...");
                                SequenceFactory.getInstance().loadFastaFile(experimentSettings.getSearchParameters().getFastaFile(), progressDialog.getProgressBar());
                            } catch (Exception e2) {
                                e2.printStackTrace();
                                JOptionPane.showMessageDialog(peptideShakerGUI,
                                        "An error occured while reading " + experimentSettings.getSearchParameters().getFastaFile() + ".\nOpen cancelled.",
                                        "File Input Error", JOptionPane.ERROR_MESSAGE);
                                clearData();

                                progressDialog.setVisible(false);
                                progressDialog.dispose();

                                // change the peptide shaker icon back to the default version
                                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                return;
                            }

                        } else {
                            clearData();

                            progressDialog.setVisible(false);
                            progressDialog.dispose();

                            // change the peptide shaker icon back to the default version
                            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            return;
                        }
                    }

                    ArrayList<String> names = new ArrayList<String>();
                    ArrayList<String> spectrumFiles = new ArrayList<String>();

                    for (String filePath : getSearchParameters().getSpectrumFiles()) {
                        try {
                            File providedSpectrumLocation = new File(filePath);
                            String fileName = providedSpectrumLocation.getName();
                            File projectFolder = currentPSFile.getParentFile();
                            File dataFolder = new File(projectFolder, "data");

                            // try to locate the spectrum file
                            if (providedSpectrumLocation.exists() && !names.contains(providedSpectrumLocation.getName())) {
                                names.add(providedSpectrumLocation.getName());
                                spectrumFiles.add(providedSpectrumLocation.getAbsolutePath());
                            } else if (new File(projectFolder, fileName).exists() && !names.contains(new File(projectFolder, fileName).getName())) {
                                names.add(new File(projectFolder, fileName).getName());
                                spectrumFiles.add(new File(projectFolder, fileName).getAbsolutePath());
                            } else if (new File(dataFolder, fileName).exists() && !names.contains(new File(dataFolder, fileName).getName())) {
                                names.add(new File(dataFolder, fileName).getName());
                                spectrumFiles.add(new File(dataFolder, fileName).getAbsolutePath());
                            } else {
                                JOptionPane.showMessageDialog(peptideShakerGUI,
                                        "An error occured while reading:\n" + new File(filePath).getName() + ".\n"
                                        + "Please select the spectrum file or the folder containing it manually.",
                                        "File Input Error", JOptionPane.ERROR_MESSAGE);

                                JFileChooser fileChooser = new JFileChooser(getLastSelectedFolder());
                                fileChooser.setDialogTitle("Open Spectrum File");

                                FileFilter filter = new FileFilter() {

                                    @Override
                                    public boolean accept(File myFile) {
                                        return myFile.getName().toLowerCase().endsWith("mgf")
                                                || myFile.isDirectory();
                                    }

                                    @Override
                                    public String getDescription() {
                                        return "Supported formats: Mascot Generic Format (.mgf)";
                                    }
                                };

                                fileChooser.setFileFilter(filter);
                                int returnVal = fileChooser.showDialog(peptideShakerGUI, "Open");

                                if (returnVal == JFileChooser.APPROVE_OPTION) {
                                    File mgfFolder = fileChooser.getSelectedFile();
                                    if (!mgfFolder.isDirectory()) {
                                        mgfFolder = mgfFolder.getParentFile();
                                    }
                                    setLastSelectedFolder(mgfFolder.getAbsolutePath());
                                    boolean found = false;
                                    for (File file : mgfFolder.listFiles()) {
                                        for (String filePath2 : getSearchParameters().getSpectrumFiles()) {
                                            try {
                                                File newFile2 = new File(filePath2);
                                                if (newFile2.getName().equals(file.getName())
                                                        && !names.contains(file.getName())) {
                                                    names.add(file.getName());
                                                    spectrumFiles.add(file.getPath());
                                                }
                                                if (new File(filePath).getName().equals(newFile2.getName())) {
                                                    found = true;
                                                }
                                            } catch (Exception e) {
                                                // ignore
                                            }
                                        }
                                    }
                                    if (!found) {
                                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                                new File(filePath).getName() + " was not found in the given folder.",
                                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                                        clearData();

                                        progressDialog.setVisible(false);
                                        progressDialog.dispose();

                                        // change the peptide shaker icon back to the default version
                                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                        return;
                                    }
                                } else {
                                    clearData();

                                    progressDialog.setVisible(false);
                                    progressDialog.dispose();

                                    // change the peptide shaker icon back to the default version
                                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(peptideShakerGUI,
                                    "An error occured while looking for the spectrum files.",
                                    "File Input Error", JOptionPane.ERROR_MESSAGE);
                            clearData();

                            progressDialog.setVisible(false);
                            progressDialog.dispose();

                            // change the peptide shaker icon back to the default version
                            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            e.printStackTrace();
                            return;
                        }
                    }
                    getSearchParameters().setSpectrumFiles(spectrumFiles);

                    ArrayList<Sample> samples = new ArrayList(tempExperiment.getSamples().values());
                    if (samples.size() == 1) {
                        tempSample = samples.get(0);
                    } else {
                        String[] sampleNames = new String[samples.size()];
                        for (int cpt = 0; cpt < sampleNames.length; cpt++) {
                            sampleNames[cpt] = samples.get(cpt).getReference();
                        }
                        SampleSelection sampleSelection = new SampleSelection(null, true, sampleNames, "sample");
                        sampleSelection.setVisible(true);
                        String choice = sampleSelection.getChoice();
                        for (Sample sampleTemp : samples) {
                            if (sampleTemp.getReference().equals(choice)) {
                                tempSample = sampleTemp;
                                break;
                            }
                        }
                    }

                    ArrayList<Integer> replicates = new ArrayList(tempExperiment.getAnalysisSet(tempSample).getReplicateNumberList());
                    if (replicates.size() == 1) {
                        tempReplicate = replicates.get(0);
                    } else {
                        String[] replicateNames = new String[replicates.size()];
                        for (int cpt = 0; cpt < replicateNames.length; cpt++) {
                            replicateNames[cpt] = samples.get(cpt).getReference();
                        }
                        SampleSelection sampleSelection = new SampleSelection(null, true, replicateNames, "replicate");
                        sampleSelection.setVisible(true);
                        Integer choice = new Integer(sampleSelection.getChoice());
                        tempReplicate = choice;
                    }

                    setProject(tempExperiment, tempSample, tempReplicate);

                    File mgfFile;
                    int cpt = 0;
                    progressDialog.setTitle("Importing Spectrum Files. Please Wait...");
                    for (String spectrumFile : spectrumFiles) {
                        progressDialog.setIndeterminate(false);
                        progressDialog.setMax(spectrumFiles.size() + 1);
                        progressDialog.setValue(++cpt);
                        try {
                            mgfFile = new File(spectrumFile);
                            spectrumFactory.addSpectra(mgfFile, progressDialog.getProgressBar());
                            progressDialog.incrementValue();
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(peptideShakerGUI,
                                    "An error occured while importing " + spectrumFile + ".",
                                    "File Input Error", JOptionPane.ERROR_MESSAGE);
                            clearData();

                            progressDialog.setVisible(false);
                            progressDialog.dispose();

                            // change the peptide shaker icon back to the default version
                            peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                            e.printStackTrace();
                            return;
                        }
                    }

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    peptideShakerGUI.displayResults(true);
                    peptideShakerGUI.setFrameTitle(experiment.getReference());


                    // change the peptide shaker icon back to the default version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                } catch (Exception e) {

                    // change the peptide shaker icon back to the default version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            "An error occured while reading " + currentPSFile + ".\n"
                            + "Please verify that the compomics-utilities version used to create\n"
                            + "the file is compatible with your version of PeptideShaker.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // do nothing
    }

    /**
     * Export the current spectrum as an mgf.
     */
    public void exportSpectrumAsMgf() {
        exportSpectrumValuesJMenuItemActionPerformed(null);
    }

    /**
     * Export the current spectrum as a figure.
     */
    public void exportSpectrumAsFigure() {
        exportSpectrumGraphicsJMenuItemActionPerformed(null);
    }

    /**
     * Export the current sequence fragmentation as a figure.
     */
    public void exportSequenceFragmentationAsFigure() {
        exportSequenceFragmentationGraphicsJMenuItemActionPerformed(null);
    }

    /**
     * Export the current intensity histogram as a figure.
     */
    public void exportIntensityHistogramAsFigure() {
        exportIntensityHistogramGraphicsJMenuItemActionPerformed(null);
    }

    /**
     * Export the current mass error plot as a figure.
     */
    public void exportMassErrorPlotAsFigure() {
        exportMassErrorPlotGraphicsJMenuItemActionPerformed(null);
    }

    /**
     * Export the current bubble plot as a figure.
     */
    public void exportBubblePlotAsFigure() {
        bubblePlotJMenuItemActionPerformed(null);
    }

    /**
     * Update the display options for the overview tab.
     * 
     * @param displayProteins
     * @param displayPeptidesAndPsms
     * @param displayCoverage
     * @param displaySpectrum 
     */
    public void setDisplayOptions(boolean displayProteins, boolean displayPeptidesAndPsms,
            boolean displayCoverage, boolean displaySpectrum) {
        proteinsJCheckBoxMenuItem.setSelected(displayProteins);
        peptidesAndPsmsJCheckBoxMenuItem.setSelected(displayPeptidesAndPsms);
        sequenceCoverageJCheckBoxMenuItem.setSelected(displayCoverage);
        spectrumJCheckBoxMenuItem.setSelected(displaySpectrum);

        overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
    }

    /**
     * Returns the identification filter used
     * @return the identification filter used
     */
    public IdFilter getIdFilter() {
        return idFilter;
    }

    /**
     * Setsthe identification filter used
     * @param idFilter the identification filter used
     */
    public void setIdFilter(IdFilter idFilter) {
        this.idFilter = idFilter;
    }
}