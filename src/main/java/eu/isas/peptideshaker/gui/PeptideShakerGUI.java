
package eu.isas.peptideshaker.gui;

import com.compomics.util.gui.error_handlers.notification.NotificationDialogParent;
import com.compomics.util.gui.gene_mapping.SpeciesDialog;
import eu.isas.peptideshaker.gui.exportdialogs.FeaturesPreferencesDialog;
import eu.isas.peptideshaker.gui.exportdialogs.FollowupPreferencesDialog;
import com.compomics.util.preferences.gui.ImportSettingsDialog;
import com.compomics.util.preferences.gui.ProcessingPreferencesDialog;
import com.compomics.util.gui.export_graphics.ExportGraphicsDialog;
import com.compomics.software.CompomicsWrapper;
import com.compomics.software.ToolFactory;
import com.compomics.software.dialogs.JavaOptionsDialog;
import com.compomics.software.dialogs.JavaOptionsDialogParent;
import com.compomics.software.dialogs.SearchGuiSetupDialog;
import com.compomics.software.dialogs.ReporterSetupDialog;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.notification.NotesDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.compomics.util.gui.export_graphics.ExportGraphicsDialogParent;
import com.compomics.util.gui.searchsettings.SearchSettingsDialog;
import com.compomics.util.gui.searchsettings.SearchSettingsDialogParent;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.preferences.IdFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.gui.preferencesdialogs.*;
import eu.isas.peptideshaker.gui.tabpanels.AnnotationPanel;
import eu.isas.peptideshaker.gui.tabpanels.GOEAPanel;
import eu.isas.peptideshaker.gui.tabpanels.OverviewPanel;
import eu.isas.peptideshaker.gui.tabpanels.ProteinStructurePanel;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.gui.tabpanels.QCPanel;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.gui.tabpanels.StatsPanel;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import com.compomics.util.preferences.ModificationProfile;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.UserPreferences;
import eu.isas.peptideshaker.PeptideShakerWrapper;
import eu.isas.peptideshaker.gui.gettingStarted.GettingStartedDialog;
import eu.isas.peptideshaker.gui.tabpanels.*;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.gui.pride.PrideExportDialog;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import com.compomics.util.preferences.GenePreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.peptideshaker.utils.StarHider;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import net.jimmc.jshortcut.JShellLink;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main PeptideShaker frame.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class PeptideShakerGUI extends JFrame implements ClipboardOwner, ExportGraphicsDialogParent, JavaOptionsDialogParent, SearchSettingsDialogParent, NotificationDialogParent {

    /**
     * The path to the example dataset.
     */
    private final String EXAMPLE_DATASET_PATH = "/resources/example_dataset/HeLa Example.cps";
    /**
     * Convenience static string indicating that no selection was done by the
     * user.
     */
    public final static String NO_SELECTION = "NO SELECTION";
    /**
     * The currently selected protein key.
     */
    private String selectedProteinKey = NO_SELECTION;
    /**
     * The currently selected peptide key.
     */
    private String selectedPeptideKey = NO_SELECTION;
    /**
     * The currently selected spectrum key.
     */
    private String selectedPsmKey = NO_SELECTION;
    /**
     * The Overview tab index.
     */
    public static final int OVER_VIEW_TAB_INDEX = 0;
    /**
     * The SpectrumID tab index.
     */
    public static final int SPECTRUM_ID_TAB_INDEX = 1;
    /**
     * The Protein Fractions tab index.
     */
    public static final int PROTEIN_FRACTIONS_TAB_INDEX = 2;
    /**
     * The Modifications tab index.
     */
    public static final int MODIFICATIONS_TAB_INDEX = 3;
    /**
     * The Structures tab index.
     */
    public static final int STRUCTURES_TAB_INDEX = 4;
    /**
     * The Annotation tab index.
     */
    public static final int ANNOTATION_TAB_INDEX = 5;
    /**
     * The GO Analysis tab index.
     */
    public static final int GO_ANALYSIS_TAB_INDEX = 6;
    /**
     * The Validation tab index.
     */
    public static final int VALIDATION_TAB_INDEX = 7;
    /**
     * The QC Plots tab index.
     */
    public static final int QC_PLOTS_TAB_INDEX = 8;
    /**
     * Array containing the tab which must be updated as indexed by the static
     * index. If true the whole panel will be reloaded, if false only the
     * selection will be updated.
     */
    private HashMap<Integer, Boolean> updateNeeded;
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
     * If true the relative error (ppm) is used instead of the absolute error
     * (Da).
     */
    private boolean useRelativeError = false;
    /**
     * If true, the latest changes have been saved.
     */
    private boolean dataSaved = true;
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
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The compomics enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The GO factory.
     */
    private GOFactory goFactory = GOFactory.getInstance();
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The overview panel.
     */
    private OverviewPanel overviewPanel;
    /**
     * The protein fractions panel.
     */
    private ProteinFractionsPanel proteinFractionsPanel;
    /**
     * The statistics panel.
     */
    private StatsPanel statsPanel;
    /**
     * The PTM panel.
     */
    private PtmPanel ptmPanel;
    /**
     * The Annotation panel.
     */
    private AnnotationPanel annotationPanel;
    /**
     * The spectrum panel.
     */
    private SpectrumIdentificationPanel spectrumIdentificationPanel;
    /**
     * The protein structure panel.
     */
    private ProteinStructurePanel proteinStructurePanel;
    /**
     * The QC panel.
     */
    private QCPanel qcPanel;
    /**
     * The GO Analysis panel.
     */
    private GOEAPanel goPanel;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance(30000);
    /**
     * The exception handler
     */
    private ExceptionHandler exceptionHandler = new ExceptionHandler(this, "http://code.google.com/p/peptide-shaker/issues/list");
    /**
     * The spectrum annotator.
     */
    private PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
    /**
     * The actually identified modifications.
     */
    private ArrayList<String> identifiedModifications = null;
    /**
     * The Jump To panel.
     */
    private JumpToPanel jumpToPanel;
    /**
     * The class used to star/hide items.
     */
    private StarHider starHider = new StarHider(this);
    /**
     * The class used to provide graphical sexy features out of the
     * identification.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The charge menus.
     */
    private HashMap<Integer, JCheckBoxMenuItem> chargeMenus = new HashMap<Integer, JCheckBoxMenuItem>();
    /**
     * The neutral loss menus.
     */
    private HashMap<NeutralLoss, JCheckBoxMenuItem> lossMenus = new HashMap<NeutralLoss, JCheckBoxMenuItem>();
    /**
     * The horizontal padding used before and after the text in the titled
     * borders. (Needed to make it look as good in Java 7 as it did in Java
     * 6...)
     */
    public static String TITLED_BORDER_HORIZONTAL_PADDING = "";
    /**
     * The list of the default modifications.
     */
    private ArrayList<String> modificationUse = new ArrayList<String>();
    /**
     * Default PeptideShaker modifications.
     */
    public static final String PEPTIDESHAKER_CONFIGURATION_FILE = "PeptideShaker_configuration.txt";
    /**
     * The list of already published tweets.
     */
    private ArrayList<String> publishedTweets = new ArrayList<String>();
    /**
     * The list of new tweets.
     */
    private ArrayList<String> newTweets = new ArrayList<String>();
    /**
     * The list of current notes to the user.
     */
    private ArrayList<String> currentNotes = new ArrayList<String>();
    /**
     * The cps parent used to manage the data.
     */
    private CpsParent cpsBean = new CpsParent() {
        @Override
        public String getJarFilePath() {
            return PeptideShakerGUI.this.getJarFilePath();
        }
    };

    /**
     * The main method used to start PeptideShaker.
     *
     * @param args
     */
    public static void main(String[] args) {

        // set the look and feel
        boolean numbusLookAndFeelSet = false;
        try {
            numbusLookAndFeelSet = UtilitiesGUIDefaults.setLookAndFeel();
        } catch (Exception e) {
        }

        if (!numbusLookAndFeelSet) {
            JOptionPane.showMessageDialog(null,
                    "Failed to set the default look and feel. Using backup look and feel.\n"
                    + "PeptideShaker will work but not look as good as it should...", "Look and Feel",
                    JOptionPane.WARNING_MESSAGE);
        }

        // need to add some padding to the text in the titled borders on Java 1.7 
        if (!System.getProperty("java.version").startsWith("1.6")) {
            TITLED_BORDER_HORIZONTAL_PADDING = "   ";
        }

        // turn off the derby log file
        DerbyUtil.disableDerbyLog();

        File cpsFile = null;
        boolean cps = false;
        for (String arg : args) {
            if (cps) {
                cpsFile = new File(arg);
                cps = false;
            }
            if (arg.equals(ToolFactory.peptideShakerFileOption)) {
                cps = true;
            }
        }

        new PeptideShakerGUI(cpsFile, true);
    }

    /**
     * Creates a new PeptideShaker frame.
     */
    public PeptideShakerGUI() {
    }

    /**
     * Creates a new PeptideShaker frame.
     *
     * @param cpsFile the cps file to load
     * @param showWelcomeDialog boolean indicating if the Welcome Dialog is to
     * be shown
     */
    public PeptideShakerGUI(File cpsFile, boolean showWelcomeDialog) {

        // check for new version
        CompomicsWrapper.checkForNewVersion(getVersion(), "PeptideShaker", "peptide-shaker");

        // set up the ErrorLog
        setUpLogFile(true);

        // load the user preferences
        loadUserPreferences();

        // set this version as the default PeptideShaker version
        if (!getJarFilePath().equalsIgnoreCase(".")) {
            utilitiesUserPreferences.setPeptideShakerPath(new File(getJarFilePath(), "PeptideShaker-" + getVersion() + ".jar").getAbsolutePath());
            UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
        }

        // check for 64 bit java and for at least 4 gb memory 
        boolean java64bit = CompomicsWrapper.is64BitJava();
        boolean memoryOk = (utilitiesUserPreferences.getMemoryPreference() >= 4000);

        // add desktop shortcut?
        if (!getJarFilePath().equalsIgnoreCase(".")
                && System.getProperty("os.name").lastIndexOf("Windows") != -1
                && new File(getJarFilePath() + "/resources/conf/firstRun").exists()) {

            // @TODO: add support for desktop icons in mac and linux??
            // delete the firstRun file such that the user is not asked the next time around
            boolean fileDeleted = new File(getJarFilePath() + "/resources/conf/firstRun").delete();

            if (!fileDeleted) {
                JOptionPane.showMessageDialog(this, "Failed to delete the file /resources/conf/firstRun.\n"
                        + "Please delete it manually.", "File Error", JOptionPane.OK_OPTION);
            }

            int value = JOptionPane.showConfirmDialog(this,
                    "Create a shortcut to PeptideShaker on the desktop?",
                    "Create Desktop Shortcut?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                addShortcutAtDeskTop();
            }
        }

        // set the font color for the titlted borders, looks better than the default black
        UIManager.put("TitledBorder.titleColor", new Color(59, 59, 59));

        initComponents();

        reshakeMenuItem.setVisible(false); // @TODO: re-enable later?
        quantifyMenuItem.setVisible(false); // @TODO: re-enable later?
        jSeparator2.setVisible(false); // @TODO: re-enable later?
        reporterPreferencesJMenuItem.setVisible(false); // @TODO: re-enable later?

        notesButton.setVisible(false); // @TODO: re-enable later?
        newsButton.setVisible(false); // @TODO: re-enable later?
        tipsButton.setVisible(false); // @TODO: re-enable later?

        // add icons to the tab componets
        //setupTabComponents(); // @TODO: implement me? requires the creation of icons for each tab...
        overviewPanel = new OverviewPanel(this);
        overviewJPanel.add(overviewPanel);
        spectrumIdentificationPanel = new SpectrumIdentificationPanel(this);
        proteinFractionsPanel = new ProteinFractionsPanel(this);
        proteinFractionsJPanel.add(proteinFractionsPanel);
        ptmPanel = new PtmPanel(this);
        proteinStructurePanel = new ProteinStructurePanel(this);
        proteinStructureJPanel.add(proteinStructurePanel);
        annotationPanel = new AnnotationPanel(this);
        statsPanel = new StatsPanel(this);

        jumpToPanel = new JumpToPanel(this);
        jumpToPanel.setEnabled(false);

        menuBar.add(Box.createHorizontalGlue());

        menuBar.add(jumpToPanel);

        setUpPanels(true);
        repaintPanels();

        // load the list of recently used projects
        updateRecentProjectsList();

        // set the title
        updateFrameTitle();

        // set the title of the frame and add the icon
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

        this.setExtendedState(MAXIMIZED_BOTH);

        loadGeneMappings();
        loadEnzymes();
        resetPtmFactory();

        File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator);
        File modUseFile = new File(folder, PEPTIDESHAKER_CONFIGURATION_FILE);
        modificationUse = SearchSettingsDialog.loadModificationsUse(modUseFile);

        //setDefaultPreferences(); // @TODO: i tried re-adding this but then we get a null pointer, but the two below have to be added or the default neutral losses won't appear
        IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.H2O);
        IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.NH3);

        setLocationRelativeTo(null);

        if (cpsFile == null) {
            if (showWelcomeDialog) {
                // open the welcome dialog
                new WelcomeDialog(this, !java64bit || !memoryOk, true);
            }
        } else {
            setVisible(true);
            importPeptideShakerFile(cpsFile);
        }
    }

    /**
     * Loads the user preferences.
     */
    public void loadUserPreferences() {

        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            e.printStackTrace();
        }
        cpsBean.loadUserPreferences();
    }

    /**
     * Sets the project.
     *
     * @param experiment the experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
     */
    public void setProject(MsExperiment experiment, Sample sample, int replicateNumber) {
        cpsBean.setProject(experiment, sample, replicateNumber);
        updateFrameTitle();
    }

    /**
     * Add icons to the tab components.
     */
    private void setupTabComponents() {
        // @TODO: implement me? requires the creation of icons for each tab...
//        int iconTextGap = 5;
//        int horizontalTextAlignment = SwingConstants.RIGHT;
//        
//        JLabel tempLabel = new JLabel("Overview");
//        Icon icon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/overview_tab_small.png")));
//        tempLabel.setIcon(icon);
//
//        // Add some spacing between text and icon, and position text to the RHS. 
//        tempLabel.setIconTextGap(iconTextGap);
//        tempLabel.setHorizontalTextPosition(horizontalTextAlignment);
//
//        // assign tab component for first tab. 
//        allTabsJTabbedPane.setTabComponentAt(0, tempLabel);
//        
//        ...
    }

    /**
     * Add the experiment title to the frame title.
     */
    public void updateFrameTitle() {
        if (getExperiment() != null) {
            this.setTitle("PeptideShaker " + getVersion() + " - " + getExperiment().getReference() + " (Sample: " + getSample().getReference() + ", Replicate: " + getReplicateNumber() + ")");
        } else {
            this.setTitle("PeptideShaker " + getVersion());
        }
    }

    /**
     * Reset the frame title.
     */
    public void resetFrameTitle() {
        this.setTitle("PeptideShaker " + getVersion());
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
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
        splitterMenu8 = new javax.swing.JMenu();
        otherMenu = new javax.swing.JMenu();
        precursorCheckMenu = new javax.swing.JCheckBoxMenuItem();
        immoniumIonsCheckMenu = new javax.swing.JCheckBoxMenuItem();
        reporterIonsCheckMenu = new javax.swing.JCheckBoxMenuItem();
        lossSplitter = new javax.swing.JMenu();
        lossMenu = new javax.swing.JMenu();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        adaptCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu2 = new javax.swing.JMenu();
        chargeMenu = new javax.swing.JMenu();
        splitterMenu3 = new javax.swing.JMenu();
        deNovoMenu = new javax.swing.JMenu();
        forwardIonsDeNovoCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        rewindIonsDeNovoCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        deNovoChargeOneJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        deNovoChargeTwoJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        splitterMenu9 = new javax.swing.JMenu();
        settingsMenu = new javax.swing.JMenu();
        allCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        barsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        bubbleScaleJMenuItem = new javax.swing.JMenuItem();
        intensityIonTableRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        mzIonTableRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        automaticAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        errorPlotTypeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        annotationColorsJMenuItem = new javax.swing.JMenuItem();
        splitterMenu4 = new javax.swing.JMenu();
        exportGraphicsMenu = new javax.swing.JMenu();
        exportSpectrumMenu = new javax.swing.JMenu();
        exportSpectrumGraphicsJMenuItem = new javax.swing.JMenuItem();
        exportSpectrumAndPlotsGraphicsJMenuItem = new javax.swing.JMenuItem();
        exportSpectrumGraphicsSeparator = new javax.swing.JPopupMenu.Separator();
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
        deNovoChargeButtonGroup = new javax.swing.ButtonGroup();
        backgroundPanel = new javax.swing.JPanel();
        backgroundLayeredPane = new javax.swing.JLayeredPane();
        allTabsJTabbedPane = new javax.swing.JTabbedPane();
        overviewJPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        proteinFractionsJPanel = new javax.swing.JPanel();
        ptmJPanel = new javax.swing.JPanel();
        proteinStructureJPanel = new javax.swing.JPanel();
        annotationsJPanel = new javax.swing.JPanel();
        goJPanel = new javax.swing.JPanel();
        statsJPanel = new javax.swing.JPanel();
        qcJPanel = new javax.swing.JPanel();
        newsButton = new javax.swing.JButton();
        notesButton = new javax.swing.JButton();
        tipsButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileJMenu = new javax.swing.JMenu();
        newJMenuItem = new javax.swing.JMenuItem();
        jSeparator18 = new javax.swing.JPopupMenu.Separator();
        openJMenuItem = new javax.swing.JMenuItem();
        openRecentJMenu = new javax.swing.JMenu();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        reshakeMenuItem = new javax.swing.JMenuItem();
        quantifyMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        projectPropertiesMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        searchParametersMenu = new javax.swing.JMenuItem();
        importFilterMenu = new javax.swing.JMenuItem();
        processingParametersMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        fractionDetailsJMenuItem = new javax.swing.JMenuItem();
        preferencesMenuItem = new javax.swing.JMenuItem();
        speciesJMenuItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        javaOptionsJMenuItem = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        findJMenuItem = new javax.swing.JMenuItem();
        starHideJMenuItem = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        toolsMenu = new javax.swing.JMenu();
        searchGuiPreferencesJMenuItem = new javax.swing.JMenuItem();
        reporterPreferencesJMenuItem = new javax.swing.JMenuItem();
        exportJMenu = new javax.swing.JMenu();
        identificationFeaturesMenuItem = new javax.swing.JMenuItem();
        followUpAnalysisMenuItem = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        exportProjectMenuItem = new javax.swing.JMenuItem();
        exportPrideMenuItem = new javax.swing.JMenuItem();
        viewJMenu = new javax.swing.JMenu();
        overViewTabViewMenu = new javax.swing.JMenu();
        proteinsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        peptidesAndPsmsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        spectrumJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sequenceCoverageJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        sparklinesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        spectrumSlidersCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        fixedModsJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        scoresJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        validatedProteinsOnlyJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpJMenuItem = new javax.swing.JMenuItem();
        gettingStartedMenuItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        logReportMenu = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
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

        splitterMenu8.setText("|");
        splitterMenu8.setEnabled(false);
        annotationMenuBar.add(splitterMenu8);

        otherMenu.setText("Other");
        otherMenu.setEnabled(false);

        precursorCheckMenu.setSelected(true);
        precursorCheckMenu.setText("Precursor");
        precursorCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(precursorCheckMenu);

        immoniumIonsCheckMenu.setSelected(true);
        immoniumIonsCheckMenu.setText("Immonium");
        immoniumIonsCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                immoniumIonsCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(immoniumIonsCheckMenu);

        reporterIonsCheckMenu.setSelected(true);
        reporterIonsCheckMenu.setText("Reporter");
        reporterIonsCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reporterIonsCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(reporterIonsCheckMenu);

        annotationMenuBar.add(otherMenu);

        lossSplitter.setText("|");
        lossSplitter.setEnabled(false);
        annotationMenuBar.add(lossSplitter);

        lossMenu.setText("Loss");
        lossMenu.setEnabled(false);
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

        splitterMenu2.setText("|");
        splitterMenu2.setEnabled(false);
        annotationMenuBar.add(splitterMenu2);

        chargeMenu.setText("Charge");
        chargeMenu.setEnabled(false);
        annotationMenuBar.add(chargeMenu);

        splitterMenu3.setText("|");
        splitterMenu3.setEnabled(false);
        annotationMenuBar.add(splitterMenu3);

        deNovoMenu.setText("De Novo");

        forwardIonsDeNovoCheckBoxMenuItem.setText("f-ions");
        forwardIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(forwardIonsDeNovoCheckBoxMenuItem);

        rewindIonsDeNovoCheckBoxMenuItem.setText("r-ions");
        rewindIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rewindIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(rewindIonsDeNovoCheckBoxMenuItem);
        deNovoMenu.add(jSeparator19);

        deNovoChargeButtonGroup.add(deNovoChargeOneJRadioButtonMenuItem);
        deNovoChargeOneJRadioButtonMenuItem.setSelected(true);
        deNovoChargeOneJRadioButtonMenuItem.setText("Single Charge");
        deNovoChargeOneJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deNovoChargeOneJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(deNovoChargeOneJRadioButtonMenuItem);

        deNovoChargeButtonGroup.add(deNovoChargeTwoJRadioButtonMenuItem);
        deNovoChargeTwoJRadioButtonMenuItem.setText("Double Charge");
        deNovoChargeTwoJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deNovoChargeTwoJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(deNovoChargeTwoJRadioButtonMenuItem);

        annotationMenuBar.add(deNovoMenu);

        splitterMenu9.setText("|");
        splitterMenu9.setEnabled(false);
        annotationMenuBar.add(splitterMenu9);

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
        settingsMenu.add(jSeparator14);

        annotationColorsJMenuItem.setText("Annotation Colors");
        annotationColorsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationColorsJMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(annotationColorsJMenuItem);

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

        exportSpectrumAndPlotsGraphicsJMenuItem.setText("Spectrum & Plots");
        exportSpectrumAndPlotsGraphicsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumAndPlotsGraphicsJMenuItemActionPerformed(evt);
            }
        });
        exportSpectrumMenu.add(exportSpectrumAndPlotsGraphicsJMenuItem);
        exportSpectrumMenu.add(exportSpectrumGraphicsSeparator);

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

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));
        backgroundPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

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

        proteinFractionsJPanel.setOpaque(false);
        proteinFractionsJPanel.setLayout(new javax.swing.BoxLayout(proteinFractionsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Fractions", proteinFractionsJPanel);

        ptmJPanel.setOpaque(false);
        ptmJPanel.setLayout(new javax.swing.BoxLayout(ptmJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Modifications", ptmJPanel);

        proteinStructureJPanel.setOpaque(false);
        proteinStructureJPanel.setLayout(new javax.swing.BoxLayout(proteinStructureJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("3D Structures", proteinStructureJPanel);

        annotationsJPanel.setOpaque(false);
        annotationsJPanel.setLayout(new javax.swing.BoxLayout(annotationsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Annotation", annotationsJPanel);

        goJPanel.setOpaque(false);
        goJPanel.setLayout(new javax.swing.BoxLayout(goJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("GO Analysis", goJPanel);

        statsJPanel.setOpaque(false);
        statsJPanel.setLayout(new javax.swing.BoxLayout(statsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("Validation", statsJPanel);

        qcJPanel.setOpaque(false);
        qcJPanel.setLayout(new javax.swing.BoxLayout(qcJPanel, javax.swing.BoxLayout.LINE_AXIS));
        allTabsJTabbedPane.addTab("QC Plots", qcJPanel);

        backgroundLayeredPane.add(allTabsJTabbedPane);
        allTabsJTabbedPane.setBounds(0, 0, 1280, 860);

        newsButton.setBackground(new java.awt.Color(204, 204, 204));
        newsButton.setFont(newsButton.getFont().deriveFont(newsButton.getFont().getStyle() | java.awt.Font.BOLD));
        newsButton.setForeground(new java.awt.Color(255, 255, 255));
        newsButton.setText("News");
        newsButton.setBorder(null);
        newsButton.setContentAreaFilled(false);
        newsButton.setOpaque(true);
        newsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                newsButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                newsButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                newsButtonMouseExited(evt);
            }
        });
        backgroundLayeredPane.add(newsButton);
        newsButton.setBounds(1205, 825, 70, 20);
        backgroundLayeredPane.setLayer(newsButton, javax.swing.JLayeredPane.MODAL_LAYER);

        notesButton.setBackground(new java.awt.Color(204, 204, 204));
        notesButton.setFont(notesButton.getFont().deriveFont(notesButton.getFont().getStyle() | java.awt.Font.BOLD));
        notesButton.setForeground(new java.awt.Color(255, 255, 255));
        notesButton.setText("Notes");
        notesButton.setBorder(null);
        notesButton.setBorderPainted(false);
        notesButton.setContentAreaFilled(false);
        notesButton.setOpaque(true);
        notesButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                notesButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                notesButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                notesButtonMouseExited(evt);
            }
        });
        backgroundLayeredPane.add(notesButton);
        notesButton.setBounds(1205, 800, 70, 20);
        backgroundLayeredPane.setLayer(notesButton, javax.swing.JLayeredPane.MODAL_LAYER);

        tipsButton.setBackground(new java.awt.Color(204, 204, 204));
        tipsButton.setFont(tipsButton.getFont().deriveFont(tipsButton.getFont().getStyle() | java.awt.Font.BOLD));
        tipsButton.setForeground(new java.awt.Color(255, 255, 255));
        tipsButton.setText("Tips");
        tipsButton.setBorder(null);
        tipsButton.setContentAreaFilled(false);
        tipsButton.setOpaque(true);
        tipsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tipsButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                tipsButtonMouseExited(evt);
            }
        });
        backgroundLayeredPane.add(tipsButton);
        tipsButton.setBounds(1205, 775, 70, 20);
        backgroundLayeredPane.setLayer(tipsButton, javax.swing.JLayeredPane.MODAL_LAYER);

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1278, Short.MAX_VALUE)
            .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(backgroundLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1278, Short.MAX_VALUE))
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 862, Short.MAX_VALUE)
            .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(backgroundLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 862, Short.MAX_VALUE))
        );

        menuBar.setBackground(new java.awt.Color(255, 255, 255));

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        newJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newJMenuItem.setMnemonic('N');
        newJMenuItem.setText("New Project...");
        newJMenuItem.setToolTipText("Create a new PeptideShaker project");
        newJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(newJMenuItem);
        fileJMenu.add(jSeparator18);

        openJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openJMenuItem.setMnemonic('O');
        openJMenuItem.setText("Open Project...");
        openJMenuItem.setToolTipText("Open an existing PeptideShaker project");
        openJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openJMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(openJMenuItem);

        openRecentJMenu.setMnemonic('R');
        openRecentJMenu.setText("Open Recent Project");
        fileJMenu.add(openRecentJMenu);
        fileJMenu.add(jSeparator8);

        reshakeMenuItem.setMnemonic('E');
        reshakeMenuItem.setText("Reshake...");
        reshakeMenuItem.setToolTipText("<html>\nReanalyze PRIDE experiments.<br>\n(Coming soon...)\n</html>");
        reshakeMenuItem.setEnabled(false);
        reshakeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reshakeMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(reshakeMenuItem);

        quantifyMenuItem.setMnemonic('I');
        quantifyMenuItem.setText("Reporter Ions...");
        quantifyMenuItem.setToolTipText("<html>\nQuantify your proteins using reporter ions.<br>\n(Coming soon...)\n</html>");
        quantifyMenuItem.setEnabled(false);
        quantifyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quantifyMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(quantifyMenuItem);
        fileJMenu.add(jSeparator2);

        projectPropertiesMenuItem.setMnemonic('P');
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
        saveMenuItem.setText("Save");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('V');
        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.setEnabled(false);
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(saveAsMenuItem);
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

        searchParametersMenu.setMnemonic('S');
        searchParametersMenu.setText("Search Settings");
        searchParametersMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchParametersMenuActionPerformed(evt);
            }
        });
        editMenu.add(searchParametersMenu);

        importFilterMenu.setMnemonic('I');
        importFilterMenu.setText("Import Filters");
        importFilterMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFilterMenuActionPerformed(evt);
            }
        });
        editMenu.add(importFilterMenu);

        processingParametersMenuItem.setMnemonic('c');
        processingParametersMenuItem.setText("Processing Preferences");
        processingParametersMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processingParametersMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(processingParametersMenuItem);
        editMenu.add(jSeparator4);

        annotationPreferencesMenu.setMnemonic('A');
        annotationPreferencesMenu.setText("Spectrum Annotations");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        editMenu.add(annotationPreferencesMenu);

        fractionDetailsJMenuItem.setMnemonic('R');
        fractionDetailsJMenuItem.setText("Fraction Details");
        fractionDetailsJMenuItem.setEnabled(false);
        fractionDetailsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fractionDetailsJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(fractionDetailsJMenuItem);

        preferencesMenuItem.setMnemonic('P');
        preferencesMenuItem.setText("Preferences");
        preferencesMenuItem.setEnabled(false);
        preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferencesMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(preferencesMenuItem);

        speciesJMenuItem.setMnemonic('C');
        speciesJMenuItem.setText("Species");
        speciesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speciesJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(speciesJMenuItem);
        editMenu.add(jSeparator13);

        javaOptionsJMenuItem.setMnemonic('O');
        javaOptionsJMenuItem.setText("Java Options");
        javaOptionsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                javaOptionsJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(javaOptionsJMenuItem);
        editMenu.add(jSeparator12);

        findJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        findJMenuItem.setMnemonic('F');
        findJMenuItem.setText("Find...");
        findJMenuItem.setToolTipText("Find a protein or peptide");
        findJMenuItem.setEnabled(false);
        findJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(findJMenuItem);

        starHideJMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        starHideJMenuItem.setMnemonic('L');
        starHideJMenuItem.setText("Filters");
        starHideJMenuItem.setEnabled(false);
        starHideJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                starHideJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(starHideJMenuItem);
        editMenu.add(jSeparator15);

        toolsMenu.setMnemonic('T');
        toolsMenu.setText("Tools");

        searchGuiPreferencesJMenuItem.setText("SearchGUI");
        searchGuiPreferencesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchGuiPreferencesJMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(searchGuiPreferencesJMenuItem);

        reporterPreferencesJMenuItem.setText("Reporter");
        reporterPreferencesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reporterPreferencesJMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(reporterPreferencesJMenuItem);

        editMenu.add(toolsMenu);

        menuBar.add(editMenu);

        exportJMenu.setMnemonic('x');
        exportJMenu.setText("Export");

        identificationFeaturesMenuItem.setMnemonic('I');
        identificationFeaturesMenuItem.setText("Identification Features");
        identificationFeaturesMenuItem.setEnabled(false);
        identificationFeaturesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                identificationFeaturesMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(identificationFeaturesMenuItem);

        followUpAnalysisMenuItem.setMnemonic('F');
        followUpAnalysisMenuItem.setText("Follow Up Analysis");
        followUpAnalysisMenuItem.setEnabled(false);
        followUpAnalysisMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                followUpAnalysisMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(followUpAnalysisMenuItem);
        exportJMenu.add(jSeparator10);

        exportProjectMenuItem.setMnemonic('E');
        exportProjectMenuItem.setText("PeptideShaker Project");
        exportProjectMenuItem.setToolTipText("Export the complete project as a zip file");
        exportProjectMenuItem.setEnabled(false);
        exportProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProjectMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportProjectMenuItem);

        exportPrideMenuItem.setMnemonic('P');
        exportPrideMenuItem.setText("PRIDE XML");
        exportPrideMenuItem.setToolTipText("Export the project as PRIDE XML");
        exportPrideMenuItem.setEnabled(false);
        exportPrideMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPrideMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(exportPrideMenuItem);

        menuBar.add(exportJMenu);

        viewJMenu.setMnemonic('V');
        viewJMenu.setText("View");

        overViewTabViewMenu.setMnemonic('O');
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

        sparklinesJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        sparklinesJCheckBoxMenuItem.setMnemonic('J');
        sparklinesJCheckBoxMenuItem.setSelected(true);
        sparklinesJCheckBoxMenuItem.setText("JSparklines");
        sparklinesJCheckBoxMenuItem.setToolTipText("View sparklines or the underlying numbers");
        sparklinesJCheckBoxMenuItem.setEnabled(false);
        sparklinesJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sparklinesJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(sparklinesJCheckBoxMenuItem);

        spectrumSlidersCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        spectrumSlidersCheckBoxMenuItem.setMnemonic('L');
        spectrumSlidersCheckBoxMenuItem.setText("Spectrum Sliders");
        spectrumSlidersCheckBoxMenuItem.setToolTipText("Show the accuracy and intensity level sliders");
        spectrumSlidersCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumSlidersCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(spectrumSlidersCheckBoxMenuItem);
        viewJMenu.add(jSeparator11);

        fixedModsJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        fixedModsJCheckBoxMenuItem.setMnemonic('F');
        fixedModsJCheckBoxMenuItem.setText("Fixed Modifications");
        fixedModsJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixedModsJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(fixedModsJCheckBoxMenuItem);

        scoresJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        scoresJCheckBoxMenuItem.setMnemonic('c');
        scoresJCheckBoxMenuItem.setText("Scores");
        scoresJCheckBoxMenuItem.setEnabled(false);
        scoresJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scoresJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(scoresJCheckBoxMenuItem);

        validatedProteinsOnlyJCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        validatedProteinsOnlyJCheckBoxMenuItem.setMnemonic('N');
        validatedProteinsOnlyJCheckBoxMenuItem.setText("Validated Proteins Only");
        validatedProteinsOnlyJCheckBoxMenuItem.setEnabled(false);
        validatedProteinsOnlyJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validatedProteinsOnlyJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewJMenu.add(validatedProteinsOnlyJCheckBoxMenuItem);

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

        gettingStartedMenuItem.setMnemonic('G');
        gettingStartedMenuItem.setText("Getting Started...");
        gettingStartedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gettingStartedMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(gettingStartedMenuItem);
        helpMenu.add(jSeparator17);

        logReportMenu.setMnemonic('B');
        logReportMenu.setText("Bug Report");
        logReportMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logReportMenuActionPerformed(evt);
            }
        });
        helpMenu.add(logReportMenu);
        helpMenu.add(jSeparator16);

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
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1278, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 862, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a dialog where the identification files to analyzed are selected.
     *
     * @param evt
     */
    private void newJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newJMenuItemActionPerformed

        if (!dataSaved && getExperiment() != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getExperiment().getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                new NewDialog(null, this, true);
            } else if (value == JOptionPane.CANCEL_OPTION) {
                // do nothing
            } else { // no option
                new NewDialog(null, this, true);
            }
        } else {
            new NewDialog(null, this, true);
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
     * Open the Save & Export dialog.
     *
     * @param evt
     */
    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        new SaveDialog(this, true);
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    /**
     * Opens the Help dialog.
     *
     * @param evt
     */
    private void helpJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/helpFiles/PeptideShaker.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
    }//GEN-LAST:event_helpJMenuItemActionPerformed

    /**
     * Opens the About dialog.
     *
     * @param evt
     */
    private void aboutJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/helpFiles/AboutPeptideShaker.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "About PeptideShaker");
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

        if (isVisible()) {

            // resize the background panel
            backgroundLayeredPane.getComponent(3).setBounds(0, 0, backgroundLayeredPane.getWidth(), backgroundLayeredPane.getHeight());
            backgroundLayeredPane.revalidate();
            backgroundLayeredPane.repaint();

            // move the note, help and news buttons
            backgroundLayeredPane.getComponent(0).setBounds(
                    backgroundLayeredPane.getWidth() - backgroundLayeredPane.getComponent(0).getWidth() - 12,
                    backgroundLayeredPane.getHeight() - backgroundLayeredPane.getComponent(0).getHeight() - 15,
                    backgroundLayeredPane.getComponent(0).getWidth(),
                    backgroundLayeredPane.getComponent(0).getHeight());

            backgroundLayeredPane.getComponent(1).setBounds(
                    backgroundLayeredPane.getWidth() - backgroundLayeredPane.getComponent(1).getWidth() - 12,
                    backgroundLayeredPane.getHeight() - backgroundLayeredPane.getComponent(1).getHeight() - 37,
                    backgroundLayeredPane.getComponent(1).getWidth(),
                    backgroundLayeredPane.getComponent(1).getHeight());

            backgroundLayeredPane.getComponent(2).setBounds(
                    backgroundLayeredPane.getWidth() - backgroundLayeredPane.getComponent(2).getWidth() - 12,
                    backgroundLayeredPane.getHeight() - backgroundLayeredPane.getComponent(2).getHeight() - 59,
                    backgroundLayeredPane.getComponent(2).getWidth(),
                    backgroundLayeredPane.getComponent(2).getHeight());

            if (overviewPanel != null) {
                overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                        sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
                overviewPanel.updateSeparators();
            }
            if (statsPanel != null) {
                statsPanel.updateSeparators();
            }
        }
    }//GEN-LAST:event_formComponentResized

    /**
     * Open the SearchPreferencesDialog.
     *
     * @param evt
     */
    private void searchParametersMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchParametersMenuActionPerformed

        SearchParameters searchParameters = getSearchParameters();
        if (searchParameters == null) {
            setDefaultPreferences();
            searchParameters = getSearchParameters();
        }

        // set the default enzyme if not set
        if (searchParameters.getEnzyme() == null) {
            searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
        }

        new SearchSettingsDialog(this, this, searchParameters,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true, true);
    }//GEN-LAST:event_searchParametersMenuActionPerformed

    /**
     * Show or hide the sparklines.
     *
     * @param evt
     */
    private void sparklinesJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sparklinesJCheckBoxMenuItemActionPerformed
        overviewPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        proteinFractionsPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        ptmPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        spectrumIdentificationPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        proteinStructurePanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
        goPanel.showSparkLines(sparklinesJCheckBoxMenuItem.isSelected());
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
     * Export the spectrum as a figure.
     *
     * @param evt
     */
    private void exportSpectrumGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumGraphicsJMenuItemActionPerformed

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, this, true, (Component) overviewPanel.getSpectrum());
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            new ExportGraphicsDialog(this, this, true, (Component) spectrumIdentificationPanel.getSpectrum());
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            new ExportGraphicsDialog(this, this, true, (Component) ptmPanel.getSpectrum());
        }
    }//GEN-LAST:event_exportSpectrumGraphicsJMenuItemActionPerformed

    /**
     * Update the menu items available on the export graphics menu to only show
     * the ones for the current tab.
     *
     * @param evt
     */
    private void allTabsJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_allTabsJTabbedPaneStateChanged

        if (getIdentification() != null) {

            final int selectedIndex = allTabsJTabbedPane.getSelectedIndex();

            // check if we have re-loaded the data using the current threshold and PEP window settings
            if (selectedIndex != VALIDATION_TAB_INDEX && statsPanel.isInitiated()) {

                if ((!statsPanel.thresholdUpdated() || !statsPanel.pepWindowApplied())) {

                    int value = JOptionPane.showConfirmDialog(
                            this, "Discard the current validation settings?", "Discard Settings?",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                    if (value == JOptionPane.YES_OPTION) {
                        // do nothing
                        statsPanel.setThresholdAndPepApplied();
                        updateNeeded.put(VALIDATION_TAB_INDEX, true);
                    } else {
                        updateNeeded.put(VALIDATION_TAB_INDEX, false);
                        allTabsJTabbedPane.setSelectedIndex(VALIDATION_TAB_INDEX);
                    }
                }
            }

            new Thread(new Runnable() {
                public void run() {

                    if (selectedIndex == OVER_VIEW_TAB_INDEX) {
                        if (updateNeeded.get(OVER_VIEW_TAB_INDEX)) {
                            overviewPanel.displayResults();
                        } else {
                            overviewPanel.updateSelection(true);
                        }
                    } else if (selectedIndex == PROTEIN_FRACTIONS_TAB_INDEX) {
                        if (updateNeeded.get(PROTEIN_FRACTIONS_TAB_INDEX)) {
                            proteinFractionsPanel.displayResults();
                        } else {
                            proteinFractionsPanel.updateSelection();
                        }
                    } else if (selectedIndex == STRUCTURES_TAB_INDEX) {
                        if (updateNeeded.get(STRUCTURES_TAB_INDEX)) {
                            //@TODO: here the panel is actually emptied and reloaded. The displayResults() method should ideally load results when nothing is displayed and simply update the results otherwise.
                            resetPanel(STRUCTURES_TAB_INDEX);
                            proteinStructurePanel.displayResults();
                        } else {
                            proteinStructurePanel.updateSelection(true);
                        }
                    } else if (selectedIndex == GO_ANALYSIS_TAB_INDEX
                            && updateNeeded.get(GO_ANALYSIS_TAB_INDEX)) {
                        resetPanel(GO_ANALYSIS_TAB_INDEX);
                        goPanel.displayResults();
                        // @TODO: reload GO enrichment tab if hidden selection is changed!
                    } else if (selectedIndex == SPECTRUM_ID_TAB_INDEX) {
                        if (updateNeeded.get(SPECTRUM_ID_TAB_INDEX)) {
                            resetPanel(SPECTRUM_ID_TAB_INDEX);
                            spectrumIdentificationPanel.displayResults();
                        } else {
                            spectrumIdentificationPanel.updateSelection();
                        }
                    } else if (selectedIndex == MODIFICATIONS_TAB_INDEX) {
                        if (updateNeeded.get(MODIFICATIONS_TAB_INDEX)) {
                            ptmPanel.displayResults();
                        } else {
                            ptmPanel.updateSelection();
                        }
                    } else if (selectedIndex == QC_PLOTS_TAB_INDEX
                            && updateNeeded.get(QC_PLOTS_TAB_INDEX)) {
                        qcPanel.displayResults();
                    } else if (selectedIndex == VALIDATION_TAB_INDEX
                            && updateNeeded.get(VALIDATION_TAB_INDEX)) {
                        statsPanel.displayResults();
                    }

                    // update the basic protein annotation
                    if (selectedIndex == ANNOTATION_TAB_INDEX) {
                        try {
                            if (getIdentification().getProteinMatch(selectedProteinKey) != null) {
                                annotationPanel.updateBasicProteinAnnotation(getIdentification().getProteinMatch(selectedProteinKey).getMainMatch());
                            }
                        } catch (Exception e) {
                            catchException(e);
                        }
                    }

                    // move the spectrum annotation menu bar and set the intensity slider value
                    if (selectedIndex == OVER_VIEW_TAB_INDEX) {
                        overviewPanel.showSpectrumAnnotationMenu();
                        overviewPanel.setIntensitySliderValue((int) (getAnnotationPreferences().getAnnotationIntensityLimit() * 100));
                    } else if (selectedIndex == SPECTRUM_ID_TAB_INDEX) {
                        spectrumIdentificationPanel.showSpectrumAnnotationMenu();
                        spectrumIdentificationPanel.setIntensitySliderValue((int) (getAnnotationPreferences().getAnnotationIntensityLimit() * 100));
                    } else if (selectedIndex == MODIFICATIONS_TAB_INDEX) {
                        ptmPanel.showSpectrumAnnotationMenu();
                        ptmPanel.setIntensitySliderValue((int) (getAnnotationPreferences().getAnnotationIntensityLimit() * 100));
                    }

                    if (selectedIndex == OVER_VIEW_TAB_INDEX || selectedIndex == SPECTRUM_ID_TAB_INDEX || selectedIndex == MODIFICATIONS_TAB_INDEX) {
                        // invoke later to give time for components to update
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                updateSpectrumAnnotations();
                            }
                        });
                    }

                    if (selectedIndex == OVER_VIEW_TAB_INDEX
                            || selectedIndex == MODIFICATIONS_TAB_INDEX
                            || selectedIndex == STRUCTURES_TAB_INDEX
                            || selectedIndex == PROTEIN_FRACTIONS_TAB_INDEX) {
                        jumpToPanel.setEnabled(true);
                        jumpToPanel.setType(JumpToPanel.JumpType.proteinAndPeptides);
                    } else if (selectedIndex == SPECTRUM_ID_TAB_INDEX) {
                        jumpToPanel.setEnabled(true);
                        jumpToPanel.setType(JumpToPanel.JumpType.spectrum);
                    } else {
                        jumpToPanel.setEnabled(false);
                    }

                    // change jump to color
                    jumpToPanel.setColor(Color.black);

                }
            }, "TabThread").start();
        }
    }//GEN-LAST:event_allTabsJTabbedPaneStateChanged

    /**
     * Export the bubble plot.
     *
     * @param evt
     */
    private void bubblePlotJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bubblePlotJMenuItemActionPerformed
        new ExportGraphicsDialog(this, this, true, (Component) overviewPanel.getBubblePlot());
    }//GEN-LAST:event_bubblePlotJMenuItemActionPerformed

    /**
     * Test if there are unsaved changes and if so asks the user if he/she wants
     * to save these. If not closes the tool.
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
        getDisplayPreferences().showScores(scoresJCheckBoxMenuItem.isSelected());
        overviewPanel.updateScores();
        proteinStructurePanel.updateScores();
        proteinFractionsPanel.updateScores();

        // make sure that the jsparklines are showing correctly
        sparklinesJCheckBoxMenuItemActionPerformed(null);
    }//GEN-LAST:event_scoresJCheckBoxMenuItemActionPerformed

    /**
     * Open a file chooser to open an existing PeptideShaker project.
     *
     * @param evt
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed

        if (!dataSaved && getExperiment() != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getExperiment().getReference() + "?",
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

        File selectedFile = getUserSelectedFile(".cps", "Supported formats: PeptideShaker (.cps)", "Open PeptideShaker Project", true);

        if (selectedFile != null) {
            if (!selectedFile.getName().toLowerCase().endsWith("cps")) {
                JOptionPane.showMessageDialog(this, "Not a PeptideShaker file (.cps).",
                        "Wrong File.", JOptionPane.ERROR_MESSAGE);
            } else {
                exceptionHandler.setIgnoreExceptions(true);
                clearData(true, true);
                exceptionHandler.setIgnoreExceptions(false);
                clearPreferences();
                getUserPreferences().addRecentProject(selectedFile);
                updateRecentProjectsList();
                importPeptideShakerFile(selectedFile);
                lastSelectedFolder = selectedFile.getAbsolutePath();
            }
        }
    }//GEN-LAST:event_openJMenuItemActionPerformed

    /**
     * Open the GUI Settings dialog.
     *
     * @param evt
     */
    private void preferencesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferencesMenuItemActionPerformed
        new PreferencesDialog(this, true);
    }//GEN-LAST:event_preferencesMenuItemActionPerformed

    /**
     * Open the features export dialog.
     *
     * @param evt
     */
    private void identificationFeaturesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_identificationFeaturesMenuItemActionPerformed
        new FeaturesPreferencesDialog(this);
    }//GEN-LAST:event_identificationFeaturesMenuItemActionPerformed

    /**
     * Open the follow up export dialog.
     *
     * @param evt
     */
    private void followUpAnalysisMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_followUpAnalysisMenuItemActionPerformed
        new FollowupPreferencesDialog(this);
    }//GEN-LAST:event_followUpAnalysisMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void aIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_aIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void bIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_bIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void cIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_cIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void xIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_xIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void yIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_yIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void zIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_zIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void allCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_allCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void barsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_barsCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_barsCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void intensityIonTableRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intensityIonTableRadioButtonMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_intensityIonTableRadioButtonMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
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
                new HelpDialog(this, getClass().getResource("/helpFiles/IonTable.html"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        "PeptideShaker - Help");
            } else if (spectrumTabIndex == 1) {
                new HelpDialog(this, getClass().getResource("/helpFiles/BubblePlot.html"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        "PeptideShaker - Help");
            } else if (spectrumTabIndex == 2) {
                new HelpDialog(this, getClass().getResource("/helpFiles/SpectrumPanel.html"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        "PeptideShaker - Help");
            }
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            new HelpDialog(this, getClass().getResource("/helpFiles/SpectrumPanel.html"),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            new HelpDialog(this, getClass().getResource("/helpFiles/PTMPanel.html"),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
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

            File selectedFile = getUserSelectedFile(".mgf", "(Mascot Generic Format) *.mgf", "Save As...", false);

            if (selectedFile != null) {
                try {
                    FileWriter w = new FileWriter(selectedFile);
                    BufferedWriter bw = new BufferedWriter(w);
                    bw.write(spectrumAsMgf);
                    bw.close();
                    w.close();

                    JOptionPane.showMessageDialog(this, "Spectrum saved to " + selectedFile.getPath() + ".",
                            "File Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "An error occured while saving " + selectedFile.getPath() + ".\n"
                            + "See resources/PeptideShaker.log for details.", "Save Error", JOptionPane.WARNING_MESSAGE);
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
            try {
                getAnnotationPreferences().resetAutomaticAnnotation(PeptideShaker.MATCHING_TYPE, getSearchParameters().getFragmentIonAccuracy());
            } catch (Exception e) {
                catchException(e);
            }

            for (int availableCharge : chargeMenus.keySet()) {
                chargeMenus.get(availableCharge).setSelected(getAnnotationPreferences().getValidatedCharges().contains(availableCharge));
            }
        }

        updateAnnotationPreferences();
    }//GEN-LAST:event_automaticAnnotationCheckBoxMenuItemActionPerformed

    /**
     * Open the project details dialog.
     *
     * @param evt
     */
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
            new ExportGraphicsDialog(this, this, true, (Component) overviewPanel.getSequenceFragmentationPlot());
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

            new ExportGraphicsDialog(this, this, true, tempChartPanel);
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

                new ExportGraphicsDialog(this, this, true, tempChartPanel);
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
        ImportSettingsDialog importSettingsDialog = new ImportSettingsDialog(this, getIdFilter(), true);
        IdFilter newFilter = importSettingsDialog.getFilter();
        if (newFilter != null) {
            setIdFilter(newFilter);
        }
    }//GEN-LAST:event_importFilterMenuActionPerformed

    /**
     * Save the current project.
     *
     * @param evt
     */
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        if (cpsBean.getCpsFile() != null && cpsBean.getCpsFile().exists()) {
            saveProject(false);
        } else {
            saveProjectAs(false);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    /**
     * Hide/display the spectrum accuracy and intensity level sliders.
     *
     * @param evt
     */
    private void spectrumSlidersCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumSlidersCheckBoxMenuItemActionPerformed
        cpsBean.getUserPreferences().setShowSliders(spectrumSlidersCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
        spectrumIdentificationPanel.updateSeparators();
        ptmPanel.updateSeparators();
    }//GEN-LAST:event_spectrumSlidersCheckBoxMenuItemActionPerformed

    /**
     * Open the filter dialog.
     *
     * @param evt
     */
    private void starHideJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_starHideJMenuItemActionPerformed
        new FiltersDialog(this);
    }//GEN-LAST:event_starHideJMenuItemActionPerformed

    /**
     * Select the Jump To text field.
     *
     * @param evt
     */
    private void findJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findJMenuItemActionPerformed
        jumpToPanel.selectTextField();
    }//GEN-LAST:event_findJMenuItemActionPerformed

    /**
     * Opens a new bug report dialog.
     *
     * @param evt
     */
    private void logReportMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logReportMenuActionPerformed
        new BugReport(this, lastSelectedFolder, "PeptideShaker", "peptide-shaker", getVersion(), new File(getJarFilePath() + "/resources/PeptideShaker.log"));
    }//GEN-LAST:event_logReportMenuActionPerformed

    /**
     * Open the Java Options menu.
     *
     * @param evt
     */
    private void javaOptionsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaOptionsJMenuItemActionPerformed

        // reload the user preferences as these may have been changed by other tools
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occured when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        new JavaOptionsDialog(this, this, null, "PeptideShaker");
    }//GEN-LAST:event_javaOptionsJMenuItemActionPerformed

    /**
     * Update annotations.
     *
     * @param evt
     */
    private void adaptCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adaptCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_adaptCheckBoxMenuItemActionPerformed

    /**
     * Open the spectrum colors dialog.
     *
     * @param evt
     */
    private void annotationColorsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationColorsJMenuItemActionPerformed

        // reload the user preferences as these may have been changed by other tools
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occured when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        new SpectrumColorsDialog(this);
    }//GEN-LAST:event_annotationColorsJMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void precursorCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorCheckMenuActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_precursorCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void immoniumIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_immoniumIonsCheckMenuActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_immoniumIonsCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void reporterIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterIonsCheckMenuActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_reporterIonsCheckMenuActionPerformed

    /**
     * Open a SearchGuiSetupDialog were the user can setup the SearchGUI link.
     *
     * @param evt
     */
    private void searchGuiPreferencesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchGuiPreferencesJMenuItemActionPerformed
        try {
            new SearchGuiSetupDialog(this, true);
            loadUserPreferences();
        } catch (Exception ex) {
            catchException(ex);
        }
    }//GEN-LAST:event_searchGuiPreferencesJMenuItemActionPerformed

    /**
     * Open the Getting Started tutorial.
     *
     * @param evt
     */
    private void gettingStartedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gettingStartedMenuItemActionPerformed
        new GettingStartedDialog(this, null, false);
    }//GEN-LAST:event_gettingStartedMenuItemActionPerformed

    /**
     * Open the ProcessingPreferencesDialog.
     *
     * @param evt
     */
    private void processingParametersMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processingParametersMenuItemActionPerformed
        new ProcessingPreferencesDialog(this, false, getProcessingPreferences(), getPtmScoringPreferences());
    }//GEN-LAST:event_processingParametersMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void forwardIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void rewindIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * Open the fraction details dialog.
     *
     * @param evt
     */
    private void fractionDetailsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fractionDetailsJMenuItemActionPerformed
        new FractionDetailsDialog(this, true);
    }//GEN-LAST:event_fractionDetailsJMenuItemActionPerformed

    /**
     * Start the Reshake dialog.
     *
     * @param evt
     */
    private void reshakeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reshakeMenuItemActionPerformed
        //new PrideReshakeGui(this, null, true); // not in use
    }//GEN-LAST:event_reshakeMenuItemActionPerformed

    /**
     * Start Reporter.
     *
     * @param evt
     */
    private void quantifyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quantifyMenuItemActionPerformed

        final PeptideShakerGUI finalRef = this;

        new Thread(new Runnable() {
            public void run() {
                try {
                    ToolFactory.startReporter(finalRef);
                } catch (Exception e) {
                    catchException(e);
                }
            }
        }, "StartReporter").start();
    }//GEN-LAST:event_quantifyMenuItemActionPerformed

    /**
     * Open the ReporterSetupDialog were the user can setup the Reporter link.
     *
     * @param evt
     */
    private void reporterPreferencesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterPreferencesJMenuItemActionPerformed
        try {
            new ReporterSetupDialog(this, true);
            loadUserPreferences();
        } catch (Exception ex) {
            catchException(ex);
        }
    }//GEN-LAST:event_reporterPreferencesJMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void deNovoChargeOneJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void deNovoChargeTwoJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void tipsButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tipsButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_tipsButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void tipsButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tipsButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_tipsButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void notesButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_notesButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void notesButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_notesButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void newsButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newsButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_newsButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void newsButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newsButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_newsButtonMouseExited

    /**
     * Show/hide the fixed modifications.
     *
     * @param evt
     */
    private void fixedModsJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixedModsJCheckBoxMenuItemActionPerformed

        // @TODO: replace by user select ptm visability
        if (fixedModsJCheckBoxMenuItem.isSelected()) {
            for (String ptm : getSearchParameters().getModificationProfile().getFixedModifications()) {
                getDisplayPreferences().setDisplayedPTM(ptm, true);
            }
        } else {
            for (String ptm : getSearchParameters().getModificationProfile().getFixedModifications()) {
                getDisplayPreferences().setDisplayedPTM(ptm, false);
            }
        }

        updatePtmColorCoding();
        displayFeaturesGenerator.setDisplayedPTMs(getDisplayPreferences().getDisplayedPtms());
    }//GEN-LAST:event_fixedModsJCheckBoxMenuItemActionPerformed

    /**
     * Show/hide the not validated proteins.
     *
     * @param evt
     */
    private void validatedProteinsOnlyJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validatedProteinsOnlyJCheckBoxMenuItemActionPerformed
        getDisplayPreferences().showValidatedProteinsOnly(validatedProteinsOnlyJCheckBoxMenuItem.isSelected());

        resetSelectedItems();
        setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
        setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
        setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
        setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
        setUpdated(PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX, false);
        setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
        setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);

        updateTabbedPanes();
    }//GEN-LAST:event_validatedProteinsOnlyJCheckBoxMenuItemActionPerformed

    /**
     * Export the spectrum and plots. Only for the Overview tab.
     *
     * @param evt
     */
    private void exportSpectrumAndPlotsGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumAndPlotsGraphicsJMenuItemActionPerformed
        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, this, true, (Component) overviewPanel.getSpectrumAndPlots());
        }
    }//GEN-LAST:event_exportSpectrumAndPlotsGraphicsJMenuItemActionPerformed

    /**
     * Export project to PRIDE XML.
     *
     * @param evt
     */
    private void exportPrideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPrideMenuItemActionPerformed
        new PrideExportDialog(this, true);
    }//GEN-LAST:event_exportPrideMenuItemActionPerformed

    /**
     * Export project as a zip file.
     *
     * @param evt
     */
    private void exportProjectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProjectMenuItemActionPerformed
        exportProjectAsZip();
    }//GEN-LAST:event_exportProjectMenuItemActionPerformed

    /**
     * Open the species selection dialog.
     *
     * @param evt
     */
    private void speciesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speciesJMenuItemActionPerformed
        GenePreferences genePreferences = getGenePreferences();
        String oldSpecies = genePreferences.getCurrentSpecies();
        new SpeciesDialog(this, genePreferences, true, getWaitingIcon(), getNormalIcon());
        String newSpecies = genePreferences.getCurrentSpecies();

        if (oldSpecies == null || !oldSpecies.equals(newSpecies)) {
            clearGeneMappings(); // clear the old mappings
            if (newSpecies != null) {
                loadGeneMappings(); // load the new mappings
            }
            updateGeneDisplay(); // display the new mappings
        }
    }//GEN-LAST:event_speciesJMenuItemActionPerformed

    /**
     * Open the CompOmics twitter page.
     *
     * @param evt
     */
    private void newsButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newsButtonMouseReleased
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("https://twitter.com/compomics");
        newsButton.setText("News");

        // set the tweets as read
        for (String tweetId : newTweets) {
            utilitiesUserPreferences.getReadTweets().add(tweetId);
        }
        for (String tweetId : publishedTweets) {
            utilitiesUserPreferences.getReadTweets().add(tweetId);
        }

        // clear the list of published tweets
        publishedTweets.clear();

        UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_newsButtonMouseReleased

    /**
     * Open the Notes Dialog.
     * 
     * @param evt 
     */
    private void notesButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesButtonMouseReleased
        NotesDialog notesDialog = new NotesDialog(this, false, currentNotes);
        notesDialog.setLocation(PeptideShakerGUI.this.getWidth() - notesDialog.getWidth() - 25 + PeptideShakerGUI.this.getX(),
                PeptideShakerGUI.this.getHeight() - notesDialog.getHeight() - 25 + PeptideShakerGUI.this.getY());
        notesDialog.setVisible(true);
        currentNotes = new ArrayList<String>();
        updateNotesNotificationCounter();
    }//GEN-LAST:event_notesButtonMouseReleased

    /**
     * Loads the enzymes from the enzyme file into the enzyme factory.
     */
    private void loadEnzymes() {
        try {
            enzymeFactory.importEnzymes(new File(getJarFilePath(), PeptideShaker.ENZYME_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Not able to load the enzyme file.", "Wrong enzyme file.", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Imports the gene mapping.
     */
    private void loadGeneMappings() {
        if (!cpsBean.loadGeneMappings(progressDialog)) {
            JOptionPane.showMessageDialog(this, "Unable to load the gene/GO mapping file.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Initiate the display by displaying the data in the currently selected
     * tab. Was previously a part of the displayResults methods, but had to be
     * split into a separate method due to threading issues.
     */
    public void initiateDisplay() {
        allTabsJTabbedPaneStateChanged(null);
    }

    /**
     * This method will display results in all panels.
     */
    public void displayResults() {

        // move to the Overview tab
        allTabsJTabbedPane.setSelectedIndex(0);

        try {
            sequenceCoverageJCheckBoxMenuItem.setSelected(true);

            // Display the variable modifications
            getDisplayPreferences().setDefaultSelection(getSearchParameters().getModificationProfile());

            overviewPanel.setDisplayOptions(true, true, true, true);
            overviewPanel.updateSeparators();
            statsPanel.updateSeparators();

            // reset show scores columns
            scoresJCheckBoxMenuItem.setSelected(false);

            // make sure that all panels are looking the way they should
            repaintPanels();

            // enable the menu items depending on a project being open
            jumpToPanel.setEnabled(true);
            saveMenuItem.setEnabled(true);
            saveAsMenuItem.setEnabled(true);
            identificationFeaturesMenuItem.setEnabled(true);
            followUpAnalysisMenuItem.setEnabled(true);
            projectPropertiesMenuItem.setEnabled(true);
            fractionDetailsJMenuItem.setEnabled(true);
            preferencesMenuItem.setEnabled(true);
            findJMenuItem.setEnabled(true);
            starHideJMenuItem.setEnabled(true);
            ionsMenu.setEnabled(true);
            otherMenu.setEnabled(true);
            lossMenu.setEnabled(true);
            chargeMenu.setEnabled(true);
            settingsMenu.setEnabled(true);
            exportGraphicsMenu.setEnabled(true);
            helpJMenu.setEnabled(true);
            scoresJCheckBoxMenuItem.setEnabled(true);
            sparklinesJCheckBoxMenuItem.setEnabled(true);
            quantifyMenuItem.setEnabled(true);
            exportPrideMenuItem.setEnabled(true);
            exportProjectMenuItem.setEnabled(true);
            speciesJMenuItem.setEnabled(true);

            // disable the fractions tab if only one mgf file
            allTabsJTabbedPane.setEnabledAt(2, getIdentification().getSpectrumFiles().size() > 1);

        } catch (Exception e) {

            // return the peptide shaker icon to the standard version
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

            e.printStackTrace();
            catchException(e);
            JOptionPane.showMessageDialog(null, "A problem occured when loading the data.\n"
                    + "See /resources/PeptideShaker.log for more details.", "Loading Failed!", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method called to disable the spectrum display.
     */
    public void disableSpectrumDisplay() {
        spectrumJPanel.setEnabled(false);
        ptmPanel.setEnabled(false);
        overviewPanel.updateSeparators();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem aIonCheckBoxMenuItem;
    private javax.swing.JMenuItem aboutJMenuItem;
    private javax.swing.JCheckBoxMenuItem adaptCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem allCheckBoxMenuItem;
    private javax.swing.JTabbedPane allTabsJTabbedPane;
    private javax.swing.JMenuItem annotationColorsJMenuItem;
    private javax.swing.JMenuBar annotationMenuBar;
    private javax.swing.JMenuItem annotationPreferencesMenu;
    private javax.swing.JPanel annotationsJPanel;
    private javax.swing.JCheckBoxMenuItem automaticAnnotationCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem bIonCheckBoxMenuItem;
    private javax.swing.JLayeredPane backgroundLayeredPane;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JCheckBoxMenuItem barsCheckBoxMenuItem;
    private javax.swing.JMenuItem bubblePlotJMenuItem;
    private javax.swing.JMenuItem bubbleScaleJMenuItem;
    private javax.swing.JCheckBoxMenuItem cIonCheckBoxMenuItem;
    private javax.swing.JMenu chargeMenu;
    private javax.swing.ButtonGroup deNovoChargeButtonGroup;
    private javax.swing.JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem;
    private javax.swing.JMenu deNovoMenu;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem errorPlotTypeCheckBoxMenuItem;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenu exportGraphicsMenu;
    private javax.swing.JMenuItem exportIntensityHistogramGraphicsJMenuItem;
    private javax.swing.JMenu exportJMenu;
    private javax.swing.JMenuItem exportMassErrorPlotGraphicsJMenuItem;
    private javax.swing.JMenuItem exportPrideMenuItem;
    private javax.swing.JMenuItem exportProjectMenuItem;
    private javax.swing.JMenuItem exportSequenceFragmentationGraphicsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumAndPlotsGraphicsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumGraphicsJMenuItem;
    private javax.swing.JPopupMenu.Separator exportSpectrumGraphicsSeparator;
    private javax.swing.JMenu exportSpectrumMenu;
    private javax.swing.JMenuItem exportSpectrumValuesJMenuItem;
    private javax.swing.JMenu fileJMenu;
    private javax.swing.JMenuItem findJMenuItem;
    private javax.swing.JCheckBoxMenuItem fixedModsJCheckBoxMenuItem;
    private javax.swing.JMenuItem followUpAnalysisMenuItem;
    private javax.swing.JCheckBoxMenuItem forwardIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenuItem fractionDetailsJMenuItem;
    private javax.swing.JMenuItem gettingStartedMenuItem;
    private javax.swing.JPanel goJPanel;
    private javax.swing.JMenu helpJMenu;
    private javax.swing.JMenuItem helpJMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuItem identificationFeaturesMenuItem;
    private javax.swing.JCheckBoxMenuItem immoniumIonsCheckMenu;
    private javax.swing.JMenuItem importFilterMenu;
    private javax.swing.JRadioButtonMenuItem intensityIonTableRadioButtonMenuItem;
    private javax.swing.ButtonGroup ionTableButtonGroup;
    private javax.swing.JMenu ionsMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator15;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator18;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JMenuItem javaOptionsJMenuItem;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenu lossMenu;
    private javax.swing.JMenu lossSplitter;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JRadioButtonMenuItem mzIonTableRadioButtonMenuItem;
    private javax.swing.JMenuItem newJMenuItem;
    private javax.swing.JButton newsButton;
    private javax.swing.JButton notesButton;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JMenu openRecentJMenu;
    private javax.swing.JMenu otherMenu;
    private javax.swing.JMenu overViewTabViewMenu;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JCheckBoxMenuItem peptidesAndPsmsJCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem precursorCheckMenu;
    private javax.swing.JMenuItem preferencesMenuItem;
    private javax.swing.JMenuItem processingParametersMenuItem;
    private javax.swing.JMenuItem projectPropertiesMenuItem;
    private javax.swing.JPanel proteinFractionsJPanel;
    private javax.swing.JPanel proteinStructureJPanel;
    private javax.swing.JCheckBoxMenuItem proteinsJCheckBoxMenuItem;
    private javax.swing.JPanel ptmJPanel;
    private javax.swing.JPanel qcJPanel;
    private javax.swing.JMenuItem quantifyMenuItem;
    private javax.swing.JCheckBoxMenuItem reporterIonsCheckMenu;
    private javax.swing.JMenuItem reporterPreferencesJMenuItem;
    private javax.swing.JMenuItem reshakeMenuItem;
    private javax.swing.JCheckBoxMenuItem rewindIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem scoresJCheckBoxMenuItem;
    private javax.swing.JMenuItem searchGuiPreferencesJMenuItem;
    private javax.swing.JMenuItem searchParametersMenu;
    private javax.swing.JCheckBoxMenuItem sequenceCoverageJCheckBoxMenuItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JCheckBoxMenuItem sparklinesJCheckBoxMenuItem;
    private javax.swing.JMenuItem speciesJMenuItem;
    private javax.swing.JCheckBoxMenuItem spectrumJCheckBoxMenuItem;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JCheckBoxMenuItem spectrumSlidersCheckBoxMenuItem;
    private javax.swing.JMenu splitterMenu2;
    private javax.swing.JMenu splitterMenu3;
    private javax.swing.JMenu splitterMenu4;
    private javax.swing.JMenu splitterMenu5;
    private javax.swing.JMenu splitterMenu6;
    private javax.swing.JMenu splitterMenu7;
    private javax.swing.JMenu splitterMenu8;
    private javax.swing.JMenu splitterMenu9;
    private javax.swing.JMenuItem starHideJMenuItem;
    private javax.swing.JPanel statsJPanel;
    private javax.swing.JButton tipsButton;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JCheckBoxMenuItem validatedProteinsOnlyJCheckBoxMenuItem;
    private javax.swing.JMenu viewJMenu;
    private javax.swing.JCheckBoxMenuItem xIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem yIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem zIonCheckBoxMenuItem;
    // End of variables declaration//GEN-END:variables

    /**
     * Set up the log file. Redirects the error and output streams to the log
     * file.
     *
     * @param redirectOutputStream if true, redirects the output stream
     */
    public void setUpLogFile(boolean redirectOutputStream) {

        if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = getJarFilePath() + "/resources/PeptideShaker.log";

                File file = new File(path);
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                if (redirectOutputStream) {
                    System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                }

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    boolean fileCreated = file.createNewFile();

                    if (fileCreated) {
                        FileWriter w = new FileWriter(file);
                        BufferedWriter bw = new BufferedWriter(w);
                        bw.close();
                        w.close();
                    } else {
                        JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                                "Failed to create the file log file.<br>"
                                + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                                "File Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                System.err.println(System.getProperty("line.separator") + System.getProperty("line.separator") + new Date()
                        + ": PeptideShaker version " + getVersion() + "." + System.getProperty("line.separator"));
                System.err.println("Total amount of memory in the Java virtual machine: " + Runtime.getRuntime().totalMemory() + "." + System.getProperty("line.separator"));
                System.err.println("Java version: " + System.getProperty("java.version") + "." + System.getProperty("line.separator"));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occured when trying to create the PeptideShaker log file.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the gene preferences.
     *
     * @return the gene preferences
     */
    public GenePreferences getGenePreferences() {
        return cpsBean.getGenePreferences();
    }

    /**
     * Returns the user preferences.
     *
     * @return the user preferences
     */
    public UserPreferences getUserPreferences() {
        return cpsBean.getUserPreferences();
    }

    /**
     * Returns the user preferences.
     *
     * @return the user preferences
     */
    public UtilitiesUserPreferences getUtilitiesUserPreferences() {
        return utilitiesUserPreferences;
    }

    /**
     * Set the utilities user preferences.
     *
     * @param utilitiesUserPreferences
     */
    public void setUtilitiesUserPreferences(UtilitiesUserPreferences utilitiesUserPreferences) {
        this.utilitiesUserPreferences = utilitiesUserPreferences;
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("PeptideShakerGUI.class").getPath(), "PeptideShaker");
    }

    /**
     * Set the default preferences.
     */
    public void setDefaultPreferences() {
        cpsBean.setDefaultPreferences();
        updateAnnotationPreferencesFromSearchSettings();
        IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.NH3);
        IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.H2O);
    }

    /**
     * Updates the ions used for fragment annotation.
     */
    public void updateAnnotationPreferencesFromSearchSettings() {

        SearchParameters searchParameters = getSearchParameters();
        AnnotationPreferences annotationPreferences = new AnnotationPreferences();
        annotationPreferences.setPreferencesFromSearchParamaers(searchParameters);
        setAnnotationPreferences(annotationPreferences);

        if (searchParameters.getIonSearched1() == PeptideFragmentIon.A_ION) {
            forwardIonsDeNovoCheckBoxMenuItem.setText("a-ions");
        } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.B_ION) {
            forwardIonsDeNovoCheckBoxMenuItem.setText("b-ions");
        } else if (searchParameters.getIonSearched1() == PeptideFragmentIon.C_ION) {
            forwardIonsDeNovoCheckBoxMenuItem.setText("c-ions");
        }

        forwardIonsDeNovoCheckBoxMenuItem.repaint();

        if (searchParameters.getIonSearched2() == PeptideFragmentIon.X_ION) {
            rewindIonsDeNovoCheckBoxMenuItem.setText("x-ions");
        } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.Y_ION) {
            rewindIonsDeNovoCheckBoxMenuItem.setText("y-ions");
        } else if (searchParameters.getIonSearched2() == PeptideFragmentIon.Z_ION) {
            rewindIonsDeNovoCheckBoxMenuItem.setText("z-ions");
        }

        rewindIonsDeNovoCheckBoxMenuItem.repaint();

        if (getAnnotationPreferences().getDeNovoCharge() == 1) {
            deNovoChargeOneJRadioButtonMenuItem.isSelected();
        } else {
            deNovoChargeTwoJRadioButtonMenuItem.isSelected();
        }
    }

    /**
     * Returns the reporter ions possibly found in this project.
     *
     * @return the reporter ions possibly found in this project
     */
    public ArrayList<Integer> getReporterIons() {

        ArrayList<String> modifications = getSearchParameters().getModificationProfile().getAllModifications();
        ArrayList<Integer> reporterIonsSubtypes = new ArrayList<Integer>();

        for (String mod : modifications) {
            PTM ptm = ptmFactory.getPTM(mod);
            for (ReporterIon reporterIon : ptm.getReporterIons()) {
                int subType = reporterIon.getSubType();
                if (!reporterIonsSubtypes.contains(subType)) {
                    reporterIonsSubtypes.add(subType);
                }
            }
        }

        return reporterIonsSubtypes;
    }

    /**
     * Returns the spectrum annotator.
     *
     * @return the spectrum annotator
     */
    public PeptideSpectrumAnnotator getSpectrumAnnorator() {
        return spectrumAnnotator;
    }

    /**
     * Updates the annotations in the selected tab.
     */
    public void updateSpectrumAnnotations() {

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            overviewPanel.setIntensitySliderValue((int) (getAnnotationPreferences().getAnnotationIntensityLimit() * 100));
            overviewPanel.setAccuracySliderValue((int) ((getAnnotationPreferences().getFragmentIonAccuracy() / getSearchParameters().getFragmentIonAccuracy()) * 100));
            overviewPanel.updateSpectrum();
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            spectrumIdentificationPanel.setIntensitySliderValue((int) (getAnnotationPreferences().getAnnotationIntensityLimit() * 100));
            spectrumIdentificationPanel.setAccuracySliderValue((int) ((getAnnotationPreferences().getFragmentIonAccuracy() / getSearchParameters().getFragmentIonAccuracy()) * 100));
            spectrumIdentificationPanel.updateSpectrum();
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            ptmPanel.setIntensitySliderValue((int) (getAnnotationPreferences().getAnnotationIntensityLimit() * 100));
            ptmPanel.setAccuracySliderValue((int) ((getAnnotationPreferences().getFragmentIonAccuracy() / getSearchParameters().getFragmentIonAccuracy()) * 100));
            ptmPanel.updateGraphics(null);
        }
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
     * Loads the modification profile from the given file.
     *
     * @param aFile the given file
     */
    private void loadModificationProfile(File aFile) {
        try {
            FileInputStream fis = new FileInputStream(aFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream in = new ObjectInputStream(bis);
            ModificationProfile modificationProfile = (ModificationProfile) in.readObject();
            in.close();
            bis.close();
            fis.close();
            getSearchParameters().setModificationProfile(modificationProfile);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, aFile.getName() + " not found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading:\n" + aFile.getName() + ".\n\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading:\n" + aFile.getName() + ".\n\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
        } catch (ClassCastException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading:\n" + aFile.getName() + ".\n\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return cpsBean.getExperiment();
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        return cpsBean.getSample();
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicateNumber
     */
    public int getReplicateNumber() {
        return cpsBean.getReplicateNumber();
    }

    /**
     * Returns the identification displayed.
     *
     * @return the identification displayed
     */
    public Identification getIdentification() {
        return cpsBean.getIdentification();
    }

    /**
     * Returns the desired spectrum.
     *
     * @param spectrumKey the key of the spectrum
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
     * Returns the precursor of a given spectrum.
     *
     * @param spectrumKey the key of the given spectrum
     * @return the precursor
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws IllegalArgumentException
     */
    public Precursor getPrecursor(String spectrumKey) throws IOException, MzMLUnmarshallerException, IllegalArgumentException {
        return getPrecursor(spectrumKey, false);
    }

    /**
     * Returns the precursor of a given spectrum.
     *
     * @param spectrumKey the key of the given spectrum
     * @param save boolean indicating whether the precursor should be saved in
     * memory for later re-use
     * @return the precursor
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws IllegalArgumentException
     */
    public Precursor getPrecursor(String spectrumKey, boolean save) throws IOException, MzMLUnmarshallerException, IllegalArgumentException {
        return spectrumFactory.getPrecursor(spectrumKey, save);
    }

    /**
     * Returns the annotation preferences as set by the user.
     *
     * @return the annotation preferences as set by the user
     */
    public AnnotationPreferences getAnnotationPreferences() {
        return cpsBean.getAnnotationPreferences();
    }

    /**
     * Return the filter preferences to use.
     *
     * @return the filter preferences to use
     */
    public FilterPreferences getFilterPreferences() {
        return cpsBean.getFilterPreferences();
    }

    /**
     * Return the display preferences to use.
     *
     * @return the display preferences to use
     */
    public DisplayPreferences getDisplayPreferences() {
        return cpsBean.getDisplayPreferences();
    }

    /**
     * Sets the GUI filter preferences to use.
     *
     * @param filterPreferences the GUI filter preferences to use
     */
    public void setFilterPreferences(FilterPreferences filterPreferences) {
        cpsBean.setFilterPreferences(filterPreferences);
    }

    /**
     * Sets the display preferences to use.
     *
     * @param displayPreferences the display preferences to use
     */
    public void setDisplayPreferences(DisplayPreferences displayPreferences) {
        cpsBean.setDisplayPreferences(displayPreferences);
    }

    /**
     * Returns the spectrum counting preferences.
     *
     * @return the spectrum counting preferences
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return cpsBean.getSpectrumCountingPreferences();
    }

    /**
     * Sets new spectrum counting preferences.
     *
     * @param spectrumCountingPreferences new spectrum counting preferences
     */
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        cpsBean.setSpectrumCountingPreferences(spectrumCountingPreferences);
    }

    /**
     * Returns the PTM scoring preferences
     *
     * @return the PTM scoring preferences
     */
    public PTMScoringPreferences getPtmScoringPreferences() {
        return cpsBean.getPtmScoringPreferences();
    }

    /**
     * Sets the PTM scoring preferences
     *
     * @param ptmScoringPreferences the PTM scoring preferences
     */
    public void setPtmScoringPreferences(PTMScoringPreferences ptmScoringPreferences) {
        cpsBean.setPtmScoringPreferences(ptmScoringPreferences);
    }

    /**
     * Returns the displayed proteomicAnalysis.
     *
     * @return the displayed proteomicAnalysis
     */
    public ProteomicAnalysis getProteomicanalysis() {
        return cpsBean.getProteomicAnalysis();
    }

    /**
     * Returns the search parameters.
     *
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return cpsBean.getSearchParameters();
    }

    /**
     * Updates the search parameters.
     *
     * @param searchParameters the new search parameters
     */
    public void setSearchParameters(SearchParameters searchParameters) {

        // update only if the new search settings are different from the old ones
        if (!searchParameters.equals(cpsBean.getSearchParameters())) {
            cpsBean.setSearchParameters(searchParameters);
            PeptideShaker.loadModifications(getSearchParameters());

            updateAnnotationPreferencesFromSearchSettings();
            setSelectedItems();
            backgroundPanel.revalidate();
            backgroundPanel.repaint();
            dataSaved = false;
        }
    }

    /**
     * Returns the initial processing preferences.
     *
     * @return the initial processing preferences
     */
    public ProcessingPreferences getProcessingPreferences() {
        if (cpsBean.getProcessingPreferences() == null) {
            cpsBean.setProcessingPreferences(new ProcessingPreferences());
        }
        return cpsBean.getProcessingPreferences();
    }

    /**
     * Sets the initial processing preferences.
     *
     * @param processingPreferences the initial processing preferences
     */
    public void setProcessingPreferences(ProcessingPreferences processingPreferences) {
        cpsBean.setProcessingPreferences(new ProcessingPreferences());
    }

    /**
     * Updates the annotation preferences.
     *
     * @param annotationPreferences the new annotation preferences
     */
    public void setAnnotationPreferences(AnnotationPreferences annotationPreferences) {
        cpsBean.setAnnotationPreferences(annotationPreferences);
    }

    /**
     * Updates the gene preferences.
     *
     * @param genePreferences the new gene preferences
     */
    public void setGenePreferences(GenePreferences genePreferences) {
        cpsBean.setGenePreferences(genePreferences);
        loadGeneMappings();
    }

    /**
     * Loads the modifications from the modification file.
     */
    public void resetPtmFactory() {

        // reset ptm factory
        ptmFactory.reloadFactory();
        ptmFactory = PTMFactory.getInstance();

        try {
            ptmFactory.importModifications(new File(getJarFilePath(), PeptideShaker.MODIFICATIONS_FILE), false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error (" + e.getMessage() + ")\n"
                    + "occured when trying to load the modifications from " + new File(getJarFilePath(), PeptideShaker.MODIFICATIONS_FILE) + ".",
                    "Configuration Import Error", JOptionPane.ERROR_MESSAGE);
        }

        try {
            ptmFactory.importModifications(new File(getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE), true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error (" + e.getMessage() + ")\n"
                    + "occured when trying to load the modifications from " + new File(getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE) + ".",
                    "Configuration Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get the sparklines color.
     *
     * @return the sparklineColor
     */
    public Color getSparklineColor() {
        return utilitiesUserPreferences.getSparklineColor();
    }

    /**
     * Set the sparklines color.
     *
     * @param sparklineColor the sparklineColor to set
     */
    public void setSparklineColor(Color sparklineColor) {
        utilitiesUserPreferences.setSparklineColor(sparklineColor);
    }

    /**
     * Get the non-validated sparklines color.
     *
     * @return the non-validated sparklineColor
     */
    public Color getSparklineColorNonValidated() {
        return utilitiesUserPreferences.getSparklineColorNonValidated();
    }

    /**
     * Set the non-validated sparklines color.
     *
     * @param sparklineColorNonValidated the non-validated sparklineColor to set
     */
    public void setSparklineColorNonValidated(Color sparklineColorNonValidated) {
        utilitiesUserPreferences.setSparklineColorNonValidated(sparklineColorNonValidated);
    }

    /**
     * Get the possible sparklines color.
     *
     * @return the possible sparklineColor
     */
    public Color getSparklineColorPossible() {
        return utilitiesUserPreferences.getSparklineColorPossible();
    }

    /**
     * Set the possible sparklines color.
     *
     * @param sparklineColorPossible the possible sparklineColor to set
     */
    public void setSparklineColorPossible(Color sparklineColorPossible) {
        utilitiesUserPreferences.setSparklineColorPossible(sparklineColorPossible);
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
     * Sets the keys of the selected protein, peptide and PSM.
     *
     * @param proteinKey the key of the selected protein
     * @param peptideKey the key of the selected peptide
     * @param psmKey the key of the selected PSM
     */
    public void setSelectedItems(String proteinKey, String peptideKey, String psmKey) {
        this.selectedProteinKey = proteinKey;
        this.selectedPeptideKey = peptideKey;
        this.selectedPsmKey = psmKey;
    }

    /**
     * Updates the selected items in the currently opened tab.
     */
    public void updateSelectionInCurrentTab() {

        int selectedIndex = allTabsJTabbedPane.getSelectedIndex();
        if (selectedIndex == OVER_VIEW_TAB_INDEX) {
            overviewPanel.updateSelection(true);
        } else if (selectedIndex == STRUCTURES_TAB_INDEX) {
            proteinStructurePanel.updateSelection(true);
        } else if (selectedIndex == SPECTRUM_ID_TAB_INDEX) {
            spectrumIdentificationPanel.updateSelection();
        } else if (selectedIndex == MODIFICATIONS_TAB_INDEX) {
            ptmPanel.updateSelection();
        } else if (selectedIndex == PROTEIN_FRACTIONS_TAB_INDEX) {
            proteinFractionsPanel.updateSelection();
        }
    }

    /**
     * Resets the items selection.
     */
    public void resetSelectedItems() {
        setSelectedItems(NO_SELECTION, NO_SELECTION, NO_SELECTION);
    }

    /**
     * Sets the selected item based on the selected tab.
     */
    public void setSelectedItems() {
        int selectedIndex = allTabsJTabbedPane.getSelectedIndex();
        if (selectedIndex == OVER_VIEW_TAB_INDEX) {
            overviewPanel.newItemSelection();
        } else if (selectedIndex == MODIFICATIONS_TAB_INDEX) {
            ptmPanel.newItemSelection();
        } else if (selectedIndex == STRUCTURES_TAB_INDEX) {
            proteinStructurePanel.newItemSelection();
        } else if (selectedIndex == PROTEIN_FRACTIONS_TAB_INDEX) {
            proteinFractionsPanel.newItemSelection();
        }
    }

    /**
     * Returns the key of the selected protein.
     *
     * @return the key of the selected protein
     */
    public String getSelectedProteinKey() {
        return selectedProteinKey;
    }

    /**
     * Returns the key of the selected peptide.
     *
     * @return the key of the selected peptide
     */
    public String getSelectedPeptideKey() {
        return selectedPeptideKey;
    }

    /**
     * Returns the currently selected spectrum key.
     *
     * @return the key for the selected spectrum
     */
    public String getSelectedPsmKey() {
        return selectedPsmKey;
    }

    /**
     * Clear the data from the previous experiment.
     *
     * @param clearDatabaseFolder decides if the database folder is to be
     * cleared or not
     * @param clearGeneAndGoFactories decides if the gene and GO factories are
     * to be cleared or not
     */
    public void clearData(boolean clearDatabaseFolder, boolean clearGeneAndGoFactories) {

        // reset the preferences
        selectedProteinKey = NO_SELECTION;
        selectedPeptideKey = NO_SELECTION;
        selectedPsmKey = NO_SELECTION;

        cpsBean.setProjectDetails(null);
        spectrumAnnotator = new PeptideSpectrumAnnotator();

        try {
            spectrumFactory.closeFiles();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }
        try {
            sequenceFactory.closeFile();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        if (clearGeneAndGoFactories) {
            try {
                goFactory.closeFiles();
            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            }
            try {
                geneFactory.closeFiles();
            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            }
        }

        try {
            spectrumFactory.clearFactory();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }
        try {
            sequenceFactory.clearFactory();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        if (clearGeneAndGoFactories) {
            try {
                goFactory.clearFactory();
            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            }
            try {
                geneFactory.clearFactory();
            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            }
        }

        identifiedModifications = null;

        if (clearDatabaseFolder) {
            clearDatabaseFolder();
        }

        resetIdentificationFeaturesGenerator();

        // set up the tabs/panels
        scoresJCheckBoxMenuItem.setSelected(false);
        setUpPanels(true);

        // repaint the panels
        repaintPanels();

        // select the overview tab
        allTabsJTabbedPane.setSelectedIndex(OVER_VIEW_TAB_INDEX);
        cpsBean.setCpsFile(null);
        dataSaved = false;
    }

    /**
     * Clears the database folder.
     */
    private void clearDatabaseFolder() {

        boolean databaseClosed = true;

        // closeFiles the database connection
        if (getIdentification() != null) {

            try {
                getIdentification().close();
                cpsBean.setIdentification(null);
            } catch (SQLException e) {
                databaseClosed = false;
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to close the database.", "Database Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        // empty the matches folder
        if (databaseClosed) {
            File matchFolder = new File(getJarFilePath(), PeptideShaker.SERIALIZATION_DIRECTORY);

            if (matchFolder.exists()) {

                File[] tempFiles = matchFolder.listFiles();

                if (tempFiles != null) {
                    for (File currentFile : tempFiles) {
                        Util.deleteDir(currentFile);
                    }
                }

                if (matchFolder.listFiles() != null && matchFolder.listFiles().length > 0) {
                    JOptionPane.showMessageDialog(null, "Failed to empty the database folder:\n" + matchFolder.getPath() + ".",
                            "Database Cleanup Failed", JOptionPane.WARNING_MESSAGE);
                }

            }
        }
    }

    /**
     * Clears the preferences.
     */
    public void clearPreferences() {

        cpsBean.clearPreferences();

        // reset enzymes, ptms and preferences
        loadEnzymes();
        resetPtmFactory();
        setDefaultPreferences();
        loadGeneMappings();
    }

    /**
     * Resets the content of a panel indexed by the given integer.
     *
     * @param tabIndex index of the panel to reset
     */
    private void resetPanel(int tabIndex) {
        switch (tabIndex) {
            case OVER_VIEW_TAB_INDEX:
                overviewPanel.clearData();
                return;
            case MODIFICATIONS_TAB_INDEX:
                ptmPanel = new PtmPanel(this);
                ptmJPanel.removeAll();
                ptmJPanel.add(ptmPanel);
                return;
            case SPECTRUM_ID_TAB_INDEX:
                spectrumIdentificationPanel = new SpectrumIdentificationPanel(this);
                spectrumJPanel.removeAll();
                spectrumJPanel.add(spectrumIdentificationPanel);
                return;
            case PROTEIN_FRACTIONS_TAB_INDEX:
                proteinFractionsPanel = new ProteinFractionsPanel(this);
                proteinFractionsJPanel.removeAll();
                proteinFractionsJPanel.add(proteinFractionsPanel);
                return;
            case STRUCTURES_TAB_INDEX:
                proteinStructurePanel.clearData();
                return;
            case ANNOTATION_TAB_INDEX:
                annotationPanel = new AnnotationPanel(this);
                annotationsJPanel.removeAll();
                annotationsJPanel.add(annotationPanel);
                return;
            case QC_PLOTS_TAB_INDEX:
                qcPanel = new QCPanel(this);
                qcJPanel.removeAll();
                qcJPanel.add(qcPanel);
                return;
            case GO_ANALYSIS_TAB_INDEX:
                goPanel = new GOEAPanel(this);
                goJPanel.removeAll();
                goJPanel.add(goPanel);
                return;
            case VALIDATION_TAB_INDEX:
                statsPanel = new StatsPanel(this);
                statsJPanel.removeAll();
                statsJPanel.add(statsPanel);
        }
    }

    /**
     * Set up the different tabs/panels.
     */
    private void setUpPanels(boolean setupValidationTab) {

        updateNeeded = new HashMap<Integer, Boolean>();
        for (int tabIndex = 0; tabIndex < allTabsJTabbedPane.getTabCount(); tabIndex++) {
            if (tabIndex == VALIDATION_TAB_INDEX) {
                if (setupValidationTab) {
                    updateNeeded.put(tabIndex, true);
                    resetPanel(tabIndex);
                } else {
                    updateNeeded.put(tabIndex, false);
                }
            } else {
                updateNeeded.put(tabIndex, true);
                resetPanel(tabIndex);
            }
        }

        // hide/show the score columns
        //scoresJCheckBoxMenuItemActionPerformed(null);
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

        proteinFractionsJPanel.revalidate();
        proteinFractionsJPanel.repaint();

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
     * Returns the ProteinFractionsPanel.
     *
     * @return the ProteinFractionsPanel
     */
    public ProteinFractionsPanel getProteinFractionsPanel() {
        return proteinFractionsPanel;
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
     * Returns the GO Panel.
     *
     * @return the GO Panel
     */
    public GOEAPanel getGOPanel() {
        return goPanel;
    }

    /**
     * Gets the preferred width of the column specified by colIndex. The column
     * will be just wide enough to show the column head and the widest cell in
     * the column. Margin pixels are added to the left and right (resulting in
     * an additional width of 2*margin pixels. Returns null if the max width
     * cannot be set.
     *
     * @param table the table
     * @param colIndex the colum index
     * @param margin the margin to add
     * @return the preferred width of the column
     */
    public Integer getPreferredAccessionColumnWidth(JTable table, int colIndex, int margin) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(colIndex);

        // get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        int width = comp.getPreferredSize().width;

        // get maximum width of column data
        if (getMetrics() == null || getMetrics().getMaxProteinKeyLength() > (table.getColumnName(colIndex).length() + margin)) {
            return null;
        }

        // add margin
        width += 2 * margin;

        return width;
    }

    /**
     * Gets the preferred width of the column specified by colIndex. The column
     * will be just wide enough to show the column head and the widest cell in
     * the column. Margin pixels are added to the left and right (resulting in
     * an additional width of 2*margin pixels. <br> Note that this method
     * iterates all rows in the table to get the perfect width of the column!
     *
     * @param table the table
     * @param colIndex the colum index
     * @param margin the margin to add
     * @return the prefereed width of the column
     */
    public int getPreferredColumnWidth(JTable table, int colIndex, int margin) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(colIndex);

        // get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        int width = comp.getPreferredSize().width;

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
     * @param enable if true the spectrum export in the overview panel will be
     * enabled
     */
    public void enableSpectrumExport(boolean enable) {
        if (exportSpectrumGraphicsJMenuItem.isVisible()) {
            exportSpectrumGraphicsJMenuItem.setEnabled(enable);
        }
    }

    /**
     * Update the protein match in the different tabs.
     *
     * @param mainMatch the protein match to use
     * @param proteinInferenceType the protein inference group type
     */
    public void updateMainMatch(String mainMatch, int proteinInferenceType) {
        try {
            PeptideShaker miniShaker = new PeptideShaker(getExperiment(), getSample(), getReplicateNumber());
            miniShaker.scorePTMs(getIdentification().getProteinMatch(selectedProteinKey), getSearchParameters(), getAnnotationPreferences(), false, getPtmScoringPreferences());
        } catch (Exception e) {
            catchException(e);
        }

        overviewPanel.updateProteinTable();
        proteinStructurePanel.updateMainMatch(mainMatch, proteinInferenceType);
    }

    /**
     * Set whether the current data has been saved to a cps file or not.
     *
     * @param dataSaved whether the current data has been saved to a cps file or
     * not
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
            case PROTEIN_FRACTIONS_TAB_INDEX:
                return proteinFractionsPanel.getDisplayedProteins();
            case MODIFICATIONS_TAB_INDEX:
                return ptmPanel.getDisplayedProteinMatches();
            default:
                return null;
        }
    }

    /**
     * Returns a list of keys of the currently displayed peptides.
     *
     * @return a list of keys of the currently displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        int selectedTab = getSelectedTab();
        switch (selectedTab) {
            case OVER_VIEW_TAB_INDEX:
                return overviewPanel.getDisplayedPeptides();
            case STRUCTURES_TAB_INDEX:
                return proteinStructurePanel.getDisplayedPeptides();
            case PROTEIN_FRACTIONS_TAB_INDEX:
                return proteinFractionsPanel.getDisplayedPeptides();
            case MODIFICATIONS_TAB_INDEX:
                return ptmPanel.getDisplayedPeptides();
            default:
                return null;
        }
    }

    /**
     * Returns a list of keys of the currently displayed psms.
     *
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
     * Returns a list of keys of the currently displayed assumptions.
     *
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
            for (String link : allLinks) {
                link = link.substring(0, link.indexOf("\""));
                BareBonesBrowserLaunch.openURL(link);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Returns true if the relative error (ppm) is used instead of the absolute
     * error (Da).
     *
     * @return true if the relative error (ppm) is used instead of the absolute
     * error (Da)
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
     * Method called whenever an exception is caught.
     *
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }

    /**
     * Returns the exception handler.
     *
     * @return the exception handler
     */
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Closes the frame by first checking if the project ought to be saved.
     */
    public void close() {

        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        if (!dataSaved && getExperiment() != null) {

            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getExperiment().getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                if (cpsBean.getCpsFile() != null && cpsBean.getCpsFile().exists()) {
                    saveProject(true);
                } else {
                    saveProjectAs(true);
                }
            } else if (value == JOptionPane.NO_OPTION) {
                closePeptideShaker();
            }
        } else {
            closePeptideShaker();
        }
    }

    /**
     * Closes PeptideShaker.
     */
    private void closePeptideShaker() {

        exceptionHandler.setIgnoreExceptions(true);

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);
        progressDialog.setTitle("Closing. Please Wait...");

        final PeptideShakerGUI finalRef = this;

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // turn off the self updating table models
                    overviewPanel.deactivateSelfUpdatingTableModels();
                    proteinFractionsPanel.deactivateSelfUpdatingTableModels();
                    proteinStructurePanel.deactivateSelfUpdatingTableModels();

                    // close the files and save the user preferences
                    if (!progressDialog.isRunCanceled()) {
                        spectrumFactory.closeFiles();
                        sequenceFactory.closeFile();
                        GOFactory.getInstance().closeFiles();
                        cpsBean.saveUserPreferences();
                        saveModificationUsage();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    catchException(e);
                } finally {
                    progressDialog.setRunFinished();

                    // hide the gui
                    finalRef.setVisible(false);

                    // clear the data and database folder
                    clearData(true, true);

                    // closeFiles the jvm
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Closes and restarts PeptideShaker. Does not work inside the IDE of
     * course.
     */
    public void restart() {

        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        if (!dataSaved && getExperiment() != null) {

            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getExperiment().getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
            } else if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);
        progressDialog.setTitle("Closing. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("RestartThread") {
            @Override
            public void run() {
                try {
                    spectrumFactory.closeFiles();
                    sequenceFactory.closeFile();
                    goFactory.closeFiles();
                    cpsBean.saveUserPreferences();
                    PeptideShakerGUI.this.clearData(true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    catchException(e);
                }
                progressDialog.setRunFinished();
                saveModificationUsage();
                PeptideShakerGUI.this.dispose();

                // @TODO: pass the current project to the new instance of PeptideShaker.
                new PeptideShakerWrapper();
                System.exit(0); // have to closeFiles the current java process (as a new one is started on the line above)
            }
        }.start();
    }

    /**
     * Update the annotation menu bar with the current annotation preferences.
     *
     * @param precursorCharge
     * @param peptide
     */
    public void updateAnnotationMenus(int precursorCharge, Peptide peptide) {

        aIonCheckBoxMenuItem.setSelected(false);
        bIonCheckBoxMenuItem.setSelected(false);
        cIonCheckBoxMenuItem.setSelected(false);
        xIonCheckBoxMenuItem.setSelected(false);
        yIonCheckBoxMenuItem.setSelected(false);
        zIonCheckBoxMenuItem.setSelected(false);
        precursorCheckMenu.setSelected(false);
        immoniumIonsCheckMenu.setSelected(false);
        reporterIonsCheckMenu.setSelected(false);

        for (Ion.IonType ionType : getAnnotationPreferences().getIonTypes().keySet()) {
            if (ionType == IonType.IMMONIUM_ION) {
                immoniumIonsCheckMenu.setSelected(true);
            } else if (ionType == IonType.PRECURSOR_ION) {
                precursorCheckMenu.setSelected(true);
            } else if (ionType == IonType.REPORTER_ION) {
                reporterIonsCheckMenu.setSelected(true);
            } else if (ionType == IonType.PEPTIDE_FRAGMENT_ION) {
                for (int subtype : getAnnotationPreferences().getIonTypes().get(ionType)) {
                    if (subtype == PeptideFragmentIon.A_ION) {
                        aIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.B_ION) {
                        bIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.C_ION) {
                        cIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.X_ION) {
                        xIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.Y_ION) {
                        yIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.Z_ION) {
                        zIonCheckBoxMenuItem.setSelected(true);
                    }
                }
            }
        }

        boolean selected;

        ArrayList<String> selectedLosses = new ArrayList<String>();

        for (JCheckBoxMenuItem lossMenuItem : lossMenus.values()) {

            if (lossMenuItem.isSelected()) {
                selectedLosses.add(lossMenuItem.getText());
            }

            lossMenu.remove(lossMenuItem);
        }

        lossMenu.setVisible(true);
        lossSplitter.setVisible(true);
        lossMenus.clear();

        HashMap<String, NeutralLoss> neutralLosses = new HashMap<String, NeutralLoss>();

        // add the general neutral losses
        for (NeutralLoss neutralLoss : IonFactory.getInstance().getDefaultNeutralLosses()) {
            neutralLosses.put(neutralLoss.name, neutralLoss);
        }

        // add the sequence specific neutral losses
        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            PTM ptm = ptmFactory.getPTM(modMatch.getTheoreticPtm());
            for (NeutralLoss neutralLoss : ptm.getNeutralLosses()) {
                neutralLosses.put(neutralLoss.name, neutralLoss);
            }
        }

        ArrayList<String> names = new ArrayList<String>(neutralLosses.keySet());
        Collections.sort(names);

        ArrayList<String> finalSelectedLosses = selectedLosses;

        if (names.isEmpty()) {
            lossMenu.setVisible(false);
            lossSplitter.setVisible(false);
        } else {
            for (int i = 0; i < names.size(); i++) {

                if (getAnnotationPreferences().areNeutralLossesSequenceDependant()) {
                    selected = false;
                    for (NeutralLoss neutralLoss : getAnnotationPreferences().getNeutralLosses().getAccountedNeutralLosses()) {
                        if (neutralLoss.isSameAs(neutralLoss)) {
                            selected = true;
                            break;
                        }
                    }
                } else {
                    selected = finalSelectedLosses.contains(names.get(i));
                }

                JCheckBoxMenuItem lossMenuItem = new JCheckBoxMenuItem(names.get(i));
                lossMenuItem.setSelected(selected);
                lossMenuItem.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        getAnnotationPreferences().useAutomaticAnnotation(false);
                        getAnnotationPreferences().setNeutralLossesSequenceDependant(false);
                        updateAnnotationPreferences();
                    }
                });
                lossMenus.put(neutralLosses.get(names.get(i)), lossMenuItem);
                lossMenu.add(lossMenuItem, i);
            }
        }

        ArrayList<String> selectedCharges = new ArrayList<String>();

        for (JCheckBoxMenuItem chargeMenuItem : chargeMenus.values()) {

            if (chargeMenuItem.isSelected()) {
                selectedCharges.add(chargeMenuItem.getText());
            }

            chargeMenu.remove(chargeMenuItem);
        }

        chargeMenus.clear();

        if (precursorCharge == 1) {
            precursorCharge = 2;
        }

        final ArrayList<String> finalSelectedCharges = selectedCharges;

        for (int charge = 1; charge < precursorCharge; charge++) {

            JCheckBoxMenuItem chargeMenuItem = new JCheckBoxMenuItem(charge + "+");

            if (getAnnotationPreferences().useAutomaticAnnotation()) {
                chargeMenuItem.setSelected(getAnnotationPreferences().getValidatedCharges().contains(charge));
            } else {
                if (finalSelectedCharges.contains(charge + "+")) {
                    chargeMenuItem.setSelected(true);
                } else {
                    chargeMenuItem.setSelected(false);
                }
            }

            chargeMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    getAnnotationPreferences().useAutomaticAnnotation(false);
                    updateAnnotationPreferences();
                }
            });

            chargeMenus.put(charge, chargeMenuItem);
            chargeMenu.add(chargeMenuItem);
        }

        automaticAnnotationCheckBoxMenuItem.setSelected(getAnnotationPreferences().useAutomaticAnnotation());
        adaptCheckBoxMenuItem.setSelected(getAnnotationPreferences().areNeutralLossesSequenceDependant());

        // disable/enable the neutral loss options
        for (JCheckBoxMenuItem lossMenuItem : lossMenus.values()) {
            lossMenuItem.setEnabled(!getAnnotationPreferences().areNeutralLossesSequenceDependant());
        }

        allCheckBoxMenuItem.setSelected(getAnnotationPreferences().showAllPeaks());

        barsCheckBoxMenuItem.setSelected(getAnnotationPreferences().showBars());
        intensityIonTableRadioButtonMenuItem.setSelected(getAnnotationPreferences().useIntensityIonTable());
    }

    /**
     * Save the current annotation preferences selected in the annotation menus.
     */
    public void updateAnnotationPreferences() {

        getAnnotationPreferences().clearIonTypes();
        if (aIonCheckBoxMenuItem.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
        }
        if (bIonCheckBoxMenuItem.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
        }
        if (cIonCheckBoxMenuItem.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
        }
        if (xIonCheckBoxMenuItem.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
        }
        if (yIonCheckBoxMenuItem.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
        }
        if (zIonCheckBoxMenuItem.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
        }
        if (precursorCheckMenu.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.PRECURSOR_ION);
        }
        if (immoniumIonsCheckMenu.isSelected()) {
            getAnnotationPreferences().addIonType(IonType.IMMONIUM_ION);
        }
        if (reporterIonsCheckMenu.isSelected()) {
            for (int subtype : getReporterIons()) {
                getAnnotationPreferences().addIonType(IonType.REPORTER_ION, subtype);
            }
        }

        getAnnotationPreferences().clearNeutralLosses();

        for (NeutralLoss neutralLoss : lossMenus.keySet()) {
            if (lossMenus.get(neutralLoss).isSelected()) {
                getAnnotationPreferences().addNeutralLoss(neutralLoss);
            }
        }

        getAnnotationPreferences().clearCharges();

        for (int charge : chargeMenus.keySet()) {
            if (chargeMenus.get(charge).isSelected()) {
                getAnnotationPreferences().addSelectedCharge(charge);
            }
        }

        getAnnotationPreferences().useAutomaticAnnotation(automaticAnnotationCheckBoxMenuItem.isSelected());
        getAnnotationPreferences().setNeutralLossesSequenceDependant(adaptCheckBoxMenuItem.isSelected());

        getAnnotationPreferences().setShowAllPeaks(allCheckBoxMenuItem.isSelected());
        getAnnotationPreferences().setShowBars(barsCheckBoxMenuItem.isSelected());
        getAnnotationPreferences().setIntensityIonTable(intensityIonTableRadioButtonMenuItem.isSelected());

        getAnnotationPreferences().setShowForwardIonDeNovoTags(forwardIonsDeNovoCheckBoxMenuItem.isSelected());
        getAnnotationPreferences().setShowRewindIonDeNovoTags(rewindIonsDeNovoCheckBoxMenuItem.isSelected());

        if (deNovoChargeOneJRadioButtonMenuItem.isSelected()) {
            getAnnotationPreferences().setDeNovoCharge(1);
        } else {
            getAnnotationPreferences().setDeNovoCharge(2);
        }

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
     * Updates the visible menu items on the settings menu of the annotation
     * menu bar.
     *
     * @param showSpectrumOptions if true, the spectrum options are shown
     * @param showBubblePlotOptions if true, the bubble plot options are shown
     * @param showIonTableOptions if true, the ion table options are shown
     * @param showPtmPlotOptions if true, the PTM plot option are shown
     */
    public void updateAnnotationMenuBarVisableOptions(boolean showSpectrumOptions, boolean showBubblePlotOptions,
            boolean showIonTableOptions, boolean showPtmPlotOptions) {

        // @TODO: replace boolean variables with an Enum
        allCheckBoxMenuItem.setVisible(showSpectrumOptions);
        exportSpectrumGraphicsJMenuItem.setVisible(showSpectrumOptions);
        exportSpectrumMenu.setVisible(showSpectrumOptions);

        // @TODO: remove this when the other tabs also use the extended spectrum panel!
        exportSpectrumAndPlotsGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportSequenceFragmentationGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportIntensityHistogramGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportMassErrorPlotGraphicsJMenuItem.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportSpectrumGraphicsSeparator.setVisible(allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);

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
    public void updateRecentProjectsList() {

        openRecentJMenu.removeAll();
        ArrayList<String> paths = cpsBean.getUserPreferences().getRecentProjects();
        int counter = 1;

        for (String line : paths) {
            JMenuItem menuItem = new JMenuItem(counter++ + ": " + line);

            final String filePath = line;
            final PeptideShakerGUI temp = this;

            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    boolean open = true;

                    if (!dataSaved && getExperiment() != null) {
                        int value = JOptionPane.showConfirmDialog(temp,
                                "Do you want to save the changes to " + getExperiment().getReference() + "?",
                                "Unsaved Changes",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);

                        if (value == JOptionPane.YES_OPTION) {
                            saveMenuItemActionPerformed(null);
                            open = false;
                        } else if (value == JOptionPane.CANCEL_OPTION) {
                            open = false;
                        }
                    }

                    if (open) {
                        if (!new File(filePath).exists()) {
                            JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
                            temp.getUserPreferences().removerRecentProject(filePath);
                        } else {
                            clearData(true, true);
                            clearPreferences();

                            importPeptideShakerFile(new File(filePath));
                            cpsBean.getUserPreferences().addRecentProject(filePath);
                            lastSelectedFolder = new File(filePath).getAbsolutePath();
                        }
                        updateRecentProjectsList();
                    }
                }
            });

            openRecentJMenu.add(menuItem);
        }

        if (openRecentJMenu.getItemCount() == 0) {
            JMenuItem menuItem = new JMenuItem("(empty)");
            menuItem.setEnabled(false);
            openRecentJMenu.add(menuItem);
        }
    }

    /**
     * Add the list of recently used files to the file menu.
     *
     * @param menu the menu to add the recent files list to
     * @param welcomeDialog the welcome dialog reference
     */
    public void loadRecentProjectsList(JPopupMenu menu, WelcomeDialog welcomeDialog) {

        final WelcomeDialog tempWelcomeDialog = welcomeDialog;
        menu.removeAll();
        ArrayList<String> paths = cpsBean.getUserPreferences().getRecentProjects();
        int counter = 1;

        for (String line : paths) {
            JMenuItem menuItem = new JMenuItem(counter++ + ": " + line);

            final String filePath = line;
            final PeptideShakerGUI temp = this;

            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    if (!new File(filePath).exists()) {
                        JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
                        temp.getUserPreferences().removerRecentProject(filePath);
                    } else {
                        tempWelcomeDialog.setVisible(false);
                        tempWelcomeDialog.dispose();
                        setVisible(true);

                        clearData(true, true);
                        clearPreferences();

                        importPeptideShakerFile(new File(filePath));
                        cpsBean.getUserPreferences().addRecentProject(filePath);
                        lastSelectedFolder = new File(filePath).getAbsolutePath();
                    }
                    updateRecentProjectsList();
                }
            });

            menu.add(menuItem);
        }

        if (menu.getComponentCount() == 0) {
            JMenuItem menuItem = new JMenuItem("(empty)");
            menuItem.setEnabled(false);
            menu.add(menuItem);
        }
    }

    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        return cpsBean.getProjectDetails();
    }

    /**
     * Sets the project details.
     *
     * @param projectDetails the project details
     */
    public void setProjectDetails(ProjectDetails projectDetails) {
        cpsBean.setProjectDetails(projectDetails);
    }

    /**
     * Imports informations from a PeptideShaker file.
     *
     * @param aPsFile the PeptideShaker file to import
     */
    public void importPeptideShakerFile(File aPsFile) {

        cpsBean.setCpsFile(aPsFile);

        final PeptideShakerGUI peptideShakerGUI = this; // needed due to threading issues

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Importing Project. Please Wait...");

        // reset the title
        resetFrameTitle();

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("ImportThread") {
            @Override
            public void run() {

                try {
                    // reset enzymes, ptms and preferences
                    loadEnzymes();
                    resetPtmFactory();
                    setDefaultPreferences();
                    setCurentNotes(new ArrayList<String>());
                    updateNotesNotificationCounter();

                    try {
                        cpsBean.loadCpsFile(progressDialog);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occured while reading:\n" + cpsBean.getCpsFile() + ".\n\n"
                                + "It looks like another instance of PeptideShaker is still connected to the file.\n"
                                + "Please close all instances of PeptideShaker and try again.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                    }

                    // Resets the display features generator according to the new project
                    resetDisplayFeaturesGenerator();
                    
                    progressDialog.setTitle("Loading Gene Mappings. Please Wait...");
                    loadGeneMappings(); // have to load the new gene mappings

                    // @TODO: check if the used gene mapping files are available and download if not?
                    if (progressDialog.isRunCanceled()) {
                        clearData(true, true);
                        clearPreferences();
                        progressDialog.setRunFinished();
                        return;
                    }

                    progressDialog.setTitle("Loading FASTA File. Please Wait...");

                    boolean fileFound;
                    try {
                        fileFound = cpsBean.loadFastaFile(new File(getLastSelectedFolder()), progressDialog);
                    } catch (Exception e) {
                        fileFound = false;
                    }

                    if (!fileFound && !locateFastaFileManually()) {
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occured while reading:\n" + getSearchParameters().getFastaFile() + "."
                                + "\n\nOpen canceled.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                        clearData(true, true);
                        clearPreferences();
                        progressDialog.setRunFinished();
                        return;
                    }

                    if (progressDialog.isRunCanceled()) {
                        clearData(true, true);
                        clearPreferences();
                        progressDialog.setRunFinished();
                        return;
                    }

                    progressDialog.setTitle("Loading Spectrum Files. Please Wait...");
                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setMaxPrimaryProgressCounter(getIdentification().getSpectrumFiles().size() + 1);
                    progressDialog.increasePrimaryProgressCounter();

                    int cpt = 0, total = getIdentification().getSpectrumFiles().size();
                    for (String spectrumFileName : getIdentification().getSpectrumFiles()) {
                        
                    progressDialog.setTitle("Loading Spectrum Files (" + ++cpt + " of " + total + "). Please Wait...");
                        progressDialog.increasePrimaryProgressCounter();

                        boolean found;
                        try {
                            found = cpsBean.loadSpectrumFiles(new File(getLastSelectedFolder()), progressDialog);
                        } catch (Exception e) {
                            found = false;
                        }
                        if (!found) {
                            JOptionPane.showMessageDialog(peptideShakerGUI,
                                    "Spectrum file not found: \'" + spectrumFileName + "\'."
                                    + "\nPlease select the spectrum file or the folder containing it manually.",
                                    "File Not Found", JOptionPane.ERROR_MESSAGE);

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
                                found = false;
                                for (File file : mgfFolder.listFiles()) {
                                    for (String spectrumFileName2 : getIdentification().getSpectrumFiles()) {
                                        try {
                                            String fileName = file.getName();
                                            if (spectrumFileName2.equals(fileName)) {
                                                getProjectDetails().addSpectrumFile(file);
                                                spectrumFactory.addSpectra(file, progressDialog);
                                            }
                                            if (fileName.equals(spectrumFileName2)) {
                                                found = true;
                                            }
                                        } catch (Exception e) {
                                            // ignore
                                        }
                                    }
                                }
                                if (!found) {
                                    JOptionPane.showMessageDialog(peptideShakerGUI,
                                            spectrumFileName + " was not found in the given folder.",
                                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                                    clearData(true, true);
                                    clearPreferences();
                                    progressDialog.setRunFinished();
                                    return;
                                }
                            }
                        }

                        if (progressDialog.isRunCanceled()) {
                            clearData(true, true);
                            clearPreferences();
                            progressDialog.setRunFinished();
                            return;
                        }

                    }

                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setRunFinished();
                    peptideShakerGUI.displayResults();
                    allTabsJTabbedPaneStateChanged(null); // display the overview tab data
                    peptideShakerGUI.updateFrameTitle();
                    dataSaved = true;

                } catch (OutOfMemoryError error) {
                    System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
                    Runtime.getRuntime().gc();
                    JOptionPane.showMessageDialog(PeptideShakerGUI.this, JOptionEditorPane.getJOptionEditorPane(
                            "PeptideShaker used up all the available memory and had to be stopped.<br>"
                            + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                            + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                            + "Java Options). See also <a href=\"http://code.google.com/p/compomics-utilities/wiki/JavaTroubleShooting\">JavaTroubleShooting</a>."),
                            "Out Of Memory", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    error.printStackTrace();
                } catch (EOFException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            "An error occured while reading:\n" + cpsBean.getCpsFile() + ".\n\n"
                            + "The file is corrupted and cannot be opened anymore.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            "An error occured while reading:\n" + cpsBean.getCpsFile() + ".\n\n"
                            + "Please verify that the compomics-utilities version used to create\n"
                            + "the file is compatible with your version of PeptideShaker.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Allows the user to locate the FASTA file manually and loads it in the
     * factory
     *
     * @return a boolean indicating whether the loading was successful
     */
    private boolean locateFastaFileManually() throws FileNotFoundException, ClassNotFoundException, IOException {

        JOptionPane.showMessageDialog(this,
                "Fasta file " + getSearchParameters().getFastaFile() + " was not found."
                + "\n\nPlease locate it manually.",
                "File Input Error", JOptionPane.ERROR_MESSAGE);

        JFileChooser fileChooser = new JFileChooser(getLastSelectedFolder()); // @TODO: replace by new getUserSelectedFile with multiple file endings option
        fileChooser.setDialogTitle("Open FASTA File");

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("fasta")
                        || myFile.getName().toLowerCase().endsWith("fas")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Supported formats: FASTA format (.fasta or .fas)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this, "Open");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fastaFile = fileChooser.getSelectedFile();
            setLastSelectedFolder(fastaFile.getAbsolutePath());
            return cpsBean.loadFastaFile(fastaFile.getParentFile(), progressDialog);
        } else {
            return false;
        }
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

        if (!displayProteins && !displayPeptidesAndPsms && !displayCoverage && !displaySpectrum) {
            displayProteins = true;
        }

        proteinsJCheckBoxMenuItem.setSelected(displayProteins);
        peptidesAndPsmsJCheckBoxMenuItem.setSelected(displayPeptidesAndPsms);
        sequenceCoverageJCheckBoxMenuItem.setSelected(displayCoverage);
        spectrumJCheckBoxMenuItem.setSelected(displaySpectrum);

        overviewPanel.setDisplayOptions(proteinsJCheckBoxMenuItem.isSelected(), peptidesAndPsmsJCheckBoxMenuItem.isSelected(),
                sequenceCoverageJCheckBoxMenuItem.isSelected(), spectrumJCheckBoxMenuItem.isSelected());
        overviewPanel.updateSeparators();
    }

    /**
     * Returns the identification filter used when loading the files.
     *
     * @return the identification filter
     */
    public IdFilter getIdFilter() {
        return cpsBean.getIdFilter();
    }

    /**
     * Sets the identification filter used when loading the files.
     *
     * @param idFilter the identification filter to set
     */
    public void setIdFilter(IdFilter idFilter) {
        cpsBean.setIdFilter(idFilter);
    }

    /**
     * Get the current delta masses for use when annotating the spectra.
     *
     * @return the current delta masses
     */
    public HashMap<Double, String> getCurrentMassDeltas() {

        HashMap<Double, String> knownMassDeltas = new HashMap<Double, String>();

        // add the monoisotopic amino acids masses
        knownMassDeltas.put(AminoAcid.A.monoisotopicMass, "A");
        knownMassDeltas.put(AminoAcid.R.monoisotopicMass, "R");
        knownMassDeltas.put(AminoAcid.N.monoisotopicMass, "N");
        knownMassDeltas.put(AminoAcid.D.monoisotopicMass, "D");
        knownMassDeltas.put(AminoAcid.C.monoisotopicMass, "C");
        knownMassDeltas.put(AminoAcid.Q.monoisotopicMass, "Q");
        knownMassDeltas.put(AminoAcid.E.monoisotopicMass, "E");
        knownMassDeltas.put(AminoAcid.G.monoisotopicMass, "G");
        knownMassDeltas.put(AminoAcid.H.monoisotopicMass, "H");
        knownMassDeltas.put(AminoAcid.I.monoisotopicMass, "I/L");
        knownMassDeltas.put(AminoAcid.K.monoisotopicMass, "K");
        knownMassDeltas.put(AminoAcid.M.monoisotopicMass, "M");
        knownMassDeltas.put(AminoAcid.F.monoisotopicMass, "F");
        knownMassDeltas.put(AminoAcid.P.monoisotopicMass, "P");
        knownMassDeltas.put(AminoAcid.S.monoisotopicMass, "S");
        knownMassDeltas.put(AminoAcid.T.monoisotopicMass, "T");
        knownMassDeltas.put(AminoAcid.W.monoisotopicMass, "W");
        knownMassDeltas.put(AminoAcid.Y.monoisotopicMass, "Y");
        knownMassDeltas.put(AminoAcid.V.monoisotopicMass, "V");
        knownMassDeltas.put(AminoAcid.U.monoisotopicMass, "U");
        knownMassDeltas.put(AminoAcid.O.monoisotopicMass, "O");

        // add default neutral losses
//        knownMassDeltas.put(NeutralLoss.H2O.mass, "H2O");
//        knownMassDeltas.put(NeutralLoss.NH3.mass, "NH3");
//        knownMassDeltas.put(NeutralLoss.CH4OS.mass, "CH4OS");
//        knownMassDeltas.put(NeutralLoss.H3PO4.mass, "H3PO4");
//        knownMassDeltas.put(NeutralLoss.HPO3.mass, "HPO3");
//        knownMassDeltas.put(4d, "18O"); // @TODO: should this be added to neutral losses??
//        knownMassDeltas.put(44d, "PEG"); // @TODO: should this be added to neutral losses??
        // add the modifications
        ModificationProfile modificationProfile = getSearchParameters().getModificationProfile();
        ArrayList<String> modificationList = modificationProfile.getAllModifications();
        Collections.sort(modificationList);

        // iterate the modifications list and add the non-terminal modifications
        for (String modification : modificationList) {
            PTM ptm = ptmFactory.getPTM(modification);

            if (ptm != null) {

                String shortName = ptmFactory.getShortName(modification);
                AminoAcidPattern ptmPattern = ptm.getPattern();
                double mass = ptm.getMass();

                if (ptm.getType() == PTM.MODAA) {
                    for (AminoAcid aa : ptmPattern.getAminoAcidsAtTarget()) {
                        if (!knownMassDeltas.containsValue((String) aa.singleLetterCode + "<" + shortName + ">")) {
                            knownMassDeltas.put(mass + aa.monoisotopicMass,
                                    (String) aa.singleLetterCode + "<" + shortName + ">");
                        }
                    }
                }
            } else {
                System.out.println("Error: PTM not found: " + modification);
            }
        }

        return knownMassDeltas;
    }

    /**
     * Saves the modifications made to the project.
     *
     * @param aCloseWhenDone if true, PeptideShaker closes after saving
     */
    public void saveProject(boolean aCloseWhenDone) {

        // check if the project is the example project
        if (cpsBean.getCpsFile() != null && cpsBean.getCpsFile().equals(new File(getJarFilePath() + EXAMPLE_DATASET_PATH))) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Overwriting the example project is not possible.\n"
                    + "Please save to a different location.", "Example Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (value == JOptionPane.OK_OPTION) {
                saveProjectAs(aCloseWhenDone);
            } else {
                // cancel the saving
            }

        } else {

            final boolean closeWhenDone = aCloseWhenDone;

            progressDialog = new ProgressDialogX(this,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Saving. Please Wait...");

            final PeptideShakerGUI tempRef = this; // needed due to threading issues

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("SaveThread") {
                @Override
                public void run() {
                    try {

                        progressDialog.setWaitingText("Saving Results. Please Wait...");
                        cpsBean.saveProject(progressDialog, closeWhenDone);

                        try {
                            ptmFactory.saveFactory();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            cpsBean.getUserPreferences().addRecentProject(cpsBean.getCpsFile());
                            updateRecentProjectsList();

                            if (closeWhenDone) {
                                closePeptideShaker();
                            } else {
                                JOptionPane.showMessageDialog(tempRef, "Project successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                                dataSaved = true;
                            }
                        } else {
                            progressDialog.setRunFinished();
                            JOptionPane.showMessageDialog(tempRef, "Saving of the project was cancelled by the user.", "Save Cancelled", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        catchException(e);
                    }
                }
            }.start();
        }
    }

    /**
     * Sets that the tab was updated.
     *
     * @param tabIndex integer indicating which tab (according to the static
     * indexing) was updated.
     * @param updated boolean indicating whether the tab is updated or not
     */
    public void setUpdated(int tabIndex, boolean updated) {
        updateNeeded.put(tabIndex, !updated);
    }

    /**
     * Update the tabbed panes.
     */
    public void updateTabbedPanes() {
        repaintPanels();
        allTabsJTabbedPaneStateChanged(null);
    }

    /**
     * Set up the initial filters.
     */
    public void setUpInitialFilters() {
        Enzyme enzyme = getSearchParameters().getEnzyme();
        ProteinFilter proteinFilter = new ProteinFilter(enzyme.getName());
        proteinFilter.setIdentifierRegex(enzyme.getName());
        proteinFilter.setDescription("Hides " + enzyme.getName() + " related proteins.");
        proteinFilter.setActive(false);
        getFilterPreferences().addHidingFilter(proteinFilter);
        proteinFilter = new ProteinFilter("Keratin");
        proteinFilter.setIdentifierRegex("keratin");
        proteinFilter.setDescription("Hides keratin.");
        proteinFilter.setActive(false);
        getFilterPreferences().addHidingFilter(proteinFilter);
    }

    /**
     * Returns the variable modifications found in this project. (Consider using
     * the search parameters instead.)
     *
     * @return the variable modifications found in this project
     */
    public ArrayList<String> getFoundVariableModifications() {
        if (identifiedModifications == null) {
            identifiedModifications = new ArrayList<String>();
            for (String peptideKey : getIdentification().getPeptideIdentification()) {

                boolean modified = false;

                for (String modificationName : Peptide.getModificationFamily(peptideKey)) {
                    if (!identifiedModifications.contains(modificationName)) {
                        identifiedModifications.add(modificationName);
                        modified = true;
                    }
                }
                if (!modified && !identifiedModifications.contains(PtmPanel.NO_MODIFICATION)) {
                    identifiedModifications.add(PtmPanel.NO_MODIFICATION);
                }
            }
        }
        return identifiedModifications;
    }

    /**
     * Returns the charges found in the dataset.
     *
     * @return the charges found in the dataset
     */
    public ArrayList<Integer> getCharges() {
        if (getMetrics() == null) {
            return new ArrayList<Integer>();
        }
        return getMetrics().getFoundCharges();
    }

    /**
     * Returns the neutral losses expected in the dataset.
     *
     * @return the neutral losses expected in the dataset
     */
    public ArrayList<NeutralLoss> getNeutralLosses() {
        ArrayList<NeutralLoss> neutralLosses = new ArrayList<NeutralLoss>();
        neutralLosses.addAll(IonFactory.getInstance().getDefaultNeutralLosses());
        boolean found;
        PTM currentPtm;
        for (String modification : getIdentificationFeaturesGenerator().getFoundModifications()) {
            currentPtm = ptmFactory.getPTM(modification);
            found = false;
            for (NeutralLoss ptmNeutralLoss : currentPtm.getNeutralLosses()) {
                for (NeutralLoss neutralLoss : neutralLosses) {
                    if (ptmNeutralLoss.isSameAs(neutralLoss)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    neutralLosses.add(ptmNeutralLoss);
                }
            }
        }
        return neutralLosses;
    }

    /**
     * Returns the object responsible for starring/hiding matches.
     *
     * @return the object responsible for starring/hiding matches
     */
    public StarHider getStarHider() {
        return starHider;
    }

    /**
     * Returns the identification features generator.
     *
     * @return the identification features generator
     */
    public IdentificationFeaturesGenerator getIdentificationFeaturesGenerator() {
        return cpsBean.getIdentificationFeaturesGenerator();
    }

    /**
     * Resets the feature generator.
     */
    public void resetIdentificationFeaturesGenerator() {
        cpsBean.resetIdentificationFeaturesGenerator();
    }

    /**
     * Sets the feature generator.
     * 
     * @param identificationFeaturesGenerator the identification features generator
     */
    public void setIdentificationFeaturesGenerator(IdentificationFeaturesGenerator identificationFeaturesGenerator) {
        cpsBean.setIdentificationFeaturesGenerator(identificationFeaturesGenerator);
    }
    
    /**
     * Resets the display features generator
     */
    public void resetDisplayFeaturesGenerator() {
        displayFeaturesGenerator = new DisplayFeaturesGenerator(getSearchParameters().getModificationProfile(), exceptionHandler);
        displayFeaturesGenerator.setDisplayedPTMs(getDisplayPreferences().getDisplayedPtms());
    }

    /**
     * Returns the display features generator.
     *
     * @return the display features generator
     */
    public DisplayFeaturesGenerator getDisplayFeaturesGenerator() {
        return displayFeaturesGenerator;
    }

    /**
     * Returns the metrics saved while loading the files.
     *
     * @return the metrics saved while loading the files
     */
    public Metrics getMetrics() {
        return cpsBean.getMetrics();
    }

    /**
     * Sets the metrics saved while loading the files.
     *
     * @param metrics the metrics saved while loading the files
     */
    public void setMetrics(Metrics metrics) {
        cpsBean.setMetrics(metrics);
    }

    /**
     * Sets the objects cache in use
     *
     * @param objectsCache the objects cache
     */
    public void setCache(ObjectsCache objectsCache) {
        cpsBean.setObjectsCache(objectsCache);
    }

    /**
     * Returns the objects cache in use
     *
     * @return the objects cache
     */
    public ObjectsCache getCache() {
        return cpsBean.getObjectsCache();
    }

    /**
     * Sets the new mgf file selected.
     *
     * @param mgfFile the name of the new mgf file
     */
    public void mgfFileSelectionChanged(String mgfFile) {
        jumpToPanel.setSpectrumFile(mgfFile);
        //@TODO: in the future we need to store this information like the selected protein/peptide/psm for selection in new tabs
    }

    /**
     * Save the project to the currentPSFile location.
     *
     * @param closeWhenDone if true, PeptideShaker closes when done saving
     */
    public void saveProjectAs(boolean closeWhenDone) {
        File selectedFile = getUserSelectedFile(".cps", "(Compomics Peptide Shaker format) *.cps", "Save As...", false);
        cpsBean.setCpsFile(selectedFile);
        if (selectedFile != null) {
            saveProject(closeWhenDone);
        }
    }

    /**
     * Returns the file selected by the user, or null if no file was selected.
     *
     * @param aFileEnding the file type, e.g., .txt
     * @param aFileFormatDescription the file format description, e.g., (Mascot
     * Generic Format) *.mgf
     * @param aDialogTitle the title for the dialog
     * @param openDialog if true an open dialog is shown, false results in a
     * save dialog
     * @return the file selected by the user, or null if no file or folder was
     * selected
     */
    public File getUserSelectedFile(String aFileEnding, String aFileFormatDescription, String aDialogTitle, boolean openDialog) {

        File selectedFile = Util.getUserSelectedFile(this, aFileEnding, aFileFormatDescription, aDialogTitle, lastSelectedFolder, openDialog);

        if (selectedFile != null) {
            if (selectedFile.isDirectory()) {
                lastSelectedFolder = selectedFile.getAbsolutePath();
            } else {
                lastSelectedFolder = selectedFile.getParentFile().getAbsolutePath();
            }
        }

        return selectedFile;
    }

    /**
     * Jumps to the desired tab
     *
     * @param tabIndex index of the tab as indexed by the static fields
     */
    public void jumpToTab(int tabIndex) {
        allTabsJTabbedPane.setSelectedIndex(tabIndex);
    }

    /**
     * Returns true if the sparklines are to be shown.
     *
     * @return true if the sparklines are to be show
     */
    public boolean showSparklines() {
        return sparklinesJCheckBoxMenuItem.isSelected();
    }

    /**
     * Update the number of surrounding amino acids displayed.
     */
    public void updateSurroundingAminoAcids() {
        overviewPanel.updateSurroundingAminoAcids();
    }

    /**
     * Ask the user if he/she wants to add a shortcut at the desktop.
     */
    private void addShortcutAtDeskTop() {

        String jarFilePath = getJarFilePath();

        if (!jarFilePath.equalsIgnoreCase(".")) {

            // remove the initial '/' at the start of the line
            if (jarFilePath.startsWith("\\") && !jarFilePath.startsWith("\\\\")) {
                jarFilePath = jarFilePath.substring(1);
            }

            String iconFileLocation = jarFilePath + "\\resources\\peptide-shaker.ico";
            String jarFileLocation = jarFilePath + "\\PeptideShaker-" + getVersion() + ".jar";

            try {
                JShellLink link = new JShellLink();
                link.setFolder(JShellLink.getDirectory("desktop"));
                link.setName("Peptide Shaker " + getVersion());
                link.setIconLocation(iconFileLocation);
                link.setPath(jarFileLocation);
                link.save();
            } catch (Exception e) {
                System.out.println("An error occurred when trying to create a desktop shortcut...");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the tips of the day.
     *
     * @return the tips of the day in an ArrayList
     */
    public ArrayList<String> getTips() {

        ArrayList<String> tips;

        try {
            InputStream stream = getClass().getResource("/tips.txt").openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            tips = new ArrayList<String>();
            String line;

            while ((line = b.readLine()) != null) {
                tips.add(line);
            }

            b.close();
        } catch (Exception e) {
            catchException(e);
            tips = new ArrayList<String>();
        }

        return tips;
    }

    /**
     * Displays a news feed at the bottom of the GUI.
     */
    public void checkNewsFeed() {
        
        // @TODO: re-enable later!

//        new Thread("NewsFeedThread") {
//            @Override
//            public synchronized void run() {
//
//                // get the number of new tweets
//                newTweets = getNewTweets();
//
//                int numberOfCurrentTweets = newTweets.size() + publishedTweets.size();
//
//                if (numberOfCurrentTweets > 0) {
//                    newsButton.setText("News (" + numberOfCurrentTweets + ")");
//
//                    // show a pop up
//                    if (newTweets.size() > 0) {
//
//                        String type = "tweets";
//
//                        if (newTweets.size() == 1) {
//                            type = "tweet";
//                        }
//
//                        NotificationDialog notificationDialog = new NotificationDialog(PeptideShakerGUI.this, PeptideShakerGUI.this, false, numberOfCurrentTweets, type);
//                        notificationDialog.setLocation(PeptideShakerGUI.this.getWidth() - notificationDialog.getWidth() - 100 + PeptideShakerGUI.this.getX(),
//                                PeptideShakerGUI.this.getHeight() - 60 + PeptideShakerGUI.this.getY());
//                        //SwingUtils.fadeInAndOut(notificationDialog);
//                    }
//
//                    publishedTweets.addAll(newTweets);
//
//                } else {
//                    newsButton.setText("News");
//                }
//            }
//        }.start();
    }

    /**
     * Returns the list of new tweets, i.e., tweets that have not been read or
     * displayed/published.
     *
     * @return the list of new tweets
     */
    private ArrayList<String> getNewTweets() {

        ArrayList<String> tweets = new ArrayList<String>();

        // set URL
        try {
            URL url = new URL("https://twitter.com/compomics");
            URLConnection spoof = url.openConnection();

            // spoof the connection so we look like a web browser
            spoof.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");
            BufferedReader in = new BufferedReader(new InputStreamReader(spoof.getInputStream()));
            String strLine;

            // loop through every line in the source
            while ((strLine = in.readLine()) != null) {
                if (strLine.lastIndexOf("tweet-timestamp js-permalink js-nav") != -1) {
                    String tweetId = strLine.substring(strLine.indexOf("\"") + 1, strLine.indexOf("\" "));
                    if (!utilitiesUserPreferences.getReadTweets().contains(tweetId) && !publishedTweets.contains(tweetId)) {
                        tweets.add(tweetId);
                    }
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Unable to get twitter feed in off line mode.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tweets;
    }

    @Override
    public void setSelectedExportFolder(String selectedFile) {
        lastSelectedFolder = selectedFile;
    }

    @Override
    public String getDefaultExportFolder() {
        return lastSelectedFolder;
    }

    /**
     * Open the PeptideShaker example dataset.
     */
    public void openExampleFile() {

        boolean open = true;

        if (!dataSaved && getExperiment() != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getExperiment().getReference() + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                open = false;
            } else if (value == JOptionPane.CANCEL_OPTION) {
                open = false;
            }
        }

        if (open) {

            String filePath = getJarFilePath() + EXAMPLE_DATASET_PATH;

            if (!new File(filePath).exists()) {
                JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
            } else {
                clearData(true, true);
                clearPreferences();

                importPeptideShakerFile(new File(filePath));
                cpsBean.getUserPreferences().addRecentProject(filePath);
                lastSelectedFolder = new File(filePath).getAbsolutePath();
            }
            updateRecentProjectsList();
        }
    }

    public void updateFilterSettingsField(String text) {
        // interface method: not implemented in this class as it is not needed
    }

    /**
     * Export the project as a zip file.
     */
    public void exportProjectAsZip() {

        if (!dataSaved) {
            JOptionPane.showMessageDialog(this, "You first need to save the data.", "Unsaved Data", JOptionPane.INFORMATION_MESSAGE);

            // save the data first
            saveMenuItemActionPerformed(null);
        } else {

            if (cpsBean.getCpsFile() != null) {

                // select the output folder
                File selectedFile = getUserSelectedFile(".zip", "(Compressed file format) *.zip", "Export As Zip...", false);

                if (selectedFile != null) {

                    final File zipFile = selectedFile;

                    final PeptideShakerGUI tempRef = this; // needed due to threading issues
                    progressDialog = new ProgressDialogX(this,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);
                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Exporting Project. Please Wait...");

                    new Thread(new Runnable() {
                        public void run() {

                            try {
                                progressDialog.setVisible(true);
                            } catch (IndexOutOfBoundsException e) {
                                // ignore
                            }
                        }
                    }, "ProgressDialog").start();

                    new Thread("ExportThread") {
                        @Override
                        public void run() {

                            progressDialog.setTitle("Getting FASTA File. Please Wait...");

                            File projectFolder = cpsBean.getCpsFile().getParentFile();
                            File fastaLocation = tempRef.getSearchParameters().getFastaFile();
                            ArrayList<String> dataFiles = new ArrayList<String>();
                            dataFiles.add(tempRef.getSearchParameters().getFastaFile().getAbsolutePath());

                            File indexFile = new File(fastaLocation.getParentFile(), fastaLocation.getName() + ".cui");

                            if (indexFile.exists()) {
                                dataFiles.add(indexFile.getAbsolutePath());
                            }

                            progressDialog.setTitle("Getting Spectrum Files. Please Wait...");
                            progressDialog.setPrimaryProgressCounterIndeterminate(false);
                            progressDialog.setValue(0);
                            progressDialog.setMaxPrimaryProgressCounter(getIdentification().getSpectrumFiles().size());

                            ArrayList<String> names = new ArrayList<String>();

                            for (String spectrumFileName : getIdentification().getSpectrumFiles()) {

                                progressDialog.increasePrimaryProgressCounter();

                                File spectrumFile = getProjectDetails().getSpectrumFile(spectrumFileName);

                                if (spectrumFile.exists() && !names.contains(spectrumFile.getName())) {

                                    names.add(spectrumFile.getName());
                                    dataFiles.add(spectrumFile.getAbsolutePath());

                                    indexFile = new File(spectrumFile.getParentFile(), spectrumFile.getName() + ".cui");

                                    if (indexFile.exists()) {
                                        dataFiles.add(indexFile.getAbsolutePath());
                                    }
                                }
                            }

                            progressDialog.setTitle("Zipping Project. Please Wait...");
                            progressDialog.setPrimaryProgressCounterIndeterminate(true);

                            // zip the project
                            try {
                                FileOutputStream fos = new FileOutputStream(zipFile);
                                BufferedOutputStream bos = new BufferedOutputStream(fos);
                                ZipOutputStream out = new ZipOutputStream(bos);
                                final int BUFFER = 2048;
                                byte data[] = new byte[BUFFER];

                                progressDialog.setTitle("Zipping PeptideShaker File. Please Wait...");

                                // add the cps file
                                FileInputStream fi = new FileInputStream(cpsBean.getCpsFile());
                                BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                                ZipEntry entry = new ZipEntry(cpsBean.getCpsFile().getName());
                                out.putNextEntry(entry);
                                int count;
                                while ((count = origin.read(data, 0, BUFFER)) != -1 && !progressDialog.isRunCanceled()) {
                                    out.write(data, 0, count);
                                }
                                origin.close();

                                // add the cps folder if present (obsolete structure)
                                String cpsFolderName = cpsBean.getCpsFile().getName().substring(0, cpsBean.getCpsFile().getName().length() - 4) + "_cps";
                                File cpsFolder = new File(projectFolder, cpsFolderName);

                                if (cpsFolder.exists()) {
                                    String files[] = cpsFolder.list();

                                    progressDialog.setTitle("Zipping PeptideShaker Folder. Please Wait...");
                                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                                    progressDialog.setValue(0);
                                    progressDialog.setMaxPrimaryProgressCounter(files.length);

                                    for (int i = 0; i < files.length && !progressDialog.isRunCanceled(); i++) {

                                        progressDialog.increasePrimaryProgressCounter();

                                        fi = new FileInputStream(new File(cpsFolder, files[i]));
                                        origin = new BufferedInputStream(fi, BUFFER);
                                        entry = new ZipEntry(cpsFolderName + File.separator + files[i]);
                                        out.putNextEntry(entry);
                                        while ((count = origin.read(data, 0, BUFFER)) != -1 && !progressDialog.isRunCanceled()) {
                                            out.write(data, 0, count);
                                        }
                                        origin.close();
                                    }
                                }

                                // add the data files
                                progressDialog.setTitle("Zipping FASTA and Spectrum Files. Please Wait...");
                                progressDialog.setPrimaryProgressCounterIndeterminate(false);
                                progressDialog.setValue(0);
                                progressDialog.setMaxPrimaryProgressCounter(dataFiles.size());

                                for (int i = 0; i < dataFiles.size() && !progressDialog.isRunCanceled(); i++) {

                                    progressDialog.increasePrimaryProgressCounter();

                                    fi = new FileInputStream(new File(dataFiles.get(i)));
                                    origin = new BufferedInputStream(fi, BUFFER);
                                    entry = new ZipEntry("data" + File.separator + new File(dataFiles.get(i)).getName());
                                    out.putNextEntry(entry);
                                    while ((count = origin.read(data, 0, BUFFER)) != -1 && !progressDialog.isRunCanceled()) {
                                        out.write(data, 0, count);
                                    }
                                    origin.close();
                                }

                                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                                progressDialog.setTitle("Cleaning Up. Please Wait...");

                                out.close();
                                bos.close();
                                fos.close();

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();

                                progressDialog.setRunFinished();
                                JOptionPane.showMessageDialog(tempRef, "Could not zip files.", "Zip Error", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();

                                progressDialog.setRunFinished();
                                JOptionPane.showMessageDialog(tempRef, "Could not zip files.", "Zip Error", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }

                            boolean processCancelled = progressDialog.isRunCanceled();
                            progressDialog.setRunFinished();

                            if (!processCancelled) {
                                // get the size (in MB) of the zip file
                                final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                                double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);

                                JOptionPane.showMessageDialog(tempRef, "Project zipped to \'" + zipFile.getAbsolutePath() + "\' (" + sizeOfZippedFile + " MB)",
                                        "Export Sucessful", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }.start();
                }
            }
        }
    }

    @Override
    public Image getNormalIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif"));
    }

    @Override
    public Image getWaitingIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif"));
    }

    /**
     * Returns the default PSM, i.e., the "best" PSM for the given peptide.
     *
     * @param peptideKey the peptide to get the PSM for
     * @return the key of the default PSM
     */
    public String getDefaultPsmSelection(String peptideKey) {

        if (peptideKey.equalsIgnoreCase(PeptideShakerGUI.NO_SELECTION)) {
            return PeptideShakerGUI.NO_SELECTION;
        }

        String psmKey = PeptideShakerGUI.NO_SELECTION;

        try {
            PeptideMatch peptideMatch = getIdentification().getPeptideMatch(peptideKey);
            ArrayList<String> psmKeys;

            try {
                psmKeys = getIdentificationFeaturesGenerator().getSortedPsmKeys(peptideKey);
            } catch (Exception e) {
                try {
                    // try without order
                    psmKeys = peptideMatch.getSpectrumMatches();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    psmKeys = new ArrayList<String>();
                }
            }

            if (!psmKeys.isEmpty()) {
                psmKey = psmKeys.get(0);
            }

        } catch (Exception ex) {
            catchException(ex);
        }

        return psmKey;
    }

    /**
     * Returns the default peptide, i.e., the "best" peptide for the given
     * protein.
     *
     * @param proteinKey the protein to get the peptide for
     * @return the key of the default peptide
     */
    public String getDefaultPeptideSelection(String proteinKey) {

        if (proteinKey.equalsIgnoreCase(PeptideShakerGUI.NO_SELECTION)) {
            return PeptideShakerGUI.NO_SELECTION;
        }

        String peptideKey = PeptideShakerGUI.NO_SELECTION;

        try {
            ProteinMatch proteinMatch = getIdentification().getProteinMatch(proteinKey);
            ArrayList<String> peptideKeys;

            try {
                peptideKeys = getIdentificationFeaturesGenerator().getSortedPeptideKeys(proteinKey);
            } catch (Exception e) {
                try {
                    // try without order
                    peptideKeys = proteinMatch.getPeptideMatches();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    peptideKeys = new ArrayList<String>();
                }
            }

            if (!peptideKeys.isEmpty()) {
                peptideKey = peptideKeys.get(0);
            }

        } catch (Exception ex) {
            catchException(ex);
        }

        return peptideKey;
    }

    /**
     * Clear the gene mappings.
     */
    public void clearGeneMappings() {
        goPanel.clearOldResults();
        dataSaved = false;
    }

    /**
     * Update the gene mapping results.
     */
    public void updateGeneDisplay() {

        dataSaved = false;

        if (getSelectedTab() == PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX) {
            goPanel.displayResults();
        } else {
            setUpdated(PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX, false);

            GenePreferences genePreferences = getGenePreferences();
            String selectedSpecies = genePreferences.getCurrentSpecies();
            String currentSpeciesType = genePreferences.getCurrentSpeciesType();

            if (currentSpeciesType != null) {

                String speciesDatabase = genePreferences.getEnsemblDatabaseName(currentSpeciesType, selectedSpecies);

                if (speciesDatabase != null) {

                    final File goMappingsFile = new File(genePreferences.getGeneMappingFolder(), speciesDatabase + GenePreferences.GO_MAPPING_FILE_SUFFIX);

                    if (goMappingsFile.exists()) {

                        progressDialog = new ProgressDialogX(this,
                                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                                true);
                        progressDialog.setTitle("Getting Gene Mapping Files. Please Wait...");
                        progressDialog.setPrimaryProgressCounterIndeterminate(true);

                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    progressDialog.setVisible(true);
                                } catch (IndexOutOfBoundsException e) {
                                    // ignore
                                }
                            }
                        }, "ProgressDialog").start();

                        new Thread("GoThread") {
                            @Override
                            public void run() {
                                try {
                                    // redraw any tables with chromosome mappings
                                    ((SelfUpdatingTableModel) overviewPanel.getProteinTable().getModel()).fireTableDataChanged();
                                    ((SelfUpdatingTableModel) proteinStructurePanel.getProteinTable().getModel()).fireTableDataChanged();
                                    progressDialog.setRunFinished();
                                } catch (Exception e) {
                                    progressDialog.setRunFinished();
                                    catchException(e);
                                }
                            }
                        }.start();
                    } else {
                        // redraw any tables with chromosome mappings
                        ((SelfUpdatingTableModel) overviewPanel.getProteinTable().getModel()).fireTableDataChanged();
                        ((SelfUpdatingTableModel) proteinStructurePanel.getProteinTable().getModel()).fireTableDataChanged();
                    }
                }
            }
        }
    }

    @Override
    public File getUserModificationsFile() {
        return new File(getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE);
    }

    @Override
    public ArrayList<String> getModificationUse() {
        return modificationUse;
    }

    /**
     * This method saves the modification use in the conf folder.
     */
    private void saveModificationUsage() {

        File folder = new File(getJarFilePath() + File.separator + "resources" + File.separator + "conf" + File.separator);

        if (!folder.exists()) {
            JOptionPane.showMessageDialog(this, new String[]{"Unable to find folder: '" + folder.getAbsolutePath() + "'!",
                "Could not save modification use."}, "Folder Not Found", JOptionPane.WARNING_MESSAGE);
        } else {
            File output = new File(folder, PEPTIDESHAKER_CONFIGURATION_FILE);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(output));
                bw.write("Modification use:" + System.getProperty("line.separator"));
                bw.write(SearchSettingsDialog.getModificationUseAsString(modificationUse) + System.getProperty("line.separator"));
                bw.flush();
                bw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this, "Unable to write file: '" + ioe.getMessage() + "'!\n"
                        + "Could not save modification use.", "File Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    public void notificationClicked(String notificationType) {

        if (notificationType.equalsIgnoreCase("note") || notificationType.equalsIgnoreCase("notes")) {
            notesButtonMouseReleased(null);
        } else if (notificationType.equalsIgnoreCase("tip") || notificationType.equalsIgnoreCase("tips")) {
            // @TODO: implement me
        } else if (notificationType.equalsIgnoreCase("tweet") || notificationType.equalsIgnoreCase("tweets")) {
            newsButtonMouseReleased(null);
        }
    }

    /**
     * Add a note to the current list of notes.
     *
     * @param note the note to add, can contain HTML formatting, but not the
     * HTML start and end tags
     */
    public void addNote(String note) {
        currentNotes.add(note);
        updateNotesNotificationCounter();
    }

    /**
     * Set the list of current notes.
     *
     * @param currentNotes the notes to set
     */
    public void setCurentNotes(ArrayList<String> currentNotes) {
        this.currentNotes = currentNotes;
        updateNotesNotificationCounter();
    }

    /**
     * Update the notification counter for the notes.
     */
    public void updateNotesNotificationCounter() {
        if (currentNotes.size() > 0) {
            notesButton.setText("Notes (" + currentNotes.size() + ")");
        } else {
            notesButton.setText("Notes");
        }
    }

    /**
     * Show a note notification pop up.
     */
    public void showNotesNotification() {
        
        // @TODO: reenable later!
        
//        if (currentNotes.size() > 0) {
//
//            // show a pop up
//            if (currentNotes.size() > 0) {
//
//                String type = "notes";
//
//                if (currentNotes.size() == 1) {
//                    type = "note";
//                }
//
//                NotificationDialog notificationDialog = new NotificationDialog(PeptideShakerGUI.this, PeptideShakerGUI.this, false, currentNotes.size(), type);
//                notificationDialog.setLocation(PeptideShakerGUI.this.getWidth() - notificationDialog.getWidth() - 100 + PeptideShakerGUI.this.getX(),
//                        PeptideShakerGUI.this.getHeight() - 100 + PeptideShakerGUI.this.getY());
//                //SwingUtils.fadeInAndOut(notificationDialog);
//            }
//        }
    }
}
